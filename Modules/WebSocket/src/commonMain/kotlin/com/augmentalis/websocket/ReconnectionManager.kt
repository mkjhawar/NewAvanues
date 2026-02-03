package com.augmentalis.websocket

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.aakira.napier.Napier

/**
 * Reconnection state
 */
sealed class ReconnectionState {
    object Idle : ReconnectionState()
    data class Attempting(val attempt: Int, val maxAttempts: Int, val delayMs: Long) : ReconnectionState()
    object Success : ReconnectionState()
    data class Failed(val reason: String) : ReconnectionState()
    object Cancelled : ReconnectionState()
}

/**
 * Manages automatic reconnection with exponential backoff
 */
class ReconnectionManager(
    private val config: WebSocketClientConfig,
    private val scope: CoroutineScope
) {
    private val TAG = "ReconnectionManager"

    private var reconnectJob: Job? = null
    private var currentAttempt = 0
    private var currentDelay = config.initialReconnectDelayMs

    private val _state = MutableStateFlow<ReconnectionState>(ReconnectionState.Idle)
    val state: StateFlow<ReconnectionState> = _state.asStateFlow()

    /**
     * Start reconnection attempts
     *
     * @param connect Suspend function that attempts connection, returns true on success
     */
    fun startReconnection(connect: suspend () -> Boolean) {
        if (!config.autoReconnect) {
            Napier.d("Auto-reconnect disabled", tag = TAG)
            return
        }

        cancel() // Cancel any existing attempt

        currentAttempt = 0
        currentDelay = config.initialReconnectDelayMs

        reconnectJob = scope.launch {
            while (currentAttempt < config.maxReconnectAttempts) {
                currentAttempt++

                _state.value = ReconnectionState.Attempting(
                    attempt = currentAttempt,
                    maxAttempts = config.maxReconnectAttempts,
                    delayMs = currentDelay
                )

                Napier.i(
                    "Reconnection attempt $currentAttempt/${config.maxReconnectAttempts} (delay: ${currentDelay}ms)",
                    tag = TAG
                )

                delay(currentDelay)

                try {
                    val success = connect()
                    if (success) {
                        Napier.i("Reconnection successful on attempt $currentAttempt", tag = TAG)
                        _state.value = ReconnectionState.Success
                        reset()
                        return@launch
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.w("Reconnection attempt $currentAttempt failed: ${e.message}", tag = TAG)
                }

                // Exponential backoff
                currentDelay = (currentDelay * config.reconnectDelayMultiplier).toLong()
                    .coerceAtMost(config.maxReconnectDelayMs)
            }

            Napier.e("Reconnection failed after ${config.maxReconnectAttempts} attempts", tag = TAG)
            _state.value = ReconnectionState.Failed("Max attempts (${config.maxReconnectAttempts}) exceeded")
        }
    }

    /**
     * Cancel ongoing reconnection attempts
     */
    fun cancel() {
        reconnectJob?.cancel()
        reconnectJob = null
        if (_state.value is ReconnectionState.Attempting) {
            _state.value = ReconnectionState.Cancelled
        }
    }

    /**
     * Reset state for new connection cycle
     */
    fun reset() {
        cancel()
        currentAttempt = 0
        currentDelay = config.initialReconnectDelayMs
        _state.value = ReconnectionState.Idle
    }

    /**
     * Check if currently reconnecting
     */
    fun isReconnecting(): Boolean = _state.value is ReconnectionState.Attempting
}

/**
 * Keep-alive manager for WebSocket ping/pong
 */
class KeepAliveManager(
    private val config: WebSocketClientConfig,
    private val scope: CoroutineScope
) {
    private val TAG = "KeepAliveManager"
    private var pingJob: Job? = null
    private var lastPongReceived: Long = 0

    /**
     * Start keep-alive pings
     *
     * @param sendPing Function to send ping message
     * @param onTimeout Called when pong not received in time
     */
    fun start(sendPing: suspend () -> Unit, onTimeout: () -> Unit) {
        if (config.pingIntervalMs <= 0) return

        stop()
        lastPongReceived = System.currentTimeMillis()

        pingJob = scope.launch {
            while (isActive) {
                delay(config.pingIntervalMs)

                // Check if last pong was too long ago
                val timeSinceLastPong = System.currentTimeMillis() - lastPongReceived
                if (timeSinceLastPong > config.pingIntervalMs * 2) {
                    Napier.w("Pong timeout (${timeSinceLastPong}ms)", tag = TAG)
                    onTimeout()
                    return@launch
                }

                try {
                    sendPing()
                    if (config.debugLogging) {
                        Napier.d("Ping sent", tag = TAG)
                    }
                } catch (e: Exception) {
                    Napier.w("Failed to send ping: ${e.message}", tag = TAG)
                }
            }
        }
    }

    /**
     * Record pong received
     */
    fun onPongReceived() {
        lastPongReceived = System.currentTimeMillis()
        if (config.debugLogging) {
            Napier.d("Pong received", tag = "KeepAliveManager")
        }
    }

    /**
     * Stop keep-alive
     */
    fun stop() {
        pingJob?.cancel()
        pingJob = null
    }
}

/**
 * Expect System.currentTimeMillis for KMP
 */
expect fun currentTimeMillis(): Long
