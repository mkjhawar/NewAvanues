/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.identity

/**
 * JVM Desktop implementation of PlatformIdentity.
 *
 * Identity priority:
 * 1. System property "aon.app.id" (set via -Daon.app.id=com.augmentalis.ava)
 * 2. System property "sun.java.command" (main class name)
 * 3. Fallback: "jvm.unknown"
 *
 * Note: Android overrides this in androidMain with context.packageName.
 */
actual object PlatformIdentity {

    actual fun getAppIdentifier(): String {
        // Priority 1: Explicit system property
        System.getProperty("aon.app.id")?.let { return it }

        // Priority 2: Main class from Java command
        System.getProperty("sun.java.command")?.let { cmd ->
            val mainClass = cmd.split(" ").firstOrNull()
            if (!mainClass.isNullOrBlank()) return mainClass
        }

        return "jvm.unknown"
    }

    actual fun getIdentityType(): String = "jvm_property"
}
