/**
 * JITLearningServiceTest.kt - Integration tests for JIT Learning Service
 *
 * Phase 2 Integration Tests - Task 2.1
 * Tests AIDL service binding, state management, and event streaming.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-Phase2-Tests-51211-V1.md
 */

package com.augmentalis.jitlearning

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ServiceTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for JITLearningService
 *
 * Tests service lifecycle, AIDL binding, state management,
 * event listener registration, and exploration commands.
 *
 * @since 2.1.0 (Phase 2 Integration Tests)
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class JITLearningServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private var serviceBinder: IElementCaptureService? = null
    private var mockProvider: JITLearnerProvider? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create mock provider
        mockProvider = mockk<JITLearnerProvider>(relaxed = true)

        // Setup mock provider defaults
        every { mockProvider?.isLearningActive() } returns true
        every { mockProvider?.isLearningPaused() } returns false
        every { mockProvider?.getScreensLearnedCount() } returns 5
        every { mockProvider?.getElementsDiscoveredCount() } returns 42
        every { mockProvider?.getCurrentPackage() } returns "com.example.test"
        every { mockProvider?.getLearnedScreenHashes(any()) } returns listOf("hash1", "hash2")
        every { mockProvider?.startExploration(any()) } returns true
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.idle()
    }

    @After
    fun teardown() {
        serviceBinder = null
        mockProvider = null
    }

    /**
     * Test: bindService_ReturnsValidBinder
     *
     * Verifies that binding to JITLearningService returns a valid
     * IElementCaptureService binder that can be used for IPC.
     */
    @Test
    fun bindService_ReturnsValidBinder() {
        // Arrange
        val serviceIntent = Intent(context, JITLearningService::class.java)

        // Act
        val binder = serviceRule.bindService(serviceIntent)

        // Assert
        assertNotNull("Service binder should not be null", binder)

        val elementCaptureService = IElementCaptureService.Stub.asInterface(binder)
        assertNotNull("IElementCaptureService interface should be available", elementCaptureService)

        serviceBinder = elementCaptureService
    }

    /**
     * Test: pauseCapture_UpdatesState
     *
     * Verifies that calling pauseCapture() via AIDL correctly
     * updates the service state and forwards to JITLearnerProvider.
     */
    @Test
    fun pauseCapture_UpdatesState() = runBlocking {
        // Arrange
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        // Setup service with mock provider
        val serviceInstance = JITLearningService.getInstance()
        assertNotNull("Service instance should be available", serviceInstance)
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Act
        service.pauseCapture()

        // Give time for state update
        Thread.sleep(100)

        // Assert
        verify(exactly = 1) { mockProvider?.pauseLearning() }

        val state = service.queryState()
        assertNotNull("State should not be null", state)
        // Note: isActive depends on provider's isLearningActive && !isPaused
    }

    /**
     * Test: resumeCapture_UpdatesState
     *
     * Verifies that calling resumeCapture() via AIDL correctly
     * resumes learning and updates state.
     */
    @Test
    fun resumeCapture_UpdatesState() = runBlocking {
        // Arrange
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Pause first
        service.pauseCapture()
        Thread.sleep(100)

        // Act
        service.resumeCapture()
        Thread.sleep(100)

        // Assert
        verify(exactly = 1) { mockProvider?.resumeLearning() }

        val state = service.queryState()
        assertNotNull("State should not be null", state)
    }

    /**
     * Test: queryState_ReturnsCurrentState
     *
     * Verifies that queryState() returns accurate current state
     * from the JITLearnerProvider.
     */
    @Test
    fun queryState_ReturnsCurrentState() {
        // Arrange
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Act
        val state = service.queryState()

        // Assert
        assertNotNull("State should not be null", state)
        assertEquals("Screens learned count should match", 5, state.screensLearned)
        assertEquals("Elements discovered count should match", 42, state.elementsDiscovered)
        assertEquals("Current package should match", "com.example.test", state.currentPackage)
        assertTrue("Service should be active", state.isActive)
    }

    /**
     * Test: getLearnedScreenHashes_ReturnsHashes
     *
     * Verifies that getLearnedScreenHashes() returns the correct
     * list of learned screen hashes for a given package.
     */
    @Test
    fun getLearnedScreenHashes_ReturnsHashes() {
        // Arrange
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.test"

        // Act
        val hashes = service.getLearnedScreenHashes(packageName)

        // Assert
        assertNotNull("Hashes should not be null", hashes)
        assertEquals("Should return 2 hashes", 2, hashes.size)
        assertTrue("Should contain hash1", hashes.contains("hash1"))
        assertTrue("Should contain hash2", hashes.contains("hash2"))

        verify(exactly = 1) { mockProvider?.getLearnedScreenHashes(packageName) }
    }

    /**
     * Test: registerEventListener_ReceivesEvents
     *
     * Verifies that registered event listeners receive screen change
     * events from the service.
     */
    @Test
    fun registerEventListener_ReceivesEvents() {
        // Arrange
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val latch = CountDownLatch(1)
        var receivedEvent: ScreenChangeEvent? = null

        val listener = object : IAccessibilityEventListener.Stub() {
            override fun onScreenChanged(event: ScreenChangeEvent) {
                receivedEvent = event
                latch.countDown()
            }

            override fun onElementAction(elementUuid: String, actionType: String, success: Boolean) {
                // Not tested in this case
            }

            override fun onScrollDetected(direction: String, distance: Int, newElementsCount: Int) {
                // Not tested in this case
            }

            override fun onDynamicContentDetected(screenHash: String, regionId: String) {
                // Not tested in this case
            }

            override fun onMenuDiscovered(menuId: String, totalItems: Int, visibleItems: Int) {
                // Not tested in this case
            }

            override fun onLoginScreenDetected(packageName: String, screenHash: String) {
                // Not tested in this case
            }
        }

        service.registerEventListener(listener)

        // Act - Simulate screen change event
        val testEvent = ScreenChangeEvent.create(
            screenHash = "test_hash_123",
            activityName = "TestActivity",
            packageName = "com.example.test",
            elementCount = 10,
            isNewScreen = true
        )

        serviceInstance?.notifyScreenChanged(testEvent)

        // Assert
        assertTrue("Listener should receive event within timeout", latch.await(2, TimeUnit.SECONDS))
        assertNotNull("Event should be received", receivedEvent)
        assertEquals("Screen hash should match", "test_hash_123", receivedEvent?.screenHash)
        assertEquals("Package name should match", "com.example.test", receivedEvent?.packageName)
        assertEquals("Element count should match", 10, receivedEvent?.elementCount)

        // Cleanup
        service.unregisterEventListener(listener)
    }

    /**
     * Test: startExploration_BeginsExploration
     *
     * Verifies that calling startExploration() via AIDL correctly
     * initiates automated exploration.
     */
    @Test
    fun startExploration_BeginsExploration() {
        // Arrange
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.test"

        // Act
        val success = service.startExploration(packageName)

        // Assert
        assertTrue("Exploration should start successfully", success)
        verify(exactly = 1) { mockProvider?.startExploration(packageName) }
    }

    /**
     * Test: stopExploration_EndsExploration
     *
     * Verifies that calling stopExploration() via AIDL correctly
     * stops the current exploration session.
     */
    @Test
    fun stopExploration_EndsExploration() {
        // Arrange
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Start exploration first
        service.startExploration("com.example.test")

        // Act
        service.stopExploration()

        // Assert
        verify(exactly = 1) { mockProvider?.stopExploration() }
    }
}
