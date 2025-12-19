// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/ui/OverlayComposables.kt
// created: 2025-11-01 23:00:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 2 - Glassmorphic UI
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.overlay.controller.OrbState
import com.augmentalis.overlay.controller.OverlayController
import com.augmentalis.overlay.controller.OverlayState

/**
 * Root overlay UI composition.
 *
 * Combines VoiceOrb and GlassMorphicPanel into complete overlay interface.
 * The orb is always visible and draggable. The panel appears above it when expanded.
 *
 * Layout:
 * - VoiceOrb: Positioned at controller.orbPosition, draggable
 * - GlassMorphicPanel: Centered, appears when expanded
 *
 * @param controller Overlay state controller
 * @param modifier Optional modifier
 * @author Manoj Jhawar
 */
@Composable
fun OverlayComposables(
    controller: OverlayController,
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsState()
    val expanded by controller.expanded.collectAsState()
    val orbPosition by controller.orbPosition.collectAsState()
    val transcript by controller.transcript.collectAsState()
    val response by controller.response.collectAsState()
    val suggestions by controller.suggestions.collectAsState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Glassmorphic panel (centered, appears when expanded)
        GlassMorphicPanel(
            expanded = expanded,
            transcript = transcript,
            response = response,
            suggestions = suggestions,
            onSuggestionClick = { suggestion ->
                controller.executeSuggestion(suggestion)
            },
            modifier = Modifier.align(Alignment.Center)
        )

        // Voice orb (draggable, always visible)
        VoiceOrb(
            position = orbPosition,
            state = mapOverlayStateToOrbState(state),
            onTap = {
                if (expanded) {
                    controller.collapse()
                } else {
                    controller.expand()
                }
            },
            onDrag = { delta ->
                controller.updateOrbPosition(delta)
            }
        )
    }
}

/**
 * Maps OverlayState to OrbState for visual representation
 */
private fun mapOverlayStateToOrbState(overlayState: OverlayState): OrbState {
    return when (overlayState) {
        OverlayState.Docked -> OrbState.Idle
        OverlayState.Listening -> OrbState.Listening
        OverlayState.Processing -> OrbState.Processing
        OverlayState.Responding -> OrbState.Speaking
        OverlayState.Error -> OrbState.Idle // Default to idle on error
    }
}
