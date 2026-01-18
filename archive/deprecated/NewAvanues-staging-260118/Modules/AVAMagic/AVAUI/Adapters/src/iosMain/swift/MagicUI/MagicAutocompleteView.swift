import SwiftUI

/**
 * MagicAutocompleteView - iOS Autocomplete/Typeahead Input
 *
 * Text field with dropdown suggestions that filter as user types.
 * Features fuzzy matching, keyboard navigation, and async loading support.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicAutocompleteView: View {
    @Binding var value: String
    @State private var filteredSuggestions: [String] = []
    @State private var showSuggestions: Bool = false

    let suggestions: [String]
    let placeholder: String
    let label: String?
    let leadingIcon: String?
    let trailingIcon: String?
    let minCharsForSuggestions: Int
    let maxSuggestions: Int
    let filterStrategy: FilterStrategy
    let fuzzyThreshold: Float
    let isLoading: Bool
    let emptyStateMessage: String
    let highlightMatch: Bool
    let enabled: Bool
    let readOnly: Bool
    let onSuggestionSelected: ((String) -> Void)?

    public init(
        value: Binding<String>,
        suggestions: [String],
        placeholder: String = "Type to search...",
        label: String? = nil,
        leadingIcon: String? = nil,
        trailingIcon: String? = nil,
        minCharsForSuggestions: Int = 1,
        maxSuggestions: Int = 5,
        filterStrategy: FilterStrategy = .contains,
        fuzzyThreshold: Float = 0.6,
        isLoading: Bool = false,
        emptyStateMessage: String = "No suggestions found",
        highlightMatch: Bool = true,
        enabled: Bool = true,
        readOnly: Bool = false,
        onSuggestionSelected: ((String) -> Void)? = nil
    ) {
        self._value = value
        self.suggestions = suggestions
        self.placeholder = placeholder
        self.label = label
        self.leadingIcon = leadingIcon
        self.trailingIcon = trailingIcon
        self.minCharsForSuggestions = minCharsForSuggestions
        self.maxSuggestions = maxSuggestions
        self.filterStrategy = filterStrategy
        self.fuzzyThreshold = fuzzyThreshold
        self.isLoading = isLoading
        self.emptyStateMessage = emptyStateMessage
        self.highlightMatch = highlightMatch
        self.enabled = enabled
        self.readOnly = readOnly
        self.onSuggestionSelected = onSuggestionSelected
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Label
            if let labelText = label {
                Text(labelText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Text field with icons
            HStack {
                if let icon = leadingIcon {
                    Image(systemName: icon)
                        .foregroundColor(.secondary)
                }

                TextField(placeholder, text: $value)
                    .disabled(!enabled || readOnly)
                    .onChange(of: value) { newValue in
                        updateSuggestions(query: newValue)
                    }

                if isLoading {
                    ProgressView()
                        .scaleEffect(0.7)
                } else if let icon = trailingIcon {
                    Image(systemName: icon)
                        .foregroundColor(.secondary)
                }
            }
            .padding(8)
            .background(Color(uiColor: .secondarySystemBackground))
            .cornerRadius(8)

            // Suggestions dropdown
            if showSuggestions {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        if filteredSuggestions.isEmpty && !isLoading {
                            // Empty state
                            Text(emptyStateMessage)
                                .font(.body)
                                .foregroundColor(.secondary)
                                .padding()
                        } else {
                            ForEach(filteredSuggestions, id: \.self) { suggestion in
                                Button(action: {
                                    selectSuggestion(suggestion)
                                }) {
                                    HStack {
                                        Text(suggestion)
                                            .font(.body)
                                            .foregroundColor(.primary)
                                        Spacer()
                                    }
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 10)
                                    .background(Color.clear)
                                }
                                .buttonStyle(PlainButtonStyle())

                                if suggestion != filteredSuggestions.last {
                                    Divider()
                                }
                            }
                        }
                    }
                }
                .frame(maxHeight: 200)
                .background(Color(uiColor: .systemBackground))
                .cornerRadius(8)
                .shadow(radius: 4)
            }
        }
        .onAppear {
            updateSuggestions(query: value)
        }
    }

    // MARK: - Private Methods

    private func updateSuggestions(query: String) {
        if query.count < minCharsForSuggestions {
            filteredSuggestions = []
            showSuggestions = false
            return
        }

        let filtered = filterSuggestions(query: query)
        filteredSuggestions = Array(filtered.prefix(maxSuggestions))
        showSuggestions = !filteredSuggestions.isEmpty
    }

    private func filterSuggestions(query: String) -> [String] {
        let lowerQuery = query.lowercased()

        switch filterStrategy {
        case .contains:
            return suggestions.filter { $0.lowercased().contains(lowerQuery) }

        case .startsWith:
            return suggestions.filter { $0.lowercased().hasPrefix(lowerQuery) }

        case .fuzzy:
            return suggestions
                .map { (suggestion: $0, score: fuzzyMatch(query: lowerQuery, target: $0.lowercased())) }
                .filter { $0.score >= fuzzyThreshold }
                .sorted { $0.score > $1.score }
                .map { $0.suggestion }
        }
    }

    private func fuzzyMatch(query: String, target: String) -> Float {
        if query.isEmpty { return 1.0 }
        if target.isEmpty { return 0.0 }
        if target.contains(query) { return 1.0 }

        var queryIndex = query.startIndex
        var targetIndex = target.startIndex
        var matches = 0

        while queryIndex < query.endIndex && targetIndex < target.endIndex {
            if query[queryIndex] == target[targetIndex] {
                matches += 1
                queryIndex = query.index(after: queryIndex)
            }
            targetIndex = target.index(after: targetIndex)
        }

        if queryIndex == query.endIndex {
            return Float(matches) / Float(target.count)
        } else {
            return 0.0
        }
    }

    private func selectSuggestion(_ suggestion: String) {
        value = suggestion
        showSuggestions = false
        onSuggestionSelected?(suggestion)
    }
}

/**
 * Filter strategy enum
 */
public enum FilterStrategy {
    case contains
    case startsWith
    case fuzzy
}

// MARK: - Preview Provider

#if DEBUG
struct MagicAutocompleteView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            // Basic autocomplete
            MagicAutocompleteView(
                value: .constant("App"),
                suggestions: ["Apple", "Apricot", "Banana", "Cherry", "Date"],
                placeholder: "Search fruits...",
                label: "Fruit"
            )

            // With icons
            MagicAutocompleteView(
                value: .constant(""),
                suggestions: ["United States", "United Kingdom", "Canada", "Australia"],
                placeholder: "Search countries...",
                label: "Country",
                leadingIcon: "magnifyingglass",
                filterStrategy: .startsWith
            )

            // Loading state
            MagicAutocompleteView(
                value: .constant("Lon"),
                suggestions: [],
                placeholder: "Search cities...",
                label: "City",
                isLoading: true
            )

            Spacer()
        }
        .padding()
    }
}
#endif
