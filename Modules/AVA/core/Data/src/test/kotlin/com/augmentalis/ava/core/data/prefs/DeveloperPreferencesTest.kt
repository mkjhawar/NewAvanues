// filename: Universal/AVA/Core/Data/src/test/kotlin/com/augmentalis/ava/core/data/prefs/DeveloperPreferencesTest.kt
// created: 2025-11-25
// updated: 2025-12-18 (converted from Mockito to MockK)
// author: Testing Swarm Agent 1 - AVA AI Features 003 + 004
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for DeveloperPreferences (REQ-007)
 *
 * Tests:
 * - Flash mode should default to false
 * - setFlashModeEnabled should persist value
 * - clearAll should reset all preferences to defaults
 *
 * Uses MockK + Robolectric for Android unit testing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeveloperPreferencesTest {

    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: Preferences
    private lateinit var developerPreferences: DeveloperPreferences

    companion object {
        private val FLASH_MODE_ENABLED = booleanPreferencesKey("flash_mode_enabled")
        private val VERBOSE_LOGGING_ENABLED = booleanPreferencesKey("verbose_logging_enabled")
        private val SHOW_PERFORMANCE_METRICS = booleanPreferencesKey("show_performance_metrics")
    }

    @Before
    fun setup() {
        // Mock Context
        context = mockk(relaxed = true) {
            every { applicationContext } returns this
        }

        // Mock Preferences to return default values
        preferences = mockk(relaxed = true) {
            every { get(FLASH_MODE_ENABLED) } returns null
            every { get(VERBOSE_LOGGING_ENABLED) } returns null
            every { get(SHOW_PERFORMANCE_METRICS) } returns null
        }

        // Mock DataStore to return preferences flow
        dataStore = mockk(relaxed = true) {
            every { data } returns flowOf(preferences)
        }

        // Create DeveloperPreferences directly with mock Context
        developerPreferences = DeveloperPreferences(context)
    }

    // ==================== Flash Mode Tests ====================

    /**
     * Test 1: flash mode should default to false
     */
    @Test
    fun `flash mode should default to false`() = runTest {
        // Given: DeveloperPreferences initialized with default values
        // (setup already handles this)

        // When: Read flash mode preference
        val value = developerPreferences.isFlashModeEnabled.first()

        // Then: Should default to false
        assertFalse(value, "Flash mode should default to false")
    }

    /**
     * Test 2: setFlashModeEnabled should persist value
     */
    @Test
    fun `setFlashModeEnabled should persist value`() = runTest {
        // Given: DeveloperPreferences initialized
        // Mock preferences to return true after setting
        every { preferences[FLASH_MODE_ENABLED] } returns true
        every { dataStore.data } returns flowOf(preferences)

        // When: Set flash mode to true
        developerPreferences.setFlashModeEnabled(true)

        // Then: Should persist and return true
        val value = developerPreferences.isFlashModeEnabled.first()
        assertTrue(value, "Flash mode should be enabled after setting to true")

        // Mock preferences to return false after second setting
        every { preferences[FLASH_MODE_ENABLED] } returns false
        every { dataStore.data } returns flowOf(preferences)

        // When: Set flash mode to false
        developerPreferences.setFlashModeEnabled(false)

        // Then: Should persist and return false
        val valueAfterDisable = developerPreferences.isFlashModeEnabled.first()
        assertFalse(valueAfterDisable, "Flash mode should be disabled after setting to false")
    }

    /**
     * Test 3: clearAll should reset all preferences to defaults
     */
    @Test
    fun `clearAll should reset all preferences to defaults`() = runTest {
        // Given: Set flash mode to true
        every { preferences[FLASH_MODE_ENABLED] } returns true
        every { dataStore.data } returns flowOf(preferences)
        developerPreferences.setFlashModeEnabled(true)

        // Verify flash mode is enabled
        val beforeClear = developerPreferences.isFlashModeEnabled.first()
        assertTrue(beforeClear, "Flash mode should be enabled before clearing")

        // When: Clear all preferences
        developerPreferences.clearAll()

        // Then: Flash mode should reset to default (false)
        // Mock empty preferences after clear
        every { preferences[FLASH_MODE_ENABLED] } returns null
        every { dataStore.data } returns flowOf(emptyPreferences())

        val afterClear = developerPreferences.isFlashModeEnabled.first()
        assertFalse(afterClear, "Flash mode should reset to false after clearAll()")
    }

    // ==================== Future Tests (Verbose Logging, Performance Metrics) ====================

    /**
     * Additional test: Verbose logging should default to false
     */
    @Test
    fun `verbose logging should default to false`() = runTest {
        val value = developerPreferences.isVerboseLoggingEnabled.first()
        assertFalse(value, "Verbose logging should default to false")
    }

    /**
     * Additional test: Performance metrics should default to false
     */
    @Test
    fun `performance metrics should default to false`() = runTest {
        val value = developerPreferences.isShowPerformanceMetrics.first()
        assertFalse(value, "Performance metrics should default to false")
    }
}
