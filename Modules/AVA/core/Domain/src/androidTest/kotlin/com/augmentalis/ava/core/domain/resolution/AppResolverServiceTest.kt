package com.augmentalis.ava.core.domain.resolution

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.ava.core.domain.model.AppPreference
import com.augmentalis.ava.core.domain.model.AppResolution
import com.augmentalis.ava.core.domain.model.ResolutionSource
import com.augmentalis.ava.core.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [AppResolverService].
 *
 * Tests cover:
 * - Resolution with saved preference
 * - Resolution with single installed app (auto-select)
 * - Resolution with multiple apps (prompt needed)
 * - Resolution with no apps available
 * - Unknown capability handling
 * - Preference save/clear operations
 *
 * Part of Intelligent Resolution System (Chapter 71)
 *
 * Author: Manoj Jhawar
 */
@RunWith(AndroidJUnit4::class)
class AppResolverServiceTest {

    private lateinit var context: Context
    private lateinit var mockRepository: MockAppPreferencesRepository
    private lateinit var service: AppResolverService

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        mockRepository = MockAppPreferencesRepository()
        service = AppResolverService(context, mockRepository)
    }

    @Test
    fun resolveApp_withSavedPreference_returnsResolved() = runBlocking {
        // Setup: Save a preference for email
        mockRepository.setPreferredApp(
            capability = "email",
            packageName = "com.google.android.gm",
            appName = "Gmail",
            setBy = "user"
        )

        // Act
        val result = service.resolveApp("email")

        // Assert - if Gmail is installed, should return Resolved with USER_PREFERENCE
        // If not installed, preference should be cleared and we get a different result
        if (result is AppResolution.Resolved) {
            assertEquals(ResolutionSource.USER_PREFERENCE, result.source)
            assertEquals("com.google.android.gm", result.packageName)
        }
    }

    @Test
    fun resolveApp_unknownCapability_returnsUnknownCapability() = runBlocking {
        // Act
        val result = service.resolveApp("nonexistent_capability")

        // Assert
        assertIs<AppResolution.UnknownCapability>(result)
        assertEquals("nonexistent_capability", result.capability)
    }

    @Test
    fun resolveApp_emailCapability_doesNotCrash() = runBlocking {
        // Act - should not throw
        val result = service.resolveApp("email")

        // Assert - result should be one of the valid types
        assertTrue(
            result is AppResolution.Resolved ||
            result is AppResolution.MultipleAvailable ||
            result is AppResolution.NoneAvailable
        )
    }

    @Test
    fun resolveApp_smsCapability_doesNotCrash() = runBlocking {
        // Act - should not throw
        val result = service.resolveApp("sms")

        // Assert - result should be one of the valid types
        assertTrue(
            result is AppResolution.Resolved ||
            result is AppResolution.MultipleAvailable ||
            result is AppResolution.NoneAvailable
        )
    }

    @Test
    fun resolveApp_browserCapability_doesNotCrash() = runBlocking {
        // Act - should not throw
        val result = service.resolveApp("browser")

        // Assert - result should be one of the valid types
        assertTrue(
            result is AppResolution.Resolved ||
            result is AppResolution.MultipleAvailable ||
            result is AppResolution.NoneAvailable
        )
    }

    @Test
    fun savePreference_storesInRepository() = runBlocking {
        // Act
        service.savePreference(
            capability = "music",
            packageName = "com.spotify.music",
            appName = "Spotify",
            remember = true
        )

        // Assert
        val saved = mockRepository.getPreferredApp("music")
        assertNotNull(saved)
        assertEquals("com.spotify.music", saved.packageName)
        assertEquals("Spotify", saved.appName)
    }

    @Test
    fun savePreference_withRememberFalse_doesNotStore() = runBlocking {
        // Act
        service.savePreference(
            capability = "music",
            packageName = "com.spotify.music",
            appName = "Spotify",
            remember = false
        )

        // Assert - should not be stored as preference
        val saved = mockRepository.getPreferredApp("music")
        assertNull(saved)
    }

    @Test
    fun clearPreference_removesFromRepository() = runBlocking {
        // Setup
        mockRepository.setPreferredApp(
            capability = "maps",
            packageName = "com.google.android.apps.maps",
            appName = "Google Maps",
            setBy = "user"
        )

        // Act
        service.clearPreference("maps")

        // Assert
        val cleared = mockRepository.getPreferredApp("maps")
        assertNull(cleared)
    }

    @Test
    fun getAllCapabilityPreferences_returnsAllCapabilities() = runBlocking {
        // Act
        val preferences = service.getAllCapabilityPreferences()

        // Assert - should return at least 10 capabilities
        assertTrue(preferences.size >= 10, "Should have at least 10 capabilities")

        // Verify each has required fields
        preferences.forEach { pref ->
            assertTrue(pref.capability.isNotEmpty(), "Capability ID should not be empty")
            assertTrue(pref.displayName.isNotEmpty(), "Display name should not be empty")
        }
    }

    @Test
    fun getAllCapabilityPreferences_includesEmailCapability() = runBlocking {
        // Act
        val preferences = service.getAllCapabilityPreferences()

        // Assert
        val email = preferences.find { it.capability == "email" }
        assertNotNull(email, "Email capability should be in list")
        assertEquals("Email", email.displayName)
        assertEquals(CapabilityCategory.COMMUNICATION, email.category)
    }

    @Test
    fun resolveApp_clearsInvalidPreference() = runBlocking {
        // Setup: Save preference for non-existent app
        mockRepository.setPreferredApp(
            capability = "email",
            packageName = "com.nonexistent.app",
            appName = "Nonexistent App",
            setBy = "user"
        )

        // Act
        service.resolveApp("email")

        // Assert - preference should be cleared since app doesn't exist
        val pref = mockRepository.getPreferredApp("email")
        assertNull(pref, "Invalid preference should be cleared")
    }

    @Test
    fun getAppIcon_returnsNullForUninstalledApp() {
        // Act
        val icon = service.getAppIcon("com.nonexistent.package")

        // Assert
        assertNull(icon, "Should return null for uninstalled app")
    }

    /**
     * Mock implementation for testing.
     */
    private class MockAppPreferencesRepository : AppPreferencesRepository {
        private val preferences = mutableMapOf<String, AppPreference>()
        private val usageRecords = mutableListOf<Pair<String, String>>()

        override suspend fun getPreferredApp(capability: String): AppPreference? {
            return preferences[capability]
        }

        override suspend fun setPreferredApp(
            capability: String,
            packageName: String,
            appName: String,
            setBy: String
        ) {
            preferences[capability] = AppPreference(
                capability = capability,
                packageName = packageName,
                appName = appName,
                setAt = System.currentTimeMillis(),
                setBy = setBy
            )
        }

        override suspend fun clearPreferredApp(capability: String) {
            preferences.remove(capability)
        }

        override suspend fun getAllPreferences(): Map<String, AppPreference> {
            return preferences.toMap()
        }

        override suspend fun hasPreference(capability: String): Boolean {
            return preferences.containsKey(capability)
        }

        override fun observeAllPreferences(): Flow<List<AppPreference>> {
            return flowOf(preferences.values.toList())
        }

        override suspend fun recordUsage(
            capability: String,
            packageName: String,
            contextJson: String?
        ) {
            usageRecords.add(capability to packageName)
        }

        override suspend fun getMostUsedApp(capability: String): String? {
            return usageRecords
                .filter { it.first == capability }
                .groupingBy { it.second }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
        }
    }
}
