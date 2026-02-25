/**
 * ComposableTestTemplate.kt - Advanced Compose UI testing template
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: {{DATE}}
 * 
 * Comprehensive Compose UI testing with interactions, accessibility, and visual regression
 */
package {{PACKAGE_NAME}}

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.voiceuielements.theme.VoiceUITheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class {{CLASS_NAME}}Test {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    {{TEST_DATA_DECLARATIONS}}
    
    @Before
    fun setup() {
        // Initialize test data
        {{TEST_DATA_SETUP}}
    }
    
    // ========== Rendering Tests ==========
    
    @Test
    fun `test composable renders correctly`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    {{DEFAULT_PARAMS}}
                )
            }
        }
        
        // Verify component exists
        composeTestRule.onNodeWithTag("{{TAG_NAME}}").assertExists()
        composeTestRule.onNodeWithTag("{{TAG_NAME}}").assertIsDisplayed()
        {{RENDERING_ASSERTIONS}}
    }
    
    @Test
    fun `test composable renders with custom parameters`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    {{CUSTOM_PARAMS}}
                )
            }
        }
        
        {{CUSTOM_RENDERING_ASSERTIONS}}
    }
    
    // ========== State Management Tests ==========
    
    @Test
    fun `test state changes update UI correctly`() {
        val stateHolder = mutableStateOf({{INITIAL_STATE}})
        
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    state = stateHolder.value,
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Initial state
        {{INITIAL_STATE_ASSERTIONS}}
        
        // Update state
        stateHolder.value = {{UPDATED_STATE}}
        
        // Verify UI updated
        composeTestRule.waitForIdle()
        {{UPDATED_STATE_ASSERTIONS}}
    }
    
    @Test
    fun `test recomposition efficiency`() {
        var recompositionCount = 0
        
        composeTestRule.setContent {
            VoiceUITheme {
                // Track recompositions
                SideEffect {
                    recompositionCount++
                }
                
                {{CLASS_NAME}}(
                    {{PARAMS}}
                )
            }
        }
        
        // Trigger state change
        {{TRIGGER_STATE_CHANGE}}
        
        composeTestRule.waitForIdle()
        
        // Verify minimal recompositions
        assertTrue(
            recompositionCount <= 2,
            "Excessive recompositions: $recompositionCount"
        )
    }
    
    // ========== Interaction Tests ==========
    
    @Test
    fun `test click interaction`() {
        var clicked = false
        
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    onClick = { clicked = true },
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Perform click
        composeTestRule.onNodeWithTag("{{CLICKABLE_TAG}}").performClick()
        
        // Verify callback invoked
        assertTrue(clicked)
        {{CLICK_RESULT_ASSERTIONS}}
    }
    
    @Test
    fun `test swipe gestures`() {
        var swipeDirection: String? = null
        
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    onSwipe = { direction -> swipeDirection = direction },
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Test swipe gestures
        composeTestRule.onNodeWithTag("{{SWIPEABLE_TAG}}").performTouchInput {
            swipeLeft()
        }
        assertEquals("left", swipeDirection)
        
        composeTestRule.onNodeWithTag("{{SWIPEABLE_TAG}}").performTouchInput {
            swipeRight()
        }
        assertEquals("right", swipeDirection)
    }
    
    @Test
    fun `test text input interaction`() {
        var inputText = ""
        
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    text = inputText,
                    onTextChange = { inputText = it },
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Enter text
        composeTestRule.onNodeWithTag("{{TEXT_FIELD_TAG}}")
            .performTextInput("Test Input")
        
        // Verify text updated
        composeTestRule.onNodeWithText("Test Input").assertExists()
        assertEquals("Test Input", inputText)
    }
    
    @Test
    fun `test scroll behavior`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    items = List(100) { "Item $it" },
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Verify initial items visible
        composeTestRule.onNodeWithText("Item 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item 99").assertDoesNotExist()
        
        // Scroll to bottom
        composeTestRule.onNodeWithTag("{{SCROLLABLE_TAG}}").performScrollToIndex(99)
        
        // Verify last item visible
        composeTestRule.onNodeWithText("Item 99").assertIsDisplayed()
    }
    
    // ========== Accessibility Tests ==========
    
    @Test
    fun `test accessibility semantics`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    contentDescription = "{{CONTENT_DESCRIPTION}}",
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Verify accessibility properties
        composeTestRule.onNodeWithContentDescription("{{CONTENT_DESCRIPTION}}")
            .assertExists()
            .assertHasClickAction()
        
        {{ACCESSIBILITY_ASSERTIONS}}
    }
    
    @Test
    fun `test screen reader support`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    {{PARAMS}}
                )
            }
        }
        
        // Verify all interactive elements have descriptions
        composeTestRule.onAllNodes(hasClickAction())
            .assertAll(hasContentDescription())
        
        // Verify focus order
        {{FOCUS_ORDER_ASSERTIONS}}
    }
    
    @Test
    fun `test minimum touch target size`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    {{PARAMS}}
                )
            }
        }
        
        // Verify all clickable elements meet minimum size (48dp)
        composeTestRule.onAllNodes(hasClickAction()).assertAll(
            hasMinimumSize(48.dp, 48.dp)
        )
    }
    
    // ========== Theme Tests ==========
    
    @Test
    fun `test light theme appearance`() {
        composeTestRule.setContent {
            VoiceUITheme(darkTheme = false) {
                {{CLASS_NAME}}(
                    {{PARAMS}}
                )
            }
        }
        
        {{LIGHT_THEME_ASSERTIONS}}
    }
    
    @Test
    fun `test dark theme appearance`() {
        composeTestRule.setContent {
            VoiceUITheme(darkTheme = true) {
                {{CLASS_NAME}}(
                    {{PARAMS}}
                )
            }
        }
        
        {{DARK_THEME_ASSERTIONS}}
    }
    
    @Test
    fun `test dynamic theming`() {
        val isDarkTheme = mutableStateOf(false)
        
        composeTestRule.setContent {
            VoiceUITheme(darkTheme = isDarkTheme.value) {
                {{CLASS_NAME}}(
                    {{PARAMS}}
                )
            }
        }
        
        // Switch theme
        isDarkTheme.value = true
        composeTestRule.waitForIdle()
        
        // Verify theme changed
        {{THEME_CHANGE_ASSERTIONS}}
    }
    
    // ========== Visual Regression Tests ==========
    
    @Test
    fun `test visual consistency`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    {{PARAMS}}
                )
            }
        }
        
        // Capture screenshot
        composeTestRule.onRoot()
            .captureToImage()
            .assertAgainstGolden("{{CLASS_NAME_LOWER}}_default")
    }
    
    @Test
    fun `test visual states`() {
        val states = listOf("enabled", "disabled", "pressed", "focused")
        
        states.forEach { state ->
            composeTestRule.setContent {
                VoiceUITheme {
                    {{CLASS_NAME}}(
                        state = state,
                        {{OTHER_PARAMS}}
                    )
                }
            }
            
            composeTestRule.onRoot()
                .captureToImage()
                .assertAgainstGolden("{{CLASS_NAME_LOWER}}_$state")
        }
    }
    
    // ========== Performance Tests ==========
    
    @Test
    fun `test rendering performance`() {
        val renderTimes = mutableListOf<Long>()
        
        repeat(10) {
            val startTime = System.nanoTime()
            
            composeTestRule.setContent {
                VoiceUITheme {
                    {{CLASS_NAME}}(
                        {{PARAMS}}
                    )
                }
            }
            
            composeTestRule.waitForIdle()
            
            val renderTime = (System.nanoTime() - startTime) / 1_000_000
            renderTimes.add(renderTime)
        }
        
        val averageRenderTime = renderTimes.average()
        assertTrue(
            averageRenderTime < 16,
            "Render time should be <16ms for 60fps, was ${averageRenderTime}ms"
        )
    }
    
    @Test
    fun `test handles large data sets efficiently`() {
        val largeDataSet = List(10000) { "Item $it" }
        
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    items = largeDataSet,
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Verify lazy loading works
        composeTestRule.waitForIdle()
        
        // Perform scroll operation
        val scrollStartTime = System.currentTimeMillis()
        composeTestRule.onNodeWithTag("{{SCROLLABLE_TAG}}").performScrollToIndex(5000)
        val scrollTime = System.currentTimeMillis() - scrollStartTime
        
        assertTrue(
            scrollTime < 1000,
            "Scroll should be smooth (<1s), took ${scrollTime}ms"
        )
    }
    
    // ========== Edge Cases ==========
    
    @Test
    fun `test handles empty state`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    items = emptyList(),
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Verify empty state UI
        composeTestRule.onNodeWithText("{{EMPTY_STATE_TEXT}}").assertExists()
    }
    
    @Test
    fun `test handles error state`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    error = Exception("Test error"),
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Verify error UI
        composeTestRule.onNodeWithText("Test error").assertExists()
        {{ERROR_STATE_ASSERTIONS}}
    }
    
    @Test
    fun `test handles loading state`() {
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    isLoading = true,
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Verify loading indicator
        composeTestRule.onNode(hasProgressBarRangeInfo()).assertExists()
    }
    
    // ========== Integration Tests ==========
    
    @Test
    fun `test integration with navigation`() {
        var navigatedTo: String? = null
        
        composeTestRule.setContent {
            VoiceUITheme {
                {{CLASS_NAME}}(
                    onNavigate = { destination -> navigatedTo = destination },
                    {{OTHER_PARAMS}}
                )
            }
        }
        
        // Trigger navigation
        composeTestRule.onNodeWithTag("{{NAVIGATION_TRIGGER}}").performClick()
        
        // Verify navigation occurred
        assertNotNull(navigatedTo)
        {{NAVIGATION_ASSERTIONS}}
    }
    
    // Helper functions
    
    private fun SemanticsNodeInteraction.hasMinimumSize(width: dp, height: dp): SemanticsMatcher {
        return SemanticsMatcher("has minimum size") { node ->
            val bounds = node.boundsInRoot
            bounds.width >= width && bounds.height >= height
        }
    }
}