package com.augmentalis.voiceoscoreng

import android.app.Application
import com.augmentalis.voiceoscoreng.app.BuildConfig
import com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG as VoiceOSCoreNGConfig
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.voiceoscoreng.persistence.AndroidCommandPersistence
import com.augmentalis.voiceoscoreng.persistence.ICommandPersistence

/**
 * Application class for VoiceOSCoreNG Test App.
 *
 * Initializes the VoiceOSCoreNG library with appropriate settings
 * and provides database access for command persistence.
 */
class VoiceOSCoreNGApplication : Application() {

    /**
     * Database manager singleton - provides access to all repositories.
     */
    lateinit var databaseManager: VoiceOSDatabaseManager
        private set

    /**
     * Command persistence layer - bridges VoiceOSCoreNG to SQLDelight.
     */
    lateinit var commandPersistence: ICommandPersistence
        private set

    /**
     * Generated command repository - direct database access.
     */
    val generatedCommandRepository: IGeneratedCommandRepository
        get() = databaseManager.generatedCommands

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        val driverFactory = DatabaseDriverFactory(this)
        databaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)

        // Create command persistence bridge
        commandPersistence = AndroidCommandPersistence(databaseManager.generatedCommands)

        // Initialize VoiceOSCoreNG configuration
        VoiceOSCoreNGConfig.initialize(
            tier = LearnAppDevToggle.Tier.LITE,
            isDebug = BuildConfig.DEBUG,
            enableTestMode = BuildConfig.ENABLE_TEST_MODE
        )

        android.util.Log.d("VoiceOSCoreNGApp", "Database initialized: voiceos.db")
    }

    companion object {
        /**
         * Get the Application instance from any context.
         */
        fun getInstance(context: android.content.Context): VoiceOSCoreNGApplication {
            return context.applicationContext as VoiceOSCoreNGApplication
        }
    }
}
