package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.FrameContent

/**
 * Cross-platform terminal / log viewer composable.
 *
 * Displays monospace text content in a scrollable list with optional
 * line numbers and auto-scroll to bottom behavior. Designed for
 * viewing logs, command output, or streaming data.
 *
 * Features:
 * - Monospace font for aligned text
 * - Auto-scroll to bottom on new content
 * - Line numbers (toggled by [showLineNumbers])
 * - Dark terminal aesthetic using AvanueTheme colors
 * - SelectionContainer for text copy support
 * - Horizontal scroll for long lines
 *
 * @param content The Terminal content model
 * @param showLineNumbers Whether to show line number gutter
 * @param modifier Compose modifier
 */
@Composable
fun TerminalContent(
    content: FrameContent.Terminal,
    showLineNumbers: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val lines = remember(content.content) {
        val raw = content.content.lines()
        if (raw.size > content.maxLines) {
            raw.takeLast(content.maxLines)
        } else raw
    }
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val fontSize = content.fontSize.sp

    // Auto-scroll to bottom when content changes
    LaunchedEffect(content.content, content.autoScroll) {
        if (content.autoScroll && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        if (lines.isEmpty() || (lines.size == 1 && lines[0].isBlank())) {
            // Empty state
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    text = "Terminal",
                    color = colors.textPrimary.copy(alpha = 0.5f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No output yet",
                    color = colors.textPrimary.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    itemsIndexed(lines) { index, line ->
                        TerminalLine(
                            lineNumber = index + 1,
                            text = line,
                            fontSize = fontSize,
                            showLineNumber = showLineNumbers,
                            textColor = categorizeLineColor(line, colors)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single terminal line with optional line number gutter.
 */
@Composable
private fun TerminalLine(
    lineNumber: Int,
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    showLineNumber: Boolean,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Row(modifier = modifier.fillMaxWidth()) {
        if (showLineNumber) {
            Text(
                text = lineNumber.toString().padStart(4),
                color = colors.textPrimary.copy(alpha = 0.25f),
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(48.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            softWrap = false,
            maxLines = 1
        )
    }
}

/**
 * Heuristic line coloring based on common log patterns.
 */
@Composable
private fun categorizeLineColor(
    line: String,
    colors: com.augmentalis.avanueui.theme.AvanueColorScheme
): androidx.compose.ui.graphics.Color {
    val lower = line.lowercase()
    return when {
        lower.contains("error") || lower.contains("exception") || lower.contains("fatal") ->
            colors.error
        lower.contains("warn") ->
            colors.warning
        lower.contains("success") || lower.contains("done") || lower.contains("completed") ->
            colors.success
        lower.startsWith("$") || lower.startsWith(">") || lower.startsWith("#") ->
            colors.primary
        else -> colors.textPrimary.copy(alpha = 0.85f)
    }
}
