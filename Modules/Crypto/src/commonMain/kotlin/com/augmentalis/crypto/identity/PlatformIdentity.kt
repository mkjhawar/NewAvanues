/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.identity

/**
 * Platform-specific application identity for AON package whitelisting.
 *
 * Each platform provides its own identity mechanism:
 * - Android: context.packageName (e.g., "com.augmentalis.ava")
 * - iOS: Bundle.main.bundleIdentifier (e.g., "com.augmentalis.ava")
 * - Desktop JVM: System property or process name
 * - JS Browser: window.location.origin (e.g., "https://app.avanues.com")
 * - JS Node.js: process.env.AON_APP_ID or process.title
 *
 * The identity is MD5-hashed and compared against the allowedPackages
 * in the AON header for authorization.
 */
expect object PlatformIdentity {

    /**
     * Get the application identifier for this platform.
     * Returns a string suitable for MD5 hashing and comparison
     * against AON header allowedPackages.
     */
    fun getAppIdentifier(): String

    /**
     * Get the identity type label for logging/diagnostics.
     * Examples: "android_package", "ios_bundle", "js_origin", "jvm_property"
     */
    fun getIdentityType(): String
}
