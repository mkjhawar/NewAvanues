package com.augmentalis.avanues.avamagic.ui.core.data

import com.augmentalis.avanues.avamagic.components.core.*

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
    val items: List<AccordionItem>,
    val expandedIndices: Set<Int> = emptySet(),
    val allowMultiple: Boolean = false,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList(),
    val onToggle: ((Int) -> Unit)? = null
) {
    init {
        require(items.isNotEmpty()) { "Accordion must have at least one item" }
    }

}

/**
 * Individual accordion item
 */
data class AccordionItem(
    val id: String,
    val title: String,
    val content: Any
)
