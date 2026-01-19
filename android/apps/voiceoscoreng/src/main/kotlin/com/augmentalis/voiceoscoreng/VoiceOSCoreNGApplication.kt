package com.augmentalis.voiceoscoreng

import android.app.Application
import com.augmentalis.voiceoscoreng.app.BuildConfig
import com.augmentalis.voiceoscore.LearnAppDevToggle
import com.augmentalis.voiceoscore.LearnAppConfig
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import com.augmentalis.voiceoscoreng.AndroidCommandPersistence
import com.augmentalis.voiceoscore.ICommandPersistence

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

    /**
     * Scraped app repository - for FK integrity (must insert before elements/commands).
     */
    val scrapedAppRepository: IScrapedAppRepository
        get() = databaseManager.scrapedApps

    /**
     * Scraped element repository - for FK integrity (must insert before commands).
     */
    val scrapedElementRepository: IScrapedElementRepository
        get() = databaseManager.scrapedElements

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        val driverFactory = DatabaseDriverFactory(this)
        databaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)

        // Create command persistence bridge
        commandPersistence = AndroidCommandPersistence(databaseManager.generatedCommands)

        // Initialize LearnApp feature toggles
        LearnAppDevToggle.initialize(
            tier = LearnAppDevToggle.Tier.LITE,
            isDebug = BuildConfig.DEBUG
        )

        // Enable test mode if configured
        if (BuildConfig.ENABLE_TEST_MODE) {
            LearnAppConfig.enableTestMode()
        }

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
