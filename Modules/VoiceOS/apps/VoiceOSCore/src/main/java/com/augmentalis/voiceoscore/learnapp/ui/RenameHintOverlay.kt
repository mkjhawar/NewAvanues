/**
 * RenameHintOverlay.kt - Contextual hint for command renaming
 * Path: Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/RenameHintOverlay.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 * Related: LearnApp-Rename-Hint-Overlay-Mockups-5081220-V1.md
 *          LearnApp-On-Demand-Command-Renaming-5081220-V2.md
 *
 * Contextual hint overlay that shows when screen has generated fallback labels.
 * Non-intrusive overlay at top of screen with 3-second auto-dismiss.
 * Only shows once per screen per session.
 *
 * Features:
 * - Detects screens with generated labels (Button 1, Tab 2, etc.)
 * - Shows Material Design 3 hint card with example command
 * - 3-second auto-dismiss with fade animation
 * - TTS announcement for accessibility
 * - Session-based tracking (doesn't repeat)
 * - High contrast mode support
 * - Small screen responsive design
 */

package com.augmentalis.voiceoscore.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.database.dto.GeneratedCommandDTO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RenameHintOverlay - Manages hint overlay lifecycle
 *
 * Shows contextual hint when screen has generated labels.
 * Uses WindowManager overlay pattern (same as ConsentDialog).
 *
 * ## Usage Example
 *
 * ```kotlin
 * val overlay = RenameHintOverlay(
 *     context = accessibilityService,
 *     tts = textToSpeech
 * )
 *
 * // When screen changes
 * overlay.showIfNeeded(
 *     packageName = "com.instagram.android",
 *     activityName = "MainActivity",
 *     generatedCommands = commands
 * )
 *
 * // Reset session (e.g., after app restart)
 * overlay.reset()
 * ```
 *
 * ## Thread Safety
 *
 * All WindowManager operations run on main thread.
 * Safe to call from any thread.
 *
 * @param context AccessibilityService context
 * @param tts TextToSpeech instance for accessibility announcements
 */
class RenameHintOverlay(
    private val context: AccessibilityService,
    private val tts: TextToSpeech?
) {
    /**
     * WindowManager for adding overlay views
     */
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Currently displayed view (if any)
     */
    private var currentView: ComposeView? = null

    /**
     * Set of screen identifiers where hint has been shown this session
     * Format: "packageName/activityName"
     */
    private val shownScreens = mutableSetOf<String>()

    /**
     * Show rename hint if screen has generated labels
     *
     * Checks if:
     * 1. Hint hasn't been shown for this screen this session
     * 2. Screen has generated labels (Button 1, Tab 2, etc.)
     *
     * If both conditions are true, shows hint overlay with example command.
     *
     * @param packageName Current app package name
     * @param activityName Current activity name
     * @param generatedCommands List of commands with potential generated labels
     */
    fun showIfNeeded(
        packageName: String,
        activityName: String,
        generatedCommands: List<GeneratedCommandDTO>
    ) {
        // Screen identifier
        val screenKey = "$packageName/$activityName"

        // Already shown this session?
        if (shownScreens.contains(screenKey)) {
            Log.d(TAG, "Hint already shown for $screenKey")
            return
        }

        // Any generated labels on this screen?
        val generatedCommand = generatedCommands.firstOrNull { command ->
            isGeneratedLabel(command.commandText)
        }

        if (generatedCommand == null) {
            Log.d(TAG, "No generated labels on $screenKey")
            return
        }

        // Show hint
        Log.i(TAG, "Showing rename hint for $screenKey (example: ${generatedCommand.commandText})")
        show(generatedCommand)
        shownScreens.add(screenKey)
    }

    /**
     * Check if command text is a generated label
     *
     * Detects patterns like:
     * - "click button 1", "click tab 2"
     * - "click top button", "click bottom card"
     * - "click top left button" (Unity 3x3 grid)
     * - "click corner top far left button" (Unreal 4x4 grid)
     *
     * @param commandText Command text to check
     * @return true if text matches generated label pattern
     */
    internal fun isGeneratedLabel(commandText: String): Boolean {
        val patterns = listOf(
            // Position-based (Button 1, Tab 2, etc.)
            Regex("click (button|tab|card|option) \\d+"),
            // Context-aware (top button, bottom card, etc.)
            Regex("click (top|bottom|center|middle) .+"),
            // Unity 3x3 grid (top left button, middle center card, etc.)
            Regex("click (top|middle|bottom) (left|center|right) .+"),
            // Unreal corner labels
            Regex("click corner .+"),
            // Unreal 4x4 grid (upper left button, lower far right card, etc.)
            Regex("click (upper|lower) .+")
        )

        return patterns.any { it.matches(commandText) }
    }

    /**
     * Show hint overlay
     *
     * Creates ComposeView with Material Design 3 card and adds to WindowManager.
     * Auto-dismisses after 3 seconds with fade animation.
     * Announces via TTS for accessibility.
     *
     * @param exampleCommand Command to use in example text
     */
    private fun show(exampleCommand: GeneratedCommandDTO) {
        // Remove any existing view first
        currentView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // View already removed, ignore
                Log.d(TAG, "Exception removing existing view: ${e.message}")
            }
        }

        // Extract button name from command
        val buttonName = extractButtonName(exampleCommand.commandText)

        // TTS announcement for accessibility
        tts?.speak(
            "Hint: You can rename buttons by saying: Rename $buttonName to Save. This message will auto-dismiss in 3 seconds.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "rename_hint"
        )

        // Create ComposeView
        val composeView = ComposeView(context).apply {
            setContent {
                RenameHintCard(
                    exampleCommand = buttonName,
                    onDismiss = { hide() }
                )
            }
        }

        // Window layout parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(16) // 16dp from top
        }

        // Add to WindowManager
        try {
            windowManager.addView(composeView, params)
            currentView = composeView
            Log.i(TAG, "Rename hint overlay displayed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add rename hint overlay", e)
        }
    }

    /**
     * Hide overlay
     *
     * Removes view from WindowManager if currently displayed.
     */
    private fun hide() {
        currentView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "Rename hint overlay removed")
            } catch (e: Exception) {
                // View already removed, ignore
                Log.d(TAG, "Exception removing overlay: ${e.message}")
            }
            currentView = null
        }
    }

    /**
     * Reset shown screens
     *
     * Clears session tracking so hints can be shown again.
     * Useful for testing or when starting new session.
     */
    fun reset() {
        shownScreens.clear()
        Log.d(TAG, "Reset shown screens")
    }

    /**
     * Extract button name from command text
     *
     * Converts "click button 1" → "Button 1"
     * Converts "click top left button" → "Top Left Button"
     *
     * @param commandText Full command text (e.g., "click button 1")
     * @return Capitalized button name (e.g., "Button 1")
     */
    private fun extractButtonName(commandText: String): String {
        return commandText
            .removePrefix("click ")
            .removePrefix("type ")
            .removePrefix("scroll ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { char -> char.uppercase() }
            }
    }

    /**
     * Convert dp to pixels
     *
     * @param dp Density-independent pixels
     * @return Pixels
     */
    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    companion object {
        private const val TAG = "RenameHintOverlay"
    }
}

/**
 * RenameHintCard - Material Design 3 hint card composable
 *
 * Shows contextual hint with example command.
 * 3-second auto-dismiss with fade animation.
 * Responsive to screen size and high contrast mode.
 *
 * ## Visual Design
 *
 * ```
 * ┌───────────────────────────────────────────────────────────┐
 * │ ℹ️  Rename buttons by saying:                             │
 * │    "Rename Button 1 to Save"                             │
 * └───────────────────────────────────────────────────────────┘
 * ```
 *
 * @param exampleCommand Button name to use in example (e.g., "Button 1")
 * @param onDismiss Callback when hint auto-dismisses
 */
@Composable
fun RenameHintCard(
    exampleCommand: String,
    onDismiss: () -> Unit = {}
) {
    // Animation state
    var visible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Configuration for responsive design
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val isHighContrast = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

    // Auto-dismiss after 3 seconds
    LaunchedEffect(Unit) {
        visible = true
        coroutineScope.launch {
            delay(3000)
            visible = false
            delay(200) // Wait for fade out animation
            onDismiss()
        }
    }

    // Animated visibility with fade
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (isSmallScreen) 0.95f else 0.9f)
                    .padding(horizontal = if (isSmallScreen) 12.dp else 16.dp)
                    .then(
                        if (isHighContrast) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Modifier
                        }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                        .copy(alpha = if (isHighContrast) 1.0f else 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(if (isSmallScreen) 12.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Info icon
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(if (isHighContrast) 28.dp else 24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Text content
                    Column {
                        // Title
                        Text(
                            text = if (isSmallScreen) "Rename:" else "Rename buttons by saying:",
                            style = if (isSmallScreen) {
                                MaterialTheme.typography.bodySmall
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = if (isHighContrast) FontWeight.Bold else FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Example command
                        Text(
                            text = "\"Rename $exampleCommand to Save\"",
                            style = if (isSmallScreen) {
                                MaterialTheme.typography.bodyMedium
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontStyle = FontStyle.Italic,
                            fontWeight = if (isHighContrast) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Preview Composables
// ============================================================================

/**
 * Preview: Light mode with standard screen size
 */
@Preview(name = "Light Mode", showBackground = true)
@Composable
fun RenameHintOverlayPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated app content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("DeviceInfo App", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Row {
                    Button(onClick = {}) { Text("Button 1") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {}) { Text("Button 2") }
                }
            }

            // Overlay
            RenameHintCard(exampleCommand = "Button 1")
        }
    }
}

/**
 * Preview: Dark mode (high contrast)
 */
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RenameHintOverlayPreviewDark() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("DeviceInfo App", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Row {
                    Button(onClick = {}) { Text("Button 1") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {}) { Text("Button 2") }
                }
            }

            RenameHintCard(exampleCommand = "Button 1")
        }
    }
}

/**
 * Preview: Small screen (compact layout)
 */
@Preview(name = "Small Screen", device = "spec:width=320dp,height=640dp,dpi=160")
@Composable
fun RenameHintOverlayPreviewSmall() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text("DeviceInfo", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = {},
                        modifier = Modifier.size(80.dp)
                    ) {
                        Text("Btn 1", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier.size(80.dp)
                    ) {
                        Text("Btn 2", fontSize = 12.sp)
                    }
                }
            }

            RenameHintCard(exampleCommand = "Button 1")
        }
    }
}

/**
 * Preview: Unity game with spatial label
 */
@Preview(name = "Unity Spatial Label", showBackground = true)
@Composable
fun RenameHintOverlayPreviewUnity() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Unity Game", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Row {
                    Button(onClick = {}) { Text("TL Btn") }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {}) { Text("TR Btn") }
                }
            }

            RenameHintCard(exampleCommand = "Top Left Button")
        }
    }
}

/**
 * Preview: RealWear Navigator 500 variant
 */
@Preview(name = "RealWear Navigator 500", showBackground = true)
@Composable
fun RenameHintOverlayPreviewRealWear() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // RealWear-style UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text("RealWear Navigator 500", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = {}) { Text("[1]") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {}) { Text("[2]") }
                }
            }

            // High contrast overlay for RealWear
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(horizontal = 8.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.Yellow,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "SAY: \"RENAME 1 TO SAVE\"",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
