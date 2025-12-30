import SwiftUI

/**
 * MagicScrollViewView - Native iOS ScrollView
 *
 * Native SwiftUI scroll view implementation for IDEAMagic framework.
 * Renders ScrollViewComponent from Core as native iOS ScrollView.
 *
 * Features:
 * - Vertical and horizontal scrolling
 * - Scroll indicators
 * - Bounce effect (iOS native)
 * - Content padding
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicScrollViewView<Content: View>: View {
    // MARK: - Properties

    let axis: Axis.Set
    let showsIndicators: Bool
    let contentPadding: CGFloat
    let content: Content

    // MARK: - Initialization

    public init(
        axis: Axis.Set = .vertical,
        showsIndicators: Bool = true,
        contentPadding: CGFloat = 0,
        @ViewBuilder content: () -> Content
    ) {
        self.axis = axis
        self.showsIndicators = showsIndicators
        self.contentPadding = contentPadding
        self.content = content()
    }

    // MARK: - Body

    public var body: some View {
        ScrollView(axis, showsIndicators: showsIndicators) {
            if axis == .vertical {
                VStack {
                    content
                }
                .padding(contentPadding)
            } else if axis == .horizontal {
                HStack {
                    content
                }
                .padding(contentPadding)
            } else {
                // Both axes
                content
                    .padding(contentPadding)
            }
        }
    }
}

// MARK: - Preview

#if DEBUG
struct MagicScrollViewView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 24) {
            // Vertical scroll view
            VStack(alignment: .leading, spacing: 8) {
                Text("Vertical ScrollView").font(.headline)

                MagicScrollViewView(axis: .vertical, contentPadding: 16) {
                    ForEach(1...20, id: \.self) { index in
                        HStack {
                            Image(systemName: "star.fill")
                                .foregroundColor(.yellow)
                            Text("Item \(index)")
                            Spacer()
                        }
                        .padding()
                        .background(Color(uiColor: .secondarySystemBackground))
                        .cornerRadius(8)
                    }
                }
                .frame(height: 300)
                .background(Color(uiColor: .systemBackground))
                .cornerRadius(12)
            }

            Divider()

            // Horizontal scroll view
            VStack(alignment: .leading, spacing: 8) {
                Text("Horizontal ScrollView").font(.headline)

                MagicScrollViewView(axis: .horizontal, contentPadding: 16) {
                    ForEach(1...10, id: \.self) { index in
                        VStack {
                            Image(systemName: "photo.fill")
                                .font(.system(size: 40))
                                .foregroundColor(.blue)
                            Text("Card \(index)")
                                .font(.caption)
                        }
                        .frame(width: 120, height: 120)
                        .background(Color(uiColor: .secondarySystemBackground))
                        .cornerRadius(12)
                    }
                }
                .frame(height: 150)
            }

            Divider()

            // No indicators
            VStack(alignment: .leading, spacing: 8) {
                Text("Without Scroll Indicators").font(.headline)

                MagicScrollViewView(
                    axis: .vertical,
                    showsIndicators: false,
                    contentPadding: 16
                ) {
                    ForEach(1...10, id: \.self) { index in
                        Text("Item \(index)")
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(uiColor: .secondarySystemBackground))
                            .cornerRadius(8)
                    }
                }
                .frame(height: 200)
            }

            Divider()

            // Grid in horizontal scroll
            VStack(alignment: .leading, spacing: 8) {
                Text("Horizontal Grid").font(.headline)

                MagicScrollViewView(axis: .horizontal, contentPadding: 16) {
                    ForEach(1...5, id: \.self) { column in
                        VStack(spacing: 8) {
                            ForEach(1...3, id: \.self) { row in
                                VStack(spacing: 4) {
                                    Image(systemName: "square.fill")
                                        .foregroundColor(.blue)
                                    Text("\(column),\(row)")
                                        .font(.caption)
                                }
                                .frame(width: 80, height: 80)
                                .background(Color(uiColor: .secondarySystemBackground))
                                .cornerRadius(8)
                            }
                        }
                    }
                }
                .frame(height: 280)
            }

            Divider()

            // Content with different heights
            VStack(alignment: .leading, spacing: 8) {
                Text("Variable Heights").font(.headline)

                MagicScrollViewView(axis: .vertical, contentPadding: 16) {
                    VStack(spacing: 8) {
                        Text("Short item")
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.red.opacity(0.2))
                            .cornerRadius(8)

                        Text("Medium item\nWith multiple lines\nOf content here")
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.green.opacity(0.2))
                            .cornerRadius(8)

                        Text("Tall item\nWith even more\nLines of content\nTo demonstrate\nVariable heights")
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.blue.opacity(0.2))
                            .cornerRadius(8)

                        Text("Another short item")
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.orange.opacity(0.2))
                            .cornerRadius(8)
                    }
                }
                .frame(height: 250)
            }

            Divider()

            // Form in scroll view
            VStack(alignment: .leading, spacing: 8) {
                Text("Form Layout").font(.headline)

                MagicScrollViewView(axis: .vertical, contentPadding: 16) {
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Profile Settings")
                            .font(.title2)
                            .fontWeight(.bold)

                        VStack(alignment: .leading, spacing: 4) {
                            Text("Full Name").font(.caption).foregroundColor(.secondary)
                            TextField("Enter name", text: .constant(""))
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text("Email").font(.caption).foregroundColor(.secondary)
                            TextField("email@example.com", text: .constant(""))
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text("Bio").font(.caption).foregroundColor(.secondary)
                            TextEditor(text: .constant(""))
                                .frame(height: 100)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(Color(uiColor: .separator), lineWidth: 1)
                                )
                        }

                        Toggle("Email Notifications", isOn: .constant(true))
                        Toggle("Push Notifications", isOn: .constant(false))

                        Button("Save Changes") {}
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                }
                .frame(height: 350)
                .background(Color(uiColor: .secondarySystemBackground))
                .cornerRadius(12)
            }

            Divider()

            // Image gallery
            VStack(alignment: .leading, spacing: 8) {
                Text("Image Gallery").font(.headline)

                MagicScrollViewView(axis: .horizontal, contentPadding: 16) {
                    ForEach(1...8, id: \.self) { index in
                        ZStack {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.blue.opacity(0.3))
                                .frame(width: 200, height: 150)

                            VStack {
                                Image(systemName: "photo.fill")
                                    .font(.system(size: 40))
                                    .foregroundColor(.white)
                                Text("Photo \(index)")
                                    .foregroundColor(.white)
                            }
                        }
                    }
                }
                .frame(height: 170)
            }
        }
        .padding()
        .previewLayout(.sizeThatFits)
    }
}
#endif
