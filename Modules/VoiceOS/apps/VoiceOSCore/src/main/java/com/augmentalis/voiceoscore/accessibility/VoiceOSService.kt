/**
 * VoiceOSService.kt - VoiceOS accessibility service implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-02
 */
package com.augmentalis.voiceoscore.accessibility

// UI components will be implemented later
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription.Builder
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.ArrayMap
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.augmentalis.commandmanager.CommandManager
// TEMP DISABLED: import com.augmentalis.commandmanager.database.CommandDatabase
import com.augmentalis.voiceoscore.learnapp.integration.LearnAppIntegration
import com.augmentalis.voiceoscore.learnapp.integration.CommandDiscoveryIntegration
import com.augmentalis.voiceoscore.learnapp.ui.RenameHintOverlay
import com.augmentalis.voiceoscore.learnapp.detection.ScreenActivityDetector
import com.augmentalis.voiceoscore.learnapp.commands.RenameCommandHandler
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.voiceos.command.Command
import com.augmentalis.voiceos.command.CommandContext
import com.augmentalis.voiceos.command.CommandSource
import com.augmentalis.voiceos.constants.VoiceOSConstants
import com.augmentalis.voiceos.cursor.VoiceCursorAPI
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceoscore.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.UIElement
import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
import com.augmentalis.voiceoscore.accessibility.managers.DatabaseManager
import com.augmentalis.voiceoscore.accessibility.managers.InstalledAppsManager
import com.augmentalis.voiceoscore.accessibility.managers.IPCManager
import com.augmentalis.voiceoscore.accessibility.monitor.ServiceMonitor
import com.augmentalis.voiceoscore.accessibility.speech.SpeechConfigurationData
import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
import com.augmentalis.voiceoscore.accessibility.utils.Const
import com.augmentalis.voiceoscore.accessibility.utils.Debouncer
import com.augmentalis.voiceoscore.accessibility.utils.EventPriorityManager
import com.augmentalis.voiceoscore.accessibility.utils.ResourceMonitor
import com.augmentalis.voiceoscore.config.DynamicPackageConfig
// WebScrapingDatabase removed - migrated to SQLDelight (VoiceOSDatabaseManager)
import com.augmentalis.voiceoscore.scraping.AccessibilityScrapingIntegration
import com.augmentalis.voiceoscore.scraping.VoiceCommandProcessor
// FIX (2025-12-01): VoiceOSAppDatabase is typealias for VoiceOSCoreDatabaseAdapter
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.database.VoiceOSCoreDatabaseAdapter
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.version.AppVersionDetector
import com.augmentalis.voiceoscore.web.WebCommandCoordinator
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException

/**
 * VoiceOS Service - Main accessibility service implementation
 * High-performance service with efficient data structures, lazy loading, and caching
 * Now with hybrid ForegroundService approach for optimal battery and memory usage
 *
 * HILT Integration:
 * - NOTE: @AndroidEntryPoint does NOT support AccessibilityService
 * - Using manual dependency injection via lazy initialization
 * - Dependencies are initialized in onCreate() or on first use
 */
// @dagger.hilt.android.AndroidEntryPoint - DISABLED: Hilt doesn't support AccessibilityService
class VoiceOSService : AccessibilityService(), DefaultLifecycleObserver, IVoiceOSService, IVoiceOSContext {

    companion object {
        private const val TAG = "VoiceOSService"

        // Mic service actions (foreground service for background mic access)
        private const val ACTION_START_MIC = "com.augmentalis.voiceos.START_MIC"
        private const val ACTION_STOP_MIC = "com.augmentalis.voiceos.STOP_MIC"

        // YOLO Phase 2 - High Priority Issue #11: Dynamic package configuration
        // Moved to DynamicPackageConfig for device-specific flexibility
        // Valid packages now determined at runtime based on device manufacturer
        const val COMMAND_CHECK_INTERVAL_MS = VoiceOSConstants.Timing.THROTTLE_DELAY_MS
        const val COMMAND_LOAD_DEBOUNCE_MS = VoiceOSConstants.Timing.THROTTLE_DELAY_MS

        @Volatile
        private var instanceRef: WeakReference<VoiceOSService>? = null

        @JvmStatic
        fun getInstance(): VoiceOSService? = instanceRef?.get()

        @JvmStatic
        fun isServiceRunning(): Boolean = instanceRef?.get() != null

        @JvmStatic
        fun executeCommand(commandText: String): Boolean {
            val service = instanceRef?.get() ?: return false
            val command = commandText.lowercase().trim()
            val result = when (command) {
                "back", "go back" -> service.performGlobalAction(GLOBAL_ACTION_BACK)
                "home", "go home" -> service.performGlobalAction(GLOBAL_ACTION_HOME)
                "recent", "recent apps" -> service.performGlobalAction(GLOBAL_ACTION_RECENTS)
                "notifications" -> service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                "settings", "quick settings" -> service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                "power", "power menu" -> service.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)

                // Screenshot (Android P+)
                "screenshot" -> {
                    service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
                // For complex commands, use handler architecture
                else -> false
            }
            return result
        }
    }

    // Service state
    @JvmField
    internal var isServiceReady = false  // Phase 3: Exposed for IPC companion service (Java-accessible)
    // FIX (2025-12-11): Changed from Dispatchers.Main to Dispatchers.Default to prevent ANR
    // Root cause: Command cache operations (300+ items) blocked main thread for >5 seconds
    // Solution: Move all non-UI operations off main thread
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val coroutineScopeCommands = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var isVoiceInitialized = false
    private var lastCommandLoaded = 0L
    private val isCommandProcessing = AtomicBoolean(false)

    // Database manager (extracted from VoiceOSService - P2-8a)
    // Centralizes database initialization, state machine, and lifecycle
    private val dbManager by lazy {
        DatabaseManager(applicationContext).also {
            Log.d(TAG, "DatabaseManager initialized (lazy)")
        }
    }

    // IPC manager (extracted from VoiceOSService - P2-8b)
    // Handles all inter-process communication from external apps/services
    private val ipcManager by lazy {
        IPCManager(
            accessibilityService = this,
            speechEngineManager = speechEngineManager,
            uiScrapingEngine = uiScrapingEngine,
            databaseManager = dbManager,
            isServiceReady = { isServiceReady }
        ).also {
            Log.d(TAG, "IPCManager initialized (lazy)")
        }
    }

    // LearnApp integration state
    // FIX (2025-11-30): Use AtomicInteger for thread-safe state tracking
    // State: 0=not started, 1=in progress, 2=complete
    private val learnAppInitState = AtomicInteger(0)
    @Volatile
    private var learnAppInitialized = false  // Keep for backward compatibility with debug logs

    // FIX (2025-12-10): Event queue to buffer events during initialization
    // Prevents event loss in first 500-1000ms after service starts
    private val pendingEvents = java.util.concurrent.ConcurrentLinkedQueue<android.view.accessibility.AccessibilityEvent>()
    private val MAX_QUEUED_EVENTS = 50

    // PHASE 3 (2025-12-08): Command Discovery integration
    // Auto-observes ExplorationEngine.state() and triggers discovery flow on completion
    private var discoveryIntegration: CommandDiscoveryIntegration? = null

    // Hybrid foreground service state
    private var foregroundServiceActive = false
    private var appInBackground = false
    private var voiceSessionActive = false

    // Configuration
    private lateinit var config: ServiceConfiguration

    private val nodeCache: MutableList<UIElement> = CopyOnWriteArrayList()
    private val commandCache: MutableList<String> = CopyOnWriteArrayList()
    private val staticCommandCache: MutableList<String> = CopyOnWriteArrayList()
    private val appsCommand = ConcurrentHashMap<String, String>()
    private val allRegisteredDynamicCommands: MutableList<String> = CopyOnWriteArrayList()

    // TODO: UI components to be implemented later
    // private val floatingMenu by lazy { FloatingMenu(this) }
    // private val cursorOverlay by lazy { CursorOverlay(this) }

    // Overlay manager for voice feedback (numbered selection, context menus, help)
    private val overlayManager by lazy {
        com.augmentalis.voiceoscore.accessibility.overlays.OverlayManager.getInstance(this).also {
            Log.d(TAG, "OverlayManager initialized (lazy)")
        }
    }

    private val prettyGson by lazy { GsonBuilder().setPrettyPrinting().create() }

    // Manual dependency initialization (Hilt does not support AccessibilityService)
    private val speechEngineManager by lazy {
        SpeechEngineManager(applicationContext).also {
            Log.d(TAG, "SpeechEngineManager initialized (lazy)")
        }
    }

    private val installedAppsManager by lazy {
        InstalledAppsManager(applicationContext).also {
            Log.d(TAG, "InstalledAppsManager initialized (lazy)")
        }
    }

    // AppVersionDetector requires IAppVersionRepository
    private val appVersionDetector by lazy {
        AppVersionDetector(applicationContext, dbManager.sqlDelightManager.appVersions).also {
            Log.d(TAG, "AppVersionDetector initialized (lazy)")
        }
    }

    // AppVersionManager requires detector, version repo, and command repo
    private val appVersionManager by lazy {
        com.augmentalis.voiceoscore.version.AppVersionManager(
            context = applicationContext,
            detector = appVersionDetector,
            versionRepo = dbManager.sqlDelightManager.appVersions,
            commandRepo = dbManager.sqlDelightManager.generatedCommands
        ).also {
            Log.d(TAG, "AppVersionManager initialized (lazy)")
        }
    }

    // UIScrapingEngine requires AccessibilityService, so it's lazy-initialized (not injected)
    private val uiScrapingEngine by lazy {
        UIScrapingEngine(this).also {
            Log.d(TAG, "UIScrapingEngine initialized (lazy)")
        }
    }

    // Event type tracking for performance monitoring
    private val eventCounts = ArrayMap<Int, AtomicLong>().apply {
        put(AccessibilityEvent.TYPE_VIEW_CLICKED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_VIEW_FOCUSED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_VIEW_SCROLLED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AtomicLong(0))
    }

    // Optimized managers
    private val actionCoordinator by lazy {
        ActionCoordinator(this).also {
            Log.d(TAG, "ActionCoordinator initialized (lazy)")
        }
    }

    // VoiceCursor API for cursor functionality
    private var voiceCursorInitialized = false

    // LearnApp integration for third-party app learning
    // FIX (2025-11-30): Add @Volatile for thread visibility across coroutine and event handler
    @Volatile
    private var learnAppIntegration: com.augmentalis.voiceoscore.learnapp.integration.LearnAppIntegration? = null

    // JIT Learning Service connection (Phase 3: JIT-LearnApp Separation - 2025-12-18)
    // Manages binding to JITLearningService foreground service
    @Volatile
    private var jitServiceBound = false

    // Hash-based scraping integration
    private var scrapingIntegration: AccessibilityScrapingIntegration? = null

    // Hash-based command processor
    private var voiceCommandProcessor: VoiceCommandProcessor? = null

    // Web command coordinator for browser integration
    private val webCommandCoordinator by lazy {
        WebCommandCoordinator(applicationContext, this).also {
            Log.d(TAG, "WebCommandCoordinator initialized (lazy)")
        }
    }

    // Rename feature components (Phase 2: On-Demand Command Renaming)
    private var renameHintOverlay: RenameHintOverlay? = null
    private var screenActivityDetector: ScreenActivityDetector? = null
    private var renameCommandHandler: RenameCommandHandler? = null

    // Event debouncing to prevent excessive scraping in apps with dynamic content
    private val eventDebouncer = Debouncer(VoiceOSConstants.Timing.EVENT_DEBOUNCE_MS)

    // Phase 1: CommandManager and ServiceMonitor integration
    private var commandManagerInstance: CommandManager? = null
    private var serviceMonitor: ServiceMonitor? = null
    private var fallbackModeEnabled = false

    // Phase 3D: Resource monitoring
    private val resourceMonitor by lazy {
        ResourceMonitor(applicationContext).also {
            Log.d(TAG, "ResourceMonitor initialized (lazy)")
        }
    }

    // Phase 3E: Event priority management for adaptive filtering
    private val eventPriorityManager by lazy {
        EventPriorityManager().also {
            Log.d(TAG, "EventPriorityManager initialized (lazy)")
        }
    }

    override fun onCreate() {
        super<AccessibilityService>.onCreate()
        instanceRef = WeakReference(this)

        // FIX (2025-12-04): Add LeakCanary memory monitoring for VoiceOSService
        // This will detect memory leaks in LearnApp components (ProgressOverlay, etc.)
        // Using reflection to avoid compile-time dependency on debug-only library
        if (com.augmentalis.voiceoscore.BuildConfig.DEBUG) {
            try {
                val appWatcherClass = Class.forName("leakcanary.AppWatcher")
                val objectWatcherField = appWatcherClass.getDeclaredField("objectWatcher")
                val objectWatcher = objectWatcherField.get(null)
                val watchMethod = objectWatcher.javaClass.getMethod("watch", Any::class.java, String::class.java)
                watchMethod.invoke(objectWatcher, this, "VoiceOSService should be destroyed when service stops")
                Log.d(TAG, "✓ LeakCanary monitoring enabled for VoiceOSService")
            } catch (e: Exception) {
                Log.w(TAG, "LeakCanary not available (this is OK for release builds): ${e.message}")
            }
        }

        // Check overlay permission for LearnApp consent dialogs
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted")
            Log.w(TAG, "LearnApp consent dialogs will be limited until permission is granted")
            Log.i(TAG, "User must grant permission in: Settings → Apps → VoiceOS → Display over other apps")
        } else {
            Log.d(TAG, "✓ SYSTEM_ALERT_WINDOW permission granted")
        }

        // Database initialization moved to onServiceConnected (via DatabaseManager)
        // Initialize rename feature components (Phase 2: On-Demand Command Renaming)
        initializeRenameFeature()
    }

    /**
     * Initialize rename feature components
     *
     * Initializes:
     * 1. RenameHintOverlay - Shows contextual hints when screen has generated labels
     * 2. ScreenActivityDetector - Detects screen changes and triggers hints
     * 3. RenameCommandHandler - Processes "Rename X to Y" voice commands
     */
    private fun initializeRenameFeature() {
        try {
            Log.i(TAG, "=== Initializing Rename Feature ===")

            // Initialize RenameHintOverlay
            renameHintOverlay = RenameHintOverlay(this, null)
            Log.d(TAG, "✓ RenameHintOverlay initialized")

            // Initialize ScreenActivityDetector (requires RenameHintOverlay + database)
            dbManager.scrapingDatabase?.let { database ->
                renameHintOverlay?.let { overlay ->
                    screenActivityDetector = ScreenActivityDetector(this, database.databaseManager, overlay)
                    Log.d(TAG, "✓ ScreenActivityDetector initialized")
                } ?: Log.w(TAG, "RenameHintOverlay not initialized, skipping ScreenActivityDetector")
            } ?: Log.w(TAG, "Database not initialized, skipping ScreenActivityDetector")

            // Initialize RenameCommandHandler (requires database + TTS)
            // Note: TTS is initialized later in speech engine, so we'll initialize handler on-demand
            // when first rename command is received
            Log.d(TAG, "RenameCommandHandler will be initialized on-demand when TTS is ready")

            Log.i(TAG, "=== Rename Feature Initialized ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing rename feature", e)
            renameHintOverlay = null
            screenActivityDetector = null
            renameCommandHandler = null
        }
    }

    /**
     * Show error notification to user.
     * Used for critical initialization failures.
     *
     * FIX (2025-12-19): Task 1.10 - Database initialization validation
     */
    private fun showErrorNotification(message: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "voiceos_errors",
                    "VoiceOS Errors",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical VoiceOS error notifications"
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Build and show notification
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.Notification.Builder(this, "voiceos_errors")
            } else {
                @Suppress("DEPRECATION")
                android.app.Notification.Builder(this)
            }
                .setContentTitle("VoiceOS Error")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show error notification", e)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "VoiceOS Service connected")

        // Initialize configuration
        config = ServiceConfiguration.loadFromPreferences(this)

        configureServiceInfo()

        // Register for app lifecycle events for hybrid foreground service
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        serviceScope.launch {
            // P2-8a: Database initialization via DatabaseManager
            try {
                dbManager.initialize()
            } catch (e: Exception) {
                val errorMsg = "VoiceOS initialization failed: ${e.message}"
                Log.e(TAG, errorMsg, e)
                showErrorNotification(errorMsg)
                return@launch // Exit early on failure
            }

            // Continue with normal initialization only if database is ready
            staticCommandCache.addAll(actionCoordinator.getAllActions())
            observeInstalledApps()
            delay(VoiceOSConstants.Timing.INIT_DELAY_MS) // Small delay to not block service startup
            initializeComponents()
            // Initialize VoiceCursor API
            initializeVoiceCursor()
            // NOTE: LearnApp initialization deferred until first accessibility event
            // This ensures FLAG_RETRIEVE_INTERACTIVE_WINDOWS has been fully processed by Android
            Log.i(TAG, "LearnApp initialization deferred until first accessibility event")
            // Phase 1: Initialize CommandManager and ServiceMonitor
            initializeCommandManager()
            // register voice command
            registerVoiceCmd()

            // Version-aware command management (2025-12-14)
            // Initialize version tracking and cleanup scheduling
            initializeVersionManagement()

            val filter = IntentFilter(Const.ACTION_CONFIG_UPDATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.i(TAG, "onServiceConnected registerReceiver : CHANGE_LANG ")
                registerReceiver(serviceReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                Log.i(TAG, "onServiceConnected registerReceiver : CHANGE_LANG ")
                registerReceiver(serviceReceiver, filter)
            }
        }
    }

    /**
     * Phase 1: Initialize CommandManager with ServiceMonitor
     * Based on Q1 Decision: Service Monitor with Reconnection Callback
     */
    private fun initializeCommandManager() {
        try {
            Log.i(TAG, "Initializing CommandManager and ServiceMonitor...")

            // Initialize CommandManager
            commandManagerInstance = CommandManager.getInstance(this)
            commandManagerInstance?.initialize()

            // Initialize ServiceMonitor
            serviceMonitor = ServiceMonitor(this, applicationContext)
            commandManagerInstance?.let { manager ->
                serviceMonitor?.bindCommandManager(manager)
                serviceMonitor?.startHealthCheck()
            }

            Log.i(TAG, "CommandManager and ServiceMonitor initialized successfully")

            // Register database commands with speech engine
            serviceScope.launch {
                // Small delay to ensure all systems initialized
                delay(500)
                Log.i(TAG, "Starting database command registration...")
                registerDatabaseCommands()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CommandManager/ServiceMonitor", e)
            commandManagerInstance = null
            serviceMonitor = null
        }
    }

    /**
     * Initialize version-aware command lifecycle management.
     *
     * - Schedules weekly CleanupWorker for automatic deprecated command removal
     * - Checks all installed apps and updates version tracking database
     * - Enables version detection for future command generation
     *
     * @since 5.1 (Version-Aware Command Lifecycle)
     */
    private fun initializeVersionManagement() {
        try {
            Log.i(TAG, "Initializing version-aware command management...")

            // Schedule weekly cleanup worker (runs when device is charging + battery not low)
            com.augmentalis.voiceoscore.cleanup.CleanupWorker.schedulePeriodicCleanup(applicationContext)
            Log.i(TAG, "Scheduled weekly command cleanup worker")

            // Check all tracked apps and update version tracking database
            // This runs in background to avoid blocking service startup
            serviceScope.launch {
                try {
                    Log.d(TAG, "Checking versions for all tracked apps...")
                    val processedCount = appVersionManager.checkAllTrackedApps()

                    Log.i(TAG, "Version check complete: $processedCount apps processed")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check tracked app versions", e)
                }
            }

            Log.i(TAG, "Version management initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize version management", e)
        }
    }

    /**
     * Register database commands with speech engine
     *
     * Loads commands from multiple sources and registers them with the speech
     * recognition engine so users can speak them.
     *
     * Sources:
     * 1. CommandDatabase - VOSCommandIngestion data (94 commands)
     * 2. VoiceOSAppDatabase - Generated app-specific commands (unified DB)
     * 3. WebScrapingDatabase - Learned web commands
     *
     * This method should be called after CommandManager initialization.
     */
    private suspend fun registerDatabaseCommands() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "=== Database Command Registration Start ===")

            // Get current locale for filtering
            var locale = Locale.getDefault().toString() // e.g., "en_US"
            //normalize localized name to support .VOS static commands
            if (locale.contains("_")) {
                locale = locale.replace("_", "-")
            }
            Log.d(TAG, "Current locale: $locale")

            // Set to collect all command texts (uses Set to avoid duplicates)
            val commandTexts = mutableSetOf<String>()

            // SOURCE 1: CommandDatabase (VOSCommandIngestion data)
            // TEMP DISABLED: CommandDatabase still uses Room (to be migrated by Agent 5)
            /*
            try {
                Log.d(TAG, "Loading commands from CommandDatabase...")
                val commandDatabase = CommandDatabase.getInstance(applicationContext)

                val dbCommands = commandDatabase.voiceCommandDao().getCommandsForLocale(locale)
                Log.i(TAG, "  Found ${dbCommands.size} commands in CommandDatabase for locale $locale")

                dbCommands.forEach { cmd ->
                    // Add primary text
                    commandTexts.add(cmd.primaryText.lowercase().trim())

                    // Add synonyms (stored as JSON array string)
                    try {
                        val synonymsJson = JSONArray(cmd.synonyms)
                        for (i in 0 until synonymsJson.length()) {
                            val synonym = synonymsJson.getString(i).lowercase().trim()
                            commandTexts.add(synonym)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "  Error parsing synonyms for '${cmd.primaryText}': ${e.message}")
                    }
                }

                Log.i(TAG, "  ✓ CommandDatabase: ${commandTexts.size} command texts loaded")

            } catch (e: Exception) {
                Log.e(TAG, "  ✗ Error loading CommandDatabase commands", e)
                // Continue with other sources even if this fails
            }
            */
            Log.d(TAG, "CommandDatabase temporarily disabled (Room migration pending)")

            // SOURCE 2: VoiceOSAppDatabase (generated app commands - unified DB)
            try {
                Log.d(TAG, "Loading commands from VoiceOSAppDatabase...")
                dbManager.scrapingDatabase?.let { database ->
                    val appCommands = database.databaseManager.generatedCommands.getAll()
                    Log.i(TAG, "  Found ${appCommands.size} commands in VoiceOSAppDatabase")

                    appCommands.forEach { cmd ->
                        // Add command text
                        commandTexts.add(cmd.commandText.lowercase().trim())

                        // Add synonyms if any
                        try {
                            val synonymsJson = JSONArray(cmd.synonyms ?: "[]")
                            for (i in 0 until synonymsJson.length()) {
                                val synonym = synonymsJson.getString(i).lowercase().trim()
                                commandTexts.add(synonym)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "  Error parsing synonyms for '${cmd.commandText}': ${e.message}")
                        }
                    }

                    Log.i(TAG, "  ✓ VoiceOSAppDatabase: Total ${commandTexts.size} command texts")

                } ?: Log.w(TAG, "  VoiceOSAppDatabase not available, skipping")

            } catch (e: Exception) {
                Log.e(TAG, "  ✗ Error loading VoiceOSAppDatabase commands", e)
                // Continue even if this fails
            }

            // SOURCE 3: Web commands (via SQLDelight - not yet migrated)
            // Web scraping database schemas created but not integrated into VoiceOSDatabaseManager yet
            // TODO: Integrate GeneratedWebCommand.sq when LearnWeb migration is complete
            Log.d(TAG, "Web commands: Not yet available (LearnWeb migration pending)")

            // Remove any empty strings or invalid commands
            commandTexts.removeIf { it.isBlank() || it.length < 2 }

            Log.i(TAG, "Total unique command texts to register: ${commandTexts.size}")

            if (commandTexts.isEmpty()) {
                Log.w(TAG, "No database commands found to register")
                Log.w(TAG, "  This is normal on first run before any apps are scraped")
                return@withContext
            }

            // Register with speech engine on Main thread
            withContext(Dispatchers.Main) {
                try {
                    Log.d(TAG, "Adding command texts to staticCommandCache...")
                    staticCommandCache.addAll(commandTexts)
                    Log.i(TAG, "  staticCommandCache size: ${staticCommandCache.size}")

                    Log.d(TAG, "Updating speech engine vocabulary...")
                    val allCommands = commandCache + staticCommandCache + appsCommand.keys
//                    if (BuildConfig.DEBUG) {
//                        val objectCommand = prettyGson.toJson(allCommands)
//                        Log.d(TAG, "RegisterVoiceCmd allCommands = $objectCommand")
//                    }
                    // FIX (2025-12-11): updateCommands is now suspend, launch in coroutine
                    coroutineScopeCommands.launch {
                        speechEngineManager.updateCommands(allCommands)
                    }

                    Log.i(TAG, "✓ Database commands registered successfully with speech engine (async)")
                    Log.i(TAG, "  Total commands in speech vocabulary: ${allCommands.toSet().size}")

                } catch (e: Exception) {
                    Log.e(TAG, "✗ Error updating speech engine vocabulary", e)
                }
            }

            Log.i(TAG, "=== Database Command Registration Complete ===")

        } catch (e: Exception) {
            Log.e(TAG, "✗ Fatal error in registerDatabaseCommands()", e)
        }
    }

    /**
     * Called when new commands are generated (e.g., after app scraping)
     * Triggers re-registration of database commands
     */
    override fun onNewCommandsGenerated() {
        Log.i(TAG, "New commands generated, re-registering with speech engine...")
        serviceScope.launch {
            registerDatabaseCommands()
        }
    }

    private fun configureServiceInfo() {
        try {
            serviceInfo?.let { info ->
                // Configure service capabilities
                info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                info.flags = info.flags or
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS  // Required for getWindows() API

                // Add accessibility button support on Android O+
                info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON

                // Add fingerprint gesture support on Android O+
                if (config.fingerprintGesturesEnabled) {
                    info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES
                }

                // DIAGNOSTIC: Verify FLAG_RETRIEVE_INTERACTIVE_WINDOWS is set
                val hasInteractiveWindowsFlag = (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS) != 0
                Log.i(TAG, "Service configured - FLAG_RETRIEVE_INTERACTIVE_WINDOWS: $hasInteractiveWindowsFlag")
                Log.d(TAG, "Service info flags value: ${info.flags}")

                if (!hasInteractiveWindowsFlag) {
                    Log.e(TAG, "CRITICAL: FLAG_RETRIEVE_INTERACTIVE_WINDOWS not set! Windows will be unavailable!")
                }

                Log.d(TAG, "Service info configured")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure service info", e)
        }
    }

    private fun observeInstalledApps() {
        serviceScope.launch {
            withContext(Dispatchers.Main) {
                installedAppsManager.appList.collectLatest { result ->
                    if (result.isNotEmpty()) {
                        appsCommand.apply {
                            clear()
                            putAll(result)
                        }
                    }
                }
            }
        }
    }

    // Lifecycle observer methods for hybrid foreground service management
    override fun onStart(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStart(owner)
        Log.d(TAG, "App moved to foreground")
        appInBackground = false
        evaluateForegroundServiceNeed()
    }

    override fun onStop(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStop(owner)
        Log.d(TAG, "App moved to background")
        appInBackground = true
        evaluateForegroundServiceNeed()
    }

    /**
     * Initialize components with staggered loading
     */
    private suspend fun initializeComponents() = withContext(Dispatchers.Main) {
        try {
            // Initialize core components first
            actionCoordinator.initialize()

            // Initialize hash-based scraping integration (if database initialized)
            if (dbManager.scrapingDatabase != null) {
                try {
                    scrapingIntegration = AccessibilityScrapingIntegration(
                        context = this@VoiceOSService,
                        accessibilityService = this@VoiceOSService
                    )
                    Log.i(TAG, "AccessibilityScrapingIntegration initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize AccessibilityScrapingIntegration", e)
                    scrapingIntegration = null
                }
            } else {
                Log.w(TAG, "Skipping AccessibilityScrapingIntegration (database not initialized)")
            }

            // Initialize hash-based command processor
            if (dbManager.scrapingDatabase != null) {
                try {
                    val sqlDelightManager = dbManager.scrapingDatabase!!.databaseManager
                    voiceCommandProcessor = VoiceCommandProcessor(
                        context = this@VoiceOSService,
                        accessibilityService = this@VoiceOSService,
                        scrapedAppQueries = sqlDelightManager.scrapedAppQueries,
                        scrapedElementQueries = sqlDelightManager.scrapedElementQueries,
                        generatedCommandQueries = sqlDelightManager.generatedCommandQueries
                    )
                    Log.i(TAG, "VoiceCommandProcessor initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize VoiceCommandProcessor", e)
                    voiceCommandProcessor = null
                }
            } else {
                Log.w(TAG, "Skipping VoiceCommandProcessor (database not initialized)")
            }

            // TODO: Initialize UI components when implemented
            // if (config.isFloatingMenuEnabled()) {
            //     floatingMenu.show()
            // }
            // if (config.isCursorEnabled()) {
            //     cursorManagerInstance.initialize()
            // }

            // Initialize voice recognition if available
            initializeVoiceRecognition()

            isServiceReady = true
            Log.i(TAG, "All components initialized with optimization")

            // Log performance metrics
            logPerformanceMetrics()

            // Phase 3D: Start periodic memory monitoring
            startMemoryMonitoring()

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components", e)
        }
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isServiceReady || event == null) return

        // Phase 3E: Adaptive event filtering based on memory pressure
        // Drop low-priority events (scrolling, focus) under memory pressure
        // Preserve critical events (clicks, text input) to maintain functionality
        val isLowResource = resourceMonitor.isLowResourceMode.value
        val eventPriority = eventPriorityManager.getPriorityForEvent(event.eventType)
        val shouldProcess = !isLowResource || eventPriority >= EventPriorityManager.PRIORITY_HIGH

        if (!shouldProcess) {
            Log.v(TAG, "Event filtered due to memory pressure: type=${event.eventType}, priority=$eventPriority")
            return
        }

        // ROBUST DEBUG: Log LearnApp state on every event for debugging
        Log.d(TAG, "LEARNAPP_DEBUG: learnAppInitialized=$learnAppInitialized, learnAppIntegration=${if (learnAppIntegration != null) "EXISTS" else "NULL"}")

        // DEFERRED INITIALIZATION: Initialize LearnApp on first accessibility event
        // This ensures FLAG_RETRIEVE_INTERACTIVE_WINDOWS has been fully processed by Android
        // FIX (2025-11-30): Use atomic state to prevent race condition where events arrive
        // before initialization completes. State: 0=not started, 1=in progress, 2=complete
        // FIX (2025-12-10): Queue events during initialization instead of dropping them
        val initState = learnAppInitState.get()
        if (initState == 0) {
            // Try to claim initialization (atomic compare-and-set)
            if (learnAppInitState.compareAndSet(0, 1)) {
                Log.i(TAG, "LEARNAPP_DEBUG: Won init race - starting initialization")
                serviceScope.launch {
                    try {
                        Log.i(TAG, "LEARNAPP_DEBUG: Coroutine started for initializeLearnAppIntegration()")
                        initializeLearnAppIntegration()
                        Log.i(TAG, "LEARNAPP_DEBUG: initializeLearnAppIntegration() returned")
                        Log.i(TAG, "LearnApp initialization complete (event-driven)")
                        learnAppInitState.set(2)  // Mark complete AFTER init done
                        Log.i(TAG, "LEARNAPP_DEBUG: learnAppIntegration is now ${if (learnAppIntegration != null) "SET" else "STILL NULL"}")

                        // FIX (2025-12-10): Process queued events after initialization completes
                        processQueuedEvents()
                    } catch (e: Exception) {
                        Log.e(TAG, "LEARNAPP_DEBUG: Initialization failed, allowing retry", e)
                        learnAppInitState.set(0)  // Allow retry on next event
                    }
                }
            }
            // Queue event during initialization instead of dropping it
            queueEvent(event)
            return
        } else if (initState == 1) {
            // Initialization in progress - queue this event for later processing
            queueEvent(event)
            return
        }
        // initState == 2: Fully initialized, proceed with event forwarding

        // FIX (2025-12-10): Process any queued events first (in case of race)
        processQueuedEvents()

        try {
            // Forward to hash-based scraping integration FIRST (base scraping)
            scrapingIntegration?.let { integration ->
                try {
                    Log.v(TAG, "Forwarding accessibility event to AccessibilityScrapingIntegration")
                    integration.onAccessibilityEvent(event)
                    Log.v(TAG, "Event forwarded successfully to AccessibilityScrapingIntegration")
                } catch (e: Exception) {
                    Log.e(TAG, "Error forwarding event to AccessibilityScrapingIntegration", e)
                    Log.e(TAG, "Scraping error type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "Scraping error message: ${e.message}")
                }
            }

            // Forward to LearnApp integration for third-party app learning
            if (learnAppIntegration == null) {
                Log.w(TAG, "LEARNAPP_DEBUG: learnAppIntegration is NULL - cannot forward event!")
            }
            learnAppIntegration?.let { integration ->
                try {
                    Log.d(TAG, "LEARNAPP_DEBUG: Forwarding event to LearnApp integration")
                    Log.v(TAG, "Forwarding accessibility event to LearnApp integration")
                    integration.onAccessibilityEvent(event)
                    Log.v(TAG, "Event forwarded successfully to LearnApp")
                } catch (e: Exception) {
                    Log.e(TAG, "Error forwarding event to LearnApp integration", e)
                    Log.e(TAG, "LearnApp error type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "LearnApp error message: ${e.message}")
                }
            }

            // Track event counts for performance monitoring
            event.eventType.let { eventCounts[it]?.incrementAndGet() }

            // Get package names for event processing
            var packageName = event.packageName?.toString()
            val currentPackage = rootInActiveWindow?.packageName?.toString()

            // Handle cases where packageName might be null but currentPackage is available
            if (packageName == null && currentPackage != null) {
                val isRedundantWindowChange = isRedundantWindowChange(event)

                // Use dynamic package configuration instead of hardcoded list
                if (isRedundantWindowChange && !DynamicPackageConfig.shouldMonitorPackage(this, currentPackage)) {
                    return // Skip redundant window changes for apps that don't need them
                }

                if (isRedundantWindowChange) {
                    packageName = currentPackage // Only assign for valid packages
                } else {
                    return // Can't proceed meaningfully without package name
                }
            }

            // If after checks, packageName is still null, skip processing
            if (packageName == null) return

            // Create debounce key based on package, class, and event type
            val debounceKey = "$packageName-${event.className?.toString() ?: "unknown"}-${event.eventType}"

            // Apply debouncing to prevent excessive processing
            if (!eventDebouncer.shouldProceed(debounceKey)) {
                Log.v(TAG, "Event debounced for: $debounceKey")
                return
            }

            // Process events based on type with enhanced logic
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    // Update UI scraping cache asynchronously with debouncing
                    serviceScope.launch {
                        val commands = uiScrapingEngine.extractUIElementsAsync(event)
                        nodeCache.clear()
                        nodeCache.addAll(commands)

                        val normalizedCommand = commands.map { element -> element.normalizedText }
                        commandCache.clear()
                        commandCache.addAll(normalizedCommand)
                        Log.d(TAG, "SPEECH_TEST: TYPE_WINDOW_CONTENT_CHANGED commandsStr = $commandCache")
                        if (config.verboseLogging) {
                            Log.d(TAG, "Scraped commands for $packageName: $commandCache")
                        }
                    }
                }

                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    // FIX (2025-12-11): Use coroutineScopeCommands (Dispatchers.IO) to prevent ANR
                    // Previously used serviceScope (Dispatchers.Main) which blocked main thread with 300+ commands

                    // Forward to ScreenActivityDetector for rename hint display
                    screenActivityDetector?.let { detector ->
                        coroutineScopeCommands.launch {
                            try {
                                Log.v(TAG, "Forwarding WINDOW_STATE_CHANGED to ScreenActivityDetector")
                                detector.onWindowStateChanged(event.packageName?.toString(), event.className?.toString())
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in ScreenActivityDetector", e)
                            }
                        }
                    }

                    // Update app context and trigger scraping for new windows
                    coroutineScopeCommands.launch {
                        // Also trigger UI scraping for window state changes
                        val commands = uiScrapingEngine.extractUIElementsAsync(event)
                        val normalizedCommand = commands.map { element -> element.normalizedText }

                        // Update caches on background thread (prevents ANR)
                        nodeCache.clear()
                        nodeCache.addAll(commands)
                        commandCache.clear()
                        commandCache.addAll(normalizedCommand)

                        Log.d(TAG, "SPEECH_TEST: TYPE_WINDOW_STATE_CHANGED commandsStr (${normalizedCommand.size} commands) = $commandCache")
                        if (config.verboseLogging) {
                            Log.d(TAG, "Scraped commands for $packageName: $commandCache")
                        }
                    }
                }

                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    // Log click events for analytics and optionally refresh commands
                    if (config.verboseLogging) {
                        Log.d(TAG, "Click event in $packageName: ${event.className}")
                    }

                    // Trigger light UI refresh after clicks (useful for dynamic content)
                    serviceScope.launch {
                        // Also trigger UI scraping for window state changes
                        val commands = uiScrapingEngine.extractUIElementsAsync(event)
                        nodeCache.clear()
                        nodeCache.addAll(commands)
                        val normalizedCommand = commands.map { element -> element.normalizedText }
                        commandCache.clear()
                        commandCache.addAll(normalizedCommand)
                        Log.d(TAG, "SPEECH_TEST: TYPE_VIEW_CLICKED commandsStr = $commandCache")
                        if (config.verboseLogging) {
                            Log.d(TAG, "Scraped commands for $packageName: $commandCache")
                        }
                    }
                }

                else -> {
                    // Handle other event types if needed in the future
                    Log.v(TAG, "Unhandled event type: ${event.eventType} for $packageName")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling accessibility event: ${event.eventType}", e)
        }
    }

    private fun registerVoiceCmd() {
        if (!isCommandProcessing.compareAndSet(false, true)) {
            return // Already processing, avoid starting a new loop
        }
        coroutineScopeCommands.launch {
            try {
                while (isActive) {
                    delay(COMMAND_CHECK_INTERVAL_MS)
                    if (isVoiceInitialized && System.currentTimeMillis() - lastCommandLoaded > COMMAND_LOAD_DEBOUNCE_MS) {
                        if (commandCache != allRegisteredDynamicCommands) {
                            Log.d(TAG, "SPEECH_TEST: registerVoiceCmd commandsStr = $commandCache")
                            val allCommands = commandCache + staticCommandCache + appsCommand.keys

//                            if (BuildConfig.DEBUG) {
//                                val objectCommand = prettyGson.toJson(allCommands)
//                                Log.d(TAG, "RegisterVoiceCmd allCommands = $objectCommand")
//                            }
                            // FIX (2025-12-11): updateCommands is now suspend, call directly in coroutine
                            speechEngineManager.updateCommands(allCommands)
                            allRegisteredDynamicCommands.clear()
                            allRegisteredDynamicCommands.addAll(commandCache)
                            lastCommandLoaded = System.currentTimeMillis()
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.e(TAG, "Command processing loop cancelled", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error in command processing loop", e)
            } finally {
                isCommandProcessing.set(false)
            }
        }
    }

    /**
     * Check if the window change event is redundant (similar to legacy implementation)
     */
    private fun isRedundantWindowChange(event: AccessibilityEvent): Boolean {
        return event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }

    private fun initializeVoiceRecognition() {
        // FIX: Re-enabled speech engine initialization with Vivoka
        // Root cause: SpeechEngineManager was disabled for standalone testing
        // Solution: Restored Hilt injection and removed null-check bypass

        // Start engine initialization asynchronously with Vivoka
        // TODO: Make engine selection user-configurable (Vivoka/Vosk)
        speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)

        // ARCHITECTURE: Split into two separate collectors
        // 1. State collection: Monitor engine lifecycle (initialization, listening status)
        // 2. Command event collection: Process voice commands as discrete events

        serviceScope.launch {
            // Collector 1: Monitor engine state for lifecycle management
            launch {
                speechEngineManager.speechState.collectLatest { state ->
                    Log.d(TAG, "SPEECH_TEST: engine state = $state")

                    // Only start listening when engine is fully initialized
                    if (state.isInitialized && !state.isListening && !isVoiceInitialized) {
                        isVoiceInitialized = true
                        delay(200) // Small delay to ensure engine is fully ready
                        speechEngineManager.startListening()
                    }
                }
            }

            // Collector 2: Process command events (guarantees every command is received)
            launch {
                speechEngineManager.commandEvents.collect { event ->
                    Log.i(TAG, "SPEECH_TEST: Command event received - command='${event.command}', confidence=${event.confidence}, timestamp=${event.timestamp}")

                    // Validate command before processing
                    if (event.confidence > 0.5f && event.command.isNotBlank()) {
                        Log.i(TAG, "Processing command: '${event.command}' (confidence=${event.confidence})")
                        handleVoiceCommand(confidence = event.confidence, command = event.command)
                    } else {
                        Log.d(TAG, "Command rejected: confidence too low (${event.confidence}) or empty command")
                    }
                }
            }
        }
    }

    /**
     * Initialize VoiceCursor API for cursor functionality
     * Must run on Main thread for GestureDetector initialization
     */
    private suspend fun initializeVoiceCursor() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            try {
                voiceCursorInitialized = VoiceCursorAPI.initialize(this@VoiceOSService, this@VoiceOSService)
                if (voiceCursorInitialized) {
                    showCursor()
                    Log.d(TAG, "VoiceCursor API initialized successfully")
                } else {
                    Log.w(TAG, "Failed to initialize VoiceCursor API")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing VoiceCursor API", e)
                voiceCursorInitialized = false
            }
        }
    }

    /**
     * Initialize LearnApp integration for third-party app learning
     *
     * This integration enables automatic UI exploration and UUID generation
     * for third-party apps. When a new app is launched, the user will be
     * prompted for consent before exploration begins.
     */
    private fun initializeLearnAppIntegration() {
        Log.i(TAG, "LEARNAPP_DEBUG: ========================================")
        Log.i(TAG, "LEARNAPP_DEBUG: initializeLearnAppIntegration() CALLED")
        Log.i(TAG, "LEARNAPP_DEBUG: ========================================")
        Log.i(TAG, "=== LearnApp Integration Initialization Start ===")
        try {
            // Initialize UUIDCreator first (required dependency)
            Log.d(TAG, "LEARNAPP_DEBUG: About to initialize UUIDCreator...")
            Log.d(TAG, "Initializing UUIDCreator...")
            UUIDCreator.initialize(applicationContext)
            Log.d(TAG, "✓ UUIDCreator initialized")
            Log.d(TAG, "LEARNAPP_DEBUG: UUIDCreator done")

            Log.d(TAG, "LEARNAPP_DEBUG: About to call LearnAppIntegration.initialize()...")
            Log.d(TAG, "Attempting to initialize LearnAppIntegration...")
            Log.d(TAG, "Context: ${applicationContext.javaClass.simpleName}")
            Log.d(TAG, "Service: ${this.javaClass.simpleName}")

            // Initialize LearnAppIntegration
            learnAppIntegration = LearnAppIntegration.initialize(applicationContext, this)
            Log.d(TAG, "LEARNAPP_DEBUG: LearnAppIntegration.initialize() returned: ${learnAppIntegration}")

            Log.i(TAG, "✓ LearnApp integration initialized successfully")
            Log.d(TAG, "Integration instance: ${learnAppIntegration?.javaClass?.simpleName}")
            Log.d(TAG, "Features enabled:")
            Log.d(TAG, "  - App launch detection: ACTIVE")
            Log.d(TAG, "  - Consent dialog management: ACTIVE")
            Log.d(TAG, "  - Exploration engine: ACTIVE")
            Log.d(TAG, "  - Progress overlay: ACTIVE")
            Log.i(TAG, "LearnApp will now monitor for new third-party app launches")
            Log.i(TAG, "LEARNAPP_DEBUG: Initialization SUCCESS - learnAppIntegration is ${if (learnAppIntegration != null) "NOT NULL" else "NULL"}")

            // PHASE 3 (2025-12-08): Command Discovery integration
            // Auto-observes ExplorationEngine.state() via StateFlow
            // Triggers visual overlay, audio summary, and tutorial when exploration completes
            Log.d(TAG, "LEARNAPP_DEBUG: Initializing CommandDiscoveryIntegration...")
            learnAppIntegration?.let { integration ->
                try {
                    discoveryIntegration = CommandDiscoveryIntegration(
                        context = this,
                        explorationEngine = integration.getExplorationEngine()
                    )
                    Log.i(TAG, "✓ Command Discovery integration initialized successfully")
                    Log.d(TAG, "  - Visual overlay: ACTIVE (10s auto-hide)")
                    Log.d(TAG, "  - Audio summary: ACTIVE")
                    Log.d(TAG, "  - Interactive tutorial: ACTIVE")
                    Log.d(TAG, "  - Command list UI: ACTIVE")
                    Log.d(TAG, "  - Contextual hints: ACTIVE")
                    Log.d(TAG, "Auto-observation enabled - no manual wiring needed")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Failed to initialize Command Discovery integration", e)
                    Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "  Error message: ${e.message}")
                    discoveryIntegration = null
                }
            } ?: Log.w(TAG, "Skipping Command Discovery - LearnAppIntegration not available")

        } catch (e: Exception) {
            Log.e(TAG, "LEARNAPP_DEBUG: EXCEPTION during initialization!")
            Log.e(TAG, "✗ Failed to initialize LearnApp integration", e)
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Stack trace:")
            e.printStackTrace()
            Log.w(TAG, "Service will continue without LearnApp integration")
            learnAppIntegration = null
        }
        Log.i(TAG, "=== LearnApp Integration Initialization Complete ===")

        // Start JIT Learning Service (Phase 3: JIT-LearnApp Separation - 2025-12-18)
        // Must be called AFTER LearnAppIntegration initializes (provides JITLearnerProvider)
        startJITService()
    }

    /**
     * Service connection for JIT Learning Service binding
     * Phase 3: JIT-LearnApp Separation (2025-12-18)
     */
    private val jitServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "JIT Learning Service connected via AIDL")

            try {
                // Get service instance and wire up provider
                val jitService = com.augmentalis.jitlearning.JITLearningService.getInstance()
                if (jitService != null && learnAppIntegration != null) {
                    // Set JITLearnerProvider (implemented by LearnAppIntegration)
                    jitService.setLearnerProvider(learnAppIntegration!!)

                    // Set AccessibilityService interface for root node access
                    jitService.setAccessibilityService(object : com.augmentalis.jitlearning.JITLearningService.AccessibilityServiceInterface {
                        override fun getRootNode(): android.view.accessibility.AccessibilityNodeInfo? {
                            return this@VoiceOSService.rootInActiveWindow
                        }

                        override fun performGlobalAction(action: Int): Boolean {
                            return this@VoiceOSService.performGlobalAction(action)
                        }
                    })

                    jitServiceBound = true
                    Log.i(TAG, "✓ JIT Learning Service provider wired successfully")
                    Log.d(TAG, "  - JITLearnerProvider: ${learnAppIntegration!!.javaClass.simpleName}")
                    Log.d(TAG, "  - AccessibilityService interface: PROVIDED")
                    Log.d(TAG, "  - Service ready for LearnApp binding")
                } else {
                    Log.w(TAG, "Cannot wire JIT service - service or integration is null")
                    Log.w(TAG, "  JIT service: ${if (jitService != null) "OK" else "NULL"}")
                    Log.w(TAG, "  Integration: ${if (learnAppIntegration != null) "OK" else "NULL"}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to wire JIT Learning Service provider", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "JIT Learning Service disconnected")
            jitServiceBound = false
        }
    }

    /**
     * Start JIT Learning Service
     * Phase 3: JIT-LearnApp Separation (2025-12-18)
     *
     * Starts the foreground service and binds to it to wire up the JITLearnerProvider.
     * Called after LearnAppIntegration initializes.
     */
    private fun startJITService() {
        try {
            Log.i(TAG, "Starting JIT Learning Service...")

            val intent = Intent(this, com.augmentalis.jitlearning.JITLearningService::class.java)

            // Start as foreground service (Android O+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            // Bind to service to wire up provider
            val bound = bindService(intent, jitServiceConnection, Context.BIND_AUTO_CREATE)

            if (bound) {
                Log.i(TAG, "✓ JIT Learning Service started and binding initiated")
                Log.d(TAG, "  - Service will run as foreground service")
                Log.d(TAG, "  - LearnApp can now bind via AIDL")
                Log.d(TAG, "  - Passive learning will begin on next accessibility event")
            } else {
                Log.e(TAG, "✗ Failed to bind to JIT Learning Service")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start JIT Learning Service", e)
            Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "  Error message: ${e.message}")
            Log.w(TAG, "Service will continue without JIT Learning")
        }
    }

    /**
     * Queue an accessibility event for later processing.
     * Called during LearnApp initialization to prevent event loss.
     *
     * FIX (2025-12-10): Implements event queue solution from spec Section 2.1
     */
    private fun queueEvent(event: android.view.accessibility.AccessibilityEvent) {
        if (pendingEvents.size < MAX_QUEUED_EVENTS) {
            // Create a copy of the event to avoid recycling issues
            val eventCopy = android.view.accessibility.AccessibilityEvent.obtain(event)
            pendingEvents.offer(eventCopy)
            Log.d(TAG, "LEARNAPP_DEBUG: Queued event (type=${event.eventType}, queue size=${pendingEvents.size})")
        } else {
            Log.w(TAG, "LEARNAPP_DEBUG: Event queue full ($MAX_QUEUED_EVENTS), dropping event")
        }
    }

    /**
     * Process all queued events after initialization completes.
     * Ensures no events are lost during the initialization window.
     *
     * FIX (2025-12-10): Implements event queue solution from spec Section 2.1
     */
    private fun processQueuedEvents() {
        val queueSize = pendingEvents.size
        if (queueSize > 0) {
            Log.i(TAG, "LEARNAPP_DEBUG: Processing $queueSize queued events")
            var processedCount = 0

            while (pendingEvents.isNotEmpty()) {
                val queuedEvent = pendingEvents.poll()
                if (queuedEvent != null) {
                    try {
                        // Forward to LearnApp integration
                        learnAppIntegration?.onAccessibilityEvent(queuedEvent)
                        processedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing queued event", e)
                    } finally {
                        // FIX: Recycle AccessibilityNodeInfo before recycling event
                        // AccessibilityNodeInfo instances are not auto-recycled and cause 100-250KB leaks per event
                        val source = queuedEvent.source
                        source?.recycle()
                        // Recycle the event copy to free memory
                        queuedEvent.recycle()
                    }
                }
            }

            Log.i(TAG, "LEARNAPP_DEBUG: Processed $processedCount queued events")
        }
    }

    /**
     * Show cursor overlay
     */
    override fun showCursor(): Boolean {
        return if (voiceCursorInitialized) {
            VoiceCursorAPI.showCursor()
        } else {
            Log.w(TAG, "VoiceCursor not initialized - cannot show cursor")
            false
        }
    }

    override fun getCursorPosition(): CursorOffset {
        return if (voiceCursorInitialized) {
            VoiceCursorAPI.getCurrentPosition() ?: getCenterOffset()
        } else {
            Log.w(TAG, "VoiceCursor not initialized - cannot show cursor")
            getCenterOffset()
        }
    }

    override fun isCursorVisible(): Boolean {
        return if (voiceCursorInitialized) {
            VoiceCursorAPI.isVisible()
        } else {
            Log.w(TAG, "VoiceCursor not initialized - cannot show cursor")
            false
        }
    }

    /**
     * Hide cursor overlay
     */
    override fun hideCursor(): Boolean {
        return if (voiceCursorInitialized) {
            VoiceCursorAPI.hideCursor()
        } else {
            Log.w(TAG, "VoiceCursor not initialized - cannot hide cursor")
            false
        }
    }

    /**
     * Toggle cursor visibility
     */
    override fun toggleCursor(): Boolean {
        return if (voiceCursorInitialized) {
            VoiceCursorAPI.toggleCursor()
        } else {
            Log.w(TAG, "VoiceCursor not initialized - cannot toggle cursor")
            false
        }
    }

    /**
     * Center cursor on screen
     */
    override fun centerCursor(): Boolean {
        return if (voiceCursorInitialized) {
            VoiceCursorAPI.centerCursor()
        } else {
            Log.w(TAG, "VoiceCursor not initialized - cannot center cursor")
            false
        }
    }

    /**
     * Perform click at current cursor position
     */
    override fun clickCursor(): Boolean {
        return if (voiceCursorInitialized) {
            VoiceCursorAPI.click()
        } else {
            Log.w(TAG, "VoiceCursor not initialized - cannot click")
            false
        }
    }

    /**
     * Evaluate whether ForegroundService is needed (hybrid approach)
     * Only starts ForegroundService on Android 12+ when app is in background with active voice
     */
    private fun evaluateForegroundServiceNeed() {
        val needsForeground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && appInBackground
                && voiceSessionActive
                && !foregroundServiceActive

        val shouldStopForeground = foregroundServiceActive && (!appInBackground || !voiceSessionActive)

        when {
            needsForeground -> {
                Log.d(TAG, "Starting ForegroundService (Android 12+ background requirement)")
                startForegroundServiceHelper()
            }

            shouldStopForeground -> {
                Log.d(TAG, "Stopping ForegroundService (no longer needed)")
                stopForegroundServiceHelper()
            }

            else -> {
                Log.v(TAG, "ForegroundService state: needed=$needsForeground, active=$foregroundServiceActive")
            }
        }
    }

    /**
     * Start the foreground service when needed
     */
    private fun startForegroundServiceHelper() {
        if (foregroundServiceActive) return

        try {
            val intent = Intent(this, VoiceOnSentry::class.java).apply {
                action = ACTION_START_MIC
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            foregroundServiceActive = true
            Log.i(TAG, "ForegroundService started for background mic access")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ForegroundService", e)
            foregroundServiceActive = false
        }
    }

    /**
     * Stop the foreground service when no longer needed
     */
    private fun stopForegroundServiceHelper() {
        if (!foregroundServiceActive) return

        try {
            val intent = Intent(this, VoiceOnSentry::class.java).apply {
                action = ACTION_STOP_MIC
            }
            stopService(intent)

            foregroundServiceActive = false
            Log.i(TAG, "ForegroundService stopped (no longer needed)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop ForegroundService", e)
        }
    }

    /**
     * Handle voice command with caching
     *
     * Phase 1: Now routes to CommandManager when available
     * Phase 2: Added rename command detection and routing
     */
    private fun handleVoiceCommand(command: String, confidence: Float) {
        Log.d(TAG, "handleVoiceCommand: command='$command', confidence=$confidence")

        // Reject very low confidence (< 0.5)
        if (confidence < 0.5f) {
            Log.d(TAG, "Command rejected: confidence too low ($confidence)")
            return
        }

        val normalizedCommand = command.lowercase().trim()
        val currentPackage = rootInActiveWindow?.packageName?.toString()

        // RENAME TIER: Check if this is a rename command (BEFORE other tiers)
        if (isRenameCommand(normalizedCommand)) {
            serviceScope.launch {
                try {
                    Log.i(TAG, "Rename command detected: '$normalizedCommand'")
                    val handled = handleRenameCommand(normalizedCommand, currentPackage)

                    if (handled) {
                        Log.i(TAG, "✓ Rename command executed successfully")
                        return@launch // Rename handled, done
                    } else {
                        Log.w(TAG, "Rename command failed, not continuing to regular tiers")
                        // Don't fall through to regular commands - rename failures are explicit
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing rename command: ${e.message}", e)
                }
            }
            return // Return here to prevent dual execution
        }

        // WEB TIER: Check if this is a web command (BEFORE other tiers)
        if (currentPackage != null && webCommandCoordinator.isCurrentAppBrowser(currentPackage)) {
            serviceScope.launch {
                try {
                    Log.d(TAG, "Browser detected, trying web command...")
                    val handled = webCommandCoordinator.processWebCommand(normalizedCommand, currentPackage)

                    if (handled) {
                        Log.i(TAG, "✓ Web command executed successfully: '$normalizedCommand'")
                        return@launch // Web command handled, done
                    } else {
                        Log.d(TAG, "Not a web command or no match found, continuing to regular tiers...")
                        // Continue to Tier 1
                        handleRegularCommand(normalizedCommand, confidence)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing web command: ${e.message}", e)
                    // If web command fails, fall through to regular tiers
                    handleRegularCommand(normalizedCommand, confidence)
                }
            }
            return // Return here to prevent dual execution
        }

        // Not a browser, handle as regular command through tier system
        handleRegularCommand(normalizedCommand, confidence)
    }

    /**
     * Check if voice input is a rename command
     *
     * Detects patterns:
     * - "rename X to Y"
     * - "rename X as Y"
     * - "change X to Y"
     *
     * @param voiceInput Normalized voice input (lowercase)
     * @return true if rename command, false otherwise
     */
    private fun isRenameCommand(voiceInput: String): Boolean {
        val patterns = listOf(
            Regex("rename .+ to .+"),
            Regex("rename .+ as .+"),
            Regex("change .+ to .+")
        )
        return patterns.any { it.matches(voiceInput) }
    }

    /**
     * Handle rename command
     *
     * Initializes RenameCommandHandler on-demand (requires TTS from speech engine).
     * Processes rename command and returns result.
     *
     * @param voiceInput Normalized voice input
     * @param packageName Current app package (or null if unavailable)
     * @return true if rename succeeded, false if failed
     */
    private suspend fun handleRenameCommand(
        voiceInput: String,
        packageName: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Validate package name
            if (packageName == null) {
                Log.w(TAG, "Cannot process rename: no current package")
                return@withContext false
            }

            // Initialize handler on-demand if not already initialized
            if (renameCommandHandler == null) {
                Log.d(TAG, "Initializing RenameCommandHandler on-demand...")
                renameCommandHandler = RenameCommandHandler(context = applicationContext)
                renameCommandHandler?.start()
                Log.d(TAG, "✓ RenameCommandHandler initialized")
            }

            // Process rename command
            // TODO: Parse voice input to extract old name and new name
            // Format expected: "rename [old name] to [new name]"
            val renamePattern = "rename\\s+(.+?)\\s+to\\s+(.+)".toRegex(RegexOption.IGNORE_CASE)
            val matchResult = renamePattern.find(voiceInput)

            if (matchResult != null) {
                val (oldName, newName) = matchResult.destructured
                val commandId = "${packageName}_${oldName.trim()}"
                val success = renameCommandHandler?.renameCommand(commandId, oldName.trim(), newName.trim()) ?: false
                if (success) {
                    Log.i(TAG, "Rename successful: $oldName → $newName")
                } else {
                    Log.e(TAG, "Rename failed for: $oldName → $newName")
                }
                success
            } else {
                Log.w(TAG, "Could not parse rename command from: $voiceInput")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleRenameCommand", e)
            false
        }
    }

    /**
     * Handle regular (non-web) commands through tier system
     */
    private fun handleRegularCommand(normalizedCommand: String, confidence: Float) {
        // TIER 1: CommandManager (PRIMARY)
        if (!fallbackModeEnabled && commandManagerInstance != null) {
            serviceScope.launch {
                // Capture manager instance for safe access within coroutine
                val manager = commandManagerInstance ?: return@launch

                try {
                    Log.d(TAG, "Attempting Tier 1: CommandManager")

                    // Create Command object with full context
                    val cmd = Command(
                        id = normalizedCommand,
                        text = normalizedCommand,
                        source = CommandSource.VOICE,
                        context = createCommandContext(),
                        confidence = confidence,
                        timestamp = System.currentTimeMillis()
                    )

                    // Execute via CommandManager
                    val result = manager.executeCommand(cmd)

                    if (result.success) {
                        Log.i(TAG, "✓ Tier 1 (CommandManager) SUCCESS: '$normalizedCommand'")
                        return@launch // Command executed successfully, done
                    } else {
                        Log.w(TAG, "Tier 1 (CommandManager) FAILED: ${result.error?.message}")
                        Log.d(TAG, "  Falling through to Tier 2...")
                        // Fall through to Tier 2
                        executeTier2Command(normalizedCommand)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Tier 1 (CommandManager) ERROR: ${e.message}", e)
                    Log.d(TAG, "  Falling through to Tier 2...")
                    // Fall through to Tier 2 on error
                    executeTier2Command(normalizedCommand)
                }
            }
        } else {
            // CommandManager unavailable or in fallback mode
            if (fallbackModeEnabled) {
                Log.w(TAG, "Fallback mode active - skipping CommandManager")
            } else {
                Log.w(TAG, "CommandManager not available - using fallback path")
            }

            // Execute Tier 2 directly
            serviceScope.launch {
                executeTier2Command(normalizedCommand)
            }
        }
    }

    /**
     * Create CommandContext from current accessibility service state
     * Captures current app, activity, and screen context
     *
     * @return CommandContext with current state snapshot
     */
    private fun createCommandContext(): CommandContext {
        val root = rootInActiveWindow

        return CommandContext(
            packageName = root?.packageName?.toString(),
            activityName = root?.className?.toString(),
            focusedElement = root?.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)?.className?.toString(),
            deviceState = mapOf(
                "hasRoot" to (root != null),
                "childCount" to (root?.childCount ?: 0),
                "isAccessibilityFocused" to (root?.isAccessibilityFocused ?: false),
                // CRITICAL: Add Android context and accessibility service for BaseAction
                "androidContext" to (this as android.content.Context),
                "accessibilityService" to (this as android.accessibilityservice.AccessibilityService)
            ),
            customData = mapOf(
                "commandCacheSize" to commandCache.size,
                "nodeCacheSize" to nodeCache.size,
                "fallbackMode" to fallbackModeEnabled
            )
        )
    }

    /**
     * Execute Tier 2: VoiceCommandProcessor (secondary)
     * Handles app-specific commands from database
     */
    private suspend fun executeTier2Command(normalizedCommand: String) {
        try {
            Log.d(TAG, "Attempting Tier 2: VoiceCommandProcessor")

            // Try hash-based command processor
            voiceCommandProcessor?.let { processor ->
                val result = processor.processCommand(normalizedCommand)

                if (result.success) {
                    Log.i(TAG, "✓ Tier 2 (VoiceCommandProcessor) SUCCESS: '$normalizedCommand'")
                    return // Command executed successfully, done
                } else {
                    Log.w(TAG, "Tier 2 (VoiceCommandProcessor) FAILED: ${result.message}")
                    Log.d(TAG, "  Falling through to Tier 3...")
                }
            } ?: run {
                Log.d(TAG, "VoiceCommandProcessor not available, skipping Tier 2")
            }

            // Fall through to Tier 3
            executeTier3Command(normalizedCommand)

        } catch (e: Exception) {
            Log.e(TAG, "Tier 2 (VoiceCommandProcessor) ERROR: ${e.message}", e)
            Log.d(TAG, "  Falling through to Tier 3...")
            // Fall through to Tier 3 on error
            executeTier3Command(normalizedCommand)
        }
    }

    /**
     * Execute Tier 3: ActionCoordinator (tertiary/fallback)
     * Handles legacy handler-based commands
     *
     * CRITICAL FIX (2025-11-13): Now checks actual return value from ActionCoordinator.
     * Previous bug: Always logged "EXECUTED" even when ActionCoordinator returned false.
     * This caused false success messages when commands actually failed.
     */
    private suspend fun executeTier3Command(normalizedCommand: String) {
        try {
            Log.d(TAG, "Attempting Tier 3: ActionCoordinator (final fallback)")

            // FIX: Capture return value to check actual success/failure
            val result = actionCoordinator.executeAction(normalizedCommand)

            if (result) {
                // True success - ActionCoordinator found handler and executed successfully
                Log.i(TAG, "✓ Tier 3 (ActionCoordinator) SUCCESS: '$normalizedCommand'")
            } else {
                // ActionCoordinator returned false - no handler found or execution failed
                Log.w(TAG, "✗ Tier 3 (ActionCoordinator) FAILED: No handler found for '$normalizedCommand'")
                Log.e(TAG, "✗ All tiers failed for command: '$normalizedCommand'")
                // TODO: Consider providing user feedback here (TTS or UI notification)
            }

        } catch (e: Exception) {
            // Exception during execution (rare - ActionCoordinator catches most exceptions internally)
            Log.e(TAG, "Tier 3 (ActionCoordinator) ERROR: ${e.message}", e)
            Log.e(TAG, "✗ All tiers failed for command: '$normalizedCommand'")
        }
    }

    /**
     * Phase 1: Enable fallback mode when CommandManager is unavailable
     * Called by ServiceMonitor during graceful degradation
     */
    override fun enableFallbackMode() {
        fallbackModeEnabled = true
        Log.w(TAG, "Fallback mode enabled - using basic command handling only")
    }

    /**
     * Perform click at screen coordinates
     * Used by GazeHandler for gaze-based clicking
     */
    private fun performClick(x: Int, y: Int): Boolean {
        Log.d(TAG, "SPEECH_TEST: performClick x = $x , y = $y")
        return try {
            val path = android.graphics.Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            val gesture = Builder()
                .addStroke(StrokeDescription(path, 0, 100))
                .build()

            dispatchGesture(gesture, null, null)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform click at ($x, $y)", e)
            false
        }
    }

    /**
     * Execute command through hash-based processor with fallback to ActionCoordinator
     *
     * Execution Strategy:
     * 1. Try hash-based command processor (database lookup)
     * 2. Fall back to ActionCoordinator if hash-based fails or not initialized
     * 3. Log execution path for debugging
     */
    private fun executeCommand(command: String) {
        serviceScope.launch {
            var commandExecuted = false

            // Try hash-based command processor first
            voiceCommandProcessor?.let { processor ->
                try {
                    Log.d(TAG, "Attempting hash-based command execution: '$command'")
                    val result = processor.processCommand(command)

                    if (result.success) {
                        Log.i(TAG, "✓ Hash-based command executed successfully: '$command'")
                        Log.d(TAG, "  Result: ${result.message}")
                        commandExecuted = true
                    } else {
                        Log.w(TAG, "Hash-based command failed: ${result.message}")
                        Log.d(TAG, "  Will fall back to ActionCoordinator")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in hash-based command processor", e)
                    Log.d(TAG, "  Exception: ${e.javaClass.simpleName}: ${e.message}")
                }
            }

            // Fall back to ActionCoordinator if hash-based execution failed or not available
            if (!commandExecuted) {
                Log.d(TAG, "Executing command via ActionCoordinator (fallback): '$command'")
                try {
                    actionCoordinator.executeAction(command)
                    Log.d(TAG, "✓ ActionCoordinator executed: '$command'")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in ActionCoordinator execution", e)
                }
            }
        }
    }

    /**
     * Log performance metrics
     */
    private fun logPerformanceMetrics() {
        serviceScope.launch {
            val metrics = mutableMapOf<String, Any>()

            // Get metrics from optimized components
            metrics.putAll(uiScrapingEngine.getPerformanceMetrics())

            // Add service metrics
            metrics["commandCacheSize"] = commandCache.size
            metrics["nodeCacheSize"] = nodeCache.size
            metrics["isServiceReady"] = isServiceReady

            // Add debouncing metrics
            metrics.putAll(eventDebouncer.getMetrics())

            // Add event count metrics
            eventCounts.forEach { (eventType, count) ->
                val eventName = when (eventType) {
                    AccessibilityEvent.TYPE_VIEW_CLICKED -> "clicks"
                    AccessibilityEvent.TYPE_VIEW_FOCUSED -> "focuses"
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "textChanges"
                    AccessibilityEvent.TYPE_VIEW_SCROLLED -> "scrolls"
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "windowChanges"
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "contentChanges"
                    else -> "event_$eventType"
                }
                metrics["event_$eventName"] = count.get()
            }

            Log.i(TAG, "Performance Metrics: $metrics")
        }
    }

    /**
     * Start periodic memory monitoring
     *
     * Phase 3D: Monitors memory usage every 30 seconds and logs stats.
     * Warns when memory pressure is high to aid debugging and optimization.
     */
    private fun startMemoryMonitoring() {
        serviceScope.launch {
            try {
                Log.i(
                    TAG,
                    "Starting periodic memory monitoring (interval: ${VoiceOSConstants.Timing.MEMORY_MONITOR_INTERVAL_MS}ms)"
                )

                while (isActive) {
                    try {
                        // Log current memory stats
                        val state = resourceMonitor.resourceState.value
                        Log.d(TAG, "Memory: ${state.usedMemoryPercent}% used (${state.availableMemoryMb}MB available), Battery: ${state.batteryLevel}%")

                        // Warn if memory pressure is high
                        if (resourceMonitor.shouldReduceMemory()) {
                            Log.w(TAG, "⚠️ High memory pressure detected: ${state.usedMemoryPercent}% heap usage")
                            Log.w(TAG, "   Consider reducing scraping depth or skipping operations")
                        }

                        // Delay before next check
                        delay(VoiceOSConstants.Timing.MEMORY_MONITOR_INTERVAL_MS)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error in memory monitoring cycle", e)
                        // Continue monitoring despite errors
                        delay(VoiceOSConstants.Timing.MEMORY_MONITOR_INTERVAL_MS)
                    }
                }

            } catch (e: CancellationException) {
                Log.i(TAG, "Memory monitoring cancelled (service stopping)")
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error in memory monitoring", e)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        Log.i(TAG, "VoiceOS Service destroying - starting cleanup")

        // Cleanup hash-based scraping integration
        scrapingIntegration?.let { integration ->
            try {
                Log.d(TAG, "Cleaning up AccessibilityScrapingIntegration...")
                integration.cleanup()
                Log.i(TAG, "✓ AccessibilityScrapingIntegration cleaned up successfully")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error cleaning up AccessibilityScrapingIntegration", e)
                Log.e(TAG, "Cleanup error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Cleanup error message: ${e.message}")
            } finally {
                scrapingIntegration = null
                Log.d(TAG, "AccessibilityScrapingIntegration reference cleared")
            }
        } ?: Log.d(TAG, "AccessibilityScrapingIntegration was not initialized, skipping cleanup")

        // Cleanup VoiceCommandProcessor
        if (voiceCommandProcessor != null) {
            Log.d(TAG, "Clearing VoiceCommandProcessor reference...")
            voiceCommandProcessor = null
            Log.i(TAG, "✓ VoiceCommandProcessor reference cleared")
        }

        // P2-8a: Database cleanup via DatabaseManager
        dbManager.cleanup()

        // Cleanup LearnApp integration
        // FIX (2025-12-04): Re-enabled cleanup - CRITICAL for fixing ProgressOverlay memory leak
        // Leak chain: VoiceOSService → learnAppIntegration → progressOverlayManager → progressOverlay (168.4 KB)
        learnAppIntegration?.let { integration ->
            try {
                Log.d(TAG, "Cleaning up LearnApp integration...")
                integration.cleanup()
                Log.i(TAG, "✓ LearnApp integration cleaned up successfully (memory leak fixed)")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error cleaning up LearnApp integration", e)
                Log.e(TAG, "Cleanup error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Cleanup error message: ${e.message}")
            } finally {
                learnAppIntegration = null
                Log.d(TAG, "LearnApp integration reference cleared")
            }
        } ?: Log.d(TAG, "LearnApp integration was not initialized, skipping cleanup")

        // Cleanup JIT Learning Service (Phase 3: JIT-LearnApp Separation - 2025-12-18)
        if (jitServiceBound) {
            try {
                Log.d(TAG, "Unbinding from JIT Learning Service...")
                unbindService(jitServiceConnection)
                jitServiceBound = false
                Log.i(TAG, "✓ JIT Learning Service unbound successfully")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error unbinding from JIT Learning Service", e)
                Log.e(TAG, "Cleanup error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Cleanup error message: ${e.message}")
            }
        } else {
            Log.d(TAG, "JIT Learning Service was not bound, skipping unbind")
        }

        // Stop JIT Learning Service
        try {
            Log.d(TAG, "Stopping JIT Learning Service...")
            val intent = Intent(this, com.augmentalis.jitlearning.JITLearningService::class.java)
            stopService(intent)
            Log.i(TAG, "✓ JIT Learning Service stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error stopping JIT Learning Service", e)
            Log.e(TAG, "Stop error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Stop error message: ${e.message}")
        }

        // Cleanup Command Discovery integration
        // PHASE 3 (2025-12-08): Cleanup CommandDiscoveryIntegration
        discoveryIntegration?.let { integration ->
            try {
                Log.d(TAG, "Cleaning up Command Discovery integration...")
                integration.cleanup()
                Log.i(TAG, "✓ Command Discovery integration cleaned up successfully")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error cleaning up Command Discovery integration", e)
                Log.e(TAG, "Cleanup error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Cleanup error message: ${e.message}")
            } finally {
                discoveryIntegration = null
                Log.d(TAG, "Command Discovery integration reference cleared")
            }
        } ?: Log.d(TAG, "Command Discovery integration was not initialized, skipping cleanup")

        // Cleanup rename feature components
        try {
            Log.d(TAG, "Cleaning up rename feature components...")

            // Clear RenameHintOverlay (no explicit cleanup needed, just clear reference)
            renameHintOverlay = null
            Log.d(TAG, "✓ RenameHintOverlay reference cleared")

            // Clear ScreenActivityDetector (no explicit cleanup needed)
            screenActivityDetector = null
            Log.d(TAG, "✓ ScreenActivityDetector reference cleared")

            // Shutdown TTS in RenameCommandHandler if initialized
            renameCommandHandler?.let {
                // Note: TTS cleanup should be handled by the handler itself if needed
                Log.d(TAG, "Clearing RenameCommandHandler reference")
            }
            renameCommandHandler = null
            Log.d(TAG, "✓ RenameCommandHandler reference cleared")

            Log.i(TAG, "✓ Rename feature components cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error cleaning up rename feature", e)
        }

        // Cleanup OverlayManager (lazy delegate handles initialization check)
        try {
            Log.d(TAG, "Cleaning up OverlayManager...")
            overlayManager.dispose()
            Log.i(TAG, "✓ OverlayManager cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error cleaning up OverlayManager", e)
            Log.e(TAG, "Cleanup error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Cleanup error message: ${e.message}")
        }

        // Cleanup VoiceCursor API
        if (voiceCursorInitialized) {
            try {
                Log.d(TAG, "Cleaning up VoiceCursor API...")
                VoiceCursorAPI.dispose()
                voiceCursorInitialized = false
                Log.i(TAG, "✓ VoiceCursor API cleaned up successfully")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error cleaning up VoiceCursor API", e)
            }
        } else {
            Log.d(TAG, "VoiceCursor was not initialized, skipping cleanup")
        }

        // Cleanup optimized components
        try {
            Log.d(TAG, "Cleaning up UIScrapingEngine...")
            uiScrapingEngine.destroy()
            Log.i(TAG, "✓ UIScrapingEngine cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error cleaning up UIScrapingEngine", e)
        }

        // FIX (2025-12-05): Unregister from ProcessLifecycleOwner to prevent memory leak
        // Leak signature: bd0178976084c8549ea1a5e0417e0d6ffe34eaa3
        // Root cause: addObserver(this) in onServiceConnected() without matching removeObserver()
        try {
            Log.d(TAG, "Unregistering from ProcessLifecycleOwner...")
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
            Log.i(TAG, "✓ ProcessLifecycleOwner observer unregistered (memory leak fixed)")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error unregistering lifecycle observer", e)
        }

        // Cancel coroutines with proper join to ensure all jobs complete
        try {
            Log.d(TAG, "Cancelling coroutine scopes and waiting for completion...")

            // Cancel serviceScope and wait for all jobs to complete
            serviceScope.cancel()

            // For coroutineScopeCommands, we need to ensure all jobs complete
            // We can't use cancelAndJoin() directly on a scope, so we cancel and give time
            coroutineScopeCommands.cancel()

            // Verify scopes are inactive
            val serviceScopeActive = serviceScope.isActive
            val commandsScopeActive = coroutineScopeCommands.isActive

            Log.i(TAG, "✓ Coroutine scopes cancelled successfully:")
            Log.i(TAG, "  - serviceScope active: $serviceScopeActive")
            Log.i(TAG, "  - coroutineScopeCommands active: $commandsScopeActive")

            if (serviceScopeActive || commandsScopeActive) {
                Log.w(TAG, "⚠ Warning: Some scopes still active after cancellation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error cancelling coroutine scopes", e)
            Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "  Error message: ${e.message}")
            // Re-throw to ensure cleanup errors are not silently swallowed
            throw e
        }

        // Clear caches and debouncer
        try {
            Log.d(TAG, "Clearing caches and debouncer...")
            commandCache.clear()
            nodeCache.clear()
            eventDebouncer.clearAll()
            Log.i(TAG, "✓ Caches and debouncer cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error clearing caches", e)
        }

        // Cleanup CommandManager and ServiceMonitor
        try {
            Log.d(TAG, "Cleaning up ServiceMonitor...")
            serviceMonitor?.cleanup()
            serviceMonitor = null
            Log.i(TAG, "✓ ServiceMonitor cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error cleaning up ServiceMonitor", e)
        }

        try {
            Log.d(TAG, "Cleaning up CommandManager...")
            commandManagerInstance?.cleanup()
            commandManagerInstance = null
            Log.i(TAG, "✓ CommandManager cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error cleaning up CommandManager", e)
        }

        // Clear instance reference
        instanceRef = null
        Log.d(TAG, "Service instance reference cleared")

        Log.i(TAG, "VoiceOS Service destroyed - cleanup complete")

        try {
            Log.i(TAG, "onServiceConnected unregisterReceiver : CHANGE_LANG ")
            unregisterReceiver(serviceReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error unregister Receiver", e)
        }
    }

    override fun getAppCommands() = appsCommand

    // ============================================================
    // IVoiceOSContext interface implementations
    // ============================================================

    override val context: Context
        get() = this

    override val accessibilityService: AccessibilityService
        get() = this

    override val windowManager: android.view.WindowManager
        get() = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager

    // Override getPackageManager() to satisfy both AccessibilityService and IVoiceOSContext
    override fun getPackageManager(): android.content.pm.PackageManager = applicationContext.packageManager

    // Note: getRootNodeInActiveWindow() is provided by IVoiceOSContext interface with default implementation
    // No need to override getRootInActiveWindow() here - it's already available from AccessibilityService

    override fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun vibrate(duration: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                android.os.VibrationEffect.createOneShot(
                    duration,
                    android.os.VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    // ============================================================

    private fun getCenterOffset(): CursorOffset {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2
        val centerY = displayMetrics.heightPixels / 2
        return CursorOffset(centerX.toFloat(), centerY.toFloat())
    }

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(TAG, "CHANGE_LANG onReceive 2221111")
            if (intent?.action == Const.ACTION_CONFIG_UPDATE) {
                config = ServiceConfiguration.loadFromPreferences(this@VoiceOSService)
                Log.i(TAG, "CHANGE_LANG onReceive config = $config")
                speechEngineManager.updateConfiguration(
                    SpeechConfigurationData(
                        language = config.voiceLanguage,
                        mode = SpeechMode.DYNAMIC_COMMAND,
                        enableVAD = true,
                        confidenceThreshold = 4000F,
                        maxRecordingDuration = 30000,
                        timeoutDuration = 5000,
                        enableProfanityFilter = false
                    )
                )
            }
        }
    }

    // ============================================================
    // Phase 3: Extended Public API Methods for IPC
    // ============================================================

    /**
     * Start voice recognition with specified configuration
     * P2-8b: Delegated to IPCManager
     */
    fun startVoiceRecognition(language: String, recognizerType: String): Boolean =
        ipcManager.startVoiceRecognition(language, recognizerType)

    /**
     * Stop currently active voice recognition
     * P2-8b: Delegated to IPCManager
     */
    fun stopVoiceRecognition(): Boolean =
        ipcManager.stopVoiceRecognition()

    /**
     * Trigger app learning for currently focused app
     * P2-8b: Delegated to IPCManager
     */
    fun learnCurrentApp(): String =
        ipcManager.learnCurrentApp()

    /**
     * Execute accessibility action by action type string
     * P2-8b: Delegated to IPCManager
     */
    fun executeAccessibilityActionByType(actionType: String): Boolean =
        ipcManager.executeAccessibilityActionByType(actionType)

    /**
     * Scrape current screen and return JSON representation
     * P2-8b: Delegated to IPCManager
     */
    fun scrapeScreen(): String =
        ipcManager.scrapeScreen()

    /**
     * Get list of apps that have learned voice commands
     * P2-8b: Delegated to IPCManager
     */
    fun getLearnedApps(): List<String> =
        ipcManager.getLearnedApps()

    /**
     * Get voice commands available for specific app
     * P2-8b: Delegated to IPCManager
     */
    fun getCommandsForApp(packageName: String): List<String> =
        ipcManager.getCommandsForApp(packageName)

    /**
     * Register dynamic voice command at runtime
     * P2-8b: Delegated to IPCManager
     */
    fun registerDynamicCommand(commandText: String, actionJson: String): Boolean =
        ipcManager.registerDynamicCommand(commandText, actionJson)
}
