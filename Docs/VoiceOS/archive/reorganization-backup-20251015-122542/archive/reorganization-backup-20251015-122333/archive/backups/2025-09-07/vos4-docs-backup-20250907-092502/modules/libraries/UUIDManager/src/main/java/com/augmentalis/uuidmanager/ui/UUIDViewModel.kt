/**
 * UUIDViewModel.kt - ViewModel for UUID Manager UI
 * 
 * Manages UUID registry state, element navigation, and command processing
 * 
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 */
package com.augmentalis.uuidmanager.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.uuidmanager.UUIDManager
import com.augmentalis.uuidmanager.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * UI state for UUID Manager
 */
data class UUIDUiState(
    val registeredElements: List<UUIDElementInfo> = emptyList(),
    val selectedElement: UUIDElementInfo? = null,
    val commandHistory: List<CommandHistoryItem> = emptyList(),
    val registryStats: RegistryStatistics = RegistryStatistics(),
    val navigationPath: List<String> = emptyList(),
    val voiceCommandActive: Boolean = false,
    val currentCommand: String = "",
    val commandResult: CommandResultInfo? = null,
    val searchQuery: String = "",
    val searchResults: List<UUIDElementInfo> = emptyList(),
    val filterType: String = "all",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Extended UUID element information for UI
 */
data class UUIDElementInfo(
    val uuid: String,
    val name: String?,
    val type: String,
    val position: UUIDPosition?,
    val isEnabled: Boolean,
    val isVisible: Boolean,
    val parentUUID: String?,
    val childrenCount: Int,
    val actionCount: Int,
    val registrationTime: Long,
    val lastAccessTime: Long?,
    val accessCount: Int
)

/**
 * Command history item
 */
data class CommandHistoryItem(
    val id: String,
    val command: String,
    val targetUUID: String?,
    val targetName: String?,
    val action: String,
    val success: Boolean,
    val timestamp: Long,
    val executionTime: Long,
    val errorMessage: String?
)

/**
 * Registry statistics
 */
data class RegistryStatistics(
    val totalElements: Int = 0,
    val activeElements: Int = 0,
    val elementsByType: Map<String, Int> = emptyMap(),
    val totalCommands: Int = 0,
    val successfulCommands: Int = 0,
    val averageExecutionTime: Long = 0,
    val memoryUsage: Long = 0
)

/**
 * Command result information
 */
data class CommandResultInfo(
    val success: Boolean,
    val message: String,
    val targetUUID: String?,
    val action: String?,
    val executionTime: Long
)

/**
 * ViewModel for UUID Manager UI
 */
class UUIDViewModel(
    private val uuidManager: UUIDManager = UUIDManager.instance
) : ViewModel() {
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    private val _uiState = MutableLiveData(UUIDUiState())
    val uiState: LiveData<UUIDUiState> = _uiState
    
    // Mock data for demonstration
    private val mockElements = mutableListOf<UUIDElementInfo>()
    private val mockHistory = mutableListOf<CommandHistoryItem>()
    
    init {
        initializeMockData()
        refreshRegistry()
        observeCommandEvents()
    }
    
    private fun initializeMockData() {
        // Create mock elements for demonstration
        val types = listOf("button", "text", "image", "container", "list", "form")
        val names = listOf("Submit", "Cancel", "Home", "Settings", "Profile", "Search", 
                          "Menu", "Header", "Footer", "Sidebar", "Content", "Card")
        
        repeat(20) { index ->
            val element = UUIDElementInfo(
                uuid = uuidManager.generateUUID(),
                name = names.random(),
                type = types.random(),
                position = UUIDPosition(
                    x = (index % 4) * 100f,
                    y = (index / 4) * 100f,
                    z = 0f,
                    width = 80f,
                    height = 40f
                ),
                isEnabled = index % 3 != 0,
                isVisible = true,
                parentUUID = if (index > 5) mockElements.randomOrNull()?.uuid else null,
                childrenCount = (0..3).random(),
                actionCount = (1..5).random(),
                registrationTime = System.currentTimeMillis() - (index * 3600000),
                lastAccessTime = if (index < 10) System.currentTimeMillis() - (index * 60000) else null,
                accessCount = (0..50).random()
            )
            mockElements.add(element)
        }
    }
    
    private fun observeCommandEvents() {
        viewModelScope.launch {
            uuidManager.commandEvents.collect { command ->
                // Update command history when new commands are processed
                refreshCommandHistory()
            }
        }
    }
    
    fun refreshRegistry() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true)
            delay(500) // Simulate loading
            
            // Get real elements from UUIDManager
            val realElements = uuidManager.getAllElements()
            val elements = if (realElements.isNotEmpty()) {
                realElements.map { element ->
                    UUIDElementInfo(
                        uuid = element.uuid,
                        name = element.name,
                        type = element.type,
                        position = element.position,
                        isEnabled = element.isEnabled,
                        isVisible = true, // Default to visible
                        parentUUID = element.parent,
                        childrenCount = 0,
                        actionCount = element.actions.size,
                        registrationTime = System.currentTimeMillis(),
                        lastAccessTime = null,
                        accessCount = 0
                    )
                }
            } else {
                mockElements // Use mock data if no real elements
            }
            
            val stats = calculateStatistics(elements)
            
            _uiState.value = _uiState.value?.copy(
                registeredElements = elements,
                registryStats = stats,
                isLoading = false
            )
        }
    }
    
    fun selectElement(element: UUIDElementInfo) {
        _uiState.value = _uiState.value?.copy(
            selectedElement = element,
            navigationPath = buildNavigationPath(element)
        )
    }
    
    fun clearSelection() {
        _uiState.value = _uiState.value?.copy(
            selectedElement = null,
            navigationPath = emptyList()
        )
    }
    
    fun generateNewUUID(): String {
        return uuidManager.generateUUID()
    }
    
    fun registerNewElement(name: String, type: String) {
        viewModelScope.launch {
            val uuid = uuidManager.registerWithAutoUUID(
                name = name,
                type = type,
                position = UUIDPosition(0f, 0f, 0f, 100f, 50f),
                actions = mapOf(
                    "click" to { params -> println("Clicked: $name") },
                    "focus" to { params -> println("Focused: $name") }
                )
            )
            
            // Add to mock elements for display
            val newElement = UUIDElementInfo(
                uuid = uuid,
                name = name,
                type = type,
                position = UUIDPosition(0f, 0f, 0f, 100f, 50f),
                isEnabled = true,
                isVisible = true,
                parentUUID = null,
                childrenCount = 0,
                actionCount = 2,
                registrationTime = System.currentTimeMillis(),
                lastAccessTime = null,
                accessCount = 0
            )
            mockElements.add(newElement)
            
            refreshRegistry()
        }
    }
    
    fun unregisterElement(uuid: String) {
        viewModelScope.launch {
            uuidManager.unregisterElement(uuid)
            mockElements.removeAll { it.uuid == uuid }
            refreshRegistry()
        }
    }
    
    fun processVoiceCommand(command: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(
                voiceCommandActive = true,
                currentCommand = command
            )
            
            val result = uuidManager.processVoiceCommand(command)
            
            // Add to history
            val historyItem = CommandHistoryItem(
                id = UUID.randomUUID().toString(),
                command = command,
                targetUUID = result.targetUUID,
                targetName = mockElements.find { it.uuid == result.targetUUID }?.name,
                action = result.action ?: "unknown",
                success = result.success,
                timestamp = System.currentTimeMillis(),
                executionTime = result.executionTime,
                errorMessage = result.error
            )
            mockHistory.add(0, historyItem)
            if (mockHistory.size > 50) {
                mockHistory.removeLast()
            }
            
            _uiState.value = _uiState.value?.copy(
                voiceCommandActive = false,
                currentCommand = "",
                commandResult = CommandResultInfo(
                    success = result.success,
                    message = result.message ?: result.error ?: "Command processed",
                    targetUUID = result.targetUUID,
                    action = result.action,
                    executionTime = result.executionTime
                ),
                commandHistory = mockHistory.toList()
            )
        }
    }
    
    fun searchElements(query: String) {
        _uiState.value = _uiState.value?.copy(searchQuery = query)
        
        if (query.isEmpty()) {
            _uiState.value = _uiState.value?.copy(searchResults = emptyList())
            return
        }
        
        val results = mockElements.filter { element ->
            element.name?.contains(query, ignoreCase = true) == true ||
            element.uuid.contains(query, ignoreCase = true) ||
            element.type.contains(query, ignoreCase = true)
        }
        
        _uiState.value = _uiState.value?.copy(searchResults = results)
    }
    
    fun filterByType(type: String) {
        _uiState.value = _uiState.value?.copy(filterType = type)
        
        val filtered = if (type == "all") {
            mockElements
        } else {
            mockElements.filter { it.type == type }
        }
        
        _uiState.value = _uiState.value?.copy(registeredElements = filtered)
    }
    
    fun navigateToElement(direction: String) {
        val currentElement = _uiState.value?.selectedElement ?: return
        
        viewModelScope.launch {
            val targetElement = uuidManager.navigate(currentElement.uuid, direction)
            targetElement?.let { target ->
                val elementInfo = mockElements.find { it.uuid == target.uuid }
                elementInfo?.let { selectElement(it) }
            }
        }
    }
    
    fun clearRegistry() {
        viewModelScope.launch {
            uuidManager.clearAll()
            mockElements.clear()
            mockHistory.clear()
            refreshRegistry()
        }
    }
    
    fun refreshCommandHistory() {
        _uiState.value = _uiState.value?.copy(commandHistory = mockHistory.toList())
    }
    
    fun testSpatialNavigation() {
        viewModelScope.launch {
            // Test navigation in different directions
            val testCommands = listOf(
                "move right",
                "go up",
                "navigate down",
                "select first",
                "click last"
            )
            
            for (command in testCommands) {
                processVoiceCommand(command)
                delay(1000)
            }
        }
    }
    
    fun exportRegistry(): String {
        val elements = _uiState.value?.registeredElements ?: emptyList()
        val stats = _uiState.value?.registryStats ?: RegistryStatistics()
        
        return buildString {
            appendLine("UUID Registry Export")
            appendLine("=" * 50)
            appendLine("Generated: ${Date()}")
            appendLine()
            appendLine("Statistics:")
            appendLine("  Total Elements: ${stats.totalElements}")
            appendLine("  Active Elements: ${stats.activeElements}")
            appendLine("  Total Commands: ${stats.totalCommands}")
            appendLine("  Success Rate: ${if (stats.totalCommands > 0) 
                (stats.successfulCommands * 100 / stats.totalCommands) else 0}%")
            appendLine()
            appendLine("Elements:")
            elements.forEach { element ->
                appendLine("  - UUID: ${element.uuid}")
                appendLine("    Name: ${element.name ?: "unnamed"}")
                appendLine("    Type: ${element.type}")
                appendLine("    Enabled: ${element.isEnabled}")
                appendLine("    Actions: ${element.actionCount}")
                appendLine()
            }
        }
    }
    
    private fun calculateStatistics(elements: List<UUIDElementInfo>): RegistryStatistics {
        val typeGroups = elements.groupBy { it.type }
        val activeCount = elements.count { it.isEnabled }
        val successCount = mockHistory.count { it.success }
        val avgTime = if (mockHistory.isNotEmpty()) {
            mockHistory.map { it.executionTime }.average().toLong()
        } else 0L
        
        return RegistryStatistics(
            totalElements = elements.size,
            activeElements = activeCount,
            elementsByType = typeGroups.mapValues { it.value.size },
            totalCommands = mockHistory.size,
            successfulCommands = successCount,
            averageExecutionTime = avgTime,
            memoryUsage = elements.size * 1024L // Rough estimate
        )
    }
    
    private fun buildNavigationPath(element: UUIDElementInfo): List<String> {
        val path = mutableListOf<String>()
        var current: UUIDElementInfo? = element
        
        while (current != null) {
            path.add(0, current.name ?: current.uuid.take(8))
            current = current.parentUUID?.let { parentId ->
                mockElements.find { it.uuid == parentId }
            }
        }
        
        return path
    }
    
    private operator fun String.times(count: Int): String = repeat(count)
}