package com.augmentalis.voiceavanue.access

import android.content.Context
import com.augmentalis.voiceavanue.client.DatabaseClient
import com.augmentalis.voiceavanue.models.AppSettings
import com.augmentalis.voiceavanue.models.User
import com.augmentalis.voiceavanue.models.VoiceCommand

/**
 * DatabaseAccess implementation using IPC via DatabaseClient.
 *
 * This adapter delegates all operations to DatabaseClient, which
 * communicates with DatabaseService via AIDL IPC.
 *
 * Benefits:
 * - Database runs in separate :database process
 * - Main process memory reduced by ~20 MB
 * - Database crashes isolated
 * - Cross-app sharing enabled via ContentProvider
 *
 * Usage:
 * ```kotlin
 * val database = DatabaseClientAdapter(context)
 * database.connect()
 * val users = database.getAllUsers()
 * database.disconnect()
 * ```
 *
 * @param context Application context
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class DatabaseClientAdapter(private val context: Context) : DatabaseAccess {

    private val client: DatabaseClient by lazy {
        DatabaseClient.getInstance(context)
    }

    // ===== Connection Management =====

    override suspend fun connect(): Boolean {
        return client.connect()
    }

    override fun disconnect() {
        client.disconnect()
    }

    override fun isConnected(): Boolean {
        return client.isConnected()
    }

    // ===== User Operations =====

    override suspend fun getAllUsers(): List<User> {
        return client.getAllUsers()
    }

    override suspend fun getUserById(userId: Int): User? {
        return client.getUserById(userId)
    }

    override suspend fun insertUser(user: User) {
        client.insertUser(user)
    }

    override suspend fun updateUser(user: User) {
        client.updateUser(user)
    }

    override suspend fun deleteUser(userId: Int) {
        client.deleteUser(userId)
    }

    override suspend fun getUserCount(): Int {
        return client.getUserCount()
    }

    // ===== Voice Command Operations =====

    override suspend fun getAllVoiceCommands(): List<VoiceCommand> {
        return client.getAllVoiceCommands()
    }

    override suspend fun getVoiceCommandById(commandId: Int): VoiceCommand? {
        return client.getVoiceCommandById(commandId)
    }

    override suspend fun getVoiceCommandsByCategory(category: String): List<VoiceCommand> {
        return client.getVoiceCommandsByCategory(category)
    }

    override suspend fun insertVoiceCommand(command: VoiceCommand) {
        client.insertVoiceCommand(command)
    }

    override suspend fun updateVoiceCommand(command: VoiceCommand) {
        client.updateVoiceCommand(command)
    }

    override suspend fun deleteVoiceCommand(commandId: Int) {
        client.deleteVoiceCommand(commandId)
    }

    // ===== Settings Operations =====

    override suspend fun getSettings(): AppSettings? {
        return client.getSettings()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        client.updateSettings(settings)
    }

    override suspend fun getSettingValue(key: String): String? {
        return client.getSettingValue(key)
    }

    override suspend fun setSettingValue(key: String, value: String) {
        client.setSettingValue(key, value)
    }

    // ===== Maintenance Operations =====

    override suspend fun clearAllData() {
        client.clearAllData()
    }

    override suspend fun getDatabaseSize(): Long {
        return client.getDatabaseSize()
    }

    override suspend fun vacuum() {
        client.vacuum()
    }

    override suspend fun getDatabaseVersion(): String? {
        return client.getDatabaseVersion()
    }

    // ===== Health & Utility =====

    override suspend fun isHealthy(): Boolean {
        return client.isHealthy()
    }

    override suspend fun getLastAccessTime(): Long {
        return client.getLastAccessTime()
    }
}
