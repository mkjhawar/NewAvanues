import SwiftUI

/**
 * MagicSliderView - Native iOS Slider
 *
 * Native SwiftUI slider implementation for IDEAMagic framework.
 * Renders SliderComponent from Core as native iOS Slider.
 *
 * Features:
 * - Value range with min/max
 * - Step increments
 * - Label support
 * - Value display
 * - Custom tint color
 * - onChange callback
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicSliderView: View {
    @Binding var value: Double
    let range: ClosedRange<Double>
    let step: Double?
    let label: String?
    let showValue: Bool
    let tintColor: Color?
    let enabled: Bool
    let onChange: ((Double) -> Void)?

    public init(
        value: Binding<Double>,
        range: ClosedRange<Double> = 0...100,
        step: Double? = nil,
        label: String? = nil,
        showValue: Bool = false,
        tintColor: Color? = nil,
        enabled: Bool = true,
        onChange: ((Double) -> Void)? = nil
    ) {
        self._value = value
        self.range = range
        self.step = step
        self.label = label
        self.showValue = showValue
        self.tintColor = tintColor
        self.enabled = enabled
        self.onChange = onChange
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let labelText = label {
                HStack {
                    Text(labelText)
                        .font(.subheadline)
                        .foregroundColor(.primary)

                    if showValue {
                        Spacer()
                        Text(formattedValue)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
            }

            if let stepValue = step {
                Slider(
                    value: $value,
                    in: range,
                    step: stepValue,
                    onEditingChanged: { _ in
                        onChange?(value)
                    }
                )
                .accentColor(tintColor ?? .accentColor)
                .disabled(!enabled)
            } else {
                Slider(
                    value: $value,
                    in: range,
                    onEditingChanged: { _ in
                        onChange?(value)
                    }
                )
                .accentColor(tintColor ?? .accentColor)
                .disabled(!enabled)
            }
        }
        .opacity(enabled ? 1.0 : 0.6)
    }

    private var formattedValue: String {
        if let stepValue = step, stepValue >= 1 {
            return String(format: "%.0f", value)
        } else {
            return String(format: "%.1f", value)
        }
    }
}

#if DEBUG
struct MagicSliderView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Basic Slider").font(.headline)
                    MagicSliderView(value: .constant(50))
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 8) {
                    Text("With Label").font(.headline)
                    MagicSliderView(
                        value: .constant(75),
                        label: "Volume"
                    )
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 8) {
                    Text("With Value Display").font(.headline)
                    MagicSliderView(
                        value: .constant(60),
                        label: "Brightness",
                        showValue: true
                    )
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Custom Range").font(.headline)
                    MagicSliderView(
                        value: .constant(25),
                        range: 0...50,
                        label: "Temperature",
                        showValue: true
                    )
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 8) {
                    Text("With Steps").font(.headline)
                    MagicSliderView(
                        value: .constant(5),
                        range: 0...10,
                        step: 1,
                        label: "Rating",
                        showValue: true
                    )
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Custom Color").font(.headline)
                    MagicSliderView(
                        value: .constant(80),
                        label: "Red",
                        showValue: true,
                        tintColor: .red
                    )
                    MagicSliderView(
                        value: .constant(60),
                        label: "Green",
                        showValue: true,
                        tintColor: .green
                    )
                    MagicSliderView(
                        value: .constant(40),
                        label: "Blue",
                        showValue: true,
                        tintColor: .blue
                    )
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
