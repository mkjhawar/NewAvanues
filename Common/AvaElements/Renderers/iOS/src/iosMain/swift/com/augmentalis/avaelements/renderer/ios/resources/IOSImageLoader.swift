import SwiftUI
import UIKit
import Foundation

/// iOS image loader with caching and transformation support
///
/// Provides utilities for loading, caching, and transforming images from multiple sources:
/// - Network URLs (with memory and disk caching)
/// - Local assets
/// - Base64-encoded data
/// - Data buffers
///
/// **Features:**
/// - Automatic memory and disk caching
/// - Placeholder and error state handling
/// - Image transformations (resize, crop, tint)
/// - Progressive loading support
/// - Thread-safe operations
///
/// **Integration:**
/// Works seamlessly with SwiftUI's `AsyncImage` and provides custom `CachedAsyncImage` component.
///
/// @since 3.0.0-flutter-parity
public actor IOSImageLoader {

    // MARK: - Singleton

    public static let shared = IOSImageLoader()

    // MARK: - Properties

    /// Memory cache for images
    private var imageCache: NSCache<NSString, UIImage>

    /// URL session for network requests
    private var urlSession: URLSession

    /// Cache statistics
    private var cacheStats: CacheStatistics

    // MARK: - Nested Types

    /// Image loading state
    public enum LoadState {
        case idle
        case loading(progress: Double)
        case success(image: UIImage)
        case failure(error: Error)
    }

    /// Image transformation options
    public struct TransformOptions {
        public var targetSize: CGSize?
        public var cropRect: CGRect?
        public var tintColor: UIColor?
        public var cornerRadius: CGFloat?
        public var contentMode: ContentMode

        public enum ContentMode {
            case fit
            case fill
            case center
        }

        public init(
            targetSize: CGSize? = nil,
            cropRect: CGRect? = nil,
            tintColor: UIColor? = nil,
            cornerRadius: CGFloat? = nil,
            contentMode: ContentMode = .fit
        ) {
            self.targetSize = targetSize
            self.cropRect = cropRect
            self.tintColor = tintColor
            self.cornerRadius = cornerRadius
            self.contentMode = contentMode
        }
    }

    /// Cache statistics
    public struct CacheStatistics {
        var memoryHits: Int = 0
        var memoryMisses: Int = 0
        var diskHits: Int = 0
        var diskMisses: Int = 0
        var totalRequests: Int = 0
        var failedRequests: Int = 0

        var overallHitRate: Double {
            guard totalRequests > 0 else { return 0.0 }
            return Double(memoryHits + diskHits) / Double(totalRequests)
        }

        var memoryHitRate: Double {
            guard totalRequests > 0 else { return 0.0 }
            return Double(memoryHits) / Double(totalRequests)
        }
    }

    // MARK: - Initialization

    private init() {
        // Configure memory cache
        self.imageCache = NSCache<NSString, UIImage>()
        self.imageCache.countLimit = 100
        self.imageCache.totalCostLimit = 100 * 1024 * 1024 // 100MB

        // Configure URL session with caching
        let config = URLSessionConfiguration.default
        config.urlCache = URLCache(
            memoryCapacity: 50 * 1024 * 1024, // 50MB memory
            diskCapacity: 200 * 1024 * 1024,  // 200MB disk
            diskPath: "com.augmentalis.avaelements.imagecache"
        )
        config.requestCachePolicy = .returnCacheDataElseLoad
        self.urlSession = URLSession(configuration: config)

        self.cacheStats = CacheStatistics()
    }

    // MARK: - Image Loading

    /// Load image from URL
    ///
    /// - Parameters:
    ///   - url: Image URL (string)
    ///   - transform: Optional transformation options
    ///   - placeholder: Optional placeholder image
    /// - Returns: Loaded (and optionally transformed) image
    public func loadImage(
        from url: String,
        transform: TransformOptions? = nil,
        placeholder: UIImage? = nil
    ) async throws -> UIImage {
        guard let imageURL = URL(string: url) else {
            throw ImageLoadError.invalidURL(url)
        }

        return try await loadImage(from: imageURL, transform: transform, placeholder: placeholder)
    }

    /// Load image from URL
    ///
    /// - Parameters:
    ///   - url: Image URL
    ///   - transform: Optional transformation options
    ///   - placeholder: Optional placeholder image
    /// - Returns: Loaded (and optionally transformed) image
    public func loadImage(
        from url: URL,
        transform: TransformOptions? = nil,
        placeholder: UIImage? = nil
    ) async throws -> UIImage {

        cacheStats.totalRequests += 1

        // Generate cache key
        let cacheKey = makeCacheKey(url: url, transform: transform)

        // Check memory cache
        if let cachedImage = imageCache.object(forKey: cacheKey as NSString) {
            cacheStats.memoryHits += 1
            return cachedImage
        }

        cacheStats.memoryMisses += 1

        // Check URL cache (disk cache)
        let request = URLRequest(url: url)
        if let cachedResponse = urlSession.configuration.urlCache?.cachedResponse(for: request),
           let image = UIImage(data: cachedResponse.data) {
            cacheStats.diskHits += 1

            // Apply transformations if needed
            let finalImage = transform != nil ? transformImage(image, options: transform!) : image

            // Store in memory cache
            imageCache.setObject(finalImage, forKey: cacheKey as NSString, cost: estimateImageSize(finalImage))

            return finalImage
        }

        cacheStats.diskMisses += 1

        // Download image
        do {
            let (data, response) = try await urlSession.data(from: url)

            guard let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode) else {
                cacheStats.failedRequests += 1
                throw ImageLoadError.httpError(statusCode: (response as? HTTPURLResponse)?.statusCode ?? 0)
            }

            guard let image = UIImage(data: data) else {
                cacheStats.failedRequests += 1
                throw ImageLoadError.invalidImageData
            }

            // Apply transformations if needed
            let finalImage = transform != nil ? transformImage(image, options: transform!) : image

            // Store in memory cache
            imageCache.setObject(finalImage, forKey: cacheKey as NSString, cost: estimateImageSize(finalImage))

            return finalImage

        } catch {
            cacheStats.failedRequests += 1
            throw ImageLoadError.networkError(error)
        }
    }

    /// Load image from base64 data
    ///
    /// - Parameters:
    ///   - base64String: Base64-encoded image data
    ///   - transform: Optional transformation options
    /// - Returns: Decoded (and optionally transformed) image
    public func loadImage(
        fromBase64 base64String: String,
        transform: TransformOptions? = nil
    ) async throws -> UIImage {
        cacheStats.totalRequests += 1

        guard let imageData = Data(base64Encoded: base64String),
              let image = UIImage(data: imageData) else {
            cacheStats.failedRequests += 1
            throw ImageLoadError.invalidImageData
        }

        // Apply transformations if needed
        let finalImage = transform != nil ? transformImage(image, options: transform!) : image

        return finalImage
    }

    /// Load image from asset bundle
    ///
    /// - Parameters:
    ///   - assetName: Asset name
    ///   - bundle: Bundle to search (defaults to main bundle)
    ///   - transform: Optional transformation options
    /// - Returns: Loaded (and optionally transformed) image
    public func loadImage(
        fromAsset assetName: String,
        bundle: Bundle = .main,
        transform: TransformOptions? = nil
    ) async throws -> UIImage {
        cacheStats.totalRequests += 1

        guard let image = UIImage(named: assetName, in: bundle, compatibleWith: nil) else {
            cacheStats.failedRequests += 1
            throw ImageLoadError.assetNotFound(assetName)
        }

        // Apply transformations if needed
        let finalImage = transform != nil ? transformImage(image, options: transform!) : image

        return finalImage
    }

    /// Preload images into cache
    ///
    /// - Parameter urls: List of image URLs to preload
    public func preloadImages(urls: [URL]) async {
        await withTaskGroup(of: Void.self) { group in
            for url in urls {
                group.addTask {
                    _ = try? await self.loadImage(from: url)
                }
            }
        }
    }

    /// Clear image cache
    ///
    /// - Parameter memoryOnly: If true, only clear memory cache
    public func clearCache(memoryOnly: Bool = false) {
        imageCache.removeAllObjects()

        if !memoryOnly {
            urlSession.configuration.urlCache?.removeAllCachedResponses()
        }

        cacheStats = CacheStatistics()
    }

    /// Get cache statistics
    public func getCacheStats() -> CacheStatistics {
        return cacheStats
    }

    // MARK: - Image Transformation

    private func transformImage(_ image: UIImage, options: TransformOptions) -> UIImage {
        var currentImage = image

        // Apply crop if specified
        if let cropRect = options.cropRect {
            currentImage = cropImage(currentImage, toRect: cropRect)
        }

        // Apply resize if specified
        if let targetSize = options.targetSize {
            currentImage = resizeImage(currentImage, targetSize: targetSize, contentMode: options.contentMode)
        }

        // Apply tint if specified
        if let tintColor = options.tintColor {
            currentImage = tintImage(currentImage, color: tintColor)
        }

        // Apply corner radius if specified
        if let cornerRadius = options.cornerRadius {
            currentImage = roundCorners(currentImage, radius: cornerRadius)
        }

        return currentImage
    }

    /// SECURITY FIX: Added @MainActor for UIGraphics thread safety
    @MainActor
    private func resizeImage(_ image: UIImage, targetSize: CGSize, contentMode: TransformOptions.ContentMode) -> UIImage {
        let size = image.size

        var newSize: CGSize
        switch contentMode {
        case .fit:
            let widthRatio = targetSize.width / size.width
            let heightRatio = targetSize.height / size.height
            let ratio = min(widthRatio, heightRatio)
            newSize = CGSize(width: size.width * ratio, height: size.height * ratio)

        case .fill:
            let widthRatio = targetSize.width / size.width
            let heightRatio = targetSize.height / size.height
            let ratio = max(widthRatio, heightRatio)
            newSize = CGSize(width: size.width * ratio, height: size.height * ratio)

        case .center:
            newSize = targetSize
        }

        let rect = CGRect(x: 0, y: 0, width: newSize.width, height: newSize.height)

        UIGraphicsBeginImageContextWithOptions(newSize, false, 0.0)
        defer { UIGraphicsEndImageContext() }

        image.draw(in: rect)
        return UIGraphicsGetImageFromCurrentImageContext() ?? image
    }

    private func cropImage(_ image: UIImage, toRect rect: CGRect) -> UIImage {
        guard let cgImage = image.cgImage?.cropping(to: rect) else {
            return image
        }
        return UIImage(cgImage: cgImage, scale: image.scale, orientation: image.imageOrientation)
    }

    /// SECURITY FIX: Removed force unwrapping, added @MainActor for UIGraphics thread safety
    @MainActor
    private func tintImage(_ image: UIImage, color: UIColor) -> UIImage {
        UIGraphicsBeginImageContextWithOptions(image.size, false, image.scale)
        defer { UIGraphicsEndImageContext() }

        color.set()

        // SECURITY FIX: Use guard instead of force unwrap
        guard let context = UIGraphicsGetCurrentContext(),
              let cgImage = image.cgImage else {
            return image  // Return original image on failure
        }

        context.translateBy(x: 0, y: image.size.height)
        context.scaleBy(x: 1.0, y: -1.0)
        context.setBlendMode(.normal)

        let rect = CGRect(x: 0, y: 0, width: image.size.width, height: image.size.height)
        context.clip(to: rect, mask: cgImage)
        context.fill(rect)

        // SECURITY FIX: Safe unwrap with fallback
        return UIGraphicsGetImageFromCurrentImageContext() ?? image
    }

    /// SECURITY FIX: Removed force unwrapping, added @MainActor for UIGraphics thread safety
    @MainActor
    private func roundCorners(_ image: UIImage, radius: CGFloat) -> UIImage {
        UIGraphicsBeginImageContextWithOptions(image.size, false, image.scale)
        defer { UIGraphicsEndImageContext() }

        let rect = CGRect(x: 0, y: 0, width: image.size.width, height: image.size.height)
        let path = UIBezierPath(roundedRect: rect, cornerRadius: radius)
        path.addClip()

        image.draw(in: rect)

        // SECURITY FIX: Safe unwrap with fallback
        return UIGraphicsGetImageFromCurrentImageContext() ?? image
    }

    // MARK: - Cache Utilities

    private func makeCacheKey(url: URL, transform: TransformOptions?) -> String {
        var key = url.absoluteString

        if let transform = transform {
            if let size = transform.targetSize {
                key += ":\(size.width)x\(size.height)"
            }
            if let crop = transform.cropRect {
                key += ":crop(\(crop.origin.x),\(crop.origin.y),\(crop.size.width),\(crop.size.height))"
            }
            if let tint = transform.tintColor {
                key += ":tint(\(tint.hexString))"
            }
            if let radius = transform.cornerRadius {
                key += ":radius(\(radius))"
            }
            key += ":mode(\(transform.contentMode))"
        }

        return key
    }

    private func estimateImageSize(_ image: UIImage) -> Int {
        guard let cgImage = image.cgImage else { return 0 }
        let bytesPerPixel = 4 // RGBA
        return cgImage.width * cgImage.height * bytesPerPixel
    }
}

// MARK: - Error Types

public enum ImageLoadError: Error, LocalizedError {
    case invalidURL(String)
    case invalidImageData
    case httpError(statusCode: Int)
    case networkError(Error)
    case assetNotFound(String)

    public var errorDescription: String? {
        switch self {
        case .invalidURL(let url):
            return "Invalid image URL: \(url)"
        case .invalidImageData:
            return "Failed to decode image data"
        case .httpError(let statusCode):
            return "HTTP error: \(statusCode)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .assetNotFound(let name):
            return "Asset not found: \(name)"
        }
    }
}

// MARK: - SwiftUI Integration

/// Cached async image view with placeholder and error handling
public struct CachedAsyncImage<Content: View, Placeholder: View, ErrorView: View>: View {
    let url: URL?
    let transform: IOSImageLoader.TransformOptions?
    let content: (Image) -> Content
    let placeholder: () -> Placeholder
    let errorView: (Error) -> ErrorView

    @State private var loadState: IOSImageLoader.LoadState = .idle
    /// IOS-P1-1 FIX: Track task for cancellation when view disappears
    @State private var loadTask: Task<Void, Never>?

    public init(
        url: URL?,
        transform: IOSImageLoader.TransformOptions? = nil,
        @ViewBuilder content: @escaping (Image) -> Content,
        @ViewBuilder placeholder: @escaping () -> Placeholder,
        @ViewBuilder error: @escaping (Error) -> ErrorView
    ) {
        self.url = url
        self.transform = transform
        self.content = content
        self.placeholder = placeholder
        self.errorView = error
    }

    public var body: some View {
        Group {
            switch loadState {
            case .idle, .loading:
                placeholder()
                    .onAppear {
                        // IOS-P1-1 FIX: Store task reference for cancellation
                        loadTask = Task {
                            // Check for cancellation before starting
                            guard !Task.isCancelled else { return }
                            await loadImage()
                        }
                    }
                    .onDisappear {
                        // IOS-P1-1 FIX: Cancel task when view disappears
                        loadTask?.cancel()
                        loadTask = nil
                    }

            case .success(let image):
                content(Image(uiImage: image))

            case .failure(let error):
                errorView(error)
            }
        }
    }

    private func loadImage() async {
        guard let url = url else {
            loadState = .failure(ImageLoadError.invalidURL("nil"))
            return
        }

        // IOS-P1-1 FIX: Check for cancellation before loading
        guard !Task.isCancelled else { return }

        loadState = .loading(progress: 0.0)

        do {
            let image = try await IOSImageLoader.shared.loadImage(from: url, transform: transform)
            // IOS-P1-1 FIX: Check for cancellation before updating state
            guard !Task.isCancelled else { return }
            loadState = .success(image: image)
        } catch {
            // IOS-P1-1 FIX: Don't update state if cancelled
            guard !Task.isCancelled else { return }
            loadState = .failure(error)
        }
    }
}

// MARK: - Convenience Initializers

extension CachedAsyncImage where Placeholder == ProgressView<EmptyView, EmptyView>,
                                  ErrorView == Image {
    public init(
        url: URL?,
        transform: IOSImageLoader.TransformOptions? = nil,
        @ViewBuilder content: @escaping (Image) -> Content
    ) {
        self.init(
            url: url,
            transform: transform,
            content: content,
            placeholder: { ProgressView() },
            error: { _ in Image(systemName: "exclamationmark.triangle") }
        )
    }
}

// MARK: - UIColor Extension

private extension UIColor {
    var hexString: String {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        getRed(&r, green: &g, blue: &b, alpha: &a)
        return String(format: "%02X%02X%02X%02X",
                     Int(r * 255), Int(g * 255), Int(b * 255), Int(a * 255))
    }
}
