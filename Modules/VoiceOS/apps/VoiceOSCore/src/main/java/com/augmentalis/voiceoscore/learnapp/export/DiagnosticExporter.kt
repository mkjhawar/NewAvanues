/**
 * DiagnosticExporter.kt - Export diagnostic reports to JSON/CSV/TXT
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/export/DiagnosticExporter.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (Swarm Agent 6)
 * Created: 2025-12-08
 *
 * Exports comprehensive diagnostic reports in multiple formats for analysis and debugging.
 */

package com.augmentalis.voiceoscore.learnapp.export

import android.content.Context
import android.os.Environment
import com.augmentalis.voiceoscore.learnapp.models.*
import com.augmentalis.voiceoscore.learnapp.tracking.ElementDiagnosticTracker
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Diagnostic Exporter
 *
 * Exports diagnostic data in multiple formats:
 * - JSON: For programmatic analysis
 * - CSV: For spreadsheet analysis
 * - TXT: For human-readable reports
 *
 * ## Usage
 *
 * ```kotlin
 * val exporter = DiagnosticExporter(context, diagnosticTracker)
 *
 * // Export to JSON
 * val jsonFile = exporter.exportToJson(sessionId, appId, startTime, endTime)
 * Log.i(TAG, "Exported to: ${jsonFile.absolutePath}")
 *
 * // Export to TXT (human-readable)
 * val txtFile = exporter.exportToTxt(sessionId, appId, startTime, endTime)
 * ```
 */
class DiagnosticExporter(
    private val context: Context,
    private val tracker: ElementDiagnosticTracker
) {

    companion object {
        private const val TAG = "DiagnosticExporter"
        private const val EXPORT_DIR = "VoiceOS/diagnostics"
    }

    /**
     * Export to JSON format
     *
     * @param sessionId Session ID
     * @param appId App package name
     * @param startedAt Session start time
     * @param completedAt Session end time
     * @return Exported file
     */
    fun exportToJson(
        sessionId: String,
        appId: String,
        startedAt: Long,
        completedAt: Long?
    ): File {
        val report = tracker.getSessionReport(sessionId, startedAt, completedAt)
        val json = buildJsonReport(report, appId)

        val filename = generateFilename(appId, "json")
        val file = File(getExportDir(), filename)
        file.writeText(json.toString(2))  // Pretty print with indent=2

        return file
    }

    /**
     * Export to CSV format
     *
     * @param sessionId Session ID
     * @param appId App package name
     * @param startedAt Session start time
     * @param completedAt Session end time
     * @return Exported file
     */
    fun exportToCsv(
        sessionId: String,
        appId: String,
        startedAt: Long,
        completedAt: Long?
    ): File {
        val report = tracker.getSessionReport(sessionId, startedAt, completedAt)
        val csv = buildCsvReport(report)

        val filename = generateFilename(appId, "csv")
        val file = File(getExportDir(), filename)
        file.writeText(csv)

        return file
    }

    /**
     * Export to TXT format (human-readable)
     *
     * @param sessionId Session ID
     * @param appId App package name
     * @param startedAt Session start time
     * @param completedAt Session end time
     * @return Exported file
     */
    fun exportToTxt(
        sessionId: String,
        appId: String,
        startedAt: Long,
        completedAt: Long?
    ): File {
        val report = tracker.getSessionReport(sessionId, startedAt, completedAt)
        val txt = buildTxtReport(report, appId)

        val filename = generateFilename(appId, "txt")
        val file = File(getExportDir(), filename)
        file.writeText(txt)

        return file
    }

    // ═══════════════════════════════════════════════════════════════
    // JSON BUILDER
    // ═══════════════════════════════════════════════════════════════

    private fun buildJsonReport(report: SessionDiagnosticReport, appId: String): JSONObject {
        return JSONObject().apply {
            put("session_id", report.sessionId)
            put("app", appId)
            put("timestamp", formatTimestamp(report.startedAt))
            put("started_at", report.startedAt)
            put("completed_at", report.completedAt)
            put("duration_ms", report.completedAt?.let { it - report.startedAt })

            // Summary
            put("summary", JSONObject().apply {
                put("total", report.totalElements)
                put("clicked", report.clickedCount)
                put("blocked", report.blockedCount)
                put("skipped", report.skippedCount)
                put("pending", report.pendingCount)
                put("completion_pct", String.format("%.1f", report.getCompletionPercent()))
            })

            // Reason breakdown
            put("reason_breakdown", JSONObject().apply {
                report.reasonCounts.forEach { (reason, count) ->
                    put(reason.name, count)
                }
            })

            // Dangerous category breakdown
            put("dangerous_breakdown", JSONObject().apply {
                report.dangerousCategoryCounts.forEach { (category, count) ->
                    put(category.name, count)
                }
            })

            // All elements
            put("elements", JSONArray().apply {
                report.diagnostics.forEach { diagnostic ->
                    put(JSONObject().apply {
                        put("uuid", diagnostic.elementUuid)
                        put("text", diagnostic.elementText)
                        put("content_desc", diagnostic.elementContentDesc)
                        put("resource_id", diagnostic.elementResourceId)
                        put("status", diagnostic.status.name)
                        put("reason", diagnostic.reason.name)
                        put("reason_display", diagnostic.reason.displayName)
                        put("reason_detail", diagnostic.reasonDetail)
                        put("dangerous_pattern", diagnostic.dangerousPattern)
                        put("dangerous_category", diagnostic.dangerousCategory?.name)
                        put("screen_hash", diagnostic.screenHash)
                        put("discovered_at", diagnostic.discoveredAt)
                        put("decision_made_at", diagnostic.decisionMadeAt)
                    })
                }
            })
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CSV BUILDER
    // ═══════════════════════════════════════════════════════════════

    private fun buildCsvReport(report: SessionDiagnosticReport): String {
        val sb = StringBuilder()

        // Header
        sb.appendLine("UUID,Text,Status,Reason,Category,Pattern,Screen,Timestamp")

        // Rows
        report.diagnostics.forEach { diagnostic ->
            sb.append(csvEscape(diagnostic.elementUuid)).append(",")
            sb.append(csvEscape(diagnostic.elementText)).append(",")
            sb.append(diagnostic.status.name).append(",")
            sb.append(diagnostic.reason.name).append(",")
            sb.append(diagnostic.dangerousCategory?.name ?: "").append(",")
            sb.append(csvEscape(diagnostic.dangerousPattern ?: "")).append(",")
            sb.append(diagnostic.screenHash.take(8)).append(",")
            sb.append(diagnostic.decisionMadeAt).appendLine()
        }

        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TXT BUILDER (Human-Readable)
    // ═══════════════════════════════════════════════════════════════

    private fun buildTxtReport(report: SessionDiagnosticReport, appId: String): String {
        val sb = StringBuilder()

        // Header
        sb.appendLine("═".repeat(60))
        sb.appendLine("DIAGNOSTIC REPORT")
        sb.appendLine("═".repeat(60))
        sb.appendLine("App: $appId")
        sb.appendLine("Session: ${report.sessionId}")
        sb.appendLine("Date: ${formatTimestamp(report.startedAt)}")
        sb.appendLine()

        // Summary
        sb.appendLine("SUMMARY")
        sb.appendLine("─".repeat(60))
        sb.appendLine("Total Elements: ${report.totalElements}")
        sb.appendLine("✅ Clicked: ${report.clickedCount} (${String.format("%.1f", report.getCompletionPercent())}%)")
        sb.appendLine("🚫 Blocked: ${report.blockedCount}")
        sb.appendLine("⏭️ Skipped: ${report.skippedCount}")
        sb.appendLine("⏳ Pending: ${report.pendingCount}")
        sb.appendLine()

        // Blocked elements
        if (report.blockedCount > 0) {
            sb.appendLine("BLOCKED ELEMENTS (${report.blockedCount})")
            sb.appendLine("─".repeat(60))
            report.diagnostics.filter { it.status == ElementStatus.BLOCKED }
                .forEachIndexed { index, diagnostic ->
                    sb.appendLine("${index + 1}. \"${diagnostic.elementText}\"")
                    sb.appendLine("   UUID: ${diagnostic.elementUuid}")
                    sb.appendLine("   Reason: ${diagnostic.reason.displayName}")
                    sb.appendLine("   Category: ${diagnostic.dangerousCategory}")
                    diagnostic.dangerousPattern?.let {
                        sb.appendLine("   Pattern: \"$it\"")
                    }
                    sb.appendLine("   Explanation: ${diagnostic.reason.description}")
                    if (diagnostic.reasonDetail.isNotBlank()) {
                        sb.appendLine("   Details: ${diagnostic.reasonDetail}")
                    }
                    sb.appendLine()
                }
        }

        // Skipped elements (grouped by reason)
        if (report.skippedCount > 0) {
            sb.appendLine("SKIPPED ELEMENTS BY REASON (${report.skippedCount})")
            sb.appendLine("─".repeat(60))

            val skippedByReason = report.diagnostics
                .filter { it.status == ElementStatus.NOT_CLICKED }
                .groupBy { it.reason }

            skippedByReason.forEach { (reason, diagnostics) ->
                sb.appendLine("${reason.getIcon()} ${reason.displayName}: ${diagnostics.size} elements")
                sb.appendLine("   ${reason.description}")
                sb.appendLine()
            }
        }

        // Recommendations
        sb.appendLine("RECOMMENDATIONS")
        sb.appendLine("─".repeat(60))
        when {
            report.getCompletionPercent() >= 90 ->
                sb.appendLine("✓ Exploration excellent (${String.format("%.1f", report.getCompletionPercent())}%)")
            report.getCompletionPercent() >= 70 ->
                sb.appendLine("✓ Exploration good (${String.format("%.1f", report.getCompletionPercent())}%)")
            report.getCompletionPercent() >= 50 ->
                sb.appendLine("⚠ Exploration acceptable (${String.format("%.1f", report.getCompletionPercent())}%)")
            else ->
                sb.appendLine("⚠ Exploration incomplete (${String.format("%.1f", report.getCompletionPercent())}%)")
        }

        if (report.blockedCount > 0) {
            sb.appendLine("⚠ ${report.blockedCount} critical elements blocked (expected)")
        }

        if (report.skippedCount > report.totalElements / 2) {
            sb.appendLine("ℹ High skip rate - optimization working correctly")
        }

        sb.appendLine()
        sb.appendLine("═".repeat(60))

        return sb.toString()
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private fun getExportDir(): File {
        val dir = File(Environment.getExternalStorageDirectory(), EXPORT_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun generateFilename(appId: String, extension: String): String {
        val appName = appId.substringAfterLast('.')
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${appName}_diagnostic_${timestamp}.$extension"
    }

    private fun formatTimestamp(epochMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(epochMs))
    }
}
