import SwiftUI

/**
 * MagicDatePickerView - Native iOS DatePicker
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicDatePickerView: View {
    @Binding var date: Date
    let label: String?
    let displayedComponents: DatePickerComponents

    public init(date: Binding<Date>, label: String? = nil, displayedComponents: DatePickerComponents = [.date]) {
        self._date = date
        self.label = label
        self.displayedComponents = displayedComponents
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let labelText = label {
                Text(labelText).font(.caption).foregroundColor(.secondary)
            }
            DatePicker("", selection: $date, displayedComponents: displayedComponents)
                .labelsHidden()
        }
    }
}
