/**
 * DesktopHandlerFactory.kt - Desktop-specific handler factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Factory for creating handlers with Desktop-specific executors.
 * TODO: Implement using AWT Robot or platform-specific APIs.
 */
package com.augmentalis.voiceoscoreng

import com.augmentalis.voiceoscoreng.handlers.*

/**
 * Desktop (JVM) implementation of [HandlerFactory].
 *
 * Creates handlers with AWT Robot-based executors.
 * Currently uses stub executors - TODO: Implement real Desktop support.
 */
class DesktopHandlerFactory : HandlerFactory {

    override fun createHandlers(): List<IHandler> {
        // Create stub executors (TODO: Implement real Desktop support)
        val navigationExecutor = DesktopNavigationExecutor()
        val uiExecutor = DesktopUIExecutor()
        val inputExecutor = DesktopInputExecutor()
        val systemExecutor = DesktopSystemExecutor()

        // Create handlers with executors
        return listOf(
            SystemHandler(systemExecutor),
            NavigationHandler(navigationExecutor),
            UIHandler(uiExecutor),
            InputHandler(inputExecutor)
        )
    }
}
