/**
 * AccessibilityServiceTest.kt - Tests for VoiceOS accessibility service enablement
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-30
 *
 * Tests:
 * - VoiceOS accessibility service detection
 * - Service configuration verification
 * - Accessibility event processing
 * - AccessibilityNodeInfo parsing
 */
package com.augmentalis.voiceoscore.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Rect
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.voiceoscore.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceoscore.mocks.MockVoiceAccessibilityService
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * Accessibility Service Test Suite
 *
 * Verifies VoiceOS accessibility service functionality including:
 * - Service detection and enablement
 * - Service configuration
 * - Accessibility event handling
 * - AccessibilityNodeInfo parsing
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityServiceTest {

    companion object {
        private const val TAG = "AccessibilityServiceTest"
        private const val VOICEOS_SERVICE_CLASS = "com.augmentalis.voiceoscore.accessibility.VoiceOSService"
        private const val VOICEOS_PACKAGE = "com.augmentalis.voiceos"
        private const val TEST_TIMEOUT_MS = 5000L
    }

    private lateinit var context: Context
    private lateinit var instrumentationContext: Context
    private lateinit var accessibilityManager: AccessibilityManager
    private var mockService: MockVoiceAccessibilityService? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        mockService = MockVoiceAccessibilityService()
    }

    @After
    fun tearDown() {
        mockService?.reset()
        mockService = null
    }

    // ============================================================================
    // SECTION 1: Service Detection Tests
    // ============================================================================

    /**
     * Test that VoiceOS service component name is correctly formed
     */
    @Test
    fun testVoiceOSServiceComponentName() {
        val componentName = ComponentName(VOICEOS_PACKAGE, VOICEOS_SERVICE_CLASS)

        assertEquals("Package name should match", VOICEOS_PACKAGE, componentName.packageName)
        assertEquals("Class name should match", VOICEOS_SERVICE_CLASS, componentName.className)

        val flattenedString = componentName.flattenToString()
        assertTrue("Flattened name should contain package", flattenedString.contains(VOICEOS_PACKAGE))
        assertTrue("Flattened name should contain service class", flattenedString.contains("VoiceOSService"))
    }

    /**
     * Test that we can check enabled accessibility services setting
     */
    @Test
    fun testAccessibilityServicesSettingAccess() {
        // This test verifies that the setting can be accessed (not necessarily that VoiceOS is enabled)
        val enabledServices = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } catch (e: Exception) {
            null
        }

        // The setting should be accessible (may be null if no services enabled)
        // We're testing the access pattern, not the value
        println("Enabled accessibility services: $enabledServices")

        // Parse the enabled services list if not empty
        if (!enabledServices.isNullOrEmpty()) {
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)

            val services = mutableListOf<String>()
            while (splitter.hasNext()) {
                services.add(splitter.next())
            }

            println("Parsed ${services.size} enabled services")
            services.forEach { println("  - $it") }
        }
    }

    /**
     * Test service detection logic for VoiceOS
     */
    @Test
    fun testVoiceOSServiceDetectionLogic() {
        // Test the detection logic with various enabled services strings
        val testCases = listOf(
            "" to false,
            "com.other.app/.OtherService" to false,
            "com.augmentalis.voiceos/.accessibility.VoiceOSService" to true,
            "com.augmentalis.voiceos/com.augmentalis.voiceoscore.accessibility.VoiceOSService" to true,
            "com.other.app/.OtherService:com.augmentalis.voiceos/.accessibility.VoiceOSService" to true,
            "com.augmentalis.voiceos/com.augmentalis.voiceoscore.accessibility.VoiceOSService:com.other.app/.OtherService" to true
        )

        testCases.forEach { (enabledServices, expected) ->
            val isEnabled = isVoiceOSServiceInEnabledList(enabledServices)
            assertEquals(
                "VoiceOS detection for '$enabledServices' should be $expected",
                expected,
                isEnabled
            )
        }
    }

    private fun isVoiceOSServiceInEnabledList(enabledServices: String): Boolean {
        if (enabledServices.isEmpty()) return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)

        while (splitter.hasNext()) {
            val componentName = splitter.next()
            if (componentName.contains(VOICEOS_SERVICE_CLASS) ||
                componentName.contains("VoiceOSService")) {
                return true
            }
        }

        return false
    }

    /**
     * Test AccessibilityManager service listing
     */
    @Test
    fun testAccessibilityManagerServiceListing() {
        // Get installed accessibility services
        val installedServices = accessibilityManager.installedAccessibilityServiceList

        println("Installed accessibility services: ${installedServices.size}")
        installedServices.forEach { serviceInfo ->
            println("  - ${serviceInfo.resolveInfo.serviceInfo.packageName}/${serviceInfo.resolveInfo.serviceInfo.name}")
        }

        // Get enabled accessibility services
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )

        println("Enabled accessibility services: ${enabledServices.size}")
        enabledServices.forEach { serviceInfo ->
            println("  - ${serviceInfo.resolveInfo.serviceInfo.packageName}/${serviceInfo.resolveInfo.serviceInfo.name}")
        }
    }

    // ============================================================================
    // SECTION 2: Service Configuration Tests
    // ============================================================================

    /**
     * Test default service configuration
     */
    @Test
    fun testDefaultServiceConfiguration() {
        val config = ServiceConfiguration.createDefault()

        // Verify default values
        assertTrue("Default config should be enabled", config.isEnabled)
        assertTrue("Handlers should be enabled by default", config.handlersEnabled)
        assertTrue("App launching should be enabled by default", config.appLaunchingEnabled)
        assertTrue("Dynamic commands should be enabled by default", config.dynamicCommandsEnabled)
        assertTrue("Cursor should be enabled by default", config.cursorEnabled)
        assertFalse("UI scraping should be disabled by default", config.uiScrapingEnabled)
        assertTrue("Command caching should be enabled by default", config.commandCachingEnabled)

        // Verify voice settings
        assertTrue("Voice recognition should be enabled by default", config.voiceRecognitionEnabled)
        assertFalse("Voice auto-start should be disabled by default", config.voiceAutoStart)
        assertEquals("Default voice engine should be google", "google", config.voiceEngine)
        assertEquals("Default language should be en-US", "en-US", config.voiceLanguage)
    }

    /**
     * Test configuration validation
     */
    @Test
    fun testServiceConfigurationValidation() {
        // Valid configuration
        val validConfig = ServiceConfiguration.createDefault()
        assertTrue("Default config should be valid", validConfig.validate())

        // Invalid configurations
        val invalidConfigs = listOf(
            ServiceConfiguration(maxCacheSize = -1),
            ServiceConfiguration(commandTimeout = -1L),
            ServiceConfiguration(initTimeout = -1L),
            ServiceConfiguration(cursorSize = -1f),
            ServiceConfiguration(cursorSpeed = -1f),
            ServiceConfiguration(voiceMinConfidence = -0.5f),
            ServiceConfiguration(voiceMinConfidence = 1.5f),
            ServiceConfiguration(voiceEngine = ""),
            ServiceConfiguration(voiceLanguage = "")
        )

        invalidConfigs.forEachIndexed { index, config ->
            assertFalse("Configuration $index should be invalid", config.validate())
        }
    }

    /**
     * Test configuration serialization to map
     */
    @Test
    fun testServiceConfigurationSerialization() {
        val config = ServiceConfiguration(
            isEnabled = true,
            verboseLogging = true,
            handlersEnabled = true,
            cursorEnabled = true,
            voiceEngine = "azure",
            voiceLanguage = "es-ES"
        )

        val map = config.toMap()

        assertEquals("enabled should match", true, map["enabled"])
        assertEquals("verbose_logging should match", true, map["verbose_logging"])
        assertEquals("handlers_enabled should match", true, map["handlers_enabled"])
        assertEquals("cursor_enabled should match", true, map["cursor_enabled"])
        assertEquals("voice_engine should match", "azure", map["voice_engine"])
        assertEquals("voice_language should match", "es-ES", map["voice_language"])

        // Verify round-trip
        val reconstructed = ServiceConfiguration.fromMap(map)
        assertTrue("Reconstructed config should be equivalent", config.isEquivalentTo(reconstructed))
    }

    /**
     * Test configuration merge
     */
    @Test
    fun testServiceConfigurationMerge() {
        val config1 = ServiceConfiguration(
            isEnabled = true,
            cursorEnabled = false,
            maxCacheSize = 50
        )

        val config2 = ServiceConfiguration(
            isEnabled = false,
            cursorEnabled = true,
            maxCacheSize = 100
        )

        val merged = config1.mergeWith(config2)

        // Boolean flags should prefer enabled state
        assertTrue("Merged isEnabled should be true", merged.isEnabled)
        assertTrue("Merged cursorEnabled should be true", merged.cursorEnabled)

        // Numeric values should use max
        assertEquals("Merged maxCacheSize should be max", 100, merged.maxCacheSize)
    }

    // ============================================================================
    // SECTION 3: Accessibility Event Tests
    // ============================================================================

    /**
     * Test accessibility event type constants
     */
    @Test
    fun testAccessibilityEventTypes() {
        // Verify supported event types
        val eventTypes = listOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED to "TYPE_VIEW_CLICKED",
            AccessibilityEvent.TYPE_VIEW_FOCUSED to "TYPE_VIEW_FOCUSED",
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED to "TYPE_VIEW_TEXT_CHANGED",
            AccessibilityEvent.TYPE_VIEW_SCROLLED to "TYPE_VIEW_SCROLLED",
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED to "TYPE_WINDOW_STATE_CHANGED",
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED to "TYPE_WINDOW_CONTENT_CHANGED"
        )

        eventTypes.forEach { (eventType, name) ->
            val event = AccessibilityEvent.obtain(eventType)
            try {
                assertEquals("Event type should match $name", eventType, event.eventType)
            } finally {
                @Suppress("DEPRECATION")
                event.recycle()
            }
        }
    }

    /**
     * Test accessibility event creation and properties
     */
    @Test
    fun testAccessibilityEventCreation() {
        val packageName = "com.test.app"
        val className = "com.test.app.MainActivity"
        val eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

        val event = AccessibilityEvent.obtain(eventType).apply {
            this.packageName = packageName
            this.className = className
            this.eventTime = System.currentTimeMillis()
        }

        try {
            assertEquals("Package name should match", packageName, event.packageName)
            assertEquals("Class name should match", className, event.className)
            assertEquals("Event type should match", eventType, event.eventType)
            assertTrue("Event time should be set", event.eventTime > 0)
        } finally {
            @Suppress("DEPRECATION")
            event.recycle()
        }
    }

    /**
     * Test mock accessibility event processing
     */
    @Test
    fun testMockAccessibilityEventProcessing() {
        val processedEvents = AtomicInteger(0)

        val eventTypes = listOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_CLICKED
        )

        eventTypes.forEach { eventType ->
            val event = AccessibilityEvent.obtain(eventType).apply {
                this.packageName = "com.test.app"
                this.className = "com.test.app.MainActivity"
            }

            try {
                // Simulate event processing
                mockService?.onAccessibilityEvent(event)
                processedEvents.incrementAndGet()
            } finally {
                @Suppress("DEPRECATION")
                event.recycle()
            }
        }

        // Verify all events were processed
        assertEquals("All events should be processed", eventTypes.size, processedEvents.get())

        // Verify mock service tracked events
        val trackedEvents = mockService?.getAccessibilityEvents() ?: emptyList()
        assertEquals("Mock service should track events", eventTypes.size, trackedEvents.size)
    }

    // ============================================================================
    // SECTION 4: AccessibilityNodeInfo Parsing Tests
    // ============================================================================

    /**
     * Test AccessibilityNodeInfo bounds extraction
     */
    @Test
    fun testAccessibilityNodeInfoBoundsExtraction() {
        // Test bounds rectangle creation and operations
        val bounds = Rect(100, 200, 300, 400)

        assertEquals("Left should match", 100, bounds.left)
        assertEquals("Top should match", 200, bounds.top)
        assertEquals("Right should match", 300, bounds.right)
        assertEquals("Bottom should match", 400, bounds.bottom)
        assertEquals("Width should be calculated", 200, bounds.width())
        assertEquals("Height should be calculated", 200, bounds.height())

        val centerX = (bounds.left + bounds.right) / 2
        val centerY = (bounds.top + bounds.bottom) / 2
        assertEquals("Center X should be calculated", 200, centerX)
        assertEquals("Center Y should be calculated", 300, centerY)
    }

    /**
     * Test element visibility check logic
     */
    @Test
    fun testElementVisibilityCheck() {
        val screenWidth = 1080
        val screenHeight = 1920

        val testCases = listOf(
            // bounds, isVisible
            Rect(0, 0, 100, 100) to true,
            Rect(-100, -100, 0, 0) to false,  // Fully off-screen
            Rect(screenWidth, screenHeight, screenWidth + 100, screenHeight + 100) to false,  // Fully off-screen
            Rect(-50, -50, 50, 50) to true,  // Partially visible
            Rect(500, 500, 600, 600) to true  // Fully on-screen
        )

        testCases.forEach { (bounds, expectedVisible) ->
            val isVisible = isElementVisible(bounds, screenWidth, screenHeight)
            assertEquals(
                "Visibility for bounds $bounds should be $expectedVisible",
                expectedVisible,
                isVisible
            )
        }
    }

    private fun isElementVisible(bounds: Rect, screenWidth: Int, screenHeight: Int): Boolean {
        // Element is visible if any part is on screen
        return bounds.right > 0 && bounds.bottom > 0 &&
               bounds.left < screenWidth && bounds.top < screenHeight
    }

    /**
     * Test element text normalization
     */
    @Test
    fun testElementTextNormalization() {
        val testCases = listOf(
            "  Hello World  " to "hello world",
            "UPPERCASE" to "uppercase",
            "Mixed Case Text" to "mixed case text",
            "  Multiple   Spaces  " to "multiple spaces",
            "Special@#\$Characters" to "specialcharacters",
            "Numbers123" to "numbers123",
            "New\nLine" to "new line",
            "Tab\tSeparated" to "tab separated"
        )

        testCases.forEach { (input, expected) ->
            val normalized = normalizeElementText(input)
            assertEquals("Normalized '$input' should be '$expected'", expected, normalized)
        }
    }

    private fun normalizeElementText(text: String): String {
        return text
            .trim()
            .replace(Regex("[\n\t]"), " ")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .lowercase()
    }

    /**
     * Test element action detection
     */
    @Test
    fun testElementActionDetection() {
        val clickableClasses = setOf(
            "android.widget.Button",
            "android.widget.ImageButton",
            "android.widget.CheckBox",
            "android.widget.Switch",
            "android.widget.RadioButton"
        )

        val nonClickableClasses = setOf(
            "android.widget.TextView",
            "android.widget.ImageView",
            "android.widget.LinearLayout",
            "android.widget.FrameLayout"
        )

        clickableClasses.forEach { className ->
            assertTrue(
                "$className should be detected as typically clickable",
                isTypicallyClickableClass(className)
            )
        }

        nonClickableClasses.forEach { className ->
            assertFalse(
                "$className should not be detected as typically clickable",
                isTypicallyClickableClass(className)
            )
        }
    }

    private fun isTypicallyClickableClass(className: String): Boolean {
        val clickableClasses = setOf(
            "android.widget.Button",
            "android.widget.ImageButton",
            "android.widget.CheckBox",
            "android.widget.Switch",
            "android.widget.RadioButton",
            "android.widget.ToggleButton",
            "android.widget.CompoundButton"
        )
        return clickableClasses.any { className.contains(it) }
    }

    /**
     * Test duplicate element detection
     */
    @Test
    fun testDuplicateElementDetection() {
        val epsilon = 8 // pixels for approximate equality

        val testCases = listOf(
            // rect1, rect2, isDuplicate
            Triple(Rect(100, 100, 200, 200), Rect(100, 100, 200, 200), true),  // Exact match
            Triple(Rect(100, 100, 200, 200), Rect(105, 100, 205, 200), true),  // Within epsilon
            Triple(Rect(100, 100, 200, 200), Rect(120, 100, 220, 200), false), // Outside epsilon
            Triple(Rect(0, 0, 100, 100), Rect(500, 500, 600, 600), false)      // Far apart
        )

        testCases.forEach { (rect1, rect2, expectedDuplicate) ->
            val isDuplicate = areRectsSimilar(rect1, rect2, epsilon)
            assertEquals(
                "Rects $rect1 and $rect2 duplicate check should be $expectedDuplicate",
                expectedDuplicate,
                isDuplicate
            )
        }
    }

    private fun areRectsSimilar(rect1: Rect, rect2: Rect, epsilon: Int): Boolean {
        return kotlin.math.abs(rect1.left - rect2.left) <= epsilon &&
               kotlin.math.abs(rect1.top - rect2.top) <= epsilon &&
               kotlin.math.abs(rect1.right - rect2.right) <= epsilon &&
               kotlin.math.abs(rect1.bottom - rect2.bottom) <= epsilon
    }

    // ============================================================================
    // SECTION 5: Service State Tests
    // ============================================================================

    /**
     * Test mock service state tracking
     */
    @Test
    fun testMockServiceStateTracking() {
        val service = MockVoiceAccessibilityService()

        // Initial state
        assertEquals("Initial gesture count should be 0", 0, service.getGestureCount())
        assertEquals("Initial action count should be 0", 0, service.getActionCount())
        assertTrue("Performed gestures should be empty", service.getPerformedGestures().isEmpty())
        assertTrue("Performed actions should be empty", service.getPerformedActions().isEmpty())

        // Perform actions
        service.mockPerformGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        service.mockPerformGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)

        assertEquals("Action count should be 2", 2, service.getActionCount())
        assertEquals("Performed actions should have 2 entries", 2, service.getPerformedActions().size)

        // Verify action tracking
        assertTrue(
            "Should have performed back action",
            service.hasPerformedAction("GLOBAL_ACTION_BACK")
        )
        assertTrue(
            "Should have performed home action",
            service.hasPerformedAction("GLOBAL_ACTION_HOME")
        )

        // Reset and verify
        service.reset()
        assertEquals("Gesture count should be 0 after reset", 0, service.getGestureCount())
        assertEquals("Action count should be 0 after reset", 0, service.getActionCount())
    }

    /**
     * Test mock gesture creation and tracking
     */
    @Test
    fun testMockGestureCreationAndTracking() {
        val service = MockVoiceAccessibilityService()

        // Create and dispatch tap gesture
        val tapGesture = service.createTapGesture(500f, 500f, 100)
        service.mockDispatchGesture(tapGesture, null, null)

        assertEquals("Gesture count should be 1", 1, service.getGestureCount())

        val gestures = service.getPerformedGestures()
        assertEquals("Should have 1 performed gesture", 1, gestures.size)

        val performedGesture = gestures.first()
        assertEquals("Gesture should be TAP type", "TAP", performedGesture.gestureType)
        assertEquals("Gesture duration should be 100", 100, performedGesture.duration)

        // Create and dispatch swipe gesture
        val swipeGesture = service.createSwipeGesture(100f, 500f, 900f, 500f, 300)
        service.mockDispatchGesture(swipeGesture, null, null)

        assertEquals("Gesture count should be 2", 2, service.getGestureCount())

        val swipeGestures = service.getPerformedGestures().filter { it.gestureType == "SWIPE_HORIZONTAL" }
        assertEquals("Should have 1 horizontal swipe", 1, swipeGestures.size)
    }

    /**
     * Test service debug info generation
     */
    @Test
    fun testServiceDebugInfo() {
        val service = MockVoiceAccessibilityService()

        // Perform some actions
        service.mockPerformGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        val tapGesture = service.createTapGesture(500f, 500f)
        service.mockDispatchGesture(tapGesture, null, null)

        val debugInfo = service.getDebugInfo()

        assertTrue("Debug info should contain service status", debugInfo.contains("Service Running"))
        assertTrue("Debug info should contain gestures count", debugInfo.contains("Gestures Performed"))
        assertTrue("Debug info should contain actions count", debugInfo.contains("Actions Performed"))

        println("Debug info:\n$debugInfo")
    }

    // ============================================================================
    // SECTION 6: Service Info Configuration Tests
    // ============================================================================

    /**
     * Test AccessibilityServiceInfo configuration constants
     */
    @Test
    fun testAccessibilityServiceInfoConfiguration() {
        // Test feedback types
        val feedbackTypes = listOf(
            AccessibilityServiceInfo.FEEDBACK_SPOKEN to "FEEDBACK_SPOKEN",
            AccessibilityServiceInfo.FEEDBACK_HAPTIC to "FEEDBACK_HAPTIC",
            AccessibilityServiceInfo.FEEDBACK_AUDIBLE to "FEEDBACK_AUDIBLE",
            AccessibilityServiceInfo.FEEDBACK_VISUAL to "FEEDBACK_VISUAL",
            AccessibilityServiceInfo.FEEDBACK_GENERIC to "FEEDBACK_GENERIC",
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK to "FEEDBACK_ALL_MASK"
        )

        feedbackTypes.forEach { (type, name) ->
            assertTrue("$name should be a positive value", type >= 0)
        }

        // Test capability flags
        val capabilities = listOf(
            AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT to "CAN_RETRIEVE_WINDOW_CONTENT",
            AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION to "CAN_REQUEST_TOUCH_EXPLORATION",
            AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES to "CAN_PERFORM_GESTURES"
        )

        capabilities.forEach { (capability, name) ->
            assertTrue("$name should be a positive value", capability > 0)
        }
    }

    /**
     * Test event type mask calculation
     */
    @Test
    fun testEventTypeMaskCalculation() {
        val eventTypes = listOf(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )

        var eventMask = 0
        eventTypes.forEach { eventType ->
            eventMask = eventMask or eventType
        }

        // Verify each type is in the mask
        eventTypes.forEach { eventType ->
            assertTrue(
                "Event type $eventType should be in mask",
                (eventMask and eventType) != 0
            )
        }

        // Verify an unincluded type is not in the mask
        assertFalse(
            "TYPE_VIEW_SCROLLED should not be in mask",
            (eventMask and AccessibilityEvent.TYPE_VIEW_SCROLLED) != 0
        )
    }
}
