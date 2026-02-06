package com.avanues.avu.dsl

import kotlinx.datetime.Clock

/**
 * Cross-platform time utility for AVU DSL execution timing.
 */
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
