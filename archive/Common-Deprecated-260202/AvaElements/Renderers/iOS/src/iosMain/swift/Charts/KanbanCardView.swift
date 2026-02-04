import SwiftUI

/// Kanban card component for iOS
///
/// Displays a single kanban card with:
/// - Title and description
/// - Priority indicator (colored left border)
/// - Tags as chips
/// - Assignee avatar/initials
/// - VoiceOver support
///
/// **iOS Version:** 16.0+
/// **Design:** Material Design 3 cards
///
/// ## Usage Example
/// ```swift
/// KanbanCardView(
///     card: KanbanCard(
///         id: "1",
///         title: "Implement feature",
///         priority: .high,
///         tags: ["backend", "urgent"]
///     )
/// )
/// ```
@available(iOS 16.0, *)
public struct KanbanCardView: View {

    // MARK: - Properties

    /// Card data
    let card: KanbanCard

    /// On card tap callback
    let onTap: (() -> Void)?

    /// Whether this card is being dragged
    @State private var isDragging: Bool = false

    // MARK: - Initialization

    public init(
        card: KanbanCard,
        onTap: (() -> Void)? = nil
    ) {
        self.card = card
        self.onTap = onTap
    }

    // MARK: - Body

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Title with priority indicator
            HStack(alignment: .top, spacing: 8) {
                Text(card.title)
                    .font(.body)
                    .fontWeight(.semibold)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Circle()
                    .fill(priorityColor)
                    .frame(width: 8, height: 8)
                    .padding(.top, 6)
            }

            // Description
            if let description = card.description, !description.isEmpty {
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }

            // Tags
            if !card.tags.isEmpty {
                FlowLayout(spacing: 4) {
                    ForEach(card.tags.prefix(3), id: \.self) { tag in
                        TagChip(text: tag)
                    }
                }
            }

            // Assignee
            if let assignee = card.assignee, !assignee.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "person.circle.fill")
                        .font(.caption2)
                        .foregroundColor(.secondary)

                    Text(assignee)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(12)
        .background(Color(.systemBackground))
        .cornerRadius(8)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
        .opacity(isDragging ? 0.5 : 1.0)
        .onTapGesture {
            onTap?()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityDescription)
        .accessibilityHint("Double tap to view card details")
        .accessibilityAddTraits(.isButton)
    }

    // MARK: - Helpers

    /// Get priority color
    private var priorityColor: Color {
        switch card.priority {
        case .low:
            return ChartHelpers.parseColor("#4CAF50") // Green
        case .medium:
            return ChartHelpers.parseColor("#2196F3") // Blue
        case .high:
            return ChartHelpers.parseColor("#FF9800") // Orange
        case .urgent:
            return ChartHelpers.parseColor("#F44336") // Red
        }
    }

    /// Accessibility description
    private var accessibilityDescription: String {
        var parts: [String] = ["Card: \(card.title)"]

        if let description = card.description, !description.isEmpty {
            parts.append("Description: \(description)")
        }

        parts.append("Priority: \(card.priority.rawValue)")

        if !card.tags.isEmpty {
            parts.append("Tags: \(card.tags.joined(separator: ", "))")
        }

        if let assignee = card.assignee, !assignee.isEmpty {
            parts.append("Assigned to: \(assignee)")
        }

        return parts.joined(separator: ". ")
    }
}

// MARK: - Tag Chip Component

/// Simple tag chip for displaying tags
@available(iOS 16.0, *)
private struct TagChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption2)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color(.systemGray5))
            .cornerRadius(4)
    }
}

// MARK: - Flow Layout

/// Flow layout for tags (wraps to next line if needed)
@available(iOS 16.0, *)
private struct FlowLayout: Layout {
    var spacing: CGFloat = 4

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(
            in: proposal.width ?? 0,
            subviews: subviews,
            spacing: spacing
        )
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(
            in: bounds.width,
            subviews: subviews,
            spacing: spacing
        )
        for (index, subview) in subviews.enumerated() {
            subview.place(at: CGPoint(x: bounds.minX + result.positions[index].x, y: bounds.minY + result.positions[index].y), proposal: .unspecified)
        }
    }

    struct FlowResult {
        var size: CGSize = .zero
        var positions: [CGPoint] = []

        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var currentX: CGFloat = 0
            var currentY: CGFloat = 0
            var lineHeight: CGFloat = 0

            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)

                if currentX + size.width > maxWidth && currentX > 0 {
                    // Move to next line
                    currentX = 0
                    currentY += lineHeight + spacing
                    lineHeight = 0
                }

                positions.append(CGPoint(x: currentX, y: currentY))
                lineHeight = max(lineHeight, size.height)
                currentX += size.width + spacing
            }

            self.size = CGSize(
                width: maxWidth,
                height: currentY + lineHeight
            )
        }
    }
}

// MARK: - Kanban Card Data Model

/// Kanban card data model
@available(iOS 16.0, *)
public struct KanbanCard: Identifiable {
    public let id: String
    public let title: String
    public let description: String?
    public let priority: Priority
    public let tags: [String]
    public let assignee: String?

    public init(
        id: String,
        title: String,
        description: String? = nil,
        priority: Priority = .medium,
        tags: [String] = [],
        assignee: String? = nil
    ) {
        self.id = id
        self.title = title
        self.description = description
        self.priority = priority
        self.tags = tags
        self.assignee = assignee
    }

    /// Priority levels
    public enum Priority: String {
        case low = "Low"
        case medium = "Medium"
        case high = "High"
        case urgent = "Urgent"
    }
}
