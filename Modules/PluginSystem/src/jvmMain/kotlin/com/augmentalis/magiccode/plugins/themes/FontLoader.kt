package com.augmentalis.magiccode.plugins.themes

import com.augmentalis.magiccode.plugins.core.PluginLog
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

        private val JVM_DEFAULT_FALLBACKS = listOf(
            "Dialog",
            "SansSerif",
            "Serif",
            "Monospaced"
        )
    }

    actual suspend fun loadFont(fontFamily: String, fontPath: String): FontLoadResult {
        PluginLog.d(TAG, "Loading font: $fontFamily from $fontPath")

        return withContext(Dispatchers.IO) {
            mutex.withLock {
                if (loadedFonts.containsKey(fontFamily)) {
                    PluginLog.d(TAG, "Font already loaded: $fontFamily")
                    return@withContext FontLoadResult.Success(fontFamily, fontPath)
                }

                val (isValid, errorMessage) = validateFontFile(fontPath)
                if (!isValid) {
                    return@withContext FontLoadResult.Failure(
                        errorMessage ?: "Font validation failed",
                        fontPath
                    )
                }

                try {
                    val fontFile = File(fontPath)
                    val fontFormat = FontLoaderUtils.getFontFormat(fontPath)

                    val awtFont = FileInputStream(fontFile).use { stream ->
                        Font.createFont(Font.TRUETYPE_FONT, stream)
                    }

                    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    val registered = ge.registerFont(awtFont)

                    if (!registered) {
                        PluginLog.w(TAG, "GraphicsEnvironment.registerFont returned false for: $fontFamily")
                        return@withContext FontLoadResult.Failure(
                            "Failed to register font with GraphicsEnvironment",
                            fontPath
                        )
                    }

                    val loadedFont = LoadedFont(
                        fontFamily = fontFamily,
                        fontPath = fontPath,
                        format = fontFormat,
                        loadedAt = System.currentTimeMillis()
                    )
                    loadedFonts[fontFamily] = loadedFont

                    PluginLog.i(TAG, "Font registered successfully: $fontFamily (${awtFont.family})")
                    FontLoadResult.Success(fontFamily, fontPath)

                } catch (e: Exception) {
                    val error = "Failed to load font $fontFamily: ${e.message}"
                    PluginLog.e(TAG, error, e)
                    FontLoadResult.Failure(error, fontPath)
                }
            }
        }
    }

    actual suspend fun loadFonts(fonts: Map<String, String>): Map<String, FontLoadResult> {
        PluginLog.d(TAG, "Batch loading ${fonts.size} fonts")
        return fonts.mapValues { (fontFamily, fontPath) ->
            loadFont(fontFamily, fontPath)
        }
    }

    actual fun isFontLoaded(fontFamily: String): Boolean {
        return loadedFonts.containsKey(fontFamily)
    }

    actual fun getLoadedFont(fontFamily: String): LoadedFont? {
        return loadedFonts[fontFamily]
    }

    actual fun getAllLoadedFonts(): List<LoadedFont> {
        return loadedFonts.values.toList()
    }

    actual fun unloadFont(fontFamily: String): Boolean {
        val removed = loadedFonts.remove(fontFamily)
        if (removed != null) {
            PluginLog.i(TAG, "Font removed from cache: $fontFamily (Note: JVM does not support font unregistration)")
            return true
        }
        return false
    }

    actual fun clearAllFonts(): Int {
        val count = loadedFonts.size
        loadedFonts.clear()
        if (count > 0) {
            PluginLog.i(TAG, "Cleared $count fonts from cache (Note: JVM does not support font unregistration)")
        }
        return count
    }

    actual fun validateFontFile(fontPath: String): Pair<Boolean, String?> {
        try {
            val file = File(fontPath)

            if (!file.exists()) return Pair(false, "Font file does not exist: $fontPath")
            if (!file.isFile) return Pair(false, "Font path is not a file: $fontPath")
            if (!file.canRead()) return Pair(false, "Font file is not readable: $fontPath")

            if (!FontLoaderUtils.isSupportedFontExtension(fontPath)) {
                val extension = fontPath.substringAfterLast(".", "")
                return Pair(false, "Unsupported font format: .$extension (supported: ${SUPPORTED_EXTENSIONS.joinToString()})")
            }

            val fileSize = file.length()
            if (fileSize == 0L) return Pair(false, "Font file is empty: $fontPath")
            if (fileSize > FontLoaderUtils.MAX_FONT_FILE_SIZE_BYTES) {
                return Pair(false, "Font file is too large: $fileSize bytes (max: ${FontLoaderUtils.MAX_FONT_FILE_SIZE_BYTES})")
            }

            return Pair(true, null)
        } catch (e: Exception) {
            return Pair(false, "Font validation error: ${e.message}")
        }
    }

    fun getDefaultFallbacks(): List<String> = JVM_DEFAULT_FALLBACKS

    fun getAvailableFonts(): List<String> {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        return ge.availableFontFamilyNames.toList()
    }
}
