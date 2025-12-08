package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.avanues.avamagic.ui.core.navigation.BreadcrumbComponent
import com.augmentalis.avanues.avamagic.ui.core.navigation.BreadcrumbItem
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * BreadcrumbMapper - Maps BreadcrumbComponent to Compose UI
 *
 * Displays a navigation trail with clickable items and separators.
 */
class BreadcrumbMapper : ComponentMapper<BreadcrumbComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: BreadcrumbComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifierConverter.convert(component.modifiers)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                component.items.forEachIndexed { index, item ->
                    val isLast = index == component.items.size - 1

                    BreadcrumbItemView(
                        item = item,
                        isLast = isLast,
                        separator = component.separator
                    )
                }
            }
        }
    }

    @Composable
    private fun BreadcrumbItemView(
        item: BreadcrumbItem,
        isLast: Boolean,
        separator: String
    ) {
        Text(
            text = item.label,
            style = if (isLast) {
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .then(
                    if (!isLast && item.onClick != null) {
                        Modifier.clickable { item.onClick?.invoke() }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 4.dp)
        )

        if (!isLast) {
            Text(
                text = separator,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
