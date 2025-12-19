package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * iOS SwiftUI Mappers for Flutter Advanced Image Display Components
 *
 * This file maps cross-platform Flutter image components to iOS SwiftUI
 * bridge representations. The SwiftUI bridge models are consumed by Swift
 * code to render native iOS UI.
 *
 * Architecture:
 * Flutter Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented:
 * - LazyImage: Lazy loading image with placeholder and error handling
 * - ImageGallery: Grid-based image gallery with lazy loading
 * - Lightbox: Full-screen image viewer with zoom and swipe navigation
 * - Zoom: Zoomable image container with pinch and pan gestures
 * - QRCode: QR code generator with customizable styling
 *
 * iOS-specific features:
 * - Native AsyncImage for lazy loading with automatic caching
 * - UIImage-based QR code generation with Core Image
 * - MagnificationGesture for pinch-to-zoom functionality
 * - LazyVGrid for efficient grid layouts
 * - Full-screen modal presentation for lightbox
 * - Smooth animations with SwiftUI's animation system
 *
 * @since 3.0.0-flutter-parity-ios
 */

/**
 * Data class representing a LazyImage component
 *
 * Provides progressive image loading with placeholder and error states,
 * similar to Flutter's FadeInImage and CachedNetworkImage.
 */
data class LazyImage(
    val id: String? = null,
    val url: String,
    val placeholder: String? = null,
    val errorWidget: String? = null,
    val fadeIn: Boolean = true,
    val cacheKey: String? = null,
    val enabled: Boolean = true
) {
    fun getAccessibilityDescription(): String = "Loading image from $url"
}

/**
 * Data class representing an ImageGallery component
 *
 * Displays a grid of images with lazy loading and tap-to-view functionality.
 */
data class ImageGallery(
    val id: String? = null,
    val images: List<String>,
    val columns: Int = 3,
    val spacing: Float = 8f,
    val onImageTap: String? = null,
    val enabled: Boolean = true
) {
    fun getAccessibilityDescription(): String = "Image gallery with ${images.size} images"
}

/**
 * Data class representing a Lightbox component
 *
 * Full-screen image viewer with zoom, pan, and swipe navigation.
 */
data class Lightbox(
    val id: String? = null,
    val isVisible: Boolean,
    val imageUrl: String? = null,
    val images: List<String>? = null,
    val currentIndex: Int = 0,
    val onClose: String? = null,
    val showControls: Boolean = true,
    val enabled: Boolean = true
) {
    fun getAccessibilityDescription(): String {
        return if (images != null) {
            "Image viewer showing image ${currentIndex + 1} of ${images.size}"
        } else {
            "Image viewer"
        }
    }
}

/**
 * Data class representing a Zoom component
 *
 * Wraps a child component with pinch-to-zoom and pan gestures.
 */
data class Zoom(
    val id: String? = null,
    val child: Component,
    val minScale: Float = 1.0f,
    val maxScale: Float = 4.0f,
    val onScaleChange: String? = null,
    val enabled: Boolean = true
) {
    fun getAccessibilityDescription(): String = "Zoomable container (${minScale}x to ${maxScale}x)"
}

/**
 * Data class representing a QRCode component
 *
 * Generates a QR code from string data with customizable appearance.
 */
data class QRCode(
    val id: String? = null,
    val data: String,
    val size: Float = 200f,
    val foregroundColor: String = "#000000",
    val backgroundColor: String = "#FFFFFF",
    val errorCorrectionLevel: String = "M", // L, M, Q, H
    val enabled: Boolean = true
) {
    fun getAccessibilityDescription(): String = "QR code containing: $data"
}

/**
 * Maps LazyImage to SwiftUI AsyncImage with loading states
 *
 * Creates a progressive image loading experience with:
 * - Placeholder shown during loading
 * - Fade-in animation when image loads (if enabled)
 * - Error widget shown on load failure
 * - Automatic image caching via URLCache
 * - Custom cache key support
 *
 * iOS Implementation:
 * - Uses AsyncImage for native async loading
 * - ProgressView as default placeholder
 * - SF Symbol (exclamationmark.triangle) as default error icon
 * - Automatic memory and disk caching
 *
 * Visual parity with Flutter's CachedNetworkImage maintained
 */
object LazyImageMapper {
    fun map(
        component: LazyImage,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add fade-in animation if enabled
        if (component.fadeIn) {
            modifiers.add(
                SwiftUIModifier(
                    type = ModifierType.Animation,
                    value = mapOf(
                        "type" to "easeIn",
                        "duration" to 0.3
                    )
                )
            )
        }

        // Add corner radius for smooth appearance
        modifiers.add(SwiftUIModifier.cornerRadius(8f))

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        return SwiftUIView(
            type = ViewType.AsyncImage,
            id = component.id,
            properties = mapOf(
                "url" to component.url,
                "placeholder" to (component.placeholder ?: "photo"),
                "errorWidget" to (component.errorWidget ?: "exclamationmark.triangle"),
                "fadeIn" to component.fadeIn,
                "cacheKey" to (component.cacheKey ?: component.url),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            modifiers = modifiers
        )
    }
}

/**
 * Maps ImageGallery to SwiftUI LazyVGrid with AsyncImage cells
 *
 * Creates a grid-based image gallery with:
 * - Configurable column count
 * - Lazy loading of images (only visible cells loaded)
 * - Uniform spacing between images
 * - Tap gesture for full-screen viewing
 * - Automatic aspect ratio preservation
 *
 * iOS Implementation:
 * - LazyVGrid with GridItem columns
 * - AsyncImage for each cell
 * - OnTapGesture for navigation
 * - Aspect ratio 1:1 for uniform grid
 * - ScrollView container for vertical scrolling
 *
 * Visual parity with Flutter's GridView.count maintained
 */
object ImageGalleryMapper {
    fun map(
        component: ImageGallery,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = component.images.map { imageUrl ->
            SwiftUIView(
                type = ViewType.AsyncImage,
                properties = mapOf(
                    "url" to imageUrl,
                    "placeholder" to "photo",
                    "errorWidget" to "exclamationmark.triangle",
                    "contentMode" to "fill"
                ),
                modifiers = listOf(
                    SwiftUIModifier.cornerRadius(8f),
                    SwiftUIModifier(
                        type = ModifierType.Frame,
                        value = FrameValue(
                            width = null,
                            height = null,
                            alignment = ZStackAlignment.Center
                        )
                    )
                )
            )
        }

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add padding around the grid
        modifiers.add(SwiftUIModifier.padding(component.spacing))

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        return SwiftUIView(
            type = ViewType.Custom("ImageGallery"),
            id = component.id,
            properties = mapOf(
                "images" to component.images,
                "columns" to component.columns,
                "spacing" to component.spacing,
                "onImageTap" to (component.onImageTap ?: ""),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = modifiers
        )
    }
}

/**
 * Maps Lightbox to SwiftUI full-screen modal with zoom and navigation
 *
 * Creates a full-screen image viewer with:
 * - Full-screen modal presentation
 * - Pinch-to-zoom gesture support
 * - Swipe navigation for gallery mode
 * - Close button in top corner
 * - Background dimming (translucent black)
 * - Page indicator for multi-image galleries
 * - Double-tap to zoom in/out
 *
 * iOS Implementation:
 * - fullScreenCover modifier for modal presentation
 * - MagnificationGesture for zoom
 * - DragGesture for pan when zoomed
 * - TabView for swipe navigation
 * - ZStack with dimmed background
 * - X button with SF Symbol
 *
 * Visual parity with Flutter's photo_view package maintained
 */
object LightboxMapper {
    fun map(
        component: Lightbox,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val images = component.images ?: component.imageUrl?.let { listOf(it) } ?: emptyList()

        val children = mutableListOf<SwiftUIView>()

        // Add close button if controls are shown
        if (component.showControls) {
            children.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "action" to (component.onClose ?: "close")
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "xmark.circle.fill"),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(32f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                            )
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier(
                            type = ModifierType.Frame,
                            value = FrameValue(
                                width = null,
                                height = null,
                                alignment = ZStackAlignment.TopTrailing
                            )
                        ),
                        SwiftUIModifier.padding(20f)
                    )
                )
            )
        }

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add background dimming
        modifiers.add(
            SwiftUIModifier.background(
                SwiftUIColor.rgb(0f, 0f, 0f, 0.9f)
            )
        )

        // Fill entire screen
        modifiers.add(
            SwiftUIModifier(
                type = ModifierType.Frame,
                value = FrameValue(
                    width = SizeValue.Infinity,
                    height = SizeValue.Infinity,
                    alignment = ZStackAlignment.Center
                )
            )
        )

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
        }

        return SwiftUIView(
            type = ViewType.Custom("Lightbox"),
            id = component.id,
            properties = mapOf(
                "isVisible" to component.isVisible,
                "images" to images,
                "currentIndex" to component.currentIndex,
                "onClose" to (component.onClose ?: ""),
                "showControls" to component.showControls,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = children,
            modifiers = modifiers
        )
    }
}

/**
 * Maps Zoom to SwiftUI view with magnification and pan gestures
 *
 * Creates a zoomable container with:
 * - Pinch-to-zoom gesture (two-finger pinch)
 * - Double-tap to toggle zoom
 * - Pan gesture when zoomed in
 * - Configurable min/max scale limits
 * - Scale change callbacks
 * - Smooth spring animations
 * - Reset to original scale on double-tap when zoomed
 *
 * iOS Implementation:
 * - scaleEffect modifier for zoom
 * - MagnificationGesture for pinch
 * - DragGesture for pan
 * - TapGesture (count: 2) for double-tap
 * - simultaneousGesture for gesture combining
 * - Spring animation for smooth transitions
 *
 * Visual parity with Flutter's InteractiveViewer maintained
 */
object ZoomMapper {
    fun map(
        component: Zoom,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val child = renderChild(component.child)

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add frame to contain the zoomable content
        modifiers.add(
            SwiftUIModifier(
                type = ModifierType.Frame,
                value = FrameValue(
                    width = SizeValue.Infinity,
                    height = SizeValue.Infinity,
                    alignment = ZStackAlignment.Center
                )
            )
        )

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        return SwiftUIView(
            type = ViewType.Custom("Zoom"),
            id = component.id,
            properties = mapOf(
                "minScale" to component.minScale,
                "maxScale" to component.maxScale,
                "onScaleChange" to (component.onScaleChange ?: ""),
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(child),
            modifiers = modifiers
        )
    }
}

/**
 * Maps QRCode to SwiftUI custom view with Core Image generation
 *
 * Creates a QR code generator with:
 * - String data encoding
 * - Customizable size (points)
 * - Custom foreground and background colors
 * - Error correction levels (L=7%, M=15%, Q=25%, H=30%)
 * - High-quality rendering at any scale
 * - Automatic anti-aliasing
 *
 * iOS Implementation:
 * - CIFilter with CIQRCodeGenerator
 * - CIContext for rendering
 * - CGImage conversion
 * - Color transformation with CIFalseColor
 * - Bicubic interpolation for scaling
 * - UIImage/NSImage wrapper
 *
 * Error Correction Levels:
 * - L (Low): 7% of codewords can be restored
 * - M (Medium): 15% of codewords can be restored
 * - Q (Quartile): 25% of codewords can be restored
 * - H (High): 30% of codewords can be restored
 *
 * Visual parity with Flutter's qr_flutter package maintained
 */
object QRCodeMapper {
    fun map(
        component: QRCode,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Set fixed size for QR code
        modifiers.add(
            SwiftUIModifier(
                type = ModifierType.Frame,
                value = FrameValue(
                    width = SizeValue.Fixed(component.size),
                    height = SizeValue.Fixed(component.size),
                    alignment = ZStackAlignment.Center
                )
            )
        )

        // Add corner radius for smooth appearance
        modifiers.add(SwiftUIModifier.cornerRadius(8f))

        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        return SwiftUIView(
            type = ViewType.Custom("QRCode"),
            id = component.id,
            properties = mapOf(
                "data" to component.data,
                "size" to component.size,
                "foregroundColor" to component.foregroundColor,
                "backgroundColor" to component.backgroundColor,
                "errorCorrectionLevel" to component.errorCorrectionLevel,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            modifiers = modifiers
        )
    }
}

// Component interface is provided by com.augmentalis.avaelements.core.Component
