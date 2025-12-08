package com.augmentalis.avaelements.renderer.android.mappers.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avanues.avamagic.ui.core.form.RatingComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter

/**
 * RatingMapper - Maps RatingComponent to star rating UI
 */
class RatingMapper : ComponentMapper<RatingComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: RatingComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Row(
                modifier = modifierConverter.convert(component.modifiers),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..component.maxRating) {
                    val icon = when {
                        i <= component.value.toInt() -> Icons.Default.Star
                        i - 0.5f <= component.value && component.allowHalf -> Icons.Default.StarHalf
                        else -> Icons.Default.StarBorder
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = "Star $i",
                        tint = if (i <= component.value) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .then(
                                if (!component.readonly) {
                                    Modifier.clickable {
                                        component.onRatingChange?.invoke(i.toFloat())
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}
