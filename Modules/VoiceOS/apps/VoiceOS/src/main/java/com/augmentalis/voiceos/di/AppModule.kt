/**
 * AppModule.kt - Koin dependency injection module for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.di

import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceos.data.VoiceOSSettingsDataStore
import com.augmentalis.voiceos.viewmodel.HomeViewModel
import com.augmentalis.voiceos.viewmodel.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin module for VoiceOS dependency injection.
 *
 * Provides:
 * - VoiceOSDatabaseManager (singleton)
 * - VoiceOSSettingsDataStore (singleton)
 * - HomeViewModel (viewModel)
 * - SettingsViewModel (viewModel)
 */
val appModule = module {
    // Database Manager (singleton)
    single {
        VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(androidContext()))
    }

    // Settings DataStore (singleton)
    single {
        VoiceOSSettingsDataStore(androidContext())
    }

    // HomeViewModel (scoped to activity/fragment lifecycle)
    viewModel {
        HomeViewModel(
            databaseManager = get(),
            packageManager = androidContext().packageManager
        )
    }

    // SettingsViewModel (scoped to activity/fragment lifecycle)
    viewModel {
        SettingsViewModel(settingsDataStore = get())
    }
}
