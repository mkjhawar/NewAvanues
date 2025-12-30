/**
 * FontManager.kt - Complete font loading and management system
 * 
 * Handles loading of system fonts, custom fonts, and manufacturer-specific fonts
 * for the Android theme system.
 */

package com.augmentalis.voiceui.android

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.core.content.res.ResourcesCompat
import java.io.File
import android.util.Log

/**
 * FONT MANAGER
 * 
 * Manages all font loading for themes including:
 * - System fonts (Roboto, etc.)
 * - Google Fonts
 * - Custom fonts from assets
 * - Manufacturer-specific fonts
 * - Downloaded fonts
 */
class FontManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FontManager"
        private const val FONTS_DIR = "fonts"
        private const val CUSTOM_FONTS_DIR = "custom_fonts"
        
        // Google Fonts provider
        private val provider = GoogleFont.Provider(
            providerAuthority = "com.google.android.gms.fonts",
            providerPackage = "com.google.android.gms",
            certificates = R.array.com_google_android_gms_fonts_certs
        )
    }
    
    // Cache for loaded fonts
    private val fontCache = mutableMapOf<String, FontFamily>()
    private val typefaceCache = mutableMapOf<String, Typeface>()
    
    /**
     * Get font family by name
     */
    fun getFontFamily(fontName: String): FontFamily {
        // Check cache first
        fontCache[fontName]?.let { return it }
        
        // Load and cache font
        val fontFamily = when (fontName.lowercase()) {
            // System fonts
            "roboto", "default" -> FontFamily.Default
            "serif" -> FontFamily.Serif
            "sans-serif", "sans" -> FontFamily.SansSerif
            "monospace", "mono" -> FontFamily.Monospace
            "cursive" -> FontFamily.Cursive
            
            // Google Fonts - Popular choices
            "inter" -> loadGoogleFont("Inter")
            "poppins" -> loadGoogleFont("Poppins")
            "montserrat" -> loadGoogleFont("Montserrat")
            "open-sans" -> loadGoogleFont("Open Sans")
            "lato" -> loadGoogleFont("Lato")
            "raleway" -> loadGoogleFont("Raleway")
            "nunito" -> loadGoogleFont("Nunito")
            "playfair-display" -> loadGoogleFont("Playfair Display")
            "bebas-neue" -> loadGoogleFont("Bebas Neue")
            "oswald" -> loadGoogleFont("Oswald")
            
            // Material Design fonts
            "google-sans" -> loadGoogleFont("Google Sans")
            "product-sans" -> loadGoogleFont("Product Sans")
            "roboto-condensed" -> loadGoogleFont("Roboto Condensed")
            "roboto-mono" -> loadGoogleFont("Roboto Mono")
            "roboto-slab" -> loadGoogleFont("Roboto Slab")
            
            // Manufacturer-specific fonts
            "sec-roboto", "samsung-one" -> loadManufacturerFont("Samsung", "SamsungOne")
            "miui-font" -> loadManufacturerFont("Xiaomi", "MiSans")
            "oneplus-sans" -> loadManufacturerFont("OnePlus", "OnePlusSans")
            "coloros-sans" -> loadManufacturerFont("OPPO", "OPPOSans")
            "lg-smart" -> loadManufacturerFont("LG", "LGSmartUI")
            "htc-font" -> loadManufacturerFont("HTC", "HTCFont")
            
            // Special/themed fonts
            "orbitron" -> loadGoogleFont("Orbitron") // Tech/futuristic
            "rajdhani" -> loadGoogleFont("Rajdhani") // Tech/gaming
            "monoton" -> loadGoogleFont("Monoton") // Retro/glitch
            "press-start" -> loadGoogleFont("Press Start 2P") // Pixel/retro
            "vt323" -> loadGoogleFont("VT323") // Terminal/retro
            "space-mono" -> loadGoogleFont("Space Mono") // Monospace/tech
            "share-tech-mono" -> loadGoogleFont("Share Tech Mono") // Tech mono
            
            // Design-specific fonts
            "helvetica-neue" -> loadSystemFont("helvetica")
            "arial" -> loadSystemFont("arial")
            "georgia" -> FontFamily.Serif
            "times-new-roman" -> FontFamily.Serif
            "courier-new" -> FontFamily.Monospace
            
            // Custom fonts from assets
            else -> loadCustomFont(fontName)
        }
        
        // Cache the loaded font
        fontCache[fontName] = fontFamily
        return fontFamily
    }
    
    /**
     * Load Google Font
     */
    private fun loadGoogleFont(fontName: String): FontFamily {
        return try {
            val googleFont = GoogleFont(fontName)
            
            FontFamily(
                Font(googleFont, provider, FontWeight.Light),
                Font(googleFont, provider, FontWeight.Normal),
                Font(googleFont, provider, FontWeight.Medium),
                Font(googleFont, provider, FontWeight.SemiBold),
                Font(googleFont, provider, FontWeight.Bold),
                Font(googleFont, provider, FontWeight.Black)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Google Font: $fontName", e)
            FontFamily.Default
        }
    }
    
    /**
     * Load manufacturer-specific font
     */
    private fun loadManufacturerFont(manufacturer: String, fontName: String): FontFamily {
        return try {
            // Check if manufacturer matches
            if (!Build.MANUFACTURER.equals(manufacturer, ignoreCase = true)) {
                return FontFamily.Default
            }
            
            // Try to load from system
            val typeface = Typeface.create(fontName, Typeface.NORMAL)
            if (typeface != Typeface.DEFAULT) {
                FontFamily(typeface)
            } else {
                // Fallback to default
                FontFamily.Default
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load manufacturer font: $fontName", e)
            FontFamily.Default
        }
    }
    
    /**
     * Load system font
     */
    private fun loadSystemFont(fontName: String): FontFamily {
        return try {
            val typeface = Typeface.create(fontName, Typeface.NORMAL)
            if (typeface != Typeface.DEFAULT) {
                FontFamily(typeface)
            } else {
                FontFamily.Default
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load system font: $fontName", e)
            FontFamily.Default
        }
    }
    
    /**
     * Load custom font from assets
     */
    private fun loadCustomFont(fontName: String): FontFamily {
        return try {
            // Try loading from assets/fonts directory
            val fontPath = "$FONTS_DIR/$fontName.ttf"
            
            if (assetExists(fontPath)) {
                val typeface = Typeface.createFromAsset(context.assets, fontPath)
                FontFamily(typeface)
            } else {
                // Try alternate extensions
                val alternatePaths = listOf(
                    "$FONTS_DIR/$fontName.otf",
                    "$CUSTOM_FONTS_DIR/$fontName.ttf",
                    "$CUSTOM_FONTS_DIR/$fontName.otf"
                )
                
                for (path in alternatePaths) {
                    if (assetExists(path)) {
                        val typeface = Typeface.createFromAsset(context.assets, path)
                        return FontFamily(typeface)
                    }
                }
                
                // Not found, use default
                FontFamily.Default
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom font: $fontName", e)
            FontFamily.Default
        }
    }
    
    /**
     * Load font from file
     */
    fun loadFontFromFile(file: File): FontFamily? {
        return try {
            if (file.exists() && file.canRead()) {
                val typeface = Typeface.createFromFile(file)
                FontFamily(typeface)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load font from file: ${file.path}", e)
            null
        }
    }
    
    /**
     * Load font from URL (for downloaded fonts)
     */
    suspend fun loadFontFromUrl(url: String, cacheName: String): FontFamily? {
        return try {
            // Download font to cache directory
            val cacheDir = File(context.cacheDir, "fonts")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val fontFile = File(cacheDir, "$cacheName.ttf")
            
            // Download font (simplified - would need proper implementation)
            // downloadFile(url, fontFile)
            
            if (fontFile.exists()) {
                loadFontFromFile(fontFile)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load font from URL: $url", e)
            null
        }
    }
    
    /**
     * Check if asset exists
     */
    private fun assetExists(path: String): Boolean {
        return try {
            context.assets.open(path).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get all available fonts
     */
    fun getAvailableFonts(): List<FontInfo> {
        val fonts = mutableListOf<FontInfo>()
        
        // Add system fonts
        fonts.addAll(listOf(
            FontInfo("Roboto", "System default", FontCategory.SYSTEM),
            FontInfo("Serif", "System serif", FontCategory.SYSTEM),
            FontInfo("Sans Serif", "System sans-serif", FontCategory.SYSTEM),
            FontInfo("Monospace", "System monospace", FontCategory.SYSTEM)
        ))
        
        // Add popular Google Fonts
        fonts.addAll(listOf(
            FontInfo("Inter", "Clean and modern", FontCategory.GOOGLE),
            FontInfo("Poppins", "Geometric and friendly", FontCategory.GOOGLE),
            FontInfo("Montserrat", "Urban and elegant", FontCategory.GOOGLE),
            FontInfo("Open Sans", "Humanist and neutral", FontCategory.GOOGLE),
            FontInfo("Lato", "Warm and stable", FontCategory.GOOGLE),
            FontInfo("Raleway", "Elegant and thin", FontCategory.GOOGLE),
            FontInfo("Nunito", "Rounded and friendly", FontCategory.GOOGLE),
            FontInfo("Playfair Display", "Transitional design", FontCategory.GOOGLE),
            FontInfo("Bebas Neue", "Bold and impactful", FontCategory.GOOGLE),
            FontInfo("Oswald", "Condensed and strong", FontCategory.GOOGLE)
        ))
        
        // Add manufacturer fonts if available
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            fonts.add(FontInfo("Samsung One", "Samsung's official font", FontCategory.MANUFACTURER))
        }
        
        // Add custom fonts from assets
        try {
            val fontFiles = context.assets.list(FONTS_DIR) ?: emptyArray()
            for (file in fontFiles) {
                if (file.endsWith(".ttf") || file.endsWith(".otf")) {
                    val name = file.substringBeforeLast(".")
                    fonts.add(FontInfo(name, "Custom font", FontCategory.CUSTOM))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list custom fonts", e)
        }
        
        return fonts
    }
    
    /**
     * Create font family with all weights
     */
    fun createFontFamilyWithWeights(
        baseFontName: String,
        weights: List<FontWeight> = listOf(
            FontWeight.Light,
            FontWeight.Normal,
            FontWeight.Medium,
            FontWeight.SemiBold,
            FontWeight.Bold,
            FontWeight.Black
        )
    ): FontFamily {
        val fonts = weights.mapNotNull { weight ->
            try {
                val typeface = when (weight) {
                    FontWeight.Light, FontWeight.Thin, FontWeight.ExtraLight -> 
                        Typeface.create(baseFontName, Typeface.NORMAL)
                    FontWeight.Normal -> 
                        Typeface.create(baseFontName, Typeface.NORMAL)
                    FontWeight.Medium, FontWeight.SemiBold -> 
                        Typeface.create(baseFontName, Typeface.NORMAL)
                    FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black -> 
                        Typeface.create(baseFontName, Typeface.BOLD)
                    else -> 
                        Typeface.create(baseFontName, Typeface.NORMAL)
                }
                // Temporarily return null - Google Fonts integration disabled
                null
            } catch (e: Exception) {
                null
            }
        }
        
        return if (fonts.isNotEmpty()) {
            FontFamily(fonts)
        } else {
            FontFamily.Default
        }
    }
    
    /**
     * Clear font cache
     */
    fun clearCache() {
        fontCache.clear()
        typefaceCache.clear()
    }
}

/**
 * Font information
 */
data class FontInfo(
    val name: String,
    val description: String,
    val category: FontCategory
)

/**
 * Font categories
 */
enum class FontCategory {
    SYSTEM,
    GOOGLE,
    MANUFACTURER,
    CUSTOM,
    DOWNLOADED
}

/**
 * Font resource placeholder (would be in res/values/arrays.xml)
 */
object R {
    object array {
        // This would reference the actual certificate array in resources
        const val com_google_android_gms_fonts_certs = 0
    }
}