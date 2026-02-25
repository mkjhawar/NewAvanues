package com.augmentalis.remotecast

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.augmentalis.remotecast.model.CastState
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.handlers.ModuleCommandCallbacks

/**
 * Overlay UI for screen casting — shows connection status, device info,
 * and streaming controls. When connected, the actual screen mirror would
 * be rendered via a SurfaceView/TextureView underneath this overlay.
 *
 * @param castState Current cast session state.
 * @param onConnect Called to initiate device connection.
 * @param onDisconnect Called to stop casting.
 * @param modifier Layout modifier.
 */
@Composable
fun CastOverlay(
    castState: CastState,
    onConnect: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    // Wire voice command executor for cast controls
    DisposableEffect(Unit) {
        ModuleCommandCallbacks.castExecutor = { actionType, _ ->
            executeCastCommand(actionType, castState, onConnect, onDisconnect)
        }
        onDispose { ModuleCommandCallbacks.castExecutor = null }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        if (castState.isStreaming) {
            // Streaming indicator overlay (top-right)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
                Text(
                    text = castState.deviceName.ifBlank { "Streaming" },
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${castState.latencyMs}ms",
                    color = colors.textPrimary.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier.size(24.dp).semantics { contentDescription = "Voice: click stop casting" }
                ) {
                    Icon(
                        Icons.Default.Stop,
                        "Stop casting",
                        tint = colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            // Not connected — show connection prompt
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (castState.isConnected) Icons.Default.CastConnected else Icons.Default.Cast,
                    contentDescription = "Cast",
                    tint = colors.textPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (castState.isConnected) "Connected to ${castState.deviceName}" else "No device connected",
                    color = colors.textPrimary.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (castState.error != null) {
                    Text(
                        text = castState.error,
                        color = colors.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                IconButton(
                    onClick = onConnect,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.15f))
                        .semantics { contentDescription = "Voice: click connect cast" }
                ) {
                    Icon(
                        Icons.Default.Cast,
                        "Connect",
                        tint = colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Status bar at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(colors.surface.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (castState.isStreaming) "Casting: ${castState.resolution.width}x${castState.resolution.height}"
                else "Ready to cast",
                color = colors.textPrimary.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
            Text(
                text = "${castState.frameRate} fps",
                color = colors.textPrimary.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Maps cast voice commands to CastOverlay callbacks.
 * Start/stop casting, connect/disconnect device.
 */
private fun executeCastCommand(
    actionType: CommandActionType,
    castState: CastState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
): HandlerResult {
    return when (actionType) {
        CommandActionType.CAST_START, CommandActionType.CAST_CONNECT -> {
            if (castState.isStreaming) {
                HandlerResult.failure("Already casting", recoverable = true)
            } else {
                onConnect()
                HandlerResult.success("Connecting to cast device")
            }
        }

        CommandActionType.CAST_STOP, CommandActionType.CAST_DISCONNECT -> {
            if (!castState.isStreaming && !castState.isConnected) {
                HandlerResult.failure("Not currently casting", recoverable = true)
            } else {
                onDisconnect()
                HandlerResult.success("Cast stopped")
            }
        }

        CommandActionType.CAST_QUALITY -> {
            // Quality change requires UI interaction — guide user
            HandlerResult.failure(
                "Quality settings require manual selection",
                recoverable = true,
                suggestedAction = "Open cast settings to change quality"
            )
        }

        else -> HandlerResult.failure("Unsupported cast action: $actionType", recoverable = true)
    }
}
