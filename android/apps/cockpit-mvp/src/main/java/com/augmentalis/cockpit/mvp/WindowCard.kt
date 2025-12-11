package com.augmentalis.cockpit.mvp

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.mvp.components.GlassmorphicCard
import com.augmentalis.cockpit.mvp.components.WindowControlBar
import com.augmentalis.cockpit.mvp.content.*
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType

@Composable
fun WindowCard(
    window: AppWindow,
    color: String,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onToggleSize: () -> Unit,
    onSelect: () -> Unit,
    onContentStateChange: (WindowContent) -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    isSelected: Boolean = false,
    freeformManager: FreeformWindowManager? = null
) {
    // Get TRUE screen dimensions (not parent container dimensions)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidth = with(density) { configuration.screenWidthDp.dp }
    val screenHeight = with(density) { configuration.screenHeightDp.dp }

    // Dynamic maximize size: fill screen minus 20dp border on all sides
    val maximizedWidth = screenWidth - 40.dp
    val maximizedHeight = screenHeight - 40.dp

    // Animated window size based on isLarge state
    // Normal: 300x400dp, Large: dynamic (screen - 40dp border)
    val animatedWidth by animateDpAsState(
        targetValue = if (window.isLarge) maximizedWidth else OceanTheme.windowWidthDefault,
        animationSpec = tween(durationMillis = 300),
        label = "window_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = if (window.isHidden) 48.dp
            else if (window.isLarge) maximizedHeight
            else OceanTheme.windowHeightDefault,
        animationSpec = tween(durationMillis = 300),
        label = "window_height"
    )

        GlassmorphicCard(
            modifier = modifier
                .width(animatedWidth)
                .height(animatedHeight)
                .clickable { onSelect() }, // Click to select window
            isFocused = isFocused,
            isSelected = isSelected
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Accent color indicator at the top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(android.graphics.Color.parseColor(color)))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Window control bar (title + minimize/maximize/close buttons)
                    WindowControlBar(
                        title = window.title,
                        isHidden = window.isHidden,
                        isLarge = window.isLarge,
                        onMinimize = onMinimize,
                        onToggleSize = onToggleSize,
                        onClose = onClose,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Window content - only visible if not hidden
                    if (!window.isHidden) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            WindowContentRouter(
                                content = window.content,
                                onContentStateChange = onContentStateChange,
                                freeformManager = freeformManager,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
}

/**
 * WindowContentRouter - Routes WindowContent to appropriate renderer
 *
 * Dispatches based on WindowContent type:
 * - WebContent → WebViewContent
 * - DocumentContent → DocumentViewerContent
 * - FreeformAppContent → FreeformWindowContent
 * - MockContent → MockWindowContent (metadata display)
 *
 * @param onContentStateChange Callback when content state changes (for Phase 3 persistence)
 */
@Composable
private fun WindowContentRouter(
    content: WindowContent,
    onContentStateChange: (WindowContent) -> Unit,
    freeformManager: FreeformWindowManager?,
    modifier: Modifier = Modifier
) {
    when (content) {
        is WindowContent.WebContent -> {
            WebViewContent(
                webContent = content,
                onScrollChanged = { scrollX, scrollY ->
                    // Update content with new scroll position
                    onContentStateChange(content.copy(scrollX = scrollX, scrollY = scrollY))
                },
                modifier = modifier
            )
        }
        is WindowContent.DocumentContent -> {
            DocumentViewerContent(
                documentContent = content,
                modifier = modifier
            )
        }
        is WindowContent.FreeformAppContent -> {
            FreeformWindowContent(
                freeformContent = content,
                freeformManager = freeformManager,
                modifier = modifier
            )
        }
        is WindowContent.MockContent -> {
            MockWindowContent(modifier = modifier)
        }
    }
}

/**
 * MockWindowContent - Shows metadata for testing/placeholder
 */
@Composable
private fun MockWindowContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Mock Content",
            style = MaterialTheme.typography.bodyMedium,
            color = OceanTheme.textSecondary
        )
    }
}

private fun getWindowTypeLabel(type: WindowType): String = when (type) {
    WindowType.ANDROID_APP -> "Android App"
    WindowType.WEB_APP -> "Web App"
    WindowType.REMOTE_DESKTOP -> "Remote Desktop"
    WindowType.WIDGET -> "Widget"
}

private fun Float.format() = "%.2f".format(this)
