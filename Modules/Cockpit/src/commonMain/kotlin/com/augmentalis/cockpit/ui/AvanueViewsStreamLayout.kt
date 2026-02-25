package com.augmentalis.cockpit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.components.AvanueFAB
import com.augmentalis.avanueui.display.DisplayProfile
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.DashboardModule
import com.augmentalis.cockpit.model.DashboardModuleRegistry
import com.augmentalis.cockpit.model.DashboardState

/**
 * AvanueViews — Ambient Card Stream shell.
 *
 * Philosophy: "The UI whispers to you — cards surface when relevant, fade when done."
 *
 * Cards are prioritized by context:
 * - **P0 Active Context** (1 card max): What you're currently working on
 * - **P1 Ambient Awareness** (2-3 cards): Passive state indicators
 * - **P2 Suggestions** (1-2 cards): AI-suggested next actions
 * - **P3 Ghost Hints** (1 card): Voice command discovery, rotates through commands
 *
 * Responsive adaptation via [DisplayProfile]:
 * - Glass (paginated): Single card at a time, voice/swipe navigation
 * - Phone portrait: Single column, 2-3 cards visible
 * - Phone landscape / Tablet: 2-3 columns, 4-6 cards visible
 * - Desktop: 3 columns + optional sidebar
 *
 * Voice is THE primary input. The FAB is always present. Cards show voice
 * command hints as ghost text to teach voice naturally.
 */
@Composable
fun AvanueViewsStreamLayout(
    dashboardState: DashboardState,
    displayProfile: DisplayProfile = DisplayProfile.PHONE,
    onModuleClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onVoiceFabClick: () -> Unit,
    onMoreModulesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AvanueTheme.colors
    val columns = streamColumns(displayProfile)
    val cards = buildStreamCards(dashboardState)

    Box(modifier = modifier.fillMaxSize()) {
        // Card stream
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = streamHorizontalPadding(displayProfile)),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = 80.dp // Space for FAB
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Status orb + time (minimal header)
            item(span = { GridItemSpan(columns) }) {
                StreamHeader(displayProfile = displayProfile)
            }

            // Stream cards
            items(
                items = cards,
                key = { it.id }
            ) { card ->
                StreamCard(
                    card = card,
                    displayProfile = displayProfile,
                    onModuleClick = onModuleClick,
                    onSessionClick = onSessionClick,
                )
            }

            // Ghost hint card — teaches voice commands
            item(span = { GridItemSpan(if (columns > 1) 1 else columns) }) {
                GhostHintCard(
                    onMoreModulesClick = onMoreModulesClick,
                )
            }

            // Footer
            item(span = { GridItemSpan(columns) }) {
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
                }
            }
        }

        // Voice FAB — always present, bottom-center on phone, bottom-right on larger
        val fabAlignment = if (displayProfile == DisplayProfile.PHONE) {
            Alignment.BottomCenter
        } else {
            Alignment.BottomEnd
        }

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(fabAlignment).padding(16.dp)
        ) {
            AvanueFAB(
                onClick = onVoiceFabClick,
                modifier = Modifier
                    .size(56.dp)
                    .semantics { contentDescription = "Voice: click Microphone" }
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ── Stream Card Data Model ──────────────────────────────────────────────────

/**
 * Priority level for stream cards. Lower number = higher priority = shown first.
 */
enum class StreamCardPriority {
    /** Active context — what you're working on right now (1 card max) */
    P0_ACTIVE,
    /** Ambient awareness — passive state indicators */
    P1_AMBIENT,
    /** AI suggestions — next actions based on patterns */
    P2_SUGGESTION,
    /** Ghost hints — voice command discovery */
    P3_GHOST,
}

/**
 * A card in the AvanueViews stream. Cards are prioritized and sorted by
 * [priority] to ensure the most relevant information is always visible first.
 */
data class StreamCardData(
    val id: String,
    val priority: StreamCardPriority,
    val title: String,
    val subtitle: String,
    val voiceHint: String,
    val iconName: String,
    val moduleId: String? = null,
    val sessionId: String? = null,
    val accentColorHex: Long = 0xFF2196F3L,
)

// ── Card Building ───────────────────────────────────────────────────────────

/**
 * Builds the stream card list from dashboard state.
 * Cards are sorted by priority (P0 first, P3 last).
 */
private fun buildStreamCards(state: DashboardState): List<StreamCardData> {
    val cards = mutableListOf<StreamCardData>()

    // P0: Active context — most recent session
    state.recentSessions.firstOrNull()?.let { session ->
        cards.add(
            StreamCardData(
                id = "active_${session.id}",
                priority = StreamCardPriority.P0_ACTIVE,
                title = session.name,
                subtitle = "Resume editing",
                voiceHint = "say: resume ${session.name.lowercase()}",
                iconName = "history",
                sessionId = session.id,
                accentColorHex = 0xFF42A5F5L,
            )
        )
    }

    // P1: Ambient awareness — open modules / recent sessions (2-3 max)
    state.recentSessions.drop(1).take(2).forEach { session ->
        cards.add(
            StreamCardData(
                id = "ambient_${session.id}",
                priority = StreamCardPriority.P1_AMBIENT,
                title = session.name,
                subtitle = layoutModeLabel(session.layoutMode),
                voiceHint = "say: open ${session.name.lowercase()}",
                iconName = "play_arrow",
                sessionId = session.id,
                accentColorHex = 0xFF78909CL,
            )
        )
    }

    // P1: Show a few top modules as ambient cards
    val topModules = state.availableModules.ifEmpty { DashboardModuleRegistry.allModules }
    topModules.take(3).forEach { module ->
        cards.add(
            StreamCardData(
                id = "module_${module.id}",
                priority = StreamCardPriority.P1_AMBIENT,
                title = module.displayName,
                subtitle = module.subtitle,
                voiceHint = "say: open ${module.displayName.lowercase()}",
                iconName = module.iconName,
                moduleId = module.id,
                accentColorHex = module.accentColorHex,
            )
        )
    }

    // P2: Suggestion — speech performance hint if available
    if (state.speechMetrics != null) {
        cards.add(
            StreamCardData(
                id = "speech_metrics",
                priority = StreamCardPriority.P2_SUGGESTION,
                title = "Speech Engine",
                subtitle = "Avg ${state.speechMetrics.avgLatencyMs}ms latency",
                voiceHint = "say: show speech stats",
                iconName = "mic",
                accentColorHex = 0xFF66BB6AL,
            )
        )
    }

    return cards.sortedBy { it.priority.ordinal }
}

// ── Responsive Helpers ──────────────────────────────────────────────────────

private fun streamColumns(profile: DisplayProfile): Int = when (profile) {
    DisplayProfile.GLASS_MICRO -> 1
    DisplayProfile.GLASS_COMPACT -> 1
    DisplayProfile.GLASS_STANDARD -> 1
    DisplayProfile.PHONE -> 1
    DisplayProfile.TABLET -> 2
    DisplayProfile.GLASS_HD -> 2
}

private fun streamHorizontalPadding(profile: DisplayProfile) = when (profile) {
    DisplayProfile.GLASS_MICRO -> 4.dp
    DisplayProfile.GLASS_COMPACT -> 6.dp
    DisplayProfile.GLASS_STANDARD -> 8.dp
    DisplayProfile.PHONE -> 16.dp
    DisplayProfile.TABLET -> 24.dp
    DisplayProfile.GLASS_HD -> 12.dp
}

// ── Composables ─────────────────────────────────────────────────────────────

/**
 * Minimal stream header with AVA orb indicator.
 */
@Composable
private fun StreamHeader(displayProfile: DisplayProfile) {
    val colors = AvanueTheme.colors

    if (displayProfile.isGlass) {
        // Glass: no header, maximize content space
        Spacer(modifier = Modifier.height(4.dp))
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AVA orb
            Box(
                modifier = Modifier.size(8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(
                        color = colors.primary,
                        radius = size.minDimension / 2
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AVA",
                color = colors.textPrimary.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * A single stream card — adapts visual weight to priority level.
 */
@Composable
private fun StreamCard(
    card: StreamCardData,
    displayProfile: DisplayProfile,
    onModuleClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
) {
    val colors = AvanueTheme.colors
    val isActive = card.priority == StreamCardPriority.P0_ACTIVE
    val cardHeight = when {
        displayProfile.isGlass -> 64.dp
        isActive -> 96.dp
        else -> 80.dp
    }

    AvanueCard(
        onClick = {
            when {
                card.sessionId != null -> onSessionClick(card.sessionId)
                card.moduleId != null -> onModuleClick(card.moduleId)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .semantics {
                contentDescription = "Voice: click ${card.title}"
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = card.title,
                    color = colors.textPrimary,
                    fontSize = if (isActive) 15.sp else 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textPrimary.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Subtitle
            Text(
                text = card.subtitle,
                color = colors.textPrimary.copy(alpha = 0.5f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Voice hint (ghost text) — hidden on glass for space
            if (!displayProfile.isGlass) {
                Text(
                    text = card.voiceHint,
                    color = colors.primary.copy(alpha = 0.3f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Ghost hint card — teaches voice commands and provides "browse all" entry point.
 */
@Composable
private fun GhostHintCard(
    onMoreModulesClick: () -> Unit,
) {
    val colors = AvanueTheme.colors

    AvanueCard(
        onClick = onMoreModulesClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { contentDescription = "Voice: click Browse Modules" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "\"Open [module name]\"",
                    color = colors.textPrimary.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = "or tap to browse all modules",
                color = colors.textPrimary.copy(alpha = 0.25f),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 26.dp, top = 2.dp)
            )
        }
    }
}
