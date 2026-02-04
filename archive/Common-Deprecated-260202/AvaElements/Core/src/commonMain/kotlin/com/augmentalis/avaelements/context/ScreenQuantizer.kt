/**
 * ScreenQuantizer.kt - AI Context Generation from Screen Hierarchy
 *
 * Converts screen hierarchy into quantized, structured format for:
 * - NLU (Natural Language Understanding) systems
 * - LLM (Large Language Model) context
 * - Voice command disambiguation
 * - AI-powered assistance
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

import kotlin.math.min

/**
 * Quantizer converts screen hierarchy to structured format for NLU/LLM
 *
 * The quantization process:
 * 1. Analyzes component tree structure
 * 2. Extracts key information (commands, forms, actions)
 * 3. Generates natural language summary
 * 4. Creates component relationship graph
 * 5. Outputs AI-ready structured data
 *
 * Example usage:
 * ```kotlin
 * val quantized = ScreenQuantizer.quantize(screenHierarchy)
 * val summary = quantized.summary  // Natural language
 * val commands = quantized.commands  // Available voice commands
 * ```
 */
object ScreenQuantizer {

    /**
     * Generate AI context from screen hierarchy
     * Output: JSON-serializable structure for NLU/LLM
     *
     * @param hierarchy Complete screen hierarchy
     * @return Quantized screen representation optimized for AI consumption
     */
    fun quantize(hierarchy: ScreenHierarchy): QuantizedScreen {
        return QuantizedScreen(
            // Natural language summary (optimized for LLM context)
            summary = generateSummary(hierarchy),

            // Structured screen information
            screen = ScreenInfo(
                type = hierarchy.screenType.name,
                purpose = hierarchy.screenPurpose,
                primaryAction = hierarchy.primaryAction?.name
            ),

            // Available voice commands
            commands = quantizeCommands(hierarchy.commandableElements),

            // Context-aware data
            context = ContextData(
                appName = hierarchy.appContext.appName,
                currentScreen = hierarchy.screenType.displayName,
                previousScreen = hierarchy.navigationContext.previousScreen,
                canGoBack = hierarchy.navigationContext.canNavigateBack,
                formMode = hierarchy.isFormScreen(),
                availableActions = hierarchy.actions.map { it.actionType.displayName }
            ),

            // Component relationship graph
            componentGraph = buildComponentGraph(hierarchy.root),

            // Form information (if applicable)
            formInfo = if (hierarchy.isFormScreen()) {
                quantizeFormInfo(hierarchy.formFields)
            } else null,

            // Metadata
            metadata = QuantizationMetadata(
                complexity = hierarchy.complexity.getLevel().name,
                interactiveCount = hierarchy.getInteractiveCount(),
                timestamp = hierarchy.timestamp
            )
        )
    }

    /**
     * Generate natural language summary for LLM context
     *
     * Creates a concise, human-readable description of the screen
     * optimized for LLM token efficiency (typically 50-150 tokens).
     *
     * Example output:
     * "Settings screen with 5 toggle switches for notifications,
     *  2 dropdown menus for language and theme, and a save button.
     *  User can modify app preferences and save changes."
     *
     * @param hierarchy Complete screen hierarchy
     * @return Natural language summary string
     */
    fun generateSummary(hierarchy: ScreenHierarchy): String {
        val components = hierarchy.root.flatten()

        // Count component types
        val componentCounts = components
            .groupingBy { it.type }
            .eachCount()
            .filter { it.value > 0 }

        val interactiveCount = hierarchy.getInteractiveCount()
        val formsCount = hierarchy.formFields.size
        val actionsCount = hierarchy.actions.size

        return buildString {
            // Screen type
            append(hierarchy.screenType.displayName)
            append(" screen")

            // Component summary
            if (componentCounts.isNotEmpty()) {
                append(" with ")
                val topComponents = componentCounts.entries
                    .sortedByDescending { it.value }
                    .take(3)

                topComponents.forEachIndexed { index, (type, count) ->
                    if (index > 0) {
                        if (index == topComponents.size - 1) append(", and ")
                        else append(", ")
                    }
                    append("$count ${type.lowercase()}${if (count > 1) "s" else ""}")
                }
            }

            // Form fields
            if (formsCount > 0) {
                append(", $formsCount input field${if (formsCount > 1) "s" else ""}")
            }

            // Actions
            if (actionsCount > 0) {
                append(", and $actionsCount action button${if (actionsCount > 1) "s" else ""}")
            }

            append(". ")

            // Screen purpose
            hierarchy.screenPurpose?.let {
                append("Purpose: $it. ")
            }

            // Primary action
            hierarchy.primaryAction?.let {
                append("Primary action: ${it.displayName}. ")
            }

            // Top voice commands
            val topCommands = hierarchy.commandableElements
                .sortedByDescending { it.priority }
                .take(5)
                .map { it.voiceLabel }

            if (topCommands.isNotEmpty()) {
                append("Voice commands: ")
                append(topCommands.joinToString(", ") { "\"$it\"" })
                append(".")
            }

            // Complexity indicator
            val complexityLevel = hierarchy.complexity.getLevel()
            if (complexityLevel == ComplexityLevel.COMPLEX ||
                complexityLevel == ComplexityLevel.VERY_COMPLEX
            ) {
                append(" (Complex interface)")
            }
        }
    }

    /**
     * Quantize commands into structured format
     */
    private fun quantizeCommands(elements: List<CommandableElement>): List<Command> {
        return elements
            .sortedByDescending { it.priority }
            .map { element ->
                Command(
                    label = element.voiceLabel,
                    action = element.primaryCommand,
                    target = element.componentType,
                    alternates = element.alternateCommands,
                    parameters = element.parameters,
                    priority = element.priority
                )
            }
    }

    /**
     * Build component relationship graph for AI understanding
     *
     * Creates a graph representation of component relationships
     * that AI systems can use to understand UI structure.
     *
     * @param root Root component node
     * @return Component graph with nodes and edges
     */
    private fun buildComponentGraph(root: ComponentNode): ComponentGraph {
        val nodes = mutableListOf<GraphNode>()
        val edges = mutableListOf<GraphEdge>()

        fun traverse(node: ComponentNode) {
            // Add node
            nodes.add(
                GraphNode(
                    id = node.id,
                    type = node.type,
                    role = node.role.name,
                    label = node.voiceLabel,
                    text = node.text,
                    interactive = node.isInteractive,
                    depth = node.depth
                )
            )

            // Add edges to children
            node.children.forEach { child ->
                edges.add(
                    GraphEdge(
                        from = node.id,
                        to = child.id,
                        relationship = "contains"
                    )
                )
                traverse(child)
            }
        }

        traverse(root)

        return ComponentGraph(
            nodes = nodes,
            edges = edges,
            maxDepth = root.getMaxDepth()
        )
    }

    /**
     * Quantize form information
     */
    private fun quantizeFormInfo(fields: List<FormField>): FormInfo {
        return FormInfo(
            fieldCount = fields.size,
            requiredFieldCount = fields.count { it.isRequired },
            fieldTypes = fields.groupingBy { it.fieldType.displayName }.eachCount(),
            fields = fields.map { field ->
                FormFieldInfo(
                    id = field.id,
                    type = field.fieldType.displayName,
                    label = field.label ?: field.voiceLabel,
                    required = field.isRequired,
                    hasValue = field.hasValue()
                )
            }
        )
    }

    /**
     * Generate compact summary for NLU (Natural Language Understanding)
     * Optimized for lower token usage than LLM summary
     *
     * @param hierarchy Screen hierarchy
     * @return Compact summary string (typically 20-50 tokens)
     */
    fun generateCompactSummary(hierarchy: ScreenHierarchy): String {
        return buildString {
            append(hierarchy.screenType.displayName)
            append(": ")
            append("${hierarchy.getInteractiveCount()} interactive, ")
            append("${hierarchy.formFields.size} fields, ")
            append("${hierarchy.actions.size} actions")

            hierarchy.primaryAction?.let {
                append(". Primary: ${it.displayName}")
            }
        }
    }

    /**
     * Extract key entities for NLU
     *
     * Identifies entities (buttons, fields, etc.) that are likely
     * targets of voice commands.
     *
     * @param hierarchy Screen hierarchy
     * @return List of entities with metadata
     */
    fun extractEntities(hierarchy: ScreenHierarchy): List<Entity> {
        val entities = mutableListOf<Entity>()

        // Extract from commandable elements
        hierarchy.commandableElements.forEach { element ->
            entities.add(
                Entity(
                    id = element.id,
                    type = element.componentType,
                    label = element.voiceLabel,
                    role = "commandable",
                    priority = element.priority
                )
            )
        }

        // Extract from form fields
        hierarchy.formFields.forEach { field ->
            field.voiceLabel?.let { label ->
                entities.add(
                    Entity(
                        id = field.id,
                        type = field.fieldType.displayName,
                        label = label,
                        role = "input",
                        priority = if (field.isRequired) 10 else 5
                    )
                )
            }
        }

        // Extract from actions
        hierarchy.actions.forEach { action ->
            entities.add(
                Entity(
                    id = action.id,
                    type = action.actionType.displayName,
                    label = action.label,
                    role = "action",
                    priority = when (action.actionType) {
                        ActionType.SUBMIT, ActionType.SAVE -> 15
                        ActionType.CANCEL, ActionType.DELETE -> 10
                        else -> 5
                    }
                )
            )
        }

        return entities.sortedByDescending { it.priority }
    }

    /**
     * Generate intent schema for NLU training
     *
     * Creates a schema that NLU systems can use to understand
     * possible intents on this screen.
     *
     * @param hierarchy Screen hierarchy
     * @return Intent schema definition
     */
    fun generateIntentSchema(hierarchy: ScreenHierarchy): IntentSchema {
        val intents = mutableListOf<Intent>()

        // Generate intents from commandable elements
        hierarchy.commandableElements
            .groupBy { it.primaryCommand }
            .forEach { (command, elements) ->
                intents.add(
                    Intent(
                        name = command,
                        utterances = generateUtterances(command, elements),
                        slots = mapOf(
                            "target" to elements.map { it.voiceLabel }
                        )
                    )
                )
            }

        return IntentSchema(
            screenType = hierarchy.screenType.name,
            intents = intents
        )
    }

    /**
     * Generate sample utterances for an intent
     */
    private fun generateUtterances(command: String, elements: List<CommandableElement>): List<String> {
        val utterances = mutableListOf<String>()

        elements.forEach { element ->
            utterances.add("$command ${element.voiceLabel}")
            utterances.add("${element.voiceLabel}")

            element.alternateCommands.forEach { alt ->
                utterances.add("$alt ${element.voiceLabel}")
            }
        }

        return utterances.distinct()
    }
}

/**
 * Quantized screen representation (AI-ready)
 *
 * Structured output optimized for consumption by:
 * - NLU systems (intent recognition, slot filling)
 * - LLMs (context understanding, conversational AI)
 * - Voice assistants (command disambiguation)
 */
data class QuantizedScreen(
    val summary: String,
    val screen: ScreenInfo,
    val commands: List<Command>,
    val context: ContextData,
    val componentGraph: ComponentGraph,
    val formInfo: FormInfo? = null,
    val metadata: QuantizationMetadata
)

data class ScreenInfo(
    val type: String,
    val purpose: String?,
    val primaryAction: String?
)

data class Command(
    val label: String,
    val action: String,
    val target: String,
    val alternates: List<String> = emptyList(),
    val parameters: Map<String, String> = emptyMap(),
    val priority: Int = 0
)

data class ContextData(
    val appName: String,
    val currentScreen: String,
    val previousScreen: String?,
    val canGoBack: Boolean,
    val formMode: Boolean,
    val availableActions: List<String>
)

data class ComponentGraph(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val maxDepth: Int
)

data class GraphNode(
    val id: String,
    val type: String,
    val role: String,
    val label: String?,
    val text: String?,
    val interactive: Boolean,
    val depth: Int
)

data class GraphEdge(
    val from: String,
    val to: String,
    val relationship: String
)

data class FormInfo(
    val fieldCount: Int,
    val requiredFieldCount: Int,
    val fieldTypes: Map<String, Int>,
    val fields: List<FormFieldInfo>
)

data class FormFieldInfo(
    val id: String,
    val type: String,
    val label: String?,
    val required: Boolean,
    val hasValue: Boolean
)

data class QuantizationMetadata(
    val complexity: String,
    val interactiveCount: Int,
    val timestamp: Long
)

/**
 * Entity for NLU
 */
data class Entity(
    val id: String,
    val type: String,
    val label: String,
    val role: String,
    val priority: Int
)

/**
 * Intent schema for NLU training
 */
data class IntentSchema(
    val screenType: String,
    val intents: List<Intent>
)

data class Intent(
    val name: String,
    val utterances: List<String>,
    val slots: Map<String, List<String>>
)
