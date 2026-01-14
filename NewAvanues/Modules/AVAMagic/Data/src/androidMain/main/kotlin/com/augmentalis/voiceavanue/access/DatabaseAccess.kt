package com.augmentalis.voiceavanue.access

import com.augmentalis.voiceavanue.models.AppSettings
import com.augmentalis.voiceavanue.models.User
import com.augmentalis.voiceavanue.models.VoiceCommand

/**
 * Database access interface abstracting implementation details.
 *
 * This interface allows switching between:
 * - Direct database access (legacy)
 * - IPC-based access via DatabaseClient (new)
 *
 * Usage:
 * ```kotlin
 * val database: DatabaseAccess = DatabaseAccessFactory.create(context)
 * val users = database.getAllUsers()
 * ```
 *
 * Implementation is selected via DatabaseConfig.USE_IPC_DATABASE flag.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
interface DatabaseAccess {

    // ===== Connection Management =====

    /**
     * Connect to database.
     * For IPC: Binds to DatabaseService
     * For direct: Opens database file
     *
     * @return true if connected successfully
     */
    suspend fun connect(): Boolean

    /**
     * Disconnect from database.
     * Releases resources and unbinds services.
     */
    fun disconnect()

    /**
     * Check if connected.
     *
     * @return true if ready for operations
     */
    fun isConnected(): Boolean

    // ===== User Operations =====

    /**
     * Get all users.
     *
     * @return List of all users, empty if none
     */
    suspend fun getAllUsers(): List<User>

    /**
     * Get user by ID.
     *
     * @param userId User's unique identifier
     * @return User if found, null otherwise
     */
    suspend fun getUserById(userId: Int): User?

    /**
     * Insert new user.
     *
     * @param user User to insert
     */
    suspend fun insertUser(user: User)

    /**
     * Update existing user.
     *
     * @param user User with updated data
     */
    suspend fun updateUser(user: User)

    /**
     * Delete user by ID.
     *
     * @param userId User's unique identifier
     */
    suspend fun deleteUser(userId: Int)

    /**
     * Get total user count.
     *
     * @return Number of users in database
     */
    suspend fun getUserCount(): Int

    // ===== Voice Command Operations =====

    /**
     * Get all voice commands.
     *
     * @return List of all commands, empty if none
     */
    suspend fun getAllVoiceCommands(): List<VoiceCommand>

    /**
     * Get voice command by ID.
     *
     * @param commandId Command's unique identifier
     * @return VoiceCommand if found, null otherwise
     */
    suspend fun getVoiceCommandById(commandId: Int): VoiceCommand?

    /**
     * Get voice commands by category.
     *
     * @param category Category to filter by
     * @return List of commands in category, empty if none
     */
    suspend fun getVoiceCommandsByCategory(category: String): List<VoiceCommand>

    /**
     * Insert new voice command.
     *
     * @param command Voice command to insert
     */
    suspend fun insertVoiceCommand(command: VoiceCommand)

    /**
     * Update existing voice command.
     *
     * @param command Voice command with updated data
     */
    suspend fun updateVoiceCommand(command: VoiceCommand)

    /**
     * Delete voice command by ID.
     *
     * @param commandId Command's unique identifier
     */
    suspend fun deleteVoiceCommand(commandId: Int)

    // ===== Settings Operations =====

    /**
     * Get application settings.
     *
     * @return AppSettings if exists, null otherwise
     */
    suspend fun getSettings(): AppSettings?

    /**
     * Update application settings.
     *
     * @param settings Settings with updated values
     */
    suspend fun updateSettings(settings: AppSettings)

    /**
     * Get specific setting value by key.
     *
     * @param key Setting key
     * @return Setting value, null if not found
     */
    suspend fun getSettingValue(key: String): String?

    /**
     * Set specific setting value.
     *
     * @param key Setting key
     * @param value Setting value
     */
    suspend fun setSettingValue(key: String, value: String)

    // ===== Maintenance Operations =====

    /**
     * Clear all data from database.
     * WARNING: Destructive operation.
     */
    suspend fun clearAllData()

    /**
     * Get database file size in bytes.
     *
     * @return Database size in bytes
     */
    suspend fun getDatabaseSize(): Long

    /**
     * Vacuum database to reclaim space.
     */
    suspend fun vacuum()

    /**
     * Get database schema version.
     *
     * @return Version string (e.g., "1.0.0")
     */
    suspend fun getDatabaseVersion(): String?

    // ===== Health & Utility =====

    /**
     * Check if database is healthy.
     *
     * @return true if healthy, false otherwise
     */
    suspend fun isHealthy(): Boolean

    /**
     * Get timestamp of last access.
     *
     * @return Unix timestamp in milliseconds
     */
    suspend fun getLastAccessTime(): Long
}
