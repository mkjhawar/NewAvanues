package com.augmentalis.voiceavanue.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.augmentalis.voiceavanue.IDatabase
import com.augmentalis.voiceavanue.models.*
import com.augmentalis.voiceos.database.Database
import com.augmentalis.voiceos.database.DatabaseFactory
import com.augmentalis.voiceos.database.Document
import com.augmentalis.voiceos.database.Query
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

/**
 * Database service running in separate process (:database).
 * Implements AIDL interface for cross-process database operations.
 *
 * This service provides IPC access to the database layer, enabling:
 * - Process isolation (crashes don't affect main app)
 * - Memory optimization (can be killed when idle)
 * - Cross-app data sharing (via ContentProvider)
 *
 * Database Structure:
 * - Collections: "users", "voice_commands", "settings"
 * - Each collection stores documents with string-based fields
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class DatabaseService : Service() {

    private lateinit var database: Database

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lastAccessTime = AtomicLong(System.currentTimeMillis())

    companion object {
        private const val TAG = "DatabaseService"
        private const val IDLE_TIMEOUT = 5 * 60 * 1000L // 5 minutes
        const val DATABASE_VERSION = "1.0.0"

        // Collection names
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_COMMANDS = "voice_commands"
        private const val COLLECTION_SETTINGS = "settings"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DatabaseService created in process: ${android.os.Process.myPid()}")

        // Initialize DatabaseFactory with Android context
        DatabaseFactory.initialize(applicationContext)

        // Initialize database
        database = DatabaseFactory.create("voiceavanue_db", version = 1)
        database.open()

        // Create collections if they don't exist
        ensureCollectionsExist()

        startIdleMonitor()
    }

    private fun ensureCollectionsExist() {
        val collections = database.listCollections()

        if (!collections.contains(COLLECTION_USERS)) {
            database.createCollection(COLLECTION_USERS)
            Log.d(TAG, "Created '$COLLECTION_USERS' collection")
        }

        if (!collections.contains(COLLECTION_COMMANDS)) {
            database.createCollection(COLLECTION_COMMANDS)
            Log.d(TAG, "Created '$COLLECTION_COMMANDS' collection")
        }

        if (!collections.contains(COLLECTION_SETTINGS)) {
            database.createCollection(COLLECTION_SETTINGS)
            Log.d(TAG, "Created '$COLLECTION_SETTINGS' collection")
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Client binding to DatabaseService")
        return binder
    }

    // ===== AIDL Implementation =====

    private val binder = object : IDatabase.Stub() {

        // ===== User Operations (6 methods) =====

        override fun getAllUsers(): List<User> {
            updateAccessTime()
            return try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_USERS)
                    if (collection == null) {
                        Log.w(TAG, "Users collection not found")
                        return@runBlocking emptyList()
                    }

                    val documents = collection.find(Query.all())
                    documents.mapNotNull { doc -> documentToUser(doc) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAllUsers failed", e)
                emptyList()
            }
        }

        override fun getUserById(userId: Int): User? {
            updateAccessTime()
            return try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_USERS)
                    if (collection == null) {
                        Log.w(TAG, "Users collection not found")
                        return@runBlocking null
                    }

                    val doc = collection.findById(userId.toString())
                    doc?.let { documentToUser(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getUserById failed: $userId", e)
                null
            }
        }

        override fun insertUser(user: User) {
            updateAccessTime()
            try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_USERS)
                    if (collection == null) {
                        Log.w(TAG, "Users collection not found")
                        return@runBlocking
                    }

                    val document = userToDocument(user)
                    collection.insert(document)
                    Log.d(TAG, "insertUser: ${user.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "insertUser failed", e)
            }
        }

        override fun updateUser(user: User) {
            updateAccessTime()
            try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_USERS)
                    if (collection == null) {
                        Log.w(TAG, "Users collection not found")
                        return@runBlocking
                    }

                    val updates = mapOf(
                        "name" to user.name,
                        "email" to user.email,
                        "createdAt" to user.createdAt.toString(),
                        "lastLoginAt" to (user.lastLoginAt?.toString() ?: "")
                    )
                    collection.updateById(user.id.toString(), updates)
                    Log.d(TAG, "updateUser: ${user.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateUser failed", e)
            }
        }

        override fun deleteUser(userId: Int) {
            updateAccessTime()
            try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_USERS)
                    if (collection == null) {
                        Log.w(TAG, "Users collection not found")
                        return@runBlocking
                    }

                    collection.deleteById(userId.toString())
                    Log.d(TAG, "deleteUser: $userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteUser failed", e)
            }
        }

        override fun getUserCount(): Int {
            updateAccessTime()
            return try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_USERS)
                    if (collection == null) {
                        Log.w(TAG, "Users collection not found")
                        return@runBlocking 0
                    }

                    collection.count()
                }
            } catch (e: Exception) {
                Log.e(TAG, "getUserCount failed", e)
                0
            }
        }

        // ===== Voice Command Operations (6 methods) =====

        override fun getAllVoiceCommands(): List<VoiceCommand> {
            updateAccessTime()
            return try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_COMMANDS)
                    if (collection == null) {
                        Log.w(TAG, "Voice commands collection not found")
                        return@runBlocking emptyList()
                    }

                    val documents = collection.find(Query.all())
                    documents.mapNotNull { doc -> documentToVoiceCommand(doc) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAllVoiceCommands failed", e)
                emptyList()
            }
        }

        override fun getVoiceCommandById(commandId: Int): VoiceCommand? {
            updateAccessTime()
            return try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_COMMANDS)
                    if (collection == null) {
                        Log.w(TAG, "Voice commands collection not found")
                        return@runBlocking null
                    }

                    val doc = collection.findById(commandId.toString())
                    doc?.let { documentToVoiceCommand(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getVoiceCommandById failed: $commandId", e)
                null
            }
        }

        override fun getVoiceCommandsByCategory(category: String): List<VoiceCommand> {
            updateAccessTime()
            return try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_COMMANDS)
                    if (collection == null) {
                        Log.w(TAG, "Voice commands collection not found")
                        return@runBlocking emptyList()
                    }

                    val documents = collection.find(Query.where("category", category))
                    documents.mapNotNull { doc -> documentToVoiceCommand(doc) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getVoiceCommandsByCategory failed: $category", e)
                emptyList()
            }
        }

        override fun insertVoiceCommand(command: VoiceCommand) {
            updateAccessTime()
            try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_COMMANDS)
                    if (collection == null) {
                        Log.w(TAG, "Voice commands collection not found")
                        return@runBlocking
                    }

                    val document = voiceCommandToDocument(command)
                    collection.insert(document)
                    Log.d(TAG, "insertVoiceCommand: ${command.command}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "insertVoiceCommand failed", e)
            }
        }

        override fun updateVoiceCommand(command: VoiceCommand) {
            updateAccessTime()
            try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_COMMANDS)
                    if (collection == null) {
                        Log.w(TAG, "Voice commands collection not found")
                        return@runBlocking
                    }

                    val updates = mapOf(
                        "command" to command.command,
                        "action" to command.action,
                        "category" to command.category,
                        "enabled" to command.enabled.toString(),
                        "usageCount" to command.usageCount.toString()
                    )
                    collection.updateById(command.id.toString(), updates)
                    Log.d(TAG, "updateVoiceCommand: ${command.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateVoiceCommand failed", e)
            }
        }

        override fun deleteVoiceCommand(commandId: Int) {
            updateAccessTime()
            try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_COMMANDS)
                    if (collection == null) {
                        Log.w(TAG, "Voice commands collection not found")
                        return@runBlocking
                    }

                    collection.deleteById(commandId.toString())
                    Log.d(TAG, "deleteVoiceCommand: $commandId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteVoiceCommand failed", e)
            }
        }

        // ===== Settings Operations (4 methods) =====

        override fun getSettings(): AppSettings? {
            updateAccessTime()
            return try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_SETTINGS)
                    if (collection == null) {
                        Log.w(TAG, "Settings collection not found")
                        return@runBlocking AppSettings.default()
                    }

                    val documents = collection.find(Query.all())
                    val doc = documents.firstOrNull()
                    doc?.let { documentToAppSettings(it) } ?: AppSettings.default()
                }
            } catch (e: Exception) {
                Log.e(TAG, "getSettings failed", e)
                null
            }
        }

        override fun updateSettings(settings: AppSettings) {
            updateAccessTime()
            try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_SETTINGS)
                    if (collection == null) {
                        Log.w(TAG, "Settings collection not found")
                        return@runBlocking
                    }

                    val document = appSettingsToDocument(settings)
                    collection.updateById(settings.id.toString(), document.data)
                    Log.d(TAG, "updateSettings: theme=${settings.theme}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateSettings failed", e)
            }
        }

        override fun getSettingValue(key: String): String? {
            updateAccessTime()
            return try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_SETTINGS)
                    if (collection == null) {
                        Log.w(TAG, "Settings collection not found")
                        return@runBlocking null
                    }

                    val documents = collection.find(Query.all())
                    val doc = documents.firstOrNull()
                    doc?.getString(key)
                }
            } catch (e: Exception) {
                Log.e(TAG, "getSettingValue failed: $key", e)
                null
            }
        }

        override fun setSettingValue(key: String, value: String) {
            updateAccessTime()
            try {
                runBlocking {
                    val collection = database.getCollection(COLLECTION_SETTINGS)
                    if (collection == null) {
                        Log.w(TAG, "Settings collection not found")
                        return@runBlocking
                    }

                    val documents = collection.find(Query.all())
                    val doc = documents.firstOrNull()
                    if (doc != null) {
                        val updates = mapOf(key to value)
                        collection.updateById(doc.id, updates)
                        Log.d(TAG, "setSettingValue: $key=$value")
                    } else {
                        Log.w(TAG, "No settings document found to update")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "setSettingValue failed", e)
            }
        }

        // ===== Maintenance Operations (4 methods) =====

        override fun clearAllData() {
            updateAccessTime()
            try {
                runBlocking {
                    database.dropCollection(COLLECTION_USERS)
                    database.dropCollection(COLLECTION_COMMANDS)
                    database.dropCollection(COLLECTION_SETTINGS)

                    ensureCollectionsExist()
                    Log.w(TAG, "clearAllData: Database cleared")
                }
            } catch (e: Exception) {
                Log.e(TAG, "clearAllData failed", e)
            }
        }

        override fun getDatabaseSize(): Long {
            updateAccessTime()
            return try {
                val dbFile = applicationContext.getDatabasePath("voiceavanue_db")
                dbFile?.length() ?: 0L
            } catch (e: Exception) {
                Log.e(TAG, "getDatabaseSize failed", e)
                0L
            }
        }

        override fun vacuum() {
            updateAccessTime()
            try {
                runBlocking {
                    database.flush()
                    Log.d(TAG, "vacuum: Database flushed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "vacuum failed", e)
            }
        }

        override fun getDatabaseVersion(): String {
            return DATABASE_VERSION
        }

        // ===== Health Check (2 methods) =====

        override fun isHealthy(): Boolean {
            return try {
                val collections = database.listCollections()
                collections.contains(COLLECTION_USERS) &&
                    collections.contains(COLLECTION_COMMANDS) &&
                    collections.contains(COLLECTION_SETTINGS)
            } catch (e: Exception) {
                Log.e(TAG, "Health check failed", e)
                false
            }
        }

        override fun getLastAccessTime(): Long {
            return this@DatabaseService.lastAccessTime.get()
        }
    }

    // ===== Helper Methods =====

    private fun updateAccessTime() {
        lastAccessTime.set(System.currentTimeMillis())
    }

    private fun startIdleMonitor() {
        serviceScope.launch {
            while (isActive) {
                delay(60_000) // Check every minute

                val idleTime = System.currentTimeMillis() - lastAccessTime.get()
                if (idleTime > IDLE_TIMEOUT) {
                    Log.d(TAG, "Service idle for ${idleTime}ms")
                    // Optional: Implement auto-shutdown logic
                    // stopSelf()
                }
            }
        }
    }

    // ===== Document Conversion Helpers =====

    private fun documentToUser(doc: Document): User? {
        return try {
            User(
                id = doc.getInt("id") ?: 0,
                name = doc.getString("name") ?: "",
                email = doc.getString("email") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L,
                lastLoginAt = doc.getLong("lastLoginAt")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert document to User: ${doc.id}", e)
            null
        }
    }

    private fun userToDocument(user: User): Document {
        return Document(
            id = user.id.toString(),
            data = mapOf(
                "id" to user.id.toString(),
                "name" to user.name,
                "email" to user.email,
                "createdAt" to user.createdAt.toString(),
                "lastLoginAt" to (user.lastLoginAt?.toString() ?: "")
            )
        )
    }

    private fun documentToVoiceCommand(doc: Document): VoiceCommand? {
        return try {
            VoiceCommand(
                id = doc.getInt("id") ?: 0,
                command = doc.getString("command") ?: "",
                action = doc.getString("action") ?: "",
                category = doc.getString("category") ?: "",
                enabled = doc.getBoolean("enabled") ?: true,
                usageCount = doc.getInt("usageCount") ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert document to VoiceCommand: ${doc.id}", e)
            null
        }
    }

    private fun voiceCommandToDocument(command: VoiceCommand): Document {
        return Document(
            id = command.id.toString(),
            data = mapOf(
                "id" to command.id.toString(),
                "command" to command.command,
                "action" to command.action,
                "category" to command.category,
                "enabled" to command.enabled.toString(),
                "usageCount" to command.usageCount.toString()
            )
        )
    }

    private fun documentToAppSettings(doc: Document): AppSettings? {
        return try {
            AppSettings(
                id = doc.getInt("id") ?: 1,
                voiceEnabled = doc.getBoolean("voiceEnabled") ?: true,
                theme = doc.getString("theme") ?: "system",
                language = doc.getString("language") ?: "en",
                notificationsEnabled = doc.getBoolean("notificationsEnabled") ?: true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert document to AppSettings: ${doc.id}", e)
            null
        }
    }

    private fun appSettingsToDocument(settings: AppSettings): Document {
        return Document(
            id = settings.id.toString(),
            data = mapOf(
                "id" to settings.id.toString(),
                "voiceEnabled" to settings.voiceEnabled.toString(),
                "theme" to settings.theme,
                "language" to settings.language,
                "notificationsEnabled" to settings.notificationsEnabled.toString()
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DatabaseService destroyed")
        serviceScope.cancel()

        // Close database connection
        database.close()
    }
}
