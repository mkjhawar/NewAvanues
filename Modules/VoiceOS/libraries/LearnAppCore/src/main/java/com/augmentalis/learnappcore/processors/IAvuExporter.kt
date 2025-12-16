/**
 * IAvuExporter.kt - Interface for AVU export operations
 *
 * Handles export of generated commands to AVU format.
 * Extracted from LearnAppCore as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 1.2.0 (SOLID Refactoring)
 */

package com.augmentalis.learnappcore.processors

import kotlinx.coroutines.flow.Flow

/**
 * AVU Exporter Interface
 *
 * Responsibilities:
 * - Export commands to AVU format
 * - Stream AVU lines
 * - Format command data
 *
 * Single Responsibility: AVU export logic
 */
interface IAvuExporter {
    /**
     * Export generated commands as AVU lines.
     *
     * Streams lines for memory efficiency with large datasets.
     *
     * @param packageName Package to export commands for
     * @return Flow of AVU line batches
     */
    suspend fun exportAvuLines(packageName: String): Flow<List<String>>

    /**
     * Format single command as AVU line.
     *
     * @param elementHash Element hash
     * @param commandText Command text
     * @param actionType Action type
     * @return Formatted AVU line
     */
    fun formatAvuLine(elementHash: String, commandText: String, actionType: String): String
}
