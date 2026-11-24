package com.example.smartheritage_mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.example.smartheritage_mobile.config.BeaconConfig
import com.example.smartheritage_mobile.config.TriggerPolicy
import com.example.smartheritage_mobile.config.getAppStrings
import com.example.smartheritage_mobile.config.resolveArtifactDrawableRes
import com.example.smartheritage_mobile.config.resolveArtifactImageModel
import com.example.smartheritage_mobile.data.local.LocalArtifactCache
import com.example.smartheritage_mobile.data.local.Session
import com.example.smartheritage_mobile.config.LanguageCycle
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

    // Khởi tạo Repository & Bộ nhớ đệm Offline (CachedArtifact)
    val repository = remember { ArtifactRepository() }
    val localCache = remember { LocalArtifactCache.getInstance(context) }

    var apiLanguage by remember { mutableStateOf(localCache.getPreferredLanguage()) }
    val selectedLanguage = "${apiLanguage.uppercase()} / ${Session.nextLanguage(apiLanguage).uppercase()}"
    val strings = remember(apiLanguage) { getAppStrings(apiLanguage) }

    // Danh sách hiện vật lấy từ API
    var artifacts by remember { mutableStateOf<List<Artifact>>(emptyList()) }

    // Danh sách 5 hiện vật mẫu song ngữ mặc định (BC-001 đến BC-005)
    val defaultSeedArtifacts = remember(apiLanguage) {
        val isEn = LanguageCycle.isEnglishUi(apiLanguage)
        LocalArtifactCache.defaultSeeds.take(5).map { seed ->
            Artifact(
                artifactId = seed.artifactId,
                artifactCode = seed.artifactCode,
                zoneName = seed.zoneName,
                name = if (isEn) seed.nameEn else seed.nameVi,
                summary = if (isEn) seed.summaryEn else seed.summaryVi,
                imageUrl = seed.imageUrl,
                status = "PUBLISHED"
            )
        }
    }

    // Ưu tiên hiển thị từ API, nếu ít hơn 5 item thì bù đủ 5 hiện vật nổi bật
    val displayArtifacts = remember(artifacts, defaultSeedArtifacts) {
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
    val radiusMap = remember { mutableMapOf<Int, Double>() } // minor → bán kính kích hoạt do Admin đặt (W5)
    val blockedUntil = remember { mutableMapOf<Int, Long>() } // minor → thời điểm được hỏi server lại (không gọi mỗi giây)
    var detectedArtifact by remember { mutableStateOf<ResolvedBeaconResponse?>(null) }
    var showDetectedBottomSheet by remember { mutableStateOf(false) }
    var isLoadingBeacon by remember { mutableStateOf(false) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    // --- 3. HÀM TRUNG TÂM XỬ LÝ TÍN HIỆU IBEACON (DÙNG CHO BLE THẬT & SIMULATOR 1 MÁY) ---
    fun processDetectedSignal(uuid: String, major: Int, minor: Int, distance: Double) {
        // Kiểm tra UUID và khoảng cách so với bán kính của chính beacon này (chưa biết → quét tối đa 5 m)
        if (!uuid.equals(BeaconConfig.GROUP_UUID, ignoreCase = true) || !TriggerPolicy.isCandidate(distance, radiusMap[minor])) {
            Log.d("BeaconProcess", "Bỏ qua iBeacon minor=$minor do khoảng cách ${distance}m > ${radiusMap[minor] ?: BeaconConfig.SCAN_MAX_DISTANCE_METERS}m hoặc sai UUID")
            return
        }

        // Kiểm tra điều kiện Cooldown 5 phút (300,000 ms)
        val now = System.currentTimeMillis()
        if ((blockedUntil[minor] ?: 0L) > now) return
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
                    val resolvedBody = response.body()!!
                    val radius = TriggerPolicy.radiusFor(resolvedBody.trigger_distance)
                    radiusMap[minor] = radius
                    if (distance > radius) {
                        // Vừa biết bán kính của beacon này: khách chưa đứng đủ gần → chưa bật popup
                        cooldownMap.remove(minor)
                        Log.d("BeaconProcess", "minor=$minor ở ${distance}m, ngoài bán kính ${radius}m")
                        return@launch
                    }
                    detectedArtifact = resolvedBody
                    // Lưu ngay hiện vật vừa quét vào bảng CachedArtifact để xem ngoài vùng sóng (M5)
                    localCache.saveFromResolvedBeacon(resolvedBody)
                    showDetectedBottomSheet = true
                    scanErrorMessage = null
                    Log.d("BeaconProcess", "Kích hoạt & lưu cache thành công hiện vật minor=$minor: ${resolvedBody.name}")
                } else {
                    // HTTP 404 (VD: Beacon LOST_SIGNAL 104) hoặc không tìm thấy:
                    cooldownMap.remove(minor)
                    blockedUntil[minor] = System.currentTimeMillis() + TriggerPolicy.RETRY_AFTER_NOT_FOUND_MS
                    scanErrorMessage = "iBeacon (minor=$minor) không khả dụng hoặc chưa được kích hoạt (${response.code()})"
                    Log.e("BeaconProcess", scanErrorMessage!!)
                }
            } catch (e: Exception) {
                cooldownMap.remove(minor)
                blockedUntil[minor] = System.currentTimeMillis() + TriggerPolicy.RETRY_AFTER_ERROR_MS
                scanErrorMessage = "Lỗi kết nối máy chủ resolve iBeacon: ${e.message}"
                Log.e("BeaconProcess", "Lỗi resolve minor=$minor", e)
            } finally {
                isLoadingBeacon = false
            }
        }
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
                        TriggerPolicy.isCandidate(beacon.distance, beacon.id3?.toInt()?.let { radiusMap[it] })
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
                .statusBarsPadding()
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
                        text = strings.youAreVisiting,
                        fontSize = 9.sp,
                        color = HomeGray
                    )

                    Text(
                        text = strings.siteName,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Serif,
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
                            val nextLang = Session.nextLanguage(apiLanguage)
                            apiLanguage = nextLang
                            localCache.setPreferredLanguage(nextLang)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = HomeOrange,
                    fontSize = 10.sp,
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
                        text = strings.scanningIbeacon,
                        color = HomeOrange,
                        fontSize = 9.sp,
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
                                    .size(28.dp)
                                    .background(HomeOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_bluetooth),
                                    contentDescription = "iBeacon",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = strings.searchingNearby,
                        color = HomeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = strings.moveToExplore,
                        color = HomeGray,
                        fontSize = 9.sp
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
                        text = "${strings.featuredArtifacts} (${displayArtifacts.size})",
                        modifier = Modifier.weight(1f),
                        color = HomeText,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = strings.seeAll,
                        color = HomeOrange,
                        fontSize = 11.sp,
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

            // THANH ĐIỀU HƯỚNG DƯỚI CỐ ĐỊNH (Có navigationBarsPadding tránh bị thanh Home che)
            SmartHeritageBottomBar(
                currentTab = BottomNavTab.HOME,
                strings = strings,
                onHome = {},
                onMap = onMap,
                onFavorites = onFavorites,
                onProfile = onProfile
            )
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
                    badgeText = strings.artifactDetectedBadge,
                    laterText = strings.laterBtn,
                    detailText = strings.viewDetailBtn,
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

// --- GIAO DIỆN POPUP BOTTOM SHEET M4 POPUP HIỆN VẬT ---
@Composable
private fun DetectedArtifactBottomSheetContent(
    artifact: ResolvedBeaconResponse,
    badgeText: String,
    laterText: String,
    detailText: String,
    onDismiss: () -> Unit,
    onDetail: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
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
                    text = badgeText,
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
            // Tải ảnh thật của đúng hiện vật vào khung hình ngôi nhà
            val popupFallbackRes = resolveArtifactDrawableRes(
                imageUrl = artifact.image_url,
                artifactName = artifact.name,
                artifactId = artifact.artifact_id
            )
            AsyncImage(
                model = resolveArtifactImageModel(
                    imageUrl = artifact.image_url,
                    artifactName = artifact.name,
                    artifactId = artifact.artifact_id
                ),
                contentDescription = artifact.name,
                placeholder = painterResource(id = popupFallbackRes),
                error = painterResource(id = popupFallbackRes),
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
                    fontFamily = FontFamily.Serif,
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
                    text = laterText,
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
                    text = detailText,
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
        val cardFallbackRes = resolveArtifactDrawableRes(
            imageUrl = artifact.imageUrl,
            artifactName = artifact.name,
            artifactId = artifact.artifactId
        )
        AsyncImage(
            model = resolveArtifactImageModel(
                imageUrl = artifact.imageUrl,
                artifactName = artifact.name,
                artifactId = artifact.artifactId
            ),
            contentDescription = artifact.name,
            placeholder = painterResource(id = cardFallbackRes),
            error = painterResource(id = cardFallbackRes),
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
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif,
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

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = HomeGray,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(18.dp)
        )
    }
}
