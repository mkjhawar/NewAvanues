package com.augmentalis.cockpit.mvp.curved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.viewpager2.widget.ViewPager2
import com.avanues.cockpit.core.window.AppWindow

/**
 * CurvedWorkspaceView - Curved ViewPager2-based workspace with real content
 *
 * Renders actual WebView content with curved preview effect.
 * Supports dual themes: Ocean Blue (glassmorphic) and MagicUI (vibrant).
 *
 * Features:
 * - Center window at 70% width
 * - Curved side previews with bitmap transformation
 * - Real WebView content rendering
 * - Swipeable navigation between windows
 * - Theme toggle (Ocean Blue ↔ MagicUI/Avanues)
 */
@Composable
fun CurvedWorkspaceView(
    windows: List<AppWindow>,
    selectedWindowId: String?,
    onWindowSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialTheme: CurvedWorkspaceTheme = CurvedWorkspaceTheme.OceanBlue
) {
    val context = LocalContext.current

    // Theme state (can be toggled)
    var currentTheme by remember { mutableStateOf(initialTheme) }

    // ViewPager2 state
    var viewPager by remember { mutableStateOf<ViewPager2?>(null) }

    // Adapter state (mutable to allow theme changes)
    var adapter by remember { mutableStateOf(WindowViewPagerAdapter(currentTheme)) }

    // Update adapter when windows change
    LaunchedEffect(windows) {
        adapter.submitWindows(windows)
    }

    // Set current page based on selected window
    LaunchedEffect(selectedWindowId, windows) {
        val selectedIndex = windows.indexOfFirst { it.id == selectedWindowId }
        if (selectedIndex >= 0) {
            viewPager?.setCurrentItem(selectedIndex, true)
        }
    }

    // NOTE: Don't use clipToBounds() to allow side windows to draw outside parent bounds
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(currentTheme.backgroundColor))
    ) {
        // ViewPager2 with curved windows
        AndroidView(
            factory = { ctx ->
                // Wrap ViewPager2 in FrameLayout with clipChildren=false
                // This allows side windows to draw outside ViewPager2 bounds
                android.widget.FrameLayout(ctx).apply {
                    clipChildren = false  // CRITICAL: Allow side windows to overflow
                    clipToPadding = false

                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Add ViewPager2 as child
                    addView(ViewPager2(ctx).apply {
                        this.adapter = adapter

                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Allow side windows to draw outside bounds (curved arc effect)
                        clipChildren = false
                        clipToPadding = false

                        // Apply curved preview effect
                        setPreviewBothSide()

                        // Handle page selection
                        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                if (position in windows.indices) {
                                    onWindowSelect(windows[position].id)
                                }
                                // Update adapter's current position for visibility toggling
                                adapter.setCurrentPosition(position)
                            }
                        })

                        viewPager = this
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Theme toggle button (top-right)
        IconButton(
            onClick = {
                currentTheme = when (currentTheme) {
                    is CurvedWorkspaceTheme.OceanBlue -> CurvedWorkspaceTheme.MagicUI
                    is CurvedWorkspaceTheme.MagicUI -> CurvedWorkspaceTheme.OceanBlue
                }

                // Recreate adapter with new theme and reapply transformations
                adapter = WindowViewPagerAdapter(currentTheme).apply {
                    submitWindows(windows)
                }
                viewPager?.apply {
                    this.adapter = adapter
                    setPreviewBothSide()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Toggle Theme",
                tint = Color(currentTheme.textColor)
            )
        }

        // Theme indicator (bottom-center)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(
                    color = Color(currentTheme.windowBorderColor).copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            androidx.compose.material3.Text(
                text = when (currentTheme) {
                    is CurvedWorkspaceTheme.OceanBlue -> "Ocean Blue Theme"
                    is CurvedWorkspaceTheme.MagicUI -> "MagicUI Theme"
                },
                color = Color(currentTheme.textColor),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Preview composable for development
 */
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun CurvedWorkspaceViewPreview() {
    val sampleWindows = listOf(
        AppWindow(
            id = "1",
            title = "Google",
            type = com.avanues.cockpit.core.window.WindowType.WEB_APP,
            sourceId = "google",
            position = com.avanues.cockpit.core.workspace.Vector3D(0f, 0f, -2f),
            widthMeters = 1.0f,
            heightMeters = 0.8f,
            content = com.avanues.cockpit.core.window.WindowContent.WebContent("https://google.com")
        ),
        AppWindow(
            id = "2",
            title = "Calculator",
            type = com.avanues.cockpit.core.window.WindowType.WIDGET,
            sourceId = "calc",
            position = com.avanues.cockpit.core.workspace.Vector3D(0f, 0f, -2f),
            widthMeters = 1.0f,
            heightMeters = 0.8f,
            content = com.avanues.cockpit.core.window.WindowContent.MockContent
        ),
        AppWindow(
            id = "3",
            title = "Weather",
            type = com.avanues.cockpit.core.window.WindowType.WEB_APP,
            sourceId = "weather",
            position = com.avanues.cockpit.core.workspace.Vector3D(0f, 0f, -2f),
            widthMeters = 1.0f,
            heightMeters = 0.8f,
            content = com.avanues.cockpit.core.window.WindowContent.WebContent("https://weather.com")
        )
    )

    CurvedWorkspaceView(
        windows = sampleWindows,
        selectedWindowId = "1",
        onWindowSelect = {}
    )
}
