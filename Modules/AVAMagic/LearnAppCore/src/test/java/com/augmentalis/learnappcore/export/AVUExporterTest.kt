/**
 * AVUExporterTest.kt - Unit tests for AVUExporter
 *
 * Tests AVU format export and parsing functionality.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition - Phase 1 Testing)
 */

package com.augmentalis.learnappcore.export

import android.content.Context
import android.graphics.Rect
import com.augmentalis.learnappcore.exploration.ExplorationState
import com.augmentalis.learnappcore.models.ElementCategory
import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.learnappcore.safety.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AVUExporter.
 *
 * Tests:
 * - AVU header generation
 * - SCR (screen) line generation
 * - ELM (element) line generation
 * - NAV (navigation) line generation
 * - DNC (do not click) line generation
 * - Synonym section generation
 * - AVU file parsing
 *
 * Uses Robolectric for Android Context mocking.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AVUExporterTest {

    private lateinit var context: Context
    private lateinit var exporter: AVUExporter
    private lateinit var explorationState: ExplorationState

    companion object {
        private const val TEST_PACKAGE = "com.example.testapp"
        private const val TEST_APP_NAME = "Test App"
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = RuntimeEnvironment.getApplication()
        exporter = AVUExporter(context, ExportMode.DEVELOPER)
        explorationState = ExplorationState(TEST_PACKAGE, TEST_APP_NAME)
    }

    @After
    fun teardown() {
        unmockkAll()
        // Clean up test files
        val exportDir = File(context.getExternalFilesDir(null), "exports/dev")
        exportDir.listFiles()?.forEach { it.delete() }
    }

    // ============================================================
    // Test: export_ValidState_GeneratesCorrectHeader
    // ============================================================

    @Test
    fun export_ValidState_GeneratesCorrectHeader() {
        // Given: State with minimal data
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click Me", "com.test:id/button"))
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)
        explorationState.complete()

        // When: Exporting to AVU
        val result = exporter.export(explorationState)

        // Then: Export succeeds
        assertTrue(result.success, "Export should succeed")
        assertNotNull(result.filePath, "File path should be set")
        assertTrue(result.lineCount > 0, "Should have generated lines")

        // Then: File exists and contains correct header
        val file = File(result.filePath!!)
        assertTrue(file.exists(), "Export file should exist")

        val content = file.readText()
        assertTrue(content.contains("# Avanues Universal Format v2.2"), "Should have format header")
        assertTrue(content.contains("# Type: VOS"), "Should have type header")
        assertTrue(content.contains("# Mode: DEVELOPER"), "Should have mode header")
        assertTrue(content.contains("schema: avu-2.2"), "Should have schema version")
        assertTrue(content.contains("version: 2.2.0"), "Should have format version")
        assertTrue(content.contains("locale: en-US"), "Should have locale")
        assertTrue(content.contains("project: voiceos"), "Should have project")
        assertTrue(content.contains("category: learned_app"), "Should have category")
        assertTrue(content.contains("file: $TEST_PACKAGE.vos"), "Should have file metadata")
    }

    @Test
    fun export_WithMetadata_IncludesTimestampAndDuration() {
        // Given: State with exploration data
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click Me", "com.test:id/button"))
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)

        Thread.sleep(100) // Simulate exploration duration

        explorationState.complete()

        // When: Exporting
        val result = exporter.export(explorationState)

        // Then: Export succeeds with metadata
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        assertTrue(content.contains("timestamp:"), "Should have timestamp")
        assertTrue(content.contains("duration_s:"), "Should have duration")
        assertTrue(content.contains("exploration_mode: developer"), "Should have exploration mode")
    }

    // ============================================================
    // Test: export_WithScreens_GeneratesSCRLines
    // ============================================================

    @Test
    fun export_WithScreens_GeneratesSCRLines() {
        // Given: State with multiple screens
        explorationState.start()
        explorationState.beginExploring()

        val elements1 = listOf(
            createTestElement("Button", "Click Me", "com.test:id/button"),
            createTestElement("TextView", "Title", "com.test:id/title")
        )
        val fingerprint1 = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements1)

        val elements2 = listOf(
            createTestElement("Button", "Back", "com.test:id/back"),
            createTestElement("TextView", "Detail", "com.test:id/detail"),
            createTestElement("ImageView", "", "com.test:id/image")
        )
        val fingerprint2 = ScreenFingerprint.create("DetailActivity", TEST_PACKAGE, elements2)

        explorationState.recordScreen(fingerprint1)
        explorationState.recordScreen(fingerprint2)

        // When: Exporting
        val result = exporter.export(explorationState)

        // Then: Export succeeds
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        // Then: Contains SCR lines for both screens
        val scrLines = content.lines().filter { it.startsWith("SCR:") }
        assertEquals(2, scrLines.size, "Should have 2 SCR lines")

        // Verify SCR format: SCR:hash:activity:timestamp:element_count
        val scr1 = scrLines.find { it.contains("MainActivity") }
        assertNotNull(scr1, "Should have MainActivity SCR line")
        assertTrue(scr1.contains(fingerprint1.screenHash), "Should contain screen hash")
        assertTrue(scr1.endsWith(":2"), "Should have element count 2")

        val scr2 = scrLines.find { it.contains("DetailActivity") }
        assertNotNull(scr2, "Should have DetailActivity SCR line")
        assertTrue(scr2.contains(fingerprint2.screenHash), "Should contain screen hash")
        assertTrue(scr2.endsWith(":3"), "Should have element count 3")
    }

    // ============================================================
    // Test: export_WithElements_GeneratesELMLines
    // ============================================================

    @Test
    fun export_WithElements_GeneratesELMLines() {
        // Given: State with screen and elements
        explorationState.start()
        explorationState.beginExploring()

        val testElements = listOf(
            createTestElement("Button", "Submit", "com.test:id/submit", isClickable = true)
                .copy(category = ElementCategory.ACTION),
            createTestElement("EditText", "", "com.test:id/input", isClickable = false)
                .copy(category = ElementCategory.INPUT),
            createTestElement("TextView", "Label", "com.test:id/label", isClickable = false)
                .copy(category = ElementCategory.DISPLAY)
        )

        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, testElements)
        explorationState.recordScreen(fingerprint)
        explorationState.recordElements(testElements)

        // When: Exporting
        val result = exporter.export(explorationState)

        // Then: Export succeeds
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        // Then: Contains ELM lines for all elements
        val elmLines = content.lines().filter { it.startsWith("ELM:") }
        assertEquals(3, elmLines.size, "Should have 3 ELM lines")

        // Verify ELM format: ELM:uuid:label:type:actions:bounds:category
        val submitLine = elmLines.find { it.contains("Submit") }
        assertNotNull(submitLine, "Should have Submit ELM line")
        assertTrue(submitLine.contains("Button"), "Should contain element type")
        assertTrue(submitLine.contains("click"), "Should contain click action")
        assertTrue(submitLine.contains("ACT"), "Should have ACTION category")

        val inputLine = elmLines.find { it.contains(":input:") || it.contains("EditText") }
        assertNotNull(inputLine, "Should have input ELM line")
        assertTrue(inputLine.contains("EditText"), "Should contain element type")
        assertTrue(inputLine.contains("INP"), "Should have INPUT category")

        val labelLine = elmLines.find { it.contains("Label") }
        assertNotNull(labelLine, "Should have Label ELM line")
        assertTrue(labelLine.contains("DSP"), "Should have DISPLAY category")
    }

    @Test
    fun export_ElementWithBounds_IncludesCorrectBounds() {
        // Given: Element with specific bounds
        explorationState.start()
        explorationState.beginExploring()

        val element = createTestElement("Button", "Test", "com.test:id/test", isClickable = true)
            .copy(bounds = Rect(10, 20, 110, 70))

        val elements = listOf(element)
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)
        explorationState.recordElements(elements)

        // When: Exporting
        val result = exporter.export(explorationState)

        // Then: Export succeeds
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        // Then: ELM line contains correct bounds
        val elmLine = content.lines().find { it.startsWith("ELM:") && it.contains("Test") }
        assertNotNull(elmLine)
        assertTrue(elmLine.contains("10,20,110,70"), "Should have bounds 10,20,110,70")
    }

    // ============================================================
    // Test: export_WithNavigation_GeneratesNAVLines
    // ============================================================

    @Test
    fun export_WithNavigation_GeneratesNAVLines() {
        // Given: State with navigation history
        explorationState.start()
        explorationState.beginExploring()

        val elements1 = listOf(createTestElement("Button", "Next", "com.test:id/next", isClickable = true))
        val fingerprint1 = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements1)

        val elements2 = listOf(createTestElement("Button", "Back", "com.test:id/back", isClickable = true))
        val fingerprint2 = ScreenFingerprint.create("DetailActivity", TEST_PACKAGE, elements2)

        explorationState.recordScreen(fingerprint1)
        explorationState.recordElements(elements1)
        explorationState.recordScreen(fingerprint2)

        // Record navigation
        explorationState.recordNavigation(
            fingerprint1.screenHash,
            fingerprint2.screenHash,
            elements1[0]
        )

        // When: Exporting
        val result = exporter.export(explorationState)

        // Then: Export succeeds
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        // Then: Contains NAV line
        val navLines = content.lines().filter { it.startsWith("NAV:") }
        assertEquals(1, navLines.size, "Should have 1 NAV line")

        // Verify NAV format: NAV:from_hash:to_hash:trigger_uuid:trigger_label:timestamp
        val navLine = navLines[0]
        assertTrue(navLine.contains(fingerprint1.screenHash), "Should contain source screen hash")
        assertTrue(navLine.contains(fingerprint2.screenHash), "Should contain destination screen hash")
        assertTrue(navLine.contains("Next"), "Should contain trigger label")
    }

    @Test
    fun export_MultipleNavigations_GeneratesAllNAVLines() {
        // Given: State with multiple navigations
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click", "com.test:id/click", isClickable = true))
        val fp1 = ScreenFingerprint.create("Screen1", TEST_PACKAGE, elements)
        val fp2 = ScreenFingerprint.create("Screen2", TEST_PACKAGE, elements)
        val fp3 = ScreenFingerprint.create("Screen3", TEST_PACKAGE, elements)

        explorationState.recordScreen(fp1)
        explorationState.recordElements(elements)

        // Nav 1->2
        explorationState.recordScreen(fp2)
        explorationState.recordNavigation(fp1.screenHash, fp2.screenHash, elements[0])

        // Nav 2->3
        explorationState.recordScreen(fp3)
        explorationState.recordNavigation(fp2.screenHash, fp3.screenHash, elements[0])

        // When: Exporting
        val result = exporter.export(explorationState)

        // Then: Contains both NAV lines
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()
        val navLines = content.lines().filter { it.startsWith("NAV:") }
        assertEquals(2, navLines.size, "Should have 2 NAV lines")
    }

    // ============================================================
    // Test: export_WithDNC_GeneratesDNCLines
    // ============================================================

    @Test
    fun export_WithDNC_GeneratesDNCLines() {
        // Given: State with dangerous elements
        explorationState.start()
        explorationState.beginExploring()

        val dangerousElements = listOf(
            createTestElement("Button", "End Call", "com.test:id/end_call", isClickable = true),
            createTestElement("Button", "Delete All", "com.test:id/delete_all", isClickable = true),
            createTestElement("Button", "Purchase", "com.test:id/purchase", isClickable = true)
        )

        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, dangerousElements)
        explorationState.recordScreen(fingerprint)

        // Record as dangerous
        explorationState.recordDangerousElement(dangerousElements[0], DoNotClickReason.CALL_ACTION)
        explorationState.recordDangerousElement(dangerousElements[1], DoNotClickReason.EXIT_ACTION)
        explorationState.recordDangerousElement(dangerousElements[2], DoNotClickReason.PAYMENT_ACTION)

        // When: Exporting
        val result = exporter.export(explorationState)

        // Then: Export succeeds
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        // Then: Contains DNC lines
        val dncLines = content.lines().filter { it.startsWith("DNC:") }
        assertEquals(3, dncLines.size, "Should have 3 DNC lines")

        // Verify DNC format: DNC:element_id:label:type:reason
        val callLine = dncLines.find { it.contains("End Call") }
        assertNotNull(callLine, "Should have End Call DNC line")
        assertTrue(callLine.contains("CALL_ACTION"), "Should have CALL_ACTION reason")

        val exitLine = dncLines.find { it.contains("Delete All") }
        assertNotNull(exitLine, "Should have Delete All DNC line")
        assertTrue(exitLine.contains("EXIT_ACTION"), "Should have EXIT_ACTION reason")

        val paymentLine = dncLines.find { it.contains("Purchase") }
        assertNotNull(paymentLine, "Should have Purchase DNC line")
        assertTrue(paymentLine.contains("PAYMENT_ACTION"), "Should have PAYMENT_ACTION reason")
    }

    @Test
    fun export_WithDynamicRegions_GeneratesDYNLines() {
        // Given: State with dynamic regions
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click", "com.test:id/click"))
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)

        val dynamicRegion = DynamicRegion(
            screenHash = fingerprint.screenHash,
            regionId = "feed_region",
            bounds = Rect(0, 0, 1080, 1920),
            changeType = DynamicChangeType.INFINITE_SCROLL,
            changeCount = 3
        )
        explorationState.recordDynamicRegion(dynamicRegion)

        // When: Exporting
        val result = exporter.export(explorationState)

        // Then: Export succeeds
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        // Then: Contains DYN line
        val dynLines = content.lines().filter { it.startsWith("DYN:") }
        assertEquals(1, dynLines.size, "Should have 1 DYN line")

        // Verify DYN format: DYN:screen_hash:region_id:change_type
        val dynLine = dynLines[0]
        assertTrue(dynLine.contains(fingerprint.screenHash), "Should contain screen hash")
        assertTrue(dynLine.contains("feed_region"), "Should contain region ID")
        assertTrue(dynLine.contains("INFINITE_SCROLL"), "Should contain change type")
    }

    // ============================================================
    // Test: export_WithSynonyms_GeneratesSynonymSection
    // ============================================================

    @Test
    fun export_WithSynonyms_GeneratesSynonymSection() {
        // Given: State with exploration data
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click", "com.test:id/click"))
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)

        val synonyms = listOf(
            SynonymSet("call", listOf("phone", "dial", "ring")),
            SynonymSet("message", listOf("text", "sms", "chat")),
            SynonymSet("delete", listOf("remove", "erase", "clear"))
        )

        // When: Exporting with synonyms
        val result = exporter.export(explorationState, emptyList(), synonyms)

        // Then: Export succeeds
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        // Then: Contains synonym section
        assertTrue(content.contains("synonyms:"), "Should have synonyms section")
        assertTrue(content.contains("call: [phone, dial, ring]"), "Should have call synonyms")
        assertTrue(content.contains("message: [text, sms, chat]"), "Should have message synonyms")
        assertTrue(content.contains("delete: [remove, erase, clear]"), "Should have delete synonyms")
    }

    @Test
    fun export_WithCommands_GeneratesCMDLines() {
        // Given: State with exploration data
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(
            createTestElement("Button", "Submit", "com.test:id/submit", isClickable = true)
        )
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)
        explorationState.recordElements(elements)

        val commands = listOf(
            GeneratedCommand("cmd-001", "submit form", "click", elements[0].stableId(), 0.95f),
            GeneratedCommand("cmd-002", "send message", "click", elements[0].stableId(), 0.87f)
        )

        // When: Exporting with commands
        val result = exporter.export(explorationState, commands)

        // Then: Export succeeds
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        // Then: Contains CMD lines
        val cmdLines = content.lines().filter { it.startsWith("CMD:") }
        assertEquals(2, cmdLines.size, "Should have 2 CMD lines")

        // Verify CMD format: CMD:uuid:trigger:action:element_uuid:confidence
        val cmd1 = cmdLines.find { it.contains("submit form") }
        assertNotNull(cmd1, "Should have submit form CMD line")
        assertTrue(cmd1.contains("0.95"), "Should have confidence 0.95")

        val cmd2 = cmdLines.find { it.contains("send message") }
        assertNotNull(cmd2, "Should have send message CMD line")
        assertTrue(cmd2.contains("0.87"), "Should have confidence 0.87")
    }

    // ============================================================
    // Test: parseAvuFile_ValidFile_ReturnsCorrectData
    // ============================================================

    @Test
    fun parseAvuFile_ValidFile_ReturnsCorrectData() {
        // Given: State with exploration data
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(
            createTestElement("Button", "Submit", "com.test:id/submit", isClickable = true),
            createTestElement("TextView", "Title", "com.test:id/title", isClickable = false)
        )
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)
        explorationState.recordElements(elements)

        val dangerousElement = createTestElement("Button", "Delete", "com.test:id/delete", isClickable = true)
        explorationState.recordDangerousElement(dangerousElement, DoNotClickReason.EXIT_ACTION)

        val synonyms = listOf(
            SynonymSet("call", listOf("phone", "dial"))
        )

        // Export first
        val exportResult = exporter.export(explorationState, emptyList(), synonyms)
        assertTrue(exportResult.success)

        // When: Parsing the exported file
        val parsedData = exporter.parseAvuFile(exportResult.filePath!!)

        // Then: Data is parsed correctly
        assertNotNull(parsedData, "Should parse file successfully")
        assertEquals(TEST_PACKAGE, parsedData.getPackageName(), "Should have correct package name")
        assertEquals(TEST_APP_NAME, parsedData.getAppName(), "Should have correct app name")
        assertEquals(1, parsedData.getScreenCount(), "Should have 1 screen")
        assertEquals(2, parsedData.getElementCount(), "Should have 2 elements")

        // Verify metadata
        assertTrue(parsedData.metadata.containsKey("schema"), "Should have schema metadata")
        assertTrue(parsedData.metadata.containsKey("version"), "Should have version metadata")

        // Verify stats
        val stats = parsedData.getStats()
        assertNotNull(stats, "Should have stats")
        assertEquals(1, stats.screensExplored, "Should have 1 screen explored")

        // Verify lines
        assertTrue(parsedData.screenLines.isNotEmpty(), "Should have screen lines")
        assertTrue(parsedData.elementLines.isNotEmpty(), "Should have element lines")
        assertTrue(parsedData.dncLines.isNotEmpty(), "Should have DNC lines")

        // Verify synonyms
        assertTrue(parsedData.synonyms.containsKey("call"), "Should have call synonyms")
        assertEquals(listOf("phone", "dial"), parsedData.synonyms["call"], "Should have correct synonyms")
    }

    @Test
    fun parseAvuFile_NonexistentFile_ReturnsNull() {
        // Given: Invalid file path
        val invalidPath = "/nonexistent/path/file.vos"

        // When: Parsing nonexistent file
        val result = exporter.parseAvuFile(invalidPath)

        // Then: Returns null
        assertEquals(null, result, "Should return null for nonexistent file")
    }

    @Test
    fun exportToString_ValidState_ReturnsContent() {
        // Given: State with data
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Test", "com.test:id/test"))
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)

        // When: Exporting to string
        val content = exporter.exportToString(explorationState)

        // Then: Content is generated
        assertTrue(content.isNotEmpty(), "Should generate content")
        assertTrue(content.contains("# Avanues Universal Format v1.0"), "Should have header")
        assertTrue(content.contains("APP:$TEST_PACKAGE"), "Should have APP line")
        assertTrue(content.contains("SCR:"), "Should have SCR line")
    }

    @Test
    fun export_EmptyState_GeneratesMinimalFile() {
        // Given: Fresh state (no exploration data)
        explorationState.start()

        // When: Exporting
        val result = exporter.export(explorationState)

        // Then: Export succeeds with minimal data
        assertTrue(result.success)
        assertNotNull(result.filePath)

        val content = File(result.filePath!!).readText()
        assertTrue(content.contains("APP:$TEST_PACKAGE"), "Should have APP line")
        assertTrue(content.contains("STA:0:0:0"), "Should have empty stats")
        assertFalse(content.contains("SCR:"), "Should not have SCR lines")
        assertFalse(content.contains("ELM:"), "Should not have ELM lines")
    }

    @Test
    fun export_UserMode_GeneratesUserModeFile() {
        // Given: Exporter in USER mode
        val userExporter = AVUExporter(context, ExportMode.USER)

        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Test", "com.test:id/test"))
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)

        // When: Exporting
        val result = userExporter.export(explorationState)

        // Then: Export succeeds
        assertTrue(result.success)
        val content = File(result.filePath!!).readText()

        // Then: Mode is USER
        assertTrue(content.contains("# Mode: USER"), "Should have USER mode")
        assertTrue(content.contains("exploration_mode: automated"), "Should have automated exploration mode")

        // Clean up
        File(result.filePath!!).delete()
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private fun createTestElement(
        className: String,
        text: String,
        resourceId: String,
        isClickable: Boolean = false
    ): ElementInfo {
        return ElementInfo(
            className = "android.widget.$className",
            text = text,
            resourceId = resourceId,
            isClickable = isClickable,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100),
            uuid = resourceId.substringAfterLast("/")
        )
    }
}
