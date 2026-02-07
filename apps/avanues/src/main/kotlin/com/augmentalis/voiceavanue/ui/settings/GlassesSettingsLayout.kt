/**
 * GlassesSettingsLayout.kt - Smart glasses settings layouts
 *
 * Provides two specialized layouts for AR/smart glasses:
 *
 * MONOCULAR (RealWear HMT/Navigator, Vuzix M400, Google Glass, Even G1):
 *   - Single setting per screen, extra-large text (20sp+), high contrast
 *   - Voice navigation: "next", "previous", "toggle", "back"
 *   - Pagination dots show position within section
 *   - No list-detail split — stack navigation only
 *
 * BINOCULAR (XREAL, Rokid, Virture, Almer, Epson Moverio):
 *   - Simplified single-pane scrollable list
 *   - GlassAvanue theme (transparent backgrounds, frosted glass blur)
 *   - Voice + touchpad/head-gesture navigation
 *
 * Theme: Hybrid GlassAvanue base + manufacturer accent colors.
 *
 * Voice navigation uses VoiceOSCore (NOT WearHF/WearML — those are disabled).
 * Manufacturer native commands are added as synonyms in StaticCommandRegistry.
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.devicemanager.deviceinfo.detection.SmartGlassDetection
import com.augmentalis.devicemanager.deviceinfo.detection.SmartGlassType
import com.avanueui.ColorTokens

/**
 * Smart glasses settings layout implementations.
 */
object GlassesSettingsLayout {

    // =========================================================================
    // Manufacturer Accent Colors
    // =========================================================================

    fun getManufacturerAccent(glassType: SmartGlassType): Color = when (glassType) {
        SmartGlassType.REALWEAR -> Color(0xFFFF6B00)        // RealWear orange
        SmartGlassType.VUZIX -> Color(0xFF0066CC)           // Vuzix blue
        SmartGlassType.ROKID -> Color(0xFF00BCD4)           // Rokid cyan
        SmartGlassType.EPSON -> Color(0xFF003399)           // Epson blue
        SmartGlassType.XREAL -> Color(0xFFFF0050)           // XREAL red/pink
        SmartGlassType.GOOGLE_GLASS -> Color(0xFF4285F4)    // Google blue
        SmartGlassType.MAGIC_LEAP -> Color(0xFF6C3CFF)      // Magic Leap purple
        SmartGlassType.MICROSOFT_HOLOLENS -> Color(0xFF0078D4) // MS blue
        SmartGlassType.TCL -> Color(0xFFE60012)             // TCL red
        SmartGlassType.PICO -> Color(0xFF00B4D8)            // Pico blue
        SmartGlassType.HTC -> Color(0xFF69BE28)             // HTC green
        else -> ColorTokens.Primary
    }

    // =========================================================================
    // MONOCULAR — Paginated single-setting view
    // =========================================================================

    /**
     * Monocular smart glasses settings screen.
     *
     * Shows one setting at a time with extra-large text, high contrast,
     * and voice command hints. Navigation: module list → single settings.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MonocularSettingsScreen(
        viewModel: UnifiedSettingsViewModel,
        onNavigateBack: () -> Unit
    ) {
        var selectedModuleId by remember { mutableStateOf<String?>(null) }

        if (selectedModuleId == null) {
            // Module chooser — large items, one module visible at a time
            MonocularModuleChooser(
                providers = viewModel.sortedProviders,
                onModuleSelected = { selectedModuleId = it },
                onNavigateBack = onNavigateBack
            )
        } else {
            val provider = viewModel.providerById(selectedModuleId!!)
            if (provider != null) {
                MonocularSettingsDetail(
                    provider = provider,
                    onNavigateBack = { selectedModuleId = null }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MonocularModuleChooser(
        providers: List<ComposableSettingsProvider>,
        onModuleSelected: (String) -> Unit,
        onNavigateBack: () -> Unit
    ) {
        var currentIndex by remember { mutableIntStateOf(0) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (providers.isNotEmpty()) {
                    val provider = providers[currentIndex.coerceIn(0, providers.lastIndex)]

                    // Single module card — fills most of the screen
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModuleSelected(provider.moduleId) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = provider.ModuleIcon(),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = provider.displayName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pagination dots
                    PaginationDots(
                        total = providers.size,
                        current = currentIndex
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Voice hints
                    Text(
                        text = "\"NEXT\"  \"PREVIOUS\"  \"SELECT\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MonocularSettingsDetail(
        provider: ComposableSettingsProvider,
        onNavigateBack: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            provider.displayName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            // Render the provider's section content
            // On monocular devices, provider content renders inside a LazyColumn
            // with extra padding and large text automatically from the theme
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val sectionId = provider.sections.firstOrNull()?.id ?: ""
                provider.SectionContent(sectionId)

                Spacer(modifier = Modifier.height(24.dp))

                // Voice hints at bottom
                Text(
                    text = "\"TOGGLE\"  \"NEXT\"  \"BACK\"",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }

    // =========================================================================
    // BINOCULAR — Simplified single-pane list with GlassAvanue theme
    // =========================================================================

    /**
     * Binocular smart glasses settings screen.
     *
     * Standard single-pane list navigation with GlassAvanue theme styling
     * (semi-transparent cards, frosted glass effect). Shows 3-5 settings
     * at once. Supports voice + touchpad/head-gesture navigation.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BinocularSettingsScreen(
        viewModel: UnifiedSettingsViewModel,
        onNavigateBack: () -> Unit,
        onNavigateToDeveloperConsole: () -> Unit
    ) {
        var selectedModuleId by remember { mutableStateOf<String?>(null) }

        if (selectedModuleId == null) {
            // Module list — simplified, no search
            BinocularModuleList(
                providers = viewModel.sortedProviders,
                onModuleSelected = { selectedModuleId = it },
                onNavigateBack = onNavigateBack
            )
        } else {
            val provider = viewModel.providerById(selectedModuleId!!)
            if (provider != null) {
                BinocularSettingsDetail(
                    provider = provider,
                    onNavigateBack = { selectedModuleId = null }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BinocularModuleList(
        providers: List<ComposableSettingsProvider>,
        onModuleSelected: (String) -> Unit,
        onNavigateBack: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                items(providers) { provider ->
                    // Semi-transparent glass card per module
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onModuleSelected(provider.moduleId) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    provider.displayName,
                                    fontSize = 18.sp
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = provider.ModuleIcon(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BinocularSettingsDetail(
        provider: ComposableSettingsProvider,
        onNavigateBack: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(provider.displayName) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val sectionId = provider.sections.firstOrNull()?.id ?: ""
                provider.SectionContent(sectionId)
            }
        }
    }

    // =========================================================================
    // Shared UI Components
    // =========================================================================

    @Composable
    private fun PaginationDots(total: Int, current: Int) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(total) { index ->
                val isActive = index == current
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isActive) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}
