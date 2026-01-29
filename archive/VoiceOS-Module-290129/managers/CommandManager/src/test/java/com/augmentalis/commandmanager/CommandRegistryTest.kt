/**
 * CommandRegistryTest.kt
 *
 * Created: 2025-10-10 19:08 PDT
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Unit tests for CommandRegistry infrastructure
 * Coverage: Registration, routing, thread safety, error handling
 * Location: CommandManager test suite
 *
 * Changelog:
 * - v1.0.0 (2025-10-10): Initial test suite for CommandRegistry
 */

package com.augmentalis.voiceoscore

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CommandRegistry.
 *
 * Test Coverage:
 * - Handler registration and unregistration
 * - Command routing logic
 * - First-match routing behavior
 * - Error handling and exception safety
 * - Thread safety (concurrent access)
 * - Edge cases (blank commands, no handlers, etc.)
 */
class CommandRegistryTest {

    // Mock handler for testing
    private class MockCommandHandler(
        override val moduleId: String,
        override val supportedCommands: List<String>,
        private val canHandleImpl: (String) -> Boolean,
        private val handleCommandImpl: suspend (String) -> Boolean
    ) : CommandHandler {
        var canHandleCallCount = 0
        var handleCommandCallCount = 0

        override fun canHandle(command: String): Boolean {
            canHandleCallCount++
            return canHandleImpl(command)
        }

        override suspend fun handleCommand(command: String): Boolean {
            handleCommandCallCount++
            return handleCommandImpl(command)
        }
    }

    @Before
    fun setUp() {
        // Clear registry before each test
        CommandRegistry.clearAllHandlers()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        CommandRegistry.clearAllHandlers()
    }

    // ===== Registration Tests =====

    @Test
    fun `register handler successfully`() {
        val handler = MockCommandHandler(
            moduleId = "test_module",
            supportedCommands = listOf("test command"),
            canHandleImpl = { true },
            handleCommandImpl = { true }
        )

        CommandRegistry.registerHandler("test_module", handler)

        assertEquals(1, CommandRegistry.getHandlerCount())
        assertTrue(CommandRegistry.isHandlerRegistered("test_module"))
        assertSame(handler, CommandRegistry.getHandler("test_module"))
    }

    @Test
    fun `register multiple handlers`() {
        val handler1 = MockCommandHandler("module1", listOf("cmd1"), { true }, { true })
        val handler2 = MockCommandHandler("module2", listOf("cmd2"), { true }, { true })
        val handler3 = MockCommandHandler("module3", listOf("cmd3"), { true }, { true })

        CommandRegistry.registerHandler("module1", handler1)
        CommandRegistry.registerHandler("module2", handler2)
        CommandRegistry.registerHandler("module3", handler3)

        assertEquals(3, CommandRegistry.getHandlerCount())
        assertTrue(CommandRegistry.isHandlerRegistered("module1"))
        assertTrue(CommandRegistry.isHandlerRegistered("module2"))
        assertTrue(CommandRegistry.isHandlerRegistered("module3"))
    }

    @Test
    fun `register handler with same moduleId overwrites previous`() {
        val handler1 = MockCommandHandler("test", listOf("cmd1"), { true }, { true })
        val handler2 = MockCommandHandler("test", listOf("cmd2"), { true }, { true })

        CommandRegistry.registerHandler("test", handler1)
        CommandRegistry.registerHandler("test", handler2)

        assertEquals(1, CommandRegistry.getHandlerCount())
        assertSame(handler2, CommandRegistry.getHandler("test"))
        assertNotSame(handler1, CommandRegistry.getHandler("test"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `register handler with blank moduleId throws exception`() {
        val handler = MockCommandHandler("", listOf("cmd"), { true }, { true })
        CommandRegistry.registerHandler("", handler)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `register handler with whitespace moduleId throws exception`() {
        val handler = MockCommandHandler("  ", listOf("cmd"), { true }, { true })
        CommandRegistry.registerHandler("  ", handler)
    }

    // ===== Unregistration Tests =====

    @Test
    fun `unregister handler successfully`() {
        val handler = MockCommandHandler("test", listOf("cmd"), { true }, { true })

        CommandRegistry.registerHandler("test", handler)
        assertEquals(1, CommandRegistry.getHandlerCount())

        CommandRegistry.unregisterHandler("test")
        assertEquals(0, CommandRegistry.getHandlerCount())
        assertFalse(CommandRegistry.isHandlerRegistered("test"))
        assertNull(CommandRegistry.getHandler("test"))
    }

    @Test
    fun `unregister non-existent handler is safe`() {
        // Should not throw exception
        CommandRegistry.unregisterHandler("non_existent")
        assertEquals(0, CommandRegistry.getHandlerCount())
    }

    @Test
    fun `unregister one of multiple handlers`() {
        val handler1 = MockCommandHandler("module1", listOf("cmd1"), { true }, { true })
        val handler2 = MockCommandHandler("module2", listOf("cmd2"), { true }, { true })
        val handler3 = MockCommandHandler("module3", listOf("cmd3"), { true }, { true })

        CommandRegistry.registerHandler("module1", handler1)
        CommandRegistry.registerHandler("module2", handler2)
        CommandRegistry.registerHandler("module3", handler3)

        CommandRegistry.unregisterHandler("module2")

        assertEquals(2, CommandRegistry.getHandlerCount())
        assertTrue(CommandRegistry.isHandlerRegistered("module1"))
        assertFalse(CommandRegistry.isHandlerRegistered("module2"))
        assertTrue(CommandRegistry.isHandlerRegistered("module3"))
    }

    @Test
    fun `clear all handlers`() {
        val handler1 = MockCommandHandler("module1", listOf("cmd1"), { true }, { true })
        val handler2 = MockCommandHandler("module2", listOf("cmd2"), { true }, { true })

        CommandRegistry.registerHandler("module1", handler1)
        CommandRegistry.registerHandler("module2", handler2)
        assertEquals(2, CommandRegistry.getHandlerCount())

        CommandRegistry.clearAllHandlers()
        assertEquals(0, CommandRegistry.getHandlerCount())
    }

    // ===== Routing Tests =====

    @Test
    fun `route command to matching handler`() = runBlocking {
        val handler = MockCommandHandler(
            moduleId = "test",
            supportedCommands = listOf("test command"),
            canHandleImpl = { it == "test command" },
            handleCommandImpl = { true }
        )

        CommandRegistry.registerHandler("test", handler)

        val result = CommandRegistry.routeCommand("test command")

        assertTrue(result)
        assertEquals(1, handler.canHandleCallCount)
        assertEquals(1, handler.handleCommandCallCount)
    }

    @Test
    fun `route command with normalization`() = runBlocking {
        val handler = MockCommandHandler(
            moduleId = "test",
            supportedCommands = listOf("test command"),
            canHandleImpl = { it == "test command" },
            handleCommandImpl = { true }
        )

        CommandRegistry.registerHandler("test", handler)

        // Command should be normalized (lowercase, trimmed)
        val result = CommandRegistry.routeCommand("  TEST COMMAND  ")

        assertTrue(result)
        assertEquals(1, handler.canHandleCallCount)
        assertEquals(1, handler.handleCommandCallCount)
    }

    @Test
    fun `route command to first matching handler`() = runBlocking {
        val handler1 = MockCommandHandler(
            moduleId = "handler1",
            supportedCommands = listOf("cmd"),
            canHandleImpl = { it == "cmd" },
            handleCommandImpl = { true }
        )
        val handler2 = MockCommandHandler(
            moduleId = "handler2",
            supportedCommands = listOf("cmd"),
            canHandleImpl = { it == "cmd" },
            handleCommandImpl = { true }
        )

        CommandRegistry.registerHandler("handler1", handler1)
        CommandRegistry.registerHandler("handler2", handler2)

        val result = CommandRegistry.routeCommand("cmd")

        assertTrue(result)
        // Only first handler should be checked and executed
        assertTrue(handler1.canHandleCallCount > 0)
        assertTrue(handler1.handleCommandCallCount > 0)
    }

    @Test
    fun `route command returns false when no handler found`() = runBlocking {
        val handler = MockCommandHandler(
            moduleId = "test",
            supportedCommands = listOf("valid command"),
            canHandleImpl = { it == "valid command" },
            handleCommandImpl = { true }
        )

        CommandRegistry.registerHandler("test", handler)

        val result = CommandRegistry.routeCommand("unknown command")

        assertFalse(result)
        assertEquals(1, handler.canHandleCallCount)
        assertEquals(0, handler.handleCommandCallCount) // Not executed
    }

    @Test
    fun `route command returns false when handler execution fails`() = runBlocking {
        val handler = MockCommandHandler(
            moduleId = "test",
            supportedCommands = listOf("cmd"),
            canHandleImpl = { true },
            handleCommandImpl = { false } // Execution fails
        )

        CommandRegistry.registerHandler("test", handler)

        val result = CommandRegistry.routeCommand("cmd")

        assertFalse(result)
        assertEquals(1, handler.canHandleCallCount)
        assertEquals(1, handler.handleCommandCallCount)
    }

    @Test
    fun `route blank command returns false`() = runBlocking {
        val handler = MockCommandHandler("test", listOf("cmd"), { true }, { true })
        CommandRegistry.registerHandler("test", handler)

        assertFalse(CommandRegistry.routeCommand(""))
        assertFalse(CommandRegistry.routeCommand("  "))

        assertEquals(0, handler.canHandleCallCount)
        assertEquals(0, handler.handleCommandCallCount)
    }

    @Test
    fun `route command with no handlers registered returns false`() = runBlocking {
        val result = CommandRegistry.routeCommand("any command")
        assertFalse(result)
    }

    // ===== Error Handling Tests =====

    @Test
    fun `handler exception during canHandle is caught and logged`() = runBlocking {
        val throwingHandler = MockCommandHandler(
            moduleId = "throwing",
            supportedCommands = listOf("cmd"),
            canHandleImpl = { throw RuntimeException("canHandle error") },
            handleCommandImpl = { true }
        )

        CommandRegistry.registerHandler("throwing", throwingHandler)

        // Should not throw, should return false
        val result = CommandRegistry.routeCommand("cmd")

        assertFalse(result)
    }

    @Test
    fun `handler exception during handleCommand is caught and logged`() = runBlocking {
        val throwingHandler = MockCommandHandler(
            moduleId = "throwing",
            supportedCommands = listOf("cmd"),
            canHandleImpl = { true },
            handleCommandImpl = { throw RuntimeException("handleCommand error") }
        )

        CommandRegistry.registerHandler("throwing", throwingHandler)

        // Should not throw, should return false
        val result = CommandRegistry.routeCommand("cmd")

        assertFalse(result)
        assertEquals(1, throwingHandler.canHandleCallCount)
        assertEquals(1, throwingHandler.handleCommandCallCount)
    }

    @Test
    fun `handler exception continues to next handler`() = runBlocking {
        val throwingHandler = MockCommandHandler(
            moduleId = "throwing",
            supportedCommands = listOf("cmd"),
            canHandleImpl = { throw RuntimeException("Error") },
            handleCommandImpl = { true }
        )
        val workingHandler = MockCommandHandler(
            moduleId = "working",
            supportedCommands = listOf("cmd"),
            canHandleImpl = { true },
            handleCommandImpl = { true }
        )

        // Register throwing handler first
        CommandRegistry.registerHandler("throwing", throwingHandler)
        CommandRegistry.registerHandler("working", workingHandler)

        val result = CommandRegistry.routeCommand("cmd")

        // Should succeed with working handler
        assertTrue(result)
        assertEquals(1, workingHandler.canHandleCallCount)
        assertEquals(1, workingHandler.handleCommandCallCount)
    }

    // ===== Query Tests =====

    @Test
    fun `getAllHandlers returns all registered handlers`() {
        val handler1 = MockCommandHandler("module1", listOf("cmd1"), { true }, { true })
        val handler2 = MockCommandHandler("module2", listOf("cmd2"), { true }, { true })

        CommandRegistry.registerHandler("module1", handler1)
        CommandRegistry.registerHandler("module2", handler2)

        val allHandlers = CommandRegistry.getAllHandlers()

        assertEquals(2, allHandlers.size)
        assertTrue(allHandlers.contains(handler1))
        assertTrue(allHandlers.contains(handler2))
    }

    @Test
    fun `getAllSupportedCommands aggregates commands from all handlers`() {
        val handler1 = MockCommandHandler("module1", listOf("cmd1", "cmd2"), { true }, { true })
        val handler2 = MockCommandHandler("module2", listOf("cmd3", "cmd4"), { true }, { true })

        CommandRegistry.registerHandler("module1", handler1)
        CommandRegistry.registerHandler("module2", handler2)

        val allCommands = CommandRegistry.getAllSupportedCommands()

        assertEquals(4, allCommands.size)
        assertTrue(allCommands.containsAll(listOf("cmd1", "cmd2", "cmd3", "cmd4")))
    }

    @Test
    fun `getHandler returns correct handler`() {
        val handler = MockCommandHandler("test", listOf("cmd"), { true }, { true })
        CommandRegistry.registerHandler("test", handler)

        val retrieved = CommandRegistry.getHandler("test")

        assertSame(handler, retrieved)
    }

    @Test
    fun `getHandler returns null for non-existent handler`() {
        val retrieved = CommandRegistry.getHandler("non_existent")
        assertNull(retrieved)
    }

    @Test
    fun `isHandlerRegistered returns correct status`() {
        val handler = MockCommandHandler("test", listOf("cmd"), { true }, { true })

        assertFalse(CommandRegistry.isHandlerRegistered("test"))

        CommandRegistry.registerHandler("test", handler)
        assertTrue(CommandRegistry.isHandlerRegistered("test"))

        CommandRegistry.unregisterHandler("test")
        assertFalse(CommandRegistry.isHandlerRegistered("test"))
    }

    // ===== Thread Safety Tests (Concurrent Access) =====

    @Test
    fun `concurrent registration is thread-safe`() {
        val threads = (1..10).map { i ->
            Thread {
                val handler = MockCommandHandler("module$i", listOf("cmd$i"), { true }, { true })
                CommandRegistry.registerHandler("module$i", handler)
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(10, CommandRegistry.getHandlerCount())
    }

    @Test
    fun `concurrent routing is thread-safe`() = runBlocking {
        val handler = MockCommandHandler(
            moduleId = "test",
            supportedCommands = listOf("cmd"),
            canHandleImpl = { true },
            handleCommandImpl = {
                Thread.sleep(10) // Simulate work
                true
            }
        )

        CommandRegistry.registerHandler("test", handler)

        val threads = (1..20).map {
            Thread {
                runBlocking {
                    CommandRegistry.routeCommand("cmd")
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Should have called handleCommand 20 times
        assertEquals(20, handler.handleCommandCallCount)
    }

    @Test
    fun `concurrent registration and routing is thread-safe`() {
        val handler = MockCommandHandler("test", listOf("cmd"), { true }, { true })

        val registrationThread = Thread {
            CommandRegistry.registerHandler("test", handler)
        }

        val routingThreads = (1..10).map {
            Thread {
                runBlocking {
                    CommandRegistry.routeCommand("cmd")
                }
            }
        }

        registrationThread.start()
        routingThreads.forEach { it.start() }

        registrationThread.join()
        routingThreads.forEach { it.join() }

        // Should not throw any exceptions
        assertTrue(CommandRegistry.isHandlerRegistered("test"))
    }
}
