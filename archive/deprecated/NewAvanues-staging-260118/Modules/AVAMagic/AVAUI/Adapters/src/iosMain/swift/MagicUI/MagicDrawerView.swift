import SwiftUI

/**
 * MagicDrawerView - iOS Sheet Drawer
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicDrawerView<Content: View>: View {
    @Binding var isPresented: Bool
    let position: DrawerPosition
    let content: Content

    public init(
        isPresented: Binding<Bool>,
        position: DrawerPosition = .bottom,
        @ViewBuilder content: () -> Content
    ) {
        self._isPresented = isPresented
        self.position = position
        self.content = content()
    }

    public var body: some View {
        EmptyView()
            .sheet(isPresented: $isPresented) {
                if position == .bottom {
                    content
                        .presentationDetents([.medium, .large])
                        .presentationDragIndicator(.visible)
                } else {
                    NavigationView {
                        content
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .navigationBarTrailing) {
                                    Button("Done") {
                                        isPresented = false
                                    }
                                }
                            }
                    }
                }
            }
    }
}

public enum DrawerPosition {
    case leading
    case trailing
    case bottom
}

// MARK: - Preview
#if DEBUG
struct MagicDrawerView_Previews: PreviewProvider {
    static var previews: some View {
        DrawerPreviewContainer()
    }

    struct DrawerPreviewContainer: View {
        @State private var showDrawer = false

        var body: some View {
            VStack {
                Button("Show Drawer") {
                    showDrawer = true
                }
            }
            .magicDrawer(isPresented: $showDrawer, position: .bottom) {
                VStack(spacing: 16) {
                    Text("Drawer Content")
                        .font(.headline)
                    Text("This is a bottom sheet drawer")
                        .foregroundColor(.secondary)
                    Button("Close") {
                        showDrawer = false
                    }
                    .padding()
                }
                .padding()
            }
        }
    }
}

// MARK: - View Extension
extension View {
    public func magicDrawer<Content: View>(
        isPresented: Binding<Bool>,
        position: DrawerPosition = .bottom,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        self.background(
            MagicDrawerView(isPresented: isPresented, position: position, content: content)
        )
    }
}
#endif
