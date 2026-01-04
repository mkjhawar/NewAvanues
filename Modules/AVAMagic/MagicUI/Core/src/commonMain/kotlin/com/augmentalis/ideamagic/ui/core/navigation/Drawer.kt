package com.augmentalis.magicui.ui.core.navigation

import com.augmentalis.magicui.components.core.*

/**
 * Drawer Component
 *
 * A navigation drawer that slides in from the side of the screen,
 * typically used for primary navigation.
 *
 * Features:
 * - Left or right positioning
 * - Optional header and footer
 * - Navigation items with icons and badges
 * - Open/closed state management
 *
 * Platform mappings:
 * - Android: NavigationView / NavigationDrawer
 * - iOS: Side menu (custom)
 * - macOS: Sidebar
 * - Web: Slide-out panel
 *
 * Usage:
 * ```kotlin
 * Drawer(
 *     isOpen = true,
 *     position = DrawerPosition.Left,
 *     header = HeaderComponent(),
 *     items = listOf(
 *         DrawerItem("home", "home", "Home"),
 *         DrawerItem("settings", "settings", "Settings")
 *     ),
 *     onItemClick = { id -> /* navigate */ },
 *     onDismiss = { /* close drawer */ }
 * )
 * ```
 */
data class DrawerComponent(
    val isOpen: Boolean = false,
    val position: DrawerPosition = DrawerPosition.Left,
    val header: Any? = null,
    val items: List<DrawerItem> = emptyList(),
    val footer: Any? = null,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList(),
    val onItemClick: ((String) -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
) {
}

/**
 * Position of the drawer on screen
 */
enum class DrawerPosition {
    Left, Right
}

/**
 * Navigation item in the drawer
 */
data class DrawerItem(
    val id: String,
    val icon: String? = null,
    val label: String,
    val badge: String? = null
)
