package com.augmentalis.avaelements.magic.buttons

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MagicButton component for user actions
 *
 * A cross-platform button that triggers actions when clicked/tapped.
 * Supports different variants (primary, secondary, outlined, text), icons, and loading states.
 *
 * @property id Unique identifier for the component
 * @property text Button text label
 * @property enabled Whether the button is enabled for user interaction
 * @property loading Whether the button is in loading state (shows spinner, disables clicks)
 * @property variant Visual variant of the button
 * @property icon Optional icon name/resource to display
 * @property iconPosition Position of icon relative to text
 * @property onClick Callback invoked when button is clicked (not serialized)
 * @property onLongPress Callback invoked on long press (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class MagicButton(
    override val type: String = "MagicButton",
    override val id: String? = null,
    val text: String,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val variant: Variant = Variant.Primary,
    val icon: String? = null,
    val iconPosition: IconPosition = IconPosition.Start,
    @Transient
    val onClick: (() -> Unit)? = null,
    @Transient
    val onLongPress: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Button visual variants
     */
    
    enum class Variant {
        /** Filled button with primary color */
        Primary,

        /** Filled button with secondary color */
        Secondary,

        /** Button with border, no fill */
        Outlined,

        /** Text-only button, no border or fill */
        Text,

        /** Filled button with danger/error color */
        Danger
    }

    /**
     * Icon position relative to text
     */
    
    enum class IconPosition {
        /** Icon before text */
        Start,

        /** Icon after text */
        End,

        /** Icon only (no text) */
        Only
    }

    companion object {
        /**
         * Create a primary button
         */
        fun primary(
            text: String,
            enabled: Boolean = true,
            onClick: (() -> Unit)? = null
        ) = MagicButton(
            text = text,
            enabled = enabled,
            variant = Variant.Primary,
            onClick = onClick
        )

        /**
         * Create a secondary button
         */
        fun secondary(
            text: String,
            enabled: Boolean = true,
            onClick: (() -> Unit)? = null
        ) = MagicButton(
            text = text,
            enabled = enabled,
            variant = Variant.Secondary,
            onClick = onClick
        )

        /**
         * Create an outlined button
         */
        fun outlined(
            text: String,
            enabled: Boolean = true,
            onClick: (() -> Unit)? = null
        ) = MagicButton(
            text = text,
            enabled = enabled,
            variant = Variant.Outlined,
            onClick = onClick
        )

        /**
         * Create a text button
         */
        fun text(
            text: String,
            enabled: Boolean = true,
            onClick: (() -> Unit)? = null
        ) = MagicButton(
            text = text,
            enabled = enabled,
            variant = Variant.Text,
            onClick = onClick
        )
    }
}
