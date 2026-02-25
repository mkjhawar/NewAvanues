/**
 * DashboardViewModel.kt - Dashboard state management for Avanues app
 *
 * Manages and observes:
 * - Service states (VoiceAvanue, WebAvanue, VoiceCursor)
 * - System permissions (accessibility, overlay, microphone, battery, notifications)
 * - Last heard command
 * - Command data (static categories, dynamic, custom, synonyms)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.home

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.foundation.state.LastHeardCommand
import com.augmentalis.foundation.state.ServiceState
import com.augmentalis.voiceavanue.service.VoiceAvanueAccessibilityService
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.StaticCommandRegistry
import com.augmentalis.voiceoscore.command.SynonymRegistry
import com.augmentalis.voiceoscore.StaticCommand as CoreStaticCommand
import com.augmentalis.voiceoscore.CommandCategory as CoreCommandCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ModuleStatus - Status of a single module/service on the dashboard.
 */
data class ModuleStatus(
    val moduleId: String,
    val displayName: String,
    val description: String,
    val state: ServiceState,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * PermissionStatus - Current status of all required system permissions.
 */
data class PermissionStatus(
    val accessibilityEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val microphoneGranted: Boolean = false,
    val batteryOptimized: Boolean = false,
    val notificationsEnabled: Boolean = false
) {
    val allGranted: Boolean
        get() = accessibilityEnabled && overlayEnabled && microphoneGranted &&
                batteryOptimized && notificationsEnabled
}

/**
 * StaticCommand - A single static voice command with enable/disable toggle.
 */
data class StaticCommand(
    val id: String,
    val phrase: String,
    val enabled: Boolean = true,
    val synonyms: List<String> = emptyList(),
    val description: String = "",
    val actionType: CommandActionType = CommandActionType.CUSTOM
)

/**
 * MacroStep - A single step in a macro (sequential action chain).
 * Each step has an action type, optional target, and delay before next step.
 */
data class MacroStep(
    val actionType: CommandActionType,
    val target: String = "",
    val delayMs: Long = 300L
)

/**
 * CustomCommandInfo - A user-created custom voice command.
 * actionType determines what happens when the command is recognized.
 * actionTarget provides context (e.g., app package for OPEN_APP, URL for NAVIGATE).
 * For macros (actionType=MACRO), steps contains the sequential action chain.
 */
data class CustomCommandInfo(
    val id: String,
    val name: String,
    val phrases: List<String>,
    val actionType: CommandActionType = CommandActionType.CUSTOM,
    val actionTarget: String = "",
    val steps: List<MacroStep> = emptyList(),
    val isActive: Boolean = true
) {
    val isMacro: Boolean get() = actionType == CommandActionType.MACRO && steps.isNotEmpty()
}

/**
 * SynonymEntryInfo - A verb synonym mapping (canonical -> alternatives).
 * isDefault=true for system-provided synonyms (cannot be deleted, only reset).
 */
data class SynonymEntryInfo(
    val canonical: String,
    val synonyms: List<String>,
    val isDefault: Boolean = false
)

/**
 * CommandCategory - A named group of static commands (e.g. Navigation, Text, Cursor).
 */
data class CommandCategory(
    val name: String,
    val commands: List<StaticCommand>
)

/**
 * CommandsUiState - Complete command management state for the dashboard.
 * Static categories contain actual command data; counts for other types
 * until full command sources are wired in Task #19.
 */
data class CommandsUiState(
    val staticCategories: List<CommandCategory> = emptyList(),
    val customCommands: List<CustomCommandInfo> = emptyList(),
    val synonymEntries: List<SynonymEntryInfo> = emptyList(),
    val dynamicCommandCount: Int = 0
) {
    val staticCount: Int get() = staticCategories.sumOf { it.commands.size }
    val customCount: Int get() = customCommands.size
    val synonymCount: Int get() = synonymEntries.size
    val dynamicCount: Int get() = dynamicCommandCount
}

/**
 * DashboardUiState - Complete dashboard state combining all observable streams.
 */
data class DashboardUiState(
    val modules: List<ModuleStatus> = emptyList(),
    val permissions: PermissionStatus = PermissionStatus(),
    val lastHeardCommand: LastHeardCommand = LastHeardCommand.NONE,
    val commands: CommandsUiState = CommandsUiState()
) {
    val voiceAvanue: ModuleStatus? get() = modules.find { it.moduleId == "voiceavanue" }
    val webAvanue: ModuleStatus? get() = modules.find { it.moduleId == "webavanue" }
    val voiceCursor: ModuleStatus? get() = modules.find { it.moduleId == "voicecursor" }
    val hasLastCommand: Boolean get() = lastHeardCommand.phrase.isNotEmpty()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: AvanuesSettingsRepository,
    private val phraseSuggestionRepo: com.augmentalis.database.repositories.IPhraseSuggestionRepository
) : ViewModel() {

    private val _modules = MutableStateFlow<List<ModuleStatus>>(emptyList())
    private val _permissions = MutableStateFlow(PermissionStatus())
    private val _lastHeardCommand = MutableStateFlow(LastHeardCommand.NONE)
    private val _commands = MutableStateFlow(CommandsUiState())

    val uiState: StateFlow<DashboardUiState> = combine(
        _modules,
        _permissions,
        _lastHeardCommand,
        _commands
    ) { modules, perms, command, commands ->
        DashboardUiState(
            modules = modules,
            permissions = perms,
            lastHeardCommand = command,
            commands = commands
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        refreshAll()
        loadStaticCommands()
        loadDefaultSynonyms()
    }

    /**
     * Refreshes all state (modules + permissions).
     * Call on lifecycle resume to pick up external changes.
     */
    fun refreshAll() {
        refreshModuleStates()
        refreshPermissions()
    }

    /**
     * Loads static commands from VoiceOSCore's StaticCommandRegistry,
     * grouped by CommandCategory. Overlays persisted toggle state from DataStore.
     */
    private fun loadStaticCommands() {
        viewModelScope.launch {
            val disabledIds = settingsRepository.disabledCommands.first()
            val coreCommands = StaticCommandRegistry.all()
            val grouped = coreCommands.groupBy { it.category }

            val categories = grouped.mapNotNull { (coreCat, cmds) ->
                if (cmds.isEmpty()) return@mapNotNull null
                CommandCategory(
                    name = formatCategoryName(coreCat),
                    commands = cmds.map { cmd ->
                        StaticCommand(
                            id = cmd.actionType.name,
                            phrase = cmd.primaryPhrase,
                            enabled = cmd.actionType.name !in disabledIds,
                            synonyms = cmd.phrases.drop(1),
                            description = cmd.description,
                            actionType = cmd.actionType
                        )
                    }
                )
            }

            val current = _commands.value
            _commands.value = current.copy(staticCategories = categories)
        }
    }

    /**
     * Loads verb synonym mappings from KMP SynonymRegistry,
     * then merges persisted user-added synonyms from DataStore.
     */
    private fun loadDefaultSynonyms() {
        viewModelScope.launch {
            val defaults = SynonymRegistry.all().map { entry ->
                SynonymEntryInfo(
                    canonical = entry.canonical,
                    synonyms = entry.synonyms,
                    isDefault = entry.isDefault
                )
            }

            val userSynonyms = settingsRepository.userSynonyms.first()
            val merged = defaults.toMutableList()
            for (persisted in userSynonyms) {
                val idx = merged.indexOfFirst {
                    it.canonical.equals(persisted.canonical, ignoreCase = true)
                }
                if (idx >= 0) {
                    val existing = merged[idx]
                    merged[idx] = existing.copy(
                        synonyms = (existing.synonyms + persisted.synonyms).distinct()
                    )
                } else {
                    merged.add(SynonymEntryInfo(persisted.canonical, persisted.synonyms))
                }
            }

            val current = _commands.value
            _commands.value = current.copy(synonymEntries = merged)
        }
    }

    private fun formatCategoryName(category: CoreCommandCategory): String {
        return category.name.lowercase()
            .replace('_', ' ')
            .replaceFirstChar { it.uppercaseChar() }
    }

    /**
     * Refreshes module states by checking actual service/permission status.
     */
    private fun refreshModuleStates() {
        viewModelScope.launch {
            val accessibilityOn = VoiceAvanueAccessibilityService.isEnabled(context)
            val overlayOn = Settings.canDrawOverlays(context)

            val voiceAvanueState = if (accessibilityOn) {
                ServiceState.Running(
                    metadata = mapOf(
                        "engine" to "Android STT",
                        "language" to "en-US"
                    )
                )
            } else {
                ServiceState.Stopped
            }

            val webAvanueState = ServiceState.Ready(
                metadata = mapOf(
                    "browser" to "Available",
                    "tabs" to "0"
                )
            )

            val voiceCursorState = if (overlayOn) {
                ServiceState.Ready(
                    metadata = mapOf("overlay" to "Granted")
                )
            } else {
                ServiceState.Stopped
            }

            _modules.value = listOf(
                ModuleStatus(
                    moduleId = "voiceavanue",
                    displayName = "VoiceTouch\u2122",
                    description = "powered by VoiceOS",
                    state = voiceAvanueState,
                    metadata = (voiceAvanueState as? ServiceState.Running)?.metadata ?: emptyMap()
                ),
                ModuleStatus(
                    moduleId = "webavanue",
                    displayName = "WebAvanue",
                    description = "Voice-enabled web browser",
                    state = webAvanueState,
                    metadata = webAvanueState.metadata
                ),
                ModuleStatus(
                    moduleId = "voicecursor",
                    displayName = "CursorAvanue",
                    description = "VoiceCursor \u2013 Handsfree cursor control",
                    state = voiceCursorState,
                    metadata = (voiceCursorState as? ServiceState.Ready)?.metadata ?: emptyMap()
                )
            )
        }
    }

    /**
     * Refreshes permission states by checking actual system permissions.
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            _permissions.value = PermissionStatus(
                accessibilityEnabled = VoiceAvanueAccessibilityService.isEnabled(context),
                overlayEnabled = Settings.canDrawOverlays(context),
                microphoneGranted = checkMicrophonePermission(),
                batteryOptimized = checkBatteryOptimization(),
                notificationsEnabled = checkNotificationPermission()
            )
        }
    }

    /**
     * Updates the last heard command display.
     * Called from VoiceOSCore when a new command is processed.
     */
    fun updateLastCommand(command: LastHeardCommand) {
        _lastHeardCommand.value = command
    }

    /**
     * Toggles a static command's enabled state by ID.
     * Updates in-memory state + persists to DataStore.
     */
    fun toggleCommand(commandId: String) {
        val current = _commands.value
        var newEnabled = true
        _commands.value = current.copy(
            staticCategories = current.staticCategories.map { category ->
                category.copy(
                    commands = category.commands.map { cmd ->
                        if (cmd.id == commandId) {
                            newEnabled = !cmd.enabled
                            cmd.copy(enabled = newEnabled)
                        } else cmd
                    }
                )
            }
        )
        viewModelScope.launch {
            settingsRepository.setCommandDisabled(commandId, disabled = !newEnabled)
        }
    }

    /**
     * Adds a user-created custom voice command with action binding.
     * For macros, actionType should be MACRO and steps should contain the action chain.
     */
    fun addCustomCommand(
        name: String,
        phrases: List<String>,
        actionType: CommandActionType = CommandActionType.CUSTOM,
        actionTarget: String = "",
        steps: List<MacroStep> = emptyList()
    ) {
        if (name.isBlank() || phrases.isEmpty()) return
        val current = _commands.value
        val newCommand = CustomCommandInfo(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            phrases = phrases,
            actionType = actionType,
            actionTarget = actionTarget,
            steps = steps,
            isActive = true
        )
        _commands.value = current.copy(
            customCommands = current.customCommands + newCommand
        )
    }

    /**
     * Removes a custom command by ID.
     */
    fun removeCustomCommand(id: String) {
        val current = _commands.value
        _commands.value = current.copy(
            customCommands = current.customCommands.filter { it.id != id }
        )
    }

    /**
     * Toggles a custom command's active state.
     */
    fun toggleCustomCommand(id: String) {
        val current = _commands.value
        _commands.value = current.copy(
            customCommands = current.customCommands.map { cmd ->
                if (cmd.id == id) cmd.copy(isActive = !cmd.isActive) else cmd
            }
        )
    }

    /**
     * Adds a verb synonym mapping (canonical → alternatives).
     * Merges with existing entry if canonical already exists.
     * Persists non-default synonyms to DataStore (AVU SYN format).
     */
    fun addSynonym(canonical: String, synonyms: List<String>) {
        if (canonical.isBlank() || synonyms.isEmpty()) return
        val current = _commands.value
        val existing = current.synonymEntries.toMutableList()
        val existingIndex = existing.indexOfFirst {
            it.canonical.equals(canonical, ignoreCase = true)
        }
        if (existingIndex >= 0) {
            val entry = existing[existingIndex]
            existing[existingIndex] = entry.copy(
                synonyms = (entry.synonyms + synonyms).distinct()
            )
        } else {
            existing.add(SynonymEntryInfo(canonical, synonyms))
        }
        _commands.value = current.copy(synonymEntries = existing)
        viewModelScope.launch {
            settingsRepository.saveUserSynonym(canonical, synonyms)
        }
    }

    /**
     * Removes a verb synonym mapping by canonical name.
     * Removes persisted synonym from DataStore.
     */
    fun removeSynonym(canonical: String) {
        val current = _commands.value
        _commands.value = current.copy(
            synonymEntries = current.synonymEntries.filter { it.canonical != canonical }
        )
        viewModelScope.launch {
            settingsRepository.removeUserSynonym(canonical)
        }
    }

    private fun checkMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    private fun checkNotificationPermission(): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }

    fun submitPhraseSuggestion(commandId: String, originalPhrase: String, suggestedPhrase: String, locale: String) {
        viewModelScope.launch {
            phraseSuggestionRepo.insert(
                com.augmentalis.database.dto.PhraseSuggestionDTO(
                    id = 0,
                    commandId = commandId,
                    originalPhrase = originalPhrase,
                    suggestedPhrase = suggestedPhrase,
                    locale = locale,
                    createdAt = System.currentTimeMillis(),
                    status = "pending",
                    source = "user"
                )
            )
        }
    }
}
