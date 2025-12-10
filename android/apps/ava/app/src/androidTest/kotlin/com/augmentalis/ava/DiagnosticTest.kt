// filename: apps/ava-standalone/src/androidTest/kotlin/com/augmentalis/ava/DiagnosticTest.kt
// Diagnostic test to see what renders when MainActivity launches

package com.augmentalis.ava

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DiagnosticTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun diagnostic_printSemanticTree() {
        // Wait 5 seconds for app to initialize
        Thread.sleep(5000)

        // Print ENTIRE semantic tree to see what's actually rendered
        composeTestRule.onRoot().printToLog("DIAGNOSTIC")

        // Count all nodes
        val allNodes = composeTestRule.onAllNodes(hasAnyAncestor(isRoot()) or isRoot())
        println("DIAGNOSTIC: Total nodes in tree: ${allNodes.fetchSemanticsNodes().size}")

        // Look for Settings specifically
        val settingsNodes = composeTestRule.onAllNodesWithContentDescription("Settings")
        println("DIAGNOSTIC: Settings nodes found: ${settingsNodes.fetchSemanticsNodes().size}")

        // Look for Chat-related nodes
        val chatNodes = composeTestRule.onAllNodesWithText("Chat", substring = true, ignoreCase = true)
        println("DIAGNOSTIC: Chat nodes found: ${chatNodes.fetchSemanticsNodes().size}")

        // This test always passes - just for debugging
        assert(true)
    }
}
