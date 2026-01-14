package com.augmentalis.magicui.ui.core.form

import com.augmentalis.magicui.components.core.*

/**
 * Toggle switch for binary on/off states.
 *
 * A toggle (also called switch) provides an on/off control similar to a
 * physical switch. Use for immediate state changes (unlike checkboxes
 * which are typically used in forms requiring submission).
 *
 * ## Usage Examples
 * ```kotlin
 * // Basic toggle
 * val toggle = ToggleComponent(
 *     label = "Enable Notifications",
 *     checked = true
 * )
 *
 * // With description
 * val toggle = ToggleComponent(
 *     label = "Dark Mode",
 *     description = "Use dark theme for better visibility at night",
 *     checked = false
 * )
 *
 * // Large toggle
 * val toggle = ToggleComponent(
 *     label = "WiFi",
 *     checked = true,
 *     size = ComponentSize.LG
 * )
 *
 * // Disabled toggle
 * val toggle = ToggleComponent(
 *     label = "Bluetooth",
 *     checked = false,
 *     enabled = false
 * )
 * ```
 *
 * @property label Display text for the toggle
 * @property checked Current state (true = on, false = off)
 * @property description Optional description text below label
 * @property size Toggle size (default MD)
 * @property enabled Whether user can interact (default true)
 * @since 1.0.0
 */
data class ToggleComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val label: String,
    val checked: Boolean = false,
    val description: String? = null,
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true
) : Component {
    init {
        require(label.isNotBlank()) { "Label cannot be blank" }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    /**
     * Returns a copy with checked state toggled.
     */
    fun toggle(): ToggleComponent = copy(checked = !checked)

    /**
     * Returns a copy with checked = true.
     */
    fun turnOn(): ToggleComponent = copy(checked = true)

    /**
     * Returns a copy with checked = false.
     */
    fun turnOff(): ToggleComponent = copy(checked = false)

    companion object {
        /**
         * Creates a toggle in the ON state.
         */
        fun on(label: String, enabled: Boolean = true) =
            ToggleComponent(label = label, checked = true, enabled = enabled)

        /**
         * Creates a toggle in the OFF state.
         */
        fun off(label: String, enabled: Boolean = true) =
            ToggleComponent(label = label, checked = false, enabled = enabled)
    }
}
