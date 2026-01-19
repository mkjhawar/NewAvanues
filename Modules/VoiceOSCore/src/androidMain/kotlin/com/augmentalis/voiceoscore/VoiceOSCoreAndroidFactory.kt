/**
 * VoiceOSCoreAndroidFactory.kt - Android factory extensions for VoiceOSCoreNG
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Provides Android-specific factory functions for creating VoiceOSCoreNG instances.
 */
package com.augmentalis.voiceoscore

import android.accessibilityservice.AccessibilityService

/**
 * Create a VoiceOSCoreNG instance configured for Android.
 *
 * This is the primary entry point for Android apps using VoiceOSCore.
 * It sets up Android-specific speech engines, handlers, and configuration.
 *
 * @param service The accessibility service for gesture dispatch
 * @param configuration Service configuration options
 * @param commandRegistry Shared command registry (optional, creates new if null)
 * @return Configured VoiceOSCoreNG instance
 */
fun VoiceOSCoreNG.Companion.createForAndroid(
    service: AccessibilityService,
    configuration: ServiceConfiguration = ServiceConfiguration.DEFAULT,
    commandRegistry: CommandRegistry? = null
): VoiceOSCoreNG {
    // Get speech engine factory from provider
    val speechEngineFactory = SpeechEngineFactoryProvider.create()

    // Get NLU processor if available
    val nluProcessor = try {
        NluProcessorFactory.create(NluConfig.DEFAULT)
    } catch (e: Exception) {
        null
    }

    // Get LLM processor if available
    val llmProcessor = try {
        LlmProcessorFactory.create(LlmConfig.DEFAULT)
    } catch (e: Exception) {
        null
    }

    // Create Android handler factory with the accessibility service
    val handlerFactory = AndroidHandlerFactory(service)

    // Build the VoiceOSCoreNG instance
    return VoiceOSCoreNG.Builder()
        .withHandlerFactory(handlerFactory)
        .withSpeechEngineFactory(speechEngineFactory)
        .withConfiguration(configuration)
        .apply {
            commandRegistry?.let { withCommandRegistry(it) }
            nluProcessor?.let { withNluProcessor(it) }
            llmProcessor?.let { withLlmProcessor(it) }
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
            AndroidGestureHandler(service)
        )
    }
}

/**
 * Gesture handler that implements IHandler interface for Android.
 *
 * Handles gesture-based commands using AndroidGestureDispatcher.
 * Supports scroll, tap (with coordinates in params), and global actions.
 */
internal class AndroidGestureHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    private val dispatcher = AndroidGestureDispatcher(service)

    override val category: ActionCategory = ActionCategory.NAVIGATION

    override val supportedActions: List<String> = listOf(
        "tap", "click", "long press", "scroll up", "scroll down",
        "scroll left", "scroll right", "swipe", "back", "home"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        return try {
            when (command.actionType) {
                CommandActionType.TAP, CommandActionType.CLICK -> {
                    // Check if coordinates are provided in params
                    val x = params["x"] as? Float
                    val y = params["y"] as? Float
                    if (x != null && y != null) {
                        val success = dispatcher.tap(x, y)
                        if (success) {
                            HandlerResult.success("Tapped ${command.phrase}")
                        } else {
                            HandlerResult.failure("Failed to tap")
                        }
                    } else {
                        // Check metadata for bounds
                        val bounds = parseBoundsFromMetadata(command.metadata)
                        if (bounds != null) {
                            val success = dispatcher.click(bounds)
                            if (success) {
                                HandlerResult.success("Clicked ${command.phrase}")
                            } else {
                                HandlerResult.failure("Failed to click")
                            }
                        } else {
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
                        HandlerResult.notHandled()
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

    /**
     * Parse bounds from command metadata if present.
     * Expected format: "left,top,right,bottom"
     */
    private fun parseBoundsFromMetadata(metadata: Map<String, String>): Bounds? {
        val boundsStr = metadata["bounds"] ?: return null
        return try {
            val parts = boundsStr.split(",").map { it.trim().toInt() }
            if (parts.size == 4) {
                Bounds(parts[0], parts[1], parts[2], parts[3])
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
