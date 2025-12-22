/**
 * EdgeCaseTest.kt - Comprehensive edge case tests for JITLearning and LearnAppCore
 *
 * Tests boundary conditions, timing edge cases, data edge cases, and cache edge cases
 * to ensure robust handling of extreme and unusual scenarios.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-LearnApp-Phase2-Tests-51211-V1.md
 *
 * @since 2.1.0 (Phase 2 Integration Tests)
 */

package com.augmentalis.jitlearning

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ServiceTestRule
import com.augmentalis.jitlearning.handlers.NodeCacheManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Edge Case Tests
 *
 * Comprehensive tests covering:
 * - Boundary conditions (empty/massive UI trees)
 * - Timing edge cases (rapid operations, concurrency)
 * - Data edge cases (extreme values, unicode, nulls)
 * - Cache edge cases (capacity, eviction, concurrency)
 *
 * Performance benchmarks included for large data sets.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class EdgeCaseTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private lateinit var mockProvider: JITLearnerProvider
    private lateinit var nodeCacheManager: NodeCacheManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockProvider = mockk(relaxed = true)
        nodeCacheManager = NodeCacheManager(maxCacheSize = 100)

        // Setup default mock behaviors
        every { mockProvider.isLearningActive() } returns true
        every { mockProvider.isLearningPaused() } returns false
        every { mockProvider.getScreensLearnedCount() } returns 0
        every { mockProvider.getElementsDiscoveredCount() } returns 0
        every { mockProvider.getCurrentPackage() } returns "com.test.app"
    }

    @After
    fun teardown() {
        nodeCacheManager.clear()
    }

    // ================================================================
    // BOUNDARY CONDITIONS - UI Tree Tests
    // ================================================================

    /**
     * Test: EmptyUITree_HandlesGracefully
     *
     * Verifies that processing an empty UI tree (root with no children)
     * doesn't crash and returns appropriate empty results.
     */
    @Test
    fun emptyUITree_HandlesGracefully() {
        // Arrange: Create root node with no children
        val rootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { rootNode.childCount } returns 0
        every { rootNode.className } returns "android.view.ViewGroup"
        every { rootNode.text } returns null
        every { rootNode.contentDescription } returns null
        every { rootNode.viewIdResourceName } returns null
        val bounds = Rect(0, 0, 1080, 1920)
        every { rootNode.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(bounds)
        }

        // Act: Convert to ParcelableNodeInfo with children
        val parcelable = ParcelableNodeInfo.fromAccessibilityNode(
            rootNode,
            includeChildren = true,
            maxDepth = 10
        )

        // Assert: Empty tree processed successfully
        assertNotNull("Parcelable should not be null", parcelable)
        assertEquals("Should have no children", 0, parcelable.children.size)
        assertEquals("Node count should be 1", 1, parcelable.getTotalNodeCount())
    }

    /**
     * Test: MaximumDepthUITree_HandlesCorrectly
     *
     * Verifies that deeply nested UI trees (50+ levels) are processed
     * correctly with proper depth limiting to prevent stack overflow.
     */
    @Test
    fun maximumDepthUITree_HandlesCorrectly() {
        // Arrange: Create deeply nested tree (50 levels)
        val deepestDepth = 50
        var currentNode = createMockNode("Root", 0, 0)
        val rootNode = currentNode

        // Build chain of 50 nested children
        for (depth in 1..deepestDepth) {
            val childNode = createMockNode("Child$depth", depth * 10, depth * 10)
            every { currentNode.childCount } returns 1
            every { currentNode.getChild(0) } returns childNode
            currentNode = childNode
        }
        // Terminal node has no children
        every { currentNode.childCount } returns 0

        // Act: Convert with maxDepth = 10 (should limit recursion)
        val startTime = SystemClock.elapsedRealtime()
        val parcelable = ParcelableNodeInfo.fromAccessibilityNode(
            rootNode,
            includeChildren = true,
            maxDepth = 10
        )
        val elapsedMs = SystemClock.elapsedRealtime() - startTime

        // Assert: Depth limited to maxDepth
        assertEquals("Should reach maxDepth", 10, parcelable.depth + getMaxChildDepth(parcelable))
        assertTrue("Should complete quickly (< 500ms)", elapsedMs < 500)
        assertNotNull("Parcelable should not be null", parcelable)
    }

    /**
     * Test: MaximumWidthUITree_HandlesCorrectly
     *
     * Verifies that wide UI trees (1000+ children at one level) are
     * processed efficiently without performance degradation.
     *
     * Performance benchmark: Should complete in < 2 seconds.
     */
    @Test
    fun maximumWidthUITree_HandlesCorrectly() {
        // Arrange: Create root with 1000 children
        val childCount = 1000
        val rootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { rootNode.childCount } returns childCount
        every { rootNode.className } returns "android.view.ViewGroup"

        // Create 1000 child nodes
        for (i in 0 until childCount) {
            val childNode = createMockNode("Child$i", i * 10, 100)
            every { childNode.childCount } returns 0
            every { rootNode.getChild(i) } returns childNode
        }

        val rootBounds = Rect(0, 0, 1080, 1920)
        every { rootNode.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(rootBounds)
        }

        // Act: Convert with children (benchmark performance)
        val startTime = SystemClock.elapsedRealtime()
        val parcelable = ParcelableNodeInfo.fromAccessibilityNode(
            rootNode,
            includeChildren = true,
            maxDepth = 2
        )
        val elapsedMs = SystemClock.elapsedRealtime() - startTime

        // Assert: All children processed efficiently
        assertEquals("Should have all children", childCount, parcelable.children.size)
        assertEquals("Total nodes should be 1001", 1001, parcelable.getTotalNodeCount())
        assertTrue("Should complete in < 2 seconds", elapsedMs < 2000)
    }

    /**
     * Test: ZeroSizeViews_HandledCorrectly
     *
     * Verifies that views with zero width/height bounds are processed
     * without errors and marked appropriately.
     */
    @Test
    fun zeroSizeViews_HandledCorrectly() {
        // Arrange: Create node with zero-size bounds
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.className } returns "android.view.View"
        every { node.text } returns "Invisible"
        every { node.childCount } returns 0
        val zeroBounds = Rect(100, 100, 100, 100) // Zero width and height
        every { node.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(zeroBounds)
        }

        // Act: Convert to parcelable
        val parcelable = ParcelableNodeInfo.fromAccessibilityNode(node, includeChildren = false)

        // Assert: Zero-size bounds preserved
        assertEquals("Left bound should match", 100, parcelable.boundsLeft)
        assertEquals("Right bound should match", 100, parcelable.boundsRight)
        assertEquals("Top bound should match", 100, parcelable.boundsTop)
        assertEquals("Bottom bound should match", 100, parcelable.boundsBottom)
        assertEquals("Width should be zero", 0, parcelable.getBounds().width())
        assertEquals("Height should be zero", 0, parcelable.getBounds().height())
    }

    /**
     * Test: OffScreenElements_HandledCorrectly
     *
     * Verifies that elements positioned outside screen bounds (negative
     * coordinates, beyond screen dimensions) are processed correctly.
     */
    @Test
    fun offScreenElements_HandledCorrectly() {
        // Arrange: Create nodes with off-screen bounds
        val offScreenCases = listOf(
            Rect(-100, -100, 0, 0),           // Negative coordinates
            Rect(2000, 3000, 2100, 3100),     // Beyond screen (1080x1920)
            Rect(-50, 100, 50, 200),          // Partially off-screen left
            Rect(1000, -50, 1080, 50)         // Partially off-screen top
        )

        offScreenCases.forEach { bounds ->
            val node = createMockNodeWithBounds("OffScreen", bounds)

            // Act: Convert to parcelable
            val parcelable = ParcelableNodeInfo.fromAccessibilityNode(node, includeChildren = false)

            // Assert: Bounds preserved as-is (no clamping)
            assertEquals("Bounds should be preserved", bounds, parcelable.getBounds())
        }
    }

    /**
     * Test: OverlappingElements_HandledDistinctly
     *
     * Verifies that overlapping elements (same bounds, different properties)
     * generate distinct UUIDs and can be differentiated.
     */
    @Test
    fun overlappingElements_HandledDistinctly() {
        // Arrange: Two elements with same bounds but different content
        val sameBounds = Rect(100, 200, 300, 400)

        val node1 = createMockNodeWithBounds("Button A", sameBounds)
        every { node1.className } returns "android.widget.Button"

        val node2 = createMockNodeWithBounds("Button B", sameBounds)
        every { node2.className } returns "android.widget.Button"

        // Act: Generate UUIDs for both
        val uuid1 = nodeCacheManager.generateNodeUuid(node1)
        val uuid2 = nodeCacheManager.generateNodeUuid(node2)

        // Assert: UUIDs are different despite same bounds
        assertNotEquals("Overlapping elements should have different UUIDs", uuid1, uuid2)
    }

    // ================================================================
    // TIMING EDGE CASES
    // ================================================================

    /**
     * Test: RapidPauseResumeCycles_MaintainsState
     *
     * Verifies that rapid pause/resume cycles don't cause state corruption
     * or race conditions.
     */
    @Test
    fun rapidPauseResumeCycles_MaintainsState() = runBlocking {
        // Arrange: Bind to service
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)
        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider)

        // Act: Rapid pause/resume cycles (20 times)
        val cycleCount = 20
        repeat(cycleCount) { i ->
            service.pauseCapture()
            delay(10) // Small delay between operations
            service.resumeCapture()
            delay(10)
        }

        // Allow state to stabilize
        delay(100)

        // Assert: Final state is consistent
        val finalState = service.queryState()
        assertNotNull("State should not be null", finalState)
        // Service should be in resumed state (last operation was resume)
        verify(atLeast = cycleCount) { mockProvider.pauseLearning() }
        verify(atLeast = cycleCount) { mockProvider.resumeLearning() }
    }

    /**
     * Test: ConcurrentScreenCaptures_HandledSafely
     *
     * Verifies that concurrent screen capture requests don't cause
     * crashes or data corruption.
     */
    @Test
    fun concurrentScreenCaptures_HandledSafely() = runBlocking {
        // Arrange: Bind to service
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)
        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider)

        val rootNode = createMockNode("Root", 0, 0)
        every { mockProvider.getCurrentRootNode() } returns rootNode

        // Act: Launch 10 concurrent screen capture requests
        val results = (1..10).map {
            async {
                service.getCurrentScreenInfo()
            }
        }.awaitAll()

        // Assert: All requests completed successfully
        assertEquals("Should have 10 results", 10, results.size)
        results.forEach { result ->
            assertNotNull("Each result should not be null", result)
        }
    }

    /**
     * Test: ServiceStartDuringActiveCapture_HandlesGracefully
     *
     * Verifies that starting the service while capture is active
     * doesn't cause conflicts or state corruption.
     */
    @Test
    fun serviceStartDuringActiveCapture_HandlesGracefully() {
        // Arrange: Start service
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)
        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider)

        // Simulate active capture
        service.resumeCapture()

        // Act: Try to "restart" by getting instance again
        val retrievedInstance = JITLearningService.getInstance()

        // Assert: Same instance returned (singleton pattern)
        assertSame("Should return same service instance", serviceInstance, retrievedInstance)

        val state = service.queryState()
        assertNotNull("State should be valid", state)
    }

    /**
     * Test: ScreenRotationDuringCapture_HandlesCorrectly
     *
     * Verifies that screen rotation during capture doesn't cause
     * crashes and updates dimensions appropriately.
     */
    @Test
    fun screenRotationDuringCapture_HandlesCorrectly() {
        // Arrange: Create nodes with portrait dimensions
        val portraitBounds = Rect(0, 0, 1080, 1920)
        val node = createMockNodeWithBounds("Button", portraitBounds)

        // Act: Convert to parcelable (portrait)
        val portraitParcelable = ParcelableNodeInfo.fromAccessibilityNode(node, includeChildren = false)

        // Simulate rotation by changing bounds to landscape
        val landscapeBounds = Rect(0, 0, 1920, 1080)
        every { node.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(landscapeBounds)
        }

        // Convert again (landscape)
        val landscapeParcelable = ParcelableNodeInfo.fromAccessibilityNode(node, includeChildren = false)

        // Assert: Dimensions updated correctly
        assertEquals("Portrait width", 1080, portraitParcelable.boundsRight)
        assertEquals("Portrait height", 1920, portraitParcelable.boundsBottom)
        assertEquals("Landscape width", 1920, landscapeParcelable.boundsRight)
        assertEquals("Landscape height", 1080, landscapeParcelable.boundsBottom)
    }

    /**
     * Test: AppSwitchingDuringCapture_HandlesGracefully
     *
     * Verifies that app switching during capture doesn't cause crashes
     * and properly updates package context.
     */
    @Test
    fun appSwitchingDuringCapture_HandlesGracefully() {
        // Arrange: Bind to service
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)
        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider)

        // Simulate app switch by changing current package
        every { mockProvider.getCurrentPackage() } returns "com.app.first"
        var state = service.queryState()
        assertEquals("First app package", "com.app.first", state.currentPackage)

        // Switch to different app
        every { mockProvider.getCurrentPackage() } returns "com.app.second"
        state = service.queryState()
        assertEquals("Second app package", "com.app.second", state.currentPackage)
    }

    // ================================================================
    // DATA EDGE CASES
    // ================================================================

    /**
     * Test: EmptyTextFields_HandledCorrectly
     *
     * Verifies that elements with empty text/contentDescription are
     * processed without errors and fallback labels generated.
     */
    @Test
    fun emptyTextFields_HandledCorrectly() {
        // Arrange: Create node with all empty text fields
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.className } returns "android.widget.Button"
        every { node.text } returns null
        every { node.contentDescription } returns null
        every { node.viewIdResourceName } returns null
        every { node.childCount } returns 0
        val bounds = Rect(0, 0, 100, 100)
        every { node.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(bounds)
        }

        // Act: Convert to parcelable
        val parcelable = ParcelableNodeInfo.fromAccessibilityNode(node, includeChildren = false)

        // Assert: Empty strings stored (not null)
        assertEquals("Text should be empty string", "", parcelable.text)
        assertEquals("ContentDescription should be empty", "", parcelable.contentDescription)
        assertEquals("ResourceId should be empty", "", parcelable.resourceId)
        // Display name should fall back to "Unknown"
        assertEquals("Display name should be Unknown", "Unknown", parcelable.getDisplayName())
    }

    /**
     * Test: MaximumLengthText_HandlesCorrectly
     *
     * Verifies that elements with extremely long text (10000+ chars)
     * are processed without truncation or errors.
     *
     * Performance benchmark: Should complete in < 500ms.
     */
    @Test
    fun maximumLengthText_HandlesCorrectly() {
        // Arrange: Create node with 10,000 character text
        val longText = "A".repeat(10000)
        val node = createMockNode(longText, 0, 0)

        // Act: Convert to parcelable (benchmark)
        val startTime = SystemClock.elapsedRealtime()
        val parcelable = ParcelableNodeInfo.fromAccessibilityNode(node, includeChildren = false)
        val elapsedMs = SystemClock.elapsedRealtime() - startTime

        // Assert: Full text preserved
        assertEquals("Text should be preserved fully", 10000, parcelable.text.length)
        assertEquals("Text content should match", longText, parcelable.text)
        assertTrue("Should complete quickly (< 500ms)", elapsedMs < 500)
    }

    /**
     * Test: UnicodeAndEmojiText_HandlesCorrectly
     *
     * Verifies that Unicode characters, emojis, and special characters
     * are preserved correctly without corruption.
     */
    @Test
    fun unicodeAndEmojiText_HandlesCorrectly() {
        // Arrange: Various Unicode test cases
        val testCases = listOf(
            "Hello 世界",                    // Chinese
            "مرحبا",                         // Arabic
            "Привет",                        // Cyrillic
            "🎉🎊🎈",                        // Emojis
            "😀👍🏻🎯",                      // Emojis with skin tones
            "Test\nNew\tLine",              // Special chars
            "A\u0000B",                      // Null character
            "Test©®™"                        // Symbols
        )

        testCases.forEach { testText ->
            val node = createMockNode(testText, 0, 0)

            // Act: Convert to parcelable
            val parcelable = ParcelableNodeInfo.fromAccessibilityNode(node, includeChildren = false)

            // Assert: Text preserved exactly
            assertEquals("Unicode text should be preserved: $testText", testText, parcelable.text)
        }
    }

    /**
     * Test: NullDescriptions_HandledSafely
     *
     * Verifies that null values in all text fields don't cause
     * NullPointerExceptions and are converted to empty strings.
     */
    @Test
    fun nullDescriptions_HandledSafely() {
        // Arrange: Create node with explicit nulls
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.className } returns null
        every { node.text } returns null
        every { node.contentDescription } returns null
        every { node.viewIdResourceName } returns null
        every { node.childCount } returns 0
        val bounds = Rect(0, 0, 100, 100)
        every { node.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(bounds)
        }

        // Act: Convert to parcelable
        val parcelable = ParcelableNodeInfo.fromAccessibilityNode(node, includeChildren = false)

        // Assert: All nulls converted to empty strings
        assertEquals("ClassName should be empty", "", parcelable.className)
        assertEquals("Text should be empty", "", parcelable.text)
        assertEquals("ContentDescription should be empty", "", parcelable.contentDescription)
        assertEquals("ResourceId should be empty", "", parcelable.resourceId)
    }

    /**
     * Test: DuplicateElementIDs_GenerateUniqueUUIDs
     *
     * Verifies that elements with same resourceId but different properties
     * generate unique UUIDs.
     */
    @Test
    fun duplicateElementIDs_GenerateUniqueUUIDs() {
        // Arrange: Two nodes with same resourceId but different text
        val sharedResourceId = "com.app:id/button"

        val node1 = createMockNode("Button 1", 0, 0)
        every { node1.viewIdResourceName } returns sharedResourceId

        val node2 = createMockNode("Button 2", 0, 100)
        every { node2.viewIdResourceName } returns sharedResourceId

        // Act: Generate UUIDs
        val uuid1 = nodeCacheManager.generateNodeUuid(node1)
        val uuid2 = nodeCacheManager.generateNodeUuid(node2)

        // Assert: Different UUIDs despite same resourceId
        assertNotEquals("Duplicate IDs should generate unique UUIDs", uuid1, uuid2)
    }

    /**
     * Test: CircularReferences_PreventedByDepthLimit
     *
     * Verifies that circular references in node hierarchy don't cause
     * infinite recursion due to maxDepth limit.
     */
    @Test
    fun circularReferences_PreventedByDepthLimit() {
        // Arrange: Create circular reference (parent -> child -> parent)
        val parentNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        val childNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { parentNode.className } returns "Parent"
        every { parentNode.childCount } returns 1
        every { parentNode.getChild(0) } returns childNode
        setupMockNodeBounds(parentNode, Rect(0, 0, 100, 100))

        every { childNode.className } returns "Child"
        every { childNode.childCount } returns 1
        every { childNode.getChild(0) } returns parentNode // Circular!
        setupMockNodeBounds(childNode, Rect(0, 0, 50, 50))

        // Act: Convert with limited depth (should prevent infinite loop)
        val startTime = SystemClock.elapsedRealtime()
        val parcelable = ParcelableNodeInfo.fromAccessibilityNode(
            parentNode,
            includeChildren = true,
            maxDepth = 5
        )
        val elapsedMs = SystemClock.elapsedRealtime() - startTime

        // Assert: Completed without infinite loop
        assertTrue("Should complete quickly (< 500ms)", elapsedMs < 500)
        assertNotNull("Parcelable should not be null", parcelable)
        assertTrue("Should have limited depth", parcelable.getTotalNodeCount() < 10)
    }

    // ================================================================
    // CACHE EDGE CASES
    // ================================================================

    /**
     * Test: CacheAtExactCapacity_HandlesCorrectly
     *
     * Verifies that filling cache to exact capacity (100 nodes) works
     * correctly and evicts oldest entry on next insert.
     */
    @Test
    fun cacheAtExactCapacity_HandlesCorrectly() {
        // Arrange: Create cache and fill to exact capacity (100)
        val cache = NodeCacheManager(maxCacheSize = 100)
        val nodes = (0 until 100).map { i ->
            createMockNode("Node$i", i * 10, 0)
        }

        // Act: Cache all 100 nodes
        nodes.forEach { node ->
            val uuid = cache.generateNodeUuid(node)
            cache.cacheNode(uuid, node)
        }

        // Assert: Cache at exact capacity
        assertEquals("Cache should be at capacity", 100, cache.getCacheSize())

        // Add one more node (should evict oldest)
        val extraNode = createMockNode("Extra", 1000, 0)
        val extraUuid = cache.generateNodeUuid(extraNode)
        cache.cacheNode(extraUuid, extraNode)

        // Cache should still be at capacity (evicted oldest)
        assertEquals("Cache should remain at capacity", 100, cache.getCacheSize())
    }

    /**
     * Test: CacheEvictionDuringAccess_HandlesRaceCondition
     *
     * Verifies that accessing a node that's being evicted doesn't cause
     * race conditions or crashes.
     */
    @Test
    fun cacheEvictionDuringAccess_HandlesRaceCondition() = runBlocking {
        // Arrange: Small cache (10 entries)
        val cache = NodeCacheManager(maxCacheSize = 10)
        val nodes = (0 until 15).map { i ->
            createMockNode("Node$i", i * 10, 0)
        }

        // Cache first 10 nodes
        val uuids = nodes.take(10).map { node ->
            val uuid = cache.generateNodeUuid(node)
            cache.cacheNode(uuid, node)
            uuid
        }

        // Act: Concurrent operations
        // Thread 1: Access cached nodes
        // Thread 2: Add new nodes (triggering evictions)
        val accessCount = AtomicInteger(0)

        val accessTask = async {
            repeat(20) {
                uuids.forEach { uuid ->
                    cache.getNode(uuid)
                    accessCount.incrementAndGet()
                }
                delay(10)
            }
        }

        val evictionTask = async {
            nodes.drop(10).forEach { node ->
                val uuid = cache.generateNodeUuid(node)
                cache.cacheNode(uuid, node)
                delay(20)
            }
        }

        // Wait for both tasks
        accessTask.await()
        evictionTask.await()

        // Assert: No crashes, all operations completed
        assertTrue("Access operations should complete", accessCount.get() > 0)
        assertTrue("Cache size should be at capacity", cache.getCacheSize() <= 10)
    }

    /**
     * Test: ConcurrentCacheModifications_ThreadSafe
     *
     * Verifies that concurrent cache insertions from multiple threads
     * don't cause data corruption or crashes.
     */
    @Test
    fun concurrentCacheModifications_ThreadSafe() = runBlocking {
        // Arrange: Shared cache
        val cache = NodeCacheManager(maxCacheSize = 100)
        val operationCount = 50

        // Act: Launch 5 concurrent tasks, each caching 50 nodes
        val tasks = (1..5).map { taskId ->
            async {
                repeat(operationCount) { i ->
                    val node = createMockNode("Task${taskId}_Node$i", i * 10, taskId * 100)
                    val uuid = cache.generateNodeUuid(node)
                    cache.cacheNode(uuid, node)
                    delay(5) // Small delay to increase contention
                }
            }
        }

        // Wait for all tasks
        tasks.awaitAll()

        // Assert: Cache state is valid
        assertTrue("Cache size should be reasonable", cache.getCacheSize() > 0)
        assertTrue("Cache size should not exceed capacity", cache.getCacheSize() <= 100)
    }

    /**
     * Test: CacheClearDuringIteration_HandlesGracefully
     *
     * Verifies that clearing cache while iterating doesn't cause
     * ConcurrentModificationException or crashes.
     */
    @Test
    fun cacheClearDuringIteration_HandlesGracefully() = runBlocking {
        // Arrange: Cache with some nodes
        val cache = NodeCacheManager(maxCacheSize = 50)
        val nodes = (0 until 30).map { i ->
            createMockNode("Node$i", i * 10, 0)
        }

        nodes.forEach { node ->
            val uuid = cache.generateNodeUuid(node)
            cache.cacheNode(uuid, node)
        }

        // Act: Build cache (iteration) and clear concurrently
        val rootNode = createMockNode("Root", 0, 0)
        every { rootNode.childCount } returns 0

        val buildTask = async {
            cache.buildCache(rootNode)
        }

        val clearTask = async {
            delay(10) // Let build start
            cache.clear()
        }

        // Wait for both
        buildTask.await()
        clearTask.await()

        // Assert: Cache cleared successfully
        assertEquals("Cache should be empty after clear", 0, cache.getCacheSize())
    }

    /**
     * Test: LargeDataSetPerformance_MeetsBenchmark
     *
     * Performance benchmark: Processing 500 nodes should complete in < 5 seconds.
     *
     * Verifies that large-scale operations maintain acceptable performance.
     */
    @Test
    fun largeDataSetPerformance_MeetsBenchmark() {
        // Arrange: Create tree with 500 nodes (10 children each, 50 parents)
        val parentCount = 50
        val childrenPerParent = 10
        val rootNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { rootNode.className } returns "Root"
        every { rootNode.childCount } returns parentCount
        setupMockNodeBounds(rootNode, Rect(0, 0, 1080, 1920))

        // Create parent nodes
        for (p in 0 until parentCount) {
            val parentNode = createMockNode("Parent$p", 0, p * 100)
            every { parentNode.childCount } returns childrenPerParent
            every { rootNode.getChild(p) } returns parentNode

            // Create children for each parent
            for (c in 0 until childrenPerParent) {
                val childNode = createMockNode("Child${p}_$c", c * 100, p * 100)
                every { childNode.childCount } returns 0
                every { parentNode.getChild(c) } returns childNode
            }
        }

        // Act: Convert entire tree (benchmark)
        val startTime = SystemClock.elapsedRealtime()
        val parcelable = ParcelableNodeInfo.fromAccessibilityNode(
            rootNode,
            includeChildren = true,
            maxDepth = 3
        )
        val elapsedMs = SystemClock.elapsedRealtime() - startTime

        // Assert: Performance benchmark met
        assertTrue("Should process 500+ nodes in < 5 seconds (was ${elapsedMs}ms)", elapsedMs < 5000)
        assertEquals("Should have all parent nodes", parentCount, parcelable.children.size)
        val totalNodes = 1 + parentCount + (parentCount * childrenPerParent)
        assertEquals("Total node count should be ~551", totalNodes, parcelable.getTotalNodeCount())
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Create a mock AccessibilityNodeInfo with default properties.
     */
    private fun createMockNode(text: String, x: Int, y: Int): AccessibilityNodeInfo {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.className } returns "android.widget.Button"
        every { node.text } returns text
        every { node.contentDescription } returns null
        every { node.viewIdResourceName } returns null
        every { node.childCount } returns 0
        every { node.isClickable } returns true
        every { node.isEnabled } returns true
        every { node.isPassword } returns false
        every { node.isScrollable } returns false
        every { node.isEditable } returns false
        every { node.isCheckable } returns false
        every { node.isChecked } returns false
        every { node.isFocusable } returns false
        every { node.isLongClickable } returns false

        val bounds = Rect(x, y, x + 100, y + 100)
        every { node.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(bounds)
        }

        return node
    }

    /**
     * Create a mock node with specific bounds.
     */
    private fun createMockNodeWithBounds(text: String, bounds: Rect): AccessibilityNodeInfo {
        val node = createMockNode(text, 0, 0)
        every { node.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(bounds)
        }
        return node
    }

    /**
     * Setup mock node bounds.
     */
    private fun setupMockNodeBounds(node: AccessibilityNodeInfo, bounds: Rect) {
        every { node.getBoundsInScreen(any()) } answers {
            firstArg<Rect>().set(bounds)
        }
    }

    /**
     * Calculate maximum depth of child hierarchy.
     */
    private fun getMaxChildDepth(node: ParcelableNodeInfo): Int {
        if (node.children.isEmpty()) return 0
        return 1 + (node.children.maxOfOrNull { getMaxChildDepth(it) } ?: 0)
    }
}
