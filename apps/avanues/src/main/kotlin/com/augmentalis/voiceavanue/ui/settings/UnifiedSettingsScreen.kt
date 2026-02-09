/**
 * UnifiedSettingsScreen.kt - Adaptive settings screen with module providers
 *
 * Detects display mode (STANDARD / GLASS_MONOCULAR / GLASS_BINOCULAR) at entry,
 * then renders the appropriate layout:
 *
 * STANDARD: ListDetailPaneScaffold adapting to phone/tablet/foldable/desktop.
 *   - Phone portrait: single pane stack navigation (list → detail)
 *   - Phone landscape: two-pane (40%|60%)
 *   - Tablet/desktop: two-pane always visible (30%|70%)
 *   - Foldable: splits at hinge
 *
 * GLASS_MONOCULAR: Paginated single-setting view with voice navigation.
 * GLASS_BINOCULAR: Simplified single-pane list with GlassAvanue theme.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetection
import com.augmentalis.devicemanager.deviceinfo.detection.SmartGlassDetection
import com.augmentalis.devicemanager.deviceinfo.detection.SmartGlassType
import com.augmentalis.voiceavanue.BuildConfig
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsNavigationRow

/**
 * Display mode for the settings screen, detected at entry from DeviceManager.
 */
enum class SettingsDisplayMode {
    STANDARD,         // Phone/tablet/foldable — ListDetailPaneScaffold
    GLASS_MONOCULAR,  // Single setting at a time, voice nav
    GLASS_BINOCULAR   // Simplified single pane, GlassAvanue theme
}

/**
 * Entry point that detects display mode and delegates to the right layout.
 */
@Composable
fun UnifiedSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDeveloperConsole: () -> Unit,
    viewModel: UnifiedSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val displayMode = remember {
        detectSettingsDisplayMode(context)
    }

    when (displayMode) {
        SettingsDisplayMode.STANDARD -> StandardSettingsScreen(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            onNavigateToDeveloperConsole = onNavigateToDeveloperConsole
        )
        SettingsDisplayMode.GLASS_MONOCULAR -> GlassesSettingsLayout.MonocularSettingsScreen(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack
        )
        SettingsDisplayMode.GLASS_BINOCULAR -> GlassesSettingsLayout.BinocularSettingsScreen(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            onNavigateToDeveloperConsole = onNavigateToDeveloperConsole
        )
    }
}

/**
 * Detects the appropriate display mode by querying DeviceManager.
 * Falls back to STANDARD if detection fails.
 */
private fun detectSettingsDisplayMode(context: android.content.Context): SettingsDisplayMode {
    return try {
        val smartGlassDetection = SmartGlassDetection(context)
        val glassType = smartGlassDetection.getSmartGlassType()

        if (glassType == SmartGlassType.UNKNOWN) {
            return SettingsDisplayMode.STANDARD
        }

        // Use DeviceDetection for monocular/binocular classification
        val deviceDetection = DeviceDetection(context)
        val arInfo = deviceDetection.detectARGlasses()

        when {
            arInfo?.displayInfo?.isMonocular == true -> SettingsDisplayMode.GLASS_MONOCULAR
            arInfo?.displayInfo?.isBinocular == true -> SettingsDisplayMode.GLASS_BINOCULAR
            // Fallback classification by known glass type
            glassType in setOf(
                SmartGlassType.REALWEAR,
                SmartGlassType.GOOGLE_GLASS
            ) -> SettingsDisplayMode.GLASS_MONOCULAR
            glassType in setOf(
                SmartGlassType.XREAL,
                SmartGlassType.ROKID,
                SmartGlassType.EPSON
            ) -> SettingsDisplayMode.GLASS_BINOCULAR
            else -> SettingsDisplayMode.STANDARD
        }
    } catch (_: Exception) {
        SettingsDisplayMode.STANDARD
    }
}

// =============================================================================
// STANDARD Layout — ListDetailPaneScaffold for phone/tablet/foldable
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun StandardSettingsScreen(
    viewModel: UnifiedSettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDeveloperConsole: () -> Unit
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            SettingsModuleList(
                providers = viewModel.sortedProviders,
                searchQuery = searchQuery,
                isSearchVisible = isSearchVisible,
                onSearchQueryChanged = { searchQuery = it },
                onSearchToggle = { isSearchVisible = !isSearchVisible },
                onNavigateBack = onNavigateBack,
                onNavigateToDeveloperConsole = onNavigateToDeveloperConsole,
                onModuleSelected = { moduleId ->
                    navigator.navigateTo(
                        pane = ListDetailPaneScaffoldRole.Detail,
                        content = moduleId
                    )
                }
            )
        },
        detailPane = {
            val selectedModuleId = navigator.currentDestination?.content
            if (selectedModuleId != null) {
                val provider = viewModel.providerById(selectedModuleId)
                if (provider != null) {
                    SettingsDetailPane(
                        provider = provider,
                        onNavigateBack = { navigator.navigateBack() }
                    )
                }
            } else {
                // Empty state for expanded layouts (tablet)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a module",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

// =============================================================================
// Module List Pane
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsModuleList(
    providers: List<ComposableSettingsProvider>,
    searchQuery: String,
    isSearchVisible: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDeveloperConsole: () -> Unit,
    onModuleSelected: (String) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchToggle) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar (collapsible)
            if (isSearchVisible) {
                item {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search settings...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            // Module cards — grouped by proximity
            val filteredProviders = if (searchQuery.isBlank()) {
                providers
            } else {
                providers.filter { provider ->
                    provider.displayName.contains(searchQuery, ignoreCase = true) ||
                        provider.searchableEntries.any { entry ->
                            entry.displayName.contains(searchQuery, ignoreCase = true) ||
                                entry.keywords.any { it.contains(searchQuery, ignoreCase = true) }
                        }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Group 1: Core settings (Permissions, VoiceCursor, Voice Control)
            val coreProviders = filteredProviders.filter {
                it.sortOrder < 400
            }
            if (coreProviders.isNotEmpty()) {
                item {
                    SettingsGroupCard {
                        coreProviders.forEach { provider ->
                            ModuleListItem(
                                provider = provider,
                                onClick = { onModuleSelected(provider.moduleId) }
                            )
                        }
                    }
                }
            }

            // Group 2: WebAvanue
            val browserProviders = filteredProviders.filter {
                it.moduleId == "webavanue"
            }
            if (browserProviders.isNotEmpty()) {
                item {
                    SettingsGroupCard {
                        browserProviders.forEach { provider ->
                            ModuleListItem(
                                provider = provider,
                                onClick = { onModuleSelected(provider.moduleId) }
                            )
                        }
                    }
                }
            }

            // Group 3: System
            val systemProviders = filteredProviders.filter {
                it.moduleId == "system"
            }
            if (systemProviders.isNotEmpty()) {
                item {
                    SettingsGroupCard {
                        systemProviders.forEach { provider ->
                            ModuleListItem(
                                provider = provider,
                                onClick = { onModuleSelected(provider.moduleId) }
                            )
                        }
                    }
                }
            }

            // About footer — fixed at bottom, NOT a provider
            item {
                AboutSettingsFooter(
                    onNavigateToDeveloperConsole = onNavigateToDeveloperConsole
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ModuleListItem(
    provider: ComposableSettingsProvider,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(provider.displayName) },
        leadingContent = {
            Icon(
                imageVector = provider.ModuleIcon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// =============================================================================
// Detail Pane
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDetailPane(
    provider: ComposableSettingsProvider,
    onNavigateBack: () -> Unit
) {
    val sections = provider.sections.sortedBy { it.sortOrder }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(provider.displayName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (sections.size <= 1) {
            // Single section — render directly with scroll support
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                provider.SectionContent(sections.firstOrNull()?.id ?: "")
            }
        } else {
            // Multiple sections — tabbed layout
            var selectedTabIndex by remember { mutableIntStateOf(0) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sections.forEachIndexed { index, section ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(section.title) }
                        )
                    }
                }

                provider.SectionContent(sections[selectedTabIndex].id)
            }
        }
    }
}

// =============================================================================
// About Footer (Version + Licenses — not a provider)
// =============================================================================

@Composable
private fun AboutSettingsFooter(
    onNavigateToDeveloperConsole: () -> Unit
) {
    val context = LocalContext.current
    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    SettingsGroupCard {
        SettingsNavigationRow(
            title = "Version",
            currentValue = BuildConfig.VERSION_NAME,
            icon = Icons.Default.Info,
            onClick = {
                val now = System.currentTimeMillis()
                if (now - lastTapTime > 2000L) {
                    versionTapCount = 1
                } else {
                    versionTapCount++
                }
                lastTapTime = now

                when {
                    versionTapCount >= 7 -> {
                        versionTapCount = 0
                        onNavigateToDeveloperConsole()
                    }
                    versionTapCount >= 4 -> {
                        Toast.makeText(
                            context,
                            "${7 - versionTapCount} taps to developer console",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        SettingsNavigationRow(
            title = "Licenses",
            subtitle = "Open source licenses",
            icon = Icons.Default.Description,
            onClick = { showLicensesDialog = true }
        )
    }

    if (showLicensesDialog) {
        OssLicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

/**
 * Hybrid Apple-style OSS license dialog.
 *
 * Two views:
 * 1. List view — section headers per license type, alphabetical library rows
 *    showing "Name — Author" with version. Tap any row → detail view.
 * 2. Detail view — library name, version, author, license type + full license text.
 */
@Composable
private fun OssLicensesDialog(onDismiss: () -> Unit) {
    data class LibRow(val lib: OssLibrary, val holder: OssHolder, val group: OssLicenseGroup)

    val groups = remember { OssLicenseRegistry.groups() }

    // Flatten into sorted rows per license group for Apple-style grouped list
    val sectionedRows = remember {
        groups.map { group ->
            val rows = group.holders.flatMap { holder ->
                holder.libraries.map { lib -> LibRow(lib, holder, group) }
            }.sortedBy { it.lib.name.lowercase() }
            group to rows
        }
    }

    var selectedRow by remember { mutableStateOf<LibRow?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxSize(0.88f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            AnimatedVisibility(
                visible = selectedRow == null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                // ── LIST VIEW ──
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Open Source Licenses",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${OssLicenseRegistry.totalLibraries} libraries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        sectionedRows.forEach { (group, rows) ->
                            // Section header
                            item(key = "section_${group.licenseName}") {
                                Text(
                                    text = group.licenseName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                                )
                            }

                            // Library rows
                            rows.forEachIndexed { index, row ->
                                item(key = "lib_${group.licenseName}_${row.lib.name}_$index") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedRow = row }
                                            .padding(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = row.lib.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (row.lib.version.isNotEmpty()) {
                                                Text(
                                                    text = row.lib.version,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = row.holder.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (row.lib.note.isNotEmpty()) {
                                            Text(
                                                text = row.lib.note,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                    if (index < rows.lastIndex) {
                                        androidx.compose.material3.HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }

            // ── DETAIL VIEW ──
            selectedRow?.let { row ->
                Column(modifier = Modifier.padding(20.dp)) {
                    // Back + title
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Licenses",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Library info
                    Text(
                        text = row.lib.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (row.lib.version.isNotEmpty()) {
                        Text(
                            text = "Version ${row.lib.version}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = row.holder.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (row.lib.note.isNotEmpty()) {
                        Text(
                            text = row.lib.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = row.group.licenseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Full license text
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        item {
                            Text(
                                text = row.group.licenseText.trimIndent(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = { selectedRow = null }) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }
}
