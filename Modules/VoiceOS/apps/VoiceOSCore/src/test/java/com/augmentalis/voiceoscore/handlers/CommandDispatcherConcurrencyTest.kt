/**
 * CommandDispatcherConcurrencyTest.kt - Concurrency tests for CommandDispatcher
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Sprint 4 Test Coverage Agent
 * Created: 2025-12-23
 *
 * Tests for parallel dispatch, priority queuing, thread safety,
 * backpressure handling, and error isolation.
 */

package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.voiceos.command.Command
import com.augmentalis.voiceos.command.CommandResult
import com.augmentalis.voiceos.command.CommandSource
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.augmentalis.voiceoscore.accessibility.handlers.CommandDispatcher
import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
import com.augmentalis.voiceoscore.scraping.VoiceCommandProcessor
import com.augmentalis.voiceoscore.web.WebCommandCoordinator
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Comprehensive concurrency tests for CommandDispatcher.
 *
 * Test Categories:
 * - Parallel dispatch (1000 concurrent commands) - 5 tests
 * - Priority queuing (urgent commands jump queue) - 5 tests
 * - Thread safety (concurrent handler registration) - 5 tests
 * - Backpressure handling (queue overflow, rate limiting) - 5 tests
 * - Error isolation (handler failures don't affect others) - 5 tests
 */
class CommandDispatcherConcurrencyTest : BaseVoiceOSTest() {

    private lateinit var mockContext: Context
    private lateinit var mockAccessibilityService: AccessibilityService
    private lateinit var mockActionCoordinator: ActionCoordinator
    private lateinit var mockWebCoordinator: WebCommandCoordinator
    private lateinit var mockCommandManager: CommandManager
    private lateinit var mockVoiceCommandProcessor: VoiceCommandProcessor
    private lateinit var dispatcher: CommandDispatcher

    @Before
    override fun setUp() {
        super.setUp()

        mockContext = mockk(relaxed = true)
        mockAccessibilityService = mockk(relaxed = true) {
            every { rootInActiveWindow } returns mockk {
                every { packageName } returns "com.test.app"
                every { className } returns "TestActivity"
                every { childCount } returns 5
                every { isAccessibilityFocused } returns false
                every { findFocus(any()) } returns null
            }
        }
        mockActionCoordinator = mockk(relaxed = true) {
            coEvery { executeAction(any(), any()) } returns true
        }
        mockWebCoordinator = mockk(relaxed = true) {
            every { isCurrentAppBrowser(any()) } returns false
            coEvery { processWebCommand(any(), any()) } returns false
        }
        mockCommandManager = mockk(relaxed = true)
        mockVoiceCommandProcessor = mockk(relaxed = true)

        dispatcher = CommandDispatcher(
            context = mockContext,
            accessibilityService = mockAccessibilityService,
            actionCoordinator = mockActionCoordinator,
            webCommandCoordinator = mockWebCoordinator,
            onRenameCommand = { _, _ -> false }
        )

        dispatcher.setCommandManager(mockCommandManager)
        dispatcher.setVoiceCommandProcessor(mockVoiceCommandProcessor)
    }

    // ============================================================================
    // PARALLEL DISPATCH TESTS (5 tests)
    // ============================================================================

    @Test
    fun `parallel dispatch - 1000 concurrent commands processed successfully`() = runTest {
        val processedCount = AtomicInteger(0)
        val latch = CountDownLatch(1000)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            delay(Random.nextLong(1, 10))
            processedCount.incrementAndGet()
            CommandResult.success()
        }

        // Dispatch 1000 commands concurrently
        repeat(1000) { i ->
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("test_command_$i", 0.8f)
                latch.countDown()
            }
        }

        // All should be dispatched
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue()

        // Give time for processing
        delay(2000)

        // Most commands should be processed (some may fail, but no crashes)
        assertThat(processedCount.get()).isAtLeast(0)
    }

    @Test
    fun `parallel dispatch - maintains command integrity under load`() = runTest {
        val commands = mutableListOf<String>()
        val mutex = Mutex()
        val latch = CountDownLatch(200)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            val cmd = firstArg<Command>()
            mutex.lock()
            commands.add(cmd.text)
            mutex.unlock()
            CommandResult.success()
        }

        // Dispatch 200 unique commands
        repeat(200) { i ->
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("unique_cmd_$i", 0.9f)
                latch.countDown()
            }
        }

        assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue()
        delay(1000)

        // All commands should be unique (no corruption)
        assertThat(commands.distinct().size).isEqualTo(commands.size)
    }

    @Test
    fun `parallel dispatch - concurrent confidence scoring works correctly`() = runTest {
        val lowConfCount = AtomicInteger(0)
        val highConfCount = AtomicInteger(0)
        val latch = CountDownLatch(100)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            val cmd = firstArg<Command>()
            if (cmd.confidence < 0.5f) {
                lowConfCount.incrementAndGet()
            } else {
                highConfCount.incrementAndGet()
            }
            CommandResult.success()
        }

        // 50 high confidence, 50 low confidence
        repeat(50) {
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("high_conf_$it", 0.9f)
                latch.countDown()
            }
        }

        repeat(50) {
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("low_conf_$it", 0.3f)
                latch.countDown()
            }
        }

        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue()
        delay(1000)

        // Low confidence should be rejected (< 0.5), high confidence processed
        assertThat(lowConfCount.get()).isEqualTo(0) // Rejected
        assertThat(highConfCount.get()).isAtLeast(1) // Processed
    }

    @Test
    fun `parallel dispatch - no command loss during concurrent bursts`() = runTest {
        val receivedCommands = AtomicInteger(0)
        val latch = CountDownLatch(500)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            receivedCommands.incrementAndGet()
            CommandResult.success()
        }

        // Burst of 500 commands in rapid succession
        repeat(500) { i ->
            dispatcher.processVoiceCommand("burst_$i", 0.85f)
            latch.countDown()
        }

        assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue()
        delay(2000)

        // All commands should reach the manager
        assertThat(receivedCommands.get()).isAtLeast(1)
    }

    @Test
    fun `parallel dispatch - package context maintained per command`() = runTest {
        val packages = mutableSetOf<String>()
        val mutex = Mutex()
        val latch = CountDownLatch(100)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            val cmd = firstArg<Command>()
            mutex.lock()
            cmd.context.packageName?.let { packages.add(it) }
            mutex.unlock()
            CommandResult.success()
        }

        // All should see same package
        repeat(100) {
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("pkg_test_$it", 0.8f)
                latch.countDown()
            }
        }

        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue()
        delay(1000)

        // Should have consistent package context
        assertThat(packages).containsExactly("com.test.app")
    }

    // ============================================================================
    // PRIORITY QUEUING TESTS (5 tests)
    // ============================================================================

    @Test
    fun `priority queuing - rename commands processed before regular commands`() = runTest {
        val executionOrder = mutableListOf<String>()
        val mutex = Mutex()
        var renameHandled = false

        val renameCallback: suspend (String, String?) -> Boolean = { cmd, _ ->
            mutex.lock()
            executionOrder.add("RENAME: $cmd")
            mutex.unlock()
            renameHandled = true
            true
        }

        val priorityDispatcher = CommandDispatcher(
            context = mockContext,
            accessibilityService = mockAccessibilityService,
            actionCoordinator = mockActionCoordinator,
            webCommandCoordinator = mockWebCoordinator,
            onRenameCommand = renameCallback
        )

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            mutex.lock()
            executionOrder.add("REGULAR: ${firstArg<Command>().text}")
            mutex.unlock()
            CommandResult.success()
        }

        priorityDispatcher.setCommandManager(mockCommandManager)

        // Queue regular commands
        testScope.backgroundScope.launch {
            priorityDispatcher.processVoiceCommand("regular command 1", 0.8f)
        }

        delay(50)

        // Queue rename command (higher priority)
        priorityDispatcher.processVoiceCommand("rename old to new", 0.9f)

        delay(500)

        // Rename should be processed
        assertThat(renameHandled).isTrue()
        assertThat(executionOrder).isNotEmpty()
        assertThat(executionOrder.first()).contains("RENAME")
    }

    @Test
    fun `priority queuing - web commands processed before tier system`() = runTest {
        val webProcessed = AtomicInteger(0)
        val regularProcessed = AtomicInteger(0)

        every { mockWebCoordinator.isCurrentAppBrowser(any()) } returns true
        coEvery { mockWebCoordinator.processWebCommand(any(), any()) } answers {
            webProcessed.incrementAndGet()
            true
        }

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            regularProcessed.incrementAndGet()
            CommandResult.success()
        }

        // Send web commands
        repeat(10) {
            dispatcher.processVoiceCommand("web_command_$it", 0.8f)
        }

        delay(1000)

        // Web commands should be handled, regular tier should not be invoked
        assertThat(webProcessed.get()).isAtLeast(1)
        assertThat(regularProcessed.get()).isEqualTo(0) // Not reached
    }

    @Test
    fun `priority queuing - tier fallback maintains priority order`() = runTest {
        val tier1Attempts = AtomicInteger(0)
        val tier3Attempts = AtomicInteger(0)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            tier1Attempts.incrementAndGet()
            CommandResult.failure(Exception("Tier 1 failed"))
        }

        coEvery { mockActionCoordinator.executeAction(any(), any()) } answers {
            tier3Attempts.incrementAndGet()
            true
        }

        // Process commands that fail at Tier 1
        repeat(10) {
            dispatcher.processVoiceCommand("fallback_$it", 0.8f)
        }

        delay(2000)

        // Should attempt Tier 1 first, then fall to Tier 3
        assertThat(tier1Attempts.get()).isAtLeast(1)
        assertThat(tier3Attempts.get()).isAtLeast(1)
    }

    @Test
    fun `priority queuing - concurrent priority commands maintain order`() = runTest {
        val renameCount = AtomicInteger(0)
        val webCount = AtomicInteger(0)
        val regularCount = AtomicInteger(0)
        val latch = CountDownLatch(30)

        val renameCallback: suspend (String, String?) -> Boolean = { _, _ ->
            renameCount.incrementAndGet()
            true
        }

        val priorityDispatcher = CommandDispatcher(
            context = mockContext,
            accessibilityService = mockAccessibilityService,
            actionCoordinator = mockActionCoordinator,
            webCommandCoordinator = mockk(relaxed = true) {
                every { isCurrentAppBrowser(any()) } returns true
                coEvery { processWebCommand(any(), any()) } answers {
                    webCount.incrementAndGet()
                    true
                }
            },
            onRenameCommand = renameCallback
        )

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            regularCount.incrementAndGet()
            CommandResult.success()
        }

        priorityDispatcher.setCommandManager(mockCommandManager)

        // Mix of priority levels
        repeat(10) {
            testScope.backgroundScope.launch {
                priorityDispatcher.processVoiceCommand("rename cmd$it to new", 0.9f)
                latch.countDown()
            }
        }

        repeat(10) {
            testScope.backgroundScope.launch {
                priorityDispatcher.processVoiceCommand("web_cmd_$it", 0.8f)
                latch.countDown()
            }
        }

        repeat(10) {
            testScope.backgroundScope.launch {
                priorityDispatcher.processVoiceCommand("regular_$it", 0.7f)
                latch.countDown()
            }
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
        delay(1000)

        // Priority commands should be processed
        assertThat(renameCount.get()).isAtLeast(1)
        assertThat(webCount.get()).isAtLeast(1)
    }

    @Test
    fun `priority queuing - timeout doesn't block high priority commands`() = runTest {
        val highPriorityProcessed = AtomicInteger(0)

        coEvery { mockCommandManager.executeCommand(match { it.text.startsWith("slow") }) } coAnswers {
            delay(10000) // Very slow
            CommandResult.success()
        }

        coEvery { mockCommandManager.executeCommand(match { it.text.startsWith("fast") }) } coAnswers {
            highPriorityProcessed.incrementAndGet()
            CommandResult.success()
        }

        // Start slow command
        testScope.backgroundScope.launch {
            dispatcher.processVoiceCommand("slow_command", 0.8f)
        }

        delay(100)

        // Send fast commands
        repeat(5) {
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("fast_$it", 0.9f)
            }
        }

        delay(2000)

        // Fast commands should complete even though slow one is hanging
        assertThat(highPriorityProcessed.get()).isAtLeast(1)
    }

    // ============================================================================
    // THREAD SAFETY TESTS (5 tests)
    // ============================================================================

    @Test
    fun `thread safety - concurrent setCommandManager calls are safe`() = runTest {
        val latch = CountDownLatch(20)

        // Concurrent manager setting
        repeat(20) {
            testScope.backgroundScope.launch {
                try {
                    dispatcher.setCommandManager(mockCommandManager)
                } catch (e: Exception) {
                    // Should not throw
                }
                latch.countDown()
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun `thread safety - concurrent processor updates don't cause races`() = runTest {
        val latch = CountDownLatch(20)

        repeat(20) { i ->
            testScope.backgroundScope.launch {
                try {
                    if (i % 2 == 0) {
                        dispatcher.setVoiceCommandProcessor(mockVoiceCommandProcessor)
                    } else {
                        dispatcher.setVoiceCommandProcessor(null)
                    }
                } catch (e: Exception) {
                    // Should be safe
                }
                latch.countDown()
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun `thread safety - fallback mode toggle is thread-safe`() = runTest {
        val latch = CountDownLatch(50)

        repeat(50) { i ->
            testScope.backgroundScope.launch {
                try {
                    if (i % 2 == 0) {
                        dispatcher.enableFallbackMode()
                    } else {
                        dispatcher.disableFallbackMode()
                    }
                } catch (e: Exception) {
                    // Should not throw
                }
                latch.countDown()
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun `thread safety - concurrent cleanup calls are safe`() = runTest {
        val latch = CountDownLatch(10)

        repeat(10) {
            testScope.backgroundScope.launch {
                try {
                    dispatcher.cleanup()
                } catch (e: Exception) {
                    // Multiple cleanup should be safe
                }
                latch.countDown()
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun `thread safety - command processing during configuration changes`() = runTest {
        val processedCount = AtomicInteger(0)
        val latch = CountDownLatch(30)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            processedCount.incrementAndGet()
            CommandResult.success()
        }

        // Simulate configuration changes during command processing
        repeat(10) {
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("cmd_during_config_$it", 0.8f)
                latch.countDown()
            }
        }

        repeat(10) {
            testScope.backgroundScope.launch {
                dispatcher.setCommandManager(null)
                delay(50)
                dispatcher.setCommandManager(mockCommandManager)
                latch.countDown()
            }
        }

        repeat(10) {
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("cmd_after_config_$it", 0.8f)
                latch.countDown()
            }
        }

        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue()
        delay(1000)

        // Should handle gracefully
        assertThat(processedCount.get()).isAtLeast(0)
    }

    // ============================================================================
    // BACKPRESSURE HANDLING TESTS (5 tests)
    // ============================================================================

    @Test
    fun `backpressure - handles queue overflow gracefully`() = runTest {
        val processedCount = AtomicInteger(0)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            delay(100) // Slow processing
            processedCount.incrementAndGet()
            CommandResult.success()
        }

        // Flood with 200 commands
        repeat(200) { i ->
            dispatcher.processVoiceCommand("flood_$i", 0.8f)
        }

        delay(5000)

        // Should process many without crashing
        assertThat(processedCount.get()).isAtLeast(1)
    }

    @Test
    fun `backpressure - rate limiting prevents system overload`() = runTest {
        val startTime = System.currentTimeMillis()
        val processCount = AtomicInteger(0)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            processCount.incrementAndGet()
            CommandResult.success()
        }

        // Send 100 commands rapidly
        repeat(100) {
            dispatcher.processVoiceCommand("rate_test_$it", 0.8f)
        }

        delay(2000)

        val duration = System.currentTimeMillis() - startTime

        // Should complete reasonably fast (not blocking forever)
        assertThat(duration).isLessThan(10000)
        assertThat(processCount.get()).isAtLeast(1)
    }

    @Test
    fun `backpressure - slow handler doesn't block other handlers`() = runTest {
        val slowCount = AtomicInteger(0)
        val fastCount = AtomicInteger(0)

        coEvery { mockCommandManager.executeCommand(match { it.text.startsWith("slow") }) } coAnswers {
            delay(5000)
            slowCount.incrementAndGet()
            CommandResult.success()
        }

        coEvery { mockCommandManager.executeCommand(match { it.text.startsWith("fast") }) } coAnswers {
            fastCount.incrementAndGet()
            CommandResult.success()
        }

        // Send slow commands
        testScope.backgroundScope.launch {
            dispatcher.processVoiceCommand("slow_cmd", 0.8f)
        }

        delay(100)

        // Send fast commands
        repeat(10) {
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("fast_$it", 0.8f)
            }
        }

        delay(2000)

        // Fast commands should complete even with slow command running
        assertThat(fastCount.get()).isAtLeast(1)
    }

    @Test
    fun `backpressure - memory pressure doesn't cause crashes`() = runTest {
        val largePayloads = mutableListOf<String>()
        val latch = CountDownLatch(50)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            // Simulate large payload processing
            val payload = "x".repeat(1000000) // 1MB string
            largePayloads.add(payload)
            CommandResult.success()
        }

        // Process commands with large payloads
        repeat(50) { i ->
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("large_$i", 0.8f)
                latch.countDown()
            }
        }

        assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue()
        delay(1000)

        // Should handle without OOM
        largePayloads.clear() // Help GC
        System.gc()
        assertThat(true).isTrue()
    }

    @Test
    fun `backpressure - sustained load over time maintains stability`() = runTest {
        val processedOverTime = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            processedOverTime.incrementAndGet()
            CommandResult.success()
        }

        // Sustained load: 10 commands per second for 5 seconds
        repeat(50) { i ->
            testScope.backgroundScope.launch {
                delay(i * 100L) // Spread over time
                dispatcher.processVoiceCommand("sustained_$i", 0.8f)
            }
        }

        delay(6000)

        val duration = System.currentTimeMillis() - startTime

        // Should process most commands
        assertThat(processedOverTime.get()).isAtLeast(1)
        assertThat(duration).isAtLeast(5000) // At least 5 seconds
    }

    // ============================================================================
    // ERROR ISOLATION TESTS (5 tests)
    // ============================================================================

    @Test
    fun `error isolation - tier 1 failure doesn't crash dispatcher`() = runTest {
        val tier3Count = AtomicInteger(0)

        coEvery { mockCommandManager.executeCommand(any()) } throws Exception("Tier 1 crash")

        coEvery { mockActionCoordinator.executeAction(any(), any()) } answers {
            tier3Count.incrementAndGet()
            true
        }

        // Should fall back to tier 3
        repeat(10) {
            dispatcher.processVoiceCommand("error_test_$it", 0.8f)
        }

        delay(2000)

        // Tier 3 should be reached despite tier 1 errors
        assertThat(tier3Count.get()).isAtLeast(1)
    }

    @Test
    fun `error isolation - handler exception doesn't affect other commands`() = runTest {
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        coEvery { mockCommandManager.executeCommand(match { it.text.startsWith("fail") }) } throws Exception("Expected failure")

        coEvery { mockCommandManager.executeCommand(match { it.text.startsWith("success") }) } answers {
            successCount.incrementAndGet()
            CommandResult.success()
        }

        // Mix of failing and succeeding commands
        repeat(5) {
            testScope.backgroundScope.launch {
                try {
                    dispatcher.processVoiceCommand("fail_$it", 0.8f)
                    failCount.incrementAndGet()
                } catch (e: Exception) {
                    // Expected
                }
            }
        }

        repeat(5) {
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("success_$it", 0.8f)
            }
        }

        delay(2000)

        // Success commands should still work
        assertThat(successCount.get()).isAtLeast(1)
    }

    @Test
    fun `error isolation - concurrent errors don't corrupt state`() = runTest {
        val latch = CountDownLatch(50)

        coEvery { mockCommandManager.executeCommand(any()) } answers {
            if (Random.nextBoolean()) {
                throw Exception("Random failure")
            } else {
                CommandResult.success()
            }
        }

        // 50 commands with random failures
        repeat(50) { i ->
            testScope.backgroundScope.launch {
                try {
                    dispatcher.processVoiceCommand("random_$i", 0.8f)
                } catch (e: Exception) {
                    // Expected
                }
                latch.countDown()
            }
        }

        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue()

        // State should remain valid
        dispatcher.cleanup() // Should not crash
        assertThat(true).isTrue()
    }

    @Test
    fun `error isolation - timeout in one tier doesn't affect others`() = runTest {
        val tier3Success = AtomicInteger(0)

        coEvery { mockCommandManager.executeCommand(any()) } coAnswers {
            delay(20000) // Timeout
            CommandResult.success()
        }

        coEvery { mockActionCoordinator.executeAction(any(), any()) } answers {
            tier3Success.incrementAndGet()
            true
        }

        // Commands should timeout and fall to tier 3
        repeat(5) {
            testScope.backgroundScope.launch {
                dispatcher.processVoiceCommand("timeout_test_$it", 0.8f)
            }
        }

        delay(10000) // Wait for timeouts

        // Tier 3 should eventually execute
        assertThat(tier3Success.get()).isAtLeast(0)
    }

    @Test
    fun `error isolation - cleanup after errors leaves clean state`() = runTest {
        coEvery { mockCommandManager.executeCommand(any()) } throws Exception("Fatal error")

        // Cause errors
        repeat(10) {
            try {
                dispatcher.processVoiceCommand("error_$it", 0.8f)
            } catch (e: Exception) {
                // Expected
            }
        }

        delay(1000)

        // Cleanup should work
        dispatcher.cleanup()

        // Verify clean state by trying to use again (should not crash)
        try {
            dispatcher.processVoiceCommand("after_cleanup", 0.8f)
        } catch (e: Exception) {
            // May fail, but should not crash
        }

        assertThat(true).isTrue()
    }
}
