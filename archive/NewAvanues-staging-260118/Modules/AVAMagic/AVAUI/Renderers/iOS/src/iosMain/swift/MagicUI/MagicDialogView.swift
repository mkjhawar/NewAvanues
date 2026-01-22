import SwiftUI

/**
 * MagicDialogView - Native iOS Sheet Dialog
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicDialogView<Content: View>: ViewModifier {
    @Binding var isPresented: Bool
    let title: String?
    let content: Content

    public func body(content: Content) -> some View {
        content.sheet(isPresented: $isPresented) {
            NavigationView {
                self.content
                    .navigationTitle(title ?? "")
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

extension View {
    public func magicDialog<Content: View>(
        isPresented: Binding<Bool>,
        title: String? = nil,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        self.modifier(MagicDialogView(isPresented: isPresented, title: title, content: content()))
    }
}
