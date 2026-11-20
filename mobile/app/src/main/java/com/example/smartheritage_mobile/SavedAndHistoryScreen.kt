package com.example.smartheritage_mobile

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.smartheritage_mobile.config.resolveArtifactDrawableRes
import com.example.smartheritage_mobile.config.resolveArtifactImageModel
import com.example.smartheritage_mobile.data.local.LocalArtifactCache
import com.example.smartheritage_mobile.data.repository.ArtifactRepository
import com.example.smartheritage_mobile.model.FavoriteToggleRequestDto
import com.example.smartheritage_mobile.model.VisitLogItemDto
import kotlinx.coroutines.launch

private val SavedBackground = Color(0xFFFAF8F6)
private val SavedOrange = Color(0xFFB95008)
private val SavedText = Color(0xFF263247)
private val SavedGray = Color(0xFF718096)
private val SavedBorder = Color(0xFFE8E5E1)
private val SavedTabBg = Color(0xFFF2ECE4)
private val SavedBeige = Color(0xFFEADBC5)

@Composable
fun SavedAndHistoryScreen(
    onHome: () -> Unit,
    onMap: () -> Unit,
    onProfile: () -> Unit,
    onArtifactDetail: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cache = remember { LocalArtifactCache.getInstance(context) }
    val repository = remember { ArtifactRepository() }
    val lang = remember { cache.getPreferredLanguage() }

    // 0 = Tab "Yêu thích", 1 = Tab "Lịch sử"
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var favorites by remember { mutableStateOf(cache.getFavoriteItems(lang)) }
    var historyList by remember { mutableStateOf(cache.getLocalVisitHistory()) }

    // Đồng bộ dữ liệu từ API GET /api/visit-logs/me kết hợp bộ nhớ đệm cục bộ (Offline-first)
    LaunchedEffect(lang) {
        favorites = cache.getFavoriteItems(lang)
        historyList = cache.getLocalVisitHistory()

        try {
            val response = repository.getMyFavoritesAndHistory(
                userId = null,
                deviceId = "android-guest-01",
                lang = lang
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.favorites.isNotEmpty()) {
                    body.favorites.forEach { fav ->
                        cache.setFavoriteLocal(fav.artifact_id, true)
                    }
                    favorites = cache.getFavoriteItems(lang)
                }
                if (body.history.isNotEmpty()) {
                    val localHist = cache.getLocalVisitHistory()
                    val combined = (localHist + body.history).distinctBy {
                        "${it.artifact_id}_${it.visited_at}"
                    }
                    historyList = combined
                }
                Log.d("SavedAndHistory", "Đã đồng bộ ${favorites.size} yêu thích & ${historyList.size} lịch sử")
            }
        } catch (e: Exception) {
            Log.w("SavedAndHistory", "Hiển thị Yêu thích & Lịch sử từ CSDL cục bộ (Offline): ${e.message}")
        }
    }

    val filteredFavorites = remember(favorites, searchQuery) {
        if (searchQuery.isBlank()) {
            favorites
        } else {
            favorites.filter {
                (it.name ?: "").contains(searchQuery, ignoreCase = true) ||
                        (it.summary ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredHistory = remember(historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter {
                (it.name ?: "").contains(searchQuery, ignoreCase = true) ||
                        (it.zone_name ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SavedBackground)
            .statusBarsPadding()
    ) {
        // --- TOP BAR CHUẨN FIGMA MÀN HÌNH 9 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đã lưu & Lịch sử",
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = SavedText,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) searchQuery = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔍",
                    fontSize = 18.sp,
                    color = SavedOrange
                )
            }
        }

        AnimatedVisibility(visible = showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                placeholder = {
                    Text("Tìm hiện vật đã lưu hoặc lịch sử...", fontSize = 12.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavedOrange,
                    unfocusedBorderColor = SavedBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }

        HorizontalDivider(color = SavedBorder)

        // --- NỘI DUNG CUỘN ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // 1. THANH CHUYỂN 2 TAB: "Yêu thích" | "Lịch sử" (Chuẩn mẫu Figma Trang 9)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(SavedTabBg, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) Color.White else Color.Transparent)
                        .clickable { selectedTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Yêu thích",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) SavedOrange else SavedGray
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) Color.White else Color.Transparent)
                        .clickable { selectedTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lịch sử",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) SavedOrange else SavedGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTab == 0) {
                // --- TAB 1: YÊU THÍCH (Kèm Lịch sử tham quan gần đây ở dưới giống hệt Figma Trang 9) ---
                Text(
                    text = "Hiện vật bạn đã lưu",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = SavedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredFavorites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, SavedBorder, RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bạn chưa lưu hiện vật yêu thích nào. Hãy chạm biểu tượng trái tim ♡ ở màn hình Chi tiết hiện vật để lưu lại!",
                            fontSize = 12.sp,
                            color = SavedGray
                        )
                    }
                } else {
                    filteredFavorites.forEach { item ->
                        SavedArtifactCard(
                            item = item,
                            isFavorite = true,
                            onCardClick = { onArtifactDetail(item.artifact_id) },
                            onToggleHeart = {
                                cache.toggleFavoriteLocal(item.artifact_id)
                                favorites = cache.getFavoriteItems(lang)
                                coroutineScope.launch {
                                    try {
                                        repository.toggleFavorite(
                                            FavoriteToggleRequestDto(
                                                user_id = null,
                                                device_id = "android-guest-01",
                                                artifact_id = item.artifact_id
                                            )
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mục Lịch sử tham quan gần đây ở cuối trang giống mẫu Figma Trang 9
                Text(
                    text = "Lịch sử tham quan gần đây",
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = SavedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                filteredHistory.take(3).forEach { hist ->
                    RecentVisitTimelineRow(
                        item = hist,
                        onClick = { onArtifactDetail(hist.artifact_id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            } else {
                // --- TAB 2: LỊCH SỬ ĐÃ THAM QUAN CHI TIẾT ---
                Text(
                    text = "Lịch sử tham quan gần đây",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = SavedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                filteredHistory.forEach { hist ->
                    RecentVisitTimelineRow(
                        item = hist,
                        onClick = { onArtifactDetail(hist.artifact_id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SavedArtifactCard(
                        item = hist,
                        isFavorite = cache.isFavorite(hist.artifact_id),
                        subtitleOverride = "Thời gian xem: ${hist.duration_seconds ?: 60} giây · ${hist.zone_name ?: "Khu trưng bày"}",
                        onCardClick = { onArtifactDetail(hist.artifact_id) },
                        onToggleHeart = {
                            cache.toggleFavoriteLocal(hist.artifact_id)
                            favorites = cache.getFavoriteItems(lang)
                            historyList = cache.getLocalVisitHistory()
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        // --- THANH ĐIỀU HƯỚNG DƯỚI CỐ ĐỊNH ---
        HorizontalDivider(color = SavedBorder)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SavedNavItem("⌂", "Trang chủ", false, onHome)
            SavedNavItem("▣", "Bản đồ", false, onMap)
            SavedNavItem("♡", "Đã lưu", true, {})
            SavedNavItem("♙", "Cá nhân", false, onProfile)
        }
    }
}

@Composable
private fun SavedArtifactCard(
    item: VisitLogItemDto,
    isFavorite: Boolean,
    subtitleOverride: String? = null,
    onCardClick: () -> Unit,
    onToggleHeart: () -> Unit
) {
    val fallbackRes = resolveArtifactDrawableRes(
        imageUrl = item.image_url,
        artifactName = item.name,
        artifactId = item.artifact_id
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, SavedBorder, RoundedCornerShape(14.dp))
            .clickable { onCardClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = resolveArtifactImageModel(
                imageUrl = item.image_url,
                artifactName = item.name,
                artifactId = item.artifact_id
            ),
            contentDescription = item.name,
            placeholder = painterResource(id = fallbackRes),
            error = painterResource(id = fallbackRes),
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SavedBeige),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp)
        ) {
            Text(
                text = item.name ?: "Hiện vật tiêu biểu",
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = SavedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitleOverride
                    ?: item.summary
                    ?: "Khám phá giá trị và câu chuyện lịch sử của hiện vật.",
                fontSize = 12.sp,
                color = SavedGray,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onToggleHeart() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isFavorite) "♡" else "♡",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFavorite) SavedOrange else SavedGray
            )
        }
    }
}

@Composable
private fun RecentVisitTimelineRow(
    item: VisitLogItemDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "🕒",
            fontSize = 14.sp,
            color = SavedGray,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.visited_at ?: "18/10/2026 · 09:30",
                fontSize = 12.sp,
                color = SavedGray
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Đã đi ngang qua: ${item.zone_name ?: "Khu trưng bày A"} (${item.name ?: "Hiện vật"})",
                fontSize = 13.sp,
                color = SavedText
            )
        }
    }
}

@Composable
private fun SavedNavItem(
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
            color = if (selected) SavedOrange else SavedGray
        )

        Text(
            text = label,
            fontSize = 8.sp,
            color = if (selected) SavedOrange else SavedGray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
