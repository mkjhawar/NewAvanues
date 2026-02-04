package com.augmentalis.avaelements.components.phase1.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Size
import kotlinx.serialization.Transient

/**
 * Icon component for displaying icons
 *
 * A cross-platform icon display component supporting Material Icons,
 * SF Symbols, custom icon fonts, and SVG icons.
 *
 * @property id Unique identifier for the component
 * @property name Icon name (e.g., "home", "settings", "user")
 * @property iconSet Icon set to use (Material, SF Symbols, custom)
 * @property size Icon size
 * @property color Icon tint color
 * @property contentDescription Accessibility description of icon
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class Icon(
    override val type: String = "Icon",
    override val id: String? = null,
    val name: String,
    val iconSet: IconSet = IconSet.Material,
    val size: Size = Size.Fixed(24f),
    val color: Color = Color.Black,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Icon set enumeration
     */
    
    enum class IconSet {
        /** Material Design Icons (Android default) */
        Material,

        /** SF Symbols (iOS/macOS default) */
        SFSymbols,

        /** Font Awesome */
        FontAwesome,

        /** Custom icon font */
        Custom
    }

    companion object {
        /**
         * Create Material Design icon
         */
        fun material(
            name: String,
            size: Size = Size.Fixed(24f),
            color: Color = Color.Black,
            contentDescription: String? = null
        ) = Icon(
            name = name,
            iconSet = IconSet.Material,
            size = size,
            color = color,
            contentDescription = contentDescription
        )

        /**
         * Create SF Symbol icon
         */
        fun sfSymbol(
            name: String,
            size: Size = Size.Fixed(24f),
            color: Color = Color.Black,
            contentDescription: String? = null
        ) = Icon(
            name = name,
            iconSet = IconSet.SFSymbols,
            size = size,
            color = color,
            contentDescription = contentDescription
        )

        /**
         * Create Font Awesome icon
         */
        fun fontAwesome(
            name: String,
            size: Size = Size.Fixed(24f),
            color: Color = Color.Black,
            contentDescription: String? = null
        ) = Icon(
            name = name,
            iconSet = IconSet.FontAwesome,
            size = size,
            color = color,
            contentDescription = contentDescription
        )
    }
}
