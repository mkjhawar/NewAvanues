/**
 * IServiceContext.kt - Interface for service-level operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Purpose: Segregated interface for service context (Interface Segregation Principle)
 * Part of Phase 1: SOLID Refactoring - Interface Segregation
 */

package com.augmentalis.voiceoscore.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Interface for service-level operations
 *
 * Provides access to core service context and accessibility operations.
 * Focused interface following Interface Segregation Principle.
 *
 * @see IVoiceOSContext Aggregate interface combining all contexts
 */
interface IServiceContext {

    /**
     * Application context
     */
    val context: Context

    /**
     * Accessibility service instance
     */
    val accessibilityService: AccessibilityService

    /**
     * Get package manager for app operations
     *
     * @return PackageManager instance
     */
    fun getPackageManager(): PackageManager = context.packageManager

    /**
     * Get root accessibility node of active window
     *
     * @return Root node or null if no active window
     */
    fun getRootNodeInActiveWindow(): AccessibilityNodeInfo? = accessibilityService.rootInActiveWindow

    /**
     * Perform global accessibility action
     *
     * @param action Global action constant from AccessibilityService
     * @return true if action was performed
     */
    fun performGlobalAction(action: Int): Boolean

    /**
     * Get system service by name
     *
     * @param name Service name constant from Context
     * @return Service instance or null
     */
    fun getSystemService(name: String): Any?
}
