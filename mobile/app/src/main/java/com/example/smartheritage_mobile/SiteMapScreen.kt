package com.example.smartheritage_mobile

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.smartheritage_mobile.config.resolveArtifactDrawableRes
import com.example.smartheritage_mobile.config.resolveArtifactImageModel
import com.example.smartheritage_mobile.data.local.LocalArtifactCache
import com.example.smartheritage_mobile.data.repository.ArtifactRepository
import com.example.smartheritage_mobile.model.MapPinDto

private val MapBackground = Color(0xFFFAF8F6)
private val MapOrange = Color(0xFFB95008)
private val MapOrangeHalo = Color(0x38B95008)
private val MapText = Color(0xFF263247)
private val MapGray = Color(0xFF718096)
private val MapBorder = Color(0xFFE8E5E1)
private val MapBoardBg = Color(0xFFEFE7DC)
private val MapRoomBg = Color(0xFFFCF9F5)
private val MapRoomBorder = Color(0xFFDCD2C4)
private val MapCorridorColor = Color(0xFFD9CEBF)
private val MapRoomLabelColor = Color(0xFF6E5D4F)
private val MapBeige = Color(0xFFEADBC5)

@Composable
fun SiteMapScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onFavorites: () -> Unit,
    onProfile: () -> Unit,
    onArtifactDetail: (Int) -> Unit
) {
    val context = LocalContext.current
    val cache = remember { LocalArtifactCache.getInstance(context) }
    val repository = remember { ArtifactRepository() }

    var lang by remember { mutableStateOf(cache.getPreferredLanguage()) }

    // Danh sách 6 điểm Pin cho đầy đủ cả 6 phòng (Sảnh chính, Khu A, Phòng trưng bày, Khu B, Khu C, Khu hiện vật)
    val fallbackPins = remember(lang) {
        val isEn = lang.equals("en", ignoreCase = true)
        LocalArtifactCache.defaultSeeds.mapIndexed { idx, seed ->
            MapPinDto(
                beacon_id = idx + 1,
                beacon_code = "BC-00${idx + 1}",
                major = 1,
                minor = 101 + idx,
                trigger_distance = 2.0,
                battery_level = 95,
                beacon_status = "ACTIVE",
                map_x = seed.mapX,
                map_y = seed.mapY,
                zone_name = seed.zoneName,
                artifact_id = seed.artifactId,
                artifact_code = seed.artifactCode,
                artifact_name = if (isEn) seed.nameEn else seed.nameVi,
                summary = if (isEn) seed.summaryEn else seed.summaryVi,
                image_url = seed.imageUrl,
                audio_url = if (isEn) seed.audioEnUrl else seed.audioViUrl
            )
        }
    }

    var pins by remember(lang) { mutableStateOf(fallbackPins) }
    val lastDetectedId = remember { cache.getLastDetectedArtifactId() }
    var selectedArtifactId by remember { mutableIntStateOf(lastDetectedId) }

    // Nạp dữ liệu bản đồ thật từ API GET /api/sites/map (và bù đủ Khu C nếu DB có 5 pin)
    LaunchedEffect(lang) {
        try {
            val response = repository.getSiteMap(siteId = 1, lang = lang)
            if (response.isSuccessful && response.body() != null) {
                val apiPins = response.body()!!.pins.filter { it.artifact_id != null }
                if (apiPins.isNotEmpty()) {
                    pins = if (apiPins.size >= 6) {
                        apiPins
                    } else {
                        apiPins + fallbackPins.drop(apiPins.size)
                    }
                    Log.d("SiteMapScreen", "Đã tải ${pins.size} điểm pin trên bản đồ")
                }
            }
        } catch (e: Exception) {
            Log.w("SiteMapScreen", "Sử dụng sơ đồ pin lưu tạm: ${e.message}")
        }
    }

    val selectedPin = remember(pins, selectedArtifactId) {
        pins.find { it.artifact_id == selectedArtifactId }
            ?: pins.lastOrNull()
            ?: fallbackPins.last()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MapBackground)
            .statusBarsPadding()
    ) {
        // --- TOP BAR CHUẨN FIGMA MÀN HÌNH 7 (Vùng bấm nút ← rộng 44dp nhạy 100%) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    fontSize = 24.sp,
                    color = MapText,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Bản đồ di tích",
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MapText,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onBack)
            )

            // Icon đổi ngôn ngữ thuyết minh ở góc phải giống mẫu Figma
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        val nextLang = if (lang == "vi") "en" else "vi"
                        lang = nextLang
                        cache.setPreferredLanguage(nextLang)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌐",
                    fontSize = 20.sp,
                    color = MapOrange
                )
            }
        }

        HorizontalDivider(color = MapBorder)

        // --- NỘI DUNG CUỘN: SƠ ĐỒ MẶT BẰNG & THẺ HIỆN VẬT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Sơ đồ mặt bằng · Chạm pin để khám phá",
                fontSize = 13.sp,
                color = MapGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // KHUNG BẢN ĐỒ LỚN MÀU BE CHUẨN FIGMA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MapBoardBg, RoundedCornerShape(18.dp))
                    .border(1.dp, MapRoomBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                val pinSanhChinh = pins.getOrNull(0) ?: fallbackPins[0]
                val pinKhuA = pins.getOrNull(1) ?: fallbackPins[1]
                val pinPhongTrungBay = pins.getOrNull(2) ?: fallbackPins[2]
                val pinKhuB = pins.getOrNull(3) ?: fallbackPins[3]
                val pinKhuHienVat = pins.getOrNull(4) ?: fallbackPins[4]
                val pinKhuC = pins.getOrNull(5) ?: fallbackPins[5]

                // LƯỚI SƠ ĐỒ 2 CỘT + HÀNH LANG (TẤT CẢ 6 PHÒNG KỂ CẢ KHU C ĐỀU BẤM ĐƯỢC)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(410.dp)
                ) {
                    // CỘT TRÁI: SẢNH CHÍNH -> PHÒNG TRƯNG BÀY -> KHU C
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Phòng 1: SẢNH CHÍNH (Trống đồng Ngọc Lũ)
                        FloorRoomBox(
                            label = "SẢNH CHÍNH",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.1f),
                            pin = pinSanhChinh,
                            isSelected = selectedPin.artifact_id == pinSanhChinh.artifact_id,
                            pinOffsetX = 4.dp,
                            pinOffsetY = (-6).dp,
                            onPinClick = {
                                pinSanhChinh.artifact_id?.let { selectedArtifactId = it }
                            }
                        )

                        // Hành lang ngang
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(MapCorridorColor)
                        )

                        // Phòng 2: PHÒNG TRƯNG BÀY (Tượng Apsara)
                        FloorRoomBox(
                            label = "PHÒNG TRƯNG BÀY",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.1f),
                            pin = pinPhongTrungBay,
                            isSelected = selectedPin.artifact_id == pinPhongTrungBay.artifact_id,
                            pinOffsetX = (-4).dp,
                            pinOffsetY = (-4).dp,
                            onPinClick = {
                                pinPhongTrungBay.artifact_id?.let { selectedArtifactId = it }
                            }
                        )

                        // Hành lang ngang
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(MapCorridorColor)
                        )

                        // Phòng 3: KHU C (Cửu đỉnh Huế - Cho phép bấm chọn & xem chi tiết)
                        FloorRoomBox(
                            label = "KHU C",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.95f),
                            pin = pinKhuC,
                            isSelected = selectedPin.artifact_id == pinKhuC.artifact_id,
                            pinOffsetX = 0.dp,
                            pinOffsetY = (-6).dp,
                            showPinDot = true,
                            onPinClick = {
                                pinKhuC.artifact_id?.let { selectedArtifactId = it }
                            }
                        )
                    }

                    // Hành lang dọc ở giữa
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight()
                            .background(MapCorridorColor)
                    )

                    // CỘT PHẢI: KHU A -> KHU B -> KHU HIỆN VẬT
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Phòng 4: KHU A (Bình gốm Chu Đậu)
                        FloorRoomBox(
                            label = "KHU A",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.95f),
                            pin = pinKhuA,
                            isSelected = selectedPin.artifact_id == pinKhuA.artifact_id,
                            pinOffsetX = (-12).dp,
                            pinOffsetY = (-6).dp,
                            onPinClick = {
                                pinKhuA.artifact_id?.let { selectedArtifactId = it }
                            }
                        )

                        // Hành lang ngang
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(MapCorridorColor)
                        )

                        // Phòng 5: KHU B (Thạp đồng Đào Thịnh)
                        FloorRoomBox(
                            label = "KHU B",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.05f),
                            pin = pinKhuB,
                            isSelected = selectedPin.artifact_id == pinKhuB.artifact_id,
                            pinOffsetX = 14.dp,
                            pinOffsetY = (-6).dp,
                            onPinClick = {
                                pinKhuB.artifact_id?.let { selectedArtifactId = it }
                            }
                        )

                        // Hành lang ngang
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(MapCorridorColor)
                        )

                        // Phòng 6: KHU HIỆN VẬT (Ấn vàng Triều Nguyễn)
                        FloorRoomBox(
                            label = "KHU HIỆN VẬT",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.15f),
                            pin = pinKhuHienVat,
                            isSelected = selectedPin.artifact_id == pinKhuHienVat.artifact_id,
                            pinOffsetX = 0.dp,
                            pinOffsetY = (-8).dp,
                            onPinClick = {
                                pinKhuHienVat.artifact_id?.let { selectedArtifactId = it }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- THẺ HIỆN VẬT GẦN BẠN / ĐANG CHỌN Ở ĐÁY BẢN ĐỒ ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, MapBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            selectedPin.artifact_id?.let { onArtifactDetail(it) }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bottomFallbackRes = resolveArtifactDrawableRes(
                        imageUrl = selectedPin.image_url,
                        artifactName = selectedPin.artifact_name,
                        artifactId = selectedPin.artifact_id
                    )
                    // Hiển thị đúng ảnh thật của hiện vật đang chọn
                    AsyncImage(
                        model = resolveArtifactImageModel(
                            imageUrl = selectedPin.image_url,
                            artifactName = selectedPin.artifact_name,
                            artifactId = selectedPin.artifact_id
                        ),
                        contentDescription = selectedPin.artifact_name ?: "Ảnh hiện vật",
                        placeholder = painterResource(id = bottomFallbackRes),
                        error = painterResource(id = bottomFallbackRes),
                        modifier = Modifier
                            .size(66.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MapBeige),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedPin.artifact_name ?: "Tên hiện vật gần bạn",
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MapText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${selectedPin.zone_name ?: "Khu hiện vật"} · Đang ở gần",
                            fontSize = 12.sp,
                            color = MapGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Xem chi tiết →",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MapOrange,
                            modifier = Modifier.clickable {
                                selectedPin.artifact_id?.let { onArtifactDetail(it) }
                            }
                        )
                    }
                }
            }
        }

        // --- THANH ĐIỀU HƯỚNG DƯỚI CỐ ĐỊNH ---
        HorizontalDivider(color = MapBorder)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MapNavItem("⌂", "Trang chủ", false, onHome)
            MapNavItem("▣", "Bản đồ", true, {})
            MapNavItem("♡", "Đã lưu", false, onFavorites)
            MapNavItem("♙", "Cá nhân", false, onProfile)
        }
    }
}

@Composable
private fun FloorRoomBox(
    label: String,
    modifier: Modifier = Modifier,
    pin: MapPinDto,
    isSelected: Boolean,
    pinOffsetX: Dp = 0.dp,
    pinOffsetY: Dp = 0.dp,
    showPinDot: Boolean = true,
    onPinClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pinPulse")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MapRoomBg, RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MapOrange else MapRoomBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onPinClick)
            .padding(10.dp)
    ) {
        if (showPinDot) {
            // Điểm Pin màu cam có chấm trắng ở giữa giống hệt mẫu Figma
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = pinOffsetX, y = pinOffsetY),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(haloScale)
                            .background(MapOrangeHalo, CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MapOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }

        // Tên khu vực ở góc dưới trái của ô phòng
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MapOrange else MapRoomLabelColor,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun MapNavItem(
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
            color = if (selected) MapOrange else MapGray
        )

        Text(
            text = label,
            fontSize = 8.sp,
            color = if (selected) MapOrange else MapGray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
