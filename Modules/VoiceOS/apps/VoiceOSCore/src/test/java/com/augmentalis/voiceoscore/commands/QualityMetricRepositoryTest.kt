/**
 * QualityMetricRepositoryTest.kt - Unit tests for quality metric repository
 *
 * Part of VOS-META-001 Phase 1 testing
 * Created: 2025-12-03
 *
 * Tests quality metric storage, queries, and statistics generation.
 */
package com.augmentalis.voiceoscore.commands

import app.cash.sqldelight.db.SqlDriver
import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.QualityMetricDTO
import com.augmentalis.database.repositories.impl.SQLDelightQualityMetricRepository
import com.augmentalis.voiceoscore.test.infrastructure.TestDatabaseDriverFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class QualityMetricRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: VoiceOSDatabase
    private lateinit var repository: SQLDelightQualityMetricRepository

    @Before
    fun setup() {
        driver = TestDatabaseDriverFactory().createDriver()
        database = VoiceOSDatabase(driver)
        repository = SQLDelightQualityMetricRepository(database)
    }

    @After
    fun teardown() {
        driver.close()
    }

    @Test
    fun `insertOrUpdate creates new metric`() = runBlocking {
        val metric = createMetric("uuid-1", qualityScore = 75)

        repository.insertOrUpdate(metric)
        val retrieved = repository.getByUuid("uuid-1")

        assertNotNull("Metric should be created", retrieved)
        assertEquals(75, retrieved?.qualityScore)
    }

    @Test
    fun `insertOrUpdate updates existing metric`() = runBlocking {
        val metric1 = createMetric("uuid-1", qualityScore = 50)
        val metric2 = createMetric("uuid-1", qualityScore = 75)

        repository.insertOrUpdate(metric1)
        repository.insertOrUpdate(metric2)
        val retrieved = repository.getByUuid("uuid-1")

        assertEquals("Should update to new score", 75, retrieved?.qualityScore)
    }

    @Test
    fun `getByApp returns all metrics for app`() = runBlocking {
        val appId = "com.example.app"
        repository.insertOrUpdate(createMetric("uuid-1", appId = appId))
        repository.insertOrUpdate(createMetric("uuid-2", appId = appId))

        val metrics = repository.getByApp(appId)

        assertEquals("Should return 2 metrics", 2, metrics.size)
    }

    @Test
    fun `getByApp does not return metrics from other apps`() = runBlocking {
        repository.insertOrUpdate(createMetric("uuid-1", appId = "com.app1.test"))
        repository.insertOrUpdate(createMetric("uuid-2", appId = "com.app2.test"))

        val metrics = repository.getByApp("com.app1.test")

        assertEquals("Should return only 1 metric", 1, metrics.size)
        assertEquals("com.app1.test", metrics[0].appId)
    }

    @Test
    fun `getByApp orders by quality score ascending`() = runBlocking {
        val appId = "com.example.app"
        repository.insertOrUpdate(createMetric("uuid-1", appId = appId, qualityScore = 80))
        repository.insertOrUpdate(createMetric("uuid-2", appId = appId, qualityScore = 30))
        repository.insertOrUpdate(createMetric("uuid-3", appId = appId, qualityScore = 50))

        val metrics = repository.getByApp(appId)

        assertEquals("First should be lowest", 30, metrics[0].qualityScore)
        assertEquals("Second should be middle", 50, metrics[1].qualityScore)
        assertEquals("Third should be highest", 80, metrics[2].qualityScore)
    }

    @Test
    fun `getPoorQualityElements returns only score below 40`() = runBlocking {
        val appId = "com.example.app"
        repository.insertOrUpdate(createMetric("uuid-1", appId = appId, qualityScore = 20))
        repository.insertOrUpdate(createMetric("uuid-2", appId = appId, qualityScore = 39))
        repository.insertOrUpdate(createMetric("uuid-3", appId = appId, qualityScore = 40))
        repository.insertOrUpdate(createMetric("uuid-4", appId = appId, qualityScore = 60))

        val poorElements = repository.getPoorQualityElements(appId)

        assertEquals("Should return 2 poor elements", 2, poorElements.size)
        assertTrue("All should be < 40", poorElements.all { it.qualityScore < 40 })
    }

    @Test
    fun `getElementsWithoutCommands returns elements with score below 40 and no commands`() = runBlocking {
        val appId = "com.example.app"
        repository.insertOrUpdate(createMetric("uuid-1", appId = appId, qualityScore = 30, commandCount = 0))
        repository.insertOrUpdate(createMetric("uuid-2", appId = appId, qualityScore = 30, commandCount = 1))
        repository.insertOrUpdate(createMetric("uuid-3", appId = appId, qualityScore = 50, commandCount = 0))

        val elements = repository.getElementsWithoutCommands(appId)

        assertEquals("Should return 1 element", 1, elements.size)
        assertEquals("uuid-1", elements[0].elementUuid)
    }

    @Test
    fun `updateCommandCounts updates counts and timestamp`() = runBlocking {
        val metric = createMetric("uuid-1", commandCount = 0, manualCommandCount = 0)
        repository.insertOrUpdate(metric)

        val beforeTime = System.currentTimeMillis()
        repository.updateCommandCounts("uuid-1", commandCount = 3, manualCount = 2)
        val afterTime = System.currentTimeMillis()

        val updated = repository.getByUuid("uuid-1")

        assertEquals("Command count should be updated", 3, updated?.commandCount)
        assertEquals("Manual count should be updated", 2, updated?.manualCommandCount)
        assertTrue("Timestamp should be updated",
            updated?.lastAssessed ?: 0 >= beforeTime &&
            updated?.lastAssessed ?: 0 <= afterTime
        )
    }

    @Test
    fun `getQualityStats returns correct aggregation`() = runBlocking {
        val appId = "com.example.app"

        // EXCELLENT (80-100): 1 element
        repository.insertOrUpdate(createMetric("uuid-1", appId = appId, qualityScore = 85, manualCommandCount = 2))

        // GOOD (60-79): 2 elements
        repository.insertOrUpdate(createMetric("uuid-2", appId = appId, qualityScore = 65))
        repository.insertOrUpdate(createMetric("uuid-3", appId = appId, qualityScore = 75, manualCommandCount = 1))

        // ACCEPTABLE (40-59): 1 element
        repository.insertOrUpdate(createMetric("uuid-4", appId = appId, qualityScore = 50))

        // POOR (0-39): 2 elements
        repository.insertOrUpdate(createMetric("uuid-5", appId = appId, qualityScore = 30))
        repository.insertOrUpdate(createMetric("uuid-6", appId = appId, qualityScore = 20, manualCommandCount = 3))

        val stats = repository.getQualityStats(appId)

        assertNotNull("Stats should not be null", stats)
        assertEquals("Total elements", 6, stats?.totalElements)
        assertEquals("Excellent count", 1, stats?.excellentCount)
        assertEquals("Good count", 2, stats?.goodCount)
        assertEquals("Acceptable count", 1, stats?.acceptableCount)
        assertEquals("Poor count", 2, stats?.poorCount)
        assertEquals("Total manual commands", 6, stats?.totalManualCommands) // 2 + 1 + 3

        // Average: (85 + 65 + 75 + 50 + 30 + 20) / 6 = 54.17
        assertEquals("Average quality score", 54.17, stats?.avgQualityScore ?: 0.0, 0.5)
    }

    @Test
    fun `getQualityStats returns null for non-existent app`() = runBlocking {
        val stats = repository.getQualityStats("com.nonexistent.app")

        assertNull("Stats should be null for non-existent app", stats)
    }

    @Test
    fun `getQualityStats handles empty app`() = runBlocking {
        val stats = repository.getQualityStats("com.empty.app")

        assertNull("Stats should be null for empty app", stats)
    }

    @Test
    fun `deleteByApp removes all metrics for app`() = runBlocking {
        val appId = "com.example.app"
        repository.insertOrUpdate(createMetric("uuid-1", appId = appId))
        repository.insertOrUpdate(createMetric("uuid-2", appId = appId))

        repository.deleteByApp(appId)
        val metrics = repository.getByApp(appId)

        assertEquals("Should have no metrics", 0, metrics.size)
    }

    @Test
    fun `deleteByApp does not affect other apps`() = runBlocking {
        repository.insertOrUpdate(createMetric("uuid-1", appId = "com.app1.test"))
        repository.insertOrUpdate(createMetric("uuid-2", appId = "com.app2.test"))

        repository.deleteByApp("com.app1.test")
        val metrics = repository.getByApp("com.app2.test")

        assertEquals("Should still have metric", 1, metrics.size)
    }

    @Test
    fun `getElementsNeedingCommands uses LEFT JOIN correctly`() = runBlocking {
        val appId = "com.example.app"

        // Create quality metrics (no commands yet)
        repository.insertOrUpdate(createMetric("uuid-1", appId = appId, qualityScore = 30, commandCount = 0))
        repository.insertOrUpdate(createMetric("uuid-2", appId = appId, qualityScore = 35, commandCount = 0))
        repository.insertOrUpdate(createMetric("uuid-3", appId = appId, qualityScore = 50, commandCount = 0))

        val elements = repository.getElementsNeedingCommands(appId)

        // Should return uuid-1 and uuid-2 (score < 40, no commands)
        assertEquals("Should return 2 elements needing commands", 2, elements.size)
        assertTrue("Should include uuid-1", elements.any { it.elementUuid == "uuid-1" })
        assertTrue("Should include uuid-2", elements.any { it.elementUuid == "uuid-2" })
    }

    @Test
    fun `quality metric preserves all metadata flags`() = runBlocking {
        val metric = createMetric(
            "uuid-1",
            hasText = true,
            hasContentDesc = false,
            hasResourceId = true
        )

        repository.insertOrUpdate(metric)
        val retrieved = repository.getByUuid("uuid-1")

        assertTrue("hasText should be true", retrieved?.hasText ?: false)
        assertFalse("hasContentDesc should be false", retrieved?.hasContentDesc ?: true)
        assertTrue("hasResourceId should be true", retrieved?.hasResourceId ?: false)
    }

    // Helper function
    private fun createMetric(
        elementUuid: String,
        appId: String = "com.example.app",
        qualityScore: Int = 50,
        hasText: Boolean = true,
        hasContentDesc: Boolean = true,
        hasResourceId: Boolean = true,
        commandCount: Int = 0,
        manualCommandCount: Int = 0
    ): QualityMetricDTO {
        return QualityMetricDTO(
            elementUuid = elementUuid,
            appId = appId,
            qualityScore = qualityScore,
            hasText = hasText,
            hasContentDesc = hasContentDesc,
            hasResourceId = hasResourceId,
            commandCount = commandCount,
            manualCommandCount = manualCommandCount,
            lastAssessed = System.currentTimeMillis()
        )
    }
}
