package com.augmentalis.magicelements.renderers.android

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.augmentalis.avaelements.core.Component
import kotlinx.serialization.json.*

/**
 * Factory for creating Jetpack Compose components from MEL UINode definitions.
 *
 * Maps MEL component types (Text, Button, Column, Row, etc.) to Material 3 Compose components.
 * Handles prop conversion from JsonElement to Compose parameters.
 *
 * ## Supported Components
 *
 * ### Layout (5)
 * - Column: Vertical linear layout
 * - Row: Horizontal linear layout
 * - Container: Box container
 * - Spacer: Empty space
 * - Divider: Visual separator
 *
 * ### Display (3)
 * - Text: Text content
 * - Icon: Icon display
 * - Image: Image display
 *
 * ### Input (6)
 * - Button: Clickable button
 * - TextField: Text input
 * - Checkbox: Boolean checkbox
 * - Switch: Toggle switch
 * - Slider: Value slider
 * - Dropdown: Selection dropdown
 *
 * ### Feedback (5)
 * - Card: Surface container
 * - Alert: Alert message
 * - ProgressBar: Progress indicator
 * - Badge: Status badge
 * - Chip: Compact element
 *
 * @since 2.0.0
 */
class MELComponentFactory {

    /**
     * Create a Compose component from UINode definition.
     *
     * @param type Component type name (e.g., "Text", "Button", "Column")
     * @param props Resolved props as JSON elements
     * @param eventHandlers Event handler callbacks
     * @param children Composable lambda for rendering children
     */
    @Composable
    fun CreateComponent(
        type: String,
        props: Map<String, JsonElement>,
        eventHandlers: Map<String, () -> Unit> = emptyMap(),
        children: @Composable () -> Unit = {}
    ) {
        when (type) {
            // Layout components
            "Column" -> CreateColumn(props, children)
            "Row" -> CreateRow(props, children)
            "Container" -> CreateContainer(props, children)
            "Spacer" -> CreateSpacer(props)
            "Divider" -> CreateDivider(props)

            // Display components
            "Text" -> CreateText(props)
            "Icon" -> CreateIcon(props)
            "Image" -> CreateImage(props)

            // Input components
            "Button" -> CreateButton(props, eventHandlers)
            "TextField" -> CreateTextField(props, eventHandlers)
            "Checkbox" -> CreateCheckbox(props, eventHandlers)
            "Switch" -> CreateSwitch(props, eventHandlers)
            "Slider" -> CreateSlider(props, eventHandlers)
            "Dropdown" -> CreateDropdown(props, eventHandlers)

            // Feedback components
            "Card" -> CreateCard(props, children)
            "Alert" -> CreateAlert(props)
            "ProgressBar" -> CreateProgressBar(props)
            "Badge" -> CreateBadge(props, children)
            "Chip" -> CreateChip(props, eventHandlers)

            else -> {
                // Fallback for unsupported component types
                Text("Unsupported component: $type")
            }
        }
    }

    /**
     * Create a Component (data structure) from UINode for AvaElements integration.
     *
     * This is used by AndroidReactiveRenderer to create Component instances
     * that can be rendered by the existing ComposeRenderer.
     */
    fun createComponent(
        type: String,
        props: Map<String, JsonElement>,
        children: List<Component>?,
        id: String?
    ): Component {
        // Map MEL types to AvaElements component types
        return when (type) {
            "Text" -> com.augmentalis.avaelements.components.phase1.display.Text(
                id = id,
                content = props["value"]?.jsonPrimitive?.contentOrNull
                    ?: props["content"]?.jsonPrimitive?.contentOrNull
                    ?: ""
            )

            "Button" -> com.augmentalis.avaelements.components.phase1.form.Button(
                id = id,
                label = props["label"]?.jsonPrimitive?.contentOrNull ?: "Button",
                onClick = { /* Event handling via MELPluginRenderer */ }
            )

            "Column" -> com.augmentalis.avaelements.components.phase1.layout.Column(
                id = id,
                children = children ?: emptyList()
            )

            "Row" -> com.augmentalis.avaelements.components.phase1.layout.Row(
                id = id,
                children = children ?: emptyList()
            )

            "Container" -> com.augmentalis.avaelements.components.phase1.layout.Container(
                id = id,
                child = children?.firstOrNull()
            )

            "TextField" -> com.augmentalis.avaelements.components.phase1.form.TextField(
                id = id,
                value = props["value"]?.jsonPrimitive?.contentOrNull ?: "",
                placeholder = props["placeholder"]?.jsonPrimitive?.contentOrNull,
                onValueChange = { /* Event handling via MELPluginRenderer */ }
            )

            "Card" -> com.augmentalis.avaelements.components.phase1.layout.Card(
                id = id,
                children = children ?: emptyList()
            )

            else -> {
                // Fallback to Text component
                com.augmentalis.avaelements.components.phase1.display.Text(
                    id = id,
                    content = "Unsupported: $type"
                )
            }
        }
    }

    // ============================================================================
    // Layout Components
    // ============================================================================

    @Composable
    private fun CreateColumn(
        props: Map<String, JsonElement>,
        children: @Composable () -> Unit
    ) {
        val spacing = props["spacing"]?.jsonPrimitive?.intOrNull ?: 0
        val padding = props["padding"]?.jsonPrimitive?.intOrNull ?: 0

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.dp)
        ) {
            children()
        }
    }

    @Composable
    private fun CreateRow(
        props: Map<String, JsonElement>,
        children: @Composable () -> Unit
    ) {
        val spacing = props["spacing"]?.jsonPrimitive?.intOrNull ?: 0
        val padding = props["padding"]?.jsonPrimitive?.intOrNull ?: 0

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing.dp)
        ) {
            children()
        }
    }

    @Composable
    private fun CreateContainer(
        props: Map<String, JsonElement>,
        children: @Composable () -> Unit
    ) {
        val padding = props["padding"]?.jsonPrimitive?.intOrNull ?: 0

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding.dp)
        ) {
            children()
        }
    }

    @Composable
    private fun CreateSpacer(props: Map<String, JsonElement>) {
        val height = props["height"]?.jsonPrimitive?.intOrNull ?: 8
        val width = props["width"]?.jsonPrimitive?.intOrNull

        if (width != null) {
            Spacer(modifier = Modifier.width(width.dp))
        } else {
            Spacer(modifier = Modifier.height(height.dp))
        }
    }

    @Composable
    private fun CreateDivider(props: Map<String, JsonElement>) {
        val thickness = props["thickness"]?.jsonPrimitive?.intOrNull ?: 1
        HorizontalDivider(thickness = thickness.dp)
    }

    // ============================================================================
    // Display Components
    // ============================================================================

    @Composable
    private fun CreateText(props: Map<String, JsonElement>) {
        val content = props["value"]?.jsonPrimitive?.contentOrNull
            ?: props["content"]?.jsonPrimitive?.contentOrNull
            ?: ""
        val fontSize = props["fontSize"]?.jsonPrimitive?.intOrNull ?: 14
        val fontWeight = props["fontWeight"]?.jsonPrimitive?.contentOrNull

        Text(
            text = content,
            fontSize = fontSize.sp,
            style = when (fontWeight) {
                "bold" -> MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                else -> MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp)
            }
        )
    }

    @Composable
    private fun CreateIcon(props: Map<String, JsonElement>) {
        val iconName = props["name"]?.jsonPrimitive?.contentOrNull ?: "star"
        val size = props["size"]?.jsonPrimitive?.intOrNull ?: 24
        val tintColor = props["color"]?.jsonPrimitive?.contentOrNull?.let { parseColor(it) }

        val imageVector = getIconByName(iconName)

        Icon(
            imageVector = imageVector,
            contentDescription = iconName,
            modifier = Modifier.size(size.dp),
            tint = tintColor ?: LocalContentColor.current
        )
    }

    /**
     * Map icon names to Material Icons.
     */
    private fun getIconByName(name: String): ImageVector {
        return when (name.lowercase()) {
            "star" -> Icons.Default.Star
            "star_outline" -> Icons.Outlined.Star
            "delete" -> Icons.Default.Delete
            "add" -> Icons.Default.Add
            "close" -> Icons.Default.Close
            "check" -> Icons.Default.Check
            "favorite" -> Icons.Default.Favorite
            "home" -> Icons.Default.Home
            "settings" -> Icons.Default.Settings
            "search" -> Icons.Default.Search
            "menu" -> Icons.Default.Menu
            "arrow_back" -> Icons.Default.ArrowBack
            "arrow_forward" -> Icons.Default.ArrowForward
            "edit" -> Icons.Default.Edit
            "share" -> Icons.Default.Share
            "info" -> Icons.Default.Info
            "warning" -> Icons.Default.Warning
            "error" -> Icons.Default.Error
            "person" -> Icons.Default.Person
            "email" -> Icons.Default.Email
            "phone" -> Icons.Default.Phone
            "place" -> Icons.Default.Place
            "notifications" -> Icons.Default.Notifications
            "account_circle" -> Icons.Default.AccountCircle
            else -> Icons.Default.HelpOutline
        }
    }

    /**
     * Parse color string to Color.
     * Supports hex colors (#RRGGBB, #AARRGGBB) and common color names.
     */
    private fun parseColor(colorString: String): Color? {
        return try {
            when {
                colorString.startsWith("#") -> {
                    Color(android.graphics.Color.parseColor(colorString))
                }
                else -> when (colorString.lowercase()) {
                    "red" -> Color.Red
                    "blue" -> Color.Blue
                    "green" -> Color.Green
                    "yellow" -> Color.Yellow
                    "black" -> Color.Black
                    "white" -> Color.White
                    "gray" -> Color.Gray
                    else -> null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    @Composable
    private fun CreateImage(props: Map<String, JsonElement>) {
        val src = props["src"]?.jsonPrimitive?.contentOrNull ?: ""
        val width = props["width"]?.jsonPrimitive?.intOrNull
        val height = props["height"]?.jsonPrimitive?.intOrNull
        val contentDescription = props["alt"]?.jsonPrimitive?.contentOrNull ?: ""
        val contentScale = props["fit"]?.jsonPrimitive?.contentOrNull?.let {
            when (it.lowercase()) {
                "cover" -> ContentScale.Crop
                "contain" -> ContentScale.Fit
                "fill" -> ContentScale.FillBounds
                "none" -> ContentScale.None
                else -> ContentScale.Fit
            }
        } ?: ContentScale.Fit

        // Use AsyncImage from Coil for image loading
        AsyncImage(
            model = src,
            contentDescription = contentDescription,
            modifier = Modifier
                .then(if (width != null) Modifier.width(width.dp) else Modifier)
                .then(if (height != null) Modifier.height(height.dp) else Modifier),
            contentScale = contentScale
        )
    }

    // ============================================================================
    // Input Components
    // ============================================================================

    @Composable
    private fun CreateButton(
        props: Map<String, JsonElement>,
        eventHandlers: Map<String, () -> Unit>
    ) {
        val label = props["label"]?.jsonPrimitive?.contentOrNull ?: "Button"
        val variant = props["variant"]?.jsonPrimitive?.contentOrNull ?: "filled"
        val enabled = props["enabled"]?.jsonPrimitive?.booleanOrNull ?: true
        val onClick = eventHandlers["onTap"] ?: eventHandlers["onClick"] ?: {}

        when (variant) {
            "filled", "primary" -> {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(label)
                }
            }
            "outlined", "outline", "secondary" -> {
                OutlinedButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(label)
                }
            }
            "text", "ghost" -> {
                TextButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(label)
                }
            }
            "danger", "error" -> {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(label)
                }
            }
            else -> {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(label)
                }
            }
        }
    }

    @Composable
    private fun CreateTextField(
        props: Map<String, JsonElement>,
        eventHandlers: Map<String, () -> Unit>
    ) {
        val value = props["value"]?.jsonPrimitive?.contentOrNull ?: ""
        val placeholder = props["placeholder"]?.jsonPrimitive?.contentOrNull ?: ""
        val onChange = eventHandlers["onChange"]

        OutlinedTextField(
            value = value,
            onValueChange = { onChange?.invoke() },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun CreateCheckbox(
        props: Map<String, JsonElement>,
        eventHandlers: Map<String, () -> Unit>
    ) {
        val checked = props["checked"]?.jsonPrimitive?.booleanOrNull ?: false
        val label = props["label"]?.jsonPrimitive?.contentOrNull
        val onChange = eventHandlers["onChange"]

        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onChange?.invoke() }
            )
            if (label != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(label)
            }
        }
    }

    @Composable
    private fun CreateSwitch(
        props: Map<String, JsonElement>,
        eventHandlers: Map<String, () -> Unit>
    ) {
        val checked = props["checked"]?.jsonPrimitive?.booleanOrNull ?: false
        val label = props["label"]?.jsonPrimitive?.contentOrNull
        val onChange = eventHandlers["onChange"]

        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            if (label != null) {
                Text(label)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Switch(
                checked = checked,
                onCheckedChange = { onChange?.invoke() }
            )
        }
    }

    @Composable
    private fun CreateSlider(
        props: Map<String, JsonElement>,
        eventHandlers: Map<String, () -> Unit>
    ) {
        val value = props["value"]?.jsonPrimitive?.floatOrNull ?: 0f
        val min = props["min"]?.jsonPrimitive?.floatOrNull ?: 0f
        val max = props["max"]?.jsonPrimitive?.floatOrNull ?: 100f
        val onChange = eventHandlers["onChange"]

        Slider(
            value = value,
            onValueChange = { onChange?.invoke() },
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun CreateDropdown(
        props: Map<String, JsonElement>,
        eventHandlers: Map<String, () -> Unit>
    ) {
        val value = props["value"]?.jsonPrimitive?.contentOrNull ?: ""
        val options = props["options"]?.jsonArray?.map {
            it.jsonPrimitive.contentOrNull ?: ""
        } ?: emptyList()
        val onChange = eventHandlers["onChange"]

        // Placeholder for dropdown - would use DropdownMenu in full implementation
        OutlinedTextField(
            value = value,
            onValueChange = { onChange?.invoke() },
            label = { Text("Select") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // ============================================================================
    // Feedback Components
    // ============================================================================

    @Composable
    private fun CreateCard(
        props: Map<String, JsonElement>,
        children: @Composable () -> Unit
    ) {
        val padding = props["padding"]?.jsonPrimitive?.intOrNull ?: 16

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(padding.dp)
            ) {
                children()
            }
        }
    }

    @Composable
    private fun CreateAlert(props: Map<String, JsonElement>) {
        val message = props["message"]?.jsonPrimitive?.contentOrNull ?: ""
        val type = props["type"]?.jsonPrimitive?.contentOrNull ?: "info"

        val containerColor = when (type) {
            "error" -> MaterialTheme.colorScheme.errorContainer
            "warning" -> MaterialTheme.colorScheme.tertiaryContainer
            "success" -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Composable
    private fun CreateProgressBar(props: Map<String, JsonElement>) {
        val value = props["value"]?.jsonPrimitive?.floatOrNull
        val indeterminate = props["indeterminate"]?.jsonPrimitive?.booleanOrNull ?: false

        if (indeterminate || value == null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearProgressIndicator(
                progress = { value / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun CreateBadge(
        props: Map<String, JsonElement>,
        children: @Composable () -> Unit
    ) {
        val count = props["count"]?.jsonPrimitive?.intOrNull
        val showZero = props["showZero"]?.jsonPrimitive?.booleanOrNull ?: false
        val max = props["max"]?.jsonPrimitive?.intOrNull ?: 99
        val variant = props["variant"]?.jsonPrimitive?.contentOrNull ?: "default"

        val containerColor = when (variant) {
            "error" -> MaterialTheme.colorScheme.error
            "success" -> MaterialTheme.colorScheme.primary
            "warning" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        }

        BadgedBox(
            badge = {
                if (count != null && (count > 0 || showZero)) {
                    Badge(
                        containerColor = containerColor
                    ) {
                        Text(
                            text = if (count > max) "$max+" else count.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else if (props["dot"]?.jsonPrimitive?.booleanOrNull == true) {
                    Badge(containerColor = containerColor)
                }
            }
        ) {
            children()
        }
    }

    @Composable
    private fun CreateChip(
        props: Map<String, JsonElement>,
        eventHandlers: Map<String, () -> Unit>
    ) {
        val label = props["label"]?.jsonPrimitive?.contentOrNull ?: "Chip"
        val selected = props["selected"]?.jsonPrimitive?.booleanOrNull ?: false
        val enabled = props["enabled"]?.jsonPrimitive?.booleanOrNull ?: true
        val variant = props["variant"]?.jsonPrimitive?.contentOrNull ?: "assist"
        val onClick = eventHandlers["onClick"] ?: eventHandlers["onTap"]

        when (variant) {
            "filter" -> {
                FilterChip(
                    selected = selected,
                    onClick = { onClick?.invoke() },
                    label = { Text(label) },
                    enabled = enabled
                )
            }
            "input" -> {
                InputChip(
                    selected = selected,
                    onClick = { onClick?.invoke() },
                    label = { Text(label) },
                    enabled = enabled
                )
            }
            "suggestion" -> {
                SuggestionChip(
                    onClick = { onClick?.invoke() },
                    label = { Text(label) },
                    enabled = enabled
                )
            }
            else -> {
                AssistChip(
                    onClick = { onClick?.invoke() },
                    label = { Text(label) },
                    enabled = enabled
                )
            }
        }
    }

    companion object {
        /**
         * Get list of supported component types.
         */
        fun getSupportedTypes(): Set<String> {
            return setOf(
                // Layout
                "Column", "Row", "Container", "Spacer", "Divider",
                // Display
                "Text", "Icon", "Image",
                // Input
                "Button", "TextField", "Checkbox", "Switch", "Slider", "Dropdown",
                // Feedback
                "Card", "Alert", "ProgressBar", "Badge", "Chip"
            )
        }

        /**
         * Check if a component type is supported.
         */
        fun isSupported(type: String): Boolean {
            return type in getSupportedTypes()
        }
    }
}
