/**
 * ElementCommandManagerTest.kt - Unit tests for ElementCommandManager business logic
 *
 * Part of VOS-META-001 Phase 1 testing
 * Created: 2025-12-03
 *
 * Tests command validation, caching, synonym management, and quality metrics integration.
 */
package com.augmentalis.voiceoscore.commands

import app.cash.sqldelight.db.SqlDriver
import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.QualityMetricDTO
import com.augmentalis.database.repositories.impl.SQLDelightElementCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightQualityMetricRepository
import com.augmentalis.voiceoscore.test.infrastructure.TestDatabaseDriverFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ElementCommandManagerTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: VoiceOSDatabase
    private lateinit var databaseManager: VoiceOSDatabaseManager
    private lateinit var manager: ElementCommandManager

    @Before
    fun setup() {
        driver = TestDatabaseDriverFactory().createDriver()
        database = VoiceOSDatabase(driver)

        // Mock VoiceOSDatabaseManager with test repositories
        databaseManager = mockk(relaxed = true)
        every { databaseManager.elementCommands } returns SQLDelightElementCommandRepository(database)
        every { databaseManager.qualityMetrics } returns SQLDelightQualityMetricRepository(database)

        manager = ElementCommandManager(databaseManager)
    }

    @After
    fun teardown() {
        driver.close()
    }

    // ==================== Command Addition Tests ====================

    @Test
    fun `addCommand succeeds with valid phrase`() = runBlocking {
        val id = manager.addCommand(
            elementUuid = "uuid-1",
            commandPhrase = "submit button",
            appId = "com.example.app"
        )

        assertTrue("Should return valid ID", id > 0)
    }

    @Test
    fun `addCommand sanitizes phrase`() = runBlocking {
        val id = manager.addCommand(
            elementUuid = "uuid-1",
            commandPhrase = "  SUBMIT   Button  ", // Mixed case, extra spaces
            appId = "com.example.app"
        )

        assertTrue("Should accept and sanitize phrase", id > 0)

        val element = manager.findElementByCommand("submit button", "com.example.app")
        assertEquals("Should find by sanitized phrase", "uuid-1", element)
    }

    @Test
    fun `addCommand rejects phrase too short`() = runBlocking {
        val id = manager.addCommand(
            elementUuid = "uuid-1",
            commandPhrase = "ab", // Only 2 chars
            appId = "com.example.app"
        )

        assertEquals("Should reject short phrase", -1L, id)
    }

    @Test
    fun `addCommand rejects phrase too long`() = runBlocking {
        val id = manager.addCommand(
            elementUuid = "uuid-1",
            commandPhrase = "a".repeat(51), // 51 chars
            appId = "com.example.app"
        )

        assertEquals("Should reject long phrase", -1L, id)
    }

    @Test
    fun `addCommand rejects phrase with special characters`() = runBlocking {
        val id = manager.addCommand(
            elementUuid = "uuid-1",
            commandPhrase = "submit@button", // Contains @
            appId = "com.example.app"
        )

        assertEquals("Should reject special characters", -1L, id)
    }

    @Test
    fun `addCommand rejects duplicate phrase`() = runBlocking {
        manager.addCommand("uuid-1", "submit button", "com.example.app")

        val id2 = manager.addCommand("uuid-2", "submit button", "com.example.app")

        assertEquals("Should reject duplicate phrase", -1L, id2)
    }

    @Test
    fun `addCommand allows same phrase in different apps`() = runBlocking {
        val id1 = manager.addCommand("uuid-1", "submit", "com.app1.test")
        val id2 = manager.addCommand("uuid-2", "submit", "com.app2.test")

        assertTrue("Should allow in app1", id1 > 0)
        assertTrue("Should allow in app2", id2 > 0)
    }

    // ==================== Synonym Management Tests ====================

    @Test
    fun `first command is marked as primary`() = runBlocking {
        manager.addCommand("uuid-1", "submit button", "com.example.app")

        val commands = manager.getCommandsForElement("uuid-1")

        assertEquals("Should have 1 command", 1, commands.size)
        assertFalse("First command should be primary", commands[0].isSynonym)
    }

    @Test
    fun `second command is marked as synonym`() = runBlocking {
        manager.addCommand("uuid-1", "submit button", "com.example.app")
        manager.addCommand("uuid-1", "send button", "com.example.app")

        val commands = manager.getCommandsForElement("uuid-1")

        assertEquals("Should have 2 commands", 2, commands.size)
        assertTrue("Second command should be synonym",
            commands.any { it.commandPhrase == "send button" && it.isSynonym })
    }

    @Test
    fun `explicit synonym flag is respected`() = runBlocking {
        val id = manager.addCommand("uuid-1", "submit", "com.example.app", isSynonym = true)

        assertTrue("Should accept explicit synonym", id > 0)

        val commands = manager.getCommandsForElement("uuid-1")
        assertTrue("Should be marked as synonym", commands[0].isSynonym)
    }

    @Test
    fun `deleteSynonyms removes only synonyms`() = runBlocking {
        manager.addCommand("uuid-1", "submit", "com.example.app") // Primary
        manager.addCommand("uuid-1", "send", "com.example.app") // Synonym
        manager.addCommand("uuid-1", "go", "com.example.app") // Synonym

        manager.deleteSynonyms("uuid-1", "com.example.app")
        val commands = manager.getCommandsForElement("uuid-1")

        assertEquals("Should have only primary", 1, commands.size)
        assertEquals("submit", commands[0].commandPhrase)
    }

    // ==================== Command Lookup Tests ====================

    @Test
    fun `findElementByCommand returns correct UUID`() = runBlocking {
        manager.addCommand("uuid-1", "submit button", "com.example.app")

        val found = manager.findElementByCommand("submit button", "com.example.app")

        assertEquals("uuid-1", found)
    }

    @Test
    fun `findElementByCommand sanitizes search phrase`() = runBlocking {
        manager.addCommand("uuid-1", "submit button", "com.example.app")

        val found = manager.findElementByCommand("  SUBMIT   Button  ", "com.example.app")

        assertEquals("Should find with sanitized phrase", "uuid-1", found)
    }

    @Test
    fun `findElementByCommand returns null for non-existent`() = runBlocking {
        val found = manager.findElementByCommand("non-existent", "com.example.app")

        assertNull("Should return null", found)
    }

    @Test
    fun `findElementByCommand is app-scoped`() = runBlocking {
        manager.addCommand("uuid-1", "submit", "com.app1.test")

        val found = manager.findElementByCommand("submit", "com.app2.test")

        assertNull("Should not find in different app", found)
    }

    // ==================== Command Update Tests ====================

    @Test
    fun `updateCommand changes phrase`() = runBlocking {
        val id = manager.addCommand("uuid-1", "old phrase", "com.example.app")

        val success = manager.updateCommand(id, "new phrase", "com.example.app")

        assertTrue("Update should succeed", success)
        val found = manager.findElementByCommand("new phrase", "com.example.app")
        assertEquals("uuid-1", found)
    }

    @Test
    fun `updateCommand rejects invalid phrase`() = runBlocking {
        val id = manager.addCommand("uuid-1", "old phrase", "com.example.app")

        val success = manager.updateCommand(id, "ab", "com.example.app") // Too short

        assertFalse("Update should fail", success)
    }

    @Test
    fun `updateCommand rejects duplicate phrase`() = runBlocking {
        val id1 = manager.addCommand("uuid-1", "phrase one", "com.example.app")
        manager.addCommand("uuid-2", "phrase two", "com.example.app")

        val success = manager.updateCommand(id1, "phrase two", "com.example.app")

        assertFalse("Update should fail on duplicate", success)
    }

    // ==================== Command Deletion Tests ====================

    @Test
    fun `deleteCommand removes command`() = runBlocking {
        val id = manager.addCommand("uuid-1", "submit button", "com.example.app")

        manager.deleteCommand(id, "com.example.app")
        val found = manager.findElementByCommand("submit button", "com.example.app")

        assertNull("Command should be deleted", found)
    }

    // ==================== Quality Metrics Tests ====================

    @Test
    fun `storeQualityMetric stores metric`() = runBlocking {
        val metric = QualityMetricDTO(
            elementUuid = "uuid-1",
            appId = "com.example.app",
            qualityScore = 75,
            hasText = true,
            hasContentDesc = false,
            hasResourceId = true,
            lastAssessed = System.currentTimeMillis()
        )

        manager.storeQualityMetric(metric)
        val retrieved = manager.getQualityMetrics("com.example.app")

        assertEquals("Should have 1 metric", 1, retrieved.size)
        assertEquals(75, retrieved[0].qualityScore)
    }

    @Test
    fun `getQualityStats returns correct statistics`() = runBlocking {
        val appId = "com.example.app"

        // Add quality metrics
        manager.storeQualityMetric(createQualityMetric("uuid-1", appId, 85)) // EXCELLENT
        manager.storeQualityMetric(createQualityMetric("uuid-2", appId, 65)) // GOOD
        manager.storeQualityMetric(createQualityMetric("uuid-3", appId, 30)) // POOR

        val stats = manager.getQualityStats(appId)

        assertNotNull("Stats should not be null", stats)
        assertEquals(3, stats?.totalElements)
        assertEquals(1, stats?.excellentCount)
        assertEquals(1, stats?.goodCount)
        assertEquals(1, stats?.poorCount)
    }

    @Test
    fun `getPoorQualityElements returns only poor elements`() = runBlocking {
        val appId = "com.example.app"

        manager.storeQualityMetric(createQualityMetric("uuid-1", appId, 85))
        manager.storeQualityMetric(createQualityMetric("uuid-2", appId, 30))
        manager.storeQualityMetric(createQualityMetric("uuid-3", appId, 20))

        val poorElements = manager.getPoorQualityElements(appId)

        assertEquals("Should return 2 poor elements", 2, poorElements.size)
        assertTrue("All should be < 40", poorElements.all { it.qualityScore < 40 })
    }

    @Test
    fun `getElementsNeedingCommands returns elements without commands`() = runBlocking {
        val appId = "com.example.app"

        // Create quality metrics
        manager.storeQualityMetric(createQualityMetric("uuid-1", appId, 30, commandCount = 0))
        manager.storeQualityMetric(createQualityMetric("uuid-2", appId, 30, commandCount = 1))

        // Actually create a command for uuid-2 (so it has commands and won't be returned)
        manager.addCommand("uuid-2", "submit", appId)

        val elements = manager.getElementsNeedingCommands(appId)

        assertEquals("Should return 1 element", 1, elements.size)
        assertEquals("uuid-1", elements[0].elementUuid)
    }

    // ==================== Cache Management Tests ====================

    @Test
    fun `preloadCache loads commands into cache`() = runBlocking {
        val appId = "com.example.app"
        manager.addCommand("uuid-1", "submit", appId)
        manager.addCommand("uuid-2", "send", appId)

        manager.preloadCache(appId)

        val cache = manager.commandCache.first()
        assertTrue("Cache should contain app", cache.containsKey(appId))
        assertEquals("Should have 2 commands", 2, cache[appId]?.size)
    }

    @Test
    fun `clearCache removes app from cache`() = runBlocking {
        val appId = "com.example.app"
        manager.addCommand("uuid-1", "submit", appId)
        manager.preloadCache(appId)

        manager.clearCache(appId)

        val cache = manager.commandCache.first()
        assertFalse("Cache should not contain app", cache.containsKey(appId))
    }

    @Test
    fun `deleteApp removes all data for app`() = runBlocking {
        val appId = "com.example.app"
        manager.addCommand("uuid-1", "submit", appId)
        manager.storeQualityMetric(createQualityMetric("uuid-1", appId, 50))

        manager.deleteApp(appId)

        val commands = manager.getCommandsForApp(appId)
        val metrics = manager.getQualityMetrics(appId)

        assertEquals("Should have no commands", 0, commands.size)
        assertEquals("Should have no metrics", 0, metrics.size)
    }

    // ==================== App-Specific Tests ====================

    @Test
    fun `getCommandsForApp returns only app commands`() = runBlocking {
        manager.addCommand("uuid-1", "submit", "com.app1.test")
        manager.addCommand("uuid-2", "send", "com.app2.test")

        val commands = manager.getCommandsForApp("com.app1.test")

        assertEquals("Should return 1 command", 1, commands.size)
        assertEquals("com.app1.test", commands[0].appId)
    }

    // Helper function
    private fun createQualityMetric(
        elementUuid: String,
        appId: String,
        qualityScore: Int,
        commandCount: Int = 0
    ): QualityMetricDTO {
        return QualityMetricDTO(
            elementUuid = elementUuid,
            appId = appId,
            qualityScore = qualityScore,
            hasText = qualityScore >= 40,
            hasContentDesc = qualityScore >= 60,
            hasResourceId = qualityScore >= 80,
            commandCount = commandCount,
            manualCommandCount = commandCount,
            lastAssessed = System.currentTimeMillis()
        )
    }
}
