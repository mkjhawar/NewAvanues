/**
 * BootReceiver.kt - Boot completed receiver
 *
 * Optionally starts AVA services on device boot.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.i(TAG, "Boot completed, checking auto-start preference")

            // TODO: Check SharedPreferences for auto-start setting
            // If enabled, start the RPC server service

            // val prefs = context.getSharedPreferences("ava_prefs", Context.MODE_PRIVATE)
            // if (prefs.getBoolean("auto_start_on_boot", false)) {
            //     val serviceIntent = Intent(context, RpcServerService::class.java)
            //     context.startForegroundService(serviceIntent)
            // }
        }
    }
}
