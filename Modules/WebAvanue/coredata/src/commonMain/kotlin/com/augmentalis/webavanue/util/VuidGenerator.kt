/**
 * VuidGenerator.kt - Centralized ID generation for WebAvanue
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-30
 *
 * Responsibility: Provide WebAvanue-specific VUID generation using shared KMP library
 *
 * ## VUID Format (Compact DNS-Style):
 * ```
 * web:{typeAbbrev}:{hash8}
 * Example: web:tab:a7f3e2c1
 * ```
 *
 * ## Benefits:
 * - Consistent IDs across all WebAvanue domain models
 * - Human readable and debuggable
 * - Compact (16-20 chars vs 50+ legacy format)
 * - Fully reversible to verbose format
 */
package com.augmentalis.webavanue.util

import com.augmentalis.voiceoscoreng.common.VUIDGenerator

/**
 * WebAvanue-specific VUID generator using shared KMP library
 *
 * Provides type-safe ID generation for all WebAvanue domain entities:
 * - Tab: web:tab:xxxxxxxx
 * - Favorite: web:fav:xxxxxxxx
 * - Download: web:dwn:xxxxxxxx
 * - History: web:hst:xxxxxxxx
 * - Session: web:ses:xxxxxxxx
 * - Group: web:grp:xxxxxxxx
 */
object WebAvanueVuidGenerator {

    /**
     * Generate VUID for browser tabs
     * Format: web:tab:xxxxxxxx
     */
    fun generateTabId(): String = VUIDGenerator.generateTabVuid()

    /**
     * Generate VUID for favorites/bookmarks
     * Format: web:fav:xxxxxxxx
     */
    fun generateFavoriteId(): String = VUIDGenerator.generateFavoriteVuid()

    /**
     * Generate VUID for downloads
     * Format: web:dwn:xxxxxxxx
     */
    fun generateDownloadId(): String = VUIDGenerator.generateDownloadVuid()

    /**
     * Generate VUID for history entries
     * Format: web:hst:xxxxxxxx
     */
    fun generateHistoryId(): String = VUIDGenerator.generateHistoryVuid()

    /**
     * Generate VUID for browser sessions
     * Format: web:ses:xxxxxxxx
     */
    fun generateSessionId(): String = VUIDGenerator.generateSessionVuid()

    /**
     * Generate VUID for tab groups
     * Format: web:grp:xxxxxxxx
     */
    fun generateGroupId(): String = VUIDGenerator.generateGroupVuid()

    /**
     * Generate VUID for favorite folders
     * Format: web:grp:xxxxxxxx (reuses group type)
     */
    fun generateFolderId(): String = VUIDGenerator.generateGroupVuid()
}
