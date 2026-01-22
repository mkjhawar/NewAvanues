/**
 * AndroidSystemCommandExecutor.kt - Android implementation of SystemCommandExecutor
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Bridges the SystemCommandPlugin to Android AccessibilityService for
 * system-level actions like back, home, recents, etc.
 */
package com.augmentalis.magiccode.plugins.android.executors

import android.accessibilityservice.AccessibilityService
import android.os.Build
import com.augmentalis.magiccode.plugins.android.ServiceRegistry
import com.augmentalis.magiccode.plugins.builtin.SystemCommandExecutor

/**
 * Android implementation of SystemCommandExecutor.
 *
 * Uses AccessibilityService.performGlobalAction() for system commands.
 *
 * @param serviceRegistry Registry to retrieve AccessibilityService from
 */
class AndroidSystemCommandExecutor(
    private val serviceRegistry: ServiceRegistry
) : SystemCommandExecutor {

    private val accessibilityService: AccessibilityService?
        get() = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

    override suspend fun goBack(): Boolean {
        val service = accessibilityService ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    override suspend fun goHome(): Boolean {
        val service = accessibilityService ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    override suspend fun showRecents(): Boolean {
        val service = accessibilityService ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    override suspend fun showNotifications(): Boolean {
        val service = accessibilityService ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    override suspend fun showQuickSettings(): Boolean {
        val service = accessibilityService ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }

    override suspend fun showPowerMenu(): Boolean {
        val service = accessibilityService ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
        } else {
            false
        }
    }

    override suspend fun lockScreen(): Boolean {
        val service = accessibilityService ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }
}
