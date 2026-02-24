package com.augmentalis.cockpit.model

import com.augmentalis.avanueui.display.GlassDisplayMode
import com.augmentalis.avanueui.theme.AppearanceMode
import com.augmentalis.avanueui.theme.AvanueColorPalette
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.cockpit.ui.BackgroundScene

/**
 * Groups the 12 read-only state parameters consumed by [CockpitScreenContent].
 *
 * Callbacks remain as direct lambda params (Compose convention) — this data class
 * only bundles the values that flow one-way from ViewModel → UI. Reduces the
 * parameter count from ~39 to ~28 in CockpitScreenContent.
 *
 * @param sessionName Display name of the active session ("Cockpit" when null)
 * @param frames Ordered list of frames in the active session
 * @param selectedFrameId ID of the currently focused frame, or null
 * @param layoutMode Active layout mode (DASHBOARD, FREEFORM, GRID, etc.)
 * @param dashboardState Aggregated dashboard data (sessions, modules, templates)
 * @param availableLayoutModes Device-filtered list of usable layout modes
 * @param backgroundScene Active background animation
 * @param glassDisplayMode Glass display rendering mode (FLAT_SCREEN, SEE_THROUGH, etc.)
 * @param currentPalette Active AvanueUI color palette
 * @param currentMaterial Active material rendering mode
 * @param currentAppearance Active appearance (Light/Dark/Auto)
 * @param currentPresetId ID of the active theme preset, or null for manual overrides
 */
data class CockpitScreenState(
    val sessionName: String = "Cockpit",
    val frames: List<CockpitFrame> = emptyList(),
    val selectedFrameId: String? = null,
    val layoutMode: LayoutMode = LayoutMode.DEFAULT,
    val dashboardState: DashboardState = DashboardState(),
    val availableLayoutModes: List<LayoutMode> = LayoutMode.entries,
    val backgroundScene: BackgroundScene = BackgroundScene.GRADIENT,
    val glassDisplayMode: GlassDisplayMode = GlassDisplayMode.FLAT_SCREEN,
    val currentPalette: AvanueColorPalette = AvanueColorPalette.DEFAULT,
    val currentMaterial: MaterialMode = MaterialMode.DEFAULT,
    val currentAppearance: AppearanceMode = AppearanceMode.DEFAULT,
    val currentPresetId: String? = null,
)
