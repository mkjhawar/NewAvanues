package com.augmentalis.avacode.plugins.themes

import com.augmentalis.avacode.plugins.core.PluginLog

/**
 * Font format enumeration.
 */
enum class FontFormat {
    TTF,
    OTF,
    WOFF,
    WOFF2,
    UNKNOWN;

    companion object {
        fun fromExtension(extension: String): FontFormat {
            return when (extension.lowercase()) {
                "ttf" -> TTF
                "otf" -> OTF
                "woff" -> WOFF
                "woff2" -> WOFF2
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Load result for font loading operations.
 */
sealed class FontLoadResult {
    data class Success(val fontFamily: String, val fontPath: String) : FontLoadResult()
    data class Failure(val reason: String, val fontPath: String? = null) : FontLoadResult()
}

/**
 * Loaded font information.
 */
data class LoadedFont(
    val fontFamily: String,
    val fontPath: String,
    val format: FontFormat,
    val loadedAt: Long
)

/**
 * Platform-specific font loader for custom theme fonts.
 *
 * Provides an expect/actual abstraction for loading custom fonts from plugin assets
 * and registering them with the platform's font system.
 *
 * Supported font formats:
 * - TTF (TrueType Font)
 * - OTF (OpenType Font)
 * - WOFF (Web Open Font Format)
 * - WOFF2 (Web Open Font Format 2)
 *
 * Platform implementations:
 * - JVM: Uses GraphicsEnvironment.registerFont()
 * - Android: Uses Typeface.createFromFile()
 * - iOS: Uses CTFontManagerRegisterFontsForURL()
 */
expect class FontLoader() {
    companion object {
        /**
         * Supported font file extensions.
         */
        val SUPPORTED_EXTENSIONS: Set<String>
    }

    /**
     * Load and register a custom font from file path.
     *
     * @param fontFamily Font family name (e.g., "Roboto", "SF Pro")
     * @param fontPath Absolute path to font file
     * @return FontLoadResult indicating success or failure
     */
    suspend fun loadFont(fontFamily: String, fontPath: String): FontLoadResult

    /**
     * Load and register multiple fonts in batch.
     *
     * @param fonts Map of font family name to font file path
     * @return Map of font family to FontLoadResult
     */
    suspend fun loadFonts(fonts: Map<String, String>): Map<String, FontLoadResult>

    /**
     * Check if a font is already loaded.
     *
     * @param fontFamily Font family name
     * @return true if font is loaded, false otherwise
     */
    fun isFontLoaded(fontFamily: String): Boolean

    /**
     * Get information about a loaded font.
     *
     * @param fontFamily Font family name
     * @return LoadedFont or null if not loaded
     */
    fun getLoadedFont(fontFamily: String): LoadedFont?

    /**
     * Get all loaded fonts.
     *
     * @return List of loaded fonts
     */
    fun getAllLoadedFonts(): List<LoadedFont>

    /**
     * Unload a font (if platform supports it).
     *
     * @param fontFamily Font family name
     * @return true if unloaded, false if not supported or not found
     */
    fun unloadFont(fontFamily: String): Boolean

    /**
     * Clear all loaded fonts (if platform supports it).
     *
     * @return Number of fonts unloaded
     */
    fun clearAllFonts(): Int

    /**
     * Validate font file before loading.
     *
     * Checks:
     * - File exists
     * - File is readable
     * - File extension is supported
     * - File size is reasonable (not empty, not too large)
     *
     * @param fontPath Absolute path to font file
     * @return Pair of (isValid, errorMessage or null)
     */
    fun validateFontFile(fontPath: String): Pair<Boolean, String?>
}

/**
 * Common font loader utilities (available on all platforms).
 */
object FontLoaderUtils {
    private const val TAG = "FontLoaderUtils"

    /**
     * Maximum font file size (10 MB).
     * Fonts larger than this are likely corrupt or not font files.
     */
    const val MAX_FONT_FILE_SIZE_BYTES = 10 * 1024 * 1024

    /**
     * Extract font format from file extension.
     *
     * @param fontPath Font file path
     * @return FontFormat
     */
    fun getFontFormat(fontPath: String): FontFormat {
        val extension = fontPath.substringAfterLast(".", "")
        return FontFormat.fromExtension(extension)
    }

    /**
     * Check if file extension is a supported font format.
     *
     * @param filename Filename or path
     * @return true if supported, false otherwise
     */
    fun isSupportedFontExtension(filename: String): Boolean {
        val extension = filename.substringAfterLast(".", "").lowercase()
        return FontLoader.SUPPORTED_EXTENSIONS.contains(extension)
    }

    /**
     * Sanitize font family name.
     * Removes invalid characters and normalizes spacing.
     *
     * @param fontFamily Font family name
     * @return Sanitized font family name
     */
    fun sanitizeFontFamily(fontFamily: String): String {
        return fontFamily.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "")
    }

    /**
     * Build font fallback chain.
     *
     * @param primaryFont Primary font family
     * @param fallbackList Custom fallback list
     * @param platformDefaults Platform-specific default fallbacks
     * @return Complete fallback chain
     */
    fun buildFallbackChain(
        primaryFont: String,
        fallbackList: List<String>,
        platformDefaults: List<String>
    ): List<String> {
        val chain = mutableListOf(primaryFont)
        chain.addAll(fallbackList)
        chain.addAll(platformDefaults)
        return chain.distinct()
    }

    /**
     * Log font loading status.
     *
     * @param result FontLoadResult from font loading
     */
    fun logLoadResult(result: FontLoadResult) {
        when (result) {
            is FontLoadResult.Success -> {
                PluginLog.i(TAG, "Font loaded successfully: ${result.fontFamily} from ${result.fontPath}")
            }
            is FontLoadResult.Failure -> {
                PluginLog.e(TAG, "Font loading failed: ${result.reason} (path: ${result.fontPath})")
            }
        }
    }
}
