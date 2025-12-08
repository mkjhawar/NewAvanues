// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/service/OverlayService.kt
// created: 2025-11-01 23:10:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - Core Infrastructure
// agent: Engineer | mode: ACT

package com.augmentalis.ava.features.overlay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.augmentalis.ava.features.overlay.controller.OverlayController
import com.augmentalis.ava.features.overlay.controller.VoiceRecognizer
import com.augmentalis.ava.features.overlay.integration.AvaIntegrationBridge
import com.augmentalis.ava.features.overlay.ui.OverlayComposables

/**
 * Foreground service that displays AVA overlay over all apps.
 *
 * Creates a TYPE_APPLICATION_OVERLAY window containing the Compose UI.
 * Runs as foreground service to maintain persistent overlay with microphone access.
 *
 * Lifecycle:
 * 1. Start service (requires SYSTEM_ALERT_WINDOW permission)
 * 2. Create foreground notification
 * 3. Add overlay window with Compose view
 * 4. Initialize voice recognizer
 * 5. Wire controller to voice callbacks
 *
 * @author Manoj Jhawar
 */
class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val controller = OverlayController()
    private var voiceRecognizer: VoiceRecognizer? = null
    private var integrationBridge: AvaIntegrationBridge? = null

    // Lifecycle components for Compose
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: throw IllegalStateException("WindowManager service not available")

        // Initialize integration bridge
        integrationBridge = AvaIntegrationBridge(this, controller)

        // Initialize voice recognizer
        voiceRecognizer = VoiceRecognizer(
            context = this,
            onPartialResult = { text ->
                controller.onTranscript(text)
            },
            onFinalResult = { text ->
                // Process through integration bridge (NLU → Chat)
                integrationBridge?.processTranscript(text)
            },
            onError = { error ->
                controller.onError(error)
            }
        )

        createOverlayWindow()
        startForegroundNotification()

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> controller.expand()
            ACTION_HIDE -> controller.collapse()
            ACTION_TOGGLE -> {
                if (controller.expanded.value) {
                    controller.collapse()
                } else {
                    controller.expand()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        integrationBridge?.release()
        integrationBridge = null

        voiceRecognizer?.release()
        voiceRecognizer = null

        overlayView?.let { view ->
            windowManager.removeView(view)
        }
        overlayView = null

        super.onDestroy()
    }

    /**
     * Create overlay window with Compose UI
     */
    private fun createOverlayWindow() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = ComposeView(this).apply {
            // Set lifecycle owners for Compose
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                OverlayComposables(controller = controller)
            }
        }

        windowManager.addView(overlayView, layoutParams)
    }

    /**
     * Start foreground notification to keep service alive
     */
    private fun startForegroundNotification() {
        val channelId = createNotificationChannel()

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AVA Assistant")
            .setContentText("Voice overlay active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Create notification channel (Android 8+)
     */
    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AVA Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps AVA voice overlay running"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        return CHANNEL_ID
    }

    companion object {
        private const val CHANNEL_ID = "ava_overlay_service"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_SHOW = "com.augmentalis.ava.overlay.SHOW"
        const val ACTION_HIDE = "com.augmentalis.ava.overlay.HIDE"
        const val ACTION_TOGGLE = "com.augmentalis.ava.overlay.TOGGLE"

        /**
         * Start overlay service
         */
        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop overlay service
         */
        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }
}
