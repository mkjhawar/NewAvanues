package com.avanues.cockpit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Cockpit Shapes
 *
 * Rounded corners for windows, controls, and UI elements
 */
val CockpitShapes = Shapes(
    // Windows (minimal and glass styles)
    extraSmall = RoundedCornerShape(4.dp),    // Small UI elements
    small = RoundedCornerShape(8.dp),         // Minimal window corners
    medium = RoundedCornerShape(12.dp),       // Glass window corners
    large = RoundedCornerShape(16.dp),        // Large windows, theater mode
    extraLarge = RoundedCornerShape(24.dp)    // Control Rail, special surfaces
)
