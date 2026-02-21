package com.augmentalis.avamagic.renderer.android.mappers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avamagic.components.core.Position
import com.augmentalis.avamagic.components.core.Severity
import com.augmentalis.avamagic.ui.core.feedback.ToastComponent
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * ToastMapper - Maps ToastComponent to Material3 Snackbar-style toast
 */
class ToastMapper : ComponentMapper<ToastComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: ToastComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val (backgroundColor, contentColor) = when (component.severity) {
                Severity.INFO -> AvanueTheme.colors.inverseSurface to AvanueTheme.colors.inverseOnSurface
                Severity.SUCCESS -> AvanueTheme.colors.primaryContainer to AvanueTheme.colors.onPrimaryContainer
                Severity.WARNING -> AvanueTheme.colors.tertiaryContainer to AvanueTheme.colors.onTertiaryContainer
                Severity.ERROR -> AvanueTheme.colors.errorContainer to AvanueTheme.colors.onErrorContainer
                else -> AvanueTheme.colors.inverseSurface to AvanueTheme.colors.inverseOnSurface
            }

            val icon = when (component.severity) {
                Severity.INFO -> Icons.Default.Info
                Severity.SUCCESS -> Icons.Default.CheckCircle
                Severity.WARNING -> Icons.Default.Warning
                Severity.ERROR -> Icons.Default.Error
                else -> Icons.Default.Info
            }

            val alignment = when (component.position) {
                Position.TOP -> Alignment.TopCenter
                Position.BOTTOM -> Alignment.BottomCenter
                Position.CENTER -> Alignment.Center
                else -> Alignment.BottomCenter
            }

            Box(
                modifier = modifierConverter.convert(component.modifiers)
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = alignment
            ) {
                Surface(
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = component.message,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        component.action?.let { actionLabel ->
                            Spacer(Modifier.width(12.dp))
                            TextButton(onClick = { /* Action handler */ }) {
                                Text(actionLabel, color = contentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
