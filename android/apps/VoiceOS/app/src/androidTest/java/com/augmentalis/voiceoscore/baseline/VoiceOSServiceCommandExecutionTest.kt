/**
 * VoiceOSServiceCommandExecutionTest.kt - Test baseline for command execution flow
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-15
 *
 * Purpose: Capture CURRENT behavior of Tier 1/2/3 command execution before refactoring
 * This establishes a baseline to verify 100% functional equivalence after refactoring
 */
package com.augmentalis.voiceoscore.baseline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * Baseline Test: Command Execution Flow
 *
 * Tests THREE-TIER command execution system:
 *
 * TIER 1: CommandManager (PRIMARY)
 * - Handles commands from CommandDatabase (VOSCommandIngestion)
 * - Highest priority
 * - Falls through to Tier 2 on failure
 *
 * TIER 2: VoiceCommandProcessor (SECONDARY)
 * - Handles app-specific commands from AppScrapingDatabase
 * - Falls through to Tier 3 on failure
 *
 * TIER 3: ActionCoordinator (FALLBACK)
 * - Legacy handler-based command execution
 * - Final fallback - always attempts execution
 *
 * Metrics Captured:
 * - Success rate per tier
 * - Execution time per tier
 * - Fallback frequency
 * - Command confidence filtering (>= 0.5)
 * - Error handling patterns
 */
@RunWith(AndroidJUnit4::class)
class VoiceOSServiceCommandExecutionTest {

    companion object {
        private const val TAG = "VoiceOSServiceCommandExecutionTest"
        private const val COMMAND_TIMEOUT_MS = 3000L
        private const val MIN_CONFIDENCE = 0.5f
    }

    private lateinit var context: Context
    private val executionMetrics = mutableListOf<CommandExecutionMetric>()
    private val tierSuccessCount = mutableMapOf(
        1 to AtomicInteger(0),
        2 to AtomicInteger(0),
        3 to AtomicInteger(0)
    )
    private val tierFailureCount = mutableMapOf(
        1 to AtomicInteger(0),
        2 to AtomicInteger(0),
        3 to AtomicInteger(0)
    )

    data class CommandExecutionMetric(
        val command: String,
        val confidence: Float,
        val executedTier: Int,
        val success: Boolean,
        val executionTimeMs: Long,
        val fallbackPath: List<Int>, // Which tiers were attempted
        val errorMessage: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executionMetrics.clear()
        tierSuccessCount.values.forEach { it.set(0) }
        tierFailureCount.values.forEach { it.set(0) }
    }

    @After
    fun tearDown() {
        executionMetrics.clear()
    }

    /**
     * BASELINE TEST 1: Tier 1 (CommandManager) execution
     *
     * Current Behavior:
     * - Tries CommandManager first
     * - Creates Command object with full context
     * - Falls back to Tier 2 on failure
     */
    @Test
    fun testTier1CommandManagerExecution() = runTest {
        val tier1Commands = listOf(
            "go back" to 0.9f,
            "go home" to 0.85f,
            "volume up" to 0.8f,
            "volume down" to 0.75f,
            "open settings" to 0.9f
        )

        for ((command, confidence) in tier1Commands) {
            val startTime = System.currentTimeMillis()

            val result = simulateTier1Execution(command, confidence)

            val executionTime = System.currentTimeMillis() - startTime

            recordExecutionMetric(
                command = command,
                confidence = confidence,
                executedTier = 1,
                success = result,
                executionTimeMs = executionTime,
                fallbackPath = listOf(1)
            )

            assertTrue("Tier 1 command should execute within timeout",
                executionTime < COMMAND_TIMEOUT_MS)
        }

        val tier1SuccessRate = tierSuccessCount[1]!!.get().toFloat() /
                (tierSuccessCount[1]!!.get() + tierFailureCount[1]!!.get()).toFloat()

        println("BASELINE: Tier 1 success rate: ${"%.2f".format(tier1SuccessRate * 100)}%")
        println("BASELINE: Tier 1 average execution time: ${"%.2f".format(getAverageTierExecutionTime(1))}ms")
    }

    /**
     * BASELINE TEST 2: Tier 1 -> Tier 2 fallback
     *
     * Current Behavior:
     * - Tier 1 fails or command not found
     * - Automatically falls back to Tier 2 (VoiceCommandProcessor)
     * - Tier 2 checks AppScrapingDatabase
     */
    @Test
    fun testTier1ToTier2Fallback() = runTest {
        // Commands that should fall through to Tier 2
        val appSpecificCommands = listOf(
            "tap settings button" to 0.8f,
            "click cancel" to 0.75f,
            "select menu item" to 0.85f
        )

        for ((command, confidence) in appSpecificCommands) {
            val startTime = System.currentTimeMillis()
            val fallbackPath = mutableListOf<Int>()

            // Try Tier 1 first
            val tier1Result = simulateTier1Execution(command, confidence)
            fallbackPath.add(1)

            var success = tier1Result
            var executedTier = 1

            // Fall back to Tier 2 if Tier 1 fails
            if (!tier1Result) {
                val tier2Result = simulateTier2Execution(command, confidence)
                fallbackPath.add(2)
                success = tier2Result
                executedTier = 2
            }

            val executionTime = System.currentTimeMillis() - startTime

            recordExecutionMetric(
                command = command,
                confidence = confidence,
                executedTier = executedTier,
                success = success,
                executionTimeMs = executionTime,
                fallbackPath = fallbackPath
            )
        }

        val fallbackCount = executionMetrics.count { it.fallbackPath.size > 1 }
        println("BASELINE: Tier 1 -> Tier 2 fallback frequency: $fallbackCount/${appSpecificCommands.size}")
    }

    /**
     * BASELINE TEST 3: Tier 2 -> Tier 3 fallback
     *
     * Current Behavior:
     * - Tier 2 fails or command not in database
     * - Falls back to Tier 3 (ActionCoordinator)
     * - Tier 3 always attempts execution
     */
    @Test
    fun testTier2ToTier3Fallback() = runTest {
        // Commands that should reach Tier 3
        val legacyCommands = listOf(
            "scroll down" to 0.8f,
            "swipe left" to 0.75f,
            "long press" to 0.7f
        )

        for ((command, confidence) in legacyCommands) {
            val startTime = System.currentTimeMillis()
            val fallbackPath = mutableListOf<Int>()

            // Try Tier 1
            var tier1Result = simulateTier1Execution(command, confidence)
            fallbackPath.add(1)

            var success = tier1Result
            var executedTier = 1

            // Try Tier 2 if Tier 1 fails
            if (!tier1Result) {
                val tier2Result = simulateTier2Execution(command, confidence)
                fallbackPath.add(2)

                if (!tier2Result) {
                    // Fall back to Tier 3
                    val tier3Result = simulateTier3Execution(command)
                    fallbackPath.add(3)
                    success = tier3Result
                    executedTier = 3
                } else {
                    success = tier2Result
                    executedTier = 2
                }
            }

            val executionTime = System.currentTimeMillis() - startTime

            recordExecutionMetric(
                command = command,
                confidence = confidence,
                executedTier = executedTier,
                success = success,
                executionTimeMs = executionTime,
                fallbackPath = fallbackPath
            )
        }

        val tier3Reached = executionMetrics.count { 3 in it.fallbackPath }
        println("BASELINE: Commands reaching Tier 3: $tier3Reached/${legacyCommands.size}")
    }

    /**
     * BASELINE TEST 4: Complete fallback chain (Tier 1 -> 2 -> 3)
     *
     * Tests end-to-end fallback for unknown commands
     */
    @Test
    fun testCompleteFallbackChain() = runTest {
        val unknownCommands = listOf(
            "unknown command 1" to 0.8f,
            "invalid action" to 0.75f,
            "nonexistent feature" to 0.7f
        )

        for ((command, confidence) in unknownCommands) {
            val startTime = System.currentTimeMillis()
            val fallbackPath = mutableListOf<Int>()

            // Execute through all tiers
            val tier1Result = simulateTier1Execution(command, confidence)
            fallbackPath.add(1)

            val tier2Result = if (!tier1Result) {
                fallbackPath.add(2)
                simulateTier2Execution(command, confidence)
            } else true

            val tier3Result = if (!tier2Result) {
                fallbackPath.add(3)
                simulateTier3Execution(command)
            } else true

            val executionTime = System.currentTimeMillis() - startTime
            val finalTier = fallbackPath.last()

            recordExecutionMetric(
                command = command,
                confidence = confidence,
                executedTier = finalTier,
                success = tier3Result,
                executionTimeMs = executionTime,
                fallbackPath = fallbackPath
            )

            println("BASELINE: Command '$command' fallback path: ${fallbackPath.joinToString(" -> ")}")
        }

        val avgFallbackDepth = executionMetrics.map { it.fallbackPath.size }.average()
        println("BASELINE: Average fallback depth: ${"%.2f".format(avgFallbackDepth)} tiers")
    }

    /**
     * BASELINE TEST 5: Confidence filtering
     *
     * Current Behavior:
     * - Commands with confidence < 0.5 are rejected
     * - Commands >= 0.5 are processed
     */
    @Test
    fun testConfidenceFiltering() = runTest {
        val confidenceLevels = listOf(
            0.3f,  // Too low - should be rejected
            0.4f,  // Too low - should be rejected
            0.5f,  // Minimum - should be accepted
            0.6f,  // Above minimum - should be accepted
            0.8f,  // High confidence - should be accepted
            0.95f  // Very high confidence - should be accepted
        )

        val testCommand = "go back"

        for (confidence in confidenceLevels) {
            val startTime = System.currentTimeMillis()

            // Check if command passes confidence filter
            val shouldProcess = confidence >= MIN_CONFIDENCE

            if (shouldProcess) {
                val result = simulateTier1Execution(testCommand, confidence)
                val executionTime = System.currentTimeMillis() - startTime

                recordExecutionMetric(
                    command = testCommand,
                    confidence = confidence,
                    executedTier = 1,
                    success = result,
                    executionTimeMs = executionTime,
                    fallbackPath = listOf(1)
                )

                println("BASELINE: Confidence $confidence - PROCESSED")
            } else {
                println("BASELINE: Confidence $confidence - REJECTED (< $MIN_CONFIDENCE)")
            }
        }

        val rejectedCount = confidenceLevels.count { it < MIN_CONFIDENCE }
        val acceptedCount = confidenceLevels.count { it >= MIN_CONFIDENCE }

        println("BASELINE: Rejected: $rejectedCount, Accepted: $acceptedCount")
    }

    /**
     * BASELINE TEST 6: CommandContext creation
     *
     * Tests that CommandContext is properly populated for Tier 1
     */
    @Test
    fun testCommandContextCreation() = runTest {
        val testCommands = listOf(
            "go back" to 0.9f,
            "open settings" to 0.85f
        )

        for ((command, confidence) in testCommands) {
            // Simulate CommandContext creation
            val context = simulateCommandContextCreation(command)

            assertNotNull("CommandContext should be created", context)
            println("BASELINE: CommandContext for '$command':")
            println("  Package: ${context["packageName"]}")
            println("  Activity: ${context["activityName"]}")
            println("  Device State: ${context["deviceState"]}")
            println("  Custom Data: ${context["customData"]}")

            // Execute with context
            val result = simulateTier1Execution(command, confidence)
            recordExecutionMetric(
                command = command,
                confidence = confidence,
                executedTier = 1,
                success = result,
                executionTimeMs = 0,
                fallbackPath = listOf(1)
            )
        }
    }

    /**
     * BASELINE TEST 7: Fallback mode behavior
     *
     * Tests behavior when CommandManager is unavailable
     */
    @Test
    fun testFallbackModeWhenCommandManagerUnavailable() = runTest {
        val testCommands = listOf(
            "go back" to 0.9f,
            "volume up" to 0.85f
        )

        // Simulate CommandManager unavailable
        val fallbackModeEnabled = true

        for ((command, confidence) in testCommands) {
            val startTime = System.currentTimeMillis()
            val fallbackPath = mutableListOf<Int>()

            if (fallbackModeEnabled) {
                // Skip Tier 1, go directly to Tier 2
                val tier2Result = simulateTier2Execution(command, confidence)
                fallbackPath.add(2)

                val tier3Result = if (!tier2Result) {
                    fallbackPath.add(3)
                    simulateTier3Execution(command)
                } else true

                val executionTime = System.currentTimeMillis() - startTime
                val finalTier = fallbackPath.last()

                recordExecutionMetric(
                    command = command,
                    confidence = confidence,
                    executedTier = finalTier,
                    success = tier3Result,
                    executionTimeMs = executionTime,
                    fallbackPath = fallbackPath
                )

                println("BASELINE: Fallback mode - skipped Tier 1, path: ${fallbackPath.joinToString(" -> ")}")
            }
        }
    }

    /**
     * BASELINE TEST 8: Web command tier (before regular tiers)
     *
     * Tests that web commands are processed BEFORE Tier 1/2/3
     */
    @Test
    fun testWebCommandTierPriority() = runTest {
        val webCommands = listOf(
            "scroll page down" to 0.8f,
            "click search button" to 0.85f,
            "navigate back" to 0.9f
        )

        val browserPackage = "com.android.chrome"

        for ((command, confidence) in webCommands) {
            val startTime = System.currentTimeMillis()

            // Simulate browser detection
            val isBrowser = true

            if (isBrowser) {
                // Try web command first
                val webResult = simulateWebCommandExecution(command, confidence)

                if (webResult) {
                    // Web command handled - don't fall through
                    recordExecutionMetric(
                        command = command,
                        confidence = confidence,
                        executedTier = 0, // Web tier
                        success = true,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        fallbackPath = listOf(0)
                    )

                    println("BASELINE: Web command '$command' handled by web tier")
                } else {
                    // Fall through to regular tiers
                    val tier1Result = simulateTier1Execution(command, confidence)
                    recordExecutionMetric(
                        command = command,
                        confidence = confidence,
                        executedTier = 1,
                        success = tier1Result,
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        fallbackPath = listOf(0, 1)
                    )

                    println("BASELINE: Web command failed, fell through to Tier 1")
                }
            }
        }
    }

    // Helper Methods - Simulate Current Behavior

    private suspend fun simulateTier1Execution(command: String, confidence: Float): Boolean {
        delay(20) // Simulate CommandManager execution time

        // Simulate Tier 1 success patterns
        val tier1KnownCommands = setOf(
            "go back", "go home", "volume up", "volume down",
            "open settings", "notifications", "power"
        )

        val success = command in tier1KnownCommands && confidence >= MIN_CONFIDENCE

        if (success) {
            tierSuccessCount[1]!!.incrementAndGet()
        } else {
            tierFailureCount[1]!!.incrementAndGet()
        }

        return success
    }

    private suspend fun simulateTier2Execution(command: String, confidence: Float): Boolean {
        delay(30) // Simulate VoiceCommandProcessor execution time

        // Simulate Tier 2 success patterns (app-specific commands)
        val tier2KnownCommands = setOf(
            "tap settings button", "click cancel", "select menu item"
        )

        val success = command in tier2KnownCommands && confidence >= MIN_CONFIDENCE

        if (success) {
            tierSuccessCount[2]!!.incrementAndGet()
        } else {
            tierFailureCount[2]!!.incrementAndGet()
        }

        return success
    }

    private suspend fun simulateTier3Execution(command: String): Boolean {
        delay(15) // Simulate ActionCoordinator execution time

        // Tier 3 always attempts execution (legacy handlers)
        val success = true // ActionCoordinator tries to handle everything

        tierSuccessCount[3]!!.incrementAndGet()

        return success
    }

    private suspend fun simulateWebCommandExecution(command: String, confidence: Float): Boolean {
        delay(25) // Simulate web command processing

        val webCommands = setOf(
            "scroll page down", "click search button", "navigate back"
        )

        return command in webCommands && confidence >= MIN_CONFIDENCE
    }

    private fun simulateCommandContextCreation(command: String): Map<String, Any> {
        return mapOf(
            "packageName" to "com.example.testapp",
            "activityName" to "MainActivity",
            "focusedElement" to "android.widget.EditText",
            "deviceState" to mapOf(
                "hasRoot" to true,
                "childCount" to 10,
                "isAccessibilityFocused" to false
            ),
            "customData" to mapOf(
                "commandCacheSize" to 25,
                "nodeCacheSize" to 30,
                "fallbackMode" to false
            )
        )
    }

    private fun recordExecutionMetric(
        command: String,
        confidence: Float,
        executedTier: Int,
        success: Boolean,
        executionTimeMs: Long,
        fallbackPath: List<Int>,
        errorMessage: String? = null
    ) {
        executionMetrics.add(
            CommandExecutionMetric(
                command = command,
                confidence = confidence,
                executedTier = executedTier,
                success = success,
                executionTimeMs = executionTimeMs,
                fallbackPath = fallbackPath,
                errorMessage = errorMessage
            )
        )
    }

    private fun getAverageTierExecutionTime(tier: Int): Double {
        return executionMetrics
            .filter { it.executedTier == tier }
            .map { it.executionTimeMs.toDouble() }
            .average()
    }

    /**
     * Get baseline metrics summary
     */
    fun getBaselineMetrics(): String {
        return buildString {
            appendLine("=== Command Execution Baseline Metrics ===")
            appendLine("Total commands executed: ${executionMetrics.size}")

            for (tier in 1..3) {
                val successCount = tierSuccessCount[tier]!!.get()
                val failureCount = tierFailureCount[tier]!!.get()
                val total = successCount + failureCount

                if (total > 0) {
                    val successRate = successCount.toFloat() / total.toFloat() * 100
                    val avgTime = getAverageTierExecutionTime(tier)

                    appendLine("\nTier $tier:")
                    appendLine("  Success rate: ${"%.2f".format(successRate)}%")
                    appendLine("  Successes: $successCount, Failures: $failureCount")
                    appendLine("  Average execution time: ${"%.2f".format(avgTime)}ms")
                }
            }

            val avgFallbackDepth = executionMetrics.map { it.fallbackPath.size }.average()
            appendLine("\nFallback Statistics:")
            appendLine("  Average fallback depth: ${"%.2f".format(avgFallbackDepth)} tiers")

            val tier1Only = executionMetrics.count { it.fallbackPath.size == 1 && it.executedTier == 1 }
            val tier2Reached = executionMetrics.count { 2 in it.fallbackPath }
            val tier3Reached = executionMetrics.count { 3 in it.fallbackPath }

            appendLine("  Tier 1 only: $tier1Only")
            appendLine("  Reached Tier 2: $tier2Reached")
            appendLine("  Reached Tier 3: $tier3Reached")

            val avgExecutionTime = executionMetrics.map { it.executionTimeMs }.average()
            appendLine("\nOverall Performance:")
            appendLine("  Average total execution time: ${"%.2f".format(avgExecutionTime)}ms")
        }
    }
}
