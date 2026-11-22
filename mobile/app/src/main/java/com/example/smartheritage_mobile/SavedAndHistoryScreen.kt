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
import com.example.smartheritage_mobile.config.getAppStrings
import com.example.smartheritage_mobile.config.resolveArtifactDrawableRes
import com.example.smartheritage_mobile.config.resolveArtifactImageModel
import com.example.smartheritage_mobile.data.local.LocalArtifactCache
import com.example.smartheritage_mobile.data.repository.ArtifactRepository
import com.example.smartheritage_mobile.model.FavoriteToggleRequestDto
import com.example.smartheritage_mobile.model.VisitLogItemDto
import kotlinx.coroutines.launch

private val SavedBackground = Color(0xFFFAF8F6)
private val SavedOrange = Color(0xFFB95008)
private val SavedHeartRed = Color(0xFFDC2626)
private val SavedHeartUnselected = Color(0xFF94A3B8)
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
    val strings = remember(lang) { getAppStrings(lang) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var favorites by remember { mutableStateOf(cache.getFavoriteItems(lang)) }
    var historyList by remember { mutableStateOf(cache.getLocalVisitHistory()) }

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
                text = strings.savedHistoryTitle,
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
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null,
                    tint = SavedOrange,
                    modifier = Modifier.size(20.dp)
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
                    Text(strings.searchPlaceholder, fontSize = 12.sp)
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
                        text = strings.tabFavorites,
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
                        text = strings.tabHistory,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) SavedOrange else SavedGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTab == 0) {
                Text(
                    text = strings.savedArtifactsHeading,
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
                            text = strings.emptyFavoritesText,
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

                Text(
                    text = strings.recentHistoryHeading,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = SavedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                filteredHistory.take(3).forEach { hist ->
                    RecentVisitTimelineRow(
                        item = hist,
                        passedByPrefix = strings.passedByPrefix,
                        onClick = { onArtifactDetail(hist.artifact_id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            } else {
                Text(
                    text = strings.recentHistoryHeading,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = SavedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                filteredHistory.forEach { hist ->
                    RecentVisitTimelineRow(
                        item = hist,
                        passedByPrefix = strings.passedByPrefix,
                        onClick = { onArtifactDetail(hist.artifact_id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SavedArtifactCard(
                        item = hist,
                        isFavorite = cache.isFavorite(hist.artifact_id),
                        subtitleOverride = "${strings.durationPrefix}${hist.duration_seconds ?: 60} ${strings.secondsUnit} · ${hist.zone_name ?: ""}",
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

        // --- THANH ĐIỀU HƯỚNG DƯỚI CỐ ĐỊNH (Có navigationBarsPadding) ---
        SmartHeritageBottomBar(
            currentTab = BottomNavTab.SAVED,
            strings = strings,
            onHome = onHome,
            onMap = onMap,
            onFavorites = {},
            onProfile = onProfile
        )
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
                text = item.name ?: "Artifact",
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = SavedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitleOverride ?: item.summary ?: "",
                fontSize = 12.sp,
                color = SavedGray,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Chưa bấm tym: viền xám (không đỏ). Đã bấm tym: tim nhỏ màu đỏ.
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onToggleHeart() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                ),
                contentDescription = null,
                tint = if (isFavorite) SavedHeartRed else SavedHeartUnselected,
                modifier = Modifier.size(if (isFavorite) 16.dp else 18.dp)
            )
        }
    }
}

@Composable
private fun RecentVisitTimelineRow(
    item: VisitLogItemDto,
    passedByPrefix: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_clock),
            contentDescription = null,
            tint = SavedGray,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp)
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
                text = "$passedByPrefix${item.zone_name ?: ""} (${item.name ?: ""})",
                fontSize = 13.sp,
                color = SavedText
            )
        }
    }
}
