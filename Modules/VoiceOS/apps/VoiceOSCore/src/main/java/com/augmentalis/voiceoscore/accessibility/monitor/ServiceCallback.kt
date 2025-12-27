/**
 * ServiceCallback.kt - Service lifecycle callbacks
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-10
 */
package com.augmentalis.voiceoscore.accessibility.monitor

/**
 * Callback interface for CommandManager service events
 */
interface ServiceCallback {
    /**
     * Called when service is successfully bound
     */
    fun onServiceBound()

    /**
     * Called when service is disconnected
     */
    fun onServiceDisconnected()
}
