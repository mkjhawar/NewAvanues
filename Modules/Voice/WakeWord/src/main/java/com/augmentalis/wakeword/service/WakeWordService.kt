// filename: Universal/AVA/Features/WakeWord/src/main/java/com/augmentalis/ava/features/wakeword/service/WakeWordService.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.wakeword.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.augmentalis.wakeword.IWakeWordDetector
import com.augmentalis.wakeword.R
import com.augmentalis.wakeword.WakeWordSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Wake Word Foreground Service
 *
 * Provides background wake word detection with:
 * - Foreground service for reliable operation
 * - Battery optimization (pause when screen off)
 * - Persistent notification
 * - Screen on/off detection
 *
 * Usage:
 * ```
 * val intent = Intent(context, WakeWordService::class.java)
 * intent.putExtra(EXTRA_SETTINGS, settings)
 * context.startForegroundService(intent)
 * ```
 *
 * @author Manoj Jhawar
 */
@AndroidEntryPoint
class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"

        // Service actions
        const val ACTION_START = "com.augmentalis.ava.wakeword.START"
        const val ACTION_STOP = "com.augmentalis.ava.wakeword.STOP"
        const val ACTION_PAUSE = "com.augmentalis.ava.wakeword.PAUSE"
        const val ACTION_RESUME = "com.augmentalis.ava.wakeword.RESUME"

        // Intent extras
        const val EXTRA_SETTINGS = "settings"

        // Notification
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "ava_wake_word"
        private const val NOTIFICATION_CHANNEL_NAME = "Wake Word Detection"

        // Battery threshold (pause below this level)
        private const val BATTERY_LOW_THRESHOLD = 15

        /**
         * Start wake word service
         */
        fun start(context: Context, settings: WakeWordSettings) {
            val intent = Intent(context, WakeWordService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SETTINGS, settings)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop wake word service
         */
        fun stop(context: Context) {
            val intent = Intent(context, WakeWordService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var wakeWordDetector: IWakeWordDetector

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentSettings: WakeWordSettings? = null
    private var isPaused = false

    // Battery monitoring
    private var batteryReceiver: android.content.BroadcastReceiver? = null

    // Screen state monitoring
    private var screenReceiver: android.content.BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("WakeWordService created")

        // Create notification channel (Android 8+)
        createNotificationChannel()

        // Register battery receiver
        registerBatteryMonitoring()

        // Register screen state receiver
        registerScreenStateMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("WakeWordService onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                val settings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_SETTINGS, WakeWordSettings::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_SETTINGS)
                }

                if (settings != null) {
                    startWakeWordDetection(settings)
                } else {
                    Timber.e("No settings provided to WakeWordService")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopWakeWordDetection()
                stopSelf()
            }
            ACTION_PAUSE -> {
                pauseWakeWordDetection("User requested")
            }
            ACTION_RESUME -> {
                resumeWakeWordDetection()
            }
        }

        // Restart service if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    override fun onDestroy() {
        Timber.d("WakeWordService destroyed")

        // Clean up
        serviceScope.launch {
            wakeWordDetector.stop()
            wakeWordDetector.cleanup()
        }

        // Unregister receivers
        batteryReceiver?.let { unregisterReceiver(it) }
        screenReceiver?.let { unregisterReceiver(it) }

        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Start wake word detection
     */
    private fun startWakeWordDetection(settings: WakeWordSettings) {
        currentSettings = settings

        // Start foreground service with notification
        val notification = createNotification(isListening = true)
        startForeground(NOTIFICATION_ID, notification)

        // Initialize and start detector
        serviceScope.launch {
            val initResult = wakeWordDetector.initialize(settings) { keyword ->
                // Wake word detected!
                Timber.i("Wake word detected in service: ${keyword.displayName}")

                // Show detection notification
                showWakeWordDetectedNotification(keyword.displayName)

                // Trigger voice input via broadcast
                // MainActivity listens for this and starts speech recognition
                val intent = Intent("com.augmentalis.ava.WAKE_WORD_DETECTED").apply {
                    putExtra("keyword", keyword.name)
                    putExtra("timestamp", System.currentTimeMillis())
                    // Explicit package for security (L+)
                    setPackage(packageName)
                }
                sendBroadcast(intent)

                // Play sound feedback (if enabled)
                if (settings.playSoundFeedback) {
                    playSoundFeedback()
                }

                // Vibrate (if enabled)
                if (settings.vibrateOnDetection) {
                    vibrateDevice()
                }
            }

            if (initResult is com.augmentalis.ava.core.common.Result.Success) {
                val startResult = wakeWordDetector.start()
                if (startResult is com.augmentalis.ava.core.common.Result.Success) {
                    Timber.i("Wake word detection started successfully")
                    updateNotification(isListening = true)
                } else {
                    Timber.e("Failed to start wake word detection")
                    stopSelf()
                }
            } else {
                Timber.e("Failed to initialize wake word detector")
                stopSelf()
            }
        }
    }

    /**
     * Stop wake word detection
     */
    private fun stopWakeWordDetection() {
        serviceScope.launch {
            wakeWordDetector.stop()
            Timber.i("Wake word detection stopped")
        }
    }

    /**
     * Pause wake word detection
     */
    private fun pauseWakeWordDetection(reason: String) {
        if (!isPaused) {
            serviceScope.launch {
                wakeWordDetector.pause(reason)
                isPaused = true
                updateNotification(isListening = false)
                Timber.i("Wake word detection paused: $reason")
            }
        }
    }

    /**
     * Resume wake word detection
     */
    private fun resumeWakeWordDetection() {
        if (isPaused) {
            serviceScope.launch {
                wakeWordDetector.resume()
                isPaused = false
                updateNotification(isListening = true)
                Timber.i("Wake word detection resumed")
            }
        }
    }

    /**
     * Create notification channel (Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when AVA is listening for wake word"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(isListening: Boolean): Notification {
        // Create intent for launching app when notification is tapped
        // Uses package manager to get the launcher activity dynamically
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent().apply { setClassName(packageName, "${packageName}.MainActivity") }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isListening) "AVA Listening" else "AVA Paused"
        val text = if (isListening) "Say 'Hey AVA' to activate" else "Wake word detection paused"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_ava_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Update notification
     */
    private fun updateNotification(isListening: Boolean) {
        val notification = createNotification(isListening)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show notification when wake word is detected
     */
    private fun showWakeWordDetectedNotification(keyword: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Wake Word Detected")
            .setContentText("Heard: $keyword")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Register battery monitoring
     */
    private fun registerBatteryMonitoring() {
        batteryReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = level * 100 / scale.toFloat()

                Timber.d("Battery level: $batteryPct%")

                // Pause if battery is low and optimization is enabled
                if (currentSettings?.batteryOptimization == true && batteryPct < BATTERY_LOW_THRESHOLD) {
                    pauseWakeWordDetection("Low battery ($batteryPct%)")
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    /**
     * Register screen state monitoring
     */
    private fun registerScreenStateMonitoring() {
        screenReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        if (currentSettings?.batteryOptimization == true) {
                            pauseWakeWordDetection("Screen off")
                        }
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        if (isPaused) {
                            resumeWakeWordDetection()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    /**
     * Play sound feedback when wake word is detected.
     * Uses ToneGenerator for a short, non-intrusive beep.
     */
    private fun playSoundFeedback() {
        try {
            val toneGenerator = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION,
                50 // 50% volume
            )
            // Play a short beep (100ms)
            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100)
            // Release after a delay to ensure tone completes
            serviceScope.launch {
                delay(150)
                toneGenerator.release()
            }
            Timber.d("Playing sound feedback")
        } catch (e: Exception) {
            Timber.w(e, "Failed to play sound feedback")
        }
    }

    /**
     * Vibrate device when wake word is detected.
     * Uses short haptic feedback (50ms) for tactile confirmation.
     */
    private fun vibrateDevice() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
            Timber.d("Vibrating device for haptic feedback")
        } catch (e: Exception) {
            Timber.w(e, "Failed to vibrate device")
        }
    }

}
