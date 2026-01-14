/**
 * IVoiceOSContext.kt - Interface providing context for action handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-17
 *
 * Purpose: Abstracts VoiceOSService dependencies for testability and SOLID compliance (DIP)
 * Allows handlers to depend on interface rather than concrete service implementation
 */
package com.augmentalis.voiceoscore.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.cursor.core.CursorOffset

/**
 * Interface providing context for action handlers
 *
 * Aggregate interface combining all context interfaces.
 * Abstracts VoiceOSService dependencies for testability and SOLID compliance.
 *
 * ## Interface Segregation Principle (ISP)
 *
 * Extends 4 segregated interfaces following Interface Segregation Principle:
 * - IServiceContext: Service-level operations (accessibility, system services)
 * - IDatabaseContext: Database access operations
 * - IUIContext: UI operations (overlays, toasts, vibration)
 * - ISpeechContext: Speech and cursor operations
 *
 * Phase 1: SOLID Refactoring - Interface Segregation
 * Clients can depend on specific context interfaces instead of this aggregate interface.
 *
 * VOS4 Exception: Interface justified for Dependency Inversion Principle
 * - Allows handlers to depend on abstraction instead of concrete VoiceOSService
 * - Enables testing with mock implementations
 * - Follows SOLID principle: Depend on abstractions, not concretions
 *
 * ## Liskov Substitution Principle (LSP) Contract
 *
 * All implementations MUST adhere to the following behavioral contracts:
 *
 * ### Property Contracts
 * - context: MUST be valid application context (never null)
 * - accessibilityService: MUST be active AccessibilityService instance
 * - windowManager: MUST be valid WindowManager instance
 *
 * ### Nullable Return Contracts
 * - getRootNodeInActiveWindow(): Returns null when no active window
 * - getSystemService(): Returns null when service not available
 * - getCursorPosition(): MUST NOT return null (returns default if cursor unavailable)
 *
 * ### Boolean Return Contracts
 * - performGlobalAction(): Returns false when action cannot be performed
 * - isCursorVisible(): Returns false when cursor feature disabled/unavailable
 *
 * ### Exception Behavior
 * - Methods MUST NOT throw exceptions for normal operation failures
 * - Only system-level errors should propagate as exceptions
 * - Service unavailability returns null/false (NOT throws)
 *
 * ### Thread Safety
 * - All methods MUST be callable from any thread
 * - Implementations handle thread-switching internally as needed
 * - No blocking operations on main thread
 *
 * @see IServiceContext
 * @see IDatabaseContext
 * @see IUIContext
 * @see ISpeechContext
 */
interface IVoiceOSContext :
    IServiceContext,
    IDatabaseContext,
    IUIContext,
    ISpeechContext {

    // All methods inherited from segregated interfaces:
    // - IServiceContext: context, accessibilityService, getPackageManager(), getRootNodeInActiveWindow(), performGlobalAction(), getSystemService()
    // - IDatabaseContext: getDatabaseManager()
    // - IUIContext: windowManager, showToast(), vibrate()
    // - ISpeechContext: getAppCommands(), startActivity(), isCursorVisible(), getCursorPosition()
    //
    // No additional methods needed - this is a pure aggregate interface for backward compatibility
}
