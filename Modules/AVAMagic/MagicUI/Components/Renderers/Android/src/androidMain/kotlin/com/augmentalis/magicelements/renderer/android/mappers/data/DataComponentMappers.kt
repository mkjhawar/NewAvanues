package com.augmentalis.avaelements.renderer.android.mappers.data

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.magicui.ui.core.data.*
import com.augmentalis.magicui.ui.core.display.*
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.IconResolver
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * AccordionMapper - Maps AccordionComponent to expandable/collapsible panels
 */
class AccordionMapper : ComponentMapper<AccordionComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: AccordionComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var expandedIndices by remember { mutableStateOf(component.expandedIndices) }

            Column(modifier = modifierConverter.convert(component.modifiers)) {
                component.items.forEachIndexed { index, item ->
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
                                            if (component.allowMultiple) {
                                                expandedIndices + index
                                            } else {
                                                setOf(index)
                                            }
                                        }
                                        component.onToggle?.invoke(index)
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
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
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
    }
}

/**
 * CarouselMapper - Maps CarouselComponent to HorizontalPager
 */
class CarouselMapper : ComponentMapper<CarouselComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: CarouselComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val pagerState = rememberPagerState(
                initialPage = component.currentIndex,
                pageCount = { component.items.size }
            )

            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage != component.currentIndex) {
                    component.onSlideChange?.invoke(pagerState.currentPage)
                }
            }

            Column(modifier = modifierConverter.convert(component.modifiers)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val itemComposable = renderer.render(component.items[page]) as @Composable () -> Unit
                        itemComposable()
                    }
                }

                // Indicators
                if (component.showIndicators) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(component.items.size) { index ->
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
    }
}

/**
 * TimelineMapper - Maps TimelineComponent to a timeline display
 */
class TimelineMapper : ComponentMapper<TimelineComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: TimelineComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            LazyColumn(modifier = modifierConverter.convert(component.modifiers)) {
                itemsIndexed(component.items) { index, item ->
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
                            if (index < component.items.size - 1) {
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
                                fontWeight = FontWeight.Medium
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
    }
}

/**
 * DataGridMapper - Maps DataGridComponent to a data grid with sorting/filtering
 */
class DataGridMapper : ComponentMapper<DataGridComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: DataGridComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(modifier = modifierConverter.convert(component.modifiers)) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .horizontalScroll(rememberScrollState())
                ) {
                    component.columns.forEach { column ->
                        Box(
                            modifier = Modifier
                                .width(column.width?.dp ?: 120.dp)
                                .clickable(enabled = component.sortable && column.sortable) {
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
                                    fontWeight = FontWeight.Bold
                                )
                                if (component.sortedBy == column.key) {
                                    Icon(
                                        imageVector = if (component.sortAscending)
                                            Icons.Default.ExpandLess
                                        else
                                            Icons.Default.ExpandMore,
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
                    itemsIndexed(component.currentPageRows) { index, row ->
                        val isSelected = index in component.selectedRowIndices
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        Color.Transparent
                                )
                                .clickable(enabled = component.selectable) {
                                    // Selection would be handled externally
                                }
                                .horizontalScroll(rememberScrollState())
                        ) {
                            component.columns.forEach { column ->
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
                if (component.paginated) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${component.currentPage} of ${component.totalPages}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row {
                            TextButton(
                                onClick = { },
                                enabled = component.currentPage > 1
                            ) {
                                Text("Previous")
                            }
                            TextButton(
                                onClick = { },
                                enabled = component.currentPage < component.totalPages
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * DataTableMapper - Maps DataTableComponent to a simple data table
 */
class DataTableMapper : ComponentMapper<DataTableComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: DataTableComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(modifier = modifierConverter.convert(component.modifiers)) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .horizontalScroll(rememberScrollState())
                ) {
                    component.headers.forEach { header ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Data rows
                LazyColumn {
                    itemsIndexed(component.rows) { index, row ->
                        val isSelected = index in component.selectedRows
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        Color.Transparent
                                )
                                .clickable(enabled = component.selectable) {
                                    // Selection handled externally
                                }
                                .horizontalScroll(rememberScrollState())
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
    }
}

/**
 * ListComponentMapper - Maps ListComponent to a Material list
 */
class ListComponentMapper : ComponentMapper<ListComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ListComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            LazyColumn(modifier = modifierConverter.convert(component.modifiers)) {
                itemsIndexed(component.items) { index, item ->
                    val isSelected = index in component.selectedIndices

                    ListItem(
                        headlineContent = { Text(item.primary) },
                        supportingContent = item.secondary?.let { { Text(it) } },
                        leadingContent = when {
                            item.avatar != null -> {
                                {
                                    // Avatar would be loaded via image loader
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
                            .clickable { component.onItemClick?.invoke(index) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * TreeViewMapper - Maps TreeViewComponent to a hierarchical tree view
 */
class TreeViewMapper : ComponentMapper<TreeViewComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: TreeViewComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var expandedIds by remember { mutableStateOf(component.expandedIds) }

            LazyColumn(modifier = modifierConverter.convert(component.modifiers)) {
                item {
                    TreeNodeList(
                        nodes = component.nodes,
                        expandedIds = expandedIds,
                        depth = 0,
                        onNodeClick = { id -> component.onNodeClick?.invoke(id) },
                        onToggle = { id ->
                            expandedIds = if (id in expandedIds) {
                                expandedIds - id
                            } else {
                                expandedIds + id
                            }
                            component.onToggle?.invoke(id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TreeNodeList(
    nodes: List<TreeNode>,
    expandedIds: Set<String>,
    depth: Int,
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
                            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
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
                    onNodeClick = onNodeClick,
                    onToggle = onToggle
                )
            }
        }
    }
}

/**
 * ChipComponentMapper - Maps ChipComponent to Material3 Chip
 * Note: This is for the data package ChipComponent, distinct from display Chip
 */
class ChipComponentMapper : ComponentMapper<ChipComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ChipComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            if (component.deletable) {
                InputChip(
                    selected = component.selected,
                    onClick = { component.onClick?.invoke() },
                    label = { Text(component.label) },
                    leadingIcon = component.icon?.let { iconName ->
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
                            onClick = { component.onDelete?.invoke() },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete"
                            )
                        }
                    },
                    modifier = modifierConverter.convert(component.modifiers)
                )
            } else if (component.selected) {
                FilterChip(
                    selected = true,
                    onClick = { component.onClick?.invoke() },
                    label = { Text(component.label) },
                    leadingIcon = component.icon?.let { iconName ->
                        {
                            Icon(
                                imageVector = IconResolver.resolve(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = modifierConverter.convert(component.modifiers)
                )
            } else {
                AssistChip(
                    onClick = { component.onClick?.invoke() },
                    label = { Text(component.label) },
                    leadingIcon = component.icon?.let { iconName ->
                        {
                            Icon(
                                imageVector = IconResolver.resolve(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = modifierConverter.convert(component.modifiers)
                )
            }
        }
    }
}

/**
 * PaperMapper - Maps PaperComponent to elevated Surface
 */
class PaperMapper : ComponentMapper<PaperComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: PaperComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Surface(
                modifier = modifierConverter.convert(component.modifiers),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = (component.elevation * 2).dp,
                shadowElevation = component.elevation.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    component.children.forEach { child ->
                        val childComposable = renderer.render(child) as @Composable () -> Unit
                        childComposable()
                    }
                }
            }
        }
    }
}

/**
 * EmptyStateMapper - Maps EmptyStateComponent to a centered empty state display
 */
class EmptyStateMapper : ComponentMapper<EmptyStateComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: EmptyStateComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(
                modifier = modifierConverter.convert(component.modifiers)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
                component.icon?.let { iconName ->
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
                    text = component.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Description
                component.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Action button
                component.action?.let { action ->
                    val actionComposable = renderer.render(action) as @Composable () -> Unit
                    actionComposable()
                }
            }
        }
    }
}
