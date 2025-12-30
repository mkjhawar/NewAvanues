import SwiftUI

/**
 * MagicDividerView - Native iOS Divider
 *
 * Native SwiftUI divider implementation for IDEAMagic framework.
 * Renders DividerComponent from Core as native iOS separators.
 *
 * Features:
 * - Horizontal and vertical dividers
 * - Thickness customization
 * - Color customization
 * - Inset support
 * - Text label support (horizontal only)
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicDividerView: View {
    // MARK: - Properties

    let orientation: DividerOrientation
    let thickness: CGFloat
    let color: Color?
    let text: String?
    let inset: CGFloat

    // MARK: - Initialization

    public init(
        orientation: DividerOrientation = .horizontal,
        thickness: CGFloat = 1,
        color: Color? = nil,
        text: String? = nil,
        inset: CGFloat = 0
    ) {
        self.orientation = orientation
        self.thickness = thickness
        self.color = color
        self.text = text
        self.inset = inset
    }

    // MARK: - Body

    public var body: some View {
        Group {
            if let labelText = text, orientation == .horizontal {
                // Horizontal divider with text label
                HStack(spacing: 8) {
                    dividerLine
                        .frame(maxWidth: .infinity)

                    Text(labelText)
                        .font(.caption)
                        .foregroundColor(.secondary)

                    dividerLine
                        .frame(maxWidth: .infinity)
                }
                .padding(.horizontal, inset)
            } else {
                // Simple divider without text
                dividerLine
                    .padding(edgeInsets)
            }
        }
        .accessibilityLabel(text ?? "Divider")
        .accessibilityAddTraits(.isStaticText)
    }

    // MARK: - Divider Line

    private var dividerLine: some View {
        Rectangle()
            .fill(dividerColor)
            .frame(
                width: orientation == .vertical ? thickness : nil,
                height: orientation == .horizontal ? thickness : nil
            )
    }

    // MARK: - Computed Properties

    private var dividerColor: Color {
        color ?? Color(uiColor: .separator)
    }

    private var edgeInsets: EdgeInsets {
        switch orientation {
        case .horizontal:
            return EdgeInsets(top: 0, leading: inset, bottom: 0, trailing: inset)
        case .vertical:
            return EdgeInsets(top: inset, leading: 0, bottom: inset, trailing: 0)
        }
    }
}

// MARK: - Divider Orientation Enum

public enum DividerOrientation {
    case horizontal
    case vertical
}

// MARK: - Preview

#if DEBUG
struct MagicDividerView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Basic horizontal divider
                VStack(alignment: .leading, spacing: 8) {
                    Text("Basic Horizontal Divider").font(.headline)

                    Text("Content above")
                    MagicDividerView()
                    Text("Content below")
                }

                // Horizontal divider with text
                VStack(alignment: .leading, spacing: 8) {
                    Text("Divider with Text").font(.headline)

                    Text("Section 1 content")
                    MagicDividerView(text: "OR")
                    Text("Section 2 content")
                }

                // Different thicknesses
                VStack(alignment: .leading, spacing: 8) {
                    Text("Different Thicknesses").font(.headline)

                    MagicDividerView(thickness: 1)
                    Text("1pt").font(.caption).foregroundColor(.secondary)

                    MagicDividerView(thickness: 2)
                    Text("2pt").font(.caption).foregroundColor(.secondary)

                    MagicDividerView(thickness: 4)
                    Text("4pt").font(.caption).foregroundColor(.secondary)

                    MagicDividerView(thickness: 8)
                    Text("8pt").font(.caption).foregroundColor(.secondary)
                }

                // Different colors
                VStack(alignment: .leading, spacing: 8) {
                    Text("Colored Dividers").font(.headline)

                    MagicDividerView(color: .red)
                    MagicDividerView(color: .blue)
                    MagicDividerView(color: .green)
                    MagicDividerView(color: .orange)
                    MagicDividerView(color: .purple)
                }

                // With insets
                VStack(alignment: .leading, spacing: 8) {
                    Text("Dividers with Insets").font(.headline)

                    MagicDividerView(inset: 0)
                    Text("No inset").font(.caption).foregroundColor(.secondary)

                    MagicDividerView(inset: 16)
                    Text("16pt inset").font(.caption).foregroundColor(.secondary)

                    MagicDividerView(inset: 32)
                    Text("32pt inset").font(.caption).foregroundColor(.secondary)
                }

                // Vertical dividers
                VStack(alignment: .leading, spacing: 8) {
                    Text("Vertical Dividers").font(.headline)

                    HStack(spacing: 0) {
                        Text("Left")
                            .frame(maxWidth: .infinity)

                        MagicDividerView(
                            orientation: .vertical,
                            thickness: 1
                        )
                        .frame(height: 60)

                        Text("Middle")
                            .frame(maxWidth: .infinity)

                        MagicDividerView(
                            orientation: .vertical,
                            thickness: 2,
                            color: .blue
                        )
                        .frame(height: 60)

                        Text("Right")
                            .frame(maxWidth: .infinity)
                    }
                }

                // Section dividers with text
                VStack(alignment: .leading, spacing: 8) {
                    Text("Section Dividers").font(.headline)

                    VStack(spacing: 16) {
                        VStack(spacing: 8) {
                            Text("Login with email")
                            // Email/password form would go here
                        }

                        MagicDividerView(text: "OR", thickness: 1)

                        VStack(spacing: 8) {
                            Text("Login with social")
                            // Social login buttons would go here
                        }
                    }
                }

                // List separators
                VStack(alignment: .leading, spacing: 8) {
                    Text("List Separators").font(.headline)

                    VStack(spacing: 0) {
                        HStack {
                            Text("Item 1")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 12)

                        MagicDividerView(inset: 16)

                        HStack {
                            Text("Item 2")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 12)

                        MagicDividerView(inset: 16)

                        HStack {
                            Text("Item 3")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 12)
                    }
                }

                // Toolbar dividers
                VStack(alignment: .leading, spacing: 8) {
                    Text("Toolbar Dividers").font(.headline)

                    HStack(spacing: 0) {
                        Button(action: {}) {
                            Image(systemName: "bold")
                                .frame(width: 44, height: 44)
                        }

                        MagicDividerView(orientation: .vertical)
                            .frame(height: 24)

                        Button(action: {}) {
                            Image(systemName: "italic")
                                .frame(width: 44, height: 44)
                        }

                        MagicDividerView(orientation: .vertical)
                            .frame(height: 24)

                        Button(action: {}) {
                            Image(systemName: "underline")
                                .frame(width: 44, height: 44)
                        }

                        MagicDividerView(orientation: .vertical)
                            .frame(height: 24)

                        Button(action: {}) {
                            Image(systemName: "strikethrough")
                                .frame(width: 44, height: 44)
                        }
                    }
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(8)
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}
#endif
