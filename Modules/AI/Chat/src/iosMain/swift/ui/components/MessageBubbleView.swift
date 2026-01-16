// filename: Universal/AVA/Features/Chat/src/iosMain/swift/ui/components/MessageBubbleView.swift
// created: 2025-11-22
// author: iOS RAG Chat Integration Specialist (Agent 1)
// © Augmentalis Inc, Intelligent Devices LLC

import SwiftUI

/// Message bubble component for displaying user and AVA messages.
///
/// iOS SwiftUI equivalent of Android's MessageBubble composable.
///
/// Displays messages with appropriate styling based on the sender (user vs AVA),
/// including background color, alignment, timestamp formatting, and confidence badges.
///
/// Phase 2 Enhancement: Confidence badges with three visual states:
/// - High (>70%): Green badge with percentage only
/// - Medium (50-70%): Orange badge with "Confirm?" button
/// - Low (<50%): Red badge with "Teach AVA" button
///
/// Phase 3 Enhancement: Long-press context menu
/// - Long-press on any message to show "Teach AVA this" option
///
/// RAG Phase 2 Enhancement: Source citations
/// - Display document sources that contributed to RAG-enhanced responses
/// - Collapsible citations section
/// - Shows document title, page number, and similarity score
struct MessageBubbleView: View {

    // MARK: - Properties

    let message: Message
    let onConfirm: (() -> Void)?
    let onTeachAva: (() -> Void)?
    let onLongPress: (() -> Void)?

    @State private var showContextMenu = false

    // MARK: - Computed Properties

    private var isUserMessage: Bool {
        message.role == .user
    }

    private var bubbleColor: Color {
        isUserMessage ? .accentColor : Color(.systemGray5)
    }

    private var textColor: Color {
        isUserMessage ? .white : .primary
    }

    private var alignment: HorizontalAlignment {
        isUserMessage ? .trailing : .leading
    }

    // MARK: - Body

    var body: some View {
        VStack(alignment: alignment, spacing: 4) {
            // Message bubble with long-press support
            messageBubble
                .contextMenu {
                    if onLongPress != nil {
                        Button {
                            onLongPress?()
                        } label: {
                            Label("Teach AVA this", systemImage: "graduationcap.fill")
                        }
                    }
                }

            // Timestamp
            timestampView

            // Confidence badge (only for AVA messages)
            if !isUserMessage, let confidence = message.confidence {
                ConfidenceBadgeView(
                    confidence: confidence,
                    onConfirm: onConfirm,
                    onTeachAva: onTeachAva
                )
                .padding(.top, 4)
                .transition(.opacity.combined(with: .scale))
            }

            // Source citations (RAG Phase 2)
            if !isUserMessage && !message.sourceCitations.isEmpty {
                SourceCitationsView(citations: message.sourceCitations)
                    .padding(.top, 8)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .frame(maxWidth: .infinity, alignment: isUserMessage ? .trailing : .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityDescription)
    }

    // MARK: - Subviews

    private var messageBubble: some View {
        Text(message.content)
            .font(.body)
            .foregroundColor(textColor)
            .padding(12)
            .background(bubbleColor)
            .clipShape(
                BubbleShape(isUserMessage: isUserMessage)
            )
            .frame(maxWidth: 280, alignment: isUserMessage ? .trailing : .leading)
    }

    private var timestampView: some View {
        Text(formatRelativeTime(timestamp: message.timestamp))
            .font(.caption2)
            .foregroundColor(.secondary)
            .padding(.horizontal, 4)
    }

    private var accessibilityDescription: String {
        var description = isUserMessage ? "You said: " : "AVA said: "
        description += message.content
        description += ", " + formatRelativeTime(timestamp: message.timestamp)

        if let confidence = message.confidence {
            description += ", confidence \(Int(confidence * 100)) percent"
        }

        if !message.sourceCitations.isEmpty {
            description += ", \(message.sourceCitations.count) sources cited"
        }

        return description
    }
}

// MARK: - Bubble Shape

/// Custom shape for message bubbles with different corner radii
struct BubbleShape: Shape {
    let isUserMessage: Bool

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: isUserMessage ?
                [.topLeft, .topRight, .bottomLeft] :
                [.topLeft, .topRight, .bottomRight],
            cornerRadii: CGSize(width: 16, height: 16)
        )
        return Path(path.cgPath)
    }
}

// MARK: - Confidence Badge View

/// Confidence badge component for AVA messages.
///
/// Displays a color-coded badge indicating AVA's confidence in her response,
/// with appropriate action buttons based on confidence level.
struct ConfidenceBadgeView: View {

    let confidence: Float
    let onConfirm: (() -> Void)?
    let onTeachAva: (() -> Void)?

    private var percentage: Int {
        Int(confidence * 100)
    }

    private var confidenceLevel: ConfidenceLevel {
        switch confidence {
        case 0.7...1.0:
            return .high
        case 0.5..<0.7:
            return .medium
        default:
            return .low
        }
    }

    private var badgeColor: Color {
        switch confidenceLevel {
        case .high:
            return Color(red: 0.30, green: 0.69, blue: 0.31) // Green 500
        case .medium:
            return Color(red: 1.0, green: 0.65, blue: 0.15) // Orange 400
        case .low:
            return Color(red: 0.90, green: 0.22, blue: 0.21) // Red 600
        }
    }

    var body: some View {
        VStack(alignment: .trailing, spacing: 6) {
            // Badge with percentage
            HStack(spacing: 6) {
                Circle()
                    .fill(Color.white.opacity(0.9))
                    .frame(width: 8, height: 8)

                Text("\(percentage)%")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(badgeColor)
            )
            .frame(height: 24)

            // Action buttons based on confidence level
            if confidenceLevel == .medium, let onConfirm = onConfirm {
                Button(action: onConfirm) {
                    Text("Confirm?")
                        .font(.system(size: 13, weight: .medium))
                }
                .buttonStyle(.borderless)
                .tint(.accentColor)
                .frame(minHeight: 48) // WCAG AA minimum touch target
            } else if confidenceLevel == .low, let onTeachAva = onTeachAva {
                Button(action: onTeachAva) {
                    Label("Teach AVA", systemImage: "graduationcap.fill")
                        .font(.system(size: 13, weight: .semibold))
                }
                .buttonStyle(.borderedProminent)
                .tint(Color(red: 0.90, green: 0.22, blue: 0.21))
                .frame(minHeight: 48) // WCAG AA minimum touch target
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityText)
    }

    private var accessibilityText: String {
        switch confidenceLevel {
        case .high:
            return "High confidence: \(percentage) percent"
        case .medium:
            return "Medium confidence: \(percentage) percent, tap to confirm"
        case .low:
            return "Low confidence: \(percentage) percent, tap to teach AVA"
        }
    }
}

/// Confidence level enum
enum ConfidenceLevel {
    case high   // >70%: Green badge
    case medium // 50-70%: Orange badge
    case low    // <50%: Red badge
}

// MARK: - Helper Functions

/// Formats Unix timestamp to human-readable relative time.
///
/// Examples:
/// - "Just now" (< 1 minute)
/// - "2m ago" (< 1 hour)
/// - "1h ago" (< 24 hours)
/// - "Yesterday 3:15 PM" (< 48 hours)
/// - "Jan 15, 3:15 PM" (older)
private func formatRelativeTime(timestamp: Double) -> String {
    let now = Date().timeIntervalSince1970 * 1000
    let diff = now - timestamp
    let seconds = diff / 1000
    let minutes = seconds / 60
    let hours = minutes / 60
    let days = hours / 24

    switch true {
    case seconds < 60:
        return "Just now"
    case minutes < 60:
        return "\(Int(minutes))m ago"
    case hours < 24:
        return "\(Int(hours))h ago"
    case days < 2:
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        let date = Date(timeIntervalSince1970: timestamp / 1000)
        return "Yesterday \(formatter.string(from: date))"
    default:
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, h:mm a"
        let date = Date(timeIntervalSince1970: timestamp / 1000)
        return formatter.string(from: date)
    }
}

// MARK: - Previews

struct MessageBubbleView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 16) {
                // User message
                MessageBubbleView(
                    message: Message(
                        id: "1",
                        content: "Turn on the lights",
                        role: .user,
                        timestamp: Date().timeIntervalSince1970 * 1000 - 120000,
                        confidence: nil,
                        intent: nil,
                        sourceCitations: []
                    ),
                    onConfirm: nil,
                    onTeachAva: nil,
                    onLongPress: {}
                )

                // High confidence AVA response
                MessageBubbleView(
                    message: Message(
                        id: "2",
                        content: "I'll control the lights.",
                        role: .assistant,
                        timestamp: Date().timeIntervalSince1970 * 1000 - 60000,
                        confidence: 0.85,
                        intent: "lights.control",
                        sourceCitations: []
                    ),
                    onConfirm: nil,
                    onTeachAva: nil,
                    onLongPress: {}
                )

                // Medium confidence with confirm button
                MessageBubbleView(
                    message: Message(
                        id: "3",
                        content: "Did you want me to check the weather?",
                        role: .assistant,
                        timestamp: Date().timeIntervalSince1970 * 1000 - 30000,
                        confidence: 0.65,
                        intent: nil,
                        sourceCitations: []
                    ),
                    onConfirm: {},
                    onTeachAva: nil,
                    onLongPress: {}
                )

                // Low confidence with teach button
                MessageBubbleView(
                    message: Message(
                        id: "4",
                        content: "I'm not sure I understood. Would you like to teach me?",
                        role: .assistant,
                        timestamp: Date().timeIntervalSince1970 * 1000 - 15000,
                        confidence: 0.35,
                        intent: nil,
                        sourceCitations: []
                    ),
                    onConfirm: nil,
                    onTeachAva: {},
                    onLongPress: {}
                )

                // Message with citations
                MessageBubbleView(
                    message: Message(
                        id: "5",
                        content: "Based on your documents, the system requirements are as follows: minimum 4GB RAM, 100MB storage, and compatible OS.",
                        role: .assistant,
                        timestamp: Date().timeIntervalSince1970 * 1000 - 5000,
                        confidence: 0.85,
                        intent: nil,
                        sourceCitations: [
                            SourceCitation(
                                documentTitle: "User Manual v2.1",
                                pageNumber: 5,
                                similarityPercent: 92
                            ),
                            SourceCitation(
                                documentTitle: "Technical Specifications",
                                pageNumber: nil,
                                similarityPercent: 87
                            )
                        ]
                    ),
                    onConfirm: nil,
                    onTeachAva: nil,
                    onLongPress: {}
                )
            }
            .padding()
        }
        .previewDisplayName("All Message Variants")
    }
}
