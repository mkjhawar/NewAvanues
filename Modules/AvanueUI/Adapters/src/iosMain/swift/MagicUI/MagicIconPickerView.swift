import SwiftUI

/**
 * MagicIconPickerView - iOS Icon Picker
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicIconPickerView: View {
    @Binding var value: String
    @State private var showPicker: Bool = false
    @State private var searchQuery: String = ""
    @State private var selectedCategory: String? = nil

    let label: String?
    let library: IconLibraryType
    let icons: [IconDataType]
    let categories: [String]
    let showSearch: Bool
    let showCategories: Bool
    let showRecent: Bool
    let recentIcons: [String]
    let gridColumns: Int
    let iconSize: IconSizeType
    let placeholder: String
    let helperText: String?
    let errorText: String?
    let enabled: Bool
    let readOnly: Bool
    let onIconChanged: ((String) -> Void)?

    public init(
        value: Binding<String>,
        label: String? = nil,
        library: IconLibraryType = .materialIcons,
        icons: [IconDataType],
        categories: [String] = [],
        showSearch: Bool = true,
        showCategories: Bool = false,
        showRecent: Bool = false,
        recentIcons: [String] = [],
        gridColumns: Int = 6,
        iconSize: IconSizeType = .medium,
        placeholder: String = "search...",
        helperText: String? = nil,
        errorText: String? = nil,
        enabled: Bool = true,
        readOnly: Bool = false,
        onIconChanged: ((String) -> Void)? = nil
    ) {
        self._value = value
        self.label = label
        self.library = library
        self.icons = icons
        self.categories = categories
        self.showSearch = showSearch
        self.showCategories = showCategories
        self.showRecent = showRecent
        self.recentIcons = recentIcons
        self.gridColumns = gridColumns
        self.iconSize = iconSize
        self.placeholder = placeholder
        self.helperText = helperText
        self.errorText = errorText
        self.enabled = enabled
        self.readOnly = readOnly
        self.onIconChanged = onIconChanged
    }

    private var filteredIcons: [IconDataType] {
        icons.filter { icon in
            let matchesSearch = searchQuery.isEmpty ||
                icon.name.localizedCaseInsensitiveContains(searchQuery) ||
                icon.label.localizedCaseInsensitiveContains(searchQuery) ||
                icon.tags.contains { $0.localizedCaseInsensitiveContains(searchQuery) }

            let matchesCategory = selectedCategory.map { category in
                icon.category.caseInsensitiveCompare(category) == .orderedSame
            } ?? true

            return matchesSearch && matchesCategory
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

            // Selected icon display button
            Button(action: {
                if enabled && !readOnly {
                    showPicker = true
                }
            }) {
                HStack {
                    if !value.isEmpty {
                        Image(systemName: getSFSymbol(for: value))
                            .frame(width: 24, height: 24)
                        Text(value)
                            .foregroundColor(.primary)
                    } else {
                        Image(systemName: "magnifyingglass")
                            .frame(width: 24, height: 24)
                        Text("Choose Icon")
                            .foregroundColor(.primary)
                    }
                    Spacer()
                }
                .padding(12)
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(8)
            }
            .disabled(!enabled || readOnly)

            // Helper/Error text
            if let helper = helperText {
                Text(helper)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.leading, 16)
            }
            if let error = errorText {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .padding(.leading, 16)
            }

            // Recent icons
            if showRecent && !recentIcons.isEmpty {
                Text("Recent")
                    .font(.caption)
                    .foregroundColor(.secondary)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(recentIcons.prefix(10), id: \.self) { iconName in
                            Button(action: {
                                if enabled && !readOnly {
                                    value = iconName
                                    onIconChanged?(iconName)
                                }
                            }) {
                                Image(systemName: getSFSymbol(for: iconName))
                                    .frame(width: 40, height: 40)
                                    .background(Color(uiColor: .secondarySystemBackground))
                                    .cornerRadius(8)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(
                                                iconName == value ? Color.accentColor : Color.gray.opacity(0.3),
                                                lineWidth: iconName == value ? 2 : 1
                                            )
                                    )
                            }
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $showPicker) {
            NavigationView {
                VStack(spacing: 12) {
                    // Search field
                    if showSearch {
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .foregroundColor(.secondary)
                            TextField(placeholder, text: $searchQuery)
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
                        .padding(.horizontal)
                    }

                    // Category filter
                    if showCategories && !categories.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                CategoryChip(
                                    label: "All",
                                    isSelected: selectedCategory == nil,
                                    action: { selectedCategory = nil }
                                )
                                ForEach(categories, id: \.self) { category in
                                    CategoryChip(
                                        label: category,
                                        isSelected: selectedCategory == category,
                                        action: { selectedCategory = category }
                                    )
                                }
                            }
                            .padding(.horizontal)
                        }
                    }

                    // Icon count
                    Text("\(filteredIcons.count) icons")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    // Icon grid
                    ScrollView {
                        LazyVGrid(
                            columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: gridColumns),
                            spacing: 4
                        ) {
                            ForEach(filteredIcons, id: \.name) { iconData in
                                IconGridItem(
                                    iconData: iconData,
                                    isSelected: iconData.name == value,
                                    iconSize: iconSize,
                                    action: {
                                        value = iconData.name
                                        onIconChanged?(iconData.name)
                                        showPicker = false
                                    }
                                )
                            }
                        }
                        .padding(.horizontal)
                    }
                }
                .navigationTitle(label ?? "Choose Icon")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("Done") {
                            showPicker = false
                        }
                    }
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("Cancel") {
                            showPicker = false
                        }
                    }
                }
            }
        }
    }

    private func getSFSymbol(for iconName: String) -> String {
        // Map Material Icons names to SF Symbols
        let normalized = iconName.lowercased().replacingOccurrences(of: "_", with: "")
        switch normalized {
        case "home": return "house"
        case "favorite", "heart": return "heart"
        case "star": return "star"
        case "settings": return "gearshape"
        case "search": return "magnifyingglass"
        case "menu": return "line.3.horizontal"
        case "close": return "xmark"
        case "arrowback", "back": return "arrow.left"
        case "arrowforward", "forward": return "arrow.right"
        case "morevert", "more": return "ellipsis"
        case "person": return "person"
        case "email": return "envelope"
        case "phone": return "phone"
        case "notifications": return "bell"
        case "edit": return "pencil"
        case "delete": return "trash"
        case "add": return "plus"
        case "remove": return "minus"
        case "check": return "checkmark"
        case "info": return "info.circle"
        case "warning": return "exclamationmark.triangle"
        case "shoppingcart", "cart": return "cart"
        case "lock": return "lock"
        case "visibility": return "eye"
        case "thumbup", "like": return "hand.thumbsup"
        case "refresh": return "arrow.clockwise"
        default: return "star" // Default fallback
        }
    }
}

// Category filter chip
private struct CategoryChip: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.caption)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? Color.accentColor.opacity(0.2) : Color(uiColor: .secondarySystemBackground))
                .foregroundColor(isSelected ? Color.accentColor : .primary)
                .cornerRadius(16)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 1)
                )
        }
    }
}

// Icon grid item
private struct IconGridItem: View {
    let iconData: IconDataType
    let isSelected: Bool
    let iconSize: IconSizeType
    let action: () -> Void

    private var size: CGFloat {
        switch iconSize {
        case .small: return 32
        case .medium: return 48
        case .large: return 64
        }
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: getSFSymbol(for: iconData.name))
                .font(.system(size: size - 16))
                .frame(width: size, height: size)
                .background(
                    isSelected ? Color.accentColor.opacity(0.2) : Color(uiColor: .secondarySystemBackground)
                )
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(
                            isSelected ? Color.accentColor : Color.gray.opacity(0.3),
                            lineWidth: isSelected ? 2 : 1
                        )
                )
        }
    }

    private func getSFSymbol(for iconName: String) -> String {
        let normalized = iconName.lowercased().replacingOccurrences(of: "_", with: "")
        switch normalized {
        case "home": return "house"
        case "favorite", "heart": return "heart"
        case "star": return "star"
        case "settings": return "gearshape"
        case "search": return "magnifyingglass"
        case "menu": return "line.3.horizontal"
        case "close": return "xmark"
        case "arrowback", "back": return "arrow.left"
        case "arrowforward", "forward": return "arrow.right"
        case "person": return "person"
        case "email": return "envelope"
        case "phone": return "phone"
        case "edit": return "pencil"
        case "delete": return "trash"
        case "add": return "plus"
        case "check": return "checkmark"
        default: return "star"
        }
    }
}

// Data types
public struct IconDataType {
    public let name: String
    public let label: String
    public let category: String
    public let tags: [String]
    public let codepoint: String?

    public init(name: String, label: String, category: String, tags: [String], codepoint: String? = nil) {
        self.name = name
        self.label = label
        self.category = category
        self.tags = tags
        self.codepoint = codepoint
    }
}

public enum IconLibraryType {
    case materialIcons
    case fontAwesome
    case sfSymbols
    case custom
}

public enum IconSizeType {
    case small
    case medium
    case large
}

#if DEBUG
struct MagicIconPickerView_Previews: PreviewProvider {
    static var previews: some View {
        let sampleIcons = [
            IconDataType(name: "home", label: "Home", category: "Action", tags: ["house"]),
            IconDataType(name: "favorite", label: "Favorite", category: "Action", tags: ["heart", "like"]),
            IconDataType(name: "star", label: "Star", category: "Action", tags: ["rating"]),
            IconDataType(name: "settings", label: "Settings", category: "Action", tags: ["gear"]),
            IconDataType(name: "search", label: "Search", category: "Action", tags: ["find"]),
            IconDataType(name: "person", label: "Person", category: "Social", tags: ["user"]),
            IconDataType(name: "email", label: "Email", category: "Communication", tags: ["mail"]),
            IconDataType(name: "phone", label: "Phone", category: "Communication", tags: ["call"])
        ]

        VStack(spacing: 20) {
            MagicIconPickerView(
                value: .constant("home"),
                label: "Choose Icon",
                icons: sampleIcons,
                categories: ["Action", "Social", "Communication"],
                showSearch: true,
                showCategories: true
            )

            Spacer()
        }
        .padding()
    }
}
#endif
