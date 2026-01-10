package com.augmentalis.voiceoscoreng.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.VoiceOSCoreNGApplication
import com.augmentalis.voiceoscoreng.VoiceOSCoreNG
import com.augmentalis.voiceoscoreng.createForAndroid
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.common.CommandGenerator
import com.augmentalis.voiceoscoreng.common.CommandRegistry
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode
import com.augmentalis.voiceoscoreng.functions.HashUtils
import com.augmentalis.voiceoscoreng.handlers.ServiceConfiguration
import com.augmentalis.voiceoscoreng.persistence.ICommandPersistence
import com.augmentalis.database.dto.ScrapedAppDTO
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.augmentalis.voiceoscoreng.persistence.ScreenHashRepository
import com.augmentalis.voiceoscoreng.persistence.ScreenHashRepositoryImpl
import com.augmentalis.voiceoscoreng.persistence.ScreenInfo
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "VoiceOSA11yService"

/** Debounce delay for screen change events (ms) */
private const val SCREEN_CHANGE_DEBOUNCE_MS = 300L

/**
 * Accessibility Service for VoiceOSCoreNG testing.
 *
 * Provides real-time exploration of apps on the device,
 * extracting UI elements and processing them through the
 * VoiceOSCoreNG library for VUID generation, deduplication,
 * hierarchy tracking, and command generation.
 */
class VoiceOSAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Shared command registry - single source of truth.
     * Passed to VoiceOSCoreNG so both service and ActionCoordinator use the same instance.
     * This allows direct synchronous access without async wrappers.
     */
    private val commandRegistry = CommandRegistry()

    /**
     * Screen hash repository for caching known screens.
     * Avoids re-scanning screens that have already been processed.
     */
    private val screenHashRepository: ScreenHashRepository = ScreenHashRepositoryImpl()

    /**
     * Whether continuous scanning is enabled.
     * When true, screens are automatically scanned on navigation.
     */
    private val continuousScanningEnabled = AtomicBoolean(true)

    /**
     * Last screen change timestamp for debouncing.
     */
    private val lastScreenChangeTime = AtomicLong(0L)

    /**
     * Current package name for tracking.
     */
    @Volatile
    private var currentPackageName: String? = null

    /**
     * Current screen hash for comparison.
     */
    @Volatile
    private var currentScreenHash: String? = null

    /** Command persistence for saving to SQLDelight database */
    private val commandPersistence: ICommandPersistence by lazy {
        VoiceOSCoreNGApplication.getInstance(applicationContext).commandPersistence
    }

    /** Scraped app repository - for FK integrity (must insert before elements/commands) */
    private val scrapedAppRepository: IScrapedAppRepository by lazy {
        VoiceOSCoreNGApplication.getInstance(applicationContext).scrapedAppRepository
    }

    /** Scraped element repository - for FK integrity (must insert before commands) */
    private val scrapedElementRepository: IScrapedElementRepository by lazy {
        VoiceOSCoreNGApplication.getInstance(applicationContext).scrapedElementRepository
    }

    /** VoiceOSCoreNG facade for voice command processing */
    private var voiceOSCore: VoiceOSCoreNG? = null

    companion object {
        private var instance: VoiceOSAccessibilityService? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _explorationResults = MutableStateFlow<ExplorationResult?>(null)
        val explorationResults: StateFlow<ExplorationResult?> = _explorationResults.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

        fun getInstance(): VoiceOSAccessibilityService? = instance

        fun exploreCurrentApp() {
            instance?.performExploration()
        }

        fun exploreAllApps() {
            instance?.performFullExploration()
        }

        /**
         * Get all currently registered dynamic commands.
         * Direct access to shared registry (synchronous, no async wrapper needed).
         */
        fun getCurrentCommands(): List<QuantizedCommand> {
            return instance?.commandRegistry?.all() ?: emptyList()
        }

        /**
         * Get count of dynamic commands for current screen.
         */
        fun getDynamicCommandCount(): Int {
            return instance?.commandRegistry?.size ?: 0
        }

        /**
         * Start voice listening.
         */
        fun startListening() {
            instance?.serviceScope?.launch {
                instance?.voiceOSCore?.startListening()
            }
        }

        /**
         * Stop voice listening.
         */
        fun stopListening() {
            instance?.serviceScope?.launch {
                instance?.voiceOSCore?.stopListening()
            }
        }

        /**
         * Check if voice engine is listening.
         */
        fun isListening(): Boolean {
            return instance?.voiceOSCore?.state?.value?.let { state ->
                state is com.augmentalis.voiceoscoreng.handlers.ServiceState.Listening
            } ?: false
        }

        // ===== Continuous Monitoring Controls =====

        private val _isContinuousMonitoring = MutableStateFlow(true)
        val isContinuousMonitoring: StateFlow<Boolean> = _isContinuousMonitoring.asStateFlow()

        private val _currentScreenInfo = MutableStateFlow<ScreenInfo?>(null)
        val currentScreenInfo: StateFlow<ScreenInfo?> = _currentScreenInfo.asStateFlow()

        // ===== Numbers Overlay for Voice Commands =====

        /**
         * Numbers overlay mode:
         * - ON: Always show numbers on all clickable elements
         * - OFF: Never show numbers
         * - AUTO: Show numbers only when there are list items (emails, messages, etc.)
         */
        enum class NumbersOverlayMode {
            ON,    // Always show
            OFF,   // Never show
            AUTO   // Show only for lists/duplicates
        }

        /**
         * Data for displaying numbered badges on screen elements.
         * Used by the numbers overlay to show which elements can be selected by number.
         * Example: User says "first" or "1" to click element with number=1
         */
        data class NumberOverlayItem(
            val number: Int,           // 1-based display number (matches "first", "second", etc.)
            val label: String,         // Short label for display (e.g., sender name)
            val left: Int,             // Element bounds
            val top: Int,
            val right: Int,
            val bottom: Int,
            val vuid: String           // Target VUID for executing action
        )

        private val _numberedOverlayItems = MutableStateFlow<List<NumberOverlayItem>>(emptyList())
        val numberedOverlayItems: StateFlow<List<NumberOverlayItem>> = _numberedOverlayItems.asStateFlow()

        private val _numbersOverlayMode = MutableStateFlow(NumbersOverlayMode.AUTO)
        val numbersOverlayMode: StateFlow<NumbersOverlayMode> = _numbersOverlayMode.asStateFlow()

        // Computed: should we show numbers based on mode and items?
        val showNumbersOverlay: StateFlow<Boolean> = _numbersOverlayMode.let { modeFlow ->
            // This is a simplified reactive pattern - in production would use combine()
            MutableStateFlow(false).also { resultFlow ->
                // Initial computation handled in updateNumbersOverlayVisibility()
            }
        }

        private val _showNumbersOverlayComputed = MutableStateFlow(false)
        val showNumbersOverlayComputed: StateFlow<Boolean> = _showNumbersOverlayComputed.asStateFlow()

        // ===== Instruction Bar Settings =====

        /**
         * Instruction bar mode:
         * - ON: Always show instruction bar
         * - OFF: Never show instruction bar
         * - AUTO: Show briefly then fade out (default)
         */
        enum class InstructionBarMode {
            ON,    // Always visible
            OFF,   // Never visible
            AUTO   // Show then fade after 3 seconds
        }

        private val _instructionBarMode = MutableStateFlow(InstructionBarMode.AUTO)
        val instructionBarMode: StateFlow<InstructionBarMode> = _instructionBarMode.asStateFlow()

        fun setInstructionBarMode(mode: InstructionBarMode) {
            _instructionBarMode.value = mode
            Log.d(TAG, "Instruction bar mode: $mode")
        }

        fun cycleInstructionBarMode() {
            val newMode = when (_instructionBarMode.value) {
                InstructionBarMode.OFF -> InstructionBarMode.AUTO
                InstructionBarMode.AUTO -> InstructionBarMode.ON
                InstructionBarMode.ON -> InstructionBarMode.OFF
            }
            setInstructionBarMode(newMode)
        }

        // ===== Badge Theme Settings =====

        /**
         * Badge color themes for numbered badges.
         */
        enum class BadgeTheme(val backgroundColor: Long, val textColor: Long) {
            GREEN(0xFF4CAF50, 0xFFFFFFFF),       // Default green
            BLUE(0xFF2196F3, 0xFFFFFFFF),        // Blue
            PURPLE(0xFF9C27B0, 0xFFFFFFFF),      // Purple
            ORANGE(0xFFFF9800, 0xFF000000),      // Orange with black text
            RED(0xFFF44336, 0xFFFFFFFF),         // Red
            TEAL(0xFF009688, 0xFFFFFFFF),        // Teal
            PINK(0xFFE91E63, 0xFFFFFFFF)         // Pink
        }

        private val _badgeTheme = MutableStateFlow(BadgeTheme.GREEN)
        val badgeTheme: StateFlow<BadgeTheme> = _badgeTheme.asStateFlow()

        fun setBadgeTheme(theme: BadgeTheme) {
            _badgeTheme.value = theme
            Log.d(TAG, "Badge theme: $theme")
        }

        fun cycleBadgeTheme() {
            val themes = BadgeTheme.entries
            val currentIndex = themes.indexOf(_badgeTheme.value)
            val nextIndex = (currentIndex + 1) % themes.size
            setBadgeTheme(themes[nextIndex])
        }

        /**
         * Set the numbers overlay mode.
         * Voice commands: "numbers on", "numbers off", "numbers auto"
         */
        fun setNumbersOverlayMode(mode: NumbersOverlayMode) {
            _numbersOverlayMode.value = mode
            updateNumbersOverlayVisibility()
            Log.d(TAG, "Numbers overlay mode: $mode")
        }

        /**
         * Cycle through overlay modes: OFF -> AUTO -> ON -> OFF
         * Voice command: "show numbers" or "toggle numbers"
         */
        fun cycleNumbersOverlayMode() {
            val newMode = when (_numbersOverlayMode.value) {
                NumbersOverlayMode.OFF -> NumbersOverlayMode.AUTO
                NumbersOverlayMode.AUTO -> NumbersOverlayMode.ON
                NumbersOverlayMode.ON -> NumbersOverlayMode.OFF
            }
            setNumbersOverlayMode(newMode)
        }

        /**
         * Update visibility based on current mode and items.
         * Called when mode changes or when items are updated.
         */
        internal fun updateNumbersOverlayVisibility() {
            val mode = _numbersOverlayMode.value
            val hasItems = _numberedOverlayItems.value.isNotEmpty()

            val shouldShow = when (mode) {
                NumbersOverlayMode.ON -> true
                NumbersOverlayMode.OFF -> false
                NumbersOverlayMode.AUTO -> hasItems  // Only show when list items exist
            }

            _showNumbersOverlayComputed.value = shouldShow
            Log.d(TAG, "Numbers overlay: mode=$mode, hasItems=$hasItems, showing=$shouldShow")
        }

        /**
         * Legacy toggle for backward compatibility.
         */
        fun setShowNumbersOverlay(show: Boolean) {
            setNumbersOverlayMode(if (show) NumbersOverlayMode.ON else NumbersOverlayMode.OFF)
        }

        /**
         * Enable or disable continuous screen scanning.
         */
        fun setContinuousMonitoring(enabled: Boolean) {
            instance?.continuousScanningEnabled?.set(enabled)
            _isContinuousMonitoring.value = enabled
            Log.d(TAG, "Continuous monitoring ${if (enabled) "ENABLED" else "DISABLED"}")
        }

        /**
         * Check if continuous monitoring is enabled.
         */
        fun isContinuousMonitoringEnabled(): Boolean {
            return instance?.continuousScanningEnabled?.get() ?: true
        }

        /**
         * Rescan current app - clears cache for current package only.
         * Runs asynchronously in service scope.
         */
        fun rescanCurrentApp() {
            instance?.serviceScope?.launch {
                val packageName = instance?.currentPackageName ?: return@launch
                val count = instance?.screenHashRepository?.clearScreensForPackage(packageName) ?: 0
                Log.d(TAG, "Rescan Current App: cleared $count screens for $packageName")
                // Trigger immediate rescan
                instance?.performExploration()
            }
        }

        /**
         * Rescan everything - clears ALL cached screens.
         * Runs asynchronously in service scope.
         */
        fun rescanEverything() {
            instance?.serviceScope?.launch {
                val count = instance?.screenHashRepository?.clearAllScreens() ?: 0
                Log.d(TAG, "Rescan Everything: cleared $count total screens")
                // Trigger immediate rescan
                instance?.performExploration()
            }
        }

        /**
         * Get total count of cached screens.
         */
        fun getCachedScreenCount(): Int {
            return runBlocking { instance?.screenHashRepository?.getScreenCount() ?: 0 }
        }

        /**
         * Get cached screen count for current package.
         */
        fun getCachedScreenCountForCurrentApp(): Int {
            val packageName = instance?.currentPackageName ?: return 0
            return runBlocking { instance?.screenHashRepository?.getScreenCountForPackage(packageName) ?: 0 }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected() called")
        instance = this
        _isConnected.value = true
        Log.d(TAG, "isConnected set to true")

        try {
            serviceInfo = serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                        AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS  // Required for windows property
                notificationTimeout = 100
            }
            Log.d(TAG, "serviceInfo configured successfully with FLAG_RETRIEVE_INTERACTIVE_WINDOWS")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring serviceInfo", e)
        }

        // Initialize VoiceOSCoreNG facade for voice command processing
        initializeVoiceOSCore()
    }

    /**
     * Initialize the VoiceOSCoreNG facade with speech engine and handlers.
     */
    private fun initializeVoiceOSCore() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Initializing VoiceOSCoreNG facade...")

                // Create the facade with Android-specific handlers and speech engine
                // Primary engine: VIVOKA (offline, commercial SDK)
                // Fallback: ANDROID_STT (requires network)
                // Pass shared commandRegistry so both service and ActionCoordinator use same instance
                voiceOSCore = VoiceOSCoreNG.createForAndroid(
                    service = this@VoiceOSAccessibilityService,
                    configuration = ServiceConfiguration(
                        autoStartListening = false,  // Don't auto-start, UI will control
                        speechEngine = "VIVOKA",     // Primary: Vivoka offline engine
                        debugMode = true
                    ),
                    commandRegistry = commandRegistry  // Shared registry - single source of truth
                )

                // Initialize the facade
                voiceOSCore?.initialize()
                Log.d(TAG, "VoiceOSCoreNG facade initialized successfully")

                // Observe speech results and process commands
                voiceOSCore?.speechResults?.collect { speechResult ->
                    Log.d(TAG, "Speech result: ${speechResult.text} (confidence: ${speechResult.confidence})")
                    if (speechResult.isFinal) {
                        // Pre-process VoiceOS control commands (numbers overlay, etc.)
                        if (!handleVoiceOSControlCommand(speechResult.text.lowercase().trim())) {
                            // Not a VoiceOS control command, delegate to normal processing
                            voiceOSCore?.processCommand(speechResult.text, speechResult.confidence)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize VoiceOSCoreNG facade", e)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Handle screen change events to invalidate caches and regenerate commands
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // New window/screen opened - regenerate commands
                Log.d(TAG, "Window state changed: ${event.packageName}")
                handleScreenChange(event.packageName?.toString())
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Content within window changed - may need to update commands
                // Only trigger if content change is significant (e.g., not just scrolling)
                if (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0) {
                    Log.v(TAG, "Window content changed (subtree): ${event.packageName}")
                    // Debounce: Don't regenerate on every small change
                    // The cache TTL will handle stale elements
                }
            }
        }
    }

    /**
     * Handle VoiceOS control commands (numbers overlay, etc.).
     * Returns true if the command was handled, false to delegate to normal processing.
     */
    private fun handleVoiceOSControlCommand(command: String): Boolean {
        return when (command) {
            // Numbers overlay commands
            "numbers on", "show numbers", "numbers always" -> {
                setNumbersOverlayMode(NumbersOverlayMode.ON)
                Log.d(TAG, "Voice command: Numbers overlay ON")
                true
            }
            "numbers off", "hide numbers", "no numbers" -> {
                setNumbersOverlayMode(NumbersOverlayMode.OFF)
                Log.d(TAG, "Voice command: Numbers overlay OFF")
                true
            }
            "numbers auto", "numbers automatic", "auto numbers" -> {
                setNumbersOverlayMode(NumbersOverlayMode.AUTO)
                Log.d(TAG, "Voice command: Numbers overlay AUTO")
                true
            }
            else -> false // Not a VoiceOS control command
        }
    }

    /**
     * Handle screen change by clearing caches and optionally regenerating commands.
     * Implements continuous monitoring with screen hash comparison.
     */
    private fun handleScreenChange(packageName: String?) {
        // Debounce rapid screen changes
        val now = System.currentTimeMillis()
        val lastChange = lastScreenChangeTime.get()
        if (now - lastChange < SCREEN_CHANGE_DEBOUNCE_MS) {
            Log.v(TAG, "Screen change debounced (${now - lastChange}ms since last)")
            return
        }
        lastScreenChangeTime.set(now)

        // Update current package tracking
        currentPackageName = packageName

        // Check if continuous monitoring is enabled
        if (!continuousScanningEnabled.get()) {
            // Manual mode - just clear registry and wait for user action
            commandRegistry.clear()
            Log.d(TAG, "Screen changed to $packageName - manual mode, awaiting user scan")
            return
        }

        // Continuous monitoring mode - auto-scan with hash comparison
        if (packageName != null && packageName != "unknown") {
            serviceScope.launch {
                try {
                    // Get root node for hash generation
                    val rootNode = rootInActiveWindow
                    if (rootNode == null) {
                        Log.w(TAG, "No active window for screen hash")
                        commandRegistry.clear()
                        return@launch
                    }

                    // Generate screen hash from current elements
                    val screenHash = generateScreenHash(rootNode)
                    rootNode.recycle()

                    // Check if this screen is already known
                    val isKnown = screenHashRepository.hasScreen(screenHash)
                    val appVersion = getAppInfo(packageName).versionName
                    val storedVersion = screenHashRepository.getAppVersion(screenHash)

                    if (isKnown && appVersion == storedVersion) {
                        // Load cached commands instead of rescanning
                        val cachedCommands = screenHashRepository.getCommandsForScreen(screenHash)
                        if (cachedCommands.isNotEmpty()) {
                            commandRegistry.updateSync(cachedCommands)
                            currentScreenHash = screenHash
                            Log.d(TAG, "Screen known - loaded ${cachedCommands.size} cached commands for $packageName")

                            // Update screen info for UI display
                            val screenInfo = screenHashRepository.getScreenInfo(screenHash)
                            _currentScreenInfo.value = screenInfo
                            return@launch
                        }
                        // Fall through to rescan if cache is empty
                    }

                    // New or updated screen - perform full scan
                    Log.d(TAG, "Screen changed to $packageName - ${if (isKnown) "version changed, rescanning" else "new screen, scanning"}")
                    commandRegistry.clear()
                    currentScreenHash = screenHash

                    // Perform exploration and cache results
                    performExplorationWithCache(screenHash, packageName, appVersion)

                } catch (e: Exception) {
                    Log.e(TAG, "Error in continuous monitoring", e)
                    commandRegistry.clear()
                }
            }
        } else {
            commandRegistry.clear()
        }
    }

    /**
     * Generate a hash of the current screen for comparison.
     *
     * IMPORTANT: Includes screen dimensions in the hash so that different
     * orientations/window sizes get separate cache entries. This means:
     * - Portrait 1080x1920 = hash1
     * - Landscape 1920x1080 = hash2
     * - Freeform 800x600 = hash3
     *
     * When user rotates back to portrait, we load from hash1 cache (instant).
     */
    private fun generateScreenHash(rootNode: AccessibilityNodeInfo): String {
        val elements = mutableListOf<String>()
        collectElementSignatures(rootNode, elements, maxDepth = 5)

        // Include screen dimensions in hash for orientation/freeform support
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val dimensionKey = "${screenWidth}x${screenHeight}"

        val signature = "$dimensionKey|${elements.sorted().joinToString("|")}"
        return HashUtils.generateHash(signature, 16)
    }

    /**
     * Collect element signatures for screen hashing.
     * Uses className, resourceId, and text (normalized) to create stable hash.
     */
    private fun collectElementSignatures(
        node: AccessibilityNodeInfo,
        signatures: MutableList<String>,
        depth: Int = 0,
        maxDepth: Int = 5
    ) {
        if (depth > maxDepth) return

        // Build signature from stable properties
        val className = node.className?.toString()?.substringAfterLast(".") ?: ""
        val resourceId = node.viewIdResourceName?.substringAfterLast("/") ?: ""
        val text = node.text?.toString()?.take(20)?.replace(Regex("\\d+"), "#") ?: ""
        val isClickable = if (node.isClickable) "C" else ""
        val isScrollable = if (node.isScrollable) "S" else ""

        if (className.isNotEmpty() || resourceId.isNotEmpty()) {
            signatures.add("$className:$resourceId:$text:$isClickable$isScrollable")
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectElementSignatures(child, signatures, depth + 1, maxDepth)
                child.recycle()
            }
        }
    }

    /**
     * Perform exploration and cache results in the screen hash repository.
     */
    private fun performExplorationWithCache(screenHash: String, packageName: String, appVersion: String) {
        serviceScope.launch {
            try {
                val rootNode = rootInActiveWindow ?: return@launch

                val result = exploreNode(rootNode)
                _explorationResults.value = result
                rootNode.recycle()

                // Get the generated commands
                val commands = commandRegistry.all()

                // Cache the screen and commands
                screenHashRepository.saveScreen(
                    hash = screenHash,
                    packageName = packageName,
                    activityName = null,
                    appVersion = appVersion,
                    elementCount = result.totalElements
                )
                screenHashRepository.saveCommandsForScreen(screenHash, commands)

                // Update screen info for UI display
                val screenInfo = ScreenInfo(
                    hash = screenHash,
                    packageName = packageName,
                    activityName = null,
                    appVersion = appVersion,
                    elementCount = result.totalElements,
                    actionableCount = result.clickableElements + result.scrollableElements,
                    commandCount = commands.size,
                    scannedAt = System.currentTimeMillis(),
                    isCached = false  // Just scanned, not from cache
                )
                _currentScreenInfo.value = screenInfo

                Log.d(TAG, "Screen cached: ${screenHash.take(16)}... with ${commands.size} commands")

            } catch (e: Exception) {
                Log.e(TAG, "Error in exploration with cache", e)
            }
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")

        // Dispose VoiceOSCoreNG facade
        serviceScope.launch {
            try {
                voiceOSCore?.dispose()
                Log.d(TAG, "VoiceOSCoreNG facade disposed")
            } catch (e: Exception) {
                Log.e(TAG, "Error disposing VoiceOSCoreNG facade", e)
            }
        }

        super.onDestroy()
        instance = null
        _isConnected.value = false
        voiceOSCore = null
        serviceScope.cancel()
    }

    /**
     * Perform exploration of the currently focused app
     */
    fun performExploration() {
        serviceScope.launch {
            try {
                val rootNode = rootInActiveWindow ?: run {
                    _lastError.value = "No active window available"
                    return@launch
                }

                val result = exploreNode(rootNode)
                _explorationResults.value = result
                rootNode.recycle()

            } catch (e: Exception) {
                _lastError.value = "Exploration failed: ${e.message}"
            }
        }
    }

    /**
     * Perform exploration across all windows
     */
    fun performFullExploration() {
        serviceScope.launch {
            try {
                Log.d(TAG, "performFullExploration() started")
                val allResults = mutableListOf<ExplorationResult>()

                val windowList = windows
                Log.d(TAG, "Found ${windowList.size} windows to explore")

                windowList.forEachIndexed { index, window ->
                    Log.d(TAG, "Window $index: type=${window.type}, title=${window.title}, layer=${window.layer}")
                    window.root?.let { rootNode ->
                        Log.d(TAG, "  Exploring root node: pkg=${rootNode.packageName}")
                        val result = exploreNode(rootNode)
                        allResults.add(result)
                        Log.d(TAG, "  Found ${result.totalElements} elements")
                        rootNode.recycle()
                    } ?: Log.d(TAG, "  Window $index has null root")
                }

                // Merge all results
                val merged = mergeResults(allResults)
                _explorationResults.value = merged
                Log.d(TAG, "performFullExploration() complete: ${merged.totalElements} total elements from ${allResults.size} windows")

            } catch (e: Exception) {
                Log.e(TAG, "Full exploration failed", e)
                _lastError.value = "Full exploration failed: ${e.message}"
            }
        }
    }

    private suspend fun exploreNode(rootNode: AccessibilityNodeInfo): ExplorationResult {
        val startTime = System.currentTimeMillis()

        // Extract all elements from the tree
        val elements = mutableListOf<ElementInfo>()
        val hierarchy = mutableListOf<HierarchyNode>()
        val seenHashes = mutableSetOf<String>()
        val duplicates = mutableListOf<DuplicateInfo>()

        extractElements(rootNode, elements, hierarchy, seenHashes, duplicates, depth = 0)

        // Generate VUIDs for all elements
        val packageName = rootNode.packageName?.toString() ?: "unknown"
        val vuids = elements.map { element ->
            val typeCode = VUIDGenerator.getTypeCode(element.className)
            val elementIdentifier = buildString {
                append(element.className)
                if (element.resourceId.isNotBlank()) append(":${element.resourceId}")
                if (element.text.isNotBlank()) append(":${element.text.take(20)}")
            }
            val elemHash = HashUtils.generateHash(elementIdentifier, 8)
            val vuid = VUIDGenerator.generate(packageName, typeCode, elemHash)

            VUIDInfo(
                element = element,
                vuid = vuid,
                hash = elemHash
            )
        }

        // Derive labels for ALL elements (looking at children for empty parents)
        val elementLabels = deriveElementLabels(elements, hierarchy)

        // Generate commands (pass hierarchy to find child labels)
        val commands = generateCommands(elements, hierarchy, elementLabels, packageName)

        // Generate AVU output with derived labels
        val avuOutput = generateAVU(packageName, elements, elementLabels, commands)

        val duration = System.currentTimeMillis() - startTime

        Log.d(TAG, "=== DEDUPLICATION RESULTS ===")
        Log.d(TAG, "Total elements: ${elements.size}")
        Log.d(TAG, "Unique hashes: ${seenHashes.size}")
        Log.d(TAG, "Duplicates found: ${duplicates.size}")
        duplicates.take(5).forEach { dup ->
            Log.d(TAG, "  DUP: ${dup.element.className.substringAfterLast(".")} '${dup.element.text.take(20)}' first@${dup.firstSeenIndex}")
        }
        Log.d(TAG, "=============================")

        return ExplorationResult(
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
            duration = duration,
            totalElements = elements.size,
            clickableElements = elements.count { it.isClickable },
            scrollableElements = elements.count { it.isScrollable },
            elements = elements,
            vuids = vuids,
            hierarchy = hierarchy,
            duplicates = duplicates,
            deduplicationStats = DeduplicationStats(
                totalHashes = elements.size,  // Total elements processed
                uniqueHashes = seenHashes.size,  // Unique hash count
                duplicateCount = duplicates.size,  // Number of duplicate occurrences
                duplicateElements = duplicates
            ),
            commands = commands,
            avuOutput = avuOutput,
            elementLabels = elementLabels  // Map of index -> derived label
        )
    }

    /**
     * Dynamic container types that indicate list/dynamic content.
     */
    private val dynamicContainerTypes = setOf(
        "RecyclerView",
        "ListView",
        "GridView",
        "ViewPager",
        "ViewPager2",
        "ScrollView",
        "HorizontalScrollView",
        "NestedScrollView",
        "LazyColumn",       // Compose
        "LazyRow",          // Compose
        "LazyVerticalGrid", // Compose
        "LazyHorizontalGrid" // Compose
    )

    /**
     * Check if a class name is a dynamic container.
     */
    private fun isDynamicContainer(className: String): Boolean {
        val simpleName = className.substringAfterLast(".")
        return dynamicContainerTypes.any { simpleName.contains(it, ignoreCase = true) }
    }

    private fun extractElements(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        hierarchy: MutableList<HierarchyNode>,
        seenHashes: MutableSet<String>,
        duplicates: MutableList<DuplicateInfo>,
        depth: Int,
        parentIndex: Int? = null,
        inDynamicContainer: Boolean = false,
        containerType: String = "",
        listIndex: Int = -1
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val className = node.className?.toString() ?: ""

        // Check if THIS node is a dynamic container
        val isContainer = isDynamicContainer(className)
        val currentContainerType = if (isContainer) className.substringAfterLast(".") else containerType
        val isInDynamic = inDynamicContainer || isContainer

        val element = ElementInfo(
            className = className,
            resourceId = node.viewIdResourceName ?: "",
            text = node.text?.toString() ?: "",
            contentDescription = node.contentDescription?.toString() ?: "",
            bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isScrollable = node.isScrollable,
            isEnabled = node.isEnabled,
            packageName = node.packageName?.toString() ?: "",
            // Dynamic content tracking
            isInDynamicContainer = isInDynamic && !isContainer, // Container itself is not dynamic, its children are
            containerType = if (isInDynamic && !isContainer) currentContainerType else "",
            listIndex = if (isInDynamic && !isContainer) listIndex else -1
        )

        // Generate hash for deduplication - use className|resourceId|text (NOT bounds, as bounds make every element unique)
        val hashInput = "${element.className}|${element.resourceId}|${element.text}"
        val hash = HashUtils.generateHash(hashInput, 16)

        if (seenHashes.contains(hash)) {
            Log.d(TAG, "DUPLICATE FOUND: hash=$hash class=${element.className.substringAfterLast(".")} text='${element.text.take(20)}'")
            duplicates.add(DuplicateInfo(
                hash = hash,
                element = element,
                firstSeenIndex = elements.indexOfFirst { e ->
                    val h = HashUtils.generateHash("${e.className}|${e.resourceId}|${e.text}", 16)
                    h == hash
                }
            ))
        } else {
            seenHashes.add(hash)
        }

        val currentIndex = elements.size
        elements.add(element)

        // Track hierarchy
        hierarchy.add(HierarchyNode(
            index = currentIndex,
            depth = depth,
            parentIndex = parentIndex,
            childCount = node.childCount,
            className = element.className.substringAfterLast(".")
        ))

        // Recurse into children
        // If this is a dynamic container, track child indices for list items
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                // Children of a dynamic container get the list index from their position
                val childListIndex = if (isContainer) i else listIndex
                extractElements(
                    child, elements, hierarchy, seenHashes, duplicates,
                    depth + 1, currentIndex,
                    isInDynamic, currentContainerType, childListIndex
                )
                child.recycle()
            }
        }
    }

    /**
     * Derive labels for ALL elements by looking at child TextViews when parent has no text.
     * Returns a map of elementIndex -> derivedLabel
     */
    private fun deriveElementLabels(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>
    ): Map<Int, String> {
        val labels = mutableMapOf<Int, String>()

        elements.forEachIndexed { index, element ->
            // First try the element's own content
            var label: String? = when {
                element.text.isNotBlank() -> element.text.take(30)
                element.contentDescription.isNotBlank() -> element.contentDescription.take(30)
                element.resourceId.isNotBlank() -> element.resourceId.substringAfterLast("/").replace("_", " ")
                else -> null
            }

            // If no label, look at children (especially for clickable Views wrapping TextViews)
            if (label == null) {
                val node = hierarchy.getOrNull(index)
                if (node != null && node.childCount > 0) {
                    for (childIdx in (index + 1) until minOf(index + 10, elements.size)) {
                        val childNode = hierarchy.getOrNull(childIdx) ?: continue
                        if (childNode.depth <= node.depth) break
                        if (childNode.depth == node.depth + 1) {
                            val childElement = elements[childIdx]
                            if (childElement.text.isNotBlank()) {
                                label = childElement.text.take(30)
                                break
                            }
                            if (childElement.contentDescription.isNotBlank()) {
                                label = childElement.contentDescription.take(30)
                                break
                            }
                        }
                    }
                }
            }

            // Store the label (or fallback to class name)
            labels[index] = label ?: element.className.substringAfterLast(".")
        }

        return labels
    }

    /**
     * Generate AVU (Avanues Universal) format output with proper command names
     */
    private fun generateAVU(
        packageName: String,
        elements: List<ElementInfo>,
        elementLabels: Map<Int, String>,
        commands: List<GeneratedCommand>
    ): String {
        return buildString {
            appendLine("# Avanues Universal Format v2.0")
            appendLine("# Package: $packageName")
            appendLine("# Elements: ${elements.size}")
            appendLine("# Commands: ${commands.size}")
            appendLine()
            appendLine("schema: avu-2.0")
            appendLine("version: 2.0.0")
            appendLine("package: $packageName")
            appendLine()

            // Elements section with derived labels
            appendLine("@elements:")
            elements.forEachIndexed { index, element ->
                val typeCode = VUIDGenerator.getTypeCode(element.className)
                val label = elementLabels[index] ?: element.className.substringAfterLast(".")
                val clickable = if (element.isClickable) "T" else "F"
                val scrollable = if (element.isScrollable) "T" else "F"
                appendLine("  - idx:$index type:${typeCode.abbrev} label:\"$label\" click:$clickable scroll:$scrollable")
            }
            appendLine()

            // Commands section with voice phrases
            appendLine("@commands:")
            if (commands.isEmpty()) {
                appendLine("  # No actionable elements found")
            } else {
                commands.forEach { cmd ->
                    // The voice command is just the label (e.g., "Accessibility", "Reset")
                    // The action (tap/scroll/toggle) is metadata
                    appendLine("  - voice:\"${cmd.derivedLabel}\" action:${cmd.action} vuid:${cmd.targetVuid}")
                    // Also include alternate phrases
                    appendLine("    alternates: [\"${cmd.phrase}\", \"press ${cmd.derivedLabel}\", \"select ${cmd.derivedLabel}\"]")
                }
            }
            appendLine()

            // Actionable elements summary
            appendLine("@actionable:")
            val actionableElements = elements.mapIndexedNotNull { index, element ->
                if (element.isClickable || element.isScrollable) {
                    val label = elementLabels[index] ?: return@mapIndexedNotNull null
                    val action = when {
                        element.isClickable -> "tap"
                        element.isScrollable -> "scroll"
                        else -> "interact"
                    }
                    "  - \"$label\" -> $action"
                } else null
            }
            actionableElements.forEach { appendLine(it) }
        }
    }

    /**
     * Generate voice commands for actionable elements using KMP CommandGenerator.
     * Also updates the in-memory CommandRegistry for voice matching.
     *
     * Static/Dynamic Separation:
     * - Static commands (menus, buttons) are persisted to database
     * - Dynamic commands (list items, emails) are kept in memory only
     */
    private fun generateCommands(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>,
        elementLabels: Map<Int, String>,
        packageName: String
    ): List<GeneratedCommand> {
        // Generate QuantizedCommands with persistence info using KMP CommandGenerator
        val commandResults = elements.mapNotNull { element ->
            CommandGenerator.fromElementWithPersistence(element, packageName)
        }

        // Separate static (persist) and dynamic (memory-only) commands
        val staticCommands = commandResults.filter { it.shouldPersist }
        val dynamicCommands = commandResults.filter { !it.shouldPersist }

        // All commands go to in-memory registry for voice matching
        val allCommands = commandResults.map { it.command }
        commandRegistry.updateSync(allCommands)

        // Also generate index commands for list items ("first", "second", etc.)
        val listItems = elements.filter { it.listIndex >= 0 }
        val indexCommands = CommandGenerator.generateListIndexCommands(listItems, packageName)
        if (indexCommands.isNotEmpty()) {
            commandRegistry.addAll(indexCommands)
        }

        // Generate label-based commands for list items (e.g., "Lifemiles" for email sender)
        val labelCommands = CommandGenerator.generateListLabelCommands(listItems, packageName)
        if (labelCommands.isNotEmpty()) {
            commandRegistry.addAll(labelCommands)
            Log.d(TAG, "Label commands for lists: ${labelCommands.take(5).map { it.phrase }}")
        }

        // Populate numbered overlay items for visual display
        // These are the elements that can be selected by saying "first", "second", "1", "2", etc.
        //
        // IMPORTANT: We filter for clickable items with meaningful labels, sort by vertical
        // position (top coordinate), and assign SEQUENTIAL numbers 1, 2, 3, etc.
        // This ensures numbers match what user sees on screen, not nested container indices.
        val clickableListItems = listItems
            .filter { element ->
                // Must have valid bounds (visible on screen)
                val hasValidBounds = !(element.bounds.left == 0 && element.bounds.top == 0 &&
                    element.bounds.right == 0 && element.bounds.bottom == 0)
                // Must be clickable
                val isClickable = element.isClickable
                // Must have extractable label (sender name, title, etc.)
                val hasLabel = CommandGenerator.extractShortLabel(element) != null

                hasValidBounds && isClickable && hasLabel
            }
            .sortedBy { it.bounds.top }  // Sort by vertical position (top to bottom)

        // Assign sequential numbers based on sorted order
        val overlayItems = clickableListItems.mapIndexed { index, element ->
            val label = CommandGenerator.extractShortLabel(element) ?: ""

            // Generate VUID for this element
            val typeCode = if (element.isClickable) VUIDTypeCode.BUTTON else VUIDTypeCode.ELEMENT
            val elementHash = HashUtils.generateHash(
                "${element.className}|${element.resourceId}|${element.text.ifBlank { element.contentDescription }}",
                8
            )
            val vuid = VUIDGenerator.generate(packageName, typeCode, elementHash)

            NumberOverlayItem(
                number = index + 1,  // Sequential 1-based numbering by screen position
                label = label,
                left = element.bounds.left,
                top = element.bounds.top,
                right = element.bounds.right,
                bottom = element.bounds.bottom,
                vuid = vuid
            )
        }

        _numberedOverlayItems.value = overlayItems
        updateNumbersOverlayVisibility()  // Update visibility based on mode (AUTO shows when items exist)
        if (overlayItems.isNotEmpty()) {
            Log.d(TAG, "Numbered overlay: ${overlayItems.size} items for voice selection")
        }

        // Log the separation
        Log.d(TAG, "Commands: ${allCommands.size} total (${staticCommands.size} static, ${dynamicCommands.size} dynamic)")
        if (dynamicCommands.isNotEmpty()) {
            Log.d(TAG, "Dynamic commands (not persisted): ${dynamicCommands.take(3).map { it.command.phrase.take(30) }}")
        }
        if (indexCommands.isNotEmpty()) {
            Log.d(TAG, "Index commands for lists: ${indexCommands.take(5).map { it.phrase }}")
        }

        // Update speech engine grammar (Vivoka SDK) so it recognizes ALL phrases
        // Includes: element commands, index commands ("first", "second"), and label commands ("Lifemiles")
        val commandPhrases = allCommands.map { it.phrase } +
            indexCommands.map { it.phrase } +
            labelCommands.map { it.phrase }
        serviceScope.launch {
            try {
                voiceOSCore?.updateCommands(commandPhrases)
                Log.d(TAG, "Updated speech engine with ${commandPhrases.size} command phrases " +
                    "(${allCommands.size} elements, ${indexCommands.size} index, ${labelCommands.size} labels)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update speech engine commands", e)
            }
        }

        // ONLY persist STATIC commands to SQLDelight database
        // Dynamic commands are kept in memory only and discarded on navigation
        val staticQuantizedCommands = staticCommands.map { it.command }
        if (staticQuantizedCommands.isNotEmpty()) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val currentTime = System.currentTimeMillis()

                    // Step 1: Ensure scraped_app exists (FK parent)
                    val appInfo = getAppInfo(packageName)
                    val scrapedApp = ScrapedAppDTO(
                        appId = packageName,
                        packageName = packageName,
                        versionCode = appInfo.versionCode,
                        versionName = appInfo.versionName,
                        appHash = HashUtils.generateHash(packageName + appInfo.versionCode, 8),
                        isFullyLearned = 0,
                        learnCompletedAt = null,
                        scrapingMode = "DYNAMIC",
                        scrapeCount = 1,
                        elementCount = elements.size.toLong(),
                        commandCount = staticQuantizedCommands.size.toLong(),
                        firstScrapedAt = currentTime,
                        lastScrapedAt = currentTime
                    )
                    scrapedAppRepository.insert(scrapedApp)
                    Log.d(TAG, "Step 1/3: Inserted scraped_app for $packageName")

                    // Step 2: Insert scraped_elements (FK parent for commands)
                    // CRITICAL: Use the SAME elementHash that's in the command's metadata
                    // to ensure FK constraint is satisfied when inserting commands
                    val insertedHashes = mutableSetOf<String>()
                    var insertedCount = 0

                    // Iterate over STATIC commands only and insert corresponding elements
                    // using the EXACT hash from command metadata
                    staticQuantizedCommands.forEach { cmd ->
                        val elementHash = cmd.metadata["elementHash"] ?: return@forEach

                        // Skip if already inserted (avoid duplicates)
                        if (elementHash in insertedHashes) return@forEach

                        // Find the corresponding element by matching metadata
                        val element = elements.find { el ->
                            // Match by className + resourceId + label
                            val cmdClassName = cmd.metadata["className"] ?: ""
                            val cmdResourceId = cmd.metadata["resourceId"] ?: ""
                            el.className == cmdClassName && el.resourceId == cmdResourceId
                        } ?: elements.firstOrNull { el ->
                            // Fallback: match by label
                            val cmdLabel = cmd.metadata["label"] ?: ""
                            el.text == cmdLabel || el.contentDescription == cmdLabel
                        }

                        // Build scraped element DTO using command's elementHash
                        val scrapedElement = ScrapedElementDTO(
                            id = 0,  // Auto-generated
                            elementHash = elementHash,  // Use EXACT hash from command
                            appId = packageName,
                            uuid = null,
                            className = element?.className ?: cmd.metadata["className"] ?: "",
                            viewIdResourceName = element?.resourceId?.ifBlank { null } ?: cmd.metadata["resourceId"]?.ifBlank { null },
                            text = element?.text?.ifBlank { null },
                            contentDescription = element?.contentDescription?.ifBlank { null },
                            bounds = element?.let { "${it.bounds.left},${it.bounds.top},${it.bounds.right},${it.bounds.bottom}" } ?: "0,0,0,0",
                            isClickable = if (element?.isClickable == true) 1L else 0L,
                            isLongClickable = if (element?.isLongClickable == true) 1L else 0L,
                            isEditable = 0L,
                            isScrollable = if (element?.isScrollable == true) 1L else 0L,
                            isCheckable = 0L,
                            isFocusable = 0L,
                            isEnabled = if (element?.isEnabled != false) 1L else 0L,
                            depth = 0L,
                            indexInParent = 0L,
                            scrapedAt = currentTime,
                            semanticRole = null,
                            inputType = null,
                            visualWeight = null,
                            isRequired = null,
                            formGroupId = null,
                            placeholderText = null,
                            validationPattern = null,
                            backgroundColor = null,
                            screen_hash = null
                        )
                        try {
                            scrapedElementRepository.insert(scrapedElement)
                            insertedHashes.add(elementHash)
                            insertedCount++
                        } catch (e: Exception) {
                            // Element may already exist, ignore duplicate errors
                            Log.v(TAG, "Element hash $elementHash may already exist: ${e.message}")
                            insertedHashes.add(elementHash)  // Mark as "handled" even if exists
                        }
                    }
                    Log.d(TAG, "Step 2/3: Inserted $insertedCount scraped_elements for ${insertedHashes.size} unique hashes (static only)")

                    // Step 3: Now insert STATIC commands only (FK references are satisfied)
                    commandPersistence.insertBatch(staticQuantizedCommands)
                    Log.d(TAG, "Step 3/3: Persisted ${staticQuantizedCommands.size} STATIC commands to voiceos.db (skipped ${dynamicCommands.size} dynamic)")

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist commands to database", e)
                }
            }
        }

        // Also create legacy GeneratedCommand for UI display (backwards compatibility)
        return elements
            .mapIndexedNotNull { index, element ->
                // Only process clickable or scrollable elements
                if (!element.isClickable && !element.isScrollable) return@mapIndexedNotNull null

                // Get the pre-derived label
                val label = elementLabels[index]

                // Skip if label is just the class name (no meaningful content)
                if (label == null || label == element.className.substringAfterLast(".")) {
                    return@mapIndexedNotNull null
                }

                val actionType = when {
                    element.isClickable && element.className.contains("Button") -> "tap"
                    element.isClickable && element.className.contains("EditText") -> "focus"
                    element.isClickable && element.className.contains("ImageView") -> "tap"
                    element.isClickable && element.className.contains("CheckBox") -> "toggle"
                    element.isClickable && element.className.contains("Switch") -> "toggle"
                    element.isClickable -> "tap"
                    element.isScrollable -> "scroll"
                    else -> "interact"
                }

                val typeCode = VUIDGenerator.getTypeCode(element.className)
                val elemHash = HashUtils.generateHash(element.resourceId.ifEmpty { label }, 8)
                val vuid = VUIDGenerator.generate(packageName, typeCode, elemHash)

                GeneratedCommand(
                    phrase = "$actionType $label",  // Full voice phrase: "tap Reset"
                    alternates = listOf(
                        "press $label",
                        "select $label",
                        label  // Just the label also works
                    ),
                    targetVuid = vuid,
                    action = actionType,  // Action type for execution
                    element = element,
                    derivedLabel = label  // The clean label without action prefix
                )
            }
    }

    /**
     * Get app version info for the given package name.
     */
    private fun getAppInfo(packageName: String): AppVersionInfo {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            AppVersionInfo(
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                versionName = packageInfo.versionName ?: "unknown"
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package not found: $packageName, using defaults")
            AppVersionInfo(versionCode = 0, versionName = "unknown")
        }
    }

    private data class AppVersionInfo(val versionCode: Long, val versionName: String)

    private fun mergeResults(results: List<ExplorationResult>): ExplorationResult {
        if (results.isEmpty()) {
            return ExplorationResult(
                packageName = "empty",
                timestamp = System.currentTimeMillis(),
                duration = 0,
                totalElements = 0,
                clickableElements = 0,
                scrollableElements = 0,
                elements = emptyList(),
                vuids = emptyList(),
                hierarchy = emptyList(),
                duplicates = emptyList(),
                deduplicationStats = DeduplicationStats(0, 0, 0, emptyList()),
                commands = emptyList(),
                avuOutput = "",
                elementLabels = emptyMap()
            )
        }

        // Merge elementLabels with re-indexed keys
        val mergedLabels = mutableMapOf<Int, String>()
        var offset = 0
        results.forEach { result ->
            result.elementLabels.forEach { (idx, label) ->
                mergedLabels[idx + offset] = label
            }
            offset += result.elements.size
        }

        return ExplorationResult(
            packageName = results.map { it.packageName }.distinct().joinToString(", "),
            timestamp = System.currentTimeMillis(),
            duration = results.sumOf { it.duration },
            totalElements = results.sumOf { it.totalElements },
            clickableElements = results.sumOf { it.clickableElements },
            scrollableElements = results.sumOf { it.scrollableElements },
            elements = results.flatMap { it.elements },
            vuids = results.flatMap { it.vuids },
            hierarchy = results.flatMap { it.hierarchy },
            duplicates = results.flatMap { it.duplicates },
            deduplicationStats = DeduplicationStats(
                totalHashes = results.sumOf { it.deduplicationStats.totalHashes },
                uniqueHashes = results.sumOf { it.deduplicationStats.uniqueHashes },
                duplicateCount = results.sumOf { it.deduplicationStats.duplicateCount },
                duplicateElements = results.flatMap { it.deduplicationStats.duplicateElements }
            ),
            commands = results.flatMap { it.commands },
            avuOutput = results.joinToString("\n---\n") { it.avuOutput },
            elementLabels = mergedLabels
        )
    }
}

// Data classes for exploration results
data class ExplorationResult(
    val packageName: String,
    val timestamp: Long,
    val duration: Long,
    val totalElements: Int,
    val clickableElements: Int,
    val scrollableElements: Int,
    val elements: List<ElementInfo>,
    val vuids: List<VUIDInfo>,
    val hierarchy: List<HierarchyNode>,
    val duplicates: List<DuplicateInfo>,
    val deduplicationStats: DeduplicationStats,
    val commands: List<GeneratedCommand>,
    val avuOutput: String,
    val elementLabels: Map<Int, String> = emptyMap()  // index -> derived label (from self or child)
)

data class VUIDInfo(
    val element: ElementInfo,
    val vuid: String,
    val hash: String
)

data class HierarchyNode(
    val index: Int,
    val depth: Int,
    val parentIndex: Int?,
    val childCount: Int,
    val className: String
)

data class DuplicateInfo(
    val hash: String,
    val element: ElementInfo,
    val firstSeenIndex: Int
)

data class DeduplicationStats(
    val totalHashes: Int,
    val uniqueHashes: Int,
    val duplicateCount: Int,
    val duplicateElements: List<DuplicateInfo>
)

data class GeneratedCommand(
    val phrase: String,
    val alternates: List<String>,
    val targetVuid: String,
    val action: String,
    val element: ElementInfo,
    val derivedLabel: String = ""  // Label derived from child elements if parent has none
)
