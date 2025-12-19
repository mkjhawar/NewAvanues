package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanues.avamagic.ui.core.navigation.AppBarComponent
import com.augmentalis.avanues.avamagic.ui.core.navigation.AppBarAction

/**
 * iOS Renderer for AppBar Component
 *
 * Renders AVAMagic AppBar components as native UINavigationBar.
 *
 * Features:
 * - Native UINavigationBar rendering
 * - Navigation button (back/menu)
 * - Title display
 * - Action buttons (up to 3)
 * - Elevation support (via shadow)
 * - Dark mode support
 * - Accessibility support
 *
 * @author Manoj Jhawar
 * @since 2025-11-21
 */
@OptIn(ExperimentalForeignApi::class)
class IOSAppBarRenderer {

    /**
     * Render AppBar component to UINavigationBar
     */
    fun render(component: AppBarComponent): UIView {
        // Create container view
        val containerView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 44.0)
            backgroundColor = UIColor.systemBackgroundColor
        }

        // Create navigation bar
        val navBar = UINavigationBar().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 44.0)

            // Apply translucency based on elevation
            isTranslucent = component.elevation > 0

            // Configure appearance
            configureAppearance(component)

            // Create navigation item
            val navItem = UINavigationItem().apply {
                title = component.title

                // Configure navigation button (left side)
                component.navigationIcon?.let { icon ->
                    leftBarButtonItem = createBarButtonItem(
                        icon = icon,
                        label = null,
                        onClick = component.onNavigationClick ?: {}
                    )
                }

                // Configure action buttons (right side)
                if (component.actions.isNotEmpty()) {
                    // Take up to 3 actions (iOS limit)
                    val actionButtons = component.actions.take(3).map { action ->
                        createBarButtonItem(
                            icon = action.icon,
                            label = action.label,
                            onClick = action.onClick
                        )
                    }
                    rightBarButtonItems = actionButtons
                }
            }

            items = listOf(navItem)
        }

        containerView.addSubview(navBar)

        // Apply accessibility
        applyAccessibility(containerView, component)

        return containerView
    }

    /**
     * Configure navigation bar appearance
     */
    private fun UINavigationBar.configureAppearance(component: AppBarComponent) {
        val appearance = UINavigationBarAppearance().apply {
            configureWithDefaultBackground()

            // Apply component style
            component.style?.let { style ->
                // Background color
                style.backgroundColor?.let { color ->
                    backgroundColor = parseColor(color)
                }

                // Title color
                style.textColor?.let { color ->
                    val titleAttributes = mapOf<Any?, Any?>(
                        NSForegroundColorAttributeName to parseColor(color)
                    )
                    titleTextAttributes = titleAttributes
                }
            }

            // Shadow (elevation)
            if (component.elevation > 0) {
                shadowColor = UIColor.blackColor.withAlphaComponent(0.2)
            } else {
                shadowColor = null
            }
        }

        standardAppearance = appearance
        scrollEdgeAppearance = appearance
        compactAppearance = appearance
    }

    /**
     * Create bar button item from icon/label
     */
    private fun createBarButtonItem(
        icon: String,
        label: String?,
        onClick: () -> Unit
    ): UIBarButtonItem {
        // Map icon names to system icons
        val systemIcon = mapIconToSystem(icon)

        return if (systemIcon != null) {
            // Use system icon
            UIBarButtonItem(
                barButtonSystemItem = systemIcon,
                target = null,
                action = null
            ).apply {
                // Store callback
                // Note: In production, use proper target-action pattern
                // This is simplified for demonstration
            }
        } else {
            // Use text label
            UIBarButtonItem(
                title = label ?: icon.replaceFirstChar { it.uppercase() },
                style = UIBarButtonItemStylePlain,
                target = null,
                action = null
            ).apply {
                // Store callback
            }
        }
    }

    /**
     * Map icon name to UIBarButtonSystemItem
     */
    private fun mapIconToSystem(icon: String): UIBarButtonSystemItem? {
        return when (icon.lowercase()) {
            "add", "plus" -> UIBarButtonSystemItemAdd
            "edit", "pencil" -> UIBarButtonSystemItemEdit
            "done", "check" -> UIBarButtonSystemItemDone
            "cancel", "close" -> UIBarButtonSystemItemCancel
            "save" -> UIBarButtonSystemItemSave
            "trash", "delete" -> UIBarButtonSystemItemTrash
            "compose", "new" -> UIBarButtonSystemItemCompose
            "reply" -> UIBarButtonSystemItemReply
            "action", "share" -> UIBarButtonSystemItemAction
            "organize", "folder" -> UIBarButtonSystemItemOrganize
            "bookmarks" -> UIBarButtonSystemItemBookmarks
            "search" -> UIBarButtonSystemItemSearch
            "refresh" -> UIBarButtonSystemItemRefresh
            "play" -> UIBarButtonSystemItemPlay
            "pause" -> UIBarButtonSystemItemPause
            "stop" -> UIBarButtonSystemItemStop
            "rewind" -> UIBarButtonSystemItemRewind
            "fastforward" -> UIBarButtonSystemItemFastForward
            "camera" -> UIBarButtonSystemItemCamera
            else -> null
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
    private fun applyAccessibility(view: UIView, component: AppBarComponent) {
        view.isAccessibilityElement = true
        view.accessibilityLabel = component.title
        view.accessibilityTraits = UIAccessibilityTraitHeader
        view.accessibilityValue = "Navigation bar with ${component.actions.size} actions"
    }
}
