package com.augmentalis.webavanue

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.db.SqlDriver
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.BrowserRepositoryImpl
import com.augmentalis.webavanue.Tab
import com.augmentalis.webavanue.createAndroidDriver
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Ignore
import org.robolectric.annotation.Config
import java.io.File

/**
 * Integration tests for encrypted database operations.
 *
 * Tests that SQLCipher encryption works correctly with SQLDelight
 * and that all database operations function properly with encryption.
 * Requires device/emulator — SQLCipher native lib unavailable in Robolectric.
 */
@Ignore("Requires device/emulator — SQLCipher native lib unavailable in Robolectric")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EncryptedDatabaseTest {

    private lateinit var context: Context
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var driver: SqlDriver
    private lateinit var database: BrowserDatabase
    private lateinit var repository: BrowserRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        encryptionManager = EncryptionManager(context)

        // Delete any existing databases
        cleanupDatabases()

        // Create encrypted driver
        driver = createAndroidDriver(context, useEncryption = true)
        database = BrowserDatabase(driver)
        repository = BrowserRepositoryImpl(database)
    }

    @After
    fun cleanup() {
        try {
            driver.close()
            encryptionManager.deleteEncryptionKey()
            cleanupDatabases()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    private fun cleanupDatabases() {
        val dbFiles = listOf(
            "browser.db",
            "browser_encrypted.db",
            "browser_plaintext_backup.db"
        )

        dbFiles.forEach { dbName ->
            val dbFile = context.getDatabasePath(dbName)
            if (dbFile.exists()) {
                dbFile.delete()
            }
            // Also delete journal files
            File(dbFile.absolutePath + "-journal").delete()
            File(dbFile.absolutePath + "-shm").delete()
            File(dbFile.absolutePath + "-wal").delete()
        }
    }

    @Test
    fun `encrypted database is created successfully`() {
        // GIVEN: Database driver created with encryption
        // WHEN: Checking database file
        val encryptedDbFile = context.getDatabasePath("browser_encrypted.db")

        // THEN: Encrypted database file exists
        assertTrue("Encrypted database should exist", encryptedDbFile.exists())

        // AND: Encryption key was created
        assertTrue("Encryption key should exist", encryptionManager.hasEncryptionKey())
    }

    @Test
    fun `can insert and retrieve data from encrypted database`() = runBlocking {
        // GIVEN: A new tab
        val tab = Tab(
            id = "test-tab-1",
            url = "https://example.com",
            title = "Example Site",
            favicon = null,
            isActive = true,
            isPinned = false,
            isIncognito = false,
            createdAt = Clock.System.now(),
            lastAccessedAt = Clock.System.now(),
            position = 0,
            parentTabId = null,
            groupId = null,
            sessionData = null,
            scrollXPosition = 0,
            scrollYPosition = 0,
            zoomLevel = 100,
            isDesktopMode = false
        )

        // WHEN: Inserting tab into encrypted database
        val insertResult = repository.createTab(tab)

        // THEN: Insert succeeds
        assertTrue("Insert should succeed", insertResult.isSuccess)

        // AND: Tab can be retrieved
        val retrieveResult = repository.getTab("test-tab-1")
        assertTrue("Retrieve should succeed", retrieveResult.isSuccess)

        val retrievedTab = retrieveResult.getOrNull()
        assertNotNull("Tab should be retrieved", retrievedTab)
        assertEquals("URLs should match", tab.url, retrievedTab?.url)
        assertEquals("Titles should match", tab.title, retrievedTab?.title)
    }

    @Test
    fun `can update data in encrypted database`() = runBlocking {
        // GIVEN: Tab in database
        val tab = Tab(
            id = "test-tab-2",
            url = "https://example.com",
            title = "Original Title",
            favicon = null,
            isActive = true,
            isPinned = false,
            isIncognito = false,
            createdAt = Clock.System.now(),
            lastAccessedAt = Clock.System.now(),
            position = 0,
            parentTabId = null,
            groupId = null,
            sessionData = null,
            scrollXPosition = 0,
            scrollYPosition = 0,
            zoomLevel = 100,
            isDesktopMode = false
        )
        repository.createTab(tab)

        // WHEN: Updating tab
        val updatedTab = tab.copy(title = "Updated Title")
        val updateResult = repository.updateTab(updatedTab)

        // THEN: Update succeeds
        assertTrue("Update should succeed", updateResult.isSuccess)

        // AND: Updated data is retrieved
        val retrievedTab = repository.getTab("test-tab-2").getOrNull()
        assertEquals("Title should be updated", "Updated Title", retrievedTab?.title)
    }

    @Test
    fun `can delete data from encrypted database`() = runBlocking {
        // GIVEN: Tab in database
        val tab = Tab(
            id = "test-tab-3",
            url = "https://example.com",
            title = "Test Tab",
            favicon = null,
            isActive = true,
            isPinned = false,
            isIncognito = false,
            createdAt = Clock.System.now(),
            lastAccessedAt = Clock.System.now(),
            position = 0,
            parentTabId = null,
            groupId = null,
            sessionData = null,
            scrollXPosition = 0,
            scrollYPosition = 0,
            zoomLevel = 100,
            isDesktopMode = false
        )
        repository.createTab(tab)

        // WHEN: Deleting tab
        val deleteResult = repository.closeTab("test-tab-3")

        // THEN: Delete succeeds
        assertTrue("Delete should succeed", deleteResult.isSuccess)

        // AND: Tab is no longer retrievable
        val retrieveResult = repository.getTab("test-tab-3")
        val retrievedTab = retrieveResult.getOrNull()
        assertNull("Tab should be deleted", retrievedTab)
    }

    @Test
    fun `encrypted database persists data across driver reopens`() = runBlocking {
        // GIVEN: Tab inserted in database
        val tab = Tab(
            id = "test-tab-4",
            url = "https://example.com",
            title = "Persistent Tab",
            favicon = null,
            isActive = true,
            isPinned = false,
            isIncognito = false,
            createdAt = Clock.System.now(),
            lastAccessedAt = Clock.System.now(),
            position = 0,
            parentTabId = null,
            groupId = null,
            sessionData = null,
            scrollXPosition = 0,
            scrollYPosition = 0,
            zoomLevel = 100,
            isDesktopMode = false
        )
        repository.createTab(tab)

        // WHEN: Closing and reopening database
        driver.close()

        val newDriver = createAndroidDriver(context, useEncryption = true)
        val newDatabase = BrowserDatabase(newDriver)
        val newRepository = BrowserRepositoryImpl(newDatabase)

        // Wait for repository initialization
        kotlinx.coroutines.delay(1000)

        // THEN: Data persists
        val retrievedTab = newRepository.getTab("test-tab-4").getOrNull()
        assertNotNull("Tab should persist", retrievedTab)
        assertEquals("URL should match", tab.url, retrievedTab?.url)
        assertEquals("Title should match", tab.title, retrievedTab?.title)

        // Cleanup
        newDriver.close()
    }

    @Test
    fun `encrypted database cannot be read without correct passphrase`() {
        // GIVEN: Encrypted database with data
        val tab = Tab(
            id = "test-tab-5",
            url = "https://example.com",
            title = "Secret Tab",
            favicon = null,
            isActive = true,
            isPinned = false,
            isIncognito = false,
            createdAt = Clock.System.now(),
            lastAccessedAt = Clock.System.now(),
            position = 0,
            parentTabId = null,
            groupId = null,
            sessionData = null,
            scrollXPosition = 0,
            scrollYPosition = 0,
            zoomLevel = 100,
            isDesktopMode = false
        )
        runBlocking {
            repository.createTab(tab)
        }

        driver.close()

        // WHEN: Trying to open with wrong passphrase (delete key and create new one)
        encryptionManager.deleteEncryptionKey()

        // Attempting to create new driver with different key should fail or create new DB
        try {
            val newDriver = createAndroidDriver(context, useEncryption = true)
            val newDatabase = BrowserDatabase(newDriver)
            val newRepository = BrowserRepositoryImpl(newDatabase)

            // THEN: Data should not be accessible (new empty database)
            runBlocking {
                kotlinx.coroutines.delay(1000) // Wait for initialization
                val tabs = newRepository.getAllTabs().getOrNull()
                // New database should be empty or throw error
                assertTrue("New database should be empty", tabs?.isEmpty() ?: true)
            }

            newDriver.close()
        } catch (e: Exception) {
            // Expected - cannot decrypt with wrong key
            println("Expected error when opening with wrong key: ${e.message}")
        }
    }

    @Test
    fun `multiple operations maintain data integrity in encrypted database`() = runBlocking {
        // GIVEN: Multiple tabs
        val tabs = listOf(
            Tab(
                id = "tab-1",
                url = "https://example1.com",
                title = "Tab 1",
                favicon = null,
                isActive = true,
                isPinned = false,
                isIncognito = false,
                createdAt = Clock.System.now(),
                lastAccessedAt = Clock.System.now(),
                position = 0,
                parentTabId = null,
                groupId = null,
                sessionData = null,
                scrollXPosition = 0,
                scrollYPosition = 0,
                zoomLevel = 100,
                isDesktopMode = false
            ),
            Tab(
                id = "tab-2",
                url = "https://example2.com",
                title = "Tab 2",
                favicon = null,
                isActive = false,
                isPinned = true,
                isIncognito = false,
                createdAt = Clock.System.now(),
                lastAccessedAt = Clock.System.now(),
                position = 1,
                parentTabId = null,
                groupId = null,
                sessionData = null,
                scrollXPosition = 0,
                scrollYPosition = 0,
                zoomLevel = 100,
                isDesktopMode = false
            )
        )

        // WHEN: Performing multiple operations
        tabs.forEach { repository.createTab(it) }
        repository.updateTab(tabs[0].copy(title = "Updated Tab 1"))
        repository.closeTab("tab-2")

        // THEN: Data integrity is maintained
        val retrievedTabs = repository.getAllTabs().getOrNull() ?: emptyList()
        assertEquals("Should have 1 tab", 1, retrievedTabs.size)
        assertEquals("Title should be updated", "Updated Tab 1", retrievedTabs[0].title)
    }
}
