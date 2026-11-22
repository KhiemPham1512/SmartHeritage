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
import com.example.smartheritage_mobile.data.local.Session
import com.example.smartheritage_mobile.data.repository.ArtifactRepository
import com.example.smartheritage_mobile.model.errorDetail
import kotlinx.coroutines.launch

private val RegisterBackground = Color(0xFFFAF8F6)
private val RegisterOrange = Color(0xFFB95008)
private val RegisterText = Color(0xFF263247)
private val RegisterGray = Color(0xFF718096)
private val RegisterBorder = Color(0xFFE8E5E1)

@Composable
fun CreateAccountScreen(
    onRegisterSuccess: () -> Unit,
    onLogin: () -> Unit
) {
    val context = LocalContext.current
    val cache = remember { LocalArtifactCache.getInstance(context) }
    val lang = remember { cache.getPreferredLanguage() }
    val strings = remember(lang) { getAppStrings(lang) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val repository = remember { ArtifactRepository() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RegisterBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(34.dp))

        // Logo Smart Heritage chuẩn Figma
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_bao_tang),
                contentDescription = "Logo Smart Heritage",
                modifier = Modifier.size(25.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Smart Heritage",
                color = RegisterText,
                fontSize = 19.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tiêu đề
        Text(
            text = strings.createAccountTitle,
            color = RegisterText,
            fontSize = 22.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = strings.createAccountSub,
            color = RegisterGray,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Họ và tên
        OutlinedTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_user),
                    contentDescription = null,
                    tint = RegisterGray,
                    modifier = Modifier.size(18.dp)
                )
            },
            placeholder = {
                Text(strings.fullNameHint, fontSize = 12.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = registerFieldColors()
        )

        Spacer(modifier = Modifier.height(10.dp))

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
                    tint = RegisterGray,
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
            colors = registerFieldColors()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Mật khẩu
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
                    tint = RegisterGray,
                    modifier = Modifier.size(18.dp)
                )
            },
            placeholder = {
                Text(strings.passwordHint, fontSize = 12.sp)
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (passwordVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
                        ),
                        contentDescription = null,
                        tint = RegisterGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            shape = RoundedCornerShape(10.dp),
            colors = registerFieldColors()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Nhập lại mật khẩu
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = null,
                    tint = RegisterGray,
                    modifier = Modifier.size(18.dp)
                )
            },
            placeholder = {
                Text(strings.confirmPasswordHint, fontSize = 12.sp)
            },
            singleLine = true,
            visualTransformation = if (confirmVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (confirmVisible) R.drawable.ic_eye else R.drawable.ic_eye_off
                        ),
                        contentDescription = null,
                        tint = RegisterGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            shape = RoundedCornerShape(10.dp),
            colors = registerFieldColors()
        )

        // Thông báo lỗi
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Nút đăng ký
        Button(
            onClick = {
                when {
                    fullName.isBlank() ||
                            email.isBlank() ||
                            password.isBlank() ||
                            confirmPassword.isBlank() -> {
                        errorMessage = if (strings.isEn) "Please fill in all fields." else "Vui lòng nhập đầy đủ thông tin."
                    }

                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email).matches() -> {
                        errorMessage = if (strings.isEn) "Invalid email address." else "Địa chỉ email không hợp lệ."
                    }

                    password.length < 6 -> {
                        errorMessage = if (strings.isEn) "Password must be at least 6 characters." else "Mật khẩu cần ít nhất 6 ký tự."
                    }

                    password != confirmPassword -> {
                        errorMessage = if (strings.isEn) "Passwords do not match." else "Mật khẩu nhập lại không khớp."
                    }

                    else -> {
                        loading = true
                        scope.launch {
                            try {
                                val res = repository.register(fullName.trim(), email.trim(), password, lang)
                                val body = res.body()
                                if (res.isSuccessful && body != null) {
                                    Session.saveLogin(body)
                                    onRegisterSuccess()
                                } else {
                                    errorMessage = res.errorDetail()
                                        ?: if (strings.isEn) "Sign-up failed (${res.code()})." else "Đăng ký thất bại (${res.code()})."
                                }
                            } catch (e: Exception) {
                                errorMessage = strings.networkError
                            } finally {
                                loading = false
                            }
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RegisterOrange
            )
        ) {
            Text(
                text = if (loading) "..." else strings.registerNow,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Điều khoản
        Text(
            text = strings.termsNotice,
            modifier = Modifier.fillMaxWidth(),
            color = RegisterGray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quay lại đăng nhập
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.alreadyHaveAccount,
                color = RegisterText,
                fontSize = 12.sp
            )

            Text(
                text = strings.loginBtn,
                color = RegisterOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    onLogin()
                }
            )
        }
    }
}

@Composable
private fun registerFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RegisterOrange,
        unfocusedBorderColor = RegisterBorder,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )
