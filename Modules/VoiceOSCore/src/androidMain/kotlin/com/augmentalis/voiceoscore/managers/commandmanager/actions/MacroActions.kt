/**
 * MacroActions.kt - Macro command actions
 * Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/MacroActions.kt
 *
 * Created: 2025-10-11 00:30 PDT
 * Module: CommandManager
 *
 * Purpose: Pre-defined macro commands for common workflows
 * Reference: IMPLEMENTATION-INSTRUCTIONS-251010-1734.md - Task 3.3 (Q9)
 *
 * Decision: Q9 - Option D (Hybrid: Pre-defined now, user-created later)
 *
 * Implemented:
 *   - Enhancement 1: Macro Categories (organize by use case)
 *   - Enhancement 3: Macro Variables (parameterized macros)
 *
 * Stubbed for V2:
 *   - Enhancement 2: Macro Sharing (TODO)
 *   - Enhancement 4: Macro Conditions (if/then logic - TODO)
 *   - Enhancement 5: Macro Marketplace (community macros - TODO)
 */

package com.augmentalis.voiceoscore.managers.commandmanager.actions

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.*
import kotlinx.coroutines.delay

/**
 * Macro actions implementation
 * Handles pre-defined macro commands with sequential execution
 *
 * ROT Analysis:
 * - Each macro step executes exactly as individual commands would
 * - 100% functional equivalency maintained
 * - No state changes between pre-macro and post-macro refactoring
 */
class MacroActions(
    private val commandExecutor: CommandExecutor
) : BaseAction() {

    companion object {
        private const val TAG = "MacroActions"

        // Pre-defined macro commands
        const val MACRO_SELECT_ALL_COPY = "select all and copy"
        const val MACRO_SELECT_ALL_CUT = "select all and cut"
        const val MACRO_PASTE_ENTER = "paste and enter"
        const val MACRO_SCREENSHOT_SHARE = "take screenshot and share"

        // Timing constants
        private const val STEP_DELAY_MS = 200L  // Delay between macro steps
        private const val LONG_STEP_DELAY_MS = 500L  // For operations that need more time
    }

    /**
     * Execute macro command
     * Sequentially executes each step in the macro
     */
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        val macroName = command.text?.lowercase()

        // Find matching macro
        val macro = PREDEFINED_MACROS[macroName]
            ?: return createErrorResult(
                command,
                ErrorCode.COMMAND_NOT_FOUND,
                "Unknown macro: $macroName"
            )

        android.util.Log.d(TAG, "Executing macro: ${macro.name} (${macro.steps.size} steps)")

        return executeMacro(command, macro, accessibilityService, context)
    }

    /**
     * Execute macro steps sequentially
     * Returns success only if ALL steps succeed
     */
    private suspend fun executeMacro(
        originalCommand: Command,
        macro: Macro,
        @Suppress("UNUSED_PARAMETER") accessibilityService: AccessibilityService?,
        @Suppress("UNUSED_PARAMETER") context: Context
    ): CommandResult {
        val executionStart = System.currentTimeMillis()
        val stepResults = mutableListOf<StepResult>()

        try {
            // Execute each step in sequence
            for ((index, step) in macro.steps.withIndex()) {
                android.util.Log.d(TAG, "Executing macro step ${index + 1}/${macro.steps.size}: $step")

                // Resolve variables in step (if any)
                val resolvedStep = resolveVariables(step, originalCommand.parameters, macro.parameters)

                // Create command for this step
                val stepCommand = Command(
                    id = "${originalCommand.id}_step_$index",
                    text = resolvedStep,
                    source = originalCommand.source,
                    context = originalCommand.context,
                    parameters = extractStepParameters(resolvedStep, originalCommand.parameters),
                    timestamp = System.currentTimeMillis(),
                    confidence = originalCommand.confidence
                )

                // Execute step
                val stepResult = commandExecutor.execute(stepCommand)

                // Track result
                stepResults.add(
                    StepResult(
                        stepNumber = index + 1,
                        stepText = resolvedStep,
                        success = stepResult.success,
                        error = stepResult.error?.message
                    )
                )

                // Check if step failed
                if (!stepResult.success) {
                    android.util.Log.w(TAG, "Macro step ${index + 1} failed: ${stepResult.error?.message}")
                    return createErrorResult(
                        originalCommand,
                        ErrorCode.EXECUTION_FAILED,
                        "Macro failed at step ${index + 1}: ${stepResult.error?.message}",
                        data = MacroExecutionData(
                            macroName = macro.name,
                            totalSteps = macro.steps.size,
                            completedSteps = index,
                            stepResults = stepResults
                        )
                    )
                }

                // Add delay between steps (except for last step)
                if (index < macro.steps.size - 1) {
                    val delayTime = if (requiresLongDelay(step)) {
                        LONG_STEP_DELAY_MS
                    } else {
                        STEP_DELAY_MS
                    }
                    delay(delayTime)
                }
            }

            // All steps succeeded
            val executionTime = System.currentTimeMillis() - executionStart
            android.util.Log.d(TAG, "Macro completed successfully: ${macro.name} (${executionTime}ms)")

            return createSuccessResult(
                originalCommand,
                "Macro completed: ${macro.name}",
                data = MacroExecutionData(
                    macroName = macro.name,
                    totalSteps = macro.steps.size,
                    completedSteps = macro.steps.size,
                    stepResults = stepResults,
                    executionTimeMs = executionTime
                )
            )

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Macro execution failed: ${macro.name}", e)
            return createErrorResult(
                originalCommand,
                ErrorCode.EXECUTION_FAILED,
                "Macro execution failed: ${e.message}",
                data = MacroExecutionData(
                    macroName = macro.name,
                    totalSteps = macro.steps.size,
                    completedSteps = stepResults.size,
                    stepResults = stepResults
                )
            )
        }
    }

    /**
     * Create error result with optional data
     */
    private fun createErrorResult(
        command: Command,
        errorCode: ErrorCode,
        message: String,
        data: Any? = null
    ): CommandResult {
        return CommandResult(
            success = false,
            command = command,
            error = CommandError(errorCode, message),
            data = data
        )
    }

    /**
     * Resolve variables in macro step text
     * Example: "open [app]" with parameters {app: "chrome"} -> "open chrome"
     */
    private fun resolveVariables(
        stepText: String,
        commandParameters: Map<String, Any>,
        macroParameters: List<MacroParameter>
    ): String {
        var resolved = stepText

        // Find all [variable] patterns
        val variablePattern = Regex("\\[([a-zA-Z_]+)]")
        val matches = variablePattern.findAll(stepText)

        for (match in matches) {
            val variableName = match.groupValues[1]
            val variableValue = commandParameters[variableName]?.toString()
                ?: macroParameters.find { it.name == variableName }?.defaultValue

            if (variableValue != null) {
                resolved = resolved.replace("[${variableName}]", variableValue)
            }
        }

        return resolved
    }

    /**
     * Extract parameters for a step from original command parameters
     */
    private fun extractStepParameters(
        @Suppress("UNUSED_PARAMETER") stepText: String,
        commandParameters: Map<String, Any>
    ): Map<String, Any> {
        // For now, pass through all parameters
        // In future, could filter based on step requirements
        return commandParameters
    }

    /**
     * Check if a step requires a longer delay
     * Some operations (like screenshots, app launches) need more time
     */
    private fun requiresLongDelay(step: String): Boolean {
        val lowercaseStep = step.lowercase()
        return lowercaseStep.contains("screenshot") ||
                lowercaseStep.contains("launch") ||
                lowercaseStep.contains("open") ||
                lowercaseStep.contains("share")
    }

    // ========== Enhancement 1: Macro Categories ==========

    /**
     * Macro category enumeration
     * Organizes macros by use case for better discoverability
     */
    enum class MacroCategory(val displayName: String, val description: String) {
        EDITING(
            "Editing",
            "Text and content editing workflows"
        ),
        NAVIGATION(
            "Navigation",
            "App and screen navigation workflows"
        ),
        ACCESSIBILITY(
            "Accessibility",
            "Accessibility-focused workflows"
        ),
        PRODUCTIVITY(
            "Productivity",
            "General productivity workflows"
        ),
        UTILITY(
            "Utility",
            "General utility macros"
        )
    }

    /**
     * Macro data class
     * Represents a pre-defined macro with steps and metadata
     */
    data class Macro(
        val name: String,
        val steps: List<String>,
        val category: MacroCategory,
        val description: String,
        val parameters: List<MacroParameter> = emptyList()
    )

    // ========== Enhancement 3: Macro Variables ==========

    /**
     * Macro parameter data class
     * Defines variables that can be used in macro steps
     */
    data class MacroParameter(
        val name: String,
        val type: MacroParameterType,
        val description: String,
        val defaultValue: String? = null,
        val required: Boolean = false
    )

    /**
     * Parameter type enumeration
     * Defines the type of macro parameter
     */
    enum class MacroParameterType {
        APP,        // Application name/package
        TEXT,       // Text string
        NUMBER      // Numeric value
    }

    /**
     * Step result tracking
     * Used for detailed macro execution reporting
     */
    data class StepResult(
        val stepNumber: Int,
        val stepText: String,
        val success: Boolean,
        val error: String? = null
    )

    /**
     * Macro execution data
     * Returned in CommandResult.data for debugging and analytics
     */
    data class MacroExecutionData(
        val macroName: String,
        val totalSteps: Int,
        val completedSteps: Int,
        val stepResults: List<StepResult>,
        val executionTimeMs: Long = 0
    )

    // ========== Pre-defined Macros ==========

    /**
     * Collection of pre-defined macros
     * Maps macro name (lowercase) to Macro definition
     */
    private val PREDEFINED_MACROS = mapOf(
        // Editing macros
        MACRO_SELECT_ALL_COPY to Macro(
            name = "Select All and Copy",
            steps = listOf("select all", "copy"),
            category = MacroCategory.EDITING,
            description = "Select all text in the current field and copy to clipboard"
        ),

        MACRO_SELECT_ALL_CUT to Macro(
            name = "Select All and Cut",
            steps = listOf("select all", "cut"),
            category = MacroCategory.EDITING,
            description = "Select all text in the current field and cut to clipboard"
        ),

        MACRO_PASTE_ENTER to Macro(
            name = "Paste and Enter",
            steps = listOf("paste", "press enter"),
            category = MacroCategory.EDITING,
            description = "Paste clipboard content and press Enter"
        ),

        // Productivity macros
        MACRO_SCREENSHOT_SHARE to Macro(
            name = "Take Screenshot and Share",
            steps = listOf("screenshot", "share"),
            category = MacroCategory.PRODUCTIVITY,
            description = "Take a screenshot and open share dialog"
        )
    )

    /**
     * Get all available macros
     * Returns list of all pre-defined macros
     */
    fun getAllMacros(): List<Macro> {
        return PREDEFINED_MACROS.values.toList()
    }

    /**
     * Get macros by category
     * Returns macros filtered by category
     */
    fun getMacrosByCategory(category: MacroCategory): List<Macro> {
        return PREDEFINED_MACROS.values.filter { it.category == category }
    }

    /**
     * Get macro by name
     * Returns macro definition if found, null otherwise
     */
    fun getMacro(name: String): Macro? {
        return PREDEFINED_MACROS[name.lowercase()]
    }

    // ========== Enhancement 2: Macro Sharing ==========

    /**
     * Export macro to shareable JSON format
     * Can be shared via QR code, file, or cloud
     */
    fun shareMacro(macro: Macro): String {
        val json = org.json.JSONObject().apply {
            put("name", macro.name)
            put("description", macro.description)
            put("category", macro.category.name)
            put("steps", org.json.JSONArray(macro.steps))
            put("exported_at", System.currentTimeMillis())
            put("version", "2.0")
        }
        return json.toString()
    }

    /**
     * Import macro from shared JSON data
     */
    fun importMacro(macroData: String): Macro {
        return try {
            val json = org.json.JSONObject(macroData)
            val stepsArray = json.getJSONArray("steps")
            val steps = mutableListOf<String>()
            for (i in 0 until stepsArray.length()) {
                steps.add(stepsArray.getString(i))
            }

            val importedMacro = Macro(
                name = json.getString("name"),
                description = json.optString("description", "Imported macro"),
                category = try {
                    MacroCategory.valueOf(json.getString("category"))
                } catch (e: Exception) {
                    MacroCategory.UTILITY
                },
                steps = steps
            )

            // Store imported macro
            importedMacros[importedMacro.name.lowercase()] = importedMacro
            Log.i(TAG, "Imported macro: ${importedMacro.name}")

            importedMacro
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import macro", e)
            throw IllegalArgumentException("Invalid macro data: ${e.message}")
        }
    }

    // Storage for imported macros
    private val importedMacros = mutableMapOf<String, Macro>()

    // ========== Enhancement 4: Macro Conditions ==========

    /**
     * Conditional macro with if/else/loop support
     */
    data class ConditionalMacro(
        val name: String,
        val steps: List<ConditionalStep>,
        val category: MacroCategory,
        val description: String
    )

    data class ConditionalStep(
        val type: StepType,
        val condition: String? = null,
        val action: String? = null
    )

    enum class StepType {
        ACTION,
        IF,
        ELSE,
        ENDIF,
        WHILE,
        ENDWHILE,
        REPEAT
    }

    // Condition evaluators
    private val conditionEvaluators = mapOf<String, () -> Boolean>(
        "clipboard_has_text" to { true }, // Placeholder - would check clipboard state
        "wifi_enabled" to { true }, // Placeholder - would check actual WiFi state
        "bluetooth_enabled" to { true }, // Placeholder
        "screen_on" to { true }, // Placeholder
        "battery_low" to { false } // Placeholder
    )

    /**
     * Execute conditional macro with if/else/loop support
     */
    suspend fun executeConditionalMacro(macro: ConditionalMacro): CommandResult {
        Log.d(TAG, "Executing conditional macro: ${macro.name}")

        val executedSteps = mutableListOf<String>()
        var stepIndex = 0
        val skipStack = mutableListOf<Boolean>() // Track if we should skip current block

        while (stepIndex < macro.steps.size) {
            val step = macro.steps[stepIndex]
            val shouldSkip = skipStack.isNotEmpty() && skipStack.last()

            when (step.type) {
                StepType.ACTION -> {
                    if (!shouldSkip && step.action != null) {
                        // Execute the action
                        val command = Command(
                            id = "macro_${macro.name}_$stepIndex",
                            text = step.action,
                            source = CommandSource.VOICE,
                            confidence = 1.0f,
                            timestamp = System.currentTimeMillis()
                        )

                        val result = commandExecutor?.execute(command)
                        if (result?.success != true) {
                            Log.w(TAG, "Conditional macro step failed: ${step.action}")
                            return CommandResult(
                                success = false,
                                command = command,
                                error = CommandError(ErrorCode.EXECUTION_FAILED, "Step failed: ${step.action}")
                            )
                        }
                        executedSteps.add(step.action)
                        delay(STEP_DELAY_MS)
                    }
                }

                StepType.IF -> {
                    val conditionMet = evaluateCondition(step.condition ?: "")
                    skipStack.add(!conditionMet)
                    Log.d(TAG, "IF condition '${step.condition}' = $conditionMet")
                }

                StepType.ELSE -> {
                    if (skipStack.isNotEmpty()) {
                        // Flip the skip state
                        val current = skipStack.removeLast()
                        skipStack.add(!current)
                    }
                }

                StepType.ENDIF -> {
                    if (skipStack.isNotEmpty()) {
                        skipStack.removeLast()
                    }
                }

                StepType.WHILE -> {
                    val conditionMet = evaluateCondition(step.condition ?: "")
                    if (!conditionMet) {
                        // Skip to ENDWHILE
                        stepIndex = findEndWhile(macro.steps, stepIndex)
                    }
                }

                StepType.ENDWHILE -> {
                    // Jump back to WHILE to re-evaluate
                    stepIndex = findWhile(macro.steps, stepIndex) - 1
                }

                StepType.REPEAT -> {
                    // REPEAT [n] - repeat the previous action n times
                    val times = step.condition?.toIntOrNull() ?: 1
                    if (executedSteps.isNotEmpty()) {
                        val lastAction = executedSteps.last()
                        repeat(times - 1) {
                            val command = Command(
                                id = "macro_${macro.name}_repeat_$it",
                                text = lastAction,
                                source = CommandSource.VOICE,
                                confidence = 1.0f,
                                timestamp = System.currentTimeMillis()
                            )
                            commandExecutor?.execute(command)
                            delay(STEP_DELAY_MS)
                        }
                    }
                }
            }

            stepIndex++
        }

        Log.i(TAG, "Conditional macro '${macro.name}' completed: ${executedSteps.size} steps executed")

        return CommandResult(
            success = true,
            command = Command(
                id = "conditional_${macro.name}",
                text = macro.name,
                source = CommandSource.VOICE,
                confidence = 1.0f,
                timestamp = System.currentTimeMillis()
            ),
            response = "Executed ${executedSteps.size} steps"
        )
    }

    private fun evaluateCondition(condition: String): Boolean {
        // Check built-in conditions
        val cleanCondition = condition.trim().lowercase().removeSurrounding("[", "]")
        return conditionEvaluators[cleanCondition]?.invoke() ?: false
    }

    private fun findEndWhile(steps: List<ConditionalStep>, fromIndex: Int): Int {
        var depth = 1
        for (i in (fromIndex + 1) until steps.size) {
            when (steps[i].type) {
                StepType.WHILE -> depth++
                StepType.ENDWHILE -> {
                    depth--
                    if (depth == 0) return i
                }
                else -> {}
            }
        }
        return steps.size - 1
    }

    private fun findWhile(steps: List<ConditionalStep>, fromIndex: Int): Int {
        var depth = 1
        for (i in (fromIndex - 1) downTo 0) {
            when (steps[i].type) {
                StepType.ENDWHILE -> depth++
                StepType.WHILE -> {
                    depth--
                    if (depth == 0) return i
                }
                else -> {}
            }
        }
        return 0
    }

    // ========== Enhancement 5: Macro Marketplace (STUBBED for V2) ==========

    /**
     * TODO: Implement community macro marketplace
     * Enhancement 5 - Deferred to V2
     *
     * Planned features:
     * - Browse community-created macros
     * - Search by category, rating, popularity
     * - Download and install macros
     * - Rate and review macros
     * - Upload custom macros (requires user-created macro support)
     * - Macro verification and security scanning
     * - Automatic updates for installed macros
     *
     * Security considerations:
     * - Signature verification for marketplace macros
     * - Sandboxed execution for untrusted macros
     * - Permission model for macro capabilities
     * - User approval before executing downloaded macros
     */
    data class MarketplaceMacro(
        val id: String,
        val macro: Macro,
        val author: String,
        val version: String,
        val downloads: Int,
        val rating: Float,
        val verified: Boolean
    )

    /**
     * Browse macro marketplace
     * Enhancement 5 - Deferred to V2
     * @return Empty list until marketplace is implemented
     * @deprecated Marketplace not implemented. Planned for V2.
     */
    @Deprecated(
        message = "Macro marketplace not yet implemented - planned for V2",
        level = DeprecationLevel.WARNING
    )
    suspend fun browseMacroMarketplace(
        @Suppress("UNUSED_PARAMETER") category: MacroCategory? = null,
        @Suppress("UNUSED_PARAMETER") searchQuery: String? = null
    ): List<MarketplaceMacro> {
        Log.w(TAG, "Macro marketplace not yet implemented (V2 feature)")
        return emptyList()
    }

    /**
     * Download macro from marketplace
     * Enhancement 5 - Deferred to V2
     * @return Stub macro until marketplace is implemented
     * @deprecated Marketplace not implemented. Planned for V2.
     */
    @Deprecated(
        message = "Macro marketplace not yet implemented - planned for V2",
        level = DeprecationLevel.WARNING
    )
    suspend fun downloadMacro(@Suppress("UNUSED_PARAMETER") macroId: String): Macro? {
        Log.w(TAG, "Macro download not yet implemented (V2 feature)")
        // Return null instead of throwing - caller should handle null case
        return null
    }

    /**
     * Upload custom macro to marketplace
     * Enhancement 5 - Deferred to V2
     * @return null until marketplace is implemented (caller should check for null)
     * @deprecated Marketplace not implemented. Planned for V2.
     */
    @Deprecated(
        message = "Macro marketplace not yet implemented - planned for V2",
        level = DeprecationLevel.WARNING
    )
    suspend fun uploadMacro(@Suppress("UNUSED_PARAMETER") macro: Macro): String? {
        Log.w(TAG, "Macro upload not yet implemented (V2 feature)")
        // Return null instead of throwing - caller should handle null case
        return null
    }
}

/**
 * Command executor interface
 * Abstraction for executing individual commands
 * Injected dependency to avoid circular references
 */
interface CommandExecutor {
    suspend fun execute(command: Command): CommandResult
}
