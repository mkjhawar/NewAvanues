package com.augmentalis.voiceoscoreng.avu

import com.augmentalis.voiceoscoreng.common.AVUSerializer
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ElementType
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.common.QuantizedContext
import com.augmentalis.voiceoscoreng.common.QuantizedElement
import com.augmentalis.voiceoscoreng.common.QuantizedScreen
import com.augmentalis.voiceoscoreng.functions.getCurrentTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AVU Serializer Tests
 *
 * TDD tests for AVU format serialization and parsing.
 */
class AVUSerializerTest {

    // ==================== Serialization Tests ====================

    @Test
    fun `serialize generates valid AVU header`() {
        val context = createSampleContext()
        val output = AVUSerializer.serialize(context)

        assertTrue(output.contains("# Avanues Universal Format v1.0"))
        assertTrue(output.contains("schema: avu-1.0"))
        assertTrue(output.contains("version: 1.0.0"))
    }

    @Test
    fun `serialize includes metadata section`() {
        val context = createSampleContext()
        val output = AVUSerializer.serialize(context)

        assertTrue(output.contains("metadata:"))
        assertTrue(output.contains("file: com.example.app.vos"))
        assertTrue(output.contains("category: learned_app"))
    }

    @Test
    fun `serialize generates APP line`() {
        val context = createSampleContext()
        val output = AVUSerializer.serialize(context)

        assertTrue(output.contains("APP:com.example.app:Example App:"))
    }

    @Test
    fun `serialize generates STA line`() {
        val context = createSampleContext()
        val stats = ExplorationStats(
            screenCount = 2,
            elementCount = 5,
            commandCount = 3,
            avgDepth = 2.5f,
            maxDepth = 4,
            coverage = 0.75f
        )
        val output = AVUSerializer.serialize(context, stats)

        assertTrue(output.contains("STA:2:5:3:2.50:4:0.75"))
    }

    @Test
    fun `serialize generates SCR lines for each screen`() {
        val context = createSampleContext()
        val output = AVUSerializer.serialize(context)

        assertTrue(output.contains("SCR:screen1:"))
        assertTrue(output.contains("SCR:screen2:"))
    }

    @Test
    fun `serialize generates ELM lines for elements`() {
        val context = createSampleContext()
        val output = AVUSerializer.serialize(context)

        assertTrue(output.contains("ELM:"))
        assertTrue(output.contains("Login"))
        assertTrue(output.contains("Settings"))
    }

    @Test
    fun `serialize generates NAV lines for navigation`() {
        val context = createSampleContext()
        val output = AVUSerializer.serialize(context)

        assertTrue(output.contains("NAV:screen1:screen2:"))
    }

    @Test
    fun `serialize generates CMD lines for commands`() {
        val context = createSampleContext()
        val output = AVUSerializer.serialize(context)

        assertTrue(output.contains("CMD:"))
        assertTrue(output.contains("tap login:CLICK"))
    }

    @Test
    fun `serialize has correct section delimiters`() {
        val context = createSampleContext()
        val output = AVUSerializer.serialize(context)
        val dashes = output.split("---")

        // Header, schema, data sections
        assertTrue(dashes.size >= 3)
    }

    // ==================== Parsing Tests ====================

    @Test
    fun `parse extracts package name`() {
        val avu = createSampleAVU()
        val result = AVUSerializer.parse(avu)

        assertNotNull(result)
        assertEquals("com.example.app", result.packageName)
    }

    @Test
    fun `parse extracts app name`() {
        val avu = createSampleAVU()
        val result = AVUSerializer.parse(avu)

        assertNotNull(result)
        assertEquals("Example App", result.appName)
    }

    @Test
    fun `parse extracts screens`() {
        val avu = createSampleAVU()
        val result = AVUSerializer.parse(avu)

        assertNotNull(result)
        assertEquals(2, result.screens.size)
    }

    @Test
    fun `parse extracts elements for each screen`() {
        val avu = createSampleAVU()
        val result = AVUSerializer.parse(avu)

        assertNotNull(result)
        val screen = result.screens.find { it.screenHash == "screen1" }
        assertNotNull(screen)
        assertTrue(screen.elements.isNotEmpty())
    }

    @Test
    fun `parse extracts navigation`() {
        val avu = createSampleAVU()
        val result = AVUSerializer.parse(avu)

        assertNotNull(result)
        assertTrue(result.navigation.isNotEmpty())
        assertEquals("screen1", result.navigation[0].fromScreenHash)
        assertEquals("screen2", result.navigation[0].toScreenHash)
    }

    @Test
    fun `parse extracts commands`() {
        val avu = createSampleAVU()
        val result = AVUSerializer.parse(avu)

        assertNotNull(result)
        assertTrue(result.knownCommands.isNotEmpty())
        assertEquals("tap login", result.knownCommands[0].phrase)
    }

    @Test
    fun `parse returns null for empty input`() {
        val result = AVUSerializer.parse("")
        assertNull(result)
    }

    @Test
    fun `parse returns null for invalid format`() {
        val result = AVUSerializer.parse("invalid content without AVU format")
        assertNull(result)
    }

    @Test
    fun `parse handles missing sections gracefully`() {
        val minimalAvu = """
            # Avanues Universal Format v1.0
            ---
            schema: avu-1.0
            ---
            APP:com.minimal:Minimal:0
        """.trimIndent()

        val result = AVUSerializer.parse(minimalAvu)
        assertNotNull(result)
        assertEquals("com.minimal", result.packageName)
        assertTrue(result.screens.isEmpty())
    }

    // ==================== Stats Parsing Tests ====================

    @Test
    fun `parseStats extracts all statistics`() {
        val avu = createSampleAVU()
        val stats = AVUSerializer.parseStats(avu)

        assertNotNull(stats)
        assertEquals(2, stats.screenCount)
        assertEquals(3, stats.elementCount)
        assertEquals(1, stats.commandCount)
    }

    @Test
    fun `parseStats returns null for missing STA line`() {
        val noStats = """
            # Avanues Universal Format v1.0
            ---
            schema: avu-1.0
            ---
            APP:com.test:Test:0
        """.trimIndent()

        val stats = AVUSerializer.parseStats(noStats)
        assertNull(stats)
    }

    // ==================== Roundtrip Tests ====================

    @Test
    fun `roundtrip preserves package name`() {
        val original = createSampleContext()
        val serialized = AVUSerializer.serialize(original)
        val parsed = AVUSerializer.parse(serialized)

        assertNotNull(parsed)
        assertEquals(original.packageName, parsed.packageName)
    }

    @Test
    fun `roundtrip preserves app name`() {
        val original = createSampleContext()
        val serialized = AVUSerializer.serialize(original)
        val parsed = AVUSerializer.parse(serialized)

        assertNotNull(parsed)
        assertEquals(original.appName, parsed.appName)
    }

    @Test
    fun `roundtrip preserves screen count`() {
        val original = createSampleContext()
        val serialized = AVUSerializer.serialize(original)
        val parsed = AVUSerializer.parse(serialized)

        assertNotNull(parsed)
        assertEquals(original.screens.size, parsed.screens.size)
    }

    @Test
    fun `roundtrip preserves navigation count`() {
        val original = createSampleContext()
        val serialized = AVUSerializer.serialize(original)
        val parsed = AVUSerializer.parse(serialized)

        assertNotNull(parsed)
        assertEquals(original.navigation.size, parsed.navigation.size)
    }

    @Test
    fun `roundtrip preserves command count`() {
        val original = createSampleContext()
        val serialized = AVUSerializer.serialize(original)
        val parsed = AVUSerializer.parse(serialized)

        assertNotNull(parsed)
        assertEquals(original.knownCommands.size, parsed.knownCommands.size)
    }

    // ==================== Validation Tests ====================

    @Test
    fun `isValidAVU returns true for valid format`() {
        val avu = createSampleAVU()
        assertTrue(AVUSerializer.isValidAVU(avu))
    }

    @Test
    fun `isValidAVU returns false for empty string`() {
        assertFalse(AVUSerializer.isValidAVU(""))
    }

    @Test
    fun `isValidAVU returns false for missing header`() {
        val noHeader = """
            schema: avu-1.0
            ---
            APP:com.test:Test:0
        """.trimIndent()

        assertFalse(AVUSerializer.isValidAVU(noHeader))
    }

    @Test
    fun `isValidAVU returns false for missing schema`() {
        val noSchema = """
            # Avanues Universal Format v1.0
            ---
            ---
            APP:com.test:Test:0
        """.trimIndent()

        assertFalse(AVUSerializer.isValidAVU(noSchema))
    }

    // ==================== Helper Methods ====================

    private fun createSampleContext(): QuantizedContext {
        val elements1 = listOf(
            QuantizedElement("v1", ElementType.BUTTON, "Login", actions = "click"),
            QuantizedElement("v2", ElementType.TEXT_FIELD, "Username")
        )
        val elements2 = listOf(
            QuantizedElement("v3", ElementType.BUTTON, "Settings", actions = "click")
        )

        val screens = listOf(
            QuantizedScreen("screen1", "Login Screen", "LoginActivity", elements1),
            QuantizedScreen("screen2", "Home Screen", "HomeActivity", elements2)
        )

        val navigation = listOf(
            QuantizedNavigation("screen1", "screen2", "Login", "v1")
        )

        val commands = listOf(
            QuantizedCommand("cmd1", "tap login", CommandActionType.CLICK, "v1", 0.95f)
        )

        return QuantizedContext(
            packageName = "com.example.app",
            appName = "Example App",
            versionCode = 1L,
            versionName = "1.0.0",
            generatedAt = getCurrentTimeMillis(),
            screens = screens,
            navigation = navigation,
            vocabulary = setOf("Login", "Username", "Settings"),
            knownCommands = commands
        )
    }

    private fun createSampleAVU(): String = """
        # Avanues Universal Format v1.0
        # Type: VOS
        ---
        schema: avu-1.0
        version: 1.0.0
        locale: en-US
        project: voiceos
        metadata:
          file: com.example.app.vos
          category: learned_app
          count: 7
        ---
        APP:com.example.app:Example App:1234567890
        STA:2:3:1:2.50:4:0.75
        SCR:screen1:LoginActivity:1234567890:2
        ELM:v1:Login:BUTTON:click:0,0,100,50:action
        ELM:v2:Username:TEXT_FIELD::0,60,100,100:input
        SCR:screen2:HomeActivity:1234567891:1
        ELM:v3:Settings:BUTTON:click:0,0,100,50:action
        NAV:screen1:screen2:v1:Login:1234567890
        CMD:cmd1:tap login:CLICK:v1:0.95
    """.trimIndent()
}
