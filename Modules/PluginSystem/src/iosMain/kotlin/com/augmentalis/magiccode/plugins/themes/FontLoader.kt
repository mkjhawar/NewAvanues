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

        actual val SUPPORTED_EXTENSIONS: Set<String> = setOf("ttf", "otf")

        private val IOS_DEFAULT_FALLBACKS = listOf(
            "SF Pro",
            "San Francisco",
            "Helvetica Neue",
            "Helvetica",
            "Arial",
            "system"
        )
    }

    actual suspend fun loadFont(fontFamily: String, fontPath: String): FontLoadResult {
        PluginLog.d(TAG, "Loading font: $fontFamily from $fontPath")

        return withContext(Dispatchers.Default) {
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
                    val nsUrl = NSURL.fileURLWithPath(fontPath)
                    if (nsUrl == null) {
                        return@withContext FontLoadResult.Failure(
                            "Failed to create NSURL from path: $fontPath",
                            fontPath
                        )
                    }

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
                            return@withContext FontLoadResult.Failure(
                                "Failed to register font: $errorDescription",
                                fontPath
                            )
                        }
                    }

                    val fontFormat = FontLoaderUtils.getFontFormat(fontPath)

                    val loadedFont = LoadedFont(
                        fontFamily = fontFamily,
                        fontPath = fontPath,
                        format = fontFormat,
                        loadedAt = NSDate().timeIntervalSince1970.toLong() * 1000
                    )
                    loadedFonts[fontFamily] = loadedFont
                    registeredUrls[fontFamily] = nsUrl

                    PluginLog.i(TAG, "Font registered successfully: $fontFamily")
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

    actual fun clearAllFonts(): Int {
        var count = 0
        val families = loadedFonts.keys.toList()
        for (fontFamily in families) {
            if (unloadFont(fontFamily)) count++
        }
        if (count > 0) PluginLog.i(TAG, "Cleared $count fonts")
        return count
    }

    actual fun validateFontFile(fontPath: String): Pair<Boolean, String?> {
        try {
            val fileManager = NSFileManager.defaultManager

            if (!fileManager.fileExistsAtPath(fontPath)) {
                return Pair(false, "Font file does not exist: $fontPath")
            }
            if (!fileManager.isReadableFileAtPath(fontPath)) {
                return Pair(false, "Font file is not readable: $fontPath")
            }
            if (!FontLoaderUtils.isSupportedFontExtension(fontPath)) {
                val extension = fontPath.substringAfterLast(".", "")
                return Pair(false, "Unsupported font format: .$extension (supported: ${SUPPORTED_EXTENSIONS.joinToString()})")
            }

            val attributes = fileManager.attributesOfItemAtPath(fontPath, null)
            if (attributes != null) {
                val fileSize = (attributes[NSFileSize] as? NSNumber)?.longValue ?: 0L
                if (fileSize == 0L) return Pair(false, "Font file is empty: $fontPath")
                if (fileSize > FontLoaderUtils.MAX_FONT_FILE_SIZE_BYTES) {
                    return Pair(false, "Font file is too large: $fileSize bytes (max: ${FontLoaderUtils.MAX_FONT_FILE_SIZE_BYTES})")
                }
            }

            return Pair(true, null)
        } catch (e: Exception) {
            return Pair(false, "Font validation error: ${e.message}")
        }
    }

    fun getDefaultFallbacks(): List<String> = IOS_DEFAULT_FALLBACKS

    fun getAvailableFonts(): List<String> {
        val familyNames = mutableListOf<String>()
        val fontFamilyNames = CTFontManagerCopyAvailableFontFamilyNames()
        if (fontFamilyNames != null) {
            val count = CFArrayGetCount(fontFamilyNames)
            for (i in 0 until count) {
                val familyName = CFArrayGetValueAtIndex(fontFamilyNames, i)
                if (familyName != null) {
                    val name = CFBridgingRelease(familyName) as? String
                    if (name != null) familyNames.add(name)
                }
            }
            CFRelease(fontFamilyNames)
        }
        return familyNames
    }

    fun getCTFont(fontFamily: String, size: Double = 12.0): CPointer<*>? {
        if (!isFontLoaded(fontFamily)) return null
        val fontName = fontFamily.toCFString()
        val font = CTFontCreateWithName(fontName, size, null)
        CFRelease(fontName)
        return font
    }

    private fun String.toCFString(): CFStringRef {
        return CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)
            ?: throw IllegalStateException("Failed to create CFString")
    }
}
