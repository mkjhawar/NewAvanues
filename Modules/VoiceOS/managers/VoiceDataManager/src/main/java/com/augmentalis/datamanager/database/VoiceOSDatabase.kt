package com.augmentalis.datamanager.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.augmentalis.datamanager.converters.StringListConverter
import com.augmentalis.datamanager.dao.AnalyticsSettingsDao
import com.augmentalis.datamanager.dao.CommandHistoryEntryDao
import com.augmentalis.datamanager.dao.CustomCommandDao
import com.augmentalis.datamanager.dao.DeviceProfileDao
import com.augmentalis.datamanager.dao.ErrorReportDao
import com.augmentalis.datamanager.dao.GestureLearningDataDao
import com.augmentalis.datamanager.dao.LanguageModelDao
import com.augmentalis.datamanager.dao.RecognitionLearningDao
import com.augmentalis.datamanager.dao.RetentionSettingsDao
import com.augmentalis.datamanager.dao.ScrappedCommandDao
import com.augmentalis.datamanager.dao.TouchGestureDao
import com.augmentalis.datamanager.dao.UsageStatisticDao
import com.augmentalis.datamanager.dao.UserPreferenceDao
import com.augmentalis.datamanager.dao.UserSequenceDao
import com.augmentalis.datamanager.entities.AnalyticsSettings
import com.augmentalis.datamanager.entities.CommandHistoryEntry
import com.augmentalis.datamanager.entities.CustomCommand
import com.augmentalis.datamanager.entities.DeviceProfile
import com.augmentalis.datamanager.entities.ErrorReport
import com.augmentalis.datamanager.entities.GestureLearningData
import com.augmentalis.datamanager.entities.LanguageModel
import com.augmentalis.datamanager.entities.RecognitionLearning
import com.augmentalis.datamanager.entities.RetentionSettings
import com.augmentalis.datamanager.entities.ScrappedCommand
import com.augmentalis.datamanager.entities.TouchGesture
import com.augmentalis.datamanager.entities.UsageStatistic
import com.augmentalis.datamanager.entities.UserPreference
import com.augmentalis.datamanager.entities.UserSequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * VoiceOS Room Database
 * Central database for all VoiceOS data persistence
 *
 * Features:
 * - 13 entities for comprehensive data management
 * - Type converters for complex data types
 * - Migration support for future updates
 * - Thread-safe singleton implementation
 * - Coroutine support for async operations
 */
@Database(
    entities = [
        AnalyticsSettings::class,
        CommandHistoryEntry::class,
        CustomCommand::class,
        DeviceProfile::class,
        ErrorReport::class,
        GestureLearningData::class,
        LanguageModel::class,
        RecognitionLearning::class,
        RetentionSettings::class,
        TouchGesture::class,
        UsageStatistic::class,
        UserPreference::class,
        UserSequence::class,
        ScrappedCommand::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class VoiceOSDatabase : RoomDatabase() {

    // DAO accessors
    abstract fun analyticsSettingsDao(): AnalyticsSettingsDao
    abstract fun commandHistoryEntryDao(): CommandHistoryEntryDao
    abstract fun customCommandDao(): CustomCommandDao
    abstract fun scrappedCommandDao(): ScrappedCommandDao
    abstract fun deviceProfileDao(): DeviceProfileDao
    abstract fun errorReportDao(): ErrorReportDao
    abstract fun gestureLearningDataDao(): GestureLearningDataDao
    abstract fun languageModelDao(): LanguageModelDao
    abstract fun recognitionLearningDao(): RecognitionLearningDao
    abstract fun retentionSettingsDao(): RetentionSettingsDao
    abstract fun touchGestureDao(): TouchGestureDao
    abstract fun usageStatisticDao(): UsageStatisticDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun userSequenceDao(): UserSequenceDao

    companion object {
        private const val DATABASE_NAME = "voiceos_database.db"

        @Volatile
        private var INSTANCE: VoiceOSDatabase? = null

        /**
         * Get database instance (singleton)
         */
        fun getInstance(context: Context): VoiceOSDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Build the database with migrations and callbacks
         */
        private fun buildDatabase(context: Context): VoiceOSDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                VoiceOSDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(DatabaseCallback())
                // MIGRATION REQUIRED: Before incrementing version, implement Migration objects
                // Currently at version 1 - safe without migrations until schema changes
                .build()
        }

        /**
         * Clear database instance (for testing)
         */
        internal fun clearInstance() {
            INSTANCE = null
        }

        /**
         * Database callback for initialization
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Initialize default settings on first creation
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        initializeDefaultData(database)
                    }
                }
            }
        }

        /**
         * Initialize default data for single-record tables
         */
        private suspend fun initializeDefaultData(database: VoiceOSDatabase) {
            // Initialize AnalyticsSettings with default values
            database.analyticsSettingsDao().insert(
                AnalyticsSettings(
                    id = 1,
                    trackPerformance = false,
                    autoEnableOnErrors = false,
                    errorThreshold = 10f,
                    sendAnonymousReports = false,
                    includeDeviceId = false,
                    userConsent = false,
                    consentDate = 0L,
                    detailedLogDays = 7,
                    aggregateOlderData = true
                )
            )

            // Initialize RetentionSettings with default values
            database.retentionSettingsDao().insert(
                RetentionSettings(
                    id = 1,
                    commandHistoryRetainCount = 50,
                    commandHistoryMaxDays = 30,
                    statisticsRetentionDays = 90,
                    enableAutoCleanup = true,
                    notifyBeforeCleanup = true,
                    maxDatabaseSizeMB = 100
                )
            )
        }

        /**
         * Migration examples for future use
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Example migration - add new column
                // database.execSQL("ALTER TABLE usage_statistics ADD COLUMN new_field TEXT DEFAULT ''")
            }
        }
    }
}