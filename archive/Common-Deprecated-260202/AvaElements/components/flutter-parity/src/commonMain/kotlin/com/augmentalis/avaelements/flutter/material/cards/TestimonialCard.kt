package com.augmentalis.avaelements.flutter.material.cards

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * TestimonialCard component - Flutter Material parity
 *
 * A Material Design 3 card for displaying user testimonials with avatar, quote, and author info.
 * Commonly used in marketing pages, reviews sections, and social proof displays.
 *
 * **Web Equivalent:** `TestimonialCard` (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - User avatar/photo display
 * - Quotation text with optional formatting
 * - Author name and title/company
 * - Star rating display
 * - Optional quote icon
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * TestimonialCard(
 *     quote = "This product has transformed our workflow completely!",
 *     authorName = "Jane Smith",
 *     authorTitle = "CEO, TechCorp",
 *     avatarUrl = "https://example.com/avatar.jpg",
 *     rating = 5,
 *     showQuoteIcon = true,
 *     onPressed = {
 *         // Navigate to full testimonial
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property quote Testimonial quote text
 * @property authorName Name of the person giving testimonial
 * @property authorTitle Optional title, position, or company
 * @property avatarUrl Optional URL for author's avatar
 * @property avatarInitials Optional initials if no avatar URL
 * @property rating Optional star rating (1-5)
 * @property showQuoteIcon Whether to show quotation mark icon
 * @property quoteIcon Optional custom quote icon
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when card is pressed (not serialized)
 * @property style Optional card style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class TestimonialCard(
    override val type: String = "TestimonialCard",
    override val id: String? = null,
    val quote: String,
    val authorName: String,
    val authorTitle: String? = null,
    val avatarUrl: String? = null,
    val avatarInitials: String? = null,
    val rating: Int? = null,
    val showQuoteIcon: Boolean = true,
    val quoteIcon: String? = null,
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
        val base = contentDescription ?: "Testimonial from $authorName"
        val ratingInfo = if (rating != null) ", rated $rating out of 5 stars" else ""
        return "$base$ratingInfo: $quote"
    }

    /**
     * Validate rating is in valid range
     */
    fun isRatingValid(): Boolean {
        return rating == null || rating in 1..5
    }

    companion object {
        /**
         * Create a simple testimonial card with quote and author
         */
        fun simple(
            quote: String,
            authorName: String,
            authorTitle: String? = null,
            onPressed: (() -> Unit)? = null
        ) = TestimonialCard(
            quote = quote,
            authorName = authorName,
            authorTitle = authorTitle,
            onPressed = onPressed
        )

        /**
         * Create a testimonial card with rating
         */
        fun withRating(
            quote: String,
            authorName: String,
            authorTitle: String? = null,
            rating: Int,
            avatarUrl: String? = null,
            onPressed: (() -> Unit)? = null
        ) = TestimonialCard(
            quote = quote,
            authorName = authorName,
            authorTitle = authorTitle,
            rating = rating,
            avatarUrl = avatarUrl,
            onPressed = onPressed
        )
    }
}
