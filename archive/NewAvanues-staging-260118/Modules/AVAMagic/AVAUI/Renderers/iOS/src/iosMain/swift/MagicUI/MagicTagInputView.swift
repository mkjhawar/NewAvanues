import SwiftUI

/**
 * MagicTagInputView - iOS Tag/Chip Input
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicTagInputView: View {
    @Binding var tags: [String]
    @State private var inputText: String = ""
    @State private var showSuggestions: Bool = false

    let suggestions: [String]
    let label: String?
    let placeholder: String
    let maxTags: Int?
    let allowDuplicates: Bool
    let caseSensitive: Bool
    let separators: [String]
    let minTagLength: Int
    let maxTagLength: Int?
    let showSuggestionList: Bool
    let enabled: Bool
    let readOnly: Bool
    let onTagAdded: ((String) -> Void)?
    let onTagRemoved: ((String) -> Void)?

    public init(
        tags: Binding<[String]>,
        suggestions: [String] = [],
        label: String? = nil,
        placeholder: String = "Add tag...",
        maxTags: Int? = nil,
        allowDuplicates: Bool = false,
        caseSensitive: Bool = false,
        separators: [String] = [",", ";"],
        minTagLength: Int = 1,
        maxTagLength: Int? = nil,
        showSuggestionList: Bool = true,
        enabled: Bool = true,
        readOnly: Bool = false,
        onTagAdded: ((String) -> Void)? = nil,
        onTagRemoved: ((String) -> Void)? = nil
    ) {
        self._tags = tags
        self.suggestions = suggestions
        self.label = label
        self.placeholder = placeholder
        self.maxTags = maxTags
        self.allowDuplicates = allowDuplicates
        self.caseSensitive = caseSensitive
        self.separators = separators
        self.minTagLength = minTagLength
        self.maxTagLength = maxTagLength
        self.showSuggestionList = showSuggestionList
        self.enabled = enabled
        self.readOnly = readOnly
        self.onTagAdded = onTagAdded
        self.onTagRemoved = onTagRemoved
    }

    private var filteredSuggestions: [String] {
        guard !inputText.isEmpty, showSuggestionList else { return [] }
        return suggestions.filter { suggestion in
            suggestion.localizedCaseInsensitiveContains(inputText) &&
            !tags.contains(caseSensitive ? suggestion : suggestion.lowercased())
        }.prefix(5).map { $0 }
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Label
            if let labelText = label {
                Text(labelText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Tags display
            if !tags.isEmpty {
                FlowLayout(spacing: 4) {
                    ForEach(tags, id: \.self) { tag in
                        HStack(spacing: 4) {
                            Text(tag)
                                .font(.subheadline)
                            if !readOnly && enabled {
                                Button(action: {
                                    removeTag(tag)
                                }) {
                                    Image(systemName: "xmark")
                                        .font(.caption2)
                                }
                            }
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.blue.opacity(0.2))
                        .cornerRadius(12)
                    }
                }
            }

            // Input field
            TextField(placeholderText, text: $inputText)
                .textFieldStyle(.roundedBorder)
                .disabled(!enabled || readOnly || (maxTags != nil && tags.count >= maxTags!))
                .onChange(of: inputText) { newValue in
                    // Check for separators
                    for separator in separators {
                        if newValue.hasSuffix(separator) {
                            let tagText = newValue.replacingOccurrences(of: separator, with: "").trimmingCharacters(in: .whitespaces)
                            addTag(tagText)
                            inputText = ""
                            return
                        }
                    }
                    showSuggestions = !newValue.isEmpty
                }
                .onSubmit {
                    if !inputText.trimmingCharacters(in: .whitespaces).isEmpty {
                        addTag(inputText.trimmingCharacters(in: .whitespaces))
                        inputText = ""
                    }
                }

            // Tag count
            if let max = maxTags {
                Text("\(tags.count) / \(max) tags")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Suggestions
            if showSuggestions && !filteredSuggestions.isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(filteredSuggestions, id: \.self) { suggestion in
                        Button(action: {
                            addTag(suggestion)
                            inputText = ""
                            showSuggestions = false
                        }) {
                            Text(suggestion)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(8)
                        }
                        .buttonStyle(PlainButtonStyle())
                        Divider()
                    }
                }
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(8)
                .frame(maxHeight: 150)
            }
        }
    }

    private var placeholderText: String {
        if let max = maxTags, tags.count >= max {
            return "Max tags reached (\(max))"
        }
        return placeholder
    }

    private func addTag(_ tag: String) {
        guard !tag.isEmpty else { return }
        guard tag.count >= minTagLength else { return }
        if let max = maxTagLength, tag.count > max { return }
        if let max = maxTags, tags.count >= max { return }

        let normalizedTag = caseSensitive ? tag : tag.lowercased()
        let normalizedExisting = caseSensitive ? tags : tags.map { $0.lowercased() }

        if !allowDuplicates && normalizedExisting.contains(normalizedTag) { return }

        tags.append(tag)
        onTagAdded?(tag)
    }

    private func removeTag(_ tag: String) {
        tags.removeAll { $0 == tag }
        onTagRemoved?(tag)
    }
}

// Simple FlowLayout for tags
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        return proposal.replacingUnspecifiedDimensions()
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var point = bounds.origin
        var maxY: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)

            if point.x + size.width > bounds.maxX {
                point.x = bounds.origin.x
                point.y = maxY + spacing
            }

            subview.place(at: point, proposal: .unspecified)
            point.x += size.width + spacing
            maxY = max(maxY, point.y + size.height)
        }
    }
}

#if DEBUG
struct MagicTagInputView_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            MagicTagInputView(
                tags: .constant(["React", "Vue", "Angular"]),
                suggestions: ["JavaScript", "TypeScript", "Python", "Kotlin"],
                label: "Technologies",
                maxTags: 10
            )
            Spacer()
        }
        .padding()
    }
}
#endif
