package com.augmentalis.avaelements.components.phase1.layout

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.CornerRadius
import com.augmentalis.avaelements.core.types.Shadow
import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Transient

/**
 * Card component for grouped content
 *
 * A Material Design-style card container with elevation/shadow,
 * rounded corners, and optional click handling.
 *
 * @property id Unique identifier for the component
 * @property child Child component to contain in card
 * @property elevation Shadow elevation level (0-24)
 * @property backgroundColor Card background color
 * @property cornerRadius Corner radius for rounded corners
 * @property border Optional border
 * @property onClick Optional click handler (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class Card(
    override val type: String = "Card",
    override val id: String? = null,
    val child: Component? = null,
    val elevation: Float = 2f,
    val backgroundColor: Color = Color.White,
    val cornerRadius: CornerRadius = CornerRadius.Medium,
    val border: Border? = null,
    @Transient
    val onClick: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    init {
        require(elevation >= 0f && elevation <= 24f) { "Elevation must be between 0 and 24" }
    }

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    companion object {
        /**
         * Create a flat card (no elevation)
         */
        fun flat(
            child: Component,
            backgroundColor: Color = Color.White
        ) = Card(
            child = child,
            elevation = 0f,
            backgroundColor = backgroundColor
        )

        /**
         * Create an elevated card
         */
        fun elevated(
            child: Component,
            elevation: Float = 4f,
            backgroundColor: Color = Color.White
        ) = Card(
            child = child,
            elevation = elevation,
            backgroundColor = backgroundColor
        )

        /**
         * Create a clickable card
         */
        fun clickable(
            child: Component,
            elevation: Float = 2f,
            onClick: () -> Unit
        ) = Card(
            child = child,
            elevation = elevation,
            onClick = onClick
        )

        /**
         * Create a padded card
         */
        fun padded(
            child: Component,
            padding: Float = 16f,
            elevation: Float = 2f
        ) = Card(
            child = child,
            elevation = elevation,
            modifiers = listOf(Modifier.Padding(com.augmentalis.avaelements.core.types.Spacing.all(padding)))
        )
    }
}
