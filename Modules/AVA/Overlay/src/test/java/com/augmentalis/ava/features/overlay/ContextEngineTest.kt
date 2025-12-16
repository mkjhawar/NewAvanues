// filename: features/overlay/src/test/java/com/augmentalis/ava/features/overlay/ContextEngineTest.kt
// created: 2025-11-02 00:25:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 4 - Testing
// agent: Engineer | mode: ACT

package com.augmentalis.ava.features.overlay

import android.content.Context
import com.augmentalis.ava.features.overlay.context.AppCategory
import com.augmentalis.ava.features.overlay.context.AppContext
import com.augmentalis.ava.features.overlay.context.ContextEngine
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ContextEngine smart suggestions
 */
class ContextEngineTest {

    private lateinit var contextEngine: ContextEngine
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        contextEngine = ContextEngine(mockContext)
    }

    @Test
    fun `generateSmartSuggestions returns browser suggestions`() {
        val appContext = AppContext(
            packageName = "com.android.chrome",
            appName = "Chrome",
            category = AppCategory.BROWSER
        )

        val suggestions = contextEngine.generateSmartSuggestions(appContext)

        assertTrue(suggestions.any { it.label == "Summarize page" })
        assertTrue(suggestions.any { it.label == "Translate" })
        assertTrue(suggestions.any { it.label == "Read aloud" })
        assertEquals(4, suggestions.size)
    }

    @Test
    fun `generateSmartSuggestions returns messaging suggestions`() {
        val appContext = AppContext(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            category = AppCategory.MESSAGING
        )

        val suggestions = contextEngine.generateSmartSuggestions(appContext)

        assertTrue(suggestions.any { it.label == "Reply" })
        assertTrue(suggestions.any { it.label == "Translate message" })
        assertTrue(suggestions.any { it.label == "Voice message" })
        assertEquals(4, suggestions.size)
    }

    @Test
    fun `generateSmartSuggestions returns email suggestions`() {
        val appContext = AppContext(
            packageName = "com.google.android.gm",
            appName = "Gmail",
            category = AppCategory.EMAIL
        )

        val suggestions = contextEngine.generateSmartSuggestions(appContext)

        assertTrue(suggestions.any { it.label == "Compose reply" })
        assertTrue(suggestions.any { it.label == "Summarize email" })
        assertTrue(suggestions.any { it.label == "Schedule send" })
        assertEquals(4, suggestions.size)
    }

    @Test
    fun `generateSmartSuggestions returns social media suggestions`() {
        val appContext = AppContext(
            packageName = "com.instagram.android",
            appName = "Instagram",
            category = AppCategory.SOCIAL
        )

        val suggestions = contextEngine.generateSmartSuggestions(appContext)

        assertTrue(suggestions.any { it.label == "Caption this" })
        assertTrue(suggestions.any { it.label == "Translate post" })
        assertEquals(4, suggestions.size)
    }

    @Test
    fun `generateSmartSuggestions returns productivity suggestions`() {
        val appContext = AppContext(
            packageName = "com.google.android.keep",
            appName = "Keep Notes",
            category = AppCategory.PRODUCTIVITY
        )

        val suggestions = contextEngine.generateSmartSuggestions(appContext)

        assertTrue(suggestions.any { it.label == "Summarize" })
        assertTrue(suggestions.any { it.label == "Proofread" })
        assertTrue(suggestions.any { it.label == "Continue writing" })
        assertEquals(4, suggestions.size)
    }

    @Test
    fun `generateSmartSuggestions returns maps suggestions`() {
        val appContext = AppContext(
            packageName = "com.google.android.apps.maps",
            appName = "Maps",
            category = AppCategory.MAPS
        )

        val suggestions = contextEngine.generateSmartSuggestions(appContext)

        assertTrue(suggestions.any { it.label == "Directions" })
        assertTrue(suggestions.any { it.label == "Traffic update" })
        assertEquals(4, suggestions.size)
    }

    @Test
    fun `generateSmartSuggestions returns shopping suggestions`() {
        val appContext = AppContext(
            packageName = "com.amazon.mShop.android",
            appName = "Amazon",
            category = AppCategory.SHOPPING
        )

        val suggestions = contextEngine.generateSmartSuggestions(appContext)

        assertTrue(suggestions.any { it.label == "Compare prices" })
        assertTrue(suggestions.any { it.label == "Read reviews" })
        assertEquals(4, suggestions.size)
    }

    @Test
    fun `generateSmartSuggestions returns media suggestions`() {
        val appContext = AppContext(
            packageName = "com.spotify.music",
            appName = "Spotify",
            category = AppCategory.MEDIA
        )

        val suggestions = contextEngine.generateSmartSuggestions(appContext)

        assertTrue(suggestions.any { it.label == "What's this song?" })
        assertTrue(suggestions.any { it.label == "Lyrics" })
        assertEquals(4, suggestions.size)
    }

    @Test
    fun `generateSmartSuggestions returns default suggestions when context is null`() {
        val suggestions = contextEngine.generateSmartSuggestions(null)

        assertTrue(suggestions.any { it.label == "Search" })
        assertTrue(suggestions.any { it.label == "Translate" })
        assertTrue(suggestions.any { it.label == "Reminder" })
        assertTrue(suggestions.any { it.label == "Note" })
        assertEquals(4, suggestions.size)
    }

    @Test
    fun `updateScreenText updates state flow`() {
        val testText = "Test screen content"

        contextEngine.updateScreenText(testText)

        assertEquals(testText, contextEngine.screenText.value)
    }

    @Test
    fun `updateScreenText can set null`() {
        contextEngine.updateScreenText("some text")
        contextEngine.updateScreenText(null)

        assertNull(contextEngine.screenText.value)
    }
}
