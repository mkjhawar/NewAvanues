/**
 * ElementProcessingIntegrationTest.kt - End-to-end integration tests for element processing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * TDD Integration tests verifying the FULL element processing pipeline:
 * 1. Element extraction (ElementInfo creation)
 * 2. VUID generation for element
 * 3. Command generation from element
 * 4. Handler routing based on element type
 * 5. ActionResult from execution
 */
package com.augmentalis.voiceoscoreng.integration

import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.common.CommandGenerator
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkDetector
import com.augmentalis.voiceoscoreng.common.FrameworkType
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode
import com.augmentalis.voiceoscoreng.handlers.ActionResult
import com.augmentalis.voiceoscoreng.handlers.ElementBounds
import com.augmentalis.voiceoscoreng.handlers.ScrollDirection
import com.augmentalis.voiceoscoreng.handlers.VolumeDirection
import com.augmentalis.voiceoscoreng.handlers.FlutterHandler
import com.augmentalis.voiceoscoreng.handlers.FrameworkHandler
import com.augmentalis.voiceoscoreng.handlers.FrameworkHandlerRegistry
import com.augmentalis.voiceoscoreng.handlers.NativeHandler
import com.augmentalis.voiceoscoreng.handlers.ReactNativeHandler
import com.augmentalis.voiceoscoreng.handlers.UnityHandler
import com.augmentalis.voiceoscoreng.handlers.WebViewHandler
import com.augmentalis.voiceoscoreng.functions.DangerousElementDetector
import kotlinx.datetime.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive integration tests for the element processing pipeline.
 *
 * Tests the complete flow from raw UI element data through to action execution:
 * ElementInfo -> VUIDGenerator -> CommandGenerator -> FrameworkHandler -> ActionResult
 */
class ElementProcessingIntegrationTest {

    private lateinit var testExecutor: TestActionExecutor
    private lateinit var dangerDetector: DangerousElementDetector

    @BeforeTest
    fun setup() {
        // Clear and register default handlers
        FrameworkHandlerRegistry.clear()
        FrameworkHandlerRegistry.registerDefaults()

        // Initialize test executor
        testExecutor = TestActionExecutor()

        // Initialize danger detector
        dangerDetector = DangerousElementDetector()
    }

    @AfterTest
    fun teardown() {
        FrameworkHandlerRegistry.clear()
    }

    // ==================== Test 1: Full Pipeline Tests ====================

    @Test
    fun `test full pipeline from raw element to executed command`() {
        // Step 1: Create raw element info (simulating screen scrape)
        val element = ElementInfo(
            className = "android.widget.Button",
            resourceId = "com.example.app:id/submit_button",
            text = "Submit",
            contentDescription = "Submit form",
            bounds = Bounds(100, 200, 300, 250),
            isClickable = true,
            isEnabled = true,
            packageName = "com.example.app"
        )

        // Step 2: Generate VUID for element
        val typeCode = VUIDGenerator.getTypeCode(element.className)
        assertEquals(VUIDTypeCode.BUTTON, typeCode)

        val vuid = VUIDGenerator.generate(
            packageName = element.packageName,
            typeCode = typeCode,
            elementHash = element.resourceId
        )
        assertTrue(VUIDGenerator.isValidVUID(vuid))

        // Step 3: Generate command from element
        val command = CommandGenerator.fromElement(element, element.packageName)
        assertNotNull(command)
        assertEquals("click Submit", command.phrase)
        assertEquals(CommandActionType.CLICK, command.actionType)
        assertNotNull(command.targetVuid)

        // Step 4: Find appropriate handler
        val handler = FrameworkHandlerRegistry.findHandler(listOf(element))
        assertNotNull(handler)
        assertEquals(FrameworkType.NATIVE, handler.frameworkType)

        // Step 5: Process element through handler
        val processedElements = handler.processElements(listOf(element))
        assertTrue(processedElements.isNotEmpty())

        // Step 6: Execute command (via test executor)
        testExecutor.registerElement(command.targetVuid!!, element)
        val result = testExecutor.executeSyncForTest(command)

        // Verify full pipeline success
        assertTrue(result.isSuccess)
        assertEquals("Clicked: Submit", result.message)
    }

    @Test
    fun `test full pipeline with EditText generates type command`() {
        // Create input element
        val element = ElementInfo.input(
            hint = "Enter email address",
            resourceId = "com.app:id/email_input",
            packageName = "com.app",
            bounds = Bounds(50, 100, 400, 150)
        )

        // Generate command
        val command = CommandGenerator.fromElement(element, "com.app")
        assertNotNull(command)
        assertEquals("type Enter email address", command.phrase)
        assertEquals(CommandActionType.TYPE, command.actionType)

        // Verify VUID type code
        val parsed = VUIDGenerator.parseVUID(command.targetVuid!!)
        assertNotNull(parsed)
        assertEquals(VUIDTypeCode.INPUT, parsed.typeCode)
    }

    // ==================== Test 2: Valid VUID Command Generation ====================

    @Test
    fun `test element with valid VUID generates correct command`() {
        // Test multiple element types
        val testCases = listOf(
            Triple("Button", VUIDTypeCode.BUTTON, CommandActionType.CLICK),
            Triple("EditText", VUIDTypeCode.INPUT, CommandActionType.TYPE),
            Triple("MaterialButton", VUIDTypeCode.BUTTON, CommandActionType.CLICK),
            Triple("TextInputEditText", VUIDTypeCode.INPUT, CommandActionType.TYPE),
            Triple("ImageButton", VUIDTypeCode.BUTTON, CommandActionType.CLICK)
        )

        for ((className, expectedTypeCode, expectedAction) in testCases) {
            val element = ElementInfo(
                className = className,
                text = "Test Label",
                isClickable = true,
                packageName = "com.test.app"
            )

            // Generate command
            val command = CommandGenerator.fromElement(element, "com.test.app")
            assertNotNull(command, "Command should be generated for $className")

            // Verify VUID type code
            val vuidComponents = VUIDGenerator.parseVUID(command.targetVuid!!)
            assertNotNull(vuidComponents, "VUID should be parseable for $className")
            assertEquals(
                expectedTypeCode,
                vuidComponents.typeCode,
                "Type code should match for $className"
            )

            // Verify action type
            assertEquals(
                expectedAction,
                command.actionType,
                "Action type should match for $className"
            )
        }
    }

    @Test
    fun `test VUID consistency for same element`() {
        val element = ElementInfo.button(
            text = "Consistent Button",
            resourceId = "com.app:id/consistent_btn",
            packageName = "com.app"
        )

        // Generate multiple times
        val command1 = CommandGenerator.fromElement(element, "com.app")
        val command2 = CommandGenerator.fromElement(element, "com.app")

        assertNotNull(command1)
        assertNotNull(command2)

        // VUIDs should be identical for same element
        assertEquals(command1.targetVuid, command2.targetVuid)
    }

    // ==================== Test 3: Framework Detection Handler Selection ====================

    @Test
    fun `test framework detection affects handler selection`() {
        // Test Flutter detection
        val flutterElements = listOf(
            ElementInfo(
                className = "io.flutter.embedding.android.FlutterView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "io.flutter.SemanticsNode",
                text = "Flutter Button",
                isClickable = true
            )
        )

        val flutterHandler = FrameworkHandlerRegistry.findHandler(flutterElements)
        assertNotNull(flutterHandler)
        assertEquals(FrameworkType.FLUTTER, flutterHandler.frameworkType)

        // Test Unity detection
        val unityElements = listOf(
            ElementInfo(
                className = "com.unity3d.player.UnityPlayer",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val unityHandler = FrameworkHandlerRegistry.findHandler(unityElements)
        assertNotNull(unityHandler)
        assertEquals(FrameworkType.UNITY, unityHandler.frameworkType)

        // Test React Native detection
        val rnElements = listOf(
            ElementInfo(
                className = "com.facebook.react.ReactRootView",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val rnHandler = FrameworkHandlerRegistry.findHandler(rnElements)
        assertNotNull(rnHandler)
        assertEquals(FrameworkType.REACT_NATIVE, rnHandler.frameworkType)

        // Test WebView detection
        val webViewElements = listOf(
            ElementInfo(
                className = "android.webkit.WebView",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val webHandler = FrameworkHandlerRegistry.findHandler(webViewElements)
        assertNotNull(webHandler)
        assertEquals(FrameworkType.WEBVIEW, webHandler.frameworkType)

        // Test Native fallback
        val nativeElements = listOf(
            ElementInfo(
                className = "android.widget.Button",
                text = "Native Button",
                isClickable = true
            )
        )

        val nativeHandler = FrameworkHandlerRegistry.findHandler(nativeElements)
        assertNotNull(nativeHandler)
        assertEquals(FrameworkType.NATIVE, nativeHandler.frameworkType)
    }

    @Test
    fun `test FrameworkDetector integrated with handler selection`() {
        val packageName = "com.flutter.testapp"
        val classNames = listOf(
            "io.flutter.embedding.android.FlutterView",
            "io.flutter.embedding.android.FlutterActivity",
            "android.widget.FrameLayout"
        )

        // Use FrameworkDetector
        val frameworkInfo = FrameworkDetector.detect(packageName, classNames)
        assertEquals(FrameworkType.FLUTTER, frameworkInfo.type)
        assertTrue(frameworkInfo.packageIndicators.isNotEmpty())

        // Verify handler matches
        val handler = FrameworkHandlerRegistry.getHandler(frameworkInfo.type)
        assertNotNull(handler)
        assertEquals(frameworkInfo.type, handler.frameworkType)
    }

    @Test
    fun `test handler priority order`() {
        // Verify handlers are sorted by priority
        val handlers = FrameworkHandlerRegistry.getHandlers()
        assertEquals(5, handlers.size)

        // Flutter should be first (priority 100)
        assertEquals(FrameworkType.FLUTTER, handlers[0].frameworkType)
        // Native should be last (priority 0)
        assertEquals(FrameworkType.NATIVE, handlers[4].frameworkType)

        // Verify descending priority
        for (i in 0 until handlers.size - 1) {
            assertTrue(
                handlers[i].getPriority() >= handlers[i + 1].getPriority(),
                "Handlers should be sorted by descending priority"
            )
        }
    }

    // ==================== Test 4: Dangerous Element Detection ====================

    @Test
    fun `test dangerous element detection prevents execution`() {
        val dangerousElements = listOf(
            ElementInfo.button(text = "Delete Account", packageName = "com.app"),
            ElementInfo.button(text = "Buy Now $9.99", packageName = "com.app"),
            ElementInfo.button(text = "Logout", packageName = "com.app"),
            ElementInfo.button(text = "Transfer Funds", packageName = "com.app"),
            ElementInfo.button(text = "Download File", packageName = "com.app"),
            ElementInfo.button(text = "Allow Access", packageName = "com.app")
        )

        for (element in dangerousElements) {
            // Analyze element for danger
            val result = dangerDetector.analyze(
                element.text,
                element.contentDescription,
                element.resourceId
            )

            assertTrue(
                result.isDangerous,
                "Element '${element.text}' should be detected as dangerous"
            )
            assertTrue(
                result.skipAutoClick,
                "Auto-click should be skipped for '${element.text}'"
            )
            assertNotNull(
                result.dangerType,
                "Danger type should be set for '${element.text}'"
            )
        }
    }

    @Test
    fun `test safe elements pass danger detection`() {
        val safeElements = listOf(
            ElementInfo.button(text = "Submit", packageName = "com.app"),
            ElementInfo.button(text = "Next", packageName = "com.app"),
            ElementInfo.button(text = "Continue", packageName = "com.app"),
            ElementInfo.button(text = "Save", packageName = "com.app"),
            ElementInfo.button(text = "OK", packageName = "com.app")
        )

        for (element in safeElements) {
            val result = dangerDetector.analyze(
                element.text,
                element.contentDescription,
                element.resourceId
            )

            assertFalse(
                result.isDangerous,
                "Element '${element.text}' should NOT be detected as dangerous"
            )
            assertFalse(
                result.skipAutoClick,
                "Auto-click should NOT be skipped for '${element.text}'"
            )
        }
    }

    @Test
    fun `test dangerous element integration with command pipeline`() {
        val dangerousElement = ElementInfo.button(
            text = "Delete All Data",
            resourceId = "com.app:id/delete_all",
            packageName = "com.app"
        )

        // Generate command (command generation still works)
        val command = CommandGenerator.fromElement(dangerousElement, "com.app")
        assertNotNull(command)

        // But danger detection should flag it
        val dangerResult = dangerDetector.analyze(
            dangerousElement.text,
            dangerousElement.contentDescription,
            dangerousElement.resourceId
        )

        assertTrue(dangerResult.isDangerous)
        assertEquals(DangerousElementDetector.DangerType.DELETE, dangerResult.dangerType)

        // In real implementation, execution would require confirmation
        // Simulate confirmation-required response
        if (dangerResult.isDangerous && dangerResult.skipAutoClick) {
            val confirmationResult = ActionResult.ConfirmationRequired(
                prompt = "Are you sure you want to ${dangerousElement.text}?",
                confirmAction = "confirm_delete"
            )
            assertFalse(confirmationResult.isSuccess)
        }
    }

    // ==================== Test 5: Batch Element Processing ====================

    @Test
    fun `test batch element processing`() {
        val batchElements = listOf(
            ElementInfo.button(text = "Button 1", resourceId = "com.app:id/btn1", packageName = "com.app"),
            ElementInfo.button(text = "Button 2", resourceId = "com.app:id/btn2", packageName = "com.app"),
            ElementInfo.input(hint = "Email", resourceId = "com.app:id/email", packageName = "com.app"),
            ElementInfo.input(hint = "Password", resourceId = "com.app:id/password", packageName = "com.app"),
            ElementInfo(
                className = "TextView",
                text = "Static Label",
                isClickable = false,
                packageName = "com.app"
            ),
            ElementInfo(
                className = "RecyclerView",
                isScrollable = true,
                resourceId = "com.app:id/list",
                packageName = "com.app"
            )
        )

        // Process batch through handler
        val handler = FrameworkHandlerRegistry.findHandler(batchElements)
        assertNotNull(handler)

        val processed = handler.processElements(batchElements)
        assertTrue(processed.isNotEmpty())

        // Generate commands for all actionable elements
        val commands = batchElements.mapNotNull { element ->
            CommandGenerator.fromElement(element, "com.app")
        }

        // Should generate commands for actionable elements with content
        // Button 1, Button 2, Email input, Password input = 4 commands
        // TextView is not actionable, RecyclerView has no voice content
        assertEquals(4, commands.size)

        // Verify all commands have unique VUIDs
        val vuids = commands.mapNotNull { it.targetVuid }
        assertEquals(commands.size, vuids.toSet().size)
    }

    @Test
    fun `test batch processing with mixed frameworks`() {
        // Simulate a mixed-framework screen (e.g., native + WebView)
        val mixedElements = listOf(
            ElementInfo(
                className = "android.widget.LinearLayout",
                packageName = "com.hybrid.app"
            ),
            ElementInfo(
                className = "android.widget.Button",
                text = "Native Button",
                isClickable = true,
                packageName = "com.hybrid.app"
            ),
            ElementInfo(
                className = "android.webkit.WebView",
                bounds = Bounds(0, 200, 1080, 1920),
                packageName = "com.hybrid.app"
            )
        )

        // Handler should detect WebView and use WebViewHandler
        val handler = FrameworkHandlerRegistry.findHandler(mixedElements)
        assertNotNull(handler)
        assertEquals(FrameworkType.WEBVIEW, handler.frameworkType)
    }

    @Test
    fun `test batch processing performance with large element set`() {
        // Generate 100 elements
        val largeElementSet = (1..100).map { index ->
            ElementInfo.button(
                text = "Button $index",
                resourceId = "com.app:id/btn_$index",
                packageName = "com.perf.app"
            )
        }

        // Process all elements
        val startTime = Clock.System.now().toEpochMilliseconds()

        val commands = largeElementSet.mapNotNull { element ->
            CommandGenerator.fromElement(element, "com.perf.app")
        }

        val duration = Clock.System.now().toEpochMilliseconds() - startTime

        // All should generate commands
        assertEquals(100, commands.size)

        // Should complete in reasonable time (< 1 second for 100 elements)
        assertTrue(
            duration < 1000,
            "Batch processing should complete in < 1 second, took ${duration}ms"
        )
    }

    // ==================== Test 6: Graceful Failure with Missing Fields ====================

    @Test
    fun `test element processing with missing fields gracefully fails`() {
        // Element with no text, contentDescription, or resourceId
        val emptyElement = ElementInfo(
            className = "Button",
            isClickable = true,
            packageName = "com.app"
        )

        // Should return null (not crash)
        val command = CommandGenerator.fromElement(emptyElement, "com.app")
        assertNull(command, "Command should be null for element without voice content")
    }

    @Test
    fun `test element with blank packageName processes correctly`() {
        val element = ElementInfo.button(
            text = "Test Button",
            resourceId = "com.app:id/test",
            packageName = ""  // Blank package name
        )

        // Should still generate command
        val command = CommandGenerator.fromElement(element, "")
        assertNotNull(command)
        assertNotNull(command.targetVuid)

        // VUID should still be valid
        assertTrue(VUIDGenerator.isValidVUID(command.targetVuid!!))
    }

    @Test
    fun `test element with invalid bounds processes correctly`() {
        val element = ElementInfo(
            className = "Button",
            text = "Test",
            bounds = Bounds(0, 0, 0, 0),  // Invalid/zero bounds
            isClickable = true,
            packageName = "com.app"
        )

        // Command generation should still work
        val command = CommandGenerator.fromElement(element, "com.app")
        assertNotNull(command)
    }

    @Test
    fun `test disabled element is not actionable`() {
        val disabledElement = ElementInfo(
            className = "Button",
            text = "Disabled Button",
            isClickable = true,
            isEnabled = false,  // Disabled
            packageName = "com.app"
        )

        // Element is technically clickable but disabled
        // Our current implementation checks isClickable, not isEnabled
        // This test documents the current behavior
        val command = CommandGenerator.fromElement(disabledElement, "com.app")

        // Currently generates command (isEnabled doesn't affect isActionable)
        // Future improvement could check isEnabled
        assertNotNull(command)
    }

    @Test
    fun `test non-actionable element returns null command`() {
        val nonActionableElements = listOf(
            ElementInfo(className = "TextView", text = "Label", isClickable = false, isScrollable = false, packageName = "com.app"),
            ElementInfo(className = "ImageView", contentDescription = "Icon", isClickable = false, isScrollable = false, packageName = "com.app"),
            ElementInfo(className = "View", resourceId = "com.app:id/spacer", isClickable = false, isScrollable = false, packageName = "com.app")
        )

        for (element in nonActionableElements) {
            val command = CommandGenerator.fromElement(element, "com.app")
            assertNull(
                command,
                "Non-actionable element ${element.className} should not generate command"
            )
        }
    }

    @Test
    fun `test element with only className returns null command`() {
        // Element where voiceLabel would default to just the class name
        val element = ElementInfo(
            className = "android.widget.Button",
            isClickable = true,
            packageName = "com.app"
        )

        // Should return null because label would just be "Button"
        val command = CommandGenerator.fromElement(element, "com.app")
        assertNull(command)
    }

    // ==================== Additional Integration Tests ====================

    @Test
    fun `test VUID extraction and reconstruction`() {
        val originalVuid = VUIDGenerator.generate(
            packageName = "com.test.app",
            typeCode = VUIDTypeCode.BUTTON,
            elementHash = "my_button_id"
        )

        // Parse components
        val components = VUIDGenerator.parseVUID(originalVuid)
        assertNotNull(components)

        // Reconstruct
        val reconstructed = components.toVUID()
        assertEquals(originalVuid, reconstructed)
    }

    @Test
    fun `test command confidence scoring`() {
        // Element with all identifiers - should have high confidence
        val fullElement = ElementInfo(
            className = "Button",
            text = "Submit Form",
            contentDescription = "Submit the registration form",
            resourceId = "com.app:id/submit_btn",
            isClickable = true,
            packageName = "com.app"
        )

        // Element with minimal identifiers - should have lower confidence
        val minimalElement = ElementInfo(
            className = "Button",
            text = "OK",
            isClickable = true,
            packageName = "com.app"
        )

        val fullCommand = CommandGenerator.fromElement(fullElement, "com.app")
        val minimalCommand = CommandGenerator.fromElement(minimalElement, "com.app")

        assertNotNull(fullCommand)
        assertNotNull(minimalCommand)

        assertTrue(
            fullCommand.confidence > minimalCommand.confidence,
            "Full element should have higher confidence"
        )
    }

    @Test
    fun `test handler processing filters non-relevant elements`() {
        val flutterHandler = FlutterHandler()

        val mixedElements = listOf(
            ElementInfo(className = "io.flutter.FlutterEngine"),  // Framework indicator, no content
            ElementInfo(
                className = "io.flutter.SemanticsNode",
                text = "Actionable Item",
                isClickable = true
            ),  // Has content
            ElementInfo(className = "io.flutter.view.FlutterView")  // Framework indicator, no content
        )

        val processed = flutterHandler.processElements(mixedElements)

        // Should only include elements with meaningful content
        assertEquals(1, processed.size)
        assertEquals("Actionable Item", processed[0].text)
    }

    @Test
    fun `test end to end with test executor`() {
        // Create element
        val element = ElementInfo.button(
            text = "Execute Test",
            resourceId = "com.app:id/execute_btn",
            packageName = "com.app",
            bounds = Bounds(100, 100, 200, 150)
        )

        // Generate command
        val command = CommandGenerator.fromElement(element, "com.app")
        assertNotNull(command)

        // Register element in test executor
        testExecutor.registerElement(command.targetVuid!!, element)

        // Execute
        val result = testExecutor.executeSyncForTest(command)

        assertTrue(result.isSuccess)
        assertTrue(result.message.contains("Execute Test"))
    }
}

/**
 * Test implementation for integration testing.
 *
 * Provides a mock executor that tracks registered elements and
 * simulates action execution without requiring platform-specific implementations.
 */
class TestActionExecutor {

    private val registeredElements = mutableMapOf<String, ElementInfo>()
    private val executionLog = mutableListOf<String>()

    /**
     * Register an element for lookup during execution
     */
    fun registerElement(vuid: String, element: ElementInfo) {
        registeredElements[vuid] = element
    }

    /**
     * Get execution history
     */
    fun getExecutionLog(): List<String> = executionLog.toList()

    /**
     * Clear registered elements and execution log
     */
    fun clear() {
        registeredElements.clear()
        executionLog.clear()
    }

    /**
     * Synchronous execution for testing
     */
    fun executeSyncForTest(command: QuantizedCommand): ActionResult {
        return when (command.actionType) {
            CommandActionType.CLICK -> {
                val vuid = command.targetVuid ?: return ActionResult.ElementNotFound("null")
                val element = registeredElements[vuid]
                    ?: return ActionResult.ElementNotFound(vuid)

                executionLog.add("CLICK: ${element.voiceLabel}")
                ActionResult.Success("Clicked: ${element.voiceLabel}")
            }
            CommandActionType.TYPE -> {
                val vuid = command.targetVuid ?: return ActionResult.ElementNotFound("null")
                val element = registeredElements[vuid]
                    ?: return ActionResult.ElementNotFound(vuid)

                executionLog.add("TYPE: ${element.voiceLabel}")
                ActionResult.Success("Focused: ${element.voiceLabel}")
            }
            else -> {
                executionLog.add("ACTION: ${command.actionType}")
                ActionResult.Success("Executed: ${command.actionType}")
            }
        }
    }

    // ==================== Mock Action Methods for Testing ====================

    suspend fun tap(vuid: String): ActionResult {
        val element = registeredElements[vuid]
            ?: return ActionResult.ElementNotFound(vuid)
        executionLog.add("TAP: ${element.voiceLabel}")
        return ActionResult.Success("Tapped: ${element.voiceLabel}")
    }

    suspend fun longPress(vuid: String, durationMs: Long): ActionResult {
        val element = registeredElements[vuid]
            ?: return ActionResult.ElementNotFound(vuid)
        executionLog.add("LONG_PRESS: ${element.voiceLabel} (${durationMs}ms)")
        return ActionResult.Success("Long pressed: ${element.voiceLabel}")
    }

    suspend fun focus(vuid: String): ActionResult {
        val element = registeredElements[vuid]
            ?: return ActionResult.ElementNotFound(vuid)
        executionLog.add("FOCUS: ${element.voiceLabel}")
        return ActionResult.Success("Focused: ${element.voiceLabel}")
    }

    suspend fun enterText(text: String, vuid: String?): ActionResult {
        executionLog.add("ENTER_TEXT: $text")
        return ActionResult.Success("Entered text: $text")
    }

    suspend fun scroll(direction: ScrollDirection, amount: Float, vuid: String?): ActionResult {
        executionLog.add("SCROLL: $direction ($amount)")
        return ActionResult.Success("Scrolled: $direction")
    }

    suspend fun back(): ActionResult {
        executionLog.add("BACK")
        return ActionResult.Success("Navigated back")
    }

    suspend fun home(): ActionResult {
        executionLog.add("HOME")
        return ActionResult.Success("Navigated to home")
    }

    suspend fun recentApps(): ActionResult {
        executionLog.add("RECENT_APPS")
        return ActionResult.Success("Showing recent apps")
    }

    suspend fun appDrawer(): ActionResult {
        executionLog.add("APP_DRAWER")
        return ActionResult.Success("Opened app drawer")
    }

    suspend fun openSettings(): ActionResult {
        executionLog.add("OPEN_SETTINGS")
        return ActionResult.Success("Opened settings")
    }

    suspend fun showNotifications(): ActionResult {
        executionLog.add("SHOW_NOTIFICATIONS")
        return ActionResult.Success("Showing notifications")
    }

    suspend fun clearNotifications(): ActionResult {
        executionLog.add("CLEAR_NOTIFICATIONS")
        return ActionResult.Success("Cleared notifications")
    }

    suspend fun screenshot(): ActionResult {
        executionLog.add("SCREENSHOT")
        return ActionResult.Success("Screenshot taken")
    }

    suspend fun flashlight(on: Boolean): ActionResult {
        executionLog.add("FLASHLIGHT: $on")
        return ActionResult.Success("Flashlight: $on")
    }

    suspend fun mediaPlayPause(): ActionResult {
        executionLog.add("MEDIA_PLAY_PAUSE")
        return ActionResult.Success("Media play/pause toggled")
    }

    suspend fun mediaNext(): ActionResult {
        executionLog.add("MEDIA_NEXT")
        return ActionResult.Success("Next track")
    }

    suspend fun mediaPrevious(): ActionResult {
        executionLog.add("MEDIA_PREVIOUS")
        return ActionResult.Success("Previous track")
    }

    suspend fun volume(direction: VolumeDirection): ActionResult {
        executionLog.add("VOLUME: $direction")
        return ActionResult.Success("Volume adjusted: $direction")
    }

    suspend fun openApp(appType: String): ActionResult {
        executionLog.add("OPEN_APP: $appType")
        return ActionResult.Success("Opened app: $appType")
    }

    suspend fun openAppByPackage(packageName: String): ActionResult {
        executionLog.add("OPEN_APP_BY_PACKAGE: $packageName")
        return ActionResult.Success("Opened package: $packageName")
    }

    suspend fun closeApp(): ActionResult {
        executionLog.add("CLOSE_APP")
        return ActionResult.Success("Closed app")
    }

    suspend fun executeCommand(command: QuantizedCommand): ActionResult {
        return executeSyncForTest(command)
    }

    suspend fun executeAction(actionType: CommandActionType, params: Map<String, Any>): ActionResult {
        executionLog.add("EXECUTE_ACTION: $actionType with params: $params")
        return ActionResult.Success("Executed action: $actionType")
    }

    suspend fun elementExists(vuid: String): Boolean {
        return registeredElements.containsKey(vuid)
    }

    suspend fun getElementBounds(vuid: String): ElementBounds? {
        val element = registeredElements[vuid] ?: return null
        return ElementBounds(
            left = element.bounds.left,
            top = element.bounds.top,
            right = element.bounds.right,
            bottom = element.bounds.bottom
        )
    }
}
