package com.example.smartheritage_mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartheritage_mobile.config.getAppStrings
import com.example.smartheritage_mobile.data.local.LocalArtifactCache

private val LoginBackground = Color(0xFFFAF8F6)
private val LoginOrange = Color(0xFFB95008)
private val LoginText = Color(0xFF263247)
private val LoginGray = Color(0xFF718096)
private val LoginBorder = Color(0xFFE8E5E1)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onGuest: () -> Unit,
    onBeaconStation: () -> Unit = {}
) {
    val context = LocalContext.current
    val cache = remember { LocalArtifactCache.getInstance(context) }
    val lang = remember { cache.getPreferredLanguage() }
    val strings = remember(lang) { getAppStrings(lang) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(38.dp))

        // Logo chuẩn Figma
        Row(
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
                "Smart Heritage",
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = LoginText
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = strings.welcomeBack,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 22.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = LoginText
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = strings.loginSubtitle,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 12.sp,
            color = LoginGray
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mail),
                    contentDescription = null,
                    tint = LoginGray,
                    modifier = Modifier.size(18.dp)
                )
            },
            placeholder = {
                Text(strings.emailHint, fontSize = 12.sp)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LoginOrange,
                unfocusedBorderColor = LoginBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = null,
                    tint = LoginGray,
                    modifier = Modifier.size(18.dp)
                )
            },
            placeholder = {
                Text(strings.passwordHint, fontSize = 12.sp)
            },
            singleLine = true,
            visualTransformation = if (passwordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (passwordVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
                        ),
                        contentDescription = null,
                        tint = LoginGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LoginOrange,
                unfocusedBorderColor = LoginBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        // Forgot password
        Text(
            text = strings.forgotPassword,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.End,
            color = LoginOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Login button
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = if (strings.isEn) "Please enter email and password." else "Vui lòng nhập email và mật khẩu."
                } else if (!android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email).matches()) {
                    errorMessage = if (strings.isEn) "Invalid email address." else "Địa chỉ email không hợp lệ."
                } else {
                    onLoginSuccess()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LoginOrange
            )
        ) {
            Text(
                strings.loginBtn,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Register
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.noAccount,
                fontSize = 12.sp,
                color = LoginText
            )
            Text(
                strings.registerNow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LoginOrange,
                modifier = Modifier.clickable {
                    onRegister()
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = LoginBorder
            )
            Text(
                strings.orDivider,
                fontSize = 10.sp,
                color = LoginGray
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = LoginBorder
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onGuest,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = LoginText
            )
        ) {
            Text(
                strings.continueGuestBtn,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            strings.guestSub,
            fontSize = 11.sp,
            color = LoginGray
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Nút mở nhanh chế độ Phát sóng iBeacon (1 chạm biến điện thoại thành máy phát cho demo nhiều máy)
        OutlinedButton(
            onClick = onBeaconStation,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFFFF5EA),
                contentColor = LoginOrange
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD8B8))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_antenna),
                contentDescription = null,
                tint = LoginOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.beaconStationShortcut,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
