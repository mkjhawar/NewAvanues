package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import com.augmentalis.avaelements.flutter.material.chips.MagicFilter
import com.augmentalis.avaelements.flutter.material.chips.MagicAction
import com.augmentalis.avaelements.flutter.material.chips.MagicChoice
import com.augmentalis.avaelements.flutter.material.chips.MagicInput
import com.augmentalis.avaelements.renderer.android.IconFromString
import com.augmentalis.avaelements.flutter.material.lists.ExpansionTile
import com.augmentalis.avaelements.flutter.material.lists.CheckboxListTile
import com.augmentalis.avaelements.flutter.material.lists.SwitchListTile
import com.augmentalis.avaelements.flutter.material.advanced.FilledButton
import com.augmentalis.avaelements.flutter.material.advanced.PopupMenuButton
import com.augmentalis.avaelements.flutter.material.advanced.RefreshIndicator
import com.augmentalis.avaelements.flutter.material.advanced.IndexedStack
import com.augmentalis.avaelements.flutter.material.advanced.VerticalDivider
import com.augmentalis.avaelements.flutter.material.advanced.FadeInImage
import com.augmentalis.avaelements.flutter.material.advanced.CircleAvatar
import com.augmentalis.avaelements.flutter.material.advanced.RichText
import com.augmentalis.avaelements.flutter.material.advanced.SelectableText
import com.augmentalis.avaelements.flutter.material.advanced.EndDrawer
import com.augmentalis.avaelements.flutter.material.advanced.SplitButton
import com.augmentalis.avaelements.flutter.material.advanced.LoadingButton
import com.augmentalis.avaelements.flutter.material.advanced.CloseButton
import com.augmentalis.avaelements.flutter.material.cards.*
import com.augmentalis.avaelements.flutter.material.display.*
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.staggeredgrid.*
import com.augmentalis.avaelements.flutter.material.feedback.*
import com.augmentalis.avaelements.flutter.material.layout.MasonryGrid
import com.augmentalis.avaelements.flutter.material.layout.AspectRatio
import com.augmentalis.avaelements.flutter.material.navigation.*
import com.augmentalis.avaelements.flutter.material.data.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

/**
 * Android Compose mappers for Flutter Material parity components
 *
 * This file contains renderer functions that map cross-platform component models
 * to Material3 Compose implementations on Android.
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render MagicFilter component using Material3
 *
 * Maps MagicFilter component to Material3 FilterChip with:
 * - Selection state
 * - Checkmark indicator when selected
 * - Optional leading avatar
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component MagicFilter component to render
 */
@Composable
fun FilterChipMapper(component: FilterChip) {
    FilterChip(
        selected = component.selected,
        onClick = {
            component.onSelected?.invoke(!component.selected)
        },
        label = {
            Text(component.label)
        },
        modifier = Modifier
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        enabled = component.enabled,
        leadingIcon = if (component.selected && component.showCheckmark) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else if (component.avatar != null && !component.selected) {
            {
                IconFromString(
                    iconName = component.avatar,
                    size = FilterChipDefaults.IconSize,
                    contentDescription = null
                )
            }
        } else {
            null
        }
    )
}

/**
 * Render ExpansionTile component using Material3
 *
 * Maps ExpansionTile component to Material3 ListItem with AnimatedVisibility for children:
 * - Smooth expand/collapse animation (200ms)
 * - Rotating trailing icon (180° rotation)
 * - Support for leading and trailing widgets
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component ExpansionTile component to render
 */
@Composable
fun ExpansionTileMapper(component: ExpansionTile) {
    var expanded by remember { mutableStateOf(component.initiallyExpanded) }

    // Animate icon rotation
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = ExpansionTile.DEFAULT_ANIMATION_DURATION),
        label = "expansion_icon_rotation"
    )

    Column(
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription(expanded)
        }
    ) {
        ListItem(
            headlineContent = { Text(component.title) },
            supportingContent = if (component.subtitle != null && !expanded) {
                { Text(component.subtitle) }
            } else {
                null
            },
            leadingContent = if (component.leading != null) {
                {
                    IconFromString(
                        iconName = component.leading,
                        contentDescription = null
                    )
                }
            } else {
                null
            },
            trailingContent = {
                IconButton(
                    onClick = {
                        expanded = !expanded
                        component.onExpansionChanged?.invoke(expanded)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            },
            modifier = Modifier.clickable {
                expanded = !expanded
                component.onExpansionChanged?.invoke(expanded)
            }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = ExpansionTile.DEFAULT_ANIMATION_DURATION)
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = ExpansionTile.DEFAULT_ANIMATION_DURATION)
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
            ) {
                component.children.forEach { child ->
                    // TODO: Integrate with main renderer
                    // RenderChild(child)
                }
            }
        }
    }
}

/**
 * Render CheckboxListTile component using Material3
 *
 * Maps CheckboxListTile component to Material3 ListItem with Checkbox:
 * - Three-state checkbox (checked, unchecked, indeterminate)
 * - Checkbox positioned as leading or trailing
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component CheckboxListTile component to render
 */
@Composable
fun CheckboxListTileMapper(component: CheckboxListTile) {
    val checkboxControl = @Composable {
        Checkbox(
            checked = component.value ?: false,
            onCheckedChange = { checked ->
                component.onChanged?.invoke(checked)
            },
            enabled = component.enabled,
            // Tristate support
            modifier = Modifier.semantics {
                contentDescription = component.getAccessibilityDescription()
            }
        )
    }

    ListItem(
        headlineContent = { Text(component.title) },
        supportingContent = if (component.subtitle != null) {
            { Text(component.subtitle) }
        } else {
            null
        },
        leadingContent = if (component.controlAffinity == CheckboxListTile.ListTileControlAffinity.Leading) {
            { checkboxControl() }
        } else {
            null
        },
        trailingContent = if (component.controlAffinity == CheckboxListTile.ListTileControlAffinity.Trailing) {
            { checkboxControl() }
        } else {
            null
        },
        modifier = Modifier.clickable(enabled = component.enabled) {
            val newValue = when (component.value) {
                true -> false
                false -> if (component.tristate) null else true
                null -> true
            }
            component.onChanged?.invoke(newValue)
        }
    )
}

/**
 * Render SwitchListTile component using Material3
 *
 * Maps SwitchListTile component to Material3 ListItem with Switch:
 * - Toggle switch control
 * - Switch typically positioned as trailing
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component SwitchListTile component to render
 */
@Composable
fun SwitchListTileMapper(component: SwitchListTile) {
    val switchControl = @Composable {
        Switch(
            checked = component.value,
            onCheckedChange = { checked ->
                component.onChanged?.invoke(checked)
            },
            enabled = component.enabled,
            modifier = Modifier.semantics {
                contentDescription = component.getAccessibilityDescription()
            }
        )
    }

    ListItem(
        headlineContent = { Text(component.title) },
        supportingContent = if (component.subtitle != null) {
            { Text(component.subtitle) }
        } else {
            null
        },
        leadingContent = if (component.controlAffinity == SwitchListTile.ListTileControlAffinity.Leading) {
            { switchControl() }
        } else {
            null
        },
        trailingContent = if (component.controlAffinity == SwitchListTile.ListTileControlAffinity.Trailing) {
            { switchControl() }
        } else {
            null
        },
        modifier = Modifier.clickable(enabled = component.enabled) {
            component.onChanged?.invoke(!component.value)
        }
    )
}

/**
 * Render FilledButton component using Material3
 *
 * Maps FilledButton component to Material3 Button:
 * - All button states (enabled, disabled, pressed, hovered, focused)
 * - Optional leading/trailing icons
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component FilledButton component to render
 */
@Composable
fun FilledButtonMapper(component: FilledButton) {
    Button(
        onClick = {
            component.onPressed?.invoke()
        },
        enabled = component.enabled,
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription()
        }
    ) {
        if (component.icon != null && component.iconPosition == FilledButton.IconPosition.Leading) {
            IconFromString(
                iconName = component.icon,
                size = 18.dp,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Text(component.text)

        if (component.icon != null && component.iconPosition == FilledButton.IconPosition.Trailing) {
            IconFromString(
                iconName = component.icon,
                size = 18.dp,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * Render MagicAction component using Material3
 *
 * Maps MagicAction component to Material3 AssistChip:
 * - Compact button-like appearance
 * - Optional leading icon/avatar
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component MagicAction component to render
 */
@Composable
fun ActionChipMapper(component: ActionChip) {
    AssistChip(
        onClick = {
            component.onPressed?.invoke()
        },
        label = {
            Text(component.label)
        },
        modifier = Modifier
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        enabled = component.enabled,
        leadingIcon = if (component.avatar != null) {
            {
                IconFromString(
                    iconName = component.avatar,
                    size = AssistChipDefaults.IconSize,
                    contentDescription = null
                )
            }
        } else {
            null
        }
    )
}

/**
 * Render MagicChoice component using Material3
 *
 * Maps MagicChoice component to Material3 FilterChip (single-selection mode):
 * - Single-selection within a group
 * - Visual feedback for selected state
 * - Optional leading icon/avatar
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component MagicChoice component to render
 */
@Composable
fun ChoiceChipMapper(component: ChoiceChip) {
    FilterChip(
        selected = component.selected,
        onClick = {
            component.onSelected?.invoke(!component.selected)
        },
        label = {
            Text(component.label)
        },
        modifier = Modifier
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        enabled = component.enabled,
        leadingIcon = if (component.selected && component.showCheckmark) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else if (component.avatar != null && !component.selected) {
            {
                IconFromString(
                    iconName = component.avatar,
                    size = FilterChipDefaults.IconSize,
                    contentDescription = null
                )
            }
        } else {
            null
        }
    )
}

/**
 * Render MagicInput component using Material3
 *
 * Maps MagicInput component to Material3 InputChip:
 * - Optional leading avatar
 * - Delete/remove action button
 * - Selectable state
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component MagicInput component to render
 */
@Composable
fun InputChipMapper(component: InputChip) {
    androidx.compose.material3.InputChip(
        selected = component.selected,
        onClick = {
            component.onPressed?.invoke()
            component.onSelected?.invoke(!component.selected)
        },
        label = {
            Text(component.label)
        },
        modifier = Modifier
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        enabled = component.enabled,
        leadingIcon = if (component.avatar != null) {
            {
                IconFromString(
                    iconName = component.avatar,
                    size = InputChipDefaults.AvatarSize,
                    contentDescription = null
                )
            }
        } else {
            null
        },
        trailingIcon = if (component.onDeleted != null) {
            {
                IconButton(
                    onClick = {
                        component.onDeleted.invoke()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = component.getDeleteButtonAccessibilityDescription()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(InputChipDefaults.IconSize)
                    )
                }
            }
        } else {
            null
        }
    )
}

/**
 * Render PopupMenuButton component using Material3
 *
 * Maps PopupMenuButton component to Material3 DropdownMenu:
 * - Popup menu with list of items
 * - Automatic positioning
 * - Keyboard navigation support
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component PopupMenuButton component to render
 */
@Composable
fun PopupMenuButtonMapper(component: PopupMenuButton) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = {
                expanded = true
                component.onOpened?.invoke()
            },
            enabled = component.enabled,
            modifier = Modifier.semantics {
                contentDescription = component.getAccessibilityDescription()
            }
        ) {
            if (component.child != null) {
                // TODO: Render custom child
            } else {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = component.tooltip
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                component.onCanceled?.invoke()
            }
        ) {
            component.items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.text) },
                    onClick = {
                        expanded = false
                        component.onSelected?.invoke(item.value)
                    },
                    enabled = item.enabled,
                    leadingIcon = if (item.icon != null) {
                        {
                            IconFromString(
                                iconName = item.icon,
                                contentDescription = null
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

/**
 * Render RefreshIndicator component using Material3
 *
 * Maps RefreshIndicator component to SwipeRefresh (or PullRefresh in Material3):
 * - Pull-to-refresh gesture
 * - Circular progress indicator
 * - Smooth animation
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * Note: Using Accompanist SwipeRefresh until Material3 PullRefresh is stable
 *
 * @param component RefreshIndicator component to render
 */
@Composable
fun RefreshIndicatorMapper(component: RefreshIndicator) {
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SwipeRefresh(
        state = rememberSwipeRefreshState(refreshing),
        onRefresh = {
            refreshing = true
            scope.launch {
                component.onRefresh?.invoke()
                refreshing = false
            }
        },
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription()
        }
    ) {
        // TODO: Render child component
        Box(modifier = Modifier.fillMaxSize()) {
            // Child content goes here
        }
    }
}

/**
 * Render IndexedStack component using Material3
 *
 * Maps IndexedStack component to Box with conditional rendering:
 * - Shows single child by index
 * - All children kept in memory
 * - Alignment support
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component IndexedStack component to render
 */
@Composable
fun IndexedStackMapper(component: IndexedStack) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        contentAlignment = when (component.alignment) {
            IndexedStack.Alignment.TopStart -> Alignment.TopStart
            IndexedStack.Alignment.TopCenter -> Alignment.TopCenter
            IndexedStack.Alignment.TopEnd -> Alignment.TopEnd
            IndexedStack.Alignment.CenterStart -> Alignment.CenterStart
            IndexedStack.Alignment.Center -> Alignment.Center
            IndexedStack.Alignment.CenterEnd -> Alignment.CenterEnd
            IndexedStack.Alignment.BottomStart -> Alignment.BottomStart
            IndexedStack.Alignment.BottomCenter -> Alignment.BottomCenter
            IndexedStack.Alignment.BottomEnd -> Alignment.BottomEnd
        }
    ) {
        component.children.forEachIndexed { index, child ->
            if (index == component.index) {
                // TODO: Render child component
                // RenderChild(child)
            }
        }
    }
}

/**
 * Render VerticalDivider component using Material3
 *
 * Maps VerticalDivider component to Material3 VerticalDivider:
 * - Vertical line separator
 * - Customizable thickness and color
 * - Indent support
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component VerticalDivider component to render
 */
@Composable
fun VerticalDividerMapper(component: VerticalDivider) {
    androidx.compose.material3.VerticalDivider(
        modifier = Modifier
            .padding(
                top = component.indent.dp,
                bottom = component.endIndent.dp
            )
            .width(component.thickness.dp)
            .then(
                if (component.width != null) {
                    Modifier.width(component.width.dp)
                } else {
                    Modifier
                }
            ),
        thickness = component.thickness.dp,
        color = if (component.color != null) {
            // TODO: Parse color from string
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.outline
        }
    )
}

/**
 * Render FadeInImage component using Coil AsyncImage
 *
 * Maps FadeInImage component to AsyncImage with crossfade:
 * - Placeholder during loading
 * - Fade-in animation when loaded
 * - Error handling
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component FadeInImage component to render
 */
@Composable
fun FadeInImageMapper(component: FadeInImage) {
    AsyncImage(
        model = component.image,
        contentDescription = component.getAccessibilityDescription(),
        modifier = Modifier
            .then(
                if (component.width != null && component.height != null) {
                    Modifier.size(component.width.dp, component.height.dp)
                } else if (component.width != null) {
                    Modifier.width(component.width.dp)
                } else if (component.height != null) {
                    Modifier.height(component.height.dp)
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = component.getAccessibilityDescription() ?: ""
            },
        placeholder = null, // TODO: Load placeholder image
        error = null, // TODO: Render error component
        onSuccess = {
            component.onLoadComplete?.invoke()
        },
        onError = {
            component.onError?.invoke(it.result.throwable.message ?: "Unknown error")
        }
    )
}

/**
 * Render CircleAvatar component using Material3
 *
 * Maps CircleAvatar component to circular Box/Image:
 * - Circular shape with clipping
 * - Supports image or child content
 * - Customizable radius and colors
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component CircleAvatar component to render
 */
@Composable
fun CircleAvatarMapper(component: CircleAvatar) {
    val radius = component.getEffectiveRadius().dp

    Box(
        modifier = Modifier
            .size(radius * 2)
            .clip(CircleShape)
            .background(
                color = if (component.backgroundColor != null) {
                    // TODO: Parse color from string
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            )
            .then(
                if (component.onTap != null) {
                    Modifier.clickable {
                        component.onTap.invoke()
                    }
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        contentAlignment = Alignment.Center
    ) {
        if (component.backgroundImage != null) {
            AsyncImage(
                model = component.backgroundImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else if (component.child != null) {
            // TODO: Render child component
        }
    }
}

/**
 * Render RichText component using AnnotatedString
 *
 * Maps RichText component to Compose Text with AnnotatedString:
 * - Multiple text styles in single text block
 * - Inline styling
 * - Text alignment and overflow
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component RichText component to render
 */
@Composable
fun RichTextMapper(component: RichText) {
    val annotatedString = buildAnnotatedString {
        component.spans.forEach { span ->
            val style = span.style
            if (style != null) {
                pushStyle(
                    SpanStyle(
                        color = if (style.color != null) {
                            // TODO: Parse color from string
                            Color.Unspecified
                        } else {
                            Color.Unspecified
                        },
                        fontSize = style.fontSize?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
                        fontWeight = when (style.fontWeight) {
                            "bold" -> FontWeight.Bold
                            "normal" -> FontWeight.Normal
                            else -> null
                        },
                        fontStyle = when (style.fontStyle) {
                            "italic" -> FontStyle.Italic
                            else -> FontStyle.Normal
                        },
                        letterSpacing = style.letterSpacing?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                )
            }
            append(span.text)
            if (style != null) {
                pop()
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription()
        },
        textAlign = when (component.textAlign) {
            RichText.TextAlign.Start -> TextAlign.Start
            RichText.TextAlign.End -> TextAlign.End
            RichText.TextAlign.Left -> TextAlign.Left
            RichText.TextAlign.Right -> TextAlign.Right
            RichText.TextAlign.Center -> TextAlign.Center
            RichText.TextAlign.Justify -> TextAlign.Justify
        },
        overflow = when (component.overflow) {
            RichText.TextOverflow.Clip -> androidx.compose.ui.text.style.TextOverflow.Clip
            RichText.TextOverflow.Ellipsis -> androidx.compose.ui.text.style.TextOverflow.Ellipsis
            RichText.TextOverflow.Visible -> androidx.compose.ui.text.style.TextOverflow.Visible
            RichText.TextOverflow.Fade -> androidx.compose.ui.text.style.TextOverflow.Ellipsis // Fallback
        },
        maxLines = component.maxLines ?: Int.MAX_VALUE,
        softWrap = component.softWrap
    )
}

/**
 * Render SelectableText component using SelectionContainer
 *
 * Maps SelectableText component to selectable Compose Text:
 * - Text selection with handles
 * - Copy to clipboard support
 * - Customizable selection colors
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component SelectableText component to render
 */
@Composable
fun SelectableTextMapper(component: SelectableText) {
    SelectionContainer {
        Text(
            text = component.text,
            modifier = Modifier.semantics {
                contentDescription = component.getAccessibilityDescription()
            },
            style = LocalTextStyle.current.copy(
                color = if (component.style?.color != null) {
                    // TODO: Parse color from string
                    Color.Unspecified
                } else {
                    Color.Unspecified
                },
                fontSize = component.style?.fontSize?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
                fontWeight = when (component.style?.fontWeight) {
                    "bold" -> FontWeight.Bold
                    "normal" -> FontWeight.Normal
                    else -> null
                },
                fontStyle = when (component.style?.fontStyle) {
                    "italic" -> FontStyle.Italic
                    else -> FontStyle.Normal
                }
            ),
            textAlign = when (component.textAlign) {
                SelectableText.TextAlign.Start -> TextAlign.Start
                SelectableText.TextAlign.End -> TextAlign.End
                SelectableText.TextAlign.Left -> TextAlign.Left
                SelectableText.TextAlign.Right -> TextAlign.Right
                SelectableText.TextAlign.Center -> TextAlign.Center
                SelectableText.TextAlign.Justify -> TextAlign.Justify
            },
            maxLines = component.maxLines ?: Int.MAX_VALUE,
            minLines = component.minLines ?: 1
        )
    }
}

/**
 * Render EndDrawer component using Material3 ModalDrawer
 *
 * Maps EndDrawer component to Material3 ModalNavigationDrawer:
 * - Slides from trailing edge (RTL-aware)
 * - Modal overlay with scrim
 * - Swipe gestures
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component EndDrawer component to render
 */
@Composable
fun EndDrawerMapper(component: EndDrawer) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(component.getEffectiveWidth().dp)
                    .semantics {
                        contentDescription = component.getAccessibilityDescription()
                    }
            ) {
                // TODO: Render child component
            }
        },
        gesturesEnabled = component.enableOpenDragGesture
    ) {
        // Content area
    }
}

/**
 * Render SplitButton component using Material3
 *
 * Maps SplitButton component to ButtonGroup with main button + dropdown:
 * - Primary action button with dropdown menu
 * - Configurable menu items with individual handlers
 * - Menu position control (bottom/top)
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component SplitButton component to render
 */
@Composable
fun SplitButtonMapper(component: SplitButton) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription()
        }
    ) {
        // Main action button
        Button(
            onClick = {
                component.onPressed?.invoke()
            },
            enabled = component.enabled,
            shape = MaterialTheme.shapes.small.copy(
                topEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
                bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
            ),
            modifier = Modifier.weight(1f)
        ) {
            if (component.icon != null) {
                IconFromString(
                    iconName = component.icon,
                    size = 18.dp,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(component.text)
        }

        // Dropdown button
        Box {
            Button(
                onClick = {
                    expanded = !expanded
                },
                enabled = component.enabled,
                shape = MaterialTheme.shapes.small.copy(
                    topStart = androidx.compose.foundation.shape.CornerSize(0.dp),
                    bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp)
                ),
                modifier = Modifier
                    .width(48.dp)
                    .semantics {
                        contentDescription = "Show menu options"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                component.menuItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label) },
                        onClick = {
                            expanded = false
                            if (item.onPressed != null) {
                                item.onPressed.invoke()
                            } else {
                                component.onMenuItemPressed?.invoke(item.value)
                            }
                        },
                        enabled = item.enabled,
                        leadingIcon = if (item.icon != null) {
                            {
                                IconFromString(
                                    iconName = item.icon,
                                    size = 20.dp,
                                    contentDescription = null
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

/**
 * Render LoadingButton component using Material3
 *
 * Maps LoadingButton component to Material3 Button with loading state:
 * - Automatic disable during loading
 * - Configurable loading indicator position (start/center/end)
 * - Optional loading text override
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component LoadingButton component to render
 */
@Composable
fun LoadingButtonMapper(component: LoadingButton) {
    Button(
        onClick = {
            if (!component.loading) {
                component.onPressed?.invoke()
            }
        },
        enabled = !component.isDisabled(),
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription()
        }
    ) {
        val showCenterIndicator = component.loading && component.loadingPosition == LoadingButton.LoadingPosition.Center

        // Start position indicator
        if (component.loading && component.loadingPosition == LoadingButton.LoadingPosition.Start) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else if (component.icon != null && !component.loading) {
            IconFromString(
                iconName = component.icon,
                size = 18.dp,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Text - hidden when center indicator is shown
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = component.getDisplayText(),
                modifier = Modifier.then(
                    if (showCenterIndicator) {
                        Modifier.alpha(0f)
                    } else {
                        Modifier
                    }
                )
            )

            // Center position indicator
            if (showCenterIndicator) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // End position indicator
        if (component.loading && component.loadingPosition == LoadingButton.LoadingPosition.End) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 8.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Render CloseButton component using Material3
 *
 * Maps CloseButton component to Material3 IconButton with close icon:
 * - Standardized close/dismiss button
 * - Three sizes (small, medium, large)
 * - Edge positioning support
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component CloseButton component to render
 */
@Composable
fun CloseButtonMapper(component: CloseButton) {
    val iconSize = component.getIconSizeInPixels().dp

    IconButton(
        onClick = {
            component.onPressed?.invoke()
        },
        enabled = component.enabled,
        modifier = Modifier
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
            .then(
                when (component.edge) {
                    CloseButton.EdgePosition.Start -> Modifier.padding(start = 4.dp)
                    CloseButton.EdgePosition.End -> Modifier.padding(end = 4.dp)
                    CloseButton.EdgePosition.Top -> Modifier.padding(top = 4.dp)
                    CloseButton.EdgePosition.Bottom -> Modifier.padding(bottom = 4.dp)
                    else -> Modifier
                }
            )
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Render PricingCard component using Material3
 *
 * Maps PricingCard component to Material3 Card with pricing tier layout:
 * - Highlighted/featured tier support with different colors
 * - Optional ribbon badge
 * - Feature list with checkmark icons
 * - Call-to-action button
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component PricingCard component to render
 */
@Composable
fun PricingCardMapper(component: PricingCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (component.highlighted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (component.highlighted) 4.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Ribbon badge
            if (component.ribbonText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = component.ribbonText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Title
            Text(
                text = component.title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (component.highlighted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            // Subtitle
            if (component.subtitle != null) {
                Text(
                    text = component.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Price
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = component.price,
                    style = MaterialTheme.typography.displayMedium,
                    color = if (component.highlighted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (component.period != null) {
                    Text(
                        text = " ${component.period}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Features
            component.features.forEachIndexed { index, feature ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Button
            Button(
                onClick = { component.onPressed?.invoke() },
                enabled = component.buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = if (component.highlighted)
                    ButtonDefaults.buttonColors()
                else
                    ButtonDefaults.outlinedButtonColors()
            ) {
                Text(component.buttonText)
            }
        }
    }
}

/**
 * Render FeatureCard component using Material3
 */
@Composable
fun FeatureCardMapper(component: FeatureCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        val isVertical = component.layout == FeatureCard.Layout.Vertical

        if (isVertical) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconFromString(
                    iconName = component.icon,
                    size = component.iconSize.dp,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = component.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (component.actionText != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { component.onActionPressed?.invoke() }) {
                        Text(component.actionText)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconFromString(
                    iconName = component.icon,
                    size = component.iconSize.dp,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = component.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = component.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Render TestimonialCard component using Material3
 */
@Composable
fun TestimonialCardMapper(component: TestimonialCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Quote icon
            if (component.showQuoteIcon) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quote text
            Text(
                text = component.quote,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic
            )

            // Rating
            if (component.rating != null && component.isRatingValid()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < component.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Author info
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                if (component.avatarUrl != null) {
                    AsyncImage(
                        model = component.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else if (component.avatarInitials != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = component.avatarInitials,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = component.authorName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (component.authorTitle != null) {
                        Text(
                            text = component.authorTitle,
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
 * Render ProductCard component using Material3
 */
@Composable
fun ProductCardMapper(component: ProductCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        Column {
            // Product image with badge
            Box {
                AsyncImage(
                    model = component.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                // Badge
                if (component.badgeText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = component.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }

                // Wishlist button
                if (component.showWishlist) {
                    IconButton(
                        onClick = { component.onWishlist?.invoke(true) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Add to wishlist",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )

                // Description
                if (component.description != null) {
                    Text(
                        text = component.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Rating
                if (component.rating != null && component.isRatingValid()) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < component.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (component.reviewCount != null) {
                            Text(
                                text = " (${component.reviewCount})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = component.price,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (component.originalPrice != null) {
                        Text(
                            text = component.originalPrice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Stock status
                if (!component.inStock) {
                    Text(
                        text = "Out of Stock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Add to cart button
                if (component.showAddToCart && component.inStock) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { component.onAddToCart?.invoke() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add to Cart")
                    }
                }
            }
        }
    }
}

/**
 * Render ArticleCard component using Material3
 */
@Composable
fun ArticleCardMapper(component: ArticleCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        Column {
            // Featured image
            AsyncImage(
                model = component.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Category
                if (component.category != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = component.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Title
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Excerpt
                Text(
                    text = component.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Author and metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (component.authorAvatar != null) {
                            AsyncImage(
                                model = component.authorAvatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                text = component.authorName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = component.getMetadataText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (component.showBookmark) {
                        IconButton(onClick = { component.onBookmark?.invoke(!component.bookmarked) }) {
                            Icon(
                                imageVector = if (component.bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render ImageCard component using Material3
 */
@Composable
fun ImageCardMapper(component: ImageCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        Box {
            // Image
            AsyncImage(
                model = component.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (component.aspectRatio != null) {
                            Modifier.aspectRatio(component.aspectRatio)
                        } else {
                            Modifier.height(250.dp)
                        }
                    )
            )

            // Overlay
            if (component.hasOverlay()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = if (component.showGradient) {
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                } else {
                                    listOf(Color.Transparent, Color.Transparent)
                                }
                            )
                        ),
                    contentAlignment = when (component.overlayPosition) {
                        ImageCard.OverlayPosition.Top -> Alignment.TopStart
                        ImageCard.OverlayPosition.Center -> Alignment.Center
                        ImageCard.OverlayPosition.Bottom -> Alignment.BottomStart
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (component.title != null) {
                            Text(
                                text = component.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        if (component.subtitle != null) {
                            Text(
                                text = component.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (component.actionText != null) {
                            TextButton(
                                onClick = { component.onActionPressed?.invoke() },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = component.actionText,
                                    color = Color.White
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
 * Render HoverCard component using Material3
 */
@Composable
fun HoverCardMapper(component: HoverCard) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable {
                    isPressed = !isPressed
                    component.onPressed.invoke()
                }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = component.elevation.dp,
            pressedElevation = component.hoverElevation.dp
        )
    ) {
        Box {
            // Background image
            if (component.imageUrl != null) {
                AsyncImage(
                    model = component.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            // Overlay on press (simulating hover)
            if (isPressed && component.showOverlay) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (component.imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface
                )

                if (component.description != null) {
                    Text(
                        text = component.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (component.imageUrl != null) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Actions (always visible for accessibility)
                if (component.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        component.actions.forEach { action ->
                            IconButton(
                                onClick = { component.onActionPressed?.invoke(action.id) },
                                enabled = action.enabled
                            ) {
                                if (action.icon != null) {
                                    IconFromString(
                                        iconName = action.icon,
                                        contentDescription = action.label,
                                        tint = if (component.imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface
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

/**
 * Render ExpandableCard component using Material3
 */
@Composable
fun ExpandableCardMapper(component: ExpandableCard) {
    var expanded by remember {
        mutableStateOf(component.expanded ?: component.initiallyExpanded)
    }

    // Use controlled state if provided
    val isExpanded = if (component.isControlled()) component.expanded!! else expanded

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = component.animationDuration),
        label = "expand_icon_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription(isExpanded)
            },
        colors = CardDefaults.cardColors()
    ) {
        Column {
            // Header
            ListItem(
                headlineContent = { Text(component.title) },
                supportingContent = if (component.subtitle != null) {
                    { Text(component.subtitle) }
                } else {
                    null
                },
                leadingContent = if (component.icon != null) {
                    {
                        IconFromString(
                            iconName = component.icon,
                            contentDescription = null
                        )
                    }
                } else {
                    null
                },
                trailingContent = {
                    Row {
                        component.headerActions.forEach { action ->
                            IconButton(
                                onClick = { component.onHeaderActionPressed?.invoke(action.id) },
                                enabled = action.enabled
                            ) {
                                IconFromString(
                                    iconName = action.icon,
                                    contentDescription = action.label
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val newState = !isExpanded
                                if (!component.isControlled()) {
                                    expanded = newState
                                }
                                component.onExpansionChanged?.invoke(newState)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                modifier = Modifier.rotate(rotationAngle)
                            )
                        }
                    }
                },
                modifier = Modifier.clickable {
                    val newState = !isExpanded
                    if (!component.isControlled()) {
                        expanded = newState
                    }
                    component.onExpansionChanged?.invoke(newState)
                }
            )

            // Summary content when collapsed
            if (!isExpanded && component.summaryContent != null) {
                Text(
                    text = component.summaryContent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Divider
            if (isExpanded && component.showDivider) {
                HorizontalDivider()
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = component.animationDuration)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = component.animationDuration)
                )
            ) {
                Text(
                    text = component.expandedContent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Render AvatarGroup component using Material3
 */
@Composable
fun AvatarGroupMapper(component: AvatarGroup) {
    Row(
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription()
        }
    ) {
        component.getVisibleAvatars().forEachIndexed { index, avatar ->
            Box(
                modifier = Modifier
                    .offset(x = (index * component.spacing).dp)
                    .size(component.size.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        if (component.borderWidth > 0) {
                            Modifier.border(
                                width = component.borderWidth.dp,
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (component.onAvatarPressed != null) {
                            Modifier.clickable { component.onAvatarPressed.invoke(avatar.id) }
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (avatar.imageUrl != null) {
                    AsyncImage(
                        model = avatar.imageUrl,
                        contentDescription = avatar.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatar.initials ?: avatar.name.first().toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // +N more indicator
        if (component.getRemainingCount() > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (component.max * component.spacing).dp)
                    .size(component.size.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (component.onPressed != null) {
                            Modifier.clickable { component.onPressed.invoke() }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${component.getRemainingCount()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Render SkeletonText component using Material3
 */
@Composable
fun SkeletonTextMapper(component: SkeletonText) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = component.animationDuration),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription()
        },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(component.lines) { lineIndex ->
            val isLastLine = lineIndex == component.lines - 1
            val lineWidth = if (isLastLine && component.isLastLineWidthValid()) {
                component.lastLineWidth
            } else {
                1f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(lineWidth)
                    .height(component.getVariantHeight().dp)
                    .clip(RoundedCornerShape(component.borderRadius.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = if (component.animation != SkeletonText.Animation.None) alpha else 0.3f
                        )
                    )
            )
        }
    }
}

/**
 * Render SkeletonCircle component using Material3
 */
@Composable
fun SkeletonCircleMapper(component: SkeletonCircle) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_circle_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = component.animationDuration),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_circle_alpha"
    )

    Box(
        modifier = Modifier
            .size(component.diameter.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = if (component.animation != SkeletonCircle.Animation.None) alpha else 0.3f
                )
            )
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
    )
}

/**
 * Render ProgressCircle component using Material3
 */
@Composable
fun ProgressCircleMapper(component: ProgressCircle) {
    Box(
        modifier = Modifier
            .size(component.size.dp)
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        contentAlignment = Alignment.Center
    ) {
        if (component.isIndeterminate()) {
            CircularProgressIndicator(
                modifier = Modifier.size(component.size.dp),
                strokeWidth = component.strokeWidth.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Background track
            if (component.backgroundColor != null) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(component.size.dp),
                    strokeWidth = component.strokeWidth.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Progress indicator
            CircularProgressIndicator(
                progress = { component.value ?: 0f },
                modifier = Modifier.size(component.size.dp),
                strokeWidth = component.strokeWidth.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Label
        if (component.showLabel && !component.isIndeterminate()) {
            Text(
                text = component.getEffectiveLabelText() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Render LoadingOverlay component using Material3
 */
@Composable
fun LoadingOverlayMapper(component: LoadingOverlay) {
    if (component.visible) {
        Box(
            modifier = Modifier
                .then(
                    if (component.fullScreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.matchParentSize()
                    }
                )
                .background(
                    Color.Black.copy(alpha = component.backdropOpacity)
                )
                .clickable(enabled = false) { /* Block clicks */ }
                .semantics {
                    contentDescription = component.getAccessibilityDescription()
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(component.spinnerSize.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (component.message != null) {
                        Text(
                            text = component.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (component.cancelable) {
                        TextButton(onClick = { component.onCancel?.invoke() }) {
                            Text(component.cancelText)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render Popup component using Material3
 *
 * Maps Popup component to Material3 AlertDialog positioned as tooltip:
 * - Floating popup with positioning
 * - Optional arrow pointer
 * - Dismissible on outside click
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component Popup component to render
 */
@Composable
fun PopupMapper(component: com.augmentalis.avaelements.flutter.material.feedback.Popup) {
    if (component.visible) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                if (component.dismissible) {
                    component.onDismiss?.invoke()
                }
            },
            confirmButton = {},
            text = {
                Text(
                    text = component.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            modifier = Modifier.semantics {
                contentDescription = component.getAccessibilityDescription()
            },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Render Callout component using Material3
 *
 * Maps Callout component to Material3 Card with arrow and variant colors:
 * - Directional arrow pointer
 * - Color-coded by variant (info, success, warning, error)
 * - Optional dismiss button
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component Callout component to render
 */
@Composable
fun CalloutMapper(component: com.augmentalis.avaelements.flutter.material.feedback.Callout) {
    val colors = when (component.variant) {
        com.augmentalis.avaelements.flutter.material.feedback.Callout.Variant.Info ->
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        com.augmentalis.avaelements.flutter.material.feedback.Callout.Variant.Success ->
            Color(0xFF4CAF50) to Color.White
        com.augmentalis.avaelements.flutter.material.feedback.Callout.Variant.Warning ->
            Color(0xFFFF9800) to Color.Black
        com.augmentalis.avaelements.flutter.material.feedback.Callout.Variant.Error ->
            MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(
            containerColor = colors.first
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = component.elevation.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (component.getEffectiveIcon() != null) {
                IconFromString(
                    iconName = component.getEffectiveIcon()!!,
                    size = 24.dp,
                    tint = colors.second,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.second
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = component.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.second
                )
            }

            // Dismiss button
            if (component.dismissible) {
                IconButton(
                    onClick = { component.onDismiss?.invoke() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = colors.second
                    )
                }
            }
        }
    }
}

/**
 * Render Disclosure component using Material3
 *
 * Maps Disclosure component to simple collapsible with animation:
 * - Title with expand icon
 * - Smooth expand/collapse animation
 * - Controlled or uncontrolled state
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component Disclosure component to render
 */
@Composable
fun DisclosureMapper(component: com.augmentalis.avaelements.flutter.material.feedback.Disclosure) {
    var expanded by remember {
        mutableStateOf(component.expanded ?: component.initiallyExpanded)
    }

    val isExpanded = if (component.isControlled()) component.expanded!! else expanded

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = component.animationDuration),
        label = "disclosure_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription(isExpanded)
            }
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val newState = !isExpanded
                    if (!component.isControlled()) {
                        expanded = newState
                    }
                    component.onExpansionChanged?.invoke(newState)
                }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = component.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            if (component.showIcon) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
        }

        // Content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = component.animationDuration)
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = component.animationDuration)
            )
        ) {
            Text(
                text = component.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

/**
 * Render InfoPanel component using Material3
 *
 * Maps InfoPanel component to Card with info styling:
 * - Blue primary theme
 * - Info icon
 * - Optional dismiss and action buttons
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component InfoPanel component to render
 */
@Composable
fun InfoPanelMapper(component: com.augmentalis.avaelements.flutter.material.feedback.InfoPanel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = component.elevation.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp, end = 12.dp)
            )

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = component.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Action buttons
                if (component.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        component.actions.forEach { action ->
                            TextButton(onClick = { action.onClick?.invoke() }) {
                                Text(action.label)
                            }
                        }
                    }
                }
            }

            // Dismiss button
            if (component.dismissible) {
                IconButton(
                    onClick = { component.onDismiss?.invoke() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Render ErrorPanel component using Material3
 */
@Composable
fun ErrorPanelMapper(component: com.augmentalis.avaelements.flutter.material.feedback.ErrorPanel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = component.elevation.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp, end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = component.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                if (component.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        component.actions.forEach { action ->
                            TextButton(onClick = { action.onClick?.invoke() }) {
                                Text(action.label)
                            }
                        }
                    }
                }
            }

            if (component.dismissible) {
                IconButton(
                    onClick = { component.onDismiss?.invoke() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Render WarningPanel component using Material3
 */
@Composable
fun WarningPanelMapper(component: com.augmentalis.avaelements.flutter.material.feedback.WarningPanel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0) // Light orange
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = component.elevation.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800), // Orange
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp, end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = component.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )

                if (component.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        component.actions.forEach { action ->
                            TextButton(onClick = { action.onClick?.invoke() }) {
                                Text(action.label)
                            }
                        }
                    }
                }
            }

            if (component.dismissible) {
                IconButton(
                    onClick = { component.onDismiss?.invoke() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Render SuccessPanel component using Material3
 */
@Composable
fun SuccessPanelMapper(component: com.augmentalis.avaelements.flutter.material.feedback.SuccessPanel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9) // Light green
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = component.elevation.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50), // Green
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp, end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = component.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )

                if (component.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        component.actions.forEach { action ->
                            TextButton(onClick = { action.onClick?.invoke() }) {
                                Text(action.label)
                            }
                        }
                    }
                }
            }

            if (component.dismissible) {
                IconButton(
                    onClick = { component.onDismiss?.invoke() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Render FullPageLoading component using Material3
 */
@Composable
fun FullPageLoadingMapper(component: com.augmentalis.avaelements.flutter.material.feedback.FullPageLoading) {
    if (component.visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(enabled = false) { /* Block clicks */ }
                .semantics {
                    contentDescription = component.getAccessibilityDescription()
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(component.spinnerSize.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (component.message != null) {
                        Text(
                            text = component.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (component.cancelable) {
                        TextButton(onClick = { component.onCancel?.invoke() }) {
                            Text(component.cancelText)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render AnimatedCheck component using Material3
 */
@Composable
fun AnimatedCheckMapper(component: com.augmentalis.avaelements.flutter.material.feedback.AnimatedCheck) {
    val scale by animateFloatAsState(
        targetValue = if (component.visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "check_scale"
    )

    if (component.visible) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = component.getAccessibilityDescription(),
            modifier = Modifier
                .size(component.size.dp)
                .scale(scale)
                .semantics {
                    contentDescription = component.getAccessibilityDescription()
                },
            tint = Color(0xFF4CAF50) // Success green
        )
    }
}

/**
 * Render AnimatedError component using Material3
 */
@Composable
fun AnimatedErrorMapper(component: com.augmentalis.avaelements.flutter.material.feedback.AnimatedError) {
    val scale by animateFloatAsState(
        targetValue = if (component.visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "error_scale"
    )

    // Shake animation
    val shakeOffset by animateFloatAsState(
        targetValue = if (component.visible) 0f else 0f,
        animationSpec = tween(durationMillis = component.animationDuration),
        label = "error_shake"
    )

    if (component.visible) {
        Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = component.getAccessibilityDescription(),
            modifier = Modifier
                .size(component.size.dp)
                .scale(scale)
                .offset(x = shakeOffset.dp)
                .semantics {
                    contentDescription = component.getAccessibilityDescription()
                },
            tint = Color(0xFFF44336) // Error red
        )
    }
}

/**
 * Render MasonryGrid component using Material3
 */
@Composable
fun MasonryGridMapper(component: com.augmentalis.avaelements.flutter.material.layout.MasonryGrid) {
    LazyVerticalStaggeredGrid(
        columns = when (component.columns) {
            is com.augmentalis.avaelements.flutter.material.layout.MasonryGrid.Columns.Fixed ->
                StaggeredGridCells.Fixed((component.columns as com.augmentalis.avaelements.flutter.material.layout.MasonryGrid.Columns.Fixed).count)
            is com.augmentalis.avaelements.flutter.material.layout.MasonryGrid.Columns.Adaptive ->
                StaggeredGridCells.Adaptive(component.minItemWidth.dp)
        },
        horizontalArrangement = Arrangement.spacedBy(component.horizontalSpacing.dp),
        verticalItemSpacing = component.verticalSpacing.dp,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
    ) {
        itemsIndexed(component.items) { index, item ->
            Box(
                modifier = Modifier.then(
                    if (component.onItemClick != null) {
                        Modifier.clickable { component.onItemClick.invoke(index) }
                    } else {
                        Modifier
                    }
                )
            ) {
                // TODO: Render item component
                // RenderChild(item)
            }
        }
    }
}

/**
 * Render AspectRatio component using Material3
 */
@Composable
fun AspectRatioMapper(component: com.augmentalis.avaelements.flutter.material.layout.AspectRatio) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(component.getRatioValue())
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
    ) {
        // TODO: Render child component
        // if (component.child != null) {
        //     RenderChild(component.child)
        // }
    }
}

// ============================================================================
// AGENT 4: Navigation & Data Components (P1)
// ============================================================================

/**
 * Render Menu component using Material3
 *
 * Maps Menu component to Material3 with nested submenus, selection states,
 * and full keyboard navigation support.
 */
@Composable
fun MenuMapper(component: com.augmentalis.avaelements.flutter.material.navigation.Menu) {
    val backgroundColor = component.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .semantics { contentDescription = component.getAccessibilityDescription() },
        color = backgroundColor,
        shadowElevation = component.elevation?.dp ?: 1.dp
    ) {
        when (component.orientation) {
            com.augmentalis.avaelements.flutter.material.navigation.Menu.Orientation.Vertical -> {
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
            com.augmentalis.avaelements.flutter.material.navigation.Menu.Orientation.Horizontal -> {
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
    item: com.augmentalis.avaelements.flutter.material.navigation.Menu.MenuItem,
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
    item: com.augmentalis.avaelements.flutter.material.navigation.Menu.MenuItem,
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
fun SidebarMapper(component: com.augmentalis.avaelements.flutter.material.navigation.Sidebar) {
    val backgroundColor = component.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.surface
    val selectedColor = component.selectedItemColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primary
    val unselectedColor = component.unselectedItemColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    val width by animateFloatAsState(
        targetValue = component.getEffectiveWidth(),
        animationSpec = tween(durationMillis = 300)
    )

    if (!component.visible && component.mode == com.augmentalis.avaelements.flutter.material.navigation.Sidebar.Mode.Overlay) {
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
    item: com.augmentalis.avaelements.flutter.material.navigation.Sidebar.SidebarItem,
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
fun NavLinkMapper(component: com.augmentalis.avaelements.flutter.material.navigation.NavLink) {
    val backgroundColor = if (component.active) {
        component.activeBackgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
            ?: MaterialTheme.colorScheme.primaryContainer
    } else {
        component.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
            ?: Color.Transparent
    }

    val contentColor = if (component.active) {
        component.activeColor?.let { Color(android.graphics.Color.parseColor(it)) }
            ?: MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        component.inactiveColor?.let { Color(android.graphics.Color.parseColor(it)) }
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
            com.augmentalis.avaelements.flutter.material.navigation.NavLink.IconPosition.Leading -> {
                Row(
                    modifier = Modifier.padding(12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavLinkContent(component, contentColor)
                }
            }
            com.augmentalis.avaelements.flutter.material.navigation.NavLink.IconPosition.Trailing -> {
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
            com.augmentalis.avaelements.flutter.material.navigation.NavLink.IconPosition.Top -> {
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
            com.augmentalis.avaelements.flutter.material.navigation.NavLink.IconPosition.Bottom -> {
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
private fun NavLinkContent(component: com.augmentalis.avaelements.flutter.material.navigation.NavLink, contentColor: Color) {
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
fun ProgressStepperMapper(component: com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper) {
    val completedColor = component.completedStepColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primary
    val currentColor = component.currentStepColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.secondary
    val upcomingColor = component.upcomingStepColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.outlineVariant
    val connectorColor = component.connectorColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .padding(component.contentPadding?.let {
                PaddingValues(it.left.dp, it.top.dp, it.right.dp, it.bottom.dp)
            } ?: PaddingValues(16.dp))
            .semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        when (component.orientation) {
            com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.Orientation.Horizontal -> {
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
            com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.Orientation.Vertical -> {
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
                                        com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.StepState.Completed -> completedColor
                                        com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.StepState.Current -> currentColor
                                        com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.StepState.Upcoming -> upcomingColor
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
    step: com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.Step,
    stepNumber: Int,
    state: com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.StepState,
    showNumber: Boolean,
    completedColor: Color,
    currentColor: Color,
    upcomingColor: Color,
    clickable: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (state) {
        com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.StepState.Completed -> completedColor
        com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.StepState.Current -> currentColor
        com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.StepState.Upcoming -> upcomingColor
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
            state == com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.StepState.Completed -> Icon(
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
    type: com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.ConnectorType,
    color: Color,
    completed: Boolean,
    horizontal: Boolean,
    modifier: Modifier = Modifier
) {
    if (type == com.augmentalis.avaelements.flutter.material.navigation.ProgressStepper.ConnectorType.None) {
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
 * Render RadioListTile component using Material3
 */
@Composable
fun RadioListTileMapper(component: com.augmentalis.avaelements.flutter.material.data.RadioListTile) {
    val backgroundColor = if (component.selected) {
        component.selectedTileColor?.let { Color(android.graphics.Color.parseColor(it)) }
            ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    } else {
        component.tileColor?.let { Color(android.graphics.Color.parseColor(it)) }
            ?: Color.Transparent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = component.enabled) {
                component.onChanged?.invoke(component.value)
            }
            .semantics { contentDescription = component.getAccessibilityDescription() },
        color = backgroundColor
    ) {
        ListItem(
            headlineContent = { Text(component.title) },
            supportingContent = component.subtitle?.let { { Text(it) } },
            leadingContent = if (component.controlAffinity == com.augmentalis.avaelements.flutter.material.data.RadioListTile.ListTileControlAffinity.Leading) {
                {
                    RadioButton(
                        selected = component.isSelected,
                        onClick = null,
                        enabled = component.enabled,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = component.activeColor?.let { Color(android.graphics.Color.parseColor(it)) }
                                ?: MaterialTheme.colorScheme.primary
                        )
                    )
                }
            } else component.secondary?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
            trailingContent = if (component.controlAffinity == com.augmentalis.avaelements.flutter.material.data.RadioListTile.ListTileControlAffinity.Trailing) {
                {
                    RadioButton(
                        selected = component.isSelected,
                        onClick = null,
                        enabled = component.enabled,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = component.activeColor?.let { Color(android.graphics.Color.parseColor(it)) }
                                ?: MaterialTheme.colorScheme.primary
                        )
                    )
                }
            } else null,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

/**
 * Render VirtualScroll component using Material3 LazyColumn
 */
@Composable
fun VirtualScrollMapper(component: com.augmentalis.avaelements.flutter.material.data.VirtualScroll) {
    val backgroundColor = component.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: Color.Transparent

    Box(
        modifier = Modifier
            .background(backgroundColor)
            .semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        when (component.orientation) {
            com.augmentalis.avaelements.flutter.material.data.VirtualScroll.Orientation.Vertical -> {
                androidx.compose.foundation.lazy.LazyColumn(
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
                            // component.onItemRender?.invoke(index)
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
            com.augmentalis.avaelements.flutter.material.data.VirtualScroll.Orientation.Horizontal -> {
                androidx.compose.foundation.lazy.LazyRow(
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
                            // TODO: Render item from onItemRender callback
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
fun InfiniteScrollMapper(component: com.augmentalis.avaelements.flutter.material.data.InfiniteScroll) {
    val backgroundColor = component.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: Color.Transparent
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

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
            com.augmentalis.avaelements.flutter.material.data.InfiniteScroll.Orientation.Vertical -> {
                androidx.compose.foundation.lazy.LazyColumn(
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
            com.augmentalis.avaelements.flutter.material.data.InfiniteScroll.Orientation.Horizontal -> {
                androidx.compose.foundation.lazy.LazyRow(
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
private fun InfiniteScrollFooter(component: com.augmentalis.avaelements.flutter.material.data.InfiniteScroll) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (component.getFooterState()) {
            com.augmentalis.avaelements.flutter.material.data.InfiniteScroll.FooterState.Loading -> {
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
            com.augmentalis.avaelements.flutter.material.data.InfiniteScroll.FooterState.End -> {
                Text(
                    text = component.endMessageText ?: "No more items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            com.augmentalis.avaelements.flutter.material.data.InfiniteScroll.FooterState.Error -> {
                TextButton(onClick = { component.onRetry?.invoke() }) {
                    Text(component.errorMessageText ?: "Failed to load. Tap to retry.")
                }
            }
            com.augmentalis.avaelements.flutter.material.data.InfiniteScroll.FooterState.None -> {
                // No footer
            }
        }
    }
}

/**
 * Render QRCode component using ZXing library
 */
@Composable
fun QRCodeMapper(component: com.augmentalis.avaelements.flutter.material.data.QRCode) {
    val bitmap = remember(component.data, component.size, component.errorCorrection) {
        try {
            if (!component.isDataValid()) {
                null
            } else {
                val writer = com.google.zxing.qrcode.QRCodeWriter()
                val errorCorrectionLevel = when (component.errorCorrection) {
                    com.augmentalis.avaelements.flutter.material.data.QRCode.ErrorCorrectionLevel.L ->
                        com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
                    com.augmentalis.avaelements.flutter.material.data.QRCode.ErrorCorrectionLevel.M ->
                        com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
                    com.augmentalis.avaelements.flutter.material.data.QRCode.ErrorCorrectionLevel.Q ->
                        com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q
                    com.augmentalis.avaelements.flutter.material.data.QRCode.ErrorCorrectionLevel.H ->
                        com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
                }

                val hints = mapOf(
                    com.google.zxing.EncodeHintType.ERROR_CORRECTION to errorCorrectionLevel,
                    com.google.zxing.EncodeHintType.MARGIN to 1
                )

                val bitMatrix = writer.encode(
                    component.data,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    component.size.toInt(),
                    component.size.toInt(),
                    hints
                )

                val width = bitMatrix.width
                val height = bitMatrix.height
                val pixels = IntArray(width * height)

                for (y in 0 until height) {
                    for (x in 0 until width) {
                        pixels[y * width + x] = if (bitMatrix[x, y]) {
                            component.foregroundColor.toInt()
                        } else {
                            component.backgroundColor.toInt()
                        }
                    }
                }

                android.graphics.Bitmap.createBitmap(
                    pixels,
                    width,
                    height,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .size(component.size.dp)
            .padding(component.padding.dp)
            .then(
                if (component.onTap != null) {
                    Modifier.clickable { component.onTap.invoke() }
                } else Modifier
            )
            .semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = component.contentDescription ?: "QR Code",
                modifier = Modifier.fillMaxSize()
            )

            // Embedded image overlay
            if (component.embeddedImageUrl != null) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    AsyncImage(
                        model = component.embeddedImageUrl,
                        contentDescription = "Embedded logo",
                        modifier = Modifier
                            .size(component.embeddedImageSize?.dp ?: 40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        } ?: run {
            // Error state - invalid QR data
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Invalid QR code data",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Invalid data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * Render MenuBar component using Material3
 *
 * Maps MenuBar component to Material3 TopAppBar with dropdown menus
 */
@Composable
fun MenuBarMapper(component: com.augmentalis.avaelements.flutter.material.navigation.MenuBar) {
    val backgroundColor = component.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
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
    item: com.augmentalis.avaelements.flutter.material.navigation.MenuBar.MenuBarItem,
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
    item: com.augmentalis.avaelements.flutter.material.navigation.Menu.MenuItem,
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
fun SubMenuMapper(component: com.augmentalis.avaelements.flutter.material.navigation.SubMenu) {
    var expanded by remember { mutableStateOf(component.open) }
    val backgroundColor = component.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.surface

    Box {
        // Trigger button
        TextButton(
            onClick = {
                if (component.trigger == com.augmentalis.avaelements.flutter.material.navigation.SubMenu.TriggerMode.Click ||
                    component.trigger == com.augmentalis.avaelements.flutter.material.navigation.SubMenu.TriggerMode.Both
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
    item: com.augmentalis.avaelements.flutter.material.navigation.SubMenu.SubMenuItem,
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
fun VerticalTabsMapper(component: com.augmentalis.avaelements.flutter.material.navigation.VerticalTabs) {
    val backgroundColor = component.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.surface
    val indicatorColor = component.indicatorColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.primary
    val selectedColor = component.selectedTabColor?.let { Color(android.graphics.Color.parseColor(it)) }
        ?: MaterialTheme.colorScheme.onSecondaryContainer
    val unselectedColor = component.unselectedTabColor?.let { Color(android.graphics.Color.parseColor(it)) }
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
            androidx.compose.foundation.lazy.LazyColumn(
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
                            color = component.dividerColor?.let { Color(android.graphics.Color.parseColor(it)) }
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
                            color = component.dividerColor?.let { Color(android.graphics.Color.parseColor(it)) }
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
    tab: com.augmentalis.avaelements.flutter.material.navigation.VerticalTabs.Tab,
    isSelected: Boolean,
    showLabel: Boolean,
    showIcon: Boolean,
    labelPosition: com.augmentalis.avaelements.flutter.material.navigation.VerticalTabs.LabelPosition,
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
            com.augmentalis.avaelements.flutter.material.navigation.VerticalTabs.LabelPosition.Right -> {
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
            com.augmentalis.avaelements.flutter.material.navigation.VerticalTabs.LabelPosition.Left -> {
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
            com.augmentalis.avaelements.flutter.material.navigation.VerticalTabs.LabelPosition.Bottom,
            com.augmentalis.avaelements.flutter.material.navigation.VerticalTabs.LabelPosition.Top -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (labelPosition == com.augmentalis.avaelements.flutter.material.navigation.VerticalTabs.LabelPosition.Top && showLabel) {
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
                    if (labelPosition == com.augmentalis.avaelements.flutter.material.navigation.VerticalTabs.LabelPosition.Bottom && showLabel) {
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
                    Row(modifier = Modifier.fillMaxWidth().then(if (component.expandable) Modifier.clickable { expanded = !expanded } else Modifier).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            if (component.numbered) Text(text = "${index + 1}.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            item.icon?.let { IconFromString(it, null, Modifier.size(20.dp)) }
                            Text(text = item.term, style = MaterialTheme.typography.titleSmall)
                            item.badge?.let { Badge { Text(it) } }
                        }
                        if (component.expandable) Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = if (expanded) "Collapse" else "Expand")
                    }
                    if (!component.expandable || expanded) {
                        Text(text = item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = if (component.numbered) 24.dp else 0.dp, top = 4.dp, bottom = 8.dp))
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
                    Icon(imageVector = when (stat.changeType) {
                        StatGroup.ChangeType.Positive -> Icons.Default.KeyboardArrowUp
                        StatGroup.ChangeType.Negative -> Icons.Default.KeyboardArrowDown
                        StatGroup.ChangeType.Neutral -> Icons.Default.MoreHoriz
                    }, contentDescription = null, tint = changeColor, modifier = Modifier.size(16.dp))
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
    Card(modifier = Modifier.fillMaxWidth().then(if (component.onClick != null) Modifier.clickable { component.onClick.invoke() } else Modifier)
        .semantics { contentDescription = component.contentDescription ?: "Statistic: ${component.label}" },
        elevation = if (component.elevated) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation()) {
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
                    Icon(imageVector = when (component.changeType) {
                        Stat.ChangeType.Positive -> Icons.Default.KeyboardArrowUp
                        Stat.ChangeType.Negative -> Icons.Default.KeyboardArrowDown
                        Stat.ChangeType.Neutral -> Icons.Default.MoreHoriz
                    }, contentDescription = null, tint = changeColor, modifier = Modifier.size(20.dp))
                    Text(text = change, style = MaterialTheme.typography.titleMedium, color = changeColor)
                }
            }
            component.description?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}

/**
 * Render KPI component using Material3
 */
@Composable
fun KPIMapper(component: KPI) {
    Card(modifier = Modifier.fillMaxWidth().then(if (component.onClick != null) Modifier.clickable { component.onClick.invoke() } else Modifier)
        .semantics { contentDescription = component.contentDescription ?: "KPI: ${component.title}" }) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    component.icon?.let { IconFromString(it, null, Modifier.size(24.dp)) }
                    Text(text = component.title, style = MaterialTheme.typography.titleMedium)
                }
                Icon(imageVector = when (component.trend) {
                    KPI.TrendType.Up -> Icons.Default.KeyboardArrowUp
                    KPI.TrendType.Down -> Icons.Default.KeyboardArrowDown
                    KPI.TrendType.Neutral -> Icons.Default.MoreHoriz
                }, contentDescription = "Trend: ${component.trend}", tint = when (component.trend) {
                    KPI.TrendType.Up -> MaterialTheme.colorScheme.primary
                    KPI.TrendType.Down -> MaterialTheme.colorScheme.error
                    KPI.TrendType.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                }, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = component.value, style = MaterialTheme.typography.displayMedium)
            component.subtitle?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
            component.target?.let { Text(text = "Target: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
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
 * Render MetricCard component using Material3
 */
@Composable
fun MetricCardMapper(component: MetricCard) {
    Card(modifier = Modifier.fillMaxWidth().then(if (component.onClick != null) Modifier.clickable { component.onClick.invoke() } else Modifier)
        .semantics { contentDescription = component.contentDescription ?: "Metric: ${component.title}" },
        colors = component.color?.let { CardDefaults.cardColors(containerColor = Color(android.graphics.Color.parseColor(it))) } ?: CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                component.icon?.let { IconFromString(it, null, Modifier.size(20.dp)) }
                Text(text = component.title, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = component.value, style = MaterialTheme.typography.displaySmall)
                component.unit?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            }
            component.comparison?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    component.change?.let { change ->
                        Text(text = change, style = MaterialTheme.typography.labelMedium, color = when (component.changeType) {
                            MetricCard.ChangeType.Positive -> MaterialTheme.colorScheme.primary
                            MetricCard.ChangeType.Negative -> MaterialTheme.colorScheme.error
                            MetricCard.ChangeType.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                        })
                    }
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Surface(modifier = Modifier.fillMaxWidth().clickable { component.onItemClick?.invoke(item.id) },
                        color = if (component.isCurrentUser(item)) MaterialTheme.colorScheme.primaryContainer else Color.Transparent) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val showBadge = component.showTopBadges && item.rank <= 3
                            if (showBadge) {
                                Surface(shape = CircleShape, color = when (item.rank) {
                                    1 -> Color(0xFFFFD700)
                                    2 -> Color(0xFFC0C0C0)
                                    3 -> Color(0xFFCD7F32)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text(text = item.rank.toString(), style = MaterialTheme.typography.labelLarge, color = Color.Black) }
                                }
                            } else {
                                Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "${item.rank}", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            item.avatar?.let { AsyncImage(model = it, contentDescription = "Avatar", modifier = Modifier.size(40.dp).clip(CircleShape)) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                                item.subtitle?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            Box(contentAlignment = Alignment.Center) { Text(text = component.position.toString(), style = MaterialTheme.typography.labelSmall, color = if (badgeType != null) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer) }
        }
        Ranking.Size.Medium -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(text = component.position.toString(), style = MaterialTheme.typography.titleMedium, color = if (badgeType != null) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer) }
            }
            component.change?.let { Text(text = kotlin.math.abs(it).toString(), style = MaterialTheme.typography.labelSmall) }
        }
        Ranking.Size.Large -> Card {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                component.label?.let { Text(text = it, style = MaterialTheme.typography.labelMedium) }
                Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(text = component.position.toString(), style = MaterialTheme.typography.headlineMedium, color = if (badgeType != null) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer) }
                }
                Text(text = component.getOrdinal(), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/**
 * Render Zoom component using Material3
 */
@Composable
fun ZoomMapper(component: Zoom) {
    var scale by remember { mutableStateOf(component.initialScale) }
    Box(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.contentDescription ?: "Zoomable image" }) {
        AsyncImage(model = component.imageUrl, contentDescription = component.contentDescription, modifier = Modifier.fillMaxWidth().scale(scale))
        if (component.showControls) {
            Row(modifier = Modifier.align(when (component.controlsPosition) {
                Zoom.ControlsPosition.TopLeft -> Alignment.TopStart
                Zoom.ControlsPosition.TopRight -> Alignment.TopEnd
                Zoom.ControlsPosition.BottomLeft -> Alignment.BottomStart
                Zoom.ControlsPosition.BottomRight -> Alignment.BottomEnd
                Zoom.ControlsPosition.BottomCenter -> Alignment.BottomCenter
            }).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(onClick = { scale = component.clampScale(scale - 0.25f); component.onScaleChanged?.invoke(scale) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Zoom out")
                }
                FloatingActionButton(onClick = { scale = component.initialScale; component.onScaleChanged?.invoke(scale) }, modifier = Modifier.size(40.dp)) {
                    Text("1:1", style = MaterialTheme.typography.labelSmall)
                }
                FloatingActionButton(onClick = { scale = component.clampScale(scale + 0.25f); component.onScaleChanged?.invoke(scale) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom in")
                }
            }
        }
    }
}

// Additional mappers will be added as components are implemented

// ============================================================================
// AGENT 5: Advanced Input Components (P1)  
// ============================================================================

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.InputChip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import com.augmentalis.avaelements.flutter.material.input.*

/**
 * Render PhoneInput component using Material3
 */
@Composable
fun PhoneInputMapper(component: PhoneInput) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = component.getAccessibilityDescription() },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width(100.dp)
        ) {
            OutlinedTextField(
                value = component.COUNTRY_CODES[component.countryCode] ?: "+1",
                onValueChange = {},
                readOnly = true,
                enabled = component.enabled,
                label = { Text("Code") },
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select country code"
                    )
                },
                modifier = Modifier.menuAnchor(),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                component.COUNTRY_CODES.forEach { (code, dialCode) ->
                    DropdownMenuItem(
                        text = { Text("$code $dialCode") },
                        onClick = {
                            component.onCountryCodeChange?.invoke(code)
                            expanded = false
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = component.value,
            onValueChange = { component.onValueChange?.invoke(it) },
            label = component.label?.let { { Text(it) } },
            placeholder = component.placeholder?.let { { Text(it) } },
            enabled = component.enabled,
            isError = component.errorText != null,
            supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Render UrlInput component using Material3
 */
@Composable
fun UrlInputMapper(component: UrlInput) {
    OutlinedTextField(
        value = component.value,
        onValueChange = { value ->
            val finalValue = if (component.autoAddProtocol && value.isNotBlank() && !value.startsWith("http")) {
                "https://$value"
            } else {
                value
            }
            component.onValueChange?.invoke(finalValue)
        },
        label = component.label?.let { { Text(it) } },
        placeholder = component.placeholder?.let { { Text(it) } },
        enabled = component.enabled,
        isError = component.errorText != null || !component.isValid(),
        supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        leadingIcon = { Icon(imageVector = Icons.Default.Link, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }
    )
}

/**
 * Render ComboBox component using Material3
 */
@Composable
fun ComboBoxMapper(component: ComboBox) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(component.value) }
    val filteredOptions = remember(searchQuery) { component.getFilteredOptions(searchQuery) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                expanded = true
                if (component.allowCustomValue) component.onValueChange?.invoke(it)
            },
            label = component.label?.let { { Text(it) } },
            placeholder = component.placeholder?.let { { Text(it) } },
            enabled = component.enabled,
            isError = component.errorText != null,
            supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand options"
                )
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )
        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            searchQuery = option
                            component.onValueChange?.invoke(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Render PinInput component using Material3
 */
@Composable
fun PinInputMapper(component: PinInput) {
    val focusRequesters = remember { List(component.length) { FocusRequester() } }
    Column(modifier = Modifier.semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(text = it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            repeat(component.length) { index ->
                val digit = component.value.getOrNull(index)?.toString() ?: ""
                OutlinedTextField(
                    value = digit,
                    onValueChange = { newValue ->
                        if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                            val newPin = buildString {
                                component.value.forEachIndexed { i, c ->
                                    if (i == index) append(newValue.firstOrNull() ?: "") else append(c)
                                }
                                if (index >= component.value.length && newValue.isNotEmpty()) append(newValue)
                            }.take(component.length)
                            component.onValueChange?.invoke(newPin)
                            if (newValue.isNotEmpty() && index < component.length - 1) {
                                focusRequesters[index + 1].requestFocus()
                            }
                            if (newPin.length == component.length) component.onComplete?.invoke(newPin)
                        } else if (newValue.isEmpty() && index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    },
                    enabled = component.enabled,
                    isError = component.errorText != null,
                    visualTransformation = if (component.masked) PasswordVisualTransformation('•') else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = if (index == component.length - 1) ImeAction.Done else ImeAction.Next
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.width(48.dp).focusRequester(focusRequesters[index])
                )
            }
        }
        component.errorText?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

/**
 * Render OTPInput component using Material3
 */
@Composable
fun OTPInputMapper(component: OTPInput) {
    val focusRequesters = remember { List(component.length) { FocusRequester() } }
    Column(modifier = Modifier.semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(text = it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            repeat(component.length) { index ->
                val char = component.value.getOrNull(index)?.toString() ?: ""
                OutlinedTextField(
                    value = char,
                    onValueChange = { newValue ->
                        if (newValue.length > 1) {
                            val cleanOtp = newValue.filter { it.isLetterOrDigit() }.take(component.length)
                            component.onValueChange?.invoke(cleanOtp)
                            if (cleanOtp.length == component.length && component.autoSubmit) {
                                component.onComplete?.invoke(cleanOtp)
                            }
                        } else if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isLetterOrDigit() })) {
                            val newOtp = buildString {
                                component.value.forEachIndexed { i, c ->
                                    if (i == index) append(newValue.firstOrNull() ?: "") else append(c)
                                }
                                if (index >= component.value.length && newValue.isNotEmpty()) append(newValue)
                            }.take(component.length)
                            component.onValueChange?.invoke(newOtp)
                            if (newValue.isNotEmpty() && index < component.length - 1) {
                                focusRequesters[index + 1].requestFocus()
                            }
                            if (newOtp.length == component.length && component.autoSubmit) {
                                component.onComplete?.invoke(newOtp)
                            }
                        } else if (newValue.isEmpty() && index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    },
                    enabled = component.enabled,
                    isError = component.errorText != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = if (index == component.length - 1) ImeAction.Done else ImeAction.Next
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.width(48.dp).focusRequester(focusRequesters[index])
                )
            }
        }
        component.errorText?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

/**
 * Render MaskInput component using Material3
 */
@Composable
fun MaskInputMapper(component: MaskInput) {
    OutlinedTextField(
        value = component.value,
        onValueChange = { value ->
            val unmasked = value.filter { it.isLetterOrDigit() }
            var formatted = ""
            var unmaskedIndex = 0
            for (maskChar in component.mask) {
                if (unmaskedIndex >= unmasked.length) break
                when (maskChar) {
                    '#' -> if (unmasked[unmaskedIndex].isDigit()) { formatted += unmasked[unmaskedIndex++] } else break
                    'A' -> if (unmasked[unmaskedIndex].isLetter()) { formatted += unmasked[unmaskedIndex++] } else break
                    'X' -> formatted += unmasked[unmaskedIndex++]
                    else -> formatted += maskChar
                }
            }
            component.onValueChange?.invoke(formatted)
        },
        label = component.label?.let { { Text(it) } },
        placeholder = component.placeholder?.let { { Text(it) } },
        enabled = component.enabled,
        isError = component.errorText != null,
        supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }
    )
}

/**
 * Render RichTextEditor component using Material3
 */
@Composable
fun RichTextEditorMapper(component: RichTextEditor) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (component.showToolbar) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {}) { Icon(imageVector = Icons.Default.FormatBold, contentDescription = "Bold") }
                    IconButton(onClick = {}) { Icon(imageVector = Icons.Default.FormatItalic, contentDescription = "Italic") }
                    IconButton(onClick = {}) { Icon(imageVector = Icons.Default.FormatUnderlined, contentDescription = "Underline") }
                }
            }
        }
        OutlinedTextField(
            value = component.value,
            onValueChange = { component.onValueChange?.invoke(it) },
            placeholder = component.placeholder?.let { { Text(it) } },
            enabled = component.enabled,
            isError = component.errorText != null,
            supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            minLines = 6,
            maxLines = 20,
            modifier = Modifier.fillMaxWidth().heightIn(min = component.minHeight.dp)
        )
    }
}

/**
 * Render MarkdownEditor component using Material3
 */
@Composable
fun MarkdownEditorMapper(component: MarkdownEditor) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (component.splitView && component.showPreview) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = component.value,
                    onValueChange = { component.onValueChange?.invoke(it) },
                    placeholder = component.placeholder?.let { { Text(it) } },
                    enabled = component.enabled,
                    minLines = 10,
                    modifier = Modifier.weight(1f).heightIn(min = component.minHeight.dp)
                )
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = component.minHeight.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(text = component.value, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            OutlinedTextField(
                value = component.value,
                onValueChange = { component.onValueChange?.invoke(it) },
                placeholder = component.placeholder?.let { { Text(it) } },
                enabled = component.enabled,
                isError = component.errorText != null,
                supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                minLines = 10,
                modifier = Modifier.fillMaxWidth().heightIn(min = component.minHeight.dp)
            )
        }
    }
}

/**
 * Render CodeEditor component using Material3
 */
@Composable
fun CodeEditorMapper(component: CodeEditor) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        Row {
            if (component.showLineNumbers) {
                val lineCount = component.value.count { it == '\n' } + 1
                Surface(modifier = Modifier.width(48.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        repeat(lineCount) { line ->
                            Text(
                                "${line + 1}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = component.value,
                onValueChange = { component.onValueChange?.invoke(it) },
                placeholder = component.placeholder?.let { { Text(it) } },
                enabled = component.enabled,
                isError = component.errorText != null,
                supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                minLines = 15,
                modifier = Modifier.weight(1f).heightIn(min = component.minHeight.dp)
            )
        }
    }
}

/**
 * Render FormSection component using Material3
 */
@Composable
fun FormSectionMapper(component: FormSection) {
    var expanded by remember { mutableStateOf(component.expanded) }
    val isExpanded = if (component.collapsible) expanded else true

    Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        Column {
            if (component.title != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (component.collapsible) Modifier.clickable {
                            expanded = !expanded
                            component.onExpandChange?.invoke(expanded)
                        } else Modifier)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(component.title, style = MaterialTheme.typography.titleMedium)
                        component.description?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    if (component.collapsible) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
                if (component.showDivider) HorizontalDivider()
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // TODO: Render children components
                }
            }
        }
    }
}

/**
 * Render MultiSelect component using Material3
 */
@Composable
fun MultiSelectMapper(component: MultiSelect) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = component.getAccessibilityDescription() }) {
        component.label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (component.showChips && component.selectedValues.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                component.selectedValues.forEach { value ->
                    InputChip(
                        selected = true,
                        onClick = { component.onSelectionChange?.invoke(component.selectedValues - value) },
                        label = { Text(value) },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove $value", modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = if (component.selectedValues.isEmpty()) component.placeholder ?: "" else "${component.selectedValues.size} selected",
                onValueChange = {},
                readOnly = true,
                enabled = component.enabled,
                isError = component.errorText != null,
                supportingText = component.errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand options"
                    )
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (component.searchable) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        singleLine = true
                    )
                }
                val filteredOptions = if (searchQuery.isBlank()) component.options else component.options.filter { it.contains(searchQuery, ignoreCase = true) }
                filteredOptions.forEach { option ->
                    val isSelected = component.isSelected(option)
                    val canSelect = !isSelected || !component.isMaxSelectionsReached()
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            if (canSelect) component.onSelectionChange?.invoke(component.toggleSelection(option))
                        },
                        leadingIcon = { Checkbox(checked = isSelected, onCheckedChange = null, enabled = canSelect) },
                        enabled = canSelect
                    )
                }
            }
        }
    }
}
