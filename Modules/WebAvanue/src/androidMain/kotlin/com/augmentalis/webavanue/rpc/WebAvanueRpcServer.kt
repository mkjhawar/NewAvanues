/**
 * WebAvanueRpcServer.kt - Android actual implementation
 *
 * Wraps WebAvanueJsonRpcServer to fulfill the expect/actual contract.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.webavanue.rpc

/**
 * Android RPC server implementation using JSON-RPC
 */
actual class WebAvanueRpcServer actual constructor(
    delegate: IWebAvanueServiceDelegate,
    config: WebAvanueServerConfig
) {
    private val jsonRpcServer = WebAvanueJsonRpcServer(delegate, config)

    actual fun start() {
        jsonRpcServer.start()
    }

    actual fun stop() {
        jsonRpcServer.stop()
    }

    actual fun isRunning(): Boolean = jsonRpcServer.isRunning()

    actual fun getPort(): Int = jsonRpcServer.getPort()
}
