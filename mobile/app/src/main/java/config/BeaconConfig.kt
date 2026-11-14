package com.example.smartheritage_mobile.config

import com.example.smartheritage_mobile.data.remote.RetrofitClient

object BeaconConfig {
    const val GROUP_UUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
    const val DEFAULT_MAJOR = 1
    const val MAX_DISTANCE_METERS = 2.0
    const val COOLDOWN_MS = 300_000L // 5 phút (300,000ms)
    const val IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    val TEST_MINORS = listOf(101, 102, 103, 104, 105)
}

/**
 * Hàm extension ghép đường dẫn tương đối từ Backend (ví dụ /static/images/hv_001.jpg)
 * với BASE_URL của Retrofit để Coil và ExoPlayer tải thành công file media.
 */
fun String?.toFullMediaUrl(): String? {
    if (this.isNullOrBlank()) return null
    if (this.startsWith("http://") || this.startsWith("https://")) {
        return this
    }
    val baseUrl = RetrofitClient.PRIMARY_URL.removeSuffix("/")
    val relativePath = if (this.startsWith("/")) this else "/$this"
    return "$baseUrl$relativePath"
}
