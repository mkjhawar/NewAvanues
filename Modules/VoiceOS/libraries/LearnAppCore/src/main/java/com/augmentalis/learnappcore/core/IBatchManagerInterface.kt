/**
 * IBatchManagerInterface.kt - Focused interface for batch management
 *
 * Part of ISP refactoring to split fat ILearnAppCore interface.
 * Extracted from LearnAppCore as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 1.2.0 (SOLID Refactoring - ISP Compliance)
 */

package com.augmentalis.learnappcore.core

import com.augmentalis.database.dto.GeneratedCommandDTO

/**
 * Batch Management Operations Interface
 *
 * Focused interface for batch queue operations.
 *
 * Single Responsibility: Batch command management
 *
 * This interface follows Interface Segregation Principle (ISP) by exposing
 * only batch operations, avoiding the fat interface problem.
 */
interface IBatchManagerInterface {
    /**
     * Add command to batch queue.
     *
     * @param command Command to queue
     */
    fun addCommand(command: GeneratedCommandDTO)

    /**
     * Flush batch queue to database.
     *
     * Inserts all queued commands in single transaction.
     */
    suspend fun flushBatch()

    /**
     * Get current batch size.
     *
     * @return Number of queued commands
     */
    fun getBatchSize(): Int

    /**
     * Clear batch queue without flushing.
     *
     * Used for cleanup or error recovery.
     */
    fun clearBatchQueue()
}
