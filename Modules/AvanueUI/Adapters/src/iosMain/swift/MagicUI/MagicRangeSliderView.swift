import SwiftUI

/**
 * MagicRangeSliderView - iOS Dual-Thumb Range Slider
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicRangeSliderView: View {
    @Binding var startValue: Float
    @Binding var endValue: Float

    let min: Float
    let max: Float
    let step: Float?
    let label: String?
    let showValues: ValueDisplayMode
    let valuePrefix: String
    let valueSuffix: String
    let minGap: Float?
    let enabled: Bool
    let onRangeChanged: ((Float, Float) -> Void)?

    @State private var isDragging: Bool = false

    public init(
        startValue: Binding<Float>,
        endValue: Binding<Float>,
        min: Float = 0,
        max: Float = 100,
        step: Float? = nil,
        label: String? = nil,
        showValues: ValueDisplayMode = .always,
        valuePrefix: String = "",
        valueSuffix: String = "",
        minGap: Float? = nil,
        enabled: Bool = true,
        onRangeChanged: ((Float, Float) -> Void)? = nil
    ) {
        self._startValue = startValue
        self._endValue = endValue
        self.min = min
        self.max = max
        self.step = step
        self.label = label
        self.showValues = showValues
        self.valuePrefix = valuePrefix
        self.valueSuffix = valueSuffix
        self.minGap = minGap
        self.enabled = enabled
        self.onRangeChanged = onRangeChanged
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Label
            if let labelText = label {
                Text(labelText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Value labels
            if showValues == .always || (showValues == .onDrag && isDragging) {
                HStack {
                    Text(formatValue(startValue))
                        .font(.body)
                        .fontWeight(.medium)
                        .foregroundColor(.blue)

                    Spacer()

                    Text(formatValue(endValue))
                        .font(.body)
                        .fontWeight(.medium)
                        .foregroundColor(.blue)
                }
            }

            // Range slider (using two sliders as iOS doesn't have native range slider)
            VStack(spacing: 4) {
                // Start value slider
                HStack {
                    Text("Min")
                        .font(.caption2)
                        .foregroundColor(.secondary)

                    Slider(
                        value: Binding(
                            get: { startValue },
                            set: { newValue in
                                let constrained = constrainStart(newValue)
                                startValue = constrained
                                isDragging = true
                            }
                        ),
                        in: min...max,
                        step: step ?? 1,
                        onEditingChanged: { editing in
                            if !editing {
                                isDragging = false
                                onRangeChanged?(startValue, endValue)
                            }
                        }
                    )
                    .disabled(!enabled)
                }

                // End value slider
                HStack {
                    Text("Max")
                        .font(.caption2)
                        .foregroundColor(.secondary)

                    Slider(
                        value: Binding(
                            get: { endValue },
                            set: { newValue in
                                let constrained = constrainEnd(newValue)
                                endValue = constrained
                                isDragging = true
                            }
                        ),
                        in: min...max,
                        step: step ?? 1,
                        onEditingChanged: { editing in
                            if !editing {
                                isDragging = false
                                onRangeChanged?(startValue, endValue)
                            }
                        }
                    )
                    .disabled(!enabled)
                }
            }

            // Min/max labels
            HStack {
                Text(formatValue(min))
                    .font(.caption)
                    .foregroundColor(.secondary)

                Spacer()

                Text(formatValue(max))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }

    private func formatValue(_ value: Float) -> String {
        let formattedNumber: String
        if value.truncatingRemainder(dividingBy: 1) == 0 {
            formattedNumber = String(Int(value))
        } else {
            formattedNumber = String(format: "%.1f", value)
        }
        return "\(valuePrefix)\(formattedNumber)\(valueSuffix)"
    }

    private func constrainStart(_ newValue: Float) -> Float {
        var constrained = newValue

        // Apply min gap
        if let gap = minGap {
            constrained = min(constrained, endValue - gap)
        } else {
            constrained = min(constrained, endValue)
        }

        return max(min, min(max, constrained))
    }

    private func constrainEnd(_ newValue: Float) -> Float {
        var constrained = newValue

        // Apply min gap
        if let gap = minGap {
            constrained = max(constrained, startValue + gap)
        } else {
            constrained = max(constrained, startValue)
        }

        return max(min, min(max, constrained))
    }
}

public enum ValueDisplayMode {
    case always
    case onDrag
    case never
}

#if DEBUG
struct MagicRangeSliderView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 30) {
            MagicRangeSliderView(
                startValue: .constant(20),
                endValue: .constant(80),
                min: 0,
                max: 100,
                step: 5,
                label: "Price Range",
                valuePrefix: "$"
            )

            MagicRangeSliderView(
                startValue: .constant(18),
                endValue: .constant(65),
                min: 0,
                max: 100,
                step: 1,
                label: "Age Range",
                valueSuffix: " yrs"
            )

            MagicRangeSliderView(
                startValue: .constant(25),
                endValue: .constant(75),
                min: 0,
                max: 100,
                step: 5,
                label: "Percentage",
                valueSuffix: "%"
            )

            Spacer()
        }
        .padding()
    }
}
#endif
