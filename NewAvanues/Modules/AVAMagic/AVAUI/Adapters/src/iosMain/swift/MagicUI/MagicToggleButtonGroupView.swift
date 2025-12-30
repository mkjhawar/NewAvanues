import SwiftUI

/**
 * MagicToggleButtonGroupView - iOS Toggle Button Group
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicToggleButtonGroupView: View {
    @Binding var selectedValues: [String]

    let buttons: [ToggleButtonData]
    let selectionMode: SelectionMode
    let orientation: ToggleOrientation
    let label: String?
    let variant: ButtonVariant
    let size: ButtonSize
    let fullWidth: Bool
    let required: Bool
    let enabled: Bool
    let onSelectionChanged: (([String]) -> Void)?

    public init(
        selectedValues: Binding<[String]>,
        buttons: [ToggleButtonData],
        selectionMode: SelectionMode = .single,
        orientation: ToggleOrientation = .horizontal,
        label: String? = nil,
        variant: ButtonVariant = .outlined,
        size: ButtonSize = .medium,
        fullWidth: Bool = false,
        required: Bool = false,
        enabled: Bool = true,
        onSelectionChanged: (([String]) -> Void)? = nil
    ) {
        self._selectedValues = selectedValues
        self.buttons = buttons
        self.selectionMode = selectionMode
        self.orientation = orientation
        self.label = label
        self.variant = variant
        self.size = size
        self.fullWidth = fullWidth
        self.required = required
        self.enabled = enabled
        self.onSelectionChanged = onSelectionChanged
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Label
            if let labelText = label {
                Text(labelText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Button group
            Group {
                switch orientation {
                case .horizontal:
                    if fullWidth {
                        HStack(spacing: 4) {
                            ForEach(buttons, id: \.value) { button in
                                ToggleButtonView(
                                    button: button,
                                    isSelected: selectedValues.contains(button.value),
                                    variant: variant,
                                    size: size,
                                    enabled: enabled && !button.disabled
                                ) {
                                    handleSelection(button.value)
                                }
                                .frame(maxWidth: .infinity)
                            }
                        }
                    } else {
                        HStack(spacing: 4) {
                            ForEach(buttons, id: \.value) { button in
                                ToggleButtonView(
                                    button: button,
                                    isSelected: selectedValues.contains(button.value),
                                    variant: variant,
                                    size: size,
                                    enabled: enabled && !button.disabled
                                ) {
                                    handleSelection(button.value)
                                }
                            }
                        }
                    }
                case .vertical:
                    VStack(spacing: 4) {
                        ForEach(buttons, id: \.value) { button in
                            ToggleButtonView(
                                button: button,
                                isSelected: selectedValues.contains(button.value),
                                variant: variant,
                                size: size,
                                enabled: enabled && !button.disabled
                            ) {
                                handleSelection(button.value)
                            }
                            .frame(maxWidth: fullWidth ? .infinity : nil)
                        }
                    }
                }
            }
        }
    }

    private func handleSelection(_ value: String) {
        var newSelection: [String]

        switch selectionMode {
        case .single:
            if selectedValues.contains(value) {
                // Deselecting
                if required {
                    newSelection = selectedValues // Can't deselect when required
                } else {
                    newSelection = []
                }
            } else {
                newSelection = [value]
            }
        case .multiple:
            if selectedValues.contains(value) {
                newSelection = selectedValues.filter { $0 != value }
            } else {
                newSelection = selectedValues + [value]
            }
        }

        selectedValues = newSelection
        onSelectionChanged?(newSelection)
    }
}

// Individual toggle button view
private struct ToggleButtonView: View {
    let button: ToggleButtonData
    let isSelected: Bool
    let variant: ButtonVariant
    let size: ButtonSize
    let enabled: Bool
    let action: () -> Void

    private var padding: EdgeInsets {
        switch size {
        case .small:
            return EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12)
        case .medium:
            return EdgeInsets(top: 10, leading: 16, bottom: 10, trailing: 16)
        case .large:
            return EdgeInsets(top: 14, leading: 20, bottom: 14, trailing: 20)
        }
    }

    private var fontSize: CGFloat {
        switch size {
        case .small: return 12
        case .medium: return 14
        case .large: return 16
        }
    }

    var body: some View {
        Button(action: action) {
            Text(button.label)
                .font(.system(size: fontSize))
                .padding(padding)
        }
        .buttonStyle(ToggleButtonStyle(
            variant: variant,
            isSelected: isSelected,
            enabled: enabled
        ))
        .disabled(!enabled)
    }
}

// Custom button style for toggle buttons
private struct ToggleButtonStyle: ButtonStyle {
    let variant: ButtonVariant
    let isSelected: Bool
    let enabled: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(backgroundColor)
            .foregroundColor(foregroundColor)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(borderColor, lineWidth: variant == .outlined ? 1 : 0)
            )
            .cornerRadius(8)
            .opacity(enabled ? (configuration.isPressed ? 0.7 : 1.0) : 0.5)
    }

    private var backgroundColor: Color {
        switch variant {
        case .filled:
            return isSelected ? Color.accentColor : Color.gray.opacity(0.2)
        case .outlined:
            return isSelected ? Color.accentColor.opacity(0.2) : Color.clear
        case .text:
            return Color.clear
        case .tonal:
            return isSelected ? Color.accentColor.opacity(0.3) : Color.accentColor.opacity(0.1)
        }
    }

    private var foregroundColor: Color {
        switch variant {
        case .filled:
            return isSelected ? Color.white : Color.primary
        case .outlined:
            return isSelected ? Color.accentColor : Color.primary
        case .text:
            return isSelected ? Color.accentColor : Color.primary
        case .tonal:
            return Color.accentColor
        }
    }

    private var borderColor: Color {
        if variant == .outlined {
            return isSelected ? Color.accentColor : Color.gray.opacity(0.5)
        }
        return Color.clear
    }
}

// Data structures
public struct ToggleButtonData {
    public let value: String
    public let label: String
    public let icon: String?
    public let disabled: Bool

    public init(value: String, label: String, icon: String? = nil, disabled: Bool = false) {
        self.value = value
        self.label = label
        self.icon = icon
        self.disabled = disabled
    }
}

public enum SelectionMode {
    case single
    case multiple
}

public enum ButtonVariant {
    case filled
    case outlined
    case text
    case tonal
}

public enum ButtonSize {
    case small
    case medium
    case large
}

public enum ToggleOrientation {
    case horizontal
    case vertical
}

#if DEBUG
struct MagicToggleButtonGroupView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            // Text alignment (single selection)
            MagicToggleButtonGroupView(
                selectedValues: .constant(["center"]),
                buttons: [
                    ToggleButtonData(value: "left", label: "Left"),
                    ToggleButtonData(value: "center", label: "Center"),
                    ToggleButtonData(value: "right", label: "Right"),
                    ToggleButtonData(value: "justify", label: "Justify")
                ],
                selectionMode: .single,
                label: "Text Alignment",
                variant: .outlined,
                required: true
            )

            // Text formatting (multiple selection)
            MagicToggleButtonGroupView(
                selectedValues: .constant(["bold", "italic"]),
                buttons: [
                    ToggleButtonData(value: "bold", label: "B"),
                    ToggleButtonData(value: "italic", label: "I"),
                    ToggleButtonData(value: "underline", label: "U"),
                    ToggleButtonData(value: "strikethrough", label: "S")
                ],
                selectionMode: .multiple,
                label: "Format",
                variant: .outlined
            )

            // View mode (filled variant)
            MagicToggleButtonGroupView(
                selectedValues: .constant(["grid"]),
                buttons: [
                    ToggleButtonData(value: "list", label: "List"),
                    ToggleButtonData(value: "grid", label: "Grid"),
                    ToggleButtonData(value: "table", label: "Table")
                ],
                selectionMode: .single,
                label: "View Mode",
                variant: .filled
            )

            Spacer()
        }
        .padding()
    }
}
#endif
