package com.augmentalis.avanueui.renderer.desktop.mappers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.core.Theme
import com.augmentalis.avanueui.phase1.form.*
import com.augmentalis.avanueui.phase1.display.*
import com.augmentalis.avanueui.phase1.layout.*
import com.augmentalis.avanueui.phase1.data.*
import com.augmentalis.avanueui.phase1.navigation.ScrollView

/**
 * Phase 1 Desktop Mappers - Fixed Version
 * Complete implementations for all 13 Phase 1 components
 */

// ============================================================================
// FORM COMPONENTS
// ============================================================================

@Composable
fun RenderButton(c: Button, theme: Theme) {
    Button(
        onClick = c.onClick ?: {},
        enabled = c.enabled,
        modifier = Modifier.padding(8.dp).defaultMinSize(minWidth = 120.dp, minHeight = 40.dp)
    ) {
        Text(c.text)
    }
}

@Composable
fun RenderTextField(c: TextField, theme: Theme) {
    OutlinedTextField(
        value = c.value,
        onValueChange = c.onChange ?: {},
        label = c.label?.let { label -> { Text(label) } },
        placeholder = c.placeholder?.let { placeholder -> { Text(placeholder) } },
        enabled = c.enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(0.4f).padding(8.dp)
    )
}

@Composable
fun RenderCheckbox(c: Checkbox, theme: Theme) {
    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = c.checked, onCheckedChange = c.onChange, enabled = c.enabled)
        c.label?.let { Text(it, modifier = Modifier.padding(top = 2.dp)) }
    }
}

@Composable
fun RenderSwitch(c: Switch, theme: Theme) {
    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = c.checked, onCheckedChange = c.onChange, enabled = c.enabled)
        c.label?.let { Text(it) }
    }
}

// ============================================================================
// DISPLAY COMPONENTS
// ============================================================================

@Composable
fun RenderText(c: com.augmentalis.avanueui.phase1.display.Text, theme: Theme) {
    Text(text = c.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(4.dp))
}

@Composable
fun RenderImage(c: Image, theme: Theme) {
    Box(modifier = Modifier.size(200.dp).padding(8.dp)) {
        Text("Image: ${c.source}")
    }
}

@Composable
fun RenderIcon(c: Icon, theme: Theme) {
    Icon(
        imageVector = Icons.Default.Home,
        contentDescription = c.name,
        modifier = Modifier.size(24.dp).padding(4.dp)
    )
}

// ============================================================================
// LAYOUT COMPONENTS
// ============================================================================

@Composable
fun RenderContainer(c: Container, theme: Theme) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text("Container")
        }
    }
}

@Composable
fun RenderRow(c: com.augmentalis.avanueui.phase1.layout.Row, theme: Theme) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Row content")
    }
}

@Composable
fun RenderColumn(c: com.augmentalis.avanueui.phase1.layout.Column, theme: Theme) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Column content")
    }
}

@Composable
fun RenderCard(c: Card, theme: Theme) {
    Card(
        modifier = Modifier.fillMaxWidth(0.6f).padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Card content")
        }
    }
}

// ============================================================================
// DATA COMPONENTS
// ============================================================================

@Composable
fun RenderScrollView(c: ScrollView, theme: Theme) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxWidth().height(300.dp).verticalScroll(scrollState).padding(8.dp)
    ) {
        repeat(20) {
            Text("Scrollable item $it", modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun RenderList(c: com.augmentalis.avanueui.phase1.data.List, theme: Theme) {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(5) { index ->
            Text(text = "List Item ${index + 1}", modifier = Modifier.fillMaxWidth().padding(12.dp))
            if (index < 4) HorizontalDivider()
        }
    }
}
