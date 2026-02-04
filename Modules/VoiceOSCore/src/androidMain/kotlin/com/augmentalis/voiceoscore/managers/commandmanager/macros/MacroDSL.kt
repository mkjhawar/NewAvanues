/**
 * MacroDSL.kt - Kotlin DSL for creating command macros
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-09
 *
 * Purpose: Provide intuitive Kotlin DSL for macro creation
 * Key Features:
 * - Type-safe builder pattern
 * - Readable syntax
 * - Compile-time validation
 */
package com.augmentalis.voiceoscore.managers.commandmanager.macros

import java.util.UUID

/**
 * Macro DSL Builder
 *
 * Creates macros using Kotlin DSL syntax for improved readability.
 *
 * ## Example Usage
 *
 * ```kotlin
 * val loginMacro = macro("Login Workflow") {
 *     description = "Automated login process"
 *     trigger = "login"
 *     author = "VOS4 Team"
 *     tags("automation", "login", "user-flow")
 *
 *     step { openApp("com.example.app") }
 *     delay(1000)
 *     step { tap("login button") }
 *     delay(500)
 *
 *     conditional {
 *         condition { screenContains("username") }
 *         then {
 *             step { typeText("username field", "john_doe") }
 *             step { typeText("password field", "password123") }
 *             step { tap("submit") }
 *         }
 *         otherwise {
 *             step { tap("back") }
 *         }
 *     }
 *
 *     waitFor {
 *         condition { screenContains("Welcome") }
 *         timeout(5000)
 *     }
 * }
 * ```
 */
@DslMarker
annotation class MacroDsl

/**
 * Create a macro using DSL
 *
 * @param name Macro name
 * @param builder DSL builder lambda
 * @return CommandMacro instance
 */
fun macro(name: String, builder: MacroBuilder.() -> Unit): CommandMacro {
    val macroBuilder = MacroBuilder(name)
    macroBuilder.builder()
    return macroBuilder.build()
}

/**
 * Macro Builder - DSL builder for CommandMacro
 */
@MacroDsl
class MacroBuilder(private val name: String) {

    private val steps = mutableListOf<MacroStep>()
    private var macroDescription: String = ""
    private var triggerPhrase: String = ""
    private var macroAuthor: String? = null
    private val macroTags = mutableListOf<String>()
    private var enabled: Boolean = true

    /**
     * Set macro description
     */
    var description: String
        get() = macroDescription
        set(value) { macroDescription = value }

    /**
     * Set trigger phrase
     */
    var trigger: String
        get() = triggerPhrase
        set(value) { triggerPhrase = value }

    /**
     * Set author
     */
    var author: String?
        get() = macroAuthor
        set(value) { macroAuthor = value }

    /**
     * Set enabled state
     */
    var isEnabled: Boolean
        get() = enabled
        set(value) { enabled = value }

    /**
     * Add tags
     */
    fun tags(vararg tags: String) {
        macroTags.addAll(tags)
    }

    /**
     * Add an action step
     */
    fun step(command: VoiceCommand) {
        steps.add(MacroStep.Action(command))
    }

    /**
     * Add an action step using builder
     */
    fun step(builder: CommandBuilder.() -> VoiceCommand) {
        val command = CommandBuilder().builder()
        steps.add(MacroStep.Action(command))
    }

    /**
     * Add a delay step
     */
    fun delay(millis: Long) {
        steps.add(MacroStep.Delay(millis))
    }

    /**
     * Add a conditional step
     */
    fun conditional(builder: ConditionalBuilder.() -> Unit) {
        val conditionalBuilder = ConditionalBuilder()
        conditionalBuilder.builder()
        steps.add(conditionalBuilder.build())
    }

    /**
     * Add a loop step
     */
    fun loop(count: Int, builder: LoopBuilder.() -> Unit) {
        val loopBuilder = LoopBuilder(count)
        loopBuilder.builder()
        steps.add(loopBuilder.build())
    }

    /**
     * Add a loop-while step
     */
    fun loopWhile(builder: LoopWhileBuilder.() -> Unit) {
        val loopWhileBuilder = LoopWhileBuilder()
        loopWhileBuilder.builder()
        steps.add(loopWhileBuilder.build())
    }

    /**
     * Add a wait-for step
     */
    fun waitFor(builder: WaitForBuilder.() -> Unit) {
        val waitForBuilder = WaitForBuilder()
        waitForBuilder.builder()
        steps.add(waitForBuilder.build())
    }

    /**
     * Add a variable step
     */
    fun variable(name: String, value: Any) {
        steps.add(MacroStep.Variable(name, value))
    }

    /**
     * Build the CommandMacro
     */
    fun build(): CommandMacro {
        require(triggerPhrase.isNotBlank()) { "Trigger phrase must be set" }
        require(steps.isNotEmpty()) { "Macro must have at least one step" }

        return CommandMacro(
            id = UUID.randomUUID().toString(),
            name = name,
            description = macroDescription,
            steps = steps.toList(),
            triggerPhrase = triggerPhrase,
            author = macroAuthor,
            tags = macroTags.toList(),
            isEnabled = enabled
        )
    }
}

/**
 * Command Builder - Helper for creating VoiceCommand
 */
@MacroDsl
class CommandBuilder {
    /**
     * Create a tap/click command
     */
    fun tap(text: String): VoiceCommand {
        return VoiceCommand(
            id = "tap_$text",
            phrase = "tap $text",
            actionType = "click"
        )
    }

    /**
     * Create a type/input command
     */
    fun typeText(target: String, text: String): VoiceCommand {
        return VoiceCommand(
            id = "type_$target",
            phrase = "type in $target",
            actionType = "type",
            parameters = mapOf("text" to text)
        )
    }

    /**
     * Create a scroll command
     */
    fun scroll(direction: String): VoiceCommand {
        return VoiceCommand(
            id = "scroll_$direction",
            phrase = "scroll $direction",
            actionType = "scroll",
            parameters = mapOf("direction" to direction)
        )
    }

    /**
     * Create an open app command
     */
    fun openApp(packageName: String): VoiceCommand {
        return VoiceCommand(
            id = "open_$packageName",
            phrase = "open app",
            actionType = "open_app",
            parameters = mapOf("packageName" to packageName)
        )
    }

    /**
     * Create a custom command
     */
    fun custom(id: String, phrase: String, actionType: String, parameters: Map<String, Any> = emptyMap()): VoiceCommand {
        return VoiceCommand(id, phrase, actionType, parameters)
    }
}

/**
 * Conditional Builder
 */
@MacroDsl
class ConditionalBuilder {
    private var conditionLambda: ((MacroContext) -> Boolean)? = null
    private val thenSteps = mutableListOf<MacroStep>()
    private val elseSteps = mutableListOf<MacroStep>()

    /**
     * Set condition
     */
    fun condition(lambda: (MacroContext) -> Boolean) {
        conditionLambda = lambda
    }

    /**
     * Define 'then' branch
     */
    fun then(builder: StepListBuilder.() -> Unit) {
        val stepBuilder = StepListBuilder()
        stepBuilder.builder()
        thenSteps.addAll(stepBuilder.steps)
    }

    /**
     * Define 'else' branch
     */
    fun otherwise(builder: StepListBuilder.() -> Unit) {
        val stepBuilder = StepListBuilder()
        stepBuilder.builder()
        elseSteps.addAll(stepBuilder.steps)
    }

    fun build(): MacroStep.Conditional {
        val condition = requireNotNull(conditionLambda) { "Condition must be set" }
        return MacroStep.Conditional(
            condition = condition,
            thenSteps = thenSteps.toList(),
            elseSteps = elseSteps.toList()
        )
    }
}

/**
 * Loop Builder
 */
@MacroDsl
class LoopBuilder(private val count: Int) {
    private val loopSteps = mutableListOf<MacroStep>()

    fun step(command: VoiceCommand) {
        loopSteps.add(MacroStep.Action(command))
    }

    fun step(builder: CommandBuilder.() -> VoiceCommand) {
        val command = CommandBuilder().builder()
        loopSteps.add(MacroStep.Action(command))
    }

    fun delay(millis: Long) {
        loopSteps.add(MacroStep.Delay(millis))
    }

    fun build(): MacroStep.Loop {
        return MacroStep.Loop(count, loopSteps.toList())
    }
}

/**
 * Loop-While Builder
 */
@MacroDsl
class LoopWhileBuilder {
    private var conditionLambda: ((MacroContext) -> Boolean)? = null
    private var maxIterations: Int = 50
    private val loopSteps = mutableListOf<MacroStep>()

    fun condition(lambda: (MacroContext) -> Boolean) {
        conditionLambda = lambda
    }

    fun maxIterations(max: Int) {
        maxIterations = max
    }

    fun step(command: VoiceCommand) {
        loopSteps.add(MacroStep.Action(command))
    }

    fun step(builder: CommandBuilder.() -> VoiceCommand) {
        val command = CommandBuilder().builder()
        loopSteps.add(MacroStep.Action(command))
    }

    fun delay(millis: Long) {
        loopSteps.add(MacroStep.Delay(millis))
    }

    fun build(): MacroStep.LoopWhile {
        val condition = requireNotNull(conditionLambda) { "Condition must be set" }
        return MacroStep.LoopWhile(
            condition = condition,
            steps = loopSteps.toList(),
            maxIterations = maxIterations
        )
    }
}

/**
 * Wait-For Builder
 */
@MacroDsl
class WaitForBuilder {
    private var conditionLambda: ((MacroContext) -> Boolean)? = null
    private var timeoutMillis: Long = 5000
    private var checkIntervalMillis: Long = 100

    fun condition(lambda: (MacroContext) -> Boolean) {
        conditionLambda = lambda
    }

    fun timeout(millis: Long) {
        timeoutMillis = millis
    }

    fun checkInterval(millis: Long) {
        checkIntervalMillis = millis
    }

    fun build(): MacroStep.WaitFor {
        val condition = requireNotNull(conditionLambda) { "Condition must be set" }
        return MacroStep.WaitFor(
            condition = condition,
            timeoutMillis = timeoutMillis,
            checkIntervalMillis = checkIntervalMillis
        )
    }
}

/**
 * Step List Builder - Helper for building lists of steps
 */
@MacroDsl
class StepListBuilder {
    val steps = mutableListOf<MacroStep>()

    fun step(command: VoiceCommand) {
        steps.add(MacroStep.Action(command))
    }

    fun step(builder: CommandBuilder.() -> VoiceCommand) {
        val command = CommandBuilder().builder()
        steps.add(MacroStep.Action(command))
    }

    fun delay(millis: Long) {
        steps.add(MacroStep.Delay(millis))
    }

    fun variable(name: String, value: Any) {
        steps.add(MacroStep.Variable(name, value))
    }
}

// ==================== Extension Functions ====================

/**
 * Helper extension for MacroContext in DSL conditions
 */
fun MacroContext.screenContains(text: String): Boolean {
    return this.screenContains(text)
}

/**
 * Helper extension to check if element is visible
 */
fun MacroContext.isVisible(text: String): Boolean {
    return this.findNodeByText(text) != null
}
