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
package com.augmentalis.webavanue

import com.augmentalis.avid.AvidGlobalID

/**
 * WebAvanue-specific ID generator using AVID system
 */
object WebAvanueVuidGenerator {
    fun generateTabId(): String = AvidGlobalID.generateTabId()
    fun generateFavoriteId(): String = AvidGlobalID.generateFavoriteId()
    fun generateDownloadId(): String = AvidGlobalID.generateDownloadId()
    fun generateHistoryId(): String = AvidGlobalID.generateHistoryId()
    fun generateSessionId(): String = AvidGlobalID.generateSessionId()
    fun generateGroupId(): String = AvidGlobalID.generateGroupId()
    fun generateFolderId(): String = AvidGlobalID.generateGroupId()
}
