import SwiftUI

/**
 * MagicButtonView - Native iOS Button
 *
 * Native SwiftUI button implementation for IDEAMagic framework.
 * Renders ButtonComponent from Core as native iOS button following HIG.
 *
 * Features:
 * - Multiple button styles (filled, tonal, outlined, text)
 * - SF Symbols icon support
 * - Disabled state
 * - Accessibility support (VoiceOver, Dynamic Type)
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicButtonView: View {
    // MARK: - Properties

    let text: String
    let style: ButtonStyle
    let enabled: Bool
    let icon: String?
    let action: () -> Void

    // MARK: - Initialization

    public init(
        text: String,
        style: ButtonStyle = .filled,
        enabled: Bool = true,
        icon: String? = nil,
        action: @escaping () -> Void
    ) {
        self.text = text
        self.style = style
        self.enabled = enabled
        self.icon = icon
        self.action = action
    }

    // MARK: - Body

    public var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let iconName = icon {
                    Image(systemName: iconName)
                        .font(.body)
                }
                Text(text)
                    .font(.body)
                    .fontWeight(.semibold)
            }
            .frame(minHeight: 48)
            .padding(.horizontal, 24)
        }
        .buttonStyle(styleVariant)
        .disabled(!enabled)
        .accessibilityLabel(text)
        .accessibilityHint(enabled ? "Double tap to activate" : "Button is disabled")
    }

    // MARK: - Style Mapping

    private var styleVariant: some PrimitiveButtonStyle {
        switch style {
        case .filled:
            return FilledButtonStyle()
        case .tonal:
            return TonalButtonStyle()
        case .outlined:
            return OutlinedButtonStyle()
        case .text:
            return TextButtonStyle()
        }
    }
}

// MARK: - Button Style Enum

public enum ButtonStyle {
    case filled
    case tonal
    case outlined
    case text
}

// MARK: - Custom Button Styles

/// Filled button style (primary action)
struct FilledButtonStyle: PrimitiveButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        FilledButton(configuration: configuration)
    }

    struct FilledButton: View {
        let configuration: Configuration
        @Environment(\.isEnabled) var isEnabled

        var body: some View {
            configuration.label
                .foregroundColor(.white)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(isEnabled ? Color.accentColor : Color.gray.opacity(0.3))
                )
        }
    }
}

/// Tonal button style (secondary action)
struct TonalButtonStyle: PrimitiveButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        TonalButton(configuration: configuration)
    }

    struct TonalButton: View {
        let configuration: Configuration
        @Environment(\.isEnabled) var isEnabled

        var body: some View {
            configuration.label
                .foregroundColor(isEnabled ? .accentColor : .gray)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(isEnabled ? Color.accentColor.opacity(0.12) : Color.gray.opacity(0.05))
                )
        }
    }
}

/// Outlined button style (tertiary action)
struct OutlinedButtonStyle: PrimitiveButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        OutlinedButton(configuration: configuration)
    }

    struct OutlinedButton: View {
        let configuration: Configuration
        @Environment(\.isEnabled) var isEnabled

        var body: some View {
            configuration.label
                .foregroundColor(isEnabled ? .accentColor : .gray)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(isEnabled ? Color.accentColor : Color.gray, lineWidth: 1)
                )
        }
    }
}

/// Text button style (minimal action)
struct TextButtonStyle: PrimitiveButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        TextButton(configuration: configuration)
    }

    struct TextButton: View {
        let configuration: Configuration
        @Environment(\.isEnabled) var isEnabled

        var body: some View {
            configuration.label
                .foregroundColor(isEnabled ? .accentColor : .gray)
        }
    }
}

// MARK: - Preview

#if DEBUG
struct MagicButtonView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 16) {
            MagicButtonView(text: "Filled Button", style: .filled) {
                print("Filled button tapped")
            }

            MagicButtonView(text: "Tonal Button", style: .tonal) {
                print("Tonal button tapped")
            }

            MagicButtonView(text: "Outlined Button", style: .outlined) {
                print("Outlined button tapped")
            }

            MagicButtonView(text: "Text Button", style: .text) {
                print("Text button tapped")
            }

            MagicButtonView(text: "With Icon", style: .filled, icon: "heart.fill") {
                print("Icon button tapped")
            }

            MagicButtonView(text: "Disabled", style: .filled, enabled: false) {
                print("Should not print")
            }
        }
        .padding()
        .previewLayout(.sizeThatFits)
    }
}
#endif
