import SwiftUI

/// Kanban column component for iOS
///
/// Displays a single kanban column with:
/// - Column title with color accent
/// - VStack of cards
/// - Card count / max cards indicator
/// - Drag-drop zones (iOS 16+)
/// - VoiceOver support
///
/// **iOS Version:** 16.0+
/// **Design:** Material Design 3 containers
///
/// ## Usage Example
/// ```swift
/// KanbanColumnView(
///     column: KanbanColumn(
///         id: "todo",
///         title: "To Do",
///         cards: cards,
///         maxCards: 10
///     )
/// )
/// ```
@available(iOS 16.0, *)
public struct KanbanColumnView: View {

    // MARK: - Properties

    /// Column data
    let column: KanbanColumn

    /// On card tap callback
    let onCardTap: ((KanbanCard) -> Void)?

    /// On card drop callback (card ID, target column ID)
    let onCardDrop: ((String, String) -> Void)?

    /// Allow drag and drop
    let allowDragDrop: Bool

    // MARK: - Initialization

    public init(
        column: KanbanColumn,
        allowDragDrop: Bool = true,
        onCardTap: ((KanbanCard) -> Void)? = nil,
        onCardDrop: ((String, String) -> Void)? = nil
    ) {
        self.column = column
        self.allowDragDrop = allowDragDrop
        self.onCardTap = onCardTap
        self.onCardDrop = onCardDrop
    }

    // MARK: - Body

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Column Header
            HStack {
                // Title with color accent
                HStack(spacing: 8) {
                    if let color = column.color {
                        Rectangle()
                            .fill(ChartHelpers.parseColor(color))
                            .frame(width: 4, height: 20)
                            .cornerRadius(2)
                    }

                    Text(column.title)
                        .font(.headline)
                        .fontWeight(.bold)
                }

                Spacer()

                // Card count badge
                HStack(spacing: 4) {
                    Text("\(column.cards.count)")
                        .font(.caption)
                        .fontWeight(.semibold)

                    if let maxCards = column.maxCards {
                        Text("/ \(maxCards)")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(cardCountBackgroundColor)
                .foregroundColor(cardCountTextColor)
                .cornerRadius(12)
            }

            // WIP limit warning
            if let maxCards = column.maxCards, column.cards.count >= maxCards {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.caption2)
                    Text("At capacity")
                        .font(.caption2)
                }
                .foregroundColor(.orange)
            }

            // Cards
            ScrollView {
                VStack(spacing: 8) {
                    ForEach(column.cards) { card in
                        KanbanCardView(
                            card: card,
                            onTap: {
                                onCardTap?(card)
                            }
                        )
                        .if(allowDragDrop) { view in
                            view.draggable(card.id)
                        }
                    }

                    // Empty state
                    if column.cards.isEmpty {
                        emptyStateView
                    }
                }
                .padding(.vertical, 4)
            }
            .if(allowDragDrop) { view in
                view.dropDestination(for: String.self) { items, location in
                    handleCardDrop(items)
                    return true
                }
            }
        }
        .padding(12)
        .frame(width: 280)
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(accessibilityDescription)
    }

    // MARK: - Subviews

    /// Empty state view
    private var emptyStateView: some View {
        VStack(spacing: 8) {
            Image(systemName: "square.dashed")
                .font(.largeTitle)
                .foregroundColor(.secondary)

            Text("No cards")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 120)
    }

    // MARK: - Helpers

    /// Card count badge background color
    private var cardCountBackgroundColor: Color {
        if let maxCards = column.maxCards {
            let utilization = Double(column.cards.count) / Double(maxCards)
            if utilization >= 1.0 {
                return Color.red.opacity(0.2)
            } else if utilization >= 0.8 {
                return Color.orange.opacity(0.2)
            }
        }
        return Color(.systemGray5)
    }

    /// Card count badge text color
    private var cardCountTextColor: Color {
        if let maxCards = column.maxCards {
            let utilization = Double(column.cards.count) / Double(maxCards)
            if utilization >= 1.0 {
                return .red
            } else if utilization >= 0.8 {
                return .orange
            }
        }
        return .primary
    }

    /// Handle card drop
    private func handleCardDrop(_ items: [String]) {
        guard let cardId = items.first else { return }
        onCardDrop?(cardId, column.id)
    }

    /// Accessibility description
    private var accessibilityDescription: String {
        var parts: [String] = ["Column: \(column.title)"]
        parts.append("\(column.cards.count) cards")

        if let maxCards = column.maxCards {
            parts.append("Maximum: \(maxCards)")
            if column.cards.count >= maxCards {
                parts.append("At capacity")
            }
        }

        return parts.joined(separator: ". ")
    }
}

// MARK: - View Extension

@available(iOS 16.0, *)
private extension View {
    /// Conditionally apply a transformation
    @ViewBuilder
    func `if`<Content: View>(_ condition: Bool, transform: (Self) -> Content) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}

// MARK: - Kanban Column Data Model

/// Kanban column data model
@available(iOS 16.0, *)
public struct KanbanColumn: Identifiable {
    public let id: String
    public let title: String
    public let cards: [KanbanCard]
    public let color: String?
    public let maxCards: Int?

    public init(
        id: String,
        title: String,
        cards: [KanbanCard] = [],
        color: String? = nil,
        maxCards: Int? = nil
    ) {
        self.id = id
        self.title = title
        self.cards = cards
        self.color = color
        self.maxCards = maxCards
    }

    /// Check if column is at capacity
    public var isAtCapacity: Bool {
        guard let maxCards = maxCards else { return false }
        return cards.count >= maxCards
    }

    /// Get cards by priority
    public func cards(withPriority priority: KanbanCard.Priority) -> [KanbanCard] {
        return cards.filter { $0.priority == priority }
    }
}
