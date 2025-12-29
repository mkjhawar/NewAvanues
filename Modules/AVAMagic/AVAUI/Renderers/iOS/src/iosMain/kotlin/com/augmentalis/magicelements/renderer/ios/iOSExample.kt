package com.augmentalis.avaelements.renderer.ios

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.dsl.*
import com.augmentalis.avaelements.renderer.ios.bridge.SwiftUIView

/**
 * iOS SwiftUI Renderer Examples
 *
 * Demonstrates how to use the SwiftUI renderer to convert AvaElements
 * components to iOS native views.
 */

/**
 * Example 1: Login Screen with iOS 26 Liquid Glass theme
 *
 * This creates a login screen that will be rendered as native SwiftUI views.
 * The SwiftUIView result can be passed to Swift code for rendering.
 */
fun createiOSLoginScreen(): SwiftUIView {
    // Create the UI using AvaElements DSL
    val ui = AvaUI {
        theme = Themes.iOS26LiquidGlass

        Column {
            padding(24f)
            arrangement = Arrangement.Center
            horizontalAlignment = Alignment.Center

            // Title
            Text("Welcome Back") {
                font = Font(size = 34f, weight = Font.Weight.Bold)
                color = theme?.colorScheme?.primary ?: Color.Blue
                padding(bottom = 8f)
            }

            Text("Sign in to continue") {
                font = Font(size = 17f, weight = Font.Weight.Regular)
                color = theme?.colorScheme?.onSurfaceVariant ?: Color.hex("#666666")
                padding(bottom = 32f)
            }

            // Email field
            TextField(
                value = "",
                placeholder = "Email"
            ) {
                label = "Email Address"
                leadingIcon = "envelope"
                fillMaxWidth()
                padding(vertical = 8f)
                cornerRadius(14f)
            }

            // Password field
            TextField(
                value = "",
                placeholder = "Password"
            ) {
                label = "Password"
                leadingIcon = "lock"
                trailingIcon = "eye"
                fillMaxWidth()
                padding(vertical = 8f)
                cornerRadius(14f)
            }

            // Login button
            Button("Sign In") {
                buttonStyle = ButtonScope.ButtonStyle.Primary
                fillMaxWidth()
                padding(vertical = 16f)
                cornerRadius(14f)
                onClick = {
                    println("Login clicked")
                }
            }

            // Divider
            Row {
                padding(vertical = 24f)
                arrangement = Arrangement.Center

                Text("Don't have an account?") {
                    font = Font(size = 15f, weight = Font.Weight.Regular)
                    color = theme?.colorScheme?.onSurfaceVariant ?: Color.hex("#666666")
                }
            }

            // Sign up button
            Button("Create Account") {
                buttonStyle = ButtonScope.ButtonStyle.Outlined
                fillMaxWidth()
                cornerRadius(14f)
            }
        }
    }

    // Render to SwiftUI
    val renderer = SwiftUIRenderer.withLiquidGlass()
    return renderer.renderUI(ui)!!
}

/**
 * Example 2: Settings Screen with Cards
 *
 * Demonstrates card layouts and toggle controls
 */
fun createiOSSettingsScreen(): SwiftUIView {
    val ui = AvaUI {
        theme = Themes.iOS26LiquidGlass

        ScrollView {
            Column {
                padding(16f)

                // Account Section
                Card {
                    elevation = 1
                    padding(16f)

                    Column {
                        Text("Account") {
                            font = Font(size = 22f, weight = Font.Weight.Bold)
                            padding(bottom = 16f)
                        }

                        Row {
                            arrangement = Arrangement.SpaceBetween
                            padding(vertical = 12f)

                            Text("Email Notifications") {
                                font = Font.Body
                            }

                            Switch(checked = true) {
                                enabled = true
                            }
                        }

                        Row {
                            arrangement = Arrangement.SpaceBetween
                            padding(vertical = 12f)

                            Text("Push Notifications") {
                                font = Font.Body
                            }

                            Switch(checked = false) {
                                enabled = true
                            }
                        }
                    }
                }

                // Appearance Section
                Card {
                    elevation = 1
                    padding(16f)

                    Column {
                        Text("Appearance") {
                            font = Font(size = 22f, weight = Font.Weight.Bold)
                            padding(bottom = 16f)
                        }

                        Checkbox(
                            label = "Dark Mode",
                            checked = false
                        ) {
                            padding(vertical = 8f)
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

                    Column {
                        Text("About") {
                            font = Font(size = 22f, weight = Font.Weight.Bold)
                            padding(bottom = 16f)
                        }

                        Row {
                            arrangement = Arrangement.SpaceBetween
                            padding(vertical = 8f)

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

    return SwiftUIRenderer.withLiquidGlass().renderUI(ui)!!
}

/**
 * Example 3: visionOS Spatial UI
 *
 * Demonstrates spatial glass effects for visionOS
 */
fun createVisionOSWelcomeScreen(): SwiftUIView {
    val ui = AvaUI {
        theme = Themes.visionOS2SpatialGlass

        Container {
            alignment = Alignment.Center
            padding(48f)

            Card {
                elevation = 3
                padding(48f)
                opacity(0.9f)

                Column {
                    horizontalAlignment = Alignment.Center
                    arrangement = Arrangement.Center

                    Text("Welcome to VisionOS") {
                        font = Font(size = 40f, weight = Font.Weight.Bold)
                        color = theme?.colorScheme?.primary ?: Color.Blue
                        padding(bottom = 16f)
                    }

                    Text("Experience immersive computing") {
                        font = Font(size = 22f, weight = Font.Weight.Regular)
                        color = theme?.colorScheme?.onSurfaceVariant ?: Color.hex("#666666")
                        padding(bottom = 32f)
                    }

                    Row {
                        arrangement = Arrangement.SpaceEvenly

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

    return SwiftUIRenderer.withSpatialGlass().renderUI(ui)!!
}

/**
 * Example 4: Component-level rendering
 *
 * Shows how to render individual components
 */
fun createiOSProfileCard(): SwiftUIView {
    val card = CardComponent(
        id = "profileCard",
        style = null,
        modifiers = listOf(
            Modifier.Padding(Spacing.all(16f)),
            Modifier.FillMaxWidth
        ),
        elevation = 2,
        children = listOf(
            RowComponent(
                id = null,
                style = null,
                modifiers = emptyList(),
                arrangement = Arrangement.Start,
                verticalAlignment = Alignment.Center,
                children = listOf(
                    IconComponent(
                        name = "person.circle.fill",
                        id = null,
                        style = null,
                        modifiers = listOf(
                            Modifier.Size(Size.Fixed(48f), Size.Fixed(48f))
                        ),
                        tint = Color.hex("#007AFF"),
                        contentDescription = "Profile"
                    ),
                    ColumnComponent(
                        id = null,
                        style = null,
                        modifiers = listOf(
                            Modifier.Padding(Spacing.horizontal(16f))
                        ),
                        arrangement = Arrangement.Start,
                        horizontalAlignment = Alignment.Start,
                        children = listOf(
                            TextComponent(
                                text = "John Doe",
                                id = null,
                                style = null,
                                modifiers = emptyList(),
                                font = Font(size = 20f, weight = Font.Weight.SemiBold),
                                color = Color.Black,
                                textAlign = TextScope.TextAlign.Start,
                                maxLines = 1,
                                overflow = TextScope.TextOverflow.Ellipsis
                            ),
                            TextComponent(
                                text = "john.doe@example.com",
                                id = null,
                                style = null,
                                modifiers = emptyList(),
                                font = Font(size = 15f, weight = Font.Weight.Regular),
                                color = Color.hex("#666666"),
                                textAlign = TextScope.TextAlign.Start,
                                maxLines = 1,
                                overflow = TextScope.TextOverflow.Ellipsis
                            )
                        )
                    )
                )
            )
        )
    )

    // Render with iOS 26 Liquid Glass theme
    return card.toSwiftUI(Themes.iOS26LiquidGlass)
}

/**
 * Example 5: Working with theme tokens
 *
 * Shows how to access and use theme design tokens
 */
fun demonstrateThemeTokens() {
    val renderer = SwiftUIRenderer.withLiquidGlass()

    // Access color tokens
    val primaryColor = renderer.getThemeColor("primary")
    val surfaceColor = renderer.getThemeColor("surface")
    println("Primary: $primaryColor")
    println("Surface: $surfaceColor")

    // Access font tokens
    val headlineFont = renderer.getThemeFont("headlineLarge")
    println("Headline font: $headlineFont")

    // Access shape tokens
    val mediumCornerRadius = renderer.getThemeShape("medium")
    println("Medium corner radius: $mediumCornerRadius")

    // Access spacing tokens
    val mediumSpacing = renderer.getThemeSpacing("md")
    println("Medium spacing: $mediumSpacing")

    // Check material effects
    if (renderer.usesLiquidGlass()) {
        val materialTokens = renderer.getMaterialTokens()
        println("Glass blur radius: ${materialTokens?.glassBlurRadius}")
        println("Glass tint: ${materialTokens?.glassTintColor}")
    }
}

/**
 * Main function demonstrating all examples
 */
fun main() {
    println("=== AvaElements iOS SwiftUI Renderer Examples ===\n")

    // Example 1: Login Screen
    println("1. Creating iOS Login Screen with Liquid Glass...")
    val loginScreen = createiOSLoginScreen()
    println("   ✓ Login screen created: ${loginScreen.type}")
    println("   ✓ Children count: ${loginScreen.children.size}")

    // Example 2: Settings Screen
    println("\n2. Creating iOS Settings Screen...")
    val settingsScreen = createiOSSettingsScreen()
    println("   ✓ Settings screen created: ${settingsScreen.type}")

    // Example 3: visionOS UI
    println("\n3. Creating visionOS Welcome Screen...")
    val visionOSScreen = createVisionOSWelcomeScreen()
    println("   ✓ VisionOS screen created: ${visionOSScreen.type}")

    // Example 4: Component-level rendering
    println("\n4. Creating Profile Card component...")
    val profileCard = createiOSProfileCard()
    println("   ✓ Profile card created: ${profileCard.type}")

    // Example 5: Theme tokens
    println("\n5. Demonstrating theme tokens...")
    demonstrateThemeTokens()

    println("\n=== All examples completed successfully! ===")
    println("\nNext steps:")
    println("  1. Pass these SwiftUIView objects to Swift code")
    println("  2. Use AvaElementsView in SwiftUI to render them")
    println("  3. Connect event handlers to Swift callbacks")
}

/**
 * Helper function to create a simple test component
 */
fun createSimpleTest(): SwiftUIView {
    return SwiftUIView.vStack(
        spacing = 16f,
        children = listOf(
            SwiftUIView.text("Hello, SwiftUI!"),
            SwiftUIView.button("Tap Me")
        )
    )
}
