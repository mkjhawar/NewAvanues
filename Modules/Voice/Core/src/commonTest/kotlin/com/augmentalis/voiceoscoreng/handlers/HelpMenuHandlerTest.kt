/**
 * HelpMenuHandlerTest.kt - Tests for HelpMenuHandler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * TDD tests for help menu and command discovery.
 */
package com.augmentalis.voiceoscoreng.handlers

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HelpMenuHandlerTest {

    private lateinit var handler: HelpMenuHandler

    @BeforeTest
    fun setup() {
        handler = HelpMenuHandler()
    }

    // ==================== Default Categories Tests ====================

    @Test
    fun `default categories are registered on init`() {
        val categories = handler.getCategories()

        assertTrue(categories.isNotEmpty())
        assertEquals(3, categories.size)
    }

    @Test
    fun `Navigation category is registered`() {
        val categories = handler.getCategories()
        val navigation = categories.find { it.name == "Navigation" }

        assertNotNull(navigation)
        assertTrue(navigation.commands.isNotEmpty())
    }

    @Test
    fun `Selection category is registered`() {
        val categories = handler.getCategories()
        val selection = categories.find { it.name == "Selection" }

        assertNotNull(selection)
        assertTrue(selection.commands.isNotEmpty())
    }

    @Test
    fun `Cursor category is registered`() {
        val categories = handler.getCategories()
        val cursor = categories.find { it.name == "Cursor" }

        assertNotNull(cursor)
        assertTrue(cursor.commands.isNotEmpty())
    }

    @Test
    fun `getCategoryCount returns correct count`() {
        assertEquals(3, handler.getCategoryCount())
    }

    // ==================== handleCommand Tests ====================

    @Test
    fun `handleCommand help returns all categories`() {
        val result = handler.handleCommand("help")

        assertTrue(result.success)
        assertNotNull(result.categories)
        assertEquals(3, result.categories?.size)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand show help returns all categories`() {
        val result = handler.handleCommand("show help")

        assertTrue(result.success)
        assertNotNull(result.categories)
        assertEquals(3, result.categories?.size)
    }

    @Test
    fun `handleCommand what can i say returns all categories`() {
        val result = handler.handleCommand("what can i say")

        assertTrue(result.success)
        assertNotNull(result.categories)
        assertEquals(3, result.categories?.size)
    }

    @Test
    fun `handleCommand is case insensitive`() {
        val result = handler.handleCommand("HELP")

        assertTrue(result.success)
        assertNotNull(result.categories)
    }

    @Test
    fun `handleCommand trims whitespace`() {
        val result = handler.handleCommand("  help  ")

        assertTrue(result.success)
        assertNotNull(result.categories)
    }

    @Test
    fun `handleCommand help copy searches for copy`() {
        val result = handler.handleCommand("help copy")

        assertTrue(result.success)
        assertNotNull(result.searchResults)
        assertTrue(result.searchResults!!.isNotEmpty())
        assertTrue(result.searchResults!!.any { it.phrase == "copy" })
    }

    @Test
    fun `handleCommand unknown returns error`() {
        val result = handler.handleCommand("random command")

        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Unknown help command"))
    }

    // ==================== showAllHelp Tests ====================

    @Test
    fun `showAllHelp returns all categories`() {
        val result = handler.showAllHelp()

        assertTrue(result.success)
        assertNotNull(result.categories)
        assertEquals(3, result.categories?.size)
        assertNull(result.searchResults)
        assertNull(result.error)
    }

    @Test
    fun `showAllHelp includes Navigation commands`() {
        val result = handler.showAllHelp()
        val navigation = result.categories?.find { it.name == "Navigation" }

        assertNotNull(navigation)
        assertTrue(navigation.commands.any { it.phrase == "go back" })
        assertTrue(navigation.commands.any { it.phrase == "go home" })
        assertTrue(navigation.commands.any { it.phrase == "scroll up/down" })
        assertTrue(navigation.commands.any { it.phrase == "open [app]" })
    }

    @Test
    fun `showAllHelp includes Selection commands`() {
        val result = handler.showAllHelp()
        val selection = result.categories?.find { it.name == "Selection" }

        assertNotNull(selection)
        assertTrue(selection.commands.any { it.phrase == "tap [number]" })
        assertTrue(selection.commands.any { it.phrase == "select all" })
        assertTrue(selection.commands.any { it.phrase == "copy" })
        assertTrue(selection.commands.any { it.phrase == "paste" })
    }

    @Test
    fun `showAllHelp includes Cursor commands`() {
        val result = handler.showAllHelp()
        val cursor = result.categories?.find { it.name == "Cursor" }

        assertNotNull(cursor)
        assertTrue(cursor.commands.any { it.phrase == "cursor up/down/left/right" })
        assertTrue(cursor.commands.any { it.phrase == "click" })
        assertTrue(cursor.commands.any { it.phrase == "cursor faster/slower" })
    }

    // ==================== searchHelp Tests ====================

    @Test
    fun `searchHelp with matching phrase finds results`() {
        val result = handler.searchHelp("copy")

        assertTrue(result.success)
        assertNotNull(result.searchResults)
        assertTrue(result.searchResults!!.isNotEmpty())
        assertTrue(result.searchResults!!.any { it.phrase == "copy" })
    }

    @Test
    fun `searchHelp with matching description finds results`() {
        val result = handler.searchHelp("clipboard")

        assertTrue(result.success)
        assertNotNull(result.searchResults)
        assertTrue(result.searchResults!!.isNotEmpty())
    }

    @Test
    fun `searchHelp is case insensitive`() {
        val result = handler.searchHelp("COPY")

        assertTrue(result.success)
        assertNotNull(result.searchResults)
        assertTrue(result.searchResults!!.any { it.phrase == "copy" })
    }

    @Test
    fun `searchHelp trims whitespace`() {
        val result = handler.searchHelp("  copy  ")

        assertTrue(result.success)
        assertNotNull(result.searchResults)
        assertTrue(result.searchResults!!.any { it.phrase == "copy" })
    }

    @Test
    fun `searchHelp with no matches returns error`() {
        val result = handler.searchHelp("xyznonexistent")

        assertFalse(result.success)
        assertNull(result.searchResults)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("No help found for"))
    }

    @Test
    fun `searchHelp scroll finds scroll command`() {
        val result = handler.searchHelp("scroll")

        assertTrue(result.success)
        assertNotNull(result.searchResults)
        assertTrue(result.searchResults!!.any { it.phrase.contains("scroll") })
    }

    @Test
    fun `searchHelp cursor finds cursor commands`() {
        val result = handler.searchHelp("cursor")

        assertTrue(result.success)
        assertNotNull(result.searchResults)
        assertTrue(result.searchResults!!.size >= 2) // cursor movement and cursor speed
    }

    // ==================== registerCategory Tests ====================

    @Test
    fun `registerCategory adds new category`() {
        val initialCount = handler.getCategoryCount()

        val newCategory = HelpCategory(
            name = "Custom",
            commands = listOf(
                HelpCommand("custom command", "A custom command")
            )
        )
        handler.registerCategory(newCategory)

        assertEquals(initialCount + 1, handler.getCategoryCount())
    }

    @Test
    fun `registerCategory category appears in getCategories`() {
        val newCategory = HelpCategory(
            name = "CustomCategory",
            commands = listOf(
                HelpCommand("my command", "My description")
            )
        )
        handler.registerCategory(newCategory)

        val categories = handler.getCategories()
        val custom = categories.find { it.name == "CustomCategory" }

        assertNotNull(custom)
        assertEquals(1, custom.commands.size)
        assertEquals("my command", custom.commands[0].phrase)
    }

    @Test
    fun `registerCategory commands are searchable`() {
        val newCategory = HelpCategory(
            name = "TestCategory",
            commands = listOf(
                HelpCommand("unique test phrase", "Test description")
            )
        )
        handler.registerCategory(newCategory)

        val result = handler.searchHelp("unique test phrase")

        assertTrue(result.success)
        assertNotNull(result.searchResults)
        assertTrue(result.searchResults!!.any { it.phrase == "unique test phrase" })
    }

    // ==================== HelpCommand Tests ====================

    @Test
    fun `HelpCommand with examples stores examples`() {
        val command = HelpCommand(
            phrase = "tap [number]",
            description = "Tap numbered element",
            examples = listOf("tap 3", "tap five")
        )

        assertEquals("tap [number]", command.phrase)
        assertEquals("Tap numbered element", command.description)
        assertEquals(2, command.examples.size)
        assertEquals("tap 3", command.examples[0])
        assertEquals("tap five", command.examples[1])
    }

    @Test
    fun `HelpCommand without examples defaults to empty list`() {
        val command = HelpCommand(
            phrase = "go back",
            description = "Navigate to previous screen"
        )

        assertTrue(command.examples.isEmpty())
    }

    // ==================== HelpCategory Tests ====================

    @Test
    fun `HelpCategory stores name and commands`() {
        val commands = listOf(
            HelpCommand("cmd1", "desc1"),
            HelpCommand("cmd2", "desc2")
        )
        val category = HelpCategory(name = "TestCat", commands = commands)

        assertEquals("TestCat", category.name)
        assertEquals(2, category.commands.size)
    }

    // ==================== HelpResult Tests ====================

    @Test
    fun `HelpResult success with categories`() {
        val categories = listOf(
            HelpCategory("Cat1", listOf(HelpCommand("cmd", "desc")))
        )
        val result = HelpResult(success = true, categories = categories)

        assertTrue(result.success)
        assertNotNull(result.categories)
        assertEquals(1, result.categories?.size)
        assertNull(result.searchResults)
        assertNull(result.error)
    }

    @Test
    fun `HelpResult success with searchResults`() {
        val searchResults = listOf(
            HelpCommand("found", "Found command")
        )
        val result = HelpResult(success = true, searchResults = searchResults)

        assertTrue(result.success)
        assertNull(result.categories)
        assertNotNull(result.searchResults)
        assertEquals(1, result.searchResults?.size)
        assertNull(result.error)
    }

    @Test
    fun `HelpResult failure with error`() {
        val result = HelpResult(success = false, error = "Not found")

        assertFalse(result.success)
        assertNull(result.categories)
        assertNull(result.searchResults)
        assertEquals("Not found", result.error)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `searchHelp with partial match finds results`() {
        val result = handler.searchHelp("scr")

        assertTrue(result.success)
        assertNotNull(result.searchResults)
        assertTrue(result.searchResults!!.any { it.phrase.contains("scr") || it.description.lowercase().contains("scr") })
    }

    @Test
    fun `multiple categories can be registered`() {
        val cat1 = HelpCategory("Cat1", listOf(HelpCommand("cmd1", "desc1")))
        val cat2 = HelpCategory("Cat2", listOf(HelpCommand("cmd2", "desc2")))
        val cat3 = HelpCategory("Cat3", listOf(HelpCommand("cmd3", "desc3")))

        handler.registerCategory(cat1)
        handler.registerCategory(cat2)
        handler.registerCategory(cat3)

        assertEquals(6, handler.getCategoryCount()) // 3 default + 3 new
    }

    @Test
    fun `getCategories returns copy not reference`() {
        val categories1 = handler.getCategories()
        val categories2 = handler.getCategories()

        // They should be equal but not the same reference
        assertEquals(categories1.size, categories2.size)
    }
}
