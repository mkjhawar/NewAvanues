package com.augmentalis.avamagic.renderer.ios.mappers

import com.augmentalis.avamagic.core.Theme
import com.augmentalis.avamagic.dsl.*
import com.augmentalis.avamagic.renderer.ios.bridge.*

/**
 * Data Component Mappers for iOS (SwiftUI)
 *
 * Maps 10 data components to SwiftUI equivalents:
 * - Accordion, Carousel, Timeline, DataGrid, DataTable
 * - ListComponent, TreeView, ChipComponent, Paper, EmptyState
 */

// ======================
// Accordion Mapper
// ======================

object AccordionMapper {
    fun map(
        component: AccordionComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val children = component.items.mapIndexed { index, item ->
            val isExpanded = index in component.expandedIndices

            SwiftUIView(
                type = ViewType.VStack,
                id = item.id,
                properties = mapOf(
                    "alignment" to ".leading",
                    "spacing" to 0
                ),
                children = listOf(
                    // Header button
                    SwiftUIView(
                        type = ViewType.Button,
                        properties = mapOf(
                            "action" to "toggleAccordion($index)"
                        ),
                        children = listOf(
                            SwiftUIView(
                                type = ViewType.HStack,
                                properties = mapOf(
                                    "spacing" to 8
                                ),
                                children = listOf(
                                    SwiftUIView(
                                        type = ViewType.Text,
                                        properties = mapOf(
                                            "text" to item.title,
                                            "font" to ".headline"
                                        )
                                    ),
                                    SwiftUIView(
                                        type = ViewType.Image,
                                        properties = mapOf(
                                            "systemName" to if (isExpanded) "chevron.up" else "chevron.down"
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    // Content (conditionally shown)
                    if (isExpanded) {
                        SwiftUIView(
                            type = ViewType.VStack,
                            properties = mapOf(
                                "alignment" to ".leading"
                            ),
                            modifiers = listOf(
                                SwiftUIModifier.Padding(16f)
                            ),
                            children = listOf(renderChild(item.content))
                        )
                    } else {
                        SwiftUIView(type = ViewType.EmptyView)
                    }
                )
            )
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 8
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = children
        )
    }
}

// ======================
// Carousel Mapper
// ======================

object CarouselMapper {
    fun map(
        component: CarouselComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val pages = component.items.map { item ->
            renderChild(item)
        }

        val children = mutableListOf<SwiftUIView>()

        // TabView with PageTabViewStyle
        children.add(
            SwiftUIView(
                type = ViewType.TabView,
                properties = mapOf(
                    "selection" to component.currentIndex,
                    "tabViewStyle" to "PageTabViewStyle(indexDisplayMode: .automatic)"
                ),
                children = pages.mapIndexed { index, page ->
                    SwiftUIView(
                        type = ViewType.VStack,
                        properties = mapOf(
                            "tag" to index
                        ),
                        children = listOf(page)
                    )
                }
            )
        )

        // Custom indicators if needed
        if (component.showIndicators) {
            children.add(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf(
                        "spacing" to 8
                    ),
                    children = component.items.indices.map { index ->
                        SwiftUIView(
                            type = ViewType.Circle,
                            properties = mapOf(
                                "fill" to if (index == component.currentIndex)
                                    "Color.primary"
                                else
                                    "Color.secondary.opacity(0.3)"
                            ),
                            modifiers = listOf(
                                SwiftUIModifier.Frame(width = 8f, height = 8f)
                            )
                        )
                    }
                )
            )
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "spacing" to 8
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = children
        )
    }
}

// ======================
// Timeline Mapper
// ======================

object TimelineMapper {
    fun map(
        component: TimelineComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val children = component.items.mapIndexed { index, item ->
            val isLast = index == component.items.lastIndex

            SwiftUIView(
                type = ViewType.HStack,
                properties = mapOf(
                    "alignment" to ".top",
                    "spacing" to 12
                ),
                children = listOf(
                    // Timeline indicator
                    SwiftUIView(
                        type = ViewType.VStack,
                        properties = mapOf(
                            "spacing" to 0
                        ),
                        children = listOf(
                            // Circle/Icon
                            SwiftUIView(
                                type = ViewType.ZStack,
                                children = listOf(
                                    SwiftUIView(
                                        type = ViewType.Circle,
                                        properties = mapOf(
                                            "fill" to if (item.completed)
                                                "Color.accentColor"
                                            else
                                                "Color.secondary.opacity(0.3)"
                                        ),
                                        modifiers = listOf(
                                            SwiftUIModifier.Frame(width = 24f, height = 24f)
                                        )
                                    ),
                                    if (item.icon != null) {
                                        SwiftUIView(
                                            type = ViewType.Image,
                                            properties = mapOf(
                                                "systemName" to convertToSFSymbol(item.icon),
                                                "foregroundColor" to if (item.completed)
                                                    "Color.white"
                                                else
                                                    "Color.secondary"
                                            ),
                                            modifiers = listOf(
                                                SwiftUIModifier.Frame(width = 12f, height = 12f)
                                            )
                                        )
                                    } else {
                                        SwiftUIView(type = ViewType.EmptyView)
                                    }
                                )
                            ),
                            // Connector line
                            if (!isLast) {
                                SwiftUIView(
                                    type = ViewType.Rectangle,
                                    properties = mapOf(
                                        "fill" to "Color.secondary.opacity(0.3)"
                                    ),
                                    modifiers = listOf(
                                        SwiftUIModifier.Frame(width = 2f, height = 40f)
                                    )
                                )
                            } else {
                                SwiftUIView(type = ViewType.EmptyView)
                            }
                        )
                    ),
                    // Content
                    SwiftUIView(
                        type = ViewType.VStack,
                        properties = mapOf(
                            "alignment" to ".leading",
                            "spacing" to 4
                        ),
                        children = listOfNotNull(
                            SwiftUIView(
                                type = ViewType.Text,
                                properties = mapOf(
                                    "text" to item.title,
                                    "font" to ".headline"
                                )
                            ),
                            item.description?.let {
                                SwiftUIView(
                                    type = ViewType.Text,
                                    properties = mapOf(
                                        "text" to it,
                                        "font" to ".subheadline",
                                        "foregroundColor" to "Color.secondary"
                                    )
                                )
                            },
                            item.timestamp?.let {
                                SwiftUIView(
                                    type = ViewType.Text,
                                    properties = mapOf(
                                        "text" to it,
                                        "font" to ".caption",
                                        "foregroundColor" to "Color.secondary"
                                    )
                                )
                            }
                        )
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 0
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = children
        )
    }

    private fun convertToSFSymbol(iconName: String?): String {
        if (iconName == null) return "circle.fill"
        return when (iconName.lowercase()) {
            "check", "done" -> "checkmark"
            "star" -> "star.fill"
            "person" -> "person.fill"
            "location" -> "location.fill"
            else -> "circle.fill"
        }
    }
}

// ======================
// DataGrid Mapper
// ======================

object DataGridMapper {
    fun map(
        component: DataGridComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Header row
        children.add(
            SwiftUIView(
                type = ViewType.HStack,
                properties = mapOf(
                    "spacing" to 0
                ),
                modifiers = listOf(
                    SwiftUIModifier.Background(SwiftUIColor(0.9f, 0.9f, 0.9f, 1f))
                ),
                children = component.columns.map { column ->
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf(
                            "text" to column.label,
                            "font" to ".headline",
                            "frame" to mapOf(
                                "width" to (column.width ?: 120f),
                                "alignment" to ".leading"
                            )
                        ),
                        modifiers = listOf(
                            SwiftUIModifier.Padding(12f)
                        )
                    )
                }
            )
        )

        // Data rows
        children.add(
            SwiftUIView(
                type = ViewType.ScrollView,
                children = listOf(
                    SwiftUIView(
                        type = ViewType.VStack,
                        properties = mapOf(
                            "spacing" to 0
                        ),
                        children = component.currentPageRows.mapIndexed { index, row ->
                            val isSelected = index in component.selectedRowIndices
                            SwiftUIView(
                                type = ViewType.HStack,
                                properties = mapOf(
                                    "spacing" to 0
                                ),
                                modifiers = listOf(
                                    if (isSelected) {
                                        SwiftUIModifier.Background(SwiftUIColor(0.9f, 0.95f, 1f, 1f))
                                    } else {
                                        SwiftUIModifier.Background(SwiftUIColor(1f, 1f, 1f, 1f))
                                    }
                                ),
                                children = component.columns.map { column ->
                                    SwiftUIView(
                                        type = ViewType.Text,
                                        properties = mapOf(
                                            "text" to (row[column.key]?.toString() ?: ""),
                                            "frame" to mapOf(
                                                "width" to (column.width ?: 120f),
                                                "alignment" to ".leading"
                                            )
                                        ),
                                        modifiers = listOf(
                                            SwiftUIModifier.Padding(12f)
                                        )
                                    )
                                }
                            )
                        }
                    )
                )
            )
        )

        // Pagination
        if (component.paginated) {
            children.add(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf(
                        "spacing" to 8
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Padding(8f)
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf(
                                "text" to "Page ${component.currentPage} of ${component.totalPages}",
                                "font" to ".caption"
                            )
                        ),
                        SwiftUIView(
                            type = ViewType.Button,
                            properties = mapOf(
                                "title" to "Previous",
                                "disabled" to (component.currentPage <= 1)
                            )
                        ),
                        SwiftUIView(
                            type = ViewType.Button,
                            properties = mapOf(
                                "title" to "Next",
                                "disabled" to (component.currentPage >= component.totalPages)
                            )
                        )
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 0
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = children
        )
    }
}

// ======================
// DataTable Mapper
// ======================

object DataTableMapper {
    fun map(
        component: DataTableComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Header row
        children.add(
            SwiftUIView(
                type = ViewType.HStack,
                properties = mapOf(
                    "spacing" to 0
                ),
                modifiers = listOf(
                    SwiftUIModifier.Background(SwiftUIColor(0.9f, 0.9f, 0.9f, 1f))
                ),
                children = component.headers.map { header ->
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf(
                            "text" to header,
                            "font" to ".headline"
                        ),
                        modifiers = listOf(
                            SwiftUIModifier.Padding(12f),
                            SwiftUIModifier.Frame(maxWidth = Float.MAX_VALUE)
                        )
                    )
                }
            )
        )

        // Data rows
        component.rows.forEachIndexed { index, row ->
            val isSelected = index in component.selectedRows
            children.add(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf(
                        "spacing" to 0
                    ),
                    modifiers = listOf(
                        if (isSelected) {
                            SwiftUIModifier.Background(SwiftUIColor(0.9f, 0.95f, 1f, 1f))
                        } else {
                            SwiftUIModifier.Background(SwiftUIColor(1f, 1f, 1f, 1f))
                        }
                    ),
                    children = row.map { cell ->
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf(
                                "text" to cell
                            ),
                            modifiers = listOf(
                                SwiftUIModifier.Padding(12f),
                                SwiftUIModifier.Frame(maxWidth = Float.MAX_VALUE)
                            )
                        )
                    }
                )
            )
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 0
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = children
        )
    }
}

// ======================
// List Component Mapper
// ======================

object ListComponentMapper {
    fun map(
        component: ListComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.List,
            id = component.id,
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.items.mapIndexed { index, item ->
                val isSelected = index in component.selectedIndices

                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf(
                        "spacing" to 12
                    ),
                    modifiers = if (isSelected) {
                        listOf(SwiftUIModifier.Background(SwiftUIColor(0.9f, 0.95f, 1f, 0.5f)))
                    } else {
                        emptyList()
                    },
                    children = listOfNotNull(
                        // Leading content (icon or avatar)
                        when {
                            item.avatar != null -> SwiftUIView(
                                type = ViewType.AsyncImage,
                                properties = mapOf(
                                    "url" to item.avatar
                                ),
                                modifiers = listOf(
                                    SwiftUIModifier.Frame(width = 40f, height = 40f),
                                    SwiftUIModifier.CornerRadius(20f)
                                )
                            )
                            item.icon != null -> SwiftUIView(
                                type = ViewType.Image,
                                properties = mapOf(
                                    "systemName" to convertToSFSymbol(item.icon)
                                )
                            )
                            else -> null
                        },
                        // Text content
                        SwiftUIView(
                            type = ViewType.VStack,
                            properties = mapOf(
                                "alignment" to ".leading"
                            ),
                            children = listOfNotNull(
                                SwiftUIView(
                                    type = ViewType.Text,
                                    properties = mapOf(
                                        "text" to item.primary,
                                        "font" to ".body"
                                    )
                                ),
                                item.secondary?.let {
                                    SwiftUIView(
                                        type = ViewType.Text,
                                        properties = mapOf(
                                            "text" to it,
                                            "font" to ".caption",
                                            "foregroundColor" to "Color.secondary"
                                        )
                                    )
                                }
                            )
                        ),
                        // Trailing content
                        item.trailing?.let { renderChild(it) }
                    )
                )
            }
        )
    }

    private fun convertToSFSymbol(iconName: String?): String {
        if (iconName == null) return "circle"
        return when (iconName.lowercase()) {
            "person" -> "person.fill"
            "email", "mail" -> "envelope.fill"
            "phone" -> "phone.fill"
            "star" -> "star.fill"
            "heart", "favorite" -> "heart.fill"
            "settings" -> "gearshape.fill"
            else -> iconName
        }
    }
}

// ======================
// TreeView Mapper
// ======================

object TreeViewMapper {
    fun map(
        component: TreeViewComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 0
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = renderNodes(component.nodes, component.expandedIds, 0)
        )
    }

    private fun renderNodes(
        nodes: List<TreeNode>,
        expandedIds: Set<String>,
        depth: Int
    ): List<SwiftUIView> {
        val result = mutableListOf<SwiftUIView>()

        nodes.forEach { node ->
            val isExpanded = node.id in expandedIds
            val hasChildren = node.children.isNotEmpty()

            // Node row
            result.add(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf(
                        "spacing" to 8
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Padding(
                            top = 8f,
                            bottom = 8f,
                            leading = (depth * 24 + 8).toFloat(),
                            trailing = 8f
                        )
                    ),
                    children = listOfNotNull(
                        // Expand/collapse button
                        if (hasChildren) {
                            SwiftUIView(
                                type = ViewType.Button,
                                properties = mapOf(
                                    "action" to "toggleNode(${node.id})"
                                ),
                                children = listOf(
                                    SwiftUIView(
                                        type = ViewType.Image,
                                        properties = mapOf(
                                            "systemName" to if (isExpanded) "chevron.down" else "chevron.right"
                                        ),
                                        modifiers = listOf(
                                            SwiftUIModifier.Frame(width = 16f, height = 16f)
                                        )
                                    )
                                )
                            )
                        } else {
                            SwiftUIView(
                                type = ViewType.Spacer,
                                modifiers = listOf(
                                    SwiftUIModifier.Frame(width = 24f)
                                )
                            )
                        },
                        // Icon
                        node.icon?.let {
                            SwiftUIView(
                                type = ViewType.Image,
                                properties = mapOf(
                                    "systemName" to convertToSFSymbol(it)
                                ),
                                modifiers = listOf(
                                    SwiftUIModifier.Frame(width = 20f, height = 20f)
                                )
                            )
                        },
                        // Label
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf(
                                "text" to node.label
                            )
                        )
                    )
                )
            )

            // Children (if expanded)
            if (isExpanded && hasChildren) {
                result.addAll(renderNodes(node.children, expandedIds, depth + 1))
            }
        }

        return result
    }

    private fun convertToSFSymbol(iconName: String): String {
        return when (iconName.lowercase()) {
            "folder" -> "folder.fill"
            "file" -> "doc.fill"
            "document" -> "doc.text.fill"
            else -> iconName
        }
    }
}

// ======================
// Chip Component Mapper
// ======================

object ChipComponentMapper {
    fun map(
        component: ChipComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Icon
        component.icon?.let {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to convertToSFSymbol(it)
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Frame(width = 16f, height = 16f)
                    )
                )
            )
        }

        // Label
        children.add(
            SwiftUIView(
                type = ViewType.Text,
                properties = mapOf(
                    "text" to component.label,
                    "font" to ".subheadline"
                )
            )
        )

        // Delete button
        if (component.deletable) {
            children.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "action" to "deleteChip"
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf(
                                "systemName" to "xmark"
                            ),
                            modifiers = listOf(
                                SwiftUIModifier.Frame(width = 12f, height = 12f)
                            )
                        )
                    )
                )
            )
        }

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(SwiftUIModifier.Padding(horizontal = 12f, vertical = 8f))

        if (component.selected) {
            modifiers.add(SwiftUIModifier.Background(SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)))
            modifiers.add(SwiftUIModifier.ForegroundColor(SwiftUIColor(1f, 1f, 1f, 1f)))
        } else {
            modifiers.add(SwiftUIModifier.Background(SwiftUIColor(0.9f, 0.9f, 0.9f, 1f)))
        }
        modifiers.add(SwiftUIModifier.CornerRadius(16f))
        modifiers.addAll(ModifierConverter.convert(component.modifiers))

        return SwiftUIView(
            type = ViewType.Button,
            id = component.id,
            properties = mapOf(
                "action" to "chipTapped"
            ),
            modifiers = modifiers,
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf(
                        "spacing" to 4
                    ),
                    children = children
                )
            )
        )
    }

    private fun convertToSFSymbol(iconName: String): String {
        return when (iconName.lowercase()) {
            "label", "tag" -> "tag.fill"
            "star" -> "star.fill"
            "check" -> "checkmark"
            else -> iconName
        }
    }
}

// ======================
// Paper Mapper
// ======================

object PaperMapper {
    fun map(
        component: PaperComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val shadowRadius = (component.elevation * 2).toFloat()

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 8
            ),
            modifiers = listOf(
                SwiftUIModifier.Padding(16f),
                SwiftUIModifier.Background(SwiftUIColor(1f, 1f, 1f, 1f)),
                SwiftUIModifier.CornerRadius(8f),
                SwiftUIModifier.Shadow(
                    color = SwiftUIColor(0f, 0f, 0f, 0.1f),
                    radius = shadowRadius,
                    x = 0f,
                    y = component.elevation.toFloat()
                )
            ) + ModifierConverter.convert(component.modifiers),
            children = component.children.map { child ->
                renderChild(child)
            }
        )
    }
}

// ======================
// EmptyState Mapper
// ======================

object EmptyStateMapper {
    fun map(
        component: EmptyStateComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Icon
        component.icon?.let {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to convertToSFSymbol(it),
                        "font" to "Font.system(size: 64)",
                        "foregroundColor" to "Color.secondary"
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Padding(bottom = 16f)
                    )
                )
            )
        }

        // Title
        children.add(
            SwiftUIView(
                type = ViewType.Text,
                properties = mapOf(
                    "text" to component.title,
                    "font" to ".title2"
                ),
                modifiers = listOf(
                    SwiftUIModifier.Padding(bottom = 8f)
                )
            )
        )

        // Description
        component.description?.let {
            children.add(
                SwiftUIView(
                    type = ViewType.Text,
                    properties = mapOf(
                        "text" to it,
                        "font" to ".body",
                        "foregroundColor" to "Color.secondary",
                        "multilineTextAlignment" to ".center"
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Padding(bottom = 16f)
                    )
                )
            )
        }

        // Action button
        component.action?.let {
            children.add(renderChild(it))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".center",
                "spacing" to 0
            ),
            modifiers = listOf(
                SwiftUIModifier.Frame(maxWidth = Float.MAX_VALUE),
                SwiftUIModifier.Padding(32f)
            ) + ModifierConverter.convert(component.modifiers),
            children = children
        )
    }

    private fun convertToSFSymbol(iconName: String): String {
        return when (iconName.lowercase()) {
            "inbox", "mail" -> "tray"
            "search" -> "magnifyingglass"
            "error", "warning" -> "exclamationmark.triangle"
            "info" -> "info.circle"
            "empty", "folder" -> "folder"
            "document", "file" -> "doc"
            else -> "square.dashed"
        }
    }
}
