package com.augmentalis.ava.core.domain.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for App Resolution model classes.
 *
 * Tests cover:
 * - AppResolution sealed class variants
 * - ResolutionSource enum values
 * - InstalledApp data class
 * - KnownApp data class
 * - AppPlatform enum values
 * - AppPreference data class
 *
 * Part of Intelligent Resolution System (Chapter 71)
 *
 * Author: Manoj Jhawar
 */
@RunWith(AndroidJUnit4::class)
class AppResolutionTest {

    // ==================== AppResolution Tests ====================

    @Test
    fun resolved_containsCorrectProperties() {
        val resolution = AppResolution.Resolved(
            packageName = "com.google.android.gm",
            appName = "Gmail",
            source = ResolutionSource.USER_PREFERENCE
        )

        assertEquals("com.google.android.gm", resolution.packageName)
        assertEquals("Gmail", resolution.appName)
        assertEquals(ResolutionSource.USER_PREFERENCE, resolution.source)
    }

    @Test
    fun multipleAvailable_containsCorrectProperties() {
        val apps = listOf(
            InstalledApp("com.google.android.gm", "Gmail"),
            InstalledApp("com.microsoft.office.outlook", "Outlook")
        )

        val resolution = AppResolution.MultipleAvailable(
            capability = "email",
            capabilityDisplayName = "Email",
            apps = apps,
            recommendedIndex = 0
        )

        assertEquals("email", resolution.capability)
        assertEquals("Email", resolution.capabilityDisplayName)
        assertEquals(2, resolution.apps.size)
        assertEquals(0, resolution.recommendedIndex)
    }

    @Test
    fun noneAvailable_containsCorrectProperties() {
        val suggestedApps = listOf(
            KnownApp("com.spotify.music", "Spotify", AppPlatform.ANDROID)
        )

        val resolution = AppResolution.NoneAvailable(
            capability = "music",
            suggestedApps = suggestedApps
        )

        assertEquals("music", resolution.capability)
        assertEquals(1, resolution.suggestedApps.size)
    }

    @Test
    fun unknownCapability_containsCapabilityId() {
        val resolution = AppResolution.UnknownCapability("invalid_cap")

        assertEquals("invalid_cap", resolution.capability)
    }

    @Test
    fun sealedClass_allVariantsCovered() {
        val resolved: AppResolution = AppResolution.Resolved("pkg", "App", ResolutionSource.AUTO_DETECTED)
        val multiple: AppResolution = AppResolution.MultipleAvailable("cap", "Cap", emptyList())
        val none: AppResolution = AppResolution.NoneAvailable("cap", emptyList())
        val unknown: AppResolution = AppResolution.UnknownCapability("cap")

        // Verify type checking works
        assertIs<AppResolution.Resolved>(resolved)
        assertIs<AppResolution.MultipleAvailable>(multiple)
        assertIs<AppResolution.NoneAvailable>(none)
        assertIs<AppResolution.UnknownCapability>(unknown)
    }

    // ==================== ResolutionSource Tests ====================

    @Test
    fun resolutionSource_hasAllExpectedValues() {
        val sources = ResolutionSource.entries

        assertEquals(3, sources.size)
        assertTrue(sources.contains(ResolutionSource.USER_PREFERENCE))
        assertTrue(sources.contains(ResolutionSource.AUTO_DETECTED))
        assertTrue(sources.contains(ResolutionSource.USAGE_PATTERN))
    }

    // ==================== InstalledApp Tests ====================

    @Test
    fun installedApp_equalityWorks() {
        val app1 = InstalledApp("com.google.android.gm", "Gmail")
        val app2 = InstalledApp("com.google.android.gm", "Gmail")
        val app3 = InstalledApp("com.microsoft.outlook", "Outlook")

        assertEquals(app1, app2)
        assertNotEquals(app1, app3)
    }

    @Test
    fun installedApp_optionalIconUri() {
        val appWithIcon = InstalledApp("pkg", "App", iconUri = "content://icon")
        val appWithoutIcon = InstalledApp("pkg", "App")

        assertEquals("content://icon", appWithIcon.iconUri)
        assertEquals(null, appWithoutIcon.iconUri)
    }

    // ==================== KnownApp Tests ====================

    @Test
    fun knownApp_containsAllProperties() {
        val app = KnownApp(
            packageName = "com.spotify.music",
            displayName = "Spotify",
            platform = AppPlatform.ANDROID,
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.spotify.music"
        )

        assertEquals("com.spotify.music", app.packageName)
        assertEquals("Spotify", app.displayName)
        assertEquals(AppPlatform.ANDROID, app.platform)
        assertEquals("https://play.google.com/store/apps/details?id=com.spotify.music", app.playStoreUrl)
    }

    @Test
    fun knownApp_playStoreUrlIsOptional() {
        val app = KnownApp("pkg", "App", AppPlatform.BOTH)

        assertEquals(null, app.playStoreUrl)
    }

    // ==================== AppPlatform Tests ====================

    @Test
    fun appPlatform_hasAllExpectedValues() {
        val platforms = AppPlatform.entries

        assertEquals(3, platforms.size)
        assertTrue(platforms.contains(AppPlatform.ANDROID))
        assertTrue(platforms.contains(AppPlatform.IOS))
        assertTrue(platforms.contains(AppPlatform.BOTH))
    }

    // ==================== AppPreference Tests ====================

    @Test
    fun appPreference_containsAllProperties() {
        val preference = AppPreference(
            capability = "email",
            packageName = "com.google.android.gm",
            appName = "Gmail",
            setAt = 1733400000000L,
            setBy = "user"
        )

        assertEquals("email", preference.capability)
        assertEquals("com.google.android.gm", preference.packageName)
        assertEquals("Gmail", preference.appName)
        assertEquals(1733400000000L, preference.setAt)
        assertEquals("user", preference.setBy)
    }

    @Test
    fun appPreference_setByCanBeAuto() {
        val preference = AppPreference(
            capability = "music",
            packageName = "com.spotify.music",
            appName = "Spotify",
            setAt = System.currentTimeMillis(),
            setBy = "auto"
        )

        assertEquals("auto", preference.setBy)
    }

    // ==================== CapabilityPreference Tests ====================

    @Test
    fun capabilityPreference_withSelectedApp() {
        val selectedApp = InstalledApp("com.google.android.gm", "Gmail")
        val availableApps = listOf(
            selectedApp,
            InstalledApp("com.microsoft.outlook", "Outlook")
        )

        val pref = CapabilityPreference(
            capability = "email",
            displayName = "Email",
            selectedApp = selectedApp,
            availableApps = availableApps,
            canChange = true
        )

        assertEquals("email", pref.capability)
        assertEquals("Email", pref.displayName)
        assertEquals(selectedApp, pref.selectedApp)
        assertEquals(2, pref.availableApps.size)
        assertTrue(pref.canChange)
    }

    @Test
    fun capabilityPreference_withNoSelection() {
        val pref = CapabilityPreference(
            capability = "music",
            displayName = "Music Player",
            selectedApp = null,
            availableApps = emptyList(),
            canChange = false
        )

        assertEquals(null, pref.selectedApp)
        assertEquals(false, pref.canChange)
    }
}
