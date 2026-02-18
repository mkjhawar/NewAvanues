/**
 * ScreenCaptureHelper.kt — MediaProjection-based screen capture
 *
 * Uses a VirtualDisplay + ImageReader pipeline to continuously capture screen
 * frames, compress them as JPEG, and emit the bytes as a Flow.
 *
 * Lifecycle:
 *   1. Construct with a live MediaProjection and the desired capture dimensions.
 *   2. Call [startCapture] to begin emitting JPEG frames.
 *   3. Call [stopCapture] to tear down the VirtualDisplay and ImageReader.
 *
 * The [startCapture] flow is hot in the sense that it drives a background
 * acquisition loop; it completes when [stopCapture] is called or the scope
 * that collects it is cancelled.
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.remotecast.service

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.ByteArrayOutputStream

/**
 * Captures the device screen via [MediaProjection] and emits JPEG frame bytes.
 *
 * @param mediaProjection  Active MediaProjection obtained from the user consent result.
 * @param width            Capture width in pixels (should match or be less than screen width).
 * @param height           Capture height in pixels.
 * @param density          Screen density (from DisplayMetrics.densityDpi).
 */
class ScreenCaptureHelper(
    private val mediaProjection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val density: Int
) {
    @Volatile private var virtualDisplay: VirtualDisplay? = null
    @Volatile private var imageReader: ImageReader? = null

    /**
     * Starts capturing and returns a [Flow] of JPEG-compressed frame bytes.
     *
     * The flow runs on [Dispatchers.IO] and throttles to [fps] frames per second.
     * Calling [stopCapture] or cancelling the collector's scope terminates the flow.
     *
     * @param fps         Target frames per second (1–60, default 15).
     * @param jpegQuality JPEG compression quality (1–100, default 60).
     */
    fun startCapture(fps: Int = 15, jpegQuality: Int = 60): Flow<ByteArray> = callbackFlow {
        val clampedFps = fps.coerceIn(1, 60)
        val frameIntervalMs = 1000L / clampedFps
        val clampedQuality = jpegQuality.coerceIn(1, 100)

        // ImageReader stores at most 2 acquired images at a time; old frames are dropped.
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        val display = mediaProjection.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null
        )
        virtualDisplay = display

        try {
            while (isActive) {
                val frameStart = System.currentTimeMillis()

                val image = reader.acquireLatestImage()
                if (image != null) {
                    try {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        // Create Bitmap from the RGBA pixel buffer
                        val bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)

                        // Crop to exact dimensions if padding was added
                        val croppedBitmap = if (bitmap.width != width) {
                            Bitmap.createBitmap(bitmap, 0, 0, width, height).also { bitmap.recycle() }
                        } else {
                            bitmap
                        }

                        // Compress to JPEG
                        val out = ByteArrayOutputStream(estimatedJpegSize(width, height, clampedQuality))
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, clampedQuality, out)
                        croppedBitmap.recycle()

                        trySend(out.toByteArray())
                    } finally {
                        image.close()
                    }
                }

                // Throttle to target fps
                val elapsed = System.currentTimeMillis() - frameStart
                val remaining = frameIntervalMs - elapsed
                if (remaining > 0) delay(remaining)
            }
        } finally {
            tearDown()
        }

        awaitClose { tearDown() }
    }.flowOn(Dispatchers.IO)

    /**
     * Stops capture and releases all resources (VirtualDisplay + ImageReader).
     * Safe to call multiple times.
     */
    fun stopCapture() {
        tearDown()
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun tearDown() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        virtualDisplay = null
        imageReader = null
    }

    /**
     * Rough estimate of JPEG output size to pre-allocate the output stream.
     * Better than the default 32 bytes for large frames.
     */
    private fun estimatedJpegSize(w: Int, h: Int, quality: Int): Int {
        // Empirical estimate: raw pixels * rough compression ratio
        val rawBytes = w * h * 4L
        val ratio = 1.0 - (quality / 200.0)  // quality=60 → ratio ≈ 0.7 → ~30% of raw
        return (rawBytes * ratio).toInt().coerceAtLeast(8192)
    }

    companion object {
        private const val VIRTUAL_DISPLAY_NAME = "RemoteCast"
    }
}
