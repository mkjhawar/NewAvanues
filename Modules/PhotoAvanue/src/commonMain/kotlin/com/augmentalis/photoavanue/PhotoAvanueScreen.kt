package com.augmentalis.photoavanue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.ExposureNeg1
import androidx.compose.material.icons.filled.ExposurePlus1
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.photoavanue.model.CaptureMode
import com.augmentalis.photoavanue.model.ExtensionMode
import com.augmentalis.photoavanue.model.FlashMode
import com.augmentalis.photoavanue.model.RecordingState
import com.augmentalis.photoavanue.model.WhiteBalanceMode

/**
 * Standalone full-screen camera experience.
 *
 * Can be launched independently from the app hub or via voice command.
 * Includes its own TopAppBar, mode selector, capture controls, zoom/exposure,
 * recording indicators, extension mode chips, and pro controls panel.
 *
 * If [controller] implements [IProCameraController], pro features are enabled:
 * - Extension chips (Bokeh, HDR, Night, FaceRetouch)
 * - Pro mode toggle with ISO, Shutter, Focus sliders
 * - White balance presets
 *
 * For embedding inside a Cockpit frame (no chrome), use CameraPreview instead.
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
    val proController = controller as? IProCameraController
    var showProPanel by remember { mutableStateOf(false) }

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
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.semantics { contentDescription = "Voice: click Back" }
                ) {
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
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                // Extension mode indicator
                if (cameraState.extensions.activeMode != ExtensionMode.NONE) {
                    Text(
                        text = cameraState.extensions.activeMode.name,
                        color = colors.warning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                // Pro mode toggle
                if (proController != null) {
                    IconButton(
                        onClick = { showProPanel = !showProPanel },
                        modifier = Modifier.semantics { contentDescription = "Voice: click Pro Controls" }
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            "Pro Controls",
                            tint = if (cameraState.pro.isProMode) colors.warning else colors.textPrimary
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        // ── Extension Mode Chips (if pro controller) ─────────────────
        if (proController != null && cameraState.extensions.hasAnyExtension) {
            ExtensionChipsRow(
                extensions = cameraState.extensions,
                onSelect = { proController.setExtensionMode(it) }
            )
        }

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
                IconButton(
                    onClick = { controller.zoomIn() },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Zoom In" }
                ) {
                    Icon(Icons.Default.ZoomIn, "Zoom In", tint = colors.textPrimary.copy(alpha = 0.7f))
                }
                Text(
                    text = run {
                        val r = cameraState.zoom.currentRatio
                        val rounded = kotlin.math.round(r * 10)
                        "${rounded / 10}.${rounded % 10}x"
                    },
                    color = colors.textPrimary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                IconButton(
                    onClick = { controller.zoomOut() },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Zoom Out" }
                ) {
                    Icon(Icons.Default.ZoomOut, "Zoom Out", tint = colors.textPrimary.copy(alpha = 0.7f))
                }

                Spacer(Modifier.height(16.dp))

                IconButton(
                    onClick = { controller.increaseExposure() },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Increase Exposure" }
                ) {
                    Icon(Icons.Default.ExposurePlus1, "Exposure +", tint = colors.textPrimary.copy(alpha = 0.7f))
                }
                Icon(Icons.Default.Exposure, "Exposure", tint = colors.textPrimary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                IconButton(
                    onClick = { controller.decreaseExposure() },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Decrease Exposure" }
                ) {
                    Icon(Icons.Default.ExposureNeg1, "Exposure -", tint = colors.textPrimary.copy(alpha = 0.7f))
                }
            }

            // Pro control readouts (left side, when pro mode is active)
            if (cameraState.pro.isProMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface.copy(alpha = 0.7f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProReadout("ISO", cameraState.pro.iso.displayText, cameraState.pro.isIsoLocked)
                    ProReadout("SS", cameraState.pro.shutterSpeed.displayText, cameraState.pro.isShutterLocked)
                    ProReadout("F", cameraState.pro.focusDistance.displayText, cameraState.pro.isFocusLocked)
                    ProReadout("WB", cameraState.pro.whiteBalance.name, cameraState.pro.isWhiteBalanceLocked)
                }
            }
        }

        // ── Pro Controls Panel (expandable) ──────────────────────────
        if (proController != null) {
            AnimatedVisibility(
                visible = showProPanel,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ProControlsPanel(
                    controller = proController,
                    pro = cameraState.pro
                )
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
                onClick = { controller.setCaptureMode(CaptureMode.PHOTO) }
            )
            Spacer(Modifier.width(12.dp))
            ModeChip(
                label = "Video",
                selected = cameraState.captureMode == CaptureMode.VIDEO,
                onClick = { controller.setCaptureMode(CaptureMode.VIDEO) }
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
            IconButton(
                onClick = {
                    val nextFlash = when (cameraState.flashMode) {
                        FlashMode.OFF -> FlashMode.ON
                        FlashMode.ON -> FlashMode.AUTO
                        FlashMode.AUTO -> FlashMode.TORCH
                        FlashMode.TORCH -> FlashMode.OFF
                    }
                    controller.setFlashMode(nextFlash)
                },
                modifier = Modifier.semantics { contentDescription = "Voice: click Flash" }
            ) {
                Icon(flashIcon, "Flash", tint = colors.textPrimary)
            }

            Spacer(Modifier.weight(1f))

            // Capture button
            when {
                cameraState.captureMode == CaptureMode.VIDEO && cameraState.recording.isRecording -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val pauseResumeLabel = if (cameraState.recording.isPaused) "Resume Recording" else "Pause Recording"
                        IconButton(
                            onClick = {
                                if (cameraState.recording.isPaused) controller.resumeRecording()
                                else controller.pauseRecording()
                            },
                            modifier = Modifier.semantics { contentDescription = "Voice: click $pauseResumeLabel" }
                        ) {
                            Icon(
                                if (cameraState.recording.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                "Pause/Resume",
                                tint = colors.warning
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { controller.stopRecording() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(colors.error.copy(alpha = 0.3f))
                                .semantics { contentDescription = "Voice: click Stop Recording" }
                        ) {
                            Icon(Icons.Default.Stop, "Stop", tint = colors.error, modifier = Modifier.size(40.dp))
                        }
                    }
                }
                cameraState.captureMode == CaptureMode.VIDEO -> {
                    IconButton(
                        onClick = { controller.startRecording() },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(colors.error.copy(alpha = 0.3f))
                            .semantics { contentDescription = "Voice: click Record" }
                    ) {
                        Icon(Icons.Default.FiberManualRecord, "Record", tint = colors.error, modifier = Modifier.size(40.dp))
                    }
                }
                else -> {
                    IconButton(
                        onClick = { controller.capturePhoto() },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.3f))
                            .semantics { contentDescription = "Voice: click Capture" }
                    ) {
                        Icon(Icons.Default.Camera, "Capture", tint = colors.textPrimary, modifier = Modifier.size(40.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Lens switch
            IconButton(
                onClick = { controller.switchLens() },
                modifier = Modifier.semantics { contentDescription = "Voice: click Switch Lens" }
            ) {
                Icon(Icons.Default.Cameraswitch, "Switch Lens", tint = colors.textPrimary)
            }
        }
    }
}

// ── Extension Mode Chips ──────────────────────────────────────────────

@Composable
private fun ExtensionChipsRow(
    extensions: com.augmentalis.photoavanue.model.CameraExtensions,
    onSelect: (ExtensionMode) -> Unit
) {
    val colors = AvanueTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExtensionChip("Auto", Icons.Default.Autorenew, extensions.activeMode == ExtensionMode.NONE, true) {
            onSelect(ExtensionMode.NONE)
        }
        if (extensions.bokehAvailable) {
            ExtensionChip("Portrait", Icons.Default.Portrait, extensions.activeMode == ExtensionMode.BOKEH, true) {
                onSelect(ExtensionMode.BOKEH)
            }
        }
        if (extensions.hdrAvailable) {
            ExtensionChip("HDR", Icons.Default.HdrOn, extensions.activeMode == ExtensionMode.HDR, true) {
                onSelect(ExtensionMode.HDR)
            }
        }
        if (extensions.nightAvailable) {
            ExtensionChip("Night", Icons.Default.DarkMode, extensions.activeMode == ExtensionMode.NIGHT, true) {
                onSelect(ExtensionMode.NIGHT)
            }
        }
        if (extensions.faceRetouchAvailable) {
            ExtensionChip("Retouch", Icons.Default.Face, extensions.activeMode == ExtensionMode.FACE_RETOUCH, true) {
                onSelect(ExtensionMode.FACE_RETOUCH)
            }
        }
    }
}

@Composable
private fun ExtensionChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    available: Boolean,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.25f) else colors.surface.copy(alpha = 0.4f))
            .clickable(enabled = available, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = "Voice: click $label mode" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = if (selected) colors.primary else colors.textSecondary)
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) colors.primary else colors.textSecondary
            )
        }
    }
}

// ── Pro Controls Panel ────────────────────────────────────────────────

@Composable
private fun ProControlsPanel(
    controller: IProCameraController,
    pro: com.augmentalis.photoavanue.model.ProCameraState
) {
    val colors = AvanueTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Pro mode toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PRO", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (pro.isProMode) colors.warning else colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniChip("ON", pro.isProMode, "Pro Mode On") { controller.setProMode(true) }
                MiniChip("OFF", !pro.isProMode, "Pro Mode Off") { controller.setProMode(false) }
            }
        }

        if (pro.isProMode) {
            // ISO slider
            ProSliderRow(
                label = "ISO",
                value = pro.iso.normalized,
                displayText = pro.iso.displayText,
                locked = pro.isIsoLocked,
                onValueChange = {
                    val isoValue = (pro.iso.minValue + it * (pro.iso.maxValue - pro.iso.minValue)).toInt()
                    controller.setIso(isoValue)
                },
                onLockToggle = { controller.lockIso(!pro.isIsoLocked) }
            )

            // Shutter speed slider
            ProSliderRow(
                label = "SS",
                value = pro.shutterSpeed.normalized,
                displayText = pro.shutterSpeed.displayText,
                locked = pro.isShutterLocked,
                onValueChange = {
                    val nanos = (pro.shutterSpeed.minNanos + it * (pro.shutterSpeed.maxNanos - pro.shutterSpeed.minNanos)).toLong()
                    controller.setShutterSpeed(nanos)
                },
                onLockToggle = { controller.lockShutter(!pro.isShutterLocked) }
            )

            // Focus distance slider
            ProSliderRow(
                label = "Focus",
                value = pro.focusDistance.normalized,
                displayText = pro.focusDistance.displayText,
                locked = pro.isFocusLocked,
                onValueChange = {
                    val diopters = pro.focusDistance.minDiopters + it * (pro.focusDistance.maxDiopters - pro.focusDistance.minDiopters)
                    controller.setFocusDistance(diopters)
                },
                onLockToggle = { controller.lockFocus(!pro.isFocusLocked) }
            )

            // White balance chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WB", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                WhiteBalanceMode.entries.forEach { mode ->
                    MiniChip(
                        label = mode.name.take(4),
                        selected = pro.whiteBalance == mode,
                        voiceLabel = "White Balance ${mode.name}",
                        onClick = { controller.setWhiteBalance(mode) }
                    )
                }
            }

            // RAW toggle
            if (pro.isRawSupported) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RAW", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                    MiniChip(
                        label = "DNG",
                        selected = pro.isRawEnabled,
                        voiceLabel = "RAW DNG capture",
                        onClick = { controller.setRawCapture(!pro.isRawEnabled) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProSliderRow(
    label: String,
    value: Float,
    displayText: String,
    locked: Boolean,
    onValueChange: (Float) -> Unit,
    onLockToggle: () -> Unit
) {
    val colors = AvanueTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.width(38.dp))
        Slider(
            value = value,
            onValueChange = { if (!locked) onValueChange(it) },
            enabled = !locked,
            modifier = Modifier.weight(1f).height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.textDisabled
            )
        )
        Text(displayText, fontSize = 10.sp, color = colors.textPrimary, modifier = Modifier.width(48.dp))
        IconButton(
            onClick = onLockToggle,
            modifier = Modifier
                .size(24.dp)
                .semantics {
                    contentDescription = if (locked) "Voice: click Unlock $label" else "Voice: click Lock $label"
                }
        ) {
            Icon(
                if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                "Lock $label",
                tint = if (locked) colors.warning else colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ProReadout(label: String, value: String, locked: Boolean) {
    val colors = AvanueTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 9.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(4.dp))
        Text(value, fontSize = 10.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
        if (locked) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(8.dp).padding(start = 2.dp), tint = colors.warning)
        }
    }
}

@Composable
private fun MiniChip(
    label: String,
    selected: Boolean,
    voiceLabel: String,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .semantics { contentDescription = "Voice: click $voiceLabel" }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.primary else colors.textSecondary
        )
    }
}

// ── Helper Composables ────────────────────────────────────────────────

@Composable
private fun RecordingOverlay(
    recording: RecordingState,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val seconds = recording.durationMs / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    val timeText = "${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"

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
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics { contentDescription = "Voice: click $label mode" }
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
