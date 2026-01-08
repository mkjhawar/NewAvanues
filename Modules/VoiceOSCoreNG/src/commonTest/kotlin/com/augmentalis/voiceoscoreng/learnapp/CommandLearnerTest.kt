package com.augmentalis.voiceoscoreng.learnapp

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandLearnerTest {

    // ==================== learnCommand Tests ====================

    @Test
    fun `learnCommand creates LearnedCommand with correct phrase from text`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Submit",
            isClickable = true
        )

        val command = learner.learnCommand(element, "vuid-123")

        assertNotNull(command)
        assertEquals("submit", command.phrase)
        assertEquals("vuid-123", command.targetVuid)
        assertEquals("tap", command.action)
        assertEquals(1.0f, command.confidence)
    }

    @Test
    fun `learnCommand creates LearnedCommand with correct phrase from contentDescription`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "ImageButton",
            contentDescription = "Open Settings",
            isClickable = true
        )

        val command = learner.learnCommand(element, "vuid-456")

        assertNotNull(command)
        assertEquals("open settings", command.phrase)
        assertEquals("vuid-456", command.targetVuid)
    }

    @Test
    fun `learnCommand returns null for elements without text or description`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "View",
            text = "",
            contentDescription = ""
        )

        val command = learner.learnCommand(element, "vuid-789")

        assertNull(command)
    }

    @Test
    fun `learnCommand returns null for blank text and description`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "View",
            text = "   ",
            contentDescription = "   "
        )

        val command = learner.learnCommand(element, "vuid-000")

        assertNull(command)
    }

    @Test
    fun `learnCommand determines tap action for clickable element`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Click Me",
            isClickable = true
        )

        val command = learner.learnCommand(element, "vuid-tap")

        assertNotNull(command)
        assertEquals("tap", command.action)
    }

    @Test
    fun `learnCommand determines scroll action for scrollable element`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "RecyclerView",
            text = "Scroll Area",
            isScrollable = true
        )

        val command = learner.learnCommand(element, "vuid-scroll")

        assertNotNull(command)
        assertEquals("scroll", command.action)
    }

    @Test
    fun `learnCommand determines long_press action for long clickable element`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Hold Me",
            isLongClickable = true,
            isClickable = true
        )

        val command = learner.learnCommand(element, "vuid-long")

        assertNotNull(command)
        assertEquals("long_press", command.action)
    }

    @Test
    fun `learnCommand prioritizes scroll over long_press`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "View",
            text = "Complex Element",
            isScrollable = true,
            isLongClickable = true
        )

        val command = learner.learnCommand(element, "vuid-priority")

        assertNotNull(command)
        assertEquals("scroll", command.action)
    }

    @Test
    fun `learnCommand generates aliases from contentDescription`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Settings",
            contentDescription = "Open App Settings",
            isClickable = true
        )

        val command = learner.learnCommand(element, "vuid-alias")

        assertNotNull(command)
        assertTrue(command.aliases.contains("open app settings"))
    }

    @Test
    fun `learnCommand generates aliases from suggestAliases`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Settings",
            isClickable = true
        )

        val command = learner.learnCommand(element, "vuid-suggest")

        assertNotNull(command)
        assertTrue(command.aliases.any { it in listOf("preferences", "options", "config") })
    }

    // ==================== findCommand Tests ====================

    @Test
    fun `findCommand finds by primary phrase`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Submit",
            isClickable = true
        )
        learner.learnCommand(element, "vuid-find")

        val found = learner.findCommand("submit")

        assertNotNull(found)
        assertEquals("vuid-find", found.targetVuid)
    }

    @Test
    fun `findCommand finds by phrase case insensitively`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Submit",
            isClickable = true
        )
        learner.learnCommand(element, "vuid-case")

        val found = learner.findCommand("SUBMIT")

        assertNotNull(found)
        assertEquals("vuid-case", found.targetVuid)
    }

    @Test
    fun `findCommand finds by alias`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Settings",
            isClickable = true
        )
        learner.learnCommand(element, "vuid-alias-find")

        // "preferences" is an alias for "settings"
        val found = learner.findCommand("preferences")

        assertNotNull(found)
        assertEquals("vuid-alias-find", found.targetVuid)
    }

    @Test
    fun `findCommand returns null for unknown phrase`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Submit",
            isClickable = true
        )
        learner.learnCommand(element, "vuid-unknown")

        val found = learner.findCommand("nonexistent")

        assertNull(found)
    }

    // ==================== getAllCommands Tests ====================

    @Test
    fun `getAllCommands returns distinct commands`() {
        val learner = CommandLearner()

        val element1 = ElementInfo(className = "Button", text = "First", isClickable = true)
        val element2 = ElementInfo(className = "Button", text = "Second", isClickable = true)
        val element3 = ElementInfo(className = "Button", text = "Third", isClickable = true)

        learner.learnCommand(element1, "vuid-1")
        learner.learnCommand(element2, "vuid-2")
        learner.learnCommand(element3, "vuid-3")

        val all = learner.getAllCommands()

        assertEquals(3, all.size)
        assertTrue(all.any { it.targetVuid == "vuid-1" })
        assertTrue(all.any { it.targetVuid == "vuid-2" })
        assertTrue(all.any { it.targetVuid == "vuid-3" })
    }

    @Test
    fun `getAllCommands returns empty list when no commands`() {
        val learner = CommandLearner()

        val all = learner.getAllCommands()

        assertTrue(all.isEmpty())
    }

    // ==================== removeCommand Tests ====================

    @Test
    fun `removeCommand removes command and aliases`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Settings",
            contentDescription = "App Settings",
            isClickable = true
        )
        learner.learnCommand(element, "vuid-remove")

        val removed = learner.removeCommand("vuid-remove")

        assertTrue(removed)
        assertNull(learner.findCommand("settings"))
        assertNull(learner.findCommand("app settings"))
        assertNull(learner.findCommand("preferences"))
    }

    @Test
    fun `removeCommand returns false for non-existent vuid`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Keep",
            isClickable = true
        )
        learner.learnCommand(element, "vuid-keep")

        val removed = learner.removeCommand("vuid-nonexistent")

        assertFalse(removed)
        assertNotNull(learner.findCommand("keep"))
    }

    // ==================== generateVoiceLabels Tests ====================

    @Test
    fun `generateVoiceLabels includes text`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Click Me"
        )

        val labels = learner.generateVoiceLabels(element)

        assertTrue(labels.contains("click me"))
    }

    @Test
    fun `generateVoiceLabels includes contentDescription`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "ImageView",
            contentDescription = "Profile Picture"
        )

        val labels = learner.generateVoiceLabels(element)

        assertTrue(labels.contains("profile picture"))
    }

    @Test
    fun `generateVoiceLabels includes className derivative`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "android.widget.Button"
        )

        val labels = learner.generateVoiceLabels(element)

        assertTrue(labels.contains("button"))
    }

    @Test
    fun `generateVoiceLabels excludes view as className derivative`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "android.view.View"
        )

        val labels = learner.generateVoiceLabels(element)

        assertFalse(labels.contains("view"))
    }

    @Test
    fun `generateVoiceLabels returns distinct labels`() {
        val learner = CommandLearner()
        val element = ElementInfo(
            className = "Button",
            text = "Button",
            contentDescription = "Button"
        )

        val labels = learner.generateVoiceLabels(element)

        // All same text, should be deduplicated
        assertEquals(1, labels.count { it == "button" })
    }

    // ==================== suggestAliases Tests ====================

    @Test
    fun `suggestAliases returns substitutions for settings`() {
        val learner = CommandLearner()

        val aliases = learner.suggestAliases("settings")

        assertTrue(aliases.contains("preferences"))
        assertTrue(aliases.contains("options"))
        assertTrue(aliases.contains("config"))
    }

    @Test
    fun `suggestAliases returns substitutions for search`() {
        val learner = CommandLearner()

        val aliases = learner.suggestAliases("search")

        assertTrue(aliases.contains("find"))
        assertTrue(aliases.contains("lookup"))
    }

    @Test
    fun `suggestAliases returns substitutions for delete`() {
        val learner = CommandLearner()

        val aliases = learner.suggestAliases("Delete")

        assertTrue(aliases.contains("remove"))
        assertTrue(aliases.contains("trash"))
    }

    @Test
    fun `suggestAliases returns empty for unknown label`() {
        val learner = CommandLearner()

        val aliases = learner.suggestAliases("unknown_label_xyz")

        assertTrue(aliases.isEmpty())
    }

    @Test
    fun `suggestAliases is case insensitive`() {
        val learner = CommandLearner()

        val aliases = learner.suggestAliases("SETTINGS")

        assertTrue(aliases.contains("preferences"))
    }

    // ==================== getCommandCount Tests ====================

    @Test
    fun `getCommandCount returns correct count`() {
        val learner = CommandLearner()

        assertEquals(0, learner.getCommandCount())

        val element1 = ElementInfo(className = "Button", text = "One", isClickable = true)
        val element2 = ElementInfo(className = "Button", text = "Two", isClickable = true)

        learner.learnCommand(element1, "vuid-count-1")
        learner.learnCommand(element2, "vuid-count-2")

        assertEquals(2, learner.getCommandCount())
    }

    @Test
    fun `getCommandCount counts by distinct vuid not by phrase count`() {
        val learner = CommandLearner()
        // Element with text "Settings" will also register aliases
        val element = ElementInfo(
            className = "Button",
            text = "Settings",
            contentDescription = "Open Settings",
            isClickable = true
        )

        learner.learnCommand(element, "vuid-single")

        // Despite multiple phrases/aliases mapping to same command, count should be 1
        assertEquals(1, learner.getCommandCount())
    }

    // ==================== clear Tests ====================

    @Test
    fun `clear removes all commands`() {
        val learner = CommandLearner()

        val element1 = ElementInfo(className = "Button", text = "One", isClickable = true)
        val element2 = ElementInfo(className = "Button", text = "Two", isClickable = true)

        learner.learnCommand(element1, "vuid-clear-1")
        learner.learnCommand(element2, "vuid-clear-2")

        assertEquals(2, learner.getCommandCount())

        learner.clear()

        assertEquals(0, learner.getCommandCount())
        assertNull(learner.findCommand("one"))
        assertNull(learner.findCommand("two"))
    }

    // ==================== LearnedCommand Data Class Tests ====================

    @Test
    fun `LearnedCommand has correct default values`() {
        val command = LearnedCommand(
            phrase = "test",
            targetVuid = "vuid-test",
            action = "tap"
        )

        assertEquals("test", command.phrase)
        assertEquals("vuid-test", command.targetVuid)
        assertEquals("tap", command.action)
        assertEquals(1.0f, command.confidence)
        assertTrue(command.aliases.isEmpty())
        assertTrue(command.createdAt > 0)
    }

    @Test
    fun `LearnedCommand can be created with custom values`() {
        val aliases = listOf("alias1", "alias2")
        val command = LearnedCommand(
            phrase = "custom",
            targetVuid = "vuid-custom",
            action = "long_press",
            confidence = 0.8f,
            aliases = aliases,
            createdAt = 1000L
        )

        assertEquals("custom", command.phrase)
        assertEquals("vuid-custom", command.targetVuid)
        assertEquals("long_press", command.action)
        assertEquals(0.8f, command.confidence)
        assertEquals(aliases, command.aliases)
        assertEquals(1000L, command.createdAt)
    }
}
