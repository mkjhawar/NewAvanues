import SwiftUI

/**
 * MagicTabsView - iOS Segmented Control Tabs
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicTabsView: View {
    @Binding var selectedIndex: Int
    let tabs: [TabItem]
    let style: TabStyle

    public init(
        selectedIndex: Binding<Int>,
        tabs: [TabItem],
        style: TabStyle = .segmented
    ) {
        self._selectedIndex = selectedIndex
        self.tabs = tabs
        self.style = style
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Tab headers
            Group {
                if style == .segmented {
                    Picker("", selection: $selectedIndex) {
                        ForEach(0..<tabs.count, id: \.self) { index in
                            Text(tabs[index].label).tag(index)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    .padding(.horizontal)
                } else {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 0) {
                            ForEach(0..<tabs.count, id: \.self) { index in
                                Button(action: { selectedIndex = index }) {
                                    VStack(spacing: 8) {
                                        if let icon = tabs[index].icon {
                                            Image(systemName: icon)
                                                .font(.system(size: 20))
                                        }
                                        Text(tabs[index].label)
                                            .font(.system(size: 14, weight: index == selectedIndex ? .semibold : .regular))
                                    }
                                    .foregroundColor(index == selectedIndex ? .accentColor : .secondary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                }
                                .background(
                                    VStack {
                                        Spacer()
                                        Rectangle()
                                            .fill(index == selectedIndex ? Color.accentColor : Color.clear)
                                            .frame(height: 2)
                                    }
                                )
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            }

            Divider()

            // Tab content
            TabView(selection: $selectedIndex) {
                ForEach(0..<tabs.count, id: \.self) { index in
                    tabs[index].content
                        .tag(index)
                }
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
        }
    }
}

public struct TabItem {
    let label: String
    let icon: String?
    let content: AnyView

    public init<Content: View>(label: String, icon: String? = nil, @ViewBuilder content: () -> Content) {
        self.label = label
        self.icon = icon
        self.content = AnyView(content())
    }
}

public enum TabStyle {
    case segmented
    case scrollable
}

// MARK: - Preview
#if DEBUG
struct MagicTabsView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 40) {
            SegmentedTabsPreview()
            ScrollableTabsPreview()
        }
    }

    struct SegmentedTabsPreview: View {
        @State private var selectedIndex = 0

        var body: some View {
            MagicTabsView(
                selectedIndex: $selectedIndex,
                tabs: [
                    TabItem(label: "Home") {
                        Text("Home Content").frame(maxWidth: .infinity, maxHeight: .infinity)
                    },
                    TabItem(label: "Profile") {
                        Text("Profile Content").frame(maxWidth: .infinity, maxHeight: .infinity)
                    },
                    TabItem(label: "Settings") {
                        Text("Settings Content").frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                ],
                style: .segmented
            )
            .frame(height: 300)
        }
    }

    struct ScrollableTabsPreview: View {
        @State private var selectedIndex = 0

        var body: some View {
            MagicTabsView(
                selectedIndex: $selectedIndex,
                tabs: [
                    TabItem(label: "Home", icon: "house") {
                        Text("Home Content").frame(maxWidth: .infinity, maxHeight: .infinity)
                    },
                    TabItem(label: "Search", icon: "magnifyingglass") {
                        Text("Search Content").frame(maxWidth: .infinity, maxHeight: .infinity)
                    },
                    TabItem(label: "Profile", icon: "person") {
                        Text("Profile Content").frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                ],
                style: .scrollable
            )
            .frame(height: 300)
        }
    }
}
#endif
