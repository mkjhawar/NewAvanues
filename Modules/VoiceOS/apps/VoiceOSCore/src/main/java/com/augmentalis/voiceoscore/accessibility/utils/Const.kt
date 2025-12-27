package com.augmentalis.voiceoscore.accessibility.utils

import android.content.Context
import android.content.Intent
import android.util.Log

object Const {

    const val ACTION_CONFIG_UPDATE = "com.augmentalis.voiceoscore.ACTION_CONFIG_UPDATE"
    const val KEY_LANGUAGE_NAME = "LANGUAGE_NAME"

    @JvmStatic
    fun Context.broadcastConfigUpdated(langCode: String) {
        Log.i("Const", "CHANGE_LANG broadcastConfigUpdated11")
        val intent = Intent(ACTION_CONFIG_UPDATE)
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        intent.putExtra(KEY_LANGUAGE_NAME, langCode)
        intent.setPackage(packageName)
        sendBroadcast(intent)
        Log.i("Const", "CHANGE_LANG broadcastConfigUpdated22")
    }


}