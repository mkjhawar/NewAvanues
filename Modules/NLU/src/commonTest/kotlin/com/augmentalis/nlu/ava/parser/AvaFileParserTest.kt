package com.augmentalis.nlu.ava.parser

import com.augmentalis.nlu.ava.model.AvaFile
import com.augmentalis.nlu.ava.model.AvaIntent
import org.junit.Assert.*
import org.junit.Test

/**
 * DEFEND Phase: AvaFileParser tests
 * Validates Universal Format v2.0 parsing logic
 */
class AvaFileParserTest {

    @Test
    fun `parse should handle valid Universal Format v2_0 with header`() {
        // Given
        val content = """
            # Avanues Universal Format v1.0
            # Type: AVA - Voice Intent Examples
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: test.ava
              category: navigation
              name: Navigation
              description: Test navigation intents
              priority: 1
              count: 2
            ---
            VCM:open_app:open gmail
            VCM:open_app:launch gmail
            VCM:open_settings:open settings
            ---
            synonyms:
              open: [launch, start]
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals("avu-1.0", result.schema)
        assertEquals("1.0.0", result.version)
        assertEquals("en-US", result.locale)
        assertEquals("test.ava", result.metadata.filename)
        assertEquals("navigation", result.metadata.category)
        assertEquals(2, result.intents.size)
        assertEquals(1, result.globalSynonyms.size)
    }

    @Test
    fun `parse should handle Universal Format without header comments`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: voice_command
              name: Test
              description: Test intents
            ---
            VCM:test_intent:test command
            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals("avu-1.0", result.schema)
        assertEquals("test.ava", result.metadata.filename)
        assertEquals(1, result.intents.size)
        assertTrue(result.globalSynonyms.isEmpty())
    }

    @Test
    fun `parse should extract IPC codes correctly`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: mixed
              name: Mixed
              description: Mixed IPC codes
            ---
            VCM:voice_cmd:turn on wifi
            AIQ:query_weather:what is the weather
            STT:dictation:hello world
            CTX:share_location:share my location
            SUG:suggestion:try this
            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals(5, result.intents.size)

        val vcmIntent = result.intents.find { it.id == "voice_cmd" }
        assertEquals("VCM", vcmIntent?.ipcCode)
        assertEquals("VCM:voice_cmd:turn on wifi", vcmIntent?.ipcTemplate)

        val aiqIntent = result.intents.find { it.id == "query_weather" }
        assertEquals("AIQ", aiqIntent?.ipcCode)

        val sttIntent = result.intents.find { it.id == "dictation" }
        assertEquals("STT", sttIntent?.ipcCode)
    }

    @Test
    fun `parse should group synonyms by intent ID`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: navigation
              name: Navigation
              description: Test
            ---
            VCM:open_app:open gmail
            VCM:open_app:launch gmail
            VCM:open_app:start gmail
            VCM:open_settings:open settings
            VCM:open_settings:launch settings
            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals(2, result.intents.size)

        val openAppIntent = result.intents.find { it.id == "open_app" }
        assertNotNull(openAppIntent)
        assertEquals("open gmail", openAppIntent?.canonical)
        assertEquals(2, openAppIntent?.synonyms?.size)
        assertTrue(openAppIntent?.synonyms?.contains("launch gmail") == true)
        assertTrue(openAppIntent?.synonyms?.contains("start gmail") == true)

        val openSettingsIntent = result.intents.find { it.id == "open_settings" }
        assertEquals(1, openSettingsIntent?.synonyms?.size)
    }

    @Test
    fun `parse should handle global synonyms section`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: test
              name: Test
              description: Test
            ---
            VCM:test:test command
            ---
            synonyms:
              open: [launch, start, begin]
              close: [exit, quit, end]
              increase: [up, raise, boost]
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals(3, result.globalSynonyms.size)
        assertEquals(listOf("launch", "start", "begin"), result.globalSynonyms["open"])
        assertEquals(listOf("exit", "quit", "end"), result.globalSynonyms["close"])
        assertEquals(listOf("up", "raise", "boost"), result.globalSynonyms["increase"])
    }

    @Test
    fun `parse should set source to UNIVERSAL_V2`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: test
              name: Test
              description: Test
            ---
            VCM:test:test command
            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals("UNIVERSAL_V2", result.intents[0].source)
    }

    @Test
    fun `parse should use defaults for missing metadata fields`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
            ---
            VCM:test:test command
            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals("voice_command", result.metadata.category)
        assertEquals("Unknown", result.metadata.name)
        assertEquals("", result.metadata.description)
        assertEquals(1, result.intents[0].priority)
    }

    @Test
    fun `parse should skip comment lines in entries section`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: test
              name: Test
              description: Test
            ---
            # This is a comment
            VCM:test1:first command
            # Another comment
            VCM:test2:second command
            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals(2, result.intents.size)
        assertEquals("test1", result.intents[0].id)
        assertEquals("test2", result.intents[1].id)
    }

    @Test
    fun `parse should skip blank lines`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: test
              name: Test
              description: Test
            ---
            VCM:test1:first command

            VCM:test2:second command

            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals(2, result.intents.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse should reject v1_0 JSON format`() {
        // Given - v1.0 JSON format (starts with {)
        val content = """
            {
              "s": "ava-1.0",
              "v": "1.0.0",
              "l": "en-US"
            }
        """.trimIndent()

        // When - Then should throw
        AvaFileParser.parse(content)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse should reject invalid format without sections`() {
        // Given
        val content = "invalid content"

        // When - Then should throw
        AvaFileParser.parse(content)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse should reject format with insufficient sections`() {
        // Given - Only 1 section (just header, no --- separators)
        val content = """
            schema: avu-1.0
            version: 1.0.0
        """.trimIndent()

        // When - Then should throw
        AvaFileParser.parse(content)
    }

    @Test
    fun `parse should handle entries with colon in text`() {
        // Given - Entry text contains colon
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: test
              name: Test
              description: Test
            ---
            VCM:time_query:what time is it: right now
            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals(1, result.intents.size)
        assertEquals("what time is it: right now", result.intents[0].canonical)
    }

    @Test
    fun `parse should preserve locale in intents`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: es-ES
            metadata:
              file: test.ava
              category: test
              name: Test
              description: Test
            ---
            VCM:abrir_app:abrir gmail
            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertEquals("es-ES", result.locale)
        assertEquals("es-ES", result.intents[0].locale)
    }

    @Test
    fun `parse should handle empty synonyms section`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: test
              name: Test
              description: Test
            ---
            VCM:test:test command
            ---
            synonyms:
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        assertTrue(result.globalSynonyms.isEmpty())
    }

    @Test
    fun `parse should set intentCount in metadata correctly`() {
        // Given
        val content = """
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            metadata:
              file: test.ava
              category: test
              name: Test
              description: Test
              count: 5
            ---
            VCM:test1:command 1
            VCM:test2:command 2
            VCM:test3:command 3
            ---
        """.trimIndent()

        // When
        val result = AvaFileParser.parse(content)

        // Then
        // intentCount should be actual count, not the count field in metadata
        assertEquals(3, result.metadata.intentCount)
    }
}
