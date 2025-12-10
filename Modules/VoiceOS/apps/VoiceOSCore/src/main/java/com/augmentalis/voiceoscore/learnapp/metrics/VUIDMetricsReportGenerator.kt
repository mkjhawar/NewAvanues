/**
 * VUIDMetricsReportGenerator.kt - VUID metrics report generation
 * Path: VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/metrics/VUIDMetricsReportGenerator.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-08
 * Feature: LearnApp VUID Creation Fix - Phase 3 (Observability)
 *
 * Generates human-readable and machine-readable reports from VUID metrics.
 * Supports multiple formats: text, CSV, JSON.
 *
 * Part of: LearnApp-VUID-Creation-Fix-Spec-5081218-V1.md (Phase 3)
 */

package com.augmentalis.voiceoscore.learnapp.metrics

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Report format types
 */
enum class ReportFormat {
    TEXT,  // Human-readable text
    CSV,   // Comma-separated values
    JSON   // Machine-readable JSON
}

/**
 * VUID metrics report generator
 *
 * Generates reports from VUID creation metrics in various formats.
 * Can generate single-app reports or aggregate reports across apps.
 *
 * ## Usage
 * ```kotlin
 * val generator = VUIDMetricsReportGenerator(context)
 *
 * // Generate text report
 * val report = generator.generateReport(metrics, ReportFormat.TEXT)
 * Log.i(TAG, report)
 *
 * // Export to file
 * val file = generator.exportToFile(metrics, ReportFormat.CSV)
 * Log.i(TAG, "Report saved to: ${file.absolutePath}")
 * ```
 *
 * @param context Android context for file operations
 *
 * @since 2025-12-08 (Phase 3: Observability)
 */
class VUIDMetricsReportGenerator(
    private val context: Context
) {

    /**
     * Date formatter for report timestamps
     */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val filenameDateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    /**
     * Generate report from metrics
     *
     * @param metrics Metrics to report on
     * @param format Report format
     * @return Report string in specified format
     */
    fun generateReport(
        metrics: VUIDCreationMetrics,
        format: ReportFormat = ReportFormat.TEXT
    ): String {
        return when (format) {
            ReportFormat.TEXT -> generateTextReport(metrics)
            ReportFormat.CSV -> generateCsvReport(listOf(metrics))
            ReportFormat.JSON -> generateJsonReport(metrics)
        }
    }

    /**
     * Generate report from multiple metrics
     *
     * @param metricsList List of metrics to report on
     * @param format Report format
     * @return Report string in specified format
     */
    fun generateAggregateReport(
        metricsList: List<VUIDCreationMetrics>,
        format: ReportFormat = ReportFormat.TEXT
    ): String {
        return when (format) {
            ReportFormat.TEXT -> generateAggregateTextReport(metricsList)
            ReportFormat.CSV -> generateCsvReport(metricsList)
            ReportFormat.JSON -> generateAggregateJsonReport(metricsList)
        }
    }

    /**
     * Export report to file
     *
     * @param metrics Metrics to export
     * @param format File format
     * @param filename Optional filename (auto-generated if null)
     * @return File where report was saved
     */
    suspend fun exportToFile(
        metrics: VUIDCreationMetrics,
        format: ReportFormat = ReportFormat.TEXT,
        filename: String? = null
    ): File = withContext(Dispatchers.IO) {
        val report = generateReport(metrics, format)
        val extension = when (format) {
            ReportFormat.TEXT -> "txt"
            ReportFormat.CSV -> "csv"
            ReportFormat.JSON -> "json"
        }

        val file = getReportFile(filename ?: generateFilename(metrics, extension))
        file.writeText(report)
        file
    }

    /**
     * Export aggregate report to file
     *
     * @param metricsList List of metrics to export
     * @param format File format
     * @param filename Optional filename (auto-generated if null)
     * @return File where report was saved
     */
    suspend fun exportAggregateToFile(
        metricsList: List<VUIDCreationMetrics>,
        format: ReportFormat = ReportFormat.TEXT,
        filename: String? = null
    ): File = withContext(Dispatchers.IO) {
        val report = generateAggregateReport(metricsList, format)
        val extension = when (format) {
            ReportFormat.TEXT -> "txt"
            ReportFormat.CSV -> "csv"
            ReportFormat.JSON -> "json"
        }

        val file = getReportFile(filename ?: "vuid-metrics-aggregate-${filenameDateFormat.format(Date())}.$extension")
        file.writeText(report)
        file
    }

    /**
     * Generate human-readable text report
     */
    private fun generateTextReport(metrics: VUIDCreationMetrics): String {
        return metrics.toReportString() // Use existing method from VUIDCreationMetrics
    }

    /**
     * Generate aggregate text report
     */
    private fun generateAggregateTextReport(metricsList: List<VUIDCreationMetrics>): String {
        if (metricsList.isEmpty()) {
            return "No metrics available"
        }

        return buildString {
            appendLine("VUID Creation Aggregate Report")
            appendLine("=".repeat(50))
            appendLine("Generated: ${dateFormat.format(Date())}")
            appendLine("Total Explorations: ${metricsList.size}")
            appendLine()

            // Calculate aggregate statistics
            val totalElements = metricsList.sumOf { it.elementsDetected }
            val totalVuids = metricsList.sumOf { it.vuidsCreated }
            val avgRate = metricsList.map { it.creationRate }.average()
            val minRate = metricsList.minOf { it.creationRate }
            val maxRate = metricsList.maxOf { it.creationRate }

            appendLine("Aggregate Statistics:")
            appendLine("  Total Elements: $totalElements")
            appendLine("  Total VUIDs: $totalVuids")
            appendLine("  Average Rate: ${formatPercentage(avgRate)}")
            appendLine("  Min Rate: ${formatPercentage(minRate)}")
            appendLine("  Max Rate: ${formatPercentage(maxRate)}")
            appendLine()

            // Per-app breakdown
            appendLine("Per-App Breakdown:")
            appendLine("-".repeat(50))

            metricsList.forEach { metrics ->
                appendLine()
                appendLine("App: ${metrics.packageName}")
                appendLine("  Time: ${dateFormat.format(Date(metrics.explorationTimestamp))}")
                appendLine("  Detected: ${metrics.elementsDetected}")
                appendLine("  Created: ${metrics.vuidsCreated}")
                appendLine("  Rate: ${formatPercentage(metrics.creationRate)} ${getStatusIcon(metrics.creationRate)}")
                appendLine("  Filtered: ${metrics.filteredCount}")
            }
        }
    }

    /**
     * Generate CSV report
     */
    private fun generateCsvReport(metricsList: List<VUIDCreationMetrics>): String {
        return buildString {
            // Header
            appendLine("Package Name,Exploration Time,Elements Detected,VUIDs Created,Creation Rate,Filtered Count")

            // Data rows
            metricsList.forEach { metrics ->
                appendLine("${metrics.packageName},${dateFormat.format(Date(metrics.explorationTimestamp))},${metrics.elementsDetected},${metrics.vuidsCreated},${metrics.creationRate},${metrics.filteredCount}")
            }
        }
    }

    /**
     * Generate JSON report for single metrics
     */
    private fun generateJsonReport(metrics: VUIDCreationMetrics): String {
        val json = JSONObject().apply {
            put("packageName", metrics.packageName)
            put("explorationTimestamp", metrics.explorationTimestamp)
            put("explorationTime", dateFormat.format(Date(metrics.explorationTimestamp)))
            put("elementsDetected", metrics.elementsDetected)
            put("vuidsCreated", metrics.vuidsCreated)
            put("creationRate", metrics.creationRate)
            put("filteredCount", metrics.filteredCount)

            // Filtered by type
            put("filteredByType", JSONObject().apply {
                metrics.filteredByType.forEach { (type, count) ->
                    put(type, count)
                }
            })

            // Filter reasons
            put("filterReasons", JSONObject().apply {
                metrics.filterReasons.forEach { (reason, count) ->
                    put(reason, count)
                }
            })
        }

        return json.toString(2) // Pretty print with 2-space indent
    }

    /**
     * Generate aggregate JSON report
     */
    private fun generateAggregateJsonReport(metricsList: List<VUIDCreationMetrics>): String {
        val json = JSONObject().apply {
            put("generatedAt", System.currentTimeMillis())
            put("generatedTime", dateFormat.format(Date()))
            put("totalExplorations", metricsList.size)

            // Aggregate stats
            if (metricsList.isNotEmpty()) {
                val totalElements = metricsList.sumOf { it.elementsDetected }
                val totalVuids = metricsList.sumOf { it.vuidsCreated }
                val avgRate = metricsList.map { it.creationRate }.average()

                put("aggregateStats", JSONObject().apply {
                    put("totalElements", totalElements)
                    put("totalVuids", totalVuids)
                    put("averageRate", avgRate)
                    put("minRate", metricsList.minOf { it.creationRate })
                    put("maxRate", metricsList.maxOf { it.creationRate })
                })
            }

            // Individual metrics
            put("explorations", JSONArray().apply {
                metricsList.forEach { metrics ->
                    put(JSONObject().apply {
                        put("packageName", metrics.packageName)
                        put("explorationTimestamp", metrics.explorationTimestamp)
                        put("elementsDetected", metrics.elementsDetected)
                        put("vuidsCreated", metrics.vuidsCreated)
                        put("creationRate", metrics.creationRate)
                        put("filteredCount", metrics.filteredCount)
                    })
                }
            })
        }

        return json.toString(2)
    }

    /**
     * Generate filename for report
     */
    private fun generateFilename(metrics: VUIDCreationMetrics, extension: String): String {
        val packageShort = metrics.packageName.substringAfterLast('.')
        val timestamp = filenameDateFormat.format(Date(metrics.explorationTimestamp))
        return "vuid-metrics-$packageShort-$timestamp.$extension"
    }

    /**
     * Get file in app's external storage
     */
    private fun getReportFile(filename: String): File {
        val reportsDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "vuid-reports"
        )
        reportsDir.mkdirs()
        return File(reportsDir, filename)
    }

    /**
     * Format percentage for display
     */
    private fun formatPercentage(rate: Double): String {
        return "${(rate * 100).toInt()}%"
    }

    /**
     * Get status icon based on creation rate
     */
    private fun getStatusIcon(rate: Double): String = when {
        rate >= 0.95 -> "✅" // Excellent (95%+)
        rate >= 0.80 -> "⚠️"  // Warning (80-95%)
        else -> "❌"         // Error (<80%)
    }

    companion object {
        private const val TAG = "VUIDMetricsReportGenerator"
    }
}
