/**
 * AppBarHandler.kt - Voice handler for App Bar/Toolbar interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven app bar and toolbar control
 * Features:
 * - Navigate back via voice command
 * - Open navigation drawer/menu
 * - Activate search functionality
 * - Open overflow/more options menu
 * - Navigate to home screen
 * - Read current screen title for orientation
 * - AVID-based targeting for precise element selection
 * - Voice feedback for navigation actions
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Navigation:
 * - "go back" / "back" - Navigate to previous screen
 * - "home" - Navigate to home/main screen
 *
 * Menu controls:
 * - "menu" / "open menu" - Open navigation drawer
 * - "more options" / "overflow" - Open overflow menu
 *
 * Search:
 * - "search" - Activate search functionality
 *
 * Orientation:
 * - "title" / "where am I" - Read current screen title
 *
 * ## App Bar Components
 *
 * Supported:
 * - ActionBar (legacy Android)
 * - Toolbar (AppCompat)
 * - TopAppBar (Material)
 * - CollapsingToolbarLayout
 * - Custom app bars with standard structure
 */

package com.augmentalis.avanueui.voice.handlers.navigation

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for App Bar/Toolbar interactions.
 *
 * Provides comprehensive voice control for app bar components including:
 * - Navigation actions (back, home)
 * - Menu controls (drawer, overflow)
 * - Search activation
 * - Screen orientation (title reading)
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for app bar operations
 */
class AppBarHandler(
    private val executor: AppBarExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "AppBarHandler"
        private val Log = LoggerFactory.getLogger(TAG)
    }

    override val category: ActionCategory = ActionCategory.NAVIGATION

    override val supportedActions: List<String> = listOf(
        // Navigation
        "go back", "back",
        "home",
        // Menu controls
        "menu", "open menu",
        "more options", "overflow", "options",
        // Search
        "search",
        // Orientation
        "title", "where am I", "current screen"
    )

    /**
     * Callback for voice feedback when navigation occurs.
     */
    var onNavigationAction: ((action: String, details: String) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing app bar command: $normalizedAction" }

        return try {
            when (normalizedAction) {
                // Back navigation
                "go back", "back" -> handleBack(command)

                // Home navigation
                "home" -> handleHome(command)

                // Navigation menu/drawer
                "menu", "open menu" -> handleOpenMenu(command)

                // Overflow menu
                "more options", "overflow", "options" -> handleOverflowMenu(command)

                // Search
                "search" -> handleSearch(command)

                // Title/orientation
                "title", "where am i", "current screen" -> handleReadTitle(command)

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error executing app bar command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "go back" / "back" command.
     *
     * Attempts to navigate back using:
     * 1. App bar back/up button
     * 2. System back navigation
     */
    private suspend fun handleBack(command: QuantizedCommand): HandlerResult {
        val appBarInfo = executor.findAppBar()

        // Try app bar back button first
        if (appBarInfo?.hasBackButton == true) {
            val result = executor.pressBackButton(appBarInfo)
            if (result.success) {
                onNavigationAction?.invoke("back", "Navigated back")
                Log.i { "Back navigation via app bar button" }
                return HandlerResult.Success(
                    message = "Going back",
                    data = mapOf(
                        "action" to "back",
                        "method" to "app_bar_button",
                        "accessibility_announcement" to "Going back"
                    )
                )
            }
        }

        // Fall back to system back
        val result = executor.performSystemBack()
        return if (result.success) {
            onNavigationAction?.invoke("back", "Navigated back via system")
            Log.i { "Back navigation via system" }
            HandlerResult.Success(
                message = "Going back",
                data = mapOf(
                    "action" to "back",
                    "method" to "system_back",
                    "accessibility_announcement" to "Going back"
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Could not navigate back",
                recoverable = true,
                suggestedAction = "Try using the back button on screen"
            )
        }
    }

    /**
     * Handle "home" command.
     *
     * Navigates to the home/main screen of the app.
     */
    private suspend fun handleHome(command: QuantizedCommand): HandlerResult {
        val result = executor.navigateHome()

        return if (result.success) {
            onNavigationAction?.invoke("home", "Navigated to home")
            Log.i { "Home navigation successful" }
            HandlerResult.Success(
                message = "Going home",
                data = mapOf(
                    "action" to "home",
                    "accessibility_announcement" to "Navigating to home"
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Could not navigate home",
                recoverable = true,
                suggestedAction = "Home navigation not available"
            )
        }
    }

    /**
     * Handle "menu" / "open menu" command.
     *
     * Opens the navigation drawer or hamburger menu.
     */
    private suspend fun handleOpenMenu(command: QuantizedCommand): HandlerResult {
        val appBarInfo = executor.findAppBar()

        if (appBarInfo?.hasMenuButton != true) {
            return HandlerResult.Failure(
                reason = "No menu button found",
                recoverable = true,
                suggestedAction = "This screen may not have a navigation menu"
            )
        }

        val result = executor.openNavigationMenu(appBarInfo)

        return if (result.success) {
            onNavigationAction?.invoke("menu", "Opened navigation menu")
            Log.i { "Navigation menu opened" }
            HandlerResult.Success(
                message = "Menu opened",
                data = mapOf(
                    "action" to "open_menu",
                    "accessibility_announcement" to "Navigation menu opened"
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Could not open menu",
                recoverable = true,
                suggestedAction = "Try tapping the menu icon"
            )
        }
    }

    /**
     * Handle "more options" / "overflow" command.
     *
     * Opens the overflow menu (three-dot menu).
     */
    private suspend fun handleOverflowMenu(command: QuantizedCommand): HandlerResult {
        val appBarInfo = executor.findAppBar()

        if (appBarInfo?.hasOverflowMenu != true) {
            return HandlerResult.Failure(
                reason = "No overflow menu found",
                recoverable = true,
                suggestedAction = "This screen may not have additional options"
            )
        }

        val result = executor.openOverflowMenu(appBarInfo)

        return if (result.success) {
            onNavigationAction?.invoke("overflow", "Opened options menu")
            Log.i { "Overflow menu opened" }
            HandlerResult.Success(
                message = "Options menu opened",
                data = mapOf(
                    "action" to "open_overflow",
                    "accessibility_announcement" to "More options menu opened"
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Could not open options menu",
                recoverable = true,
                suggestedAction = "Try tapping the three-dot icon"
            )
        }
    }

    /**
     * Handle "search" command.
     *
     * Activates search functionality in the app bar.
     */
    private suspend fun handleSearch(command: QuantizedCommand): HandlerResult {
        val appBarInfo = executor.findAppBar()

        if (appBarInfo?.hasSearchAction != true) {
            return HandlerResult.Failure(
                reason = "No search available",
                recoverable = true,
                suggestedAction = "This screen may not have search functionality"
            )
        }

        val result = executor.activateSearch(appBarInfo)

        return if (result.success) {
            onNavigationAction?.invoke("search", "Search activated")
            Log.i { "Search activated" }
            HandlerResult.Success(
                message = "Search activated",
                data = mapOf(
                    "action" to "search",
                    "accessibility_announcement" to "Search field activated, ready for input"
                )
            )
        } else {
            HandlerResult.Failure(
                reason = result.error ?: "Could not activate search",
                recoverable = true,
                suggestedAction = "Try tapping the search icon"
            )
        }
    }

    /**
     * Handle "title" / "where am I" command.
     *
     * Reads the current screen title for orientation.
     */
    private suspend fun handleReadTitle(command: QuantizedCommand): HandlerResult {
        val appBarInfo = executor.findAppBar()

        val title = appBarInfo?.title
        val subtitle = appBarInfo?.subtitle

        if (title.isNullOrBlank()) {
            return HandlerResult.Failure(
                reason = "Could not determine current screen",
                recoverable = true,
                suggestedAction = "Title not available for this screen"
            )
        }

        val announcement = buildString {
            append("You are on: $title")
            if (!subtitle.isNullOrBlank()) {
                append(". $subtitle")
            }
        }

        onNavigationAction?.invoke("title", title)
        Log.i { "Title read: $title" }

        return HandlerResult.Success(
            message = announcement,
            data = mapOf(
                "action" to "read_title",
                "title" to title,
                "subtitle" to (subtitle ?: ""),
                "accessibility_announcement" to announcement
            )
        )
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about an app bar component.
 *
 * @property avid AVID fingerprint for the app bar (format: BAR:{hash8})
 * @property title Current screen title displayed in the app bar
 * @property subtitle Optional subtitle text
 * @property hasBackButton Whether a back/up navigation button is present
 * @property hasMenuButton Whether a navigation drawer/hamburger button is present
 * @property hasSearchAction Whether search functionality is available
 * @property hasOverflowMenu Whether an overflow (three-dot) menu is present
 * @property bounds Screen bounds for the app bar
 * @property actionButtons List of action buttons in the app bar
 * @property node Platform-specific node reference
 */
data class AppBarInfo(
    val avid: String,
    val title: String = "",
    val subtitle: String = "",
    val hasBackButton: Boolean = false,
    val hasMenuButton: Boolean = false,
    val hasSearchAction: Boolean = false,
    val hasOverflowMenu: Boolean = false,
    val bounds: Bounds = Bounds.EMPTY,
    val actionButtons: List<ActionButtonInfo> = emptyList(),
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "AppBar",
        text = title,
        bounds = bounds,
        isClickable = false,
        isEnabled = true,
        avid = avid,
        stateDescription = buildString {
            if (hasBackButton) append("back, ")
            if (hasMenuButton) append("menu, ")
            if (hasSearchAction) append("search, ")
            if (hasOverflowMenu) append("more options")
        }.trimEnd(',', ' ')
    )
}

/**
 * Information about an action button in the app bar.
 *
 * @property avid AVID fingerprint for the button
 * @property label Button label or content description
 * @property iconDescription Description of the icon
 * @property bounds Screen bounds for the button
 */
data class ActionButtonInfo(
    val avid: String,
    val label: String,
    val iconDescription: String = "",
    val bounds: Bounds = Bounds.EMPTY
)

/**
 * Result of an app bar operation.
 *
 * @property success Whether the operation succeeded
 * @property error Error message if operation failed
 * @property screenTitle New screen title after navigation (if applicable)
 */
data class AppBarOperationResult(
    val success: Boolean,
    val error: String? = null,
    val screenTitle: String? = null
) {
    companion object {
        fun success(screenTitle: String? = null) = AppBarOperationResult(
            success = true,
            screenTitle = screenTitle
        )

        fun error(message: String) = AppBarOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for app bar operations.
 *
 * Implementations should:
 * 1. Find app bar components by traversing the view hierarchy
 * 2. Identify navigation buttons, action buttons, and menus
 * 3. Execute navigation actions via accessibility services
 * 4. Handle various app bar types (ActionBar, Toolbar, TopAppBar)
 *
 * ## App Bar Detection Algorithm
 *
 * ```kotlin
 * fun findAppBarNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - android.widget.Toolbar
 *     // - androidx.appcompat.widget.Toolbar
 *     // - com.google.android.material.appbar.MaterialToolbar
 *     // - android.view.ActionBar
 *     // Or nodes at the top of screen with navigation/action role
 * }
 * ```
 *
 * ## Button Detection
 *
 * ```kotlin
 * fun identifyButtons(appBarNode: AccessibilityNodeInfo) {
 *     // Back button: contentDescription contains "back", "up", "navigate up"
 *     // Menu button: contentDescription contains "menu", "drawer", "navigation"
 *     // Search: contentDescription contains "search"
 *     // Overflow: contentDescription contains "more", "options", "overflow"
 * }
 * ```
 */
interface AppBarExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // App Bar Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find the app bar/toolbar on the current screen.
     *
     * @return AppBarInfo if found, null otherwise
     */
    suspend fun findAppBar(): AppBarInfo?

    /**
     * Find an app bar by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: BAR:{hash8})
     * @return AppBarInfo if found, null otherwise
     */
    suspend fun findAppBarByAvid(avid: String): AppBarInfo?

    // ═══════════════════════════════════════════════════════════════════════════
    // Navigation Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Press the back/up button in the app bar.
     *
     * @param appBar The app bar containing the back button
     * @return Operation result
     */
    suspend fun pressBackButton(appBar: AppBarInfo): AppBarOperationResult

    /**
     * Perform system back navigation.
     *
     * Falls back to system BACK action when app bar button is unavailable.
     *
     * @return Operation result
     */
    suspend fun performSystemBack(): AppBarOperationResult

    /**
     * Navigate to the home/main screen.
     *
     * May use app-specific deep links or multi-back navigation.
     *
     * @return Operation result
     */
    suspend fun navigateHome(): AppBarOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Menu Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Open the navigation drawer/menu.
     *
     * @param appBar The app bar containing the menu button
     * @return Operation result
     */
    suspend fun openNavigationMenu(appBar: AppBarInfo): AppBarOperationResult

    /**
     * Open the overflow (three-dot) menu.
     *
     * @param appBar The app bar containing the overflow button
     * @return Operation result
     */
    suspend fun openOverflowMenu(appBar: AppBarInfo): AppBarOperationResult

    /**
     * Close any open menus.
     *
     * @return Operation result
     */
    suspend fun closeMenus(): AppBarOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Action Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Activate search functionality.
     *
     * @param appBar The app bar containing the search action
     * @return Operation result
     */
    suspend fun activateSearch(appBar: AppBarInfo): AppBarOperationResult

    /**
     * Press an action button by its label or content description.
     *
     * @param appBar The app bar containing the action
     * @param actionLabel Label or description of the action
     * @return Operation result
     */
    suspend fun pressActionButton(appBar: AppBarInfo, actionLabel: String): AppBarOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Title Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the current title displayed in the app bar.
     *
     * @return Current title, or null if unavailable
     */
    suspend fun getCurrentTitle(): String?

    /**
     * Get the current subtitle displayed in the app bar.
     *
     * @return Current subtitle, or null if unavailable
     */
    suspend fun getCurrentSubtitle(): String?
}
