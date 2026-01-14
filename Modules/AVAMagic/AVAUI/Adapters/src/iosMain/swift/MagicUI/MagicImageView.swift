import SwiftUI

/**
 * MagicImageView - Native iOS Image
 *
 * Native SwiftUI image implementation for IDEAMagic framework.
 * Renders ImageComponent from Core as native iOS image views.
 *
 * Features:
 * - AsyncImage for remote URLs
 * - Local image support
 * - Content modes (Fill, Fit, Stretch)
 * - Placeholder support
 * - Error handling
 * - Loading indicators
 * - Corner radius
 * - Accessibility support
 * - Dark mode compatible
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicImageView: View {
    // MARK: - Properties

    let imageSource: ImageSource
    let contentMode: ImageContentMode
    let cornerRadius: CGFloat
    let width: CGFloat?
    let height: CGFloat?
    let contentDescription: String?

    // MARK: - Initialization

    public init(
        imageSource: ImageSource,
        contentMode: ImageContentMode = .fit,
        cornerRadius: CGFloat = 0,
        width: CGFloat? = nil,
        height: CGFloat? = nil,
        contentDescription: String? = nil
    ) {
        self.imageSource = imageSource
        self.contentMode = contentMode
        self.cornerRadius = cornerRadius
        self.width = width
        self.height = height
        self.contentDescription = contentDescription
    }

    // MARK: - Body

    public var body: some View {
        Group {
            switch imageSource {
            case .url(let urlString):
                asyncImageView(urlString: urlString)
            case .asset(let assetName):
                localImageView(assetName: assetName)
            case .systemIcon(let iconName):
                systemImageView(iconName: iconName)
            }
        }
        .frame(width: width, height: height)
        .cornerRadius(cornerRadius)
        .accessibilityLabel(contentDescription ?? "Image")
    }

    // MARK: - Image Views

    private func asyncImageView(urlString: String) -> some View {
        AsyncImage(url: URL(string: urlString)) { phase in
            switch phase {
            case .empty:
                placeholderView()
            case .success(let image):
                imageContent(image: image)
            case .failure:
                errorView()
            @unknown default:
                placeholderView()
            }
        }
    }

    private func localImageView(assetName: String) -> some View {
        imageContent(image: Image(assetName))
    }

    private func systemImageView(iconName: String) -> some View {
        imageContent(image: Image(systemName: iconName))
    }

    private func imageContent(image: Image) -> some View {
        image
            .resizable()
            .aspectRatio(contentMode: aspectRatioForMode)
    }

    private func placeholderView() -> some View {
        ZStack {
            Color(uiColor: .secondarySystemBackground)

            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
        }
    }

    private func errorView() -> some View {
        ZStack {
            Color(uiColor: .secondarySystemBackground)

            VStack(spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 32))
                    .foregroundColor(.secondary)
                Text("Failed to load image")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }

    // MARK: - Content Mode Mapping

    private var aspectRatioForMode: ContentMode {
        switch contentMode {
        case .fill:
            return .fill
        case .fit:
            return .fit
        case .stretch:
            return .fill
        }
    }
}

// MARK: - Image Source Enum

public enum ImageSource {
    case url(String)
    case asset(String)
    case systemIcon(String)
}

// MARK: - Image Content Mode Enum

public enum ImageContentMode {
    case fill
    case fit
    case stretch
}

// MARK: - Preview

#if DEBUG
struct MagicImageView_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // System icon image
                VStack(alignment: .leading, spacing: 8) {
                    Text("System Icon").font(.headline)
                    MagicImageView(
                        imageSource: .systemIcon("photo.fill"),
                        width: 100,
                        height: 100,
                        contentDescription: "Photo icon"
                    )
                }

                Divider()

                // Content modes
                VStack(alignment: .leading, spacing: 8) {
                    Text("Content Modes").font(.headline)

                    HStack(spacing: 16) {
                        VStack {
                            MagicImageView(
                                imageSource: .systemIcon("photo.fill"),
                                contentMode: .fit,
                                width: 80,
                                height: 80
                            )
                            Text("Fit").font(.caption)
                        }

                        VStack {
                            MagicImageView(
                                imageSource: .systemIcon("photo.fill"),
                                contentMode: .fill,
                                width: 80,
                                height: 80
                            )
                            .clipped()
                            Text("Fill").font(.caption)
                        }
                    }
                }

                Divider()

                // Corner radius
                VStack(alignment: .leading, spacing: 8) {
                    Text("Corner Radius").font(.headline)

                    HStack(spacing: 16) {
                        VStack {
                            Rectangle()
                                .fill(Color.blue.opacity(0.3))
                                .frame(width: 80, height: 80)
                                .cornerRadius(0)
                                .overlay(
                                    MagicImageView(
                                        imageSource: .systemIcon("star.fill"),
                                        width: 40,
                                        height: 40
                                    )
                                )
                            Text("0").font(.caption)
                        }

                        VStack {
                            Rectangle()
                                .fill(Color.blue.opacity(0.3))
                                .frame(width: 80, height: 80)
                                .cornerRadius(8)
                                .overlay(
                                    MagicImageView(
                                        imageSource: .systemIcon("star.fill"),
                                        width: 40,
                                        height: 40
                                    )
                                )
                            Text("8").font(.caption)
                        }

                        VStack {
                            Rectangle()
                                .fill(Color.blue.opacity(0.3))
                                .frame(width: 80, height: 80)
                                .cornerRadius(16)
                                .overlay(
                                    MagicImageView(
                                        imageSource: .systemIcon("star.fill"),
                                        width: 40,
                                        height: 40
                                    )
                                )
                            Text("16").font(.caption)
                        }

                        VStack {
                            Rectangle()
                                .fill(Color.blue.opacity(0.3))
                                .frame(width: 80, height: 80)
                                .cornerRadius(40)
                                .overlay(
                                    MagicImageView(
                                        imageSource: .systemIcon("star.fill"),
                                        width: 40,
                                        height: 40
                                    )
                                )
                            Text("Circle").font(.caption)
                        }
                    }
                }

                Divider()

                // Sizes
                VStack(alignment: .leading, spacing: 8) {
                    Text("Different Sizes").font(.headline)

                    HStack(spacing: 16) {
                        MagicImageView(
                            imageSource: .systemIcon("heart.fill"),
                            width: 40,
                            height: 40
                        )

                        MagicImageView(
                            imageSource: .systemIcon("heart.fill"),
                            width: 60,
                            height: 60
                        )

                        MagicImageView(
                            imageSource: .systemIcon("heart.fill"),
                            width: 80,
                            height: 80
                        )

                        MagicImageView(
                            imageSource: .systemIcon("heart.fill"),
                            width: 100,
                            height: 100
                        )
                    }
                }

                Divider()

                // Placeholder example
                VStack(alignment: .leading, spacing: 8) {
                    Text("Loading State").font(.headline)

                    ZStack {
                        Color(uiColor: .secondarySystemBackground)
                            .frame(width: 200, height: 150)
                            .cornerRadius(8)

                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle())
                    }
                }

                Divider()

                // Error example
                VStack(alignment: .leading, spacing: 8) {
                    Text("Error State").font(.headline)

                    ZStack {
                        Color(uiColor: .secondarySystemBackground)
                            .frame(width: 200, height: 150)
                            .cornerRadius(8)

                        VStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .font(.system(size: 32))
                                .foregroundColor(.secondary)
                            Text("Failed to load image")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Divider()

                // Remote image example (will show loading)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Remote Image (with AsyncImage)").font(.headline)

                    MagicImageView(
                        imageSource: .url("https://picsum.photos/300/200"),
                        contentMode: .fill,
                        cornerRadius: 8,
                        width: 300,
                        height: 200,
                        contentDescription: "Random placeholder image"
                    )
                }
            }
            .padding()
        }
        .previewLayout(.sizeThatFits)
    }
}
#endif
