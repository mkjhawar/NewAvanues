/*
 * VoiceCursorClient.kt - Client for calling VoiceCursor service
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * This client provides suspend functions for AVA to interact with VoiceCursor
 * services, enabling cursor control for voice, gaze, and touch-based navigation.
 */
package com.augmentalis.universalrpc.android.ava

import com.augmentalis.universalrpc.cursor.CursorActionRequest
import com.augmentalis.universalrpc.cursor.CursorConfig
import com.augmentalis.universalrpc.cursor.CursorPosition
import com.augmentalis.universalrpc.cursor.CursorResponse
import com.augmentalis.universalrpc.cursor.GetPositionRequest
import com.augmentalis.universalrpc.cursor.MoveCursorRequest
import com.augmentalis.universalrpc.cursor.StreamPositionRequest
import com.augmentalis.universalrpc.cursor.VoiceCursorServiceGrpcKt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.UUID

/**
 * Cursor mode for VoiceCursor.
 */
enum class CursorMode(val value: String) {
    GAZE("gaze"),
    TOUCH("touch"),
    VOICE("voice")
}

/**
 * Cursor action type.
 */
enum class CursorAction(val value: String) {
    CLICK("click"),
    DOUBLE_CLICK("double_click"),
    LONG_PRESS("long_press"),
    SCROLL_UP("scroll_up"),
    SCROLL_DOWN("scroll_down"),
    SCROLL_LEFT("scroll_left"),
    SCROLL_RIGHT("scroll_right")
}

/**
 * Client for interacting with VoiceCursor service.
 *
 * Provides high-level suspend functions for cursor control operations including:
 * - Position queries
 * - Cursor movement
 * - Click actions
 * - Configuration updates
 * - Position streaming
 *
 * @param grpcClient The base gRPC client for connection management
 * @param dispatcher The coroutine dispatcher for async operations
 */
class VoiceCursorClient(
    private val grpcClient: AvaGrpcClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : Closeable {

    private var stub: VoiceCursorServiceGrpcKt.VoiceCursorServiceCoroutineStub? = null

    /**
     * Get or create the gRPC stub for VoiceCursor service.
     */
    private suspend fun getStub(): VoiceCursorServiceGrpcKt.VoiceCursorServiceCoroutineStub {
        stub?.let { return it }

        val channel = grpcClient.getChannel()
        return VoiceCursorServiceGrpcKt.VoiceCursorServiceCoroutineStub(channel).also {
            stub = it
        }
    }

    // =========================================================================
    // Position Operations
    // =========================================================================

    /**
     * Get the current cursor position.
     *
     * @return CursorPosition with x, y coordinates and timestamp
     */
    suspend fun getPosition(): CursorPosition = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = GetPositionRequest(request_id = generateRequestId())
            getStub().GetPosition(request)
        }
    }

    /**
     * Move cursor to a specific position.
     *
     * @param x Target X coordinate
     * @param y Target Y coordinate
     * @param animate Whether to animate the movement
     * @param durationMs Animation duration in milliseconds
     * @return CursorResponse with success status and final position
     */
    suspend fun moveTo(
        x: Int,
        y: Int,
        animate: Boolean = true,
        durationMs: Int = 200
    ): CursorResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = MoveCursorRequest(
                request_id = generateRequestId(),
                target_x = x,
                target_y = y,
                animate = animate,
                duration_ms = durationMs
            )
            getStub().MoveTo(request)
        }
    }

    /**
     * Move cursor by a relative offset.
     *
     * @param deltaX X offset (positive = right, negative = left)
     * @param deltaY Y offset (positive = down, negative = up)
     * @param animate Whether to animate the movement
     * @return CursorResponse with success status and final position
     */
    suspend fun moveBy(
        deltaX: Int,
        deltaY: Int,
        animate: Boolean = true
    ): CursorResponse = withContext(dispatcher) {
        val currentPos = getPosition()
        moveTo(
            x = currentPos.x + deltaX,
            y = currentPos.y + deltaY,
            animate = animate
        )
    }

    // =========================================================================
    // Action Operations
    // =========================================================================

    /**
     * Execute a cursor action at current position.
     *
     * @param action The action to execute
     * @param params Optional parameters for the action
     * @return CursorResponse with success status
     */
    suspend fun executeAction(
        action: CursorAction,
        params: Map<String, String> = emptyMap()
    ): CursorResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = CursorActionRequest(
                request_id = generateRequestId(),
                action = action.value,
                params = params
            )
            getStub().ExecuteAction(request)
        }
    }

    /**
     * Click at the current cursor position.
     */
    suspend fun click(): CursorResponse = executeAction(CursorAction.CLICK)

    /**
     * Double-click at the current cursor position.
     */
    suspend fun doubleClick(): CursorResponse = executeAction(CursorAction.DOUBLE_CLICK)

    /**
     * Long press at the current cursor position.
     */
    suspend fun longPress(): CursorResponse = executeAction(CursorAction.LONG_PRESS)

    /**
     * Click at a specific position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return CursorResponse with success status
     */
    suspend fun clickAt(x: Int, y: Int): CursorResponse = withContext(dispatcher) {
        moveTo(x, y, animate = false)
        click()
    }

    /**
     * Long press at a specific position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param durationMs Duration of long press in milliseconds
     * @return CursorResponse with success status
     */
    suspend fun longPressAt(
        x: Int,
        y: Int,
        durationMs: Int = 500
    ): CursorResponse = withContext(dispatcher) {
        moveTo(x, y, animate = false)
        executeAction(CursorAction.LONG_PRESS, mapOf("duration_ms" to durationMs.toString()))
    }

    // =========================================================================
    // Scroll Operations
    // =========================================================================

    /**
     * Scroll up at current position.
     *
     * @param distance Scroll distance in pixels
     * @return CursorResponse with success status
     */
    suspend fun scrollUp(distance: Int = 300): CursorResponse =
        executeAction(CursorAction.SCROLL_UP, mapOf("distance" to distance.toString()))

    /**
     * Scroll down at current position.
     *
     * @param distance Scroll distance in pixels
     * @return CursorResponse with success status
     */
    suspend fun scrollDown(distance: Int = 300): CursorResponse =
        executeAction(CursorAction.SCROLL_DOWN, mapOf("distance" to distance.toString()))

    /**
     * Scroll left at current position.
     *
     * @param distance Scroll distance in pixels
     * @return CursorResponse with success status
     */
    suspend fun scrollLeft(distance: Int = 300): CursorResponse =
        executeAction(CursorAction.SCROLL_LEFT, mapOf("distance" to distance.toString()))

    /**
     * Scroll right at current position.
     *
     * @param distance Scroll distance in pixels
     * @return CursorResponse with success status
     */
    suspend fun scrollRight(distance: Int = 300): CursorResponse =
        executeAction(CursorAction.SCROLL_RIGHT, mapOf("distance" to distance.toString()))

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Update cursor configuration.
     *
     * @param size Cursor size in dp
     * @param color ARGB color value
     * @param speed Movement speed multiplier
     * @param visible Visibility state
     * @param mode Cursor mode: "gaze", "touch", "voice"
     * @return CursorResponse with success status
     */
    suspend fun configure(
        size: Int? = null,
        color: Int? = null,
        speed: Float? = null,
        visible: Boolean? = null,
        mode: CursorMode? = null
    ): CursorResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val config = CursorConfig(
                size = size ?: 24,
                color = color ?: 0xFF00FF00.toInt(),
                speed = speed ?: 1.0f,
                visible = visible ?: true,
                mode = mode?.value ?: CursorMode.TOUCH.value
            )
            getStub().Configure(config)
        }
    }

    /**
     * Show the cursor.
     */
    suspend fun show(): CursorResponse = configure(visible = true)

    /**
     * Hide the cursor.
     */
    suspend fun hide(): CursorResponse = configure(visible = false)

    /**
     * Set cursor mode.
     *
     * @param mode The cursor mode
     */
    suspend fun setMode(mode: CursorMode): CursorResponse = configure(mode = mode)

    /**
     * Set cursor color.
     *
     * @param argb ARGB color value
     */
    suspend fun setColor(argb: Int): CursorResponse = configure(color = argb)

    /**
     * Set cursor size.
     *
     * @param sizeDp Size in dp
     */
    suspend fun setSize(sizeDp: Int): CursorResponse = configure(size = sizeDp)

    // =========================================================================
    // Position Streaming
    // =========================================================================

    /**
     * Stream cursor position updates.
     *
     * @param intervalMs Update interval in milliseconds
     * @return Flow of CursorPosition updates
     */
    suspend fun streamPosition(intervalMs: Int = 50): Flow<CursorPosition> {
        return grpcClient.withStreaming { _ ->
            val request = StreamPositionRequest(
                request_id = generateRequestId(),
                interval_ms = intervalMs
            )
            getStub().StreamPosition(request)
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private fun generateRequestId(): String = UUID.randomUUID().toString()

    override fun close() {
        grpcClient.close()
    }

    companion object {
        /**
         * Create a client for local UDS connection.
         */
        fun forLocalConnection(
            socketPath: String = "/data/local/tmp/voiceos.sock"
        ): VoiceCursorClient {
            return VoiceCursorClient(AvaGrpcClient.forLocalConnection(socketPath))
        }

        /**
         * Create a client for remote TCP connection.
         */
        fun forRemoteConnection(
            host: String,
            port: Int = 50051,
            useTls: Boolean = false
        ): VoiceCursorClient {
            return VoiceCursorClient(AvaGrpcClient.forRemoteConnection(host, port, useTls))
        }
    }
}
