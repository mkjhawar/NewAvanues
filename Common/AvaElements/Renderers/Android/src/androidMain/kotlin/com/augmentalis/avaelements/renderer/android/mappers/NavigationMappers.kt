package com.augmentalis.avaelements.renderer.android.mappers

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.flutter.material.navigation.*
import com.augmentalis.avaelements.renderer.android.IconFromString

/**
 * Android Compose mappers for navigation components
 *
 * This file contains renderer functions that map cross-platform navigation component models
 * to Material3 Compose implementations on Android.
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render Menu component using Material3
 *
 * Maps Menu component to Material3 with nested submenus, selection states,
 * and full keyboard navigation support.
 */
@Composable
fun MenuMapper(component: Menu) {
    val backgroundColor = component.backgroundColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .semantics { contentDescription = component.getAccessibilityDescription() },
        color = backgroundColor,
        shadowElevation = component.elevation?.dp ?: 1.dp
    ) {
        when (component.orientation) {
            Menu.Orientation.Vertical -> {
                Column(
                    modifier = Modifier.padding(component.contentPadding?.let {
                        PaddingValues(
                            start = it.left.dp,
                            top = it.top.dp,
                            end = it.right.dp,
                            bottom = it.bottom.dp
                        )
                    } ?: PaddingValues(0.dp))
                ) {
                    component.items.forEachIndexed { index, item ->
                        MenuItemRenderer(
                            item = item,
                            index = index,
                            isSelected = component.isItemSelected(index),
                            dense = component.dense,
                            onItemClick = {
                                item.onClick?.invoke()
                                component.onItemClick?.invoke(item.id)
                                component.onSelectionChanged?.invoke(index)
                            }
                        )

                        if (item.divider) {
                            HorizontalDivider()
                        }
                    }
                }
            }
            Menu.Orientation.Horizontal -> {
                Row(
                    modifier = Modifier.padding(component.contentPadding?.let {
                        PaddingValues(
                            start = it.left.dp,
                            top = it.top.dp,
                            end = it.right.dp,
                            bottom = it.bottom.dp
                        )
                    } ?: PaddingValues(8.dp)),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    component.items.forEachIndexed { index, item ->
                        MenuItemHorizontalRenderer(
                            item = item,
                            index = index,
                            isSelected = component.isItemSelected(index),
                            onItemClick = {
                                item.onClick?.invoke()
                                component.onItemClick?.invoke(item.id)
                                component.onSelectionChanged?.invoke(index)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemRenderer(
    item: Menu.MenuItem,
    index: Int,
    isSelected: Boolean,
    dense: Boolean,
    onItemClick: () -> Unit,
    indent: Int = 0
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.label)
                    if (item.badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge { Text(item.badge) }
                    }
                }
            },
            leadingContent = item.icon?.let {
                {
                    IconFromString(
                        iconName = it,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            trailingContent = if (item.hasSubmenu()) {
                {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            } else null,
            modifier = Modifier
                .clickable(enabled = item.enabled) {
                    if (item.hasSubmenu()) {
                        expanded = !expanded
                    } else {
                        onItemClick()
                    }
                }
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
                .padding(start = (indent * 16).dp)
                .semantics { contentDescription = item.getAccessibilityDescription() },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                leadingIconColor = if (item.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        )

        // Render submenu
        AnimatedVisibility(visible = expanded && item.hasSubmenu()) {
            Column {
                item.children?.forEachIndexed { childIndex, childItem ->
                    MenuItemRenderer(
                        item = childItem,
                        index = childIndex,
                        isSelected = false,
                        dense = dense,
                        onItemClick = {
                            childItem.onClick?.invoke()
                        },
                        indent = indent + 1
                    )
                    if (childItem.divider) {
                        HorizontalDivider(modifier = Modifier.padding(start = ((indent + 1) * 16).dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemHorizontalRenderer(
    item: Menu.MenuItem,
    index: Int,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    var showSubmenu by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = {
                if (item.hasSubmenu()) {
                    showSubmenu = !showSubmenu
                } else {
                    onItemClick()
                }
            },
            enabled = item.enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.semantics { contentDescription = item.getAccessibilityDescription() }
        ) {
            if (item.icon != null) {
                IconFromString(
                    iconName = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(item.label)
            if (item.badge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge { Text(item.badge) }
            }
        }

        // Dropdown submenu
        DropdownMenu(
            expanded = showSubmenu,
            onDismissRequest = { showSubmenu = false }
        ) {
            item.children?.forEach { childItem ->
                DropdownMenuItem(
                    text = { Text(childItem.label) },
                    onClick = {
                        childItem.onClick?.invoke()
                        showSubmenu = false
                    },
                    enabled = childItem.enabled,
                    leadingIcon = childItem.icon?.let {
                        {
                            IconFromString(
                                iconName = it,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
                if (childItem.divider) {
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * Render Sidebar component using Material3
 *
 * Maps Sidebar component to Material3 NavigationRail/ModalDrawer with
 * collapsible behavior and persistent/overlay modes.
 */
@Composable
fun SidebarMapper(component: Sidebar) {
    val backgroundColor = component.backgroundColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.surface
    val selectedColor = component.selectedItemColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primary
    val unselectedColor = component.unselectedItemColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    val width by animateFloatAsState(
        targetValue = component.getEffectiveWidth(),
        animationSpec = tween(durationMillis = 300)
    )

    if (!component.visible && component.mode == Sidebar.Mode.Overlay) {
        return
    }

    Surface(
        modifier = Modifier
            .width(width.dp)
            .fillMaxHeight()
            .semantics { contentDescription = component.getAccessibilityDescription() },
        color = backgroundColor,
        shadowElevation = component.elevation?.dp ?: 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            if (component.headerContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(component.headerHeight?.dp ?: 64.dp)
                        .padding(16.dp)
                ) {
                    Text(
                        text = component.headerContent,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = if (component.collapsed) 0 else 2
                    )
                }
                HorizontalDivider()
            }

            // Collapse toggle button
            if (component.collapsible) {
                IconButton(
                    onClick = { component.onCollapseToggle?.invoke(!component.collapsed) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = if (component.collapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                        contentDescription = if (component.collapsed) "Expand sidebar" else "Collapse sidebar"
                    )
                }
            }

            // Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                component.items.forEach { item ->
                    SidebarItemRenderer(
                        item = item,
                        collapsed = component.collapsed,
                        selectedColor = selectedColor,
                        unselectedColor = unselectedColor,
                        onItemClick = {
                            item.onClick?.invoke()
                            component.onItemClick?.invoke(item.id)
                        }
                    )

                    if (item.divider) {
                        HorizontalDivider()
                    }
                }
            }

            // Footer
            if (component.footerContent != null && !component.collapsed) {
                HorizontalDivider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = component.footerContent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarItemRenderer(
    item: Sidebar.SidebarItem,
    collapsed: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onItemClick: () -> Unit
) {
    NavigationRailItem(
        selected = item.selected,
        onClick = onItemClick,
        icon = {
            Box {
                if (item.icon != null) {
                    IconFromString(
                        iconName = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (item.badge != null) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(item.badge)
                    }
                }
            }
        },
        label = if (!collapsed) {
            { Text(item.label, maxLines = 1) }
        } else null,
        enabled = item.enabled,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = selectedColor,
            selectedTextColor = selectedColor,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = item.getAccessibilityDescription() }
    )
}

/**
 * Render NavLink component using Material3
 *
 * Maps NavLink component to Material3 NavigationBarItem with active state styling.
 */
@Composable
fun NavLinkMapper(component: NavLink) {
    val backgroundColor = if (component.active) {
        component.activeBackgroundColor?.let { Color(AndroidColor.parseColor(it)) }
            ?: MaterialTheme.colorScheme.primaryContainer
    } else {
        component.backgroundColor?.let { Color(AndroidColor.parseColor(it)) }
            ?: Color.Transparent
    }

    val contentColor = if (component.active) {
        component.activeColor?.let { Color(AndroidColor.parseColor(it)) }
            ?: MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        component.inactiveColor?.let { Color(AndroidColor.parseColor(it)) }
            ?: MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .clickable(enabled = component.enabled) { component.onClick?.invoke() }
            .padding(component.contentPadding?.let {
                PaddingValues(it.left.dp, it.top.dp, it.right.dp, it.bottom.dp)
            } ?: PaddingValues(8.dp))
            .semantics { contentDescription = component.getAccessibilityDescription() },
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        when (component.iconPosition) {
            NavLink.IconPosition.Leading -> {
                Row(
                    modifier = Modifier.padding(12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavLinkContent(component, contentColor)
                }
            }
            NavLink.IconPosition.Trailing -> {
                Row(
                    modifier = Modifier.padding(12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(component.label, color = contentColor, style = MaterialTheme.typography.labelLarge)
                    if (component.icon != null) {
                        IconFromString(component.icon, null, Modifier.size(20.dp))
                    }
                    if (component.badge != null) {
                        Badge { Text(component.badge) }
                    }
                }
            }
            NavLink.IconPosition.Top -> {
                Column(
                    modifier = Modifier.padding(12.dp, 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (component.icon != null) {
                        Box {
                            IconFromString(component.icon, null, Modifier.size(24.dp))
                            if (component.badge != null) {
                                Badge(modifier = Modifier.align(Alignment.TopEnd)) { Text(component.badge) }
                            }
                        }
                    }
                    Text(component.label, color = contentColor, style = MaterialTheme.typography.labelMedium)
                }
            }
            NavLink.IconPosition.Bottom -> {
                Column(
                    modifier = Modifier.padding(12.dp, 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(component.label, color = contentColor, style = MaterialTheme.typography.labelMedium)
                    if (component.icon != null) {
                        Box {
                            IconFromString(component.icon, null, Modifier.size(24.dp))
                            if (component.badge != null) {
                                Badge(modifier = Modifier.align(Alignment.TopEnd)) { Text(component.badge) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavLinkContent(component: NavLink, contentColor: Color) {
    if (component.icon != null) {
        Box {
            IconFromString(component.icon, null, Modifier.size(20.dp))
            if (component.badge != null) {
                Badge(modifier = Modifier.align(Alignment.TopEnd)) { Text(component.badge) }
            }
        }
    }
    Text(component.label, color = contentColor, style = MaterialTheme.typography.labelLarge)
    if (component.badge != null && component.icon == null) {
        Badge { Text(component.badge) }
    }
}

/**
 * Render ProgressStepper component using Material3
 *
 * Maps ProgressStepper to a custom stepper with completed/current/upcoming states.
 */
@Composable
fun ProgressStepperMapper(component: ProgressStepper) {
    val completedColor = component.completedStepColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primary
    val currentColor = component.currentStepColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.secondary
    val upcomingColor = component.upcomingStepColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.outlineVariant
    val connectorColor = component.connectorColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .padding(component.contentPadding?.let {
                PaddingValues(it.left.dp, it.top.dp, it.right.dp, it.bottom.dp)
            } ?: PaddingValues(16.dp))
            .semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        when (component.orientation) {
            ProgressStepper.Orientation.Horizontal -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    component.steps.forEachIndexed { index, step ->
                        val stepState = component.getStepState(index)

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StepIndicator(
                                step = step,
                                stepNumber = index + 1,
                                state = stepState,
                                showNumber = component.showStepNumbers,
                                completedColor = completedColor,
                                currentColor = currentColor,
                                upcomingColor = upcomingColor,
                                clickable = component.canClickStep(index),
                                onClick = { component.onStepClicked?.invoke(index) }
                            )

                            // Connector line
                            if (index < component.steps.size - 1) {
                                StepConnector(
                                    type = component.connectorType,
                                    color = connectorColor,
                                    completed = index < component.currentStep,
                                    horizontal = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            ProgressStepper.Orientation.Vertical -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    component.steps.forEachIndexed { index, step ->
                        val stepState = component.getStepState(index)

                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                StepIndicator(
                                    step = step,
                                    stepNumber = index + 1,
                                    state = stepState,
                                    showNumber = component.showStepNumbers,
                                    completedColor = completedColor,
                                    currentColor = currentColor,
                                    upcomingColor = upcomingColor,
                                    clickable = component.canClickStep(index),
                                    onClick = { component.onStepClicked?.invoke(index) }
                                )

                                // Vertical connector
                                if (index < component.steps.size - 1) {
                                    StepConnector(
                                        type = component.connectorType,
                                        color = connectorColor,
                                        completed = index < component.currentStep,
                                        horizontal = false,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = step.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = when (stepState) {
                                        ProgressStepper.StepState.Completed -> completedColor
                                        ProgressStepper.StepState.Current -> currentColor
                                        ProgressStepper.StepState.Upcoming -> upcomingColor
                                    }
                                )
                                if (step.description != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = step.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (step.optional) {
                                    Text(
                                        text = "Optional",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        if (index < component.steps.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    step: ProgressStepper.Step,
    stepNumber: Int,
    state: ProgressStepper.StepState,
    showNumber: Boolean,
    completedColor: Color,
    currentColor: Color,
    upcomingColor: Color,
    clickable: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (state) {
        ProgressStepper.StepState.Completed -> completedColor
        ProgressStepper.StepState.Current -> currentColor
        ProgressStepper.StepState.Upcoming -> upcomingColor
    }

    val errorColor = if (step.error) MaterialTheme.colorScheme.error else backgroundColor

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(errorColor)
            .then(
                if (clickable) Modifier.clickable { onClick() }
                else Modifier
            )
            .semantics { contentDescription = step.getAccessibilityDescription(state, stepNumber) },
        contentAlignment = Alignment.Center
    ) {
        when {
            step.error -> Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(24.dp)
            )
            state == ProgressStepper.StepState.Completed -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
            step.icon != null -> IconFromString(
                iconName = step.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            showNumber -> Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            else -> Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun StepConnector(
    type: ProgressStepper.ConnectorType,
    color: Color,
    completed: Boolean,
    horizontal: Boolean,
    modifier: Modifier = Modifier
) {
    if (type == ProgressStepper.ConnectorType.None) {
        return
    }

    val lineColor = if (completed) color else color.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .then(
                if (horizontal) Modifier.fillMaxWidth().height(2.dp)
                else Modifier.width(2.dp).fillMaxHeight()
            )
            .background(lineColor)
    )
}

/**
 * Render MenuBar component using Material3
 *
 * Maps MenuBar component to Material3 TopAppBar with dropdown menus
 */
@Composable
fun MenuBarMapper(component: MenuBar) {
    val backgroundColor = component.backgroundColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.surface
    var expandedMenuId by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(component.height.dp)
            .semantics { contentDescription = component.getAccessibilityDescription() },
        color = backgroundColor,
        shadowElevation = component.elevation?.dp ?: 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(component.contentPadding?.let {
                    PaddingValues(
                        start = it.left.dp,
                        top = it.top.dp,
                        end = it.right.dp,
                        bottom = it.bottom.dp
                    )
                } ?: PaddingValues(horizontal = 8.dp)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            component.items.forEach { menuBarItem ->
                MenuBarItemRenderer(
                    item = menuBarItem,
                    expanded = expandedMenuId == menuBarItem.id,
                    showAccelerators = component.showAccelerators,
                    onMenuOpen = {
                        expandedMenuId = if (expandedMenuId == menuBarItem.id) null else menuBarItem.id
                        component.onMenuOpen?.invoke(menuBarItem.id)
                    },
                    onItemClick = { itemId ->
                        component.onItemClick?.invoke(itemId)
                        expandedMenuId = null
                    },
                    onDismiss = { expandedMenuId = null }
                )
            }
        }
    }
}

@Composable
private fun MenuBarItemRenderer(
    item: MenuBar.MenuBarItem,
    expanded: Boolean,
    showAccelerators: Boolean,
    onMenuOpen: () -> Unit,
    onItemClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box {
        TextButton(
            onClick = {
                item.onClick?.invoke()
                if (item.hasDropdown()) {
                    onMenuOpen()
                } else {
                    onItemClick(item.id)
                }
            },
            enabled = item.enabled,
            modifier = Modifier.semantics { contentDescription = item.getAccessibilityDescription() }
        ) {
            if (item.icon != null) {
                IconFromString(
                    iconName = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 4.dp)
                )
            }
            Text(
                text = if (showAccelerators) item.getFormattedLabel() else item.label,
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Dropdown menu
        if (item.hasDropdown()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismiss
            ) {
                item.children?.forEach { menuItem ->
                    MenuBarDropdownItem(
                        item = menuItem,
                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuBarDropdownItem(
    item: Menu.MenuItem,
    onItemClick: (String) -> Unit
) {
    if (item.divider) {
        HorizontalDivider()
    } else {
        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.label)
                    if (item.badge != null) {
                        Badge { Text(item.badge) }
                    }
                }
            },
            onClick = {
                item.onClick?.invoke()
                onItemClick(item.id)
            },
            enabled = item.enabled,
            leadingIcon = item.icon?.let {
                {
                    IconFromString(
                        iconName = it,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
    }
}

/**
 * Render SubMenu component using Material3
 *
 * Maps SubMenu component to Material3 nested DropdownMenu with cascading support
 */
@Composable
fun SubMenuMapper(component: SubMenu) {
    var expanded by remember { mutableStateOf(component.open) }
    val backgroundColor = component.backgroundColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.surface

    Box {
        // Trigger button
        TextButton(
            onClick = {
                if (component.trigger == SubMenu.TriggerMode.Click ||
                    component.trigger == SubMenu.TriggerMode.Both
                ) {
                    expanded = !expanded
                    component.onOpenChange?.invoke(expanded)
                }
            },
            enabled = component.enabled,
            modifier = Modifier.semantics { contentDescription = component.getAccessibilityDescription() }
        ) {
            if (component.icon != null) {
                IconFromString(
                    iconName = component.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(end = 4.dp)
                )
            }
            Text(component.label)
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(16.dp).padding(start = 4.dp)
            )
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                component.onOpenChange?.invoke(false)
            },
            modifier = Modifier.semantics { contentDescription = component.getAccessibilityDescription() }
        ) {
            Surface(
                color = backgroundColor,
                shadowElevation = component.elevation?.dp ?: 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(component.contentPadding?.let {
                        PaddingValues(
                            start = it.left.dp,
                            top = it.top.dp,
                            end = it.right.dp,
                            bottom = it.bottom.dp
                        )
                    } ?: PaddingValues(0.dp))
                ) {
                    component.items.forEach { item ->
                        SubMenuItemRenderer(
                            item = item,
                            onItemClick = { itemId ->
                                component.onItemClick?.invoke(itemId)
                                if (component.closeOnItemClick && !item.hasSubmenu()) {
                                    expanded = false
                                    component.onOpenChange?.invoke(false)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubMenuItemRenderer(
    item: SubMenu.SubMenuItem,
    onItemClick: (String) -> Unit,
    indent: Int = 0
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        if (item.divider) {
            HorizontalDivider()
        }

        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.label)
                        if (item.badge != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge { Text(item.badge) }
                        }
                    }
                    if (item.shortcut != null) {
                        Text(
                            text = item.shortcut,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            onClick = {
                if (item.hasSubmenu()) {
                    expanded = !expanded
                } else {
                    item.onClick?.invoke()
                    onItemClick(item.id)
                }
            },
            enabled = item.enabled,
            leadingIcon = item.icon?.let {
                {
                    IconFromString(
                        iconName = it,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            trailingIcon = if (item.hasSubmenu()) {
                { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Has submenu") }
            } else null,
            colors = MenuDefaults.itemColors(
                textColor = if (item.destructive) MaterialTheme.colorScheme.error else Color.Unspecified
            ),
            modifier = Modifier
                .padding(start = (indent * 16).dp)
                .semantics { contentDescription = item.getAccessibilityDescription() }
        )

        // Nested submenu items
        if (item.hasSubmenu() && expanded) {
            item.children?.forEach { childItem ->
                SubMenuItemRenderer(
                    item = childItem,
                    onItemClick = onItemClick,
                    indent = indent + 1
                )
            }
        }
    }
}

/**
 * Render VerticalTabs component using Material3
 *
 * Maps VerticalTabs component to Material3 NavigationRail
 */
@Composable
fun VerticalTabsMapper(component: VerticalTabs) {
    val backgroundColor = component.backgroundColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.surface
    val indicatorColor = component.indicatorColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primary
    val selectedColor = component.selectedTabColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.onSecondaryContainer
    val unselectedColor = component.unselectedTabColor?.let { Color(AndroidColor.parseColor(it)) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .width(component.width.dp)
            .fillMaxHeight()
            .semantics { contentDescription = component.getAccessibilityDescription() },
        color = backgroundColor,
        shadowElevation = component.elevation?.dp ?: 0.dp
    ) {
        if (component.scrollable && component.shouldScroll()) {
            // Scrollable tabs
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(component.tabs.size) { index ->
                    val tab = component.tabs[index]
                    VerticalTabItemRenderer(
                        tab = tab,
                        isSelected = tab.id == component.selectedTabId || tab.selected,
                        showLabel = component.showLabels,
                        showIcon = component.showIcons,
                        labelPosition = component.labelPosition,
                        selectedColor = selectedColor,
                        unselectedColor = unselectedColor,
                        indicatorColor = indicatorColor,
                        indicatorWidth = component.indicatorWidth,
                        dense = component.dense,
                        onTabClick = {
                            tab.onClick?.invoke()
                            component.onTabSelected?.invoke(tab.id)
                        }
                    )

                    if (tab.divider && index < component.tabs.size - 1) {
                        HorizontalDivider(
                            color = component.dividerColor?.let { Color(AndroidColor.parseColor(it)) }
                                ?: MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        } else {
            // Fixed tabs
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                component.tabs.forEachIndexed { index, tab ->
                    VerticalTabItemRenderer(
                        tab = tab,
                        isSelected = tab.id == component.selectedTabId || tab.selected,
                        showLabel = component.showLabels,
                        showIcon = component.showIcons,
                        labelPosition = component.labelPosition,
                        selectedColor = selectedColor,
                        unselectedColor = unselectedColor,
                        indicatorColor = indicatorColor,
                        indicatorWidth = component.indicatorWidth,
                        dense = component.dense,
                        onTabClick = {
                            tab.onClick?.invoke()
                            component.onTabSelected?.invoke(tab.id)
                        }
                    )

                    if (tab.divider && index < component.tabs.size - 1) {
                        HorizontalDivider(
                            color = component.dividerColor?.let { Color(AndroidColor.parseColor(it)) }
                                ?: MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalTabItemRenderer(
    tab: VerticalTabs.Tab,
    isSelected: Boolean,
    showLabel: Boolean,
    showIcon: Boolean,
    labelPosition: VerticalTabs.LabelPosition,
    selectedColor: Color,
    unselectedColor: Color,
    indicatorColor: Color,
    indicatorWidth: Float,
    dense: Boolean,
    onTabClick: () -> Unit
) {
    val contentColor = if (isSelected) selectedColor else unselectedColor
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = tab.enabled, onClick = onTabClick)
            .background(backgroundColor)
            .padding(
                horizontal = if (dense) 8.dp else 16.dp,
                vertical = if (dense) 8.dp else 12.dp
            )
            .semantics { contentDescription = tab.getAccessibilityDescription() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(indicatorWidth.dp)
                    .height(if (dense) 32.dp else 40.dp)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width((indicatorWidth + 8).dp))
        }

        // Icon and label based on position
        when (labelPosition) {
            VerticalTabs.LabelPosition.Right -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showIcon && tab.hasIcon()) {
                        IconFromString(
                            iconName = tab.icon!!,
                            contentDescription = null,
                            modifier = Modifier.size(if (dense) 20.dp else 24.dp),
                            tint = contentColor
                        )
                    }
                    if (showLabel) {
                        Text(
                            text = tab.label,
                            style = if (dense) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (tab.hasBadge()) {
                        Badge { Text(tab.badge!!) }
                    }
                }
            }
            VerticalTabs.LabelPosition.Left -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showLabel) {
                        Text(
                            text = tab.label,
                            style = if (dense) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (showIcon && tab.hasIcon()) {
                        IconFromString(
                            iconName = tab.icon!!,
                            contentDescription = null,
                            modifier = Modifier.size(if (dense) 20.dp else 24.dp),
                            tint = contentColor
                        )
                    }
                    if (tab.hasBadge()) {
                        Badge { Text(tab.badge!!) }
                    }
                }
            }
            VerticalTabs.LabelPosition.Bottom,
            VerticalTabs.LabelPosition.Top -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (labelPosition == VerticalTabs.LabelPosition.Top && showLabel) {
                        Text(
                            text = tab.label,
                            style = if (dense) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (showIcon && tab.hasIcon()) {
                        Box {
                            IconFromString(
                                iconName = tab.icon!!,
                                contentDescription = null,
                                modifier = Modifier.size(if (dense) 20.dp else 24.dp),
                                tint = contentColor
                            )
                            if (tab.hasBadge()) {
                                Badge(
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)
                                ) {
                                    Text(tab.badge!!)
                                }
                            }
                        }
                    }
                    if (labelPosition == VerticalTabs.LabelPosition.Bottom && showLabel) {
                        Text(
                            text = tab.label,
                            style = if (dense) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
