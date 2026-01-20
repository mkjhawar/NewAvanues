package com.augmentalis.webavanue

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific XR overlay for BrowserScreen.
 *
 * - Android: Shows XRSessionIndicator and XRPerformanceWarning components
 * - Other platforms: No-op (XR not supported)
 *
 * @param xrState Platform-specific XR state (Any? for cross-platform compatibility)
 * @param modifier Modifier for the overlay
 */
@Composable
expect fun XROverlay(
    xrState: Any?,
    modifier: Modifier = Modifier
)
