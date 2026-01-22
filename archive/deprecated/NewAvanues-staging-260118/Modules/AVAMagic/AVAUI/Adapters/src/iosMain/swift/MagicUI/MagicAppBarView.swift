import SwiftUI

/**
 * MagicAppBarView - iOS Navigation Bar
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicAppBarView<Content: View>: View {
    let title: String
    let leadingButton: BarButton?
    let trailingButton: BarButton?
    let content: Content

    public init(
        title: String,
        leadingButton: BarButton? = nil,
        trailingButton: BarButton? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.leadingButton = leadingButton
        self.trailingButton = trailingButton
        self.content = content()
    }

    public var body: some View {
        NavigationView {
            content
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.large)
                .toolbar {
                    if let leading = leadingButton {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button(action: leading.action) {
                                if let icon = leading.icon {
                                    Image(systemName: icon)
                                } else {
                                    Text(leading.title)
                                }
                            }
                        }
                    }
                    if let trailing = trailingButton {
                        ToolbarItem(placement: .navigationBarTrailing) {
                            Button(action: trailing.action) {
                                if let icon = trailing.icon {
                                    Image(systemName: icon)
                                } else {
                                    Text(trailing.title)
                                }
                            }
                        }
                    }
                }
        }
    }
}

public struct BarButton {
    let title: String
    let icon: String?
    let action: () -> Void

    public init(title: String = "", icon: String? = nil, action: @escaping () -> Void) {
        self.title = title
        self.icon = icon
        self.action = action
    }
}
