package com.augmentalis.webavanue

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HistoryEntryTest {

    @Test
    fun `create history entry with default values`() {
        val url = "https://www.example.com"
        val title = "Example Page"
        val entry = HistoryEntry.create(url, title)

        assertEquals(url, entry.url)
        assertEquals(title, entry.title)
        assertEquals(1, entry.visitCount)
        assertEquals(0, entry.visitDuration)
        assertFalse(entry.isIncognito)
        assertNotNull(entry.id)
        assertNotNull(entry.visitedAt)
    }

    @Test
    fun `create incognito history entry`() {
        val entry = HistoryEntry.create(
            url = "https://www.private.com",
            title = "Private",
            isIncognito = true
        )

        assertTrue(entry.isIncognito)
    }

    @Test
    fun `history entry with referrer`() {
        val referrer = "https://www.google.com/search?q=example"
        val entry = HistoryEntry.create(
            url = "https://www.example.com",
            title = "Example",
            referrer = referrer
        )

        assertEquals(referrer, entry.referrer)
    }

    @Test
    fun `history entry with search terms`() {
        val searchTerms = "kotlin multiplatform browser"
        val entry = HistoryEntry(
            id = "history_123",
            url = "https://www.google.com/search",
            title = "Google Search",
            searchTerms = searchTerms,
            visitedAt = Clock.System.now()
        )

        assertEquals(searchTerms, entry.searchTerms)
    }

    @Test
    fun `history entry with visit duration`() {
        val duration = 300L // 5 minutes in seconds
        val entry = HistoryEntry(
            id = "history_123",
            url = "https://www.example.com",
            title = "Example",
            visitDuration = duration,
            visitedAt = Clock.System.now()
        )

        assertEquals(duration, entry.visitDuration)
    }

    @Test
    fun `history entry with device id for sync`() {
        val deviceId = "device_android_123"
        val entry = HistoryEntry(
            id = "history_123",
            url = "https://www.example.com",
            title = "Example",
            deviceId = deviceId,
            visitedAt = Clock.System.now()
        )

        assertEquals(deviceId, entry.deviceId)
    }

    @Test
    fun `history id is unique`() {
        val entry1 = HistoryEntry.create("https://www.example.com", "Example")
        val entry2 = HistoryEntry.create("https://www.example.com", "Example")

        assertTrue(entry1.id != entry2.id)
    }

    @Test
    fun `max history entries constant`() {
        assertEquals(10000, HistoryEntry.MAX_HISTORY_ENTRIES)
    }

    @Test
    fun `retention days constant`() {
        assertEquals(90, HistoryEntry.RETENTION_DAYS)
    }

    @Test
    fun `create history session`() {
        val session = HistorySession(
            id = "session_123",
            title = "Morning browsing",
            startTime = Clock.System.now(),
            endTime = null,
            entryIds = listOf("entry1", "entry2", "entry3"),
            tabCount = 5,
            totalDuration = 1800 // 30 minutes
        )

        assertEquals("Morning browsing", session.title)
        assertEquals(3, session.entryIds.size)
        assertEquals(5, session.tabCount)
        assertEquals(1800, session.totalDuration)
    }
}