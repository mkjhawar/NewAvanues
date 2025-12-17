/**
 * AppScrapingDatabase.kt - Room database for accessibility scraping
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.scraping.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.augmentalis.voiceoscore.scraping.dao.ElementRelationshipDao
import com.augmentalis.voiceoscore.scraping.dao.ElementStateHistoryDao
import com.augmentalis.voiceoscore.scraping.dao.GeneratedCommandDao
import com.augmentalis.voiceoscore.scraping.dao.ScrapedAppDao
import com.augmentalis.voiceoscore.scraping.dao.ScrapedElementDao
import com.augmentalis.voiceoscore.scraping.dao.ScrapedHierarchyDao
import com.augmentalis.voiceoscore.scraping.dao.ScreenContextDao
import com.augmentalis.voiceoscore.scraping.dao.ScreenTransitionDao
import com.augmentalis.voiceoscore.scraping.dao.UserInteractionDao
import com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity
import com.augmentalis.voiceoscore.scraping.entities.ElementStateHistoryEntity
import com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedAppEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedHierarchyEntity
import com.augmentalis.voiceoscore.scraping.entities.ScreenContextEntity
import com.augmentalis.voiceoscore.scraping.entities.ScreenTransitionEntity
import com.augmentalis.voiceoscore.scraping.entities.UserInteractionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * App Scraping Database
 *
 * This database stores:
 * 1. ScrapedAppEntity - App metadata and fingerprints
 * 2. ScrapedElementEntity - UI elements from accessibility tree
 * 3. ScrapedHierarchyEntity - Parent-child relationships
 * 4. GeneratedCommandEntity - Voice commands mapped to elements
 *
 * Features:
 * - Hash-based element lookup (O(1) performance)
 * - Foreign key cascading deletes
 * - Automatic cleanup of old data
 * - Migration support for schema updates
 */
@Database(
    entities = [
        ScrapedAppEntity::class,
        ScrapedElementEntity::class,
        ScrapedHierarchyEntity::class,
        GeneratedCommandEntity::class,
        ScreenContextEntity::class,
        ElementRelationshipEntity::class,
        ScreenTransitionEntity::class,
        UserInteractionEntity::class,
        ElementStateHistoryEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppScrapingDatabase : RoomDatabase() {

    // DAOs
    abstract fun scrapedAppDao(): ScrapedAppDao
    abstract fun scrapedElementDao(): ScrapedElementDao
    abstract fun scrapedHierarchyDao(): ScrapedHierarchyDao
    abstract fun generatedCommandDao(): GeneratedCommandDao
    abstract fun screenContextDao(): ScreenContextDao
    abstract fun elementRelationshipDao(): ElementRelationshipDao
    abstract fun screenTransitionDao(): ScreenTransitionDao
    abstract fun userInteractionDao(): UserInteractionDao
    abstract fun elementStateHistoryDao(): ElementStateHistoryDao

    companion object {
        private const val DATABASE_NAME = "app_scraping_database"
        private const val RETENTION_DAYS = 7L

        @Volatile
        private var INSTANCE: AppScrapingDatabase? = null

        // Coroutine scope for background cleanup tasks
        private val databaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Get database instance (singleton pattern)
         */
        fun getInstance(context: Context): AppScrapingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppScrapingDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Database callback for initialization and cleanup
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Database created - no initial data needed
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Trigger automatic cleanup on database open
                INSTANCE?.let { database ->
                    databaseScope.launch {
                        cleanupOldData(database)
                    }
                }
            }
        }

        /**
         * Clean up data older than retention period
         * Called automatically on database open
         *
         * Cleanup strategy:
         * 1. Delete apps not scraped in 7 days (cascades to elements/commands)
         * 2. Delete low-quality commands (unused, low confidence)
         */
        private suspend fun cleanupOldData(database: AppScrapingDatabase) {
            try {
                val retentionTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS)

                // Delete old apps (cascades to elements, hierarchy, and commands via foreign keys)
                val deletedApps = database.scrapedAppDao().deleteAppsOlderThan(retentionTimestamp)

                // Delete low-quality commands (unused and low confidence < 0.3)
                val deletedCommands = database.generatedCommandDao().deleteLowQualityCommands(threshold = 0.3f)

                android.util.Log.d(
                    "AppScrapingDatabase",
                    "Cleanup complete: $deletedApps apps deleted, $deletedCommands low-quality commands removed"
                )
            } catch (e: Exception) {
                android.util.Log.e("AppScrapingDatabase", "Error during cleanup", e)
            }
        }

        /**
         * Manually trigger cleanup (can be called from settings/maintenance)
         */
        suspend fun performCleanup(context: Context) {
            val database = getInstance(context)
            cleanupOldData(database)
        }

        /**
         * Clear all data (for testing or user reset)
         */
        suspend fun clearAllData(context: Context) {
            val database = getInstance(context)
            database.clearAllTables()
            android.util.Log.d("AppScrapingDatabase", "All data cleared")
        }

        /**
         * Get database statistics
         */
        suspend fun getDatabaseStats(context: Context): DatabaseStats {
            val database = getInstance(context)
            return DatabaseStats(
                appCount = database.scrapedAppDao().getAppCount(),
                totalElements = 0, // Would need to query each app
                totalCommands = 0,  // Would need to aggregate
                totalRelationships = database.scrapedHierarchyDao().getRelationshipCount()
            )
        }
    }

    /**
     * Database statistics data class
     */
    data class DatabaseStats(
        val appCount: Int,
        val totalElements: Int,
        val totalCommands: Int,
        val totalRelationships: Int
    )
}

/**
 * Migration from version 1 to 2
 *
 * Changes:
 * 1. Add unique constraint to scraped_elements.element_hash
 * 2. Migrate generated_commands from element_id (Long FK) to element_hash (String FK)
 *
 * Strategy:
 * - Create new generated_commands table with element_hash FK
 * - Migrate existing data by joining with scraped_elements to get element_hash
 * - Drop old table and rename new table
 * - Recreate indexes
 *
 * Note: ScrapedHierarchyEntity remains unchanged (keeps Long ID FKs for performance)
 * See: /coding/DECISIONS/ScrapedHierarchy-Migration-Analysis-251010-0220.md
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 1 → 2")

        try {
            // ===== STEP 1: Add unique constraint to element_hash =====
            // Drop existing non-unique index
            db.execSQL("DROP INDEX IF EXISTS index_scraped_elements_element_hash")

            // Create unique index
            db.execSQL(
                "CREATE UNIQUE INDEX index_scraped_elements_element_hash " +
                "ON scraped_elements(element_hash)"
            )

            android.util.Log.d("AppScrapingDatabase", "✓ Added unique constraint to element_hash")

            // ===== STEP 2: Create new generated_commands table with element_hash FK =====
            db.execSQL("""
                CREATE TABLE generated_commands_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    element_hash TEXT NOT NULL,
                    command_text TEXT NOT NULL,
                    action_type TEXT NOT NULL,
                    confidence REAL NOT NULL,
                    synonyms TEXT NOT NULL,
                    is_user_approved INTEGER NOT NULL DEFAULT 0,
                    usage_count INTEGER NOT NULL DEFAULT 0,
                    last_used INTEGER,
                    generated_at INTEGER NOT NULL,
                    FOREIGN KEY(element_hash)
                        REFERENCES scraped_elements(element_hash)
                        ON DELETE CASCADE
                )
            """)

            android.util.Log.d("AppScrapingDatabase", "✓ Created new generated_commands table")

            // ===== STEP 3: Migrate existing data (join to get element_hash) =====
            // Count existing commands for verification
            val cursor = db.query("SELECT COUNT(*) FROM generated_commands")
            var oldCommandCount = 0
            if (cursor.moveToFirst()) {
                oldCommandCount = cursor.getInt(0)
            }
            cursor.close()

            android.util.Log.d("AppScrapingDatabase", "Migrating $oldCommandCount commands...")

            // Migrate data
            db.execSQL("""
                INSERT INTO generated_commands_new
                (id, element_hash, command_text, action_type, confidence, synonyms,
                 is_user_approved, usage_count, last_used, generated_at)
                SELECT
                    gc.id,
                    se.element_hash,
                    gc.command_text,
                    gc.action_type,
                    gc.confidence,
                    gc.synonyms,
                    gc.is_user_approved,
                    gc.usage_count,
                    gc.last_used,
                    gc.generated_at
                FROM generated_commands gc
                INNER JOIN scraped_elements se ON gc.element_id = se.id
            """)

            // Verify migration count
            val newCursor = db.query("SELECT COUNT(*) FROM generated_commands_new")
            var newCommandCount = 0
            if (newCursor.moveToFirst()) {
                newCommandCount = newCursor.getInt(0)
            }
            newCursor.close()

            android.util.Log.d("AppScrapingDatabase", "✓ Migrated $newCommandCount/$oldCommandCount commands")

            if (newCommandCount < oldCommandCount) {
                android.util.Log.w(
                    "AppScrapingDatabase",
                    "Warning: ${oldCommandCount - newCommandCount} commands lost (orphaned - no matching element)"
                )
            }

            // ===== STEP 4: Drop old table and rename new table =====
            db.execSQL("DROP TABLE generated_commands")
            db.execSQL("ALTER TABLE generated_commands_new RENAME TO generated_commands")

            android.util.Log.d("AppScrapingDatabase", "✓ Replaced old table with new table")

            // ===== STEP 5: Create indexes on new table =====
            db.execSQL(
                "CREATE INDEX index_generated_commands_element_hash " +
                "ON generated_commands(element_hash)"
            )
            db.execSQL(
                "CREATE INDEX index_generated_commands_command_text " +
                "ON generated_commands(command_text)"
            )
            db.execSQL(
                "CREATE INDEX index_generated_commands_action_type " +
                "ON generated_commands(action_type)"
            )

            android.util.Log.d("AppScrapingDatabase", "✓ Created indexes on new table")

            android.util.Log.i("AppScrapingDatabase", "✅ Migration 1 → 2 completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "❌ Migration 1 → 2 failed", e)
            throw e
        }
    }
}

/**
 * Migration from version 2 to 3
 *
 * Changes:
 * 1. Add is_fully_learned column to scraped_apps (default: false)
 * 2. Add learn_completed_at column to scraped_apps (nullable)
 * 3. Add scraping_mode column to scraped_apps (default: "DYNAMIC")
 *
 * Purpose: Support LearnApp mode tracking and merge functionality
 *
 * Strategy:
 * - Add new columns with appropriate defaults
 * - No data migration needed (all apps default to not fully learned)
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 2 → 3")

        try {
            // ===== STEP 1: Add is_fully_learned column (default: false) =====
            db.execSQL(
                "ALTER TABLE scraped_apps ADD COLUMN is_fully_learned INTEGER NOT NULL DEFAULT 0"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added is_fully_learned column")

            // ===== STEP 2: Add learn_completed_at column (nullable) =====
            db.execSQL(
                "ALTER TABLE scraped_apps ADD COLUMN learn_completed_at INTEGER"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added learn_completed_at column")

            // ===== STEP 3: Add scraping_mode column (default: "DYNAMIC") =====
            db.execSQL(
                "ALTER TABLE scraped_apps ADD COLUMN scraping_mode TEXT NOT NULL DEFAULT 'DYNAMIC'"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added scraping_mode column")

            android.util.Log.i("AppScrapingDatabase", "✅ Migration 2 → 3 completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "❌ Migration 2 → 3 failed", e)
            throw e
        }
    }
}

/**
 * Migration from version 3 to 4
 *
 * Changes:
 * 1. Add uuid column to scraped_elements (nullable, indexed)
 *
 * Purpose: Support UUIDCreator integration for universal element identification
 *
 * Strategy:
 * - Add new uuid column (nullable to allow existing elements)
 * - Create index on uuid for fast lookups
 * - UUIDs will be generated on next scrape for existing elements
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 3 → 4")

        try {
            // ===== STEP 1: Add uuid column (nullable) =====
            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN uuid TEXT"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added uuid column to scraped_elements")

            // ===== STEP 2: Create index on uuid =====
            db.execSQL(
                "CREATE INDEX index_scraped_elements_uuid ON scraped_elements(uuid)"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Created index on uuid column")

            android.util.Log.i("AppScrapingDatabase", "✅ Migration 3 → 4 completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "❌ Migration 3 → 4 failed", e)
            throw e
        }
    }
}

/**
 * Migration from version 4 to 5
 *
 * Changes:
 * 1. Add semantic_role column to scraped_elements (nullable)
 * 2. Add input_type column to scraped_elements (nullable)
 * 3. Add visual_weight column to scraped_elements (nullable)
 * 4. Add is_required column to scraped_elements (nullable)
 *
 * Purpose: Support AI context inference (Phase 1) for semantic understanding of UI elements
 *
 * Strategy:
 * - Add new columns (nullable to allow existing elements)
 * - No indices needed (these are filter/analysis fields, not lookup keys)
 * - Values will be inferred on next scrape for existing elements
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 4 → 5")

        try {
            // ===== STEP 1: Add semantic_role column (nullable) =====
            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN semantic_role TEXT"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added semantic_role column to scraped_elements")

            // ===== STEP 2: Add input_type column (nullable) =====
            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN input_type TEXT"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added input_type column to scraped_elements")

            // ===== STEP 3: Add visual_weight column (nullable) =====
            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN visual_weight TEXT"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added visual_weight column to scraped_elements")

            // ===== STEP 4: Add is_required column (nullable) =====
            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN is_required INTEGER"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added is_required column to scraped_elements")

            android.util.Log.i("AppScrapingDatabase", "✅ Migration 4 → 5 completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "❌ Migration 4 → 5 failed", e)
            throw e
        }
    }
}

/**
 * Migration from version 5 to 6
 *
 * Changes:
 * 1. Add Phase 2 fields to scraped_elements (formGroupId, placeholderText, validationPattern, backgroundColor)
 * 2. Create screen_contexts table for screen-level context tracking
 * 3. Create element_relationships table for element relationship modeling
 *
 * Purpose: Support AI context inference Phase 2 - screen context and form relationships
 *
 * Strategy:
 * - Add new columns to scraped_elements (nullable for backward compatibility)
 * - Create new tables for screen contexts and element relationships
 * - Create appropriate indices for performance
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 5 → 6")

        try {
            // ===== STEP 1: Add Phase 2 fields to scraped_elements =====
            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN form_group_id TEXT"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added form_group_id column to scraped_elements")

            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN placeholder_text TEXT"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added placeholder_text column to scraped_elements")

            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN validation_pattern TEXT"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added validation_pattern column to scraped_elements")

            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN background_color TEXT"
            )
            android.util.Log.d("AppScrapingDatabase", "✓ Added background_color column to scraped_elements")

            // ===== STEP 2: Create screen_contexts table =====
            db.execSQL("""
                CREATE TABLE screen_contexts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    screen_hash TEXT NOT NULL,
                    app_id TEXT NOT NULL,
                    package_name TEXT NOT NULL,
                    activity_name TEXT,
                    window_title TEXT,
                    screen_type TEXT,
                    form_context TEXT,
                    navigation_level INTEGER NOT NULL DEFAULT 0,
                    primary_action TEXT,
                    element_count INTEGER NOT NULL DEFAULT 0,
                    has_back_button INTEGER NOT NULL DEFAULT 0,
                    first_scraped INTEGER NOT NULL,
                    last_scraped INTEGER NOT NULL,
                    visit_count INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(app_id) REFERENCES scraped_apps(app_id) ON DELETE CASCADE
                )
            """)
            android.util.Log.d("AppScrapingDatabase", "✓ Created screen_contexts table")

            // ===== STEP 3: Create indices for screen_contexts =====
            db.execSQL("CREATE UNIQUE INDEX index_screen_contexts_screen_hash ON screen_contexts(screen_hash)")
            db.execSQL("CREATE INDEX index_screen_contexts_app_id ON screen_contexts(app_id)")
            db.execSQL("CREATE INDEX index_screen_contexts_package_name ON screen_contexts(package_name)")
            db.execSQL("CREATE INDEX index_screen_contexts_screen_type ON screen_contexts(screen_type)")
            android.util.Log.d("AppScrapingDatabase", "✓ Created indices for screen_contexts")

            // ===== STEP 4: Create element_relationships table =====
            db.execSQL("""
                CREATE TABLE element_relationships (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    source_element_hash TEXT NOT NULL,
                    target_element_hash TEXT,
                    relationship_type TEXT NOT NULL,
                    relationship_data TEXT,
                    confidence REAL NOT NULL DEFAULT 1.0,
                    inferred_by TEXT NOT NULL DEFAULT 'accessibility_tree',
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY(source_element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE,
                    FOREIGN KEY(target_element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE
                )
            """)
            android.util.Log.d("AppScrapingDatabase", "✓ Created element_relationships table")

            // ===== STEP 5: Create indices for element_relationships =====
            db.execSQL("CREATE INDEX index_element_relationships_source_element_hash ON element_relationships(source_element_hash)")
            db.execSQL("CREATE INDEX index_element_relationships_target_element_hash ON element_relationships(target_element_hash)")
            db.execSQL("CREATE INDEX index_element_relationships_relationship_type ON element_relationships(relationship_type)")
            db.execSQL("CREATE UNIQUE INDEX index_element_relationships_unique ON element_relationships(source_element_hash, target_element_hash, relationship_type)")
            android.util.Log.d("AppScrapingDatabase", "✓ Created indices for element_relationships")

            android.util.Log.i("AppScrapingDatabase", "✅ Migration 5 → 6 completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "❌ Migration 5 → 6 failed", e)
            throw e
        }
    }
}

/**
 * Migration from version 6 to 7
 *
 * Changes:
 * 1. Add screen_transitions table for navigation flow tracking
 *
 * Purpose: Support Phase 2.5 screen transition tracking for user journey analysis
 *
 * Strategy:
 * - Create new table for screen-to-screen transitions
 * - Track transition counts and timing
 * - Enable navigation flow analysis
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 6 → 7")

        try {
            // ===== STEP 1: Create screen_transitions table =====
            db.execSQL("""
                CREATE TABLE screen_transitions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    from_screen_hash TEXT NOT NULL,
                    to_screen_hash TEXT NOT NULL,
                    transition_count INTEGER NOT NULL DEFAULT 1,
                    first_transition INTEGER NOT NULL,
                    last_transition INTEGER NOT NULL,
                    avg_transition_time INTEGER,
                    FOREIGN KEY(from_screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE,
                    FOREIGN KEY(to_screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE
                )
            """)
            android.util.Log.d("AppScrapingDatabase", "✓ Created screen_transitions table")

            // ===== STEP 2: Create indices for screen_transitions =====
            db.execSQL("CREATE INDEX index_screen_transitions_from_screen_hash ON screen_transitions(from_screen_hash)")
            db.execSQL("CREATE INDEX index_screen_transitions_to_screen_hash ON screen_transitions(to_screen_hash)")
            db.execSQL("CREATE UNIQUE INDEX index_screen_transitions_unique ON screen_transitions(from_screen_hash, to_screen_hash)")
            android.util.Log.d("AppScrapingDatabase", "✓ Created indices for screen_transitions")

            android.util.Log.i("AppScrapingDatabase", "✅ Migration 6 → 7 completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "❌ Migration 6 → 7 failed", e)
            throw e
        }
    }
}

/**
 * Migration from version 7 to 8
 *
 * Changes:
 * 1. Add user_interactions table for user interaction tracking
 * 2. Add element_state_history table for element state change tracking
 *
 * Purpose: Support Phase 3 user interaction tracking and state-aware voice commands
 *
 * Strategy:
 * - Create new tables for interaction events and state changes
 * - Track visibility duration for interaction confidence scoring
 * - Enable state-aware command generation ("check" vs "uncheck")
 * - Support multi-step navigation through interaction history
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 7 → 8")

        try {
            // ===== STEP 1: Create user_interactions table =====
            db.execSQL("""
                CREATE TABLE user_interactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    element_hash TEXT NOT NULL,
                    screen_hash TEXT NOT NULL,
                    interaction_type TEXT NOT NULL,
                    interaction_time INTEGER NOT NULL,
                    visibility_start INTEGER,
                    visibility_duration INTEGER,
                    success INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE,
                    FOREIGN KEY(screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE
                )
            """)
            android.util.Log.d("AppScrapingDatabase", "✓ Created user_interactions table")

            // ===== STEP 2: Create indices for user_interactions =====
            db.execSQL("CREATE INDEX index_user_interactions_element_hash ON user_interactions(element_hash)")
            db.execSQL("CREATE INDEX index_user_interactions_screen_hash ON user_interactions(screen_hash)")
            db.execSQL("CREATE INDEX index_user_interactions_interaction_type ON user_interactions(interaction_type)")
            db.execSQL("CREATE INDEX index_user_interactions_interaction_time ON user_interactions(interaction_time)")
            android.util.Log.d("AppScrapingDatabase", "✓ Created indices for user_interactions")

            // ===== STEP 3: Create element_state_history table =====
            db.execSQL("""
                CREATE TABLE element_state_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    element_hash TEXT NOT NULL,
                    screen_hash TEXT NOT NULL,
                    state_type TEXT NOT NULL,
                    old_value TEXT,
                    new_value TEXT,
                    changed_at INTEGER NOT NULL,
                    triggered_by TEXT,
                    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE,
                    FOREIGN KEY(screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE
                )
            """)
            android.util.Log.d("AppScrapingDatabase", "✓ Created element_state_history table")

            // ===== STEP 4: Create indices for element_state_history =====
            db.execSQL("CREATE INDEX index_element_state_history_element_hash ON element_state_history(element_hash)")
            db.execSQL("CREATE INDEX index_element_state_history_screen_hash ON element_state_history(screen_hash)")
            db.execSQL("CREATE INDEX index_element_state_history_state_type ON element_state_history(state_type)")
            db.execSQL("CREATE INDEX index_element_state_history_changed_at ON element_state_history(changed_at)")
            android.util.Log.d("AppScrapingDatabase", "✓ Created indices for element_state_history")

            android.util.Log.i("AppScrapingDatabase", "✅ Migration 7 → 8 completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "❌ Migration 7 → 8 failed", e)
            throw e
        }
    }
}
