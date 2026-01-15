package com.augmentalis.voiceoscoreng.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.VoiceOSCoreNGApplication
import com.augmentalis.voiceoscoreng.VoiceOSCoreNG
import com.augmentalis.voiceoscoreng.createForAndroid
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.common.CommandRegistry
import com.augmentalis.voiceoscoreng.common.ElementFingerprint
import com.augmentalis.voiceoscoreng.handlers.ServiceConfiguration
import com.augmentalis.voiceoscoreng.persistence.ICommandPersistence
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
 *
 * SOLID Refactored: Delegates to extracted managers:
 * - OverlayStateManager: Overlay state and UI preferences
 * - ElementExtractor: Accessibility tree traversal
 * - AVUFormatter: AVU output generation
 * - ScreenCacheManager: Screen hash and caching
 * - DynamicCommandGenerator: Voice command generation
 */
class VoiceOSAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Broadcast receiver for controlling numbers overlay via adb commands.
     * Usage: adb shell am broadcast -a com.augmentalis.voiceoscoreng.SET_NUMBERS_MODE --es mode "ON"
     */
    private val modeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_SET_NUMBERS_MODE) {
                val modeStr = intent.getStringExtra(EXTRA_MODE)?.uppercase() ?: return
                Log.d(TAG, "Received broadcast to set numbers mode: $modeStr")
                val mode = when (modeStr) {
                    "ON" -> OverlayStateManager.NumbersOverlayMode.ON
                    "OFF" -> OverlayStateManager.NumbersOverlayMode.OFF
                    "AUTO" -> OverlayStateManager.NumbersOverlayMode.AUTO
                    else -> return
                }
                OverlayStateManager.setNumbersOverlayMode(mode)
            }
        }
    }

    /**
     * Shared command registry - single source of truth.
     * Passed to VoiceOSCoreNG so both service and ActionCoordinator use the same instance.
     */
    private val commandRegistry = CommandRegistry()

    /**
     * Screen hash repository for caching known screens.
     */
    private val screenHashRepository: ScreenHashRepository = ScreenHashRepositoryImpl()

    /**
     * Screen cache manager for hash generation and caching logic.
     */
    private val screenCacheManager: ScreenCacheManager by lazy {
        ScreenCacheManager(screenHashRepository, resources)
    }

    /**
     * Dynamic command generator for voice command creation.
     */
    private val dynamicCommandGenerator: DynamicCommandGenerator by lazy {
        DynamicCommandGenerator(
            commandRegistry = commandRegistry,
            commandPersistence = commandPersistence,
            scrapedAppRepository = scrapedAppRepository,
            scrapedElementRepository = scrapedElementRepository,
            scope = serviceScope,
            getAppInfo = { packageName -> getAppInfo(packageName) }
        )
    }

    /**
     * Whether continuous scanning is enabled.
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

    /** Scraped app repository - for FK integrity */
    private val scrapedAppRepository: IScrapedAppRepository by lazy {
        VoiceOSCoreNGApplication.getInstance(applicationContext).scrapedAppRepository
    }

    /** Scraped element repository - for FK integrity */
    private val scrapedElementRepository: IScrapedElementRepository by lazy {
        VoiceOSCoreNGApplication.getInstance(applicationContext).scrapedElementRepository
    }

    /** VoiceOSCoreNG facade for voice command processing */
    private var voiceOSCore: VoiceOSCoreNG? = null

    companion object {
        private var instance: VoiceOSAccessibilityService? = null

        // Broadcast action for controlling numbers overlay via adb
        const val ACTION_SET_NUMBERS_MODE = "com.augmentalis.voiceoscoreng.SET_NUMBERS_MODE"
        const val EXTRA_MODE = "mode"  // "ON", "OFF", or "AUTO"

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

        // Delegate to ScreenCacheManager's StateFlow
        val currentScreenInfo: StateFlow<ScreenInfo?>
            get() = instance?.screenCacheManager?.currentScreenInfo ?: MutableStateFlow(null)

        // ===== Delegated to OverlayStateManager =====

        val TARGET_APPS get() = OverlayStateManager.TARGET_APPS

        // Type aliases for backward compatibility
        typealias AppNumbersPreference = OverlayStateManager.AppNumbersPreference
        typealias NumbersOverlayMode = OverlayStateManager.NumbersOverlayMode
        typealias NumberOverlayItem = OverlayStateManager.NumberOverlayItem
        typealias InstructionBarMode = OverlayStateManager.InstructionBarMode
        typealias BadgeTheme = OverlayStateManager.BadgeTheme

        // Delegated StateFlows
        val showAppDetectionDialog: StateFlow<String?> get() = OverlayStateManager.showAppDetectionDialog
        val currentDetectedAppName: StateFlow<String?> get() = OverlayStateManager.currentDetectedAppName
        val numberedOverlayItems: StateFlow<List<OverlayStateManager.NumberOverlayItem>> get() = OverlayStateManager.numberedOverlayItems
        val numbersOverlayMode: StateFlow<OverlayStateManager.NumbersOverlayMode> get() = OverlayStateManager.numbersOverlayMode
        val showNumbersOverlayComputed: StateFlow<Boolean> get() = OverlayStateManager.showNumbersOverlayComputed
        val instructionBarMode: StateFlow<OverlayStateManager.InstructionBarMode> get() = OverlayStateManager.instructionBarMode
        val badgeTheme: StateFlow<OverlayStateManager.BadgeTheme> get() = OverlayStateManager.badgeTheme

        // Delegated methods
        fun showAppDetectionDialogFor(packageName: String, appName: String) =
            OverlayStateManager.showAppDetectionDialogFor(packageName, appName)

        fun dismissAppDetectionDialog() = OverlayStateManager.dismissAppDetectionDialog()

        fun handleAppDetectionResponse(packageName: String, preference: OverlayStateManager.AppNumbersPreference) =
            OverlayStateManager.handleAppDetectionResponse(packageName, preference) { exploreAllApps() }

        fun setNumbersOverlayMode(mode: OverlayStateManager.NumbersOverlayMode) =
            OverlayStateManager.setNumbersOverlayMode(mode)

        fun cycleNumbersOverlayMode() = OverlayStateManager.cycleNumbersOverlayMode()

        fun setShowNumbersOverlay(show: Boolean) = OverlayStateManager.setShowNumbersOverlay(show)

        fun setInstructionBarMode(mode: OverlayStateManager.InstructionBarMode) =
            OverlayStateManager.setInstructionBarMode(mode)

        fun cycleInstructionBarMode() = OverlayStateManager.cycleInstructionBarMode()

        fun setBadgeTheme(theme: OverlayStateManager.BadgeTheme) = OverlayStateManager.setBadgeTheme(theme)

        fun cycleBadgeTheme() = OverlayStateManager.cycleBadgeTheme()

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
         */
        fun rescanCurrentApp() {
            instance?.serviceScope?.launch {
                val packageName = instance?.currentPackageName ?: return@launch
                val count = instance?.screenHashRepository?.clearScreensForPackage(packageName) ?: 0
                Log.d(TAG, "Rescan Current App: cleared $count screens for $packageName")
                instance?.performExploration()
            }
        }

        /**
         * Rescan everything - clears ALL cached screens.
         */
        fun rescanEverything() {
            instance?.serviceScope?.launch {
                val count = instance?.screenHashRepository?.clearAllScreens() ?: 0
                Log.d(TAG, "Rescan Everything: cleared $count total screens")
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

        // Setup OverlayStateManager preference callback
        OverlayStateManager.preferenceCallback = object : OverlayStateManager.PreferenceCallback {
            override fun saveAppNumbersPreference(packageName: String, preference: OverlayStateManager.AppNumbersPreference) {
                this@VoiceOSAccessibilityService.saveAppNumbersPreference(packageName, preference)
            }
        }

        try {
            serviceInfo = serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                        AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                notificationTimeout = 100
            }
            Log.d(TAG, "serviceInfo configured successfully with FLAG_RETRIEVE_INTERACTIVE_WINDOWS")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring serviceInfo", e)
        }

        // Register broadcast receiver for adb control
        val filter = IntentFilter(ACTION_SET_NUMBERS_MODE)
        registerReceiver(modeReceiver, filter, RECEIVER_EXPORTED)
        Log.d(TAG, "Registered broadcast receiver for numbers mode control")

        // Initialize VoiceOSCoreNG facade
        initializeVoiceOSCore()

        // Auto-start OverlayService if permission granted
        if (Settings.canDrawOverlays(this)) {
            Log.d(TAG, "Overlay permission granted, auto-starting OverlayService")
            OverlayService.start(this)
        } else {
            Log.w(TAG, "Overlay permission not granted - numbers overlay will not show")
        }
    }

    /**
     * Initialize the VoiceOSCoreNG facade with speech engine and handlers.
     */
    private fun initializeVoiceOSCore() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Initializing VoiceOSCoreNG facade...")

                voiceOSCore = VoiceOSCoreNG.createForAndroid(
                    service = this@VoiceOSAccessibilityService,
                    configuration = ServiceConfiguration(
                        autoStartListening = false,
                        speechEngine = "VIVOKA",
                        debugMode = true
                    ),
                    commandRegistry = commandRegistry
                )

                voiceOSCore?.initialize()
                Log.d(TAG, "VoiceOSCoreNG facade initialized successfully")

                // Auto-start voice listening
                try {
                    voiceOSCore?.startListening()
                    Log.d(TAG, "Voice listening auto-started")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to auto-start voice listening", e)
                }

                // Observe speech results
                voiceOSCore?.speechResults?.collect { speechResult ->
                    Log.d(TAG, "Speech result: ${speechResult.text} (confidence: ${speechResult.confidence})")
                    if (speechResult.isFinal) {
                        if (!handleVoiceOSControlCommand(speechResult.text.lowercase().trim())) {
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

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "Window state changed: ${event.packageName}")
                handleScreenChange(event.packageName?.toString())
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0) {
                    Log.v(TAG, "Window content changed (subtree): ${event.packageName}")
                }
            }
        }
    }

    /**
     * Handle VoiceOS control commands.
     */
    private fun handleVoiceOSControlCommand(command: String): Boolean {
        return when (command) {
            "numbers on", "show numbers", "numbers always" -> {
                OverlayStateManager.setNumbersOverlayMode(OverlayStateManager.NumbersOverlayMode.ON)
                Log.d(TAG, "Voice command: Numbers overlay ON")
                true
            }
            "numbers off", "hide numbers", "no numbers" -> {
                OverlayStateManager.setNumbersOverlayMode(OverlayStateManager.NumbersOverlayMode.OFF)
                Log.d(TAG, "Voice command: Numbers overlay OFF")
                true
            }
            "numbers auto", "numbers automatic", "auto numbers" -> {
                OverlayStateManager.setNumbersOverlayMode(OverlayStateManager.NumbersOverlayMode.AUTO)
                Log.d(TAG, "Voice command: Numbers overlay AUTO")
                true
            }
            else -> false
        }
    }

    /**
     * Handle screen change with debouncing and caching.
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

        currentPackageName = packageName

        // Check for target app dialog
        if (packageName != null && OverlayStateManager.TARGET_APPS.contains(packageName)) {
            checkAndShowAppDetectionDialog(packageName)
        }

        if (!continuousScanningEnabled.get()) {
            commandRegistry.clear()
            Log.d(TAG, "Screen changed to $packageName - manual mode, awaiting user scan")
            return
        }

        // Continuous monitoring mode
        if (packageName != null && packageName != "unknown") {
            serviceScope.launch {
                try {
                    val rootNode = rootInActiveWindow
                    if (rootNode == null) {
                        Log.w(TAG, "No active window for screen hash")
                        commandRegistry.clear()
                        return@launch
                    }

                    val screenHash = screenCacheManager.generateScreenHash(rootNode)
                    rootNode.recycle()

                    val isKnown = screenCacheManager.hasScreen(screenHash)
                    val appVersion = getAppInfo(packageName).versionName
                    val storedVersion = screenCacheManager.getAppVersion(screenHash)

                    if (isKnown && appVersion == storedVersion) {
                        val cachedCommands = screenCacheManager.getCommandsForScreen(screenHash)
                        if (cachedCommands.isNotEmpty()) {
                            commandRegistry.updateSync(cachedCommands)
                            currentScreenHash = screenHash
                            Log.d(TAG, "Screen known - loaded ${cachedCommands.size} cached commands for $packageName")

                            val screenInfo = screenCacheManager.getScreenInfo(screenHash)
                            screenCacheManager.updateCurrentScreenInfo(screenInfo)
                            return@launch
                        }
                    }

                    Log.d(TAG, "Screen changed to $packageName - ${if (isKnown) "version changed, rescanning" else "new screen, scanning"}")
                    commandRegistry.clear()
                    currentScreenHash = screenHash

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

    // ===== App Detection Helpers =====

    private val PREFS_NAME = "voiceos_app_prefs"
    private val PREF_KEY_PREFIX = "app_numbers_mode_"

    private fun checkAndShowAppDetectionDialog(packageName: String) {
        val pref = getAppNumbersPreference(packageName)
        if (pref == OverlayStateManager.AppNumbersPreference.ASK) {
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName.substringAfterLast(".")
            }
            OverlayStateManager.showAppDetectionDialogFor(packageName, appName)
        } else {
            when (pref) {
                OverlayStateManager.AppNumbersPreference.ALWAYS ->
                    OverlayStateManager.setNumbersOverlayMode(OverlayStateManager.NumbersOverlayMode.ON)
                OverlayStateManager.AppNumbersPreference.AUTO ->
                    OverlayStateManager.setNumbersOverlayMode(OverlayStateManager.NumbersOverlayMode.AUTO)
                OverlayStateManager.AppNumbersPreference.NEVER ->
                    OverlayStateManager.setNumbersOverlayMode(OverlayStateManager.NumbersOverlayMode.OFF)
                else -> {}
            }
        }
    }

    private fun getAppNumbersPreference(packageName: String): OverlayStateManager.AppNumbersPreference {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedValue = prefs.getString("$PREF_KEY_PREFIX$packageName", null)
        return savedValue?.let {
            try {
                OverlayStateManager.AppNumbersPreference.valueOf(it)
            } catch (e: Exception) {
                OverlayStateManager.AppNumbersPreference.ASK
            }
        } ?: OverlayStateManager.AppNumbersPreference.ASK
    }

    internal fun saveAppNumbersPreference(packageName: String, preference: OverlayStateManager.AppNumbersPreference) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("$PREF_KEY_PREFIX$packageName", preference.name)
            .apply()
        Log.d(TAG, "Saved app numbers preference: $packageName -> $preference")
    }

    /**
     * Perform exploration and cache results.
     */
    private fun performExplorationWithCache(screenHash: String, packageName: String, appVersion: String) {
        serviceScope.launch {
            try {
                val rootNode = rootInActiveWindow ?: return@launch

                val result = exploreNode(rootNode)
                _explorationResults.value = result
                rootNode.recycle()

                val commands = commandRegistry.all()

                // Cache the screen
                screenCacheManager.saveScreen(
                    hash = screenHash,
                    packageName = packageName,
                    activityName = null,
                    appVersion = appVersion,
                    elementCount = result.totalElements
                )
                screenCacheManager.saveCommandsForScreen(screenHash, commands)

                val screenInfo = screenCacheManager.createScreenInfo(
                    hash = screenHash,
                    packageName = packageName,
                    activityName = null,
                    appVersion = appVersion,
                    elementCount = result.totalElements,
                    actionableCount = result.clickableElements + result.scrollableElements,
                    commandCount = commands.size,
                    isCached = false
                )
                screenCacheManager.updateCurrentScreenInfo(screenInfo)

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

        try {
            unregisterReceiver(modeReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver", e)
        }

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
     * Perform exploration of the currently focused app.
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
     * Perform exploration across all windows.
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

                val merged = mergeResults(allResults)
                _explorationResults.value = merged
                Log.d(TAG, "performFullExploration() complete: ${merged.totalElements} total elements from ${allResults.size} windows")

            } catch (e: Exception) {
                Log.e(TAG, "Full exploration failed", e)
                _lastError.value = "Full exploration failed: ${e.message}"
            }
        }
    }

    /**
     * Explore a single accessibility node tree.
     */
    private suspend fun exploreNode(rootNode: AccessibilityNodeInfo): ExplorationResult {
        val startTime = System.currentTimeMillis()

        // Extract elements using ElementExtractor
        val elements = mutableListOf<com.augmentalis.voiceoscoreng.common.ElementInfo>()
        val hierarchy = mutableListOf<HierarchyNode>()
        val seenHashes = mutableSetOf<String>()
        val duplicates = mutableListOf<DuplicateInfo>()

        ElementExtractor.extractElements(rootNode, elements, hierarchy, seenHashes, duplicates, depth = 0)

        val packageName = rootNode.packageName?.toString() ?: "unknown"

        // Generate VUIDs
        val vuids = elements.map { element ->
            val fingerprint = ElementFingerprint.generate(
                className = element.className,
                packageName = packageName,
                resourceId = element.resourceId,
                text = element.text,
                contentDesc = element.contentDescription
            )
            val elemHash = ElementFingerprint.parse(fingerprint)?.second ?: ""
            VUIDInfo(element = element, vuid = fingerprint, hash = elemHash)
        }

        // Derive labels using ElementExtractor
        val elementLabels = ElementExtractor.deriveElementLabels(elements, hierarchy)

        // Generate commands using DynamicCommandGenerator
        val commandResult = dynamicCommandGenerator.generateCommands(
            elements = elements,
            hierarchy = hierarchy,
            elementLabels = elementLabels,
            packageName = packageName,
            updateSpeechEngine = { phrases ->
                serviceScope.launch {
                    try {
                        voiceOSCore?.updateCommands(phrases)
                        Log.d(TAG, "Updated speech engine with ${phrases.size} command phrases")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update speech engine commands", e)
                    }
                }
            }
        )

        // Generate AVU output using AVUFormatter
        val avuOutput = AVUFormatter.generateAVU(packageName, elements, elementLabels, commandResult.legacyCommands)

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
                totalHashes = elements.size,
                uniqueHashes = seenHashes.size,
                duplicateCount = duplicates.size,
                duplicateElements = duplicates
            ),
            commands = commandResult.legacyCommands,
            avuOutput = avuOutput,
            elementLabels = elementLabels
        )
    }

    /**
     * Get app version info for the given package name.
     */
    private fun getAppInfo(packageName: String): DynamicCommandGenerator.AppVersionInfo {
        return DynamicCommandGenerator.getAppInfoFromPackageManager(packageManager, packageName)
    }

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

// Data classes moved to ExplorationModels.kt for SOLID compliance
