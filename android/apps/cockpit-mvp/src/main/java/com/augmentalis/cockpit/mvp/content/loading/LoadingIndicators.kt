package com.augmentalis.cockpit.mvp.content.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.mvp.OceanTheme

/**
 * LoadingIndicators - Loading state UI components
 *
 * Provides various loading indicators for window content:
 * - Simple spinner
 * - Progress bar with percentage
 * - Full-screen loading overlay
 * - Inline loading state
 */

/**
 * Simple circular loading spinner
 *
 * @param modifier Modifier for positioning
 * @param color Spinner color (default: Ocean Blue primary)
 */
@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color = OceanTheme.primary
) {
    CircularProgressIndicator(
        modifier = modifier.size(48.dp),
        color = color,
        strokeWidth = 4.dp
    )
}

/**
 * Loading progress bar with percentage
 *
 * @param progress Loading progress (0-100)
 * @param modifier Modifier for positioning
 * @param showPercentage Whether to show percentage text
 */
@Composable
fun LoadingProgressBar(
    progress: Int,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = (progress / 100f).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = OceanTheme.primary,
            trackColor = OceanTheme.glassBorder
        )

        if (showPercentage) {
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodySmall,
                color = OceanTheme.textSecondary
            )
        }
    }
}

/**
 * Full-screen loading overlay with message
 *
 * Covers entire window with semi-transparent background and loading indicator.
 *
 * @param message Loading message (e.g., "Loading augmentalis.com...")
 * @param progress Optional progress percentage (0-100, null for indeterminate)
 * @param url Optional URL being loaded
 * @param modifier Modifier for positioning
 */
@Composable
fun LoadingOverlay(
    message: String = "Loading...",
    progress: Int? = null,
    url: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanTheme.backgroundStart.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Spinner or progress indicator
            if (progress != null) {
                CircularProgressIndicator(
                    progress = (progress / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.size(64.dp),
                    color = OceanTheme.primary,
                    strokeWidth = 6.dp
                )
            } else {
                LoadingSpinner(modifier = Modifier.size(64.dp))
            }

            // Loading message
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = OceanTheme.textPrimary,
                textAlign = TextAlign.Center
            )

            // Progress percentage
            if (progress != null) {
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OceanTheme.primary
                )
            }

            // URL being loaded
            url?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanTheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        }
    }
}

/**
 * Inline loading state (for small spaces, e.g., within window title bar)
 *
 * @param message Loading message
 * @param progress Optional progress percentage
 * @param modifier Modifier for positioning
 */
@Composable
fun InlineLoadingIndicator(
    message: String = "Loading...",
    progress: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = OceanTheme.primary,
            strokeWidth = 2.dp
        )

        Text(
            text = if (progress != null) "$message ($progress%)" else message,
            style = MaterialTheme.typography.bodySmall,
            color = OceanTheme.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Pulsing loading indicator (subtle, for background loading)
 *
 * @param modifier Modifier for positioning
 */
@Composable
fun PulsingLoadingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = OceanTheme.primary.copy(alpha = alpha),
                shape = MaterialTheme.shapes.small
            )
    )
}

/**
 * Shimmer loading effect (for skeleton screens)
 *
 * @param modifier Modifier for positioning
 */
@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = OceanTheme.glassSurface.copy(
                    alpha = 0.1f + (shimmerTranslate + 1f) * 0.1f
                ),
                shape = MaterialTheme.shapes.medium
            )
    )
}
