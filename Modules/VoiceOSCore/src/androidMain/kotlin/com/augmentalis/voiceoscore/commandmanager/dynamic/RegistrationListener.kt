/**
 * RegistrationListener.kt - Callback interface for command registration events
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Provides event-driven notifications for command lifecycle changes
 *
 * @since VOS4 Week 4
 * @author VOS4 Development Team
 */

package com.augmentalis.voiceoscore.commandmanager.dynamic

/**
 * Listener interface for command registration events
 *
 * Implementations can receive callbacks when commands are registered,
 * unregistered, or when conflicts are detected.
 */
interface RegistrationListener {

    /**
     * Called when a command is successfully registered
     *
     * @param command The command that was registered
     * @param namespace The namespace the command was registered in
     */
    fun onCommandRegistered(command: VoiceCommand, namespace: String) {
        // Default implementation does nothing
    }

    /**
     * Called when a command is successfully unregistered
     *
     * @param commandId ID of the command that was unregistered
     * @param namespace The namespace the command was in
     */
    fun onCommandUnregistered(commandId: String, namespace: String) {
        // Default implementation does nothing
    }

    /**
     * Called when a conflict is detected during command registration
     *
     * @param conflict Information about the detected conflict
     * @param newCommand The command being registered that caused the conflict
     */
    fun onConflictDetected(conflict: ConflictInfo, newCommand: VoiceCommand) {
        // Default implementation does nothing
    }

    /**
     * Called when a command is updated (metadata, priority, etc.)
     *
     * @param commandId ID of the command that was updated
     * @param oldCommand Previous state of the command
     * @param newCommand New state of the command
     */
    fun onCommandUpdated(commandId: String, oldCommand: VoiceCommand, newCommand: VoiceCommand) {
        // Default implementation does nothing
    }

    /**
     * Called when a command is enabled or disabled
     *
     * @param commandId ID of the command
     * @param enabled New enabled state
     */
    fun onCommandEnabledChanged(commandId: String, enabled: Boolean) {
        // Default implementation does nothing
    }

    /**
     * Called when a conflict is resolved
     *
     * @param conflict The conflict that was resolved
     * @param resolution How the conflict was resolved
     */
    fun onConflictResolved(conflict: ConflictInfo, resolution: ConflictResolutionResult) {
        // Default implementation does nothing
    }

    /**
     * Called when all commands in a namespace are cleared
     *
     * @param namespace The namespace that was cleared
     * @param commandCount Number of commands that were removed
     */
    fun onNamespaceCleared(namespace: String, commandCount: Int) {
        // Default implementation does nothing
    }

    /**
     * Called when a command execution fails
     *
     * @param commandId ID of the command
     * @param error The error that occurred
     */
    fun onCommandExecutionFailed(commandId: String, error: CommandResult.Error) {
        // Default implementation does nothing
    }

    /**
     * Called when registry statistics change significantly
     *
     * @param statistics Current registry statistics
     */
    fun onStatisticsChanged(statistics: RegistryStatistics) {
        // Default implementation does nothing
    }
}

/**
 * Abstract base class for registration listeners with selective overrides
 *
 * Extend this class and override only the methods you're interested in.
 */
abstract class RegistrationListenerAdapter : RegistrationListener {
    // All methods have default implementations in interface
}

/**
 * Logging listener that logs all registration events
 *
 * Useful for debugging and monitoring
 */
class LoggingRegistrationListener(
    private val tag: String = "RegistrationListener",
    private val verbose: Boolean = false
) : RegistrationListener {

    override fun onCommandRegistered(command: VoiceCommand, namespace: String) {
        android.util.Log.i(tag, "Command registered: ${command.id} in namespace '$namespace'")
        if (verbose) {
            android.util.Log.d(tag, "  Phrases: ${command.phrases}")
            android.util.Log.d(tag, "  Priority: ${command.priority}")
        }
    }

    override fun onCommandUnregistered(commandId: String, namespace: String) {
        android.util.Log.i(tag, "Command unregistered: $commandId from namespace '$namespace'")
    }

    override fun onConflictDetected(conflict: ConflictInfo, newCommand: VoiceCommand) {
        android.util.Log.w(
            tag,
            "Conflict detected [${conflict.severity}]: ${conflict.getDescription()}"
        )
        if (verbose) {
            android.util.Log.d(tag, "  Affected commands: ${conflict.affectedCommands}")
            android.util.Log.d(tag, "  New command: ${newCommand.id}")
        }
    }

    override fun onCommandUpdated(commandId: String, oldCommand: VoiceCommand, newCommand: VoiceCommand) {
        android.util.Log.i(tag, "Command updated: $commandId")
        if (verbose) {
            if (oldCommand.priority != newCommand.priority) {
                android.util.Log.d(tag, "  Priority: ${oldCommand.priority} -> ${newCommand.priority}")
            }
            if (oldCommand.enabled != newCommand.enabled) {
                android.util.Log.d(tag, "  Enabled: ${oldCommand.enabled} -> ${newCommand.enabled}")
            }
        }
    }

    override fun onCommandEnabledChanged(commandId: String, enabled: Boolean) {
        android.util.Log.i(tag, "Command ${if (enabled) "enabled" else "disabled"}: $commandId")
    }

    override fun onConflictResolved(conflict: ConflictInfo, resolution: ConflictResolutionResult) {
        when (resolution) {
            is ConflictResolutionResult.Resolved ->
                android.util.Log.i(tag, "Conflict resolved: ${conflict.conflictType}")
            is ConflictResolutionResult.Failed ->
                android.util.Log.w(tag, "Conflict resolution failed: ${resolution.reason}")
            is ConflictResolutionResult.Partial ->
                android.util.Log.w(
                    tag,
                    "Conflict partially resolved: ${resolution.remainingConflicts.size} conflicts remain"
                )
        }
    }

    override fun onNamespaceCleared(namespace: String, commandCount: Int) {
        android.util.Log.i(tag, "Namespace cleared: '$namespace' ($commandCount commands removed)")
    }

    override fun onCommandExecutionFailed(commandId: String, error: CommandResult.Error) {
        android.util.Log.e(
            tag,
            "Command execution failed: $commandId - ${error.message}",
            error.cause
        )
    }

    override fun onStatisticsChanged(statistics: RegistryStatistics) {
        if (verbose) {
            android.util.Log.d(tag, "Registry statistics changed:")
            android.util.Log.d(tag, "  Total commands: ${statistics.totalCommands}")
            android.util.Log.d(tag, "  Namespaces: ${statistics.namespaceCount}")
            android.util.Log.d(tag, "  Enabled: ${statistics.enabledCommands}")
        }
    }
}

/**
 * Composite listener that forwards events to multiple listeners
 *
 * Useful for combining multiple listeners into a single registration
 */
class CompositeRegistrationListener(
    private val listeners: List<RegistrationListener>
) : RegistrationListener {

    constructor(vararg listeners: RegistrationListener) : this(listeners.toList())

    override fun onCommandRegistered(command: VoiceCommand, namespace: String) {
        listeners.forEach { it.onCommandRegistered(command, namespace) }
    }

    override fun onCommandUnregistered(commandId: String, namespace: String) {
        listeners.forEach { it.onCommandUnregistered(commandId, namespace) }
    }

    override fun onConflictDetected(conflict: ConflictInfo, newCommand: VoiceCommand) {
        listeners.forEach { it.onConflictDetected(conflict, newCommand) }
    }

    override fun onCommandUpdated(commandId: String, oldCommand: VoiceCommand, newCommand: VoiceCommand) {
        listeners.forEach { it.onCommandUpdated(commandId, oldCommand, newCommand) }
    }

    override fun onCommandEnabledChanged(commandId: String, enabled: Boolean) {
        listeners.forEach { it.onCommandEnabledChanged(commandId, enabled) }
    }

    override fun onConflictResolved(conflict: ConflictInfo, resolution: ConflictResolutionResult) {
        listeners.forEach { it.onConflictResolved(conflict, resolution) }
    }

    override fun onNamespaceCleared(namespace: String, commandCount: Int) {
        listeners.forEach { it.onNamespaceCleared(namespace, commandCount) }
    }

    override fun onCommandExecutionFailed(commandId: String, error: CommandResult.Error) {
        listeners.forEach { it.onCommandExecutionFailed(commandId, error) }
    }

    override fun onStatisticsChanged(statistics: RegistryStatistics) {
        listeners.forEach { it.onStatisticsChanged(statistics) }
    }

    /**
     * Add a listener to this composite
     */
    fun addListener(listener: RegistrationListener): CompositeRegistrationListener {
        return CompositeRegistrationListener(listeners + listener)
    }

    /**
     * Remove a listener from this composite
     */
    fun removeListener(listener: RegistrationListener): CompositeRegistrationListener {
        return CompositeRegistrationListener(listeners - listener)
    }
}

/**
 * Registry statistics for monitoring
 */
data class RegistryStatistics(
    val totalCommands: Int,
    val enabledCommands: Int,
    val disabledCommands: Int,
    val namespaceCount: Int,
    val averagePriority: Float,
    val totalUsageCount: Long,
    val commandsByCategory: Map<CommandCategory, Int>,
    val commandsByPriorityLevel: Map<PriorityLevel, Int>,
    val lastUpdateTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if registry is empty
     */
    fun isEmpty(): Boolean = totalCommands == 0

    /**
     * Get percentage of enabled commands
     */
    fun getEnabledPercentage(): Float =
        if (totalCommands > 0) (enabledCommands.toFloat() / totalCommands) * 100f else 0f

    /**
     * Get most popular category
     */
    fun getMostPopularCategory(): CommandCategory? =
        commandsByCategory.maxByOrNull { it.value }?.key
}
