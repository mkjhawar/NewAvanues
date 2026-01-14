package com.augmentalis.voiceoscoreng.exploration

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExplorationEngineTest {

    private lateinit var engine: ExplorationEngine

    @Before
    fun setup() {
        LearnAppDevToggle.reset()
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        engine = ExplorationEngine()
    }

    @After
    fun teardown() {
        engine.stop()
        LearnAppDevToggle.reset()
    }

    // ==================== State Tests ====================

    @Test
    fun `isRunning returns false initially`() {
        assertFalse(engine.isRunning())
    }

    @Test
    fun `start changes state to running`() {
        engine.start("com.test.app")
        assertTrue(engine.isRunning())
    }

    @Test
    fun `stop changes state to not running`() {
        engine.start("com.test.app")
        engine.stop()
        assertFalse(engine.isRunning())
    }

    @Test
    fun `pause pauses exploration`() {
        engine.start("com.test.app")
        engine.pause()
        assertTrue(engine.isPaused())
    }

    @Test
    fun `resume continues exploration`() {
        engine.start("com.test.app")
        engine.pause()
        engine.resume()
        assertFalse(engine.isPaused())
    }

    // ==================== Package Tests ====================

    @Test
    fun `getCurrentPackage returns current package`() {
        engine.start("com.test.app")
        assertEquals("com.test.app", engine.getCurrentPackage())
    }

    @Test
    fun `getCurrentPackage returns null when not running`() {
        assertEquals(null, engine.getCurrentPackage())
    }

    // ==================== Screen State Tests ====================

    @Test
    fun `captureScreen returns screen state`() {
        engine.start("com.test.app")
        val screenState = engine.captureScreen(createTestElements())

        assertNotNull(screenState)
        assertEquals("com.test.app", screenState.packageName)
    }

    @Test
    fun `captureScreen includes elements`() {
        engine.start("com.test.app")
        val elements = createTestElements()
        val screenState = engine.captureScreen(elements)

        assertEquals(elements.size, screenState.elements.size)
    }

    @Test
    fun `captureScreen increments screen count`() {
        engine.start("com.test.app")
        engine.captureScreen(createTestElements())
        engine.captureScreen(createTestElements())

        assertEquals(2, engine.getScreenCount())
    }

    // ==================== Element Collection Tests ====================

    @Test
    fun `getTotalElements returns 0 initially`() {
        assertEquals(0, engine.getTotalElements())
    }

    @Test
    fun `captureScreen increases total elements`() {
        engine.start("com.test.app")
        val elements = createTestElements()
        engine.captureScreen(elements)

        assertEquals(elements.size, engine.getTotalElements())
    }

    // ==================== Framework Detection Tests ====================

    @Test
    fun `detectFramework returns framework info`() {
        engine.start("com.test.app")
        val elements = createFlutterElements()
        val framework = engine.detectFramework(elements)

        assertNotNull(framework)
        assertEquals(FrameworkType.FLUTTER, framework.type)
    }

    @Test
    fun `detectFramework returns NATIVE for native apps`() {
        engine.start("com.test.app")
        val elements = createNativeElements()
        val framework = engine.detectFramework(elements)

        assertEquals(FrameworkType.NATIVE, framework.type)
    }

    // ==================== Feature Gate Tests ====================

    @Test
    fun `exploration requires DEV tier`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        val liteEngine = ExplorationEngine()

        assertFalse(liteEngine.isAvailable())
    }

    @Test
    fun `exploration available in DEV tier`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        assertTrue(engine.isAvailable())
    }

    // ==================== Progress Tests ====================

    @Test
    fun `getProgress returns 0 initially`() {
        assertEquals(0f, engine.getProgress())
    }

    @Test
    fun `exploration updates progress`() {
        engine.start("com.test.app")
        engine.captureScreen(createTestElements())
        assertTrue(engine.getProgress() > 0f)
    }

    // ==================== Reset Tests ====================

    @Test
    fun `reset clears all state`() {
        engine.start("com.test.app")
        engine.captureScreen(createTestElements())
        engine.reset()

        assertFalse(engine.isRunning())
        assertEquals(0, engine.getScreenCount())
        assertEquals(0, engine.getTotalElements())
    }

    // ==================== Helper Methods ====================

    private fun createTestElements(): List<ElementInfo> {
        return listOf(
            ElementInfo(
                className = "android.widget.Button",
                resourceId = "btn_submit",
                text = "Submit",
                contentDescription = "Submit button",
                isClickable = true,
                bounds = Bounds(0, 0, 100, 50)
            ),
            ElementInfo(
                className = "android.widget.EditText",
                resourceId = "et_username",
                text = "",
                contentDescription = "Username input",
                isClickable = true,
                bounds = Bounds(0, 60, 200, 100)
            )
        )
    }

    private fun createFlutterElements(): List<ElementInfo> {
        return listOf(
            ElementInfo(
                className = "io.flutter.embedding.FlutterView",
                resourceId = "",
                text = "",
                contentDescription = "",
                isClickable = false,
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )
    }

    private fun createNativeElements(): List<ElementInfo> {
        return listOf(
            ElementInfo(
                className = "android.widget.FrameLayout",
                resourceId = "content",
                text = "",
                contentDescription = "",
                isClickable = false,
                bounds = Bounds(0, 0, 1080, 1920)
            )
        )
    }
}
