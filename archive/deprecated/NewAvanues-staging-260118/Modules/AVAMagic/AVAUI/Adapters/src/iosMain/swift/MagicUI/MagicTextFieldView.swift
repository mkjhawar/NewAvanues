import SwiftUI

/**
 * MagicTextFieldView - Native iOS TextField
 *
 * Native SwiftUI text field implementation for IDEAMagic framework.
 * Renders TextFieldComponent from Core as native iOS text field following HIG.
 *
 * Features:
 * - Text input with placeholder
 * - Label support
 * - Leading/trailing icons
 * - Error state display
 * - Disabled state
 * - Secure text entry (password)
 * - Keyboard types
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicTextFieldView: View {
    // MARK: - Properties

    @Binding var text: String
    let placeholder: String
    let label: String?
    let errorText: String?
    let leadingIcon: String?
    let trailingIcon: String?
    let enabled: Bool
    let isSecure: Bool
    let keyboardType: UIKeyboardType
    let onCommit: (() -> Void)?

    @FocusState private var isFocused: Bool

    // MARK: - Initialization

    public init(
        text: Binding<String>,
        placeholder: String = "",
        label: String? = nil,
        errorText: String? = nil,
        leadingIcon: String? = nil,
        trailingIcon: String? = nil,
        enabled: Bool = true,
        isSecure: Bool = false,
        keyboardType: UIKeyboardType = .default,
        onCommit: (() -> Void)? = nil
    ) {
        self._text = text
        self.placeholder = placeholder
        self.label = label
        self.errorText = errorText
        self.leadingIcon = leadingIcon
        self.trailingIcon = trailingIcon
        self.enabled = enabled
        self.isSecure = isSecure
        self.keyboardType = keyboardType
        self.onCommit = onCommit
    }

    // MARK: - Body

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Label
            if let label = label {
                Text(label)
                    .font(.caption)
                    .foregroundColor(labelColor)
            }

            // Text Field Container
            HStack(spacing: 8) {
                // Leading Icon
                if let iconName = leadingIcon {
                    Image(systemName: iconName)
                        .font(.body)
                        .foregroundColor(iconColor)
                }

                // Text Field
                if isSecure {
                    SecureField(placeholder, text: $text)
                        .textFieldStyle(.plain)
                        .keyboardType(keyboardType)
                        .disabled(!enabled)
                        .focused($isFocused)
                } else {
                    TextField(placeholder, text: $text)
                        .textFieldStyle(.plain)
                        .keyboardType(keyboardType)
                        .disabled(!enabled)
                        .focused($isFocused)
                        .onSubmit {
                            onCommit?()
                        }
                }

                // Trailing Icon
                if let iconName = trailingIcon {
                    Image(systemName: iconName)
                        .font(.body)
                        .foregroundColor(iconColor)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(backgroundColor)
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(borderColor, lineWidth: borderWidth)
            )

            // Error Text
            if let error = errorText, !error.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.circle.fill")
                        .font(.caption)
                    Text(error)
                        .font(.caption)
                }
                .foregroundColor(.red)
            }
        }
        .opacity(enabled ? 1.0 : 0.6)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label ?? placeholder)
        .accessibilityValue(text)
        .accessibilityHint(errorText ?? "")
    }

    // MARK: - Colors & Styles

    private var labelColor: Color {
        if errorText != nil {
            return .red
        }
        return .secondary
    }

    private var iconColor: Color {
        if errorText != nil {
            return .red
        }
        if isFocused {
            return .accentColor
        }
        return .secondary
    }

    private var backgroundColor: Color {
        if enabled {
            return Color(uiColor: .secondarySystemBackground)
        }
        return Color(uiColor: .tertiarySystemBackground)
    }

    private var borderColor: Color {
        if errorText != nil {
            return .red
        }
        if isFocused {
            return .accentColor
        }
        return Color(uiColor: .separator)
    }

    private var borderWidth: CGFloat {
        if errorText != nil || isFocused {
            return 2
        }
        return 1
    }
}

// MARK: - Preview

#if DEBUG
struct MagicTextFieldView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Basic text field
                VStack(alignment: .leading, spacing: 8) {
                    Text("Basic TextField").font(.headline)
                    MagicTextFieldView(
                        text: .constant(""),
                        placeholder: "Enter text"
                    )
                }

                // With label
                VStack(alignment: .leading, spacing: 8) {
                    Text("With Label").font(.headline)
                    MagicTextFieldView(
                        text: .constant(""),
                        placeholder: "email@example.com",
                        label: "Email Address"
                    )
                }

                // With leading icon
                VStack(alignment: .leading, spacing: 8) {
                    Text("With Leading Icon").font(.headline)
                    MagicTextFieldView(
                        text: .constant(""),
                        placeholder: "Search",
                        leadingIcon: "magnifyingglass"
                    )
                }

                // With trailing icon
                VStack(alignment: .leading, spacing: 8) {
                    Text("With Trailing Icon").font(.headline)
                    MagicTextFieldView(
                        text: .constant(""),
                        placeholder: "Enter password",
                        label: "Password",
                        trailingIcon: "eye.slash"
                    )
                }

                // Secure field
                VStack(alignment: .leading, spacing: 8) {
                    Text("Secure Field").font(.headline)
                    MagicTextFieldView(
                        text: .constant(""),
                        placeholder: "Enter password",
                        label: "Password",
                        leadingIcon: "lock.fill",
                        isSecure: true
                    )
                }

                // Error state
                VStack(alignment: .leading, spacing: 8) {
                    Text("Error State").font(.headline)
                    MagicTextFieldView(
                        text: .constant("invalid@"),
                        placeholder: "email@example.com",
                        label: "Email",
                        errorText: "Please enter a valid email address",
                        leadingIcon: "envelope.fill"
                    )
                }

                // Disabled
                VStack(alignment: .leading, spacing: 8) {
                    Text("Disabled").font(.headline)
                    MagicTextFieldView(
                        text: .constant("Read only text"),
                        placeholder: "Disabled",
                        label: "Disabled Field",
                        enabled: false
                    )
                }

                // With both icons
                VStack(alignment: .leading, spacing: 8) {
                    Text("With Both Icons").font(.headline)
                    MagicTextFieldView(
                        text: .constant(""),
                        placeholder: "Username",
                        label: "Username",
                        leadingIcon: "person.fill",
                        trailingIcon: "checkmark.circle.fill"
                    )
                }

                // Email keyboard
                VStack(alignment: .leading, spacing: 8) {
                    Text("Email Keyboard").font(.headline)
                    MagicTextFieldView(
                        text: .constant(""),
                        placeholder: "email@example.com",
                        label: "Email",
                        leadingIcon: "envelope.fill",
                        keyboardType: .emailAddress
                    )
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}
#endif
