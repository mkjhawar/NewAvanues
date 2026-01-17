package com.augmentalis.llm.alc

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for TokenSampler
 *
 * Tests token sampling strategies and presets.
 */
class TokenSamplerTest {

    @Test
    fun `test greedy sampling returns highest probability token`() {
        val logits = floatArrayOf(1.0f, 5.0f, 3.0f, 2.0f)
        val token = TokenSampler.sampleGreedy(logits)
        assertEquals("Should return index of highest logit", 1, token)
    }

    @Test
    fun `test greedy sampling is deterministic`() {
        val logits = floatArrayOf(1.0f, 5.0f, 3.0f, 2.0f)

        val samples = (1..10).map {
            TokenSampler.sampleGreedy(logits)
        }

        // All samples should be identical
        assertTrue("Greedy sampling should be deterministic", samples.distinct().size == 1)
        assertEquals(1, samples.first())
    }

    @Test
    fun `test temperature increases randomness`() {
        val logits = floatArrayOf(3.0f, 3.5f, 3.2f) // Similar scores

        // Low temperature - more deterministic
        val lowTempSamples = (1..100).map {
            TokenSampler.sample(logits, temperature = 0.1f, topP = 1.0f, topK = 3)
        }

        // High temperature - more random
        val highTempSamples = (1..100).map {
            TokenSampler.sample(logits, temperature = 2.0f, topP = 1.0f, topK = 3)
        }

        val lowTempVariety = lowTempSamples.distinct().size
        val highTempVariety = highTempSamples.distinct().size

        assertTrue("High temp should have more variety than low temp", highTempVariety >= lowTempVariety)
    }

    @Test
    fun `test top-k limits candidates`() {
        val logits = floatArrayOf(5.0f, 4.0f, 3.0f, 2.0f, 1.0f)

        // Sample with top-k = 2 (only top 2 tokens)
        val samples = (1..100).map {
            TokenSampler.sample(logits, temperature = 1.0f, topP = 1.0f, topK = 2)
        }

        // Should only sample from top 2 tokens (indices 0 and 1)
        val uniqueTokens = samples.distinct().sorted()
        assertTrue("Top-k=2 should only sample from top 2 tokens", uniqueTokens.all { it in setOf(0, 1) })
    }

    @Test
    fun `test repetition penalty reduces repeated tokens`() {
        val logits = floatArrayOf(5.0f, 4.0f, 3.0f)
        val previousTokens = listOf(0, 0, 0) // Token 0 repeated 3 times

        // Sample with high repetition penalty
        val samples = (1..100).map {
            TokenSampler.sample(
                logits = logits,
                temperature = 0.5f,
                topP = 1.0f,
                topK = 3,
                repetitionPenalty = 1.5f,
                previousTokens = previousTokens
            )
        }

        // Token 0 should appear less frequently due to penalty
        val token0Count = samples.count { it == 0 }
        val otherCount = samples.count { it != 0 }

        assertTrue("Repeated token should be penalized", otherCount > token0Count)
    }

    @Test
    fun `test sample returns valid token index`() {
        val logits = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)

        val token = TokenSampler.sample(logits, temperature = 0.8f, topP = 0.95f, topK = 4)

        assertTrue("Token should be valid index", token in logits.indices)
    }

    @Test
    fun `test PRECISE config parameters`() {
        val config = TokenSampler.SamplingConfig.PRECISE

        assertEquals(0.3f, config.temperature)
        assertEquals(0.9f, config.topP)
        assertEquals(40, config.topK)
        assertEquals(1.1f, config.repetitionPenalty)
    }

    @Test
    fun `test BALANCED config parameters`() {
        val config = TokenSampler.SamplingConfig.BALANCED

        assertEquals(0.8f, config.temperature)
        assertEquals(0.95f, config.topP)
        assertEquals(50, config.topK)
        assertEquals(1.15f, config.repetitionPenalty)
    }

    @Test
    fun `test CREATIVE config parameters`() {
        val config = TokenSampler.SamplingConfig.CREATIVE

        assertEquals(1.2f, config.temperature)
        assertEquals(0.98f, config.topP)
        assertEquals(100, config.topK)
        assertEquals(1.2f, config.repetitionPenalty)
    }

    @Test
    fun `test GREEDY config parameters`() {
        val config = TokenSampler.SamplingConfig.GREEDY

        assertEquals(0.1f, config.temperature)
        assertEquals(0.8f, config.topP)
        assertEquals(10, config.topK)
        assertEquals(1.0f, config.repetitionPenalty)
    }

    @Test
    fun `test sampling with empty logits throws exception`() {
        val logits = floatArrayOf()

        try {
            TokenSampler.sample(logits)
            fail("Should throw exception for empty logits")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("cannot be empty") == true)
        }
    }

    @Test
    fun `test sampling with negative temperature throws exception`() {
        val logits = floatArrayOf(1.0f, 2.0f, 3.0f)

        try {
            TokenSampler.sample(logits, temperature = -0.5f)
            fail("Should throw exception for negative temperature")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("must be positive") == true)
        }
    }

    @Test
    fun `test sampling with invalid topP throws exception`() {
        val logits = floatArrayOf(1.0f, 2.0f, 3.0f)

        try {
            TokenSampler.sample(logits, topP = 1.5f)
            fail("Should throw exception for topP > 1.0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("between 0 and 1") == true)
        }
    }

    @Test
    fun `test sampling with zero topK throws exception`() {
        val logits = floatArrayOf(1.0f, 2.0f, 3.0f)

        try {
            TokenSampler.sample(logits, topK = 0)
            fail("Should throw exception for topK = 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("must be positive") == true)
        }
    }

    @Test
    fun `test sampling fallback on error`() {
        val logits = floatArrayOf(1.0f, Float.NaN, 3.0f) // Invalid logit

        // Should fallback to greedy and not crash
        val token = TokenSampler.sample(logits)

        assertTrue("Should return valid token despite NaN", token in logits.indices)
    }

    @Test
    fun `test top-p with low threshold is more deterministic`() {
        val logits = floatArrayOf(5.0f, 4.0f, 3.0f, 2.0f, 1.0f)

        val lowPSamples = (1..100).map {
            TokenSampler.sample(logits, temperature = 1.0f, topP = 0.5f, topK = 100)
        }

        val highPSamples = (1..100).map {
            TokenSampler.sample(logits, temperature = 1.0f, topP = 0.99f, topK = 100)
        }

        val lowPVariety = lowPSamples.distinct().size
        val highPVariety = highPSamples.distinct().size

        assertTrue("High top-p should allow more variety", highPVariety >= lowPVariety)
    }

    @Test
    fun `test temperature of 1_0 does not change logits`() {
        val logits = floatArrayOf(1.0f, 2.0f, 3.0f)

        // With temperature = 1.0, the distribution should remain the same
        // We can't directly test the internal state, but we can verify it doesn't crash
        val token = TokenSampler.sample(logits, temperature = 1.0f)

        assertTrue("Should return valid token", token in logits.indices)
    }

    @Test
    fun `test single logit always returns that token`() {
        val logits = floatArrayOf(5.0f)

        val samples = (1..10).map {
            TokenSampler.sample(logits)
        }

        assertTrue("Single logit should always return index 0", samples.all { it == 0 })
    }

    @Test
    fun `test uniform logits produce varied samples`() {
        val logits = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f) // All equal

        val samples = (1..100).map {
            TokenSampler.sample(logits, temperature = 1.0f, topP = 1.0f, topK = 4)
        }

        val variety = samples.distinct().size

        assertTrue("Uniform logits should produce varied samples", variety > 1)
    }
}
