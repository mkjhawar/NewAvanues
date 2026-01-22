package com.augmentalis.avaelements.renderer.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.augmentalis.avamagic.components.core.Alignment
import com.augmentalis.avamagic.components.core.Gradient
import com.augmentalis.avamagic.components.core.Size
import com.augmentalis.avamagic.components.core.Modifier as MagicModifier

/**
 * ModifierConverter - Converts AvaElements modifiers to Compose modifiers
 *
 * This class handles the conversion of cross-platform AvaElements modifiers
 * to Android Compose's Modifier system.
 */
class ModifierConverter {

    /**
     * Convert a list of AvaElements modifiers to a Compose Modifier chain
     */
    fun convert(modifiers: List<MagicModifier>): Modifier {
        var modifier: Modifier = Modifier

        for (mod in modifiers) {
            modifier = modifier.then(convertSingle(mod))
        }

        return modifier
    }

    /**
     * Convert a single AvaElements modifier to Compose Modifier
     */
    private fun convertSingle(modifier: MagicModifier): Modifier {
        return when (modifier) {
            is MagicModifier.Padding -> {
                Modifier.padding(
                    top = modifier.spacing.top.dp,
                    end = modifier.spacing.right.dp,
                    bottom = modifier.spacing.bottom.dp,
                    start = modifier.spacing.left.dp
                )
            }

            is MagicModifier.Background -> {
                Modifier.background(modifier.color.toComposeColor())
            }

            is MagicModifier.BackgroundGradient -> {
                Modifier.background(brush = modifier.gradient.toComposeBrush())
            }

            is MagicModifier.Border -> {
                val shape = RoundedCornerShape(
                    topStart = modifier.border.radius.topLeft.dp,
                    topEnd = modifier.border.radius.topRight.dp,
                    bottomEnd = modifier.border.radius.bottomRight.dp,
                    bottomStart = modifier.border.radius.bottomLeft.dp
                )
                Modifier.border(
                    width = modifier.border.width.dp,
                    color = modifier.border.color.toComposeColor(),
                    shape = shape
                )
            }

            is MagicModifier.CornerRadius -> {
                val shape = RoundedCornerShape(
                    topStart = modifier.radius.topLeft.dp,
                    topEnd = modifier.radius.topRight.dp,
                    bottomEnd = modifier.radius.bottomRight.dp,
                    bottomStart = modifier.radius.bottomLeft.dp
                )
                Modifier.clip(shape)
            }

            is MagicModifier.Shadow -> {
                Modifier.shadow(
                    elevation = modifier.shadow.blurRadius.dp,
                    shape = RoundedCornerShape(0.dp),
                    // Note: Compose doesn't support shadow offset like CSS
                    // For full shadow control, use graphicsLayer
                )
            }

            is MagicModifier.Opacity -> {
                Modifier.alpha(modifier.value)
            }

            is MagicModifier.Size -> {
                var mod: Modifier = Modifier
                modifier.width?.let { w ->
                    mod = when (w) {
                        is Size.Fixed -> mod.width(w.value.dp)
                        is Size.Fill -> mod.fillMaxWidth()
                        is Size.Percent -> mod.fillMaxWidth(w.value / 100f)
                        is Size.Auto -> mod
                    }
                }
                modifier.height?.let { h ->
                    mod = when (h) {
                        is Size.Fixed -> mod.height(h.value.dp)
                        is Size.Fill -> mod.fillMaxHeight()
                        is Size.Percent -> mod.fillMaxHeight(h.value / 100f)
                        is Size.Auto -> mod
                    }
                }
                mod
            }

            is MagicModifier.Clickable -> {
                Modifier.clickable { modifier.onClick() }
            }

            is MagicModifier.Hoverable -> {
                // Compose doesn't have direct hover support on mobile
                // This would need pointer input for desktop
                Modifier
            }

            is MagicModifier.Focusable -> {
                // Focusable handling would need more complex implementation
                Modifier
            }

            is MagicModifier.Animated -> {
                // Animation handling would need AnimatedVisibility or animate* APIs
                Modifier
            }

            is MagicModifier.Align -> {
                // Alignment is typically handled by parent layout
                // This would need to be processed in layout components
                Modifier
            }

            is MagicModifier.Weight -> {
                // Weight is handled by RowScope/ColumnScope
                // This would need to be processed in layout components
                Modifier
            }

            is MagicModifier.ZIndex -> {
                Modifier.zIndex(modifier.value.toFloat())
            }

            is MagicModifier.Clip -> {
                Modifier.clip(modifier.shape.toComposeShape())
            }

            is MagicModifier.Transform -> {
                when (val transform = modifier.transformation) {
                    is MagicModifier.Transformation.Rotate -> {
                        Modifier.rotate(transform.degrees)
                    }
                    is MagicModifier.Transformation.Scale -> {
                        Modifier.scale(scaleX = transform.x, scaleY = transform.y)
                    }
                    is MagicModifier.Transformation.Translate -> {
                        // Would need graphicsLayer for translate
                        Modifier
                    }
                }
            }

            is MagicModifier.FillMaxWidth -> {
                Modifier.fillMaxWidth()
            }

            is MagicModifier.FillMaxHeight -> {
                Modifier.fillMaxHeight()
            }

            is MagicModifier.FillMaxSize -> {
                Modifier.fillMaxSize()
            }
        }
    }

    /**
     * Get Compose Alignment from AvaElements Alignment
     */
    fun toComposeAlignment(alignment: Alignment): androidx.compose.ui.Alignment {
        return when (alignment) {
            Alignment.TopStart -> androidx.compose.ui.Alignment.TopStart
            Alignment.TopCenter -> androidx.compose.ui.Alignment.TopCenter
            Alignment.TopEnd -> androidx.compose.ui.Alignment.TopEnd
            Alignment.CenterStart -> androidx.compose.ui.Alignment.CenterStart
            Alignment.Center -> androidx.compose.ui.Alignment.Center
            Alignment.CenterEnd -> androidx.compose.ui.Alignment.CenterEnd
            Alignment.BottomStart -> androidx.compose.ui.Alignment.BottomStart
            Alignment.BottomCenter -> androidx.compose.ui.Alignment.BottomCenter
            Alignment.BottomEnd -> androidx.compose.ui.Alignment.BottomEnd
        }
    }

    /**
     * Get Compose Arrangement from AvaElements Arrangement
     */
    fun toComposeArrangement(arrangement: com.augmentalis.avanues.avamagic.components.core.Arrangement): androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical {
        return when (arrangement) {
            com.augmentalis.avanues.avamagic.components.core.Arrangement.Start -> androidx.compose.foundation.layout.Arrangement.Start as androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical
            com.augmentalis.avanues.avamagic.components.core.Arrangement.Center -> androidx.compose.foundation.layout.Arrangement.Center
            com.augmentalis.avanues.avamagic.components.core.Arrangement.End -> androidx.compose.foundation.layout.Arrangement.End as androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical
            com.augmentalis.avanues.avamagic.components.core.Arrangement.SpaceBetween -> androidx.compose.foundation.layout.Arrangement.SpaceBetween
            com.augmentalis.avanues.avamagic.components.core.Arrangement.SpaceAround -> androidx.compose.foundation.layout.Arrangement.SpaceAround
            com.augmentalis.avanues.avamagic.components.core.Arrangement.SpaceEvenly -> androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        }
    }
}

/**
 * Convert AvaElements Gradient to Compose Brush
 */
fun Gradient.toComposeBrush(): Brush {
    return when (this) {
        is Gradient.Linear -> {
            val stops = this.colors.map { it.position to it.color.toComposeColor() }.toTypedArray()
            Brush.linearGradient(
                colorStops = stops,
                // Angle conversion: 0 = left to right, 90 = top to bottom
                // Compose uses start/end points, would need trigonometry for accurate conversion
            )
        }
        is Gradient.Radial -> {
            val stops = this.colors.map { it.position to it.color.toComposeColor() }.toTypedArray()
            Brush.radialGradient(
                colorStops = stops,
                center = androidx.compose.ui.geometry.Offset(this.centerX, this.centerY),
                radius = this.radius
            )
        }
    }
}

/**
 * Convert AvaElements ClipShape to Compose Shape
 */
fun MagicModifier.ClipShape.toComposeShape(): Shape {
    return when (this) {
        is MagicModifier.ClipShape.Rectangle -> {
            RoundedCornerShape(
                topStart = this.radius.topLeft.dp,
                topEnd = this.radius.topRight.dp,
                bottomEnd = this.radius.bottomRight.dp,
                bottomStart = this.radius.bottomLeft.dp
            )
        }
        is MagicModifier.ClipShape.Circle -> {
            CircleShape
        }
        is MagicModifier.ClipShape.RoundedRectangle -> {
            RoundedCornerShape(this.radius.dp)
        }
    }
}
