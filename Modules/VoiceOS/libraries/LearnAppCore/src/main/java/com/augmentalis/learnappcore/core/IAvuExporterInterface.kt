/**
 * IAvuExporterInterface.kt - Focused interface for AVU export
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

import kotlinx.coroutines.flow.Flow

/**
 * AVU Export Operations Interface
 *
 * Focused interface for exporting commands to AVU format.
 *
 * Single Responsibility: AVU format export
 *
 * This interface follows Interface Segregation Principle (ISP) by exposing
 * only export operations, avoiding the fat interface problem.
 */
interface IAvuExporterInterface {
    /**
     * Export commands as AVU lines.
     *
     * Streams lines for memory efficiency with large datasets.
     *
     * @param packageName Package to export commands for
     * @return Flow of AVU line batches
     */
    suspend fun exportAvuLines(packageName: String): Flow<List<String>>
}
