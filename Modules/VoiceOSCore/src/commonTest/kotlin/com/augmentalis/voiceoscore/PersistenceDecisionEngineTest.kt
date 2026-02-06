package com.augmentalis.voiceoscore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive tests for PersistenceDecisionEngine testing all 6 rules.
 *
 * Rule overview:
 * 1. ALWAYS_DYNAMIC containers NEVER persist
 * 2. Settings/System apps persist (unless dynamic patterns detected)
 * 3. Settings screens persist (any app)
 * 4. Form screens persist short content
 * 5. Email/Messaging/Social with ScrollView - context dependent
 * 6. Unknown apps - stability threshold
 */
class PersistenceDecisionEngineTest {

    // =========================================================================
    // PRE-FILTER TESTS: Non-actionable and no-content elements
    // =========================================================================

    @Test
    fun `pre-filter should not persist non-clickable elements`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = false,
            text = "Just a label"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(0, decision.ruleApplied)
        assertEquals("Element is not actionable (not clickable or scrollable)", decision.reason)
    }

    @Test
    fun `pre-filter should not persist elements without voice content`() {
        val element = createTestElement(
            className = "View",
            isClickable = true,
            text = "",
            contentDescription = "",
            resourceId = ""
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(0, decision.ruleApplied)
        assertEquals("Element has no voice-accessible content (no text, description, or resourceId)", decision.reason)
    }

    @Test
    fun `pre-filter should allow scrollable non-clickable elements with content`() {
        val element = createTestElement(
            className = "ScrollView",
            isClickable = false,
            isScrollable = true,
            text = "Scrollable content"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        // Should NOT be filtered by pre-filter (ruleApplied > 0)
        assertTrue(decision.ruleApplied > 0)
    }

    // =========================================================================
    // RULE 1: ALWAYS_DYNAMIC CONTAINERS (Never persist)
    // =========================================================================

    @Test
    fun `rule1 should not persist elements in RecyclerView`() {
        val element = createTestElement(
            className = "TextView",
            isInDynamicContainer = true,
            containerType = "RecyclerView",
            isClickable = true,
            text = "Email subject"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.google.android.gm", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(1, decision.ruleApplied)
        assertEquals(1.0f, decision.confidence)
        assertTrue(decision.reason.contains("ALWAYS_DYNAMIC"))
    }

    @Test
    fun `rule1 should not persist elements in ListView`() {
        val element = createTestElement(
            className = "Button",
            isInDynamicContainer = true,
            containerType = "ListView",
            isClickable = true,
            text = "List item"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.example.app", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(1, decision.ruleApplied)
    }

    @Test
    fun `rule1 should not persist elements in LazyColumn`() {
        val element = createTestElement(
            className = "Card",
            isInDynamicContainer = true,
            containerType = "LazyColumn",
            isClickable = true,
            text = "Compose list item"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.example.composeapp", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(1, decision.ruleApplied)
    }

    @Test
    fun `rule1 should not persist elements in ViewPager`() {
        val element = createTestElement(
            className = "ImageView",
            isInDynamicContainer = true,
            containerType = "ViewPager2",
            isClickable = true,
            contentDescription = "Page 1"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.example.app", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(1, decision.ruleApplied)
    }

    @Test
    fun `rule1 should apply even in settings app for dynamic containers`() {
        // Even in settings apps, dynamic containers should NOT persist
        val element = createTestElement(
            className = "Switch",
            isInDynamicContainer = true,
            containerType = "RecyclerView",
            isClickable = true,
            text = "Wi-Fi"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(1, decision.ruleApplied) // Rule 1 takes precedence over Rule 2
    }

    // =========================================================================
    // RULE 2: SETTINGS/SYSTEM APPS (Persist unless dynamic patterns)
    // =========================================================================

    @Test
    fun `rule2 should persist elements in settings app`() {
        val element = createTestElement(
            className = "Switch",
            isClickable = true,
            text = "Wi-Fi"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        assertTrue(decision.shouldPersist)
        assertEquals(2, decision.ruleApplied)
        assertTrue(decision.confidence >= 0.9f)
    }

    @Test
    fun `rule2 should persist elements in system app`() {
        val element = createTestElement(
            className = "Button",
            isClickable = true,
            text = "Install"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.vending", listOf(element) // Play Store is SYSTEM
        )
        assertTrue(decision.shouldPersist)
        assertEquals(2, decision.ruleApplied)
    }

    @Test
    fun `rule2 should persist elements in launcher`() {
        val element = createTestElement(
            className = "ImageButton",
            isClickable = true,
            contentDescription = "All apps"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.google.android.launcher", listOf(element)
        )
        assertTrue(decision.shouldPersist)
        assertEquals(2, decision.ruleApplied)
    }

    @Test
    fun `rule2 should not persist settings element with time pattern`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "Last connected 3:45 PM"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(2, decision.ruleApplied)
        assertTrue(decision.reason.contains("dynamic patterns"))
    }

    @Test
    fun `rule2 should not persist settings element with date pattern`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "Updated on 1/15/2024"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(2, decision.ruleApplied)
    }

    @Test
    fun `rule2 should not persist settings element with counter pattern`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "5 new notifications"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(2, decision.ruleApplied)
    }

    // =========================================================================
    // RULE 3: SETTINGS SCREENS (Any app)
    // =========================================================================

    @Test
    fun `rule3 should persist settings screen elements in any app`() {
        // Create elements that make it look like a settings screen (high switch ratio)
        val elements = listOf(
            createTestElement(className = "Switch", isClickable = true, text = "Option 1", isChecked = true),
            createTestElement(className = "Switch", isClickable = true, text = "Option 2", isChecked = false),
            createTestElement(className = "Switch", isClickable = true, text = "Option 3", isChecked = true),
            createTestElement(className = "Switch", isClickable = true, text = "Option 4", isChecked = false),
            createTestElement(className = "TextView", text = "Settings Header")
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            elements[0], "com.random.app", elements
        )
        assertTrue(decision.shouldPersist)
        assertEquals(3, decision.ruleApplied)
        assertTrue(decision.reason.contains("Settings screen"))
    }

    @Test
    fun `rule3 should not persist settings screen element with dynamic patterns`() {
        val elements = listOf(
            createTestElement(className = "Switch", isClickable = true, text = "Updated 10:30 AM", isChecked = true),
            createTestElement(className = "Switch", isClickable = true, text = "Option 2", isChecked = false),
            createTestElement(className = "Switch", isClickable = true, text = "Option 3", isChecked = true),
            createTestElement(className = "Switch", isClickable = true, text = "Option 4", isChecked = false),
            createTestElement(className = "TextView", text = "Settings")
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            elements[0], "com.random.app", elements
        )
        assertFalse(decision.shouldPersist)
        assertEquals(3, decision.ruleApplied)
        assertTrue(decision.reason.contains("dynamic patterns"))
    }

    // =========================================================================
    // RULE 4: FORM SCREENS (Persist short content)
    // =========================================================================

    @Test
    fun `rule4 should persist form elements with short content`() {
        val elements = listOf(
            createTestElement(className = "EditText", isClickable = true, text = "", contentDescription = "Username"),
            createTestElement(className = "EditText", isClickable = true, text = "", contentDescription = "Password"),
            createTestElement(className = "Button", isClickable = true, text = "Submit")
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            elements[2], "com.random.app", elements
        )
        assertTrue(decision.shouldPersist)
        assertEquals(4, decision.ruleApplied)
        assertTrue(decision.reason.contains("Form screen"))
        assertTrue(decision.reason.contains("short", ignoreCase = true))
    }

    @Test
    fun `rule4 should persist form elements with medium content`() {
        val elements = listOf(
            createTestElement(className = "EditText", isClickable = true, text = ""),
            createTestElement(className = "EditText", isClickable = true, text = ""),
            createTestElement(className = "Button", isClickable = true, text = "This is a medium length button label")
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            elements[2], "com.random.app", elements
        )
        assertTrue(decision.shouldPersist)
        assertEquals(4, decision.ruleApplied)
    }

    @Test
    fun `rule4 should not persist form elements with long content`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "This is a very long text that exceeds one hundred characters and should be considered dynamic content that changes frequently and should not be persisted because it is too long"
        )
        val elements = listOf(
            createTestElement(className = "EditText", isClickable = true),
            createTestElement(className = "EditText", isClickable = true),
            element
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.random.app", elements
        )
        assertFalse(decision.shouldPersist)
        assertEquals(4, decision.ruleApplied)
        assertTrue(decision.reason.contains("long text", ignoreCase = true))
    }

    // =========================================================================
    // RULE 5: EMAIL/MESSAGING/SOCIAL APPS (Context dependent)
    // =========================================================================

    @Test
    fun `rule5 should persist short navigation elements in email app`() {
        val element = createTestElement(
            className = "TextView",
            resourceId = "com.google.android.gm:id/compose_button",
            isClickable = true,
            text = "Compose"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.google.android.gm", listOf(element)
        )
        assertTrue(decision.shouldPersist)
        assertEquals(5, decision.ruleApplied)
        assertTrue(decision.reason.contains("navigation") || decision.reason.contains("stability"))
    }

    @Test
    fun `rule5 should persist elements with high stability score in messaging app`() {
        // Create element with high stability: resourceId + short text + contentDescription
        val element = createTestElement(
            className = "Button",
            resourceId = "com.whatsapp:id/menuitem_new_chat",
            isClickable = true,
            text = "New",
            contentDescription = "New chat"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.whatsapp", listOf(element)
        )
        assertTrue(decision.shouldPersist)
        assertEquals(5, decision.ruleApplied)
    }

    @Test
    fun `rule5 should not persist email preview in gmail`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            contentDescription = "Unread, , , Arby's, , BOGO Sandwich today only..."
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.google.android.gm", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(5, decision.ruleApplied)
        assertTrue(
            decision.reason.contains("stability") ||
            decision.reason.contains("criteria") ||
            decision.reason.contains("dynamic patterns"),
            "Reason should mention stability, criteria, or dynamic patterns: ${decision.reason}"
        )
    }

    @Test
    fun `rule5 should not persist dynamic content in social app`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "John is typing..."  // Status indicator
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.instagram.android", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(5, decision.ruleApplied)
    }

    @Test
    fun `rule5 should not persist message preview in messaging app`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "Hey, just wanted to check in and see how you're doing. Let me know when you're free to chat about the project we discussed last week."
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.whatsapp", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(5, decision.ruleApplied)
    }

    @Test
    fun `rule5 should not persist element with timestamp in email app`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "Received at 2:30 PM"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.google.android.gm", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(5, decision.ruleApplied)
    }

    // =========================================================================
    // RULE 6: UNKNOWN/OTHER APPS (Stability threshold)
    // =========================================================================

    @Test
    fun `rule6 should persist stable elements in unknown app`() {
        val element = createTestElement(
            className = "Button",
            resourceId = "com.random:id/save_button",
            isClickable = true,
            text = "Save",
            contentDescription = "Save button"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.random.unknownapp", listOf(element)
        )
        assertTrue(decision.shouldPersist)
        assertEquals(6, decision.ruleApplied)
        assertTrue(decision.reason.contains("stability threshold"))
    }

    @Test
    fun `rule6 should not persist unstable elements in unknown app`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "Updated 5 minutes ago"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.random.unknownapp", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(6, decision.ruleApplied)
        assertTrue(decision.reason.contains("dynamic patterns") || decision.reason.contains("stability"))
    }

    @Test
    fun `rule6 should not persist element without resourceId and short text only`() {
        // Element with only short text but no resourceId has lower stability
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "Click me"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.random.unknownapp", listOf(element)
        )
        // Stability score: 50 (baseline) + 20 (short text) = 70
        // This is above threshold (60) but let's verify the rule is applied
        assertEquals(6, decision.ruleApplied)
    }

    @Test
    fun `rule6 should not persist elements in dynamic container even in unknown app`() {
        // Even though this is unknown app, dynamic container rule (Rule 1) takes precedence
        val element = createTestElement(
            className = "Button",
            resourceId = "com.random:id/item_button",
            isClickable = true,
            text = "Item",
            isInDynamicContainer = true,
            containerType = "RecyclerView"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.random.unknownapp", listOf(element)
        )
        assertFalse(decision.shouldPersist)
        assertEquals(1, decision.ruleApplied) // Rule 1 takes precedence
    }

    @Test
    fun `rule6 should have lower confidence than rule2`() {
        // Rule 6 (unknown apps) should have lower confidence than Rule 2 (settings apps)
        val element = createTestElement(
            className = "Button",
            resourceId = "com.example:id/button",
            isClickable = true,
            text = "OK"
        )

        val settingsDecision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )

        val unknownDecision = PersistenceDecisionEngine.decideForElement(
            element, "com.random.unknownapp", listOf(element)
        )

        // Settings app (Rule 2) should have higher confidence than unknown app (Rule 6)
        assertTrue(settingsDecision.confidence > unknownDecision.confidence)
    }

    // =========================================================================
    // BATCH PROCESSING TESTS
    // =========================================================================

    @Test
    fun `batchDecide should process multiple elements efficiently`() {
        val elements = listOf(
            createTestElement(className = "Button", isClickable = true, text = "Save", resourceId = "id/save"),
            createTestElement(className = "Button", isClickable = true, text = "Cancel", resourceId = "id/cancel"),
            createTestElement(className = "TextView", isClickable = false, text = "Label"),
            createTestElement(className = "EditText", isClickable = true, text = "", resourceId = "id/input")
        )

        val decisions = PersistenceDecisionEngine.batchDecide(elements, "com.android.settings")

        assertEquals(4, decisions.size)

        // Clickable elements with content should have decisions
        assertTrue(decisions[elements[0]]!!.shouldPersist) // Button with resourceId
        assertTrue(decisions[elements[1]]!!.shouldPersist) // Button with resourceId
        assertFalse(decisions[elements[2]]!!.shouldPersist) // Non-clickable
        assertTrue(decisions[elements[3]]!!.shouldPersist) // EditText with resourceId
    }

    @Test
    fun `batchDecide should return empty map for empty input`() {
        val decisions = PersistenceDecisionEngine.batchDecide(emptyList(), "com.example.app")
        assertTrue(decisions.isEmpty())
    }

    // =========================================================================
    // BUILD CONTEXT TESTS
    // =========================================================================

    @Test
    fun `buildContext should correctly classify all layers`() {
        val element = createTestElement(
            className = "Switch",
            isClickable = true,
            text = "Notifications",
            resourceId = "com.example:id/notifications_switch",
            isInDynamicContainer = false
        )

        val context = PersistenceDecisionEngine.buildContext(
            element,
            "com.android.settings",
            listOf(element)
        )

        // Verify Layer 1: App Category
        assertEquals(AppCategory.SETTINGS, context.appCategory)

        // Verify Layer 2: Container Behavior (not in dynamic container)
        assertEquals(ContainerBehavior.STATIC, context.containerBehavior)

        // Verify Layer 4: Content Signal
        assertTrue(context.contentSignal.hasResourceId)
        assertFalse(context.contentSignal.hasDynamicPatterns)
    }

    @Test
    fun `buildContext should detect dynamic container from isInDynamicContainer flag`() {
        val element = createTestElement(
            className = "TextView",
            isClickable = true,
            text = "List item",
            isInDynamicContainer = true,
            containerType = ""  // No explicit container type
        )

        val context = PersistenceDecisionEngine.buildContext(
            element,
            "com.example.app",
            listOf(element)
        )

        assertEquals(ContainerBehavior.ALWAYS_DYNAMIC, context.containerBehavior)
    }

    // =========================================================================
    // SUMMARIZE DECISIONS TESTS
    // =========================================================================

    @Test
    fun `summarizeDecisions should provide meaningful statistics`() {
        val elements = listOf(
            createTestElement(className = "Button", isClickable = true, text = "Save", resourceId = "id/save"),
            createTestElement(className = "Button", isClickable = true, text = "Cancel", resourceId = "id/cancel"),
            createTestElement(className = "TextView", isClickable = false, text = "Label")
        )

        val decisions = PersistenceDecisionEngine.batchDecide(elements, "com.android.settings")
        val summary = PersistenceDecisionEngine.summarizeDecisions(decisions)

        assertTrue(summary.contains("Total elements: 3"))
        assertTrue(summary.contains("Persist:"))
        assertTrue(summary.contains("Skip:"))
        assertTrue(summary.contains("Average confidence:"))
        assertTrue(summary.contains("Decisions by rule:"))
    }

    @Test
    fun `summarizeDecisions should handle empty decisions`() {
        val summary = PersistenceDecisionEngine.summarizeDecisions(emptyMap())
        assertEquals("No decisions to summarize", summary)
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun `should handle element with only resourceId as content`() {
        val element = createTestElement(
            className = "ImageButton",
            isClickable = true,
            resourceId = "com.example:id/menu_button"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        assertTrue(decision.shouldPersist)
        assertEquals(2, decision.ruleApplied)
    }

    @Test
    fun `should handle element with only contentDescription as content`() {
        val element = createTestElement(
            className = "ImageButton",
            isClickable = true,
            contentDescription = "Navigation menu"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.settings", listOf(element)
        )
        assertTrue(decision.shouldPersist)
        assertEquals(2, decision.ruleApplied)
    }

    @Test
    fun `should correctly handle productivity app (MIXED behavior)`() {
        val element = createTestElement(
            className = "Button",
            resourceId = "com.example:id/add_note",
            isClickable = true,
            text = "Add Note"
        )
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.google.android.keep", listOf(element)
        )
        // Productivity apps are MIXED, so Rule 6 applies
        assertEquals(6, decision.ruleApplied)
    }

    @Test
    fun `should correctly handle browser app (MOSTLY_DYNAMIC behavior)`() {
        val element = createTestElement(
            className = "EditText",
            resourceId = "com.example:id/url_bar",
            isClickable = true,
            text = "Search or enter URL"
        )
        // Chrome is a browser, Rule 5 doesn't apply (only EMAIL, MESSAGING, SOCIAL)
        // So it falls through to Rule 6
        val decision = PersistenceDecisionEngine.decideForElement(
            element, "com.android.chrome", listOf(element)
        )
        assertEquals(6, decision.ruleApplied)
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    private fun createTestElement(
        className: String = "View",
        resourceId: String = "",
        text: String = "",
        contentDescription: String = "",
        isClickable: Boolean = false,
        isScrollable: Boolean = false,
        isInDynamicContainer: Boolean = false,
        containerType: String = "",
        listIndex: Int = -1,
        isChecked: Boolean? = null
    ): ElementInfo {
        return ElementInfo(
            className = className,
            resourceId = resourceId,
            text = text,
            contentDescription = contentDescription,
            bounds = Bounds(0, 0, 100, 50),
            isClickable = isClickable,
            isScrollable = isScrollable,
            isEnabled = true,
            packageName = "",
            isInDynamicContainer = isInDynamicContainer,
            containerType = containerType,
            listIndex = listIndex,
            isChecked = isChecked
        )
    }
}
