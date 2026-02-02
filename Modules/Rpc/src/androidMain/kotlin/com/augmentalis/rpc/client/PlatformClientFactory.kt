/**
 * PlatformClientFactory.kt - Android implementation of platform factory
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Android-specific factory utilities for UniversalRPC clients.
 */
package com.augmentalis.rpc.client

/**
 * Android implementation of PlatformClientFactory.
 */
actual object PlatformClientFactory {

    /**
     * Get the default protocol for Android.
     * Returns UDS for same-process/device, gRPC for remote.
     */
    actual fun getDefaultProtocol(): ClientConfig.Protocol {
        return ClientConfig.Protocol.UDS
    }

    /**
     * Check if UDS (Unix Domain Socket) is supported on Android.
     * Always true on Android 5.0+.
     */
    actual fun isUDSSupported(): Boolean = true

    /**
     * Get Android-specific default configuration.
     */
    actual fun getDefaultConfig(): ClientConfig {
        return ClientConfig(
            protocol = ClientConfig.Protocol.UDS,
            autoReconnect = true,
            maxRetryAttempts = 3
        )
    }

    /**
     * Get the platform name for logging/debugging.
     */
    actual fun getPlatformName(): String = "Android"
}
