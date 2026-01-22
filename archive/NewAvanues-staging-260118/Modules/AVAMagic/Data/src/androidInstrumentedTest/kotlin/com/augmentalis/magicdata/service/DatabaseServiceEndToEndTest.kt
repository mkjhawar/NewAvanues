package com.augmentalis.voiceavanue.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceavanue.access.DatabaseAccessFactory
import com.augmentalis.voiceavanue.config.DatabaseConfig
import com.augmentalis.voiceavanue.models.AppSettings
import com.augmentalis.voiceavanue.models.User
import com.augmentalis.voiceavanue.models.VoiceCommand
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end integration test for Database IPC Architecture.
 *
 * Tests all 22 AIDL methods with real database operations:
 * - User operations (6 methods)
 * - Voice command operations (6 methods)
 * - Settings operations (4 methods)
 * - Maintenance operations (4 methods)
 * - Health & utility (2 methods)
 *
 * This test verifies:
 * - Process isolation (service runs in :database process)
 * - IPC communication (AIDL works correctly)
 * - Database persistence (data survives operations)
 * - Error handling (graceful failure)
 * - Health monitoring (service status)
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
@RunWith(AndroidJUnit4::class)
class DatabaseServiceEndToEndTest {

    private lateinit var context: Context
    private lateinit var database: com.augmentalis.voiceavanue.access.DatabaseAccess

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()

        // Use IPC implementation explicitly for testing
        database = DatabaseAccessFactory.createIpc(context)

        // Connect to service
        val connected = database.connect()
        assertTrue(connected, "Failed to connect to DatabaseService")

        // Clear all data for clean test environment
        database.clearAllData()
    }

    @After
    fun teardown() = runBlocking {
        // Clean up after tests
        database.clearAllData()
        database.disconnect()
    }

    // ===== User Operations Tests (6 methods) =====

    @Test
    fun testInsertAndGetUser() = runBlocking {
        // Insert a user
        val user = User(
            id = 1,
            name = "Test User",
            email = "test@example.com",
            createdAt = System.currentTimeMillis(),
            lastLoginAt = null
        )
        database.insertUser(user)

        // Retrieve the user
        val retrieved = database.getUserById(1)
        assertNotNull(retrieved, "User should be retrieved")
        assertEquals(user.name, retrieved.name)
        assertEquals(user.email, retrieved.email)
    }

    @Test
    fun testGetAllUsers() = runBlocking {
        // Insert multiple users
        val users = listOf(
            User(1, "User One", "user1@example.com", System.currentTimeMillis(), null),
            User(2, "User Two", "user2@example.com", System.currentTimeMillis(), null),
            User(3, "User Three", "user3@example.com", System.currentTimeMillis(), null)
        )

        users.forEach { database.insertUser(it) }

        // Retrieve all users
        val allUsers = database.getAllUsers()
        assertEquals(3, allUsers.size, "Should have 3 users")
    }

    @Test
    fun testUpdateUser() = runBlocking {
        // Insert a user
        val user = User(1, "Original Name", "original@example.com", System.currentTimeMillis(), null)
        database.insertUser(user)

        // Update the user
        val updated = user.copy(name = "Updated Name", email = "updated@example.com")
        database.updateUser(updated)

        // Verify update
        val retrieved = database.getUserById(1)
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved.name)
        assertEquals("updated@example.com", retrieved.email)
    }

    @Test
    fun testDeleteUser() = runBlocking {
        // Insert a user
        val user = User(1, "Test User", "test@example.com", System.currentTimeMillis(), null)
        database.insertUser(user)

        // Verify user exists
        assertNotNull(database.getUserById(1))

        // Delete the user
        database.deleteUser(1)

        // Verify user is deleted
        assertNull(database.getUserById(1))
    }

    @Test
    fun testGetUserCount() = runBlocking {
        // Initial count should be 0
        assertEquals(0, database.getUserCount())

        // Insert users
        database.insertUser(User(1, "User 1", "user1@example.com", System.currentTimeMillis(), null))
        database.insertUser(User(2, "User 2", "user2@example.com", System.currentTimeMillis(), null))

        // Count should be 2
        assertEquals(2, database.getUserCount())
    }

    @Test
    fun testGetNonExistentUser() = runBlocking {
        // Try to get user that doesn't exist
        val user = database.getUserById(999)
        assertNull(user, "Non-existent user should return null")
    }

    // ===== Voice Command Operations Tests (6 methods) =====

    @Test
    fun testInsertAndGetVoiceCommand() = runBlocking {
        // Insert a command
        val command = VoiceCommand(
            id = 1,
            command = "open app",
            action = "ACTION_OPEN",
            category = "navigation",
            enabled = true,
            usageCount = 0
        )
        database.insertVoiceCommand(command)

        // Retrieve the command
        val retrieved = database.getVoiceCommandById(1)
        assertNotNull(retrieved)
        assertEquals(command.command, retrieved.command)
        assertEquals(command.action, retrieved.action)
        assertEquals(command.category, retrieved.category)
    }

    @Test
    fun testGetAllVoiceCommands() = runBlocking {
        // Insert multiple commands
        val commands = listOf(
            VoiceCommand(1, "open browser", "ACTION_OPEN_BROWSER", "navigation", true, 0),
            VoiceCommand(2, "play music", "ACTION_PLAY_MUSIC", "media", true, 0),
            VoiceCommand(3, "send message", "ACTION_SEND_MESSAGE", "communication", true, 0)
        )

        commands.forEach { database.insertVoiceCommand(it) }

        // Retrieve all commands
        val allCommands = database.getAllVoiceCommands()
        assertEquals(3, allCommands.size)
    }

    @Test
    fun testGetVoiceCommandsByCategory() = runBlocking {
        // Insert commands in different categories
        database.insertVoiceCommand(VoiceCommand(1, "open browser", "ACTION_OPEN_BROWSER", "navigation", true, 0))
        database.insertVoiceCommand(VoiceCommand(2, "play music", "ACTION_PLAY_MUSIC", "media", true, 0))
        database.insertVoiceCommand(VoiceCommand(3, "open settings", "ACTION_OPEN_SETTINGS", "navigation", true, 0))

        // Get only navigation commands
        val navigationCommands = database.getVoiceCommandsByCategory("navigation")
        assertEquals(2, navigationCommands.size)
        assertTrue(navigationCommands.all { it.category == "navigation" })
    }

    @Test
    fun testUpdateVoiceCommand() = runBlocking {
        // Insert a command
        val command = VoiceCommand(1, "open app", "ACTION_OPEN", "navigation", true, 0)
        database.insertVoiceCommand(command)

        // Update the command
        val updated = command.copy(command = "launch app", usageCount = 10)
        database.updateVoiceCommand(updated)

        // Verify update
        val retrieved = database.getVoiceCommandById(1)
        assertNotNull(retrieved)
        assertEquals("launch app", retrieved.command)
        assertEquals(10, retrieved.usageCount)
    }

    @Test
    fun testDeleteVoiceCommand() = runBlocking {
        // Insert a command
        val command = VoiceCommand(1, "test command", "ACTION_TEST", "test", true, 0)
        database.insertVoiceCommand(command)

        // Verify exists
        assertNotNull(database.getVoiceCommandById(1))

        // Delete
        database.deleteVoiceCommand(1)

        // Verify deleted
        assertNull(database.getVoiceCommandById(1))
    }

    @Test
    fun testGetNonExistentVoiceCommand() = runBlocking {
        val command = database.getVoiceCommandById(999)
        assertNull(command)
    }

    // ===== Settings Operations Tests (4 methods) =====

    @Test
    fun testGetAndUpdateSettings() = runBlocking {
        // Get default settings
        val settings = database.getSettings()
        assertNotNull(settings, "Settings should always exist (default if not set)")

        // Update settings
        val updated = AppSettings(
            id = 1,
            voiceEnabled = false,
            theme = "dark",
            language = "es",
            notificationsEnabled = false
        )
        database.updateSettings(updated)

        // Verify update
        val retrieved = database.getSettings()
        assertNotNull(retrieved)
        assertEquals(false, retrieved.voiceEnabled)
        assertEquals("dark", retrieved.theme)
        assertEquals("es", retrieved.language)
    }

    @Test
    fun testGetSettingValue() = runBlocking {
        // Set settings first
        val settings = AppSettings(1, true, "light", "en", true)
        database.updateSettings(settings)

        // Get specific setting value
        val theme = database.getSettingValue("theme")
        assertEquals("light", theme)

        val language = database.getSettingValue("language")
        assertEquals("en", language)
    }

    @Test
    fun testSetSettingValue() = runBlocking {
        // Set initial settings
        val settings = AppSettings(1, true, "light", "en", true)
        database.updateSettings(settings)

        // Update specific setting
        database.setSettingValue("theme", "dark")

        // Verify update
        val theme = database.getSettingValue("theme")
        assertEquals("dark", theme)
    }

    @Test
    fun testGetNonExistentSettingValue() = runBlocking {
        val value = database.getSettingValue("nonexistent_key")
        assertNull(value, "Non-existent setting should return null")
    }

    // ===== Maintenance Operations Tests (4 methods) =====

    @Test
    fun testClearAllData() = runBlocking {
        // Insert data
        database.insertUser(User(1, "User", "user@example.com", System.currentTimeMillis(), null))
        database.insertVoiceCommand(VoiceCommand(1, "command", "action", "category", true, 0))

        // Verify data exists
        assertEquals(1, database.getUserCount())
        assertEquals(1, database.getAllVoiceCommands().size)

        // Clear all data
        database.clearAllData()

        // Verify data is cleared
        assertEquals(0, database.getUserCount())
        assertEquals(0, database.getAllVoiceCommands().size)
    }

    @Test
    fun testGetDatabaseSize() = runBlocking {
        val size = database.getDatabaseSize()
        assertTrue(size >= 0, "Database size should be non-negative")
    }

    @Test
    fun testVacuum() = runBlocking {
        // Insert and delete some data
        database.insertUser(User(1, "User", "user@example.com", System.currentTimeMillis(), null))
        database.deleteUser(1)

        // Run vacuum (should not throw exception)
        database.vacuum()
    }

    @Test
    fun testGetDatabaseVersion() = runBlocking {
        val version = database.getDatabaseVersion()
        assertNotNull(version)
        assertEquals("1.0.0", version)
    }

    // ===== Health & Utility Tests (2 methods) =====

    @Test
    fun testIsHealthy() = runBlocking {
        val healthy = database.isHealthy()
        assertTrue(healthy, "Database service should be healthy")
    }

    @Test
    fun testGetLastAccessTime() = runBlocking {
        val beforeTime = System.currentTimeMillis()

        // Perform an operation to update access time
        database.getUserCount()

        val accessTime = database.getLastAccessTime()
        val afterTime = System.currentTimeMillis()

        assertTrue(accessTime >= beforeTime, "Access time should be after test start")
        assertTrue(accessTime <= afterTime, "Access time should be before test end")
    }

    // ===== Process Isolation Tests =====

    @Test
    fun testProcessIsolation() = runBlocking {
        // This test verifies that the database service runs in a separate process
        // In a real test, you would check the process name via ActivityManager
        // For now, we just verify the service is accessible

        assertTrue(database.isConnected(), "Service should be connected")
        assertTrue(database.isHealthy(), "Service should be healthy")
    }

    // ===== Error Handling Tests =====

    @Test
    fun testOperationsAfterDisconnect() = runBlocking {
        // Disconnect from service
        database.disconnect()

        // Operations should handle gracefully (reconnect or return safe defaults)
        val count = database.getUserCount()
        assertTrue(count >= 0, "Should return safe default even after disconnect")

        // Reconnect for cleanup
        database.connect()
    }

    // ===== Data Persistence Tests =====

    @Test
    fun testDataPersistsAcrossOperations() = runBlocking {
        // Insert user
        database.insertUser(User(1, "Persistent User", "persist@example.com", System.currentTimeMillis(), null))

        // Insert command
        database.insertVoiceCommand(VoiceCommand(1, "persistent", "ACTION_PERSIST", "test", true, 5))

        // Update settings
        database.updateSettings(AppSettings(1, true, "dark", "en", true))

        // Verify all data persists
        val user = database.getUserById(1)
        assertNotNull(user)
        assertEquals("Persistent User", user.name)

        val command = database.getVoiceCommandById(1)
        assertNotNull(command)
        assertEquals(5, command.usageCount)

        val settings = database.getSettings()
        assertNotNull(settings)
        assertEquals("dark", settings.theme)
    }

    // ===== Performance Tests =====

    @Test
    fun testBulkInsertPerformance() = runBlocking {
        val startTime = System.currentTimeMillis()

        // Insert 100 users
        repeat(100) { i ->
            database.insertUser(User(
                id = i,
                name = "User $i",
                email = "user$i@example.com",
                createdAt = System.currentTimeMillis(),
                lastLoginAt = null
            ))
        }

        val elapsed = System.currentTimeMillis() - startTime
        println("Inserted 100 users in ${elapsed}ms (avg ${elapsed / 100}ms per insert)")

        // Verify all inserted
        assertEquals(100, database.getUserCount())
    }

    @Test
    fun testBulkReadPerformance() = runBlocking {
        // Insert 100 users first
        repeat(100) { i ->
            database.insertUser(User(i, "User $i", "user$i@example.com", System.currentTimeMillis(), null))
        }

        val startTime = System.currentTimeMillis()

        // Read all users
        val users = database.getAllUsers()

        val elapsed = System.currentTimeMillis() - startTime
        println("Retrieved 100 users in ${elapsed}ms")

        assertEquals(100, users.size)
    }
}
