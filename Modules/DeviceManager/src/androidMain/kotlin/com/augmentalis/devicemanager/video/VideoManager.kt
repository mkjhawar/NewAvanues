// Author: Manoj Jhawar
// Purpose: Comprehensive video and camera management for VOS4

package com.augmentalis.devicemanager.video

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Video Manager Component
 * Handles camera management, video capture, recording, and processing
 * Supports multiple cameras, external cameras, and advanced features
 */
class VideoManager(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoManager"
        private const val VIDEO_BIT_RATE = 10_000_000 // 10 Mbps
        private const val VIDEO_FRAME_RATE = 30
        private const val AUDIO_BIT_RATE = 128_000 // 128 Kbps
        private const val AUDIO_SAMPLE_RATE = 44100
    }
    
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var currentCameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaCodec: MediaCodec? = null
    
    // Background thread for camera operations
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    // State management
    private val _cameraState = MutableStateFlow(CameraState.CLOSED)
    val cameraState: StateFlow<CameraState> = _cameraState
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState
    
    private val _availableCameras = MutableStateFlow<List<CameraDetails>>(emptyList())
    val availableCameras: StateFlow<List<CameraDetails>> = _availableCameras
    
    private val _videoProfile = MutableStateFlow(VideoProfile.HD_1080P)
    val videoProfile: StateFlow<VideoProfile> = _videoProfile
    
    init {
        discoverCameras()
    }
    
    /**
     * Discover all available cameras
     */
    fun discoverCameras() {
        val cameras = mutableListOf<CameraDetails>()
        
        try {
            cameraManager.cameraIdList.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                
                cameras.add(
                    CameraDetails(
                        id = cameraId,
                        facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                            CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
                            CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
                            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
                            else -> CameraFacing.UNKNOWN
                        },
                        isExternal = characteristics.get(CameraCharacteristics.LENS_FACING) == 
                                   CameraCharacteristics.LENS_FACING_EXTERNAL,
                        supportedResolutions = getSupportedResolutions(characteristics),
                        supportedFrameRates = getSupportedFrameRates(characteristics),
                        hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false,
                        hasAutoFocus = hasAutoFocus(characteristics),
                        hasOIS = hasOpticalStabilization(characteristics),
                        maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f,
                        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0,
                        hardwareLevel = getHardwareLevel(characteristics),
                        capabilities = getCameraCapabilities(characteristics)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering cameras", e)
        }
        
        _availableCameras.value = cameras
    }
    
    /**
     * Open camera for use
     */
    fun openCamera(cameraId: String, surface: Surface? = null) {
        startBackgroundThread()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                context.checkSelfPermission(android.Manifest.permission.CAMERA) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted")
                return
            }
            
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    currentCameraDevice = camera
                    _cameraState.value = CameraState.OPENED
                    Log.d(TAG, "Camera opened: $cameraId")
                    
                    surface?.let { createCaptureSession(it) }
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    currentCameraDevice = null
                    _cameraState.value = CameraState.CLOSED
                    Log.d(TAG, "Camera disconnected: $cameraId")
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    currentCameraDevice = null
                    _cameraState.value = CameraState.ERROR
                    Log.e(TAG, "Camera error: $error")
                }
            }, backgroundHandler)
            
            _cameraState.value = CameraState.OPENING
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception", e)
            _cameraState.value = CameraState.ERROR
        }
    }
    
    /**
     * Create capture session for preview or recording
     */
    private fun createCaptureSession(surface: Surface) {
        val camera = currentCameraDevice ?: return
        
        try {
            @Suppress("DEPRECATION")
            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        _cameraState.value = CameraState.PREVIEW
                        startPreview(surface)
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                        _cameraState.value = CameraState.ERROR
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create capture session", e)
        }
    }
    
    /**
     * Start camera preview
     */
    private fun startPreview(surface: Surface) {
        val camera = currentCameraDevice ?: return
        val session = captureSession ?: return
        
        try {
            val previewRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            }.build()
            
            session.setRepeatingRequest(previewRequest, null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to start preview", e)
        }
    }
    
    /**
     * Start video recording
     */
    fun startRecording(outputFile: File, profile: VideoProfile = VideoProfile.HD_1080P) {
        if (_recordingState.value != RecordingState.IDLE) {
            Log.w(TAG, "Already recording")
            return
        }
        
        _videoProfile.value = profile
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile.absolutePath)
            setVideoEncodingBitRate(profile.bitRate)
            setVideoFrameRate(profile.frameRate)
            setVideoSize(profile.width, profile.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(AUDIO_BIT_RATE)
            setAudioSamplingRate(AUDIO_SAMPLE_RATE)
            
            // Set orientation hint based on device rotation
            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                    .defaultDisplay.rotation
            }
            setOrientationHint(getOrientationHint(rotation))
            
            try {
                prepare()
                start()
                _recordingState.value = RecordingState.RECORDING
                Log.d(TAG, "Recording started: ${outputFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                _recordingState.value = RecordingState.ERROR
            }
        }
    }
    
    /**
     * Stop video recording
     */
    fun stopRecording() {
        if (_recordingState.value != RecordingState.RECORDING) {
            return
        }
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _recordingState.value = RecordingState.IDLE
            Log.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            _recordingState.value = RecordingState.ERROR
        }
    }
    
    /**
     * Take a photo
     */
    fun takePhoto(callback: (ByteArray?) -> Unit) {
        val camera = currentCameraDevice ?: return
        
        val reader = ImageReader.newInstance(
            1920, 1080,
            ImageFormat.JPEG,
            1
        )
        
        reader.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireLatestImage()
            val buffer = image?.planes?.get(0)?.buffer
            val bytes = buffer?.let { buf ->
                ByteArray(buf.remaining()).also { buf.get(it) }
            }
            image?.close()
            callback(bytes)
        }, backgroundHandler)
        
        try {
            val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation())
            }.build()
            
            captureSession?.capture(captureRequest, null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to take photo", e)
            callback(null)
        }
    }
    
    /**
     * Switch between cameras
     */
    fun switchCamera() {
        val cameras = _availableCameras.value
        if (cameras.size < 2) return
        
        val currentCamera = currentCameraDevice?.id
        val nextCamera = cameras.firstOrNull { it.id != currentCamera }
        
        nextCamera?.let {
            closeCamera()
            openCamera(it.id)
        }
    }
    
    /**
     * Set zoom level
     */
    fun setZoom(zoomLevel: Float) {
        val camera = currentCameraDevice ?: return
        val session = captureSession ?: return
        
        try {
            val characteristics = cameraManager.getCameraCharacteristics(camera.id)
            val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
            val clampedZoom = zoomLevel.coerceIn(1f, maxZoom)
            
            val sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            if (sensorRect != null) {
                val centerX = sensorRect.width() / 2
                val centerY = sensorRect.height() / 2
                val deltaX = (sensorRect.width() / (2 * clampedZoom)).toInt()
                val deltaY = (sensorRect.height() / (2 * clampedZoom)).toInt()
                
                val zoomRect = android.graphics.Rect(
                    centerX - deltaX,
                    centerY - deltaY,
                    centerX + deltaX,
                    centerY + deltaY
                )
                
                val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
                }.build()
                
                session.setRepeatingRequest(request, null, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to set zoom", e)
        }
    }
    
    /**
     * Enable/disable flash
     */
    fun setFlashMode(mode: FlashMode) {
        val camera = currentCameraDevice ?: return
        val session = captureSession ?: return
        
        try {
            val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                when (mode) {
                    FlashMode.OFF -> {
                        set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                        set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
                    }
                    FlashMode.ON -> {
                        set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                    }
                    FlashMode.AUTO -> {
                        set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH)
                    }
                    FlashMode.TORCH -> {
                        set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                        set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
                    }
                }
            }.build()
            
            session.setRepeatingRequest(request, null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to set flash mode", e)
        }
    }
    
    /**
     * Apply video filter/effect
     */
    fun applyVideoFilter(filter: VideoFilter) {
        // This would typically involve using a SurfaceTexture with OpenGL ES
        // or MediaCodec with a Surface for real-time video processing
        Log.d(TAG, "Applying video filter: $filter")
    }
    
    /**
     * Configure for voice-first video capture
     */
    fun configureForVoiceFirst() {
        // Optimize for hands-free operation
        // Enable auto-focus, auto-exposure, stabilization
        val camera = currentCameraDevice ?: return
        val session = captureSession ?: return
        
        try {
            val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO)
                set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)
            }.build()
            
            session.setRepeatingRequest(request, null, backgroundHandler)
            Log.d(TAG, "Configured for voice-first video capture")
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to configure for voice-first", e)
        }
    }
    
    // Helper methods
    
    private fun getSupportedResolutions(characteristics: CameraCharacteristics): List<Size> {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map?.getOutputSizes(ImageFormat.JPEG)?.toList() ?: emptyList()
    }
    
    private fun getSupportedFrameRates(characteristics: CameraCharacteristics): List<Int> {
        val ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        return ranges?.map { it.upper }?.distinct() ?: emptyList()
    }
    
    private fun hasAutoFocus(characteristics: CameraCharacteristics): Boolean {
        val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        return afModes?.contains(CameraMetadata.CONTROL_AF_MODE_AUTO) ?: false
    }
    
    private fun hasOpticalStabilization(characteristics: CameraCharacteristics): Boolean {
        val oisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        return oisModes?.isNotEmpty() ?: false
    }
    
    private fun getHardwareLevel(characteristics: CameraCharacteristics): HardwareLevel {
        return when (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> HardwareLevel.LEGACY
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> HardwareLevel.LIMITED
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> HardwareLevel.FULL
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> HardwareLevel.LEVEL_3
            else -> HardwareLevel.UNKNOWN
        }
    }
    
    private fun getCameraCapabilities(characteristics: CameraCharacteristics): CameraCapabilities {
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        return CameraCapabilities(
            hasManualControl = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR),
            hasRaw = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
            hasDepth = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT),
            hasHighSpeedVideo = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO),
            hasLogicalMultiCamera = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
            } else false
        )
    }
    
    private fun getOrientationHint(rotation: Int): Int {
        return when (rotation) {
            Surface.ROTATION_0 -> 90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> 90
        }
    }
    
    private fun getJpegOrientation(): Int {
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                .defaultDisplay.rotation
        }
        return getOrientationHint(rotation)
    }
    
    private fun startBackgroundThread() {
        val thread = HandlerThread("CameraBackground").also { it.start() }
        backgroundThread = thread
        backgroundHandler = Handler(thread.looper)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Background thread interrupted", e)
        }
    }
    
    /**
     * Close camera and release resources
     */
    fun closeCamera() {
        captureSession?.close()
        captureSession = null
        
        currentCameraDevice?.close()
        currentCameraDevice = null
        
        mediaRecorder?.release()
        mediaRecorder = null
        
        stopBackgroundThread()
        
        _cameraState.value = CameraState.CLOSED
    }
    
    /**
     * Release all resources
     */
    fun release() {
        closeCamera()
    }
}

// Data classes and enums

enum class CameraFacing {
    FRONT,
    BACK,
    EXTERNAL,
    UNKNOWN
}

data class CameraDetails(
    val id: String,
    val facing: CameraFacing,
    val isExternal: Boolean,
    val supportedResolutions: List<Size>,
    val supportedFrameRates: List<Int>,
    val hasFlash: Boolean,
    val hasAutoFocus: Boolean,
    val hasOIS: Boolean,
    val maxDigitalZoom: Float,
    val sensorOrientation: Int,
    val hardwareLevel: HardwareLevel,
    val capabilities: CameraCapabilities
)

data class CameraCapabilities(
    val hasManualControl: Boolean,
    val hasRaw: Boolean,
    val hasDepth: Boolean,
    val hasHighSpeedVideo: Boolean,
    val hasLogicalMultiCamera: Boolean
)

enum class CameraState {
    CLOSED,
    OPENING,
    OPENED,
    PREVIEW,
    RECORDING,
    ERROR
}

enum class RecordingState {
    IDLE,
    PREPARING,
    RECORDING,
    PAUSED,
    ERROR
}

enum class HardwareLevel {
    LEGACY,
    LIMITED,
    FULL,
    LEVEL_3,
    UNKNOWN
}

enum class FlashMode {
    OFF,
    ON,
    AUTO,
    TORCH
}

enum class VideoProfile(val width: Int, val height: Int, val bitRate: Int, val frameRate: Int) {
    HD_720P(1280, 720, 5_000_000, 30),
    HD_1080P(1920, 1080, 10_000_000, 30),
    UHD_4K(3840, 2160, 35_000_000, 30),
    HD_720P_60FPS(1280, 720, 8_000_000, 60),
    HD_1080P_60FPS(1920, 1080, 15_000_000, 60),
    SLOW_MOTION_720P(1280, 720, 10_000_000, 120),
    SLOW_MOTION_1080P(1920, 1080, 20_000_000, 120)
}

enum class VideoFilter {
    NONE,
    GRAYSCALE,
    SEPIA,
    NEGATIVE,
    BLUR,
    SHARPEN,
    EDGE_DETECTION,
    CARTOON,
    VIGNETTE
}
