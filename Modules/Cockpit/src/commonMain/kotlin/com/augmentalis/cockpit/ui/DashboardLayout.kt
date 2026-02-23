package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.BuiltInTemplates
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.DashboardModule
import com.augmentalis.cockpit.model.DashboardModuleRegistry
import com.augmentalis.cockpit.model.DashboardState
import com.augmentalis.cockpit.model.SessionTemplate

/**
 * Dashboard layout — the Cockpit home/launcher view.
 *
 * Displays module tiles for quick launch, recent sessions for resuming work,
 * and session templates for creating pre-configured workspaces.
 *
 * Shown when:
 * - No session is active (first launch)
 * - User navigates "home" from an active session
 * - LayoutMode is DASHBOARD
 */
@Composable
fun DashboardLayout(
    dashboardState: DashboardState,
    onModuleClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onTemplateClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Recent Sessions (horizontal strip spanning full width) ────────
        if (dashboardState.recentSessions.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                RecentSessionsStrip(
                    sessions = dashboardState.recentSessions,
                    onSessionClick = onSessionClick
                )
            }
        }

        // ── Quick Launch Section Header ──────────────────────────────────
        item(span = { GridItemSpan(3) }) {
            SectionHeader(title = "Quick Launch")
        }

        // ── Core Modules ─────────────────────────────────────────────────
        items(
            items = dashboardState.availableModules.ifEmpty { DashboardModuleRegistry.allModules },
            key = { it.id }
        ) { module ->
            ModuleTile(
                module = module,
                onClick = { onModuleClick(module.id) }
            )
        }

        // ── Templates Section ────────────────────────────────────────────
        item(span = { GridItemSpan(3) }) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "Templates")
        }

        items(
            items = dashboardState.templates.ifEmpty { BuiltInTemplates.ALL },
            key = { it.id }
        ) { template ->
            TemplateTile(
                template = template,
                onClick = { onTemplateClick(template.id) }
            )
        }

        // ── Footer ───────────────────────────────────────────────────────
        item(span = { GridItemSpan(3) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VoiceOS\u00AE Avanues EcoSystem",
                    color = colors.textPrimary.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Designed and Created in California with Love.",
                    color = colors.textPrimary.copy(alpha = 0.18f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Horizontal strip of recent sessions as compact cards.
 */
@Composable
private fun RecentSessionsStrip(
    sessions: List<CockpitSession>,
    onSessionClick: (String) -> Unit
) {
    val colors = AvanueTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = colors.textPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Recent Sessions",
                color = colors.textPrimary.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(sessions.take(8), key = { it.id }) { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session.id) }
                )
            }
        }
    }
}

/**
 * Compact session card for the recent sessions strip.
 */
@Composable
private fun SessionCard(
    session: CockpitSession,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors

    AvanueCard(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(72.dp)
            .semantics { contentDescription = "Voice: open ${session.name}" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = session.name,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = layoutModeLabel(session.layoutMode),
                    color = colors.textPrimary.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Section header text.
 */
@Composable
private fun SectionHeader(title: String) {
    val colors = AvanueTheme.colors

    Text(
        text = title,
        color = colors.textPrimary.copy(alpha = 0.7f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

/**
 * Module tile — square card with icon, name, and subtitle.
 * Used in the Quick Launch grid.
 */
@Composable
private fun ModuleTile(
    module: DashboardModule,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    val accentColor = Color(module.accentColorHex)

    AvanueCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .semantics { contentDescription = "Voice: click ${module.displayName}" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon circle with accent color
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    moduleIcon(module.iconName),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = module.displayName,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = module.subtitle,
                color = colors.textPrimary.copy(alpha = 0.4f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Maps a module's [iconName] string to a Material [ImageVector].
 * Falls back to [Icons.Default.Add] for unknown icon names.
 */
private fun moduleIcon(iconName: String): ImageVector = when (iconName) {
    "mic" -> Icons.Default.Mic
    "language" -> Icons.Default.Language
    "mouse" -> Icons.Default.TouchApp
    "picture_as_pdf" -> Icons.Default.PictureAsPdf
    "image" -> Icons.Default.Image
    "videocam" -> Icons.Default.Videocam
    "edit_note" -> Icons.Default.EditNote
    "photo_camera" -> Icons.Default.PhotoCamera
    "cast" -> Icons.Default.Cast
    "draw" -> Icons.Default.Brush
    else -> Icons.Default.Add
}

/**
 * Template tile — compact card for session templates.
 */
@Composable
private fun TemplateTile(
    template: SessionTemplate,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors

    AvanueCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .semantics { contentDescription = "Voice: click ${template.name}" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = template.name,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = template.description,
                color = colors.textPrimary.copy(alpha = 0.4f),
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "${template.frameDefinitions.size} frames \u00B7 ${layoutModeLabel(template.layoutMode)}",
                color = colors.primary.copy(alpha = 0.5f),
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
