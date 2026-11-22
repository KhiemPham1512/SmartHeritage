package com.example.smartheritage_mobile.data.remote // Giữ nguyên dòng package hiện tại của TV2

import com.example.smartheritage_mobile.config.ServerAddress
import com.example.smartheritage_mobile.data.local.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Mặc định (tự động): 127.0.0.1 (qua adb reverse), dự phòng 10.0.2.2 (mạng máy ảo).
    // Người dùng có thể nhập IP máy tính chạy backend (Session.serverAddress) để dùng qua Wi-Fi.
    const val PRIMARY_URL = "http://127.0.0.1:8000/"
    private const val FALLBACK_HOST = "10.0.2.2"

    /** Gốc URL đang dùng cho API và media (/static/...), có dấu "/" ở cuối. */
    fun currentBaseUrl(): String =
        (ServerAddress.normalize(Session.serverAddress) ?: PRIMARY_URL.trimEnd('/')) + "/"

    private val smartFailoverInterceptor = Interceptor { chain ->
        var request = chain.request()
        val custom = ServerAddress.normalize(Session.serverAddress)?.toHttpUrlOrNull()
        if (custom != null) {
            request = request.newBuilder()
                .url(request.url.newBuilder().scheme(custom.scheme).host(custom.host).port(custom.port).build())
                .build()
        }
        // Đã đăng nhập: gửi token để server biết danh tính (lịch sử/yêu thích theo tài khoản)
        Session.token?.let { request = request.newBuilder().header("Authorization", "Bearer $it").build() }

        try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (custom != null) throw e // đã chọn máy chủ cụ thể: không tự đổi sang địa chỉ khác
            // Máy chưa chạy adb reverse: tự động chuyển sang 10.0.2.2:8000
            chain.proceed(request.newBuilder().url(request.url.newBuilder().host(FALLBACK_HOST).build()).build())
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .addInterceptor(smartFailoverInterceptor)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Client riêng để thử một địa chỉ CHƯA lưu (không đi qua interceptor ghi đè máy chủ)
    private val pingClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    /** true nếu backend ở địa chỉ này trả lời và kết nối được SQL Server (/api/health-db). null = chế độ tự động. */
    suspend fun ping(address: String?): Boolean = withContext(Dispatchers.IO) {
        val candidates = if (address != null) listOf(address) else listOf("http://127.0.0.1:8000", "http://$FALLBACK_HOST:8000")
        candidates.any { base ->
            try {
                pingClient.newCall(Request.Builder().url("$base/api/health-db").build()).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }
    }

    val artifactApi: ArtifactApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PRIMARY_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ArtifactApiService::class.java)
    }
}
