/**
 * ThemePersistence.kt - Theme storage and persistence using DataStore
 * 
 * Handles saving, loading, and managing theme preferences and custom themes
 * using Android's DataStore for type-safe, asynchronous storage.
 */

package com.augmentalis.voiceui.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.IOException
import android.util.Log
import com.augmentalis.voiceui.theming.CustomTheme
import com.augmentalis.voiceui.theming.CustomThemeBuilder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * THEME PERSISTENCE MANAGER
 * 
 * Manages theme storage using DataStore for:
 * - Current theme selection
 * - Custom theme definitions
 * - Theme preferences
 * - Import/export functionality
 */
class ThemePersistence(private val context: Context) {
    
    companion object {
        private const val TAG = "ThemePersistence"
        private const val PREFERENCES_NAME = "voiceui_theme_preferences"
        
        // DataStore instance
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = PREFERENCES_NAME
        )
        
        // Preference keys
        private val CURRENT_THEME_ID = stringPreferencesKey("current_theme_id")
        private val CURRENT_THEME_NAME = stringPreferencesKey("current_theme_name")
        private val THEME_MODE = stringPreferencesKey("theme_mode") // light, dark, system
        private val DYNAMIC_COLORS_ENABLED = booleanPreferencesKey("dynamic_colors_enabled")
        private val CUSTOM_THEMES_JSON = stringPreferencesKey("custom_themes_json")
        private val FAVORITE_THEMES = stringSetPreferencesKey("favorite_themes")
        private val RECENT_THEMES = stringSetPreferencesKey("recent_themes")
        private val THEME_HISTORY = stringPreferencesKey("theme_history")
        
        // JSON serializer
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    
    private val dataStore = context.dataStore
    
    /**
     * Save current theme selection
     */
    suspend fun saveCurrentTheme(theme: AndroidTheme) {
        try {
            dataStore.edit { preferences ->
                preferences[CURRENT_THEME_ID] = theme.id
                preferences[CURRENT_THEME_NAME] = theme.name
                
                // Add to recent themes
                val recentThemes = preferences[RECENT_THEMES]?.toMutableSet() ?: mutableSetOf()
                recentThemes.add(theme.id)
                // Keep only last 10 recent themes
                if (recentThemes.size > 10) {
                    recentThemes.remove(recentThemes.first())
                }
                preferences[RECENT_THEMES] = recentThemes
                
                // Update theme history
                updateThemeHistory(preferences, theme.id)
            }
            
            Log.d(TAG, "Saved current theme: ${theme.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save current theme", e)
        }
    }
    
    /**
     * Get current theme ID
     */
    fun getCurrentThemeId(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(TAG, "Error reading preferences", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[CURRENT_THEME_ID]
            }
    }
    
    /**
     * Get current theme
     */
    suspend fun getCurrentTheme(): String? {
        return try {
            dataStore.data.first()[CURRENT_THEME_ID]
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current theme", e)
            null
        }
    }
    
    /**
     * Save theme mode (light/dark/system)
     */
    suspend fun saveThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }
    
    /**
     * Get theme mode
     */
    fun getThemeMode(): Flow<ThemeMode> {
        return dataStore.data
            .catch { 
                emit(emptyPreferences())
            }
            .map { preferences ->
                val modeName = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
                try {
                    ThemeMode.valueOf(modeName)
                } catch (e: Exception) {
                    ThemeMode.SYSTEM
                }
            }
    }
    
    /**
     * Save dynamic colors preference
     */
    suspend fun saveDynamicColorsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS_ENABLED] = enabled
        }
    }
    
    /**
     * Get dynamic colors preference
     */
    fun getDynamicColorsEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { 
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[DYNAMIC_COLORS_ENABLED] ?: true
            }
    }
    
    /**
     * Save custom theme
     */
    suspend fun saveCustomTheme(theme: SerializableTheme) {
        try {
            dataStore.edit { preferences ->
                val existingThemesJson = preferences[CUSTOM_THEMES_JSON]
                val existingThemes = if (existingThemesJson != null) {
                    try {
                        json.decodeFromString<List<SerializableTheme>>(existingThemesJson).toMutableList()
                    } catch (e: Exception) {
                        mutableListOf()
                    }
                } else {
                    mutableListOf()
                }
                
                // Remove if already exists (to update)
                existingThemes.removeAll { it.id == theme.id }
                // Add new/updated theme
                existingThemes.add(theme)
                
                // Save back to preferences
                preferences[CUSTOM_THEMES_JSON] = json.encodeToString(existingThemes)
            }
            
            Log.d(TAG, "Saved custom theme: ${theme.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom theme", e)
        }
    }
    
    /**
     * Load all custom themes
     */
    suspend fun loadCustomThemes(): List<AndroidTheme> {
        return try {
            val themesJson = dataStore.data.first()[CUSTOM_THEMES_JSON]
            if (themesJson != null) {
                val serializableThemes = json.decodeFromString<List<SerializableTheme>>(themesJson)
                serializableThemes.map { it.toAndroidTheme() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom themes", e)
            emptyList()
        }
    }
    
    /**
     * Delete custom theme
     */
    suspend fun deleteCustomTheme(themeId: String) {
        try {
            dataStore.edit { preferences ->
                val existingThemesJson = preferences[CUSTOM_THEMES_JSON]
                if (existingThemesJson != null) {
                    val existingThemes = json.decodeFromString<List<SerializableTheme>>(existingThemesJson)
                        .toMutableList()
                    existingThemes.removeAll { it.id == themeId }
                    preferences[CUSTOM_THEMES_JSON] = json.encodeToString(existingThemes)
                }
                
                // Remove from favorites if present
                val favorites = preferences[FAVORITE_THEMES]?.toMutableSet() ?: mutableSetOf()
                favorites.remove(themeId)
                preferences[FAVORITE_THEMES] = favorites
            }
            
            Log.d(TAG, "Deleted custom theme: $themeId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete custom theme", e)
        }
    }
    
    /**
     * Add theme to favorites
     */
    suspend fun addToFavorites(themeId: String) {
        dataStore.edit { preferences ->
            val favorites = preferences[FAVORITE_THEMES]?.toMutableSet() ?: mutableSetOf()
            favorites.add(themeId)
            preferences[FAVORITE_THEMES] = favorites
        }
    }
    
    /**
     * Remove theme from favorites
     */
    suspend fun removeFromFavorites(themeId: String) {
        dataStore.edit { preferences ->
            val favorites = preferences[FAVORITE_THEMES]?.toMutableSet() ?: mutableSetOf()
            favorites.remove(themeId)
            preferences[FAVORITE_THEMES] = favorites
        }
    }
    
    /**
     * Get favorite themes
     */
    fun getFavoriteThemes(): Flow<Set<String>> {
        return dataStore.data
            .catch { 
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[FAVORITE_THEMES] ?: emptySet()
            }
    }
    
    /**
     * Get recent themes
     */
    fun getRecentThemes(): Flow<Set<String>> {
        return dataStore.data
            .catch { 
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[RECENT_THEMES] ?: emptySet()
            }
    }
    
    /**
     * Export theme to JSON string
     */
    fun exportTheme(theme: AndroidTheme): String {
        val serializableTheme = SerializableTheme.fromAndroidTheme(theme)
        return json.encodeToString(serializableTheme)
    }
    
    /**
     * Import theme from JSON string
     */
    fun importTheme(jsonString: String): AndroidTheme? {
        return try {
            val serializableTheme = json.decodeFromString<SerializableTheme>(jsonString)
            serializableTheme.toAndroidTheme()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import theme", e)
            null
        }
    }
    
    /**
     * Export all themes
     */
    suspend fun exportAllThemes(): String {
        val themes = loadCustomThemes()
        val serializableThemes = themes.map { SerializableTheme.fromAndroidTheme(it) }
        return json.encodeToString(serializableThemes)
    }
    
    /**
     * Import multiple themes
     */
    suspend fun importThemes(jsonString: String): Int {
        return try {
            val serializableThemes = json.decodeFromString<List<SerializableTheme>>(jsonString)
            for (theme in serializableThemes) {
                saveCustomTheme(theme)
            }
            serializableThemes.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import themes", e)
            0
        }
    }
    
    /**
     * Clear all preferences
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Update theme history
     */
    private fun updateThemeHistory(preferences: MutablePreferences, themeId: String) {
        val historyJson = preferences[THEME_HISTORY]
        val history = if (historyJson != null) {
            try {
                json.decodeFromString<ThemeHistory>(historyJson)
            } catch (e: Exception) {
                ThemeHistory()
            }
        } else {
            ThemeHistory()
        }
        
        history.addEntry(themeId)
        preferences[THEME_HISTORY] = json.encodeToString(history)
    }
}

/**
 * Theme mode enum
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Serializable theme for storage
 */
@Serializable
data class SerializableTheme(
    val id: String,
    val name: String,
    val description: String,
    val minSdkVersion: Int = 14,
    val colors: SerializableColors,
    val typography: SerializableTypography,
    val spacing: Map<String, Float> = emptyMap(),
    val shapes: Map<String, Float> = emptyMap(),
    val animations: Map<String, SerializableAnimation> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    fun toAndroidTheme(): AndroidTheme {
        val customTheme = CustomThemeBuilder()
            .name(name)
            .colors {
                primary(Color(colors.primary))
                secondary(Color(colors.secondary))
                background(Color(colors.background))
                surface(Color(colors.surface))
                error(Color(colors.error))
                onPrimary(Color(colors.onPrimary))
                onSecondary(Color(colors.onSecondary))
                onBackground(Color(colors.onBackground))
                onSurface(Color(colors.onSurface))
                onError(Color(colors.onError))
            }
            .typography {
                h1(typography.h1Size, FontWeight(typography.h1Weight))
                h2(typography.h2Size, FontWeight(typography.h2Weight))
                h3(typography.h3Size, FontWeight(typography.h3Weight))
                body1(typography.body1Size, FontWeight(typography.body1Weight))
                body2(typography.body2Size, FontWeight(typography.body2Weight))
            }
            .build()
        
        return AndroidTheme(
            id = id,
            name = name,
            description = description,
            minSdkVersion = minSdkVersion,
            theme = customTheme
        )
    }
    
    companion object {
        fun fromAndroidTheme(theme: AndroidTheme): SerializableTheme {
            return SerializableTheme(
                id = theme.id,
                name = theme.name,
                description = theme.description,
                minSdkVersion = theme.minSdkVersion,
                colors = SerializableColors(
                    primary = theme.theme.colors.primary.value.toLong(),
                    secondary = theme.theme.colors.secondary.value.toLong(),
                    background = theme.theme.colors.background.value.toLong(),
                    surface = theme.theme.colors.surface.value.toLong(),
                    error = theme.theme.colors.error.value.toLong(),
                    onPrimary = theme.theme.colors.onPrimary.value.toLong(),
                    onSecondary = theme.theme.colors.onSecondary.value.toLong(),
                    onBackground = theme.theme.colors.onBackground.value.toLong(),
                    onSurface = theme.theme.colors.onSurface.value.toLong(),
                    onError = theme.theme.colors.onError.value.toLong()
                ),
                typography = SerializableTypography(
                    h1Size = theme.theme.typography.h1.fontSize.value,
                    h1Weight = theme.theme.typography.h1.fontWeight?.weight ?: 400,
                    h2Size = theme.theme.typography.h2.fontSize.value,
                    h2Weight = theme.theme.typography.h2.fontWeight?.weight ?: 400,
                    h3Size = theme.theme.typography.h3.fontSize.value,
                    h3Weight = theme.theme.typography.h3.fontWeight?.weight ?: 400,
                    body1Size = theme.theme.typography.body1.fontSize.value,
                    body1Weight = theme.theme.typography.body1.fontWeight?.weight ?: 400,
                    body2Size = theme.theme.typography.body2.fontSize.value,
                    body2Weight = theme.theme.typography.body2.fontWeight?.weight ?: 400
                )
            )
        }
    }
}

/**
 * Serializable colors
 */
@Serializable
data class SerializableColors(
    val primary: Long,
    val secondary: Long,
    val background: Long,
    val surface: Long,
    val error: Long,
    val onPrimary: Long,
    val onSecondary: Long,
    val onBackground: Long,
    val onSurface: Long,
    val onError: Long
)

/**
 * Serializable typography
 */
@Serializable
data class SerializableTypography(
    val h1Size: Float,
    val h1Weight: Int,
    val h2Size: Float,
    val h2Weight: Int,
    val h3Size: Float,
    val h3Weight: Int,
    val body1Size: Float,
    val body1Weight: Int,
    val body2Size: Float,
    val body2Weight: Int
)

/**
 * Serializable animation
 */
@Serializable
data class SerializableAnimation(
    val duration: Long,
    val easing: String
)

/**
 * Theme history tracking
 */
@Serializable
data class ThemeHistory(
    val entries: MutableList<ThemeHistoryEntry> = mutableListOf()
) {
    fun addEntry(themeId: String) {
        entries.add(ThemeHistoryEntry(themeId, System.currentTimeMillis()))
        // Keep only last 50 entries
        if (entries.size > 50) {
            entries.removeAt(0)
        }
    }
}

/**
 * Theme history entry
 */
@Serializable
data class ThemeHistoryEntry(
    val themeId: String,
    val timestamp: Long
)

// Extension to convert Color value
private val Color.value: ULong
    get() = this.value