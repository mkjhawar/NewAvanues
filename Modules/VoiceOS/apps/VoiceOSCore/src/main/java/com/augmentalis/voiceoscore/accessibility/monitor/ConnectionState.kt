/**
 * ConnectionState.kt - Service connection states
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-10
 */
package com.augmentalis.voiceoscore.accessibility.monitor

/**
 * Connection states for service monitoring
 */
enum class ConnectionState {
    /**
     * Service is connected and healthy
     */
    CONNECTED,

    /**
     * Service is disconnected
     */
    DISCONNECTED,

    /**
     * Service is attempting recovery from disconnection
     */
    RECOVERING,

    /**
     * Service is operating in degraded mode (fallback functionality only)
     */
    DEGRADED
}
