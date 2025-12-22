package com.augmentalis.magiccode.plugins.themes

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.CoreFoundation.*
import platform.CoreText.*
import platform.Foundation.*

/**
 * iOS implementation of FontLoader using CoreText.
 *
 * Uses CTFontManagerRegisterFontsForURL() to load and register custom fonts
 * for use in iOS applications.
 */
actual class FontLoader {
    private val mutex = Mutex()
    private val loadedFonts = mutableMapOf<String, LoadedFont>()
    private val registeredUrls = mutableMapOf<String, NSURL>()

    companion object {
        private const val TAG = "FontLoader[iOS]"

        /**
         * Supported font file extensions on iOS.
         * iOS supports TTF, OTF natively through CoreText.
         */
        actual val SUPPORTED_EXTENSIONS: Set<String> = setOf("ttf", "otf")

        /**
         * Default fallback fonts for iOS.
         */
        private val IOS_DEFAULT_FALLBACKS = listOf(
            "SF Pro",
            "San Francisco",
            "Helvetica Neue",
            "Helvetica",
            "Arial",
            "system"
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

        return withContext(Dispatchers.Default) {
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
                    // Create NSURL from file path
                    val nsUrl = NSURL.fileURLWithPath(fontPath)
                    if (nsUrl == null) {
                        return@withContext LoadResult.Failure(
                            "Failed to create NSURL from path: $fontPath",
                            fontPath
                        )
                    }

                    // Register font with CTFontManager
                    memScoped {
                        val errorRef = alloc<ObjCObjectVar<CFErrorRef?>>()
                        errorRef.value = null

                        val registered = CTFontManagerRegisterFontsForURL(
                            nsUrl,
                            kCTFontManagerScopeProcess,
                            errorRef.ptr
                        )

                        if (!registered) {
                            val error = errorRef.value
                            val errorDescription = if (error != null) {
                                CFErrorCopyDescription(error)?.let {
                                    CFBridgingRelease(it) as? String
                                } ?: "Unknown error"
                            } else {
                                "CTFontManagerRegisterFontsForURL returned false"
                            }

                            PluginLog.e(TAG, "Failed to register font: $errorDescription")
                            return@withContext LoadResult.Failure(
                                "Failed to register font: $errorDescription",
                                fontPath
                            )
                        }
                    }

                    // Get font format
                    val fontFormat = FontLoaderUtils.getFontFormat(fontPath)

                    // Cache loaded font
                    val loadedFont = LoadedFont(
                        fontFamily = fontFamily,
                        fontPath = fontPath,
                        format = fontFormat,
                        loadedAt = NSDate().timeIntervalSince1970.toLong() * 1000
                    )
                    loadedFonts[fontFamily] = loadedFont
                    registeredUrls[fontFamily] = nsUrl

                    PluginLog.i(TAG, "Font registered successfully: $fontFamily")
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
     * Uses CTFontManagerUnregisterFontsForURL to unregister the font.
     *
     * @param fontFamily Font family name
     * @return true if unloaded, false if not found or unload failed
     */
    actual fun unloadFont(fontFamily: String): Boolean {
        val url = registeredUrls[fontFamily] ?: return false

        return try {
            memScoped {
                val errorRef = alloc<ObjCObjectVar<CFErrorRef?>>()
                errorRef.value = null

                val unregistered = CTFontManagerUnregisterFontsForURL(
                    url,
                    kCTFontManagerScopeProcess,
                    errorRef.ptr
                )

                if (unregistered) {
                    loadedFonts.remove(fontFamily)
                    registeredUrls.remove(fontFamily)
                    PluginLog.i(TAG, "Font unregistered successfully: $fontFamily")
                    true
                } else {
                    val error = errorRef.value
                    val errorDescription = if (error != null) {
                        CFErrorCopyDescription(error)?.let {
                            CFBridgingRelease(it) as? String
                        } ?: "Unknown error"
                    } else {
                        "CTFontManagerUnregisterFontsForURL returned false"
                    }
                    PluginLog.w(TAG, "Failed to unregister font $fontFamily: $errorDescription")
                    false
                }
            }
        } catch (e: Exception) {
            PluginLog.e(TAG, "Error unregistering font $fontFamily: ${e.message}", e)
            false
        }
    }

    /**
     * Clear all loaded fonts.
     *
     * Unregisters all fonts using CTFontManagerUnregisterFontsForURL.
     *
     * @return Number of fonts unloaded
     */
    actual fun clearAllFonts(): Int {
        var count = 0
        val families = loadedFonts.keys.toList()

        for (fontFamily in families) {
            if (unloadFont(fontFamily)) {
                count++
            }
        }

        if (count > 0) {
            PluginLog.i(TAG, "Cleared $count fonts")
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
            val fileManager = NSFileManager.defaultManager

            // Check file exists
            if (!fileManager.fileExistsAtPath(fontPath)) {
                return Pair(false, "Font file does not exist: $fontPath")
            }

            // Check file is readable
            if (!fileManager.isReadableFileAtPath(fontPath)) {
                return Pair(false, "Font file is not readable: $fontPath")
            }

            // Check file extension
            if (!FontLoaderUtils.isSupportedFontExtension(fontPath)) {
                val extension = fontPath.substringAfterLast(".", "")
                return Pair(false, "Unsupported font format: .$extension (supported: ${SUPPORTED_EXTENSIONS.joinToString()})")
            }

            // Check file size
            val attributes = fileManager.attributesOfItemAtPath(fontPath, null)
            if (attributes != null) {
                val fileSize = (attributes[NSFileSize] as? NSNumber)?.longValue ?: 0L

                if (fileSize == 0L) {
                    return Pair(false, "Font file is empty: $fontPath")
                }
                if (fileSize > FontLoaderUtils.MAX_FONT_FILE_SIZE_BYTES) {
                    return Pair(false, "Font file is too large: $fileSize bytes (max: ${FontLoaderUtils.MAX_FONT_FILE_SIZE_BYTES})")
                }
            }

            return Pair(true, null)

        } catch (e: Exception) {
            return Pair(false, "Font validation error: ${e.message}")
        }
    }

    /**
     * Get iOS-specific default fallback fonts.
     *
     * @return List of default fallback font families
     */
    fun getDefaultFallbacks(): List<String> {
        return IOS_DEFAULT_FALLBACKS
    }

    /**
     * Get all available system font families.
     *
     * @return List of available font family names
     */
    fun getAvailableFonts(): List<String> {
        val familyNames = mutableListOf<String>()

        val fontFamilyNames = CTFontManagerCopyAvailableFontFamilyNames()
        if (fontFamilyNames != null) {
            val count = CFArrayGetCount(fontFamilyNames)
            for (i in 0 until count) {
                val familyName = CFArrayGetValueAtIndex(fontFamilyNames, i)
                if (familyName != null) {
                    val name = CFBridgingRelease(familyName) as? String
                    if (name != null) {
                        familyNames.add(name)
                    }
                }
            }
            CFRelease(fontFamilyNames)
        }

        return familyNames
    }

    /**
     * Get CTFont instance for a loaded font.
     *
     * @param fontFamily Font family name
     * @param size Font size in points
     * @return CTFont or null if not loaded
     */
    fun getCTFont(fontFamily: String, size: Double = 12.0): CPointer<*>? {
        if (!isFontLoaded(fontFamily)) {
            return null
        }

        val fontName = fontFamily.toCFString()
        val font = CTFontCreateWithName(fontName, size, null)
        CFRelease(fontName)

        return font
    }

    /**
     * Helper to convert String to CFStringRef.
     */
    private fun String.toCFString(): CFStringRef {
        return CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)
            ?: throw IllegalStateException("Failed to create CFString")
    }
}
