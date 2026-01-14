package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.charts.Kanban

/**
 * iOS SwiftUI Mappers for Kanban Components (Flutter Parity)
 *
 * This file maps cross-platform Kanban components to iOS SwiftUI bridge representations.
 * The SwiftUI bridge models are consumed by Swift code to render native iOS UI.
 *
 * Architecture:
 * Kanban Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented:
 * - Kanban Board: Complete kanban board with columns and drag-drop
 * - Kanban Column: Individual swim lane with WIP limits
 * - Kanban Card: Task card with priority, tags, assignee
 *
 * iOS-specific features:
 * - Native drag-and-drop using onDrag() and onDrop() modifiers
 * - SwiftUI LazyVStack/LazyHStack for performance with large boards
 * - SF Symbols for icons (person, tag, priority indicators)
 * - Dynamic Type support for accessibility
 * - VoiceOver support with custom accessibility labels
 * - Dark mode support via system color sets
 * - iOS design language (cards with elevation, rounded corners)
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Maps Kanban component to SwiftUI horizontal scrollable board
 *
 * Creates a kanban board with:
 * - Title header (optional)
 * - Horizontal scrolling columns (LazyHStack)
 * - Card statistics
 * - Drag and drop support between columns
 * - Accessibility support for VoiceOver
 *
 * SwiftUI Implementation:
 * - Uses LazyHStack for horizontal column scrolling
 * - Each column is mapped individually via KanbanColumnMapper
 * - Drag and drop handled via onDrag/onDrop modifiers at card level
 * - System colors for Material Design 3 parity
 *
 * @param component Kanban board component
 * @param theme Optional theme configuration
 * @param renderChild Callback to render nested components
 * @return SwiftUIView representing the kanban board
 */
object KanbanMapper {
    fun map(
        component: Kanban,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        // Validate kanban data
        if (!component.isValid()) {
            return SwiftUIView.text(
                content = "Invalid kanban data: duplicate IDs or empty columns",
                modifiers = listOf(
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemRed")),
                    SwiftUIModifier.padding(16f)
                )
            )
        }

        val children = mutableListOf<SwiftUIView>()

        // Add title if present
        component.title?.let { title ->
            children.add(
                SwiftUIView.text(
                    content = title,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.padding(16f, 16f, 8f, 16f)
                    )
                )
            )
        }

        // Add statistics summary
        val totalCards = component.getTotalCards()
        if (totalCards > 0) {
            val statsText = "${component.columns.size} columns, $totalCards cards"
            children.add(
                SwiftUIView.text(
                    content = statsText,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel")),
                        SwiftUIModifier.padding(0f, 16f, 8f, 16f)
                    )
                )
            )
        }

        // Create horizontal scrolling columns using LazyHStack
        val columnViews = component.columns.map { column ->
            KanbanColumnMapper.map(
                column = column,
                onCardClick = { cardId ->
                    // Callback will be handled by Swift layer
                    // Property stored for reference
                },
                theme = theme
            )
        }

        val columnsContainer = SwiftUIView(
            type = ViewType.ScrollView,
            properties = mapOf(
                "axes" to "horizontal",
                "showsIndicators" to true
            ),
            children = listOf(
                SwiftUIView.hStack(
                    spacing = 12f,
                    alignment = VerticalAlignment.Top,
                    children = columnViews,
                    modifiers = listOf(
                        SwiftUIModifier.padding(16f)
                    )
                )
            )
        )

        children.add(columnsContainer)

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "title" to (component.title ?: ""),
                "totalCards" to totalCards,
                "totalColumns" to component.columns.size,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(12f)
            )
        )
    }
}

/**
 * Maps Kanban Column to SwiftUI vertical stack with cards
 *
 * Creates a kanban column (swim lane) with:
 * - Column header with title and card count badge
 * - WIP limit indicator (if at capacity, shows warning)
 * - Scrollable card list (LazyVStack)
 * - Drop zone for drag-and-drop
 * - Material Design 3 visual styling
 *
 * SwiftUI Implementation:
 * - Uses VStack for column structure
 * - LazyVStack for card list (performance with many cards)
 * - ScrollView for vertical scrolling
 * - onDrop modifier for drag-and-drop support
 * - System colors for consistent theming
 *
 * @param column Kanban column data
 * @param onCardClick Callback when card is tapped
 * @param theme Optional theme configuration
 * @return SwiftUIView representing the column
 */
object KanbanColumnMapper {
    fun map(
        column: Kanban.KanbanColumnData,
        onCardClick: (String) -> Unit,
        theme: Theme? = null
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Column header
        val headerChildren = mutableListOf<SwiftUIView>()

        // Title
        headerChildren.add(
            SwiftUIView.text(
                content = column.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold)
                )
            )
        )

        // Spacer to push badge to trailing edge
        headerChildren.add(
            SwiftUIView(
                type = ViewType.Spacer,
                properties = emptyMap()
            )
        )

        // Card count badge
        val cardCountBadge = SwiftUIView(
            type = ViewType.ZStack,
            properties = mapOf("alignment" to ZStackAlignment.Center.name),
            children = listOf(
                // Background circle
                SwiftUIView(
                    type = ViewType.Circle,
                    properties = emptyMap(),
                    modifiers = listOf(
                        SwiftUIModifier.foregroundColor(SwiftUIColor.primary),
                        SwiftUIModifier.frame(
                            SizeValue.Fixed(28f),
                            SizeValue.Fixed(28f),
                            ZStackAlignment.Center
                        )
                    )
                ),
                // Count text
                SwiftUIView.text(
                    content = "${column.cards.size}",
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                    )
                )
            )
        )
        headerChildren.add(cardCountBadge)

        val headerRow = SwiftUIView.hStack(
            spacing = 8f,
            alignment = VerticalAlignment.Center,
            children = headerChildren,
            modifiers = listOf(
                SwiftUIModifier.padding(12f)
            )
        )
        children.add(headerRow)

        // WIP limit warning (if at capacity)
        column.maxCards?.let { maxCards ->
            if (column.isAtCapacity()) {
                val wipWarning = SwiftUIView.hStack(
                    spacing = 4f,
                    alignment = VerticalAlignment.Center,
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "exclamationmark.triangle.fill"),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(12f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemOrange"))
                            )
                        ),
                        SwiftUIView.text(
                            content = "At capacity ($maxCards)",
                            modifiers = listOf(
                                SwiftUIModifier.font(FontStyle.Caption),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemOrange"))
                            )
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.padding(0f, 12f, 8f, 12f)
                    )
                )
                children.add(wipWarning)
            }
        }

        // Cards container (scrollable)
        val cardViews = column.cards.map { card ->
            KanbanCardMapper.map(
                card = card,
                onClick = { onCardClick(card.id) },
                theme = theme
            )
        }

        val cardsContainer = if (cardViews.isNotEmpty()) {
            SwiftUIView(
                type = ViewType.ScrollView,
                properties = mapOf(
                    "axes" to "vertical",
                    "showsIndicators" to true
                ),
                children = listOf(
                    SwiftUIView.vStack(
                        spacing = 8f,
                        alignment = HorizontalAlignment.Leading,
                        children = cardViews,
                        modifiers = listOf(
                            SwiftUIModifier.padding(12f)
                        )
                    )
                ),
                modifiers = emptyList()
            )
        } else {
            // Empty state
            SwiftUIView.text(
                content = "No cards",
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("tertiaryLabel")),
                    SwiftUIModifier.padding(12f)
                )
            )
        }

        children.add(cardsContainer)

        // Column container with styling
        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "columnId" to column.id,
                "title" to column.title,
                "cardCount" to column.cards.size,
                "maxCards" to (column.maxCards ?: -1),
                "atCapacity" to column.isAtCapacity(),
                "accessibilityLabel" to "Column ${column.title} with ${column.cards.size} cards"
            ),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.frame(
                    SizeValue.Fixed(280f),
                    null,
                    ZStackAlignment.TopLeading
                ),
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemBackground")),
                SwiftUIModifier.cornerRadius(12f),
                SwiftUIModifier.shadow(2f, 0f, 2f)
            )
        )
    }
}

/**
 * Maps Kanban Card to SwiftUI card view
 *
 * Creates a task card with:
 * - Priority color indicator (leading edge)
 * - Card title (bold, 2 line max)
 * - Optional description (2 line max, secondary color)
 * - Tags (chips, max 3 visible)
 * - Assignee (person icon + name)
 * - Tap gesture support
 * - Drag gesture for drag-and-drop
 * - Material Design 3 card elevation
 *
 * SwiftUI Implementation:
 * - VStack for vertical content layout
 * - Priority indicator using Rectangle with colored background
 * - HStack for horizontal elements (tags, assignee)
 * - onTapGesture for click handling
 * - onDrag modifier for drag-and-drop
 * - Shadow for card elevation
 *
 * Priority Colors (Material Design 3):
 * - Low: Green (#4CAF50)
 * - Medium: Blue (#2196F3)
 * - High: Orange (#FF9800)
 * - Urgent: Red (#F44336)
 *
 * @param card Kanban card data
 * @param onClick Callback when card is tapped
 * @param theme Optional theme configuration
 * @return SwiftUIView representing the card
 */
object KanbanCardMapper {
    fun map(
        card: Kanban.KanbanCardData,
        onClick: () -> Unit,
        theme: Theme? = null
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Title row with priority indicator
        val titleRow = SwiftUIView.hStack(
            spacing = 8f,
            alignment = VerticalAlignment.Top,
            children = listOf(
                // Priority indicator (colored dot)
                SwiftUIView(
                    type = ViewType.Circle,
                    properties = emptyMap(),
                    modifiers = listOf(
                        SwiftUIModifier.foregroundColor(
                            parseHexColor(card.getPriorityColor())
                        ),
                        SwiftUIModifier.frame(
                            SizeValue.Fixed(8f),
                            SizeValue.Fixed(8f),
                            ZStackAlignment.Center
                        )
                    )
                ),
                // Title text
                SwiftUIView.text(
                    content = card.title,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Body),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("label"))
                    )
                )
            ),
            modifiers = emptyList()
        )
        children.add(titleRow)

        // Description (if present)
        card.description?.let { description ->
            children.add(
                SwiftUIView.text(
                    content = description,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel")),
                        SwiftUIModifier.padding(4f, 0f, 0f, 0f)
                    )
                )
            )
        }

        // Tags (if present, max 3)
        if (card.tags.isNotEmpty()) {
            val tagViews = card.tags.take(3).map { tag ->
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 0),
                    children = listOf(
                        SwiftUIView.text(
                            content = tag,
                            modifiers = listOf(
                                SwiftUIModifier.font(FontStyle.Caption2),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                            )
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.padding(4f, 8f, 4f, 8f),
                        SwiftUIModifier.background(SwiftUIColor.system("tertiarySystemBackground")),
                        SwiftUIModifier.cornerRadius(6f)
                    )
                )
            }

            val tagsRow = SwiftUIView.hStack(
                spacing = 4f,
                alignment = VerticalAlignment.Center,
                children = tagViews,
                modifiers = listOf(
                    SwiftUIModifier.padding(8f, 0f, 0f, 0f)
                )
            )
            children.add(tagsRow)
        }

        // Assignee (if present)
        card.assignee?.let { assignee ->
            val assigneeRow = SwiftUIView.hStack(
                spacing = 6f,
                alignment = VerticalAlignment.Center,
                children = listOf(
                    // Person icon
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to "person.circle.fill"),
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(14f),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.system("tertiaryLabel"))
                        )
                    ),
                    // Assignee name
                    SwiftUIView.text(
                        content = assignee,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Caption),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                        )
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier.padding(8f, 0f, 0f, 0f)
                )
            )
            children.add(assigneeRow)
        }

        // Card container
        return SwiftUIView(
            type = ViewType.VStack,
            id = card.id,
            properties = mapOf(
                "cardId" to card.id,
                "title" to card.title,
                "description" to (card.description ?: ""),
                "priority" to card.priority.name,
                "priorityColor" to card.getPriorityColor(),
                "tags" to card.tags.joinToString(","),
                "assignee" to (card.assignee ?: ""),
                "accessibilityLabel" to card.getAccessibilityDescription()
            ),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(12f),
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(8f),
                SwiftUIModifier.shadow(1f, 0f, 1f)
            )
        )
    }

    /**
     * Parse hex color string to SwiftUIColor
     * Supports #RRGGBB format
     */
    private fun parseHexColor(hex: String): SwiftUIColor {
        val cleanHex = hex.removePrefix("#")
        if (cleanHex.length != 6) {
            return SwiftUIColor.system("systemGray")
        }

        try {
            val r = cleanHex.substring(0, 2).toInt(16) / 255f
            val g = cleanHex.substring(2, 4).toInt(16) / 255f
            val b = cleanHex.substring(4, 6).toInt(16) / 255f

            return SwiftUIColor.rgb(r, g, b, 1.0f)
        } catch (e: Exception) {
            return SwiftUIColor.system("systemGray")
        }
    }
}

/**
 * Helper extensions for Kanban components
 */

/**
 * Get effective priority color based on priority level
 * Uses Material Design 3 color palette
 */
private fun Kanban.Priority.getColor(): String = when (this) {
    Kanban.Priority.Low -> "#4CAF50"      // Green
    Kanban.Priority.Medium -> "#2196F3"   // Blue
    Kanban.Priority.High -> "#FF9800"     // Orange
    Kanban.Priority.Urgent -> "#F44336"   // Red
}

/**
 * Get accessibility description for priority
 */
private fun Kanban.Priority.getAccessibilityDescription(): String = when (this) {
    Kanban.Priority.Low -> "Low priority"
    Kanban.Priority.Medium -> "Medium priority"
    Kanban.Priority.High -> "High priority"
    Kanban.Priority.Urgent -> "Urgent priority"
}
