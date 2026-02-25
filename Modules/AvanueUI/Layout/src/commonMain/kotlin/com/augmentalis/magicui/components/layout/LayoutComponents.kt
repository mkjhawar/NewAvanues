package com.augmentalis.avanueui.layout

import kotlinx.serialization.Serializable

/**
 * MagicUI Layout Components
 *
 * 5 layout components for advanced layouts
 */

/**
 * Grid component for grid layout
 */
@Serializable
data class Grid(
    val id: String,
    val columns: Int = 2,
    val spacing: Float = 8f,
    val childrenIds: List<String> = emptyList()
)

/**
 * Stack component for layered layouts
 */
@Serializable
data class Stack(
    val id: String,
    val alignment: StackAlignment = StackAlignment.Center,
    val childrenIds: List<String> = emptyList()
)

/**
 * Spacer component for fixed spacing
 */
@Serializable
data class Spacer(
    val id: String,
    val width: Float? = null,
    val height: Float? = null
)

/**
 * Drawer component for side panel
 */
@Serializable
data class Drawer(
    val id: String,
    val open: Boolean = false,
    val anchor: DrawerAnchor = DrawerAnchor.Start,
    val contentId: String? = null,
    val mainContentId: String? = null
)

/**
 * Tabs component for tab navigation
 */
@Serializable
data class Tabs(
    val id: String,
    val tabs: List<Tab>,
    val selectedIndex: Int = 0,
    val variant: TabVariant = TabVariant.Standard
)

// Supporting enums and data classes

@Serializable
enum class StackAlignment {
    TopStart,
    TopCenter,
    TopEnd,
    CenterStart,
    Center,
    CenterEnd,
    BottomStart,
    BottomCenter,
    BottomEnd
}

@Serializable
enum class DrawerAnchor {
    Start,
    End,
    Top,
    Bottom
}

@Serializable
data class Tab(
    val id: String,
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val badge: String? = null
)

@Serializable
enum class TabVariant {
    Standard,
    Scrollable,
    Fixed
}
