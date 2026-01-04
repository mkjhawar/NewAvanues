package com.augmentalis.magicelements.renderer.android.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.IconResolver
import com.augmentalis.avaelements.renderer.android.ModifierConverter
import com.augmentalis.avaelements.renderer.android.toComposeColor
import com.augmentalis.magicui.components.core.Position
import com.augmentalis.magicui.components.core.Severity
import com.augmentalis.magicui.ui.core.data.*
import com.augmentalis.magicui.ui.core.display.*
import com.augmentalis.magicui.ui.core.layout.*
import com.augmentalis.magicui.ui.core.navigation.TabsComponent
import kotlinx.coroutines.launch

/**
 * Layout & Display Component Extensions
 *
 * Extension functions for rendering advanced layout and display MagicUI components.
 * Converted from mapper pattern to extension pattern for improved performance and readability.
 *
 * Layout Components:
 * - Scaffold
 * - LazyColumn
 * - LazyRow
 * - Spacer
 * - Box
 * - Surface
 * - Drawer
 * - Divider
 * - Tabs
 *
 * Display Components:
 * - ListTile
 * - TabBar
 * - CircularProgress
 * - Tooltip
 * - Skeleton
 * - Spinner
 * - Grid
 * - Stack
 * - Pagination
 * - StatCard
 * - FAB
 * - StickyHeader
 * - MasonryGrid
 * - ProgressCircle
 * - Banner
 * - NotificationCenter
 * - Table
 *
 * Data Components:
 * - Accordion
 * - Carousel
 * - Timeline
 * - DataGrid
 * - DataTable
 * - List
 * - TreeView
 * - Chip
 * - Paper
 * - EmptyState
 */

// ==================== Layout Components ====================

/**
 * Render ScaffoldComponent to Material3 Scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Scaffold(
        modifier = modifierConverter.convert(modifiers),
        topBar = {
            topBar?.let {
                val composable = renderer.render(it) as @Composable () -> Unit
                composable()
            }
        },
        bottomBar = {
            bottomBar?.let {
                val composable = renderer.render(it) as @Composable () -> Unit
                composable()
            }
        },
        floatingActionButton = {
            floatingActionButton?.let {
                val composable = renderer.render(it) as @Composable () -> Unit
                composable()
            }
        },
        floatingActionButtonPosition = when (floatingActionButtonPosition) {
            FabPosition.START -> androidx.compose.material3.FabPosition.Start
            FabPosition.CENTER -> androidx.compose.material3.FabPosition.Center
            FabPosition.END -> androidx.compose.material3.FabPosition.End
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            val contentComposable = renderer.render(content) as @Composable () -> Unit
            contentComposable()
        }
    }
}

/**
 * Render LazyColumnComponent to Jetpack Compose LazyColumn
 */
@Composable
fun LazyColumnComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    LazyColumn(
        modifier = modifierConverter.convert(modifiers),
        reverseLayout = reverseLayout,
        verticalArrangement = when (verticalArrangement) {
            VerticalArrangement.Top -> Arrangement.Top
            VerticalArrangement.Bottom -> Arrangement.Bottom
            VerticalArrangement.Center -> Arrangement.Center
            VerticalArrangement.SpaceBetween -> Arrangement.SpaceBetween
            VerticalArrangement.SpaceAround -> Arrangement.SpaceAround
            VerticalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
        },
        horizontalAlignment = when (horizontalAlignment) {
            HorizontalAlignment.Start -> Alignment.Start
            HorizontalAlignment.Center -> Alignment.CenterHorizontally
            HorizontalAlignment.End -> Alignment.End
        }
    ) {
        items(items) { item ->
            val composable = renderer.render(item) as @Composable () -> Unit
            composable()
        }
    }
}

/**
 * Render LazyRowComponent to Jetpack Compose LazyRow
 */
@Composable
fun LazyRowComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    LazyRow(
        modifier = modifierConverter.convert(modifiers),
        reverseLayout = reverseLayout,
        horizontalArrangement = when (horizontalArrangement) {
            HorizontalArrangement.Start -> Arrangement.Start
            HorizontalArrangement.End -> Arrangement.End
            HorizontalArrangement.Center -> Arrangement.Center
            HorizontalArrangement.SpaceBetween -> Arrangement.SpaceBetween
            HorizontalArrangement.SpaceAround -> Arrangement.SpaceAround
            HorizontalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
        },
        verticalAlignment = when (verticalAlignment) {
            VerticalAlignment.Top -> Alignment.Top
            VerticalAlignment.Center -> Alignment.CenterVertically
            VerticalAlignment.Bottom -> Alignment.Bottom
        }
    ) {
        items(items) { item ->
            val composable = renderer.render(item) as @Composable () -> Unit
            composable()
        }
    }
}

/**
 * Render SpacerComponent to Jetpack Compose Spacer
 */
@Composable
fun SpacerComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    val sizeModifier = when {
        width != null && height != null ->
            Modifier.size(width!!.dp, height!!.dp)
        width != null ->
            Modifier.width(width!!.dp)
        height != null ->
            Modifier.height(height!!.dp)
        else ->
            Modifier
    }
    Spacer(modifier = modifierConverter.convert(modifiers).then(sizeModifier))
}

/**
 * Render BoxComponent to Jetpack Compose Box
 */
@Composable
fun BoxComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Box(
        modifier = modifierConverter.convert(modifiers),
        contentAlignment = when (contentAlignment) {
            ContentAlignment.TopStart -> Alignment.TopStart
            ContentAlignment.TopCenter -> Alignment.TopCenter
            ContentAlignment.TopEnd -> Alignment.TopEnd
            ContentAlignment.CenterStart -> Alignment.CenterStart
            ContentAlignment.Center -> Alignment.Center
            ContentAlignment.CenterEnd -> Alignment.CenterEnd
            ContentAlignment.BottomStart -> Alignment.BottomStart
            ContentAlignment.BottomCenter -> Alignment.BottomCenter
            ContentAlignment.BottomEnd -> Alignment.BottomEnd
        }
    ) {
        children.forEach { child ->
            val composable = renderer.render(child) as @Composable () -> Unit
            composable()
        }
    }
}

/**
 * Render SurfaceComponent to Material3 Surface
 */
@Composable
fun SurfaceComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Surface(
        modifier = modifierConverter.convert(modifiers),
        shape = when (shape) {
            Shape.Rectangle -> RoundedCornerShape(0.dp)
            Shape.RoundedSmall -> RoundedCornerShape(4.dp)
            Shape.RoundedMedium -> RoundedCornerShape(8.dp)
            Shape.RoundedLarge -> RoundedCornerShape(16.dp)
            Shape.Circle -> CircleShape
        },
        color = color?.toComposeColor() ?: MaterialTheme.colorScheme.surface,
        contentColor = contentColor?.toComposeColor() ?: MaterialTheme.colorScheme.onSurface,
        tonalElevation = tonalElevation.dp,
        shadowElevation = shadowElevation.dp
    ) {
        child?.let {
            val composable = renderer.render(it) as @Composable () -> Unit
            composable()
        }
    }
}

/**
 * Render DrawerComponent to Material3 ModalNavigationDrawer
 */
@Composable
fun DrawerComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    val drawerState = rememberDrawerState(
        initialValue = if (isOpen) DrawerValue.Open else DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(
                modifier = modifierConverter.convert(modifiers)
            ) {
                drawerContent?.let {
                    val composable = renderer.render(it) as @Composable () -> Unit
                    composable()
                }
            }
        }
    ) {
        content?.let {
            val composable = renderer.render(it) as @Composable () -> Unit
            composable()
        }
    }

    // Handle state changes
    if (isOpen && drawerState.isClosed) {
        scope.launch { drawerState.open() }
    } else if (!isOpen && drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
}

/**
 * Render DividerComponent to Material3 Divider
 */
@Composable
fun DividerComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    if (orientation == "vertical") {
        VerticalDivider(
            modifier = modifierConverter.convert(modifiers)
                .then(if (thickness != null) Modifier.width(thickness!!.dp) else Modifier),
            thickness = (thickness ?: 1f).dp,
            color = color?.toComposeColor() ?: MaterialTheme.colorScheme.outlineVariant
        )
    } else {
        HorizontalDivider(
            modifier = modifierConverter.convert(modifiers),
            thickness = (thickness ?: 1f).dp,
            color = color?.toComposeColor() ?: MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/**
 * Render TabsComponent to Material3 TabRow
 */
@Composable
fun TabsComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    var selectedIndex by remember { mutableStateOf(this.selectedIndex) }

    Column(modifier = modifierConverter.convert(modifiers)) {
        if (scrollable) {
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            onTabSelected?.invoke(index)
                        },
                        text = { Text(tab.label) },
                        icon = tab.icon?.let {
                            {
                                val iconComposable = renderer.render(it) as @Composable () -> Unit
                                iconComposable()
                            }
                        },
                        enabled = tab.enabled
                    )
                }
            }
        } else {
            TabRow(selectedTabIndex = selectedIndex) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            onTabSelected?.invoke(index)
                        },
                        text = { Text(tab.label) },
                        icon = tab.icon?.let {
                            {
                                val iconComposable = renderer.render(it) as @Composable () -> Unit
                                iconComposable()
                            }
                        },
                        enabled = tab.enabled
                    )
                }
            }
        }

        // Render the content of the selected tab
        tabs.getOrNull(selectedIndex)?.content?.let { content ->
            val contentComposable = renderer.render(content) as @Composable () -> Unit
            contentComposable()
        }
    }
}

// ==================== Display Components ====================

/**
 * Render ListTileComponent to Material3 ListItem
 */
@Composable
fun ListTileComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    val onClick = this.onClick

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = leading?.let {
            {
                val composable = renderer.render(it) as @Composable () -> Unit
                composable()
            }
        },
        trailingContent = trailing?.let {
            {
                val composable = renderer.render(it) as @Composable () -> Unit
                composable()
            }
        },
        modifier = modifierConverter.convert(modifiers)
            .then(if (onClick != null && enabled) {
                Modifier.clickable { onClick.invoke() }
            } else Modifier)
    )
}

/**
 * Render TabBarComponent to Material3 TabRow
 */
@Composable
fun TabBarComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    if (scrollable) {
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifierConverter.convert(modifiers)
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onTabSelected?.invoke(index) },
                    text = { Text(tab.label) },
                    enabled = tab.enabled
                )
            }
        }
    } else {
        TabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifierConverter.convert(modifiers)
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onTabSelected?.invoke(index) },
                    text = { Text(tab.label) },
                    enabled = tab.enabled
                )
            }
        }
    }
}

/**
 * Render CircularProgressComponent to Material3 CircularProgressIndicator
 */
@Composable
fun CircularProgressComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    if (progress != null) {
        CircularProgressIndicator(
            progress = { progress!! },
            modifier = modifierConverter.convert(modifiers).size(size.dp),
            color = color?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
            strokeWidth = strokeWidth.dp
        )
    } else {
        CircularProgressIndicator(
            modifier = modifierConverter.convert(modifiers).size(size.dp),
            color = color?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
            strokeWidth = strokeWidth.dp
        )
    }
}

/**
 * Render TooltipComponent to Material3 TooltipBox
 */
@Composable
fun TooltipComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    val tooltipState = rememberTooltipState()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(message)
            }
        },
        state = tooltipState,
        modifier = modifierConverter.convert(modifiers)
    ) {
        child?.let {
            val composable = renderer.render(it) as @Composable () -> Unit
            composable()
        }
    }
}

/**
 * Render SkeletonComponent to skeleton loading placeholder
 */
@Composable
fun SkeletonComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    val shimmerColor = Color.LightGray.copy(alpha = 0.3f)
    val baseModifier = modifierConverter.convert(modifiers)

    when (variant) {
        "circular" -> {
            Box(
                modifier = baseModifier
                    .size(width?.dp ?: 40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(shimmerColor)
            )
        }
        "rectangular" -> {
            Box(
                modifier = baseModifier
                    .width(width?.dp ?: 200.dp)
                    .height(height?.dp ?: 100.dp)
                    .background(shimmerColor)
            )
        }
        "rounded" -> {
            Box(
                modifier = baseModifier
                    .width(width?.dp ?: 200.dp)
                    .height(height?.dp ?: 100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerColor)
            )
        }
        else -> { // text
            Box(
                modifier = baseModifier
                    .width(width?.dp ?: 200.dp)
                    .height(height?.dp ?: 16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
        }
    }
}

/**
 * Render SpinnerComponent to Material3 CircularProgressIndicator
 */
@Composable
fun SpinnerComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    CircularProgressIndicator(
        modifier = modifierConverter.convert(modifiers).size(size.dp),
        color = color?.toComposeColor() ?: MaterialTheme.colorScheme.primary,
        strokeWidth = (size / 10).dp
    )
}

/**
 * Render GridComponent to LazyVerticalGrid
 */
@Composable
fun GridComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifierConverter.convert(modifiers),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        items(children) { child ->
            val composable = renderer.render(child) as @Composable () -> Unit
            composable()
        }
    }
}

/**
 * Render StackComponent to Box with alignment
 */
@Composable
fun StackComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Box(
        modifier = modifierConverter.convert(modifiers),
        contentAlignment = when (alignment) {
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
        children.forEach { child ->
            val composable = renderer.render(child) as @Composable () -> Unit
            composable()
        }
    }
}

/**
 * Render PaginationComponent to Material3 pagination controls
 */
@Composable
fun PaginationComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Row(
        modifier = modifierConverter.convert(modifiers),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        IconButton(
            onClick = {
                if (currentPage > 1) {
                    onPageChange?.invoke(currentPage - 1)
                }
            },
            enabled = currentPage > 1
        ) {
            Text("<")
        }

        // Page numbers
        val startPage = maxOf(1, currentPage - 2)
        val endPage = minOf(totalPages, currentPage + 2)

        for (page in startPage..endPage) {
            val isSelected = page == currentPage
            FilledTonalButton(
                onClick = { onPageChange?.invoke(page) },
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
                if (currentPage < totalPages) {
                    onPageChange?.invoke(currentPage + 1)
                }
            },
            enabled = currentPage < totalPages
        ) {
            Text(">")
        }
    }
}

/**
 * Render StatCardComponent to Material3 Card with stats
 */
@Composable
fun StatCardComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Card(
        modifier = modifierConverter.convert(modifiers)
            .then(if (clickable && onClick != null) {
                Modifier.clickable { onClick!!.invoke() }
            } else Modifier),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let { iconName ->
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )

            if (hasTrend) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val trendColor = when (trend) {
                        TrendDirection.Up -> Color(0xFF4CAF50)
                        TrendDirection.Down -> Color(0xFFF44336)
                        TrendDirection.Neutral -> Color(0xFF9E9E9E)
                    }
                    Icon(
                        imageVector = IconResolver.resolve(
                            when (trend) {
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
                        text = formattedTrend,
                        style = MaterialTheme.typography.bodySmall,
                        color = trendColor
                    )
                }
            }

            subtitle?.let {
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

/**
 * Render FABComponent to Material3 FloatingActionButton
 */
@Composable
fun FABComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    if (extended && !label.isNullOrBlank()) {
        ExtendedFloatingActionButton(
            onClick = { onClick?.invoke() },
            icon = { Icon(IconResolver.resolve(icon), contentDescription = null) },
            text = { Text(label!!) },
            modifier = modifierConverter.convert(modifiers)
        )
    } else {
        when (size) {
            com.augmentalis.avanues.avamagic.components.core.ComponentSize.SM -> {
                SmallFloatingActionButton(
                    onClick = { onClick?.invoke() },
                    modifier = modifierConverter.convert(modifiers)
                ) {
                    Icon(IconResolver.resolve(icon), contentDescription = label)
                }
            }
            com.augmentalis.avanues.avamagic.components.core.ComponentSize.LG -> {
                LargeFloatingActionButton(
                    onClick = { onClick?.invoke() },
                    modifier = modifierConverter.convert(modifiers)
                ) {
                    Icon(
                        IconResolver.resolve(icon),
                        contentDescription = label,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            else -> {
                FloatingActionButton(
                    onClick = { onClick?.invoke() },
                    modifier = modifierConverter.convert(modifiers)
                ) {
                    Icon(IconResolver.resolve(icon), contentDescription = label)
                }
            }
        }
    }
}

/**
 * Render StickyHeaderComponent to elevated Surface
 */
@Composable
fun StickyHeaderComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Surface(
        modifier = modifierConverter.convert(modifiers),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = content,
                style = MaterialTheme.typography.titleMedium
            )
            child?.let {
                Spacer(Modifier.height(8.dp))
                val composable = renderer.render(it) as @Composable () -> Unit
                composable()
            }
        }
    }
}

/**
 * Render MasonryGridComponent to LazyVerticalGrid
 */
@Composable
fun MasonryGridComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifierConverter.convert(modifiers),
        horizontalArrangement = Arrangement.spacedBy(gap.dp),
        verticalArrangement = Arrangement.spacedBy(gap.dp)
    ) {
        items(children.size) { index ->
            val child = children[index]
            val composable = renderer.render(child) as @Composable () -> Unit
            composable()
        }
    }
}

/**
 * Render ProgressCircleComponent to Material3 CircularProgressIndicator
 */
@Composable
fun ProgressCircleComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    val size = when (size) {
        com.augmentalis.avanues.avamagic.components.core.ComponentSize.SM -> 24.dp
        com.augmentalis.avanues.avamagic.components.core.ComponentSize.MD -> 40.dp
        com.augmentalis.avanues.avamagic.components.core.ComponentSize.LG -> 56.dp
        else -> 40.dp
    }

    if (indeterminate) {
        CircularProgressIndicator(
            modifier = modifierConverter.convert(modifiers).size(size),
            strokeWidth = strokeWidth.dp,
            color = color.toComposeColor()
        )
    } else {
        CircularProgressIndicator(
            progress = { value / max },
            modifier = modifierConverter.convert(modifiers).size(size),
            strokeWidth = strokeWidth.dp,
            color = color.toComposeColor()
        )
    }
}

/**
 * Render BannerComponent to Material3 Surface with banner styling
 */
@Composable
fun BannerComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    val backgroundColor = when (severity) {
        Severity.INFO -> Color(0xFFE3F2FD)
        Severity.SUCCESS -> Color(0xFFE8F5E9)
        Severity.WARNING -> Color(0xFFFFF8E1)
        Severity.ERROR -> Color(0xFFFFEBEE)
    }

    val contentColor = when (severity) {
        Severity.INFO -> Color(0xFF1976D2)
        Severity.SUCCESS -> Color(0xFF388E3C)
        Severity.WARNING -> Color(0xFFF57C00)
        Severity.ERROR -> Color(0xFFD32F2F)
    }

    Surface(
        modifier = modifierConverter.convert(modifiers).fillMaxWidth(),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let { iconName ->
                Icon(
                    imageVector = IconResolver.resolve(iconName),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            if (dismissible) {
                IconButton(
                    onClick = { onDismiss?.invoke() },
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

/**
 * Render NotificationCenterComponent to list of notification cards
 */
@Composable
fun NotificationCenterComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Column(
        modifier = modifierConverter.convert(modifiers),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        displayedNotifications.forEach { notification ->
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
                            onClick = { onDismiss?.invoke(notification.id) },
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

/**
 * Render TableComponent to Material3 data table
 */
@Composable
fun TableComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Column(modifier = modifierConverter.convert(modifiers)) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (bordered) {
            HorizontalDivider()
        }

        // Data rows
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (striped && index % 2 == 1) {
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

            if (bordered && index < rows.size - 1) {
                HorizontalDivider()
            }
        }
    }
}

// ==================== Data Components ====================

/**
 * Render AccordionComponent to expandable/collapsible panels
 */
@Composable
fun AccordionComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    var expandedIndices by remember { mutableStateOf(this.expandedIndices) }

    Column(modifier = modifierConverter.convert(modifiers)) {
        items.forEachIndexed { index, item ->
            val isExpanded = index in expandedIndices

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedIndices = if (isExpanded) {
                                    expandedIndices - index
                                } else {
                                    if (allowMultiple) {
                                        expandedIndices + index
                                    } else {
                                        setOf(index)
                                    }
                                }
                                onToggle?.invoke(index)
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = IconResolver.resolve(
                                if (isExpanded) "expand_less" else "expand_more"
                            ),
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }

                    // Content
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            val contentComposable = renderer.render(item.content) as @Composable () -> Unit
                            contentComposable()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render CarouselComponent to HorizontalPager
 */
@Composable
fun CarouselComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = currentIndex,
        pageCount = { items.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentIndex) {
            onSlideChange?.invoke(pagerState.currentPage)
        }
    }

    Column(modifier = modifierConverter.convert(modifiers)) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val itemComposable = renderer.render(items[page]) as @Composable () -> Unit
                itemComposable()
            }
        }

        // Indicators
        if (showIndicators) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(items.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }
    }
}

/**
 * Render TimelineComponent to a timeline display
 */
@Composable
fun TimelineComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    LazyColumn(modifier = modifierConverter.convert(modifiers)) {
        items(items.size) { index ->
            val item = items[index]

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Timeline indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    // Dot/Icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.completed)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = IconResolver.resolve(item.icon),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (item.completed)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Connector line (except for last item)
                    if (index < items.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    item.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item.timestamp?.let { timestamp ->
                        Text(
                            text = timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Render DataGridComponent to a data grid with sorting/filtering
 */
@Composable
fun DataGridComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Column(modifier = modifierConverter.convert(modifiers)) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            columns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(column.width?.dp ?: 120.dp)
                        .clickable(enabled = sortable && column.sortable) {
                            // Sorting would be handled externally
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = column.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        if (sortedBy == column.key) {
                            Icon(
                                imageVector = IconResolver.resolve(
                                    if (sortAscending) "expand_less" else "expand_more"
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Data rows
        LazyColumn {
            items(currentPageRows.size) { index ->
                val row = currentPageRows[index]
                val isSelected = index in selectedRowIndices

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                Color.Transparent
                        )
                        .clickable(enabled = selectable) {
                            // Selection would be handled externally
                        }
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    columns.forEach { column ->
                        Box(
                            modifier = Modifier
                                .width(column.width?.dp ?: 120.dp)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = row[column.key]?.toString() ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        // Pagination
        if (paginated) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page $currentPage of $totalPages",
                    style = MaterialTheme.typography.bodySmall
                )
                Row {
                    TextButton(
                        onClick = { },
                        enabled = currentPage > 1
                    ) {
                        Text("Previous")
                    }
                    TextButton(
                        onClick = { },
                        enabled = currentPage < totalPages
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

/**
 * Render DataTableComponent to a simple data table
 */
@Composable
fun DataTableComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Column(modifier = modifierConverter.convert(modifiers)) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            headers.forEach { header ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        // Data rows
        LazyColumn {
            items(rows.size) { index ->
                val row = rows[index]
                val isSelected = index in selectedRows

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                Color.Transparent
                        )
                        .clickable(enabled = selectable) {
                            // Selection handled externally
                        }
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

/**
 * Render ListComponent to Material3 list
 */
@Composable
fun ListComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    LazyColumn(modifier = modifierConverter.convert(modifiers)) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = index in selectedIndices

            ListItem(
                headlineContent = { Text(item.primary) },
                supportingContent = item.secondary?.let { { Text(it) } },
                leadingContent = when {
                    item.avatar != null -> {
                        {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                    }
                    item.icon != null -> {
                        {
                            Icon(
                                imageVector = IconResolver.resolve(item.icon),
                                contentDescription = null
                            )
                        }
                    }
                    else -> null
                },
                trailingContent = item.trailing?.let { trailing ->
                    {
                        val trailingComposable = renderer.render(trailing) as @Composable () -> Unit
                        trailingComposable()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else
                            Color.Transparent
                    )
                    .clickable { onItemClick?.invoke(index) }
            )
            HorizontalDivider()
        }
    }
}

/**
 * Render TreeViewComponent to hierarchical tree view
 */
@Composable
fun TreeViewComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    var expandedIds by remember { mutableStateOf(this.expandedIds) }

    LazyColumn(modifier = modifierConverter.convert(modifiers)) {
        item {
            TreeNodeList(
                nodes = nodes,
                expandedIds = expandedIds,
                depth = 0,
                renderer = renderer,
                onNodeClick = { id -> onNodeClick?.invoke(id) },
                onToggle = { id ->
                    expandedIds = if (id in expandedIds) {
                        expandedIds - id
                    } else {
                        expandedIds + id
                    }
                    onToggle?.invoke(id)
                }
            )
        }
    }
}

@Composable
private fun TreeNodeList(
    nodes: List<TreeNode>,
    expandedIds: Set<String>,
    depth: Int,
    renderer: ComposeRenderer,
    onNodeClick: (String) -> Unit,
    onToggle: (String) -> Unit
) {
    Column {
        nodes.forEach { node ->
            val isExpanded = node.id in expandedIds
            val hasChildren = node.children.isNotEmpty()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNodeClick(node.id) }
                    .padding(
                        start = (depth * 24 + 8).dp,
                        top = 8.dp,
                        end = 8.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/collapse button
                if (hasChildren) {
                    IconButton(
                        onClick = { onToggle(node.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = IconResolver.resolve(
                                if (isExpanded) "expand_more" else "chevron_right"
                            ),
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }

                // Node icon
                node.icon?.let { iconName ->
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                    )
                }

                // Node label
                Text(
                    text = node.label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Render children if expanded
            AnimatedVisibility(
                visible = isExpanded && hasChildren,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                TreeNodeList(
                    nodes = node.children,
                    expandedIds = expandedIds,
                    depth = depth + 1,
                    renderer = renderer,
                    onNodeClick = onNodeClick,
                    onToggle = onToggle
                )
            }
        }
    }
}

/**
 * Render ChipComponent to Material3 Chip
 */
@Composable
fun ChipComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    if (deletable) {
        InputChip(
            selected = selected,
            onClick = { onClick?.invoke() },
            label = { Text(label) },
            leadingIcon = icon?.let { iconName ->
                {
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = { onDelete?.invoke() },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = IconResolver.resolve("close"),
                        contentDescription = "Delete"
                    )
                }
            },
            modifier = modifierConverter.convert(modifiers)
        )
    } else if (selected) {
        FilterChip(
            selected = true,
            onClick = { onClick?.invoke() },
            label = { Text(label) },
            leadingIcon = icon?.let { iconName ->
                {
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            modifier = modifierConverter.convert(modifiers)
        )
    } else {
        AssistChip(
            onClick = { onClick?.invoke() },
            label = { Text(label) },
            leadingIcon = icon?.let { iconName ->
                {
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            modifier = modifierConverter.convert(modifiers)
        )
    }
}

/**
 * Render PaperComponent to elevated Surface
 */
@Composable
fun PaperComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Surface(
        modifier = modifierConverter.convert(modifiers),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = (elevation * 2).dp,
        shadowElevation = elevation.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            children.forEach { child ->
                val childComposable = renderer.render(child) as @Composable () -> Unit
                childComposable()
            }
        }
    }
}

/**
 * Render EmptyStateComponent to centered empty state display
 */
@Composable
fun EmptyStateComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()

    Column(
        modifier = modifierConverter.convert(modifiers)
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        icon?.let { iconName ->
            Icon(
                imageVector = IconResolver.resolve(iconName),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Description
        description?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Action button
        action?.let { action ->
            val actionComposable = renderer.render(action) as @Composable () -> Unit
            actionComposable()
        }
    }
}
