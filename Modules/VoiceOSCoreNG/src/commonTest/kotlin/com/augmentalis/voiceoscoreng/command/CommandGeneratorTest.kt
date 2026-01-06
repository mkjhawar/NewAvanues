package com.augmentalis.voiceoscoreng.command

import com.augmentalis.voiceoscoreng.avu.CommandActionType
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandGeneratorTest {

    @Test
    fun `fromElement generates click command for clickable button`() {
        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.app:id/submit_btn",
            packageName = "com.app"
        )

        val command = CommandGenerator.fromElement(element, "com.app")

        assertNotNull(command)
        assertEquals("click Submit", command.phrase)
        assertEquals(CommandActionType.CLICK, command.actionType)
        assertTrue(command.confidence > 0.5f)
    }

    @Test
    fun `fromElement generates click command for button with content description`() {
        val element = ElementInfo(
            className = "Button",
            contentDescription = "Send message",
            isClickable = true,
            packageName = "com.app"
        )

        val command = CommandGenerator.fromElement(element, "com.app")

        assertNotNull(command)
        assertEquals("click Send message", command.phrase)
        assertEquals(CommandActionType.CLICK, command.actionType)
    }

    @Test
    fun `fromElement returns null for non-actionable elements`() {
        val element = ElementInfo(
            className = "TextView",
            text = "Static text",
            isClickable = false,
            isScrollable = false,
            packageName = "com.app"
        )

        val command = CommandGenerator.fromElement(element, "com.app")

        assertNull(command)
    }

    @Test
    fun `fromElement returns null for elements without voice content`() {
        val element = ElementInfo(
            className = "Button",
            isClickable = true,
            packageName = "com.app"
            // No text, contentDescription, or resourceId
        )

        val command = CommandGenerator.fromElement(element, "com.app")

        assertNull(command)
    }

    @Test
    fun `fromElement generates type command for EditText`() {
        val element = ElementInfo.input(
            hint = "Enter email",
            resourceId = "com.app:id/email_field",
            packageName = "com.app"
        )

        val command = CommandGenerator.fromElement(element, "com.app")

        assertNotNull(command)
        assertEquals("type Enter email", command.phrase)
        assertEquals(CommandActionType.TYPE, command.actionType)
    }

    @Test
    fun `fromElement uses resourceId when text and contentDescription empty`() {
        val element = ElementInfo(
            className = "Button",
            resourceId = "com.app:id/save_button",
            isClickable = true,
            packageName = "com.app"
        )

        val command = CommandGenerator.fromElement(element, "com.app")

        assertNotNull(command)
        assertEquals("click save button", command.phrase)
    }

    @Test
    fun `fromElement confidence higher with more identifiers`() {
        val elementWithAll = ElementInfo(
            className = "Button",
            text = "Submit",
            contentDescription = "Submit form",
            resourceId = "com.app:id/submit_btn",
            isClickable = true,
            packageName = "com.app"
        )

        val elementMinimal = ElementInfo(
            className = "Button",
            text = "OK",
            isClickable = true,
            packageName = "com.app"
        )

        val commandFull = CommandGenerator.fromElement(elementWithAll, "com.app")
        val commandMinimal = CommandGenerator.fromElement(elementMinimal, "com.app")

        assertNotNull(commandFull)
        assertNotNull(commandMinimal)
        assertTrue(commandFull.confidence > commandMinimal.confidence)
    }

    @Test
    fun `fromElement skips elements where label is just class name`() {
        val element = ElementInfo(
            className = "android.widget.Button",
            isClickable = true,
            packageName = "com.app"
        )

        val command = CommandGenerator.fromElement(element, "com.app")

        // Should return null because voiceLabel would default to "Button" which is just the class name
        assertNull(command)
    }

    @Test
    fun `fromElement generates VUID when not provided`() {
        val element = ElementInfo.button(
            text = "Test",
            packageName = "com.app"
        )

        val command = CommandGenerator.fromElement(element, "com.app")

        assertNotNull(command)
        assertNotNull(command.targetVuid)
        assertTrue(command.targetVuid!!.isNotBlank())
    }

    @Test
    fun `fromElement confidence capped at 1_0`() {
        val element = ElementInfo(
            className = "Button",
            text = "Submit",
            contentDescription = "Submit the form now",
            resourceId = "com.app:id/submit_btn",
            isClickable = true,
            isEnabled = true,
            packageName = "com.app",
            bounds = Bounds(0, 0, 100, 50)
        )

        val command = CommandGenerator.fromElement(element, "com.app")

        assertNotNull(command)
        assertTrue(command.confidence <= 1.0f)
    }
}
