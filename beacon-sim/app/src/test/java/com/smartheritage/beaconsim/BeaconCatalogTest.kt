package com.smartheritage.beaconsim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeaconCatalogTest {

    @Test
    fun parsesPublicBeaconResponse() {
        val json = """
            [{"beacon_code":"BC-002","uuid":"FDA50693-A4E2-4FB1-AFCF-C6EB07647825","major":1,"minor":102,
              "tx_power":-59,"trigger_distance":2.5,"status":"ACTIVE","artifact_code":"HV-002",
              "artifact_name_vi":"Bình gốm Chu Đậu","artifact_name_en":"Chu Dau Ceramic Vase"},
             {"beacon_code":"BC-009","uuid":"FDA50693-A4E2-4FB1-AFCF-C6EB07647825","major":1,"minor":109,
              "tx_power":-60,"trigger_distance":2.0,"status":"ACTIVE","artifact_code":null,
              "artifact_name_vi":null,"artifact_name_en":null}]
        """.trimIndent()

        val list = BeaconCatalog.parse(json)

        assertEquals(2, list.size)
        assertEquals(102, list[0].minor)
        assertEquals(2.5, list[0].triggerDistance, 0.0)
        assertEquals("Bình gốm Chu Đậu", list[0].artifactName)
        assertTrue(list[0].willResolve)
        assertNull(list[1].artifactName)
        assertFalse(list[1].willResolve) // chưa gắn hiện vật → app khách không bật popup
    }

    @Test
    fun lostSignalBeaconWillNotResolve() {
        assertFalse(BeaconCatalog.seed.first { it.status == "LOST_SIGNAL" }.willResolve)
    }

    @Test
    fun seedMatchesDatabaseMinors() {
        assertEquals(listOf(101, 102, 103, 104, 105), BeaconCatalog.seed.map { it.minor })
    }

    @Test
    fun buildsEndpointFromLooseInput() {
        assertEquals("http://192.168.1.5:8000/api/beacons/public", BeaconCatalog.endpoint(" 192.168.1.5:8000/ "))
        assertEquals("https://demo.vn/api/beacons/public", BeaconCatalog.endpoint("https://demo.vn"))
    }
}
