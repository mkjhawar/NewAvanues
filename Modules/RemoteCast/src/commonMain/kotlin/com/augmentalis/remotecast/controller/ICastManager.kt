package com.augmentalis.remotecast.controller

import com.augmentalis.remotecast.model.CastDevice
import com.augmentalis.remotecast.model.CastResolution
import com.augmentalis.remotecast.model.CastState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller interface for screen casting operations.
 * Manages device discovery, connection, and streaming lifecycle.
 *
 * Architecture: Defined in commonMain, implemented per-platform.
 * Android uses MediaProjection + VirtualDisplay; Desktop uses java.awt.Robot.
 * Transport: MJPEG-over-TCP for simplicity (no codec complexity).
 */
interface ICastManager {

    /** Observable cast state. */
    val state: StateFlow<CastState>

    /** Discover available cast devices on the local network. */
    fun discoverDevices(): Flow<List<CastDevice>>

    /** Connect to a specific cast device. */
    suspend fun connectToDevice(device: CastDevice): Boolean

    /** Disconnect from the current device. */
    suspend fun disconnect()

    /** Start screen capture and streaming. Requires prior connection. */
    suspend fun startCasting(): Boolean

    /** Stop casting and release capture resources. */
    suspend fun stopCasting()

    /** Change streaming resolution/quality. */
    fun setQuality(resolution: CastResolution)

    /** Release all resources. */
    fun release()
}
