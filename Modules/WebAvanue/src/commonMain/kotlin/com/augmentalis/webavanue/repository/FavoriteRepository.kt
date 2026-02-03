package com.augmentalis.webavanue.repository

import com.augmentalis.webavanue.Favorite
import com.augmentalis.webavanue.FavoriteFolder
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.toDbModel
import com.augmentalis.webavanue.toDomainModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository interface for favorite/bookmark operations.
 *
 * Handles all favorite-related data persistence including:
 * - Favorite CRUD operations
 * - Folder management
 * - Search functionality
 * - Reactive observation via Flow
 */
interface FavoriteRepository {
    /** Adds a new favorite */
    suspend fun addFavorite(favorite: Favorite): Result<Favorite>

    /** Gets a favorite by ID */
    suspend fun getFavorite(favoriteId: String): Result<Favorite?>

    /** Gets all favorites */
    suspend fun getAllFavorites(): Result<List<Favorite>>

    /** Gets favorites in a specific folder */
    suspend fun getFavoritesInFolder(folderId: String?): Result<List<Favorite>>

    /** Observes all favorites with real-time updates */
    fun observeFavorites(): Flow<List<Favorite>>

    /** Updates a favorite */
    suspend fun updateFavorite(favorite: Favorite): Result<Unit>

    /** Removes a favorite */
    suspend fun removeFavorite(favoriteId: String): Result<Unit>

    /** Removes multiple favorites */
    suspend fun removeFavorites(favoriteIds: List<String>): Result<Unit>

    /** Checks if a URL is favorited */
    suspend fun isFavorite(url: String): Result<Boolean>

    /** Searches favorites */
    suspend fun searchFavorites(query: String): Result<List<Favorite>>

    /** Creates a favorite folder */
    suspend fun createFolder(folder: FavoriteFolder): Result<FavoriteFolder>

    /** Gets all favorite folders */
    suspend fun getAllFolders(): Result<List<FavoriteFolder>>

    /** Deletes a folder and optionally its contents */
    suspend fun deleteFolder(folderId: String, deleteContents: Boolean): Result<Unit>

    /** Refreshes in-memory state from database */
    suspend fun refresh()

    /** Updates in-memory state directly (for fast startup) */
    suspend fun updateState(favorites: List<Favorite>)
}

/**
 * SQLDelight implementation of FavoriteRepository.
 *
 * @param database SQLDelight database instance
 */
class FavoriteRepositoryImpl(
    private val database: BrowserDatabase
) : FavoriteRepository {

    private val queries = database.browserDatabaseQueries
    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())

    override suspend fun addFavorite(favorite: Favorite): Result<Favorite> = withContext(Dispatchers.IO) {
        try {
            queries.insertFavorite(favorite.toDbModel())
            refresh()
            Result.success(favorite)
        } catch (e: Exception) {
            Napier.e("Error adding favorite: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getFavorite(favoriteId: String): Result<Favorite?> = withContext(Dispatchers.IO) {
        try {
            val dbFavorite = queries.selectFavoriteById(favoriteId).executeAsOneOrNull()
            Result.success(dbFavorite?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Error getting favorite: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getAllFavorites(): Result<List<Favorite>> = withContext(Dispatchers.IO) {
        try {
            val favorites = queries.selectAllFavorites().executeAsList().map { it.toDomainModel() }
            Result.success(favorites)
        } catch (e: Exception) {
            Napier.e("Error getting all favorites: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getFavoritesInFolder(folderId: String?): Result<List<Favorite>> = withContext(Dispatchers.IO) {
        try {
            val favorites = queries.selectFavoritesInFolder(folderId).executeAsList().map { it.toDomainModel() }
            Result.success(favorites)
        } catch (e: Exception) {
            Napier.e("Error getting favorites in folder: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override fun observeFavorites(): Flow<List<Favorite>> = _favorites.asStateFlow()

    override suspend fun updateFavorite(favorite: Favorite): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateFavorite(
                title = favorite.title,
                favicon = favorite.favicon,
                folder_id = favorite.folderId,
                description = favorite.description,
                last_modified_at = favorite.lastModifiedAt.toEpochMilliseconds(),
                visit_count = favorite.visitCount.toLong(),
                position = favorite.position.toLong(),
                id = favorite.id
            )
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error updating favorite: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(favoriteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteFavorite(favoriteId)
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error removing favorite: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun removeFavorites(favoriteIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionWithResult {
                favoriteIds.forEach { queries.deleteFavorite(it) }
            }
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error removing favorites: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(url: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val favorite = queries.selectFavoriteByUrl(url).executeAsOneOrNull()
            Result.success(favorite != null)
        } catch (e: Exception) {
            Napier.e("Error checking favorite: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun searchFavorites(query: String): Result<List<Favorite>> = withContext(Dispatchers.IO) {
        try {
            val favorites = queries.searchFavorites(query, query, query).executeAsList().map { it.toDomainModel() }
            Result.success(favorites)
        } catch (e: Exception) {
            Napier.e("Error searching favorites: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun createFolder(folder: FavoriteFolder): Result<FavoriteFolder> = withContext(Dispatchers.IO) {
        try {
            queries.insertFavoriteFolder(folder.toDbModel())
            Napier.d("Created folder: ${folder.name} (${folder.id})", tag = TAG)
            Result.success(folder)
        } catch (e: Exception) {
            Napier.e("Error creating folder: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getAllFolders(): Result<List<FavoriteFolder>> = withContext(Dispatchers.IO) {
        try {
            val folders = queries.selectAllFolders().executeAsList().map { it.toDomainModel() }
            Napier.d("Loaded ${folders.size} folders", tag = TAG)
            Result.success(folders)
        } catch (e: Exception) {
            Napier.e("Error getting folders: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun deleteFolder(folderId: String, deleteContents: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                if (deleteContents) {
                    queries.deleteFavoritesInFolder(folderId)
                    Napier.d("Deleted favorites in folder: $folderId", tag = TAG)
                } else {
                    queries.clearFolderFromFavorites(folderId)
                    Napier.d("Cleared folder from favorites: $folderId", tag = TAG)
                }
                queries.deleteFolderById(folderId)
            }
            Napier.d("Deleted folder: $folderId", tag = TAG)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error deleting folder: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun refresh() {
        try {
            _favorites.value = queries.selectAllFavorites().executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            Napier.e("Error refreshing favorites: ${e.message}", e, tag = TAG)
        }
    }

    override suspend fun updateState(favorites: List<Favorite>) {
        _favorites.value = favorites
    }

    companion object {
        private const val TAG = "FavoriteRepository"
    }
}
