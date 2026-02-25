package com.augmentalis.avanueui.renderer.android.mappers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.ui.core.feedback.ContextMenu
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.IconResolver
import com.augmentalis.avanueui.renderer.android.ModifierConverter

/**
 * ContextMenuMapper - Maps ContextMenu to Material3 DropdownMenu
 */
class ContextMenuMapper : ComponentMapper<ContextMenu> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ContextMenu, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var expanded by remember { mutableStateOf(true) }

            Box(modifier = modifierConverter.convert(component.modifiers)) {
                // Render anchor if provided
                component.anchor?.let { anchor ->
                    val anchorComposable = renderer.render(anchor) as @Composable () -> Unit
                    anchorComposable()
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    component.items.forEach { item ->
                        if (item.divider) {
                            HorizontalDivider()
                        } else {
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = {
                                    item.onClick?.invoke()
                                    expanded = false
                                },
                                enabled = !item.disabled,
                                leadingIcon = item.icon?.let { iconName ->
                                    {
                                        Icon(
                                            IconResolver.resolve(iconName),
                                            contentDescription = null
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
}
