/**
 * VoiceOSRpcServer.kt - Desktop stub implementation
 *
 * Placeholder for desktop gRPC server. Full implementation TBD.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore.rpc

/**
 * Desktop RPC server (stub)
 */
actual class VoiceOSRpcServer actual constructor(
    private val delegate: IVoiceOSServiceDelegate,
    private val config: VoiceOSServerConfig
) {
    private var running = false

    actual fun start() {
        running = true
        println("VoiceOS RPC Server (desktop stub) started on port ${config.port}")
    }

    actual fun stop() {
        running = false
        println("VoiceOS RPC Server (desktop stub) stopped")
    }

    actual fun isRunning(): Boolean = running
}
