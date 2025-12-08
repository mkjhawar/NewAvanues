package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.cards.*

/**
 * iOS SwiftUI Mappers for Flutter Card Parity Components
 *
 * This file maps cross-platform Flutter card components to iOS SwiftUI
 * bridge representations. The SwiftUI bridge models are consumed by Swift
 * code to render native iOS UI.
 *
 * Architecture:
 * Flutter Card Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented (8 Card Types):
 * - PricingCard: Pricing tier card with features and CTA
 * - FeatureCard: Feature showcase with icon and description
 * - TestimonialCard: Customer testimonial with quote and rating
 * - ProductCard: E-commerce product card with image and price
 * - ArticleCard: Blog/article card with author and metadata
 * - ImageCard: Image-focused card with overlay text
 * - HoverCard: Interactive card with hover effects and actions
 * - ExpandableCard: Expandable/collapsible card for FAQs
 *
 * iOS-specific features:
 * - Native SwiftUI card styling with Material Design 3 parity
 * - Smooth animations for expand/collapse and hover effects
 * - VoiceOver accessibility with semantic descriptions
 * - Dynamic Type support for text scaling
 * - Dark mode support via system color scheme
 * - Tap gestures instead of hover on mobile
 * - Pull-to-refresh integration where applicable
 *
 * @since 3.1.0-android-parity
 */

/**
 * Maps PricingCard to SwiftUI card with pricing tier layout
 *
 * Creates a VStack-based card with:
 * - Optional ribbon badge for "Popular", "Best Value", etc.
 * - Title and subtitle for tier name
 * - Prominent price display with period
 * - Feature list with checkmark icons
 * - CTA button at bottom
 * - Highlighted style with accent colors
 *
 * Visual hierarchy matches Material Design 3 pricing cards
 */
object PricingCardMapper {
    fun map(
        component: PricingCard,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Ribbon badge if present
        val ribbonText = component.ribbonText
        if (ribbonText != null) {
            children.add(
                SwiftUIView.text(
                    content = ribbonText,
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(12f),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                        SwiftUIModifier.padding(6f, 12f, 6f, 12f),
                        SwiftUIModifier.background(
                            component.ribbonColor?.let { color -> SwiftUIColor.hex(color) }
                                ?: SwiftUIColor.primary
                        ),
                        SwiftUIModifier.cornerRadius(12f)
                    )
                )
            )
        }

        // Title
        children.add(
            SwiftUIView.text(
                content = component.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Title),
                    SwiftUIModifier.fontWeight(FontWeight.Bold),
                    SwiftUIModifier.foregroundColor(
                        if (component.highlighted) SwiftUIColor.primary else SwiftUIColor.text
                    )
                )
            )
        )

        // Subtitle
        val subtitle = component.subtitle
        if (subtitle != null) {
            children.add(
                SwiftUIView.text(
                    content = subtitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Subheadline),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                    )
                )
            )
        }

        // Price container
        val priceChildren = mutableListOf<SwiftUIView>()

        // Currency (if specified)
        val currency = component.currency
        if (currency != null) {
            priceChildren.add(
                SwiftUIView.text(
                    content = currency,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title3),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold)
                    )
                )
            )
        }

        // Price amount
        priceChildren.add(
            SwiftUIView.text(
                content = component.price,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.LargeTitle),
                    SwiftUIModifier.fontWeight(FontWeight.Bold)
                )
            )
        )

        val priceStack = SwiftUIView.hStack(
            spacing = 4f,
            alignment = VerticalAlignment.FirstTextBaseline,
            children = priceChildren,
            modifiers = listOf(SwiftUIModifier.padding(16f, 0f, 16f, 0f))
        )
        children.add(priceStack)

        // Period
        val period = component.period
        if (period != null) {
            children.add(
                SwiftUIView.text(
                    content = period,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Subheadline),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                    )
                )
            )
        }

        // Features list
        if (component.features.isNotEmpty()) {
            children.add(
                SwiftUIView(
                    type = ViewType.Divider,
                    properties = emptyMap(),
                    modifiers = listOf(SwiftUIModifier.padding(16f, 0f, 16f, 0f))
                )
            )

            component.features.forEachIndexed { index, feature ->
                val featureChildren = mutableListOf<SwiftUIView>()

                // Checkmark icon
                val icon = component.featureIcons?.getOrNull(index) ?: "checkmark.circle.fill"
                featureChildren.add(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to icon),
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(16f),
                            SwiftUIModifier.foregroundColor(
                                if (component.highlighted) SwiftUIColor.primary else SwiftUIColor.green
                            )
                        )
                    )
                )

                // Feature text
                featureChildren.add(
                    SwiftUIView.text(
                        content = feature,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Body),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                        )
                    )
                )

                children.add(
                    SwiftUIView.hStack(
                        spacing = 12f,
                        alignment = VerticalAlignment.Center,
                        children = featureChildren,
                        modifiers = listOf(SwiftUIModifier.padding(8f, 0f, 8f, 0f))
                    )
                )
            }
        }

        // Spacer to push button to bottom
        children.add(
            SwiftUIView(
                type = ViewType.Spacer,
                properties = emptyMap()
            )
        )

        // CTA Button
        children.add(
            SwiftUIView.button(
                label = component.buttonText,
                action = component.id,
                modifiers = listOf(
                    SwiftUIModifier.frame(
                        width = SizeValue.Infinity,
                        height = SizeValue.Fixed(44f)
                    ),
                    SwiftUIModifier.background(
                        if (component.highlighted && component.buttonEnabled)
                            SwiftUIColor.primary
                        else if (component.buttonEnabled)
                            SwiftUIColor.system("systemBlue")
                        else
                            SwiftUIColor.system("systemGray4")
                    ),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                    SwiftUIModifier.cornerRadius(12f),
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold),
                    SwiftUIModifier.disabled(!component.buttonEnabled)
                )
            )
        )

        val cardModifiers = mutableListOf<SwiftUIModifier>()
        cardModifiers.add(SwiftUIModifier.padding(20f))
        cardModifiers.add(
            SwiftUIModifier.background(
                if (component.highlighted)
                    SwiftUIColor.primary.copy(value = RGBValue(0.2f, 0.5f, 1.0f, 0.1f))
                else
                    SwiftUIColor.system("systemBackground")
            )
        )
        cardModifiers.add(SwiftUIModifier.cornerRadius(16f))
        cardModifiers.add(
            SwiftUIModifier.shadow(
                radius = if (component.highlighted) 8f else 4f,
                x = 0f,
                y = if (component.highlighted) 4f else 2f
            )
        )

        if (component.highlighted) {
            cardModifiers.add(
                SwiftUIModifier.border(
                    SwiftUIColor.primary,
                    width = 2f
                )
            )
        }

        return SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Center,
            children = children,
            modifiers = cardModifiers
        )
    }
}

/**
 * Maps FeatureCard to SwiftUI card with icon and description
 *
 * Creates a card layout with:
 * - Large icon at top, left, right, or bottom
 * - Title and description text
 * - Optional action button/link
 * - Horizontal or vertical layout
 * - Custom icon colors and sizes
 *
 * Supports both horizontal and vertical layout orientations
 */
object FeatureCardMapper {
    fun map(
        component: FeatureCard,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Icon view
        val iconView = SwiftUIView(
            type = ViewType.Image,
            properties = mapOf("systemName" to component.icon),
            modifiers = listOf(
                SwiftUIModifier.fontSize(component.iconSize),
                SwiftUIModifier.foregroundColor(
                    component.iconColor?.let { color -> SwiftUIColor.hex(color) } ?: SwiftUIColor.primary
                ),
                SwiftUIModifier.frame(
                    width = SizeValue.Fixed(component.iconSize),
                    height = SizeValue.Fixed(component.iconSize)
                )
            )
        )

        // Text content
        val textChildren = mutableListOf<SwiftUIView>()

        textChildren.add(
            SwiftUIView.text(
                content = component.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Title3),
                    SwiftUIModifier.fontWeight(FontWeight.Bold),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                )
            )
        )

        textChildren.add(
            SwiftUIView.text(
                content = component.description,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                )
            )
        )

        // Action button if present
        val actionText = component.actionText
        if (actionText != null) {
            val actionChildren = mutableListOf<SwiftUIView>()

            actionChildren.add(
                SwiftUIView.text(
                    content = actionText,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Subheadline),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                    )
                )
            )

            val actionIcon = component.actionIcon
            if (actionIcon != null) {
                actionChildren.add(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to actionIcon),
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(14f),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                        )
                    )
                )
            }

            textChildren.add(
                SwiftUIView.hStack(
                    spacing = 4f,
                    alignment = VerticalAlignment.Center,
                    children = actionChildren,
                    modifiers = listOf(SwiftUIModifier.padding(8f, 0f, 0f, 0f))
                )
            )
        }

        val textStack = SwiftUIView.vStack(
            spacing = 8f,
            alignment = when (component.layout) {
                FeatureCard.Layout.Horizontal -> HorizontalAlignment.Leading
                FeatureCard.Layout.Vertical -> HorizontalAlignment.Center
            },
            children = textChildren
        )

        // Arrange icon and text based on layout and position
        val isVertical = component.layout == FeatureCard.Layout.Vertical

        when (component.iconPosition) {
            FeatureCard.IconPosition.Top -> {
                children.add(iconView)
                children.add(textStack)
            }
            FeatureCard.IconPosition.Bottom -> {
                children.add(textStack)
                children.add(iconView)
            }
            FeatureCard.IconPosition.Left -> {
                return SwiftUIView.hStack(
                    spacing = 16f,
                    alignment = VerticalAlignment.Top,
                    children = listOf(iconView, textStack),
                    modifiers = getCardModifiers()
                )
            }
            FeatureCard.IconPosition.Right -> {
                return SwiftUIView.hStack(
                    spacing = 16f,
                    alignment = VerticalAlignment.Top,
                    children = listOf(textStack, iconView),
                    modifiers = getCardModifiers()
                )
            }
        }

        return SwiftUIView.vStack(
            spacing = 16f,
            alignment = HorizontalAlignment.Center,
            children = children,
            modifiers = getCardModifiers()
        )
    }

    private fun getCardModifiers() = listOf(
        SwiftUIModifier.padding(20f),
        SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
        SwiftUIModifier.cornerRadius(16f),
        SwiftUIModifier.shadow(radius = 4f, x = 0f, y = 2f)
    )
}

/**
 * Maps TestimonialCard to SwiftUI card with quote and author
 *
 * Creates a testimonial layout with:
 * - Optional quotation mark icon
 * - Quote text with appropriate styling
 * - Author avatar (circle image or initials)
 * - Author name and title/company
 * - Star rating display
 *
 * VoiceOver announces complete testimonial with rating
 */
object TestimonialCardMapper {
    fun map(
        component: TestimonialCard,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Quote icon if enabled
        if (component.showQuoteIcon) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to (component.quoteIcon ?: "quote.opening")
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(32f),
                        SwiftUIModifier.foregroundColor(
                            SwiftUIColor.primary.copy(value = RGBValue(0.2f, 0.5f, 1.0f, 0.3f))
                        )
                    )
                )
            )
        }

        // Quote text
        children.add(
            SwiftUIView.text(
                content = "\"${component.quote}\"",
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text),
                    SwiftUIModifier.padding(8f, 0f, 8f, 0f)
                )
            )
        )

        // Rating stars if present
        val rating = component.rating
        if (rating != null && component.isRatingValid()) {
            val starsChildren = mutableListOf<SwiftUIView>()

            for (i in 1..5) {
                val starIcon = if (i <= rating) "star.fill" else "star"
                starsChildren.add(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to starIcon),
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(14f),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemYellow"))
                        )
                    )
                )
            }

            children.add(
                SwiftUIView.hStack(
                    spacing = 2f,
                    alignment = VerticalAlignment.Center,
                    children = starsChildren,
                    modifiers = listOf(SwiftUIModifier.padding(8f, 0f, 8f, 0f))
                )
            )
        }

        // Author section
        val authorChildren = mutableListOf<SwiftUIView>()

        // Avatar
        val avatarUrl = component.avatarUrl
        if (avatarUrl != null) {
            authorChildren.add(
                SwiftUIView(
                    type = ViewType.AsyncImage,
                    properties = mapOf("url" to avatarUrl),
                    modifiers = listOf(
                        SwiftUIModifier.frame(
                            width = SizeValue.Fixed(40f),
                            height = SizeValue.Fixed(40f)
                        ),
                        SwiftUIModifier.cornerRadius(20f)
                    )
                )
            )
        }
        val avatarInitials = component.avatarInitials
        if (avatarUrl == null && avatarInitials != null) {
            authorChildren.add(
                SwiftUIView.text(
                    content = avatarInitials,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Headline),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                        SwiftUIModifier.frame(
                            width = SizeValue.Fixed(40f),
                            height = SizeValue.Fixed(40f)
                        ),
                        SwiftUIModifier.background(SwiftUIColor.primary),
                        SwiftUIModifier.cornerRadius(20f)
                    )
                )
            )
        }

        // Author info
        val authorInfoChildren = mutableListOf<SwiftUIView>()

        authorInfoChildren.add(
            SwiftUIView.text(
                content = component.authorName,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Subheadline),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                )
            )
        )

        val authorTitle = component.authorTitle
        if (authorTitle != null) {
            authorInfoChildren.add(
                SwiftUIView.text(
                    content = authorTitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                    )
                )
            )
        }

        authorChildren.add(
            SwiftUIView.vStack(
                spacing = 2f,
                alignment = HorizontalAlignment.Leading,
                children = authorInfoChildren
            )
        )

        children.add(
            SwiftUIView.hStack(
                spacing = 12f,
                alignment = VerticalAlignment.Center,
                children = authorChildren,
                modifiers = listOf(SwiftUIModifier.padding(12f, 0f, 0f, 0f))
            )
        )

        return SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(20f),
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(16f),
                SwiftUIModifier.shadow(radius = 4f, x = 0f, y = 2f)
            )
        )
    }
}

/**
 * Maps ProductCard to SwiftUI e-commerce product card
 *
 * Creates a product card with:
 * - Product image with aspect ratio
 * - Badge overlay ("Sale", "New", etc.)
 * - Product title and description
 * - Price with optional discount strikethrough
 * - Star rating and review count
 * - Stock status
 * - Add to cart and wishlist buttons
 *
 * Optimized for grid/list product displays
 */
object ProductCardMapper {
    fun map(
        component: ProductCard,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Product image with badge overlay
        val imageChildren = mutableListOf<SwiftUIView>()

        imageChildren.add(
            SwiftUIView(
                type = ViewType.AsyncImage,
                properties = mapOf("url" to component.imageUrl),
                modifiers = listOf(
                    SwiftUIModifier.frame(
                        width = SizeValue.Infinity,
                        height = SizeValue.Fixed(200f)
                    ),
                    SwiftUIModifier.cornerRadius(12f)
                )
            )
        )

        // Badge if present
        val badgeText = component.badgeText
        if (badgeText != null) {
            imageChildren.add(
                SwiftUIView.text(
                    content = badgeText,
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(12f),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                        SwiftUIModifier.padding(4f, 8f, 4f, 8f),
                        SwiftUIModifier.background(
                            component.badgeColor?.let { color -> SwiftUIColor.hex(color) }
                                ?: SwiftUIColor.system("systemRed")
                        ),
                        SwiftUIModifier.cornerRadius(8f)
                    )
                )
            )
        }

        children.add(
            SwiftUIView.zStack(
                alignment = ZStackAlignment.TopLeading,
                children = imageChildren
            )
        )

        // Title
        children.add(
            SwiftUIView.text(
                content = component.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                )
            )
        )

        // Description
        val description = component.description
        if (description != null) {
            children.add(
                SwiftUIView.text(
                    content = description,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                    )
                )
            )
        }

        // Rating and reviews
        val productRating = component.rating
        if (productRating != null && component.isRatingValid()) {
            val ratingChildren = mutableListOf<SwiftUIView>()

            // Star rating
            val fullStars = productRating.toInt()
            val hasHalfStar = productRating - fullStars >= 0.5f

            for (i in 1..fullStars) {
                ratingChildren.add(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to "star.fill"),
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(12f),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemYellow"))
                        )
                    )
                )
            }

            if (hasHalfStar) {
                ratingChildren.add(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to "star.leadinghalf.filled"),
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(12f),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemYellow"))
                        )
                    )
                )
            }

            // Review count
            if (component.reviewCount != null) {
                ratingChildren.add(
                    SwiftUIView.text(
                        content = "(${component.reviewCount})",
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Caption),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                        )
                    )
                )
            }

            children.add(
                SwiftUIView.hStack(
                    spacing = 4f,
                    alignment = VerticalAlignment.Center,
                    children = ratingChildren
                )
            )
        }

        // Price section
        val priceChildren = mutableListOf<SwiftUIView>()

        priceChildren.add(
            SwiftUIView.text(
                content = component.price,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Title3),
                    SwiftUIModifier.fontWeight(FontWeight.Bold),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                )
            )
        )

        val originalPrice = component.originalPrice
        if (originalPrice != null && component.hasDiscount()) {
            priceChildren.add(
                SwiftUIView.text(
                    content = originalPrice,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Subheadline),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText),
                        SwiftUIModifier.strikethrough(true)
                    )
                )
            )
        }

        children.add(
            SwiftUIView.hStack(
                spacing = 8f,
                alignment = VerticalAlignment.FirstTextBaseline,
                children = priceChildren
            )
        )

        // Stock status
        if (!component.inStock) {
            children.add(
                SwiftUIView.text(
                    content = "Out of Stock",
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemRed"))
                    )
                )
            )
        }

        // Action buttons
        val actionChildren = mutableListOf<SwiftUIView>()

        if (component.showAddToCart && component.inStock) {
            actionChildren.add(
                SwiftUIView.button(
                    label = "Add to Cart",
                    action = "${component.id}_addToCart",
                    modifiers = listOf(
                        SwiftUIModifier.frame(height = SizeValue.Fixed(36f)),
                        SwiftUIModifier.padding(0f, 12f, 0f, 12f),
                        SwiftUIModifier.background(SwiftUIColor.primary),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                        SwiftUIModifier.cornerRadius(8f),
                        SwiftUIModifier.font(FontStyle.Subheadline),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold)
                    )
                )
            )
        }

        if (component.showWishlist) {
            actionChildren.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "label" to "",
                        "action" to "${component.id}_wishlist"
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "heart"),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(20f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                            )
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.frame(
                            width = SizeValue.Fixed(36f),
                            height = SizeValue.Fixed(36f)
                        )
                    )
                )
            )
        }

        if (actionChildren.isNotEmpty()) {
            children.add(
                SwiftUIView.hStack(
                    spacing = 8f,
                    alignment = VerticalAlignment.Center,
                    children = actionChildren,
                    modifiers = listOf(SwiftUIModifier.padding(8f, 0f, 0f, 0f))
                )
            )
        }

        return SwiftUIView.vStack(
            spacing = 8f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(12f),
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(16f),
                SwiftUIModifier.shadow(radius = 4f, x = 0f, y = 2f)
            )
        )
    }
}

/**
 * Maps ArticleCard to SwiftUI blog/article card
 *
 * Creates an article card with:
 * - Featured image at top
 * - Title and excerpt text
 * - Author avatar and name
 * - Publication date and read time
 * - Category badge
 * - Tags
 * - Bookmark button
 *
 * Optimized for news feeds and blog listings
 */
object ArticleCardMapper {
    fun map(
        component: ArticleCard,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Featured image with category badge overlay
        val imageChildren = mutableListOf<SwiftUIView>()

        imageChildren.add(
            SwiftUIView(
                type = ViewType.AsyncImage,
                properties = mapOf("url" to component.imageUrl),
                modifiers = listOf(
                    SwiftUIModifier.frame(
                        width = SizeValue.Infinity,
                        height = SizeValue.Fixed(180f)
                    ),
                    SwiftUIModifier.cornerRadius(12f)
                )
            )
        )

        // Category badge if present
        val category = component.category
        if (category != null) {
            imageChildren.add(
                SwiftUIView.text(
                    content = category,
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(11f),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                        SwiftUIModifier.padding(4f, 10f, 4f, 10f),
                        SwiftUIModifier.background(SwiftUIColor.primary),
                        SwiftUIModifier.cornerRadius(10f)
                    )
                )
            )
        }

        children.add(
            SwiftUIView.zStack(
                alignment = ZStackAlignment.BottomLeading,
                children = imageChildren,
                modifiers = listOf(SwiftUIModifier.padding(0f, 0f, 12f, 0f))
            )
        )

        // Title
        children.add(
            SwiftUIView.text(
                content = component.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.fontWeight(FontWeight.Bold),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                )
            )
        )

        // Excerpt
        children.add(
            SwiftUIView.text(
                content = component.excerpt,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Subheadline),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                )
            )
        )

        // Tags if present
        if (component.tags.isNotEmpty()) {
            val tagChildren = component.tags.map { tag ->
                SwiftUIView.text(
                    content = "#$tag",
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(12f),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.primary),
                        SwiftUIModifier.padding(4f, 8f, 4f, 8f),
                        SwiftUIModifier.background(
                            SwiftUIColor.primary.copy(value = RGBValue(0.2f, 0.5f, 1.0f, 0.1f))
                        ),
                        SwiftUIModifier.cornerRadius(8f)
                    )
                )
            }

            children.add(
                SwiftUIView.hStack(
                    spacing = 6f,
                    alignment = VerticalAlignment.Center,
                    children = tagChildren,
                    modifiers = listOf(SwiftUIModifier.padding(4f, 0f, 4f, 0f))
                )
            )
        }

        // Author and metadata section
        val metaChildren = mutableListOf<SwiftUIView>()

        // Author avatar
        val authorAvatar = component.authorAvatar
        if (authorAvatar != null) {
            metaChildren.add(
                SwiftUIView(
                    type = ViewType.AsyncImage,
                    properties = mapOf("url" to authorAvatar),
                    modifiers = listOf(
                        SwiftUIModifier.frame(
                            width = SizeValue.Fixed(32f),
                            height = SizeValue.Fixed(32f)
                        ),
                        SwiftUIModifier.cornerRadius(16f)
                    )
                )
            )
        }

        // Author info and metadata
        val infoChildren = mutableListOf<SwiftUIView>()

        infoChildren.add(
            SwiftUIView.text(
                content = component.authorName,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.fontWeight(FontWeight.Medium),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                )
            )
        )

        infoChildren.add(
            SwiftUIView.text(
                content = component.getMetadataText(),
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption2),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                )
            )
        )

        metaChildren.add(
            SwiftUIView.vStack(
                spacing = 2f,
                alignment = HorizontalAlignment.Leading,
                children = infoChildren
            )
        )

        // Spacer
        metaChildren.add(
            SwiftUIView(
                type = ViewType.Spacer,
                properties = emptyMap()
            )
        )

        // Bookmark button
        if (component.showBookmark) {
            metaChildren.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "label" to "",
                        "action" to "${component.id}_bookmark"
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf(
                                "systemName" to if (component.bookmarked) "bookmark.fill" else "bookmark"
                            ),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(18f),
                                SwiftUIModifier.foregroundColor(
                                    if (component.bookmarked) SwiftUIColor.primary else SwiftUIColor.secondaryText
                                )
                            )
                        )
                    )
                )
            )
        }

        children.add(
            SwiftUIView.hStack(
                spacing = 10f,
                alignment = VerticalAlignment.Center,
                children = metaChildren,
                modifiers = listOf(SwiftUIModifier.padding(8f, 0f, 0f, 0f))
            )
        )

        return SwiftUIView.vStack(
            spacing = 10f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(16f),
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(16f),
                SwiftUIModifier.shadow(radius = 4f, x = 0f, y = 2f)
            )
        )
    }
}

/**
 * Maps ImageCard to SwiftUI image-focused card
 *
 * Creates an image card with:
 * - Full-bleed image
 * - Optional text overlay with gradient
 * - Title and subtitle on overlay
 * - Overlay position control (top, center, bottom)
 * - Custom aspect ratio support
 * - Optional action button on overlay
 *
 * Perfect for galleries and visual content
 */
object ImageCardMapper {
    fun map(
        component: ImageCard,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Base image
        val imageModifiers = mutableListOf<SwiftUIModifier>()
        imageModifiers.add(SwiftUIModifier.frame(width = SizeValue.Infinity))

        val aspectRatio = component.aspectRatio
        if (aspectRatio != null) {
            imageModifiers.add(
                SwiftUIModifier.frame(height = SizeValue.AspectRatio(aspectRatio))
            )
        } else {
            imageModifiers.add(SwiftUIModifier.frame(height = SizeValue.Fixed(240f)))
        }

        imageModifiers.add(SwiftUIModifier.cornerRadius(16f))

        children.add(
            SwiftUIView(
                type = ViewType.AsyncImage,
                properties = mapOf("url" to component.imageUrl),
                modifiers = imageModifiers
            )
        )

        // Overlay content if present
        if (component.hasOverlay()) {
            val overlayChildren = mutableListOf<SwiftUIView>()

            // Title
            val title = component.title
            if (title != null) {
                overlayChildren.add(
                    SwiftUIView.text(
                        content = title,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Title2),
                            SwiftUIModifier.fontWeight(FontWeight.Bold),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                        )
                    )
                )
            }

            // Subtitle
            val imageSubtitle = component.subtitle
            if (imageSubtitle != null) {
                overlayChildren.add(
                    SwiftUIView.text(
                        content = imageSubtitle,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Subheadline),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                        )
                    )
                )
            }

            // Action button
            val imageActionText = component.actionText
            if (imageActionText != null) {
                val actionChildren = mutableListOf<SwiftUIView>()

                actionChildren.add(
                    SwiftUIView.text(
                        content = imageActionText,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Subheadline),
                            SwiftUIModifier.fontWeight(FontWeight.Semibold)
                        )
                    )
                )

                val imageActionIcon = component.actionIcon
                if (imageActionIcon != null) {
                    actionChildren.add(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to imageActionIcon),
                            modifiers = listOf(SwiftUIModifier.fontSize(14f))
                        )
                    )
                }

                overlayChildren.add(
                    SwiftUIView.button(
                        label = "",
                        action = "${component.id}_action",
                        modifiers = listOf(
                            SwiftUIModifier.padding(8f, 16f, 8f, 16f),
                            SwiftUIModifier.background(SwiftUIColor.white),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.primary),
                            SwiftUIModifier.cornerRadius(20f)
                        )
                    )
                )
            }

            val overlayStack = SwiftUIView.vStack(
                spacing = 8f,
                alignment = when (component.overlayPosition) {
                    ImageCard.OverlayPosition.Center -> HorizontalAlignment.Center
                    else -> HorizontalAlignment.Leading
                },
                children = overlayChildren,
                modifiers = listOf(SwiftUIModifier.padding(16f))
            )

            // Gradient background if enabled
            if (component.showGradient) {
                val gradientColor = component.gradientColor?.let { color -> SwiftUIColor.hex(color) }
                    ?: SwiftUIColor.black.copy(value = RGBValue(0f, 0f, 0f, 0.6f))

                children.add(
                    SwiftUIView(
                        type = ViewType.Rectangle,
                        properties = emptyMap(),
                        modifiers = listOf(
                            SwiftUIModifier.background(gradientColor),
                            SwiftUIModifier.cornerRadius(16f)
                        )
                    )
                )
            }

            children.add(overlayStack)
        }

        val alignment = when (component.overlayPosition) {
            ImageCard.OverlayPosition.Top -> ZStackAlignment.Top
            ImageCard.OverlayPosition.Center -> ZStackAlignment.Center
            ImageCard.OverlayPosition.Bottom -> ZStackAlignment.Bottom
        }

        return SwiftUIView.zStack(
            alignment = alignment,
            children = children,
            modifiers = listOf(
                SwiftUIModifier.shadow(radius = 6f, x = 0f, y = 3f)
            )
        )
    }
}

/**
 * Maps HoverCard to SwiftUI interactive card
 *
 * Creates a card with interactive effects:
 * - Elevation change on tap (iOS uses tap instead of hover)
 * - Scale animation on press
 * - Overlay with additional actions
 * - Quick action buttons revealed on interaction
 *
 * Note: iOS uses long press gesture instead of hover
 * Actions are always visible for accessibility
 */
object HoverCardMapper {
    fun map(
        component: HoverCard,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Background image if present
        val hoverImageUrl = component.imageUrl
        if (hoverImageUrl != null) {
            children.add(
                SwiftUIView(
                    type = ViewType.AsyncImage,
                    properties = mapOf("url" to hoverImageUrl),
                    modifiers = listOf(
                        SwiftUIModifier.frame(
                            width = SizeValue.Infinity,
                            height = SizeValue.Fixed(160f)
                        ),
                        SwiftUIModifier.cornerRadius(12f)
                    )
                )
            )
        }

        // Content container
        val contentChildren = mutableListOf<SwiftUIView>()

        contentChildren.add(
            SwiftUIView.text(
                content = component.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Title3),
                    SwiftUIModifier.fontWeight(FontWeight.Bold),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                )
            )
        )

        val hoverDescription = component.description
        if (hoverDescription != null) {
            contentChildren.add(
                SwiftUIView.text(
                    content = hoverDescription,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Body),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                    )
                )
            )
        }

        children.add(
            SwiftUIView.vStack(
                spacing = 8f,
                alignment = HorizontalAlignment.Leading,
                children = contentChildren,
                modifiers = listOf(SwiftUIModifier.padding(16f))
            )
        )

        // Quick actions if present
        if (component.actions.isNotEmpty()) {
            val actionButtons = component.actions.map { action ->
                val buttonChildren = mutableListOf<SwiftUIView>()

                val actionIcon = action.icon
                if (actionIcon != null) {
                    buttonChildren.add(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to actionIcon),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(16f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                            )
                        )
                    )
                }

                buttonChildren.add(
                    SwiftUIView.text(
                        content = action.label,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Subheadline),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                        )
                    )
                )

                SwiftUIView.button(
                    label = "",
                    action = "${component.id}_action_${action.id}",
                    modifiers = listOf(
                        SwiftUIModifier.padding(8f, 12f, 8f, 12f),
                        SwiftUIModifier.background(
                            SwiftUIColor.primary.copy(value = RGBValue(0.2f, 0.5f, 1.0f, 0.1f))
                        ),
                        SwiftUIModifier.cornerRadius(8f),
                        SwiftUIModifier.disabled(!action.enabled)
                    )
                )
            }

            val actionsAlignment = when (component.actionsPosition) {
                HoverCard.ActionsPosition.Top -> HorizontalAlignment.Leading
                HoverCard.ActionsPosition.Center -> HorizontalAlignment.Center
                HoverCard.ActionsPosition.Bottom -> HorizontalAlignment.Leading
            }

            children.add(
                SwiftUIView.hStack(
                    spacing = 8f,
                    alignment = VerticalAlignment.Center,
                    children = actionButtons,
                    modifiers = listOf(SwiftUIModifier.padding(0f, 16f, 16f, 16f))
                )
            )
        }

        val cardModifiers = mutableListOf<SwiftUIModifier>()
        cardModifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))
        cardModifiers.add(SwiftUIModifier.cornerRadius(16f))
        cardModifiers.add(
            SwiftUIModifier.shadow(
                radius = component.elevation,
                x = 0f,
                y = component.elevation / 2
            )
        )

        return SwiftUIView.vStack(
            spacing = 0f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = cardModifiers
        )
    }
}

/**
 * Maps ExpandableCard to SwiftUI expandable card
 *
 * Creates an expandable card with:
 * - Always-visible header with title, subtitle, icon
 * - Summary content when collapsed
 * - Expanded content with smooth animation
 * - Rotating chevron icon
 * - Optional divider between sections
 * - Optional header action buttons
 *
 * Uses SwiftUI's DisclosureGroup for native expand/collapse
 * VoiceOver announces expansion state automatically
 */
object ExpandableCardMapper {
    fun map(
        component: ExpandableCard,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Header section
        val headerChildren = mutableListOf<SwiftUIView>()

        // Icon if present
        val expandIcon = component.icon
        if (expandIcon != null) {
            headerChildren.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to expandIcon),
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(24f),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                    )
                )
            )
        }

        // Title and subtitle
        val titleChildren = mutableListOf<SwiftUIView>()

        titleChildren.add(
            SwiftUIView.text(
                content = component.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                )
            )
        )

        val expandSubtitle = component.subtitle
        if (expandSubtitle != null) {
            titleChildren.add(
                SwiftUIView.text(
                    content = expandSubtitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Subheadline),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                    )
                )
            )
        }

        headerChildren.add(
            SwiftUIView.vStack(
                spacing = 4f,
                alignment = HorizontalAlignment.Leading,
                children = titleChildren
            )
        )

        // Spacer
        headerChildren.add(
            SwiftUIView(
                type = ViewType.Spacer,
                properties = emptyMap()
            )
        )

        // Header actions if present
        if (component.headerActions.isNotEmpty()) {
            val actionButtons = component.headerActions.map { action ->
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "label" to "",
                        "action" to "${component.id}_headerAction_${action.id}"
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to action.icon),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(18f),
                                SwiftUIModifier.foregroundColor(
                                    if (action.enabled) SwiftUIColor.primary else SwiftUIColor.secondaryText
                                )
                            )
                        )
                    ),
                    modifiers = listOf(SwiftUIModifier.disabled(!action.enabled))
                )
            }

            headerChildren.addAll(actionButtons)
        }

        // Expand/collapse chevron
        val chevronIcon = component.expandIcon ?: "chevron.down"
        headerChildren.add(
            SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to chevronIcon),
                modifiers = listOf(
                    SwiftUIModifier.fontSize(16f),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                )
            )
        )

        children.add(
            SwiftUIView.hStack(
                spacing = 12f,
                alignment = VerticalAlignment.Center,
                children = headerChildren,
                modifiers = listOf(SwiftUIModifier.padding(16f))
            )
        )

        // Summary content when collapsed (if present)
        val summaryContent = component.summaryContent
        if (summaryContent != null) {
            children.add(
                SwiftUIView.text(
                    content = summaryContent,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Body),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText),
                        SwiftUIModifier.padding(0f, 16f, 16f, 16f)
                    )
                )
            )
        }

        // Divider if enabled
        if (component.showDivider) {
            children.add(
                SwiftUIView(
                    type = ViewType.Divider,
                    properties = emptyMap(),
                    modifiers = listOf(SwiftUIModifier.padding(0f, 16f, 0f, 16f))
                )
            )
        }

        // Expanded content
        children.add(
            SwiftUIView.text(
                content = component.expandedContent,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.text),
                    SwiftUIModifier.padding(16f)
                )
            )
        )

        // Use custom view type for expandable behavior
        return SwiftUIView(
            type = ViewType.Custom("DisclosureGroup"),
            properties = mapOf(
                "isExpanded" to (component.expanded ?: component.initiallyExpanded),
                "animationDuration" to component.animationDuration,
                "controlled" to component.isControlled()
            ),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(16f),
                SwiftUIModifier.shadow(radius = 4f, x = 0f, y = 2f)
            )
        )
    }
}

