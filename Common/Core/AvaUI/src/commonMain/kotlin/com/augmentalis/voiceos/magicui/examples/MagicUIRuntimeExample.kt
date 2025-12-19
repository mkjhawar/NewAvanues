package com.augmentalis.avanues.avaui.examples

import com.augmentalis.avanues.avaui.AvaUIRuntime
import kotlinx.coroutines.runBlocking

/**
 * Example demonstrating AvaUIRuntime usage.
 *
 * This file shows how to:
 * - Load a DSL app from source
 * - Start and control app lifecycle
 * - Handle voice commands
 * - Manage app state
 * - Shutdown runtime
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27
 */

/**
 * Sample .vos DSL app source.
 *
 * This app demonstrates:
 * - App metadata (id, name)
 * - Component instantiation (ColorPicker)
 * - Property assignment (initialColor, showAlpha)
 * - Voice commands ("change color" → openColorPicker)
 */
val sampleAppSource = """
    App {
        id: "com.example.colorpicker"
        name: "Color Picker Example"
        runtime: "AvaUI"

        ColorPicker {
            id: "mainPicker"
            initialColor: "#FF5733"
            showAlpha: true
            showHex: true
            allowEyedropper: true

            onColorChange: (color) => {
                VoiceOS.speak("Color changed")
            }
        }

        VoiceCommands {
            "change color" => openColorPicker
            "show picker" => openColorPicker
            "select color" => openColorPicker
        }
    }
""".trimIndent()

/**
 * Main example function.
 *
 * Demonstrates complete runtime workflow from app loading to shutdown.
 */
fun main() = runBlocking {
    println("=== AvaUI Runtime Example ===\n")

    // 1. Create runtime
    println("1. Creating runtime...")
    val runtime = AvaUIRuntime()

    // 2. Load DSL app
    println("2. Loading DSL app...")
    val app = runtime.loadApp(sampleAppSource)
    println("   Loaded: ${app.name} (${app.id})")
    println("   Components: ${app.components.size}")
    println("   Voice commands: ${app.voiceCommands.size}")

    // 3. Start app
    println("\n3. Starting app...")
    val runningApp = runtime.start(app)
    println("   App started: ${runningApp.name}")
    println("   Lifecycle state: ${runningApp.lifecycle.state.value}")
    println("   Instantiated components: ${runningApp.components.size}")

    // 4. Handle voice commands
    println("\n4. Testing voice commands...")

    val commands = listOf(
        "change color",           // Exact match
        "change the color",       // Fuzzy match
        "show picker",            // Alternative phrase
        "invalid command"         // No match
    )

    for (command in commands) {
        val handled = runtime.handleVoiceCommand(app.id, command)
        println("   \"$command\" -> ${if (handled) "✓ Handled" else "✗ Not recognized"}")
    }

    // 5. Pause and resume
    println("\n5. Testing lifecycle...")
    runtime.pause(app.id)
    println("   App paused: ${runningApp.lifecycle.state.value}")

    runtime.resume(app.id)
    println("   App resumed: ${runningApp.lifecycle.state.value}")

    // 6. Inspect running apps
    println("\n6. Inspecting runtime state...")
    val allApps = runtime.getAllApps()
    println("   Total running apps: ${allApps.size}")
    allApps.forEach { runningApp ->
        println("   - ${runningApp.name} [${runningApp.id}]")
        println("     State: ${runningApp.lifecycle.state.value}")
        println("     Components: ${runningApp.components.keys.joinToString(", ")}")
    }

    // 7. Stop app
    println("\n7. Stopping app...")
    runtime.stop(app.id)
    println("   App stopped")
    println("   Running apps: ${runtime.getAllApps().size}")

    // 8. Shutdown runtime
    println("\n8. Shutting down runtime...")
    runtime.shutdown()
    println("   Runtime shutdown complete")

    println("\n=== Example Complete ===")
}

/**
 * Example with multiple apps.
 */
fun multiAppExample() = runBlocking {
    val runtime = AvaUIRuntime()

    // App 1: Color Picker
    val colorPickerSource = """
        App {
            id: "com.example.colorpicker"
            name: "Color Picker"

            ColorPicker {
                id: "picker"
                initialColor: "#FF5733"
            }
        }
    """.trimIndent()

    // App 2: Preferences
    val preferencesSource = """
        App {
            id: "com.example.preferences"
            name: "Preferences"

            Preferences {
                id: "prefs"
                title: "Settings"
            }
        }
    """.trimIndent()

    // Load and start both apps
    val app1 = runtime.loadApp(colorPickerSource)
    val app2 = runtime.loadApp(preferencesSource)

    runtime.start(app1)
    runtime.start(app2)

    println("Running apps: ${runtime.getAllApps().size}")
    runtime.getAllApps().forEach { app ->
        println("- ${app.name}")
    }

    // Stop all apps via shutdown
    runtime.shutdown()
}

/**
 * Example with error handling.
 */
fun errorHandlingExample() = runBlocking {
    val runtime = AvaUIRuntime()

    // Test invalid DSL
    try {
        val invalidSource = """
            App {
                # Missing required 'id' field
                name: "Invalid App"
            }
        """.trimIndent()

        runtime.loadApp(invalidSource)
    } catch (e: RuntimeException) {
        println("Caught expected error: ${e.message}")
    }

    // Test duplicate app start
    try {
        val validSource = """
            App {
                id: "com.test.app"
                name: "Test App"
            }
        """.trimIndent()

        val app = runtime.loadApp(validSource)
        runtime.start(app)
        runtime.start(app)  // Should throw
    } catch (e: RuntimeException) {
        println("Caught expected error: ${e.message}")
    }

    runtime.shutdown()
}
