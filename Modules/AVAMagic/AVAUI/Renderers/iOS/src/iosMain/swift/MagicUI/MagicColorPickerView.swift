import SwiftUI

/**
 * MagicColorPickerView - iOS Color Picker
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicColorPickerView: View {
    @Binding var value: String
    @State private var selectedColor: Color
    @State private var showPicker: Bool = false

    let label: String?
    let mode: ColorPickerModeType
    let showAlpha: Boolean
    let showHexInput: Boolean
    let showPresets: Boolean
    let showRecent: Boolean
    let presetColors: [String]
    let recentColors: [String]
    let placeholder: String
    let helperText: String?
    let errorText: String?
    let enabled: Boolean
    let readOnly: Boolean
    let onColorChanged: ((String) -> Void)?

    public init(
        value: Binding<String>,
        label: String? = nil,
        mode: ColorPickerModeType = .full,
        showAlpha: Bool = false,
        showHexInput: Bool = true,
        showPresets: Bool = true,
        showRecent: Bool = false,
        presetColors: [String] = [],
        recentColors: [String] = [],
        placeholder: String = "#000000",
        helperText: String? = nil,
        errorText: String? = nil,
        enabled: Bool = true,
        readOnly: Bool = false,
        onColorChanged: ((String) -> Void)? = nil
    ) {
        self._value = value
        self._selectedColor = State(initialValue: Color(hex: value.wrappedValue) ?? .black)
        self.label = label
        self.mode = mode
        self.showAlpha = showAlpha
        self.showHexInput = showHexInput
        self.showPresets = showPresets
        self.showRecent = showRecent
        self.presetColors = presetColors
        self.recentColors = recentColors
        self.placeholder = placeholder
        self.helperText = helperText
        self.errorText = errorText
        self.enabled = enabled
        self.readOnly = readOnly
        self.onColorChanged = onColorChanged
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Label
            if let labelText = label {
                Text(labelText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            switch mode {
            case .presetsOnly:
                // Show preset grid directly
                if showPresets && !presetColors.isEmpty {
                    ColorPresetGrid(
                        presetColors: presetColors,
                        selectedColor: value,
                        onColorSelected: { color in
                            value = color
                            selectedColor = Color(hex: color) ?? .black
                            onColorChanged?(color)
                        },
                        enabled: enabled
                    )
                }

            case .hexOnly:
                // Show hex input field
                HStack {
                    Circle()
                        .fill(selectedColor)
                        .frame(width: 32, height: 32)
                        .overlay(
                            Circle()
                                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                        )

                    TextField(placeholder, text: $value)
                        .textFieldStyle(.roundedBorder)
                        .disabled(!enabled || readOnly)
                        .onChange(of: value) { newValue in
                            if let color = Color(hex: newValue) {
                                selectedColor = color
                                onColorChanged?(newValue)
                            }
                        }
                }

                if let helper = helperText {
                    Text(helper)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                if let error = errorText {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                }

            default:
                // Show color preview button that opens picker
                Button(action: {
                    if enabled && !readOnly {
                        showPicker = true
                    }
                }) {
                    HStack {
                        Circle()
                            .fill(selectedColor)
                            .frame(width: 24, height: 24)
                            .overlay(
                                Circle()
                                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                            )

                        Text(value)
                            .foregroundColor(.primary)

                        Spacer()
                    }
                    .padding(12)
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(8)
                }
                .disabled(!enabled || readOnly)

                if let helper = helperText {
                    Text(helper)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.leading, 16)
                }
                if let error = errorText {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                        .padding(.leading, 16)
                }
            }

            // Recent colors (if enabled)
            if showRecent && !recentColors.isEmpty {
                Text("Recent")
                    .font(.caption)
                    .foregroundColor(.secondary)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(recentColors.prefix(10), id: \.self) { colorHex in
                            Circle()
                                .fill(Color(hex: colorHex) ?? .black)
                                .frame(width: 32, height: 32)
                                .overlay(
                                    Circle()
                                        .stroke(
                                            colorHex == value ? Color.accentColor : Color.gray.opacity(0.3),
                                            lineWidth: colorHex == value ? 2 : 1
                                        )
                                )
                                .onTapGesture {
                                    if enabled && !readOnly {
                                        value = colorHex
                                        selectedColor = Color(hex: colorHex) ?? .black
                                        onColorChanged?(colorHex)
                                    }
                                }
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $showPicker) {
            NavigationView {
                VStack(spacing: 16) {
                    // Color preview
                    RoundedRectangle(cornerRadius: 8)
                        .fill(selectedColor)
                        .frame(height: 80)
                        .overlay(
                            Text(value)
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(isLightColor(selectedColor) ? .black : .white)
                        )
                        .padding()

                    // Native ColorPicker
                    if #available(iOS 14.0, *) {
                        ColorPicker("Color", selection: $selectedColor, supportsOpacity: showAlpha)
                            .padding(.horizontal)
                            .onChange(of: selectedColor) { newColor in
                                value = newColor.toHex()
                                onColorChanged?(value)
                            }
                    }

                    // Hex input (if enabled)
                    if showHexInput {
                        HStack {
                            Text("Hex")
                            TextField("#000000", text: $value)
                                .textFieldStyle(.roundedBorder)
                                .onChange(of: value) { newValue in
                                    if let color = Color(hex: newValue) {
                                        selectedColor = color
                                        onColorChanged?(newValue)
                                    }
                                }
                        }
                        .padding(.horizontal)
                    }

                    // Preset colors (if enabled)
                    if showPresets && !presetColors.isEmpty {
                        VStack(alignment: .leading) {
                            Text("Presets")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .padding(.horizontal)

                            ScrollView {
                                ColorPresetGrid(
                                    presetColors: presetColors,
                                    selectedColor: value,
                                    onColorSelected: { color in
                                        value = color
                                        selectedColor = Color(hex: color) ?? .black
                                        onColorChanged?(color)
                                    },
                                    enabled: true
                                )
                                .padding(.horizontal)
                            }
                            .frame(height: 200)
                        }
                    }

                    Spacer()
                }
                .navigationTitle(label ?? "Choose Color")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("Done") {
                            showPicker = false
                        }
                    }
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("Cancel") {
                            showPicker = false
                        }
                    }
                }
            }
        }
    }

    private func isLightColor(_ color: Color) -> Bool {
        guard let components = UIColor(color).cgColor.components else { return false }
        let luminance = 0.299 * components[0] + 0.587 * components[1] + 0.114 * components[2]
        return luminance > 0.5
    }
}

// Color preset grid
private struct ColorPresetGrid: View {
    let presetColors: [String]
    let selectedColor: String
    let onColorSelected: (String) -> Void
    let enabled: Bool

    let columns = [
        GridItem(.adaptive(minimum: 40), spacing: 4)
    ]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 4) {
            ForEach(presetColors, id: \.self) { colorHex in
                Circle()
                    .fill(Color(hex: colorHex) ?? .black)
                    .frame(width: 40, height: 40)
                    .overlay(
                        Circle()
                            .stroke(
                                colorHex == selectedColor ? Color.accentColor : Color.gray.opacity(0.3),
                                lineWidth: colorHex == selectedColor ? 3 : 1
                            )
                    )
                    .onTapGesture {
                        if enabled {
                            onColorSelected(colorHex)
                        }
                    }
            }
        }
    }
}

// Color picker mode enum
public enum ColorPickerModeType {
    case full
    case compact
    case presetsOnly
    case hexOnly
    case wheel
    case hsvSliders
    case rgbSliders
}

// Color extension for hex conversion
extension Color {
    init?(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6: // RGB
            (r, g, b, a) = ((int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF, 255)
        case 8: // RGBA
            (r, g, b, a) = ((int >> 24) & 0xFF, (int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        default:
            return nil
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }

    func toHex() -> String {
        guard let components = UIColor(self).cgColor.components else {
            return "#000000"
        }

        let r = Int(components[0] * 255.0)
        let g = Int(components[1] * 255.0)
        let b = Int(components[2] * 255.0)
        let a = components.count > 3 ? Int(components[3] * 255.0) : 255

        if a < 255 {
            return String(format: "#%02X%02X%02X%02X", r, g, b, a)
        } else {
            return String(format: "#%02X%02X%02X", r, g, b)
        }
    }
}

#if DEBUG
struct MagicColorPickerView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            // Full picker
            MagicColorPickerView(
                value: .constant("#FF0000"),
                label: "Choose Color",
                mode: .full,
                showPresets: true,
                presetColors: [
                    "#FF0000", "#00FF00", "#0000FF", "#FFFF00",
                    "#FF00FF", "#00FFFF", "#FFFFFF", "#000000"
                ]
            )

            // Hex only
            MagicColorPickerView(
                value: .constant("#00FF00"),
                label: "Hex Color",
                mode: .hexOnly
            )

            // Presets only
            MagicColorPickerView(
                value: .constant("#0000FF"),
                label: "Presets",
                mode: .presetsOnly,
                presetColors: [
                    "#FF0000", "#00FF00", "#0000FF", "#FFFF00",
                    "#FF00FF", "#00FFFF", "#FFFFFF", "#000000"
                ]
            )

            Spacer()
        }
        .padding()
    }
}
#endif
