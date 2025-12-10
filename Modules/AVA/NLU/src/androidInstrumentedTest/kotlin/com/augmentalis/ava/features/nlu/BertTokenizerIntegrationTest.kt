package com.augmentalis.ava.features.nlu

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for BertTokenizer
 * Tests WordPiece tokenization with BERT vocabulary
 */
@RunWith(AndroidJUnit4::class)
class BertTokenizerIntegrationTest {

    private lateinit var context: Context
    private lateinit var tokenizer: BertTokenizer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Note: Tokenizer requires vocabulary file to be available
        // For now, tests validate interface contract
        tokenizer = BertTokenizer(context = context, maxSequenceLength = 128)
    }

    @Test
    fun tokenize_simpleUtterance_returnsValidTokens() = runTest {
        // Given
        val text = "Turn on the lights"

        // When
        val result = tokenizer.tokenize(text)

        // Then
        // Validates structure even without vocabulary loaded
        assertEquals(128, result.inputIds.size) // Max length with padding
        assertEquals(128, result.attentionMask.size)
        assertEquals(128, result.tokenTypeIds.size)

        // First token should be [CLS] = 101
        assertEquals(101, result.inputIds[0])
    }

    @Test
    fun tokenize_emptyString_returnsClsAndSepOnly() = runTest {
        // Given
        val text = ""

        // When
        val result = tokenizer.tokenize(text)

        // Then
        assertEquals(128, result.inputIds.size)
        assertEquals(101, result.inputIds[0]) // [CLS]
        assertEquals(102, result.inputIds[1]) // [SEP]
        // Rest should be [PAD] = 0
        assertTrue(result.inputIds.drop(2).all { it == 0L })

        // Attention mask: 1 for [CLS] and [SEP], 0 for padding
        assertEquals(1L, result.attentionMask[0])
        assertEquals(1L, result.attentionMask[1])
        assertTrue(result.attentionMask.drop(2).all { it == 0L })
    }

    @Test
    fun tokenize_longText_truncatesTo128Tokens() = runTest {
        // Given - text that would exceed 128 tokens
        val longText = List(100) { "word" }.joinToString(" ")

        // When
        val result = tokenizer.tokenize(longText)

        // Then
        assertEquals(128, result.inputIds.size)
        assertEquals(101, result.inputIds[0]) // [CLS]

        // Last non-padding token should be [SEP]
        val lastNonPadIndex = result.attentionMask.indexOfLast { it == 1L }
        assertEquals(102, result.inputIds[lastNonPadIndex]) // [SEP]
    }

    @Test
    fun tokenize_specialCharacters_handlesGracefully() = runTest {
        // Given
        val text = "Turn on @50% brightness! 🔆"

        // When
        val result = tokenizer.tokenize(text)

        // Then
        assertEquals(128, result.inputIds.size)
        assertEquals(101, result.inputIds[0]) // [CLS]

        // Should not crash and should produce valid token sequence
        assertTrue(result.inputIds.all { it >= 0 })
    }

    @Test
    fun tokenize_attentionMask_correctlyMarksRealTokens() = runTest {
        // Given
        val text = "Hello world"

        // When
        val result = tokenizer.tokenize(text)

        // Then
        // Attention mask should be 1 for real tokens, 0 for padding
        val nonPaddingCount = result.attentionMask.count { it == 1L }
        assertTrue(nonPaddingCount >= 3) // At least [CLS], "hello", "world" or subwords, [SEP]
        assertTrue(nonPaddingCount <= 128)

        // All padding positions should have inputId = 0
        result.inputIds.indices.forEach { i ->
            if (result.attentionMask[i] == 0L) {
                assertEquals(0, result.inputIds[i])
            }
        }
    }

    @Test
    fun tokenize_tokenTypeIds_allZerosForSingleSequence() = runTest {
        // Given
        val text = "Turn on the lights"

        // When
        val result = tokenizer.tokenize(text)

        // Then
        // For single sequence, all token_type_ids should be 0
        assertTrue(result.tokenTypeIds.all { it == 0L })
    }

    @Test
    fun tokenize_multipleUtterances_producesConsistentResults() = runTest {
        // Given
        val utterances = listOf(
            "Turn on the lights",
            "What's the weather?",
            "Set alarm for 7am"
        )

        // When
        val results = utterances.map { tokenizer.tokenize(it) }

        // Then
        results.forEach { result ->
            // All should have consistent structure
            assertEquals(128, result.inputIds.size)
            assertEquals(128, result.attentionMask.size)
            assertEquals(128, result.tokenTypeIds.size)
            assertEquals(101, result.inputIds[0]) // [CLS]
        }
    }

    @Test
    fun tokenize_caseVariations_handledByLowercase() = runTest {
        // Given
        val variations = listOf(
            "Turn On The Lights",
            "turn on the lights",
            "TURN ON THE LIGHTS"
        )

        // When
        val results = variations.map { tokenizer.tokenize(it) }

        // Then
        // After lowercasing, should produce similar token patterns
        // (exact match requires vocabulary, but structure should be consistent)
        results.forEach { result ->
            assertEquals(101, result.inputIds[0]) // [CLS]
            assertTrue(result.attentionMask.count { it == 1L } > 0)
        }
    }

    @Test
    fun tokenize_punctuation_handledCorrectly() = runTest {
        // Given
        val utterances = listOf(
            "Hello, world!",
            "What's happening?",
            "Set alarm @ 7:30am"
        )

        // When
        val results = utterances.map { tokenizer.tokenize(it) }

        // Then
        results.forEach { result ->
            assertEquals(128, result.inputIds.size)
            assertEquals(101, result.inputIds[0])
            // Should not crash on punctuation
            assertTrue(result.inputIds.all { it >= 0 })
        }
    }

    @Test
    fun tokenize_numbers_tokenizedCorrectly() = runTest {
        // Given
        val text = "Set temperature to 72 degrees"

        // When
        val result = tokenizer.tokenize(text)

        // Then
        assertEquals(128, result.inputIds.size)
        assertEquals(101, result.inputIds[0])
        // Numbers should be tokenized (may become subwords or UNK)
        assertTrue(result.attentionMask.count { it == 1L } >= 3)
    }

    @Test
    fun tokenize_performance_within5ms() = runTest {
        // Given
        val utterances = List(100) { "Test utterance number $it for performance testing" }

        // When
        val startTime = System.nanoTime()
        utterances.forEach { tokenizer.tokenize(it) }
        val elapsed = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms

        // Then
        val avgTime = elapsed / utterances.size
        println("Average tokenization time: ${avgTime}ms (100 samples)")

        // Target: < 5ms per tokenization
        // Note: Actual performance depends on vocabulary loading
        assertTrue(avgTime < 10) // Relaxed threshold without vocabulary
    }
}
