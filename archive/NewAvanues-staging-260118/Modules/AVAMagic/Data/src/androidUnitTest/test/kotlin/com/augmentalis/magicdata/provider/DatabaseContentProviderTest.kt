package com.augmentalis.voiceavanue.provider

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.augmentalis.voiceavanue.client.DatabaseClient
import com.augmentalis.voiceavanue.models.AppSettings
import com.augmentalis.voiceavanue.models.User
import com.augmentalis.voiceavanue.models.VoiceCommand
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ContentProviderController
import kotlin.test.*

/**
 * Comprehensive unit tests for DatabaseContentProvider.
 *
 * Tests cover:
 * - URI matching (6 test cases)
 * - Query operations (6 test cases)
 * - Insert operations (4 test cases)
 * - Update operations (4 test cases)
 * - Delete operations (3 test cases)
 * - Change notifications (3 test cases)
 * - Error handling (3 test cases)
 * - Content types (1 test case)
 *
 * Total: 30 test cases
 *
 * @author AI Test Generator
 * @version 1.0.0
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseContentProviderTest {

    private lateinit var providerController: ContentProviderController<DatabaseContentProvider>
    private lateinit var provider: DatabaseContentProvider
    private lateinit var context: Context
    private lateinit var mockClient: DatabaseClient

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()

        // Mock DatabaseClient
        mockClient = mockk(relaxed = true)
        mockkObject(DatabaseClient)
        every { DatabaseClient.getInstance(any()) } returns mockClient

        // Setup mock client to simulate successful connection
        coEvery { mockClient.connect() } returns true

        // Create provider
        providerController = Robolectric.buildContentProvider(DatabaseContentProvider::class.java)
        provider = providerController.create().get()
    }

    @After
    fun tearDown() {
        providerController.shutdown()
        unmockkAll()
    }

    // ===== URI Matching Tests =====

    @Test
    fun `onCreate initializes client and connects`() {
        // Verify DatabaseClient.getInstance was called
        verify { DatabaseClient.getInstance(any()) }

        // Verify connect() was called
        coVerify { mockClient.connect() }
    }

    @Test
    fun `getType returns correct MIME type for users collection`() {
        val uri = DatabaseContentProvider.USERS_URI

        val type = provider.getType(uri)

        assertEquals("vnd.android.cursor.dir/vnd.${DatabaseContentProvider.AUTHORITY}.users", type)
    }

    @Test
    fun `getType returns correct MIME type for user item`() {
        val uri = DatabaseContentProvider.userUri(1)

        val type = provider.getType(uri)

        assertEquals("vnd.android.cursor.item/vnd.${DatabaseContentProvider.AUTHORITY}.user", type)
    }

    @Test
    fun `getType returns correct MIME type for commands collection`() {
        val uri = DatabaseContentProvider.COMMANDS_URI

        val type = provider.getType(uri)

        assertEquals("vnd.android.cursor.dir/vnd.${DatabaseContentProvider.AUTHORITY}.commands", type)
    }

    @Test
    fun `getType returns correct MIME type for command item`() {
        val uri = DatabaseContentProvider.commandUri(5)

        val type = provider.getType(uri)

        assertEquals("vnd.android.cursor.item/vnd.${DatabaseContentProvider.AUTHORITY}.command", type)
    }

    @Test
    fun `getType returns correct MIME type for settings`() {
        val uri = DatabaseContentProvider.SETTINGS_URI

        val type = provider.getType(uri)

        assertEquals("vnd.android.cursor.item/vnd.${DatabaseContentProvider.AUTHORITY}.settings", type)
    }

    @Test
    fun `getType returns null for invalid URI`() {
        val invalidUri = Uri.parse("content://${DatabaseContentProvider.AUTHORITY}/invalid")

        val type = provider.getType(invalidUri)

        assertNull(type)
    }

    // ===== Query Operations Tests =====

    @Test
    fun `query users returns all users`() {
        // Setup mock data
        val users = listOf(
            User(1, "Alice", "alice@example.com", 1000L, 2000L),
            User(2, "Bob", "bob@example.com", 3000L, null)
        )
        coEvery { mockClient.getAllUsers() } returns users

        val cursor = provider.query(
            DatabaseContentProvider.USERS_URI,
            null, null, null, null
        )

        assertNotNull(cursor)
        assertEquals(2, cursor.count)

        cursor.moveToFirst()
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("id")))
        assertEquals("Alice", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals("alice@example.com", cursor.getString(cursor.getColumnIndex("email")))
        assertEquals(1000L, cursor.getLong(cursor.getColumnIndex("createdAt")))
        assertEquals(2000L, cursor.getLong(cursor.getColumnIndex("lastLoginAt")))

        cursor.moveToNext()
        assertEquals(2, cursor.getInt(cursor.getColumnIndex("id")))
        assertEquals("Bob", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals(0L, cursor.getLong(cursor.getColumnIndex("lastLoginAt"))) // null -> 0L

        cursor.close()
    }

    @Test
    fun `query user by ID returns specific user`() {
        val user = User(42, "John", "john@example.com", 5000L, 6000L)
        coEvery { mockClient.getUserById(42) } returns user

        val uri = DatabaseContentProvider.userUri(42)
        val cursor = provider.query(uri, null, null, null, null)

        assertNotNull(cursor)
        assertEquals(1, cursor.count)

        cursor.moveToFirst()
        assertEquals(42, cursor.getInt(cursor.getColumnIndex("id")))
        assertEquals("John", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals("john@example.com", cursor.getString(cursor.getColumnIndex("email")))

        cursor.close()
    }

    @Test
    fun `query user by ID returns empty cursor when user not found`() {
        coEvery { mockClient.getUserById(999) } returns null

        val uri = DatabaseContentProvider.userUri(999)
        val cursor = provider.query(uri, null, null, null, null)

        assertNotNull(cursor)
        assertEquals(0, cursor.count)

        cursor.close()
    }

    @Test
    fun `query commands returns all commands`() {
        val commands = listOf(
            VoiceCommand(1, "open email", "OPEN_EMAIL", "navigation", true, 10),
            VoiceCommand(2, "close app", "CLOSE_APP", "system", false, 5)
        )
        coEvery { mockClient.getAllVoiceCommands() } returns commands

        val cursor = provider.query(
            DatabaseContentProvider.COMMANDS_URI,
            null, null, null, null
        )

        assertNotNull(cursor)
        assertEquals(2, cursor.count)

        cursor.moveToFirst()
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("id")))
        assertEquals("open email", cursor.getString(cursor.getColumnIndex("command")))
        assertEquals("OPEN_EMAIL", cursor.getString(cursor.getColumnIndex("action")))
        assertEquals("navigation", cursor.getString(cursor.getColumnIndex("category")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("enabled"))) // true -> 1
        assertEquals(10, cursor.getInt(cursor.getColumnIndex("usageCount")))

        cursor.moveToNext()
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("enabled"))) // false -> 0

        cursor.close()
    }

    @Test
    fun `query commands with category filter returns filtered results`() {
        val commands = listOf(
            VoiceCommand(1, "navigate home", "NAV_HOME", "navigation", true, 5),
            VoiceCommand(2, "navigate back", "NAV_BACK", "navigation", true, 3)
        )
        coEvery { mockClient.getVoiceCommandsByCategory("navigation") } returns commands

        val cursor = provider.query(
            DatabaseContentProvider.COMMANDS_URI,
            null,
            "category='navigation'",
            null,
            null
        )

        assertNotNull(cursor)
        assertEquals(2, cursor.count)

        cursor.moveToFirst()
        assertEquals("navigation", cursor.getString(cursor.getColumnIndex("category")))
        cursor.moveToNext()
        assertEquals("navigation", cursor.getString(cursor.getColumnIndex("category")))

        cursor.close()

        coVerify { mockClient.getVoiceCommandsByCategory("navigation") }
    }

    @Test
    fun `query settings returns settings`() {
        val settings = AppSettings(1, voiceEnabled = true, theme = "dark", language = "en", notificationsEnabled = false)
        coEvery { mockClient.getSettings() } returns settings

        val cursor = provider.query(
            DatabaseContentProvider.SETTINGS_URI,
            null, null, null, null
        )

        assertNotNull(cursor)
        assertEquals(1, cursor.count)

        cursor.moveToFirst()
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("id")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("voiceEnabled"))) // true -> 1
        assertEquals("dark", cursor.getString(cursor.getColumnIndex("theme")))
        assertEquals("en", cursor.getString(cursor.getColumnIndex("language")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("notificationsEnabled"))) // false -> 0

        cursor.close()
    }

    @Test
    fun `query returns null for invalid URI`() {
        val invalidUri = Uri.parse("content://${DatabaseContentProvider.AUTHORITY}/invalid")

        val cursor = provider.query(invalidUri, null, null, null, null)

        assertNull(cursor)
    }

    @Test
    fun `query command by ID returns specific command`() {
        val command = VoiceCommand(7, "test command", "TEST_ACTION", "test", true, 0)
        coEvery { mockClient.getVoiceCommandById(7) } returns command

        val uri = DatabaseContentProvider.commandUri(7)
        val cursor = provider.query(uri, null, null, null, null)

        assertNotNull(cursor)
        assertEquals(1, cursor.count)

        cursor.moveToFirst()
        assertEquals(7, cursor.getInt(cursor.getColumnIndex("id")))
        assertEquals("test command", cursor.getString(cursor.getColumnIndex("command")))

        cursor.close()
    }

    // ===== Insert Operations Tests =====

    @Test
    fun `insert user adds user and returns URI`() {
        val values = ContentValues().apply {
            put("name", "New User")
            put("email", "new@example.com")
            put("createdAt", 7000L)
        }

        coEvery { mockClient.insertUser(any()) } just Runs

        val uri = provider.insert(DatabaseContentProvider.USERS_URI, values)

        assertNotNull(uri)
        assertTrue(uri.toString().startsWith("content://${DatabaseContentProvider.AUTHORITY}/users/"))

        coVerify {
            mockClient.insertUser(match { user ->
                user.name == "New User" &&
                user.email == "new@example.com" &&
                user.createdAt == 7000L
            })
        }
    }

    @Test
    fun `insert command adds command and returns URI`() {
        val values = ContentValues().apply {
            put("command", "test cmd")
            put("action", "TEST_ACTION")
            put("category", "test")
            put("enabled", true)
            put("usageCount", 0)
        }

        coEvery { mockClient.insertVoiceCommand(any()) } just Runs

        val uri = provider.insert(DatabaseContentProvider.COMMANDS_URI, values)

        assertNotNull(uri)
        assertTrue(uri.toString().startsWith("content://${DatabaseContentProvider.AUTHORITY}/commands/"))

        coVerify {
            mockClient.insertVoiceCommand(match { cmd ->
                cmd.command == "test cmd" &&
                cmd.action == "TEST_ACTION" &&
                cmd.category == "test"
            })
        }
    }

    @Test
    fun `insert settings returns null (not allowed)`() {
        val values = ContentValues().apply {
            put("theme", "light")
        }

        val uri = provider.insert(DatabaseContentProvider.SETTINGS_URI, values)

        assertNull(uri)

        // Verify insertSettings was NOT called
        coVerify(exactly = 0) { mockClient.updateSettings(any()) }
    }

    @Test
    fun `insert returns null for invalid URI`() {
        val invalidUri = Uri.parse("content://${DatabaseContentProvider.AUTHORITY}/invalid")
        val values = ContentValues()

        val uri = provider.insert(invalidUri, values)

        assertNull(uri)
    }

    @Test
    fun `insert returns null when values is null`() {
        val uri = provider.insert(DatabaseContentProvider.USERS_URI, null)

        assertNull(uri)
    }

    // ===== Update Operations Tests =====

    @Test
    fun `update user modifies user and returns count`() {
        val values = ContentValues().apply {
            put("name", "Updated Name")
            put("email", "updated@example.com")
            put("createdAt", 1000L)
            put("lastLoginAt", 2000L)
        }

        coEvery { mockClient.updateUser(any()) } just Runs

        val uri = DatabaseContentProvider.userUri(10)
        val count = provider.update(uri, values, null, null)

        assertEquals(1, count)

        coVerify {
            mockClient.updateUser(match { user ->
                user.id == 10 &&
                user.name == "Updated Name" &&
                user.email == "updated@example.com"
            })
        }
    }

    @Test
    fun `update command modifies command and returns count`() {
        val values = ContentValues().apply {
            put("command", "updated cmd")
            put("action", "UPDATED_ACTION")
            put("category", "updated")
            put("enabled", false)
            put("usageCount", 100)
        }

        coEvery { mockClient.updateVoiceCommand(any()) } just Runs

        val uri = DatabaseContentProvider.commandUri(20)
        val count = provider.update(uri, values, null, null)

        assertEquals(1, count)

        coVerify {
            mockClient.updateVoiceCommand(match { cmd ->
                cmd.id == 20 &&
                cmd.command == "updated cmd" &&
                cmd.enabled == false &&
                cmd.usageCount == 100
            })
        }
    }

    @Test
    fun `update settings modifies settings and returns count`() {
        val values = ContentValues().apply {
            put("id", 1)
            put("voiceEnabled", false)
            put("theme", "light")
            put("language", "es")
            put("notificationsEnabled", true)
        }

        coEvery { mockClient.updateSettings(any()) } just Runs

        val count = provider.update(DatabaseContentProvider.SETTINGS_URI, values, null, null)

        assertEquals(1, count)

        coVerify {
            mockClient.updateSettings(match { settings ->
                settings.id == 1 &&
                settings.voiceEnabled == false &&
                settings.theme == "light" &&
                settings.language == "es"
            })
        }
    }

    @Test
    fun `update returns 0 for invalid URI`() {
        val invalidUri = Uri.parse("content://${DatabaseContentProvider.AUTHORITY}/invalid")
        val values = ContentValues()

        val count = provider.update(invalidUri, values, null, null)

        assertEquals(0, count)
    }

    @Test
    fun `update returns 0 when values is null`() {
        val count = provider.update(DatabaseContentProvider.SETTINGS_URI, null, null, null)

        assertEquals(0, count)
    }

    // ===== Delete Operations Tests =====

    @Test
    fun `delete user removes user and returns count`() {
        coEvery { mockClient.deleteUser(123) } just Runs

        val uri = DatabaseContentProvider.userUri(123)
        val count = provider.delete(uri, null, null)

        assertEquals(1, count)

        coVerify { mockClient.deleteUser(123) }
    }

    @Test
    fun `delete command removes command and returns count`() {
        coEvery { mockClient.deleteVoiceCommand(456) } just Runs

        val uri = DatabaseContentProvider.commandUri(456)
        val count = provider.delete(uri, null, null)

        assertEquals(1, count)

        coVerify { mockClient.deleteVoiceCommand(456) }
    }

    @Test
    fun `delete settings returns 0 (not allowed)`() {
        val count = provider.delete(DatabaseContentProvider.SETTINGS_URI, null, null)

        assertEquals(0, count)

        // Verify delete was NOT called
        coVerify(exactly = 0) { mockClient.deleteUser(any()) }
        coVerify(exactly = 0) { mockClient.deleteVoiceCommand(any()) }
    }

    @Test
    fun `delete returns 0 for invalid URI`() {
        val invalidUri = Uri.parse("content://${DatabaseContentProvider.AUTHORITY}/invalid")

        val count = provider.delete(invalidUri, null, null)

        assertEquals(0, count)
    }

    // ===== Change Notification Tests =====

    @Test
    fun `insert notifies content observers`() {
        val mockResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns mockResolver

        val values = ContentValues().apply {
            put("name", "Test User")
            put("email", "test@example.com")
        }

        coEvery { mockClient.insertUser(any()) } just Runs

        provider.insert(DatabaseContentProvider.USERS_URI, values)

        // Verify notifyChange was called
        verify { mockResolver.notifyChange(DatabaseContentProvider.USERS_URI, null) }
    }

    @Test
    fun `update notifies content observers`() {
        val mockResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { provider.context?.contentResolver } returns mockResolver

        val values = ContentValues().apply {
            put("name", "Updated User")
            put("email", "updated@example.com")
            put("createdAt", 1000L)
        }

        coEvery { mockClient.updateUser(any()) } just Runs

        val uri = DatabaseContentProvider.userUri(5)
        provider.update(uri, values, null, null)

        // Verify notifyChange was called with specific URI
        verify { mockResolver.notifyChange(uri, null) }
    }

    @Test
    fun `delete notifies content observers`() {
        val mockResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { provider.context?.contentResolver } returns mockResolver

        coEvery { mockClient.deleteUser(10) } just Runs

        val uri = DatabaseContentProvider.userUri(10)
        provider.delete(uri, null, null)

        // Verify notifyChange was called with specific URI
        verify { mockResolver.notifyChange(uri, null) }
    }

    // ===== Error Handling Tests =====

    @Test
    fun `query handles client exception gracefully`() {
        coEvery { mockClient.getAllUsers() } throws RuntimeException("Database error")

        val cursor = provider.query(
            DatabaseContentProvider.USERS_URI,
            null, null, null, null
        )

        // Should return empty cursor instead of throwing
        assertNotNull(cursor)
        assertEquals(0, cursor.count)

        cursor.close()
    }

    @Test
    fun `insert handles client exception gracefully`() {
        val values = ContentValues().apply {
            put("name", "Test")
            put("email", "test@example.com")
        }

        coEvery { mockClient.insertUser(any()) } throws RuntimeException("Insert error")

        val uri = provider.insert(DatabaseContentProvider.USERS_URI, values)

        // Should return null instead of throwing
        assertNull(uri)
    }

    @Test
    fun `update handles client exception gracefully`() {
        val values = ContentValues().apply {
            put("name", "Updated")
            put("email", "updated@example.com")
            put("createdAt", 1000L)
        }

        coEvery { mockClient.updateUser(any()) } throws RuntimeException("Update error")

        val uri = DatabaseContentProvider.userUri(1)
        val count = provider.update(uri, values, null, null)

        // Should return 0 instead of throwing
        assertEquals(0, count)
    }

    @Test
    fun `delete handles client exception gracefully`() {
        coEvery { mockClient.deleteUser(any()) } throws RuntimeException("Delete error")

        val uri = DatabaseContentProvider.userUri(1)
        val count = provider.delete(uri, null, null)

        // Should return 0 instead of throwing
        assertEquals(0, count)
    }

    // ===== URI Helper Tests =====

    @Test
    fun `userUri creates correct URI`() {
        val uri = DatabaseContentProvider.userUri(42)

        assertEquals("content://${DatabaseContentProvider.AUTHORITY}/users/42", uri.toString())
    }

    @Test
    fun `commandUri creates correct URI`() {
        val uri = DatabaseContentProvider.commandUri(99)

        assertEquals("content://${DatabaseContentProvider.AUTHORITY}/commands/99", uri.toString())
    }

    // ===== Additional Edge Cases =====

    @Test
    fun `query handles empty category filter correctly`() {
        val commands = listOf(
            VoiceCommand(1, "cmd1", "act1", "cat1", true, 0)
        )
        coEvery { mockClient.getAllVoiceCommands() } returns commands

        val cursor = provider.query(
            DatabaseContentProvider.COMMANDS_URI,
            null,
            "",  // Empty selection
            null,
            null
        )

        assertNotNull(cursor)

        // Should call getAllVoiceCommands, not getVoiceCommandsByCategory
        coVerify { mockClient.getAllVoiceCommands() }
        coVerify(exactly = 0) { mockClient.getVoiceCommandsByCategory(any()) }

        cursor.close()
    }

    @Test
    fun `insert user with null lastLoginAt works correctly`() {
        val values = ContentValues().apply {
            put("name", "No Login User")
            put("email", "nologin@example.com")
            put("createdAt", 1000L)
            // lastLoginAt not included (null)
        }

        coEvery { mockClient.insertUser(any()) } just Runs

        val uri = provider.insert(DatabaseContentProvider.USERS_URI, values)

        assertNotNull(uri)

        coVerify {
            mockClient.insertUser(match { user ->
                user.lastLoginAt == null
            })
        }
    }

    @Test
    fun `query settings handles null settings gracefully`() {
        coEvery { mockClient.getSettings() } returns null

        val cursor = provider.query(
            DatabaseContentProvider.SETTINGS_URI,
            null, null, null, null
        )

        // Should return empty cursor
        assertNotNull(cursor)
        assertEquals(0, cursor.count)

        cursor.close()
    }

    @Test
    fun `onCreate handles connection failure gracefully`() {
        // Create new provider with connection failure
        val mockFailingClient = mockk<DatabaseClient>(relaxed = true)
        coEvery { mockFailingClient.connect() } returns false

        mockkObject(DatabaseClient)
        every { DatabaseClient.getInstance(any()) } returns mockFailingClient

        val newProvider = Robolectric.buildContentProvider(DatabaseContentProvider::class.java).create().get()

        // Provider should still be created (onCreate returns true)
        assertNotNull(newProvider)

        coVerify { mockFailingClient.connect() }
    }
}
