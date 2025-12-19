package com.augmentalis.browseravanue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * IDEAMagic UI abstraction layer
 *
 * Architecture:
 * - Wrapper around Compose components
 * - Easy migration path to IDEAMagic
 * - Consistent API surface
 * - Type-safe component builders
 *
 * Migration Strategy:
 * 1. Phase 1 (Now): Use Compose underneath
 * 2. Phase 2 (Future): Replace with IDEAMagic implementations
 * 3. Code using these components doesn't change
 *
 * Components:
 * - MagicButton: Primary/Secondary/Tertiary buttons
 * - MagicTextField: Text input with validation
 * - MagicCard: Content containers
 * - MagicDialog: Modal dialogs
 * - MagicTopBar: App bars
 * - MagicBottomBar: Bottom navigation
 * - MagicList: Scrollable lists
 * - MagicChip: Tags/chips
 *
 * Design Tokens:
 * - Follows Material 3 design system
 * - Consistent spacing, colors, typography
 * - Dark mode support
 *
 * Usage:
 * ```
 * MagicButton(
 *     text = "New Tab",
 *     onClick = { viewModel.onEvent(BrowserEvent.NewTab) }
 * )
 * ```
 *
 * When IDEAMagic is ready:
 * - Replace @Composable with IDEAMagic equivalent
 * - Keep same function signatures
 * - Zero code changes in consuming code
 */

// ==========================================
// Buttons
// ==========================================

/**
 * Primary action button
 */
@Composable
fun MagicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text)
    }
}

/**
 * Secondary action button
 */
@Composable
fun MagicOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Text(text)
    }
}

/**
 * Tertiary/text button
 */
@Composable
fun MagicTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Text(text)
    }
}

/**
 * Icon button
 */
@Composable
fun MagicIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        icon()
    }
}

// ==========================================
// Text Fields
// ==========================================

/**
 * Text input field
 */
@Composable
fun MagicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth()
        )

        if (isError && errorText != null) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Search field (URL bar)
 */
@Composable
fun MagicSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search or enter URL",
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

// ==========================================
// Cards & Containers
// ==========================================

/**
 * Content card
 */
@Composable
fun MagicCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

/**
 * Surface container
 */
@Composable
fun MagicSurface(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = color
    ) {
        content()
    }
}

// ==========================================
// Dialogs
// ==========================================

/**
 * Alert dialog
 */
@Composable
fun MagicAlertDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = "OK",
    onConfirm: () -> Unit = onDismiss,
    dismissText: String? = "Cancel",
    onDismissAction: (() -> Unit)? = onDismiss
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismissAction ?: onDismiss) {
                    Text(it)
                }
            }
        }
    )
}

/**
 * Custom dialog
 */
@Composable
fun MagicDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {}, // Required but can be empty
        text = content
    )
}

// ==========================================
// App Bars
// ==========================================

/**
 * Top app bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = navigationIcon ?: {},
        actions = actions ?: {}
    )
}

/**
 * Bottom navigation bar
 */
@Composable
fun MagicBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    BottomAppBar(
        modifier = modifier
    ) {
        content()
    }
}

// ==========================================
// Lists & Grids
// ==========================================

/**
 * List item
 */
@Composable
fun MagicListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier,
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = leadingIcon,
        trailingContent = trailingIcon
    )
}

// ==========================================
// Chips & Tags
// ==========================================

/**
 * Chip component
 */
@Composable
fun MagicChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier,
        leadingIcon = leadingIcon
    )
}

// ==========================================
// Progress Indicators
// ==========================================

/**
 * Linear progress bar
 */
@Composable
fun MagicLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Circular progress indicator
 */
@Composable
fun MagicCircularProgress(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(
        modifier = modifier
    )
}

// ==========================================
// Switches & Checkboxes
// ==========================================

/**
 * Switch component
 */
@Composable
fun MagicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled
    )
}

/**
 * Checkbox component
 */
@Composable
fun MagicCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled
    )
}

// ==========================================
// Dividers & Spacers
// ==========================================

/**
 * Horizontal divider
 */
@Composable
fun MagicDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(modifier = modifier)
}

/**
 * Spacer with standard spacing
 */
@Composable
fun MagicSpacer(
    size: SpacingSize = SpacingSize.MEDIUM
) {
    Spacer(modifier = Modifier.height(size.dp))
}

/**
 * Standard spacing sizes
 */
enum class SpacingSize(val dp: androidx.compose.ui.unit.Dp) {
    SMALL(8.dp),
    MEDIUM(16.dp),
    LARGE(24.dp),
    XLARGE(32.dp)
}

// ==========================================
// Design Tokens
// ==========================================

/**
 * Standard spacing values
 */
object MagicSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/**
 * Standard corner radius
 */
object MagicRadius {
    val sm = 4.dp
    val md = 8.dp
    val lg = 16.dp
    val xl = 24.dp
}

/**
 * Migration notes for IDEAMagic:
 *
 * When IDEAMagic is ready, replace @Composable functions with:
 * - MagicButton → AVAMagicButton
 * - MagicTextField → AVAMagicTextField
 * - etc.
 *
 * Keep same function signatures and parameters.
 * No code changes needed in BrowserScreen.kt or other UI files.
 *
 * Example:
 * ```
 * // Before (Compose)
 * @Composable
 * fun MagicButton(...) {
 *     Button(...) // Compose implementation
 * }
 *
 * // After (IDEAMagic)
 * fun MagicButton(...): AVAMagicComponent {
 *     return AVAMagicButton(...) // IDEAMagic implementation
 * }
 * ```
 */
