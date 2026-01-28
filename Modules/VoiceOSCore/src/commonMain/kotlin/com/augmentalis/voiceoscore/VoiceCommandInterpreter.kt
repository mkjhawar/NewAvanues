@file:Suppress("UNUSED_PARAMETER") // replacePrefix reserved for future rule expansion

/**
 * VoiceCommandInterpreter.kt - Rule-based voice command interpretation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Extensible voice command interpretation system.
 * Open for extension - new rules can be added without modifying existing code.
 */
package com.augmentalis.voiceoscore

/**
 * Interface for voice command interpretation.
 */
interface IVoiceCommandInterpreter {
    /**
     * Interpret a voice command into an action string.
     *
     * @param command The raw voice command
     * @return Interpreted action string, or null if not recognized
     */
    fun interpret(command: String): String?
}

/**
 * Rule for interpreting voice commands.
 */
data class InterpretationRule(
    val patterns: Set<String>,
    val action: String,
    val priority: Int = 0
) {
    /**
     * Check if command matches this rule.
     */
    fun matches(command: String): Boolean = patterns.any { pattern ->
        when {
            pattern.endsWith("*") -> command.startsWith(pattern.dropLast(1))
            pattern.startsWith("*") -> command.endsWith(pattern.drop(1))
            else -> command.contains(pattern)
        }
    }
}

/**
 * Rule-based voice command interpreter.
 *
 * Supports:
 * - Pattern matching with wildcards (* at start/end)
 * - Priority-based rule ordering
 * - Runtime rule addition
 */
class RuleBasedVoiceCommandInterpreter : IVoiceCommandInterpreter {

    private val rules = mutableListOf<InterpretationRule>()

    init {
        // Navigation commands
        addRule(setOf("go back", "back"), "back")
        addRule(setOf("go home", "home"), "home")
        addRule(setOf("scroll up"), "scroll up")
        addRule(setOf("scroll down"), "scroll down")
        addRule(setOf("scroll left"), "scroll left")
        addRule(setOf("scroll right"), "scroll right")
        addRule(setOf("swipe up"), "swipe up")
        addRule(setOf("swipe down"), "swipe down")
        addRule(setOf("swipe left"), "swipe left")
        addRule(setOf("swipe right"), "swipe right")

        // UI commands (prefix patterns)
        addRule(setOf("click *", "tap *"), passthrough = true)
        addRule(setOf("press *"), "click", replacePrefix = "press ")

        // Input commands
        addRule(setOf("type *"), passthrough = true)
        addRule(setOf("enter text *"), "type", replacePrefix = "enter text ")

        // System commands
        addRule(setOf("show notifications", "notifications"), "show notifications")
        addRule(setOf("quick settings"), "quick settings")
        addRule(setOf("recent apps", "recents"), "recents")
    }

    /**
     * Add an interpretation rule.
     */
    fun addRule(patterns: Set<String>, action: String, priority: Int = 0) {
        rules.add(InterpretationRule(patterns, action, priority))
        rules.sortByDescending { it.priority }
    }

    /**
     * Add a passthrough rule (command passes through unchanged).
     */
    private fun addRule(patterns: Set<String>, passthrough: Boolean) {
        if (passthrough) {
            patterns.forEach { pattern ->
                val basePattern = pattern.removeSuffix(" *")
                rules.add(InterpretationRule(setOf(pattern), basePattern, 0))
            }
        }
    }

    /**
     * Add a rule that replaces a prefix.
     */
    private fun addRule(patterns: Set<String>, action: String, replacePrefix: String) {
        patterns.forEach { pattern ->
            rules.add(InterpretationRule(
                setOf(pattern.removeSuffix(" *")),
                action,
                0
            ))
        }
    }

    override fun interpret(command: String): String? {
        val normalizedCommand = command.lowercase().trim()

        // Check prefix replacement rules
        for (rule in rules) {
            if (rule.matches(normalizedCommand)) {
                // Handle passthrough patterns
                for (pattern in rule.patterns) {
                    if (pattern.endsWith("*") && normalizedCommand.startsWith(pattern.dropLast(1).trim())) {
                        return normalizedCommand // Passthrough
                    }
                }
                // Handle prefix replacement
                if (rule.action == "click") {
                    val prefixes = listOf("press ")
                    for (prefix in prefixes) {
                        if (normalizedCommand.startsWith(prefix)) {
                            return "click " + normalizedCommand.removePrefix(prefix)
                        }
                    }
                }
                if (rule.action == "type") {
                    val prefixes = listOf("enter text ")
                    for (prefix in prefixes) {
                        if (normalizedCommand.startsWith(prefix)) {
                            return "type " + normalizedCommand.removePrefix(prefix)
                        }
                    }
                }
                return rule.action
            }
        }

        return null
    }

    /**
     * Get all registered rules.
     */
    fun getRules(): List<InterpretationRule> = rules.toList()

    /**
     * Clear all rules.
     */
    fun clearRules() {
        rules.clear()
    }
}

/**
 * Default interpreter instance.
 */
object DefaultVoiceCommandInterpreter : IVoiceCommandInterpreter {
    private val interpreter = RuleBasedVoiceCommandInterpreter()

    override fun interpret(command: String): String? = interpreter.interpret(command)

    /**
     * Add custom rules to the default interpreter.
     */
    fun addRule(patterns: Set<String>, action: String, priority: Int = 0) {
        interpreter.addRule(patterns, action, priority)
    }
}
