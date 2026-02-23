/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.identity

import platform.Foundation.NSBundle

/**
 * iOS implementation of PlatformIdentity.
 * Uses Bundle.main.bundleIdentifier as the app identity.
 */
actual object PlatformIdentity {

    actual fun getAppIdentifier(): String {
        return NSBundle.mainBundle.bundleIdentifier ?: "ios.unknown"
    }

    actual fun getIdentityType(): String = "ios_bundle"
}
