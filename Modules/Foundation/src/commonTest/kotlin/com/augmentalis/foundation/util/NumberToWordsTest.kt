/**
 * NumberToWordsTest.kt - Unit tests for NumberToWords
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NumberToWordsTest {

    // -------------------------------------------------------------------------
    // Western system (default)
    // -------------------------------------------------------------------------

    @Test
    fun convert_zeroReturnsZero() {
        assertEquals("zero", NumberToWords.convert(0))
    }

    @Test
    fun convert_singleDigits() {
        assertEquals("one", NumberToWords.convert(1))
        assertEquals("nine", NumberToWords.convert(9))
        assertEquals("thirteen", NumberToWords.convert(13))
    }

    @Test
    fun convert_hundredsAndThousands() {
        assertEquals("one hundred", NumberToWords.convert(100))
        assertEquals("one thousand", NumberToWords.convert(1_000))
        assertEquals("one million", NumberToWords.convert(1_000_000))
    }

    @Test
    fun convert_negativeNumbers() {
        assertEquals("negative one", NumberToWords.convert(-1))
        assertEquals("negative one hundred", NumberToWords.convert(-100L, NumberSystem.WESTERN))
    }

    @Test
    fun convert_compoundNumber() {
        // 1,234 = one thousand two hundred thirty four
        assertEquals("one thousand two hundred thirty four", NumberToWords.convert(1_234))
    }

    // -------------------------------------------------------------------------
    // Indian system
    // -------------------------------------------------------------------------

    @Test
    fun convert_indianSystemLakh() {
        assertEquals("one lakh", NumberToWords.convert(100_000L, NumberSystem.INDIAN))
    }

    @Test
    fun convert_indianSystemCrore() {
        assertEquals("one crore", NumberToWords.convert(10_000_000L, NumberSystem.INDIAN))
    }

    // -------------------------------------------------------------------------
    // East Asian system
    // -------------------------------------------------------------------------

    @Test
    fun convert_eastAsianSystemWan() {
        assertEquals("one wan", NumberToWords.convert(10_000L, NumberSystem.EAST_ASIAN))
    }

    // -------------------------------------------------------------------------
    // Parse round-trip
    // -------------------------------------------------------------------------

    @Test
    fun parse_roundTripForKnownValues() {
        listOf(0L, 1L, 13L, 100L, 1_000L).forEach { n ->
            val words = NumberToWords.convert(n)
            val parsed = NumberToWords.parse(words)
            assertNotNull(parsed, "parse() must not return null for '$words'")
            assertEquals(n, parsed, "Round-trip failed for $n → '$words' → $parsed")
        }
    }

    @Test
    fun parse_returnsNullForGarbage() {
        assertNull(NumberToWords.parse("banana split"))
    }

    // -------------------------------------------------------------------------
    // Suffix / currency helpers
    // -------------------------------------------------------------------------

    @Test
    fun convertWithSuffix_percentSymbol() {
        assertEquals("fifty percent", NumberToWords.convertWithSuffix("50%"))
    }

    @Test
    fun convertCurrency_inrUsesIndianSystem() {
        // 1,00,000 INR = "one lakh indian rupees"
        val result = NumberToWords.convertCurrency(100_000L, "INR")
        assertNotNull(result)
        assertEquals("one lakh indian rupees", result)
    }
}
