package com.augmentalis.avamagic.renderer.android.mappers.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.augmentalis.avamagic.ui.core.feedback.*
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter
import com.augmentalis.avanueui.theme.AvanueTheme

class BottomSheetMapper : ComponentMapper<BottomSheetComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: BottomSheetComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = component.skipPartiallyExpanded
            )

            ModalBottomSheet(
                onDismissRequest = { component.onDismiss?.invoke() },
                sheetState = sheetState,
                dragHandle = if (component.dragHandle) {
                    { BottomSheetDefaults.DragHandle() }
                } else null,
                modifier = modifierConverter.convert(component.modifiers)
            ) {
                val sheetContentComposable = renderer.render(component.sheetContent) as @Composable () -> Unit
                sheetContentComposable()
            }

            // Main content
            val contentComposable = renderer.render(component.content) as @Composable () -> Unit
            contentComposable()
        }
    }
}

class LoadingDialogMapper : ComponentMapper<LoadingDialogComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: LoadingDialogComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Dialog(
                onDismissRequest = {
                    if (component.dismissible) {
                        component.onDismiss?.invoke()
                    }
                }
            ) {
                Surface(
                    modifier = modifierConverter.convert(component.modifiers)
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = AvanueTheme.colors.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val progress = component.progress
                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        component.message?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
