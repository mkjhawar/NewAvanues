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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetection
import com.augmentalis.devicemanager.deviceinfo.detection.SmartGlassDetection
import com.augmentalis.devicemanager.deviceinfo.detection.SmartGlassType
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme

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
                        color = AvanueTheme.colors.textSecondary
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

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            AvanueTheme.colors.background,
            AvanueTheme.colors.surface.copy(alpha = 0.6f),
            AvanueTheme.colors.background
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = AvanueTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AvanueTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchToggle) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AvanueTheme.colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
        ) {
            // Search bar (collapsible)
            if (isSearchVisible) {
                item {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = {
                            Text(
                                "Search settings...",
                                color = AvanueTheme.colors.textDisabled
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedTextColor = AvanueTheme.colors.textPrimary,
                            unfocusedTextColor = AvanueTheme.colors.textPrimary,
                            cursorColor = AvanueTheme.colors.primary,
                            focusedIndicatorColor = AvanueTheme.colors.primary,
                            unfocusedIndicatorColor = AvanueTheme.colors.textDisabled
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
                Spacer(modifier = Modifier.height(SpacingTokens.sm))
            }

            // Group 1: Core settings (Permissions, VoiceCursor, Voice Control)
            val coreProviders = filteredProviders.filter {
                it.sortOrder < 400
            }
            if (coreProviders.isNotEmpty()) {
                item {
                    Text(
                        text = "CORE",
                        style = MaterialTheme.typography.labelMedium,
                        color = AvanueTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = SpacingTokens.md,
                            vertical = SpacingTokens.xs
                        )
                    )
                }
                coreProviders.forEach { provider ->
                    item(key = "core_${provider.moduleId}") {
                        ModuleListItem(
                            provider = provider,
                            accentColor = moduleAccentColor(provider.moduleId),
                            onClick = { onModuleSelected(provider.moduleId) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(SpacingTokens.md)) }

            // Group 2: WebAvanue
            val browserProviders = filteredProviders.filter {
                it.moduleId == "webavanue"
            }
            if (browserProviders.isNotEmpty()) {
                item {
                    Text(
                        text = "BROWSER",
                        style = MaterialTheme.typography.labelMedium,
                        color = AvanueTheme.colors.info,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = SpacingTokens.md,
                            vertical = SpacingTokens.xs
                        )
                    )
                }
                browserProviders.forEach { provider ->
                    item(key = "browser_${provider.moduleId}") {
                        ModuleListItem(
                            provider = provider,
                            accentColor = moduleAccentColor(provider.moduleId),
                            onClick = { onModuleSelected(provider.moduleId) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(SpacingTokens.md)) }

            // Group 3: System
            val systemProviders = filteredProviders.filter {
                it.moduleId == "system"
            }
            if (systemProviders.isNotEmpty()) {
                item {
                    Text(
                        text = "SYSTEM",
                        style = MaterialTheme.typography.labelMedium,
                        color = AvanueTheme.colors.warning,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = SpacingTokens.md,
                            vertical = SpacingTokens.xs
                        )
                    )
                }
                systemProviders.forEach { provider ->
                    item(key = "system_${provider.moduleId}") {
                        ModuleListItem(
                            provider = provider,
                            accentColor = moduleAccentColor(provider.moduleId),
                            onClick = { onModuleSelected(provider.moduleId) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(SpacingTokens.lg)) }

            // Developer easter egg icon — 5 taps to open developer console
            item {
                var devTapCount by remember { mutableIntStateOf(0) }
                var lastDevTapTime by remember { mutableLongStateOf(0L) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SpacingTokens.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = AvanueTheme.colors.textDisabled.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                val now = System.currentTimeMillis()
                                if (now - lastDevTapTime > 2000L) {
                                    devTapCount = 1
                                } else {
                                    devTapCount++
                                }
                                lastDevTapTime = now

                                when {
                                    devTapCount >= 5 -> {
                                        devTapCount = 0
                                        onNavigateToDeveloperConsole()
                                    }
                                    devTapCount >= 3 -> {
                                        Toast.makeText(
                                            context,
                                            "${5 - devTapCount} tap(s) to developer mode",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Returns an accent color for each settings module, giving visual identity.
 */
@Composable
private fun moduleAccentColor(moduleId: String): Color {
    return when (moduleId) {
        "permissions" -> AvanueTheme.colors.warning
        "voicecursor" -> AvanueTheme.colors.success
        "voicecontrol" -> AvanueTheme.colors.primary
        "webavanue" -> AvanueTheme.colors.info
        "system" -> AvanueTheme.colors.textSecondary
        else -> AvanueTheme.colors.primary
    }
}

@Composable
private fun ModuleListItem(
    provider: ComposableSettingsProvider,
    accentColor: Color,
    onClick: () -> Unit
) {
    AvanueCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.xs)
            .heightIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.sm),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent icon in colored circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = provider.ModuleIcon(),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Name
            Text(
                text = provider.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = AvanueTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            // Forward arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open ${provider.displayName}",
                tint = AvanueTheme.colors.textDisabled,
                modifier = Modifier.size(18.dp)
            )
        }
    }
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
        containerColor = AvanueTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        provider.displayName,
                        color = AvanueTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AvanueTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AvanueTheme.colors.surface
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

// About footer removed — now a dedicated AboutScreen (see ui/about/AboutScreen.kt)
