package com.augmentalis.llm.alc

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for StopTokenDetector
 *
 * Tests model-specific EOS token detection and stop sequence handling.
 */
class StopTokenDetectorTest {

    @Test
    fun `test Gemma stop tokens`() {
        val stopTokens = StopTokenDetector.getStopTokens("gemma-2b-it-q4f16_1")

        assertTrue("Gemma should have token 1 as stop token", 1 in stopTokens)
        assertTrue("Gemma should have token 2 as stop token", 2 in stopTokens)
    }

    @Test
    fun `test Qwen stop tokens`() {
        val stopTokens = StopTokenDetector.getStopTokens("qwen2.5-1.5b-instruct-q4f16_1")

        assertTrue("Qwen should have token 151643 as stop token", 151643 in stopTokens)
        assertTrue("Qwen should have token 151645 as stop token", 151645 in stopTokens)
    }

    @Test
    fun `test Llama stop tokens`() {
        val stopTokens = StopTokenDetector.getStopTokens("Llama-3.2-3B-Instruct-q4f16_1")

        assertTrue("Llama should have token 2 as stop token", 2 in stopTokens)
        assertTrue("Llama should have token 128001 as stop token", 128001 in stopTokens)
    }

    @Test
    fun `test Phi stop tokens`() {
        val stopTokens = StopTokenDetector.getStopTokens("Phi-3.5-mini-instruct-q4f16_1")

        assertTrue("Phi should have token 32000 as stop token", 32000 in stopTokens)
        assertTrue("Phi should have token 32001 as stop token", 32001 in stopTokens)
    }

    @Test
    fun `test Mistral stop tokens`() {
        val stopTokens = StopTokenDetector.getStopTokens("Mistral-7B-Instruct-v0.3-q4f16_1")

        assertTrue("Mistral should have token 2 as stop token", 2 in stopTokens)
    }

    @Test
    fun `test unknown model returns default stop tokens`() {
        val stopTokens = StopTokenDetector.getStopTokens("unknown-model-123")

        assertTrue("Unknown model should have default stop tokens", stopTokens.isNotEmpty())
        assertTrue("Default should include token 1 or 2", 1 in stopTokens || 2 in stopTokens)
    }

    @Test
    fun `test isStopToken for Gemma`() {
        assertTrue(StopTokenDetector.isStopToken(1, "gemma-2b-it-q4f16_1"))
        assertTrue(StopTokenDetector.isStopToken(2, "gemma-2b-it-q4f16_1"))
        assertFalse(StopTokenDetector.isStopToken(100, "gemma-2b-it-q4f16_1"))
    }

    @Test
    fun `test isStopToken for Qwen`() {
        assertTrue(StopTokenDetector.isStopToken(151643, "qwen2.5-1.5b-instruct-q4f16_1"))
        assertTrue(StopTokenDetector.isStopToken(151645, "qwen2.5-1.5b-instruct-q4f16_1"))
        assertFalse(StopTokenDetector.isStopToken(100, "qwen2.5-1.5b-instruct-q4f16_1"))
    }

    @Test
    fun `test Gemma stop sequences`() {
        val sequences = StopTokenDetector.getStopSequences("gemma-2b-it-q4f16_1")

        assertTrue("Should include </s>", "</s>" in sequences)
        assertTrue("Should include <eos>", "<eos>" in sequences)
    }

    @Test
    fun `test Qwen stop sequences`() {
        val sequences = StopTokenDetector.getStopSequences("qwen2.5-1.5b-instruct-q4f16_1")

        assertTrue("Should include <|im_end|>", "<|im_end|>" in sequences)
        assertTrue("Should include <|endoftext|>", "<|endoftext|>" in sequences)
    }

    @Test
    fun `test Llama stop sequences`() {
        val sequences = StopTokenDetector.getStopSequences("Llama-3.2-3B-Instruct-q4f16_1")

        assertTrue("Should include </s>", "</s>" in sequences)
        assertTrue("Should include <|end_of_text|>", "<|end_of_text|>" in sequences)
    }

    @Test
    fun `test Phi stop sequences`() {
        val sequences = StopTokenDetector.getStopSequences("Phi-3.5-mini-instruct-q4f16_1")

        assertTrue("Should include <|endoftext|>", "<|endoftext|>" in sequences)
        assertTrue("Should include <|end|>", "<|end|>" in sequences)
    }

    @Test
    fun `test Mistral stop sequences`() {
        val sequences = StopTokenDetector.getStopSequences("Mistral-7B-Instruct-v0.3-q4f16_1")

        assertTrue("Should include </s>", "</s>" in sequences)
        assertTrue("Should include [/INST]", "[/INST]" in sequences)
    }

    @Test
    fun `test endsWithStopSequence detects end marker`() {
        assertTrue(StopTokenDetector.endsWithStopSequence("Hello world</s>", "gemma-2b-it-q4f16_1"))
        assertTrue(StopTokenDetector.endsWithStopSequence("Response text<|im_end|>", "qwen2.5-1.5b-instruct-q4f16_1"))
        assertFalse(StopTokenDetector.endsWithStopSequence("Hello world", "gemma-2b-it-q4f16_1"))
    }

    @Test
    fun `test endsWithStopSequence handles whitespace`() {
        assertTrue(StopTokenDetector.endsWithStopSequence("Hello world</s>  \n", "gemma-2b-it-q4f16_1"))
    }

    @Test
    fun `test removeStopSequences cleans text`() {
        val cleaned = StopTokenDetector.removeStopSequences("Hello world</s>", "gemma-2b-it-q4f16_1")
        assertEquals("Hello world", cleaned)
    }

    @Test
    fun `test removeStopSequences for Qwen`() {
        val cleaned = StopTokenDetector.removeStopSequences(
            "Response text<|im_end|>",
            "qwen2.5-1.5b-instruct-q4f16_1"
        )
        assertEquals("Response text", cleaned)
    }

    @Test
    fun `test removeStopSequences for Llama`() {
        val cleaned = StopTokenDetector.removeStopSequences(
            "Response text<|end_of_text|>",
            "Llama-3.2-3B-Instruct-q4f16_1"
        )
        assertEquals("Response text", cleaned)
    }

    @Test
    fun `test removeStopSequences handles multiple sequences`() {
        val text = "Hello</s><eos>"
        val cleaned = StopTokenDetector.removeStopSequences(text, "gemma-2b-it-q4f16_1")
        assertEquals("Hello", cleaned)
    }

    @Test
    fun `test removeStopSequences preserves text without sequences`() {
        val text = "Hello world"
        val cleaned = StopTokenDetector.removeStopSequences(text, "gemma-2b-it-q4f16_1")
        assertEquals(text, cleaned)
    }

    @Test
    fun `test getModelTokenInfo for Gemma`() {
        val info = StopTokenDetector.getModelTokenInfo("gemma-2b-it-q4f16_1")

        assertEquals(1, info.eosTokenId)
        assertEquals(2, info.bosTokenId)
        assertEquals(0, info.padTokenId)
        assertTrue("</s>" in info.stopSequences)
    }

    @Test
    fun `test getModelTokenInfo for Qwen`() {
        val info = StopTokenDetector.getModelTokenInfo("qwen2.5-1.5b-instruct-q4f16_1")

        assertEquals(151643, info.eosTokenId)
        assertEquals(151644, info.bosTokenId)
        assertEquals(151643, info.padTokenId)
        assertTrue("<|im_end|>" in info.stopSequences)
    }

    @Test
    fun `test getModelTokenInfo for Llama`() {
        val info = StopTokenDetector.getModelTokenInfo("Llama-3.2-3B-Instruct-q4f16_1")

        assertEquals(2, info.eosTokenId)
        assertEquals(1, info.bosTokenId)
        assertEquals(0, info.padTokenId)
        assertTrue("</s>" in info.stopSequences)
    }

    @Test
    fun `test getModelTokenInfo for Phi`() {
        val info = StopTokenDetector.getModelTokenInfo("Phi-3.5-mini-instruct-q4f16_1")

        assertEquals(32000, info.eosTokenId)
        assertNull(info.bosTokenId)
        assertEquals(32000, info.padTokenId)
        assertTrue("<|endoftext|>" in info.stopSequences)
    }

    @Test
    fun `test getMaxGenerationLength for models`() {
        assertEquals(2048, StopTokenDetector.getMaxGenerationLength("gemma-2b-it-q4f16_1"))
        assertEquals(2048, StopTokenDetector.getMaxGenerationLength("qwen2.5-1.5b-instruct-q4f16_1"))
        assertEquals(4096, StopTokenDetector.getMaxGenerationLength("Llama-3.2-3B-Instruct-q4f16_1"))
        assertEquals(4096, StopTokenDetector.getMaxGenerationLength("Phi-3.5-mini-instruct-q4f16_1"))
        assertEquals(8192, StopTokenDetector.getMaxGenerationLength("Mistral-7B-Instruct-v0.3-q4f16_1"))
        assertEquals(2048, StopTokenDetector.getMaxGenerationLength("unknown-model"))
    }

    @Test
    fun `test getRecommendedSamplingConfig for models`() {
        val phiConfig = StopTokenDetector.getRecommendedSamplingConfig("Phi-3.5-mini-instruct-q4f16_1")
        assertEquals(TokenSampler.SamplingConfig.PRECISE, phiConfig)

        val qwenConfig = StopTokenDetector.getRecommendedSamplingConfig("qwen2.5-1.5b-instruct-q4f16_1")
        assertEquals(TokenSampler.SamplingConfig.BALANCED, qwenConfig)

        val gemmaConfig = StopTokenDetector.getRecommendedSamplingConfig("gemma-2b-it-q4f16_1")
        assertEquals(TokenSampler.SamplingConfig.BALANCED, gemmaConfig)

        val mistralConfig = StopTokenDetector.getRecommendedSamplingConfig("Mistral-7B-Instruct-v0.3-q4f16_1")
        assertEquals(TokenSampler.SamplingConfig.PRECISE, mistralConfig)
    }

    @Test
    fun `test case insensitive model detection`() {
        val gemmaLower = StopTokenDetector.getStopTokens("gemma-2b-it-q4f16_1")
        val gemmaUpper = StopTokenDetector.getStopTokens("GEMMA-2B-IT-Q4F16_1")
        val gemmaMixed = StopTokenDetector.getStopTokens("Gemma-2B-It-q4f16_1")

        assertEquals(gemmaLower, gemmaUpper)
        assertEquals(gemmaLower, gemmaMixed)
    }
}
