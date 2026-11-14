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

private val LoginBackground = Color(0xFFFAF8F6)
private val LoginOrange = Color(0xFFB95008)
private val LoginText = Color(0xFF263247)
private val LoginGray = Color(0xFF718096)
private val LoginBorder = Color(0xFFE8E5E1)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onGuest: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackground)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(55.dp))

        // Logo
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("♜", color = LoginOrange, fontSize = 25.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Smart Heritage",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LoginText
            )
        }

        Spacer(modifier = Modifier.height(38.dp))

        Text(
            text = "Chào mừng trở lại",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LoginText
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Đăng nhập để tiếp tục hành trình khám phá",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 11.sp,
            color = LoginGray
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                Text("✉   Địa chỉ email", fontSize = 12.sp)
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
            placeholder = {
                Text("♙   Mật khẩu", fontSize = 12.sp)
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
                Text(
                    text = if (passwordVisible) "Ẩn" else "Hiện",
                    color = LoginGray,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clickable {
                            passwordVisible = !passwordVisible
                        }
                        .padding(8.dp)
                )
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
            text = "Quên mật khẩu?",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.End,
            color = LoginOrange,
            fontSize = 10.sp,
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
                    errorMessage = "Vui lòng nhập email và mật khẩu."
                } else if (!android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email).matches()) {
                    errorMessage = "Địa chỉ email không hợp lệ."
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
                "Đăng nhập",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Register
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Chưa có tài khoản? ",
                fontSize = 11.sp,
                color = LoginText
            )
            Text(
                "Đăng ký",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LoginOrange,
                modifier = Modifier.clickable {
                    onRegister()
                }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = LoginBorder
            )
            Text(
                "  HOẶC  ",
                fontSize = 9.sp,
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
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = LoginText
            )
        ) {
            Text(
                "Tiếp tục với tư cách Khách (Guest)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            "Khám phá di sản không cần tài khoản",
            fontSize = 10.sp,
            color = LoginGray
        )
    }
}
