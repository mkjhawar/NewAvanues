package com.augmentalis.httpavanue.sse

import com.augmentalis.httpavanue.http.HttpResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.withLock

/**
 * Server-Sent Events (SSE) data model
 */
data class SseEvent(
    val data: String,
    val event: String? = null,
    val id: String? = null,
    val retry: Long? = null,
) {
    /** Serialize to SSE wire format */
    fun toWireFormat(): String = buildString {
        id?.let { append("id: $it\n") }
        event?.let { append("event: $it\n") }
        retry?.let { append("retry: $it\n") }
        data.split('\n').forEach { line -> append("data: $line\n") }
        append('\n') // Empty line terminates event
    }
}

/**
 * SSE emitter — manages a single SSE connection.
 * Send events to the channel; the connection loop writes them to the socket.
 */
class SseEmitter(val id: String) {
    private val eventChannel = Channel<SseEvent>(Channel.BUFFERED)
    val events: Flow<SseEvent> = eventChannel.receiveAsFlow()
    var closed = false
        private set

    /** Send an event to this emitter */
    suspend fun send(event: SseEvent) {
        if (!closed) eventChannel.send(event)
    }

    /** Send a simple data-only event */
    suspend fun send(data: String, event: String? = null, id: String? = null) {
        send(SseEvent(data, event, id))
    }

    /** Close this emitter */
    fun close() {
        closed = true
        eventChannel.close()
    }

    companion object {
        /** Create the SSE HTTP response headers (must be written before event streaming begins) */
        fun createSseResponse(): HttpResponse = HttpResponse(
            status = 200,
            statusMessage = "OK",
            headers = mapOf(
                "Content-Type" to "text/event-stream",
                "Cache-Control" to "no-cache",
                "Connection" to "keep-alive",
                "X-Accel-Buffering" to "no", // Disable nginx buffering
            ),
        )
    }
}

/**
 * SSE connection manager — tracks all active SSE connections for broadcast
 */
class SseConnectionManager {
    private val emitters = mutableMapOf<String, SseEmitter>()
    private val mutex = kotlinx.coroutines.sync.Mutex()

    /** Create a new SSE emitter and register it */
    suspend fun createEmitter(id: String): SseEmitter {
        val emitter = SseEmitter(id)
        mutex.withLock { emitters[id] = emitter }
        return emitter
    }

    /** Remove an emitter */
    suspend fun removeEmitter(id: String) {
        mutex.withLock { emitters.remove(id) }?.close()
    }

    /** Broadcast an event to all connected emitters */
    suspend fun broadcast(event: SseEvent) {
        val snapshot = mutex.withLock { emitters.toMap() }
        val deadEmitters = mutableListOf<String>()
        for ((id, emitter) in snapshot) {
            if (emitter.closed) { deadEmitters.add(id); continue }
            try { emitter.send(event) } catch (_: Exception) { deadEmitters.add(id) }
        }
        if (deadEmitters.isNotEmpty()) {
            mutex.withLock { deadEmitters.forEach { emitters.remove(it) } }
        }
    }

    /** Broadcast to emitters matching a filter */
    suspend fun broadcast(event: SseEvent, filter: (String) -> Boolean) {
        val snapshot = mutex.withLock { emitters.toMap() }
        for ((id, emitter) in snapshot) {
            if (!emitter.closed && filter(id)) {
                try { emitter.send(event) } catch (_: Exception) {}
            }
        }
    }

    suspend fun activeCount(): Int = mutex.withLock { emitters.count { !it.value.closed } }
    suspend fun getEmitter(id: String): SseEmitter? = mutex.withLock { emitters[id] }
}
