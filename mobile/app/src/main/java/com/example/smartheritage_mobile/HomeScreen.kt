package com.example.smartheritage_mobile

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.smartheritage_mobile.model.Artifact
import android.util.Log
import com.example.smartheritage_mobile.data.repository.ArtifactRepository

private val HomeBackground = Color(0xFFFAF8F6)
private val HomeOrange = Color(0xFFB95008)
private val HomeText = Color(0xFF263247)
private val HomeGray = Color(0xFF718096)
private val HomeBorder = Color(0xFFE8E5E1)
private val HomeBeige = Color(0xFFEADBC5)

@Composable
fun HomeScreen(
    onDetail: () -> Unit,
    onMap: () -> Unit,
    onFavorites: () -> Unit,
    onProfile: () -> Unit
) {
    Log.d("HOME_API", "HomeScreen đã được gọi")

    var selectedLanguage by remember { mutableStateOf("VI / EN") }
    // Ngôn ngữ gửi lên API
    val apiLanguage = if (selectedLanguage == "VI / EN") {
        "vi"
    } else {
        "en"
    }

    // Danh sách hiện vật lấy từ API
    var artifacts by remember {
        mutableStateOf<List<Artifact>>(emptyList())
    }

    // Trạng thái tải dữ liệu
    var isLoading by remember {
        mutableStateOf(false)
    }

    // Thông báo lỗi nếu gọi API thất bại
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // Khởi tạo Repository
    val repository = remember {
        ArtifactRepository()
    }
    // Kiểm tra quyền truy cập mạng cục bộ
    val context = LocalContext.current
    val localNetworkPermission = "android.permission.ACCESS_LOCAL_NETWORK"

    var hasLocalNetworkPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 37 ||
                    ContextCompat.checkSelfPermission(
                        context,
                        localNetworkPermission
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

// Bộ xử lý yêu cầu cấp quyền
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocalNetworkPermission = granted
    }

// Xin quyền khi mở màn hình Home
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 37 && !hasLocalNetworkPermission) {
            permissionLauncher.launch(localNetworkPermission)
        }
    }
    // Gọi API khi vào trang chủ hoặc đổi ngôn ngữ
    LaunchedEffect(apiLanguage, hasLocalNetworkPermission) {
        if (!hasLocalNetworkPermission) {
            errorMessage = "Ứng dụng chưa được cấp quyền truy cập mạng cục bộ"
            isLoading = false
            return@LaunchedEffect
        }
        Log.d("HOME_API", "HomeScreen đã chạy LaunchedEffect")
        Log.d("HOME_API", "Đang gọi API với ngôn ngữ: $apiLanguage")

        isLoading = true
        errorMessage = null

        try {
            artifacts = repository.getArtifacts(apiLanguage)

            Log.d("HOME_API", "Tải thành công: ${artifacts.size} hiện vật")

        } catch (e: Exception) {
            Log.e("HOME_API", "Lỗi gọi API", e)

            errorMessage = e.message ?: "Không thể tải dữ liệu hiện vật"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {

        // HEADER
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
                    .background(
                        Color.White,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        HomeBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        selectedLanguage =
                            if (selectedLanguage == "VI / EN") "EN / VI"
                            else "VI / EN"
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = HomeOrange,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
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
                            painter = painterResource(
                                id = R.drawable.ibeacon_cam
                            ),
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
                text = "Hiện vật nổi bật",
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
                    onDetail()
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // HIỂN THỊ DỮ LIỆU HIỆN VẬT TỪ API
        when {
            // Đang tải dữ liệu
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp),
                    color = HomeOrange
                )
            }

            // Có lỗi khi gọi API
            errorMessage != null -> {
                Text(
                    text = "Lỗi tải dữ liệu: ${errorMessage}",
                    modifier = Modifier.padding(14.dp),
                    color = Color.Red,
                    fontSize = 10.sp
                )
            }

            // API không có dữ liệu
            artifacts.isEmpty() -> {
                Text(
                    text = "Chưa có hiện vật nào",
                    modifier = Modifier.padding(14.dp),
                    color = HomeGray,
                    fontSize = 10.sp
                )
            }

            // API trả về dữ liệu thành công
            else -> {
                artifacts.take(2).forEach { artifact ->

                    ArtifactCard(
                        title = artifact.name,
                        description = artifact.summary,
                        onClick = onDetail
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // THANH ĐIỀU HƯỚNG DƯỚI
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
}

@Composable
private fun ArtifactCard(
    title: String,
    description: String,
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

        // VÙNG ẢNH: ĐỂ TRỐNG ĐỂ BẠN THÊM ẢNH SAU
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(
                    HomeBeige,
                    RoundedCornerShape(8.dp)
                )
        )

        Spacer(modifier = Modifier.width(9.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = HomeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = HomeGray,
                fontSize = 8.sp,
                lineHeight = 11.sp
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
