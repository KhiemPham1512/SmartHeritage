package com.example.smartheritage_mobile


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RegisterBackground)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Logo Smart Heritage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "♜",
                color = RegisterOrange,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Smart Heritage",
                color = RegisterText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Tiêu đề
        Text(
            text = "Tạo tài khoản",
            color = RegisterText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Lưu lại hành trình khám phá của bạn",
            color = RegisterGray,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            placeholder = {
                Text("♙   Họ và tên", fontSize = 11.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = registerFieldColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            placeholder = {
                Text("✉   Địa chỉ email", fontSize = 11.sp)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            shape = RoundedCornerShape(10.dp),
            colors = registerFieldColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            placeholder = {
                Text("♙   Mật khẩu", fontSize = 11.sp)
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                Text(
                    text = if (passwordVisible) "Ẩn" else "Hiện",
                    color = RegisterGray,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clickable {
                            passwordVisible = !passwordVisible
                        }
                        .padding(8.dp)
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            shape = RoundedCornerShape(10.dp),
            colors = registerFieldColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            placeholder = {
                Text("♙   Nhập lại mật khẩu", fontSize = 11.sp)
            },
            singleLine = true,
            visualTransformation = if (confirmVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                Text(
                    text = if (confirmVisible) "Ẩn" else "Hiện",
                    color = RegisterGray,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clickable {
                            confirmVisible = !confirmVisible
                        }
                        .padding(8.dp)
                )
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
                fontSize = 10.sp,
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
                        errorMessage = "Vui lòng nhập đầy đủ thông tin."
                    }

                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email).matches() -> {
                        errorMessage = "Địa chỉ email không hợp lệ."
                    }

                    password.length < 6 -> {
                        errorMessage = "Mật khẩu cần ít nhất 6 ký tự."
                    }

                    password != confirmPassword -> {
                        errorMessage = "Mật khẩu nhập lại không khớp."
                    }

                    else -> {
                        onRegisterSuccess()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RegisterOrange
            )
        ) {
            Text(
                text = "Đăng ký",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Điều khoản
        Text(
            text = "Bằng việc đăng ký, bạn đồng ý với Điều khoản",
            modifier = Modifier.fillMaxWidth(),
            color = RegisterGray,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Quay lại đăng nhập
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đã có tài khoản? ",
                color = RegisterText,
                fontSize = 11.sp
            )

            Text(
                text = "Đăng nhập",
                color = RegisterOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    onLogin()
                }
            )
        }
    }
}

// Màu sắc chung cho các ô nhập
@Composable
private fun registerFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RegisterOrange,
        unfocusedBorderColor = RegisterBorder,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )
