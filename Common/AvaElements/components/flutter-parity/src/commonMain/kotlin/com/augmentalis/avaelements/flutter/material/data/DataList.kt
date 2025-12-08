package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * DataList component - Flutter Material parity
 *
 * A key-value data list for displaying structured information in a clear format.
 * Commonly used for specifications, details, or property listings.
 *
 * **Flutter Equivalent:** Custom implementation (DataTable simplified)
 * **Material Design 3:** https://m3.material.io/components/lists/overview
 *
 * ## Features
 * - Key-value pair display
 * - Multiple layout modes (stacked, inline, grid)
 * - Optional dividers between items
 * - Customizable spacing
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * DataList(
 *     title = "Product Details",
 *     items = listOf(
 *         DataItem("Brand", "Acme Corp"),
 *         DataItem("Model", "X-2000"),
 *         DataItem("Price", "$599.99"),
 *         DataItem("Stock", "In Stock")
 *     ),
 *     layout = Layout.Stacked
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Optional title for the data list
 * @property items List of key-value data items
 * @property layout Layout mode for the data items
 * @property showDividers Whether to show dividers between items
 * @property dense Whether to use dense vertical spacing
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class DataList(
    override val type: String = "DataList",
    override val id: String? = null,
    val title: String? = null,
    val items: List<DataItem> = emptyList(),
    val layout: Layout = Layout.Stacked,
    val showDividers: Boolean = true,
    val dense: Boolean = false,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Single data item in the list
     */
    data class DataItem(
        val key: String,
        val value: String,
        val keyStyle: TextStyle? = null,
        val valueStyle: TextStyle? = null
    )

    /**
     * Text style for customization
     */
    data class TextStyle(
        val color: String? = null,
        val weight: FontWeight = FontWeight.Normal,
        val size: FontSize = FontSize.Medium
    )

    /**
     * Font weight options
     */
    enum class FontWeight {
        Light,
        Normal,
        Medium,
        Bold
    }

    /**
     * Font size options
     */
    enum class FontSize {
        Small,
        Medium,
        Large
    }

    /**
     * Layout mode for data items
     */
    enum class Layout {
        /** Key and value stacked vertically */
        Stacked,

        /** Key and value on same line */
        Inline,

        /** Two-column grid layout */
        Grid
    }

    companion object {
        /**
         * Create a simple data list with inline layout
         */
        fun inline(
            title: String? = null,
            items: List<DataItem>
        ) = DataList(
            title = title,
            items = items,
            layout = Layout.Inline
        )

        /**
         * Create a data list with grid layout
         */
        fun grid(
            title: String? = null,
            items: List<DataItem>
        ) = DataList(
            title = title,
            items = items,
            layout = Layout.Grid
        )
    }
}
