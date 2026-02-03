package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.feedback.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.renderer.ios.bridge.SwiftUIColor.ColorType

/**
 * Flutter Material Feedback Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity feedback components to SwiftUI equivalents.
 *
 * Components:
 * - Popup → Popover or sheet presentation
 * - Callout → VStack with rounded background and pointer
 * - Disclosure → DisclosureGroup with custom styling
 * - InfoPanel → HStack with info icon and content
 * - ErrorPanel → HStack with error icon and content
 * - WarningPanel → HStack with warning icon and content
 * - SuccessPanel → HStack with success icon and content
 * - FullPageLoading → ZStack with loading indicator
 * - AnimatedCheck → Animated checkmark view
 * - AnimatedError → Animated error icon view
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// POPUP
// ============================================

object PopupMapper {
    fun map(component: Popup, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Convert anchorPosition enum to string
        val arrowEdge = when (component.anchorPosition) {
            Popup.Position.TopStart, Popup.Position.TopCenter, Popup.Position.TopEnd -> "top"
            Popup.Position.BottomStart, Popup.Position.BottomCenter, Popup.Position.BottomEnd -> "bottom"
            Popup.Position.LeftStart, Popup.Position.LeftCenter, Popup.Position.LeftEnd -> "leading"
            Popup.Position.RightStart, Popup.Position.RightCenter, Popup.Position.RightEnd -> "trailing"
        }

        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf(
                "popover" to mapOf(
                    "isPresented" to component.visible,
                    "arrowEdge" to arrowEdge
                )
            )
        ))

        // Content
        val contentView = SwiftUIView.text(
            content = component.content,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        )

        val containerModifiers = mutableListOf<SwiftUIModifier>()
        containerModifiers.add(SwiftUIModifier.padding(16f, 16f, 16f, 16f))

        component.backgroundColor?.let { bg ->
            containerModifiers.add(SwiftUIModifier.background(parseColor(bg)))
        }

        component.width?.let { w ->
            containerModifiers.add(SwiftUIModifier.frame(
                width = SizeValue.Fixed(w),
                height = null
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = listOf(contentView),
            modifiers = containerModifiers + modifiers,
        )
    }
}

// ============================================
// CALLOUT
// ============================================

object CalloutMapper {
    fun map(component: Callout, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Get colors based on variant
        val (backgroundColor, iconColor) = when (component.variant) {
            Callout.Variant.Info -> Pair(
                SwiftUIColor.system("systemBlue").copy(alpha = 0.1f),
                SwiftUIColor.system("systemBlue")
            )
            Callout.Variant.Success -> Pair(
                SwiftUIColor.system("systemGreen").copy(alpha = 0.1f),
                SwiftUIColor.system("systemGreen")
            )
            Callout.Variant.Warning -> Pair(
                SwiftUIColor.system("systemOrange").copy(alpha = 0.1f),
                SwiftUIColor.system("systemOrange")
            )
            Callout.Variant.Error -> Pair(
                SwiftUIColor.system("systemRed").copy(alpha = 0.1f),
                SwiftUIColor.system("systemRed")
            )
        }

        modifiers.add(SwiftUIModifier.padding(12f, 16f, 12f, 16f))
        modifiers.add(SwiftUIModifier.background(backgroundColor))
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("cornerRadius" to 8f)
        ))
        modifiers.add(SwiftUIModifier.shadow(
            color = SwiftUIColor.rgb(0f, 0f, 0f, 0.1f),
            radius = 4f,
            x = 0f,
            y = 2f
        ))

        val children = mutableListOf<SwiftUIView>()

        // Icon - use effective icon from component
        val effectiveIcon = component.getEffectiveIcon()
        if (effectiveIcon != null) {
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to effectiveIcon, "size" to 20f),
                modifiers = listOf(
                    SwiftUIModifier.foregroundColor(iconColor)
                )
            ))
        }

        // Content VStack with title and message
        val contentChildren = mutableListOf<SwiftUIView>()

        // Title
        contentChildren.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Headline),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))

        // Message
        contentChildren.add(SwiftUIView.text(
            content = component.message,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))

        children.add(SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 4f),
            children = contentChildren
        ))

        return SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("alignment" to "top", "spacing" to 12f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// DISCLOSURE
// ============================================

object DisclosureMapper {
    fun map(component: Disclosure, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.padding(12f, 16f, 12f, 16f))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("secondarySystemBackground")))
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("cornerRadius" to 8f)
        ))

        // Label
        val labelView = SwiftUIView.text(
            content = component.title,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Headline),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        )

        // Content
        val contentView = SwiftUIView.text(
            content = component.content,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        )

        return SwiftUIView(
            type = ViewType.Custom("DisclosureGroup"),
            properties = mapOf("isExpanded" to component.initiallyExpanded),
            children = listOf(labelView, contentView),
            modifiers = modifiers,
        )
    }
}

// ============================================
// INFO PANEL
// ============================================

object InfoPanelMapper {
    fun map(component: InfoPanel, theme: Theme?): SwiftUIView {
        val effectiveIcon = component.icon ?: "info.circle.fill"
        return createPanel(
            component.id,
            component.title,
            component.message,
            effectiveIcon,
            SwiftUIColor.system("systemBlue"),
            theme
        )
    }
}

// ============================================
// ERROR PANEL
// ============================================

object ErrorPanelMapper {
    fun map(component: ErrorPanel, theme: Theme?): SwiftUIView {
        val effectiveIcon = component.icon ?: "exclamationmark.triangle.fill"
        return createPanel(
            component.id,
            component.title,
            component.message,
            effectiveIcon,
            SwiftUIColor.system("systemRed"),
            theme
        )
    }
}

// ============================================
// WARNING PANEL
// ============================================

object WarningPanelMapper {
    fun map(component: WarningPanel, theme: Theme?): SwiftUIView {
        val effectiveIcon = component.icon ?: "exclamationmark.triangle.fill"
        return createPanel(
            component.id,
            component.title,
            component.message,
            effectiveIcon,
            SwiftUIColor.system("systemOrange"),
            theme
        )
    }
}

// ============================================
// SUCCESS PANEL
// ============================================

object SuccessPanelMapper {
    fun map(component: SuccessPanel, theme: Theme?): SwiftUIView {
        val effectiveIcon = component.icon ?: "checkmark.circle.fill"
        return createPanel(
            component.id,
            component.title,
            component.message,
            effectiveIcon,
            SwiftUIColor.system("systemGreen"),
            theme
        )
    }
}

// ============================================
// FULL PAGE LOADING
// ============================================

object FullPageLoadingMapper {
    fun map(component: FullPageLoading, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Semi-transparent background
        children.add(SwiftUIView(
            type = ViewType.Custom("Color"),
            properties = mapOf("color" to "black"),
            modifiers = listOf(
                SwiftUIModifier.opacity(0.3f),
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("ignoresSafeArea" to true)
                )
            )
        ))

        // Loading indicator container
        val indicatorChildren = mutableListOf<SwiftUIView>()

        // Progress indicator - scale based on spinnerSize
        val scale = component.spinnerSize / 32f // Default ProgressView is ~32dp
        indicatorChildren.add(SwiftUIView(
            type = ViewType.Custom("ProgressView"),
            properties = mapOf("style" to "circular"),
            modifiers = listOf(
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("scaleEffect" to scale)
                ),
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("tint" to SwiftUIColor.white)
                )
            )
        ))

        // Loading message if present
        component.message?.let { message ->
            indicatorChildren.add(SwiftUIView.text(
                content = message,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Title3),
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
            properties = mapOf("spacing" to 20f),
            children = indicatorChildren,
            modifiers = listOf(
                SwiftUIModifier.padding(32f, 32f, 32f, 32f),
                SwiftUIModifier.background(SwiftUIColor.rgb(0f, 0f, 0f, 0.7f)),
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("cornerRadius" to 12f)
                )
            )
        ))

        return SwiftUIView(
            type = ViewType.ZStack,
            properties = mapOf("alignment" to "center"),
            children = children,
        )
    }
}

// ============================================
// ANIMATED CHECK
// ============================================

object AnimatedCheckMapper {
    fun map(component: AnimatedCheck, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(component.size),
            height = SizeValue.Fixed(component.size)
        ))

        // Animation based on visible state
        if (component.visible) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf(
                    "animation" to mapOf(
                        "type" to "spring",
                        "duration" to (component.animationDuration / 1000.0)
                    )
                )
            ))
        }

        val children = mutableListOf<SwiftUIView>()

        // Background circle - use color property
        val backgroundColor = component.color?.let { parseColor(it) }
            ?: SwiftUIColor.system("systemGreen")

        children.add(SwiftUIView(
            type = ViewType.Custom("Circle"),
            properties = emptyMap(),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(backgroundColor)
            )
        ))

        // Checkmark
        children.add(SwiftUIView(
            type = ViewType.Image,
            properties = mapOf("systemName" to "checkmark", "size" to (component.size * 0.5f)),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("bold" to true)
                )
            )
        ))

        return SwiftUIView(
            type = ViewType.ZStack,
            properties = mapOf("alignment" to "center"),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// ANIMATED ERROR
// ============================================

object AnimatedErrorMapper {
    fun map(component: AnimatedError, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(component.size),
            height = SizeValue.Fixed(component.size)
        ))

        // Animation based on visible state
        if (component.visible) {
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf(
                    "animation" to mapOf(
                        "type" to "spring",
                        "duration" to (component.animationDuration / 1000.0)
                    )
                )
            ))
            // Add shake effect
            modifiers.add(SwiftUIModifier(
                type = ModifierType.Custom,
                value = mapOf("shake" to component.shakeIntensity)
            ))
        }

        val children = mutableListOf<SwiftUIView>()

        // Background circle - use color property
        val backgroundColor = component.color?.let { parseColor(it) }
            ?: SwiftUIColor.system("systemRed")

        children.add(SwiftUIView(
            type = ViewType.Custom("Circle"),
            properties = emptyMap(),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(backgroundColor)
            )
        ))

        // X mark
        children.add(SwiftUIView(
            type = ViewType.Image,
            properties = mapOf("systemName" to "xmark", "size" to (component.size * 0.5f)),
            modifiers = listOf(
                SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("bold" to true)
                )
            )
        ))

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

/**
 * Create a standard panel with icon and content
 */
private fun createPanel(
    id: String?,
    title: String,
    message: String,
    icon: String,
    iconColor: SwiftUIColor,
    theme: Theme?
): SwiftUIView {
    val modifiers = mutableListOf<SwiftUIModifier>()

    modifiers.add(SwiftUIModifier.padding(12f, 16f, 12f, 16f))
    modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("secondarySystemBackground")))
    modifiers.add(SwiftUIModifier(
        type = ModifierType.Custom,
        value = mapOf("cornerRadius" to 8f)
    ))

    val children = mutableListOf<SwiftUIView>()

    // Icon
    children.add(SwiftUIView(
        type = ViewType.Image,
        properties = mapOf("systemName" to icon, "size" to 24f),
        modifiers = listOf(SwiftUIModifier.foregroundColor(iconColor))
    ))

    // Content VStack
    val contentChildren = mutableListOf<SwiftUIView>()

    contentChildren.add(SwiftUIView.text(
        content = title,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Headline),
            SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
        )
    ))

    contentChildren.add(SwiftUIView.text(
        content = message,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Body),
            SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
        )
    ))

    children.add(SwiftUIView(
        type = ViewType.VStack,
        properties = mapOf("alignment" to "leading", "spacing" to 4f),
        children = contentChildren
    ))

    return SwiftUIView(
        type = ViewType.HStack,
        properties = mapOf("alignment" to "top", "spacing" to 12f),
        children = children,
        modifiers = modifiers,
        id = id
    )
}

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

// Extension to create color with alpha
private fun SwiftUIColor.copy(alpha: Float): SwiftUIColor {
    return when (this.type) {
        ColorType.System -> SwiftUIColor.rgb(0.5f, 0.5f, 0.5f, alpha) // Approximation
        ColorType.RGB -> {
            val rgb = this.value as RGBValue
            SwiftUIColor.rgb(rgb.red, rgb.green, rgb.blue, alpha)
        }
        ColorType.Named -> this
    }
}
