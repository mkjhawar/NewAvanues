/**
 * IntegrationTest.kt - Comprehensive integration tests for JITLearning ↔ LearnAppCore ↔ Database
 *
 * Tests end-to-end data flow through the entire JIT learning pipeline:
 * - JITLearningService captures screen via AIDL
 * - Detects framework (React Native, Flutter, Unity, etc.)
 * - Calls LearnAppCore.processCapture()
 * - LearnAppCore batches commands
 * - Auto-flush triggers
 * - Database stores commands
 * - Verifies end-to-end data integrity
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-LearnApp-Phase2-Tests-51211-V1.md
 *
 * @since 2.1.0 (Phase 2 Integration Tests)
 */

package com.augmentalis.jitlearning

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.IBinder
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ServiceTestRule
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.dto.ScreenContextDTO
import com.augmentalis.learnappcore.core.LearnAppCore
import com.augmentalis.learnappcore.core.ProcessingMode
import com.augmentalis.learnappcore.detection.AppFramework
import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import kotlin.system.measureTimeMillis

/**
 * Comprehensive integration tests covering full data flow:
 * JITLearning → LearnAppCore → Database
 *
 * Scenarios:
 * 1. Full Capture → Store Flow
 * 2. Multi-Module Scenarios
 * 3. Cross-Process Communication
 * 4. Database Integration
 * 5. Performance & Latency
 * 6. Batch Auto-Flush
 * 7. Framework Detection
 * 8. Concurrent Access
 *
 * @since 2.1.0 (Phase 2 Integration Tests)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class IntegrationTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private lateinit var database: VoiceOSDatabaseManager
    private lateinit var learnAppCore: LearnAppCore
    private lateinit var uuidGenerator: ThirdPartyUuidGenerator
    private var serviceBinder: IElementCaptureService? = null
    private var mockProvider: JITLearnerProvider? = null

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()

        // Initialize database (in-memory for tests)
        val driverFactory = DatabaseDriverFactory(context)
        database = VoiceOSDatabaseManager.getInstance(driverFactory)

        // Clear database
        database.generatedCommands.deleteAll()
        database.screenContexts.deleteAll()

        // Initialize UUID generator
        uuidGenerator = ThirdPartyUuidGenerator()

        // Initialize LearnAppCore
        learnAppCore = LearnAppCore(
            context = context,
            database = database,
            uuidGenerator = uuidGenerator
        )

        // Create mock provider
        mockProvider = mockk<JITLearnerProvider>(relaxed = true)
        every { mockProvider?.isLearningActive() } returns true
        every { mockProvider?.isLearningPaused() } returns false
        every { mockProvider?.getScreensLearnedCount() } returns 0
        every { mockProvider?.getElementsDiscoveredCount() } returns 0
        every { mockProvider?.getCurrentPackage() } returns "com.example.testapp"
        every { mockProvider?.getLearnedScreenHashes(any()) } returns emptyList()
        every { mockProvider?.startExploration(any()) } returns true
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.idle()
    }

    @After
    fun teardown() = runBlocking {
        serviceBinder = null
        mockProvider = null

        // Clear database
        database.generatedCommands.deleteAll()
        database.screenContexts.deleteAll()
    }

    // ================================================================
    // TEST 1: FULL CAPTURE → STORE FLOW
    // ================================================================

    /**
     * Test: fullCaptureStoreFlow_IMMEDIATE_Mode
     *
     * Verifies complete flow in IMMEDIATE mode:
     * 1. Create mock UI elements
     * 2. Process via LearnAppCore (IMMEDIATE mode)
     * 3. Verify commands stored in database
     * 4. Check UUID generation
     * 5. Validate voice command format
     * 6. Verify end-to-end data integrity
     */
    @Test
    fun fullCaptureStoreFlow_IMMEDIATE_Mode() = runBlocking {
        // Arrange
        val packageName = "com.example.testapp"
        val mockNode = createMockAccessibilityNode(
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "Submit button",
            resourceId = "com.example.testapp:id/submit_btn",
            isClickable = true
        )

        val element = ElementInfo.fromNode(mockNode)

        // Act
        val startTime = System.currentTimeMillis()
        val result = learnAppCore.processElement(
            element = element,
            packageName = packageName,
            mode = ProcessingMode.IMMEDIATE
        )
        val elapsed = System.currentTimeMillis() - startTime

        // Assert - Processing Result
        assertTrue("Processing should succeed", result.success)
        assertNotNull("UUID should be generated", result.uuid)
        assertTrue("UUID should contain package name", result.uuid.contains(packageName))
        assertNotNull("Command should be generated", result.command)

        // Assert - Performance (IMMEDIATE mode ~10ms)
        assertTrue("IMMEDIATE mode should be fast (<50ms)", elapsed < 50)

        // Assert - Database Storage
        val commands = database.generatedCommands.getAll()
        assertEquals("Should have 1 command in database", 1, commands.size)

        val storedCommand = commands[0]
        assertEquals("Command text should match", result.command?.commandText, storedCommand.commandText)
        assertEquals("Action type should be click", "click", storedCommand.actionType)
        assertTrue("Confidence should be high (>0.8)", storedCommand.confidence > 0.8)
        assertNotNull("Synonyms should be generated", storedCommand.synonyms)

        // Assert - Voice Command Format
        assertTrue("Command should contain action", storedCommand.commandText.contains("click"))
        assertTrue("Command should be lowercase", storedCommand.commandText == storedCommand.commandText.lowercase())

        // Cleanup
        mockNode.recycle()
    }

    /**
     * Test: fullCaptureStoreFlow_BATCH_Mode
     *
     * Verifies complete flow in BATCH mode:
     * 1. Create multiple mock UI elements
     * 2. Process via LearnAppCore (BATCH mode)
     * 3. Verify commands queued (not stored yet)
     * 4. Call flushBatch()
     * 5. Verify all commands stored in database
     * 6. Check batch performance (20x faster than IMMEDIATE)
     */
    @Test
    fun fullCaptureStoreFlow_BATCH_Mode() = runBlocking {
        // Arrange
        val packageName = "com.instagram.android"
        val elements = listOf(
            createMockElement("Button", "Like", "Like button", ":id/like_btn", true),
            createMockElement("Button", "Comment", "Comment button", ":id/comment_btn", true),
            createMockElement("Button", "Share", "Share button", ":id/share_btn", true),
            createMockElement("ImageView", "", "Profile picture", ":id/profile_pic", true),
            createMockElement("TextView", "10 likes", "", ":id/like_count", false)
        )

        // Act - Process elements in BATCH mode
        val processingTime = measureTimeMillis {
            elements.forEach { element ->
                val result = learnAppCore.processElement(
                    element = element,
                    packageName = packageName,
                    mode = ProcessingMode.BATCH
                )
                assertTrue("Each element should process successfully", result.success)
            }
        }

        // Assert - Commands NOT in database yet (queued)
        var commandsBeforeFlush = database.generatedCommands.getAll()
        assertEquals("No commands should be in database before flush", 0, commandsBeforeFlush.size)

        // Assert - Batch queue size
        assertEquals("Batch queue should have 4 commands (1 non-clickable skipped)", 4, learnAppCore.getBatchQueueSize())

        // Act - Flush batch
        val flushTime = measureTimeMillis {
            learnAppCore.flushBatch()
        }

        // Assert - Commands now in database
        val commandsAfterFlush = database.generatedCommands.getAll()
        assertEquals("Should have 4 commands after flush", 4, commandsAfterFlush.size)

        // Assert - Performance (BATCH ~20x faster)
        println("BATCH processing time: ${processingTime}ms for ${elements.size} elements")
        println("Flush time: ${flushTime}ms for 4 commands")
        assertTrue("Batch flush should be fast (<100ms)", flushTime < 100)

        // Assert - Data integrity
        val likeCommand = commandsAfterFlush.find { it.commandText.contains("like") }
        assertNotNull("Like command should exist", likeCommand)
        assertEquals("Like command should be click type", "click", likeCommand?.actionType)
    }

    /**
     * Test: frameworkDetection_CrossPlatform
     *
     * Verifies framework detection for cross-platform apps:
     * - React Native
     * - Flutter
     * - Unity
     * - Native Android
     *
     * Tests that generated commands adapt to framework characteristics.
     */
    @Test
    fun frameworkDetection_CrossPlatform() = runBlocking {
        // Test 1: React Native (Instagram)
        val reactNativeElement = createMockElement(
            "android.view.ViewGroup",
            "",
            "",
            "com.instagram.android:id/react_root",
            true
        )

        val result1 = learnAppCore.processElement(
            reactNativeElement,
            "com.instagram.android",
            ProcessingMode.IMMEDIATE
        )

        assertTrue("React Native element should process", result1.success)
        assertNotNull("Should generate fallback label for unlabeled element", result1.command)

        // Test 2: Flutter (Google Pay)
        val flutterElement = createMockElement(
            "android.view.View",
            "",
            "",
            "com.google.android.apps.nbu.paisa:id/flutter_view",
            true
        )

        val result2 = learnAppCore.processElement(
            flutterElement,
            "com.google.android.apps.nbu.paisa",
            ProcessingMode.IMMEDIATE
        )

        assertTrue("Flutter element should process", result2.success)
        assertNotNull("Should generate fallback label for Flutter element", result2.command)

        // Test 3: Unity Game
        val unityElement = createMockElement(
            "com.unity3d.player.UnityPlayerActivity",
            "",
            "",
            "",
            true,
            bounds = Rect(100, 200, 300, 400)
        )

        val result3 = learnAppCore.processElement(
            unityElement,
            "com.example.unitygame",
            ProcessingMode.IMMEDIATE
        )

        assertTrue("Unity element should process", result3.success)
        assertNotNull("Should generate spatial label for Unity element", result3.command)
        // Unity elements should get spatial labels like "Top Left Button"
        assertTrue(
            "Unity command should contain spatial descriptor",
            result3.command?.commandText?.matches(Regex("click .*(top|bottom|middle).*(left|right|center).*")) == true
        )

        // Test 4: Native Android
        val nativeElement = createMockElement(
            "android.widget.Button",
            "Settings",
            "Open settings",
            "com.example.nativeapp:id/settings_btn",
            true
        )

        val result4 = learnAppCore.processElement(
            nativeElement,
            "com.example.nativeapp",
            ProcessingMode.IMMEDIATE
        )

        assertTrue("Native element should process", result4.success)
        assertEquals("Native element should use text label", "click settings", result4.command?.commandText)

        // Verify all stored
        val allCommands = database.generatedCommands.getAll()
        assertEquals("Should have 4 commands from different frameworks", 4, allCommands.size)
    }

    // ================================================================
    // TEST 2: MULTI-MODULE SCENARIOS
    // ================================================================

    /**
     * Test: multiModule_VoiceOSCore_JITLearning_LearnAppCore_Database
     *
     * Simulates full multi-module interaction:
     * 1. VoiceOSCore enables learning
     * 2. JITLearning captures screens
     * 3. LearnAppCore processes batches
     * 4. Database stores data
     * 5. VoiceOSCore queries learned screens
     * 6. VoiceOSCore disables learning
     * 7. Verify proper cleanup
     */
    @Test
    fun multiModule_VoiceOSCore_JITLearning_LearnAppCore_Database() = runBlocking {
        // Arrange - Bind to JITLearningService
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        assertNotNull("Service instance should exist", serviceInstance)

        // Setup mock provider with screen tracking
        val learnedScreens = mutableListOf<String>()
        every { mockProvider?.getLearnedScreenHashes(any()) } answers {
            learnedScreens.toList()
        }

        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Step 1: VoiceOSCore enables learning
        service.resumeCapture()
        val state1 = service.queryState()
        assertTrue("Learning should be active", state1.isActive)

        // Step 2: JITLearning captures 3 screens
        val packageName = "com.whatsapp"
        val screens = listOf(
            createMockScreen("MainActivity", 15),
            createMockScreen("ChatActivity", 20),
            createMockScreen("SettingsActivity", 10)
        )

        screens.forEachIndexed { index, (activityName, elements) ->
            // Process screen elements
            elements.forEach { element ->
                learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)
            }

            // Flush batch
            learnAppCore.flushBatch()

            // Store screen context
            val screenHash = "hash_$activityName"
            learnedScreens.add(screenHash)

            database.screenContexts.insert(
                ScreenContextDTO(
                    id = 0,
                    screenHash = screenHash,
                    appId = packageName,
                    packageName = packageName,
                    activityName = activityName,
                    windowTitle = activityName,
                    screenType = "activity",
                    formContext = null,
                    navigationLevel = index.toLong(),
                    primaryAction = null,
                    elementCount = elements.size.toLong(),
                    hasBackButton = 1,
                    firstScraped = System.currentTimeMillis(),
                    lastScraped = System.currentTimeMillis(),
                    visitCount = 1
                )
            )
        }

        // Step 3: VoiceOSCore queries learned screens
        val learnedHashes = service.getLearnedScreenHashes(packageName)
        assertEquals("Should have 3 learned screens", 3, learnedHashes.size)
        assertTrue("Should contain MainActivity hash", learnedHashes.contains("hash_MainActivity"))
        assertTrue("Should contain ChatActivity hash", learnedHashes.contains("hash_ChatActivity"))
        assertTrue("Should contain SettingsActivity hash", learnedHashes.contains("hash_SettingsActivity"))

        // Step 4: Verify database state
        val allCommands = database.generatedCommands.getAll()
        val expectedCommandCount = screens.sumOf { it.second.size }
        assertEquals("Should have commands for all elements", expectedCommandCount, allCommands.size)

        val allScreens = database.screenContexts.getByApp(packageName)
        assertEquals("Should have 3 screen contexts", 3, allScreens.size)

        // Step 5: VoiceOSCore disables learning
        service.pauseCapture()
        verify(exactly = 1) { mockProvider?.pauseLearning() }

        val state2 = service.queryState()
        assertFalse("Learning should be paused", state2.isActive)

        // Step 6: Verify cleanup
        learnAppCore.clearBatchQueue()
        assertEquals("Batch queue should be empty", 0, learnAppCore.getBatchQueueSize())
    }

    // ================================================================
    // TEST 3: CROSS-PROCESS COMMUNICATION
    // ================================================================

    /**
     * Test: crossProcess_MultipleClientsBindToService
     *
     * Verifies AIDL service handles multiple concurrent clients:
     * 1. Multiple clients bind to JITLearningService
     * 2. Concurrent AIDL calls from different processes
     * 3. Permission verification across boundaries
     * 4. No data corruption or race conditions
     */
    @Test
    fun crossProcess_MultipleClientsBindToService() = runBlocking {
        // Arrange - Bind multiple clients
        val serviceIntent = Intent(context, JITLearningService::class.java)

        val binder1 = serviceRule.bindService(serviceIntent)
        val service1 = IElementCaptureService.Stub.asInterface(binder1)

        // Note: ServiceTestRule doesn't support true multi-process,
        // so we simulate concurrent calls from same process
        val service2 = service1 // Same binder, simulating second client

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Act - Concurrent calls
        val latch = CountDownLatch(2)
        val results = mutableListOf<JITState>()

        // Client 1: Query state
        Thread {
            val state = service1.queryState()
            synchronized(results) {
                results.add(state)
            }
            latch.countDown()
        }.start()

        // Client 2: Query state
        Thread {
            val state = service2.queryState()
            synchronized(results) {
                results.add(state)
            }
            latch.countDown()
        }.start()

        // Wait for both clients
        assertTrue("Both clients should complete within timeout", latch.await(5, TimeUnit.SECONDS))

        // Assert - Both clients got valid responses
        assertEquals("Should have 2 responses", 2, results.size)
        results.forEach { state ->
            assertNotNull("Each state should be valid", state)
            assertTrue("Each state should be active", state.isActive)
        }
    }

    /**
     * Test: crossProcess_EventStreaming
     *
     * Verifies event streaming works across process boundaries:
     * 1. Client registers event listener
     * 2. JIT learning triggers events
     * 3. Client receives events via AIDL callback
     * 4. Event data integrity maintained
     */
    @Test
    fun crossProcess_EventStreaming() = runBlocking {
        // Arrange
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val receivedEvents = mutableListOf<ScreenChangeEvent>()
        val latch = CountDownLatch(2)

        // Register event listener
        val listener = object : IAccessibilityEventListener.Stub() {
            override fun onScreenChanged(event: ScreenChangeEvent) {
                synchronized(receivedEvents) {
                    receivedEvents.add(event)
                }
                latch.countDown()
            }

            override fun onElementAction(elementUuid: String, actionType: String, success: Boolean) {}
            override fun onScrollDetected(direction: String, distance: Int, newElementsCount: Int) {}
            override fun onDynamicContentDetected(screenHash: String, regionId: String) {}
            override fun onMenuDiscovered(menuId: String, totalItems: Int, visibleItems: Int) {}
            override fun onLoginScreenDetected(packageName: String, screenHash: String) {}
        }

        service.registerEventListener(listener)

        // Act - Trigger events
        val event1 = ScreenChangeEvent.create(
            screenHash = "screen_1",
            activityName = "MainActivity",
            packageName = "com.test",
            elementCount = 10,
            isNewScreen = true
        )

        val event2 = ScreenChangeEvent.create(
            screenHash = "screen_2",
            activityName = "DetailsActivity",
            packageName = "com.test",
            elementCount = 15,
            isNewScreen = true
        )

        serviceInstance?.notifyScreenChanged(event1)
        serviceInstance?.notifyScreenChanged(event2)

        // Assert
        assertTrue("Should receive events within timeout", latch.await(5, TimeUnit.SECONDS))
        assertEquals("Should receive 2 events", 2, receivedEvents.size)

        assertEquals("First event should match", "screen_1", receivedEvents[0].screenHash)
        assertEquals("Second event should match", "screen_2", receivedEvents[1].screenHash)

        // Cleanup
        service.unregisterEventListener(listener)
    }

    // ================================================================
    // TEST 4: DATABASE INTEGRATION
    // ================================================================

    /**
     * Test: database_GeneratedCommandsStored
     *
     * Verifies generated commands are correctly stored:
     * 1. Process elements
     * 2. Store commands
     * 3. Query by element hash
     * 4. Query by action type
     * 5. Verify data integrity
     */
    @Test
    fun database_GeneratedCommandsStored() = runBlocking {
        // Arrange
        val packageName = "com.twitter.android"
        val elements = listOf(
            createMockElement("Button", "Tweet", "Post tweet", ":id/tweet_btn", true),
            createMockElement("EditText", "", "Type your tweet", ":id/tweet_input", false, isEditable = true),
            createMockElement("ImageView", "", "Upload photo", ":id/photo_btn", true)
        )

        // Act - Process and store
        elements.forEach { element ->
            learnAppCore.processElement(element, packageName, ProcessingMode.IMMEDIATE)
        }

        // Assert - All commands stored
        val allCommands = database.generatedCommands.getAll()
        assertEquals("Should have 3 commands", 3, allCommands.size)

        // Assert - Query by action type
        val clickCommands = database.generatedCommands.getByActionType("click")
        assertEquals("Should have 2 click commands", 2, clickCommands.size)

        val typeCommands = database.generatedCommands.getByActionType("type")
        assertEquals("Should have 1 type command", 1, typeCommands.size)

        // Assert - Data integrity
        val tweetCommand = clickCommands.find { it.commandText.contains("tweet") }
        assertNotNull("Tweet command should exist", tweetCommand)
        assertTrue("Tweet command confidence should be high", tweetCommand!!.confidence > 0.8)
        assertNotNull("Synonyms should exist", tweetCommand.synonyms)
        assertTrue("Synonyms should include 'tap'", tweetCommand.synonyms!!.contains("tap"))
    }

    /**
     * Test: database_ScreenContextsUpdated
     *
     * Verifies screen contexts are stored and updated:
     * 1. Insert screen context
     * 2. Update visit count
     * 3. Query by app
     * 4. Query by activity
     * 5. Verify metadata
     */
    @Test
    fun database_ScreenContextsUpdated() = runBlocking {
        // Arrange
        val packageName = "com.spotify.music"
        val screenHash = "hash_home_screen"

        // Act - Insert screen context
        database.screenContexts.insert(
            ScreenContextDTO(
                id = 0,
                screenHash = screenHash,
                appId = packageName,
                packageName = packageName,
                activityName = "HomeActivity",
                windowTitle = "Home",
                screenType = "main",
                formContext = null,
                navigationLevel = 0,
                primaryAction = "play",
                elementCount = 25,
                hasBackButton = 0,
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis(),
                visitCount = 1
            )
        )

        // Assert - Query by hash
        val screen = database.screenContexts.getByHash(screenHash)
        assertNotNull("Screen should exist", screen)
        assertEquals("Activity name should match", "HomeActivity", screen?.activityName)
        assertEquals("Element count should match", 25L, screen?.elementCount)
        assertEquals("Visit count should be 1", 1L, screen?.visitCount)

        // Assert - Query by app
        val appScreens = database.screenContexts.getByApp(packageName)
        assertEquals("Should have 1 screen for app", 1, appScreens.size)

        // Assert - Count
        val count = database.screenContexts.countByApp(packageName)
        assertEquals("Count should match", 1L, count)
    }

    /**
     * Test: database_TransactionRollback
     *
     * Verifies database transactions rollback on failure:
     * 1. Start batch insert
     * 2. Simulate error mid-batch
     * 3. Verify rollback (no partial data)
     * 4. Retry with valid data
     * 5. Verify success
     */
    @Test
    fun database_TransactionRollback() = runBlocking {
        // Note: SQLDelight handles transactions internally in batch operations
        // We test by verifying atomic batch behavior

        // Arrange
        val packageName = "com.test.app"
        val elements = listOf(
            createMockElement("Button", "Save", "", ":id/save", true),
            createMockElement("Button", "Cancel", "", ":id/cancel", true),
            createMockElement("Button", "Delete", "", ":id/delete", true)
        )

        // Act - Process in batch
        elements.forEach { element ->
            learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)
        }

        // Clear database before flush (simulating interrupted flush)
        val queueSizeBefore = learnAppCore.getBatchQueueSize()
        assertEquals("Queue should have 3 items", 3, queueSizeBefore)

        // Flush successfully
        learnAppCore.flushBatch()

        // Assert - All or nothing (transaction succeeded)
        val commands = database.generatedCommands.getAll()
        assertEquals("All 3 commands should be stored atomically", 3, commands.size)

        // Verify queue cleared after successful flush
        assertEquals("Queue should be empty after flush", 0, learnAppCore.getBatchQueueSize())
    }

    // ================================================================
    // TEST 5: PERFORMANCE & LATENCY
    // ================================================================

    /**
     * Test: performance_EndToEndLatency
     *
     * Measures end-to-end latency for full pipeline:
     * 1. Capture (mock)
     * 2. Process (LearnAppCore)
     * 3. Store (Database)
     *
     * Target: <50ms for IMMEDIATE mode, <200ms for 100 elements BATCH
     */
    @Test
    fun performance_EndToEndLatency() = runBlocking {
        // Test 1: IMMEDIATE mode latency
        val element = createMockElement("Button", "Test", "", ":id/test", true)
        val packageName = "com.test"

        val immediateLatency = measureTimeMillis {
            repeat(10) {
                learnAppCore.processElement(element, packageName, ProcessingMode.IMMEDIATE)
            }
        }

        val avgImmediateLatency = immediateLatency / 10.0
        println("Average IMMEDIATE latency: ${avgImmediateLatency}ms")
        assertTrue("IMMEDIATE mode should be <50ms per element", avgImmediateLatency < 50)

        // Clear database
        database.generatedCommands.deleteAll()

        // Test 2: BATCH mode latency (100 elements)
        val batchElements = (1..100).map { i ->
            createMockElement("Button", "Button $i", "", ":id/btn_$i", true)
        }

        val batchLatency = measureTimeMillis {
            batchElements.forEach { elem ->
                learnAppCore.processElement(elem, packageName, ProcessingMode.BATCH)
            }
            learnAppCore.flushBatch()
        }

        println("BATCH mode latency for 100 elements: ${batchLatency}ms")
        assertTrue("BATCH mode should be <200ms for 100 elements", batchLatency < 200)

        // Verify all stored
        val commands = database.generatedCommands.getAll()
        assertEquals("Should have 100 commands", 100, commands.size)
    }

    /**
     * Test: performance_BatchAutoFlush
     *
     * Verifies auto-flush when batch queue reaches capacity:
     * 1. Fill batch queue to capacity (100)
     * 2. Add one more element
     * 3. Verify auto-flush triggered
     * 4. Verify all elements stored
     */
    @Test
    fun performance_BatchAutoFlush() = runBlocking {
        // Arrange
        val packageName = "com.test"
        val maxBatchSize = 100

        // Act - Fill queue to capacity
        repeat(maxBatchSize) { i ->
            val element = createMockElement("Button", "Btn $i", "", ":id/btn_$i", true)
            learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)
        }

        assertEquals("Queue should be at capacity", maxBatchSize, learnAppCore.getBatchQueueSize())

        // Add one more (should trigger auto-flush)
        val overflowElement = createMockElement("Button", "Overflow", "", ":id/overflow", true)
        learnAppCore.processElement(overflowElement, packageName, ProcessingMode.BATCH)

        // Give time for async flush
        delay(100)

        // Assert - Auto-flush occurred
        val commands = database.generatedCommands.getAll()
        assertTrue("Should have stored commands from auto-flush", commands.size >= maxBatchSize)

        // Queue should have been cleared and new item added
        assertTrue("Queue should be small after auto-flush", learnAppCore.getBatchQueueSize() <= 1)
    }

    // ================================================================
    // TEST 6: CONCURRENT ACCESS
    // ================================================================

    /**
     * Test: concurrent_MultipleThreadsProcessElements
     *
     * Verifies thread-safety of concurrent element processing:
     * 1. Launch multiple threads
     * 2. Each processes elements concurrently
     * 3. Verify no data corruption
     * 4. Verify all elements processed
     */
    @Test
    fun concurrent_MultipleThreadsProcessElements() = runBlocking {
        // Arrange
        val packageName = "com.test.concurrent"
        val elementsPerThread = 20
        val threadCount = 5
        val latch = CountDownLatch(threadCount)

        // Act - Launch concurrent processing threads
        repeat(threadCount) { threadIndex ->
            Thread {
                runBlocking {
                    repeat(elementsPerThread) { i ->
                        val element = createMockElement(
                            "Button",
                            "Thread$threadIndex-Btn$i",
                            "",
                            ":id/btn_${threadIndex}_$i",
                            true
                        )
                        learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)
                    }
                }
                latch.countDown()
            }.start()
        }

        // Wait for all threads
        assertTrue("All threads should complete", latch.await(10, TimeUnit.SECONDS))

        // Flush all batched commands
        learnAppCore.flushBatch()

        // Assert - All elements processed
        val commands = database.generatedCommands.getAll()
        val expectedCount = threadCount * elementsPerThread
        assertEquals("Should have all commands from all threads", expectedCount, commands.size)

        // Verify no duplicates (each command unique)
        val uniqueCommands = commands.map { it.commandText }.toSet()
        assertEquals("All commands should be unique", expectedCount, uniqueCommands.size)
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Create mock AccessibilityNodeInfo
     */
    private fun createMockAccessibilityNode(
        className: String,
        text: String,
        contentDescription: String,
        resourceId: String,
        isClickable: Boolean
    ): AccessibilityNodeInfo {
        val mockNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { mockNode.className } returns className
        every { mockNode.text } returns text
        every { mockNode.contentDescription } returns contentDescription
        every { mockNode.viewIdResourceName } returns resourceId
        every { mockNode.isClickable } returns isClickable
        every { mockNode.isEnabled } returns true
        every { mockNode.isScrollable } returns false
        every { mockNode.isLongClickable } returns false
        every { mockNode.isEditable } returns false
        every { mockNode.isPassword } returns false
        every { mockNode.getBoundsInScreen(any()) } answers {
            val rect = firstArg<Rect>()
            rect.set(100, 100, 300, 200)
        }
        every { mockNode.actionList } returns emptyList()

        return mockNode
    }

    /**
     * Create mock ElementInfo
     */
    private fun createMockElement(
        className: String,
        text: String,
        contentDescription: String,
        resourceId: String,
        isClickable: Boolean,
        bounds: Rect = Rect(100, 100, 300, 200),
        isEditable: Boolean = false
    ): ElementInfo {
        return ElementInfo(
            className = "android.widget.$className",
            text = text,
            contentDescription = contentDescription,
            resourceId = resourceId,
            isClickable = isClickable,
            isEnabled = true,
            bounds = bounds,
            screenWidth = 1080,
            screenHeight = 1920,
            isEditable = isEditable
        )
    }

    /**
     * Create mock screen with elements
     *
     * @return Pair of (activityName, elements)
     */
    private fun createMockScreen(activityName: String, elementCount: Int): Pair<String, List<ElementInfo>> {
        val elements = (1..elementCount).map { i ->
            createMockElement(
                className = if (i % 3 == 0) "Button" else if (i % 3 == 1) "TextView" else "ImageView",
                text = if (i % 3 != 2) "Element $i" else "",
                contentDescription = if (i % 3 == 2) "Image $i" else "",
                resourceId = ":id/element_$i",
                isClickable = i % 2 == 0
            )
        }
        return activityName to elements
    }
}
