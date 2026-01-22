import SwiftUI

/**
 * MagicIconView - Native iOS Icon
 *
 * Native SwiftUI icon implementation for IDEAMagic framework.
 * Renders IconComponent from Core as native SF Symbols icons.
 *
 * Features:
 * - SF Symbols support (3,000+ icons)
 * - Size variants (Small, Medium, Large, ExtraLarge)
 * - Color customization
 * - Tint and fill support
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicIconView: View {
    // MARK: - Properties

    let iconName: String
    let size: IconSize
    let color: Color?
    let contentDescription: String?

    // MARK: - Initialization

    public init(
        iconName: String,
        size: IconSize = .medium,
        color: Color? = nil,
        contentDescription: String? = nil
    ) {
        self.iconName = iconName
        self.size = size
        self.color = color
        self.contentDescription = contentDescription
    }

    // MARK: - Body

    public var body: some View {
        Image(systemName: iconName)
            .font(fontForSize)
            .foregroundColor(color ?? .primary)
            .accessibilityLabel(contentDescription ?? iconName)
    }

    // MARK: - Size Mapping

    private var fontForSize: Font {
        switch size {
        case .small:
            return .system(size: 16)
        case .medium:
            return .system(size: 24)
        case .large:
            return .system(size: 32)
        case .extraLarge:
            return .system(size: 48)
        }
    }
}

// MARK: - Icon Size Enum

public enum IconSize {
    case small      // 16pt
    case medium     // 24pt
    case large      // 32pt
    case extraLarge // 48pt
}

// MARK: - Common Icon Names (Material → SF Symbols Mapping)

public struct MagicIcons {
    // Navigation
    public static let home = "house.fill"
    public static let back = "chevron.left"
    public static let forward = "chevron.right"
    public static let menu = "line.3.horizontal"
    public static let close = "xmark"
    public static let search = "magnifyingglass"
    public static let settings = "gear"

    // Actions
    public static let add = "plus"
    public static let remove = "minus"
    public static let delete = "trash.fill"
    public static let edit = "pencil"
    public static let save = "checkmark"
    public static let cancel = "xmark.circle"
    public static let share = "square.and.arrow.up"
    public static let download = "arrow.down.circle.fill"
    public static let upload = "arrow.up.circle.fill"
    public static let refresh = "arrow.clockwise"

    // Communication
    public static let email = "envelope.fill"
    public static let phone = "phone.fill"
    public static let message = "message.fill"
    public static let notification = "bell.fill"
    public static let send = "paperplane.fill"

    // Media
    public static let play = "play.fill"
    public static let pause = "pause.fill"
    public static let stop = "stop.fill"
    public static let camera = "camera.fill"
    public static let image = "photo.fill"
    public static let video = "video.fill"
    public static let music = "music.note"
    public static let microphone = "mic.fill"

    // Content
    public static let favorite = "heart.fill"
    public static let star = "star.fill"
    public static let bookmark = "bookmark.fill"
    public static let flag = "flag.fill"
    public static let attach = "paperclip"
    public static let link = "link"
    public static let copy = "doc.on.doc"

    // User
    public static let person = "person.fill"
    public static let group = "person.3.fill"
    public static let account = "person.circle.fill"
    public static let login = "arrow.right.circle.fill"
    public static let logout = "arrow.left.circle.fill"

    // Status
    public static let check = "checkmark.circle.fill"
    public static let error = "exclamationmark.circle.fill"
    public static let warning = "exclamationmark.triangle.fill"
    public static let info = "info.circle.fill"
    public static let help = "questionmark.circle.fill"

    // File & Folder
    public static let folder = "folder.fill"
    public static let file = "doc.fill"
    public static let document = "doc.text.fill"
    public static let cloud = "icloud.fill"
    public static let cloudDownload = "icloud.and.arrow.down.fill"
    public static let cloudUpload = "icloud.and.arrow.up.fill"

    // UI Controls
    public static let visibility = "eye.fill"
    public static let visibilityOff = "eye.slash.fill"
    public static let lock = "lock.fill"
    public static let unlock = "lock.open.fill"
    public static let calendar = "calendar"
    public static let clock = "clock.fill"
    public static let location = "location.fill"

    // Arrows & Directions
    public static let arrowUp = "arrow.up"
    public static let arrowDown = "arrow.down"
    public static let arrowLeft = "arrow.left"
    public static let arrowRight = "arrow.right"
    public static let expand = "arrow.up.left.and.arrow.down.right"
    public static let collapse = "arrow.down.right.and.arrow.up.left"

    // Social
    public static let thumbsUp = "hand.thumbsup.fill"
    public static let thumbsDown = "hand.thumbsdown.fill"
    public static let comment = "bubble.left.fill"
    public static let share2 = "square.and.arrow.up.fill"
}

// MARK: - Preview

#if DEBUG
struct MagicIconView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Size variants
                VStack(alignment: .leading, spacing: 8) {
                    Text("Icon Sizes").font(.headline)
                    HStack(spacing: 16) {
                        VStack {
                            MagicIconView(iconName: MagicIcons.home, size: .small)
                            Text("Small").font(.caption)
                        }
                        VStack {
                            MagicIconView(iconName: MagicIcons.home, size: .medium)
                            Text("Medium").font(.caption)
                        }
                        VStack {
                            MagicIconView(iconName: MagicIcons.home, size: .large)
                            Text("Large").font(.caption)
                        }
                        VStack {
                            MagicIconView(iconName: MagicIcons.home, size: .extraLarge)
                            Text("XLarge").font(.caption)
                        }
                    }
                }

                Divider()

                // Color variants
                VStack(alignment: .leading, spacing: 8) {
                    Text("Icon Colors").font(.headline)
                    HStack(spacing: 16) {
                        MagicIconView(iconName: MagicIcons.favorite, color: .red)
                        MagicIconView(iconName: MagicIcons.star, color: .yellow)
                        MagicIconView(iconName: MagicIcons.check, color: .green)
                        MagicIconView(iconName: MagicIcons.info, color: .blue)
                        MagicIconView(iconName: MagicIcons.warning, color: .orange)
                    }
                }

                Divider()

                // Navigation icons
                VStack(alignment: .leading, spacing: 8) {
                    Text("Navigation").font(.headline)
                    HStack(spacing: 12) {
                        MagicIconView(iconName: MagicIcons.home)
                        MagicIconView(iconName: MagicIcons.back)
                        MagicIconView(iconName: MagicIcons.forward)
                        MagicIconView(iconName: MagicIcons.menu)
                        MagicIconView(iconName: MagicIcons.search)
                        MagicIconView(iconName: MagicIcons.settings)
                    }
                }

                // Action icons
                VStack(alignment: .leading, spacing: 8) {
                    Text("Actions").font(.headline)
                    HStack(spacing: 12) {
                        MagicIconView(iconName: MagicIcons.add)
                        MagicIconView(iconName: MagicIcons.edit)
                        MagicIconView(iconName: MagicIcons.delete)
                        MagicIconView(iconName: MagicIcons.save)
                        MagicIconView(iconName: MagicIcons.share)
                        MagicIconView(iconName: MagicIcons.refresh)
                    }
                }

                // Communication icons
                VStack(alignment: .leading, spacing: 8) {
                    Text("Communication").font(.headline)
                    HStack(spacing: 12) {
                        MagicIconView(iconName: MagicIcons.email)
                        MagicIconView(iconName: MagicIcons.phone)
                        MagicIconView(iconName: MagicIcons.message)
                        MagicIconView(iconName: MagicIcons.notification)
                        MagicIconView(iconName: MagicIcons.send)
                    }
                }

                // Media icons
                VStack(alignment: .leading, spacing: 8) {
                    Text("Media").font(.headline)
                    HStack(spacing: 12) {
                        MagicIconView(iconName: MagicIcons.play)
                        MagicIconView(iconName: MagicIcons.pause)
                        MagicIconView(iconName: MagicIcons.camera)
                        MagicIconView(iconName: MagicIcons.image)
                        MagicIconView(iconName: MagicIcons.music)
                    }
                }

                // Status icons
                VStack(alignment: .leading, spacing: 8) {
                    Text("Status").font(.headline)
                    HStack(spacing: 12) {
                        MagicIconView(iconName: MagicIcons.check, color: .green)
                        MagicIconView(iconName: MagicIcons.error, color: .red)
                        MagicIconView(iconName: MagicIcons.warning, color: .orange)
                        MagicIconView(iconName: MagicIcons.info, color: .blue)
                        MagicIconView(iconName: MagicIcons.help, color: .secondary)
                    }
                }

                // User icons
                VStack(alignment: .leading, spacing: 8) {
                    Text("User").font(.headline)
                    HStack(spacing: 12) {
                        MagicIconView(iconName: MagicIcons.person)
                        MagicIconView(iconName: MagicIcons.group)
                        MagicIconView(iconName: MagicIcons.account)
                        MagicIconView(iconName: MagicIcons.login)
                        MagicIconView(iconName: MagicIcons.logout)
                    }
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}
#endif
