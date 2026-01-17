/**
 * CommandGeneratorUtilsTest.kt - Unit tests for CommandGeneratorUtils
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */
package com.augmentalis.voiceoscore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandGeneratorUtilsTest {

    // ==================== generateTrigger Tests ====================

    @Test
    fun generateTrigger_navigationCategory_usesGoToVerb() {
        val trigger = CommandGeneratorUtils.generateTrigger("settings", ElementCategory.NAVIGATION)
        assertEquals("go to settings", trigger)
    }

    @Test
    fun generateTrigger_actionCategory_usesClickVerb() {
        val trigger = CommandGeneratorUtils.generateTrigger("submit", ElementCategory.ACTION)
        assertEquals("click submit", trigger)
    }

    @Test
    fun generateTrigger_inputCategory_usesTypeInVerb() {
        val trigger = CommandGeneratorUtils.generateTrigger("username", ElementCategory.INPUT, "type")
        assertEquals("type in username", trigger)
    }

    @Test
    fun generateTrigger_removesSpecialCharacters() {
        val trigger = CommandGeneratorUtils.generateTrigger("Save & Exit!", ElementCategory.ACTION)
        assertEquals("click save  exit", trigger)
    }

    @Test
    fun generateTrigger_convertsToLowercase() {
        val trigger = CommandGeneratorUtils.generateTrigger("SETTINGS", ElementCategory.NAVIGATION)
        assertEquals("go to settings", trigger)
    }

    // ==================== generateSynonyms Tests ====================

    @Test
    fun generateSynonyms_homeLabel_generatesCorrectSynonyms() {
        val synonymSet = CommandGeneratorUtils.generateSynonyms("go to home")
        assertTrue(synonymSet.synonyms.isNotEmpty())
        assertTrue(synonymSet.synonyms.contains("go to main"))
    }

    @Test
    fun generateSynonyms_settingsLabel_generatesCorrectSynonyms() {
        val synonymSet = CommandGeneratorUtils.generateSynonyms("open settings")
        assertTrue(synonymSet.synonyms.contains("open preferences"))
        assertTrue(synonymSet.synonyms.contains("open options"))
    }

    @Test
    fun generateSynonyms_unknownWord_returnsEmptySynonyms() {
        val synonymSet = CommandGeneratorUtils.generateSynonyms("click foobar123")
        assertTrue(synonymSet.synonyms.isEmpty())
    }

    // ==================== inferCategory Tests ====================

    @Test
    fun inferCategory_button_returnsAction() {
        val category = CommandGeneratorUtils.inferCategory(ElementType.BUTTON)
        assertEquals(ElementCategory.ACTION, category)
    }

    @Test
    fun inferCategory_textField_returnsInput() {
        val category = CommandGeneratorUtils.inferCategory(ElementType.TEXT_FIELD)
        assertEquals(ElementCategory.INPUT, category)
    }

    @Test
    fun inferCategory_tab_returnsNavigation() {
        val category = CommandGeneratorUtils.inferCategory(ElementType.TAB)
        assertEquals(ElementCategory.NAVIGATION, category)
    }

    @Test
    fun inferCategory_menu_returnsMenu() {
        val category = CommandGeneratorUtils.inferCategory(ElementType.MENU)
        assertEquals(ElementCategory.MENU, category)
    }

    @Test
    fun inferCategory_text_returnsDisplay() {
        val category = CommandGeneratorUtils.inferCategory(ElementType.TEXT)
        assertEquals(ElementCategory.DISPLAY, category)
    }

    // ==================== fromElement Tests ====================

    @Test
    fun fromElement_validElement_generatesCommand() {
        val element = QuantizedElement(
            vuid = "test-vuid-123",
            label = "Submit",
            type = ElementType.BUTTON,
            actions = "click",
            bounds = "0,0,100,50",
            aliases = emptyList()
        )

        val command = CommandGeneratorUtils.fromElement(element, "com.test.app")

        assertNotNull(command)
        assertEquals("test-vuid-123", command.elementHash)
        assertEquals("click submit", command.commandText)
        assertEquals("click", command.actionType)
        assertTrue(command.confidence > 0.5)
    }

    @Test
    fun fromElement_blankLabel_returnsNull() {
        val element = QuantizedElement(
            vuid = "test-vuid-123",
            label = "",
            type = ElementType.BUTTON,
            actions = "click",
            bounds = "0,0,100,50",
            aliases = emptyList()
        )

        val command = CommandGeneratorUtils.fromElement(element, "com.test.app")
        assertNull(command)
    }

    @Test
    fun fromElement_unlabeledElement_returnsNull() {
        val element = QuantizedElement(
            vuid = "test-vuid-123",
            label = "unlabeled",
            type = ElementType.BUTTON,
            actions = "click",
            bounds = "0,0,100,50",
            aliases = emptyList()
        )

        val command = CommandGeneratorUtils.fromElement(element, "com.test.app")
        assertNull(command)
    }

    @Test
    fun fromElement_noActions_returnsNull() {
        val element = QuantizedElement(
            vuid = "test-vuid-123",
            label = "Label",
            type = ElementType.TEXT,
            actions = "",
            bounds = "0,0,100,50",
            aliases = emptyList()
        )

        val command = CommandGeneratorUtils.fromElement(element, "com.test.app")
        assertNull(command)
    }

    @Test
    fun fromElement_textFieldElement_usesTypeAction() {
        val element = QuantizedElement(
            vuid = "test-vuid-123",
            label = "Username",
            type = ElementType.TEXT_FIELD,
            actions = "click,edit",
            bounds = "0,0,100,50",
            aliases = emptyList()
        )

        val command = CommandGeneratorUtils.fromElement(element, "com.test.app")

        assertNotNull(command)
        assertEquals("type", command.actionType)
    }

    // ==================== validateCommands Tests ====================

    @Test
    fun validateCommands_validCommand_passesValidation() {
        val commands = listOf(
            GeneratedCommand(
                elementHash = "hash1",
                commandText = "click submit",
                actionType = "click",
                confidence = 0.8
            )
        )

        val validated = CommandGeneratorUtils.validateCommands(commands)
        assertEquals(1, validated.size)
    }

    @Test
    fun validateCommands_shortTrigger_filtered() {
        val commands = listOf(
            GeneratedCommand(
                elementHash = "hash1",
                commandText = "x",
                actionType = "click",
                confidence = 0.8
            )
        )

        val validated = CommandGeneratorUtils.validateCommands(commands)
        assertTrue(validated.isEmpty())
    }

    @Test
    fun validateCommands_lowConfidence_filtered() {
        val commands = listOf(
            GeneratedCommand(
                elementHash = "hash1",
                commandText = "click submit",
                actionType = "click",
                confidence = 0.1
            )
        )

        val validated = CommandGeneratorUtils.validateCommands(commands)
        assertTrue(validated.isEmpty())
    }

    @Test
    fun validateCommands_invalidAction_filtered() {
        val commands = listOf(
            GeneratedCommand(
                elementHash = "hash1",
                commandText = "click submit",
                actionType = "invalid_action",
                confidence = 0.8
            )
        )

        val validated = CommandGeneratorUtils.validateCommands(commands)
        assertTrue(validated.isEmpty())
    }

    // ==================== deduplicateCommands Tests ====================

    @Test
    fun deduplicateCommands_removeDuplicates() {
        val commands = listOf(
            GeneratedCommand(elementHash = "hash1", commandText = "click submit", actionType = "click"),
            GeneratedCommand(elementHash = "hash2", commandText = "Click Submit", actionType = "click"),
            GeneratedCommand(elementHash = "hash3", commandText = "click cancel", actionType = "click")
        )

        val deduplicated = CommandGeneratorUtils.deduplicateCommands(commands)
        assertEquals(2, deduplicated.size)
    }

    @Test
    fun deduplicateCommands_preservesFirst() {
        val commands = listOf(
            GeneratedCommand(elementHash = "hash1", commandText = "click submit", actionType = "click"),
            GeneratedCommand(elementHash = "hash2", commandText = "click submit", actionType = "long_click")
        )

        val deduplicated = CommandGeneratorUtils.deduplicateCommands(commands)
        assertEquals(1, deduplicated.size)
        assertEquals("hash1", deduplicated[0].elementHash)
    }

    // ==================== toDbFormat Tests ====================

    @Test
    fun toDbFormat_producesCorrectMap() {
        val command = GeneratedCommand(
            elementHash = "hash1",
            commandText = "click submit",
            actionType = "click",
            confidence = 0.85,
            synonyms = "[\"tap submit\"]"
        )

        val map = CommandGeneratorUtils.toDbFormat(command, "com.test.app")

        assertEquals("hash1", map["element_hash"])
        assertEquals("click submit", map["command_text"])
        assertEquals("click", map["action_type"])
        assertEquals(0.85, map["confidence"])
        assertEquals("com.test.app", map["package_name"])
        assertTrue(map.containsKey("created_at"))
    }
}
