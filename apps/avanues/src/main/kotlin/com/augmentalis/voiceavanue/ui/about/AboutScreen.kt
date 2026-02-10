/**
 * AboutScreen.kt - Dedicated "About Avanues" screen
 *
 * Separated from Settings to avoid confusion. Shows only:
 * - App info (version, build type, dev console Easter egg)
 * - Legal (open source licenses, privacy policy, terms)
 * - Credits
 *
 * Uses GroupedListDetailScaffold from AvanueUI for iOS-style navigation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.about

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.navigation.GroupedListDetailScaffold
import com.augmentalis.avanueui.components.navigation.GroupedListRow
import com.augmentalis.avanueui.components.navigation.GroupedListSection
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceavanue.BuildConfig
import com.augmentalis.voiceavanue.ui.settings.OssLicenseRegistry
import com.augmentalis.voiceavanue.ui.settings.OssLibrary
import com.augmentalis.voiceavanue.ui.settings.OssHolder
import com.augmentalis.voiceavanue.ui.settings.OssLicenseGroup

/**
 * About screen items — each represents a row in the grouped list.
 */
private enum class AboutItem(
    val displayTitle: String,
    val icon: ImageVector
) {
    VERSION("Version", Icons.Default.Info),
    BUILD("Version & Changelog", Icons.Default.Code),
    LICENSES("Open Source Licenses", Icons.Default.Description),
    PRIVACY("Privacy Policy", Icons.Default.PrivacyTip),
    TERMS("Terms of Service", Icons.Default.Gavel),
    CREDITS("Credits", Icons.Default.Favorite)
}

private fun aboutItemSubtitle(item: AboutItem): String = when (item) {
    AboutItem.VERSION -> BuildConfig.VERSION_NAME
    AboutItem.BUILD -> "What's new in this release"
    AboutItem.LICENSES -> "${OssLicenseRegistry.totalLibraries} libraries"
    AboutItem.PRIVACY -> "How we handle your data"
    AboutItem.TERMS -> "Usage terms and conditions"
    AboutItem.CREDITS -> "\u00A9 2018-2024 Intelligent Devices LLC"
}

/**
 * Entry point for the About Avanues screen.
 */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDeveloperConsole: () -> Unit
) {
    val sections = listOf(
        GroupedListSection(
            title = "App Info",
            items = listOf(AboutItem.VERSION, AboutItem.BUILD)
        ),
        GroupedListSection(
            title = "Legal",
            items = listOf(AboutItem.LICENSES, AboutItem.PRIVACY, AboutItem.TERMS)
        ),
        GroupedListSection(
            title = "",
            items = listOf(AboutItem.CREDITS)
        )
    )

    val context = LocalContext.current
    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    GroupedListDetailScaffold(
        title = "VoiceOS\u00AE Avanues",
        sections = sections,
        itemKey = { it.name },
        onNavigateBack = onNavigateBack,
        listRow = { item, onClick ->
            GroupedListRow(
                title = item.displayTitle,
                subtitle = aboutItemSubtitle(item),
                icon = item.icon,
                onClick = {
                    if (item == AboutItem.VERSION) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime > 2000L) {
                            versionTapCount = 1
                        } else {
                            versionTapCount++
                        }
                        lastTapTime = now

                        when {
                            versionTapCount >= 3 -> {
                                versionTapCount = 0
                                onNavigateToDeveloperConsole()
                            }
                            versionTapCount >= 2 -> {
                                Toast.makeText(
                                    context,
                                    "${3 - versionTapCount} tap(s) to developer mode",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        onClick()
                    }
                }
            )
        },
        detailTitle = { it.displayTitle },
        detailContent = { item, paddingValues ->
            AboutDetailContent(
                item = item,
                modifier = Modifier.padding(paddingValues)
            )
        }
    )
}

@Composable
private fun AboutDetailContent(
    item: AboutItem,
    modifier: Modifier = Modifier
) {
    when (item) {
        AboutItem.VERSION -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow("Version", BuildConfig.VERSION_NAME)
                InfoRow("Version Code", BuildConfig.VERSION_CODE.toString())
                InfoRow("Build Type", BuildConfig.BUILD_TYPE)
                InfoRow("Application ID", BuildConfig.APPLICATION_ID)
            }
        }

        AboutItem.BUILD -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AvanueTheme.colors.textPrimary
                )
                Text(
                    text = "Changelog will be loaded from AVU format files in a future update.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Build Type", BuildConfig.BUILD_TYPE)
                InfoRow("Debug", BuildConfig.DEBUG.toString())
            }
        }

        AboutItem.LICENSES -> {
            LicenseDetailContent(modifier = modifier)
        }

        AboutItem.PRIVACY -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AvanueTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Avanues is designed with privacy as a priority. We do not collect, " +
                        "transmit, or sell any personal data. All browsing data, voice commands, " +
                        "and settings are stored locally on your device.\n\n" +
                        "Voice processing occurs on-device. No audio recordings are transmitted " +
                        "to external servers.\n\n" +
                        "For the full privacy policy, visit our website.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textSecondary
                )
            }
        }

        AboutItem.TERMS -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AvanueTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "By using Avanues, you agree to our terms of service. " +
                        "This software is provided as-is for accessibility purposes.\n\n" +
                        "For the full terms, visit our website.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textSecondary
                )
            }
        }

        AboutItem.CREDITS -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Credits",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AvanueTheme.colors.textPrimary
                )
                Text(
                    text = "VoiceOS\u00AE Avanues EcoSystem",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AvanueTheme.colors.textPrimary
                )
                Text(
                    text = "Imagined, Designed & Written by: Manoj Jhawar with Aman Jhawar",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AvanueTheme.colors.textSecondary
                )
                Text(
                    text = "\u00A9 2018-2024 Intelligent Devices LLC and Augmentalis Inc.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AvanueTheme.colors.primary
                )
                Text(
                    text = "Designed and Created in California with Love.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textSecondary
                )
            }
        }
    }
}

/**
 * License list with expandable license text per group.
 */
@Composable
private fun LicenseDetailContent(modifier: Modifier = Modifier) {
    data class LibRow(
        val lib: OssLibrary,
        val holder: OssHolder,
        val group: OssLicenseGroup
    )

    val groups = remember { OssLicenseRegistry.groups() }
    val sectionedRows = remember {
        groups.map { group ->
            val rows = group.holders.flatMap { holder ->
                holder.libraries.map { lib -> LibRow(lib, holder, group) }
            }.sortedBy { it.lib.name.lowercase() }
            group to rows
        }
    }

    var selectedRow by remember { mutableStateOf<LibRow?>(null) }

    if (selectedRow != null) {
        val row = selectedRow!!
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedRow = null }
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp),
                        tint = AvanueTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Licenses",
                        style = MaterialTheme.typography.labelMedium,
                        color = AvanueTheme.colors.primary
                    )
                }
            }
            item {
                Column {
                    Text(
                        text = row.lib.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AvanueTheme.colors.textPrimary
                    )
                    if (row.lib.version.isNotEmpty()) {
                        Text(
                            text = "Version ${row.lib.version}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                    Text(
                        text = row.holder.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AvanueTheme.colors.primary
                    )
                    if (row.lib.note.isNotEmpty()) {
                        Text(
                            text = row.lib.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = row.group.licenseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AvanueTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = row.group.licenseText.trimIndent(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        color = AvanueTheme.colors.textSecondary
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = "Open Source Licenses",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AvanueTheme.colors.textPrimary
                )
                Text(
                    text = "${OssLicenseRegistry.totalLibraries} libraries",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            sectionedRows.forEach { (group, rows) ->
                item(key = "section_${group.licenseName}") {
                    Text(
                        text = group.licenseName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AvanueTheme.colors.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                }

                rows.forEachIndexed { index, row ->
                    item(key = "lib_${group.licenseName}_${row.lib.name}_$index") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRow = row }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = row.lib.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AvanueTheme.colors.textPrimary
                            )
                            Text(
                                text = "${row.holder.name}${if (row.lib.version.isNotEmpty()) " · ${row.lib.version}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AvanueTheme.colors.textSecondary
                            )
                        }
                        if (index < rows.lastIndex) {
                            HorizontalDivider(
                                color = AvanueTheme.colors.textDisabled.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AvanueTheme.colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = AvanueTheme.colors.textPrimary
        )
    }
}
