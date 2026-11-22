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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartheritage_mobile.data.local.Session

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Session.init(applicationContext)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = if (Session.isLoggedIn) "home" else "language"
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
                                navController.navigate("home") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            },
                            onRegister = {
                                navController.navigate("register")
                            },
                            onGuest = {
                                navController.navigate("home")
                            },
                            onBeaconStation = {
                                navController.navigate("beacon_simulator")
                            }
                        )
                    }

                    //MÀn hình 3
                    composable("register"){
                        CreateAccountScreen(
                            onRegisterSuccess = {
                                navController.navigate("home") {
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
                            onDetail = { artifactId ->
                                navController.navigate("detail/$artifactId")
                            },
                            onMap = {
                                navController.navigate("map")
                            },
                            onFavorites = {
                                navController.navigate("favorites")
                            },
                            onProfile = {
                                navController.navigate("profile")
                            }
                        )
                    }
                    //Chi tiết hiện vật (M5 - Màn hình 6 + Hộp thoại M6 Màn hình 8)
                    composable(
                        route = "detail/{artifactId}",
                        arguments = listOf(navArgument("artifactId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val artifactId = requireNotNull(backStackEntry.arguments).getInt("artifactId")
                        ArtifactDetailScreen(
                            artifactId = artifactId,
                            onBack = {
                                if (!navController.popBackStack()) {
                                    navController.navigate("home")
                                }
                            }
                        )
                    }
                    //Bản đồ di tích (M5 - Màn hình 7)
                    composable("map") {
                        SiteMapScreen(
                            onBack = {
                                if (!navController.popBackStack()) {
                                    navController.navigate("home")
                                }
                            },
                            onHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onFavorites = {
                                navController.navigate("favorites")
                            },
                            onProfile = {
                                navController.navigate("profile")
                            },
                            onArtifactDetail = { artifactId ->
                                navController.navigate("detail/$artifactId")
                            }
                        )
                    }
                    //Đã lưu & Lịch sử tham quan (M6 - Màn hình 9)
                    composable("favorites") {
                        SavedAndHistoryScreen(
                            onHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onMap = {
                                navController.navigate("map")
                            },
                            onProfile = {
                                navController.navigate("profile")
                            },
                            onArtifactDetail = { artifactId ->
                                navController.navigate("detail/$artifactId")
                            }
                        )
                    }
                    //Hồ sơ cá nhân
                    composable("profile") {
                        ProfileScreen(
                            onLogout = {
                                navController.navigate("auth") {
                                    popUpTo(navController.graph.id) { inclusive = true }
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
                                navController.navigate("map")
                            },
                            onFavorites = {
                                navController.navigate("favorites")
                            },
                            onSimulator = {
                                navController.navigate("beacon_simulator")
                            }
                        )
                    }

                    // Màn hình giả lập iBeacon (Test / Demo M3/M4 & M8)
                    composable("beacon_simulator") {
                        BeaconSimulatorScreen(
                            onBack = {
                                navController.popBackStack()
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
