package com.augmentalis.avacode.plugins.themes

import com.augmentalis.avacode.plugins.core.PluginLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.FileInputStream

/**
 * JVM implementation of FontLoader using GraphicsEnvironment.
 *
 * Uses java.awt.Font and GraphicsEnvironment.registerFont() to load and register
 * custom fonts for use in Swing/AWT applications.
 */
actual class FontLoader {
    private val mutex = Mutex()
    private val loadedFonts = mutableMapOf<String, LoadedFont>()

    companion object {
        private const val TAG = "FontLoader[JVM]"

        /**
         * Supported font file extensions on JVM.
         * JVM supports TTF and OTF natively. WOFF/WOFF2 require conversion.
         */
        actual val SUPPORTED_EXTENSIONS: Set<String> = setOf("ttf", "otf")

        /**
         * Default fallback fonts for JVM.
         */
        private val JVM_DEFAULT_FALLBACKS = listOf(
            "Dialog",
            "SansSerif",
            "Serif",
            "Monospaced"
        )
    }

    /**
     * Load result for font loading operations.
     */
    actual sealed class LoadResult {
        actual data class Success(
            actual val fontFamily: String,
            actual val fontPath: String
        ) : LoadResult()

        actual data class Failure(
            actual val reason: String,
            actual val fontPath: String? = null
        ) : LoadResult()
    }

    /**
     * Loaded font information.
     */
    actual data class LoadedFont(
        actual val fontFamily: String,
        actual val fontPath: String,
        actual val format: FontFormat,
        actual val loadedAt: Long
    )

    /**
     * Font format enumeration.
     */
    actual enum class FontFormat {
        TTF,
        OTF,
        WOFF,
        WOFF2,
        UNKNOWN;

        actual companion object {
            actual fun fromExtension(extension: String): FontFormat {
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
     * Load and register a custom font from file path.
     *
     * @param fontFamily Font family name (e.g., "Roboto", "SF Pro")
     * @param fontPath Absolute path to font file
     * @return LoadResult indicating success or failure
     */
    actual suspend fun loadFont(fontFamily: String, fontPath: String): LoadResult {
        PluginLog.d(TAG, "Loading font: $fontFamily from $fontPath")

        return withContext(Dispatchers.IO) {
            mutex.withLock {
                // Check if already loaded
                if (loadedFonts.containsKey(fontFamily)) {
                    PluginLog.d(TAG, "Font already loaded: $fontFamily")
                    return@withContext LoadResult.Success(fontFamily, fontPath)
                }

                // Validate font file
                val (isValid, errorMessage) = validateFontFile(fontPath)
                if (!isValid) {
                    return@withContext LoadResult.Failure(
                        errorMessage ?: "Font validation failed",
                        fontPath
                    )
                }

                try {
                    // Load font using AWT Font.createFont()
                    val fontFile = File(fontPath)
                    val fontFormat = FontLoaderUtils.getFontFormat(fontPath)

                    val awtFont = FileInputStream(fontFile).use { stream ->
                        Font.createFont(Font.TRUETYPE_FONT, stream)
                    }

                    // Register font with GraphicsEnvironment
                    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    val registered = ge.registerFont(awtFont)

                    if (!registered) {
                        PluginLog.w(TAG, "GraphicsEnvironment.registerFont returned false for: $fontFamily")
                        return@withContext LoadResult.Failure(
                            "Failed to register font with GraphicsEnvironment",
                            fontPath
                        )
                    }

                    // Cache loaded font
                    val loadedFont = LoadedFont(
                        fontFamily = fontFamily,
                        fontPath = fontPath,
                        format = fontFormat,
                        loadedAt = System.currentTimeMillis()
                    )
                    loadedFonts[fontFamily] = loadedFont

                    PluginLog.i(TAG, "Font registered successfully: $fontFamily (${awtFont.family})")
                    LoadResult.Success(fontFamily, fontPath)

                } catch (e: Exception) {
                    val error = "Failed to load font $fontFamily: ${e.message}"
                    PluginLog.e(TAG, error, e)
                    LoadResult.Failure(error, fontPath)
                }
            }
        }
    }

    /**
     * Load and register multiple fonts in batch.
     *
     * @param fonts Map of font family name to font file path
     * @return Map of font family to LoadResult
     */
    actual suspend fun loadFonts(fonts: Map<String, String>): Map<String, LoadResult> {
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
     * Note: JVM GraphicsEnvironment does not support unregistering fonts.
     * This method only removes the font from our cache.
     *
     * @param fontFamily Font family name
     * @return true if removed from cache, false if not found
     */
    actual fun unloadFont(fontFamily: String): Boolean {
        val removed = loadedFonts.remove(fontFamily)
        if (removed != null) {
            PluginLog.i(TAG, "Font removed from cache: $fontFamily (Note: JVM does not support font unregistration)")
            return true
        }
        return false
    }

    /**
     * Clear all loaded fonts.
     *
     * Note: JVM GraphicsEnvironment does not support unregistering fonts.
     * This method only clears our cache.
     *
     * @return Number of fonts removed from cache
     */
    actual fun clearAllFonts(): Int {
        val count = loadedFonts.size
        loadedFonts.clear()
        if (count > 0) {
            PluginLog.i(TAG, "Cleared $count fonts from cache (Note: JVM does not support font unregistration)")
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
     * Get JVM-specific default fallback fonts.
     *
     * @return List of default fallback font families
     */
    fun getDefaultFallbacks(): List<String> {
        return JVM_DEFAULT_FALLBACKS
    }

    /**
     * Get all available font families in the system.
     *
     * @return List of available font family names
     */
    fun getAvailableFonts(): List<String> {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        return ge.availableFontFamilyNames.toList()
    }
}
