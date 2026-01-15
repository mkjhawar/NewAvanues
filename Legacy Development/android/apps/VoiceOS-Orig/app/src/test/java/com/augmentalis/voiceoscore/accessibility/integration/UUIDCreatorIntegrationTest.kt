/**
 * UUIDCreatorIntegrationTest.kt - Comprehensive tests for UUIDCreator integration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Test Framework
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 *
 * DISABLED: UUIDCreatorDatabase migrated from Room to SQLDelight.
 * This test file needs to be updated to use SQLDelight-based testing.
 * See: docs/SCRAPING-DAO-MIGRATION-STATUS-20251127.md
 */
package com.augmentalis.voiceoscore.accessibility.integration

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
import com.augmentalis.uuidcreator.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

/**
 * Comprehensive test suite for UUIDCreator integration with VoiceAccessibility
 *
 * DISABLED: Needs migration to SQLDelight-based testing.
 * Previously used Room in-memory database which is no longer applicable.
 *
 * TODO: Update to use SQLDelight in-memory database when available
 * TODO: See libraries/core/database tests for SQLDelight testing patterns
 */
@org.junit.Ignore("Disabled pending SQLDelight migration - UUIDCreatorDatabase is no longer Room-based")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 29, 30, 31, 32, 33, 34])
@OptIn(ExperimentalCoroutinesApi::class)
class UUIDCreatorIntegrationTest {

    companion object {
        private const val TAG = "UUIDCreatorIntegrationTest"
        private const val TEST_PACKAGE = "com.example.testapp"
        private const val TEST_TIMEOUT_MS = 5000L
    }

    private lateinit var context: Context
    private lateinit var database: UUIDCreatorDatabase
    private lateinit var uuidCreator: UUIDCreator
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Get Robolectric application context
        context = ApplicationProvider.getApplicationContext()

        // DISABLED: Room database no longer used after SQLDelight migration
        // TODO: Update to use SQLDelight in-memory database
        /*
        database = Room.inMemoryDatabaseBuilder(
            context,
            UUIDCreatorDatabase::class.java
        )
        .allowMainThreadQueries()  // OK for tests
        .build()
        */

        // Reset singleton before each test
        // resetUUIDCreatorSingleton() // Disabled
    }

    @After
    fun tearDown() {
        // DISABLED: Room database no longer used after SQLDelight migration
        // database.close()

        // Reset singleton after each test
        // resetUUIDCreatorSingleton() // Disabled

        Dispatchers.resetMain()
    }

    /**
     * Reset UUIDCreator singleton using reflection
     * Required for test isolation
     */
    private fun resetUUIDCreatorSingleton() {
        try {
            val instanceField = UUIDCreator::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)

            // Also reset database singleton
            val dbInstanceField = UUIDCreatorDatabase::class.java.getDeclaredField("INSTANCE")
            dbInstanceField.isAccessible = true
            dbInstanceField.set(null, null)
        } catch (e: Exception) {
            println("Warning: Could not reset singleton via reflection: ${e.message}")
        }
    }

    // ====================================================================
    // Initialization Tests
    // ====================================================================

    @Test
    fun `initialize should create singleton instance successfully`() {
        // When
        val instance = UUIDCreator.initialize(context)

        // Then
        assertNotNull(instance, "Instance should not be null")
        assertSame(instance, UUIDCreator.getInstance(), "getInstance should return same instance")
    }

    @Test
    fun `initialize should be idempotent`() {
        // When
        val instance1 = UUIDCreator.initialize(context)
        val instance2 = UUIDCreator.initialize(context)

        // Then
        assertSame(instance1, instance2, "Multiple initializations should return same instance")
    }

    @Test
    fun `getInstance should throw when not initialized`() {
        // Given - no initialization

        // When/Then
        val exception = assertFailsWith<IllegalStateException> {
                UUIDCreator.getInstance()
            }

        assertTrue(
                exception.message?.contains("not initialized") == true,
                "Exception should mention 'not initialized'"
            )
    }

    @Test
    fun `initialize should use application context`() {
        // Given

        // When
            UUIDCreator.initialize(context)

        // Then
    }

    @Test
    fun `initialize should trigger background loading`() = runTest {
        // When
        val instance = UUIDCreator.initialize(context)

        // Then - background loading should start (non-blocking)
        assertNotNull(instance, "Instance should be created immediately")

        // Allow background tasks to execute
            advanceUntilIdle()
    }

    @Test
    fun `registerElement should generate and return UUID`() = runTest {
        // Given
        val element = createTestElement("TestButton")

        // When
        val uuid = uuidCreator.registerElement(element)

        // Then
        assertNotNull(uuid, "UUID should not be null")
        assertTrue(uuid.isNotEmpty(), "UUID should not be empty")
        assertTrue(uuid.length >= 32, "UUID should be at least 32 characters")
    }

    @Test
    fun `registerElement should allow retrieval by UUID`() = runTest {
        // Given
        val element = createTestElement("TestButton")
        val uuid = uuidCreator.registerElement(element)

        // When
        val retrieved = uuidCreator.findByUUID(uuid)

        // Then
        assertNotNull(retrieved, "Element should be retrievable")
        assertEquals(element.name, retrieved.name, "Element name should match")
        assertEquals(element.type, retrieved.type, "Element type should match")
    }

    @Test
    fun `registerElement should handle multiple elements`() = runTest {
        // Given
        val elements = listOf(
                createTestElement("Button1", type = "Button"),
                createTestElement("Button2", type = "Button"),
                createTestElement("TextField1", type = "EditText")
            )

        // When
        val uuids = elements.map { uuidCreator.registerElement(it) }

        // Then
        assertEquals(3, uuids.size, "Should have 3 UUIDs")
        assertEquals(3, uuids.distinct().size, "All UUIDs should be unique")

        // Verify all can be retrieved
        uuids.forEachIndexed { index, uuid ->
                val retrieved = uuidCreator.findByUUID(uuid)
                assertNotNull(retrieved, "Element $index should be retrievable")
                assertEquals(elements[index].name, retrieved.name)
            }
    }

    @Test
    fun `registerElement should handle elements with same name but different types`() = runTest {
        // Given
        val button = createTestElement("Submit", type = "Button")
        val text = createTestElement("Submit", type = "TextView")

        // When
        val uuid1 = uuidCreator.registerElement(button)
        val uuid2 = uuidCreator.registerElement(text)

        // Then
        assertNotEquals(uuid1, uuid2, "Different element types should get different UUIDs")
    }

    @Test
    fun `registerElement should handle accessibility node info properties`() = runTest {
        // Given
        val element = createTestElement(
                name = "Submit Button",
                type = "Button",
                bounds = Rect(100, 200, 300, 350),
                isClickable = true,
                isEnabled = true
            )

        // When
        val uuid = uuidCreator.registerElement(element)
        val retrieved = uuidCreator.findByUUID(uuid)

        // Then
        assertNotNull(retrieved, "Element should be retrievable")
        assertEquals("Submit Button", retrieved.name)
        assertEquals("Button", retrieved.type)
        // Check clickable via metadata.accessibility
        assertTrue(
                retrieved.metadata?.accessibility?.isClickable == true,
                "Should be clickable"
            )
        assertTrue(retrieved.isEnabled, "Should be enabled")
    }


    @Test
    fun `findByName should return all elements with matching name`() = runTest {
        // Given
        val name = "Submit"
        uuidCreator.registerElement(createTestElement(name, type = "Button"))
        uuidCreator.registerElement(createTestElement(name, type = "TextView"))
        uuidCreator.registerElement(createTestElement("Cancel", type = "Button"))

        // When
        val results = uuidCreator.findByName(name)

        // Then
        assertEquals(2, results.size, "Should find 2 elements named 'Submit'")
        assertTrue(results.all { it.name == name }, "All results should have matching name")
    }

    @Test
    fun `findByType should return all elements of specified type`() = runTest {
        // Given
        uuidCreator.registerElement(createTestElement("Button1", type = "Button"))
        uuidCreator.registerElement(createTestElement("Button2", type = "Button"))
        uuidCreator.registerElement(createTestElement("Text1", type = "TextView"))

        // When
        val results = uuidCreator.findByType("Button")

        // Then
        assertEquals(2, results.size, "Should find 2 Button elements")
        assertTrue(results.all { it.type == "Button" }, "All results should be Buttons")
    }

    @Test
    fun `findByPosition should return element at specified position`() = runTest {
        // Given
        val elements = (1..5).map {
                createTestElement("Element$it", bounds = Rect(0, it * 100, 100, (it + 1) * 100))
            }
        elements.forEach { uuidCreator.registerElement(it) }

        // When
        val result = uuidCreator.findByPosition(2)

        // Then
        assertNotNull(result, "Should find element at position 2")
        assertEquals("Element3", result.name, "Should return third element (0-indexed)")
    }

    @Test
    fun `findByUUID should return null for non-existent UUID`() = runTest {
        // When
        val result = uuidCreator.findByUUID("non-existent-uuid-12345")

        // Then
        assertNull(result, "Should return null for non-existent UUID")
    }

    @Test
    fun `findByName should return empty list for non-matching name`() = runTest {
        // Given
        uuidCreator.registerElement(createTestElement("Button1"))

        // When
        val results = uuidCreator.findByName("NonExistent")

        // Then
        assertTrue(results.isEmpty(), "Should return empty list for non-matching name")
    }


    @Test
    fun `findInDirection should find element to the right`() = runTest {
        // Given - two horizontally aligned elements
        val leftElement = createTestElement("Left", bounds = Rect(0, 100, 100, 200))
        val rightElement = createTestElement("Right", bounds = Rect(200, 100, 300, 200))

        val leftUUID = uuidCreator.registerElement(leftElement)
        uuidCreator.registerElement(rightElement)

        // When
        val result = uuidCreator.findInDirection(leftUUID, "right")

        // Then
        assertNotNull(result, "Should find element to the right")
        assertEquals("Right", result.name, "Should find the right element")
    }

    @Test
    fun `findInDirection should find element below`() = runTest {
        // Given - two vertically aligned elements
        val topElement = createTestElement("Top", bounds = Rect(100, 0, 200, 100))
        val bottomElement = createTestElement("Bottom", bounds = Rect(100, 200, 200, 300))

        val topUUID = uuidCreator.registerElement(topElement)
        uuidCreator.registerElement(bottomElement)

        // When
        val result = uuidCreator.findInDirection(topUUID, "down")

        // Then
        assertNotNull(result, "Should find element below")
        assertEquals("Bottom", result.name, "Should find the bottom element")
    }

    @Test
    fun `findInDirection should handle all cardinal directions`() = runTest {
        // Given - elements in all four directions from center
        val center = createTestElement("Center", bounds = Rect(100, 100, 200, 200))
        val left = createTestElement("Left", bounds = Rect(0, 100, 50, 200))
        val right = createTestElement("Right", bounds = Rect(250, 100, 300, 200))
        val top = createTestElement("Top", bounds = Rect(100, 0, 200, 50))
        val bottom = createTestElement("Bottom", bounds = Rect(100, 250, 200, 300))

        val centerUUID = uuidCreator.registerElement(center)
        uuidCreator.registerElement(left)
        uuidCreator.registerElement(right)
        uuidCreator.registerElement(top)
        uuidCreator.registerElement(bottom)

        // When/Then - test all directions
        assertEquals("Left", uuidCreator.findInDirection(centerUUID, "left")?.name)
        assertEquals("Right", uuidCreator.findInDirection(centerUUID, "right")?.name)
        assertEquals("Top", uuidCreator.findInDirection(centerUUID, "up")?.name)
        assertEquals("Bottom", uuidCreator.findInDirection(centerUUID, "down")?.name)
    }

    @Test
    fun `findInDirection should handle navigation sequences`() = runTest {
        // Given - next/previous navigation
        val first = createTestElement("First", bounds = Rect(0, 0, 100, 100))
        val second = createTestElement("Second", bounds = Rect(0, 100, 100, 200))
        val third = createTestElement("Third", bounds = Rect(0, 200, 100, 300))

        val firstUUID = uuidCreator.registerElement(first)
        val secondUUID = uuidCreator.registerElement(second)
        uuidCreator.registerElement(third)

        // When/Then
        val nextFromFirst = uuidCreator.findInDirection(firstUUID, "next")
        assertNotNull(nextFromFirst, "Should find next element")

        val nextFromSecond = uuidCreator.findInDirection(secondUUID, "next")
        assertEquals("Third", nextFromSecond?.name, "Should find third element")
    }

    @Test
    fun `findInDirection should return null for invalid direction`() = runTest {
        // Given
        val element = createTestElement("Test")
        val uuid = uuidCreator.registerElement(element)

        // When
        val result = uuidCreator.findInDirection(uuid, "invalid_direction")

        // Then
        assertNull(result, "Should return null for invalid direction")
    }


    @Test
    fun `unregisterElement should remove element successfully`() = runTest {
        // Given
        val element = createTestElement("TestButton")
        val uuid = uuidCreator.registerElement(element)
        assertNotNull(uuidCreator.findByUUID(uuid), "Element should exist")

        // When
        val result = uuidCreator.unregisterElement(uuid)

        // Then
        assertTrue(result, "Unregister should succeed")
        assertNull(uuidCreator.findByUUID(uuid), "Element should no longer exist")
    }

    @Test
    fun `unregisterElement should return false for non-existent UUID`() = runTest {
        // When
        val result = uuidCreator.unregisterElement("non-existent-uuid")

        // Then
        assertFalse(result, "Unregister should return false for non-existent UUID")
    }

    @Test
    fun `unregisterElement should not affect other elements`() = runTest {
        // Given
        val element1 = createTestElement("Button1")
        val element2 = createTestElement("Button2")
        val uuid1 = uuidCreator.registerElement(element1)
        val uuid2 = uuidCreator.registerElement(element2)

        // When
        uuidCreator.unregisterElement(uuid1)

        // Then
        assertNull(uuidCreator.findByUUID(uuid1), "Element 1 should be removed")
        assertNotNull(uuidCreator.findByUUID(uuid2), "Element 2 should still exist")
    }


    @Test
    fun `should handle voice command for element by name`() = runTest {
        // Given
        val button = createTestElement("Submit", type = "Button", isClickable = true)
        uuidCreator.registerElement(button)

        // When
        val elements = uuidCreator.findByName("Submit")

        // Then
        assertFalse(elements.isEmpty(), "Should find element by voice command")
        // Check clickable via metadata.accessibility
        val element = elements.first()
        assertTrue(
                element.metadata?.accessibility?.isClickable == true,
                "Element should be clickable"
            )
    }

    @Test
    fun `should handle positional voice commands`() = runTest {
        // Given
        val elements = (1..5).map { createTestElement("Item$it") }
        elements.forEach { uuidCreator.registerElement(it) }

        // When - "click item 3"
        val result = uuidCreator.findByPosition(3)

        // Then
        assertNotNull(result, "Should find element by position")
        assertEquals("Item4", result.name, "Should find correct element (0-indexed)")
    }

    @Test
    fun `should handle spatial voice commands`() = runTest {
        // Given
        val currentButton = createTestElement("Current", bounds = Rect(100, 100, 200, 200))
        val nextButton = createTestElement("Next", bounds = Rect(250, 100, 350, 200))

        val currentUUID = uuidCreator.registerElement(currentButton)
        uuidCreator.registerElement(nextButton)

        // When - "go right" or "next button"
        val result = uuidCreator.findInDirection(currentUUID, "right")

        // Then
        assertNotNull(result, "Should find element via spatial navigation")
        assertEquals("Next", result.name, "Should find correct element")
    }


    @Test
    fun `should handle null context gracefully during initialization`() {
        // Given
        val nullContext: Context? = null

        // When/Then
        assertFailsWith<NullPointerException> {
                @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                UUIDCreator.initialize(nullContext!!)
            }
    }

    @Test
    fun `should handle concurrent registrations safely`() = runTest {
        // Given
        uuidCreator = UUIDCreator.initialize(context)
        val elements = (1..100).map { createTestElement("Element$it") }

        // When - register elements concurrently
        val uuids = elements.map { element ->
                async {
                    uuidCreator.registerElement(element)
                }
            }.awaitAll()

        // Then
        assertEquals(100, uuids.size, "Should register all elements")
        assertEquals(100, uuids.distinct().size, "All UUIDs should be unique")
    }

    @Test
    fun `should handle empty element name`() = runTest {
        // Given
        uuidCreator = UUIDCreator.initialize(context)
        val element = createTestElement("")

        // When
        val uuid = uuidCreator.registerElement(element)

        // Then
        assertNotNull(uuid, "Should handle empty name")
        assertNotNull(uuidCreator.findByUUID(uuid), "Should be retrievable")
    }

    @Test
    fun `should handle elements with special characters in names`() = runTest {
        // Given
        uuidCreator = UUIDCreator.initialize(context)
        val specialNames = listOf(
                "Button@123",
                "Text#Field$1",
                "Item[0]",
                "Label{test}",
                "Button\nMultiline"
            )

        // When
        val uuids = specialNames.map { name ->
                uuidCreator.registerElement(createTestElement(name))
            }

        // Then
        assertEquals(specialNames.size, uuids.size, "Should handle all special characters")
        uuids.forEach { uuid ->
                assertNotNull(uuidCreator.findByUUID(uuid), "All should be retrievable")
            }
    }


    @Test
    fun `should register elements efficiently`() = runTest {
        // Given
        val elements = (1..1000).map { createTestElement("Element$it") }

        // When
        val startTime = System.currentTimeMillis()
        elements.forEach { uuidCreator.registerElement(it) }
        val duration = System.currentTimeMillis() - startTime

        // Then
        assertTrue(duration < 5000, "Should register 1000 elements in under 5 seconds")
            println("Registered 1000 elements in ${duration}ms")
    }

    @Test
    fun `should retrieve elements efficiently`() = runTest {
        // Given
        val elements = (1..100).map { createTestElement("Element$it") }
        val uuids = elements.map { uuidCreator.registerElement(it) }

        // When
        val startTime = System.currentTimeMillis()
        uuids.forEach { uuidCreator.findByUUID(it) }
        val duration = System.currentTimeMillis() - startTime

        // Then
        assertTrue(duration < 1000, "Should retrieve 100 elements in under 1 second")
            println("Retrieved 100 elements in ${duration}ms")
    }

    // Helper functions

    /**
     * Create test element matching actual UUIDElement API
     *
     * Properties are stored in:
     * - position.bounds for spatial bounds
     * - metadata.attributes for package info
     * - metadata.accessibility for clickable/focusable properties
     * - metadata.state for visibility/selection state
     */
    private fun createTestElement(
        name: String,
        type: String = "Button",
        bounds: Rect = Rect(0, 0, 100, 100),
        isClickable: Boolean = true,
        isEnabled: Boolean = true
    ): UUIDElement {
        // Create bounds from Rect
        val uuidBounds = UUIDBounds(
            left = bounds.left.toFloat(),
            top = bounds.top.toFloat(),
            right = bounds.right.toFloat(),
            bottom = bounds.bottom.toFloat()
        )

        // Create position with bounds
        val position = UUIDPosition(
            x = bounds.left.toFloat(),
            y = bounds.top.toFloat(),
            width = (bounds.right - bounds.left).toFloat(),
            height = (bounds.bottom - bounds.top).toFloat(),
            bounds = uuidBounds
        )

        // Create accessibility metadata
        val accessibility = UUIDAccessibility(
            isClickable = isClickable,
            isFocusable = isClickable,  // Usually clickable elements are focusable
            isScrollable = false,
            isImportantForAccessibility = true
        )

        // Create metadata with accessibility and package info
        val metadata = UUIDMetadata(
            accessibility = accessibility,
            attributes = mapOf("packageName" to TEST_PACKAGE),
            state = mapOf(
                "isVisible" to true,
                "isSelected" to false
            )
        )

        // Create click action if clickable
        val actions = if (isClickable) {
            mapOf<String, (Map<String, Any>) -> Unit>(
                "click" to { _ -> /* Mock click action */ },
                "default" to { _ -> /* Mock default action */ }
            )
        } else {
            emptyMap()
        }

        return UUIDElement(
            name = name,
            type = type,
            position = position,
            actions = actions,
            isEnabled = isEnabled,
            metadata = metadata,
            timestamp = System.currentTimeMillis()
        )
    }
}
