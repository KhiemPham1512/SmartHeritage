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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.example.smartheritage_mobile.config.AppStrings
import com.example.smartheritage_mobile.config.getAppStrings
import com.example.smartheritage_mobile.config.resolveArtifactDrawableRes
import com.example.smartheritage_mobile.config.resolveArtifactImageModel
import com.example.smartheritage_mobile.config.toFullMediaUrl
import com.example.smartheritage_mobile.data.local.LocalArtifactCache
import com.example.smartheritage_mobile.data.repository.ArtifactRepository
import com.example.smartheritage_mobile.model.FavoriteToggleRequestDto
import com.example.smartheritage_mobile.model.FeedbackCreateRequestDto
import com.example.smartheritage_mobile.model.VisitLogCreateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DetailBackground = Color(0xFFFAF8F6)
private val DetailOrange = Color(0xFFB95008)
private val DetailHeartRed = Color(0xFFDC2626)
private val DetailHeartUnselected = Color(0xFF94A3B8)
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
    val coroutineScope = rememberCoroutineScope()
    val cache = remember { LocalArtifactCache.getInstance(context) }
    val repository = remember { ArtifactRepository() }
    val lang = remember { cache.getPreferredLanguage() }
    val strings = remember(lang) { getAppStrings(lang) }
    val openedAtMillis = remember { System.currentTimeMillis() }

    // Khởi tạo ngay từ bộ nhớ đệm CachedArtifact để mở tức thì cả khi rời vùng sóng / mất mạng
    var detail by remember(artifactId) {
        mutableStateOf(cache.getFallbackDetailResponse(artifactId, lang))
    }
    var isFavorite by remember(artifactId) {
        mutableStateOf(cache.isFavorite(artifactId))
    }
    var feedbackList by remember(artifactId) {
        mutableStateOf(detail.feedbacks ?: emptyList())
    }
    var selectedRating by remember { mutableIntStateOf(4) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Tải chi tiết hiện vật từ Backend và lưu cập nhật vào bảng CachedArtifact
    LaunchedEffect(artifactId, lang) {
        try {
            val response = repository.getArtifactDetail(artifactId = artifactId, lang = lang)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                detail = body
                val localReviews = cache.getLocalFeedbacks(artifactId)
                val serverReviews = body.feedbacks ?: emptyList()
                feedbackList = (localReviews + serverReviews).ifEmpty {
                    detail.feedbacks ?: emptyList()
                }
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
    var currentSec by remember { mutableIntStateOf(84) }
    var hasStartedPlayback by remember { mutableStateOf(false) }
    val totalSec = remember(detail.audio_duration_sec) {
        (detail.audio_duration_sec ?: 250).coerceAtLeast(1)
    }

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
            exoPlayer.removeListener(listener)
            exoPlayer.release()

            val durationSeconds = ((System.currentTimeMillis() - openedAtMillis) / 1000L).toInt().coerceAtLeast(1)
            cache.recordLocalVisit(artifactId = artifactId, durationSec = durationSeconds)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.recordVisitLog(
                        VisitLogCreateRequest(
                            user_id = null,
                            device_id = "android-guest-01",
                            artifact_id = artifactId,
                            beacon_id = detail.beacon_id ?: artifactId,
                            lang_used = lang,
                            duration_seconds = durationSeconds,
                            is_favorite = isFavorite
                        )
                    )
                    Log.d("VisitLog", "Đã ghi nhận lịch sử xem hiện vật #$artifactId trong ${durationSeconds}s")
                } catch (e: Exception) {
                    Log.w("VisitLog", "Đã lưu lịch sử xem cục bộ (Offline): ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000L)
            val playerPosSec = (exoPlayer.currentPosition / 1000L).toInt()
            if (exoPlayer.isPlaying && playerPosSec > 0) {
                currentSec = playerPosSec.coerceAtMost(totalSec)
            } else {
                currentSec = if (currentSec + 1 <= totalSec) currentSec + 1 else 0
                if (currentSec == 0) {
                    isPlaying = false
                }
            }
        }
    }

    val progressFraction = (currentSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DetailBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- TOP BAR CHUẨN FIGMA MÀN HÌNH 6 ---
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
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = null,
                        tint = DetailText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = strings.artifactDetailTitle,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = DetailText,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onBack)
                )

                // Nút Thả tim Yêu thích: chưa bấm -> viền xám (không đỏ); đã bấm -> tim nhỏ màu đỏ
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable {
                            val newState = cache.toggleFavoriteLocal(artifactId)
                            isFavorite = newState
                            coroutineScope.launch {
                                try {
                                    val res = repository.toggleFavorite(
                                        FavoriteToggleRequestDto(
                                            user_id = null,
                                            device_id = "android-guest-01",
                                            artifact_id = artifactId
                                        )
                                    )
                                    if (res.isSuccessful && res.body() != null) {
                                        isFavorite = res.body()!!.is_favorite
                                        cache.setFavoriteLocal(artifactId, isFavorite)
                                    }
                                } catch (e: Exception) {
                                    Log.w("FavoriteToggle", "Đã lưu trạng thái thả tim cục bộ: ${e.message}")
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                        ),
                        contentDescription = null,
                        tint = if (isFavorite) DetailHeartRed else DetailHeartUnselected,
                        modifier = Modifier.size(if (isFavorite) 18.dp else 21.dp)
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
                // 1. KHUNG ẢNH HIỆN VẬT
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
                    contentDescription = detail.name ?: strings.artifactDetailTitle,
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
                    text = detail.name ?: "Artifact",
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = DetailText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = detail.summary ?: detail.zone_name ?: "",
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
                            Icon(
                                painter = painterResource(
                                    id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                ),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.audioGuideTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DetailText
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = strings.audioGuideVoice,
                                fontSize = 12.sp,
                                color = DetailGray
                            )
                        }

                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
                                ),
                                contentDescription = null,
                                tint = DetailOrange,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE6E4E0))
                            .clickable {
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
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            tint = DetailGreenText,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = strings.cachedNotice,
                        color = DetailGreenText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 5. CÂU CHUYỆN LỊCH SỬ
                Text(
                    text = strings.historicalStoryTitle,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = DetailText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = detail.description ?: "",
                    fontSize = 14.sp,
                    color = DetailText,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 6. ĐÁNH GIÁ CỦA KHÁCH THAM QUAN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.visitorReviewsTitle,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = DetailText,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = strings.writeReviewBtn,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DetailOrange,
                        modifier = Modifier.clickable {
                            showFeedbackDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hàng 5 ngôi sao vector chuẩn
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star_outline),
                            contentDescription = null,
                            tint = DetailOrange,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    selectedRating = star
                                    showFeedbackDialog = true
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 7. BÌNH LUẬN GẦN ĐÂY
                Text(
                    text = strings.recentCommentsTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailText
                )

                Spacer(modifier = Modifier.height(6.dp))

                val latestComment = feedbackList.firstOrNull()?.comment
                    ?: if (strings.isEn) "Very clear and informative audio guide." else "Nội dung thuyết minh rất dễ hiểu."

                Text(
                    text = "“$latestComment”",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = DetailGray,
                    lineHeight = 19.sp
                )

                if (feedbackList.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    feedbackList.drop(1).take(3).forEach { fb ->
                        if (!fb.comment.isNullOrBlank()) {
                            Text(
                                text = "• ${fb.visitor_name ?: "Visitor"} (${fb.rating ?: 5}★): “${fb.comment}”",
                                fontSize = 12.sp,
                                color = DetailGray,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // --- HỘP THOẠI MÀN HÌNH 8: CHIA SẺ CẢM NHẬN CỦA BẠN ---
        if (showFeedbackDialog) {
            ShareFeedbackDialog(
                strings = strings,
                initialRating = selectedRating,
                onDismiss = { showFeedbackDialog = false },
                onSubmit = { rating, commentText ->
                    selectedRating = rating
                    val finalComment = commentText.ifBlank {
                        if (strings.isEn) "Very clear and informative audio guide." else "Nội dung thuyết minh rất dễ hiểu."
                    }

                    val savedLocal = cache.saveLocalFeedback(
                        artifactId = artifactId,
                        visitorName = "Nguyễn Văn A",
                        rating = rating,
                        comment = finalComment
                    )
                    feedbackList = listOf(savedLocal) + feedbackList
                    showFeedbackDialog = false

                    coroutineScope.launch {
                        try {
                            repository.submitFeedback(
                                FeedbackCreateRequestDto(
                                    user_id = null,
                                    visitor_name = "Nguyễn Văn A",
                                    artifact_id = artifactId,
                                    rating = rating,
                                    comment = finalComment
                                )
                            )
                            Log.d("FeedbackSubmit", "Đã gửi đánh giá $rating sao lên Server")
                        } catch (e: Exception) {
                            Log.w("FeedbackSubmit", "Đã lưu đánh giá ở chế độ Offline: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ShareFeedbackDialog(
    strings: AppStrings,
    initialRating: Int,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(initialRating.coerceIn(1, 5)) }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFFFFF3E6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_message),
                        contentDescription = null,
                        tint = DetailOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = strings.feedbackDialogTitle,
                    fontSize = 19.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = DetailText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.feedbackDialogSub,
                    fontSize = 12.sp,
                    color = DetailGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        val isSelected = star <= rating
                        Icon(
                            painter = painterResource(
                                id = if (isSelected) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                            ),
                            contentDescription = null,
                            tint = if (isSelected) DetailOrange else Color(0xFFCBD5E1),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { rating = star }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                    placeholder = {
                        Text(
                            text = strings.feedbackPlaceholder,
                            fontSize = 12.sp,
                            color = DetailGray
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DetailOrange,
                        unfocusedBorderColor = DetailBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onSubmit(rating, comment.trim()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DetailOrange,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = strings.submitFeedbackBtn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.cancelBtn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailGray,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun formatDurationMmSs(totalSeconds: Int): String {
    val safeSec = totalSeconds.coerceAtLeast(0)
    val minutes = safeSec / 60
    val seconds = safeSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}
