package com.augmentalis.webavanue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.webavanue.XRBrowserOverlay
import com.augmentalis.webavanue.XRState

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
