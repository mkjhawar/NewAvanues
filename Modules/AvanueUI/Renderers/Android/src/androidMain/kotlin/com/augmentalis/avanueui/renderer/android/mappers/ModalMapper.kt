package com.augmentalis.avanueui.renderer.android.mappers

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.augmentalis.avanueui.ui.core.feedback.Modal
import com.augmentalis.avanueui.ui.core.feedback.ModalActionVariant
import com.augmentalis.avanueui.ui.core.feedback.ModalSize
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter

/**
 * ModalMapper - Maps Modal to Material3 Dialog
 */
class ModalMapper : ComponentMapper<Modal> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: Modal, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val widthFraction = when (component.size) {
                ModalSize.SMALL -> 0.7f
                ModalSize.MEDIUM -> 0.85f
                ModalSize.LARGE -> 0.95f
                ModalSize.FULL_WIDTH -> 1f
                ModalSize.FULL_SCREEN -> 1f
            }

            Dialog(
                onDismissRequest = {
                    if (component.dismissible) {
                        component.onDismiss?.invoke()
                    }
                }
            ) {
                Surface(
                    modifier = modifierConverter.convert(component.modifiers)
                        .fillMaxWidth(widthFraction),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Header with title and close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = component.title,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            if (component.dismissible) {
                                IconButton(onClick = { component.onDismiss?.invoke() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Content
                        component.content?.let { content ->
                            val composable = renderer.render(content) as @Composable () -> Unit
                            composable()
                        }

                        // Actions
                        if (component.actions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                component.actions.forEachIndexed { index, action ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    when (action.variant) {
                                        ModalActionVariant.TEXT -> {
                                            TextButton(onClick = action.onClick) {
                                                Text(action.label)
                                            }
                                        }
                                        ModalActionVariant.OUTLINED -> {
                                            OutlinedButton(onClick = action.onClick) {
                                                Text(action.label)
                                            }
                                        }
                                        ModalActionVariant.FILLED -> {
                                            Button(onClick = action.onClick) {
                                                Text(action.label)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
