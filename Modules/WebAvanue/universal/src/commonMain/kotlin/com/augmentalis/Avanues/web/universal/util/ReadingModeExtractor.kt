package com.augmentalis.Avanues.web.universal.util

import kotlinx.serialization.Serializable

/**
 * ReadingModeExtractor - Extracts article content from web pages for reading mode
 *
 * Uses Mozilla Readability algorithm principles:
 * - Identifies main article content
 * - Removes ads, navigation, sidebars, popups
 * - Preserves article images, formatting, and links
 * - Extracts metadata (title, author, date, featured image)
 *
 * Implementation:
 * - Uses JavaScript injection in WebView to run extraction
 * - Returns structured article data
 * - Handles various article formats (news, blogs, documentation)
 *
 * @see ReadingModeArticle for extracted article structure
 */
object ReadingModeExtractor {

    /**
     * Generates JavaScript code to extract article content from current page
     *
     * This JavaScript will be injected into the WebView and returns a JSON string
     * containing the extracted article data.
     *
     * Algorithm:
     * 1. Look for article tags (article, main, .post-content, etc.)
     * 2. Calculate content density (text vs HTML ratio)
     * 3. Find elements with high content density and word count > 300
     * 4. Remove low-value elements (ads, navigation, social, comments)
     * 5. Extract metadata from OpenGraph, meta tags, or page structure
     * 6. Clean and format HTML content
     *
     * @return JavaScript code as a string
     */
    fun getExtractionScript(): String {
        return """
            (function() {
                // Helper: Get text content length (excluding whitespace)
                function getTextLength(element) {
                    return (element.textContent || '').trim().length;
                }

                // Helper: Calculate content density (text / HTML ratio)
                function getContentDensity(element) {
                    const textLength = getTextLength(element);
                    const htmlLength = element.innerHTML.length;
                    return htmlLength > 0 ? textLength / htmlLength : 0;
                }

                // Helper: Count words in text
                function countWords(text) {
                    return text.trim().split(/\s+/).length;
                }

                // Helper: Remove unwanted elements
                function cleanElement(element) {
                    const clone = element.cloneNode(true);

                    // Remove ads, navigation, sidebars, comments, social share, etc.
                    const unwantedSelectors = [
                        '.ad', '.ads', '.advertisement', '.adsbygoogle',
                        'aside', '.sidebar', '.side-bar',
                        'nav', '.navigation', '.nav',
                        '.comment', '.comments', '.comment-section',
                        '.social', '.social-share', '.share-buttons',
                        'header', 'footer',
                        '.popup', '.modal', '.overlay',
                        'script', 'style', 'iframe[src*="ads"]'
                    ];

                    unwantedSelectors.forEach(selector => {
                        clone.querySelectorAll(selector).forEach(el => el.remove());
                    });

                    return clone;
                }

                // Step 1: Try to find article container
                let articleElement = null;

                // Priority 1: Semantic HTML5 article tags
                articleElement = document.querySelector('article');

                // Priority 2: main tag
                if (!articleElement) {
                    articleElement = document.querySelector('main');
                }

                // Priority 3: Common article class names
                if (!articleElement) {
                    const articleSelectors = [
                        '[role="main"]',
                        '.article', '.post', '.entry',
                        '.post-content', '.entry-content', '.article-content',
                        '.content', '.main-content',
                        '#article', '#post', '#content'
                    ];

                    for (const selector of articleSelectors) {
                        const candidate = document.querySelector(selector);
                        if (candidate && countWords(candidate.textContent) > 300) {
                            articleElement = candidate;
                            break;
                        }
                    }
                }

                // Priority 4: Find element with highest content density and word count
                if (!articleElement) {
                    const candidates = document.querySelectorAll('div, section');
                    let bestCandidate = null;
                    let bestScore = 0;

                    candidates.forEach(candidate => {
                        const wordCount = countWords(candidate.textContent);
                        const density = getContentDensity(candidate);
                        const score = wordCount * density;

                        if (wordCount > 300 && score > bestScore) {
                            bestScore = score;
                            bestCandidate = candidate;
                        }
                    });

                    articleElement = bestCandidate;
                }

                // If still no article found, return null
                if (!articleElement) {
                    return JSON.stringify({ error: 'No article content found' });
                }

                // Step 2: Extract metadata

                // Title - try multiple sources
                let title = '';
                title = document.querySelector('meta[property="og:title"]')?.content ||
                        document.querySelector('meta[name="twitter:title"]')?.content ||
                        document.querySelector('h1')?.textContent ||
                        document.title;

                // Author
                let author = '';
                author = document.querySelector('meta[name="author"]')?.content ||
                        document.querySelector('meta[property="article:author"]')?.content ||
                        document.querySelector('[rel="author"]')?.textContent ||
                        document.querySelector('.author')?.textContent ||
                        '';

                // Publish date
                let publishDate = '';
                publishDate = document.querySelector('meta[property="article:published_time"]')?.content ||
                            document.querySelector('meta[name="date"]')?.content ||
                            document.querySelector('time')?.getAttribute('datetime') ||
                            document.querySelector('time')?.textContent ||
                            '';

                // Featured image
                let featuredImage = '';
                featuredImage = document.querySelector('meta[property="og:image"]')?.content ||
                               document.querySelector('meta[name="twitter:image"]')?.content ||
                               articleElement.querySelector('img')?.src ||
                               '';

                // Site name
                let siteName = '';
                siteName = document.querySelector('meta[property="og:site_name"]')?.content ||
                          window.location.hostname;

                // Step 3: Clean article content
                const cleanedArticle = cleanElement(articleElement);
                const articleHtml = cleanedArticle.innerHTML;
                const articleText = cleanedArticle.textContent;

                // Step 4: Extract article images (within article content)
                const images = [];
                cleanedArticle.querySelectorAll('img').forEach(img => {
                    if (img.src) {
                        images.push({
                            src: img.src,
                            alt: img.alt || '',
                            width: img.width || 0,
                            height: img.height || 0
                        });
                    }
                });

                // Step 5: Return structured article data
                return JSON.stringify({
                    title: title.trim(),
                    author: author.trim(),
                    publishDate: publishDate.trim(),
                    featuredImage: featuredImage,
                    siteName: siteName,
                    content: articleHtml,
                    textContent: articleText.trim(),
                    wordCount: countWords(articleText),
                    images: images,
                    url: window.location.href,
                    success: true
                });
            })();
        """.trimIndent()
    }

    /**
     * Checks if the current page is likely an article page
     *
     * Heuristics:
     * - Has article/main tags
     * - Has meta tags for article (og:type = article)
     * - Has sufficient word count (> 300 words)
     * - Not a homepage or search results
     *
     * @return JavaScript code to check if page is an article
     */
    fun getArticleDetectionScript(): String {
        return """
            (function() {
                // Check 1: Has article or main tag
                const hasArticleTag = document.querySelector('article, main') !== null;

                // Check 2: OpenGraph type is article
                const ogType = document.querySelector('meta[property="og:type"]')?.content;
                const isArticleType = ogType === 'article';

                // Check 3: Has sufficient text content
                const bodyText = document.body.textContent || '';
                const wordCount = bodyText.trim().split(/\s+/).length;
                const hasSufficientContent = wordCount > 300;

                // Check 4: Not a homepage (has path beyond /)
                const hasPath = window.location.pathname.length > 1;

                // Check 5: Has common article elements
                const hasArticleElements = document.querySelector('.article, .post, .entry, [role="main"]') !== null;

                // Article is likely if any 2 conditions are true
                const score = [hasArticleTag, isArticleType, hasSufficientContent, hasPath, hasArticleElements]
                    .filter(Boolean).length;

                return JSON.stringify({
                    isArticle: score >= 2,
                    score: score,
                    wordCount: wordCount,
                    hasArticleTag: hasArticleTag,
                    isArticleType: isArticleType
                });
            })();
        """.trimIndent()
    }
}

/**
 * Represents an extracted article for reading mode
 *
 * @property title Article title
 * @property author Article author (if available)
 * @property publishDate Publish date (if available)
 * @property featuredImage Main article image URL (if available)
 * @property siteName Site/publication name
 * @property content Cleaned HTML content
 * @property textContent Plain text content (for word count, etc.)
 * @property wordCount Number of words in article
 * @property images List of images within article
 * @property url Original article URL
 */
@Serializable
data class ReadingModeArticle(
    val title: String,
    val author: String = "",
    val publishDate: String = "",
    val featuredImage: String = "",
    val siteName: String = "",
    val content: String,
    val textContent: String,
    val wordCount: Int = 0,
    val images: List<ArticleImage> = emptyList(),
    val url: String
)

/**
 * Represents an image within an article
 *
 * @property src Image source URL
 * @property alt Image alt text
 * @property width Image width (if available)
 * @property height Image height (if available)
 */
@Serializable
data class ArticleImage(
    val src: String,
    val alt: String = "",
    val width: Int = 0,
    val height: Int = 0
)

/**
 * Result of article detection check
 *
 * @property isArticle Whether the page is likely an article
 * @property score Confidence score (0-5)
 * @property wordCount Total word count on page
 * @property hasArticleTag Whether page has article/main tag
 * @property isArticleType Whether OpenGraph type is article
 */
@Serializable
data class ArticleDetectionResult(
    val isArticle: Boolean,
    val score: Int,
    val wordCount: Int,
    val hasArticleTag: Boolean,
    val isArticleType: Boolean
)
