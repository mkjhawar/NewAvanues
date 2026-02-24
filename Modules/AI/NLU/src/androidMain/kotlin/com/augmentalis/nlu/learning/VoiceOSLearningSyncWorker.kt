package com.augmentalis.nlu.learning

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.augmentalis.nlu.nluLogDebug
import com.augmentalis.nlu.nluLogError
import com.augmentalis.nlu.nluLogInfo
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.learning.domain.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * VoiceOS Learning Sync Worker
 *
 * Background worker that syncs VoiceOS scraped commands to AVA's NLU system.
 * Uses WorkManager for reliable, battery-efficient background execution.
 *
 * Sync Strategy:
 * - High-confidence commands (≥0.85): Sync every 5 minutes
 * - Low-confidence commands (0.6-0.85): Sync every 6 hours
 * - User-approved commands: Sync immediately (priority)
 *
 * Process:
 * 1. Query VoiceOS ContentProvider for new commands
 * 2. Filter by confidence threshold
 * 3. Compute BERT embeddings
 * 4. Save to AVA's TrainExample table
 * 5. Mark as synced in VoiceOS database
 *
 * @see ADR-014: Unified Learning Architecture
 */
/**
 * Issue I-02 Fix: Route all sync operations through UnifiedLearningService
 * instead of directly calling IntentClassifier. This ensures:
 * - Consistent event emission for all learned commands
 * - Proper consumer notification
 * - Unified statistics tracking
 */
@HiltWorker
class VoiceOSLearningSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val unifiedLearningService: UnifiedLearningService,
    private val intentClassifier: IntentClassifier
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "VoiceOSLearningSyncWorker"

        // Work tags
        const val WORK_TAG = "voiceos_learning_sync"
        const val WORK_NAME_HIGH_CONFIDENCE = "voiceos_sync_high_confidence"
        const val WORK_NAME_LOW_CONFIDENCE = "voiceos_sync_low_confidence"
        const val WORK_NAME_IMMEDIATE = "voiceos_sync_immediate"

        // Input data keys
        const val KEY_SYNC_TYPE = "sync_type"
        const val KEY_MIN_CONFIDENCE = "min_confidence"
        const val KEY_MAX_COMMANDS = "max_commands"
        const val KEY_COMMAND_ID = "command_id" // For immediate sync

        // Sync types
        const val SYNC_TYPE_HIGH_CONFIDENCE = "high_confidence"
        const val SYNC_TYPE_LOW_CONFIDENCE = "low_confidence"
        const val SYNC_TYPE_IMMEDIATE = "immediate"
        const val SYNC_TYPE_USER_APPROVED = "user_approved"

        // Default thresholds
        const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
        const val LOW_CONFIDENCE_THRESHOLD = 0.6f
        const val DEFAULT_MAX_COMMANDS = 50

        // Output keys
        const val KEY_SYNCED_COUNT = "synced_count"
        const val KEY_FAILED_COUNT = "failed_count"
        const val KEY_SKIPPED_COUNT = "skipped_count"

        /**
         * Enqueue high-confidence sync (runs frequently)
         */
        fun enqueueHighConfidenceSync(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val inputData = workDataOf(
                KEY_SYNC_TYPE to SYNC_TYPE_HIGH_CONFIDENCE,
                KEY_MIN_CONFIDENCE to HIGH_CONFIDENCE_THRESHOLD,
                KEY_MAX_COMMANDS to DEFAULT_MAX_COMMANDS
            )

            val request = PeriodicWorkRequestBuilder<VoiceOSLearningSyncWorker>(
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(WORK_TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_HIGH_CONFIDENCE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            nluLogInfo(TAG, "Enqueued high-confidence sync (every 5 min)")
        }

        /**
         * Enqueue low-confidence sync (runs infrequently)
         */
        fun enqueueLowConfidenceSync(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(true) // Only when charging
                .build()

            val inputData = workDataOf(
                KEY_SYNC_TYPE to SYNC_TYPE_LOW_CONFIDENCE,
                KEY_MIN_CONFIDENCE to LOW_CONFIDENCE_THRESHOLD,
                KEY_MAX_COMMANDS to 100
            )

            val request = PeriodicWorkRequestBuilder<VoiceOSLearningSyncWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(WORK_TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_LOW_CONFIDENCE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            nluLogInfo(TAG, "Enqueued low-confidence sync (every 6 hours)")
        }

        /**
         * Enqueue immediate sync for a specific command
         */
        fun enqueueImmediateSync(workManager: WorkManager, commandId: String) {
            val inputData = workDataOf(
                KEY_SYNC_TYPE to SYNC_TYPE_IMMEDIATE,
                KEY_COMMAND_ID to commandId
            )

            val request = OneTimeWorkRequestBuilder<VoiceOSLearningSyncWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(WORK_TAG)
                .build()

            workManager.enqueueUniqueWork(
                "${WORK_NAME_IMMEDIATE}_$commandId",
                ExistingWorkPolicy.REPLACE,
                request
            )

            nluLogInfo(TAG, "Enqueued immediate sync for command: $commandId")
        }

        /**
         * Enqueue user-approved commands sync (priority)
         */
        fun enqueueUserApprovedSync(workManager: WorkManager) {
            val inputData = workDataOf(
                KEY_SYNC_TYPE to SYNC_TYPE_USER_APPROVED,
                KEY_MAX_COMMANDS to 100
            )

            val request = OneTimeWorkRequestBuilder<VoiceOSLearningSyncWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(WORK_TAG)
                .build()

            workManager.enqueueUniqueWork(
                "${WORK_NAME_IMMEDIATE}_user_approved",
                ExistingWorkPolicy.REPLACE,
                request
            )

            nluLogInfo(TAG, "Enqueued user-approved sync")
        }

        /**
         * Cancel all sync workers
         */
        fun cancelAllSync(workManager: WorkManager) {
            workManager.cancelAllWorkByTag(WORK_TAG)
            nluLogInfo(TAG, "Cancelled all VoiceOS sync workers")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_HIGH_CONFIDENCE
        val minConfidence = inputData.getFloat(KEY_MIN_CONFIDENCE, HIGH_CONFIDENCE_THRESHOLD)
        val maxCommands = inputData.getInt(KEY_MAX_COMMANDS, DEFAULT_MAX_COMMANDS)

        nluLogInfo(TAG, "Starting sync: type=$syncType, minConfidence=$minConfidence, max=$maxCommands")

        try {
            val result = when (syncType) {
                SYNC_TYPE_IMMEDIATE -> syncSingleCommand()
                SYNC_TYPE_USER_APPROVED -> syncUserApproved(maxCommands)
                else -> syncByConfidence(minConfidence, maxCommands)
            }

            val outputData = workDataOf(
                KEY_SYNCED_COUNT to result.synced,
                KEY_FAILED_COUNT to result.failed,
                KEY_SKIPPED_COUNT to result.skipped
            )

            nluLogInfo(TAG, "Sync complete: synced=${result.synced}, failed=${result.failed}, skipped=${result.skipped}")

            if (result.failed > 0 && result.synced == 0) {
                Result.retry()
            } else {
                Result.success(outputData)
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Sync failed: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to e.message))
            }
        }
    }

    /**
     * Sync commands by confidence threshold
     *
     * Issue I-02 Fix: Route through UnifiedLearningService for proper
     * event emission and consumer notification.
     */
    private suspend fun syncByConfidence(minConfidence: Float, maxCommands: Int): SyncResult {
        var synced = 0
        var failed = 0
        var skipped = 0

        // Query VoiceOS ContentProvider for commands
        val commands = queryVoiceOSCommands(minConfidence, maxCommands)

        nluLogDebug(TAG, "Found ${commands.size} commands to sync")

        for (command in commands) {
            try {
                // Check if already exists in AVA
                val existing = intentClassifier.findEmbeddingByUtterance(command.utterance)
                if (existing != null) {
                    skipped++
                    continue
                }

                // Issue I-02: Route through UnifiedLearningService instead of direct IntentClassifier
                val consumed = unifiedLearningService.consume(command)

                if (consumed) {
                    synced++
                    // Mark as synced in VoiceOS (via ContentProvider or direct DB)
                    markSyncedInVoiceOS(command.id)
                } else {
                    failed++
                }
            } catch (e: Exception) {
                // PII-safe: log utterance length, not content
                nluLogError(TAG, "Failed to sync command ${command.utterance.length}-char cmd: ${e.message}")
                failed++
            }
        }

        return SyncResult(synced, failed, skipped)
    }

    /**
     * Sync user-approved commands (priority)
     *
     * Issue I-02 Fix: Route through UnifiedLearningService for proper
     * event emission and consumer notification.
     */
    private suspend fun syncUserApproved(maxCommands: Int): SyncResult {
        var synced = 0
        var failed = 0
        var skipped = 0

        val commands = queryVoiceOSUserApproved(maxCommands)

        nluLogDebug(TAG, "Found ${commands.size} user-approved commands to sync")

        for (command in commands) {
            try {
                val existing = intentClassifier.findEmbeddingByUtterance(command.utterance)
                if (existing != null) {
                    skipped++
                    continue
                }

                // Issue I-02: Route through UnifiedLearningService
                // Mark as VOICEOS_APPROVED source for higher priority
                val approvedCommand = command.copy(source = LearningSource.VOICEOS_APPROVED)
                val consumed = unifiedLearningService.consume(approvedCommand)

                if (consumed) {
                    synced++
                    markSyncedInVoiceOS(command.id)
                } else {
                    failed++
                }
            } catch (e: Exception) {
                nluLogError(TAG, "Failed to sync approved command: ${e.message}")
                failed++
            }
        }

        return SyncResult(synced, failed, skipped)
    }

    /**
     * Sync a single command immediately
     *
     * Issue I-02 Fix: Route through UnifiedLearningService for proper
     * event emission and consumer notification.
     */
    private suspend fun syncSingleCommand(): SyncResult {
        val commandId = inputData.getString(KEY_COMMAND_ID)
            ?: return SyncResult(0, 1, 0)

        val command = queryVoiceOSCommandById(commandId)
            ?: return SyncResult(0, 1, 0)

        return try {
            val existing = intentClassifier.findEmbeddingByUtterance(command.utterance)
            if (existing != null) {
                return SyncResult(0, 0, 1)
            }

            // Issue I-02: Route through UnifiedLearningService
            val consumed = unifiedLearningService.consume(command)

            if (consumed) {
                markSyncedInVoiceOS(commandId)
                SyncResult(1, 0, 0)
            } else {
                SyncResult(0, 1, 0)
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to sync single command: ${e.message}")
            SyncResult(0, 1, 0)
        }
    }

    // ==================== VoiceOS Integration ====================

    /**
     * Query VoiceOS ContentProvider for commands
     *
     * Note: This queries VoiceOS via ContentProvider.
     * If VoiceOS is not installed, returns empty list.
     */
    private fun queryVoiceOSCommands(minConfidence: Float, limit: Int): List<LearnedCommand> {
        val commands = mutableListOf<LearnedCommand>()

        try {
            val uri = android.net.Uri.parse(
                "content://com.avanues.voiceos.provider/commands_generated"
            )

            val cursor = context.contentResolver.query(
                uri,
                null,
                "confidence >= ? AND synced_to_ava = 0",
                arrayOf(minConfidence.toString()),
                "confidence DESC LIMIT $limit"
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
            nluLogError(TAG, "Failed to query VoiceOS commands: ${e.message}")
        }

        return commands
    }

    /**
     * Query user-approved commands from VoiceOS
     */
    private fun queryVoiceOSUserApproved(limit: Int): List<LearnedCommand> {
        val commands = mutableListOf<LearnedCommand>()

        try {
            val uri = android.net.Uri.parse(
                "content://com.avanues.voiceos.provider/commands_generated"
            )

            val cursor = context.contentResolver.query(
                uri,
                null,
                "isUserApproved = 1 AND synced_to_ava = 0",
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
            nluLogError(TAG, "Failed to query VoiceOS approved commands: ${e.message}")
        }

        return commands
    }

    /**
     * Query single command by ID
     */
    private fun queryVoiceOSCommandById(id: String): LearnedCommand? {
        try {
            val uri = android.net.Uri.parse(
                "content://com.avanues.voiceos.provider/commands_generated/$id"
            )

            val cursor = context.contentResolver.query(uri, null, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    return cursorToLearnedCommand(it)
                }
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to query VoiceOS command by ID: ${e.message}")
        }

        return null
    }

    /**
     * Mark command as synced in VoiceOS
     */
    private fun markSyncedInVoiceOS(commandId: String) {
        try {
            val uri = android.net.Uri.parse(
                "content://com.avanues.voiceos.provider/commands_generated/$commandId"
            )

            val values = android.content.ContentValues().apply {
                put("synced_to_ava", 1)
                put("synced_at", System.currentTimeMillis())
            }

            context.contentResolver.update(uri, values, null, null)
            nluLogDebug(TAG, "Marked command $commandId as synced")
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to mark synced in VoiceOS: ${e.message}")
        }
    }

    /**
     * Convert cursor row to LearnedCommand
     */
    private fun cursorToLearnedCommand(cursor: android.database.Cursor): LearnedCommand? {
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
            val synonymsJson = cursor.getString(cursor.getColumnIndexOrThrow("synonyms"))
            val synonyms = parseSynonymsJson(synonymsJson)

            // Get package name if available
            val packageNameIndex = cursor.getColumnIndex("packageName")
            val packageName = if (packageNameIndex >= 0) cursor.getString(packageNameIndex) else null

            LearnedCommand.fromVoiceOS(
                commandText = commandText,
                actionType = actionType,
                confidence = confidence,
                elementHash = elementHash,
                packageName = packageName,
                synonyms = synonyms,
                isUserApproved = isUserApproved,
                createdAt = createdAt,
                usageCount = usageCount
            ).copy(id = id)
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to parse cursor to command: ${e.message}")
            null
        }
    }

    /**
     * Parse synonyms JSON array
     */
    private fun parseSynonymsJson(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            // Simple JSON array parsing
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

    /**
     * Sync result data class
     */
    private data class SyncResult(
        val synced: Int,
        val failed: Int,
        val skipped: Int
    )
}
