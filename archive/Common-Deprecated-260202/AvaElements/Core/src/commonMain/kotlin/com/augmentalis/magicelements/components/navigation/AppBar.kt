package com.augmentalis.avaelements.components.navigation

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * AppBar Component
 *
 * A top app bar (also known as action bar or toolbar) that displays navigation,
 * title, and actions at the top of the screen.
 *
 * Features:
 * - Navigation icon (typically back button or menu)
 * - Title text
 * - Action buttons
 * - Customizable elevation
 *
 * Platform mappings:
 * - Android: MaterialToolbar / TopAppBar
 * - iOS: UINavigationBar
 * - macOS: NSToolbar
 * - Web: Header with flex layout
 *
 * Usage:
 * ```kotlin
 * AppBar(
 *     title = "My App",
 *     navigationIcon = "menu",
 *     actions = listOf(
 *         AppBarAction("search", "Search") { /* search */ },
 *         AppBarAction("settings", "Settings") { /* settings */ }
 *     ),
 *     elevation = 2
 * )
 * ```
 */
data class AppBarComponent(
    override val type: String = "AppBar",
    val title: String,
    val navigationIcon: String? = null,
    val actions: List<AppBarAction> = emptyList(),
    val elevation: Int = 1,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onNavigationClick: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Action button displayed in the app bar
 */
data class AppBarAction(
    val icon: String,
    val label: String? = null,
    val onClick: () -> Unit
)
