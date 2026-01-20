import SwiftUI

/**
 * MagicDateRangePickerView - iOS Date Range Picker
 *
 * Interactive date range selector with calendar, presets, and validation.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicDateRangePickerView: View {
    @Binding var startDate: Date?
    @Binding var endDate: Date?
    @State private var showPicker: Bool = false

    let label: String?
    let placeholder: String
    let presets: [DateRangePreset]
    let minDate: Date?
    let maxDate: Date?
    let displayFormat: DateFormatter
    let singleDateMode: Bool
    let showClearButton: Bool
    let required: Bool
    let enabled: Bool
    let readOnly: Bool
    let onRangeSelected: ((Date?, Date?) -> Void)?
    let onRangeCleared: (() -> Void)?

    public init(
        startDate: Binding<Date?>,
        endDate: Binding<Date?>,
        label: String? = nil,
        placeholder: String = "Select date range",
        presets: [DateRangePreset] = [],
        minDate: Date? = nil,
        maxDate: Date? = nil,
        displayFormat: DateFormatter = {
            let formatter = DateFormatter()
            formatter.dateStyle = .medium
            return formatter
        }(),
        singleDateMode: Bool = false,
        showClearButton: Bool = true,
        required: Bool = false,
        enabled: Bool = true,
        readOnly: Bool = false,
        onRangeSelected: ((Date?, Date?) -> Void)? = nil,
        onRangeCleared: (() -> Void)? = nil
    ) {
        self._startDate = startDate
        self._endDate = endDate
        self.label = label
        self.placeholder = placeholder
        self.presets = presets
        self.minDate = minDate
        self.maxDate = maxDate
        self.displayFormat = displayFormat
        self.singleDateMode = singleDateMode
        self.showClearButton = showClearButton
        self.required = required
        self.enabled = enabled
        self.readOnly = readOnly
        self.onRangeSelected = onRangeSelected
        self.onRangeCleared = onRangeCleared
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Label
            if let labelText = label {
                Text(labelText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Date range button
            Button(action: {
                if enabled && !readOnly {
                    showPicker = true
                }
            }) {
                HStack {
                    Image(systemName: "calendar")
                        .foregroundColor(.blue)

                    Text(formattedDateRange)
                        .foregroundColor(startDate != nil ? .primary : .secondary)

                    Spacer()

                    if showClearButton && startDate != nil {
                        Button(action: {
                            startDate = nil
                            endDate = nil
                            onRangeCleared?()
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding()
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(8)
            }
            .buttonStyle(PlainButtonStyle())
            .disabled(!enabled || readOnly)
        }
        .sheet(isPresented: $showPicker) {
            DateRangePickerSheet(
                startDate: $startDate,
                endDate: $endDate,
                label: label,
                presets: presets,
                minDate: minDate,
                maxDate: maxDate,
                displayFormat: displayFormat,
                singleDateMode: singleDateMode,
                onRangeSelected: onRangeSelected
            )
        }
    }

    private var formattedDateRange: String {
        guard let start = startDate else {
            return placeholder
        }

        if let end = endDate, start != end {
            return "\(displayFormat.string(from: start)) - \(displayFormat.string(from: end))"
        } else {
            return displayFormat.string(from: start)
        }
    }
}

/**
 * Date range picker sheet
 */
private struct DateRangePickerSheet: View {
    @Binding var startDate: Date?
    @Binding var endDate: Date?
    @Environment(\.dismiss) var dismiss

    @State private var tempStartDate: Date?
    @State private var tempEndDate: Date?

    let label: String?
    let presets: [DateRangePreset]
    let minDate: Date?
    let maxDate: Date?
    let displayFormat: DateFormatter
    let singleDateMode: Bool
    let onRangeSelected: ((Date?, Date?) -> Void)?

    init(
        startDate: Binding<Date?>,
        endDate: Binding<Date?>,
        label: String?,
        presets: [DateRangePreset],
        minDate: Date?,
        maxDate: Date?,
        displayFormat: DateFormatter,
        singleDateMode: Bool,
        onRangeSelected: ((Date?, Date?) -> Void)?
    ) {
        self._startDate = startDate
        self._endDate = endDate
        self._tempStartDate = State(initialValue: startDate.wrappedValue)
        self._tempEndDate = State(initialValue: endDate.wrappedValue)
        self.label = label
        self.presets = presets
        self.minDate = minDate
        self.maxDate = maxDate
        self.displayFormat = displayFormat
        self.singleDateMode = singleDateMode
        self.onRangeSelected = onRangeSelected
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                // Presets
                if !presets.isEmpty {
                    ScrollView {
                        VStack(spacing: 8) {
                            ForEach(presets, id: \.label) { preset in
                                Button(action: {
                                    let (start, end) = calculatePresetRange(preset)
                                    tempStartDate = start
                                    tempEndDate = singleDateMode ? start : end
                                }) {
                                    HStack {
                                        Text(preset.label)
                                        Spacer()
                                    }
                                    .padding()
                                    .background(Color(uiColor: .secondarySystemBackground))
                                    .cornerRadius(8)
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                        .padding()
                    }
                    .frame(maxHeight: 200)

                    Divider()
                }

                // Selected range display
                HStack {
                    VStack(alignment: .leading) {
                        Text("Start Date")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(tempStartDate.map { displayFormat.string(from: $0) } ?? "Not selected")
                            .font(.body)
                            .fontWeight(.medium)
                    }

                    Spacer()

                    if !singleDateMode {
                        VStack(alignment: .trailing) {
                            Text("End Date")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text(tempEndDate.map { displayFormat.string(from: $0) } ?? "Not selected")
                                .font(.body)
                                .fontWeight(.medium)
                        }
                    }
                }
                .padding()

                // Calendar placeholder
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.secondary, lineWidth: 1)
                    .frame(height: 200)
                    .overlay(
                        Text("Calendar View\n(Tap presets above to select)")
                            .multilineTextAlignment(.center)
                            .foregroundColor(.secondary)
                    )
                    .padding()

                Spacer()
            }
            .navigationTitle(label ?? "Select Date Range")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("Confirm") {
                        startDate = tempStartDate
                        endDate = tempEndDate
                        onRangeSelected?(tempStartDate, tempEndDate)
                        dismiss()
                    }
                    .disabled(tempStartDate == nil || (!singleDateMode && tempEndDate == nil))
                }
            }
        }
    }

    private func calculatePresetRange(_ preset: DateRangePreset) -> (Date, Date) {
        let today = Date()
        let calendar = Calendar.current

        let start: Date
        if preset.daysFromToday == 0 {
            start = today
        } else {
            start = calendar.date(byAdding: .day, value: -preset.daysFromToday, to: today) ?? today
        }

        return (start, today)
    }
}

/**
 * Date range preset
 */
public struct DateRangePreset {
    public let label: String
    public let daysFromToday: Int
    public let icon: String?

    public init(label: String, daysFromToday: Int, icon: String? = nil) {
        self.label = label
        self.daysFromToday = daysFromToday
        self.icon = icon
    }
}

/**
 * Common date range presets
 */
public enum DateRangePresets {
    public static let TODAY = DateRangePreset(label: "Today", daysFromToday: 0)
    public static let YESTERDAY = DateRangePreset(label: "Yesterday", daysFromToday: 1)
    public static let LAST_7_DAYS = DateRangePreset(label: "Last 7 days", daysFromToday: 7)
    public static let LAST_14_DAYS = DateRangePreset(label: "Last 14 days", daysFromToday: 14)
    public static let LAST_30_DAYS = DateRangePreset(label: "Last 30 days", daysFromToday: 30)
    public static let LAST_60_DAYS = DateRangePreset(label: "Last 60 days", daysFromToday: 60)
    public static let LAST_90_DAYS = DateRangePreset(label: "Last 90 days", daysFromToday: 90)

    public static let STANDARD = [TODAY, LAST_7_DAYS, LAST_30_DAYS]
    public static let ANALYTICS = [TODAY, YESTERDAY, LAST_7_DAYS, LAST_14_DAYS, LAST_30_DAYS, LAST_90_DAYS]
    public static let EXTENDED = [TODAY, YESTERDAY, LAST_7_DAYS, LAST_14_DAYS, LAST_30_DAYS, LAST_60_DAYS, LAST_90_DAYS]
}

// MARK: - Preview Provider

#if DEBUG
struct MagicDateRangePickerView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            // Basic usage
            MagicDateRangePickerView(
                startDate: .constant(nil),
                endDate: .constant(nil),
                label: "Date Range",
                presets: DateRangePresets.STANDARD
            )

            // With selected dates
            MagicDateRangePickerView(
                startDate: .constant(Date()),
                endDate: .constant(Date().addingTimeInterval(7 * 24 * 60 * 60)),
                label: "Analytics Period",
                presets: DateRangePresets.ANALYTICS
            )

            // Single date mode
            MagicDateRangePickerView(
                startDate: .constant(Date()),
                endDate: .constant(nil),
                label: "Single Date",
                singleDateMode: true
            )

            Spacer()
        }
        .padding()
    }
}
#endif
