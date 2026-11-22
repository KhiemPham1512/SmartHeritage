package com.example.smartheritage_mobile

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartheritage_mobile.config.getAppStrings
import com.example.smartheritage_mobile.config.ServerAddress
import com.example.smartheritage_mobile.data.local.LocalArtifactCache
import com.example.smartheritage_mobile.data.local.Session
import com.example.smartheritage_mobile.data.repository.ArtifactRepository
import com.example.smartheritage_mobile.model.ProfileUpdateDto
import kotlinx.coroutines.launch

private val ProfileBackground = Color(0xFFFAF8F6)
private val ProfileOrange = Color(0xFFB95008)
private val ProfileText = Color(0xFF263247)
private val ProfileGray = Color(0xFF718096)
private val ProfileBorder = Color(0xFFE8E5E1)

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onHome: () -> Unit,
    onMap: () -> Unit,
    onFavorites: () -> Unit,
    onSimulator: () -> Unit = {}
) {
    val context = LocalContext.current
    val cache = remember { LocalArtifactCache.getInstance(context) }

    var beaconEnabled by remember { mutableStateOf(true) }
    var currentLang by remember { mutableStateOf(cache.getPreferredLanguage()) }
    val strings = remember(currentLang) { getAppStrings(currentLang) }
    val repository = remember { ArtifactRepository() }
    val scope = rememberCoroutineScope()
    var showServerDialog by remember { mutableStateOf(false) }
    var serverShown by remember { mutableStateOf(Session.serverAddress) }

    if (showServerDialog) {
        ServerSettingsDialog(strings = strings, onDismiss = {
            showServerDialog = false
            serverShown = Session.serverAddress
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBackground)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                strings.profileTitle,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ProfileText,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = null,
                tint = ProfileOrange,
                modifier = Modifier.size(22.dp)
            )
        }

        HorizontalDivider(color = ProfileBorder)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // User information
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .background(Color.White, RoundedCornerShape(13.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(13.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFFFFF5EA),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_user),
                        contentDescription = null,
                        tint = ProfileOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        Session.fullName ?: strings.guestName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        Session.email ?: strings.guestHint,
                        fontSize = 11.sp,
                        color = ProfileGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                strings.settingsHeader,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ProfileGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Language setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(11.dp))
                    .clickable {
                        val nextLang = Session.nextLanguage(currentLang)
                        currentLang = nextLang
                        cache.setPreferredLanguage(nextLang)
                        if (Session.isLoggedIn) {
                            scope.launch {
                                try {
                                    repository.updateProfile(ProfileUpdateDto(preferred_lang = nextLang))
                                } catch (e: Exception) {
                                    // Không có mạng: lựa chọn vẫn được giữ trên máy
                                }
                            }
                        }
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_globe),
                    contentDescription = null,
                    tint = ProfileOrange,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        strings.audioLanguageTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileText
                    )
                    Text(
                        strings.languageDisplayName,
                        fontSize = 11.sp,
                        color = ProfileGray
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = ProfileGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Beacon notification
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(11.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bell),
                    contentDescription = null,
                    tint = ProfileOrange,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        strings.autoBeaconTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileText
                    )
                    Text(
                        if (beaconEnabled) strings.autoBeaconOn else strings.autoBeaconOff,
                        fontSize = 11.sp,
                        color = ProfileGray
                    )
                }

                Switch(
                    checked = beaconEnabled,
                    onCheckedChange = {
                        beaconEnabled = it
                        if (Session.isLoggedIn) {
                            scope.launch {
                                try {
                                    repository.updateProfile(ProfileUpdateDto(auto_beacon_notify = it))
                                } catch (e: Exception) {
                                    // Không có mạng: công tắc vẫn có tác dụng trên máy
                                }
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ProfileOrange
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Help and support
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(11.dp))
                    .clickable { }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_help),
                    contentDescription = null,
                    tint = ProfileOrange,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    strings.helpSupportTitle,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProfileText
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = ProfileGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // iBeacon Transmitter Mode (Multi-Phone Demo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(11.dp))
                    .clickable { onSimulator() }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_antenna),
                    contentDescription = null,
                    tint = ProfileOrange,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        strings.simulatorModeTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileText
                    )
                    Text(
                        strings.simulatorModeSub,
                        fontSize = 10.sp,
                        color = ProfileGray
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = ProfileGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(11.dp))
                    .clickable { showServerDialog = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.serverLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfileText)
                    Text(ServerAddress.normalize(serverShown) ?: strings.serverAuto, fontSize = 10.sp, color = ProfileGray)
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = ProfileGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Logout
            OutlinedButton(
                onClick = {
                    Session.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFFFF8F7),
                    contentColor = Color(0xFFB42318)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFFFD5D2)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logout),
                    contentDescription = null,
                    tint = Color(0xFFB42318),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    strings.logoutBtn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom navigation (Có navigationBarsPadding)
        SmartHeritageBottomBar(
            currentTab = BottomNavTab.PROFILE,
            strings = strings,
            onHome = onHome,
            onMap = onMap,
            onFavorites = onFavorites,
            onProfile = {}
        )
    }
}
