/**
 * TestUtils.kt - Common test utilities for Universal Plugin Architecture integration tests
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides reusable test utilities, mock implementations, and factory functions
 * for plugin integration testing.
 */
package com.augmentalis.magiccode.plugins.integration

import com.augmentalis.magiccode.plugins.sdk.BasePlugin
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.magiccode.plugins.universal.data.*
import com.augmentalis.rpc.ServiceEndpoint
import com.augmentalis.rpc.ServiceRegistry
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.element.ElementType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// =============================================================================
// Test Utility Object
// =============================================================================

/**
 * Common test utilities for plugin integration testing.
 *
 * Provides factory methods for creating mock objects and test data
 * consistently across all integration tests.
 */
object TestUtils {

    /**
     * Create a mock PluginConfig with optional settings.
     *
     * @param settings Configuration settings map
     * @param secrets Secret values map
     * @param features Enabled feature flags
     * @return PluginConfig for testing
     */
    fun createMockPluginConfig(
        settings: Map<String, String> = emptyMap(),
        secrets: Map<String, String> = emptyMap(),
        features: Set<String> = emptySet()
    ): PluginConfig {
        return PluginConfig(
            settings = mapOf(
                "testMode" to "true",
                "timeout" to "5000"
            ) + settings,
            secrets = secrets,
            features = setOf("testing") + features
        )
    }

    /**
     * Create a mock PluginContext for testing.
     *
     * @param appDataDir Application data directory path
     * @param cacheDir Cache directory path
     * @return PluginContext for testing
     */
    fun createMockPluginContext(
        appDataDir: String = "/tmp/test/data",
        cacheDir: String = "/tmp/test/cache"
    ): PluginContext {
        return PluginContext(
            appDataDir = appDataDir,
            cacheDir = cacheDir,
            serviceRegistry = ServiceRegistry(),
            eventBus = GrpcPluginEventBus(),
            platformInfo = PlatformInfo(
                platform = "test",
                osVersion = "1.0.0",
                deviceType = "test-device"
            )
        )
    }

    /**
     * Create a mock HandlerContext for testing handler plugins.
     *
     * @param elements List of UI elements on the mock screen
     * @param packageName Mock app package name
     * @param activityName Mock activity name
     * @param previousCommand Previous command for context chaining
     * @param userPreferences User preferences map
     * @return HandlerContext for testing
     */
    fun createMockHandlerContext(
        elements: List<QuantizedElement> = emptyList(),
        packageName: String = "com.test.app",
        activityName: String = "MainActivity",
        previousCommand: QuantizedCommand? = null,
        userPreferences: Map<String, Any> = emptyMap()
    ): HandlerContext {
        return HandlerContext(
            currentScreen = ScreenContext(
                packageName = packageName,
                activityName = activityName,
                screenTitle = "Test Screen",
                elementCount = elements.size,
                primaryAction = "click"
            ),
            elements = elements,
            previousCommand = previousCommand,
            userPreferences = userPreferences
        )
    }

    /**
     * Create a ScreenContext for testing.
     *
     * @param packageName Package name
     * @param activityName Activity name
     * @param screenTitle Screen title
     * @param elementCount Number of elements
     * @param primaryAction Primary action
     * @return ScreenContext for testing
     */
    fun createMockScreenContext(
        packageName: String = "com.test.app",
        activityName: String = "MainActivity",
        screenTitle: String = "Test Screen",
        elementCount: Int = 0,
        primaryAction: String? = "click"
    ): ScreenContext {
        return ScreenContext(
            packageName = packageName,
            activityName = activityName,
            screenTitle = screenTitle,
            elementCount = elementCount,
            primaryAction = primaryAction
        )
    }

    /**
     * Create a mock QuantizedCommand for testing.
     *
     * @param phrase Voice command phrase
     * @param actionType Type of action
     * @param targetAvid Target element AVID
     * @param confidence Confidence score
     * @param packageName Package name in metadata
     * @return QuantizedCommand for testing
     */
    fun createMockQuantizedCommand(
        phrase: String,
        actionType: CommandActionType = CommandActionType.CLICK,
        targetAvid: String? = null,
        confidence: Float = 0.95f,
        packageName: String = "com.test.app"
    ): QuantizedCommand {
        return QuantizedCommand(
            avid = "CMD_${phrase.replace(" ", "_").uppercase()}_${System.currentTimeMillis()}",
            phrase = phrase,
            actionType = actionType,
            targetAvid = targetAvid,
            confidence = confidence,
            metadata = mapOf(
                "packageName" to packageName,
                "screenId" to "test_screen"
            )
        )
    }

    /**
     * Create a mock QuantizedElement for testing.
     *
     * @param avid Element AVID
     * @param label Element label
     * @param type Element type
     * @param actions Available actions
     * @param aliases Alternative labels
     * @return QuantizedElement for testing
     */
    fun createMockQuantizedElement(
        avid: String,
        label: String,
        type: ElementType = ElementType.BUTTON,
        actions: String = "click",
        aliases: List<String> = emptyList()
    ): QuantizedElement {
        return QuantizedElement(
            avid = avid,
            type = type,
            label = label,
            aliases = aliases,
            bounds = "0,0,100,50",
            actions = actions,
            category = "action"
        )
    }

    /**
     * Create a mock ServiceEndpoint for testing.
     *
     * @param serviceName Service name
     * @param host Host address
     * @param port Port number
     * @return ServiceEndpoint for testing
     */
    fun createMockServiceEndpoint(
        serviceName: String = "test.service",
        host: String = "localhost",
        port: Int = 50060
    ): ServiceEndpoint {
        return ServiceEndpoint(
            serviceName = serviceName,
            host = host,
            port = port,
            protocol = "grpc"
        )
    }

    /**
     * Create a list of mock elements for a typical screen.
     *
     * @param count Number of elements to create
     * @return List of QuantizedElements
     */
    fun createMockScreenElements(count: Int = 5): List<QuantizedElement> {
        return (1..count).map { i ->
            createMockQuantizedElement(
                avid = "ELM_${i}_${System.currentTimeMillis()}",
                label = "Button $i",
                type = if (i % 2 == 0) ElementType.BUTTON else ElementType.TEXT,
                actions = if (i % 2 == 0) "click" else ""
            )
        }
    }

    /**
     * Create a mock PluginEvent for testing.
     *
     * @param sourcePluginId Source plugin ID
     * @param eventType Event type
     * @param payload Event payload
     * @return PluginEvent for testing
     */
    fun createMockPluginEvent(
        sourcePluginId: String = "test.plugin",
        eventType: String = "test.event",
        payload: Map<String, String> = emptyMap()
    ): PluginEvent {
        return PluginEvent(
            eventId = "evt_test_${System.currentTimeMillis()}",
            sourcePluginId = sourcePluginId,
            eventType = eventType,
            timestamp = System.currentTimeMillis(),
            payload = payload
        )
    }
}

// =============================================================================
// Test Plugin Implementation
// =============================================================================

/**
 * Configurable test plugin for integration testing.
 *
 * Tracks lifecycle method calls and allows configuration of success/failure
 * for testing error handling scenarios.
 *
 * @param pluginId Unique plugin ID
 * @param failOnInitialize Whether to fail during initialization
 * @param failOnShutdown Whether to fail during shutdown
 * @param failOnPause Whether to fail during pause
 * @param failOnResume Whether to fail during resume
 * @param initDelay Delay in milliseconds before completing initialization
 */
open class TestPlugin(
    override val pluginId: String = "test.plugin",
    private val failOnInitialize: Boolean = false,
    private val failOnShutdown: Boolean = false,
    private val failOnPause: Boolean = false,
    private val failOnResume: Boolean = false,
    private val initDelay: Long = 0
) : BasePlugin() {

    override val pluginName: String = "Test Plugin: $pluginId"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.LLM_TEXT_GENERATION,
            name = "Test Capability",
            version = "1.0.0"
        )
    )

    // =========================================================================
    // Tracking Variables
    // =========================================================================

    /** Count of initialize calls */
    var initializeCount = 0
        private set

    /** Count of shutdown calls */
    var shutdownCount = 0
        private set

    /** Count of pause calls */
    var pauseCount = 0
        private set

    /** Count of resume calls */
    var resumeCount = 0
        private set

    /** Count of activate calls */
    var activateCount = 0
        private set

    /** Count of config change calls */
    var configChangeCount = 0
        private set

    /** List of received events */
    val events = mutableListOf<PluginEvent>()

    /** List of received config changes */
    val configChanges = mutableListOf<Map<String, Any>>()

    /** Last health check result */
    var lastHealthCheck: HealthStatus? = null
        private set

    /** Simulated health state */
    var simulatedHealthy: Boolean = true

    // =========================================================================
    // Lifecycle Implementation
    // =========================================================================

    override suspend fun onInitialize(): InitResult {
        initializeCount++

        if (initDelay > 0) {
            delay(initDelay)
        }

        return if (failOnInitialize) {
            InitResult.failure("Simulated initialization failure", recoverable = true)
        } else {
            InitResult.success("Test plugin initialized successfully")
        }
    }

    override suspend fun onActivate() {
        activateCount++
    }

    override suspend fun onPause() {
        pauseCount++
        if (failOnPause) {
            throw RuntimeException("Simulated pause failure")
        }
    }

    override suspend fun onResume() {
        resumeCount++
        if (failOnResume) {
            throw RuntimeException("Simulated resume failure")
        }
    }

    override suspend fun onShutdown() {
        shutdownCount++
        if (failOnShutdown) {
            throw RuntimeException("Simulated shutdown failure")
        }
    }

    override suspend fun onConfigChanged(config: Map<String, Any>) {
        configChangeCount++
        configChanges.add(config)
    }

    override suspend fun onEventReceived(event: PluginEvent) {
        events.add(event)
    }

    override fun getHealthDiagnostics(): Map<String, String> = mapOf(
        "initializeCount" to initializeCount.toString(),
        "shutdownCount" to shutdownCount.toString(),
        "pauseCount" to pauseCount.toString(),
        "resumeCount" to resumeCount.toString(),
        "eventCount" to events.size.toString(),
        "simulatedHealthy" to simulatedHealthy.toString()
    )

    override fun healthCheck(): HealthStatus {
        val status = if (simulatedHealthy) {
            HealthStatus.healthy(
                message = "Test plugin is healthy",
                diagnostics = getHealthDiagnostics()
            )
        } else {
            HealthStatus.unhealthy(
                message = "Test plugin is unhealthy (simulated)",
                diagnostics = getHealthDiagnostics()
            )
        }
        lastHealthCheck = status
        return status
    }

    /**
     * Reset all tracking counters and lists.
     */
    fun reset() {
        initializeCount = 0
        shutdownCount = 0
        pauseCount = 0
        resumeCount = 0
        activateCount = 0
        configChangeCount = 0
        events.clear()
        configChanges.clear()
        lastHealthCheck = null
        simulatedHealthy = true
    }
}

// =============================================================================
// Test Handler Plugin Implementation
// =============================================================================

/**
 * Test handler plugin for testing handler-specific functionality.
 *
 * @param pluginId Unique plugin ID
 * @param handlerType Type of handler
 * @param supportedPhrases Phrases this handler can process
 * @param shouldSucceed Whether handler execution should succeed
 */
open class TestHandlerPlugin(
    override val pluginId: String = "test.handler",
    override val handlerType: HandlerType = HandlerType.UI_INTERACTION,
    private val supportedPhrases: List<String> = listOf("test", "click", "tap"),
    private val shouldSucceed: Boolean = true
) : BasePlugin(), HandlerPlugin {

    override val pluginName: String = "Test Handler Plugin"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.ACCESSIBILITY_HANDLER,
            name = "Test Handler",
            version = "1.0.0"
        )
    )

    override val patterns: List<CommandPattern> = supportedPhrases.map { phrase ->
        CommandPattern(
            regex = Regex("^$phrase$", RegexOption.IGNORE_CASE),
            intent = phrase.uppercase(),
            examples = listOf(phrase)
        )
    }

    /** List of handled commands */
    val handledCommands = mutableListOf<QuantizedCommand>()

    /** List of canHandle calls */
    val canHandleCalls = mutableListOf<QuantizedCommand>()

    override suspend fun onInitialize(): InitResult {
        return InitResult.success("Test handler initialized")
    }

    override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        canHandleCalls.add(command)
        val phrase = command.phrase.lowercase().trim()
        return supportedPhrases.any { it.lowercase() == phrase }
    }

    override suspend fun handle(command: QuantizedCommand, context: HandlerContext): ActionResult {
        handledCommands.add(command)
        return if (shouldSucceed) {
            ActionResult.Success("Test handler executed: ${command.phrase}")
        } else {
            ActionResult.Error("Test handler failed: ${command.phrase}")
        }
    }

    override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
        val phrase = command.phrase.lowercase().trim()
        return if (supportedPhrases.any { it.lowercase() == phrase }) {
            0.95f
        } else {
            0.0f
        }
    }

    /**
     * Reset tracking lists.
     */
    fun reset() {
        handledCommands.clear()
        canHandleCalls.clear()
    }
}

// =============================================================================
// Mock Data Provider Implementation
// =============================================================================

/**
 * Mock AccessibilityDataProvider for testing data access.
 *
 * Provides configurable mock data for testing without requiring real accessibility services.
 */
class MockAccessibilityDataProvider(
    private var elements: List<QuantizedElement> = emptyList(),
    private var commands: List<QuantizedCommand> = emptyList(),
    private var screenContext: ScreenContext = ScreenContext(
        packageName = "",
        activityName = "",
        screenTitle = "Unknown Screen",
        elementCount = 0,
        primaryAction = null
    )
) : AccessibilityDataProvider {

    private val _screenElementsFlow = MutableStateFlow<List<QuantizedElement>>(elements)
    override val screenElementsFlow: StateFlow<List<QuantizedElement>> = _screenElementsFlow.asStateFlow()

    private val _screenContextFlow = MutableStateFlow<ScreenContext?>(screenContext)
    override val screenContextFlow: StateFlow<ScreenContext?> = _screenContextFlow.asStateFlow()

    /** Count of getCurrentScreenElements calls */
    var getElementsCallCount = 0
        private set

    /** Count of getElement calls */
    var getElementCallCount = 0
        private set

    /** Count of getScreenCommands calls */
    var getCommandsCallCount = 0
        private set

    override suspend fun getCurrentScreenElements(): List<QuantizedElement> {
        getElementsCallCount++
        return elements
    }

    override suspend fun getElement(avid: String): QuantizedElement? {
        getElementCallCount++
        return elements.find { it.avid == avid }
    }

    override suspend fun getScreenCommands(): List<QuantizedCommand> {
        getCommandsCallCount++
        return commands
    }

    override suspend fun getCommandHistory(limit: Int, successOnly: Boolean): List<CommandHistoryEntry> {
        return emptyList()
    }

    override suspend fun getTopCommands(limit: Int, context: String?): List<RankedCommand> {
        return emptyList()
    }

    override suspend fun getScreenContext(): ScreenContext {
        return screenContext
    }

    override suspend fun getNavigationGraph(packageName: String): NavigationGraph {
        return NavigationGraph.empty(packageName)
    }

    override suspend fun getContextPreferences(): List<ContextPreference> {
        return emptyList()
    }

    /**
     * Update the mock elements.
     */
    fun setElements(newElements: List<QuantizedElement>) {
        elements = newElements
        _screenElementsFlow.value = newElements
    }

    /**
     * Update the mock commands.
     */
    fun setCommands(newCommands: List<QuantizedCommand>) {
        commands = newCommands
    }

    /**
     * Update the mock screen context.
     */
    fun setScreenContext(newContext: ScreenContext) {
        screenContext = newContext
        _screenContextFlow.value = newContext
    }

    /**
     * Reset call counters.
     */
    fun resetCounters() {
        getElementsCallCount = 0
        getElementCallCount = 0
        getCommandsCallCount = 0
    }
}

// =============================================================================
// Extension Functions for Testing
// =============================================================================

/**
 * Extension to create a ScreenContext with isLearned property for tests.
 */
val ScreenContext.isLearned: Boolean
    get() = elementCount > 0

/**
 * Extension to get screen ID for tests.
 */
fun ScreenContext.getScreenId(): String = "$packageName/$activityName"

/**
 * Check if NavigationGraph is empty.
 */
fun NavigationGraph.isEmpty(): Boolean = nodes.isEmpty() && edges.isEmpty()
