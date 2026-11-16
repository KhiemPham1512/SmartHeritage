package com.example.smartheritage_mobile

import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.example.smartheritage_mobile.config.resolveArtifactDrawableRes
import com.example.smartheritage_mobile.config.resolveArtifactImageModel
import com.example.smartheritage_mobile.config.toFullMediaUrl
import com.example.smartheritage_mobile.data.local.LocalArtifactCache
import com.example.smartheritage_mobile.data.repository.ArtifactRepository
import com.example.smartheritage_mobile.model.ArtifactDetailResponse
import kotlinx.coroutines.delay

private val DetailBackground = Color(0xFFFAF8F6)
private val DetailOrange = Color(0xFFB95008)
private val DetailText = Color(0xFF263247)
private val DetailGray = Color(0xFF718096)
private val DetailBorder = Color(0xFFE8E5E1)
private val DetailBeige = Color(0xFFEADBC5)
private val DetailGreenBg = Color(0xFFECFDF3)
private val DetailGreenCircle = Color(0xFFD1FAE5)
private val DetailGreenText = Color(0xFF15803D)

@Composable
fun ArtifactDetailScreen(
    artifactId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cache = remember { LocalArtifactCache.getInstance(context) }
    val repository = remember { ArtifactRepository() }
    val lang = remember { cache.getPreferredLanguage() }

    // Khởi tạo ngay từ bộ nhớ đệm CachedArtifact để mở tức thì cả khi rời vùng sóng / mất mạng
    var detail by remember(artifactId) {
        mutableStateOf(cache.getFallbackDetailResponse(artifactId, lang))
    }
    var isFavorite by remember(artifactId) {
        mutableStateOf(cache.isFavorite(artifactId))
    }
    var selectedRating by remember { mutableIntStateOf(0) }

    // Tải chi tiết hiện vật từ Backend và lưu cập nhật vào bảng CachedArtifact
    LaunchedEffect(artifactId, lang) {
        try {
            val response = repository.getArtifactDetail(artifactId = artifactId, lang = lang)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                detail = body
                cache.saveFromDetail(body)
                Log.d("ArtifactDetail", "Đã nạp & lưu tạm hiện vật #$artifactId: ${body.name}")
            }
        } catch (e: Exception) {
            Log.w("ArtifactDetail", "Đang dùng dữ liệu lưu tạm Offline cho hiện vật #$artifactId: ${e.message}")
        }
    }

    // --- TRÌNH PHÁT AUDIO GUIDE BẰNG EXOPLAYER (MEDIA3) ---
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentSec by remember { mutableIntStateOf(84) } // Mặc định hiển thị 01:24 theo mẫu Figma khi chưa bấm phát
    var hasStartedPlayback by remember { mutableStateOf(false) }
    val totalSec = remember(detail.audio_duration_sec) {
        (detail.audio_duration_sec ?: 250).coerceAtLeast(1)
    }

    // Nạp URL file .mp3 vào ExoPlayer khi có dữ liệu hiện vật
    LaunchedEffect(detail.audio_url) {
        val fullAudioUrl = detail.audio_url.toFullMediaUrl()
        if (!fullAudioUrl.isNullOrBlank()) {
            try {
                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(fullAudioUrl)))
                exoPlayer.prepare()
            } catch (e: Exception) {
                Log.e("ArtifactDetail", "Lỗi chuẩn bị ExoPlayer: ${e.message}")
            }
        }
    }

    // Theo dõi trạng thái Player và cập nhật thanh thời gian
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    currentSec = 0
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            // Chỉ giải phóng ExoPlayer khi người dùng thoát khỏi màn hình Chi tiết
            // Tuyệt đối không tắt khi đi xa khỏi vùng sóng iBeacon
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000L)
            val playerPosSec = (exoPlayer.currentPosition / 1000L).toInt()
            if (exoPlayer.isPlaying && playerPosSec > 0) {
                currentSec = playerPosSec.coerceAtMost(totalSec)
            } else {
                // Dự phòng đếm thời gian mượt khi máy ảo/điện thoại test offline chưa bật Server media
                currentSec = if (currentSec + 1 <= totalSec) currentSec + 1 else 0
                if (currentSec == 0) {
                    isPlaying = false
                }
            }
        }
    }

    val progressFraction = (currentSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailBackground)
            .statusBarsPadding()
    ) {
        // --- TOP BAR CHUẨN FIGMA MÀN HÌNH 6 (Vùng bấm nút ← rộng 44dp nhạy 100%) ---
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
                    color = DetailText,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Chi tiết hiện vật",
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = DetailText,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onBack)
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        isFavorite = cache.toggleFavoriteLocal(artifactId)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFavorite) "♥" else "♡",
                    fontSize = 22.sp,
                    color = DetailOrange
                )
            }
        }

        HorizontalDivider(color = DetailBorder)

        // --- NỘI DUNG CUỘN CHI TIẾT HIỆN VẬT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // 1. KHUNG ẢNH HIỆN VẬT (Hiển thị đúng ảnh chụp thật của hiện vật theo tên/ID)
            val fallbackRes = resolveArtifactDrawableRes(
                imageUrl = detail.image_url,
                artifactName = detail.name,
                artifactId = detail.artifact_id
            )
            AsyncImage(
                model = resolveArtifactImageModel(
                    imageUrl = detail.image_url,
                    artifactName = detail.name,
                    artifactId = detail.artifact_id
                ),
                contentDescription = detail.name ?: "Ảnh hiện vật",
                placeholder = painterResource(id = fallbackRes),
                error = painterResource(id = fallbackRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DetailBeige),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. TÊN HIỆN VẬT & MIÊU TẢ NGẮN GỌN
            Text(
                text = detail.name ?: "Tên hiện vật",
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = DetailText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = detail.summary ?: detail.zone_name ?: "Miêu tả ngắn gọn",
                fontSize = 13.sp,
                color = DetailGray,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. THẺ TRÌNH PHÁT AUDIO THUYẾT MINH (EXOPLAYER)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, DetailBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút Play / Pause tròn màu cam
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DetailOrange)
                            .clickable {
                                if (!hasStartedPlayback) {
                                    hasStartedPlayback = true
                                    if (exoPlayer.currentPosition <= 0L) {
                                        currentSec = 0
                                    }
                                }
                                isPlaying = !isPlaying
                                try {
                                    if (isPlaying) {
                                        exoPlayer.play()
                                    } else {
                                        exoPlayer.pause()
                                    }
                                } catch (e: Exception) {
                                    Log.w("ArtifactDetail", "Chế độ phát offline: ${e.message}")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Thuyết minh hiện vật",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DetailText
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (lang.equals("en", ignoreCase = true)) {
                                "Giọng đọc Tiếng Anh"
                            } else {
                                "Giọng đọc Tiếng Việt"
                            },
                            fontSize = 12.sp,
                            color = DetailGray
                        )
                    }

                    // Nút Loa bật/tắt âm thanh
                    Text(
                        text = if (isMuted) "🔇" else "🔊",
                        fontSize = 20.sp,
                        color = DetailOrange,
                        modifier = Modifier
                            .clickable {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            }
                            .padding(6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thanh tiến trình (Progress Bar) màu cam chuẩn Figma
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE6E4E0))
                        .clickable {
                            // Cho phép chạm tua nhanh để kiểm thử
                            val nextSec = (currentSec + 30).coerceAtMost(totalSec)
                            currentSec = nextSec
                            try {
                                exoPlayer.seekTo(nextSec * 1000L)
                            } catch (_: Exception) {
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(DetailOrange)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thời gian phát: 01:24 / 04:10
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDurationMmSs(currentSec),
                        fontSize = 11.sp,
                        color = DetailGray
                    )
                    Text(
                        text = formatDurationMmSs(totalSec),
                        fontSize = 11.sp,
                        color = DetailGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. HỘP THÔNG BÁO XANH LÁ: ĐÃ LƯU TẠM XEM NGOÀI VÙNG SÓNG
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DetailGreenBg, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(DetailGreenCircle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = DetailGreenText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Đã lưu tạm – Bạn có thể vừa đi xa\nvừa nghe tiếp",
                    color = DetailGreenText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 5. CÂU CHUYỆN LỊCH SỬ
            Text(
                text = "Câu chuyện lịch sử",
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = DetailText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = detail.description
                    ?: "Hiện vật phản ánh đời sống văn hóa, kỹ thuật chế tác và nghệ thuật trang trí của cư dân Việt cổ. Hãy khám phá thêm những câu chuyện đằng sau từng chi tiết của hiện vật.",
                fontSize = 14.sp,
                color = DetailText,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            // 6. ĐÁNH GIÁ CỦA KHÁCH THAM QUAN
            Text(
                text = "Đánh giá của khách tham quan",
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = DetailText
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Hàng 5 ngôi sao màu cam
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (star in 1..5) {
                    val isSelected = star <= selectedRating
                    Text(
                        text = if (isSelected) "★" else "☆",
                        fontSize = 26.sp,
                        color = DetailOrange,
                        modifier = Modifier.clickable {
                            selectedRating = star
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. BÌNH LUẬN GẦN ĐÂY
            Text(
                text = "Bình luận gần đây",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DetailText
            )

            Spacer(modifier = Modifier.height(6.dp))

            val latestComment = detail.feedbacks?.firstOrNull()?.comment
                ?: "Nội dung thuyết minh rất dễ hiểu."

            Text(
                text = "“$latestComment”",
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = DetailGray,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun formatDurationMmSs(totalSeconds: Int): String {
    val safeSec = totalSeconds.coerceAtLeast(0)
    val minutes = safeSec / 60
    val seconds = safeSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}
