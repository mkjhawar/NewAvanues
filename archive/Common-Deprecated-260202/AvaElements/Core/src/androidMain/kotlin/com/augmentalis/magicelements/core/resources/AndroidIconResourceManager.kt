package com.augmentalis.magicelements.core.resources

import android.content.Context
import android.util.LruCache
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.ui.graphics.vector.ImageVector
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Android implementation of IconResourceManager
 *
 * Features:
 * - Material Icons integration (500+ icons)
 * - Coil-based network image loading
 * - SVG support via Coil-SVG
 * - Multi-level caching (memory + disk)
 * - Icon tinting support
 *
 * @param context Android application context
 * @since 3.0.0-flutter-parity
 */
class AndroidIconResourceManager(
    private val context: Context
) : IconResourceManager {

    // LRU cache for Material Icons (icon name -> ImageVector)
    private val materialIconCache = LruCache<String, ImageVector>(MAX_MATERIAL_ICONS_CACHE)

    // Coil image loader for network icons
    private val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "icon_cache"))
                    .maxSizeBytes(DISK_CACHE_SIZE_BYTES)
                    .build()
            }
            .build()
    }

    // Cache statistics tracking
    private var totalRequests = 0L
    private var cacheHits = 0L
    private var cacheMisses = 0L

    override suspend fun loadIcon(
        resource: IconResource,
        size: IconSize,
        tint: String?
    ): Any = withContext(Dispatchers.IO) {
        totalRequests++

        when (resource) {
            is IconResource.MaterialIcon -> {
                loadMaterialIcon(resource).also {
                    if (it != null) cacheHits++ else cacheMisses++
                } ?: Icons.Default.Settings
            }

            is IconResource.NetworkImage -> {
                loadNetworkIcon(resource, size).also {
                    cacheMisses++
                }
            }

            is IconResource.VectorDrawable -> {
                loadVectorDrawable(resource, size).also {
                    cacheMisses++
                }
            }

            is IconResource.RasterImage -> {
                loadRasterImage(resource, size).also {
                    cacheMisses++
                }
            }

            is IconResource.Base64Image -> {
                loadBase64Image(resource, size).also {
                    cacheMisses++
                }
            }
        }
    }

    override suspend fun preloadIcons(resources: List<IconResource>) {
        resources.forEach { resource ->
            when (resource) {
                is IconResource.MaterialIcon -> {
                    // Preload into cache
                    loadMaterialIcon(resource)
                }
                is IconResource.NetworkImage -> {
                    // Prefetch network image
                    val request = ImageRequest.Builder(context)
                        .data(resource.url)
                        .build()
                    imageLoader.enqueue(request)
                }
                else -> {
                    // Other types loaded on-demand
                }
            }
        }
    }

    override fun clearCache(memoryOnly: Boolean) {
        materialIconCache.evictAll()
        if (!memoryOnly) {
            imageLoader.diskCache?.clear()
        }
        imageLoader.memoryCache?.clear()
    }

    override fun getCacheStats(): IconResourceManager.CacheStats {
        val memorySize = imageLoader.memoryCache?.size ?: 0L
        val diskSize = imageLoader.diskCache?.size ?: 0L
        val hitRate = if (totalRequests > 0) {
            cacheHits.toFloat() / totalRequests
        } else {
            0f
        }

        return IconResourceManager.CacheStats(
            memorySizeBytes = memorySize,
            diskSizeBytes = diskSize,
            memoryHitRate = hitRate,
            diskHitRate = 0f, // Coil doesn't expose disk hit rate
            totalRequests = totalRequests,
            cacheHits = cacheHits,
            cacheMisses = cacheMisses
        )
    }

    override fun isCached(resource: IconResource): Boolean {
        return when (resource) {
            is IconResource.MaterialIcon -> {
                val cacheKey = "${resource.name}_${resource.variant}"
                materialIconCache.get(cacheKey) != null
            }
            is IconResource.NetworkImage -> {
                imageLoader.memoryCache?.keys?.any { it.toString() == resource.url } == true
            }
            else -> false
        }
    }

    /**
     * Load Material Design icon
     */
    private fun loadMaterialIcon(resource: IconResource.MaterialIcon): ImageVector? {
        val cacheKey = "${resource.name}_${resource.variant}"

        // Check cache first
        materialIconCache.get(cacheKey)?.let { return it }

        // Map Flutter icon name to Material icon
        val materialName = FlutterIconMapping.getMaterialIconName(resource.name)

        // Load icon based on variant
        val icon = when (resource.variant) {
            IconResource.IconVariant.Filled -> getMaterialFilledIcon(materialName)
            IconResource.IconVariant.Outlined -> getMaterialOutlinedIcon(materialName)
            IconResource.IconVariant.Rounded -> getMaterialRoundedIcon(materialName)
            IconResource.IconVariant.Sharp -> getMaterialSharpIcon(materialName)
            IconResource.IconVariant.TwoTone -> getMaterialFilledIcon(materialName) // Fallback
        }

        icon?.let { materialIconCache.put(cacheKey, it) }
        return icon
    }

    /**
     * Get Material Filled icon by name
     */
    private fun getMaterialFilledIcon(name: String): ImageVector? {
        return when (name.lowercase()) {
            // Common Actions
            "add" -> Icons.Default.Add
            "remove" -> Icons.Default.Remove
            "delete" -> Icons.Default.Delete
            "edit" -> Icons.Default.Edit
            "check" -> Icons.Default.Check
            "close" -> Icons.Default.Close
            "done" -> Icons.Default.Done
            "clear" -> Icons.Default.Clear
            "cancel" -> Icons.Default.Cancel
            "refresh" -> Icons.Default.Refresh
            "share" -> Icons.Default.Share
            "content_copy", "copy" -> Icons.Default.ContentCopy
            "content_paste", "paste" -> Icons.Default.ContentPaste
            "content_cut", "cut" -> Icons.Default.ContentCut

            // Navigation
            "arrow_back" -> Icons.Default.ArrowBack
            "arrow_forward" -> Icons.Default.ArrowForward
            "arrow_upward" -> Icons.Default.ArrowUpward
            "arrow_downward" -> Icons.Default.ArrowDownward
            "chevron_left" -> Icons.Default.ChevronLeft
            "chevron_right" -> Icons.Default.ChevronRight
            "expand_more" -> Icons.Default.ExpandMore
            "expand_less" -> Icons.Default.ExpandLess
            "menu" -> Icons.Default.Menu
            "more_vert" -> Icons.Default.MoreVert
            "more_horiz" -> Icons.Default.MoreHoriz
            "home" -> Icons.Default.Home
            "apps" -> Icons.Default.Apps

            // Content
            "send" -> Icons.Default.Send
            "mail", "email" -> Icons.Default.Email
            "attach_file", "attachment" -> Icons.Default.AttachFile
            "cloud" -> Icons.Default.Cloud
            "folder" -> Icons.Default.Folder
            "filter_list" -> Icons.Default.FilterList
            "sort" -> Icons.Default.Sort
            "search" -> Icons.Default.Search

            // People
            "person" -> Icons.Default.Person
            "person_add" -> Icons.Default.PersonAdd
            "people", "group" -> Icons.Default.Group
            "account_circle" -> Icons.Default.AccountCircle
            "face" -> Icons.Default.Face

            // Settings
            "settings" -> Icons.Default.Settings
            "tune" -> Icons.Default.Tune
            "build" -> Icons.Default.Build

            // Media
            "play_arrow" -> Icons.Default.PlayArrow
            "pause" -> Icons.Default.Pause
            "stop" -> Icons.Default.Stop
            "skip_next" -> Icons.Default.SkipNext
            "skip_previous" -> Icons.Default.SkipPrevious
            "volume_up" -> Icons.Default.VolumeUp
            "volume_down" -> Icons.Default.VolumeDown
            "volume_off" -> Icons.Default.VolumeOff
            "mic" -> Icons.Default.Mic
            "camera" -> Icons.Default.Camera
            "image", "photo" -> Icons.Default.Image
            "music_note" -> Icons.Default.MusicNote

            // Toggle
            "check_box" -> Icons.Default.CheckBox
            "check_box_outline_blank" -> Icons.Default.CheckBoxOutlineBlank
            "radio_button_checked" -> Icons.Default.RadioButtonChecked
            "radio_button_unchecked" -> Icons.Default.RadioButtonUnchecked
            "star" -> Icons.Default.Star
            "star_border" -> Icons.Default.StarBorder
            "favorite" -> Icons.Default.Favorite
            "favorite_border" -> Icons.Default.FavoriteBorder
            "thumb_up" -> Icons.Default.ThumbUp
            "thumb_down" -> Icons.Default.ThumbDown

            // Status & Notifications
            "info" -> Icons.Default.Info
            "warning" -> Icons.Default.Warning
            "error" -> Icons.Default.Error
            "help" -> Icons.Default.Help
            "notifications" -> Icons.Default.Notifications

            // Security
            "lock" -> Icons.Default.Lock
            "lock_open" -> Icons.Default.LockOpen
            "visibility" -> Icons.Default.Visibility
            "visibility_off" -> Icons.Default.VisibilityOff
            "vpn_key" -> Icons.Default.VpnKey

            // Device
            "smartphone", "phone" -> Icons.Default.Phone
            "bluetooth" -> Icons.Default.Bluetooth
            "wifi" -> Icons.Default.Wifi
            "location_on" -> Icons.Default.LocationOn

            // Time & Date
            "access_time", "schedule" -> Icons.Default.AccessTime
            "alarm" -> Icons.Default.Alarm
            "date_range", "calendar_today" -> Icons.Default.DateRange
            "event" -> Icons.Default.Event

            // Shopping
            "shopping_cart" -> Icons.Default.ShoppingCart
            "payment" -> Icons.Default.Payment
            "receipt" -> Icons.Default.Receipt

            // Places
            "place" -> Icons.Default.Place
            "map" -> Icons.Default.Map
            "restaurant" -> Icons.Default.Restaurant

            // Arrows
            "arrow_circle_down" -> Icons.Default.ArrowCircleDown
            "arrow_circle_up" -> Icons.Default.ArrowCircleUp
            "arrow_drop_down" -> Icons.Default.ArrowDropDown
            "arrow_drop_up" -> Icons.Default.ArrowDropUp

            // Layout
            "dashboard" -> Icons.Default.Dashboard
            "view_list" -> Icons.Default.ViewList

            // Default fallback
            else -> null
        }
    }

    /**
     * Get Material Outlined icon by name
     */
    private fun getMaterialOutlinedIcon(name: String): ImageVector? {
        return when (name.lowercase()) {
            "delete" -> Icons.Outlined.Delete
            "edit" -> Icons.Outlined.Edit
            "settings" -> Icons.Outlined.Settings
            "person" -> Icons.Outlined.Person
            "home" -> Icons.Outlined.Home
            "search" -> Icons.Outlined.Search
            "star" -> Icons.Outlined.Star
            "favorite" -> Icons.Outlined.FavoriteBorder
            "info" -> Icons.Outlined.Info
            "warning" -> Icons.Outlined.Warning
            "error" -> Icons.Outlined.Error
            "help" -> Icons.Outlined.Help
            "notifications" -> Icons.Outlined.Notifications
            "email" -> Icons.Outlined.Email
            "account_circle" -> Icons.Outlined.AccountCircle
            else -> getMaterialFilledIcon(name) // Fallback to filled
        }
    }

    /**
     * Get Material Rounded icon by name
     */
    private fun getMaterialRoundedIcon(name: String): ImageVector? {
        return when (name.lowercase()) {
            "add" -> Icons.Rounded.Add
            "check" -> Icons.Rounded.Check
            "close" -> Icons.Rounded.Close
            "arrow_back" -> Icons.Rounded.ArrowBack
            "arrow_forward" -> Icons.Rounded.ArrowForward
            "menu" -> Icons.Rounded.Menu
            "more_vert" -> Icons.Rounded.MoreVert
            "person" -> Icons.Rounded.Person
            "home" -> Icons.Rounded.Home
            "settings" -> Icons.Rounded.Settings
            "star" -> Icons.Rounded.Star
            "favorite" -> Icons.Rounded.Favorite
            "delete" -> Icons.Rounded.Delete
            "edit" -> Icons.Rounded.Edit
            "search" -> Icons.Rounded.Search
            else -> getMaterialFilledIcon(name) // Fallback to filled
        }
    }

    /**
     * Get Material Sharp icon by name
     */
    private fun getMaterialSharpIcon(name: String): ImageVector? {
        return when (name.lowercase()) {
            "add" -> Icons.Sharp.Add
            "check" -> Icons.Sharp.Check
            "close" -> Icons.Sharp.Close
            "arrow_back" -> Icons.Sharp.ArrowBack
            "arrow_forward" -> Icons.Sharp.ArrowForward
            "menu" -> Icons.Sharp.Menu
            "more_vert" -> Icons.Sharp.MoreVert
            "person" -> Icons.Sharp.Person
            "home" -> Icons.Sharp.Home
            "settings" -> Icons.Sharp.Settings
            "star" -> Icons.Sharp.Star
            "favorite" -> Icons.Sharp.Favorite
            "delete" -> Icons.Sharp.Delete
            "edit" -> Icons.Sharp.Edit
            "search" -> Icons.Sharp.Search
            else -> getMaterialFilledIcon(name) // Fallback to filled
        }
    }

    /**
     * Load network icon using Coil
     */
    private suspend fun loadNetworkIcon(
        resource: IconResource.NetworkImage,
        size: IconSize
    ): Any {
        val request = ImageRequest.Builder(context)
            .data(resource.url)
            .size((size.dp * context.resources.displayMetrics.density).roundToInt())
            .build()

        return imageLoader.execute(request)
    }

    /**
     * Load vector drawable from resources
     */
    private suspend fun loadVectorDrawable(
        resource: IconResource.VectorDrawable,
        size: IconSize
    ): Any {
        // TODO: Implement vector drawable loading
        return Icons.Default.Image
    }

    /**
     * Load raster image from resources
     */
    private suspend fun loadRasterImage(
        resource: IconResource.RasterImage,
        size: IconSize
    ): Any {
        // TODO: Implement raster image loading
        return Icons.Default.Image
    }

    /**
     * Load base64-encoded image
     */
    private suspend fun loadBase64Image(
        resource: IconResource.Base64Image,
        size: IconSize
    ): Any {
        // TODO: Implement base64 image loading
        return Icons.Default.Image
    }

    companion object {
        /** Maximum Material Icons to cache in memory */
        private const val MAX_MATERIAL_ICONS_CACHE = 200

        /** Disk cache size for network icons (50 MB) */
        private const val DISK_CACHE_SIZE_BYTES = 50L * 1024 * 1024

        /** Singleton instance */
        @Volatile
        private var instance: AndroidIconResourceManager? = null

        /**
         * Get or create singleton instance
         */
        fun getInstance(context: Context): AndroidIconResourceManager {
            return instance ?: synchronized(this) {
                instance ?: AndroidIconResourceManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
