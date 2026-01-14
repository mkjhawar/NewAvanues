package com.augmentalis.avaelements.flutter.material.cards

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ProductCard component - Flutter Material parity
 *
 * A Material Design 3 card for displaying e-commerce products with image, title, price, and rating.
 * Commonly used in product catalogs, shopping grids, and marketplace listings.
 *
 * **Web Equivalent:** `ProductCard` (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Product image with aspect ratio control
 * - Product title and description
 * - Price display with optional discount
 * - Star rating display
 * - Availability/stock status
 * - Add to cart/wishlist actions
 * - Badge for "Sale", "New", etc.
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ProductCard(
 *     imageUrl = "https://example.com/product.jpg",
 *     title = "Premium Headphones",
 *     description = "High-quality wireless headphones",
 *     price = "$199.99",
 *     originalPrice = "$249.99",
 *     rating = 4.5f,
 *     reviewCount = 128,
 *     badgeText = "Sale",
 *     inStock = true,
 *     onPressed = {
 *         // Navigate to product details
 *     },
 *     onAddToCart = {
 *         // Add to cart
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property imageUrl Product image URL
 * @property title Product title/name
 * @property description Optional product description
 * @property price Current price display
 * @property originalPrice Optional original price (for discounts)
 * @property currency Optional currency symbol override
 * @property rating Optional product rating (0-5)
 * @property reviewCount Optional number of reviews
 * @property badgeText Optional badge text ("Sale", "New", etc.)
 * @property badgeColor Optional badge color
 * @property inStock Whether product is in stock
 * @property showAddToCart Whether to show add to cart button
 * @property showWishlist Whether to show wishlist button
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when card is pressed (not serialized)
 * @property onAddToCart Callback invoked when add to cart is pressed (not serialized)
 * @property onWishlist Callback invoked when wishlist is toggled (not serialized)
 * @property style Optional card style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class ProductCard(
    override val type: String = "ProductCard",
    override val id: String? = null,
    val imageUrl: String,
    val title: String,
    val description: String? = null,
    val price: String,
    val originalPrice: String? = null,
    val currency: String? = null,
    val rating: Float? = null,
    val reviewCount: Int? = null,
    val badgeText: String? = null,
    val badgeColor: String? = null,
    val inStock: Boolean = true,
    val showAddToCart: Boolean = true,
    val showWishlist: Boolean = true,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    @Transient
    val onAddToCart: (() -> Unit)? = null,
    @Transient
    val onWishlist: ((Boolean) -> Unit)? = null,
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
        val base = contentDescription ?: title
        val priceInfo = if (originalPrice != null) "$price, was $originalPrice" else price
        val ratingInfo = if (rating != null) ", rated $rating out of 5 stars" else ""
        val stockInfo = if (!inStock) ", out of stock" else ""
        val badge = if (badgeText != null) ", $badgeText" else ""
        return "$base, $priceInfo$ratingInfo$stockInfo$badge"
    }

    /**
     * Check if product has discount
     */
    fun hasDiscount(): Boolean {
        return originalPrice != null && originalPrice != price
    }

    /**
     * Validate rating is in valid range
     */
    fun isRatingValid(): Boolean {
        return rating == null || rating in 0f..5f
    }

    companion object {
        /**
         * Create a simple product card
         */
        fun simple(
            imageUrl: String,
            title: String,
            price: String,
            onPressed: (() -> Unit)? = null
        ) = ProductCard(
            imageUrl = imageUrl,
            title = title,
            price = price,
            onPressed = onPressed
        )

        /**
         * Create a product card with discount
         */
        fun withDiscount(
            imageUrl: String,
            title: String,
            price: String,
            originalPrice: String,
            badgeText: String = "Sale",
            onPressed: (() -> Unit)? = null
        ) = ProductCard(
            imageUrl = imageUrl,
            title = title,
            price = price,
            originalPrice = originalPrice,
            badgeText = badgeText,
            onPressed = onPressed
        )
    }
}
