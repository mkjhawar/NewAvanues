package com.augmentalis.httpavanue

import com.augmentalis.httpavanue.voice.VoiceRouteConfig
import com.augmentalis.httpavanue.voice.VoiceRouteEntry
import com.augmentalis.httpavanue.voice.VoiceRouteExporter
import com.augmentalis.httpavanue.voice.VoiceRouteRegistry
import kotlin.test.*

class VoiceRouteTest {

    @BeforeTest
    fun setup() {
        VoiceRouteRegistry.clear()
    }

    @Test
    fun testVoiceRouteExporterFormat() {
        VoiceRouteRegistry.register("/api/status", "GET", VoiceRouteConfig(
            phrase = "show status",
            aliases = listOf("check status", "what's the status"),
            category = "API",
            description = "Shows server status",
        ))

        val vos = VoiceRouteExporter.toVosString()
        assertTrue(vos.contains("API|show status|HTTP_GET|GET /api/status|en-US"))
        assertTrue(vos.contains("API|check status|HTTP_GET|GET /api/status|en-US"))
        assertTrue(vos.contains("API|what's the status|HTTP_GET|GET /api/status|en-US"))
    }

    @Test
    fun testMultipleVoiceRoutes() {
        VoiceRouteRegistry.register("/api/users", "GET", VoiceRouteConfig(phrase = "list users"))
        VoiceRouteRegistry.register("/api/users", "POST", VoiceRouteConfig(phrase = "create user"))

        val entries = VoiceRouteRegistry.getAll()
        assertEquals(2, entries.size)
        assertEquals("list users", entries[0].config.phrase)
        assertEquals("create user", entries[1].config.phrase)
    }

    @Test
    fun testVosFormatHeader() {
        val vos = VoiceRouteExporter.toVosString(emptyList())
        assertTrue(vos.contains("VOS Compact v3.0"))
    }

    @Test
    fun testCustomLocale() {
        VoiceRouteRegistry.register("/api/status", "GET", VoiceRouteConfig(phrase = "zeige status"))
        val vos = VoiceRouteExporter.toVosString(locale = "de-DE")
        assertTrue(vos.contains("|de-DE"))
    }
}
