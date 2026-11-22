plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.smartheritage.beaconsim"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Khác app khách (com.example.smartheritage_mobile) để cài song song trên cùng máy nếu cần
        applicationId = "com.smartheritage.beaconsim"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    // Phát iBeacon (cùng thư viện app khách dùng để quét)
    implementation("org.altbeacon:android-beacon-library:2.21.2")

    testImplementation(libs.junit)
    // org.json trong android.jar chỉ là stub khi chạy unit test JVM
    testImplementation("org.json:json:20240303")
}
