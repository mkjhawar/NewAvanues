/**
 * RenameCommandHandlerTest.kt - Unit tests for RenameCommandHandler
 * Path: apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/learnapp/commands/RenameCommandHandlerTest.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 *
 * Comprehensive unit tests for RenameCommandHandler covering:
 * - Command parsing (all 3 formats)
 * - Fuzzy matching (exact, partial, with/without action prefix)
 * - Synonym management (add, preserve original)
 * - Database operations (update verification)
 * - Error handling (not found, invalid format)
 */

package com.augmentalis.voiceoscore.learnapp.commands

import android.content.Context
import android.speech.tts.TextToSpeech
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RenameCommandHandler
 *
 * Test Coverage:
 * 1. Command parsing (3 patterns)
 * 2. Fuzzy matching (exact, partial, with/without prefix)
 * 3. Synonym addition
 * 4. Database update
 * 5. Error handling
 * 6. TTS feedback
 * 7. Edge cases
 */
class RenameCommandHandlerTest {

    private lateinit var context: Context
    private lateinit var database: VoiceOSDatabaseManager
    private lateinit var repository: IGeneratedCommandRepository
    private lateinit var tts: TextToSpeech
    private lateinit var handler: RenameCommandHandler

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        database = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        tts = mockk(relaxed = true)

        // Mock database.generatedCommands
        every { database.generatedCommands } returns repository

        handler = RenameCommandHandler(context, database, tts)
    }

    // ========================================
    // TEST GROUP 1: Command Parsing
    // ========================================

    @Test
    fun `test parse rename X to Y pattern`() {
        val result = handler.parseRenameCommand("Rename Button 1 to Save")

        assertNotNull(result)
        assertEquals("button 1", result?.oldName)
        assertEquals("save", result?.newName)
    }

    @Test
    fun `test parse rename X as Y pattern`() {
        val result = handler.parseRenameCommand("Rename Button 1 as Save")

        assertNotNull(result)
        assertEquals("button 1", result?.oldName)
        assertEquals("save", result?.newName)
    }

    @Test
    fun `test parse change X to Y pattern`() {
        val result = handler.parseRenameCommand("Change Button 1 to Save")

        assertNotNull(result)
        assertEquals("button 1", result?.oldName)
        assertEquals("save", result?.newName)
    }

    @Test
    fun `test parse invalid format returns null`() {
        val result = handler.parseRenameCommand("Button 1 Save")

        assertNull(result)
    }

    @Test
    fun `test parse with mixed case normalizes to lowercase`() {
        val result = handler.parseRenameCommand("RENAME BUTTON 1 TO SAVE")

        assertNotNull(result)
        assertEquals("button 1", result?.oldName)
        assertEquals("save", result?.newName)
    }

    @Test
    fun `test parse with extra whitespace trims correctly`() {
        val result = handler.parseRenameCommand("  Rename   Button 1   to   Save  ")

        assertNotNull(result)
        assertEquals("button 1", result?.oldName)
        assertEquals("save", result?.newName)
    }

    // ========================================
    // TEST GROUP 2: Fuzzy Matching
    // ========================================

    @Test
    fun `test find command by exact match`() = runBlocking {
        val command = createMockCommand(1, "click button 1", "click")
        coEvery { repository.getByPackage("com.example.app") } returns listOf(command)

        val result = handler.findCommandByName("click button 1", "com.example.app")

        assertNotNull(result)
        assertEquals("click button 1", result?.commandText)
    }

    @Test
    fun `test find command without action prefix`() = runBlocking {
        val command = createMockCommand(1, "click button 1", "click")
        coEvery { repository.getByPackage("com.example.app") } returns listOf(command)

        val result = handler.findCommandByName("button 1", "com.example.app")

        assertNotNull(result)
        assertEquals("click button 1", result?.commandText)
    }

    @Test
    fun `test find command with action prefix added`() = runBlocking {
        val command = createMockCommand(1, "click button 1", "click")
        coEvery { repository.getByPackage("com.example.app") } returns listOf(command)

        val result = handler.findCommandByName("button 1", "com.example.app")

        assertNotNull(result)
        assertEquals("click button 1", result?.commandText)
    }

    @Test
    fun `test find command case insensitive`() = runBlocking {
        val command = createMockCommand(1, "click button 1", "click")
        coEvery { repository.getByPackage("com.example.app") } returns listOf(command)

        val result = handler.findCommandByName("CLICK BUTTON 1", "com.example.app")

        assertNotNull(result)
        assertEquals("click button 1", result?.commandText)
    }

    @Test
    fun `test find command returns null when not found`() = runBlocking {
        coEvery { repository.getByPackage("com.example.app") } returns emptyList()

        val result = handler.findCommandByName("nonexistent", "com.example.app")

        assertNull(result)
    }

    @Test
    fun `test find command matches first occurrence when multiple exist`() = runBlocking {
        val command1 = createMockCommand(1, "click button 1", "click")
        val command2 = createMockCommand(2, "click button 2", "click")
        coEvery { repository.getByPackage("com.example.app") } returns listOf(command1, command2)

        val result = handler.findCommandByName("button 1", "com.example.app")

        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    // ========================================
    // TEST GROUP 3: Synonym Management
    // ========================================

    @Test
    fun `test add synonym to command with empty synonyms`() {
        val command = createMockCommand(1, "click button 1", "click", synonyms = null)

        val result = handler.addSynonym(command, "save")

        assertNotNull(result.synonyms)
        assertTrue(result.synonyms!!.contains("save"))
        assertTrue(result.synonyms!!.contains("button 1"))
    }

    @Test
    fun `test add synonym to command with existing synonyms`() {
        val command = createMockCommand(1, "click button 1", "click", synonyms = "submit")

        val result = handler.addSynonym(command, "save")

        assertNotNull(result.synonyms)
        assertTrue(result.synonyms!!.contains("save"))
        assertTrue(result.synonyms!!.contains("submit"))
        assertTrue(result.synonyms!!.contains("button 1"))
    }

    @Test
    fun `test add synonym preserves original command text as fallback`() {
        val command = createMockCommand(1, "click button 1", "click", synonyms = null)

        val result = handler.addSynonym(command, "save")

        assertTrue(result.synonyms!!.contains("button 1"))
    }

    @Test
    fun `test add synonym normalizes to lowercase`() {
        val command = createMockCommand(1, "click button 1", "click", synonyms = null)

        val result = handler.addSynonym(command, "SAVE")

        assertTrue(result.synonyms!!.contains("save"))
        assertFalse(result.synonyms!!.contains("SAVE"))
    }

    @Test
    fun `test add synonym removes duplicates`() {
        val command = createMockCommand(1, "click button 1", "click", synonyms = "save")

        val result = handler.addSynonym(command, "save")

        // Should only have "save" once in the set
        val synonymCount = result.synonyms!!.split(",").filter { it.trim() == "save" }.size
        assertEquals(1, synonymCount)
    }

    // ========================================
    // TEST GROUP 4: Integration Tests
    // ========================================

    @Test
    fun `test processRenameCommand success flow`() = runBlocking {
        val command = createMockCommand(1, "click button 1", "click", synonyms = null)
        coEvery { repository.getByPackage("com.example.app") } returns listOf(command)
        coEvery { repository.update(any()) } just Runs
        every { tts.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS

        val result = handler.processRenameCommand("Rename Button 1 to Save", "com.example.app")

        assertTrue(result is RenameResult.Success)
        val success = result as RenameResult.Success
        assertEquals("button 1", success.oldName)
        assertEquals("save", success.newName)
        assertNotNull(success.command.synonyms)
        assertTrue(success.command.synonyms!!.contains("save"))

        coVerify { repository.update(any()) }
        verify { tts.speak(any(), TextToSpeech.QUEUE_FLUSH, any(), "rename_success") }
    }

    @Test
    fun `test processRenameCommand fails when command not found`() = runBlocking {
        coEvery { repository.getByPackage("com.example.app") } returns emptyList()
        every { tts.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS

        val result = handler.processRenameCommand("Rename Button 99 to Save", "com.example.app")

        assertTrue(result is RenameResult.Error)
        assertEquals("Could not find command 'button 99'", (result as RenameResult.Error).message)

        coVerify(exactly = 0) { repository.update(any()) }
    }

    @Test
    fun `test processRenameCommand fails with invalid format`() = runBlocking {
        every { tts.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS

        val result = handler.processRenameCommand("Button 1 Save", "com.example.app")

        assertTrue(result is RenameResult.Error)
        assertEquals("Could not understand rename command", (result as RenameResult.Error).message)

        coVerify(exactly = 0) { repository.getByPackage(any()) }
        coVerify(exactly = 0) { repository.update(any()) }
    }

    @Test
    fun `test processRenameCommand handles database exception`() = runBlocking {
        val command = createMockCommand(1, "click button 1", "click")
        coEvery { repository.getByPackage("com.example.app") } returns listOf(command)
        coEvery { repository.update(any()) } throws RuntimeException("Database error")
        every { tts.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS

        val result = handler.processRenameCommand("Rename Button 1 to Save", "com.example.app")

        assertTrue(result is RenameResult.Error)
        assertTrue((result as RenameResult.Error).message.contains("Database error"))

        verify { tts.speak(match { it.contains("Failed to rename") }, TextToSpeech.QUEUE_FLUSH, any(), "rename_error") }
    }

    // ========================================
    // TEST GROUP 5: Edge Cases
    // ========================================

    @Test
    fun `test rename with multi-word synonym`() {
        val command = createMockCommand(1, "click button 1", "click", synonyms = null)

        val result = handler.addSynonym(command, "save button")

        assertTrue(result.synonyms!!.contains("save button"))
    }

    @Test
    fun `test rename with special characters in synonym`() {
        val command = createMockCommand(1, "click button 1", "click", synonyms = null)

        val result = handler.addSynonym(command, "save & submit")

        assertTrue(result.synonyms!!.contains("save & submit"))
    }

    @Test
    fun `test find command handles type action prefix`() = runBlocking {
        val command = createMockCommand(1, "type username", "type")
        coEvery { repository.getByPackage("com.example.app") } returns listOf(command)

        val result = handler.findCommandByName("username", "com.example.app")

        assertNotNull(result)
        assertEquals("type username", result?.commandText)
    }

    @Test
    fun `test find command handles scroll action prefix`() = runBlocking {
        val command = createMockCommand(1, "scroll down", "scroll")
        coEvery { repository.getByPackage("com.example.app") } returns listOf(command)

        val result = handler.findCommandByName("down", "com.example.app")

        assertNotNull(result)
        assertEquals("scroll down", result?.commandText)
    }

    // ========================================
    // Helper Methods
    // ========================================

    private fun createMockCommand(
        id: Long,
        commandText: String,
        actionType: String,
        synonyms: String? = null,
        confidence: Double = 0.9,
        isUserApproved: Long = 0,
        usageCount: Long = 0
    ): GeneratedCommandDTO {
        return GeneratedCommandDTO(
            id = id,
            elementHash = "hash_$id",
            commandText = commandText,
            actionType = actionType,
            confidence = confidence,
            synonyms = synonyms,
            isUserApproved = isUserApproved,
            usageCount = usageCount,
            lastUsed = null,
            createdAt = System.currentTimeMillis()
        )
    }
}
