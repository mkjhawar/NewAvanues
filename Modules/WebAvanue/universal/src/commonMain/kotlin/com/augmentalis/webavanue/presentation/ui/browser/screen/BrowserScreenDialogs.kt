package com.augmentalis.webavanue.ui.screen.browser.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * AddPageDialog - Dialog for adding a new page with URL input
 *
 * @param url Current URL value
 * @param onUrlChange Callback when URL changes
 * @param onConfirm Callback when user confirms (creates new tab)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun AddPageDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.augmentalis.webavanue.presentation.ui.components.OceanDialog(
        onDismissRequest = onDismiss,
        title = "Add New Page",
        modifier = modifier,
        confirmButton = {
            com.augmentalis.webavanue.presentation.ui.components.OceanTextButton(
                onClick = onConfirm,
                isPrimary = true
            ) {
                Text("Add Page")
            }
        },
        dismissButton = {
            com.augmentalis.webavanue.presentation.ui.components.OceanTextButton(
                onClick = onDismiss,
                isPrimary = false
            ) {
                Text("Cancel")
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Enter a URL or leave blank for a new empty tab",
                style = MaterialTheme.typography.bodyMedium,
                color = com.augmentalis.webavanue.presentation.ui.components.OceanDialogDefaults.textSecondary
            )

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("example.com or google.com")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onConfirm() }),
                colors = com.augmentalis.webavanue.presentation.ui.components.OceanDialogDefaults.outlinedTextFieldColors()
            )
        }
    }
}

/**
 * WebViewPlaceholder - Placeholder for actual WebView (development/testing)
 *
 * @param url Current URL
 * @param isLoading Whether page is loading
 * @param modifier Modifier for customization
 */
@Composable
fun WebViewPlaceholder(
    url: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = if (url.isBlank()) "Enter a URL to browse" else url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "WebView will be integrated here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * EmptyBrowserState - Shown when no tabs are open
 *
 * @param onNewTab Callback when new tab button is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun EmptyBrowserState(
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No tabs open",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Create a new tab to start browsing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onNewTab) {
                Text("New Tab")
            }
        }
    }
}
