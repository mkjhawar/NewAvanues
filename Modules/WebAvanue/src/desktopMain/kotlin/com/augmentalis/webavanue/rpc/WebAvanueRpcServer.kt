/**
 * WebAvanueRpcServer.kt - Desktop actual implementation
 *
 * Stub implementation for desktop. RPC server not yet implemented for desktop.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.webavanue.rpc

/**
 * Desktop RPC server stub implementation
 *
 * TODO: Implement desktop RPC server when needed
 */
actual class WebAvanueRpcServer actual constructor(
    private val delegate: IWebAvanueServiceDelegate,
    private val config: WebAvanueServerConfig
) {
    private var running = false

    actual fun start() {
        // Desktop RPC server not yet implemented
        running = true
    }

    actual fun stop() {
        running = false
    }

    actual fun isRunning(): Boolean = running

    actual fun getPort(): Int = config.port
}
