import SwiftUI

/// Renders numbered badges on interactive DOM elements.
///
/// Positioned as an overlay on top of the WKWebView.
/// Each badge shows the element's index (1, 2, 3...) for voice commands
/// like "click 3". Only renders elements within the current viewport.
///
/// Mirrors the Android two-layer overlay system:
/// - Layer 1: Icon text labels
/// - Layer 2: Numbered badges
struct ElementOverlayView: View {
    let elements: [DOMElement]
    let webViewFrame: CGRect
    let scrollOffset: CGPoint
    @Binding var showOverlay: Bool

    /// Offset from WKWebView top to account for address bar.
    var topInset: CGFloat = 0

    private var visibleElements: [(index: Int, element: DOMElement)] {
        elements.enumerated().compactMap { (index, element) in
            let bounds = element.bounds
            // Only show elements currently in viewport
            let inViewport = bounds.right > 0
                && bounds.bottom > 0
                && bounds.left < Int(webViewFrame.width)
                && bounds.top < Int(webViewFrame.height)
            guard inViewport && !element.isDisabled else { return nil }
            return (index: index + 1, element: element)
        }
    }

    var body: some View {
        if showOverlay {
            ZStack(alignment: .topLeading) {
                ForEach(visibleElements, id: \.element.id) { item in
                    badgeView(index: item.index, element: item.element)
                }
            }
            .frame(width: webViewFrame.width, height: webViewFrame.height)
            .allowsHitTesting(false)
        }
    }

    @ViewBuilder
    private func badgeView(index: Int, element: DOMElement) -> some View {
        let x = CGFloat(element.bounds.left)
        let y = CGFloat(element.bounds.top) + topInset

        Text("\(index)")
            .font(.system(size: 10, weight: .bold, design: .monospaced))
            .foregroundColor(.white)
            .padding(.horizontal, 4)
            .padding(.vertical, 2)
            .background(
                Capsule()
                    .fill(badgeColor(for: element.type))
                    .shadow(color: .black.opacity(0.3), radius: 1, y: 1)
            )
            .position(x: x + 8, y: y - 4)
    }

    /// Badge color based on element type (matches Android overlay).
    private func badgeColor(for type: String) -> Color {
        switch type {
        case "button": return Color(hex: 0x1E40AF) // Hydra primary
        case "link": return Color(hex: 0x7C3AED)   // Purple
        case "input": return Color(hex: 0x059669)   // Green
        case "checkbox", "radio": return Color(hex: 0xD97706) // Amber
        case "dropdown": return Color(hex: 0xDC2626) // Red
        default: return Color(hex: 0x64748B)        // Slate
        }
    }
}
