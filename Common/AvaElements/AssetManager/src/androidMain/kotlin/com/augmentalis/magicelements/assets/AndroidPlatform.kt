package com.augmentalis.avaelements.assets

import android.annotation.SuppressLint
import android.content.Context

/**
 * Android platform implementation for AssetManager
 */

// Global context holder (should be initialized by app)
@SuppressLint("StaticFieldLeak")
private var appContext: Context? = null

/**
 * Initialize AssetManager with Android context
 *
 * Call this from your Application.onCreate()
 */
fun AssetManager.Companion.initialize(context: Context) {
    appContext = context.applicationContext
}

/**
 * Create default Android asset storage
 */
internal actual fun createDefaultStorage(): AssetStorage {
    val context = appContext
        ?: throw IllegalStateException(
            "AssetManager not initialized. " +
            "Call AssetManager.initialize(context) from Application.onCreate()"
        )
    return AndroidAssetStorage(context)
}

/**
 * Get current time in milliseconds
 */
internal actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}
