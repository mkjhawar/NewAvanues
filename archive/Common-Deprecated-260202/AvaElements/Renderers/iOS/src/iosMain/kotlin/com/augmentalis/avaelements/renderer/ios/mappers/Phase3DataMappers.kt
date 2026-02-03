package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.components.phase3.data.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.Theme

/**
 * iOS SwiftUI mappers for Phase 3 Data Components
 *
 * Components: Table, ListComponent, Accordion, Stepper, Timeline, TreeView,
 * Carousel, Paper, EmptyState, DataGrid
 */

object TableMapper {
    fun map(
        component: Table,
        theme: Theme?,
        childMapper: (Component) -> SwiftUIView
    ): SwiftUIView {
        val views = mutableListOf<SwiftUIView>()

        // Header row
        if (component.columns.isNotEmpty()) {
            val headerCells = component.columns.map { column ->
                SwiftUIView.text(
                    content = column.label,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Headline),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.padding(8f)
                    )
                )
            }
            views.add(SwiftUIView.hStack(
                spacing = 0f,
                alignment = VerticalAlignment.Center,
                children = headerCells,
                modifiers = listOf(
                    SwiftUIModifier.background(SwiftUIColor.systemGroupedBackground)
                )
            ))
        }

        // Data rows
        component.rows.forEachIndexed { rowIndex, row ->
            val rowCells = row.cells.map { cell ->
                val comp = cell.component
                if (comp != null) {
                    childMapper(comp)
                } else {
                    SwiftUIView.text(
                        content = cell.content,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Body),
                            SwiftUIModifier.padding(8f)
                        )
                    )
                }
            }
            views.add(SwiftUIView.hStack(
                spacing = 0f,
                alignment = VerticalAlignment.Center,
                children = rowCells,
                modifiers = if (component.striped && rowIndex % 2 == 1) {
                    listOf(SwiftUIModifier.background(SwiftUIColor.systemGroupedBackground.withOpacity(0.5f)))
                } else emptyList()
            ))
        }

        return SwiftUIView.vStack(
            spacing = 0f,
            alignment = HorizontalAlignment.Leading,
            children = views,
            modifiers = ModifierConverter.convert(component.modifiers, theme)
        )
    }
}

object ListMapper {
    fun map(
        component: ListComponent,
        theme: Theme?,
        childMapper: (Component) -> SwiftUIView
    ): SwiftUIView {
        val itemViews = component.items.mapIndexed { index, item ->
            val isSelected = component.selectable && component.selectedIndices.contains(index)

            val children = mutableListOf<SwiftUIView>()

            // Icon or avatar
            val icon = item.icon
            val avatar = item.avatar
            if (icon != null) {
                children.add(SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to icon),
                    modifiers = listOf(SwiftUIModifier.fontSize(20f))
                ))
            } else if (avatar != null) {
                children.add(SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("url" to avatar),
                    modifiers = listOf(
                        SwiftUIModifier.frame(SizeValue.Fixed(40f), SizeValue.Fixed(40f)),
                        SwiftUIModifier.clipShape("circle")
                    )
                ))
            }

            // Text content
            val textStack = SwiftUIView.vStack(
                spacing = 4f,
                alignment = HorizontalAlignment.Leading,
                children = listOfNotNull(
                    SwiftUIView.text(item.primary, modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))),
                    item.secondary?.let {
                        SwiftUIView.text(it, modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Subheadline),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                        ))
                    }
                )
            )
            children.add(textStack)

            // Trailing component
            item.trailing?.let { children.add(childMapper(it)) }

            SwiftUIView.hStack(
                spacing = 12f,
                alignment = VerticalAlignment.Center,
                children = children,
                modifiers = listOf(
                    SwiftUIModifier.padding(12f),
                    if (isSelected) SwiftUIModifier.background(SwiftUIColor.accentColor.withOpacity(0.1f))
                    else SwiftUIModifier.background(SwiftUIColor.clear)
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Custom("List"),
            id = component.id,
            properties = emptyMap(),
            modifiers = ModifierConverter.convert(component.modifiers, theme),
            children = itemViews
        )
    }
}

object AccordionMapper {
    fun map(
        component: Accordion,
        theme: Theme?,
        childMapper: (Component) -> SwiftUIView
    ): SwiftUIView {
        val sectionViews = component.items.mapIndexed { index, item ->
            val isExpanded = component.expandedIndices.contains(index)

            val headerView = SwiftUIView.hStack(
                spacing = 8f,
                alignment = VerticalAlignment.Center,
                children = listOf(
                    SwiftUIView.text(item.title, modifiers = listOf(SwiftUIModifier.font(FontStyle.Headline))),
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to if (isExpanded) "chevron.up" else "chevron.down")
                    )
                ),
                modifiers = listOf(SwiftUIModifier.padding(12f))
            )

            val children = mutableListOf(headerView)
            if (isExpanded) {
                children.add(childMapper(item.content))
            }

            SwiftUIView.vStack(
                spacing = 0f,
                alignment = HorizontalAlignment.Leading,
                children = children
            )
        }

        return SwiftUIView.vStack(
            spacing = 1f,
            alignment = HorizontalAlignment.Leading,
            children = sectionViews,
            modifiers = ModifierConverter.convert(component.modifiers, theme),
            id = component.id
        )
    }
}

object StepperMapper {
    fun map(component: Stepper, theme: Theme?): SwiftUIView {
        val stepViews = component.steps.mapIndexed { index, step ->
            val isActive = index == component.currentStep
            val isCompleted = index < component.currentStep

            val indicatorColor = when {
                isCompleted -> SwiftUIColor.system("green")
                isActive -> SwiftUIColor.accentColor
                else -> SwiftUIColor.systemGray
            }

            val icon = when (step.status) {
                StepStatus.Completed -> "checkmark.circle.fill"
                StepStatus.Error -> "exclamationmark.circle.fill"
                StepStatus.Active -> "circle.fill"
                StepStatus.Pending -> "circle"
            }

            val stepContent = SwiftUIView.vStack(
                spacing = 4f,
                alignment = HorizontalAlignment.Center,
                children = listOfNotNull(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to icon),
                        modifiers = listOf(
                            SwiftUIModifier.foregroundColor(indicatorColor),
                            SwiftUIModifier.fontSize(24f)
                        )
                    ),
                    SwiftUIView.text(step.label, modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        if (isActive) SwiftUIModifier.fontWeight(FontWeight.Bold) else SwiftUIModifier.fontWeight(FontWeight.Regular)
                    )),
                    step.description?.let {
                        SwiftUIView.text(it, modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Caption2),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                        ))
                    }
                )
            )

            stepContent
        }

        val container = if (component.orientation == Orientation.Horizontal) {
            SwiftUIView.hStack(
                spacing = 16f,
                alignment = VerticalAlignment.Top,
                children = stepViews
            )
        } else {
            SwiftUIView.vStack(
                spacing = 16f,
                alignment = HorizontalAlignment.Leading,
                children = stepViews
            )
        }

        return SwiftUIView(
            type = container.type,
            id = component.id,
            properties = emptyMap(),
            modifiers = ModifierConverter.convert(component.modifiers, theme),
            children = container.children
        )
    }
}

object TimelineMapper {
    fun map(
        component: Timeline,
        theme: Theme?,
        childMapper: (Component) -> SwiftUIView
    ): SwiftUIView {
        val itemViews = component.items.map { item ->
            val iconColor = item.color?.let { ModifierConverter.convertColor(it) } ?: SwiftUIColor.accentColor

            SwiftUIView.hStack(
                spacing = 12f,
                alignment = VerticalAlignment.Top,
                children = listOf(
                    // Timeline indicator
                    SwiftUIView.vStack(
                        spacing = 0f,
                        alignment = HorizontalAlignment.Center,
                        children = listOf(
                            SwiftUIView(
                                type = ViewType.Image,
                                properties = mapOf("systemName" to (item.icon ?: "circle.fill")),
                                modifiers = listOf(
                                    SwiftUIModifier.foregroundColor(iconColor),
                                    SwiftUIModifier.fontSize(16f)
                                )
                            ),
                            SwiftUIView(
                                type = ViewType.Custom("Rectangle"),
                                properties = emptyMap(),
                                modifiers = listOf(
                                    SwiftUIModifier.frame(SizeValue.Fixed(2f), SizeValue.Fixed(40f)),
                                    SwiftUIModifier.background(SwiftUIColor.separator)
                                )
                            )
                        )
                    ),
                    // Content
                    SwiftUIView.vStack(
                        spacing = 4f,
                        alignment = HorizontalAlignment.Leading,
                        children = listOfNotNull(
                            SwiftUIView.text(item.timestamp, modifiers = listOf(
                                SwiftUIModifier.font(FontStyle.Caption),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                            )),
                            SwiftUIView.text(item.title, modifiers = listOf(
                                SwiftUIModifier.font(FontStyle.Headline)
                            )),
                            item.description?.let {
                                SwiftUIView.text(it, modifiers = listOf(
                                    SwiftUIModifier.font(FontStyle.Body),
                                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                                ))
                            }
                        )
                    )
                )
            )
        }

        return SwiftUIView.vStack(
            spacing = 0f,
            alignment = HorizontalAlignment.Leading,
            children = itemViews,
            modifiers = ModifierConverter.convert(component.modifiers, theme),
            id = component.id
        )
    }
}

object TreeViewMapper {
    fun map(
        component: TreeView,
        theme: Theme?,
        childMapper: (Component) -> SwiftUIView
    ): SwiftUIView {
        fun mapNode(node: TreeNode, level: Int): SwiftUIView {
            val isExpanded = component.expandedIds.contains(node.id)
            val hasChildren = node.children.isNotEmpty()

            val children = mutableListOf<SwiftUIView>()

            // Node row
            val rowChildren = mutableListOf<SwiftUIView>()

            if (hasChildren) {
                rowChildren.add(SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to if (isExpanded) "chevron.down" else "chevron.right"),
                    modifiers = listOf(SwiftUIModifier.fontSize(12f))
                ))
            } else {
                rowChildren.add(SwiftUIView(
                    type = ViewType.Custom("Spacer"),
                    properties = emptyMap(),
                    modifiers = listOf(SwiftUIModifier.frame(SizeValue.Fixed(12f), null))
                ))
            }

            val nodeIcon = node.icon
            if (nodeIcon != null) {
                rowChildren.add(SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to nodeIcon),
                    modifiers = listOf(SwiftUIModifier.fontSize(16f))
                ))
            }

            rowChildren.add(SwiftUIView.text(node.label, modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))))

            children.add(SwiftUIView.hStack(
                spacing = 8f,
                alignment = VerticalAlignment.Center,
                children = rowChildren,
                modifiers = listOf(SwiftUIModifier.padding(4f, (level * 16).toFloat(), 4f, 4f))
            ))

            // Child nodes
            if (isExpanded && hasChildren) {
                node.children.forEach { child ->
                    children.add(mapNode(child, level + 1))
                }
            }

            return SwiftUIView.vStack(
                spacing = 0f,
                alignment = HorizontalAlignment.Leading,
                children = children
            )
        }

        val nodeViews = component.nodes.map { mapNode(it, 0) }

        return SwiftUIView.vStack(
            spacing = 0f,
            alignment = HorizontalAlignment.Leading,
            children = nodeViews,
            modifiers = ModifierConverter.convert(component.modifiers, theme),
            id = component.id
        )
    }
}

object CarouselMapper {
    fun map(
        component: Carousel,
        theme: Theme?,
        childMapper: (Component) -> SwiftUIView
    ): SwiftUIView {
        val slideViews = component.items.map { childMapper(it) }

        val properties = mutableMapOf<String, Any>(
            "currentIndex" to component.currentIndex,
            "autoPlay" to component.autoPlay,
            "interval" to component.interval,
            "showIndicators" to component.showIndicators,
            "showControls" to component.showControls
        )

        return SwiftUIView(
            type = ViewType.Custom("TabView"),
            properties = properties,
            children = slideViews,
            modifiers = ModifierConverter.convert(component.modifiers, theme),
            id = component.id
        )
    }
}

object PaperMapper {
    fun map(
        component: Paper,
        theme: Theme?,
        childMapper: (Component) -> SwiftUIView
    ): SwiftUIView {
        val childViews = component.children.map { childMapper(it) }

        val shadowRadius = when (component.elevation) {
            0 -> 0f
            1 -> 2f
            2 -> 4f
            3 -> 8f
            else -> (component.elevation * 2).toFloat()
        }

        return SwiftUIView.vStack(
            spacing = 0f,
            alignment = HorizontalAlignment.Leading,
            children = childViews,
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.systemBackground),
                SwiftUIModifier.cornerRadius(8f),
                SwiftUIModifier.shadow(color = SwiftUIColor.black.withOpacity(0.1f), radius = shadowRadius, x = 0f, y = shadowRadius / 2)
            ) + ModifierConverter.convert(component.modifiers, theme),
            id = component.id
        )
    }
}

object EmptyStateMapper {
    fun map(
        component: EmptyState,
        theme: Theme?,
        childMapper: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Icon
        component.icon?.let {
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to it),
                modifiers = listOf(
                    SwiftUIModifier.fontSize(48f),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                )
            ))
        }

        // Title
        children.add(SwiftUIView.text(
            component.title,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Title2),
                SwiftUIModifier.fontWeight(FontWeight.Bold)
            )
        ))

        // Description
        component.description?.let {
            children.add(SwiftUIView.text(
                it,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText),
                    SwiftUIModifier.multilineTextAlignment("center")
                )
            ))
        }

        // Action button
        component.action?.let {
            children.add(childMapper(it))
        }

        return SwiftUIView.vStack(
            spacing = 16f,
            alignment = HorizontalAlignment.Center,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(32f)) + ModifierConverter.convert(component.modifiers, theme),
            id = component.id
        )
    }
}

object DataGridMapper {
    fun map(
        component: DataGrid,
        theme: Theme?,
        childMapper: (Component) -> SwiftUIView
    ): SwiftUIView {
        val views = mutableListOf<SwiftUIView>()

        // Header row
        if (component.columns.isNotEmpty()) {
            val headerCells = component.columns.map { column ->
                val isSorted = component.sortBy == column.id
                val sortIcon = if (isSorted) {
                    if (component.sortOrder == SortOrder.Ascending) "arrow.up" else "arrow.down"
                } else null

                val headerContent = mutableListOf<SwiftUIView>(
                    SwiftUIView.text(
                        column.label,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Headline),
                            SwiftUIModifier.fontWeight(FontWeight.Semibold)
                        )
                    )
                )

                if (sortIcon != null && column.sortable) {
                    headerContent.add(SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to sortIcon),
                        modifiers = listOf(SwiftUIModifier.fontSize(12f))
                    ))
                }

                SwiftUIView.hStack(
                    spacing = 4f,
                    alignment = VerticalAlignment.Center,
                    children = headerContent,
                    modifiers = listOf(SwiftUIModifier.padding(8f))
                )
            }

            views.add(SwiftUIView.hStack(
                spacing = 0f,
                alignment = VerticalAlignment.Center,
                children = headerCells,
                modifiers = listOf(SwiftUIModifier.background(SwiftUIColor.systemGroupedBackground))
            ))
        }

        // Data rows
        component.rows.forEachIndexed { rowIndex, row ->
            val isSelected = component.selectable && component.selectedIds.contains(row.id)

            val rowCells = component.columns.map { column ->
                val cellValue = row.cells[column.id]?.toString() ?: ""
                SwiftUIView.text(
                    cellValue,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Body),
                        SwiftUIModifier.padding(8f)
                    )
                )
            }

            views.add(SwiftUIView.hStack(
                spacing = 0f,
                alignment = VerticalAlignment.Center,
                children = rowCells,
                modifiers = if (isSelected) {
                    listOf(SwiftUIModifier.background(SwiftUIColor.accentColor.withOpacity(0.1f)))
                } else emptyList()
            ))
        }

        // Pagination info
        val totalPages = (component.rows.size + component.pageSize - 1) / component.pageSize
        if (totalPages > 1) {
            views.add(SwiftUIView.hStack(
                spacing = 8f,
                alignment = VerticalAlignment.Center,
                children = listOf(
                    SwiftUIView.text("Page ${component.currentPage} of $totalPages", modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption)
                    ))
                ),
                modifiers = listOf(SwiftUIModifier.padding(8f))
            ))
        }

        return SwiftUIView.vStack(
            spacing = 0f,
            alignment = HorizontalAlignment.Leading,
            children = views,
            modifiers = ModifierConverter.convert(component.modifiers, theme),
            id = component.id
        )
    }
}
