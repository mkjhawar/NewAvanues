package com.augmentalis.photoavanue

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.augmentalis.photoavanue.model.AspectRatioMode
import com.augmentalis.photoavanue.model.CameraLens
import com.augmentalis.photoavanue.model.CameraState
import com.augmentalis.photoavanue.model.CaptureMode
import com.augmentalis.photoavanue.model.ExposureState
import com.augmentalis.photoavanue.model.FlashMode
import com.augmentalis.photoavanue.model.RecordingState
import com.augmentalis.photoavanue.model.ZoomState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Android CameraX implementation of [ICameraController].
 *
 * Ported from Avenue-Redux CameraViewContainer, converted from imperative FrameLayout
 * to reactive StateFlow-driven controller. Supports photo capture with GPS EXIF,
 * video recording with pause/resume, 5-level zoom and exposure, flash modes, and lens switching.
 */
class AndroidCameraController(
    private val context: Context
) : ICameraController {

    private val _state = MutableStateFlow(CameraState())
    override val state: StateFlow<CameraState> = _state.asStateFlow()

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null

    val locationProvider = AndroidLocationProvider(context)
    private val mainExecutor by lazy { ContextCompat.getMainExecutor(context) }

    // ── Lifecycle ────────────────────────────────────────────────────

    /**
     * Bind camera to a [PreviewView] and [LifecycleOwner].
     * Must be called before any capture operations.
     */
    fun bindCamera(owner: LifecycleOwner, preview: PreviewView) {
        this.lifecycleOwner = owner
        this.previewView = preview

        val hasGps = locationProvider.start()
        _state.value = _state.value.copy(hasGpsLocation = hasGps)

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                this.cameraProvider = provider
                rebindCamera(provider, owner, preview)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Camera init failed: ${e.message}")
            }
        }, mainExecutor)
    }

    private fun rebindCamera(provider: ProcessCameraProvider, owner: LifecycleOwner, preview: PreviewView) {
        val currentState = _state.value
        val lensFacing = when (currentState.lens) {
            CameraLens.BACK -> CameraSelector.LENS_FACING_BACK
            CameraLens.FRONT -> CameraSelector.LENS_FACING_FRONT
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val previewUseCase = Preview.Builder().build().also {
            it.setSurfaceProvider(preview.surfaceProvider)
        }

        val screenAspectRatio = computeAspectRatio(preview.width, preview.height)
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetAspectRatio(screenAspectRatio)
            .build()
        imageCapture = capture

        val recorder = Recorder.Builder().build()
        val vidCapture = VideoCapture.withOutput(recorder)
        videoCapture = vidCapture

        provider.unbindAll()
        camera = provider.bindToLifecycle(owner, cameraSelector, previewUseCase, capture, vidCapture)

        // Read zoom and exposure capabilities from the bound camera
        camera?.cameraInfo?.let { info ->
            info.zoomState.value?.let { zs ->
                _state.value = _state.value.copy(
                    zoom = ZoomState(
                        currentRatio = zs.zoomRatio,
                        minRatio = zs.minZoomRatio,
                        maxRatio = zs.maxZoomRatio
                    )
                )
            }
            info.exposureState.let { es ->
                _state.value = _state.value.copy(
                    exposure = ExposureState(
                        currentIndex = es.exposureCompensationIndex,
                        minIndex = es.exposureCompensationRange.lower,
                        maxIndex = es.exposureCompensationRange.upper
                    )
                )
            }
        }

        // Update GPS state
        _state.value = _state.value.copy(
            hasGpsLocation = locationProvider.currentMetadata != null,
            error = null
        )
    }

    private fun computeAspectRatio(width: Int, height: Int): Int {
        if (width == 0 || height == 0) return AspectRatio.RATIO_4_3
        val ratio43 = 4.0 / 3.0
        val ratio169 = 16.0 / 9.0
        val previewRatio = max(width, height).toDouble() / min(width, height)
        return if (abs(previewRatio - ratio43) <= abs(previewRatio - ratio169)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    override fun release() {
        currentRecording?.stop()
        currentRecording = null
        locationProvider.stop()
        cameraProvider?.unbindAll()
        camera = null
    }

    // ── Photo Capture ────────────────────────────────────────────────

    override fun capturePhoto() {
        val capture = imageCapture ?: run {
            _state.value = _state.value.copy(error = "Camera not initialized")
            return
        }

        _state.value = _state.value.copy(isCapturing = true, error = null)

        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "PhotoAvanue_${System.currentTimeMillis()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/PhotoAvanue")
            }
        }

        val metadata = ImageCapture.Metadata().apply {
            locationProvider.currentLocation?.let { location = it }
        }

        val opts = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv
        ).setMetadata(metadata).build()

        capture.takePicture(opts, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                _state.value = _state.value.copy(
                    isCapturing = false,
                    lastCapturedUri = output.savedUri?.toString()
                )
            }
            override fun onError(exception: ImageCaptureException) {
                _state.value = _state.value.copy(
                    isCapturing = false,
                    error = "Capture failed: ${exception.message}"
                )
            }
        })
    }

    // ── Video Recording ──────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override fun startRecording() {
        val vidCapture = videoCapture ?: run {
            _state.value = _state.value.copy(error = "Video capture not initialized")
            return
        }

        val outputDir = context.cacheDir
        val outputFile = File(outputDir, "PhotoAvanue_${System.currentTimeMillis()}.mp4")
        val fileOptions = FileOutputOptions.Builder(outputFile).build()

        _state.value = _state.value.copy(
            recording = RecordingState(isRecording = true),
            error = null
        )

        currentRecording = vidCapture.output
            .prepareRecording(context, fileOptions)
            .apply { withAudioEnabled() }
            .start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        val uri = event.outputResults.outputUri.toString()
                        _state.value = _state.value.copy(
                            recording = RecordingState(isRecording = false, outputUri = uri),
                            lastCapturedUri = uri
                        )
                        currentRecording = null
                    }
                    is VideoRecordEvent.Status -> {
                        val durationNs = event.recordingStats.recordedDurationNanos
                        val durationMs = durationNs / 1_000_000
                        _state.value = _state.value.copy(
                            recording = _state.value.recording.copy(durationMs = durationMs)
                        )
                    }
                }
            }
    }

    override fun stopRecording() {
        currentRecording?.stop()
    }

    override fun pauseRecording() {
        currentRecording?.pause()
        _state.value = _state.value.copy(
            recording = _state.value.recording.copy(isPaused = true)
        )
    }

    override fun resumeRecording() {
        currentRecording?.resume()
        _state.value = _state.value.copy(
            recording = _state.value.recording.copy(isPaused = false)
        )
    }

    // ── Controls ─────────────────────────────────────────────────────

    override fun switchLens() {
        val newLens = when (_state.value.lens) {
            CameraLens.BACK -> CameraLens.FRONT
            CameraLens.FRONT -> CameraLens.BACK
        }
        _state.value = _state.value.copy(lens = newLens)

        // Rebind camera with new lens
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        val preview = previewView ?: return
        rebindCamera(provider, owner, preview)
    }

    override fun setFlashMode(mode: FlashMode) {
        _state.value = _state.value.copy(flashMode = mode)
        when (mode) {
            FlashMode.OFF -> {
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                camera?.cameraControl?.enableTorch(false)
            }
            FlashMode.ON -> {
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
                camera?.cameraControl?.enableTorch(false)
            }
            FlashMode.AUTO -> {
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
                camera?.cameraControl?.enableTorch(false)
            }
            FlashMode.TORCH -> {
                camera?.cameraControl?.enableTorch(true)
            }
        }
    }

    override fun zoomIn() {
        val zoom = _state.value.zoom
        if (zoom.stepSize <= 0f) return
        val newRatio = (zoom.currentRatio + zoom.stepSize).coerceAtMost(zoom.maxRatio)
        camera?.cameraControl?.setZoomRatio(newRatio)
        _state.value = _state.value.copy(zoom = zoom.copy(currentRatio = newRatio))
    }

    override fun zoomOut() {
        val zoom = _state.value.zoom
        if (zoom.stepSize <= 0f) return
        val newRatio = (zoom.currentRatio - zoom.stepSize).coerceAtLeast(zoom.minRatio)
        camera?.cameraControl?.setZoomRatio(newRatio)
        _state.value = _state.value.copy(zoom = zoom.copy(currentRatio = newRatio))
    }

    override fun setZoomLevel(level: Int) {
        val zoom = _state.value.zoom
        val clamped = level.coerceIn(1, 5)
        val newRatio = (zoom.minRatio + ((clamped - 1) * zoom.stepSize)).coerceIn(zoom.minRatio, zoom.maxRatio)
        camera?.cameraControl?.setZoomRatio(newRatio)
        _state.value = _state.value.copy(zoom = zoom.copy(currentRatio = newRatio))
    }

    override fun increaseExposure() {
        val exp = _state.value.exposure
        if (exp.stepSize <= 0) return
        val newIndex = (exp.currentIndex + exp.stepSize).coerceAtMost(exp.maxIndex)
        camera?.cameraControl?.setExposureCompensationIndex(newIndex)
        _state.value = _state.value.copy(exposure = exp.copy(currentIndex = newIndex))
    }

    override fun decreaseExposure() {
        val exp = _state.value.exposure
        if (exp.stepSize <= 0) return
        val newIndex = (exp.currentIndex - exp.stepSize).coerceAtLeast(exp.minIndex)
        camera?.cameraControl?.setExposureCompensationIndex(newIndex)
        _state.value = _state.value.copy(exposure = exp.copy(currentIndex = newIndex))
    }

    override fun setExposureLevel(level: Int) {
        val exp = _state.value.exposure
        val clamped = level.coerceIn(1, 5)
        val newIndex = (exp.minIndex + ((clamped - 1) * exp.stepSize)).coerceIn(exp.minIndex, exp.maxIndex)
        camera?.cameraControl?.setExposureCompensationIndex(newIndex)
        _state.value = _state.value.copy(exposure = exp.copy(currentIndex = newIndex))
    }
}
