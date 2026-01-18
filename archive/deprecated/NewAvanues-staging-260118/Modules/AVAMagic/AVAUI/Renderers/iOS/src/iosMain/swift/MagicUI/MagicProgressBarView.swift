import SwiftUI

/**
 * MagicProgressBarView - Native iOS ProgressView
 *
 * Native SwiftUI progress bar implementation for IDEAMagic framework.
 * Renders ProgressBarComponent from Core as native iOS ProgressView.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicProgressBarView: View {
    let value: Double?
    let total: Double
    let label: String?
    let tintColor: Color?

    public init(
        value: Double? = nil,
        total: Double = 1.0,
        label: String? = nil,
        tintColor: Color? = nil
    ) {
        self.value = value
        self.total = total
        self.label = label
        self.tintColor = tintColor
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let labelText = label {
                HStack {
                    Text(labelText).font(.subheadline)
                    Spacer()
                    if let val = value {
                        Text("\(Int((val/total) * 100))%")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }

            if let val = value {
                ProgressView(value: val, total: total)
                    .tint(tintColor ?? .accentColor)
            } else {
                ProgressView()
                    .tint(tintColor ?? .accentColor)
            }
        }
    }
}

#if DEBUG
struct MagicProgressBarView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 24) {
            MagicProgressBarView(value: 0.3, label: "Loading...")
            MagicProgressBarView(value: 0.7, label: "Progress", tintColor: .green)
            MagicProgressBarView(label: "Indeterminate")
        }
        .padding()
    }
}
#endif
