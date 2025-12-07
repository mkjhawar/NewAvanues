/**
 * VoiceOnSentry.kt - Lightweight foreground service for background microphone access
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.augmentalis.voiceoscore.R
import kotlinx.coroutines.*

/**
 * Lightweight foreground service for microphone access in background
 * Only active when:
 * - Android 12+ (S+)
 * - App is in background
 * - Voice session is active
 * 
 * Automatically stops when any condition is not met
 */
class VoiceOnSentry : LifecycleService() {
    
    companion object {
        private const val TAG = "VoiceOnSentry"
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "voiceos_mic_channel"
        
        // Actions
        const val ACTION_START_MIC = "com.augmentalis.voiceos.START_MIC"
        const val ACTION_STOP_MIC = "com.augmentalis.voiceos.STOP_MIC"
        const val ACTION_UPDATE_STATE = "com.augmentalis.voiceos.UPDATE_STATE"
        
        // States for notification
        enum class MicState {
            IDLE,
            LISTENING,
            PROCESSING,
            ERROR
        }
    }
    
    private lateinit var notificationManager: NotificationManager
    private var currentState = MicState.IDLE
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VoiceOnSentry onCreate")
        
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_MIC -> startMicService()
            ACTION_STOP_MIC -> stopMicService()
            ACTION_UPDATE_STATE -> updateState(intent)
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }
        
        // START_NOT_STICKY - don't restart if killed (saves battery)
        return START_NOT_STICKY
    }
    
    /**
     * Start the foreground service with minimal notification
     */
    private fun startMicService() {
        Log.i(TAG, "Starting VoiceOnSentry for background mic access")
        
        val notification = buildNotification(MicState.LISTENING)
        
        // Start foreground with proper type for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startForeground(
                    NOTIFICATION_ID, 
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
                stopSelf()
                return
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        currentState = MicState.LISTENING
    }
    
    /**
     * Stop the service and clean up
     */
    private fun stopMicService() {
        Log.i(TAG, "Stopping VoiceOnSentry")
        
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    /**
     * Update the notification state
     */
    private fun updateState(intent: Intent) {
        val stateOrdinal = intent.getIntExtra("state", MicState.IDLE.ordinal)
        val newState = MicState.values().getOrNull(stateOrdinal) ?: MicState.IDLE
        
        if (currentState != newState) {
            currentState = newState
            val notification = buildNotification(newState)
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            Log.d(TAG, "State updated to: $newState")
        }
    }
    
    /**
     * Build a minimal notification based on state
     */
    private fun buildNotification(state: MicState): Notification {
        val title = when (state) {
            MicState.IDLE -> "Voice ready"
            MicState.LISTENING -> "Listening..."
            MicState.PROCESSING -> "Processing..."
            MicState.ERROR -> "Voice error"
        }
        
        val icon = when (state) {
            MicState.LISTENING -> R.drawable.ic_mic_on
            MicState.PROCESSING -> R.drawable.ic_processing
            MicState.ERROR -> R.drawable.ic_mic_off
            else -> R.drawable.ic_mic_off
        }
        
        // Create intent to return to app
        val appIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("VoiceOS voice service active")
            .setSmallIcon(icon)
            .setOngoing(true) // Can't be swiped away
            .setSilent(true) // No sound
            .setPriority(NotificationCompat.PRIORITY_LOW) // Minimal intrusion
            .setContentIntent(pendingIntent)
            .setShowWhen(false) // Hide timestamp
            .build()
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Microphone Service",
                NotificationManager.IMPORTANCE_LOW // Quiet channel
            ).apply {
                description = "Background microphone access for voice commands"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null // No binding needed
    }
    
    override fun onDestroy() {
        Log.d(TAG, "VoiceOnSentry onDestroy")
        serviceScope.cancel()
        super.onDestroy()
    }
}