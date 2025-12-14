package com.augmentalis.webavanue.ui.screen.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.webavanue.ui.screen.xr.XRBrowserOverlay
import com.augmentalis.webavanue.feature.xr.XRState

/**
 * Android implementation of XROverlay - shows XR UI components when session is active.
 */
@Composable
actual fun XROverlay(
    xrState: Any?,
    modifier: Modifier
) {
    xrState?.let { state ->
        // Safe cast to XRState (from commonMain, platform-agnostic)
        (state as? XRState)?.let { xrStateTyped ->
            XRBrowserOverlay(
                xrState = xrStateTyped,
                modifier = modifier
            )
        }
    }
}
