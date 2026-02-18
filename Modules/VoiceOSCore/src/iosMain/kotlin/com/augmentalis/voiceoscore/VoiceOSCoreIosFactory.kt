/**
 * VoiceOSCoreIosFactory.kt - iOS factory for VoiceOSCore
 *
 * Provides IosHandlerFactory (6 handlers scoped to iOS capabilities)
 * and the VoiceOSCore.Companion.createForIOS() extension function that
 * assembles all platform components into a working VoiceOSCore instance.
 *
 * iOS Handler Coverage:
 * - SystemHandler(IosSystemExecutor) — limited system nav (goBack via callback)
 * - AppHandler(IosAppLauncher) — app launching via URL schemes
 * - IosVoiceControlHandler — mute/wake/dictation/command mode
 * - IosNoteCommandHandler — NoteAvanue voice commands
 * - IosCockpitCommandHandler — Cockpit voice commands
 * - WebCommandHandler — browser/web commands (KMP commonMain)
 *
 * Not included (no iOS equivalent):
 * - GestureHandler (requires AccessibilityService)
 * - CursorHandler (requires overlay + AccessibilityService)
 * - MediaHandler (requires media session control)
 * - ScreenHandler (requires AccessibilityService)
 * - TextHandler (requires AccessibilityService)
 * - InputHandler (requires AccessibilityService)
 * - AppControlHandler (requires AccessibilityService)
 * - ReadingHandler (requires AccessibilityService)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.loader.StaticCommandPersistenceImpl
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/**
 * iOS handler factory — creates the set of handlers that work within
 * the iOS app sandbox (no system-wide AccessibilityService control).
 */
class IosHandlerFactory : HandlerFactory {
    override fun createHandlers(): List<IHandler> = listOf(
        SystemHandler(IosSystemExecutor()),
        AppHandler(IosAppLauncher(), null),
        IosVoiceControlHandler(),
        IosNoteCommandHandler(),
        IosCockpitCommandHandler(),
        WebCommandHandler()
    )
}

/**
 * Creates a fully-configured VoiceOSCore instance for iOS.
 *
 * This mirrors VoiceOSCore.Companion.createForAndroid() but uses iOS-specific
 * platform executors and reads VOS command files from the iOS app bundle
 * instead of Android assets.
 *
 * @param configuration Service configuration (defaults to DEFAULT)
 * @param commandRegistry Optional pre-populated command registry
 * @return A ready-to-initialize VoiceOSCore instance
 */
fun VoiceOSCore.Companion.createForIOS(
    configuration: ServiceConfiguration = ServiceConfiguration.DEFAULT,
    commandRegistry: CommandRegistry? = null
): VoiceOSCore {
    val speechEngineFactory = SpeechEngineFactoryProvider.create()
    val handlerFactory = IosHandlerFactory()

    // Database — iOS uses NativeSqliteDriver (no Context needed)
    val driverFactory = DatabaseDriverFactory()
    val dbManager = VoiceOSDatabaseManager.getInstance(driverFactory)
    val database = dbManager.getDatabase()

    // File reader for VOS command files from iOS app bundle.
    // VOS files are expected in the bundle under localization/commands/ directory.
    // The path argument is e.g. "localization/commands/en-US.app.vos"
    val fileReader: (String) -> String? = { path ->
        readBundleResource(path)
    }

    val staticCommandPersistence = StaticCommandPersistenceImpl(database, fileReader)

    val builder = VoiceOSCore.Builder()
        .withHandlerFactory(handlerFactory)
        .withSpeechEngineFactory(speechEngineFactory)
        .withConfiguration(configuration)
        .withStaticCommandPersistence(staticCommandPersistence)

    if (commandRegistry != null) {
        builder.withCommandRegistry(commandRegistry)
    }

    NSLog("VoiceOSCoreIosFactory: Created VoiceOSCore with ${handlerFactory.createHandlers().size} iOS handlers")
    return builder.build()
}

/**
 * Reads a resource file from the iOS app bundle.
 *
 * Handles two naming patterns:
 * 1. Path with directory separators: "localization/commands/en-US.app.vos"
 *    → resource name "en-US.app", type "vos", subdirectory "localization/commands"
 * 2. Simple filename: "en-US.app.vos"
 *    → resource name "en-US.app", type "vos"
 *
 * @param path The relative path to the resource
 * @return The file contents as a String, or null if not found
 */
@OptIn(ExperimentalForeignApi::class)
private fun readBundleResource(path: String): String? {
    val lastSlash = path.lastIndexOf('/')
    val directory = if (lastSlash >= 0) path.substring(0, lastSlash) else null
    val filename = if (lastSlash >= 0) path.substring(lastSlash + 1) else path

    val lastDot = filename.lastIndexOf('.')
    val resourceName = if (lastDot >= 0) filename.substring(0, lastDot) else filename
    val resourceType = if (lastDot >= 0) filename.substring(lastDot + 1) else null

    val fullPath = if (directory != null) {
        NSBundle.mainBundle.pathForResource(resourceName, ofType = resourceType, inDirectory = directory)
    } else {
        NSBundle.mainBundle.pathForResource(resourceName, ofType = resourceType)
    }

    if (fullPath == null) {
        NSLog("VoiceOSCoreIosFactory: Resource not found in bundle: $path")
        return null
    }

    return try {
        @Suppress("CAST_NEVER_SUCCEEDS")
        NSString.stringWithContentsOfFile(fullPath, encoding = NSUTF8StringEncoding, error = null) as? String
    } catch (e: Exception) {
        NSLog("VoiceOSCoreIosFactory: Failed to read resource $path: ${e.message}")
        null
    }
}
