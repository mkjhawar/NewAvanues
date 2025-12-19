package com.augmentalis.magicelements.core.mel

/**
 * JVM platform detection implementation
 *
 * Detects the underlying operating system to determine the correct platform
 * and tier enforcement. macOS is treated as an Apple platform (Tier 1 only)
 * due to App Store compliance, while Windows and Linux support Tier 2 (LOGIC).
 *
 * Platform Detection:
 * - macOS: Detected via "os.name" system property containing "mac" -> Platform.MACOS (Tier 1)
 * - Windows: Detected via "os.name" containing "windows" -> Platform.WINDOWS (Tier 2)
 * - Linux: Detected via "os.name" containing "linux" -> Platform.LINUX (Tier 2)
 * - Other JVM: Falls back to Platform.JVM (Tier 2)
 *
 * Tier Enforcement:
 * - macOS (Apple): Restricted to Tier 1 (DATA) for App Store compliance
 * - Windows/Linux: Full Tier 2 (LOGIC) support with scripts and extended APIs
 *
 * @since 2.0.0
 */
actual fun getCurrentPlatform(): Platform {
    val osName = System.getProperty("os.name")?.lowercase() ?: "unknown"

    return when {
        osName.contains("mac") -> Platform.MACOS      // Apple platform: Tier 1 only
        osName.contains("windows") -> Platform.WINDOWS // Non-Apple: Tier 2 supported
        osName.contains("linux") -> Platform.LINUX     // Non-Apple: Tier 2 supported
        else -> Platform.JVM                           // Generic JVM: Tier 2 supported
    }
}
