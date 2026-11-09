package com.example.smartheritage_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "language"
                ) {
                    composable("language") {
                        WelcomeScreen(
                            onContinue = {
                                navController.navigate("auth")
                            }
                        )
                    }

                    //Màn hình đăng nhập
                    composable("auth") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("profile") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            },
                            onRegister = {
                                navController.navigate("register")
                            },
                            onGuest = {
                                navController.navigate("home")
                            }
                        )
                    }

                    //MÀn hình 3
                    composable("register"){
                        CreateAccountScreen(
                            onRegisterSuccess = {
                                navController.navigate("profile") {
                                    popUpTo("auth") {
                                        inclusive = true
                                    }
                                }
                            },
                            onLogin = {
                                navController.popBackStack()
                            }
                        )
                    }

                    //Trang chủ và Radar
                    composable("home") {
                        HomeScreen(
                            onDetail = {
                                navController.navigate("detail")
                            },
                            onMap = {
                                // Kết nối màn hình 7
                            },
                            onFavorites = {
                                // Kết nối màn hình 9
                            },
                            onProfile = {
                                navController.navigate("profile")
                            }
                        )
                    }
                    //Chi tiết hiện vật
                    composable("detail") {
                        PlaceholderScreen(
                            title = "Chi tiết hiện vật",
                            navController = navController,
                            primaryText = "Về trang chủ",
                            primaryRoute = "home",
                            secondaryText = "Hồ sơ cá nhân",
                            secondaryRoute = "profile"
                        )
                    }
                    //Hồ sơ cá nhân
                    composable("profile") {
                        ProfileScreen(
                            onLogout = {
                                navController.navigate("auth") {
                                    popUpTo("profile") {
                                        inclusive = true
                                    }
                                }
                            },
                            onHome = {
                                navController.navigate("home") {
                                    popUpTo("profile") {
                                        inclusive = true
                                    }
                                }
                            },
                            onMap = {
                                // Kết nối màn hình bản đồ sau
                            },
                            onFavorites = {
                                // Kết nối màn hình đã lưu sau
                            }
                        )

                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    navController: NavHostController,
    primaryText: String,
    primaryRoute: String,
    secondaryText: String? = null,
    secondaryRoute: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                navController.navigate(primaryRoute)
            }
        ) {
            Text(text = primaryText)
        }

        if (secondaryText != null && secondaryRoute != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    navController.navigate(secondaryRoute)
                }
            ) {
                Text(text = secondaryText)
            }
        }
    }
}
