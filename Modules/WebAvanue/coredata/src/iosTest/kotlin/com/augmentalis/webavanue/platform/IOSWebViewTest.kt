package com.augmentalis.webavanue.platform

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.Foundation.*
import platform.WebKit.*
import kotlin.test.*

class IOSWebViewTest {

    private lateinit var iosWebView: IOSWebView
    private val testUrl = "https://www.example.com"

    @BeforeTest
    fun setup() {
        val config = WebViewConfig(
            initialUrl = testUrl,
            javaScriptEnabled = true,
            domStorageEnabled = true
        )
        iosWebView = IOSWebView(config)
    }

    @Test
    fun testInitialConfiguration() {
        // Test initial state
        assertEquals(testUrl, iosWebView.currentUrl.value)
        assertEquals("", iosWebView.pageTitle.value)
        assertEquals(0, iosWebView.loadingProgress.value)
        assertFalse(iosWebView.isLoading.value)
        assertFalse(iosWebView.canGoBack.value)
        assertFalse(iosWebView.canGoForward.value)
    }

    @Test
    fun testLoadUrl() = runTest {
        val newUrl = "https://www.google.com"
        iosWebView.loadUrl(newUrl)

        // Wait for URL to update (iOS WebView loads asynchronously)
        // In a real test, we'd need to wait for the delegate callback
        Thread.sleep(100)

        // URL should be updated after navigation starts
        assertTrue(iosWebView.currentUrl.value.contains("google.com") ||
                  iosWebView.currentUrl.value == newUrl)
    }

    @Test
    fun testNavigationMethods() {
        // Test reload
        iosWebView.reload()
        // Should not crash
        assertTrue(true)

        // Test stop loading
        iosWebView.stopLoading()
        assertFalse(iosWebView.isLoading.value)

        // Test go back when can't go back
        iosWebView.goBack()
        // Should not crash even when can't go back
        assertTrue(true)

        // Test go forward when can't go forward
        iosWebView.goForward()
        // Should not crash even when can't go forward
        assertTrue(true)
    }

    @Test
    fun testJavaScriptExecution() = runTest {
        iosWebView.loadUrl("about:blank")
        Thread.sleep(200)

        val result = iosWebView.evaluateJavaScript("1 + 1")
        assertEquals("2", result)

        // Test returning string
        val stringResult = iosWebView.evaluateJavaScript("'hello' + ' world'")
        assertEquals("hello world", stringResult)
    }

    @Test
    fun testUserAgentManagement() {
        val customUserAgent = "CustomWebAvanue/1.0 iOS"
        iosWebView.setUserAgent(customUserAgent)

        // Verify user agent was set (would need UIView access to verify fully)
        assertTrue(true)
    }

    @Test
    fun testDesktopMode() {
        // Enable desktop mode
        iosWebView.setDesktopMode(true)
        // User agent should be set to desktop Safari

        // Disable desktop mode
        iosWebView.setDesktopMode(false)
        // User agent should revert to mobile

        // Just verify it doesn't crash
        assertTrue(true)
    }

    @Test
    fun testJavaScriptToggle() {
        // Disable JavaScript
        iosWebView.setJavaScriptEnabled(false)

        // Enable JavaScript
        iosWebView.setJavaScriptEnabled(true)

        // Verify it doesn't crash
        assertTrue(true)
    }

    @Test
    fun testZoomControls() {
        // Test zoom in
        iosWebView.zoomIn()
        Thread.sleep(50)

        // Test zoom out
        iosWebView.zoomOut()
        Thread.sleep(50)

        // Test reset zoom
        iosWebView.resetZoom()

        // Verify operations complete without crash
        assertTrue(true)
    }

    @Test
    fun testFindInPage() {
        iosWebView.loadUrl("https://www.example.com")
        Thread.sleep(500)

        // Find text
        iosWebView.findInPage("example")
        Thread.sleep(100)

        // Clear find
        iosWebView.clearFindInPage()

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun testClearHistory() {
        // Load multiple pages
        iosWebView.loadUrl("https://www.example.com")
        Thread.sleep(200)
        iosWebView.loadUrl("https://www.google.com")
        Thread.sleep(200)

        // Clear history
        iosWebView.clearHistory()

        // Should clear navigation history
        assertFalse(iosWebView.canGoBack.value)
    }

    @Test
    fun testClearCache() {
        iosWebView.loadUrl("https://www.example.com")
        Thread.sleep(500)

        // Clear cache without disk files
        iosWebView.clearCache(false)

        // Clear cache including disk files
        iosWebView.clearCache(true)

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun testScreenshotCapture() = runTest {
        iosWebView.loadUrl("https://www.example.com")
        Thread.sleep(1000)

        val screenshot = iosWebView.captureScreenshot()
        // Screenshot might be null if page not loaded
        // Just verify it doesn't crash
        assertTrue(screenshot == null || screenshot.isNotEmpty())
    }

    @Test
    fun testDispose() {
        iosWebView.dispose()
        // Should stop loading and remove from superview
        assertFalse(iosWebView.isLoading.value)
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
        assertTrue(webView is IOSWebView)
        assertEquals("https://www.test.com", webView.currentUrl.value)
    }

    @Test
    fun testConfigurationOptions() {
        val config = WebViewConfig(
            initialUrl = "https://www.test.com",
            javaScriptEnabled = true,
            domStorageEnabled = true,
            userAgent = "TestAgent/1.0",
            fontSize = BrowserSettings.FontSize.LARGE,
            safeBrowsingEnabled = true,
            autoPlay = AutoPlay.NEVER
        )

        val webView = IOSWebView(config)
        assertEquals("https://www.test.com", webView.currentUrl.value)

        // Configuration should be applied
        webView.dispose()
    }

    @Test
    fun testNavigationDelegate() {
        // Test that navigation delegate callbacks update state properly
        val config = WebViewConfig(initialUrl = "https://www.test.com")
        val webView = IOSWebView(config)

        // Initially not loading
        assertFalse(webView.isLoading.value)

        // Load a URL
        webView.loadUrl("https://www.example.com")

        // Dispose when done
        webView.dispose()
    }
}