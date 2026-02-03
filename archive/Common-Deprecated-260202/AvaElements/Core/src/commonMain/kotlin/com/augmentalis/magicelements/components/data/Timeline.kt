package com.augmentalis.avaelements.components.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * Timeline Component
 *
 * A timeline component that displays events in chronological order,
 * typically with timestamps and descriptions.
 *
 * Features:
 * - Vertical or horizontal orientation
 * - Timestamps
 * - Icons and colors for events
 * - Descriptions
 *
 * Platform mappings:
 * - Android: RecyclerView with custom layout
 * - iOS: UITableView with custom cells
 * - Web: Custom timeline layout
 *
 * Usage:
 * ```kotlin
 * Timeline(
 *     items = listOf(
 *         TimelineItem(
 *             id = "1",
 *             timestamp = "2025-01-15 10:30",
 *             title = "Order placed",
 *             description = "Your order has been placed",
 *             icon = "check_circle",
 *             color = Color.Green
 *         ),
 *         TimelineItem(
 *             id = "2",
 *             timestamp = "2025-01-16 14:20",
 *             title = "Shipped",
 *             description = "Your order is on its way"
 *         )
 *     ),
 *     orientation = Orientation.Vertical
 * )
 * ```
 */
data class TimelineComponent(
    override val type: String = "Timeline",
    val items: List<TimelineItem>,
    val orientation: Orientation = Orientation.Vertical,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    init {
        require(items.isNotEmpty()) { "Timeline must have at least one item" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Individual timeline item
 */
data class TimelineItem(
    val id: String,
    val timestamp: String,
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: Color? = null
)
