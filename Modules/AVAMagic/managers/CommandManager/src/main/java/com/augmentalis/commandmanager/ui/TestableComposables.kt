/**
 * TestableComposables.kt - Test-accessible UI components
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team  
 * Created: 2025-01-28
 * 
 * Provides public/internal versions of UI components for testing
 */
package com.augmentalis.commandmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceos.command.*

/**
 * CommandManagerContent - Wrapper for CommandManagerScreen for testing
 */
@Composable
fun CommandManagerContent(
    viewModel: CommandViewModel
) {
    CommandManagerScreen(viewModel = viewModel)
}

/**
 * ErrorDisplay - Display error messages
 */
@Composable
fun ErrorDisplay(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Dismiss")
            }
        }
    }
}

/**
 * SuccessDisplay - Display success messages
 */
@Composable
fun SuccessDisplay(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss
            ) {
                Text("Dismiss")
            }
        }
    }
}