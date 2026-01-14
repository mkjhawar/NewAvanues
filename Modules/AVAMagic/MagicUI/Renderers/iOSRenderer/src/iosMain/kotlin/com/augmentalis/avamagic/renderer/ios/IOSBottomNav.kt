package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.magicui.ui.core.navigation.BottomNavComponent
import com.augmentalis.magicui.ui.core.navigation.BottomNavItem

/**
 * iOS Renderer for BottomNav Component
 *
 * Renders AVAMagic BottomNav components as native UITabBar.
 *
 * Features:
 * - Native UITabBar rendering
 * - Icon-based navigation items
 * - Text labels
 * - Badge notifications
 * - Selection state tracking
 * - Dark mode support
 * - Accessibility support
 *
 * @author Manoj Jhawar
 * @since 2025-11-21
 */
@OptIn(ExperimentalForeignApi::class)
class IOSBottomNavRenderer {

    /**
     * Render BottomNav component to UITabBar
     */
    fun render(component: BottomNavComponent): UITabBar {
        return UITabBar().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 49.0)

            // Create tab bar items
            val tabBarItems = component.items.mapIndexed { index, item ->
                createTabBarItem(item, index)
            }

            items = tabBarItems

            // Set selected item
            selectedItem = tabBarItems.getOrNull(component.selectedIndex)

            // Apply component style
            applyStyle(component)

            // Configure appearance
            configureAppearance(component)

            // Apply accessibility
            applyAccessibility(this, component)

            // Handle selection change
            // Note: In production, use delegate pattern
            // This is simplified for demonstration
            component.onItemSelected?.let { callback ->
                // Delegate would call: callback(newSelectedIndex)
            }
        }
    }

    /**
     * Create UITabBarItem from BottomNavItem
     */
    private fun createTabBarItem(item: BottomNavItem, index: Int): UITabBarItem {
        return UITabBarItem().apply {
            title = item.label

            // Map icon name to system icon
            val systemImage = mapIconToSystemImage(item.icon)
            if (systemImage != null) {
                image = UIImage.systemImageNamed(systemImage)
                selectedImage = UIImage.systemImageNamed(systemImage)
            }

            // Set badge
            item.badge?.let { badgeText ->
                badgeValue = badgeText
            }

            // Set tag for identification
            tag = index.toLong()

            // Accessibility
            isAccessibilityElement = true
            accessibilityLabel = item.label
            accessibilityHint = item.badge?.let { "Has $it notifications" }
        }
    }

    /**
     * Apply component style to tab bar
     */
    private fun UITabBar.applyStyle(component: BottomNavComponent) {
        component.style?.let { style ->
            // Background color
            style.backgroundColor?.let { color ->
                backgroundColor = parseColor(color)
                barTintColor = parseColor(color)
            }

            // Tint color (selected item color)
            style.textColor?.let { color ->
                tintColor = parseColor(color)
            }

            // Unselected item color
            unselectedItemTintColor = UIColor.systemGrayColor
        }
    }

    /**
     * Configure tab bar appearance
     */
    private fun UITabBar.configureAppearance(component: BottomNavComponent) {
        val appearance = UITabBarAppearance().apply {
            configureWithDefaultBackground()

            // Apply component style to appearance
            component.style?.let { style ->
                style.backgroundColor?.let { color ->
                    backgroundColor = parseColor(color)
                }
            }

            // Stack layout (icon above text)
            stackedLayoutAppearance.normal.iconColor = UIColor.systemGrayColor
            stackedLayoutAppearance.selected.iconColor = tintColor
        }

        standardAppearance = appearance
        scrollEdgeAppearance = appearance
    }

    /**
     * Map icon name to SF Symbols system image name
     */
    private fun mapIconToSystemImage(icon: String): String? {
        return when (icon.lowercase()) {
            "home" -> "house.fill"
            "search" -> "magnifyingglass"
            "profile", "person" -> "person.fill"
            "settings" -> "gearshape.fill"
            "favorites", "heart" -> "heart.fill"
            "notifications", "bell" -> "bell.fill"
            "messages", "chat" -> "message.fill"
            "calendar" -> "calendar"
            "camera" -> "camera.fill"
            "library", "photo" -> "photo.fill"
            "video" -> "video.fill"
            "music" -> "music.note"
            "map" -> "map.fill"
            "bookmark" -> "bookmark.fill"
            "shopping", "cart" -> "cart.fill"
            "more" -> "ellipsis.circle.fill"
            else -> {
                // Try to use the icon name as-is
                // SF Symbols naming convention
                icon.lowercase().replace("_", ".")
            }
        }
    }

    /**
     * Parse hex color string to UIColor
     */
    private fun parseColor(hex: String): UIColor {
        val cleanHex = hex.removePrefix("#")
        val rgb = cleanHex.toLongOrNull(16) ?: 0x000000

        val red = ((rgb shr 16) and 0xFF) / 255.0
        val green = ((rgb shr 8) and 0xFF) / 255.0
        val blue = (rgb and 0xFF) / 255.0

        return UIColor(red = red, green = green, blue = blue, alpha = 1.0)
    }

    /**
     * Apply accessibility features
     */
    private fun applyAccessibility(tabBar: UITabBar, component: BottomNavComponent) {
        tabBar.isAccessibilityElement = false // Container is not accessible, items are
        tabBar.accessibilityLabel = "Navigation bar with ${component.items.size} items"
    }
}
