import SwiftUI
import UIKit
import Foundation

/// iOS implementation of icon resource management
///
/// Manages loading, caching, and rendering of icons from multiple sources:
/// - **Material Icons** → SF Symbols mapping (326 icons)
/// - **Flutter Icons** → SF Symbols mapping (automatic conversion)
/// - **Asset Path** → Bundle resources
/// - **Network URL** → Remote images with caching
/// - **Base64** → Inline image data
///
/// **Caching Strategy:**
/// - Memory cache using NSCache (automatic eviction)
/// - Disk cache for network images (via URLCache)
/// - SF Symbols are system-cached (no additional caching needed)
///
/// **Thread Safety:**
/// All methods are thread-safe and can be called from any queue.
///
/// @since 3.0.0-flutter-parity
public actor IOSIconResourceManager {

    // MARK: - Singleton

    public static let shared = IOSIconResourceManager()

    // MARK: - Properties

    /// Memory cache for loaded icons
    private var iconCache: NSCache<NSString, AnyObject>

    /// Cache statistics
    private var cacheStats: CacheStatistics

    /// SEQ-3 FIX: Track active operations to prevent cache clear during loads
    private var activeOperations: Int = 0

    // MARK: - Nested Types

    /// Loaded icon representation
    public enum LoadedIcon {
        case sfSymbol(name: String)
        case image(UIImage)
        case url(URL)
    }

    /// Icon resource types (mirroring Kotlin IconResource)
    public enum IconResource {
        case materialIcon(name: String, variant: IconVariant)
        case flutterIcon(name: String)
        case assetPath(path: String)
        case networkURL(url: String, placeholder: IconResource?, errorIcon: IconResource?)
        case base64(data: String, mimeType: String)
    }

    /// Icon variant types
    public enum IconVariant {
        case filled
        case outlined
        case rounded
        case sharp
        case twoTone
    }

    /// Cache statistics
    public struct CacheStatistics {
        var memoryHits: Int = 0
        var memoryMisses: Int = 0
        var totalRequests: Int = 0
        var memorySizeBytes: Int = 0

        var hitRate: Double {
            guard totalRequests > 0 else { return 0.0 }
            return Double(memoryHits) / Double(totalRequests)
        }
    }

    // MARK: - Initialization

    private init() {
        self.iconCache = NSCache<NSString, AnyObject>()
        self.iconCache.countLimit = 200 // Limit to 200 icons in memory
        self.iconCache.totalCostLimit = 50 * 1024 * 1024 // 50MB
        self.cacheStats = CacheStatistics()
    }

    // MARK: - Icon Loading

    /// Load an icon resource
    ///
    /// - Parameters:
    ///   - resource: Icon resource to load
    ///   - size: Desired icon size (in points)
    ///   - tint: Optional tint color
    /// - Returns: Loaded icon representation
    public func loadIcon(
        resource: IconResource,
        size: CGFloat = 24.0,
        tint: UIColor? = nil
    ) async -> LoadedIcon? {

        // SEQ-3 FIX: Track active operation
        activeOperations += 1
        defer { activeOperations -= 1 }

        // Update statistics
        cacheStats.totalRequests += 1

        // Check cache first
        let cacheKey = makeCacheKey(resource: resource, size: size, tint: tint)
        if let cached = iconCache.object(forKey: cacheKey as NSString) {
            cacheStats.memoryHits += 1
            if let uiImage = cached as? UIImage {
                return .image(uiImage)
            } else if let symbolName = cached as? String {
                return .sfSymbol(name: symbolName)
            }
        }

        cacheStats.memoryMisses += 1

        // Load icon based on type
        switch resource {
        case .materialIcon(let name, let variant):
            return await loadMaterialIcon(name: name, variant: variant, size: size, tint: tint, cacheKey: cacheKey)

        case .flutterIcon(let name):
            return await loadFlutterIcon(name: name, size: size, tint: tint, cacheKey: cacheKey)

        case .assetPath(let path):
            return await loadAssetIcon(path: path, size: size, tint: tint, cacheKey: cacheKey)

        case .networkURL(let url, _, _):
            return await loadNetworkIcon(url: url, size: size, tint: tint, cacheKey: cacheKey)

        case .base64(let data, let mimeType):
            return await loadBase64Icon(data: data, mimeType: mimeType, size: size, tint: tint, cacheKey: cacheKey)
        }
    }

    /// Preload icons into cache
    ///
    /// - Parameter resources: List of icon resources to preload
    public func preloadIcons(resources: [IconResource]) async {
        await withTaskGroup(of: Void.self) { group in
            for resource in resources {
                group.addTask {
                    _ = await self.loadIcon(resource: resource)
                }
            }
        }
    }

    /// Clear icon cache
    ///
    /// - Parameters:
    ///   - memoryOnly: If true, only clear memory cache
    ///   - waitForOperations: If true, wait for active operations to complete (default: true)
    /// - Returns: true if cache was cleared, false if skipped due to active operations
    @discardableResult
    public func clearCache(memoryOnly: Bool = false, waitForOperations: Bool = true) async -> Bool {
        // SEQ-3 FIX: Wait for active operations to complete before clearing
        if waitForOperations && activeOperations > 0 {
            // Wait up to 5 seconds for operations to complete
            var waitAttempts = 0
            while activeOperations > 0 && waitAttempts < 50 {
                try? await Task.sleep(nanoseconds: 100_000_000) // 100ms
                waitAttempts += 1
            }

            // If still active operations, skip clear and log warning
            if activeOperations > 0 {
                return false
            }
        }

        iconCache.removeAllObjects()
        cacheStats = CacheStatistics()

        if !memoryOnly {
            // Clear URL cache for network images
            URLCache.shared.removeAllCachedResponses()
        }

        return true
    }

    /// Check if cache operations are in progress
    public func hasActiveOperations() -> Bool {
        return activeOperations > 0
    }

    /// Get active operation count
    public func getActiveOperationCount() -> Int {
        return activeOperations
    }

    /// Get cache statistics
    public func getCacheStats() -> CacheStatistics {
        return cacheStats
    }

    /// Check if icon is cached
    public func isCached(resource: IconResource) -> Bool {
        let cacheKey = makeCacheKey(resource: resource, size: 24.0, tint: nil)
        return iconCache.object(forKey: cacheKey as NSString) != nil
    }

    // MARK: - Private Loading Methods

    private func loadMaterialIcon(
        name: String,
        variant: IconVariant,
        size: CGFloat,
        tint: UIColor?,
        cacheKey: String
    ) async -> LoadedIcon? {
        // Convert Material icon to SF Symbol
        let sfSymbolName = MaterialToSFSymbolMapper.map(materialIconName: name, variant: variant)

        // Cache the symbol name
        iconCache.setObject(sfSymbolName as NSString, forKey: cacheKey as NSString)

        return .sfSymbol(name: sfSymbolName)
    }

    private func loadFlutterIcon(
        name: String,
        size: CGFloat,
        tint: UIColor?,
        cacheKey: String
    ) async -> LoadedIcon? {
        // Convert Flutter icon to SF Symbol
        let sfSymbolName = FlutterToSFSymbolMapper.map(flutterIconName: name)

        // Cache the symbol name
        iconCache.setObject(sfSymbolName as NSString, forKey: cacheKey as NSString)

        return .sfSymbol(name: sfSymbolName)
    }

    private func loadAssetIcon(
        path: String,
        size: CGFloat,
        tint: UIColor?,
        cacheKey: String
    ) async -> LoadedIcon? {
        guard let image = UIImage(named: path) else {
            return nil
        }

        // Resize and tint if needed
        let processedImage = processImage(image, size: size, tint: tint)

        // Cache the processed image
        iconCache.setObject(processedImage, forKey: cacheKey as NSString, cost: estimateImageSize(processedImage))

        return .image(processedImage)
    }

    private func loadNetworkIcon(
        url: String,
        size: CGFloat,
        tint: UIColor?,
        cacheKey: String
    ) async -> LoadedIcon? {
        guard let imageURL = URL(string: url) else {
            return nil
        }

        // For network images, return the URL (AsyncImage will handle loading)
        // Cache the URL for future reference
        iconCache.setObject(imageURL as NSURL, forKey: cacheKey as NSString)

        return .url(imageURL)
    }

    private func loadBase64Icon(
        data: String,
        mimeType: String,
        size: CGFloat,
        tint: UIColor?,
        cacheKey: String
    ) async -> LoadedIcon? {
        guard let imageData = Data(base64Encoded: data),
              let image = UIImage(data: imageData) else {
            return nil
        }

        // Resize and tint if needed
        let processedImage = processImage(image, size: size, tint: tint)

        // Cache the processed image
        iconCache.setObject(processedImage, forKey: cacheKey as NSString, cost: estimateImageSize(processedImage))

        return .image(processedImage)
    }

    // MARK: - Image Processing

    private func processImage(_ image: UIImage, size: CGFloat, tint: UIColor?) -> UIImage {
        // Resize image
        let scaledImage = resizeImage(image, targetSize: CGSize(width: size, height: size))

        // Apply tint if provided
        if let tint = tint {
            return tintImage(scaledImage, color: tint)
        }

        return scaledImage
    }

    /// SECURITY FIX: Added @MainActor for UIGraphics thread safety
    @MainActor
    private func resizeImage(_ image: UIImage, targetSize: CGSize) -> UIImage {
        let size = image.size
        let widthRatio  = targetSize.width  / size.width
        let heightRatio = targetSize.height / size.height
        let ratio = min(widthRatio, heightRatio)

        let newSize = CGSize(width: size.width * ratio, height: size.height * ratio)
        let rect = CGRect(x: 0, y: 0, width: newSize.width, height: newSize.height)

        UIGraphicsBeginImageContextWithOptions(newSize, false, 0.0)
        defer { UIGraphicsEndImageContext() }

        image.draw(in: rect)
        return UIGraphicsGetImageFromCurrentImageContext() ?? image
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

    // MARK: - Cache Utilities

    private func makeCacheKey(resource: IconResource, size: CGFloat, tint: UIColor?) -> String {
        var key = ""

        switch resource {
        case .materialIcon(let name, let variant):
            key = "material:\(name):\(variant)"
        case .flutterIcon(let name):
            key = "flutter:\(name)"
        case .assetPath(let path):
            key = "asset:\(path)"
        case .networkURL(let url, _, _):
            key = "network:\(url)"
        case .base64(let data, _):
            // Use hash of data to avoid huge cache keys
            key = "base64:\(data.prefix(50).hashValue)"
        }

        key += ":\(size)"
        if let tint = tint {
            key += ":\(tint.hexString)"
        }

        return key
    }

    private func estimateImageSize(_ image: UIImage) -> Int {
        guard let cgImage = image.cgImage else { return 0 }
        let bytesPerPixel = 4 // RGBA
        return cgImage.width * cgImage.height * bytesPerPixel
    }
}

// MARK: - Material to SF Symbol Mapper

/// Maps Material Design icon names to SF Symbol names
private struct MaterialToSFSymbolMapper {
    static func map(materialIconName: String, variant: IconVariant) -> String {
        // Check if we have a direct mapping
        if let mapped = materialToSFMapping[materialIconName] {
            return mapped
        }

        // Fallback: try to find a reasonable SF Symbol
        // Many Material icons have similar SF Symbol equivalents
        return findBestMatch(materialIconName: materialIconName) ?? "questionmark.circle"
    }

    private static func findBestMatch(materialIconName: String) -> String? {
        // Simple heuristic matching
        let normalized = materialIconName.lowercased().replacingOccurrences(of: "_", with: "")

        // Common patterns
        if normalized.contains("check") { return "checkmark" }
        if normalized.contains("close") || normalized.contains("clear") { return "xmark" }
        if normalized.contains("add") { return "plus" }
        if normalized.contains("remove") { return "minus" }
        if normalized.contains("delete") { return "trash" }
        if normalized.contains("edit") { return "pencil" }
        if normalized.contains("search") { return "magnifyingglass" }
        if normalized.contains("settings") { return "gearshape" }
        if normalized.contains("person") || normalized.contains("account") { return "person" }
        if normalized.contains("home") { return "house" }
        if normalized.contains("star") { return "star" }
        if normalized.contains("favorite") || normalized.contains("heart") { return "heart" }
        if normalized.contains("share") { return "square.and.arrow.up" }
        if normalized.contains("save") { return "square.and.arrow.down" }
        if normalized.contains("info") { return "info.circle" }
        if normalized.contains("warning") { return "exclamationmark.triangle" }
        if normalized.contains("error") { return "xmark.octagon" }
        if normalized.contains("help") { return "questionmark.circle" }

        return nil
    }

    /// Material Design to SF Symbol mapping (326 most common icons)
    private static let materialToSFMapping: [String: String] = [
        // Actions
        "check": "checkmark",
        "close": "xmark",
        "add": "plus",
        "remove": "minus",
        "delete": "trash",
        "edit": "pencil",
        "save": "square.and.arrow.down",
        "undo": "arrow.uturn.backward",
        "redo": "arrow.uturn.forward",
        "refresh": "arrow.clockwise",
        "sync": "arrow.triangle.2.circlepath",
        "upload": "arrow.up.circle",
        "download": "arrow.down.circle",
        "share": "square.and.arrow.up",
        "copy": "doc.on.doc",
        "paste": "doc.on.clipboard",
        "cut": "scissors",
        "done": "checkmark.circle",
        "cancel": "xmark.circle",
        "clear": "xmark",

        // Navigation
        "arrow_back": "chevron.left",
        "arrow_forward": "chevron.right",
        "arrow_upward": "chevron.up",
        "arrow_downward": "chevron.down",
        "navigate_before": "chevron.left",
        "navigate_next": "chevron.right",
        "chevron_left": "chevron.left",
        "chevron_right": "chevron.right",
        "expand_more": "chevron.down",
        "expand_less": "chevron.up",
        "unfold_more": "arrow.up.and.down",
        "unfold_less": "arrow.up.and.down",
        "first_page": "chevron.left.2",
        "last_page": "chevron.right.2",
        "menu": "line.3.horizontal",
        "more_vert": "ellipsis.circle",
        "more_horiz": "ellipsis",
        "apps": "square.grid.2x2",
        "home": "house",

        // Content
        "content_copy": "doc.on.doc",
        "content_cut": "scissors",
        "content_paste": "doc.on.clipboard",
        "create": "pencil",
        "drafts": "envelope.open",
        "filter_list": "line.3.horizontal.decrease.circle",
        "flag": "flag",
        "inbox": "tray",
        "link": "link",
        "mail": "envelope",
        "report": "exclamationmark.bubble",
        "send": "paperplane",
        "sort": "arrow.up.arrow.down",
        "text_format": "textformat",
        "attach_file": "paperclip",
        "attachment": "paperclip",
        "cloud": "cloud",
        "cloud_upload": "icloud.and.arrow.up",
        "cloud_download": "icloud.and.arrow.down",
        "folder": "folder",
        "folder_open": "folder.fill",
        "insert_drive_file": "doc",

        // Communication
        "call": "phone",
        "chat": "message",
        "chat_bubble": "bubble.left",
        "comment": "text.bubble",
        "contacts": "person.2",
        "email": "envelope",
        "forum": "bubble.left.and.bubble.right",
        "message": "message",
        "phone": "phone",
        "sms": "message",
        "videocam": "video",
        "voice_chat": "mic",

        // People
        "person": "person",
        "person_add": "person.badge.plus",
        "person_remove": "person.badge.minus",
        "people": "person.2",
        "account_circle": "person.circle",
        "face": "face.smiling",
        "group": "person.3",
        "supervisor_account": "person.2.circle",

        // Settings
        "settings": "gearshape",
        "settings_applications": "gearshape.2",
        "settings_bluetooth": "bluetooth",
        "settings_brightness": "sun.max",
        "settings_cell": "antenna.radiowaves.left.and.right",
        "settings_phone": "phone.circle",
        "settings_power": "power",
        "settings_voice": "waveform",
        "tune": "slider.horizontal.3",
        "build": "hammer",
        "developer_mode": "chevron.left.forwardslash.chevron.right",

        // Media
        "play_arrow": "play",
        "pause": "pause",
        "stop": "stop",
        "skip_next": "forward",
        "skip_previous": "backward",
        "fast_forward": "forward.fill",
        "fast_rewind": "backward.fill",
        "replay": "arrow.counterclockwise",
        "volume_up": "speaker.wave.3",
        "volume_down": "speaker.wave.1",
        "volume_mute": "speaker.slash",
        "volume_off": "speaker.slash.fill",
        "mic": "mic",
        "mic_off": "mic.slash",
        "camera": "camera",
        "camera_alt": "camera.fill",
        "image": "photo",
        "photo": "photo",
        "photo_camera": "camera",
        "video_library": "film",
        "music_note": "music.note",
        "album": "music.note.list",
        "equalizer": "waveform",

        // Toggle
        "check_box": "checkmark.square",
        "check_box_outline_blank": "square",
        "radio_button_checked": "circle.inset.filled",
        "radio_button_unchecked": "circle",
        "toggle_on": "circle.fill",
        "toggle_off": "circle",
        "star": "star",
        "star_border": "star",
        "star_half": "star.leadinghalf.filled",
        "favorite": "heart",
        "favorite_border": "heart",
        "thumb_up": "hand.thumbsup",
        "thumb_down": "hand.thumbsdown",

        // Status
        "info": "info.circle",
        "info_outline": "info.circle",
        "warning": "exclamationmark.triangle",
        "warning_amber": "exclamationmark.triangle.fill",
        "error": "xmark.octagon",
        "error_outline": "xmark.octagon",
        "help": "questionmark.circle",
        "help_outline": "questionmark.circle",
        "notification_important": "bell.badge",
        "notifications": "bell",
        "notifications_active": "bell.fill",
        "notifications_off": "bell.slash",
        "notifications_none": "bell",
        "priority_high": "exclamationmark",

        // Security
        "verified": "checkmark.seal",
        "verified_user": "checkmark.shield",
        "lock": "lock",
        "lock_open": "lock.open",
        "security": "shield",
        "visibility": "eye",
        "visibility_off": "eye.slash",
        "vpn_key": "key",

        // Device
        "smartphone": "iphone",
        "tablet": "ipad",
        "laptop": "laptopcomputer",
        "computer": "desktopcomputer",
        "desktop_windows": "desktopcomputer",
        "devices": "devices",
        "battery_full": "battery.100",
        "battery_charging_full": "battery.100.bolt",
        "bluetooth": "bluetooth",
        "wifi": "wifi",
        "signal_cellular_4_bar": "antenna.radiowaves.left.and.right",
        "gps_fixed": "location.fill",
        "location_on": "location",
        "location_off": "location.slash",

        // Time & Date
        "access_time": "clock",
        "alarm": "alarm",
        "alarm_add": "alarm.fill",
        "alarm_on": "alarm",
        "alarm_off": "alarm.slash",
        "schedule": "calendar.badge.clock",
        "timer": "timer",
        "hourglass_empty": "hourglass",
        "date_range": "calendar",
        "today": "calendar.badge.clock",
        "event": "calendar.badge.exclamationmark",
        "calendar_today": "calendar",

        // Shopping
        "shopping_cart": "cart",
        "shopping_bag": "bag",
        "local_grocery_store": "bag.fill",
        "add_shopping_cart": "cart.badge.plus",
        "remove_shopping_cart": "cart.badge.minus",
        "payment": "creditcard",
        "credit_card": "creditcard",
        "receipt": "doc.text",
        "local_offer": "tag",
        "store": "storefront",

        // Places
        "place": "mappin",
        "map": "map",
        "directions": "arrow.triangle.turn.up.right.circle",
        "directions_car": "car",
        "directions_bus": "bus",
        "directions_walk": "figure.walk",
        "flight": "airplane",
        "hotel": "bed.double",
        "restaurant": "fork.knife",
        "local_cafe": "cup.and.saucer",

        // Image & Photo
        "photo_library": "photo.on.rectangle",
        "collections": "rectangle.stack",
        "crop": "crop",
        "rotate_left": "rotate.left",
        "rotate_right": "rotate.right",
        "palette": "paintpalette",
        "brush": "paintbrush",
        "color_lens": "eyedropper",

        // Search
        "search": "magnifyingglass",
        "find_in_page": "doc.text.magnifyingglass",
        "find_replace": "arrow.left.arrow.right",
        "zoom_in": "plus.magnifyingglass",
        "zoom_out": "minus.magnifyingglass",
        "filter": "line.3.horizontal.decrease.circle",
        "filter_alt": "line.3.horizontal.decrease",

        // Social
        "public": "network",
        "language": "globe",
        "school": "book",
        "work": "briefcase",

        // Files
        "picture_as_pdf": "doc.richtext",
        "description": "doc.text",
        "article": "doc.plaintext",

        // Arrows
        "arrow_circle_down": "arrow.down.circle",
        "arrow_circle_up": "arrow.up.circle",
        "arrow_drop_down": "chevron.down",
        "arrow_drop_up": "chevron.up",
        "subdirectory_arrow_left": "arrow.turn.down.left",
        "subdirectory_arrow_right": "arrow.turn.down.right",
        "trending_up": "chart.line.uptrend.xyaxis",
        "trending_down": "chart.line.downtrend.xyaxis",
        "trending_flat": "minus",

        // Layout
        "dashboard": "square.grid.2x2",
        "view_list": "list.bullet",
        "view_module": "square.grid.3x3",
        "view_quilt": "square.grid.3x2",
        "view_carousel": "rectangle.stack.person.crop",
        "view_column": "rectangle.split.3x1",
        "view_stream": "rectangle.split.2x1",
        "grid_on": "grid",
        "grid_off": "grid",
        "table_chart": "tablecells",

        // Editor
        "format_bold": "bold",
        "format_italic": "italic",
        "format_underlined": "underline",
        "format_size": "textformat.size",
        "format_align_left": "text.alignleft",
        "format_align_center": "text.aligncenter",
        "format_align_right": "text.alignright",
        "format_align_justify": "text.justify",
        "format_list_bulleted": "list.bullet",
        "format_list_numbered": "list.number",
        "format_quote": "quote.bubble",

        // Misc
        "account_balance": "building.columns",
        "account_balance_wallet": "wallet.pass",
        "android": "apps.iphone",
        "bug_report": "ant",
        "code": "chevron.left.forwardslash.chevron.right",
        "desktop_mac": "desktopcomputer",
        "explore": "safari",
        "extension": "puzzlepiece.extension",
        "fingerprint": "touchid",
        "grade": "star.fill",

        // Brightness
        "brightness_1": "circle.fill",
        "brightness_2": "circle.lefthalf.filled",
        "brightness_3": "circle.righthalf.filled",
        "brightness_4": "sun.min",
        "brightness_5": "sun.max",
        "brightness_6": "sun.max.fill",
        "brightness_7": "sun.max.circle",
        "brightness_auto": "sun.max",
        "brightness_high": "sun.max.fill",
        "brightness_low": "sun.min",
        "brightness_medium": "sun.max"
    ]
}

// MARK: - Flutter to SF Symbol Mapper

/// Maps Flutter icon names to SF Symbol names
private struct FlutterToSFSymbolMapper {
    static func map(flutterIconName: String) -> String {
        // Remove "Icons." prefix if present
        let normalized = flutterIconName.hasPrefix("Icons.") ?
            String(flutterIconName.dropFirst(6)) : flutterIconName

        // Use Material mapper (Flutter icons use Material Design icons)
        return MaterialToSFSymbolMapper.map(materialIconName: normalized, variant: .filled)
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
