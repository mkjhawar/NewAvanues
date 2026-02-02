/**
 * CacheTier.kt - Cache tier enumeration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-10
 *
 * Defines the 3-tier caching architecture for command resolution
 * Based on Q3 Decision: Tiered Caching (Tier 1/2/3)
 */
package com.augmentalis.voiceoscore.managers.commandmanager.cache

/**
 * Cache tiers for command resolution
 *
 * Performance targets:
 * - TIER_1: <0.5ms (preloaded, ~10KB)
 * - TIER_2: <0.5ms (LRU cache, ~25KB)
 * - TIER_3: 5-15ms (database fallback)
 *
 * Total target: <100ms end-to-end command resolution
 */
enum class CacheTier {
    /**
     * Tier 1: Preloaded top 20 commands (~10KB)
     * Fastest access, always in memory
     * Populated at service startup
     */
    TIER_1,

    /**
     * Tier 2: LRU cache of recently used commands (max 50, ~25KB)
     * Fast access for frequently used commands
     * Automatically promotes from Tier 3
     */
    TIER_2,

    /**
     * Tier 3: Database fallback (slower but complete)
     * All commands available
     * Queries take 5-15ms but provide full coverage
     */
    TIER_3
}
