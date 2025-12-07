/**
 * ElementCommandRepositoryTest.kt - Unit tests for element command repository
 *
 * Part of VOS-META-001 Phase 1 testing
 * Created: 2025-12-03
 *
 * Tests CRUD operations, queries, and edge cases for element command storage.
 */
package com.augmentalis.voiceoscore.commands

import app.cash.sqldelight.db.SqlDriver
import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ElementCommandDTO
import com.augmentalis.database.repositories.impl.SQLDelightElementCommandRepository
import com.augmentalis.voiceoscore.test.infrastructure.TestDatabaseDriverFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ElementCommandRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: VoiceOSDatabase
    private lateinit var repository: SQLDelightElementCommandRepository

    @Before
    fun setup() {
        driver = TestDatabaseDriverFactory().createDriver()
        database = VoiceOSDatabase(driver)
        repository = SQLDelightElementCommandRepository(database)
    }

    @After
    fun teardown() {
        driver.close()
    }

    @Test
    fun `insert command returns valid ID`() = runBlocking {
        val command = createCommand("submit button")

        val id = repository.insert(command)

        assertTrue("Insert should return positive ID", id > 0)
    }

    @Test
    fun `insert duplicate command returns error`() = runBlocking {
        val command1 = createCommand("submit button")
        val command2 = createCommand("submit button") // Duplicate

        repository.insert(command1)
        val id2 = repository.insert(command2)

        assertEquals("Duplicate insert should return -1", -1L, id2)
    }

    @Test
    fun `getByUuid returns all commands for element`() = runBlocking {
        val uuid = "test-uuid"
        val command1 = createCommand("submit", elementUuid = uuid)
        val command2 = createCommand("send", elementUuid = uuid, isSynonym = true)

        repository.insert(command1)
        repository.insert(command2)

        val commands = repository.getByUuid(uuid)

        assertEquals("Should return 2 commands", 2, commands.size)
    }

    @Test
    fun `getByUuid returns empty list when no commands`() = runBlocking {
        val commands = repository.getByUuid("non-existent")

        assertEquals("Should return empty list", 0, commands.size)
    }

    @Test
    fun `getByApp returns all commands for app`() = runBlocking {
        val appId = "com.example.app"
        val command1 = createCommand("submit", appId = appId, elementUuid = "uuid-1")
        val command2 = createCommand("send", appId = appId, elementUuid = "uuid-2")

        repository.insert(command1)
        repository.insert(command2)

        val commands = repository.getByApp(appId)

        assertEquals("Should return 2 commands", 2, commands.size)
    }

    @Test
    fun `getByApp does not return commands from other apps`() = runBlocking {
        val command1 = createCommand("submit", appId = "com.app1.test")
        val command2 = createCommand("send", appId = "com.app2.test")

        repository.insert(command1)
        repository.insert(command2)

        val commands = repository.getByApp("com.app1.test")

        assertEquals("Should return only 1 command", 1, commands.size)
        assertEquals("com.app1.test", commands[0].appId)
    }

    @Test
    fun `getByPhrase finds exact match`() = runBlocking {
        val command = createCommand("submit button")
        repository.insert(command)

        val found = repository.getByPhrase("submit button", "com.example.app")

        assertNotNull("Should find command", found)
        assertEquals("submit button", found?.commandPhrase)
    }

    @Test
    fun `getByPhrase returns null for non-existent phrase`() = runBlocking {
        val found = repository.getByPhrase("non-existent", "com.example.app")

        assertNull("Should return null", found)
    }

    @Test
    fun `getByPhrase is app-scoped`() = runBlocking {
        val command = createCommand("submit", appId = "com.app1.test")
        repository.insert(command)

        val found = repository.getByPhrase("submit", "com.app2.test")

        assertNull("Should not find command from different app", found)
    }

    @Test
    fun `delete removes command`() = runBlocking {
        val command = createCommand("submit button")
        val id = repository.insert(command)

        repository.delete(id)
        val found = repository.getByPhrase("submit button", "com.example.app")

        assertNull("Command should be deleted", found)
    }

    @Test
    fun `deleteSynonyms removes only synonyms`() = runBlocking {
        val uuid = "test-uuid"
        val primary = createCommand("submit", elementUuid = uuid, isSynonym = false)
        val synonym1 = createCommand("send", elementUuid = uuid, isSynonym = true)
        val synonym2 = createCommand("go", elementUuid = uuid, isSynonym = true)

        repository.insert(primary)
        repository.insert(synonym1)
        repository.insert(synonym2)

        repository.deleteSynonyms(uuid)
        val commands = repository.getByUuid(uuid)

        assertEquals("Should have only primary command", 1, commands.size)
        assertFalse("Remaining command should be primary", commands[0].isSynonym)
    }

    @Test
    fun `getAllForElement returns commands ordered by synonym status`() = runBlocking {
        val uuid = "test-uuid"
        val synonym = createCommand("send", elementUuid = uuid, isSynonym = true)
        val primary = createCommand("submit", elementUuid = uuid, isSynonym = false)

        repository.insert(synonym)
        repository.insert(primary)

        val commands = repository.getAllForElement(uuid)

        assertEquals("Should return 2 commands", 2, commands.size)
        assertFalse("First should be primary", commands[0].isSynonym)
        assertTrue("Second should be synonym", commands[1].isSynonym)
    }

    @Test
    fun `hasPrimaryCommand returns true when primary exists`() = runBlocking {
        val uuid = "test-uuid"
        val primary = createCommand("submit", elementUuid = uuid, isSynonym = false)
        repository.insert(primary)

        val hasPrimary = repository.hasPrimaryCommand(uuid)

        assertTrue("Should have primary command", hasPrimary)
    }

    @Test
    fun `hasPrimaryCommand returns false when only synonyms exist`() = runBlocking {
        val uuid = "test-uuid"
        val synonym = createCommand("send", elementUuid = uuid, isSynonym = true)
        repository.insert(synonym)

        val hasPrimary = repository.hasPrimaryCommand(uuid)

        assertFalse("Should not have primary command", hasPrimary)
    }

    @Test
    fun `countCommands returns correct count`() = runBlocking {
        val uuid = "test-uuid"
        repository.insert(createCommand("submit", elementUuid = uuid))
        repository.insert(createCommand("send", elementUuid = uuid, isSynonym = true))
        repository.insert(createCommand("go", elementUuid = uuid, isSynonym = true))

        val count = repository.countCommands(uuid)

        assertEquals("Should count 3 commands", 3L, count)
    }

    @Test
    fun `updateCommand changes phrase and confidence`() = runBlocking {
        val command = createCommand("old phrase")
        val id = repository.insert(command)

        repository.updateCommand(id, "new phrase", 0.95)
        val updated = repository.getByPhrase("new phrase", "com.example.app")

        assertNotNull("Should find updated command", updated)
        assertEquals("new phrase", updated?.commandPhrase)
        assertEquals(0.95, updated?.confidence ?: 0.0, 0.001)
    }

    @Test
    fun `deleteByApp removes all commands for app`() = runBlocking {
        val appId = "com.example.app"
        repository.insert(createCommand("submit", appId = appId))
        repository.insert(createCommand("send", appId = appId))

        repository.deleteByApp(appId)
        val commands = repository.getByApp(appId)

        assertEquals("Should have no commands", 0, commands.size)
    }

    @Test
    fun `deleteByApp does not affect other apps`() = runBlocking {
        repository.insert(createCommand("submit", appId = "com.app1.test"))
        repository.insert(createCommand("send", appId = "com.app2.test"))

        repository.deleteByApp("com.app1.test")
        val commands = repository.getByApp("com.app2.test")

        assertEquals("Should still have command", 1, commands.size)
    }

    @Test
    fun `getAll returns all commands across apps`() = runBlocking {
        repository.insert(createCommand("submit", appId = "com.app1.test"))
        repository.insert(createCommand("send", appId = "com.app2.test"))

        val commands = repository.getAll()

        assertEquals("Should return 2 commands", 2, commands.size)
    }

    // Helper function
    private fun createCommand(
        phrase: String,
        elementUuid: String = "test-uuid",
        appId: String = "com.example.app",
        isSynonym: Boolean = false
    ): ElementCommandDTO {
        return ElementCommandDTO(
            elementUuid = elementUuid,
            commandPhrase = phrase,
            confidence = 1.0,
            createdAt = System.currentTimeMillis(),
            createdBy = "user",
            isSynonym = isSynonym,
            appId = appId
        )
    }
}
