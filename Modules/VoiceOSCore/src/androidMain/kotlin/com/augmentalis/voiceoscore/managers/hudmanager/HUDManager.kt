/**
 * HUDManager.kt
 * Path: /CodeImport/HUDManager/src/main/java/com/augmentalis/hudmanager/HUDManager.kt
 * 
 * Created: 2025-01-23
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Main HUD manager for smart glasses and AR interfaces
 * Follows VOS4 direct implementation principles with zero abstraction overhead
 */

package com.augmentalis.voiceoscore.managers.hudmanager

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.augmentalis.voiceoscore.managers.hudmanager.core.*
import com.augmentalis.voiceoscore.managers.hudmanager.core.Environment
import com.augmentalis.voiceoscore.managers.hudmanager.spatial.*
import com.augmentalis.voiceoscore.managers.hudmanager.models.*
import com.augmentalis.devicemanager.imu.IMUData
import com.augmentalis.devicemanager.imu.IMUManager
import com.augmentalis.voiceoscore.managers.hudmanager.stubs.DatabaseModule
import com.augmentalis.voiceoscore.managers.hudmanager.stubs.VOSAccessibilitySvc
import com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDIntent
import com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDRenderer
import com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDSystem
import com.augmentalis.voiceoscore.managers.hudmanager.stubs.voiceui
import com.augmentalis.voiceoscore.managers.hudmanager.settings.HUDSettings
import com.augmentalis.voiceoscore.managers.hudmanager.settings.HUDSettingsManager
import com.augmentalis.voiceoscore.managers.hudmanager.settings.HUDDisplayMode
import com.augmentalis.localization.Localizer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Central HUD management system following VOS4 direct implementation pattern
 * 
 * Key Features:
 * - Spatial voice command visualization
 * - Gaze + Voice fusion interaction
 * - Environmental context awareness
 * - Accessibility-first design
 * - Zero abstraction overhead
 */
class HUDManager constructor(
    private val context: Context
) {
    
    companion object {
        @Volatile
        private var INSTANCE: HUDManager? = null
        
        fun getInstance(context: Context): HUDManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HUDManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Core HUD subsystems (direct instantiation - VOS4 pattern)
    val spatialRenderer = SpatialRenderer(context)
    val voiceIndicatorSystem = VoiceIndicatorSystem(context)
    
    // Use VoiceUI's HUDRenderer instead of duplicating
    private var voiceUIRenderer: HUDRenderer? = null
    private var voiceUISystem: HUDSystem? = null
    
    val gazeTracker = com.augmentalis.voiceoscore.managers.hudmanager.spatial.GazeTracker(context)
    val contextManager = com.augmentalis.voiceoscore.managers.hudmanager.core.ContextManager(context)
    val enhancer = com.augmentalis.voiceoscore.managers.hudmanager.accessibility.Enhancer(context)
    
    // VOS4 system integrations (direct access - zero overhead)
    private val imuManager = IMUManager.getInstance(context)
    private val dataManager = DatabaseModule.getInstance(context)
    private val accessibilityService = VOSAccessibilitySvc.getInstance()
    private val localizer = Localizer.getInstance(context).also { it.initialize() }
    
    // Settings management
    private val settingsManager = HUDSettingsManager.getInstance(context)
    
    // Speech recognition integration (direct access to existing system)
    private var speechRecognitionModule: Any? = null // Will connect to existing SpeechRecognition module
    
    // Coroutine management
    private val hudScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // HUD state management
    private val _isActive = mutableStateOf(false)
    val isActive: State<Boolean> = _isActive
    
    private val _currentMode = mutableStateOf(HUDMode.STANDARD)
    val currentMode: State<HUDMode> = _currentMode
    
    // Consumer tracking for multi-app support
    private val activeConsumers = mutableSetOf<String>()
    
    // LiveData properties for testing and external observation
    private val _hudState = MutableLiveData<HUDState>()
    val hudState: LiveData<HUDState> = _hudState
    
    private val _renderingStats = MutableLiveData<RenderingStats>()
    val renderingStats: LiveData<RenderingStats> = _renderingStats
    
    private val _spatialData = MutableLiveData<SpatialData>()
    val spatialData: LiveData<SpatialData> = _spatialData
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // Element management
    private val hudElements = mutableMapOf<String, HUDElement>()
    private val collisionList = mutableListOf<ElementCollision>()
    
    /**
     * Initialize HUD system with configuration
     */
    fun initialize(config: HUDConfig = HUDConfig.default()): Boolean {
        return try {
            // Initialize VoiceUI HUD components
            initializeVoiceUIIntegration()
            
            // Initialize core systems
            spatialRenderer.initialize()
            voiceIndicatorSystem.initialize()
            gazeTracker.initialize()
            contextManager.initialize()
            enhancer.initialize()
            
            // Start IMU tracking for head movement
            imuManager.startIMUTracking("HUDManager")
            
            // Begin context monitoring
            startContextMonitoring()
            
            // Start observing settings changes
            observeSettings()
            
            // Update initial state
            updateHUDState(HUDState(
                mode = config.mode,
                isTracking = config.trackingEnabled,
                activeElements = emptyList(),
                opacity = 1.0f,
                isInitialized = true
            ))
            
            true
        } catch (e: Exception) {
            _errorMessage.value = "Initialization failed: ${e.message}"
            false
        }
    }
    
    /**
     * Initialize HUD system (legacy method)
     */
    fun initialize(): Boolean = initialize(HUDConfig.default())
    
    /**
     * Initialize integration with VoiceUI HUD system
     */
    private fun initializeVoiceUIIntegration() {
        // Create intent to initialize system HUD
        val intent = Intent(HUDIntent.ACTION_SHOW_HUD).apply {
            setPackage("com.augmentalis.voiceos")
        }
        context.sendBroadcast(intent)
        
        // Initialize VoiceUI HUD components
        voiceUIRenderer = HUDRenderer
        
        // Note: VoiceUISystem would be obtained via service binding in production
    }
    
    /**
     * Register a consumer application for HUD services
     */
    fun registerConsumer(consumerId: String): Boolean {
        synchronized(activeConsumers) {
            activeConsumers.add(consumerId)
        }
        // Compose mutableStateOf writes must happen on the Main thread.
        // hudScope is Dispatchers.Main, so this is safe from any caller thread.
        hudScope.launch {
            if (!_isActive.value) {
                _isActive.value = true
                startHUDServices()
            }
        }
        return true
    }

    /**
     * Unregister a consumer application
     */
    fun unregisterConsumer(consumerId: String) {
        synchronized(activeConsumers) {
            activeConsumers.remove(consumerId)
        }
        // Compose mutableStateOf writes must happen on the Main thread.
        hudScope.launch {
            if (activeConsumers.isEmpty()) {
                _isActive.value = false
                stopHUDServices()
            }
        }
    }
    
    /**
     * Display spatial voice command indicators with localization
     */
    fun showVoiceCommands(commands: List<VoiceCommand>, context: UIContext) {
        hudScope.launch {
            // Localize command labels if needed
            val localizedCommands = commands.map { cmd ->
                if (cmd.translationKey != null) {
                    cmd.copy(label = localizer.translate(cmd.translationKey))
                } else {
                    cmd
                }
            }
            voiceIndicatorSystem.displayCommands(localizedCommands, context)
        }
    }
    
    /**
     * Show voice recognition confidence
     */
    fun showSpeechConfidence(confidence: Float) {
        hudScope.launch {
            voiceIndicatorSystem.displayConfidence(confidence)
        }
    }
    
    /**
     * Enable gaze + voice fusion mode
     */
    fun enableGazeVoiceFusion(enabled: Boolean) {
        hudScope.launch {
            if (enabled) {
                gazeTracker.startTracking()
                voiceIndicatorSystem.enableGazeIntegration()
            } else {
                gazeTracker.stopTracking()
                voiceIndicatorSystem.disableGazeIntegration()
            }
        }
    }
    
    /**
     * Set HUD mode for different environments
     */
    fun setHUDMode(mode: HUDMode) {
        hudScope.launch {
            _currentMode.value = mode
            contextManager.applyModeSettings(mode)
            enhancer.adaptToMode(mode)
            // Update rendering mode via VoiceUI
            val voiceUIMode = when (mode) {
                HUDMode.MEETING -> voiceui.hud.HUDMode.MEETING
                HUDMode.DRIVING -> voiceui.hud.HUDMode.DRIVING
                HUDMode.WORKSHOP -> voiceui.hud.HUDMode.WORKSHOP
                HUDMode.ACCESSIBILITY -> voiceui.hud.HUDMode.ACCESSIBILITY
                HUDMode.GAMING -> voiceui.hud.HUDMode.GAMING
                HUDMode.ENTERTAINMENT -> voiceui.hud.HUDMode.ENTERTAINMENT
                else -> voiceui.hud.HUDMode.STANDARD
            }
            voiceUIRenderer?.updateModeRendering(voiceUIMode)
        }
    }
    
    /**
     * Display notification in spatial UI with localization support
     */
    fun showSpatialNotification(notification: HUDNotification) {
        hudScope.launch {
            // Localize notification message if it's a translation key
            val localizedNotification = if (notification.isTranslationKey) {
                notification.copy(
                    message = localizer.translate(notification.message),
                    languageCode = localizer.getCurrentLanguage()
                )
            } else {
                notification
            }
            spatialRenderer.renderNotification(localizedNotification)
        }
    }
    
    /**
     * Display localized notification using translation key
     */
    fun showLocalizedNotification(
        translationKey: String,
        position: SpatialPosition = SpatialPosition(0f, 0f, -2f),
        priority: com.augmentalis.voiceoscore.managers.hudmanager.spatial.NotificationPriority = com.augmentalis.voiceoscore.managers.hudmanager.spatial.NotificationPriority.NORMAL,
        vararg args: Any
    ) {
        hudScope.launch {
            val notification = HUDNotification(
                message = localizer.translate(translationKey, *args),
                position = position,
                priority = priority,
                languageCode = localizer.getCurrentLanguage(),
                isTranslationKey = false
            )
            spatialRenderer.renderNotification(notification)
        }
    }
    
    /**
     * Create floating control panel
     */
    fun createControlPanel(
        actions: List<HUDAction>,
        position: SpatialPosition,
        style: PanelStyle = PanelStyle.STANDARD
    ) {
        hudScope.launch {
            spatialRenderer.createControlPanel(actions, position, style)
        }
    }
    
    /**
     * Get current gaze target for voice commands
     */
    suspend fun getCurrentGazeTarget(): com.augmentalis.voiceoscore.managers.hudmanager.models.GazeTarget? {
        return gazeTracker.getCurrentTarget()
    }
    
    /**
     * Get available voice commands for current context
     */
    suspend fun getContextualCommands(): List<VoiceCommand> {
        return contextManager.getAvailableCommands()
    }
    
    private fun startHUDServices() {
        hudScope.launch {
            // Start real-time HUD rendering
            startHUDRendering()
            
            // Begin IMU head tracking integration
            startIMUIntegration()
            
            // Enable contextual awareness
            startContextualAdaptation()
        }
    }
    
    private fun stopHUDServices() {
        hudScope.launch {
            spatialRenderer.clearAll()
            voiceIndicatorSystem.clearAll()
            gazeTracker.stopTracking()
        }
    }
    
    private suspend fun startHUDRendering() {
        // Use VoiceUI renderer instead of local one
        voiceUIRenderer?.startRendering(HUDRenderer.TARGET_FPS_HIGH)
    }
    
    private fun startIMUIntegration() {
        hudScope.launch {
            imuManager.orientationFlow.collect { orientationData ->
                // Update spatial rendering based on head movement
                spatialRenderer.updateHeadOrientation(orientationData)
                
                // Adjust HUD elements to maintain optimal positioning via VoiceUI
                voiceUIRenderer?.adjustForHeadMovement(
                    voiceui.hud.OrientationData(
                        yaw = 0f, // Would extract from orientationData
                        pitch = 0f,
                        roll = 0f
                    )
                )
            }
        }
    }
    
    private suspend fun startContextualAdaptation() {
        contextManager.environmentFlow.collect { environment ->
            // Adapt HUD to current environment
            when (environment) {
                Environment.MEETING -> setHUDMode(HUDMode.MEETING)
                Environment.DRIVING -> setHUDMode(HUDMode.DRIVING)
                Environment.WORKSHOP -> setHUDMode(HUDMode.WORKSHOP)
                Environment.HOME -> setHUDMode(HUDMode.STANDARD)
                else -> setHUDMode(HUDMode.STANDARD)
            }
        }
    }
    
    private fun startContextMonitoring() {
        hudScope.launch {
            contextManager.startEnvironmentDetection()
        }
    }
    
    /**
     * Start HUD display
     */
    @Suppress("UNUSED_PARAMETER")
    fun startHUD(consumerId: String = "default") {
        hudScope.launch {
            _isActive.value = true
            startHUDServices()
        }
    }
    
    /**
     * Stop HUD display
     */
    @Suppress("UNUSED_PARAMETER")
    fun stopHUD(reason: String = "manual") {
        hudScope.launch {
            _isActive.value = false
            stopHUDServices()
        }
    }
    
    /**
     * Enable HUD tracking
     */
    fun enableTracking(): Boolean {
        return try {
            hudScope.launch {
                gazeTracker.startTracking()
            }
            imuManager.startIMUTracking("HUDManager")
            updateHUDState(getCurrentHUDState().copy(isTracking = true))
            true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to enable tracking: ${e.message}"
            false
        }
    }
    
    /**
     * Disable HUD tracking
     */
    fun disableTracking(): Boolean {
        return try {
            gazeTracker.stopTracking()
            imuManager.stopIMUTracking("HUDManager")
            updateHUDState(getCurrentHUDState().copy(isTracking = false))
            true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to disable tracking: ${e.message}"
            false
        }
    }
    
    /**
     * Add HUD element
     */
    fun addElement(element: HUDElement): Boolean {
        return try {
            // Validate element
            if (element.id.isEmpty()) {
                _errorMessage.value = "Invalid element: ID cannot be empty"
                return false
            }
            
            hudElements[element.id] = element
            updateHUDState(getCurrentHUDState().copy(
                activeElements = hudElements.values.toList()
            ))
            true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to add element: ${e.message}"
            false
        }
    }
    
    /**
     * Remove HUD element
     */
    fun removeElement(elementId: String): Boolean {
        return try {
            hudElements.remove(elementId)
            updateHUDState(getCurrentHUDState().copy(
                activeElements = hudElements.values.toList()
            ))
            true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to remove element: ${e.message}"
            false
        }
    }
    
    /**
     * Update element position
     */
    fun updateElementPosition(elementId: String, newPosition: Vector3D): Boolean {
        return try {
            val element = hudElements[elementId]
            if (element != null) {
                hudElements[elementId] = element.copy(position = newPosition)
                updateHUDState(getCurrentHUDState().copy(
                    activeElements = hudElements.values.toList()
                ))
                true
            } else {
                _errorMessage.value = "Element not found: $elementId"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update element position: ${e.message}"
            false
        }
    }
    
    /**
     * Calibrate spatial mapping
     */
    @Suppress("UNUSED_PARAMETER")
    fun calibrateSpatialMapping(calibrationPoints: List<CalibrationPoint>): Boolean {
        return try {
            // Perform calibration logic
            updateSpatialData(getCurrentSpatialData().copy(
                isCalibrated = true,
                trackingQuality = 0.95f
            ))
            true
        } catch (e: Exception) {
            _errorMessage.value = "Calibration failed: ${e.message}"
            false
        }
    }
    
    /**
     * Handle rendering frame
     */
    fun onRenderFrame() {
        try {
            val currentStats = getCurrentRenderingStats()
            updateRenderingStats(currentStats.copy(
                renderedElements = hudElements.size,
                frameTime = System.currentTimeMillis(),
                averageFPS = calculateAverageFPS()
            ))
        } catch (e: Exception) {
            _errorMessage.value = "Rendering frame failed: ${e.message}"
        }
    }
    
    /**
     * Switch HUD mode
     */
    fun switchMode(mode: HUDMode): Boolean {
        return try {
            _currentMode.value = mode
            updateHUDState(getCurrentHUDState().copy(mode = mode))
            true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to switch mode: ${e.message}"
            false
        }
    }
    
    /**
     * Set HUD opacity
     */
    fun setOpacity(opacity: Float) {
        updateHUDState(getCurrentHUDState().copy(opacity = opacity))
    }
    
    /**
     * Detect element collisions
     */
    fun detectCollisions(): List<ElementCollision> {
        collisionList.clear()
        val elements = hudElements.values.toList()
        
        for (i in elements.indices) {
            for (j in i + 1 until elements.size) {
                val element1 = elements[i]
                val element2 = elements[j]
                
                if (elementsOverlap(element1, element2)) {
                    collisionList.add(
                        ElementCollision(
                            element1 = element1,
                            element2 = element2,
                            overlapArea = calculateOverlapArea(element1, element2),
                            severity = CollisionSeverity.MODERATE
                        )
                    )
                }
            }
        }
        
        return collisionList.toList()
    }
    
    /**
     * Get elements sorted by depth
     */
    fun getElementsSortedByDepth(): List<HUDElement> {
        return hudElements.values.sortedBy { it.position.z }
    }
    
    /**
     * Clean up HUD system
     */
    fun cleanup() {
        hudElements.clear()
        collisionList.clear()
        updateHUDState(HUDState(
            mode = HUDMode.DISABLED,
            isTracking = false,
            activeElements = emptyList(),
            opacity = 1.0f,
            isInitialized = false
        ))
    }
    
    // Helper methods
    
    private fun updateHUDState(state: HUDState) {
        _hudState.value = state
    }
    
    private fun updateRenderingStats(stats: RenderingStats) {
        _renderingStats.value = stats
    }
    
    private fun updateSpatialData(data: SpatialData) {
        _spatialData.value = data
    }
    
    private fun getCurrentHUDState(): HUDState {
        return _hudState.value ?: HUDState()
    }
    
    private fun getCurrentRenderingStats(): RenderingStats {
        return _renderingStats.value ?: RenderingStats()
    }
    
    private fun getCurrentSpatialData(): SpatialData {
        return _spatialData.value ?: SpatialData()
    }
    
    private fun calculateAverageFPS(): Float {
        // Simplified FPS calculation
        return 60.0f
    }
    
    private fun elementsOverlap(element1: HUDElement, element2: HUDElement): Boolean {
        val bounds1 = element1.bounds ?: com.augmentalis.voiceoscore.managers.hudmanager.models.ElementBounds(0.1f, 0.1f)
        val bounds2 = element2.bounds ?: com.augmentalis.voiceoscore.managers.hudmanager.models.ElementBounds(0.1f, 0.1f)
        
        val distance = kotlin.math.sqrt(
            (element1.position.x - element2.position.x) * (element1.position.x - element2.position.x) +
            (element1.position.y - element2.position.y) * (element1.position.y - element2.position.y)
        )
        
        return distance < (bounds1.width + bounds2.width) / 2
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun calculateOverlapArea(element1: HUDElement, element2: HUDElement): Float {
        // Simplified overlap calculation
        return 0.1f
    }
    
    /**
     * Cleanup HUD resources
     */
    fun dispose() {
        hudScope.cancel()
        imuManager.stopIMUTracking("HUDManager")
        spatialRenderer.dispose()
        voiceIndicatorSystem.dispose()
        gazeTracker.dispose()
        contextManager.dispose()
        enhancer.dispose()
        // voiceUIRenderer cleanup handled by stub
    }
    
    /**
     * Observe settings changes and apply them
     */
    private fun observeSettings() {
        hudScope.launch {
            settingsManager.settings.collect { settings ->
                applySettings(settings)
            }
        }
    }
    
    /**
     * Apply HUD settings
     */
    private fun applySettings(settings: HUDSettings) {
        // Check if HUD should be enabled/disabled
        if (!settings.hudEnabled && _isActive.value) {
            stopHUD("settings_disabled")
        } else if (settings.hudEnabled && !_isActive.value && activeConsumers.isNotEmpty()) {
            startHUD(activeConsumers.first())
        }
        
        // Apply display mode
        when (settings.displayMode) {
            HUDDisplayMode.OFF -> stopHUD("display_off")
            HUDDisplayMode.MINIMAL -> applyMinimalMode(settings)
            HUDDisplayMode.CONTEXTUAL -> applyContextualMode(settings)
            HUDDisplayMode.FULL -> applyFullMode(settings)
            HUDDisplayMode.CUSTOM -> applyCustomMode(settings)
            HUDDisplayMode.DRIVING -> applyDrivingMode(settings)
            HUDDisplayMode.WORK -> applyWorkMode(settings)
            HUDDisplayMode.FITNESS -> applyFitnessMode(settings)
            HUDDisplayMode.PRIVACY -> applyPrivacyMode(settings)
        }
        
        // Apply visual settings
        spatialRenderer.setTransparency(settings.visual.transparency)
        spatialRenderer.setBrightness(settings.visual.brightness)
        spatialRenderer.setContrast(settings.visual.contrast)
        
        // Apply positioning
        spatialRenderer.setPosition(
            distance = settings.positioning.hudDistance,
            verticalOffset = settings.positioning.verticalOffset,
            horizontalOffset = settings.positioning.horizontalOffset
        )
        
        // Apply performance settings
        setTargetFps(settings.performance.targetFps)
        
        // Apply privacy settings
        if (settings.privacy.hideInPublic) {
            contextManager.enablePublicSpaceDetection()
        }
    }
    
    /**
     * Apply minimal display mode
     */
    @Suppress("UNUSED_PARAMETER")
    private fun applyMinimalMode(settings: HUDSettings) {
        spatialRenderer.hideAllElements()
        if (settings.displayElements.batteryStatus) {
            spatialRenderer.showElement("battery")
        }
        if (settings.displayElements.time) {
            spatialRenderer.showElement("time")
        }
    }
    
    /**
     * Apply contextual display mode
     */
    @Suppress("UNUSED_PARAMETER")
    private fun applyContextualMode(settings: HUDSettings) {
        spatialRenderer.setContextualMode(true)
        spatialRenderer.applyDisplayElements(settings.displayElements)
    }
    
    /**
     * Apply full display mode
     */
    @Suppress("UNUSED_PARAMETER")
    private fun applyFullMode(settings: HUDSettings) {
        spatialRenderer.showAllElements()
        spatialRenderer.applyDisplayElements(settings.displayElements)
    }
    
    /**
     * Apply custom display mode
     */
    @Suppress("UNUSED_PARAMETER")
    private fun applyCustomMode(settings: HUDSettings) {
        spatialRenderer.hideAllElements()
        spatialRenderer.applyDisplayElements(settings.displayElements)
    }
    
    /**
     * Apply driving display mode
     */
    @Suppress("UNUSED_PARAMETER")
    private fun applyDrivingMode(settings: HUDSettings) {
        _currentMode.value = HUDMode.DRIVING
        spatialRenderer.setDrivingMode(true)
        setLargeIndicators(true)
    }
    
    /**
     * Apply work display mode
     */
    @Suppress("UNUSED_PARAMETER")
    private fun applyWorkMode(settings: HUDSettings) {
        _currentMode.value = HUDMode.WORKSHOP
        spatialRenderer.setWorkMode(true)
        enhancer.enableHighContrast()
    }
    
    /**
     * Apply fitness display mode
     */
    @Suppress("UNUSED_PARAMETER")
    private fun applyFitnessMode(settings: HUDSettings) {
        spatialRenderer.setFitnessMode(true)
        spatialRenderer.showElement("heartRate")
        spatialRenderer.showElement("steps")
    }
    
    /**
     * Apply privacy display mode
     */
    @Suppress("UNUSED_PARAMETER")
    private fun applyPrivacyMode(settings: HUDSettings) {
        spatialRenderer.setPrivacyMode(true)
        spatialRenderer.blurSensitiveContent(true)
    }
    
    /**
     * Handle voice commands for HUD
     */
    fun handleVoiceCommand(command: String): Boolean {
        // First try settings commands
        if (settingsManager.handleVoiceCommand(command)) {
            return true
        }
        
        // Then handle HUD-specific commands
        val normalizedCommand = command.lowercase().trim()
        
        return when {
            "show hud" in normalizedCommand -> {
                startHUD("voice_command")
                true
            }
            "hide hud" in normalizedCommand -> {
                stopHUD("voice_command")
                true
            }
            "center display" in normalizedCommand -> {
                spatialRenderer.centerDisplay()
                true
            }
            "reset position" in normalizedCommand -> {
                spatialRenderer.resetPosition()
                true
            }
            else -> false
        }
    }
    
    /**
     * Get current settings
     */
    fun getCurrentSettings(): HUDSettings {
        return settingsManager.currentSettings
    }
    
    /**
     * Update settings
     */
    fun updateSettings(block: HUDSettings.() -> HUDSettings) {
        settingsManager.updateSettings(block)
    }
    
    /**
     * Set target FPS for HUD rendering
     */
    @Suppress("UNUSED_PARAMETER")
    private fun setTargetFps(fps: Int) {
        // Apply FPS settings to voice UI renderer
        voiceUIRenderer?.let { _ ->
            // This is a stub implementation - would need actual renderer method
            // In a real implementation, this would call the renderer's setTargetFps method
        }
    }
    
    /**
     * Set large indicators for better visibility
     */
    private fun setLargeIndicators(enabled: Boolean) {
        // Configure voice indicator system for large indicators
        voiceIndicatorSystem.setLargeMode(enabled)
    }
}

/**
 * HUD operation modes
 */
enum class HUDMode {
    DISABLED,       // HUD is disabled
    STANDARD,       // Normal operation
    MEETING,        // Silent, meeting-focused
    DRIVING,        // Voice-only, navigation-focused
    WORKSHOP,       // Hands-free, safety-focused
    ACCESSIBILITY,  // Enhanced accessibility features
    GAMING,         // Gaming optimized
    ENTERTAINMENT   // Media consumption optimized
}

