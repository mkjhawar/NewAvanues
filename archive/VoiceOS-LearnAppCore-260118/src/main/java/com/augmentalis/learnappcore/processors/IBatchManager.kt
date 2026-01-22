/**
 * IBatchManager.kt - Interface for batch command management
 *
 * Handles batch queue and flush operations for exploration mode.
 * Extracted from LearnAppCore as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 1.2.0 (SOLID Refactoring)
 */

package com.augmentalis.learnappcore.processors

import com.augmentalis.database.dto.GeneratedCommandDTO

/**
 * Batch Manager Interface
 *
 * Responsibilities:
 * - Queue commands for batch processing
 * - Flush batch to database
 * - Monitor batch size
 * - Clear batch queue
 *
 * Single Responsibility: Batch operation management
 */
interface IBatchManager {
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
     *
     * @return Number of commands flushed
     */
    suspend fun flushBatch(): Int

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
     *
     * @return Number of commands cleared
     */
    fun clearBatch(): Int

    /**
     * Check if batch is empty.
     *
     * @return True if queue is empty
     */
    fun isEmpty(): Boolean
}
