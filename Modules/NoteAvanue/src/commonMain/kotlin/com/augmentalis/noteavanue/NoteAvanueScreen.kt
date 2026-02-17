package com.augmentalis.noteavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

/**
 * NoteAvanueScreen — Standalone rich note editor screen.
 *
 * SpatialVoice gradient background, transparent TopAppBar,
 * scrollable formatting toolbar, compose-rich-editor with Markdown round-trip.
 * Follows the PhotoAvanueScreen self-running module pattern.
 *
 * @param initialMarkdown Markdown content to load (empty for new note)
 * @param initialTitle Note title
 * @param onBack Navigate back callback
 * @param onSave Save callback with (title, markdownContent)
 * @param onAttachFile Trigger file attachment picker
 * @param onTakePhoto Trigger camera capture
 * @param onStartDictation Toggle dictation mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteAvanueScreen(
    initialMarkdown: String = "",
    initialTitle: String = "",
    onBack: () -> Unit = {},
    onSave: (String, String) -> Unit = { _, _ -> },
    onAttachFile: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onStartDictation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val richTextState = rememberRichTextState()
    var title by remember { mutableStateOf(initialTitle) }
    var hasLoaded by remember { mutableStateOf(false) }

    // Load initial Markdown content once
    if (!hasLoaded && initialMarkdown.isNotBlank()) {
        richTextState.setMarkdown(initialMarkdown)
        hasLoaded = true
    }

    // SpatialVoice gradient background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            colors.background,
            colors.surface.copy(alpha = 0.6f),
            colors.background
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        // ── Transparent TopAppBar ─────────────────────────────────────
        TopAppBar(
            title = { Text("NoteAvanue", color = colors.textPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
            },
            actions = {
                IconButton(onClick = { onSave(title, richTextState.toMarkdown()) }) {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = colors.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // ── Title field ──────────────────────────────────────────────
        BasicTextField(
            value = title,
            onValueChange = { title = it },
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            cursorBrush = SolidColor(colors.primary),
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (title.isEmpty()) {
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

        // ── Formatting toolbar ───────────────────────────────────────
        FormattingToolbar(richTextState = richTextState)

        // ── Rich Text Editor ─────────────────────────────────────────
        RichTextEditor(
            state = richTextState,
            placeholder = {
                Text(
                    "Start typing or dictate...",
                    color = colors.textPrimary.copy(alpha = 0.3f)
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
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // ── Action bar ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Word/char count
            val markdown = richTextState.toMarkdown()
            val wc = markdown.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            Text(
                "$wc words  |  ${markdown.length} chars",
                color = colors.textPrimary.copy(alpha = 0.5f),
                fontSize = 12.sp
            )

            // Quick actions
            Row {
                IconButton(onClick = onTakePhoto, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.CameraAlt, "Photo", tint = colors.textPrimary.copy(alpha = 0.7f))
                }
                IconButton(onClick = onAttachFile, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.AttachFile, "Attach", tint = colors.textPrimary.copy(alpha = 0.7f))
                }
                IconButton(onClick = onStartDictation, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Mic, "Dictate", tint = colors.primary)
                }
            }
        }
    }
}

/**
 * Scrollable formatting toolbar for the rich text editor.
 *
 * Groups: [B][I][U][S] | [H1][H2][H3] | [bullet][numbered][checklist][code][quote][divider]
 */
@Composable
private fun FormattingToolbar(
    richTextState: RichTextState,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.3f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Inline styles ──────────────────────────────────────────
        FormatButton(
            icon = Icons.Default.FormatBold,
            label = "Bold",
            isActive = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold,
            onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
        )
        FormatButton(
            icon = Icons.Default.FormatItalic,
            label = "Italic",
            isActive = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
            onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
        )
        FormatButton(
            icon = Icons.Default.FormatUnderlined,
            label = "Underline",
            isActive = richTextState.currentSpanStyle.textDecoration == TextDecoration.Underline,
            onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
        )
        FormatButton(
            icon = Icons.Default.FormatStrikethrough,
            label = "Strikethrough",
            isActive = richTextState.currentSpanStyle.textDecoration == TextDecoration.LineThrough,
            onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
        )

        ToolbarDivider()

        // ── Headings ───────────────────────────────────────────────
        HeadingButton(level = 1, richTextState = richTextState)
        HeadingButton(level = 2, richTextState = richTextState)
        HeadingButton(level = 3, richTextState = richTextState)

        ToolbarDivider()

        // ── Block styles ───────────────────────────────────────────
        @Suppress("DEPRECATION")
        FormatButton(
            icon = Icons.Default.FormatListBulleted,
            label = "Bullet List",
            isActive = richTextState.isUnorderedList,
            onClick = { richTextState.toggleUnorderedList() }
        )
        FormatButton(
            icon = Icons.Default.FormatListNumbered,
            label = "Numbered List",
            isActive = richTextState.isOrderedList,
            onClick = { richTextState.toggleOrderedList() }
        )
        FormatButton(
            icon = Icons.Default.Check,
            label = "Checklist",
            isActive = false,
            onClick = {
                // Insert markdown checklist item
                richTextState.addTextAfterSelection("- [ ] ")
            }
        )
        FormatButton(
            icon = Icons.Default.Code,
            label = "Code Block",
            isActive = richTextState.isCodeSpan,
            onClick = { richTextState.toggleCodeSpan() }
        )
        FormatButton(
            icon = Icons.Default.FormatQuote,
            label = "Blockquote",
            isActive = false,
            onClick = {
                richTextState.addTextAfterSelection("> ")
            }
        )
        FormatButton(
            icon = Icons.Default.HorizontalRule,
            label = "Divider",
            isActive = false,
            onClick = {
                richTextState.addTextAfterSelection("\n---\n")
            }
        )
    }
}

/**
 * Individual formatting button with active state highlighting.
 */
@Composable
private fun FormatButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    val tint = if (isActive) colors.primary else colors.textPrimary.copy(alpha = 0.7f)
    val bgColor = if (isActive) colors.primary.copy(alpha = 0.15f) else Color.Transparent

    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = bgColor),
        modifier = Modifier.size(36.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/**
 * Heading level button (H1/H2/H3) with paragraph-level toggle.
 */
@Composable
private fun HeadingButton(
    level: Int,
    richTextState: RichTextState
) {
    val colors = AvanueTheme.colors
    val fontSize = when (level) {
        1 -> 28.sp
        2 -> 22.sp
        else -> 18.sp
    }
    val isActive = richTextState.currentSpanStyle.fontSize == fontSize

    FormatButton(
        icon = Icons.Default.FormatBold, // Reused icon; text label distinguishes
        label = "H$level",
        isActive = isActive,
        onClick = {
            val style = SpanStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
            richTextState.toggleSpanStyle(style)
        }
    )
}

/**
 * Visual separator between toolbar groups.
 */
@Composable
private fun ToolbarDivider() {
    val colors = AvanueTheme.colors
    Spacer(Modifier.width(2.dp))
    VerticalDivider(
        modifier = Modifier.height(24.dp),
        color = colors.textPrimary.copy(alpha = 0.15f),
        thickness = 1.dp
    )
    Spacer(Modifier.width(2.dp))
}
