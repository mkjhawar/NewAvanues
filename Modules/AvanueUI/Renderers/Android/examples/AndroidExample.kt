package com.augmentalis.avanueui.renderer.android.examples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.dsl.ButtonScope
import com.augmentalis.avanueui.dsl.AvaUI
import com.augmentalis.avanueui.renderer.android.AvaUI as RenderAvaUI

/**
 * AndroidExample - Demo Android app using AvaElements Compose Renderer
 *
 * This example shows how to integrate AvaElements into an Android app
 * using Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create UI using AvaElements DSL
        val ui = createLoginScreen()

        setContent {
            // Render AvaUI in Compose
            RenderAvaUI(ui)
        }
    }
}

/**
 * Create a login screen using AvaElements DSL
 */
fun createLoginScreen() = AvaUI {
    // Set theme
    theme = Themes.Material3Light

    // Build UI tree
    Column {
        padding(all = 24f)
        arrangement = Arrangement.Center
        horizontalAlignment = Alignment.Center
        fillMaxSize()

        // Logo/Icon
        Icon("person") {
            tint = Color.hex("#6750A4")
            size(width = Size.Fixed(80f), height = Size.Fixed(80f))
        }

        // Title
        Text("Welcome Back") {
            font = Font.Title
            color = Color.hex("#1D1B20")
            padding(vertical = 16f)
        }

        // Subtitle
        Text("Sign in to continue") {
            font = Font.Body
            color = Color.hex("#49454F")
            padding(bottom = 32f)
        }

        // Email field
        TextField(
            value = "",
            placeholder = "Enter your email"
        ) {
            label = "Email"
            leadingIcon = "email"
            fillMaxWidth()
            padding(bottom = 16f)
        }

        // Password field
        TextField(
            value = "",
            placeholder = "Enter your password"
        ) {
            label = "Password"
            leadingIcon = "lock"
            trailingIcon = "visibility"
            fillMaxWidth()
            padding(bottom = 8f)
        }

        // Remember me checkbox
        Row {
            arrangement = Arrangement.SpaceBetween
            fillMaxWidth()
            padding(vertical = 16f)

            Checkbox(label = "Remember me", checked = false) {
                enabled = true
            }
        }

        // Sign in button
        Button("Sign In") {
            buttonStyle = ButtonScope.ButtonStyle.Primary
            fillMaxWidth()
            padding(vertical = 8f)
            onClick = {
                println("Sign in clicked!")
            }
        }

        // Secondary button
        Button("Sign Up") {
            buttonStyle = ButtonScope.ButtonStyle.Outlined
            fillMaxWidth()
            padding(top = 8f)
        }

        // Footer text
        Text("Forgot password?") {
            font = Font.Caption
            color = Color.hex("#6750A4")
            padding(top = 24f)
            clickable {
                println("Forgot password clicked!")
            }
        }
    }
}

/**
 * Example: Card-based dashboard
 */
fun createDashboard() = AvaUI {
    theme = Themes.Material3Light

    ScrollView(orientation = Orientation.Vertical) {
        Column {
            padding(all = 16f)

            // Header
            Text("Dashboard") {
                font = Font(size = 28f, weight = Font.Weight.Bold)
                padding(bottom = 16f)
            }

            // Stats cards
            Row {
                arrangement = Arrangement.SpaceBetween
                fillMaxWidth()
                padding(bottom = 16f)

                Card {
                    elevation = 2
                    padding(all = 16f)

                    Column {
                        Text("Users") {
                            font = Font.Caption
                            color = Color.hex("#49454F")
                        }
                        Text("1,234") {
                            font = Font.Title
                            color = Color.hex("#1D1B20")
                        }
                    }
                }

                Card {
                    elevation = 2
                    padding(all = 16f)

                    Column {
                        Text("Revenue") {
                            font = Font.Caption
                            color = Color.hex("#49454F")
                        }
                        Text("$45.2K") {
                            font = Font.Title
                            color = Color.hex("#1D1B20")
                        }
                    }
                }
            }

            // Activity card
            Card {
                elevation = 1
                padding(all = 16f)
                fillMaxWidth()

                Column {
                    Text("Recent Activity") {
                        font = Font.Heading
                        padding(bottom = 12f)
                    }

                    Text("User John Doe signed up") {
                        font = Font.Body
                        padding(vertical = 4f)
                    }

                    Text("New order #1234 received") {
                        font = Font.Body
                        padding(vertical = 4f)
                    }

                    Text("Payment processed") {
                        font = Font.Body
                        padding(vertical = 4f)
                    }
                }
            }
        }
    }
}

/**
 * Example: Settings screen with switches and checkboxes
 */
fun createSettingsScreen() = AvaUI {
    theme = Themes.Material3Light

    Column {
        padding(all = 16f)

        Text("Settings") {
            font = Font.Title
            padding(bottom = 24f)
        }

        // Notifications section
        Text("Notifications") {
            font = Font.Heading
            padding(bottom = 8f)
        }

        Row {
            arrangement = Arrangement.SpaceBetween
            fillMaxWidth()
            padding(vertical = 8f)

            Text("Push Notifications") {
                font = Font.Body
            }

            Switch(checked = true) {
                enabled = true
            }
        }

        Row {
            arrangement = Arrangement.SpaceBetween
            fillMaxWidth()
            padding(vertical = 8f)

            Text("Email Notifications") {
                font = Font.Body
            }

            Switch(checked = false) {
                enabled = true
            }
        }

        // Privacy section
        Text("Privacy") {
            font = Font.Heading
            padding(top = 24f, bottom = 8f)
        }

        Checkbox(label = "Share analytics data", checked = false)
        Checkbox(label = "Show online status", checked = true)
        Checkbox(label = "Allow location access", checked = false)

        // About section
        Text("About") {
            font = Font.Heading
            padding(top = 24f, bottom = 8f)
        }

        Text("Version 1.0.0") {
            font = Font.Caption
            color = Color.hex("#49454F")
        }
    }
}
