package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.data.*
import com.augmentalis.avaelements.flutter.material.advanced.RichText
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Material Data Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity data components to SwiftUI equivalents.
 *
 * Components:
 * - DataList → VStack with key-value rows
 * - DescriptionList → VStack with term-definition pairs
 * - StatGroup → HStack/LazyVGrid of stat cards
 * - Stat → Card with value and change indicator
 * - KPI → Card with progress and target display
 * - MetricCard → Card with metric visualization
 * - Leaderboard → List with ranked items
 * - Ranking → List with rank indicators
 * - Zoom → Interactive zoom container
 * - VirtualScroll → LazyVStack with optimized rendering
 * - InfiniteScroll → LazyVStack with load more
 * - QRCode → Image view with QR code generation
 * - RichText → Text with rich formatting
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// DATA LIST
// ============================================

object DataListMapper {
    fun map(component: DataList, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Title
        component.title?.let { title ->
            children.add(SwiftUIView.text(
                content = title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                )
            ))
        }

        // Items based on layout
        val itemViews = component.items.map { item ->
            when (component.layout) {
                DataList.Layout.Stacked -> createStackedItem(item, component.showDividers, theme)
                DataList.Layout.Inline -> createInlineItem(item, component.showDividers, theme)
                DataList.Layout.Grid -> createGridItem(item, theme)
            }
        }

        when (component.layout) {
            DataList.Layout.Grid -> {
                // Use LazyVGrid for grid layout
                children.add(SwiftUIView(
                    type = ViewType.Custom("LazyVGrid"),
                    properties = mapOf("columns" to 2),
                    children = itemViews
                ))
            }
            else -> {
                children.addAll(itemViews)
            }
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "alignment" to "leading",
                "spacing" to if (component.dense) 8f else 12f
            ),
            children = children,
        )
    }

    private fun createStackedItem(item: DataList.DataItem, showDivider: Boolean, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        children.add(SwiftUIView.text(
            content = item.key,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
            )
        ))

        children.add(SwiftUIView.text(
            content = item.value,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
        ))

        if (showDivider) {
            children.add(SwiftUIView(
                type = ViewType.Custom("Divider"),
                properties = emptyMap()
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 4f),
            children = children
        )
    }

    private fun createInlineItem(item: DataList.DataItem, showDivider: Boolean, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val row = SwiftUIView(
            type = ViewType.HStack,
            properties = emptyMap(),
            children = listOf(
                SwiftUIView.text(
                    content = item.key,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Body),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                    )
                ),
                SwiftUIView(
                    type = ViewType.Custom("Spacer"),
                    properties = emptyMap()
                ),
                SwiftUIView.text(
                    content = item.value,
                    modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
                )
            )
        )

        children.add(row)

        if (showDivider) {
            children.add(SwiftUIView(
                type = ViewType.Custom("Divider"),
                properties = emptyMap()
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("spacing" to 4f),
            children = children
        )
    }

    private fun createGridItem(item: DataList.DataItem, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 4f),
            children = listOf(
                SwiftUIView.text(
                    content = item.key,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                    )
                ),
                SwiftUIView.text(
                    content = item.value,
                    modifiers = listOf(SwiftUIModifier.font(FontStyle.Subheadline))
                )
            ),
            modifiers = listOf(
                SwiftUIModifier.padding(12f),
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                SwiftUIModifier.cornerRadius(8f)
            )
        )
    }
}

// ============================================
// DESCRIPTION LIST
// ============================================

object DescriptionListMapper {
    fun map(component: DescriptionList, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        component.title?.let { title ->
            children.add(SwiftUIView.text(
                content = title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                )
            ))
        }

        component.items.forEachIndexed { index, item ->
            val termChildren = mutableListOf<SwiftUIView>()

            // Term with optional numbering
            val termText = if (component.numbered) "${index + 1}. ${item.term}" else item.term
            termChildren.add(SwiftUIView.text(
                content = termText,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Subheadline),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold)
                )
            ))

            // Description
            termChildren.add(SwiftUIView.text(
                content = item.description,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))

            children.add(SwiftUIView(
                type = ViewType.VStack,
                properties = mapOf("alignment" to "leading", "spacing" to 4f),
                children = termChildren
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "alignment" to "leading",
                "spacing" to if (component.dense) 8f else 16f
            ),
            children = children,
        )
    }
}

// ============================================
// STAT GROUP
// ============================================

object StatGroupMapper {
    fun map(component: StatGroup, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        component.title?.let { title ->
            children.add(SwiftUIView.text(
                content = title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                )
            ))
        }

        // Render stat items directly
        val statViews = component.stats.map { statItem ->
            createStatItemView(statItem)
        }

        when (component.layout) {
            StatGroup.Layout.Grid -> {
                children.add(SwiftUIView(
                    type = ViewType.Custom("LazyVGrid"),
                    properties = mapOf("columns" to 2), // Default 2 columns for grid
                    children = statViews
                ))
            }
            StatGroup.Layout.Horizontal -> {
                children.add(SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 12f),
                    children = statViews
                ))
            }
            StatGroup.Layout.Vertical -> {
                children.addAll(statViews)
            }
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 12f),
            children = children,
        )
    }

    private fun createStatItemView(item: StatGroup.StatItem): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Icon if present
        item.icon?.let { icon ->
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to icon, "size" to 24f),
                modifiers = listOf(SwiftUIModifier.foregroundColor(SwiftUIColor.primary))
            ))
        }

        // Value
        children.add(SwiftUIView.text(
            content = item.value,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.LargeTitle),
                SwiftUIModifier.fontWeight(FontWeight.Bold)
            )
        ))

        // Label
        children.add(SwiftUIView.text(
            content = item.label,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
            )
        ))

        // Change indicator
        item.change?.let { change ->
            val changeColor = when (item.changeType) {
                StatGroup.ChangeType.Positive -> SwiftUIColor.system("systemGreen")
                StatGroup.ChangeType.Negative -> SwiftUIColor.system("systemRed")
                StatGroup.ChangeType.Neutral -> SwiftUIColor.system("label")
            }

            children.add(SwiftUIView.text(
                content = change,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(changeColor)
                )
            ))
        }

        // Description
        item.description?.let { desc ->
            children.add(SwiftUIView.text(
                content = desc,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption2),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("tertiaryLabel"))
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(16f),
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                SwiftUIModifier.cornerRadius(12f)
            )
        )
    }
}

// ============================================
// STAT
// ============================================

object StatMapper {
    fun map(component: Stat, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Icon if present
        component.icon?.let { icon ->
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to icon, "size" to 24f),
                modifiers = listOf(SwiftUIModifier.foregroundColor(SwiftUIColor.primary))
            ))
        }

        // Value
        children.add(SwiftUIView.text(
            content = component.value,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.LargeTitle),
                SwiftUIModifier.fontWeight(FontWeight.Bold)
            )
        ))

        // Label
        children.add(SwiftUIView.text(
            content = component.label,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
            )
        ))

        // Change indicator
        component.change?.let { change ->
            val changeColor = when (component.changeType) {
                Stat.ChangeType.Positive -> SwiftUIColor.system("systemGreen")
                Stat.ChangeType.Negative -> SwiftUIColor.system("systemRed")
                Stat.ChangeType.Neutral -> SwiftUIColor.system("label")
            }

            children.add(SwiftUIView.text(
                content = change,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(changeColor)
                )
            ))
        }

        // Description
        component.description?.let { desc ->
            children.add(SwiftUIView.text(
                content = desc,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption2),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("tertiaryLabel"))
                )
            ))
        }

        val modifiers = mutableListOf(
            SwiftUIModifier.padding(16f)
        )

        if (component.elevated) {
            modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")))
            modifiers.add(SwiftUIModifier.cornerRadius(12f))
            modifiers.add(SwiftUIModifier.shadow(radius = 4f, x = 0f, y = 2f))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// KPI
// ============================================

object KPIMapper {
    fun map(component: KPI, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Header with icon
        val headerChildren = mutableListOf<SwiftUIView>()
        headerChildren.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Subheadline),
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
            )
        ))

        component.icon?.let { icon ->
            headerChildren.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to icon, "size" to 16f),
                modifiers = listOf(SwiftUIModifier.foregroundColor(SwiftUIColor.system("tertiaryLabel")))
            ))
        }

        children.add(SwiftUIView(
            type = ViewType.HStack,
            properties = emptyMap(),
            children = headerChildren
        ))

        // Value
        children.add(SwiftUIView.text(
            content = component.value,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.LargeTitle),
                SwiftUIModifier.fontWeight(FontWeight.Bold)
            )
        ))

        // Target
        component.target?.let { target ->
            children.add(SwiftUIView.text(
                content = "Target: $target",
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            ))
        }

        // Progress bar
        if (component.showProgressBar && component.progress != null) {
            children.add(SwiftUIView(
                type = ViewType.Custom("ProgressView"),
                properties = mapOf("value" to component.progress!!),
                modifiers = listOf(SwiftUIModifier(ModifierType.Custom, mapOf("progressViewStyle" to "linear")))
            ))
        }

        // Subtitle
        component.subtitle?.let { subtitle ->
            children.add(SwiftUIView.text(
                content = subtitle,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption2),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("tertiaryLabel"))
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(16f),
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                SwiftUIModifier.cornerRadius(12f)
            ),
        )
    }
}

// ============================================
// METRIC CARD
// ============================================

object MetricCardMapper {
    fun map(component: MetricCard, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Title
        children.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Subheadline),
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
            )
        ))

        // Value with unit
        val valueText = if (component.unit != null) "${component.value} ${component.unit}" else component.value
        children.add(SwiftUIView.text(
            content = valueText,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Title),
                SwiftUIModifier.fontWeight(FontWeight.Bold)
            )
        ))

        // Description
        component.contentDescription?.let { desc ->
            children.add(SwiftUIView.text(
                content = desc,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("tertiaryLabel"))
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(16f),
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                SwiftUIModifier.cornerRadius(12f)
            ),
        )
    }
}

// ============================================
// LEADERBOARD
// ============================================

object LeaderboardMapper {
    fun map(component: Leaderboard, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Title
        component.title?.let { title ->
            children.add(SwiftUIView.text(
                content = title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                )
            ))
        }

        // Items
        component.items.forEach { entry ->
            val entryChildren = mutableListOf<SwiftUIView>()

            // Rank badge
            entryChildren.add(SwiftUIView.text(
                content = entry.rank.toString(),
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.fontWeight(FontWeight.Bold),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("white")),
                    SwiftUIModifier.padding(8f),
                    SwiftUIModifier.background(SwiftUIColor.primary),
                    SwiftUIModifier.cornerRadius(8f)
                )
            ))

            // Name and score
            entryChildren.add(SwiftUIView(
                type = ViewType.VStack,
                properties = mapOf("alignment" to "leading", "spacing" to 2f),
                children = listOf(
                    SwiftUIView.text(
                        content = entry.name,
                        modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
                    ),
                    SwiftUIView.text(
                        content = "Score: ${entry.score}",
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Caption),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                        )
                    )
                )
            ))

            children.add(SwiftUIView(
                type = ViewType.HStack,
                properties = mapOf("spacing" to 12f),
                children = entryChildren,
                modifiers = listOf(
                    SwiftUIModifier.padding(12f),
                    SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                    SwiftUIModifier.cornerRadius(8f)
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = children,
        )
    }
}

// ============================================
// RANKING
// ============================================

object RankingMapper {
    fun map(component: Ranking, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Position badge
        children.add(SwiftUIView.text(
            content = "#${component.position}",
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Title2),
                SwiftUIModifier.fontWeight(FontWeight.Bold),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))

        // Change indicator
        component.change?.let { change ->
            val changeText = if (change > 0) "+$change" else "$change"
            children.add(SwiftUIView.text(
                content = changeText,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(
                        if (change > 0) SwiftUIColor.system("systemGreen") else SwiftUIColor.system("systemRed")
                    )
                )
            ))
        }

        // Label
        component.label?.let { label ->
            children.add(SwiftUIView.text(
                content = label,
                modifiers = listOf(SwiftUIModifier.font(FontStyle.Subheadline))
            ))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("spacing" to 8f),
            children = children,
        )
    }
}

// ============================================
// ZOOM
// ============================================

object ZoomMapper {
    fun map(component: Zoom, theme: Theme?): SwiftUIView {
        // Zoom implemented as interactive image view with zoom controls
        return SwiftUIView(
            type = ViewType.Custom("AsyncImage"),
            properties = mapOf(
                "url" to component.imageUrl,
                "contentMode" to "fit"
            ),
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf(
                    "scaleEffect" to component.initialScale,
                    "gesture" to "magnification" // Pinch to zoom
                ))
            ),
        )
    }
}

// ============================================
// VIRTUAL SCROLL
// ============================================

object VirtualScrollMapper {
    fun map(component: VirtualScroll, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        // VirtualScroll uses onItemRender callback to generate items
        // For SwiftUI, we'll create placeholders or use the itemCount
        val itemViews = if (component.onItemRender != null) {
            // Generate items using the callback
            (0 until component.itemCount).mapNotNull { index ->
                component.onItemRender?.invoke(index)?.let { renderChild(it) }
            }
        } else {
            // No items to render
            emptyList()
        }

        val spacing = 8f // Default spacing

        return SwiftUIView(
            type = ViewType.Custom("ScrollView"),
            properties = emptyMap(),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Custom("LazyVStack"),
                    properties = mapOf("spacing" to spacing),
                    children = itemViews
                )
            ),
        )
    }
}

// ============================================
// INFINITE SCROLL
// ============================================

object InfiniteScrollMapper {
    fun map(component: InfiniteScroll, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Render items from component.items (List<Component>)
        component.items.forEach { child ->
            children.add(renderChild(child))
        }

        // Loading indicator at bottom
        if (component.loading) {
            children.add(SwiftUIView(
                type = ViewType.Custom("ProgressView"),
                properties = emptyMap(),
                modifiers = listOf(SwiftUIModifier.padding(16f))
            ))
        }

        // End message if no more items
        if (!component.hasMore && !component.loading) {
            component.endMessageText?.let { message ->
                children.add(SwiftUIView.text(
                    content = message,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel")),
                        SwiftUIModifier.padding(16f)
                    )
                ))
            }
        }

        return SwiftUIView(
            type = ViewType.Custom("ScrollView"),
            properties = emptyMap(),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Custom("LazyVStack"),
                    properties = mapOf("spacing" to 8f),
                    children = children
                )
            ),
        )
    }
}

// ============================================
// QR CODE
// ============================================

object QRCodeMapper {
    fun map(component: QRCode, theme: Theme?): SwiftUIView {
        // QR Code via CoreImage filter
        return SwiftUIView(
            type = ViewType.Image,
            properties = mapOf(
                "qrData" to component.data,
                "size" to component.size
            ),
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("interpolation" to "none"))
            ),
        )
    }
}

// ============================================
// RICH TEXT
// ============================================

object RichTextMapper {
    fun map(component: RichText, theme: Theme?): SwiftUIView {
        // Rich text with AttributedString
        // For now, concatenate spans' text content
        val text = component.spans.joinToString("") { it.text }
        return SwiftUIView.text(
            content = text,
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("textSelection" to "enabled"))
            )
        )
    }
}
