package com.augmentalis.browseravanue.data.repository

import com.augmentalis.browseravanue.core.BrowserError
import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.data.local.dao.BrowserFavoriteDao
import com.augmentalis.browseravanue.data.local.dao.BrowserSettingsDao
import com.augmentalis.browseravanue.data.local.dao.BrowserTabDao
import com.augmentalis.browseravanue.data.mapper.BrowserSettingsMapper.toDomain
import com.augmentalis.browseravanue.data.mapper.BrowserSettingsMapper.toEntity
import com.augmentalis.browseravanue.data.mapper.FavoriteMapper.toDomain
import com.augmentalis.browseravanue.data.mapper.FavoriteMapper.toEntity
import com.augmentalis.browseravanue.data.mapper.TabMapper.toDomain
import com.augmentalis.browseravanue.data.mapper.TabMapper.toEntity
import com.augmentalis.browseravanue.domain.model.BrowserSettings
import com.augmentalis.browseravanue.domain.model.Favorite
import com.augmentalis.browseravanue.domain.model.Tab
import com.augmentalis.browseravanue.domain.repository.BrowserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Implementation of BrowserRepository
 *
 * Architecture:
 * - Lives in data layer
 * - Implements domain layer interface (Dependency Inversion)
 * - Uses DAOs for database operations
 * - Uses Mappers for entity ↔ domain conversions
 * - Wraps all operations in BrowserResult for type-safe error handling
 * - Catches exceptions and converts to BrowserError
 *
 * Dependency Injection:
 * ```
 * val repository: BrowserRepository = BrowserRepositoryImpl(
 *     tabDao = database.browserTabDao(),
 *     favoriteDao = database.browserFavoriteDao(),
 *     settingsDao = database.browserSettingsDao()
 * )
 * ```
 */
class BrowserRepositoryImpl(
    private val tabDao: BrowserTabDao,
    private val favoriteDao: BrowserFavoriteDao,
    private val settingsDao: BrowserSettingsDao
) : BrowserRepository {

    // ==========================================
    // Tab Operations
    // ==========================================

    override fun observeAllTabs(): Flow<BrowserResult<List<Tab>>> {
        return tabDao.observeAllTabs()
            .map { entities -> BrowserResult.Success(entities.toDomain()) }
            .catch { e -> emit(BrowserResult.Error(e.toBrowserError("observe all tabs"))) }
    }

    override suspend fun getAllTabs(): BrowserResult<List<Tab>> {
        return try {
            val entities = tabDao.getAllTabs()
            BrowserResult.Success(entities.toDomain())
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("get all tabs"))
        }
    }

    override fun observeTabById(tabId: String): Flow<BrowserResult<Tab>> {
        return tabDao.observeTabById(tabId)
            .map { entity ->
                entity?.let { BrowserResult.Success(it.toDomain()) }
                    ?: BrowserResult.Error(BrowserError.TabNotFound)
            }
            .catch { e -> emit(BrowserResult.Error(e.toBrowserError("observe tab $tabId"))) }
    }

    override suspend fun getTabById(tabId: String): BrowserResult<Tab> {
        return try {
            val entity = tabDao.getTabById(tabId)
            entity?.let { BrowserResult.Success(it.toDomain()) }
                ?: BrowserResult.Error(BrowserError.TabNotFound)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("get tab $tabId"))
        }
    }

    override suspend fun getMostRecentTab(): BrowserResult<Tab> {
        return try {
            val entity = tabDao.getMostRecentTab()
            entity?.let { BrowserResult.Success(it.toDomain()) }
                ?: BrowserResult.Error(BrowserError.NoActiveTabs)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("get most recent tab"))
        }
    }

    override suspend fun getTabCount(): BrowserResult<Int> {
        return try {
            val count = tabDao.getTabCount()
            BrowserResult.Success(count)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("get tab count"))
        }
    }

    override fun observeTabCount(): Flow<BrowserResult<Int>> {
        return tabDao.observeTabCount()
            .map { count -> BrowserResult.Success(count) }
            .catch { e -> emit(BrowserResult.Error(e.toBrowserError("observe tab count"))) }
    }

    override suspend fun searchTabs(query: String): BrowserResult<List<Tab>> {
        return try {
            val entities = tabDao.searchTabs(query)
            BrowserResult.Success(entities.toDomain())
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("search tabs"))
        }
    }

    override suspend fun createTab(url: String): BrowserResult<Tab> {
        return try {
            val tab = Tab.createNew(url)
            tabDao.insertTab(tab.toEntity())
            BrowserResult.Success(tab)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("create tab"))
        }
    }

    override suspend fun createTabWithSearch(query: String): BrowserResult<Tab> {
        return try {
            val tab = Tab.createWithSearch(query)
            tabDao.insertTab(tab.toEntity())
            BrowserResult.Success(tab)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("create tab with search"))
        }
    }

    override suspend fun updateTab(tab: Tab): BrowserResult<Unit> {
        return try {
            tabDao.updateTab(tab.toEntity())
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("update tab"))
        }
    }

    override suspend fun deleteTab(tabId: String): BrowserResult<Unit> {
        return try {
            tabDao.deleteTabById(tabId)
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("delete tab"))
        }
    }

    override suspend fun deleteAllTabs(): BrowserResult<Unit> {
        return try {
            tabDao.deleteAllTabs()
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("delete all tabs"))
        }
    }

    override suspend fun markTabAccessed(tabId: String): BrowserResult<Unit> {
        return try {
            tabDao.updateLastAccessed(tabId, System.currentTimeMillis())
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("mark tab accessed"))
        }
    }

    // ==========================================
    // Favorite Operations
    // ==========================================

    override fun observeAllFavorites(): Flow<BrowserResult<List<Favorite>>> {
        return favoriteDao.observeAllFavorites()
            .map { entities -> BrowserResult.Success(entities.toDomain()) }
            .catch { e -> emit(BrowserResult.Error(e.toBrowserError("observe all favorites"))) }
    }

    override suspend fun getAllFavorites(): BrowserResult<List<Favorite>> {
        return try {
            val entities = favoriteDao.getAllFavorites()
            BrowserResult.Success(entities.toDomain())
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("get all favorites"))
        }
    }

    override fun observeFavoritesByFolder(folder: String?): Flow<BrowserResult<List<Favorite>>> {
        return favoriteDao.observeFavoritesByFolder(folder)
            .map { entities -> BrowserResult.Success(entities.toDomain()) }
            .catch { e -> emit(BrowserResult.Error(e.toBrowserError("observe favorites in folder"))) }
    }

    override fun observeFolders(): Flow<BrowserResult<List<String>>> {
        return favoriteDao.observeFolders()
            .map { folders -> BrowserResult.Success(folders) }
            .catch { e -> emit(BrowserResult.Error(e.toBrowserError("observe folders"))) }
    }

    override suspend fun getFavoriteById(favoriteId: String): BrowserResult<Favorite> {
        return try {
            val entity = favoriteDao.getFavoriteById(favoriteId)
            entity?.let { BrowserResult.Success(it.toDomain()) }
                ?: BrowserResult.Error(BrowserError.FavoriteNotFound)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("get favorite"))
        }
    }

    override suspend fun isFavorited(url: String): BrowserResult<Boolean> {
        return try {
            val isFavorited = favoriteDao.isFavorited(url)
            BrowserResult.Success(isFavorited)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("check if favorited"))
        }
    }

    override fun observeIsFavorited(url: String): Flow<BrowserResult<Boolean>> {
        return favoriteDao.observeIsFavorited(url)
            .map { isFavorited -> BrowserResult.Success(isFavorited) }
            .catch { e -> emit(BrowserResult.Error(e.toBrowserError("observe if favorited"))) }
    }

    override suspend fun searchFavorites(query: String): BrowserResult<List<Favorite>> {
        return try {
            val entities = favoriteDao.searchFavorites(query)
            BrowserResult.Success(entities.toDomain())
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("search favorites"))
        }
    }

    override suspend fun addFavorite(url: String, title: String): BrowserResult<Favorite> {
        return try {
            // Check if already favorited
            val existing = favoriteDao.getFavoriteByUrl(url)
            if (existing != null) {
                return BrowserResult.Error(BrowserError.FavoriteAlreadyExists(url))
            }

            val favorite = Favorite.create(url, title)
            favoriteDao.insertFavorite(favorite.toEntity())
            BrowserResult.Success(favorite)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("add favorite"))
        }
    }

    override suspend fun addFavoriteFromTab(tab: Tab): BrowserResult<Favorite> {
        return try {
            // Check if already favorited
            val existing = favoriteDao.getFavoriteByUrl(tab.url)
            if (existing != null) {
                return BrowserResult.Error(BrowserError.FavoriteAlreadyExists(tab.url))
            }

            val favorite = Favorite.fromTab(tab)
            favoriteDao.insertFavorite(favorite.toEntity())
            BrowserResult.Success(favorite)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("add favorite from tab"))
        }
    }

    override suspend fun updateFavorite(favorite: Favorite): BrowserResult<Unit> {
        return try {
            favoriteDao.updateFavorite(favorite.toEntity())
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("update favorite"))
        }
    }

    override suspend fun deleteFavorite(favoriteId: String): BrowserResult<Unit> {
        return try {
            favoriteDao.deleteFavoriteById(favoriteId)
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("delete favorite"))
        }
    }

    override suspend fun deleteFavoriteByUrl(url: String): BrowserResult<Unit> {
        return try {
            favoriteDao.deleteFavoriteByUrl(url)
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("delete favorite by URL"))
        }
    }

    override suspend fun deleteAllFavorites(): BrowserResult<Unit> {
        return try {
            favoriteDao.deleteAllFavorites()
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("delete all favorites"))
        }
    }

    override suspend fun recordFavoriteVisit(favoriteId: String): BrowserResult<Unit> {
        return try {
            favoriteDao.recordVisit(favoriteId, System.currentTimeMillis())
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("record favorite visit"))
        }
    }

    // ==========================================
    // Navigation Operations
    // ==========================================

    override suspend fun navigateToUrl(tabId: String, url: String): BrowserResult<Unit> {
        return try {
            val tab = tabDao.getTabById(tabId)?.toDomain()
                ?: return BrowserResult.Error(BrowserError.TabNotFound)

            val updatedTab = tab.updatePageInfo(url)
            tabDao.updateTab(updatedTab.toEntity())
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("navigate to URL"))
        }
    }

    override suspend fun goBack(tabId: String): BrowserResult<Unit> {
        // WebView handles actual navigation, this just validates tab exists
        return try {
            val tab = tabDao.getTabById(tabId)?.toDomain()
                ?: return BrowserResult.Error(BrowserError.TabNotFound)

            if (!tab.canGoBack) {
                return BrowserResult.Error(BrowserError.Unknown("Cannot go back"))
            }

            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("go back"))
        }
    }

    override suspend fun goForward(tabId: String): BrowserResult<Unit> {
        // WebView handles actual navigation, this just validates tab exists
        return try {
            val tab = tabDao.getTabById(tabId)?.toDomain()
                ?: return BrowserResult.Error(BrowserError.TabNotFound)

            if (!tab.canGoForward) {
                return BrowserResult.Error(BrowserError.Unknown("Cannot go forward"))
            }

            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("go forward"))
        }
    }

    override suspend fun reload(tabId: String): BrowserResult<Unit> {
        // WebView handles actual reload, this just validates tab exists
        return try {
            tabDao.getTabById(tabId)
                ?: return BrowserResult.Error(BrowserError.TabNotFound)

            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("reload"))
        }
    }

    // ==========================================
    // Settings Operations
    // ==========================================

    override fun observeSettings(): Flow<BrowserResult<BrowserSettings>> {
        return settingsDao.observeSettings()
            .map { entity ->
                entity?.let { BrowserResult.Success(it.toDomain()) }
                    ?: BrowserResult.Success(BrowserSettings.default()) // Return defaults if not initialized
            }
            .catch { e -> emit(BrowserResult.Error(e.toBrowserError("observe settings"))) }
    }

    override suspend fun getSettings(): BrowserResult<BrowserSettings> {
        return try {
            val entity = settingsDao.getSettings()
            val settings = entity?.toDomain() ?: BrowserSettings.default()

            // Initialize if not exists
            if (entity == null) {
                settingsDao.insertSettings(settings.toEntity())
            }

            BrowserResult.Success(settings)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("get settings"))
        }
    }

    override suspend fun updateSettings(settings: BrowserSettings): BrowserResult<Unit> {
        return try {
            settingsDao.updateSettings(settings.toEntity())
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("update settings"))
        }
    }

    override suspend fun resetSettings(): BrowserResult<Unit> {
        return try {
            val defaults = BrowserSettings.default()
            settingsDao.insertSettings(defaults.toEntity())
            BrowserResult.Success(Unit)
        } catch (e: Exception) {
            BrowserResult.Error(e.toBrowserError("reset settings"))
        }
    }

    // ==========================================
    // Helper Functions
    // ==========================================

    /**
     * Convert Exception to BrowserError
     */
    private fun Exception.toBrowserError(operation: String): BrowserError {
        return BrowserError.DatabaseError(
            message = message ?: "Unknown error",
            operation = operation
        )
    }
}
