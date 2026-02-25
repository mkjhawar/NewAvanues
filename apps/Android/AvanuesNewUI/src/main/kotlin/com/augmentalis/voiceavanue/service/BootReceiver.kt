/**
 * BootReceiver.kt - Boot completed receiver
 *
 * Starts AVA cursor overlay service on device boot if the user has
 * enabled "Start on Boot" in Settings (persisted via DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.augmentalis.voiceavanue.data.avanuesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.augmentalis.voicecursor.overlay.CursorOverlayService
import kotlinx.coroutines.launch

private const val TAG = "BootReceiver"
private val KEY_AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        Log.i(TAG, "Boot completed, checking auto-start preference")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val autoStart = context.avanuesDataStore.data
                    .map { prefs -> prefs[KEY_AUTO_START_ON_BOOT] ?: false }
                    .first()

                if (autoStart) {
                    Log.i(TAG, "Auto-start enabled, starting CursorOverlayService")
                    val serviceIntent = Intent(context, CursorOverlayService::class.java)
                    context.startForegroundService(serviceIntent)
                } else {
                    Log.i(TAG, "Auto-start disabled, skipping service start")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check auto-start preference", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
