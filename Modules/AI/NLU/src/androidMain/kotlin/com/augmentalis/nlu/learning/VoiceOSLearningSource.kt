package com.augmentalis.nlu.learning

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.augmentalis.nlu.learning.domain.*
import com.augmentalis.nlu.nluLogDebug
import com.augmentalis.nlu.nluLogError
import com.augmentalis.nlu.nluLogInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VoiceOS Learning Source - Adapter for VoiceOS generated commands
 *
 * Implements ILearningSource to provide VoiceOS commands to the unified
 * learning system. Queries VoiceOS via ContentProvider.
 *
 * Features:
 * - Queries commands_generated table via ContentProvider
 * - Filters by confidence threshold
 * - Tracks sync status via synced_to_ava column
 * - Emits events when new commands are detected
 *
 * Prerequisites:
 * - VoiceOS must expose ContentProvider at: content://com.avanues.voiceos.provider/
 * - commands_generated table must have synced_to_ava column
 *
 * @see ADR-014: Unified Learning Architecture
 */
@Singleton
class VoiceOSLearningSource @Inject constructor(
    private val context: Context
) : ILearningSource {

    companion object {
        private const val TAG = "VoiceOSLearningSource"

        // ContentProvider authority
        private const val VOICEOS_AUTHORITY = "com.avanues.voiceos.provider"
        private val COMMANDS_URI = Uri.parse("content://$VOICEOS_AUTHORITY/commands_generated")

        // Minimum confidence to sync
        const val MIN_SYNC_CONFIDENCE = 0.6f
    }

    override val sourceId: String = "voiceos_learning"
    override val sourceName: String = "VoiceOS Scraping"

    // Registered listeners
    private val listeners = CopyOnWriteArrayList<LearningEventListener>()

    /**
     * Check if VoiceOS ContentProvider is available
     */
    fun isVoiceOSAvailable(): Boolean {
        return try {
            context.contentResolver.query(
                COMMANDS_URI,
                arrayOf("id"),
                null,
                null,
                "LIMIT 1"
            )?.use { it.count >= 0 } ?: false
        } catch (e: Exception) {
            nluLogDebug(TAG, "VoiceOS not available: ${e.message}")
            false
        }
    }

    /**
     * Get unsynced commands from VoiceOS
     */
    override suspend fun getUnsyncedCommands(limit: Int): List<LearnedCommand> = withContext(Dispatchers.IO) {
        if (!isVoiceOSAvailable()) {
            nluLogDebug(TAG, "VoiceOS not available, returning empty list")
            return@withContext emptyList()
        }

        val commands = mutableListOf<LearnedCommand>()

        try {
            val cursor = context.contentResolver.query(
                COMMANDS_URI,
                null,
                "confidence >= ? AND (synced_to_ava IS NULL OR synced_to_ava = 0)",
                arrayOf(MIN_SYNC_CONFIDENCE.toString()),
                "confidence DESC, isUserApproved DESC LIMIT $limit"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val command = cursorToLearnedCommand(it)
                    if (command != null) {
                        commands.add(command)
                    }
                }
            }

            nluLogDebug(TAG, "Found ${commands.size} unsynced commands")
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to query unsynced commands: ${e.message}", e)
        }

        commands
    }

    /**
     * Mark commands as synced
     */
    override suspend fun markSynced(commandIds: List<String>) = withContext(Dispatchers.IO) {
        if (!isVoiceOSAvailable() || commandIds.isEmpty()) return@withContext

        try {
            for (id in commandIds) {
                val uri = Uri.withAppendedPath(COMMANDS_URI, id)
                val values = android.content.ContentValues().apply {
                    put("synced_to_ava", 1)
                    put("synced_at", System.currentTimeMillis())
                }
                context.contentResolver.update(uri, values, null, null)
            }
            nluLogDebug(TAG, "Marked ${commandIds.size} commands as synced")
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to mark synced: ${e.message}", e)
        }
    }

    /**
     * Get total command count
     */
    override suspend fun getCommandCount(): Int = withContext(Dispatchers.IO) {
        if (!isVoiceOSAvailable()) return@withContext 0

        try {
            val cursor = context.contentResolver.query(
                COMMANDS_URI,
                arrayOf("COUNT(*) as count"),
                "confidence >= ?",
                arrayOf(MIN_SYNC_CONFIDENCE.toString()),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    return@withContext it.getInt(0)
                }
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to get command count: ${e.message}", e)
        }

        0
    }

    /**
     * Get high-confidence commands (user-approved or confidence >= 0.85)
     */
    suspend fun getHighConfidenceCommands(limit: Int = 50): List<LearnedCommand> = withContext(Dispatchers.IO) {
        if (!isVoiceOSAvailable()) return@withContext emptyList()

        val commands = mutableListOf<LearnedCommand>()

        try {
            val cursor = context.contentResolver.query(
                COMMANDS_URI,
                null,
                "(isUserApproved = 1 OR confidence >= 0.85) AND (synced_to_ava IS NULL OR synced_to_ava = 0)",
                null,
                "isUserApproved DESC, confidence DESC LIMIT $limit"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val command = cursorToLearnedCommand(it)
                    if (command != null) {
                        commands.add(command)
                    }
                }
            }

            nluLogDebug(TAG, "Found ${commands.size} high-confidence commands")
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to query high-confidence commands: ${e.message}", e)
        }

        commands
    }

    /**
     * Get user-approved commands only
     */
    suspend fun getUserApprovedCommands(limit: Int = 100): List<LearnedCommand> = withContext(Dispatchers.IO) {
        if (!isVoiceOSAvailable()) return@withContext emptyList()

        val commands = mutableListOf<LearnedCommand>()

        try {
            val cursor = context.contentResolver.query(
                COMMANDS_URI,
                null,
                "isUserApproved = 1 AND (synced_to_ava IS NULL OR synced_to_ava = 0)",
                null,
                "createdAt DESC LIMIT $limit"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val command = cursorToLearnedCommand(it)
                    if (command != null) {
                        commands.add(command)
                    }
                }
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to query user-approved commands: ${e.message}", e)
        }

        commands
    }

    /**
     * Get commands by package name (app-specific)
     */
    suspend fun getCommandsByPackage(packageName: String, limit: Int = 50): List<LearnedCommand> = withContext(Dispatchers.IO) {
        if (!isVoiceOSAvailable()) return@withContext emptyList()

        val commands = mutableListOf<LearnedCommand>()

        try {
            // Need to join with scraped_element table to get package
            val uri = Uri.parse("content://$VOICEOS_AUTHORITY/commands_by_package/$packageName")
            val cursor = context.contentResolver.query(
                uri,
                null,
                "confidence >= ?",
                arrayOf(MIN_SYNC_CONFIDENCE.toString()),
                "confidence DESC LIMIT $limit"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val command = cursorToLearnedCommand(it)?.copy(packageName = packageName)
                    if (command != null) {
                        commands.add(command)
                    }
                }
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to query commands by package: ${e.message}", e)
        }

        commands
    }

    // ==================== Event Listener Management ====================

    override fun addLearningListener(listener: LearningEventListener) {
        listeners.add(listener)
        nluLogDebug(TAG, "Added listener, total: ${listeners.size}")
    }

    override fun removeLearningListener(listener: LearningEventListener) {
        listeners.remove(listener)
        nluLogDebug(TAG, "Removed listener, total: ${listeners.size}")
    }

    /**
     * Emit learning event to all registered listeners
     */
    private fun emitEvent(event: LearningEvent) {
        listeners.forEach { listener ->
            try {
                listener.onLearningEvent(event)
            } catch (e: Exception) {
                nluLogError(TAG, "Listener failed: ${e.message}", e)
            }
        }
    }

    /**
     * Call this when VoiceOS learns a new command
     * (To be called from VoiceOS ContentObserver or broadcast receiver)
     */
    fun onCommandLearned(command: LearnedCommand) {
        emitEvent(LearningEvent.CommandLearned(
            command = command,
            sourceSystem = "VoiceOS"
        ))
    }

    // ==================== Private Helpers ====================

    /**
     * Convert cursor row to LearnedCommand
     */
    private fun cursorToLearnedCommand(cursor: Cursor): LearnedCommand? {
        return try {
            val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            val commandText = cursor.getString(cursor.getColumnIndexOrThrow("commandText"))
            val actionType = cursor.getString(cursor.getColumnIndexOrThrow("actionType"))
            val confidence = cursor.getDouble(cursor.getColumnIndexOrThrow("confidence"))
            val elementHash = cursor.getString(cursor.getColumnIndexOrThrow("elementHash"))
            val isUserApproved = cursor.getInt(cursor.getColumnIndexOrThrow("isUserApproved")) == 1
            val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
            val usageCount = cursor.getInt(cursor.getColumnIndexOrThrow("usageCount"))

            // Parse synonyms JSON
            val synonymsJson = try {
                cursor.getString(cursor.getColumnIndexOrThrow("synonyms"))
            } catch (e: Exception) { null }
            val synonyms = parseSynonymsJson(synonymsJson)

            LearnedCommand.fromVoiceOS(
                commandText = commandText,
                actionType = actionType,
                confidence = confidence,
                elementHash = elementHash,
                synonyms = synonyms,
                isUserApproved = isUserApproved,
                createdAt = createdAt,
                usageCount = usageCount
            ).copy(id = id)
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to parse cursor: ${e.message}")
            null
        }
    }

    /**
     * Parse synonyms JSON array
     */
    private fun parseSynonymsJson(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            json.trim()
                .removePrefix("[")
                .removeSuffix("]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== Statistics ====================

    /**
     * Get source statistics
     */
    suspend fun getStats(): VoiceOSLearningStats = withContext(Dispatchers.IO) {
        if (!isVoiceOSAvailable()) {
            return@withContext VoiceOSLearningStats(0, 0, 0, 0, 0, false)
        }

        try {
            val totalCount = getCommandCount()

            // Get synced count
            val syncedCursor = context.contentResolver.query(
                COMMANDS_URI,
                arrayOf("COUNT(*) as count"),
                "synced_to_ava = 1",
                null,
                null
            )
            val syncedCount = syncedCursor?.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            } ?: 0

            // Get pending count
            val pendingCount = totalCount - syncedCount

            // Get user-approved count
            val approvedCursor = context.contentResolver.query(
                COMMANDS_URI,
                arrayOf("COUNT(*) as count"),
                "isUserApproved = 1",
                null,
                null
            )
            val approvedCount = approvedCursor?.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            } ?: 0

            // Get high-confidence count
            val highConfCursor = context.contentResolver.query(
                COMMANDS_URI,
                arrayOf("COUNT(*) as count"),
                "confidence >= 0.85",
                null,
                null
            )
            val highConfCount = highConfCursor?.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            } ?: 0

            VoiceOSLearningStats(
                totalCommands = totalCount,
                syncedToAva = syncedCount,
                pendingSync = pendingCount,
                userApproved = approvedCount,
                highConfidence = highConfCount,
                voiceosAvailable = true
            )
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to get stats: ${e.message}", e)
            VoiceOSLearningStats(0, 0, 0, 0, 0, false)
        }
    }

    /**
     * Statistics data class
     */
    data class VoiceOSLearningStats(
        val totalCommands: Int,
        val syncedToAva: Int,
        val pendingSync: Int,
        val userApproved: Int,
        val highConfidence: Int,
        val voiceosAvailable: Boolean
    ) {
        val syncPercent: Int
            get() = if (totalCommands > 0) (syncedToAva * 100) / totalCommands else 0
    }
}
