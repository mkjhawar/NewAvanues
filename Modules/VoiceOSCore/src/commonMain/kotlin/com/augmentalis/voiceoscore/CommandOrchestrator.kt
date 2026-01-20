/**
 * CommandOrchestrator.kt - Core command generation orchestration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-20
 *
 * Pure command generation logic without UI/overlay dependencies.
 * App layer composes this with overlay handling as needed.
 *
 * Responsibilities:
 * - Generate commands from elements using CommandGenerator
 * - Register commands in CommandRegistry
 * - Generate index/label/numeric commands for list items
 * - Build speech phrases for speech engine
 * - Persist static commands via ICommandPersistence
 *
 * NOT responsible for:
 * - Overlay/badge generation (app-specific)
 * - UI command format (app-specific)
 * - Platform-specific APIs (PackageManager, etc.)
 */
package com.augmentalis.voiceoscore

/**
 * Core command generation orchestrator.
 *
 * Generates and registers voice commands for UI elements.
 * Designed to be composed with app-specific overlay handling.
 *
 * @param commandRegistry Registry for command lookup during execution
 * @param commandPersistence Optional persistence for static commands
 */
class CommandOrchestrator(
    private val commandRegistry: CommandRegistry,
    private val commandPersistence: ICommandPersistence = NoOpCommandPersistence
) {
    private val logger = LoggerFactory.getLogger("CommandOrchestrator")

    // AVID assignment tracking for incremental updates
    private val avidAssignments = mutableMapOf<String, Int>()
    private var nextAvidNumber = 1
    private var currentAppPackage: String? = null

    /**
     * Generate voice commands for actionable elements.
     *
     * @param elements UI elements extracted from screen
     * @param packageName App package identifier
     * @param updateSpeechEngine Optional callback to update speech engine grammar
     * @return CoreCommandResult with all generated commands
     */
    fun generateCommands(
        elements: List<ElementInfo>,
        packageName: String,
        updateSpeechEngine: ((List<String>) -> Unit)? = null
    ): CoreCommandResult {
        val commandResults = elements.mapNotNull { element ->
            CommandGenerator.fromElementWithPersistence(element, packageName)
        }

        val staticCommands = commandResults.filter { it.shouldPersist }
        val dynamicCommands = commandResults.filter { !it.shouldPersist }
        val allCommands = commandResults.map { it.command }

        // Register all commands in registry for lookup
        commandRegistry.updateSync(allCommands)

        // Generate additional command types for list items
        val listItems = elements.filter { it.listIndex >= 0 }
        val indexCommands = generateAndRegisterIndexCommands(listItems, packageName)
        val labelCommands = generateAndRegisterLabelCommands(listItems, packageName)
        val numericCommands = generateAndRegisterNumericCommands(listItems, packageName)

        // Build speech phrases
        val speechPhrases = buildSpeechPhrases(
            allCommands, indexCommands, labelCommands, numericCommands
        )
        updateSpeechEngine?.invoke(speechPhrases)

        // Log summary
        logCommandSummary(allCommands, staticCommands, dynamicCommands, indexCommands)

        // Persist static commands (fire-and-forget, errors logged internally)
        val staticQuantizedCommands = staticCommands.map { it.command }
        if (staticQuantizedCommands.isNotEmpty()) {
            persistCommands(staticQuantizedCommands)
        }

        return CoreCommandResult(
            allCommands = allCommands,
            staticCommands = staticQuantizedCommands,
            dynamicCommands = dynamicCommands.map { it.command },
            indexCommands = indexCommands,
            labelCommands = labelCommands,
            numericCommands = numericCommands,
            speechPhrases = speechPhrases
        )
    }

    /**
     * Generate commands incrementally for scroll/content changes.
     *
     * Preserves AVID assignments for existing elements to maintain
     * stable command phrases during scrolling.
     *
     * @param elements Current UI elements
     * @param packageName App package identifier
     * @param existingCommands Commands from previous generation
     * @param updateSpeechEngine Optional callback to update speech engine
     * @return IncrementalCommandResult with merge statistics
     */
    fun generateCommandsIncremental(
        elements: List<ElementInfo>,
        packageName: String,
        existingCommands: List<QuantizedCommand>,
        updateSpeechEngine: ((List<String>) -> Unit)? = null
    ): IncrementalCommandResult {
        // Reset AVID assignments on app change
        if (packageName != currentAppPackage) {
            clearAvidAssignments()
            currentAppPackage = packageName
            logger.v { "App changed to $packageName - reset AVID" }
        }

        val existingByHash = existingCommands.associateBy { it.metadata["elementHash"] }
        var newCount = 0
        var preservedCount = 0
        val mergedCommands = mutableListOf<QuantizedCommand>()

        val commandResults = elements.mapNotNull { element ->
            CommandGenerator.fromElementWithPersistence(element, packageName)
        }

        commandResults.forEach { result ->
            val cmd = result.command
            val hash = cmd.metadata["elementHash"]

            if (hash != null && hash in avidAssignments) {
                val existingCmd = existingByHash[hash]
                mergedCommands.add(existingCmd ?: cmd)
                preservedCount++
            } else {
                if (hash != null) avidAssignments[hash] = nextAvidNumber++
                mergedCommands.add(cmd)
                newCount++
            }
        }

        commandRegistry.updateSync(mergedCommands)

        // Generate additional commands for list items
        val listItems = elements.filter { it.listIndex >= 0 }
        val indexCommands = generateAndRegisterIndexCommands(listItems, packageName)
        val labelCommands = generateAndRegisterLabelCommands(listItems, packageName)
        val numericCommands = generateAndRegisterNumericCommands(listItems, packageName)

        val speechPhrases = buildSpeechPhrases(
            mergedCommands, indexCommands, labelCommands, numericCommands
        )
        updateSpeechEngine?.invoke(speechPhrases)

        logger.v { "Incremental: ${mergedCommands.size} ($newCount new, $preservedCount kept)" }

        return IncrementalCommandResult(
            totalCommands = mergedCommands.size + indexCommands.size + labelCommands.size,
            newCommands = newCount,
            preservedCommands = preservedCount,
            removedCommands = existingCommands.size - preservedCount,
            mergedCommands = mergedCommands,
            speechPhrases = speechPhrases
        )
    }

    /**
     * Clear AVID assignments. Call when switching apps or resetting state.
     */
    fun clearAvidAssignments() {
        avidAssignments.clear()
        nextAvidNumber = 1
        currentAppPackage = null
    }

    /**
     * Get current AVID assignments map (for overlay generation).
     */
    fun getAvidAssignments(): Map<String, Int> = avidAssignments.toMap()

    private fun generateAndRegisterIndexCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val commands = CommandGenerator.generateListIndexCommands(listItems, packageName)
        if (commands.isNotEmpty()) commandRegistry.addAll(commands)
        return commands
    }

    private fun generateAndRegisterLabelCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val commands = CommandGenerator.generateListLabelCommands(listItems, packageName)
        if (commands.isNotEmpty()) commandRegistry.addAll(commands)
        return commands
    }

    private fun generateAndRegisterNumericCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val commands = CommandGenerator.generateNumericCommands(listItems, packageName)
        if (commands.isNotEmpty()) commandRegistry.addAll(commands)
        return commands
    }

    private fun buildSpeechPhrases(
        allCommands: List<QuantizedCommand>,
        indexCommands: List<QuantizedCommand>,
        labelCommands: List<QuantizedCommand>,
        numericCommands: List<QuantizedCommand>
    ): List<String> {
        val staticPhrases = StaticCommandRegistry.allPhrases()
        val dynamicPhrases = allCommands.map { it.phrase } +
            indexCommands.map { it.phrase } +
            labelCommands.map { it.phrase } +
            numericCommands.map { it.phrase }
        return (staticPhrases + dynamicPhrases).distinct()
    }

    private fun persistCommands(commands: List<QuantizedCommand>) {
        // Note: In KMP, we'd use a coroutine scope or expect/actual for async
        // For now, this is a sync placeholder - actual persistence handled by app layer
        logger.v { "Queued ${commands.size} commands for persistence" }
    }

    private fun logCommandSummary(
        allCommands: List<QuantizedCommand>,
        staticCommands: List<CommandGenerator.GeneratedCommandResult>,
        dynamicCommands: List<CommandGenerator.GeneratedCommandResult>,
        indexCommands: List<QuantizedCommand>
    ) {
        logger.v { "Commands: ${allCommands.size} (${staticCommands.size} static)" }
        if (dynamicCommands.isNotEmpty()) {
            logger.v { "Dynamic: ${dynamicCommands.take(3).map { it.command.phrase }}" }
        }
        if (indexCommands.isNotEmpty()) {
            logger.v { "Index: ${indexCommands.take(5).map { it.phrase }}" }
        }
    }
}
