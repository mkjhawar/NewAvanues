package com.augmentalis.avaelements.renderer.android.mappers.display

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.avanues.avamagic.ui.core.display.*
import com.augmentalis.avanues.avamagic.ui.core.layout.*
import com.augmentalis.avanues.avamagic.ui.core.feedback.*
import com.augmentalis.avanues.avamagic.ui.core.data.TableComponent
import com.augmentalis.avanues.avamagic.components.core.Position
import com.augmentalis.avanues.avamagic.components.core.Severity
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter
import com.augmentalis.avaelements.renderer.android.IconResolver
import com.augmentalis.avaelements.renderer.android.toComposeColor

class ListTileMapper : ComponentMapper<ListTileComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ListTileComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val onClick = component.onClick
            ListItem(
                headlineContent = { Text(component.title) },
                supportingContent = component.subtitle?.let { { Text(it) } },
                leadingContent = component.leading?.let {
                    {
                        val composable = renderer.render(it) as @Composable () -> Unit
                        composable()
                    }
                },
                trailingContent = component.trailing?.let {
                    {
                        val composable = renderer.render(it) as @Composable () -> Unit
                        composable()
                    }
                },
                modifier = modifierConverter.convert(component.modifiers)
                    .then(if (onClick != null && component.enabled) {
                        Modifier.clickable { onClick.invoke() }
                    } else Modifier)
            )
        }
    }
}

class TabBarMapper : ComponentMapper<TabBarComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: TabBarComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            if (component.scrollable) {
                ScrollableTabRow(
                    selectedTabIndex = component.selectedIndex,
                    modifier = modifierConverter.convert(component.modifiers)
                ) {
                    component.tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == component.selectedIndex,
                            onClick = { component.onTabSelected?.invoke(index) },
                            text = { Text(tab.label) },
                            enabled = tab.enabled
                        )
                    }
                }
            } else {
                TabRow(
                    selectedTabIndex = component.selectedIndex,
                    modifier = modifierConverter.convert(component.modifiers)
                ) {
                    component.tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == component.selectedIndex,
                            onClick = { component.onTabSelected?.invoke(index) },
                            text = { Text(tab.label) },
                            enabled = tab.enabled
                        )
                    }
                }
            }
        }
    }
}

class CircularProgressMapper : ComponentMapper<CircularProgressComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: CircularProgressComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val progress = component.progress
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = modifierConverter.convert(component.modifiers).size(component.size.dp),
                    color = component.color?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                    strokeWidth = component.strokeWidth.dp
                )
            } else {
                CircularProgressIndicator(
                    modifier = modifierConverter.convert(component.modifiers).size(component.size.dp),
                    color = component.color?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                    strokeWidth = component.strokeWidth.dp
                )
            }
        }
    }
}

class TooltipMapper : ComponentMapper<TooltipComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: TooltipComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val tooltipState = rememberTooltipState()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(component.message)
                    }
                },
                state = tooltipState,
                modifier = modifierConverter.convert(component.modifiers)
            ) {
                val child = component.child
                if (child != null) {
                    val composable = renderer.render(child) as @Composable () -> Unit
                    composable()
                }
            }
        }
    }
}

class SkeletonMapper : ComponentMapper<SkeletonComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: SkeletonComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val shimmerColor = Color.LightGray.copy(alpha = 0.3f)
            val baseModifier = modifierConverter.convert(component.modifiers)

            when (component.variant) {
                "circular" -> {
                    Box(
                        modifier = baseModifier
                            .size(component.width?.dp ?: 40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(shimmerColor)
                    )
                }
                "rectangular" -> {
                    Box(
                        modifier = baseModifier
                            .width(component.width?.dp ?: 200.dp)
                            .height(component.height?.dp ?: 100.dp)
                            .background(shimmerColor)
                    )
                }
                "rounded" -> {
                    Box(
                        modifier = baseModifier
                            .width(component.width?.dp ?: 200.dp)
                            .height(component.height?.dp ?: 100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerColor)
                    )
                }
                else -> { // text
                    Box(
                        modifier = baseModifier
                            .width(component.width?.dp ?: 200.dp)
                            .height(component.height?.dp ?: 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }
    }
}

class SpinnerMapper : ComponentMapper<SpinnerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: SpinnerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            CircularProgressIndicator(
                modifier = modifierConverter.convert(component.modifiers).size(component.size.dp),
                color = component.color?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
                strokeWidth = (component.size / 10).dp
            )
        }
    }
}

class GridMapper : ComponentMapper<GridComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: GridComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            LazyVerticalGrid(
                columns = GridCells.Fixed(component.columns),
                modifier = modifierConverter.convert(component.modifiers),
                horizontalArrangement = Arrangement.spacedBy(component.spacing.dp),
                verticalArrangement = Arrangement.spacedBy(component.spacing.dp)
            ) {
                items(component.children) { child ->
                    val composable = renderer.render(child) as @Composable () -> Unit
                    composable()
                }
            }
        }
    }
}

class StackMapper : ComponentMapper<StackComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: StackComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Box(
                modifier = modifierConverter.convert(component.modifiers),
                contentAlignment = when (component.alignment) {
                    "center" -> Alignment.Center
                    "topStart" -> Alignment.TopStart
                    "topCenter" -> Alignment.TopCenter
                    "topEnd" -> Alignment.TopEnd
                    "centerStart" -> Alignment.CenterStart
                    "centerEnd" -> Alignment.CenterEnd
                    "bottomStart" -> Alignment.BottomStart
                    "bottomCenter" -> Alignment.BottomCenter
                    "bottomEnd" -> Alignment.BottomEnd
                    else -> Alignment.TopStart
                }
            ) {
                component.children.forEach { child ->
                    val composable = renderer.render(child) as @Composable () -> Unit
                    composable()
                }
            }
        }
    }
}

class PaginationMapper : ComponentMapper<PaginationComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: PaginationComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Row(
                modifier = modifierConverter.convert(component.modifiers),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                IconButton(
                    onClick = {
                        if (component.currentPage > 1) {
                            component.onPageChange?.invoke(component.currentPage - 1)
                        }
                    },
                    enabled = component.currentPage > 1
                ) {
                    Text("<")
                }

                // Page numbers
                val startPage = maxOf(1, component.currentPage - 2)
                val endPage = minOf(component.totalPages, component.currentPage + 2)

                for (page in startPage..endPage) {
                    val isSelected = page == component.currentPage
                    FilledTonalButton(
                        onClick = { component.onPageChange?.invoke(page) },
                        colors = if (isSelected) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(page.toString())
                    }
                }

                // Next button
                IconButton(
                    onClick = {
                        if (component.currentPage < component.totalPages) {
                            component.onPageChange?.invoke(component.currentPage + 1)
                        }
                    },
                    enabled = component.currentPage < component.totalPages
                ) {
                    Text(">")
                }
            }
        }
    }
}

class StatCardMapper : ComponentMapper<StatCardComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: StatCardComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Card(
                modifier = modifierConverter.convert(component.modifiers)
                    .then(if (component.clickable && component.onClick != null) {
                        Modifier.clickable { component.onClick!!.invoke() }
                    } else Modifier),
                colors = CardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        component.icon?.let { iconName ->
                            Icon(
                                imageVector = IconResolver.resolve(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = component.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = component.value,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    if (component.hasTrend) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val trendColor = when (component.trend) {
                                TrendDirection.Up -> Color(0xFF4CAF50)
                                TrendDirection.Down -> Color(0xFFF44336)
                                TrendDirection.Neutral -> Color(0xFF9E9E9E)
                            }
                            Icon(
                                imageVector = IconResolver.resolve(
                                    when (component.trend) {
                                        TrendDirection.Up -> "trending_up"
                                        TrendDirection.Down -> "trending_down"
                                        TrendDirection.Neutral -> "remove"
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = trendColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = component.formattedTrend,
                                style = MaterialTheme.typography.bodySmall,
                                color = trendColor
                            )
                        }
                    }

                    component.subtitle?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

class FABMapper : ComponentMapper<FABComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: FABComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            if (component.extended && !component.label.isNullOrBlank()) {
                ExtendedFloatingActionButton(
                    onClick = { component.onClick?.invoke() },
                    icon = { Icon(IconResolver.resolve(component.icon), contentDescription = null) },
                    text = { Text(component.label!!) },
                    modifier = modifierConverter.convert(component.modifiers)
                )
            } else {
                when (component.size) {
                    com.augmentalis.avanues.avamagic.components.core.ComponentSize.SM -> {
                        SmallFloatingActionButton(
                            onClick = { component.onClick?.invoke() },
                            modifier = modifierConverter.convert(component.modifiers)
                        ) {
                            Icon(IconResolver.resolve(component.icon), contentDescription = component.label)
                        }
                    }
                    com.augmentalis.avanues.avamagic.components.core.ComponentSize.LG -> {
                        LargeFloatingActionButton(
                            onClick = { component.onClick?.invoke() },
                            modifier = modifierConverter.convert(component.modifiers)
                        ) {
                            Icon(
                                IconResolver.resolve(component.icon),
                                contentDescription = component.label,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    else -> {
                        FloatingActionButton(
                            onClick = { component.onClick?.invoke() },
                            modifier = modifierConverter.convert(component.modifiers)
                        ) {
                            Icon(IconResolver.resolve(component.icon), contentDescription = component.label)
                        }
                    }
                }
            }
        }
    }
}

class StickyHeaderMapper : ComponentMapper<StickyHeaderComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: StickyHeaderComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Surface(
                modifier = modifierConverter.convert(component.modifiers),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = component.content,
                        style = MaterialTheme.typography.titleMedium
                    )
                    component.child?.let {
                        Spacer(Modifier.height(8.dp))
                        val composable = renderer.render(it) as @Composable () -> Unit
                        composable()
                    }
                }
            }
        }
    }
}

class MasonryGridMapper : ComponentMapper<MasonryGridComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: MasonryGridComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            LazyVerticalGrid(
                columns = GridCells.Fixed(component.columns),
                modifier = modifierConverter.convert(component.modifiers),
                horizontalArrangement = Arrangement.spacedBy(component.gap.dp),
                verticalArrangement = Arrangement.spacedBy(component.gap.dp)
            ) {
                items(component.children.size) { index ->
                    val child = component.children[index]
                    val composable = renderer.render(child) as @Composable () -> Unit
                    composable()
                }
            }
        }
    }
}

class ProgressCircleMapper : ComponentMapper<ProgressCircleComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ProgressCircleComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val size = when (component.size) {
                com.augmentalis.avanues.avamagic.components.core.ComponentSize.SM -> 24.dp
                com.augmentalis.avanues.avamagic.components.core.ComponentSize.MD -> 40.dp
                com.augmentalis.avanues.avamagic.components.core.ComponentSize.LG -> 56.dp
                else -> 40.dp
            }

            if (component.indeterminate) {
                CircularProgressIndicator(
                    modifier = modifierConverter.convert(component.modifiers).size(size),
                    strokeWidth = component.strokeWidth.dp,
                    color = component.color.toComposeColor()
                )
            } else {
                CircularProgressIndicator(
                    progress = { component.value / component.max },
                    modifier = modifierConverter.convert(component.modifiers).size(size),
                    strokeWidth = component.strokeWidth.dp,
                    color = component.color.toComposeColor()
                )
            }
        }
    }
}

class BannerMapper : ComponentMapper<BannerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: BannerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val backgroundColor = when (component.severity) {
                Severity.INFO -> Color(0xFFE3F2FD)
                Severity.SUCCESS -> Color(0xFFE8F5E9)
                Severity.WARNING -> Color(0xFFFFF8E1)
                Severity.ERROR -> Color(0xFFFFEBEE)
            }

            val contentColor = when (component.severity) {
                Severity.INFO -> Color(0xFF1976D2)
                Severity.SUCCESS -> Color(0xFF388E3C)
                Severity.WARNING -> Color(0xFFF57C00)
                Severity.ERROR -> Color(0xFFD32F2F)
            }

            Surface(
                modifier = modifierConverter.convert(component.modifiers).fillMaxWidth(),
                color = backgroundColor
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    component.icon?.let { iconName ->
                        Icon(
                            imageVector = IconResolver.resolve(iconName),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    Text(
                        text = component.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )

                    if (component.dismissible) {
                        IconButton(
                            onClick = { component.onDismiss?.invoke() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = IconResolver.resolve("close"),
                                contentDescription = "Dismiss",
                                tint = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

class NotificationCenterMapper : ComponentMapper<NotificationCenterComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: NotificationCenterComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(
                modifier = modifierConverter.convert(component.modifiers),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                component.displayedNotifications.forEach { notification ->
                    val backgroundColor = when (notification.severity) {
                        Severity.INFO -> Color(0xFFE3F2FD)
                        Severity.SUCCESS -> Color(0xFFE8F5E9)
                        Severity.WARNING -> Color(0xFFFFF8E1)
                        Severity.ERROR -> Color(0xFFFFEBEE)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = backgroundColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                notification.title?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                Text(
                                    text = notification.message,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (notification.dismissible) {
                                IconButton(
                                    onClick = { component.onDismiss?.invoke(notification.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = IconResolver.resolve("close"),
                                        contentDescription = "Dismiss",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class TableMapper : ComponentMapper<TableComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: TableComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(modifier = modifierConverter.convert(component.modifiers)) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    component.headers.forEach { header ->
                        Text(
                            text = header,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (component.bordered) {
                    HorizontalDivider()
                }

                // Data rows
                component.rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (component.striped && index % 2 == 1) {
                                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                } else Modifier
                            )
                            .padding(12.dp)
                    ) {
                        row.forEach { cell ->
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (component.bordered && index < component.rows.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
