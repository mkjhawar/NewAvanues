/**
 * QuantizedCommandTest.kt - Unit tests for QuantizedCommand
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */
package com.augmentalis.voiceoscore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QuantizedCommandTest {

    // ==================== Basic Construction Tests ====================

    @Test
    fun constructor_createsValidCommand() {
        val command = QuantizedCommand(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = "elem-456",
            confidence = 0.95f
        )

        assertEquals("cmd-123", command.avid)
        assertEquals("click submit", command.phrase)
        assertEquals(CommandActionType.CLICK, command.actionType)
        assertEquals("elem-456", command.targetAvid)
        assertEquals(0.95f, command.confidence)
    }

    @Test
    fun constructor_defaultAvid() {
        val command = QuantizedCommand(
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = null,
            confidence = 1.0f
        )

        assertEquals("", command.avid)
    }

    @Test
    fun constructor_nullableTargetAvid() {
        val command = QuantizedCommand(
            avid = "cmd-123",
            phrase = "go back",
            actionType = CommandActionType.BACK,
            targetAvid = null,
            confidence = 1.0f
        )

        assertNull(command.targetAvid)
    }

    // ==================== Factory Method Tests ====================

    @Test
    fun create_withRequiredFields() {
        val command = QuantizedCommand.create(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            packageName = "com.test.app"
        )

        assertEquals("cmd-123", command.avid)
        assertEquals("click submit", command.phrase)
        assertEquals("com.test.app", command.packageName)
    }

    @Test
    fun create_withAllOptionalFields() {
        val command = QuantizedCommand.create(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            packageName = "com.test.app",
            targetAvid = "elem-456",
            confidence = 0.9f,
            screenId = "screen-main",
            appVersion = "1.0.0"
        )

        assertEquals("elem-456", command.targetAvid)
        assertEquals(0.9f, command.confidence)
        assertEquals("screen-main", command.screenId)
        assertEquals("1.0.0", command.appVersion)
    }

    // ==================== Action Type Tests ====================

    @Test
    fun actionType_clickType() {
        val command = QuantizedCommand(
            phrase = "click button",
            actionType = CommandActionType.CLICK,
            targetAvid = "btn-1",
            confidence = 1.0f
        )

        assertEquals(CommandActionType.CLICK, command.actionType)
    }

    @Test
    fun actionType_longClickType() {
        val command = QuantizedCommand(
            phrase = "long press button",
            actionType = CommandActionType.LONG_CLICK,
            targetAvid = "btn-1",
            confidence = 1.0f
        )

        assertEquals(CommandActionType.LONG_CLICK, command.actionType)
    }

    @Test
    fun actionType_typeAction() {
        val command = QuantizedCommand(
            phrase = "type hello",
            actionType = CommandActionType.TYPE,
            targetAvid = "txt-1",
            confidence = 1.0f
        )

        assertEquals(CommandActionType.TYPE, command.actionType)
    }

    @Test
    fun actionType_scrollAction() {
        val command = QuantizedCommand(
            phrase = "scroll down",
            actionType = CommandActionType.SCROLL,
            targetAvid = null,
            confidence = 1.0f
        )

        assertEquals(CommandActionType.SCROLL, command.actionType)
    }

    // ==================== Metadata Tests ====================

    @Test
    fun metadata_withMetadataAddsEntry() {
        val command = QuantizedCommand(
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = null,
            confidence = 1.0f
        )

        val updated = command.withMetadata("key", "value")

        assertEquals("value", updated.metadata["key"])
        assertTrue(command.metadata.isEmpty()) // Original unchanged
    }

    @Test
    fun metadata_withMetadataMultiple() {
        val command = QuantizedCommand(
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = null,
            confidence = 1.0f
        )

        val updated = command.withMetadata(mapOf("key1" to "val1", "key2" to "val2"))

        assertEquals("val1", updated.metadata["key1"])
        assertEquals("val2", updated.metadata["key2"])
    }

    // ==================== CMD Line Format Tests ====================

    @Test
    fun toCmdLine_producesCorrectFormat() {
        val command = QuantizedCommand(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = "elem-456",
            confidence = 0.95f
        )

        val line = command.toCmdLine()

        assertTrue(line.startsWith("CMD:"))
        assertTrue(line.contains("cmd-123"))
        assertTrue(line.contains("click submit"))
        assertTrue(line.contains("CLICK"))
    }

    @Test
    fun fromCmdLine_parsesValidLine() {
        val line = "CMD:cmd-123:click submit:CLICK:elem-456:0.95"

        val command = QuantizedCommand.fromCmdLine(line)

        assertNotNull(command)
        assertEquals("cmd-123", command.avid)
        assertEquals("click submit", command.phrase)
        assertEquals(CommandActionType.CLICK, command.actionType)
    }

    @Test
    fun fromCmdLine_invalidLineReturnsNull() {
        val command = QuantizedCommand.fromCmdLine("invalid line")
        assertNull(command)
    }

    @Test
    fun fromCmdLine_withPackageName() {
        val line = "CMD:cmd-123:click submit:CLICK:elem-456:0.95"

        val command = QuantizedCommand.fromCmdLine(line, "com.test.app")

        assertNotNull(command)
        assertEquals("com.test.app", command.packageName)
    }

    // ==================== Validation Tests ====================

    @Test
    fun isValid_withPackageName() {
        val command = QuantizedCommand.create(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            packageName = "com.test.app"
        )

        assertTrue(command.isValid())
    }

    @Test
    fun isValid_withoutPackageName() {
        val command = QuantizedCommand(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = null,
            confidence = 1.0f
        )

        assertFalse(command.isValid())
    }

    @Test
    fun validationErrors_emptyWhenValid() {
        val command = QuantizedCommand.create(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            packageName = "com.test.app"
        )

        assertTrue(command.validationErrors().isEmpty())
    }

    // ==================== Equality Tests ====================

    @Test
    fun equals_sameValues_areEqual() {
        val command1 = QuantizedCommand(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = "elem-456",
            confidence = 0.9f
        )

        val command2 = QuantizedCommand(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = "elem-456",
            confidence = 0.9f
        )

        assertEquals(command1, command2)
    }

    @Test
    fun equals_differentAvid_notEqual() {
        val command1 = QuantizedCommand(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = null,
            confidence = 1.0f
        )

        val command2 = QuantizedCommand(
            avid = "cmd-456",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = null,
            confidence = 1.0f
        )

        assertFalse(command1 == command2)
    }

    // ==================== Copy Tests ====================

    @Test
    fun copy_createsModifiedCopy() {
        val original = QuantizedCommand(
            avid = "cmd-123",
            phrase = "click submit",
            actionType = CommandActionType.CLICK,
            targetAvid = null,
            confidence = 0.8f
        )

        val modified = original.copy(confidence = 0.95f)

        assertEquals("cmd-123", modified.avid)
        assertEquals("click submit", modified.phrase)
        assertEquals(0.95f, modified.confidence)
    }
}
