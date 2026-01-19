/**
 * IDatabaseContext.kt - Interface for database access
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Purpose: Segregated interface for database context (Interface Segregation Principle)
 * Part of Phase 1: SOLID Refactoring - Interface Segregation
 */

package com.augmentalis.voiceoscore.accessibility

import com.augmentalis.voiceoscore.accessibility.managers.DatabaseManager

/**
 * Interface for database access operations
 *
 * Provides access to database manager for persistence operations.
 * Focused interface following Interface Segregation Principle.
 *
 * @see IVoiceOSContext Aggregate interface combining all contexts
 */
interface IDatabaseContext {

    /**
     * Get database manager
     *
     * Handles all database operations including:
     * - VoiceOSDatabaseManager (SQLDelight repositories)
     * - VoiceOSAppDatabase (scraping database adapter)
     * - Database lifecycle management
     *
     * @return DatabaseManager instance
     */
    fun getDatabaseManager(): DatabaseManager
}
