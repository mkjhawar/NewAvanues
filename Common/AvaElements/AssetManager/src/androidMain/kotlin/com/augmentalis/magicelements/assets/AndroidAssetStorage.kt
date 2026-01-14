package com.augmentalis.avaelements.assets

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.augmentalis.avaelements.assets.models.*
import com.augmentalis.avaelements.assets.utils.AssetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android SQLite-based asset storage
 *
 * Provides persistent storage for icons and images with
 * efficient search and LRU caching.
 */
class AndroidAssetStorage(context: Context) : AssetStorage {
    private val dbHelper = AssetDatabaseHelper(context)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun storeIcon(icon: Icon) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", icon.id)
            put("name", icon.name)
            put("svg", icon.svg)
            put("png_data", icon.png?.let { json.encodeToString(it) })
            put("tags", icon.tags.joinToString(","))
            put("library", icon.library)
            put("category", icon.category)
            put("aliases", icon.aliases.joinToString(","))
        }
        db.insertWithOnConflict("icons", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override suspend fun storeIcons(icons: List<Icon>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            icons.forEach { icon ->
                val values = ContentValues().apply {
                    put("id", icon.id)
                    put("name", icon.name)
                    put("svg", icon.svg)
                    put("png_data", icon.png?.let { json.encodeToString(it) })
                    put("tags", icon.tags.joinToString(","))
                    put("library", icon.library)
                    put("category", icon.category)
                    put("aliases", icon.aliases.joinToString(","))
                }
                db.insertWithOnConflict("icons", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun getIcon(iconId: String): Icon? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "icons",
            null,
            "id = ?",
            arrayOf(iconId),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                Icon(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    svg = it.getString(it.getColumnIndexOrThrow("svg")),
                    png = it.getString(it.getColumnIndexOrThrow("png_data"))?.let { data ->
                        json.decodeFromString(data)
                    },
                    tags = it.getString(it.getColumnIndexOrThrow("tags"))
                        ?.split(",")?.filter { tag -> tag.isNotBlank() } ?: emptyList(),
                    library = it.getString(it.getColumnIndexOrThrow("library")),
                    category = it.getString(it.getColumnIndexOrThrow("category")),
                    aliases = it.getString(it.getColumnIndexOrThrow("aliases"))
                        ?.split(",")?.filter { alias -> alias.isNotBlank() } ?: emptyList()
                )
            } else {
                null
            }
        }
    }

    override suspend fun storeImage(image: ImageAsset) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", image.id)
            put("name", image.name)
            put("path", image.path)
            put("format", image.format.name)
            put("width", image.dimensions.width)
            put("height", image.dimensions.height)
            put("thumbnail", image.thumbnail)
            put("file_size", image.fileSize)
            put("tags", image.tags.joinToString(","))
            put("alt_text", image.altText)
            put("attribution", image.attribution)
        }
        db.insertWithOnConflict("images", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override suspend fun getImage(imageId: String): ImageAsset? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "images",
            null,
            "id = ?",
            arrayOf(imageId),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                ImageAsset(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    path = it.getString(it.getColumnIndexOrThrow("path")),
                    format = ImageFormat.valueOf(it.getString(it.getColumnIndexOrThrow("format"))),
                    dimensions = Dimensions(
                        width = it.getInt(it.getColumnIndexOrThrow("width")),
                        height = it.getInt(it.getColumnIndexOrThrow("height"))
                    ),
                    thumbnail = it.getBlob(it.getColumnIndexOrThrow("thumbnail")),
                    fileSize = it.getLong(it.getColumnIndexOrThrow("file_size")),
                    tags = it.getString(it.getColumnIndexOrThrow("tags"))
                        ?.split(",")?.filter { tag -> tag.isNotBlank() } ?: emptyList(),
                    altText = it.getString(it.getColumnIndexOrThrow("alt_text")),
                    attribution = it.getString(it.getColumnIndexOrThrow("attribution"))
                )
            } else {
                null
            }
        }
    }

    override suspend fun searchIcons(query: AssetQuery): AssetSearchResult<Icon> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val selection = buildIconSearchSelection(query)
        val selectionArgs = buildIconSearchArgs(query)

        val cursor = db.query(
            "icons",
            null,
            selection.ifEmpty { null },
            selectionArgs.ifEmpty { null },
            null,
            null,
            null,
            "${query.offset},${query.limit}"
        )

        val results = mutableListOf<Icon>()
        cursor.use {
            while (it.moveToNext()) {
                val icon = Icon(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    svg = it.getString(it.getColumnIndexOrThrow("svg")),
                    png = it.getString(it.getColumnIndexOrThrow("png_data"))?.let { data ->
                        json.decodeFromString(data)
                    },
                    tags = it.getString(it.getColumnIndexOrThrow("tags"))
                        ?.split(",")?.filter { tag -> tag.isNotBlank() } ?: emptyList(),
                    library = it.getString(it.getColumnIndexOrThrow("library")),
                    category = it.getString(it.getColumnIndexOrThrow("category")),
                    aliases = it.getString(it.getColumnIndexOrThrow("aliases"))
                        ?.split(",")?.filter { alias -> alias.isNotBlank() } ?: emptyList()
                )

                // Calculate relevance if query text exists
                if (query.query != null) {
                    val score = AssetUtils.calculateRelevanceScore(
                        query.query,
                        icon.name,
                        icon.tags,
                        icon.aliases
                    )
                    if (score > 0.2f) { // Threshold for relevance
                        results.add(icon)
                    }
                } else {
                    results.add(icon)
                }
            }
        }

        // Sort by relevance if query exists
        val sortedResults = if (query.query != null) {
            results.sortedByDescending { icon ->
                AssetUtils.calculateRelevanceScore(
                    query.query,
                    icon.name,
                    icon.tags,
                    icon.aliases
                )
            }
        } else {
            results
        }

        AssetSearchResult(
            results = sortedResults.take(query.limit),
            totalCount = sortedResults.size,
            query = query
        )
    }

    override suspend fun searchImages(query: AssetQuery): AssetSearchResult<ImageAsset> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val selection = buildImageSearchSelection(query)
        val selectionArgs = buildImageSearchArgs(query)

        val cursor = db.query(
            "images",
            null,
            selection.ifEmpty { null },
            selectionArgs.ifEmpty { null },
            null,
            null,
            null,
            "${query.offset},${query.limit}"
        )

        val results = mutableListOf<ImageAsset>()
        cursor.use {
            while (it.moveToNext()) {
                results.add(
                    ImageAsset(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        path = it.getString(it.getColumnIndexOrThrow("path")),
                        format = ImageFormat.valueOf(it.getString(it.getColumnIndexOrThrow("format"))),
                        dimensions = Dimensions(
                            width = it.getInt(it.getColumnIndexOrThrow("width")),
                            height = it.getInt(it.getColumnIndexOrThrow("height"))
                        ),
                        thumbnail = it.getBlob(it.getColumnIndexOrThrow("thumbnail")),
                        fileSize = it.getLong(it.getColumnIndexOrThrow("file_size")),
                        tags = it.getString(it.getColumnIndexOrThrow("tags"))
                            ?.split(",")?.filter { tag -> tag.isNotBlank() } ?: emptyList(),
                        altText = it.getString(it.getColumnIndexOrThrow("alt_text")),
                        attribution = it.getString(it.getColumnIndexOrThrow("attribution"))
                    )
                )
            }
        }

        AssetSearchResult(
            results = results,
            totalCount = results.size,
            query = query
        )
    }

    override suspend fun getLibraries(): List<AssetLibrary> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("libraries", null, null, null, null, null, null)

        val libraries = mutableListOf<AssetLibrary>()
        cursor.use {
            while (it.moveToNext()) {
                libraries.add(
                    AssetLibrary(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        version = it.getString(it.getColumnIndexOrThrow("version")),
                        assetCount = it.getInt(it.getColumnIndexOrThrow("asset_count")),
                        categories = it.getString(it.getColumnIndexOrThrow("categories"))
                            ?.split(",")?.filter { cat -> cat.isNotBlank() } ?: emptyList(),
                        license = it.getString(it.getColumnIndexOrThrow("license")),
                        attribution = it.getString(it.getColumnIndexOrThrow("attribution")),
                        url = it.getString(it.getColumnIndexOrThrow("url")),
                        description = it.getString(it.getColumnIndexOrThrow("description"))
                    )
                )
            }
        }
        libraries
    }

    override suspend fun registerLibrary(library: AssetLibrary) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", library.id)
            put("name", library.name)
            put("version", library.version)
            put("asset_count", library.assetCount)
            put("categories", library.categories.joinToString(","))
            put("license", library.license)
            put("attribution", library.attribution)
            put("url", library.url)
            put("description", library.description)
        }
        db.insertWithOnConflict("libraries", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override suspend fun getLibraryIcons(
        libraryId: String,
        limit: Int,
        offset: Int
    ): List<Icon> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "icons",
            null,
            "library = ?",
            arrayOf(libraryId),
            null,
            null,
            "name ASC",
            "$offset,$limit"
        )

        val icons = mutableListOf<Icon>()
        cursor.use {
            while (it.moveToNext()) {
                icons.add(
                    Icon(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        svg = it.getString(it.getColumnIndexOrThrow("svg")),
                        png = it.getString(it.getColumnIndexOrThrow("png_data"))?.let { data ->
                            json.decodeFromString(data)
                        },
                        tags = it.getString(it.getColumnIndexOrThrow("tags"))
                            ?.split(",")?.filter { tag -> tag.isNotBlank() } ?: emptyList(),
                        library = it.getString(it.getColumnIndexOrThrow("library")),
                        category = it.getString(it.getColumnIndexOrThrow("category")),
                        aliases = it.getString(it.getColumnIndexOrThrow("aliases"))
                            ?.split(",")?.filter { alias -> alias.isNotBlank() } ?: emptyList()
                    )
                )
            }
        }
        icons
    }

    override suspend fun deleteIcon(iconId: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val deleted = db.delete("icons", "id = ?", arrayOf(iconId))
        deleted > 0
    }

    override suspend fun deleteImage(imageId: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val deleted = db.delete("images", "id = ?", arrayOf(imageId))
        deleted > 0
    }

    override suspend fun clearLibrary(libraryId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("icons", "library = ?", arrayOf(libraryId))
    }

    override suspend fun getStats(): StorageStats = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase

        val iconCount = db.rawQuery("SELECT COUNT(*) FROM icons", null).use {
            it.moveToFirst()
            it.getInt(0)
        }

        val imageCount = db.rawQuery("SELECT COUNT(*) FROM images", null).use {
            it.moveToFirst()
            it.getInt(0)
        }

        val libraryCount = db.rawQuery("SELECT COUNT(*) FROM libraries", null).use {
            it.moveToFirst()
            it.getInt(0)
        }

        val totalSize = db.rawQuery("SELECT SUM(file_size) FROM images", null).use {
            it.moveToFirst()
            it.getLong(0)
        }

        StorageStats(
            totalIcons = iconCount,
            totalImages = imageCount,
            libraryCount = libraryCount,
            totalSizeBytes = totalSize
        )
    }

    private fun buildIconSearchSelection(query: AssetQuery): String {
        val conditions = mutableListOf<String>()

        if (query.library != null) {
            conditions.add("library = ?")
        }

        if (query.category != null) {
            conditions.add("category = ?")
        }

        if (query.query != null) {
            conditions.add("(name LIKE ? OR tags LIKE ? OR aliases LIKE ?)")
        }

        return conditions.joinToString(" AND ")
    }

    private fun buildIconSearchArgs(query: AssetQuery): Array<String> {
        val args = mutableListOf<String>()

        if (query.library != null) {
            args.add(query.library)
        }

        if (query.category != null) {
            args.add(query.category)
        }

        if (query.query != null) {
            val pattern = "%${query.query}%"
            args.add(pattern)
            args.add(pattern)
            args.add(pattern)
        }

        return args.toTypedArray()
    }

    private fun buildImageSearchSelection(query: AssetQuery): String {
        if (query.query != null) {
            return "name LIKE ? OR tags LIKE ?"
        }
        return ""
    }

    private fun buildImageSearchArgs(query: AssetQuery): Array<String> {
        if (query.query != null) {
            val pattern = "%${query.query}%"
            return arrayOf(pattern, pattern)
        }
        return emptyArray()
    }
}

/**
 * SQLite database helper for asset storage
 */
private class AssetDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        // Create icons table
        db.execSQL("""
            CREATE TABLE icons (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                svg TEXT,
                png_data TEXT,
                tags TEXT,
                library TEXT,
                category TEXT,
                aliases TEXT
            )
        """)

        // Create images table
        db.execSQL("""
            CREATE TABLE images (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                path TEXT NOT NULL,
                format TEXT NOT NULL,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL,
                thumbnail BLOB,
                file_size INTEGER NOT NULL,
                tags TEXT,
                alt_text TEXT,
                attribution TEXT
            )
        """)

        // Create libraries table
        db.execSQL("""
            CREATE TABLE libraries (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                version TEXT NOT NULL,
                asset_count INTEGER NOT NULL,
                categories TEXT,
                license TEXT,
                attribution TEXT,
                url TEXT,
                description TEXT
            )
        """)

        // Create indices for faster searches
        db.execSQL("CREATE INDEX idx_icons_library ON icons(library)")
        db.execSQL("CREATE INDEX idx_icons_category ON icons(category)")
        db.execSQL("CREATE INDEX idx_icons_name ON icons(name)")
        db.execSQL("CREATE INDEX idx_images_name ON images(name)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // For now, just recreate tables
        db.execSQL("DROP TABLE IF EXISTS icons")
        db.execSQL("DROP TABLE IF EXISTS images")
        db.execSQL("DROP TABLE IF EXISTS libraries")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "avaelements_assets.db"
        private const val DATABASE_VERSION = 1
    }
}
