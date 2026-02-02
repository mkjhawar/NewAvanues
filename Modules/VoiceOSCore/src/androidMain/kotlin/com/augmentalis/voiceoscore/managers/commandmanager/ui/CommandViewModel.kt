/**
 * CommandViewModel.kt - ViewModel for Command Manager UI
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Manages UI state and business logic for Command Manager
 */
package com.augmentalis.voiceoscore.managers.commandmanager.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.augmentalis.voiceoscore.*
import com.augmentalis.voiceoscore.managers.commandmanager.processor.CommandProcessor
import com.augmentalis.voiceoscore.managers.commandmanager.history.CommandHistory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for Command Manager UI
 */
class CommandViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "CommandViewModel"
    }
    
    private val commandProcessor = CommandProcessor(context)
    private val commandHistoryManager = CommandHistory()
    
    private val _commandStats = MutableLiveData<CommandStats>()
    val commandStats: LiveData<CommandStats> = _commandStats
    
    private val _commandHistory = MutableLiveData<List<CommandHistoryEntry>>()
    val commandHistory: LiveData<List<CommandHistoryEntry>> = _commandHistory
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    private val _availableCommands = MutableLiveData<Map<CommandCategory, List<CommandDefinition>>>()
    val availableCommands: LiveData<Map<CommandCategory, List<CommandDefinition>>> = _availableCommands
    
    init {
        // Initialize command processor
        viewModelScope.launch {
            try {
                commandProcessor.initialize()
                loadInitialData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize command processor", e)
                _errorMessage.value = "Failed to initialize command system: ${e.message}"
            }
        }
    }
    
    /**
     * Load initial data
     */
    fun loadData() {
        viewModelScope.launch {
            try {
                loadInitialData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load data", e)
                _errorMessage.value = "Failed to load command data: ${e.message}"
            }
        }
    }
    
    private suspend fun loadInitialData() {
        // Load command history
        _commandHistory.value = commandHistoryManager.getRecentEntries(20)
        
        // Calculate and load stats
        refreshStats()
        
        // Load available commands by category
        loadAvailableCommands()
    }
    
    /**
     * Test a command
     */
    fun testCommand(commandText: String, source: CommandSource) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                Log.d(TAG, "Testing command: $commandText")
                
                val result = commandProcessor.processCommand(
                    text = commandText,
                    source = source,
                    context = null
                )
                
                // Add to history
                val entry = CommandHistoryEntry(
                    command = result.command,
                    result = result,
                    timestamp = System.currentTimeMillis()
                )
                commandHistoryManager.addEntry(entry)
                
                // Update UI
                _commandHistory.value = commandHistoryManager.getRecentEntries(20)
                refreshStats()
                
                if (result.success) {
                    _successMessage.value = "Command executed successfully in ${result.executionTime}ms"
                    Log.d(TAG, "Command test successful: ${result.response}")
                } else {
                    _errorMessage.value = result.error?.message ?: "Command failed"
                    Log.w(TAG, "Command test failed: ${result.error?.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to test command", e)
                _errorMessage.value = "Failed to test command: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Start voice test (simulated for now)
     */
    fun startVoiceTest() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Simulate voice recognition delay
                delay(2000)
                
                // Simulate a voice command
                val simulatedCommands = listOf(
                    "go back",
                    "volume up", 
                    "scroll down",
                    "take screenshot",
                    "open settings"
                )
                
                val randomCommand = simulatedCommands.random()
                _successMessage.value = "Voice recognition simulated: \"$randomCommand\""
                
                // Execute the simulated command
                testCommand(randomCommand, CommandSource.VOICE)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start voice test", e)
                _errorMessage.value = "Voice test failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh command statistics
     */
    fun refreshStats() {
        viewModelScope.launch {
            try {
                val history = commandHistoryManager.getRecentEntries(1000) // Get a large number for stats
                
                val totalCommands = history.size
                val successfulCommands = history.count { it.result.success }
                val failedCommands = totalCommands - successfulCommands
                val averageExecutionTime = if (totalCommands > 0) {
                    history.map { it.result.executionTime }.average().toLong()
                } else 0L
                
                // Get top commands
                val commandCounts = history.groupBy { it.command.id }
                    .mapValues { it.value.size }
                val topCommands = commandCounts.toList()
                    .sortedByDescending { it.second }
                    .take(5)
                    .map { it.first }
                
                val stats = CommandStats(
                    totalCommands = totalCommands,
                    successfulCommands = successfulCommands,
                    failedCommands = failedCommands,
                    averageExecutionTime = averageExecutionTime,
                    topCommands = topCommands
                )
                
                _commandStats.value = stats
                Log.d(TAG, "Stats updated: $stats")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh stats", e)
                _errorMessage.value = "Failed to refresh statistics: ${e.message}"
            }
        }
    }
    
    /**
     * Show commands for a specific category
     */
    fun showCategoryCommands(category: CommandCategory) {
        viewModelScope.launch {
            try {
                val allCommands = commandProcessor.getAvailableCommands(null)
                val categoryCommands = allCommands.filter { 
                    it.category.equals(category.name, ignoreCase = true) 
                }
                
                _successMessage.value = "Found ${categoryCommands.size} commands in ${category.name} category"
                Log.d(TAG, "Category ${category.name}: ${categoryCommands.size} commands")
                
                // You could show these in a dialog or navigate to a detail screen
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load category commands", e)
                _errorMessage.value = "Failed to load category commands: ${e.message}"
            }
        }
    }
    
    /**
     * Clear command history
     */
    fun clearHistory() {
        viewModelScope.launch {
            try {
                commandHistoryManager.clearHistory()
                _commandHistory.value = emptyList()
                refreshStats()
                _successMessage.value = "Command history cleared"
                Log.d(TAG, "Command history cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear history", e)
                _errorMessage.value = "Failed to clear history: ${e.message}"
            }
        }
    }
    
    /**
     * Load available commands by category
     */
    private fun loadAvailableCommands() {
        viewModelScope.launch {
            try {
                val allCommands = commandProcessor.getAvailableCommands(null)
                val categorizedCommands = allCommands.groupBy { definition ->
                    try {
                        CommandCategory.valueOf(definition.category.uppercase())
                    } catch (e: IllegalArgumentException) {
                        CommandCategory.CUSTOM
                    }
                }
                
                _availableCommands.value = categorizedCommands
                Log.d(TAG, "Loaded ${allCommands.size} commands across ${categorizedCommands.keys.size} categories")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load available commands", e)
                _errorMessage.value = "Failed to load available commands: ${e.message}"
            }
        }
    }
    
    /**
     * Get command suggestions for input
     */
    fun getCommandSuggestions(input: String): List<String> {
        return try {
            val allCommands = commandProcessor.getAvailableCommands(null)
            allCommands.flatMap { it.patterns }
                .filter { it.contains(input, ignoreCase = true) }
                .take(5)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get command suggestions", e)
            emptyList()
        }
    }
    
    /**
     * Get processor configuration info
     */
    fun getProcessorInfo(): Map<String, Any> {
        return mapOf(
            "initialized" to true,
            "language" to "en",
            "fuzzy_matching" to true,
            "match_threshold" to 0.7f
        )
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clear success message  
     */
    fun clearSuccess() {
        _successMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                commandProcessor.shutdown()
                Log.d(TAG, "CommandViewModel cleared and processor shutdown")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}

/**
 * ViewModelProvider Factory for CommandViewModel
 */
class CommandViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommandViewModel::class.java)) {
            return CommandViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}