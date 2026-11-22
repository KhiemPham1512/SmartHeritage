package com.example.smartheritage_mobile.config

/** Chuẩn hóa địa chỉ máy chủ người dùng nhập (thuần Kotlin để kiểm thử bằng JUnit). */
object ServerAddress {
    const val DEFAULT_PORT = 8000

    private val SCHEME = Regex("^(https?)://", RegexOption.IGNORE_CASE)
    private val HOST = Regex("^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$")

    /**
     * Không ghi scheme/cổng (mạng LAN): "192.168.1.5" → "http://192.168.1.5:8000"; "pc-khiem:9000/" → "http://pc-khiem:9000".
     * Có ghi scheme mà không ghi cổng (triển khai thật): "https://demo.vn" → "https://demo.vn" (cổng 443).
     * Rỗng hoặc không hợp lệ → null (null = chế độ tự động).
     */
    fun normalize(input: String): String? {
        var s = input.trim()
        if (s.isEmpty()) return null

        // Tách scheme TRƯỚC khi cắt dấu "/" cuối (nếu không "http://" sẽ thành "http:" và bị hiểu là tên máy)
        var scheme = "http"
        var explicitScheme = false
        SCHEME.find(s)?.let {
            scheme = it.groupValues[1].lowercase()
            explicitScheme = true
            s = s.substring(it.value.length)
        }

        val hostPort = s.substringBefore('/')
        val host = hostPort.substringBefore(':')
        val portText = if (':' in hostPort) hostPort.substringAfter(':') else ""
        if (!HOST.matches(host)) return null

        val port: Int? = when {
            portText.isNotEmpty() -> portText.toIntOrNull() ?: return null
            explicitScheme -> null // cổng mặc định của scheme (80/443)
            else -> DEFAULT_PORT
        }
        if (port != null && port !in 1..65535) return null
        return if (port == null) "$scheme://$host" else "$scheme://$host:$port"
    }
}
