import SwiftUI

/**
 * MagicRadioView - iOS Radio Group (Picker)
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicRadioView<T: Hashable>: View {
    @Binding var selection: T
    let options: [RadioOption<T>]
    let label: String?

    public init(selection: Binding<T>, options: [RadioOption<T>], label: String? = nil) {
        self._selection = selection
        self.options = options
        self.label = label
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let labelText = label {
                Text(labelText).font(.subheadline).foregroundColor(.secondary)
            }

            ForEach(options, id: \.value) { option in
                Button(action: { selection = option.value }) {
                    HStack {
                        Image(systemName: selection == option.value ? "circle.circle.fill" : "circle")
                            .foregroundColor(selection == option.value ? .accentColor : .secondary)
                        Text(option.label)
                        Spacer()
                    }
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
    }
}

public struct RadioOption<T: Hashable> {
    let value: T
    let label: String

    public init(value: T, label: String) {
        self.value = value
        self.label = label
    }
}
