/**
 * WebAvanueRpcServer.kt - iOS actual implementation
 *
 * Stub implementation for iOS. RPC server not yet implemented for iOS.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.webavanue.rpc

/**
 * iOS RPC server stub implementation
 *
 * TODO: Implement iOS RPC server when needed
 */
actual class WebAvanueRpcServer actual constructor(
    private val delegate: IWebAvanueServiceDelegate,
    private val config: WebAvanueServerConfig
) {
    private var running = false

    actual fun start() {
        // iOS RPC server not yet implemented
        running = true
    }

    actual fun stop() {
        running = false
    }

    actual fun isRunning(): Boolean = running

    actual fun getPort(): Int = config.port
}
