package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.content.ContentRenderer
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.LayoutMode
import com.augmentalis.cockpit.viewmodel.CockpitViewModel

/**
 * Main Cockpit screen that displays the active session with its frames
 * in the selected layout mode.
 *
 * Top bar: session name, layout switcher, add frame button.
 * Body: LayoutEngine rendering frames.
 * Background: SpatialVoice gradient using AvanueTheme colors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CockpitScreen(
    viewModel: CockpitViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.activeSession.collectAsState()
    val frames by viewModel.frames.collectAsState()
    val selectedFrameId by viewModel.selectedFrameId.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val colors = AvanueTheme.colors

    var showLayoutMenu by remember { mutableStateOf(false) }
    var showAddFrameMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.background,
                        colors.surface.copy(alpha = 0.6f),
                        colors.background
                    )
                )
            )
    ) {
        // Top app bar
        TopAppBar(
            title = {
                Text(
                    text = session?.name ?: "Cockpit",
                    color = colors.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            actions = {
                // Layout mode switcher
                Box {
                    IconButton(onClick = { showLayoutMenu = true }) {
                        Icon(
                            imageVector = layoutModeIcon(layoutMode),
                            contentDescription = "Layout: ${layoutMode.name}",
                            tint = colors.onBackground
                        )
                    }
                    DropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false }
                    ) {
                        LayoutMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            layoutModeIcon(mode),
                                            mode.name,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(layoutModeLabel(mode))
                                    }
                                },
                                onClick = {
                                    viewModel.setLayoutMode(mode)
                                    showLayoutMenu = false
                                }
                            )
                        }
                    }
                }

                // Add frame button
                Box {
                    IconButton(onClick = { showAddFrameMenu = true }) {
                        Icon(
                            Icons.Default.Add,
                            "Add frame",
                            tint = colors.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showAddFrameMenu,
                        onDismissRequest = { showAddFrameMenu = false }
                    ) {
                        addFrameOptions().forEach { (label, content) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.addFrame(content, label)
                                    showAddFrameMenu = false
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Main content area
        if (frames.isEmpty()) {
            EmptySessionView(
                onAddFrame = { showAddFrameMenu = true },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            LayoutEngine(
                layoutMode = layoutMode,
                frames = frames,
                selectedFrameId = selectedFrameId,
                onFrameSelected = { viewModel.selectFrame(it) },
                onFrameMoved = { id, x, y -> viewModel.moveFrame(id, x, y) },
                onFrameResized = { id, w, h -> viewModel.resizeFrame(id, w, h) },
                onFrameClose = { viewModel.removeFrame(it) },
                onFrameMinimize = { viewModel.toggleMinimize(it) },
                onFrameMaximize = { viewModel.toggleMaximize(it) },
                frameContent = { frame ->
                    ContentRenderer(
                        frame = frame,
                        onContentStateChanged = { frameId, jsonState ->
                            viewModel.updateContentState(frameId, jsonState)
                        }
                    )
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }

        // Frame count indicator
        if (frames.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(colors.background)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${frames.size} frame${if (frames.size != 1) "s" else ""} • ${layoutMode.name.lowercase().replace('_', ' ')}",
                    color = colors.onSurface.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
                Text(
                    text = session?.name ?: "",
                    color = colors.onSurface.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Empty state shown when no frames exist in the session.
 */
@Composable
private fun EmptySessionView(
    onAddFrame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Dashboard,
                "Empty cockpit",
                tint = colors.outline,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No frames yet",
                color = colors.onBackground.copy(alpha = 0.6f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Tap + to add a web browser, PDF, camera, or note",
                color = colors.onBackground.copy(alpha = 0.3f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.15f))
                    .clickable { onAddFrame() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, "Add", tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Frame", color = colors.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun layoutModeIcon(mode: LayoutMode) = when (mode) {
    LayoutMode.FREEFORM -> Icons.Default.Dashboard
    LayoutMode.GRID -> Icons.Default.GridView
    LayoutMode.SPLIT_LEFT, LayoutMode.SPLIT_RIGHT -> Icons.Default.Splitscreen
    LayoutMode.COCKPIT -> Icons.Default.ViewCarousel
    LayoutMode.FULLSCREEN -> Icons.Default.Dashboard
    LayoutMode.WORKFLOW -> Icons.Default.ViewColumn
    LayoutMode.ROW -> Icons.Default.ViewColumn
}

private fun layoutModeLabel(mode: LayoutMode) = when (mode) {
    LayoutMode.FREEFORM -> "Freeform"
    LayoutMode.GRID -> "Grid"
    LayoutMode.SPLIT_LEFT -> "Split (Primary Left)"
    LayoutMode.SPLIT_RIGHT -> "Split (Primary Right)"
    LayoutMode.COCKPIT -> "Cockpit (Swipe)"
    LayoutMode.FULLSCREEN -> "Fullscreen"
    LayoutMode.WORKFLOW -> "Workflow"
    LayoutMode.ROW -> "Row"
}

private fun addFrameOptions(): List<Pair<String, FrameContent>> = listOf(
    "Web Browser" to FrameContent.Web(),
    "PDF Viewer" to FrameContent.Pdf(),
    "Image Viewer" to FrameContent.Image(),
    "Video Player" to FrameContent.Video(),
    "Note" to FrameContent.Note(),
    "Camera" to FrameContent.Camera(),
    "Voice Note" to FrameContent.VoiceNote(),
    "Whiteboard" to FrameContent.Whiteboard(),
    "Signature" to FrameContent.Signature(),
    "Screen Cast" to FrameContent.ScreenCast()
)
