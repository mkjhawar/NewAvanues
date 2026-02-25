package com.augmentalis.fileavanue

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.fileavanue.model.FileItem

/**
 * Bottom sheet showing file details on long-press.
 *
 * Displays: filename, path, size, MIME type, dates, preview (Coil for images),
 * and action buttons (Open, Share, Delete, Copy Path). All buttons have AVID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailSheet(
    file: FileItem,
    onDismiss: () -> Unit,
    onDelete: (FileItem) -> Unit,
    onOpen: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Image preview
            if (file.isImage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = file.thumbnailUri.ifBlank { file.uri },
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.matchParentSize()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // File name
            Text(
                file.name,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            // Metadata rows
            DetailRow("Path", file.uri)
            DetailRow("Size", file.formattedSize)
            DetailRow("Type", file.mimeType)
            if (file.dateModified > 0) {
                DetailRow("Modified", formatEpochMs(file.dateModified))
            }
            if (file.dateCreated > 0) {
                DetailRow("Created", formatEpochMs(file.dateCreated))
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colors.border)
            Spacer(Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Open
                Button(
                    onClick = { onOpen(file) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Voice: click Open" }
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open")
                }

                // Share
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = file.mimeType
                            putExtra(Intent.EXTRA_TEXT, file.uri)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Voice: click Share" }
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy Path
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(file.uri))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Voice: click Copy Path" }
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy Path")
                }

                // Delete
                OutlinedButton(
                    onClick = { onDelete(file) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Voice: click Delete" }
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = AvanueTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            color = colors.textPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Format epoch milliseconds to a human-readable date string.
 * Uses simple formatting without java.time (KMP-friendly pattern).
 */
private fun formatEpochMs(epochMs: Long): String {
    if (epochMs <= 0) return "Unknown"
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) + 1
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} " +
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
