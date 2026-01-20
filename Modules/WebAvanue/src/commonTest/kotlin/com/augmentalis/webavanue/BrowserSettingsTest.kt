package com.augmentalis.webavanue

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowserSettingsTest {

    @Test
    fun `default settings have expected values`() {
        val settings = BrowserSettings.default()

        assertEquals(BrowserSettings.Theme.SYSTEM, settings.theme)
        assertEquals(BrowserSettings.FontSize.MEDIUM, settings.fontSize)
        assertFalse(settings.forceZoom)
        assertTrue(settings.showImages)
        assertFalse(settings.useDesktopMode)
        assertTrue(settings.blockPopups)
        assertTrue(settings.blockAds)
        assertTrue(settings.blockTrackers)
        assertTrue(settings.doNotTrack)
        assertFalse(settings.clearCacheOnExit)
        assertTrue(settings.enableJavaScript)
        assertEquals(BrowserSettings.SearchEngine.GOOGLE, settings.defaultSearchEngine)
    }

    @Test
    fun `privacy mode settings`() {
        val settings = BrowserSettings.privacyMode()

        assertTrue(settings.blockPopups)
        assertTrue(settings.blockAds)
        assertTrue(settings.blockTrackers)
        assertTrue(settings.doNotTrack)
        assertTrue(settings.clearCacheOnExit)
        assertTrue(settings.clearHistoryOnExit)
        assertTrue(settings.clearCookiesOnExit)
        assertFalse(settings.enableWebRTC)
        assertEquals(BrowserSettings.SearchEngine.DUCKDUCKGO, settings.defaultSearchEngine)
        assertFalse(settings.searchSuggestions)
        assertFalse(settings.syncEnabled)
    }

    @Test
    fun `performance mode settings`() {
        val settings = BrowserSettings.performanceMode()

        assertFalse(settings.showImages)
        assertFalse(settings.enableJavaScript)
        assertTrue(settings.hardwareAcceleration)
        assertFalse(settings.preloadPages)
        assertTrue(settings.dataSaver)
        assertEquals(BrowserSettings.AutoPlay.NEVER, settings.autoPlay)
        assertTrue(settings.blockAds)
    }

    @Test
    fun `theme options`() {
        val themes = BrowserSettings.Theme.values()

        assertEquals(4, themes.size)
        assertTrue(themes.contains(BrowserSettings.Theme.LIGHT))
        assertTrue(themes.contains(BrowserSettings.Theme.DARK))
        assertTrue(themes.contains(BrowserSettings.Theme.SYSTEM))
        assertTrue(themes.contains(BrowserSettings.Theme.AUTO))
    }

    @Test
    fun `font size scales`() {
        assertEquals(0.75f, BrowserSettings.FontSize.TINY.scale)
        assertEquals(0.875f, BrowserSettings.FontSize.SMALL.scale)
        assertEquals(1.0f, BrowserSettings.FontSize.MEDIUM.scale)
        assertEquals(1.125f, BrowserSettings.FontSize.LARGE.scale)
        assertEquals(1.25f, BrowserSettings.FontSize.HUGE.scale)
    }

    @Test
    fun `search engine urls`() {
        assertEquals("https://www.google.com/search", BrowserSettings.SearchEngine.GOOGLE.baseUrl)
        assertEquals("q", BrowserSettings.SearchEngine.GOOGLE.queryParam)

        assertEquals("https://duckduckgo.com/", BrowserSettings.SearchEngine.DUCKDUCKGO.baseUrl)
        assertEquals("q", BrowserSettings.SearchEngine.DUCKDUCKGO.queryParam)

        assertEquals("https://www.bing.com/search", BrowserSettings.SearchEngine.BING.baseUrl)
        assertEquals("q", BrowserSettings.SearchEngine.BING.queryParam)
    }

    @Test
    fun `new tab page options`() {
        val options = BrowserSettings.NewTabPage.values()

        assertEquals(6, options.size)
        assertTrue(options.contains(BrowserSettings.NewTabPage.BLANK))
        assertTrue(options.contains(BrowserSettings.NewTabPage.HOME_PAGE))
        assertTrue(options.contains(BrowserSettings.NewTabPage.TOP_SITES))
        assertTrue(options.contains(BrowserSettings.NewTabPage.MOST_VISITED))
        assertTrue(options.contains(BrowserSettings.NewTabPage.SPEED_DIAL))
        assertTrue(options.contains(BrowserSettings.NewTabPage.NEWS_FEED))
    }

    @Test
    fun `auto play options`() {
        val options = BrowserSettings.AutoPlay.values()

        assertEquals(4, options.size)
        assertTrue(options.contains(BrowserSettings.AutoPlay.ALWAYS))
        assertTrue(options.contains(BrowserSettings.AutoPlay.WIFI_ONLY))
        assertTrue(options.contains(BrowserSettings.AutoPlay.NEVER))
        assertTrue(options.contains(BrowserSettings.AutoPlay.ASK))
    }

    @Test
    fun `custom settings configuration`() {
        val settings = BrowserSettings(
            theme = BrowserSettings.Theme.DARK,
            fontSize = BrowserSettings.FontSize.LARGE,
            forceZoom = true,
            showImages = false,
            useDesktopMode = true,
            blockPopups = false,
            enableVoiceCommands = true,
            aiSummaries = true,
            aiTranslation = true,
            readAloud = true
        )

        assertEquals(BrowserSettings.Theme.DARK, settings.theme)
        assertEquals(BrowserSettings.FontSize.LARGE, settings.fontSize)
        assertTrue(settings.forceZoom)
        assertFalse(settings.showImages)
        assertTrue(settings.useDesktopMode)
        assertFalse(settings.blockPopups)
        assertTrue(settings.enableVoiceCommands)
        assertTrue(settings.aiSummaries)
        assertTrue(settings.aiTranslation)
        assertTrue(settings.readAloud)
    }
}