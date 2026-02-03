package com.augmentalis.avaelements.renderers.android.mappers
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.components.phase1.form.*
import com.augmentalis.avaelements.components.phase1.display.*
import com.augmentalis.avaelements.components.phase1.layout.*
import com.augmentalis.avaelements.components.phase1.navigation.*
import com.augmentalis.avaelements.components.phase1.data.*

@Composable fun RenderCheckbox(c: Checkbox, theme: Theme) = Checkbox(
    checked = c.checked,
    onCheckedChange = c.onChange,
    enabled = c.enabled
)

@Composable fun RenderTextField(c: TextField, theme: Theme) = OutlinedTextField(
    value = c.value, 
    onValueChange = c.onChange ?: {}, 
    label = c.label?.let {{ Text(it) }}, 
    enabled = c.enabled, 
    singleLine = !c.multiline,
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = theme.colorScheme.primary.toCompose(),
        focusedLabelColor = theme.colorScheme.primary.toCompose()
    )
)

@Composable fun RenderButton(c: Button, theme: Theme) = Button(
    onClick = c.onClick ?: {},
    enabled = c.enabled
) { Text(c.text) }

@Composable fun RenderSwitch(c: Switch, theme: Theme) = Switch(
    checked = c.checked, 
    onCheckedChange = c.onChange, 
    enabled = c.enabled,
    colors = SwitchDefaults.colors(
        checkedThumbColor = theme.colorScheme.primary.toCompose()
    )
)

@Composable fun RenderText(c: com.augmentalis.avaelements.components.phase1.display.Text, theme: Theme) = Text(
    text = c.content
)

@Composable fun RenderImage(c: Image, theme: Theme) = Text("Image: ${c.source}")
@Composable fun RenderIcon(c: Icon, theme: Theme) = Text("Icon: ${c.name}")
@Composable fun RenderContainer(c: Container, theme: Theme) = Box(modifier = Modifier.fillMaxWidth()) { Text("Container") }
@Composable fun RenderRow(c: com.augmentalis.avaelements.components.phase1.layout.Row, theme: Theme) = Row(modifier = Modifier.fillMaxWidth()) { Text("Row") }
@Composable fun RenderColumn(c: com.augmentalis.avaelements.components.phase1.layout.Column, theme: Theme) = Column(modifier = Modifier.fillMaxWidth()) { Text("Column") }

@Composable fun RenderCard(c: Card, theme: Theme) = Card(
    modifier = Modifier.fillMaxWidth().padding(8.dp)
) { Text("Card") }

@Composable fun RenderScrollView(c: ScrollView, theme: Theme) = Text("ScrollView")
@Composable fun RenderList(c: com.augmentalis.avaelements.components.phase1.data.List, theme: Theme) = Text("List")
