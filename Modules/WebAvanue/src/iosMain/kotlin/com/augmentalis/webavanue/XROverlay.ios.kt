package com.augmentalis.webavanue

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS XROverlay stub implementation
 *
 * iOS doesn't have an XR overlay system like Android smart glasses
 * This is a no-op implementation
 */
@Composable
actual fun XROverlay(
    isXRMode: Boolean,
    onOpenInBrowser: (String) -> Unit,
    onDismissOverlay: () -> Unit,
    modifier: Modifier
) {
    // No-op for iOS - no XR overlay system
}
