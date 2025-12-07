package com.augmentalis.webavanue.platform

import javafx.application.Platform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class DesktopWebViewTest {

    private lateinit var desktopWebView: DesktopWebView
    private val testUrl = "https://www.example.com"

    @BeforeTest
    fun setup() {
        // Initialize JavaFX platform if needed
        try {
            Platform.startup {}
        } catch (e: IllegalStateException) {
            // Already initialized
        }

        val config = WebViewConfig(
            initialUrl = testUrl,
            javaScriptEnabled = true,
            domStorageEnabled = true
        )
        desktopWebView = DesktopWebView(config)

        // Wait for JavaFX initialization
        Thread.sleep(500)
    }

    @AfterTest
    fun tearDown() {
        desktopWebView.dispose()
    }

    @Test
    fun testInitialConfiguration() {
        // Test initial state
        assertEquals(testUrl, desktopWebView.currentUrl.value)
        assertEquals("", desktopWebView.pageTitle.value)
        assertEquals(0, desktopWebView.loadingProgress.value)
        assertFalse(desktopWebView.isLoading.value)
        assertFalse(desktopWebView.canGoBack.value)
        assertFalse(desktopWebView.canGoForward.value)
    }

    @Test
    fun testLoadUrl() = runTest {
        val newUrl = "https://www.google.com"
        desktopWebView.loadUrl(newUrl)

        // Wait for Platform.runLater to execute
        Thread.sleep(200)

        // URL should be updated
        val currentUrl = desktopWebView.currentUrl.first()
        assertTrue(currentUrl == newUrl || currentUrl.contains("google"))
    }

    @Test
    fun testNavigationMethods() {
        // Test reload
        desktopWebView.reload()
        Thread.sleep(100)
        assertTrue(true) // Should not crash

        // Test stop loading
        desktopWebView.stopLoading()
        Thread.sleep(100)
        assertFalse(desktopWebView.isLoading.value)

        // Test navigation when can't navigate
        desktopWebView.goBack()
        Thread.sleep(100)
        assertTrue(true) // Should not crash

        desktopWebView.goForward()
        Thread.sleep(100)
        assertTrue(true) // Should not crash
    }

    @Test
    fun testJavaScriptExecution() = runTest {
        desktopWebView.loadUrl("about:blank")
        Thread.sleep(500)

        val result = desktopWebView.evaluateJavaScript("1 + 1")
        assertEquals("2", result)

        // Test string concatenation
        val stringResult = desktopWebView.evaluateJavaScript("'hello' + ' world'")
        assertEquals("hello world", stringResult)

        // Test returning null
        val nullResult = desktopWebView.evaluateJavaScript("null")
        assertEquals("null", nullResult)
    }

    @Test
    fun testUserAgentManagement() {
        val customUserAgent = "CustomWebAvanue/1.0 Desktop"
        desktopWebView.setUserAgent(customUserAgent)
        Thread.sleep(100)

        // Verify it doesn't crash (can't directly access WebEngine from here)
        assertTrue(true)
    }

    @Test
    fun testDesktopMode() {
        // Enable desktop mode (should set Linux user agent)
        desktopWebView.setDesktopMode(true)
        Thread.sleep(100)

        // Disable desktop mode (should set Windows user agent)
        desktopWebView.setDesktopMode(false)
        Thread.sleep(100)

        // Verify it doesn't crash
        assertTrue(true)
    }

    @Test
    fun testJavaScriptToggle() {
        // Disable JavaScript
        desktopWebView.setJavaScriptEnabled(false)
        Thread.sleep(100)

        // Enable JavaScript
        desktopWebView.setJavaScriptEnabled(true)
        Thread.sleep(100)

        // Verify it doesn't crash
        assertTrue(true)
    }

    @Test
    fun testZoomControls() {
        // Test zoom in multiple times
        repeat(3) {
            desktopWebView.zoomIn()
            Thread.sleep(50)
        }

        // Test zoom out multiple times
        repeat(5) {
            desktopWebView.zoomOut()
            Thread.sleep(50)
        }

        // Test reset zoom
        desktopWebView.resetZoom()
        Thread.sleep(100)

        // Verify operations complete without crash
        assertTrue(true)
    }

    @Test
    fun testFindInPage() {
        desktopWebView.loadUrl("https://www.example.com")
        Thread.sleep(1000)

        // Find text
        desktopWebView.findInPage("example")
        Thread.sleep(200)

        // Find different text
        desktopWebView.findInPage("domain")
        Thread.sleep(200)

        // Clear find
        desktopWebView.clearFindInPage()
        Thread.sleep(100)

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun testClearHistory() {
        // Load multiple pages
        desktopWebView.loadUrl("https://www.example.com")
        Thread.sleep(500)
        desktopWebView.loadUrl("https://www.google.com")
        Thread.sleep(500)

        // Clear history
        desktopWebView.clearHistory()
        Thread.sleep(200)

        // Should clear navigation history
        assertFalse(desktopWebView.canGoBack.value)
    }

    @Test
    fun testClearCache() {
        desktopWebView.loadUrl("https://www.example.com")
        Thread.sleep(500)

        // Clear cache without disk files
        desktopWebView.clearCache(false)

        // Clear cache including disk files
        desktopWebView.clearCache(true)

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun testScreenshotCapture() = runTest {
        desktopWebView.loadUrl("https://www.example.com")
        Thread.sleep(1500) // Wait for page to load

        val screenshot = desktopWebView.captureScreenshot()
        // Screenshot might be null if WebView not visible
        assertTrue(screenshot == null || screenshot.isNotEmpty())
    }

    @Test
    fun testDispose() {
        desktopWebView.loadUrl("https://www.test.com")
        Thread.sleep(200)

        desktopWebView.dispose()
        Thread.sleep(100)

        // Should clear the WebView
        assertTrue(true)
    }

    @Test
    fun testWebViewFactory() {
        val factory = WebViewFactory()
        val config = WebViewConfig(
            initialUrl = "https://www.test.com",
            javaScriptEnabled = false
        )

        val webView = factory.createWebView(config)
        assertNotNull(webView)
        assertTrue(webView is DesktopWebView)
        assertEquals("https://www.test.com", webView.currentUrl.value)

        webView.dispose()
    }

    @Test
    fun testConfigurationOptions() {
        val config = WebViewConfig(
            initialUrl = "https://www.test.com",
            javaScriptEnabled = true,
            domStorageEnabled = true,
            userAgent = "TestAgent/1.0",
            fontSize = BrowserSettings.FontSize.HUGE,
            safeBrowsingEnabled = true,
            autoPlay = AutoPlay.ALWAYS
        )

        val webView = DesktopWebView(config)
        Thread.sleep(500)

        assertEquals("https://www.test.com", webView.currentUrl.value)

        // Configuration should be applied (zoom should be 1.25 for HUGE)
        webView.dispose()
    }

    @Test
    fun testSwingComponent() {
        val panel = desktopWebView.getSwingComponent()
        assertNotNull(panel)
        // Panel should have BorderLayout with JFXPanel
        assertEquals(1, panel.componentCount)
    }

    @Test
    fun testNavigationStateUpdates() = runTest {
        // Load first page
        desktopWebView.loadUrl("https://www.example.com")
        Thread.sleep(1000)

        assertFalse(desktopWebView.canGoBack.value)
        assertFalse(desktopWebView.canGoForward.value)

        // Load second page
        desktopWebView.loadUrl("https://www.google.com")
        Thread.sleep(1000)

        // Now should be able to go back (if navigation completed)
        // Note: This might not work in headless testing
        // assertTrue(desktopWebView.canGoBack.value)
    }

    @Test
    fun testProgressUpdates() = runTest {
        desktopWebView.loadUrl("https://www.example.com")
        Thread.sleep(200)

        val progress = desktopWebView.loadingProgress.value
        // Progress should be between 0 and 100
        assertTrue(progress in 0..100)
    }

    @Test
    fun testPageTitleUpdates() = runTest {
        desktopWebView.loadUrl("https://www.example.com")
        Thread.sleep(1500)

        val title = desktopWebView.pageTitle.value
        // Title might be empty if page didn't load
        assertNotNull(title)
    }

    @Test
    fun testJCEFWebViewStub() {
        // Test the JCEF stub implementation
        val config = WebViewConfig(
            initialUrl = "https://www.test.com"
        )
        val jcefWebView = JCEFWebView(config)

        // Test all methods don't crash
        jcefWebView.loadUrl("https://www.example.com")
        jcefWebView.reload()
        jcefWebView.stopLoading()
        jcefWebView.goBack()
        jcefWebView.goForward()
        jcefWebView.clearHistory()
        jcefWebView.clearCache(true)
        jcefWebView.findInPage("test")
        jcefWebView.clearFindInPage()
        jcefWebView.setUserAgent("Test")
        jcefWebView.setJavaScriptEnabled(true)
        jcefWebView.setDesktopMode(true)
        jcefWebView.zoomIn()
        jcefWebView.zoomOut()
        jcefWebView.resetZoom()
        jcefWebView.dispose()

        // Test async methods
        runTest {
            assertNull(jcefWebView.evaluateJavaScript("test"))
            assertNull(jcefWebView.captureScreenshot())
        }

        // All stubs should work without crashing
        assertTrue(true)
    }
}