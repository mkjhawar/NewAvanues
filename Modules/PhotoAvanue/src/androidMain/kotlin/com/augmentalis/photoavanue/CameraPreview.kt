package com.augmentalis.photoavanue

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.photoavanue.model.CaptureMode
import com.augmentalis.photoavanue.model.FlashMode

/**
 * Embeddable camera preview composable — designed for Cockpit frame embedding.
 *
 * Includes camera preview, bottom control bar (capture, flash, lens switch),
 * and recording controls. No TopAppBar or navigation chrome.
 *
 * For the full standalone experience with TopAppBar and zoom/exposure controls,
 * use [PhotoAvanueScreen] instead.
 *
 * @param onPhotoCaptured Callback with saved image URI string.
 * @param onVideoFinalized Callback with saved video URI string.
 * @param onError Callback with error message.
 */
@Composable
fun CameraPreview(
    onPhotoCaptured: (String) -> Unit = {},
    onVideoFinalized: (String) -> Unit = {},
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = AvanueTheme.colors

    // Create and remember controller
    val controller = remember { AndroidCameraController(context) }
    val cameraState by controller.state.collectAsState()

    // Forward state changes to callbacks
    cameraState.lastCapturedUri?.let { uri ->
        if (cameraState.recording.outputUri == uri) {
            onVideoFinalized(uri)
        } else if (!cameraState.isCapturing) {
            onPhotoCaptured(uri)
        }
    }
    cameraState.error?.let { onError(it) }

    // Release controller on dispose
    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    // Permission handling
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.CAMERA] == true
        if (!hasPermission) onError("Camera permission denied")
    }

    if (!hasPermission) {
        PermissionRequestUI(
            colors = colors,
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            },
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview surface
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { preview ->
                    controller.bindCamera(lifecycleOwner, preview)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Recording indicator
        if (cameraState.recording.isRecording) {
            val durationMs = cameraState.recording.durationMs
            val seconds = durationMs / 1000
            val minutes = seconds / 60
            val secs = seconds % 60
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.error.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (cameraState.recording.isPaused) Icons.Default.Pause else Icons.Default.FiberManualRecord,
                    null, tint = Color.White, modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = String.format("%02d:%02d", minutes, secs),
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom controls
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(colors.surface.copy(alpha = 0.6f)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
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
                val next = when (cameraState.flashMode) {
                    FlashMode.OFF -> FlashMode.ON
                    FlashMode.ON -> FlashMode.AUTO
                    FlashMode.AUTO -> FlashMode.TORCH
                    FlashMode.TORCH -> FlashMode.OFF
                }
                controller.setFlashMode(next)
            }) {
                Icon(flashIcon, "Flash", tint = colors.textPrimary)
            }

            // Capture / Record button
            when {
                cameraState.captureMode == CaptureMode.VIDEO && cameraState.recording.isRecording -> {
                    // Pause/Resume
                    IconButton(onClick = {
                        if (cameraState.recording.isPaused) controller.resumeRecording()
                        else controller.pauseRecording()
                    }) {
                        Icon(
                            if (cameraState.recording.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            "Pause", tint = colors.warning
                        )
                    }
                    // Stop
                    IconButton(
                        onClick = { controller.stopRecording() },
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.error.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Stop, "Stop", tint = colors.error, modifier = Modifier.size(48.dp))
                    }
                }
                cameraState.captureMode == CaptureMode.VIDEO -> {
                    IconButton(
                        onClick = { controller.startRecording() },
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.error.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.FiberManualRecord, "Record", tint = colors.error, modifier = Modifier.size(48.dp))
                    }
                }
                else -> {
                    IconButton(
                        onClick = { controller.capturePhoto() },
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Camera, "Capture", tint = colors.textPrimary, modifier = Modifier.size(48.dp))
                    }
                }
            }

            // Lens switch
            IconButton(onClick = { controller.switchLens() }) {
                Icon(Icons.Default.Cameraswitch, "Switch", tint = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun PermissionRequestUI(
    colors: com.augmentalis.avanueui.theme.AvanueColorScheme,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Camera",
                tint = colors.textPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Camera Permission Required",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Grant camera and microphone access to use this feature",
                color = colors.textPrimary.copy(alpha = 0.5f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.15f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onRequestPermission() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Grant Permissions",
                    color = colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
