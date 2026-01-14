/**
 * IOSHandlerFactory.kt - iOS-specific handler factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Factory for creating handlers with iOS-specific executors.
 * TODO: Implement using UIAccessibility APIs.
 */
package com.augmentalis.voiceoscoreng

import com.augmentalis.voiceoscoreng.handlers.*

/**
 * iOS implementation of [HandlerFactory].
 *
 * Creates handlers with iOS UIAccessibility-based executors.
 * Currently uses stub executors - TODO: Implement real iOS support.
 */
class IOSHandlerFactory : HandlerFactory {

    override fun createHandlers(): List<IHandler> {
        // Create stub executors (TODO: Implement real iOS support)
        val navigationExecutor = IOSNavigationExecutor()
        val uiExecutor = IOSUIExecutor()
        val inputExecutor = IOSInputExecutor()
        val systemExecutor = IOSSystemExecutor()

        // Create handlers with executors
        return listOf(
            SystemHandler(systemExecutor),
            NavigationHandler(navigationExecutor),
            UIHandler(uiExecutor),
            InputHandler(inputExecutor)
        )
    }
}
