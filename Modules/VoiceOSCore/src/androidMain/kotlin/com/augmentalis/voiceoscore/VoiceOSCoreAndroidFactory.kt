/**
 * VoiceOSCoreAndroidFactory.kt - Android factory extensions for VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Provides Android-specific factory functions for creating VoiceOSCore instances.
 */
package com.augmentalis.voiceoscore

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.handlers.*
import com.augmentalis.voiceoscore.loader.StaticCommandPersistenceImpl

private const val TAG = "VoiceOSFactory"

/**
 * Create a VoiceOSCore instance configured for Android.
 *
 * This is the primary entry point for Android apps using VoiceOSCore.
 * It sets up Android-specific speech engines, handlers, and configuration.
 *
 * Wires [StaticCommandPersistenceImpl] so that VoiceOSCore.initialize()
 * populates the commands_static table from VOS seed files on first run.
 *
 * @param service The accessibility service for gesture dispatch
 * @param configuration Service configuration options
 * @param commandRegistry Shared command registry (optional, creates new if null)
 * @return Configured VoiceOSCore instance
 */
fun VoiceOSCore.Companion.createForAndroid(
    service: AccessibilityService,
    configuration: ServiceConfiguration = ServiceConfiguration.DEFAULT,
    commandRegistry: CommandRegistry? = null
): VoiceOSCore {
    // Get speech engine factory from provider
    val speechEngineFactory = SpeechEngineFactoryProvider.create()

    // Create Android handler factory with the accessibility service
    val handlerFactory = AndroidHandlerFactory(service)

    // Get VoiceOSDatabase from VoiceOSDatabaseManager singleton.
    // The accessibility service already calls VoiceOSDatabaseManager.getInstance()
    // before createForAndroid(), so the database is already initialized.
    val dbManager = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(service.applicationContext))
    val database: VoiceOSDatabase = dbManager.getDatabase()

    // Create platform file reader for Android assets
    val context = service.applicationContext
    val fileReader: (String) -> String? = { path ->
        try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read asset: $path", e)
            null
        }
    }

    // Create KMP static command persistence
    val persistence = StaticCommandPersistenceImpl(
        database = database,
        fileReader = fileReader,
        fallbackLocale = "en-US"
    )

    // Build the VoiceOSCore instance with persistence wired
    return VoiceOSCore.Builder()
        .withHandlerFactory(handlerFactory)
        .withSpeechEngineFactory(speechEngineFactory)
        .withConfiguration(configuration)
        .withStaticCommandPersistence(persistence)
        .apply {
            commandRegistry?.let { withCommandRegistry(it) }
        }
        .build()
}

/**
 * Android-specific handler factory.
 *
 * Creates handlers appropriate for Android accessibility service context.
 */
internal class AndroidHandlerFactory(
    private val service: AccessibilityService
) : HandlerFactory {

    override fun createHandlers(): List<IHandler> {
        return listOf(
            // Existing handlers
            AndroidGestureHandler(service),
            SystemHandler(AndroidSystemExecutor(service)),
            AppHandler(AndroidAppLauncher(service)),
            AndroidCursorHandler(service),
            // Wave 2: Static command dispatch handlers
            MediaHandler(service),
            ScreenHandler(service),
            TextHandler(service),
            InputHandler(service),
            AppControlHandler(service),
            ReadingHandler(service),
            VoiceControlHandler(service),
            // Wave 3: Module-specific handlers
            NoteCommandHandler(service),
            CockpitCommandHandler(service),
            // Wave 4: Media/utility module handlers
            AnnotationCommandHandler(service),
            ImageCommandHandler(service),
            VideoCommandHandler(service),
            CastCommandHandler(service),
            AICommandHandler(service)
        )
    }
}

/**
 * Android implementation of SystemExecutor.
 * Uses AccessibilityService global actions for system commands.
 */
internal class AndroidSystemExecutor(
    private val service: AccessibilityService
) : SystemExecutor {

    override suspend fun goBack(): Boolean {
        Log.d(TAG, "Executing goBack")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    override suspend fun goHome(): Boolean {
        Log.d(TAG, "Executing goHome")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    override suspend fun showRecents(): Boolean {
        Log.d(TAG, "Executing showRecents")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    override suspend fun showNotifications(): Boolean {
        Log.d(TAG, "Executing showNotifications")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    override suspend fun showQuickSettings(): Boolean {
        Log.d(TAG, "Executing showQuickSettings")
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }

    override suspend fun showPowerMenu(): Boolean {
        Log.d(TAG, "Executing showPowerMenu")
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
        } else {
            false
        }
    }

    override suspend fun lockScreen(): Boolean {
        Log.d(TAG, "Executing lockScreen")
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    override suspend fun openAppDrawer(): Boolean {
        Log.d(TAG, "Executing openAppDrawer")
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // GLOBAL_ACTION_ALL_APPS available on API 30+ (Android 11)
            service.performGlobalAction(14) // AccessibilityService.GLOBAL_ACTION_ALL_APPS = 14
        } else {
            // Fallback: open home first, which many launchers follow with app drawer swipe
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }
    }
}

/**
 * Gesture handler that implements IHandler interface for Android.
 *
 * Handles gesture-based commands using AndroidGestureDispatcher.
 * Supports scroll, tap (with coordinates in params), and global actions.
 * Uses BoundsResolver for layered bounds resolution to handle stale cached bounds.
 */
internal class AndroidGestureHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    private val dispatcher = AndroidGestureDispatcher(service)
    private val boundsResolver = BoundsResolver(service)

    override val category: ActionCategory = ActionCategory.NAVIGATION

    override val supportedActions: List<String> = listOf(
        "tap", "click", "press",
        "long press", "long click", "hold",
        "scroll up", "scroll down", "scroll left", "scroll right",
        "page up", "page down",
        "swipe up", "swipe down", "swipe left", "swipe right",
        // Advanced gestures (parallel to web gesture commands)
        "double tap", "double click",
        "pinch in", "pinch out", "pinch to zoom in", "pinch to zoom out",
        "fling up", "fling down", "fling left", "fling right",
        "flick up", "flick down", "flick left", "flick right",
        "throw", "toss",
        "scale up", "scale down", "enlarge", "shrink",
        "grab", "grab element", "lock element",
        "release", "let go", "drop",
        "pan left", "pan right", "pan up", "pan down",
        "select word", "clear selection", "deselect",
        "zoom in", "zoom out", "reset zoom"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "AndroidGestureHandler.execute: phrase='$phrase', actionType=${command.actionType}")

        return try {
            // ═══════════════════════════════════════════════════════════════
            // Phrase-based routing — handles commands arriving from the
            // static handler path (actionType=EXECUTE) as well as
            // scroll/swipe commands regardless of actionType.
            // ═══════════════════════════════════════════════════════════════
            when {
                // ── Scroll ──────────────────────────────────────────────
                phrase.startsWith("scroll down") || phrase.startsWith("swipe up") -> {
                    val success = dispatcher.scroll("down")
                    return if (success) HandlerResult.success("Scrolled down")
                           else HandlerResult.failure("Failed to scroll down")
                }
                phrase.startsWith("scroll up") || phrase.startsWith("swipe down") -> {
                    val success = dispatcher.scroll("up")
                    return if (success) HandlerResult.success("Scrolled up")
                           else HandlerResult.failure("Failed to scroll up")
                }
                phrase.startsWith("scroll left") || phrase.startsWith("swipe right") -> {
                    val success = dispatcher.scroll("left")
                    return if (success) HandlerResult.success("Scrolled left")
                           else HandlerResult.failure("Failed to scroll left")
                }
                phrase.startsWith("scroll right") || phrase.startsWith("swipe left") -> {
                    val success = dispatcher.scroll("right")
                    return if (success) HandlerResult.success("Scrolled right")
                           else HandlerResult.failure("Failed to scroll right")
                }

                // ── Page scroll (bigger gesture) ────────────────────────
                phrase == "page up" -> {
                    val success = dispatcher.scroll("up")
                    return if (success) HandlerResult.success("Page up")
                           else HandlerResult.failure("Failed to page up")
                }
                phrase == "page down" -> {
                    val success = dispatcher.scroll("down")
                    return if (success) HandlerResult.success("Page down")
                           else HandlerResult.failure("Failed to page down")
                }

                // ── Tap/Click (standalone — screen center) ──────────────
                phrase == "tap" || phrase == "click" || phrase == "press" -> {
                    val metrics = service.resources.displayMetrics
                    val success = dispatcher.tap(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                    return if (success) HandlerResult.success("Tapped")
                           else HandlerResult.failure("Failed to tap")
                }

                // ── Long press (standalone — screen center) ─────────────
                phrase in listOf("long press", "long click", "hold") -> {
                    val metrics = service.resources.displayMetrics
                    val success = dispatcher.longPress(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                    return if (success) HandlerResult.success("Long pressed")
                           else HandlerResult.failure("Failed to long press")
                }

                // ── Double tap (standalone — screen center) ─────────────
                phrase in listOf("double tap", "double click") -> {
                    val metrics = service.resources.displayMetrics
                    val success = dispatcher.doubleTap(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                    return if (success) HandlerResult.success("Double tapped")
                           else HandlerResult.failure("Failed to double tap")
                }

                // ── Zoom ────────────────────────────────────────────────
                phrase in listOf("zoom in", "pinch out", "pinch to zoom in") -> {
                    val success = dispatcher.pinch(1.5f)
                    return if (success) HandlerResult.success("Zoomed in")
                           else HandlerResult.failure("Failed to zoom in")
                }
                phrase in listOf("zoom out", "pinch in", "pinch to zoom out") -> {
                    val success = dispatcher.pinch(0.5f)
                    return if (success) HandlerResult.success("Zoomed out")
                           else HandlerResult.failure("Failed to zoom out")
                }
                phrase == "reset zoom" -> {
                    val success = dispatcher.pinch(1.0f)
                    return if (success) HandlerResult.success("Reset zoom")
                           else HandlerResult.failure("Failed to reset zoom")
                }

                // ── Scale ───────────────────────────────────────────────
                phrase in listOf("scale up", "enlarge") -> {
                    val success = dispatcher.pinch(1.5f)
                    return if (success) HandlerResult.success("Scaled up")
                           else HandlerResult.failure("Failed to scale up")
                }
                phrase in listOf("scale down", "shrink") -> {
                    val success = dispatcher.pinch(0.5f)
                    return if (success) HandlerResult.success("Scaled down")
                           else HandlerResult.failure("Failed to scale down")
                }

                // ── Fling / Flick / Throw ───────────────────────────────
                phrase.startsWith("fling") || phrase.startsWith("flick") ||
                phrase == "throw" || phrase == "toss" -> {
                    val direction = extractDirectionFromPhrase(phrase) ?: "down"
                    val success = dispatcher.fling(direction)
                    return if (success) HandlerResult.success("Flung $direction")
                           else HandlerResult.failure("Failed to fling $direction")
                }

                // ── Grab / Release ──────────────────────────────────────
                phrase in listOf("grab", "grab element", "lock element") -> {
                    val metrics = service.resources.displayMetrics
                    val success = dispatcher.longPress(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                    return if (success) HandlerResult.success("Grabbed element")
                           else HandlerResult.failure("Failed to grab")
                }
                phrase in listOf("release", "let go", "drop") -> {
                    val metrics = service.resources.displayMetrics
                    val success = dispatcher.tap(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                    return if (success) HandlerResult.success("Released")
                           else HandlerResult.failure("Failed to release")
                }

                // ── Pan ─────────────────────────────────────────────────
                phrase.startsWith("pan ") -> {
                    val direction = extractDirectionFromPhrase(phrase) ?: "down"
                    val success = dispatcher.scroll(direction)
                    return if (success) HandlerResult.success("Panned $direction")
                           else HandlerResult.failure("Failed to pan $direction")
                }

                // ── Select word / Clear selection ───────────────────────
                phrase == "select word" -> {
                    val metrics = service.resources.displayMetrics
                    val success = dispatcher.doubleTap(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                    return if (success) HandlerResult.success("Selected word")
                           else HandlerResult.failure("Failed to select word")
                }
                phrase in listOf("clear selection", "deselect") -> {
                    val metrics = service.resources.displayMetrics
                    val success = dispatcher.tap(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                    return if (success) HandlerResult.success("Selection cleared")
                           else HandlerResult.failure("Failed to clear selection")
                }
            }

            // Then route by actionType
            when (command.actionType) {
                CommandActionType.TAP, CommandActionType.CLICK -> {
                    Log.d(TAG, "Executing TAP/CLICK for '${command.phrase}', metadata: ${command.metadata}")
                    // Check if coordinates are provided in params (direct tap)
                    val x = params["x"] as? Float
                    val y = params["y"] as? Float
                    if (x != null && y != null) {
                        Log.d(TAG, "Tapping with coords: ($x, $y)")
                        val success = dispatcher.tap(x, y)
                        if (success) {
                            HandlerResult.success("Tapped ${command.phrase}")
                        } else {
                            HandlerResult.failure("Failed to tap")
                        }
                    } else {
                        // Use BoundsResolver for layered bounds resolution
                        // This handles stale cached bounds by trying multiple strategies
                        val bounds = boundsResolver.resolve(command)
                        if (bounds != null) {
                            Log.d(TAG, "Clicking with resolved bounds: ${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}")
                            val success = dispatcher.click(bounds)
                            if (success) {
                                Log.d(TAG, "Click succeeded for '${command.phrase}'")
                                HandlerResult.success("Clicked ${command.phrase}")
                            } else {
                                Log.w(TAG, "Click failed for '${command.phrase}'")
                                HandlerResult.failure("Failed to click")
                            }
                        } else {
                            Log.w(TAG, "BoundsResolver failed for '${command.phrase}', returning notHandled")
                            HandlerResult.notHandled()
                        }
                    }
                }

                CommandActionType.LONG_CLICK -> {
                    val x = params["x"] as? Float
                    val y = params["y"] as? Float
                    if (x != null && y != null) {
                        val success = dispatcher.longPress(x, y)
                        if (success) {
                            HandlerResult.success("Long pressed ${command.phrase}")
                        } else {
                            HandlerResult.failure("Failed to long press")
                        }
                    } else {
                        // Use BoundsResolver for layered bounds resolution
                        val bounds = boundsResolver.resolve(command)
                        if (bounds != null) {
                            val centerX = bounds.centerX.toFloat()
                            val centerY = bounds.centerY.toFloat()
                            val success = dispatcher.longPress(centerX, centerY)
                            if (success) {
                                HandlerResult.success("Long pressed ${command.phrase}")
                            } else {
                                HandlerResult.failure("Failed to long press")
                            }
                        } else {
                            HandlerResult.notHandled()
                        }
                    }
                }

                CommandActionType.SCROLL_DOWN -> {
                    val success = dispatcher.scroll("down")
                    if (success) {
                        HandlerResult.success("Scrolled down")
                    } else {
                        HandlerResult.failure("Failed to scroll down")
                    }
                }

                CommandActionType.SCROLL_UP -> {
                    val success = dispatcher.scroll("up")
                    if (success) {
                        HandlerResult.success("Scrolled up")
                    } else {
                        HandlerResult.failure("Failed to scroll up")
                    }
                }

                CommandActionType.SCROLL_LEFT -> {
                    val success = dispatcher.scroll("left")
                    if (success) {
                        HandlerResult.success("Scrolled left")
                    } else {
                        HandlerResult.failure("Failed to scroll left")
                    }
                }

                CommandActionType.SCROLL_RIGHT -> {
                    val success = dispatcher.scroll("right")
                    if (success) {
                        HandlerResult.success("Scrolled right")
                    } else {
                        HandlerResult.failure("Failed to scroll right")
                    }
                }

                CommandActionType.BACK -> {
                    val success = dispatcher.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_BACK
                    )
                    if (success) {
                        HandlerResult.success("Navigated back")
                    } else {
                        HandlerResult.failure("Failed to go back")
                    }
                }

                CommandActionType.HOME -> {
                    val success = dispatcher.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_HOME
                    )
                    if (success) {
                        HandlerResult.success("Navigated home")
                    } else {
                        HandlerResult.failure("Failed to go home")
                    }
                }

                CommandActionType.RECENT_APPS -> {
                    val success = dispatcher.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_RECENTS
                    )
                    if (success) {
                        HandlerResult.success("Opened recent apps")
                    } else {
                        HandlerResult.failure("Failed to open recents")
                    }
                }

                CommandActionType.NOTIFICATIONS -> {
                    val success = dispatcher.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                    )
                    if (success) {
                        HandlerResult.success("Opened notifications")
                    } else {
                        HandlerResult.failure("Failed to open notifications")
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // Advanced Gesture Actions (parallel to web JS gestures)
                // ═══════════════════════════════════════════════════════════

                CommandActionType.DOUBLE_CLICK -> {
                    val x = params["x"] as? Float
                    val y = params["y"] as? Float
                    if (x != null && y != null) {
                        val success = dispatcher.doubleTap(x, y)
                        if (success) HandlerResult.success("Double tapped")
                        else HandlerResult.failure("Failed to double tap")
                    } else {
                        val bounds = boundsResolver.resolve(command)
                        if (bounds != null) {
                            val success = dispatcher.doubleTap(bounds.centerX.toFloat(), bounds.centerY.toFloat())
                            if (success) HandlerResult.success("Double tapped ${command.phrase}")
                            else HandlerResult.failure("Failed to double tap")
                        } else HandlerResult.notHandled()
                    }
                }

                CommandActionType.PINCH -> {
                    val scale = command.metadata["scale"]?.toFloatOrNull()
                        ?: params["scale"]?.toString()?.toFloatOrNull()
                        ?: 0.5f // Default pinch-in
                    val success = dispatcher.pinch(scale)
                    val label = if (scale < 1f) "Pinched in" else "Pinched out"
                    if (success) HandlerResult.success(label)
                    else HandlerResult.failure("Failed to pinch")
                }

                CommandActionType.FLING -> {
                    val direction = command.metadata["direction"]
                        ?: params["direction"]?.toString()
                        ?: extractDirectionFromPhrase(phrase)
                        ?: "down"
                    val success = dispatcher.fling(direction)
                    if (success) HandlerResult.success("Flung $direction")
                    else HandlerResult.failure("Failed to fling $direction")
                }

                CommandActionType.THROW -> {
                    // Throw = fling with velocity, same gesture dispatch
                    val direction = command.metadata["direction"]
                        ?: params["direction"]?.toString()
                        ?: "up"
                    val success = dispatcher.fling(direction)
                    if (success) HandlerResult.success("Threw element $direction")
                    else HandlerResult.failure("Failed to throw")
                }

                CommandActionType.PAN -> {
                    // Pan maps to scroll on Android native
                    val direction = command.metadata["direction"]
                        ?: params["direction"]?.toString()
                        ?: extractDirectionFromPhrase(phrase)
                        ?: "down"
                    val success = dispatcher.scroll(direction)
                    if (success) HandlerResult.success("Panned $direction")
                    else HandlerResult.failure("Failed to pan $direction")
                }

                CommandActionType.SCALE -> {
                    val factor = command.metadata["factor"]?.toFloatOrNull()
                        ?: params["factor"]?.toString()?.toFloatOrNull()
                        ?: 1.5f
                    val success = dispatcher.pinch(factor)
                    val label = if (factor > 1f) "Scaled up" else "Scaled down"
                    if (success) HandlerResult.success(label)
                    else HandlerResult.failure("Failed to scale")
                }

                CommandActionType.GRAB -> {
                    // Grab = long press (start drag) on Android
                    val x = params["x"] as? Float
                    val y = params["y"] as? Float
                    if (x != null && y != null) {
                        val success = dispatcher.longPress(x, y)
                        if (success) HandlerResult.success("Grabbed element")
                        else HandlerResult.failure("Failed to grab")
                    } else {
                        val bounds = boundsResolver.resolve(command)
                        if (bounds != null) {
                            val success = dispatcher.longPress(bounds.centerX.toFloat(), bounds.centerY.toFloat())
                            if (success) HandlerResult.success("Grabbed element")
                            else HandlerResult.failure("Failed to grab")
                        } else HandlerResult.notHandled()
                    }
                }

                CommandActionType.RELEASE -> {
                    // Release = tap to cancel drag state
                    val metrics = service.resources.displayMetrics
                    val success = dispatcher.tap(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                    if (success) HandlerResult.success("Released element")
                    else HandlerResult.failure("Failed to release")
                }

                CommandActionType.DRAG -> {
                    val startX = params["startX"]?.toString()?.toFloatOrNull()
                    val startY = params["startY"]?.toString()?.toFloatOrNull()
                    val endX = params["endX"]?.toString()?.toFloatOrNull()
                    val endY = params["endY"]?.toString()?.toFloatOrNull()
                    if (startX != null && startY != null && endX != null && endY != null) {
                        val success = dispatcher.drag(startX, startY, endX, endY)
                        if (success) HandlerResult.success("Dragged element")
                        else HandlerResult.failure("Failed to drag")
                    } else HandlerResult.failure("Drag requires startX, startY, endX, endY params")
                }

                CommandActionType.SELECT_WORD -> {
                    // Double-tap selects a word in Android text views
                    val x = params["x"] as? Float
                    val y = params["y"] as? Float
                    if (x != null && y != null) {
                        val success = dispatcher.doubleTap(x, y)
                        if (success) HandlerResult.success("Selected word")
                        else HandlerResult.failure("Failed to select word")
                    } else {
                        val bounds = boundsResolver.resolve(command)
                        if (bounds != null) {
                            val success = dispatcher.doubleTap(bounds.centerX.toFloat(), bounds.centerY.toFloat())
                            if (success) HandlerResult.success("Selected word")
                            else HandlerResult.failure("Failed to select word")
                        } else HandlerResult.notHandled()
                    }
                }

                CommandActionType.CLEAR_SELECTION -> {
                    // Tap to deselect on Android
                    val metrics = service.resources.displayMetrics
                    val success = dispatcher.tap(metrics.widthPixels / 2f, metrics.heightPixels / 2f)
                    if (success) HandlerResult.success("Cleared selection")
                    else HandlerResult.failure("Failed to clear selection")
                }

                CommandActionType.ZOOM_IN -> {
                    val success = dispatcher.pinch(1.5f) // Pinch out = zoom in
                    if (success) HandlerResult.success("Zoomed in")
                    else HandlerResult.failure("Failed to zoom in")
                }

                CommandActionType.ZOOM_OUT -> {
                    val success = dispatcher.pinch(0.5f) // Pinch in = zoom out
                    if (success) HandlerResult.success("Zoomed out")
                    else HandlerResult.failure("Failed to zoom out")
                }

                CommandActionType.RESET_ZOOM -> {
                    // No direct native equivalent — pinch to neutral scale
                    val success = dispatcher.pinch(1.0f)
                    if (success) HandlerResult.success("Reset zoom")
                    else HandlerResult.failure("Failed to reset zoom")
                }

                CommandActionType.SWIPE_LEFT -> {
                    val success = dispatcher.scroll("right") // Swipe left = content moves right
                    if (success) HandlerResult.success("Swiped left")
                    else HandlerResult.failure("Failed to swipe left")
                }

                CommandActionType.SWIPE_RIGHT -> {
                    val success = dispatcher.scroll("left") // Swipe right = content moves left
                    if (success) HandlerResult.success("Swiped right")
                    else HandlerResult.failure("Failed to swipe right")
                }

                CommandActionType.SWIPE_UP -> {
                    val success = dispatcher.scroll("down") // Swipe up = scroll down
                    if (success) HandlerResult.success("Swiped up")
                    else HandlerResult.failure("Failed to swipe up")
                }

                CommandActionType.SWIPE_DOWN -> {
                    val success = dispatcher.scroll("up") // Swipe down = scroll up
                    if (success) HandlerResult.success("Swiped down")
                    else HandlerResult.failure("Failed to swipe down")
                }

                CommandActionType.HOVER -> {
                    // Hover has no direct native equivalent; tap lightly as approximation
                    val bounds = boundsResolver.resolve(command)
                    if (bounds != null) {
                        val success = dispatcher.tap(bounds.centerX.toFloat(), bounds.centerY.toFloat())
                        if (success) HandlerResult.success("Hovered on ${command.phrase}")
                        else HandlerResult.failure("Failed to hover")
                    } else HandlerResult.notHandled()
                }

                // Tilt, Orbit, Rotate X/Y/Z, Hover Out — web-only, no native equivalent
                CommandActionType.TILT -> {
                    val direction = command.metadata["direction"]
                        ?: extractDirectionFromPhrase(phrase) ?: "up"
                    val success = dispatcher.scroll(direction)
                    if (success) HandlerResult.success("Tilted $direction (scroll)")
                    else HandlerResult.failure("Failed to tilt")
                }

                CommandActionType.ORBIT -> {
                    val direction = command.metadata["direction"]
                        ?: extractDirectionFromPhrase(phrase) ?: "left"
                    val success = dispatcher.scroll(direction)
                    if (success) HandlerResult.success("Orbited $direction (scroll)")
                    else HandlerResult.failure("Failed to orbit")
                }

                CommandActionType.ROTATE, CommandActionType.ROTATE_X,
                CommandActionType.ROTATE_Y, CommandActionType.ROTATE_Z -> {
                    HandlerResult.failure("Rotation gestures not supported in native Android context", recoverable = true)
                }

                CommandActionType.HOVER_OUT -> {
                    HandlerResult.failure("Hover out not supported in native Android context", recoverable = true)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            HandlerResult.failure("Error executing command: ${e.message}")
        }
    }

    /**
     * Extract directional keyword from a voice phrase.
     * Used for gestures that encode direction in the phrase (e.g., "fling left", "pan up").
     */
    private fun extractDirectionFromPhrase(phrase: String): String? {
        val normalized = phrase.lowercase()
        return when {
            normalized.contains("left") -> "left"
            normalized.contains("right") -> "right"
            normalized.contains("up") -> "up"
            normalized.contains("down") -> "down"
            else -> null
        }
    }
}
