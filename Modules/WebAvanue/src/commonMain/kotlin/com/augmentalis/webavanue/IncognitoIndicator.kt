package com.augmentalis.webavanue

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * IncognitoIndicator - Visual indicator for private browsing mode
 *
 * Shows a dark pill with mask icon when incognito/private mode is active.
 * Includes subtle pulsing animation for attention.
 */
@Composable
fun IncognitoIndicator(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    // Subtle pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "incognito_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "incognito_alpha"
    )

    Surface(
        modifier = modifier.alpha(alpha),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1C1C1E),  // Dark gray, similar to Chrome incognito
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = "Incognito mode",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )

            if (showLabel) {
                Text(
                    text = "Incognito",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Full-screen incognito overlay for new tab screen
 */
@Composable
fun IncognitoNewTabOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "You've gone incognito",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IncognitoFeatureRow(text = "Browsing history won't be saved")
                IncognitoFeatureRow(text = "Cookies deleted when you close tabs")
                IncognitoFeatureRow(text = "Form data not remembered")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your activity might still be visible to:\n" +
                    "- Websites you visit\n" +
                    "- Your employer or school\n" +
                    "- Your internet service provider",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun IncognitoFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
