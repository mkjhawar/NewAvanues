/**
 * AndroidCastManager.kt — Android implementation of ICastManager
 *
 * Orchestrates the full cast pipeline:
 *   Sender path:  ScreenCaptureHelper → CastFrameData → MjpegTcpServer → remote client
 *   Receiver path: MjpegTcpClient → CastReceiverView (via receivedFrames flow)
 *
 * The manager owns a [MjpegTcpServer] that starts on [startCasting] and a
 * [MjpegTcpClient] that connects on [connectToDevice].
 *
 * MediaProjection must be injected via [setMediaProjection] before calling
 * [startCasting] — Android requires the user to grant consent via system dialog,
 * which happens outside this class.
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.remotecast.controller

import android.media.projection.MediaProjection
import com.augmentalis.remotecast.model.CastDevice
import com.augmentalis.remotecast.model.CastResolution
import com.augmentalis.remotecast.model.CastState
import com.augmentalis.remotecast.protocol.CastFrameData
import com.augmentalis.remotecast.service.ScreenCaptureHelper
import com.augmentalis.remotecast.transport.MjpegTcpClient
import com.augmentalis.remotecast.transport.MjpegTcpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android cast manager. Created once per session, typically held by a
 * ViewModel or a long-lived service component.
 *
 * @param scope CoroutineScope tied to the owning component's lifecycle.
 */
class AndroidCastManager(
    private val scope: CoroutineScope
) : ICastManager {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _state = MutableStateFlow(CastState())
    override val state: StateFlow<CastState> = _state.asStateFlow()

    // ── Transport components ──────────────────────────────────────────────────

    private val server = MjpegTcpServer(port = DEFAULT_PORT, scope = scope)
    private val client = MjpegTcpClient(scope = scope)

    // ── Capture components ────────────────────────────────────────────────────

    @Volatile private var mediaProjection: MediaProjection? = null
    @Volatile private var captureHelper: ScreenCaptureHelper? = null
    @Volatile private var captureJob: Job? = null
    private val sequenceCounter = AtomicInteger(0)

    // ── Receiver flow (for display by CastReceiverView) ───────────────────────

    /** Exposes incoming frames when this device is acting as the receiver. */
    @Volatile private var _receivedFrames: Flow<ByteArray> = emptyFlow<ByteArray>()
    val receivedFrames: Flow<ByteArray> get() = _receivedFrames

    // ── ICastManager ──────────────────────────────────────────────────────────

    /**
     * Discovers devices on the local network.
     * mDNS-based discovery is deferred to a future iteration; returns an empty list.
     */
    override fun discoverDevices(): Flow<List<CastDevice>> = emptyFlow()

    /**
     * Connects as a receiver to the given [device].
     * Starts the [MjpegTcpClient] and updates [receivedFrames].
     */
    override suspend fun connectToDevice(device: CastDevice): Boolean {
        return try {
            val framesFlow = client.connect(host = device.address, port = DEFAULT_PORT)
            _receivedFrames = framesFlow
            _state.update {
                it.copy(
                    deviceName = device.name,
                    deviceId = device.id,
                    isConnected = true,
                    error = null
                )
            }
            // Observe server-side client connection state
            server.clientConnected
                .onEach { connected -> _state.update { it.copy(isStreaming = connected) } }
                .launchIn(scope)
            true
        } catch (e: Exception) {
            _state.update { it.copy(error = "Connection failed: ${e.message}") }
            false
        }
    }

    /**
     * Disconnects from the remote device and stops receiving frames.
     */
    override suspend fun disconnect() {
        client.disconnect()
        _receivedFrames = emptyFlow()
        _state.update {
            it.copy(
                isConnected = false,
                isStreaming = false,
                latencyMs = 0L,
                error = null
            )
        }
    }

    /**
     * Starts screen capture (sender mode). Requires [setMediaProjection] to have
     * been called with a valid projection, and the server to already be running
     * (use [startServer] from the owning service before calling this).
     *
     * @return true if capture started successfully, false if MediaProjection is missing.
     */
    override suspend fun startCasting(): Boolean {
        val projection = mediaProjection ?: run {
            _state.update { it.copy(error = "MediaProjection not set — call setMediaProjection first") }
            return false
        }
        val resolution = _state.value.resolution
        val fps = _state.value.frameRate

        // Start the TCP server first (idempotent)
        server.start()

        val helper = ScreenCaptureHelper(
            mediaProjection = projection,
            width = resolution.width,
            height = resolution.height,
            density = DEFAULT_DENSITY
        )
        captureHelper = helper

        sequenceCounter.set(0)
        captureJob = scope.launch {
            helper.startCapture(fps = fps, jpegQuality = DEFAULT_JPEG_QUALITY)
                .collect { jpegBytes ->
                    val frame = CastFrameData(
                        frameBytes = jpegBytes,
                        timestamp = System.currentTimeMillis(),
                        sequenceNumber = sequenceCounter.getAndIncrement(),
                        width = resolution.width,
                        height = resolution.height
                    )
                    server.sendFrame(frame)
                }
        }

        _state.update { it.copy(isStreaming = true, error = null) }
        return true
    }

    /**
     * Stops capture and closes the server. Does not reset connection state.
     */
    override suspend fun stopCasting() {
        captureJob?.cancel()
        captureJob = null
        captureHelper?.stopCapture()
        captureHelper = null
        server.stop()
        _state.update { it.copy(isStreaming = false) }
    }

    /**
     * Updates the capture resolution for the next [startCasting] call.
     * If currently streaming, the change takes effect on the next session.
     */
    override fun setQuality(resolution: CastResolution) {
        _state.update { it.copy(resolution = resolution) }
    }

    /**
     * Releases all resources. Should be called from the owning component's
     * onDestroy / onCleared.
     */
    override fun release() {
        captureJob?.cancel()
        captureHelper?.stopCapture()
        server.stop()
        client.disconnect()
        mediaProjection?.stop()
        mediaProjection = null
        captureHelper = null
        captureJob = null
    }

    // ── Public helpers ────────────────────────────────────────────────────────

    /**
     * Injects the MediaProjection obtained from the system consent dialog.
     * Must be called before [startCasting].
     *
     * @param projection  Active MediaProjection; pass null to revoke.
     * @param screenWidth  Actual device screen width in pixels.
     * @param screenHeight Actual device screen height in pixels.
     * @param screenDensity Device screen density (DisplayMetrics.densityDpi).
     */
    fun setMediaProjection(
        projection: MediaProjection?,
        screenWidth: Int = DEFAULT_SCREEN_WIDTH,
        screenHeight: Int = DEFAULT_SCREEN_HEIGHT,
        screenDensity: Int = DEFAULT_DENSITY
    ) {
        mediaProjection = projection
    }

    /**
     * Starts the TCP server independently (e.g., from a foreground service before
     * the user connects a receiver device).
     */
    suspend fun startServer() {
        server.start()
        _state.update { it.copy(error = null) }
    }

    /**
     * Exposes the server's current client-connected state for UI observation.
     */
    val isClientConnected: StateFlow<Boolean> = server.clientConnected

    companion object {
        const val DEFAULT_PORT = 54321
        const val DEFAULT_JPEG_QUALITY = 60
        const val DEFAULT_DENSITY = 320       // XHDPI — reasonable default
        const val DEFAULT_SCREEN_WIDTH = 1280
        const val DEFAULT_SCREEN_HEIGHT = 720
    }
}
