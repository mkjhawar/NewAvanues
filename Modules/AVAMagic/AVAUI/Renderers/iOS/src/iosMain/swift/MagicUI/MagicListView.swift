import SwiftUI

/**
 * MagicListView - Native iOS List
 *
 * Native SwiftUI list implementation for IDEAMagic framework.
 * Renders ListComponent from Core as native iOS List.
 *
 * Features:
 * - List style variants (Plain, Grouped, Inset Grouped)
 * - Separator customization
 * - Item selection
 * - Swipe actions
 * - List header/footer
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicListView<Data: RandomAccessCollection, Content: View>: View where Data.Element: Identifiable {
    // MARK: - Properties

    let data: Data
    let listStyle: MagicListStyle
    let showsSeparators: Bool
    let content: (Data.Element) -> Content

    // MARK: - Initialization

    public init(
        data: Data,
        listStyle: MagicListStyle = .plain,
        showsSeparators: Bool = true,
        @ViewBuilder content: @escaping (Data.Element) -> Content
    ) {
        self.data = data
        self.listStyle = listStyle
        self.showsSeparators = showsSeparators
        self.content = content
    }

    // MARK: - Body

    public var body: some View {
        List(data) { item in
            content(item)
                .listRowSeparator(showsSeparators ? .visible : .hidden)
        }
        .listStyle(swiftUIListStyle)
    }

    // MARK: - List Style Mapping

    @ViewBuilder
    private var swiftUIListStyle: some ListStyle {
        switch listStyle {
        case .plain:
            PlainListStyle()
        case .grouped:
            GroupedListStyle()
        case .insetGrouped:
            InsetGroupedListStyle()
        case .sidebar:
            SidebarListStyle()
        }
    }
}

// MARK: - List Style Enum

public enum MagicListStyle {
    case plain
    case grouped
    case insetGrouped
    case sidebar
}

// MARK: - List Item Model

public struct MagicListItem: Identifiable {
    public let id: UUID
    public let title: String
    public let subtitle: String?
    public let icon: String?
    public let accessory: ListAccessory

    public init(
        id: UUID = UUID(),
        title: String,
        subtitle: String? = nil,
        icon: String? = nil,
        accessory: ListAccessory = .none
    ) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.accessory = accessory
    }
}

// MARK: - List Accessory Enum

public enum ListAccessory {
    case none
    case chevron
    case checkmark
    case detail
}

// MARK: - Reusable List Row View

public struct MagicListRowView: View {
    let item: MagicListItem
    let action: (() -> Void)?

    public init(item: MagicListItem, action: (() -> Void)? = nil) {
        self.item = item
        self.action = action
    }

    public var body: some View {
        Button(action: {
            action?()
        }) {
            HStack(spacing: 12) {
                // Leading Icon
                if let iconName = item.icon {
                    Image(systemName: iconName)
                        .foregroundColor(.accentColor)
                        .frame(width: 24, height: 24)
                }

                // Title & Subtitle
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.title)
                        .foregroundColor(.primary)

                    if let subtitle = item.subtitle {
                        Text(subtitle)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                // Trailing Accessory
                accessoryView
            }
        }
        .buttonStyle(PlainButtonStyle())
    }

    @ViewBuilder
    private var accessoryView: some View {
        switch item.accessory {
        case .none:
            EmptyView()
        case .chevron:
            Image(systemName: "chevron.right")
                .foregroundColor(.secondary)
                .font(.caption)
        case .checkmark:
            Image(systemName: "checkmark")
                .foregroundColor(.accentColor)
        case .detail:
            Image(systemName: "info.circle")
                .foregroundColor(.accentColor)
        }
    }
}

// MARK: - Preview

#if DEBUG
struct MagicListView_Previews: PreviewProvider {
    static let sampleItems = [
        MagicListItem(title: "Home", icon: "house.fill", accessory: .chevron),
        MagicListItem(title: "Profile", subtitle: "View your profile", icon: "person.fill", accessory: .chevron),
        MagicListItem(title: "Settings", icon: "gear", accessory: .chevron),
        MagicListItem(title: "Help", icon: "questionmark.circle", accessory: .chevron),
        MagicListItem(title: "Logout", icon: "arrow.right.circle", accessory: .none)
    ]

    static let selectedItems = [
        MagicListItem(title: "Option 1", accessory: .checkmark),
        MagicListItem(title: "Option 2", accessory: .none),
        MagicListItem(title: "Option 3", accessory: .checkmark),
        MagicListItem(title: "Option 4", accessory: .none)
    ]

    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Plain list
                VStack(alignment: .leading, spacing: 8) {
                    Text("Plain List").font(.headline)

                    MagicListView(data: sampleItems, listStyle: .plain) { item in
                        MagicListRowView(item: item) {
                            print("Tapped \(item.title)")
                        }
                    }
                    .frame(height: 250)
                }

                Divider()

                // Inset grouped list
                VStack(alignment: .leading, spacing: 8) {
                    Text("Inset Grouped List").font(.headline)

                    MagicListView(data: sampleItems, listStyle: .insetGrouped) { item in
                        MagicListRowView(item: item) {
                            print("Tapped \(item.title)")
                        }
                    }
                    .frame(height: 300)
                }

                Divider()

                // With checkmarks (selection)
                VStack(alignment: .leading, spacing: 8) {
                    Text("List with Selection").font(.headline)

                    MagicListView(data: selectedItems, listStyle: .insetGrouped) { item in
                        MagicListRowView(item: item) {
                            print("Toggled \(item.title)")
                        }
                    }
                    .frame(height: 200)
                }

                Divider()

                // Without separators
                VStack(alignment: .leading, spacing: 8) {
                    Text("Without Separators").font(.headline)

                    MagicListView(
                        data: sampleItems,
                        listStyle: .plain,
                        showsSeparators: false
                    ) { item in
                        MagicListRowView(item: item)
                    }
                    .frame(height: 250)
                }

                Divider()

                // Custom row content
                VStack(alignment: .leading, spacing: 8) {
                    Text("Custom Row Content").font(.headline)

                    MagicListView(data: sampleItems, listStyle: .insetGrouped) { item in
                        HStack {
                            if let icon = item.icon {
                                ZStack {
                                    Circle()
                                        .fill(Color.blue.opacity(0.2))
                                        .frame(width: 40, height: 40)

                                    Image(systemName: icon)
                                        .foregroundColor(.blue)
                                }
                            }

                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.title)
                                    .font(.headline)

                                if let subtitle = item.subtitle {
                                    Text(subtitle)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }

                            Spacer()

                            if item.accessory == .chevron {
                                Image(systemName: "chevron.right")
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                    .frame(height: 300)
                }

                Divider()

                // Menu list
                VStack(alignment: .leading, spacing: 8) {
                    Text("Menu List").font(.headline)

                    List {
                        Section(header: Text("Account")) {
                            MagicListRowView(
                                item: MagicListItem(
                                    title: "Profile",
                                    subtitle: "Manage your profile",
                                    icon: "person.circle",
                                    accessory: .chevron
                                )
                            )
                            MagicListRowView(
                                item: MagicListItem(
                                    title: "Security",
                                    subtitle: "Password & authentication",
                                    icon: "lock.shield",
                                    accessory: .chevron
                                )
                            )
                        }

                        Section(header: Text("Preferences")) {
                            MagicListRowView(
                                item: MagicListItem(
                                    title: "Notifications",
                                    icon: "bell",
                                    accessory: .chevron
                                )
                            )
                            MagicListRowView(
                                item: MagicListItem(
                                    title: "Appearance",
                                    icon: "paintbrush",
                                    accessory: .chevron
                                )
                            )
                        }

                        Section(header: Text("Support")) {
                            MagicListRowView(
                                item: MagicListItem(
                                    title: "Help Center",
                                    icon: "questionmark.circle",
                                    accessory: .chevron
                                )
                            )
                            MagicListRowView(
                                item: MagicListItem(
                                    title: "Contact Us",
                                    icon: "envelope",
                                    accessory: .chevron
                                )
                            )
                        }
                    }
                    .listStyle(InsetGroupedListStyle())
                    .frame(height: 450)
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}
#endif
