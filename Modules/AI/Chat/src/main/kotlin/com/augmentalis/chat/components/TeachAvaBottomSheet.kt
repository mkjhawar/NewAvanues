package com.augmentalis.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.augmentalis.chat.data.BuiltInIntents

/**
 * Bottom sheet component for teaching AVA new intent-utterance mappings.
 *
 * Appears when user taps "Teach AVA" button on low-confidence messages or
 * long-presses any message to access the context menu. Allows users to:
 * 1. See the original user utterance (read-only)
 * 2. Select an existing intent (built-in or user-taught)
 * 3. Create a new custom intent name
 * 4. Submit the training example to improve AVA's understanding
 *
 * Phase 3 Implementation (Tasks P3T01, P3T02):
 * - Material 3 ModalBottomSheet with drag handle
 * - ExposedDropdownMenuBox for intent selection
 * - TextField for custom intent creation
 * - Validation: Disables submit if no intent selected or custom intent blank
 * - Accessibility: WCAG AA compliant (48dp touch targets, semantic labels)
 *
 * Design specifications:
 * - Max height: 90% screen (prevents fullscreen takeover)
 * - Sections: Header → Utterance Display → Intent Selector → Submit
 * - Color scheme: Matches MessageBubble confidence badge system
 * - Animation: Smooth expansion/collapse with Material motion
 *
 * @param show Whether the bottom sheet is visible
 * @param onDismiss Callback when user dismisses the sheet (swipe down, scrim tap, close button)
 * @param messageId ID of the message being taught (for database TrainExample)
 * @param userUtterance Original user text to be taught
 * @param suggestedIntent AVA's classified intent (may be incorrect, nullable)
 * @param existingIntents List of user-taught intents (loaded from TrainExample table)
 * @param onSubmit Callback when user submits (utterance: String, intent: String) -> Unit
 * @param modifier Optional modifier for this composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachAvaBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    messageId: String,
    userUtterance: String,
    suggestedIntent: String?,
    existingIntents: List<String>,
    onSubmit: (utterance: String, intent: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // ==================== State ====================

    /**
     * Currently selected intent (from dropdown or custom)
     */
    var selectedIntent by remember(messageId) { mutableStateOf(suggestedIntent ?: "") }

    /**
     * Whether "Create new intent" option is selected
     */
    var isCreatingNewIntent by remember(messageId) { mutableStateOf(false) }

    /**
     * Custom intent name (when creating new)
     */
    var customIntentName by remember(messageId) { mutableStateOf("") }

    /**
     * Dropdown expanded state
     */
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Focus management
    val focusManager = LocalFocusManager.current
    val customIntentFocusRequester = remember { FocusRequester() }

    // ==================== Helper Functions ====================

    /**
     * Validation: Check if submit button should be enabled
     */
    val isSubmitEnabled = when {
        isCreatingNewIntent -> customIntentName.isNotBlank()
        else -> selectedIntent.isNotBlank() && selectedIntent != CREATE_NEW_INTENT_OPTION
    }

    /**
     * Get final intent name for submission
     */
    val finalIntent = if (isCreatingNewIntent) customIntentName else selectedIntent

    /**
     * Combined intent list: Built-in + User-taught + "Create new"
     */
    val allIntents = remember(existingIntents) {
        buildList {
            // Built-in intents (with display labels)
            addAll(BuiltInIntents.ALL_INTENTS.filter { it != BuiltInIntents.UNKNOWN })
            // User-taught intents (deduplicate with built-in)
            addAll(existingIntents.filter { it !in BuiltInIntents.ALL_INTENTS })
            // Add "Create new intent" option at the end
            add(CREATE_NEW_INTENT_OPTION)
        }
    }

    // ==================== Bottom Sheet ====================

    // Ocean Glass Design v2.3 - Solid colors to prevent transparency stacking
    val OceanSolidBackground = Color(0xFF1E293B)  // Solid slate instead of transparent glass
    val OceanDarker = Color(0xFF0F172A)
    val CoralBlue = Color(0xFF3B82F6)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)

    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle(color = textSecondary) },
            containerColor = OceanSolidBackground,
            contentColor = textPrimary,
            scrimColor = Color.Black.copy(alpha = 0.6f), // More opaque scrim
            modifier = modifier.semantics { contentDescription = "Teach AVA bottom sheet" }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp) // Prevent fullscreen takeover
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp) // Extra bottom padding for system insets
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==================== Header ====================

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Teach AVA",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp) // WCAG AA minimum touch target
                            .semantics { contentDescription = "Close teach AVA sheet" }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = textSecondary
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                // ==================== Section 1: User Utterance Display ====================

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "What you said:",
                        style = MaterialTheme.typography.labelLarge,
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Highlighted utterance display (read-only) - Ocean Glass style
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OceanDarker,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "User utterance: $userUtterance" }
                    ) {
                        Text(
                            text = userUtterance,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CoralBlue,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ==================== Section 2: Intent Selector ====================

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "What did you mean?",
                        style = MaterialTheme.typography.labelLarge,
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Intent dropdown - Ocean Glass styled
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Intent selector dropdown" }
                    ) {
                        OutlinedTextField(
                            value = when {
                                isCreatingNewIntent -> CREATE_NEW_INTENT_OPTION
                                selectedIntent.isNotBlank() -> BuiltInIntents.getDisplayLabel(selectedIntent)
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select intent", color = textSecondary) },
                            placeholder = { Text("Choose an intent...", color = textSecondary) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.ExpandMore,
                                    contentDescription = if (dropdownExpanded) "Collapse" else "Expand",
                                    tint = textSecondary
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = CoralBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = CoralBlue,
                                unfocusedLabelColor = textSecondary,
                                cursorColor = CoralBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )

                        // Dropdown menu with Ocean Glass colors
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .background(OceanDarker)
                                .heightIn(max = 300.dp) // Limit dropdown height
                        ) {
                            allIntents.forEach { intent ->
                                val isCreateNew = intent == CREATE_NEW_INTENT_OPTION

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Show "+" icon for create new option
                                            if (isCreateNew) {
                                                Icon(
                                                    imageVector = Icons.Filled.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = CoralBlue
                                                )
                                            }

                                            Text(
                                                text = if (isCreateNew) {
                                                    intent
                                                } else {
                                                    BuiltInIntents.getDisplayLabel(intent)
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isCreateNew) {
                                                    FontWeight.SemiBold
                                                } else {
                                                    FontWeight.Normal
                                                },
                                                color = if (isCreateNew) CoralBlue else textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (isCreateNew) {
                                            isCreatingNewIntent = true
                                            selectedIntent = ""
                                            customIntentName = ""
                                            // Request focus on custom intent field after dropdown closes
                                            dropdownExpanded = false
                                        } else {
                                            selectedIntent = intent
                                            isCreatingNewIntent = false
                                            customIntentName = ""
                                            dropdownExpanded = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp) // WCAG AA minimum touch target
                                        .background(OceanDarker),
                                    colors = MenuDefaults.itemColors(
                                        textColor = textPrimary,
                                        leadingIconColor = textSecondary,
                                        trailingIconColor = textSecondary
                                    )
                                )
                            }
                        }
                    }

                    // Custom intent text field (animated visibility) - Ocean Glass styled
                    AnimatedVisibility(
                        visible = isCreatingNewIntent,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customIntentName,
                                onValueChange = { customIntentName = it },
                                label = { Text("Custom intent name", color = textSecondary) },
                                placeholder = { Text("e.g., play_music, order_pizza", color = textSecondary) },
                                supportingText = {
                                    Text(
                                        text = "Use lowercase with underscores (e.g., my_custom_intent)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textSecondary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (isSubmitEnabled) {
                                            onSubmit(userUtterance, finalIntent)
                                            onDismiss()
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = CoralBlue,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedLabelColor = CoralBlue,
                                    unfocusedLabelColor = textSecondary,
                                    cursorColor = CoralBlue
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(customIntentFocusRequester)
                                    .semantics { contentDescription = "Custom intent name text field" }
                            )

                            // Cancel custom intent creation
                            TextButton(
                                onClick = {
                                    isCreatingNewIntent = false
                                    customIntentName = ""
                                    selectedIntent = ""
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Cancel", color = textSecondary)
                            }
                        }
                    }

                    // Suggestion hint if suggested intent exists - Ocean Glass style
                    if (!isCreatingNewIntent && suggestedIntent != null &&
                        suggestedIntent != BuiltInIntents.UNKNOWN) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OceanDarker,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Suggestion: AVA thought this was \"${BuiltInIntents.getDisplayLabel(suggestedIntent)}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // ==================== Section 3: Submit Button ====================

                Button(
                    onClick = {
                        if (isSubmitEnabled) {
                            onSubmit(userUtterance, finalIntent)
                            onDismiss()
                        }
                    },
                    enabled = isSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp) // WCAG AA minimum touch target
                        .semantics {
                            contentDescription = if (isSubmitEnabled) {
                                "Teach AVA button, enabled"
                            } else {
                                "Teach AVA button, disabled. Select an intent first"
                            }
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.1f),
                        disabledContentColor = Color.White.copy(alpha = 0.38f)
                    )
                ) {
                    Text(
                        text = "Teach AVA",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Helper text
                Text(
                    text = "AVA will learn from this example and improve her responses over time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Constant for "Create new intent" dropdown option
 */
private const val CREATE_NEW_INTENT_OPTION = "+ Create new intent"

// ==================== Previews ====================

@Preview(name = "Teach AVA - With Suggested Intent", showBackground = true)
@Composable
private fun TeachAvaBottomSheetPreview() {
    MaterialTheme {
        TeachAvaBottomSheet(
            show = true,
            onDismiss = {},
            messageId = "msg_123",
            userUtterance = "Turn on the lights in the living room",
            suggestedIntent = BuiltInIntents.CONTROL_LIGHTS,
            existingIntents = listOf(
                "play_music",
                "order_pizza",
                "call_contact"
            ),
            onSubmit = { _, _ -> }
        )
    }
}

@Preview(name = "Teach AVA - Unknown Intent", showBackground = true)
@Composable
private fun TeachAvaBottomSheetUnknownPreview() {
    MaterialTheme {
        TeachAvaBottomSheet(
            show = true,
            onDismiss = {},
            messageId = "msg_456",
            userUtterance = "Make me a sandwich",
            suggestedIntent = BuiltInIntents.UNKNOWN,
            existingIntents = listOf(
                "play_music",
                "order_food"
            ),
            onSubmit = { _, _ -> }
        )
    }
}

@Preview(name = "Teach AVA - No Existing Intents", showBackground = true)
@Composable
private fun TeachAvaBottomSheetEmptyIntentsPreview() {
    MaterialTheme {
        TeachAvaBottomSheet(
            show = true,
            onDismiss = {},
            messageId = "msg_789",
            userUtterance = "What's the weather like?",
            suggestedIntent = BuiltInIntents.CHECK_WEATHER,
            existingIntents = emptyList(),
            onSubmit = { _, _ -> }
        )
    }
}

@Preview(
    name = "Teach AVA - Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun TeachAvaBottomSheetDarkPreview() {
    MaterialTheme {
        TeachAvaBottomSheet(
            show = true,
            onDismiss = {},
            messageId = "msg_dark",
            userUtterance = "Turn on the lights in the living room",
            suggestedIntent = BuiltInIntents.CONTROL_LIGHTS,
            existingIntents = listOf(
                "play_music",
                "order_pizza",
                "call_contact"
            ),
            onSubmit = { _, _ -> }
        )
    }
}

@Preview(name = "Teach AVA - Long Utterance", showBackground = true)
@Composable
private fun TeachAvaBottomSheetLongUtterancePreview() {
    MaterialTheme {
        TeachAvaBottomSheet(
            show = true,
            onDismiss = {},
            messageId = "msg_long",
            userUtterance = "Hey AVA, can you please turn on all the lights in the living room and also set the temperature to 72 degrees because it's getting cold in here?",
            suggestedIntent = null,
            existingIntents = listOf("play_music"),
            onSubmit = { _, _ -> }
        )
    }
}
