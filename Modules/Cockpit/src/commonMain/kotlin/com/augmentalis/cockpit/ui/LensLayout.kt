package com.augmentalis.cockpit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.components.AvanueSurface
import com.augmentalis.avanueui.display.DisplayProfile
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.DashboardModule
import com.augmentalis.cockpit.model.DashboardModuleRegistry
import com.augmentalis.cockpit.model.DashboardState

/**
 * Lens — Command Palette Focus shell.
 *
 * Philosophy: "Everything is one voice command or one keystroke away.
 * The UI is a lens — it focuses on exactly what you ask for."
 *
 * The home screen is intentionally empty except for the centered Lens bar.
 * When activated (voice, tap, or keyboard shortcut), the Lens bar shows
 * fuzzy-matched results across modules, recent files, commands, and settings.
 *
 * This is the recommended variation because:
 * 1. Lowest implementation effort — command palette is a proven pattern
 * 2. Highest power-user satisfaction — keyboard + voice users both benefit
 * 3. Best voice mapping — "type what you want" = "say what you want"
 * 4. Most universal — works on every display including glasses
 * 5. Cleanest cognitive model — ONE entry point for everything
 *
 * Responsive adaptation:
 * - Glass: Voice-only (no visual Lens bar, audio results)
 * - Phone: Full-width Lens centered at 40% height
 * - Tablet: 60% width, centered
 * - Desktop: 50% width, centered, keyboard shortcut hint shown
 */
@Composable
fun LensLayout(
    dashboardState: DashboardState,
    displayProfile: DisplayProfile = DisplayProfile.PHONE,
    onModuleClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onVoiceActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AvanueTheme.colors
    var query by remember { mutableStateOf("") }
    var isLensActive by remember { mutableStateOf(false) }

    val modules = dashboardState.availableModules.ifEmpty { DashboardModuleRegistry.allModules }
    val recentSessions = dashboardState.recentSessions

    // Build search results
    val results = remember(query, modules, recentSessions) {
        buildLensResults(query, modules, recentSessions)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = lensHorizontalPadding(displayProfile)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top spacer — pushes Lens bar to ~35% of screen height
            Spacer(modifier = Modifier.weight(if (isLensActive) 0.15f else 0.35f))

            // ── Lens Bar ─────────────────────────────────────────────────
            LensBar(
                query = query,
                onQueryChange = { query = it },
                onFocusChange = { isLensActive = it },
                onVoiceClick = onVoiceActivate,
                displayProfile = displayProfile,
                modifier = Modifier
                    .widthIn(max = lensMaxWidth(displayProfile))
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Results or Ghost Hints ───────────────────────────────────
            AnimatedVisibility(
                visible = isLensActive || query.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                LensResultsPanel(
                    results = results,
                    displayProfile = displayProfile,
                    onModuleClick = { id ->
                        query = ""
                        isLensActive = false
                        onModuleClick(id)
                    },
                    onSessionClick = { id ->
                        query = ""
                        isLensActive = false
                        onSessionClick(id)
                    },
                    modifier = Modifier
                        .widthIn(max = lensMaxWidth(displayProfile))
                        .fillMaxWidth()
                )
            }

            // Ghost hints — shown only when Lens is inactive
            AnimatedVisibility(
                visible = !isLensActive && query.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LensGhostHints(
                    recentSessions = recentSessions,
                    modules = modules,
                    displayProfile = displayProfile,
                    onModuleClick = onModuleClick,
                    onSessionClick = onSessionClick,
                )
            }

            // Bottom spacer
            Spacer(modifier = Modifier.weight(0.5f))

            // Footer
            if (!isLensActive) {
                Text(
                    text = "VoiceOS\u00AE Avanues EcoSystem",
                    color = colors.textPrimary.copy(alpha = 0.2f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        // Voice indicator (bottom-right)
        if (!isLensActive && !displayProfile.isGlass) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.primary.copy(alpha = 0.1f))
                    .clickable { onVoiceActivate() }
                    .semantics { contentDescription = "Voice: click Microphone" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Lens Bar ────────────────────────────────────────────────────────────────

/**
 * The central Lens bar — search/voice entry point for everything.
 */
@Composable
private fun LensBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onVoiceClick: () -> Unit,
    displayProfile: DisplayProfile,
    modifier: Modifier = Modifier,
) {
    val colors = AvanueTheme.colors

    AvanueSurface(
        modifier = modifier
            .height(52.dp)
            .semantics { contentDescription = "Voice: click Search" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = colors.textPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onFocusChange(it.isFocused) },
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(colors.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "What next?",
                                color = colors.textPrimary.copy(alpha = 0.35f),
                                fontSize = 16.sp,
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Voice activation button inside the bar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onVoiceClick() }
                    .semantics { contentDescription = "Voice: click Speak" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Lens Results ────────────────────────────────────────────────────────────

/**
 * Categories of Lens search results.
 */
enum class LensResultCategory(val label: String) {
    MODULES("Modules"),
    RECENT("Recent"),
    COMMANDS("Commands"),
}

/**
 * A single result item in the Lens results panel.
 */
data class LensResult(
    val id: String,
    val category: LensResultCategory,
    val title: String,
    val subtitle: String,
    val moduleId: String? = null,
    val sessionId: String? = null,
)

/**
 * Builds search results from query, matching across modules and sessions.
 */
private fun buildLensResults(
    query: String,
    modules: List<DashboardModule>,
    sessions: List<CockpitSession>,
): List<LensResult> {
    if (query.isBlank()) {
        // Zero-query: show recent sessions + top modules
        val results = mutableListOf<LensResult>()
        sessions.take(3).forEach { session ->
            results.add(
                LensResult(
                    id = "recent_${session.id}",
                    category = LensResultCategory.RECENT,
                    title = session.name,
                    subtitle = layoutModeLabel(session.layoutMode),
                    sessionId = session.id,
                )
            )
        }
        modules.take(5).forEach { module ->
            results.add(
                LensResult(
                    id = "module_${module.id}",
                    category = LensResultCategory.MODULES,
                    title = module.displayName,
                    subtitle = module.subtitle,
                    moduleId = module.id,
                )
            )
        }
        return results
    }

    val lowerQuery = query.lowercase()
    val results = mutableListOf<LensResult>()

    // Match modules
    modules.filter {
        it.displayName.lowercase().contains(lowerQuery) ||
            it.subtitle.lowercase().contains(lowerQuery) ||
            it.id.lowercase().contains(lowerQuery)
    }.forEach { module ->
        results.add(
            LensResult(
                id = "module_${module.id}",
                category = LensResultCategory.MODULES,
                title = module.displayName,
                subtitle = module.subtitle,
                moduleId = module.id,
            )
        )
    }

    // Match recent sessions
    sessions.filter {
        it.name.lowercase().contains(lowerQuery)
    }.forEach { session ->
        results.add(
            LensResult(
                id = "recent_${session.id}",
                category = LensResultCategory.RECENT,
                title = session.name,
                subtitle = layoutModeLabel(session.layoutMode),
                sessionId = session.id,
            )
        )
    }

    return results
}

/**
 * Results panel shown below the Lens bar when active.
 */
@Composable
private fun LensResultsPanel(
    results: List<LensResult>,
    displayProfile: DisplayProfile,
    onModuleClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AvanueTheme.colors
    val grouped = results.groupBy { it.category }

    AvanueSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            grouped.forEach { (category, items) ->
                // Category header
                item(key = "header_${category.name}") {
                    Text(
                        text = category.label,
                        color = colors.textPrimary.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Result items
                items(
                    items = items,
                    key = { it.id }
                ) { result ->
                    LensResultRow(
                        result = result,
                        onClick = {
                            when {
                                result.moduleId != null -> onModuleClick(result.moduleId)
                                result.sessionId != null -> onSessionClick(result.sessionId)
                            }
                        }
                    )
                }
            }

            if (results.isEmpty()) {
                item {
                    Text(
                        text = "No matches found",
                        color = colors.textPrimary.copy(alpha = 0.3f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * A single result row in the Lens results panel.
 */
@Composable
private fun LensResultRow(
    result: LensResult,
    onClick: () -> Unit,
) {
    val colors = AvanueTheme.colors
    val icon = when (result.category) {
        LensResultCategory.RECENT -> Icons.Default.History
        LensResultCategory.MODULES -> Icons.AutoMirrored.Filled.ArrowForward
        LensResultCategory.COMMANDS -> Icons.AutoMirrored.Filled.ArrowForward
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { contentDescription = "Voice: click ${result.title}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.textPrimary.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = result.subtitle,
                color = colors.textPrimary.copy(alpha = 0.4f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Ghost Hints ─────────────────────────────────────────────────────────────

/**
 * Ghost hints shown below the Lens bar when inactive.
 * Displays the 3-5 most relevant actions as subtle text.
 */
@Composable
private fun LensGhostHints(
    recentSessions: List<CockpitSession>,
    modules: List<DashboardModule>,
    displayProfile: DisplayProfile,
    onModuleClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
) {
    val colors = AvanueTheme.colors
    val maxHints = if (displayProfile == DisplayProfile.PHONE) 3 else 5

    // Build hint strings from recent sessions and modules
    val hints = mutableListOf<Pair<String, () -> Unit>>()
    recentSessions.take(2).forEach { session ->
        hints.add(session.name to { onSessionClick(session.id) })
    }
    modules.take(maxHints - hints.size).forEach { module ->
        hints.add(module.displayName to { onModuleClick(module.id) })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        hints.forEachIndexed { index, (label, onClick) ->
            if (index > 0) {
                Text(
                    text = " \u00B7 ",
                    color = colors.textPrimary.copy(alpha = 0.2f),
                    fontSize = 12.sp,
                )
            }
            Text(
                text = label,
                color = colors.textPrimary.copy(alpha = 0.3f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .semantics { contentDescription = "Voice: click $label" }
            )
        }
    }
}

// ── Responsive Helpers ──────────────────────────────────────────────────────

private fun lensMaxWidth(profile: DisplayProfile) = when (profile) {
    DisplayProfile.GLASS_MICRO -> 400.dp
    DisplayProfile.GLASS_COMPACT -> 400.dp
    DisplayProfile.GLASS_STANDARD -> 500.dp
    DisplayProfile.PHONE -> 600.dp
    DisplayProfile.TABLET -> 560.dp
    DisplayProfile.GLASS_HD -> 500.dp
}

private fun lensHorizontalPadding(profile: DisplayProfile) = when (profile) {
    DisplayProfile.GLASS_MICRO -> 8.dp
    DisplayProfile.GLASS_COMPACT -> 8.dp
    DisplayProfile.GLASS_STANDARD -> 12.dp
    DisplayProfile.PHONE -> 24.dp
    DisplayProfile.TABLET -> 48.dp
    DisplayProfile.GLASS_HD -> 16.dp
}
