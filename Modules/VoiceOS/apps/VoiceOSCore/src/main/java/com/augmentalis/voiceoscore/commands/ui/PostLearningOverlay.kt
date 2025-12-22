package com.augmentalis.voiceoscore.commands.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.database.dto.QualityMetricDTO
import com.augmentalis.voiceoscore.accessibility.ui.overlays.BaseOverlay
import com.augmentalis.voiceoscore.accessibility.ui.overlays.OverlayType
import com.augmentalis.voiceoscore.commands.ElementCommandManager

/**
 * Post-learning overlay that appears after app exploration.
 * Shows elements that need manual voice commands assigned.
 *
 * UI Layout:
 * - Top panel: "Voice Commands Needed - X elements"
 * - Element list: Scrollable list with element cards
 * - Bottom action bar: "Skip" and "Assign All Commands" buttons
 *
 * Integration Point:
 * Called from LearnAppIntegration.handleExplorationStateChange()
 * when exploration state is Completed.
 */
class PostLearningOverlay(
    context: Context,
    private val accessibilityService: AccessibilityService,
    private val commandManager: ElementCommandManager,
    private val speechEngineManager: com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
) : BaseOverlay(context, OverlayType.FULLSCREEN) {

    private var currentPackageName: String = ""
    private var elements = mutableStateListOf<QualityMetricDTO>()
    private var showingDialog by mutableStateOf(false)
    private var currentElement: QualityMetricDTO? = null

    /**
     * Show overlay with elements needing commands.
     *
     * @param packageName Application package name
     * @param elements List of quality metrics for elements needing commands
     */
    fun show(packageName: String, elements: List<QualityMetricDTO>) {
        this.currentPackageName = packageName
        this.elements.clear()
        this.elements.addAll(elements)
        show()
    }

    @Composable
    override fun OverlayContent() {
        // Semi-transparent background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            // Main content card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top panel
                    TopPanel(
                        elementCount = elements.size,
                        onClose = { hide() }
                    )

                    Divider()

                    // Element list
                    ElementList(
                        elements = elements,
                        onAssignCommand = { element ->
                            currentElement = element
                            showingDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Divider()

                    // Bottom action bar
                    BottomActionBar(
                        onSkip = { hide() },
                        onAssignAll = {
                            // Start with first element
                            if (elements.isNotEmpty()) {
                                currentElement = elements[0]
                                showingDialog = true
                            }
                        }
                    )
                }
            }

            // Command assignment dialog
            if (showingDialog && currentElement != null) {
                val element = currentElement!!

                CommandAssignmentDialog(
                    elementInfo = ElementInfo(
                        uuid = element.elementUuid,
                        type = "Element", // Could be extracted from metadata
                        genericLabel = "Unlabeled element",
                        appId = element.appId,
                        existingCommands = emptyList()
                    ),
                    commandManager = commandManager,
                    onDismiss = {
                        showingDialog = false
                        currentElement = null
                    },
                    onCommandSaved = { phrase ->
                        // Remove element from list
                        elements.remove(element)

                        // Show next element if in "Assign All" mode
                        if (elements.isNotEmpty()) {
                            currentElement = elements[0]
                        } else {
                            showingDialog = false
                            currentElement = null
                            hide()
                        }
                    },
                    onRecordAudio = { callback ->
                        // Use existing SpeechEngineManager to capture voice command
                        // Speech engine is already listening, just collect next command event
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            try {
                                // Collect next speech command
                                speechEngineManager.commandEvents.first { event ->
                                    // Filter: only accept commands with reasonable confidence
                                    event.confidence >= 0.6f && event.command.isNotBlank()
                                }.let { event ->
                                    callback(event.command, event.confidence.toDouble())
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("PostLearningOverlay", "Error capturing speech", e)
                                callback(null, 0.0)
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun TopPanel(
        elementCount: Int,
        onClose: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Voice Commands Needed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$elementCount elements need commands",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    @Composable
    private fun ElementList(
        elements: List<QualityMetricDTO>,
        onAssignCommand: (QualityMetricDTO) -> Unit,
        modifier: Modifier = Modifier
    ) {
        if (elements.isEmpty()) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All elements have commands!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(elements) { element ->
                    ElementCard(
                        element = element,
                        onAssignCommand = { onAssignCommand(element) }
                    )
                }
            }
        }
    }

    @Composable
    private fun ElementCard(
        element: QualityMetricDTO,
        onAssignCommand: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = getQualityColor(element.qualityScore),
                    shape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Element ${element.elementUuid.take(8)}...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QualityBadge(
                            label = "Quality: ${element.qualityScore}%",
                            color = getQualityColor(element.qualityScore)
                        )

                        MetadataBadge(
                            hasText = element.hasText,
                            hasContentDesc = element.hasContentDesc,
                            hasResourceId = element.hasResourceId
                        )
                    }
                }

                Button(
                    onClick = onAssignCommand,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assign")
                }
            }
        }
    }

    @Composable
    private fun QualityBadge(label: String, color: Color) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }

    @Composable
    private fun MetadataBadge(
        hasText: Boolean,
        hasContentDesc: Boolean,
        hasResourceId: Boolean
    ) {
        val fields = buildList {
            if (hasText) add("T")
            if (hasContentDesc) add("C")
            if (hasResourceId) add("R")
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = if (fields.isEmpty()) "No metadata" else fields.joinToString("/"),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    @Composable
    private fun BottomActionBar(
        onSkip: () -> Unit,
        onAssignAll: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }

            Button(
                onClick = onAssignAll,
                modifier = Modifier.weight(1f),
                enabled = elements.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Assign All Commands")
            }
        }
    }

    private fun getQualityColor(score: Int): Color {
        return when {
            score >= 80 -> Color(0xFF4CAF50) // Green
            score >= 60 -> Color(0xFFFFC107) // Yellow
            score >= 40 -> Color(0xFFFF9800) // Orange
            else -> Color(0xFFF44336) // Red
        }
    }
}
