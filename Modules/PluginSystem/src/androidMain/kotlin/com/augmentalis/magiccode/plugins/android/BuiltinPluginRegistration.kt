/**
 * BuiltinPluginRegistration.kt - Registers all built-in handler plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Provides a single point of registration for all built-in handler plugins
 * with their Android-specific executor implementations.
 */
package com.augmentalis.magiccode.plugins.android

import android.util.Log
import com.augmentalis.magiccode.plugins.android.executors.*
import com.augmentalis.magiccode.plugins.builtin.*

/**
 * Registers all built-in handler plugins with the AndroidPluginHost.
 *
 * This utility class provides:
 * - Registration of all first-party handler plugins
 * - Lazy executor binding (executors created when plugins initialize)
 * - Easy extension point for adding new plugins
 *
 * ## Usage
 * ```kotlin
 * // In Application.onCreate() or after plugin host initialization
 * BuiltinPluginRegistration.registerAll(pluginHost)
 * ```
 */
object BuiltinPluginRegistration {

    private const val TAG = "BuiltinPluginReg"

    /**
     * All built-in plugin IDs for reference.
     */
    val PLUGIN_IDS = listOf(
        NavigationHandlerPlugin.PLUGIN_ID,
        UIInteractionPlugin.PLUGIN_ID,
        TextInputPlugin.PLUGIN_ID,
        SystemCommandPlugin.PLUGIN_ID,
        GesturePlugin.PLUGIN_ID,
        SelectionPlugin.PLUGIN_ID,
        AppLauncherPlugin.PLUGIN_ID
    )

    /**
     * Register all built-in handler plugins with the plugin host.
     *
     * Executors are lazily created when plugins initialize, allowing
     * platform services (like AccessibilityService) to be registered
     * after plugin factories are set up.
     *
     * @param pluginHost The AndroidPluginHost to register plugins with
     */
    fun registerAll(pluginHost: AndroidPluginHost) {
        Log.i(TAG, "Registering ${PLUGIN_IDS.size} built-in handler plugins...")

        val serviceRegistry = pluginHost.serviceRegistry

        // Navigation Handler Plugin
        pluginHost.registerBuiltinPluginFactory(NavigationHandlerPlugin.PLUGIN_ID) {
            NavigationHandlerPlugin {
                AndroidNavigationExecutor(serviceRegistry)
            }
        }

        // UI Interaction Handler Plugin
        pluginHost.registerBuiltinPluginFactory(UIInteractionPlugin.PLUGIN_ID) {
            UIInteractionPlugin {
                AndroidUIInteractionExecutor(serviceRegistry)
            }
        }

        // Text Input Handler Plugin
        pluginHost.registerBuiltinPluginFactory(TextInputPlugin.PLUGIN_ID) {
            TextInputPlugin {
                AndroidTextInputExecutor(serviceRegistry)
            }
        }

        // System Command Handler Plugin
        pluginHost.registerBuiltinPluginFactory(SystemCommandPlugin.PLUGIN_ID) {
            SystemCommandPlugin {
                AndroidSystemCommandExecutor(serviceRegistry)
            }
        }

        // Gesture Handler Plugin
        pluginHost.registerBuiltinPluginFactory(GesturePlugin.PLUGIN_ID) {
            GesturePlugin {
                AndroidGestureExecutor(serviceRegistry)
            }
        }

        // Selection Handler Plugin
        // SelectionPlugin takes two providers: clipboard and selection executor
        // We use separate implementations to avoid method signature conflicts
        pluginHost.registerBuiltinPluginFactory(SelectionPlugin.PLUGIN_ID) {
            SelectionPlugin(
                clipboardProvider = { AndroidClipboardProvider(serviceRegistry) },
                selectionExecutor = { AndroidSelectionExecutor(serviceRegistry) }
            )
        }

        // App Launcher Handler Plugin
        pluginHost.registerBuiltinPluginFactory(AppLauncherPlugin.PLUGIN_ID) {
            AppLauncherPlugin {
                AndroidAppLauncherExecutor(serviceRegistry)
            }
        }

        Log.i(TAG, "All built-in plugins registered")
    }

    /**
     * Register only essential plugins for minimal footprint.
     *
     * Registers: Navigation, UI Interaction, Text Input
     *
     * @param pluginHost The AndroidPluginHost to register plugins with
     */
    fun registerEssential(pluginHost: AndroidPluginHost) {
        Log.i(TAG, "Registering essential handler plugins...")

        val serviceRegistry = pluginHost.serviceRegistry

        pluginHost.registerBuiltinPluginFactory(NavigationHandlerPlugin.PLUGIN_ID) {
            NavigationHandlerPlugin {
                AndroidNavigationExecutor(serviceRegistry)
            }
        }

        pluginHost.registerBuiltinPluginFactory(UIInteractionPlugin.PLUGIN_ID) {
            UIInteractionPlugin {
                AndroidUIInteractionExecutor(serviceRegistry)
            }
        }

        pluginHost.registerBuiltinPluginFactory(TextInputPlugin.PLUGIN_ID) {
            TextInputPlugin {
                AndroidTextInputExecutor(serviceRegistry)
            }
        }

        Log.i(TAG, "Essential plugins registered (3 of ${PLUGIN_IDS.size})")
    }
}
