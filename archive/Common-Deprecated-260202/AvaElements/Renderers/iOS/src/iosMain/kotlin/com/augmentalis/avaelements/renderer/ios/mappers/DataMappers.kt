package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.flutter.material.data.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.core.Component

/**
 * iOS SwiftUI mappers for Flutter Data Parity Components - Part 1
 *
 * This file contains renderer functions that map cross-platform data component models
 * to SwiftUI implementations via Kotlin/Native.
 *
 * iOS-specific features:
 * - Native SwiftUI components (List, LazyVStack, LazyHStack, etc.)
 * - Pull-to-refresh support
 * - Dynamic Type support for accessibility
 * - RTL language support via environment values
 * - Smooth animations with SwiftUI's animation system
 * - Adaptive layouts for iPhone/iPad/Mac Catalyst
 * - Native scroll performance with UIScrollView backing
 *
 * All components support RTL (Right-to-Left) layouts automatically through SwiftUI's
 * LayoutDirection environment value.
 *
 * Data Components Part 1 (11 Components):
 * - RadioListTile, VirtualScroll, InfiniteScroll, DataList, DescriptionList,
 *   StatGroup, Stat, KPI, MetricCard, Leaderboard, Ranking
 *
 * @since 3.0.0-flutter-parity-ios
 */

/**
 * Render RadioListTile component using SwiftUI List row with radio button
 *
 * Maps RadioListTile to SwiftUI with:
 * - Native List row appearance
 * - Radio button (circle with fill indicator)
 * - Title and optional subtitle
 * - Configurable control affinity (leading/trailing)
 * - Selection state management
 * - Material3 theming support
 * - iOS: Uses HStack with Circle indicators
 *
 * @param component RadioListTile to render
 * @return SwiftUIView representing the radio list tile
 */
fun mapRadioListTile(
    component: RadioListTile
): SwiftUIView {
    val isSelected = component.isSelected
    val isTrailing = component.controlAffinity == RadioListTile.ListTileControlAffinity.Trailing

    // Build content views
    val contentViews = mutableListOf<SwiftUIView>()

    // Radio button view
    val radioButton = buildRadioButton(isSelected, component.activeColor)

    // Text content
    val textContent = buildTextContent(
        title = component.title,
        subtitle = component.subtitle,
        dense = component.dense,
        isThreeLine = component.isThreeLine
    )

    // Arrange based on control affinity
    if (isTrailing) {
        contentViews.add(textContent)
        contentViews.add(SwiftUIView.hStack(
            alignment = VerticalAlignment.Center,
            children = listOf(radioButton)
        ))
    } else {
        contentViews.add(radioButton)
        contentViews.add(textContent)
    }

    // Build main HStack
    val hStack = SwiftUIView.hStack(
        spacing = 12f,
        alignment = VerticalAlignment.Center,
        children = contentViews
    )

    // Apply styling modifiers
    val modifiers = mutableListOf<SwiftUIModifier>()

    // Padding
    component.contentPadding?.let { padding ->
        modifiers.add(SwiftUIModifier.padding(
            padding.top.toFloat(),
            padding.left,
            padding.bottom,
            padding.right
        ))
    } ?: run {
        modifiers.add(SwiftUIModifier.padding(if (component.dense) 8f else 16f))
    }

    // Background color
    val selectedColor = component.selectedTileColor
    val tileColor = component.tileColor
    val backgroundColor = when {
        isSelected && selectedColor != null -> parseColor(selectedColor)
        tileColor != null -> parseColor(tileColor)
        else -> null
    }
    backgroundColor?.let { modifiers.add(SwiftUIModifier.background(it)) }

    // Shape
    component.shape?.let { modifiers.add(SwiftUIModifier.cornerRadius(8f)) }

    // Disabled state
    if (!component.enabled) {
        modifiers.add(SwiftUIModifier.opacity(0.5f))
        modifiers.add(SwiftUIModifier.disabled(true))
    }

    // Add tap gesture for onChanged callback
    if (component.enabled && component.onChanged != null) {
        modifiers.add(SwiftUIModifier.onTapGesture(
            action = "radioListTile_${component.value}"
        ))
    }

    return hStack.copy(modifiers = modifiers)
}

/**
 * Build radio button indicator
 */
private fun buildRadioButton(selected: Boolean, activeColor: String?): SwiftUIView {
    val outerCircle = SwiftUIView(
        type = ViewType.Circle,
        properties = emptyMap(),
        modifiers = listOf(
            SwiftUIModifier.frame(width = 24f, height = 24f),
            SwiftUIModifier.border(
                parseColor(activeColor ?: "#007AFF"),
                width = 2f
            )
        )
    )

    if (!selected) return outerCircle

    val innerCircle = SwiftUIView(
        type = ViewType.Circle,
        properties = emptyMap(),
        modifiers = listOf(
            SwiftUIModifier.frame(width = 12f, height = 12f),
            SwiftUIModifier.background(parseColor(activeColor ?: "#007AFF"))
        )
    )

    return SwiftUIView.zStack(
        alignment = ZStackAlignment.Center,
        children = listOf(outerCircle, innerCircle)
    )
}

/**
 * Build text content (title and subtitle)
 */
private fun buildTextContent(
    title: String,
    subtitle: String?,
    dense: Boolean,
    isThreeLine: Boolean
): SwiftUIView {
    val views = mutableListOf<SwiftUIView>()

    // Title
    views.add(SwiftUIView.text(
        content = title,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Body),
            SwiftUIModifier.fontWeight(FontWeight.Medium)
        )
    ))

    // Subtitle
    subtitle?.let {
        views.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))
    }

    return SwiftUIView.vStack(
        spacing = if (dense) 2f else 4f,
        alignment = HorizontalAlignment.Leading,
        children = views,
        modifiers = listOf(SwiftUIModifier.fillMaxWidth())
    )
}

/**
 * Render VirtualScroll component using SwiftUI LazyVStack/LazyHStack
 *
 * Maps VirtualScroll to SwiftUI with:
 * - Virtualized rendering (only visible items)
 * - Vertical or horizontal orientation
 * - Fixed or dynamic item heights
 * - Scroll position restoration
 * - iOS: Uses LazyVStack/LazyHStack with ScrollView
 *
 * @param component VirtualScroll to render
 * @param childMapper Callback to render child components
 * @return SwiftUIView representing the virtual scroll
 */
fun mapVirtualScroll(
    component: VirtualScroll,
    childMapper: (Component) -> SwiftUIView
): SwiftUIView {
    val isVertical = component.orientation == VirtualScroll.Orientation.Vertical

    // Build lazy stack with items
    val itemViews = (0 until component.itemCount).map { index ->
        component.onItemRender?.invoke(index)?.let { childMapper(it) }
            ?: SwiftUIView(type = ViewType.EmptyView, properties = emptyMap())
    }

    val lazyStack = if (isVertical) {
        SwiftUIView(
            type = ViewType.Custom("LazyVStack"),
            properties = mapOf(
                "spacing" to 0f,
                "alignment" to "leading"
            ),
            children = itemViews
        )
    } else {
        SwiftUIView(
            type = ViewType.Custom("LazyHStack"),
            properties = mapOf(
                "spacing" to 0f,
                "alignment" to "top"
            ),
            children = itemViews
        )
    }

    // Wrap in ScrollView
    val scrollView = SwiftUIView(
        type = ViewType.ScrollView,
        properties = mapOf(
            "axes" to if (isVertical) "vertical" else "horizontal",
            "showsIndicators" to component.scrollbarVisible
        ),
        children = listOf(lazyStack)
    )

    // Apply modifiers
    val modifiers = mutableListOf<SwiftUIModifier>()

    // Background color
    component.backgroundColor?.let {
        modifiers.add(SwiftUIModifier.background(parseColor(it)))
    }

    // Content padding
    component.contentPadding?.let { padding ->
        modifiers.add(SwiftUIModifier.padding(
            padding.top.toFloat(),
            padding.left,
            padding.bottom,
            padding.right
        ))
    }

    return scrollView.copy(modifiers = modifiers)
}

/**
 * Render InfiniteScroll component using SwiftUI List with loading indicator
 *
 * Maps InfiniteScroll to SwiftUI with:
 * - Automatic load-more triggering
 * - Loading state indicator
 * - Error state handling
 * - End-of-list message
 * - Pull-to-refresh support
 * - iOS: Uses List with threshold-based loading
 *
 * @param component InfiniteScroll to render
 * @param childMapper Callback to render child components
 * @return SwiftUIView representing the infinite scroll
 */
fun mapInfiniteScroll(
    component: InfiniteScroll,
    childMapper: (Component) -> SwiftUIView
): SwiftUIView {
    val itemViews = component.items.map { item -> childMapper(item) }.toMutableList()

    // Add footer based on state
    val footerView = when (component.getFooterState()) {
        InfiniteScroll.FooterState.Loading -> buildLoadingFooter(component.loadingIndicatorText)
        InfiniteScroll.FooterState.End -> buildEndFooter(component.endMessageText)
        InfiniteScroll.FooterState.Error -> buildErrorFooter(component.errorMessageText)
        InfiniteScroll.FooterState.None -> null
    }

    footerView?.let { itemViews.add(it) }

    // Build lazy stack
    val isVertical = component.orientation == InfiniteScroll.Orientation.Vertical
    val lazyStack = if (isVertical) {
        SwiftUIView(
            type = ViewType.Custom("LazyVStack"),
            properties = mapOf("spacing" to 8f),
            children = itemViews
        )
    } else {
        SwiftUIView(
            type = ViewType.Custom("LazyHStack"),
            properties = mapOf("spacing" to 8f),
            children = itemViews
        )
    }

    // Wrap in ScrollView with load-more detection
    val scrollView = SwiftUIView(
        type = ViewType.ScrollView,
        properties = mapOf(
            "axes" to if (isVertical) "vertical" else "horizontal",
            "onScrolledToBottom" to "infiniteScroll_loadMore",
            "threshold" to component.loadingThreshold
        ),
        children = listOf(lazyStack)
    )

    // Apply modifiers
    val modifiers = mutableListOf<SwiftUIModifier>()

    component.backgroundColor?.let {
        modifiers.add(SwiftUIModifier.background(parseColor(it)))
    }

    component.contentPadding?.let { padding ->
        modifiers.add(SwiftUIModifier.padding(
            padding.top.toFloat(),
            padding.left,
            padding.bottom,
            padding.right
        ))
    }

    return scrollView.copy(modifiers = modifiers)
}

/**
 * Build loading footer
 */
private fun buildLoadingFooter(text: String?): SwiftUIView {
    val progressView = SwiftUIView(
        type = ViewType.Custom("ProgressView"),
        properties = mapOf("style" to "circular")
    )

    val views = mutableListOf(progressView)

    text?.let {
        views.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))
    }

    return SwiftUIView.hStack(
        spacing = 8f,
        alignment = VerticalAlignment.Center,
        children = views,
        modifiers = listOf(SwiftUIModifier.padding(16f))
    )
}

/**
 * Build end footer
 */
private fun buildEndFooter(text: String?): SwiftUIView {
    return SwiftUIView.text(
        content = text ?: "No more items",
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Caption),
            SwiftUIModifier.foregroundColor(SwiftUIColor.secondary),
            SwiftUIModifier.padding(16f)
        )
    )
}

/**
 * Build error footer
 */
private fun buildErrorFooter(text: String?): SwiftUIView {
    return SwiftUIView.button(
        label = text ?: "Tap to retry",
        action = "infiniteScroll_retry",
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Caption),
            SwiftUIModifier.foregroundColor(SwiftUIColor.red),
            SwiftUIModifier.padding(16f)
        )
    )
}

/**
 * Render DataList component using SwiftUI VStack with key-value pairs
 *
 * Maps DataList to SwiftUI with:
 * - Stacked, inline, or grid layouts
 * - Optional dividers between items
 * - Customizable text styling
 * - iOS: Uses VStack/HStack combinations
 *
 * @param component DataList to render
 * @return SwiftUIView representing the data list
 */
fun mapDataList(component: DataList): SwiftUIView {
    val itemViews = component.items.mapIndexed { index, item ->
        val itemView = when (component.layout) {
            DataList.Layout.Stacked -> buildStackedDataItem(item)
            DataList.Layout.Inline -> buildInlineDataItem(item)
            DataList.Layout.Grid -> buildGridDataItem(item)
        }

        // Add divider if needed
        if (component.showDividers && index < component.items.size - 1) {
            listOf(itemView, SwiftUIView(type = ViewType.Divider, properties = emptyMap()))
        } else {
            listOf(itemView)
        }
    }.flatten()

    val contentStack = SwiftUIView.vStack(
        spacing = if (component.dense) 4f else 8f,
        alignment = HorizontalAlignment.Leading,
        children = itemViews
    )

    // Add title if present
    val views = mutableListOf<SwiftUIView>()
    component.title?.let {
        views.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Headline),
                SwiftUIModifier.fontWeight(FontWeight.Bold)
            )
        ))
    }
    views.add(contentStack)

    return SwiftUIView.vStack(
        spacing = 12f,
        alignment = HorizontalAlignment.Leading,
        children = views,
        modifiers = listOf(SwiftUIModifier.padding(16f))
    )
}

/**
 * Build stacked data item (key above value)
 */
private fun buildStackedDataItem(item: DataList.DataItem): SwiftUIView {
    return SwiftUIView.vStack(
        spacing = 4f,
        alignment = HorizontalAlignment.Leading,
        children = listOf(
            SwiftUIView.text(
                content = item.key,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                )
            ),
            SwiftUIView.text(
                content = item.value,
                modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
            )
        )
    )
}

/**
 * Build inline data item (key and value on same line)
 */
private fun buildInlineDataItem(item: DataList.DataItem): SwiftUIView {
    return SwiftUIView.hStack(
        spacing = 8f,
        alignment = VerticalAlignment.Center,
        children = listOf(
            SwiftUIView.text(
                content = "${item.key}:",
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.fontWeight(FontWeight.Medium)
                )
            ),
            SwiftUIView.text(
                content = item.value,
                modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
            )
        )
    )
}

/**
 * Build grid data item (for 2-column layout)
 */
private fun buildGridDataItem(item: DataList.DataItem): SwiftUIView {
    return SwiftUIView.hStack(
        spacing = 16f,
        alignment = VerticalAlignment.Top,
        children = listOf(
            SwiftUIView.text(
                content = item.key,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.fontWeight(FontWeight.Medium),
                    SwiftUIModifier.frame(width = 120f)
                )
            ),
            SwiftUIView.text(
                content = item.value,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.fillMaxWidth()
                )
            )
        )
    )
}

/**
 * Render DescriptionList component using SwiftUI VStack with term-description pairs
 *
 * Maps DescriptionList to SwiftUI with:
 * - Term and description pairing
 * - Optional numbering
 * - Expandable descriptions
 * - iOS: Uses VStack with disclosure groups for expandable
 *
 * @param component DescriptionList to render
 * @return SwiftUIView representing the description list
 */
fun mapDescriptionList(component: DescriptionList): SwiftUIView {
    val itemViews = component.items.mapIndexed { index, descItem ->
        if (component.expandable) {
            buildExpandableDescriptionItem(descItem, index + 1, component.numbered)
        } else {
            buildDescriptionItem(descItem, index + 1, component.numbered, component.dense)
        }
    }

    val contentStack = SwiftUIView.vStack(
        spacing = if (component.dense) 8f else 16f,
        alignment = HorizontalAlignment.Leading,
        children = itemViews
    )

    // Add title if present
    val views = mutableListOf<SwiftUIView>()
    component.title?.let {
        views.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Headline),
                SwiftUIModifier.fontWeight(FontWeight.Bold)
            )
        ))
    }
    views.add(contentStack)

    return SwiftUIView.vStack(
        spacing = 12f,
        alignment = HorizontalAlignment.Leading,
        children = views,
        modifiers = listOf(SwiftUIModifier.padding(16f))
    )
}

/**
 * Build standard description item
 */
private fun buildDescriptionItem(
    item: DescriptionList.DescriptionItem,
    number: Int,
    numbered: Boolean,
    dense: Boolean
): SwiftUIView {
    val termText = if (numbered) "$number. ${item.term}" else item.term

    return SwiftUIView.vStack(
        spacing = if (dense) 4f else 8f,
        alignment = HorizontalAlignment.Leading,
        children = listOf(
            SwiftUIView.text(
                content = termText,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.fontWeight(FontWeight.Bold)
                )
            ),
            SwiftUIView.text(
                content = item.description,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                )
            )
        )
    )
}

/**
 * Build expandable description item
 */
private fun buildExpandableDescriptionItem(
    item: DescriptionList.DescriptionItem,
    number: Int,
    numbered: Boolean
): SwiftUIView {
    val termText = if (numbered) "$number. ${item.term}" else item.term

    return SwiftUIView(
        type = ViewType.Custom("DisclosureGroup"),
        properties = mapOf(
            "label" to termText,
            "content" to item.description
        )
    )
}

/**
 * Render StatGroup component using SwiftUI layout based on orientation
 *
 * Maps StatGroup to SwiftUI with:
 * - Horizontal, vertical, or grid layouts
 * - Multiple statistics display
 * - Change indicators
 * - iOS: Uses HStack/VStack/LazyVGrid
 *
 * @param component StatGroup to render
 * @return SwiftUIView representing the stat group
 */
fun mapStatGroup(component: StatGroup): SwiftUIView {
    val statViews = component.stats.map { stat -> buildStatItem(stat) }

    val contentStack = when (component.layout) {
        StatGroup.Layout.Horizontal -> SwiftUIView.hStack(
            spacing = if (component.showDividers) 0f else 16f,
            alignment = VerticalAlignment.Top,
            children = if (component.showDividers) {
                statViews.flatMapIndexed { index, statView ->
                    if (index < statViews.size - 1) {
                        listOf(statView, SwiftUIView(type = ViewType.VerticalDivider, properties = emptyMap()))
                    } else listOf(statView)
                }
            } else statViews
        )
        StatGroup.Layout.Vertical -> SwiftUIView.vStack(
            spacing = if (component.showDividers) 0f else 16f,
            alignment = HorizontalAlignment.Leading,
            children = if (component.showDividers) {
                statViews.flatMapIndexed { index, statView ->
                    if (index < statViews.size - 1) {
                        listOf(statView, SwiftUIView(type = ViewType.Divider, properties = emptyMap()))
                    } else listOf(statView)
                }
            } else statViews
        )
        StatGroup.Layout.Grid -> SwiftUIView(
            type = ViewType.Custom("LazyVGrid"),
            properties = mapOf("columns" to 2),
            children = statViews
        )
    }

    // Add title if present
    val views = mutableListOf<SwiftUIView>()
    component.title?.let {
        views.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Headline),
                SwiftUIModifier.fontWeight(FontWeight.Bold)
            )
        ))
    }
    views.add(contentStack)

    return SwiftUIView.vStack(
        spacing = 12f,
        alignment = HorizontalAlignment.Leading,
        children = views,
        modifiers = listOf(SwiftUIModifier.padding(16f))
    )
}

/**
 * Build single stat item
 */
private fun buildStatItem(stat: StatGroup.StatItem): SwiftUIView {
    val views = mutableListOf<SwiftUIView>()

    // Icon (if present)
    stat.icon?.let {
        views.add(SwiftUIView(
            type = ViewType.Image,
            properties = mapOf("systemName" to it),
            modifiers = listOf(
                SwiftUIModifier.frame(width = 24f, height = 24f),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))
    }

    // Text content
    val textContent = SwiftUIView.vStack(
        spacing = 4f,
        alignment = HorizontalAlignment.Leading,
        children = listOf(
            SwiftUIView.text(
                content = stat.label,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                )
            ),
            SwiftUIView.text(
                content = stat.value,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Title2),
                    SwiftUIModifier.fontWeight(FontWeight.Bold)
                )
            ),
            buildChangeIndicator(stat.change, stat.changeType)
        ).filterNotNull()
    )
    views.add(textContent)

    return SwiftUIView.vStack(
        spacing = 8f,
        alignment = HorizontalAlignment.Leading,
        children = views,
        modifiers = listOf(SwiftUIModifier.padding(12f))
    )
}

/**
 * Build change indicator
 */
private fun buildChangeIndicator(change: String?, changeType: StatGroup.ChangeType): SwiftUIView? {
    if (change == null) return null

    val color = when (changeType) {
        StatGroup.ChangeType.Positive -> SwiftUIColor.green
        StatGroup.ChangeType.Negative -> SwiftUIColor.red
        StatGroup.ChangeType.Neutral -> SwiftUIColor.secondary
    }

    return SwiftUIView.text(
        content = change,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Caption),
            SwiftUIModifier.foregroundColor(color)
        )
    )
}

/**
 * Render Stat component using SwiftUI card with metric display
 *
 * Maps Stat to SwiftUI with:
 * - Large value display
 * - Optional change indicator
 * - Icon support
 * - iOS: Uses VStack with card styling
 *
 * @param component Stat to render
 * @return SwiftUIView representing the stat
 */
fun mapStat(component: Stat): SwiftUIView {
    val views = mutableListOf<SwiftUIView>()

    // Icon and label row
    val headerViews = mutableListOf<SwiftUIView>()
    component.icon?.let {
        headerViews.add(SwiftUIView(
            type = ViewType.Image,
            properties = mapOf("systemName" to it),
            modifiers = listOf(
                SwiftUIModifier.frame(width = 20f, height = 20f),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))
    }
    headerViews.add(SwiftUIView.text(
        content = component.label,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Subheadline),
            SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
        )
    ))

    views.add(SwiftUIView.hStack(
        spacing = 6f,
        alignment = VerticalAlignment.Center,
        children = headerViews
    ))

    // Value
    views.add(SwiftUIView.text(
        content = component.value,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.LargeTitle),
            SwiftUIModifier.fontWeight(FontWeight.Bold)
        )
    ))

    // Change indicator
    component.change?.let { change ->
        val color = when (component.changeType) {
            Stat.ChangeType.Positive -> SwiftUIColor.green
            Stat.ChangeType.Negative -> SwiftUIColor.red
            Stat.ChangeType.Neutral -> SwiftUIColor.secondary
        }

        views.add(SwiftUIView.text(
            content = change,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(color)
            )
        ))
    }

    // Description
    component.description?.let {
        views.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))
    }

    val vStack = SwiftUIView.vStack(
        spacing = 8f,
        alignment = HorizontalAlignment.Leading,
        children = views
    )

    // Apply card styling
    val modifiers = mutableListOf(
        SwiftUIModifier.padding(16f),
        SwiftUIModifier.background(SwiftUIColor.systemBackground),
        SwiftUIModifier.cornerRadius(12f)
    )

    if (component.elevated) {
        modifiers.add(SwiftUIModifier.shadow(radius = 4f, x = 0f, y = 2f))
    }

    // Add tap gesture if clickable
    if (component.onClick != null) {
        modifiers.add(SwiftUIModifier.onTapGesture(action = "stat_${component.id}"))
    }

    return vStack.copy(modifiers = modifiers)
}

/**
 * Render KPI component using SwiftUI card with target and progress
 *
 * Maps KPI to SwiftUI with:
 * - Large value display
 * - Target comparison
 * - Progress bar
 * - Trend indicators
 * - iOS: Uses VStack with ProgressView
 *
 * @param component KPI to render
 * @return SwiftUIView representing the KPI
 */
fun mapKPI(component: KPI): SwiftUIView {
    val views = mutableListOf<SwiftUIView>()

    // Header with icon and title
    val headerViews = mutableListOf<SwiftUIView>()
    component.icon?.let {
        headerViews.add(SwiftUIView(
            type = ViewType.Image,
            properties = mapOf("systemName" to it),
            modifiers = listOf(
                SwiftUIModifier.frame(width = 24f, height = 24f),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))
    }
    headerViews.add(SwiftUIView.text(
        content = component.title,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Headline),
            SwiftUIModifier.fontWeight(FontWeight.Semibold)
        )
    ))

    views.add(SwiftUIView.hStack(
        spacing = 8f,
        alignment = VerticalAlignment.Center,
        children = headerViews
    ))

    // Value
    views.add(SwiftUIView.text(
        content = component.value,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.LargeTitle),
            SwiftUIModifier.fontWeight(FontWeight.Bold)
        )
    ))

    // Target and subtitle
    val subtitleViews = mutableListOf<SwiftUIView>()
    component.target?.let {
        subtitleViews.add(SwiftUIView.text(
            content = "Target: $it",
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))
    }
    component.subtitle?.let {
        subtitleViews.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))
    }
    if (subtitleViews.isNotEmpty()) {
        views.add(SwiftUIView.vStack(
            spacing = 2f,
            alignment = HorizontalAlignment.Leading,
            children = subtitleViews
        ))
    }

    // Progress bar
    val progressValue = component.progress
    if (component.showProgressBar && progressValue != null) {
        views.add(SwiftUIView(
            type = ViewType.Custom("ProgressView"),
            properties = mapOf(
                "value" to progressValue,
                "style" to "linear"
            ),
            modifiers = listOf(SwiftUIModifier.padding(vertical = 8f))
        ))
    }

    // Trend indicator
    val trendIcon = when (component.trend) {
        KPI.TrendType.Up -> "arrow.up.right"
        KPI.TrendType.Down -> "arrow.down.right"
        KPI.TrendType.Neutral -> "arrow.right"
    }
    val trendColor = when (component.trend) {
        KPI.TrendType.Up -> SwiftUIColor.green
        KPI.TrendType.Down -> SwiftUIColor.red
        KPI.TrendType.Neutral -> SwiftUIColor.secondary
    }

    views.add(SwiftUIView(
        type = ViewType.Image,
        properties = mapOf("systemName" to trendIcon),
        modifiers = listOf(
            SwiftUIModifier.frame(width = 16f, height = 16f),
            SwiftUIModifier.foregroundColor(trendColor)
        )
    ))

    val vStack = SwiftUIView.vStack(
        spacing = 12f,
        alignment = HorizontalAlignment.Leading,
        children = views
    )

    // Apply card styling
    val modifiers = mutableListOf(
        SwiftUIModifier.padding(20f),
        SwiftUIModifier.background(SwiftUIColor.systemBackground),
        SwiftUIModifier.cornerRadius(16f),
        SwiftUIModifier.shadow(radius = 4f, x = 0f, y = 2f)
    )

    // Add tap gesture if clickable
    if (component.onClick != null) {
        modifiers.add(SwiftUIModifier.onTapGesture(action = "kpi_${component.id}"))
    }

    return vStack.copy(modifiers = modifiers)
}

/**
 * Render MetricCard component using SwiftUI card with comparison data
 *
 * Maps MetricCard to SwiftUI with:
 * - Large metric value
 * - Comparison with previous period
 * - Optional sparkline
 * - iOS: Uses VStack with optional Chart view
 *
 * @param component MetricCard to render
 * @return SwiftUIView representing the metric card
 */
fun mapMetricCard(component: MetricCard): SwiftUIView {
    val views = mutableListOf<SwiftUIView>()

    // Icon and title row
    val headerViews = mutableListOf<SwiftUIView>()
    component.icon?.let {
        headerViews.add(SwiftUIView(
            type = ViewType.Image,
            properties = mapOf("systemName" to it),
            modifiers = listOf(
                SwiftUIModifier.frame(width = 20f, height = 20f),
                SwiftUIModifier.foregroundColor(component.color?.let { parseColor(it) } ?: SwiftUIColor.primary)
            )
        ))
    }
    headerViews.add(SwiftUIView.text(
        content = component.title,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Subheadline),
            SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
        )
    ))

    views.add(SwiftUIView.hStack(
        spacing = 6f,
        alignment = VerticalAlignment.Center,
        children = headerViews
    ))

    // Value with unit
    views.add(SwiftUIView.text(
        content = component.getFormattedValue(),
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Title),
            SwiftUIModifier.fontWeight(FontWeight.Bold)
        )
    ))

    // Change and comparison
    val changeViews = mutableListOf<SwiftUIView>()
    component.change?.let { change ->
        val color = when (component.changeType) {
            MetricCard.ChangeType.Positive -> SwiftUIColor.green
            MetricCard.ChangeType.Negative -> SwiftUIColor.red
            MetricCard.ChangeType.Neutral -> SwiftUIColor.secondary
        }

        changeViews.add(SwiftUIView.text(
            content = change,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.fontWeight(FontWeight.Medium),
                SwiftUIModifier.foregroundColor(color)
            )
        ))
    }
    component.comparison?.let {
        changeViews.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))
    }
    if (changeViews.isNotEmpty()) {
        views.add(SwiftUIView.hStack(
            spacing = 4f,
            alignment = VerticalAlignment.Center,
            children = changeViews
        ))
    }

    // Sparkline
    val sparklineData = component.sparklineData
    if (component.showSparkline && sparklineData != null) {
        views.add(SwiftUIView(
            type = ViewType.Custom("Sparkline"),
            properties = mapOf(
                "data" to sparklineData,
                "color" to (component.color ?: "#007AFF")
            ),
            modifiers = listOf(
                SwiftUIModifier.frame(height = 40f),
                SwiftUIModifier.padding(vertical = 8f)
            )
        ))
    }

    val vStack = SwiftUIView.vStack(
        spacing = 8f,
        alignment = HorizontalAlignment.Leading,
        children = views
    )

    // Apply card styling
    val modifiers = mutableListOf(
        SwiftUIModifier.padding(16f),
        SwiftUIModifier.background(SwiftUIColor.systemBackground),
        SwiftUIModifier.cornerRadius(12f),
        SwiftUIModifier.shadow(radius = 2f, x = 0f, y = 1f)
    )

    // Add tap gesture if clickable
    if (component.onClick != null) {
        modifiers.add(SwiftUIModifier.onTapGesture(action = "metricCard_${component.id}"))
    }

    return vStack.copy(modifiers = modifiers)
}

/**
 * Render Leaderboard component using SwiftUI List with ranked items
 *
 * Maps Leaderboard to SwiftUI with:
 * - Ranked list with position indicators
 * - Avatar support
 * - Top 3 badge support
 * - Current user highlighting
 * - iOS: Uses List with custom cells
 *
 * @param component Leaderboard to render
 * @return SwiftUIView representing the leaderboard
 */
fun mapLeaderboard(component: Leaderboard): SwiftUIView {
    val itemViews = component.getDisplayItems().map { leaderboardItem ->
        buildLeaderboardItem(
            item = leaderboardItem,
            isCurrentUser = component.isCurrentUser(leaderboardItem),
            showBadge = component.showTopBadges && leaderboardItem.rank <= 3
        )
    }

    val views = mutableListOf<SwiftUIView>()

    // Title
    component.title?.let {
        views.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Title2),
                SwiftUIModifier.fontWeight(FontWeight.Bold),
                SwiftUIModifier.padding(16f)
            )
        ))
    }

    // Items
    views.add(SwiftUIView.vStack(
        spacing = 8f,
        alignment = HorizontalAlignment.Leading,
        children = itemViews
    ))

    return SwiftUIView.vStack(
        spacing = 0f,
        alignment = HorizontalAlignment.Leading,
        children = views,
        modifiers = listOf(SwiftUIModifier.padding(16f))
    )
}

/**
 * Build single leaderboard item
 */
private fun buildLeaderboardItem(
    item: Leaderboard.LeaderboardItem,
    isCurrentUser: Boolean,
    showBadge: Boolean
): SwiftUIView {
    val itemViews = mutableListOf<SwiftUIView>()

    // Rank badge
    val rankView = if (showBadge) {
        buildRankBadge(item.rank)
    } else {
        SwiftUIView.text(
            content = "#${item.rank}",
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.fontWeight(FontWeight.Bold),
                SwiftUIModifier.frame(width = 40f)
            )
        )
    }
    itemViews.add(rankView)

    // Avatar
    item.avatar?.let {
        itemViews.add(SwiftUIView(
            type = ViewType.AsyncImage,
            properties = mapOf("url" to it),
            modifiers = listOf(
                SwiftUIModifier.frame(width = 40f, height = 40f),
                SwiftUIModifier.cornerRadius(20f)
            )
        ))
    }

    // Name and subtitle
    val textViews = mutableListOf(
        SwiftUIView.text(
            content = item.name,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.fontWeight(if (isCurrentUser) FontWeight.Bold else FontWeight.Medium)
            )
        )
    )
    item.subtitle?.let {
        textViews.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))
    }
    itemViews.add(SwiftUIView.vStack(
        spacing = 2f,
        alignment = HorizontalAlignment.Leading,
        children = textViews,
        modifiers = listOf(SwiftUIModifier.fillMaxWidth())
    ))

    // Score
    itemViews.add(SwiftUIView.text(
        content = item.score,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Body),
            SwiftUIModifier.fontWeight(FontWeight.Bold)
        )
    ))

    val hStack = SwiftUIView.hStack(
        spacing = 12f,
        alignment = VerticalAlignment.Center,
        children = itemViews
    )

    // Highlight current user
    val modifiers = mutableListOf(SwiftUIModifier.padding(12f))
    if (isCurrentUser) {
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.secondary.copy(opacity = 0.1f)))
        modifiers.add(SwiftUIModifier.cornerRadius(8f))
    }

    return hStack.copy(modifiers = modifiers)
}

/**
 * Build rank badge for top 3
 */
private fun buildRankBadge(rank: Int): SwiftUIView {
    val emoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#$rank"
    }

    return SwiftUIView.text(
        content = emoji,
        modifiers = listOf(
            SwiftUIModifier.font(FontStyle.Title3),
            SwiftUIModifier.frame(width = 40f)
        )
    )
}

/**
 * Render Ranking component using SwiftUI badge with position
 *
 * Maps Ranking to SwiftUI with:
 * - Numeric rank display
 * - Top 3 badge styling
 * - Change indicator
 * - iOS: Uses Text with custom styling
 *
 * @param component Ranking to render
 * @return SwiftUIView representing the ranking
 */
fun mapRanking(component: Ranking): SwiftUIView {
    val badgeType = component.getBadgeType()

    val views = mutableListOf<SwiftUIView>()

    // Label (if present)
    component.label?.let {
        views.add(SwiftUIView.text(
            content = it,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))
    }

    // Rank display
    val rankText = if (badgeType != null) {
        when (badgeType) {
            Ranking.BadgeType.Gold -> "🥇"
            Ranking.BadgeType.Silver -> "🥈"
            Ranking.BadgeType.Bronze -> "🥉"
        }
    } else {
        component.getOrdinal()
    }

    val fontSize = when (component.size) {
        Ranking.Size.Small -> FontStyle.Body
        Ranking.Size.Medium -> FontStyle.Title3
        Ranking.Size.Large -> FontStyle.Title
    }

    views.add(SwiftUIView.text(
        content = rankText,
        modifiers = listOf(
            SwiftUIModifier.font(fontSize),
            SwiftUIModifier.fontWeight(FontWeight.Bold)
        )
    ))

    // Change indicator
    component.change?.let { change ->
        val changeDirection = component.getChangeDirection()
        val changeIcon = when (changeDirection) {
            Ranking.ChangeDirection.Up -> "↑"
            Ranking.ChangeDirection.Down -> "↓"
            Ranking.ChangeDirection.Same -> "−"
            null -> ""
        }
        val changeColor = when (changeDirection) {
            Ranking.ChangeDirection.Up -> SwiftUIColor.green
            Ranking.ChangeDirection.Down -> SwiftUIColor.red
            else -> SwiftUIColor.secondary
        }

        views.add(SwiftUIView.text(
            content = "$changeIcon${kotlin.math.abs(change)}",
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(changeColor)
            )
        ))
    }

    return SwiftUIView.vStack(
        spacing = 4f,
        alignment = HorizontalAlignment.Center,
        children = views,
        modifiers = listOf(SwiftUIModifier.padding(8f))
    )
}

/**
 * Helper: Parse color string to SwiftUIColor
 */
private fun parseColor(colorString: String): SwiftUIColor {
    return when {
        colorString.startsWith("#") && colorString.length >= 7 -> SwiftUIColor.rgb(
            red = colorString.substring(1, 3).toInt(16) / 255f,
            green = colorString.substring(3, 5).toInt(16) / 255f,
            blue = colorString.substring(5, 7).toInt(16) / 255f
        )
        else -> SwiftUIColor.primary // Fallback
    }
}

/**
 * Helper: Copy SwiftUIColor with opacity
 */
private fun SwiftUIColor.copy(opacity: Float): SwiftUIColor {
    return when (this.type) {
        SwiftUIColor.ColorType.RGB -> {
            val rgbValue = this.value as? RGBValue
            if (rgbValue != null) {
                SwiftUIColor.rgb(rgbValue.red, rgbValue.green, rgbValue.blue, opacity)
            } else {
                this
            }
        }
        else -> this
    }
}

/**
 * Helper: Vertical padding modifier
 */
private fun SwiftUIModifier.Companion.padding(vertical: Float): SwiftUIModifier {
    return SwiftUIModifier.padding(vertical, 0f, vertical, 0f)
}

/**
 * Helper: Frame with width/height
 */
private fun SwiftUIModifier.Companion.frame(width: Float? = null, height: Float? = null): SwiftUIModifier {
    return SwiftUIModifier(
        type = ModifierType.Frame,
        value = FrameValue(
            width = width?.let { SizeValue.Fixed(it) },
            height = height?.let { SizeValue.Fixed(it) },
            alignment = ZStackAlignment.Center
        )
    )
}

/**
 * Helper: OnTapGesture modifier
 */
private fun SwiftUIModifier.Companion.onTapGesture(action: String): SwiftUIModifier {
    return SwiftUIModifier(ModifierType.OnTapGesture, action)
}
