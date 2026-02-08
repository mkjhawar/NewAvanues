/**
 * PermissionsSettingsProvider.kt - System permissions settings section
 *
 * Provides Accessibility Service and Overlay Permission settings.
 * These link to system settings screens (no local state to persist).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsNavigationRow
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import javax.inject.Inject

class PermissionsSettingsProvider @Inject constructor() : ComposableSettingsProvider {

    override val moduleId = "permissions"
    override val displayName = "Permissions"
    override val iconName = "Security"
    override val sortOrder = 100

    override val sections = listOf(
        SettingsSection(id = "permissions", title = "Permissions")
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "accessibility_service",
            displayName = "Accessibility Service",
            sectionId = "permissions",
            keywords = listOf("accessibility", "voice control", "screen reader")
        ),
        SearchableSettingEntry(
            key = "overlay_permission",
            displayName = "Overlay Permission",
            sectionId = "permissions",
            keywords = listOf("overlay", "cursor", "HUD", "draw over")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val context = LocalContext.current

        SettingsGroupCard {
            SettingsNavigationRow(
                title = "Accessibility Service",
                subtitle = "Required for voice control and screen reading",
                icon = Icons.Default.Accessibility,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            SettingsNavigationRow(
                title = "Overlay Permission",
                subtitle = "Required for cursor and HUD display",
                icon = Icons.Default.Layers,
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.Security
}
