/**
 * IFrameworkDetector.kt - Interface for app framework detection
 *
 * Handles detection of app frameworks with LRU caching.
 * Extracted from LearnAppCore as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 1.2.0 (SOLID Refactoring)
 */

package com.augmentalis.learnappcore.processors

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.learnappcore.detection.AppFramework

/**
 * Framework Detector Interface
 *
 * Responsibilities:
 * - Detect app framework (Native, React Native, Flutter, etc.)
 * - Cache detection results per package
 * - Clear stale cache
 *
 * Single Responsibility: Framework detection and caching
 */
interface IFrameworkDetector {
    /**
     * Detect app framework for package.
     *
     * Uses cache if available, otherwise performs detection.
     *
     * @param packageName Package to detect
     * @param node Sample accessibility node
     * @return Detected framework
     */
    suspend fun detectFramework(packageName: String, node: AccessibilityNodeInfo?): AppFramework

    /**
     * Clear framework cache.
     *
     * Forces re-detection on next call.
     */
    fun clearCache()

    /**
     * Get cache size.
     *
     * @return Number of cached packages
     */
    fun getCacheSize(): Int
}
