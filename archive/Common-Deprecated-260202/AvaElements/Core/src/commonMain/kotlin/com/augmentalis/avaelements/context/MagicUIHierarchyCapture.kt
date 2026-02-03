/**
 * MagicUIHierarchyCapture.kt - Screen Hierarchy Extraction from MagicUI
 *
 * Captures complete screen hierarchy from MagicUI component trees
 * and converts to structured format for AI/NLU processing.
 *
 * Features:
 * - Recursive component tree traversal
 * - Automatic voice label extraction
 * - Screen type inference
 * - Form field detection
 * - Action element extraction
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.dsl.*
import kotlin.random.Random

/**
 * Captures screen hierarchy from MagicUI component tree
 *
 * This class performs deep analysis of the MagicUI component tree to:
 * - Build hierarchical component structure
 * - Extract commandable elements for voice interaction
 * - Identify forms and input fields
 * - Detect available actions
 * - Infer screen purpose and type
 *
 * Example usage:
 * ```kotlin
 * val capture = MagicUIHierarchyCapture()
 * val hierarchy = capture.capture(
 *     rootComponent = myScreenRoot,
 *     appContext = AppContext(
 *         appId = "com.myapp",
 *         appName = "My App",
 *         packageName = "com.myapp"
 *     )
 * )
 * ```
 */
class MagicUIHierarchyCapture {

    private var currentDepth = 0
    private val maxDepth = 20 // Prevent infinite recursion

    /**
     * Capture hierarchy from MagicUI root component
     *
     * @param rootComponent Root component of the screen
     * @param appContext Application context information
     * @param navigationContext Navigation context (optional)
     * @param screenId Custom screen ID (optional, will be generated if null)
     * @return Complete screen hierarchy
     */
    fun capture(
        rootComponent: Component,
        appContext: AppContext,
        navigationContext: NavigationContext? = null,
        screenId: String? = null
    ): ScreenHierarchy {
        currentDepth = 0

        // Build component tree
        val componentTree = buildComponentTree(rootComponent)

        // Extract quantized information
        val commandableElements = extractCommandableElements(componentTree)
        val formFields = extractFormFields(componentTree)
        val actions = extractActions(componentTree)
        val dataDisplay = extractDataElements(componentTree)

        // Infer screen characteristics
        val screenType = inferScreenType(componentTree, formFields, actions)
        val screenPurpose = inferScreenPurpose(screenType, formFields, actions)
        val primaryAction = PrimaryAction.fromActions(actions)

        // Calculate complexity
        val complexity = calculateComplexity(componentTree, formFields, actions)

        // Generate identifiers
        val finalScreenId = screenId ?: generateScreenId(appContext, screenType)
        val screenHash = generateScreenHash(componentTree)

        return ScreenHierarchy(
            screenId = finalScreenId,
            screenHash = screenHash,
            screenType = screenType,
            screenPurpose = screenPurpose,
            primaryAction = primaryAction,
            appContext = appContext,
            navigationContext = navigationContext ?: NavigationContext(
                currentScreen = finalScreenId,
                canNavigateBack = hasBackButton(componentTree)
            ),
            root = componentTree,
            commandableElements = commandableElements,
            formFields = formFields,
            actions = actions,
            dataDisplay = dataDisplay,
            complexity = complexity
        )
    }

    /**
     * Build component tree recursively
     *
     * @param component Source component
     * @param parent Parent node ID
     * @param depth Current depth
     * @return Component node with children
     */
    private fun buildComponentTree(
        component: Component,
        parent: String? = null,
        depth: Int = 0
    ): ComponentNode {
        if (depth > maxDepth) {
            // Prevent infinite recursion
            return createLeafNode(component, parent, depth)
        }

        // Extract voice label
        val voiceLabel = extractVoiceLabel(component)

        // Determine available voice commands
        val voiceCommands = inferVoiceCommands(component)

        // Get component role
        val role = ComponentRole.fromComponentType(component.type)

        // Extract children
        val children = extractChildren(component)
            .map { child -> buildComponentTree(child, component.id, depth + 1) }

        return ComponentNode(
            id = component.id ?: generateComponentId(component, depth),
            type = component.type,
            role = role,
            bounds = null, // TODO: Get from renderer in future
            voiceLabel = voiceLabel,
            voiceCommands = voiceCommands,
            text = extractText(component),
            contentDescription = extractContentDescription(component),
            value = extractValue(component),
            isEnabled = isEnabled(component),
            isVisible = true,
            isFocused = false,
            isInteractive = isInteractive(component),
            children = children,
            parent = parent,
            depth = depth
        )
    }

    /**
     * Extract children from component
     */
    private fun extractChildren(component: Component): List<Component> {
        return when (component) {
            is ColumnComponent -> component.children
            is RowComponent -> component.children
            is CardComponent -> component.children
            is ScrollViewComponent -> listOfNotNullNot(component.child)
            is ContainerComponent -> listOfNotNull(component.child)
            is StackComponent -> component.children
            is GridComponent -> component.children
            else -> emptyList()
        }
    }

    /**
     * Extract voice label from component
     */
    private fun extractVoiceLabel(component: Component): String? {
        return when (component) {
            is ButtonComponent -> component.contentDescription ?: component.text
            is TextFieldComponent -> component.contentDescription ?: component.placeholder
            is ImageComponent -> component.contentDescription
            is IconComponent -> component.contentDescription
            is CheckboxComponent -> component.contentDescription ?: component.label
            is SwitchComponent -> component.contentDescription ?: component.label
            is RadioButtonComponent -> component.contentDescription ?: component.label
            is DropdownComponent -> component.contentDescription ?: component.label
            else -> null
        }
    }

    /**
     * Infer voice commands for component
     */
    private fun inferVoiceCommands(component: Component): List<String> {
        val commandType = VoiceCommandType.fromComponentType(component.type)

        return when (commandType) {
            VoiceCommandType.CLICK -> listOf("click", "tap", "press", "select")
            VoiceCommandType.TYPE -> listOf("type", "enter", "input")
            VoiceCommandType.SELECT -> listOf("select", "choose", "pick")
            VoiceCommandType.SCROLL -> listOf("scroll", "swipe")
            VoiceCommandType.TOGGLE -> listOf("toggle", "switch", "turn on", "turn off")
            VoiceCommandType.SUBMIT -> listOf("submit", "send", "confirm")
            VoiceCommandType.NAVIGATE -> listOf("go to", "navigate to", "open")
            else -> emptyList()
        }
    }

    /**
     * Extract text from component
     */
    private fun extractText(component: Component): String? {
        return when (component) {
            is TextComponent -> component.text
            is ButtonComponent -> component.text
            is CheckboxComponent -> component.label
            is SwitchComponent -> component.label
            is RadioButtonComponent -> component.label
            else -> null
        }
    }

    /**
     * Extract content description
     */
    private fun extractContentDescription(component: Component): String? {
        return when (component) {
            is ButtonComponent -> component.contentDescription
            is TextFieldComponent -> component.contentDescription
            is ImageComponent -> component.contentDescription
            is IconComponent -> component.contentDescription
            is CheckboxComponent -> component.contentDescription
            is SwitchComponent -> component.contentDescription
            else -> null
        }
    }

    /**
     * Extract value from component
     */
    private fun extractValue(component: Component): Any? {
        return when (component) {
            is TextFieldComponent -> component.value
            is CheckboxComponent -> component.checked
            is SwitchComponent -> component.checked
            is SliderComponent -> component.value
            is RadioButtonComponent -> component.selected
            is DropdownComponent -> component.selectedValue
            else -> null
        }
    }

    /**
     * Check if component is enabled
     */
    private fun isEnabled(component: Component): Boolean {
        return when (component) {
            is ButtonComponent -> component.enabled
            is TextFieldComponent -> component.enabled
            is CheckboxComponent -> component.enabled
            is SwitchComponent -> component.enabled
            is SliderComponent -> component.enabled
            is RadioButtonComponent -> component.enabled
            is DropdownComponent -> component.enabled
            else -> true
        }
    }

    /**
     * Check if component is interactive
     */
    private fun isInteractive(component: Component): Boolean {
        // Check if component has click modifier
        val hasClickModifier = component.modifiers.any { it is Modifier.Clickable }

        // Check component type
        val interactiveTypes = setOf(
            "Button", "TextField", "Checkbox", "Switch", "Slider",
            "RadioButton", "Dropdown", "IconButton", "FloatingActionButton",
            "Chip", "Tab", "BottomNav"
        )

        return hasClickModifier || component.type in interactiveTypes
    }

    /**
     * Extract commandable elements from tree
     */
    private fun extractCommandableElements(root: ComponentNode): List<CommandableElement> {
        val elements = mutableListOf<CommandableElement>()

        fun traverse(node: ComponentNode) {
            if (node.isInteractive && node.voiceLabel != null) {
                val primaryCommand = determinePrimaryCommand(node.type)
                val alternateCommands = determineAlternateCommands(node.type)
                val priority = calculatePriority(node)

                elements.add(
                    CommandableElement(
                        id = node.id,
                        voiceLabel = node.voiceLabel,
                        componentType = node.type,
                        primaryCommand = primaryCommand,
                        alternateCommands = alternateCommands,
                        priority = priority
                    )
                )
            }

            node.children.forEach { traverse(it) }
        }

        traverse(root)
        return elements
    }

    /**
     * Extract form fields from tree
     */
    private fun extractFormFields(root: ComponentNode): List<FormField> {
        val fields = mutableListOf<FormField>()

        fun traverse(node: ComponentNode) {
            when (node.type) {
                "TextField", "Checkbox", "Switch", "RadioButton",
                "Dropdown", "Slider" -> {
                    val fieldType = FieldType.fromComponent(
                        node.type,
                        node.contentDescription,
                        node.text
                    )

                    fields.add(
                        FormField(
                            id = node.id,
                            fieldType = fieldType,
                            label = node.contentDescription,
                            placeholder = node.text,
                            currentValue = node.value?.toString(),
                            isRequired = false, // TODO: Detect from validation
                            voiceLabel = node.voiceLabel
                        )
                    )
                }
            }

            node.children.forEach { traverse(it) }
        }

        traverse(root)
        return fields
    }

    /**
     * Extract action elements from tree
     */
    private fun extractActions(root: ComponentNode): List<ActionElement> {
        val actions = mutableListOf<ActionElement>()

        fun traverse(node: ComponentNode) {
            when (node.type) {
                "Button", "IconButton", "FloatingActionButton" -> {
                    val actionType = node.text?.let {
                        ActionType.fromButtonText(it)
                    } ?: ActionType.UNKNOWN

                    val label = node.text ?: node.voiceLabel ?: "Button"
                    val voiceCommand = "click ${node.voiceLabel ?: label}"

                    actions.add(
                        ActionElement(
                            id = node.id,
                            actionType = actionType,
                            label = label,
                            voiceCommand = voiceCommand,
                            confirmationRequired = actionType in setOf(
                                ActionType.DELETE,
                                ActionType.SUBMIT
                            )
                        )
                    )
                }
            }

            node.children.forEach { traverse(it) }
        }

        traverse(root)
        return actions
    }

    /**
     * Extract data display elements
     */
    private fun extractDataElements(root: ComponentNode): List<DataElement> {
        val elements = mutableListOf<DataElement>()

        fun traverse(node: ComponentNode) {
            when (node.type) {
                "Text" -> {
                    elements.add(
                        DataElement(
                            id = node.id,
                            dataType = DataType.TEXT,
                            content = node.text
                        )
                    )
                }
                "Image" -> {
                    elements.add(
                        DataElement(
                            id = node.id,
                            dataType = DataType.IMAGE,
                            semanticMeaning = node.contentDescription
                        )
                    )
                }
                "Icon" -> {
                    elements.add(
                        DataElement(
                            id = node.id,
                            dataType = DataType.ICON,
                            semanticMeaning = node.contentDescription
                        )
                    )
                }
            }

            node.children.forEach { traverse(it) }
        }

        traverse(root)
        return elements
    }

    /**
     * Infer screen type from component tree
     */
    private fun inferScreenType(
        root: ComponentNode,
        formFields: List<FormField>,
        actions: List<ActionElement>
    ): ScreenType {
        val allNodes = root.flatten()
        val keywords = mutableListOf<String>()

        // Collect keywords from text and labels
        allNodes.forEach { node ->
            node.text?.let { keywords.add(it.lowercase()) }
            node.voiceLabel?.let { keywords.add(it.lowercase()) }
            node.contentDescription?.let { keywords.add(it.lowercase()) }
        }

        // Check for password fields (strong indicator of login)
        val hasPasswordField = formFields.any { it.fieldType == FieldType.PASSWORD }
        if (hasPasswordField) {
            return if (keywords.any { it.contains("sign up") || it.contains("register") }) {
                ScreenType.SIGNUP
            } else {
                ScreenType.LOGIN
            }
        }

        // Check for payment fields
        val hasPaymentFields = formFields.any {
            it.fieldType == FieldType.NUMBER &&
            (it.label?.contains("card", ignoreCase = true) == true ||
             it.label?.contains("cvv", ignoreCase = true) == true)
        }
        if (hasPaymentFields) return ScreenType.CHECKOUT

        // Check for settings indicators
        val switchCount = formFields.count { it.fieldType == FieldType.SWITCH }
        if (switchCount >= 3) return ScreenType.SETTINGS

        // Use keyword-based inference
        return ScreenType.fromKeywords(keywords)
    }

    /**
     * Infer screen purpose from type and content
     */
    private fun inferScreenPurpose(
        screenType: ScreenType,
        formFields: List<FormField>,
        actions: List<ActionElement>
    ): String? {
        return when (screenType) {
            ScreenType.LOGIN -> "User authentication"
            ScreenType.SIGNUP -> "New account creation"
            ScreenType.CHECKOUT -> "Complete purchase"
            ScreenType.SETTINGS -> "Configure app preferences"
            ScreenType.FORM -> "Data input and submission"
            ScreenType.SEARCH -> "Find content"
            else -> null
        }
    }

    /**
     * Calculate complexity score
     */
    private fun calculateComplexity(
        root: ComponentNode,
        formFields: List<FormField>,
        actions: List<ActionElement>
    ): ComplexityScore {
        val allNodes = root.flatten()
        val interactiveNodes = allNodes.filter { it.isInteractive }

        return ComplexityScore(
            totalComponents = allNodes.size,
            interactiveComponents = interactiveNodes.size,
            maxDepth = root.getMaxDepth(),
            formFieldCount = formFields.size,
            actionCount = actions.size
        )
    }

    /**
     * Determine primary command for component type
     */
    private fun determinePrimaryCommand(componentType: String): String {
        return when (componentType.lowercase()) {
            "button", "iconbutton" -> "click"
            "textfield" -> "type"
            "checkbox", "radiobutton", "dropdown" -> "select"
            "switch" -> "toggle"
            "slider" -> "set"
            else -> "click"
        }
    }

    /**
     * Determine alternate commands
     */
    private fun determineAlternateCommands(componentType: String): List<String> {
        return when (componentType.lowercase()) {
            "button", "iconbutton" -> listOf("tap", "press")
            "textfield" -> listOf("enter", "input")
            "checkbox", "radiobutton", "dropdown" -> listOf("choose", "pick")
            "switch" -> listOf("switch", "turn on", "turn off")
            else -> emptyList()
        }
    }

    /**
     * Calculate priority for commandable element
     */
    private fun calculatePriority(node: ComponentNode): Int {
        var priority = 5 // Base priority

        // Increase for submit/primary actions
        if (node.type == "Button" && node.text != null) {
            val lowerText = node.text.lowercase()
            if (lowerText in setOf("submit", "save", "confirm", "ok")) {
                priority += 10
            }
        }

        // Increase for required fields
        // TODO: Add required field detection

        // Decrease based on depth (deeper = lower priority)
        priority -= (node.depth / 2)

        return priority.coerceIn(0, 20)
    }

    /**
     * Check if tree has back button
     */
    private fun hasBackButton(root: ComponentNode): Boolean {
        return root.flatten().any { node ->
            node.type == "Button" &&
            (node.text?.contains("back", ignoreCase = true) == true ||
             node.voiceLabel?.contains("back", ignoreCase = true) == true ||
             node.contentDescription?.contains("back", ignoreCase = true) == true)
        }
    }

    /**
     * Generate screen ID
     */
    private fun generateScreenId(appContext: AppContext, screenType: ScreenType): String {
        return "${appContext.appId}.${screenType.name.lowercase()}"
    }

    /**
     * Generate screen hash
     */
    private fun generateScreenHash(root: ComponentNode): String {
        val signature = buildString {
            append(root.type)
            root.flatten().take(10).forEach {
                append("|${it.type}")
            }
        }
        return signature.hashCode().toString(16)
    }

    /**
     * Generate component ID
     */
    private fun generateComponentId(component: Component, depth: Int): String {
        return "${component.type.lowercase()}-${depth}-${Random.nextInt(1000, 9999)}"
    }

    /**
     * Create leaf node (for recursion prevention)
     */
    private fun createLeafNode(
        component: Component,
        parent: String?,
        depth: Int
    ): ComponentNode {
        return ComponentNode(
            id = component.id ?: generateComponentId(component, depth),
            type = component.type,
            role = ComponentRole.fromComponentType(component.type),
            voiceLabel = extractVoiceLabel(component),
            voiceCommands = emptyList(),
            text = extractText(component),
            contentDescription = extractContentDescription(component),
            value = extractValue(component),
            isEnabled = isEnabled(component),
            isInteractive = isInteractive(component),
            children = emptyList(),
            parent = parent,
            depth = depth
        )
    }
}

// Helper extension for nullable lists
private fun <T> listOfNotNull(item: T?): List<T> {
    return if (item != null) listOf(item) else emptyList()
}
