/**
 * PlatformClientFactory.kt - Desktop (JVM) implementation of platform factory
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * JVM-specific factory utilities for UniversalRPC clients.
 */
package com.augmentalis.universalrpc.client

/**
 * JVM/Desktop implementation of PlatformClientFactory.
 */
actual object PlatformClientFactory {

    /**
     * Get the default protocol for Desktop/JVM.
     * Returns gRPC as default for cross-network support.
     */
    actual fun getDefaultProtocol(): ClientConfig.Protocol {
        return ClientConfig.Protocol.GRPC
    }

    /**
     * Check if UDS (Unix Domain Socket) is supported.
     * True on Unix-like systems (Linux, macOS), false on Windows (pre-JDK16).
     */
    actual fun isUDSSupported(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return os.contains("linux") || os.contains("mac") || os.contains("unix")
    }

    /**
     * Get Desktop-specific default configuration.
     */
    actual fun getDefaultConfig(): ClientConfig {
        return ClientConfig(
            protocol = if (isUDSSupported()) ClientConfig.Protocol.UDS else ClientConfig.Protocol.GRPC,
            autoReconnect = true,
            maxRetryAttempts = 3
        )
    }

    /**
     * Get the platform name for logging/debugging.
     */
    actual fun getPlatformName(): String = "Desktop (JVM)"
}
