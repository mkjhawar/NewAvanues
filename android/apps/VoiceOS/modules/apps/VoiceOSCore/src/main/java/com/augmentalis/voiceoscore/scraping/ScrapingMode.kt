/**
 * ScrapingMode.kt - Defines scraping behavior modes
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-10
 */
package com.augmentalis.voiceoscore.scraping

/**
 * Scraping mode determines how elements are processed and stored
 *
 * DYNAMIC Mode:
 * - Triggered automatically by window state changes
 * - Scrapes only currently visible UI elements
 * - Fast, lightweight, on-demand processing
 * - Provides partial UI coverage based on user navigation
 * - Updates element timestamps on revisit
 *
 * LEARN_APP Mode:
 * - User-triggered comprehensive scan
 * - Attempts to traverse entire app UI tree
 * - Discovers all screens and elements systematically
 * - Provides complete UI coverage
 * - Merges with existing dynamic data using hash-based deduplication
 * - Marks app as fully learned after successful completion
 */
enum class ScrapingMode {
    /**
     * Real-time scraping mode (default)
     *
     * Characteristics:
     * - Triggered by AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
     * - Scrapes currently visible UI only
     * - Updates element timestamps on re-scrape
     * - Incremental coverage as user explores app
     * - Minimal performance impact
     *
     * Use case: Normal operation, passive learning
     */
    DYNAMIC,

    /**
     * Full app learning mode (user-triggered)
     *
     * Characteristics:
     * - User-initiated comprehensive UI traversal
     * - Attempts to visit all screens and discover all elements
     * - Fills gaps in dynamic coverage
     * - Merges with existing elements (hash-based UPSERT)
     * - Marks app as fully learned on completion
     * - Higher performance impact during scan
     *
     * Use case: Complete app mapping, filling coverage gaps
     */
    LEARN_APP
}
