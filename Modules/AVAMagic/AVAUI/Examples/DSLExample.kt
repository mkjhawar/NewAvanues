package com.augmentalis.avamagic.examples

import com.augmentalis.avamagic.core.*
import com.augmentalis.avamagic.dsl.*

/**
 * Example: Login Screen using AvaElements DSL
 *
 * Demonstrates:
 * - iOS 26 Liquid Glass theme
 * - Column layout with proper spacing
 * - Text, TextField, and Button components
 * - State management
 * - Event handling
 */
fun createLoginScreen(): AvaUI {
    return AvaUI {
        // Set theme to iOS 26 Liquid Glass
        theme = Themes.iOS26LiquidGlass

        // Root layout
        Column {
            padding(24f)
            arrangement = Arrangement.Center
            horizontalAlignment = Alignment.Center

            // Logo/Title
            Text("Welcome Back") {
                font = Font.Title
                color = theme?.colorScheme?.primary ?: Color.Blue
            }

            Text("Sign in to continue") {
                font = Font.Caption
                color = theme?.colorScheme?.onSurfaceVariant ?: Color.hex("#666666")
                padding(vertical = 8f)
            }

            // Email field
            TextField(
                value = "",
                placeholder = "Email"
            ) {
                label = "Email Address"
                leadingIcon = "email"
                fillMaxWidth()
                padding(vertical = 8f)
                cornerRadius(12f)
            }

            // Password field
            TextField(
                value = "",
                placeholder = "Password"
            ) {
                label = "Password"
                leadingIcon = "lock"
                trailingIcon = "visibility"
                fillMaxWidth()
                padding(vertical = 8f)
                cornerRadius(12f)
            }

            // Login button
            Button("Sign In") {
                buttonStyle = ButtonScope.ButtonStyle.Primary
                fillMaxWidth()
                padding(vertical = 16f)
                cornerRadius(12f)
                onClick = {
                    println("Login clicked")
                }
            }

            // Divider
            Row {
                padding(vertical = 24f)
                arrangement = Arrangement.Center

                Text("Don't have an account?") {
                    font = Font.Caption
                    color = theme?.colorScheme?.onSurfaceVariant ?: Color.hex("#666666")
                }
            }

            // Sign up button
            Button("Create Account") {
                buttonStyle = ButtonScope.ButtonStyle.Outlined
                fillMaxWidth()
                cornerRadius(12f)
                onClick = {
                    println("Sign up clicked")
                }
            }
        }
    }
}

/**
 * Example: Settings Screen with Card Layout
 *
 * Demonstrates:
 * - Material Design 3 theme
 * - Card components
 * - Switch and Checkbox components
 * - Nested layouts
 */
fun createSettingsScreen(): AvaUI {
    return AvaUI {
        theme = Themes.Material3Light

        ScrollView {
            Column {
                padding(16f)

                // Account Section
                Card {
                    elevation = 1
                    padding(16f)
                    cornerRadius(16f)

                    Column {
                        Text("Account") {
                            font = Font.Heading
                            color = theme?.colorScheme?.onSurface ?: Color.Black
                        }

                        Row {
                            padding(vertical = 16f)
                            arrangement = Arrangement.SpaceBetween

                            Text("Email Notifications") {
                                font = Font.Body
                            }
                        }

                        Row {
                            padding(vertical = 8f)

                            Text("Push Notifications") {
                                font = Font.Body
                            }
                        }
                    }
                }

                // Appearance Section
                Card {
                    elevation = 1
                    padding(16f)
                    cornerRadius(16f)

                    Column {
                        Text("Appearance") {
                            font = Font.Heading
                        }

                        Checkbox(
                            label = "Dark Mode",
                            checked = false
                        ) {
                            padding(vertical = 8f)
                            onCheckedChange = { isChecked ->
                                println("Dark mode: $isChecked")
                            }
                        }

                        Checkbox(
                            label = "Use System Theme",
                            checked = true
                        ) {
                            padding(vertical = 8f)
                        }
                    }
                }

                // About Section
                Card {
                    elevation = 1
                    padding(16f)
                    cornerRadius(16f)

                    Column {
                        Text("About") {
                            font = Font.Heading
                        }

                        Row {
                            padding(vertical = 8f)
                            arrangement = Arrangement.SpaceBetween

                            Text("Version") {
                                font = Font.Body
                            }

                            Text("1.0.0") {
                                font = Font.Body
                                color = theme?.colorScheme?.onSurfaceVariant ?: Color.hex("#666666")
                            }
                        }

                        Button("Check for Updates") {
                            buttonStyle = ButtonScope.ButtonStyle.Text
                            padding(vertical = 8f)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Example: Dashboard with Windows 11 Fluent 2
 *
 * Demonstrates:
 * - Windows 11 Fluent 2 theme
 * - Grid layout with cards
 * - Icons and images
 * - Complex nested layouts
 */
fun createDashboard(): AvaUI {
    return AvaUI {
        theme = Themes.Windows11Fluent2

        Column {
            padding(24f)

            // Header
            Row {
                arrangement = Arrangement.SpaceBetween
                padding(bottom = 24f)

                Text("Dashboard") {
                    font = Font(size = 32f, weight = Font.Weight.Bold)
                }

                Button("Refresh") {
                    buttonStyle = ButtonScope.ButtonStyle.Secondary
                    leadingIcon = "refresh"
                }
            }

            // Stats Row
            Row {
                arrangement = Arrangement.SpaceBetween
                padding(bottom = 16f)

                Card {
                    elevation = 2
                    padding(20f)
                    cornerRadius(8f)
                    background(theme?.colorScheme?.surfaceVariant ?: Color.White)

                    Column {
                        Icon("users") {
                            tint = theme?.colorScheme?.primary ?: Color.Blue
                        }

                        Text("Users") {
                            font = Font.Caption
                            padding(top = 8f)
                        }

                        Text("1,234") {
                            font = Font(size = 28f, weight = Font.Weight.Bold)
                        }
                    }
                }

                Card {
                    elevation = 2
                    padding(20f)
                    cornerRadius(8f)

                    Column {
                        Icon("chart") {
                            tint = theme?.colorScheme?.secondary ?: Color.hex("#8764B8")
                        }

                        Text("Revenue") {
                            font = Font.Caption
                            padding(top = 8f)
                        }

                        Text("$12,345") {
                            font = Font(size = 28f, weight = Font.Weight.Bold)
                        }
                    }
                }

                Card {
                    elevation = 2
                    padding(20f)
                    cornerRadius(8f)

                    Column {
                        Icon("trending_up") {
                            tint = theme?.colorScheme?.tertiary ?: Color.hex("#00B7C3")
                        }

                        Text("Growth") {
                            font = Font.Caption
                            padding(top = 8f)
                        }

                        Text("+23%") {
                            font = Font(size = 28f, weight = Font.Weight.Bold)
                        }
                    }
                }
            }

            // Recent Activity Card
            Card {
                elevation = 1
                padding(24f)
                cornerRadius(8f)

                Column {
                    Text("Recent Activity") {
                        font = Font.Heading
                        padding(bottom = 16f)
                    }

                    // Activity items would go here
                    Text("No recent activity") {
                        font = Font.Body
                        color = theme?.colorScheme?.onSurfaceVariant ?: Color.hex("#666666")
                    }
                }
            }
        }
    }
}

/**
 * Example: visionOS Spatial UI
 *
 * Demonstrates:
 * - visionOS 2 Spatial Glass theme
 * - Spatial depth and glass effects
 * - 3D-aware layouts
 */
fun createVisionOSUI(): AvaUI {
    return AvaUI {
        theme = Themes.visionOS2SpatialGlass

        Container {
            alignment = Alignment.Center
            padding(32f)

            Card {
                elevation = 3
                padding(48f)
                cornerRadius(30f)
                opacity(0.9f)  // Glass effect

                Column {
                    horizontalAlignment = Alignment.Center
                    arrangement = Arrangement.Center

                    Text("Welcome to VisionOS") {
                        font = Font(size = 40f, weight = Font.Weight.Bold)
                        color = theme?.colorScheme?.primary ?: Color.Blue
                    }

                    Text("Experience immersive computing") {
                        font = Font.Title
                        color = theme?.colorScheme?.onSurfaceVariant ?: Color.hex("#666666")
                        padding(vertical = 16f)
                    }

                    Row {
                        arrangement = Arrangement.SpaceEvenly
                        padding(top = 32f)

                        Button("Get Started") {
                            buttonStyle = ButtonScope.ButtonStyle.Primary
                            padding(horizontal = 24f, vertical = 12f)
                            cornerRadius(20f)
                        }

                        Button("Learn More") {
                            buttonStyle = ButtonScope.ButtonStyle.Outlined
                            padding(horizontal = 24f, vertical = 12f)
                            cornerRadius(20f)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Example usage in an application
 */
fun main() {
    // Create login screen
    val loginUI = createLoginScreen()

    // In a real application, you would render to platform:
    // val renderer = AndroidRenderer() // or iOSRenderer(), etc.
    // val platformView = loginUI.render(renderer)

    println("AvaElements DSL Examples created successfully!")
    println("Themes demonstrated:")
    println("  - iOS 26 Liquid Glass")
    println("  - Material Design 3")
    println("  - Windows 11 Fluent 2")
    println("  - visionOS 2 Spatial Glass")
}
