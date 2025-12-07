// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.io

import android.content.Context
import android.util.Base64
import android.util.Log
import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DataImporter(private val context: Context) {
    
    companion object {
        private const val TAG = "DataImporter"
        private const val ENCRYPTION_KEY = "VOS4DataExport2024SecureKey12345"
        private const val ENCRYPTION_IV = "VOS4InitVector16"
    }
    
    private val gson = Gson()
    
    suspend fun importFromJson(
        jsonData: String,
        replaceExisting: Boolean = false,
        options: ImportOptions = ImportOptions()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val wrapper = gson.fromJson(jsonData, ExportWrapper::class.java)
            
            // Verify checksum if required
            if (options.verifyChecksum) {
                val decrypted = decryptData(wrapper.encodedData)
                val calculatedChecksum = calculateChecksum(decrypted)
                if (calculatedChecksum != wrapper.dataChecksum) {
                    Log.e(TAG, "Checksum verification failed")
                    return@withContext false
                }
            }
            
            // Decrypt and parse data
            val decryptedJson = decryptData(wrapper.encodedData)
            val data = parseCompactJson(decryptedJson)
            
            // Import data based on options
            importData(data, replaceExisting, options)
            
            Log.i(TAG, "Data imported successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            false
        }
    }
    
    suspend fun importFromFile(file: File, replaceExisting: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonData = file.readText()
            importFromJson(jsonData, replaceExisting)
        } catch (e: Exception) {
            Log.e(TAG, "Import from file failed", e)
            false
        }
    }
    
    private fun decryptData(encryptedData: String): String {
        return try {
            val encrypted = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(ENCRYPTION_KEY.toByteArray(Charsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(ENCRYPTION_IV.toByteArray(Charsets.UTF_8))
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decrypted = cipher.doFinal(encrypted)
            
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Decryption failed, trying base64 decode", e)
            // Fallback to simple base64 decode
            String(Base64.decode(encryptedData, Base64.NO_WRAP), Charsets.UTF_8)
        }
    }
    
    private fun parseCompactJson(json: String): VOS4DataExport {
        val jsonObject = gson.fromJson(json, JsonObject::class.java)
        val data = VOS4DataExport()
        
        // Parse user preferences
        jsonObject.getAsJsonArray("p")?.let { array ->
            val prefs = mutableListOf<UserPreference>()
            array.forEach { element ->
                val arr = element.asJsonArray
                prefs.add(UserPreference(
                    key = arr[0].asString,
                    value = arr[1].asString,
                    type = arr[2].asString,
                    module = arr[3].asString
                ))
            }
            data.copy(userPreferences = prefs)
        }
        
        // Parse custom commands
        jsonObject.getAsJsonArray("c")?.let { array ->
            val commands = mutableListOf<CustomCommand>()
            array.forEach { element ->
                val obj = element.asJsonObject
                commands.add(CustomCommand(
                    name = obj.get("n").asString,
                    phrases = try {
                        @Suppress("UNCHECKED_CAST")
                        gson.fromJson(obj.get("ph"), List::class.java) as List<String>
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse phrases, using empty list", e)
                        emptyList()
                    },
                    action = obj.get("a").asString,
                    parameters = obj.get("pr").asString,
                    language = obj.get("l").asString,
                    isActive = obj.get("ac").asBoolean,
                    createdDate = obj.get("cd").asLong,
                    usageCount = obj.get("uc").asInt
                ))
            }
            data.copy(customCommands = commands)
        }
        
        // Parse touch gestures
        jsonObject.getAsJsonArray("g")?.let { array ->
            val gestures = mutableListOf<TouchGesture>()
            array.forEach { element ->
                val obj = element.asJsonObject
                gestures.add(TouchGesture(
                    name = obj.get("n").asString,
                    gestureData = obj.get("d").asString,
                    description = obj.get("ds").asString,
                    createdDate = obj.get("cd").asLong,
                    usageCount = obj.get("uc").asInt,
                    associatedCommand = obj.get("ac")?.asString
                ))
            }
            data.copy(touchGestures = gestures)
        }
        
        // Parse user sequences
        jsonObject.getAsJsonArray("s")?.let { array ->
            val sequences = mutableListOf<UserSequence>()
            array.forEach { element ->
                val obj = element.asJsonObject
                sequences.add(UserSequence(
                    name = obj.get("n").asString,
                    description = obj.get("d").asString,
                    steps = try {
                        @Suppress("UNCHECKED_CAST")
                        gson.fromJson(obj.get("st"), List::class.java) as List<String>
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse steps, using empty list", e)
                        emptyList()
                    },
                    triggerPhrase = obj.get("t").asString,
                    language = obj.get("l").asString,
                    createdDate = obj.get("cd").asLong,
                    usageCount = obj.get("uc").asInt,
                    estimatedDurationMs = obj.get("ed").asLong
                ))
            }
            data.copy(userSequences = sequences)
        }
        
        return data
    }
    
    private suspend fun importData(
        data: VOS4DataExport,
        replaceExisting: Boolean,
        options: ImportOptions
    ) = withContext(Dispatchers.IO) {
        val database = DatabaseManager.database
        
        if (replaceExisting) {
            // Clear existing data for selected types
            if (options.importPreferences) database.userPreferenceDao().deleteAll()
            if (options.importCommandHistory) database.commandHistoryEntryDao().deleteAll()
            if (options.importCustomCommands) database.customCommandDao().deleteAll()
            if (options.importTouchGestures) {
                // Delete non-system gestures
                val nonSystemGestures = database.touchGestureDao().getBySystemGestureStatus(false)
                database.touchGestureDao().deleteAll(nonSystemGestures)
            }
            if (options.importUserSequences) database.userSequenceDao().deleteAll()
            if (options.importDeviceProfiles) database.deviceProfileDao().deleteAll()
        }
        
        // Import data
        if (options.importPreferences) {
            data.userPreferences?.let { database.userPreferenceDao().insertAll(it) }
        }
        
        if (options.importCommandHistory) {
            data.commandHistory?.let { database.commandHistoryEntryDao().insertAll(it) }
        }
        
        if (options.importCustomCommands) {
            data.customCommands?.let { database.customCommandDao().insertAll(it) }
        }
        
        if (options.importTouchGestures) {
            data.touchGestures?.let { database.touchGestureDao().insertAll(it) }
        }
        
        if (options.importUserSequences) {
            data.userSequences?.let { database.userSequenceDao().insertAll(it) }
        }
        
        if (options.importDeviceProfiles) {
            data.deviceProfiles?.let { database.deviceProfileDao().insertAll(it) }
        }
        
        if (options.importStatistics) {
            data.usageStatistics?.let { database.usageStatisticDao().insertAll(it) }
        }
        
        // Always import settings
        data.retentionSettings?.let { 
            database.retentionSettingsDao().insert(it.copy(id = 1))
        }
        
        data.analyticsSettings?.let {
            database.analyticsSettingsDao().insert(it.copy(id = 1))
        }
    }
    
    private fun calculateChecksum(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}

data class ImportOptions(
    val replaceExisting: Boolean = false,
    val importPreferences: Boolean = true,
    val importCommandHistory: Boolean = true,
    val importCustomCommands: Boolean = true,
    val importUserSequences: Boolean = true,
    val importTouchGestures: Boolean = true,
    val importDeviceProfiles: Boolean = true,
    val importStatistics: Boolean = false,
    val verifyChecksum: Boolean = true
)