package com.augmentalis.remotecast.controller

import com.augmentalis.remotecast.model.CastDevice
import com.augmentalis.remotecast.model.CastResolution
import com.augmentalis.remotecast.model.CastState
import com.augmentalis.remotecast.protocol.CastFrameData
import com.augmentalis.remotecast.transport.CastWebSocketServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

/**
 * Desktop (JVM) implementation of [ICastManager].
 *
 * Screen capture is performed by [java.awt.Robot.createScreenCapture] on a background
 * coroutine that fires at the configured frame rate.  Each captured frame is encoded
 * as a JPEG [ByteArray] at the quality level that corresponds to the active
 * [CastResolution], then wrapped in [CastFrameData] for downstream consumers
 * (network transport, preview surface, etc.).
 *
 * Frame delivery: The captured frames are exposed through [frameFlow], a
 * [MutableStateFlow] that the UI or network layer should observe.  The companion
 * [CastFrameData] data class carries the raw JPEG bytes together with timestamp
 * and resolution metadata.
 *
 * mDNS device discovery is deferred; [discoverDevices] returns [emptyFlow] until a
 * full JmDNS or DNS-SD integration is added.  Manual device entry via [connectToDevice]
 * works today.
 *
 * Threading:
 * - All state mutations run on [Dispatchers.Default] inside a [SupervisorJob] scope.
 * - Screen capture is pinned to [Dispatchers.IO] to avoid blocking the render thread.
 *
 * Author: Manoj Jhawar
 */
class DesktopCastManager : ICastManager {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private val _state = MutableStateFlow(CastState())
    override val state: StateFlow<CastState> = _state.asStateFlow()

    /**
     * Live stream of captured frames.  Null when not casting.
     * Observers should collect this flow to receive JPEG frames.
     */
    val frameFlow = MutableStateFlow<CastFrameData?>(null)

    // -------------------------------------------------------------------------
    // Transport + Coroutine scope
    // -------------------------------------------------------------------------

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val server = CastWebSocketServer(port = DEFAULT_PORT, scope = scope)
    private var captureJob: Job? = null

    // -------------------------------------------------------------------------
    // Device discovery
    // -------------------------------------------------------------------------

    /**
     * Returns [emptyFlow] — mDNS/DNS-SD discovery is deferred.
     * Populate devices manually via the UI or inject them through [connectToDevice].
     */
    override fun discoverDevices(): Flow<List<CastDevice>> = emptyFlow()

    // -------------------------------------------------------------------------
    // Connection lifecycle
    // -------------------------------------------------------------------------

    override suspend fun connectToDevice(device: CastDevice): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                // Future: open a TCP socket to device.address here.
                // For now, update state to reflect a logical connection.
                _state.update { current ->
                    current.copy(
                        deviceId = device.id,
                        deviceName = device.name,
                        isConnected = true,
                        error = null
                    )
                }
                true
            }.getOrElse { ex ->
                _state.update { it.copy(error = "Connection failed: ${ex.message}", isConnected = false) }
                false
            }
        }
    }

    override suspend fun disconnect() {
        stopCasting()
        _state.update {
            CastState() // Reset to defaults
        }
    }

    // -------------------------------------------------------------------------
    // Casting lifecycle
    // -------------------------------------------------------------------------

    override suspend fun startCasting(): Boolean {
        if (_state.value.isStreaming) return true
        if (!_state.value.isConnected) {
            _state.update { it.copy(error = "Not connected to a device") }
            return false
        }
        _state.update { it.copy(isStreaming = true, error = null) }

        // Start WebSocket server for network transport
        server.start()

        captureJob = scope.launch {
            val robot = runCatching { Robot() }.getOrElse { ex ->
                _state.update { it.copy(isStreaming = false, error = "Robot init failed: ${ex.message}") }
                return@launch
            }
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val captureRect = Rectangle(screenSize)
            val targetFps = _state.value.frameRate.coerceIn(1, 60)
            val frameDelayMs = 1000L / targetFps
            var sequenceNumber = 0

            while (isActive) {
                val captureStart = System.currentTimeMillis()
                val resolution = _state.value.resolution

                val rawFrame = withContext(Dispatchers.IO) {
                    runCatching { robot.createScreenCapture(captureRect) }.getOrNull()
                } ?: continue

                val scaledFrame = scaleToResolution(rawFrame, resolution)
                val jpegBytes = withContext(Dispatchers.IO) {
                    encodeJpeg(scaledFrame, jpegQualityFor(resolution))
                }

                val latencyMs = System.currentTimeMillis() - captureStart
                _state.update { it.copy(latencyMs = latencyMs) }

                val frame = CastFrameData(
                    frameBytes = jpegBytes,
                    timestamp = System.currentTimeMillis(),
                    sequenceNumber = sequenceNumber++,
                    width = scaledFrame.width,
                    height = scaledFrame.height
                )
                frameFlow.value = frame
                server.sendFrame(frame)

                val elapsed = System.currentTimeMillis() - captureStart
                val remaining = frameDelayMs - elapsed
                if (remaining > 0) delay(remaining)
            }
        }
        return true
    }

    override suspend fun stopCasting() {
        captureJob?.cancelAndJoin()
        captureJob = null
        frameFlow.value = null
        server.stop()
        _state.update { it.copy(isStreaming = false, latencyMs = 0) }
    }

    // -------------------------------------------------------------------------
    // Quality
    // -------------------------------------------------------------------------

    override fun setQuality(resolution: CastResolution) {
        _state.update { it.copy(resolution = resolution) }
        // If currently streaming the capture loop reads the updated resolution
        // on the next frame iteration — no restart needed.
    }

    // -------------------------------------------------------------------------
    // Release
    // -------------------------------------------------------------------------

    override fun release() {
        captureJob?.cancel()
        captureJob = null
        frameFlow.value = null
        _state.value = CastState()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Scale a [BufferedImage] to fit within the bounds of [resolution] using
     * [java.awt.Image.getScaledInstance] with SCALE_SMOOTH.
     * If the source is already smaller or equal, returns the source unchanged.
     */
    private fun scaleToResolution(source: BufferedImage, resolution: CastResolution): BufferedImage {
        val targetW = resolution.width
        val targetH = resolution.height
        if (source.width <= targetW && source.height <= targetH) return source

        val scaleX = targetW.toDouble() / source.width
        val scaleY = targetH.toDouble() / source.height
        val scale = minOf(scaleX, scaleY)
        val scaledW = (source.width * scale).toInt().coerceAtLeast(1)
        val scaledH = (source.height * scale).toInt().coerceAtLeast(1)

        val awtScaled = source.getScaledInstance(scaledW, scaledH, java.awt.Image.SCALE_SMOOTH)
        val result = BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB)
        val g2d = result.createGraphics()
        try {
            g2d.drawImage(awtScaled, 0, 0, null)
        } finally {
            g2d.dispose()
        }
        return result
    }

    /**
     * Encode a [BufferedImage] to a JPEG [ByteArray] at the given quality [0.0–1.0].
     */
    private fun encodeJpeg(image: BufferedImage, quality: Float): ByteArray {
        val writers = ImageIO.getImageWritersByFormatName("jpeg")
        if (!writers.hasNext()) {
            // Fallback: PNG if no JPEG writer found (rare but defensive)
            val out = ByteArrayOutputStream()
            ImageIO.write(image, "PNG", out)
            return out.toByteArray()
        }
        val writer = writers.next()
        val param: ImageWriteParam = writer.defaultWriteParam.also {
            it.compressionMode = ImageWriteParam.MODE_EXPLICIT
            it.compressionQuality = quality.coerceIn(0.1f, 1.0f)
        }
        val out = ByteArrayOutputStream()
        val memOut = MemoryCacheImageOutputStream(out)
        writer.output = memOut
        try {
            writer.write(null, IIOImage(image, null, null), param)
        } finally {
            writer.dispose()
            memOut.close()
        }
        return out.toByteArray()
    }

    /**
     * Map a [CastResolution] to an appropriate JPEG quality level.
     * Higher resolution → higher quality to preserve fidelity at larger sizes.
     */
    private fun jpegQualityFor(resolution: CastResolution): Float = when (resolution) {
        CastResolution.SD -> 0.65f
        CastResolution.HD -> 0.78f
        CastResolution.FHD -> 0.88f
    }

    companion object {
        const val DEFAULT_PORT = 54321
    }
}
