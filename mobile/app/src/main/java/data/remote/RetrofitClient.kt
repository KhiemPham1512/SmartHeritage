package com.example.smartheritage_mobile.data.remote // Giữ nguyên dòng package hiện tại của TV2

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Ưu tiên 127.0.0.1 (qua adb reverse) và tự động dự phòng sang 10.0.2.2 (mạng máy ảo)
    private const val PRIMARY_URL = "http://127.0.0.1:8000/"
    private const val FALLBACK_HOST = "10.0.2.2"

    private val smartFailoverInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        try {
            // Thử kết nối qua 127.0.0.1:8000 trước
            chain.proceed(originalRequest)
        } catch (e: IOException) {
            // Nếu máy chưa chạy adb reverse, tự động chuyển sang 10.0.2.2:8000
            val fallbackUrl = originalRequest.url.newBuilder()
                .host(FALLBACK_HOST)
                .build()
            val fallbackRequest = originalRequest.newBuilder()
                .url(fallbackUrl)
                .build()
            chain.proceed(fallbackRequest)
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .addInterceptor(smartFailoverInterceptor)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val artifactApi: ArtifactApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PRIMARY_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ArtifactApiService::class.java)
    }
}