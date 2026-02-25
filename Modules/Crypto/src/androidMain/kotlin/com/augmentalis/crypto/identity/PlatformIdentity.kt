/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.identity

/**
 * Android implementation of PlatformIdentity.
 *
 * Uses the Android package name as identity, which is the standard
 * app identifier for AON package whitelisting.
 *
 * Call [init] with an Android Context at app startup before using AONCodec.
 * If not initialized, falls back to the JVM parent's system property approach.
 */
actual object PlatformIdentity {

    @Volatile
    private var packageName: String? = null

    /**
     * Initialize with Android application context.
     * Call once at app startup (e.g., in Application.onCreate).
     */
    fun init(context: android.content.Context) {
        packageName = context.packageName
    }

    actual fun getAppIdentifier(): String {
        // Priority 1: Android package name (set via init)
        packageName?.let { return it }

        // Priority 2: System property fallback
        System.getProperty("aon.app.id")?.let { return it }

        return "android.unknown"
    }

    actual fun getIdentityType(): String = "android_package"
}
