package com.augmentalis.avaelements.renderer.ios

import com.augmentalis.avaelements.common.color.UniversalColor
import com.augmentalis.avaelements.common.color.ColorUtils
import com.augmentalis.avaelements.common.spacing.*
import com.augmentalis.avaelements.common.properties.PropertyExtractor
import com.augmentalis.avaelements.common.alignment.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * iOS Shared Utilities Bridge
 *
 * This file provides SwiftUI-compatible extensions for the shared utilities
 * from com.augmentalis.avaelements.common.*
 *
 * Architecture:
 * Shared Utilities (Core) → Bridge Extensions → iOS SwiftUI Bridge Models
 *
 * Purpose:
 * - Convert UniversalColor to SwiftUIColor
 * - Convert EdgeInsets to SwiftUI EdgeInsets
 * - Convert CornerRadius to SwiftUI corner radius values
 * - Convert Shadow to SwiftUI ShadowValue
 * - Convert Border to SwiftUI BorderValue
 * - Provide SwiftUI-compatible spacing and sizing helpers
 *
 * This bridge eliminates duplicate color/spacing code in iOS mappers
 * by leveraging the shared utilities created by Agent 1.
 *
 * @since 3.1.0-android-parity-ios
 */

// ═══════════════════════════════════════════════════════════════
// Color Bridge Extensions
// ═══════════════════════════════════════════════════════════════

/**
 * Convert UniversalColor to SwiftUIColor
 *
 * Maps platform-agnostic ARGB color to SwiftUI RGB representation.
 * SwiftUI uses 0.0-1.0 range for color components.
 */
fun UniversalColor.toSwiftUIColor(): SwiftUIColor {
    return SwiftUIColor.rgb(
        red = this.red,
        green = this.green,
        blue = this.blue,
        opacity = this.alpha
    )
}

/**
 * Convert hex string to SwiftUIColor via UniversalColor
 */
fun String.hexToSwiftUIColor(): SwiftUIColor {
    return UniversalColor.fromHex(this).toSwiftUIColor()
}

/**
 * Convert ARGB int to SwiftUIColor via UniversalColor
 */
fun Int.argbToSwiftUIColor(): SwiftUIColor {
    return UniversalColor.fromArgb(this).toSwiftUIColor()
}

/**
 * Parse color from component properties using PropertyExtractor,
 * then convert to SwiftUIColor.
 *
 * @param props Component properties map
 * @param key Property key
 * @param default Default SwiftUIColor if not found
 * @return SwiftUIColor instance
 */
fun parseColorFromProps(
    props: Map<String, Any?>,
    key: String,
    default: SwiftUIColor = SwiftUIColor.black
): SwiftUIColor {
    val argb = PropertyExtractor.getColorArgb(props, key, default.toArgb())
    return argb.argbToSwiftUIColor()
}

/**
 * Helper to convert SwiftUIColor back to ARGB int
 * (for internal use in bridge)
 */
private fun SwiftUIColor.toArgb(): Int {
    return when (this.type) {
        SwiftUIColor.ColorType.RGB -> {
            val rgb = this.value as RGBValue
            val a = (rgb.opacity * 255).toInt() and 0xFF
            val r = (rgb.red * 255).toInt() and 0xFF
            val g = (rgb.green * 255).toInt() and 0xFF
            val b = (rgb.blue * 255).toInt() and 0xFF
            (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        SwiftUIColor.ColorType.System -> {
            // System colors map to defaults
            when (this.value) {
                "black" -> 0xFF000000.toInt()
                "white" -> 0xFFFFFFFF.toInt()
                "red" -> 0xFFFF0000.toInt()
                "blue" -> 0xFF0000FF.toInt()
                "green" -> 0xFF00FF00.toInt()
                else -> 0xFF000000.toInt()
            }
        }
        else -> 0xFF000000.toInt()
    }
}

// ═══════════════════════════════════════════════════════════════
// Color Manipulation Bridge Extensions
// ═══════════════════════════════════════════════════════════════

/**
 * Lighten a SwiftUIColor by a factor (0.0 - 1.0)
 */
fun SwiftUIColor.lighten(factor: Float): SwiftUIColor {
    val universal = UniversalColor.fromArgb(this.toArgb())
    return ColorUtils.lighten(universal, factor).toSwiftUIColor()
}

/**
 * Darken a SwiftUIColor by a factor (0.0 - 1.0)
 */
fun SwiftUIColor.darken(factor: Float): SwiftUIColor {
    val universal = UniversalColor.fromArgb(this.toArgb())
    return ColorUtils.darken(universal, factor).toSwiftUIColor()
}

/**
 * Adjust alpha/opacity of a SwiftUIColor
 */
fun SwiftUIColor.withAlpha(alpha: Float): SwiftUIColor {
    val universal = UniversalColor.fromArgb(this.toArgb())
    return ColorUtils.withAlpha(universal, alpha).toSwiftUIColor()
}

/**
 * Mix two SwiftUIColors
 * @param other The color to mix with
 * @param ratio 0.0 = 100% this color, 1.0 = 100% other color
 */
fun SwiftUIColor.mix(other: SwiftUIColor, ratio: Float): SwiftUIColor {
    val universal1 = UniversalColor.fromArgb(this.toArgb())
    val universal2 = UniversalColor.fromArgb(other.toArgb())
    return ColorUtils.mix(universal1, universal2, ratio).toSwiftUIColor()
}

/**
 * Get contrasting foreground color (black or white) for this background
 */
fun SwiftUIColor.contrastingForeground(): SwiftUIColor {
    val universal = UniversalColor.fromArgb(this.toArgb())
    return ColorUtils.contrastingForeground(universal).toSwiftUIColor()
}

// ═══════════════════════════════════════════════════════════════
// Spacing Bridge Extensions
// ═══════════════════════════════════════════════════════════════

/**
 * Convert shared EdgeInsets to SwiftUI EdgeInsets bridge model
 *
 * Shared EdgeInsets uses start/end for RTL support.
 * SwiftUI EdgeInsets uses leading/trailing.
 */
fun com.augmentalis.avaelements.common.spacing.EdgeInsets.toSwiftUI(): com.augmentalis.avaelements.renderer.ios.bridge.EdgeInsets {
    return com.augmentalis.avaelements.renderer.ios.bridge.EdgeInsets(
        top = this.top,
        leading = this.start,
        bottom = this.bottom,
        trailing = this.end
    )
}

/**
 * Parse EdgeInsets from component properties
 */
fun parseEdgeInsetsFromProps(
    props: Map<String, Any?>,
    key: String = "padding"
): com.augmentalis.avaelements.common.spacing.EdgeInsets {
    val map = PropertyExtractor.getMap(props, key, emptyMap())
    return com.augmentalis.avaelements.common.spacing.EdgeInsets.fromMap(map)
}

/**
 * Create SwiftUI padding modifier from shared EdgeInsets
 */
fun com.augmentalis.avaelements.common.spacing.EdgeInsets.toPaddingModifier(): SwiftUIModifier {
    return if (this.isZero) {
        SwiftUIModifier.padding(0f)
    } else {
        SwiftUIModifier.padding(
            top = this.top,
            leading = this.start,
            bottom = this.bottom,
            trailing = this.end
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Corner Radius Bridge Extensions
// ═══════════════════════════════════════════════════════════════

/**
 * Convert shared CornerRadius to SwiftUI corner radius modifier
 *
 * If uniform, uses single value. Otherwise, would need RoundedRectangle
 * with individual corner radii (SwiftUI 3.0+).
 */
fun CornerRadius.toSwiftUIModifier(): SwiftUIModifier {
    return if (this.isUniform) {
        SwiftUIModifier.cornerRadius(this.uniform)
    } else {
        // For non-uniform corners, use the top-leading as primary
        // Note: Full non-uniform corner support requires RoundedRectangle shape
        SwiftUIModifier.cornerRadius(this.topStart)
    }
}

/**
 * Parse CornerRadius from component properties
 */
fun parseCornerRadiusFromProps(
    props: Map<String, Any?>,
    key: String = "cornerRadius"
): CornerRadius {
    return CornerRadius.from(props[key])
}

// ═══════════════════════════════════════════════════════════════
// Shadow Bridge Extensions
// ═══════════════════════════════════════════════════════════════

/**
 * Convert shared Shadow to SwiftUI ShadowValue
 */
fun Shadow.toSwiftUI(): ShadowValueWithColor {
    return ShadowValueWithColor(
        color = this.color.argbToSwiftUIColor(),
        radius = this.blurRadius,
        x = this.offsetX,
        y = this.offsetY
    )
}

/**
 * Parse Shadow from component properties
 */
fun parseShadowFromProps(
    props: Map<String, Any?>,
    key: String = "shadow"
): Shadow? {
    val map = PropertyExtractor.getMap(props, key)
    return if (map.isNotEmpty()) {
        Shadow.fromMap(map)
    } else {
        null
    }
}

/**
 * Create SwiftUI shadow modifier from shared Shadow
 */
fun Shadow.toSwiftUIModifier(): SwiftUIModifier {
    return SwiftUIModifier.shadow(
        radius = this.blurRadius,
        x = this.offsetX,
        y = this.offsetY
    )
}

// ═══════════════════════════════════════════════════════════════
// Border Bridge Extensions
// ═══════════════════════════════════════════════════════════════

/**
 * Convert shared Border to SwiftUI BorderValue
 */
fun Border.toSwiftUI(): BorderValue {
    return BorderValue(
        color = this.color.argbToSwiftUIColor(),
        width = this.width
    )
}

/**
 * Parse Border from component properties
 */
fun parseBorderFromProps(
    props: Map<String, Any?>,
    key: String = "border"
): Border? {
    val map = PropertyExtractor.getMap(props, key)
    return if (map.isNotEmpty()) {
        val width = PropertyExtractor.getFloat(map, "width", 0f)
        val color = PropertyExtractor.getColorArgb(map, "color", 0xFF000000.toInt())
        val style = PropertyExtractor.getEnum(map, "style", BorderStyle.Solid)
        Border(width, color, style)
    } else {
        null
    }
}

/**
 * Create SwiftUI border modifier from shared Border
 */
fun Border.toSwiftUIModifier(): SwiftUIModifier {
    return SwiftUIModifier.border(
        color = this.color.argbToSwiftUIColor(),
        width = this.width
    )
}

// ═══════════════════════════════════════════════════════════════
// Size Bridge Extensions
// ═══════════════════════════════════════════════════════════════

/**
 * Convert shared Size to SwiftUI SizeValue
 */
fun Size.toSwiftUIWidth(): SizeValue? {
    return if (this.width.isNaN()) null else SizeValue.Fixed(this.width)
}

fun Size.toSwiftUIHeight(): SizeValue? {
    return if (this.height.isNaN()) null else SizeValue.Fixed(this.height)
}

/**
 * Parse Size from component properties
 */
fun parseSizeFromProps(
    props: Map<String, Any?>,
    widthKey: String = "width",
    heightKey: String = "height"
): Size {
    val width = PropertyExtractor.getFloat(props, widthKey, Float.NaN)
    val height = PropertyExtractor.getFloat(props, heightKey, Float.NaN)
    return Size(width, height)
}

/**
 * Create SwiftUI frame modifier from shared Size
 */
fun Size.toFrameModifier(alignment: ZStackAlignment = ZStackAlignment.Center): SwiftUIModifier {
    return SwiftUIModifier.frame(
        width = this.toSwiftUIWidth(),
        height = this.toSwiftUIHeight(),
        alignment = alignment
    )
}

// ═══════════════════════════════════════════════════════════════
// Alignment Bridge Extensions
// ═══════════════════════════════════════════════════════════════

/**
 * Convert shared HorizontalAlignment to SwiftUI HorizontalAlignment
 */
fun com.augmentalis.avaelements.common.alignment.HorizontalAlignment.toSwiftUI(): com.augmentalis.avaelements.renderer.ios.bridge.HorizontalAlignment {
    return when (this) {
        com.augmentalis.avaelements.common.alignment.HorizontalAlignment.Start ->
            com.augmentalis.avaelements.renderer.ios.bridge.HorizontalAlignment.Leading
        com.augmentalis.avaelements.common.alignment.HorizontalAlignment.End ->
            com.augmentalis.avaelements.renderer.ios.bridge.HorizontalAlignment.Trailing
        com.augmentalis.avaelements.common.alignment.HorizontalAlignment.Center ->
            com.augmentalis.avaelements.renderer.ios.bridge.HorizontalAlignment.Center
    }
}

/**
 * Convert shared VerticalAlignment to SwiftUI VerticalAlignment
 */
fun com.augmentalis.avaelements.common.alignment.VerticalAlignment.toSwiftUI(): com.augmentalis.avaelements.renderer.ios.bridge.VerticalAlignment {
    return when (this) {
        com.augmentalis.avaelements.common.alignment.VerticalAlignment.Top ->
            com.augmentalis.avaelements.renderer.ios.bridge.VerticalAlignment.Top
        com.augmentalis.avaelements.common.alignment.VerticalAlignment.Bottom ->
            com.augmentalis.avaelements.renderer.ios.bridge.VerticalAlignment.Bottom
        com.augmentalis.avaelements.common.alignment.VerticalAlignment.Center ->
            com.augmentalis.avaelements.renderer.ios.bridge.VerticalAlignment.Center
    }
}

/**
 * Parse horizontal alignment from component properties
 */
fun parseHorizontalAlignmentFromProps(
    props: Map<String, Any?>,
    key: String = "alignment"
): com.augmentalis.avaelements.renderer.ios.bridge.HorizontalAlignment {
    val value = PropertyExtractor.getString(props, key, "center").lowercase()
    return when (value) {
        "start", "leading", "left" -> com.augmentalis.avaelements.renderer.ios.bridge.HorizontalAlignment.Leading
        "end", "trailing", "right" -> com.augmentalis.avaelements.renderer.ios.bridge.HorizontalAlignment.Trailing
        else -> com.augmentalis.avaelements.renderer.ios.bridge.HorizontalAlignment.Center
    }
}

/**
 * Parse vertical alignment from component properties
 */
fun parseVerticalAlignmentFromProps(
    props: Map<String, Any?>,
    key: String = "alignment"
): com.augmentalis.avaelements.renderer.ios.bridge.VerticalAlignment {
    val value = PropertyExtractor.getString(props, key, "center").lowercase()
    return when (value) {
        "top" -> com.augmentalis.avaelements.renderer.ios.bridge.VerticalAlignment.Top
        "bottom" -> com.augmentalis.avaelements.renderer.ios.bridge.VerticalAlignment.Bottom
        else -> com.augmentalis.avaelements.renderer.ios.bridge.VerticalAlignment.Center
    }
}

// ═══════════════════════════════════════════════════════════════
// Spacing Scale Helpers
// ═══════════════════════════════════════════════════════════════

/**
 * Get spacing value from SpacingScale by name or multiplier
 *
 * Usage:
 * ```kotlin
 * val spacing = getSpacing("lg")  // 16dp
 * val custom = getSpacing("4")    // 16dp (4 * base unit)
 * ```
 */
fun getSpacing(name: String): Float {
    return SpacingScale.byName(name)
}

/**
 * Parse spacing from component properties
 */
fun parseSpacingFromProps(
    props: Map<String, Any?>,
    key: String = "spacing",
    default: Float = SpacingScale.MD
): Float {
    val value = props[key]
    return when (value) {
        is Number -> value.toFloat()
        is String -> SpacingScale.byName(value)
        else -> default
    }
}

// ═══════════════════════════════════════════════════════════════
// PropertyExtractor Convenience Extensions for iOS
// ═══════════════════════════════════════════════════════════════

/**
 * Extract all common styling properties from a component's properties map
 * and return a list of SwiftUI modifiers.
 *
 * Handles:
 * - padding: EdgeInsets → padding modifier
 * - cornerRadius: CornerRadius → cornerRadius modifier
 * - shadow: Shadow → shadow modifier
 * - border: Border → border modifier
 * - backgroundColor: Color → background modifier
 * - size: width/height → frame modifier
 *
 * @param props Component properties map
 * @return List of SwiftUI modifiers
 */
fun extractCommonModifiers(props: Map<String, Any?>): List<SwiftUIModifier> {
    val modifiers = mutableListOf<SwiftUIModifier>()

    // Padding
    val paddingMap = PropertyExtractor.getMap(props, "padding")
    if (paddingMap.isNotEmpty()) {
        val edgeInsets = com.augmentalis.avaelements.common.spacing.EdgeInsets.fromMap(paddingMap)
        if (!edgeInsets.isZero) {
            modifiers.add(edgeInsets.toPaddingModifier())
        }
    }

    // Corner radius
    props["cornerRadius"]?.let { value ->
        val cornerRadius = CornerRadius.from(value)
        modifiers.add(cornerRadius.toSwiftUIModifier())
    }

    // Shadow
    val shadowMap = PropertyExtractor.getMap(props, "shadow")
    if (shadowMap.isNotEmpty()) {
        val shadow = Shadow.fromMap(shadowMap)
        if (shadow.isVisible) {
            modifiers.add(shadow.toSwiftUIModifier())
        }
    }

    // Border
    val borderMap = PropertyExtractor.getMap(props, "border")
    if (borderMap.isNotEmpty()) {
        val border = parseBorderFromProps(props, "border")
        if (border != null && border.isVisible) {
            modifiers.add(border.toSwiftUIModifier())
        }
    }

    // Background color
    if (props.containsKey("backgroundColor")) {
        val bgColor = parseColorFromProps(props, "backgroundColor")
        modifiers.add(SwiftUIModifier.background(bgColor))
    }

    // Size (width/height)
    val width = PropertyExtractor.getFloat(props, "width", Float.NaN)
    val height = PropertyExtractor.getFloat(props, "height", Float.NaN)
    if (!width.isNaN() || !height.isNaN()) {
        val size = Size(width, height)
        modifiers.add(size.toFrameModifier())
    }

    return modifiers
}

/**
 * Extract text color from properties, with fallback to theme primary
 */
fun extractTextColor(
    props: Map<String, Any?>,
    key: String = "color",
    default: SwiftUIColor = SwiftUIColor.primary
): SwiftUIColor {
    return if (props.containsKey(key)) {
        parseColorFromProps(props, key, default)
    } else {
        default
    }
}

/**
 * Extract boolean enabled state from properties
 */
fun extractEnabled(props: Map<String, Any?>): Boolean {
    return PropertyExtractor.getBoolean(props, "enabled", true)
}

/**
 * Extract accessibility label from properties
 */
fun extractAccessibilityLabel(props: Map<String, Any?>, default: String = ""): String {
    return PropertyExtractor.getString(props, "accessibilityLabel", default)
}

/**
 * Extract accessibility hint from properties
 */
fun extractAccessibilityHint(props: Map<String, Any?>, default: String = ""): String {
    return PropertyExtractor.getString(props, "accessibilityHint", default)
}
