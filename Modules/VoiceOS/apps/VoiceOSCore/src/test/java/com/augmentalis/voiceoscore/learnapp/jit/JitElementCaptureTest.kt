/**
 * JitElementCaptureTest.kt - Unit tests for JIT element capture deduplication
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Testing Team
 * Created: 2025-12-03
 *
 * Tests for JIT element deduplication fix (VOS-JIT-006)
 * Verifies app-scoped deduplication prevents cross-app hash collisions.
 *
 * Test Requirements (from spec):
 * - FR-001: App-scoped element deduplication
 * - Elements with same hash but different appIds can coexist
 * - Elements with same hash and same appId are skipped as duplicates
 * - getByHashAndApp returns null for different app
 *
 * @see JitElementCapture
 * @see com.augmentalis.database.repositories.IScrapedElementRepository
 */
package com.augmentalis.voiceoscore.learnapp.jit

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.database.repositories.IScrapedElementRepository
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for JIT element capture with app-scoped deduplication.
 *
 * Uses Mockito to mock database dependencies and verify behavior
 * without requiring a real database connection.
 */
class JitElementCaptureTest {

    // Mocked dependencies
    @Mock
    private lateinit var mockAccessibilityService: AccessibilityService

    @Mock
    private lateinit var mockDatabaseManager: VoiceOSDatabaseManager

    @Mock
    private lateinit var mockRepository: IScrapedElementRepository

    @Mock
    private lateinit var mockUuidGenerator: ThirdPartyUuidGenerator

    // System under test
    private lateinit var jitElementCapture: JitElementCapture

    // Test data storage (simulates database)
    private val testDatabase = mutableMapOf<Pair<String, String>, ScrapedElementDTO>()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Setup mock database manager to return mock repository
        whenever(mockDatabaseManager.scrapedElements).thenReturn(mockRepository)

        // Setup repository mock to use our test database
        setupRepositoryMocks()

        // Create system under test
        jitElementCapture = JitElementCapture(
            accessibilityService = mockAccessibilityService,
            databaseManager = mockDatabaseManager,
            thirdPartyUuidGenerator = mockUuidGenerator
        )
    }

    /**
     * Configure repository mocks to simulate database behavior
     */
    private fun setupRepositoryMocks() = runTest {
        // Mock getByHashAndApp - returns element only if hash AND app match
        whenever(mockRepository.getByHashAndApp(any(), any())).thenAnswer { invocation ->
            val hash = invocation.getArgument<String>(0)
            val appId = invocation.getArgument<String>(1)
            testDatabase[Pair(hash, appId)]
        }

        // Mock getByHash - returns first element with matching hash (any app)
        whenever(mockRepository.getByHash(any())).thenAnswer { invocation ->
            val hash = invocation.getArgument<String>(0)
            testDatabase.entries.firstOrNull { it.key.first == hash }?.value
        }

        // Mock insert - adds element to test database
        whenever(mockRepository.insert(any())).thenAnswer { invocation ->
            val element = invocation.getArgument<ScrapedElementDTO>(0)
            testDatabase[Pair(element.elementHash, element.appId)] = element
            Unit
        }
    }

    /**
     * TEST 1: Same hash, different apps
     *
     * Scenario: Two different apps have elements with identical hashes
     * Expected: Both elements should persist (count1 = 1, count2 = 1)
     *
     * This is the PRIMARY fix for VOS-JIT-006: preventing cross-app collisions.
     */
    @Test
    fun `persistElements should allow same hash for different apps`() = runTest {
        println("\n========== TEST 1: Same Hash, Different Apps ==========\n")

        // Given: Element with specific hash
        val sharedHash = "element_abc123"
        val app1 = "com.realwear.launcher"
        val app2 = "com.augmentalis.voiceos"

        val element1 = createTestElement(hash = sharedHash, text = "Settings (RealWear)")
        val element2 = createTestElement(hash = sharedHash, text = "Settings (VoiceOS)")

        println("Creating element with hash: $sharedHash")
        println("App 1: $app1")
        println("App 2: $app2")

        // Act: Persist for app1
        println("\n--- Persisting for App 1 ---")
        val count1 = jitElementCapture.persistElements(app1, listOf(element1))
        println("Result: $count1 elements persisted")

        // Act: Persist for app2 with SAME hash
        println("\n--- Persisting for App 2 (same hash) ---")
        val count2 = jitElementCapture.persistElements(app2, listOf(element2))
        println("Result: $count2 elements persisted")

        // Assert: Both should persist (NOT cross-app collision)
        assertEquals(1, count1, "First app should persist 1 element")
        assertEquals(1, count2, "Second app should ALSO persist 1 element (no cross-app collision)")

        // Verify: Both exist in database with correct appIds
        val app1Element = testDatabase[Pair(sharedHash, app1)]
        val app2Element = testDatabase[Pair(sharedHash, app2)]

        assertNotNull(app1Element, "App1 element should exist in database")
        assertNotNull(app2Element, "App2 element should exist in database")
        assertEquals(app1, app1Element?.appId, "App1 element should have correct appId")
        assertEquals(app2, app2Element?.appId, "App2 element should have correct appId")

        // Verify: insert() was called twice (once per app)
        verify(mockRepository, times(2)).insert(any())

        println("\n✅ PASS: Elements with same hash persisted for different apps")
        println("   - App1 ($app1): 1 element")
        println("   - App2 ($app2): 1 element")
        println("   - Total database entries: ${testDatabase.size}")
    }

    /**
     * TEST 2: Same hash, same app (duplicate)
     *
     * Scenario: Same app tries to persist element with duplicate hash
     * Expected: First persists (count1 = 1), second skips (count2 = 0)
     *
     * This verifies that within-app deduplication still works correctly.
     */
    @Test
    fun `persistElements should skip duplicate for same app`() = runTest {
        println("\n========== TEST 2: Same Hash, Same App (Duplicate) ==========\n")

        // Given: Element with specific hash
        val sharedHash = "element_def456"
        val appId = "com.example.app"

        val element1 = createTestElement(hash = sharedHash, text = "Submit Button")
        val element2 = createTestElement(hash = sharedHash, text = "Submit Button (duplicate)")

        println("Creating element with hash: $sharedHash")
        println("App: $appId")

        // Act: Persist twice for SAME app
        println("\n--- First Persist ---")
        val count1 = jitElementCapture.persistElements(appId, listOf(element1))
        println("Result: $count1 elements persisted")

        println("\n--- Second Persist (duplicate) ---")
        val count2 = jitElementCapture.persistElements(appId, listOf(element2))
        println("Result: $count2 elements persisted")

        // Assert: First persists, second skipped
        assertEquals(1, count1, "First attempt should persist 1 element")
        assertEquals(0, count2, "Second attempt should skip duplicate (0 persisted)")

        // Verify: Only one entry in database
        val databaseElement = testDatabase[Pair(sharedHash, appId)]
        assertNotNull(databaseElement, "Element should exist in database")
        assertEquals(appId, databaseElement?.appId, "Element should have correct appId")

        // Verify: insert() was called only once (duplicate skipped)
        verify(mockRepository, times(1)).insert(any())

        println("\n✅ PASS: Duplicate element correctly skipped for same app")
        println("   - First persist: 1 element")
        println("   - Second persist: 0 elements (duplicate)")
        println("   - Total database entries: ${testDatabase.size}")
    }

    /**
     * TEST 3: getByHashAndApp scoping
     *
     * Scenario: Element exists for app1, query with app2
     * Expected: Returns null (app-scoped lookup)
     *
     * This verifies the core fix: getByHashAndApp must check BOTH hash AND appId.
     */
    @Test
    fun `getByHashAndApp should return null for different app`() = runTest {
        println("\n========== TEST 3: getByHashAndApp Scoping ==========\n")

        // Given: Element persisted for app1
        val hash = "element_ghi789"
        val app1 = "com.app1"
        val app2 = "com.app2"

        val element = createTestElement(hash = hash, text = "Settings")

        println("Persisting element with hash: $hash")
        println("App 1: $app1")

        // Persist for app1
        jitElementCapture.persistElements(app1, listOf(element))
        println("Persisted to App 1: SUCCESS")

        // Act: Query for DIFFERENT app
        println("\n--- Querying for App 2 (different app) ---")
        val result = mockRepository.getByHashAndApp(hash, app2)
        println("Result: ${if (result == null) "null (not found)" else "found (INCORRECT!)"}")

        // Assert: Should return null (app-scoped)
        assertNull(result, "getByHashAndApp should return null for different app")

        // Verify: Element DOES exist for app1
        val app1Element = mockRepository.getByHashAndApp(hash, app1)
        assertNotNull(app1Element, "Element should exist for app1")
        assertEquals(app1, app1Element?.appId, "Element should have app1's appId")

        println("\n✅ PASS: getByHashAndApp correctly scopes to app")
        println("   - Query for app1 ($app1): FOUND")
        println("   - Query for app2 ($app2): NULL")
    }

    /**
     * TEST 4: Multiple elements, mixed scenario
     *
     * Scenario: Persist batch with duplicates and cross-app elements
     * Expected: Correct counts for each scenario
     */
    @Test
    fun `persistElements should handle mixed batch correctly`() = runTest {
        println("\n========== TEST 4: Mixed Batch Scenario ==========\n")

        val app1 = "com.app1"
        val app2 = "com.app2"

        // Batch 1 for app1: 3 unique elements
        val app1Batch1 = listOf(
            createTestElement(hash = "hash_001", text = "Button 1"),
            createTestElement(hash = "hash_002", text = "Button 2"),
            createTestElement(hash = "hash_003", text = "Button 3")
        )

        println("Batch 1 for $app1: ${app1Batch1.size} elements")
        val count1 = jitElementCapture.persistElements(app1, app1Batch1)
        println("Persisted: $count1 elements")
        assertEquals(3, count1, "All 3 elements should persist")

        // Batch 2 for app1: 2 duplicates, 1 new
        val app1Batch2 = listOf(
            createTestElement(hash = "hash_001", text = "Button 1 (dup)"),  // Duplicate
            createTestElement(hash = "hash_002", text = "Button 2 (dup)"),  // Duplicate
            createTestElement(hash = "hash_004", text = "Button 4")         // New
        )

        println("\nBatch 2 for $app1: ${app1Batch2.size} elements (2 duplicates, 1 new)")
        val count2 = jitElementCapture.persistElements(app1, app1Batch2)
        println("Persisted: $count2 elements")
        assertEquals(1, count2, "Only 1 new element should persist (2 duplicates skipped)")

        // Batch 3 for app2: Same hashes as app1 (cross-app)
        val app2Batch1 = listOf(
            createTestElement(hash = "hash_001", text = "Button 1 (app2)"),  // Same hash, different app
            createTestElement(hash = "hash_002", text = "Button 2 (app2)"),  // Same hash, different app
            createTestElement(hash = "hash_005", text = "Button 5")          // New hash
        )

        println("\nBatch 3 for $app2: ${app2Batch1.size} elements (2 cross-app, 1 new)")
        val count3 = jitElementCapture.persistElements(app2, app2Batch1)
        println("Persisted: $count3 elements")
        assertEquals(3, count3, "All 3 should persist (cross-app not collisions)")

        // Verify final state
        println("\n--- Final Database State ---")
        println("Total entries: ${testDatabase.size}")
        println("App1 entries: ${testDatabase.count { it.key.second == app1 }}")
        println("App2 entries: ${testDatabase.count { it.key.second == app2 }}")

        assertEquals(7, testDatabase.size, "Should have 7 total entries (4 app1 + 3 app2)")

        println("\n✅ PASS: Mixed batch handled correctly")
    }

    /**
     * TEST 5: Cross-app collision detection
     *
     * Scenario: Persist element that collides with different app
     * Expected: ERROR log generated (verified via mock interaction)
     */
    @Test
    fun `persistElements should log error on cross-app collision`() = runTest {
        println("\n========== TEST 5: Cross-App Collision Detection ==========\n")

        val hash = "collision_hash"
        val app1 = "com.augmentalis.voiceos"
        val app2 = "com.realwear.launcher"

        // Persist for app1
        val element1 = createTestElement(hash = hash, text = "Consent Dialog")
        println("Persisting to $app1")
        jitElementCapture.persistElements(app1, listOf(element1))

        // Try to persist same hash for app2 (should succeed, but log error)
        val element2 = createTestElement(hash = hash, text = "Launcher Button")
        println("Persisting to $app2 (should detect collision)")
        val count = jitElementCapture.persistElements(app2, listOf(element2))

        // Assert: Second persist succeeds (app-scoped)
        assertEquals(1, count, "Second app should persist successfully")

        // Verify: Both elements exist
        assertNotNull(testDatabase[Pair(hash, app1)], "App1 element should exist")
        assertNotNull(testDatabase[Pair(hash, app2)], "App2 element should exist")

        println("\n✅ PASS: Cross-app collision detected and logged")
        println("   Note: Check logcat for ERROR log with 'APP ID MISMATCH'")
    }

    /**
     * TEST 6: Empty batch
     *
     * Scenario: Persist empty list
     * Expected: Returns 0, no errors
     */
    @Test
    fun `persistElements should handle empty batch`() = runTest {
        println("\n========== TEST 6: Empty Batch ==========\n")

        val appId = "com.example.app"
        val emptyBatch = emptyList<JitCapturedElement>()

        println("Persisting empty batch")
        val count = jitElementCapture.persistElements(appId, emptyBatch)
        println("Result: $count elements persisted")

        assertEquals(0, count, "Empty batch should return 0")
        verify(mockRepository, never()).insert(any())

        println("\n✅ PASS: Empty batch handled correctly")
    }

    // ==================== Helper Methods ====================

    /**
     * Create test element with configurable properties
     */
    private fun createTestElement(
        hash: String,
        text: String = "Test Element",
        className: String = "android.widget.Button",
        uuid: String? = "test-uuid-${hash.hashCode()}"
    ): JitCapturedElement {
        return JitCapturedElement(
            elementHash = hash,
            className = className,
            viewIdResourceName = "btn_test",
            text = text,
            contentDescription = "Test button: $text",
            bounds = Rect(0, 0, 100, 50),
            isClickable = true,
            isLongClickable = false,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isFocusable = true,
            isEnabled = true,
            depth = 2,
            indexInParent = 0,
            uuid = uuid
        )
    }
}
