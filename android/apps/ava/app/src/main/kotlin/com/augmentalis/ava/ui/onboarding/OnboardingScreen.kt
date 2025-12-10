// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/onboarding/OnboardingScreen.kt
// created: 2025-11-21
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Onboarding flow for AVA AI
 *
 * Multi-page onboarding with:
 * - Welcome screen
 * - Privacy policy acceptance
 * - Analytics opt-in
 * - Crash reporting opt-in
 * - Feature overview
 */
@Composable
fun OnboardingScreen(
    onComplete: (OnboardingPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val preferences = remember { mutableStateOf(OnboardingPreferences()) }

    val pages = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.Privacy,
        OnboardingPage.DataCollection,
        OnboardingPage.Features,
        OnboardingPage.GetStarted
    )

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Page indicator
            LinearProgressIndicator(
                progress = (currentPage + 1) / pages.size.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )

            // Page content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "page_transition"
                ) { page ->
                    when (pages[page]) {
                        OnboardingPage.Welcome -> WelcomePage()
                        OnboardingPage.Privacy -> PrivacyPage()
                        OnboardingPage.DataCollection -> DataCollectionPage(
                            preferences = preferences.value,
                            onPreferencesChange = { preferences.value = it }
                        )
                        OnboardingPage.Features -> FeaturesPage()
                        OnboardingPage.GetStarted -> GetStartedPage()
                    }
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button
                if (currentPage > 0) {
                    TextButton(onClick = { currentPage-- }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Next/Finish button
                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onComplete(preferences.value)
                        }
                    }
                ) {
                    Text(if (currentPage < pages.size - 1) "Next" else "Get Started")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                }
            }
        }
    }
}

/**
 * Welcome page
 */
@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiPeople,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Welcome to AVA AI",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Your intelligent voice assistant powered by on-device AI",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                FeatureItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy First",
                    description = "Your data stays on your device"
                )
                Divider(Modifier.padding(vertical = 12.dp))
                FeatureItem(
                    icon = Icons.Default.Bolt,
                    title = "Lightning Fast",
                    description = "On-device processing with no cloud delays"
                )
                Divider(Modifier.padding(vertical = 12.dp))
                FeatureItem(
                    icon = Icons.Default.School,
                    title = "Teach & Learn",
                    description = "Train AVA to understand you better"
                )
            }
        }
    }
}

/**
 * Privacy policy page
 */
@Composable
private fun PrivacyPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Your Privacy Matters",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "AVA AI is designed with privacy at its core:",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(16.dp))

        PrivacyPoint(
            icon = Icons.Default.DevicesOther,
            text = "All AI processing happens on your device - no cloud required"
        )

        PrivacyPoint(
            icon = Icons.Default.CloudOff,
            text = "Your conversations never leave your phone unless you explicitly choose cloud providers"
        )

        PrivacyPoint(
            icon = Icons.Default.Lock,
            text = "All data is encrypted and stored securely on your device"
        )

        PrivacyPoint(
            icon = Icons.Default.DeleteForever,
            text = "You can export or delete all your data at any time"
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "By continuing, you agree to our Privacy Policy and Terms of Service.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Data collection preferences page
 */
@Composable
private fun DataCollectionPage(
    preferences: OnboardingPreferences,
    onPreferencesChange: (OnboardingPreferences) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Icon(
            imageVector = Icons.Default.Analytics,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Help Us Improve",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "You're in control. Choose what data you'd like to share to help us improve AVA:",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OptInItem(
                    icon = Icons.Default.BugReport,
                    title = "Crash Reports",
                    description = "Send anonymous crash reports to help us fix bugs faster",
                    checked = preferences.crashReportingEnabled,
                    onCheckedChange = {
                        onPreferencesChange(preferences.copy(crashReportingEnabled = it))
                    }
                )

                Divider(Modifier.padding(vertical = 16.dp))

                OptInItem(
                    icon = Icons.Default.BarChart,
                    title = "Usage Analytics",
                    description = "Share anonymous usage statistics to help us understand how you use AVA",
                    checked = preferences.analyticsEnabled,
                    onCheckedChange = {
                        onPreferencesChange(preferences.copy(analyticsEnabled = it))
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Note: All data is anonymized and you can change these settings anytime in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Features overview page
 */
@Composable
private fun FeaturesPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Icon(
            imageVector = Icons.Default.Stars,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "What You Can Do",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "AVA AI brings powerful features to your fingertips:",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(24.dp))

        FeatureItem(
            icon = Icons.Default.Chat,
            title = "Smart Conversations",
            description = "Have natural conversations with AI that understands context"
        )

        Spacer(Modifier.height(16.dp))

        FeatureItem(
            icon = Icons.Default.School,
            title = "Teach AVA",
            description = "Train AVA to recognize your custom intents and commands"
        )

        Spacer(Modifier.height(16.dp))

        FeatureItem(
            icon = Icons.Default.CloudQueue,
            title = "Choose Your Model",
            description = "Use on-device AI or connect to cloud providers like GPT-4, Claude, or Gemini"
        )

        Spacer(Modifier.height(16.dp))

        FeatureItem(
            icon = Icons.Default.LibraryBooks,
            title = "Document Chat (RAG)",
            description = "Chat with your documents using retrieval-augmented generation"
        )
    }
}

/**
 * Get started page
 */
@Composable
private fun GetStartedPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Rocket,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "You're All Set!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Start chatting with AVA and discover what's possible",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Helper Composables ====================

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyPoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun OptInItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// ==================== Data Classes ====================

/**
 * Onboarding pages enum
 */
private enum class OnboardingPage {
    Welcome,
    Privacy,
    DataCollection,
    Features,
    GetStarted
}

/**
 * Onboarding preferences collected during flow
 */
data class OnboardingPreferences(
    val crashReportingEnabled: Boolean = false,  // Privacy-first: opt-in
    val analyticsEnabled: Boolean = false         // Privacy-first: opt-in
)
