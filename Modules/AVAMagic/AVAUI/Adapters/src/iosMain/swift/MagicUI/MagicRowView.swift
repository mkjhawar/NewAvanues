import SwiftUI

/**
 * MagicRowView - Native iOS HStack
 *
 * Native SwiftUI horizontal stack implementation for IDEAMagic framework.
 * Renders RowComponent from Core as native iOS HStack layout.
 *
 * Features:
 * - Horizontal alignment (Top, Center, Bottom)
 * - Spacing customization
 * - Padding support
 * - Background color
 * - Corner radius
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicRowView<Content: View>: View {
    // MARK: - Properties

    let alignment: VerticalAlignment
    let spacing: CGFloat
    let padding: CGFloat
    let backgroundColor: Color?
    let cornerRadius: CGFloat
    let content: Content

    // MARK: - Initialization

    public init(
        alignment: VerticalAlignment = .center,
        spacing: CGFloat = 8,
        padding: CGFloat = 0,
        backgroundColor: Color? = nil,
        cornerRadius: CGFloat = 0,
        @ViewBuilder content: () -> Content
    ) {
        self.alignment = alignment
        self.spacing = spacing
        self.padding = padding
        self.backgroundColor = backgroundColor
        self.cornerRadius = cornerRadius
        self.content = content()
    }

    // MARK: - Body

    public var body: some View {
        HStack(alignment: alignment, spacing: spacing) {
            content
        }
        .padding(padding)
        .background(backgroundColor)
        .cornerRadius(cornerRadius)
    }
}

// MARK: - Preview

#if DEBUG
struct MagicRowView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Basic row
                VStack(alignment: .leading, spacing: 8) {
                    Text("Basic Row").font(.headline)

                    MagicRowView {
                        Text("Item 1")
                        Text("Item 2")
                        Text("Item 3")
                    }
                }

                Divider()

                // Different alignments
                VStack(alignment: .leading, spacing: 8) {
                    Text("Alignments").font(.headline)

                    // Top
                    MagicRowView(alignment: .top, backgroundColor: Color.blue.opacity(0.1), cornerRadius: 8) {
                        Text("Top\nMultiple\nLines")
                        Text("Top")
                        Image(systemName: "star.fill")
                    }

                    // Center
                    MagicRowView(alignment: .center, backgroundColor: Color.green.opacity(0.1), cornerRadius: 8) {
                        Text("Center\nMultiple\nLines")
                        Text("Center")
                        Image(systemName: "heart.fill")
                    }

                    // Bottom
                    MagicRowView(alignment: .bottom, backgroundColor: Color.orange.opacity(0.1), cornerRadius: 8) {
                        Text("Bottom\nMultiple\nLines")
                        Text("Bottom")
                        Image(systemName: "square.fill")
                    }
                }

                Divider()

                // Different spacing
                VStack(alignment: .leading, spacing: 8) {
                    Text("Spacing Variants").font(.headline)

                    MagicRowView(spacing: 0, backgroundColor: Color.gray.opacity(0.1), cornerRadius: 8) {
                        Text("Spacing")
                        Text("0")
                        Text("pixels")
                    }

                    MagicRowView(spacing: 8, backgroundColor: Color.gray.opacity(0.1), cornerRadius: 8) {
                        Text("Spacing")
                        Text("8")
                        Text("pixels")
                    }

                    MagicRowView(spacing: 16, backgroundColor: Color.gray.opacity(0.1), cornerRadius: 8) {
                        Text("Spacing")
                        Text("16")
                        Text("pixels")
                    }
                }

                Divider()

                // With padding
                VStack(alignment: .leading, spacing: 8) {
                    Text("With Padding").font(.headline)

                    MagicRowView(padding: 16, backgroundColor: Color.purple.opacity(0.1), cornerRadius: 8) {
                        Image(systemName: "star.fill")
                        Text("Padded content")
                        Image(systemName: "star.fill")
                    }
                }

                Divider()

                // Icon + Text patterns
                VStack(alignment: .leading, spacing: 8) {
                    Text("Icon + Text Patterns").font(.headline)

                    MagicRowView(spacing: 8, padding: 12, backgroundColor: Color(uiColor: .secondarySystemBackground), cornerRadius: 8) {
                        Image(systemName: "envelope.fill")
                            .foregroundColor(.blue)
                        Text("Messages")
                    }

                    MagicRowView(spacing: 8, padding: 12, backgroundColor: Color(uiColor: .secondarySystemBackground), cornerRadius: 8) {
                        Image(systemName: "person.fill")
                            .foregroundColor(.green)
                        Text("Profile")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                    }

                    MagicRowView(spacing: 8, padding: 12, backgroundColor: Color(uiColor: .secondarySystemBackground), cornerRadius: 8) {
                        Image(systemName: "gear")
                            .foregroundColor(.gray)
                        Text("Settings")
                        Spacer()
                        Toggle("", isOn: .constant(true))
                    }
                }

                Divider()

                // Button groups
                VStack(alignment: .leading, spacing: 8) {
                    Text("Button Groups").font(.headline)

                    MagicRowView(spacing: 8) {
                        Button("Cancel") {}
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(uiColor: .secondarySystemBackground))
                            .cornerRadius(8)

                        Button("Save") {}
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }

                    MagicRowView(spacing: 12) {
                        Button(action: {}) {
                            Image(systemName: "heart")
                                .frame(width: 44, height: 44)
                        }
                        Button(action: {}) {
                            Image(systemName: "star")
                                .frame(width: 44, height: 44)
                        }
                        Button(action: {}) {
                            Image(systemName: "bookmark")
                                .frame(width: 44, height: 44)
                        }
                        Button(action: {}) {
                            Image(systemName: "square.and.arrow.up")
                                .frame(width: 44, height: 44)
                        }
                    }
                }

                Divider()

                // Stat cards
                VStack(alignment: .leading, spacing: 8) {
                    Text("Stat Cards").font(.headline)

                    MagicRowView(spacing: 12) {
                        VStack(spacing: 4) {
                            Text("123")
                                .font(.title)
                                .fontWeight(.bold)
                            Text("Followers")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(uiColor: .secondarySystemBackground))
                        .cornerRadius(12)

                        VStack(spacing: 4) {
                            Text("456")
                                .font(.title)
                                .fontWeight(.bold)
                            Text("Following")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(uiColor: .secondarySystemBackground))
                        .cornerRadius(12)

                        VStack(spacing: 4) {
                            Text("789")
                                .font(.title)
                                .fontWeight(.bold)
                            Text("Posts")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(uiColor: .secondarySystemBackground))
                        .cornerRadius(12)
                    }
                }

                Divider()

                // Toolbar
                VStack(alignment: .leading, spacing: 8) {
                    Text("Toolbar").font(.headline)

                    MagicRowView(spacing: 0, padding: 8, backgroundColor: Color(uiColor: .secondarySystemBackground), cornerRadius: 8) {
                        Button(action: {}) {
                            Image(systemName: "bold")
                                .frame(width: 44, height: 44)
                        }
                        Button(action: {}) {
                            Image(systemName: "italic")
                                .frame(width: 44, height: 44)
                        }
                        Button(action: {}) {
                            Image(systemName: "underline")
                                .frame(width: 44, height: 44)
                        }

                        Spacer()

                        Button(action: {}) {
                            Image(systemName: "link")
                                .frame(width: 44, height: 44)
                        }
                        Button(action: {}) {
                            Image(systemName: "photo")
                                .frame(width: 44, height: 44)
                        }
                    }
                }

                Divider()

                // Nested rows and columns
                VStack(alignment: .leading, spacing: 8) {
                    Text("Nested Layout").font(.headline)

                    VStack(spacing: 12) {
                        MagicRowView(spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Profile")
                                    .font(.headline)
                                Text("John Doe")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding()
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(8)

                            VStack(alignment: .leading, spacing: 4) {
                                Text("Score")
                                    .font(.headline)
                                Text("95%")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding()
                            .background(Color.green.opacity(0.1))
                            .cornerRadius(8)
                        }
                    }
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}
#endif
