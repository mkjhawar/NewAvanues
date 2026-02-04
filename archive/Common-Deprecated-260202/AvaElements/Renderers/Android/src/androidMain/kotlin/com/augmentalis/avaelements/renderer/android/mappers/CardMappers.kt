package com.augmentalis.avaelements.renderer.android.mappers

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.augmentalis.avaelements.flutter.material.cards.*
import com.augmentalis.avaelements.renderer.android.IconFromString

/**
 * Android Compose mappers for card components
 *
 * This file contains renderer functions that map cross-platform card component models
 * to Material3 Compose implementations on Android.
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render PricingCard component using Material3
 *
 * Maps PricingCard component to Material3 Card with pricing tier layout:
 * - Highlighted/featured tier support with different colors
 * - Optional ribbon badge
 * - Feature list with checkmark icons
 * - Call-to-action button
 * - Full accessibility support
 * - Dark mode compatibility
 *
 * @param component PricingCard component to render
 */
@Composable
fun PricingCardMapper(component: PricingCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (component.highlighted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (component.highlighted) 4.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Ribbon badge
            if (component.ribbonText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = component.ribbonText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Title
            Text(
                text = component.title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (component.highlighted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            // Subtitle
            if (component.subtitle != null) {
                Text(
                    text = component.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Price
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = component.price,
                    style = MaterialTheme.typography.displayMedium,
                    color = if (component.highlighted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (component.period != null) {
                    Text(
                        text = " ${component.period}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Features
            component.features.forEachIndexed { index, feature ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Button
            Button(
                onClick = { component.onPressed?.invoke() },
                enabled = component.buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = if (component.highlighted)
                    ButtonDefaults.buttonColors()
                else
                    ButtonDefaults.outlinedButtonColors()
            ) {
                Text(component.buttonText)
            }
        }
    }
}

/**
 * Render FeatureCard component using Material3
 */
@Composable
fun FeatureCardMapper(component: FeatureCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        val isVertical = component.layout == FeatureCard.Layout.Vertical

        if (isVertical) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconFromString(
                    iconName = component.icon,
                    size = component.iconSize.dp,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = component.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (component.actionText != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { component.onActionPressed?.invoke() }) {
                        Text(component.actionText)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconFromString(
                    iconName = component.icon,
                    size = component.iconSize.dp,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = component.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = component.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Render TestimonialCard component using Material3
 */
@Composable
fun TestimonialCardMapper(component: TestimonialCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Quote icon
            if (component.showQuoteIcon) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quote text
            Text(
                text = component.quote,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic
            )

            // Rating
            if (component.rating != null && component.isRatingValid()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < component.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Author info
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                if (component.avatarUrl != null) {
                    AsyncImage(
                        model = component.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else if (component.avatarInitials != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = component.avatarInitials,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = component.authorName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (component.authorTitle != null) {
                        Text(
                            text = component.authorTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Render ProductCard component using Material3
 */
@Composable
fun ProductCardMapper(component: ProductCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        Column {
            // Product image with badge
            Box {
                AsyncImage(
                    model = component.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                // Badge
                if (component.badgeText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = component.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }

                // Wishlist button
                if (component.showWishlist) {
                    IconButton(
                        onClick = { component.onWishlist?.invoke(true) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Add to wishlist",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )

                // Description
                if (component.description != null) {
                    Text(
                        text = component.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Rating
                if (component.rating != null && component.isRatingValid()) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < component.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (component.reviewCount != null) {
                            Text(
                                text = " (${component.reviewCount})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = component.price,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (component.originalPrice != null) {
                        Text(
                            text = component.originalPrice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Stock status
                if (!component.inStock) {
                    Text(
                        text = "Out of Stock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Add to cart button
                if (component.showAddToCart && component.inStock) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { component.onAddToCart?.invoke() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add to Cart")
                    }
                }
            }
        }
    }
}

/**
 * Render ArticleCard component using Material3
 */
@Composable
fun ArticleCardMapper(component: ArticleCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        Column {
            // Featured image
            AsyncImage(
                model = component.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Category
                if (component.category != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = component.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Title
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Excerpt
                Text(
                    text = component.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Author and metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (component.authorAvatar != null) {
                            AsyncImage(
                                model = component.authorAvatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                text = component.authorName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = component.getMetadataText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (component.showBookmark) {
                        IconButton(onClick = { component.onBookmark?.invoke(!component.bookmarked) }) {
                            Icon(
                                imageVector = if (component.bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render ImageCard component using Material3
 */
@Composable
fun ImageCardMapper(component: ImageCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable { component.onPressed.invoke() }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors()
    ) {
        Box {
            // Image
            AsyncImage(
                model = component.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (component.aspectRatio != null) {
                            Modifier.aspectRatio(component.aspectRatio)
                        } else {
                            Modifier.height(250.dp)
                        }
                    )
            )

            // Overlay
            if (component.hasOverlay()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (component.showGradient) {
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                } else {
                                    listOf(Color.Transparent, Color.Transparent)
                                }
                            )
                        ),
                    contentAlignment = when (component.overlayPosition) {
                        ImageCard.OverlayPosition.Top -> Alignment.TopStart
                        ImageCard.OverlayPosition.Center -> Alignment.Center
                        ImageCard.OverlayPosition.Bottom -> Alignment.BottomStart
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (component.title != null) {
                            Text(
                                text = component.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        if (component.subtitle != null) {
                            Text(
                                text = component.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (component.actionText != null) {
                            TextButton(
                                onClick = { component.onActionPressed?.invoke() },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = component.actionText,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render HoverCard component using Material3
 */
@Composable
fun HoverCardMapper(component: HoverCard) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onPressed != null) {
                Modifier.clickable {
                    isPressed = !isPressed
                    component.onPressed.invoke()
                }
            } else {
                Modifier
            })
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = component.elevation.dp,
            pressedElevation = component.hoverElevation.dp
        )
    ) {
        Box {
            // Background image
            if (component.imageUrl != null) {
                AsyncImage(
                    model = component.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            // Overlay on press (simulating hover)
            if (isPressed && component.showOverlay) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = component.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (component.imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface
                )

                if (component.description != null) {
                    Text(
                        text = component.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (component.imageUrl != null) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Actions (always visible for accessibility)
                if (component.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        component.actions.forEach { action ->
                            IconButton(
                                onClick = { component.onActionPressed?.invoke(action.id) },
                                enabled = action.enabled
                            ) {
                                if (action.icon != null) {
                                    IconFromString(
                                        iconName = action.icon,
                                        contentDescription = action.label,
                                        tint = if (component.imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render ExpandableCard component using Material3
 */
@Composable
fun ExpandableCardMapper(component: ExpandableCard) {
    var expanded by remember {
        mutableStateOf(component.expanded ?: component.initiallyExpanded)
    }

    // Use controlled state if provided
    val isExpanded = if (component.isControlled()) component.expanded!! else expanded

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = component.animationDuration),
        label = "expand_icon_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription(isExpanded)
            },
        colors = CardDefaults.cardColors()
    ) {
        Column {
            // Header
            ListItem(
                headlineContent = { Text(component.title) },
                supportingContent = if (component.subtitle != null) {
                    { Text(component.subtitle) }
                } else {
                    null
                },
                leadingContent = if (component.icon != null) {
                    {
                        IconFromString(
                            iconName = component.icon,
                            contentDescription = null
                        )
                    }
                } else {
                    null
                },
                trailingContent = {
                    Row {
                        component.headerActions.forEach { action ->
                            IconButton(
                                onClick = { component.onHeaderActionPressed?.invoke(action.id) },
                                enabled = action.enabled
                            ) {
                                IconFromString(
                                    iconName = action.icon,
                                    contentDescription = action.label
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val newState = !isExpanded
                                if (!component.isControlled()) {
                                    expanded = newState
                                }
                                component.onExpansionChanged?.invoke(newState)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                modifier = Modifier.rotate(rotationAngle)
                            )
                        }
                    }
                },
                modifier = Modifier.clickable {
                    val newState = !isExpanded
                    if (!component.isControlled()) {
                        expanded = newState
                    }
                    component.onExpansionChanged?.invoke(newState)
                }
            )

            // Summary content when collapsed
            if (!isExpanded && component.summaryContent != null) {
                Text(
                    text = component.summaryContent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Divider
            if (isExpanded && component.showDivider) {
                HorizontalDivider()
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = component.animationDuration)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = component.animationDuration)
                )
            ) {
                Text(
                    text = component.expandedContent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Render MetricCard component using Material3
 */
@Composable
fun MetricCardMapper(component: MetricCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (component.onClick != null) Modifier.clickable { component.onClick.invoke() } else Modifier)
            .semantics { contentDescription = component.contentDescription ?: "Metric: ${component.title}" },
        colors = component.color?.let { CardDefaults.cardColors(containerColor = Color(AndroidColor.parseColor(it))) } ?: CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                component.icon?.let { IconFromString(it, null, Modifier.size(20.dp)) }
                Text(text = component.title, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = component.value, style = MaterialTheme.typography.displaySmall)
                component.unit?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            }
            component.comparison?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    component.change?.let { change ->
                        Text(
                            text = change,
                            style = MaterialTheme.typography.labelMedium,
                            color = when (component.changeType) {
                                MetricCard.ChangeType.Positive -> MaterialTheme.colorScheme.primary
                                MetricCard.ChangeType.Negative -> MaterialTheme.colorScheme.error
                                MetricCard.ChangeType.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
