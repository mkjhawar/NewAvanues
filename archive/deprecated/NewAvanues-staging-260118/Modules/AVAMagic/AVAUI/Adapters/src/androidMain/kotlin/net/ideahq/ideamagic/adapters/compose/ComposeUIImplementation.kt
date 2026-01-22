package net.ideahq.avamagic.adapters.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import net.ideahq.avamagic.components.foundation.*
import net.ideahq.avamagic.components.core.*

/**
 * ComposeUIImplementation - Actual Jetpack Compose implementations
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

/**
 * Foundation Components - Full Implementations
 */
@Composable
fun MagicButtonCompose(
    text: String,
    onClick: () -> Unit,
    variant: ButtonVariant,
    size: ButtonSize,
    enabled: Boolean,
    fullWidth: Boolean,
    icon: String?,
    iconPosition: IconPosition,
    modifier: Modifier = Modifier
) {
    val buttonModifier = if (fullWidth) {
        modifier.fillMaxWidth()
    } else {
        modifier
    }

    val contentPadding = when (size) {
        ButtonSize.SMALL -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ButtonSize.MEDIUM -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ButtonSize.LARGE -> PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    }

    when (variant) {
        ButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                contentPadding = contentPadding
            ) {
                ButtonContent(text, icon, iconPosition)
            }
        }
        ButtonVariant.SECONDARY, ButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                contentPadding = contentPadding
            ) {
                ButtonContent(text, icon, iconPosition)
            }
        }
        ButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                contentPadding = contentPadding
            ) {
                ButtonContent(text, icon, iconPosition)
            }
        }
    }
}

@Composable
private fun ButtonContent(text: String, icon: String?, iconPosition: IconPosition) {
    if (icon != null) {
        if (iconPosition == IconPosition.START) {
            Icon(
                imageVector = getIconByName(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text)
        if (iconPosition == IconPosition.END) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = getIconByName(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    } else {
        Text(text)
    }
}

@Composable
fun MagicCardCompose(
    content: List<Any>,
    elevated: Boolean,
    variant: CardVariant,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val elevation = if (elevated) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(defaultElevation = 1.dp)

    val colors = when (variant) {
        CardVariant.OUTLINED -> CardDefaults.outlinedCardColors()
        CardVariant.FILLED -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else -> CardDefaults.cardColors()
    }

    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    if (variant == CardVariant.OUTLINED) {
        OutlinedCard(
            modifier = cardModifier,
            colors = colors
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content.forEach { item ->
                    when (item) {
                        is String -> Text(item)
                        // Handle other content types
                    }
                }
            }
        }
    } else {
        Card(
            modifier = cardModifier,
            elevation = elevation,
            colors = colors
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content.forEach { item ->
                    when (item) {
                        is String -> Text(item)
                        // Handle other content types
                    }
                }
            }
        }
    }
}

@Composable
fun MagicCheckboxCompose(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String?,
    enabled: Boolean,
    state: CheckboxState,
    modifier: Modifier = Modifier
) {
    val checkboxState = when (state) {
        CheckboxState.CHECKED -> ToggleableState.On
        CheckboxState.UNCHECKED -> ToggleableState.Off
        CheckboxState.INDETERMINATE -> ToggleableState.Indeterminate
    }

    if (label != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TriStateCheckbox(
                state = checkboxState,
                onClick = { onCheckedChange(!checked) },
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
            )
        }
    } else {
        TriStateCheckbox(
            state = checkboxState,
            onClick = { onCheckedChange(!checked) },
            enabled = enabled,
            modifier = modifier
        )
    }
}

@Composable
fun MagicChipCompose(
    label: String,
    variant: ChipVariant,
    icon: String?,
    deletable: Boolean,
    onClick: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val leadingIcon: @Composable (() -> Unit)? = if (icon != null) {
        { Icon(imageVector = getIconByName(icon), contentDescription = null, modifier = Modifier.size(18.dp)) }
    } else null

    val trailingIcon: @Composable (() -> Unit)? = if (deletable && onDelete != null) {
        {
            IconButton(onClick = onDelete, modifier = Modifier.size(18.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(18.dp))
            }
        }
    } else null

    when (variant) {
        ChipVariant.OUTLINED -> {
            OutlinedFilterChip(
                selected = false,
                onClick = { onClick?.invoke() },
                label = { Text(label) },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                modifier = modifier
            )
        }
        else -> {
            FilterChip(
                selected = false,
                onClick = { onClick?.invoke() },
                label = { Text(label) },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                modifier = modifier
            )
        }
    }
}

@Composable
fun MagicDividerCompose(
    orientation: DividerOrientation,
    thickness: Int,
    color: String?,
    inset: Boolean,
    modifier: Modifier = Modifier
) {
    val dividerColor = color?.let { parseColor(it) } ?: MaterialTheme.colorScheme.outlineVariant
    val insetModifier = if (inset) Modifier.padding(horizontal = 16.dp) else Modifier

    when (orientation) {
        DividerOrientation.HORIZONTAL -> {
            HorizontalDivider(
                modifier = modifier.then(insetModifier),
                thickness = thickness.dp,
                color = dividerColor
            )
        }
        DividerOrientation.VERTICAL -> {
            VerticalDivider(
                modifier = modifier.then(insetModifier),
                thickness = thickness.dp,
                color = dividerColor
            )
        }
    }
}

@Composable
fun MagicImageCompose(
    source: String,
    alt: String?,
    fit: ImageFit,
    width: Int?,
    height: Int?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val contentScale = when (fit) {
        ImageFit.CONTAIN -> ContentScale.Fit
        ImageFit.COVER -> ContentScale.Crop
        ImageFit.FILL -> ContentScale.FillBounds
        ImageFit.NONE -> ContentScale.None
    }

    val imageModifier = modifier
        .let { if (width != null) it.width(width.dp) else it }
        .let { if (height != null) it.height(height.dp) else it }
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    AsyncImage(
        model = source,
        contentDescription = alt,
        contentScale = contentScale,
        modifier = imageModifier
    )
}

@Composable
fun MagicListItemCompose(
    title: String,
    subtitle: String?,
    leadingIcon: String?,
    trailingIcon: String?,
    onClick: (() -> Unit)?,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = subtitle?.let { { Text(it) } },
            leadingContent = leadingIcon?.let {
                { Icon(imageVector = getIconByName(it), contentDescription = null) }
            },
            trailingContent = trailingIcon?.let {
                { Icon(imageVector = getIconByName(it), contentDescription = null) }
            },
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
        if (showDivider) {
            HorizontalDivider()
        }
    }
}

@Composable
fun MagicTextCompose(
    content: String,
    variant: TextVariant,
    color: String?,
    align: TextAlign,
    bold: Boolean,
    italic: Boolean,
    underline: Boolean,
    maxLines: Int?,
    modifier: Modifier = Modifier
) {
    val textStyle = when (variant) {
        TextVariant.H1 -> MaterialTheme.typography.headlineLarge
        TextVariant.H2 -> MaterialTheme.typography.headlineMedium
        TextVariant.H3 -> MaterialTheme.typography.headlineSmall
        TextVariant.BODY1 -> MaterialTheme.typography.bodyLarge
        TextVariant.BODY2 -> MaterialTheme.typography.bodyMedium
        TextVariant.CAPTION -> MaterialTheme.typography.bodySmall
    }

    val fontWeight = if (bold) FontWeight.Bold else null
    val fontStyle = if (italic) FontStyle.Italic else null
    val textDecoration = if (underline) TextDecoration.Underline else null
    val textColor = color?.let { parseColor(it) } ?: Color.Unspecified

    Text(
        text = content,
        style = textStyle,
        color = textColor,
        textAlign = align,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textDecoration = textDecoration,
        maxLines = maxLines ?: Int.MAX_VALUE,
        overflow = if (maxLines != null) TextOverflow.Ellipsis else TextOverflow.Clip,
        modifier = modifier
    )
}

@Composable
fun MagicTextFieldCompose(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    placeholder: String?,
    enabled: Boolean,
    readOnly: Boolean,
    type: TextFieldType,
    error: String?,
    helperText: String?,
    leadingIcon: String?,
    trailingIcon: String?,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    val isError = error != null
    val supportingText = error ?: helperText

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = getIconByName(it), contentDescription = null) }
        },
        trailingIcon = trailingIcon?.let {
            { Icon(imageVector = getIconByName(it), contentDescription = null) }
        },
        maxLines = maxLines,
        singleLine = maxLines == 1,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Core Components - Full Implementations
 */
@Composable
fun MagicColorPickerCompose(
    selectedColor: String,
    onColorChange: (String) -> Unit,
    mode: ColorPickerMode,
    showAlpha: Boolean,
    presetColors: List<String>?,
    label: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Display current color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(parseColor(selectedColor), RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Preset colors grid
        val colors = presetColors ?: getDefaultColorPalette()
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors.size) { index ->
                val colorHex = colors[index]
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(parseColor(colorHex), RoundedCornerShape(8.dp))
                        .border(
                            width = if (colorHex == selectedColor) 3.dp else 1.dp,
                            color = if (colorHex == selectedColor) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onColorChange(colorHex) }
                )
            }
        }
    }
}

@Composable
fun MagicIconPickerCompose(
    selectedIcon: String?,
    onIconChange: (String?) -> Unit,
    library: IconLibrary,
    searchQuery: String?,
    category: String?,
    iconSize: IconSize,
    columns: Int,
    label: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Search field
        var query by remember { mutableStateOf(searchQuery ?: "") }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search icons") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Icon grid
        val iconNames = getIconLibrary(library, query)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(iconNames.size) { index ->
                val iconName = iconNames[index]
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            width = if (iconName == selectedIcon) 2.dp else 1.dp,
                            color = if (iconName == selectedIcon) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onIconChange(iconName) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconByName(iconName),
                        contentDescription = iconName,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Helper Functions
 */
private fun getIconByName(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name.lowercase()) {
        "home" -> Icons.Default.Home
        "search" -> Icons.Default.Search
        "settings" -> Icons.Default.Settings
        "person" -> Icons.Default.Person
        "email" -> Icons.Default.Email
        "lock" -> Icons.Default.Lock
        "favorite" -> Icons.Default.Favorite
        "star" -> Icons.Default.Star
        "edit" -> Icons.Default.Edit
        "delete" -> Icons.Default.Delete
        "add" -> Icons.Default.Add
        "close" -> Icons.Default.Close
        "check" -> Icons.Default.Check
        "arrow_back" -> Icons.Default.ArrowBack
        "arrow_forward" -> Icons.Default.ArrowForward
        "menu" -> Icons.Default.Menu
        "more_vert" -> Icons.Default.MoreVert
        "share" -> Icons.Default.Share
        "notifications" -> Icons.Default.Notifications
        "account_circle" -> Icons.Default.AccountCircle
        else -> Icons.Default.Info
    }
}

private fun parseColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun getDefaultColorPalette(): List<String> {
    return listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
    )
}

private fun getIconLibrary(library: IconLibrary, query: String): List<String> {
    val allIcons = listOf(
        "home", "search", "settings", "person", "email",
        "lock", "favorite", "star", "edit", "delete",
        "add", "close", "check", "arrow_back", "arrow_forward",
        "menu", "more_vert", "share", "notifications", "account_circle"
    )
    return if (query.isBlank()) {
        allIcons
    } else {
        allIcons.filter { it.contains(query, ignoreCase = true) }
    }
}
