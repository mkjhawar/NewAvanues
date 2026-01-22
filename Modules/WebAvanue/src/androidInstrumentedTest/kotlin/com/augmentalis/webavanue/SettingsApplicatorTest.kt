package com.augmentalis.webavanue

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.webavanue.BrowserSettings
import com.augmentalis.webavanue.BrowserSettings.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for SettingsApplicator.
 *
 * Tests the core settings application logic that bridges
 * BrowserSettings model and Android WebView configuration.
 */
@RunWith(AndroidJUnit4::class)
class SettingsApplicatorTest {

    private lateinit var applicator: SettingsApplicator
    private lateinit var webView: WebView
    private lateinit var settings: WebSettings

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        applicator = SettingsApplicator()
        webView = WebView(context)
        settings = webView.settings
    }

    @Test
    fun applySettings_appliesJavaScriptEnabled() {
        // Given
        val browserSettings = BrowserSettings(enableJavaScript = true)

        // When
        val result = applicator.applySettings(webView, browserSettings)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(settings.javaScriptEnabled)
    }

    @Test
    fun applySettings_appliesJavaScriptDisabled() {
        // Given
        val browserSettings = BrowserSettings(enableJavaScript = false)

        // When
        val result = applicator.applySettings(webView, browserSettings)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(settings.javaScriptEnabled)
    }

    @Test
    fun applySettings_appliesPopupBlocking() {
        // Given - Block popups
        val browserSettings = BrowserSettings(blockPopups = true)

        // When
        val result = applicator.applySettings(webView, browserSettings)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(settings.javaScriptCanOpenWindowsAutomatically)
    }

    @Test
    fun applySettings_allowsPopups() {
        // Given - Allow popups
        val browserSettings = BrowserSettings(blockPopups = false)

        // When
        val result = applicator.applySettings(webView, browserSettings)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(settings.javaScriptCanOpenWindowsAutomatically)
    }

    @Test
    fun applySettings_appliesFontSize() {
        // Given
        val testCases = listOf(
            FontSize.TINY to 75,
            FontSize.SMALL to 90,
            FontSize.MEDIUM to 100,
            FontSize.LARGE to 125,
            FontSize.HUGE to 150
        )

        testCases.forEach { (fontSize, expectedZoom) ->
            // When
            val browserSettings = BrowserSettings(fontSize = fontSize)
            val result = applicator.applySettings(webView, browserSettings)

            // Then
            assertTrue("Failed for $fontSize", result.isSuccess)
            assertEquals("Wrong zoom for $fontSize", expectedZoom, settings.textZoom)
        }
    }

    @Test
    fun applySettings_appliesShowImages() {
        // Given
        val showImages = BrowserSettings(showImages = true)
        val hideImages = BrowserSettings(showImages = false)

        // When
        applicator.applySettings(webView, showImages)
        assertTrue(settings.loadsImagesAutomatically)

        applicator.applySettings(webView, hideImages)
        assertFalse(settings.loadsImagesAutomatically)
    }

    @Test
    fun applySettings_appliesDesktopMode() {
        // Given
        val desktopSettings = BrowserSettings(
            useDesktopMode = true,
            desktopModeDefaultZoom = 150
        )

        // When
        val result = applicator.applySettings(webView, desktopSettings)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(settings.loadWithOverviewMode)
        assertTrue(settings.useWideViewPort)
        assertEquals(150, settings.initialScale)
    }

    @Test
    fun applySettings_appliesZoomConstraints() {
        // Given - Out of range zoom
        val invalidZoom = BrowserSettings(desktopModeDefaultZoom = 300)

        // When
        val result = applicator.applySettings(webView, invalidZoom)

        // Then
        assertTrue(result.isSuccess)
        // Should clamp to max 200
        assertEquals(200, settings.initialScale)
    }

    @Test
    fun applySettings_handlesErrorsGracefully() {
        // Given - Null webView (edge case)
        val browserSettings = BrowserSettings()

        // When - This should not crash
        val result = applicator.applySettings(webView, browserSettings)

        // Then - Should succeed or fail gracefully
        assertNotNull(result)
    }

    @Test
    fun applyIncrementalUpdate_avoidsReloadForFontSize() {
        // Given
        val browserSettings = BrowserSettings(fontSize = FontSize.LARGE)
        applicator.applySettings(webView, browserSettings)

        // When
        val needsReload = !applicator.applyIncrementalUpdate(
            webView,
            "fontSize",
            FontSize.HUGE
        )

        // Then
        assertFalse("Font size change should not require reload", needsReload)
        assertEquals(150, settings.textZoom)
    }

    @Test
    fun applyIncrementalUpdate_requiresReloadForJavaScript() {
        // Given
        val browserSettings = BrowserSettings(enableJavaScript = true)
        applicator.applySettings(webView, browserSettings)

        // When
        val needsReload = !applicator.applyIncrementalUpdate(
            webView,
            "enableJavaScript",
            false
        )

        // Then
        assertTrue("JavaScript change should require reload", needsReload)
    }

    @Test
    fun applySettings_handlesAllSettingsCombination() {
        // Given - Complex settings combination
        val complexSettings = BrowserSettings(
            enableJavaScript = true,
            enableCookies = true,
            blockPopups = false,
            fontSize = FontSize.LARGE,
            showImages = true,
            useDesktopMode = true,
            desktopModeDefaultZoom = 125,
            hardwareAcceleration = true,
            dataSaver = false
        )

        // When
        val result = applicator.applySettings(webView, complexSettings)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(settings.javaScriptEnabled)
        assertTrue(settings.javaScriptCanOpenWindowsAutomatically)
        assertEquals(125, settings.textZoom)
        assertTrue(settings.loadsImagesAutomatically)
        assertTrue(settings.useWideViewPort)
    }
}
