package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrameworkHandlerTest {

    @BeforeTest
    fun setup() {
        FrameworkHandlerRegistry.clear()
    }

    @AfterTest
    fun teardown() {
        FrameworkHandlerRegistry.clear()
    }

    // ==================== Registry Tests ====================

    @Test
    fun `registry starts empty`() {
        assertEquals(0, FrameworkHandlerRegistry.getHandlers().size)
    }

    @Test
    fun `register adds handler`() {
        FrameworkHandlerRegistry.register(FlutterHandler())
        assertEquals(1, FrameworkHandlerRegistry.getHandlers().size)
    }

    @Test
    fun `unregister removes handler`() {
        val handler = FlutterHandler()
        FrameworkHandlerRegistry.register(handler)
        FrameworkHandlerRegistry.unregister(handler)
        assertEquals(0, FrameworkHandlerRegistry.getHandlers().size)
    }

    @Test
    fun `registerDefaults adds all handlers`() {
        FrameworkHandlerRegistry.registerDefaults()
        assertEquals(5, FrameworkHandlerRegistry.getHandlers().size)
    }

    @Test
    fun `handlers sorted by priority`() {
        FrameworkHandlerRegistry.registerDefaults()
        val handlers = FrameworkHandlerRegistry.getHandlers()

        // Flutter should be first (highest priority)
        assertEquals(FrameworkType.FLUTTER, handlers[0].frameworkType)
        // Native should be last (lowest priority)
        assertEquals(FrameworkType.NATIVE, handlers.last().frameworkType)
    }

    @Test
    fun `findHandler returns correct handler for Flutter`() {
        FrameworkHandlerRegistry.registerDefaults()
        val elements = listOf(createFlutterElement())
        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.FLUTTER, handler.frameworkType)
    }

    @Test
    fun `findHandler returns correct handler for Unity`() {
        FrameworkHandlerRegistry.registerDefaults()
        val elements = listOf(createUnityElement())
        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.UNITY, handler.frameworkType)
    }

    @Test
    fun `findHandler returns Native for unknown elements`() {
        FrameworkHandlerRegistry.registerDefaults()
        val elements = listOf(createNativeElement())
        val handler = FrameworkHandlerRegistry.findHandler(elements)

        assertNotNull(handler)
        assertEquals(FrameworkType.NATIVE, handler.frameworkType)
    }

    @Test
    fun `getHandler by type returns correct handler`() {
        FrameworkHandlerRegistry.registerDefaults()
        val handler = FrameworkHandlerRegistry.getHandler(FrameworkType.REACT_NATIVE)

        assertNotNull(handler)
        assertEquals(FrameworkType.REACT_NATIVE, handler.frameworkType)
    }

    @Test
    fun `getHandler returns null for unregistered type`() {
        val handler = FrameworkHandlerRegistry.getHandler(FrameworkType.FLUTTER)
        assertNull(handler)
    }

    // ==================== Flutter Handler Tests ====================

    @Test
    fun `FlutterHandler canHandle Flutter elements`() {
        val handler = FlutterHandler()
        val elements = listOf(createFlutterElement())
        assertTrue(handler.canHandle(elements))
    }

    @Test
    fun `FlutterHandler cannot handle native elements`() {
        val handler = FlutterHandler()
        val elements = listOf(createNativeElement())
        assertFalse(handler.canHandle(elements))
    }

    @Test
    fun `FlutterHandler processElements filters relevant elements`() {
        val handler = FlutterHandler()
        val elements = listOf(
            createFlutterElement(),
            ElementInfo(className = "io.flutter.FlutterEngine"),
            createFlutterElementWithContent()
        )
        val processed = handler.processElements(elements)

        assertEquals(1, processed.size) // Only element with content
    }

    @Test
    fun `FlutterHandler has correct priority`() {
        val handler = FlutterHandler()
        assertEquals(100, handler.getPriority())
    }

    @Test
    fun `FlutterHandler getWidgetType returns correct type`() {
        val handler = FlutterHandler()
        assertEquals("Button", handler.getWidgetType(ElementInfo(className = "FlutterButton")))
        assertEquals("TextField", handler.getWidgetType(ElementInfo(className = "FlutterTextField")))
        assertEquals("Widget", handler.getWidgetType(ElementInfo(className = "Unknown")))
    }

    // ==================== Unity Handler Tests ====================

    @Test
    fun `UnityHandler canHandle Unity elements`() {
        val handler = UnityHandler()
        val elements = listOf(createUnityElement())
        assertTrue(handler.canHandle(elements))
    }

    @Test
    fun `UnityHandler cannot handle Flutter elements`() {
        val handler = UnityHandler()
        val elements = listOf(createFlutterElement())
        assertFalse(handler.canHandle(elements))
    }

    @Test
    fun `UnityHandler isUnityPlayerSurface identifies player`() {
        val handler = UnityHandler()
        assertTrue(handler.isUnityPlayerSurface(createUnityElement()))
        assertFalse(handler.isUnityPlayerSurface(createNativeElement()))
    }

    @Test
    fun `UnityHandler has correct priority`() {
        val handler = UnityHandler()
        assertEquals(90, handler.getPriority())
    }

    // ==================== React Native Handler Tests ====================

    @Test
    fun `ReactNativeHandler canHandle RN elements`() {
        val handler = ReactNativeHandler()
        val elements = listOf(createReactNativeElement())
        assertTrue(handler.canHandle(elements))
    }

    @Test
    fun `ReactNativeHandler cannot handle Unity elements`() {
        val handler = ReactNativeHandler()
        val elements = listOf(createUnityElement())
        assertFalse(handler.canHandle(elements))
    }

    @Test
    fun `ReactNativeHandler getComponentType returns correct type`() {
        val handler = ReactNativeHandler()
        assertEquals("Button", handler.getComponentType(ElementInfo(className = "ReactButton")))
        assertEquals("TextInput", handler.getComponentType(ElementInfo(className = "ReactEditText")))
        assertEquals("View", handler.getComponentType(ElementInfo(className = "ReactViewGroup")))
    }

    @Test
    fun `ReactNativeHandler has correct priority`() {
        val handler = ReactNativeHandler()
        assertEquals(80, handler.getPriority())
    }

    // ==================== WebView Handler Tests ====================

    @Test
    fun `WebViewHandler canHandle WebView elements`() {
        val handler = WebViewHandler()
        val elements = listOf(createWebViewElement())
        assertTrue(handler.canHandle(elements))
    }

    @Test
    fun `WebViewHandler isWebViewContainer identifies container`() {
        val handler = WebViewHandler()
        assertTrue(handler.isWebViewContainer(createWebViewElement()))
        assertFalse(handler.isWebViewContainer(createNativeElement()))
    }

    @Test
    fun `WebViewHandler isCordova detects Cordova`() {
        val handler = WebViewHandler()
        val elements = listOf(ElementInfo(className = "org.apache.cordova.CordovaWebView"))
        assertTrue(handler.isCordova(elements))
    }

    @Test
    fun `WebViewHandler getHtmlRole returns correct role`() {
        val handler = WebViewHandler()
        assertEquals("button", handler.getHtmlRole(ElementInfo(className = "", contentDescription = "button")))
        assertEquals("link", handler.getHtmlRole(ElementInfo(className = "", contentDescription = "link")))
        assertEquals("element", handler.getHtmlRole(ElementInfo(className = "")))
    }

    @Test
    fun `WebViewHandler has correct priority`() {
        val handler = WebViewHandler()
        assertEquals(70, handler.getPriority())
    }

    // ==================== Native Handler Tests ====================

    @Test
    fun `NativeHandler canHandle any elements`() {
        val handler = NativeHandler()
        assertTrue(handler.canHandle(listOf(createNativeElement())))
        assertTrue(handler.canHandle(listOf(createFlutterElement())))
        assertTrue(handler.canHandle(emptyList()))
    }

    @Test
    fun `NativeHandler isLayoutContainer identifies layouts`() {
        val handler = NativeHandler()
        assertTrue(handler.isLayoutContainer(ElementInfo(className = "android.widget.LinearLayout")))
        assertTrue(handler.isLayoutContainer(ElementInfo(className = "ConstraintLayout")))
        assertFalse(handler.isLayoutContainer(ElementInfo(className = "android.widget.Button")))
    }

    @Test
    fun `NativeHandler getWidgetType returns correct type`() {
        val handler = NativeHandler()
        assertEquals("Button", handler.getWidgetType(ElementInfo(className = "android.widget.Button")))
        assertEquals("EditText", handler.getWidgetType(ElementInfo(className = "android.widget.EditText")))
        assertEquals("RecyclerView", handler.getWidgetType(ElementInfo(className = "androidx.recyclerview.widget.RecyclerView")))
    }

    @Test
    fun `NativeHandler isMaterialComponent identifies Material`() {
        val handler = NativeHandler()
        assertTrue(handler.isMaterialComponent(ElementInfo(className = "com.google.android.material.button.MaterialButton")))
        assertFalse(handler.isMaterialComponent(ElementInfo(className = "android.widget.Button")))
    }

    @Test
    fun `NativeHandler has lowest priority`() {
        val handler = NativeHandler()
        assertEquals(0, handler.getPriority())
    }

    // ==================== FrameworkHandlingResult Tests ====================

    @Test
    fun `FrameworkHandlingResult stores data correctly`() {
        val elements = listOf(createNativeElement())
        val result = FrameworkHandlingResult(
            frameworkType = FrameworkType.NATIVE,
            processedElements = elements,
            actionableCount = 1,
            metadata = mapOf("key" to "value")
        )

        assertEquals(FrameworkType.NATIVE, result.frameworkType)
        assertEquals(1, result.processedElements.size)
        assertEquals(1, result.actionableCount)
        assertEquals("value", result.metadata["key"])
    }

    // ==================== Helper Methods ====================

    private fun createFlutterElement(): ElementInfo {
        return ElementInfo(
            className = "io.flutter.embedding.android.FlutterView",
            bounds = Bounds(0, 0, 1080, 1920)
        )
    }

    private fun createFlutterElementWithContent(): ElementInfo {
        return ElementInfo(
            className = "io.flutter.SemanticsNode",
            text = "Submit",
            isClickable = true,
            bounds = Bounds(0, 0, 100, 50)
        )
    }

    private fun createUnityElement(): ElementInfo {
        return ElementInfo(
            className = "com.unity3d.player.UnityPlayer",
            bounds = Bounds(0, 0, 1080, 1920)
        )
    }

    private fun createReactNativeElement(): ElementInfo {
        return ElementInfo(
            className = "com.facebook.react.ReactRootView",
            bounds = Bounds(0, 0, 1080, 1920)
        )
    }

    private fun createWebViewElement(): ElementInfo {
        return ElementInfo(
            className = "android.webkit.WebView",
            bounds = Bounds(0, 0, 1080, 1920)
        )
    }

    private fun createNativeElement(): ElementInfo {
        return ElementInfo(
            className = "android.widget.Button",
            text = "Click Me",
            isClickable = true,
            bounds = Bounds(0, 0, 100, 50)
        )
    }
}
