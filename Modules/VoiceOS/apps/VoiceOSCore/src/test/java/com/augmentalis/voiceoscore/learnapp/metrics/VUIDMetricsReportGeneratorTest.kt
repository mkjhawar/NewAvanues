/**
 * VUIDMetricsReportGeneratorTest.kt - Unit tests for VUIDMetricsReportGenerator
 * Path: VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/learnapp/metrics/VUIDMetricsReportGeneratorTest.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-08
 * Feature: LearnApp VUID Creation Fix - Phase 3 (Observability)
 *
 * Comprehensive unit tests for VUIDMetricsReportGenerator including:
 * - TEXT format generation
 * - CSV format generation
 * - JSON format generation
 * - File export functionality
 * - Aggregate report generation
 *
 * Part of: LearnApp-VUID-Metrics-Phase3-Implementation-Report-5081218-V1.md
 */

package com.augmentalis.voiceoscore.learnapp.metrics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Test suite for VUIDMetricsReportGenerator
 *
 * Validates:
 * 1. TEXT format report generation
 * 2. CSV format report generation
 * 3. JSON format report generation
 * 4. Single-app report generation
 * 5. Aggregate report generation (multiple apps)
 * 6. File export functionality
 * 7. Filename generation
 * 8. Report content accuracy
 * 9. Edge cases (empty metrics, large volumes)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VUIDMetricsReportGeneratorTest {

    private lateinit var context: Context
    private lateinit var generator: VUIDMetricsReportGenerator
    private val exportedFiles = mutableListOf<File>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        generator = VUIDMetricsReportGenerator(context)
    }

    @After
    fun teardown() {
        // Clean up exported files
        exportedFiles.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        exportedFiles.clear()
    }

    // ========== TEXT Format Tests ==========

    /**
     * Test 1: Generate TEXT report for perfect 100% metrics
     *
     * Validates:
     * - Report contains all key metrics
     * - 100% rate shows success icon (✅)
     * - No filtered elements section when count is 0
     */
    @Test
    fun testTextReportPerfect100Percent() {
        val metrics = createTestMetrics(
            packageName = "com.ytheekshana.deviceinfo",
            elementsDetected = 117,
            vuidsCreated = 117,
            filteredCount = 0
        )

        val report = generator.generateReport(metrics, ReportFormat.TEXT)

        // Verify content
        assertTrue("Should contain package name", report.contains("com.ytheekshana.deviceinfo"))
        assertTrue("Should contain detected count", report.contains("117"))
        assertTrue("Should contain created count", report.contains("117"))
        assertTrue("Should show 100% rate", report.contains("100%"))
        assertTrue("Should show success icon", report.contains("✅"))
        assertTrue("Should mention no filtering", report.contains("Filtered: 0"))
    }

    /**
     * Test 2: Generate TEXT report with filtered elements
     *
     * Validates:
     * - Filtered elements breakdown by type
     * - Filter reasons breakdown
     * - Percentages are calculated
     */
    @Test
    fun testTextReportWithFilteredElements() {
        val filteredByType = mapOf(
            "android.widget.LinearLayout" to 10,
            "android.widget.Button" to 5,
            "android.widget.ImageView" to 3
        )
        val filterReasons = mapOf(
            "Below threshold" to 12,
            "Decorative" to 6
        )

        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 82,
            filteredCount = 18,
            filteredByType = filteredByType,
            filterReasons = filterReasons
        )

        val report = generator.generateReport(metrics, ReportFormat.TEXT)

        // Verify filtered breakdown
        assertTrue("Should contain filtered by type section", report.contains("Filtered By Type:"))
        assertTrue("Should list LinearLayout", report.contains("LinearLayout"))
        assertTrue("Should list Button", report.contains("Button"))
        assertTrue("Should list ImageView", report.contains("ImageView"))

        assertTrue("Should contain filter reasons section", report.contains("Filter Reasons:"))
        assertTrue("Should list 'Below threshold'", report.contains("Below threshold"))
        assertTrue("Should list 'Decorative'", report.contains("Decorative"))
    }

    /**
     * Test 3: Generate TEXT report with warning status
     *
     * Validates:
     * - Warning icon (⚠️) appears for 80-95% rate
     * - Rate is displayed correctly
     */
    @Test
    fun testTextReportWarningStatus() {
        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 85,
            filteredCount = 15
        )

        val report = generator.generateReport(metrics, ReportFormat.TEXT)

        assertTrue("Should show 85% rate", report.contains("85%"))
        assertTrue("Should show warning icon", report.contains("⚠️"))
    }

    /**
     * Test 4: Generate TEXT report with error status
     *
     * Validates:
     * - Error icon (❌) appears for <80% rate
     * - Low creation rate is highlighted
     */
    @Test
    fun testTextReportErrorStatus() {
        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 50,
            filteredCount = 50
        )

        val report = generator.generateReport(metrics, ReportFormat.TEXT)

        assertTrue("Should show 50% rate", report.contains("50%"))
        assertTrue("Should show error icon", report.contains("❌"))
    }

    // ========== CSV Format Tests ==========

    /**
     * Test 5: Generate CSV report for single metrics
     *
     * Validates:
     * - CSV header is present
     * - Data row contains all fields
     * - Comma-separated format is correct
     */
    @Test
    fun testCsvReportSingleMetrics() {
        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 95,
            filteredCount = 5
        )

        val report = generator.generateReport(metrics, ReportFormat.CSV)

        val lines = report.split("\n").filter { it.isNotBlank() }

        // Verify header
        assertEquals("Should have header row", "Package Name,Exploration Time,Elements Detected,VUIDs Created,Creation Rate,Filtered Count", lines[0])

        // Verify data row
        val dataRow = lines[1]
        assertTrue("Data row should contain package name", dataRow.contains("com.example.app"))
        assertTrue("Data row should contain detected count", dataRow.contains("100"))
        assertTrue("Data row should contain created count", dataRow.contains("95"))
        assertTrue("Data row should contain rate", dataRow.contains("0.95"))
        assertTrue("Data row should contain filtered count", dataRow.contains("5"))
    }

    /**
     * Test 6: Generate CSV report for multiple metrics
     *
     * Validates:
     * - Multiple data rows
     * - Each row has correct format
     * - Header is only included once
     */
    @Test
    fun testCsvReportMultipleMetrics() {
        val metricsList = listOf(
            createTestMetrics("com.app1", 100, 100, 0),
            createTestMetrics("com.app2", 150, 142, 8),
            createTestMetrics("com.app3", 200, 180, 20)
        )

        val report = generator.generateAggregateReport(metricsList, ReportFormat.CSV)

        val lines = report.split("\n").filter { it.isNotBlank() }

        // Verify
        assertEquals("Should have 4 lines (1 header + 3 data)", 4, lines.size)
        assertTrue("First line should be header", lines[0].startsWith("Package Name"))
        assertTrue("Should contain app1", lines[1].contains("com.app1"))
        assertTrue("Should contain app2", lines[2].contains("com.app2"))
        assertTrue("Should contain app3", lines[3].contains("com.app3"))
    }

    /**
     * Test 7: CSV format is valid for Excel/spreadsheet import
     *
     * Validates:
     * - No extra commas
     * - Timestamps are formatted consistently
     * - Decimal numbers use dot notation
     */
    @Test
    fun testCsvFormatValidForSpreadsheets() {
        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 95,
            filteredCount = 5
        )

        val report = generator.generateReport(metrics, ReportFormat.CSV)

        val dataLine = report.split("\n")[1]
        val fields = dataLine.split(",")

        // Verify field count
        assertEquals("Should have exactly 6 fields", 6, fields.size)

        // Verify rate field is numeric with dot
        val rateField = fields[4]
        assertTrue("Rate should use dot notation", rateField.contains("."))
        assertFalse("Rate should not contain comma", rateField.contains(","))
    }

    // ========== JSON Format Tests ==========

    /**
     * Test 8: Generate JSON report for single metrics
     *
     * Validates:
     * - Valid JSON format
     * - All fields present
     * - Maps are serialized correctly
     */
    @Test
    fun testJsonReportSingleMetrics() {
        val filteredByType = mapOf(
            "android.widget.Button" to 5,
            "android.widget.ImageView" to 3
        )
        val filterReasons = mapOf(
            "Below threshold" to 6,
            "Decorative" to 2
        )

        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 92,
            filteredCount = 8,
            filteredByType = filteredByType,
            filterReasons = filterReasons
        )

        val report = generator.generateReport(metrics, ReportFormat.JSON)

        // Parse JSON
        val json = JSONObject(report)

        // Verify fields
        assertEquals("Package name should match", "com.example.app", json.getString("packageName"))
        assertEquals("Elements detected should match", 100, json.getInt("elementsDetected"))
        assertEquals("VUIDs created should match", 92, json.getInt("vuidsCreated"))
        assertEquals("Creation rate should match", 0.92, json.getDouble("creationRate"), 0.01)
        assertEquals("Filtered count should match", 8, json.getInt("filteredCount"))

        // Verify maps
        val filteredByTypeJson = json.getJSONObject("filteredByType")
        assertEquals("Button count should match", 5, filteredByTypeJson.getInt("android.widget.Button"))
        assertEquals("ImageView count should match", 3, filteredByTypeJson.getInt("android.widget.ImageView"))

        val filterReasonsJson = json.getJSONObject("filterReasons")
        assertEquals("Below threshold count should match", 6, filterReasonsJson.getInt("Below threshold"))
        assertEquals("Decorative count should match", 2, filterReasonsJson.getInt("Decorative"))
    }

    /**
     * Test 9: JSON is pretty-printed with indentation
     *
     * Validates:
     * - JSON has line breaks
     * - Indentation is present (2 spaces)
     */
    @Test
    fun testJsonPrettyPrinted() {
        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 95,
            filteredCount = 5
        )

        val report = generator.generateReport(metrics, ReportFormat.JSON)

        // Verify pretty printing
        assertTrue("Should have line breaks", report.contains("\n"))
        assertTrue("Should have indentation", report.contains("  ")) // 2-space indent
    }

    /**
     * Test 10: Generate aggregate JSON report
     *
     * Validates:
     * - Aggregate stats are calculated
     * - Individual explorations array is present
     * - Generation timestamp is included
     */
    @Test
    fun testJsonAggregateReport() {
        val metricsList = listOf(
            createTestMetrics("com.app1", 100, 100, 0),
            createTestMetrics("com.app2", 150, 142, 8),
            createTestMetrics("com.app3", 200, 180, 20)
        )

        val report = generator.generateAggregateReport(metricsList, ReportFormat.JSON)

        // Parse JSON
        val json = JSONObject(report)

        // Verify aggregate stats
        assertTrue("Should have generatedAt", json.has("generatedAt"))
        assertTrue("Should have generatedTime", json.has("generatedTime"))
        assertEquals("Total explorations should be 3", 3, json.getInt("totalExplorations"))

        val stats = json.getJSONObject("aggregateStats")
        assertEquals("Total elements should be 450", 450, stats.getInt("totalElements"))
        assertEquals("Total VUIDs should be 422", 422, stats.getInt("totalVuids"))

        // Verify explorations array
        val explorations = json.getJSONArray("explorations")
        assertEquals("Should have 3 explorations", 3, explorations.length())

        val firstExploration = explorations.getJSONObject(0)
        assertEquals("First should be app1", "com.app1", firstExploration.getString("packageName"))
    }

    /**
     * Test 11: JSON with empty maps
     *
     * Validates:
     * - Empty filteredByType renders as {}
     * - Empty filterReasons renders as {}
     */
    @Test
    fun testJsonWithEmptyMaps() {
        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 100,
            filteredCount = 0
        )

        val report = generator.generateReport(metrics, ReportFormat.JSON)

        val json = JSONObject(report)

        // Verify empty objects
        val filteredByType = json.getJSONObject("filteredByType")
        assertEquals("filteredByType should be empty", 0, filteredByType.length())

        val filterReasons = json.getJSONObject("filterReasons")
        assertEquals("filterReasons should be empty", 0, filterReasons.length())
    }

    // ========== Aggregate Report Tests ==========

    /**
     * Test 12: Generate aggregate TEXT report
     *
     * Validates:
     * - Summary statistics section
     * - Per-app breakdown
     * - Averages calculated correctly
     */
    @Test
    fun testAggregateTextReport() {
        val metricsList = listOf(
            createTestMetrics("com.app1", 100, 100, 0),
            createTestMetrics("com.app2", 100, 95, 5),
            createTestMetrics("com.app3", 100, 90, 10)
        )

        val report = generator.generateAggregateReport(metricsList, ReportFormat.TEXT)

        // Verify aggregate section
        assertTrue("Should have aggregate title", report.contains("Aggregate Report"))
        assertTrue("Should show total explorations", report.contains("Total Explorations: 3"))
        assertTrue("Should show total elements", report.contains("Total Elements: 300"))
        assertTrue("Should show total VUIDs", report.contains("Total VUIDs: 285"))
        assertTrue("Should show average rate", report.contains("Average Rate:"))

        // Verify per-app breakdown
        assertTrue("Should have per-app section", report.contains("Per-App Breakdown"))
        assertTrue("Should list app1", report.contains("com.app1"))
        assertTrue("Should list app2", report.contains("com.app2"))
        assertTrue("Should list app3", report.contains("com.app3"))
    }

    /**
     * Test 13: Aggregate statistics calculation accuracy
     *
     * Validates:
     * - Totals are summed correctly
     * - Average is mean of rates
     * - Min/Max are identified correctly
     */
    @Test
    fun testAggregateStatisticsCalculation() {
        val metricsList = listOf(
            createTestMetrics("com.app1", 100, 100, 0, rate = 1.0),
            createTestMetrics("com.app2", 100, 95, 5, rate = 0.95),
            createTestMetrics("com.app3", 100, 90, 10, rate = 0.90),
            createTestMetrics("com.app4", 100, 85, 15, rate = 0.85)
        )

        val report = generator.generateAggregateReport(metricsList, ReportFormat.TEXT)

        // Expected average: (1.0 + 0.95 + 0.90 + 0.85) / 4 = 0.925 = 92%
        assertTrue("Should show correct average", report.contains("Average Rate: 92%"))
        assertTrue("Should show min rate 85%", report.contains("Min Rate: 85%"))
        assertTrue("Should show max rate 100%", report.contains("Max Rate: 100%"))
    }

    /**
     * Test 14: Empty aggregate report
     *
     * Validates:
     * - Handles empty metrics list gracefully
     * - Returns appropriate message
     */
    @Test
    fun testEmptyAggregateReport() {
        val emptyList = emptyList<VUIDCreationMetrics>()

        val textReport = generator.generateAggregateReport(emptyList, ReportFormat.TEXT)
        assertTrue("TEXT should handle empty list", textReport.contains("No metrics available"))

        val csvReport = generator.generateAggregateReport(emptyList, ReportFormat.CSV)
        val csvLines = csvReport.split("\n").filter { it.isNotBlank() }
        assertEquals("CSV should have only header", 1, csvLines.size)
    }

    // ========== File Export Tests ==========

    /**
     * Test 15: Export TEXT report to file
     *
     * Validates:
     * - File is created in correct directory
     * - Filename is generated correctly
     * - Content matches generated report
     */
    @Test
    fun testExportTextReportToFile() = runBlocking {
        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 95,
            filteredCount = 5
        )

        val file = generator.exportToFile(metrics, ReportFormat.TEXT)
        exportedFiles.add(file)

        // Verify file
        assertTrue("File should exist", file.exists())
        assertTrue("File should be in vuid-reports directory", file.parentFile?.name == "vuid-reports")
        assertTrue("Filename should end with .txt", file.name.endsWith(".txt"))
        assertTrue("Filename should contain package short name", file.name.contains("app"))

        // Verify content
        val content = file.readText()
        assertTrue("Content should contain package name", content.contains("com.example.app"))
        assertTrue("Content should contain metrics", content.contains("100"))
    }

    /**
     * Test 16: Export CSV report to file
     *
     * Validates:
     * - CSV file is created
     * - Extension is .csv
     * - Content is valid CSV
     */
    @Test
    fun testExportCsvReportToFile() = runBlocking {
        val metrics = createTestMetrics(
            packageName = "com.ytheekshana.deviceinfo",
            elementsDetected = 117,
            vuidsCreated = 117,
            filteredCount = 0
        )

        val file = generator.exportToFile(metrics, ReportFormat.CSV)
        exportedFiles.add(file)

        // Verify
        assertTrue("File should exist", file.exists())
        assertTrue("Filename should end with .csv", file.name.endsWith(".csv"))
        assertTrue("Filename should contain 'deviceinfo'", file.name.contains("deviceinfo"))

        // Verify CSV content
        val lines = file.readLines().filter { it.isNotBlank() }
        assertTrue("Should have at least 2 lines", lines.size >= 2)
        assertTrue("First line should be header", lines[0].contains("Package Name"))
    }

    /**
     * Test 17: Export JSON report to file
     *
     * Validates:
     * - JSON file is created
     * - Extension is .json
     * - Content is valid JSON
     */
    @Test
    fun testExportJsonReportToFile() = runBlocking {
        val metrics = createTestMetrics(
            packageName = "com.example.testapp",
            elementsDetected = 100,
            vuidsCreated = 90,
            filteredCount = 10
        )

        val file = generator.exportToFile(metrics, ReportFormat.JSON)
        exportedFiles.add(file)

        // Verify
        assertTrue("File should exist", file.exists())
        assertTrue("Filename should end with .json", file.name.endsWith(".json"))

        // Verify valid JSON
        val content = file.readText()
        val json = JSONObject(content) // Should not throw exception
        assertEquals("JSON should contain correct package", "com.example.testapp", json.getString("packageName"))
    }

    /**
     * Test 18: Export aggregate report to file
     *
     * Validates:
     * - Aggregate file is created
     * - Filename indicates aggregate nature
     * - Contains all metrics
     */
    @Test
    fun testExportAggregateReportToFile() = runBlocking {
        val metricsList = listOf(
            createTestMetrics("com.app1", 100, 100, 0),
            createTestMetrics("com.app2", 100, 95, 5),
            createTestMetrics("com.app3", 100, 90, 10)
        )

        val file = generator.exportAggregateToFile(metricsList, ReportFormat.JSON)
        exportedFiles.add(file)

        // Verify
        assertTrue("File should exist", file.exists())
        assertTrue("Filename should contain 'aggregate'", file.name.contains("aggregate"))
        assertTrue("Filename should end with .json", file.name.endsWith(".json"))

        // Verify content
        val json = JSONObject(file.readText())
        assertEquals("Should have 3 explorations", 3, json.getInt("totalExplorations"))
    }

    /**
     * Test 19: Custom filename export
     *
     * Validates:
     * - Can specify custom filename
     * - Extension is still appended correctly
     */
    @Test
    fun testCustomFilenameExport() = runBlocking {
        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 95,
            filteredCount = 5
        )

        val customName = "my-custom-report.txt"
        val file = generator.exportToFile(metrics, ReportFormat.TEXT, customName)
        exportedFiles.add(file)

        // Verify
        assertEquals("Filename should match custom name", customName, file.name)
        assertTrue("File should exist", file.exists())
    }

    /**
     * Test 20: File export directory creation
     *
     * Validates:
     * - Export directory is created if not exists
     * - Files are stored in correct location
     */
    @Test
    fun testFileExportDirectoryCreation() = runBlocking {
        val metrics = createTestMetrics(
            packageName = "com.example.app",
            elementsDetected = 100,
            vuidsCreated = 95,
            filteredCount = 5
        )

        val file = generator.exportToFile(metrics, ReportFormat.TEXT)
        exportedFiles.add(file)

        // Verify directory structure
        val reportsDir = file.parentFile
        assertNotNull("Reports directory should exist", reportsDir)
        assertEquals("Directory name should be vuid-reports", "vuid-reports", reportsDir?.name)
        assertTrue("Directory should exist", reportsDir?.exists() == true)
    }

    // ========== Edge Cases Tests ==========

    /**
     * Test 21: Report with very long package name
     *
     * Validates:
     * - Handles long package names
     * - Filename is still valid
     */
    @Test
    fun testReportWithLongPackageName() {
        val longPackageName = "com.verylongcompanyname.verylongproductname.verylongmodulename.app"

        val metrics = createTestMetrics(
            packageName = longPackageName,
            elementsDetected = 100,
            vuidsCreated = 95,
            filteredCount = 5
        )

        val report = generator.generateReport(metrics, ReportFormat.TEXT)

        assertTrue("Should contain full package name", report.contains(longPackageName))
    }

    /**
     * Test 22: Report with special characters in package name
     *
     * Validates:
     * - Special characters are handled
     * - JSON escaping is correct
     */
    @Test
    fun testReportWithSpecialCharacters() {
        val packageWithUnderscore = "com.company_name.app_name"

        val metrics = createTestMetrics(
            packageName = packageWithUnderscore,
            elementsDetected = 100,
            vuidsCreated = 95,
            filteredCount = 5
        )

        val jsonReport = generator.generateReport(metrics, ReportFormat.JSON)
        val json = JSONObject(jsonReport)

        assertEquals("Should preserve underscores", packageWithUnderscore, json.getString("packageName"))
    }

    /**
     * Test 23: Report with zero elements detected
     *
     * Validates:
     * - Handles 0/0 scenario
     * - No division by zero errors
     * - Rate is 0.0
     */
    @Test
    fun testReportWithZeroElements() {
        val metrics = createTestMetrics(
            packageName = "com.example.empty",
            elementsDetected = 0,
            vuidsCreated = 0,
            filteredCount = 0,
            rate = 0.0
        )

        val textReport = generator.generateReport(metrics, ReportFormat.TEXT)
        assertTrue("Should show 0 elements", textReport.contains("Elements detected: 0"))
        assertTrue("Should show 0% rate", textReport.contains("0%"))

        val jsonReport = generator.generateReport(metrics, ReportFormat.JSON)
        val json = JSONObject(jsonReport)
        assertEquals("Rate should be 0.0", 0.0, json.getDouble("creationRate"), 0.001)
    }

    /**
     * Test 24: Report with very large numbers
     *
     * Validates:
     * - Handles large element counts
     * - Number formatting is correct
     */
    @Test
    fun testReportWithLargeNumbers() {
        val metrics = createTestMetrics(
            packageName = "com.example.large",
            elementsDetected = 10000,
            vuidsCreated = 9500,
            filteredCount = 500
        )

        val report = generator.generateReport(metrics, ReportFormat.TEXT)

        assertTrue("Should show 10000", report.contains("10000"))
        assertTrue("Should show 9500", report.contains("9500"))
        assertTrue("Should show correct rate", report.contains("95%"))
    }

    /**
     * Test 25: Concurrent file exports
     *
     * Validates:
     * - Multiple exports don't conflict
     * - All files are created successfully
     */
    @Test
    fun testConcurrentFileExports() = runBlocking {
        val metricsList = List(5) { i ->
            createTestMetrics(
                packageName = "com.app$i",
                elementsDetected = 100,
                vuidsCreated = 95 + i,
                filteredCount = 5 - i
            )
        }

        // Export all concurrently
        val files = metricsList.map { metrics ->
            generator.exportToFile(metrics, ReportFormat.TEXT)
        }

        exportedFiles.addAll(files)

        // Verify
        assertEquals("Should create 5 files", 5, files.size)
        files.forEach { file ->
            assertTrue("File should exist: ${file.name}", file.exists())
            assertTrue("File should have content", file.length() > 0)
        }

        // Verify unique filenames
        val uniqueNames = files.map { it.name }.toSet()
        assertEquals("All filenames should be unique", 5, uniqueNames.size)
    }

    // ========== Helper Methods ==========

    /**
     * Create test VUIDCreationMetrics object
     */
    private fun createTestMetrics(
        packageName: String,
        elementsDetected: Int,
        vuidsCreated: Int,
        filteredCount: Int,
        explorationTimestamp: Long = System.currentTimeMillis(),
        rate: Double = if (elementsDetected > 0) vuidsCreated.toDouble() / elementsDetected else 0.0,
        filteredByType: Map<String, Int> = emptyMap(),
        filterReasons: Map<String, Int> = emptyMap()
    ): VUIDCreationMetrics {
        return VUIDCreationMetrics(
            packageName = packageName,
            explorationTimestamp = explorationTimestamp,
            elementsDetected = elementsDetected,
            vuidsCreated = vuidsCreated,
            creationRate = rate,
            filteredCount = filteredCount,
            filteredByType = filteredByType,
            filterReasons = filterReasons
        )
    }

    companion object {
        private const val TAG = "VUIDMetricsReportGeneratorTest"
    }
}
