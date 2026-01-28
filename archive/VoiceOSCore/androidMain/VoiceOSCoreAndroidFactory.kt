/**
 * VoiceOSCoreAndroidFactory.kt - Android factory extensions for VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Provides Android-specific factory functions for creating VoiceOSCore instances.
 */
package com.augmentalis.voiceoscore

import android.accessibilityservice.AccessibilityService
import android.util.Log

private const val TAG = "VoiceOSFactory"

/**
 * Create a VoiceOSCore instance configured for Android.
 *
 * This is the primary entry point for Android apps using VoiceOSCore.
 * It sets up Android-specific speech engines, handlers, and configuration.
 *
 * @param service The accessibility service for gesture dispatch
 * @param configuration Service configuration options
 * @param commandRegistry Shared command registry (optional, creates new if null)
 * @return Configured VoiceOSCore instance
 */
fun VoiceOSCore.Companion.createForAndroid(
    service: AccessibilityService,
    configuration: ServiceConfiguration = ServiceConfiguration.DEFAULT,
    commandRegistry: CommandRegistry? = null
): VoiceOSCore {
    // Get speech engine factory from provider
    val speechEngineFactory = SpeechEngineFactoryProvider.create()

    // Create Android handler factory with the accessibility service
    val handlerFactory = AndroidHandlerFactory(service)

    // Build the VoiceOSCore instance
    return VoiceOSCore.Builder()
        .withHandlerFactory(handlerFactory)
        .withSpeechEngineFactory(speechEngineFactory)
        .withConfiguration(configuration)
        .apply {
            commandRegistry?.let { withCommandRegistry(it) }
        }
        .build()
}

/**
 * Android-specific handler factory.
 *
 * Creates handlers appropriate for Android accessibility service context.
 */
internal class AndroidHandlerFactory(
    private val service: AccessibilityService
) : HandlerFactory {

    override fun createHandlers(): List<IHandler> {
        return listOf(
            AndroidGestureHandler(service),
            SystemHandler(AndroidSystemExecutor(service)),
            AppHandler(AndroidAppLauncher(service))
        )
    }
}

/**
 * Android implementation of SystemExecutor.
 * Uses AccessibilityService global actions for system commands.
 */
internal class AndroidSystemExecutor(
    private val service: AccessibilityService
) : SystemExecutor {

    override suspend fun goBack(): Boolean {
        Log.d(TAG, "Executing goBack")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    override suspend fun goHome(): Boolean {
        Log.d(TAG, "Executing goHome")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    override suspend fun showRecents(): Boolean {
        Log.d(TAG, "Executing showRecents")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    override suspend fun showNotifications(): Boolean {
        Log.d(TAG, "Executing showNotifications")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    override suspend fun showQuickSettings(): Boolean {
        Log.d(TAG, "Executing showQuickSettings")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }

    override suspend fun showPowerMenu(): Boolean {
        Log.d(TAG, "Executing showPowerMenu")
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
        } else {
            false
        }
    }

    override suspend fun lockScreen(): Boolean {
        Log.d(TAG, "Executing lockScreen")
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }
}

/**
 * Gesture handler that implements IHandler interface for Android.
 *
 * Handles gesture-based commands using AndroidGestureDispatcher.
 * Supports scroll, tap (with coordinates in params), and global actions.
 * Uses BoundsResolver for layered bounds resolution to handle stale cached bounds.
 */
internal class AndroidGestureHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    private val dispatcher = AndroidGestureDispatcher(service)
    private val boundsResolver = BoundsResolver(service)

    override val category: ActionCategory = ActionCategory.NAVIGATION

    override val supportedActions: List<String> = listOf(
        "tap", "click", "press", "select",
        "long press", "long click", "hold",
        "scroll up", "scroll down", "scroll left", "scroll right",
        "swipe up", "swipe down", "swipe left", "swipe right"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "AndroidGestureHandler.execute: phrase='$phrase', actionType=${command.actionType}")

        return try {
            // First try phrase-based routing for scroll/swipe commands
            // This handles commands that come in with EXECUTE actionType
            when {
                phrase.startsWith("scroll down") || phrase.startsWith("swipe up") -> {
                    val success = dispatcher.scroll("down")
                    return if (success) HandlerResult.success("Scrolled down")
                           else HandlerResult.failure("Failed to scroll down")
                }
                phrase.startsWith("scroll up") || phrase.startsWith("swipe down") -> {
                    val success = dispatcher.scroll("up")
                    return if (success) HandlerResult.success("Scrolled up")
                           else HandlerResult.failure("Failed to scroll up")
                }
                phrase.startsWith("scroll left") || phrase.startsWith("swipe right") -> {
                    val success = dispatcher.scroll("left")
                    return if (success) HandlerResult.success("Scrolled left")
                           else HandlerResult.failure("Failed to scroll left")
                }
                phrase.startsWith("scroll right") || phrase.startsWith("swipe left") -> {
                    val success = dispatcher.scroll("right")
                    return if (success) HandlerResult.success("Scrolled right")
                           else HandlerResult.failure("Failed to scroll right")
                }
            }

            // Then route by actionType
            when (command.actionType) {
                CommandActionType.TAP, CommandActionType.CLICK -> {
                    Log.d(TAG, "Executing TAP/CLICK for '${command.phrase}', metadata: ${command.metadata}")
                    // Check if coordinates are provided in params (direct tap)
                    val x = params["x"] as? Float
                    val y = params["y"] as? Float
                    if (x != null && y != null) {
                        Log.d(TAG, "Tapping with coords: ($x, $y)")
                        val success = dispatcher.tap(x, y)
                        if (success) {
                            HandlerResult.success("Tapped ${command.phrase}")
                        } else {
                            HandlerResult.failure("Failed to tap")
                        }
                    } else {
                        // Use BoundsResolver for layered bounds resolution
                        // This handles stale cached bounds by trying multiple strategies
                        val bounds = boundsResolver.resolve(command)
                        if (bounds != null) {
                            Log.d(TAG, "Clicking with resolved bounds: ${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}")
                            val success = dispatcher.click(bounds)
                            if (success) {
                                Log.d(TAG, "Click succeeded for '${command.phrase}'")
                                HandlerResult.success("Clicked ${command.phrase}")
                            } else {
                                Log.w(TAG, "Click failed for '${command.phrase}'")
                                HandlerResult.failure("Failed to click")
                            }
                        } else {
                            Log.w(TAG, "BoundsResolver failed for '${command.phrase}', returning notHandled")
                            HandlerResult.notHandled()
                        }
                    }
                }

                CommandActionType.LONG_CLICK -> {
                    val x = params["x"] as? Float
                    val y = params["y"] as? Float
                    if (x != null && y != null) {
                        val success = dispatcher.longPress(x, y)
                        if (success) {
                            HandlerResult.success("Long pressed ${command.phrase}")
                        } else {
                            HandlerResult.failure("Failed to long press")
                        }
                    } else {
                        // Use BoundsResolver for layered bounds resolution
                        val bounds = boundsResolver.resolve(command)
                        if (bounds != null) {
                            val centerX = bounds.centerX.toFloat()
                            val centerY = bounds.centerY.toFloat()
                            val success = dispatcher.longPress(centerX, centerY)
                            if (success) {
                                HandlerResult.success("Long pressed ${command.phrase}")
                            } else {
                                HandlerResult.failure("Failed to long press")
                            }
                        } else {
                            HandlerResult.notHandled()
                        }
                    }
                }

                CommandActionType.SCROLL_DOWN -> {
                    val success = dispatcher.scroll("down")
                    if (success) {
                        HandlerResult.success("Scrolled down")
                    } else {
                        HandlerResult.failure("Failed to scroll down")
                    }
                }

                CommandActionType.SCROLL_UP -> {
                    val success = dispatcher.scroll("up")
                    if (success) {
                        HandlerResult.success("Scrolled up")
                    } else {
                        HandlerResult.failure("Failed to scroll up")
                    }
                }

                CommandActionType.SCROLL_LEFT -> {
                    val success = dispatcher.scroll("left")
                    if (success) {
                        HandlerResult.success("Scrolled left")
                    } else {
                        HandlerResult.failure("Failed to scroll left")
                    }
                }

                CommandActionType.SCROLL_RIGHT -> {
                    val success = dispatcher.scroll("right")
                    if (success) {
                        HandlerResult.success("Scrolled right")
                    } else {
                        HandlerResult.failure("Failed to scroll right")
                    }
                }

                CommandActionType.BACK -> {
                    val success = dispatcher.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_BACK
                    )
                    if (success) {
                        HandlerResult.success("Navigated back")
                    } else {
                        HandlerResult.failure("Failed to go back")
                    }
                }

                CommandActionType.HOME -> {
                    val success = dispatcher.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_HOME
                    )
                    if (success) {
                        HandlerResult.success("Navigated home")
                    } else {
                        HandlerResult.failure("Failed to go home")
                    }
                }

                CommandActionType.RECENT_APPS -> {
                    val success = dispatcher.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_RECENTS
                    )
                    if (success) {
                        HandlerResult.success("Opened recent apps")
                    } else {
                        HandlerResult.failure("Failed to open recents")
                    }
                }

                CommandActionType.NOTIFICATIONS -> {
                    val success = dispatcher.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                    )
                    if (success) {
                        HandlerResult.success("Opened notifications")
                    } else {
                        HandlerResult.failure("Failed to open notifications")
                    }
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            HandlerResult.failure("Error executing command: ${e.message}")
        }
    }
}
