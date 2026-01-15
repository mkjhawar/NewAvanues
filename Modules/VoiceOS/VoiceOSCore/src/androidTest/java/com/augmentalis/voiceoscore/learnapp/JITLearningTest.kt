/**
 * JITLearningTest.kt - Instrumented tests for JIT (Just-In-Time) learning functionality
 *
 * Tests real-time element capture, element fingerprinting, and VUID generation
 * for the VoiceOS JIT learning system.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code
 * Created: 2025-12-30
 */

package com.augmentalis.voiceoscore.learnapp

import android.content.Context
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import com.augmentalis.voiceoscore.learnapp.jit.JitCapturedElement
import com.augmentalis.voiceoscore.learnapp.jit.JustInTimeLearner
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Field
import java.security.MessageDigest

/**
 * Instrumented tests for JIT Learning functionality.
 *
 * Verifies:
 * - Real-time element capture
 * - Element fingerprinting/hashing
 * - VUID generation for captured elements
 * - Deduplication of already-captured elements
 */
@RunWith(AndroidJUnit4::class)
class JITLearningTest {

    private lateinit var context: Context
    private lateinit var databaseManager: VoiceOSDatabaseManager
    private lateinit var uuidGenerator: ThirdPartyUuidGenerator

    companion object {
        private const val TEST_PACKAGE_NAME = "com.test.jit.learning"
        private const val TEST_SCREEN_HASH = "test_screen_hash_12345"
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Reset singleton to ensure clean state
        resetDatabaseSingleton()

        // Initialize database
        val driverFactory = DatabaseDriverFactory(context)
        databaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)

        // Initialize UUID generator
        uuidGenerator = ThirdPartyUuidGenerator(context)

        // Clean up any existing test data
        cleanupTestData()

        // Create parent scraped_app record for FK constraint
        createParentAppRecord(TEST_PACKAGE_NAME)
    }

    @After
    fun teardown() {
        cleanupTestData()
        resetDatabaseSingleton()
    }

    private fun resetDatabaseSingleton() {
        try {
            val instanceField: Field = VoiceOSDatabaseManager::class.java
                .declaredFields
                .first { it.name == "INSTANCE" }
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            println("Warning: Could not reset singleton: ${e.message}")
        }
    }

    private fun cleanupTestData() {
        try {
            runBlocking {
                // Clear test elements first (child records)
                databaseManager.scrapedElements.deleteByApp(TEST_PACKAGE_NAME)
                // Clear test commands
                databaseManager.generatedCommands.deleteCommandsByPackage(TEST_PACKAGE_NAME)
                // Delete scraped_app record last (parent)
                databaseManager.scrapedAppQueries.deleteById(TEST_PACKAGE_NAME)
            }
        } catch (e: Exception) {
            println("Cleanup error: ${e.message}")
        }
    }

    /**
     * Creates parent scraped_app record required by FK constraint.
     */
    private fun createParentAppRecord(packageName: String) {
        val currentTime = System.currentTimeMillis()
        runBlocking {
            try {
                databaseManager.scrapedAppQueries.insert(
                    appId = packageName,
                    packageName = packageName,
                    versionCode = 1L,
                    versionName = "1.0.0",
                    appHash = "test_app_hash_$packageName",
                    isFullyLearned = 0L,
                    learnCompletedAt = null,
                    scrapingMode = "DYNAMIC",
                    scrapeCount = 0L,
                    elementCount = 0L,
                    commandCount = 0L,
                    firstScrapedAt = currentTime,
                    lastScrapedAt = currentTime,
                    pkg_hash = null
                )
            } catch (e: Exception) {
                println("Parent app record may already exist: ${e.message}")
            }
        }
    }

    // =========================================================================
    // Element Capture Tests
    // =========================================================================

    @Test
    fun testJitCapturedElementCreation() {
        // Given: Element properties
        val className = "android.widget.Button"
        val text = "Submit"
        val bounds = Rect(100, 200, 300, 280)

        // When: Creating JitCapturedElement
        val element = JitCapturedElement.from(
            className = className,
            text = text,
            contentDescription = "Submit button",
            viewIdResourceName = "com.test:id/submit_btn",
            isClickable = true,
            isEnabled = true,
            isScrollable = false,
            bounds = bounds,
            depth = 3,
            indexInParent = 2
        )

        // Then: Element should have correct properties
        assertEquals("ClassName should match", className, element.className)
        assertEquals("Text should match", text, element.text)
        assertEquals("Bounds should match", bounds, element.bounds)
        assertTrue("Element should be clickable", element.isClickable)
        assertTrue("Element should be enabled", element.isEnabled)
        assertEquals("Depth should match", 3, element.depth)
        assertEquals("IndexInParent should match", 2, element.indexInParent)
    }

    @Test
    fun testJitCapturedElementHashGeneration() {
        // Given: Element with specific properties
        val element1 = createJitCapturedElement(
            text = "Button A",
            resourceId = "com.test:id/btn_a",
            bounds = Rect(0, 0, 100, 50)
        )

        val element2 = createJitCapturedElement(
            text = "Button A",
            resourceId = "com.test:id/btn_a",
            bounds = Rect(0, 0, 100, 50)
        )

        // Then: Same properties should produce same hash
        assertEquals(
            "Elements with same properties should have same hash",
            element1.elementHash,
            element2.elementHash
        )
    }

    @Test
    fun testJitCapturedElementHashDiffersForDifferentElements() {
        // Given: Elements with different properties
        val element1 = createJitCapturedElement(
            text = "Button A",
            resourceId = "com.test:id/btn_a"
        )

        val element2 = createJitCapturedElement(
            text = "Button B",
            resourceId = "com.test:id/btn_b"
        )

        // Then: Different properties should produce different hash
        assertNotEquals(
            "Elements with different properties should have different hash",
            element1.elementHash,
            element2.elementHash
        )
    }

    @Test
    fun testJitCapturedElementGetBestLabel() {
        // Given: Elements with various label sources
        val elementWithText = createJitCapturedElement(
            text = "Primary Text",
            contentDescription = "Description",
            resourceId = "com.test:id/element"
        )

        val elementWithDescriptionOnly = createJitCapturedElement(
            text = null,
            contentDescription = "Description Only",
            resourceId = "com.test:id/element"
        )

        val elementWithResourceIdOnly = createJitCapturedElement(
            text = null,
            contentDescription = null,
            resourceId = "com.test:id/element_name"
        )

        // Then: Best label should follow priority (text > description > resourceId)
        assertEquals("Should prefer text", "Primary Text", elementWithText.getBestLabel())
        assertEquals("Should use description when no text", "Description Only", elementWithDescriptionOnly.getBestLabel())
        assertEquals("Should use resourceId suffix when no other label", "element_name", elementWithResourceIdOnly.getBestLabel())
    }

    @Test
    fun testJitCapturedElementHasLabel() {
        // Given: Elements with and without labels
        val elementWithLabel = createJitCapturedElement(text = "Has Label")
        val elementWithoutLabel = createJitCapturedElement(
            text = null,
            contentDescription = null,
            resourceId = null
        )

        // Then: hasLabel should return correct values
        assertTrue("Element with text should have label", elementWithLabel.hasLabel())
        assertFalse("Element without any label should not have label", elementWithoutLabel.hasLabel())
    }

    @Test
    fun testJitCapturedElementIsActionable() {
        // Given: Clickable and non-clickable elements
        val actionableElement = createJitCapturedElement(
            isClickable = true,
            isEnabled = true
        )

        val disabledElement = createJitCapturedElement(
            isClickable = true,
            isEnabled = false
        )

        val nonClickableElement = createJitCapturedElement(
            isClickable = false,
            isEnabled = true
        )

        // Then: isActionable should check both clickable and enabled
        assertTrue("Clickable and enabled should be actionable", actionableElement.isActionable())
        assertFalse("Disabled element should not be actionable", disabledElement.isActionable())
        assertFalse("Non-clickable element should not be actionable", nonClickableElement.isActionable())
    }

    // =========================================================================
    // Element Fingerprinting Tests
    // =========================================================================

    @Test
    fun testElementFingerprintIncludesAllProperties() {
        // Given: Two elements with one differing property
        val baseElement = createJitCapturedElement(
            className = "android.widget.Button",
            text = "Test",
            resourceId = "com.test:id/btn",
            bounds = Rect(0, 0, 100, 50)
        )

        val elementWithDifferentBounds = createJitCapturedElement(
            className = "android.widget.Button",
            text = "Test",
            resourceId = "com.test:id/btn",
            bounds = Rect(10, 10, 110, 60) // Different bounds
        )

        // Then: Hash should differ when bounds differ
        assertNotEquals(
            "Elements with different bounds should have different hash",
            baseElement.elementHash,
            elementWithDifferentBounds.elementHash
        )
    }

    @Test
    fun testElementFingerprintIsDeterministic() {
        // Given: Same element created multiple times
        val elements = (1..5).map {
            createJitCapturedElement(
                className = "android.widget.Button",
                text = "Deterministic Test",
                resourceId = "com.test:id/deterministic_btn",
                bounds = Rect(50, 100, 250, 180)
            )
        }

        // Then: All hashes should be identical
        val firstHash = elements.first().elementHash
        elements.forEach { element ->
            assertEquals(
                "Hash should be deterministic",
                firstHash,
                element.elementHash
            )
        }
    }

    // =========================================================================
    // VUID Generation Tests
    // =========================================================================

    @Test
    fun testWithUuidCreatesNewInstance() {
        // Given: Element without UUID
        val originalElement = createJitCapturedElement(text = "Original")
        assertNull("Original should have no UUID", originalElement.uuid)

        // When: Adding UUID
        val newUuid = "test-uuid-12345"
        val elementWithUuid = originalElement.withUuid(newUuid)

        // Then: New instance should have UUID, original unchanged
        assertEquals("New element should have UUID", newUuid, elementWithUuid.uuid)
        assertNull("Original element should still have no UUID", originalElement.uuid)
    }

    @Test
    fun testVUIDCompactFormatGeneration() {
        // Given: Package name and element data for VUID generation
        val packageName = "com.instagram.android"
        val version = "12.0.0"
        val elementType = "button"
        val elementHash = "a7f3e2c1"

        // When: Creating compact VUID format
        val vuid = com.augmentalis.avid.core.AvidGenerator.generateCompact(
            packageName = packageName,
            version = version,
            typeName = elementType,
            elementHash = elementHash
        )

        // Then: VUID should follow compact format
        assertTrue("VUID should not be empty", vuid.isNotEmpty())
        assertTrue("VUID should contain reversed package", vuid.contains("android.instagram.com"))
        assertTrue("VUID should contain version", vuid.contains(version))
        assertTrue("VUID should contain element type abbreviation", vuid.contains("btn"))
        assertTrue("VUID should contain element hash", vuid.contains(elementHash))
    }

    // =========================================================================
    // Database Persistence Tests
    // =========================================================================

    @Test
    fun testElementPersistenceToDatabase() = runBlocking {
        // Given: JIT captured element
        val element = createJitCapturedElement(
            text = "Persist Test",
            resourceId = "com.test:id/persist_test"
        )

        // When: Persisting to database
        val elementDTO = ScrapedElementDTO(
            id = 0L,
            elementHash = element.elementHash,
            appId = TEST_PACKAGE_NAME,
            uuid = "test-uuid-persist",
            className = element.className,
            viewIdResourceName = element.viewIdResourceName,
            text = element.text,
            contentDescription = element.contentDescription,
            bounds = "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
            isClickable = if (element.isClickable) 1L else 0L,
            isLongClickable = if (element.isLongClickable) 1L else 0L,
            isEditable = if (element.isEditable) 1L else 0L,
            isScrollable = if (element.isScrollable) 1L else 0L,
            isCheckable = if (element.isCheckable) 1L else 0L,
            isFocusable = if (element.isFocusable) 1L else 0L,
            isEnabled = if (element.isEnabled) 1L else 0L,
            depth = element.depth.toLong(),
            indexInParent = element.indexInParent.toLong(),
            scrapedAt = System.currentTimeMillis(),
            semanticRole = null,
            inputType = null,
            visualWeight = null,
            isRequired = null,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = TEST_SCREEN_HASH
        )

        databaseManager.scrapedElements.insert(elementDTO)

        // Then: Element should be retrievable
        val retrieved = databaseManager.scrapedElements.getByHash(element.elementHash)
        assertNotNull("Element should be retrievable", retrieved)
        assertEquals("Text should match", element.text, retrieved?.text)
        assertEquals("UUID should match", "test-uuid-persist", retrieved?.uuid)
    }

    @Test
    fun testScreenHashBasedElementQuery() = runBlocking {
        // Given: Elements with screen hash
        val elements = listOf(
            createJitCapturedElement(text = "Screen1 Element A"),
            createJitCapturedElement(text = "Screen1 Element B")
        )

        // Insert elements with screen hash
        elements.forEach { element ->
            val elementDTO = createScrapedElementDTO(element, screenHash = TEST_SCREEN_HASH)
            databaseManager.scrapedElements.insert(elementDTO)
        }

        // When: Counting elements by screen hash
        val count = databaseManager.scrapedElements.countByScreenHash(
            TEST_PACKAGE_NAME,
            TEST_SCREEN_HASH
        )

        // Then: Should return correct count
        assertEquals("Should have 2 elements for screen hash", 2, count)
    }

    @Test
    fun testElementUuidQuery() = runBlocking {
        // Given: Element with specific UUID
        val testUuid = "unique-uuid-for-query-test"
        val element = createJitCapturedElement(
            text = "UUID Query Test",
            uuid = testUuid
        )

        val elementDTO = createScrapedElementDTO(element.copy(uuid = testUuid))
        databaseManager.scrapedElements.insert(elementDTO)

        // When: Querying by UUID
        val retrieved = databaseManager.scrapedElements.getByUuid(TEST_PACKAGE_NAME, testUuid)

        // Then: Should find the element
        assertNotNull("Element should be found by UUID", retrieved)
        assertEquals("UUID should match", testUuid, retrieved?.uuid)
        assertEquals("Text should match", element.text, retrieved?.text)
    }

    // =========================================================================
    // JustInTimeLearner State Tests
    // =========================================================================

    @Test
    fun testJitStatsInitialization() {
        // Given: Fresh JIT learner instance stats
        // Note: We can't create JustInTimeLearner without AccessibilityService
        // But we can test the JITStats data class

        // When: Creating initial stats
        val stats = JustInTimeLearner.JITStats(
            screensLearned = 0,
            elementsDiscovered = 0,
            currentPackage = null,
            isActive = false
        )

        // Then: Stats should reflect initial state
        assertEquals("Initial screens learned should be 0", 0, stats.screensLearned)
        assertEquals("Initial elements discovered should be 0", 0, stats.elementsDiscovered)
        assertNull("Initial package should be null", stats.currentPackage)
        assertFalse("Should not be active initially", stats.isActive)
    }

    @Test
    fun testJitHashMetrics() {
        // Given: Hash metrics data
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 75,
            rescanned = 25,
            skipPercentage = 75.0f
        )

        // Then: Should correctly report optimization effectiveness
        assertTrue("75% skip rate should be effective", metrics.isOptimizationEffective())
        assertTrue("Summary should contain percentage", metrics.getSummary().contains("75.0%"))
    }

    @Test
    fun testJitHashMetricsIneffective() {
        // Given: Low skip rate
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 50,
            rescanned = 50,
            skipPercentage = 50.0f
        )

        // Then: Should report as not effective (below 70% threshold)
        assertFalse("50% skip rate should not be effective", metrics.isOptimizationEffective())
    }

    @Test
    fun testDeepScanResult() {
        // Given: Successful deep scan result
        val successResult = JustInTimeLearner.DeepScanResult(
            success = true,
            expandablesFound = 3,
            expandablesScanned = 3,
            newElementsDiscovered = 15,
            duration = 1500
        )

        // Then: Should format correctly
        val resultString = successResult.toString()
        assertTrue("Should indicate scan count", resultString.contains("3/3"))
        assertTrue("Should show elements discovered", resultString.contains("15"))
        assertTrue("Should show duration", resultString.contains("1500ms"))
    }

    @Test
    fun testDeepScanResultFailure() {
        // Given: Failed deep scan result
        val failureResult = JustInTimeLearner.DeepScanResult(
            success = false,
            expandablesFound = 0,
            expandablesScanned = 0,
            newElementsDiscovered = 0,
            duration = 100,
            error = "Root node unavailable"
        )

        // Then: Should indicate failure
        assertFalse("Should not be successful", failureResult.success)
        val resultString = failureResult.toString()
        assertTrue("Should indicate failure", resultString.contains("Failed"))
        assertTrue("Should show error message", resultString.contains("Root node unavailable"))
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private fun createJitCapturedElement(
        className: String = "android.widget.Button",
        text: String? = null,
        contentDescription: String? = null,
        resourceId: String? = null,
        isClickable: Boolean = true,
        isEnabled: Boolean = true,
        isScrollable: Boolean = false,
        bounds: Rect = Rect(0, 0, 100, 50),
        uuid: String? = null
    ): JitCapturedElement {
        return JitCapturedElement.from(
            className = className,
            text = text,
            contentDescription = contentDescription,
            viewIdResourceName = resourceId,
            isClickable = isClickable,
            isEnabled = isEnabled,
            isScrollable = isScrollable,
            bounds = bounds
        ).let {
            if (uuid != null) it.withUuid(uuid) else it
        }
    }

    private fun createScrapedElementDTO(
        element: JitCapturedElement,
        screenHash: String = TEST_SCREEN_HASH
    ): ScrapedElementDTO {
        return ScrapedElementDTO(
            id = 0L,
            elementHash = element.elementHash,
            appId = TEST_PACKAGE_NAME,
            uuid = element.uuid,
            className = element.className,
            viewIdResourceName = element.viewIdResourceName,
            text = element.text,
            contentDescription = element.contentDescription,
            bounds = "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
            isClickable = if (element.isClickable) 1L else 0L,
            isLongClickable = if (element.isLongClickable) 1L else 0L,
            isEditable = if (element.isEditable) 1L else 0L,
            isScrollable = if (element.isScrollable) 1L else 0L,
            isCheckable = if (element.isCheckable) 1L else 0L,
            isFocusable = if (element.isFocusable) 1L else 0L,
            isEnabled = if (element.isEnabled) 1L else 0L,
            depth = element.depth.toLong(),
            indexInParent = element.indexInParent.toLong(),
            scrapedAt = System.currentTimeMillis(),
            semanticRole = null,
            inputType = null,
            visualWeight = null,
            isRequired = null,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = screenHash
        )
    }
}
