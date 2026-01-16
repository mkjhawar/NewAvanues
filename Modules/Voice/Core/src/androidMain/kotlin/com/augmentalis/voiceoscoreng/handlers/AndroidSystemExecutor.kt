/**
 * AndroidSystemExecutor.kt - Android system executor
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Android implementation of SystemExecutor using AccessibilityService.
 */
package com.augmentalis.voiceoscoreng.handlers

import android.accessibilityservice.AccessibilityService
import android.os.Build

/**
 * Android implementation of [SystemExecutor].
 *
 * Uses AccessibilityService global actions for system-level operations.
 *
 * @param accessibilityServiceProvider Provider for the accessibility service instance
 */
class AndroidSystemExecutor(
    private val accessibilityServiceProvider: () -> AccessibilityService?
) : SystemExecutor {

    override suspend fun goBack(): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    override suspend fun goHome(): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    override suspend fun showRecents(): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    override suspend fun showNotifications(): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    override suspend fun showQuickSettings(): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }

    override suspend fun showPowerMenu(): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
        } else {
            false
        }
    }

    override suspend fun lockScreen(): Boolean {
        val service = accessibilityServiceProvider() ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }
}
