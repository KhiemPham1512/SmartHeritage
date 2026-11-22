package com.example.smartheritage_mobile.model

import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import retrofit2.Response

data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequestDto(
    @SerializedName("full_name") val full_name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("preferred_lang") val preferred_lang: String
)

data class AuthUserDto(
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val full_name: String,
    @SerializedName("role") val role: String?,
    @SerializedName("preferred_lang") val preferred_lang: String?,
    @SerializedName("auto_beacon_notify") val auto_beacon_notify: Boolean?
)

data class AuthResponseDto(
    @SerializedName("access_token") val access_token: String,
    @SerializedName("user") val user: AuthUserDto
)

data class LanguageDto(
    @SerializedName("lang_code") val lang_code: String,
    @SerializedName("name_native") val name_native: String,
    @SerializedName("is_enabled") val is_enabled: Boolean
)

/** Gson bỏ qua trường null → chỉ gửi những trường cần đổi (server dùng COALESCE). */
data class ProfileUpdateDto(
    @SerializedName("preferred_lang") val preferred_lang: String? = null,
    @SerializedName("auto_beacon_notify") val auto_beacon_notify: Boolean? = null
)

/** Lấy thông báo lỗi tiếng Việt của FastAPI ({"detail": "..."}); null nếu không có. */
fun Response<*>.errorDetail(): String? = try {
    val raw = errorBody()?.string().orEmpty()
    val detail = JsonParser.parseString(raw).asJsonObject.get("detail")
    if (detail != null && detail.isJsonPrimitive) detail.asString else null
} catch (e: Exception) {
    null
}
