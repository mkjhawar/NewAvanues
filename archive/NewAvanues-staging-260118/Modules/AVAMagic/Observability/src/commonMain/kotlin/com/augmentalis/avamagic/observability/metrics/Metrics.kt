package com.augmentalis.avamagic.observability.metrics

import com.augmentalis.avamagic.observability.*
import kotlinx.coroutines.*
import kotlin.math.sqrt

/**
 * Metrics Provider Interface
 */
interface MetricsProvider {
    fun counter(name: String, description: String = ""): Counter
    fun gauge(name: String, description: String = ""): Gauge
    fun histogram(name: String, description: String = "", buckets: List<Double> = defaultBuckets): Histogram
    fun timer(name: String, description: String = ""): Timer
}

/**
 * Metrics Provider Implementation
 */
class MetricsProviderImpl(
    private val serviceName: String,
    private val config: ObservabilityConfig
) : MetricsProvider {

    private val counters = mutableMapOf<String, CounterImpl>()
    private val gauges = mutableMapOf<String, GaugeImpl>()
    private val histograms = mutableMapOf<String, HistogramImpl>()
    private val timers = mutableMapOf<String, TimerImpl>()

    private var metricCount = 0L
    private var exportJob: Job? = null

    override fun counter(name: String, description: String): Counter {
        val fullName = prefixName(name)
        return counters.getOrPut(fullName) {
            CounterImpl(fullName, description)
        }
    }

    override fun gauge(name: String, description: String): Gauge {
        val fullName = prefixName(name)
        return gauges.getOrPut(fullName) {
            GaugeImpl(fullName, description)
        }
    }

    override fun histogram(name: String, description: String, buckets: List<Double>): Histogram {
        val fullName = prefixName(name)
        return histograms.getOrPut(fullName) {
            HistogramImpl(fullName, description, buckets)
        }
    }

    override fun timer(name: String, description: String): Timer {
        val fullName = prefixName(name)
        return timers.getOrPut(fullName) {
            TimerImpl(fullName, description)
        }
    }

    fun start() {
        if (!config.metricsEnabled) return

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

    fun getMetricCount(): Long = metricCount

    fun collectAll(): List<MetricData> {
        val metrics = mutableListOf<MetricData>()

        counters.values.forEach { counter ->
            metrics.add(MetricData(
                name = counter.name,
                description = counter.description,
                type = MetricType.COUNTER,
                value = counter.get().toDouble(),
                timestamp = System.currentTimeMillis(),
                attributes = Attributes.EMPTY
            ))
        }

        gauges.values.forEach { gauge ->
            metrics.add(MetricData(
                name = gauge.name,
                description = gauge.description,
                type = MetricType.GAUGE,
                value = gauge.get(),
                timestamp = System.currentTimeMillis(),
                attributes = Attributes.EMPTY
            ))
        }

        histograms.values.forEach { histogram ->
            val stats = histogram.getStats()
            metrics.add(MetricData(
                name = histogram.name,
                description = histogram.description,
                type = MetricType.HISTOGRAM,
                value = stats.mean,
                timestamp = System.currentTimeMillis(),
                attributes = Attributes.of(
                    "count" to stats.count,
                    "sum" to stats.sum,
                    "min" to stats.min,
                    "max" to stats.max,
                    "p50" to stats.p50,
                    "p95" to stats.p95,
                    "p99" to stats.p99
                )
            ))
        }

        timers.values.forEach { timer ->
            val stats = timer.getStats()
            metrics.add(MetricData(
                name = timer.name,
                description = timer.description,
                type = MetricType.TIMER,
                value = stats.mean,
                timestamp = System.currentTimeMillis(),
                attributes = Attributes.of(
                    "count" to stats.count,
                    "totalMs" to stats.sum,
                    "minMs" to stats.min,
                    "maxMs" to stats.max
                )
            ))
        }

        return metrics
    }

    private fun export() {
        if (!config.metricsEnabled) return

        val metrics = collectAll()
        metricCount += metrics.size

        when (config.exporterType) {
            ExporterType.CONSOLE -> {
                metrics.forEach { metric ->
                    println("[METRIC] ${metric.name}: ${metric.value} (${metric.type})")
                }
            }
            else -> {
                // TODO: Implement OTLP, Prometheus exporters
            }
        }
    }

    private fun prefixName(name: String): String {
        return if (config.metricsPrefix.isNotEmpty()) {
            "${config.metricsPrefix}_$name"
        } else {
            name
        }
    }
}

/**
 * Counter - monotonically increasing value
 */
interface Counter {
    val name: String
    fun increment(amount: Long = 1)
    fun get(): Long
}

class CounterImpl(
    override val name: String,
    val description: String
) : Counter {
    private var value = 0L

    override fun increment(amount: Long) {
        value += amount
    }

    override fun get(): Long = value
}

/**
 * Gauge - point-in-time value
 */
interface Gauge {
    val name: String
    fun set(value: Double)
    fun increment(amount: Double = 1.0)
    fun decrement(amount: Double = 1.0)
    fun get(): Double
}

class GaugeImpl(
    override val name: String,
    val description: String
) : Gauge {
    private var value = 0.0

    override fun set(value: Double) {
        this.value = value
    }

    override fun increment(amount: Double) {
        value += amount
    }

    override fun decrement(amount: Double) {
        value -= amount
    }

    override fun get(): Double = value
}

/**
 * Histogram - distribution of values
 */
interface Histogram {
    val name: String
    fun record(value: Double)
    fun getStats(): HistogramStats
}

class HistogramImpl(
    override val name: String,
    val description: String,
    private val buckets: List<Double>
) : Histogram {
    private val values = mutableListOf<Double>()
    private val bucketCounts = buckets.associateWith { 0L }.toMutableMap()

    override fun record(value: Double) {
        values.add(value)
        buckets.forEach { bucket ->
            if (value <= bucket) {
                bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + 1
            }
        }
    }

    override fun getStats(): HistogramStats {
        if (values.isEmpty()) {
            return HistogramStats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val sorted = values.sorted()
        val count = values.size.toLong()
        val sum = values.sum()
        val mean = sum / count
        val min = sorted.first()
        val max = sorted.last()

        val variance = values.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        val p50 = percentile(sorted, 0.50)
        val p95 = percentile(sorted, 0.95)
        val p99 = percentile(sorted, 0.99)

        return HistogramStats(count, sum, mean, min, max, stdDev, p50, p95, p99)
    }

    private fun percentile(sorted: List<Double>, p: Double): Double {
        val index = (sorted.size * p).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }
}

/**
 * Timer - measures duration
 */
interface Timer {
    val name: String
    fun <T> record(block: () -> T): T
    suspend fun <T> recordSuspend(block: suspend () -> T): T
    fun recordMs(durationMs: Long)
    fun getStats(): TimerStats
}

class TimerImpl(
    override val name: String,
    val description: String
) : Timer {
    private val durations = mutableListOf<Long>()

    override fun <T> record(block: () -> T): T {
        val start = System.currentTimeMillis()
        try {
            return block()
        } finally {
            durations.add(System.currentTimeMillis() - start)
        }
    }

    override suspend fun <T> recordSuspend(block: suspend () -> T): T {
        val start = System.currentTimeMillis()
        try {
            return block()
        } finally {
            durations.add(System.currentTimeMillis() - start)
        }
    }

    override fun recordMs(durationMs: Long) {
        durations.add(durationMs)
    }

    override fun getStats(): TimerStats {
        if (durations.isEmpty()) {
            return TimerStats(0, 0, 0.0, 0, 0)
        }

        return TimerStats(
            count = durations.size.toLong(),
            sum = durations.sum(),
            mean = durations.average(),
            min = durations.minOrNull() ?: 0,
            max = durations.maxOrNull() ?: 0
        )
    }
}

/**
 * Metric data for export
 */
data class MetricData(
    val name: String,
    val description: String,
    val type: MetricType,
    val value: Double,
    val timestamp: Long,
    val attributes: Attributes
)

enum class MetricType {
    COUNTER,
    GAUGE,
    HISTOGRAM,
    TIMER
}

data class HistogramStats(
    val count: Long,
    val sum: Double,
    val mean: Double,
    val min: Double,
    val max: Double,
    val stdDev: Double,
    val p50: Double,
    val p95: Double,
    val p99: Double
)

data class TimerStats(
    val count: Long,
    val sum: Long,
    val mean: Double,
    val min: Long,
    val max: Long
)

val defaultBuckets = listOf(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)
