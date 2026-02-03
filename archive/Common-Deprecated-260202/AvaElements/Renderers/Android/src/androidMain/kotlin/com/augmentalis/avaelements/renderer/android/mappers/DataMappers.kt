package com.augmentalis.avaelements.renderer.android.mappers

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.augmentalis.avaelements.flutter.material.data.*
import com.augmentalis.avaelements.renderer.android.IconFromString
import kotlinx.coroutines.flow.collect

/**
 * Android Compose mappers for data display components
 *
 * This file contains renderer functions that map cross-platform data component models
 * to Material3 Compose implementations on Android.
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render DataList component using Material3
 *
 * Maps DataList component to Material3 Column with structured data display.
 */
@Composable
fun DataListMapper(component: DataList) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = component.contentDescription ?: "Data list" }
    ) {
        component.title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        when (component.layout) {
            DataList.Layout.Stacked -> {
                Column(verticalArrangement = Arrangement.spacedBy(if (component.dense) 4.dp else 8.dp)) {
                    component.items.forEachIndexed { index, item ->
                        Column {
                            Text(text = item.key, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = item.value, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (component.showDividers && index < component.items.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
            DataList.Layout.Inline -> {
                Column(verticalArrangement = Arrangement.spacedBy(if (component.dense) 4.dp else 8.dp)) {
                    component.items.forEachIndexed { index, item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = item.key, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(text = item.value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                        if (component.showDividers && index < component.items.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
            DataList.Layout.Grid -> {
                Column(verticalArrangement = Arrangement.spacedBy(if (component.dense) 4.dp else 8.dp)) {
                    component.items.chunked(2).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowItems.forEach { item ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.key, style = MaterialTheme.typography.labelMedium)
                                    Text(text = item.value, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render DescriptionList component using Material3
 */
@Composable
fun DescriptionListMapper(component: DescriptionList) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.contentDescription ?: "Description list" }) {
        component.title?.let { Text(text = it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp)) }
        Column(verticalArrangement = Arrangement.spacedBy(if (component.dense) 4.dp else 8.dp)) {
            component.items.forEachIndexed { index, item ->
                var expanded by remember { mutableStateOf(component.defaultExpanded) }
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (component.expandable) Modifier.clickable { expanded = !expanded } else Modifier)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (component.numbered) Text(text = "${index + 1}.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            item.icon?.let { IconFromString(it, null, Modifier.size(20.dp)) }
                            Text(text = item.term, style = MaterialTheme.typography.titleSmall)
                            item.badge?.let { Badge { Text(it) } }
                        }
                        if (component.expandable) Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                    if (!component.expandable || expanded) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = if (component.numbered) 24.dp else 0.dp, top = 4.dp, bottom = 8.dp)
                        )
                    }
                }
                if (index < component.items.size - 1) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

/**
 * Render StatGroup component using Material3
 */
@Composable
fun StatGroupMapper(component: StatGroup) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.contentDescription ?: "Statistics group" }) {
        component.title?.let { Text(text = it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp)) }
        when (component.layout) {
            StatGroup.Layout.Horizontal -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                component.stats.forEach { stat -> Box(modifier = Modifier.weight(1f)) { StatCard(stat) } }
            }
            StatGroup.Layout.Vertical -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                component.stats.forEach { stat -> StatCard(stat) }
            }
            StatGroup.Layout.Grid -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                component.stats.chunked(2).forEach { rowStats ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowStats.forEach { stat -> Box(modifier = Modifier.weight(1f)) { StatCard(stat) } }
                        if (rowStats.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(stat: StatGroup.StatItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                stat.icon?.let { IconFromString(it, null, Modifier.size(20.dp)) }
                Text(text = stat.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stat.value, style = MaterialTheme.typography.headlineMedium)
            stat.change?.let { change ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    val changeColor = when (stat.changeType) {
                        StatGroup.ChangeType.Positive -> MaterialTheme.colorScheme.primary
                        StatGroup.ChangeType.Negative -> MaterialTheme.colorScheme.error
                        StatGroup.ChangeType.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(
                        imageVector = when (stat.changeType) {
                            StatGroup.ChangeType.Positive -> Icons.Default.KeyboardArrowUp
                            StatGroup.ChangeType.Negative -> Icons.Default.KeyboardArrowDown
                            StatGroup.ChangeType.Neutral -> Icons.Default.MoreHoriz
                        },
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(text = change, style = MaterialTheme.typography.bodySmall, color = changeColor)
                }
            }
        }
    }
}

/**
 * Render Stat component using Material3
 */
@Composable
fun StatMapper(component: Stat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onClick != null) Modifier.clickable { component.onClick.invoke() } else Modifier)
            .semantics { contentDescription = component.contentDescription ?: "Statistic: ${component.label}" },
        elevation = if (component.elevated) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                component.icon?.let { IconFromString(it, null, Modifier.size(24.dp)) }
                Text(text = component.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = component.value, style = MaterialTheme.typography.displaySmall)
            component.change?.let { change ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    val changeColor = when (component.changeType) {
                        Stat.ChangeType.Positive -> MaterialTheme.colorScheme.primary
                        Stat.ChangeType.Negative -> MaterialTheme.colorScheme.error
                        Stat.ChangeType.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(
                        imageVector = when (component.changeType) {
                            Stat.ChangeType.Positive -> Icons.Default.KeyboardArrowUp
                            Stat.ChangeType.Negative -> Icons.Default.KeyboardArrowDown
                            Stat.ChangeType.Neutral -> Icons.Default.MoreHoriz
                        },
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = change, style = MaterialTheme.typography.titleMedium, color = changeColor)
                }
            }
            component.description?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/**
 * Render KPI component using Material3
 */
@Composable
fun KPIMapper(component: KPI) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onClick != null) Modifier.clickable { component.onClick.invoke() } else Modifier)
            .semantics { contentDescription = component.contentDescription ?: "KPI: ${component.title}" }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    component.icon?.let { IconFromString(it, null, Modifier.size(24.dp)) }
                    Text(text = component.title, style = MaterialTheme.typography.titleMedium)
                }
                Icon(
                    imageVector = when (component.trend) {
                        KPI.TrendType.Up -> Icons.Default.KeyboardArrowUp
                        KPI.TrendType.Down -> Icons.Default.KeyboardArrowDown
                        KPI.TrendType.Neutral -> Icons.Default.MoreHoriz
                    },
                    contentDescription = "Trend: ${component.trend}",
                    tint = when (component.trend) {
                        KPI.TrendType.Up -> MaterialTheme.colorScheme.primary
                        KPI.TrendType.Down -> MaterialTheme.colorScheme.error
                        KPI.TrendType.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = component.value, style = MaterialTheme.typography.displayMedium)
            component.subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            component.target?.let {
                Text(text = "Target: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            component.progress?.let { progress ->
                if (component.showProgressBar) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
                    Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/**
 * Render Leaderboard component using Material3
 */
@Composable
fun LeaderboardMapper(component: Leaderboard) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.contentDescription ?: "Leaderboard" }) {
        component.title?.let { Text(text = it, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp)) }
        Card {
            Column {
                component.getDisplayItems().forEachIndexed { index, item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { component.onItemClick?.invoke(item.id) },
                        color = if (component.isCurrentUser(item)) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val showBadge = component.showTopBadges && item.rank <= 3
                            if (showBadge) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (item.rank) {
                                        1 -> Color(0xFFFFD700)
                                        2 -> Color(0xFFC0C0C0)
                                        3 -> Color(0xFFCD7F32)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = item.rank.toString(), style = MaterialTheme.typography.labelLarge, color = Color.Black)
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "${item.rank}", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            item.avatar?.let {
                                AsyncImage(model = it, contentDescription = "Avatar", modifier = Modifier.size(40.dp).clip(CircleShape))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                                item.subtitle?.let {
                                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(text = item.score, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (index < component.getDisplayItems().size - 1) HorizontalDivider()
                }
            }
        }
    }
}

/**
 * Render Ranking component using Material3
 */
@Composable
fun RankingMapper(component: Ranking) {
    val badgeType = component.getBadgeType()
    val badgeColor = badgeType?.let {
        when (it) {
            Ranking.BadgeType.Gold -> Color(0xFFFFD700)
            Ranking.BadgeType.Silver -> Color(0xFFC0C0C0)
            Ranking.BadgeType.Bronze -> Color(0xFFCD7F32)
        }
    } ?: MaterialTheme.colorScheme.secondaryContainer

    when (component.size) {
        Ranking.Size.Small -> Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(24.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = component.position.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (badgeType != null) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Ranking.Size.Medium -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = component.position.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (badgeType != null) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            component.change?.let {
                Text(text = kotlin.math.abs(it).toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
        Ranking.Size.Large -> Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                component.label?.let { Text(text = it, style = MaterialTheme.typography.labelMedium) }
                Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = component.position.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (badgeType != null) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Text(text = component.getOrdinal(), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/**
 * Render VirtualScroll component using Material3 LazyColumn
 */
@Composable
fun VirtualScrollMapper(component: VirtualScroll) {
    val backgroundColor = component.backgroundColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: Color.Transparent

    Box(
        modifier = Modifier
            .background(backgroundColor)
            .semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        when (component.orientation) {
            VirtualScroll.Orientation.Vertical -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = component.contentPadding?.let {
                        PaddingValues(it.left.dp, it.top.dp, it.right.dp, it.bottom.dp)
                    } ?: PaddingValues(0.dp),
                    reverseLayout = component.reverseLayout
                ) {
                    items(component.itemCount) { index ->
                        Box(
                            modifier = component.itemHeight?.let { Modifier.height(it.dp) } ?: Modifier
                        ) {
                            // TODO: Render item from onItemRender callback
                            Text("Item $index", modifier = Modifier.padding(16.dp))
                        }

                        // Trigger onScrolledToEnd
                        if (index == component.itemCount - 1) {
                            LaunchedEffect(Unit) {
                                component.onScrolledToEnd?.invoke()
                            }
                        }
                    }
                }
            }
            VirtualScroll.Orientation.Horizontal -> {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = component.contentPadding?.let {
                        PaddingValues(it.left.dp, it.top.dp, it.right.dp, it.bottom.dp)
                    } ?: PaddingValues(0.dp),
                    reverseLayout = component.reverseLayout
                ) {
                    items(component.itemCount) { index ->
                        Box(
                            modifier = component.itemWidth?.let { Modifier.width(it.dp) } ?: Modifier
                        ) {
                            Text("Item $index", modifier = Modifier.padding(16.dp))
                        }

                        if (index == component.itemCount - 1) {
                            LaunchedEffect(Unit) {
                                component.onScrolledToEnd?.invoke()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render InfiniteScroll component using Material3 LazyColumn with load more
 */
@Composable
fun InfiniteScrollMapper(component: InfiniteScroll) {
    val backgroundColor = component.backgroundColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: Color.Transparent
    val listState = rememberLazyListState()

    // Detect when scrolled near bottom
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= component.items.size - 3 &&
                    component.hasMore &&
                    !component.loading &&
                    !component.showError
                ) {
                    component.onLoadMore?.invoke()
                }
            }
    }

    Box(
        modifier = Modifier
            .background(backgroundColor)
            .semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        when (component.orientation) {
            InfiniteScroll.Orientation.Vertical -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = component.contentPadding?.let {
                        PaddingValues(it.left.dp, it.top.dp, it.right.dp, it.bottom.dp)
                    } ?: PaddingValues(0.dp)
                ) {
                    items(component.items.size) { index ->
                        // TODO: Render component.items[index]
                        Text("Item $index", modifier = Modifier.padding(16.dp))
                    }

                    // Footer
                    item {
                        InfiniteScrollFooter(component)
                    }
                }
            }
            InfiniteScroll.Orientation.Horizontal -> {
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = component.contentPadding?.let {
                        PaddingValues(it.left.dp, it.top.dp, it.right.dp, it.bottom.dp)
                    } ?: PaddingValues(0.dp)
                ) {
                    items(component.items.size) { index ->
                        Text("Item $index", modifier = Modifier.padding(16.dp))
                    }

                    item {
                        InfiniteScrollFooter(component)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfiniteScrollFooter(component: InfiniteScroll) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (component.getFooterState()) {
            InfiniteScroll.FooterState.Loading -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(
                        text = component.loadingIndicatorText ?: "Loading...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            InfiniteScroll.FooterState.End -> {
                Text(
                    text = component.endMessageText ?: "No more items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            InfiniteScroll.FooterState.Error -> {
                TextButton(onClick = { component.onRetry?.invoke() }) {
                    Text(component.errorMessageText ?: "Failed to load. Tap to retry.")
                }
            }
            InfiniteScroll.FooterState.None -> {
                // No footer
            }
        }
    }
}
