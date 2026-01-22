package com.augmentalis.voiceavanue.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.augmentalis.voiceavanue.IDatabase
import com.augmentalis.voiceavanue.models.*
import com.augmentalis.voiceavanue.service.DatabaseService
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Client wrapper for Database IPC access.
 * Provides coroutine-based API and handles connection lifecycle.
 *
 * Usage:
 * ```kotlin
 * val client = DatabaseClient.getInstance(context)
 * client.connect()
 * val users = client.getAllUsers()
 * client.disconnect()
 * ```
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class DatabaseClient private constructor(private val context: Context) {

    private val database = AtomicReference<IDatabase?>(null)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serviceConnection: ServiceConnection? = null

    companion object {
        private const val TAG = "DatabaseClient"

        @Volatile
        private var instance: DatabaseClient? = null

        /**
         * Get singleton instance of DatabaseClient.
         * Thread-safe double-checked locking.
         *
         * @param context Application context
         * @return DatabaseClient singleton instance
         */
        fun getInstance(context: Context): DatabaseClient {
            return instance ?: synchronized(this) {
                instance ?: DatabaseClient(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // ===== Connection Management =====

    /**
     * Connect to the database service.
     * Must be called before any database operations.
     *
     * @return true if connected successfully, false otherwise
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.Main) {
        if (database.get() != null) {
            Log.d(TAG, "Already connected to DatabaseService")
            return@withContext true
        }

        val connected = CompletableDeferred<Boolean>()

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d(TAG, "Connected to DatabaseService")
                database.set(IDatabase.Stub.asInterface(service))
                connected.complete(true)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.w(TAG, "Disconnected from DatabaseService")
                database.set(null)
            }
        }

        val intent = Intent(context, DatabaseService::class.java)
        val bound = context.bindService(
            intent,
            serviceConnection!!,
            Context.BIND_AUTO_CREATE
        )

        if (!bound) {
            Log.e(TAG, "Failed to bind to DatabaseService")
            connected.complete(false)
        }

        connected.await()
    }

    /**
     * Disconnect from the database service.
     * Call when done using database to free resources.
     */
    fun disconnect() {
        serviceConnection?.let {
            try {
                context.unbindService(it)
                Log.d(TAG, "Disconnected from DatabaseService")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Service already unbound", e)
            }
        }
        database.set(null)
        serviceConnection = null
    }

    /**
     * Check if connected to service.
     *
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean = database.get() != null

    // ===== User Operations (6 methods) =====

    /**
     * Get all users from database.
     *
     * @return List of all users, empty if none or error
     * @throws IllegalStateException if not connected
     */
    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            requireConnected().getAllUsers() ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "getAllUsers failed", e)
            handleRemoteException(e)
            emptyList()
        }
    }

    /**
     * Get user by ID.
     *
     * @param userId User's unique identifier
     * @return User if found, null otherwise
     * @throws IllegalStateException if not connected
     */
    suspend fun getUserById(userId: Int): User? = withContext(Dispatchers.IO) {
        try {
            requireConnected().getUserById(userId)
        } catch (e: RemoteException) {
            Log.e(TAG, "getUserById failed: $userId", e)
            handleRemoteException(e)
            null
        }
    }

    /**
     * Insert new user into database.
     *
     * @param user User to insert
     * @throws IllegalStateException if not connected
     */
    suspend fun insertUser(user: User) = withContext(Dispatchers.IO) {
        try {
            requireConnected().insertUser(user)
        } catch (e: RemoteException) {
            Log.e(TAG, "insertUser failed", e)
            handleRemoteException(e)
        }
    }

    /**
     * Update existing user.
     *
     * @param user User with updated data
     * @throws IllegalStateException if not connected
     */
    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        try {
            requireConnected().updateUser(user)
        } catch (e: RemoteException) {
            Log.e(TAG, "updateUser failed", e)
            handleRemoteException(e)
        }
    }

    /**
     * Delete user by ID.
     *
     * @param userId User's unique identifier
     * @throws IllegalStateException if not connected
     */
    suspend fun deleteUser(userId: Int) = withContext(Dispatchers.IO) {
        try {
            requireConnected().deleteUser(userId)
        } catch (e: RemoteException) {
            Log.e(TAG, "deleteUser failed: $userId", e)
            handleRemoteException(e)
        }
    }

    /**
     * Get total count of users.
     *
     * @return Number of users in database, 0 on error
     * @throws IllegalStateException if not connected
     */
    suspend fun getUserCount(): Int = withContext(Dispatchers.IO) {
        try {
            requireConnected().getUserCount()
        } catch (e: RemoteException) {
            Log.e(TAG, "getUserCount failed", e)
            handleRemoteException(e)
            0
        }
    }

    // ===== Voice Command Operations (6 methods) =====

    /**
     * Get all voice commands.
     *
     * @return List of all commands, empty if none or error
     * @throws IllegalStateException if not connected
     */
    suspend fun getAllVoiceCommands(): List<VoiceCommand> = withContext(Dispatchers.IO) {
        try {
            requireConnected().getAllVoiceCommands() ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "getAllVoiceCommands failed", e)
            handleRemoteException(e)
            emptyList()
        }
    }

    /**
     * Get voice command by ID.
     *
     * @param commandId Command's unique identifier
     * @return VoiceCommand if found, null otherwise
     * @throws IllegalStateException if not connected
     */
    suspend fun getVoiceCommandById(commandId: Int): VoiceCommand? = withContext(Dispatchers.IO) {
        try {
            requireConnected().getVoiceCommandById(commandId)
        } catch (e: RemoteException) {
            Log.e(TAG, "getVoiceCommandById failed: $commandId", e)
            handleRemoteException(e)
            null
        }
    }

    /**
     * Get voice commands by category.
     *
     * @param category Category to filter by
     * @return List of commands in category, empty if none or error
     * @throws IllegalStateException if not connected
     */
    suspend fun getVoiceCommandsByCategory(category: String): List<VoiceCommand> = withContext(Dispatchers.IO) {
        try {
            requireConnected().getVoiceCommandsByCategory(category) ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "getVoiceCommandsByCategory failed: $category", e)
            handleRemoteException(e)
            emptyList()
        }
    }

    /**
     * Insert new voice command.
     *
     * @param command Voice command to insert
     * @throws IllegalStateException if not connected
     */
    suspend fun insertVoiceCommand(command: VoiceCommand) = withContext(Dispatchers.IO) {
        try {
            requireConnected().insertVoiceCommand(command)
        } catch (e: RemoteException) {
            Log.e(TAG, "insertVoiceCommand failed", e)
            handleRemoteException(e)
        }
    }

    /**
     * Update existing voice command.
     *
     * @param command Voice command with updated data
     * @throws IllegalStateException if not connected
     */
    suspend fun updateVoiceCommand(command: VoiceCommand) = withContext(Dispatchers.IO) {
        try {
            requireConnected().updateVoiceCommand(command)
        } catch (e: RemoteException) {
            Log.e(TAG, "updateVoiceCommand failed", e)
            handleRemoteException(e)
        }
    }

    /**
     * Delete voice command by ID.
     *
     * @param commandId Command's unique identifier
     * @throws IllegalStateException if not connected
     */
    suspend fun deleteVoiceCommand(commandId: Int) = withContext(Dispatchers.IO) {
        try {
            requireConnected().deleteVoiceCommand(commandId)
        } catch (e: RemoteException) {
            Log.e(TAG, "deleteVoiceCommand failed: $commandId", e)
            handleRemoteException(e)
        }
    }

    // ===== Settings Operations (4 methods) =====

    /**
     * Get application settings.
     *
     * @return AppSettings if exists, null otherwise
     * @throws IllegalStateException if not connected
     */
    suspend fun getSettings(): AppSettings? = withContext(Dispatchers.IO) {
        try {
            requireConnected().getSettings()
        } catch (e: RemoteException) {
            Log.e(TAG, "getSettings failed", e)
            handleRemoteException(e)
            null
        }
    }

    /**
     * Update application settings.
     *
     * @param settings Settings with updated values
     * @throws IllegalStateException if not connected
     */
    suspend fun updateSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        try {
            requireConnected().updateSettings(settings)
        } catch (e: RemoteException) {
            Log.e(TAG, "updateSettings failed", e)
            handleRemoteException(e)
        }
    }

    /**
     * Get specific setting value by key.
     *
     * @param key Setting key
     * @return Setting value, null if not found or error
     * @throws IllegalStateException if not connected
     */
    suspend fun getSettingValue(key: String): String? = withContext(Dispatchers.IO) {
        try {
            requireConnected().getSettingValue(key)
        } catch (e: RemoteException) {
            Log.e(TAG, "getSettingValue failed: $key", e)
            handleRemoteException(e)
            null
        }
    }

    /**
     * Set specific setting value.
     *
     * @param key Setting key
     * @param value Setting value
     * @throws IllegalStateException if not connected
     */
    suspend fun setSettingValue(key: String, value: String) = withContext(Dispatchers.IO) {
        try {
            requireConnected().setSettingValue(key, value)
        } catch (e: RemoteException) {
            Log.e(TAG, "setSettingValue failed: $key", e)
            handleRemoteException(e)
        }
    }

    // ===== Maintenance Operations (4 methods) =====

    /**
     * Clear all data from database.
     * WARNING: This is destructive and cannot be undone.
     *
     * @throws IllegalStateException if not connected
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            requireConnected().clearAllData()
        } catch (e: RemoteException) {
            Log.e(TAG, "clearAllData failed", e)
            handleRemoteException(e)
        }
    }

    /**
     * Get database file size in bytes.
     *
     * @return Database size in bytes, 0 on error
     * @throws IllegalStateException if not connected
     */
    suspend fun getDatabaseSize(): Long = withContext(Dispatchers.IO) {
        try {
            requireConnected().getDatabaseSize()
        } catch (e: RemoteException) {
            Log.e(TAG, "getDatabaseSize failed", e)
            handleRemoteException(e)
            0L
        }
    }

    /**
     * Vacuum database to reclaim space.
     *
     * @throws IllegalStateException if not connected
     */
    suspend fun vacuum() = withContext(Dispatchers.IO) {
        try {
            requireConnected().vacuum()
        } catch (e: RemoteException) {
            Log.e(TAG, "vacuum failed", e)
            handleRemoteException(e)
        }
    }

    /**
     * Get database schema version.
     *
     * @return Version string (e.g., "1.0.0"), null on error
     * @throws IllegalStateException if not connected
     */
    suspend fun getDatabaseVersion(): String? = withContext(Dispatchers.IO) {
        try {
            requireConnected().getDatabaseVersion()
        } catch (e: RemoteException) {
            Log.e(TAG, "getDatabaseVersion failed", e)
            handleRemoteException(e)
            null
        }
    }

    // ===== Health & Utility =====

    /**
     * Check if database service is healthy.
     *
     * @return true if healthy, false if unhealthy or disconnected
     */
    suspend fun isHealthy(): Boolean = withContext(Dispatchers.IO) {
        try {
            database.get()?.isHealthy() ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "Health check failed", e)
            false
        }
    }

    /**
     * Get timestamp of last access.
     *
     * @return Unix timestamp in milliseconds, 0 on error
     */
    suspend fun getLastAccessTime(): Long = withContext(Dispatchers.IO) {
        try {
            requireConnected().getLastAccessTime()
        } catch (e: RemoteException) {
            Log.e(TAG, "getLastAccessTime failed", e)
            0L
        }
    }

    // ===== Helper Methods =====

    private fun requireConnected(): IDatabase {
        return database.get() ?: throw IllegalStateException(
            "DatabaseClient not connected. Call connect() first."
        )
    }

    private suspend fun handleRemoteException(e: RemoteException) {
        Log.w(TAG, "Service crashed or disconnected, attempting reconnection...")
        disconnect()
        delay(1000) // Wait 1 second before reconnecting
        connect()
    }
}
