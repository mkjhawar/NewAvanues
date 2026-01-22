package com.augmentalis.voiceavanue.service

import android.content.Intent
import android.os.IBinder
import com.augmentalis.voiceavanue.IDatabase
import com.augmentalis.voiceavanue.models.AppSettings
import com.augmentalis.voiceavanue.models.User
import com.augmentalis.voiceavanue.models.VoiceCommand
import com.augmentalis.voiceos.database.Collection
import com.augmentalis.voiceos.database.Database
import com.augmentalis.voiceos.database.DatabaseFactory
import com.augmentalis.voiceos.database.Document
import com.augmentalis.voiceos.database.Query
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for DatabaseService.
 *
 * Tests cover:
 * - Service lifecycle (onCreate, onBind, onDestroy)
 * - Document conversion helpers (6 functions)
 * - User operations (6 methods)
 * - Voice command operations (6 methods)
 * - Settings operations (4 methods)
 * - Maintenance operations (4 methods)
 * - Health and utility methods (2 methods)
 * - Error handling scenarios
 *
 * @author AI Test Generator
 * @version 1.0.0
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseServiceTest {

    private lateinit var serviceController: ServiceController<DatabaseService>
    private lateinit var service: DatabaseService
    private lateinit var binder: IDatabase

    private lateinit var mockDatabase: Database
    private lateinit var mockUsersCollection: Collection
    private lateinit var mockCommandsCollection: Collection
    private lateinit var mockSettingsCollection: Collection

    @Before
    fun setup() {
        // Mock DatabaseFactory
        mockkObject(DatabaseFactory)

        // Create mock database and collections
        mockDatabase = mockk(relaxed = true)
        mockUsersCollection = mockk(relaxed = true)
        mockCommandsCollection = mockk(relaxed = true)
        mockSettingsCollection = mockk(relaxed = true)

        // Configure DatabaseFactory mock
        every { DatabaseFactory.initialize(any()) } just Runs
        every { DatabaseFactory.create(any(), any()) } returns mockDatabase

        // Configure database mock
        every { mockDatabase.open() } just Runs
        every { mockDatabase.close() } just Runs
        every { mockDatabase.flush() } just Runs
        every { mockDatabase.listCollections() } returns listOf("users", "voice_commands", "settings")
        every { mockDatabase.getCollection("users") } returns mockUsersCollection
        every { mockDatabase.getCollection("voice_commands") } returns mockCommandsCollection
        every { mockDatabase.getCollection("settings") } returns mockSettingsCollection
        every { mockDatabase.createCollection(any()) } just Runs
        every { mockDatabase.dropCollection(any()) } just Runs

        // Create service
        serviceController = Robolectric.buildService(DatabaseService::class.java)
        service = serviceController.create().get()

        // Get binder
        val intent = Intent()
        binder = service.onBind(intent) as IDatabase
    }

    @After
    fun tearDown() {
        serviceController.destroy()
        unmockkAll()
    }

    // ===== Service Lifecycle Tests =====

    @Test
    fun `onCreate initializes database`() {
        // Verify DatabaseFactory was initialized
        verify { DatabaseFactory.initialize(any()) }
        verify { DatabaseFactory.create("voiceavanue_db", version = 1) }
        verify { mockDatabase.open() }
    }

    @Test
    fun `onCreate creates all 3 collections when missing`() {
        // Setup: Database with no collections
        every { mockDatabase.listCollections() } returns emptyList()

        // Recreate service to trigger onCreate again
        val newService = Robolectric.buildService(DatabaseService::class.java).create().get()

        // Verify all collections were created
        verify(atLeast = 1) { mockDatabase.createCollection("users") }
        verify(atLeast = 1) { mockDatabase.createCollection("voice_commands") }
        verify(atLeast = 1) { mockDatabase.createCollection("settings") }
    }

    @Test
    fun `onCreate skips existing collections`() {
        // Setup: Database already has all collections
        every { mockDatabase.listCollections() } returns listOf("users", "voice_commands", "settings")

        // Clear previous invocations
        clearMocks(mockDatabase, answers = false)

        // Recreate service
        val newService = Robolectric.buildService(DatabaseService::class.java).create().get()

        // Verify createCollection was NOT called for existing collections
        verify(exactly = 0) { mockDatabase.createCollection("users") }
        verify(exactly = 0) { mockDatabase.createCollection("voice_commands") }
        verify(exactly = 0) { mockDatabase.createCollection("settings") }
    }

    @Test
    fun `onBind returns valid binder`() {
        val intent = Intent()
        val result = service.onBind(intent)

        assertNotNull(result)
        assertTrue(result is IDatabase)
    }

    @Test
    fun `onDestroy closes database`() {
        serviceController.destroy()

        verify { mockDatabase.close() }
    }

    // ===== Document Conversion Tests =====

    @Test
    fun `userToDocument and documentToUser round-trip conversion`() {
        val originalUser = User(
            id = 42,
            name = "John Doe",
            email = "john@example.com",
            createdAt = 1234567890L,
            lastLoginAt = 9876543210L
        )

        // Insert user (triggers conversion to document)
        binder.insertUser(originalUser)

        // Capture the inserted document
        val documentSlot = slot<Document>()
        verify { mockUsersCollection.insert(capture(documentSlot)) }

        val document = documentSlot.captured

        // Verify document fields
        assertEquals("42", document.id)
        assertEquals("John Doe", document.getString("name"))
        assertEquals("john@example.com", document.getString("email"))
        assertEquals("1234567890", document.getString("createdAt"))
        assertEquals("9876543210", document.getString("lastLoginAt"))
    }

    @Test
    fun `documentToUser handles null lastLoginAt`() {
        val doc = Document(
            id = "1",
            data = mapOf(
                "id" to "1",
                "name" to "Jane Doe",
                "email" to "jane@example.com",
                "createdAt" to "1234567890",
                "lastLoginAt" to ""  // Empty string represents null
            )
        )

        every { mockUsersCollection.findById("1") } returns doc

        val user = binder.getUserById(1)

        assertNotNull(user)
        assertEquals("Jane Doe", user.name)
        assertNull(user.lastLoginAt)
    }

    @Test
    fun `documentToUser handles invalid document gracefully`() {
        val invalidDoc = Document(
            id = "bad",
            data = mapOf("invalid" to "data")
        )

        every { mockUsersCollection.findById("1") } returns invalidDoc

        // Should return null for invalid document
        val user = binder.getUserById(1)

        // Due to null safety, will return User with default values (0, "", "", 0L, null)
        assertNotNull(user)
        assertEquals(0, user.id)
        assertEquals("", user.name)
    }

    @Test
    fun `voiceCommandToDocument and documentToVoiceCommand round-trip conversion`() {
        val originalCommand = VoiceCommand(
            id = 99,
            command = "open email",
            action = "com.example.OPEN_EMAIL",
            category = "navigation",
            enabled = true,
            usageCount = 42
        )

        binder.insertVoiceCommand(originalCommand)

        val documentSlot = slot<Document>()
        verify { mockCommandsCollection.insert(capture(documentSlot)) }

        val document = documentSlot.captured

        assertEquals("99", document.id)
        assertEquals("open email", document.getString("command"))
        assertEquals("com.example.OPEN_EMAIL", document.getString("action"))
        assertEquals("navigation", document.getString("category"))
        assertEquals("true", document.getString("enabled"))
        assertEquals("42", document.getString("usageCount"))
    }

    @Test
    fun `documentToVoiceCommand handles missing fields with defaults`() {
        val doc = Document(
            id = "5",
            data = mapOf(
                "id" to "5",
                "command" to "test command",
                "action" to "test action",
                "category" to "test"
                // Missing enabled and usageCount
            )
        )

        every { mockCommandsCollection.findById("5") } returns doc

        val command = binder.getVoiceCommandById(5)

        assertNotNull(command)
        assertEquals("test command", command.command)
        assertTrue(command.enabled)  // Default
        assertEquals(0, command.usageCount)  // Default
    }

    @Test
    fun `appSettingsToDocument and documentToAppSettings round-trip conversion`() {
        val originalSettings = AppSettings(
            id = 1,
            voiceEnabled = false,
            theme = "dark",
            language = "es",
            notificationsEnabled = false
        )

        binder.updateSettings(originalSettings)

        val updatesSlot = slot<Map<String, String>>()
        verify { mockSettingsCollection.updateById("1", capture(updatesSlot)) }

        val updates = updatesSlot.captured

        assertEquals("false", updates["voiceEnabled"])
        assertEquals("dark", updates["theme"])
        assertEquals("es", updates["language"])
        assertEquals("false", updates["notificationsEnabled"])
    }

    // ===== User Operations Tests =====

    @Test
    fun `getAllUsers returns empty list when no users`() {
        every { mockUsersCollection.find(any()) } returns emptyList()

        val users = binder.getAllUsers()

        assertNotNull(users)
        assertEquals(0, users.size)
    }

    @Test
    fun `getAllUsers returns all users`() {
        val docs = listOf(
            Document("1", mapOf("id" to "1", "name" to "User1", "email" to "u1@test.com", "createdAt" to "1000")),
            Document("2", mapOf("id" to "2", "name" to "User2", "email" to "u2@test.com", "createdAt" to "2000"))
        )

        every { mockUsersCollection.find(Query.all()) } returns docs

        val users = binder.getAllUsers()

        assertEquals(2, users.size)
        assertEquals("User1", users[0].name)
        assertEquals("User2", users[1].name)
    }

    @Test
    fun `getUserById returns null for non-existent user`() {
        every { mockUsersCollection.findById("999") } returns null

        val user = binder.getUserById(999)

        assertNull(user)
    }

    @Test
    fun `getUserById returns correct user`() {
        val doc = Document(
            "42",
            mapOf(
                "id" to "42",
                "name" to "John",
                "email" to "john@test.com",
                "createdAt" to "1234567890"
            )
        )

        every { mockUsersCollection.findById("42") } returns doc

        val user = binder.getUserById(42)

        assertNotNull(user)
        assertEquals(42, user.id)
        assertEquals("John", user.name)
    }

    @Test
    fun `insertUser adds user to collection`() {
        val user = User.createNew("Test User", "test@example.com")

        binder.insertUser(user)

        verify { mockUsersCollection.insert(any()) }
    }

    @Test
    fun `updateUser modifies existing user`() {
        val user = User(
            id = 10,
            name = "Updated Name",
            email = "updated@test.com",
            createdAt = 1000L,
            lastLoginAt = 2000L
        )

        binder.updateUser(user)

        val updatesSlot = slot<Map<String, String>>()
        verify { mockUsersCollection.updateById("10", capture(updatesSlot)) }

        val updates = updatesSlot.captured
        assertEquals("Updated Name", updates["name"])
        assertEquals("updated@test.com", updates["email"])
    }

    @Test
    fun `deleteUser removes user`() {
        binder.deleteUser(123)

        verify { mockUsersCollection.deleteById("123") }
    }

    @Test
    fun `getUserCount returns correct count`() {
        every { mockUsersCollection.count() } returns 42

        val count = binder.getUserCount()

        assertEquals(42, count)
    }

    @Test
    fun `getUserCount returns 0 when collection is null`() {
        every { mockDatabase.getCollection("users") } returns null

        val count = binder.getUserCount()

        assertEquals(0, count)
    }

    // ===== Voice Command Operations Tests =====

    @Test
    fun `getAllVoiceCommands returns all commands`() {
        val docs = listOf(
            Document("1", mapOf("id" to "1", "command" to "cmd1", "action" to "act1", "category" to "cat1")),
            Document("2", mapOf("id" to "2", "command" to "cmd2", "action" to "act2", "category" to "cat2"))
        )

        every { mockCommandsCollection.find(Query.all()) } returns docs

        val commands = binder.getAllVoiceCommands()

        assertEquals(2, commands.size)
        assertEquals("cmd1", commands[0].command)
        assertEquals("cmd2", commands[1].command)
    }

    @Test
    fun `getVoiceCommandById returns correct command`() {
        val doc = Document(
            "7",
            mapOf(
                "id" to "7",
                "command" to "test cmd",
                "action" to "test action",
                "category" to "test cat"
            )
        )

        every { mockCommandsCollection.findById("7") } returns doc

        val command = binder.getVoiceCommandById(7)

        assertNotNull(command)
        assertEquals(7, command.id)
        assertEquals("test cmd", command.command)
    }

    @Test
    fun `getVoiceCommandsByCategory filters correctly`() {
        val docs = listOf(
            Document("1", mapOf("id" to "1", "command" to "cmd1", "action" to "act1", "category" to "navigation")),
            Document("2", mapOf("id" to "2", "command" to "cmd2", "action" to "act2", "category" to "navigation"))
        )

        every { mockCommandsCollection.find(Query.where("category", "navigation")) } returns docs

        val commands = binder.getVoiceCommandsByCategory("navigation")

        assertEquals(2, commands.size)
        assertEquals("navigation", commands[0].category)
        assertEquals("navigation", commands[1].category)
    }

    @Test
    fun `insertVoiceCommand adds command`() {
        val command = VoiceCommand.createNew("open app", "com.example.OPEN", "system")

        binder.insertVoiceCommand(command)

        verify { mockCommandsCollection.insert(any()) }
    }

    @Test
    fun `updateVoiceCommand modifies command`() {
        val command = VoiceCommand(
            id = 20,
            command = "updated command",
            action = "updated action",
            category = "updated category",
            enabled = false,
            usageCount = 100
        )

        binder.updateVoiceCommand(command)

        val updatesSlot = slot<Map<String, String>>()
        verify { mockCommandsCollection.updateById("20", capture(updatesSlot)) }

        val updates = updatesSlot.captured
        assertEquals("updated command", updates["command"])
        assertEquals("false", updates["enabled"])
        assertEquals("100", updates["usageCount"])
    }

    @Test
    fun `deleteVoiceCommand removes command`() {
        binder.deleteVoiceCommand(456)

        verify { mockCommandsCollection.deleteById("456") }
    }

    // ===== Settings Operations Tests =====

    @Test
    fun `getSettings returns default settings when no settings exist`() {
        every { mockSettingsCollection.find(Query.all()) } returns emptyList()

        val settings = binder.getSettings()

        assertNotNull(settings)
        assertEquals(1, settings.id)
        assertEquals("system", settings.theme)
        assertEquals("en", settings.language)
        assertTrue(settings.voiceEnabled)
        assertTrue(settings.notificationsEnabled)
    }

    @Test
    fun `getSettings returns existing settings`() {
        val doc = Document(
            "1",
            mapOf(
                "id" to "1",
                "voiceEnabled" to "false",
                "theme" to "dark",
                "language" to "fr",
                "notificationsEnabled" to "false"
            )
        )

        every { mockSettingsCollection.find(Query.all()) } returns listOf(doc)

        val settings = binder.getSettings()

        assertNotNull(settings)
        assertFalse(settings.voiceEnabled)
        assertEquals("dark", settings.theme)
        assertEquals("fr", settings.language)
        assertFalse(settings.notificationsEnabled)
    }

    @Test
    fun `updateSettings modifies settings`() {
        val settings = AppSettings(
            id = 1,
            voiceEnabled = true,
            theme = "light",
            language = "de",
            notificationsEnabled = true
        )

        binder.updateSettings(settings)

        verify { mockSettingsCollection.updateById("1", any()) }
    }

    @Test
    fun `getSettingValue retrieves specific value`() {
        val doc = Document(
            "1",
            mapOf(
                "id" to "1",
                "theme" to "dark",
                "language" to "en"
            )
        )

        every { mockSettingsCollection.find(Query.all()) } returns listOf(doc)

        val theme = binder.getSettingValue("theme")

        assertEquals("dark", theme)
    }

    @Test
    fun `getSettingValue returns null for missing key`() {
        val doc = Document("1", mapOf("id" to "1"))

        every { mockSettingsCollection.find(Query.all()) } returns listOf(doc)

        val value = binder.getSettingValue("nonexistent")

        assertNull(value)
    }

    @Test
    fun `setSettingValue updates specific value`() {
        val doc = Document("1", mapOf("id" to "1", "theme" to "light"))

        every { mockSettingsCollection.find(Query.all()) } returns listOf(doc)

        binder.setSettingValue("theme", "dark")

        val updatesSlot = slot<Map<String, String>>()
        verify { mockSettingsCollection.updateById("1", capture(updatesSlot)) }

        assertEquals("dark", updatesSlot.captured["theme"])
    }

    @Test
    fun `setSettingValue handles no existing settings document`() {
        every { mockSettingsCollection.find(Query.all()) } returns emptyList()

        // Should not throw exception
        binder.setSettingValue("theme", "dark")

        // Should not call updateById when no document exists
        verify(exactly = 0) { mockSettingsCollection.updateById(any(), any()) }
    }

    // ===== Maintenance Operations Tests =====

    @Test
    fun `clearAllData drops and recreates collections`() {
        binder.clearAllData()

        verify { mockDatabase.dropCollection("users") }
        verify { mockDatabase.dropCollection("voice_commands") }
        verify { mockDatabase.dropCollection("settings") }

        // Collections should be recreated
        verify(atLeast = 1) { mockDatabase.createCollection("users") }
        verify(atLeast = 1) { mockDatabase.createCollection("voice_commands") }
        verify(atLeast = 1) { mockDatabase.createCollection("settings") }
    }

    @Test
    fun `getDatabaseSize returns non-negative value`() {
        val size = binder.getDatabaseSize()

        assertTrue(size >= 0)
    }

    @Test
    fun `vacuum flushes operations`() {
        binder.vacuum()

        verify { mockDatabase.flush() }
    }

    @Test
    fun `getDatabaseVersion returns correct version`() {
        val version = binder.getDatabaseVersion()

        assertEquals("1.0.0", version)
    }

    // ===== Health & Utility Tests =====

    @Test
    fun `isHealthy returns true when all collections exist`() {
        every { mockDatabase.listCollections() } returns listOf("users", "voice_commands", "settings")

        val healthy = binder.isHealthy()

        assertTrue(healthy)
    }

    @Test
    fun `isHealthy returns false when collections missing`() {
        every { mockDatabase.listCollections() } returns listOf("users")  // Missing two collections

        val healthy = binder.isHealthy()

        assertFalse(healthy)
    }

    @Test
    fun `isHealthy returns false on exception`() {
        every { mockDatabase.listCollections() } throws RuntimeException("Database error")

        val healthy = binder.isHealthy()

        assertFalse(healthy)
    }

    @Test
    fun `getLastAccessTime updates on operations`() {
        val timeBefore = binder.getLastAccessTime()

        Thread.sleep(10)  // Small delay

        // Perform an operation
        binder.getUserCount()

        val timeAfter = binder.getLastAccessTime()

        assertTrue(timeAfter > timeBefore)
    }

    // ===== Error Handling Tests =====

    @Test
    fun `operations handle null collection gracefully`() {
        every { mockDatabase.getCollection("users") } returns null

        // Should not throw, should return safe defaults
        val users = binder.getAllUsers()
        val user = binder.getUserById(1)
        val count = binder.getUserCount()

        assertEquals(0, users.size)
        assertNull(user)
        assertEquals(0, count)
    }

    @Test
    fun `operations catch and log exceptions`() {
        every { mockUsersCollection.find(any()) } throws RuntimeException("Database error")

        // Should not throw, should return safe default
        val users = binder.getAllUsers()

        assertEquals(0, users.size)
    }

    @Test
    fun `insert operations handle exceptions gracefully`() {
        every { mockUsersCollection.insert(any()) } throws RuntimeException("Insert error")

        val user = User.createNew("Test", "test@example.com")

        // Should not throw exception
        binder.insertUser(user)

        // Operation attempted
        verify { mockUsersCollection.insert(any()) }
    }

    @Test
    fun `update operations handle exceptions gracefully`() {
        every { mockUsersCollection.updateById(any(), any()) } throws RuntimeException("Update error")

        val user = User(1, "Test", "test@example.com", 1000L)

        // Should not throw exception
        binder.updateUser(user)

        // Operation attempted
        verify { mockUsersCollection.updateById(any(), any()) }
    }

    @Test
    fun `delete operations handle exceptions gracefully`() {
        every { mockUsersCollection.deleteById(any()) } throws RuntimeException("Delete error")

        // Should not throw exception
        binder.deleteUser(1)

        // Operation attempted
        verify { mockUsersCollection.deleteById("1") }
    }

    @Test
    fun `getAllVoiceCommands handles null collection`() {
        every { mockDatabase.getCollection("voice_commands") } returns null

        val commands = binder.getAllVoiceCommands()

        assertEquals(0, commands.size)
    }

    @Test
    fun `getSettings handles null collection`() {
        every { mockDatabase.getCollection("settings") } returns null

        val settings = binder.getSettings()

        // Should return default settings
        assertNotNull(settings)
        assertEquals(1, settings.id)
        assertEquals("system", settings.theme)
    }

    @Test
    fun `clearAllData handles exceptions during drop`() {
        every { mockDatabase.dropCollection("users") } throws RuntimeException("Drop error")

        // Should not throw exception
        binder.clearAllData()

        // Operation attempted
        verify { mockDatabase.dropCollection("users") }
    }
}
