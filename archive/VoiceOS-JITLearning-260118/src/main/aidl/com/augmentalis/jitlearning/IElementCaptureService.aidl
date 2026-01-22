/**
 * IElementCaptureService.aidl - AIDL interface for JIT Learning Service
 *
 * Defines the IPC interface between JIT service (always running in VoiceOSCore)
 * and LearnApp (standalone app for manual exploration).
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Updated: 2025-12-11 (v2.0 - Event streaming + exploration commands)
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * ## Purpose
 *
 * Allows LearnApp to coordinate with JIT service:
 * - Pause JIT capture during active exploration (avoid duplicates)
 * - Resume JIT capture after exploration completes
 * - Query JIT statistics for UI display
 * - Get list of learned screens (skip during exploration)
 * - Stream accessibility events to LearnApp (v2.0)
 * - Execute exploration commands via JIT (v2.0)
 *
 * @since 2.0.0 (JIT-LearnApp Separation)
 */

package com.augmentalis.jitlearning;

import com.augmentalis.jitlearning.JITState;
import com.augmentalis.jitlearning.IAccessibilityEventListener;
import com.augmentalis.jitlearning.IExplorationProgressListener;
import com.augmentalis.jitlearning.ParcelableNodeInfo;
import com.augmentalis.jitlearning.ExplorationCommand;
import com.augmentalis.jitlearning.ExplorationProgress;
import com.augmentalis.jitlearning.ScreenChangeEvent;

/**
 * Element Capture Service Interface
 *
 * IPC interface for controlling JIT screen capture from external apps.
 * Implemented by JITLearningService, called by LearnApp.
 */
interface IElementCaptureService {

    // ================================================================
    // EXISTING METHODS (v1.0)
    // ================================================================

    /**
     * Pause JIT screen capture
     *
     * Use when LearnApp is actively exploring to avoid duplicate captures.
     * JIT service stops processing TYPE_WINDOW_STATE_CHANGED events.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms (IPC overhead)
     */
    void pauseCapture();

    /**
     * Resume JIT screen capture
     *
     * Call after LearnApp exploration completes to resume passive learning.
     * JIT service resumes processing TYPE_WINDOW_STATE_CHANGED events.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms (IPC overhead)
     */
    void resumeCapture();

    /**
     * Query current JIT state
     *
     * Returns active status, current package, and statistics.
     * Useful for LearnApp UI to show "JIT is learning X elements".
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms (IPC overhead)
     *
     * @return JITState parcelable with current state
     */
    JITState queryState();

    /**
     * Get list of screen hashes already learned by JIT
     *
     * LearnApp can skip these screens during exploration to avoid
     * redundant work. Screen hash is MD5 of view hierarchy.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~5-15ms (IPC overhead + database query)
     *
     * @param packageName App package name to query
     * @return List of screen hash strings (12-character MD5 prefixes)
     */
    List<String> getLearnedScreenHashes(in String packageName);

    // ================================================================
    // EVENT STREAMING (v2.0)
    // ================================================================

    /**
     * Register event listener for accessibility event streaming
     *
     * Allows LearnApp to receive real-time accessibility events from JIT.
     * Events include: screen changes, element actions, scroll events.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms (IPC overhead)
     *
     * @param listener IAccessibilityEventListener callback
     * @since 2.0.0
     */
    void registerEventListener(IAccessibilityEventListener listener);

    /**
     * Unregister event listener
     *
     * Stop receiving accessibility events. Should be called in onDestroy.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms (IPC overhead)
     *
     * @param listener Previously registered listener
     * @since 2.0.0
     */
    void unregisterEventListener(IAccessibilityEventListener listener);

    // ================================================================
    // SCREEN/ELEMENT QUERIES (v2.0)
    // ================================================================

    /**
     * Get current screen information
     *
     * Returns full screen content including all accessible elements.
     * Use for initial capture when exploration starts.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~10-50ms (depends on screen complexity)
     *
     * @return ParcelableNodeInfo with screen tree
     * @since 2.0.0
     */
    ParcelableNodeInfo getCurrentScreenInfo();

    /**
     * Get full menu content
     *
     * Queries complete menu items including those not currently visible.
     * Use for overflow menus, dropdowns, etc. to avoid re-scanning.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~5-20ms (depends on menu size)
     *
     * @param menuNodeId Node ID of the menu container
     * @return ParcelableNodeInfo with all menu items
     * @since 2.0.0
     */
    ParcelableNodeInfo getFullMenuContent(in String menuNodeId);

    /**
     * Query elements matching a selector
     *
     * Find elements by class name, resource ID pattern, or text.
     * Selector format: "class:Button" or "id:*submit*" or "text:Login"
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~5-30ms (depends on selector complexity)
     *
     * @param selector Query selector string
     * @return List of matching ParcelableNodeInfo objects
     * @since 2.0.0
     */
    List<ParcelableNodeInfo> queryElements(in String selector);

    // ================================================================
    // EXPLORATION COMMANDS (v2.0)
    // ================================================================

    /**
     * Perform click action on element
     *
     * Executes click via AccessibilityNodeInfo.performAction.
     * Used by LearnApp exploration engine.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~5-20ms (includes action + feedback)
     *
     * @param elementUuid UUID of element to click
     * @return true if action succeeded
     * @since 2.0.0
     */
    boolean performClick(in String elementUuid);

    /**
     * Perform scroll action
     *
     * Executes scroll in specified direction.
     * Direction: "up", "down", "left", "right"
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~10-30ms (includes scroll animation)
     *
     * @param direction Scroll direction
     * @param distance Scroll distance in pixels (0 for default)
     * @return true if scroll succeeded
     * @since 2.0.0
     */
    boolean performScroll(in String direction, int distance);

    /**
     * Perform generic exploration action
     *
     * Executes action defined in ExplorationCommand parcelable.
     * Supports: click, longClick, scroll, setText, swipe, back.
     *
     * Thread-safe: Can be called from any thread
     * Performance: varies by action type
     *
     * @param command ExplorationCommand with action details
     * @return true if action succeeded
     * @since 2.0.0
     */
    boolean performAction(in ExplorationCommand command);

    /**
     * Perform back navigation
     *
     * Executes GLOBAL_ACTION_BACK via AccessibilityService.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~5-15ms
     *
     * @return true if back action succeeded
     * @since 2.0.0
     */
    boolean performBack();

    // ================================================================
    // ELEMENT REGISTRATION (v2.0)
    // ================================================================

    /**
     * Register element with UUID
     *
     * Stores element-UUID mapping for later reference.
     * Required before performClick can be called.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms
     *
     * @param nodeInfo Element to register
     * @param uuid UUID to assign
     * @since 2.0.0
     */
    void registerElement(in ParcelableNodeInfo nodeInfo, in String uuid);

    /**
     * Clear registered elements
     *
     * Removes all element-UUID mappings.
     * Call when switching screens or ending exploration.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms
     *
     * @since 2.0.0
     */
    void clearRegisteredElements();

    // ================================================================
    // EXPLORATION SYNC (v2.1 - P2 Feature)
    // ================================================================

    /**
     * Start automated exploration of an app
     *
     * Triggers full automated exploration of the specified package.
     * Progress updates sent via IExplorationProgressListener.
     *
     * Thread-safe: Can be called from any thread
     * Performance: Async - returns immediately
     *
     * @param packageName Package name to explore
     * @return true if exploration started successfully
     * @since 2.1.0
     */
    boolean startExploration(in String packageName);

    /**
     * Stop current exploration
     *
     * Cancels ongoing exploration immediately.
     * Final progress sent via listener's onCompleted or onFailed.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~5-10ms
     *
     * @since 2.1.0
     */
    void stopExploration();

    /**
     * Pause current exploration
     *
     * Temporarily pauses exploration. Can be resumed with resumeExploration().
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms
     *
     * @since 2.1.0
     */
    void pauseExploration();

    /**
     * Resume paused exploration
     *
     * Resumes a previously paused exploration.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms
     *
     * @since 2.1.0
     */
    void resumeExploration();

    /**
     * Get current exploration progress
     *
     * Returns current exploration state and progress.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms
     *
     * @return ExplorationProgress with current state
     * @since 2.1.0
     */
    ExplorationProgress getExplorationProgress();

    /**
     * Register exploration progress listener
     *
     * Receive exploration progress updates.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms
     *
     * @param listener IExplorationProgressListener callback
     * @since 2.1.0
     */
    void registerExplorationListener(IExplorationProgressListener listener);

    /**
     * Unregister exploration progress listener
     *
     * Stop receiving exploration progress updates.
     *
     * Thread-safe: Can be called from any thread
     * Performance: ~2-5ms
     *
     * @param listener Previously registered listener
     * @since 2.1.0
     */
    void unregisterExplorationListener(IExplorationProgressListener listener);
}
