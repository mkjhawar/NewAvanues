/**
 * ProcessingMode.kt - Element processing modes for LearnApp
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Defines processing modes for element insertion.
 */
package com.augmentalis.voiceoscore.learnapp.core

/**
 * Processing Mode
 *
 * Defines how elements are processed and stored:
 * - IMMEDIATE: Insert to database immediately (JIT mode)
 * - BATCH: Queue for batch insertion (Exploration mode)
 */
enum class ProcessingMode {
    /**
     * Immediate mode - insert each element immediately.
     * Used in JIT (Just-In-Time) learning mode.
     * ~10ms per element.
     */
    IMMEDIATE,

    /**
     * Batch mode - queue elements for batch insertion.
     * Used in Exploration mode for efficiency.
     * ~50ms for 100 elements (20x faster than immediate).
     */
    BATCH
}
