import SwiftUI

/**
 * MagicTooltipView - iOS Tooltip/Popover
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicTooltipView: ViewModifier {
    @Binding var isPresented: Bool
    let text: String

    public func body(content: Content) -> some View {
        content.overlay(
            Group {
                if isPresented {
                    VStack {
                        Text(text)
                            .font(.caption)
                            .padding(8)
                            .background(Color.black.opacity(0.8))
                            .foregroundColor(.white)
                            .cornerRadius(6)
                    }
                    .transition(.opacity)
                }
            }
            .offset(y: -40)
        )
    }
}

extension View {
    public func magicTooltip(isPresented: Binding<Bool>, text: String) -> some View {
        self.modifier(MagicTooltipView(isPresented: isPresented, text: text))
    }
}
