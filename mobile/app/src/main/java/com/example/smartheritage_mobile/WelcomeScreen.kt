
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackgroundColor = Color(0xFFFAF8F6)
private val PrimaryColor = Color(0xFFB95008)
private val TextColor = Color(0xFF263247)
private val GrayColor = Color(0xFF718096)
private val BorderColor = Color(0xFFE8E5E1)

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    var selectedLanguage by remember {
        mutableStateOf("Tiếng Việt")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Logo và tên ứng dụng
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "♜",
                color = PrimaryColor,
                fontSize = 25.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Smart Heritage",
                color = TextColor,
                fontSize = 18.sp,
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
                .height(156.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tiêu đề
        Text(
            text = "Di sản trong tầm tay",
            color = TextColor,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Khám phá hiện vật, lắng nghe những câu chuyện " +
                    "lịch sử và lưu lại hành trình của bạn.",
            color = GrayColor,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Chọn ngôn ngữ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(
                    1.dp,
                    BorderColor,
                    RoundedCornerShape(10.dp)
                )
                .clickable {
                    selectedLanguage =
                        if (selectedLanguage == "Tiếng Việt") {
                            "English"
                        } else {
                            "Tiếng Việt"
                        }
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "◎",
                color = PrimaryColor,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = selectedLanguage,
                color = TextColor,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "›",
                color = GrayColor,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "BẮT ĐẦU HÀNH TRÌNH",
            color = GrayColor,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Nút bắt đầu khám phá
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor
            )
        ) {
            Text(
                text = "Bắt đầu khám phá",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Lịch sử sống động hơn",
            modifier = Modifier.fillMaxWidth(),
            color = TextColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Mỗi hiện vật là một câu chuyện.",
            modifier = Modifier.fillMaxWidth(),
            color = GrayColor,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}


