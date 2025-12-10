package com.augmentalis.ava.core.domain.resolution

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.ava.core.domain.model.AppPreference
import com.augmentalis.ava.core.domain.model.AppResolution
import com.augmentalis.ava.core.domain.model.InstalledApp
import com.augmentalis.ava.core.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [PreferencePromptManager].
 *
 * Tests cover:
 * - Prompt showing and queuing
 * - App selection handling
 * - Prompt dismissal
 * - Queue management
 * - State tracking
 *
 * Part of Intelligent Resolution System (Chapter 71)
 *
 * Author: Manoj Jhawar
 */
@RunWith(AndroidJUnit4::class)
class PreferencePromptManagerTest {

    private lateinit var context: Context
    private lateinit var mockRepository: MockAppPreferencesRepository
    private lateinit var appResolverService: AppResolverService
    private lateinit var manager: PreferencePromptManager

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        mockRepository = MockAppPreferencesRepository()
        appResolverService = AppResolverService(context, mockRepository)
        manager = PreferencePromptManager(appResolverService)
    }

    @Test
    fun initialState_noPromptShowing() {
        // Assert
        assertFalse(manager.isPromptShowing())
        assertEquals(0, manager.getQueueSize())
    }

    @Test
    fun requestAppSelection_showsPrompt() = runBlocking {
        // Setup
        val resolution = createMultipleAvailableResolution()

        // Act
        val result = manager.requestAppSelection(resolution) { _, _ -> }

        // Assert
        assertTrue(result)
        assertTrue(manager.isPromptShowing())
    }

    @Test
    fun requestAppSelection_queuesWhenPromptShowing() = runBlocking {
        // Setup
        val resolution1 = createMultipleAvailableResolution("email")
        val resolution2 = createMultipleAvailableResolution("sms")

        // Act - show first, queue second
        manager.requestAppSelection(resolution1) { _, _ -> }
        manager.requestAppSelection(resolution2) { _, _ -> }

        // Assert
        assertTrue(manager.isPromptShowing())
        assertEquals(1, manager.getQueueSize())
    }

    @Test
    fun currentPrompt_returnsActivePrompt() = runBlocking {
        // Setup
        val resolution = createMultipleAvailableResolution("email")

        // Act
        manager.requestAppSelection(resolution) { _, _ -> }

        // Assert
        val prompt = manager.currentPrompt.first()
        assertNotNull(prompt)
        assertEquals("email", prompt.capability)
        assertEquals("Email", prompt.capabilityDisplayName)
    }

    @Test
    fun onAppSelected_clearsCurrentPrompt() = runBlocking {
        // Setup
        val resolution = createMultipleAvailableResolution()
        manager.requestAppSelection(resolution) { _, _ -> }

        val selectedApp = InstalledApp(
            packageName = "com.google.android.gm",
            appName = "Gmail"
        )

        // Act
        manager.onAppSelected(selectedApp, remember = true)

        // Assert
        assertFalse(manager.isPromptShowing())
    }

    @Test
    fun onAppSelected_savesPreference() = runBlocking {
        // Setup
        val resolution = createMultipleAvailableResolution()
        manager.requestAppSelection(resolution) { _, _ -> }

        val selectedApp = InstalledApp(
            packageName = "com.google.android.gm",
            appName = "Gmail"
        )

        // Act
        manager.onAppSelected(selectedApp, remember = true)

        // Assert
        val saved = mockRepository.getPreferredApp("email")
        assertNotNull(saved)
        assertEquals("com.google.android.gm", saved.packageName)
    }

    @Test
    fun onAppSelected_showsNextQueuedPrompt() = runBlocking {
        // Setup
        val resolution1 = createMultipleAvailableResolution("email")
        val resolution2 = createMultipleAvailableResolution("sms")

        manager.requestAppSelection(resolution1) { _, _ -> }
        manager.requestAppSelection(resolution2) { _, _ -> }

        val selectedApp = InstalledApp(
            packageName = "com.google.android.gm",
            appName = "Gmail"
        )

        // Act - select for first prompt
        manager.onAppSelected(selectedApp, remember = true)

        // Assert - second prompt should now be showing
        assertTrue(manager.isPromptShowing())
        val currentPrompt = manager.currentPrompt.first()
        assertNotNull(currentPrompt)
        assertEquals("sms", currentPrompt.capability)
    }

    @Test
    fun onPromptDismissed_clearsPrompt() = runBlocking {
        // Setup
        val resolution = createMultipleAvailableResolution()
        manager.requestAppSelection(resolution) { _, _ -> }

        // Act
        manager.onPromptDismissed()

        // Assert
        assertFalse(manager.isPromptShowing())
    }

    @Test
    fun onPromptDismissed_showsNextQueuedPrompt() = runBlocking {
        // Setup
        val resolution1 = createMultipleAvailableResolution("email")
        val resolution2 = createMultipleAvailableResolution("sms")

        manager.requestAppSelection(resolution1) { _, _ -> }
        manager.requestAppSelection(resolution2) { _, _ -> }

        // Act - dismiss first prompt
        manager.onPromptDismissed()

        // Assert - second prompt should now be showing
        assertTrue(manager.isPromptShowing())
        val currentPrompt = manager.currentPrompt.first()
        assertEquals("sms", currentPrompt?.capability)
    }

    @Test
    fun clearAllPrompts_clearsEverything() = runBlocking {
        // Setup
        val resolution1 = createMultipleAvailableResolution("email")
        val resolution2 = createMultipleAvailableResolution("sms")
        val resolution3 = createMultipleAvailableResolution("music")

        manager.requestAppSelection(resolution1) { _, _ -> }
        manager.requestAppSelection(resolution2) { _, _ -> }
        manager.requestAppSelection(resolution3) { _, _ -> }

        // Act
        manager.clearAllPrompts()

        // Assert
        assertFalse(manager.isPromptShowing())
        assertEquals(0, manager.getQueueSize())
    }

    @Test
    fun getQueueSize_tracksQueuedPrompts() = runBlocking {
        // Setup & Act
        assertEquals(0, manager.getQueueSize())

        manager.requestAppSelection(createMultipleAvailableResolution("email")) { _, _ -> }
        assertEquals(0, manager.getQueueSize()) // First shows immediately

        manager.requestAppSelection(createMultipleAvailableResolution("sms")) { _, _ -> }
        assertEquals(1, manager.getQueueSize())

        manager.requestAppSelection(createMultipleAvailableResolution("music")) { _, _ -> }
        assertEquals(2, manager.getQueueSize())
    }

    @Test
    fun callbackInvoked_onAppSelection() = runBlocking {
        // Setup
        var callbackInvoked = false
        var selectedPackage: String? = null

        val resolution = createMultipleAvailableResolution()
        manager.requestAppSelection(resolution) { app, _ ->
            callbackInvoked = true
            selectedPackage = app.packageName
        }

        val selectedApp = InstalledApp(
            packageName = "com.google.android.gm",
            appName = "Gmail"
        )

        // Act
        manager.onAppSelected(selectedApp, remember = true)

        // Assert
        assertTrue(callbackInvoked)
        assertEquals("com.google.android.gm", selectedPackage)
    }

    // ==================== Helper Methods ====================

    private fun createMultipleAvailableResolution(
        capability: String = "email"
    ): AppResolution.MultipleAvailable {
        val displayName = when (capability) {
            "email" -> "Email"
            "sms" -> "Text Messages"
            "music" -> "Music Player"
            else -> capability.replaceFirstChar { it.uppercase() }
        }

        return AppResolution.MultipleAvailable(
            capability = capability,
            capabilityDisplayName = displayName,
            apps = listOf(
                InstalledApp("com.google.android.gm", "Gmail"),
                InstalledApp("com.microsoft.office.outlook", "Outlook")
            ),
            recommendedIndex = 0
        )
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
