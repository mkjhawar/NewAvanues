package com.augmentalis.voiceoscoreng.avu

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.functions.getCurrentTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AVU Models Tests
 *
 * TDD tests for the AVU (Avanues Universal) format models.
 * AVU is a compact line-based format for voice accessibility data.
 */
class AVUModelsTest {

    // ==================== ElementType Tests ====================

    @Test
    fun `ElementType has all expected types`() {
        val types = ElementType.entries
        assertTrue(types.contains(ElementType.BUTTON))
        assertTrue(types.contains(ElementType.TEXT_FIELD))
        assertTrue(types.contains(ElementType.CHECKBOX))
        assertTrue(types.contains(ElementType.SWITCH))
        assertTrue(types.contains(ElementType.DROPDOWN))
        assertTrue(types.contains(ElementType.TAB))
        assertTrue(types.contains(ElementType.OTHER))
    }

    @Test
    fun `ElementType fromClassName detects button`() {
        assertEquals(ElementType.BUTTON, ElementType.fromClassName("android.widget.Button"))
        assertEquals(ElementType.BUTTON, ElementType.fromClassName("android.widget.ImageButton"))
        assertEquals(ElementType.BUTTON, ElementType.fromClassName("MaterialButton"))
    }

    @Test
    fun `ElementType fromClassName detects text field`() {
        assertEquals(ElementType.TEXT_FIELD, ElementType.fromClassName("android.widget.EditText"))
        assertEquals(ElementType.TEXT_FIELD, ElementType.fromClassName("TextInput"))
        assertEquals(ElementType.TEXT_FIELD, ElementType.fromClassName("AutoCompleteTextView"))
    }

    @Test
    fun `ElementType fromClassName detects checkbox`() {
        assertEquals(ElementType.CHECKBOX, ElementType.fromClassName("android.widget.CheckBox"))
    }

    @Test
    fun `ElementType fromClassName detects switch`() {
        assertEquals(ElementType.SWITCH, ElementType.fromClassName("android.widget.Switch"))
        assertEquals(ElementType.SWITCH, ElementType.fromClassName("ToggleButton"))
    }

    @Test
    fun `ElementType fromClassName detects dropdown`() {
        assertEquals(ElementType.DROPDOWN, ElementType.fromClassName("android.widget.Spinner"))
        assertEquals(ElementType.DROPDOWN, ElementType.fromClassName("DropDownView"))
    }

    @Test
    fun `ElementType fromClassName detects tab`() {
        assertEquals(ElementType.TAB, ElementType.fromClassName("TabLayout"))
        assertEquals(ElementType.TAB, ElementType.fromClassName("TabView"))
    }

    @Test
    fun `ElementType fromClassName returns OTHER for unknown`() {
        assertEquals(ElementType.OTHER, ElementType.fromClassName("CustomView"))
        assertEquals(ElementType.OTHER, ElementType.fromClassName("android.view.View"))
    }

    // ==================== CommandActionType Tests ====================

    @Test
    fun `CommandActionType has all expected types`() {
        val types = CommandActionType.entries
        assertTrue(types.contains(CommandActionType.CLICK))
        assertTrue(types.contains(CommandActionType.LONG_CLICK))
        assertTrue(types.contains(CommandActionType.TYPE))
        assertTrue(types.contains(CommandActionType.NAVIGATE))
        assertTrue(types.contains(CommandActionType.CUSTOM))
    }

    // ==================== QuantizedElement Tests ====================

    @Test
    fun `QuantizedElement stores basic properties`() {
        val element = QuantizedElement(
            vuid = "a3f2e1-b917cc9dc",
            type = ElementType.BUTTON,
            label = "Submit",
            aliases = listOf("Send", "OK")
        )

        assertEquals("a3f2e1-b917cc9dc", element.vuid)
        assertEquals(ElementType.BUTTON, element.type)
        assertEquals("Submit", element.label)
        assertEquals(2, element.aliases.size)
    }

    @Test
    fun `QuantizedElement default aliases is empty`() {
        val element = QuantizedElement(
            vuid = "test-vuid",
            type = ElementType.TEXT_FIELD,
            label = "Username"
        )

        assertTrue(element.aliases.isEmpty())
    }

    @Test
    fun `QuantizedElement fromElementInfo converts correctly`() {
        val elementInfo = ElementInfo(
            className = "android.widget.Button",
            text = "Login",
            contentDescription = "Sign In Button",
            resourceId = "btn_login",
            bounds = Bounds(0, 0, 100, 50),
            isClickable = true
        )

        val quantized = QuantizedElement.fromElementInfo(elementInfo, "test-vuid")

        assertEquals("test-vuid", quantized.vuid)
        assertEquals(ElementType.BUTTON, quantized.type)
        assertEquals("Login", quantized.label)
        assertTrue(quantized.aliases.contains("Sign In Button"))
    }

    @Test
    fun `QuantizedElement toElmLine generates correct format`() {
        val element = QuantizedElement(
            vuid = "a3f2e1-b917cc9dc",
            type = ElementType.BUTTON,
            label = "Submit",
            bounds = "0,0,100,50",
            actions = "click",
            category = "action"
        )

        val line = element.toElmLine()
        assertTrue(line.startsWith("ELM:"))
        assertTrue(line.contains("a3f2e1-b917cc9dc"))
        assertTrue(line.contains("Submit"))
    }

    // ==================== QuantizedCommand Tests ====================

    @Test
    fun `QuantizedCommand stores basic properties`() {
        val command = QuantizedCommand(
            phrase = "tap submit",
            actionType = CommandActionType.CLICK,
            targetVuid = "a3f2e1-b917cc9dc",
            confidence = 0.95f
        )

        assertEquals("tap submit", command.phrase)
        assertEquals(CommandActionType.CLICK, command.actionType)
        assertEquals("a3f2e1-b917cc9dc", command.targetVuid)
        assertEquals(0.95f, command.confidence)
    }

    @Test
    fun `QuantizedCommand toCmdLine generates correct format`() {
        val command = QuantizedCommand(
            uuid = "cmd-123",
            phrase = "tap submit",
            actionType = CommandActionType.CLICK,
            targetVuid = "a3f2e1-b917cc9dc",
            confidence = 0.95f
        )

        val line = command.toCmdLine()
        assertEquals("CMD:cmd-123:tap submit:CLICK:a3f2e1-b917cc9dc:0.95", line)
    }

    // ==================== QuantizedScreen Tests ====================

    @Test
    fun `QuantizedScreen stores basic properties`() {
        val screen = QuantizedScreen(
            screenHash = "abc123",
            screenTitle = "Login Screen",
            activityName = "com.app.LoginActivity",
            elements = emptyList()
        )

        assertEquals("abc123", screen.screenHash)
        assertEquals("Login Screen", screen.screenTitle)
        assertEquals("com.app.LoginActivity", screen.activityName)
        assertTrue(screen.elements.isEmpty())
    }

    @Test
    fun `QuantizedScreen toScrLine generates correct format`() {
        val screen = QuantizedScreen(
            screenHash = "abc123",
            screenTitle = "Login",
            activityName = "LoginActivity",
            elements = listOf(
                QuantizedElement("v1", ElementType.BUTTON, "OK"),
                QuantizedElement("v2", ElementType.TEXT_FIELD, "Email")
            ),
            timestamp = 1234567890L
        )

        val line = screen.toScrLine()
        assertTrue(line.startsWith("SCR:"))
        assertTrue(line.contains("abc123"))
        assertTrue(line.contains("LoginActivity"))
        assertTrue(line.contains("2")) // element count
    }

    @Test
    fun `QuantizedScreen finds element by label`() {
        val screen = QuantizedScreen(
            screenHash = "test",
            screenTitle = "Test",
            activityName = null,
            elements = listOf(
                QuantizedElement("v1", ElementType.BUTTON, "Submit"),
                QuantizedElement("v2", ElementType.BUTTON, "Cancel")
            )
        )

        val found = screen.findElementByLabel("Submit")
        assertNotNull(found)
        assertEquals("v1", found.vuid)

        val notFound = screen.findElementByLabel("Unknown")
        assertNull(notFound)
    }

    // ==================== QuantizedNavigation Tests ====================

    @Test
    fun `QuantizedNavigation stores basic properties`() {
        val nav = QuantizedNavigation(
            fromScreenHash = "screen1",
            toScreenHash = "screen2",
            triggerLabel = "Next",
            triggerVuid = "btn-next"
        )

        assertEquals("screen1", nav.fromScreenHash)
        assertEquals("screen2", nav.toScreenHash)
        assertEquals("Next", nav.triggerLabel)
        assertEquals("btn-next", nav.triggerVuid)
    }

    @Test
    fun `QuantizedNavigation toNavLine generates correct format`() {
        val nav = QuantizedNavigation(
            fromScreenHash = "screen1",
            toScreenHash = "screen2",
            triggerLabel = "Next",
            triggerVuid = "btn-next",
            timestamp = 1234567890L
        )

        val line = nav.toNavLine()
        assertEquals("NAV:screen1:screen2:btn-next:Next:1234567890", line)
    }

    // ==================== QuantizedContext Tests ====================

    @Test
    fun `QuantizedContext stores basic properties`() {
        val context = QuantizedContext(
            packageName = "com.example.app",
            appName = "Example App",
            versionCode = 10L,
            versionName = "1.0.0",
            generatedAt = getCurrentTimeMillis(),
            screens = emptyList(),
            navigation = emptyList(),
            vocabulary = emptySet(),
            knownCommands = emptyList()
        )

        assertEquals("com.example.app", context.packageName)
        assertEquals("Example App", context.appName)
        assertEquals(10L, context.versionCode)
        assertEquals("1.0.0", context.versionName)
    }

    @Test
    fun `QuantizedContext findScreen returns correct screen`() {
        val screen1 = QuantizedScreen("hash1", "Screen 1", null, emptyList())
        val screen2 = QuantizedScreen("hash2", "Screen 2", null, emptyList())

        val context = QuantizedContext(
            packageName = "com.example",
            appName = "Test",
            versionCode = 1L,
            versionName = "1.0",
            generatedAt = 0L,
            screens = listOf(screen1, screen2),
            navigation = emptyList(),
            vocabulary = emptySet(),
            knownCommands = emptyList()
        )

        val found = context.findScreen("hash1")
        assertNotNull(found)
        assertEquals("Screen 1", found.screenTitle)

        val notFound = context.findScreen("unknown")
        assertNull(notFound)
    }

    @Test
    fun `QuantizedContext getNavigationFrom returns edges from screen`() {
        val nav1 = QuantizedNavigation("A", "B", "Go to B", "btn1")
        val nav2 = QuantizedNavigation("A", "C", "Go to C", "btn2")
        val nav3 = QuantizedNavigation("B", "A", "Back", "btn3")

        val context = QuantizedContext(
            packageName = "com.example",
            appName = "Test",
            versionCode = 1L,
            versionName = "1.0",
            generatedAt = 0L,
            screens = emptyList(),
            navigation = listOf(nav1, nav2, nav3),
            vocabulary = emptySet(),
            knownCommands = emptyList()
        )

        val fromA = context.getNavigationFrom("A")
        assertEquals(2, fromA.size)

        val fromB = context.getNavigationFrom("B")
        assertEquals(1, fromB.size)

        val fromC = context.getNavigationFrom("C")
        assertTrue(fromC.isEmpty())
    }

    @Test
    fun `QuantizedContext findScreensWithElement finds matching screens`() {
        val screen1 = QuantizedScreen(
            "hash1", "Login",
            null,
            listOf(QuantizedElement("v1", ElementType.BUTTON, "Login"))
        )
        val screen2 = QuantizedScreen(
            "hash2", "Home",
            null,
            listOf(QuantizedElement("v2", ElementType.BUTTON, "Settings"))
        )

        val context = QuantizedContext(
            packageName = "com.example",
            appName = "Test",
            versionCode = 1L,
            versionName = "1.0",
            generatedAt = 0L,
            screens = listOf(screen1, screen2),
            navigation = emptyList(),
            vocabulary = emptySet(),
            knownCommands = emptyList()
        )

        val loginScreens = context.findScreensWithElement("Login")
        assertEquals(1, loginScreens.size)
        assertEquals("hash1", loginScreens[0].screenHash)

        val noMatch = context.findScreensWithElement("Unknown")
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `QuantizedContext toAppLine generates correct format`() {
        val context = QuantizedContext(
            packageName = "com.example.app",
            appName = "Example",
            versionCode = 1L,
            versionName = "1.0",
            generatedAt = 1234567890L,
            screens = emptyList(),
            navigation = emptyList(),
            vocabulary = emptySet(),
            knownCommands = emptyList()
        )

        val line = context.toAppLine()
        assertEquals("APP:com.example.app:Example:1234567890", line)
    }

    // ==================== ExplorationStats Tests ====================

    @Test
    fun `ExplorationStats stores basic properties`() {
        val stats = ExplorationStats(
            screenCount = 10,
            elementCount = 150,
            commandCount = 45,
            avgDepth = 3.5f,
            maxDepth = 6,
            coverage = 0.85f,
            durationMs = 120000L
        )

        assertEquals(10, stats.screenCount)
        assertEquals(150, stats.elementCount)
        assertEquals(45, stats.commandCount)
        assertEquals(3.5f, stats.avgDepth)
        assertEquals(6, stats.maxDepth)
        assertEquals(0.85f, stats.coverage)
    }

    @Test
    fun `ExplorationStats toStaLine generates correct format`() {
        val stats = ExplorationStats(
            screenCount = 10,
            elementCount = 150,
            commandCount = 45,
            avgDepth = 3.5f,
            maxDepth = 6,
            coverage = 0.85f,
            durationMs = 120000L
        )

        val line = stats.toStaLine()
        assertEquals("STA:10:150:45:3.50:6:0.85", line)
    }

    @Test
    fun `ExplorationStats fromStaLine parses correctly`() {
        val line = "STA:10:150:45:3.50:6:0.85"
        val stats = ExplorationStats.fromStaLine(line)

        assertNotNull(stats)
        assertEquals(10, stats.screenCount)
        assertEquals(150, stats.elementCount)
        assertEquals(45, stats.commandCount)
        assertEquals(3.5f, stats.avgDepth)
        assertEquals(6, stats.maxDepth)
        assertEquals(0.85f, stats.coverage)
    }

    @Test
    fun `ExplorationStats fromStaLine returns null for invalid line`() {
        assertNull(ExplorationStats.fromStaLine("INVALID:1:2:3"))
        assertNull(ExplorationStats.fromStaLine("STA:invalid"))
        assertNull(ExplorationStats.fromStaLine(""))
    }
}
