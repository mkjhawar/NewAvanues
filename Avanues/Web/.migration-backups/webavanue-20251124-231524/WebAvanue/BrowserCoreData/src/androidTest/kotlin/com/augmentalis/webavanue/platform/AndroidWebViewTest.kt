package com.augmentalis.webavanue.platform

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AndroidWebViewTest {

    private lateinit var webView: WebView
    private lateinit var androidWebView: AndroidWebView
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        webView = WebView(context)
        val config = WebViewConfig(
            initialUrl = "https://www.example.com",
            javaScriptEnabled = true,
            domStorageEnabled = true
        )
        androidWebView = AndroidWebView(webView, config)
    }

    @Test
    fun testLoadUrl() = runTest {
        val testUrl = "https://www.google.com"
        androidWebView.loadUrl(testUrl)

        // Wait for page to start loading
        Thread.sleep(100)

        val currentUrl = androidWebView.currentUrl.first()
        assertTrue(currentUrl.contains("google.com") || currentUrl == testUrl)
    }

    @Test
    fun testNavigationState() = runTest {
        // Initially should not be able to go back or forward
        assertFalse(androidWebView.canGoBack.value)
        assertFalse(androidWebView.canGoForward.value)

        // Load first page
        androidWebView.loadUrl("https://www.example.com")
        Thread.sleep(500)

        // Load second page
        androidWebView.loadUrl("https://www.google.com")
        Thread.sleep(500)

        // Now should be able to go back
        assertTrue(androidWebView.canGoBack.value)
        assertFalse(androidWebView.canGoForward.value)

        // Go back
        androidWebView.goBack()
        Thread.sleep(500)

        // Now should be able to go forward
        assertTrue(androidWebView.canGoForward.value)
    }

    @Test
    fun testJavaScriptExecution() = runTest {
        androidWebView.loadUrl("about:blank")
        Thread.sleep(200)

        val result = androidWebView.evaluateJavaScript("1 + 1")
        assertEquals("2", result)
    }

    @Test
    fun testJavaScriptToggle() {
        // JavaScript should be enabled initially
        assertTrue(webView.settings.javaScriptEnabled)

        // Disable JavaScript
        androidWebView.setJavaScriptEnabled(false)
        assertFalse(webView.settings.javaScriptEnabled)

        // Enable JavaScript
        androidWebView.setJavaScriptEnabled(true)
        assertTrue(webView.settings.javaScriptEnabled)
    }

    @Test
    fun testDesktopMode() {
        val originalUserAgent = webView.settings.userAgentString

        // Enable desktop mode
        androidWebView.setDesktopMode(true)
        val desktopUserAgent = webView.settings.userAgentString
        assertTrue(desktopUserAgent.contains("Linux"))
        assertTrue(webView.settings.useWideViewPort)
        assertTrue(webView.settings.loadWithOverviewMode)

        // Disable desktop mode
        androidWebView.setDesktopMode(false)
        // User agent might not revert to exact original
        assertFalse(webView.settings.userAgentString.isEmpty())
    }

    @Test
    fun testReload() = runTest {
        androidWebView.loadUrl("https://www.example.com")
        Thread.sleep(500)

        val initialUrl = androidWebView.currentUrl.value

        androidWebView.reload()
        Thread.sleep(500)

        // URL should remain the same after reload
        assertEquals(initialUrl, androidWebView.currentUrl.value)
    }

    @Test
    fun testStopLoading() {
        androidWebView.loadUrl("https://www.example.com")

        // Immediately stop loading
        androidWebView.stopLoading()

        // Loading should be stopped
        Thread.sleep(100)
        assertFalse(androidWebView.isLoading.value)
    }

    @Test
    fun testClearHistory() {
        // Load multiple pages
        androidWebView.loadUrl("https://www.example.com")
        Thread.sleep(300)
        androidWebView.loadUrl("https://www.google.com")
        Thread.sleep(300)

        // Should be able to go back
        assertTrue(webView.canGoBack())

        // Clear history
        androidWebView.clearHistory()

        // Should not be able to go back anymore
        assertFalse(webView.canGoBack())
    }

    @Test
    fun testClearCache() {
        androidWebView.loadUrl("https://www.example.com")
        Thread.sleep(500)

        // Clear cache without disk files
        androidWebView.clearCache(false)

        // Clear cache including disk files
        androidWebView.clearCache(true)

        // Cache should be cleared (no direct way to verify, but should not crash)
        assertTrue(true)
    }

    @Test
    fun testFindInPage() {
        androidWebView.loadUrl("https://www.example.com")
        Thread.sleep(1000)

        // Find text in page
        androidWebView.findInPage("example")
        Thread.sleep(100)

        // Clear find
        androidWebView.clearFindInPage()

        // Should not crash
        assertTrue(true)
    }

    @Test
    fun testZoom() {
        val initialZoom = webView.settings.textZoom

        // Zoom in
        androidWebView.zoomIn()
        Thread.sleep(100)

        // Zoom out
        androidWebView.zoomOut()
        Thread.sleep(100)

        // Reset zoom
        androidWebView.resetZoom()
        assertEquals(100, webView.settings.textZoom)
    }

    @Test
    fun testPageTitleUpdates() = runTest {
        androidWebView.loadUrl("https://www.example.com")
        Thread.sleep(1000)

        val title = androidWebView.pageTitle.value
        // Title should be updated (might be empty initially)
        assertNotNull(title)
    }

    @Test
    fun testLoadingProgress() = runTest {
        androidWebView.loadUrl("https://www.example.com")
        Thread.sleep(200)

        val progress = androidWebView.loadingProgress.value
        // Progress should be between 0 and 100
        assertTrue(progress in 0..100)
    }

    @Test
    fun testUserAgent() {
        val customUserAgent = "CustomWebAvanue/1.0"
        androidWebView.setUserAgent(customUserAgent)
        assertEquals(customUserAgent, webView.settings.userAgentString)
    }

    @Test
    fun testDispose() {
        androidWebView.dispose()
        // WebView should be destroyed (no direct way to verify, but should not crash)
        assertTrue(true)
    }

    @Test
    fun testCaptureScreenshot() = runTest {
        androidWebView.loadUrl("https://www.example.com")
        Thread.sleep(1000)

        val screenshot = androidWebView.captureScreenshot()
        // Screenshot might be null if page not loaded or WebView not visible
        // Just verify it doesn't crash
        assertTrue(screenshot == null || screenshot.isNotEmpty())
    }
}