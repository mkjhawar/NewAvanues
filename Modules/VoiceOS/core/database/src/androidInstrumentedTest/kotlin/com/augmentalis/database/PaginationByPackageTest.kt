/**
 * PaginationByPackageTest.kt - Tests for package-based pagination
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-13
 *
 * Tests pagination functionality for retrieving commands by package name.
 */

package com.augmentalis.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightGeneratedCommandRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaginationByPackageTest {

    private lateinit var database: VoiceOSDatabase
    private lateinit var repository: IGeneratedCommandRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null  // In-memory database for testing
        )
        database = VoiceOSDatabase(driver)
        repository = SQLDelightGeneratedCommandRepository(database)
    }

    @Test
    fun testGetByPackagePaginated_returnsCorrectCommandsForPackage() = runBlocking {
        // Insert commands for different packages
        insertTestCommands("com.google.gmail", count = 50)
        insertTestCommands("com.android.chrome", count = 30)

        // Get first page for Gmail
        val page1 = repository.getByPackagePaginated(
            packageName = "com.google.gmail",
            limit = 20,
            offset = 0
        )

        assertEquals("Should return 20 commands", 20, page1.size)
        assertTrue("All commands should be from Gmail", page1.all { it.appId == "com.google.gmail" })
    }

    @Test
    fun testGetByPackagePaginated_paginationLimitsWork() = runBlocking {
        insertTestCommands("com.test.app", count = 100)

        // Get different page sizes
        val page10 = repository.getByPackagePaginated("com.test.app", limit = 10, offset = 0)
        val page25 = repository.getByPackagePaginated("com.test.app", limit = 25, offset = 0)
        val page50 = repository.getByPackagePaginated("com.test.app", limit = 50, offset = 0)

        assertEquals("Should return 10 commands", 10, page10.size)
        assertEquals("Should return 25 commands", 25, page25.size)
        assertEquals("Should return 50 commands", 50, page50.size)
    }

    @Test
    fun testGetByPackagePaginated_offsetWorksCorrectly() = runBlocking {
        insertTestCommands("com.test.app", count = 100)

        // Get pages with offset
        val page1 = repository.getByPackagePaginated("com.test.app", limit = 25, offset = 0)
        val page2 = repository.getByPackagePaginated("com.test.app", limit = 25, offset = 25)
        val page3 = repository.getByPackagePaginated("com.test.app", limit = 25, offset = 50)

        assertEquals("Page 1 should have 25 commands", 25, page1.size)
        assertEquals("Page 2 should have 25 commands", 25, page2.size)
        assertEquals("Page 3 should have 25 commands", 25, page3.size)

        // Pages should not overlap
        val page1Ids = page1.map { it.id }.toSet()
        val page2Ids = page2.map { it.id }.toSet()
        val page3Ids = page3.map { it.id }.toSet()

        assertEquals("No overlap between page 1 and 2", 0, page1Ids.intersect(page2Ids).size)
        assertEquals("No overlap between page 2 and 3", 0, page2Ids.intersect(page3Ids).size)
    }

    @Test
    fun testGetByPackagePaginated_validatesPackageName() = runBlocking {
        // Empty package name should throw
        var exceptionThrown = false
        try {
            repository.getByPackagePaginated("", 10, 0)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Empty package name should throw IllegalArgumentException", exceptionThrown)
    }

    @Test
    fun testGetByPackagePaginated_validatesLimit() = runBlocking {
        // Limit too small
        var exceptionThrown = false
        try {
            repository.getByPackagePaginated("com.test", 0, 0)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Limit too small should throw IllegalArgumentException", exceptionThrown)

        // Limit too large
        exceptionThrown = false
        try {
            repository.getByPackagePaginated("com.test", 1001, 0)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Limit too large should throw IllegalArgumentException", exceptionThrown)
    }

    @Test
    fun testGetByPackagePaginated_validatesOffset() = runBlocking {
        // Negative offset
        var exceptionThrown = false
        try {
            repository.getByPackagePaginated("com.test", 10, -1)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Negative offset should throw IllegalArgumentException", exceptionThrown)
    }

    @Test
    fun testGetByPackagePaginated_returnsEmptyWhenNoMatches() = runBlocking {
        insertTestCommands("com.google.gmail", count = 10)

        // Query for different package
        val results = repository.getByPackagePaginated("com.nonexistent.app", 10, 0)

        assertEquals("Should return empty list", 0, results.size)
    }

    @Test
    fun testGetByPackagePaginated_handlesPartialLastPage() = runBlocking {
        insertTestCommands("com.test.app", count = 42)

        // Get last page (should have only 2 commands)
        val lastPage = repository.getByPackagePaginated("com.test.app", limit = 20, offset = 40)

        assertEquals("Last page should have 2 commands", 2, lastPage.size)
        assertTrue("All commands should be from test.app", lastPage.all { it.appId == "com.test.app" })
    }

    @Test
    fun testGetByPackagePaginated_isolatesDifferentPackages() = runBlocking {
        insertTestCommands("com.google.gmail", count = 30)
        insertTestCommands("com.android.chrome", count = 40)
        insertTestCommands("com.spotify.music", count = 20)

        // Get all commands for each package
        val gmailCommands = repository.getByPackagePaginated("com.google.gmail", 100, 0)
        val chromeCommands = repository.getByPackagePaginated("com.android.chrome", 100, 0)
        val spotifyCommands = repository.getByPackagePaginated("com.spotify.music", 100, 0)

        assertEquals("Gmail should have 30 commands", 30, gmailCommands.size)
        assertEquals("Chrome should have 40 commands", 40, chromeCommands.size)
        assertEquals("Spotify should have 20 commands", 20, spotifyCommands.size)

        // Verify isolation
        assertTrue("All Gmail commands have correct appId", gmailCommands.all { it.appId == "com.google.gmail" })
        assertTrue("All Chrome commands have correct appId", chromeCommands.all { it.appId == "com.android.chrome" })
        assertTrue("All Spotify commands have correct appId", spotifyCommands.all { it.appId == "com.spotify.music" })
    }

    // KEYSET PAGINATION TESTS

    @Test
    fun testKeysetPagination_worksCorrectly() = runBlocking {
        insertTestCommands("com.test.app", count = 100)

        // Get first page (lastId = 0 for first page)
        val page1 = repository.getByPackageKeysetPaginated(
            packageName = "com.test.app",
            lastId = 0,
            limit = 25
        )

        assertEquals("Page 1 should have 25 commands", 25, page1.size)

        // Get second page using last ID from first page
        val page2 = repository.getByPackageKeysetPaginated(
            packageName = "com.test.app",
            lastId = page1.last().id,
            limit = 25
        )

        assertEquals("Page 2 should have 25 commands", 25, page2.size)

        // No overlap between pages
        assertTrue("Page 1 last ID < Page 2 first ID", page1.last().id < page2.first().id)

        // Pages should be sequential
        val allIds = (page1 + page2).map { it.id }
        assertEquals("Should have 50 total commands", 50, allIds.size)
    }

    @Test
    fun testKeysetPagination_noOverlapBetweenPages() = runBlocking {
        insertTestCommands("com.test.app", count = 100)

        // Get 4 pages
        var lastId = 0L
        val pages = mutableListOf<List<GeneratedCommandDTO>>()

        repeat(4) {
            val page = repository.getByPackageKeysetPaginated("com.test.app", lastId, 25)
            pages.add(page)
            lastId = page.last().id
        }

        // Verify all pages are distinct
        val allIds = pages.flatten().map { it.id }
        val uniqueIds = allIds.toSet()
        assertEquals("All IDs should be unique (no overlap)", allIds.size, uniqueIds.size)
    }

    @Test
    fun testKeysetPagination_validatesInputs() = runBlocking {
        // Empty package name
        var exceptionThrown = false
        try {
            repository.getByPackageKeysetPaginated("", 0, 25)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Empty package should throw", exceptionThrown)

        // Negative lastId
        exceptionThrown = false
        try {
            repository.getByPackageKeysetPaginated("com.test", -1, 25)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Negative lastId should throw", exceptionThrown)

        // Invalid limit
        exceptionThrown = false
        try {
            repository.getByPackageKeysetPaginated("com.test", 0, 0)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Limit too small should throw", exceptionThrown)
    }

    @Test
    fun testKeysetPagination_handlesLastPageCorrectly() = runBlocking {
        insertTestCommands("com.test.app", count = 42)

        // Get first 40 items
        var lastId = 0L
        repeat(2) {
            val page = repository.getByPackageKeysetPaginated("com.test.app", lastId, 20)
            lastId = page.last().id
        }

        // Get last page (should have only 2 items)
        val lastPage = repository.getByPackageKeysetPaginated("com.test.app", lastId, 20)
        assertEquals("Last page should have 2 commands", 2, lastPage.size)
    }

    @Test
    fun testKeysetPagination_isolatesPackages() = runBlocking {
        insertTestCommands("com.google.gmail", count = 50)
        insertTestCommands("com.android.chrome", count = 50)

        // Get Gmail pages
        val gmailPage = repository.getByPackageKeysetPaginated("com.google.gmail", 0, 30)

        // All should be from Gmail
        assertTrue("All commands from Gmail", gmailPage.all { it.appId == "com.google.gmail" })
        assertEquals("Should have 30 Gmail commands", 30, gmailPage.size)
    }

    // Helper function to insert test commands
    private suspend fun insertTestCommands(packageName: String, count: Int) {
        repeat(count) { i ->
            repository.insert(
                GeneratedCommandDTO(
                    id = 0,  // Auto-generated
                    elementHash = "hash_${packageName}_$i",
                    commandText = "command $i",
                    actionType = "CLICK",
                    confidence = 0.8,
                    synonyms = null,
                    isUserApproved = 0L,
                    usageCount = 0,
                    lastUsed = null,
                    createdAt = System.currentTimeMillis(),
                    appId = packageName,
                    appVersion = "",
                    versionCode = 0,
                    lastVerified = System.currentTimeMillis(),
                    isDeprecated = 0
                )
            )
        }
    }
}
