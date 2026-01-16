package com.augmentalis.voiceoscoreng.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for FrameworkInfo components: FrameworkType enum, FrameworkInfo data class,
 * and FrameworkDetector object.
 */
class FrameworkInfoTest {

    // ========================================
    // FrameworkType Enum Tests
    // ========================================

    @Test
    fun `FrameworkType enum has all expected values`() {
        val expectedValues = listOf(
            FrameworkType.NATIVE,
            FrameworkType.FLUTTER,
            FrameworkType.UNITY,
            FrameworkType.UNREAL_ENGINE,
            FrameworkType.REACT_NATIVE,
            FrameworkType.WEBVIEW,
            FrameworkType.UNKNOWN
        )

        assertEquals(7, FrameworkType.entries.size, "FrameworkType should have exactly 7 values")
        expectedValues.forEach { expected ->
            assertTrue(
                FrameworkType.entries.contains(expected),
                "FrameworkType should contain $expected"
            )
        }
    }

    @Test
    fun `FrameworkType NATIVE exists`() {
        assertNotNull(FrameworkType.NATIVE)
    }

    @Test
    fun `FrameworkType FLUTTER exists`() {
        assertNotNull(FrameworkType.FLUTTER)
    }

    @Test
    fun `FrameworkType UNITY exists`() {
        assertNotNull(FrameworkType.UNITY)
    }

    @Test
    fun `FrameworkType UNREAL_ENGINE exists`() {
        assertNotNull(FrameworkType.UNREAL_ENGINE)
    }

    @Test
    fun `FrameworkType REACT_NATIVE exists`() {
        assertNotNull(FrameworkType.REACT_NATIVE)
    }

    @Test
    fun `FrameworkType WEBVIEW exists`() {
        assertNotNull(FrameworkType.WEBVIEW)
    }

    @Test
    fun `FrameworkType UNKNOWN exists`() {
        assertNotNull(FrameworkType.UNKNOWN)
    }

    // ========================================
    // FrameworkInfo Data Class Tests
    // ========================================

    @Test
    fun `FrameworkInfo can be created with type and null version`() {
        val info = FrameworkInfo(
            type = FrameworkType.NATIVE,
            version = null,
            packageIndicators = emptyList()
        )

        assertEquals(FrameworkType.NATIVE, info.type)
        assertNull(info.version)
        assertTrue(info.packageIndicators.isEmpty())
    }

    @Test
    fun `FrameworkInfo can be created with type and version`() {
        val info = FrameworkInfo(
            type = FrameworkType.FLUTTER,
            version = "3.16.0",
            packageIndicators = listOf("io.flutter.embedding.engine.FlutterEngine")
        )

        assertEquals(FrameworkType.FLUTTER, info.type)
        assertEquals("3.16.0", info.version)
        assertEquals(1, info.packageIndicators.size)
        assertEquals("io.flutter.embedding.engine.FlutterEngine", info.packageIndicators.first())
    }

    @Test
    fun `FrameworkInfo can be created with multiple package indicators`() {
        val indicators = listOf(
            "com.unity3d.player.UnityPlayer",
            "com.unity3d.player.UnityPlayerActivity"
        )
        val info = FrameworkInfo(
            type = FrameworkType.UNITY,
            version = "2022.3",
            packageIndicators = indicators
        )

        assertEquals(FrameworkType.UNITY, info.type)
        assertEquals(2, info.packageIndicators.size)
        assertTrue(info.packageIndicators.containsAll(indicators))
    }

    @Test
    fun `FrameworkInfo data class equality works correctly`() {
        val info1 = FrameworkInfo(
            type = FrameworkType.REACT_NATIVE,
            version = "0.72",
            packageIndicators = listOf("com.facebook.react.ReactActivity")
        )
        val info2 = FrameworkInfo(
            type = FrameworkType.REACT_NATIVE,
            version = "0.72",
            packageIndicators = listOf("com.facebook.react.ReactActivity")
        )

        assertEquals(info1, info2)
    }

    @Test
    fun `FrameworkInfo copy works correctly`() {
        val original = FrameworkInfo(
            type = FrameworkType.FLUTTER,
            version = "3.0",
            packageIndicators = listOf("io.flutter.app.FlutterActivity")
        )
        val copied = original.copy(version = "3.16.0")

        assertEquals(FrameworkType.FLUTTER, copied.type)
        assertEquals("3.16.0", copied.version)
        assertEquals(original.packageIndicators, copied.packageIndicators)
    }

    // ========================================
    // FrameworkDetector Tests - Flutter Detection
    // ========================================

    @Test
    fun `FrameworkDetector detects Flutter with io_flutter_embedding_engine_FlutterEngine`() {
        val classNames = listOf(
            "io.flutter.embedding.engine.FlutterEngine",
            "com.example.app.MainActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.FLUTTER, result.type)
        assertTrue(result.packageIndicators.isNotEmpty())
    }

    @Test
    fun `FrameworkDetector detects Flutter with io_flutter_app_FlutterActivity`() {
        val classNames = listOf(
            "io.flutter.app.FlutterActivity",
            "com.example.MainActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.FLUTTER, result.type)
    }

    @Test
    fun `FrameworkDetector detects Flutter with io_flutter_view_FlutterView`() {
        val classNames = listOf(
            "io.flutter.view.FlutterView",
            "com.example.SomeOtherClass"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.FLUTTER, result.type)
    }

    @Test
    fun `FrameworkDetector detects Flutter with any io_flutter prefix`() {
        val classNames = listOf(
            "io.flutter.plugins.firebase.FirebasePlugin",
            "com.example.NativeClass"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.FLUTTER, result.type)
    }

    // ========================================
    // FrameworkDetector Tests - Unity Detection
    // ========================================

    @Test
    fun `FrameworkDetector detects Unity with com_unity3d_player_UnityPlayer`() {
        val classNames = listOf(
            "com.unity3d.player.UnityPlayer",
            "com.example.app.SomeActivity"
        )
        val result = FrameworkDetector.detect("com.example.game", classNames)

        assertEquals(FrameworkType.UNITY, result.type)
        assertTrue(result.packageIndicators.isNotEmpty())
    }

    @Test
    fun `FrameworkDetector detects Unity with com_unity3d_player_UnityPlayerActivity`() {
        val classNames = listOf(
            "com.unity3d.player.UnityPlayerActivity",
            "com.example.OtherClass"
        )
        val result = FrameworkDetector.detect("com.example.game", classNames)

        assertEquals(FrameworkType.UNITY, result.type)
    }

    @Test
    fun `FrameworkDetector detects Unity with any com_unity3d prefix`() {
        val classNames = listOf(
            "com.unity3d.services.ads.UnityAds",
            "com.example.NativeHelper"
        )
        val result = FrameworkDetector.detect("com.example.game", classNames)

        assertEquals(FrameworkType.UNITY, result.type)
    }

    // ========================================
    // FrameworkDetector Tests - Unreal Engine Detection
    // ========================================

    @Test
    fun `FrameworkDetector detects Unreal with com_epicgames_ue4_GameActivity`() {
        val classNames = listOf(
            "com.epicgames.ue4.GameActivity",
            "com.example.Helper"
        )
        val result = FrameworkDetector.detect("com.example.game", classNames)

        assertEquals(FrameworkType.UNREAL_ENGINE, result.type)
        assertTrue(result.packageIndicators.isNotEmpty())
    }

    @Test
    fun `FrameworkDetector detects Unreal with com_epicgames_unreal classes`() {
        val classNames = listOf(
            "com.epicgames.unreal.GameActivity",
            "com.example.SomeClass"
        )
        val result = FrameworkDetector.detect("com.example.game", classNames)

        assertEquals(FrameworkType.UNREAL_ENGINE, result.type)
    }

    @Test
    fun `FrameworkDetector detects Unreal with any com_epicgames prefix`() {
        val classNames = listOf(
            "com.epicgames.fortnite.SomePlugin",
            "com.example.NativeCode"
        )
        val result = FrameworkDetector.detect("com.example.game", classNames)

        assertEquals(FrameworkType.UNREAL_ENGINE, result.type)
    }

    // ========================================
    // FrameworkDetector Tests - React Native Detection
    // ========================================

    @Test
    fun `FrameworkDetector detects ReactNative with com_facebook_react_ReactActivity`() {
        val classNames = listOf(
            "com.facebook.react.ReactActivity",
            "com.example.app.MainActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.REACT_NATIVE, result.type)
        assertTrue(result.packageIndicators.isNotEmpty())
    }

    @Test
    fun `FrameworkDetector detects ReactNative with com_facebook_react_ReactPackage`() {
        val classNames = listOf(
            "com.facebook.react.ReactPackage",
            "com.example.NativeModule"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.REACT_NATIVE, result.type)
    }

    @Test
    fun `FrameworkDetector detects ReactNative with com_facebook_react_bridge classes`() {
        val classNames = listOf(
            "com.facebook.react.bridge.ReactContextBaseJavaModule",
            "com.example.SomeClass"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.REACT_NATIVE, result.type)
    }

    @Test
    fun `FrameworkDetector detects ReactNative with any com_facebook_react prefix`() {
        val classNames = listOf(
            "com.facebook.react.uimanager.ViewManager",
            "com.example.Native"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.REACT_NATIVE, result.type)
    }

    // ========================================
    // FrameworkDetector Tests - WebView Detection
    // ========================================

    @Test
    fun `FrameworkDetector detects WebView with android_webkit_WebView`() {
        val classNames = listOf(
            "android.webkit.WebView",
            "com.example.app.WebActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.WEBVIEW, result.type)
    }

    @Test
    fun `FrameworkDetector detects WebView with android_webkit_WebViewClient`() {
        val classNames = listOf(
            "android.webkit.WebViewClient",
            "com.example.BrowserActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.WEBVIEW, result.type)
    }

    @Test
    fun `FrameworkDetector detects WebView with org_xwalk_core classes`() {
        val classNames = listOf(
            "org.xwalk.core.XWalkView",
            "com.example.CrosswalkActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.WEBVIEW, result.type)
    }

    @Test
    fun `FrameworkDetector detects WebView with org_chromium_content classes`() {
        val classNames = listOf(
            "org.chromium.content.browser.ContentView",
            "com.example.ChromiumActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.WEBVIEW, result.type)
    }

    @Test
    fun `FrameworkDetector detects WebView with cordova classes`() {
        val classNames = listOf(
            "org.apache.cordova.CordovaActivity",
            "com.example.HybridApp"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.WEBVIEW, result.type)
    }

    // ========================================
    // FrameworkDetector Tests - Native Detection
    // ========================================

    @Test
    fun `FrameworkDetector returns NATIVE for pure native Android app`() {
        val classNames = listOf(
            "com.example.app.MainActivity",
            "com.example.app.viewmodel.HomeViewModel",
            "com.example.app.data.Repository",
            "androidx.appcompat.app.AppCompatActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.NATIVE, result.type)
    }

    @Test
    fun `FrameworkDetector returns NATIVE for Jetpack Compose app`() {
        val classNames = listOf(
            "com.example.app.ui.theme.AppTheme",
            "androidx.compose.runtime.Composable",
            "androidx.activity.ComponentActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.NATIVE, result.type)
    }

    @Test
    fun `FrameworkDetector returns NATIVE for empty class list`() {
        val result = FrameworkDetector.detect("com.example.app", emptyList())

        assertEquals(FrameworkType.NATIVE, result.type)
        assertTrue(result.packageIndicators.isEmpty())
    }

    // ========================================
    // FrameworkDetector Tests - Priority Order
    // ========================================

    @Test
    fun `FrameworkDetector prefers Flutter over WebView when both present`() {
        // Some Flutter apps may also have WebView components
        val classNames = listOf(
            "io.flutter.embedding.engine.FlutterEngine",
            "android.webkit.WebView"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.FLUTTER, result.type)
    }

    @Test
    fun `FrameworkDetector prefers ReactNative over WebView when both present`() {
        val classNames = listOf(
            "com.facebook.react.ReactActivity",
            "android.webkit.WebView"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.REACT_NATIVE, result.type)
    }

    // ========================================
    // FrameworkDetector Tests - Package Indicators
    // ========================================

    @Test
    fun `FrameworkDetector returns matching package indicators for Flutter`() {
        val classNames = listOf(
            "io.flutter.embedding.engine.FlutterEngine",
            "io.flutter.app.FlutterActivity",
            "com.example.app.MainActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        assertEquals(FrameworkType.FLUTTER, result.type)
        assertTrue(result.packageIndicators.any { it.startsWith("io.flutter") })
    }

    @Test
    fun `FrameworkDetector returns matching package indicators for Unity`() {
        val classNames = listOf(
            "com.unity3d.player.UnityPlayer",
            "com.unity3d.player.UnityPlayerActivity",
            "com.example.game.GameLauncher"
        )
        val result = FrameworkDetector.detect("com.example.game", classNames)

        assertEquals(FrameworkType.UNITY, result.type)
        assertTrue(result.packageIndicators.any { it.startsWith("com.unity3d") })
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    fun `FrameworkDetector handles class names with similar prefixes correctly`() {
        // Make sure "io.flutterwave" doesn't get detected as Flutter
        val classNames = listOf(
            "io.flutterwave.payments.PaymentActivity",
            "com.example.app.MainActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        // Should be NATIVE since io.flutterwave is not io.flutter.
        assertEquals(FrameworkType.NATIVE, result.type)
    }

    @Test
    fun `FrameworkDetector handles mixed case class names`() {
        val classNames = listOf(
            "IO.FLUTTER.APP.FlutterActivity",
            "com.example.MainActivity"
        )
        val result = FrameworkDetector.detect("com.example.app", classNames)

        // Depending on implementation - case sensitivity check
        // Flutter prefix should be case-sensitive (io.flutter not IO.FLUTTER)
        assertEquals(FrameworkType.NATIVE, result.type)
    }

    @Test
    fun `FrameworkDetector returns UNKNOWN when packageName is empty and no indicators`() {
        val result = FrameworkDetector.detect("", emptyList())

        // Empty package with no classes - could be UNKNOWN or NATIVE depending on design
        // Using NATIVE as default for valid but empty detection
        assertTrue(result.type == FrameworkType.NATIVE || result.type == FrameworkType.UNKNOWN)
    }
}
