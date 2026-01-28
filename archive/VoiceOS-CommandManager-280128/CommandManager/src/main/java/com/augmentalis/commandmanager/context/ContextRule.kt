/**
 * ContextRule.kt - Context-based activation rules for commands
 * Defines rules that determine when commands should be available
 *
 * Created: 2025-10-09 12:37:32 PDT
 * Part of Week 4 - Context-Aware Commands implementation
 */

package com.augmentalis.commandmanager.context

import com.augmentalis.voiceoscore.CommandDefinition

/**
 * Context rule data class
 * Defines when a command should be activated based on context
 *
 * @param id Unique identifier for this rule
 * @param commandId The ID of the command this rule applies to
 * @param contexts List of contexts in which this command is available
 * @param matchMode How to match multiple contexts (ANY or ALL)
 * @param priority Priority for resolving conflicts (0-100, higher = higher priority)
 */
data class ContextRule(
    val id: String,
    val commandId: String,
    val contexts: List<CommandContext>,
    val matchMode: MatchMode = MatchMode.ANY,
    val priority: Int = 50
) {

    /**
     * Evaluate if this rule allows the command in the given context
     *
     * @param commandDef Command definition to check
     * @param currentContext Current context
     * @return True if command is allowed, false otherwise
     */
    fun evaluate(commandDef: CommandDefinition, currentContext: CommandContext): Boolean {
        // Check if this rule applies to this command
        if (commandDef.id != commandId) {
            return true // Rule doesn't apply, so don't block
        }

        // Flatten composite contexts
        val currentContexts = if (currentContext is CommandContext.Composite) {
            currentContext.flatten()
        } else {
            listOf(currentContext)
        }

        return when (matchMode) {
            MatchMode.ANY -> evaluateAnyMatch(currentContexts)
            MatchMode.ALL -> evaluateAllMatch(currentContexts)
        }
    }

    /**
     * Evaluate ANY match mode
     * Command is available if ANY of the rule's contexts match current context
     */
    private fun evaluateAnyMatch(currentContexts: List<CommandContext>): Boolean {
        for (ruleContext in contexts) {
            for (currentContext in currentContexts) {
                if (contextsMatch(ruleContext, currentContext)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Evaluate ALL match mode
     * Command is available only if ALL rule contexts match current context
     */
    private fun evaluateAllMatch(currentContexts: List<CommandContext>): Boolean {
        for (ruleContext in contexts) {
            var found = false
            for (currentContext in currentContexts) {
                if (contextsMatch(ruleContext, currentContext)) {
                    found = true
                    break
                }
            }
            if (!found) {
                return false // One required context not found
            }
        }
        return true // All required contexts found
    }

    /**
     * Check if two contexts match
     */
    private fun contextsMatch(ruleContext: CommandContext, currentContext: CommandContext): Boolean {
        return when {
            ruleContext is CommandContext.App && currentContext is CommandContext.App -> {
                ruleContext.matches(currentContext)
            }

            ruleContext is CommandContext.Screen && currentContext is CommandContext.Screen -> {
                ruleContext.screenId == currentContext.screenId
            }

            ruleContext is CommandContext.Time && currentContext is CommandContext.Time -> {
                // Time contexts match if they're in the same time of day
                ruleContext.timeOfDay == currentContext.timeOfDay
            }

            ruleContext is CommandContext.Location && currentContext is CommandContext.Location -> {
                ruleContext.type == currentContext.type
            }

            ruleContext is CommandContext.Activity && currentContext is CommandContext.Activity -> {
                ruleContext.type == currentContext.type
            }

            else -> false
        }
    }
}

/**
 * Match mode for context rules
 */
enum class MatchMode {
    /**
     * ANY mode: Command is available if ANY context matches
     * Example: Command available in Gmail OR Chrome
     */
    ANY,

    /**
     * ALL mode: Command is available only if ALL contexts match
     * Example: Command available only when in Gmail AND during morning hours
     */
    ALL
}

// VoiceCommand removed - use dynamic.VoiceCommand from CommandPersistence.kt instead

/**
 * Context rule builder for easy rule creation
 */
class ContextRuleBuilder {

    private var id: String? = null
    private var commandId: String? = null
    private val contexts = mutableListOf<CommandContext>()
    private var matchMode = MatchMode.ANY
    private var priority = 50

    /**
     * Set rule ID
     */
    fun id(id: String): ContextRuleBuilder {
        this.id = id
        return this
    }

    /**
     * Set command ID
     */
    fun commandId(commandId: String): ContextRuleBuilder {
        this.commandId = commandId
        return this
    }

    /**
     * Add app context
     */
    fun inApp(packageName: String, activityName: String? = null): ContextRuleBuilder {
        contexts.add(CommandContext.App(packageName, activityName))
        return this
    }

    /**
     * Add screen context
     */
    fun onScreen(screenId: String): ContextRuleBuilder {
        contexts.add(CommandContext.Screen(screenId))
        return this
    }

    /**
     * Add time context
     */
    fun atTime(hour: Int, dayOfWeek: Int): ContextRuleBuilder {
        contexts.add(CommandContext.Time(hour, dayOfWeek))
        return this
    }

    /**
     * Add time of day context
     */
    fun duringTimeOfDay(timeOfDay: TimeOfDay): ContextRuleBuilder {
        val hour = when (timeOfDay) {
            TimeOfDay.EARLY_MORNING -> 6
            TimeOfDay.MORNING -> 9
            TimeOfDay.AFTERNOON -> 14
            TimeOfDay.EVENING -> 18
            TimeOfDay.NIGHT -> 21
            TimeOfDay.LATE_NIGHT -> 1
        }
        contexts.add(CommandContext.Time(hour, 0))
        return this
    }

    /**
     * Add location context
     */
    fun atLocation(type: LocationType): ContextRuleBuilder {
        contexts.add(CommandContext.Location(type))
        return this
    }

    /**
     * Add activity context
     */
    fun duringActivity(type: ActivityType, confidence: Float = 1.0f): ContextRuleBuilder {
        contexts.add(CommandContext.Activity(type, confidence))
        return this
    }

    /**
     * Set match mode to ANY
     */
    fun matchAny(): ContextRuleBuilder {
        this.matchMode = MatchMode.ANY
        return this
    }

    /**
     * Set match mode to ALL
     */
    fun matchAll(): ContextRuleBuilder {
        this.matchMode = MatchMode.ALL
        return this
    }

    /**
     * Set priority
     */
    fun priority(priority: Int): ContextRuleBuilder {
        this.priority = priority.coerceIn(0, 100)
        return this
    }

    /**
     * Build the context rule
     */
    fun build(): ContextRule {
        val ruleId = requireNotNull(id) { "Rule ID is required" }
        val cmdId = requireNotNull(commandId) { "Command ID is required" }
        require(contexts.isNotEmpty()) { "At least one context is required" }

        return ContextRule(
            id = ruleId,
            commandId = cmdId,
            contexts = contexts,
            matchMode = matchMode,
            priority = priority
        )
    }
}

/**
 * DSL function for creating context rules
 */
fun contextRule(id: String, commandId: String, block: ContextRuleBuilder.() -> Unit): ContextRule {
    return ContextRuleBuilder()
        .id(id)
        .commandId(commandId)
        .apply(block)
        .build()
}

/**
 * Example context rules
 */
object ExampleContextRules {

    /**
     * Gmail-specific commands
     * Only available when Gmail app is open
     */
    fun gmailCommands(commandId: String): ContextRule {
        return contextRule("gmail_commands", commandId) {
            inApp("com.google.android.gm")
            matchAny()
            priority(70)
        }
    }

    /**
     * Morning routine commands
     * Only available during morning hours (6-10 AM)
     */
    fun morningCommands(commandId: String): ContextRule {
        return contextRule("morning_commands", commandId) {
            duringTimeOfDay(TimeOfDay.MORNING)
            matchAny()
            priority(60)
        }
    }

    /**
     * Driving mode commands
     * Only available when user is driving
     * Higher priority for safety
     */
    fun drivingCommands(commandId: String): ContextRule {
        return contextRule("driving_commands", commandId) {
            duringActivity(ActivityType.DRIVING, confidence = 0.7f)
            matchAny()
            priority(90) // High priority for safety
        }
    }

    /**
     * Home automation commands
     * Only available when user is at home
     */
    fun homeCommands(commandId: String): ContextRule {
        return contextRule("home_commands", commandId) {
            atLocation(LocationType.HOME)
            matchAny()
            priority(60)
        }
    }

    /**
     * Work productivity commands
     * Only available at work during work hours
     */
    fun workCommands(commandId: String): ContextRule {
        return contextRule("work_commands", commandId) {
            atLocation(LocationType.WORK)
            duringTimeOfDay(TimeOfDay.MORNING)
            matchAll() // Both conditions must be true
            priority(70)
        }
    }

    /**
     * Text editing commands
     * Only available when a text field is focused
     */
    fun textEditingCommands(commandId: String): ContextRule {
        return contextRule("text_editing_commands", commandId) {
            onScreen("editable_field")
            matchAny()
            priority(80)
        }
    }
}

/**
 * Context rule registry for managing rules
 */
class ContextRuleRegistry {

    private val rules = mutableListOf<ContextRule>()

    /**
     * Register a new rule
     */
    fun registerRule(rule: ContextRule) {
        // Remove existing rule with same ID
        rules.removeAll { it.id == rule.id }
        rules.add(rule)
        android.util.Log.d("ContextRuleRegistry", "Registered rule: ${rule.id}")
    }

    /**
     * Unregister a rule by ID
     */
    fun unregisterRule(ruleId: String): Boolean {
        val removed = rules.removeAll { it.id == ruleId }
        if (removed) {
            android.util.Log.d("ContextRuleRegistry", "Unregistered rule: $ruleId")
        }
        return removed
    }

    /**
     * Get all rules for a command
     */
    fun getRulesForCommand(commandId: String): List<ContextRule> {
        return rules.filter { it.commandId == commandId }
    }

    /**
     * Get all rules
     */
    fun getAllRules(): List<ContextRule> {
        return rules.toList()
    }

    /**
     * Clear all rules
     */
    fun clearRules() {
        rules.clear()
        android.util.Log.d("ContextRuleRegistry", "Cleared all rules")
    }

    /**
     * Evaluate all rules for a command in current context
     * Returns true if command is allowed by all rules
     */
    fun evaluateRules(commandDef: CommandDefinition, currentContext: CommandContext): Boolean {
        val commandRules = getRulesForCommand(commandDef.id)

        if (commandRules.isEmpty()) {
            return true // No rules = always allowed
        }

        // All rules must pass for command to be allowed
        return commandRules.all { rule ->
            rule.evaluate(commandDef, currentContext)
        }
    }

    /**
     * Get rule conflicts
     * Returns pairs of rules that conflict (same command, different contexts)
     */
    fun getConflicts(): List<Pair<ContextRule, ContextRule>> {
        val conflicts = mutableListOf<Pair<ContextRule, ContextRule>>()

        for (i in rules.indices) {
            for (j in i + 1 until rules.size) {
                val rule1 = rules[i]
                val rule2 = rules[j]

                // Check if rules conflict (same command, overlapping contexts)
                if (rule1.commandId == rule2.commandId) {
                    // Rules for same command with different priorities might conflict
                    if (rule1.priority != rule2.priority) {
                        conflicts.add(Pair(rule1, rule2))
                    }
                }
            }
        }

        return conflicts
    }
}
