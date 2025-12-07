/**
 * UUIDCreatorDatabase.kt - UUID database access (Stub)
 * Path: modules/libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/UUIDCreatorDatabase.kt
 *
 * Author: VoiceOS Restoration Team
 * Created: 2025-11-27
 *
 * STUB: This is a temporary stub to enable compilation.
 * UUIDs are now managed by VoiceOSDatabaseManager.uuids repository.
 */

package com.augmentalis.uuidcreator.database

import android.content.Context

/**
 * UUID Creator Database (STUB)
 *
 * Legacy database for UUID storage.
 * Now replaced by VoiceOSDatabaseManager.uuids repository.
 *
 * **STUB STATUS:**
 * - ✅ Provides required API surface for getInstance()
 * - ❌ No actual database operations
 * - ❌ Use VoiceOSDatabaseManager.uuids instead
 *
 * **Migration Path:**
 * Replace:
 * ```kotlin
 * val uuidDb = UUIDCreatorDatabase.getInstance(context)
 * ```
 *
 * With:
 * ```kotlin
 * val databaseManager = VoiceOSDatabaseManager(DatabaseDriverFactory(context))
 * val uuidRepo = databaseManager.uuids
 * ```
 *
 * @property context Application context
 */
class UUIDCreatorDatabase private constructor(
    private val context: Context
) {

    companion object {
        @Volatile
        private var INSTANCE: UUIDCreatorDatabase? = null

        /**
         * Get singleton instance
         *
         * @param context Application context
         * @return Database instance
         */
        fun getInstance(context: Context): UUIDCreatorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UUIDCreatorDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // STUB: No methods implemented
    // This class exists only for compilation compatibility
    // Use VoiceOSDatabaseManager.uuids repository instead
}
