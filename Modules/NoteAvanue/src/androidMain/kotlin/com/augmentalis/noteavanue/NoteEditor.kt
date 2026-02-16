package com.augmentalis.noteavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.noteavanue.model.NoteAttachment

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditor(
    initialTitle: String = "",
    initialContent: String = "",
    attachments: List<NoteAttachment> = emptyList(),
    onSave: (String, String) -> Unit = { _, _ -> },
    onAttachFile: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onStartDictation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var titleField by remember { mutableStateOf(TextFieldValue(initialTitle)) }
    var contentField by remember { mutableStateOf(TextFieldValue(initialContent)) }

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar
        Row(
            Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = {}) { Icon(Icons.Default.FormatBold, "Bold", tint = colors.onSurface) }
                IconButton(onClick = {}) { Icon(Icons.Default.FormatItalic, "Italic", tint = colors.onSurface) }
            }
            Row {
                IconButton(onClick = onTakePhoto) { Icon(Icons.Default.CameraAlt, "Photo", tint = colors.onSurface) }
                IconButton(onClick = onAttachFile) { Icon(Icons.Default.AttachFile, "Attach", tint = colors.onSurface) }
                IconButton(onClick = onStartDictation) { Icon(Icons.Default.Mic, "Dictate", tint = colors.primary) }
                IconButton(onClick = { onSave(titleField.text, contentField.text) }) { Icon(Icons.Default.Save, "Save", tint = colors.primary) }
            }
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
            BasicTextField(
                value = titleField, onValueChange = { titleField = it },
                textStyle = TextStyle(color = colors.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { inner -> Box { if (titleField.text.isEmpty()) Text("Title", color = colors.onBackground.copy(alpha = 0.4f), fontSize = 24.sp, fontWeight = FontWeight.Bold); inner() } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = contentField, onValueChange = { contentField = it },
                textStyle = TextStyle(color = colors.onBackground.copy(alpha = 0.9f), fontSize = 16.sp, lineHeight = 24.sp),
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { inner -> Box { if (contentField.text.isEmpty()) Text("Start typing or dictate...", color = colors.onBackground.copy(alpha = 0.3f), fontSize = 16.sp); inner() } },
                modifier = Modifier.fillMaxWidth().height(400.dp)
            )
            if (attachments.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Attachments", color = colors.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    attachments.forEach { att -> AssistChip(onClick = {}, label = { Text(att.name.ifBlank { att.type.name }, maxLines = 1) }) }
                }
            }
        }

        Row(Modifier.fillMaxWidth().background(colors.surface.copy(alpha = 0.5f)).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val wc = contentField.text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            Text("$wc words", color = colors.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
            Text("${contentField.text.length} chars", color = colors.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}
