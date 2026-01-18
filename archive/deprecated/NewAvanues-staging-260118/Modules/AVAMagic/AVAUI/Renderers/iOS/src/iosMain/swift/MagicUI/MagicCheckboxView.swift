import SwiftUI

/**
 * MagicCheckboxView - Native iOS Toggle (Checkbox)
 *
 * Native SwiftUI toggle implementation for IDEAMagic framework.
 * Renders CheckboxComponent from Core as native iOS Toggle.
 *
 * Note: iOS doesn't have a native checkbox. This uses Toggle which is the
 * iOS equivalent. For checkbox appearance, use .toggleStyle(.button) in iOS 15+
 *
 * Features:
 * - Toggle switch (iOS default)
 * - Checkbox button style (iOS 15+)
 * - Label support
 * - Disabled state
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicCheckboxView: View {
    // MARK: - Properties

    @Binding var isChecked: Bool
    let label: String
    let style: CheckboxStyle
    let enabled: Bool
    let onChange: ((Bool) -> Void)?

    // MARK: - Initialization

    public init(
        isChecked: Binding<Bool>,
        label: String,
        style: CheckboxStyle = .toggle,
        enabled: Bool = true,
        onChange: ((Bool) -> Void)? = nil
    ) {
        self._isChecked = isChecked
        self.label = label
        self.style = style
        self.enabled = enabled
        self.onChange = onChange
    }

    // MARK: - Body

    public var body: some View {
        Group {
            switch style {
            case .toggle:
                Toggle(label, isOn: $isChecked)
                    .toggleStyle(SwitchToggleStyle())
                    .disabled(!enabled)
                    .onChange(of: isChecked) { newValue in
                        onChange?(newValue)
                    }

            case .checkmark:
                Button(action: {
                    if enabled {
                        isChecked.toggle()
                        onChange?(isChecked)
                    }
                }) {
                    HStack(spacing: 12) {
                        Image(systemName: isChecked ? "checkmark.square.fill" : "square")
                            .font(.system(size: 24))
                            .foregroundColor(isChecked ? .accentColor : .secondary)

                        Text(label)
                            .foregroundColor(.primary)

                        Spacer()
                    }
                }
                .buttonStyle(PlainButtonStyle())
                .disabled(!enabled)
                .opacity(enabled ? 1.0 : 0.6)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(label)
        .accessibilityValue(isChecked ? "Checked" : "Unchecked")
        .accessibilityAddTraits(isChecked ? .isSelected : [])
    }
}

// MARK: - Checkbox Style Enum

public enum CheckboxStyle {
    case toggle     // Native iOS toggle switch
    case checkmark  // Custom checkmark button
}

// MARK: - Preview

#if DEBUG
struct MagicCheckboxView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Toggle style (iOS default)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Toggle Style (iOS Default)").font(.headline)

                    MagicCheckboxView(
                        isChecked: .constant(false),
                        label: "Unchecked",
                        style: .toggle
                    )

                    MagicCheckboxView(
                        isChecked: .constant(true),
                        label: "Checked",
                        style: .toggle
                    )

                    MagicCheckboxView(
                        isChecked: .constant(true),
                        label: "Disabled",
                        style: .toggle,
                        enabled: false
                    )
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)

                // Checkmark style (custom)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Checkmark Style").font(.headline)

                    MagicCheckboxView(
                        isChecked: .constant(false),
                        label: "Unchecked",
                        style: .checkmark
                    )

                    MagicCheckboxView(
                        isChecked: .constant(true),
                        label: "Checked",
                        style: .checkmark
                    )

                    MagicCheckboxView(
                        isChecked: .constant(true),
                        label: "Disabled",
                        style: .checkmark,
                        enabled: false
                    )
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)

                // Settings list
                VStack(alignment: .leading, spacing: 8) {
                    Text("Settings List").font(.headline)

                    List {
                        Section(header: Text("Notifications")) {
                            MagicCheckboxView(
                                isChecked: .constant(true),
                                label: "Email Notifications",
                                style: .toggle
                            )

                            MagicCheckboxView(
                                isChecked: .constant(false),
                                label: "Push Notifications",
                                style: .toggle
                            )

                            MagicCheckboxView(
                                isChecked: .constant(true),
                                label: "SMS Notifications",
                                style: .toggle
                            )
                        }

                        Section(header: Text("Privacy")) {
                            MagicCheckboxView(
                                isChecked: .constant(true),
                                label: "Share Analytics",
                                style: .toggle
                            )

                            MagicCheckboxView(
                                isChecked: .constant(false),
                                label: "Location Services",
                                style: .toggle
                            )
                        }
                    }
                    .frame(height: 350)
                }

                // Checklist (checkmark style)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Todo Checklist").font(.headline)

                    VStack(spacing: 0) {
                        MagicCheckboxView(
                            isChecked: .constant(true),
                            label: "Design mockups",
                            style: .checkmark
                        )
                        .padding()

                        Divider()

                        MagicCheckboxView(
                            isChecked: .constant(true),
                            label: "Write documentation",
                            style: .checkmark
                        )
                        .padding()

                        Divider()

                        MagicCheckboxView(
                            isChecked: .constant(false),
                            label: "Code review",
                            style: .checkmark
                        )
                        .padding()

                        Divider()

                        MagicCheckboxView(
                            isChecked: .constant(false),
                            label: "Deploy to production",
                            style: .checkmark
                        )
                        .padding()
                    }
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(12)
                }

                // Form example
                VStack(alignment: .leading, spacing: 8) {
                    Text("Form Example").font(.headline)

                    VStack(alignment: .leading, spacing: 16) {
                        Text("Select your interests:")
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        MagicCheckboxView(
                            isChecked: .constant(true),
                            label: "Technology",
                            style: .checkmark
                        )

                        MagicCheckboxView(
                            isChecked: .constant(false),
                            label: "Design",
                            style: .checkmark
                        )

                        MagicCheckboxView(
                            isChecked: .constant(true),
                            label: "Business",
                            style: .checkmark
                        )

                        MagicCheckboxView(
                            isChecked: .constant(false),
                            label: "Marketing",
                            style: .checkmark
                        )

                        MagicCheckboxView(
                            isChecked: .constant(true),
                            label: "I agree to the terms and conditions",
                            style: .checkmark
                        )

                        Button("Submit") {}
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(12)
                }

                // Grouped checkboxes
                VStack(alignment: .leading, spacing: 8) {
                    Text("Grouped Options").font(.headline)

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Preferred Contact Method")
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        VStack(spacing: 8) {
                            MagicCheckboxView(
                                isChecked: .constant(true),
                                label: "Email",
                                style: .checkmark
                            )

                            MagicCheckboxView(
                                isChecked: .constant(true),
                                label: "Phone",
                                style: .checkmark
                            )

                            MagicCheckboxView(
                                isChecked: .constant(false),
                                label: "SMS",
                                style: .checkmark
                            )

                            MagicCheckboxView(
                                isChecked: .constant(false),
                                label: "Mail",
                                style: .checkmark
                            )
                        }
                    }
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(12)
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}
#endif
