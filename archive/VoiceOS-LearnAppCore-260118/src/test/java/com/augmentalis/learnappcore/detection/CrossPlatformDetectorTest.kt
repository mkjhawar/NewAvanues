/**
 * CrossPlatformDetectorTest.kt - Unit tests for CrossPlatformDetector
 *
 * Tests framework detection logic for Flutter, Unity, Unreal, React Native, and Native apps.
 * Validates fallback label generation for game engines (Unity, Unreal).
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: Phase 1 Architecture Improvement Plan
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappcore.detection

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for CrossPlatformDetector
 *
 * Tests:
 * - Framework detection (Flutter, Unity, Unreal, React Native, Native)
 * - Fallback label generation for game engines
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrossPlatformDetectorTest {

    private lateinit var mockNode: AccessibilityNodeInfo

    @Before
    fun setup() {
        mockNode = mockk(relaxed = true)
    }

    // ============================================================
    // Framework Detection Tests
    // ============================================================

    @Test
    fun detectFramework_Flutter_ReturnsFlutter() {
        // Given: Node with Flutter signatures
        every { mockNode.className } returns "io.flutter.view.FlutterView"
        every { mockNode.viewIdResourceName } returns null
        every { mockNode.childCount } returns 0

        // When: Detect framework
        val result = CrossPlatformDetector.detectFramework("com.example.flutter", mockNode)

        // Then: Flutter detected
        assertEquals(AppFramework.FLUTTER, result)
    }

    @Test
    fun detectFramework_Unity_ReturnsUnity() {
        // Given: Node with Unity signatures
        every { mockNode.className } returns "com.unity3d.player.UnityPlayer"
        every { mockNode.viewIdResourceName } returns null
        every { mockNode.childCount } returns 1
        every { mockNode.getChild(0) } returns null

        // When: Detect framework
        val result = CrossPlatformDetector.detectFramework("com.example.unity3d.game", mockNode)

        // Then: Unity detected
        assertEquals(AppFramework.UNITY, result)
    }

    @Test
    fun detectFramework_UnityByPackageName_ReturnsUnity() {
        // Given: Package name with Unity pattern
        every { mockNode.className } returns "android.widget.FrameLayout"
        every { mockNode.viewIdResourceName } returns null
        every { mockNode.childCount } returns 1
        every { mockNode.getChild(0) } returns null

        // When: Detect framework with Unity package pattern
        val result = CrossPlatformDetector.detectFramework("com.unity3d.test.app", mockNode)

        // Then: Unity detected by package name
        assertEquals(AppFramework.UNITY, result)
    }

    @Test
    fun detectFramework_Unreal_ReturnsUnreal() {
        // Given: Node with Unreal signatures
        every { mockNode.className } returns "com.epicgames.ue4.GameActivity"
        every { mockNode.viewIdResourceName } returns null
        every { mockNode.childCount } returns 2
        every { mockNode.getChild(0) } returns null
        every { mockNode.getChild(1) } returns null

        // When: Detect framework
        val result = CrossPlatformDetector.detectFramework("com.epicgames.fortnite", mockNode)

        // Then: Unreal detected
        assertEquals(AppFramework.UNREAL, result)
    }

    @Test
    fun detectFramework_UnrealByPackageName_ReturnsUnreal() {
        // Given: Package name with Unreal pattern
        every { mockNode.className } returns "android.widget.FrameLayout"
        every { mockNode.viewIdResourceName } returns null
        every { mockNode.childCount } returns 2

        // When: Detect framework with Unreal package pattern
        val result = CrossPlatformDetector.detectFramework("com.unrealengine.test", mockNode)

        // Then: Unreal detected by package name
        assertEquals(AppFramework.UNREAL, result)
    }

    @Test
    fun detectFramework_ReactNative_ReturnsReactNative() {
        // Given: Node with React Native signatures
        every { mockNode.className } returns "com.facebook.react.ReactRootView"
        every { mockNode.viewIdResourceName } returns null
        every { mockNode.childCount } returns 0

        // When: Detect framework
        val result = CrossPlatformDetector.detectFramework("com.example.reactnative", mockNode)

        // Then: React Native detected
        assertEquals(AppFramework.REACT_NATIVE, result)
    }

    @Test
    fun detectFramework_Native_ReturnsNative() {
        // Given: Node with standard Android classes
        every { mockNode.className } returns "android.widget.LinearLayout"
        every { mockNode.viewIdResourceName } returns "com.example.app:id/main_layout"
        every { mockNode.childCount } returns 5

        // When: Detect framework
        val result = CrossPlatformDetector.detectFramework("com.example.app", mockNode)

        // Then: Native detected
        assertEquals(AppFramework.NATIVE, result)
    }

    @Test
    fun detectFramework_NullNode_ReturnsNative() {
        // Given: Null node

        // When: Detect framework with null node
        val result = CrossPlatformDetector.detectFramework("com.example.app", null)

        // Then: Native returned as fallback
        assertEquals(AppFramework.NATIVE, result)
    }

    @Test
    fun detectFramework_Xamarin_ReturnsXamarin() {
        // Given: Node with Xamarin signatures
        every { mockNode.className } returns "mono.android.view.View"
        every { mockNode.viewIdResourceName } returns null
        every { mockNode.childCount } returns 0

        // When: Detect framework
        val result = CrossPlatformDetector.detectFramework("com.xamarin.test", mockNode)

        // Then: Xamarin detected
        assertEquals(AppFramework.XAMARIN, result)
    }

    @Test
    fun detectFramework_Cordova_ReturnsCordova() {
        // Given: Node with Cordova signatures (WebView-based)
        every { mockNode.className } returns "android.webkit.WebView"
        every { mockNode.viewIdResourceName } returns null
        every { mockNode.childCount } returns 0

        // When: Detect framework with Cordova package pattern
        val result = CrossPlatformDetector.detectFramework("com.ionic.cordova.test", mockNode)

        // Then: Cordova detected
        assertEquals(AppFramework.CORDOVA, result)
    }

    // ============================================================
    // Framework Priority Tests (Unity/Unreal checked first)
    // ============================================================

    @Test
    fun detectFramework_UnityPriorityOverFlutter_ReturnsUnity() {
        // Given: Node with Unity signatures takes priority
        every { mockNode.className } returns "com.unity3d.player.UnityPlayer"
        every { mockNode.viewIdResourceName } returns "flutter_view"
        every { mockNode.childCount } returns 1
        every { mockNode.getChild(0) } returns null

        // When: Detect framework
        val result = CrossPlatformDetector.detectFramework("com.unity3d.app", mockNode)

        // Then: Unity detected (higher priority than Flutter)
        assertEquals(AppFramework.UNITY, result)
    }

    @Test
    fun detectFramework_UnrealPriorityOverReactNative_ReturnsUnreal() {
        // Given: Node with Unreal signatures takes priority
        every { mockNode.className } returns "com.epicgames.ue4.GameActivity"
        every { mockNode.viewIdResourceName } returns null
        every { mockNode.childCount } returns 2

        // When: Detect framework
        val result = CrossPlatformDetector.detectFramework("com.epicgames.test", mockNode)

        // Then: Unreal detected (higher priority than React Native)
        assertEquals(AppFramework.UNREAL, result)
    }

    // ============================================================
    // AppFramework Extension Function Tests
    // ============================================================

    @Test
    fun appFramework_needsAggressiveFallback_TrueForGameEngines() {
        // Given: Game engine frameworks
        val unityFramework = AppFramework.UNITY
        val unrealFramework = AppFramework.UNREAL
        val flutterFramework = AppFramework.FLUTTER
        val cordovaFramework = AppFramework.CORDOVA

        // When/Then: Verify aggressive fallback needed
        assertTrue(unityFramework.needsAggressiveFallback())
        assertTrue(unrealFramework.needsAggressiveFallback())
        assertTrue(flutterFramework.needsAggressiveFallback())
        assertTrue(cordovaFramework.needsAggressiveFallback())
    }

    @Test
    fun appFramework_needsModerateFallback_TrueForReactNative() {
        // Given: React Native framework
        val reactNativeFramework = AppFramework.REACT_NATIVE
        val xamarinFramework = AppFramework.XAMARIN

        // When/Then: Verify moderate fallback needed
        assertTrue(reactNativeFramework.needsModerateFallback())
        assertTrue(xamarinFramework.needsModerateFallback())
    }

    @Test
    fun appFramework_needsCoordinateTapping_TrueForUnity() {
        // Given: Unity framework
        val unityFramework = AppFramework.UNITY

        // When/Then: Verify coordinate tapping needed
        assertTrue(unityFramework.needsCoordinateTapping())
    }

    @Test
    fun appFramework_getMinLabelLength_LowerForGameEngines() {
        // Given: Game engine frameworks
        val unityFramework = AppFramework.UNITY
        val unrealFramework = AppFramework.UNREAL
        val nativeFramework = AppFramework.NATIVE

        // When: Get minimum label length
        val unityMinLength = unityFramework.getMinLabelLength(5)
        val unrealMinLength = unrealFramework.getMinLabelLength(5)
        val nativeMinLength = nativeFramework.getMinLabelLength(5)

        // Then: Game engines have lower thresholds
        assertEquals(1, unityMinLength)
        assertEquals(1, unrealMinLength)
        assertEquals(5, nativeMinLength)
    }
}
