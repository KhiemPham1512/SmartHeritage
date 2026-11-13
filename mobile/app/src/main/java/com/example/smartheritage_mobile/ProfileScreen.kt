package com.example.smartheritage_mobile



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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var beaconEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("Tiếng Việt") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Tài khoản cá nhân",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ProfileText,
                modifier = Modifier.weight(1f)
            )

            Text(
                "⚙",
                fontSize = 23.sp,
                color = ProfileOrange
            )
        }

        HorizontalDivider(color = ProfileBorder)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
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
                    Text(
                        "♙",
                        fontSize = 24.sp,
                        color = ProfileOrange
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        "Nguyễn Văn A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "nguyenvana@email.com",
                        fontSize = 9.sp,
                        color = ProfileGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                "CÀI ĐẶT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = ProfileGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Language setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(11.dp))
                    .clickable {
                        selectedLanguage =
                            if (selectedLanguage == "Tiếng Việt")
                                "English"
                            else
                                "Tiếng Việt"
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("◎", color = ProfileOrange, fontSize = 19.sp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Ngôn ngữ thuyết minh",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileText
                    )
                    Text(
                        selectedLanguage,
                        fontSize = 9.sp,
                        color = ProfileGray
                    )
                }

                Text("›", color = ProfileGray, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Beacon notification
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(11.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("♧", color = ProfileOrange, fontSize = 19.sp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Thông báo tự động iBeacon",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileText
                    )
                    Text(
                        if (beaconEnabled) "Bật thông báo" else "Tắt thông báo",
                        fontSize = 9.sp,
                        color = ProfileGray
                    )
                }

                Switch(
                    checked = beaconEnabled,
                    onCheckedChange = {
                        beaconEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ProfileOrange
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Help and support
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(11.dp))
                    .clickable { }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("□", color = ProfileOrange, fontSize = 19.sp)

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    "Trợ giúp & hỗ trợ",
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProfileText
                )

                Text("›", color = ProfileGray, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // iBeacon Simulator (Test/Demo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, ProfileBorder, RoundedCornerShape(11.dp))
                    .clickable { onSimulator() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📡", color = ProfileOrange, fontSize = 19.sp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Chế độ Giả lập iBeacon (Test/Demo)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileText
                    )
                    Text(
                        "Phát sóng iBeacon để kiểm thử cột mốc M3/M4",
                        fontSize = 9.sp,
                        color = ProfileGray
                    )
                }

                Text("›", color = ProfileGray, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(43.dp),
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
                Text(
                    "⇥   Đăng xuất",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom navigation
        HorizontalDivider(color = ProfileBorder)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileNavItem("⌂", "Trang chủ", false, onHome)
            ProfileNavItem("▣", "Bản đồ", false, onMap)
            ProfileNavItem("♡", "Đã lưu", false, onFavorites)
            ProfileNavItem("♙", "Cá nhân", true, {})
        }
    }
}

@Composable
private fun ProfileNavItem(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            icon,
            fontSize = 19.sp,
            color = if (selected) ProfileOrange else ProfileGray
        )

        Text(
            label,
            fontSize = 8.sp,
            color = if (selected) ProfileOrange else ProfileGray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
