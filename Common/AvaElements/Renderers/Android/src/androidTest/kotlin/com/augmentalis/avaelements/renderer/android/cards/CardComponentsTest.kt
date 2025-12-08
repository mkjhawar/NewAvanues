package com.augmentalis.avaelements.renderer.android.cards

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.augmentalis.avaelements.flutter.material.cards.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test

/**
 * Comprehensive test suite for Android Card components
 *
 * Tests 8 card components with ~40 test cases total covering:
 * - Component rendering
 * - Content display
 * - User interactions
 * - Accessibility
 * - State management
 * - Edge cases
 */
class CardComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== PricingCard Tests (5 tests) ====================

    @Test
    fun testPricingCard_renders() {
        composeTestRule.setContent {
            PricingCardMapper(
                PricingCard(
                    title = "Pro",
                    price = "$29",
                    period = "per month",
                    features = listOf("Feature 1", "Feature 2"),
                    buttonText = "Subscribe"
                )
            )
        }

        composeTestRule.onNodeWithText("Pro").assertIsDisplayed()
        composeTestRule.onNodeWithText("$29").assertIsDisplayed()
        composeTestRule.onNodeWithText("per month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Subscribe").assertIsDisplayed()
    }

    @Test
    fun testPricingCard_displaysFeatures() {
        composeTestRule.setContent {
            PricingCardMapper(
                PricingCard(
                    title = "Basic",
                    price = "Free",
                    features = listOf("10 projects", "Community support", "Basic analytics"),
                    buttonText = "Get Started"
                )
            )
        }

        composeTestRule.onNodeWithText("10 projects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Community support").assertIsDisplayed()
        composeTestRule.onNodeWithText("Basic analytics").assertIsDisplayed()
    }

    @Test
    fun testPricingCard_highlighted() {
        composeTestRule.setContent {
            PricingCardMapper(
                PricingCard(
                    title = "Enterprise",
                    price = "$99",
                    features = listOf("Unlimited"),
                    buttonText = "Contact Sales",
                    highlighted = true,
                    ribbonText = "Popular"
                )
            )
        }

        composeTestRule.onNodeWithText("Popular").assertIsDisplayed()
    }

    @Test
    fun testPricingCard_buttonClick() {
        var clicked = false
        composeTestRule.setContent {
            PricingCardMapper(
                PricingCard(
                    title = "Pro",
                    price = "$29",
                    features = emptyList(),
                    buttonText = "Subscribe",
                    onPressed = { clicked = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Subscribe").performClick()
        assert(clicked)
    }

    @Test
    fun testPricingCard_accessibility() {
        composeTestRule.setContent {
            PricingCardMapper(
                PricingCard(
                    title = "Pro",
                    price = "$29",
                    features = listOf("Feature 1"),
                    buttonText = "Subscribe",
                    contentDescription = "Pro tier pricing"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Pro tier pricing, \$29, featured"))
            .assertExists()
    }

    // ==================== FeatureCard Tests (5 tests) ====================

    @Test
    fun testFeatureCard_renders() {
        composeTestRule.setContent {
            FeatureCardMapper(
                FeatureCard(
                    icon = "rocket",
                    title = "Fast Performance",
                    description = "Lightning fast loading times"
                )
            )
        }

        composeTestRule.onNodeWithText("Fast Performance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lightning fast loading times").assertIsDisplayed()
    }

    @Test
    fun testFeatureCard_verticalLayout() {
        composeTestRule.setContent {
            FeatureCardMapper(
                FeatureCard(
                    icon = "shield",
                    title = "Secure",
                    description = "Bank-grade encryption",
                    layout = FeatureCard.Layout.Vertical
                )
            )
        }

        composeTestRule.onNodeWithText("Secure").assertIsDisplayed()
    }

    @Test
    fun testFeatureCard_withAction() {
        var actionClicked = false
        composeTestRule.setContent {
            FeatureCardMapper(
                FeatureCard(
                    icon = "star",
                    title = "Premium Features",
                    description = "Unlock advanced capabilities",
                    actionText = "Learn More",
                    onActionPressed = { actionClicked = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Learn More").performClick()
        assert(actionClicked)
    }

    @Test
    fun testFeatureCard_clickable() {
        var clicked = false
        composeTestRule.setContent {
            FeatureCardMapper(
                FeatureCard(
                    icon = "info",
                    title = "Info",
                    description = "Click to learn more",
                    onPressed = { clicked = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Info").performClick()
        assert(clicked)
    }

    @Test
    fun testFeatureCard_accessibility() {
        composeTestRule.setContent {
            FeatureCardMapper(
                FeatureCard(
                    icon = "rocket",
                    title = "Fast",
                    description = "Very fast",
                    contentDescription = "Fast performance feature"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Fast performance feature"))
            .assertExists()
    }

    // ==================== TestimonialCard Tests (5 tests) ====================

    @Test
    fun testTestimonialCard_renders() {
        composeTestRule.setContent {
            TestimonialCardMapper(
                TestimonialCard(
                    quote = "Great product!",
                    authorName = "John Doe",
                    authorTitle = "CEO, TechCorp"
                )
            )
        }

        composeTestRule.onNodeWithText("Great product!").assertIsDisplayed()
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("CEO, TechCorp").assertIsDisplayed()
    }

    @Test
    fun testTestimonialCard_withRating() {
        composeTestRule.setContent {
            TestimonialCardMapper(
                TestimonialCard(
                    quote = "Excellent service",
                    authorName = "Jane Smith",
                    rating = 5
                )
            )
        }

        composeTestRule.onNodeWithText("Excellent service").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Smith").assertIsDisplayed()
    }

    @Test
    fun testTestimonialCard_withAvatar() {
        composeTestRule.setContent {
            TestimonialCardMapper(
                TestimonialCard(
                    quote = "Amazing!",
                    authorName = "Bob Johnson",
                    avatarInitials = "BJ"
                )
            )
        }

        composeTestRule.onNodeWithText("BJ").assertIsDisplayed()
    }

    @Test
    fun testTestimonialCard_quoteIcon() {
        composeTestRule.setContent {
            TestimonialCardMapper(
                TestimonialCard(
                    quote = "Fantastic",
                    authorName = "Alice",
                    showQuoteIcon = true
                )
            )
        }

        composeTestRule.onNodeWithText("Fantastic").assertIsDisplayed()
    }

    @Test
    fun testTestimonialCard_accessibility() {
        composeTestRule.setContent {
            TestimonialCardMapper(
                TestimonialCard(
                    quote = "Great!",
                    authorName = "John",
                    rating = 5,
                    contentDescription = "Customer testimonial"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Customer testimonial, rated 5 out of 5 stars: Great!"))
            .assertExists()
    }

    // ==================== ProductCard Tests (6 tests) ====================

    @Test
    fun testProductCard_renders() {
        composeTestRule.setContent {
            ProductCardMapper(
                ProductCard(
                    imageUrl = "https://example.com/product.jpg",
                    title = "Wireless Headphones",
                    price = "$199"
                )
            )
        }

        composeTestRule.onNodeWithText("Wireless Headphones").assertIsDisplayed()
        composeTestRule.onNodeWithText("$199").assertIsDisplayed()
    }

    @Test
    fun testProductCard_withDiscount() {
        composeTestRule.setContent {
            ProductCardMapper(
                ProductCard(
                    imageUrl = "https://example.com/product.jpg",
                    title = "Smart Watch",
                    price = "$299",
                    originalPrice = "$399",
                    badgeText = "Sale"
                )
            )
        }

        composeTestRule.onNodeWithText("Sale").assertIsDisplayed()
        composeTestRule.onNodeWithText("$299").assertIsDisplayed()
        composeTestRule.onNodeWithText("$399").assertIsDisplayed()
    }

    @Test
    fun testProductCard_withRating() {
        composeTestRule.setContent {
            ProductCardMapper(
                ProductCard(
                    imageUrl = "https://example.com/product.jpg",
                    title = "Laptop",
                    price = "$999",
                    rating = 4.5f,
                    reviewCount = 128
                )
            )
        }

        composeTestRule.onNodeWithText("(128)").assertIsDisplayed()
    }

    @Test
    fun testProductCard_outOfStock() {
        composeTestRule.setContent {
            ProductCardMapper(
                ProductCard(
                    imageUrl = "https://example.com/product.jpg",
                    title = "Rare Item",
                    price = "$49",
                    inStock = false
                )
            )
        }

        composeTestRule.onNodeWithText("Out of Stock").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add to Cart").assertDoesNotExist()
    }

    @Test
    fun testProductCard_addToCart() {
        var added = false
        composeTestRule.setContent {
            ProductCardMapper(
                ProductCard(
                    imageUrl = "https://example.com/product.jpg",
                    title = "Phone",
                    price = "$699",
                    onAddToCart = { added = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Add to Cart").performClick()
        assert(added)
    }

    @Test
    fun testProductCard_accessibility() {
        composeTestRule.setContent {
            ProductCardMapper(
                ProductCard(
                    imageUrl = "https://example.com/product.jpg",
                    title = "Tablet",
                    price = "$499",
                    rating = 4f,
                    contentDescription = "Tablet product"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Tablet product, \$499, rated 4.0 out of 5 stars"))
            .assertExists()
    }

    // ==================== ArticleCard Tests (5 tests) ====================

    @Test
    fun testArticleCard_renders() {
        composeTestRule.setContent {
            ArticleCardMapper(
                ArticleCard(
                    imageUrl = "https://example.com/article.jpg",
                    title = "10 Tips for Productivity",
                    excerpt = "Learn how to be more productive...",
                    authorName = "John Writer",
                    publishedDate = "2024-01-15"
                )
            )
        }

        composeTestRule.onNodeWithText("10 Tips for Productivity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Learn how to be more productive...").assertIsDisplayed()
        composeTestRule.onNodeWithText("John Writer").assertIsDisplayed()
    }

    @Test
    fun testArticleCard_withCategory() {
        composeTestRule.setContent {
            ArticleCardMapper(
                ArticleCard(
                    imageUrl = "https://example.com/article.jpg",
                    title = "Tech News",
                    excerpt = "Latest updates",
                    authorName = "Tech Reporter",
                    publishedDate = "2024-01-20",
                    category = "Technology"
                )
            )
        }

        composeTestRule.onNodeWithText("Technology").assertIsDisplayed()
    }

    @Test
    fun testArticleCard_withReadTime() {
        composeTestRule.setContent {
            ArticleCardMapper(
                ArticleCard(
                    imageUrl = "https://example.com/article.jpg",
                    title = "Long Article",
                    excerpt = "Very detailed content",
                    authorName = "Author",
                    publishedDate = "2024-01-10",
                    readTime = "15 min read"
                )
            )
        }

        composeTestRule.onNodeWithText("2024-01-10 • 15 min read").assertIsDisplayed()
    }

    @Test
    fun testArticleCard_bookmark() {
        var bookmarked = false
        composeTestRule.setContent {
            ArticleCardMapper(
                ArticleCard(
                    imageUrl = "https://example.com/article.jpg",
                    title = "Article",
                    excerpt = "Content",
                    authorName = "Author",
                    publishedDate = "2024-01-01",
                    onBookmark = { bookmarked = it }
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Bookmark")).performClick()
        assert(bookmarked)
    }

    @Test
    fun testArticleCard_accessibility() {
        composeTestRule.setContent {
            ArticleCardMapper(
                ArticleCard(
                    imageUrl = "https://example.com/article.jpg",
                    title = "News",
                    excerpt = "Breaking news",
                    authorName = "Reporter",
                    publishedDate = "2024-01-25",
                    contentDescription = "News article card"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("News article card, by Reporter, published 2024-01-25"))
            .assertExists()
    }

    // ==================== ImageCard Tests (4 tests) ====================

    @Test
    fun testImageCard_renders() {
        composeTestRule.setContent {
            ImageCardMapper(
                ImageCard(
                    imageUrl = "https://example.com/photo.jpg",
                    title = "Beautiful Sunset",
                    subtitle = "Photo by Jane"
                )
            )
        }

        composeTestRule.onNodeWithText("Beautiful Sunset").assertIsDisplayed()
        composeTestRule.onNodeWithText("Photo by Jane").assertIsDisplayed()
    }

    @Test
    fun testImageCard_withAction() {
        var actionClicked = false
        composeTestRule.setContent {
            ImageCardMapper(
                ImageCard(
                    imageUrl = "https://example.com/photo.jpg",
                    title = "Landscape",
                    actionText = "View Gallery",
                    onActionPressed = { actionClicked = true }
                )
            )
        }

        composeTestRule.onNodeWithText("View Gallery").performClick()
        assert(actionClicked)
    }

    @Test
    fun testImageCard_clickable() {
        var clicked = false
        composeTestRule.setContent {
            ImageCardMapper(
                ImageCard(
                    imageUrl = "https://example.com/photo.jpg",
                    title = "Mountain",
                    onPressed = { clicked = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Mountain").performClick()
        assert(clicked)
    }

    @Test
    fun testImageCard_accessibility() {
        composeTestRule.setContent {
            ImageCardMapper(
                ImageCard(
                    imageUrl = "https://example.com/photo.jpg",
                    title = "Ocean",
                    contentDescription = "Ocean photograph card"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Ocean photograph card: Ocean"))
            .assertExists()
    }

    // ==================== HoverCard Tests (4 tests) ====================

    @Test
    fun testHoverCard_renders() {
        composeTestRule.setContent {
            HoverCardMapper(
                HoverCard(
                    title = "Project Alpha",
                    description = "Revolutionary project"
                )
            )
        }

        composeTestRule.onNodeWithText("Project Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Revolutionary project").assertIsDisplayed()
    }

    @Test
    fun testHoverCard_withActions() {
        val actions = listOf(
            HoverCard.HoverCardAction("edit", "Edit", "edit"),
            HoverCard.HoverCardAction("delete", "Delete", "delete")
        )

        composeTestRule.setContent {
            HoverCardMapper(
                HoverCard(
                    title = "Document",
                    actions = actions
                )
            )
        }

        // Actions should be visible for accessibility
        composeTestRule.onNode(hasContentDescription("Edit")).assertExists()
        composeTestRule.onNode(hasContentDescription("Delete")).assertExists()
    }

    @Test
    fun testHoverCard_actionClick() {
        var clickedActionId = ""
        val actions = listOf(
            HoverCard.HoverCardAction("share", "Share", "share")
        )

        composeTestRule.setContent {
            HoverCardMapper(
                HoverCard(
                    title = "Item",
                    actions = actions,
                    onActionPressed = { clickedActionId = it }
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Share")).performClick()
        assert(clickedActionId == "share")
    }

    @Test
    fun testHoverCard_accessibility() {
        composeTestRule.setContent {
            HoverCardMapper(
                HoverCard(
                    title = "Gallery Item",
                    description = "Photo collection",
                    contentDescription = "Gallery hover card"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Gallery hover card, Photo collection"))
            .assertExists()
    }

    // ==================== ExpandableCard Tests (6 tests) ====================

    @Test
    fun testExpandableCard_renders() {
        composeTestRule.setContent {
            ExpandableCardMapper(
                ExpandableCard(
                    title = "Advanced Settings",
                    expandedContent = "Detailed settings here"
                )
            )
        }

        composeTestRule.onNodeWithText("Advanced Settings").assertIsDisplayed()
    }

    @Test
    fun testExpandableCard_expand() {
        composeTestRule.setContent {
            ExpandableCardMapper(
                ExpandableCard(
                    title = "FAQ",
                    expandedContent = "Answer to question",
                    initiallyExpanded = false
                )
            )
        }

        composeTestRule.onNodeWithText("Answer to question").assertDoesNotExist()
        composeTestRule.onNode(hasContentDescription("Expand")).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Answer to question").assertExists()
    }

    @Test
    fun testExpandableCard_collapse() {
        composeTestRule.setContent {
            ExpandableCardMapper(
                ExpandableCard(
                    title = "Info",
                    expandedContent = "Details",
                    initiallyExpanded = true
                )
            )
        }

        composeTestRule.onNodeWithText("Details").assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Collapse")).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Details").assertDoesNotExist()
    }

    @Test
    fun testExpandableCard_callback() {
        var isExpanded = false
        composeTestRule.setContent {
            ExpandableCardMapper(
                ExpandableCard(
                    title = "Panel",
                    expandedContent = "Content",
                    onExpansionChanged = { isExpanded = it }
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Expand")).performClick()
        assert(isExpanded)
    }

    @Test
    fun testExpandableCard_withSummary() {
        composeTestRule.setContent {
            ExpandableCardMapper(
                ExpandableCard(
                    title = "Details",
                    summaryContent = "Click to expand",
                    expandedContent = "Full details here"
                )
            )
        }

        composeTestRule.onNodeWithText("Click to expand").assertIsDisplayed()
    }

    @Test
    fun testExpandableCard_accessibility() {
        composeTestRule.setContent {
            ExpandableCardMapper(
                ExpandableCard(
                    title = "Accordion",
                    expandedContent = "Content",
                    contentDescription = "Settings accordion"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Settings accordion, collapsed"))
            .assertExists()
    }
}
