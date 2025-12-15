package com.augmentalis.cockpit.mvp

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.mvp.components.GlassmorphicCard
import com.augmentalis.cockpit.mvp.components.WindowControlBar
import com.augmentalis.cockpit.mvp.content.*
import com.augmentalis.cockpit.mvp.content.widgets.WidgetRenderer
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType

/**
 * Safely parse color string with fallback
 * @param colorString Color string (e.g., "#FF0000")
 * @param fallback Fallback color if parsing fails (default: Ocean primary)
 * @return Parsed Color or fallback
 */
private fun safeParseColor(colorString: String, fallback: Color = Color(0xFF4A90B8)): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: IllegalArgumentException) {
        android.util.Log.w("WindowCard", "Invalid color: $colorString, using fallback")
        fallback
    }
}

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

    // Animated window size based on isLarge state (Natural spring physics)
    // Normal: 300x400dp, Large: dynamic (screen - 40dp border)
    val animatedWidth by animateDpAsState(
        targetValue = if (window.isLarge) maximizedWidth else OceanTheme.windowWidthDefault,
        animationSpec = spring(
            dampingRatio = OceanTheme.springDampingRatio,
            stiffness = OceanTheme.springStiffnessMedium,
            visibilityThreshold = 1.dp
        ),
        label = "window_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = if (window.isHidden) 48.dp
            else if (window.isLarge) maximizedHeight
            else OceanTheme.windowHeightDefault,
        animationSpec = spring(
            dampingRatio = OceanTheme.springDampingRatio,
            stiffness = OceanTheme.springStiffnessMedium,
            visibilityThreshold = 1.dp
        ),
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
                        .background(safeParseColor(color))
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
                                window = window,
                                content = window.content,
                                onContentStateChange = onContentStateChange,
                                onMinimize = onMinimize,
                                onToggleSize = onToggleSize,
                                onClose = onClose,
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
 * - WidgetContent → WidgetRenderer (Calculator, Weather, etc.)
 * - FreeformAppContent → FreeformWindowContent
 * - MockContent → MockWindowContent (metadata display)
 *
 * @param onContentStateChange Callback when content state changes (for Phase 3 persistence)
 */
@Composable
private fun WindowContentRouter(
    window: AppWindow,
    content: WindowContent,
    onContentStateChange: (WindowContent) -> Unit,
    onMinimize: () -> Unit,
    onToggleSize: () -> Unit,
    onClose: () -> Unit,
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
                windowId = window.id,
                enableBridge = true,
                onMinimize = onMinimize,
                onMaximize = onToggleSize,
                onClose = onClose,
                modifier = modifier
            )
        }
        is WindowContent.DocumentContent -> {
            DocumentViewerContent(
                documentContent = content,
                modifier = modifier
            )
        }
        is WindowContent.WidgetContent -> {
            WidgetRenderer(
                widgetContent = content,
                onStateChanged = { newState ->
                    onContentStateChange(content.copy(state = newState))
                },
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
