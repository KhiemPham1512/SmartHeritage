package com.example.smartheritage_mobile

import com.example.smartheritage_mobile.config.BeaconConfig
import com.example.smartheritage_mobile.config.TriggerPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerPolicyTest {

    @Test
    fun radiusFallsBackToDefaultWhenServerGivesNothingUsable() {
        assertEquals(BeaconConfig.MAX_DISTANCE_METERS, TriggerPolicy.radiusFor(null), 0.0)
        assertEquals(BeaconConfig.MAX_DISTANCE_METERS, TriggerPolicy.radiusFor(0.0), 0.0)
        assertEquals(BeaconConfig.MAX_DISTANCE_METERS, TriggerPolicy.radiusFor(-1.0), 0.0)
    }

    @Test
    fun radiusUsesServerValue() {
        assertEquals(2.5, TriggerPolicy.radiusFor(2.5), 0.0)
        assertEquals(1.5, TriggerPolicy.radiusFor(1.5), 0.0)
    }

    @Test
    fun unknownBeaconIsCandidateWithinScanLimit() {
        assertTrue(TriggerPolicy.isCandidate(4.9, null))
        assertTrue(TriggerPolicy.isCandidate(BeaconConfig.SCAN_MAX_DISTANCE_METERS, null))
        assertFalse(TriggerPolicy.isCandidate(5.1, null))
    }

    @Test
    fun knownBeaconUsesItsOwnRadius() {
        assertTrue(TriggerPolicy.isCandidate(2.4, 2.5))   // BC-002 có bán kính 2,5 m
        assertFalse(TriggerPolicy.isCandidate(2.6, 2.5))
        assertFalse(TriggerPolicy.isCandidate(1.8, 1.5))  // BC-003 có bán kính 1,5 m: 1,8 m là chưa đủ gần
        assertTrue(TriggerPolicy.isCandidate(1.5, 1.5))
    }
}
