/**
 * AndroidWiringSimulationTest.kt - Simulation test for Android wiring
 *
 * Verifies that the new Android wiring components work together correctly
 * by simulating the flow without requiring a real AccessibilityService.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 */
package com.augmentalis.voiceoscore

import org.junit.Test
import org.junit.Assert.*

/**
 * Simulation test for Android wiring integration.
 *
 * Tests the flow:
 * 1. AccessibilityNodeAdapter converts nodes to ElementInfo
 * 2. CommandGenerator creates commands from ElementInfo
 * 3. ActionCoordinator receives and stores commands
 *
 * Note: This test simulates the flow without real Android components.
 */
class AndroidWiringSimulationTest {

    /**
     * Test 1: Verify ElementInfo can be created with all properties.
     */
    @Test
    fun `ElementInfo creation preserves all properties`() {
        val element = ElementInfo(
            className = "android.widget.Button",
            resourceId = "com.test:id/submit_btn",
            text = "Submit",
            contentDescription = "Submit form",
            bounds = Bounds(0, 0, 100, 50),
            isClickable = true,
            isScrollable = false,
            isEnabled = true,
            packageName = "com.test.app",
            listIndex = -1,
            isInDynamicContainer = false
        )

        assertEquals("android.widget.Button", element.className)
        assertEquals("Submit", element.text)
        assertTrue(element.isClickable)
        assertTrue(element.hasVoiceContent)
        assertTrue(element.isActionable)
        assertEquals("Submit", element.voiceLabel)
    }

    /**
     * Test 2: Verify CommandGenerator creates valid commands.
     */
    @Test
    fun `CommandGenerator creates command from ElementInfo`() {
        val element = ElementInfo(
            className = "android.widget.Button",
            resourceId = "com.test:id/login_btn",
            text = "Login",
            contentDescription = "",
            bounds = Bounds(10, 20, 110, 70),
            isClickable = true,
            isScrollable = false,
            isEnabled = true,
            packageName = "com.test.app"
        )

        val command = CommandGenerator.fromElement(element, "com.test.app")

        assertNotNull(command)
        assertEquals("login", command!!.phrase.lowercase())
        assertEquals(CommandActionType.CLICK, command.actionType)
        assertTrue(command.confidence > 0.5f)
    }

    /**
     * Test 3: Verify list index commands are generated.
     */
    @Test
    fun `CommandGenerator creates list index commands`() {
        val listItems = listOf(
            ElementInfo(
                className = "android.widget.TextView",
                text = "Item A",
                bounds = Bounds(0, 0, 100, 50),
                isClickable = true,
                packageName = "com.test.app",
                listIndex = 0,
                isInDynamicContainer = true
            ),
            ElementInfo(
                className = "android.widget.TextView",
                text = "Item B",
                bounds = Bounds(0, 50, 100, 100),
                isClickable = true,
                packageName = "com.test.app",
                listIndex = 1,
                isInDynamicContainer = true
            )
        )

        val commands = CommandGenerator.generateListIndexCommands(listItems, "com.test.app")

        assertTrue(commands.isNotEmpty())
        // Should have ordinal commands like "first", "second"
        val phrases = commands.map { it.phrase.lowercase() }
        assertTrue(phrases.any { it.contains("first") || it == "1" })
    }

    /**
     * Test 4: Verify dynamic content detection.
     */
    @Test
    fun `ElementInfo detects dynamic content`() {
        val dynamicElement = ElementInfo(
            className = "android.widget.TextView",
            text = "Unread, , , Arby's, , BOGO Deal",
            bounds = Bounds(0, 0, 300, 80),
            isClickable = true,
            packageName = "com.google.android.gm",
            isInDynamicContainer = true
        )

        assertTrue(dynamicElement.isDynamicContent)
        assertFalse(dynamicElement.shouldPersist)

        val staticElement = ElementInfo(
            className = "android.widget.Button",
            text = "Settings",
            bounds = Bounds(0, 0, 100, 50),
            isClickable = true,
            packageName = "com.android.settings",
            isInDynamicContainer = false
        )

        assertFalse(staticElement.isDynamicContent)
        assertTrue(staticElement.shouldPersist)
    }

    /**
     * Test 5: Verify AVID fingerprint generation.
     */
    @Test
    fun `ElementFingerprint generates deterministic AVID`() {
        val avid1 = ElementFingerprint.generate(
            className = "Button",
            packageName = "com.test",
            resourceId = "btn_submit",
            text = "Submit"
        )

        val avid2 = ElementFingerprint.generate(
            className = "Button",
            packageName = "com.test",
            resourceId = "btn_submit",
            text = "Submit"
        )

        // Same input should produce same AVID
        assertEquals(avid1, avid2)

        // Different input should produce different AVID
        val avid3 = ElementFingerprint.generate(
            className = "Button",
            packageName = "com.test",
            resourceId = "btn_cancel",
            text = "Cancel"
        )

        assertNotEquals(avid1, avid3)
    }

    /**
     * Test 6: Verify screen fingerprinting.
     */
    @Test
    fun `ScreenFingerprinter creates consistent hashes`() {
        val elements = listOf(
            ElementInfo(
                className = "Button",
                text = "OK",
                bounds = Bounds(0, 0, 100, 50),
                isClickable = true,
                packageName = "com.test"
            ),
            ElementInfo(
                className = "Button",
                text = "Cancel",
                bounds = Bounds(100, 0, 200, 50),
                isClickable = true,
                packageName = "com.test"
            )
        )

        val fingerprinter = ScreenFingerprinter()
        val hash1 = fingerprinter.calculateFingerprint(elements)
        val hash2 = fingerprinter.calculateFingerprint(elements)

        // Same elements should produce same hash
        assertEquals(hash1, hash2)
        assertTrue(hash1.length == 64) // SHA-256 produces 64 hex chars
    }

    /**
     * Test 7: Verify Bounds calculations.
     */
    @Test
    fun `Bounds calculates center correctly`() {
        val bounds = Bounds(left = 100, top = 200, right = 300, bottom = 400)

        assertEquals(200, bounds.width)
        assertEquals(200, bounds.height)
        assertEquals(200, bounds.centerX)
        assertEquals(300, bounds.centerY)
    }

    /**
     * Test 8: Simulate full flow from element to command.
     */
    @Test
    fun `Full flow simulation - element extraction to command generation`() {
        // Simulate elements extracted from screen
        val extractedElements = listOf(
            ElementInfo(
                className = "android.widget.Button",
                resourceId = "com.app:id/btn_submit",
                text = "Submit",
                bounds = Bounds(50, 100, 150, 150),
                isClickable = true,
                isEnabled = true,
                packageName = "com.example.app"
            ),
            ElementInfo(
                className = "android.widget.EditText",
                resourceId = "com.app:id/input_name",
                contentDescription = "Enter your name",
                bounds = Bounds(50, 50, 250, 90),
                isClickable = true,
                isEnabled = true,
                packageName = "com.example.app"
            ),
            ElementInfo(
                className = "android.widget.TextView",
                text = "Email from John",
                bounds = Bounds(0, 200, 300, 250),
                isClickable = true,
                packageName = "com.example.app",
                listIndex = 0,
                isInDynamicContainer = true
            )
        )

        // Generate commands
        val commands = mutableListOf<QuantizedCommand>()
        extractedElements.forEach { element ->
            CommandGenerator.fromElement(element, element.packageName)?.let {
                commands.add(it)
            }
        }

        // Verify commands were generated
        assertTrue(commands.isNotEmpty())
        println("Generated ${commands.size} commands:")
        commands.forEach { cmd ->
            println("  - '${cmd.phrase}' -> ${cmd.actionType}")
        }

        // Verify static vs dynamic
        val submitCmd = commands.find { it.phrase.lowercase() == "submit" }
        assertNotNull("Should have Submit command", submitCmd)

        // Verify list commands
        val listElements = extractedElements.filter { it.listIndex >= 0 }
        val listCommands = CommandGenerator.generateListIndexCommands(listElements, "com.example.app")
        println("Generated ${listCommands.size} list index commands")
    }
}
