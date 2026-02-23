/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.identity

import platform.Foundation.NSBundle

/**
 * Darwin (iOS + macOS) implementation of PlatformIdentity.
 * Uses Bundle.main.bundleIdentifier as the app identity.
 */
actual object PlatformIdentity {

    actual fun getAppIdentifier(): String {
        return NSBundle.mainBundle.bundleIdentifier ?: "darwin.unknown"
    }

    actual fun getIdentityType(): String = "darwin_bundle"
}
