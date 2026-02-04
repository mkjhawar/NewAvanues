/**
 * PlatformClientFactory.kt - iOS implementation of platform factory
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * iOS-specific factory utilities for UniversalRPC clients.
 */
package com.augmentalis.rpc.client

/**
 * iOS implementation of PlatformClientFactory.
 */
actual object PlatformClientFactory {

    /**
     * Get the default protocol for iOS.
     * Returns UDS for local, gRPC for remote.
     */
    actual fun getDefaultProtocol(): ClientConfig.Protocol {
        return ClientConfig.Protocol.UDS
    }

    /**
     * Check if UDS (Unix Domain Socket) is supported.
     * Always true on iOS.
     */
    actual fun isUDSSupported(): Boolean = true

    /**
     * Get iOS-specific default configuration.
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
    actual fun getPlatformName(): String = "iOS"
}
