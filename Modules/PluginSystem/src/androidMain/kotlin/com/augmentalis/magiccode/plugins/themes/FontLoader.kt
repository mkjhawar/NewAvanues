package com.augmentalis.magiccode.plugins.themes

import android.graphics.Typeface
import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of FontLoader using Typeface.
 *
 * Uses android.graphics.Typeface.createFromFile() to load custom fonts
 * for use in Android applications.
 */
actual class FontLoader {
    private val mutex = Mutex()
    private val loadedFonts = mutableMapOf<String, LoadedFont>()
    private val typefaces = mutableMapOf<String, Typeface>()

    actual companion object {
        private const val TAG = "FontLoader[Android]"

        /**
         * Supported font file extensions on Android.
         * Android supports TTF, OTF, and some support for WOFF/WOFF2 on newer versions.
         */
        actual val SUPPORTED_EXTENSIONS: Set<String> = setOf("ttf", "otf", "woff", "woff2")

        /**
         * Default fallback fonts for Android.
         */
        private val ANDROID_DEFAULT_FALLBACKS = listOf(
            "sans-serif",
            "sans-serif-light",
            "sans-serif-condensed",
            "serif",
            "monospace"
        )
    }

    /**
     * Load and register a custom font from file path.
     *
     * @param fontFamily Font family name (e.g., "Roboto", "SF Pro")
     * @param fontPath Absolute path to font file
     * @return FontLoadResult indicating success or failure
     */
    actual suspend fun loadFont(fontFamily: String, fontPath: String): FontLoadResult {
        PluginLog.d(TAG, "Loading font: $fontFamily from $fontPath")

        return withContext(Dispatchers.IO) {
            mutex.withLock {
                // Check if already loaded
                if (loadedFonts.containsKey(fontFamily)) {
                    PluginLog.d(TAG, "Font already loaded: $fontFamily")
                    return@withContext FontLoadResult.Success(fontFamily, fontPath)
                }

                // Validate font file
                val (isValid, errorMessage) = validateFontFile(fontPath)
                if (!isValid) {
                    return@withContext FontLoadResult.Failure(
                        errorMessage ?: "Font validation failed",
                        fontPath
                    )
                }

                try {
                    // Load font using Typeface.createFromFile()
                    val fontFile = File(fontPath)
                    val typeface = Typeface.createFromFile(fontFile)

                    if (typeface == null) {
                        return@withContext FontLoadResult.Failure(
                            "Typeface.createFromFile returned null",
                            fontPath
                        )
                    }

                    // Get font format
                    val fontFormat = FontLoaderUtils.getFontFormat(fontPath)

                    // Cache loaded font and typeface
                    val loadedFont = LoadedFont(
                        fontFamily = fontFamily,
                        fontPath = fontPath,
                        format = fontFormat,
                        loadedAt = System.currentTimeMillis()
                    )
                    loadedFonts[fontFamily] = loadedFont
                    typefaces[fontFamily] = typeface

                    PluginLog.i(TAG, "Font loaded successfully: $fontFamily")
                    FontLoadResult.Success(fontFamily, fontPath)

                } catch (e: Exception) {
                    val error = "Failed to load font $fontFamily: ${e.message}"
                    PluginLog.e(TAG, error, e)
                    FontLoadResult.Failure(error, fontPath)
                }
            }
        }
    }

    /**
     * Load and register multiple fonts in batch.
     *
     * @param fonts Map of font family name to font file path
     * @return Map of font family to FontLoadResult
     */
    actual suspend fun loadFonts(fonts: Map<String, String>): Map<String, FontLoadResult> {
        PluginLog.d(TAG, "Batch loading ${fonts.size} fonts")

        return fonts.mapValues { (fontFamily, fontPath) ->
            loadFont(fontFamily, fontPath)
        }
    }

    /**
     * Check if a font is already loaded.
     *
     * @param fontFamily Font family name
     * @return true if font is loaded, false otherwise
     */
    actual fun isFontLoaded(fontFamily: String): Boolean {
        return loadedFonts.containsKey(fontFamily)
    }

    /**
     * Get information about a loaded font.
     *
     * @param fontFamily Font family name
     * @return LoadedFont or null if not loaded
     */
    actual fun getLoadedFont(fontFamily: String): LoadedFont? {
        return loadedFonts[fontFamily]
    }

    /**
     * Get all loaded fonts.
     *
     * @return List of loaded fonts
     */
    actual fun getAllLoadedFonts(): List<LoadedFont> {
        return loadedFonts.values.toList()
    }

    /**
     * Unload a font.
     *
     * Note: Android does not support unloading Typeface objects.
     * This method only removes the font from our cache.
     *
     * @param fontFamily Font family name
     * @return true if removed from cache, false if not found
     */
    actual fun unloadFont(fontFamily: String): Boolean {
        val removed = loadedFonts.remove(fontFamily)
        typefaces.remove(fontFamily)
        if (removed != null) {
            PluginLog.i(TAG, "Font removed from cache: $fontFamily (Note: Android does not support Typeface unloading)")
            return true
        }
        return false
    }

    /**
     * Clear all loaded fonts.
     *
     * Note: Android does not support unloading Typeface objects.
     * This method only clears our cache.
     *
     * @return Number of fonts removed from cache
     */
    actual fun clearAllFonts(): Int {
        val count = loadedFonts.size
        loadedFonts.clear()
        typefaces.clear()
        if (count > 0) {
            PluginLog.i(TAG, "Cleared $count fonts from cache (Note: Android does not support Typeface unloading)")
        }
        return count
    }

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
    actual fun validateFontFile(fontPath: String): Pair<Boolean, String?> {
        try {
            val file = File(fontPath)

            // Check file exists
            if (!file.exists()) {
                return Pair(false, "Font file does not exist: $fontPath")
            }

            // Check file is a regular file (not directory)
            if (!file.isFile) {
                return Pair(false, "Font path is not a file: $fontPath")
            }

            // Check file is readable
            if (!file.canRead()) {
                return Pair(false, "Font file is not readable: $fontPath")
            }

            // Check file extension
            if (!FontLoaderUtils.isSupportedFontExtension(fontPath)) {
                val extension = fontPath.substringAfterLast(".", "")
                return Pair(false, "Unsupported font format: .$extension (supported: ${SUPPORTED_EXTENSIONS.joinToString()})")
            }

            // Check file size
            val fileSize = file.length()
            if (fileSize == 0L) {
                return Pair(false, "Font file is empty: $fontPath")
            }
            if (fileSize > FontLoaderUtils.MAX_FONT_FILE_SIZE_BYTES) {
                return Pair(false, "Font file is too large: $fileSize bytes (max: ${FontLoaderUtils.MAX_FONT_FILE_SIZE_BYTES})")
            }

            return Pair(true, null)

        } catch (e: Exception) {
            return Pair(false, "Font validation error: ${e.message}")
        }
    }

    /**
     * Get the Android Typeface for a loaded font.
     *
     * @param fontFamily Font family name
     * @return Typeface or null if not loaded
     */
    fun getTypeface(fontFamily: String): Typeface? {
        return typefaces[fontFamily]
    }

    /**
     * Get Android-specific default fallback fonts.
     *
     * @return List of default fallback font families
     */
    fun getDefaultFallbacks(): List<String> {
        return ANDROID_DEFAULT_FALLBACKS
    }

    /**
     * Get all available system font families.
     *
     * Note: Android doesn't provide a standard API to enumerate all system fonts.
     * This returns a list of common Android system fonts.
     *
     * @return List of available font family names
     */
    fun getAvailableFonts(): List<String> {
        return listOf(
            "sans-serif",
            "sans-serif-light",
            "sans-serif-thin",
            "sans-serif-condensed",
            "sans-serif-medium",
            "sans-serif-black",
            "serif",
            "monospace",
            "cursive",
            "casual"
        )
    }
}
