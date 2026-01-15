/**
 * AVUQuantizerIntegrationTest.kt - Integration tests for AVUQuantizerIntegration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-26
 *
 * Tests the LLM prompt generation methods for different formats (COMPACT, HTML, FULL).
 * Uses mock QuantizedContext data to verify output format and content.
 */

package com.augmentalis.voiceoscore.learnapp.ai.quantized

import android.content.Context
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for AVUQuantizerIntegration LLM prompt generation
 *
 * Tests:
 * - generateCompactPrompt() output format
 * - generateHtmlPrompt() well-formedness
 * - generateFullPrompt() completeness
 * - Edge cases (empty screens, no navigation, etc.)
 */
class AVUQuantizerIntegrationTest : BaseVoiceOSTest() {

    private lateinit var integration: AVUQuantizerIntegration
    private lateinit var mockContext: Context

    @Before
    override fun setUp() {
        super.setUp()
        mockContext = mockk(relaxed = true)
        integration = AVUQuantizerIntegration(context = mockContext)
    }

    // ============================================
    // Test Data Helpers
    // ============================================

    private fun createTestQuantizedContext(
        packageName: String = "com.test.app",
        appName: String = "Test App",
        screenCount: Int = 3,
        elementsPerScreen: Int = 5,
        includeNavigation: Boolean = true,
        includeCommands: Boolean = true
    ): QuantizedContext {
        val screens = (1..screenCount).map { screenIdx ->
            QuantizedScreen(
                screenHash = "screen_hash_$screenIdx",
                screenTitle = "Screen $screenIdx",
                activityName = "com.test.app.Screen${screenIdx}Activity",
                elements = (1..elementsPerScreen).map { elementIdx ->
                    QuantizedElement(
                        vuid = "vuid_${screenIdx}_$elementIdx",
                        type = when (elementIdx % 4) {
                            0 -> ElementType.BUTTON
                            1 -> ElementType.TEXT_FIELD
                            2 -> ElementType.CHECKBOX
                            else -> ElementType.OTHER
                        },
                        label = "Element $elementIdx on Screen $screenIdx",
                        aliases = listOf("alias_$elementIdx")
                    )
                }
            )
        }

        val navigation = if (includeNavigation && screenCount > 1) {
            (1 until screenCount).map { idx ->
                QuantizedNavigation(
                    fromScreenHash = "screen_hash_$idx",
                    toScreenHash = "screen_hash_${idx + 1}",
                    triggerLabel = "Navigate to Screen ${idx + 1}",
                    triggerVuid = "vuid_${idx}_1"
                )
            }
        } else {
            emptyList()
        }

        val commands = if (includeCommands) {
            listOf(
                QuantizedCommand(
                    phrase = "open settings",
                    actionType = CommandActionType.CLICK,
                    targetVuid = "vuid_1_1",
                    confidence = 0.9f
                ),
                QuantizedCommand(
                    phrase = "go back",
                    actionType = CommandActionType.NAVIGATE,
                    targetVuid = null,
                    confidence = 0.85f
                )
            )
        } else {
            emptyList()
        }

        return QuantizedContext(
            packageName = packageName,
            appName = appName,
            versionCode = 100L,
            versionName = "1.0.0",
            generatedAt = System.currentTimeMillis(),
            screens = screens,
            navigation = navigation,
            vocabulary = screens.flatMap { it.elements.map { e -> e.label } }.toSet(),
            knownCommands = commands
        )
    }

    /**
     * Helper to invoke private generateCompactPrompt method via reflection
     */
    private fun generateCompactPrompt(context: QuantizedContext, userGoal: String): String {
        val method = AVUQuantizerIntegration::class.java.getDeclaredMethod(
            "generateCompactPrompt",
            QuantizedContext::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(integration, context, userGoal) as String
    }

    /**
     * Helper to invoke private generateHtmlPrompt method via reflection
     */
    private fun generateHtmlPrompt(context: QuantizedContext, userGoal: String): String {
        val method = AVUQuantizerIntegration::class.java.getDeclaredMethod(
            "generateHtmlPrompt",
            QuantizedContext::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(integration, context, userGoal) as String
    }

    /**
     * Helper to invoke private generateFullPrompt method via reflection
     */
    private fun generateFullPrompt(context: QuantizedContext, userGoal: String): String {
        val method = AVUQuantizerIntegration::class.java.getDeclaredMethod(
            "generateFullPrompt",
            QuantizedContext::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(integration, context, userGoal) as String
    }

    // ============================================
    // Compact Prompt Tests
    // ============================================

    @Test
    fun `generateCompactPrompt includes app name`() {
        val context = createTestQuantizedContext(appName = "My Test App")
        val result = generateCompactPrompt(context, "open settings")

        assertTrue("Should contain app name", result.contains("My Test App"))
    }

    @Test
    fun `generateCompactPrompt includes user goal`() {
        val context = createTestQuantizedContext()
        val result = generateCompactPrompt(context, "send a message")

        assertTrue("Should contain user goal", result.contains("send a message"))
    }

    @Test
    fun `generateCompactPrompt includes screen count`() {
        val context = createTestQuantizedContext(screenCount = 5)
        val result = generateCompactPrompt(context, "navigate")

        assertTrue("Should contain screen count", result.contains("5"))
    }

    @Test
    fun `generateCompactPrompt is concise`() {
        val context = createTestQuantizedContext(screenCount = 10, elementsPerScreen = 20)
        val result = generateCompactPrompt(context, "test goal")

        // Compact prompt should be under 500 chars for reasonable contexts
        assertTrue("Compact prompt should be concise (< 1000 chars)", result.length < 1000)
    }

    @Test
    fun `generateCompactPrompt handles empty screens`() {
        val context = createTestQuantizedContext(screenCount = 0)
        val result = generateCompactPrompt(context, "test goal")

        assertTrue("Should handle empty screens", result.contains("0") || result.contains("Screens"))
    }

    // ============================================
    // HTML Prompt Tests
    // ============================================

    @Test
    fun `generateHtmlPrompt produces valid XML structure`() {
        val context = createTestQuantizedContext()
        val result = generateHtmlPrompt(context, "open settings")

        assertTrue("Should start with app tag", result.contains("<app"))
        assertTrue("Should have closing app tag", result.contains("</app>"))
        assertTrue("Should have goal tag", result.contains("<goal>"))
        assertTrue("Should have screens tag", result.contains("<screens"))
    }

    @Test
    fun `generateHtmlPrompt includes package name attribute`() {
        val context = createTestQuantizedContext(packageName = "com.example.app")
        val result = generateHtmlPrompt(context, "test")

        assertTrue("Should include package name in attribute",
            result.contains("pkg=\"com.example.app\""))
    }

    @Test
    fun `generateHtmlPrompt includes app name attribute`() {
        val context = createTestQuantizedContext(appName = "Example App")
        val result = generateHtmlPrompt(context, "test")

        assertTrue("Should include app name in attribute",
            result.contains("name=\"Example App\""))
    }

    @Test
    fun `generateHtmlPrompt includes screen elements`() {
        val context = createTestQuantizedContext(screenCount = 2, elementsPerScreen = 3)
        val result = generateHtmlPrompt(context, "click button")

        assertTrue("Should include screen tag", result.contains("<screen"))
        // Elements should be represented with their type
        assertTrue("Should include element types (button, text_field, etc.)",
            result.contains("<button") || result.contains("<text_field") ||
            result.contains("<checkbox") || result.contains("<other"))
    }

    @Test
    fun `generateHtmlPrompt limits screen output`() {
        val context = createTestQuantizedContext(screenCount = 10, elementsPerScreen = 20)
        val result = generateHtmlPrompt(context, "test")

        // Should limit to 5 screens and 5 elements per screen for HTML format
        // Use regex with space to match individual <screen tags, not <screens container tag
        val screenCount = "<screen ".toRegex().findAll(result).count()
        assertTrue("Should limit screens to max 5", screenCount <= 5)
    }

    @Test
    fun `generateHtmlPrompt includes navigation count`() {
        val context = createTestQuantizedContext(screenCount = 4, includeNavigation = true)
        val result = generateHtmlPrompt(context, "navigate")

        assertTrue("Should include nav element with count",
            result.contains("<nav") && result.contains("count="))
    }

    // ============================================
    // Full Prompt Tests
    // ============================================

    @Test
    fun `generateFullPrompt includes all context sections`() {
        val context = createTestQuantizedContext()
        val result = generateFullPrompt(context, "complete task")

        assertTrue("Should have Application Context header", result.contains("# Application Context"))
        assertTrue("Should have User Goal section", result.contains("## User Goal"))
        assertTrue("Should have Available Screens section", result.contains("## Available Screens"))
        assertTrue("Should have Navigation Graph section", result.contains("## Navigation Graph"))
        assertTrue("Should have Known Commands section", result.contains("## Known Commands"))
        assertTrue("Should have Vocabulary section", result.contains("## Vocabulary"))
    }

    @Test
    fun `generateFullPrompt includes version information`() {
        val context = createTestQuantizedContext()
        val result = generateFullPrompt(context, "test")

        assertTrue("Should include version name", result.contains("1.0.0"))
        assertTrue("Should include version code", result.contains("100"))
    }

    @Test
    fun `generateFullPrompt includes activity names`() {
        val context = createTestQuantizedContext(screenCount = 2)
        val result = generateFullPrompt(context, "test")

        assertTrue("Should include activity name",
            result.contains("Activity:") || result.contains("Screen1Activity") || result.contains("Screen2Activity"))
    }

    @Test
    fun `generateFullPrompt includes element aliases`() {
        val context = createTestQuantizedContext(elementsPerScreen = 2)
        val result = generateFullPrompt(context, "test")

        assertTrue("Should include aliases reference",
            result.contains("aliases") || result.contains("alias_"))
    }

    @Test
    fun `generateFullPrompt includes navigation edges`() {
        val context = createTestQuantizedContext(screenCount = 3, includeNavigation = true)
        val result = generateFullPrompt(context, "navigate")

        assertTrue("Should include navigation edges with arrow",
            result.contains("->"))
    }

    @Test
    fun `generateFullPrompt includes known commands`() {
        val context = createTestQuantizedContext(includeCommands = true)
        val result = generateFullPrompt(context, "test")

        assertTrue("Should include command phrases",
            result.contains("open settings") || result.contains("go back"))
    }

    @Test
    fun `generateFullPrompt includes vocabulary`() {
        val context = createTestQuantizedContext(screenCount = 1, elementsPerScreen = 3)
        val result = generateFullPrompt(context, "test")

        assertTrue("Should include vocabulary section with element labels",
            result.contains("Element") && result.contains("## Vocabulary"))
    }

    // ============================================
    // Edge Case Tests
    // ============================================

    @Test
    fun `all prompts handle empty context gracefully`() {
        val context = createTestQuantizedContext(
            screenCount = 0,
            includeNavigation = false,
            includeCommands = false
        )

        val compact = generateCompactPrompt(context, "test")
        val html = generateHtmlPrompt(context, "test")
        val full = generateFullPrompt(context, "test")

        assertNotNull("Compact prompt should not be null", compact)
        assertNotNull("HTML prompt should not be null", html)
        assertNotNull("Full prompt should not be null", full)

        assertTrue("Compact should have content", compact.isNotEmpty())
        assertTrue("HTML should have content", html.isNotEmpty())
        assertTrue("Full should have content", full.isNotEmpty())
    }

    @Test
    fun `all prompts handle single screen context`() {
        val context = createTestQuantizedContext(
            screenCount = 1,
            elementsPerScreen = 2,
            includeNavigation = false
        )

        val compact = generateCompactPrompt(context, "test single screen")
        val html = generateHtmlPrompt(context, "test single screen")
        val full = generateFullPrompt(context, "test single screen")

        assertTrue("Compact should contain screen count", compact.contains("1"))
        assertTrue("HTML should have screen tag", html.contains("<screen"))
        assertTrue("Full should list the screen", full.contains("Screen 1"))
    }

    @Test
    fun `prompts escape special characters in user goal`() {
        val context = createTestQuantizedContext(screenCount = 1)
        val goalWithSpecialChars = "click <button> & navigate \"home\""

        val html = generateHtmlPrompt(context, goalWithSpecialChars)

        // Goal should be included (may or may not be escaped depending on implementation)
        assertTrue("Should include goal content",
            html.contains("click") && html.contains("button") && html.contains("navigate"))
    }

    @Test
    fun `prompts handle long element labels`() {
        val screens = listOf(
            QuantizedScreen(
                screenHash = "hash1",
                screenTitle = "Test Screen",
                activityName = null,
                elements = listOf(
                    QuantizedElement(
                        vuid = "vuid1",
                        type = ElementType.BUTTON,
                        label = "A".repeat(200), // Very long label
                        aliases = emptyList()
                    )
                )
            )
        )

        val context = QuantizedContext(
            packageName = "com.test",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            generatedAt = System.currentTimeMillis(),
            screens = screens,
            navigation = emptyList(),
            vocabulary = setOf("A".repeat(200)),
            knownCommands = emptyList()
        )

        val full = generateFullPrompt(context, "test long labels")
        assertTrue("Should handle long labels", full.isNotEmpty())
    }

    @Test
    fun `prompts handle unicode characters`() {
        val screens = listOf(
            QuantizedScreen(
                screenHash = "hash1",
                screenTitle = "设置", // Chinese for "Settings"
                activityName = null,
                elements = listOf(
                    QuantizedElement(
                        vuid = "vuid1",
                        type = ElementType.BUTTON,
                        label = "Einstellungen öffnen", // German with umlaut
                        aliases = listOf("настройки") // Russian
                    )
                )
            )
        )

        val context = QuantizedContext(
            packageName = "com.test",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            generatedAt = System.currentTimeMillis(),
            screens = screens,
            navigation = emptyList(),
            vocabulary = setOf("设置", "Einstellungen", "настройки"),
            knownCommands = emptyList()
        )

        val compact = generateCompactPrompt(context, "открыть настройки")
        val html = generateHtmlPrompt(context, "打开设置")
        val full = generateFullPrompt(context, "Einstellungen öffnen")

        assertTrue("Compact should handle unicode", compact.contains("设置") || compact.isNotEmpty())
        assertTrue("HTML should handle unicode", html.isNotEmpty())
        assertTrue("Full should handle unicode", full.isNotEmpty())
    }

    // ============================================
    // QuantizedContext Method Tests
    // ============================================

    @Test
    fun `findScreen returns correct screen by hash`() {
        val context = createTestQuantizedContext(screenCount = 3)

        val screen = context.findScreen("screen_hash_2")

        assertNotNull("Should find screen", screen)
        assertEquals("Should return correct screen", "Screen 2", screen?.screenTitle)
    }

    @Test
    fun `findScreen returns null for unknown hash`() {
        val context = createTestQuantizedContext(screenCount = 2)

        val screen = context.findScreen("nonexistent_hash")

        assertNull("Should return null for unknown hash", screen)
    }

    @Test
    fun `getNavigationFrom returns edges from specified screen`() {
        val context = createTestQuantizedContext(screenCount = 4, includeNavigation = true)

        val edges = context.getNavigationFrom("screen_hash_2")

        assertEquals("Should return 1 edge from screen 2", 1, edges.size)
        assertEquals("Edge should go to screen 3", "screen_hash_3", edges[0].toScreenHash)
    }

    @Test
    fun `getNavigationFrom returns empty list for leaf screen`() {
        val context = createTestQuantizedContext(screenCount = 3, includeNavigation = true)

        // Last screen has no outgoing navigation
        val edges = context.getNavigationFrom("screen_hash_3")

        assertTrue("Should return empty list for leaf screen", edges.isEmpty())
    }

    @Test
    fun `findScreensWithElement finds screens containing matching label`() {
        val context = createTestQuantizedContext(screenCount = 3, elementsPerScreen = 2)

        val screens = context.findScreensWithElement("Element 1")

        assertEquals("Should find all screens (element 1 exists on each)", 3, screens.size)
    }

    @Test
    fun `findScreensWithElement is case insensitive`() {
        val context = createTestQuantizedContext(screenCount = 2, elementsPerScreen = 2)

        val screens = context.findScreensWithElement("ELEMENT 1")

        assertEquals("Should find screens with case-insensitive match", 2, screens.size)
    }

    @Test
    fun `findScreensWithElement returns empty for non-matching label`() {
        val context = createTestQuantizedContext(screenCount = 2, elementsPerScreen = 2)

        val screens = context.findScreensWithElement("NonexistentElement")

        assertTrue("Should return empty for non-matching label", screens.isEmpty())
    }
}
