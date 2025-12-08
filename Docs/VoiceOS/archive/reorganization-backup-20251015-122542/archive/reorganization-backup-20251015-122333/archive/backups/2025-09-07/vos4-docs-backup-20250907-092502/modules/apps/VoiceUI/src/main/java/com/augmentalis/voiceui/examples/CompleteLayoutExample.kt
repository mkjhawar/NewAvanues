package com.augmentalis.voiceui.examples

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceui.api.*
import com.augmentalis.voiceui.dsl.*
import com.augmentalis.voiceui.layout.*
import com.augmentalis.voiceui.theme.*

/**
 * Complete example showing all padding and layout features
 * This demonstrates the flexibility of VoiceUI's layout system
 */

/**
 * Example 1: Dashboard with different padding for all sides
 */
@Composable
fun DashboardWithCustomPadding() {
    GreyARTheme {
        MagicScreen(
            layout = "column",  // Default vertical layout
            defaultSpacing = 20,  // 20dp between all elements
            screenPadding = 24    // 24dp padding around entire screen
        ) {
            // Card with explicit padding for each side
            card(
                title = "Login Information",
                padTop = 24.dp,
                padBottom = 32.dp,
                padLeft = 20.dp,
                padRight = 20.dp
            ) {
                text("Username: john.doe@example.com")
                text("Last Login: 2 hours ago")
                button("Logout") { }
            }
            
            // Card with CSS-style padding string
            card(
                title = "System Information",
                pad = "16 24 20 24"  // top right bottom left
            ) {
                text("Device: Pixel 8 Pro")
                text("OS: Android 14")
                text("Memory: 8GB")
            }
            
            // Card with preset padding
            card(
                title = "Random Stuff",
                pad = "comfortable"  // Uses preset
            ) {
                text("Daily Quote: Innovation distinguishes leaders")
                button("Refresh") { }
            }
        }
    }
}

/**
 * Example 2: Grid layout with spacing between cards
 */
@Composable
fun GridLayoutExample() {
    GreyARTheme {
        MagicScreen(
            layout = "grid 3",  // 3-column grid
            defaultSpacing = 24  // 24dp gap between cards
        ) {
            // Cards automatically arranged in 3-column grid
            card(title = "Card 1", pad = 16) {
                text("Content 1")
            }
            
            card(title = "Card 2", pad = "small") {
                text("Content 2")
            }
            
            card(title = "Card 3", pad = "large") {
                text("Content 3")
            }
            
            card(title = "Card 4", pad = "12 16") {
                text("Content 4")
            }
            
            card(title = "Card 5", pad = "comfortable") {
                text("Content 5")
            }
            
            card(title = "Card 6", pad = 20) {
                text("Content 6")
            }
        }
    }
}

/**
 * Example 3: Mixed layouts with custom spacing
 */
@Composable
fun MixedLayoutExample() {
    GreyARTheme {
        MagicScreen {
            // Top row with 2 cards side by side
            row(gap = 30.dp, pad = "0 0 24 0") {  // No padding except bottom
                card(
                    title = "Left Card",
                    width = "half",
                    pad = "16"
                ) {
                    text("Left content")
                    button("Action") { }
                }
                
                card(
                    title = "Right Card",
                    width = "half",
                    pad = "16"
                ) {
                    text("Right content")
                    toggle("Enable feature")
                }
            }
            
            // Full width card with custom padding
            card(
                title = "Wide Card",
                width = "full",
                padTop = 8.dp,
                padBottom = 24.dp,
                padHorizontal = 32.dp  // Sets both left and right
            ) {
                // Nested row inside card
                row(gap = 16.dp) {
                    button("Option 1") { }
                    button("Option 2") { }
                    button("Option 3") { }
                }
            }
            
            // Grid section at bottom
            grid(columns = 4, gap = 12.dp, pad = "16") {
                item { text("Item 1") }
                item { text("Item 2") }
                item { text("Item 3") }
                item { text("Item 4") }
                item { text("Item 5") }
                item { text("Item 6") }
                item { text("Item 7") }
                item { text("Item 8") }
            }
        }
    }
}

/**
 * Example 4: AR overlay with absolute positioning
 */
@Composable
fun AROverlayExample() {
    GreyARTheme {
        ARLayout {
            // Card positioned at specific location
            positioned(top = 50.dp, left = 100.dp) {
                VoiceMagicCard(title = "Top Left Card") {
                    Text("Positioned at (100, 50)")
                }
            }
            
            // Centered card
            positioned(centerX = true, centerY = true) {
                VoiceMagicCard(title = "Centered Card") {
                    Text("This card is centered on screen")
                    VoiceMagicButton("OK") {}
                }
            }
            
            // Bottom right corner
            positioned(bottom = 50.dp, right = 50.dp) {
                VoiceMagicCard(title = "Quick Actions") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceMagicButton("Save") {}
                        VoiceMagicButton("Cancel") {}
                    }
                }
            }
        }
    }
}

/**
 * Example 5: Responsive layout that adapts to screen size
 */
@Composable
fun ResponsiveLayoutExample() {
    GreyARTheme {
        ResponsiveLayout {
            // Different layouts for different screen sizes
            small {
                // Stack vertically on small screens
                MagicScreen(layout = "column", defaultSpacing = 16) {
                    card(title = "Card 1", pad = "16") { text("Mobile view") }
                    card(title = "Card 2", pad = "16") { text("Stacked vertically") }
                    card(title = "Card 3", pad = "16") { text("For small screens") }
                }
            }
            
            medium {
                // 2-column grid on tablets
                MagicScreen(layout = "grid 2", defaultSpacing = 20) {
                    card(title = "Card 1", pad = "20") { text("Tablet view") }
                    card(title = "Card 2", pad = "20") { text("2 columns") }
                    card(title = "Card 3", pad = "20") { text("Medium screens") }
                    card(title = "Card 4", pad = "20") { text("More space") }
                }
            }
            
            large {
                // 3-column grid on desktop
                MagicScreen(layout = "grid 3", defaultSpacing = 24) {
                    card(title = "Card 1", pad = "24") { text("Desktop view") }
                    card(title = "Card 2", pad = "24") { text("3 columns") }
                    card(title = "Card 3", pad = "24") { text("Large screens") }
                }
            }
        }
    }
}

/**
 * Example 6: Using padding builder pattern
 */
@Composable
fun PaddingBuilderExample() {
    GreyARTheme {
        MagicScreen {
            card(title = "Custom Padding") {
                // Using builder pattern for complex padding
                padded(pad = "24") {
                    text("This has 24dp padding all around")
                }
                
                padded(pad = "8 16") {
                    text("8dp vertical, 16dp horizontal")
                }
                
                padded(pad = "comfortable") {
                    text("Using comfortable preset")
                }
            }
        }
    }
}

/**
 * Example 7: Complete dashboard matching your requirements
 */
@Composable
fun CompleteDashboard() {
    GreyARTheme {
        MagicScreen(
            defaultSpacing = 20,     // Global spacing between elements
            screenPadding = 24       // Padding around entire screen
        ) {
            // Row of cards at top with custom spacing
            row(gap = 24.dp, pad = "0 0 20 0") {
                // Login Information - different padding per side
                card(
                    title = "Login Information",
                    padTop = 20.dp,
                    padBottom = 24.dp,
                    padLeft = 24.dp,
                    padRight = 24.dp,
                    width = 0.5f  // 50% width
                ) {
                    text("Username: john.doe")  // Bottom padding only
                    text("Last Login: 2 hours ago")
                    text("Session: Active")
                    button("Logout") { }  // Top padding only
                }
                
                // System Information - CSS style padding
                card(
                    title = "System Information",
                    pad = "20 24 24 24",  // Different top padding
                    width = 0.5f
                ) {
                    text("Device: Pixel 8 Pro")
                    text("OS: Android 14")
                    text("Memory: 8GB RAM")
                    text("Storage: 64GB Available")
                }
            }
            
            // Spacer between sections
            spacer(32)
            
            // Random Stuff - full width with comfortable padding
            card(
                title = "Random Stuff",
                pad = "comfortable",
                width = "full"
            ) {
                column(gap = 12.dp) {
                    text("Daily Quote: 'The best way to predict the future is to invent it.'")
                    text("Weather: 72°F, Partly Cloudy")
                    text("News: Latest updates from around the world")
                    
                    row(gap = 16.dp, pad = "16 0 0 0") {
                        button("Action 1") { }
                        button("Action 2") { }
                        button("Refresh") { }
                    }
                }
            }
        }
    }
}

/**
 * Example 8: Natural language with padding
 */
@Composable
fun NaturalLanguageWithPadding() {
    GreyARMagicScreen(
        description = "dashboard with three cards having comfortable padding and 24dp spacing between them"
    ) {
        // VoiceUI automatically applies the described padding and spacing
    }
}