package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.display.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Material Display Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity display components to SwiftUI equivalents.
 *
 * Components:
 * - AvatarGroup → HStack with overlapping circles
 * - SkeletonText → Rectangle with shimmer animation
 * - SkeletonCircle → Circle with shimmer animation
 * - ProgressCircle → CircularProgressView
 * - LoadingOverlay → ZStack with semi-transparent background
 * - Popover → Popover modifier
 * - ErrorState → VStack with error icon and message
 * - NoData → VStack with empty state icon and message
 * - ImageCarousel → TabView with PageTabViewStyle
 * - LazyImage → AsyncImage with placeholder
 * - ImageGallery → LazyVGrid with images
 * - Lightbox → Full-screen overlay with image viewer
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// AVATAR GROUP
// ============================================

object AvatarGroupMapper {
    fun map(component: AvatarGroup, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        val avatars = component.avatars.take(component.max)
        val remaining = (component.avatars.size - component.max).coerceAtLeast(0)

        val children = mutableListOf<SwiftUIView>()

        avatars.forEach { avatar ->
            children.add(SwiftUIView(
                type = ViewType.Custom("AsyncImage"),
                properties = mapOf("url" to (avatar.imageUrl ?: ""), "size" to component.size),
                modifiers = listOf(
                    SwiftUIModifier(type = ModifierType.Custom, value = mapOf("clipShape" to "Circle")),
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("overlay" to mapOf("stroke" to "white", "lineWidth" to 2f))
                    )
                )
            ))
        }

        // Show "+N" if there are more avatars
        if (remaining > 0) {
            children.add(SwiftUIView(
                type = ViewType.ZStack,
                properties = emptyMap(),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Custom("Circle"),
                        properties = mapOf("size" to component.size),
                        modifiers = listOf(
                            SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondarySystemFill"))
                        )
                    ),
                    SwiftUIView.text(
                        content = "+$remaining",
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Caption),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                        )
                    )
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to -8f), // Negative spacing for overlap
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// SKELETON TEXT
// ============================================

object SkeletonTextMapper {
    fun map(component: SkeletonText, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Width - use full width if null
        val width = component.width ?: 200f
        // Height - use variant height
        val height = component.getVariantHeight()

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(width),
            height = SizeValue.Fixed(height)
        ))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("tertiarySystemFill")))
        if (component.borderRadius > 0f) {
            modifiers.add(SwiftUIModifier(
                ModifierType.Custom,
                mapOf("cornerRadius" to component.borderRadius)
            ))
        }

        // Shimmer animation if not None
        if (component.animation != SkeletonText.Animation.None) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf(
                    "shimmer" to true,
                    "animationType" to component.animation.name.lowercase()
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.Custom("Rectangle"),
            properties = emptyMap(),
            modifiers = modifiers,
        )
    }
}

// ============================================
// SKELETON CIRCLE
// ============================================

object SkeletonCircleMapper {
    fun map(component: SkeletonCircle, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(component.diameter),
            height = SizeValue.Fixed(component.diameter)
        ))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("tertiarySystemFill")))

        // Shimmer animation if not None
        if (component.animation != SkeletonCircle.Animation.None) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf(
                    "shimmer" to true,
                    "animationType" to component.animation.name.lowercase()
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.Custom("Circle"),
            properties = emptyMap(),
            modifiers = modifiers,
        )
    }
}

// ============================================
// PROGRESS CIRCLE
// ============================================

object ProgressCircleMapper {
    fun map(component: ProgressCircle, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(component.size),
            height = SizeValue.Fixed(component.size)
        ))

        component.color?.let { color ->
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("tint" to parseColor(color))
            ))
        }

        val children = mutableListOf<SwiftUIView>()

        // Progress view
        children.add(SwiftUIView(
            type = ViewType.Custom("ProgressView"),
            properties = mapOf(
                "value" to (component.value ?: -1f), // -1 for indeterminate
                "style" to "circular"
            ),
            modifiers = emptyList()
        ))

        // Label if shown
        if (component.showLabel) {
            val labelText = component.getEffectiveLabelText()
            labelText?.let { text ->
                children.add(SwiftUIView.text(
                    content = text,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                    )
                ))
            }
        }

        return if (children.size > 1) {
            SwiftUIView(
                type = ViewType.ZStack,
                properties = mapOf("alignment" to "center"),
                children = children,
                modifiers = modifiers,
            )
        } else {
            SwiftUIView(
                type = ViewType.Custom("ProgressView"),
                properties = mapOf(
                    "value" to (component.value ?: -1f),
                    "style" to "circular"
                ),
                modifiers = modifiers,
            )
        }
    }
}

// ============================================
// LOADING OVERLAY
// ============================================

object LoadingOverlayMapper {
    fun map(component: LoadingOverlay, theme: Theme?): SwiftUIView {
        if (!component.visible) {
            return SwiftUIView(type = ViewType.EmptyView, properties = emptyMap(), id = component.id)
        }

        val children = mutableListOf<SwiftUIView>()

        // Semi-transparent background
        children.add(SwiftUIView(
            type = ViewType.Custom("Rectangle"),
            properties = emptyMap(),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(SwiftUIColor.rgb(0f, 0f, 0f, component.backdropOpacity)),
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("ignoresSafeArea" to component.fullScreen)
                )
            )
        ))

        // Loading indicator and message
        val indicatorChildren = mutableListOf<SwiftUIView>()

        // Spinner
        val spinnerScale = component.spinnerSize / 32f
        indicatorChildren.add(SwiftUIView(
            type = ViewType.Custom("ProgressView"),
            properties = mapOf("style" to "circular"),
            modifiers = listOf(
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("scaleEffect" to spinnerScale)
                )
            )
        ))

        // Message
        component.message?.let { message ->
            indicatorChildren.add(SwiftUIView.text(
                content = message,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                )
            ))
        }

        // Cancel button if cancelable
        if (component.cancelable) {
            indicatorChildren.add(SwiftUIView(
                type = ViewType.Custom("Button"),
                properties = mapOf("action" to "cancel"),
                children = listOf(
                    SwiftUIView.text(
                        content = component.cancelText,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Body),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                        )
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier.padding(8f, 16f, 8f, 16f)
                )
            ))
        }

        children.add(SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("spacing" to 16f),
            children = indicatorChildren
        ))

        return SwiftUIView(
            type = ViewType.ZStack,
            properties = mapOf("alignment" to "center"),
            children = children,
        )
    }
}

// ============================================
// POPOVER
// ============================================

object PopoverMapper {
    fun map(component: Popover, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf(
                "popover" to mapOf(
                    "isPresented" to component.visible,
                    "anchorId" to component.anchorId,
                    "position" to component.position.name.lowercase()
                )
            )
        ))

        val contentChildren = mutableListOf<SwiftUIView>()

        // Title if present
        component.title?.let { title ->
            contentChildren.add(SwiftUIView.text(
                content = title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                )
            ))
        }

        // Content
        contentChildren.add(SwiftUIView.text(
            content = component.content,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))

        // Actions
        if (component.hasActions()) {
            val actionViews = component.actions.map { action ->
                SwiftUIView(
                    type = ViewType.Custom("Button"),
                    properties = mapOf("text" to action.label),
                    modifiers = listOf(
                        SwiftUIModifier(
                            type = ModifierType.Custom,
                            value = mapOf("buttonStyle" to if (action.primary) "borderedProminent" else "bordered")
                        )
                    )
                )
            }
            contentChildren.add(SwiftUIView(
                type = ViewType.HStack,
                properties = mapOf("spacing" to 8f),
                children = actionViews
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = contentChildren,
            modifiers = listOf(
                SwiftUIModifier.padding(16f, 16f, 16f, 16f),
                SwiftUIModifier.frame(width = SizeValue.Fixed(component.maxWidth), height = null)
            ) + modifiers,
        )
    }
}

// ============================================
// ERROR STATE
// ============================================

object ErrorStateMapper {
    fun map(component: ErrorState, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.padding(32f, 32f, 32f, 32f))

        val children = mutableListOf<SwiftUIView>()

        // Error icon
        children.add(SwiftUIView(
            type = ViewType.Image,
            properties = mapOf(
                "systemName" to component.icon,
                "size" to 48f
            ),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemRed"))
            )
        ))

        // Title (message is the primary text)
        children.add(SwiftUIView.text(
            content = component.message,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Title2),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))

        // Message
        children.add(SwiftUIView.text(
            content = component.message,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary),
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("multilineTextAlignment" to "center")
                )
            )
        ))

        // Retry button if action available
        if (component.showRetry) {
            children.add(SwiftUIView(
                type = ViewType.Custom("Button"),
                properties = mapOf("text" to component.retryLabel, "action" to "retry"),
                modifiers = listOf(
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("buttonStyle" to "bordered")
                    )
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("spacing" to 16f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// NO DATA
// ============================================

object NoDataMapper {
    fun map(component: NoData, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.padding(32f, 32f, 32f, 32f))

        val children = mutableListOf<SwiftUIView>()

        // Icon
        val iconColor = component.color?.let { parseColor(it) }
            ?: SwiftUIColor.system("tertiaryLabel")

        children.add(SwiftUIView(
            type = ViewType.Image,
            properties = mapOf(
                "systemName" to component.icon,
                "size" to component.iconSize
            ),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(iconColor)
            )
        ))

        // Primary message
        children.add(SwiftUIView.text(
            content = component.message,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Title2),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))

        // Description if present
        component.description?.let { description ->
            children.add(SwiftUIView.text(
                content = description,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary),
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("multilineTextAlignment" to "center")
                    )
                )
            ))
        }

        // Action button if shown
        if (component.showAction) {
            children.add(SwiftUIView(
                type = ViewType.Custom("Button"),
                properties = mapOf("text" to component.actionLabel, "action" to "add"),
                modifiers = listOf(
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("buttonStyle" to "borderedProminent")
                    )
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("spacing" to 16f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// IMAGE CAROUSEL
// ============================================

object ImageCarouselMapper {
    fun map(component: ImageCarousel, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Use aspect ratio instead of fixed height
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("aspectRatio" to component.aspectRatio, "contentMode" to "fill")
        ))
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("tabViewStyle" to "page")
        ))

        val children = component.images.map { carouselImage ->
            SwiftUIView(
                type = ViewType.Custom("AsyncImage"),
                properties = mapOf(
                    "url" to carouselImage.url,
                    "description" to carouselImage.description
                ),
                modifiers = listOf(
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("contentMode" to "fill")
                    ),
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("clipped" to true)
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Custom("TabView"),
            properties = mapOf(
                "selection" to component.initialPage,
                "showIndicators" to component.showIndicators
            ),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// LAZY IMAGE
// ============================================

object LazyImageMapper {
    fun map(component: LazyImage, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Aspect ratio if set
        component.aspectRatio?.let { ratio ->
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("aspectRatio" to ratio)
            ))
        }

        // Content scale
        val contentMode = when (component.contentScale) {
            ImageContentScale.FIT -> "fit"
            ImageContentScale.FILL -> "fill"
            ImageContentScale.CROP -> "fill"
            ImageContentScale.INSIDE -> "fit"
            ImageContentScale.NONE -> "none"
        }
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("contentMode" to contentMode)
        ))

        // Shape
        when (component.shape) {
            ImageShape.CIRCULAR -> {
                modifiers.add(SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("clipShape" to "Circle")
                ))
            }
            ImageShape.ROUNDED -> {
                modifiers.add(SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("cornerRadius" to component.cornerRadius)
                ))
            }
            ImageShape.DEFAULT -> {}
        }

        return SwiftUIView(
            type = ViewType.Custom("AsyncImage"),
            properties = mapOf(
                "url" to component.url,
                "placeholder" to (component.placeholder ?: "photo"),
                "errorPlaceholder" to (component.errorPlaceholder ?: "broken_image"),
                "description" to component.contentDescription
            ),
            modifiers = modifiers,
        )
    }
}

// ============================================
// IMAGE GALLERY
// ============================================

object ImageGalleryMapper {
    fun map(component: ImageGallery, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        val columns = (1..component.columns).map {
            mapOf("type" to "flexible", "spacing" to component.spacing)
        }

        val children = component.images.mapIndexed { index, galleryImage ->
            SwiftUIView(
                type = ViewType.Custom("AsyncImage"),
                properties = mapOf(
                    "url" to galleryImage.url,
                    "description" to galleryImage.description
                ),
                modifiers = listOf(
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("aspectRatio" to component.aspectRatio)
                    ),
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("contentMode" to "fill")
                    ),
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("clipped" to true)
                    ),
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("cornerRadius" to 8f)
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Custom("LazyVGrid"),
            properties = mapOf(
                "columns" to columns,
                "spacing" to component.spacing
            ),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// LIGHTBOX
// ============================================

object LightboxMapper {
    fun map(component: Lightbox, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        if (!component.visible) {
            return SwiftUIView(type = ViewType.EmptyView, properties = emptyMap(), id = component.id)
        }

        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("fullScreenCover" to true)
        ))

        val children = mutableListOf<SwiftUIView>()

        // Black background
        children.add(SwiftUIView(
            type = ViewType.Custom("Color"),
            properties = mapOf("color" to "black"),
            modifiers = listOf(
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("ignoresSafeArea" to true)
                )
            )
        ))

        // Main image - use initialIndex
        val currentImage = component.images.getOrNull(component.initialIndex)
        currentImage?.let { imageData ->
            children.add(SwiftUIView(
                type = ViewType.Custom("AsyncImage"),
                properties = mapOf(
                    "url" to imageData.url,
                    "description" to imageData.description
                ),
                modifiers = listOf(
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("contentMode" to "fit")
                    ),
                    SwiftUIModifier(
                        type = ModifierType.Custom,
                        value = mapOf("pinchToZoom" to component.enableZoom, "maxZoom" to component.maxZoom)
                    )
                )
            ))
        }

        // Counter if shown
        if (component.showCounter) {
            children.add(SwiftUIView(
                type = ViewType.VStack,
                properties = mapOf("alignment" to "center"),
                children = listOf(
                    SwiftUIView.text(
                        content = "${component.initialIndex + 1} of ${component.images.size}",
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Caption),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                        )
                    ),
                    SwiftUIView(type = ViewType.Spacer, properties = emptyMap())
                )
            ))
        }

        // Close button overlay
        if (component.showCloseButton) {
            children.add(SwiftUIView(
                type = ViewType.VStack,
                properties = mapOf("alignment" to "trailing"),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Custom("Button"),
                        properties = mapOf("systemImage" to "xmark", "action" to "close"),
                        modifiers = listOf(
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                            SwiftUIModifier.padding(16f, 16f, 16f, 16f)
                        )
                    ),
                    SwiftUIView(type = ViewType.Spacer, properties = emptyMap())
                )
            ))
        }

        // Caption if shown
        if (component.showCaption) {
            currentImage?.caption?.let { caption ->
                children.add(SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf("alignment" to "center"),
                    children = listOf(
                        SwiftUIView(type = ViewType.Spacer, properties = emptyMap()),
                        SwiftUIView.text(
                            content = caption,
                            modifiers = listOf(
                                SwiftUIModifier.font(FontStyle.Body),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                                SwiftUIModifier.padding(16f, 16f, 32f, 16f)
                            )
                        )
                    )
                ))
            }
        }

        return SwiftUIView(
            type = ViewType.ZStack,
            properties = mapOf("alignment" to "center"),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// Helper Functions
// ============================================

private fun parseColor(colorString: String): SwiftUIColor {
    return when {
        colorString.startsWith("#") -> {
            val hex = colorString.removePrefix("#")
            val r = hex.substring(0, 2).toInt(16) / 255f
            val g = hex.substring(2, 4).toInt(16) / 255f
            val b = hex.substring(4, 6).toInt(16) / 255f
            val a = if (hex.length == 8) hex.substring(6, 8).toInt(16) / 255f else 1f
            SwiftUIColor.rgb(r, g, b, a)
        }
        colorString.equals("primary", ignoreCase = true) -> SwiftUIColor.primary
        colorString.equals("secondary", ignoreCase = true) -> SwiftUIColor.secondary
        colorString.equals("accent", ignoreCase = true) -> SwiftUIColor.system("accentColor")
        else -> SwiftUIColor.system(colorString)
    }
}
