import SwiftUI

/// Kanban board component for iOS
///
/// A kanban board for task management with drag-and-drop support.
/// Displays multiple columns horizontally with scrolling.
///
/// **iOS Version:** 16.0+
/// **Design:** Material Design 3 with HIG compliance
///
/// ## Features
/// - Horizontal scrolling board
/// - Multiple columns (swim lanes)
/// - Drag and drop between columns (iOS 16+)
/// - Priority color coding
/// - Tag chips
/// - Column capacity indicators
/// - VoiceOver support (100%)
/// - Smooth animations
///
/// ## Usage Example
/// ```swift
/// KanbanView(
///     title: "Sprint Board",
///     columns: [
///         KanbanColumn(
///             id: "todo",
///             title: "To Do",
///             cards: todoCards
///         ),
///         KanbanColumn(
///             id: "in-progress",
///             title: "In Progress",
///             cards: inProgressCards,
///             maxCards: 5
///         ),
///         KanbanColumn(
///             id: "done",
///             title: "Done",
///             cards: doneCards
///         )
///     ]
/// )
/// ```
///
/// ## Priority Colors
/// - **LOW:** Green (#4CAF50)
/// - **MEDIUM:** Blue (#2196F3)
/// - **HIGH:** Orange (#FF9800)
/// - **URGENT:** Red (#F44336)
@available(iOS 16.0, *)
public struct KanbanView: View {

    // MARK: - Properties

    /// Board title (optional)
    let title: String?

    /// List of kanban columns
    let columns: [KanbanColumn]

    /// Allow drag and drop
    let allowDragDrop: Bool

    /// Content description for accessibility
    let contentDescription: String?

    /// Callback when a card is tapped
    let onCardTap: ((String, String) -> Void)?

    /// Callback when a card is moved between columns
    let onCardMove: ((String, String, String) -> Void)?

    // MARK: - State

    @State private var animateIn: Bool = false

    // MARK: - Initialization

    public init(
        title: String? = nil,
        columns: [KanbanColumn],
        allowDragDrop: Bool = true,
        contentDescription: String? = nil,
        onCardTap: ((String, String) -> Void)? = nil,
        onCardMove: ((String, String, String) -> Void)? = nil
    ) {
        self.title = title
        self.columns = columns
        self.allowDragDrop = allowDragDrop
        self.contentDescription = contentDescription
        self.onCardTap = onCardTap
        self.onCardMove = onCardMove
    }

    // MARK: - Body

    public var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Board title
            if let title = title {
                Text(title)
                    .font(.title2)
                    .fontWeight(.bold)
                    .padding(.horizontal, 16)
            }

            // Kanban board (horizontal scrolling columns)
            if columns.isEmpty {
                emptyStateView
            } else {
                ScrollView(.horizontal, showsIndicators: true) {
                    HStack(spacing: 16) {
                        ForEach(columns) { column in
                            KanbanColumnView(
                                column: column,
                                allowDragDrop: allowDragDrop,
                                onCardTap: { card in
                                    onCardTap?(column.id, card.id)
                                },
                                onCardDrop: { cardId, toColumnId in
                                    handleCardMove(cardId: cardId, toColumnId: toColumnId)
                                }
                            )
                            .transition(.move(edge: .trailing).combined(with: .opacity))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }
            }
        }
        .opacity(animateIn ? 1.0 : 0.0)
        .onAppear {
            withAnimation(.easeOut(duration: 0.3)) {
                animateIn = true
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(accessibilityDescription)
    }

    // MARK: - Subviews

    /// Empty state view
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: "square.grid.3x3.square")
                .font(.system(size: 60))
                .foregroundColor(.secondary)

            Text("No columns")
                .font(.title3)
                .foregroundColor(.secondary)

            Text("Add columns to create your kanban board")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(40)
    }

    // MARK: - Helpers

    /// Handle card move between columns
    private func handleCardMove(cardId: String, toColumnId: String) {
        // Find source column
        guard let sourceColumn = findColumnContainingCard(cardId: cardId) else {
            return
        }

        // Don't move if same column
        guard sourceColumn.id != toColumnId else {
            return
        }

        // Check capacity
        if let targetColumn = columns.first(where: { $0.id == toColumnId }),
           targetColumn.isAtCapacity {
            // Cannot move - target column is at capacity
            return
        }

        // Notify callback
        onCardMove?(cardId, sourceColumn.id, toColumnId)
    }

    /// Find column containing a card
    private func findColumnContainingCard(cardId: String) -> KanbanColumn? {
        for column in columns {
            if column.cards.contains(where: { $0.id == cardId }) {
                return column
            }
        }
        return nil
    }

    /// Accessibility description
    private var accessibilityDescription: String {
        if let contentDescription = contentDescription {
            return contentDescription
        }

        var parts: [String] = []

        if let title = title {
            parts.append(title)
        }

        let totalCards = columns.reduce(0) { $0 + $1.cards.count }
        parts.append("Kanban board with \(columns.count) columns and \(totalCards) total cards")

        let columnSummary = columns.map { "\($0.title): \($0.cards.count) cards" }.joined(separator: ", ")
        parts.append(columnSummary)

        return parts.joined(separator: ". ")
    }
}

// MARK: - Preview Helpers

@available(iOS 16.0, *)
public extension KanbanView {
    /// Create a standard kanban board with To Do, In Progress, Done columns
    static func standard(
        title: String? = nil,
        todoCards: [KanbanCard] = [],
        inProgressCards: [KanbanCard] = [],
        doneCards: [KanbanCard] = []
    ) -> KanbanView {
        KanbanView(
            title: title,
            columns: [
                KanbanColumn(
                    id: "todo",
                    title: "To Do",
                    cards: todoCards,
                    color: "#2196F3"
                ),
                KanbanColumn(
                    id: "in-progress",
                    title: "In Progress",
                    cards: inProgressCards,
                    color: "#FF9800",
                    maxCards: 5
                ),
                KanbanColumn(
                    id: "done",
                    title: "Done",
                    cards: doneCards,
                    color: "#4CAF50"
                )
            ]
        )
    }

    /// Create a scrum board with Backlog, Sprint, In Progress, Review, Done columns
    static func scrum(
        title: String = "Sprint Board",
        backlogCards: [KanbanCard] = [],
        sprintCards: [KanbanCard] = [],
        inProgressCards: [KanbanCard] = [],
        reviewCards: [KanbanCard] = [],
        doneCards: [KanbanCard] = []
    ) -> KanbanView {
        KanbanView(
            title: title,
            columns: [
                KanbanColumn(
                    id: "backlog",
                    title: "Backlog",
                    cards: backlogCards,
                    color: "#9E9E9E"
                ),
                KanbanColumn(
                    id: "sprint",
                    title: "Sprint",
                    cards: sprintCards,
                    color: "#2196F3"
                ),
                KanbanColumn(
                    id: "in-progress",
                    title: "In Progress",
                    cards: inProgressCards,
                    color: "#FF9800",
                    maxCards: 3
                ),
                KanbanColumn(
                    id: "review",
                    title: "Review",
                    cards: reviewCards,
                    color: "#9C27B0"
                ),
                KanbanColumn(
                    id: "done",
                    title: "Done",
                    cards: doneCards,
                    color: "#4CAF50"
                )
            ]
        )
    }

    /// Sample data for testing and previews
    static func sampleBoard() -> KanbanView {
        let todoCards = [
            KanbanCard(
                id: "1",
                title: "Design new landing page",
                description: "Create mockups and user flows",
                priority: .high,
                tags: ["design", "ui"],
                assignee: "Alice"
            ),
            KanbanCard(
                id: "2",
                title: "Fix login bug",
                description: "Users can't log in with email",
                priority: .urgent,
                tags: ["bug", "backend"],
                assignee: "Bob"
            )
        ]

        let inProgressCards = [
            KanbanCard(
                id: "3",
                title: "Implement search feature",
                priority: .medium,
                tags: ["feature"],
                assignee: "Carol"
            )
        ]

        let doneCards = [
            KanbanCard(
                id: "4",
                title: "Update dependencies",
                priority: .low,
                tags: ["maintenance"]
            )
        ]

        return .standard(
            title: "Development Board",
            todoCards: todoCards,
            inProgressCards: inProgressCards,
            doneCards: doneCards
        )
    }
}

// MARK: - SwiftUI Previews

@available(iOS 16.0, *)
struct KanbanView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            // Standard board
            KanbanView.sampleBoard()
                .previewDisplayName("Standard Board")

            // Empty board
            KanbanView(
                title: "Empty Board",
                columns: []
            )
            .previewDisplayName("Empty Board")

            // Scrum board
            KanbanView.scrum(
                sprintCards: [
                    KanbanCard(
                        id: "s1",
                        title: "Sprint task",
                        priority: .high
                    )
                ]
            )
            .previewDisplayName("Scrum Board")
        }
    }
}
