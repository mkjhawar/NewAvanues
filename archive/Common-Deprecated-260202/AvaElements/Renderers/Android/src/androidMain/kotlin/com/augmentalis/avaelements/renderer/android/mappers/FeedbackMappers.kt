package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.augmentalis.avaelements.flutter.material.feedback.*
import com.augmentalis.avaelements.renderer.android.IconFromString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Flutter Parity Feedback Component Mappers
 *
 * Maps all 13 feedback components to Material 3 Compose implementations:
 * 1. Popup - Floating popup with flexible positioning
 * 2. Callout - Banner/callout message
 * 3. HoverCard - Hover information card (touch fallback on mobile)
 * 4. Disclosure - Expandable disclosure widget
 * 5. InfoPanel - Information panel (blue theme)
 * 6. ErrorPanel - Error panel (red theme)
 * 7. WarningPanel - Warning panel (amber theme)
 * 8. SuccessPanel - Success panel (green theme)
 * 9. FullPageLoading - Full-page loading overlay
 * 10. AnimatedCheck - Animated checkmark
 * 11. AnimatedError - Animated error icon
 * 12. AnimatedSuccess - Animated success icon (with particles)
 * 13. AnimatedWarning - Animated warning icon (with pulse)
 *
 * @since 3.2.0-feedback-components
 */

// ============================================================================
// POPUP COMPONENT
// ============================================================================

/**
 * Render Popup component using Material3
 *
 * Maps Popup to Material3 Popup with flexible positioning:
 * - Floating above content
 * - Auto-positioning
 * - Optional arrow pointer
 * - Dismissible behavior
 * - Full accessibility support
 *
 * @param component Popup component to render
 */
@Composable
fun PopupMapper(component: com.augmentalis.avaelements.flutter.material.feedback.Popup) {
    if (!component.visible) return

    Popup(
        alignment = getAlignmentFromPosition(component.anchorPosition),
        offset = androidx.compose.ui.unit.IntOffset(
            component.offsetX.dp.value.toInt(),
            component.offsetY.dp.value.toInt()
        ),
        onDismissRequest = if (component.dismissible) {
            { component.onDismiss?.invoke() }
        } else null,
        properties = PopupProperties(
            dismissOnBackPress = component.dismissible,
            dismissOnClickOutside = component.dismissible
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = component.maxWidth.dp)
                .let { mod ->
                    component.width?.let { mod.width(it.dp) } ?: mod
                }
                .semantics {
                    contentDescription = component.getAccessibilityDescription()
                },
            shape = RoundedCornerShape(8.dp),
            color = component.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) }
                ?: MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = component.elevation.dp,
            shadowElevation = component.elevation.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = component.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getAlignmentFromPosition(position: com.augmentalis.avaelements.flutter.material.feedback.Popup.Position): Alignment {
    return when (position) {
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.TopStart -> Alignment.TopStart
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.TopCenter -> Alignment.TopCenter
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.TopEnd -> Alignment.TopEnd
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.BottomStart -> Alignment.BottomStart
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.BottomCenter -> Alignment.BottomCenter
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.BottomEnd -> Alignment.BottomEnd
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.LeftStart -> Alignment.CenterStart
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.LeftCenter -> Alignment.CenterStart
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.LeftEnd -> Alignment.CenterStart
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.RightStart -> Alignment.CenterEnd
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.RightCenter -> Alignment.CenterEnd
        com.augmentalis.avaelements.flutter.material.feedback.Popup.Position.RightEnd -> Alignment.CenterEnd
    }
}

// ============================================================================
// CALLOUT COMPONENT
// ============================================================================

/**
 * Render Callout component using Material3
 *
 * Maps Callout to Material3 Card with banner styling:
 * - Full-width banner
 * - Optional icon
 * - Dismissible
 * - Theme-aware colors
 *
 * @param component Callout component to render
 */
@Composable
fun CalloutMapper(component: Callout) {
    if (!component.visible) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(
            containerColor = when (component.severity) {
                Callout.Severity.Info -> MaterialTheme.colorScheme.primaryContainer
                Callout.Severity.Success -> MaterialTheme.colorScheme.tertiaryContainer
                Callout.Severity.Warning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                Callout.Severity.Error -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        shape = RoundedCornerShape(component.borderRadius.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (component.showIcon) {
                Icon(
                    imageVector = when (component.severity) {
                        Callout.Severity.Info -> Icons.Default.Info
                        Callout.Severity.Success -> Icons.Default.CheckCircle
                        Callout.Severity.Warning -> Icons.Default.Warning
                        Callout.Severity.Error -> Icons.Default.Error
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = when (component.severity) {
                        Callout.Severity.Info -> MaterialTheme.colorScheme.primary
                        Callout.Severity.Success -> MaterialTheme.colorScheme.tertiary
                        Callout.Severity.Warning -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        Callout.Severity.Error -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (component.title != null) {
                    Text(
                        text = component.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = component.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (component.dismissible) {
                IconButton(onClick = { component.onDismiss?.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// HOVER CARD COMPONENT
// ============================================================================

/**
 * Render HoverCard component using Material3
 *
 * Maps HoverCard to Material3 Card with hover behavior (touch fallback):
 * - Rich content display
 * - Auto-positioning
 * - Optional actions
 * - Smooth transitions
 *
 * @param component HoverCard component to render
 */
@Composable
fun HoverCardMapper(component: HoverCard) {
    var isHovered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box {
        // Trigger content
        Text(
            text = component.triggerContent,
            modifier = Modifier
                .clickable { isHovered = !isHovered }
                .semantics {
                    contentDescription = "Tap to show ${component.cardTitle}"
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Hover card
        if (isHovered) {
            Popup(
                alignment = when (component.position) {
                    HoverCard.Position.Top -> Alignment.TopCenter
                    HoverCard.Position.Bottom -> Alignment.BottomCenter
                    HoverCard.Position.Left -> Alignment.CenterStart
                    HoverCard.Position.Right -> Alignment.CenterEnd
                    HoverCard.Position.Auto -> Alignment.BottomCenter
                },
                onDismissRequest = { isHovered = false }
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = component.maxWidth.dp)
                        .let { mod ->
                            component.width?.let { mod.width(it.dp) } ?: mod
                        }
                        .semantics {
                            contentDescription = component.getAccessibilityDescription()
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = component.elevation.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (component.cardIcon != null) {
                            IconFromString(
                                iconName = component.cardIcon,
                                size = 24.dp,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = component.cardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = component.cardContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (component.actions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                component.actions.forEach { action ->
                                    TextButton(onClick = {
                                        action.onClick?.invoke()
                                        isHovered = false
                                    }) {
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

// ============================================================================
// DISCLOSURE COMPONENT
// ============================================================================

/**
 * Render Disclosure component using Material3
 *
 * Maps Disclosure to Material3 expandable content:
 * - Smooth expand/collapse animation
 * - Controlled/uncontrolled state
 * - Keyboard navigation
 * - Full accessibility
 *
 * @param component Disclosure component to render
 */
@Composable
fun DisclosureMapper(component: Disclosure) {
    var expanded by remember(component.initiallyExpanded) {
        mutableStateOf(component.expanded ?: component.initiallyExpanded)
    }

    val currentExpanded = component.expanded ?: expanded
    val rotationAngle by animateFloatAsState(
        targetValue = if (currentExpanded) 180f else 0f,
        animationSpec = tween(component.animationDuration)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription(currentExpanded)
            }
    ) {
        // Disclosure header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (currentExpanded) "Collapse" else "Expand"
                ) {
                    val newExpanded = !currentExpanded
                    if (component.expanded == null) {
                        expanded = newExpanded
                    }
                    component.onExpansionChanged?.invoke(newExpanded)
                }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = component.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (component.showIcon) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expandable content
        AnimatedVisibility(
            visible = currentExpanded,
            enter = expandVertically(animationSpec = tween(component.animationDuration)),
            exit = shrinkVertically(animationSpec = tween(component.animationDuration))
        ) {
            Text(
                text = component.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

// ============================================================================
// PANEL COMPONENTS (Info, Error, Warning, Success)
// ============================================================================

/**
 * Render InfoPanel component using Material3
 */
@Composable
fun InfoPanelMapper(component: InfoPanel) {
    PanelBase(
        title = component.title,
        message = component.message,
        icon = component.getEffectiveIcon(),
        dismissible = component.dismissible,
        actions = component.actions,
        elevation = component.elevation,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        iconTint = MaterialTheme.colorScheme.primary,
        contentDescription = component.getAccessibilityDescription(),
        onDismiss = component.onDismiss
    )
}

/**
 * Render ErrorPanel component using Material3
 */
@Composable
fun ErrorPanelMapper(component: ErrorPanel) {
    PanelBase(
        title = component.title,
        message = component.message,
        icon = component.getEffectiveIcon(),
        dismissible = component.dismissible,
        actions = component.actions,
        elevation = component.elevation,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        iconTint = MaterialTheme.colorScheme.error,
        contentDescription = component.getAccessibilityDescription(),
        onDismiss = component.onDismiss
    )
}

/**
 * Render WarningPanel component using Material3
 */
@Composable
fun WarningPanelMapper(component: WarningPanel) {
    PanelBase(
        title = component.title,
        message = component.message,
        icon = component.getEffectiveIcon(),
        dismissible = component.dismissible,
        actions = component.actions,
        elevation = component.elevation,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        iconTint = MaterialTheme.colorScheme.tertiary,
        contentDescription = component.getAccessibilityDescription(),
        onDismiss = component.onDismiss
    )
}

/**
 * Render SuccessPanel component using Material3
 */
@Composable
fun SuccessPanelMapper(component: SuccessPanel) {
    PanelBase(
        title = component.title,
        message = component.message,
        icon = component.getEffectiveIcon(),
        dismissible = component.dismissible,
        actions = component.actions,
        elevation = component.elevation,
        containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        iconTint = Color(0xFF4CAF50),
        contentDescription = component.getAccessibilityDescription(),
        onDismiss = component.onDismiss
    )
}

/**
 * Reusable panel base composable
 */
@Composable
private fun PanelBase(
    title: String,
    message: String,
    icon: String,
    dismissible: Boolean,
    actions: List<Any>,
    elevation: Float,
    containerColor: Color,
    contentColor: Color,
    iconTint: Color,
    contentDescription: String,
    onDismiss: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                this.contentDescription = contentDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (icon) {
                    "info" -> Icons.Default.Info
                    "error" -> Icons.Default.Error
                    "warning" -> Icons.Default.Warning
                    "success", "check_circle" -> Icons.Default.CheckCircle
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )

                // Actions
                if (actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        actions.forEach { action ->
                            when (action) {
                                is InfoPanel.Action -> {
                                    TextButton(onClick = { action.onClick?.invoke() }) {
                                        Text(action.label)
                                    }
                                }
                                is ErrorPanel.Action -> {
                                    TextButton(onClick = { action.onClick?.invoke() }) {
                                        Text(action.label)
                                    }
                                }
                                is WarningPanel.Action -> {
                                    TextButton(onClick = { action.onClick?.invoke() }) {
                                        Text(action.label)
                                    }
                                }
                                is SuccessPanel.Action -> {
                                    TextButton(onClick = { action.onClick?.invoke() }) {
                                        Text(action.label)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (dismissible) {
                IconButton(onClick = { onDismiss?.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = contentColor
                    )
                }
            }
        }
    }
}

// ============================================================================
// FULL PAGE LOADING COMPONENT
// ============================================================================

/**
 * Render FullPageLoading component using Material3
 */
@Composable
fun FullPageLoadingMapper(component: FullPageLoading) {
    if (!component.visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(component.spinnerSize.dp)
                )

                component.message?.let {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                if (component.cancelable) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { component.onCancel?.invoke() }) {
                        Text(component.cancelText)
                    }
                }
            }
        }
    }
}

// ============================================================================
// ANIMATED ICON COMPONENTS
// ============================================================================

/**
 * Render AnimatedCheck component using Material3
 */
@Composable
fun AnimatedCheckMapper(component: AnimatedCheck) {
    if (!component.visible) return

    val scale by animateFloatAsState(
        targetValue = if (component.visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = component.getAccessibilityDescription(),
        modifier = Modifier
            .size(component.size.dp)
            .scale(scale)
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        tint = Color(android.graphics.Color.parseColor(component.getEffectiveColor()))
    )
}

/**
 * Render AnimatedError component using Material3
 */
@Composable
fun AnimatedErrorMapper(component: AnimatedError) {
    if (!component.visible) return

    val infiniteTransition = rememberInfiniteTransition()
    var shakeOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(component.visible) {
        if (component.visible) {
            // Shake animation
            for (i in 0..3) {
                shakeOffset = component.shakeIntensity
                delay(50)
                shakeOffset = -component.shakeIntensity
                delay(50)
            }
            shakeOffset = 0f
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (component.visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Icon(
        imageVector = Icons.Default.Cancel,
        contentDescription = component.getAccessibilityDescription(),
        modifier = Modifier
            .size(component.size.dp)
            .scale(scale)
            .offset(x = shakeOffset.dp)
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        tint = Color(android.graphics.Color.parseColor(component.getEffectiveColor()))
    )
}

/**
 * Render AnimatedSuccess component using Material3
 */
@Composable
fun AnimatedSuccessMapper(component: AnimatedSuccess) {
    if (!component.visible) return

    val scale by animateFloatAsState(
        targetValue = if (component.visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription()
        }
    ) {
        // Particle effects (if enabled)
        if (component.showParticles && component.visible) {
            ParticleEffect(count = component.particleCount, size = component.size)
        }

        // Main icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = component.getAccessibilityDescription(),
            modifier = Modifier
                .size(component.size.dp)
                .scale(scale),
            tint = Color(android.graphics.Color.parseColor(component.getEffectiveColor()))
        )
    }
}

/**
 * Render AnimatedWarning component using Material3
 */
@Composable
fun AnimatedWarningMapper(component: AnimatedWarning) {
    if (!component.visible) return

    var pulseScale by remember { mutableStateOf(1f) }

    LaunchedEffect(component.visible) {
        if (component.visible && component.pulseCount > 0) {
            repeat(component.pulseCount) {
                pulseScale = component.pulseIntensity
                delay(200)
                pulseScale = 1f
                delay(200)
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (component.visible) pulseScale else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = component.getAccessibilityDescription(),
        modifier = Modifier
            .size(component.size.dp)
            .scale(scale)
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        tint = Color(android.graphics.Color.parseColor(component.getEffectiveColor()))
    )
}

/**
 * Particle effect for success animation
 */
@Composable
private fun ParticleEffect(count: Int, size: Float) {
    val particles = remember { (0 until count).map { ParticleState() } }

    LaunchedEffect(Unit) {
        particles.forEach { particle ->
            launch {
                particle.animate()
            }
        }
    }

    particles.forEach { particle ->
        Box(
            modifier = Modifier
                .offset(
                    x = (particle.offsetX * size).dp,
                    y = (particle.offsetY * size).dp
                )
                .size(4.dp)
                .alpha(particle.alpha)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
    }
}

private class ParticleState {
    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)
    var alpha by mutableStateOf(1f)
    private val angle = (0..360).random().toFloat()
    private val distance = (0.5f..1.5f).random()

    suspend fun animate() {
        val steps = 30
        repeat(steps) { step ->
            val progress = step / steps.toFloat()
            offsetX = cos(Math.toRadians(angle.toDouble())).toFloat() * distance * progress
            offsetY = sin(Math.toRadians(angle.toDouble())).toFloat() * distance * progress
            alpha = 1f - progress
            delay(16)
        }
    }
}
