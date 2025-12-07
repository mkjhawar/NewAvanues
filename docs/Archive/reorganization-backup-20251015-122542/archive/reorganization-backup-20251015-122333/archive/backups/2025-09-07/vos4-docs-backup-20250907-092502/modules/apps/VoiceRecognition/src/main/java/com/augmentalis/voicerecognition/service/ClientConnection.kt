/**
 * ClientConnection.kt - Data class for tracking client connections
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Purpose: Simple data structure for managing client connection metadata
 */
package com.augmentalis.voicerecognition.service

import com.augmentalis.voicerecognition.IRecognitionCallback

/**
 * Represents a client connection to the voice recognition service
 * 
 * @param callback The AIDL callback interface for this client
 * @param connectionTime When this client connected (system timestamp)
 * @param clientId Optional identifier for the client (for debugging)
 */
data class ClientConnection(
    val callback: IRecognitionCallback,
    val connectionTime: Long = System.currentTimeMillis(),
    val clientId: String? = null
) {
    
    /**
     * Check if this client connection is still alive
     */
    fun isAlive(): Boolean {
        return try {
            // Try to ping the callback - this will throw if the client is dead
            callback.asBinder().isBinderAlive
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get a readable description of this connection
     */
    fun getDescription(): String {
        return "Client(id=${clientId ?: "unknown"}, connected=${connectionTime})"
    }
}