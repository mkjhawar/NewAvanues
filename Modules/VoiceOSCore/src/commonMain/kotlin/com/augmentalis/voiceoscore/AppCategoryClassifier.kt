package com.augmentalis.voiceoscore

/**
 * Represents how dynamic the UI content typically is for a category of apps.
 * This affects how the hybrid persistence system handles command storage.
 */
enum class DynamicBehavior {
    /**
     * Almost everything should persist.
     * Apps in this category have stable UI structures that rarely change.
     * Examples: Settings apps, system apps, configuration screens.
     */
    STATIC,

    /**
     * Most content is dynamic, only structural elements (menus, navigation) persist.
     * Apps in this category have content that changes frequently based on user data.
     * Examples: Email clients, messaging apps, social media feeds.
     */
    MOSTLY_DYNAMIC,

    /**
     * Context-dependent persistence.
     * Apps in this category have a mix of static UI (menus, controls) and dynamic content.
     * Examples: Media players (static controls, dynamic playlists), productivity apps.
     */
    MIXED
}

/**
 * Categories of applications based on their typical UI behavior patterns.
 * Each category has an associated [DynamicBehavior] that determines how
 * the hybrid persistence system should handle learned commands.
 *
 * @property dynamicBehavior The typical dynamic behavior pattern for apps in this category.
 */
enum class AppCategory(val dynamicBehavior: DynamicBehavior) {
    /**
     * Email applications (Gmail, Outlook, Yahoo Mail).
     * Content is highly dynamic (emails change constantly), but navigation is stable.
     */
    EMAIL(DynamicBehavior.MOSTLY_DYNAMIC),

    /**
     * Messaging and communication apps (WhatsApp, Telegram, Slack).
     * Messages are dynamic, but UI structure and contacts are more stable.
     */
    MESSAGING(DynamicBehavior.MOSTLY_DYNAMIC),

    /**
     * Social media applications (Instagram, Twitter, Facebook).
     * Feeds are constantly changing, only navigation elements persist well.
     */
    SOCIAL(DynamicBehavior.MOSTLY_DYNAMIC),

    /**
     * Settings and configuration apps (System Settings, app preferences).
     * Highly stable UI that rarely changes between sessions.
     */
    SETTINGS(DynamicBehavior.STATIC),

    /**
     * System-level applications (Launcher, SystemUI).
     * Core system UI that is very stable and predictable.
     */
    SYSTEM(DynamicBehavior.STATIC),

    /**
     * Productivity applications (Notes, Calendar, Documents).
     * Mix of stable UI (toolbars, menus) and dynamic content (documents, events).
     */
    PRODUCTIVITY(DynamicBehavior.MIXED),

    /**
     * Web browsers (Chrome, Firefox, Edge).
     * Browser UI is stable, but web content is completely dynamic.
     */
    BROWSER(DynamicBehavior.MOSTLY_DYNAMIC),

    /**
     * Media playback applications (Spotify, YouTube, Netflix).
     * Controls are stable, but content (playlists, videos) changes.
     */
    MEDIA(DynamicBehavior.MIXED),

    /**
     * Enterprise and industrial applications (RealWear, Augmentalis).
     * Often task-focused with mix of stable workflows and dynamic data.
     */
    ENTERPRISE(DynamicBehavior.MIXED),

    /**
     * Default category for unrecognized applications.
     * Uses MIXED behavior as a safe middle ground.
     */
    UNKNOWN(DynamicBehavior.MIXED)
}

/**
 * Classifies Android application packages into categories using pattern matching.
 *
 * This classifier provides pattern-based classification as a **fallback** when:
 * - Database lookup (via [AppCategoryLoader]) returns no result
 * - PackageManager API is unavailable (AOSP without Play Store)
 *
 * ## Hybrid Persistence Classification Hierarchy
 *
 * 1. **Database Lookup** (highest priority) - ACD file loaded via [AppCategoryLoader]
 * 2. **PackageManager API** - Android's ApplicationInfo.category (Play Store apps)
 * 3. **Permission Heuristics** - Infer from requested permissions
 * 4. **Pattern Matching** (this class) - Substring matching against known patterns
 *
 * ## Usage
 *
 * ```kotlin
 * // Pattern-only classification (fallback)
 * val category = AppCategoryClassifier.classifyByPattern("com.google.android.gmail")
 * // Returns AppCategory.EMAIL
 *
 * val behavior = category.dynamicBehavior
 * // Returns DynamicBehavior.MOSTLY_DYNAMIC
 * ```
 *
 * ## Pattern Matching
 *
 * The classifier checks if the package name (lowercase) contains any of the
 * registered patterns for each category. Categories are checked in order of
 * specificity (enterprise > settings > email > ... > system).
 *
 * Pattern matching provides ~70% confidence classification.
 *
 * ## Database-Backed Classification
 *
 * For high-confidence classification (~90%), use [AppCategoryLoader] to load
 * the `known-apps.acd` file into SQLite and query via [IAppCategoryRepository].
 * The ACD file contains curated entries for RealWear, Microsoft, Google, and
 * common enterprise apps.
 *
 * @see AppCategoryLoader for loading ACD files into database
 * @see IAppCategoryProvider for platform-specific classification
 */
object AppCategoryClassifier {

    // ============================================================
    // Pattern Matching (Fallback Classification)
    // ============================================================

    /**
     * Package name patterns for email applications.
     * Matches common email clients and generic email-related package names.
     */
    private val emailPatterns = listOf(
        "gmail",
        "android.gm", // Gmail's actual package: com.google.android.gm
        "outlook",
        "yahoo.mail",
        "mail",
        "email",
        "inbox",
        "protonmail",
        "fastmail",
        "spark",
        "edison.mail",
        "aquamail"
    )

    /**
     * Package name patterns for messaging applications.
     * Matches instant messaging, SMS, and team communication apps.
     */
    private val messagingPatterns = listOf(
        "whatsapp",
        "telegram",
        "messenger",
        "messaging", // Android Messages: com.google.android.apps.messaging
        "messages",
        "slack",
        "teams",
        "discord",
        "signal",
        "securesms", // Signal's package: org.thoughtcrime.securesms
        "viber",
        "wechat",
        "line",
        "skype",
        "hangouts",
        "chat",
        "sms",
        "mms"
    )

    /**
     * Package name patterns for social media applications.
     * Matches popular social networking platforms.
     */
    private val socialPatterns = listOf(
        "instagram",
        "twitter",
        "facebook",
        "tiktok",
        "musically", // TikTok's package: com.zhiliaoapp.musically
        "linkedin",
        "snapchat",
        "pinterest",
        "reddit",
        "tumblr",
        "threads",
        "barcelona", // Threads' package: com.instagram.barcelona
        "mastodon",
        "social"
    )

    /**
     * Package name patterns for settings applications.
     * Matches system settings, app settings, and configuration screens.
     */
    private val settingsPatterns = listOf(
        "settings",
        "preferences",
        "config",
        "configuration",
        "setup"
    )

    /**
     * Package name patterns for system applications.
     * Matches core Android system components and launchers.
     */
    private val systemPatterns = listOf(
        "launcher",
        "systemui",
        "android.system",
        "android.internal",
        "packageinstaller",
        "permissioncontroller",
        "documentsui",
        "vending" // Play Store
    )

    /**
     * Package name patterns for productivity applications.
     * Matches note-taking, calendar, document editing, and office apps.
     */
    private val productivityPatterns = listOf(
        "notes",
        "calendar",
        "docs",
        "sheets",
        "slides",
        "drive",
        "office",
        "word",
        "excel",
        "powerpoint",
        "onenote",
        "evernote",
        "notion",
        "trello",
        "asana",
        "todoist",
        "tasks",
        "keep",
        "dropbox"
    )

    /**
     * Package name patterns for web browsers.
     * Matches common mobile and desktop browsers.
     */
    private val browserPatterns = listOf(
        "chrome",
        "firefox",
        "browser",
        "edge",
        "emmx", // Edge's package: com.microsoft.emmx
        "opera",
        "safari",
        "brave",
        "vivaldi",
        "duckduckgo",
        "webview",
        "silk" // Amazon Silk
    )

    /**
     * Package name patterns for media applications.
     * Matches music players, video players, and streaming services.
     */
    private val mediaPatterns = listOf(
        "spotify",
        "youtube",
        "netflix",
        "music",
        "video",
        "player",
        "podcast",
        "audible",
        "prime.video",
        "hulu",
        "disney",
        "hbo",
        "twitch",
        "soundcloud",
        "pandora",
        "tidal",
        "deezer",
        "gallery",
        "photos",
        "camera"
    )

    /**
     * Package name patterns for enterprise applications.
     * Matches RealWear, Augmentalis, and other industrial/enterprise apps.
     */
    private val enterprisePatterns = listOf(
        "realwear",
        "augmentalis",
        "hmt",
        "navigator500",
        "teamviewer",
        "anydesk",
        "remote",
        "enterprise",
        "mdm",
        "intune",
        "workspace",
        "webex",
        "zoom",
        "meet"
    )

    /**
     * Mapping of categories to their pattern lists.
     * Ordered for optimal matching (more specific patterns checked first).
     */
    private val categoryPatterns: List<Pair<AppCategory, List<String>>> = listOf(
        // Enterprise first (most specific, branded apps)
        AppCategory.ENTERPRISE to enterprisePatterns,
        // Settings before system (settings is more specific)
        AppCategory.SETTINGS to settingsPatterns,
        // Communication apps
        AppCategory.EMAIL to emailPatterns,
        AppCategory.MESSAGING to messagingPatterns,
        AppCategory.SOCIAL to socialPatterns,
        // Utility apps
        AppCategory.BROWSER to browserPatterns,
        AppCategory.MEDIA to mediaPatterns,
        AppCategory.PRODUCTIVITY to productivityPatterns,
        // System last (broadest patterns)
        AppCategory.SYSTEM to systemPatterns
    )

    /**
     * Classifies an application package into a category using pattern matching only.
     *
     * This is a **fallback** classification method. For higher confidence, use
     * database lookup via [AppCategoryLoader] and [IAppCategoryRepository].
     *
     * The classification is performed by checking if the package name (converted to lowercase)
     * contains any of the registered patterns for each category. Categories are checked in
     * order of specificity to ensure accurate classification.
     *
     * @param packageName The full package name of the application (e.g., "com.google.android.gmail").
     * @return The [AppCategory] that best matches the package, or [AppCategory.UNKNOWN] if no match.
     *
     * ## Confidence
     * Pattern matching provides ~70% confidence. For ~90% confidence on known apps,
     * use database lookup first.
     *
     * ## Examples
     * ```kotlin
     * classifyByPattern("com.google.android.gmail") // EMAIL
     * classifyByPattern("com.whatsapp") // MESSAGING
     * classifyByPattern("com.android.settings") // SETTINGS
     * classifyByPattern("com.realwear.hmt1") // ENTERPRISE
     * classifyByPattern("com.random.app") // UNKNOWN
     * ```
     */
    fun classifyByPattern(packageName: String): AppCategory {
        val lowerPackageName = packageName.lowercase()

        for ((category, patterns) in categoryPatterns) {
            if (patterns.any { pattern -> lowerPackageName.contains(pattern) }) {
                return category
            }
        }

        return AppCategory.UNKNOWN
    }

    /**
     * Alias for [classifyByPattern] for backward compatibility.
     * @deprecated Use [classifyByPattern] for clarity that this is pattern-only classification.
     */
    @Deprecated(
        message = "Use classifyByPattern() for clarity",
        replaceWith = ReplaceWith("classifyByPattern(packageName)")
    )
    fun classifyPackage(packageName: String): AppCategory = classifyByPattern(packageName)

    /**
     * Checks if a package belongs to a specific category using pattern matching.
     *
     * @param packageName The full package name of the application.
     * @param category The category to check against.
     * @return True if the package belongs to the specified category.
     */
    fun isCategoryByPattern(packageName: String, category: AppCategory): Boolean {
        return classifyByPattern(packageName) == category
    }

    /**
     * Gets the dynamic behavior for a package using pattern matching.
     *
     * Convenience function that classifies the package and returns its dynamic behavior.
     *
     * @param packageName The full package name of the application.
     * @return The [DynamicBehavior] for the classified category.
     */
    fun getDynamicBehaviorByPattern(packageName: String): DynamicBehavior {
        return classifyByPattern(packageName).dynamicBehavior
    }

    /**
     * Checks if a package's content is mostly static (safe to persist most commands).
     * Uses pattern matching only.
     *
     * @param packageName The full package name of the application.
     * @return True if the package is classified as having STATIC dynamic behavior.
     */
    fun isStaticAppByPattern(packageName: String): Boolean {
        return getDynamicBehaviorByPattern(packageName) == DynamicBehavior.STATIC
    }

    /**
     * Checks if a package's content is mostly dynamic (should minimize persistence).
     * Uses pattern matching only.
     *
     * @param packageName The full package name of the application.
     * @return True if the package is classified as having MOSTLY_DYNAMIC dynamic behavior.
     */
    fun isDynamicAppByPattern(packageName: String): Boolean {
        return getDynamicBehaviorByPattern(packageName) == DynamicBehavior.MOSTLY_DYNAMIC
    }

    /**
     * Parse a category string to AppCategory enum.
     * Used when loading from database/ACD files.
     *
     * @param categoryName The category name (e.g., "EMAIL", "MESSAGING")
     * @return The corresponding [AppCategory], or [AppCategory.UNKNOWN] if not recognized.
     */
    fun parseCategory(categoryName: String): AppCategory {
        return try {
            AppCategory.valueOf(categoryName.uppercase())
        } catch (e: IllegalArgumentException) {
            AppCategory.UNKNOWN
        }
    }
}
