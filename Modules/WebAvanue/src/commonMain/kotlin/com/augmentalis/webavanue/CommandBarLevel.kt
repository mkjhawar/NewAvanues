package com.augmentalis.webavanue

/**
 * Command bar menu levels - FLAT HIERARCHY (Max 2 Levels)
 *
 * Design Principles:
 * - Max 2 levels deep (MAIN → sub-level)
 * - Max 6 buttons per level (no scrolling needed)
 * - Context-based grouping
 * - Single Close button to return (no redundant Back/Home)
 *
 * Hierarchy:
 * MAIN → SCROLL | ZOOM | PAGE | MENU
 */
enum class CommandBarLevel {
    MAIN,    // Primary: Back, Home, Add, Scroll, Page, Menu
    SCROLL,  // Scroll: Close, Up, Down, Top, Bottom, Freeze
    ZOOM,    // Zoom: Close, In, Out, 50%, 100%, 150%
    PAGE,    // Page: Close, Prev, Next, Reload, Desktop, Fav
    MENU     // Menu: Close, Bookmarks, Downloads, History, Settings
}
