package com.example.smartheritage_mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.smartheritage_mobile.model.AuthResponseDto
import java.util.UUID

/**
 * Phiên đăng nhập, định danh thiết bị và địa chỉ máy chủ (SharedPreferences).
 * Gọi [init] một lần ở MainActivity trước khi dựng giao diện.
 *
 * - deviceId: mã ngẫu nhiên riêng cho từng máy, tạo một lần → khách (Guest) không còn dùng chung "android-guest-01".
 * - token/userId: có khi đã đăng nhập; server lấy danh tính từ token chứ không tin user_id do app gửi.
 * - serverAddress: "" = tự động (127.0.0.1 qua adb reverse, dự phòng 10.0.2.2 trên máy ảo).
 */
object Session {
    private const val PREFS = "smart_heritage_session"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    val deviceId: String
        get() {
            val sp = prefs ?: return "android-unknown"
            return sp.getString("device_id", null)
                ?: ("android-" + UUID.randomUUID().toString().take(8)).also {
                    sp.edit().putString("device_id", it).apply()
                }
        }

    val token: String? get() = prefs?.getString("token", null)
    val userId: Int? get() = prefs?.getInt("user_id", 0)?.takeIf { it > 0 }
    val fullName: String? get() = prefs?.getString("full_name", null)
    val email: String? get() = prefs?.getString("email", null)
    val isLoggedIn: Boolean get() = token != null && userId != null

    var serverAddress: String
        get() = prefs?.getString("server", "") ?: ""
        set(value) {
            prefs?.edit()?.putString("server", value)?.apply()
        }

    fun saveLogin(res: AuthResponseDto) {
        prefs?.edit()
            ?.putString("token", res.access_token)
            ?.putInt("user_id", res.user.user_id)
            ?.putString("full_name", res.user.full_name)
            ?.putString("email", res.user.email)
            ?.apply()
    }

    /** Đăng xuất: xóa tài khoản nhưng giữ deviceId và địa chỉ máy chủ. */
    fun logout() {
        prefs?.edit()
            ?.remove("token")
            ?.remove("user_id")
            ?.remove("full_name")
            ?.remove("email")
            ?.apply()
    }
}
