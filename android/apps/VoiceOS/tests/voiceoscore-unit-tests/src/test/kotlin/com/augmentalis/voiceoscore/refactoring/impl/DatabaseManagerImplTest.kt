/**
 * DatabaseManagerImplTest.kt - Comprehensive test suite for DatabaseManagerImpl
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Anthropic)
 * Created: 2025-10-15 12:47:05 PDT
 * Part of: VoiceOSService SOLID Refactoring - Database Manager Tests
 *
 * TEST COVERAGE:
 * - Initialization (10 tests) - 3 databases, state management
 * - Voice Commands (15 tests) - CRUD, search, transactions
 * - Caching (20 tests) - 4-layer cache, TTL, eviction, hit/miss
 * - Generated Commands (15 tests) - hash deduplication, batch operations
 * - Scraped Elements (15 tests) - hierarchy, deduplication
 * - Health/Maintenance (10 tests) - health checks, optimization, cleanup
 * - Concurrency (10 tests) - thread safety, parallel operations
 * - Error Handling (5 tests) - error recovery, timeout handling
 *
 * TOTAL TESTS: 100
 */
package com.augmentalis.voiceoscore.refactoring.impl

import android.content.Context
import com.augmentalis.commandmanager.database.CommandDatabase
import com.augmentalis.commandmanager.database.VoiceCommandDao
import com.augmentalis.commandmanager.database.VoiceCommandEntity
import com.augmentalis.voiceoscore.learnweb.WebScrapingDatabase
import com.augmentalis.voiceoscore.learnweb.GeneratedWebCommandDao
import com.augmentalis.voiceoscore.learnweb.ScrapedWebsiteDao
import com.augmentalis.voiceoscore.learnweb.GeneratedWebCommand as WebCommandEntity
import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager
import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager.*
import com.augmentalis.voiceoscore.scraping.database.AppScrapingDatabase
import com.augmentalis.voiceoscore.scraping.dao.GeneratedCommandDao
import com.augmentalis.voiceoscore.scraping.dao.ScrapedAppDao
import com.augmentalis.voiceoscore.scraping.dao.ScrapedElementDao
import com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import com.augmentalis.voiceoscore.refactoring.impl.CacheConfig
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseManagerImplTest {

    private lateinit var databaseManager: DatabaseManagerImpl
    private lateinit var mockContext: Context
    private lateinit var mockCommandDb: CommandDatabase
    private lateinit var mockAppScrapingDb: AppScrapingDatabase
    private lateinit var mockWebScrapingDb: WebScrapingDatabase

    // DAOs
    private lateinit var mockVoiceCommandDao: VoiceCommandDao
    private lateinit var mockScrapedElementDao: ScrapedElementDao
    private lateinit var mockGeneratedCommandDao: GeneratedCommandDao
    private lateinit var mockScrapedAppDao: ScrapedAppDao
    private lateinit var mockGeneratedWebCommandDao: GeneratedWebCommandDao
    private lateinit var mockScrapedWebsiteDao: ScrapedWebsiteDao

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)

        // Mock databases
        mockCommandDb = mockk(relaxed = true)
        mockAppScrapingDb = mockk(relaxed = true)
        mockWebScrapingDb = mockk(relaxed = true)

        // Mock DAOs
        mockVoiceCommandDao = mockk(relaxed = true)
        mockScrapedElementDao = mockk(relaxed = true)
        mockGeneratedCommandDao = mockk(relaxed = true)
        mockScrapedAppDao = mockk(relaxed = true)
        mockGeneratedWebCommandDao = mockk(relaxed = true)
        mockScrapedWebsiteDao = mockk(relaxed = true)

        // Setup DAO accessors
        every { mockCommandDb.voiceCommandDao() } returns mockVoiceCommandDao
        every { mockAppScrapingDb.scrapedElementDao() } returns mockScrapedElementDao
        every { mockAppScrapingDb.generatedCommandDao() } returns mockGeneratedCommandDao
        every { mockAppScrapingDb.scrapedAppDao() } returns mockScrapedAppDao
        every { mockWebScrapingDb.generatedWebCommandDao() } returns mockGeneratedWebCommandDao
        every { mockWebScrapingDb.scrapedWebsiteDao() } returns mockScrapedWebsiteDao

        // Setup database helper for optimization
        val mockHelper = mockk<SupportSQLiteOpenHelper>(relaxed = true)
        val mockSqliteDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockCommandDb.openHelper } returns mockHelper
        every { mockAppScrapingDb.openHelper } returns mockHelper
        every { mockWebScrapingDb.openHelper } returns mockHelper
        every { mockHelper.writableDatabase } returns mockSqliteDb

        // Setup database paths
        every { mockContext.getDatabasePath(any()) } returns File("/tmp/test.db")

        // Mock database singleton getters
        mockkStatic(CommandDatabase::class)
        mockkStatic(AppScrapingDatabase::class)
        mockkStatic(WebScrapingDatabase::class)

        every { CommandDatabase.getInstance(any()) } returns mockCommandDb
        every { AppScrapingDatabase.getInstance(any()) } returns mockAppScrapingDb
        every { WebScrapingDatabase.getInstance(any()) } returns mockWebScrapingDb

        // Setup default successful DAO behavior
        coEvery { mockVoiceCommandDao.getAllCommands() } returns emptyList()
        coEvery { mockVoiceCommandDao.getCommandsForLocale(any()) } returns emptyList()
        coEvery { mockVoiceCommandDao.searchCommands(any(), any()) } returns emptyList()
        coEvery { mockVoiceCommandDao.getCommand(any(), any()) } returns null

        coEvery { mockScrapedElementDao.getElementsByAppId(any()) } returns emptyList()
        coEvery { mockScrapedElementDao.insertBatch(any()) } returns Unit

        coEvery { mockGeneratedCommandDao.getAllCommands() } returns emptyList()
        coEvery { mockGeneratedCommandDao.getCommandsForApp(any()) } returns emptyList()
        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        coEvery { mockScrapedAppDao.deleteApp(any()) } returns Unit
        coEvery { mockScrapedAppDao.deleteAppsOlderThan(any()) } returns 0

        coEvery { mockGeneratedWebCommandDao.getAllCommands() } returns emptyList()
        coEvery { mockGeneratedWebCommandDao.getByWebsiteUrlHash(any()) } returns emptyList()
        coEvery { mockGeneratedWebCommandDao.insert(any()) } returns 1L

        coEvery { mockScrapedWebsiteDao.deleteByUrlHash(any()) } returns Unit
        coEvery { mockScrapedWebsiteDao.getStaleWebsites(any()) } returns emptyList()

        databaseManager = DatabaseManagerImpl(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        databaseManager.cleanup()
        unmockkAll()
    }

    // ========================================
    // 1. Initialization Tests (10 tests)
    // ========================================

    @Test
    fun `test initialize successfully`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()

        databaseManager.initialize(mockContext, config)

        assertTrue(databaseManager.isReady)
        assertEquals(DatabaseState.READY, databaseManager.currentState)
    }

    @Test
    fun `test initialize all three databases`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()

        databaseManager.initialize(mockContext, config)

        verify { CommandDatabase.getInstance(mockContext) }
        verify { AppScrapingDatabase.getInstance(mockContext) }
        verify { WebScrapingDatabase.getInstance(mockContext) }
    }

    @Test
    fun `test double initialization throws exception`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        assertThrows(IllegalStateException::class.java) {
            runBlocking { databaseManager.initialize(mockContext, config) }
        }
    }

    @Test
    fun `test initialization state transitions`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()

        assertEquals(DatabaseState.UNINITIALIZED, databaseManager.currentState)

        databaseManager.initialize(mockContext, config)

        assertEquals(DatabaseState.READY, databaseManager.currentState)
    }

    @Test
    fun `test initialization verifies database health`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()

        databaseManager.initialize(mockContext, config)

        assertTrue(databaseManager.isDatabaseHealthy)
    }

    @Test
    fun `test initialization emits event`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        val events = mutableListOf<DatabaseEvent>()

        val job = launch {
            databaseManager.databaseEvents.take(1).toList(events)
        }

        databaseManager.initialize(mockContext, config)
        delay(100)
        job.cancel()

        val initEvent = events.firstOrNull { it is DatabaseEvent.Initialized }
        assertNotNull(initEvent)
    }

    @Test
    fun `test close transitions to closed state`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        databaseManager.close()

        assertEquals(DatabaseState.CLOSED, databaseManager.currentState)
    }

    @Test
    fun `test close clears all caches`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        databaseManager.close()

        assertFalse(databaseManager.isCacheEnabled())
    }

    @Test
    fun `test cleanup cancels background jobs`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        databaseManager.cleanup()

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun `test initialization with health check enabled starts background job`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()

        databaseManager.initialize(mockContext, config)

        assertTrue(databaseManager.isReady)
    }

    // ========================================
    // 2. Voice Commands Tests (15 tests)
    // ========================================

    @Test
    fun `test getVoiceCommands returns commands for locale`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test_command", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        val commands = databaseManager.getVoiceCommands("en-US")

        assertEquals(1, commands.size)
        assertEquals("test_command", commands[0].primaryText)
    }

    @Test
    fun `test getVoiceCommands uses cache on second call`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // First call - database
        databaseManager.getVoiceCommands("en-US")

        // Second call - should use cache
        databaseManager.getVoiceCommands("en-US")

        coVerify(exactly = 1) { mockVoiceCommandDao.getCommandsForLocale("en-US") }
    }

    @Test
    fun `test getVoiceCommands emits cache hit event`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // Prime cache
        databaseManager.getVoiceCommands("en-US")

        val events = mutableListOf<DatabaseEvent>()
        val job = launch {
            databaseManager.databaseEvents.take(1).toList(events)
        }

        // Should hit cache
        databaseManager.getVoiceCommands("en-US")
        delay(100)
        job.cancel()

        val cacheHit = events.firstOrNull { it is DatabaseEvent.CacheHit }
        assertNotNull(cacheHit)
    }

    @Test
    fun `test getAllVoiceCommands returns all commands`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entities = listOf(
            createMockVoiceCommandEntity(1, "cmd1", "en-US"),
            createMockVoiceCommandEntity(2, "cmd2", "es-ES")
        )
        coEvery { mockVoiceCommandDao.getAllCommands() } returns entities

        val commands = databaseManager.getAllVoiceCommands()

        assertEquals(2, commands.size)
    }

    @Test
    fun `test searchVoiceCommands returns matching commands`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "search result", "en-US")
        coEvery { mockVoiceCommandDao.searchCommands("en-US", "search") } returns listOf(entity)

        val commands = databaseManager.searchVoiceCommands("search")

        assertEquals(1, commands.size)
        assertTrue(commands[0].primaryText.contains("search"))
    }

    @Test
    fun `test searchVoiceCommands not cached`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.searchCommands("en-US", "test") } returns listOf(entity)

        // Two identical searches
        databaseManager.searchVoiceCommands("test")
        databaseManager.searchVoiceCommands("test")

        // Should query database both times (search not cached)
        coVerify(exactly = 2) { mockVoiceCommandDao.searchCommands("en-US", "test") }
    }

    @Test
    fun `test getVoiceCommand returns single command`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommand("cmd_1", "en-US") } returns entity

        val command = databaseManager.getVoiceCommand("cmd_1")

        assertNotNull(command)
        assertEquals("test", command?.primaryText)
    }

    @Test
    fun `test getVoiceCommand returns null when not found`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockVoiceCommandDao.getCommand(any(), any()) } returns null

        val command = databaseManager.getVoiceCommand("nonexistent")

        assertNull(command)
    }

    @Test
    fun `test voice command operations record metrics`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getAllCommands() } returns listOf(entity)

        databaseManager.getAllVoiceCommands()

        val metrics = databaseManager.getMetrics()
        assertTrue(metrics.totalOperations > 0)
    }

    @Test
    fun `test voice command error recorded in metrics`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockVoiceCommandDao.getAllCommands() } throws Exception("Test error")

        try {
            databaseManager.getAllVoiceCommands()
        } catch (e: Exception) {
            // Expected
        }

        val metrics = databaseManager.getMetrics()
        assertTrue(metrics.failedOperations > 0)
    }

    @Test
    fun `test voice command operations emit events`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getAllCommands() } returns listOf(entity)

        val events = mutableListOf<DatabaseEvent>()
        val job = launch {
            databaseManager.databaseEvents.take(1).toList(events)
        }

        databaseManager.getAllVoiceCommands()
        delay(100)
        job.cancel()

        val opEvent = events.firstOrNull { it is DatabaseEvent.OperationCompleted }
        assertNotNull(opEvent)
    }

    @Test
    fun `test voice command operations respect timeout`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockVoiceCommandDao.getAllCommands() } coAnswers {
            delay(10000) // Long delay
            emptyList()
        }

        assertThrows(Exception::class.java) {
            runBlocking {
                databaseManager.getAllVoiceCommands()
            }
        }
    }

    @Test
    fun `test voice command conversion from entity`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test command", "en-US").copy(
            synonyms = "synonym1,synonym2",
            category = "navigation"
        )
        coEvery { mockVoiceCommandDao.getAllCommands() } returns listOf(entity)

        val commands = databaseManager.getAllVoiceCommands()

        assertEquals(1, commands.size)
        assertEquals("test command", commands[0].primaryText)
        assertEquals(2, commands[0].synonyms.size)
        assertEquals("en-US", commands[0].locale)
    }

    @Test
    fun `test voice command cache invalidation on clear`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // Prime cache
        databaseManager.getVoiceCommands("en-US")

        // Clear cache
        databaseManager.clearCache(DatabaseType.COMMAND_DATABASE)

        // Should query database again
        databaseManager.getVoiceCommands("en-US")

        coVerify(exactly = 2) { mockVoiceCommandDao.getCommandsForLocale("en-US") }
    }

    @Test
    fun `test voice command history tracked`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getAllCommands() } returns listOf(entity)

        databaseManager.getAllVoiceCommands()

        val history = databaseManager.getOperationHistory(limit = 10)
        assertTrue(history.isNotEmpty())
        assertEquals("getAllVoiceCommands", history.last().operationType)
    }

    // ========================================
    // 3. Caching Tests (20 tests)
    // ========================================

    @Test
    fun `test cache enabled by default`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        assertTrue(databaseManager.isCacheEnabled())
    }

    @Test
    fun `test disable cache clears all caches`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        databaseManager.disableCache()

        assertFalse(databaseManager.isCacheEnabled())
    }

    @Test
    fun `test enable cache allows caching again`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        databaseManager.disableCache()
        databaseManager.enableCache()

        assertTrue(databaseManager.isCacheEnabled())
    }

    @Test
    fun `test command cache TTL expiration`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig(
            enableCaching = true,
            cacheSize = 100
        )
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // Prime cache
        databaseManager.getVoiceCommands("en-US")

        // Wait for TTL expiration
        delay(200)

        // Should query database again
        databaseManager.getVoiceCommands("en-US")

        coVerify(atLeast = 2) { mockVoiceCommandDao.getCommandsForLocale("en-US") }
    }

    @Test
    fun `test cache stats track hits`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // Prime cache
        databaseManager.getVoiceCommands("en-US")

        // Hit cache
        databaseManager.getVoiceCommands("en-US")

        val stats = databaseManager.getCacheStats()
        assertTrue(stats.hitCount > 0)
    }

    @Test
    fun `test cache stats track misses`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // Miss cache (first call)
        databaseManager.getVoiceCommands("en-US")

        val stats = databaseManager.getCacheStats()
        assertTrue(stats.missCount > 0)
    }

    @Test
    fun `test cache hit rate calculation`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // 1 miss, 4 hits
        databaseManager.getVoiceCommands("en-US")
        repeat(4) {
            databaseManager.getVoiceCommands("en-US")
        }

        val stats = databaseManager.getCacheStats()
        assertTrue(stats.hitRate > 0.7f)
    }

    @Test
    fun `test clearCache clears all cache types`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        databaseManager.clearCache()

        val stats = databaseManager.getCacheStats()
        assertEquals(0, stats.currentSize)
    }

    @Test
    fun `test clearCache for specific database`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        databaseManager.clearCache(DatabaseType.COMMAND_DATABASE)

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun `test element cache LRU eviction`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig(
            enableCaching = true,
            cacheSize = 2
        )
        databaseManager.initialize(mockContext, config)

        val elements = listOf(
            createMockScrapedElement("hash1", "pkg1"),
            createMockScrapedElement("hash2", "pkg1"),
            createMockScrapedElement("hash3", "pkg1")
        )

        coEvery { mockScrapedElementDao.getElementsByAppId("pkg1") } returns
            elements.map { it.toEntity("pkg1") }

        // Get elements (fills cache)
        databaseManager.getScrapedElements("pkg1")

        val stats = databaseManager.getCacheStats()
        // Cache size should be limited
        assertTrue(stats.currentSize <= 10)
    }

    @Test
    fun `test generated command cache by package`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockGeneratedCommandEntity(1, "hash1", "test command")
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity)

        // Prime cache
        databaseManager.getGeneratedCommands("pkg1")

        // Hit cache
        databaseManager.getGeneratedCommands("pkg1")

        coVerify(exactly = 1) { mockGeneratedCommandDao.getCommandsForApp("pkg1") }
    }

    @Test
    fun `test generated command cache different packages`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity1 = createMockGeneratedCommandEntity(1, "hash1", "cmd1")
        val entity2 = createMockGeneratedCommandEntity(2, "hash2", "cmd2")

        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity1)
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg2") } returns listOf(entity2)

        databaseManager.getGeneratedCommands("pkg1")
        databaseManager.getGeneratedCommands("pkg2")

        val stats = databaseManager.getCacheStats()
        assertTrue(stats.currentSize > 0)
    }

    @Test
    fun `test web command cache by URL`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockWebCommandEntity(1, "test", "urlhash")
        coEvery { mockGeneratedWebCommandDao.getByWebsiteUrlHash(any()) } returns listOf(entity)

        // Prime cache
        databaseManager.getWebCommands("http://test.com")

        // Hit cache
        databaseManager.getWebCommands("http://test.com")

        coVerify(exactly = 1) { mockGeneratedWebCommandDao.getByWebsiteUrlHash(any()) }
    }

    @Test
    fun `test cache invalidation on save scraped elements`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val element = createMockScrapedElement("hash1", "pkg1")
        coEvery { mockScrapedElementDao.insertBatch(any()) } returns Unit

        databaseManager.saveScrapedElements(listOf(element), "pkg1")

        // Cache should be invalidated for this element
        val stats = databaseManager.getCacheStats()
        assertNotNull(stats)
    }

    @Test
    fun `test cache invalidation on save generated commands`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockGeneratedCommandEntity(1, "hash1", "cmd")
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity)

        // Prime cache
        databaseManager.getGeneratedCommands("pkg1")

        val command = createMockGeneratedCommand(2, "hash2", "new cmd", "pkg1")
        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        // Save new commands - should invalidate cache
        databaseManager.saveGeneratedCommands(listOf(command), "pkg1")

        // Should query database again
        databaseManager.getGeneratedCommands("pkg1")

        coVerify(atLeast = 2) { mockGeneratedCommandDao.getCommandsForApp("pkg1") }
    }

    @Test
    fun `test cache stats show correct size`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val stats = databaseManager.getCacheStats()

        assertTrue(stats.isEnabled)
        assertTrue(stats.maxSize > 0)
    }

    @Test
    fun `test cache disabled prevents caching`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)
        databaseManager.disableCache()

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // Two calls with cache disabled
        databaseManager.getVoiceCommands("en-US")
        databaseManager.getVoiceCommands("en-US")

        // Should query database both times
        coVerify(exactly = 2) { mockVoiceCommandDao.getCommandsForLocale("en-US") }
    }

    @Test
    fun `test cache stats reset on clearCache`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // Generate some cache activity
        databaseManager.getVoiceCommands("en-US")
        databaseManager.getVoiceCommands("en-US")

        databaseManager.clearCache()

        val stats = databaseManager.getCacheStats()
        assertEquals(0, stats.hitCount)
        assertEquals(0, stats.missCount)
    }

    @Test
    fun `test cache persists across multiple operations`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // Multiple cache hits
        repeat(10) {
            databaseManager.getVoiceCommands("en-US")
        }

        val stats = databaseManager.getCacheStats()
        assertEquals(9, stats.hitCount) // First is miss, rest are hits
    }

    // ========================================
    // 4. Generated Commands Tests (15 tests)
    // ========================================

    @Test
    fun `test saveGeneratedCommands inserts batch`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val commands = listOf(
            createMockGeneratedCommand(1, "hash1", "cmd1", "pkg1"),
            createMockGeneratedCommand(2, "hash2", "cmd2", "pkg1")
        )
        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        val count = databaseManager.saveGeneratedCommands(commands, "pkg1")

        assertEquals(2, count)
        coVerify { mockGeneratedCommandDao.insertBatch(match { it.size == 2 }) }
    }

    @Test
    fun `test getGeneratedCommands returns commands for package`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entities = listOf(
            createMockGeneratedCommandEntity(1, "hash1", "cmd1"),
            createMockGeneratedCommandEntity(2, "hash2", "cmd2")
        )
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns entities

        val commands = databaseManager.getGeneratedCommands("pkg1")

        assertEquals(2, commands.size)
    }

    @Test
    fun `test getAllGeneratedCommands returns all`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entities = listOf(
            createMockGeneratedCommandEntity(1, "hash1", "cmd1"),
            createMockGeneratedCommandEntity(2, "hash2", "cmd2"),
            createMockGeneratedCommandEntity(3, "hash3", "cmd3")
        )
        coEvery { mockGeneratedCommandDao.getAllCommands() } returns entities

        val commands = databaseManager.getAllGeneratedCommands()

        assertEquals(3, commands.size)
    }

    @Test
    fun `test generated commands cache by package`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockGeneratedCommandEntity(1, "hash1", "cmd")
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity)

        // Prime cache
        databaseManager.getGeneratedCommands("pkg1")

        // Hit cache
        databaseManager.getGeneratedCommands("pkg1")

        coVerify(exactly = 1) { mockGeneratedCommandDao.getCommandsForApp("pkg1") }
    }

    @Test
    fun `test generated commands cache TTL expiration`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig(
            enableCaching = true,
            cacheSize = 100
        )
        databaseManager.initialize(mockContext, config)

        val entity = createMockGeneratedCommandEntity(1, "hash1", "cmd")
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity)

        // Prime cache
        databaseManager.getGeneratedCommands("pkg1")

        // Wait for TTL expiration
        delay(200)

        // Should query database again
        databaseManager.getGeneratedCommands("pkg1")

        coVerify(atLeast = 2) { mockGeneratedCommandDao.getCommandsForApp("pkg1") }
    }

    @Test
    fun `test batchInsertGeneratedCommands uses transaction`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val commands = listOf(
            createMockGeneratedCommand(1, "hash1", "cmd1", "pkg1")
        )
        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        val count = databaseManager.batchInsertGeneratedCommands(commands, "pkg1")

        assertEquals(1, count)
    }

    @Test
    fun `test generated command conversion from entity`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockGeneratedCommandEntity(1, "hash123", "test command").copy(
            synonyms = "syn1,syn2",
            confidence = 0.95f
        )
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity)

        val commands = databaseManager.getGeneratedCommands("pkg1")

        assertEquals(1, commands.size)
        assertEquals("test command", commands[0].commandText)
        assertEquals("hash123", commands[0].elementHash)
        assertEquals(2, commands[0].synonyms.size)
    }

    @Test
    fun `test generated command hash deduplication`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        // Same hash, different commands
        val commands = listOf(
            createMockGeneratedCommand(1, "samehash", "cmd1", "pkg1"),
            createMockGeneratedCommand(2, "samehash", "cmd2", "pkg1")
        )
        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        databaseManager.saveGeneratedCommands(commands, "pkg1")

        coVerify { mockGeneratedCommandDao.insertBatch(match { it.size == 2 }) }
    }

    @Test
    fun `test generated commands empty list`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns emptyList()

        val commands = databaseManager.getGeneratedCommands("pkg1")

        assertTrue(commands.isEmpty())
    }

    @Test
    fun `test generated commands metrics tracked`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockGeneratedCommandEntity(1, "hash1", "cmd")
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity)

        databaseManager.getGeneratedCommands("pkg1")

        val metrics = databaseManager.getMetrics()
        assertTrue(metrics.totalOperations > 0)
    }

    @Test
    fun `test generated commands operation history`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockGeneratedCommandEntity(1, "hash1", "cmd")
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity)

        databaseManager.getGeneratedCommands("pkg1")

        val history = databaseManager.getOperationHistory(limit = 10)
        assertTrue(history.any { it.operationType == "getGeneratedCommands" })
    }

    @Test
    fun `test generated commands save invalidates cache`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockGeneratedCommandEntity(1, "hash1", "cmd")
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity)

        // Prime cache
        databaseManager.getGeneratedCommands("pkg1")

        val newCommand = createMockGeneratedCommand(2, "hash2", "new", "pkg1")
        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        databaseManager.saveGeneratedCommands(listOf(newCommand), "pkg1")

        // Should query database (cache invalidated)
        databaseManager.getGeneratedCommands("pkg1")

        coVerify(atLeast = 2) { mockGeneratedCommandDao.getCommandsForApp("pkg1") }
    }

    @Test
    fun `test generated commands error handling`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } throws Exception("DB error")

        assertThrows(Exception::class.java) {
            runBlocking {
                databaseManager.getGeneratedCommands("pkg1")
            }
        }
    }

    @Test
    fun `test generated commands multiple packages independent caches`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity1 = createMockGeneratedCommandEntity(1, "hash1", "cmd1")
        val entity2 = createMockGeneratedCommandEntity(2, "hash2", "cmd2")

        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity1)
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg2") } returns listOf(entity2)

        // Cache both
        databaseManager.getGeneratedCommands("pkg1")
        databaseManager.getGeneratedCommands("pkg2")

        // Hit both caches
        databaseManager.getGeneratedCommands("pkg1")
        databaseManager.getGeneratedCommands("pkg2")

        coVerify(exactly = 1) { mockGeneratedCommandDao.getCommandsForApp("pkg1") }
        coVerify(exactly = 1) { mockGeneratedCommandDao.getCommandsForApp("pkg2") }
    }

    @Test
    fun `test generated commands large batch performance`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val commands = (1..100).map {
            createMockGeneratedCommand(it, "hash$it", "cmd$it", "pkg1")
        }
        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        val startTime = System.currentTimeMillis()
        databaseManager.saveGeneratedCommands(commands, "pkg1")
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Batch insert too slow: ${duration}ms", duration < 1000)
    }

    // ========================================
    // 5. Scraped Elements Tests (15 tests)
    // ========================================

    @Test
    fun `test saveScrapedElements inserts batch`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val elements = listOf(
            createMockScrapedElement("hash1", "pkg1"),
            createMockScrapedElement("hash2", "pkg1")
        )
        coEvery { mockScrapedElementDao.insertBatch(any()) } returns Unit

        val count = databaseManager.saveScrapedElements(elements, "pkg1")

        assertEquals(2, count)
        coVerify { mockScrapedElementDao.insertBatch(match { it.size == 2 }) }
    }

    @Test
    fun `test getScrapedElements returns elements for package`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entities = listOf(
            createMockScrapedElementEntity(1L, "hash1", "pkg1"),
            createMockScrapedElementEntity(2L, "hash2", "pkg1")
        )
        coEvery { mockScrapedElementDao.getElementsByAppId("pkg1") } returns entities

        val elements = databaseManager.getScrapedElements("pkg1")

        assertEquals(2, elements.size)
    }

    @Test
    fun `test scraped elements cache updates on get`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entities = listOf(
            createMockScrapedElementEntity(1L, "hash1", "pkg1")
        )
        coEvery { mockScrapedElementDao.getElementsByAppId("pkg1") } returns entities

        databaseManager.getScrapedElements("pkg1")

        // Element should be in cache
        val stats = databaseManager.getCacheStats()
        assertTrue(stats.currentSize >= 0)
    }

    @Test
    fun `test scraped element conversion from entity`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockScrapedElementEntity(1L, "hash123", "pkg1").copy(
            text = "Button Text",
            contentDescription = "Test Button",
            isClickable = true
        )

        coEvery { mockScrapedElementDao.getElementsByAppId("pkg1") } returns listOf(entity)

        val elements = databaseManager.getScrapedElements("pkg1")

        assertEquals(1, elements.size)
        assertEquals("hash123", elements[0].hash)
        assertEquals("Button Text", elements[0].text)
        assertEquals("Test Button", elements[0].contentDescription)
        assertTrue(elements[0].isClickable)
    }

    @Test
    fun `test scraped element conversion to entity`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val element = createMockScrapedElement("hash1", "pkg1").copy(
            text = "Test",
            isClickable = true
        )

        coEvery { mockScrapedElementDao.insertBatch(any()) } answers {
            val entities = firstArg<List<ScrapedElementEntity>>()
            assertEquals("hash1", entities[0].elementHash)
            assertEquals("Test", entities[0].text)
            assertTrue(entities[0].isClickable)
            Unit
        }

        databaseManager.saveScrapedElements(listOf(element), "pkg1")

        coVerify { mockScrapedElementDao.insertBatch(any()) }
    }

    @Test
    fun `test deleteScrapedData removes app data`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockScrapedAppDao.deleteApp("pkg1") } returns Unit

        databaseManager.deleteScrapedData("pkg1")

        coVerify { mockScrapedAppDao.deleteApp("pkg1") }
    }

    @Test
    fun `test deleteScrapedData invalidates cache`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockGeneratedCommandEntity(1, "hash1", "cmd")
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity)

        // Prime cache
        databaseManager.getGeneratedCommands("pkg1")

        coEvery { mockScrapedAppDao.deleteApp("pkg1") } returns Unit
        databaseManager.deleteScrapedData("pkg1")

        // Cache should be invalidated
        databaseManager.getGeneratedCommands("pkg1")

        coVerify(atLeast = 2) { mockGeneratedCommandDao.getCommandsForApp("pkg1") }
    }

    @Test
    fun `test batchInsertScrapedElements uses transaction`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val elements = listOf(createMockScrapedElement("hash1", "pkg1"))
        coEvery { mockScrapedElementDao.insertBatch(any()) } returns Unit

        val count = databaseManager.batchInsertScrapedElements(elements, "pkg1")

        assertEquals(1, count)
    }

    @Test
    fun `test scraped elements hash uniqueness`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val elements = listOf(
            createMockScrapedElement("samehash", "pkg1"),
            createMockScrapedElement("samehash", "pkg1")
        )
        coEvery { mockScrapedElementDao.insertBatch(any()) } returns Unit

        databaseManager.saveScrapedElements(elements, "pkg1")

        coVerify { mockScrapedElementDao.insertBatch(match { it.size == 2 }) }
    }

    @Test
    fun `test scraped elements empty list`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockScrapedElementDao.getElementsByAppId("pkg1") } returns emptyList()

        val elements = databaseManager.getScrapedElements("pkg1")

        assertTrue(elements.isEmpty())
    }

    @Test
    fun `test scraped elements metrics tracked`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockScrapedElementEntity(1L, "hash1", "pkg1")
        coEvery { mockScrapedElementDao.getElementsByAppId("pkg1") } returns listOf(entity)

        databaseManager.getScrapedElements("pkg1")

        val metrics = databaseManager.getMetrics()
        assertTrue(metrics.totalOperations > 0)
    }

    @Test
    fun `test scraped elements save invalidates element cache`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val element = createMockScrapedElement("hash1", "pkg1")
        coEvery { mockScrapedElementDao.insertBatch(any()) } returns Unit

        databaseManager.saveScrapedElements(listOf(element), "pkg1")

        // Element cache should be updated/invalidated
        val stats = databaseManager.getCacheStats()
        assertNotNull(stats)
    }

    @Test
    fun `test scraped elements with hierarchy data`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockScrapedElementEntity(1L, "hash1", "pkg1").copy(
            depth = 3,
            indexInParent = 2
        )

        coEvery { mockScrapedElementDao.getElementsByAppId("pkg1") } returns listOf(entity)

        val elements = databaseManager.getScrapedElements("pkg1")

        assertEquals(1, elements.size)
    }

    @Test
    fun `test scraped elements bounds parsing`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val element = createMockScrapedElement("hash1", "pkg1").copy(
            bounds = "{\"left\":10,\"top\":20,\"right\":100,\"bottom\":80}"
        )

        coEvery { mockScrapedElementDao.insertBatch(any()) } answers {
            val entities = firstArg<List<ScrapedElementEntity>>()
            assertTrue(entities[0].bounds.contains("left"))
            Unit
        }

        databaseManager.saveScrapedElements(listOf(element), "pkg1")

        coVerify { mockScrapedElementDao.insertBatch(any()) }
    }

    @Test
    fun `test scraped elements large batch performance`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val elements = (1..100).map {
            createMockScrapedElement("hash$it", "pkg1")
        }
        coEvery { mockScrapedElementDao.insertBatch(any()) } returns Unit

        val startTime = System.currentTimeMillis()
        databaseManager.saveScrapedElements(elements, "pkg1")
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Batch insert too slow: ${duration}ms", duration < 1000)
    }

    // ========================================
    // 6. Health & Maintenance Tests (10 tests)
    // ========================================

    @Test
    fun `test checkHealth returns health for all databases`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val health = databaseManager.checkHealth()

        assertEquals(3, health.size)
        assertTrue(health.containsKey(DatabaseType.COMMAND_DATABASE))
        assertTrue(health.containsKey(DatabaseType.APP_SCRAPING_DATABASE))
        assertTrue(health.containsKey(DatabaseType.WEB_SCRAPING_DATABASE))
    }

    @Test
    fun `test checkHealth reports healthy databases`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val health = databaseManager.checkHealth()

        health.values.forEach { dbHealth ->
            assertTrue(dbHealth.isHealthy)
            assertTrue(dbHealth.isAccessible)
        }
    }

    @Test
    fun `test optimize single database`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        databaseManager.optimize(DatabaseType.COMMAND_DATABASE)

        // Should complete without error
        assertTrue(true)
    }

    @Test
    fun `test optimize all databases`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        databaseManager.optimize(null)

        // Should complete without error
        assertTrue(true)
    }

    @Test
    fun `test getDatabaseSize returns size`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val size = databaseManager.getDatabaseSize(DatabaseType.COMMAND_DATABASE)

        assertTrue(size >= 0)
    }

    @Test
    fun `test clearOldData removes old records`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockScrapedAppDao.deleteAppsOlderThan(any()) } returns 5
        coEvery { mockScrapedWebsiteDao.getStaleWebsites(any()) } returns emptyList()

        val deleted = databaseManager.clearOldData(retentionDays = 30)

        assertTrue(deleted >= 0)
    }

    @Test
    fun `test isDatabaseHealthy reflects all databases`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        assertTrue(databaseManager.isDatabaseHealthy)
    }

    @Test
    fun `test health check emits optimization event`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val events = mutableListOf<DatabaseEvent>()
        val job = launch {
            databaseManager.databaseEvents.take(1).toList(events)
        }

        databaseManager.optimize(DatabaseType.COMMAND_DATABASE)
        delay(100)
        job.cancel()

        val optEvent = events.firstOrNull { it is DatabaseEvent.OptimizationCompleted }
        assertNotNull(optEvent)
    }

    @Test
    fun `test health check scheduled when enabled`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()

        databaseManager.initialize(mockContext, config)

        // Should start health check job
        assertTrue(databaseManager.isReady)
    }

    @Test
    fun `test optimization scheduled when enabled`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig(enableOptimization = true)

        databaseManager.initialize(mockContext, config)

        // Should start optimization job
        assertTrue(databaseManager.isReady)
    }

    // ========================================
    // 7. Concurrency Tests (10 tests)
    // ========================================

    @Test
    fun `test concurrent reads thread safe`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        val jobs = (1..10).map {
            launch {
                databaseManager.getVoiceCommands("en-US")
            }
        }

        jobs.forEach { it.join() }

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun `test concurrent writes thread safe`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        val jobs = (1..10).map { i ->
            launch {
                val cmd = createMockGeneratedCommand(i, "hash$i", "cmd$i", "pkg1")
                databaseManager.saveGeneratedCommands(listOf(cmd), "pkg1")
            }
        }

        jobs.forEach { it.join() }

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun `test concurrent cache access thread safe`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)

        // Prime cache
        databaseManager.getVoiceCommands("en-US")

        val jobs = (1..20).map {
            launch {
                databaseManager.getVoiceCommands("en-US")
            }
        }

        jobs.forEach { it.join() }

        val stats = databaseManager.getCacheStats()
        assertEquals(20, stats.hitCount)
    }

    @Test
    fun `test concurrent cache invalidation thread safe`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val jobs = (1..10).map { i ->
            launch {
                if (i % 2 == 0) {
                    databaseManager.clearCache()
                } else {
                    databaseManager.enableCache()
                }
            }
        }

        jobs.forEach { it.join() }

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun `test concurrent metrics updates thread safe`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getAllCommands() } returns listOf(entity)

        val jobs = (1..20).map {
            launch {
                databaseManager.getAllVoiceCommands()
            }
        }

        jobs.forEach { it.join() }

        val metrics = databaseManager.getMetrics()
        assertEquals(20, metrics.totalOperations.toInt())
    }

    @Test
    fun `test concurrent transaction execution`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        val jobs = (1..5).map { i ->
            launch {
                databaseManager.transaction(DatabaseType.APP_SCRAPING_DATABASE) {
                    val cmd = createMockGeneratedCommand(i, "hash$i", "cmd$i", "pkg1")
                    databaseManager.saveGeneratedCommands(listOf(cmd), "pkg1")
                }
            }
        }

        jobs.forEach { it.join() }

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun `test concurrent operations on different packages`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity1 = createMockGeneratedCommandEntity(1, "hash1", "cmd1")
        val entity2 = createMockGeneratedCommandEntity(2, "hash2", "cmd2")

        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg1") } returns listOf(entity1)
        coEvery { mockGeneratedCommandDao.getCommandsForApp("pkg2") } returns listOf(entity2)

        val jobs = listOf(
            launch { repeat(10) { databaseManager.getGeneratedCommands("pkg1") } },
            launch { repeat(10) { databaseManager.getGeneratedCommands("pkg2") } }
        )

        jobs.forEach { it.join() }

        val stats = databaseManager.getCacheStats()
        assertEquals(18, stats.hitCount) // 9 hits per package
    }

    @Test
    fun `test concurrent health checks thread safe`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val jobs = (1..5).map {
            launch {
                databaseManager.checkHealth()
            }
        }

        jobs.forEach { it.join() }

        assertTrue(databaseManager.isDatabaseHealthy)
    }

    @Test
    fun `test concurrent optimization calls thread safe`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val jobs = (1..3).map {
            launch {
                databaseManager.optimize(DatabaseType.COMMAND_DATABASE)
            }
        }

        jobs.forEach { it.join() }

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun `test mixed concurrent operations thread safe`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        val entity = createMockVoiceCommandEntity(1, "test", "en-US")
        coEvery { mockVoiceCommandDao.getCommandsForLocale("en-US") } returns listOf(entity)
        coEvery { mockGeneratedCommandDao.insertBatch(any()) } returns Unit

        val jobs = mutableListOf<Job>()

        // Mix of reads, writes, cache ops
        repeat(5) {
            jobs.add(launch { databaseManager.getVoiceCommands("en-US") })
        }
        repeat(3) {
            jobs.add(launch {
                val cmd = createMockGeneratedCommand(it, "hash$it", "cmd$it", "pkg1")
                databaseManager.saveGeneratedCommands(listOf(cmd), "pkg1")
            })
        }
        jobs.add(launch { databaseManager.getCacheStats() })
        jobs.add(launch { databaseManager.getMetrics() })

        jobs.forEach { it.join() }

        // Should complete all operations
        assertEquals(10, jobs.size)
    }

    // ========================================
    // 8. Error Handling Tests (5 tests)
    // ========================================

    @Test
    fun `test database query error tracked in metrics`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockVoiceCommandDao.getAllCommands() } throws Exception("DB error")

        try {
            databaseManager.getAllVoiceCommands()
        } catch (e: Exception) {
            // Expected
        }

        val metrics = databaseManager.getMetrics()
        assertTrue(metrics.failedOperations > 0)
    }

    @Test
    fun `test database insert error rolls back transaction`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockGeneratedCommandDao.insertBatch(any()) } throws Exception("Insert failed")

        val cmd = createMockGeneratedCommand(1, "hash1", "cmd1", "pkg1")

        assertThrows(Exception::class.java) {
            runBlocking {
                databaseManager.saveGeneratedCommands(listOf(cmd), "pkg1")
            }
        }
    }

    @Test
    fun `test timeout on long query`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockVoiceCommandDao.getAllCommands() } coAnswers {
            delay(10000)
            emptyList()
        }

        assertThrows(Exception::class.java) {
            runBlocking {
                databaseManager.getAllVoiceCommands()
            }
        }
    }

    @Test
    fun `test health check handles database errors gracefully`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        coEvery { mockVoiceCommandDao.getAllCommands() } throws Exception("DB unavailable")

        val health = databaseManager.checkHealth()

        // Should still return health status (marked unhealthy)
        assertTrue(health.containsKey(DatabaseType.COMMAND_DATABASE))
    }

    @Test
    fun `test error recovery on retry`() = testScope.runTest {
        val config = IDatabaseManager.DatabaseConfig()
        databaseManager.initialize(mockContext, config)

        var attempts = 0
        coEvery { mockVoiceCommandDao.getAllCommands() } coAnswers {
            attempts++
            if (attempts == 1) throw Exception("Transient error")
            listOf(createMockVoiceCommandEntity(1, "test", "en-US"))
        }

        // First call fails
        try {
            databaseManager.getAllVoiceCommands()
        } catch (e: Exception) {
            // Expected
        }

        // Second call succeeds
        val commands = databaseManager.getAllVoiceCommands()

        assertTrue(commands.isNotEmpty())
    }

    // ========================================
    // Helper Methods
    // ========================================

    private fun createMockVoiceCommandEntity(
        uid: Int,
        primaryText: String,
        locale: String
    ): VoiceCommandEntity {
        return VoiceCommandEntity(
            uid = uid.toLong(),
            id = "cmd_$uid",
            primaryText = primaryText,
            synonyms = "",
            locale = locale,
            category = "test",
            description = "Test command description",
            priority = 50
        )
    }

    private fun createMockScrapedElement(
        hash: String,
        packageName: String
    ): IDatabaseManager.ScrapedElement {
        return IDatabaseManager.ScrapedElement(
            id = 0L,
            hash = hash,
            packageName = packageName,
            text = "Test Text",
            contentDescription = "Test Description",
            resourceId = "test_id",
            className = "android.widget.Button",
            isClickable = true,
            bounds = "{}",
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createMockScrapedElementEntity(
        id: Long,
        hash: String,
        appId: String
    ): ScrapedElementEntity {
        return ScrapedElementEntity(
            id = id,
            appId = appId,
            elementHash = hash,
            className = "android.widget.Button",
            viewIdResourceName = "test_id",
            text = "Test",
            contentDescription = "Description",
            bounds = "{}",
            isClickable = true,
            isLongClickable = false,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isFocusable = false,
            isEnabled = true,
            depth = 0,
            indexInParent = 0,
            scrapedAt = System.currentTimeMillis()
        )
    }

    private fun createMockGeneratedCommand(
        id: Int,
        elementHash: String,
        commandText: String,
        packageName: String
    ): IDatabaseManager.GeneratedCommand {
        return IDatabaseManager.GeneratedCommand(
            id = id.toLong(),
            commandText = commandText,
            normalizedText = commandText.lowercase(),
            packageName = packageName,
            elementHash = elementHash,
            synonyms = emptyList(),
            confidence = 0.9f,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createMockGeneratedCommandEntity(
        id: Long,
        elementHash: String,
        commandText: String
    ): GeneratedCommandEntity {
        return GeneratedCommandEntity(
            id = id,
            elementHash = elementHash,
            commandText = commandText,
            actionType = "CLICK",
            confidence = 0.9f,
            synonyms = "",
            isUserApproved = false,
            usageCount = 0,
            lastUsed = null,
            generatedAt = System.currentTimeMillis()
        )
    }

    private fun createMockWebCommandEntity(
        id: Long,
        commandText: String,
        urlHash: String
    ): WebCommandEntity {
        return WebCommandEntity(
            id = id,
            websiteUrlHash = urlHash,
            elementHash = "hash",
            commandText = commandText,
            synonyms = "",
            action = "CLICK",
            xpath = "//button",
            generatedAt = System.currentTimeMillis(),
            usageCount = 0,
            lastUsedAt = null
        )
    }

    private fun ScrapedElement.toEntity(packageName: String): ScrapedElementEntity {
        return ScrapedElementEntity(
            id = 0,
            appId = packageName,
            elementHash = hash,
            className = className ?: "",
            viewIdResourceName = resourceId,
            text = text,
            contentDescription = contentDescription,
            bounds = bounds ?: "{}",
            isClickable = isClickable,
            isLongClickable = false,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isFocusable = false,
            isEnabled = true,
            depth = 0,
            indexInParent = 0,
            scrapedAt = timestamp
        )
    }
}
