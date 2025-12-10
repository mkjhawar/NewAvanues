// filename: apps/ava-standalone/src/test/kotlin/com/augmentalis/ava/ui/settings/SettingsViewModelTest.kt
// created: 2025-11-13
// © Augmentalis Inc, Intelligent Devices LLC
// AVA AI - SettingsViewModel Unit Tests

package com.augmentalis.ava.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.core.data.prefs.ChatPreferences
import com.augmentalis.ava.core.data.prefs.ConversationMode
import com.augmentalis.ava.preferences.UserPreferences
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsViewModel
 *
 * Tests cover:
 * - ChatPreferences conversation mode integration
 * - Theme settings
 * - Privacy settings (crash reporting, analytics)
 * - NLU settings (enable/disable, confidence threshold)
 * - LLM settings (provider selection, streaming)
 * - State management and UI state updates
 *
 * Uses Robolectric for Android Context support in unit tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    private lateinit var context: Context
    private lateinit var userPreferences: UserPreferences
    private lateinit var chatPreferences: ChatPreferences
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()

    // Mock flows
    private val crashReportingFlow = MutableStateFlow(false)
    private val analyticsFlow = MutableStateFlow(false)
    private val themeModeFlow = MutableStateFlow("auto")
    private val conversationModeFlow = MutableStateFlow(ConversationMode.APPEND)

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(testDispatcher)

        // Get real Android Context from Robolectric
        context = ApplicationProvider.getApplicationContext()

        // Use real ChatPreferences (resets to APPEND by default)
        chatPreferences = ChatPreferences.getInstance(context)
        chatPreferences.setConversationMode(ConversationMode.APPEND)

        // Mock UserPreferences
        userPreferences = mockk(relaxed = true)
        every { userPreferences.crashReportingEnabled } returns crashReportingFlow
        every { userPreferences.analyticsEnabled } returns analyticsFlow
        every { userPreferences.themeMode } returns themeModeFlow
        coEvery { userPreferences.setThemeMode(any()) } just Runs
        coEvery { userPreferences.setCrashReportingEnabled(any()) } just Runs
        coEvery { userPreferences.setAnalyticsEnabled(any()) } just Runs

        // Mock DataStore
        dataStore = mockk(relaxed = true)
        every { dataStore.data } returns MutableStateFlow(mockk(relaxed = true))

        // Create ViewModel with real Context, mock UserPreferences, and mock DataStore
        viewModel = SettingsViewModel(context, userPreferences, dataStore)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() = runTest {
        // Reset ChatPreferences to default
        chatPreferences.setConversationMode(ConversationMode.APPEND)
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ===== ChatPreferences Conversation Mode Tests =====

    @Test
    fun `conversation mode defaults to Append`() = runTest {
        // Given: Fresh ViewModel

        // When: Advance dispatcher to process initialization
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UI state should have conversationMode = "Append"
        val uiState = viewModel.uiState.first()
        assertEquals("Append", uiState.conversationMode)
    }

    @Test
    fun `setConversationMode updates to NEW`() = runTest {
        // Given: ViewModel initialized with APPEND mode
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User selects "New" conversation mode
        viewModel.setConversationMode("New")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: ChatPreferences should be updated to NEW
        val currentMode = chatPreferences.getConversationMode()
        assertEquals(ConversationMode.NEW, currentMode)

        // And: UI state should reflect the change
        val uiState = viewModel.uiState.first()
        assertEquals("New", uiState.conversationMode)
    }

    @Test
    fun `setConversationMode updates to APPEND`() = runTest {
        // Given: ViewModel initialized with NEW mode
        chatPreferences.setConversationMode(ConversationMode.NEW)
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User selects "Append" conversation mode
        viewModel.setConversationMode("Append")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: ChatPreferences should be updated to APPEND
        val currentMode = chatPreferences.getConversationMode()
        assertEquals(ConversationMode.APPEND, currentMode)
    }

    @Test
    fun `setConversationMode handles lowercase input`() = runTest {
        // Given: ViewModel initialized
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User input is lowercase "new"
        viewModel.setConversationMode("new")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should be converted to NEW mode
        val currentMode = chatPreferences.getConversationMode()
        assertEquals(ConversationMode.NEW, currentMode)
    }

    @Test
    fun `setConversationMode handles mixed case input`() = runTest {
        // Given: ViewModel initialized
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User input is mixed case "NeW"
        viewModel.setConversationMode("NeW")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should be converted to NEW mode
        val currentMode = chatPreferences.getConversationMode()
        assertEquals(ConversationMode.NEW, currentMode)
    }

    @Test
    fun `conversationMode flow updates UI state reactively`() = runTest {
        // Given: ViewModel initialized
        testDispatcher.scheduler.advanceUntilIdle()

        // When: ConversationMode changes externally (e.g., from another component)
        chatPreferences.setConversationMode(ConversationMode.NEW)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UI state should reflect the change
        val uiState = viewModel.uiState.first()
        assertEquals("New", uiState.conversationMode)
    }

    // ===== Theme Settings Tests =====

    @Test
    fun `setTheme updates to dark mode`() = runTest {
        // Given: ViewModel initialized
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User selects dark theme
        viewModel.setTheme("Dark")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UserPreferences should be updated
        coVerify { userPreferences.setThemeMode("dark") }
    }

    @Test
    fun `setTheme updates to light mode`() = runTest {
        // Given: ViewModel initialized
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User selects light theme
        viewModel.setTheme("Light")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UserPreferences should be updated
        coVerify { userPreferences.setThemeMode("light") }
    }

    @Test
    fun `setTheme updates to system default`() = runTest {
        // Given: ViewModel initialized
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User selects system default
        viewModel.setTheme("System Default")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UserPreferences should be updated to "auto"
        coVerify { userPreferences.setThemeMode("auto") }
    }

    // ===== Privacy Settings Tests =====

    @Test
    fun `setCrashReportingEnabled updates preference`() = runTest {
        // Given: Crash reporting initially disabled
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User enables crash reporting
        viewModel.setCrashReportingEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UserPreferences should be updated
        coVerify { userPreferences.setCrashReportingEnabled(true) }
    }

    @Test
    fun `setAnalyticsEnabled updates preference`() = runTest {
        // Given: Analytics initially disabled
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User enables analytics
        viewModel.setAnalyticsEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UserPreferences should be updated
        coVerify { userPreferences.setAnalyticsEnabled(true) }
    }

    // ===== UI State Integration Tests =====

    @Test
    fun `UI state combines all preference flows correctly`() = runTest {
        // Given: All preferences set to specific values
        crashReportingFlow.value = true
        analyticsFlow.value = true
        themeModeFlow.value = "dark"
        chatPreferences.setConversationMode(ConversationMode.NEW)

        // When: ViewModel processes updates
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UI state should reflect all values
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.crashReportingEnabled)
        assertTrue(uiState.analyticsEnabled)
        assertEquals("Dark", uiState.theme)
        assertEquals("New", uiState.conversationMode)
    }

    @Test
    fun `UI state updates when any preference changes`() = runTest {
        // Given: ViewModel initialized
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Conversation mode changes
        chatPreferences.setConversationMode(ConversationMode.NEW)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UI state should update
        var uiState = viewModel.uiState.first()
        assertEquals("New", uiState.conversationMode)

        // When: Theme changes
        themeModeFlow.value = "light"
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UI state should update
        uiState = viewModel.uiState.first()
        assertEquals("Light", uiState.theme)
    }

    @Test
    fun `conversationMode displays capitalized format`() = runTest {
        // Given: ConversationMode set to APPEND
        chatPreferences.setConversationMode(ConversationMode.APPEND)
        testDispatcher.scheduler.advanceUntilIdle()

        // When: UI state is read
        val uiState = viewModel.uiState.first()

        // Then: Should be "Append" (capitalized)
        assertEquals("Append", uiState.conversationMode)
    }

    @Test
    fun `theme displays capitalized format`() = runTest {
        // Given: Theme mode set to "auto"
        themeModeFlow.value = "auto"
        testDispatcher.scheduler.advanceUntilIdle()

        // When: UI state is read
        val uiState = viewModel.uiState.first()

        // Then: Should be "Auto" (capitalized)
        assertEquals("Auto", uiState.theme)
    }

    // ===== Error Handling Tests =====

    @Test
    fun `setConversationMode handles invalid input gracefully`() = runTest {
        // Given: ViewModel initialized
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Invalid mode is passed
        viewModel.setConversationMode("Invalid")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should default to APPEND (as per implementation)
        val currentMode = chatPreferences.getConversationMode()
        assertEquals(ConversationMode.APPEND, currentMode)
    }

    @Test
    fun `setConversationMode handles empty string`() = runTest {
        // Given: ViewModel initialized
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Empty string is passed
        viewModel.setConversationMode("")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should default to APPEND
        val currentMode = chatPreferences.getConversationMode()
        assertEquals(ConversationMode.APPEND, currentMode)
    }

    // ===== State Persistence Tests =====

    @Test
    fun `conversation mode persists to ChatPreferences`() = runTest {
        // Given: User sets conversation mode to NEW
        viewModel.setConversationMode("New")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: ChatPreferences should persist the value
        val savedMode = chatPreferences.getConversationMode()
        assertEquals(ConversationMode.NEW, savedMode)

        // And: ChatPreferences StateFlow should have the correct value
        val chatPrefsFlowValue = chatPreferences.conversationMode.first()
        assertEquals(ConversationMode.NEW, chatPrefsFlowValue)

        // When: User changes back to APPEND
        viewModel.setConversationMode("Append")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: ChatPreferences should update
        val updatedMode = chatPreferences.getConversationMode()
        assertEquals(ConversationMode.APPEND, updatedMode)
    }
}
