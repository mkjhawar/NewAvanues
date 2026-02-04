/**
 * AppVersionDTO.kt - Data transfer object for app version records
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Represents app version data from database.
 * Used for version-aware command lifecycle management.
 */

package com.augmentalis.database.dto

/**
 * DTO for app version database records.
 *
 * Stores the last known version for an installed app that VoiceOS
 * has generated commands for.
 *
 * Note: This DTO is for database layer only. Consumers should create
 * domain model objects (e.g., AppVersion) from the versionName and versionCode fields.
 *
 * @property packageName App package identifier (e.g., "com.google.android.gm")
 * @property versionName Human-readable version string (e.g., "8.2024.11.123")
 * @property versionCode Integer version for comparison (e.g., 82024)
 * @property lastChecked Timestamp when version was last checked (epoch millis)
 */
data class AppVersionDTO(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val lastChecked: Long
) {
    companion object {
        /**
         * Create from SQLDelight generated class.
         *
         * @param entity Generated AppVersion entity from SQLDelight
         * @return AppVersionDTO
         */
        fun from(entity: com.augmentalis.database.App_version): AppVersionDTO {
            return AppVersionDTO(
                packageName = entity.package_name,
                versionName = entity.version_name,
                versionCode = entity.version_code,
                lastChecked = entity.last_checked
            )
        }
    }
}
