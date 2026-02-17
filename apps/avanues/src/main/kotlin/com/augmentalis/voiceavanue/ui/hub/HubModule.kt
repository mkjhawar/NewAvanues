/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.hub

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceavanue.AvanueMode

/**
 * Orbit tier determines which ring a module appears on.
 * CORE modules are the 4 primary apps on the inner orbit.
 * CONTENT modules are the 7 content viewers on the outer orbit.
 */
enum class OrbitTier { CORE, CONTENT }

/**
 * A module entry for the Spatial Orbit hub launcher.
 * Each module maps to a visual node on one of the two orbit rings.
 */
data class HubModule(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val icon: ImageVector,
    val orbit: OrbitTier,
    val route: String
)

/**
 * Static registry of all modules displayed on the Spatial Orbit hub.
 *
 * Core orbit (inner ring, 4 modules):
 *   VoiceTouch, WebAvanue, CursorAvanue, Cockpit
 *
 * Content orbit (outer ring, 7 modules):
 *   PDF, Image, Video, Notes, Camera, Cast, Annotate
 *
 * Content modules navigate to Cockpit where they render inside frames.
 */
object HubModuleRegistry {

    val coreModules: List<HubModule> = listOf(
        HubModule(
            id = "voiceavanue",
            displayName = "VoiceTouch\u2122",
            subtitle = "Voice control platform",
            icon = Icons.Default.Mic,
            orbit = OrbitTier.CORE,
            route = AvanueMode.VOICE.route
        ),
        HubModule(
            id = "webavanue",
            displayName = "WebAvanue",
            subtitle = "Voice browser",
            icon = Icons.Default.Language,
            orbit = OrbitTier.CORE,
            route = AvanueMode.BROWSER.route
        ),
        HubModule(
            id = "voicecursor",
            displayName = "CursorAvanue",
            subtitle = "Handsfree cursor",
            icon = Icons.Default.Mouse,
            orbit = OrbitTier.CORE,
            route = AvanueMode.SETTINGS.route // TODO: Route to CURSOR when dedicated screen exists
        ),
        HubModule(
            id = "cockpit",
            displayName = "Cockpit",
            subtitle = "Multi-window display",
            icon = Icons.Default.Dashboard,
            orbit = OrbitTier.CORE,
            route = AvanueMode.COCKPIT.route
        )
    )

    val contentModules: List<HubModule> = listOf(
        HubModule(
            id = "pdfavanue",
            displayName = "PDF",
            subtitle = "Document viewer",
            icon = Icons.Default.PictureAsPdf,
            orbit = OrbitTier.CONTENT,
            route = AvanueMode.COCKPIT.route
        ),
        HubModule(
            id = "imageavanue",
            displayName = "Image",
            subtitle = "Image viewer",
            icon = Icons.Default.Image,
            orbit = OrbitTier.CONTENT,
            route = AvanueMode.COCKPIT.route
        ),
        HubModule(
            id = "videoavanue",
            displayName = "Video",
            subtitle = "Media player",
            icon = Icons.Default.VideoLibrary,
            orbit = OrbitTier.CONTENT,
            route = AvanueMode.COCKPIT.route
        ),
        HubModule(
            id = "noteavanue",
            displayName = "Notes",
            subtitle = "Rich text editor",
            icon = Icons.Default.EditNote,
            orbit = OrbitTier.CONTENT,
            route = AvanueMode.COCKPIT.route
        ),
        HubModule(
            id = "cameraavanue",
            displayName = "Camera",
            subtitle = "Capture & scan",
            icon = Icons.Default.CameraAlt,
            orbit = OrbitTier.CONTENT,
            route = AvanueMode.COCKPIT.route
        ),
        HubModule(
            id = "remotecast",
            displayName = "Cast",
            subtitle = "Screen sharing",
            icon = Icons.Default.Cast,
            orbit = OrbitTier.CONTENT,
            route = AvanueMode.COCKPIT.route
        ),
        HubModule(
            id = "annotationavanue",
            displayName = "Annotate",
            subtitle = "Draw & sign",
            icon = Icons.Default.Draw,
            orbit = OrbitTier.CONTENT,
            route = AvanueMode.COCKPIT.route
        )
    )

    val allModules: List<HubModule> = coreModules + contentModules
}

/**
 * Resolves the accent color for a module by its ID.
 * Core and content modules may share colors since they're on different orbits.
 */
@Composable
fun moduleAccentColor(moduleId: String): Color {
    return when (moduleId) {
        // Core orbit
        "voiceavanue" -> AvanueTheme.colors.success
        "webavanue" -> AvanueTheme.colors.info
        "voicecursor" -> AvanueTheme.colors.warning
        "cockpit" -> AvanueTheme.colors.tertiary
        // Content orbit
        "pdfavanue" -> AvanueTheme.colors.error
        "imageavanue" -> AvanueTheme.colors.primary
        "videoavanue" -> AvanueTheme.colors.secondary
        "noteavanue" -> AvanueTheme.colors.success
        "cameraavanue" -> AvanueTheme.colors.info
        "remotecast" -> AvanueTheme.colors.warning
        "annotationavanue" -> AvanueTheme.colors.tertiary
        else -> AvanueTheme.colors.primary
    }
}
