package com.augmentalis.avanueui.renderer.android.extensions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.IconResolver
import com.augmentalis.avanueui.renderer.android.toComposeColor
import com.augmentalis.avanueui.core.Position
import com.augmentalis.avanueui.core.Severity
import com.augmentalis.avanueui.ui.core.feedback.*
import com.augmentalis.avanueui.ui.core.navigation.*
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * Navigation & Feedback Component Extensions
 *
 * Extension functions for rendering navigation and feedback MagicUI components.
 * Converted from mapper pattern to extension pattern for improved performance and readability.
 *
 * Navigation Components:
 * - AppBar
 * - BottomNav
 * - NavigationDrawer
 *
 * Feedback Components:
 * - Toast
 * - Snackbar
 * - ProgressBar
 * - Modal
 * - Confirm
 * - ContextMenu
 * - Dialog
 */

// ==================== Navigation Components ====================

/**
 * Render AppBarComponent to Material3 TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarComponent.Render(renderer: ComposeRenderer) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            navigationIcon?.let { icon ->
                onNavigationClick?.let { onClick ->
                    IconButton(onClick = onClick) {
                        val imageVector = when (icon) {
                            "back", "arrow_back" -> Icons.AutoMirrored.Filled.ArrowBack
                            "menu" -> Icons.Default.Menu
                            else -> Icons.AutoMirrored.Filled.ArrowBack
                        }
                        Icon(
                            imageVector = imageVector,
                            contentDescription = "Navigation"
                        )
                    }
                }
            }
        },
        actions = {
            actions.forEach { action ->
                IconButton(onClick = action.onClick) {
                    Icon(
                        imageVector = IconResolver.resolve(action.icon),
                        contentDescription = action.label
                    )
                }
            }
        }
    )
}

/**
 * Render BottomNavComponent to Material3 NavigationBar
 */
@Composable
fun BottomNavComponent.Render(renderer: ComposeRenderer) {
    NavigationBar {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected?.invoke(index) },
                icon = {
                    val badgeText = item.badge
                    if (badgeText != null) {
                        BadgedBox(
                            badge = {
                                Badge { Text(badgeText) }
                            }
                        ) {
                            Icon(
                                imageVector = IconResolver.resolve(item.icon),
                                contentDescription = item.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = IconResolver.resolve(item.icon),
                            contentDescription = item.label
                        )
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}

/**
 * Render NavigationDrawerComponent to Material3 NavigationDrawer variants
 */
@Composable
fun NavigationDrawerComponent.Render(renderer: ComposeRenderer) {
    val drawerContent: @Composable ColumnScope.() -> Unit = {
        header?.let {
            renderer.RenderComponent(it)
        }
        Spacer(Modifier.height(12.dp))
        items.forEachIndexed { index, item ->
            NavigationDrawerItem(
                icon = { Icon(IconResolver.resolve(item.icon), contentDescription = null) },
                label = { Text(item.label) },
                selected = index == selectedIndex,
                onClick = { onItemSelected?.invoke(index) },
                badge = item.badge?.let { { Text(it) } },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    when (drawerType) {
        DrawerType.MODAL -> ModalNavigationDrawer(
            drawerContent = { ModalDrawerSheet { drawerContent() } },
            modifier = renderer.convertModifiers(modifiers)
        ) {
            // Content rendered by parent
        }
        DrawerType.DISMISSIBLE -> DismissibleNavigationDrawer(
            drawerContent = { DismissibleDrawerSheet { drawerContent() } },
            modifier = renderer.convertModifiers(modifiers)
        ) {
            // Content rendered by parent
        }
        DrawerType.PERMANENT -> PermanentNavigationDrawer(
            drawerContent = { PermanentDrawerSheet { drawerContent() } },
            modifier = renderer.convertModifiers(modifiers)
        ) {
            // Content rendered by parent
        }
    }
}

// ==================== Feedback Components ====================

/**
 * Render ToastComponent to Material3 Snackbar-style toast
 */
@Composable
fun ToastComponent.Render(renderer: ComposeRenderer) {
    val (backgroundColor, contentColor) = when (severity) {
        Severity.INFO -> AvanueTheme.colors.inverseSurface to AvanueTheme.colors.inverseOnSurface
        Severity.SUCCESS -> AvanueTheme.colors.primaryContainer to AvanueTheme.colors.onPrimaryContainer
        Severity.WARNING -> AvanueTheme.colors.tertiaryContainer to AvanueTheme.colors.onTertiaryContainer
        Severity.ERROR -> AvanueTheme.colors.errorContainer to AvanueTheme.colors.onErrorContainer
        else -> AvanueTheme.colors.inverseSurface to AvanueTheme.colors.inverseOnSurface
    }

    val icon = when (severity) {
        Severity.INFO -> Icons.Default.Info
        Severity.SUCCESS -> Icons.Default.CheckCircle
        Severity.WARNING -> Icons.Default.Warning
        Severity.ERROR -> Icons.Default.Error
        else -> Icons.Default.Info
    }

    val alignment = when (position) {
        Position.TOP -> Alignment.TopCenter
        Position.BOTTOM -> Alignment.BottomCenter
        Position.CENTER -> Alignment.Center
        else -> Alignment.BottomCenter
    }

    Box(
        modifier = renderer.convertModifiers(modifiers)
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = message,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                action?.let { actionLabel ->
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { /* Action handler */ }) {
                        Text(actionLabel, color = contentColor)
                    }
                }
            }
        }
    }
}

/**
 * Render SnackbarComponent to Material3 Snackbar
 */
@Composable
fun SnackbarComponent.Render(renderer: ComposeRenderer) {
    Snackbar(
        modifier = renderer.convertModifiers(modifiers),
        action = actionLabel?.let { label ->
            {
                TextButton(onClick = { /* Action handler */ }) {
                    Text(label)
                }
            }
        }
    ) {
        Text(message)
    }
}

/**
 * Render ProgressBarComponent to Material3 LinearProgressIndicator
 */
@Composable
fun ProgressBarComponent.Render(renderer: ComposeRenderer) {
    if (indeterminate) {
        LinearProgressIndicator(
            modifier = renderer.convertModifiers(modifiers).fillMaxWidth(),
            color = color.toComposeColor()
        )
    } else {
        val progress = value / max
        LinearProgressIndicator(
            progress = { progress },
            modifier = renderer.convertModifiers(modifiers).fillMaxWidth(),
            color = color.toComposeColor()
        )
    }
}

/**
 * Render Modal to Material3 Dialog
 */
@Composable
fun Modal.Render(renderer: ComposeRenderer) {
    val widthFraction = when (size) {
        ModalSize.SMALL -> 0.7f
        ModalSize.MEDIUM -> 0.85f
        ModalSize.LARGE -> 0.95f
        ModalSize.FULL_WIDTH -> 1f
        ModalSize.FULL_SCREEN -> 1f
    }

    Dialog(
        onDismissRequest = {
            if (dismissible) {
                onDismiss?.invoke()
            }
        }
    ) {
        Surface(
            modifier = renderer.convertModifiers(modifiers)
                .fillMaxWidth(widthFraction),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (dismissible) {
                        IconButton(onClick = { onDismiss?.invoke() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                content?.let { contentComponent ->
                    renderer.RenderComponent(contentComponent)
                }

                // Actions
                if (actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions.forEachIndexed { index, action ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            when (action.variant) {
                                ModalActionVariant.TEXT -> {
                                    TextButton(onClick = action.onClick) {
                                        Text(action.label)
                                    }
                                }
                                ModalActionVariant.OUTLINED -> {
                                    OutlinedButton(onClick = action.onClick) {
                                        Text(action.label)
                                    }
                                }
                                ModalActionVariant.FILLED -> {
                                    Button(onClick = action.onClick) {
                                        Text(action.label)
                                    }
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
 * Render Confirm to Material3 AlertDialog
 */
@Composable
fun Confirm.Render(renderer: ComposeRenderer) {
    val (containerColor, contentColor) = when (severity) {
        ConfirmSeverity.INFO -> AvanueTheme.colors.surface to AvanueTheme.colors.textPrimary
        ConfirmSeverity.WARNING -> AvanueTheme.colors.tertiaryContainer to AvanueTheme.colors.onTertiaryContainer
        ConfirmSeverity.ERROR -> AvanueTheme.colors.errorContainer to AvanueTheme.colors.onErrorContainer
        ConfirmSeverity.SUCCESS -> AvanueTheme.colors.primaryContainer to AvanueTheme.colors.onPrimaryContainer
    }

    AlertDialog(
        onDismissRequest = { onCancel?.invoke() },
        title = {
            Text(
                text = title,
                color = contentColor
            )
        },
        text = {
            Text(
                text = message,
                color = contentColor
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm?.invoke() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (severity) {
                        ConfirmSeverity.ERROR -> AvanueTheme.colors.error
                        else -> AvanueTheme.colors.primary
                    }
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = { onCancel?.invoke() }) {
                Text(cancelLabel)
            }
        },
        containerColor = containerColor,
        modifier = renderer.convertModifiers(modifiers)
    )
}

/**
 * Render ContextMenu to Material3 DropdownMenu
 */
@Composable
fun ContextMenu.Render(renderer: ComposeRenderer) {
    var expanded by remember { mutableStateOf(true) }

    Box(modifier = renderer.convertModifiers(modifiers)) {
        // Render anchor if provided
        anchor?.let { anchorComponent ->
            renderer.RenderComponent(anchorComponent)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                if (item.divider) {
                    HorizontalDivider()
                } else {
                    DropdownMenuItem(
                        text = { Text(item.label) },
                        onClick = {
                            item.onClick?.invoke()
                            expanded = false
                        },
                        enabled = !item.disabled,
                        leadingIcon = item.icon?.let { iconName ->
                            {
                                Icon(
                                    IconResolver.resolve(iconName),
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Render DialogComponent to Material3 AlertDialog
 */
@Composable
fun DialogComponent.Render(renderer: ComposeRenderer) {
    AlertDialog(
        onDismissRequest = { /* Dismissal handled by parent state */ },
        title = { Text(title) },
        text = { Text(content) },
        confirmButton = {
            TextButton(onClick = { /* Confirm action */ }) {
                Text(confirmLabel)
            }
        },
        dismissButton = cancelLabel?.let { label ->
            {
                TextButton(onClick = { /* Cancel action */ }) {
                    Text(label)
                }
            }
        },
        modifier = renderer.convertModifiers(modifiers)
    )
}
