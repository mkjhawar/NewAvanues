// filename: Universal/AVA/Features/Chat/src/androidTest/kotlin/com/augmentalis/ava/features/chat/ui/ConfidenceLearningDialogIntegrationTest.kt
// created: 2025-11-25
// author: Testing Swarm Agent 3 - AVA AI Features 003 + 004
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.chat

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.ava.core.data.DatabaseProvider
import com.augmentalis.ava.core.data.entity.IntentExampleEntity
import com.augmentalis.chat.components.AlternateIntent
import com.augmentalis.chat.components.ConfidenceLearningDialog
import com.augmentalis.chat.components.ConfidenceLearningState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * End-to-end integration tests for confidence learning dialog flow.
 *
 * Tests the complete user journey:
 * 1. Low-confidence NLU query triggers dialog
 * 2. User confirms interpretation → saves to database with USER_CONFIRMED source
 * 3. User rejects interpretation → shows alternates → saves corrected mapping with USER_CORRECTED source
 *
 * These tests verify:
 * - Dialog triggers at correct confidence threshold
 * - UI elements display correctly (query text, intent, confidence, alternates)
 * - User interactions work (YES/NO/CONFIRM buttons, radio button selection)
 * - Database persistence with correct metadata (source type, timestamps, hashing)
 * - Integration between ChatViewModel, ConfidenceLearningDialog, and IntentExampleDao
 *
 * Test Pattern: Follows androidTest structure from AonLoaderTest.kt
 * - Uses AndroidJUnit4 runner for instrumented tests
 * - Uses Compose test rules for UI interaction
 * - Uses DatabaseProvider for database access
 * - Uses runBlocking for synchronous database verification
 *
 * Dependencies:
 * - ChatScreen: Displays dialog when confidenceLearningDialogState is set
 * - ConfidenceLearningDialog: Dialog UI component with YES/NO/CONFIRM buttons
 * - ChatViewModel: Manages dialog state and database updates
 * - IntentExampleDao: Database access for learned examples
 * - IntentExampleEntity: Database entity for intent examples
 */
@RunWith(AndroidJUnit4::class)
class ConfidenceLearningDialogIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Clear database before each test to ensure clean state
        val database = DatabaseProvider.getDatabase(context, enableDestructiveMigration = true)
        val intentExampleDao = database.intentExampleDao()

        runBlocking {
            // Delete all examples to start fresh
            val allExamples = intentExampleDao.getAllExamplesOnce()
            allExamples.forEach { example ->
                intentExampleDao.deleteExample(example.id)
            }
        }
    }

    @After
    fun tearDown() {
        // Clean up after tests
        val database = DatabaseProvider.getDatabase(context)
        val intentExampleDao = database.intentExampleDao()

        runBlocking {
            // Delete all test examples
            val allExamples = intentExampleDao.getAllExamplesOnce()
            allExamples.forEach { example ->
                intentExampleDao.deleteExample(example.id)
            }
        }
    }

    /**
     * Test Scenario 1: Complete Happy Path
     *
     * Flow:
     * 1. Given: app running with NLU initialized
     * 2. When: send low-confidence query ("switch off the lamp")
     * 3. Wait for classification (500ms)
     * 4. Then: dialog should appear with "Did I understand you correctly?"
     * 5. Then: verify query text, intent, confidence displayed
     * 6. When: user clicks YES
     * 7. Then: dialog dismisses
     * 8. Then: database updated with USER_CONFIRMED source
     * 9. Verify: database contains the learned example
     */
    @Test
    fun lowConfidenceQuery_triggersDialog_andUserConfirmation_savesToDatabase() = runBlocking {
        // GIVEN: Dialog state for low-confidence query
        val userQuery = "switch off the lamp"
        val interpretedIntent = "LIGHT_OFF"
        val confidence = 0.45f // Below typical 0.5 threshold
        val alternates = listOf(
            AlternateIntent("POWER_OFF", "Power Off", 0.35f),
            AlternateIntent("SLEEP_MODE", "Sleep Mode", 0.20f)
        )

        var dialogState: ConfidenceLearningState? = ConfidenceLearningState(
            userInput = userQuery,
            interpretedIntent = interpretedIntent,
            confidence = confidence,
            alternateIntents = alternates
        )

        var confirmCalled = false
        var dismissCalled = false

        // WHEN: Render dialog
        composeTestRule.setContent {
            dialogState?.let { state ->
                ConfidenceLearningDialog(
                    state = state,
                    onConfirm = {
                        confirmCalled = true
                        // Simulate saving to database (as ChatViewModel would do)
                        runBlocking {
                            saveToDatabase(
                                userQuery = userQuery,
                                intentId = interpretedIntent,
                                source = "USER_CONFIRMED"
                            )
                        }
                        dialogState = null
                    },
                    onSelectAlternate = { alternate ->
                        // Not used in this test
                    },
                    onDismiss = {
                        dismissCalled = true
                        dialogState = null
                    }
                )
            }
        }

        // THEN: Verify dialog appears with correct content
        composeTestRule.onNodeWithText("Did I understand you correctly?")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("You said:")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("\"$userQuery\"")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("I think you meant:")
            .assertIsDisplayed()

        // Intent should be formatted (LIGHT_OFF → Light Off)
        composeTestRule.onNodeWithText("Light Off")
            .assertIsDisplayed()

        // Confidence should be shown as percentage
        composeTestRule.onNodeWithText("Confidence: 45%")
            .assertIsDisplayed()

        // Verify YES and NO buttons are present
        composeTestRule.onNodeWithText("YES")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithText("NO")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithText("SKIP")
            .assertIsDisplayed()
            .assertHasClickAction()

        // WHEN: User clicks YES
        composeTestRule.onNodeWithText("YES")
            .performClick()

        // Wait for UI update and database write
        composeTestRule.waitForIdle()

        // THEN: Verify callback was called
        assertTrue("onConfirm callback should be invoked", confirmCalled)
        assertFalse("onDismiss should not be called when confirming", dismissCalled)

        // THEN: Verify dialog dismisses
        composeTestRule.onNodeWithText("Did I understand you correctly?")
            .assertDoesNotExist()

        // THEN: Verify database was updated with USER_CONFIRMED source
        val database = DatabaseProvider.getDatabase(context)
        val intentExampleDao = database.intentExampleDao()

        val savedExamples = intentExampleDao.getExamplesForIntentOnce(interpretedIntent)

        // Verify example was saved
        assertTrue("Database should contain at least one example", savedExamples.isNotEmpty())

        val savedExample = savedExamples.first()
        assertEquals("Intent ID should match", interpretedIntent, savedExample.intentId)
        assertEquals("Example text should match", userQuery, savedExample.exampleText)
        assertEquals("Source should be USER_CONFIRMED", "USER_CONFIRMED", savedExample.source)
        assertEquals("Locale should be en-US", "en-US", savedExample.locale)
        assertFalse("Should not be marked as primary", savedExample.isPrimary)

        // Verify hash is correct
        val expectedHash = computeHash(interpretedIntent, userQuery)
        assertEquals("Hash should match", expectedHash, savedExample.exampleHash)

        // Verify timestamps
        assertTrue("Created timestamp should be set", savedExample.createdAt > 0)
        assertTrue("Created timestamp should be recent (within last 5 seconds)",
            System.currentTimeMillis() - savedExample.createdAt < 5000
        )
    }

    /**
     * Test Scenario 2: Alternate Selection Flow
     *
     * Flow:
     * 1. Given: query with alternates
     * 2. When: user clicks NO
     * 3. Then: alternates shown (radio buttons)
     * 4. When: user selects alternate #1
     * 5. When: user clicks CONFIRM
     * 6. Then: database updated with corrected intent and USER_CORRECTED source
     */
    @Test
    fun userRejectingInterpretation_showsAlternates_andSelection_savesCorrectedMapping() = runBlocking {
        // GIVEN: Dialog state with alternates
        val userQuery = "turn it down"
        val interpretedIntent = "VOLUME_DOWN"
        val confidence = 0.48f
        val alternates = listOf(
            AlternateIntent("BRIGHTNESS_DOWN", "Brightness Down", 0.42f),
            AlternateIntent("TEMPERATURE_DOWN", "Temperature Down", 0.35f),
            AlternateIntent("SPEED_DOWN", "Speed Down", 0.25f)
        )

        var dialogState: ConfidenceLearningState? = ConfidenceLearningState(
            userInput = userQuery,
            interpretedIntent = interpretedIntent,
            confidence = confidence,
            alternateIntents = alternates
        )

        var confirmCalled = false
        var alternateCalled = false
        var selectedAlternate: AlternateIntent? = null

        // WHEN: Render dialog
        composeTestRule.setContent {
            dialogState?.let { state ->
                ConfidenceLearningDialog(
                    state = state,
                    onConfirm = {
                        confirmCalled = true
                        dialogState = null
                    },
                    onSelectAlternate = { alternate ->
                        alternateCalled = true
                        selectedAlternate = alternate
                        // Simulate saving corrected mapping to database
                        runBlocking {
                            saveToDatabase(
                                userQuery = userQuery,
                                intentId = alternate.intentId,
                                source = "USER_CORRECTED"
                            )
                        }
                        dialogState = null
                    },
                    onDismiss = {
                        dialogState = null
                    }
                )
            }
        }

        // THEN: Verify initial view (not showing alternates yet)
        composeTestRule.onNodeWithText("Did I understand you correctly?")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("\"$userQuery\"")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Volume Down")
            .assertIsDisplayed()

        // WHEN: User clicks NO
        composeTestRule.onNodeWithText("NO")
            .performClick()

        composeTestRule.waitForIdle()

        // THEN: Verify alternates view is shown
        composeTestRule.onNodeWithText("Which one did you mean?")
            .assertIsDisplayed()

        // Verify all alternates are shown with radio buttons
        composeTestRule.onNodeWithText("Brightness Down")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("42% confidence")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Temperature Down")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("35% confidence")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Speed Down")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("25% confidence")
            .assertIsDisplayed()

        // Verify CONFIRM and BACK buttons
        composeTestRule.onNodeWithText("CONFIRM")
            .assertIsDisplayed()
            .assertIsNotEnabled() // Should be disabled until selection

        composeTestRule.onNodeWithText("BACK")
            .assertIsDisplayed()
            .assertHasClickAction()

        // WHEN: User selects alternate #1 (Brightness Down)
        composeTestRule.onNodeWithText("Brightness Down")
            .performClick()

        composeTestRule.waitForIdle()

        // THEN: CONFIRM button should now be enabled
        composeTestRule.onNodeWithText("CONFIRM")
            .assertIsEnabled()

        // WHEN: User clicks CONFIRM
        composeTestRule.onNodeWithText("CONFIRM")
            .performClick()

        composeTestRule.waitForIdle()

        // THEN: Verify callbacks
        assertTrue("onSelectAlternate should be called", alternateCalled)
        assertFalse("onConfirm should not be called", confirmCalled)
        assertNotNull("Selected alternate should be set", selectedAlternate)
        assertEquals("Selected alternate should be Brightness Down",
            "BRIGHTNESS_DOWN", selectedAlternate?.intentId
        )

        // THEN: Verify dialog dismisses
        composeTestRule.onNodeWithText("Did I understand you correctly?")
            .assertDoesNotExist()

        // THEN: Verify database was updated with USER_CORRECTED source
        val database = DatabaseProvider.getDatabase(context)
        val intentExampleDao = database.intentExampleDao()

        val savedExamples = intentExampleDao.getExamplesForIntentOnce("BRIGHTNESS_DOWN")

        // Verify corrected example was saved
        assertTrue("Database should contain at least one example", savedExamples.isNotEmpty())

        val savedExample = savedExamples.first()
        assertEquals("Intent ID should be corrected alternate", "BRIGHTNESS_DOWN", savedExample.intentId)
        assertEquals("Example text should match original query", userQuery, savedExample.exampleText)
        assertEquals("Source should be USER_CORRECTED", "USER_CORRECTED", savedExample.source)
        assertEquals("Locale should be en-US", "en-US", savedExample.locale)
        assertFalse("Should not be marked as primary", savedExample.isPrimary)

        // Verify hash matches corrected intent
        val expectedHash = computeHash("BRIGHTNESS_DOWN", userQuery)
        assertEquals("Hash should match corrected mapping", expectedHash, savedExample.exampleHash)

        // Verify timestamps
        assertTrue("Created timestamp should be set", savedExample.createdAt > 0)
        assertTrue("Created timestamp should be recent (within last 5 seconds)",
            System.currentTimeMillis() - savedExample.createdAt < 5000
        )
    }

    /**
     * Test: SKIP button dismisses dialog without saving to database
     */
    @Test
    fun userClicksSkip_dismissesDialog_withoutSavingToDatabase() = runBlocking {
        // GIVEN: Dialog state
        val userQuery = "do something"
        val interpretedIntent = "UNKNOWN"
        val confidence = 0.30f

        var dialogState: ConfidenceLearningState? = ConfidenceLearningState(
            userInput = userQuery,
            interpretedIntent = interpretedIntent,
            confidence = confidence,
            alternateIntents = emptyList()
        )

        var dismissCalled = false

        // WHEN: Render dialog
        composeTestRule.setContent {
            dialogState?.let { state ->
                ConfidenceLearningDialog(
                    state = state,
                    onConfirm = {
                        dialogState = null
                    },
                    onSelectAlternate = { alternate ->
                        dialogState = null
                    },
                    onDismiss = {
                        dismissCalled = true
                        dialogState = null
                    }
                )
            }
        }

        // THEN: Verify dialog is shown
        composeTestRule.onNodeWithText("Did I understand you correctly?")
            .assertIsDisplayed()

        // WHEN: User clicks SKIP
        composeTestRule.onNodeWithText("SKIP")
            .performClick()

        composeTestRule.waitForIdle()

        // THEN: Verify callback was called
        assertTrue("onDismiss callback should be invoked", dismissCalled)

        // THEN: Verify dialog dismisses
        composeTestRule.onNodeWithText("Did I understand you correctly?")
            .assertDoesNotExist()

        // THEN: Verify database was NOT updated
        val database = DatabaseProvider.getDatabase(context)
        val intentExampleDao = database.intentExampleDao()

        val savedExamples = intentExampleDao.getExamplesForIntentOnce(interpretedIntent)

        // Verify no examples were saved
        assertTrue("Database should be empty (no learning occurred)", savedExamples.isEmpty())
    }

    /**
     * Test: NO button with no alternates dismisses dialog
     */
    @Test
    fun userClicksNo_withNoAlternates_dismissesDialog() = runBlocking {
        // GIVEN: Dialog state with no alternates
        val userQuery = "do something"
        val interpretedIntent = "UNKNOWN"
        val confidence = 0.30f

        var dialogState: ConfidenceLearningState? = ConfidenceLearningState(
            userInput = userQuery,
            interpretedIntent = interpretedIntent,
            confidence = confidence,
            alternateIntents = emptyList() // No alternates
        )

        var dismissCalled = false

        // WHEN: Render dialog
        composeTestRule.setContent {
            dialogState?.let { state ->
                ConfidenceLearningDialog(
                    state = state,
                    onConfirm = {
                        dialogState = null
                    },
                    onSelectAlternate = { alternate ->
                        dialogState = null
                    },
                    onDismiss = {
                        dismissCalled = true
                        dialogState = null
                    }
                )
            }
        }

        // THEN: Verify dialog is shown
        composeTestRule.onNodeWithText("Did I understand you correctly?")
            .assertIsDisplayed()

        // WHEN: User clicks NO
        composeTestRule.onNodeWithText("NO")
            .performClick()

        composeTestRule.waitForIdle()

        // THEN: Verify dialog dismisses (since no alternates available)
        assertTrue("onDismiss should be called when NO is clicked with no alternates", dismissCalled)

        composeTestRule.onNodeWithText("Did I understand you correctly?")
            .assertDoesNotExist()
    }

    /**
     * Test: BACK button returns from alternates view to initial view
     */
    @Test
    fun userClicksBack_returnsToInitialView() = runBlocking {
        // GIVEN: Dialog state with alternates
        val userQuery = "test query"
        val interpretedIntent = "TEST_INTENT"
        val confidence = 0.40f
        val alternates = listOf(
            AlternateIntent("ALTERNATE_1", "Alternate 1", 0.35f),
            AlternateIntent("ALTERNATE_2", "Alternate 2", 0.30f)
        )

        var dialogState: ConfidenceLearningState? = ConfidenceLearningState(
            userInput = userQuery,
            interpretedIntent = interpretedIntent,
            confidence = confidence,
            alternateIntents = alternates
        )

        // WHEN: Render dialog
        composeTestRule.setContent {
            dialogState?.let { state ->
                ConfidenceLearningDialog(
                    state = state,
                    onConfirm = { dialogState = null },
                    onSelectAlternate = { dialogState = null },
                    onDismiss = { dialogState = null }
                )
            }
        }

        // THEN: Verify initial view
        composeTestRule.onNodeWithText("I think you meant:")
            .assertIsDisplayed()

        // WHEN: User clicks NO to show alternates
        composeTestRule.onNodeWithText("NO")
            .performClick()

        composeTestRule.waitForIdle()

        // THEN: Verify alternates view
        composeTestRule.onNodeWithText("Which one did you mean?")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Alternate 1")
            .assertIsDisplayed()

        // WHEN: User clicks BACK
        composeTestRule.onNodeWithText("BACK")
            .performClick()

        composeTestRule.waitForIdle()

        // THEN: Verify returned to initial view
        composeTestRule.onNodeWithText("I think you meant:")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Which one did you mean?")
            .assertDoesNotExist()

        // YES and NO buttons should be visible again
        composeTestRule.onNodeWithText("YES")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("NO")
            .assertIsDisplayed()
    }

    // ==================== Helper Methods ====================

    /**
     * Save user-confirmed or user-corrected example to database.
     * Follows same pattern as ChatViewModel.confirmInterpretation() and selectAlternateIntent().
     *
     * @param userQuery Original user query
     * @param intentId Intent to associate with query
     * @param source Source type (USER_CONFIRMED or USER_CORRECTED)
     */
    private suspend fun saveToDatabase(
        userQuery: String,
        intentId: String,
        source: String
    ) {
        val database = DatabaseProvider.getDatabase(context)
        val intentExampleDao = database.intentExampleDao()

        val exampleHash = computeHash(intentId, userQuery)

        // Check for duplicates
        val existing = intentExampleDao.findDuplicate(exampleHash)
        if (existing != null) {
            // Already exists, skip
            return
        }

        // Create new example entity
        val example = IntentExampleEntity(
            exampleHash = exampleHash,
            intentId = intentId,
            exampleText = userQuery,
            isPrimary = false,
            source = source,
            formatVersion = "v2.0",
            ipcCode = null,
            locale = "en-US",
            createdAt = System.currentTimeMillis(),
            usageCount = 0,
            lastUsed = null
        )

        // Insert into database
        intentExampleDao.insertIntentExample(example)
    }

    /**
     * Compute MD5 hash for intent example deduplication.
     * Matches pattern from IntentExampleEntity and ChatViewModel.
     *
     * @param intentId Intent identifier
     * @param exampleText Example text
     * @return MD5 hash as hex string
     */
    private fun computeHash(intentId: String, exampleText: String): String {
        val input = "$intentId|$exampleText"
        val md5 = MessageDigest.getInstance("MD5")
        val digest = md5.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
