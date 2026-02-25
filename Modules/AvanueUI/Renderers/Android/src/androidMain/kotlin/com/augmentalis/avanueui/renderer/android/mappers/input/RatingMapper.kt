package com.augmentalis.avanueui.renderer.android.mappers.input

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
import com.augmentalis.avanueui.ui.core.form.RatingComponent
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.ModifierConverter
import com.augmentalis.avanueui.theme.AvanueTheme

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
                            AvanueTheme.colors.primary
                        } else {
                            AvanueTheme.colors.border
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
