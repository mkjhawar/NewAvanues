package com.augmentalis.webavanue

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.OceanDesignTokens
import kotlinx.coroutines.delay

/**
 * DOM scraping state for UI display
 */
sealed class DOMScrapingState {
    object Idle : DOMScrapingState()
    data class Scanning(val progress: Float = 0f) : DOMScrapingState()
    data class Complete(
        val elementCount: Int,
        val commandCount: Int,
        val isWhitelisted: Boolean
    ) : DOMScrapingState()
    data class Error(val message: String) : DOMScrapingState()
}

/**
 * Full-width scanning indicator bar
 *
 * Shows at the top of the WebView during DOM scraping.
 * Minimal, non-intrusive design.
 */
@Composable
fun DOMScrapingBar(
    state: DOMScrapingState,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state is DOMScrapingState.Scanning,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = OceanDesignTokens.Surface.elevated,
            tonalElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated scanning icon
                    val infiniteTransition = rememberInfiniteTransition(label = "scan")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing)
                        ),
                        label = "scanRotation"
                    )

                    Icon(
                        Icons.Default.Radar,
                        contentDescription = "Scanning",
                        tint = OceanDesignTokens.Icon.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Scanning page for voice commands...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OceanDesignTokens.Text.primary
                    )
                }

                // Progress bar
                if (state is DOMScrapingState.Scanning && state.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = OceanDesignTokens.Icon.primary,
                        trackColor = OceanDesignTokens.Surface.default
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = OceanDesignTokens.Icon.primary,
                        trackColor = OceanDesignTokens.Surface.default
                    )
                }
            }
        }
    }
}

/**
 * Compact scanning indicator chip
 *
 * Small floating indicator in the corner during scanning.
 */
@Composable
fun DOMScrapingChip(
    state: DOMScrapingState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chipAnim")

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
        },
        label = "chipContent",
        modifier = modifier
    ) { currentState ->
        when (currentState) {
            is DOMScrapingState.Idle -> {
                // Hidden when idle
            }

            is DOMScrapingState.Scanning -> {
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(16.dp),
                    color = OceanDesignTokens.Icon.primary.copy(alpha = 0.15f),
                    modifier = Modifier.scale(pulse)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = OceanDesignTokens.Icon.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scanning...",
                            style = MaterialTheme.typography.labelMedium,
                            color = OceanDesignTokens.Icon.primary
                        )
                    }
                }
            }

            is DOMScrapingState.Complete -> {
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(16.dp),
                    color = OceanDesignTokens.Icon.success.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OceanDesignTokens.Icon.success,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${currentState.commandCount} commands",
                            style = MaterialTheme.typography.labelMedium,
                            color = OceanDesignTokens.Icon.success
                        )
                        if (currentState.isWhitelisted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Saved",
                                tint = OceanDesignTokens.Icon.warning,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            is DOMScrapingState.Error -> {
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(16.dp),
                    color = OceanDesignTokens.Icon.error.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = OceanDesignTokens.Icon.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan failed",
                            style = MaterialTheme.typography.labelMedium,
                            color = OceanDesignTokens.Icon.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Scanning complete toast notification
 *
 * Brief notification that appears when scanning completes.
 */
@Composable
fun ScanCompleteToast(
    state: DOMScrapingState.Complete?,
    onDismiss: () -> Unit,
    autoDismissMs: Long = 2000L,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state != null) {
            visible = true
            delay(autoDismissMs)
            visible = false
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible && state != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        state?.let { s ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OceanDesignTokens.Surface.elevated,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = OceanDesignTokens.Icon.success,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${s.commandCount} voice commands ready",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = OceanDesignTokens.Text.primary
                        )
                        Text(
                            text = "${s.elementCount} elements scanned" +
                                if (s.isWhitelisted) " (saved)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = OceanDesignTokens.Text.secondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overlay scanning animation
 *
 * Full-screen scanning effect that shows briefly when page loads.
 * Creates a "radar sweep" effect to indicate scanning.
 */
@Composable
fun ScanningOverlay(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "overlay")

    AnimatedVisibility(
        visible = isScanning,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing circle animation
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearOutSlowInEasing)
                ),
                label = "pulseScale"
            )

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearOutSlowInEasing)
                ),
                label = "pulseAlpha"
            )

            // Multiple expanding circles
            listOf(0f, 0.33f, 0.66f).forEach { delay ->
                val adjustedScale by infiniteTransition.animateFloat(
                    initialValue = 0.5f + delay,
                    targetValue = 2f + delay,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearOutSlowInEasing)
                    ),
                    label = "scale$delay"
                )

                val adjustedAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearOutSlowInEasing)
                    ),
                    label = "alpha$delay"
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(adjustedScale)
                        .clip(CircleShape)
                        .background(
                            OceanDesignTokens.Icon.primary.copy(alpha = adjustedAlpha)
                        )
                )
            }

            // Center scanning icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(OceanDesignTokens.Surface.elevated),
                contentAlignment = Alignment.Center
            ) {
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing)
                    ),
                    label = "iconRotation"
                )

                Icon(
                    Icons.Default.Radar,
                    contentDescription = "Scanning",
                    tint = OceanDesignTokens.Icon.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

/**
 * Minimal scanning dot indicator
 *
 * Tiny pulsing dot that shows in the address bar during scanning.
 */
@Composable
fun ScanningDot(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")

    AnimatedVisibility(
        visible = isScanning,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotScale"
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(OceanDesignTokens.Icon.primary)
        )
    }
}
