package com.example.smartheritage_mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartheritage_mobile.config.AppStrings

private val NavOrange = Color(0xFFB95008)
private val NavGray = Color(0xFF718096)
private val NavBorder = Color(0xFFE8E5E1)

enum class BottomNavTab {
    HOME, MAP, SAVED, PROFILE
}

@Composable
fun SmartHeritageBottomBar(
    currentTab: BottomNavTab,
    strings: AppStrings,
    onHome: () -> Unit,
    onMap: () -> Unit,
    onFavorites: () -> Unit,
    onProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = NavBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                iconRes = R.drawable.ic_home,
                label = strings.navHome,
                selected = currentTab == BottomNavTab.HOME,
                onClick = onHome
            )
            BottomNavItem(
                iconRes = R.drawable.ic_map,
                label = strings.navMap,
                selected = currentTab == BottomNavTab.MAP,
                onClick = onMap
            )
            BottomNavItem(
                iconRes = R.drawable.ic_heart_outline,
                label = strings.navSaved,
                selected = currentTab == BottomNavTab.SAVED,
                onClick = onFavorites
            )
            BottomNavItem(
                iconRes = R.drawable.ic_user,
                label = strings.navProfile,
                selected = currentTab == BottomNavTab.PROFILE,
                onClick = onProfile
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tintColor = if (selected) NavOrange else NavGray
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = tintColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
