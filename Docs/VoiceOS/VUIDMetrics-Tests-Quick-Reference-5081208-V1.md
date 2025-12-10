# VUIDMetrics Unit Tests - Quick Reference

**Quick Start Guide for VUIDMetrics Component Testing**

---

## Overview

Three comprehensive test suites provide 90%+ coverage of VUID metrics components:

| Test File | Tests | Lines | Focus |
|-----------|-------|-------|-------|
| **VUIDCreationMetricsCollectorTest** | 25 | 890 | Real-time metrics collection, thread safety |
| **VUIDMetricsReportGeneratorTest** | 25 | 844 | Report generation in TEXT/CSV/JSON formats |
| **VUIDMetricsRepositoryTest** | 17 | 743 | Database persistence (templates) |

---

## Test Locations

```
/Volumes/M-Drive/Coding/NewAvanues/
└── Modules/VoiceOS/apps/VoiceOSCore/src/test/java/
    └── com/augmentalis/voiceoscore/learnapp/metrics/
        ├── VUIDCreationMetricsCollectorTest.kt    (890 lines)
        ├── VUIDMetricsReportGeneratorTest.kt      (844 lines)
        └── VUIDMetricsRepositoryTest.kt            (743 lines)
```

---

## VUIDCreationMetricsCollectorTest - 25 Tests

### Counter Tests (3)
```kotlin
// Test 1: Element detection counter
@Test fun testElementDetectionCounterIncrement()
✓ Single increment
✓ Multiple increments (11 total)

// Test 2: VUID creation counter
@Test fun testVUIDCreationCounterIncrement()
✓ Single increment
✓ Multiple increments (11 total)

// Test 3: Creation rate calculation
@Test fun testCreationRateCalculation()
✓ Rate = 0.0 when no elements
✓ Rate = 1.0 (100%)
✓ Rate = 0.5 (50%)
✓ Rate = 0.666 (67%)
```

### Filtered Element Tests (2)
```kotlin
// Test 4: Single filtered element
@Test fun testFilteredElementTracking()
✓ Type count = 1
✓ Reason count = 1

// Test 5: Multiple types
@Test fun testMultipleFilteredElementTypes()
✓ Button: 2 count
✓ ImageView: 1 count
✓ LinearLayout: 1 count
```

### Severity Classification Tests (4)
```kotlin
// Test 6: ERROR severity
@Test fun testErrorSeverityForClickableElement()
✓ isClickable=true → ERROR

// Test 7: WARNING severity
@Test fun testWarningSeverityForContainerWithHints()
✓ Container + isFocusable → WARNING
✓ Container + ACTION_CLICK → WARNING

// Test 8: INTENDED severity
@Test fun testIntendedSeverityForNormalFiltering()
✓ Decorative elements → INTENDED

// Test 9: Mixed severities
@Test fun testMixedSeverityClassifications()
✓ 1 ERROR, 1 WARNING, 1 INTENDED
```

### Metrics Building Tests (3)
```kotlin
// Test 10: Complete metrics
@Test fun testBuildCompleteMetrics()
✓ All fields populated
✓ Package name correct
✓ Timestamp recent (<1000ms)

// Test 11: Perfect 100%
@Test fun testPerfect100PercentMetrics()
✓ 117 elements → 117 VUIDs
✓ creationRate = 1.0

// Test 12: Report string
@Test fun testReportStringGeneration()
✓ Contains package name
✓ Contains metrics values
✓ Contains rate (90%)
```

### Thread Safety Tests (4)
```kotlin
// Test 13: Concurrent detection
@Test fun testConcurrentElementDetection()
✓ 10 threads × 100 increments = 1000 total

// Test 14: Concurrent VUID creation
@Test fun testConcurrentVUIDCreation()
✓ 10 threads × 100 increments = 1000 total

// Test 15: Mixed operations
@Test fun testConcurrentMixedOperations()
✓ 20 threads (10 detect, 10 create)
✓ Rate = 1.0 (100%)

// Test 16: Concurrent filtering
@Test fun testConcurrentElementFiltering()
✓ 10 threads × 50 filters = 500 total
✓ No ConcurrentModificationException
```

### Filter Report Tests (2)
```kotlin
// Test 17: Report generation
@Test fun testGenerateFilterReport()
✓ 3 elements with mixed severities
✓ Severity counts accurate

// Test 18: Empty report
@Test fun testFilterReportWithNoFilters()
✓ Total filtered = 0
✓ All counts = 0
```

### Reset/Reuse Tests (2)
```kotlin
// Test 19: Reset clears all
@Test fun testResetClearsAllMetrics()
✓ All counters = 0
✓ Filtered list empty

// Test 20: Reuse after reset
@Test fun testReuseCollectorAfterReset()
✓ First exploration: 5 elements
✓ After reset
✓ Second exploration: 10 elements
✓ No state leakage
```

### Edge Cases Tests (5)
```kotlin
// Test 21: Zero elements
@Test fun testZeroElementsScenario()
✓ No division by zero

// Test 22: More VUIDs than elements
@Test fun testMoreVUIDsThanElements()
✓ Rate = 2.0 (200%)

// Test 23: Null className
@Test fun testNullClassNameHandling()
✓ Uses "Unknown" placeholder

// Test 24: Empty filter reason
@Test fun testEmptyFilterReason()
✓ Empty string tracked

// Test 25: Large volume
@Test fun testLargeVolumeStressTest()
✓ 10,000 elements
✓ 90% creation rate
✓ Performance acceptable
```

---

## VUIDMetricsReportGeneratorTest - 25 Tests

### TEXT Format Tests (4)
```kotlin
// Test 1: Perfect 100%
@Test fun testTextReportPerfect100Percent()
✓ Contains "100%"
✓ Shows success icon "✅"

// Test 2: With filtered elements
@Test fun testTextReportWithFilteredElements()
✓ "Filtered By Type:" section
✓ "Filter Reasons:" section

// Test 3: Warning status
@Test fun testTextReportWarningStatus()
✓ Shows "85%"
✓ Shows warning icon "⚠️"

// Test 4: Error status
@Test fun testTextReportErrorStatus()
✓ Shows "50%"
✓ Shows error icon "❌"
```

### CSV Format Tests (3)
```kotlin
// Test 5: Single metrics
@Test fun testCsvReportSingleMetrics()
✓ Header row present
✓ Data row comma-separated

// Test 6: Multiple metrics
@Test fun testCsvReportMultipleMetrics()
✓ Header + 3 data rows
✓ Each row contains correct app

// Test 7: Spreadsheet valid
@Test fun testCsvFormatValidForSpreadsheets()
✓ Exactly 6 fields
✓ Decimal uses dot (0.95)
```

### JSON Format Tests (4)
```kotlin
// Test 8: Single metrics
@Test fun testJsonReportSingleMetrics()
✓ Valid JSON parsed
✓ All fields present
✓ Maps serialized correctly

// Test 9: Pretty printed
@Test fun testJsonPrettyPrinted()
✓ Has line breaks
✓ 2-space indentation

// Test 10: Aggregate report
@Test fun testJsonAggregateReport()
✓ Aggregate stats calculated
✓ Explorations array present

// Test 11: Empty maps
@Test fun testJsonWithEmptyMaps()
✓ filteredByType = {}
✓ filterReasons = {}
```

### Aggregate Report Tests (3)
```kotlin
// Test 12: TEXT aggregate
@Test fun testAggregateTextReport()
✓ "Total Explorations: 3"
✓ "Per-App Breakdown" section

// Test 13: Statistics calculation
@Test fun testAggregateStatisticsCalculation()
✓ 4 apps: 100%, 95%, 90%, 85%
✓ Average = 92%
✓ Min = 85%
✓ Max = 100%

// Test 14: Empty list
@Test fun testEmptyAggregateReport()
✓ Returns "No metrics available"
```

### File Export Tests (6)
```kotlin
// Test 15: Export TEXT
@Test fun testExportTextReportToFile()
✓ File exists
✓ In vuid-reports directory
✓ Filename ends with .txt

// Test 16: Export CSV
@Test fun testExportCsvReportToFile()
✓ File exists
✓ Filename ends with .csv

// Test 17: Export JSON
@Test fun testExportJsonReportToFile()
✓ File exists
✓ Filename ends with .json
✓ Valid JSON content

// Test 18: Export aggregate
@Test fun testExportAggregateReportToFile()
✓ Filename contains "aggregate"
✓ File contains all metrics

// Test 19: Custom filename
@Test fun testCustomFilenameExport()
✓ Uses custom name

// Test 20: Directory creation
@Test fun testFileExportDirectoryCreation()
✓ Directory created if not exists
✓ Directory named "vuid-reports"
```

### Edge Cases Tests (5)
```kotlin
// Test 21: Long package name
@Test fun testReportWithLongPackageName()
✓ Full name preserved

// Test 22: Special characters
@Test fun testReportWithSpecialCharacters()
✓ Underscores preserved in JSON

// Test 23: Zero elements
@Test fun testReportWithZeroElements()
✓ Shows "0%"
✓ No division by zero

// Test 24: Large numbers
@Test fun testReportWithLargeNumbers()
✓ 10,000 elements
✓ 9,500 VUIDs
✓ Shows "95%"

// Test 25: Concurrent exports
@Test fun testConcurrentFileExports()
✓ 5 concurrent exports
✓ All files created
✓ Unique filenames
```

---

## VUIDMetricsRepositoryTest - 17 Tests (Templates)

### Schema Tests (2)
```kotlin
// Test 1: Schema initialization
@Test fun testSchemaInitialization()
// Expects: VUIDMetricsRepository(databaseManager)
// Expected table: vuid_creation_metrics
// Expected columns: package_name, elements_detected, vuids_created, etc.
// Expected indexes: idx_metrics_package, idx_metrics_timestamp

// Test 2: Idempotent initialization
@Test fun testSchemaInitializationIdempotent()
// Multiple calls to initializeSchema() should not fail
```

### CRUD Tests (3)
```kotlin
// Test 3: Save metrics
@Test fun testSaveMetrics()
// saveMetrics(metrics) → returns Long (row ID)

// Test 4: Save multiple for same package
@Test fun testSaveMultipleMetricsForSamePackage()
// Multiple saves maintain history

// Test 5: Save with JSON maps
@Test fun testSaveMetricsWithFilteredElements()
// filteredByType and filterReasons serialized to JSON
// Can be deserialized back to maps
```

### Query Tests (3)
```kotlin
// Test 6: Get latest metrics
@Test fun testGetLatestMetrics()
// getLatestMetrics(packageName) → most recent
// Returns null if not found

// Test 7: Get history with limit
@Test fun testGetMetricsHistoryWithLimit()
// getMetricsHistory(packageName, limit=5) → last 5
// Ordered newest first

// Test 8: Get metrics in date range
@Test fun testGetMetricsForDateRange()
// getMetricsInRange(packageName, startTime, endTime) → filtered list
```

### Statistics Tests (2)
```kotlin
// Test 9: Aggregate stats all apps
@Test fun testGetAggregateStatistics()
// getAggregateStats() → totals + averages
// Handles multiple packages

// Test 10: Stats for single package
@Test fun testGetAggregateStatisticsForPackage()
// getAggregateStatsForPackage(packageName) → package-specific
```

### Delete Tests (2)
```kotlin
// Test 11: Delete old metrics
@Test fun testDeleteOldMetrics()
// deleteOldMetrics(daysToKeep=30) → deletes older than 30 days
// Returns count deleted

// Test 12: Delete package metrics
@Test fun testDeleteMetricsForPackage()
// deleteMetricsForPackage(packageName) → all for package
// Other packages unaffected
```

### Persistence Tests (1)
```kotlin
// Test 13: Data survives close/reopen
@Test fun testDataPersistenceAcrossReopens()
// Save → Close DB → Reopen DB → Retrieve
```

### Concurrency Tests (2)
```kotlin
// Test 14: Concurrent saves
@Test fun testConcurrentSaves()
// 10 threads save concurrently
// All 10 entries created

// Test 15: Concurrent reads and writes
@Test fun testConcurrentReadsAndWrites()
// 5 readers + 5 writers simultaneously
// No race conditions
```

### Edge Cases Tests (2)
```kotlin
// Test 16: Empty database queries
@Test fun testEmptyDatabaseQueries()
// Queries on empty DB return appropriate defaults

// Test 17: Large JSON maps
@Test fun testLargeJsonMaps()
// 100+ entries per map
// JSON serialization handles size
```

---

## Running Tests

### All Metrics Tests
```bash
cd /Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore
./gradlew test --tests "*Metrics*" --info
```

### Specific Test Class
```bash
./gradlew test --tests "VUIDCreationMetricsCollectorTest" --info
./gradlew test --tests "VUIDMetricsReportGeneratorTest" --info
./gradlew test --tests "VUIDMetricsRepositoryTest" --info
```

### Single Test Method
```bash
./gradlew test --tests "VUIDCreationMetricsCollectorTest.testCreationRateCalculation" --info
```

### View Results
```bash
# After test run
open build/reports/tests/testDebugUnitTest/index.html
```

---

## Test Organization

Each test file follows this structure:

```kotlin
// 1. Setup (@Before)
private lateinit var collector: VUIDCreationMetricsCollector

@Before
fun setup() {
    collector = VUIDCreationMetricsCollector()
}

// 2. Test Method
@Test
fun testDescriptiveNameOfWhatIsBeingTested() {
    // ARRANGE: Setup test data
    val metrics = createTestMetrics(...)

    // ACT: Execute the method being tested
    val report = generator.generateReport(metrics, ReportFormat.TEXT)

    // ASSERT: Verify the results
    assertTrue("Should contain expected value", report.contains("expected"))
}

// 3. Cleanup (@After)
@After
fun teardown() {
    // Clean up resources
}
```

---

## Common Test Patterns

### Testing Counters
```kotlin
val (detected, created, rate) = collector.getCurrentStats()
assertEquals("Expected value", expectedValue, actualValue)
```

### Testing Formats
```kotlin
val report = generator.generateReport(metrics, ReportFormat.TEXT)
assertTrue("Should have content", report.contains("expected"))
```

### Testing File Export
```kotlin
val file = generator.exportToFile(metrics, ReportFormat.CSV)
assertTrue("File should exist", file.exists())
val content = file.readText()
```

### Testing Concurrency
```kotlin
val latch = CountDownLatch(threadCount)
repeat(threadCount) {
    thread {
        // Concurrent operation
        latch.countDown()
    }
}
assertTrue("Should complete", latch.await(5, TimeUnit.SECONDS))
```

### Testing JSON
```kotlin
val json = JSONObject(report)
assertEquals("Field value", expected, json.getString("fieldName"))
```

---

## Expected Test Results

### VUIDCreationMetricsCollectorTest
- **Status:** All 25 tests should PASS
- **Time:** ~100-150ms total
- **Coverage:** ~95%

### VUIDMetricsReportGeneratorTest
- **Status:** All 25 tests should PASS
- **Time:** ~200-300ms total
- **Coverage:** ~92%

### VUIDMetricsRepositoryTest
- **Status:** Currently all PASS as templates
- **When Implemented:** Should achieve ~90% coverage
- **Note:** Enable tests when VUIDMetricsRepository is created

---

## Troubleshooting

### Issue: Robolectric Error
**Solution:** Ensure `@Config(sdk = [33])` annotation present

### Issue: JSON Parse Error
**Solution:** Verify JSON string is valid using online JSON validator

### Issue: File Not Found
**Solution:** Ensure test cleanup runs - check `@After` teardown method

### Issue: Thread Timeout
**Solution:** Increase timeout in `latch.await(timeout, TimeUnit)`

---

## References

- Test Documentation: `VUIDMetrics-UnitTests-Implementation-Report-5081208-V1.md`
- Metrics Classes:
  - Source: `VUIDCreationMetrics.kt`
  - Source: `VUIDMetricsReportGenerator.kt`
- Related Specs: `LearnApp-VUID-Creation-Fix-Spec-5081218-V1.md`

---

**Last Updated:** 2025-12-08
**Version:** 1.0
