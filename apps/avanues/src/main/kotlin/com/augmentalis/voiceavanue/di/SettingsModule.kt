/**
 * SettingsModule.kt - Hilt multibinding for settings providers
 *
 * Binds all ComposableSettingsProvider implementations into a Set<> via
 * Hilt @IntoSet. The UnifiedSettingsViewModel receives this set and
 * renders all modules dynamically.
 *
 * To add a new module's settings: create a provider class and add a
 * @Provides @IntoSet method here. The unified screen picks it up automatically.
 *
 * Note: @JvmSuppressWildcards is MANDATORY on the Set injection - without it
 * Hilt generates wildcard types that fail to match @IntoSet bindings.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.di

import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.PermissionsSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.SystemSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.VoiceControlSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.VoiceCursorSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.WebAvanueSettingsProvider
import com.augmentalis.webavanue.BrowserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @IntoSet
    fun providePermissionsSettings(): ComposableSettingsProvider =
        PermissionsSettingsProvider()

    @Provides
    @IntoSet
    fun provideVoiceCursorSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        VoiceCursorSettingsProvider(repository)

    @Provides
    @IntoSet
    fun provideVoiceControlSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        VoiceControlSettingsProvider(repository)

    @Provides
    @IntoSet
    fun provideWebAvanueSettings(
        repository: BrowserRepository
    ): ComposableSettingsProvider =
        WebAvanueSettingsProvider(repository)

    @Provides
    @IntoSet
    fun provideSystemSettings(
        repository: AvanuesSettingsRepository,
        credentialStore: com.augmentalis.voiceavanue.data.SftpCredentialStore
    ): ComposableSettingsProvider =
        SystemSettingsProvider(repository, credentialStore)
}
