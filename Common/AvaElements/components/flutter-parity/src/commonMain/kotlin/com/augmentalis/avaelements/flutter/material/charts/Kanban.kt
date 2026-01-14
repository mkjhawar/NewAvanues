package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Kanban component - Flutter Material parity
 *
 * A kanban board for task management with drag-and-drop support.
 *
 * **Flutter Equivalent:** Custom kanban implementations
 * **Material Design 3:** Custom component with Material theming
 *
 * ## Features
 * - Multiple columns (swim lanes)
 * - Drag-and-drop cards between columns
 * - Card priority indicators
 * - Tags and assignees
 * - Column limits (WIP limits)
 * - Interactive callbacks
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * Kanban(
 *     title = "Sprint Board",
 *     columns = listOf(
 *         Kanban.KanbanColumnData(
 *             id = "todo",
 *             title = "To Do",
 *             cards = listOf(
 *                 Kanban.KanbanCardData(
 *                     id = "card1",
 *                     title = "Implement feature",
 *                     priority = Kanban.Priority.High
 *                 )
 *             )
 *         )
 *     )
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Optional board title
 * @property columns List of kanban columns
 * @property onCardClick Callback when a card is clicked
 * @property onCardMove Callback when a card is moved
 * @property contentDescription Accessibility description
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Kanban(
    override val type: String = "Kanban",
    override val id: String? = null,
    val title: String? = null,
    val columns: List<KanbanColumnData> = emptyList(),
    val contentDescription: String? = null,
    @Transient
    val onCardClick: ((columnId: String, cardId: String) -> Unit)? = null,
    @Transient
    val onCardMove: ((cardId: String, fromColumnId: String, toColumnId: String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Priority levels for kanban cards
     */
    enum class Priority {
        Low,
        Medium,
        High,
        Urgent
    }

    /**
     * Kanban column data
     *
     * @property id Unique column identifier
     * @property title Column title
     * @property cards List of cards in this column
     * @property maxCards Optional WIP limit
     * @property color Optional column header color
     */
    data class KanbanColumnData(
        val id: String,
        val title: String,
        val cards: List<KanbanCardData> = emptyList(),
        val maxCards: Int? = null,
        val color: String? = null
    ) {
        /**
         * Check if column is at capacity
         */
        fun isAtCapacity(): Boolean {
            return maxCards != null && cards.size >= maxCards
        }

        /**
         * Get card count
         */
        fun getCardCount(): Int = cards.size

        /**
         * Get cards by priority
         */
        fun getCardsByPriority(priority: Priority): List<KanbanCardData> {
            return cards.filter { it.priority == priority }
        }
    }

    /**
     * Kanban card data
     *
     * @property id Unique card identifier
     * @property title Card title
     * @property description Optional card description
     * @property tags List of tags
     * @property assignee Optional assignee name
     * @property priority Card priority level
     * @property color Optional card color
     */
    data class KanbanCardData(
        val id: String,
        val title: String,
        val description: String? = null,
        val tags: List<String> = emptyList(),
        val assignee: String? = null,
        val priority: Priority = Priority.Medium,
        val color: String? = null
    ) {
        /**
         * Get priority color
         */
        fun getPriorityColor(): String {
            return when (priority) {
                Priority.Low -> "#4CAF50"      // Green
                Priority.Medium -> "#2196F3"   // Blue
                Priority.High -> "#FF9800"     // Orange
                Priority.Urgent -> "#F44336"   // Red
            }
        }

        /**
         * Get accessibility description
         */
        fun getAccessibilityDescription(): String {
            val titlePart = "Card: $title"
            val descPart = description?.let { ". Description: $it" } ?: ""
            val priorityPart = ". Priority: ${priority.name}"
            val assigneePart = assignee?.let { ". Assigned to: $it" } ?: ""
            val tagsPart = if (tags.isNotEmpty()) {
                ". Tags: ${tags.joinToString(", ")}"
            } else ""

            return "$titlePart$descPart$priorityPart$assigneePart$tagsPart"
        }
    }

    /**
     * Get total card count across all columns
     */
    fun getTotalCards(): Int {
        return columns.sumOf { it.cards.size }
    }

    /**
     * Get column by ID
     */
    fun getColumn(columnId: String): KanbanColumnData? {
        return columns.find { it.id == columnId }
    }

    /**
     * Get card by ID
     */
    fun getCard(cardId: String): Pair<KanbanColumnData, KanbanCardData>? {
        columns.forEach { column ->
            column.cards.find { it.id == cardId }?.let { card ->
                return column to card
            }
        }
        return null
    }

    /**
     * Get accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        val titlePart = title?.let { "$it. " } ?: ""
        val columnCount = columns.size
        val cardCount = getTotalCards()

        val columnSummary = columns.joinToString(", ") { column ->
            "${column.title}: ${column.cards.size} cards"
        }

        return "${titlePart}Kanban board with $columnCount columns and $cardCount total cards. $columnSummary"
    }

    /**
     * Validate kanban data
     */
    fun isValid(): Boolean {
        // Check unique column IDs
        val columnIds = columns.map { it.id }
        if (columnIds.size != columnIds.distinct().size) return false

        // Check unique card IDs
        val cardIds = columns.flatMap { it.cards.map { card -> card.id } }
        if (cardIds.size != cardIds.distinct().size) return false

        return columns.isNotEmpty()
    }

    companion object {
        /**
         * Create a simple kanban board with standard columns
         */
        fun standard(
            title: String? = null,
            todoCards: List<KanbanCardData> = emptyList(),
            inProgressCards: List<KanbanCardData> = emptyList(),
            doneCards: List<KanbanCardData> = emptyList()
        ) = Kanban(
            title = title,
            columns = listOf(
                KanbanColumnData(
                    id = "todo",
                    title = "To Do",
                    cards = todoCards
                ),
                KanbanColumnData(
                    id = "in-progress",
                    title = "In Progress",
                    cards = inProgressCards,
                    maxCards = 5
                ),
                KanbanColumnData(
                    id = "done",
                    title = "Done",
                    cards = doneCards
                )
            )
        )

        /**
         * Create a scrum board
         */
        fun scrum(
            title: String = "Sprint Board",
            backlogCards: List<KanbanCardData> = emptyList(),
            sprintCards: List<KanbanCardData> = emptyList(),
            inProgressCards: List<KanbanCardData> = emptyList(),
            reviewCards: List<KanbanCardData> = emptyList(),
            doneCards: List<KanbanCardData> = emptyList()
        ) = Kanban(
            title = title,
            columns = listOf(
                KanbanColumnData(id = "backlog", title = "Backlog", cards = backlogCards),
                KanbanColumnData(id = "sprint", title = "Sprint", cards = sprintCards),
                KanbanColumnData(id = "in-progress", title = "In Progress", cards = inProgressCards, maxCards = 3),
                KanbanColumnData(id = "review", title = "Review", cards = reviewCards),
                KanbanColumnData(id = "done", title = "Done", cards = doneCards)
            )
        )
    }
}
