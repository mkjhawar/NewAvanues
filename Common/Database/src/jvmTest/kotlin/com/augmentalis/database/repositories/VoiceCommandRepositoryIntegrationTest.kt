/**
 * VoiceCommandRepositoryIntegrationTest.kt - Comprehensive VoiceCommand repository tests
 *
 * Tests all voice command operations with multi-locale support.
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database.repositories

import com.avanues.database.dto.VoiceCommandDTO
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class VoiceCommandRepositoryIntegrationTest : BaseRepositoryTest() {

    // ==================== CRUD Tests ====================

    @Test
    fun testCommandInsertAndGet() = runTest {
        val repo = databaseManager.voiceCommands
        val command = createCommand("cmd-001", "en-US", "open settings")

        val id = repo.insert(command)
        assertTrue(id > 0)

        val retrieved = repo.getById(id)
        assertNotNull(retrieved)
        assertEquals("cmd-001", retrieved.commandId)
        assertEquals("open settings", retrieved.triggerPhrase)
    }

    @Test
    fun testCommandUpdate() = runTest {
        val repo = databaseManager.voiceCommands
        val command = createCommand("cmd-001", "en-US", "original phrase")

        val id = repo.insert(command)

        repo.updateEnabledState(id, false)

        val disabled = repo.getById(id)
        assertEquals(0, disabled?.isEnabled)
    }

    @Test
    fun testCommandDeletion() = runTest {
        val repo = databaseManager.voiceCommands
        val id = repo.insert(createCommand("cmd-001", "en-US", "test"))

        assertNotNull(repo.getById(id))

        repo.delete(id)

        assertNull(repo.getById(id))
    }

    // ==================== Multi-Locale Tests ====================

    @Test
    fun testGetByCommandId() = runTest {
        val repo = databaseManager.voiceCommands

        repo.insert(createCommand("nav-home", "en-US", "go home"))
        repo.insert(createCommand("nav-home", "es-ES", "ir a inicio"))
        repo.insert(createCommand("nav-home", "fr-FR", "aller à accueil"))
        repo.insert(createCommand("nav-back", "en-US", "go back"))

        val allLocales = repo.getByCommandId("nav-home")
        assertEquals(3, allLocales.size)
        assertTrue(allLocales.all { it.commandId == "nav-home" })
    }

    @Test
    fun testGetByLocale() = runTest {
        val repo = databaseManager.voiceCommands

        repo.insert(createCommand("cmd-001", "en-US", "settings"))
        repo.insert(createCommand("cmd-002", "en-US", "home"))
        repo.insert(createCommand("cmd-003", "es-ES", "inicio"))

        val enCommands = repo.getByLocale("en-US")
        assertEquals(2, enCommands.size)
        assertTrue(enCommands.all { it.locale == "en-US" })
    }

    @Test
    fun testCountByLocale() = runTest {
        val repo = databaseManager.voiceCommands

        repo.insert(createCommand("cmd-001", "en-US", "one"))
        repo.insert(createCommand("cmd-002", "en-US", "two"))
        repo.insert(createCommand("cmd-003", "es-ES", "uno"))

        assertEquals(2, repo.countByLocale("en-US"))
        assertEquals(1, repo.countByLocale("es-ES"))
        assertEquals(0, repo.countByLocale("fr-FR"))
    }

    @Test
    fun testDeleteByCommandId() = runTest {
        val repo = databaseManager.voiceCommands

        repo.insert(createCommand("cmd-001", "en-US", "test"))
        repo.insert(createCommand("cmd-001", "es-ES", "prueba"))
        repo.insert(createCommand("cmd-002", "en-US", "other"))

        assertEquals(3, repo.count())

        repo.deleteByCommandId("cmd-001")

        assertEquals(1, repo.count())
        assertEquals(0, repo.getByCommandId("cmd-001").size)
    }

    // ==================== Category & Search Tests ====================

    @Test
    fun testGetByCategory() = runTest {
        val repo = databaseManager.voiceCommands

        repo.insert(createCommand("cmd-001", "en-US", "settings", category = "SYSTEM"))
        repo.insert(createCommand("cmd-002", "en-US", "home", category = "NAVIGATION"))
        repo.insert(createCommand("cmd-003", "en-US", "back", category = "NAVIGATION"))

        val navigation = repo.getByCategory("NAVIGATION")
        assertEquals(2, navigation.size)
        assertTrue(navigation.all { it.category == "NAVIGATION" })
    }

    @Test
    fun testGetEnabled() = runTest {
        val repo = databaseManager.voiceCommands

        repo.insert(createCommand("cmd-001", "en-US", "enabled1").copy(isEnabled = 1))
        repo.insert(createCommand("cmd-002", "en-US", "disabled").copy(isEnabled = 0))
        repo.insert(createCommand("cmd-003", "en-US", "enabled2").copy(isEnabled = 1))

        val enabled = repo.getEnabled()
        assertEquals(2, enabled.size)
        assertTrue(enabled.all { it.isEnabled == 1L })
    }

    @Test
    fun testSearchByTrigger() = runTest {
        val repo = databaseManager.voiceCommands

        repo.insert(createCommand("cmd-001", "en-US", "open settings"))
        repo.insert(createCommand("cmd-002", "en-US", "open browser"))
        repo.insert(createCommand("cmd-003", "en-US", "close window"))

        val openCommands = repo.searchByTrigger("%open%")
        assertEquals(2, openCommands.size)
        assertTrue(openCommands.all { it.triggerPhrase.contains("open") })
    }

    // ==================== Helpers ====================

    private fun createCommand(
        commandId: String,
        locale: String,
        triggerPhrase: String,
        action: String = "TEST_ACTION",
        category: String = "TEST",
        priority: Long = 0
    ): VoiceCommandDTO {
        val timestamp = now()
        return VoiceCommandDTO(
            id = 0,
            commandId = commandId,
            locale = locale,
            triggerPhrase = triggerPhrase,
            action = action,
            category = category,
            priority = priority,
            isEnabled = 1,
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }
}
