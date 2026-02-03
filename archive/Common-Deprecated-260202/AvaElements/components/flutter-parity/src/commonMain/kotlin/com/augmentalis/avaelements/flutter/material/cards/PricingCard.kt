package com.augmentalis.avaelements.flutter.material.cards

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * PricingCard component - Flutter Material parity
 *
 * A Material Design 3 card for displaying pricing tiers with features, price, and CTA button.
 * Commonly used in pricing pages, subscription flows, and feature comparison tables.
 *
 * **Web Equivalent:** `PricingCard` (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Configurable title and subtitle for tier name
 * - Prominent price display with customizable period
 * - Feature list with optional icons
 * - Call-to-action button
 * - Highlighted/featured tier support
 * - Ribbon/badge for "Best Value", "Popular", etc.
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * PricingCard(
 *     title = "Professional",
 *     subtitle = "For growing teams",
 *     price = "$29",
 *     period = "per month",
 *     features = listOf(
 *         "Unlimited projects",
 *         "Advanced analytics",
 *         "Priority support"
 *     ),
 *     buttonText = "Get Started",
 *     highlighted = true,
 *     ribbonText = "Best Value",
 *     onPressed = {
 *         // Handle subscription
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Pricing tier title (e.g., "Pro", "Enterprise")
 * @property subtitle Optional subtitle/description
 * @property price Price amount (e.g., "$29", "Free")
 * @property period Optional period descriptor (e.g., "per month", "per year")
 * @property currency Optional currency symbol override
 * @property features List of feature descriptions
 * @property featureIcons Optional icons for features
 * @property buttonText Call-to-action button text
 * @property buttonEnabled Whether the button is enabled
 * @property highlighted Whether this tier is featured/highlighted
 * @property ribbonText Optional ribbon text (e.g., "Popular", "Best Value")
 * @property ribbonColor Optional ribbon color
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when CTA button is pressed (not serialized)
 * @property style Optional card style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class PricingCard(
    override val type: String = "PricingCard",
    override val id: String? = null,
    val title: String,
    val subtitle: String? = null,
    val price: String,
    val period: String? = null,
    val currency: String? = null,
    val features: List<String> = emptyList(),
    val featureIcons: List<String>? = null,
    val buttonText: String,
    val buttonEnabled: Boolean = true,
    val highlighted: Boolean = false,
    val ribbonText: String? = null,
    val ribbonColor: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "$title pricing tier"
        val priceInfo = if (period != null) "$price $period" else price
        val highlight = if (highlighted) ", featured" else ""
        val ribbon = if (ribbonText != null) ", $ribbonText" else ""
        return "$base, $priceInfo$highlight$ribbon"
    }

    companion object {
        /**
         * Create a simple pricing card
         */
        fun simple(
            title: String,
            price: String,
            features: List<String>,
            buttonText: String,
            onPressed: (() -> Unit)? = null
        ) = PricingCard(
            title = title,
            price = price,
            features = features,
            buttonText = buttonText,
            onPressed = onPressed
        )

        /**
         * Create a highlighted/featured pricing card
         */
        fun featured(
            title: String,
            price: String,
            features: List<String>,
            buttonText: String,
            ribbonText: String = "Popular",
            onPressed: (() -> Unit)? = null
        ) = PricingCard(
            title = title,
            price = price,
            features = features,
            buttonText = buttonText,
            highlighted = true,
            ribbonText = ribbonText,
            onPressed = onPressed
        )
    }
}
