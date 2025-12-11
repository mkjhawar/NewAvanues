/**
 * IElementCaptureService.aidl - AIDL interface for JIT Learning Service
 *
 * Defines the IPC interface between JIT service (always running in VoiceOSCore)
 * and LearnApp (standalone app for manual exploration).
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: JIT-LearnApp Separation (Phase 2)
 *
 * ## Purpose
 *
 * Allows LearnApp to coordinate with JIT service:
 * - Pause JIT capture during active exploration (avoid duplicates)
 * - Resume JIT capture after exploration completes
 * - Query JIT statistics for UI display
 * - Get list of learned screens (skip during exploration)
 *
 * @since 2.0.0 (JIT-LearnApp Separation)
 */

package com.augmentalis.jitlearning;

import com.augmentalis.jitlearning.JITState;

/**
 * Element Capture Service Interface
 *
 * IPC interface for controlling JIT screen capture from external apps.
 * Implemented by JITLearningService, called by LearnApp.
 */
interface IElementCaptureService {
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
}
