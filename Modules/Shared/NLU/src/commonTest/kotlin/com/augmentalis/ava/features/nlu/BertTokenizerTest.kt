package com.augmentalis.ava.features.nlu

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * DEFEND Phase: BertTokenizer tests
 * Validates tokenization logic
 */
@RunWith(RobolectricTestRunner::class)
class BertTokenizerTest {

    private lateinit var context: Context
    private lateinit var tokenizer: BertTokenizer

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        tokenizer = BertTokenizer(context)
    }

    @Test
    fun `tokenize should add CLS and SEP tokens`() {
        // Given
        val text = "hello world"

        // When
        val result = tokenizer.tokenize(text)

        // Then
        // First token should be [CLS] (ID 101)
        assertEquals(101, result.inputIds[0])

        // Last non-padded token should be [SEP] (ID 102)
        val firstPadIndex = result.attentionMask.indexOf(0)
        if (firstPadIndex > 0) {
            assertEquals(102, result.inputIds[firstPadIndex - 1])
        }
    }

    @Test
    fun `tokenize should create attention mask correctly`() {
        // Given
        val text = "hello"

        // When
        val result = tokenizer.tokenize(text)

        // Then
        // Attention mask should have 1s for real tokens
        val firstPadIndex = result.attentionMask.indexOf(0)
        assertTrue("Should have padding", firstPadIndex > 0)

        // All positions before padding should be 1
        for (i in 0 until firstPadIndex) {
            assertEquals(1, result.attentionMask[i])
        }

        // All padding positions should be 0
        for (i in firstPadIndex until result.attentionMask.size) {
            assertEquals(0, result.attentionMask[i])
        }
    }

    @Test
    fun `tokenize should pad to max sequence length`() {
        // Given
        val text = "hello"

        // When
        val result = tokenizer.tokenize(text)

        // Then
        assertEquals(128, result.inputIds.size)
        assertEquals(128, result.attentionMask.size)
        assertEquals(128, result.tokenTypeIds.size)
    }

    @Test
    fun `tokenize should handle empty string`() {
        // Given
        val text = ""

        // When
        val result = tokenizer.tokenize(text)

        // Then
        // Should still have [CLS] and [SEP] tokens
        assertEquals(101, result.inputIds[0]) // [CLS]
        assertEquals(102, result.inputIds[1]) // [SEP]
    }

    @Test
    fun `tokenize should lowercase input`() {
        // Given
        val text = "HELLO WORLD"

        // When
        val result1 = tokenizer.tokenize(text)
        val result2 = tokenizer.tokenize("hello world")

        // Then - Should produce same token IDs
        assertArrayEquals(result1.inputIds, result2.inputIds)
    }

    @Test
    fun `tokenize should handle long text truncation`() {
        // Given - Text longer than 128 tokens
        val longText = (1..150).joinToString(" ") { "word$it" }

        // When
        val result = tokenizer.tokenize(longText)

        // Then
        assertEquals(128, result.inputIds.size)
        // Last token should be [SEP]
        assertEquals(102, result.inputIds[127])
    }

    @Test
    fun `token type IDs should be all zeros for single sentence`() {
        // Given
        val text = "hello world"

        // When
        val result = tokenizer.tokenize(text)

        // Then - All token type IDs should be 0 (single sentence)
        assertTrue(result.tokenTypeIds.all { it == 0L })
    }

    @Test
    fun `tokenized input equals should work correctly`() {
        // Given
        val text = "test"
        val result1 = tokenizer.tokenize(text)
        val result2 = tokenizer.tokenize(text)

        // Then
        assertEquals(result1, result2)
    }

    @Test
    fun `tokenized input hashCode should be consistent`() {
        // Given
        val text = "test"
        val result = tokenizer.tokenize(text)

        // Then
        assertEquals(result.hashCode(), result.hashCode())
    }
}
