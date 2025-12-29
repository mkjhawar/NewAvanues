import SwiftUI

/**
 * MagicSwitchView - Native iOS Switch
 *
 * Native SwiftUI switch (Toggle) implementation for IDEAMagic framework.
 * Renders SwitchComponent from Core as native iOS Toggle.
 *
 * Features:
 * - Native iOS switch toggle
 * - Label support
 * - Disabled state
 * - Custom tint color
 * - onChange callback
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicSwitchView: View {
    @Binding var isOn: Bool
    let label: String?
    let tintColor: Color?
    let enabled: Bool
    let onChange: ((Bool) -> Void)?

    public init(
        isOn: Binding<Bool>,
        label: String? = nil,
        tintColor: Color? = nil,
        enabled: Bool = true,
        onChange: ((Bool) -> Void)? = nil
    ) {
        self._isOn = isOn
        self.label = label
        self.tintColor = tintColor
        self.enabled = enabled
        self.onChange = onChange
    }

    public var body: some View {
        if let labelText = label {
            Toggle(labelText, isOn: $isOn)
                .toggleStyle(SwitchToggleStyle(tint: tintColor ?? .accentColor))
                .disabled(!enabled)
                .onChange(of: isOn) { newValue in
                    onChange?(newValue)
                }
        } else {
            Toggle("", isOn: $isOn)
                .labelsHidden()
                .toggleStyle(SwitchToggleStyle(tint: tintColor ?? .accentColor))
                .disabled(!enabled)
                .onChange(of: isOn) { newValue in
                    onChange?(newValue)
                }
        }
    }
}

#if DEBUG
struct MagicSwitchView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Basic Switches").font(.headline)
                    HStack { Text("Off"); MagicSwitchView(isOn: .constant(false)) }
                    HStack { Text("On"); MagicSwitchView(isOn: .constant(true)) }
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 8) {
                    Text("With Labels").font(.headline)
                    MagicSwitchView(isOn: .constant(true), label: "Wi-Fi")
                    MagicSwitchView(isOn: .constant(false), label: "Bluetooth")
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Custom Colors").font(.headline)
                    MagicSwitchView(isOn: .constant(true), label: "Red", tintColor: .red)
                    MagicSwitchView(isOn: .constant(true), label: "Green", tintColor: .green)
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)
            }
            .padding()
        }
    }
}
#endif
