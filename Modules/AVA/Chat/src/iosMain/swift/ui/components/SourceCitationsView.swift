// filename: Universal/AVA/Features/Chat/src/iosMain/swift/ui/components/SourceCitationsView.swift
// created: 2025-11-22
// author: iOS RAG Chat Integration Specialist (Agent 1)
// © Augmentalis Inc, Intelligent Devices LLC

import SwiftUI

/// Source citations section component for RAG-enhanced messages.
///
/// iOS SwiftUI equivalent of Android's SourceCitationsSection composable.
///
/// Displays document sources as collapsible view with:
/// - Document title
/// - Page number (if available)
/// - Similarity percentage
///
/// Design specifications:
/// - Compact header with toggle button when collapsed
/// - Expandable flow layout of chips when expanded
/// - Material 3-inspired design for iOS
/// - Leading icon showing document symbol
/// - Smooth SwiftUI animations
struct SourceCitationsView: View {

    // MARK: - Properties

    let citations: [SourceCitation]
    @State private var isExpanded: Bool

    // MARK: - Initialization

    init(citations: [SourceCitation], isExpanded: Bool = true) {
        self.citations = citations
        self._isExpanded = State(initialValue: isExpanded)
    }

    // MARK: - Body

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Collapsible header
            headerView
                .contentShape(Rectangle())
                .onTapGesture {
                    withAnimation(.easeInOut(duration: 0.3)) {
                        isExpanded.toggle()
                    }
                }

            // Expanded content with citation chips
            if isExpanded {
                citationsContent
                    .transition(.asymmetric(
                        insertion: .opacity.combined(with: .move(edge: .top)),
                        removal: .opacity
                    ))
            }
        }
        .padding(.vertical, 8)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Source citations section with \(citations.count) sources")
    }

    // MARK: - Subviews

    private var headerView: some View {
        HStack(spacing: 12) {
            // Document icon
            Image(systemName: "doc.text.fill")
                .font(.system(size: 16))
                .foregroundColor(.accentColor)

            // Title
            Text("Sources (\(citations.count))")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(.secondary)

            Spacer()

            // Expand/collapse icon
            Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(.accentColor)
                .animation(.easeInOut(duration: 0.3), value: isExpanded)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.systemGray6).opacity(0.5))
        )
    }

    private var citationsContent: some View {
        FlowLayout(spacing: 8) {
            ForEach(citations) { citation in
                CitationChip(citation: citation)
            }
        }
        .padding(.top, 12)
        .padding(.horizontal, 4)
    }
}

// MARK: - Citation Chip

/// Individual citation chip component
struct CitationChip: View {
    let citation: SourceCitation

    var body: some View {
        HStack(spacing: 6) {
            // Document icon
            Image(systemName: "doc.text")
                .font(.system(size: 14))
                .foregroundColor(.secondary)

            // Citation text
            Text(citation.format())
                .font(.system(size: 12))
                .foregroundColor(.primary)
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemGray5))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .strokeBorder(Color(.systemGray4), lineWidth: 0.5)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(formatAccessibilityLabel())
    }

    private func formatAccessibilityLabel() -> String {
        var label = citation.documentTitle
        if let page = citation.pageNumber {
            label += " page \(page)"
        }
        label += ", \(citation.similarityPercent) percent similarity"
        return label
    }
}

// MARK: - Flow Layout

/// Custom flow layout for wrapping citation chips
/// Similar to Android's FlowRow composable
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(
            in: proposal.replacingUnspecifiedDimensions().width,
            subviews: subviews,
            spacing: spacing
        )
        return CGSize(width: proposal.width ?? 0, height: result.height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(
            in: bounds.width,
            subviews: subviews,
            spacing: spacing
        )

        for (index, subview) in subviews.enumerated() {
            let position = result.positions[index]
            subview.place(
                at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                proposal: ProposedViewSize(result.sizes[index])
            )
        }
    }

    struct FlowResult {
        var positions: [CGPoint] = []
        var sizes: [CGSize] = []
        var height: CGFloat = 0

        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var x: CGFloat = 0
            var y: CGFloat = 0
            var rowHeight: CGFloat = 0

            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)

                if x + size.width > maxWidth && x > 0 {
                    // Move to next row
                    x = 0
                    y += rowHeight + spacing
                    rowHeight = 0
                }

                positions.append(CGPoint(x: x, y: y))
                sizes.append(size)

                x += size.width + spacing
                rowHeight = max(rowHeight, size.height)
            }

            height = y + rowHeight
        }
    }
}

// MARK: - Previews

struct SourceCitationsView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            // Expanded citations
            SourceCitationsView(
                citations: [
                    SourceCitation(
                        documentTitle: "User Manual v2.1",
                        pageNumber: 5,
                        similarityPercent: 92
                    ),
                    SourceCitation(
                        documentTitle: "Technical Specifications",
                        pageNumber: nil,
                        similarityPercent: 87
                    ),
                    SourceCitation(
                        documentTitle: "Installation Guide",
                        pageNumber: 2,
                        similarityPercent: 78
                    ),
                    SourceCitation(
                        documentTitle: "FAQ Document",
                        pageNumber: 12,
                        similarityPercent: 65
                    )
                ],
                isExpanded: true
            )
            .padding()
            .previewDisplayName("Expanded Citations")

            // Collapsed citations
            SourceCitationsView(
                citations: [
                    SourceCitation(
                        documentTitle: "User Manual v2.1",
                        pageNumber: 5,
                        similarityPercent: 92
                    ),
                    SourceCitation(
                        documentTitle: "Technical Specs",
                        pageNumber: nil,
                        similarityPercent: 87
                    )
                ],
                isExpanded: false
            )
            .padding()
            .previewDisplayName("Collapsed Citations")

            // Dark mode
            SourceCitationsView(
                citations: [
                    SourceCitation(
                        documentTitle: "User Manual v2.1",
                        pageNumber: 5,
                        similarityPercent: 92
                    )
                ],
                isExpanded: true
            )
            .preferredColorScheme(.dark)
            .padding()
            .previewDisplayName("Dark Mode")
        }
    }
}
