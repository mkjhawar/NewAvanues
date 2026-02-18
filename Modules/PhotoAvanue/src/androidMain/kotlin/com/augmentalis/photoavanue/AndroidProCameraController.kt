package com.augmentalis.photoavanue

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.provider.MediaStore
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode as CxExtensionMode
import androidx.camera.extensions.ExtensionsManager
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
import com.augmentalis.photoavanue.model.CameraExtensions
import com.augmentalis.photoavanue.model.CameraLens
import com.augmentalis.photoavanue.model.CameraState
import com.augmentalis.photoavanue.model.CaptureMode
import com.augmentalis.photoavanue.model.ExposureState
import com.augmentalis.photoavanue.model.ExtensionMode
import com.augmentalis.photoavanue.model.FlashMode
import com.augmentalis.photoavanue.model.FocusState
import com.augmentalis.photoavanue.model.IsoState
import com.augmentalis.photoavanue.model.ProCameraState
import com.augmentalis.photoavanue.model.RecordingState
import com.augmentalis.photoavanue.model.ShutterSpeedState
import com.augmentalis.photoavanue.model.StabilizationMode
import com.augmentalis.photoavanue.model.WhiteBalanceMode
import com.augmentalis.photoavanue.model.ZoomState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Android implementation of [IProCameraController].
 *
 * Extends basic CameraX with:
 * - CameraX Extensions API (Bokeh/HDR/Night/FaceRetouch)
 * - Camera2 interop for manual ISO, shutter speed, focus distance
 * - White balance presets via Camera2 capture request
 * - RAW capture support detection
 * - Video stabilization modes
 */
class AndroidProCameraController(
    private val context: Context
) : IProCameraController {

    private val _state = MutableStateFlow(CameraState())
    override val state: StateFlow<CameraState> = _state.asStateFlow()

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null

    val locationProvider = AndroidLocationProvider(context)
    private val mainExecutor by lazy { ContextCompat.getMainExecutor(context) }

    // ── Lifecycle ────────────────────────────────────────────────────

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

                // Initialize extensions manager
                val extFuture = ExtensionsManager.getInstanceAsync(context, provider)
                extFuture.addListener({
                    try {
                        extensionsManager = extFuture.get()
                        checkExtensionAvailability()
                        rebindCamera(provider, owner, preview)
                    } catch (e: Exception) {
                        // Extensions not available — proceed without
                        rebindCamera(provider, owner, preview)
                    }
                }, mainExecutor)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Camera init failed: ${e.message}")
            }
        }, mainExecutor)
    }

    private fun checkExtensionAvailability() {
        val mgr = extensionsManager ?: return
        val provider = cameraProvider ?: return
        val currentState = _state.value
        val lensFacing = when (currentState.lens) {
            CameraLens.BACK -> CameraSelector.LENS_FACING_BACK
            CameraLens.FRONT -> CameraSelector.LENS_FACING_FRONT
        }
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        _state.value = _state.value.copy(
            extensions = currentState.extensions.copy(
                bokehAvailable = mgr.isExtensionAvailable(selector, CxExtensionMode.BOKEH),
                hdrAvailable = mgr.isExtensionAvailable(selector, CxExtensionMode.HDR),
                nightAvailable = mgr.isExtensionAvailable(selector, CxExtensionMode.NIGHT),
                faceRetouchAvailable = mgr.isExtensionAvailable(selector, CxExtensionMode.FACE_RETOUCH)
            )
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun rebindCamera(provider: ProcessCameraProvider, owner: LifecycleOwner, preview: PreviewView) {
        val currentState = _state.value
        val lensFacing = when (currentState.lens) {
            CameraLens.BACK -> CameraSelector.LENS_FACING_BACK
            CameraLens.FRONT -> CameraSelector.LENS_FACING_FRONT
        }

        var cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Apply extension if active and available
        val activeExt = currentState.extensions.activeMode
        val mgr = extensionsManager
        if (activeExt != ExtensionMode.NONE && mgr != null) {
            val cxMode = extensionModeToInt(activeExt)
            if (mgr.isExtensionAvailable(cameraSelector, cxMode)) {
                cameraSelector = mgr.getExtensionEnabledCameraSelector(cameraSelector, cxMode)
            }
        }

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

        // Read zoom/exposure capabilities
        camera?.cameraInfo?.let { info ->
            info.zoomState.value?.let { zs ->
                _state.value = _state.value.copy(
                    zoom = ZoomState(zs.zoomRatio, zs.minZoomRatio, zs.maxZoomRatio)
                )
            }
            info.exposureState.let { es ->
                _state.value = _state.value.copy(
                    exposure = ExposureState(
                        es.exposureCompensationIndex,
                        es.exposureCompensationRange.lower,
                        es.exposureCompensationRange.upper
                    )
                )
            }
        }

        // Read pro capabilities via Camera2 interop
        readProCapabilities()

        _state.value = _state.value.copy(
            hasGpsLocation = locationProvider.currentMetadata != null,
            error = null
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun readProCapabilities() {
        val cam = camera ?: return
        val cam2Info = Camera2CameraInfo.from(cam.cameraInfo)
        val characteristics = cam2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposureRange = cam2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val focusRange = cam2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

        val rawCapable = cam2Info.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false

        val currentPro = _state.value.pro
        _state.value = _state.value.copy(
            pro = currentPro.copy(
                iso = IsoState(
                    currentValue = characteristics?.lower ?: 100,
                    minValue = characteristics?.lower ?: 100,
                    maxValue = characteristics?.upper ?: 3200
                ),
                shutterSpeed = ShutterSpeedState(
                    currentNanos = 33_333_333L,
                    minNanos = exposureRange?.lower ?: 1_000_000L,
                    maxNanos = exposureRange?.upper ?: 1_000_000_000L
                ),
                focusDistance = FocusState(
                    currentDiopters = 0f,
                    minDiopters = 0f,
                    maxDiopters = focusRange ?: 10f
                ),
                isRawSupported = rawCapable
            )
        )
    }

    private fun extensionModeToInt(mode: ExtensionMode): Int = when (mode) {
        ExtensionMode.NONE -> CxExtensionMode.NONE
        ExtensionMode.BOKEH -> CxExtensionMode.BOKEH
        ExtensionMode.HDR -> CxExtensionMode.HDR
        ExtensionMode.NIGHT -> CxExtensionMode.NIGHT
        ExtensionMode.FACE_RETOUCH -> CxExtensionMode.FACE_RETOUCH
    }

    @Suppress("DEPRECATION")
    private fun computeAspectRatio(width: Int, height: Int): Int {
        if (width == 0 || height == 0) return androidx.camera.core.AspectRatio.RATIO_4_3
        val ratio43 = 4.0 / 3.0
        val ratio169 = 16.0 / 9.0
        val previewRatio = max(width, height).toDouble() / min(width, height)
        return if (abs(previewRatio - ratio43) <= abs(previewRatio - ratio169)) {
            androidx.camera.core.AspectRatio.RATIO_4_3
        } else {
            androidx.camera.core.AspectRatio.RATIO_16_9
        }
    }

    override fun release() {
        currentRecording?.stop()
        currentRecording = null
        locationProvider.stop()
        cameraProvider?.unbindAll()
        camera = null
    }

    // ── Extensions ────────────────────────────────────────────────────

    override fun setExtensionMode(mode: ExtensionMode) {
        val current = _state.value
        if (current.extensions.activeMode == mode) return
        if (!current.extensions.isAvailable(mode)) return

        // Pro mode and extensions are mutually exclusive
        _state.value = current.copy(
            extensions = current.extensions.copy(activeMode = mode),
            pro = current.pro.copy(isProMode = false)
        )
        rebindIfBound()
    }

    // ── Pro Mode ──────────────────────────────────────────────────────

    override fun setProMode(enabled: Boolean) {
        val current = _state.value
        _state.value = current.copy(
            pro = current.pro.copy(isProMode = enabled),
            extensions = if (enabled) current.extensions.copy(activeMode = ExtensionMode.NONE) else current.extensions
        )
        if (enabled) {
            rebindIfBound()
        } else {
            // Reset Camera2 overrides to auto
            resetCamera2Controls()
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun setIso(value: Int) {
        if (!_state.value.pro.isProMode) return
        val pro = _state.value.pro
        val clamped = value.coerceIn(pro.iso.minValue, pro.iso.maxValue)
        _state.value = _state.value.copy(
            pro = pro.copy(iso = pro.iso.copy(currentValue = clamped, isManual = true))
        )
        applyCamera2Control(CaptureRequest.SENSOR_SENSITIVITY, clamped)
    }

    override fun lockIso(locked: Boolean) {
        val pro = _state.value.pro
        _state.value = _state.value.copy(pro = pro.copy(isIsoLocked = locked))
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun setShutterSpeed(nanos: Long) {
        if (!_state.value.pro.isProMode) return
        val pro = _state.value.pro
        val clamped = nanos.coerceIn(pro.shutterSpeed.minNanos, pro.shutterSpeed.maxNanos)
        _state.value = _state.value.copy(
            pro = pro.copy(shutterSpeed = pro.shutterSpeed.copy(currentNanos = clamped, isManual = true))
        )
        applyCamera2Control(CaptureRequest.SENSOR_EXPOSURE_TIME, clamped)
    }

    override fun lockShutter(locked: Boolean) {
        val pro = _state.value.pro
        _state.value = _state.value.copy(pro = pro.copy(isShutterLocked = locked))
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun setFocusDistance(diopters: Float) {
        if (!_state.value.pro.isProMode) return
        val pro = _state.value.pro
        val clamped = diopters.coerceIn(pro.focusDistance.minDiopters, pro.focusDistance.maxDiopters)
        _state.value = _state.value.copy(
            pro = pro.copy(focusDistance = pro.focusDistance.copy(currentDiopters = clamped, isManual = true))
        )
        applyCamera2Control(CaptureRequest.LENS_FOCUS_DISTANCE, clamped)
        applyCamera2Control(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
    }

    override fun lockFocus(locked: Boolean) {
        val pro = _state.value.pro
        _state.value = _state.value.copy(pro = pro.copy(isFocusLocked = locked))
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun setWhiteBalance(mode: WhiteBalanceMode) {
        val pro = _state.value.pro
        _state.value = _state.value.copy(pro = pro.copy(whiteBalance = mode))

        val awbMode = when (mode) {
            WhiteBalanceMode.AUTO -> CaptureRequest.CONTROL_AWB_MODE_AUTO
            WhiteBalanceMode.DAYLIGHT -> CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
            WhiteBalanceMode.CLOUDY -> CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
            WhiteBalanceMode.TUNGSTEN -> CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
            WhiteBalanceMode.FLUORESCENT -> CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
            WhiteBalanceMode.SHADE -> CaptureRequest.CONTROL_AWB_MODE_SHADE
        }
        applyCamera2Control(CaptureRequest.CONTROL_AWB_MODE, awbMode)
    }

    override fun lockWhiteBalance(locked: Boolean) {
        val pro = _state.value.pro
        _state.value = _state.value.copy(pro = pro.copy(isWhiteBalanceLocked = locked))
    }

    override fun setRawCapture(enabled: Boolean) {
        if (!_state.value.pro.isRawSupported) return
        val pro = _state.value.pro
        _state.value = _state.value.copy(pro = pro.copy(isRawEnabled = enabled))
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun setStabilization(mode: StabilizationMode) {
        val pro = _state.value.pro
        _state.value = _state.value.copy(pro = pro.copy(stabilization = mode))

        val stabilizationMode = when (mode) {
            StabilizationMode.OFF -> CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            StabilizationMode.AUTO -> CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
            StabilizationMode.OPTICAL -> CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
            StabilizationMode.VIDEO -> CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
        }
        if (mode == StabilizationMode.OPTICAL) {
            applyCamera2Control(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, stabilizationMode)
        } else {
            applyCamera2Control(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, stabilizationMode)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun <T : Any> applyCamera2Control(key: CaptureRequest.Key<T>, value: T) {
        val cam = camera ?: return
        val cam2Control = Camera2CameraControl.from(cam.cameraControl)
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(key, value)
            .build()
        cam2Control.captureRequestOptions = options
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun resetCamera2Controls() {
        val cam = camera ?: return
        val cam2Control = Camera2CameraControl.from(cam.cameraControl)
        cam2Control.captureRequestOptions = CaptureRequestOptions.Builder().build()
    }

    private fun rebindIfBound() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        val preview = previewView ?: return
        rebindCamera(provider, owner, preview)
    }

    // ── ICameraController (Base) ──────────────────────────────────────

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
                        val durationMs = event.recordingStats.recordedDurationNanos / 1_000_000
                        _state.value = _state.value.copy(
                            recording = _state.value.recording.copy(durationMs = durationMs)
                        )
                    }
                }
            }
    }

    override fun stopRecording() { currentRecording?.stop() }

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

    override fun switchLens() {
        val newLens = when (_state.value.lens) {
            CameraLens.BACK -> CameraLens.FRONT
            CameraLens.FRONT -> CameraLens.BACK
        }
        _state.value = _state.value.copy(lens = newLens)
        checkExtensionAvailability()
        rebindIfBound()
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
