/**
 * IosSystemExecutor.kt - iOS implementation of SystemExecutor
 *
 * iOS cannot perform system-wide actions like Home, Recents, Notifications,
 * etc. from within an app sandbox. All methods return false with a
 * diagnostic NSLog message explaining the iOS limitation.
 *
 * goBack() is the only method with potential for in-app navigation
 * (via SwiftUI NavigationStack), but that requires Swift-side wiring
 * through the onSystemAction callback on VoiceOSCore.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

import platform.Foundation.NSLog

class IosSystemExecutor : SystemExecutor {

    override suspend fun goBack(): Boolean {
        // In-app back navigation is handled by SwiftUI NavigationStack.
        // The iOS app should wire VoiceOSCore.onSystemAction to pop the nav stack.
        NSLog("IosSystemExecutor: goBack() — delegating to onSystemAction callback")
        return false
    }

    override suspend fun goHome(): Boolean {
        NSLog("IosSystemExecutor: goHome() not available — iOS does not allow programmatic Home press")
        return false
    }

    override suspend fun showRecents(): Boolean {
        NSLog("IosSystemExecutor: showRecents() not available — iOS does not expose app switcher API")
        return false
    }

    override suspend fun showNotifications(): Boolean {
        NSLog("IosSystemExecutor: showNotifications() not available — iOS does not expose Notification Center API")
        return false
    }

    override suspend fun showQuickSettings(): Boolean {
        NSLog("IosSystemExecutor: showQuickSettings() not available — iOS does not expose Control Center API")
        return false
    }

    override suspend fun showPowerMenu(): Boolean {
        NSLog("IosSystemExecutor: showPowerMenu() not available — iOS does not expose power menu API")
        return false
    }

    override suspend fun lockScreen(): Boolean {
        NSLog("IosSystemExecutor: lockScreen() not available — iOS does not allow programmatic screen lock")
        return false
    }

    override suspend fun openAppDrawer(): Boolean {
        NSLog("IosSystemExecutor: openAppDrawer() not available — iOS has no app drawer concept")
        return false
    }
}
