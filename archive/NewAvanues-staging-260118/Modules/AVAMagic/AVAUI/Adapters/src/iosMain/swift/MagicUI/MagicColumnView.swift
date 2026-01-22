import SwiftUI

/**
 * MagicColumnView - Native iOS VStack
 *
 * Native SwiftUI vertical stack implementation for IDEAMagic framework.
 * Renders ColumnComponent from Core as native iOS VStack layout.
 *
 * Features:
 * - Vertical alignment (Leading, Center, Trailing)
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
public struct MagicColumnView<Content: View>: View {
    // MARK: - Properties

    let alignment: HorizontalAlignment
    let spacing: CGFloat
    let padding: CGFloat
    let backgroundColor: Color?
    let cornerRadius: CGFloat
    let content: Content

    // MARK: - Initialization

    public init(
        alignment: HorizontalAlignment = .center,
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
        VStack(alignment: alignment, spacing: spacing) {
            content
        }
        .padding(padding)
        .background(backgroundColor)
        .cornerRadius(cornerRadius)
    }
}

// MARK: - Preview

#if DEBUG
struct MagicColumnView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Basic column
                VStack(alignment: .leading, spacing: 8) {
                    Text("Basic Column").font(.headline)

                    MagicColumnView {
                        Text("Item 1")
                        Text("Item 2")
                        Text("Item 3")
                    }
                }

                Divider()

                // Different alignments
                VStack(alignment: .leading, spacing: 8) {
                    Text("Alignments").font(.headline)

                    // Leading
                    MagicColumnView(alignment: .leading, backgroundColor: Color.blue.opacity(0.1), cornerRadius: 8) {
                        Text("Leading 1")
                        Text("Leading 2")
                        Text("Leading 3")
                    }

                    // Center
                    MagicColumnView(alignment: .center, backgroundColor: Color.green.opacity(0.1), cornerRadius: 8) {
                        Text("Center 1")
                        Text("Center 2")
                        Text("Center 3")
                    }

                    // Trailing
                    MagicColumnView(alignment: .trailing, backgroundColor: Color.orange.opacity(0.1), cornerRadius: 8) {
                        Text("Trailing 1")
                        Text("Trailing 2")
                        Text("Trailing 3")
                    }
                }

                Divider()

                // Different spacing
                VStack(alignment: .leading, spacing: 8) {
                    Text("Spacing Variants").font(.headline)

                    MagicColumnView(spacing: 0, backgroundColor: Color.gray.opacity(0.1), cornerRadius: 8) {
                        Text("Spacing 0")
                        Text("Spacing 0")
                        Text("Spacing 0")
                    }

                    MagicColumnView(spacing: 8, backgroundColor: Color.gray.opacity(0.1), cornerRadius: 8) {
                        Text("Spacing 8")
                        Text("Spacing 8")
                        Text("Spacing 8")
                    }

                    MagicColumnView(spacing: 16, backgroundColor: Color.gray.opacity(0.1), cornerRadius: 8) {
                        Text("Spacing 16")
                        Text("Spacing 16")
                        Text("Spacing 16")
                    }
                }

                Divider()

                // With padding
                VStack(alignment: .leading, spacing: 8) {
                    Text("With Padding").font(.headline)

                    MagicColumnView(padding: 16, backgroundColor: Color.purple.opacity(0.1), cornerRadius: 8) {
                        Text("Padded content 1")
                        Text("Padded content 2")
                        Text("Padded content 3")
                    }
                }

                Divider()

                // Nested columns
                VStack(alignment: .leading, spacing: 8) {
                    Text("Nested Columns").font(.headline)

                    MagicColumnView(padding: 16, backgroundColor: Color.blue.opacity(0.1), cornerRadius: 8) {
                        Text("Outer Column").font(.headline)

                        MagicColumnView(padding: 12, backgroundColor: Color.green.opacity(0.2), cornerRadius: 8) {
                            Text("Inner Column 1")
                            Text("Inner Column 2")
                        }

                        Text("Back to outer")
                    }
                }

                Divider()

                // Form layout
                VStack(alignment: .leading, spacing: 8) {
                    Text("Form Layout").font(.headline)

                    MagicColumnView(alignment: .leading, spacing: 16, padding: 16, backgroundColor: Color(uiColor: .secondarySystemBackground), cornerRadius: 12) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Email").font(.caption).foregroundColor(.secondary)
                            TextField("email@example.com", text: .constant(""))
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text("Password").font(.caption).foregroundColor(.secondary)
                            SecureField("Enter password", text: .constant(""))
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                        }

                        Button("Sign In") {}
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                }

                Divider()

                // Card-like layout
                VStack(alignment: .leading, spacing: 8) {
                    Text("Card Layout").font(.headline)

                    MagicColumnView(
                        alignment: .leading,
                        spacing: 12,
                        padding: 16,
                        backgroundColor: Color(uiColor: .secondarySystemBackground),
                        cornerRadius: 12
                    ) {
                        HStack {
                            Image(systemName: "star.fill")
                                .foregroundColor(.yellow)
                            Text("Featured Item").font(.title3).fontWeight(.semibold)
                        }

                        Text("This is a description of the featured item. It can contain multiple lines of text.")
                            .font(.body)
                            .foregroundColor(.secondary)

                        HStack(spacing: 8) {
                            Button("Learn More") {}
                                .padding(.horizontal, 16)
                                .padding(.vertical, 8)
                                .background(Color.accentColor)
                                .foregroundColor(.white)
                                .cornerRadius(8)

                            Button("Dismiss") {}
                                .padding(.horizontal, 16)
                                .padding(.vertical, 8)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(Color.accentColor, lineWidth: 1)
                                )
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
