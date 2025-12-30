import SwiftUI

/**
 * MagicMultiSelectView - iOS Multi-Selection Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicMultiSelectView: View {
    @Binding var selectedValues: [String]

    let options: [SelectOption]
    let label: String?
    let placeholder: String
    let searchable: Bool
    let showSelectAll: Bool
    let maxSelections: Int?
    let showSelectedChips: Bool
    let enabled: Bool
    let readOnly: Bool
    let onSelectionChanged: (([String]) -> Void)?

    @State private var searchQuery: String = ""

    public init(
        selectedValues: Binding<[String]>,
        options: [SelectOption],
        label: String? = nil,
        placeholder: String = "Select items",
        searchable: Bool = false,
        showSelectAll: Bool = false,
        maxSelections: Int? = nil,
        showSelectedChips: Bool = true,
        enabled: Bool = true,
        readOnly: Bool = false,
        onSelectionChanged: (([String]) -> Void)? = nil
    ) {
        self._selectedValues = selectedValues
        self.options = options
        self.label = label
        self.placeholder = placeholder
        self.searchable = searchable
        self.showSelectAll = showSelectAll
        self.maxSelections = maxSelections
        self.showSelectedChips = showSelectedChips
        self.enabled = enabled
        self.readOnly = readOnly
        self.onSelectionChanged = onSelectionChanged
    }

    private var filteredOptions: [SelectOption] {
        if searchQuery.isEmpty {
            return options
        }
        return options.filter {
            $0.label.localizedCaseInsensitiveContains(searchQuery) ||
            ($0.description?.localizedCaseInsensitiveContains(searchQuery) ?? false)
        }
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Label
            if let labelText = label {
                Text(labelText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Search bar
            if searchable {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("Search", text: $searchQuery)
                    if !searchQuery.isEmpty {
                        Button(action: { searchQuery = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(8)
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(8)
            }

            // Select all / Deselect all
            if showSelectAll {
                HStack {
                    Button("Select All") {
                        let enabledOptions = options.filter { !$0.disabled }.map { $0.value }
                        selectedValues = maxSelections.map { Array(enabledOptions.prefix($0)) } ?? enabledOptions
                        onSelectionChanged?(selectedValues)
                    }
                    .disabled(!enabled || readOnly)

                    Spacer()

                    Button("Deselect All") {
                        selectedValues = []
                        onSelectionChanged?([])
                    }
                    .disabled(!enabled || readOnly || selectedValues.isEmpty)
                }
                .font(.subheadline)
            }

            // Selected chips
            if showSelectedChips && !selectedValues.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(selectedValues, id: \.self) { value in
                            if let option = options.first(where: { $0.value == value }) {
                                HStack(spacing: 4) {
                                    Text(option.label)
                                        .font(.caption)
                                    Button(action: {
                                        selectedValues.removeAll { $0 == value }
                                        onSelectionChanged?(selectedValues)
                                    }) {
                                        Image(systemName: "xmark")
                                            .font(.caption2)
                                    }
                                }
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.blue.opacity(0.2))
                                .cornerRadius(12)
                            }
                        }
                    }
                }
            }

            // Options list
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(filteredOptions, id: \.value) { option in
                        MultiSelectRow(
                            option: option,
                            isSelected: selectedValues.contains(option.value),
                            enabled: enabled && !readOnly && !option.disabled,
                            onTap: {
                                toggleSelection(option)
                            }
                        )
                        if option.value != filteredOptions.last?.value {
                            Divider()
                        }
                    }

                    if filteredOptions.isEmpty {
                        Text("No options found")
                            .foregroundColor(.secondary)
                            .padding()
                    }
                }
            }
            .frame(maxHeight: 300)
            .background(Color(uiColor: .secondarySystemBackground))
            .cornerRadius(8)

            // Selection count
            if let max = maxSelections {
                Text("\(selectedValues.count) / \(max) selected")
                    .font(.caption)
                    .foregroundColor(.secondary)
            } else if !selectedValues.isEmpty {
                Text("\(selectedValues.count) selected")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }

    private func toggleSelection(_ option: SelectOption) {
        if selectedValues.contains(option.value) {
            selectedValues.removeAll { $0 == option.value }
        } else {
            if let max = maxSelections, selectedValues.count >= max {
                return // Max reached
            }
            selectedValues.append(option.value)
        }
        onSelectionChanged?(selectedValues)
    }
}

private struct MultiSelectRow: View {
    let option: SelectOption
    let isSelected: Bool
    let enabled: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .blue : .secondary)

                VStack(alignment: .leading, spacing: 2) {
                    Text(option.label)
                        .font(.body)
                        .fontWeight(isSelected ? .medium : .regular)
                        .foregroundColor(enabled ? .primary : .secondary)

                    if let description = option.description {
                        Text(description)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 12)
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(!enabled)
    }
}

public struct SelectOption {
    public let value: String
    public let label: String
    public let group: String?
    public let icon: String?
    public let description: String?
    public let disabled: Bool

    public init(
        value: String,
        label: String,
        group: String? = nil,
        icon: String? = nil,
        description: String? = nil,
        disabled: Bool = false
    ) {
        self.value = value
        self.label = label
        self.group = group
        self.icon = icon
        self.description = description
        self.disabled = disabled
    }
}

#if DEBUG
struct MagicMultiSelectView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            MagicMultiSelectView(
                selectedValues: .constant(["1", "3"]),
                options: [
                    SelectOption(value: "1", label: "Apple", description: "A sweet fruit"),
                    SelectOption(value: "2", label: "Banana", description: "Yellow fruit"),
                    SelectOption(value: "3", label: "Cherry", description: "Red fruit"),
                    SelectOption(value: "4", label: "Date", description: "Sweet dried fruit")
                ],
                label: "Select fruits",
                searchable: true,
                showSelectAll: true
            )

            Spacer()
        }
        .padding()
    }
}
#endif
