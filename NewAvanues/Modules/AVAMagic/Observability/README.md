# Observability Module

**Version:** 1.0.0
**Date:** 2025-11-19
**Author:** Manoj Jhawar (manoj@ideahq.net)

---

## Overview

Unified observability system for Avanues/AVAMagic implementing OpenTelemetry-style instrumentation with three pillars:

1. **Metrics** - Quantitative measurements (counters, gauges, histograms, timers)
2. **Logging** - Structured log events with context
3. **Tracing** - Distributed request tracing across services

### Features

- Cross-platform (Android, iOS, Web, Desktop)
- Low overhead (<1% CPU, <1MB memory)
- Batched export for efficiency
- Configurable sampling rates
- Context propagation
- Multiple exporter support

---

## Quick Start

```kotlin
// Initialize
val observability = Observability.initialize(
    serviceName = "com.avanue.myapp",
    config = ObservabilityConfig(
        metricsEnabled = true,
        loggingEnabled = true,
        tracingEnabled = true,
        exporterType = ExporterType.CONSOLE
    )
)

// Start collection
observability.start()

// Use metrics
observability.metrics.counter("api_requests").increment()
observability.metrics.gauge("active_users").set(42.0)
observability.metrics.histogram("response_time").record(150.0)

// Use logging
observability.logger.info("User logged in", mapOf("userId" to "123"))
observability.logger.error("Failed to connect", mapOf("host" to "api.example.com"))

// Use tracing
observability.tracer.span("processRequest") {
    setAttribute("userId", "123")

    // Nested span
    observability.tracer.span("fetchData") {
        // Your code
    }
}

// Stop and flush
observability.stop()
```

---

## Metrics

### Counter
Monotonically increasing value.

```kotlin
val counter = metrics.counter("http_requests_total", "Total HTTP requests")
counter.increment()
counter.increment(5)
```

### Gauge
Point-in-time value that can go up or down.

```kotlin
val gauge = metrics.gauge("active_connections", "Current active connections")
gauge.set(42.0)
gauge.increment()
gauge.decrement()
```

### Histogram
Distribution of values with percentiles.

```kotlin
val histogram = metrics.histogram(
    "request_duration_ms",
    "Request duration in milliseconds",
    buckets = listOf(10.0, 50.0, 100.0, 500.0, 1000.0)
)
histogram.record(150.0)

val stats = histogram.getStats()
println("p50: ${stats.p50}, p95: ${stats.p95}, p99: ${stats.p99}")
```

### Timer
Measures duration automatically.

```kotlin
val timer = metrics.timer("db_query_time")

// Synchronous
val result = timer.record {
    database.query("SELECT * FROM users")
}

// Suspending
val result = timer.recordSuspend {
    api.fetchData()
}
```

---

## Logging

### Log Levels
- TRACE
- DEBUG
- INFO
- WARN
- ERROR
- FATAL

### Basic Usage

```kotlin
logger.trace("Entering function", mapOf("function" to "process"))
logger.debug("Processing item", mapOf("itemId" to "123"))
logger.info("User action", mapOf("action" to "login", "userId" to "456"))
logger.warn("Rate limit approaching", mapOf("current" to 90, "limit" to 100))
logger.error("Connection failed", mapOf("host" to "api.com"), exception)
logger.fatal("Database corrupted", mapOf("table" to "users"))
```

### Contextual Logger

```kotlin
val requestLogger = logger.withContext(mapOf(
    "requestId" to "req-123",
    "userId" to "user-456"
))

requestLogger.info("Processing request")  // Includes requestId and userId
requestLogger.info("Request complete")
```

### Structured Attributes

All logs include:
- Timestamp
- Service name
- Log level
- Message
- Custom attributes
- Trace ID (if tracing enabled)
- Span ID (if in span)
- Exception info (if provided)

---

## Tracing

### Basic Spans

```kotlin
tracer.span("operation") {
    // Your code here
}

// With attributes
tracer.span("fetchUser", mapOf("userId" to "123")) {
    setAttribute("cached", false)
    // Your code
}
```

### Nested Spans

```kotlin
tracer.span("handleRequest") {
    setAttribute("endpoint", "/api/users")

    tracer.span("authenticate") {
        // Auth logic
    }

    tracer.span("fetchData") {
        // Data fetching
    }

    tracer.span("formatResponse") {
        // Formatting
    }
}
```

### Manual Span Control

```kotlin
val span = tracer.startSpan("longOperation")
try {
    // Long running work
    span.addEvent("checkpoint", mapOf("progress" to 50))
    // More work
} catch (e: Exception) {
    span.setStatus(SpanStatus.ERROR, e.message)
    span.recordException(e)
    throw e
} finally {
    span.end()
}
```

### Async/Suspend Spans

```kotlin
tracer.spanSuspend("asyncOperation") {
    val result = async { fetchData() }.await()
    setAttribute("resultSize", result.size)
}
```

### Context Propagation

```kotlin
// Get current trace context
val context = TraceContext(
    traceId = tracer.getTraceId()!!,
    spanId = tracer.getCurrentSpan()!!.spanId,
    sampled = true
)

// Propagate via header
val header = context.toHeader()
// "00-{traceId}-{spanId}-01"

// Receive and parse
val received = TraceContext.fromHeader(header)
```

---

## Configuration

```kotlin
ObservabilityConfig(
    // Enable/disable pillars
    metricsEnabled = true,
    loggingEnabled = true,
    tracingEnabled = true,

    // Export settings
    exporterType = ExporterType.CONSOLE,  // CONSOLE, OTLP, PROMETHEUS, JAEGER, ZIPKIN
    exportIntervalMs = 60_000,
    batchSize = 100,

    // Sampling
    samplingRate = 1.0f,  // 1.0 = 100%, 0.1 = 10%

    // Logging
    logLevel = LogLevel.INFO,

    // Metrics
    metricsPrefix = "myapp",

    // Global attributes
    additionalAttributes = mapOf(
        "environment" to "production",
        "version" to "1.0.0"
    )
)
```

---

## Exporters

### Console (Development)
```kotlin
exporterType = ExporterType.CONSOLE
```

### OpenTelemetry Protocol (Production)
```kotlin
exporterType = ExporterType.OTLP
// TODO: Configure OTLP endpoint
```

### Prometheus (Metrics)
```kotlin
exporterType = ExporterType.PROMETHEUS
// TODO: Configure scrape endpoint
```

### Jaeger/Zipkin (Tracing)
```kotlin
exporterType = ExporterType.JAEGER
// or
exporterType = ExporterType.ZIPKIN
```

---

## Statistics

```kotlin
val stats = observability.getStats()
println(stats)
// Stats(metrics=150, logs=320, spans=45, exported=500, dropped=0)
```

---

## Best Practices

### Metrics
- Use descriptive names with units: `request_duration_ms`
- Add descriptions for documentation
- Use labels/attributes sparingly (high cardinality = high cost)

### Logging
- Use structured attributes, not string interpolation
- Set appropriate log levels
- Include context (userId, requestId, etc.)

### Tracing
- Name spans by operation, not code location
- Add meaningful attributes
- Keep spans focused (not too long)
- Use sampling in production

---

## Files

```
modules/AVAMagic/Observability/
├── build.gradle.kts
├── README.md
└── src/
    └── commonMain/
        └── kotlin/
            └── com/augmentalis/avamagic/observability/
                ├── Observability.kt
                ├── metrics/
                │   └── Metrics.kt
                ├── logging/
                │   └── Logging.kt
                └── tracing/
                    └── Tracing.kt
```

---

## Performance

| Operation | Target | Notes |
|-----------|--------|-------|
| Counter increment | <100ns | Lock-free |
| Gauge set | <100ns | Lock-free |
| Histogram record | <1µs | Bucket lookup |
| Log entry | <10µs | Buffered |
| Span start | <1µs | ID generation |
| Span end | <1µs | Buffered |

Memory overhead: ~100KB base + ~1KB per 100 metrics/logs/spans

---

## Dependencies

- `kotlinx-serialization-json:1.6.0`
- `kotlinx-coroutines-core:1.7.3`

---

## License

Proprietary - Augmentalis ES

---

**IDEACODE Version:** 8.4
**Created by:** Manoj Jhawar (manoj@ideahq.net)
