package com.augmentalis.voiceoscoreng

import android.app.Application
import com.augmentalis.voiceoscoreng.app.BuildConfig
import com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle

/**
 * Application class for VoiceOSCoreNG Test App.
 *
 * Initializes the VoiceOSCoreNG library with appropriate settings.
 */
class VoiceOSCoreNGApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize VoiceOSCoreNG
        VoiceOSCoreNG.initialize(
            tier = LearnAppDevToggle.Tier.LITE,
            isDebug = BuildConfig.DEBUG,
            enableTestMode = BuildConfig.ENABLE_TEST_MODE
        )
    }
}
