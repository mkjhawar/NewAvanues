import SwiftUI

/**
 * MagicAvatarView - iOS Avatar Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicAvatarView: View {
    let imageUrl: String?
    let text: String?
    let icon: String?
    let alt: String
    let size: String
    let shape: String
    let backgroundColor: String?
    let textColor: String?
    let statusIndicator: String?
    let badgeContent: String?
    let clickable: Bool
    let onClick: (() -> Void)?

    public init(
        imageUrl: String? = nil,
        text: String? = nil,
        icon: String? = nil,
        alt: String = "",
        size: String = "medium",
        shape: String = "circle",
        backgroundColor: String? = nil,
        textColor: String? = nil,
        statusIndicator: String? = nil,
        badgeContent: String? = nil,
        clickable: Bool = false,
        onClick: (() -> Void)? = nil
    ) {
        self.imageUrl = imageUrl
        self.text = text
        self.icon = icon
        self.alt = alt
        self.size = size
        self.shape = shape
        self.backgroundColor = backgroundColor
        self.textColor = textColor
        self.statusIndicator = statusIndicator
        self.badgeContent = badgeContent
        self.clickable = clickable
        self.onClick = onClick
    }

    public var body: some View {
        ZStack(alignment: .bottomTrailing) {
            avatarContent
                .frame(width: avatarSize, height: avatarSize)
                .background(bgColor)
                .clipShape(avatarShape)
                .onTapGesture {
                    if clickable {
                        onClick?()
                    }
                }

            // Status indicator
            if let status = statusIndicator {
                Circle()
                    .fill(statusColor(status))
                    .frame(width: avatarSize * 0.25, height: avatarSize * 0.25)
                    .overlay(
                        Circle()
                            .stroke(Color(uiColor: .systemBackground), lineWidth: 2)
                    )
            }

            // Badge
            if let badge = badgeContent {
                Text(badge)
                    .font(.caption2)
                    .foregroundColor(.white)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 2)
                    .background(Color.red)
                    .clipShape(Capsule())
                    .offset(x: 4, y: -4)
            }
        }
    }

    @ViewBuilder
    private var avatarContent: some View {
        if let url = imageUrl {
            // In production, use AsyncImage or SDWebImage
            Image(systemName: "person.fill")
                .resizable()
                .scaledToFit()
                .frame(width: avatarSize * 0.6, height: avatarSize * 0.6)
                .foregroundColor(fgColor)
        } else if let txt = text {
            Text(txt)
                .font(fontSize)
                .fontWeight(.bold)
                .foregroundColor(fgColor)
        } else if let iconName = icon {
            Image(systemName: mapIcon(iconName))
                .resizable()
                .scaledToFit()
                .frame(width: avatarSize * 0.6, height: avatarSize * 0.6)
                .foregroundColor(fgColor)
        }
    }

    private var avatarSize: CGFloat {
        switch size {
        case "xsmall": return 24
        case "small": return 32
        case "large": return 56
        case "xlarge": return 72
        default: return 40
        }
    }

    private var fontSize: Font {
        switch size {
        case "xsmall": return .caption2
        case "small": return .caption
        case "large": return .title3
        case "xlarge": return .title2
        default: return .body
        }
    }

    private var avatarShape: some Shape {
        switch shape {
        case "roundedsquare": return AnyShape(RoundedRectangle(cornerRadius: 8))
        case "square": return AnyShape(Rectangle())
        default: return AnyShape(Circle())
        }
    }

    private var bgColor: Color {
        if let bg = backgroundColor {
            return Color(hex: bg) ?? Color.accentColor.opacity(0.2)
        }
        return Color.accentColor.opacity(0.2)
    }

    private var fgColor: Color {
        if let fg = textColor {
            return Color(hex: fg) ?? Color.accentColor
        }
        return Color.accentColor
    }

    private func statusColor(_ status: String) -> Color {
        switch status.lowercased() {
        case "online": return .green
        case "offline": return .gray
        case "busy": return .red
        case "away": return .orange
        default: return .gray
        }
    }

    private func mapIcon(_ name: String) -> String {
        switch name.lowercased() {
        case "person", "user": return "person.fill"
        case "settings": return "gearshape.fill"
        case "star": return "star.fill"
        case "favorite": return "heart.fill"
        case "home": return "house.fill"
        default: return "person.fill"
        }
    }
}

// Helper for any shape
struct AnyShape: Shape {
    private let _path: (CGRect) -> Path

    init<S: Shape>(_ shape: S) {
        _path = { rect in
            shape.path(in: rect)
        }
    }

    func path(in rect: CGRect) -> Path {
        return _path(rect)
    }
}

// Color extension
extension Color {
    init?(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6:
            (r, g, b, a) = ((int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF, 255)
        case 8:
            (r, g, b, a) = ((int >> 24) & 0xFF, (int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        default:
            return nil
        }
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255, opacity: Double(a) / 255)
    }
}

#if DEBUG
struct MagicAvatarView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            MagicAvatarView(text: "JD", size: "medium")
            MagicAvatarView(icon: "person", size: "large", statusIndicator: "online")
            MagicAvatarView(text: "AB", badgeContent: "5", size: "medium")
        }
        .padding()
    }
}
#endif
