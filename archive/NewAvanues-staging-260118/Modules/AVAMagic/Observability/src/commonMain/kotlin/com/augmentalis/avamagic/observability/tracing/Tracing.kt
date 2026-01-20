package com.augmentalis.avamagic.observability.tracing

import com.augmentalis.avamagic.observability.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Distributed Tracer Interface
 */
interface Tracer {
    fun <T> span(name: String, attributes: Map<String, Any> = emptyMap(), block: SpanScope.() -> T): T
    suspend fun <T> spanSuspend(name: String, attributes: Map<String, Any> = emptyMap(), block: suspend SpanScope.() -> T): T
    fun startSpan(name: String, attributes: Map<String, Any> = emptyMap()): Span
    fun getCurrentSpan(): Span?
    fun getTraceId(): String?
}

/**
 * Tracer Implementation
 */
class TracerImpl(
    private val serviceName: String,
    private val config: ObservabilityConfig
) : Tracer {

    private val spans = mutableListOf<SpanData>()
    private var spanCount = 0L
    private var exportJob: Job? = null

    // Thread-local current span (simplified)
    private var currentSpan: SpanImpl? = null
    private var currentTraceId: String? = null

    override fun <T> span(name: String, attributes: Map<String, Any>, block: SpanScope.() -> T): T {
        if (!config.tracingEnabled) {
            return block(NoOpSpanScope)
        }

        if (shouldSample()) {
            val span = createSpan(name, attributes)
            val previousSpan = currentSpan
            currentSpan = span

            try {
                val scope = SpanScopeImpl(span)
                return block(scope)
            } catch (e: Exception) {
                span.setStatus(SpanStatus.ERROR, e.message)
                span.recordException(e)
                throw e
            } finally {
                span.end()
                recordSpan(span)
                currentSpan = previousSpan
            }
        } else {
            return block(NoOpSpanScope)
        }
    }

    override suspend fun <T> spanSuspend(name: String, attributes: Map<String, Any>, block: suspend SpanScope.() -> T): T {
        if (!config.tracingEnabled) {
            return block(NoOpSpanScope)
        }

        if (shouldSample()) {
            val span = createSpan(name, attributes)
            val previousSpan = currentSpan
            currentSpan = span

            try {
                val scope = SpanScopeImpl(span)
                return block(scope)
            } catch (e: Exception) {
                span.setStatus(SpanStatus.ERROR, e.message)
                span.recordException(e)
                throw e
            } finally {
                span.end()
                recordSpan(span)
                currentSpan = previousSpan
            }
        } else {
            return block(NoOpSpanScope)
        }
    }

    override fun startSpan(name: String, attributes: Map<String, Any>): Span {
        if (!config.tracingEnabled || !shouldSample()) {
            return NoOpSpan
        }

        val span = createSpan(name, attributes)
        val previousSpan = currentSpan
        currentSpan = span
        span.previousSpan = previousSpan
        return span
    }

    override fun getCurrentSpan(): Span? = currentSpan

    override fun getTraceId(): String? = currentTraceId

    fun start() {
        if (!config.tracingEnabled) return

        exportJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(config.exportIntervalMs)
                export()
            }
        }
    }

    suspend fun flush() {
        exportJob?.cancel()
        export()
    }

    fun getSpanCount(): Long = spanCount

    private fun createSpan(name: String, attributes: Map<String, Any>): SpanImpl {
        val traceId = currentTraceId ?: generateTraceId().also { currentTraceId = it }
        val parentSpanId = currentSpan?.spanId

        return SpanImpl(
            traceId = traceId,
            spanId = generateSpanId(),
            parentSpanId = parentSpanId,
            name = name,
            service = serviceName,
            startTime = System.currentTimeMillis(),
            attributes = attributes.toMutableMap()
        )
    }

    private fun recordSpan(span: SpanImpl) {
        synchronized(spans) {
            spans.add(span.toSpanData())
            spanCount++

            if (spans.size >= config.batchSize) {
                export()
            }
        }

        // Clear trace ID if this was the root span
        if (span.parentSpanId == null) {
            currentTraceId = null
        }
    }

    private fun export() {
        val toExport = synchronized(spans) {
            val copy = spans.toList()
            spans.clear()
            copy
        }

        if (toExport.isEmpty()) return

        when (config.exporterType) {
            ExporterType.CONSOLE -> {
                toExport.forEach { span ->
                    val duration = span.endTime - span.startTime
                    val indent = if (span.parentSpanId != null) "  " else ""
                    println("${indent}[SPAN] ${span.name} (${duration}ms) - ${span.status}")
                }
            }
            else -> {
                // TODO: Implement OTLP, Jaeger, Zipkin exporters
            }
        }
    }

    private fun shouldSample(): Boolean {
        return Random.nextFloat() < config.samplingRate
    }

    private fun generateTraceId(): String {
        return buildString {
            repeat(32) {
                append(HEX_CHARS.random())
            }
        }
    }

    private fun generateSpanId(): String {
        return buildString {
            repeat(16) {
                append(HEX_CHARS.random())
            }
        }
    }

    companion object {
        private val HEX_CHARS = "0123456789abcdef".toCharArray()
    }
}

/**
 * Span interface
 */
interface Span {
    val spanId: String
    val traceId: String
    fun setAttribute(key: String, value: Any)
    fun setStatus(status: SpanStatus, message: String? = null)
    fun addEvent(name: String, attributes: Map<String, Any> = emptyMap())
    fun recordException(exception: Throwable)
    fun end()
}

/**
 * Span implementation
 */
class SpanImpl(
    override val traceId: String,
    override val spanId: String,
    val parentSpanId: String?,
    val name: String,
    val service: String,
    val startTime: Long,
    val attributes: MutableMap<String, Any>
) : Span {

    var endTime: Long = 0
    var status: SpanStatus = SpanStatus.OK
    var statusMessage: String? = null
    val events = mutableListOf<SpanEvent>()
    var previousSpan: SpanImpl? = null

    override fun setAttribute(key: String, value: Any) {
        attributes[key] = value
    }

    override fun setStatus(status: SpanStatus, message: String?) {
        this.status = status
        this.statusMessage = message
    }

    override fun addEvent(name: String, attributes: Map<String, Any>) {
        events.add(SpanEvent(
            name = name,
            timestamp = System.currentTimeMillis(),
            attributes = attributes
        ))
    }

    override fun recordException(exception: Throwable) {
        events.add(SpanEvent(
            name = "exception",
            timestamp = System.currentTimeMillis(),
            attributes = mapOf(
                "exception.type" to (exception::class.simpleName ?: "Unknown"),
                "exception.message" to (exception.message ?: ""),
                "exception.stacktrace" to exception.stackTraceToString().take(1000)
            )
        ))
    }

    override fun end() {
        endTime = System.currentTimeMillis()
    }

    fun toSpanData(): SpanData {
        return SpanData(
            traceId = traceId,
            spanId = spanId,
            parentSpanId = parentSpanId,
            name = name,
            service = service,
            startTime = startTime,
            endTime = endTime,
            status = status,
            statusMessage = statusMessage,
            attributes = attributes,
            events = events
        )
    }
}

/**
 * No-op span for when tracing is disabled
 */
object NoOpSpan : Span {
    override val spanId: String = ""
    override val traceId: String = ""
    override fun setAttribute(key: String, value: Any) {}
    override fun setStatus(status: SpanStatus, message: String?) {}
    override fun addEvent(name: String, attributes: Map<String, Any>) {}
    override fun recordException(exception: Throwable) {}
    override fun end() {}
}

/**
 * Span scope for DSL usage
 */
interface SpanScope {
    fun setAttribute(key: String, value: Any)
    fun setStatus(status: SpanStatus, message: String? = null)
    fun addEvent(name: String, attributes: Map<String, Any> = emptyMap())
}

class SpanScopeImpl(private val span: Span) : SpanScope {
    override fun setAttribute(key: String, value: Any) = span.setAttribute(key, value)
    override fun setStatus(status: SpanStatus, message: String?) = span.setStatus(status, message)
    override fun addEvent(name: String, attributes: Map<String, Any>) = span.addEvent(name, attributes)
}

object NoOpSpanScope : SpanScope {
    override fun setAttribute(key: String, value: Any) {}
    override fun setStatus(status: SpanStatus, message: String?) {}
    override fun addEvent(name: String, attributes: Map<String, Any>) {}
}

/**
 * Span status
 */
enum class SpanStatus {
    UNSET,
    OK,
    ERROR
}

/**
 * Span event
 */
@Serializable
data class SpanEvent(
    val name: String,
    val timestamp: Long,
    val attributes: Map<String, @Serializable(with = AnyToStringSerializer::class) Any>
)

/**
 * Span data for export
 */
@Serializable
data class SpanData(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String?,
    val name: String,
    val service: String,
    val startTime: Long,
    val endTime: Long,
    val status: SpanStatus,
    val statusMessage: String?,
    val attributes: Map<String, @Serializable(with = AnyToStringSerializer::class) Any>,
    val events: List<SpanEvent>
) {
    val durationMs: Long get() = endTime - startTime
}

/**
 * Trace context for propagation
 */
data class TraceContext(
    val traceId: String,
    val spanId: String,
    val sampled: Boolean
) {
    fun toHeader(): String = "00-$traceId-$spanId-${if (sampled) "01" else "00"}"

    companion object {
        fun fromHeader(header: String): TraceContext? {
            val parts = header.split("-")
            if (parts.size != 4) return null
            return TraceContext(
                traceId = parts[1],
                spanId = parts[2],
                sampled = parts[3] == "01"
            )
        }
    }
}

/**
 * Simple serializer for Any to String
 */
object AnyToStringSerializer : kotlinx.serialization.KSerializer<Any> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "Any", kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Any) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Any {
        return decoder.decodeString()
    }
}
