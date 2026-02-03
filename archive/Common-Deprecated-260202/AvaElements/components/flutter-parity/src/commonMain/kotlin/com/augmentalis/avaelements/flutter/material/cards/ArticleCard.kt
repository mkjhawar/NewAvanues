package com.augmentalis.avaelements.flutter.material.cards

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ArticleCard component - Flutter Material parity
 *
 * A Material Design 3 card for displaying blog/news articles with image, title, excerpt, and metadata.
 * Commonly used in news feeds, blog listings, and content grids.
 *
 * **Web Equivalent:** `ArticleCard` (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Featured image with aspect ratio control
 * - Article title and excerpt
 * - Author information with avatar
 * - Publication date and read time
 * - Category/tag badges
 * - Bookmark/save action
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ArticleCard(
 *     imageUrl = "https://example.com/article.jpg",
 *     title = "10 Tips for Better Productivity",
 *     excerpt = "Learn how to maximize your productivity with these proven strategies...",
 *     authorName = "John Doe",
 *     authorAvatar = "https://example.com/avatar.jpg",
 *     publishedDate = "2024-01-15",
 *     readTime = "5 min read",
 *     category = "Productivity",
 *     tags = listOf("Tips", "Work"),
 *     onPressed = {
 *         // Navigate to full article
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property imageUrl Featured image URL
 * @property title Article title
 * @property excerpt Article excerpt/summary
 * @property authorName Author's name
 * @property authorAvatar Optional author avatar URL
 * @property publishedDate Publication date string
 * @property readTime Optional estimated read time
 * @property category Optional category/section
 * @property tags Optional list of tags
 * @property showBookmark Whether to show bookmark button
 * @property bookmarked Whether article is bookmarked
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when card is pressed (not serialized)
 * @property onBookmark Callback invoked when bookmark is toggled (not serialized)
 * @property style Optional card style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class ArticleCard(
    override val type: String = "ArticleCard",
    override val id: String? = null,
    val imageUrl: String,
    val title: String,
    val excerpt: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val publishedDate: String,
    val readTime: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val showBookmark: Boolean = true,
    val bookmarked: Boolean = false,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    @Transient
    val onBookmark: ((Boolean) -> Unit)? = null,
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
        val base = contentDescription ?: "Article: $title"
        val author = "by $authorName"
        val date = "published $publishedDate"
        val time = if (readTime != null) ", $readTime" else ""
        val bookmark = if (bookmarked) ", bookmarked" else ""
        return "$base, $author, $date$time$bookmark"
    }

    /**
     * Get formatted metadata line
     */
    fun getMetadataText(): String {
        val parts = mutableListOf<String>()
        parts.add(publishedDate)
        if (readTime != null) parts.add(readTime)
        return parts.joinToString(" • ")
    }

    companion object {
        /**
         * Create a simple article card
         */
        fun simple(
            imageUrl: String,
            title: String,
            excerpt: String,
            authorName: String,
            publishedDate: String,
            onPressed: (() -> Unit)? = null
        ) = ArticleCard(
            imageUrl = imageUrl,
            title = title,
            excerpt = excerpt,
            authorName = authorName,
            publishedDate = publishedDate,
            onPressed = onPressed
        )

        /**
         * Create an article card with category and tags
         */
        fun categorized(
            imageUrl: String,
            title: String,
            excerpt: String,
            authorName: String,
            publishedDate: String,
            category: String,
            tags: List<String> = emptyList(),
            onPressed: (() -> Unit)? = null
        ) = ArticleCard(
            imageUrl = imageUrl,
            title = title,
            excerpt = excerpt,
            authorName = authorName,
            publishedDate = publishedDate,
            category = category,
            tags = tags,
            onPressed = onPressed
        )
    }
}
