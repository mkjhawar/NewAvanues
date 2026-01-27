/**
 * DrawerHandler.kt
 *
 * Created: 2026-01-27 23:00 PST
 * Last Modified: 2026-01-27 23:00 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Voice command handler for navigation drawer operations
 * Features: Open/close drawer, menu item navigation, quick access shortcuts
 * Location: CommandManager module (handlers package)
 *
 * Changelog:
 * - v1.0.0 (2026-01-27): Initial implementation for navigation drawer voice control
 */

package com.augmentalis.commandmanager.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandHandler
import com.augmentalis.commandmanager.CommandRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * Voice command handler for VoiceOS navigation drawer
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
 * Design:
 * - Command parsing and routing only (no UI logic)
 * - Uses AccessibilityService for drawer manipulation
 * - Implements CommandHandler for CommandRegistry integration
 * - Thread-safe singleton pattern
 */
class DrawerHandler private constructor(
    context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "DrawerHandler"

        @Volatile
        private var instance: DrawerHandler? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): DrawerHandler {
            return instance ?: synchronized(this) {
                instance ?: DrawerHandler(context.applicationContext).also {
                    instance = it
                }
            }
        }

        // Common drawer-related class names
        private val DRAWER_CLASS_NAMES = setOf(
            "androidx.drawerlayout.widget.DrawerLayout",
            "android.support.v4.widget.DrawerLayout",
            "com.google.android.material.navigation.NavigationView",
            "androidx.navigation.ui.NavigationUI"
        )

        // Common navigation view class names
        private val NAVIGATION_VIEW_CLASS_NAMES = setOf(
            "com.google.android.material.navigation.NavigationView",
            "android.support.design.widget.NavigationView",
            "androidx.recyclerview.widget.RecyclerView",
            "android.widget.ListView"
        )

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
    }

    // CommandHandler interface implementation
    override val moduleId: String = "drawer"

    override val supportedCommands: List<String> = listOf(
        // Open drawer commands
        "open drawer",
        "show menu",
        "show drawer",
        "open menu",
        "open navigation",
        "show navigation",
        "menu",

        // Close drawer commands
        "close drawer",
        "hide menu",
        "hide drawer",
        "close menu",
        "close navigation",
        "hide navigation",
        "dismiss menu",
        "dismiss drawer",

        // Navigation commands
        "go to [menu item]",
        "navigate to [menu item]",
        "open [menu item]",
        "select [menu item]",

        // Quick access shortcuts
        "home",
        "main screen",
        "settings",
        "profile",
        "help",
        "about",
        "logout",

        // Index-based navigation
        "menu item [N]",
        "item [N]",
        "select item [N]",
        "option [N]"
    )

    private val contextRef: WeakReference<Context> = WeakReference(context)
    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var accessibilityServiceRef: WeakReference<AccessibilityService>? = null

    @Volatile
    private var isInitialized = false

    init {
        initialize()
        // Register with CommandRegistry automatically
        CommandRegistry.registerHandler(moduleId, this)
    }

    /**
     * Initialize drawer handler
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return try {
            isInitialized = true
            Log.d(TAG, "DrawerHandler initialized with ${supportedCommands.size} supported commands")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }

    /**
     * Set accessibility service reference for drawer operations
     */
    fun setAccessibilityService(service: AccessibilityService?) {
        accessibilityServiceRef = service?.let { WeakReference(it) }
        Log.d(TAG, "Accessibility service ${if (service != null) "set" else "cleared"}")
    }

    /**
     * CommandHandler interface: Check if this handler can process the command
     */
    override fun canHandle(command: String): Boolean {
        return when {
            // Drawer open commands
            isOpenDrawerCommand(command) -> true
            // Drawer close commands
            isCloseDrawerCommand(command) -> true
            // Navigation commands with "go to", "navigate to", etc.
            isNavigationCommand(command) -> true
            // Quick access shortcuts
            isQuickAccessCommand(command) -> true
            // Index-based menu item selection
            isMenuItemIndexCommand(command) -> true
            else -> false
        }
    }

    /**
     * CommandHandler interface: Execute the command
     */
    override suspend fun handleCommand(command: String): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized for command processing")
            return false
        }

        Log.d(TAG, "Processing drawer command: '$command'")

        return try {
            when {
                isOpenDrawerCommand(command) -> openDrawer()
                isCloseDrawerCommand(command) -> closeDrawer()
                isQuickAccessCommand(command) -> handleQuickAccess(command)
                isMenuItemIndexCommand(command) -> handleMenuItemIndex(command)
                isNavigationCommand(command) -> handleNavigation(command)
                else -> {
                    Log.w(TAG, "Unhandled command: $command")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $command", e)
            false
        }
    }

    // ========== Command Type Detection ==========

    private fun isOpenDrawerCommand(command: String): Boolean {
        return command in setOf(
            "open drawer", "show menu", "show drawer", "open menu",
            "open navigation", "show navigation", "menu"
        )
    }

    private fun isCloseDrawerCommand(command: String): Boolean {
        return command in setOf(
            "close drawer", "hide menu", "hide drawer", "close menu",
            "close navigation", "hide navigation", "dismiss menu", "dismiss drawer"
        )
    }

    private fun isNavigationCommand(command: String): Boolean {
        return command.startsWith("go to ") ||
                command.startsWith("navigate to ") ||
                command.startsWith("open ") ||
                command.startsWith("select ")
    }

    private fun isQuickAccessCommand(command: String): Boolean {
        return QUICK_ACCESS_ITEMS.keys.any { key ->
            command == key || QUICK_ACCESS_ITEMS[key]?.contains(command) == true
        }
    }

    private fun isMenuItemIndexCommand(command: String): Boolean {
        return command.startsWith("menu item ") ||
                command.startsWith("item ") ||
                command.startsWith("select item ") ||
                command.startsWith("option ") ||
                command.matches(Regex("^(menu\\s+)?item\\s+(\\d+|one|two|three|four|five|six|seven|eight|nine|ten|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)$"))
    }

    // ========== Drawer Operations ==========

    /**
     * Open the navigation drawer
     */
    private suspend fun openDrawer(): Boolean = withContext(Dispatchers.Main) {
        val service = accessibilityServiceRef?.get()
        if (service == null) {
            Log.e(TAG, "AccessibilityService not available")
            return@withContext false
        }

        val rootNode = service.rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "Root window not available")
            return@withContext false
        }

        try {
            // Find DrawerLayout
            val drawerLayout = findDrawerLayout(rootNode)
            if (drawerLayout != null) {
                // Try to expand/open the drawer
                val result = performDrawerAction(drawerLayout, open = true)
                if (result) {
                    Log.i(TAG, "Drawer opened successfully")
                    return@withContext true
                }
            }

            // Fallback: Look for hamburger menu button and click it
            val menuButton = findMenuButton(rootNode)
            if (menuButton != null && menuButton.isClickable) {
                val clicked = menuButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    Log.i(TAG, "Opened drawer via menu button")
                    return@withContext true
                }
            }

            // Last resort: Swipe from left edge
            Log.d(TAG, "Attempting swipe gesture to open drawer")
            return@withContext performSwipeToOpenDrawer(service)

        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Close the navigation drawer
     */
    private suspend fun closeDrawer(): Boolean = withContext(Dispatchers.Main) {
        val service = accessibilityServiceRef?.get()
        if (service == null) {
            Log.e(TAG, "AccessibilityService not available")
            return@withContext false
        }

        val rootNode = service.rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "Root window not available")
            return@withContext false
        }

        try {
            // Find DrawerLayout
            val drawerLayout = findDrawerLayout(rootNode)
            if (drawerLayout != null) {
                // Try to collapse/close the drawer
                val result = performDrawerAction(drawerLayout, open = false)
                if (result) {
                    Log.i(TAG, "Drawer closed successfully")
                    return@withContext true
                }
            }

            // Fallback: Perform back action to close drawer
            val backResult = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            if (backResult) {
                Log.i(TAG, "Closed drawer via back action")
                return@withContext true
            }

            // Fallback: Swipe to close
            return@withContext performSwipeToCloseDrawer(service)

        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Handle quick access shortcuts (home, settings, profile, etc.)
     */
    private suspend fun handleQuickAccess(command: String): Boolean = withContext(Dispatchers.Main) {
        // First, ensure drawer is open
        val drawerOpened = openDrawerIfNeeded()
        if (!drawerOpened) {
            Log.w(TAG, "Could not open drawer for quick access: $command")
        }

        // Find which quick access category matches
        val targetCategory = QUICK_ACCESS_ITEMS.entries.find { (key, aliases) ->
            command == key || aliases.contains(command)
        }?.key

        if (targetCategory == null) {
            Log.w(TAG, "No quick access match for: $command")
            return@withContext false
        }

        // Search for menu item matching the category
        val searchTerms = QUICK_ACCESS_ITEMS[targetCategory] ?: listOf(targetCategory)
        return@withContext navigateToMenuItem(searchTerms)
    }

    /**
     * Handle navigation commands like "go to [item]"
     */
    private suspend fun handleNavigation(command: String): Boolean = withContext(Dispatchers.Main) {
        // Extract target from command
        val target = extractNavigationTarget(command)
        if (target.isBlank()) {
            Log.w(TAG, "No navigation target extracted from: $command")
            return@withContext false
        }

        // First, ensure drawer is open
        val drawerOpened = openDrawerIfNeeded()
        if (!drawerOpened) {
            Log.w(TAG, "Could not open drawer for navigation: $command")
        }

        // Navigate to the specified item
        return@withContext navigateToMenuItem(listOf(target))
    }

    /**
     * Handle index-based menu item selection
     */
    private suspend fun handleMenuItemIndex(command: String): Boolean = withContext(Dispatchers.Main) {
        val index = extractMenuIndex(command)
        if (index < 1) {
            Log.w(TAG, "Invalid menu index in command: $command")
            return@withContext false
        }

        // First, ensure drawer is open
        val drawerOpened = openDrawerIfNeeded()
        if (!drawerOpened) {
            Log.w(TAG, "Could not open drawer for menu item selection")
        }

        // Select menu item by index
        return@withContext selectMenuItemByIndex(index)
    }

    // ========== Helper Methods ==========

    /**
     * Extract navigation target from command
     */
    private fun extractNavigationTarget(command: String): String {
        val prefixes = listOf("go to ", "navigate to ", "open ", "select ")
        for (prefix in prefixes) {
            if (command.startsWith(prefix)) {
                return command.removePrefix(prefix).trim()
            }
        }
        return ""
    }

    /**
     * Extract menu index from command
     */
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

    /**
     * Open drawer if not already open
     */
    private suspend fun openDrawerIfNeeded(): Boolean {
        val service = accessibilityServiceRef?.get() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        try {
            // Check if drawer is already visible
            val navigationView = findNavigationView(rootNode)
            if (navigationView != null && navigationView.isVisibleToUser) {
                Log.d(TAG, "Drawer already open")
                return true
            }

            // Open the drawer
            return openDrawer()
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Navigate to menu item by search terms
     */
    private suspend fun navigateToMenuItem(searchTerms: List<String>): Boolean {
        val service = accessibilityServiceRef?.get() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        try {
            // Find navigation view
            val navigationView = findNavigationView(rootNode)
            if (navigationView == null) {
                Log.w(TAG, "Navigation view not found")
                return false
            }

            // Find menu item by text
            for (term in searchTerms) {
                val menuItem = findMenuItemByText(navigationView, term)
                if (menuItem != null) {
                    val clicked = clickMenuItem(menuItem)
                    if (clicked) {
                        Log.i(TAG, "Navigated to menu item: $term")
                        return true
                    }
                }
            }

            Log.w(TAG, "Menu item not found for terms: $searchTerms")
            return false

        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Select menu item by 1-based index
     */
    private suspend fun selectMenuItemByIndex(index: Int): Boolean {
        val service = accessibilityServiceRef?.get() ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        try {
            // Find navigation view
            val navigationView = findNavigationView(rootNode)
            if (navigationView == null) {
                Log.w(TAG, "Navigation view not found")
                return false
            }

            // Collect clickable menu items
            val menuItems = collectMenuItems(navigationView)

            if (index < 1 || index > menuItems.size) {
                Log.w(TAG, "Menu index $index out of range (1-${menuItems.size})")
                return false
            }

            // Click the item (convert to 0-based index)
            val targetItem = menuItems[index - 1]
            val clicked = clickMenuItem(targetItem)

            if (clicked) {
                Log.i(TAG, "Selected menu item $index")
                return true
            }

            return false

        } finally {
            rootNode.recycle()
        }
    }

    // ========== Accessibility Node Operations ==========

    /**
     * Find DrawerLayout in the UI tree
     */
    private fun findDrawerLayout(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeByClassName(rootNode, DRAWER_CLASS_NAMES)
    }

    /**
     * Find NavigationView in the UI tree
     */
    private fun findNavigationView(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findNodeByClassName(rootNode, NAVIGATION_VIEW_CLASS_NAMES)
    }

    /**
     * Find hamburger menu button
     */
    private fun findMenuButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for common menu button identifiers
        val buttonIdentifiers = listOf(
            "Open navigation drawer",
            "Open drawer",
            "Navigation",
            "Menu",
            "Toggle navigation",
            "hamburger"
        )

        for (identifier in buttonIdentifiers) {
            val node = findNodeByContentDescription(rootNode, identifier)
            if (node != null) return node
        }

        // Also look for ImageButton at typical hamburger menu location
        return findNodeByClassName(rootNode, setOf("android.widget.ImageButton"))?.takeIf { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            desc.contains("navigation") || desc.contains("menu") || desc.contains("drawer")
        }
    }

    /**
     * Find node by class name
     */
    private fun findNodeByClassName(
        rootNode: AccessibilityNodeInfo,
        classNames: Set<String>
    ): AccessibilityNodeInfo? {
        val className = rootNode.className?.toString()
        if (className != null && classNames.contains(className)) {
            return rootNode
        }

        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val found = findNodeByClassName(child, classNames)
            if (found != null) return found
            if (found != child) child.recycle()
        }

        return null
    }

    /**
     * Find node by content description
     */
    private fun findNodeByContentDescription(
        rootNode: AccessibilityNodeInfo,
        description: String
    ): AccessibilityNodeInfo? {
        val nodeDesc = rootNode.contentDescription?.toString()?.lowercase() ?: ""
        val targetDesc = description.lowercase()

        if (nodeDesc.contains(targetDesc)) {
            return rootNode
        }

        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val found = findNodeByContentDescription(child, description)
            if (found != null) return found
            if (found != child) child.recycle()
        }

        return null
    }

    /**
     * Find menu item by text (case-insensitive)
     */
    private fun findMenuItemByText(
        rootNode: AccessibilityNodeInfo,
        text: String
    ): AccessibilityNodeInfo? {
        val targetText = text.lowercase()

        // Check current node's text and content description
        val nodeText = rootNode.text?.toString()?.lowercase() ?: ""
        val nodeDesc = rootNode.contentDescription?.toString()?.lowercase() ?: ""

        if (nodeText.contains(targetText) || nodeDesc.contains(targetText)) {
            // Return this node if clickable, otherwise find clickable parent
            if (rootNode.isClickable) {
                return rootNode
            }
            // Look for clickable parent
            var parent = rootNode.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent
                }
                val grandParent = parent.parent
                if (grandParent != parent) parent.recycle()
                parent = grandParent
            }
        }

        // Search children
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val found = findMenuItemByText(child, text)
            if (found != null) return found
            if (found != child) child.recycle()
        }

        return null
    }

    /**
     * Collect all clickable menu items from navigation view
     */
    private fun collectMenuItems(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val items = mutableListOf<AccessibilityNodeInfo>()
        collectClickableItems(rootNode, items)
        return items
    }

    private fun collectClickableItems(
        node: AccessibilityNodeInfo,
        items: MutableList<AccessibilityNodeInfo>
    ) {
        // Only add items that have text or content description (actual menu items)
        if (node.isClickable) {
            val hasContent = !node.text.isNullOrBlank() ||
                           !node.contentDescription.isNullOrBlank()
            if (hasContent) {
                items.add(node)
                return // Don't recurse into clickable items
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectClickableItems(child, items)
        }
    }

    /**
     * Click a menu item node
     */
    private fun clickMenuItem(node: AccessibilityNodeInfo): Boolean {
        // Try direct click
        if (node.isClickable) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (result) return true
        }

        // Try ACTION_SELECT
        val selectResult = node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
        if (selectResult) return true

        // Try clicking parent if this node isn't clickable
        val parent = node.parent
        if (parent != null && parent.isClickable) {
            val parentResult = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            parent.recycle()
            return parentResult
        }

        return false
    }

    /**
     * Perform drawer action (open/close)
     */
    private fun performDrawerAction(
        drawerLayout: AccessibilityNodeInfo,
        open: Boolean
    ): Boolean {
        val action = if (open) {
            AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND
        } else {
            AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE
        }

        // Check if action is available
        if (drawerLayout.actionList?.contains(action) == true) {
            return drawerLayout.performAction(action.id)
        }

        // Try scroll action as alternative
        val scrollAction = if (open) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }

        return drawerLayout.performAction(scrollAction)
    }

    /**
     * Perform swipe gesture to open drawer (left edge to center)
     */
    private fun performSwipeToOpenDrawer(service: AccessibilityService): Boolean {
        return try {
            val displayMetrics = service.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            val startX = 0f // Left edge
            val startY = screenHeight / 2f
            val endX = screenWidth * 0.7f
            val endY = screenHeight / 2f

            performSwipeGesture(service, startX, startY, endX, endY, 300L)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing swipe to open drawer", e)
            false
        }
    }

    /**
     * Perform swipe gesture to close drawer (center to left)
     */
    private fun performSwipeToCloseDrawer(service: AccessibilityService): Boolean {
        return try {
            val displayMetrics = service.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            val startX = screenWidth * 0.7f
            val startY = screenHeight / 2f
            val endX = 0f
            val endY = screenHeight / 2f

            performSwipeGesture(service, startX, startY, endX, endY, 300L)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing swipe to close drawer", e)
            false
        }
    }

    /**
     * Perform swipe gesture using AccessibilityService gesture API
     */
    private fun performSwipeGesture(
        service: AccessibilityService,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long
    ): Boolean {
        return try {
            // Use GestureDescription API (requires API 24+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val path = android.graphics.Path()
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)

                val gesture = android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(
                        android.accessibilityservice.GestureDescription.StrokeDescription(
                            path, 0, durationMs
                        )
                    )
                    .build()

                service.dispatchGesture(gesture, null, null)
            } else {
                Log.w(TAG, "Gesture API not available on API < 24")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing swipe gesture", e)
            false
        }
    }

    /**
     * Get handler status
     */
    fun getStatus(): DrawerHandlerStatus {
        return DrawerHandlerStatus(
            isInitialized = isInitialized,
            hasAccessibilityService = accessibilityServiceRef?.get() != null,
            supportedCommandCount = supportedCommands.size
        )
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        // Unregister from CommandRegistry
        CommandRegistry.unregisterHandler(moduleId)
        accessibilityServiceRef = null
        handlerScope.cancel()
        instance = null
        Log.d(TAG, "DrawerHandler disposed")
    }
}

/**
 * Status data class for DrawerHandler
 */
data class DrawerHandlerStatus(
    val isInitialized: Boolean,
    val hasAccessibilityService: Boolean,
    val supportedCommandCount: Int
)
