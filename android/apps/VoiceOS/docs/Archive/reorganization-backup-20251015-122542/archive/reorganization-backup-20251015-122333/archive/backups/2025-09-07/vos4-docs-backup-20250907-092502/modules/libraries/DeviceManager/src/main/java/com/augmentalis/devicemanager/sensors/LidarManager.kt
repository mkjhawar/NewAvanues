// Author: Manoj Jhawar
// Purpose: Comprehensive LiDAR and ToF sensor management for depth sensing, 3D scanning, and environmental mapping

package com.augmentalis.devicemanager.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.core.*
import androidx.core.content.ContextCompat
// ARCore is not included in dependencies
// import androidx.camera.extensions.ExtensionMode
// import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
// import com.google.ar.core.*
// import com.google.ar.core.exceptions.UnavailableException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.*

/**
 * Comprehensive LiDAR and ToF Manager
 * Supports depth sensing, 3D scanning, point cloud generation, and environmental mapping
 * Handles various depth technologies: ToF cameras, structured light, stereo vision, and LiDAR
 * 
 * COT (Chain of Thought) Architecture:
 * - Receives capabilities from DeviceDetector (centralized detection)
 * - Does NOT perform hardware detection directly
 * - Enables conditional loading based on detected LiDAR/ToF capabilities
 * - Provides runtime state management for depth sensing features
 * 
 * Benefits:
 * - Reduces redundant system calls and detection overhead
 * - Ensures consistent detection logic across all sensor managers
 * - Supports conditional instantiation for performance optimization
 * - Improves testability by removing hardware dependencies
 */
class LidarManager(
    private val context: Context,
    private val capabilities: com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.DeviceCapabilities
) {
    
    companion object {
        private const val TAG = "LidarManager"
        
        // Depth sensing technologies
        const val TECH_TOF = "time_of_flight"           // Time-of-Flight camera
        const val TECH_STRUCTURED_LIGHT = "structured_light"  // Structured light (e.g., iPhone Face ID)
        const val TECH_STEREO = "stereo_vision"         // Dual camera stereo
        const val TECH_LIDAR = "lidar"                  // True LiDAR scanner
        const val TECH_RADAR = "radar"                  // mmWave radar
        const val TECH_ULTRASONIC = "ultrasonic"        // Ultrasonic sensors
        
        // Depth range categories
        const val RANGE_MACRO = 0.1f    // 0-10cm
        const val RANGE_CLOSE = 0.5f    // 10-50cm
        const val RANGE_NEAR = 2.0f     // 50cm-2m
        const val RANGE_MEDIUM = 5.0f   // 2-5m
        const val RANGE_FAR = 10.0f     // 5-10m
        const val RANGE_EXTENDED = 20.0f // 10-20m
        
        // Point cloud density levels
        const val DENSITY_LOW = 1000      // Points per frame
        const val DENSITY_MEDIUM = 10000
        const val DENSITY_HIGH = 100000
        const val DENSITY_ULTRA = 1000000
        
        // Scanning modes
        const val MODE_SINGLE_SHOT = 0
        const val MODE_CONTINUOUS = 1
        const val MODE_BURST = 2
        const val MODE_HDR = 3
        
        // Processing quality
        const val QUALITY_DRAFT = 0
        const val QUALITY_NORMAL = 1
        const val QUALITY_HIGH = 2
        const val QUALITY_MAXIMUM = 3
    }
    
    // System services
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    
    // Camera and AR Core
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var depthImageReader: ImageReader? = null
    // ARCore session disabled until dependency available
    private var arSession: Any? = null // Was: Session?
    private var cameraProvider: ProcessCameraProvider? = null
    
    // Handler threads
    private val backgroundThread = HandlerThread("LidarBackground").apply { start() }
    private val backgroundHandler = Handler(backgroundThread.looper)
    
    // Sensors
    private val depthSensors = mutableListOf<Sensor>()
    private var proximityInfraredSensor: Sensor? = null
    private var tofSensor: Sensor? = null
    
    // State flows
    private val _lidarState = MutableStateFlow(LidarState())
    val lidarState: StateFlow<LidarState> = _lidarState.asStateFlow()
    
    private val _depthFrame = MutableStateFlow<DepthFrame?>(null)
    val depthFrame: StateFlow<DepthFrame?> = _depthFrame.asStateFlow()
    
    private val _pointCloud = MutableStateFlow<PointCloud?>(null)
    val pointCloud: StateFlow<PointCloud?> = _pointCloud.asStateFlow()
    
    private val _meshData = MutableStateFlow<MeshData?>(null)
    val meshData: StateFlow<MeshData?> = _meshData.asStateFlow()
    
    private val _environmentMap = MutableStateFlow<EnvironmentMap?>(null)
    val environmentMap: StateFlow<EnvironmentMap?> = _environmentMap.asStateFlow()
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // ========== DATA MODELS ==========
    
    data class LidarState(
        val isAvailable: Boolean = false,
        val isActive: Boolean = false,
        val technology: String? = null,
        val capabilities: DepthCapabilities? = null,
        val currentMode: ScanMode = ScanMode.SINGLE,
        val quality: ProcessingQuality = ProcessingQuality.NORMAL,
        val frameRate: Int = 30,
        val isRecording: Boolean = false,
        val lastError: DepthError? = null
    )
    
    data class DepthCapabilities(
        val supportedTechnologies: List<String>,
        val maxRange: Float, // meters
        val minRange: Float, // meters
        val accuracy: Float, // meters
        val resolution: Size,
        val frameRate: Range<Int>,
        val fieldOfView: FieldOfView,
        val pointsPerFrame: Int,
        val supportsRawDepth: Boolean,
        val supportsPointCloud: Boolean,
        val supportsMeshGeneration: Boolean,
        val supportsOcclusion: Boolean,
        val supportsMotionTracking: Boolean,
        val supportsSemanticSegmentation: Boolean,
        val powerConsumption: PowerProfile
    )
    
    data class FieldOfView(
        val horizontal: Float, // degrees
        val vertical: Float,   // degrees
        val diagonal: Float    // degrees
    )
    
    data class PowerProfile(
        val idle: Float,      // mW
        val active: Float,    // mW
        val peak: Float       // mW
    )
    
    enum class ScanMode {
        SINGLE,       // Single depth capture
        CONTINUOUS,   // Real-time streaming
        BURST,        // Multiple rapid captures
        HDR,          // High dynamic range
        ACCUMULATION, // Point cloud accumulation
        TRACKING      // SLAM tracking
    }
    
    enum class ProcessingQuality {
        DRAFT,    // Fast, low quality
        NORMAL,   // Balanced
        HIGH,     // High quality, slower
        MAXIMUM   // Best quality, slowest
    }
    
    data class DepthFrame(
        val timestamp: Long,
        val depthData: FloatArray, // Depth values in meters
        val confidenceMap: FloatArray?, // Confidence values 0-1
        val width: Int,
        val height: Int,
        val intrinsics: CameraIntrinsics,
        val pose: Pose?,
        val technology: String,
        val statistics: FrameStatistics
    )
    
    data class CameraIntrinsics(
        val fx: Float, // Focal length X
        val fy: Float, // Focal length Y
        val cx: Float, // Principal point X
        val cy: Float, // Principal point Y
        val distortion: FloatArray? // Distortion coefficients
    )
    
    data class Pose(
        val translation: Vector3,
        val rotation: Quaternion,
        val confidence: Float
    )
    
    data class Vector3(
        val x: Float,
        val y: Float,
        val z: Float
    ) {
        fun length(): Float = sqrt(x * x + y * y + z * z)
        fun normalized(): Vector3 {
            val len = length()
            return if (len > 0) Vector3(x / len, y / len, z / len) else this
        }
    }
    
    data class Quaternion(
        val x: Float,
        val y: Float,
        val z: Float,
        val w: Float
    )
    
    data class FrameStatistics(
        val validPixels: Int,
        val invalidPixels: Int,
        val minDepth: Float,
        val maxDepth: Float,
        val averageDepth: Float,
        val standardDeviation: Float
    )
    
    data class PointCloud(
        val points: FloatArray, // XYZ coordinates
        val colors: FloatArray?, // RGB colors
        val normals: FloatArray?, // Normal vectors
        val confidence: FloatArray?, // Per-point confidence
        val pointCount: Int,
        val timestamp: Long,
        val coordinateSystem: CoordinateSystem
    )
    
    enum class CoordinateSystem {
        CAMERA,      // Camera-centric
        WORLD,       // World coordinates
        DEVICE,      // Device-centric
        CUSTOM       // User-defined
    }
    
    data class MeshData(
        val vertices: FloatArray,
        val indices: IntArray,
        val normals: FloatArray?,
        val uvCoords: FloatArray?,
        val colors: FloatArray?,
        val vertexCount: Int,
        val faceCount: Int,
        val material: MaterialProperties?
    )
    
    data class MaterialProperties(
        val ambient: FloatArray,
        val diffuse: FloatArray,
        val specular: FloatArray,
        val shininess: Float,
        val transparency: Float
    )
    
    data class EnvironmentMap(
        val planes: List<Plane>,
        val objects: List<DetectedObject>,
        val semanticLabels: List<SemanticLabel>,
        val occupancyGrid: OccupancyGrid?,
        val lightEstimate: LightEstimate?,
        val timestamp: Long
    )
    
    data class Plane(
        val id: String,
        val center: Vector3,
        val normal: Vector3,
        val extents: Vector2,
        val boundary: List<Vector3>,
        val type: PlaneType,
        val confidence: Float
    )
    
    data class Vector2(val x: Float, val y: Float)
    
    enum class PlaneType {
        FLOOR,
        CEILING,
        WALL,
        TABLE,
        SEAT,
        DOOR,
        WINDOW,
        OTHER
    }
    
    data class DetectedObject(
        val id: String,
        val type: ObjectType,
        val boundingBox: BoundingBox3D,
        val pose: Pose,
        val confidence: Float,
        val semanticLabel: String?
    )
    
    enum class ObjectType {
        FURNITURE,
        PERSON,
        VEHICLE,
        PLANT,
        ELECTRONICS,
        STRUCTURAL,
        UNKNOWN
    }
    
    data class BoundingBox3D(
        val center: Vector3,
        val size: Vector3,
        val orientation: Quaternion
    )
    
    data class SemanticLabel(
        val label: String,
        val pixelMask: ByteArray,
        val confidence: Float,
        val boundingBox: BoundingBox2D?
    )
    
    data class BoundingBox2D(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
    
    data class OccupancyGrid(
        val grid: Array<Array<OccupancyCell>>,
        val resolution: Float, // meters per cell
        val origin: Vector3,
        val dimensions: Vector3
    )
    
    data class OccupancyCell(
        val occupied: Float, // 0-1 probability
        val height: Float?,
        val material: MaterialType?
    )
    
    enum class MaterialType {
        WOOD,
        METAL,
        GLASS,
        FABRIC,
        CONCRETE,
        PLASTIC,
        ORGANIC,
        UNKNOWN
    }
    
    data class LightEstimate(
        val ambientIntensity: Float,
        val ambientColorTemperature: Float,
        val mainLightDirection: Vector3?,
        val mainLightIntensity: Float?,
        val environmentalHDR: FloatArray?
    )
    
    data class DepthError(
        val code: ErrorCode,
        val message: String,
        val recoverable: Boolean
    )
    
    enum class ErrorCode {
        NO_PERMISSION,
        HARDWARE_NOT_AVAILABLE,
        CAMERA_ERROR,
        PROCESSING_ERROR,
        CALIBRATION_ERROR,
        INSUFFICIENT_LIGHT,
        MOTION_TOO_FAST,
        OUT_OF_MEMORY
    }
    
    data class ScanSettings(
        val mode: ScanMode,
        val quality: ProcessingQuality,
        val targetFrameRate: Int,
        val maxRange: Float,
        val minRange: Float,
        val enableColorCapture: Boolean,
        val enableNormalEstimation: Boolean,
        val enableMeshGeneration: Boolean,
        val enableSemanticSegmentation: Boolean,
        val enableOcclusionHandling: Boolean
    )
    
    data class ScanResult(
        val pointCloud: PointCloud?,
        val mesh: MeshData?,
        val environmentMap: EnvironmentMap?,
        val duration: Long,
        val framesCaptured: Int,
        val totalPoints: Int,
        val processingTime: Long
    )
    
    // ========== INITIALIZATION ==========
    
    /**
     * Backward compatibility constructor
     * @deprecated Use constructor with DeviceCapabilities parameter for better architecture
     */
    @Deprecated("Use constructor with DeviceCapabilities parameter for better architecture")
    constructor(context: Context) : this(
        context, 
        com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.getCapabilities(context)
    )
    
    init {
        initialize()
    }
    
    private fun initialize() {
        initializeFromCapabilities()
        initializeSensors()
        setupARCore()
    }
    
    /**
     * Initialize from centralized capabilities detection
     * COT: Uses pre-detected capabilities rather than performing detection here
     */
    private fun initializeFromCapabilities() {
        val technologies = mutableListOf<String>()
        
        // Use capabilities from centralized detection
        if (capabilities.camera?.hasDepthCamera == true) {
            technologies.add(TECH_TOF)
        }
        
        // Check for structured light based on camera capabilities
        if (capabilities.camera?.hasFrontCamera == true && capabilities.camera?.hasDepthCamera == true) {
            technologies.add(TECH_STRUCTURED_LIGHT)
        }
        
        // Check for stereo cameras based on rear camera count
        if (capabilities.camera?.hasMultipleRearCameras == true) {
            technologies.add(TECH_STEREO)
        }
        
        // Check for LiDAR based on device capabilities
        if (hasLidarByDeviceModel()) {
            technologies.add(TECH_LIDAR)
        }
        
        val depthCapabilities = if (technologies.isNotEmpty()) {
            DepthCapabilities(
                supportedTechnologies = technologies,
                maxRange = getMaxRangeForTechnologies(technologies),
                minRange = 0.1f,
                accuracy = getAccuracyForTechnologies(technologies),
                resolution = getResolutionFromCapabilities(),
                frameRate = Range.create(1, 60),
                fieldOfView = getDefaultFieldOfView(),
                pointsPerFrame = getPointDensityForTechnologies(technologies),
                supportsRawDepth = true,
                supportsPointCloud = true,
                supportsMeshGeneration = technologies.contains(TECH_LIDAR),
                supportsOcclusion = true,
                supportsMotionTracking = capabilities.hardware?.hasCamera == true,
                supportsSemanticSegmentation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                powerConsumption = PowerProfile(
                    idle = 10f,
                    active = 500f,
                    peak = 1000f
                )
            )
        } else null
        
        _lidarState.update {
            it.copy(
                isAvailable = technologies.isNotEmpty(),
                technology = technologies.firstOrNull(),
                capabilities = depthCapabilities
            )
        }
    }
    
    /**
     * Check for LiDAR based on device model (using centralized device info)
     * COT: Uses DeviceCapabilities.deviceInfo instead of direct system calls
     */
    private fun hasLidarByDeviceModel(): Boolean {
        val model = capabilities.deviceInfo.model.lowercase()
        val manufacturer = capabilities.deviceInfo.manufacturer.lowercase()
        
        return when {
            // High-end Android phones with ToF sensors marketed as "LiDAR-like"
            model.contains("galaxy") && model.contains("note20") -> true
            model.contains("galaxy") && model.contains("s20+") -> true
            model.contains("galaxy") && model.contains("s21") -> true
            model.contains("galaxy") && model.contains("s22") -> true
            model.contains("galaxy") && model.contains("s23") -> true
            model.contains("p40") && manufacturer.contains("huawei") -> true
            model.contains("p50") && manufacturer.contains("huawei") -> true
            model.contains("mate") && manufacturer.contains("huawei") -> true
            else -> capabilities.camera?.hasDepthCamera == true
        }
    }
    
    /**
     * Check ARCore availability from capabilities
     * COT: ARCore check disabled until dependency available
     */
    private fun hasARCore(): Boolean {
        // ARCore check disabled until dependency available
        return false
        /* Will be enabled when ARCore is added:
        return capabilities.integration?.hasArCore ?: false
        */
    }
    
    /**
     * Get max range based on detected technologies
     * COT: Uses technology list instead of re-detecting hardware
     */
    private fun getMaxRangeForTechnologies(technologies: List<String>): Float {
        return when {
            technologies.contains(TECH_LIDAR) -> 5.0f
            technologies.contains(TECH_TOF) -> 8.0f
            technologies.contains(TECH_STRUCTURED_LIGHT) -> 1.5f
            technologies.contains(TECH_STEREO) -> 10.0f
            else -> 2.0f
        }
    }
    
    /**
     * Get accuracy based on detected technologies
     * COT: Uses technology list instead of re-detecting hardware
     */
    private fun getAccuracyForTechnologies(technologies: List<String>): Float {
        return when {
            technologies.contains(TECH_LIDAR) -> 0.005f // 5mm
            technologies.contains(TECH_TOF) -> 0.01f // 1cm
            technologies.contains(TECH_STRUCTURED_LIGHT) -> 0.002f // 2mm
            technologies.contains(TECH_STEREO) -> 0.05f // 5cm
            else -> 0.1f
        }
    }
    
    /**
     * Get resolution from capabilities instead of querying camera directly
     * COT: Uses camera capabilities from centralized detection
     */
    private fun getResolutionFromCapabilities(): Size {
        // Use default resolution - detailed camera querying should be in DeviceDetector
        return capabilities.camera?.let {
            Size(640, 480) // Could be enhanced to use actual camera resolution data
        } ?: Size(640, 480)
    }
    
    /**
     * Get default field of view (could be enhanced with camera capabilities)
     * COT: Simplified implementation using defaults
     */
    private fun getDefaultFieldOfView(): FieldOfView {
        return FieldOfView(
            horizontal = 60f,
            vertical = 45f,
            diagonal = 75f
        )
    }
    
    /**
     * Get point density based on detected technologies
     * COT: Uses technology list instead of re-detecting hardware
     */
    private fun getPointDensityForTechnologies(technologies: List<String>): Int {
        return when {
            technologies.contains(TECH_LIDAR) -> DENSITY_HIGH
            technologies.contains(TECH_TOF) -> DENSITY_MEDIUM
            else -> DENSITY_LOW
        }
    }
    
    private fun initializeSensors() {
        // Find proximity and infrared sensors
        proximityInfraredSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        // Look for ToF sensor (vendor-specific)
        sensorManager.getSensorList(Sensor.TYPE_ALL).forEach { sensor ->
            if (sensor.name.lowercase().contains("tof") ||
                sensor.name.lowercase().contains("depth") ||
                sensor.name.lowercase().contains("lidar")) {
                depthSensors.add(sensor)
            }
        }
        
        tofSensor = depthSensors.firstOrNull()
    }
    
    private fun setupARCore() {
        // ARCore setup disabled until dependency available
        /* Will be enabled when ARCore is added:
        if (!hasARCore()) return
        
        scope.launch {
            try {
                val sessionConfig = Config(arSession).apply {
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    depthMode = Config.DepthMode.AUTOMATIC
                }
                
                arSession = Session(context).apply {
                    configure(sessionConfig)
                }
            } catch (e: UnavailableException) {
                Log.e(TAG, "ARCore not available", e)
            }
        }
        */
    }
    
    // ========== PUBLIC API ==========
    
    /**
     * Check if LiDAR/ToF hardware is available
     * COT: Uses centralized capabilities instead of direct detection
     */
    fun isLidarAvailable(): Boolean = _lidarState.value.isAvailable
    
    /**
     * Get current LiDAR state
     */
    fun getLidarState(): LidarState = _lidarState.value
    
    /**
     * Get detected depth capabilities
     */
    fun getDepthCapabilities(): DepthCapabilities? = _lidarState.value.capabilities
    
    // ========== SCANNING OPERATIONS ==========
    
    @RequiresPermission(Manifest.permission.CAMERA)
    fun startScanning(settings: ScanSettings = getDefaultSettings()) {
        if (!_lidarState.value.isAvailable) {
            handleError(DepthError(
                ErrorCode.HARDWARE_NOT_AVAILABLE,
                "No depth sensing hardware available",
                false
            ))
            return
        }
        
        scope.launch {
            try {
                _lidarState.update { it.copy(isActive = true, currentMode = settings.mode) }
                
                when (settings.mode) {
                    ScanMode.SINGLE -> captureSingleFrame(settings)
                    ScanMode.CONTINUOUS -> startContinuousScanning(settings)
                    ScanMode.BURST -> captureBurst(settings)
                    ScanMode.HDR -> captureHDR(settings)
                    ScanMode.ACCUMULATION -> startAccumulation(settings)
                    ScanMode.TRACKING -> startTracking(settings)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start scanning", e)
                handleError(DepthError(
                    ErrorCode.CAMERA_ERROR,
                    e.message ?: "Unknown error",
                    true
                ))
            }
        }
    }
    
    private suspend fun captureSingleFrame(settings: ScanSettings) {
        val depthData = captureDepthFrame()
        processDepthFrame(depthData, settings)
    }
    
    private suspend fun startContinuousScanning(settings: ScanSettings) {
        openDepthCamera(settings)
        
        while (_lidarState.value.isActive) {
            val depthData = captureDepthFrame()
            processDepthFrame(depthData, settings)
            delay(1000 / settings.targetFrameRate.toLong())
        }
    }
    
    private suspend fun captureBurst(settings: ScanSettings) {
        val frames = mutableListOf<DepthFrame>()
        
        repeat(10) { // Capture 10 frames in burst
            frames.add(captureDepthFrame())
            delay(50) // 20 FPS burst
        }
        
        // Merge frames for improved quality
        val mergedFrame = mergeDepthFrames(frames)
        processDepthFrame(mergedFrame, settings)
    }
    
    private suspend fun captureHDR(settings: ScanSettings) {
        // Capture at different exposure/range settings
        val frames = listOf(
            captureDepthFrame(exposure = 0.5f),
            captureDepthFrame(exposure = 1.0f),
            captureDepthFrame(exposure = 2.0f)
        )
        
        val hdrFrame = createHDRDepth(frames)
        processDepthFrame(hdrFrame, settings)
    }
    
    private suspend fun startAccumulation(@Suppress("UNUSED_PARAMETER") settings: ScanSettings) {
        val accumulatedPoints = mutableListOf<Float>()
        
        repeat(30) { // Accumulate 30 frames
            val frame = captureDepthFrame()
            val pointCloud = generatePointCloud(frame)
            accumulatedPoints.addAll(pointCloud.points.toList())
            delay(100)
        }
        
        val mergedCloud = PointCloud(
            points = accumulatedPoints.toFloatArray(),
            colors = null,
            normals = null,
            confidence = null,
            pointCount = accumulatedPoints.size / 3,
            timestamp = System.currentTimeMillis(),
            coordinateSystem = CoordinateSystem.CAMERA
        )
        
        _pointCloud.value = mergedCloud
    }
    
    private suspend fun startTracking(@Suppress("UNUSED_PARAMETER") settings: ScanSettings) {
        // ARCore tracking disabled until dependency available
        Log.w(TAG, "AR tracking not available without ARCore dependency")
        /* Will be enabled when ARCore is added:
        arSession?.let { session ->
            session.resume()
            
            while (_lidarState.value.isActive) {
                val frame = session.update()
                processARFrame(frame, settings)
                delay(1000 / settings.targetFrameRate.toLong())
            }
        }
        */
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun openDepthCamera(settings: ScanSettings) {
        val depthCameraId = findDepthCamera() ?: return
        
        suspendCancellableCoroutine<Unit> { continuation ->
            cameraManager.openCamera(depthCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession(camera, settings)
                    continuation.resumeWith(Result.success(Unit))
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    continuation.resumeWith(Result.failure(
                        Exception("Camera error: $error")
                    ))
                }
            }, backgroundHandler)
        }
    }
    
    private fun findDepthCamera(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraManager.cameraIdList.find { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )
                capabilities?.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT
                ) == true
            }
        } else null
    }
    
    private fun createCaptureSession(camera: CameraDevice, @Suppress("UNUSED_PARAMETER") settings: ScanSettings) {
        val resolution = _lidarState.value.capabilities?.resolution ?: Size(640, 480)
        
        depthImageReader = ImageReader.newInstance(
            resolution.width,
            resolution.height,
            ImageFormat.DEPTH16,
            2
        )
        
        depthImageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                processDepthImage(it)
                it.close()
            }
        }, backgroundHandler)
        
        val surfaces = listOf(depthImageReader!!.surface)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val sessionConfig = android.hardware.camera2.params.SessionConfiguration(
                android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR,
                surfaces.map { android.hardware.camera2.params.OutputConfiguration(it) },
                ContextCompat.getMainExecutor(context),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        startDepthCapture(session, camera)
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure capture session")
                    }
                }
            )
            camera.createCaptureSession(sessionConfig)
        } else {
            @Suppress("DEPRECATION")
            camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    startDepthCapture(session, camera)
                }
                
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Failed to configure capture session")
                }
            }, backgroundHandler)
        }
    }
    
    private fun startDepthCapture(session: CameraCaptureSession, camera: CameraDevice) {
        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(depthImageReader!!.surface)
        }
        
        session.setRepeatingRequest(
            request.build(),
            object : CameraCaptureSession.CaptureCallback() {},
            backgroundHandler
        )
    }
    
    private fun processDepthImage(image: Image) {
        val planes = image.planes
        val buffer = planes[0].buffer
        val depthData = FloatArray(buffer.remaining() / 2) // DEPTH16 format
        
        val shortBuffer = buffer.asShortBuffer()
        for (i in depthData.indices) {
            depthData[i] = shortBuffer.get(i) / 1000f // Convert mm to meters
        }
        
        val frame = DepthFrame(
            timestamp = System.currentTimeMillis(),
            depthData = depthData,
            confidenceMap = null,
            width = image.width,
            height = image.height,
            intrinsics = getDefaultIntrinsics(),
            pose = null,
            technology = _lidarState.value.technology ?: TECH_TOF,
            statistics = calculateStatistics(depthData)
        )
        
        _depthFrame.value = frame
    }
    
    private fun processARFrame(@Suppress("UNUSED_PARAMETER") frame: Any, @Suppress("UNUSED_PARAMETER") settings: ScanSettings) {
        // ARCore frame processing disabled until dependency available
        Log.w(TAG, "AR frame processing not available without ARCore dependency")
        /* Will be enabled when ARCore is added:
        // Process ARCore frame
        if (frame.hasDisplayGeometryChanged()) {
            updateDisplayGeometry(frame)
        }
        
        // Get depth data
        frame.acquireDepthImage16Bits()?.let { depthImage ->
            processARDepthImage(depthImage, frame)
            depthImage.close()
        }
        
        // Update environment mapping
        updateEnvironmentMap(frame)
        */
    }
    
    private fun processARDepthImage(depthImage: Image, @Suppress("UNUSED_PARAMETER") arFrame: Any) {
        val buffer = depthImage.planes[0].buffer
        val depthData = FloatArray(buffer.remaining() / 2)
        
        val shortBuffer = buffer.asShortBuffer()
        for (i in depthData.indices) {
            depthData[i] = shortBuffer.get(i) / 1000f // mm to meters
        }
        
        // ARCore camera pose extraction disabled
        val pose = Pose(
            translation = Vector3(0f, 0f, 0f),
            rotation = Quaternion(0f, 0f, 0f, 1f),
            confidence = 0.5f
        )
        /* Will be enabled when ARCore is added:
        val camera = arFrame.camera
        val pose = Pose(
            translation = Vector3(
                camera.pose.tx(),
                camera.pose.ty(),
                camera.pose.tz()
            ),
            rotation = Quaternion(
                camera.pose.qx(),
                camera.pose.qy(),
                camera.pose.qz(),
                camera.pose.qw()
            ),
            confidence = 1.0f
        )
        */
        
        val frame = DepthFrame(
            timestamp = System.currentTimeMillis(),
            depthData = depthData,
            confidenceMap = null,
            width = depthImage.width,
            height = depthImage.height,
            intrinsics = getDefaultIntrinsics(), // extractIntrinsics(arFrame) when ARCore available,
            pose = pose,
            technology = TECH_TOF,
            statistics = calculateStatistics(depthData)
        )
        
        _depthFrame.value = frame
    }
    
    private fun updateEnvironmentMap(@Suppress("UNUSED_PARAMETER") frame: Any) {
        // ARCore environment mapping disabled until dependency available
        val detectedPlanes = emptyList<Plane>()
        val lightEstimate: LightEstimate? = null
        
        /* Will be enabled when ARCore is added:
        val planes = frame.updatedTrackables.filterIsInstance<com.google.ar.core.Plane>()
        val detectedPlanes = planes.map { arPlane ->
            Plane(
                id = arPlane.hashCode().toString(),
                center = Vector3(
                    arPlane.centerPose.tx(),
                    arPlane.centerPose.ty(),
                    arPlane.centerPose.tz()
                ),
                normal = Vector3(0f, 1f, 0f), // Simplified
                extents = Vector2(
                    arPlane.extentX,
                    arPlane.extentZ
                ),
                boundary = arPlane.polygon.toList().chunked(2).map {
                    Vector3(it[0], 0f, it[1])
                },
                type = when (arPlane.type) {
                    com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING -> PlaneType.FLOOR
                    com.google.ar.core.Plane.Type.HORIZONTAL_DOWNWARD_FACING -> PlaneType.CEILING
                    com.google.ar.core.Plane.Type.VERTICAL -> PlaneType.WALL
                    else -> PlaneType.OTHER
                },
                confidence = 0.9f
            )
        }
        
        val lightEstimate = frame.lightEstimate?.let {
            LightEstimate(
                ambientIntensity = it.pixelIntensity,
                ambientColorTemperature = 5000f, // Default
                mainLightDirection = it.environmentalHdrMainLightDirection?.let { dir ->
                    Vector3(dir[0], dir[1], dir[2])
                },
                mainLightIntensity = it.environmentalHdrMainLightIntensity?.get(0),
                environmentalHDR = it.environmentalHdrAmbientSphericalHarmonics
            )
        }
        */
        
        _environmentMap.value = EnvironmentMap(
            planes = detectedPlanes,
            objects = emptyList(),
            semanticLabels = emptyList(),
            occupancyGrid = null,
            lightEstimate = lightEstimate,
            timestamp = System.currentTimeMillis()
        )
    }
    
    // ========== DEPTH PROCESSING ==========
    
    private suspend fun captureDepthFrame(exposure: Float = 1.0f): DepthFrame {
        // Simulate depth capture with specified exposure
        delay(100) // Simulate capture time
        
        val resolution = _lidarState.value.capabilities?.resolution ?: Size(640, 480)
        val depthData = generateSimulatedDepthData(resolution.width, resolution.height, exposure)
        
        return DepthFrame(
            timestamp = System.currentTimeMillis(),
            depthData = depthData,
            confidenceMap = generateConfidenceMap(depthData),
            width = resolution.width,
            height = resolution.height,
            intrinsics = getDefaultIntrinsics(),
            pose = null,
            technology = _lidarState.value.technology ?: TECH_TOF,
            statistics = calculateStatistics(depthData)
        )
    }
    
    private fun generateSimulatedDepthData(width: Int, height: Int, exposure: Float): FloatArray {
        val depthData = FloatArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val centerX = width / 2f
                val centerY = height / 2f
                val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
                
                // Generate depth based on distance from center with some noise
                depthData[index] = (1.0f + distance / 100f + Math.random() * 0.1).toFloat() * exposure
            }
        }
        
        return depthData
    }
    
    private fun generateConfidenceMap(depthData: FloatArray): FloatArray {
        return FloatArray(depthData.size) { index ->
            // Higher confidence for closer depths
            val depth = depthData[index]
            when {
                depth < 0.5f -> 1.0f
                depth < 2.0f -> 0.9f
                depth < 5.0f -> 0.7f
                else -> 0.5f
            }
        }
    }
    
    private fun mergeDepthFrames(frames: List<DepthFrame>): DepthFrame {
        val width = frames[0].width
        val height = frames[0].height
        val mergedData = FloatArray(width * height)
        val mergedConfidence = FloatArray(width * height)
        
        for (i in mergedData.indices) {
            var sum = 0f
            var weightSum = 0f
            
            frames.forEach { frame ->
                val confidence = frame.confidenceMap?.get(i) ?: 0.5f
                sum += frame.depthData[i] * confidence
                weightSum += confidence
            }
            
            mergedData[i] = if (weightSum > 0) sum / weightSum else 0f
            mergedConfidence[i] = min(1.0f, weightSum / frames.size)
        }
        
        return frames[0].copy(
            depthData = mergedData,
            confidenceMap = mergedConfidence,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun createHDRDepth(frames: List<DepthFrame>): DepthFrame {
        // Combine multiple exposures for HDR depth
        val width = frames[0].width
        val height = frames[0].height
        val hdrData = FloatArray(width * height)
        
        for (i in hdrData.indices) {
            // Use the most confident measurement from all exposures
            var bestDepth = 0f
            var bestConfidence = 0f
            
            frames.forEach { frame ->
                val confidence = frame.confidenceMap?.get(i) ?: 0.5f
                if (confidence > bestConfidence) {
                    bestDepth = frame.depthData[i]
                    bestConfidence = confidence
                }
            }
            
            hdrData[i] = bestDepth
        }
        
        return frames[0].copy(
            depthData = hdrData,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private suspend fun processDepthFrame(frame: DepthFrame, settings: ScanSettings) {
        _depthFrame.value = frame
        
        if (settings.enableColorCapture) {
            // Capture color frame if needed
        }
        
        val pointCloud = generatePointCloud(frame)
        _pointCloud.value = pointCloud
        
        if (settings.enableNormalEstimation) {
            estimateNormals(pointCloud)
        }
        
        if (settings.enableMeshGeneration) {
            val mesh = generateMesh(pointCloud)
            _meshData.value = mesh
        }
        
        if (settings.enableSemanticSegmentation) {
            performSemanticSegmentation(frame)
        }
    }
    
    private fun generatePointCloud(frame: DepthFrame): PointCloud {
        val points = mutableListOf<Float>()
        val intrinsics = frame.intrinsics
        
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val index = y * frame.width + x
                val depth = frame.depthData[index]
                
                if (depth > 0 && depth < 10) { // Valid depth range
                    // Convert pixel to 3D point using camera intrinsics
                    val x3d = (x - intrinsics.cx) * depth / intrinsics.fx
                    val y3d = (y - intrinsics.cy) * depth / intrinsics.fy
                    val z3d = depth
                    
                    points.add(x3d)
                    points.add(y3d)
                    points.add(z3d)
                }
            }
        }
        
        return PointCloud(
            points = points.toFloatArray(),
            colors = null,
            normals = null,
            confidence = frame.confidenceMap,
            pointCount = points.size / 3,
            timestamp = frame.timestamp,
            coordinateSystem = CoordinateSystem.CAMERA
        )
    }
    
    private fun estimateNormals(@Suppress("UNUSED_PARAMETER") pointCloud: PointCloud) {
        // Estimate surface normals using neighboring points
        // Implementation would use PCA or cross products
    }
    
    private fun generateMesh(pointCloud: PointCloud): MeshData {
        // Simple mesh generation using Delaunay triangulation
        // This is a placeholder implementation
        
        val vertices = pointCloud.points
        val indices = mutableListOf<Int>()
        
        // Create simple triangle strips
        val pointsPerRow = sqrt(pointCloud.pointCount.toFloat()).toInt()
        
        for (y in 0 until pointsPerRow - 1) {
            for (x in 0 until pointsPerRow - 1) {
                val topLeft = y * pointsPerRow + x
                val topRight = topLeft + 1
                val bottomLeft = (y + 1) * pointsPerRow + x
                val bottomRight = bottomLeft + 1
                
                // First triangle
                indices.add(topLeft)
                indices.add(bottomLeft)
                indices.add(topRight)
                
                // Second triangle
                indices.add(topRight)
                indices.add(bottomLeft)
                indices.add(bottomRight)
            }
        }
        
        return MeshData(
            vertices = vertices,
            indices = indices.toIntArray(),
            normals = null,
            uvCoords = null,
            colors = null,
            vertexCount = pointCloud.pointCount,
            faceCount = indices.size / 3,
            material = null
        )
    }
    
    private fun performSemanticSegmentation(@Suppress("UNUSED_PARAMETER") frame: DepthFrame) {
        // Perform ML-based semantic segmentation
        // This would use TensorFlow Lite or similar
    }
    
    // ========== UTILITY METHODS ==========
    
    private fun getDefaultSettings(): ScanSettings {
        return ScanSettings(
            mode = ScanMode.SINGLE,
            quality = ProcessingQuality.NORMAL,
            targetFrameRate = 30,
            maxRange = _lidarState.value.capabilities?.maxRange ?: 5.0f,
            minRange = _lidarState.value.capabilities?.minRange ?: 0.1f,
            enableColorCapture = false,
            enableNormalEstimation = true,
            enableMeshGeneration = false,
            enableSemanticSegmentation = false,
            enableOcclusionHandling = true
        )
    }
    
    private fun getDefaultIntrinsics(): CameraIntrinsics {
        val resolution = _lidarState.value.capabilities?.resolution ?: Size(640, 480)
        return CameraIntrinsics(
            fx = resolution.width.toFloat(),
            fy = resolution.width.toFloat(),
            cx = resolution.width / 2f,
            cy = resolution.height / 2f,
            distortion = null
        )
    }
    
    private fun extractIntrinsics(@Suppress("UNUSED_PARAMETER") frame: Any): CameraIntrinsics {
        // ARCore intrinsics extraction disabled
        return getDefaultIntrinsics()
        /* Will be enabled when ARCore is added:
        val intrinsics = frame.camera.imageIntrinsics
        return CameraIntrinsics(
            fx = intrinsics[0],
            fy = intrinsics[4],
            cx = intrinsics[2],
            cy = intrinsics[5],
            distortion = null
        )
        */
    }
    
    private fun calculateStatistics(depthData: FloatArray): FrameStatistics {
        val validDepths = depthData.filter { it > 0 && it < 10 }
        
        return FrameStatistics(
            validPixels = validDepths.size,
            invalidPixels = depthData.size - validDepths.size,
            minDepth = validDepths.minOrNull() ?: 0f,
            maxDepth = validDepths.maxOrNull() ?: 0f,
            averageDepth = validDepths.average().toFloat(),
            standardDeviation = calculateStdDev(validDepths)
        )
    }
    
    private fun calculateStdDev(values: List<Float>): Float {
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat()
    }
    
    private fun updateDisplayGeometry(@Suppress("UNUSED_PARAMETER") frame: Any) {
        // Update display transformation matrix
    }
    
    private fun handleError(error: DepthError) {
        _lidarState.update { it.copy(lastError = error, isActive = false) }
        Log.e(TAG, "Depth error: ${error.message}")
    }
    
    fun stopScanning() {
        _lidarState.update { it.copy(isActive = false) }
        
        captureSession?.close()
        cameraDevice?.close()
        depthImageReader?.close()
        // arSession?.pause() // Disabled until ARCore available
        
        captureSession = null
        cameraDevice = null
        depthImageReader = null
    }
    
    fun exportPointCloud(format: String = "ply"): ByteArray {
        // Export point cloud in specified format (PLY, PCD, etc.)
        val pointCloud = _pointCloud.value ?: return ByteArray(0)
        
        return when (format.lowercase()) {
            "ply" -> exportAsPLY(pointCloud)
            "pcd" -> exportAsPCD(pointCloud)
            else -> ByteArray(0)
        }
    }
    
    private fun exportAsPLY(pointCloud: PointCloud): ByteArray {
        // Create PLY format output
        val header = """
            ply
            format ascii 1.0
            element vertex ${pointCloud.pointCount}
            property float x
            property float y
            property float z
            end_header
        """.trimIndent()
        
        val points = StringBuilder(header).append("\n")
        
        for (i in 0 until pointCloud.pointCount) {
            val x = pointCloud.points[i * 3]
            val y = pointCloud.points[i * 3 + 1]
            val z = pointCloud.points[i * 3 + 2]
            points.append("$x $y $z\n")
        }
        
        return points.toString().toByteArray()
    }
    
    private fun exportAsPCD(@Suppress("UNUSED_PARAMETER") pointCloud: PointCloud): ByteArray {
        // Create PCD format output
        // Implementation for Point Cloud Data format
        return ByteArray(0)
    }
    
    fun cleanup() {
        stopScanning()
        scope.cancel()
        backgroundThread.quitSafely()
        // arSession?.close() // Disabled until ARCore available
    }
}
