package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.augmentalis.avaelements.flutter.animation.*
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Size
import kotlin.math.roundToInt

/**
 * Android Compose mappers for Flutter Animation parity components
 *
 * This file contains renderer functions that map cross-platform animation component models
 * to Jetpack Compose animation implementations on Android.
 *
 * Performance targets:
 * - All animations run at 60 FPS minimum
 * - GPU-accelerated where possible
 * - Minimal layout recomposition
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render AnimatedContainer using Jetpack Compose animations
 *
 * Maps AnimatedContainer to Compose with animated:
 * - Size (width, height)
 * - Padding
 * - Background color
 * - Border decoration
 * - Shadows
 * - Margin
 *
 * Performance: 60 FPS using animate*AsState
 *
 * @param component AnimatedContainer component to render
 * @param content Child content renderer
 */
@Composable
fun AnimatedContainerMapper(
    component: AnimatedContainer,
    content: @Composable () -> Unit
) {
    // Animate dimensions
    val width by animateDpAsState(
        targetValue = component.width?.toDp() ?: Dp.Unspecified,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        finishedListener = { component.onEnd?.invoke() },
        label = "container_width"
    )

    val height by animateDpAsState(
        targetValue = component.height?.toDp() ?: Dp.Unspecified,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "container_height"
    )

    // Animate color
    val backgroundColor by animateColorAsState(
        targetValue = component.color?.toComposeColor() ?: Color.Transparent,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "container_color"
    )

    // Animate padding
    val padding = component.padding?.let { p ->
        PaddingValues(
            start = animateDpAsState(
                targetValue = p.left.dp,
                animationSpec = component.duration.toAnimationSpec(component.curve),
                label = "padding_start"
            ).value,
            top = animateDpAsState(
                targetValue = p.top.dp,
                animationSpec = component.duration.toAnimationSpec(component.curve),
                label = "padding_top"
            ).value,
            end = animateDpAsState(
                targetValue = p.right.dp,
                animationSpec = component.duration.toAnimationSpec(component.curve),
                label = "padding_end"
            ).value,
            bottom = animateDpAsState(
                targetValue = p.bottom.dp,
                animationSpec = component.duration.toAnimationSpec(component.curve),
                label = "padding_bottom"
            ).value
        )
    } ?: PaddingValues(0.dp)

    // Animate margin
    val margin = component.margin?.let { m ->
        PaddingValues(
            start = animateDpAsState(
                targetValue = m.left.dp,
                animationSpec = component.duration.toAnimationSpec(component.curve),
                label = "margin_start"
            ).value,
            top = animateDpAsState(
                targetValue = m.top.dp,
                animationSpec = component.duration.toAnimationSpec(component.curve),
                label = "margin_top"
            ).value,
            end = animateDpAsState(
                targetValue = m.right.dp,
                animationSpec = component.duration.toAnimationSpec(component.curve),
                label = "margin_end"
            ).value,
            bottom = animateDpAsState(
                targetValue = m.bottom.dp,
                animationSpec = component.duration.toAnimationSpec(component.curve),
                label = "margin_bottom"
            ).value
        )
    } ?: PaddingValues(0.dp)

    val decoration = component.decoration
    val shape = decoration?.let { it.toComposeShape() } ?: RectangleShape

    Box(
        modifier = Modifier
            .padding(margin)
            .then(
                if (width != Dp.Unspecified && height != Dp.Unspecified) {
                    Modifier.size(width, height)
                } else if (width != Dp.Unspecified) {
                    Modifier.width(width)
                } else if (height != Dp.Unspecified) {
                    Modifier.height(height)
                } else {
                    Modifier
                }
            )
            .then(
                decoration?.borderRadius?.let {
                    Modifier.clip(shape)
                } ?: Modifier
            )
            .background(backgroundColor, shape)
            .then(
                decoration?.border?.let { border ->
                    border.toBorderModifier(shape)
                } ?: Modifier
            )
            .padding(padding),
        contentAlignment = component.alignment?.toComposeAlignment() ?: Alignment.TopStart
    ) {
        content()
    }
}

/**
 * Render AnimatedOpacity using Jetpack Compose animations
 *
 * Maps AnimatedOpacity to Compose graphicsLayer alpha animation.
 * GPU-accelerated, no layout recomposition.
 *
 * Performance: 60 FPS using animateFloatAsState
 *
 * @param component AnimatedOpacity component to render
 * @param content Child content renderer
 */
@Composable
fun AnimatedOpacityMapper(
    component: AnimatedOpacity,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = component.opacity,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        finishedListener = { component.onEnd?.invoke() },
        label = "opacity"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
        }
    ) {
        content()
    }
}

/**
 * Render AnimatedPositioned using Jetpack Compose animations
 *
 * Maps AnimatedPositioned to animated offset within Stack/Box.
 * Must be used within a Box with relative positioning.
 *
 * Performance: 60 FPS using animateDpAsState for positions
 *
 * @param component AnimatedPositioned component to render
 * @param content Child content renderer
 */
@Composable
fun AnimatedPositionedMapper(
    component: AnimatedPositioned,
    content: @Composable () -> Unit
) {
    val left by animateDpAsState(
        targetValue = component.left?.toDp() ?: Dp.Unspecified,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        finishedListener = { component.onEnd?.invoke() },
        label = "positioned_left"
    )

    val top by animateDpAsState(
        targetValue = component.top?.toDp() ?: Dp.Unspecified,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "positioned_top"
    )

    val right by animateDpAsState(
        targetValue = component.right?.toDp() ?: Dp.Unspecified,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "positioned_right"
    )

    val bottom by animateDpAsState(
        targetValue = component.bottom?.toDp() ?: Dp.Unspecified,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "positioned_bottom"
    )

    val width by animateDpAsState(
        targetValue = component.width?.toDp() ?: Dp.Unspecified,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "positioned_width"
    )

    val height by animateDpAsState(
        targetValue = component.height?.toDp() ?: Dp.Unspecified,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "positioned_height"
    )

    // Use Layout to position child
    Layout(
        content = { content() },
        modifier = Modifier
    ) { measurables, constraints ->
        val placeable = measurables.firstOrNull()?.measure(
            androidx.compose.ui.unit.Constraints(
                minWidth = if (width != Dp.Unspecified) width.roundToPx() else 0,
                maxWidth = if (width != Dp.Unspecified) width.roundToPx() else constraints.maxWidth,
                minHeight = if (height != Dp.Unspecified) height.roundToPx() else 0,
                maxHeight = if (height != Dp.Unspecified) height.roundToPx() else constraints.maxHeight
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable?.let {
                val x = when {
                    left != Dp.Unspecified -> left.roundToPx()
                    right != Dp.Unspecified -> constraints.maxWidth - right.roundToPx() - it.width
                    else -> 0
                }

                val y = when {
                    top != Dp.Unspecified -> top.roundToPx()
                    bottom != Dp.Unspecified -> constraints.maxHeight - bottom.roundToPx() - it.height
                    else -> 0
                }

                it.place(x, y)
            }
        }
    }
}

/**
 * Render AnimatedDefaultTextStyle using Jetpack Compose animations
 *
 * Maps AnimatedDefaultTextStyle to animated TextStyle properties.
 *
 * Performance: 60 FPS using animated text properties
 *
 * @param component AnimatedDefaultTextStyle component to render
 * @param content Child content renderer
 */
@Composable
fun AnimatedDefaultTextStyleMapper(
    component: AnimatedDefaultTextStyle,
    content: @Composable () -> Unit
) {
    val fontSize by animateFloatAsState(
        targetValue = component.style.fontSize ?: 16f,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        finishedListener = { component.onEnd?.invoke() },
        label = "text_font_size"
    )

    val letterSpacing by animateFloatAsState(
        targetValue = component.style.letterSpacing ?: 0f,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "text_letter_spacing"
    )

    val color by animateColorAsState(
        targetValue = component.style.color?.toComposeColor() ?: Color.Black,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "text_color"
    )

    CompositionLocalProvider(
        androidx.compose.ui.text.LocalTextStyle provides TextStyle(
            color = color,
            fontSize = fontSize.sp,
            fontWeight = component.style.fontWeight?.toComposeFontWeight(),
            fontStyle = component.style.fontStyle?.toComposeFontStyle(),
            letterSpacing = letterSpacing.sp,
            lineHeight = component.style.lineHeight?.sp ?: TextUnit.Unspecified,
            textDecoration = component.style.decoration?.toComposeTextDecoration(),
            textAlign = component.textAlign?.toComposeTextAlign()
        )
    ) {
        content()
    }
}

/**
 * Render AnimatedPadding using Jetpack Compose animations
 *
 * Maps AnimatedPadding to animated PaddingValues.
 *
 * Performance: 60 FPS using animateDpAsState for each edge
 *
 * @param component AnimatedPadding component to render
 * @param content Child content renderer
 */
@Composable
fun AnimatedPaddingMapper(
    component: AnimatedPadding,
    content: @Composable () -> Unit
) {
    val paddingStart by animateDpAsState(
        targetValue = component.padding.left.dp,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        finishedListener = { component.onEnd?.invoke() },
        label = "padding_start"
    )

    val paddingTop by animateDpAsState(
        targetValue = component.padding.top.dp,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "padding_top"
    )

    val paddingEnd by animateDpAsState(
        targetValue = component.padding.right.dp,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "padding_end"
    )

    val paddingBottom by animateDpAsState(
        targetValue = component.padding.bottom.dp,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "padding_bottom"
    )

    Box(
        modifier = Modifier.padding(
            start = paddingStart,
            top = paddingTop,
            end = paddingEnd,
            bottom = paddingBottom
        )
    ) {
        content()
    }
}

/**
 * Render AnimatedSize using Jetpack Compose animations
 *
 * Maps AnimatedSize to Compose's animateContentSize modifier.
 * Automatically animates to child's size.
 *
 * Performance: 60 FPS using animateContentSize
 *
 * @param component AnimatedSize component to render
 * @param content Child content renderer
 */
@Composable
fun AnimatedSizeMapper(
    component: AnimatedSize,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.animateContentSize(
            animationSpec = component.duration.toAnimationSpec(component.curve),
            finishedListener = { _, _ -> component.onEnd?.invoke() }
        ),
        contentAlignment = component.alignment.toComposeAlignment()
    ) {
        content()
    }
}

/**
 * Render AnimatedAlign using Jetpack Compose animations
 *
 * Maps AnimatedAlign to animated BiasAlignment.
 *
 * Performance: 60 FPS using animateFloatAsState for bias
 *
 * @param component AnimatedAlign component to render
 * @param content Child content renderer
 */
@Composable
fun AnimatedAlignMapper(
    component: AnimatedAlign,
    content: @Composable () -> Unit
) {
    val alignment = component.alignment.toAlignmentBias()

    val horizontalBias by animateFloatAsState(
        targetValue = alignment.first,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        finishedListener = { component.onEnd?.invoke() },
        label = "align_horizontal"
    )

    val verticalBias by animateFloatAsState(
        targetValue = alignment.second,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        label = "align_vertical"
    )

    Box(
        modifier = Modifier
            .then(
                component.widthFactor?.let {
                    Modifier.wrapContentWidth()
                } ?: Modifier.fillMaxWidth()
            )
            .then(
                component.heightFactor?.let {
                    Modifier.wrapContentHeight()
                } ?: Modifier.fillMaxHeight()
            ),
        contentAlignment = BiasAlignment(horizontalBias, verticalBias)
    ) {
        content()
    }
}

/**
 * Render AnimatedScale using Jetpack Compose animations
 *
 * Maps AnimatedScale to graphicsLayer scale transformation.
 * GPU-accelerated, no layout recomposition.
 *
 * Performance: 60 FPS using animateFloatAsState
 *
 * @param component AnimatedScale component to render
 * @param content Child content renderer
 */
@Composable
fun AnimatedScaleMapper(
    component: AnimatedScale,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = component.scale,
        animationSpec = component.duration.toAnimationSpec(component.curve),
        finishedListener = { component.onEnd?.invoke() },
        label = "scale"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = component.alignment.toTransformOrigin()
        }
    ) {
        content()
    }
}

// ============================================================================
// Extension Functions for Type Conversions
// ============================================================================

/**
 * Convert Size to Compose Dp
 */
private fun Size.toDp(): Dp = when (this) {
    is Size.Dp -> this.value.dp
    is Size.Pixels -> this.value.dp // Simplified conversion
    Size.Infinity -> Dp.Unspecified
    Size.Zero -> 0.dp
}

/**
 * Convert Color to Compose Color
 */
private fun com.augmentalis.avaelements.flutter.animation.Color.toComposeColor(): Color =
    Color(red, green, blue, alpha)

/**
 * Convert Duration and Curve to AnimationSpec
 */
private fun <T> Duration.toAnimationSpec(curve: Curve): AnimationSpec<T> {
    val durationMillis = this.milliseconds
    val easing = curve.toEasing()

    return tween(
        durationMillis = durationMillis,
        easing = easing
    )
}

/**
 * Convert Curve to Easing
 */
private fun Curve.toEasing(): Easing = when (this) {
    is Curve.Linear -> LinearEasing
    is Curve.EaseIn -> FastOutSlowInEasing
    is Curve.EaseOut -> FastOutLinearInEasing
    is Curve.EaseInOut -> FastOutSlowInEasing
    is Curve.FastOutSlowIn -> FastOutSlowInEasing
    is Curve.BounceIn -> LinearEasing // Compose doesn't have built-in bounce
    is Curve.BounceOut -> LinearEasing
    is Curve.ElasticIn -> LinearEasing // Compose doesn't have built-in elastic
    is Curve.ElasticOut -> LinearEasing
    is Curve.Cubic -> CubicBezierEasing(a, b, c, d)
}

/**
 * Convert AlignmentGeometry to Compose Alignment
 */
private fun AlignmentGeometry.toComposeAlignment(): Alignment = when (this) {
    is AlignmentGeometry.TopLeft -> Alignment.TopStart
    is AlignmentGeometry.TopCenter -> Alignment.TopCenter
    is AlignmentGeometry.TopRight -> Alignment.TopEnd
    is AlignmentGeometry.CenterLeft -> Alignment.CenterStart
    is AlignmentGeometry.Center -> Alignment.Center
    is AlignmentGeometry.CenterRight -> Alignment.CenterEnd
    is AlignmentGeometry.BottomLeft -> Alignment.BottomStart
    is AlignmentGeometry.BottomCenter -> Alignment.BottomCenter
    is AlignmentGeometry.BottomRight -> Alignment.BottomEnd
    is AlignmentGeometry.Custom -> BiasAlignment(x, y)
}

/**
 * Convert AlignmentGeometry to alignment bias (horizontal, vertical)
 */
private fun AlignmentGeometry.toAlignmentBias(): Pair<Float, Float> = when (this) {
    is AlignmentGeometry.TopLeft -> Pair(-1f, -1f)
    is AlignmentGeometry.TopCenter -> Pair(0f, -1f)
    is AlignmentGeometry.TopRight -> Pair(1f, -1f)
    is AlignmentGeometry.CenterLeft -> Pair(-1f, 0f)
    is AlignmentGeometry.Center -> Pair(0f, 0f)
    is AlignmentGeometry.CenterRight -> Pair(1f, 0f)
    is AlignmentGeometry.BottomLeft -> Pair(-1f, 1f)
    is AlignmentGeometry.BottomCenter -> Pair(0f, 1f)
    is AlignmentGeometry.BottomRight -> Pair(1f, 1f)
    is AlignmentGeometry.Custom -> Pair(x, y)
}

/**
 * Convert AlignmentGeometry to TransformOrigin
 */
private fun AlignmentGeometry.toTransformOrigin(): androidx.compose.ui.graphics.TransformOrigin {
    val (x, y) = this.toAlignmentBias()
    // Convert from -1..1 range to 0..1 range
    return androidx.compose.ui.graphics.TransformOrigin(
        pivotFractionX = (x + 1f) / 2f,
        pivotFractionY = (y + 1f) / 2f
    )
}

/**
 * Convert BoxDecoration to Compose Shape
 */
private fun BoxDecoration.toComposeShape(): Shape {
    return borderRadius?.let {
        RoundedCornerShape(
            topStart = it.topLeft.dp,
            topEnd = it.topRight.dp,
            bottomEnd = it.bottomRight.dp,
            bottomStart = it.bottomLeft.dp
        )
    } ?: RectangleShape
}

/**
 * Convert Border to Modifier
 */
private fun Border.toBorderModifier(shape: Shape): Modifier {
    // Simplified: use uniform border if all sides are equal
    val uniformSide = when {
        top == right && right == bottom && bottom == left -> top
        else -> top // Default to top for now
    }

    return uniformSide?.let { side ->
        Modifier.border(
            width = side.width.dp,
            color = side.color.toComposeColor(),
            shape = shape
        )
    } ?: Modifier
}

/**
 * Convert FontWeight to Compose FontWeight
 */
private fun com.augmentalis.avaelements.flutter.animation.FontWeight.toComposeFontWeight(): FontWeight =
    when (this) {
        com.augmentalis.avaelements.flutter.animation.FontWeight.Thin -> FontWeight.Thin
        com.augmentalis.avaelements.flutter.animation.FontWeight.ExtraLight -> FontWeight.ExtraLight
        com.augmentalis.avaelements.flutter.animation.FontWeight.Light -> FontWeight.Light
        com.augmentalis.avaelements.flutter.animation.FontWeight.Normal -> FontWeight.Normal
        com.augmentalis.avaelements.flutter.animation.FontWeight.Medium -> FontWeight.Medium
        com.augmentalis.avaelements.flutter.animation.FontWeight.SemiBold -> FontWeight.SemiBold
        com.augmentalis.avaelements.flutter.animation.FontWeight.Bold -> FontWeight.Bold
        com.augmentalis.avaelements.flutter.animation.FontWeight.ExtraBold -> FontWeight.ExtraBold
        com.augmentalis.avaelements.flutter.animation.FontWeight.Black -> FontWeight.Black
    }

/**
 * Convert FontStyle to Compose FontStyle
 */
private fun com.augmentalis.avaelements.flutter.animation.FontStyle.toComposeFontStyle(): FontStyle =
    when (this) {
        com.augmentalis.avaelements.flutter.animation.FontStyle.Normal -> FontStyle.Normal
        com.augmentalis.avaelements.flutter.animation.FontStyle.Italic -> FontStyle.Italic
    }

/**
 * Convert TextDecoration to Compose TextDecoration
 */
private fun com.augmentalis.avaelements.flutter.animation.TextDecoration.toComposeTextDecoration(): TextDecoration =
    when (this) {
        com.augmentalis.avaelements.flutter.animation.TextDecoration.None -> TextDecoration.None
        com.augmentalis.avaelements.flutter.animation.TextDecoration.Underline -> TextDecoration.Underline
        com.augmentalis.avaelements.flutter.animation.TextDecoration.Overline -> TextDecoration.None // Not supported
        com.augmentalis.avaelements.flutter.animation.TextDecoration.LineThrough -> TextDecoration.LineThrough
    }

/**
 * Convert TextAlign to Compose TextAlign
 */
private fun com.augmentalis.avaelements.flutter.animation.TextAlign.toComposeTextAlign(): TextAlign =
    when (this) {
        com.augmentalis.avaelements.flutter.animation.TextAlign.Left -> TextAlign.Left
        com.augmentalis.avaelements.flutter.animation.TextAlign.Right -> TextAlign.Right
        com.augmentalis.avaelements.flutter.animation.TextAlign.Center -> TextAlign.Center
        com.augmentalis.avaelements.flutter.animation.TextAlign.Justify -> TextAlign.Justify
        com.augmentalis.avaelements.flutter.animation.TextAlign.Start -> TextAlign.Start
        com.augmentalis.avaelements.flutter.animation.TextAlign.End -> TextAlign.End
    }
