/**
 * IntegrationCoordinator.kt - Centralized integration initialization and lifecycle
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v18)
 * Created: 2026-01-15
 *
 * Extracts integration initialization from VoiceOSService to follow Single Responsibility Principle.
 * Manages LearnApp, CommandDiscovery, AccessibilityScraping, and WebCommand integrations.
 *
 * P2-8e: Part of SOLID refactoring - IntegrationCoordinator extracts ~200 lines from VoiceOSService
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.voiceoscore.learnapp.integration.LearnAppIntegration
import com.augmentalis.voiceoscore.learnapp.integration.CommandDiscoveryIntegration
import com.augmentalis.voiceoscore.scraping.AccessibilityScrapingIntegration
import com.augmentalis.voiceoscore.web.WebCommandCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration Coordinator
 *
 * Centralizes integration lifecycle management for VoiceOSService:
 * - LearnApp integration (third-party app learning)
 * - CommandDiscovery integration (auto-discover commands after exploration)
 * - AccessibilityScraping integration (hash-based scraping)
 * - WebCommand integration (browser command handling)
 * - JIT Learning Service (passive learning foreground service)
 *
 * INITIALIZATION ORDER:
 * 1. AccessibilityScrapingIntegration (requires database)
 * 2. LearnAppIntegration (deferred until first accessibility event)
 * 3. CommandDiscoveryIntegration (requires LearnApp)
 * 4. WebCommandCoordinator (lazy, on first browser detection)
 * 5. JITLearningService (after LearnApp initializes)
 *
 * @param context Application context
 * @param accessibilityService Parent VoiceOSService for accessibility actions
 * @param serviceScope Coroutine scope for async initialization
 * @param databaseManager Database manager for integration dependencies
 */
class IntegrationCoordinator(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val serviceScope: CoroutineScope,
    private val databaseManager: DatabaseManager
) {

    companion object {
        private const val TAG = "IntegrationCoordinator"
    }

    // Integration instances
    private var _learnAppIntegration: LearnAppIntegration? = null
    val learnAppIntegration: LearnAppIntegration?
        get() = _learnAppIntegration

    private var _discoveryIntegration: CommandDiscoveryIntegration? = null
    val discoveryIntegration: CommandDiscoveryIntegration?
        get() = _discoveryIntegration

    private var _scrapingIntegration: AccessibilityScrapingIntegration? = null
    val scrapingIntegration: AccessibilityScrapingIntegration?
        get() = _scrapingIntegration

    private var _webCommandCoordinator: WebCommandCoordinator? = null
    val webCommandCoordinator: WebCommandCoordinator
        get() {
            if (_webCommandCoordinator == null) {
                _webCommandCoordinator = WebCommandCoordinator(context, accessibilityService).also {
                    Log.d(TAG, "WebCommandCoordinator initialized (lazy)")
                }
            }
            return _webCommandCoordinator!!
        }

    // JIT Learning Service state
    private val jitServiceBound = AtomicBoolean(false)

    // LearnApp initialization state
    // State: 0=not started, 1=in progress, 2=complete
    private val learnAppInitState = AtomicInteger(0)
    private val learnAppInitMutex = Mutex()

    // Initialization complete callback
    private var onLearnAppInitialized: (() -> Unit)? = null

    /**
     * JIT Learning Service connection handler
     */
    private val jitServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "JIT Learning Service connected via AIDL")

            try {
                val jitService = com.augmentalis.jitlearning.JITLearningService.getInstance()
                val integration = _learnAppIntegration

                if (jitService != null && integration != null) {
                    // Set JITLearnerProvider (implemented by LearnAppIntegration)
                    jitService.setLearnerProvider(integration)

                    // Set AccessibilityService interface for root node access
                    jitService.setAccessibilityService(object : com.augmentalis.jitlearning.JITLearningService.AccessibilityServiceInterface {
                        override fun getRootNode(): AccessibilityNodeInfo? {
                            return accessibilityService.rootInActiveWindow
                        }

                        override fun performGlobalAction(action: Int): Boolean {
                            return accessibilityService.performGlobalAction(action)
                        }
                    })

                    jitServiceBound.set(true)
                    Log.i(TAG, "JIT Learning Service provider wired successfully")
                    Log.d(TAG, "  - JITLearnerProvider: ${integration.javaClass.simpleName}")
                    Log.d(TAG, "  - AccessibilityService interface: PROVIDED")
                } else {
                    Log.w(TAG, "Cannot wire JIT service - service or integration is null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to wire JIT Learning Service provider", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "JIT Learning Service disconnected")
            jitServiceBound.set(false)
        }
    }

    /**
     * Initialize scraping integration (requires database)
     *
     * Call during service initialization after database is ready.
     */
    fun initializeScrapingIntegration() {
        if (databaseManager.scrapingDatabase == null) {
            Log.w(TAG, "Skipping AccessibilityScrapingIntegration (database not initialized)")
            return
        }

        try {
            _scrapingIntegration = AccessibilityScrapingIntegration(
                context = context,
                accessibilityService = accessibilityService
            )
            Log.i(TAG, "AccessibilityScrapingIntegration initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AccessibilityScrapingIntegration", e)
            _scrapingIntegration = null
        }
    }

    /**
     * Set callback for when LearnApp initialization completes
     */
    fun setOnLearnAppInitialized(callback: () -> Unit) {
        onLearnAppInitialized = callback
    }

    /**
     * Get LearnApp initialization state
     * @return 0=not started, 1=in progress, 2=complete
     */
    fun getLearnAppInitState(): Int = learnAppInitState.get()

    /**
     * Try to start LearnApp initialization (atomic, only first caller wins)
     * @return true if this call started initialization, false if already started/complete
     */
    fun tryStartLearnAppInit(): Boolean {
        return learnAppInitState.compareAndSet(0, 1)
    }

    /**
     * Initialize LearnApp integration
     *
     * DEFERRED INITIALIZATION: Called on first accessibility event to ensure
     * FLAG_RETRIEVE_INTERACTIVE_WINDOWS has been fully processed by Android.
     *
     * Thread-safe via Mutex to prevent race conditions.
     */
    fun initializeLearnAppIntegration() {
        val currentState = learnAppInitState.get()
        if (currentState == 2) {
            Log.d(TAG, "LearnApp already initialized (state=2), skipping")
            return
        }

        serviceScope.launch {
            learnAppInitMutex.withLock {
                try {
                    Log.i(TAG, "=== LearnApp Integration Initialization Start ===")

                    // Initialize UUIDCreator first (required dependency)
                    Log.d(TAG, "Initializing UUIDCreator...")
                    UUIDCreator.initialize(context)
                    Log.d(TAG, "UUIDCreator initialized")

                    // Initialize LearnAppIntegration
                    Log.d(TAG, "Initializing LearnAppIntegration...")
                    _learnAppIntegration = LearnAppIntegration.initialize(context, accessibilityService)
                    Log.i(TAG, "LearnApp integration initialized successfully")

                    Log.d(TAG, "Features enabled:")
                    Log.d(TAG, "  - App launch detection: ACTIVE")
                    Log.d(TAG, "  - Consent dialog management: ACTIVE")
                    Log.d(TAG, "  - Exploration engine: ACTIVE")
                    Log.d(TAG, "  - Progress overlay: ACTIVE")

                    // Initialize CommandDiscoveryIntegration
                    initializeCommandDiscovery()

                    // Mark initialization complete
                    learnAppInitState.set(2)

                    // Start JIT Learning Service
                    startJITService()

                    // Notify callback
                    onLearnAppInitialized?.invoke()

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize LearnApp integration", e)
                    Log.e(TAG, "Error: ${e.javaClass.simpleName}: ${e.message}")
                    _learnAppIntegration = null
                    learnAppInitState.set(0) // Reset for potential retry
                } finally {
                    Log.i(TAG, "=== LearnApp Integration Initialization Complete ===")
                }
            }
        }
    }

    /**
     * Initialize CommandDiscoveryIntegration (requires LearnApp)
     */
    private fun initializeCommandDiscovery() {
        val integration = _learnAppIntegration ?: run {
            Log.w(TAG, "Skipping Command Discovery - LearnAppIntegration not available")
            return
        }

        try {
            _discoveryIntegration = CommandDiscoveryIntegration(
                context = context,
                explorationEngine = integration.getExplorationEngine()
            )
            Log.i(TAG, "Command Discovery integration initialized successfully")
            Log.d(TAG, "  - Visual overlay: ACTIVE (10s auto-hide)")
            Log.d(TAG, "  - Audio summary: ACTIVE")
            Log.d(TAG, "  - Interactive tutorial: ACTIVE")
            Log.d(TAG, "  - Command list UI: ACTIVE")
            Log.d(TAG, "  - Contextual hints: ACTIVE")
            Log.d(TAG, "Auto-observation enabled - no manual wiring needed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Command Discovery integration", e)
            _discoveryIntegration = null
        }
    }

    /**
     * Start JIT Learning Service foreground service
     */
    private fun startJITService() {
        try {
            Log.i(TAG, "Starting JIT Learning Service...")

            val intent = Intent(context, com.augmentalis.jitlearning.JITLearningService::class.java)

            // Start as foreground service (Android O+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            // Bind to service to wire up provider
            val bound = context.bindService(intent, jitServiceConnection, Context.BIND_AUTO_CREATE)

            if (bound) {
                Log.i(TAG, "JIT Learning Service started and binding initiated")
                Log.d(TAG, "  - Service will run as foreground service")
                Log.d(TAG, "  - LearnApp can now bind via AIDL")
            } else {
                Log.e(TAG, "Failed to bind to JIT Learning Service")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start JIT Learning Service", e)
            Log.w(TAG, "Service will continue without JIT Learning")
        }
    }

    /**
     * Forward accessibility event to scraping integration
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Forward to scraping integration
        _scrapingIntegration?.let { integration ->
            try {
                integration.onAccessibilityEvent(event)
            } catch (e: Exception) {
                Log.e(TAG, "Error in AccessibilityScrapingIntegration", e)
            }
        }

        // Forward to LearnApp integration
        _learnAppIntegration?.let { integration ->
            try {
                integration.onAccessibilityEvent(event)
            } catch (e: Exception) {
                Log.e(TAG, "Error in LearnAppIntegration", e)
            }
        }
    }

    /**
     * Check if current app is a browser (for web command routing)
     */
    fun isCurrentAppBrowser(packageName: String): Boolean {
        return webCommandCoordinator.isCurrentAppBrowser(packageName)
    }

    /**
     * Process web command
     * @return true if command was handled
     */
    suspend fun processWebCommand(command: String, packageName: String): Boolean {
        return webCommandCoordinator.processWebCommand(command, packageName)
    }

    /**
     * Cleanup all integrations
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up integrations...")

        // Cleanup scraping integration
        _scrapingIntegration?.let {
            try {
                Log.d(TAG, "Cleaning up AccessibilityScrapingIntegration...")
                // Note: AccessibilityScrapingIntegration cleanup handled internally
                Log.i(TAG, "AccessibilityScrapingIntegration cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up AccessibilityScrapingIntegration", e)
            }
        }
        _scrapingIntegration = null

        // Cleanup discovery integration
        _discoveryIntegration?.let {
            try {
                Log.d(TAG, "Cleaning up CommandDiscoveryIntegration...")
                // Note: CommandDiscoveryIntegration cleanup handled internally
                Log.i(TAG, "CommandDiscoveryIntegration cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up CommandDiscoveryIntegration", e)
            }
        }
        _discoveryIntegration = null

        // Cleanup LearnApp integration
        _learnAppIntegration?.let {
            try {
                Log.d(TAG, "Cleaning up LearnAppIntegration...")
                // Note: LearnAppIntegration cleanup handled internally
                Log.i(TAG, "LearnAppIntegration cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up LearnAppIntegration", e)
            }
        }
        _learnAppIntegration = null

        // Unbind JIT service
        if (jitServiceBound.get()) {
            try {
                context.unbindService(jitServiceConnection)
                jitServiceBound.set(false)
                Log.d(TAG, "JIT Learning Service unbound")
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding JIT service", e)
            }
        }

        // Reset state
        learnAppInitState.set(0)
        onLearnAppInitialized = null

        Log.i(TAG, "IntegrationCoordinator cleanup complete")
    }
}
