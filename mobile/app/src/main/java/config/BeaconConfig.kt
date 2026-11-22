package com.example.smartheritage_mobile.config

import com.example.smartheritage_mobile.R
import com.example.smartheritage_mobile.data.remote.RetrofitClient

object BeaconConfig {
    const val GROUP_UUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
    const val DEFAULT_MAJOR = 1
    const val MAX_DISTANCE_METERS = 2.0 // bán kính mặc định khi server chưa cấu hình trigger_distance
    const val SCAN_MAX_DISTANCE_METERS = 5.0 // chỉ gọi resolve khi beacon trong phạm vi này (bán kính lớn nhất ở W5)
    const val COOLDOWN_MS = 300_000L // 5 phút (300,000ms)
    const val IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    val TEST_MINORS = listOf(101, 102, 103, 104, 105)
}

/** Quyết định khi nào một beacon được thử kích hoạt: mỗi beacon có bán kính riêng do Admin đặt ở W5 (trigger_distance). */
object TriggerPolicy {
    const val RETRY_AFTER_NOT_FOUND_MS = 30_000L // beacon bị khóa/mất tín hiệu/chưa gắn hiện vật: đừng hỏi server mỗi giây
    const val RETRY_AFTER_ERROR_MS = 5_000L

    fun radiusFor(serverRadius: Double?): Double =
        if (serverRadius != null && serverRadius > 0.0) serverRadius else BeaconConfig.MAX_DISTANCE_METERS

    /** knownRadius = null: chưa resolve beacon này lần nào → dùng giới hạn quét tối đa để biết bán kính của nó. */
    fun isCandidate(distance: Double, knownRadius: Double?): Boolean =
        distance <= (knownRadius ?: BeaconConfig.SCAN_MAX_DISTANCE_METERS)
}

/**
 * Hàm extension ghép đường dẫn tương đối từ Backend (ví dụ /static/audio/ngoc-lu-thuyet-minh-vi.mp3)
 * với BASE_URL của Retrofit để Coil và ExoPlayer tải thành công file media.
 */
fun String?.toFullMediaUrl(): String? {
    if (this.isNullOrBlank()) return null
    if (this.startsWith("http://") || this.startsWith("https://")) {
        return this
    }
    val baseUrl = RetrofitClient.currentBaseUrl().removeSuffix("/")
    val relativePath = if (this.startsWith("/")) this else "/$this"
    return "$baseUrl$relativePath"
}

/**
 * Trả về đúng ảnh chụp thật của từng hiện vật theo Tên hiện vật, Mã ảnh hoặc ID hiện vật.
 */
fun resolveArtifactDrawableRes(
    imageUrl: String? = null,
    artifactName: String? = null,
    artifactId: Int? = null
): Int {
    val nameLower = artifactName?.lowercase().orEmpty()
    val urlLower = imageUrl?.lowercase().orEmpty()

    return when {
        nameLower.contains("ngọc lũ") || nameLower.contains("ngoc lu") || nameLower.contains("trống đồng") || nameLower.contains("bronze drum") ->
            R.drawable.img_trong_dong_ngoc_lu

        nameLower.contains("chu đậu") || nameLower.contains("chu dau") || nameLower.contains("bình gốm") || nameLower.contains("ceramic") ->
            R.drawable.img_binh_gom_chu_dau

        nameLower.contains("phật tích") || nameLower.contains("phat tich") || nameLower.contains("tượng phật") ->
            R.drawable.img_tuong_phat_tich

        nameLower.contains("apsara") || nameLower.contains("chăm pa") || nameLower.contains("champa") ->
            R.drawable.img_tuong_apsara

        nameLower.contains("đào thịnh") || nameLower.contains("dao thinh") || nameLower.contains("thạp đồng") || nameLower.contains("bronze jar") ->
            R.drawable.img_thap_dong_dao_thinh

        nameLower.contains("ấn vàng") || nameLower.contains("hoàng đế") || nameLower.contains("golden seal") || nameLower.contains("triều nguyễn") ->
            R.drawable.img_an_vang_trieu_nguyen

        nameLower.contains("cửu đỉnh") || nameLower.contains("cuu dinh") || nameLower.contains("nine dynastic") ->
            R.drawable.img_cuu_dinh_hue

        urlLower.endsWith("hv_001.jpg") -> R.drawable.img_trong_dong_ngoc_lu
        urlLower.endsWith("hv_002.jpg") -> R.drawable.img_binh_gom_chu_dau
        urlLower.endsWith("hv_003.jpg") -> R.drawable.img_tuong_apsara
        urlLower.endsWith("hv_004.jpg") -> R.drawable.img_thap_dong_dao_thinh
        urlLower.endsWith("hv_005.jpg") -> R.drawable.img_an_vang_trieu_nguyen
        urlLower.endsWith("hv_006.jpg") -> R.drawable.img_cuu_dinh_hue

        artifactId == 1 -> R.drawable.img_trong_dong_ngoc_lu
        artifactId == 2 -> R.drawable.img_binh_gom_chu_dau
        artifactId == 3 -> R.drawable.img_tuong_apsara
        artifactId == 4 -> R.drawable.img_thap_dong_dao_thinh
        artifactId == 5 -> R.drawable.img_an_vang_trieu_nguyen
        artifactId == 6 -> R.drawable.img_cuu_dinh_hue

        else -> R.drawable.img_trong_dong_ngoc_lu
    }
}

/**
 * Nếu ảnh từ Backend là bộ 5 file mẫu hv_001..hv_005 (vốn là ảnh màu đơn sắc do seed_media.py tạo)
 * hoặc đang chạy offline, tự động hiển thị ảnh chụp hiện vật thật sắc nét đã tích hợp.
 * Nếu Admin tải lên ảnh mới qua Web W4 (ví dụ /static/images/media_xxx.jpg) thì tải từ URL Server.
 */
fun resolveArtifactImageModel(
    imageUrl: String? = null,
    artifactName: String? = null,
    artifactId: Int? = null
): Any {
    val urlLower = imageUrl?.lowercase().orEmpty()
    val isDefaultSeedImage = urlLower.isBlank() ||
            urlLower.endsWith("hv_001.jpg") ||
            urlLower.endsWith("hv_002.jpg") ||
            urlLower.endsWith("hv_003.jpg") ||
            urlLower.endsWith("hv_004.jpg") ||
            urlLower.endsWith("hv_005.jpg") ||
            urlLower.endsWith("hv_006.jpg")

    if (!isDefaultSeedImage) {
        return imageUrl.toFullMediaUrl() ?: resolveArtifactDrawableRes(imageUrl, artifactName, artifactId)
    }
    return resolveArtifactDrawableRes(imageUrl, artifactName, artifactId)
}
