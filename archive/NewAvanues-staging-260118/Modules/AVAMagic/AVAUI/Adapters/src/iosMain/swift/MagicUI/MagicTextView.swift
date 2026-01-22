import SwiftUI

/**
 * MagicTextView - Native iOS Text
 *
 * Native SwiftUI text implementation for IDEAMagic framework.
 * Renders TextComponent from Core as native iOS text following HIG.
 *
 * Features:
 * - Typography scale (13 variants matching Material 3)
 * - Text alignment support
 * - Color customization
 * - Accessibility support (Dynamic Type, VoiceOver)
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicTextView: View {
    // MARK: - Properties

    let text: String
    let style: TextStyle
    let alignment: TextAlignment
    let color: Color?
    let maxLines: Int?

    // MARK: - Initialization

    public init(
        text: String,
        style: TextStyle = .bodyMedium,
        alignment: TextAlignment = .leading,
        color: Color? = nil,
        maxLines: Int? = nil
    ) {
        self.text = text
        self.style = style
        self.alignment = alignment
        self.color = color
        self.maxLines = maxLines
    }

    // MARK: - Body

    public var body: some View {
        Text(text)
            .font(fontForStyle)
            .foregroundColor(color ?? .primary)
            .multilineTextAlignment(alignment)
            .lineLimit(maxLines)
            .accessibilityLabel(text)
    }

    // MARK: - Font Mapping

    private var fontForStyle: Font {
        switch style {
        // Display styles
        case .displayLarge:
            return .system(size: 57, weight: .regular)
        case .displayMedium:
            return .system(size: 45, weight: .regular)
        case .displaySmall:
            return .system(size: 36, weight: .regular)

        // Headline styles
        case .headlineLarge:
            return .system(size: 32, weight: .regular)
        case .headlineMedium:
            return .system(size: 28, weight: .regular)
        case .headlineSmall:
            return .system(size: 24, weight: .regular)

        // Title styles
        case .titleLarge:
            return .system(size: 22, weight: .regular)
        case .titleMedium:
            return .system(size: 16, weight: .medium)
        case .titleSmall:
            return .system(size: 14, weight: .medium)

        // Body styles
        case .bodyLarge:
            return .system(size: 16, weight: .regular)
        case .bodyMedium:
            return .system(size: 14, weight: .regular)
        case .bodySmall:
            return .system(size: 12, weight: .regular)

        // Label styles
        case .labelLarge:
            return .system(size: 14, weight: .medium)
        case .labelMedium:
            return .system(size: 12, weight: .medium)
        case .labelSmall:
            return .system(size: 11, weight: .medium)
        }
    }
}

// MARK: - Text Style Enum

public enum TextStyle {
    // Display
    case displayLarge
    case displayMedium
    case displaySmall

    // Headline
    case headlineLarge
    case headlineMedium
    case headlineSmall

    // Title
    case titleLarge
    case titleMedium
    case titleSmall

    // Body
    case bodyLarge
    case bodyMedium
    case bodySmall

    // Label
    case labelLarge
    case labelMedium
    case labelSmall
}

// MARK: - Preview

#if DEBUG
struct MagicTextView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Group {
                    Text("Display Styles").font(.headline)
                    MagicTextView(text: "Display Large", style: .displayLarge)
                    MagicTextView(text: "Display Medium", style: .displayMedium)
                    MagicTextView(text: "Display Small", style: .displaySmall)
                }

                Group {
                    Text("Headline Styles").font(.headline)
                    MagicTextView(text: "Headline Large", style: .headlineLarge)
                    MagicTextView(text: "Headline Medium", style: .headlineMedium)
                    MagicTextView(text: "Headline Small", style: .headlineSmall)
                }

                Group {
                    Text("Title Styles").font(.headline)
                    MagicTextView(text: "Title Large", style: .titleLarge)
                    MagicTextView(text: "Title Medium", style: .titleMedium)
                    MagicTextView(text: "Title Small", style: .titleSmall)
                }

                Group {
                    Text("Body Styles").font(.headline)
                    MagicTextView(
                        text: "Body Large - Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                        style: .bodyLarge
                    )
                    MagicTextView(
                        text: "Body Medium - Sed do eiusmod tempor incididunt ut labore.",
                        style: .bodyMedium
                    )
                    MagicTextView(
                        text: "Body Small - Ut enim ad minim veniam.",
                        style: .bodySmall
                    )
                }

                Group {
                    Text("Label Styles").font(.headline)
                    MagicTextView(text: "Label Large", style: .labelLarge)
                    MagicTextView(text: "Label Medium", style: .labelMedium)
                    MagicTextView(text: "Label Small", style: .labelSmall)
                }

                Group {
                    Text("Alignment").font(.headline)
                    MagicTextView(text: "Leading (Default)", alignment: .leading)
                    MagicTextView(text: "Center", alignment: .center)
                    MagicTextView(text: "Trailing", alignment: .trailing)
                }

                Group {
                    Text("Colors").font(.headline)
                    MagicTextView(text: "Primary Color", color: .primary)
                    MagicTextView(text: "Accent Color", color: .accentColor)
                    MagicTextView(text: "Red Color", color: .red)
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}
#endif
