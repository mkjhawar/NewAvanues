/**
 * VoiceRecognitionService.kt - Foreground service for voice recognition
 *
 * This is a thin wrapper that:
 * 1. Keeps the app process alive with a foreground notification
 * 2. Delegates speech recognition to VoiceOSCore (via AccessibilityService)
 *
 * Speech recognition is handled by VoiceOSCore - this service just manages lifecycle.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.augmentalis.voiceavanue.MainActivity
import com.augmentalis.voiceavanue.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "VoiceRecognitionService"
private const val CHANNEL_ID = "ava_voice_service"
private const val NOTIFICATION_ID = 1003

/**
 * Foreground service to keep voice recognition active.
 * Actual recognition is handled by VoiceOSCore through the AccessibilityService.
 */
class VoiceRecognitionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Voice Recognition Service starting")

        startForeground(NOTIFICATION_ID, createNotification("Listening for commands..."))

        // Start voice recognition via AccessibilityService
        serviceScope.launch {
            val accessibilityService = VoiceAvanueAccessibilityService.getInstance()
            if (accessibilityService != null) {
                accessibilityService.processVoiceCommand("", 0f) // Wake up VoiceOSCore
                Log.i(TAG, "Delegating to VoiceOSCore via AccessibilityService")
            } else {
                Log.w(TAG, "AccessibilityService not available")
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "Voice Recognition Service destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_voice_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_voice_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
