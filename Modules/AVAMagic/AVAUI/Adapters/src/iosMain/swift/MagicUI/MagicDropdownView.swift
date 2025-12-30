import SwiftUI

/**
 * MagicDropdownView - iOS Dropdown (Menu/Picker)
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicDropdownView<T: Hashable>: View {
    @Binding var selection: T
    let options: [DropdownOption<T>]
    let label: String?

    public init(selection: Binding<T>, options: [DropdownOption<T>], label: String? = nil) {
        self._selection = selection
        self.options = options
        self.label = label
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let labelText = label {
                Text(labelText).font(.caption).foregroundColor(.secondary)
            }

            Menu {
                ForEach(options, id: \.value) { option in
                    Button(option.label) {
                        selection = option.value
                    }
                }
            } label: {
                HStack {
                    Text(selectedLabel)
                    Spacer()
                    Image(systemName: "chevron.down")
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(8)
            }
        }
    }

    private var selectedLabel: String {
        options.first(where: { $0.value == selection })?.label ?? ""
    }
}

public struct DropdownOption<T: Hashable> {
    let value: T
    let label: String

    public init(value: T, label: String) {
        self.value = value
        self.label = label
    }
}
