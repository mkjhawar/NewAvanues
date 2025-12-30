/**
 * ScreenHierarchy.kt - Universal Screen Hierarchy System
 *
 * Complete screen hierarchy with quantized information for AI/NLU
 * Cross-platform support (Android, iOS, Web, Desktop)
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System for context-aware voice commands
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

/**
 * Complete screen hierarchy with quantized information for AI/NLU
 *
 * This represents the entire state of a screen including:
 * - Component tree structure
 * - Voice-commandable elements
 * - Form fields and actions
 * - Context information for AI reasoning
 *
 * Example usage:
 * ```kotlin
 * val hierarchy = ScreenHierarchy(
 *     screenId = "com.app.LoginScreen",
 *     screenType = ScreenType.LOGIN,
 *     root = componentTree,
 *     commandableElements = listOf(submitButton, emailField)
 * )
 * ```
 */
data class ScreenHierarchy(
    // Identity
    val screenId: String,
    val screenHash: String,

    // Classification
    val screenType: ScreenType,
    val screenPurpose: String? = null,
    val primaryAction: PrimaryAction? = null,

    // Context
    val appContext: AppContext,
    val navigationContext: NavigationContext,

    // Component Tree (hierarchical)
    val root: ComponentNode,

    // Quantized Information for AI
    val commandableElements: List<CommandableElement> = emptyList(),
    val formFields: List<FormField> = emptyList(),
    val actions: List<ActionElement> = emptyList(),
    val dataDisplay: List<DataElement> = emptyList(),

    // Metadata
    val timestamp: Long = System.currentTimeMillis(),
    val complexity: ComplexityScore
) {
    /**
     * Get total number of interactive elements
     */
    fun getInteractiveCount(): Int = commandableElements.size

    /**
     * Check if this is a form screen
     */
    fun isFormScreen(): Boolean = formFields.size >= 2

    /**
     * Get all available voice commands
     */
    fun getAvailableCommands(): List<String> {
        return commandableElements.map { it.voiceLabel }
    }

    /**
     * Find element by voice label
     */
    fun findElementByVoiceLabel(label: String): CommandableElement? {
        return commandableElements.find {
            it.voiceLabel.equals(label, ignoreCase = true)
        }
    }
}

/**
 * Hierarchical component node representing UI elements in a tree structure
 *
 * Each node represents a UI component (Button, TextField, Container, etc.)
 * with its properties, state, and relationship to other components.
 *
 * Example:
 * ```kotlin
 * val buttonNode = ComponentNode(
 *     id = "submit-btn",
 *     type = "Button",
 *     role = ComponentRole.ACTION,
 *     voiceLabel = "submit button",
 *     text = "Submit",
 *     isInteractive = true
 * )
 * ```
 */
data class ComponentNode(
    // Identity
    val id: String,
    val type: String,
    val role: ComponentRole,
    val bounds: Rectangle? = null,

    // Voice Integration
    val voiceLabel: String? = null,
    val voiceCommands: List<String> = emptyList(),

    // Content
    val text: String? = null,
    val contentDescription: String? = null,
    val value: Any? = null,

    // State
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val isFocused: Boolean = false,
    val isInteractive: Boolean = false,

    // Hierarchy
    val children: List<ComponentNode> = emptyList(),
    val parent: String? = null,
    val depth: Int = 0
) {
    /**
     * Flatten tree to list of all nodes
     */
    fun flatten(): List<ComponentNode> {
        val result = mutableListOf(this)
        children.forEach { result.addAll(it.flatten()) }
        return result
    }

    /**
     * Find node by ID in tree
     */
    fun findById(id: String): ComponentNode? {
        if (this.id == id) return this
        return children.firstNotNullOfOrNull { it.findById(id) }
    }

    /**
     * Get all interactive children
     */
    fun getInteractiveChildren(): List<ComponentNode> {
        return flatten().filter { it.isInteractive }
    }

    /**
     * Get maximum depth of tree
     */
    fun getMaxDepth(): Int {
        return if (children.isEmpty()) {
            depth
        } else {
            children.maxOf { it.getMaxDepth() }
        }
    }
}

/**
 * Commandable element - UI element that can be targeted by voice commands
 *
 * These are extracted from the component tree and represent
 * interactive elements that users can control via voice.
 *
 * Example:
 * ```kotlin
 * val element = CommandableElement(
 *     id = "email-field",
 *     voiceLabel = "email field",
 *     componentType = "TextField",
 *     primaryCommand = "type",
 *     alternateCommands = listOf("enter", "input")
 * )
 * ```
 */
data class CommandableElement(
    val id: String,
    val voiceLabel: String,
    val componentType: String,
    val primaryCommand: String,
    val alternateCommands: List<String> = emptyList(),
    val parameters: Map<String, String> = emptyMap(),
    val priority: Int = 0
) {
    /**
     * Check if this element matches a voice command
     */
    fun matchesCommand(command: String): Boolean {
        return primaryCommand.equals(command, ignoreCase = true) ||
               alternateCommands.any { it.equals(command, ignoreCase = true) }
    }

    /**
     * Check if this element matches a voice label
     */
    fun matchesLabel(label: String): Boolean {
        return voiceLabel.contains(label, ignoreCase = true)
    }
}

/**
 * Form field element representing input fields
 *
 * Captures information about form inputs including type,
 * validation, and current state.
 */
data class FormField(
    val id: String,
    val fieldType: FieldType,
    val label: String? = null,
    val placeholder: String? = null,
    val currentValue: String? = null,
    val isRequired: Boolean = false,
    val validation: ValidationRule? = null,
    val voiceLabel: String? = null
) {
    /**
     * Check if field has value
     */
    fun hasValue(): Boolean = !currentValue.isNullOrBlank()

    /**
     * Check if field is empty and required
     */
    fun needsValue(): Boolean = isRequired && !hasValue()
}

/**
 * Action element representing buttons, links, and other actions
 */
data class ActionElement(
    val id: String,
    val actionType: ActionType,
    val label: String,
    val destination: String? = null,
    val confirmationRequired: Boolean = false,
    val voiceCommand: String
) {
    /**
     * Check if action requires confirmation
     */
    fun needsConfirmation(): Boolean = confirmationRequired

    /**
     * Check if action is navigation
     */
    fun isNavigation(): Boolean = destination != null
}

/**
 * Data display element representing text, images, and other content
 */
data class DataElement(
    val id: String,
    val dataType: DataType,
    val content: String? = null,
    val semanticMeaning: String? = null,
    val isScrollable: Boolean = false
)

/**
 * App context information
 */
data class AppContext(
    val appId: String,
    val appName: String,
    val packageName: String,
    val activityName: String? = null,
    val category: String? = null
)

/**
 * Navigation context information
 */
data class NavigationContext(
    val currentScreen: String,
    val previousScreen: String? = null,
    val navigationStack: List<String> = emptyList(),
    val canNavigateBack: Boolean = false,
    val canNavigateForward: Boolean = false
) {
    /**
     * Get navigation depth
     */
    fun getDepth(): Int = navigationStack.size
}

/**
 * Complexity score for screen analysis
 */
data class ComplexityScore(
    val totalComponents: Int,
    val interactiveComponents: Int,
    val maxDepth: Int,
    val formFieldCount: Int,
    val actionCount: Int
) {
    /**
     * Calculate overall complexity (0-100)
     */
    fun getOverallScore(): Int {
        val componentScore = minOf(totalComponents / 50.0 * 30, 30.0)
        val depthScore = minOf(maxDepth / 10.0 * 20, 20.0)
        val interactionScore = minOf(interactiveComponents / 20.0 * 30, 30.0)
        val formScore = minOf(formFieldCount / 10.0 * 20, 20.0)

        return (componentScore + depthScore + interactionScore + formScore).toInt()
    }

    /**
     * Get complexity level
     */
    fun getLevel(): ComplexityLevel {
        return when (getOverallScore()) {
            in 0..30 -> ComplexityLevel.SIMPLE
            in 31..60 -> ComplexityLevel.MODERATE
            in 61..80 -> ComplexityLevel.COMPLEX
            else -> ComplexityLevel.VERY_COMPLEX
        }
    }
}

/**
 * Bounding rectangle for component positioning
 */
data class Rectangle(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    /**
     * Check if point is inside rectangle
     */
    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }

    /**
     * Check if this rectangle overlaps another
     */
    fun overlaps(other: Rectangle): Boolean {
        return !(right < other.left || left > other.right ||
                 bottom < other.top || top > other.bottom)
    }
}

/**
 * Validation rule for form fields
 */
data class ValidationRule(
    val pattern: String? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val errorMessage: String? = null
)
