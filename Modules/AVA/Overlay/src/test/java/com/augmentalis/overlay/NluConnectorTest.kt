// filename: features/overlay/src/test/java/com/augmentalis/ava/features/overlay/NluConnectorTest.kt
// created: 2025-11-02 00:20:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 4 - Testing
// agent: Engineer | mode: ACT

package com.augmentalis.overlay

import android.content.Context
import com.augmentalis.overlay.integration.NluConnector
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NluConnector intent classification
 */
class NluConnectorTest {

    private lateinit var connector: NluConnector
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        connector = NluConnector(mockContext)
    }

    @Test
    fun `classifyIntent detects search intent`() = runTest {
        val inputs = listOf(
            "search for restaurants nearby",
            "find the best laptop",
            "look up quantum physics",
            "google weather today"
        )

        inputs.forEach { input ->
            val intent = connector.classifyIntent(input)
            assertEquals("search", intent)
        }
    }

    @Test
    fun `classifyIntent detects translate intent`() = runTest {
        val inputs = listOf(
            "translate this to Spanish",
            "how do you say hello in French",
            "translate in German"
        )

        inputs.forEach { input ->
            val intent = connector.classifyIntent(input)
            assertEquals("translate", intent)
        }
    }

    @Test
    fun `classifyIntent detects reminder intent`() = runTest {
        val inputs = listOf(
            "remind me to call John",
            "schedule a meeting tomorrow",
            "set calendar appointment",
            "create reminder for 3pm"
        )

        inputs.forEach { input ->
            val intent = connector.classifyIntent(input)
            assertEquals("reminder", intent)
        }
    }

    @Test
    fun `classifyIntent detects message intent`() = runTest {
        val inputs = listOf(
            "message Sarah",
            "send a text to Mom",
            "call the office",
            "email the team"
        )

        inputs.forEach { input ->
            val intent = connector.classifyIntent(input)
            assertEquals("message", intent)
        }
    }

    @Test
    fun `classifyIntent detects summarize intent`() = runTest {
        val inputs = listOf(
            "summarize this article",
            "give me a summary",
            "tldr this document"
        )

        inputs.forEach { input ->
            val intent = connector.classifyIntent(input)
            assertEquals("summarize", intent)
        }
    }

    @Test
    fun `classifyIntent detects query intent`() = runTest {
        val inputs = listOf(
            "what is photosynthesis",
            "who is the president",
            "when is the deadline",
            "where is Tokyo",
            "how to bake bread"
        )

        inputs.forEach { input ->
            val intent = connector.classifyIntent(input)
            assertEquals("query", intent)
        }
    }

    @Test
    fun `classifyIntent defaults to general for unknown input`() = runTest {
        val inputs = listOf(
            "hello there",
            "this is random text",
            "abcdefg"
        )

        inputs.forEach { input ->
            val intent = connector.classifyIntent(input)
            assertEquals("general", intent)
        }
    }

    @Test
    fun `classifyIntent is case insensitive`() = runTest {
        val inputs = listOf(
            "SEARCH for something",
            "Search For Something",
            "sEaRcH fOr SoMeThInG"
        )

        inputs.forEach { input ->
            val intent = connector.classifyIntent(input)
            assertEquals("search", intent)
        }
    }
}
