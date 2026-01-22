package com.augmentalis.webavanue

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TabTest {

    @Test
    fun `create tab with default values`() {
        val url = "https://www.google.com"
        val tab = Tab.create(url)

        assertEquals(url, tab.url)
        assertEquals("", tab.title)
        assertFalse(tab.isIncognito)
        assertFalse(tab.isActive)
        assertFalse(tab.isPinned)
        assertNotNull(tab.id)
        assertNotNull(tab.createdAt)
        assertNotNull(tab.lastAccessedAt)
        assertEquals(0, tab.position)
    }

    @Test
    fun `create incognito tab`() {
        val url = "https://www.example.com"
        val tab = Tab.create(url, isIncognito = true)

        assertEquals(url, tab.url)
        assertTrue(tab.isIncognito)
    }

    @Test
    fun `create tab with title`() {
        val url = "https://www.example.com"
        val title = "Example Website"
        val tab = Tab.create(url, title = title)

        assertEquals(url, tab.url)
        assertEquals(title, tab.title)
    }

    @Test
    fun `tab id is unique`() {
        val tab1 = Tab.create("https://www.google.com")
        val tab2 = Tab.create("https://www.google.com")

        // IDs should be different even for same URL
        assertTrue(tab1.id != tab2.id)
    }

    @Test
    fun `tab can have parent for grouping`() {
        val parentTab = Tab.create("https://www.parent.com")
        val childTab = Tab(
            id = "child_123",
            url = "https://www.child.com",
            title = "Child Tab",
            parentTabId = parentTab.id,
            createdAt = Clock.System.now(),
            lastAccessedAt = Clock.System.now()
        )

        assertEquals(parentTab.id, childTab.parentTabId)
    }

    @Test
    fun `tab can store session data`() {
        val sessionData = """{"scrollPosition": 500, "formData": {"username": "test"}}"""
        val tab = Tab(
            id = "tab_123",
            url = "https://www.example.com",
            title = "Example",
            sessionData = sessionData,
            createdAt = Clock.System.now(),
            lastAccessedAt = Clock.System.now()
        )

        assertEquals(sessionData, tab.sessionData)
    }

    @Test
    fun `max tabs constant is defined`() {
        assertEquals(100, Tab.MAX_TABS)
    }

    @Test
    fun `default url is google`() {
        assertEquals("https://www.google.com", Tab.DEFAULT_URL)
    }
}