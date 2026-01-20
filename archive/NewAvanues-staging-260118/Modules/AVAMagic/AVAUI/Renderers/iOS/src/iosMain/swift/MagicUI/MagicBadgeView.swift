import SwiftUI

/**
 * MagicBadgeView - iOS Badge Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicBadgeView: View {
    let content: String
    let variant: String
    let color: String
    let size: String
    let maxCount: Int?
    let showZero: Bool
    let pulse: Bool
    let invisible: Bool

    @State private var scale: CGFloat = 1.0

    public init(
        content: String = "",
        variant: String = "filled",
        color: String = "primary",
        size: String = "medium",
        maxCount: Int? = nil,
        showZero: Bool = false,
        pulse: Bool = false,
        invisible: Bool = false
    ) {
        self.content = content
        self.variant = variant
        self.color = color
        self.size = size
        self.maxCount = maxCount
        self.showZero = showZero
        self.pulse = pulse
        self.invisible = invisible
    }

    private var displayContent: String {
        if content.isEmpty { return "" }

        if let numericValue = Int(content), let max = maxCount, numericValue > max {
            return "\(max)+"
        }

        if let numericValue = Int(content), numericValue == 0, !showZero {
            return ""
        }

        return content
    }

    private var isVisible: Bool {
        if invisible { return false }
        if variant == "dot" { return true }
        if content.isEmpty { return false }

        if let numericValue = Int(content), numericValue == 0, !showZero {
            return false
        }

        return true
    }

    public var body: some View {
        if isVisible {
            ZStack {
                if variant == "dot" {
                    Circle()
                        .fill(badgeBackground)
                        .frame(width: dotSize, height: dotSize)
                } else if !displayContent.isEmpty {
                    Group {
                        if variant == "filled" {
                            Capsule()
                                .fill(badgeBackground)
                        } else {
                            Capsule()
                                .stroke(badgeBorder, lineWidth: 1)
                        }
                    }
                    .frame(width: badgeWidth, height: badgeHeight)
                    .overlay(
                        Text(displayContent)
                            .font(badgeFont)
                            .foregroundColor(badgeForeground)
                    )
                }
            }
            .scaleEffect(scale)
            .onAppear {
                if pulse {
                    withAnimation(
                        Animation.easeInOut(duration: 1.0)
                            .repeatForever(autoreverses: true)
                    ) {
                        scale = 1.2
                    }
                }
            }
        }
    }

    private var dotSize: CGFloat {
        switch size {
        case "small": return 8
        case "large": return 12
        default: return 10
        }
    }

    private var badgeHeight: CGFloat {
        switch size {
        case "small": return 16
        case "large": return 24
        default: return 20
        }
    }

    private var badgeWidth: CGFloat {
        let baseWidth = badgeHeight
        let extraWidth = CGFloat(max(0, displayContent.count - 2)) * 4
        return baseWidth + extraWidth
    }

    private var badgeFont: Font {
        switch size {
        case "small": return .caption2
        case "large": return .footnote
        default: return .caption
        }
    }

    private var badgeBackground: Color {
        switch (variant, color) {
        case ("filled", "primary"): return .accentColor
        case ("filled", "secondary"): return .secondary
        case ("filled", "error"): return .red
        case ("filled", "success"): return .green
        case ("filled", "warning"): return .orange
        case ("filled", "info"): return .blue
        case ("filled", _): return Color(uiColor: .systemGray)
        case ("dot", "success"): return .green
        case ("dot", "error"): return .red
        case ("dot", _): return .accentColor
        default: return .clear
        }
    }

    private var badgeForeground: Color {
        switch (variant, color) {
        case ("filled", "warning"): return .black
        case ("filled", _): return .white
        case ("outlined", "primary"): return .accentColor
        case ("outlined", "secondary"): return .secondary
        case ("outlined", "error"): return .red
        case ("outlined", "success"): return .green
        case ("outlined", "warning"): return .orange
        case ("outlined", "info"): return .blue
        default: return .primary
        }
    }

    private var badgeBorder: Color {
        switch color {
        case "primary": return .accentColor
        case "secondary": return .secondary
        case "error": return .red
        case "success": return .green
        case "warning": return .orange
        case "info": return .blue
        default: return Color(uiColor: .systemGray)
        }
    }
}
