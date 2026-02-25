/**
 * ComposableSettingsProvider.kt - Compose-aware settings provider interface
 *
 * Extends the pure KMP ModuleSettingsProvider with Compose rendering.
 * Lives at app level where both Hilt and Compose are available.
 * Each module implements this to render its settings sections.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.foundation.settings.ModuleSettingsProvider

/**
 * Compose-aware extension of ModuleSettingsProvider.
 *
 * Implementations render their settings sections as Compose content.
 * The UnifiedSettingsScreen calls SectionContent() for the selected section.
 */
interface ComposableSettingsProvider : ModuleSettingsProvider {

    /**
     * Renders the content for a specific section of this module's settings.
     *
     * Called by the UnifiedSettingsScreen when the user selects this module
     * and navigates to a particular section (or the default section).
     *
     * @param sectionId The section to render (from [sections] list)
     */
    @Composable
    fun SectionContent(sectionId: String)

    /**
     * The Material icon for this module in the settings list.
     *
     * Default implementation resolves iconName to a generic settings icon.
     * Providers should override with the appropriate icon.
     */
    @Composable
    fun ModuleIcon(): ImageVector = Icons.Default.Settings
}
