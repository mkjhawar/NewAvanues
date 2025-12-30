import SwiftUI

/**
 * MagicCardView - Native iOS Card
 *
 * Native SwiftUI card implementation for IDEAMagic framework.
 * Renders CardComponent from Core as native iOS card following HIG.
 *
 * Features:
 * - Multiple card styles (filled, elevated, outlined)
 * - Shadow/elevation support
 * - Clickable cards
 * - Corner radius customization
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicCardView<Content: View>: View {
    // MARK: - Properties

    let style: CardStyle
    let cornerRadius: CGFloat
    let action: (() -> Void)?
    let content: Content

    // MARK: - Initialization

    public init(
        style: CardStyle = .filled,
        cornerRadius: CGFloat = 12,
        action: (() -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.style = style
        self.cornerRadius = cornerRadius
        self.action = action
        self.content = content()
    }

    // MARK: - Body

    public var body: some View {
        Group {
            if let action = action {
                Button(action: action) {
                    cardContent
                }
                .buttonStyle(PlainButtonStyle())
            } else {
                cardContent
            }
        }
    }

    // MARK: - Card Content

    private var cardContent: some View {
        content
            .background(backgroundForStyle)
            .cornerRadius(cornerRadius)
            .shadow(
                color: shadowColor,
                radius: shadowRadius,
                x: 0,
                y: shadowOffset
            )
            .accessibilityElement(children: .contain)
    }

    // MARK: - Style Mapping

    private var backgroundForStyle: some View {
        Group {
            switch style {
            case .filled:
                Color(uiColor: .secondarySystemGroupedBackground)
            case .elevated:
                Color(uiColor: .systemBackground)
            case .outlined:
                Color.clear
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .stroke(Color(uiColor: .separator), lineWidth: 1)
                    )
            }
        }
    }

    private var shadowColor: Color {
        switch style {
        case .filled:
            return Color.black.opacity(0.05)
        case .elevated:
            return Color.black.opacity(0.1)
        case .outlined:
            return Color.clear
        }
    }

    private var shadowRadius: CGFloat {
        switch style {
        case .filled:
            return 2
        case .elevated:
            return 4
        case .outlined:
            return 0
        }
    }

    private var shadowOffset: CGFloat {
        switch style {
        case .filled:
            return 1
        case .elevated:
            return 2
        case .outlined:
            return 0
        }
    }
}

// MARK: - Card Style Enum

public enum CardStyle {
    case filled
    case elevated
    case outlined
}

// MARK: - Preview

#if DEBUG
struct MagicCardView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Filled card
                VStack(alignment: .leading, spacing: 8) {
                    Text("Filled Card").font(.headline)
                    MagicCardView(style: .filled) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Card Title")
                                .font(.title3)
                                .fontWeight(.semibold)
                            Text("This is a filled card with default elevation and styling.")
                                .font(.body)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                    }
                }

                // Elevated card
                VStack(alignment: .leading, spacing: 8) {
                    Text("Elevated Card").font(.headline)
                    MagicCardView(style: .elevated) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Card Title")
                                .font(.title3)
                                .fontWeight(.semibold)
                            Text("This card has higher elevation for more prominence.")
                                .font(.body)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                    }
                }

                // Outlined card
                VStack(alignment: .leading, spacing: 8) {
                    Text("Outlined Card").font(.headline)
                    MagicCardView(style: .outlined) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Card Title")
                                .font(.title3)
                                .fontWeight(.semibold)
                            Text("This card uses a border instead of elevation.")
                                .font(.body)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                    }
                }

                // Clickable card
                VStack(alignment: .leading, spacing: 8) {
                    Text("Clickable Card").font(.headline)
                    MagicCardView(
                        style: .filled,
                        action: {
                            print("Card tapped!")
                        }
                    ) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Clickable Card")
                                .font(.title3)
                                .fontWeight(.semibold)
                            Text("Tap me to trigger an action!")
                                .font(.body)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                    }
                }

                // Card with image
                VStack(alignment: .leading, spacing: 8) {
                    Text("Card with Image").font(.headline)
                    MagicCardView(style: .filled) {
                        VStack(alignment: .leading, spacing: 0) {
                            // Image area
                            Rectangle()
                                .fill(Color.blue.opacity(0.2))
                                .frame(height: 120)
                                .overlay(
                                    Text("Image Area")
                                        .foregroundColor(.blue)
                                )

                            // Content
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Card Title")
                                    .font(.title3)
                                    .fontWeight(.semibold)
                                Text("Card with image header and content below.")
                                    .font(.body)
                                    .foregroundColor(.secondary)
                            }
                            .padding()
                        }
                    }
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}
#endif
