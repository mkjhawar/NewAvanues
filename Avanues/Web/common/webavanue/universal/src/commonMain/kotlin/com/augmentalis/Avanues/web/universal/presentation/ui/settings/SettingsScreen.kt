package com.augmentalis.Avanues.web.universal.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.OceanTheme
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel

/**
 * Settings category for two-pane AR/XR layout
 */
enum class SettingsCategory(val title: String, val icon: ImageVector) {
    GENERAL("General", Icons.Default.Settings),
    APPEARANCE("Appearance", Icons.Default.Palette),
    PRIVACY("Privacy & Security", Icons.Default.Shield),
    ADVANCED("Advanced", Icons.Default.Build),
    XR("AR/XR", Icons.Default.ViewInAr)
}

/**
 * SettingsScreen - Main browser settings screen
 *
 * Features:
 * - Browser settings (JavaScript, cookies, etc.)
 * - Privacy settings (desktop mode, popup blocker)
 * - Default search engine
 * - Homepage
 * - Theme selection (WebAvanue branding vs AvaMagic)
 * - Clear data options
 *
 * @param viewModel SettingsViewModel for state and actions
 * @param onNavigateBack Callback to navigate back
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToXRSettings: () -> Unit = {},
    onNavigateToSitePermissions: () -> Unit = {},
    onNavigateToARPreview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    // Detect landscape orientation for AR/XR optimized layout
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // AR/XR: Track selected category for two-pane layout
    var selectedCategory by remember { mutableStateOf(SettingsCategory.GENERAL) }

    // Show success snackbar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            // Success feedback shown
            kotlinx.coroutines.delay(2000)
            viewModel.clearSaveSuccess()
        }
    }

    // AR/XR gradient background
    val arXrBackground = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF0A1628),
            Color(0xFF1A2744),
            Color(0xFF0A1628)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLandscape) arXrBackground else Brush.verticalGradient(
                colors = listOf(OceanTheme.surface, OceanTheme.background)
            ))
    ) {
        if (isLandscape) {
            // AR/XR Landscape: Two-pane layout
            LandscapeSettingsLayout(
                settings = settings,
                isLoading = isLoading,
                error = error,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                onNavigateBack = onNavigateBack,
                onNavigateToXRSettings = onNavigateToXRSettings,
                onNavigateToSitePermissions = onNavigateToSitePermissions,
                onNavigateToARPreview = onNavigateToARPreview,
                viewModel = viewModel
            )
        } else {
            // Portrait: Single column with AR/XR styling
            PortraitSettingsLayout(
                settings = settings,
                isLoading = isLoading,
                error = error,
                onNavigateBack = onNavigateBack,
                onNavigateToXRSettings = onNavigateToXRSettings,
                onNavigateToSitePermissions = onNavigateToSitePermissions,
                onNavigateToARPreview = onNavigateToARPreview,
                viewModel = viewModel
            )
        }

        // Save success indicator
        if (saveSuccess) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = OceanTheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Settings saved")
            }
        }
    }
}

/**
 * Portrait Settings Layout - Single column with AR/XR styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitSettingsLayout(
    settings: BrowserSettings?,
    isLoading: Boolean,
    error: String?,
    onNavigateBack: () -> Unit,
    onNavigateToXRSettings: () -> Unit,
    onNavigateToSitePermissions: () -> Unit,
    onNavigateToARPreview: () -> Unit,
    viewModel: SettingsViewModel
) {
    Scaffold(
        topBar = {
            // AR/XR: Glassmorphism top bar
            Surface(
                color = OceanTheme.surface.copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
                            color = OceanTheme.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(48.dp)  // AR/XR: 48dp touch target
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = OceanTheme.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { viewModel.loadSettings() }) {
                            Text("Retry")
                        }
                    }
                }

                settings != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // General section
                        generalSettingsPortraitItems(settings, viewModel)

                        // Appearance section
                        appearanceSettingsPortraitItems(settings, viewModel)

                        // Privacy section
                        privacySettingsPortraitItems(settings, viewModel, onNavigateToSitePermissions)

                        // Advanced section
                        advancedSettingsPortraitItems(settings, viewModel, onNavigateToXRSettings, onNavigateToARPreview)

                        // XR section
                        xrSettingsPortraitItems(settings, viewModel, onNavigateToXRSettings)

                        // Reset section
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        item {
                            TextButton(
                                onClick = { viewModel.resetToDefaults() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    "Reset to Defaults",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Landscape Settings Layout - Two-pane AR/XR optimized layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LandscapeSettingsLayout(
    settings: BrowserSettings?,
    isLoading: Boolean,
    error: String?,
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToXRSettings: () -> Unit,
    onNavigateToSitePermissions: () -> Unit,
    onNavigateToARPreview: () -> Unit,
    viewModel: SettingsViewModel
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Left pane: Category navigation (30% width)
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.3f),
            color = OceanTheme.surface.copy(alpha = 0.85f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
            ) {
                // Back button and title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OceanTheme.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = OceanTheme.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsCategory.entries.forEach { category ->
                        CategoryNavItem(
                            category = category,
                            isSelected = category == selectedCategory,
                            onClick = { onCategorySelected(category) }
                        )
                    }
                }
            }
        }

        // Right pane: Settings content (70% width)
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.7f)
                .padding(16.dp),
            color = OceanTheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 12.dp
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = OceanTheme.primary)
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = OceanTheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadSettings() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OceanTheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                settings != null -> {
                    CategorySettingsContent(
                        category = selectedCategory,
                        settings = settings,
                        viewModel = viewModel,
                        onNavigateToXRSettings = onNavigateToXRSettings,
                        onNavigateToSitePermissions = onNavigateToSitePermissions,
                        onNavigateToARPreview = onNavigateToARPreview
                    )
                }
            }
        }
    }
}

/**
 * Category navigation item for landscape two-pane layout
 */
@Composable
private fun CategoryNavItem(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) OceanTheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected) OceanTheme.primary else OceanTheme.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) OceanTheme.primary else OceanTheme.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = OceanTheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Settings content for selected category in landscape mode
 */
@Composable
private fun CategorySettingsContent(
    category: SettingsCategory,
    settings: BrowserSettings,
    viewModel: SettingsViewModel,
    onNavigateToXRSettings: () -> Unit,
    onNavigateToSitePermissions: () -> Unit,
    onNavigateToARPreview: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = category.title,
                style = MaterialTheme.typography.headlineSmall,
                color = OceanTheme.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        when (category) {
            SettingsCategory.GENERAL -> {
                generalSettingsLandscapeItems(settings, viewModel)
            }

            SettingsCategory.APPEARANCE -> {
                appearanceSettingsLandscapeItems(settings, viewModel)
            }

            SettingsCategory.PRIVACY -> {
                privacySettingsLandscapeItems(settings, viewModel, onNavigateToSitePermissions)
            }

            SettingsCategory.ADVANCED -> {
                advancedSettingsLandscapeItems(settings, viewModel, onNavigateToARPreview)
            }

            SettingsCategory.XR -> {
                xrSettingsLandscapeItems(settings, viewModel, onNavigateToXRSettings)
            }
        }

        // Reset button at bottom
        item {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Reset to Defaults",
                    color = OceanTheme.error
                )
            }
        }
    }
}
