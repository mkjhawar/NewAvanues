package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.cards.*
import com.augmentalis.avaelements.flutter.material.feedback.HoverCard
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Material Card Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity specialized card components to SwiftUI equivalents.
 * All cards use VStack/HStack layouts with Material Design 3 styling.
 *
 * Components:
 * - PricingCard → VStack with price, features, and CTA button
 * - FeatureCard → VStack with icon, title, description
 * - TestimonialCard → VStack with quote, author info
 * - ProductCard → VStack with image, title, price, actions
 * - ArticleCard → VStack with image, title, excerpt, metadata
 * - ImageCard → ZStack with image and overlay content
 * - HoverCard → Button with VStack content (simulated hover)
 * - ExpandableCard → DisclosureGroup with card styling
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// PRICING CARD
// ============================================

object PricingCardMapper {
    fun map(component: PricingCard, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Card styling
        modifiers.add(SwiftUIModifier.padding(16f, 16f, 16f, 16f))
        modifiers.add(SwiftUIModifier.background(
            if (component.highlighted) SwiftUIColor.system("secondarySystemGroupedBackground")
            else SwiftUIColor.system("systemBackground")
        ))
        modifiers.add(SwiftUIModifier(
            type = ModifierType.Custom,
            value = mapOf("cornerRadius" to 12f)
        ))
        modifiers.add(SwiftUIModifier.shadow(
            color = SwiftUIColor.rgb(0f, 0f, 0f, 0.1f),
            radius = if (component.highlighted) 8f else 4f,
            x = 0f,
            y = 2f
        ))

        val children = mutableListOf<SwiftUIView>()

        // Ribbon if present
        component.ribbonText?.let { ribbonText ->
            children.add(SwiftUIView.text(
                content = ribbonText,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                    SwiftUIModifier.padding(4f, 8f, 4f, 8f),
                    SwiftUIModifier.background(
                        component.ribbonColor?.let { parseColor(it) }
                            ?: SwiftUIColor.system("accentColor")
                    )
                )
            ))
        }

        // Title
        children.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Title2),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))

        // Subtitle if present
        component.subtitle?.let { subtitle ->
            children.add(SwiftUIView.text(
                content = subtitle,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Subheadline),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                )
            ))
        }

        // Price
        val priceText = "${component.currency ?: ""}${component.price}"
        children.add(SwiftUIView.text(
            content = priceText,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.LargeTitle),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))

        // Period if present
        component.period?.let { period ->
            children.add(SwiftUIView.text(
                content = period,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                )
            ))
        }

        // Features list
        component.features.forEachIndexed { index, feature ->
            val icon = component.featureIcons?.getOrNull(index) ?: "checkmark"
            children.add(SwiftUIView(
                type = ViewType.HStack,
                properties = mapOf("spacing" to 8f),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to icon, "size" to 14f),
                        modifiers = listOf(SwiftUIModifier.foregroundColor(SwiftUIColor.system("accentColor")))
                    ),
                    SwiftUIView.text(content = feature, modifiers = listOf(SwiftUIModifier.font(FontStyle.Body)))
                )
            ))
        }

        // CTA Button
        children.add(SwiftUIView(
            type = ViewType.Custom("Button"),
            properties = mapOf(
                "enabled" to component.buttonEnabled,
                "text" to component.buttonText
            ),
            modifiers = listOf(
                SwiftUIModifier(
                    type = ModifierType.Custom,
                    value = mapOf("buttonStyle" to "borderedProminent")
                )
            )
        ))

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 12f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// FEATURE CARD
// ============================================

object FeatureCardMapper {
    fun map(component: FeatureCard, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.padding(16f, 16f, 16f, 16f))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))
        modifiers.add(SwiftUIModifier(type = ModifierType.Custom, value = mapOf("cornerRadius" to 12f)))
        modifiers.add(SwiftUIModifier.shadow(SwiftUIColor.rgb(0f, 0f, 0f, 0.1f), 4f, 0f, 2f))

        val children = mutableListOf<SwiftUIView>()

        // Icon if present
        component.icon?.let { icon ->
            children.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to icon, "size" to 32f),
                modifiers = listOf(
                    SwiftUIModifier.foregroundColor(
                        component.iconColor?.let { parseColor(it) } ?: SwiftUIColor.system("accentColor")
                    )
                )
            ))
        }

        // Title
        children.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Title3))
        ))

        // Description
        children.add(SwiftUIView.text(
            content = component.description,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "center", "spacing" to 12f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// TESTIMONIAL CARD
// ============================================

object TestimonialCardMapper {
    fun map(component: TestimonialCard, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.padding(16f, 16f, 16f, 16f))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("secondarySystemBackground")))
        modifiers.add(SwiftUIModifier(type = ModifierType.Custom, value = mapOf("cornerRadius" to 12f)))

        val children = mutableListOf<SwiftUIView>()

        // Quote text
        children.add(SwiftUIView.text(
            content = "\"${component.quote}\"",
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
            )
        ))

        // Author info row
        val authorChildren = mutableListOf<SwiftUIView>()

        component.avatarUrl?.let { avatar ->
            authorChildren.add(SwiftUIView(
                type = ViewType.Custom("AsyncImage"),
                properties = mapOf("url" to avatar, "size" to 40f),
                modifiers = listOf(
                    SwiftUIModifier(type = ModifierType.Custom, value = mapOf("clipShape" to "Circle"))
                )
            ))
        }

        val authorTextChildren = mutableListOf<SwiftUIView>()
        authorTextChildren.add(SwiftUIView.text(
            content = component.authorName,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Headline))
        ))

        component.authorTitle?.let { role ->
            authorTextChildren.add(SwiftUIView.text(
                content = role,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                )
            ))
        }

        authorChildren.add(SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 2f),
            children = authorTextChildren
        ))

        children.add(SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 12f),
            children = authorChildren
        ))

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 16f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// PRODUCT CARD
// ============================================

object ProductCardMapper {
    fun map(component: ProductCard, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))
        modifiers.add(SwiftUIModifier(type = ModifierType.Custom, value = mapOf("cornerRadius" to 12f)))
        modifiers.add(SwiftUIModifier.shadow(SwiftUIColor.rgb(0f, 0f, 0f, 0.1f), 4f, 0f, 2f))

        val children = mutableListOf<SwiftUIView>()

        // Image if present
        children.add(SwiftUIView(
            type = ViewType.Custom("AsyncImage"),
            properties = mapOf("url" to component.imageUrl, "height" to 200f),
            modifiers = listOf(
                SwiftUIModifier(type = ModifierType.Custom, value = mapOf("aspectRatio" to 1.5f)),
                SwiftUIModifier(type = ModifierType.Custom, value = mapOf("contentMode" to "fit"))
            )
        ))

        // Content VStack
        val contentChildren = mutableListOf<SwiftUIView>()

        // Title
        contentChildren.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Headline))
        ))

        // Description if present
        component.description?.let { desc ->
            contentChildren.add(SwiftUIView.text(
                content = desc,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Subheadline),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                )
            ))
        }

        // Price and rating row
        val bottomRowChildren = mutableListOf<SwiftUIView>()

        bottomRowChildren.add(SwiftUIView.text(
            content = component.price,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Title3),
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("accentColor"))
            )
        ))

        bottomRowChildren.add(SwiftUIView(type = ViewType.Spacer, properties = emptyMap()))

        component.rating?.let { rating ->
            bottomRowChildren.add(SwiftUIView(
                type = ViewType.HStack,
                properties = mapOf("spacing" to 4f),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to "star.fill", "size" to 12f),
                        modifiers = listOf(SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemYellow")))
                    ),
                    SwiftUIView.text(content = rating.toString(), modifiers = listOf(SwiftUIModifier.font(FontStyle.Caption)))
                )
            ))
        }

        contentChildren.add(SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 8f),
            children = bottomRowChildren
        ))

        children.add(SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = contentChildren,
            modifiers = listOf(SwiftUIModifier.padding(12f, 12f, 12f, 12f))
        ))

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 0f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// ARTICLE CARD
// ============================================

object ArticleCardMapper {
    fun map(component: ArticleCard, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))
        modifiers.add(SwiftUIModifier(type = ModifierType.Custom, value = mapOf("cornerRadius" to 12f)))
        modifiers.add(SwiftUIModifier.shadow(SwiftUIColor.rgb(0f, 0f, 0f, 0.1f), 4f, 0f, 2f))

        val children = mutableListOf<SwiftUIView>()

        // Thumbnail image
        children.add(SwiftUIView(
            type = ViewType.Custom("AsyncImage"),
            properties = mapOf("url" to component.imageUrl, "height" to 160f),
            modifiers = listOf(
                SwiftUIModifier(type = ModifierType.Custom, value = mapOf("contentMode" to "fill"))
            )
        ))

        // Content VStack
        val contentChildren = mutableListOf<SwiftUIView>()

        // Category if present
        component.category?.let { category ->
            contentChildren.add(SwiftUIView.text(
                content = category,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Caption),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("accentColor"))
                )
            ))
        }

        // Title
        contentChildren.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Headline))
        ))

        // Excerpt
        contentChildren.add(SwiftUIView.text(
            content = component.excerpt,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Subheadline),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))

        // Metadata row
        val metadataChildren = mutableListOf<SwiftUIView>()

        metadataChildren.add(SwiftUIView.text(
            content = component.authorName,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Caption))
        ))

        metadataChildren.add(SwiftUIView.text(
            content = " • ${component.publishedDate}",
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Caption),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))

        contentChildren.add(SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 4f),
            children = metadataChildren
        ))

        children.add(SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 8f),
            children = contentChildren,
            modifiers = listOf(SwiftUIModifier.padding(12f, 12f, 12f, 12f))
        ))

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 0f),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// IMAGE CARD
// ============================================

object ImageCardMapper {
    fun map(component: ImageCard, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier(type = ModifierType.Custom, value = mapOf("cornerRadius" to 12f)))
        modifiers.add(SwiftUIModifier.shadow(SwiftUIColor.rgb(0f, 0f, 0f, 0.15f), 6f, 0f, 3f))

        val children = mutableListOf<SwiftUIView>()

        // Background image
        children.add(SwiftUIView(
            type = ViewType.Custom("AsyncImage"),
            properties = mapOf("url" to component.imageUrl, "height" to 300f),
            modifiers = listOf(
                SwiftUIModifier(type = ModifierType.Custom, value = mapOf("contentMode" to "fill"))
            )
        ))

        // Overlay content if present
        component.title?.let { title ->
            val overlayChildren = mutableListOf<SwiftUIView>()

            overlayChildren.add(SwiftUIView.text(
                content = title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Title2),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                )
            ))

            component.subtitle?.let { subtitle ->
                overlayChildren.add(SwiftUIView.text(
                    content = subtitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Subheadline),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                    )
                ))
            }

            children.add(SwiftUIView(
                type = ViewType.VStack,
                properties = mapOf("alignment" to "leading", "spacing" to 8f),
                children = overlayChildren,
                modifiers = listOf(
                    SwiftUIModifier.padding(16f, 16f, 16f, 16f),
                    SwiftUIModifier.background(SwiftUIColor.rgb(0f, 0f, 0f, 0.3f))
                )
            ))
        }

        return SwiftUIView(
            type = ViewType.ZStack,
            properties = mapOf("alignment" to "bottomLeading"),
            children = children,
            modifiers = modifiers,
        )
    }
}

// ============================================
// HOVER CARD
// ============================================

object HoverCardMapper {
    fun map(component: HoverCard, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.padding(16f, 16f, 16f, 16f))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))
        modifiers.add(SwiftUIModifier(type = ModifierType.Custom, value = mapOf("cornerRadius" to 12f)))
        modifiers.add(SwiftUIModifier.shadow(SwiftUIColor.rgb(0f, 0f, 0f, 0.1f), 4f, 0f, 2f))

        val children = mutableListOf<SwiftUIView>()

        // Title
        children.add(SwiftUIView.text(
            content = component.cardTitle,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Headline))
        ))

        // Content
        children.add(SwiftUIView.text(
            content = component.cardContent,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        ))

        return SwiftUIView(
            type = ViewType.Custom("Button"),
            properties = mapOf("style" to "plain"),
            children = listOf(
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf("alignment" to "leading", "spacing" to 8f),
                    children = children,
                    modifiers = modifiers
                )
            ),
        )
    }
}

// ============================================
// EXPANDABLE CARD
// ============================================

object ExpandableCardMapper {
    fun map(component: ExpandableCard, theme: Theme?, renderChild: (Component) -> SwiftUIView): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.add(SwiftUIModifier.padding(16f, 16f, 16f, 16f))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.system("systemBackground")))
        modifiers.add(SwiftUIModifier(type = ModifierType.Custom, value = mapOf("cornerRadius" to 12f)))
        modifiers.add(SwiftUIModifier.shadow(SwiftUIColor.rgb(0f, 0f, 0f, 0.1f), 4f, 0f, 2f))

        // Label view
        val labelChildren = mutableListOf<SwiftUIView>()

        component.icon?.let { icon ->
            labelChildren.add(SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to icon, "size" to 20f)
            ))
        }

        labelChildren.add(SwiftUIView.text(
            content = component.title,
            modifiers = listOf(SwiftUIModifier.font(FontStyle.Headline))
        ))

        val labelView = SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 8f),
            children = labelChildren
        )

        // Expanded content
        val expandedContent = SwiftUIView.text(
            content = component.expandedContent,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
            )
        )

        return SwiftUIView(
            type = ViewType.Custom("DisclosureGroup"),
            properties = mapOf("isExpanded" to component.initiallyExpanded),
            children = listOf(labelView, expandedContent),
            modifiers = modifiers,
        )
    }
}

// ============================================
// Helper Functions
// ============================================

private fun parseColor(colorString: String): SwiftUIColor {
    return when {
        colorString.startsWith("#") -> {
            val hex = colorString.removePrefix("#")
            val r = hex.substring(0, 2).toInt(16) / 255f
            val g = hex.substring(2, 4).toInt(16) / 255f
            val b = hex.substring(4, 6).toInt(16) / 255f
            val a = if (hex.length == 8) hex.substring(6, 8).toInt(16) / 255f else 1f
            SwiftUIColor.rgb(r, g, b, a)
        }
        colorString.equals("primary", ignoreCase = true) -> SwiftUIColor.primary
        colorString.equals("secondary", ignoreCase = true) -> SwiftUIColor.secondary
        colorString.equals("accent", ignoreCase = true) -> SwiftUIColor.system("accentColor")
        else -> SwiftUIColor.system(colorString)
    }
}
