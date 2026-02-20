/**
 * CommandCreationWizard.kt - Step-by-step command creation wizard
 *
 * 6-step wizard for creating custom voice commands:
 * 1. Enter command phrases (primary + synonyms)
 * 2. Select action type
 * 3. Configure action parameters
 * 4. Set priority and namespace
 * 5. Test command
 * 6. Save command
 */

package com.augmentalis.voiceoscore.commandmanager.ui.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.voiceoscore.commandmanager.registry.ActionType

/**
 * Command creation wizard main screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CommandCreationWizard(
    viewModel: CommandEditorViewModel = viewModel(),
    onComplete: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val wizardState by viewModel.wizardState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Command - Step ${wizardState.currentStep.ordinal + 1}/6") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AvanueTheme.colors.primaryContainer,
                    titleContentColor = AvanueTheme.colors.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            WizardNavigationBar(
                currentStep = wizardState.currentStep,
                onPrevious = { viewModel.updateWizardStep(previousStep(wizardState.currentStep)) },
                onNext = { viewModel.updateWizardStep(nextStep(wizardState.currentStep)) },
                onComplete = {
                    viewModel.completeWizard()
                    onComplete()
                },
                canProceed = canProceedToNextStep(wizardState)
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = wizardState.currentStep,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { step ->
            when (step) {
                WizardStep.PHRASES -> PhrasesStep(viewModel, wizardState)
                WizardStep.ACTION_TYPE -> ActionTypeStep(viewModel, wizardState)
                WizardStep.ACTION_PARAMS -> ActionParamsStep(viewModel, wizardState)
                WizardStep.PRIORITY_NAMESPACE -> PriorityNamespaceStep(viewModel, wizardState)
                WizardStep.TEST -> TestStep(viewModel, wizardState)
                WizardStep.CONFIRM -> ConfirmStep(viewModel, wizardState)
            }
        }
    }
}

/**
 * Step 1: Enter command phrases
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhrasesStep(
    viewModel: CommandEditorViewModel,
    wizardState: WizardState
) {
    var newPhrase by remember { mutableStateOf("") }
    var commandId by remember { mutableStateOf(wizardState.commandId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Define Command Phrases",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Enter the phrases that will trigger this command. Add multiple variations to improve recognition.",
            style = MaterialTheme.typography.bodyMedium,
            color = AvanueTheme.colors.textSecondary
        )

        // Command ID
        OutlinedTextField(
            value = commandId,
            onValueChange = {
                commandId = it
                viewModel.updateWizardPhrases(wizardState.phrases.toMutableList().apply {
                    // Update command ID in viewModel (need to add this method)
                })
            },
            label = { Text("Command ID") },
            placeholder = { Text("e.g., custom_command_001") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        HorizontalDivider()

        // Add phrase input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newPhrase,
                onValueChange = { newPhrase = it },
                label = { Text("Add phrase") },
                placeholder = { Text("e.g., open settings") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Button(
                onClick = {
                    if (newPhrase.isNotBlank()) {
                        viewModel.updateWizardPhrases(wizardState.phrases + newPhrase)
                        newPhrase = ""
                    }
                },
                enabled = newPhrase.isNotBlank()
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }

        // Phrase list
        if (wizardState.phrases.isNotEmpty()) {
            Text(
                text = "Phrases (${wizardState.phrases.size})",
                style = MaterialTheme.typography.titleSmall
            )

            wizardState.phrases.forEach { phrase ->
                PhraseChip(
                    phrase = phrase,
                    onDelete = {
                        viewModel.updateWizardPhrases(wizardState.phrases - phrase)
                    }
                )
            }
        }
    }
}

/**
 * Step 2: Select action type
 */
@Composable
private fun ActionTypeStep(
    viewModel: CommandEditorViewModel,
    wizardState: WizardState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select Action Type",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Choose what this command should do when triggered.",
            style = MaterialTheme.typography.bodyMedium,
            color = AvanueTheme.colors.textSecondary
        )

        ActionType.values().forEach { actionType ->
            ActionTypeCard(
                actionType = actionType,
                isSelected = wizardState.actionType == actionType,
                onClick = { viewModel.updateWizardActionType(actionType) }
            )
        }
    }
}

/**
 * Step 3: Configure action parameters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionParamsStep(
    viewModel: CommandEditorViewModel,
    wizardState: WizardState
) {
    var paramKey by remember { mutableStateOf("") }
    var paramValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configure Parameters",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Add parameters for the ${wizardState.actionType.name} action.",
            style = MaterialTheme.typography.bodyMedium,
            color = AvanueTheme.colors.textSecondary
        )

        // Common params for action type
        when (wizardState.actionType) {
            ActionType.LAUNCH_APP -> {
                OutlinedTextField(
                    value = wizardState.actionParams["package"]?.toString() ?: "",
                    onValueChange = {
                        viewModel.updateWizardParams(wizardState.actionParams + ("package" to it))
                    },
                    label = { Text("Package Name") },
                    placeholder = { Text("e.g., com.android.calculator2") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ActionType.NAVIGATE -> {
                OutlinedTextField(
                    value = wizardState.actionParams["action"]?.toString() ?: "",
                    onValueChange = {
                        viewModel.updateWizardParams(wizardState.actionParams + ("action" to it))
                    },
                    label = { Text("Navigation Action") },
                    placeholder = { Text("e.g., back, home, next") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                // Generic param entry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = paramKey,
                        onValueChange = { paramKey = it },
                        label = { Text("Key") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = paramValue,
                        onValueChange = { paramValue = it },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (paramKey.isNotBlank()) {
                                viewModel.updateWizardParams(
                                    wizardState.actionParams + (paramKey to paramValue)
                                )
                                paramKey = ""
                                paramValue = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            }
        }

        // Show current params
        if (wizardState.actionParams.isNotEmpty()) {
            HorizontalDivider()
            Text("Parameters:", style = MaterialTheme.typography.titleSmall)

            wizardState.actionParams.forEach { (key, value) ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(key, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                value.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = AvanueTheme.colors.textSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.updateWizardParams(wizardState.actionParams - key)
                            }
                        ) {
                            Icon(Icons.Default.Delete, "Remove")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 4: Set priority and namespace
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityNamespaceStep(
    viewModel: CommandEditorViewModel,
    wizardState: WizardState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Priority & Namespace",
            style = MaterialTheme.typography.headlineSmall
        )

        // Priority slider
        Text("Priority: ${wizardState.priority}")
        Slider(
            value = wizardState.priority.toFloat(),
            onValueChange = { viewModel.updateWizardPriority(it.toInt()) },
            valueRange = 1f..100f,
            steps = 99
        )
        Text(
            text = "Higher priority commands are matched first when conflicts occur.",
            style = MaterialTheme.typography.bodySmall,
            color = AvanueTheme.colors.textSecondary
        )

        HorizontalDivider()

        // Namespace
        OutlinedTextField(
            value = wizardState.namespace,
            onValueChange = { viewModel.updateWizardNamespace(it) },
            label = { Text("Namespace") },
            placeholder = { Text("e.g., navigation, system, custom") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Namespace helps organize commands by module or category.",
            style = MaterialTheme.typography.bodySmall,
            color = AvanueTheme.colors.textSecondary
        )
    }
}

/**
 * Step 5: Test command
 */
@Composable
private fun TestStep(
    viewModel: CommandEditorViewModel,
    @Suppress("UNUSED_PARAMETER") wizardState: WizardState
) {
    var testPhrase by remember { mutableStateOf("") }
    var testResults by remember { mutableStateOf<List<com.augmentalis.voiceoscore.commandmanager.registry.VoiceCommand>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Test Command",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Test how well your command phrases match.",
            style = MaterialTheme.typography.bodyMedium,
            color = AvanueTheme.colors.textSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = testPhrase,
                onValueChange = { testPhrase = it },
                label = { Text("Test phrase") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    testResults = viewModel.testCommand(testPhrase)
                }
            ) {
                Text("Test")
            }
        }

        if (testResults.isNotEmpty()) {
            Text("Matching commands:", style = MaterialTheme.typography.titleSmall)
            testResults.forEach { match ->
                Text("• ${match.id} (priority: ${match.priority})")
            }
        }
    }
}

/**
 * Step 6: Confirm and save
 */
@Composable
private fun ConfirmStep(
    @Suppress("UNUSED_PARAMETER") viewModel: CommandEditorViewModel,
    wizardState: WizardState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Confirm Command",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Review your command before saving.",
            style = MaterialTheme.typography.bodyMedium,
            color = AvanueTheme.colors.textSecondary
        )

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AvanueTheme.colors.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryRow("ID", wizardState.commandId)
                SummaryRow("Phrases", wizardState.phrases.joinToString(", "))
                SummaryRow("Action Type", wizardState.actionType.name)
                SummaryRow("Priority", wizardState.priority.toString())
                SummaryRow("Namespace", wizardState.namespace)

                if (wizardState.actionParams.isNotEmpty()) {
                    Text("Parameters:", style = MaterialTheme.typography.titleSmall)
                    wizardState.actionParams.forEach { (key, value) ->
                        Text("  • $key: $value", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// Helper composables

@Composable
private fun PhraseChip(phrase: String, onDelete: () -> Unit) {
    AssistChip(
        onClick = { },
        label = { Text(phrase) },
        trailingIcon = {
            IconButton(onClick = onDelete, modifier = Modifier.size(18.dp)) {
                Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(14.dp))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionTypeCard(
    actionType: ActionType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                AvanueTheme.colors.primaryContainer
            else
                AvanueTheme.colors.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(actionType.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    getActionTypeDescription(actionType),
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WizardNavigationBar(
    currentStep: WizardStep,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    canProceed: Boolean
) {
    Surface(
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentStep != WizardStep.PHRASES
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Spacer(Modifier.width(4.dp))
                Text("Previous")
            }

            if (currentStep == WizardStep.CONFIRM) {
                Button(onClick = onComplete, enabled = canProceed) {
                    Text("Save Command")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Check, null)
                }
            } else {
                Button(onClick = onNext, enabled = canProceed) {
                    Text("Next")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}

// Helper functions

private fun previousStep(current: WizardStep): WizardStep {
    val steps = WizardStep.values()
    val index = current.ordinal
    return if (index > 0) steps[index - 1] else current
}

private fun nextStep(current: WizardStep): WizardStep {
    val steps = WizardStep.values()
    val index = current.ordinal
    return if (index < steps.size - 1) steps[index + 1] else current
}

private fun canProceedToNextStep(state: WizardState): Boolean {
    return when (state.currentStep) {
        WizardStep.PHRASES -> state.phrases.isNotEmpty() && state.commandId.isNotBlank()
        WizardStep.ACTION_TYPE -> true
        WizardStep.ACTION_PARAMS -> true
        WizardStep.PRIORITY_NAMESPACE -> state.namespace.isNotBlank()
        WizardStep.TEST -> true
        WizardStep.CONFIRM -> true
    }
}

private fun getActionTypeDescription(actionType: ActionType): String {
    return when (actionType) {
        ActionType.LAUNCH_APP -> "Launch an application"
        ActionType.NAVIGATE -> "Navigate to a screen or location"
        ActionType.SYSTEM_COMMAND -> "System-level command"
        ActionType.CUSTOM_ACTION -> "Custom action"
        ActionType.TEXT_EDITING -> "Text editing operation"
        ActionType.MEDIA_CONTROL -> "Media playback control"
        ActionType.ACCESSIBILITY -> "Accessibility feature"
    }
}
