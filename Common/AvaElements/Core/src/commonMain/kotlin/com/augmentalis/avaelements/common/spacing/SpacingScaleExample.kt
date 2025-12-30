package com.augmentalis.avaelements.common.spacing

/**
 * Example usage of the extensible SpacingScale system.
 *
 * This file demonstrates how the SOLID refactoring enables:
 * 1. Custom spacing scales for different design systems
 * 2. Per-platform spacing customization
 * 3. Backward compatibility with existing code
 */

// ═══════════════════════════════════════════════════════════════
// Example 1: Default Usage (Backward Compatible)
// ═══════════════════════════════════════════════════════════════

/**
 * Standard usage - works exactly as before
 */
fun example1_DefaultUsage() {
    // All existing usages work unchanged
    val padding = SpacingScale.MD  // 12dp
    val margin = SpacingScale.LG   // 16dp

    println("Padding: $padding")
    println("Margin: $margin")

    // Get by multiplier
    val custom = SpacingScale.get(5f)  // 20dp (4 * 5)
    println("Custom: $custom")

    // Get by name
    val named = SpacingScale.byName("xl")  // 24dp
    println("Named: $named")
}

// ═══════════════════════════════════════════════════════════════
// Example 2: Custom Spacing Scale (8dp base)
// ═══════════════════════════════════════════════════════════════

/**
 * Custom spacing scale with 8dp base unit
 */
object LargeSpacingScale : SpacingScaleProvider {
    override val base = 8f
    override val none = 0f
    override val xxs = base * 0.25f   // 2dp
    override val xs = base * 0.5f     // 4dp
    override val sm = base * 1f       // 8dp
    override val md = base * 2f       // 16dp
    override val lg = base * 3f       // 24dp
    override val xl = base * 4f       // 32dp
    override val xxl = base * 6f      // 48dp
    override val xxxl = base * 8f     // 64dp
}

/**
 * Use custom spacing scale
 */
fun example2_CustomSpacing() {
    // Switch to 8dp base
    SpacingScale.setProvider(LargeSpacingScale)

    println("MD with 8dp base: ${SpacingScale.MD}")  // 16dp instead of 12dp
    println("LG with 8dp base: ${SpacingScale.LG}")  // 24dp instead of 16dp

    // Reset to default
    SpacingScale.resetToDefault()
    println("MD with 4dp base: ${SpacingScale.MD}")  // Back to 12dp
}

// ═══════════════════════════════════════════════════════════════
// Example 3: Compact Spacing (Mobile)
// ═══════════════════════════════════════════════════════════════

/**
 * Compact spacing scale for mobile devices
 */
object CompactSpacingScale : SpacingScaleProvider {
    override val base = 2f
    override val none = 0f
    override val xxs = base * 0.5f    // 1dp
    override val xs = base * 1f       // 2dp
    override val sm = base * 2f       // 4dp
    override val md = base * 3f       // 6dp
    override val lg = base * 4f       // 8dp
    override val xl = base * 6f       // 12dp
    override val xxl = base * 8f      // 16dp
    override val xxxl = base * 12f    // 24dp
}

/**
 * Use compact spacing for small screens
 */
fun example3_CompactSpacing(isSmallScreen: Boolean) {
    if (isSmallScreen) {
        SpacingScale.setProvider(CompactSpacingScale)
        println("Using compact spacing for small screen")
    } else {
        SpacingScale.setProvider(MaterialSpacingScale)
        println("Using standard Material spacing")
    }
}

// ═══════════════════════════════════════════════════════════════
// Example 4: Apple HIG Spacing (iOS)
// ═══════════════════════════════════════════════════════════════

/**
 * Spacing scale based on Apple Human Interface Guidelines
 */
object AppleSpacingScale : SpacingScaleProvider {
    override val base = 8f
    override val none = 0f
    override val xxs = 4f     // Minimum spacing
    override val xs = 8f      // Extra small
    override val sm = 12f     // Small
    override val md = 16f     // Standard
    override val lg = 20f     // Large
    override val xl = 24f     // Extra large
    override val xxl = 32f    // 2X large
    override val xxxl = 44f   // 3X large (tap target size)
}

/**
 * Use Apple HIG spacing on iOS
 */
fun example4_AppleSpacing() {
    SpacingScale.setProvider(AppleSpacingScale)
    println("Standard spacing (Apple HIG): ${SpacingScale.MD}")  // 16dp
}

// ═══════════════════════════════════════════════════════════════
// Example 5: Platform-Specific Initialization
// ═══════════════════════════════════════════════════════════════

/**
 * Initialize spacing based on platform
 */
fun example5_PlatformSpecific(platform: String) {
    when (platform) {
        "android" -> {
            SpacingScale.setProvider(MaterialSpacingScale)
            println("Using Material Design spacing (4dp base)")
        }
        "ios" -> {
            SpacingScale.setProvider(AppleSpacingScale)
            println("Using Apple HIG spacing")
        }
        "desktop" -> {
            SpacingScale.setProvider(LargeSpacingScale)
            println("Using large spacing for desktop (8dp base)")
        }
        "mobile-compact" -> {
            SpacingScale.setProvider(CompactSpacingScale)
            println("Using compact spacing (2dp base)")
        }
        else -> {
            SpacingScale.resetToDefault()
            println("Using default Material spacing")
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Example 6: Dynamic Spacing (Accessibility)
// ═══════════════════════════════════════════════════════════════

/**
 * Accessibility-friendly spacing scale with larger values
 */
class AccessibilitySpacingScale(
    private val scaleFactor: Float = 1.5f
) : SpacingScaleProvider {
    private val baseScale = MaterialSpacingScale

    override val base: Float get() = baseScale.base * scaleFactor
    override val none: Float get() = baseScale.none
    override val xxs: Float get() = baseScale.xxs * scaleFactor
    override val xs: Float get() = baseScale.xs * scaleFactor
    override val sm: Float get() = baseScale.sm * scaleFactor
    override val md: Float get() = baseScale.md * scaleFactor
    override val lg: Float get() = baseScale.lg * scaleFactor
    override val xl: Float get() = baseScale.xl * scaleFactor
    override val xxl: Float get() = baseScale.xxl * scaleFactor
    override val xxxl: Float get() = baseScale.xxxl * scaleFactor
}

/**
 * Enable accessibility mode with larger spacing
 */
fun example6_AccessibilityMode(enabled: Boolean, scaleFactor: Float = 1.5f) {
    if (enabled) {
        SpacingScale.setProvider(AccessibilitySpacingScale(scaleFactor))
        println("Accessibility spacing enabled (${scaleFactor}x)")
        println("Standard spacing: ${SpacingScale.MD}")  // 18dp (12 * 1.5)
    } else {
        SpacingScale.resetToDefault()
        println("Accessibility spacing disabled")
        println("Standard spacing: ${SpacingScale.MD}")  // 12dp
    }
}

// ═══════════════════════════════════════════════════════════════
// Example 7: EdgeInsets with Custom Spacing
// ═══════════════════════════════════════════════════════════════

/**
 * Create EdgeInsets using custom spacing scale
 */
fun example7_EdgeInsetsWithCustomSpacing() {
    // Switch to large spacing
    SpacingScale.setProvider(LargeSpacingScale)

    // Create padding using spacing scale
    val padding = EdgeInsets.all(SpacingScale.MD)  // 16dp with large scale
    val margin = EdgeInsets.symmetric(
        horizontal = SpacingScale.LG,  // 24dp
        vertical = SpacingScale.SM     // 8dp
    )

    println("Padding: $padding")
    println("Margin: $margin")

    // Reset
    SpacingScale.resetToDefault()
}

// ═══════════════════════════════════════════════════════════════
// Example 8: Testing Custom Spacing
// ═══════════════════════════════════════════════════════════════

/**
 * Test spacing scale provider
 */
object TestSpacingScale : SpacingScaleProvider {
    override val base = 1f
    override val none = 0f
    override val xxs = 1f
    override val xs = 2f
    override val sm = 3f
    override val md = 4f
    override val lg = 5f
    override val xl = 6f
    override val xxl = 7f
    override val xxxl = 8f
}

/**
 * Unit test example
 */
fun example8_TestCustomSpacing() {
    val originalProvider = SpacingScale.getProvider()

    try {
        // Use test spacing
        SpacingScale.setProvider(TestSpacingScale)

        // Verify spacing values
        require(SpacingScale.Base == 1f) { "Base spacing should be 1f" }
        require(SpacingScale.MD == 4f) { "MD spacing should be 4f" }
        require(SpacingScale.XL == 6f) { "XL spacing should be 6f" }

        println("Test spacing verified")
    } finally {
        // Always restore original provider
        SpacingScale.setProvider(originalProvider)
    }
}
