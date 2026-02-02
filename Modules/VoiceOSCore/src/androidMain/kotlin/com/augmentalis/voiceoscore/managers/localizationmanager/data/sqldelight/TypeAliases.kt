/**
 * TypeAliases.kt - Type aliases for backward compatibility
 *
 * Allows existing code to use the new SQLDelight adapter with minimal changes.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-01
 * Migration: Room -> SQLDelight for KMP compatibility
 */
package com.augmentalis.voiceoscore.managers.localizationmanager.data.sqldelight

/**
 * Type alias for backward compatibility with Room-style DAO usage
 */
typealias PreferencesDao = PreferencesDaoAdapter
