/**
 * ExplorationFlowTest.kt - End-to-End Exploration Flow Tests
 *
 * Phase 2 Integration Tests - Task 2.3
 * Tests complete exploration workflows from UI to database persistence.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-Phase2-Tests-51211-V1.md
 */

package com.augmentalis.learnapp

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.augmentalis.learnappcore.exploration.ExplorationPhase
import com.augmentalis.learnappcore.exploration.ExplorationState
import com.augmentalis.learnappcore.export.AVUExporter
import com.augmentalis.learnappcore.export.ExportMode
import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.learnappcore.safety.DoNotClickReason
import com.augmentalis.learnappcore.safety.LoginType
import com.augmentalis.learnappcore.safety.SafetyManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-End Exploration Flow Tests
 *
 * Tests complete exploration workflows including:
 * - Simple app exploration
 * - Login screen detection and pausing
 * - Do-Not-Click element skipping
 * - AVU export generation
 *
 * @since 2.1.0 (Phase 2 E2E Tests)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ExplorationFlowTest {

    private lateinit var device: UiDevice
    private lateinit var explorationState: ExplorationState
    private lateinit var safetyManager: SafetyManager
    private lateinit var avuExporter: AVUExporter

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Initialize exploration components
        explorationState = ExplorationState(
            packageName = "com.example.simpleapp",
            appName = "Simple Test App"
        )

        safetyManager = SafetyManager.create(null)

        avuExporter = AVUExporter(
            context = ApplicationProvider.getApplicationContext(),
            mode = ExportMode.TEST
        )
    }

    @After
    fun teardown() {
        // Clean up any generated test files
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val testDir = File(context.filesDir, "test_exports")
        if (testDir.exists()) {
            testDir.deleteRecursively()
        }
    }

    /**
     * Test: fullExploration_SimpleApp_CompletesSuccessfully
     *
     * Verifies that a complete exploration flow works end-to-end:
     * 1. Start exploration
     * 2. Process multiple screens
     * 3. Discover elements
     * 4. Complete exploration
     * 5. Verify stats
     */
    @Test
    fun fullExploration_SimpleApp_CompletesSuccessfully() {
        // Arrange
        val testScreens = createSimpleAppScreens()

        // Act - Start exploration
        explorationState.reset()
        explorationState.start()
        assertEquals("Should be in READY phase", ExplorationPhase.READY, explorationState.phase)

        explorationState.beginExploring()
        assertEquals("Should be in EXPLORING phase", ExplorationPhase.EXPLORING, explorationState.phase)

        // Process screens
        for (screen in testScreens) {
            explorationState.processScreen(screen.fingerprint, screen.elements)

            // Click some elements
            screen.elements.take(3).forEach { element ->
                if (!safetyManager.isDangerous(element)) {
                    explorationState.clickElement(element)
                }
            }
        }

        // Complete exploration
        explorationState.complete()

        // Assert
        assertEquals("Should be in COMPLETED phase", ExplorationPhase.COMPLETED, explorationState.phase)

        val stats = explorationState.getStats()
        assertTrue("Should have explored screens", stats.screensExplored > 0)
        assertTrue("Should have discovered elements", stats.elementsDiscovered > 0)
        assertTrue("Should have clicked elements", stats.elementsClicked > 0)
        assertTrue("Coverage should be > 0", stats.coverage > 0f)

        // Verify all screens are tracked
        assertEquals("Should track all screens", testScreens.size, stats.screensExplored)
    }

    /**
     * Test: exploration_WithLoginScreen_PausesCorrectly
     *
     * Verifies that exploration correctly pauses when a login
     * screen is detected and waits for user intervention.
     */
    @Test
    fun exploration_WithLoginScreen_PausesCorrectly() {
        // Arrange
        val normalScreen = createTestScreen(
            hash = "normal_screen_001",
            elements = createButtonElements(count = 5)
        )

        val loginScreen = createLoginScreen(
            hash = "login_screen_001",
            loginType = LoginType.STANDARD
        )

        // Act - Start exploration
        explorationState.reset()
        explorationState.start()
        explorationState.beginExploring()

        // Process normal screen first
        explorationState.processScreen(normalScreen.fingerprint, normalScreen.elements)
        assertEquals("Should be exploring", ExplorationPhase.EXPLORING, explorationState.phase)

        // Process login screen
        val loginResult = safetyManager.processScreen(loginScreen.fingerprint, loginScreen.elements)

        if (loginResult.isLoginScreen) {
            explorationState.waitForUser("Login screen detected: ${loginResult.loginType?.name}")
        }

        // Assert
        assertEquals("Should be waiting for user", ExplorationPhase.WAITING_USER, explorationState.phase)

        val stats = explorationState.getStats()
        assertEquals("Should have processed 2 screens", 2, stats.screensExplored)

        // Verify login screen tracking
        val lastScreen = explorationState.getCurrentScreen()
        assertEquals("Current screen should be login screen", "login_screen_001", lastScreen?.screenHash)
    }

    /**
     * Test: exploration_WithDNCElement_SkipsElement
     *
     * Verifies that exploration correctly identifies and skips
     * dangerous elements (Do-Not-Click list).
     */
    @Test
    fun exploration_WithDNCElement_SkipsElement() {
        // Arrange
        val elements = mutableListOf<ElementInfo>()

        // Add safe elements
        elements.addAll(createButtonElements(count = 5))

        // Add dangerous elements
        val deleteButton = createElementInfo(
            stableId = "delete_all_button",
            text = "Delete All Data",
            type = "Button",
            isClickable = true
        )

        val uninstallButton = createElementInfo(
            stableId = "uninstall_button",
            text = "Uninstall App",
            type = "Button",
            isClickable = true
        )

        elements.add(deleteButton)
        elements.add(uninstallButton)

        val screen = createTestScreen(
            hash = "dangerous_screen_001",
            elements = elements
        )

        // Act - Start exploration
        explorationState.reset()
        explorationState.start()
        explorationState.beginExploring()

        explorationState.processScreen(screen.fingerprint, screen.elements)

        var dangerousCount = 0

        // Try to click all elements
        screen.elements.forEach { element ->
            val result = safetyManager.checkElement(element)

            if (result.isDangerous) {
                explorationState.recordDangerousElement(element, result.reason!!)
                dangerousCount++
            } else {
                explorationState.clickElement(element)
            }
        }

        // Assert
        assertTrue("Should have detected dangerous elements", dangerousCount > 0)

        val stats = explorationState.getStats()
        assertEquals("Should have skipped dangerous elements", dangerousCount, stats.dangerousElementsSkipped)

        // Verify safe elements were clicked
        val safeElementCount = elements.size - dangerousCount
        assertTrue("Should have clicked safe elements", stats.elementsClicked >= safeElementCount - 2)
    }

    /**
     * Test: exploration_Export_GeneratesValidAVU
     *
     * Verifies that exploration data can be successfully exported
     * to AVU format and the file contains valid data.
     */
    @Test
    fun exploration_Export_GeneratesValidAVU() {
        // Arrange
        val testScreens = createSimpleAppScreens()

        explorationState.reset()
        explorationState.start()
        explorationState.beginExploring()

        // Process screens and click elements
        for (screen in testScreens) {
            explorationState.processScreen(screen.fingerprint, screen.elements)

            screen.elements.take(2).forEach { element ->
                if (!safetyManager.isDangerous(element)) {
                    explorationState.clickElement(element)
                }
            }
        }

        explorationState.complete()

        // Generate commands (normally done by CommandGenerator in activity)
        val elements = explorationState.getElements()
        val mockCommands = elements.take(10).mapIndexed { index, element ->
            createMockCommand(
                id = "cmd_$index",
                text = element.text ?: "action $index",
                elementId = element.stableId
            )
        }

        val mockSynonyms = mapOf(
            "cmd_0" to listOf("open", "show", "display"),
            "cmd_1" to listOf("close", "hide", "dismiss")
        )

        // Act - Export to AVU
        val result = avuExporter.export(explorationState, mockCommands, mockSynonyms)

        // Assert
        assertTrue("Export should succeed", result.success)
        assertNotNull("Export path should not be null", result.filePath)
        assertTrue("Line count should be > 0", result.lineCount > 0)

        // Verify file exists
        val exportFile = File(result.filePath!!)
        assertTrue("Export file should exist", exportFile.exists())
        assertTrue("Export file should not be empty", exportFile.length() > 0)

        // Read file content
        val content = exportFile.readText()

        // Verify AVU format markers
        assertTrue("Should contain AVU header", content.contains("# AVU") || content.contains("# VoiceOS"))
        assertTrue("Should contain package name", content.contains("com.example.simpleapp"))
        assertTrue("Should contain commands", mockCommands.any { cmd ->
            content.contains(cmd.text)
        })

        // Verify metadata
        val stats = explorationState.getStats()
        assertTrue("Should contain screen count", content.contains(stats.screensExplored.toString()))

        // Cleanup
        exportFile.delete()
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Create simple app test screens
     */
    private fun createSimpleAppScreens(): List<TestScreen> {
        return listOf(
            createTestScreen(
                hash = "home_screen_001",
                activityName = "MainActivity",
                elements = createButtonElements(count = 10)
            ),
            createTestScreen(
                hash = "settings_screen_001",
                activityName = "SettingsActivity",
                elements = createButtonElements(count = 8)
            ),
            createTestScreen(
                hash = "about_screen_001",
                activityName = "AboutActivity",
                elements = createButtonElements(count = 5)
            )
        )
    }

    /**
     * Create test screen with fingerprint and elements
     */
    private fun createTestScreen(
        hash: String,
        activityName: String = "TestActivity",
        elements: List<ElementInfo>
    ): TestScreen {
        return TestScreen(
            fingerprint = com.augmentalis.learnappcore.safety.ScreenFingerprint(
                screenHash = hash,
                activityName = activityName,
                packageName = "com.example.simpleapp",
                elementCount = elements.size,
                hasPasswordField = false,
                hasUsernameField = false,
                buttonTexts = elements.filter { it.type == "Button" }.mapNotNull { it.text }
            ),
            elements = elements
        )
    }

    /**
     * Create login screen
     */
    private fun createLoginScreen(
        hash: String,
        loginType: LoginType
    ): TestScreen {
        val elements = mutableListOf<ElementInfo>()

        // Add username field
        elements.add(
            createElementInfo(
                stableId = "username_field",
                text = null,
                contentDesc = "Username",
                type = "EditText",
                isClickable = true
            )
        )

        // Add password field
        elements.add(
            createElementInfo(
                stableId = "password_field",
                text = null,
                contentDesc = "Password",
                type = "EditText",
                isClickable = true
            )
        )

        // Add login button
        elements.add(
            createElementInfo(
                stableId = "login_button",
                text = "Login",
                type = "Button",
                isClickable = true
            )
        )

        return TestScreen(
            fingerprint = com.augmentalis.learnappcore.safety.ScreenFingerprint(
                screenHash = hash,
                activityName = "LoginActivity",
                packageName = "com.example.simpleapp",
                elementCount = elements.size,
                hasPasswordField = true,
                hasUsernameField = true,
                buttonTexts = listOf("Login")
            ),
            elements = elements
        )
    }

    /**
     * Create button elements for testing
     */
    private fun createButtonElements(count: Int): List<ElementInfo> {
        return (1..count).map { i ->
            createElementInfo(
                stableId = "button_$i",
                text = "Button $i",
                type = "Button",
                isClickable = true
            )
        }
    }

    /**
     * Create ElementInfo for testing
     */
    private fun createElementInfo(
        stableId: String,
        text: String?,
        contentDesc: String? = null,
        type: String = "Button",
        isClickable: Boolean = true
    ): ElementInfo {
        return ElementInfo(
            stableId = stableId,
            text = text,
            contentDescription = contentDesc,
            className = "android.widget.$type",
            bounds = android.graphics.Rect(0, 0, 100, 50),
            isClickable = isClickable,
            isLongClickable = false,
            isScrollable = false,
            isFocusable = isClickable,
            isEnabled = true,
            isPassword = false,
            isChecked = null,
            viewIdResourceName = "com.example.simpleapp:id/${stableId}",
            packageName = "com.example.simpleapp"
        )
    }

    /**
     * Create mock command for export testing
     */
    private fun createMockCommand(
        id: String,
        text: String,
        elementId: String
    ): com.augmentalis.learnappcore.export.VoiceCommand {
        return com.augmentalis.learnappcore.export.VoiceCommand(
            id = id,
            text = text,
            elementId = elementId,
            action = "click",
            confidence = 1.0f
        )
    }

    /**
     * Test screen data class
     */
    private data class TestScreen(
        val fingerprint: com.augmentalis.learnappcore.safety.ScreenFingerprint,
        val elements: List<ElementInfo>
    )
}
