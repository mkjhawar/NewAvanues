package com.augmentalis.webavanue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.OceanDialog
import com.augmentalis.webavanue.OceanDialogDefaults
import com.augmentalis.webavanue.OceanTextButton

/**
 * AddPageDialog - Dialog for adding a new page with URL input
 *
 * FIX: URL validation now happens inside the dialog, keeping it open if validation fails.
 * The dialog validates the URL synchronously before calling onConfirm.
 *
 * @param url Current URL value
 * @param onUrlChange Callback when URL changes
 * @param onConfirm Callback when user confirms with validated URL - receives the formatted/validated URL
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun AddPageDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: (validatedUrl: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // FIX: Track validation error state inside dialog
    var validationError by remember { mutableStateOf<String?>(null) }

    // FIX: Validate URL before confirming - keep dialog open if invalid
    fun validateAndConfirm() {
        // Clear previous error
        validationError = null

        // Allow blank URLs (creates new blank tab)
        if (url.isBlank()) {
            onConfirm("")
            return
        }

        // Format URL with scheme if needed
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://") &&
            !url.startsWith("avanues://") && !url.startsWith("about:") &&
            !url.startsWith("file://") && !url.startsWith("data:")) {
            "https://$url"
        } else {
            url
        }

        // Validate using UrlValidation
        val result = UrlValidation.validate(formattedUrl, allowBlank = true)
        when (result) {
            is UrlValidation.UrlValidationResult.Valid -> {
                // URL is valid - pass the normalized URL to confirm
                onConfirm(result.normalizedUrl)
            }
            is UrlValidation.UrlValidationResult.Invalid -> {
                // URL is invalid - show error, keep dialog open
                validationError = result.error.userMessage
            }
        }
    }

    OceanDialog(
        onDismissRequest = onDismiss,
        title = "Add New Page",
        modifier = modifier,
        confirmButton = {
            OceanTextButton(
                onClick = { validateAndConfirm() },
                isPrimary = true
            ) {
                Text("Add Page")
            }
        },
        dismissButton = {
            OceanTextButton(
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
                color = OceanDialogDefaults.textSecondary
            )

            OutlinedTextField(
                value = url,
                onValueChange = { newValue ->
                    onUrlChange(newValue)
                    // Clear error when user types
                    validationError = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("example.com or google.com")
                },
                singleLine = true,
                isError = validationError != null,
                supportingText = validationError?.let { error ->
                    { Text(error, color = AvanueTheme.colors.error) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { validateAndConfirm() }),
                colors = OceanDialogDefaults.outlinedTextFieldColors()
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
                color = AvanueTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "WebView will be integrated here",
                style = MaterialTheme.typography.bodySmall,
                color = AvanueTheme.colors.textSecondary.copy(alpha = 0.6f)
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
                color = AvanueTheme.colors.textSecondary
            )

            Button(onClick = onNewTab) {
                Text("New Tab")
            }
        }
    }
}
