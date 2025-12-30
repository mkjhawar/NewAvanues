package com.augmentalis.avamagic.ipc.dsl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Benchmark utilities for DSL vs JSON serialization
 *
 * Measures:
 * - Serialization time
 * - Deserialization time
 * - Output size
 * - Memory usage
 *
 * Usage:
 * ```kotlin
 * val benchmark = DSLBenchmark()
 * val results = benchmark.runBenchmark(component, iterations = 1000)
 * println(results.report())
 * ```
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-11-19
 */
class DSLBenchmark {

    private val serializer = DSLSerializer()
    private val json = Json {
        prettyPrint = false
        encodeDefaults = false
    }

    /**
     * Run comprehensive benchmark
     */
    fun runBenchmark(
        component: UIComponent,
        iterations: Int = 100
    ): BenchmarkResults {
        // Warmup
        repeat(10) {
            serializer.serialize(component)
            json.encodeToString(component)
        }

        // DSL serialization
        val dslSerializeTimes = mutableListOf<Long>()
        repeat(iterations) {
            val start = System.nanoTime()
            serializer.serialize(component)
            dslSerializeTimes.add(System.nanoTime() - start)
        }

        // JSON serialization
        val jsonSerializeTimes = mutableListOf<Long>()
        repeat(iterations) {
            val start = System.nanoTime()
            json.encodeToString(component)
            jsonSerializeTimes.add(System.nanoTime() - start)
        }

        // Get serialized outputs
        val dslOutput = serializer.serialize(component)
        val jsonOutput = json.encodeToString(component)

        // DSL deserialization
        val dslDeserializeTimes = mutableListOf<Long>()
        repeat(iterations) {
            val start = System.nanoTime()
            serializer.deserialize(dslOutput)
            dslDeserializeTimes.add(System.nanoTime() - start)
        }

        // JSON deserialization
        val jsonDeserializeTimes = mutableListOf<Long>()
        repeat(iterations) {
            val start = System.nanoTime()
            json.decodeFromString<UIComponent>(jsonOutput)
            jsonDeserializeTimes.add(System.nanoTime() - start)
        }

        return BenchmarkResults(
            iterations = iterations,
            dslSerializeAvgNs = dslSerializeTimes.average().toLong(),
            dslSerializeMinNs = dslSerializeTimes.minOrNull() ?: 0,
            dslSerializeMaxNs = dslSerializeTimes.maxOrNull() ?: 0,
            jsonSerializeAvgNs = jsonSerializeTimes.average().toLong(),
            jsonSerializeMinNs = jsonSerializeTimes.minOrNull() ?: 0,
            jsonSerializeMaxNs = jsonSerializeTimes.maxOrNull() ?: 0,
            dslDeserializeAvgNs = dslDeserializeTimes.average().toLong(),
            dslDeserializeMinNs = dslDeserializeTimes.minOrNull() ?: 0,
            dslDeserializeMaxNs = dslDeserializeTimes.maxOrNull() ?: 0,
            jsonDeserializeAvgNs = jsonDeserializeTimes.average().toLong(),
            jsonDeserializeMinNs = jsonDeserializeTimes.minOrNull() ?: 0,
            jsonDeserializeMaxNs = jsonDeserializeTimes.maxOrNull() ?: 0,
            dslSizeBytes = dslOutput.toByteArray().size,
            jsonSizeBytes = jsonOutput.toByteArray().size
        )
    }

    /**
     * Run quick benchmark
     */
    fun quickBenchmark(component: UIComponent): QuickBenchmarkResult {
        val dslOutput = serializer.serialize(component)
        val jsonOutput = json.encodeToString(component)

        val dslSerializeStart = System.nanoTime()
        repeat(10) { serializer.serialize(component) }
        val dslSerializeTime = (System.nanoTime() - dslSerializeStart) / 10

        val jsonSerializeStart = System.nanoTime()
        repeat(10) { json.encodeToString(component) }
        val jsonSerializeTime = (System.nanoTime() - jsonSerializeStart) / 10

        return QuickBenchmarkResult(
            dslSerializeUs = dslSerializeTime / 1000,
            jsonSerializeUs = jsonSerializeTime / 1000,
            dslSizeBytes = dslOutput.toByteArray().size,
            jsonSizeBytes = jsonOutput.toByteArray().size,
            sizeReductionPercent = ((jsonOutput.length - dslOutput.length).toFloat() / jsonOutput.length * 100).toInt(),
            speedupPercent = ((jsonSerializeTime - dslSerializeTime).toFloat() / jsonSerializeTime * 100).toInt()
        )
    }

    /**
     * Create sample component tree for testing
     */
    companion object {
        fun createSampleSmall(): UIComponent {
            return UIComponent(
                type = "Column",
                id = "main",
                properties = mapOf("spacing" to 16),
                modifiers = listOf(UIModifier.Padding(16f)),
                children = listOf(
                    UIComponent(
                        type = "Text",
                        properties = mapOf("text" to "Hello World")
                    ),
                    UIComponent(
                        type = "Button",
                        id = "btn1",
                        properties = mapOf("label" to "Click Me"),
                        callbacks = mapOf("onClick" to "handleClick")
                    )
                )
            )
        }

        fun createSampleMedium(): UIComponent {
            return UIComponent(
                type = "Scaffold",
                id = "scaffold",
                children = listOf(
                    UIComponent(
                        type = "AppBar",
                        properties = mapOf("title" to "Dashboard")
                    ),
                    UIComponent(
                        type = "Column",
                        properties = mapOf("spacing" to 16),
                        modifiers = listOf(UIModifier.Padding(16f)),
                        children = (1..10).map { i ->
                            UIComponent(
                                type = "Card",
                                id = "card$i",
                                modifiers = listOf(
                                    UIModifier.CornerRadius(12f),
                                    UIModifier.Shadow(4f)
                                ),
                                children = listOf(
                                    UIComponent(
                                        type = "Text",
                                        properties = mapOf(
                                            "text" to "Card $i",
                                            "font" to "headline"
                                        )
                                    ),
                                    UIComponent(
                                        type = "Text",
                                        properties = mapOf(
                                            "text" to "Description for card $i"
                                        )
                                    )
                                )
                            )
                        }
                    )
                )
            )
        }

        fun createSampleLarge(): UIComponent {
            return UIComponent(
                type = "Scaffold",
                id = "scaffold",
                children = listOf(
                    UIComponent(
                        type = "AppBar",
                        properties = mapOf("title" to "Complex App")
                    ),
                    UIComponent(
                        type = "Column",
                        properties = mapOf("spacing" to 8),
                        children = (1..50).map { i ->
                            UIComponent(
                                type = "ListTile",
                                id = "tile$i",
                                properties = mapOf(
                                    "title" to "Item $i",
                                    "subtitle" to "Subtitle for item $i"
                                ),
                                modifiers = listOf(
                                    UIModifier.Padding(12f),
                                    UIModifier.Background("#FFFFFF")
                                ),
                                callbacks = mapOf(
                                    "onClick" to "handleItemClick_$i",
                                    "onLongPress" to "handleLongPress_$i"
                                ),
                                children = listOf(
                                    UIComponent(
                                        type = "Avatar",
                                        properties = mapOf("initials" to "U$i")
                                    ),
                                    UIComponent(
                                        type = "Icon",
                                        properties = mapOf("name" to "chevron.right")
                                    )
                                )
                            )
                        }
                    ),
                    UIComponent(
                        type = "BottomNav",
                        children = listOf(
                            UIComponent(type = "NavItem", properties = mapOf("label" to "Home", "icon" to "house")),
                            UIComponent(type = "NavItem", properties = mapOf("label" to "Search", "icon" to "search")),
                            UIComponent(type = "NavItem", properties = mapOf("label" to "Profile", "icon" to "person"))
                        )
                    )
                )
            )
        }
    }
}

/**
 * Full benchmark results
 */
data class BenchmarkResults(
    val iterations: Int,
    val dslSerializeAvgNs: Long,
    val dslSerializeMinNs: Long,
    val dslSerializeMaxNs: Long,
    val jsonSerializeAvgNs: Long,
    val jsonSerializeMinNs: Long,
    val jsonSerializeMaxNs: Long,
    val dslDeserializeAvgNs: Long,
    val dslDeserializeMinNs: Long,
    val dslDeserializeMaxNs: Long,
    val jsonDeserializeAvgNs: Long,
    val jsonDeserializeMinNs: Long,
    val jsonDeserializeMaxNs: Long,
    val dslSizeBytes: Int,
    val jsonSizeBytes: Int
) {
    val serializeSpeedup: Float
        get() = jsonSerializeAvgNs.toFloat() / dslSerializeAvgNs

    val deserializeSpeedup: Float
        get() = jsonDeserializeAvgNs.toFloat() / dslDeserializeAvgNs

    val sizeSavingsPercent: Int
        get() = ((jsonSizeBytes - dslSizeBytes).toFloat() / jsonSizeBytes * 100).toInt()

    fun report(): String = buildString {
        append("=== DSL vs JSON Benchmark Results ===\n")
        append("Iterations: $iterations\n\n")

        append("SERIALIZATION:\n")
        append("  DSL:  avg=${dslSerializeAvgNs / 1000}µs, min=${dslSerializeMinNs / 1000}µs, max=${dslSerializeMaxNs / 1000}µs\n")
        append("  JSON: avg=${jsonSerializeAvgNs / 1000}µs, min=${jsonSerializeMinNs / 1000}µs, max=${jsonSerializeMaxNs / 1000}µs\n")
        append("  Speedup: ${String.format("%.2f", serializeSpeedup)}x\n\n")

        append("DESERIALIZATION:\n")
        append("  DSL:  avg=${dslDeserializeAvgNs / 1000}µs, min=${dslDeserializeMinNs / 1000}µs, max=${dslDeserializeMaxNs / 1000}µs\n")
        append("  JSON: avg=${jsonDeserializeAvgNs / 1000}µs, min=${jsonDeserializeMinNs / 1000}µs, max=${jsonDeserializeMaxNs / 1000}µs\n")
        append("  Speedup: ${String.format("%.2f", deserializeSpeedup)}x\n\n")

        append("SIZE:\n")
        append("  DSL:  $dslSizeBytes bytes\n")
        append("  JSON: $jsonSizeBytes bytes\n")
        append("  Savings: $sizeSavingsPercent%\n")
    }
}

/**
 * Quick benchmark result
 */
data class QuickBenchmarkResult(
    val dslSerializeUs: Long,
    val jsonSerializeUs: Long,
    val dslSizeBytes: Int,
    val jsonSizeBytes: Int,
    val sizeReductionPercent: Int,
    val speedupPercent: Int
) {
    override fun toString(): String {
        return "DSL: ${dslSerializeUs}µs / ${dslSizeBytes}B | JSON: ${jsonSerializeUs}µs / ${jsonSizeBytes}B | " +
               "Size: -$sizeReductionPercent% | Speed: +$speedupPercent%"
    }
}
