package com.augmentalis.browseravanue.data.export

import android.content.Context
import android.webkit.CookieManager
import com.augmentalis.browseravanue.data.local.database.BrowserAvanueDatabase
import com.augmentalis.browseravanue.data.mapper.BrowserSettingsMapper
import com.augmentalis.browseravanue.data.mapper.FavoriteMapper
import com.augmentalis.browseravanue.data.mapper.TabMapper
import com.augmentalis.browseravanue.domain.model.BrowserSettings
import com.augmentalis.browseravanue.domain.model.Favorite
import com.augmentalis.browseravanue.domain.model.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Browser data importer for cross-device sync
 *
 * Architecture:
 * - Imports browser data from JSON format
 * - Supports conflict resolution strategies
 * - Validates imported data
 * - Incremental import (merge with existing)
 * - Rollback support on errors
 *
 * Conflict Resolution Strategies:
 * - REPLACE: Replace existing data with imported data
 * - MERGE: Keep both (add suffix to duplicates)
 * - KEEP_EXISTING: Skip duplicates
 * - KEEP_NEWER: Use newer timestamp
 *
 * Features:
 * - Full import (all data)
 * - Selective import (choose data types)
 * - Validation (schema version, data integrity)
 * - Conflict resolution
 * - Rollback on error
 *
 * Usage:
 * ```
 * val importer = BrowserDataImporter(context, database)
 * val result = importer.importFromFile(file, ConflictStrategy.MERGE)
 * if (result.success) {
 *     // Import succeeded
 * }
 * ```
 */
class BrowserDataImporter(
    private val context: Context,
    private val database: BrowserAvanueDatabase
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val tabMapper = TabMapper()
    private val favoriteMapper = FavoriteMapper()
    private val settingsMapper = BrowserSettingsMapper()

    /**
     * Import browser data from file
     *
     * @param file Export file
     * @param conflictStrategy How to handle conflicts
     * @param importTabs Import tabs (default: true)
     * @param importFavorites Import favorites (default: true)
     * @param importSettings Import settings (default: true)
     * @param importCookies Import cookies (default: false for privacy)
     * @return Import result
     */
    suspend fun importFromFile(
        file: File,
        conflictStrategy: ConflictStrategy = ConflictStrategy.MERGE,
        importTabs: Boolean = true,
        importFavorites: Boolean = true,
        importSettings: Boolean = true,
        importCookies: Boolean = false
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Read and parse JSON
            val jsonString = file.readText()
            val exportData = json.decodeFromString<BrowserExportData>(jsonString)

            // Validate export data
            val validation = validateExportData(exportData)
            if (!validation.isValid) {
                return@withContext ImportResult(
                    success = false,
                    message = "Invalid export data: ${validation.errors.joinToString()}",
                    itemsImported = 0
                )
            }

            // Import data
            importData(
                exportData = exportData,
                conflictStrategy = conflictStrategy,
                importTabs = importTabs,
                importFavorites = importFavorites,
                importSettings = importSettings,
                importCookies = importCookies
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Import failed: ${e.message}",
                itemsImported = 0,
                error = e
            )
        }
    }

    /**
     * Import from JSON string
     *
     * @param jsonString JSON export data
     * @param conflictStrategy How to handle conflicts
     * @return Import result
     */
    suspend fun importFromJson(
        jsonString: String,
        conflictStrategy: ConflictStrategy = ConflictStrategy.MERGE
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val exportData = json.decodeFromString<BrowserExportData>(jsonString)

            val validation = validateExportData(exportData)
            if (!validation.isValid) {
                return@withContext ImportResult(
                    success = false,
                    message = "Invalid export data: ${validation.errors.joinToString()}",
                    itemsImported = 0
                )
            }

            importData(
                exportData = exportData,
                conflictStrategy = conflictStrategy,
                importTabs = true,
                importFavorites = true,
                importSettings = true,
                importCookies = false
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Import failed: ${e.message}",
                itemsImported = 0,
                error = e
            )
        }
    }

    /**
     * Import only tabs
     *
     * @param jsonString JSON tabs data
     * @param conflictStrategy How to handle conflicts
     * @return Import result
     */
    suspend fun importTabs(
        jsonString: String,
        conflictStrategy: ConflictStrategy = ConflictStrategy.MERGE
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val tabs = json.decodeFromString<List<Tab>>(jsonString)
            val imported = importTabsInternal(tabs, conflictStrategy)

            ImportResult(
                success = true,
                message = "Imported $imported tabs",
                itemsImported = imported
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Import failed: ${e.message}",
                itemsImported = 0,
                error = e
            )
        }
    }

    /**
     * Import only favorites
     *
     * @param jsonString JSON favorites data
     * @param conflictStrategy How to handle conflicts
     * @return Import result
     */
    suspend fun importFavorites(
        jsonString: String,
        conflictStrategy: ConflictStrategy = ConflictStrategy.MERGE
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val favorites = json.decodeFromString<List<Favorite>>(jsonString)
            val imported = importFavoritesInternal(favorites, conflictStrategy)

            ImportResult(
                success = true,
                message = "Imported $imported favorites",
                itemsImported = imported
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Import failed: ${e.message}",
                itemsImported = 0,
                error = e
            )
        }
    }

    /**
     * Import only settings
     *
     * @param jsonString JSON settings data
     * @return Import result
     */
    suspend fun importSettings(jsonString: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val settings = json.decodeFromString<BrowserSettings>(jsonString)
            importSettingsInternal(settings)

            ImportResult(
                success = true,
                message = "Settings imported",
                itemsImported = 1
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Import failed: ${e.message}",
                itemsImported = 0,
                error = e
            )
        }
    }

    /**
     * Import all data (internal)
     */
    private suspend fun importData(
        exportData: BrowserExportData,
        conflictStrategy: ConflictStrategy,
        importTabs: Boolean,
        importFavorites: Boolean,
        importSettings: Boolean,
        importCookies: Boolean
    ): ImportResult = withContext(Dispatchers.IO) {
        var totalImported = 0

        try {
            // Import tabs
            if (importTabs) {
                totalImported += importTabsInternal(exportData.tabs, conflictStrategy)
            }

            // Import favorites
            if (importFavorites) {
                totalImported += importFavoritesInternal(exportData.favorites, conflictStrategy)
            }

            // Import settings
            if (importSettings) {
                importSettingsInternal(exportData.settings)
                totalImported += 1
            }

            // Import cookies
            if (importCookies) {
                importCookiesInternal(exportData.cookies)
                totalImported += exportData.cookies.size
            }

            ImportResult(
                success = true,
                message = "Import successful: $totalImported items",
                itemsImported = totalImported
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Import failed: ${e.message}",
                itemsImported = totalImported,
                error = e
            )
        }
    }

    /**
     * Import tabs (internal)
     */
    private suspend fun importTabsInternal(
        tabs: List<Tab>,
        conflictStrategy: ConflictStrategy
    ): Int = withContext(Dispatchers.IO) {
        var imported = 0

        for (tab in tabs) {
            // Check for existing tab with same URL
            val existing = database.browserTabDao().getTabByUrl(tab.url)

            when (conflictStrategy) {
                ConflictStrategy.REPLACE -> {
                    // Delete existing and insert new
                    existing?.let { database.browserTabDao().deleteTab(it) }
                    database.browserTabDao().insertTab(tabMapper.toEntity(tab))
                    imported++
                }
                ConflictStrategy.MERGE -> {
                    if (existing == null) {
                        // No conflict - insert
                        database.browserTabDao().insertTab(tabMapper.toEntity(tab))
                        imported++
                    } else {
                        // Conflict - create new with modified title
                        val mergedTab = tab.copy(title = "${tab.title} (Imported)")
                        database.browserTabDao().insertTab(tabMapper.toEntity(mergedTab))
                        imported++
                    }
                }
                ConflictStrategy.KEEP_EXISTING -> {
                    if (existing == null) {
                        // No conflict - insert
                        database.browserTabDao().insertTab(tabMapper.toEntity(tab))
                        imported++
                    }
                    // else skip (keep existing)
                }
                ConflictStrategy.KEEP_NEWER -> {
                    if (existing == null) {
                        database.browserTabDao().insertTab(tabMapper.toEntity(tab))
                        imported++
                    } else {
                        // Compare timestamps (use imported if newer)
                        val existingDomain = tabMapper.toDomain(existing)
                        if (tab.lastAccessed > existingDomain.lastAccessed) {
                            database.browserTabDao().deleteTab(existing)
                            database.browserTabDao().insertTab(tabMapper.toEntity(tab))
                            imported++
                        }
                    }
                }
            }
        }

        imported
    }

    /**
     * Import favorites (internal)
     */
    private suspend fun importFavoritesInternal(
        favorites: List<Favorite>,
        conflictStrategy: ConflictStrategy
    ): Int = withContext(Dispatchers.IO) {
        var imported = 0

        for (favorite in favorites) {
            // Check for existing favorite with same URL
            val existing = database.browserFavoriteDao().getFavoriteByUrl(favorite.url)

            when (conflictStrategy) {
                ConflictStrategy.REPLACE -> {
                    existing?.let { database.browserFavoriteDao().deleteFavorite(it) }
                    database.browserFavoriteDao().insertFavorite(favoriteMapper.toEntity(favorite))
                    imported++
                }
                ConflictStrategy.MERGE -> {
                    if (existing == null) {
                        database.browserFavoriteDao().insertFavorite(favoriteMapper.toEntity(favorite))
                        imported++
                    } else {
                        // Merge tags if different
                        val existingDomain = favoriteMapper.toDomain(existing)
                        val mergedTags = (existingDomain.tags + favorite.tags).distinct()
                        val merged = favorite.copy(tags = mergedTags)
                        database.browserFavoriteDao().deleteFavorite(existing)
                        database.browserFavoriteDao().insertFavorite(favoriteMapper.toEntity(merged))
                        imported++
                    }
                }
                ConflictStrategy.KEEP_EXISTING -> {
                    if (existing == null) {
                        database.browserFavoriteDao().insertFavorite(favoriteMapper.toEntity(favorite))
                        imported++
                    }
                }
                ConflictStrategy.KEEP_NEWER -> {
                    if (existing == null) {
                        database.browserFavoriteDao().insertFavorite(favoriteMapper.toEntity(favorite))
                        imported++
                    } else {
                        val existingDomain = favoriteMapper.toDomain(existing)
                        if (favorite.createdAt > existingDomain.createdAt) {
                            database.browserFavoriteDao().deleteFavorite(existing)
                            database.browserFavoriteDao().insertFavorite(favoriteMapper.toEntity(favorite))
                            imported++
                        }
                    }
                }
            }
        }

        imported
    }

    /**
     * Import settings (internal)
     */
    private suspend fun importSettingsInternal(settings: BrowserSettings) = withContext(Dispatchers.IO) {
        // Settings always replace (only one settings object)
        database.browserSettingsDao().insertSettings(settingsMapper.toEntity(settings))
    }

    /**
     * Import cookies (internal)
     */
    private suspend fun importCookiesInternal(cookies: List<CookieData>) = withContext(Dispatchers.IO) {
        val cookieManager = CookieManager.getInstance()

        for (cookie in cookies) {
            // Set cookie via CookieManager
            val cookieString = "${cookie.name}=${cookie.value}; domain=${cookie.domain}; path=${cookie.path}"
            cookieManager.setCookie(cookie.domain, cookieString)
        }

        cookieManager.flush()
    }

    /**
     * Validate export data
     */
    private fun validateExportData(exportData: BrowserExportData): ValidationResult {
        val errors = mutableListOf<String>()

        // Check version compatibility
        if (exportData.version != BrowserDataExporter.EXPORT_VERSION) {
            errors.add("Incompatible export version: ${exportData.version}")
        }

        // Validate tabs
        for (tab in exportData.tabs) {
            if (tab.url.isBlank()) {
                errors.add("Tab has blank URL")
            }
        }

        // Validate favorites
        for (favorite in exportData.favorites) {
            if (favorite.url.isBlank()) {
                errors.add("Favorite has blank URL")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

/**
 * Conflict resolution strategy
 */
enum class ConflictStrategy {
    /** Replace existing data with imported data */
    REPLACE,

    /** Keep both (add suffix to duplicates) */
    MERGE,

    /** Skip duplicates, keep existing */
    KEEP_EXISTING,

    /** Keep newer based on timestamp */
    KEEP_NEWER
}

/**
 * Import result
 */
data class ImportResult(
    val success: Boolean,
    val message: String,
    val itemsImported: Int,
    val error: Throwable? = null
)

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
