/**
 * AndroidHandlerFactory.kt - Android-specific handler factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Factory for creating handlers with Android-specific executors.
 */
package com.augmentalis.voiceoscoreng

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.handlers.*

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

        // Create handlers with executors
        return listOf(
            SystemHandler(systemExecutor),
            NavigationHandler(navigationExecutor),
            UIHandler(uiExecutor),
            InputHandler(inputExecutor)
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
 */
fun VoiceOSCoreNG.Companion.createForAndroid(
    service: AccessibilityService,
    configuration: com.augmentalis.voiceoscoreng.managers.ServiceConfiguration =
        com.augmentalis.voiceoscoreng.managers.ServiceConfiguration.DEFAULT
): VoiceOSCoreNG {
    return VoiceOSCoreNG.Builder()
        .withHandlerFactory(AndroidHandlerFactory.create(service))
        .withSpeechEngineFactory(
            com.augmentalis.voiceoscoreng.speech.SpeechEngineFactoryProvider.create(service)
        )
        .withConfiguration(configuration)
        .build()
}
