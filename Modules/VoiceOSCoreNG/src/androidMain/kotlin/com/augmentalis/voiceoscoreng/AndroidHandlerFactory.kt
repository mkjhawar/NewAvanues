/**
 * AndroidHandlerFactory.kt - Android-specific handler factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-09 - Added AppHandler, StaticCommandPersistence integration
 *
 * Factory for creating handlers with Android-specific executors.
 */
package com.augmentalis.voiceoscoreng

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.database.repositories.IVoiceCommandRepository
import com.augmentalis.voiceoscoreng.common.CommandRegistry
import com.augmentalis.voiceoscoreng.handlers.*
import com.augmentalis.voiceoscoreng.persistence.IStaticCommandPersistence
import com.augmentalis.voiceoscoreng.persistence.StaticCommandPersistence

/**
 * Android implementation of [HandlerFactory].
 *
 * Creates handlers with Android AccessibilityService-based executors.
 *
 * @param accessibilityServiceProvider Provider for the accessibility service instance
 * @param vuidLookup Optional function to lookup elements by VUID
 */
class AndroidHandlerFactory(
    private val accessibilityServiceProvider: () -> AccessibilityService?,
    private val vuidLookup: (String) -> AccessibilityNodeInfo? = { null }
) : HandlerFactory {

    override fun createHandlers(): List<IHandler> {
        // Create executors
        val navigationExecutor = AndroidNavigationExecutor(accessibilityServiceProvider)
        val uiExecutor = AndroidUIExecutor(accessibilityServiceProvider, vuidLookup)
        val inputExecutor = AndroidInputExecutor(accessibilityServiceProvider)
        val systemExecutor = AndroidSystemExecutor(accessibilityServiceProvider)
        val appLauncher = AndroidAppLauncher(accessibilityServiceProvider)

        // Create handlers with executors
        // AppHandler is included for app launching commands (open, launch, start)
        return listOf(
            SystemHandler(systemExecutor),
            NavigationHandler(navigationExecutor),
            UIHandler(uiExecutor),
            InputHandler(inputExecutor),
            AppHandler(appLauncher)
        )
    }

    companion object {
        /**
         * Create factory with accessibility service.
         */
        fun create(service: AccessibilityService): AndroidHandlerFactory {
            return AndroidHandlerFactory(
                accessibilityServiceProvider = { service }
            )
        }

        /**
         * Create factory with service provider.
         */
        fun create(serviceProvider: () -> AccessibilityService?): AndroidHandlerFactory {
            return AndroidHandlerFactory(
                accessibilityServiceProvider = serviceProvider
            )
        }
    }
}

/**
 * Extension function to create VoiceOSCoreNG with Android service.
 *
 * @param service The accessibility service instance
 * @param configuration Service configuration (optional)
 * @param commandRegistry Optional shared CommandRegistry for direct synchronous access.
 *        If provided, both caller and VoiceOSCoreNG share the same registry instance.
 * @param voiceCommandRepository Optional repository for static command persistence.
 *        If provided, static commands will be saved to database and registered with VoiceEngine.
 * @param locale Locale for static commands (default: "en-US")
 */
fun VoiceOSCoreNG.Companion.createForAndroid(
    service: AccessibilityService,
    configuration: ServiceConfiguration = ServiceConfiguration.DEFAULT,
    commandRegistry: CommandRegistry? = null,
    voiceCommandRepository: IVoiceCommandRepository? = null,
    locale: String = "en-US"
): VoiceOSCoreNG {
    val builder = VoiceOSCoreNG.Builder()
        .withHandlerFactory(AndroidHandlerFactory.create(service))
        .withSpeechEngineFactory(
            com.augmentalis.voiceoscoreng.features.SpeechEngineFactoryProvider.create(service)
        )
        .withConfiguration(configuration)

    // Add shared command registry if provided
    if (commandRegistry != null) {
        builder.withCommandRegistry(commandRegistry)
    }

    // Add static command persistence if repository provided
    if (voiceCommandRepository != null) {
        builder.withStaticCommandPersistence(
            StaticCommandPersistence(voiceCommandRepository, locale)
        )
    }

    return builder.build()
}

/**
 * Extension function to create VoiceOSCoreNG with full persistence support.
 *
 * This is the recommended method when you want static commands to be:
 * - Saved to database for offline availability
 * - Registered with VoiceEngine for speech recognition
 * - Available for user customization
 *
 * @param service The accessibility service instance
 * @param voiceCommandRepository Repository for static command persistence
 * @param configuration Service configuration (optional)
 * @param locale Locale for static commands (default: "en-US")
 */
fun VoiceOSCoreNG.Companion.createForAndroidWithPersistence(
    service: AccessibilityService,
    voiceCommandRepository: IVoiceCommandRepository,
    configuration: ServiceConfiguration = ServiceConfiguration.DEFAULT,
    locale: String = "en-US"
): VoiceOSCoreNG {
    return createForAndroid(
        service = service,
        configuration = configuration,
        voiceCommandRepository = voiceCommandRepository,
        locale = locale
    )
}
