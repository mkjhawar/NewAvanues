/**
 * AvidViewModel.kt - ViewModel for VUID Manager UI
 *
 * Manages VUID registry state, element navigation, and command processing
 *
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 */
package com.augmentalis.avidcreator.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.avidcreator.AvidCreator
import com.augmentalis.avidcreator.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * UI state for VUID Manager
 */
data class AvidUiState(
    val registeredElements: List<AvidElementInfo> = emptyList(),
    val selectedElement: AvidElementInfo? = null,
    val commandHistory: List<CommandHistoryItem> = emptyList(),
    val registryStats: RegistryStatistics = RegistryStatistics(),
    val navigationPath: List<String> = emptyList(),
    val voiceCommandActive: Boolean = false,
    val currentCommand: String = "",
    val commandResult: CommandResultInfo? = null,
    val searchQuery: String = "",
    val searchResults: List<AvidElementInfo> = emptyList(),
    val filterType: String = "all",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Extended AVID element information for UI
 */
data class AvidElementInfo(
    val avid: String,
    val name: String?,
    val type: String,
    val position: AvidPosition?,
    val isEnabled: Boolean,
    val isVisible: Boolean,
    val parentAvid: String?,
    val childrenCount: Int,
    val actionCount: Int,
    val registrationTime: Long,
    val lastAccessTime: Long?,
    val accessCount: Int
) {
    /**
     * Backward-compatible alias for avid
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use avid instead", ReplaceWith("avid"))
    val vuid: String get() = avid

    @Suppress("DEPRECATION")
    @Deprecated("Use avid instead", ReplaceWith("avid"))
    val uuid: String get() = avid

    /**
     * Backward-compatible alias for parentAvid
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use parentAvid instead", ReplaceWith("parentAvid"))
    val parentUUID: String? get() = parentAvid
}

/**
 * Command history item
 */
data class CommandHistoryItem(
    val id: String,
    val command: String,
    val targetAvid: String?,
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
    val targetAvid: String?,
    val action: String?,
    val executionTime: Long
) {
    @Suppress("DEPRECATION")
    @Deprecated("Use targetAvid instead", ReplaceWith("targetAvid"))
    val targetUUID: String? get() = targetAvid
}

/**
 * ViewModel for VUID Manager UI
 */
class AvidViewModel(
    private val avidManager: AvidCreator = AvidCreator.getInstance()
) : ViewModel() {
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    private val _uiState = MutableLiveData(AvidUiState())
    val uiState: LiveData<AvidUiState> = _uiState

    // Mock data for demonstration
    private val mockElements = mutableListOf<AvidElementInfo>()
    private val mockHistory = mutableListOf<CommandHistoryItem>()
    
    init {
        initializeMockData()
        refreshRegistry()
        observeCommandEvents()
    }
    
    private fun initializeMockData() {
        // Development fallback: displayed when AVID registry has no real elements.
        // These synthetic elements allow the UI to be exercised before any real
        // app elements have been registered via AvidCreator.
        val types = listOf("button", "text", "image", "container", "list", "form")
        val names = listOf("Submit", "Cancel", "Home", "Settings", "Profile", "Search", 
                          "Menu", "Header", "Footer", "Sidebar", "Content", "Card")
        
        repeat(20) { index ->
            val element = AvidElementInfo(
                avid = avidManager.generateAvid(),
                name = names.random(),
                type = types.random(),
                position = AvidPosition(
                    x = (index % 4) * 100f,
                    y = (index / 4) * 100f,
                    z = 0f,
                    width = 80f,
                    height = 40f
                ),
                isEnabled = index % 3 != 0,
                isVisible = true,
                parentAvid = if (index > 5) mockElements.randomOrNull()?.avid else null,
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
            avidManager.commandEvents.collect { _ ->
                // Update command history when new commands are processed
                refreshCommandHistory()
            }
        }
    }
    
    fun refreshRegistry() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true)

            // Get real elements from AvidManager
            val realElements = avidManager.getAllElements()
            val elements = if (realElements.isNotEmpty()) {
                realElements.map { element ->
                    AvidElementInfo(
                        avid = element.avid,
                        name = element.name,
                        type = element.type,
                        position = element.position,
                        isEnabled = element.isEnabled,
                        isVisible = true, // Default to visible
                        parentAvid = element.parent,
                        childrenCount = 0,
                        actionCount = element.actions.size,
                        registrationTime = System.currentTimeMillis(),
                        lastAccessTime = null,
                        accessCount = 0
                    )
                }
            } else {
                android.util.Log.d("AvidViewModel", "AVID registry empty, using ${mockElements.size} mock elements for development")
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
    
    fun selectElement(element: AvidElementInfo) {
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
    
    fun generateNewVUID(): String {
        return avidManager.generateAvid()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Use generateNewVUID instead", ReplaceWith("generateNewVUID()"))
    fun generateNewUUID(): String = generateNewVUID()

    fun registerNewElement(name: String, type: String) {
        viewModelScope.launch {
            val elementAvid = avidManager.registerWithAutoAvid(
                name = name,
                type = type,
                position = AvidPosition(0f, 0f, 0f, 100f, 50f),
                actions = mapOf(
                    "click" to { _ -> println("Clicked: $name") },
                    "focus" to { _ -> println("Focused: $name") }
                )
            )

            // Add to mock elements for display
            val newElement = AvidElementInfo(
                avid = elementAvid,
                name = name,
                type = type,
                position = AvidPosition(0f, 0f, 0f, 100f, 50f),
                isEnabled = true,
                isVisible = true,
                parentAvid = null,
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

    fun unregisterElement(avid: String) {
        viewModelScope.launch {
            avidManager.unregisterElement(avid)
            mockElements.removeAll { it.avid == avid }
            refreshRegistry()
        }
    }
    
    fun processVoiceCommand(command: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(
                voiceCommandActive = true,
                currentCommand = command
            )
            
            val result = avidManager.processVoiceCommand(command)
            
            // Add to history
            val historyItem = CommandHistoryItem(
                id = UUID.randomUUID().toString(),
                command = command,
                targetAvid = result.targetAvid,
                targetName = mockElements.find { it.avid == result.targetAvid }?.name,
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
                    targetAvid = result.targetAvid,
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
            element.avid.contains(query, ignoreCase = true) ||
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
            val targetElement = avidManager.navigate(currentElement.avid, direction)
            targetElement?.let { target ->
                val elementInfo = mockElements.find { it.avid == target.avid }
                elementInfo?.let { selectElement(it) }
            }
        }
    }
    
    fun clearRegistry() {
        viewModelScope.launch {
            avidManager.clearAll()
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
            appendLine("VUID Registry Export")
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
                appendLine("  - VUID: ${element.avid}")
                appendLine("    Name: ${element.name ?: "unnamed"}")
                appendLine("    Type: ${element.type}")
                appendLine("    Enabled: ${element.isEnabled}")
                appendLine("    Actions: ${element.actionCount}")
                appendLine()
            }
        }
    }
    
    private fun calculateStatistics(elements: List<AvidElementInfo>): RegistryStatistics {
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
    
    private fun buildNavigationPath(element: AvidElementInfo): List<String> {
        val path = mutableListOf<String>()
        var current: AvidElementInfo? = element

        while (current != null) {
            path.add(0, current.name ?: current.avid.take(8))
            current = current.parentAvid?.let { parentId ->
                mockElements.find { it.avid == parentId }
            }
        }

        return path
    }
    
    private operator fun String.times(count: Int): String = repeat(count)
}