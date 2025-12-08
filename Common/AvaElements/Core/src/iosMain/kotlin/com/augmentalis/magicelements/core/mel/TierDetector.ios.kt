package com.augmentalis.magicelements.core.mel

/**
 * iOS platform detection implementation
 *
 * Returns Platform.IOS to enforce Tier 1 (DATA) mode for Apple App Store compliance.
 * This ensures that all plugins loaded on iOS use only declarative templates with
 * whitelisted functions, preventing arbitrary code execution.
 *
 * @since 2.0.0
 */
actual fun getCurrentPlatform(): Platform {
    return Platform.IOS
}
