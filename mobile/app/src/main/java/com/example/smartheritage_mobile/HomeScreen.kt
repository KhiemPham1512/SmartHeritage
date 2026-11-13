package com.example.smartheritage_mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.example.smartheritage_mobile.config.BeaconConfig
import com.example.smartheritage_mobile.config.toFullMediaUrl
import com.example.smartheritage_mobile.data.repository.ArtifactRepository
import com.example.smartheritage_mobile.model.Artifact
import com.example.smartheritage_mobile.model.ResolvedBeaconResponse
import kotlinx.coroutines.launch
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region

private val HomeBackground = Color(0xFFFAF8F6)
private val HomeOrange = Color(0xFFB95008)
private val HomeText = Color(0xFF263247)
private val HomeGray = Color(0xFF718096)
private val HomeBorder = Color(0xFFE8E5E1)
private val HomeBeige = Color(0xFFEADBC5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDetail: (Int) -> Unit,
    onMap: () -> Unit,
    onFavorites: () -> Unit,
    onProfile: () -> Unit
) {
    Log.d("HOME_API", "HomeScreen đã được gọi")

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedLanguage by remember { mutableStateOf("VI / EN") }
    // Ngôn ngữ gửi lên API
    val apiLanguage = if (selectedLanguage == "VI / EN") "vi" else "en"

    // Danh sách hiện vật lấy từ API
    var artifacts by remember { mutableStateOf<List<Artifact>>(emptyList()) }

    // Danh sách 5 hiện vật mẫu mặc định (BC-001 đến BC-005 trong DB Seed)
    val defaultSeedArtifacts = remember {
        listOf(
            Artifact(1, "BC-001", "Khu Đông Sơn", "Trống đồng Ngọc Lũ", "Dấu ấn văn hóa Đông Sơn rực rỡ cách đây hơn 2.500 năm.", "/static/images/hv_001.jpg", "PUBLISHED"),
            Artifact(2, "BC-002", "Khu Gốm sứ", "Bình gốm Chu Đậu", "Tuyệt tác gốm hoa lam Việt Nam thế kỷ XV với nét vẽ thủ công tinh xảo.", "/static/images/hv_002.jpg", "PUBLISHED"),
            Artifact(3, "BC-003", "Khu Điêu khắc", "Tượng Phật Phật Tích", "Tác phẩm điêu khắc đá thời Lý thể hiện đỉnh cao nghệ thuật Phật giáo.", "/static/images/hv_003.jpg", "PUBLISHED"),
            Artifact(4, "BC-004", "Khu Triều Nguyễn", "Ấn vàng Hoàng đế chi bảo", "Bảo vật quốc gia thời Nguyễn bằng vàng ròng chạm khắc hình rồng uốn lượn.", "/static/images/hv_004.jpg", "PUBLISHED"),
            Artifact(5, "BC-005", "Khu Bảo vật", "Cửu đỉnh Huế", "Bộ 9 đỉnh đồng đồ sộ đúc năm 1835 biểu tượng cho quyền lực và sự trường tồn.", "/static/images/hv_005.jpg", "PUBLISHED")
        )
    }

    // Ưu tiên hiển thị từ API, nếu ít hơn 5 item thì bù đủ 5 hiện vật nổi bật
    val displayArtifacts = remember(artifacts) {
        if (artifacts.size >= 5) {
            artifacts.take(5)
        } else if (artifacts.isNotEmpty()) {
            (artifacts + defaultSeedArtifacts.drop(artifacts.size)).take(5)
        } else {
            defaultSeedArtifacts
        }
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Khởi tạo Repository
    val repository = remember { ArtifactRepository() }

    // --- 1. QUẢN LÝ QUYỀN RUNTIME PERMISSIONS ---
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        Log.d("AltBeacon", "Kết quả cấp quyền Bluetooth/Location: $permissionsGranted")
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // --- 2. TRẠNG THÁI IBEACON DETECTED & COOLDOWN MAP (M4) ---
    val cooldownMap = remember { mutableStateMapOf<Int, Long>() }
    var detectedArtifact by remember { mutableStateOf<ResolvedBeaconResponse?>(null) }
    var showDetectedBottomSheet by remember { mutableStateOf(false) }
    var isLoadingBeacon by remember { mutableStateOf(false) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    // --- 3. HÀM TRUNG TÂM XỬ LÝ TÍN HIỆU IBEACON (DÙNG CHO BLE THẬT & SIMULATOR 1 MÁY) ---
    fun processDetectedSignal(uuid: String, major: Int, minor: Int, distance: Double) {
        // Kiểm tra UUID và Khoảng cách <= 2.0m
        if (!uuid.equals(BeaconConfig.GROUP_UUID, ignoreCase = true) || distance > BeaconConfig.MAX_DISTANCE_METERS) {
            Log.d("BeaconProcess", "Bỏ qua iBeacon minor=$minor do khoảng cách ${distance}m > ${BeaconConfig.MAX_DISTANCE_METERS}m hoặc sai UUID")
            return
        }

        // Kiểm tra điều kiện Cooldown 5 phút (300,000 ms)
        val now = System.currentTimeMillis()
        val lastTriggered = cooldownMap[minor]
        if (lastTriggered != null && (now - lastTriggered) <= BeaconConfig.COOLDOWN_MS) {
            val remainSec = (BeaconConfig.COOLDOWN_MS - (now - lastTriggered)) / 1000
            Log.d("BeaconProcess", "Minor $minor đang trong thời gian Cooldown 5 phút (còn $remainSec giây)")
            return
        }

        // Lưu thời điểm kích hoạt
        cooldownMap[minor] = now
        isLoadingBeacon = true
        scanErrorMessage = null

        coroutineScope.launch {
            try {
                val response = repository.resolveBeacon(major = major, minor = minor, lang = apiLanguage)
                if (response.isSuccessful && response.body() != null) {
                    detectedArtifact = response.body()
                    showDetectedBottomSheet = true
                    scanErrorMessage = null
                    Log.d("BeaconProcess", "Kích hoạt thành công hiện vật minor=$minor: ${detectedArtifact?.name}")
                } else {
                    // HTTP 404 (VD: Beacon LOW_BATTERY/LOST_SIGNAL 103, 104) hoặc không tìm thấy:
                    cooldownMap.remove(minor)
                    scanErrorMessage = "iBeacon (minor=$minor) không khả dụng hoặc chưa được kích hoạt (${response.code()})"
                    Log.e("BeaconProcess", scanErrorMessage!!)
                }
            } catch (e: Exception) {
                cooldownMap.remove(minor)
                scanErrorMessage = "Lỗi kết nối máy chủ resolve iBeacon: ${e.message}"
                Log.e("BeaconProcess", "Lỗi resolve minor=$minor", e)
            } finally {
                isLoadingBeacon = false
            }
        }
    }

    fun resetCooldown() {
        cooldownMap.clear()
        scanErrorMessage = "Đã xóa bộ nhớ Cooldown (bạn có thể test lại ngay)"
    }

    // --- 4. BỘ QUÉT TÍN HIỆU BLE IBEACON THẬT (ALTBEACON) ---
    DisposableEffect(permissionsGranted, apiLanguage) {
        if (!permissionsGranted) return@DisposableEffect onDispose {}

        val beaconManager = BeaconManager.getInstanceForApplication(context)
        beaconManager.beaconParsers.clear()
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout(BeaconConfig.IBEACON_LAYOUT)
        )
        beaconManager.foregroundScanPeriod = 1_000L
        beaconManager.foregroundBetweenScanPeriod = 0L
        beaconManager.updateScanPeriods()

        val region = Region("smart-heritage-beacon-region", null, null, null)

        val rangeNotifier = RangeNotifier { beacons, _ ->
            if (beacons.isNullOrEmpty()) return@RangeNotifier

            // Lọc các beacon trùng GROUP_UUID và distance <= 2.0m
            val validBeacons = beacons.filter { beacon ->
                beacon.id1 != null &&
                        beacon.id1.toString().equals(BeaconConfig.GROUP_UUID, ignoreCase = true) &&
                        beacon.distance <= BeaconConfig.MAX_DISTANCE_METERS
            }

            val closestBeacon = validBeacons.minByOrNull { it.distance }
            closestBeacon?.let { beacon ->
                val uuid = beacon.id1.toString()
                val major = beacon.id2?.toInt() ?: BeaconConfig.DEFAULT_MAJOR
                val minor = beacon.id3?.toInt() ?: return@let
                val distance = beacon.distance

                processDetectedSignal(uuid, major, minor, distance)
            }
        }

        try {
            beaconManager.addRangeNotifier(rangeNotifier)
            beaconManager.startRangingBeacons(region)
            Log.d("AltBeacon", "Đã bắt đầu quét iBeacon thật")
        } catch (e: Exception) {
            Log.e("AltBeacon", "Lỗi khi bật quét iBeacon", e)
        }

        onDispose {
            try {
                beaconManager.stopRangingBeacons(region)
                beaconManager.removeRangeNotifier(rangeNotifier)
                Log.d("AltBeacon", "Đã dừng quét iBeacon")
            } catch (e: Exception) {
                Log.e("AltBeacon", "Lỗi khi dừng quét iBeacon", e)
            }
        }
    }

    // Gọi API lấy danh sách hiện vật khi mở màn hình hoặc đổi ngôn ngữ
    LaunchedEffect(apiLanguage) {
        isLoading = true
        errorMessage = null

        try {
            artifacts = repository.getArtifacts(apiLanguage)
            Log.d("HOME_API", "Tải thành công ${artifacts.size} hiện vật")
        } catch (e: Exception) {
            Log.e("HOME_API", "Lỗi gọi API", e)
            errorMessage = e.message ?: "Không thể tải dữ liệu hiện vật"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeBackground)
        ) {

            // HEADER CỐ ĐỊNH Ở ĐỈNH
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "BẠN ĐANG THAM QUAN",
                        fontSize = 8.sp,
                        color = HomeGray
                    )

                    Text(
                        text = "Tên khu di tích",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeText
                    )
                }

                Text(
                    text = selectedLanguage,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, HomeBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            selectedLanguage = if (selectedLanguage == "VI / EN") "EN / VI" else "VI / EN"
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = HomeOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // NỘI DUNG CUỘN (RADAR + 5 HIỆN VẬT NỔI BẬT)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // CHẾ ĐỘ TEST IBEACON TRÊN 1 ĐIỆN THOẠI (1-DEVICE SIMULATOR PANEL)
                SinglePhoneBeaconTestBar(
                    onTriggerSignal = { minor, distance ->
                        processDetectedSignal(BeaconConfig.GROUP_UUID, BeaconConfig.DEFAULT_MAJOR, minor, distance)
                    },
                    onResetCooldown = {
                        resetCooldown()
                    }
                )

                // CHỈ BÁO TRẠNG THÁI SCAN LỖI / LOADING BEACON
                if (scanErrorMessage != null) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = scanErrorMessage!!,
                            color = Color(0xFFE65100),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (isLoadingBeacon) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        color = HomeOrange
                    )
                }

                // RADAR IBEACON
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(
                            Color(0xFFFFF5EA),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ĐANG QUÉT IBEACON",
                        color = HomeOrange,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vòng radar
                    Box(
                        modifier = Modifier.size(82.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .border(1.dp, Color(0xFFFFC58F), CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .border(1.dp, Color(0xFFFFB36D), CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .border(1.dp, Color(0xFFFFA45B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(HomeOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ibeacon_cam),
                                    contentDescription = "iBeacon",
                                    modifier = Modifier.size(28.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Đang dò tìm hiện vật ở gần...",
                        color = HomeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Di chuyển trong khu trưng bày để khám phá",
                        color = HomeGray,
                        fontSize = 8.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TIÊU ĐỀ HIỆN VẬT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hiện vật nổi bật (${displayArtifacts.size})",
                        modifier = Modifier.weight(1f),
                        color = HomeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Xem tất cả",
                        color = HomeOrange,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable {
                            displayArtifacts.firstOrNull()?.let { onDetail(it.artifactId) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HIỂN THỊ ĐỦ 5 HIỆN VẬT NỔI BẬT
                when {
                    isLoading && artifacts.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp),
                            color = HomeOrange
                        )
                    }

                    else -> {
                        displayArtifacts.take(5).forEach { artifact ->
                            ArtifactCard(
                                artifact = artifact,
                                onClick = { onDetail(artifact.artifactId) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // THANH ĐIỀU HƯỚNG DƯỚI CỐ ĐỊNH
            HorizontalDivider(color = HomeBorder)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color.White),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeNavItem("⌂", "Trang chủ", true) {}
                HomeNavItem("▣", "Bản đồ", false, onMap)
                HomeNavItem("♡", "Đã lưu", false, onFavorites)
                HomeNavItem("♙", "Cá nhân", false, onProfile)
            }
        }

        // --- 5. MODAL BOTTOM SHEET M4: POPUP PHÁT HIỆN HIỆN VẬT GẦN ---
        if (showDetectedBottomSheet && detectedArtifact != null) {
            ModalBottomSheet(
                onDismissRequest = { showDetectedBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                DetectedArtifactBottomSheetContent(
                    artifact = detectedArtifact!!,
                    onDismiss = { showDetectedBottomSheet = false },
                    onDetail = {
                        showDetectedBottomSheet = false
                        onDetail(detectedArtifact!!.artifact_id)
                    }
                )
            }
        }
    }
}

// --- BAR CÔNG CỤ TEST IBEACON TRÊN 1 ĐIỆN THOẠI ---
@Composable
private fun SinglePhoneBeaconTestBar(
    onTriggerSignal: (minor: Int, distance: Double) -> Unit,
    onResetCooldown: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedDistance by remember { mutableStateOf(1.2) } // Mặc định 1.2m (Đứng gần <= 2.0m)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
        border = BorderStroke(1.dp, Color(0xFFD0D7DE))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🧪 Test iBeacon (1 Máy)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeText,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = if (isExpanded) "Thu gọn ▲" else "Mở rộng ▼",
                    fontSize = 9.sp,
                    color = HomeOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Chọn khoảng cách giả lập
                    Text(
                        text = "KHOẢNG CÁCH GIẢ LẬP:",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDistance == 1.2,
                            onClick = { selectedDistance = 1.2 },
                            label = { Text("Đứng gần (1.2m - Bật Popup)", fontSize = 9.sp) }
                        )
                        FilterChip(
                            selected = selectedDistance == 3.5,
                            onClick = { selectedDistance = 3.5 },
                            label = { Text("Đứng xa (3.5m - Bỏ qua)", fontSize = 9.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "BẤM VÀO MINOR ĐỂ GIẢ LẬP KÍCH HOẠT:",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BeaconConfig.TEST_MINORS.forEach { minor ->
                            Button(
                                onClick = { onTriggerSignal(minor, selectedDistance) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                contentPadding = PaddingValues(2.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HomeOrange)
                            ) {
                                Text(
                                    text = "$minor",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = onResetCooldown,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "🔄 Xóa bộ nhớ Cooldown (Test lại)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// --- GIAO DIỆN POPUP BOTTOM SHEET M4 POPUP HIỆN VẬT ---
@Composable
private fun DetectedArtifactBottomSheetContent(
    artifact: ResolvedBeaconResponse,
    onDismiss: () -> Unit,
    onDetail: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Tag badge: • PHÁT HIỆN HIỆN VẬT
        Surface(
            color = Color(0xFFFFF0E5),
            shape = RoundedCornerShape(50)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(HomeOrange, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PHÁT HIỆN HIỆN VẬT",
                    color = HomeOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hàng thông tin hiện vật: Ảnh + Tên + Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tải ảnh từ URL đầy đủ với Coil (Nếu null sẽ dùng màu nền ngôi nhà mặc định)
            AsyncImage(
                model = artifact.image_url.toFullMediaUrl(),
                contentDescription = artifact.name,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HomeBeige),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = artifact.name ?: "Tên hiện vật",
                    color = HomeText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = artifact.summary ?: "Thông tin tóm tắt hiện vật đang chờ bạn khám phá.",
                    color = HomeGray,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nút bấm: "Để sau" và "Xem chi tiết" / "Nghe thuyết minh"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(HomeBorder)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HomeText
                )
            ) {
                Text(
                    text = "Để sau",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onDetail,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeOrange,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Xem chi tiết",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ArtifactCard(
    artifact: Artifact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(76.dp)
            .background(Color.White, RoundedCornerShape(11.dp))
            .border(
                1.dp,
                HomeBorder,
                RoundedCornerShape(11.dp)
            )
            .clickable { onClick() }
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artifact.imageUrl.toFullMediaUrl(),
            contentDescription = artifact.name,
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HomeBeige),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(9.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = artifact.name,
                color = HomeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = artifact.summary,
                color = HomeGray,
                fontSize = 8.sp,
                lineHeight = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "›",
            color = HomeGray,
            fontSize = 21.sp,
            modifier = Modifier.padding(horizontal = 5.dp)
        )
    }
}

@Composable
private fun HomeNavItem(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 17.sp,
            color = if (selected) HomeOrange else HomeGray
        )

        Text(
            text = label,
            fontSize = 8.sp,
            color = if (selected) HomeOrange else HomeGray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
