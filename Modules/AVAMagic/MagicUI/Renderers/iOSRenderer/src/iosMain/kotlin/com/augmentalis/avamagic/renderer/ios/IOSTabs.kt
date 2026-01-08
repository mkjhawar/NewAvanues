package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.magicui.ui.core.navigation.TabsComponent
import com.augmentalis.magicui.ui.core.navigation.Tab

/**
 * iOS Renderer for Tabs Component
 *
 * Renders AVAMagic Tabs components as native UISegmentedControl.
 *
 * Features:
 * - Native UISegmentedControl rendering
 * - Text labels with optional icons
 * - Selection state tracking
 * - Customizable appearance
 * - Dark mode support
 * - Accessibility support
 *
 * Note: For 2-3 tabs, uses UISegmentedControl. For 4+ tabs, could use
 * custom scrollable tab bar (not implemented in this version).
 *
 * @author Manoj Jhawar
 * @since 2025-11-21
 */
@OptIn(ExperimentalForeignApi::class)
class IOSTabsRenderer {

    /**
     * Render Tabs component to UISegmentedControl
     */
    fun render(component: TabsComponent): UIView {
        // For many tabs, we'd create a scrollable custom view
        // For now, using UISegmentedControl (works best with 2-5 tabs)

        val segmentedControl = UISegmentedControl().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 32.0)

            // Add segments for each tab
            component.tabs.forEachIndexed { index, tab ->
                // Use icon if available, otherwise text
                if (tab.icon != null) {
                    val systemImage = mapIconToSystemImage(tab.icon)
                    if (systemImage != null) {
                        val image = UIImage.systemImageNamed(systemImage)
                        insertSegmentWithImage(image, index.toULong(), false)
                    } else {
                        insertSegmentWithTitle(tab.label, index.toULong(), false)
                    }
                } else {
                    insertSegmentWithTitle(tab.label, index.toULong(), false)
                }
            }

            // Set selected segment
            selectedSegmentIndex = component.selectedIndex.toLong()

            // Apply component style
            applyStyle(component)

            // Configure appearance
            configureAppearance(component)

            // Apply accessibility
            applyAccessibility(this, component)

            // Handle selection change
            // Note: In production, use addTarget:action:forControlEvents:
            // This is simplified for demonstration
            component.onTabSelected?.let { callback ->
                // Target-action would call: callback(newSelectedIndex)
            }
        }

        // Create container with optional content view
        val containerView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 400.0)
            backgroundColor = UIColor.systemBackgroundColor
            addSubview(segmentedControl)
        }

        return containerView
    }

    /**
     * Apply component style to segmented control
     */
    private fun UISegmentedControl.applyStyle(component: TabsComponent) {
        val style = component.style

        if (style != null) {
            // Background color
            // Note: UISegmentedControl uses backgroundColor for the container
            // and selectedSegmentTintColor for the selected segment

            // Selected segment color
            selectedSegmentTintColor = UIColor.systemBlueColor

            // In iOS 13+, we can customize more with appearance
            // For now, using default styling with tint
        }
    }

    /**
     * Configure segmented control appearance
     */
    private fun UISegmentedControl.configureAppearance(component: TabsComponent) {
        // Set apportions segments equally (default behavior)
        apportionsSegmentWidthsByContent = false

        // Enable momentum (for scrolling if needed)
        isMomentary = false
    }

    /**
     * Map icon name to SF Symbols system image name
     */
    private fun mapIconToSystemImage(icon: String): String? {
        return when (icon.lowercase()) {
            "dashboard", "overview" -> "square.grid.2x2.fill"
            "details", "info" -> "info.circle.fill"
            "settings" -> "gearshape.fill"
            "list" -> "list.bullet"
            "grid" -> "square.grid.3x3.fill"
            "chart", "analytics" -> "chart.bar.fill"
            "calendar" -> "calendar"
            "map" -> "map.fill"
            "photo", "gallery" -> "photo.fill"
            "video" -> "video.fill"
            "music" -> "music.note"
            "document", "file" -> "doc.fill"
            "folder" -> "folder.fill"
            "star", "favorites" -> "star.fill"
            "bookmark" -> "bookmark.fill"
            "tag" -> "tag.fill"
            else -> {
                // Try to use the icon name as-is
                icon.lowercase().replace("_", ".")
            }
        }
    }

    /**
     * Apply accessibility features
     */
    private fun applyAccessibility(control: UISegmentedControl, component: TabsComponent) {
        control.isAccessibilityElement = true
        control.accessibilityLabel = "Tab selector with ${component.tabs.size} tabs"
        control.accessibilityTraits = UIAccessibilityTraitTabBar
        control.accessibilityValue = component.tabs[component.selectedIndex].label
    }
}

/**
 * Alternative: Custom Scrollable Tab Bar for many tabs
 *
 * This would be implemented for 6+ tabs where UISegmentedControl
 * becomes unwieldy. Would use UIScrollView with custom tab buttons.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSScrollableTabsRenderer {

    /**
     * Render Tabs component to custom scrollable tab bar
     */
    fun render(component: TabsComponent): UIView {
        val scrollView = UIScrollView().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 44.0)
            showsHorizontalScrollIndicator = false
            backgroundColor = UIColor.systemBackgroundColor
        }

        // Create tab buttons
        var xOffset = 0.0
        component.tabs.forEachIndexed { index, tab ->
            val button = createTabButton(
                tab = tab,
                index = index,
                isSelected = index == component.selectedIndex,
                xOffset = xOffset,
                onClick = { component.onTabSelected?.invoke(index) }
            )

            scrollView.addSubview(button)
            xOffset += button.frame.useContents { size.width } + 8.0
        }

        // Set content size
        scrollView.contentSize = CGSizeMake(xOffset, 44.0)

        return scrollView
    }

    /**
     * Create individual tab button
     */
    private fun createTabButton(
        tab: Tab,
        index: Int,
        isSelected: Boolean,
        xOffset: Double,
        onClick: () -> Unit
    ): UIButton {
        val button = UIButton.buttonWithType(UIButtonTypeSystem).apply {
            frame = CGRectMake(xOffset, 8.0, 100.0, 28.0)
            setTitle(tab.label, UIControlStateNormal)

            if (isSelected) {
                backgroundColor = UIColor.systemBlueColor
                setTitleColor(UIColor.whiteColor, UIControlStateNormal)
            } else {
                backgroundColor = UIColor.systemGray5Color
                setTitleColor(UIColor.labelColor, UIControlStateNormal)
            }

            layer.cornerRadius = 14.0
            layer.masksToBounds = true

            // Add icon if available
            tab.icon?.let { iconName ->
                val systemImage = mapIconToSystemImage(iconName)
                if (systemImage != null) {
                    val image = UIImage.systemImageNamed(systemImage)
                    setImage(image, UIControlStateNormal)
                }
            }

            // Accessibility
            isAccessibilityElement = true
            accessibilityLabel = tab.label
            accessibilityTraits = UIAccessibilityTraitButton
            if (isSelected) {
                accessibilityTraits = accessibilityTraits or UIAccessibilityTraitSelected
            }

            // Note: In production, use addTarget:action:forControlEvents:
            // This is simplified for demonstration
        }

        return button
    }

    /**
     * Map icon name to SF Symbols system image name
     */
    private fun mapIconToSystemImage(icon: String): String? {
        return when (icon.lowercase()) {
            "dashboard", "overview" -> "square.grid.2x2.fill"
            "details", "info" -> "info.circle.fill"
            "settings" -> "gearshape.fill"
            else -> icon.lowercase().replace("_", ".")
        }
    }
}
