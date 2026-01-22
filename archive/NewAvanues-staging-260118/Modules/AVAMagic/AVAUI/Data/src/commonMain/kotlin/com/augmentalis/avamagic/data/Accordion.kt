package com.augmentalis.avamagic.avaui.data

import com.augmentalis.avamagic.components.core.*
import com.augmentalis.avamagic.components.core.ComponentStyle
import com.augmentalis.avamagic.components.core.Size
import com.augmentalis.avamagic.components.core.Color
import com.augmentalis.avamagic.components.core.Spacing
import com.augmentalis.avamagic.components.core.Border
import com.augmentalis.avamagic.components.core.Shadow

/**
 * Accordion Component
 *
 * An accordion component that displays collapsible content panels,
 * allowing users to expand and collapse sections.
 *
 * Features:
 * - Expandable/collapsible sections
 * - Single or multiple expansion
 * - Custom content in each section
 * - Toggle animations
 *
 * Platform mappings:
 * - Android: ExpandableListView or custom
 * - iOS: UITableView with expandable cells
 * - Web: Details/summary or custom accordion
 *
 * Usage:
 * ```kotlin
 * Accordion(
 *     items = listOf(
 *         AccordionItem(
 *             id = "1",
 *             title = "Section 1",
 *             content = TextComponent("Content 1")
 *         ),
 *         AccordionItem(
 *             id = "2",
 *             title = "Section 2",
 *             content = TextComponent("Content 2")
 *         )
 *     ),
 *     expandedIndices = setOf(0),
 *     allowMultiple = false
 * )
 * ```
 */
data class AccordionComponent(
    val type: String = "Accordion",
    val items: List<AccordionItem>,
    val expandedIndices: Set<Int> = emptySet(),
    val allowMultiple: Boolean = false,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onToggle: ((Int) -> Unit)? = null
) : Component {
    init {
        require(items.isNotEmpty()) { "Accordion must have at least one item" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Individual accordion item
 */
data class AccordionItem(
    val id: String,
    val title: String,
    val content: Component
)
