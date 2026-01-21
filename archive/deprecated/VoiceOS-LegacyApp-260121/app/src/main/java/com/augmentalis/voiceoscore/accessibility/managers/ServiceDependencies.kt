/**
 * ServiceDependencies.kt - Dependency injection interface for VoiceOSService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v18)
 * Created: 2025-12-27
 * Updated: 2026-01-19 - Consolidated to use KMP classes only
 *
 * Provides dependency abstraction for testability. Since AccessibilityService
 * cannot use @AndroidEntryPoint, this interface enables manual DI with the
 * ability to swap implementations for testing.
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.accessibilityservice.AccessibilityService
import com.augmentalis.voiceoscore.ActionCoordinator
import com.augmentalis.voiceoscore.AndroidScreenExtractor
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.EventPriorityManager
import com.augmentalis.voiceoscore.IAppVersionDetector
import com.augmentalis.voiceoscore.OverlayManager as KmpOverlayManager
import com.augmentalis.voiceoscore.SpeechEngineManager
import com.augmentalis.voiceoscore.SpeechEngineFactoryProvider
import com.augmentalis.voiceoscore.createAppVersionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Service Dependencies Interface
 *
 * Abstracts core dependencies required by VoiceOSService using KMP classes.
 * All components are from the VoiceOSCore KMP module.
 *
 * Implementations:
 * - [ProductionServiceDependencies]: Real dependencies for production
 * - Test implementations can mock individual components
 */
interface IServiceDependencies {

    /** Speech recognition engine manager (KMP) */
    val speechEngineManager: SpeechEngineManager

    /** UI element extraction engine (KMP AndroidScreenExtractor) */
    val screenExtractor: AndroidScreenExtractor

    /** Overlay display manager (KMP OverlayManager) */
    val overlayManager: KmpOverlayManager

    /** App version detection (KMP IAppVersionDetector) */
    val appVersionDetector: IAppVersionDetector

    /** Action coordinator (KMP ActionCoordinator) */
    val actionCoordinator: ActionCoordinator

    /** Event priority manager (KMP EventPriorityManager) */
    val eventPriorityManager: EventPriorityManager

    /** Cache lock for thread-safe cache access */
    val cacheLock: ReentrantReadWriteLock

    /** Node cache for UI elements (KMP ElementInfo) */
    val nodeCache: MutableList<ElementInfo>

    /** Command cache for normalized command text */
    val commandCache: MutableList<String>

    /**
     * Initialize all dependencies
     * Called during service startup
     */
    fun initialize()

    /**
     * Cleanup all dependencies
     * Called during service shutdown
     */
    fun cleanup()
}

/**
 * Production Service Dependencies
 *
 * Real implementation of [IServiceDependencies] for production use.
 * Uses lazy initialization for efficient resource usage.
 * All managers are from the VoiceOSCore KMP module.
 *
 * @param service The AccessibilityService instance
 * @param isServiceReady Supplier function to check service readiness
 */
class ProductionServiceDependencies(
    private val service: AccessibilityService,
    private val isServiceReady: () -> Boolean
) : IServiceDependencies {

    companion object {
        private const val TAG = "ServiceDependencies"
    }

    // Coroutine scope for managers
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val speechEngineManager: SpeechEngineManager by lazy {
        SpeechEngineManager(SpeechEngineFactoryProvider.create(), serviceScope).also {
            android.util.Log.d(TAG, "SpeechEngineManager initialized (lazy)")
        }
    }

    override val screenExtractor: AndroidScreenExtractor by lazy {
        AndroidScreenExtractor().also {
            android.util.Log.d(TAG, "AndroidScreenExtractor initialized (lazy)")
        }
    }

    override val overlayManager: KmpOverlayManager by lazy {
        KmpOverlayManager().also {
            android.util.Log.d(TAG, "OverlayManager initialized (lazy)")
        }
    }

    override val appVersionDetector: IAppVersionDetector by lazy {
        createAppVersionDetector(service.applicationContext).also {
            android.util.Log.d(TAG, "AppVersionDetector initialized (lazy)")
        }
    }

    override val eventPriorityManager: EventPriorityManager by lazy {
        EventPriorityManager().also {
            android.util.Log.d(TAG, "EventPriorityManager initialized (lazy)")
        }
    }

    override val cacheLock: ReentrantReadWriteLock by lazy {
        ReentrantReadWriteLock().also {
            android.util.Log.d(TAG, "CacheLock initialized (lazy)")
        }
    }

    override val nodeCache: MutableList<ElementInfo> by lazy {
        CopyOnWriteArrayList<ElementInfo>().also {
            android.util.Log.d(TAG, "NodeCache initialized (lazy)")
        }
    }

    override val commandCache: MutableList<String> by lazy {
        CopyOnWriteArrayList<String>().also {
            android.util.Log.d(TAG, "CommandCache initialized (lazy)")
        }
    }

    override val actionCoordinator: ActionCoordinator by lazy {
        ActionCoordinator().also {
            android.util.Log.d(TAG, "ActionCoordinator initialized (lazy)")
        }
    }

    override fun initialize() {
        android.util.Log.d(TAG, "Initializing production dependencies...")
        // Dependencies are lazy-initialized on first access
        // This method can be used for eager initialization if needed
    }

    override fun cleanup() {
        android.util.Log.d(TAG, "Cleaning up production dependencies...")

        // Cleanup speech engine
        speechEngineManager.cleanup()

        // Cleanup app version detector monitoring
        appVersionDetector.stopMonitoring()

        android.util.Log.i(TAG, "All dependencies cleaned up")
    }
}

/**
 * Factory for creating service dependencies
 *
 * Allows injection of custom dependencies for testing
 */
object ServiceDependenciesFactory {

    @Volatile
    private var testDependencies: IServiceDependencies? = null

    /**
     * Create dependencies for a service
     *
     * @param service The AccessibilityService instance
     * @param isServiceReady Supplier function to check service readiness
     * @return Dependencies instance (test or production)
     */
    fun create(
        service: AccessibilityService,
        isServiceReady: () -> Boolean
    ): IServiceDependencies {
        return testDependencies ?: ProductionServiceDependencies(service, isServiceReady)
    }

    /**
     * Set test dependencies (for unit tests only)
     *
     * @param dependencies Mock dependencies or null to use production
     */
    @JvmStatic
    fun setTestDependencies(dependencies: IServiceDependencies?) {
        testDependencies = dependencies
    }

    /**
     * Reset to production dependencies (call in test teardown)
     */
    @JvmStatic
    fun reset() {
        testDependencies = null
    }
}
