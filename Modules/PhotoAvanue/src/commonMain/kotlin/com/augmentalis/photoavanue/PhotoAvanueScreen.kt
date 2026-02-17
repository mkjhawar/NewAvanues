package com.augmentalis.photoavanue

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.ExposureNeg1
import androidx.compose.material.icons.filled.ExposurePlus1
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.photoavanue.model.CaptureMode
import com.augmentalis.photoavanue.model.FlashMode
import com.augmentalis.photoavanue.model.RecordingState

/**
 * Standalone full-screen camera experience.
 *
 * Can be launched independently from the app hub or via voice command.
 * Includes its own TopAppBar, mode selector, capture controls, zoom/exposure,
 * and recording indicators. All state is driven by [controller].
 *
 * For embedding inside a Cockpit frame (no chrome), use [CameraPreview] instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAvanueScreen(
    controller: ICameraController,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cameraState by controller.state.collectAsState()
    val colors = AvanueTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.surface.copy(alpha = 0.6f), colors.background)
                )
            )
    ) {
        // ── Top App Bar ──────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    text = "PhotoAvanue",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                }
            },
            actions = {
                // GPS indicator
                if (cameraState.hasGpsLocation) {
                    Text(
                        text = "GPS",
                        color = colors.success,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // ── Camera Preview Area ──────────────────────────────────────
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Recording overlay
            if (cameraState.recording.isRecording) {
                RecordingOverlay(
                    recording = cameraState.recording,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                )
            }

            // Error overlay
            cameraState.error?.let { error ->
                Text(
                    text = error,
                    color = colors.error,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Zoom/Exposure indicators
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom level
                IconButton(onClick = { controller.zoomIn() }) {
                    Icon(Icons.Default.ZoomIn, "Zoom In", tint = colors.textPrimary.copy(alpha = 0.7f))
                }
                Text(
                    text = "${String.format("%.1f", cameraState.zoom.currentRatio)}x",
                    color = colors.textPrimary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                IconButton(onClick = { controller.zoomOut() }) {
                    Icon(Icons.Default.ZoomOut, "Zoom Out", tint = colors.textPrimary.copy(alpha = 0.7f))
                }

                Spacer(Modifier.height(16.dp))

                // Exposure level
                IconButton(onClick = { controller.increaseExposure() }) {
                    Icon(Icons.Default.ExposurePlus1, "Exposure +", tint = colors.textPrimary.copy(alpha = 0.7f))
                }
                Icon(Icons.Default.Exposure, "Exposure", tint = colors.textPrimary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                IconButton(onClick = { controller.decreaseExposure() }) {
                    Icon(Icons.Default.ExposureNeg1, "Exposure -", tint = colors.textPrimary.copy(alpha = 0.7f))
                }
            }
        }

        // ── Mode Selector ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            ModeChip(
                label = "Photo",
                selected = cameraState.captureMode == CaptureMode.PHOTO,
                onClick = { /* Mode switch handled by controller or parent */ }
            )
            Spacer(Modifier.width(12.dp))
            ModeChip(
                label = "Video",
                selected = cameraState.captureMode == CaptureMode.VIDEO,
                onClick = { /* Mode switch handled by controller or parent */ }
            )
            Spacer(Modifier.weight(1f))
        }

        // ── Bottom Controls ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash toggle
            val flashIcon = when (cameraState.flashMode) {
                FlashMode.OFF -> Icons.Default.FlashOff
                FlashMode.ON -> Icons.Default.FlashOn
                FlashMode.AUTO -> Icons.Default.FlashAuto
                FlashMode.TORCH -> Icons.Default.FlashlightOn
            }
            IconButton(onClick = {
                val nextFlash = when (cameraState.flashMode) {
                    FlashMode.OFF -> FlashMode.ON
                    FlashMode.ON -> FlashMode.AUTO
                    FlashMode.AUTO -> FlashMode.TORCH
                    FlashMode.TORCH -> FlashMode.OFF
                }
                controller.setFlashMode(nextFlash)
            }) {
                Icon(flashIcon, "Flash", tint = colors.textPrimary)
            }

            Spacer(Modifier.weight(1f))

            // Capture button
            when {
                cameraState.captureMode == CaptureMode.VIDEO && cameraState.recording.isRecording -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pause/Resume
                        IconButton(onClick = {
                            if (cameraState.recording.isPaused) controller.resumeRecording()
                            else controller.pauseRecording()
                        }) {
                            Icon(
                                if (cameraState.recording.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                "Pause/Resume",
                                tint = colors.warning
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // Stop recording
                        IconButton(
                            onClick = { controller.stopRecording() },
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.error.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Stop, "Stop", tint = colors.error, modifier = Modifier.size(40.dp))
                        }
                    }
                }
                cameraState.captureMode == CaptureMode.VIDEO -> {
                    IconButton(
                        onClick = { controller.startRecording() },
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.error.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.FiberManualRecord, "Record", tint = colors.error, modifier = Modifier.size(40.dp))
                    }
                }
                else -> {
                    IconButton(
                        onClick = { controller.capturePhoto() },
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Camera, "Capture", tint = colors.textPrimary, modifier = Modifier.size(40.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Lens switch
            IconButton(onClick = { controller.switchLens() }) {
                Icon(Icons.Default.Cameraswitch, "Switch Lens", tint = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun RecordingOverlay(
    recording: RecordingState,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val seconds = recording.durationMs / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    val timeText = String.format("%02d:%02d", minutes, secs)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.error.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (recording.isPaused) Icons.Default.Pause else Icons.Default.FiberManualRecord,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (recording.isPaused) "PAUSED $timeText" else "REC $timeText",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.2f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (label == "Photo") Icons.Default.PhotoCamera else Icons.Default.Videocam,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.textPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = if (selected) colors.primary else colors.textPrimary.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
