/**
 * VoiceOSRpcServer.kt - iOS stub implementation
 *
 * Placeholder for iOS gRPC server. Full implementation TBD.
 * iOS uses a different RPC mechanism (Network.framework or gRPC-Swift).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore.rpc

import platform.Foundation.NSLog

/**
 * iOS RPC server (stub)
 */
actual class VoiceOSRpcServer actual constructor(
    private val delegate: IVoiceOSServiceDelegate,
    private val config: VoiceOSServerConfig
) {
    private var running = false

    actual fun start() {
        running = true
        NSLog("VoiceOS RPC Server (iOS stub) started on port ${config.port}")
    }

    actual fun stop() {
        running = false
        NSLog("VoiceOS RPC Server (iOS stub) stopped")
    }

    actual fun isRunning(): Boolean = running
}
