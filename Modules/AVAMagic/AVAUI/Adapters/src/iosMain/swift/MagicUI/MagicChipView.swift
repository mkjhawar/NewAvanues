import SwiftUI

/**
 * MagicChipView - Native iOS Chip
 *
 * Native SwiftUI chip implementation for IDEAMagic framework.
 * Renders ChipComponent from Core as native iOS pill-shaped buttons.
 *
 * Features:
 * - Multiple chip styles (Filled, Outlined, Input)
 * - Selection support
 * - Leading icon support
 * - Trailing close button (for input chips)
 * - Disabled state
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicChipView: View {
    // MARK: - Properties

    let text: String
    let style: ChipStyle
    let selected: Bool
    let enabled: Bool
    let leadingIcon: String?
    let onTap: (() -> Void)?
    let onDelete: (() -> Void)?

    // MARK: - Initialization

    public init(
        text: String,
        style: ChipStyle = .filled,
        selected: Bool = false,
        enabled: Bool = true,
        leadingIcon: String? = nil,
        onTap: (() -> Void)? = nil,
        onDelete: (() -> Void)? = nil
    ) {
        self.text = text
        self.style = style
        self.selected = selected
        self.enabled = enabled
        self.leadingIcon = leadingIcon
        self.onTap = onTap
        self.onDelete = onDelete
    }

    // MARK: - Body

    public var body: some View {
        Button(action: {
            onTap?()
        }) {
            HStack(spacing: 6) {
                // Leading Icon
                if let iconName = leadingIcon {
                    Image(systemName: iconName)
                        .font(.system(size: 14))
                }

                // Text
                Text(text)
                    .font(.system(size: 14, weight: .medium))

                // Trailing Close Button (for input chips)
                if onDelete != nil {
                    Button(action: {
                        onDelete?()
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 14))
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .padding(.horizontal, onDelete != nil ? 12 : 16)
            .padding(.vertical, 8)
            .background(backgroundForStyle)
            .foregroundColor(foregroundColor)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(borderColor, lineWidth: borderWidth)
            )
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(!enabled)
        .opacity(enabled ? 1.0 : 0.6)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(text)
        .accessibilityHint(selected ? "Selected" : "Not selected")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    // MARK: - Style Properties

    private var backgroundForStyle: Color {
        switch style {
        case .filled:
            if selected {
                return .accentColor
            }
            return Color(uiColor: .secondarySystemFill)

        case .outlined:
            if selected {
                return Color.accentColor.opacity(0.12)
            }
            return Color.clear

        case .input:
            if selected {
                return Color.accentColor.opacity(0.12)
            }
            return Color(uiColor: .secondarySystemFill)
        }
    }

    private var foregroundColor: Color {
        switch style {
        case .filled:
            if selected {
                return .white
            }
            return .primary

        case .outlined:
            if selected {
                return .accentColor
            }
            return .primary

        case .input:
            if selected {
                return .accentColor
            }
            return .primary
        }
    }

    private var borderColor: Color {
        switch style {
        case .filled:
            return Color.clear

        case .outlined:
            if selected {
                return .accentColor
            }
            return Color(uiColor: .separator)

        case .input:
            return Color.clear
        }
    }

    private var borderWidth: CGFloat {
        switch style {
        case .filled, .input:
            return 0
        case .outlined:
            return 1
        }
    }
}

// MARK: - Chip Style Enum

public enum ChipStyle {
    case filled
    case outlined
    case input
}

// MARK: - Preview

#if DEBUG
struct MagicChipView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Filled chips
                VStack(alignment: .leading, spacing: 8) {
                    Text("Filled Chips").font(.headline)

                    HStack(spacing: 8) {
                        MagicChipView(
                            text: "Unselected",
                            style: .filled,
                            selected: false,
                            onTap: {}
                        )

                        MagicChipView(
                            text: "Selected",
                            style: .filled,
                            selected: true,
                            onTap: {}
                        )

                        MagicChipView(
                            text: "Disabled",
                            style: .filled,
                            selected: false,
                            enabled: false,
                            onTap: {}
                        )
                    }
                }

                // Outlined chips
                VStack(alignment: .leading, spacing: 8) {
                    Text("Outlined Chips").font(.headline)

                    HStack(spacing: 8) {
                        MagicChipView(
                            text: "Unselected",
                            style: .outlined,
                            selected: false,
                            onTap: {}
                        )

                        MagicChipView(
                            text: "Selected",
                            style: .outlined,
                            selected: true,
                            onTap: {}
                        )

                        MagicChipView(
                            text: "Disabled",
                            style: .outlined,
                            selected: false,
                            enabled: false,
                            onTap: {}
                        )
                    }
                }

                // Input chips with delete
                VStack(alignment: .leading, spacing: 8) {
                    Text("Input Chips (with delete)").font(.headline)

                    VStack(alignment: .leading, spacing: 8) {
                        MagicChipView(
                            text: "Removable",
                            style: .input,
                            onTap: {},
                            onDelete: {
                                print("Delete tapped")
                            }
                        )

                        MagicChipView(
                            text: "Selected & Removable",
                            style: .input,
                            selected: true,
                            onTap: {},
                            onDelete: {}
                        )
                    }
                }

                // Chips with icons
                VStack(alignment: .leading, spacing: 8) {
                    Text("Chips with Icons").font(.headline)

                    HStack(spacing: 8) {
                        MagicChipView(
                            text: "Home",
                            style: .filled,
                            leadingIcon: "house.fill",
                            onTap: {}
                        )

                        MagicChipView(
                            text: "Favorite",
                            style: .filled,
                            selected: true,
                            leadingIcon: "heart.fill",
                            onTap: {}
                        )

                        MagicChipView(
                            text: "Star",
                            style: .outlined,
                            leadingIcon: "star.fill",
                            onTap: {}
                        )
                    }
                }

                // Filter chips
                VStack(alignment: .leading, spacing: 8) {
                    Text("Filter Chips").font(.headline)

                    FlowLayout(spacing: 8) {
                        MagicChipView(
                            text: "All",
                            style: .outlined,
                            selected: true,
                            onTap: {}
                        )
                        MagicChipView(
                            text: "Active",
                            style: .outlined,
                            onTap: {}
                        )
                        MagicChipView(
                            text: "Completed",
                            style: .outlined,
                            onTap: {}
                        )
                        MagicChipView(
                            text: "Archived",
                            style: .outlined,
                            onTap: {}
                        )
                    }
                }

                // Category chips
                VStack(alignment: .leading, spacing: 8) {
                    Text("Category Chips").font(.headline)

                    FlowLayout(spacing: 8) {
                        MagicChipView(
                            text: "Technology",
                            style: .filled,
                            selected: true,
                            leadingIcon: "laptopcomputer",
                            onTap: {}
                        )
                        MagicChipView(
                            text: "Design",
                            style: .filled,
                            leadingIcon: "paintbrush.fill",
                            onTap: {}
                        )
                        MagicChipView(
                            text: "Music",
                            style: .filled,
                            leadingIcon: "music.note",
                            onTap: {}
                        )
                        MagicChipView(
                            text: "Sports",
                            style: .filled,
                            leadingIcon: "sportscourt.fill",
                            onTap: {}
                        )
                    }
                }

                // Tag chips (input style with delete)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Tag Chips").font(.headline)

                    FlowLayout(spacing: 8) {
                        MagicChipView(
                            text: "Swift",
                            style: .input,
                            onTap: {},
                            onDelete: {}
                        )
                        MagicChipView(
                            text: "iOS",
                            style: .input,
                            onTap: {},
                            onDelete: {}
                        )
                        MagicChipView(
                            text: "SwiftUI",
                            style: .input,
                            onTap: {},
                            onDelete: {}
                        )
                        MagicChipView(
                            text: "Xcode",
                            style: .input,
                            onTap: {},
                            onDelete: {}
                        )
                    }
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}

// Simple flow layout for wrapping chips
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(in: proposal.width ?? 0, subviews: subviews, spacing: spacing)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(in: bounds.width, subviews: subviews, spacing: spacing)
        for (index, subview) in subviews.enumerated() {
            subview.place(at: CGPoint(x: bounds.minX + result.positions[index].x, y: bounds.minY + result.positions[index].y), proposal: .unspecified)
        }
    }

    struct FlowResult {
        var size: CGSize = .zero
        var positions: [CGPoint] = []

        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var currentX: CGFloat = 0
            var currentY: CGFloat = 0
            var lineHeight: CGFloat = 0

            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)

                if currentX + size.width > maxWidth && currentX > 0 {
                    currentX = 0
                    currentY += lineHeight + spacing
                    lineHeight = 0
                }

                positions.append(CGPoint(x: currentX, y: currentY))
                currentX += size.width + spacing
                lineHeight = max(lineHeight, size.height)
            }

            self.size = CGSize(width: maxWidth, height: currentY + lineHeight)
        }
    }
}
#endif
