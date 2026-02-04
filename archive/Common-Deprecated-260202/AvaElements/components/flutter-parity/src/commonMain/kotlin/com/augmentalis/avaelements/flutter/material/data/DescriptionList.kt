package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * DescriptionList component - Flutter Material parity
 *
 * A definition/description list for displaying terms with their definitions or descriptions.
 * Similar to HTML's <dl> element, optimized for glossaries, FAQs, and explanatory content.
 *
 * **Flutter Equivalent:** Custom implementation
 * **Material Design 3:** https://m3.material.io/components/lists/overview
 *
 * ## Features
 * - Term and description pairing
 * - Multiple description layout modes
 * - Optional numbering for definitions
 * - Expandable descriptions
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * DescriptionList(
 *     title = "Glossary",
 *     items = listOf(
 *         DescriptionItem(
 *             term = "API",
 *             description = "Application Programming Interface - a set of protocols..."
 *         ),
 *         DescriptionItem(
 *             term = "SDK",
 *             description = "Software Development Kit - collection of tools..."
 *         )
 *     )
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Optional title for the description list
 * @property items List of term-description pairs
 * @property numbered Whether to number the items
 * @property expandable Whether descriptions can be expanded/collapsed
 * @property defaultExpanded Initial expansion state (if expandable)
 * @property dense Whether to use dense vertical spacing
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class DescriptionList(
    override val type: String = "DescriptionList",
    override val id: String? = null,
    val title: String? = null,
    val items: List<DescriptionItem> = emptyList(),
    val numbered: Boolean = false,
    val expandable: Boolean = false,
    val defaultExpanded: Boolean = true,
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
     * Single term-description pair
     */
    data class DescriptionItem(
        val term: String,
        val description: String,
        val icon: String? = null,
        val badge: String? = null
    )

    companion object {
        /**
         * Create a numbered description list
         */
        fun numbered(
            title: String? = null,
            items: List<DescriptionItem>
        ) = DescriptionList(
            title = title,
            items = items,
            numbered = true
        )

        /**
         * Create an expandable description list (for FAQs)
         */
        fun faq(
            title: String? = null,
            items: List<DescriptionItem>
        ) = DescriptionList(
            title = title,
            items = items,
            expandable = true,
            defaultExpanded = false
        )
    }
}
