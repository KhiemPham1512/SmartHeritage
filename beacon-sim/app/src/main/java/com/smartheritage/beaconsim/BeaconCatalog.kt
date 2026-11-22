package com.smartheritage.beaconsim

import android.bluetooth.le.AdvertiseSettings
import org.json.JSONArray

/** Một trạm iBeacon cần giả lập — khớp phần tử của GET /api/beacons/public. */
data class SimBeacon(
    val code: String,
    val uuid: String,
    val major: Int,
    val minor: Int,
    val txPower: Int,
    val triggerDistance: Double,
    val status: String,
    val artifactName: String?,
) {
    /** App khách chỉ resolve beacon ACTIVE/LOW_BATTERY đã gắn hiện vật. */
    val willResolve: Boolean get() = status != "LOST_SIGNAL" && artifactName != null
}

/** Mức công suất phát: phát yếu ≈ khách đứng xa, phát mạnh ≈ khách đứng sát hiện vật. */
enum class PowerLevel(val label: String, val txPowerLevel: Int) {
    LOW("Thấp (xa)", AdvertiseSettings.ADVERTISE_TX_POWER_LOW),
    MEDIUM("Trung bình", AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM),
    HIGH("Cao (gần)", AdvertiseSettings.ADVERTISE_TX_POWER_HIGH),
}

object BeaconCatalog {
    const val GROUP_UUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
    const val IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    const val PUBLIC_PATH = "/api/beacons/public"

    /** Dùng khi không gọi được server — khớp dữ liệu mẫu trong database/init_schema.sql. */
    val seed: List<SimBeacon> = listOf(
        SimBeacon("BC-001", GROUP_UUID, 1, 101, -59, 2.0, "ACTIVE", "Trống đồng Ngọc Lũ"),
        SimBeacon("BC-002", GROUP_UUID, 1, 102, -59, 2.5, "ACTIVE", "Bình gốm Chu Đậu"),
        SimBeacon("BC-003", GROUP_UUID, 1, 103, -59, 1.5, "LOW_BATTERY", "Tượng Apsara"),
        SimBeacon("BC-004", GROUP_UUID, 1, 104, -59, 2.0, "LOST_SIGNAL", "Thạp đồng Đào Thịnh"),
        SimBeacon("BC-005", GROUP_UUID, 1, 105, -59, 2.0, "ACTIVE", "Ấn vàng Triều Nguyễn"),
    )

    fun parse(json: String): List<SimBeacon> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SimBeacon(
                code = o.getString("beacon_code"),
                uuid = o.getString("uuid"),
                major = o.getInt("major"),
                minor = o.getInt("minor"),
                txPower = o.optInt("tx_power", -59),
                triggerDistance = o.optDouble("trigger_distance", 2.0),
                status = o.optString("status", "ACTIVE"),
                artifactName = if (o.isNull("artifact_name_vi")) null else o.getString("artifact_name_vi"),
            )
        }
    }

    /** "192.168.1.5:8000" → "http://192.168.1.5:8000/api/beacons/public" */
    fun endpoint(server: String): String {
        val base = server.trim().trimEnd('/')
        val withScheme = if (base.startsWith("http://") || base.startsWith("https://")) base else "http://$base"
        return withScheme + PUBLIC_PATH
    }
}
