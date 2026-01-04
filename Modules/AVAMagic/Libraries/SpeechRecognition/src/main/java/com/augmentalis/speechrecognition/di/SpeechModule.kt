/**
 * SpeechModule.kt - Hilt Dependency Injection Module for Speech Recognition
 * Path: modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/speechrecognition/di/SpeechModule.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-10-09
 *
 * Provides speech recognition dependencies for Hilt injection:
 * - SpeechConfig for speech recognition configuration
 * - VivokaEngine for Vivoka-based speech recognition
 * - Future: VoskEngine, GoogleEngine, WhisperEngine
 */

package com.augmentalis.speechrecognition.di

import android.content.Context
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechMode
// import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine  // DISABLED: Learning dependency
import com.augmentalis.voiceos.speech.engines.vivoka.model.VivokaLanguageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Speech Recognition Hilt module
 *
 * This module provides speech recognition related dependencies including
 * configuration and engine implementations. All engines are scoped as
 * Singleton to maintain state across the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object SpeechModule {

    /**
     * Provides default SpeechConfig for the application
     *
     * Configures speech recognition with:
     * - English (US) language
     * - Dynamic command mode (adapts to screen content)
     * - Voice Activity Detection (VAD) enabled
     * - 70% confidence threshold
     * - VOSK engine (offline capable)
     *
     * @return SpeechConfig with default settings
     */
    @Provides
    @Singleton
    fun provideSpeechConfig(): SpeechConfig {
        return SpeechConfig(
            language = VivokaLanguageRepository.LANGUAGE_CODE_ENGLISH_USA,
            mode = SpeechMode.DYNAMIC_COMMAND,
            enableVAD = true,
            confidenceThreshold = 0.7f,
            engine = SpeechEngine.VOSK,
            timeoutDuration = 5000,
            maxRecordingDuration = 30000
        )
    }

    /**
     * DEPRECATED: VivokaEngine is now provided by RefactoringModule
     *
     * This provider has been commented out to resolve Hilt duplicate binding conflict.
     * VivokaEngine is now provided by RefactoringModule as a dependency for ISpeechManager
     * as part of the SOLID refactoring (Phase 3).
     *
     * If you need VivokaEngine, inject ISpeechManager instead and access engines via
     * the speech manager interface.
     *
     * Date deprecated: 2025-10-17
     * Reason: Duplicate binding with RefactoringModule.provideVivokaEngine()
     * Migration path: Use ISpeechManager for speech engine access
     */
    // @Provides
    // @Singleton
    // fun provideVivokaEngine(
    //     @ApplicationContext context: Context
    // ): VivokaEngine {
    //     return VivokaEngine(context)
    // }

    // TODO: Add VoskEngine when ready
    // @Provides
    // @Singleton
    // fun provideVoskEngine(
    //     @ApplicationContext context: Context,
    //     config: SpeechConfig
    // ): VoskEngine {
    //     return VoskEngine(context, config)
    // }

    // TODO: Add GoogleCloudEngine when ready
    // @Provides
    // @Singleton
    // fun provideGoogleCloudEngine(
    //     @ApplicationContext context: Context,
    //     config: SpeechConfig
    // ): GoogleCloudEngine {
    //     return GoogleCloudEngine(context, config)
    // }

    // TODO: Add WhisperEngine when ready
    // @Provides
    // @Singleton
    // fun provideWhisperEngine(
    //     @ApplicationContext context: Context,
    //     config: SpeechConfig
    // ): WhisperEngine {
    //     return WhisperEngine(context, config)
    // }

    // TODO: Add EngineFactory when multiple engines are ready
    // @Provides
    // @Singleton
    // fun provideEngineFactory(
    //     vivokaEngine: VivokaEngine,
    //     voskEngine: VoskEngine,
    //     googleCloudEngine: GoogleCloudEngine,
    //     whisperEngine: WhisperEngine
    // ): SpeechEngineFactory {
    //     return SpeechEngineFactory(
    //         engines = mapOf(
    //             SpeechEngine.VIVOKA to vivokaEngine,
    //             SpeechEngine.VOSK to voskEngine,
    //             SpeechEngine.GOOGLE_CLOUD to googleCloudEngine,
    //             SpeechEngine.WHISPER to whisperEngine
    //         )
    //     )
    // }
}
