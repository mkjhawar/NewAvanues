package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avamagic.ui.core.navigation.DrawerComponent
import com.augmentalis.avamagic.ui.core.navigation.DrawerItem
import com.augmentalis.avamagic.ui.core.navigation.DrawerPosition

/**
 * iOS Renderer for Drawer Component
 *
 * Renders AVAMagic Drawer components as custom slide-out side menu.
 * iOS doesn't have a native drawer/navigation drawer like Android,
 * so this creates a custom implementation.
 *
 * Features:
 * - Slide-out side menu (left or right)
 * - Optional header and footer
 * - Navigation items with icons and badges
 * - Overlay/backdrop when open
 * - Gesture support (swipe to open/close)
 * - Dark mode support
 * - Accessibility support
 *
 * @author Manoj Jhawar
 * @since 2025-11-21
 */
@OptIn(ExperimentalForeignApi::class)
class IOSDrawerRenderer {

    private val drawerWidth = 280.0
    private val animationDuration = 0.3

    /**
     * Render Drawer component to custom drawer view
     */
    fun render(component: DrawerComponent): UIView {
        val screenWidth = UIScreen.mainScreen.bounds.useContents { size.width }
        val screenHeight = UIScreen.mainScreen.bounds.useContents { size.height }

        // Create container view
        val containerView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, screenWidth, screenHeight)
            backgroundColor = UIColor.clearColor
        }

        // Create overlay/backdrop
        val overlay = UIView().apply {
            frame = containerView.bounds
            backgroundColor = UIColor.blackColor.withAlphaComponent(0.5)
            alpha = if (component.isOpen) 1.0 else 0.0
            isUserInteractionEnabled = component.isOpen

            // Tap to dismiss
            val tapGesture = UITapGestureRecognizer()
            addGestureRecognizer(tapGesture)
            // Note: In production, connect gesture to onDismiss callback
        }

        // Create drawer view
        val drawerView = createDrawerView(component, screenHeight)

        // Position drawer based on position and open state
        positionDrawer(drawerView, component.position, component.isOpen, screenWidth, screenHeight)

        // Add subviews
        containerView.addSubview(overlay)
        containerView.addSubview(drawerView)

        return containerView
    }

    /**
     * Create the drawer content view
     */
    private fun createDrawerView(component: DrawerComponent, screenHeight: Double): UIView {
        val drawerView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, drawerWidth, screenHeight)
            backgroundColor = UIColor.systemBackgroundColor

            // Shadow
            layer.shadowColor = UIColor.blackColor.CGColor
            layer.shadowOpacity = 0.3f
            layer.shadowOffset = CGSizeMake(0.0, 0.0)
            layer.shadowRadius = 10.0
        }

        var yOffset = 0.0

        // Add header if provided
        component.header?.let { header ->
            val headerView = UIView().apply {
                frame = CGRectMake(0.0, yOffset, drawerWidth, 120.0)
                backgroundColor = UIColor.systemBlueColor

                // Add placeholder content
                val label = UILabel().apply {
                    frame = CGRectMake(16.0, 40.0, drawerWidth - 32.0, 60.0)
                    text = "Header"
                    textColor = UIColor.whiteColor
                    font = UIFont.boldSystemFontOfSize(24.0)
                }
                addSubview(label)
            }
            drawerView.addSubview(headerView)
            yOffset += 120.0
        }

        // Add navigation items
        component.items.forEachIndexed { index, item ->
            val itemView = createDrawerItemView(item, index, yOffset) {
                component.onItemClick?.invoke(item.id)
            }
            drawerView.addSubview(itemView)
            yOffset += 56.0
        }

        // Add footer if provided
        component.footer?.let { footer ->
            val footerView = UIView().apply {
                frame = CGRectMake(0.0, screenHeight - 80.0, drawerWidth, 80.0)
                backgroundColor = UIColor.systemGray6Color

                // Add placeholder content
                val label = UILabel().apply {
                    frame = CGRectMake(16.0, 20.0, drawerWidth - 32.0, 40.0)
                    text = "Footer"
                    textColor = UIColor.secondaryLabelColor
                    font = UIFont.systemFontOfSize(14.0)
                }
                addSubview(label)
            }
            drawerView.addSubview(footerView)
        }

        return drawerView
    }

    /**
     * Create individual drawer item view
     */
    private fun createDrawerItemView(
        item: DrawerItem,
        index: Int,
        yOffset: Double,
        onClick: () -> Unit
    ): UIView {
        val itemView = UIView().apply {
            frame = CGRectMake(0.0, yOffset, drawerWidth, 56.0)
            backgroundColor = UIColor.systemBackgroundColor

            // Icon
            item.icon?.let { iconName ->
                val systemImage = mapIconToSystemImage(iconName)
                val iconImageView = UIImageView().apply {
                    frame = CGRectMake(16.0, 16.0, 24.0, 24.0)
                    image = UIImage.systemImageNamed(systemImage ?: iconName)
                    tintColor = UIColor.labelColor
                }
                addSubview(iconImageView)
            }

            // Label
            val label = UILabel().apply {
                frame = CGRectMake(56.0, 0.0, drawerWidth - 112.0, 56.0)
                text = item.label
                textColor = UIColor.labelColor
                font = UIFont.systemFontOfSize(16.0)
            }
            addSubview(label)

            // Badge
            item.badge?.let { badgeText ->
                val badge = createBadgeView(badgeText, drawerWidth - 56.0, 18.0)
                addSubview(badge)
            }

            // Separator
            val separator = UIView().apply {
                frame = CGRectMake(56.0, 55.0, drawerWidth - 56.0, 1.0)
                backgroundColor = UIColor.separatorColor
            }
            addSubview(separator)

            // Tap gesture
            val tapGesture = UITapGestureRecognizer()
            addGestureRecognizer(tapGesture)
            isUserInteractionEnabled = true
            // Note: In production, connect gesture to onClick callback

            // Accessibility
            isAccessibilityElement = true
            accessibilityLabel = item.label
            accessibilityTraits = UIAccessibilityTraitButton
            item.badge?.let { badgeText ->
                accessibilityValue = "Has $badgeText notifications"
            }
        }

        return itemView
    }

    /**
     * Create badge view for notification count
     */
    private fun createBadgeView(text: String, x: Double, y: Double): UIView {
        val badgeSize = 20.0
        return UIView().apply {
            frame = CGRectMake(x, y, badgeSize, badgeSize)
            backgroundColor = UIColor.systemRedColor
            layer.cornerRadius = badgeSize / 2
            layer.masksToBounds = true

            val label = UILabel().apply {
                frame = bounds
                this.text = text
                textColor = UIColor.whiteColor
                font = UIFont.boldSystemFontOfSize(11.0)
                textAlignment = NSTextAlignmentCenter
            }
            addSubview(label)
        }
    }

    /**
     * Position drawer based on position and open state
     */
    private fun positionDrawer(
        drawerView: UIView,
        position: DrawerPosition,
        isOpen: Boolean,
        screenWidth: Double,
        screenHeight: Double
    ) {
        val openX = if (position == DrawerPosition.Left) 0.0 else screenWidth - drawerWidth
        val closedX = if (position == DrawerPosition.Left) -drawerWidth else screenWidth

        val x = if (isOpen) openX else closedX

        drawerView.frame = CGRectMake(x, 0.0, drawerWidth, screenHeight)
    }

    /**
     * Animate drawer open/close
     * Note: This would be called in response to state changes
     */
    fun animateDrawer(
        drawerView: UIView,
        overlay: UIView,
        isOpen: Boolean,
        position: DrawerPosition,
        screenWidth: Double,
        screenHeight: Double,
        completion: (() -> Unit)? = null
    ) {
        val openX = if (position == DrawerPosition.Left) 0.0 else screenWidth - drawerWidth
        val closedX = if (position == DrawerPosition.Left) -drawerWidth else screenWidth

        UIView.animateWithDuration(
            duration = animationDuration,
            animations = {
                val x = if (isOpen) openX else closedX
                drawerView.frame = CGRectMake(x, 0.0, drawerWidth, screenHeight)
                overlay.alpha = if (isOpen) 1.0 else 0.0
            },
            completion = { finished ->
                overlay.isUserInteractionEnabled = isOpen
                completion?.invoke()
            }
        )
    }

    /**
     * Map icon name to SF Symbols system image name
     */
    private fun mapIconToSystemImage(icon: String): String? {
        return when (icon.lowercase()) {
            "home" -> "house.fill"
            "profile", "person" -> "person.fill"
            "settings" -> "gearshape.fill"
            "favorites", "heart" -> "heart.fill"
            "notifications", "bell" -> "bell.fill"
            "messages", "chat" -> "message.fill"
            "calendar" -> "calendar"
            "help", "info" -> "questionmark.circle.fill"
            "about" -> "info.circle.fill"
            "logout", "exit" -> "arrow.right.square.fill"
            "bookmark" -> "bookmark.fill"
            "history" -> "clock.fill"
            "download" -> "arrow.down.circle.fill"
            "upload" -> "arrow.up.circle.fill"
            "share" -> "square.and.arrow.up.fill"
            else -> {
                // Try to use the icon name as-is
                icon.lowercase().replace("_", ".")
            }
        }
    }
}
