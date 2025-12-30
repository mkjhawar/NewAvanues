import SwiftUI

/**
 * MagicBottomNavView - iOS Tab Bar
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicBottomNavView: View {
    @Binding var selectedIndex: Int
    let items: [NavItem]

    public init(selectedIndex: Binding<Int>, items: [NavItem]) {
        self._selectedIndex = selectedIndex
        self.items = items
    }

    public var body: some View {
        TabView(selection: $selectedIndex) {
            ForEach(0..<items.count, id: \.self) { index in
                items[index].content
                    .tabItem {
                        Image(systemName: items[index].icon)
                        Text(items[index].label)
                    }
                    .tag(index)
            }
        }
    }
}

public struct NavItem {
    let label: String
    let icon: String
    let content: AnyView

    public init<Content: View>(label: String, icon: String, @ViewBuilder content: () -> Content) {
        self.label = label
        self.icon = icon
        self.content = AnyView(content())
    }
}
