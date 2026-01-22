package com.augmentalis.avamagic.observability.logging

import com.augmentalis.avamagic.observability.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Structured Logger Interface
 */
interface StructuredLogger {
    fun trace(message: String, attributes: Map<String, Any> = emptyMap())
    fun debug(message: String, attributes: Map<String, Any> = emptyMap())
    fun info(message: String, attributes: Map<String, Any> = emptyMap())
    fun warn(message: String, attributes: Map<String, Any> = emptyMap())
    fun error(message: String, attributes: Map<String, Any> = emptyMap(), throwable: Throwable? = null)
    fun fatal(message: String, attributes: Map<String, Any> = emptyMap(), throwable: Throwable? = null)

    fun withContext(attributes: Map<String, Any>): StructuredLogger
}

/**
 * Structured Logger Implementation
 */
class StructuredLoggerImpl(
    private val serviceName: String,
    private val config: ObservabilityConfig,
    private val contextAttributes: Map<String, Any> = emptyMap()
) : StructuredLogger {

    private val buffer = mutableListOf<LogEntry>()
    private var logCount = 0L
    private var exportJob: Job? = null

    private val json = Json {
        prettyPrint = false
        encodeDefaults = false
    }

    override fun trace(message: String, attributes: Map<String, Any>) {
        log(LogLevel.TRACE, message, attributes)
    }

    override fun debug(message: String, attributes: Map<String, Any>) {
        log(LogLevel.DEBUG, message, attributes)
    }

    override fun info(message: String, attributes: Map<String, Any>) {
        log(LogLevel.INFO, message, attributes)
    }

    override fun warn(message: String, attributes: Map<String, Any>) {
        log(LogLevel.WARN, message, attributes)
    }

    override fun error(message: String, attributes: Map<String, Any>, throwable: Throwable?) {
        log(LogLevel.ERROR, message, attributes, throwable)
    }

    override fun fatal(message: String, attributes: Map<String, Any>, throwable: Throwable?) {
        log(LogLevel.FATAL, message, attributes, throwable)
    }

    override fun withContext(attributes: Map<String, Any>): StructuredLogger {
        return StructuredLoggerImpl(
            serviceName = serviceName,
            config = config,
            contextAttributes = contextAttributes + attributes
        )
    }

    private fun log(
        level: LogLevel,
        message: String,
        attributes: Map<String, Any>,
        throwable: Throwable? = null
    ) {
        if (!config.loggingEnabled) return
        if (level.ordinal < config.logLevel.ordinal) return

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message,
            service = serviceName,
            attributes = contextAttributes + attributes + config.additionalAttributes,
            traceId = getCurrentTraceId(),
            spanId = getCurrentSpanId(),
            exception = throwable?.let { ExceptionInfo.from(it) }
        )

        synchronized(buffer) {
            buffer.add(entry)
            logCount++

            if (buffer.size >= config.batchSize) {
                export()
            }
        }
    }

    fun start() {
        if (!config.loggingEnabled) return

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

    fun getLogCount(): Long = logCount

    private fun export() {
        val toExport = synchronized(buffer) {
            val copy = buffer.toList()
            buffer.clear()
            copy
        }

        if (toExport.isEmpty()) return

        when (config.exporterType) {
            ExporterType.CONSOLE -> {
                toExport.forEach { entry ->
                    val timestamp = formatTimestamp(entry.timestamp)
                    val level = entry.level.name.padEnd(5)
                    val attrs = if (entry.attributes.isNotEmpty()) {
                        " " + entry.attributes.entries.joinToString(", ") { "${it.key}=${it.value}" }
                    } else ""

                    println("$timestamp [$level] ${entry.message}$attrs")

                    entry.exception?.let { ex ->
                        println("  Exception: ${ex.type}: ${ex.message}")
                        ex.stackTrace.take(5).forEach { frame ->
                            println("    at $frame")
                        }
                    }
                }
            }
            else -> {
                // TODO: Implement OTLP exporter
                toExport.forEach { entry ->
                    println(json.encodeToString(entry.toSerializable()))
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        // Simple ISO-like format
        val seconds = timestamp / 1000
        val millis = timestamp % 1000
        return "$seconds.${millis.toString().padStart(3, '0')}"
    }

    private fun getCurrentTraceId(): String? {
        // TODO: Get from current trace context
        return null
    }

    private fun getCurrentSpanId(): String? {
        // TODO: Get from current span context
        return null
    }
}

/**
 * Log entry
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val message: String,
    val service: String,
    val attributes: Map<String, Any> = emptyMap(),
    val traceId: String? = null,
    val spanId: String? = null,
    val exception: ExceptionInfo? = null
) {
    fun toSerializable(): SerializableLogEntry {
        return SerializableLogEntry(
            timestamp = timestamp,
            level = level.name,
            message = message,
            service = service,
            attributes = attributes.mapValues { it.value.toString() },
            traceId = traceId,
            spanId = spanId,
            exception = exception
        )
    }
}

@Serializable
data class SerializableLogEntry(
    val timestamp: Long,
    val level: String,
    val message: String,
    val service: String,
    val attributes: Map<String, String> = emptyMap(),
    val traceId: String? = null,
    val spanId: String? = null,
    val exception: ExceptionInfo? = null
)

/**
 * Exception information
 */
@Serializable
data class ExceptionInfo(
    val type: String,
    val message: String?,
    val stackTrace: List<String>
) {
    companion object {
        fun from(throwable: Throwable): ExceptionInfo {
            return ExceptionInfo(
                type = throwable::class.simpleName ?: "Unknown",
                message = throwable.message,
                stackTrace = throwable.stackTraceToString().lines().take(20)
            )
        }
    }
}

/**
 * Log query builder for searching logs
 */
class LogQuery private constructor(
    val service: String? = null,
    val level: LogLevel? = null,
    val minLevel: LogLevel? = null,
    val messageContains: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val startTime: Long? = null,
    val endTime: Long? = null,
    val traceId: String? = null,
    val limit: Int = 100
) {
    class Builder {
        private var service: String? = null
        private var level: LogLevel? = null
        private var minLevel: LogLevel? = null
        private var messageContains: String? = null
        private var attributes = mutableMapOf<String, String>()
        private var startTime: Long? = null
        private var endTime: Long? = null
        private var traceId: String? = null
        private var limit: Int = 100

        fun service(service: String) = apply { this.service = service }
        fun level(level: LogLevel) = apply { this.level = level }
        fun minLevel(level: LogLevel) = apply { this.minLevel = level }
        fun messageContains(text: String) = apply { this.messageContains = text }
        fun attribute(key: String, value: String) = apply { this.attributes[key] = value }
        fun timeRange(start: Long, end: Long) = apply {
            this.startTime = start
            this.endTime = end
        }
        fun traceId(id: String) = apply { this.traceId = id }
        fun limit(n: Int) = apply { this.limit = n }

        fun build() = LogQuery(
            service, level, minLevel, messageContains, attributes,
            startTime, endTime, traceId, limit
        )
    }

    companion object {
        fun builder() = Builder()
    }
}
