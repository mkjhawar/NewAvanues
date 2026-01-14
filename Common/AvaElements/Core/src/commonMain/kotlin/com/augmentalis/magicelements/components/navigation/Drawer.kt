package com.augmentalis.avaelements.components.navigation

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

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
    override val type: String = "Drawer",
    val isOpen: Boolean = false,
    val position: DrawerPosition = DrawerPosition.Left,
    val header: Component? = null,
    val items: List<DrawerItem> = emptyList(),
    val footer: Component? = null,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onItemClick: ((String) -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
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
