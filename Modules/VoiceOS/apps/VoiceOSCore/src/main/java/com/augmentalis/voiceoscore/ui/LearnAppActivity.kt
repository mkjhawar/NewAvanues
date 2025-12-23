/**
 * LearnAppActivity.kt - UI for triggering LearnApp mode
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-10
 */
package com.augmentalis.voiceoscore.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscore.scraping.AccessibilityScrapingIntegration
import com.augmentalis.voiceoscore.scraping.LearnAppResult
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.dto.ScrapedAppDTO
import com.augmentalis.voiceoscore.ui.VoiceOSTheme
import com.augmentalis.voiceoscore.ui.DepthLevel
import com.augmentalis.voiceoscore.ui.GlassMorphismConfig
import com.augmentalis.voiceoscore.ui.glassMorphism
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LearnApp Activity
 *
 * Provides UI for triggering comprehensive app learning via LearnApp mode.
 * Users can select an app and trigger full UI traversal to discover all elements.
 */
class LearnAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoiceOSTheme {
                // TODO: Inject IScrapedAppRepository from DI container or service
                // For now, this will need to be passed from the service that creates this activity
                LearnAppScreen(
                    packageManager = packageManager,
                    scrapedAppRepository = null, // Will be passed from service if available
                    scrapingIntegration = null // Will be passed from service if available
                )
            }
        }
    }
}

/**
 * Main screen for LearnApp functionality
 */
@Composable
fun LearnAppScreen(
    packageManager: PackageManager,
    scrapedAppRepository: IScrapedAppRepository?,
    scrapingIntegration: AccessibilityScrapingIntegration?
) {
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var scrapedApps by remember { mutableStateOf<List<ScrapedAppDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var learningInProgress by remember { mutableStateOf(false) }
    var currentLearningPackage by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf<LearnAppResult?>(null) }

    val scope = rememberCoroutineScope()

    // Load data on launch
    LaunchedEffect(Unit) {
        loadData(
            packageManager = packageManager,
            scrapedAppRepository = scrapedAppRepository,
            onAppsLoaded = { apps, scraped ->
                installedApps = apps
                scrapedApps = scraped
                isLoading = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        if (isLoading) {
            // Loading state
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color(0xFF4285F4))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading apps...", color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                LearnAppHeader()

                Spacer(modifier = Modifier.height(24.dp))

                // Result card (if last result exists)
                lastResult?.let { result ->
                    ResultCard(result = result)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // App list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(installedApps) { appInfo ->
                        val scrapedApp = scrapedApps.find { it.packageName == appInfo.packageName }
                        AppCard(
                            appInfo = appInfo,
                            scrapedApp = scrapedApp,
                            isLearning = learningInProgress && currentLearningPackage == appInfo.packageName,
                            onLearnClick = {
                                if (scrapingIntegration != null) {
                                    learningInProgress = true
                                    currentLearningPackage = appInfo.packageName
                                    scope.launch {
                                        val result = scrapingIntegration.learnApp(appInfo.packageName)
                                        lastResult = result
                                        learningInProgress = false
                                        currentLearningPackage = null

                                        // Reload scraped apps to update status
                                        scrapedAppRepository?.let { repo ->
                                            scrapedApps = repo.getAll()
                                        }
                                    }
                                } else {
                                    lastResult = LearnAppResult(
                                        success = false,
                                        message = "Accessibility service not available",
                                        elementsDiscovered = 0,
                                        newElements = 0,
                                        updatedElements = 0
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header section
 */
@Composable
fun LearnAppHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.8f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Learn Apps",
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF4285F4)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Learn Apps",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Comprehensive UI discovery for voice commands",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Result card showing last learning result
 */
@Composable
fun ResultCard(result: LearnAppResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = if (result.success) Color(0xFF00C853) else Color(0xFFFF5722),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (result.success) Color(0xFF00C853) else Color(0xFFFF5722),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = result.message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (result.success && result.elementsDiscovered > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discovered: ${result.elementsDiscovered} elements | New: ${result.newElements} | Updated: ${result.updatedElements}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * App card with learn button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCard(
    appInfo: AppInfo,
    scrapedApp: ScrapedAppDTO?,
    isLearning: Boolean,
    onLearnClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = if (scrapedApp?.isFullyLearned != 0L) Color(0xFF00C853) else Color(0xFF673AB7),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.4f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = appInfo.packageName,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )

                if (scrapedApp != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (scrapedApp.isFullyLearned != 0L) {
                            "✓ Fully Learned (${scrapedApp.elementCount} elements)"
                        } else {
                            "Partial (${scrapedApp.elementCount} elements)"
                        },
                        color = if (scrapedApp.isFullyLearned != 0L) Color(0xFF00C853) else Color(0xFFFFB300),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onLearnClick,
                enabled = !isLearning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4),
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLearning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Learning...")
                } else {
                    Text("Learn")
                }
            }
        }
    }
}

/**
 * Data class for app information
 */
data class AppInfo(
    val appName: String,
    val packageName: String
)

/**
 * Load installed apps and scraped app data
 */
private suspend fun loadData(
    packageManager: PackageManager,
    scrapedAppRepository: IScrapedAppRepository?,
    onAppsLoaded: (List<AppInfo>, List<ScrapedAppDTO>) -> Unit
) {
    withContext(Dispatchers.IO) {
        // Get installed user apps (exclude system apps)
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { appInfo ->
                AppInfo(
                    appName = appInfo.loadLabel(packageManager).toString(),
                    packageName = appInfo.packageName
                )
            }
            .sortedBy { it.appName }

        // Get scraped apps from database
        val scrapedApps = scrapedAppRepository?.getAll() ?: emptyList()

        withContext(Dispatchers.Main) {
            onAppsLoaded(apps, scrapedApps)
        }
    }
}
