package com.augmentalis.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * JS/Web tests for BertTokenizer WordPiece implementation
 *
 * Tests tokenization with stub vocabulary (no model download required).
 * Verifies:
 * - CLS/SEP/PAD token placement
 * - Attention mask correctness
 * - Token type IDs for single and pair inputs
 * - Sequence length and padding behavior
 * - Vocab loading from text
 */
class BertTokenizerJsTest {

    @Test
    fun tokenize_producesCorrectLength() {
        val tokenizer = BertTokenizer(maxSequenceLength = 128)
        val result = tokenizer.tokenize("open camera", maxLength = 32)

        assertEquals(32, result.inputIds.size, "inputIds should be padded to maxLength")
        assertEquals(32, result.attentionMask.size, "attentionMask should be padded to maxLength")
        assertEquals(32, result.tokenTypeIds.size, "tokenTypeIds should be padded to maxLength")
    }

    @Test
    fun tokenize_startsWithCLS() {
        val tokenizer = BertTokenizer()
        val result = tokenizer.tokenize("hello", maxLength = 16)

        // With stub vocab: [CLS] = 101
        assertEquals(101L, result.inputIds[0], "First token should be [CLS] = 101")
    }

    @Test
    fun tokenize_hasCorrectAttentionMask() {
        val tokenizer = BertTokenizer()
        val result = tokenizer.tokenize("test", maxLength = 16)

        // At minimum: [CLS] + [UNK] (for "test" with stub vocab) + [SEP] = 3 real tokens
        // First 3 positions should have attention = 1
        assertEquals(1L, result.attentionMask[0], "CLS position should have attention 1")
        assertEquals(1L, result.attentionMask[1], "Token position should have attention 1")
        assertEquals(1L, result.attentionMask[2], "SEP position should have attention 1")

        // Padding positions should have attention = 0
        assertEquals(0L, result.attentionMask[15], "Padding position should have attention 0")
    }

    @Test
    fun tokenize_singleSentenceHasZeroTokenTypeIds() {
        val tokenizer = BertTokenizer()
        val result = tokenizer.tokenize("single sentence", maxLength = 16)

        for (i in result.tokenTypeIds.indices) {
            assertEquals(0L, result.tokenTypeIds[i], "Single sentence token type IDs should all be 0")
        }
    }

    @Test
    fun tokenizePair_hasCorrectTokenTypeIds() {
        val tokenizer = BertTokenizer()
        val result = tokenizer.tokenizePair("sentence A", "sentence B", maxLength = 32)

        // Token type IDs: 0 for textA segment, 1 for textB segment
        assertEquals(0L, result.tokenTypeIds[0], "CLS should have type 0")

        // After the first [SEP], token types should switch to 1
        var foundSwitchToOne = false
        for (i in result.tokenTypeIds.indices) {
            if (result.tokenTypeIds[i] == 1L) {
                foundSwitchToOne = true
                break
            }
        }
        assertTrue(foundSwitchToOne, "Pair tokenization should have token type 1 for textB")
    }

    @Test
    fun tokenize_emptyText_producesOnlyCLSSEP() {
        val tokenizer = BertTokenizer()
        val result = tokenizer.tokenize("", maxLength = 8)

        // Empty text: [CLS] + [SEP] = 2 real tokens
        assertEquals(101L, result.inputIds[0], "CLS at position 0")
        assertEquals(102L, result.inputIds[1], "SEP at position 1")
        assertEquals(0L, result.attentionMask[2], "Position 2 should be padding")
    }

    @Test
    fun tokenize_truncatesLongInput() {
        val tokenizer = BertTokenizer()
        // Very long input that would exceed maxLength
        val longText = (1..100).joinToString(" ") { "word$it" }
        val result = tokenizer.tokenize(longText, maxLength = 16)

        assertEquals(16, result.inputIds.size, "Should be truncated to maxLength")
        // Last real token should be [SEP] = 102
        val lastRealTokenIndex = result.attentionMask.indexOfLast { it == 1L }
        assertEquals(102L, result.inputIds[lastRealTokenIndex], "Last real token should be SEP after truncation")
    }

    @Test
    fun loadVocabFromText_worksCorrectly() {
        val tokenizer = BertTokenizer()

        val vocabText = """
            [PAD]
            [UNK]
            [CLS]
            [SEP]
            hello
            world
            open
            camera
        """.trimIndent()

        tokenizer.loadVocabFromText(vocabText)
        assertTrue(tokenizer.isVocabLoaded(), "Vocab should be loaded")
        assertEquals(8, tokenizer.getVocabSize(), "Should have 8 tokens")

        // Tokenize with the custom vocab
        val result = tokenizer.tokenize("hello world", maxLength = 8)
        // [CLS]=2, hello=4, world=5, [SEP]=3, [PAD]=0, [PAD]=0, [PAD]=0, [PAD]=0
        assertEquals(2L, result.inputIds[0], "CLS should be token 2")
        assertEquals(4L, result.inputIds[1], "hello should be token 4")
        assertEquals(5L, result.inputIds[2], "world should be token 5")
        assertEquals(3L, result.inputIds[3], "SEP should be token 3")
    }

    @Test
    fun tokenize_defaultMaxLength() {
        val tokenizer = BertTokenizer(maxSequenceLength = 64)
        val result = tokenizer.tokenize("test", maxLength = 0)

        // maxLength=0 should use default maxSequenceLength
        assertEquals(64, result.inputIds.size, "Should use default maxSequenceLength when maxLength=0")
    }

    @Test
    fun tokenize_lowercasesInput() {
        val tokenizer = BertTokenizer()
        val vocabText = "[PAD]\n[UNK]\n[CLS]\n[SEP]\nhello"
        tokenizer.loadVocabFromText(vocabText)

        val result = tokenizer.tokenize("HELLO", maxLength = 8)
        // "HELLO" lowercased → "hello" → token 4
        assertEquals(4L, result.inputIds[1], "Input should be lowercased before tokenization")
    }
}
