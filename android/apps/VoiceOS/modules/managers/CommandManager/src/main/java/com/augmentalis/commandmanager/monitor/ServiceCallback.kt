/**
 * ServiceCallback.kt - Service lifecycle callbacks
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-10
 *
 * Callback interface for service lifecycle events
 * Duplicated in CommandManager to avoid circular dependency with VoiceAccessibility
 */
package com.augmentalis.commandmanager.monitor

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
