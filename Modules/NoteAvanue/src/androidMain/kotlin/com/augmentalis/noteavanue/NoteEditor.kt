package com.augmentalis.noteavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.noteavanue.model.NoteAttachment
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.handlers.ModuleCommandCallbacks
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

/**
 * NoteEditor — Embeddable rich note editor for Cockpit frames.
 *
 * No TopAppBar (embedded mode). Uses compose-rich-editor for rich text
 * with Markdown round-trip. Loads from `initialContent` (Markdown),
 * saves via `onSave(title, markdownContent)`.
 *
 * @param initialTitle Pre-loaded title
 * @param initialContent Markdown content to load
 * @param attachments Inline attachment chips
 * @param onSave Callback with (title, markdownContent)
 * @param onAttachFile Trigger file picker
 * @param onTakePhoto Trigger camera
 * @param onStartDictation Toggle dictation mode
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    val richTextState = rememberRichTextState()
    var titleField by remember { mutableStateOf(initialTitle) }
    var hasLoaded by remember(initialContent) { mutableStateOf(false) }

    // Load initial Markdown content once
    if (!hasLoaded && initialContent.isNotBlank()) {
        richTextState.setMarkdown(initialContent)
        hasLoaded = true
    }

    // Wire voice command executor for note formatting/editing
    DisposableEffect(richTextState) {
        ModuleCommandCallbacks.noteExecutor = { actionType, metadata ->
            executeNoteCommand(richTextState, actionType, metadata,
                getTitle = { titleField },
                doSave = { onSave(titleField, richTextState.toMarkdown()) },
                doAttachFile = onAttachFile,
                doTakePhoto = onTakePhoto,
                doStartDictation = onStartDictation
            )
        }
        onDispose { ModuleCommandCallbacks.noteExecutor = null }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Compact toolbar ──────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                val boldActive = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold
                IconButton(
                    onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (boldActive) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.FormatBold, "Bold",
                        tint = if (boldActive) colors.primary else colors.textPrimary
                    )
                }

                val italicActive = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic
                IconButton(
                    onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (italicActive) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.FormatItalic, "Italic",
                        tint = if (italicActive) colors.primary else colors.textPrimary
                    )
                }
            }
            Row {
                IconButton(onClick = onTakePhoto) {
                    Icon(Icons.Default.CameraAlt, "Photo", tint = colors.textPrimary)
                }
                IconButton(onClick = onAttachFile) {
                    Icon(Icons.Default.AttachFile, "Attach", tint = colors.textPrimary)
                }
                IconButton(onClick = onStartDictation) {
                    Icon(Icons.Default.Mic, "Dictate", tint = colors.primary)
                }
                IconButton(onClick = { onSave(titleField, richTextState.toMarkdown()) }) {
                    Icon(Icons.Default.Save, "Save", tint = colors.primary)
                }
            }
        }

        // ── Title ────────────────────────────────────────────────────
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            BasicTextField(
                value = titleField,
                onValueChange = { titleField = it },
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { inner ->
                    Box {
                        if (titleField.isEmpty()) {
                            Text(
                                "Title",
                                color = colors.textPrimary.copy(alpha = 0.4f),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── Rich Text Editor ─────────────────────────────────────
            RichTextEditor(
                state = richTextState,
                placeholder = {
                    Text(
                        "Start typing or dictate...",
                        color = colors.textPrimary.copy(alpha = 0.3f),
                        fontSize = 16.sp
                    )
                },
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    textColor = colors.textPrimary.copy(alpha = 0.9f),
                    cursorColor = colors.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // ── Attachments ──────────────────────────────────────────
            if (attachments.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Attachments",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attachments.forEach { att ->
                        AssistChip(
                            onClick = {},
                            label = { Text(att.name.ifBlank { att.type.name }, maxLines = 1) }
                        )
                    }
                }
            }
        }

        // ── Status bar ───────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val markdown = richTextState.toMarkdown()
            val wc = markdown.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            Text("$wc words", color = colors.textPrimary.copy(alpha = 0.5f), fontSize = 12.sp)
            Text("${markdown.length} chars", color = colors.textPrimary.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

/**
 * Maps note voice commands to RichTextState formatting operations.
 *
 * Uses compose-rich-editor's toggleSpanStyle for bold/italic/underline/strikethrough.
 * Save triggers the parent onSave callback with current markdown.
 */
@Suppress("CyclomaticComplexMethod")
private fun executeNoteCommand(
    richTextState: com.mohamedrejeb.richeditor.model.RichTextState,
    actionType: CommandActionType,
    metadata: Map<String, String>,
    getTitle: () -> String,
    doSave: () -> Unit,
    doAttachFile: () -> Unit,
    doTakePhoto: () -> Unit,
    doStartDictation: () -> Unit,
): HandlerResult {
    return when (actionType) {
        // ── Formatting ────────────────────────────────────────────────
        CommandActionType.FORMAT_BOLD -> {
            richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            HandlerResult.success("Bold toggled")
        }
        CommandActionType.FORMAT_ITALIC -> {
            richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
            HandlerResult.success("Italic toggled")
        }
        CommandActionType.FORMAT_UNDERLINE -> {
            richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            HandlerResult.success("Underline toggled")
        }
        CommandActionType.FORMAT_STRIKETHROUGH -> {
            richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
            HandlerResult.success("Strikethrough toggled")
        }
        CommandActionType.HEADING_1 -> {
            richTextState.toggleSpanStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold))
            HandlerResult.success("Heading 1")
        }
        CommandActionType.HEADING_2 -> {
            richTextState.toggleSpanStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
            HandlerResult.success("Heading 2")
        }
        CommandActionType.HEADING_3 -> {
            richTextState.toggleSpanStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold))
            HandlerResult.success("Heading 3")
        }
        CommandActionType.CODE_BLOCK -> {
            richTextState.toggleCodeSpan()
            HandlerResult.success("Code block toggled")
        }

        // ── Editing ───────────────────────────────────────────────────
        CommandActionType.NOTE_UNDO -> {
            // compose-rich-editor RC13 does not expose undo API
            HandlerResult.failure("Undo not available in current editor version", recoverable = true)
        }
        CommandActionType.NOTE_REDO -> {
            // compose-rich-editor RC13 does not expose redo API
            HandlerResult.failure("Redo not available in current editor version", recoverable = true)
        }
        CommandActionType.CLEAR_FORMATTING -> {
            richTextState.removeSpanStyle(richTextState.currentSpanStyle)
            HandlerResult.success("Formatting cleared")
        }

        // ── Lifecycle ─────────────────────────────────────────────────
        CommandActionType.SAVE_NOTE -> {
            doSave()
            HandlerResult.success("Note saved")
        }
        CommandActionType.ATTACH_FILE -> {
            doAttachFile()
            HandlerResult.success("File picker opened")
        }
        CommandActionType.ATTACH_AUDIO -> {
            doStartDictation()
            HandlerResult.success("Dictation started")
        }

        // ── Word count ────────────────────────────────────────────────
        CommandActionType.WORD_COUNT -> {
            val md = richTextState.toMarkdown()
            val words = md.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            HandlerResult.success("$words words, ${md.length} characters")
        }

        // ── Insert text from metadata (dictation pipeline) ────────────
        CommandActionType.INSERT_TEXT -> {
            val text = metadata["text"] ?: ""
            if (text.isNotBlank()) {
                richTextState.setMarkdown(richTextState.toMarkdown() + text)
                HandlerResult.success("Text inserted")
            } else {
                HandlerResult.failure("No text to insert", recoverable = true)
            }
        }

        else -> HandlerResult.failure("Unsupported note action: $actionType", recoverable = true)
    }
}
