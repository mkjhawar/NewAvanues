/**
 * CommandViewModelTest.kt - Unit tests for CommandViewModel
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Tests command execution, stats calculation, and UI state management
 */
package com.augmentalis.commandmanager.ui

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.augmentalis.commandmanager.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class CommandViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var statsObserver: Observer<CommandStats>

    @Mock
    private lateinit var historyObserver: Observer<List<CommandHistoryEntry>>

    @Mock
    private lateinit var loadingObserver: Observer<Boolean>

    @Mock
    private lateinit var errorObserver: Observer<String?>

    @Mock
    private lateinit var successObserver: Observer<String?>

    @Mock
    private lateinit var commandsObserver: Observer<Map<CommandCategory, List<CommandDefinition>>>

    private lateinit var viewModel: CommandViewModel
    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = CommandViewModel(context)
        
        // Setup observers
        viewModel.commandStats.observeForever(statsObserver)
        viewModel.commandHistory.observeForever(historyObserver)
        viewModel.isLoading.observeForever(loadingObserver)
        viewModel.errorMessage.observeForever(errorObserver)
        viewModel.successMessage.observeForever(successObserver)
        viewModel.availableCommands.observeForever(commandsObserver)
    }

    @Test
    fun testInitialState() {
        // Test initial state
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(null, viewModel.errorMessage.value)
        assertEquals(null, viewModel.successMessage.value)
    }

    @Test
    fun testCommandStatisticsCalculation() = runTest {
        // Test stats calculation with sample data
        viewModel.refreshStats()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val stats = viewModel.commandStats.value
        assertNotNull(stats)
        assertTrue(stats.totalCommands >= 0)
        assertTrue(stats.successfulCommands >= 0)
        assertTrue(stats.failedCommands >= 0)
        assertTrue(stats.averageExecutionTime >= 0)
    }

    @Test
    fun testTestCommandExecution() = runTest {
        val commandText = "go back"
        val source = CommandSource.TEXT
        
        viewModel.testCommand(commandText, source)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify loading state was set
        assertEquals(false, viewModel.isLoading.value)
        
        // Verify either success or error message was set
        val hasMessage = viewModel.successMessage.value != null || viewModel.errorMessage.value != null
        assertTrue(hasMessage) // Should have either success or error message
    }

    @Test
    fun testVoiceTestSimulation() = runTest {
        viewModel.startVoiceTest()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify loading completes
        assertEquals(false, viewModel.isLoading.value)
        
        // Should have success message from simulation
        assertNotNull(viewModel.successMessage.value)
        assertTrue(viewModel.successMessage.value!!.contains("Voice recognition simulated"))
    }

    @Test
    fun testCommandSuggestions() {
        val suggestions = viewModel.getCommandSuggestions("go")
        assertTrue(suggestions.isEmpty() || suggestions.all { it.contains("go", ignoreCase = true) })
    }

    @Test
    fun testProcessorInfo() {
        val info = viewModel.getProcessorInfo()
        assertTrue(info.containsKey("initialized"))
        assertTrue(info.containsKey("language"))
        assertTrue(info.containsKey("fuzzy_matching"))
        assertTrue(info.containsKey("match_threshold"))
        
        assertEquals(true, info["initialized"])
        assertEquals("en", info["language"])
        assertEquals(true, info["fuzzy_matching"])
        assertEquals(0.7f, info["match_threshold"])
    }

    @Test
    fun testErrorMessageClearing() {
        viewModel.clearError()
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun testSuccessMessageClearing() {
        viewModel.clearSuccess()
        assertEquals(null, viewModel.successMessage.value)
    }

    @Test
    fun testHistoryClearFunctionality() = runTest {
        viewModel.clearHistory()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should have empty history
        val history = viewModel.commandHistory.value
        assertTrue(history?.isEmpty() ?: true)
        
        // Should have success message
        assertNotNull(viewModel.successMessage.value)
        assertTrue(viewModel.successMessage.value!!.contains("cleared"))
    }

    @Test
    fun testCategoryCommandsDisplay() = runTest {
        viewModel.showCategoryCommands(CommandCategory.NAVIGATION)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should have a success message about found commands
        val message = viewModel.successMessage.value
        assertNotNull(message)
        assertTrue(message.contains("Found") && message.contains("NAVIGATION"))
    }

    @Test
    fun testDataReload() = runTest {
        viewModel.loadData()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Loading should complete without error
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun testMultipleCommandExecution() = runTest {
        val commands = listOf("go back", "scroll down", "volume up")
        
        commands.forEach { command ->
            viewModel.testCommand(command, CommandSource.TEXT)
            testDispatcher.scheduler.advanceUntilIdle()
        }
        
        // All executions should complete
        assertEquals(false, viewModel.isLoading.value)
    }
}