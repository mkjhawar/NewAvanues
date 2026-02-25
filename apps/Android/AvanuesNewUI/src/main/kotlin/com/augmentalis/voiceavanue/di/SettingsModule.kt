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
import com.augmentalis.voiceavanue.ui.settings.providers.CockpitSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.PDFAvanueSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.PhotoAvanueSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.VideoAvanueSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.NoteAvanueSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.FileAvanueSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.RemoteCastSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.AnnotationAvanueSettingsProvider
import com.augmentalis.voiceavanue.ui.settings.providers.ImageAvanueSettingsProvider
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

    @Provides
    @IntoSet
    fun provideCockpitSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        CockpitSettingsProvider(repository)

    @Provides
    @IntoSet
    fun providePDFAvanueSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        PDFAvanueSettingsProvider(repository)

    @Provides
    @IntoSet
    fun providePhotoAvanueSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        PhotoAvanueSettingsProvider(repository)

    @Provides
    @IntoSet
    fun provideVideoAvanueSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        VideoAvanueSettingsProvider(repository)

    @Provides
    @IntoSet
    fun provideNoteAvanueSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        NoteAvanueSettingsProvider(repository)

    @Provides
    @IntoSet
    fun provideFileAvanueSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        FileAvanueSettingsProvider(repository)

    @Provides
    @IntoSet
    fun provideRemoteCastSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        RemoteCastSettingsProvider(repository)

    @Provides
    @IntoSet
    fun provideAnnotationAvanueSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        AnnotationAvanueSettingsProvider(repository)

    @Provides
    @IntoSet
    fun provideImageAvanueSettings(
        repository: AvanuesSettingsRepository
    ): ComposableSettingsProvider =
        ImageAvanueSettingsProvider(repository)
}
