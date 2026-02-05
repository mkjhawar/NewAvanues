package com.augmentalis.browseravanue.data.local.dao

import androidx.room.*
import com.augmentalis.browseravanue.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO interface for Browser Favorite/Bookmark operations
 *
 * Architecture:
 * - Interface-based design works with BOTH shared and standalone databases
 * - Shared mode: Implemented by AvanuesDatabase
 * - Standalone mode: Implemented by BrowserAvanueDatabase
 * - Flow-based reactive queries
 * - Suspend functions for CRUD operations
 */
@Dao
interface BrowserFavoriteDao {

    /**
     * Observe all favorites ordered by title
     */
    @Query("SELECT * FROM favorites ORDER BY title ASC")
    fun observeAllFavorites(): Flow<List<FavoriteEntity>>

    /**
     * Get all favorites (one-time query)
     */
    @Query("SELECT * FROM favorites ORDER BY title ASC")
    suspend fun getAllFavorites(): List<FavoriteEntity>

    /**
     * Observe favorites ordered by most visited
     */
    @Query("SELECT * FROM favorites ORDER BY visit_count DESC, title ASC")
    fun observeFavoritesByVisitCount(): Flow<List<FavoriteEntity>>

    /**
     * Observe favorites ordered by most recently visited
     */
    @Query("SELECT * FROM favorites ORDER BY last_visited DESC NULLS LAST, title ASC")
    fun observeFavoritesByRecentVisit(): Flow<List<FavoriteEntity>>

    /**
     * Observe favorites in specific folder
     */
    @Query("SELECT * FROM favorites WHERE folder = :folder ORDER BY title ASC")
    fun observeFavoritesByFolder(folder: String?): Flow<List<FavoriteEntity>>

    /**
     * Get favorites in specific folder
     */
    @Query("SELECT * FROM favorites WHERE folder = :folder ORDER BY title ASC")
    suspend fun getFavoritesByFolder(folder: String?): List<FavoriteEntity>

    /**
     * Observe all unique folders
     */
    @Query("SELECT DISTINCT folder FROM favorites WHERE folder IS NOT NULL ORDER BY folder ASC")
    fun observeFolders(): Flow<List<String>>

    /**
     * Get all unique folders
     */
    @Query("SELECT DISTINCT folder FROM favorites WHERE folder IS NOT NULL ORDER BY folder ASC")
    suspend fun getFolders(): List<String>

    /**
     * Observe specific favorite by ID
     */
    @Query("SELECT * FROM favorites WHERE id = :favoriteId")
    fun observeFavoriteById(favoriteId: String): Flow<FavoriteEntity?>

    /**
     * Get specific favorite by ID
     */
    @Query("SELECT * FROM favorites WHERE id = :favoriteId")
    suspend fun getFavoriteById(favoriteId: String): FavoriteEntity?

    /**
     * Get favorite by URL (for checking if URL is favorited)
     */
    @Query("SELECT * FROM favorites WHERE url = :url LIMIT 1")
    suspend fun getFavoriteByUrl(url: String): FavoriteEntity?

    /**
     * Check if URL is favorited
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE url = :url)")
    suspend fun isFavorited(url: String): Boolean

    /**
     * Observe if URL is favorited
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE url = :url)")
    fun observeIsFavorited(url: String): Flow<Boolean>

    /**
     * Get count of favorites
     */
    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int

    /**
     * Observe count of favorites
     */
    @Query("SELECT COUNT(*) FROM favorites")
    fun observeFavoriteCount(): Flow<Int>

    /**
     * Get count of favorites in folder
     */
    @Query("SELECT COUNT(*) FROM favorites WHERE folder = :folder")
    suspend fun getFavoriteCountInFolder(folder: String?): Int

    /**
     * Search favorites by URL, title, or tags
     */
    @Query("""
        SELECT * FROM favorites
        WHERE url LIKE '%' || :query || '%'
        OR title LIKE '%' || :query || '%'
        OR tags LIKE '%' || :query || '%'
        OR notes LIKE '%' || :query || '%'
        ORDER BY visit_count DESC, title ASC
    """)
    suspend fun searchFavorites(query: String): List<FavoriteEntity>

    /**
     * Get favorites with specific tag
     */
    @Query("SELECT * FROM favorites WHERE tags LIKE '%' || :tag || '%' ORDER BY title ASC")
    suspend fun getFavoritesWithTag(tag: String): List<FavoriteEntity>

    /**
     * Get recently visited favorites (within specified days)
     */
    @Query("SELECT * FROM favorites WHERE last_visited > :timestamp ORDER BY last_visited DESC")
    suspend fun getRecentlyVisitedFavorites(timestamp: Long): List<FavoriteEntity>

    /**
     * Get most visited favorites (limit to top N)
     */
    @Query("SELECT * FROM favorites WHERE visit_count > 0 ORDER BY visit_count DESC LIMIT :limit")
    suspend fun getMostVisitedFavorites(limit: Int): List<FavoriteEntity>

    /**
     * Get unvisited favorites
     */
    @Query("SELECT * FROM favorites WHERE visit_count = 0 OR last_visited IS NULL ORDER BY created_at DESC")
    suspend fun getUnvisitedFavorites(): List<FavoriteEntity>

    /**
     * Insert favorite
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    /**
     * Insert multiple favorites
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteEntity>)

    /**
     * Update favorite
     */
    @Update
    suspend fun updateFavorite(favorite: FavoriteEntity)

    /**
     * Delete favorite
     */
    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    /**
     * Delete favorite by ID
     */
    @Query("DELETE FROM favorites WHERE id = :favoriteId")
    suspend fun deleteFavoriteById(favoriteId: String)

    /**
     * Delete favorite by URL
     */
    @Query("DELETE FROM favorites WHERE url = :url")
    suspend fun deleteFavoriteByUrl(url: String)

    /**
     * Delete all favorites
     */
    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()

    /**
     * Delete favorites in folder
     */
    @Query("DELETE FROM favorites WHERE folder = :folder")
    suspend fun deleteFavoritesInFolder(folder: String?)

    /**
     * Update visit count and last visited
     */
    @Query("UPDATE favorites SET visit_count = visit_count + 1, last_visited = :timestamp WHERE id = :favoriteId")
    suspend fun recordVisit(favoriteId: String, timestamp: Long)

    /**
     * Move favorite to folder
     */
    @Query("UPDATE favorites SET folder = :newFolder WHERE id = :favoriteId")
    suspend fun moveToFolder(favoriteId: String, newFolder: String?)

    /**
     * Update title
     */
    @Query("UPDATE favorites SET title = :newTitle WHERE id = :favoriteId")
    suspend fun updateTitle(favoriteId: String, newTitle: String)
}
