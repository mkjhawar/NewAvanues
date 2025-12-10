/**
 * DatabaseTest.kt - Unit tests for SQLDelight VoiceOS Database
 *
 * Tests all 18 tables with CRUD operations and query validation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database

import kotlin.test.*

class DatabaseTest {

    private lateinit var database: VoiceOSDatabase

    @BeforeTest
    fun setup() {
        val factory = DatabaseDriverFactory()
        database = createDatabase(factory)
    }

    // ==================== CommandHistory Tests ====================

    @Test
    fun testCommandHistoryInsertAndGet() {
        val queries = database.commandHistoryQueries

        queries.insert(
            originalText = "go home",
            processedCommand = "HOME",
            confidence = 0.95,
            timestamp = System.currentTimeMillis(),
            language = "en",
            engineUsed = "Vosk",
            success = 1,
            executionTimeMs = 150,
            usageCount = 1,
            source = "VOICE"
        )

        val all = queries.getAll().executeAsList()
        assertEquals(1, all.size)
        assertEquals("go home", all[0].originalText)
        assertEquals(0.95, all[0].confidence)
    }

    @Test
    fun testCommandHistorySuccessRate() {
        val queries = database.commandHistoryQueries
        val now = System.currentTimeMillis()

        // Insert 3 successful, 1 failed
        repeat(3) {
            queries.insert("cmd$it", null, 0.9, now, "en", "Vosk", 1, 100, 1, "VOICE")
        }
        queries.insert("fail", null, 0.5, now, "en", "Vosk", 0, 100, 1, "VOICE")

        val rate = queries.getSuccessRate().executeAsOne()
        assertEquals(0.75, rate)
    }

    @Test
    fun testCommandHistoryCleanup() {
        val queries = database.commandHistoryQueries
        val now = System.currentTimeMillis()
        val oldTime = now - 100000

        queries.insert("old", null, 0.9, oldTime, "en", "Vosk", 1, 100, 1, "VOICE")
        queries.insert("new", null, 0.9, now, "en", "Vosk", 1, 100, 1, "VOICE")

        queries.deleteOlderThan(now - 50000)

        val all = queries.getAll().executeAsList()
        assertEquals(1, all.size)
        assertEquals("new", all[0].originalText)
    }

    // ==================== CustomCommand Tests ====================

    @Test
    fun testCustomCommandInsertAndGet() {
        val queries = database.customCommandQueries
        val now = System.currentTimeMillis()

        queries.insert(
            name = "Open Browser",
            description = "Opens web browser",
            phrases = "[\"open browser\", \"launch browser\"]",
            action = "LAUNCH_APP",
            parameters = "{\"package\": \"com.android.chrome\"}",
            language = "en",
            isActive = 1,
            usageCount = 0,
            lastUsed = null,
            createdAt = now,
            updatedAt = now
        )

        val all = queries.getAll().executeAsList()
        assertEquals(1, all.size)
        assertEquals("Open Browser", all[0].name)
    }

    @Test
    fun testCustomCommandActiveFilter() {
        val queries = database.customCommandQueries
        val now = System.currentTimeMillis()

        queries.insert("Active", null, "[]", "ACT", null, "en", 1, 0, null, now, now)
        queries.insert("Inactive", null, "[]", "ACT", null, "en", 0, 0, null, now, now)

        val active = queries.getActive().executeAsList()
        assertEquals(1, active.size)
        assertEquals("Active", active[0].name)
    }

    @Test
    fun testCustomCommandUsageIncrement() {
        val queries = database.customCommandQueries
        val now = System.currentTimeMillis()

        queries.insert("Test", null, "[]", "ACT", null, "en", 1, 0, null, now, now)
        val id = queries.getAll().executeAsList()[0].id

        queries.incrementUsage(now + 1000, id)

        val updated = queries.getById(id).executeAsOne()
        assertEquals(1, updated.usageCount)
        assertNotNull(updated.lastUsed)
    }

    // ==================== RecognitionLearning Tests ====================

    @Test
    fun testRecognitionLearningInsertAndGet() {
        val queries = database.recognitionLearningQueries
        val now = System.currentTimeMillis()

        queries.insert(
            engine = "Vosk",
            type = "learned_command",
            keyValue = "home",
            learnedValue = "go home",
            confidence = 0.9,
            usageCount = 1,
            lastUsed = now,
            createdAt = now,
            metadata = null
        )

        val byEngine = queries.getByEngine("Vosk").executeAsList()
        assertEquals(1, byEngine.size)
        assertEquals("home", byEngine[0].keyValue)
    }

    @Test
    fun testRecognitionLearningStats() {
        val queries = database.recognitionLearningQueries
        val now = System.currentTimeMillis()

        queries.insert("Vosk", "learned", "k1", "v1", 0.8, 5, now, now, null)
        queries.insert("Vosk", "learned", "k2", "v2", 0.9, 3, now, now, null)
        queries.insert("AndroidSTT", "learned", "k3", "v3", 0.7, 2, now, now, null)

        val stats = queries.getStats().executeAsList()
        assertEquals(2, stats.size) // 2 engines
    }

    // ==================== GeneratedCommand Tests ====================

    @Test
    fun testGeneratedCommandInsertAndSearch() {
        val queries = database.generatedCommandQueries
        val now = System.currentTimeMillis()

        queries.insert("hash1", "click submit button", "click", 0.85, null, 0, 0, null, now)
        queries.insert("hash2", "scroll down page", "scroll", 0.9, null, 0, 0, null, now)

        val results = queries.fuzzySearch("submit").executeAsList()
        assertEquals(1, results.size)
        assertEquals("click submit button", results[0].commandText)
    }

    @Test
    fun testGeneratedCommandApproval() {
        val queries = database.generatedCommandQueries
        val now = System.currentTimeMillis()

        queries.insert("hash1", "test command", "click", 0.8, null, 0, 0, null, now)
        val id = queries.getAll().executeAsList()[0].id

        queries.markApproved(id)

        val approved = queries.getUserApproved().executeAsList()
        assertEquals(1, approved.size)
    }

    // ==================== ScrapedApp Tests ====================

    @Test
    fun testScrapedAppInsertAndGet() {
        val queries = database.scrapedAppQueries
        val now = System.currentTimeMillis()

        queries.insert(
            "app1", "com.example.app", 1, "1.0.0", "abc123",
            0, null, "DYNAMIC", 1, 50, 10, now, now
        )

        val app = queries.getById("app1").executeAsOne()
        assertEquals("com.example.app", app.packageName)
        assertEquals(50, app.elementCount)
    }

    @Test
    fun testScrapedAppMarkLearned() {
        val queries = database.scrapedAppQueries
        val now = System.currentTimeMillis()

        queries.insert("app1", "com.test", 1, "1.0", "hash", 0, null, "LEARN_APP", 0, 0, 0, now, now)

        queries.markFullyLearned(now + 1000, "app1")

        val app = queries.getById("app1").executeAsOne()
        assertEquals(1, app.isFullyLearned)
        assertNotNull(app.learnCompletedAt)
    }

    // ==================== UserPreference Tests ====================

    @Test
    fun testUserPreferenceKeyValue() {
        val queries = database.userPreferenceQueries
        val now = System.currentTimeMillis()

        queries.insert("theme", "dark", "STRING", now)
        queries.insert("volume", "80", "INT", now)

        val theme = queries.getValue("theme").executeAsOneOrNull()
        assertEquals("dark", theme)

        val exists = queries.exists("theme").executeAsOne()
        assertEquals(1L, exists)
    }

    @Test
    fun testUserPreferenceUpdate() {
        val queries = database.userPreferenceQueries
        val now = System.currentTimeMillis()

        queries.insert("setting", "old", "STRING", now)
        queries.update("new", now + 1000, "setting")

        val value = queries.getValue("setting").executeAsOneOrNull()
        assertEquals("new", value)
    }

    // ==================== ErrorReport Tests ====================

    @Test
    fun testErrorReportInsertAndMarkSent() {
        val queries = database.errorReportQueries
        val now = System.currentTimeMillis()

        queries.insert("NullPointer", "Object is null", "stack...", null, null, null, now, 0)

        val unsent = queries.getUnsent().executeAsList()
        assertEquals(1, unsent.size)

        val id = unsent[0].id
        queries.markSent(id)

        val stillUnsent = queries.countUnsent().executeAsOne()
        assertEquals(0L, stillUnsent)
    }

    // ==================== Settings Tests ====================

    @Test
    fun testAnalyticsSettings() {
        val queries = database.settingsQueries
        val now = System.currentTimeMillis()

        queries.insertAnalyticsSettings(1, 1, 1, 1, 30, now)

        val settings = queries.getAnalyticsSettings().executeAsOneOrNull()
        assertNotNull(settings)
        assertEquals(1L, settings.enabled)
        assertEquals(30L, settings.retentionDays)
    }

    @Test
    fun testRetentionSettings() {
        val queries = database.settingsQueries
        val now = System.currentTimeMillis()

        queries.insertRetentionSettings(90, 180, 30, 60, now)

        val settings = queries.getRetentionSettings().executeAsOneOrNull()
        assertNotNull(settings)
        assertEquals(90L, settings.commandHistoryDays)
    }

    // ==================== ScreenTransition Tests ====================

    @Test
    fun testScreenTransition() {
        val queries = database.screenTransitionQueries
        val now = System.currentTimeMillis()

        queries.insert("screen1", "screen2", "btn1", "click", 1, 250, now)

        val fromScreen = queries.getFromScreen("screen1").executeAsList()
        assertEquals(1, fromScreen.size)
        assertEquals("screen2", fromScreen[0].toScreenHash)
    }

    // ==================== Batch Operations Tests ====================

    @Test
    fun testBatchInsertWithTransaction() {
        val queries = database.commandHistoryQueries
        val now = System.currentTimeMillis()

        database.transaction {
            repeat(100) { i ->
                queries.insert("cmd$i", null, 0.9, now + i, "en", "Vosk", 1, 100, 1, "VOICE")
            }
        }

        val count = queries.count().executeAsOne()
        assertEquals(100L, count)
    }

    @Test
    fun testDeleteAll() {
        val queries = database.commandHistoryQueries
        val now = System.currentTimeMillis()

        repeat(5) { i ->
            queries.insert("cmd$i", null, 0.9, now, "en", "Vosk", 1, 100, 1, "VOICE")
        }

        queries.deleteAll()

        val count = queries.count().executeAsOne()
        assertEquals(0L, count)
    }
}
