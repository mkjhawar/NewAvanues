/**
 * HubDashboardScreen.kt - Master dashboard for the Avanues ecosystem
 *
 * Spatial Orbit launcher showing all 11 modules in a radial layout:
 *   Center: VoiceOS brain (pulsing hub icon)
 *   Inner orbit: 4 core modules (VoiceTouch, WebAvanue, CursorAvanue, Cockpit)
 *   Outer orbit: 7 content modules (PDF, Image, Video, Notes, Camera, Cast, Annotate)
 *   Bottom dock: Settings, About, Developer (easter egg)
 *
 * Launched from the "Avanues" launcher icon.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.hub

import androidx.compose.runtime.Composable

/**
 * Master hub dashboard — Spatial Orbit launcher.
 *
 * Routes module clicks via [onNavigateToRoute], which receives the route string
 * from the clicked [HubModule]. The caller (NavHost) handles actual navigation.
 */
@Composable
fun HubDashboardScreen(
    onNavigateToRoute: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToDeveloperSettings: () -> Unit = {}
) {
    SpatialOrbitHub(
        onModuleClick = { module -> onNavigateToRoute(module.route) },
        onSettingsClick = onNavigateToSettings,
        onAboutClick = onNavigateToAbout,
        onDeveloperClick = onNavigateToDeveloperSettings
    )
}
