package com.example.smartheritage_mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartheritage_mobile.config.getAppStrings
import com.example.smartheritage_mobile.data.local.LocalArtifactCache
import com.example.smartheritage_mobile.data.local.Session

private val BackgroundColor = Color(0xFFFAF8F6)
private val PrimaryColor = Color(0xFFB95008)
private val TextColor = Color(0xFF263247)
private val GrayColor = Color(0xFF718096)
private val BorderColor = Color(0xFFE8E5E1)

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val cache = remember { LocalArtifactCache.getInstance(context) }
    var currentLang by remember { mutableStateOf(cache.getPreferredLanguage()) }
    val strings = remember(currentLang) { getAppStrings(currentLang) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Logo và tên ứng dụng chuẩn Figma
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_bao_tang),
                contentDescription = "Logo Smart Heritage",
                modifier = Modifier.size(26.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Smart Heritage",
                color = TextColor,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Khung chứa ảnh di sản
        Image(
            painter = painterResource(
                id = R.drawable.bao_tang_lich_su_tphcm
            ),
            contentDescription = "Hình ảnh di sản",
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Tiêu đề
        Text(
            text = strings.welcomeTitle,
            color = TextColor,
            fontSize = 22.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = strings.welcomeSubtitle,
            color = GrayColor,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Chọn ngôn ngữ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(
                    1.dp,
                    BorderColor,
                    RoundedCornerShape(10.dp)
                )
                .clickable {
                    val nextLang = Session.nextLanguage(currentLang)
                    currentLang = nextLang
                    cache.setPreferredLanguage(nextLang)
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_globe),
                contentDescription = null,
                tint = PrimaryColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = strings.languageDisplayName,
                color = TextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = GrayColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = strings.startJourneyLabel,
            color = GrayColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Nút bắt đầu khám phá
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor
            )
        ) {
            Text(
                text = strings.startExploringBtn,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = strings.livingHistoryTitle,
            modifier = Modifier.fillMaxWidth(),
            color = TextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = strings.livingHistorySub,
            modifier = Modifier.fillMaxWidth(),
            color = GrayColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
