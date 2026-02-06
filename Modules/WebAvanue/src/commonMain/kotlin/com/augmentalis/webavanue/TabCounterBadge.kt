package com.augmentalis.webavanue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.ava.core.theme.OceanTheme

/**
 * TabCounterBadge - Chrome-like tab counter badge with Ocean theme
 *
 * Shows the number of open tabs in a rounded square badge.
 * Clicking opens the TabSwitcherView to see all tabs.
 *
 * Features:
 * - Shows tab count (1-99, then 99+ for overflow)
 * - Ocean Blue Glassmorphism styling
 * - Rounded rectangle with border
 * - Clickable to open tab switcher
 * - Voice command: "show tabs" or "switch tabs"
 *
 * @param tabCount Number of open tabs
 * @param onClick Callback when badge is clicked (opens tab switcher)
 * @param modifier Modifier for customization
 */
@Composable
fun TabCounterBadge(
    tabCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = OceanTheme.surfaceElevated,
        border = BorderStroke(1.5.dp, OceanTheme.border),
        shadowElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Display count with overflow handling
            val displayCount = when {
                tabCount <= 0 -> "0"
                tabCount < 100 -> tabCount.toString()
                else -> "99+"
            }

            Text(
                text = displayCount,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (tabCount < 100) 14.sp else 11.sp
                ),
                color = OceanTheme.textPrimary
            )
        }
    }
}

/**
 * CompactTabCounterBadge - Compact tab counter for AddressBar with Ocean theme
 *
 * Features:
 * - Smaller size (32x28dp) for tight spaces
 * - Ocean Blue rounded rectangle style
 * - Border matching theme
 * - Shows count up to 99
 */
@Composable
fun CompactTabCounterBadge(
    tabCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(32.dp)
            .height(28.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = OceanTheme.surfaceElevated,
        border = BorderStroke(1.dp, OceanTheme.border)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val displayCount = when {
                tabCount <= 0 -> "0"
                tabCount < 100 -> tabCount.toString()
                else -> "99"
            }

            Text(
                text = displayCount,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (tabCount < 10) 12.sp else 10.sp
                ),
                color = OceanTheme.textPrimary
            )
        }
    }
}
