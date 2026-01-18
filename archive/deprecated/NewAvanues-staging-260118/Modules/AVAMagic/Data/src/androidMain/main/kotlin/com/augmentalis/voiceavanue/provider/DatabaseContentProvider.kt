package com.augmentalis.voiceavanue.provider

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.augmentalis.voiceavanue.client.DatabaseClient
import com.augmentalis.voiceavanue.models.*
import kotlinx.coroutines.runBlocking

/**
 * ContentProvider bridge for cross-app Database access.
 * Uses DatabaseClient internally for IPC communication.
 *
 * URI Structure:
 * - content://com.augmentalis.voiceavanue.database/users
 * - content://com.augmentalis.voiceavanue.database/users/{id}
 * - content://com.augmentalis.voiceavanue.database/commands
 * - content://com.augmentalis.voiceavanue.database/commands/{id}
 * - content://com.augmentalis.voiceavanue.database/settings
 *
 * Example from AVA AI:
 * ```kotlin
 * val cursor = contentResolver.query(
 *     Uri.parse("content://com.augmentalis.voiceavanue.database/users"),
 *     null, null, null, null
 * )
 * ```
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class DatabaseContentProvider : ContentProvider() {

    private lateinit var client: DatabaseClient

    companion object {
        private const val TAG = "DatabaseContentProvider"

        // Authority (must match manifest)
        const val AUTHORITY = "com.augmentalis.voiceavanue.database"

        // URI codes
        private const val USERS = 1
        private const val USER_ID = 2
        private const val COMMANDS = 3
        private const val COMMAND_ID = 4
        private const val SETTINGS = 5

        // URI matcher
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "users", USERS)
            addURI(AUTHORITY, "users/#", USER_ID)
            addURI(AUTHORITY, "commands", COMMANDS)
            addURI(AUTHORITY, "commands/#", COMMAND_ID)
            addURI(AUTHORITY, "settings", SETTINGS)
        }

        // Content URIs (for external apps)
        val USERS_URI: Uri = Uri.parse("content://$AUTHORITY/users")
        val COMMANDS_URI: Uri = Uri.parse("content://$AUTHORITY/commands")
        val SETTINGS_URI: Uri = Uri.parse("content://$AUTHORITY/settings")

        /**
         * Create URI for specific user.
         */
        fun userUri(id: Int): Uri {
            return ContentUris.withAppendedId(USERS_URI, id.toLong())
        }

        /**
         * Create URI for specific command.
         */
        fun commandUri(id: Int): Uri {
            return ContentUris.withAppendedId(COMMANDS_URI, id.toLong())
        }
    }

    override fun onCreate(): Boolean {
        Log.d(TAG, "DatabaseContentProvider created")
        client = DatabaseClient.getInstance(context!!)

        // Connect to service
        runBlocking {
            val connected = client.connect()
            if (!connected) {
                Log.e(TAG, "Failed to connect to DatabaseService")
            }
        }

        return true
    }

    // ===== Query =====

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "Query: $uri")

        return when (uriMatcher.match(uri)) {
            USERS -> queryAllUsers()
            USER_ID -> {
                val id = uri.lastPathSegment?.toIntOrNull()
                    ?: return null
                queryUserById(id)
            }
            COMMANDS -> queryAllCommands(selection)
            COMMAND_ID -> {
                val id = uri.lastPathSegment?.toIntOrNull()
                    ?: return null
                queryCommandById(id)
            }
            SETTINGS -> querySettings()
            else -> {
                Log.w(TAG, "Unknown URI: $uri")
                null
            }
        }
    }

    private fun queryAllUsers(): Cursor {
        val cursor = MatrixCursor(arrayOf(
            "id",
            "name",
            "email",
            "createdAt",
            "lastLoginAt"
        ))

        runBlocking {
            try {
                val users = client.getAllUsers()
                users.forEach { user ->
                    cursor.addRow(arrayOf(
                        user.id,
                        user.name,
                        user.email,
                        user.createdAt,
                        user.lastLoginAt ?: 0L
                    ))
                }
                Log.d(TAG, "queryAllUsers: returned ${users.size} users")
            } catch (e: Exception) {
                Log.e(TAG, "queryAllUsers failed", e)
            }
        }

        return cursor
    }

    private fun queryUserById(id: Int): Cursor {
        val cursor = MatrixCursor(arrayOf(
            "id", "name", "email", "createdAt", "lastLoginAt"
        ))

        runBlocking {
            try {
                val user = client.getUserById(id)
                user?.let {
                    cursor.addRow(arrayOf(
                        it.id, it.name, it.email, it.createdAt, it.lastLoginAt ?: 0L
                    ))
                    Log.d(TAG, "queryUserById: found user $id")
                }
            } catch (e: Exception) {
                Log.e(TAG, "queryUserById failed: $id", e)
            }
        }

        return cursor
    }

    private fun queryAllCommands(selection: String?): Cursor {
        val cursor = MatrixCursor(arrayOf(
            "id",
            "command",
            "action",
            "category",
            "enabled",
            "usageCount"
        ))

        runBlocking {
            try {
                // If selection contains category, filter by category
                val commands = if (selection?.contains("category") == true) {
                    val category = selection.substringAfter("=").trim(' ', '\'', '"')
                    client.getVoiceCommandsByCategory(category)
                } else {
                    client.getAllVoiceCommands()
                }

                commands.forEach { cmd ->
                    cursor.addRow(arrayOf(
                        cmd.id,
                        cmd.command,
                        cmd.action,
                        cmd.category,
                        if (cmd.enabled) 1 else 0,
                        cmd.usageCount
                    ))
                }
                Log.d(TAG, "queryAllCommands: returned ${commands.size} commands")
            } catch (e: Exception) {
                Log.e(TAG, "queryAllCommands failed", e)
            }
        }

        return cursor
    }

    private fun queryCommandById(id: Int): Cursor {
        val cursor = MatrixCursor(arrayOf(
            "id", "command", "action", "category", "enabled", "usageCount"
        ))

        runBlocking {
            try {
                val cmd = client.getVoiceCommandById(id)
                cmd?.let {
                    cursor.addRow(arrayOf(
                        it.id, it.command, it.action, it.category,
                        if (it.enabled) 1 else 0, it.usageCount
                    ))
                    Log.d(TAG, "queryCommandById: found command $id")
                }
            } catch (e: Exception) {
                Log.e(TAG, "queryCommandById failed: $id", e)
            }
        }

        return cursor
    }

    private fun querySettings(): Cursor {
        val cursor = MatrixCursor(arrayOf(
            "id",
            "voiceEnabled",
            "theme",
            "language",
            "notificationsEnabled"
        ))

        runBlocking {
            try {
                val settings = client.getSettings()
                settings?.let {
                    cursor.addRow(arrayOf(
                        it.id,
                        if (it.voiceEnabled) 1 else 0,
                        it.theme,
                        it.language,
                        if (it.notificationsEnabled) 1 else 0
                    ))
                    Log.d(TAG, "querySettings: returned settings")
                }
            } catch (e: Exception) {
                Log.e(TAG, "querySettings failed", e)
            }
        }

        return cursor
    }

    // ===== Insert =====

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.d(TAG, "Insert: $uri")

        if (values == null) {
            Log.w(TAG, "Insert: values is null")
            return null
        }

        return when (uriMatcher.match(uri)) {
            USERS -> insertUser(values)
            COMMANDS -> insertCommand(values)
            SETTINGS -> {
                Log.w(TAG, "Insert not allowed for settings (use update)")
                null
            }
            else -> {
                Log.w(TAG, "Insert: Unknown URI: $uri")
                null
            }
        }
    }

    private fun insertUser(values: ContentValues): Uri? {
        val user = User(
            id = 0, // Auto-generated
            name = values.getAsString("name") ?: "",
            email = values.getAsString("email") ?: "",
            createdAt = values.getAsLong("createdAt") ?: System.currentTimeMillis(),
            lastLoginAt = values.getAsLong("lastLoginAt")
        )

        return runBlocking {
            try {
                client.insertUser(user)
                context?.contentResolver?.notifyChange(USERS_URI, null)
                Log.d(TAG, "insertUser: inserted ${user.name}")
                userUri(user.id)
            } catch (e: Exception) {
                Log.e(TAG, "insertUser failed", e)
                null
            }
        }
    }

    private fun insertCommand(values: ContentValues): Uri? {
        val command = VoiceCommand(
            id = 0, // Auto-generated
            command = values.getAsString("command") ?: "",
            action = values.getAsString("action") ?: "",
            category = values.getAsString("category") ?: "",
            enabled = values.getAsBoolean("enabled") ?: true,
            usageCount = values.getAsInteger("usageCount") ?: 0
        )

        return runBlocking {
            try {
                client.insertVoiceCommand(command)
                context?.contentResolver?.notifyChange(COMMANDS_URI, null)
                Log.d(TAG, "insertCommand: inserted ${command.command}")
                commandUri(command.id)
            } catch (e: Exception) {
                Log.e(TAG, "insertCommand failed", e)
                null
            }
        }
    }

    // ===== Update =====

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        Log.d(TAG, "Update: $uri")

        if (values == null) {
            Log.w(TAG, "Update: values is null")
            return 0
        }

        return when (uriMatcher.match(uri)) {
            USER_ID -> updateUser(uri, values)
            COMMAND_ID -> updateCommand(uri, values)
            SETTINGS -> updateSettings(values)
            else -> {
                Log.w(TAG, "Update: Unknown URI: $uri")
                0
            }
        }
    }

    private fun updateUser(uri: Uri, values: ContentValues): Int {
        val id = uri.lastPathSegment?.toIntOrNull() ?: return 0

        val user = User(
            id = id,
            name = values.getAsString("name") ?: "",
            email = values.getAsString("email") ?: "",
            createdAt = values.getAsLong("createdAt") ?: 0L,
            lastLoginAt = values.getAsLong("lastLoginAt")
        )

        return runBlocking {
            try {
                client.updateUser(user)
                context?.contentResolver?.notifyChange(uri, null)
                Log.d(TAG, "updateUser: updated user $id")
                1
            } catch (e: Exception) {
                Log.e(TAG, "updateUser failed: $id", e)
                0
            }
        }
    }

    private fun updateCommand(uri: Uri, values: ContentValues): Int {
        val id = uri.lastPathSegment?.toIntOrNull() ?: return 0

        val command = VoiceCommand(
            id = id,
            command = values.getAsString("command") ?: "",
            action = values.getAsString("action") ?: "",
            category = values.getAsString("category") ?: "",
            enabled = values.getAsBoolean("enabled") ?: true,
            usageCount = values.getAsInteger("usageCount") ?: 0
        )

        return runBlocking {
            try {
                client.updateVoiceCommand(command)
                context?.contentResolver?.notifyChange(uri, null)
                Log.d(TAG, "updateCommand: updated command $id")
                1
            } catch (e: Exception) {
                Log.e(TAG, "updateCommand failed: $id", e)
                0
            }
        }
    }

    private fun updateSettings(values: ContentValues): Int {
        val settings = AppSettings(
            id = values.getAsInteger("id") ?: 1,
            voiceEnabled = values.getAsBoolean("voiceEnabled") ?: true,
            theme = values.getAsString("theme") ?: "system",
            language = values.getAsString("language") ?: "en",
            notificationsEnabled = values.getAsBoolean("notificationsEnabled") ?: true
        )

        return runBlocking {
            try {
                client.updateSettings(settings)
                context?.contentResolver?.notifyChange(SETTINGS_URI, null)
                Log.d(TAG, "updateSettings: updated settings")
                1
            } catch (e: Exception) {
                Log.e(TAG, "updateSettings failed", e)
                0
            }
        }
    }

    // ===== Delete =====

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        Log.d(TAG, "Delete: $uri")

        return when (uriMatcher.match(uri)) {
            USER_ID -> deleteUser(uri)
            COMMAND_ID -> deleteCommand(uri)
            SETTINGS -> {
                Log.w(TAG, "Delete not allowed for settings")
                0
            }
            else -> {
                Log.w(TAG, "Delete: Unknown URI: $uri")
                0
            }
        }
    }

    private fun deleteUser(uri: Uri): Int {
        val id = uri.lastPathSegment?.toIntOrNull() ?: return 0

        return runBlocking {
            try {
                client.deleteUser(id)
                context?.contentResolver?.notifyChange(uri, null)
                Log.d(TAG, "deleteUser: deleted user $id")
                1
            } catch (e: Exception) {
                Log.e(TAG, "deleteUser failed: $id", e)
                0
            }
        }
    }

    private fun deleteCommand(uri: Uri): Int {
        val id = uri.lastPathSegment?.toIntOrNull() ?: return 0

        return runBlocking {
            try {
                client.deleteVoiceCommand(id)
                context?.contentResolver?.notifyChange(uri, null)
                Log.d(TAG, "deleteCommand: deleted command $id")
                1
            } catch (e: Exception) {
                Log.e(TAG, "deleteCommand failed: $id", e)
                0
            }
        }
    }

    // ===== Required Methods =====

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            USERS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.users"
            USER_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.user"
            COMMANDS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.commands"
            COMMAND_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.command"
            SETTINGS -> "vnd.android.cursor.item/vnd.$AUTHORITY.settings"
            else -> null
        }
    }
}
