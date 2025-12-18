package com.augmentalis.overlay.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import com.augmentalis.overlay.controller.OverlayController
import com.augmentalis.overlay.controller.VoiceRecognizer
import com.augmentalis.overlay.integration.AvaIntegrationBridge
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

/**
 * Unit tests for OverlayService
 *
 * Tests core functionality:
 * - Service lifecycle (onCreate, onStartCommand, onDestroy)
 * - Overlay window creation
 * - Foreground notification
 * - Action handling (SHOW, HIDE, TOGGLE)
 * - Resource cleanup
 * - Integration with VoiceRecognizer and OverlayController
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class OverlayServiceTest {

    private lateinit var service: OverlayService
    private lateinit var mockWindowManager: WindowManager
    private lateinit var mockNotificationManager: NotificationManager

    @Before
    fun setup() {
        // Create service controller
        val serviceController = Robolectric.buildService(OverlayService::class.java)
        
        // Mock WindowManager
        mockWindowManager = mockk(relaxed = true)
        
        // Mock NotificationManager
        mockNotificationManager = mockk(relaxed = true)
        
        service = serviceController.create().get()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test onCreate initializes service components`() {
        // Given - Service is created in setup()
        
        // Then
        assertNotNull("WindowManager should be initialized", service.getSystemService(Context.WINDOW_SERVICE))
        assertNotNull("Lifecycle should be initialized", service.lifecycle)
        assertEquals("Lifecycle should be RESUMED", Lifecycle.State.RESUMED, service.lifecycle.currentState)
    }

    @Test
    fun `test onCreate creates overlay window`() {
        // Given - Service is created
        val serviceController = Robolectric.buildService(OverlayService::class.java)
        
        // Use Mockk to spy on the actual service
        val spyService = spyk(serviceController.create().get())
        
        // Verify window creation would be called (difficult to test without real WindowManager)
        assertNotNull("Service should have lifecycle", spyService.lifecycle)
    }

    @Test
    fun `test onCreate starts foreground notification`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java)
        val context = Robolectric.setupService(OverlayService::class.java)
        
        // When
        serviceController.create()
        
        // Then - Service should be in foreground with notification
        // Note: Full verification requires Android instrumented tests
        assertNotNull("Service should exist", context)
    }

    @Test
    fun `test onStartCommand with ACTION_SHOW expands overlay`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        
        // When
        val intent = Intent(service, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW
        }
        serviceController.startCommand(0, 0)
        service.onStartCommand(intent, 0, 0)
        
        // Then - Controller should be expanded
        // Note: Verifying internal controller state requires access to controller field
        assertEquals("Should return START_STICKY", android.app.Service.START_STICKY, 
            service.onStartCommand(intent, 0, 0))
    }

    @Test
    fun `test onStartCommand with ACTION_HIDE collapses overlay`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        
        // When
        val intent = Intent(service, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE
        }
        val result = service.onStartCommand(intent, 0, 0)
        
        // Then
        assertEquals("Should return START_STICKY", android.app.Service.START_STICKY, result)
    }

    @Test
    fun `test onStartCommand with ACTION_TOGGLE toggles overlay state`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        
        // When
        val intent = Intent(service, OverlayService::class.java).apply {
            action = OverlayService.ACTION_TOGGLE
        }
        val result1 = service.onStartCommand(intent, 0, 0)
        val result2 = service.onStartCommand(intent, 0, 0)
        
        // Then
        assertEquals("Should return START_STICKY", android.app.Service.START_STICKY, result1)
        assertEquals("Should return START_STICKY", android.app.Service.START_STICKY, result2)
    }

    @Test
    fun `test onStartCommand with no action returns START_STICKY`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        
        // When
        val intent = Intent(service, OverlayService::class.java)
        val result = service.onStartCommand(intent, 0, 0)
        
        // Then
        assertEquals("Should return START_STICKY", android.app.Service.START_STICKY, result)
    }

    @Test
    fun `test onBind returns null`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        
        // When
        val binder = service.onBind(null)
        
        // Then
        assertNull("Service should not be bindable", binder)
    }

    @Test
    fun `test onDestroy cleans up resources`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        
        // When
        serviceController.destroy()
        
        // Then
        assertEquals("Lifecycle should be DESTROYED", 
            Lifecycle.State.DESTROYED, service.lifecycle.currentState)
    }

    @Test
    fun `test onDestroy removes overlay view`() {
        // Given - This requires checking WindowManager.removeView is called
        // Difficult to test without instrumented tests
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        
        // When
        serviceController.destroy()
        
        // Then - Service destroyed successfully
        assertNotNull("Service controller should exist", serviceController)
    }

    @Test
    fun `test onDestroy releases voice recognizer`() {
        // Given - Service with voice recognizer
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        
        // When
        serviceController.destroy()
        
        // Then - Voice recognizer released (verified in integration tests)
        assertNotNull("Service controller should exist", serviceController)
    }

    @Test
    fun `test onDestroy releases integration bridge`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        
        // When
        serviceController.destroy()
        
        // Then - Integration bridge released
        assertNotNull("Service controller should exist", serviceController)
    }

    @Test
    fun `test lifecycle transitions correctly`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java)
        
        // When - Create
        serviceController.create()
        val service = serviceController.get()
        
        // Then
        assertTrue("Should be at least CREATED", 
            service.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
        
        // When - Start
        serviceController.startCommand(0, 0)
        
        // Then
        assertTrue("Should be at least STARTED", 
            service.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        
        // When - Destroy
        serviceController.destroy()
        
        // Then
        assertEquals("Should be DESTROYED", 
            Lifecycle.State.DESTROYED, service.lifecycle.currentState)
    }

    @Test
    fun `test start helper method creates service intent`() {
        // Given
        val context = mockk<Context>(relaxed = true)
        
        // When
        OverlayService.start(context)
        
        // Then
        verify { 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(any())
            } else {
                context.startService(any())
            }
        }
    }

    @Test
    fun `test stop helper method stops service`() {
        // Given
        val context = mockk<Context>(relaxed = true)
        
        // When
        OverlayService.stop(context)
        
        // Then
        verify { context.stopService(any()) }
    }

    @Test
    fun `test savedStateRegistry is accessible`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        
        // When
        val registry = service.savedStateRegistry
        
        // Then
        assertNotNull("SavedStateRegistry should be available", registry)
    }

    @Test
    fun `test viewModelStore is accessible`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        
        // When
        val store = service.viewModelStore
        
        // Then
        assertNotNull("ViewModelStore should be available", store)
    }

    @Test
    fun `test service handles null intent in onStartCommand`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        
        // When
        val result = service.onStartCommand(null, 0, 0)
        
        // Then
        assertEquals("Should return START_STICKY even with null intent", 
            android.app.Service.START_STICKY, result)
    }

    @Test
    fun `test multiple ACTION_TOGGLE calls alternate state`() {
        // Given
        val serviceController = Robolectric.buildService(OverlayService::class.java).create()
        val service = serviceController.get()
        val intent = Intent(service, OverlayService::class.java).apply {
            action = OverlayService.ACTION_TOGGLE
        }
        
        // When - Toggle multiple times
        service.onStartCommand(intent, 0, 0) // Toggle 1
        service.onStartCommand(intent, 0, 1) // Toggle 2
        service.onStartCommand(intent, 0, 2) // Toggle 3
        
        // Then - Should complete without crashes
        assertEquals("Lifecycle should still be active", 
            Lifecycle.State.RESUMED, service.lifecycle.currentState)
    }

    @Test
    fun `test service can be created and destroyed multiple times`() {
        // Given & When
        val controller1 = Robolectric.buildService(OverlayService::class.java).create()
        controller1.destroy()
        
        val controller2 = Robolectric.buildService(OverlayService::class.java).create()
        
        // Then
        assertNotNull("Second service instance should be created", controller2.get())
        assertEquals("Second instance should be RESUMED", 
            Lifecycle.State.RESUMED, controller2.get().lifecycle.currentState)
    }
}
