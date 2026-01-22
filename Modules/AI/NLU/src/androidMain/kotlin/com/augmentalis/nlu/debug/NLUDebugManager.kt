package com.augmentalis.nlu.debug

import android.content.Context
import android.util.Log
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.migration.IntentSourceCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NLU Debug Manager
 *
 * Provides debugging utilities for NLU system:
 * - Clear database and reload from .ava files
 * - Force re-migration
 * - View current state
 *
 * Created: 2025-11-17
 * Author: AVA Team
 */
object NLUDebugManager {
    private const val TAG = "NLUDebugManager"

    /**
     * Clear database and reload intents from .ava files
     *
     * This forces a complete re-migration from .ava sources,
     * replacing any existing database content.
     *
     * @param context Application context
     * @return Number of examples loaded
     */
    suspend fun reloadFromAvaSources(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "=== Starting NLU Data Reload ===")

            // Step 1: Clear existing database
            val coordinator = IntentSourceCoordinator(context)
            Log.i(TAG, "Clearing existing database...")
            coordinator.clearDatabase()

            // Step 2: Force migration from .ava files
            Log.i(TAG, "Forcing migration from .ava sources...")
            val count = coordinator.forceMigration()

            // Step 3: Re-initialize classifier to reload embeddings
            Log.i(TAG, "Re-initializing classifier with new data...")
            val classifier = IntentClassifier.getInstance(context)
            val modelPath = context.getExternalFilesDir(null)?.absolutePath + "/models/AVA-384-Base-INT8.AON"
            classifier.initialize(modelPath)

            Log.i(TAG, "=== NLU Data Reload Complete ===")
            Log.i(TAG, "Loaded $count examples from .ava files")

            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload NLU data: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get current migration status
     *
     * @param context Application context
     * @return Migration status map
     */
    suspend fun getMigrationStatus(context: Context): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val coordinator = IntentSourceCoordinator(context)
            coordinator.getMigrationStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get migration status: ${e.message}", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }

    /**
     * Verify .ava files exist on device
     *
     * @param context Application context
     * @return List of .ava file paths found
     */
    suspend fun verifyAvaFiles(context: Context): List<String> = withContext(Dispatchers.IO) {
        try {
            val storageBase = context.getExternalFilesDir(null)?.absolutePath + "/.ava"
            val avaFiles = mutableListOf<String>()

            // Check core directory
            val coreDir = java.io.File("$storageBase/core/en-US")
            if (coreDir.exists()) {
                coreDir.listFiles()?.forEach { file ->
                    if (file.extension == "ava") {
                        avaFiles.add(file.absolutePath)
                        Log.d(TAG, "Found .ava file: ${file.name} (${file.length()} bytes)")
                    }
                }
            } else {
                Log.w(TAG, "Core directory not found: ${coreDir.absolutePath}")
            }

            Log.i(TAG, "Found ${avaFiles.size} .ava files")
            avaFiles
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify .ava files: ${e.message}", e)
            emptyList()
        }
    }
}
