// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.io

import android.content.Context
import android.util.Base64
import android.util.Log
import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DataExporter(private val context: Context) {
    
    companion object {
        private const val TAG = "DataExporter"
        private const val EXPORT_VERSION = "1.0.0"
        private const val ENCRYPTION_KEY = "VOS4DataExport2024SecureKey12345" // 32 chars for AES-256
        private const val ENCRYPTION_IV = "VOS4InitVector16" // 16 chars for IV
    }
    
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .disableHtmlEscaping()
        .create()
    
    suspend fun exportToJson(includeAll: Boolean = true): String? = withContext(Dispatchers.IO) {
        try {
            val exportData = collectData(includeAll)
            val jsonData = createCompactJson(exportData)
            val encrypted = encryptData(jsonData)
            val checksum = calculateChecksum(jsonData)
            
            val exportWrapper = ExportWrapper(
                version = EXPORT_VERSION,
                exportDate = System.currentTimeMillis(),
                deviceId = getAnonymousDeviceId(),
                dataChecksum = checksum,
                encodedData = encrypted
            )
            
            gson.toJson(exportWrapper)
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            null
        }
    }
    
    suspend fun exportToFile(fileName: String = "vos3_backup.json", includeAll: Boolean = true): File? = withContext(Dispatchers.IO) {
        try {
            val json = exportToJson(includeAll) ?: return@withContext null
            
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val file = File(exportDir, fileName)
            file.writeText(json)
            
            Log.i(TAG, "Data exported to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Export to file failed", e)
            null
        }
    }
    
    private suspend fun collectData(includeAll: Boolean): VOS4DataExport = withContext(Dispatchers.IO) {
        val database = DatabaseManager.database
        
        VOS4DataExport(
            userPreferences = if (includeAll) database.userPreferenceDao().getAll() else null,
            commandHistory = if (includeAll) database.commandHistoryEntryDao().getAll() else null,
            customCommands = database.customCommandDao().getAll(),
            touchGestures = database.touchGestureDao().getBySystemGestureStatus(false),
            userSequences = database.userSequenceDao().getAll(),
            deviceProfiles = if (includeAll) database.deviceProfileDao().getAll() else null,
            usageStatistics = if (includeAll) database.usageStatisticDao().getAll() else null,
            retentionSettings = database.retentionSettingsDao().getById(1),
            analyticsSettings = if (includeAll) database.analyticsSettingsDao().getById(1) else null
        )
    }
    
    private fun createCompactJson(data: VOS4DataExport): String {
        // Create compact JSON with arrays and short keys
        val compactData = mutableMapOf<String, Any>()
        
        data.userPreferences?.let { prefs ->
            compactData["p"] = prefs.map { arrayOf(it.key, it.value, it.type, it.module) }
        }
        
        data.commandHistory?.let { history ->
            compactData["h"] = history.map { 
                arrayOf(it.originalText, it.processedCommand, it.confidence, 
                       it.timestamp, it.language, it.engineUsed, it.success, 
                       it.executionTimeMs, it.usageCount)
            }
        }
        
        data.customCommands?.let { commands ->
            compactData["c"] = commands.map {
                mapOf(
                    "n" to it.name,
                    "ph" to it.phrases,
                    "a" to it.action,
                    "pr" to it.parameters,
                    "l" to it.language,
                    "ac" to it.isActive,
                    "cd" to it.createdDate,
                    "uc" to it.usageCount
                )
            }
        }
        
        data.touchGestures?.let { gestures ->
            compactData["g"] = gestures.map {
                mapOf(
                    "n" to it.name,
                    "d" to it.gestureData,
                    "ds" to it.description,
                    "cd" to it.createdDate,
                    "uc" to it.usageCount,
                    "ac" to it.associatedCommand
                )
            }
        }
        
        data.userSequences?.let { sequences ->
            compactData["s"] = sequences.map {
                mapOf(
                    "n" to it.name,
                    "d" to it.description,
                    "st" to it.steps,
                    "t" to it.triggerPhrase,
                    "l" to it.language,
                    "cd" to it.createdDate,
                    "uc" to it.usageCount,
                    "ed" to it.estimatedDurationMs
                )
            }
        }
        
        return gson.toJson(compactData)
    }
    
    private fun encryptData(data: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(ENCRYPTION_KEY.toByteArray(Charsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(ENCRYPTION_IV.toByteArray(Charsets.UTF_8))
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            // Fallback to base64 encoding without encryption
            Base64.encodeToString(data.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }
    
    private fun calculateChecksum(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private fun getAnonymousDeviceId(): String {
        // Generate anonymous device ID based on installation
        val prefs = context.getSharedPreferences("vos3_prefs", Context.MODE_PRIVATE)
        var deviceId = prefs.getString("device_id", null)
        
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", deviceId).apply()
        }
        
        return deviceId
    }
}

data class ExportWrapper(
    val version: String,
    val exportDate: Long,
    val deviceId: String,
    val dataChecksum: String,
    val encodedData: String
)

data class VOS4DataExport(
    val userPreferences: List<UserPreference>? = null,
    val commandHistory: List<CommandHistoryEntry>? = null,
    val customCommands: List<CustomCommand>? = null,
    val touchGestures: List<TouchGesture>? = null,
    val userSequences: List<UserSequence>? = null,
    val deviceProfiles: List<DeviceProfile>? = null,
    val usageStatistics: List<UsageStatistic>? = null,
    val retentionSettings: RetentionSettings? = null,
    val analyticsSettings: AnalyticsSettings? = null
)