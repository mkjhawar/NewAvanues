/**
 * CommandEditorViewModel.kt - ViewModel for command editor UI
 *
 * MVVM architecture with state management for command creation/editing
 */

package com.augmentalis.voiceoscore.commandmanager.ui.editor

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.voiceoscore.commandmanager.registry.ActionType
import com.augmentalis.voiceoscore.commandmanager.registry.ConflictInfo
import com.augmentalis.voiceoscore.commandmanager.registry.DynamicCommandRegistry
import com.augmentalis.voiceoscore.commandmanager.registry.VoiceCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for command editor
 */
class CommandEditorViewModel : ViewModel() {

    companion object {
        private const val TAG = "CommandEditorViewModel"
    }

    // Dependencies (in real app, inject via Hilt)
    private val registry = DynamicCommandRegistry()
    private val importExport = CommandImportExport()

    // UI State
    private val _uiState = MutableStateFlow(CommandEditorUiState())
    val uiState: StateFlow<CommandEditorUiState> = _uiState.asStateFlow()

    // Template state
    private val _templates = MutableStateFlow<List<CommandTemplate>>(emptyList())
    val templates: StateFlow<List<CommandTemplate>> = _templates.asStateFlow()

    // Wizard state
    private val _wizardState = MutableStateFlow(WizardState())
    val wizardState: StateFlow<WizardState> = _wizardState.asStateFlow()

    init {
        loadTemplates()
        loadCommands()
    }

    // Command operations

    /**
     * Load all registered commands
     */
    fun loadCommands() {
        viewModelScope.launch {
            try {
                val commands = registry.getAllCommands()
                _uiState.value = _uiState.value.copy(
                    commands = commands,
                    isLoading = false
                )
                Log.i(TAG, "Loaded ${commands.size} commands")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load commands", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load commands: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Register a new command
     */
    fun registerCommand(command: VoiceCommand) {
        viewModelScope.launch {
            val result = registry.registerCommand(command)
            if (result.isSuccess) {
                loadCommands()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Command registered successfully"
                )
                Log.i(TAG, "Registered command: ${command.id}")
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Registration failed"
                )
                Log.e(TAG, "Failed to register command", result.exceptionOrNull())
            }
        }
    }

    /**
     * Unregister a command
     */
    fun unregisterCommand(commandId: String) {
        viewModelScope.launch {
            val result = registry.unregisterCommand(commandId)
            if (result.isSuccess) {
                loadCommands()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Command removed successfully"
                )
                Log.i(TAG, "Unregistered command: $commandId")
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Removal failed"
                )
                Log.e(TAG, "Failed to unregister command", result.exceptionOrNull())
            }
        }
    }

    /**
     * Detect conflicts for a command
     */
    fun detectConflicts(command: VoiceCommand): List<ConflictInfo> {
        return registry.detectConflicts(command)
    }

    /**
     * Test command matching
     */
    fun testCommand(phrase: String): List<VoiceCommand> {
        return registry.resolveCommand(phrase)
    }

    /**
     * Search commands
     */
    fun searchCommands(query: String) {
        viewModelScope.launch {
            val allCommands = registry.getAllCommands()
            val filtered = if (query.isEmpty()) {
                allCommands
            } else {
                allCommands.filter { command ->
                    command.id.contains(query, ignoreCase = true) ||
                            command.phrases.any { it.contains(query, ignoreCase = true) } ||
                            command.namespace.contains(query, ignoreCase = true)
                }
            }
            _uiState.value = _uiState.value.copy(
                commands = filtered,
                searchQuery = query
            )
        }
    }

    /**
     * Filter commands by namespace
     */
    fun filterByNamespace(namespace: String?) {
        viewModelScope.launch {
            val filtered = if (namespace == null) {
                registry.getAllCommands()
            } else {
                registry.getCommandsByNamespace(namespace)
            }
            _uiState.value = _uiState.value.copy(
                commands = filtered,
                selectedNamespace = namespace
            )
        }
    }

    // Template operations

    /**
     * Load templates from repository
     */
    fun loadTemplates() {
        viewModelScope.launch {
            val allTemplates = TemplateRepository.getAllTemplates()
            _templates.value = allTemplates
            Log.i(TAG, "Loaded ${allTemplates.size} templates")
        }
    }

    /**
     * Search templates
     */
    fun searchTemplates(query: String): List<CommandTemplate> {
        return TemplateRepository.searchTemplates(query)
    }

    /**
     * Filter templates
     */
    fun filterTemplates(filter: TemplateFilter): List<CommandTemplate> {
        return TemplateRepository.filterTemplates(filter)
    }

    /**
     * Apply template to create command
     */
    fun applyTemplate(template: CommandTemplate, customization: TemplateCustomization? = null) {
        val phrases = customization?.customPhrases?.ifEmpty { template.phrases } ?: template.phrases
        val params = customization?.customParams ?: template.defaultParams
        val priority = customization?.customPriority ?: template.priority
        val namespace = customization?.customNamespace ?: template.namespace

        val command = VoiceCommand(
            id = customization?.templateId ?: template.id,
            phrases = phrases,
            priority = priority,
            namespace = namespace,
            actionType = template.actionType,
            actionParams = params,
            metadata = mapOf("templateId" to template.id)
        )

        // Start wizard with pre-filled data
        startWizardWithTemplate(command)
    }

    // Wizard operations

    /**
     * Start wizard (fresh or from template)
     */
    fun startWizard() {
        _wizardState.value = WizardState(isActive = true)
    }

    /**
     * Start wizard with template data
     */
    private fun startWizardWithTemplate(command: VoiceCommand) {
        _wizardState.value = WizardState(
            isActive = true,
            currentStep = WizardStep.PHRASES,
            commandId = command.id,
            phrases = command.phrases.toMutableList(),
            actionType = command.actionType,
            actionParams = command.actionParams.toMutableMap(),
            priority = command.priority,
            namespace = command.namespace
        )
    }

    /**
     * Update wizard step
     */
    fun updateWizardStep(step: WizardStep) {
        _wizardState.value = _wizardState.value.copy(currentStep = step)
    }

    /**
     * Update wizard phrases
     */
    fun updateWizardPhrases(phrases: List<String>) {
        _wizardState.value = _wizardState.value.copy(phrases = phrases.toMutableList())
    }

    /**
     * Update wizard action type
     */
    fun updateWizardActionType(actionType: ActionType) {
        _wizardState.value = _wizardState.value.copy(actionType = actionType)
    }

    /**
     * Update wizard action params
     */
    fun updateWizardParams(params: Map<String, Any>) {
        _wizardState.value = _wizardState.value.copy(actionParams = params.toMutableMap())
    }

    /**
     * Update wizard priority
     */
    fun updateWizardPriority(priority: Int) {
        _wizardState.value = _wizardState.value.copy(priority = priority)
    }

    /**
     * Update wizard namespace
     */
    fun updateWizardNamespace(namespace: String) {
        _wizardState.value = _wizardState.value.copy(namespace = namespace)
    }

    /**
     * Complete wizard and create command
     */
    fun completeWizard() {
        val state = _wizardState.value

        // Validate
        if (state.commandId.isEmpty() || state.phrases.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Invalid command data")
            return
        }

        val command = VoiceCommand(
            id = state.commandId,
            phrases = state.phrases,
            priority = state.priority,
            namespace = state.namespace,
            actionType = state.actionType,
            actionParams = state.actionParams
        )

        registerCommand(command)
        cancelWizard()
    }

    /**
     * Cancel wizard
     */
    fun cancelWizard() {
        _wizardState.value = WizardState(isActive = false)
    }

    // Import/Export operations

    /**
     * Export commands to JSON
     */
    fun exportCommands(context: Context, commands: List<VoiceCommand>? = null) {
        viewModelScope.launch {
            val commandsToExport = commands ?: registry.getAllCommands()
            val result = importExport.exportToFile(context, commandsToExport)

            when (result) {
                is ExportFileResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Exported ${result.commandCount} commands to ${result.filePath}"
                    )
                    Log.i(TAG, "Exported commands successfully")
                }
                is ExportFileResult.Failure -> {
                    _uiState.value = _uiState.value.copy(error = result.error)
                    Log.e(TAG, "Export failed: ${result.error}")
                }
            }
        }
    }

    /**
     * Import commands from JSON file
     */
    fun importCommands(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = importExport.importFromFile(context, uri)

            when (result) {
                is ImportResult.Success -> {
                    // Register all imported commands
                    result.commands.forEach { command ->
                        registry.registerCommand(command)
                    }
                    loadCommands()

                    val warnings = if (result.warnings.isNotEmpty()) {
                        "\nWarnings: ${result.warnings.joinToString(", ")}"
                    } else ""

                    _uiState.value = _uiState.value.copy(
                        successMessage = "Imported ${result.commands.size} commands$warnings"
                    )
                    Log.i(TAG, "Imported ${result.commands.size} commands")
                }
                is ImportResult.Failure -> {
                    _uiState.value = _uiState.value.copy(error = result.error)
                    Log.e(TAG, "Import failed: ${result.error}")
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

/**
 * Command editor UI state
 */
data class CommandEditorUiState(
    val commands: List<VoiceCommand> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val selectedNamespace: String? = null
)

/**
 * Wizard state for command creation
 */
data class WizardState(
    val isActive: Boolean = false,
    val currentStep: WizardStep = WizardStep.PHRASES,
    val commandId: String = "",
    val phrases: MutableList<String> = mutableListOf(),
    val actionType: ActionType = ActionType.CUSTOM_ACTION,
    val actionParams: MutableMap<String, Any> = mutableMapOf(),
    val priority: Int = 50,
    val namespace: String = "default"
)

/**
 * Wizard steps
 */
enum class WizardStep {
    PHRASES,
    ACTION_TYPE,
    ACTION_PARAMS,
    PRIORITY_NAMESPACE,
    TEST,
    CONFIRM
}
