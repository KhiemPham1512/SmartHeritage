package com.example.smartheritage_mobile

import com.example.smartheritage_mobile.config.ServerAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerAddressTest {

    @Test
    fun ipOnlyGetsSchemeAndDefaultPort() {
        assertEquals("http://192.168.1.5:8000", ServerAddress.normalize("192.168.1.5"))
    }

    @Test
    fun keepsExplicitPort() {
        assertEquals("http://192.168.1.5:9000", ServerAddress.normalize("192.168.1.5:9000"))
    }

    @Test
    fun trimsSpacesTrailingSlashAndPath() {
        assertEquals("http://192.168.1.5:8000", ServerAddress.normalize("  192.168.1.5:8000/  "))
        assertEquals("http://192.168.1.5:8000", ServerAddress.normalize("http://192.168.1.5:8000/api/health-db"))
    }

    @Test
    fun acceptsHostnamesAndHttps() {
        assertEquals("http://pc-khiem:8000", ServerAddress.normalize("pc-khiem"))
    }

    @Test
    fun explicitSchemeWithoutPortUsesTheSchemeDefaultPort() {
        // Triển khai thật qua HTTPS (Caddy/Cloudflare Tunnel) nghe cổng 443, không phải 8000
        assertEquals("https://demo.smartheritage.vn", ServerAddress.normalize("HTTPS://demo.smartheritage.vn"))
        assertEquals("https://demo.smartheritage.vn", ServerAddress.normalize("https://demo.smartheritage.vn/api/x"))
        assertEquals("http://192.168.1.5", ServerAddress.normalize("http://192.168.1.5"))
        assertEquals("https://demo.smartheritage.vn:8443", ServerAddress.normalize("https://demo.smartheritage.vn:8443"))
    }

    @Test
    fun blankMeansAutomatic() {
        assertNull(ServerAddress.normalize(""))
        assertNull(ServerAddress.normalize("   "))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(ServerAddress.normalize("http://"))
        assertNull(ServerAddress.normalize("192.168.1.5:abc"))
        assertNull(ServerAddress.normalize("192.168.1.5:99999"))
        assertNull(ServerAddress.normalize("192.168.1.5:0"))
        assertNull(ServerAddress.normalize("bad host"))
        assertNull(ServerAddress.normalize("-bad"))
    }
}
