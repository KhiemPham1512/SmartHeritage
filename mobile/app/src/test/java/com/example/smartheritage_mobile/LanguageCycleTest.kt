package com.example.smartheritage_mobile

import com.example.smartheritage_mobile.config.LanguageCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageCycleTest {

    @Test
    fun cyclesThroughEnabledLanguagesInOrder() {
        val enabled = listOf("vi", "en", "ko")
        assertEquals("en", LanguageCycle.next("vi", enabled))
        assertEquals("ko", LanguageCycle.next("en", enabled))
        assertEquals("vi", LanguageCycle.next("ko", enabled))
    }

    @Test
    fun unknownOrDisabledCurrentLanguageStartsFromTheFirst() {
        // Admin vừa tắt "fr" ở W8 trong khi máy khách đang chọn "fr"
        assertEquals("vi", LanguageCycle.next("fr", listOf("vi", "en")))
    }

    @Test
    fun emptyListFallsBackToVietnameseAndEnglish() {
        assertEquals("en", LanguageCycle.next("vi", emptyList()))
        assertEquals("vi", LanguageCycle.next("en", emptyList()))
    }

    @Test
    fun singleLanguageStaysTheSame() {
        assertEquals("vi", LanguageCycle.next("vi", listOf("vi")))
    }

    @Test
    fun isCaseInsensitive() {
        assertEquals("en", LanguageCycle.next("VI", listOf("vi", "en")))
    }

    @Test
    fun onlyVietnameseUsesVietnameseUi() {
        assertFalse(LanguageCycle.isEnglishUi("vi"))
        assertFalse(LanguageCycle.isEnglishUi("VI"))
        assertTrue(LanguageCycle.isEnglishUi("en"))
        assertTrue(LanguageCycle.isEnglishUi("ko"))
    }
}
