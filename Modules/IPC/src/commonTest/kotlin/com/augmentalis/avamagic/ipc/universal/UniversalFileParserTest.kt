package com.augmentalis.avamagic.ipc.universal

import com.augmentalis.avamagic.ipc.AIQueryMessage
import com.augmentalis.avamagic.ipc.VoiceCommandMessage
import kotlin.test.*

/**
 * Comprehensive test suite for UniversalFileParser
 *
 * Tests all file formats: .ava, .vos, .avc, .awb, .ami
 */
class UniversalFileParserTest {

    @Test
    fun `parse AVA file format`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AVA
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: test.ava
              category: voice_command
              count: 2
            ---
            VCM:open_gmail:open gmail
            AIQ:weather:what's the weather
            ---
            synonyms:
              open: [launch, start]
        """.trimIndent()

        val file = UniversalFileParser.parse(content)

        assertEquals(FileType.AVA, file.type)
        assertEquals(".ava", file.extension)
        assertEquals("avu-1.0", file.schema)
        assertEquals("1.0.0", file.version)
        assertEquals("en-US", file.locale)
        assertEquals("ava", file.project)
        assertEquals(2, file.entries.size)
        assertEquals("VCM", file.entries[0].code)
        assertEquals("open_gmail", file.entries[0].id)
        assertEquals("open gmail", file.entries[0].data)
        assertEquals(listOf("launch", "start"), file.synonyms["open"])
    }

    @Test
    fun `parse VOS file format`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: VOS
            # Extension: .vos
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: voiceos
            metadata:
              file: accessibility.vos
              category: accessibility
              count: 3
              requires_permission: [ACCESSIBILITY_SERVICE]
            ---
            VCM:tap_button:tap on button login
            VCM:scroll_down:scroll down
            VCM:go_back:go back
            ---
        """.trimIndent()

        val file = UniversalFileParser.parse(content)

        assertEquals(FileType.VOS, file.type)
        assertEquals(".vos", file.extension)
        assertEquals("voiceos", file.project)
        assertEquals(3, file.entries.size)
    }

    @Test
    fun `parse AVC file format`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AVC
            # Extension: .avc
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: avaconnect
            metadata:
              file: video-call.avc
              category: communication
              count: 4
              feature: video-call
            ---
            VCA:call1:Pixel7:Manoj
            ACC:call1
            MIC:call1:1
            DIS:call1:User ended call
            ---
        """.trimIndent()

        val file = UniversalFileParser.parse(content)

        assertEquals(FileType.AVC, file.type)
        assertEquals(".avc", file.extension)
        assertEquals("avaconnect", file.project)
        assertEquals(4, file.entries.size)
    }

    @Test
    fun `parse AWB file format`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AWB
            # Extension: .awb
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: webavanue
            metadata:
              file: browser.awb
              category: browser_control
              count: 3
            ---
            URL:google:https://google.com
            NAV:back:go back
            TAB:new:new tab
            ---
        """.trimIndent()

        val file = UniversalFileParser.parse(content)

        assertEquals(FileType.AWB, file.type)
        assertEquals(".awb", file.extension)
        assertEquals("webavanue", file.project)
        assertEquals(3, file.entries.size)
    }

    @Test
    fun `parse AMI file format`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AMI
            # Extension: .ami
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: newavanue
            metadata:
              file: platform.ami
              category: platform
              count: 2
            ---
            HND:init_ava:ava:2.0:device1
            CAP:caps_ava:ava:video,screen,file
            ---
        """.trimIndent()

        val file = UniversalFileParser.parse(content)

        assertEquals(FileType.AMI, file.type)
        assertEquals(".ami", file.extension)
        assertEquals("newavanue", file.project)
        assertEquals(2, file.entries.size)
    }

    @Test
    fun `parse AMI file format with UI components`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AMI
            # Extension: .ami
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: avanues
            metadata:
              file: ui-components.ami
              category: ui_component
              count: 1
            ---
            JSN:call_prompt:Col{Text{text:"Incoming call"}}
            ---
        """.trimIndent()

        val file = UniversalFileParser.parse(content)

        assertEquals(FileType.AMI, file.type)
        assertEquals(".ami", file.extension)
        assertEquals("avanues", file.project)
        assertEquals(1, file.entries.size)
    }

    @Test
    fun `filter entries by code`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AVA
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: mixed.ava
              count: 4
            ---
            VCM:cmd1:command 1
            AIQ:q1:query 1
            VCM:cmd2:command 2
            URL:u1:https://example.com
            ---
        """.trimIndent()

        val file = UniversalFileParser.parse(content)
        val vcmEntries = file.filterByCode("VCM")

        assertEquals(2, vcmEntries.size)
        assertEquals("cmd1", vcmEntries[0].id)
        assertEquals("cmd2", vcmEntries[1].id)
    }

    @Test
    fun `get entry by ID`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AVA
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: test.ava
              count: 2
            ---
            VCM:open_gmail:open gmail
            AIQ:weather:what's the weather
            ---
        """.trimIndent()

        val file = UniversalFileParser.parse(content)
        val entry = file.getEntryById("open_gmail")

        assertNotNull(entry)
        assertEquals("VCM", entry.code)
        assertEquals("open gmail", entry.data)
    }

    @Test
    fun `convert entry to IPC message`() {
        val entry = UniversalEntry("VCM", "open_gmail", "open gmail")
        val message = entry.toIPCMessage("cmd123")

        assertTrue(message is VoiceCommandMessage)
        assertEquals("cmd123", (message as VoiceCommandMessage).commandId)
        assertEquals("open gmail", message.command)
    }

    @Test
    fun `convert all entries to IPC messages`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AVA
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: test.ava
              count: 2
            ---
            VCM:open_gmail:open gmail
            AIQ:weather:what's the weather
            ---
        """.trimIndent()

        val file = UniversalFileParser.parse(content)
        val messages = file.toIPCMessages()

        assertEquals(2, messages.size)
        assertTrue(messages[0] is VoiceCommandMessage)
        assertTrue(messages[1] is AIQueryMessage)
    }

    @Test
    fun `project-specific readers validate file type`() {
        val avaContent = """
            # Avanues Universal Format v1.0
            # Type: AVA
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: test.ava
              count: 1
            ---
            VCM:test:test command
            ---
        """.trimIndent()

        val avaReader = AvaFileReader()
        val file = avaReader.load(avaContent)
        assertEquals(FileType.AVA, file.type)

        // Try to load AVA file with VOS reader (should fail)
        val vosReader = VosFileReader()
        assertFails {
            vosReader.load(avaContent)
        }
    }

    @Test
    fun `parse file without synonyms section`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AVA
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: test.ava
              count: 1
            ---
            VCM:test:test command
        """.trimIndent()

        val file = UniversalFileParser.parse(content)

        assertEquals(1, file.entries.size)
        assertTrue(file.synonyms.isEmpty())
    }

    @Test
    fun `parse metadata with various types`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AVA
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: test.ava
              count: 5
              priority: 1
              enabled: true
              tags: [voice, navigation, test]
            ---
            VCM:test:test command
            ---
        """.trimIndent()

        val file = UniversalFileParser.parse(content)

        assertEquals(5, file.metadata["count"])
        assertEquals(1, file.metadata["priority"])
        assertEquals(true, file.metadata["enabled"])
        assertTrue(file.metadata["tags"] is List<*>)
    }

    @Test
    fun `fail on invalid format`() {
        val invalidContent = "This is not a valid file"

        assertFails {
            UniversalFileParser.parse(invalidContent)
        }
    }

    @Test
    fun `fail on missing header`() {
        val content = """
            ---
            schema: avu-1.0
            ---
            VCM:test:test
            ---
        """.trimIndent()

        assertFails {
            UniversalFileParser.parse(content)
        }
    }

    @Test
    fun `fail on invalid entry format`() {
        val content = """
            # Avanues Universal Format v1.0
            # Type: AVA
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: test.ava
              count: 1
            ---
            INVALID_ENTRY_NO_COLON
            ---
        """.trimIndent()

        assertFails {
            UniversalFileParser.parse(content)
        }
    }

    @Test
    fun `round trip parse and serialize`() {
        val original = """
            # Avanues Universal Format v1.0
            # Type: AVA
            # Extension: .ava
            ---
            schema: avu-1.0
            version: 1.0.0
            locale: en-US
            project: ava
            metadata:
              file: test.ava
              count: 2
            ---
            VCM:open_gmail:open gmail
            AIQ:weather:what's the weather
            ---
            synonyms:
              open: [launch, start]
        """.trimIndent()

        val parsed = UniversalFileParser.parse(original)

        // Verify all data preserved
        assertEquals("avu-1.0", parsed.schema)
        assertEquals("1.0.0", parsed.version)
        assertEquals(2, parsed.entries.size)
        assertEquals(1, parsed.synonyms.size)
    }
}
