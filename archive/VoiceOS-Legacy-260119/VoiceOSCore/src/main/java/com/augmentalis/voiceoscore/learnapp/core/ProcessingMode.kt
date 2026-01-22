/**
 * ProcessingMode.kt - Element processing modes for LearnAppCore
 *
 * Determines how LearnAppCore processes and stores elements.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-04
 * Related: JIT-LearnApp Merge (ADR-002)
 *
 * @since 1.1.0 (JIT-LearnApp Merge)
 */

package com.augmentalis.voiceoscore.learnapp.core

/**
 * Processing Mode
 *
 * Determines how LearnAppCore processes elements and stores voice commands.
 *
 * ## Mode Comparison:
 *
 * | Mode | Elements | Storage | Performance | Memory | Use Case |
 * |------|----------|---------|-------------|--------|----------|
 * | IMMEDIATE | 1 | Insert now | ~10ms/element | ~1KB | JIT Mode |
 * | BATCH | 100+ | Queue + flush | ~50ms/100 elements | ~150KB | Exploration |
 *
 * ## Usage - JIT Mode:
 * ```kotlin
 * core.processElement(element, packageName, ProcessingMode.IMMEDIATE)
 * // Command inserted to database immediately
 * ```
 *
 * ## Usage - Exploration Mode:
 * ```kotlin
 * elements.forEach {
 *     core.processElement(it, packageName, ProcessingMode.BATCH)
 * }
 * core.flushBatch()  // Single transaction insert
 * ```
 *
 * @since 1.1.0 (JIT-LearnApp Merge)
 */
enum class ProcessingMode {
    /**
     * Immediate Mode (JIT)
     *
     * Process 1 element at a time.
     * Insert to database immediately.
     * Used by: JustInTimeLearner
     *
     * ## Performance:
     * - ~10ms per element
     * - Individual database transaction per element
     * - Low memory usage (~1KB per element)
     *
     * ## Use When:
     * - User is actively clicking
     * - Processing 1-10 elements
     * - Immediate feedback required
     */
    IMMEDIATE,

    /**
     * Batch Mode (Exploration)
     *
     * Process 100+ elements at once.
     * Queue commands, insert as batch.
     * Used by: ExplorationEngine
     *
     * ## Performance:
     * - ~50ms for 100 elements (20x faster than IMMEDIATE)
     * - Single database transaction for all elements
     * - Higher memory usage (~150KB peak for queue)
     *
     * ## Use When:
     * - Full app exploration
     * - Processing 50+ elements
     * - Batch efficiency desired
     *
     * ## Important:
     * MUST call `flushBatch()` after processing to persist commands!
     */
    BATCH
}
