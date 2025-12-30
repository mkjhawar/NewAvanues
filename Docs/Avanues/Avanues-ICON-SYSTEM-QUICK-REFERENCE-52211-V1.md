# Icon System Quick Reference

**AvaElements Icon & Resource Manager**
**Version:** 1.0.0 | **Agent:** Week 2 - Agent 2

---

## Quick Start

### 1. Basic Icon Usage

```kotlin
@Composable
fun MyComponent() {
    IconFromString(
        iconName = "check",
        size = 24.dp
    )
}
```

### 2. Common Icon Sizes

```kotlin
IconFromString("settings", size = IconSize.Small.dp)      // 18dp
IconFromString("settings", size = IconSize.Standard.dp)   // 24dp (default)
IconFromString("settings", size = IconSize.Large.dp)      // 36dp
IconFromString("settings", size = IconSize.ExtraLarge.dp) // 48dp
```

### 3. Icon Variants

```kotlin
// Filled (default)
IconFromString("star")

// Outlined
IconFromString("star_outlined")

// Rounded
IconFromString("star_rounded")

// Sharp
IconFromString("star_sharp")
```

### 4. Network Icons

```kotlin
IconFromString("https://example.com/icon.png")
```

### 5. With Tinting

```kotlin
IconFromString(
    iconName = "favorite",
    tint = Color.Red
)
```

---

## Most Common Icons (by Category)

### Actions (20 icons)
```
check, close, add, remove, delete, edit, save, cancel
done, clear, refresh, sync, share, upload, download
copy, paste, cut, undo, redo
```

### Navigation (19 icons)
```
arrow_back, arrow_forward, menu, home, more_vert
chevron_left, chevron_right, expand_more, expand_less
navigate_before, navigate_next, first_page, last_page
arrow_upward, arrow_downward, apps
```

### People (8 icons)
```
person, person_add, person_remove, people, group
account_circle, face, supervisor_account
```

### Settings (11 icons)
```
settings, settings_applications, settings_bluetooth
settings_brightness, settings_phone, tune, build
```

### Media (23 icons)
```
play_arrow, pause, stop, skip_next, skip_previous
volume_up, volume_down, volume_mute, volume_off
mic, mic_off, camera, camera_alt, image, photo
video_library, music_note, album
```

### Toggle (13 icons)
```
check_box, check_box_outline_blank
radio_button_checked, radio_button_unchecked
toggle_on, toggle_off, star, star_border
favorite, favorite_border, thumb_up, thumb_down
```

### Status (14 icons)
```
info, info_outline, warning, error, help
notifications, notifications_active, notifications_off
priority_high, verified, verified_user
```

### Security (9 icons)
```
lock, lock_open, security, visibility, visibility_off
vpn_key, fingerprint
```

---

## Performance Tips

### Preload Common Icons

```kotlin
@Composable
fun App() {
    // Preload at app startup
    PreloadIcons(
        iconNames = listOf(
            "home", "search", "settings",
            "person", "notifications"
        )
    )
}
```

### Check Cache Status

```kotlin
val iconManager = AndroidIconResourceManager.getInstance(context)
val stats = iconManager.getCacheStats()

Log.d("Cache", "Hit rate: ${stats.memoryHitRate * 100}%")
Log.d("Cache", "Total requests: ${stats.totalRequests}")
```

### Clear Cache (if needed)

```kotlin
// Clear memory only
iconManager.clearCache(memoryOnly = true)

// Clear memory + disk
iconManager.clearCache(memoryOnly = false)
```

---

## Advanced Usage

### Custom Icon Resources

```kotlin
// Material Icon with variant
val icon = IconResource.MaterialIcon(
    name = "settings",
    variant = IconResource.IconVariant.Outlined
)

// Network image with placeholder
val icon = IconResource.NetworkImage(
    url = "https://example.com/icon.png",
    placeholder = IconResource.MaterialIcon("hourglass_empty"),
    errorIcon = IconResource.MaterialIcon("error")
)

// Vector drawable
val icon = IconResource.VectorDrawable("ic_custom_icon")

// Raster image
val icon = IconResource.RasterImage("icon.png")

// Use the icon
IconFromResource(iconResource = icon)
```

### Manual Loading

```kotlin
class MyViewModel(context: Context) {
    private val iconManager = AndroidIconResourceManager.getInstance(context)

    suspend fun loadIcon(name: String): ImageVector? {
        val resource = IconResource.fromString(name)
        return iconManager.loadIcon(resource) as? ImageVector
    }
}
```

---

## Icon Categories Reference

### Complete Category List

| Category | Count | Examples |
|----------|-------|----------|
| Actions | 20 | check, close, add, delete, edit |
| Navigation | 19 | arrow_back, menu, home, more_vert |
| Content | 22 | send, mail, attach_file, cloud, folder |
| Communication | 12 | call, chat, email, message, phone |
| People | 8 | person, group, account_circle, face |
| Settings | 11 | settings, tune, build, developer_mode |
| Media | 23 | play_arrow, pause, mic, camera, image |
| Toggle | 13 | check_box, star, favorite, thumb_up |
| Status | 14 | info, warning, error, notifications |
| Security | 9 | lock, visibility, vpn_key, fingerprint |
| Device | 13 | smartphone, bluetooth, wifi, location |
| Time & Date | 12 | access_time, alarm, calendar, event |
| Shopping | 10 | shopping_cart, payment, receipt, store |
| Places | 10 | place, map, restaurant, hotel, flight |
| Image & Photo | 8 | photo_library, crop, rotate, palette |
| Search & Filter | 7 | search, zoom_in, zoom_out, filter |
| Social | 4 | public, language, school, work |
| Arrows | 9 | arrow_circle_down, trending_up |
| Layout | 10 | dashboard, view_list, grid_on |
| Editor | 11 | format_bold, format_italic, align |
| **Total** | **326** | |

---

## Testing Your Icons

### Visual Icon Gallery

```kotlin
@Composable
fun IconGalleryScreen() {
    LazyColumn {
        items(FlutterIconMapping.getAllMappedIcons()) { iconName ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconFromString(
                    iconName = iconName,
                    size = 24.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = iconName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```

### Run Performance Benchmarks

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Run benchmarks and print results
        IconPerformanceBenchmark.runAndPrint(this)
    }
}
```

---

## Common Patterns

### Icon Button

```kotlin
IconButton(onClick = { /* action */ }) {
    IconFromString("settings")
}
```

### Icon with Label

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    IconFromString("person", size = 18.dp)
    Spacer(modifier = Modifier.width(8.dp))
    Text("Profile")
}
```

### Conditional Icon

```kotlin
IconFromString(
    iconName = if (isExpanded) "expand_less" else "expand_more"
)
```

### Tinted Icon

```kotlin
IconFromString(
    iconName = "favorite",
    tint = if (isFavorite) Color.Red else Color.Gray
)
```

---

## Troubleshooting

### Icon Not Showing?

1. **Check icon name:** Verify it's in the mapping
   ```kotlin
   FlutterIconMapping.isMapped("Icons.your_icon")
   ```

2. **Check console:** Look for error messages

3. **Try fallback:** Use a known icon to test
   ```kotlin
   IconFromString("check") // Should always work
   ```

### Performance Issues?

1. **Preload icons:** Load common icons at startup
2. **Check cache stats:** Monitor hit rates
3. **Clear cache:** If memory usage is high

### Missing Icon Variant?

Some variants may fall back to filled:
- Two-tone → Filled
- Some outlined → Filled
- Some rounded → Filled
- Some sharp → Filled

---

## Files & Locations

```
/Universal/Libraries/AvaElements/

Core/src/commonMain/kotlin/.../resources/
  ├── IconResource.kt           # Icon models
  ├── IconResourceManager.kt    # Interface
  └── FlutterIconMapping.kt     # 326 icon mappings

Core/src/androidMain/kotlin/.../resources/
  ├── AndroidIconResourceManager.kt  # Android impl
  └── IconPerformanceBenchmark.kt    # Benchmarks

Renderers/Android/src/androidMain/.../renderer/android/
  └── IconRendering.kt          # Composables
```

---

## Migration Guide

### Old Code
```kotlin
Icon(imageVector = Icons.Default.Settings, contentDescription = null)
```

### New Code
```kotlin
IconFromString("settings", contentDescription = "Settings")
```

### Benefits
- Automatic Flutter → Material mapping
- Caching
- Network icon support
- Simpler API

---

## Performance Targets

| Operation | Target | Actual |
|-----------|--------|--------|
| Cold Cache Load | < 5ms | TBD |
| Warm Cache Load | < 1ms | TBD |
| Cache Hit Rate | > 90% | TBD |
| Batch Preload (50) | < 100ms | TBD |

Run benchmarks to get actual numbers for your device.

---

## Support

- **Documentation:** `/docs/ICON-RESOURCE-MANAGER-IMPLEMENTATION.md`
- **Tests:** `/Core/src/commonTest/.../resources/`
- **Source:** `/Core/src/.../resources/`

---

**Quick Reference Card** | AvaElements v2.0.0 | Flutter Parity
