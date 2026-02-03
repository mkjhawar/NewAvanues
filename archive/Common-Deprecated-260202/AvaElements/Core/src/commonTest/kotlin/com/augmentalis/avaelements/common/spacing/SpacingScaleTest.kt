package com.augmentalis.avaelements.common.spacing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for extensible SpacingScale system.
 *
 * Demonstrates how the SOLID refactoring enables:
 * - Custom spacing scales
 * - Platform-specific spacing
 * - Dynamic spacing changes
 * - Backward compatibility
 */
class SpacingScaleTest {

    // ═══════════════════════════════════════════════════════════════
    // Default Material Spacing Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testDefaultSpacingValues() {
        SpacingScale.resetToDefault()

        assertEquals(4f, SpacingScale.Base, "Base should be 4dp")
        assertEquals(0f, SpacingScale.None, "None should be 0dp")
        assertEquals(2f, SpacingScale.XXS, "XXS should be 2dp")
        assertEquals(4f, SpacingScale.XS, "XS should be 4dp")
        assertEquals(8f, SpacingScale.SM, "SM should be 8dp")
        assertEquals(12f, SpacingScale.MD, "MD should be 12dp")
        assertEquals(16f, SpacingScale.LG, "LG should be 16dp")
        assertEquals(24f, SpacingScale.XL, "XL should be 24dp")
        assertEquals(32f, SpacingScale.XXL, "XXL should be 32dp")
        assertEquals(48f, SpacingScale.XXXL, "XXXL should be 48dp")
    }

    @Test
    fun testGetByMultiplier() {
        SpacingScale.resetToDefault()

        assertEquals(20f, SpacingScale.get(5f), "5 * base should be 20dp")
        assertEquals(16f, SpacingScale.get(4f), "4 * base should be 16dp")
        assertEquals(2f, SpacingScale.get(0.5f), "0.5 * base should be 2dp")
    }

    @Test
    fun testGetByName() {
        SpacingScale.resetToDefault()

        assertEquals(0f, SpacingScale.byName("none"))
        assertEquals(2f, SpacingScale.byName("xxs"))
        assertEquals(4f, SpacingScale.byName("xs"))
        assertEquals(8f, SpacingScale.byName("sm"))
        assertEquals(12f, SpacingScale.byName("md"))
        assertEquals(16f, SpacingScale.byName("lg"))
        assertEquals(24f, SpacingScale.byName("xl"))
        assertEquals(32f, SpacingScale.byName("xxl"))
        assertEquals(48f, SpacingScale.byName("xxxl"))
    }

    @Test
    fun testGetByNameCaseInsensitive() {
        SpacingScale.resetToDefault()

        assertEquals(12f, SpacingScale.byName("MD"))
        assertEquals(12f, SpacingScale.byName("md"))
        assertEquals(12f, SpacingScale.byName("Md"))
        assertEquals(12f, SpacingScale.byName("medium"))
    }

    @Test
    fun testGetByNameFallback() {
        SpacingScale.resetToDefault()

        // Invalid name should return MD as default
        assertEquals(12f, SpacingScale.byName("invalid"))

        // Numeric string should parse
        assertEquals(25f, SpacingScale.byName("25"))
    }

    // ═══════════════════════════════════════════════════════════════
    // Custom Spacing Provider Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testCustomSpacingProvider() {
        // Create 8dp base spacing
        SpacingScale.setProvider(LargeSpacingScale)

        assertEquals(8f, SpacingScale.Base, "Custom base should be 8dp")
        assertEquals(16f, SpacingScale.MD, "Custom MD should be 16dp")
        assertEquals(24f, SpacingScale.LG, "Custom LG should be 24dp")

        // Reset to default
        SpacingScale.resetToDefault()
        assertEquals(4f, SpacingScale.Base, "Should reset to 4dp base")
    }

    @Test
    fun testCompactSpacingProvider() {
        // Create 2dp base spacing for mobile
        SpacingScale.setProvider(CompactSpacingScale)

        assertEquals(2f, SpacingScale.Base, "Compact base should be 2dp")
        assertEquals(6f, SpacingScale.MD, "Compact MD should be 6dp")
        assertEquals(8f, SpacingScale.LG, "Compact LG should be 8dp")

        SpacingScale.resetToDefault()
    }

    @Test
    fun testAppleSpacingProvider() {
        SpacingScale.setProvider(AppleSpacingScale)

        assertEquals(8f, SpacingScale.Base, "Apple base should be 8dp")
        assertEquals(16f, SpacingScale.MD, "Apple MD should be 16dp")
        assertEquals(44f, SpacingScale.XXXL, "Apple XXXL should be 44dp (tap target)")

        SpacingScale.resetToDefault()
    }

    // ═══════════════════════════════════════════════════════════════
    // Dynamic Spacing Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testAccessibilitySpacing() {
        SpacingScale.resetToDefault()
        val normalMd = SpacingScale.MD

        // Enable 1.5x accessibility scaling
        SpacingScale.setProvider(AccessibilitySpacingScale(1.5f))
        val accessibilityMd = SpacingScale.MD

        assertEquals(normalMd * 1.5f, accessibilityMd, "Accessibility MD should be 1.5x")

        SpacingScale.resetToDefault()
    }

    @Test
    fun testDynamicProviderSwitch() {
        // Start with Material
        SpacingScale.resetToDefault()
        val materialMd = SpacingScale.MD

        // Switch to Large
        SpacingScale.setProvider(LargeSpacingScale)
        val largeMd = SpacingScale.MD

        assertNotEquals(materialMd, largeMd, "Different providers should have different values")

        // Switch to Compact
        SpacingScale.setProvider(CompactSpacingScale)
        val compactMd = SpacingScale.MD

        assertTrue(compactMd < materialMd, "Compact should be smaller than Material")
        assertTrue(largeMd > materialMd, "Large should be bigger than Material")

        SpacingScale.resetToDefault()
    }

    // ═══════════════════════════════════════════════════════════════
    // EdgeInsets Integration Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testEdgeInsetsWithDefaultSpacing() {
        SpacingScale.resetToDefault()

        val padding = EdgeInsets.all(SpacingScale.MD)
        assertEquals(12f, padding.start)
        assertEquals(12f, padding.top)
        assertEquals(12f, padding.end)
        assertEquals(12f, padding.bottom)
    }

    @Test
    fun testEdgeInsetsWithCustomSpacing() {
        SpacingScale.setProvider(LargeSpacingScale)

        val padding = EdgeInsets.all(SpacingScale.MD)
        assertEquals(16f, padding.start, "Padding should use custom spacing")

        SpacingScale.resetToDefault()
    }

    @Test
    fun testEdgeInsetsSymmetric() {
        SpacingScale.resetToDefault()

        val padding = EdgeInsets.symmetric(
            horizontal = SpacingScale.LG,
            vertical = SpacingScale.SM
        )

        assertEquals(16f, padding.start)
        assertEquals(8f, padding.top)
        assertEquals(16f, padding.end)
        assertEquals(8f, padding.bottom)
    }

    // ═══════════════════════════════════════════════════════════════
    // Provider Isolation Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testGetProviderReturnsCurrentProvider() {
        SpacingScale.resetToDefault()
        val defaultProvider = SpacingScale.getProvider()
        assertTrue(defaultProvider is MaterialSpacingScale, "Default should be Material")

        SpacingScale.setProvider(LargeSpacingScale)
        val customProvider = SpacingScale.getProvider()
        assertTrue(customProvider is LargeSpacingScale, "Should return custom provider")

        SpacingScale.resetToDefault()
    }

    @Test
    fun testProviderIsolation() {
        // Save original
        val original = SpacingScale.getProvider()

        try {
            // Change provider
            SpacingScale.setProvider(LargeSpacingScale)
            val md1 = SpacingScale.MD

            // Change again
            SpacingScale.setProvider(CompactSpacingScale)
            val md2 = SpacingScale.MD

            assertNotEquals(md1, md2, "Different providers should give different values")
        } finally {
            // Always restore
            SpacingScale.setProvider(original)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Backward Compatibility Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testBackwardCompatibilityConstAccess() {
        SpacingScale.resetToDefault()

        // All const-like access patterns should work
        val base = SpacingScale.Base
        val md = SpacingScale.MD
        val lg = SpacingScale.LG

        assertEquals(4f, base)
        assertEquals(12f, md)
        assertEquals(16f, lg)
    }

    @Test
    fun testBackwardCompatibilityMethods() {
        SpacingScale.resetToDefault()

        // All existing methods should work
        val byMultiplier = SpacingScale.get(3f)
        val byName = SpacingScale.byName("lg")

        assertEquals(12f, byMultiplier)
        assertEquals(16f, byName)
    }
}

// ═══════════════════════════════════════════════════════════════
// Test Spacing Providers
// ═══════════════════════════════════════════════════════════════

/**
 * Large spacing scale with 8dp base (for desktop/tablet)
 */
private object LargeSpacingScale : SpacingScaleProvider {
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
 * Compact spacing scale with 2dp base (for small mobile screens)
 */
private object CompactSpacingScale : SpacingScaleProvider {
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
 * Apple HIG spacing scale
 */
private object AppleSpacingScale : SpacingScaleProvider {
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
 * Accessibility spacing scale with configurable multiplier
 */
private class AccessibilitySpacingScale(
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
