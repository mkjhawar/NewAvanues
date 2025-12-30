import SwiftUI

/**
 * MagicTimePickerView - Native iOS Time Picker
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicTimePickerView: View {
    @Binding var date: Date
    let label: String?

    public init(date: Binding<Date>, label: String? = nil) {
        self._date = date
        self.label = label
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let labelText = label {
                Text(labelText).font(.caption).foregroundColor(.secondary)
            }
            DatePicker("", selection: $date, displayedComponents: [.hourAndMinute])
                .labelsHidden()
        }
    }
}
