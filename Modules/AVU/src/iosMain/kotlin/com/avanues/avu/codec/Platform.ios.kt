package com.avanues.avu.codec

/**
 * iOS-specific IPC utilities.
 *
 * iOS uses URL schemes and App Groups for IPC.
 */
object IOSIPCUtils {
    /**
     * Default URL scheme for iOS IPC.
     */
    const val DEFAULT_URL_SCHEME = "augmentalis-ipc"
}
