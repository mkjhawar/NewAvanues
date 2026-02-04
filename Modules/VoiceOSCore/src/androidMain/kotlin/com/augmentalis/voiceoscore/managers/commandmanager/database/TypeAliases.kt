/**
 * TypeAliases.kt - Backward compatibility type aliases for Room-to-SQLDelight migration
 *
 * Purpose: Allow existing code to continue using the same import paths
 * while the underlying implementation has been migrated to SQLDelight.
 *
 * These type aliases will be deprecated in a future version.
 * New code should import directly from the sqldelight package.
 */

package com.augmentalis.voiceoscore.managers.commandmanager.database

import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.CommandSuccessRate
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.CommandUsageDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.CommandUsageEntity
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.CommandUsageStats
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.DatabaseVersionDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.DatabaseVersionEntity
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.LocaleStats
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandEntity

// ==================== Entity Type Aliases ====================

/**
 * Voice command entity - migrated from Room to SQLDelight.
 * @deprecated Use com.augmentalis.commandmanager.database.sqldelight.VoiceCommandEntity
 */
@Deprecated(
    message = "Migrated to SQLDelight. Use import from sqldelight package.",
    replaceWith = ReplaceWith("VoiceCommandEntity", "com.augmentalis.commandmanager.database.sqldelight.VoiceCommandEntity")
)
typealias VoiceCommandEntityLegacy = VoiceCommandEntity

/**
 * Command usage entity - migrated from Room to SQLDelight.
 * @deprecated Use com.augmentalis.commandmanager.database.sqldelight.CommandUsageEntity
 */
@Deprecated(
    message = "Migrated to SQLDelight. Use import from sqldelight package.",
    replaceWith = ReplaceWith("CommandUsageEntity", "com.augmentalis.commandmanager.database.sqldelight.CommandUsageEntity")
)
typealias CommandUsageEntityLegacy = CommandUsageEntity

/**
 * Database version entity - migrated from Room to SQLDelight.
 * @deprecated Use com.augmentalis.commandmanager.database.sqldelight.DatabaseVersionEntity
 */
@Deprecated(
    message = "Migrated to SQLDelight. Use import from sqldelight package.",
    replaceWith = ReplaceWith("DatabaseVersionEntity", "com.augmentalis.commandmanager.database.sqldelight.DatabaseVersionEntity")
)
typealias DatabaseVersionEntityLegacy = DatabaseVersionEntity

// ==================== DAO Type Aliases ====================

/**
 * Voice command DAO - migrated from Room to SQLDelight adapter.
 * @deprecated Use com.augmentalis.commandmanager.database.sqldelight.VoiceCommandDaoAdapter
 */
@Deprecated(
    message = "Migrated to SQLDelight. Use VoiceCommandDaoAdapter from sqldelight package.",
    replaceWith = ReplaceWith("VoiceCommandDaoAdapter", "com.augmentalis.commandmanager.database.sqldelight.VoiceCommandDaoAdapter")
)
typealias VoiceCommandDao = VoiceCommandDaoAdapter

/**
 * Command usage DAO - migrated from Room to SQLDelight adapter.
 * @deprecated Use com.augmentalis.commandmanager.database.sqldelight.CommandUsageDaoAdapter
 */
@Deprecated(
    message = "Migrated to SQLDelight. Use CommandUsageDaoAdapter from sqldelight package.",
    replaceWith = ReplaceWith("CommandUsageDaoAdapter", "com.augmentalis.commandmanager.database.sqldelight.CommandUsageDaoAdapter")
)
typealias CommandUsageDao = CommandUsageDaoAdapter

/**
 * Database version DAO - migrated from Room to SQLDelight adapter.
 * @deprecated Use com.augmentalis.commandmanager.database.sqldelight.DatabaseVersionDaoAdapter
 */
@Deprecated(
    message = "Migrated to SQLDelight. Use DatabaseVersionDaoAdapter from sqldelight package.",
    replaceWith = ReplaceWith("DatabaseVersionDaoAdapter", "com.augmentalis.commandmanager.database.sqldelight.DatabaseVersionDaoAdapter")
)
typealias DatabaseVersionDao = DatabaseVersionDaoAdapter

// ==================== Re-exports for direct use ====================
// These allow existing code to use the same class names without deprecation warnings

// Re-export entities from sqldelight package
// Note: Due to Kotlin type alias limitations, we use object re-exports for common patterns

/**
 * Locale statistics for database.
 */
typealias LocaleStatsCompat = LocaleStats

/**
 * Command usage statistics.
 */
typealias CommandUsageStatsCompat = CommandUsageStats

/**
 * Command success rate.
 */
typealias CommandSuccessRateCompat = CommandSuccessRate
