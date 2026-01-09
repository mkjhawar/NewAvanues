/**
 * StubExecutors.kt - iOS stub executors
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Stub implementations of executors for iOS platform.
 * TODO: Implement using UIAccessibility APIs.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * iOS stub for [NavigationExecutor].
 * TODO: Implement using UIAccessibility.
 */
class IOSNavigationExecutor : NavigationExecutor {
    override suspend fun scrollUp(): Boolean = false
    override suspend fun scrollDown(): Boolean = false
    override suspend fun scrollLeft(): Boolean = false
    override suspend fun scrollRight(): Boolean = false
    override suspend fun next(): Boolean = false
    override suspend fun previous(): Boolean = false
}

/**
 * iOS stub for [UIExecutor].
 * TODO: Implement using UIAccessibility.
 */
class IOSUIExecutor : UIExecutor {
    // Element discovery for disambiguation
    override suspend fun getScreenElements(): List<ElementInfo> = emptyList()

    // Direct element actions (used after disambiguation)
    override suspend fun clickElement(element: ElementInfo): Boolean = false
    override suspend fun longClickElement(element: ElementInfo): Boolean = false
    override suspend fun doubleClickElement(element: ElementInfo): Boolean = false

    // Legacy text/VUID actions
    override suspend fun clickByText(text: String): Boolean = false
    override suspend fun clickByVuid(vuid: String): Boolean = false
    override suspend fun longClickByText(text: String): Boolean = false
    override suspend fun longClickByVuid(vuid: String): Boolean = false
    override suspend fun doubleClickByText(text: String): Boolean = false
    override suspend fun expand(target: String): Boolean = false
    override suspend fun collapse(target: String): Boolean = false
    override suspend fun setChecked(target: String, checked: Boolean): Boolean = false
    override suspend fun toggle(target: String): Boolean = false
    override suspend fun focus(target: String): Boolean = false
    override suspend fun dismiss(): Boolean = false
}

/**
 * iOS stub for [InputExecutor].
 * TODO: Implement using UIAccessibility.
 */
class IOSInputExecutor : InputExecutor {
    override suspend fun enterText(text: String): Boolean = false
    override suspend fun deleteCharacter(): Boolean = false
    override suspend fun clearText(): Boolean = false
    override suspend fun selectAll(): Boolean = false
    override suspend fun copy(): Boolean = false
    override suspend fun cut(): Boolean = false
    override suspend fun paste(): Boolean = false
    override suspend fun undo(): Boolean = false
    override suspend fun redo(): Boolean = false
    override suspend fun search(query: String): Boolean = false
}

/**
 * iOS stub for [SystemExecutor].
 * TODO: Implement using UIApplication.
 */
class IOSSystemExecutor : SystemExecutor {
    override suspend fun goBack(): Boolean = false
    override suspend fun goHome(): Boolean = false
    override suspend fun showRecents(): Boolean = false
    override suspend fun showNotifications(): Boolean = false
    override suspend fun showQuickSettings(): Boolean = false
    override suspend fun showPowerMenu(): Boolean = false
    override suspend fun lockScreen(): Boolean = false
}
