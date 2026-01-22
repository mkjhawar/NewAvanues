/**
 * RefactoringTestFixtures.kt - Test data fixtures
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-15 03:37:00 PDT
 * Part of: VoiceOSService SOLID Refactoring - Day 3 Afternoon
 */
package com.augmentalis.voiceoscore.refactoring.utils

import com.augmentalis.voiceoscore.refactoring.interfaces.*

/**
 * Test Data Fixtures
 *
 * Provides pre-configured test data for all interfaces:
 * - Common test scenarios
 * - Edge cases
 * - Error conditions
 * - Performance test data
 */
object RefactoringTestFixtures {

    // ========================================
    // Command Orchestrator Fixtures
    // ========================================

    object CommandOrchestrator {
        val sampleCommands = listOf(
            "open settings",
            "click ok",
            "scroll down",
            "go back",
            "switch to chrome"
        )

        val lowConfidenceCommands = listOf(
            "mumblemumble" to 0.3f,
            "unclear speech" to 0.4f,
            "background noise" to 0.45f
        )

        val highConfidenceCommands = listOf(
            "open settings" to 0.95f,
            "click ok" to 0.92f,
            "go back" to 0.98f
        )

        val tier1Commands = listOf("click ok", "scroll down", "swipe left")
        val tier2Commands = listOf("open chrome", "switch to gmail")
        val tier3Commands = listOf("tap coordinate", "gesture action")

        fun createSuccessResult(tier: Int = 1, executionTimeMs: Long = 10) =
            ICommandOrchestrator.CommandResult.Success(tier, executionTimeMs)

        fun createFailureResult(tier: Int? = null, reason: String = "Mock failure") =
            ICommandOrchestrator.CommandResult.Failure(tier, reason)
    }

    // ========================================
    // Event Router Fixtures
    // ========================================

    object EventRouter {
        val commonPackageNames = listOf(
            "com.android.settings",
            "com.android.chrome",
            "com.google.android.gms",
            "com.test.app"
        )

        val systemPackages = listOf(
            "com.android.systemui",
            "com.android.launcher",
            "android"
        )

        val testEventTypes = listOf(
            android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED,
            android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED,
            android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        )

        fun createEventConfig(
            debounceMs: Long = 1000,
            packageFilters: Set<String> = systemPackages.toSet()
        ) = IEventRouter.EventRouterConfig(
            defaultDebounceMs = debounceMs,
            packageFilters = packageFilters
        )
    }

    // ========================================
    // Speech Manager Fixtures
    // ========================================

    object SpeechManager {
        val sampleRecognitions = listOf(
            "hello world" to 0.95f,
            "open settings" to 0.92f,
            "click button" to 0.88f,
            "unclear speech" to 0.45f
        )

        val partialResults = listOf(
            "hel",
            "hello",
            "hello wor",
            "hello world"
        )

        val engineConfigs = mapOf(
            ISpeechManager.SpeechEngine.VIVOKA to ISpeechManager.SpeechConfig(
                preferredEngine = ISpeechManager.SpeechEngine.VIVOKA,
                minConfidenceThreshold = 0.7f
            ),
            ISpeechManager.SpeechEngine.VOSK to ISpeechManager.SpeechConfig(
                preferredEngine = ISpeechManager.SpeechEngine.VOSK,
                minConfidenceThreshold = 0.6f,
                enableAutoFallback = false
            ),
            ISpeechManager.SpeechEngine.GOOGLE to ISpeechManager.SpeechConfig(
                preferredEngine = ISpeechManager.SpeechEngine.GOOGLE,
                minConfidenceThreshold = 0.5f
            )
        )

        val commonErrors = listOf(
            ISpeechManager.RecognitionError(
                errorCode = ISpeechManager.ErrorCode.NETWORK_ERROR,
                message = "Network unavailable",
                engine = ISpeechManager.SpeechEngine.VIVOKA,
                isRecoverable = true
            ),
            ISpeechManager.RecognitionError(
                errorCode = ISpeechManager.ErrorCode.NO_SPEECH_DETECTED,
                message = "No speech input",
                engine = ISpeechManager.SpeechEngine.VOSK,
                isRecoverable = true
            )
        )

        fun createEngineStatus(
            engine: ISpeechManager.SpeechEngine,
            isHealthy: Boolean = true
        ) = ISpeechManager.EngineStatus(
            engine = engine,
            isInitialized = true,
            isAvailable = isHealthy,
            isHealthy = isHealthy,
            successRate = if (isHealthy) 0.95f else 0.5f,
            averageConfidence = if (isHealthy) 0.85f else 0.6f,
            totalRecognitions = 100
        )
    }

    // ========================================
    // UI Scraping Service Fixtures
    // ========================================

    object UIScrapingService {
        fun createUIElement(
            text: String = "Sample Button",
            packageName: String = "com.test.app",
            resourceId: String? = "button_ok",
            isClickable: Boolean = true
        ) = IUIScrapingService.UIElement(
            text = text,
            contentDescription = null,
            resourceId = resourceId,
            className = "android.widget.Button",
            packageName = packageName,
            isClickable = isClickable,
            isFocusable = true,
            isEnabled = true,
            isScrollable = false,
            bounds = IUIScrapingService.ElementBounds(0, 0, 100, 50),
            normalizedText = text.toLowerCase(),
            hash = text.hashCode().toString()
        )

        val sampleElements = listOf(
            createUIElement("OK", resourceId = "button_ok"),
            createUIElement("Cancel", resourceId = "button_cancel"),
            createUIElement("Settings", resourceId = "button_settings"),
            createUIElement("Back", resourceId = "button_back")
        )

        fun createGeneratedCommand(
            commandText: String = "click ok",
            packageName: String = "com.test.app"
        ) = IUIScrapingService.GeneratedCommand(
            commandText = commandText,
            normalizedText = commandText.toLowerCase(),
            targetElement = createUIElement(commandText.substringAfter("click ")),
            confidence = 0.9f
        )

        val sampleCommands = listOf(
            createGeneratedCommand("click ok"),
            createGeneratedCommand("click cancel"),
            createGeneratedCommand("tap settings")
        )

        fun createScrapingConfig(
            maxCacheSize: Int = 100,
            enablePersistence: Boolean = true
        ) = IUIScrapingService.ScrapingConfig(
            maxCacheSize = maxCacheSize,
            enablePersistence = enablePersistence,
            enableCommandGeneration = true,
            minTextLength = 2,
            maxDepth = 10
        )
    }

    // ========================================
    // Service Monitor Fixtures
    // ========================================

    object ServiceMonitor {
        fun createHealthyComponentHealth(component: IServiceMonitor.MonitoredComponent) =
            IServiceMonitor.ComponentHealth(
                component = component,
                status = IServiceMonitor.HealthStatus.HEALTHY,
                isResponsive = true,
                lastCheckTime = System.currentTimeMillis(),
                errorCount = 0
            )

        fun createUnhealthyComponentHealth(
            component: IServiceMonitor.MonitoredComponent,
            errorMessage: String = "Component not responding"
        ) = IServiceMonitor.ComponentHealth(
            component = component,
            status = IServiceMonitor.HealthStatus.UNHEALTHY,
            isResponsive = false,
            lastCheckTime = System.currentTimeMillis(),
            errorCount = 5,
            errorMessage = errorMessage
        )

        fun createPerformanceSnapshot(
            cpuPercent: Float = 2.5f,
            memoryMb: Long = 12L
        ) = IServiceMonitor.PerformanceSnapshot(
            timestamp = System.currentTimeMillis(),
            cpuUsagePercent = cpuPercent,
            memoryUsageMb = memoryMb,
            batteryDrainPercent = 0.5f,
            eventProcessingRate = 10f,
            commandExecutionRate = 2f,
            averageResponseTimeMs = 50L,
            activeThreads = 3,
            queuedEvents = 0
        )

        fun createMonitorConfig(
            healthCheckIntervalMs: Long = 5000,
            enableAutoRecovery: Boolean = true
        ) = IServiceMonitor.MonitorConfig(
            healthCheckIntervalMs = healthCheckIntervalMs,
            metricsCollectionIntervalMs = 1000,
            enablePerformanceMonitoring = true,
            enableAutoRecovery = enableAutoRecovery,
            maxRecoveryAttempts = 3
        )

        val sampleAlerts = listOf(
            IServiceMonitor.HealthAlert(
                severity = IServiceMonitor.AlertSeverity.WARNING,
                component = IServiceMonitor.MonitoredComponent.SPEECH_ENGINE,
                message = "High error rate detected",
                timestamp = System.currentTimeMillis()
            ),
            IServiceMonitor.HealthAlert(
                severity = IServiceMonitor.AlertSeverity.ERROR,
                component = IServiceMonitor.MonitoredComponent.DATABASE,
                message = "Database connection lost",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // ========================================
    // Database Manager Fixtures
    // ========================================

    object DatabaseManager {
        fun createVoiceCommand(
            id: String = "cmd_001",
            primaryText: String = "open settings",
            locale: String = "en_US"
        ) = IDatabaseManager.VoiceCommand(
            id = id,
            primaryText = primaryText,
            synonyms = listOf("settings", "preferences"),
            locale = locale,
            category = "navigation",
            action = "OPEN_SETTINGS",
            parameters = emptyMap()
        )

        val sampleVoiceCommands = listOf(
            createVoiceCommand("cmd_001", "open settings"),
            createVoiceCommand("cmd_002", "click ok"),
            createVoiceCommand("cmd_003", "go back")
        )

        fun createScrapedElement(
            hash: String = "hash_001",
            packageName: String = "com.test.app",
            text: String = "OK"
        ) = IDatabaseManager.ScrapedElement(
            hash = hash,
            packageName = packageName,
            text = text,
            contentDescription = null,
            resourceId = "button_ok",
            className = "android.widget.Button",
            isClickable = true,
            bounds = "{left:0,top:0,right:100,bottom:50}"
        )

        fun createGeneratedCommand(
            commandText: String = "click ok",
            packageName: String = "com.test.app"
        ) = IDatabaseManager.GeneratedCommand(
            commandText = commandText,
            normalizedText = commandText.toLowerCase(),
            packageName = packageName,
            elementHash = "hash_001",
            confidence = 0.9f
        )

        fun createDatabaseConfig(
            enableCaching: Boolean = true,
            cacheSize: Int = 100
        ) = IDatabaseManager.DatabaseConfig(
            enableCaching = enableCaching,
            cacheSize = cacheSize,
            enableOptimization = true,
            retentionDays = 30
        )
    }

    // ========================================
    // State Manager Fixtures
    // ========================================

    object StateManager {
        fun createServiceConfiguration(
            fingerprintGesturesEnabled: Boolean = false,
            commandCheckIntervalMs: Long = 500
        ) = IStateManager.ServiceConfiguration(
            fingerprintGesturesEnabled = fingerprintGesturesEnabled,
            commandCheckIntervalMs = commandCheckIntervalMs,
            commandLoadDebounceMs = 500,
            eventDebounceMs = 1000,
            cacheSize = 100,
            initDelayMs = 200
        )

        fun createStateConfig(
            enablePersistence: Boolean = true,
            enableValidation: Boolean = true
        ) = IStateManager.StateConfig(
            enablePersistence = enablePersistence,
            enableValidation = enableValidation,
            maxHistorySize = 100,
            autoSaveIntervalMs = 5000
        )

        fun createStateSnapshot(
            isServiceReady: Boolean = true,
            isVoiceInitialized: Boolean = true
        ) = IStateManager.StateSnapshot(
            timestamp = System.currentTimeMillis(),
            isServiceReady = isServiceReady,
            isVoiceInitialized = isVoiceInitialized,
            isCommandProcessing = false,
            isForegroundServiceActive = true,
            isAppInBackground = false,
            isVoiceSessionActive = false,
            isVoiceCursorInitialized = true,
            isFallbackModeEnabled = false,
            lastCommandLoadedTime = System.currentTimeMillis(),
            configuration = createServiceConfiguration(),
            validationResult = IStateManager.ValidationResult.Valid
        )
    }

    // ========================================
    // Performance Test Data
    // ========================================

    object Performance {
        const val MAX_DI_OVERHEAD_MS = 5L
        const val MAX_CPU_PERCENT = 5f
        const val MAX_MEMORY_MB = 20L
        const val MAX_RESPONSE_TIME_MS = 100L

        val acceptableMetricRanges = mapOf(
            "cpu" to 0.0..5.0,
            "memory" to 0.0..20.0,
            "responseTime" to 0.0..100.0
        )
    }
}
