package com.augmentalis.cockpit.mvp.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.avanues.cockpit.core.window.WindowContent
import com.augmentalis.cockpit.mvp.OceanTheme

/**
 * DocumentViewerContent - Placeholder for document viewing
 */
@Composable
fun DocumentViewerContent(
    documentContent: WindowContent.DocumentContent,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanTheme.backgroundStart),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Document",
                modifier = Modifier.size(64.dp),
                tint = OceanTheme.primary
            )

            Text(
                text = "Document Viewer",
                style = MaterialTheme.typography.headlineMedium,
                color = OceanTheme.textPrimary
            )

            Text(
                text = "Type: ${documentContent.documentType}",
                style = MaterialTheme.typography.bodyMedium,
                color = OceanTheme.textSecondary
            )
        }
    }
}
