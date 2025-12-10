/**
 * RelearnAppCommand.kt - Smart app relearning with retroactive VUID creation
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/commands/RelearnAppCommand.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 *
 * Implements "Relearn App" voice command that intelligently decides whether to:
 * 1. Create missing VUIDs (retroactive - 8 seconds) if VUIDs exist
 * 2. Full re-exploration (18+ minutes) if no VUIDs exist
 *
 * ## User Experience
 * Voice Command: "Relearn DeviceInfo" or "Relearn this app"
 * - If VUIDs exist: Fast update (8 sec) - "Found 1 VUID, creating 116 missing"
 * - If no VUIDs: Full exploration (18 min) - "No VUIDs found, starting full exploration"
 *
 * ## Integration with Phase 4
 * Uses RetroactiveVUIDCreator for smart VUID updates when possible
 */

package com.augmentalis.voiceoscore.learnapp.commands

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.voiceoscore.learnapp.exploration.RetroactiveVUIDCreator
import com.augmentalis.voiceoscore.learnapp.exploration.RetroactiveResult
import com.augmentalis.voiceoscore.learnapp.integration.LearnAppIntegration
import com.augmentalis.voiceoscore.learnapp.database.repository.AppMetadataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of Relearn App command
 */
sealed class RelearnResult {
    /**
     * Success with retroactive creation (fast path)
     * @param existingCount Number of existing VUIDs
     * @param newCount Number of newly created VUIDs
     * @param totalCount Total VUIDs after update
     * @param durationMs Time taken in milliseconds
     */
    data class RetroactiveSuccess(
        val existingCount: Int,
        val newCount: Int,
        val totalCount: Int,
        val durationMs: Long
    ) : RelearnResult()

    /**
     * Full exploration started (no existing VUIDs)
     * @param packageName Package being explored
     */
    data class FullExplorationStarted(
        val packageName: String
    ) : RelearnResult()

    /**
     * Error occurred
     * @param message Error message
     */
    data class Error(val message: String) : RelearnResult()
}

/**
 * Relearn App Command Handler
 *
 * Provides smart app relearning that minimizes user wait time.
 *
 * ## Strategy
 * 1. Check if VUIDs exist for app
 * 2. If VUIDs exist: Use RetroactiveVUIDCreator (8 sec)
 * 3. If no VUIDs: Trigger full exploration (18 min)
 *
 * ## Usage
 * ```kotlin
 * val handler = RelearnAppCommandHandler(context, learnAppIntegration)
 *
 * // Relearn specific app
 * val result = handler.relearnApp("com.ytheekshana.deviceinfo")
 *
 * // Relearn current foreground app
 * val result = handler.relearnCurrentApp()
 * ```
 */
class RelearnAppCommandHandler(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val learnAppIntegration: LearnAppIntegration,
    private val databaseManager: VoiceOSDatabaseManager,
    private val uuidCreator: UUIDCreator,
    private val metadataProvider: AppMetadataProvider
) {
    companion object {
        private const val TAG = "RelearnAppCommand"
        private const val MIN_VUID_THRESHOLD = 1 // If 1+ VUIDs exist, use retroactive
    }

    private val retroactiveCreator: RetroactiveVUIDCreator by lazy {
        RetroactiveVUIDCreator(
            context = context,
            accessibilityService = accessibilityService,
            databaseManager = databaseManager,
            uuidCreator = uuidCreator
        )
    }

    /**
     * Relearn app with smart path selection
     *
     * @param packageName Package name to relearn
     * @return RelearnResult indicating success or error
     */
    suspend fun relearnApp(packageName: String): RelearnResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🔄 Relearn requested for $packageName")

            // Get app name for better UX
            val appName = metadataProvider.getMetadata(packageName)?.appName
                ?: packageName.substringAfterLast(".")

            // Check if VUIDs exist
            // Query generated commands for this package (these contain VUIDs)
            val existingVUIDs = databaseManager.generatedCommands.getByPackage(packageName)
            Log.d(TAG, "Found ${existingVUIDs.size} existing VUIDs for $packageName")

            if (existingVUIDs.size >= MIN_VUID_THRESHOLD) {
                // Fast path: Use retroactive creation
                Log.i(TAG, "✅ Using fast retroactive creation (${existingVUIDs.size} VUIDs exist)")

                val startTime = System.currentTimeMillis()
                when (val result = retroactiveCreator.createMissingVUIDs(packageName)) {
                    is RetroactiveResult.Success -> {
                        val duration = System.currentTimeMillis() - startTime
                        Log.i(TAG, "✅ Retroactive creation successful: ${result.existingCount} → ${result.totalCount} VUIDs in ${duration}ms")

                        RelearnResult.RetroactiveSuccess(
                            existingCount = result.existingCount,
                            newCount = result.newCount,
                            totalCount = result.totalCount,
                            durationMs = duration
                        )
                    }

                    is RetroactiveResult.Error -> {
                        Log.e(TAG, "❌ Retroactive creation failed: ${result.message}")
                        RelearnResult.Error("Failed to create missing VUIDs: ${result.message}")
                    }
                }
            } else {
                // Slow path: Full exploration needed
                Log.i(TAG, "⏳ No VUIDs found, starting full exploration for $appName")

                learnAppIntegration.triggerLearning(packageName)

                RelearnResult.FullExplorationStarted(packageName = packageName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in relearnApp", e)
            RelearnResult.Error("Unexpected error: ${e.message}")
        }
    }

    /**
     * Relearn current foreground app
     *
     * Detects the current foreground app and relearns it.
     *
     * @return RelearnResult indicating success or error
     */
    suspend fun relearnCurrentApp(): RelearnResult = withContext(Dispatchers.IO) {
        try {
            // Get current foreground package
            val foregroundPackage = learnAppIntegration.getCurrentForegroundPackage()

            if (foregroundPackage == null) {
                Log.w(TAG, "❌ Could not detect foreground app")
                return@withContext RelearnResult.Error("Could not detect current app")
            }

            Log.i(TAG, "🔍 Detected foreground app: $foregroundPackage")
            relearnApp(foregroundPackage)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in relearnCurrentApp", e)
            RelearnResult.Error("Failed to detect current app: ${e.message}")
        }
    }

    /**
     * Parse voice command and extract package name
     *
     * Supports patterns:
     * - "relearn DeviceInfo"
     * - "relearn this app"
     * - "relearn Microsoft Teams"
     *
     * @param command Voice command text
     * @return Package name or null if not found
     */
    private suspend fun parseAppNameFromCommand(command: String): String? {
        val normalized = command.trim().lowercase()

        // Pattern: "relearn this app" or "relearn current app"
        if (normalized.matches(Regex("relearn (this|current) app"))) {
            return null // Indicates "current app" - caller should use relearnCurrentApp()
        }

        // Pattern: "relearn [app name]"
        val appNameMatch = Regex("relearn (.+)", RegexOption.IGNORE_CASE).find(normalized)
        if (appNameMatch != null) {
            val appName = appNameMatch.groupValues[1].trim()

            // Try to resolve app name to package
            val packageName = metadataProvider.resolvePackageByAppName(appName)
            if (packageName != null) {
                Log.d(TAG, "Resolved '$appName' → $packageName")
                return packageName
            }

            Log.w(TAG, "Could not resolve app name: $appName")
        }

        return null
    }

    /**
     * Process voice command
     *
     * Main entry point for voice command processing.
     *
     * @param command Voice command text (e.g., "relearn DeviceInfo")
     * @return RelearnResult indicating success or error
     */
    suspend fun processCommand(command: String): RelearnResult {
        Log.d(TAG, "Processing command: $command")

        val packageName = parseAppNameFromCommand(command)

        return if (packageName == null && command.lowercase().contains("this app")) {
            // "relearn this app" - use current foreground app
            relearnCurrentApp()
        } else if (packageName != null) {
            // Specific app name provided
            relearnApp(packageName)
        } else {
            RelearnResult.Error("Could not determine which app to relearn")
        }
    }
}
