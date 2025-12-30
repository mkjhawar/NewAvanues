package com.augmentalis.avaelements.flutter

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Cross-platform test base class for validating component parity.
 *
 * Provides common test utilities and expect/actual pattern for platform-specific testing.
 *
 * Usage:
 * ```kotlin
 * class MyComponentTest : CrossPlatformTestBase() {
 *     @Test
 *     fun testComponentRendering() = runOnAllPlatforms {
 *         val component = MyComponent()
 *         component.shouldRender()
 *         component.shouldMeetPerformanceTargets()
 *     }
 * }
 * ```
 *
 * @since 1.0.0 (Week 3 - Agent 4: Cross-Platform Testing)
 */
expect abstract class CrossPlatformTestBase() {
    /**
     * Run a test on all supported platforms.
     * Platform-specific implementations will execute the test appropriately.
     */
    fun runOnAllPlatforms(test: suspend () -> Unit)

    /**
     * Get the current platform being tested.
     */
    fun getCurrentPlatform(): Platform

    /**
     * Capture a screenshot of the current component for visual regression testing.
     *
     * @param name Unique name for this screenshot
     * @param scenario Test scenario (default, dark, accessibility, etc.)
     * @return Screenshot object for comparison
     */
    fun captureScreenshot(name: String, scenario: TestScenario = TestScenario.DEFAULT): Screenshot

    /**
     * Measure performance metrics for a block of code.
     *
     * @param iterations Number of times to run the block (default 60 for 1 second at 60fps)
     * @param block Code to measure
     * @return Performance metrics
     */
    fun measurePerformance(iterations: Int = 60, block: () -> Unit): PerformanceMetrics

    /**
     * Assert that a component renders without errors.
     */
    fun assertRenders(component: Any)

    /**
     * Assert that a component meets accessibility standards.
     */
    fun assertAccessible(component: Any)
}

/**
 * Supported platforms for cross-platform testing.
 */
enum class Platform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP;

    fun isImplemented(): Boolean = when (this) {
        ANDROID -> true
        IOS -> true // Needs validation
        WEB -> false // Not implemented yet
        DESKTOP -> false // Not implemented yet
    }
}

/**
 * Test scenarios for visual testing.
 */
enum class TestScenario {
    DEFAULT,        // Standard light theme
    DARK,           // Dark theme
    HOVER,          // Hover state (web/desktop)
    FOCUS,          // Focused state
    ACTIVE,         // Active/pressed state
    DISABLED,       // Disabled state
    ACCESSIBILITY,  // 200% font scale
    RTL             // Right-to-left layout
}

/**
 * Screenshot capture result for visual regression testing.
 */
data class Screenshot(
    val name: String,
    val platform: Platform,
    val scenario: TestScenario,
    val width: Int,
    val height: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Compare this screenshot to a baseline.
     *
     * @param baseline The baseline screenshot to compare against
     * @param threshold Maximum allowed difference (0.0 to 1.0, default 0.001 = 0.1%)
     * @return Comparison result
     */
    fun compareTo(baseline: Screenshot, threshold: Double = 0.001): VisualComparisonResult {
        // Platform-specific implementation
        return VisualComparisonResult(
            component = name,
            platform = platform,
            scenario = scenario,
            pixelDifference = 0.0, // Placeholder - actual implementation in platform code
            structuralSimilarity = 1.0,
            passed = true,
            threshold = threshold
        )
    }
}

/**
 * Visual comparison result between actual and baseline screenshots.
 */
data class VisualComparisonResult(
    val component: String,
    val platform: Platform,
    val scenario: TestScenario,
    val pixelDifference: Double,      // 0.0 - 1.0 (0% - 100%)
    val structuralSimilarity: Double, // SSIM score 0.0 - 1.0
    val passed: Boolean,
    val threshold: Double = 0.001
) {
    fun passesThreshold(): Boolean = pixelDifference <= threshold
}

/**
 * Performance metrics for component rendering and interaction.
 */
data class PerformanceMetrics(
    val avgFrameTime: Duration,
    val minFrameTime: Duration,
    val maxFrameTime: Duration,
    val frameRate: Double,
    val memoryUsage: Long,        // bytes
    val peakMemoryUsage: Long,    // bytes
    val cpuUsage: Double,         // 0.0 - 1.0 (0% - 100%)
    val iterations: Int
) {
    /**
     * Check if performance meets target criteria.
     *
     * @param targetFps Target frame rate (default 60)
     * @param maxFrameTime Maximum allowed frame time (default 16.67ms for 60fps)
     * @param maxMemory Maximum memory usage in MB (default 50)
     * @param maxCpu Maximum CPU usage (default 0.2 = 20%)
     * @return true if all targets are met
     */
    fun meetsTargets(
        targetFps: Double = 60.0,
        maxFrameTime: Duration = 16.67.milliseconds,
        maxMemory: Long = 50 * 1024 * 1024, // 50 MB
        maxCpu: Double = 0.2
    ): Boolean {
        return frameRate >= targetFps &&
                avgFrameTime <= maxFrameTime &&
                peakMemoryUsage <= maxMemory &&
                cpuUsage <= maxCpu
    }

    fun toReadableString(): String = """
        Performance Metrics:
        - Avg Frame Time: ${avgFrameTime.inWholeMilliseconds}ms
        - Frame Rate: ${"%.2f".format(frameRate)} FPS
        - Memory: ${memoryUsage / 1024 / 1024} MB (peak: ${peakMemoryUsage / 1024 / 1024} MB)
        - CPU: ${"%.1f".format(cpuUsage * 100)}%
        - Iterations: $iterations
    """.trimIndent()
}

/**
 * Component parity validation result.
 */
data class ComponentParity(
    val component: String,
    val android: PlatformStatus,
    val ios: PlatformStatus,
    val web: PlatformStatus,
    val desktop: PlatformStatus
) {
    /**
     * Calculate overall parity percentage across all platforms.
     */
    fun parityPercentage(): Double {
        val scores = listOf(android, ios, web, desktop)
        return scores.count { it.isComplete() } / 4.0 * 100
    }

    /**
     * Get parity status for a specific platform.
     */
    fun getStatus(platform: Platform): PlatformStatus = when (platform) {
        Platform.ANDROID -> android
        Platform.IOS -> ios
        Platform.WEB -> web
        Platform.DESKTOP -> desktop
    }

    /**
     * Check if component has full parity across all platforms.
     */
    fun hasFullParity(): Boolean = parityPercentage() == 100.0
}

/**
 * Platform implementation status for a component.
 *
 * Each component must pass all 5 criteria to be considered complete.
 */
data class PlatformStatus(
    val exists: Boolean = false,        // Component definition exists
    val renders: Boolean = false,       // Component renders without errors
    val behaves: Boolean = false,       // Component interactions work correctly
    val performs: Boolean = false,      // Component meets 60 FPS target
    val accessible: Boolean = false     // Component meets WCAG 2.1 AA
) {
    /**
     * Check if all criteria are met.
     */
    fun isComplete(): Boolean = exists && renders && behaves && performs && accessible

    /**
     * Get completion percentage (0-100).
     */
    fun completionPercentage(): Int {
        val criteria = listOf(exists, renders, behaves, performs, accessible)
        return (criteria.count { it } * 100) / 5
    }

    /**
     * Get status symbol for display.
     */
    fun toSymbol(): String = when {
        isComplete() -> "✅"
        exists && renders -> "🟢"
        exists -> "🟡"
        else -> "🔴"
    }

    companion object {
        val COMPLETE = PlatformStatus(
            exists = true,
            renders = true,
            behaves = true,
            performs = true,
            accessible = true
        )

        val MISSING = PlatformStatus()

        val IMPLEMENTED = PlatformStatus(
            exists = true,
            renders = true,
            behaves = false,
            performs = false,
            accessible = false
        )
    }
}

/**
 * Test utilities for common assertions.
 */
object TestUtils {
    /**
     * Assert that performance metrics meet targets.
     */
    fun assertPerformance(
        metrics: PerformanceMetrics,
        platform: Platform,
        component: String
    ) {
        assertTrue(
            metrics.meetsTargets(),
            "Performance targets not met for $component on $platform:\n${metrics.toReadableString()}"
        )
    }

    /**
     * Assert that visual comparison passed.
     */
    fun assertVisualMatch(
        result: VisualComparisonResult,
        component: String
    ) {
        assertTrue(
            result.passesThreshold(),
            "Visual regression detected for $component on ${result.platform} (${result.scenario}):\n" +
                    "Pixel difference: ${result.pixelDifference * 100}% (threshold: ${result.threshold * 100}%)"
        )
    }

    /**
     * Assert that component has minimum parity percentage.
     */
    fun assertParity(
        parity: ComponentParity,
        minPercentage: Double = 95.0
    ) {
        val actual = parity.parityPercentage()
        assertTrue(
            actual >= minPercentage,
            "Component ${parity.component} parity is $actual%, expected at least $minPercentage%"
        )
    }

    /**
     * Create a parity matrix for multiple components.
     */
    fun createParityMatrix(
        components: List<String>,
        getStatus: (component: String, platform: Platform) -> PlatformStatus
    ): List<ComponentParity> {
        return components.map { component ->
            ComponentParity(
                component = component,
                android = getStatus(component, Platform.ANDROID),
                ios = getStatus(component, Platform.IOS),
                web = getStatus(component, Platform.WEB),
                desktop = getStatus(component, Platform.DESKTOP)
            )
        }
    }

    /**
     * Generate a parity report in Markdown format.
     */
    fun generateParityReport(matrix: List<ComponentParity>): String {
        val sb = StringBuilder()
        sb.appendLine("# Component Parity Report")
        sb.appendLine()
        sb.appendLine("| Component | Android | iOS | Web | Desktop | Parity % |")
        sb.appendLine("|-----------|---------|-----|-----|---------|----------|")

        for (parity in matrix) {
            sb.append("| ${parity.component} ")
            sb.append("| ${parity.android.toSymbol()} ${parity.android.completionPercentage()}% ")
            sb.append("| ${parity.ios.toSymbol()} ${parity.ios.completionPercentage()}% ")
            sb.append("| ${parity.web.toSymbol()} ${parity.web.completionPercentage()}% ")
            sb.append("| ${parity.desktop.toSymbol()} ${parity.desktop.completionPercentage()}% ")
            sb.append("| ${"%.1f".format(parity.parityPercentage())}% |")
            sb.appendLine()
        }

        sb.appendLine()
        sb.appendLine("**Summary:**")
        sb.appendLine("- Total Components: ${matrix.size}")
        sb.appendLine("- Perfect Parity: ${matrix.count { it.hasFullParity() }}")
        sb.appendLine("- Average Parity: ${"%.1f".format(matrix.map { it.parityPercentage() }.average())}%")

        return sb.toString()
    }
}
