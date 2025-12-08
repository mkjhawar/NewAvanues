package com.augmentalis.avanues.avaui.core

/**
 * Interface for extensible UI components in the AvaUI plugin system.
 *
 * Implementing this interface allows third-party developers to create custom components
 * that integrate seamlessly with the core AvaUI framework. All plugin components receive
 * lifecycle callbacks and can export custom code generation logic.
 *
 * ## Lifecycle
 *
 * 1. **onCreate()** - Component instantiation and initialization
 * 2. **onRender()** - Convert ComponentModel to platform-specific UI
 * 3. **onDestroy()** - Cleanup resources before removal
 *
 * ## Example Implementation
 *
 * ```kotlin
 * class CustomButton : PluginComponent {
 *     override val componentType: String = "CustomButton"
 *
 *     override fun onCreate(model: ComponentModel) {
 *         // Initialize state, validate properties
 *     }
 *
 *     override fun onRender(model: ComponentModel): Any {
 *         // Return platform-specific UI (Compose, View, etc.)
 *         return Button(text = model.properties["text"] ?: "")
 *     }
 *
 *     override fun onDestroy() {
 *         // Release resources
 *     }
 *
 *     override fun generateExport(model: ComponentModel, format: ExportFormat): String {
 *         return when (format) {
 *             ExportFormat.KOTLIN -> "CustomButton(text = \"${model.properties["text"]}\")"
 *             ExportFormat.DSL -> "custom_button[text=${model.properties["text"]}]"
 *             else -> ""
 *         }
 *     }
 * }
 * ```
 *
 * @since 3.1.0
 */
interface PluginComponent {
    /**
     * Unique component type identifier (e.g., "CustomButton", "VideoPlayer").
     *
     * This identifier is used in layouts to reference the component:
     * ```yaml
     * type: CustomButton
     * properties:
     *   text: Click Me
     * ```
     */
    val componentType: String

    /**
     * Called when the component is first instantiated from a ComponentModel.
     *
     * Use this lifecycle method to:
     * - Validate required properties
     * - Initialize internal state
     * - Set up event handlers
     *
     * @param model The component data model
     */
    fun onCreate(model: ComponentModel)

    /**
     * Renders the component to a platform-specific representation.
     *
     * Return types vary by platform:
     * - **Android**: `View` or `@Composable` function
     * - **iOS**: `UIView` or SwiftUI `View`
     * - **Web**: DOM `Element` or React component
     * - **JVM**: Swing `JComponent` or JavaFX `Node`
     *
     * @param model The component data model
     * @return Platform-specific UI representation
     */
    fun onRender(model: ComponentModel): Any

    /**
     * Called before the component is removed from the UI hierarchy.
     *
     * Use this lifecycle method to:
     * - Release resources (timers, subscriptions, file handles)
     * - Cancel pending operations
     * - Save state if needed
     */
    fun onDestroy()

    /**
     * Generates export code for this component in the specified format.
     *
     * Used by design tools to convert visual layouts into source code.
     *
     * @param model The component data model
     * @param format Export format (KOTLIN, XML, JSON, DSL, SVG)
     * @return Generated code as a string
     */
    fun generateExport(model: ComponentModel, format: ExportFormat): String {
        // Default implementation returns empty (components can override)
        return ""
    }
}

/**
 * Export format for code generation.
 */
enum class ExportFormat {
    /**
     * Kotlin source code (Compose, View builders).
     */
    KOTLIN,

    /**
     * Android XML layout.
     */
    XML,

    /**
     * JSON layout definition.
     */
    JSON,

    /**
     * AvaUI DSL compact syntax.
     */
    DSL,

    /**
     * SVG vector graphics with metadata.
     */
    SVG
}
