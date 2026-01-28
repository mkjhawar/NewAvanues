package com.augmentalis.commandmanager

/**
 * Platform-agnostic interface for app category detection.
 *
 * This interface is part of the 4-layer Hybrid Persistence system (Layer 1: App Category).
 * It provides an abstraction for detecting application categories across different platforms.
 *
 * ## Platform Implementations
 *
 * ### Android
 * The Android implementation uses `PackageManager.getApplicationInfo().category` to query
 * the system for accurate app categorization. This provides precise classification based
 * on Google Play Store categories and app manifests. The Android implementation should:
 * - Cache results to avoid repeated PackageManager queries
 * - Handle cases where apps are installed/uninstalled
 * - Fall back to pattern-based classification for unknown apps
 *
 * Example Android implementation:
 * ```kotlin
 * class AndroidAppCategoryProvider(private val context: Context) : IAppCategoryProvider {
 *     private val cache = ConcurrentHashMap<String, AppCategory>()
 *
 *     override fun getCategory(packageName: String): AppCategory {
 *         return cache.getOrPut(packageName) {
 *             try {
 *                 val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
 *                 mapAndroidCategory(appInfo.category)
 *             } catch (e: PackageManager.NameNotFoundException) {
 *                 AppCategoryClassifier.classifyPackage(packageName)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ### iOS / Desktop / Other Platforms
 * Other platforms can provide their own implementations using platform-specific APIs
 * or fall back to the [DefaultAppCategoryProvider] which uses pattern-based classification.
 *
 * ## Usage in Hybrid Persistence
 *
 * The app category determines the dynamic behavior of the application, which affects:
 * - Whether UI elements should be persisted (static apps)
 * - How often the screen cache should be refreshed (dynamic apps)
 * - Whether location/context-based caching strategies apply
 *
 * @see AppCategory
 * @see AppCategoryClassifier
 * @see DynamicBehavior
 */
interface IAppCategoryProvider {
    /**
     * Get the app category for a given package name.
     *
     * On Android, this queries PackageManager for the app category, mapping the
     * Android category constants to [AppCategory] values. Falls back to pattern-based
     * classification via [AppCategoryClassifier] if the platform API is unavailable
     * or the app is not found.
     *
     * @param packageName The application package name (e.g., "com.example.app")
     * @return [AppCategory] classification for the application
     */
    fun getCategory(packageName: String): AppCategory

    /**
     * Get dynamic behavior for a package.
     *
     * This is a convenience method that returns the [DynamicBehavior] associated
     * with the app's category. The behavior determines persistence strategies:
     * - [DynamicBehavior.STATIC]: UI rarely changes, safe to persist commands
     * - [DynamicBehavior.SEMI_DYNAMIC]: UI changes occasionally, use short TTL
     * - [DynamicBehavior.DYNAMIC]: UI changes frequently, prefer real-time matching
     *
     * @param packageName The application package name
     * @return [DynamicBehavior] of the application
     */
    fun getDynamicBehavior(packageName: String): DynamicBehavior {
        return getCategory(packageName).dynamicBehavior
    }

    /**
     * Check if app category is cached.
     *
     * Used to avoid repeated platform API queries (e.g., PackageManager on Android).
     * Caching is important for performance since category detection may involve
     * expensive system calls.
     *
     * @param packageName The application package name
     * @return true if category is already cached, false otherwise
     */
    fun isCached(packageName: String): Boolean

    /**
     * Clear the category cache.
     *
     * Should be called when the app landscape changes:
     * - When apps are installed or uninstalled
     * - When app updates may change categories
     * - During maintenance or memory pressure situations
     *
     * The [DefaultAppCategoryProvider] implementation is a no-op since it uses
     * stateless pattern-based classification.
     */
    fun clearCache()
}

/**
 * Default implementation using pattern-based classification only.
 *
 * This implementation is used when platform-specific APIs are unavailable.
 * It relies on [AppCategoryClassifier.classifyPackage] to determine app categories
 * based on package name patterns and known app signatures.
 *
 * ## Characteristics
 * - No caching: Each call re-evaluates the package name
 * - Stateless: No side effects or persistent state
 * - Universal: Works on all platforms without modification
 * - Limited accuracy: Cannot detect runtime app metadata
 *
 * ## When to Use
 * - As a fallback when platform API fails
 * - On platforms without native app category APIs
 * - During testing and development
 *
 * @see IAppCategoryProvider
 * @see AppCategoryClassifier
 */
object DefaultAppCategoryProvider : IAppCategoryProvider {

    /**
     * Classify the package using pattern-based heuristics.
     *
     * Delegates to [AppCategoryClassifier.classifyPackage] which analyzes the
     * package name against known patterns for social media, games, productivity,
     * and other app categories.
     *
     * @param packageName The application package name
     * @return [AppCategory] based on pattern matching
     */
    override fun getCategory(packageName: String): AppCategory {
        return AppCategoryClassifier.classifyPackage(packageName)
    }

    /**
     * Always returns false since pattern-based classification is stateless.
     *
     * @param packageName The application package name (unused)
     * @return false - no caching is performed
     */
    override fun isCached(packageName: String): Boolean = false

    /**
     * No-op implementation since pattern-based classification has no cache.
     */
    override fun clearCache() {
        // No-op: Pattern-based classification is stateless and has no cache
    }
}
