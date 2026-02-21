package com.augmentalis.avamagic.renderer.android.mappers.input

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.augmentalis.avamagic.ui.core.form.FileUploadComponent
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.ModifierConverter
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * FileUploadMapper - Maps FileUploadComponent to file upload UI
 * Note: Actual file picking requires Activity integration
 */
class FileUploadMapper : ComponentMapper<FileUploadComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: FileUploadComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Column(modifier = modifierConverter.convert(component.modifiers)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(
                            width = 2.dp,
                            color = AvanueTheme.colors.border,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = component.enabled) {
                            // File picker would be triggered here via Activity
                            // This requires integration with ActivityResultContracts
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload",
                            modifier = Modifier.size(32.dp),
                            tint = AvanueTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = component.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AvanueTheme.colors.primary
                        )
                        if (component.accept.isNotEmpty()) {
                            Text(
                                text = component.accept.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = AvanueTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                        component.maxFileSize?.let { maxSize ->
                            val maxSizeMB = maxSize / (1024 * 1024)
                            Text(
                                text = "Max size: ${maxSizeMB}MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = AvanueTheme.colors.textSecondary
                            )
                        }
                    }
                }

                // Show selected files
                if (component.files.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    component.files.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = file.formattedSize,
                                style = MaterialTheme.typography.bodySmall,
                                color = AvanueTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
