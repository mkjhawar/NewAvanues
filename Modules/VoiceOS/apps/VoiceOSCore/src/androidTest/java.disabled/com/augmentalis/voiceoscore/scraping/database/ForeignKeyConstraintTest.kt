/**
 * ForeignKeyConstraintTest.kt - Test foreign key constraints in app scraping database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Database Expert
 * Created: 2025-10-24
 *
 * Purpose: Verify foreign key constraints work correctly for element relationships,
 *          user interactions, and element state history tables.
 *
 * Tests:
 * - Parent-child insertion order (parent must exist before child)
 * - Cascade delete operations
 * - Null target_element_hash in ElementRelationshipEntity
 * - Error handling for missing parent records
 */
package com.augmentalis.voiceoscore.scraping.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceoscore.scraping.entities.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test foreign key constraints for Phase 3 database tables
 *
 * Verifies that:
 * 1. Foreign keys are enforced (parent records must exist)
 * 2. Cascade deletes work correctly
 * 3. Null foreign keys are handled (for optional relationships)
 */
@RunWith(AndroidJUnit4::class)
class ForeignKeyConstraintTest {

    private lateinit var database: AppScrapingDatabase
    private lateinit var context: Context

    // Test data constants
    private val testAppId = "test_app_001"
    private val testPackageName = "com.example.testapp"
    private val testElementHash = "hash_test_button"
    private val testElementHash2 = "hash_test_input"
    private val testScreenHash = "hash_test_screen"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database for testing (fresh for each test)
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppScrapingDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()
    }

    @After
    fun cleanup() {
        database.close()
    }

    // ===== Helper Functions =====

    /**
     * Create a test app entity
     */
    private fun createTestApp(appId: String = testAppId): ScrapedAppEntity {
        return ScrapedAppEntity(
            appId = appId,
            packageName = testPackageName,
            appName = "Test App",
            versionCode = 1,
            versionName = "1.0",
            appHash = "test_hash_$appId",
            firstScraped = System.currentTimeMillis(),
            lastScraped = System.currentTimeMillis()
        )
    }

    /**
     * Create a test scraped element entity
     */
    private fun createTestElement(
        elementHash: String = testElementHash,
        appId: String = testAppId
    ): ScrapedElementEntity {
        return ScrapedElementEntity(
            elementHash = elementHash,
            appId = appId,
            className = "android.widget.Button",
            viewIdResourceName = "test_button",
            text = "Submit",
            contentDescription = "Submit button",
            bounds = """{"left":0,"top":0,"right":100,"bottom":50}""",
            isClickable = true,
            isLongClickable = false,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isFocusable = true,
            isEnabled = true,
            depth = 1,
            indexInParent = 0
        )
    }

    /**
     * Create a test screen context entity
     */
    private fun createTestScreenContext(
        screenHash: String = testScreenHash,
        appId: String = testAppId
    ): ScreenContextEntity {
        return ScreenContextEntity(
            screenHash = screenHash,
            appId = appId,
            packageName = testPackageName,
            activityName = "MainActivity",
            windowTitle = "Test Screen",
            screenType = "test_screen",
            formContext = null,
            primaryAction = "test"
        )
    }

    // ===== ElementStateHistoryEntity Tests =====

    @Test
    fun testElementStateHistory_insertWithValidParents_succeeds() = runBlocking {
        // Insert parent records first
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement())
        database.screenContextDao().insert(createTestScreenContext())

        // Insert child record (should succeed)
        val stateChange = ElementStateHistoryEntity(
            elementHash = testElementHash,
            screenHash = testScreenHash,
            stateType = StateType.CHECKED,
            oldValue = "false",
            newValue = "true",
            triggeredBy = TriggerSource.USER_CLICK
        )

        val insertedId = database.elementStateHistoryDao().insert(stateChange)
        assertTrue("Insert should return valid ID", insertedId > 0)

        // Verify record was inserted
        val history = database.elementStateHistoryDao().getStateHistoryForElement(testElementHash)
        assertEquals(1, history.size)
        assertEquals(StateType.CHECKED, history[0].stateType)
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testElementStateHistory_insertWithoutElementParent_fails() = runBlocking {
        // Insert app and screen, but NOT element
        database.scrapedAppDao().insert(createTestApp())
        database.screenContextDao().insert(createTestScreenContext())

        // Try to insert state change (should fail - missing element parent)
        val stateChange = ElementStateHistoryEntity(
            elementHash = testElementHash, // This element doesn't exist!
            screenHash = testScreenHash,
            stateType = StateType.CHECKED,
            oldValue = "false",
            newValue = "true"
        )

        database.elementStateHistoryDao().insert(stateChange) // Should throw
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testElementStateHistory_insertWithoutScreenParent_fails() = runBlocking {
        // Insert app and element, but NOT screen
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement())

        // Try to insert state change (should fail - missing screen parent)
        val stateChange = ElementStateHistoryEntity(
            elementHash = testElementHash,
            screenHash = testScreenHash, // This screen doesn't exist!
            stateType = StateType.CHECKED,
            oldValue = "false",
            newValue = "true"
        )

        database.elementStateHistoryDao().insert(stateChange) // Should throw
    }

    @Test
    fun testElementStateHistory_cascadeDeleteOnElement_works() = runBlocking {
        // Setup: Insert parents and child
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement())
        database.screenContextDao().insert(createTestScreenContext())

        val stateChange = ElementStateHistoryEntity(
            elementHash = testElementHash,
            screenHash = testScreenHash,
            stateType = StateType.CHECKED,
            oldValue = "false",
            newValue = "true"
        )
        database.elementStateHistoryDao().insert(stateChange)

        // Verify child exists
        assertEquals(1, database.elementStateHistoryDao().getStateHistoryForElement(testElementHash).size)

        // Delete parent element (should cascade delete state history)
        database.scrapedElementDao().deleteByHash(testElementHash)

        // Verify child was deleted
        assertEquals(0, database.elementStateHistoryDao().getStateHistoryForElement(testElementHash).size)
    }

    // ===== UserInteractionEntity Tests =====

    @Test
    fun testUserInteraction_insertWithValidParents_succeeds() = runBlocking {
        // Insert parent records first
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement())
        database.screenContextDao().insert(createTestScreenContext())

        // Insert user interaction (should succeed)
        val interaction = UserInteractionEntity(
            elementHash = testElementHash,
            screenHash = testScreenHash,
            interactionType = InteractionType.CLICK,
            visibilityDuration = 1500L,
            success = true
        )

        val insertedId = database.userInteractionDao().insert(interaction)
        assertTrue("Insert should return valid ID", insertedId > 0)

        // Verify record was inserted
        val interactions = database.userInteractionDao().getInteractionsForElement(testElementHash)
        assertEquals(1, interactions.size)
        assertEquals(InteractionType.CLICK, interactions[0].interactionType)
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testUserInteraction_insertWithoutElementParent_fails() = runBlocking {
        // Insert app and screen, but NOT element
        database.scrapedAppDao().insert(createTestApp())
        database.screenContextDao().insert(createTestScreenContext())

        // Try to insert interaction (should fail - missing element parent)
        val interaction = UserInteractionEntity(
            elementHash = testElementHash, // This element doesn't exist!
            screenHash = testScreenHash,
            interactionType = InteractionType.CLICK
        )

        database.userInteractionDao().insert(interaction) // Should throw
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testUserInteraction_insertWithoutScreenParent_fails() = runBlocking {
        // Insert app and element, but NOT screen
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement())

        // Try to insert interaction (should fail - missing screen parent)
        val interaction = UserInteractionEntity(
            elementHash = testElementHash,
            screenHash = testScreenHash, // This screen doesn't exist!
            interactionType = InteractionType.CLICK
        )

        database.userInteractionDao().insert(interaction) // Should throw
    }

    @Test
    fun testUserInteraction_cascadeDeleteOnScreen_works() = runBlocking {
        // Setup: Insert parents and child
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement())
        database.screenContextDao().insert(createTestScreenContext())

        val interaction = UserInteractionEntity(
            elementHash = testElementHash,
            screenHash = testScreenHash,
            interactionType = InteractionType.CLICK
        )
        database.userInteractionDao().insert(interaction)

        // Verify child exists
        assertEquals(1, database.userInteractionDao().getInteractionsForElement(testElementHash).size)

        // Delete parent screen (should cascade delete interactions)
        database.screenContextDao().deleteByHash(testScreenHash)

        // Verify child was deleted
        assertEquals(0, database.userInteractionDao().getInteractionsForElement(testElementHash).size)
    }

    // ===== ElementRelationshipEntity Tests =====

    @Test
    fun testElementRelationship_insertWithValidParents_succeeds() = runBlocking {
        // Insert parent records first
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement(testElementHash))
        database.scrapedElementDao().insert(createTestElement(testElementHash2))

        // Insert relationship (should succeed)
        val relationship = ElementRelationshipEntity(
            sourceElementHash = testElementHash,
            targetElementHash = testElementHash2,
            relationshipType = RelationshipType.LABEL_FOR,
            confidence = 0.95f
        )

        val insertedId = database.elementRelationshipDao().insert(relationship)
        assertTrue("Insert should return valid ID", insertedId > 0)

        // Verify record was inserted
        val relationships = database.elementRelationshipDao().getRelationshipsForSource(testElementHash)
        assertEquals(1, relationships.size)
        assertEquals(RelationshipType.LABEL_FOR, relationships[0].relationshipType)
    }

    @Test
    fun testElementRelationship_insertWithNullTarget_succeeds() = runBlocking {
        // Insert parent records (only source element needed)
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement(testElementHash))

        // Insert relationship with null target (should succeed)
        val relationship = ElementRelationshipEntity(
            sourceElementHash = testElementHash,
            targetElementHash = null, // Null is allowed
            relationshipType = RelationshipType.NAVIGATES_TO,
            relationshipData = """{"targetScreen":"ExternalActivity"}""",
            confidence = 0.80f
        )

        val insertedId = database.elementRelationshipDao().insert(relationship)
        assertTrue("Insert should return valid ID", insertedId > 0)

        // Verify record was inserted
        val relationships = database.elementRelationshipDao().getRelationshipsForSource(testElementHash)
        assertEquals(1, relationships.size)
        assertNull(relationships[0].targetElementHash)
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testElementRelationship_insertWithoutSourceParent_fails() = runBlocking {
        // Insert app but NOT source element
        database.scrapedAppDao().insert(createTestApp())

        // Try to insert relationship (should fail - missing source parent)
        val relationship = ElementRelationshipEntity(
            sourceElementHash = testElementHash, // This element doesn't exist!
            targetElementHash = null,
            relationshipType = RelationshipType.TRIGGERS
        )

        database.elementRelationshipDao().insert(relationship) // Should throw
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testElementRelationship_insertWithInvalidTargetParent_fails() = runBlocking {
        // Insert app and source element, but NOT target element
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement(testElementHash))

        // Try to insert relationship with non-existent target
        val relationship = ElementRelationshipEntity(
            sourceElementHash = testElementHash,
            targetElementHash = testElementHash2, // This element doesn't exist!
            relationshipType = RelationshipType.LABEL_FOR
        )

        database.elementRelationshipDao().insert(relationship) // Should throw
    }

    @Test
    fun testElementRelationship_cascadeDeleteOnSource_works() = runBlocking {
        // Setup: Insert parents and child
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement(testElementHash))
        database.scrapedElementDao().insert(createTestElement(testElementHash2))

        val relationship = ElementRelationshipEntity(
            sourceElementHash = testElementHash,
            targetElementHash = testElementHash2,
            relationshipType = RelationshipType.LABEL_FOR
        )
        database.elementRelationshipDao().insert(relationship)

        // Verify child exists
        assertEquals(1, database.elementRelationshipDao().getRelationshipsForSource(testElementHash).size)

        // Delete source element (should cascade delete relationship)
        database.scrapedElementDao().deleteByHash(testElementHash)

        // Verify child was deleted
        assertEquals(0, database.elementRelationshipDao().getRelationshipsForSource(testElementHash).size)
    }

    @Test
    fun testElementRelationship_cascadeDeleteOnTarget_works() = runBlocking {
        // Setup: Insert parents and child
        database.scrapedAppDao().insert(createTestApp())
        database.scrapedElementDao().insert(createTestElement(testElementHash))
        database.scrapedElementDao().insert(createTestElement(testElementHash2))

        val relationship = ElementRelationshipEntity(
            sourceElementHash = testElementHash,
            targetElementHash = testElementHash2,
            relationshipType = RelationshipType.LABEL_FOR
        )
        database.elementRelationshipDao().insert(relationship)

        // Verify child exists
        assertEquals(1, database.elementRelationshipDao().getRelationshipsForSource(testElementHash).size)

        // Delete target element (should cascade delete relationship)
        database.scrapedElementDao().deleteByHash(testElementHash2)

        // Verify child was deleted
        assertEquals(0, database.elementRelationshipDao().getRelationshipsForSource(testElementHash).size)
    }

    // ===== Complex Scenarios =====

    @Test
    fun testComplexScenario_multipleRelationshipsAndInteractions() = runBlocking {
        // Setup complex data structure
        val app = createTestApp()
        val screen = createTestScreenContext()
        val button = createTestElement(testElementHash)
        val input = createTestElement(testElementHash2)

        database.scrapedAppDao().insert(app)
        database.screenContextDao().insert(screen)
        database.scrapedElementDao().insert(button)
        database.scrapedElementDao().insert(input)

        // Add relationship
        val relationship = ElementRelationshipEntity(
            sourceElementHash = testElementHash,
            targetElementHash = testElementHash2,
            relationshipType = RelationshipType.BUTTON_SUBMITS_FORM
        )
        database.elementRelationshipDao().insert(relationship)

        // Add interaction
        val interaction = UserInteractionEntity(
            elementHash = testElementHash,
            screenHash = testScreenHash,
            interactionType = InteractionType.CLICK
        )
        database.userInteractionDao().insert(interaction)

        // Add state change
        val stateChange = ElementStateHistoryEntity(
            elementHash = testElementHash2,
            screenHash = testScreenHash,
            stateType = StateType.TEXT_VALUE,
            oldValue = "",
            newValue = "test@example.com"
        )
        database.elementStateHistoryDao().insert(stateChange)

        // Verify all records exist
        assertEquals(1, database.elementRelationshipDao().getRelationshipsForSource(testElementHash).size)
        assertEquals(1, database.userInteractionDao().getInteractionsForElement(testElementHash).size)
        assertEquals(1, database.elementStateHistoryDao().getStateHistoryForElement(testElementHash2).size)

        // Delete app (should cascade delete EVERYTHING)
        database.scrapedAppDao().deleteByAppId(testAppId)

        // Verify everything was deleted
        assertEquals(0, database.elementRelationshipDao().getRelationshipsForSource(testElementHash).size)
        assertEquals(0, database.userInteractionDao().getInteractionsForElement(testElementHash).size)
        assertEquals(0, database.elementStateHistoryDao().getStateHistoryForElement(testElementHash2).size)
        assertNull(database.scrapedElementDao().getElementByHash(testElementHash))
        assertNull(database.screenContextDao().getScreenByHash(testScreenHash))
    }

    @Test
    fun testInsertionOrder_correctOrder_allSucceed() = runBlocking {
        // Demonstrate correct insertion order

        // 1. Insert app first (no dependencies)
        database.scrapedAppDao().insert(createTestApp())

        // 2. Insert screen (depends on app)
        database.screenContextDao().insert(createTestScreenContext())

        // 3. Insert elements (depend on app)
        database.scrapedElementDao().insert(createTestElement(testElementHash))
        database.scrapedElementDao().insert(createTestElement(testElementHash2))

        // 4. Insert child records (depend on elements and screens)
        database.elementRelationshipDao().insert(
            ElementRelationshipEntity(
                sourceElementHash = testElementHash,
                targetElementHash = testElementHash2,
                relationshipType = RelationshipType.LABEL_FOR
            )
        )

        database.userInteractionDao().insert(
            UserInteractionEntity(
                elementHash = testElementHash,
                screenHash = testScreenHash,
                interactionType = InteractionType.CLICK
            )
        )

        database.elementStateHistoryDao().insert(
            ElementStateHistoryEntity(
                elementHash = testElementHash,
                screenHash = testScreenHash,
                stateType = StateType.CHECKED,
                oldValue = "false",
                newValue = "true"
            )
        )

        // Verify all records were inserted
        assertEquals(1, database.scrapedAppDao().getAppCount())
        assertEquals(1, database.screenContextDao().getAllScreens().size)
        assertEquals(2, database.scrapedElementDao().getAllElements().size)
        assertEquals(1, database.elementRelationshipDao().getRelationshipsForSource(testElementHash).size)
        assertEquals(1, database.userInteractionDao().getInteractionsForElement(testElementHash).size)
        assertEquals(1, database.elementStateHistoryDao().getStateHistoryForElement(testElementHash).size)
    }
}
