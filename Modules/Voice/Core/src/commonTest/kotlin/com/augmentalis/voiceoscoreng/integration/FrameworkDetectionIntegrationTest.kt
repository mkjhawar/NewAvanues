package com.augmentalis.voiceoscoreng.integration

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkDetector
import com.augmentalis.voiceoscoreng.common.FrameworkInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType
import com.augmentalis.voiceoscoreng.handlers.FlutterHandler
import com.augmentalis.voiceoscoreng.handlers.FrameworkHandler
import com.augmentalis.voiceoscoreng.handlers.FrameworkHandlerRegistry
import com.augmentalis.voiceoscoreng.handlers.NativeHandler
import com.augmentalis.voiceoscoreng.handlers.ReactNativeHandler
import com.augmentalis.voiceoscoreng.handlers.UnityHandler
import com.augmentalis.voiceoscoreng.handlers.WebViewHandler
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for framework detection and handler routing.
 *
 * These tests verify the end-to-end flow from detecting a framework
 * based on element characteristics to routing to the correct handler.
 *
 * TDD Approach: Tests written first to define expected behavior.
 */
class FrameworkDetectionIntegrationTest {

    // Real handlers - no mocks
    private lateinit var flutterHandler: FlutterHandler
    private lateinit var unityHandler: UnityHandler
    private lateinit var reactNativeHandler: ReactNativeHandler
    private lateinit var webViewHandler: WebViewHandler
    private lateinit var nativeHandler: NativeHandler

    @BeforeTest
    fun setup() {
        // Initialize real handlers
        flutterHandler = FlutterHandler()
        unityHandler = UnityHandler()
        reactNativeHandler = ReactNativeHandler()
        webViewHandler = WebViewHandler()
        nativeHandler = NativeHandler()

        // Register all handlers
        FrameworkHandlerRegistry.registerDefaults()
    }

    @AfterTest
    fun teardown() {
        FrameworkHandlerRegistry.clear()
    }

    // ================================================================
    // Test 1: Flutter Detection from Semantics Node Patterns
    // ================================================================

    @Test
    fun `test flutter detection from semantics node patterns`() {
        // Arrange: Create elements that represent a typical Flutter app screen
        val elements = createFlutterAppElements()

        // Act: Detect framework using FrameworkDetector
        val classNames = elements.map { it.className }
        val frameworkInfo = FrameworkDetector.detect("com.example.flutterapp", classNames)

        // Assert: Framework should be detected as Flutter
        assertEquals(FrameworkType.FLUTTER, frameworkInfo.type)
        assertTrue(frameworkInfo.packageIndicators.isNotEmpty())

        // Act: Find handler through registry
        val handler = FrameworkHandlerRegistry.findHandler(elements)

        // Assert: Handler should be FlutterHandler
        assertNotNull(handler)
        assertEquals(FrameworkType.FLUTTER, handler.frameworkType)
        assertTrue(handler.canHandle(elements))

        // Assert: Handler should process Flutter semantic elements
        val processedElements = handler.processElements(elements)
        assertTrue(processedElements.isNotEmpty())
    }

    @Test
    fun `test flutter detection from FlutterSemanticsView class`() {
        val elements = listOf(
            ElementInfo(
                className = "io.flutter.view.FlutterSemanticsView",
                text = "Submit Button",
                isClickable = true,
                bounds = Bounds(100, 200, 300, 250)
            ),
            ElementInfo(
                className = "io.flutter.embedding.android.FlutterSurfaceView",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.FLUTTER, handler.frameworkType)
    }

    @Test
    fun `test flutter detection identifies SemanticsNode with accessibility content`() {
        val elements = listOf(
            ElementInfo(
                className = "io.flutter.SemanticsNode",
                contentDescription = "Navigation Menu",
                isClickable = true,
                bounds = Bounds(0, 100, 200, 150)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.FLUTTER, handler.frameworkType)
        assertTrue(flutterHandler.isActionable(elements.first()))
    }

    // ================================================================
    // Test 2: Unity Detection from Activity Class Name
    // ================================================================

    @Test
    fun `test unity detection from activity class name`() {
        // Arrange: Create elements that represent a Unity game
        val elements = createUnityAppElements()

        // Act: Detect framework
        val classNames = elements.map { it.className }
        val frameworkInfo = FrameworkDetector.detect("com.example.unitygame", classNames)

        // Assert: Framework should be Unity
        assertEquals(FrameworkType.UNITY, frameworkInfo.type)
        assertTrue(frameworkInfo.packageIndicators.any { it.contains("unity3d") })

        // Act: Find handler
        val handler = FrameworkHandlerRegistry.findHandler(elements)

        // Assert: Handler should be UnityHandler
        assertNotNull(handler)
        assertEquals(FrameworkType.UNITY, handler.frameworkType)
    }

    @Test
    fun `test unity detection from UnityPlayerActivity`() {
        val elements = listOf(
            ElementInfo(
                className = "com.unity3d.player.UnityPlayerActivity",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val classNames = elements.map { it.className }
        val frameworkInfo = FrameworkDetector.detect("com.example.game", classNames)

        assertEquals(FrameworkType.UNITY, frameworkInfo.type)

        val handler = FrameworkHandlerRegistry.findHandler(elements)
        assertNotNull(handler)
        assertEquals(FrameworkType.UNITY, handler.frameworkType)
    }

    @Test
    fun `test unity detection from UnityPlayerNativeActivity`() {
        val elements = listOf(
            ElementInfo(
                className = "com.unity3d.player.UnityPlayerNativeActivity",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.UNITY, handler.frameworkType)
    }

    @Test
    fun `test unity handler identifies main player surface`() {
        val playerElement = ElementInfo(
            className = "com.unity3d.player.UnityPlayer",
            bounds = Bounds(0, 0, 1080, 1920)
        )

        assertTrue(unityHandler.isUnityPlayerSurface(playerElement))
    }

    // ================================================================
    // Test 3: React Native Detection from RN Bridge Patterns
    // ================================================================

    @Test
    fun `test react native detection from rn bridge patterns`() {
        // Arrange: Create elements that represent a React Native app
        val elements = createReactNativeAppElements()

        // Act: Detect framework
        val classNames = elements.map { it.className }
        val frameworkInfo = FrameworkDetector.detect("com.example.rnapp", classNames)

        // Assert: Framework should be React Native
        assertEquals(FrameworkType.REACT_NATIVE, frameworkInfo.type)

        // Act: Find handler
        val handler = FrameworkHandlerRegistry.findHandler(elements)

        // Assert: Handler should be ReactNativeHandler
        assertNotNull(handler)
        assertEquals(FrameworkType.REACT_NATIVE, handler.frameworkType)
        assertTrue(handler.canHandle(elements))
    }

    @Test
    fun `test react native detection from ReactRootView`() {
        val elements = listOf(
            ElementInfo(
                className = "com.facebook.react.ReactRootView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "com.facebook.react.views.view.ReactViewGroup",
                text = "Welcome",
                bounds = Bounds(0, 100, 1080, 200)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.REACT_NATIVE, handler.frameworkType)
    }

    @Test
    fun `test react native detection from swmansion libraries`() {
        val elements = listOf(
            ElementInfo(
                className = "com.swmansion.reanimated.ReanimatedModule",
                bounds = Bounds(0, 0, 100, 100)
            ),
            ElementInfo(
                className = "com.facebook.react.ReactRootView",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.REACT_NATIVE, handler.frameworkType)
    }

    @Test
    fun `test react native handler identifies component types`() {
        val buttonElement = ElementInfo(
            className = "com.facebook.react.views.text.ReactTextView",
            text = "Press Me",
            isClickable = true
        )
        val inputElement = ElementInfo(
            className = "com.facebook.react.views.textinput.ReactEditText",
            contentDescription = "Email input"
        )

        assertEquals("Text", reactNativeHandler.getComponentType(buttonElement))
        assertEquals("TextInput", reactNativeHandler.getComponentType(inputElement))
    }

    // ================================================================
    // Test 4: WebView Detection from WebView Class
    // ================================================================

    @Test
    fun `test webview detection from webview class`() {
        // Arrange: Create elements that represent a WebView-based app
        val elements = createWebViewAppElements()

        // Act: Detect framework
        val classNames = elements.map { it.className }
        val frameworkInfo = FrameworkDetector.detect("com.example.hybridapp", classNames)

        // Assert: Framework should be WebView
        assertEquals(FrameworkType.WEBVIEW, frameworkInfo.type)

        // Act: Find handler
        val handler = FrameworkHandlerRegistry.findHandler(elements)

        // Assert: Handler should be WebViewHandler
        assertNotNull(handler)
        assertEquals(FrameworkType.WEBVIEW, handler.frameworkType)
    }

    @Test
    fun `test webview detection from android webkit WebView`() {
        val elements = listOf(
            ElementInfo(
                className = "android.webkit.WebView",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val classNames = elements.map { it.className }
        val frameworkInfo = FrameworkDetector.detect("com.example.browser", classNames)

        assertEquals(FrameworkType.WEBVIEW, frameworkInfo.type)

        val handler = FrameworkHandlerRegistry.findHandler(elements)
        assertNotNull(handler)
        assertEquals(FrameworkType.WEBVIEW, handler.frameworkType)
    }

    @Test
    fun `test webview detection from cordova classes`() {
        val elements = listOf(
            ElementInfo(
                className = "org.apache.cordova.CordovaWebView",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.WEBVIEW, handler.frameworkType)
        assertTrue(webViewHandler.isCordova(elements))
    }

    @Test
    fun `test webview detection from xwalk crosswalk classes`() {
        val elements = listOf(
            ElementInfo(
                className = "org.xwalk.core.XWalkView",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.WEBVIEW, handler.frameworkType)
    }

    @Test
    fun `test webview handler identifies web content roles`() {
        val buttonElement = ElementInfo(
            className = "android.webkit.WebView",
            contentDescription = "Submit button"
        )
        val linkElement = ElementInfo(
            className = "android.webkit.WebView",
            contentDescription = "Read more link"
        )

        assertEquals("button", webViewHandler.getHtmlRole(buttonElement))
        assertEquals("link", webViewHandler.getHtmlRole(linkElement))
    }

    // ================================================================
    // Test 5: Native Fallback When No Framework Detected
    // ================================================================

    @Test
    fun `test native fallback when no framework detected`() {
        // Arrange: Create elements that represent a pure native app
        val elements = createNativeAppElements()

        // Act: Detect framework
        val classNames = elements.map { it.className }
        val frameworkInfo = FrameworkDetector.detect("com.example.nativeapp", classNames)

        // Assert: Framework should be NATIVE (default fallback)
        assertEquals(FrameworkType.NATIVE, frameworkInfo.type)
        assertTrue(frameworkInfo.packageIndicators.isEmpty())

        // Act: Find handler
        val handler = FrameworkHandlerRegistry.findHandler(elements)

        // Assert: Handler should be NativeHandler (fallback)
        assertNotNull(handler)
        assertEquals(FrameworkType.NATIVE, handler.frameworkType)
    }

    @Test
    fun `test native handler processes android widget elements`() {
        val elements = listOf(
            ElementInfo(
                className = "android.widget.Button",
                text = "Submit",
                isClickable = true,
                bounds = Bounds(100, 400, 300, 480)
            ),
            ElementInfo(
                className = "android.widget.TextView",
                text = "Welcome to the app",
                bounds = Bounds(50, 100, 400, 150)
            ),
            ElementInfo(
                className = "android.widget.EditText",
                contentDescription = "Email address",
                isClickable = true,
                bounds = Bounds(50, 200, 400, 280)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.NATIVE, handler.frameworkType)

        val processedElements = handler.processElements(elements)
        assertTrue(processedElements.isNotEmpty())
    }

    @Test
    fun `test native handler identifies material components`() {
        val materialButton = ElementInfo(
            className = "com.google.android.material.button.MaterialButton",
            text = "Save",
            isClickable = true
        )

        assertTrue(nativeHandler.isMaterialComponent(materialButton))
    }

    @Test
    fun `test native handler identifies layout containers`() {
        val linearLayout = ElementInfo(
            className = "android.widget.LinearLayout",
            bounds = Bounds(0, 0, 1080, 500)
        )
        val constraintLayout = ElementInfo(
            className = "androidx.constraintlayout.widget.ConstraintLayout",
            bounds = Bounds(0, 0, 1080, 1920)
        )
        val button = ElementInfo(
            className = "android.widget.Button",
            text = "Click",
            isClickable = true
        )

        assertTrue(nativeHandler.isLayoutContainer(linearLayout))
        assertTrue(nativeHandler.isLayoutContainer(constraintLayout))
        assertTrue(!nativeHandler.isLayoutContainer(button))
    }

    // ================================================================
    // Test 6: Framework Detection Does Not Regress for Known Apps
    // ================================================================

    @Test
    fun `test framework detection does not regress for known apps`() {
        // Known Flutter apps pattern
        val flutterElements = createKnownFlutterAppElements()
        val flutterHandler = FrameworkHandlerRegistry.findHandler(flutterElements)
        assertEquals(FrameworkType.FLUTTER, flutterHandler?.frameworkType)

        // Known Unity game pattern
        val unityElements = createKnownUnityGameElements()
        val unityHandler = FrameworkHandlerRegistry.findHandler(unityElements)
        assertEquals(FrameworkType.UNITY, unityHandler?.frameworkType)

        // Known React Native app pattern
        val rnElements = createKnownReactNativeAppElements()
        val rnHandler = FrameworkHandlerRegistry.findHandler(rnElements)
        assertEquals(FrameworkType.REACT_NATIVE, rnHandler?.frameworkType)

        // Known WebView hybrid app pattern
        val webViewElements = createKnownWebViewAppElements()
        val webHandler = FrameworkHandlerRegistry.findHandler(webViewElements)
        assertEquals(FrameworkType.WEBVIEW, webHandler?.frameworkType)

        // Known native app pattern
        val nativeElements = createKnownNativeAppElements()
        val nativeHandlerResult = FrameworkHandlerRegistry.findHandler(nativeElements)
        assertEquals(FrameworkType.NATIVE, nativeHandlerResult?.frameworkType)
    }

    @Test
    fun `test detection for popular flutter apps like google pay pattern`() {
        // Google Pay uses Flutter - simulate its element pattern
        val elements = listOf(
            ElementInfo(
                className = "io.flutter.embedding.android.FlutterView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "io.flutter.view.FlutterSemanticsView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "io.flutter.SemanticsNode",
                contentDescription = "Pay",
                isClickable = true,
                bounds = Bounds(400, 800, 680, 880)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)
        assertEquals(FrameworkType.FLUTTER, handler?.frameworkType)
    }

    @Test
    fun `test detection for popular unity games like pokemon go pattern`() {
        // Pokemon Go uses Unity - simulate its element pattern
        val elements = listOf(
            ElementInfo(
                className = "com.unity3d.player.UnityPlayerNativeActivity",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "com.unity3d.player.UnityPlayer",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "android.widget.FrameLayout",
                bounds = Bounds(0, 0, 1080, 100)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)
        assertEquals(FrameworkType.UNITY, handler?.frameworkType)
    }

    // ================================================================
    // Test 7: Multiple Framework Indicators Resolve Correctly
    // ================================================================

    @Test
    fun `test multiple framework indicators resolve correctly`() {
        // Scenario: App has both Flutter and WebView elements
        // (e.g., Flutter app with embedded WebView)
        val mixedElements = listOf(
            ElementInfo(
                className = "io.flutter.embedding.android.FlutterView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "android.webkit.WebView",
                bounds = Bounds(100, 500, 980, 1500)
            )
        )

        // Flutter should win due to higher priority
        val handler = FrameworkHandlerRegistry.findHandler(mixedElements)

        assertNotNull(handler)
        assertEquals(FrameworkType.FLUTTER, handler.frameworkType)
    }

    @Test
    fun `test flutter takes priority over webview in hybrid scenario`() {
        // Flutter with WebView plugin scenario
        val elements = listOf(
            ElementInfo(
                className = "io.flutter.plugins.webviewflutter.FlutterWebView",
                bounds = Bounds(0, 200, 1080, 1500)
            ),
            ElementInfo(
                className = "android.webkit.WebView",
                bounds = Bounds(0, 200, 1080, 1500)
            ),
            ElementInfo(
                className = "io.flutter.SemanticsNode",
                text = "Back",
                isClickable = true,
                bounds = Bounds(0, 0, 100, 100)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)
        assertEquals(FrameworkType.FLUTTER, handler?.frameworkType)
    }

    @Test
    fun `test react native takes priority over webview in hybrid scenario`() {
        // React Native with WebView component
        val elements = listOf(
            ElementInfo(
                className = "com.facebook.react.ReactRootView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "android.webkit.WebView",
                bounds = Bounds(0, 400, 1080, 1600)
            ),
            ElementInfo(
                className = "com.facebook.react.views.text.ReactTextView",
                text = "Home",
                isClickable = true,
                bounds = Bounds(0, 0, 200, 100)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)
        assertEquals(FrameworkType.REACT_NATIVE, handler?.frameworkType)
    }

    @Test
    fun `test unity with native overlay elements still detected as unity`() {
        // Unity game with native Android UI overlay
        val elements = listOf(
            ElementInfo(
                className = "com.unity3d.player.UnityPlayer",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "android.widget.Button",
                text = "Settings",
                isClickable = true,
                bounds = Bounds(900, 50, 1050, 100)
            ),
            ElementInfo(
                className = "com.google.android.material.floatingactionbutton.FloatingActionButton",
                contentDescription = "Menu",
                isClickable = true,
                bounds = Bounds(920, 1750, 1030, 1860)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)
        assertEquals(FrameworkType.UNITY, handler?.frameworkType)
    }

    @Test
    fun `test priority order flutter gt unity gt react native gt webview gt native`() {
        // Verify the priority chain by testing each transition point

        // Flutter vs Unity: Flutter wins
        val flutterVsUnity = listOf(
            ElementInfo(className = "io.flutter.embedding.android.FlutterView"),
            ElementInfo(className = "com.unity3d.player.UnityPlayer")
        )
        assertEquals(FrameworkType.FLUTTER, FrameworkHandlerRegistry.findHandler(flutterVsUnity)?.frameworkType)

        // Unity vs React Native: Unity wins (higher priority)
        val unityVsRN = listOf(
            ElementInfo(className = "com.unity3d.player.UnityPlayer"),
            ElementInfo(className = "com.facebook.react.ReactRootView")
        )
        assertEquals(FrameworkType.UNITY, FrameworkHandlerRegistry.findHandler(unityVsRN)?.frameworkType)

        // React Native vs WebView: React Native wins
        val rnVsWebView = listOf(
            ElementInfo(className = "com.facebook.react.ReactRootView"),
            ElementInfo(className = "android.webkit.WebView")
        )
        assertEquals(FrameworkType.REACT_NATIVE, FrameworkHandlerRegistry.findHandler(rnVsWebView)?.frameworkType)

        // WebView vs Native: WebView wins
        val webViewVsNative = listOf(
            ElementInfo(className = "android.webkit.WebView"),
            ElementInfo(className = "android.widget.Button", text = "Click", isClickable = true)
        )
        assertEquals(FrameworkType.WEBVIEW, FrameworkHandlerRegistry.findHandler(webViewVsNative)?.frameworkType)
    }

    // ================================================================
    // Additional Integration Tests
    // ================================================================

    @Test
    fun `test handler registry returns handlers in priority order`() {
        val handlers = FrameworkHandlerRegistry.getHandlers()

        assertEquals(5, handlers.size)

        // Verify order by priority (highest to lowest)
        assertTrue(handlers[0].getPriority() >= handlers[1].getPriority())
        assertTrue(handlers[1].getPriority() >= handlers[2].getPriority())
        assertTrue(handlers[2].getPriority() >= handlers[3].getPriority())
        assertTrue(handlers[3].getPriority() >= handlers[4].getPriority())

        // Verify specific order
        assertEquals(FrameworkType.FLUTTER, handlers[0].frameworkType)
        assertEquals(FrameworkType.UNITY, handlers[1].frameworkType)
        assertEquals(FrameworkType.REACT_NATIVE, handlers[2].frameworkType)
        assertEquals(FrameworkType.WEBVIEW, handlers[3].frameworkType)
        assertEquals(FrameworkType.NATIVE, handlers[4].frameworkType)
    }

    @Test
    fun `test empty element list falls back to native handler`() {
        val emptyElements = emptyList<ElementInfo>()

        val handler = FrameworkHandlerRegistry.findHandler(emptyElements)

        assertNotNull(handler)
        assertEquals(FrameworkType.NATIVE, handler.frameworkType)
    }

    @Test
    fun `test handler processes actionable elements correctly`() {
        val elements = listOf(
            ElementInfo(
                className = "android.widget.Button",
                text = "Submit",
                isClickable = true,
                bounds = Bounds(100, 400, 300, 480)
            ),
            ElementInfo(
                className = "android.widget.LinearLayout",
                bounds = Bounds(0, 0, 1080, 200)
            ),
            ElementInfo(
                className = "android.widget.TextView",
                text = "Welcome",
                bounds = Bounds(50, 50, 400, 100)
            )
        )

        val handler = FrameworkHandlerRegistry.findHandler(elements)!!
        val processedElements = handler.processElements(elements)

        // Layout without content should be filtered out
        assertTrue(processedElements.none { it.className.contains("LinearLayout") && !it.hasVoiceContent })

        // Button and TextView with content should remain
        assertTrue(processedElements.any { it.text == "Submit" })
        assertTrue(processedElements.any { it.text == "Welcome" })
    }

    @Test
    fun `test detection consistency across multiple calls`() {
        val flutterElements = createFlutterAppElements()

        // Call detection multiple times
        repeat(10) {
            val handler = FrameworkHandlerRegistry.findHandler(flutterElements)
            assertEquals(FrameworkType.FLUTTER, handler?.frameworkType)
        }
    }

    // ================================================================
    // Test Fixtures - Flutter App Elements
    // ================================================================

    private fun createFlutterAppElements(): List<ElementInfo> {
        return listOf(
            ElementInfo(
                className = "io.flutter.embedding.android.FlutterView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "io.flutter.SemanticsNode",
                text = "Login",
                isClickable = true,
                bounds = Bounds(400, 800, 680, 880)
            ),
            ElementInfo(
                className = "io.flutter.SemanticsNode",
                contentDescription = "Username input field",
                isClickable = true,
                bounds = Bounds(100, 400, 980, 500)
            ),
            ElementInfo(
                className = "io.flutter.SemanticsNode",
                contentDescription = "Password input field",
                isClickable = true,
                bounds = Bounds(100, 550, 980, 650)
            )
        )
    }

    // ================================================================
    // Test Fixtures - Unity App Elements
    // ================================================================

    private fun createUnityAppElements(): List<ElementInfo> {
        return listOf(
            ElementInfo(
                className = "com.unity3d.player.UnityPlayer",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "com.unity3d.player.UnityPlayerActivity",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "android.widget.FrameLayout",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )
    }

    // ================================================================
    // Test Fixtures - React Native App Elements
    // ================================================================

    private fun createReactNativeAppElements(): List<ElementInfo> {
        return listOf(
            ElementInfo(
                className = "com.facebook.react.ReactRootView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "com.facebook.react.views.view.ReactViewGroup",
                bounds = Bounds(0, 100, 1080, 300)
            ),
            ElementInfo(
                className = "com.facebook.react.views.text.ReactTextView",
                text = "Welcome to React Native",
                bounds = Bounds(100, 150, 980, 200)
            ),
            ElementInfo(
                className = "com.facebook.react.views.textinput.ReactEditText",
                contentDescription = "Search",
                isClickable = true,
                bounds = Bounds(100, 250, 980, 320)
            )
        )
    }

    // ================================================================
    // Test Fixtures - WebView App Elements
    // ================================================================

    private fun createWebViewAppElements(): List<ElementInfo> {
        return listOf(
            ElementInfo(
                className = "android.webkit.WebView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "android.webkit.WebViewClient",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "android.widget.FrameLayout",
                bounds = Bounds(0, 0, 1080, 100)
            )
        )
    }

    // ================================================================
    // Test Fixtures - Native App Elements
    // ================================================================

    private fun createNativeAppElements(): List<ElementInfo> {
        return listOf(
            ElementInfo(
                className = "android.widget.LinearLayout",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "android.widget.TextView",
                text = "Settings",
                bounds = Bounds(50, 100, 400, 150)
            ),
            ElementInfo(
                className = "android.widget.Button",
                text = "Save",
                isClickable = true,
                bounds = Bounds(100, 400, 300, 480)
            ),
            ElementInfo(
                className = "androidx.recyclerview.widget.RecyclerView",
                isScrollable = true,
                bounds = Bounds(0, 200, 1080, 1800)
            )
        )
    }

    // ================================================================
    // Test Fixtures - Known App Patterns for Regression Tests
    // ================================================================

    private fun createKnownFlutterAppElements(): List<ElementInfo> {
        // Pattern similar to Google Pay, Alibaba, eBay Motors
        return listOf(
            ElementInfo(
                className = "io.flutter.embedding.android.FlutterView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "io.flutter.view.FlutterSemanticsView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "io.flutter.SemanticsNode",
                text = "Home",
                isClickable = true,
                bounds = Bounds(100, 100, 200, 200)
            )
        )
    }

    private fun createKnownUnityGameElements(): List<ElementInfo> {
        // Pattern similar to Pokemon Go, Call of Duty Mobile
        return listOf(
            ElementInfo(
                className = "com.unity3d.player.UnityPlayerNativeActivity",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "com.unity3d.player.UnityPlayer",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )
    }

    private fun createKnownReactNativeAppElements(): List<ElementInfo> {
        // Pattern similar to Facebook, Instagram, Shopify
        return listOf(
            ElementInfo(
                className = "com.facebook.react.ReactRootView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "com.facebook.react.views.view.ReactViewGroup",
                bounds = Bounds(0, 0, 1080, 100)
            ),
            ElementInfo(
                className = "com.facebook.react.views.text.ReactTextView",
                text = "Feed",
                isClickable = true,
                bounds = Bounds(50, 30, 150, 70)
            )
        )
    }

    private fun createKnownWebViewAppElements(): List<ElementInfo> {
        // Pattern similar to Cordova/Ionic/Capacitor apps
        return listOf(
            ElementInfo(
                className = "android.webkit.WebView",
                bounds = Bounds(0, 0, 1080, 1920)
            ),
            ElementInfo(
                className = "org.apache.cordova.CordovaWebView",
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )
    }

    private fun createKnownNativeAppElements(): List<ElementInfo> {
        // Pattern similar to pure Kotlin/Java Android apps
        return listOf(
            ElementInfo(
                className = "com.google.android.material.appbar.AppBarLayout",
                bounds = Bounds(0, 0, 1080, 150)
            ),
            ElementInfo(
                className = "androidx.appcompat.widget.Toolbar",
                bounds = Bounds(0, 50, 1080, 150)
            ),
            ElementInfo(
                className = "com.google.android.material.floatingactionbutton.FloatingActionButton",
                contentDescription = "Add item",
                isClickable = true,
                bounds = Bounds(920, 1750, 1030, 1860)
            ),
            ElementInfo(
                className = "androidx.recyclerview.widget.RecyclerView",
                isScrollable = true,
                bounds = Bounds(0, 150, 1080, 1700)
            )
        )
    }
}
