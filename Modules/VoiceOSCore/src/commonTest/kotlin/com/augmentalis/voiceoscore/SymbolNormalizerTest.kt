/**
 * SymbolNormalizerTest.kt - Unit tests for SymbolNormalizer
 *
 * Tests symbol-to-speech normalization and bidirectional matching
 * for voice command recognition.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-20
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.utils.SymbolNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SymbolNormalizerTest {

    // ==================== containsSymbols Tests ====================

    @Test
    fun containsSymbols_returnsTrueForAmpersand() {
        assertTrue(SymbolNormalizer.containsSymbols("Sound & vibration"))
    }

    @Test
    fun containsSymbols_returnsTrueForHash() {
        assertTrue(SymbolNormalizer.containsSymbols("Item #5"))
    }

    @Test
    fun containsSymbols_returnsTrueForPercent() {
        assertTrue(SymbolNormalizer.containsSymbols("100%"))
    }

    @Test
    fun containsSymbols_returnsFalseForPlainText() {
        assertFalse(SymbolNormalizer.containsSymbols("Plain text without symbols"))
    }

    @Test
    fun containsSymbols_returnsFalseForEmptyString() {
        assertFalse(SymbolNormalizer.containsSymbols(""))
    }

    // ==================== normalize Tests ====================

    @Test
    fun normalize_convertsAmpersandToAnd() {
        val result = SymbolNormalizer.normalize("Sound & vibration")
        assertEquals("Sound and vibration", result)
    }

    @Test
    fun normalize_convertsHashToNumber() {
        val result = SymbolNormalizer.normalize("Item #5")
        // First alias for # is "pound"
        assertTrue(result.contains("pound") || result.contains("number") || result.contains("hash"))
    }

    @Test
    fun normalize_convertsPercentToPercent() {
        val result = SymbolNormalizer.normalize("100%")
        assertTrue(result.contains("percent"))
    }

    @Test
    fun normalize_handlesMultipleSymbols() {
        val result = SymbolNormalizer.normalize("A & B + C")
        assertTrue(result.contains("and"))
        assertTrue(result.contains("plus"))
    }

    @Test
    fun normalize_preservesTextWithoutSymbols() {
        val original = "No symbols here"
        assertEquals(original, SymbolNormalizer.normalize(original))
    }

    @Test
    fun normalize_cleansUpMultipleSpaces() {
        val result = SymbolNormalizer.normalize("A  &  B")
        assertFalse(result.contains("  ")) // No double spaces
    }

    // ==================== matchWithAliases Tests ====================

    @Test
    fun matchWithAliases_matchesAndWithAmpersand() {
        assertTrue(
            SymbolNormalizer.matchWithAliases(
                voiceInput = "sound and vibration",
                phrase = "Sound & vibration"
            )
        )
    }

    @Test
    fun matchWithAliases_matchesPoundWithHash() {
        assertTrue(
            SymbolNormalizer.matchWithAliases(
                voiceInput = "item pound 5",
                phrase = "Item #5"
            )
        )
    }

    @Test
    fun matchWithAliases_matchesPercentWithPercent() {
        assertTrue(
            SymbolNormalizer.matchWithAliases(
                voiceInput = "100 percent",
                phrase = "100%"
            )
        )
    }

    @Test
    fun matchWithAliases_caseInsensitive() {
        assertTrue(
            SymbolNormalizer.matchWithAliases(
                voiceInput = "SOUND AND VIBRATION",
                phrase = "Sound & Vibration"
            )
        )
    }

    @Test
    fun matchWithAliases_failsForNonMatchingInput() {
        assertFalse(
            SymbolNormalizer.matchWithAliases(
                voiceInput = "completely different",
                phrase = "Sound & vibration"
            )
        )
    }

    // ==================== getAliases Tests ====================

    @Test
    fun getAliases_returnsAliasesForAmpersand() {
        val aliases = SymbolNormalizer.getAliases("&")
        assertTrue(aliases.contains("and"))
    }

    @Test
    fun getAliases_returnsAliasesForHash() {
        val aliases = SymbolNormalizer.getAliases("#")
        assertTrue(aliases.isNotEmpty())
        // Should contain at least one of: pound, hash, number
        assertTrue(
            aliases.any { it in listOf("pound", "hash", "number", "hashtag") }
        )
    }

    @Test
    fun getAliases_returnsEmptyForUnknownSymbol() {
        val aliases = SymbolNormalizer.getAliases("§") // Not in mappings
        assertTrue(aliases.isEmpty())
    }

    // ==================== matchesAlias Tests ====================

    @Test
    fun matchesAlias_matchesAndForAmpersand() {
        assertTrue(SymbolNormalizer.matchesAlias("and", "&"))
    }

    @Test
    fun matchesAlias_matchesPoundForHash() {
        assertTrue(SymbolNormalizer.matchesAlias("pound", "#"))
    }

    @Test
    fun matchesAlias_isCaseInsensitive() {
        assertTrue(SymbolNormalizer.matchesAlias("AND", "&"))
    }

    @Test
    fun matchesAlias_failsForNonAlias() {
        assertFalse(SymbolNormalizer.matchesAlias("xyz", "&"))
    }

    // ==================== generatePhraseVariations Tests ====================

    @Test
    fun generatePhraseVariations_generatesMultipleVariations() {
        val variations = SymbolNormalizer.generatePhraseVariations("Sound & vibration")
        assertTrue(variations.size >= 2)
        assertTrue(variations.any { it.contains("and") })
    }

    @Test
    fun generatePhraseVariations_includesOriginal() {
        val variations = SymbolNormalizer.generatePhraseVariations("Sound & vibration")
        assertTrue(variations.any { it.contains("&") })
    }

    // ==================== findSymbolForSpoken Tests ====================

    @Test
    fun findSymbolForSpoken_findsAmpersandForAnd() {
        val symbol = SymbolNormalizer.findSymbolForSpoken("and")
        assertEquals("&", symbol)
    }

    @Test
    fun findSymbolForSpoken_findsAmpersandForAmpersand() {
        val symbol = SymbolNormalizer.findSymbolForSpoken("ampersand")
        assertEquals("&", symbol)
    }

    @Test
    fun findSymbolForSpoken_findsPlusForPlus() {
        val symbol = SymbolNormalizer.findSymbolForSpoken("plus")
        assertEquals("+", symbol)
    }

    @Test
    fun findSymbolForSpoken_findsAtForAt() {
        val symbol = SymbolNormalizer.findSymbolForSpoken("at")
        assertEquals("@", symbol)
    }

    @Test
    fun findSymbolForSpoken_returnsNullForUnknown() {
        val symbol = SymbolNormalizer.findSymbolForSpoken("xyz")
        assertEquals(null, symbol)
    }

    // ==================== Edge Cases ====================

    @Test
    fun normalize_handlesCurrencySymbols() {
        val result = SymbolNormalizer.normalize("$100")
        assertTrue(result.contains("dollar"))
    }

    @Test
    fun normalize_handlesEmptyString() {
        assertEquals("", SymbolNormalizer.normalize(""))
    }

    @Test
    fun matchWithAliases_handlesEmptyStrings() {
        assertTrue(SymbolNormalizer.matchWithAliases("", ""))
    }
}
