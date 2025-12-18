/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.features.nlu

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance tests for WordPiece tokenization
 *
 * Verifies that tokenization meets <10ms requirement
 */
@RunWith(AndroidJUnit4::class)
class WordPiecePerformanceTest {

    private lateinit var context: Context
    private lateinit var tokenizer: BertTokenizer

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        tokenizer = BertTokenizer(context)
    }

    @Test
    fun testTokenizationPerformance() {
        val sentences = listOf(
            "turn on the lights",
            "what's the weather today",
            "play some music",
            "set a timer for 5 minutes",
            "tell me a joke",
            "remind me to call mom tomorrow",
            "how do I get to the airport",
            "show me photos from last week"
        )

        // Warm up (10 iterations)
        repeat(10) {
            tokenizer.tokenize(sentences.random())
        }

        // Measure 50 iterations
        val times = mutableListOf<Long>()
        repeat(50) {
            val sentence = sentences.random()
            val time = measureTimeMillis {
                tokenizer.tokenize(sentence)
            }
            times.add(time)
        }

        val avgTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        val minTime = times.minOrNull() ?: 0L
        val p95Time = times.sorted()[47] // 95th percentile of 50 samples

        println("========================================")
        println("WordPiece Tokenization Performance")
        println("========================================")
        println("Average:    ${avgTime}ms")
        println("Min:        ${minTime}ms")
        println("Max:        ${maxTime}ms")
        println("P95:        ${p95Time}ms")
        println("Target:     <10ms")
        println("========================================")

        // Assert performance requirements
        assertTrue("Average tokenization time should be <10ms, was ${avgTime}ms",
            avgTime < 10.0)
        assertTrue("P95 tokenization time should be <20ms, was ${p95Time}ms",
            p95Time < 20)

        // If we're way under budget, report it
        if (avgTime < 5.0) {
            println("✅ EXCELLENT: Tokenization is ${5.0 - avgTime}ms under 5ms target!")
        }
    }

    @Test
    fun testBatchTokenizationPerformance() {
        val texts = listOf(
            "hello world",
            "how are you today",
            "the quick brown fox",
            "artificial intelligence",
            "natural language processing"
        )

        // Warm up
        repeat(5) {
            tokenizer.tokenizeBatch(texts)
        }

        // Measure
        val times = mutableListOf<Long>()
        repeat(20) {
            val time = measureTimeMillis {
                tokenizer.tokenizeBatch(texts)
            }
            times.add(time)
        }

        val avgTime = times.average()
        val perTextTime = avgTime / texts.size

        println("========================================")
        println("Batch Tokenization Performance")
        println("========================================")
        println("Batch size:         ${texts.size}")
        println("Total avg time:     ${avgTime}ms")
        println("Per-text avg:       ${perTextTime}ms")
        println("Target per-text:    <10ms")
        println("========================================")

        assertTrue("Per-text batch tokenization should be <10ms, was ${perTextTime}ms",
            perTextTime < 10.0)
    }
}
