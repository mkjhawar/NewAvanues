/**
 * LocalizationManagerActivity.kt - Main UI for Localization Manager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Comprehensive localization management interface with glassmorphism design
 */
package com.augmentalis.localizationmanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.augmentalis.localizationmanager.data.LocalizationDatabase
import com.augmentalis.localizationmanager.repository.PreferencesRepository
import com.augmentalis.localizationmanager.ui.components.MessageHandler
import com.augmentalis.localizationmanager.ui.components.MessageType
import com.augmentalis.localizationmanager.ui.components.AnimatedCurrentLanguage
import com.augmentalis.localizationmanager.ui.components.SettingsDialog

/**
 * Main Activity for Localization Manager
 */
class LocalizationManagerActivity : ComponentActivity() {
    
    // Initialize repository using SQLDelight adapter (direct access pattern)
    private val preferencesRepository by lazy {
        PreferencesRepository(LocalizationDatabase.getPreferencesDao(this))
    }
    
    private val viewModel: LocalizationViewModel by viewModels {
        LocalizationViewModelFactory(this, preferencesRepository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0E27)
                ) {
                    LocalizationManagerContent(viewModel)
                }
            }
        }
    }
}

/**
 * Main content composable
 */
@Composable
fun LocalizationManagerContent(viewModel: LocalizationViewModel) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val availableLanguages by viewModel.availableLanguages.observeAsState(emptyList())
    val downloadedLanguages by viewModel.downloadedLanguages.observeAsState(emptyList())
    val languageStatistics by viewModel.languageStatistics.observeAsState()
    val translationPairs by viewModel.translationPairs.observeAsState(emptyList())
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val recentTranslations by viewModel.recentTranslations.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val successMessage by viewModel.successMessage.observeAsState()
    
    // User preferences
    val debounceDuration by viewModel.debounceDuration.collectAsState()
    val statisticsAutoShow by viewModel.statisticsAutoShow.collectAsState()
    val languageAnimationEnabled by viewModel.languageAnimationEnabled.collectAsState()
    
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showTranslationDialog by remember { mutableStateOf(false) }
    var showStatisticsDetail by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedLanguageForAction by remember { mutableStateOf<LanguageInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            HeaderSection(
                currentLanguage = currentLanguage,
                languageAnimationEnabled = languageAnimationEnabled,
                onRefresh = { viewModel.refreshStatistics() },
                onSettings = { showSettingsDialog = true }
            )
        }
        
        // Current Language Card
        item {
            CurrentLanguageCard(
                languageCode = currentLanguage,
                languageName = getLanguageDisplayName(currentLanguage),
                onChangeLanguage = { showLanguageSelector = true },
                onTestSpeech = { viewModel.testSpeechRecognition(currentLanguage) }
            )
        }
        
        // Language Statistics
        item {
            languageStatistics?.let { stats ->
                LanguageStatisticsCard(
                    statistics = stats,
                    onViewDetails = { 
                        showStatisticsDetail = true
                    }
                )
            }
        }
        
        // Quick Actions
        item {
            QuickActionsCard(
                onTranslate = { showTranslationDialog = true },
                onDownloadLanguages = { showLanguageSelector = true },
                onManageLanguages = { },
                isLoading = isLoading
            )
        }
        
        // Downloaded Languages
        item {
            DownloadedLanguagesCard(
                languages = downloadedLanguages,
                onLanguageClick = { language ->
                    selectedLanguageForAction = language
                },
                onDeleteLanguage = { language ->
                    viewModel.deleteLanguage(language.code)
                }
            )
        }
        
        // Available Languages Grid
        item {
            AvailableLanguagesSection(
                languages = availableLanguages,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onLanguageClick = { language ->
                    if (language.isDownloaded) {
                        viewModel.changeLanguage(language.code)
                    } else {
                        viewModel.downloadLanguage(language.code)
                    }
                }
            )
        }
        
        // Recent Translations
        if (recentTranslations.isNotEmpty()) {
            item {
                RecentTranslationsCard(
                    translations = recentTranslations,
                    onTranslationClick = { }
                )
            }
        }
        
        // Translation Capabilities
        item {
            TranslationCapabilitiesCard(
                pairsCount = translationPairs.size,
                offlineCount = translationPairs.count { it.isOfflineCapable }
            )
        }
    }
    
    // Download Progress Overlay
    downloadProgress?.let { progress ->
        DownloadProgressOverlay(
            progress = progress,
            onCancel = { viewModel.cancelDownload() }
        )
    }
    
    // Language Selector Dialog
    if (showLanguageSelector) {
        LanguageSelectorDialog(
            languages = availableLanguages,
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                viewModel.changeLanguage(language.code)
                showLanguageSelector = false
            },
            onDismiss = { showLanguageSelector = false }
        )
    }
    
    // Translation Dialog
    if (showTranslationDialog) {
        TranslationDialog(
            availableLanguages = downloadedLanguages,
            onTranslate = { text, source, target ->
                viewModel.translateText(text, source, target)
                showTranslationDialog = false
            },
            onDismiss = { showTranslationDialog = false }
        )
    }
    
    // Statistics Detail Dialog
    if (showStatisticsDetail) {
        languageStatistics?.let { stats ->
            StatisticsDetailDialog(
                statistics = stats,
                onDismiss = { showStatisticsDetail = false }
            )
        }
    }
    
    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentDebounceDuration = debounceDuration,
            statisticsAutoShow = statisticsAutoShow,
            languageAnimationEnabled = languageAnimationEnabled,
            onDebounceDurationChange = { viewModel.updateDebounceDuration(it) },
            onStatisticsAutoShowChange = { viewModel.updateStatisticsAutoShow(it) },
            onLanguageAnimationChange = { viewModel.updateLanguageAnimationEnabled(it) },
            onResetPreferences = { viewModel.resetPreferences() },
            onDismiss = { showSettingsDialog = false }
        )
    }
    
    // Enhanced Error/Success Messages with user-configurable debounce
    MessageHandler(
        message = errorMessage,
        messageType = MessageType.ERROR,
        debounceDuration = debounceDuration,
        onClearMessage = { viewModel.clearError() }
    )
    
    MessageHandler(
        message = successMessage,
        messageType = MessageType.SUCCESS,
        debounceDuration = debounceDuration,
        onClearMessage = { viewModel.clearSuccess() }
    )
}

/**
 * Header section
 */
@Composable
fun HeaderSection(
    currentLanguage: String,
    languageAnimationEnabled: Boolean,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Localization Manager",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "42+ Languages • Multi-Engine Support",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedCurrentLanguage(
                currentLanguage = currentLanguage,
                animationEnabled = languageAnimationEnabled,
                fontSize = 12.sp,
                color = Color(0xFF81C784)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Current language card
 */
@Composable
fun CurrentLanguageCard(
    languageCode: String,
    languageName: String,
    onChangeLanguage: () -> Unit,
    onTestSpeech: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(LocalizationGlassConfigs.CurrentLanguage)
            .testTag("current_language_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Language",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = languageName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Code: ${languageCode.uppercase()}",
                        fontSize = 12.sp,
                        color = LocalizationColors.StatusActive
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(LocalizationColors.StatusActive.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = languageCode.uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalizationColors.StatusActive
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onChangeLanguage,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalizationColors.Primary.copy(alpha = 0.2f),
                        contentColor = LocalizationColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change")
                }
                
                Button(
                    onClick = onTestSpeech,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalizationColors.FeatureVosk.copy(alpha = 0.2f),
                        contentColor = LocalizationColors.FeatureVosk
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Speech")
                }
            }
        }
    }
}

/**
 * Language statistics card
 */
@Composable
fun LanguageStatisticsCard(
    statistics: LanguageStatistics,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(LocalizationGlassConfigs.Primary)
            .clickable { onViewDetails() }
            .testTag("statistics_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Language Statistics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Details",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Filled.Language,
                    label = "Total",
                    value = statistics.totalLanguages.toString(),
                    color = LocalizationColors.Primary
                )
                StatItem(
                    icon = Icons.Filled.Download,
                    label = "Downloaded",
                    value = statistics.downloadedLanguages.toString(),
                    color = LocalizationColors.DownloadComplete
                )
                StatItem(
                    icon = Icons.Filled.Translate,
                    label = "Translations",
                    value = statistics.totalTranslations.toString(),
                    color = LocalizationColors.FeatureTranslation
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Storage: ${formatBytes(statistics.storageUsed)}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Last sync: ${formatTime(statistics.lastSync)}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Quick actions card
 */
@Composable
fun QuickActionsCard(
    onTranslate: () -> Unit,
    onDownloadLanguages: () -> Unit,
    onManageLanguages: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(LocalizationGlassConfigs.Primary)
            .testTag("quick_actions_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "Translate",
                    icon = Icons.Filled.Translate,
                    onClick = onTranslate,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    color = LocalizationColors.FeatureTranslation
                )
                ActionButton(
                    text = "Download",
                    icon = Icons.Filled.Download,
                    onClick = onDownloadLanguages,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    color = LocalizationColors.DownloadInProgress
                )
                ActionButton(
                    text = "Manage",
                    icon = Icons.Filled.Settings,
                    onClick = onManageLanguages,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    color = LocalizationColors.Secondary
                )
            }
        }
    }
}

/**
 * Downloaded languages card
 */
@Composable
fun DownloadedLanguagesCard(
    languages: List<LanguageInfo>,
    onLanguageClick: (LanguageInfo) -> Unit,
    onDeleteLanguage: (LanguageInfo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(LocalizationGlassConfigs.Primary),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downloaded Languages",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${languages.size} installed",
                    fontSize = 14.sp,
                    color = LocalizationColors.DownloadComplete
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (languages.isEmpty()) {
                Text(
                    text = "No languages downloaded yet",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(languages) { language ->
                        DownloadedLanguageChip(
                            language = language,
                            onClick = { onLanguageClick(language) },
                            onDelete = { onDeleteLanguage(language) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Available languages section
 */
@Composable
fun AvailableLanguagesSection(
    languages: List<LanguageInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLanguageClick: (LanguageInfo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(LocalizationGlassConfigs.Primary),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Available Languages",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search languages...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Language grid
            val filteredLanguages = if (searchQuery.isEmpty()) {
                languages
            } else {
                languages.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true)
                }
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLanguages) { language ->
                    LanguageCard(
                        language = language,
                        onClick = { onLanguageClick(language) }
                    )
                }
            }
        }
    }
}

/**
 * Recent translations card
 */
@Composable
fun RecentTranslationsCard(
    translations: List<TranslationResult>,
    onTranslationClick: (TranslationResult) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(LocalizationGlassConfigs.TranslationCard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Recent Translations",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            translations.take(3).forEach { translation ->
                TranslationItem(
                    translation = translation,
                    onClick = { onTranslationClick(translation) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Translation capabilities card
 */
@Composable
fun TranslationCapabilitiesCard(
    pairsCount: Int,
    offlineCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(LocalizationGlassConfigs.FeatureCard),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = "Translation Pairs",
                    modifier = Modifier.size(32.dp),
                    tint = LocalizationColors.FeatureTranslation
                )
                Text(
                    text = "$pairsCount",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Translation Pairs",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = Color.White.copy(alpha = 0.2f)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.OfflinePin,
                    contentDescription = "Offline Capable",
                    modifier = Modifier.size(32.dp),
                    tint = LocalizationColors.StatusActive
                )
                Text(
                    text = "$offlineCount",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Offline Capable",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Helper Components

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    color: Color
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 14.sp)
    }
}

@Composable
fun DownloadedLanguageChip(
    language: LanguageInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (language.isActive) 
                LocalizationColors.StatusActive.copy(alpha = 0.2f)
            else 
                Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.code.uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (language.isActive) LocalizationColors.StatusActive else Color.White
                )
                Text(
                    text = language.name,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun LanguageCard(
    language: LanguageInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = getRegionColor(language.region).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = language.code.uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = getRegionColor(language.region)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = language.name,
                    fontSize = 11.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (language.isDownloaded) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Downloaded",
                            modifier = Modifier.size(16.dp),
                            tint = LocalizationColors.DownloadComplete
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    if (language.supportedEngines.contains("VOSK")) {
                        Text(
                            text = "V",
                            fontSize = 10.sp,
                            color = LocalizationColors.FeatureVosk,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (language.supportedEngines.contains("VIVOKA")) {
                        Text(
                            text = "VI",
                            fontSize = 10.sp,
                            color = LocalizationColors.FeatureVivoka,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (language.isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LocalizationColors.StatusActive)
                )
            }
        }
    }
}

@Composable
fun TranslationItem(
    translation: TranslationResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Text(
                        text = translation.sourceLanguage.uppercase(),
                        fontSize = 12.sp,
                        color = LocalizationColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " → ",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = translation.targetLanguage.uppercase(),
                        fontSize = 12.sp,
                        color = LocalizationColors.Secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = formatTimestamp(translation.timestamp),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = translation.originalText,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = translation.translatedText,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DownloadProgressOverlay(
    progress: DownloadProgress,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .glassMorphism(LocalizationGlassConfigs.DownloadCard),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    modifier = Modifier.size(48.dp),
                    tint = LocalizationColors.DownloadInProgress
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Downloading ${progress.languageName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when (progress.status) {
                        DownloadStatus.DOWNLOADING -> LocalizationColors.DownloadInProgress
                        DownloadStatus.COMPLETED -> LocalizationColors.DownloadComplete
                        DownloadStatus.FAILED -> LocalizationColors.DownloadFailed
                        else -> LocalizationColors.DownloadPending
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress.progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                if (progress.status == DownloadStatus.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = LocalizationColors.Error)
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectorDialog(
    languages: List<LanguageInfo>,
    currentLanguage: String,
    onLanguageSelected: (LanguageInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .glassMorphism(LocalizationGlassConfigs.Primary),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Select Language",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(languages.filter { it.isDownloaded }) { language ->
                        LanguageSelectionItem(
                            language = language,
                            isSelected = language.code == currentLanguage,
                            onClick = { onLanguageSelected(language) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun TranslationDialog(
    availableLanguages: List<LanguageInfo>,
    onTranslate: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var sourceLanguage by remember { mutableStateOf("en") }
    var targetLanguage by remember { mutableStateOf("es") }
    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism(LocalizationGlassConfigs.TranslationCard),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Translate Text",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enter text to translate") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source language selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = sourceLanguage.uppercase(),
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sourceExpanded = true },
                            label = { Text("From") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select source language",
                                    modifier = Modifier.clickable { sourceExpanded = true }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        DropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            availableLanguages.forEach { languageInfo ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = languageInfo.name,
                                                color = Color.White
                                            )
                                            Text(
                                                text = languageInfo.code.uppercase(),
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        sourceLanguage = languageInfo.code
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterVertically),
                        tint = LocalizationColors.FeatureTranslation
                    )
                    
                    // Target language selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = targetLanguage.uppercase(),
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetExpanded = true },
                            label = { Text("To") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select target language",
                                    modifier = Modifier.clickable { targetExpanded = true }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        DropdownMenu(
                            expanded = targetExpanded,
                            onDismissRequest = { targetExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            availableLanguages.forEach { languageInfo ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = languageInfo.name,
                                                color = Color.White
                                            )
                                            Text(
                                                text = languageInfo.code.uppercase(),
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        targetLanguage = languageInfo.code
                                        targetExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            if (text.isNotEmpty()) {
                                onTranslate(text, sourceLanguage, targetLanguage)
                            }
                        },
                        enabled = text.isNotEmpty()
                    ) {
                        Text("Translate")
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionItem(
    language: LanguageInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                LocalizationColors.StatusActive.copy(alpha = 0.2f)
            else 
                Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = language.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "${language.code.uppercase()} • ${language.region}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = LocalizationColors.StatusActive
                )
            }
        }
    }
}

@Composable
fun ErrorDisplay(
    message: String,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Error",
                modifier = Modifier.size(20.dp),
                tint = LocalizationColors.Error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message)
        }
    }
}

@Composable
fun SuccessDisplay(
    message: String,
    onDismiss: () -> Unit
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(20.dp),
                tint = LocalizationColors.Success
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message)
        }
    }
}

// Helper functions

fun getLanguageDisplayName(code: String): String {
    return try {
        val locale = Locale(code)
        locale.getDisplayName(Locale.getDefault())
    } catch (e: Exception) {
        code.uppercase()
    }
}

fun getRegionColor(region: String): Color {
    return when (region) {
        "Europe" -> LocalizationColors.RegionEurope
        "Asia" -> LocalizationColors.RegionAsia
        "Americas" -> LocalizationColors.RegionAmericas
        "Middle East" -> LocalizationColors.RegionMiddleEast
        "Africa" -> LocalizationColors.RegionAfrica
        "Oceania" -> LocalizationColors.RegionOceania
        "Eurasia" -> LocalizationColors.RegionEurope
        else -> LocalizationColors.Primary
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
        else -> "${diff / 86_400_000} days ago"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Statistics Detail Dialog
 */
@Composable
fun StatisticsDetailDialog(
    statistics: LanguageStatistics,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassMorphism(LocalizationGlassConfigs.Primary),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Detailed Statistics",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Detailed stats grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DetailStatCard("Total Languages", statistics.totalLanguages.toString(), Icons.Filled.Language)
                    }
                    item {
                        DetailStatCard("Downloaded", statistics.downloadedLanguages.toString(), Icons.Filled.Download)
                    }
                    item {
                        DetailStatCard("Vosk Supported", statistics.voskSupported.toString(), Icons.Filled.Cloud)
                    }
                    item {
                        DetailStatCard("Vivoka Supported", statistics.vivokaSupported.toString(), Icons.Filled.CloudOff)
                    }
                    item {
                        DetailStatCard("Active Engine", "Vivoka", Icons.Filled.PowerSettingsNew)
                    }
                    item {
                        DetailStatCard("Cache Size", "125 MB", Icons.Filled.Storage)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalizationColors.Primary
                    )
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DetailStatCard(
    label: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalizationColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

