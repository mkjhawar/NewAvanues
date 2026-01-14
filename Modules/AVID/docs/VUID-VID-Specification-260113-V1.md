# VUID/VID System Specification

**Version:** 1.0
**Date:** 2026-01-13
**Status:** DRAFT - Awaiting Approval
**Author:** Claude (AI Assistant)

---

## 1. Overview

This specification defines a dual-layer identifier system for tracking UI elements across the VoiceOS platform:

| Layer | Name | Scope | Purpose |
|-------|------|-------|---------|
| Universal | VID (VoiceOS ID) | Cross-device | Permanent app identifier assigned by cloud |
| Local | VUID (Voice Unique ID) | Per-device | Runtime element tracking with integer IDs |

### Design Principles

1. **Offline-First** - Full functionality without cloud connectivity
2. **Performance** - Integer IDs at runtime (8 bytes per element)
3. **Human Readability** - Decoded on demand, not stored in IDs
4. **Cross-Device Consistency** - VIDs provide universal references
5. **Collision Prevention** - Centralized VID assignment

---

## 2. VID (VoiceOS ID) Format

### 2.1 Format Structure

```
VID-{platform}-{sequence}
```

| Component | Size | Description |
|-----------|------|-------------|
| `VID-` | 4 chars | Fixed prefix for identification |
| `platform` | 1 char | Platform code (A/I/W/D) |
| `-` | 1 char | Separator |
| `sequence` | 6 chars | Zero-padded sequential number |

**Total Length:** 12 characters

### 2.2 Platform Codes

| Code | Platform | Example |
|------|----------|---------|
| A | Android | VID-A-000001 |
| I | iOS | VID-I-000001 |
| W | Web | VID-W-000001 |
| D | Desktop | VID-D-000001 |

### 2.3 Capacity

- Per platform: 999,999 unique apps
- Total capacity: ~4 million apps
- Version tracking: Separate table (unlimited versions per VID)

### 2.4 Reserved Ranges

| Range | Purpose |
|-------|---------|
| VID-X-000001 to VID-X-099999 | Public apps (store-distributed) |
| VID-X-100000 to VID-X-199999 | Enterprise apps |
| VID-X-200000 to VID-X-899999 | General allocation |
| VID-X-900000 to VID-X-999999 | Development/Testing |

---

## 3. Local VUID System

### 3.1 Runtime Representation

```kotlin
data class TrackedElement(
    val appId: Int,    // 4 bytes - local sequential ID
    val elemId: Int    // 4 bytes - sequential per app
)
```

**Total Runtime Size:** 8 bytes per element

### 3.2 Type Codes

| Code | Type | UI Pattern |
|------|------|------------|
| btn | Button | Clickable actions |
| inp | Input | Text fields, search boxes |
| txt | Text | Labels, headings |
| img | Image | Pictures, icons |
| lst | List | RecyclerView, LazyColumn |
| itm | Item | List items |
| scr | Screen | Activities, Composables |
| nav | Navigation | Tabs, drawers |
| swt | Switch | Toggles, checkboxes |
| slr | Slider | Range selectors |
| sel | Select | Dropdowns, pickers |
| dia | Dialog | Modals, alerts |
| mnu | Menu | Context menus, popups |
| crd | Card | Card containers |
| tab | Tab | Tab items |

### 3.3 Human-Readable Format (Display Only)

```
{VID}:{version}:{type}:{resource_id}
VID-A-000001:350:btn:like_button
```

This format is generated on-demand via database views, not stored.

---

## 4. Database Schema

### 4.1 Cloud Registry (Master)

```sql
-- Master app registry
CREATE TABLE vid_registry (
    vid TEXT PRIMARY KEY,           -- VID-A-000001
    platform TEXT NOT NULL,         -- and, ios, web, dsk
    package_name TEXT NOT NULL,     -- com.instagram.android
    app_name TEXT,                  -- Instagram
    fingerprint TEXT NOT NULL,      -- Deterministic hash for dedup
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(platform, package_name),
    UNIQUE(fingerprint)
);

-- Version tracking
CREATE TABLE vid_versions (
    vid TEXT NOT NULL,
    version TEXT NOT NULL,          -- 350.0.0 (normalized)
    element_count INTEGER DEFAULT 0,
    first_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (vid, version),
    FOREIGN KEY (vid) REFERENCES vid_registry(vid)
);

-- Element library (shared)
CREATE TABLE vid_elements (
    vid TEXT NOT NULL,
    version TEXT NOT NULL,
    elem_hash TEXT NOT NULL,        -- Deterministic element hash
    type TEXT NOT NULL,
    resource_id TEXT,
    name TEXT,
    content_desc TEXT,
    bounds_signature TEXT,          -- Relative position signature
    PRIMARY KEY (vid, version, elem_hash),
    FOREIGN KEY (vid, version) REFERENCES vid_versions(vid, version)
);
```

### 4.2 Device Local Schema

```sql
-- Local app registry
CREATE TABLE local_apps (
    local_id INTEGER PRIMARY KEY AUTOINCREMENT,
    vid TEXT,                       -- NULL if not synced to cloud
    platform TEXT NOT NULL,
    package_name TEXT NOT NULL,
    version TEXT NOT NULL,
    app_name TEXT,
    fingerprint TEXT NOT NULL,
    last_scanned TIMESTAMP,
    UNIQUE(platform, package_name, version)
);

-- Local element storage
CREATE TABLE local_elements (
    local_app_id INTEGER NOT NULL,
    elem_id INTEGER NOT NULL,       -- Sequential per app
    elem_hash TEXT NOT NULL,        -- For matching with cloud library
    type TEXT NOT NULL,
    resource_id TEXT,
    name TEXT,
    content_desc TEXT,
    bounds_signature TEXT,
    PRIMARY KEY (local_app_id, elem_id),
    FOREIGN KEY (local_app_id) REFERENCES local_apps(local_id)
);

-- VID sync mapping
CREATE TABLE vid_sync (
    vid TEXT NOT NULL,
    version TEXT NOT NULL,
    local_app_id INTEGER NOT NULL,
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (vid, version),
    FOREIGN KEY (local_app_id) REFERENCES local_apps(local_id)
);

-- Sequence tracking
CREATE TABLE elem_sequence (
    local_app_id INTEGER PRIMARY KEY,
    next_elem_id INTEGER DEFAULT 1,
    FOREIGN KEY (local_app_id) REFERENCES local_apps(local_id)
);
```

### 4.3 Views (Human-Readable)

```sql
-- Full readable VUID for display
CREATE VIEW v_readable_elements AS
SELECT
    la.local_id AS app_id,
    le.elem_id,
    COALESCE(la.vid, 'LOCAL-' || la.local_id) || ':' ||
        la.version || ':' || le.type || ':' ||
        COALESCE(le.resource_id, le.name, le.elem_hash) AS readable_vuid,
    la.app_name,
    la.package_name,
    la.version,
    le.type,
    le.resource_id,
    le.name,
    le.content_desc
FROM local_elements le
JOIN local_apps la ON le.local_app_id = la.local_id;

-- App summary view
CREATE VIEW v_app_summary AS
SELECT
    la.local_id,
    COALESCE(la.vid, 'LOCAL-' || la.local_id) AS display_id,
    la.platform,
    la.app_name,
    la.package_name,
    la.version,
    COUNT(le.elem_id) AS element_count,
    CASE WHEN la.vid IS NOT NULL THEN 'synced' ELSE 'local' END AS sync_status
FROM local_apps la
LEFT JOIN local_elements le ON la.local_id = le.local_app_id
GROUP BY la.local_id;
```

---

## 5. Hash Functions

### 5.1 App Fingerprint

Used for deduplication when registering apps.

```kotlin
object AppFingerprint {
    /**
     * Generates deterministic fingerprint for an app.
     * Same input always produces same output across all devices.
     */
    fun generate(platform: String, packageName: String): String {
        val input = "$platform:$packageName".lowercase()
        return hashToHex(input, 12)
    }

    private fun hashToHex(input: String, length: Int): String {
        var hash1 = 0L
        var hash2 = 0L
        input.forEachIndexed { index, char ->
            val charValue = char.code.toLong()
            hash1 = (hash1 * 31 + charValue) and 0xFFFFFFFFL
            hash2 = (hash2 * 37 + charValue * (index + 1)) and 0xFFFFFFFFL
        }
        val combined = ((hash1 shl 16) xor hash2) and 0xFFFFFFFFFFFFL
        return combined.toString(16).padStart(12, '0').takeLast(length).lowercase()
    }
}
```

### 5.2 Element Hash

Used for matching elements between devices and versions.

```kotlin
object ElementHash {
    /**
     * Generates deterministic hash for UI element.
     * Based on stable properties that don't change across devices.
     */
    fun generate(
        resourceId: String?,
        className: String,
        contentDesc: String?,
        boundsSignature: String?
    ): String {
        val input = listOfNotNull(
            resourceId,
            className,
            contentDesc,
            boundsSignature
        ).joinToString("|")

        return hashToHex(input, 8)
    }

    /**
     * Bounds signature is relative position within screen.
     * Format: "{left%},{top%},{right%},{bottom%}"
     * Example: "0.10,0.80,0.15,0.85"
     */
    fun generateBoundsSignature(
        bounds: Rect,
        screenWidth: Int,
        screenHeight: Int
    ): String {
        val left = (bounds.left.toFloat() / screenWidth).roundTo2()
        val top = (bounds.top.toFloat() / screenHeight).roundTo2()
        val right = (bounds.right.toFloat() / screenWidth).roundTo2()
        val bottom = (bounds.bottom.toFloat() / screenHeight).roundTo2()
        return "$left,$top,$right,$bottom"
    }
}
```

---

## 6. Version Normalization

```kotlin
object VersionNormalizer {
    /**
     * Normalizes version strings for consistent comparison.
     *
     * Examples:
     * - "350.0.0.1234" → "350.0.0"
     * - "2.24.1.123-beta" → "2.24.1"
     * - "1.0" → "1.0.0"
     * - "v2.5.1" → "2.5.0"
     */
    fun normalize(version: String): String {
        val cleaned = version
            .removePrefix("v")
            .removePrefix("V")
            .split(Regex("[^0-9.]"))[0]

        val parts = cleaned.split(".").take(3).map {
            it.toIntOrNull() ?: 0
        }

        return when (parts.size) {
            1 -> "${parts[0]}.0.0"
            2 -> "${parts[0]}.${parts[1]}.0"
            else -> "${parts[0]}.${parts[1]}.${parts[2]}"
        }
    }
}
```

---

## 7. Export/Import Protocol

### 7.1 Export Format (Device → Cloud)

```json
{
  "schema_version": "1.0",
  "export_type": "full",
  "device_id": "device-uuid-here",
  "exported_at": "2026-01-13T10:30:00Z",
  "apps": [
    {
      "vid": null,
      "fingerprint": "a3f2e1c9b7d4",
      "platform": "and",
      "package_name": "com.instagram.android",
      "app_name": "Instagram",
      "version": "350.0.0",
      "elements": [
        {
          "elem_hash": "7cc9dc1a",
          "type": "btn",
          "resource_id": "like_button",
          "name": "Like",
          "content_desc": "Like this post",
          "bounds_signature": "0.10,0.80,0.15,0.85"
        }
      ]
    }
  ]
}
```

### 7.2 Import Format (Cloud → Device)

```json
{
  "schema_version": "1.0",
  "import_type": "full",
  "processed_at": "2026-01-13T10:31:00Z",
  "apps": [
    {
      "vid": "VID-A-000001",
      "fingerprint": "a3f2e1c9b7d4",
      "platform": "and",
      "package_name": "com.instagram.android",
      "app_name": "Instagram",
      "version": "350.0.0",
      "is_new": false,
      "elements": [
        {
          "elem_hash": "7cc9dc1a",
          "type": "btn",
          "resource_id": "like_button",
          "name": "Like",
          "content_desc": "Like this post",
          "bounds_signature": "0.10,0.80,0.15,0.85"
        }
      ]
    }
  ]
}
```

### 7.3 Sync Protocol

```
DEVICE                              CLOUD
  │                                   │
  │── POST /sync/export ─────────────>│
  │   {apps: [...]}                   │
  │                                   │ 1. Check fingerprints
  │                                   │ 2. Assign VIDs to new apps
  │                                   │ 3. Merge elements
  │<── 200 OK ────────────────────────│
  │   {apps: [{vid: "VID-A-000001"}]} │
  │                                   │
  │ Update local vid_sync table       │
  │                                   │
  │── GET /sync/library?since=... ───>│
  │                                   │
  │<── 200 OK ────────────────────────│
  │   {apps: [...new elements...]}    │
  │                                   │
  │ Merge into local_elements         │
  └───────────────────────────────────┘
```

---

## 8. API Reference

### 8.1 VUIDGenerator (Local)

```kotlin
interface VUIDGenerator {
    /**
     * Registers a new app or retrieves existing local ID.
     * @return local app ID (integer)
     */
    suspend fun registerApp(
        platform: String,
        packageName: String,
        version: String,
        appName: String?
    ): Int

    /**
     * Registers a new element for tracking.
     * @return local element ID (integer)
     */
    suspend fun registerElement(
        appId: Int,
        type: String,
        resourceId: String?,
        name: String?,
        contentDesc: String?,
        boundsSignature: String?
    ): Int

    /**
     * Gets or creates element ID for given properties.
     * @return existing or new element ID
     */
    suspend fun getOrCreateElement(
        appId: Int,
        elemHash: String,
        type: String,
        resourceId: String?,
        name: String?,
        contentDesc: String?,
        boundsSignature: String?
    ): Int

    /**
     * Gets human-readable VUID for display.
     */
    suspend fun getReadableVuid(appId: Int, elemId: Int): String

    /**
     * Gets element by local IDs.
     */
    suspend fun getElement(appId: Int, elemId: Int): TrackedElement?
}
```

### 8.2 VIDSyncService (Cloud Integration)

```kotlin
interface VIDSyncService {
    /**
     * Exports local data for cloud processing.
     */
    suspend fun export(): ExportData

    /**
     * Imports VID assignments from cloud.
     */
    suspend fun importVidAssignments(data: ImportData)

    /**
     * Downloads element library updates.
     */
    suspend fun downloadLibraryUpdates(since: Instant?): List<AppData>

    /**
     * Gets sync status.
     */
    suspend fun getSyncStatus(): SyncStatus
}
```

---

## 9. Migration Path

### 9.1 From Existing Implementations

| Current | Target | Action |
|---------|--------|--------|
| VoiceOSCoreNG VUIDGenerator | Modules/VUID | Deprecate, redirect calls |
| Modules/VUID (DNS-style) | Integer-based | Update generator |
| UUIDCreator library | Modules/VUID | Replace dependency |

### 9.2 Files Requiring Update

See: `VUID-Unified-System-Analysis-260113-V1.md` for complete list (~82 files)

### 9.3 Migration Steps

1. Implement new `VUIDGenerator` in `Modules/VUID`
2. Create database schema (SQLDelight)
3. Add `VIDSyncService` interface (cloud prep)
4. Update all consumers to use new API
5. Deprecate old implementations
6. Delete redundant UUIDCreator copies

---

## 10. Performance Considerations

| Operation | Complexity | Notes |
|-----------|------------|-------|
| Get element by IDs | O(1) | Integer primary key lookup |
| Register new app | O(1) | Insert with auto-increment |
| Register new element | O(1) | Insert with sequence |
| Get readable VUID | O(1) | Single join query |
| Export data | O(n) | Full table scan |
| Import data | O(n) | Batch upsert |

### Memory Footprint

| Component | Size | Notes |
|-----------|------|-------|
| TrackedElement | 8 bytes | Two integers |
| App cache entry | ~100 bytes | With metadata |
| Element cache entry | ~50 bytes | Core properties |

---

## 11. Security Considerations

1. **No Sensitive Data in IDs** - IDs are opaque integers
2. **Fingerprints are One-Way** - Cannot reverse to package name
3. **Export Sanitization** - Strip device-specific data
4. **Cloud Auth** - Require device authentication for sync

---

## 12. Future Considerations

1. **VID Capacity** - Can extend to 8 digits if needed (99M apps)
2. **Regional Registries** - Distributed VID assignment
3. **Element Versioning** - Track element changes across app versions
4. **ML Integration** - Train models on element patterns

---

## Appendix A: Type Code Reference

Full list of 3-character type codes with UI pattern mappings.

| Code | Full Name | Android (View) | Android (Compose) | iOS (UIKit) | iOS (SwiftUI) |
|------|-----------|----------------|-------------------|-------------|---------------|
| btn | Button | Button, ImageButton | Button, IconButton | UIButton | Button |
| inp | Input | EditText | TextField, OutlinedTextField | UITextField | TextField |
| txt | Text | TextView | Text | UILabel | Text |
| img | Image | ImageView | Image | UIImageView | Image |
| lst | List | RecyclerView, ListView | LazyColumn, LazyRow | UITableView | List |
| itm | Item | ViewHolder item | LazyListScope item | UITableViewCell | ForEach item |
| scr | Screen | Activity, Fragment | Scaffold, Screen | UIViewController | View (root) |
| nav | Navigation | NavigationView | NavigationBar | UINavigationBar | NavigationView |
| swt | Switch | Switch, CheckBox | Switch, Checkbox | UISwitch | Toggle |
| slr | Slider | SeekBar | Slider | UISlider | Slider |
| sel | Select | Spinner | DropdownMenu | UIPickerView | Picker |
| dia | Dialog | AlertDialog | AlertDialog | UIAlertController | Alert |
| mnu | Menu | PopupMenu | DropdownMenu | UIMenu | Menu |
| crd | Card | CardView | Card | - | - |
| tab | Tab | TabLayout item | Tab | UITabBarItem | TabView item |
| fab | FAB | FloatingActionButton | FloatingActionButton | - | - |
| prg | Progress | ProgressBar | CircularProgressIndicator | UIActivityIndicator | ProgressView |
| wbv | WebView | WebView | AndroidView(WebView) | WKWebView | WebView |

---

## Appendix B: Example Usage

### B.1 Registering Elements During Scan

```kotlin
class AccessibilityScanner(
    private val vuidGenerator: VUIDGenerator
) {
    suspend fun scanWindow(
        packageName: String,
        version: String,
        rootNode: AccessibilityNodeInfo
    ) {
        // Register/get app ID
        val appId = vuidGenerator.registerApp(
            platform = "and",
            packageName = packageName,
            version = version,
            appName = null // Will be populated later
        )

        // Scan nodes
        scanNode(appId, rootNode)
    }

    private suspend fun scanNode(appId: Int, node: AccessibilityNodeInfo) {
        val type = mapClassToType(node.className.toString())
        val elemHash = ElementHash.generate(
            resourceId = node.viewIdResourceName,
            className = node.className.toString(),
            contentDesc = node.contentDescription?.toString(),
            boundsSignature = null // Calculate if needed
        )

        val elemId = vuidGenerator.getOrCreateElement(
            appId = appId,
            elemHash = elemHash,
            type = type,
            resourceId = node.viewIdResourceName,
            name = node.text?.toString(),
            contentDesc = node.contentDescription?.toString(),
            boundsSignature = null
        )

        // Store tracked element
        val tracked = TrackedElement(appId, elemId)
        // ... use for voice command registration

        // Recurse
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { scanNode(appId, it) }
        }
    }
}
```

### B.2 Querying Elements

```kotlin
// Fast runtime lookup
val element = database.localElementsQueries
    .getByIds(appId = 1, elemId = 42)
    .executeAsOneOrNull()

// Human-readable display
val readable = database.readableElementsQueries
    .getReadableVuid(appId = 1, elemId = 42)
    .executeAsOne()
// Returns: "VID-A-000001:350:btn:like_button"
```

---

**End of Specification**
