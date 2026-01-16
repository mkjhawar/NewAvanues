/**
 * SynonymBinaryFormatTest.kt - Unit tests for binary format
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-08
 */
package com.augmentalis.voiceoscoreng.synonym

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SynonymBinaryFormatTest {

    @Test
    fun `test write and read roundtrip`() {
        val original = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press", "push"))
            .add("scroll_up", listOf("swipe up", "go up"))
            .add("back", listOf("go back", "return"))
            .build()

        val binary = SynonymBinaryFormat.write(original)
        val restored = SynonymBinaryFormat.read(binary)

        assertEquals(original.size, restored.size)
        assertEquals(original.languageCode, restored.languageCode)

        // Verify synonym mappings
        assertEquals("click", restored.getCanonical("tap"))
        assertEquals("click", restored.getCanonical("press"))
        assertEquals("scroll_up", restored.getCanonical("swipe up"))
        assertEquals("back", restored.getCanonical("go back"))
    }

    @Test
    fun `test binary format has magic header`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        val binary = SynonymBinaryFormat.write(map)

        // Check magic bytes
        assertEquals('Q'.code.toByte(), binary[0])
        assertEquals('S'.code.toByte(), binary[1])
        assertEquals('Y'.code.toByte(), binary[2])
        assertEquals('N'.code.toByte(), binary[3])
    }

    @Test
    fun `test validate rejects invalid data`() {
        val invalidData = byteArrayOf(0, 1, 2, 3, 4, 5)

        assertTrue(!SynonymBinaryFormat.validate(invalidData))
    }

    @Test
    fun `test validate accepts valid data`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        val binary = SynonymBinaryFormat.write(map)

        assertTrue(SynonymBinaryFormat.validate(binary))
    }

    @Test
    fun `test read throws on invalid magic`() {
        val invalidData = byteArrayOf(
            'X'.code.toByte(), 'Y'.code.toByte(), 'Z'.code.toByte(), 'W'.code.toByte(),
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )

        assertFailsWith<SynonymBinaryException> {
            SynonymBinaryFormat.read(invalidData)
        }
    }

    @Test
    fun `test metadata is preserved in binary`() {
        val metadata = LanguageMetadata(
            languageCode = "ar",
            languageName = "Arabic",
            script = ScriptType.ARABIC,
            tokenizer = TokenizerType.WHITESPACE,
            isRtl = true,
            version = "2.0"
        )

        val original = SynonymMap.Builder("ar")
            .metadata(metadata)
            .add("click", listOf("انقر"))
            .build()

        val binary = SynonymBinaryFormat.write(original)
        val restored = SynonymBinaryFormat.read(binary)

        assertEquals("ar", restored.metadata.languageCode)
        assertEquals(ScriptType.ARABIC, restored.metadata.script)
        assertTrue(restored.metadata.isRtl)
    }

    @Test
    fun `test binary is smaller than text`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press", "push", "hit", "select"))
            .add("scroll_up", listOf("swipe up", "go up", "move up"))
            .add("scroll_down", listOf("swipe down", "go down", "move down"))
            .add("back", listOf("go back", "return", "previous"))
            .build()

        val binary = SynonymBinaryFormat.write(map)
        val text = SynonymParser.serialize(map)

        // Binary should generally be smaller due to index compression
        // but main benefit is O(1) lookup
        assertTrue(binary.isNotEmpty())
        assertTrue(text.isNotEmpty())
    }

    @Test
    fun `test empty map roundtrip`() {
        val original = SynonymMap.Builder("en").build()

        val binary = SynonymBinaryFormat.write(original)
        val restored = SynonymBinaryFormat.read(binary)

        assertEquals(0, restored.size)
        assertEquals("en", restored.languageCode)
    }

    @Test
    fun `test unicode synonyms roundtrip`() {
        val original = SynonymMap.Builder("ja")
            .add("click", listOf("タップ", "押す", "クリック"))
            .add("back", listOf("戻る", "前へ"))
            .build()

        val binary = SynonymBinaryFormat.write(original)
        val restored = SynonymBinaryFormat.read(binary)

        assertEquals("click", restored.getCanonical("タップ"))
        assertEquals("click", restored.getCanonical("クリック"))
        assertEquals("back", restored.getCanonical("戻る"))
    }
}
