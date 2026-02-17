package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueButton
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.ExternalAppStatus
import com.augmentalis.cockpit.model.FrameContent

/**
 * Cross-platform composable rendered inside a FrameWindow for ExternalApp content.
 *
 * Shows the app label, package name, and a status-dependent UI:
 * - [ExternalAppStatus.NOT_INSTALLED]: Error state with install suggestion
 * - [ExternalAppStatus.INSTALLED_NO_EMBED]: Warning with "Open in Split Screen" button
 * - [ExternalAppStatus.EMBEDDABLE]: Success badge with placeholder for future inline embedding
 *
 * All colors use [AvanueTheme.colors] — works across all 32 theme combinations.
 *
 * @param content The ExternalApp content model with package/activity/label info
 * @param status Resolution result from [IExternalAppResolver]
 * @param onLaunchAdjacent Callback to launch the app in split-screen mode
 */
@Composable
fun ExternalAppContent(
    content: FrameContent.ExternalApp,
    status: ExternalAppStatus,
    onLaunchAdjacent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val displayName = content.label.ifBlank { content.packageName.substringAfterLast('.') }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon placeholder (large circle with first letter)
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.firstOrNull()?.uppercase() ?: "?",
                color = colors.onPrimaryContainer,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        // App label
        Text(
            text = displayName,
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        // Package name (smaller, muted)
        if (content.packageName.isNotBlank()) {
            Text(
                text = content.packageName,
                color = colors.textPrimary.copy(alpha = 0.4f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Status badge
        StatusBadge(status = status)

        Spacer(Modifier.height(24.dp))

        // Action area
        when (status) {
            ExternalAppStatus.NOT_INSTALLED -> {
                Text(
                    text = "This app is not installed on your device.\nInstall it from your app store to use it in Cockpit.",
                    color = colors.textPrimary.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            ExternalAppStatus.INSTALLED_NO_EMBED -> {
                Text(
                    text = "This app doesn't support inline embedding.\nIt will open in a split-screen window.",
                    color = colors.textPrimary.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                AvanueButton(onClick = onLaunchAdjacent) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Open in Split Screen")
                }
            }

            ExternalAppStatus.EMBEDDABLE -> {
                Text(
                    text = "This app supports embedded display.\nInline rendering will be available in a future update.",
                    color = colors.textPrimary.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                AvanueButton(onClick = onLaunchAdjacent) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Open Adjacent")
                }
            }
        }
    }
}

/**
 * Compact status badge showing the external app's availability state.
 */
@Composable
private fun StatusBadge(
    status: ExternalAppStatus,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val (badgeColor, icon, label) = when (status) {
        ExternalAppStatus.NOT_INSTALLED -> Triple(
            colors.error, Icons.Default.Error, "Not Installed"
        )
        ExternalAppStatus.INSTALLED_NO_EMBED -> Triple(
            colors.warning, Icons.Default.Warning, "Split Screen Only"
        )
        ExternalAppStatus.EMBEDDABLE -> Triple(
            colors.success, Icons.Default.CheckCircle, "Embeddable"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .background(badgeColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = badgeColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
