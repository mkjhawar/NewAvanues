/**
 * DrawerHandler.kt
 *
 * Created: 2026-01-27 23:00 PST
 * Last Modified: 2026-01-28 12:00 PST
 * Author: VOS4 Development Team
 * Version: 2.0.0
 *
 * Purpose: Voice command handler for navigation drawer operations
 * Features: Open/close drawer, menu item navigation, quick access shortcuts
 * Location: MagicVoiceHandlers module
 *
 * Changelog:
 * - v2.0.0 (2026-01-28): Migrated to BaseHandler pattern with executor
 * - v1.0.0 (2026-01-27): Initial implementation for navigation drawer voice control
 */

package com.augmentalis.avamagic.voice.handlers.feedback

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for VoiceOS navigation drawer
 *
 * Routes commands to navigation drawer operations via executor pattern.
 *
 * Supported commands:
 * - "open drawer" / "show menu" - Opens the navigation drawer
 * - "close drawer" / "hide menu" - Closes the navigation drawer
 * - "go to [menu item]" - Navigate to a menu item by name
 * - "home" - Go to home/main screen
 * - "settings" - Go to settings screen
 * - "profile" - Go to profile screen
 * - "menu item [N]" - Select the Nth menu item (1-indexed)
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for drawer operations
 */
class DrawerHandler(
    private val executor: DrawerExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "DrawerHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Quick access menu items (case-insensitive matching)
        private val QUICK_ACCESS_ITEMS = mapOf(
            "home" to listOf("home", "main", "dashboard", "start"),
            "settings" to listOf("settings", "preferences", "options", "configuration"),
            "profile" to listOf("profile", "account", "my account", "user"),
            "help" to listOf("help", "support", "faq"),
            "about" to listOf("about", "info", "information"),
            "logout" to listOf("logout", "log out", "sign out", "signout", "exit")
        )

        // Number words to integer mapping
        private val NUMBER_WORDS = mapOf(
            "one" to 1, "first" to 1,
            "two" to 2, "second" to 2,
            "three" to 3, "third" to 3,
            "four" to 4, "fourth" to 4,
            "five" to 5, "fifth" to 5,
            "six" to 6, "sixth" to 6,
            "seven" to 7, "seventh" to 7,
            "eight" to 8, "eighth" to 8,
            "nine" to 9, "ninth" to 9,
            "ten" to 10, "tenth" to 10
        )

        // Open drawer commands
        private val OPEN_DRAWER_COMMANDS = setOf(
            "open drawer", "show menu", "show drawer", "open menu",
            "open navigation", "show navigation", "menu"
        )

        // Close drawer commands
        private val CLOSE_DRAWER_COMMANDS = setOf(
            "close drawer", "hide menu", "hide drawer", "close menu",
            "close navigation", "hide navigation", "dismiss menu", "dismiss drawer"
        )

        // Navigation prefixes
        private val NAVIGATION_PREFIXES = listOf("go to ", "navigate to ", "open ", "select ")

        // Menu item index prefixes
        private val MENU_ITEM_PREFIXES = listOf("menu item ", "item ", "select item ", "option ")
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Open drawer commands
        "open drawer", "show menu", "show drawer", "open menu",
        "open navigation", "show navigation", "menu",

        // Close drawer commands
        "close drawer", "hide menu", "hide drawer", "close menu",
        "close navigation", "hide navigation", "dismiss menu", "dismiss drawer",

        // Navigation commands
        "go to", "navigate to",

        // Quick access shortcuts
        "home", "main screen", "settings", "profile", "help", "about", "logout",

        // Index-based navigation
        "menu item", "item", "select item", "option"
    )

    /**
     * Callback for voice feedback when drawer state changes
     */
    var onDrawerStateChanged: ((isOpen: Boolean) -> Unit)? = null

    /**
     * Callback for voice feedback when menu item is selected
     */
    var onMenuItemSelected: ((itemName: String) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing drawer command: $normalizedAction" }

        return try {
            when {
                // Open drawer commands
                isOpenDrawerCommand(normalizedAction) -> handleOpenDrawer()

                // Close drawer commands
                isCloseDrawerCommand(normalizedAction) -> handleCloseDrawer()

                // Quick access shortcuts
                isQuickAccessCommand(normalizedAction) -> handleQuickAccess(normalizedAction)

                // Index-based menu item selection
                isMenuItemIndexCommand(normalizedAction) -> handleMenuItemIndex(normalizedAction)

                // Navigation commands with "go to", "navigate to", etc.
                isNavigationCommand(normalizedAction) -> handleNavigation(normalizedAction)

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error processing drawer command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Type Detection
    // ═══════════════════════════════════════════════════════════════════════════

    private fun isOpenDrawerCommand(command: String): Boolean {
        return command in OPEN_DRAWER_COMMANDS
    }

    private fun isCloseDrawerCommand(command: String): Boolean {
        return command in CLOSE_DRAWER_COMMANDS
    }

    private fun isNavigationCommand(command: String): Boolean {
        return NAVIGATION_PREFIXES.any { command.startsWith(it) }
    }

    private fun isQuickAccessCommand(command: String): Boolean {
        return QUICK_ACCESS_ITEMS.keys.any { key ->
            command == key || QUICK_ACCESS_ITEMS[key]?.contains(command) == true
        }
    }

    private fun isMenuItemIndexCommand(command: String): Boolean {
        return MENU_ITEM_PREFIXES.any { command.startsWith(it) } ||
                command.matches(Regex("^(menu\\s+)?item\\s+(\\d+|one|two|three|four|five|six|seven|eight|nine|ten|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)$"))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    private suspend fun handleOpenDrawer(): HandlerResult {
        val result = executor.openDrawer()

        return when (result) {
            is DrawerResult.Success -> {
                onDrawerStateChanged?.invoke(true)
                HandlerResult.Success(
                    message = "Drawer opened",
                    data = mapOf("drawerState" to "open")
                )
            }
            is DrawerResult.AlreadyInState -> {
                HandlerResult.Success(
                    message = "Drawer is already open",
                    data = mapOf("drawerState" to "open")
                )
            }
            is DrawerResult.NotFound -> {
                HandlerResult.failure(
                    reason = "Navigation drawer not found",
                    recoverable = true
                )
            }
            is DrawerResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            DrawerResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            is DrawerResult.MenuItemSelected -> {
                HandlerResult.success(message = "Drawer opened")
            }
        }
    }

    private suspend fun handleCloseDrawer(): HandlerResult {
        val result = executor.closeDrawer()

        return when (result) {
            is DrawerResult.Success -> {
                onDrawerStateChanged?.invoke(false)
                HandlerResult.Success(
                    message = "Drawer closed",
                    data = mapOf("drawerState" to "closed")
                )
            }
            is DrawerResult.AlreadyInState -> {
                HandlerResult.Success(
                    message = "Drawer is already closed",
                    data = mapOf("drawerState" to "closed")
                )
            }
            is DrawerResult.NotFound -> {
                HandlerResult.failure(
                    reason = "Navigation drawer not found",
                    recoverable = true
                )
            }
            is DrawerResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            DrawerResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            is DrawerResult.MenuItemSelected -> {
                HandlerResult.success(message = "Drawer closed")
            }
        }
    }

    private suspend fun handleQuickAccess(command: String): HandlerResult {
        // Find which quick access category matches
        val targetCategory = QUICK_ACCESS_ITEMS.entries.find { (key, aliases) ->
            command == key || aliases.contains(command)
        }?.key

        if (targetCategory == null) {
            Log.w { "No quick access match for: $command" }
            return HandlerResult.notHandled()
        }

        // Search for menu item matching the category
        val searchTerms = QUICK_ACCESS_ITEMS[targetCategory] ?: listOf(targetCategory)
        val result = executor.navigateToMenuItem(searchTerms)

        return when (result) {
            is DrawerResult.MenuItemSelected -> {
                onMenuItemSelected?.invoke(result.itemName)
                HandlerResult.Success(
                    message = "Navigated to ${result.itemName}",
                    data = mapOf("menuItem" to result.itemName)
                )
            }
            is DrawerResult.Success -> {
                HandlerResult.success(message = "Navigated to $targetCategory")
            }
            is DrawerResult.NotFound -> {
                HandlerResult.failure(
                    reason = "Menu item '$targetCategory' not found",
                    recoverable = true
                )
            }
            is DrawerResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            DrawerResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            is DrawerResult.AlreadyInState -> {
                HandlerResult.success(message = "Already at $targetCategory")
            }
        }
    }

    private suspend fun handleNavigation(command: String): HandlerResult {
        // Extract target from command
        val target = extractNavigationTarget(command)
        if (target.isBlank()) {
            Log.w { "No navigation target extracted from: $command" }
            return HandlerResult.notHandled()
        }

        val result = executor.navigateToMenuItem(listOf(target))

        return when (result) {
            is DrawerResult.MenuItemSelected -> {
                onMenuItemSelected?.invoke(result.itemName)
                HandlerResult.Success(
                    message = "Navigated to ${result.itemName}",
                    data = mapOf("menuItem" to result.itemName)
                )
            }
            is DrawerResult.Success -> {
                HandlerResult.success(message = "Navigated to $target")
            }
            is DrawerResult.NotFound -> {
                HandlerResult.failure(
                    reason = "Menu item '$target' not found",
                    recoverable = true
                )
            }
            is DrawerResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            DrawerResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            is DrawerResult.AlreadyInState -> {
                HandlerResult.success(message = "Already at $target")
            }
        }
    }

    private suspend fun handleMenuItemIndex(command: String): HandlerResult {
        val index = extractMenuIndex(command)
        if (index < 1) {
            Log.w { "Invalid menu index in command: $command" }
            return HandlerResult.failure(
                reason = "Invalid menu item index",
                recoverable = true
            )
        }

        val result = executor.selectMenuItemByIndex(index)

        return when (result) {
            is DrawerResult.MenuItemSelected -> {
                onMenuItemSelected?.invoke(result.itemName)
                HandlerResult.Success(
                    message = "Selected menu item $index: ${result.itemName}",
                    data = mapOf(
                        "menuItem" to result.itemName,
                        "index" to index
                    )
                )
            }
            is DrawerResult.Success -> {
                HandlerResult.success(message = "Selected menu item $index")
            }
            is DrawerResult.NotFound -> {
                HandlerResult.Failure(
                    reason = "Menu item $index not found",
                    recoverable = true,
                    suggestedAction = "Try a different menu item number"
                )
            }
            is DrawerResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            DrawerResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            is DrawerResult.AlreadyInState -> {
                HandlerResult.success(message = "Menu item $index already selected")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    private fun extractNavigationTarget(command: String): String {
        for (prefix in NAVIGATION_PREFIXES) {
            if (command.startsWith(prefix)) {
                return command.removePrefix(prefix).trim()
            }
        }
        return ""
    }

    private fun extractMenuIndex(command: String): Int {
        // Try to extract number directly
        val numberMatch = Regex("(\\d+)").find(command)
        if (numberMatch != null) {
            return numberMatch.value.toIntOrNull() ?: -1
        }

        // Try to match word numbers
        for ((word, value) in NUMBER_WORDS) {
            if (command.contains(word)) {
                return value
            }
        }

        return -1
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Drawer handler status
 */
data class DrawerHandlerStatus(
    val isInitialized: Boolean,
    val hasAccessibilityService: Boolean,
    val supportedCommandCount: Int
)

/**
 * Drawer operation result
 */
sealed class DrawerResult {
    data class Success(val action: String) : DrawerResult()
    data class AlreadyInState(val state: String) : DrawerResult()
    data class MenuItemSelected(val itemName: String) : DrawerResult()
    data class NotFound(val target: String) : DrawerResult()
    data class Error(val message: String) : DrawerResult()
    object NoAccessibility : DrawerResult()
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for drawer operations.
 *
 * Implementations should:
 * 1. Find DrawerLayout in accessibility tree
 * 2. Open/close drawer using gestures or accessibility actions
 * 3. Find and click menu items by name or index
 */
interface DrawerExecutor {

    /**
     * Open the navigation drawer.
     */
    suspend fun openDrawer(): DrawerResult

    /**
     * Close the navigation drawer.
     */
    suspend fun closeDrawer(): DrawerResult

    /**
     * Navigate to a menu item by search terms.
     * Opens drawer if needed, then clicks the matching menu item.
     *
     * @param searchTerms List of terms to search for (case-insensitive)
     */
    suspend fun navigateToMenuItem(searchTerms: List<String>): DrawerResult

    /**
     * Select a menu item by 1-based index.
     * Opens drawer if needed, then clicks the item at the specified index.
     *
     * @param index 1-based index of the menu item
     */
    suspend fun selectMenuItemByIndex(index: Int): DrawerResult

    /**
     * Get handler status.
     */
    suspend fun getStatus(): DrawerHandlerStatus
}
