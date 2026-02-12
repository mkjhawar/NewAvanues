// Author: Manoj Jhawar
// Purpose: Comprehensive foldable device detection and state management

package com.augmentalis.devicemanager.smartdevices

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Comprehensive foldable device state management
 * Tracks hinge angle, posture, and screen configuration
 */
class FoldableDeviceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FoldableDeviceManager"
        
        // Posture angle thresholds
        private const val FLAT_POSTURE_MIN_ANGLE = 170f
        private const val HALF_OPENED_POSTURE_MIN_ANGLE = 30f
        private const val HALF_OPENED_POSTURE_MAX_ANGLE = 150f
        private const val TABLETOP_MIN_ANGLE = 75f
        private const val TABLETOP_MAX_ANGLE = 115f
        
        // Update debounce
        private const val STATE_UPDATE_DEBOUNCE_MS = 100L
    }
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val windowInfoTracker = WindowInfoTracker.getOrCreate(context)
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateDebounceJob: Job? = null
    
    // State flows
    private val _foldState = MutableStateFlow(FoldableState())
    val foldState: StateFlow<FoldableState> = _foldState.asStateFlow()
    
    private val _orientationState = MutableStateFlow(OrientationState())
    val orientationState: StateFlow<OrientationState> = _orientationState.asStateFlow()
    
    private val _multiDisplayState = MutableStateFlow(MultiDisplayState())
    val multiDisplayState: StateFlow<MultiDisplayState> = _multiDisplayState.asStateFlow()
    
    // Hinge angle sensor listener
    private val hingeAngleListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_HINGE_ANGLE) {
                updateHingeAngle(event.values[0])
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    
    // ========== DATA MODELS ==========
    
    data class FoldableState(
        val isFoldable: Boolean = false,
        val foldState: FoldState = FoldState.UNKNOWN,
        val hingeAngle: Float = 0f,
        val posture: DevicePosture = DevicePosture.UNKNOWN,
        val activeScreens: List<ScreenIdentifier> = emptyList(),
        val creaseInfo: CreaseInfo? = null,
        val continuitySupported: Boolean = false,
        val transitionDuration: Long = 0L,
        val manufacturer: FoldableManufacturer = FoldableManufacturer.UNKNOWN,
        val model: FoldableModel = FoldableModel.UNKNOWN,
        val hingeOrientation: HingeOrientation = HingeOrientation.UNKNOWN,
        val postureConfidence: Float = 0f
    )
    
    enum class FoldState {
        CLOSED,         // Phone completely closed (0-30°)
        HALF_OPEN,      // Partially open (30-150°)
        OPEN,           // Fully open (150-180°)
        FLIPPED,        // Flipped backwards (>180°)
        UNKNOWN
    }
    
    enum class DevicePosture {
        CLOSED,         // Device is closed
        FLAT,           // Device is flat (170-180°)
        TENT,           // Standing like a tent (45-75°)
        TABLETOP,       // Laptop-like position (75-115°)
        BOOK,           // Half-folded like a book (115-150°)
        FLIPPED,        // Flipped backwards
        UNKNOWN
    }
    
    enum class FoldableManufacturer {
        SAMSUNG,
        GOOGLE,
        MICROSOFT,
        OPPO,
        XIAOMI,
        HUAWEI,
        MOTOROLA,
        LG,
        HONOR,
        VIVO,
        UNKNOWN
    }
    
    enum class FoldableModel {
        // Samsung
        GALAXY_FOLD,
        GALAXY_Z_FOLD_2,
        GALAXY_Z_FOLD_3,
        GALAXY_Z_FOLD_4,
        GALAXY_Z_FOLD_5,
        GALAXY_Z_FLIP,
        GALAXY_Z_FLIP_3,
        GALAXY_Z_FLIP_4,
        GALAXY_Z_FLIP_5,
        
        // Google
        PIXEL_FOLD,
        
        // Microsoft
        SURFACE_DUO,
        SURFACE_DUO_2,
        
        // OPPO
        OPPO_FIND_N,
        OPPO_FIND_N2,
        OPPO_FIND_N2_FLIP,
        OPPO_FIND_N3,
        
        // Xiaomi
        XIAOMI_MIX_FOLD,
        XIAOMI_MIX_FOLD_2,
        XIAOMI_MIX_FOLD_3,
        
        // Huawei
        HUAWEI_MATE_X,
        HUAWEI_MATE_X2,
        HUAWEI_MATE_XS,
        HUAWEI_P50_POCKET,
        
        // Motorola
        MOTOROLA_RAZR,
        MOTOROLA_RAZR_2022,
        MOTOROLA_RAZR_40,
        MOTOROLA_RAZR_40_ULTRA,
        
        // Honor
        HONOR_MAGIC_V,
        HONOR_MAGIC_V2,
        HONOR_MAGIC_VS,
        
        // Vivo
        VIVO_X_FOLD,
        VIVO_X_FOLD_PLUS,
        VIVO_X_FOLD_2,
        
        // LG (discontinued but for legacy support)
        LG_WING,
        LG_V60_DUAL_SCREEN,
        LG_G8X_DUAL_SCREEN,
        
        UNKNOWN
    }
    
    enum class HingeOrientation {
        HORIZONTAL,     // Fold/flip style
        VERTICAL,       // Book style
        SWIVEL,         // LG Wing style
        DUAL_SCREEN,    // Surface Duo style
        UNKNOWN
    }
    
    data class ScreenIdentifier(
        val id: Int,
        val type: ScreenType,
        val isActive: Boolean,
        val bounds: Rect
    )
    
    enum class ScreenType {
        INNER_MAIN,
        OUTER_COVER,
        LEFT_SCREEN,
        RIGHT_SCREEN,
        SECONDARY_SWIVEL,
        EXTERNAL_DISPLAY
    }
    
    data class CreaseInfo(
        val bounds: Rect,
        val width: Int,
        val orientation: CreaseOrientation,
        val shouldAvoid: Boolean = true
    )
    
    enum class CreaseOrientation {
        HORIZONTAL, VERTICAL
    }
    
    data class OrientationState(
        val currentOrientation: Orientation = Orientation.PORTRAIT,
        val deviceNaturalOrientation: Orientation = Orientation.PORTRAIT,
        val rotationDegrees: Int = 0,
        val isAutoRotateEnabled: Boolean = false,
        val isRotationLocked: Boolean = false,
        val userPreferredOrientation: Orientation? = null,
        val isTabletopStable: Boolean = false,
        val tiltAngle: Float = 0f,
        val isFaceUp: Boolean = false,
        val isFaceDown: Boolean = false,
        val supportsReverseOrientation: Boolean = false,
        val isLockedToLandscape: Boolean = false,
        val isPortraitOnly: Boolean = false
    )
    
    enum class Orientation {
        PORTRAIT,
        LANDSCAPE,
        REVERSE_PORTRAIT,
        REVERSE_LANDSCAPE
    }
    
    data class MultiDisplayState(
        val displayCount: Int = 1,
        val displays: List<DisplayInfo> = emptyList(),
        val primaryDisplayId: Int = 0,
        val presentationDisplayId: Int? = null,
        val displayArrangement: DisplayArrangement = DisplayArrangement.SINGLE,
        val spanningMode: SpanningMode? = null,
        val isDualScreenDevice: Boolean = false,
        val screenSpan: Rect? = null,
        val hingeGap: Int? = null,
        val isSpannedAcrossScreens: Boolean = false,
        val hasExternalDisplay: Boolean = false,
        val externalDisplayMode: ExternalDisplayMode? = null,
        val isWirelessDisplay: Boolean = false,
        val displayLatency: Long? = null
    )
    
    enum class DisplayArrangement {
        SINGLE,
        EXTENDED,
        MIRRORED,
        DUAL_INDEPENDENT,
        SPANNING
    }
    
    enum class SpanningMode {
        SINGLE_SCREEN,
        DUAL_SCREEN,
        SPANNING_BOTH
    }
    
    enum class ExternalDisplayMode {
        MIRROR,
        EXTEND,
        SECOND_SCREEN,
        PRESENTATION
    }
    
    data class DisplayInfo(
        val displayId: Int,
        val name: String,
        val isInternal: Boolean,
        val bounds: Rect,
        val rotation: Int,
        val refreshRate: Float,
        val density: Float,
        val isHDR: Boolean,
        val isWideColorGamut: Boolean,
        val cutout: DisplayCutout?,
        val roundedCorners: RoundedCorners?,
        val isActive: Boolean,
        val isTouchEnabled: Boolean
    )
    
    data class DisplayCutout(
        val bounds: List<Rect>,
        val safeInsets: Insets
    )
    
    data class RoundedCorners(
        val topLeft: Float,
        val topRight: Float,
        val bottomLeft: Float,
        val bottomRight: Float
    )
    
    data class Insets(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )
    
    // ========== INITIALIZATION ==========
    
    fun initialize() {
        detectFoldableDevice()
        registerHingeAngleSensor()
        observeWindowLayoutChanges()
        updateOrientationState()
        updateMultiDisplayState()
    }
    
    fun release() {
        unregisterHingeAngleSensor()
        scope.cancel()
    }
    
    // ========== DETECTION METHODS ==========
    
    private fun detectFoldableDevice() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        
        val foldableManufacturer = detectFoldableManufacturer(manufacturer)
        val foldableModel = detectFoldableModel(model, foldableManufacturer)
        val hingeOrientation = detectHingeOrientation(foldableModel)
        
        val isFoldable = foldableModel != FoldableModel.UNKNOWN ||
                        hasFoldableFeatures() ||
                        hasHingeAngleSensor()
        
        _foldState.update { current ->
            current.copy(
                isFoldable = isFoldable,
                manufacturer = foldableManufacturer,
                model = foldableModel,
                hingeOrientation = hingeOrientation,
                continuitySupported = supportsContinuity()
            )
        }
    }
    
    private fun detectFoldableManufacturer(manufacturer: String): FoldableManufacturer {
        return when {
            manufacturer.contains("samsung") -> FoldableManufacturer.SAMSUNG
            manufacturer.contains("google") -> FoldableManufacturer.GOOGLE
            manufacturer.contains("microsoft") -> FoldableManufacturer.MICROSOFT
            manufacturer.contains("oppo") -> FoldableManufacturer.OPPO
            manufacturer.contains("xiaomi") -> FoldableManufacturer.XIAOMI
            manufacturer.contains("huawei") -> FoldableManufacturer.HUAWEI
            manufacturer.contains("motorola") -> FoldableManufacturer.MOTOROLA
            manufacturer.contains("lg") -> FoldableManufacturer.LG
            manufacturer.contains("honor") -> FoldableManufacturer.HONOR
            manufacturer.contains("vivo") -> FoldableManufacturer.VIVO
            else -> FoldableManufacturer.UNKNOWN
        }
    }
    
    private fun detectFoldableModel(model: String, manufacturer: FoldableManufacturer): FoldableModel {
        return when (manufacturer) {
            FoldableManufacturer.SAMSUNG -> detectSamsungFoldable(model)
            FoldableManufacturer.GOOGLE -> detectGoogleFoldable(model)
            FoldableManufacturer.MICROSOFT -> detectMicrosoftFoldable(model)
            FoldableManufacturer.OPPO -> detectOppoFoldable(model)
            FoldableManufacturer.XIAOMI -> detectXiaomiFoldable(model)
            FoldableManufacturer.HUAWEI -> detectHuaweiFoldable(model)
            FoldableManufacturer.MOTOROLA -> detectMotorolaFoldable(model)
            FoldableManufacturer.LG -> detectLGFoldable(model)
            FoldableManufacturer.HONOR -> detectHonorFoldable(model)
            FoldableManufacturer.VIVO -> detectVivoFoldable(model)
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectSamsungFoldable(model: String): FoldableModel {
        return when {
            model.contains("sm-f900") || model.contains("galaxy fold") && !model.contains("z") -> FoldableModel.GALAXY_FOLD
            model.contains("sm-f916") || model.contains("z fold2") -> FoldableModel.GALAXY_Z_FOLD_2
            model.contains("sm-f926") || model.contains("z fold3") -> FoldableModel.GALAXY_Z_FOLD_3
            model.contains("sm-f936") || model.contains("z fold4") -> FoldableModel.GALAXY_Z_FOLD_4
            model.contains("sm-f946") || model.contains("z fold5") -> FoldableModel.GALAXY_Z_FOLD_5
            model.contains("sm-f700") || model.contains("z flip") && !model.contains("3") && !model.contains("4") && !model.contains("5") -> FoldableModel.GALAXY_Z_FLIP
            model.contains("sm-f711") || model.contains("z flip3") -> FoldableModel.GALAXY_Z_FLIP_3
            model.contains("sm-f721") || model.contains("z flip4") -> FoldableModel.GALAXY_Z_FLIP_4
            model.contains("sm-f731") || model.contains("z flip5") -> FoldableModel.GALAXY_Z_FLIP_5
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectGoogleFoldable(model: String): FoldableModel {
        return when {
            model.contains("pixel fold") || model.contains("felix") -> FoldableModel.PIXEL_FOLD
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectMicrosoftFoldable(model: String): FoldableModel {
        return when {
            model.contains("surface duo 2") -> FoldableModel.SURFACE_DUO_2
            model.contains("surface duo") -> FoldableModel.SURFACE_DUO
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectOppoFoldable(model: String): FoldableModel {
        return when {
            model.contains("find n3") -> FoldableModel.OPPO_FIND_N3
            model.contains("find n2 flip") -> FoldableModel.OPPO_FIND_N2_FLIP
            model.contains("find n2") -> FoldableModel.OPPO_FIND_N2
            model.contains("find n") -> FoldableModel.OPPO_FIND_N
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectXiaomiFoldable(model: String): FoldableModel {
        return when {
            model.contains("mix fold 3") -> FoldableModel.XIAOMI_MIX_FOLD_3
            model.contains("mix fold 2") -> FoldableModel.XIAOMI_MIX_FOLD_2
            model.contains("mix fold") -> FoldableModel.XIAOMI_MIX_FOLD
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectHuaweiFoldable(model: String): FoldableModel {
        return when {
            model.contains("mate x2") -> FoldableModel.HUAWEI_MATE_X2
            model.contains("mate xs") -> FoldableModel.HUAWEI_MATE_XS
            model.contains("mate x") -> FoldableModel.HUAWEI_MATE_X
            model.contains("p50 pocket") -> FoldableModel.HUAWEI_P50_POCKET
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectMotorolaFoldable(model: String): FoldableModel {
        return when {
            model.contains("razr 40 ultra") -> FoldableModel.MOTOROLA_RAZR_40_ULTRA
            model.contains("razr 40") -> FoldableModel.MOTOROLA_RAZR_40
            model.contains("razr 2022") || model.contains("razr 5g") -> FoldableModel.MOTOROLA_RAZR_2022
            model.contains("razr") -> FoldableModel.MOTOROLA_RAZR
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectLGFoldable(model: String): FoldableModel {
        return when {
            model.contains("wing") -> FoldableModel.LG_WING
            model.contains("v60") && model.contains("dual") -> FoldableModel.LG_V60_DUAL_SCREEN
            model.contains("g8x") && model.contains("dual") -> FoldableModel.LG_G8X_DUAL_SCREEN
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectHonorFoldable(model: String): FoldableModel {
        return when {
            model.contains("magic v2") -> FoldableModel.HONOR_MAGIC_V2
            model.contains("magic vs") -> FoldableModel.HONOR_MAGIC_VS
            model.contains("magic v") -> FoldableModel.HONOR_MAGIC_V
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectVivoFoldable(model: String): FoldableModel {
        return when {
            model.contains("x fold 2") || model.contains("x fold2") -> FoldableModel.VIVO_X_FOLD_2
            model.contains("x fold plus") || model.contains("x fold+") -> FoldableModel.VIVO_X_FOLD_PLUS
            model.contains("x fold") -> FoldableModel.VIVO_X_FOLD
            else -> FoldableModel.UNKNOWN
        }
    }
    
    private fun detectHingeOrientation(model: FoldableModel): HingeOrientation {
        return when (model) {
            FoldableModel.GALAXY_Z_FLIP,
            FoldableModel.GALAXY_Z_FLIP_3,
            FoldableModel.GALAXY_Z_FLIP_4,
            FoldableModel.GALAXY_Z_FLIP_5,
            FoldableModel.MOTOROLA_RAZR,
            FoldableModel.MOTOROLA_RAZR_2022,
            FoldableModel.MOTOROLA_RAZR_40,
            FoldableModel.MOTOROLA_RAZR_40_ULTRA,
            FoldableModel.HUAWEI_P50_POCKET,
            FoldableModel.OPPO_FIND_N2_FLIP -> HingeOrientation.HORIZONTAL
            
            FoldableModel.SURFACE_DUO,
            FoldableModel.SURFACE_DUO_2,
            FoldableModel.LG_V60_DUAL_SCREEN,
            FoldableModel.LG_G8X_DUAL_SCREEN -> HingeOrientation.DUAL_SCREEN
            
            FoldableModel.LG_WING -> HingeOrientation.SWIVEL
            
            else -> HingeOrientation.VERTICAL // Most foldables are book-style
        }
    }
    
    private fun hasFoldableFeatures(): Boolean {
        return context.packageManager.hasSystemFeature("com.samsung.feature.device_category_foldable") ||
               context.packageManager.hasSystemFeature("com.google.android.feature.FOLD") ||
               context.packageManager.hasSystemFeature("com.microsoft.device.display.displaymask") ||
               hasHingeAngleSensor()
    }
    
    private fun hasHingeAngleSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            sensorManager?.getSensorList(Sensor.TYPE_HINGE_ANGLE)?.isNotEmpty() == true
        } else {
            false
        }
    }
    
    private fun supportsContinuity(): Boolean {
        // App continuity is supported on most modern foldables
        return _foldState.value.isFoldable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
    
    // ========== HINGE ANGLE MONITORING ==========
    
    private fun registerHingeAngleSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hingeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
            hingeSensor?.let {
                sensorManager?.registerListener(
                    hingeAngleListener,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                Log.d(TAG, "Hinge angle sensor registered")
            }
        }
    }
    
    private fun unregisterHingeAngleSensor() {
        sensorManager?.unregisterListener(hingeAngleListener)
    }
    
    private fun updateHingeAngle(angle: Float) {
        stateDebounceJob?.cancel()
        stateDebounceJob = scope.launch {
            delay(STATE_UPDATE_DEBOUNCE_MS)
            
            val foldState = calculateFoldState(angle)
            val posture = calculatePosture(angle)
            val confidence = calculatePostureConfidence(angle, posture)
            
            _foldState.update { current ->
                current.copy(
                    hingeAngle = angle,
                    foldState = foldState,
                    posture = posture,
                    postureConfidence = confidence
                )
            }
            
            updateCreaseInfo(angle)
        }
    }
    
    private fun calculateFoldState(angle: Float): FoldState {
        return when {
            angle < 30f -> FoldState.CLOSED
            angle in 30f..150f -> FoldState.HALF_OPEN
            angle in 150f..180f -> FoldState.OPEN
            angle > 180f -> FoldState.FLIPPED
            else -> FoldState.UNKNOWN
        }
    }
    
    private fun calculatePosture(angle: Float): DevicePosture {
        return when {
            angle < 30f -> DevicePosture.CLOSED
            angle >= FLAT_POSTURE_MIN_ANGLE -> DevicePosture.FLAT
            angle in TABLETOP_MIN_ANGLE..TABLETOP_MAX_ANGLE -> DevicePosture.TABLETOP
            angle in 45f..75f -> DevicePosture.TENT
            angle in 115f..150f -> DevicePosture.BOOK
            angle > 180f -> DevicePosture.FLIPPED
            else -> DevicePosture.UNKNOWN
        }
    }
    
    private fun calculatePostureConfidence(angle: Float, posture: DevicePosture): Float {
        // Calculate confidence based on how close the angle is to ideal posture angles
        return when (posture) {
            DevicePosture.CLOSED -> if (angle < 10f) 1.0f else (30f - angle) / 30f
            DevicePosture.FLAT -> if (angle > 175f) 1.0f else (angle - 150f) / 30f
            DevicePosture.TABLETOP -> {
                val idealAngle = 95f // Middle of tabletop range
                1.0f - (kotlin.math.abs(angle - idealAngle) / 20f)
            }
            DevicePosture.TENT -> {
                val idealAngle = 60f // Middle of tent range
                1.0f - (kotlin.math.abs(angle - idealAngle) / 15f)
            }
            DevicePosture.BOOK -> {
                val idealAngle = 132.5f // Middle of book range
                1.0f - (kotlin.math.abs(angle - idealAngle) / 17.5f)
            }
            else -> 0.5f
        }.coerceIn(0f, 1f)
    }
    
    // ========== WINDOW LAYOUT OBSERVATION ==========
    
    private fun observeWindowLayoutChanges() {
        scope.launch {
            windowInfoTracker.windowLayoutInfo(context)
                .collect { layoutInfo ->
                    handleWindowLayoutChange(layoutInfo)
                }
        }
    }
    
    private fun handleWindowLayoutChange(layoutInfo: WindowLayoutInfo) {
        val foldingFeatures = layoutInfo.displayFeatures
            .filterIsInstance<FoldingFeature>()
        
        foldingFeatures.firstOrNull()?.let { feature ->
            updateCreaseInfoFromFeature(feature)
            updateFoldStateFromFeature(feature)
        }
    }
    
    private fun updateCreaseInfoFromFeature(feature: FoldingFeature) {
        val bounds = Rect(
            feature.bounds.left,
            feature.bounds.top,
            feature.bounds.right,
            feature.bounds.bottom
        )
        
        val orientation = if (feature.orientation == FoldingFeature.Orientation.HORIZONTAL) {
            CreaseOrientation.HORIZONTAL
        } else {
            CreaseOrientation.VERTICAL
        }
        
        val creaseInfo = CreaseInfo(
            bounds = bounds,
            width = if (orientation == CreaseOrientation.HORIZONTAL) {
                bounds.height()
            } else {
                bounds.width()
            },
            orientation = orientation,
            shouldAvoid = feature.isSeparating
        )
        
        _foldState.update { current ->
            current.copy(creaseInfo = creaseInfo)
        }
    }
    
    private fun updateFoldStateFromFeature(feature: FoldingFeature) {
        val state = when (feature.state) {
            FoldingFeature.State.FLAT -> FoldState.OPEN
            FoldingFeature.State.HALF_OPENED -> FoldState.HALF_OPEN
            else -> FoldState.UNKNOWN
        }
        
        if (state != FoldState.UNKNOWN) {
            _foldState.update { current ->
                current.copy(foldState = state)
            }
        }
    }
    
    private fun updateCreaseInfo(angle: Float) {
        // Estimate crease position based on device model and angle
        val model = _foldState.value.model
        val orientation = _foldState.value.hingeOrientation
        
        if (model == FoldableModel.UNKNOWN) return
        
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        val creaseInfo = when (orientation) {
            HingeOrientation.VERTICAL -> {
                // Book-style foldables (Galaxy Fold, Pixel Fold, etc.)
                val creaseX = screenWidth / 2
                CreaseInfo(
                    bounds = Rect(creaseX - 10, 0, creaseX + 10, screenHeight),
                    width = 20,
                    orientation = CreaseOrientation.VERTICAL,
                    shouldAvoid = angle < 170f
                )
            }
            HingeOrientation.HORIZONTAL -> {
                // Flip-style foldables (Galaxy Flip, Razr, etc.)
                val creaseY = screenHeight / 2
                CreaseInfo(
                    bounds = Rect(0, creaseY - 10, screenWidth, creaseY + 10),
                    width = 20,
                    orientation = CreaseOrientation.HORIZONTAL,
                    shouldAvoid = angle < 170f
                )
            }
            HingeOrientation.DUAL_SCREEN -> {
                // Surface Duo style - gap between screens
                val gapX = screenWidth / 2
                CreaseInfo(
                    bounds = Rect(gapX - 35, 0, gapX + 35, screenHeight),
                    width = 70, // Wider gap for dual screens
                    orientation = CreaseOrientation.VERTICAL,
                    shouldAvoid = true
                )
            }
            else -> null
        }
        
        creaseInfo?.let {
            _foldState.update { current ->
                current.copy(creaseInfo = it)
            }
        }
    }
    
    // ========== ORIENTATION STATE ==========
    
    @Suppress("DEPRECATION") // defaultDisplay deprecated in API 30, but no universal replacement for API 28+
    private fun updateOrientationState() {
        val rotation = windowManager.defaultDisplay?.rotation ?: Surface.ROTATION_0
        val rotationDegrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        
        val orientation = when (rotation) {
            Surface.ROTATION_0 -> Orientation.PORTRAIT
            Surface.ROTATION_90 -> Orientation.LANDSCAPE
            Surface.ROTATION_180 -> Orientation.REVERSE_PORTRAIT
            Surface.ROTATION_270 -> Orientation.REVERSE_LANDSCAPE
            else -> Orientation.PORTRAIT
        }
        
        val autoRotate = android.provider.Settings.System.getInt(
            context.contentResolver,
            android.provider.Settings.System.ACCELEROMETER_ROTATION,
            0
        ) == 1
        
        _orientationState.value = OrientationState(
            currentOrientation = orientation,
            deviceNaturalOrientation = getDeviceNaturalOrientation(),
            rotationDegrees = rotationDegrees,
            isAutoRotateEnabled = autoRotate,
            isRotationLocked = !autoRotate,
            supportsReverseOrientation = supportsReverseOrientation()
        )
    }
    
    @Suppress("DEPRECATION") // defaultDisplay deprecated in API 30, but no universal replacement for API 28+
    private fun getDeviceNaturalOrientation(): Orientation {
        val config = context.resources.configuration
        val rotation = windowManager.defaultDisplay?.rotation ?: Surface.ROTATION_0
        
        return if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                   config.orientation == Configuration.ORIENTATION_PORTRAIT ||
                   (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                   config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Orientation.PORTRAIT
        } else {
            Orientation.LANDSCAPE
        }
    }
    
    private fun supportsReverseOrientation(): Boolean {
        // Most modern devices support 180-degree rotation
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
    
    // ========== MULTI-DISPLAY STATE ==========
    
    private fun updateMultiDisplayState() {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
        val displays = displayManager?.displays ?: emptyArray()
        
        val displayInfos = displays.map { display ->
            DisplayInfo(
                displayId = display.displayId,
                name = display.name,
                isInternal = display.displayId == android.view.Display.DEFAULT_DISPLAY,
                bounds = Rect(), // Would need more complex calculation
                rotation = display.rotation,
                refreshRate = display.refreshRate,
                density = context.resources.displayMetrics.density,
                isHDR = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) display.isHdr else false,
                isWideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) display.isWideColorGamut else false,
                cutout = null, // Would need display cutout info
                roundedCorners = null, // Would need corner radius info
                isActive = display.state == android.view.Display.STATE_ON,
                isTouchEnabled = true // Assume touch for most displays
            )
        }
        
        val isDualScreen = _foldState.value.model in listOf(
            FoldableModel.SURFACE_DUO,
            FoldableModel.SURFACE_DUO_2,
            FoldableModel.LG_V60_DUAL_SCREEN,
            FoldableModel.LG_G8X_DUAL_SCREEN
        )
        
        _multiDisplayState.value = MultiDisplayState(
            displayCount = displays.size,
            displays = displayInfos,
            primaryDisplayId = android.view.Display.DEFAULT_DISPLAY,
            displayArrangement = if (displays.size > 1) DisplayArrangement.EXTENDED else DisplayArrangement.SINGLE,
            isDualScreenDevice = isDualScreen,
            hasExternalDisplay = displays.size > 1 && !isDualScreen
        )
    }
    
    // ========== PUBLIC API ==========
    
    fun isFoldable(): Boolean = _foldState.value.isFoldable
    
    fun getFoldState(): FoldState = _foldState.value.foldState
    
    fun getHingeAngle(): Float = _foldState.value.hingeAngle
    
    fun getPosture(): DevicePosture = _foldState.value.posture
    
    fun getCreaseInfo(): CreaseInfo? = _foldState.value.creaseInfo
    
    fun isTabletopMode(): Boolean = _foldState.value.posture == DevicePosture.TABLETOP
    
    fun isTentMode(): Boolean = _foldState.value.posture == DevicePosture.TENT
    
    fun isFlexMode(): Boolean = _foldState.value.foldState == FoldState.HALF_OPEN
    
    fun shouldAvoidCrease(): Boolean = _foldState.value.creaseInfo?.shouldAvoid == true
    
    fun getActiveScreens(): List<ScreenIdentifier> = _foldState.value.activeScreens
    
    fun isSpanning(): Boolean = _multiDisplayState.value.isSpannedAcrossScreens
    
    fun getDisplayArrangement(): DisplayArrangement = _multiDisplayState.value.displayArrangement
}
