/**
 * VuidGenerator.kt - WebAvanue ID generation using AVID
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-30
 * Updated: 2026-01-13 - Migrated to AVID
 *
 * Provides WebAvanue-specific ID generation using the unified AVID system.
 * Format: AVID-{platform}-{sequence} (e.g., AVID-A-000001)
 */
package com.augmentalis.webavanue.util

import com.augmentalis.avid.AvidGenerator

/**
 * WebAvanue-specific ID generator using AVID system
 */
object WebAvanueVuidGenerator {
    fun generateTabId(): String = AvidGenerator.generateTabId()
    fun generateFavoriteId(): String = AvidGenerator.generateFavoriteId()
    fun generateDownloadId(): String = AvidGenerator.generateDownloadId()
    fun generateHistoryId(): String = AvidGenerator.generateHistoryId()
    fun generateSessionId(): String = AvidGenerator.generateSessionId()
    fun generateGroupId(): String = AvidGenerator.generateGroupId()
    fun generateFolderId(): String = AvidGenerator.generateGroupId()
}
