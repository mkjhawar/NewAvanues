/**
 * ScrapingMode.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Defines scraping modes for accessibility element capture
 */
package com.augmentalis.voiceoscore.scraping

/**
 * Modes for accessibility scraping
 */
enum class ScrapingMode {
    /**
     * Learn mode - actively learning app structure and commands
     */
    LEARN_APP,

    /**
     * Dynamic mode - using learned commands dynamically
     */
    DYNAMIC,

    /**
     * Manual mode - user manually defines commands
     */
    MANUAL
}
