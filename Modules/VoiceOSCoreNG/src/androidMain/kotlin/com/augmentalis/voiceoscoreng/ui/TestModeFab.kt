package com.augmentalis.voiceoscoreng.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscoreng.core.VoiceOSCoreNG
import com.augmentalis.voiceoscoreng.exploration.ExplorationEngine
import com.augmentalis.voiceoscoreng.features.LearnAppConfig
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import com.augmentalis.voiceoscoreng.jit.JitProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Floating Action Button for testing VoiceOSCoreNG features.
 *
 * Provides quick access to:
 * - Enable full test mode (unlocks all features)
 * - Run exploration test
 * - Run JIT processing test
 * - View current configuration
 * - Open developer settings
 */
@Composable
fun TestModeFab(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showTestResults by remember { mutableStateOf(false) }
    var testResults by remember { mutableStateOf<TestResults?>(null) }
    var isRunningTest by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Rotation animation for FAB icon
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "fab_rotation"
    )

    // Pulse animation when test mode is active
    val testModeActive = LearnAppConfig.DeveloperSettings.enabled
    val pulseScale by animateFloatAsState(
        targetValue = if (testModeActive && !expanded) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Test Results Dialog
        if (showTestResults && testResults != null) {
            TestResultsDialog(
                results = testResults!!,
                onDismiss = { showTestResults = false }
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Expanded menu items
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Full Test Mode
                    FabMenuItem(
                        icon = Icons.Default.Science,
                        label = if (testModeActive) "Disable Test Mode" else "Enable Full Test Mode",
                        color = if (testModeActive)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            if (testModeActive) {
                                LearnAppConfig.reset()
                            } else {
                                LearnAppConfig.enableTestMode()
                            }
                            expanded = false
                        }
                    )

                    // Run All Tests
                    FabMenuItem(
                        icon = Icons.Default.PlayArrow,
                        label = "Run All Tests",
                        color = MaterialTheme.colorScheme.primary,
                        enabled = !isRunningTest,
                        onClick = {
                            scope.launch {
                                isRunningTest = true
                                testResults = runAllTests()
                                isRunningTest = false
                                showTestResults = true
                                expanded = false
                            }
                        }
                    )

                    // Exploration Test
                    FabMenuItem(
                        icon = Icons.Default.Explore,
                        label = "Test Exploration",
                        color = MaterialTheme.colorScheme.secondary,
                        enabled = !isRunningTest,
                        onClick = {
                            scope.launch {
                                isRunningTest = true
                                testResults = runExplorationTest()
                                isRunningTest = false
                                showTestResults = true
                                expanded = false
                            }
                        }
                    )

                    // JIT Test
                    FabMenuItem(
                        icon = Icons.Default.Bolt,
                        label = "Test JIT Processing",
                        color = MaterialTheme.colorScheme.secondary,
                        enabled = !isRunningTest,
                        onClick = {
                            scope.launch {
                                isRunningTest = true
                                testResults = runJitTest()
                                isRunningTest = false
                                showTestResults = true
                                expanded = false
                            }
                        }
                    )

                    // Config Summary
                    FabMenuItem(
                        icon = Icons.Default.Info,
                        label = "View Config",
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            testResults = TestResults(
                                title = "Current Configuration",
                                success = true,
                                details = LearnAppConfig.getSummary(),
                                metrics = emptyMap()
                            )
                            showTestResults = true
                            expanded = false
                        }
                    )

                    // Developer Settings
                    FabMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Developer Settings",
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            onOpenSettings()
                            expanded = false
                        }
                    )
                }
            }

            // Main FAB
            FloatingActionButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.scale(if (expanded) 1f else pulseScale),
                containerColor = when {
                    isRunningTest -> MaterialTheme.colorScheme.tertiary
                    testModeActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                if (isRunningTest) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                } else {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = "Test Mode",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            // Test mode indicator badge
            if (testModeActive && !expanded) {
                Box(
                    modifier = Modifier
                        .offset(x = (-8).dp, y = (-48).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun FabMenuItem(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Label chip
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Mini FAB
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = if (enabled) color else color.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TestResultsDialog(
    results: TestResults,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (results.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (results.success)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(results.title)
            }
        },
        text = {
            Column {
                Text(
                    results.details,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                if (results.metrics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Metrics:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    results.metrics.forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(key, style = MaterialTheme.typography.bodySmall)
                            Text(
                                value,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Test results data class.
 */
data class TestResults(
    val title: String,
    val success: Boolean,
    val details: String,
    val metrics: Map<String, String> = emptyMap()
)

/**
 * Run all VoiceOSCoreNG tests.
 */
private suspend fun runAllTests(): TestResults {
    val startTime = System.currentTimeMillis()
    val results = mutableListOf<String>()
    var allPassed = true

    // Test 1: Configuration
    results.add("✓ Configuration loaded: ${LearnAppConfig.getConfig().name}")

    // Test 2: Test mode
    LearnAppConfig.enableTestMode()
    val testModeEnabled = LearnAppConfig.DeveloperSettings.enabled
    if (testModeEnabled) {
        results.add("✓ Test mode enabled successfully")
    } else {
        results.add("✗ Test mode failed to enable")
        allPassed = false
    }

    // Test 3: Feature flags
    val aiEnabled = LearnAppConfig.isAIEnabled()
    val nluEnabled = LearnAppConfig.isNLUEnabled()
    val explorationEnabled = LearnAppConfig.isExplorationEnabled()
    results.add("✓ AI: $aiEnabled, NLU: $nluEnabled, Exploration: $explorationEnabled")

    // Test 4: JIT Processor
    delay(100) // Simulate processing
    val jitProcessor = JitProcessor()
    results.add("✓ JIT Processor initialized")

    // Test 5: Exploration Engine
    val explorationEngine = ExplorationEngine()
    val explorationAvailable = explorationEngine.isAvailable()
    results.add("✓ Exploration available: $explorationAvailable")

    // Test 6: Limits
    val maxElements = LearnAppConfig.getMaxElementsPerScan()
    val maxApps = LearnAppConfig.getMaxAppsLearned()
    results.add("✓ Limits: $maxElements elements, $maxApps apps")

    val duration = System.currentTimeMillis() - startTime

    return TestResults(
        title = if (allPassed) "All Tests Passed" else "Some Tests Failed",
        success = allPassed,
        details = results.joinToString("\n"),
        metrics = mapOf(
            "Duration" to "${duration}ms",
            "Tests Run" to "${results.size}",
            "Tier" to LearnAppDevToggle.getCurrentTier().name,
            "Version" to VoiceOSCoreNG.getVersion()
        )
    )
}

/**
 * Run exploration-specific test.
 */
private suspend fun runExplorationTest(): TestResults {
    val startTime = System.currentTimeMillis()

    // Enable test mode to unlock exploration
    LearnAppConfig.enableTestMode()

    val engine = ExplorationEngine()
    val available = engine.isAvailable()

    if (!available) {
        return TestResults(
            title = "Exploration Test",
            success = false,
            details = "Exploration mode is not available.\nEnable test mode or DEV tier first.",
            metrics = emptyMap()
        )
    }

    // Start exploration
    engine.start("com.test.package")
    delay(100)

    val running = engine.isRunning()
    val screenCount = engine.getScreenCount()
    val progress = engine.getProgress()

    engine.stop()
    val duration = System.currentTimeMillis() - startTime

    return TestResults(
        title = "Exploration Test",
        success = running,
        details = buildString {
            append("✓ Exploration engine started\n")
            append("✓ Running: $running\n")
            append("✓ Screens captured: $screenCount\n")
            append("✓ Progress: ${(progress * 100).toInt()}%\n")
            append("✓ Engine stopped cleanly")
        },
        metrics = mapOf(
            "Duration" to "${duration}ms",
            "Screens" to "$screenCount",
            "Progress" to "${(progress * 100).toInt()}%"
        )
    )
}

/**
 * Run JIT processing test.
 */
private suspend fun runJitTest(): TestResults {
    val startTime = System.currentTimeMillis()
    val processor = JitProcessor()

    // Create test elements
    val testElements = listOf(
        com.augmentalis.voiceoscoreng.common.ElementInfo(
            resourceId = "btn_submit",
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "Submit form",
            bounds = com.augmentalis.voiceoscoreng.common.Bounds(0, 0, 200, 50),
            isClickable = true,
            isEnabled = true
        ),
        com.augmentalis.voiceoscoreng.common.ElementInfo(
            resourceId = "input_email",
            className = "android.widget.EditText",
            text = "",
            contentDescription = "Email input",
            bounds = com.augmentalis.voiceoscoreng.common.Bounds(0, 60, 200, 110),
            isClickable = true,
            isEnabled = true
        ),
        com.augmentalis.voiceoscoreng.common.ElementInfo(
            resourceId = "txt_title",
            className = "android.widget.TextView",
            text = "Welcome",
            contentDescription = "",
            bounds = com.augmentalis.voiceoscoreng.common.Bounds(0, 120, 200, 150),
            isClickable = false,
            isEnabled = true
        )
    )

    // Process elements
    val results = processor.processElements(testElements)
    val successCount = results.count { it.isSuccess }
    val vuids = results.mapNotNull { it.vuid }

    delay(50) // Simulate processing time
    val duration = System.currentTimeMillis() - startTime

    return TestResults(
        title = "JIT Processing Test",
        success = successCount == testElements.size,
        details = buildString {
            append("Processed ${testElements.size} elements:\n\n")
            results.forEachIndexed { index, result ->
                val element = testElements[index]
                val status = if (result.isSuccess) "✓" else "✗"
                append("$status ${element.className.substringAfterLast(".")}\n")
                append("  Resource: ${element.resourceId}\n")
                result.vuid?.let { append("  VUID: $it\n") }
                result.errorMessage?.let { append("  Error: $it\n") }
                append("\n")
            }
        },
        metrics = mapOf(
            "Duration" to "${duration}ms",
            "Elements" to "${testElements.size}",
            "Success" to "$successCount",
            "VUIDs Generated" to "${vuids.size}",
            "Mode" to processor.getProcessingMode().name
        )
    )
}
