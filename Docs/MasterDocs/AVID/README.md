# AVID - Avanues Voice ID System

**Version:** 1.0.0 | **Platform:** Kotlin Multiplatform (KMP) | **Last Updated:** 2026-01-13

---

## Executive Summary

AVID (Avanues Voice ID) is the unified identifier system for the Avanues ecosystem. It replaces the previous fragmented UUID/VUID implementations with a single, consistent format that supports both cloud-synced and local-only identifiers.

### Key Features

| Feature | Description |
|---------|-------------|
| **Unified Format** | Single identifier format across all platforms |
| **Cloud Sync Support** | AVID format for cross-device synchronization |
| **Offline Support** | AVIDL format for local-only, offline scenarios |
| **Platform Awareness** | Embedded platform code for origin tracking |
| **Deterministic Hashing** | ElementFingerprint for reproducible UI element IDs |
| **Cross-Platform** | KMP support for Android, iOS, Desktop |

---

## Table of Contents

1. [Why AVID? (Migration from UUID/VUID)](#why-avid-migration-from-uuidvuid)
2. [ID Format Specification](#id-format-specification)
3. [Cloud vs Local IDs](#cloud-vs-local-ids)
4. [Quick Start Guide](#quick-start-guide)
5. [API Reference](#api-reference)
6. [Integration Examples](#integration-examples)
7. [ElementFingerprint (UI Elements)](#elementfingerprint-ui-elements)
8. [TypeCode Reference](#typecode-reference)
9. [Migration Guide](#migration-guide)

---

## Why AVID? (Migration from UUID/VUID)

### The Problem: Fragmented ID Systems

The Avanues platform previously had multiple overlapping identifier systems:

```
Before AVID (3+ duplicate implementations):
├── Common/uuidcreator/               # Original UUID library
├── Common/Libraries/uuidcreator/     # Duplicate copy
├── Modules/AVAMagic/Libraries/UUIDCreator/  # Another duplicate
├── Modules/VoiceOS/libraries/UUIDCreator/   # Android-only
├── Modules/VUID/                     # KMP VUID attempt
└── VoiceOSCoreNG/common/VUIDGenerator.kt    # Embedded copy
```

**Issues with the old approach:**
- **Inconsistent formats**: UUIDs, VUIDs, and custom hashes all used differently
- **No sync support**: IDs couldn't distinguish between synced and local-only data
- **Platform blindness**: No way to identify which device created an ID
- **Code duplication**: Same logic copy-pasted across modules
- **Type confusion**: No semantic meaning in IDs

### The Solution: AVID

AVID provides a single, unified identifier system:

```
After AVID (single source of truth):
└── Modules/AVID/
    ├── AvidGenerator.kt    # Core generator (AVID/AVIDL)
    ├── Platform.kt         # Platform codes (A=Android, I=iOS, etc.)
    ├── TypeCode.kt         # Semantic type codes (BTN, MSG, TAB, etc.)
    └── Fingerprint.kt      # Deterministic UI element hashing
```

**Benefits:**
- **Single format** for all identifier needs
- **Cloud vs local** distinction built-in
- **Platform tracking** embedded in every ID
- **Semantic types** for filtering and categorization
- **Zero duplication** - one module, all platforms

---

## ID Format Specification

### AVID Format (Cloud-Synced)

```
AVID-{platform}-{sequence}

Examples:
  AVID-A-000001    # Android, first ID
  AVID-I-000042    # iOS, 42nd ID
  AVID-W-001234    # Web, ID 1234
```

| Component | Description | Values |
|-----------|-------------|--------|
| `AVID` | Prefix indicating cloud-synced ID | Always "AVID" |
| `platform` | Single character platform code | A, I, W, M, X, L |
| `sequence` | 6-digit zero-padded sequence number | 000001-999999 |

### AVIDL Format (Local-Only)

```
AVIDL-{platform}-{sequence}

Examples:
  AVIDL-A-000001   # Android, local-only
  AVIDL-I-000042   # iOS, local-only (pending sync)
```

| Component | Description | Values |
|-----------|-------------|--------|
| `AVIDL` | Prefix indicating local-only ID | Always "AVIDL" |
| `platform` | Single character platform code | A, I, W, M, X, L |
| `sequence` | 6-digit zero-padded sequence number | 000001-999999 |

### Platform Codes

| Code | Platform | Description |
|------|----------|-------------|
| `A` | Android | Android devices (phones, tablets, TV) |
| `I` | iOS | iPhone, iPad, Apple Watch |
| `W` | Web | Web browsers (WebAvanue) |
| `M` | macOS | Mac desktop applications |
| `X` | Windows | Windows desktop applications |
| `L` | Linux | Linux desktop applications |

---

## Cloud vs Local IDs

### Understanding the Distinction

AVID provides two ID types to handle the **online/offline dichotomy**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        AVID ID LIFECYCLE                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐        ┌─────────────┐        ┌─────────────┐         │
│  │   OFFLINE   │        │   SYNCING   │        │   SYNCED    │         │
│  │             │        │             │        │             │         │
│  │  AVIDL-A-*  │ ─────▶ │  Promotion  │ ─────▶ │  AVID-A-*   │         │
│  │  (local)    │        │  to AVID    │        │  (global)   │         │
│  └─────────────┘        └─────────────┘        └─────────────┘         │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### When to Use AVID (Cloud-Synced)

Use **AVID** for data that:
- Should sync across devices
- Is part of the user's persistent data
- May be shared or exported
- Needs to be globally unique

**Examples:**
- Messages in AVA conversations
- Browser tabs/favorites in WebAvanue
- User preferences and settings
- Documents and notes

```kotlin
// Generate cloud-synced ID
val messageId = AvidGenerator.generateMessageId()
// Result: AVID-A-000123

val tabId = AvidGenerator.generateTabId()
// Result: AVID-A-000124
```

### When to Use AVIDL (Local-Only)

Use **AVIDL** for data that:
- Is temporary or ephemeral
- Should NOT sync to cloud
- Is device-specific
- Is pending user confirmation before sync

**Examples:**
- Draft messages (before sending)
- Temporary UI element references
- Session-specific state
- Cached data

```kotlin
// Generate local-only ID
val draftId = AvidGenerator.generateLocal()
// Result: AVIDL-A-000001

val sessionId = AvidGenerator.generateSessionId()
// Result: AVID-A-000002 (sessions are typically synced)
```

### Promoting Local IDs to Cloud

When offline data should be synced:

```kotlin
// User was offline, created a draft with local ID
val localId = "AVIDL-A-000047"

// Now online, promote to cloud ID
val cloudId = AvidGenerator.promoteToGlobal(localId)
// Result: "AVID-A-000047" (same sequence, different prefix)

// Original local ID is now invalid - update all references
database.updateId(oldId = localId, newId = cloudId)
```

### Sync Workflow Example

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     OFFLINE-TO-ONLINE SYNC FLOW                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User creates data OFFLINE                                           │
│     └─▶ AVIDL-A-000001 (local ID assigned)                             │
│                                                                         │
│  2. User continues working OFFLINE                                      │
│     └─▶ AVIDL-A-000002, AVIDL-A-000003, ... (more local IDs)           │
│                                                                         │
│  3. Device comes ONLINE                                                 │
│     └─▶ Sync service detects pending local IDs                         │
│                                                                         │
│  4. PROMOTION process:                                                  │
│     ├─▶ AVIDL-A-000001 → AVID-A-000001 (promoted)                      │
│     ├─▶ AVIDL-A-000002 → AVID-A-000002 (promoted)                      │
│     └─▶ Update all foreign key references                              │
│                                                                         │
│  5. Cloud receives promoted IDs                                         │
│     └─▶ All devices now see AVID-A-000001, AVID-A-000002               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Start Guide

### Installation

Add AVID dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":Modules:AVID"))
}
```

### Basic Usage

```kotlin
import com.augmentalis.avid.AvidGenerator
import com.augmentalis.avid.Platform

// Step 1: Set platform at app startup (once)
AvidGenerator.setPlatform(Platform.ANDROID)

// Step 2: Generate IDs as needed
val messageId = AvidGenerator.generate()           // AVID-A-000001
val localId = AvidGenerator.generateLocal()        // AVIDL-A-000002
val tabId = AvidGenerator.generateTabId()          // AVID-A-000003

// Step 3: Validate IDs
val isValid = AvidGenerator.isAvid("AVID-A-000001")     // true
val isLocal = AvidGenerator.isAvidl("AVIDL-A-000001")   // true

// Step 4: Parse IDs
val parsed = AvidGenerator.parse("AVID-A-000123")
// parsed.prefix = "AVID"
// parsed.platform = Platform.ANDROID
// parsed.sequence = 123
```

---

## API Reference

### AvidGenerator

The primary interface for generating and managing AVIDs.

```kotlin
object AvidGenerator {

    // ═══════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Set the platform for ID generation.
     * Call once at app startup.
     */
    fun setPlatform(platform: Platform)

    /**
     * Get the current platform.
     */
    fun getPlatform(): Platform

    // ═══════════════════════════════════════════════════════════════════
    // CLOUD-SYNCED IDS (AVID)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Generate a cloud-synced AVID.
     *
     * @param platform Optional platform override
     * @param sequence Optional sequence number (auto-increments if null)
     * @return AVID string, e.g., "AVID-A-000001"
     */
    fun generate(platform: Platform = currentPlatform, sequence: Long? = null): String

    // ═══════════════════════════════════════════════════════════════════
    // LOCAL-ONLY IDS (AVIDL)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Generate a local-only AVIDL.
     *
     * @param platform Optional platform override
     * @param sequence Optional sequence number (auto-increments if null)
     * @return AVIDL string, e.g., "AVIDL-A-000001"
     */
    fun generateLocal(platform: Platform = currentPlatform, sequence: Long? = null): String

    // ═══════════════════════════════════════════════════════════════════
    // CONVENIENCE METHODS (Common Entity Types)
    // ═══════════════════════════════════════════════════════════════════

    fun generateMessageId(): String      // For chat messages
    fun generateTabId(): String          // For browser tabs
    fun generateFavoriteId(): String     // For bookmarks
    fun generateDownloadId(): String     // For downloads
    fun generateHistoryId(): String      // For history entries
    fun generateSessionId(): String      // For sessions
    fun generateGroupId(): String        // For groups/folders
    fun generateConversationId(): String // For conversations
    fun generateDocumentId(): String     // For documents
    fun generateChunkId(): String        // For document chunks
    fun generateMemoryId(): String       // For memory entries
    fun generateDecisionId(): String     // For decisions
    fun generateLearningId(): String     // For learning data

    // ═══════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Check if string is a valid AVID (cloud-synced).
     */
    fun isAvid(id: String): Boolean

    /**
     * Check if string is a valid AVIDL (local-only).
     */
    fun isAvidl(id: String): Boolean

    /**
     * Check if string is any valid AVID format (AVID or AVIDL).
     */
    fun isValid(id: String): Boolean

    // ═══════════════════════════════════════════════════════════════════
    // PARSING
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Parse an AVID/AVIDL into its components.
     *
     * @param id The ID string to parse
     * @return ParsedAvid or null if invalid
     */
    fun parse(id: String): ParsedAvid?

    // ═══════════════════════════════════════════════════════════════════
    // PROMOTION (Local to Cloud)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Promote a local AVIDL to cloud-synced AVID.
     *
     * @param localId The AVIDL to promote
     * @return The promoted AVID, or null if input is invalid
     */
    fun promoteToGlobal(localId: String): String?
}

/**
 * Parsed AVID components.
 */
data class ParsedAvid(
    val prefix: String,        // "AVID" or "AVIDL"
    val platform: Platform,    // Platform enum
    val sequence: Long,        // Sequence number
    val isLocal: Boolean       // true if AVIDL
)
```

### Platform Enum

```kotlin
enum class Platform(val code: Char, val displayName: String) {
    ANDROID('A', "Android"),
    IOS('I', "iOS"),
    WEB('W', "Web"),
    MACOS('M', "macOS"),
    WINDOWS('X', "Windows"),
    LINUX('L', "Linux");

    companion object {
        /**
         * Get Platform from single-character code.
         */
        fun fromCode(code: Char): Platform?
    }
}
```

---

## Integration Examples

### AVA Chat Messages

```kotlin
class ChatRepository(private val db: ChatDatabase) {

    // Initialize platform once at app startup
    init {
        AvidGenerator.setPlatform(Platform.ANDROID)
    }

    suspend fun sendMessage(content: String, conversationId: String): Message {
        // Generate cloud-synced ID for message
        val messageId = AvidGenerator.generateMessageId()

        val message = Message(
            id = messageId,           // AVID-A-000123
            conversationId = conversationId,
            content = content,
            timestamp = Clock.System.now(),
            status = MessageStatus.SENT
        )

        db.messages.insert(message)
        syncService.enqueue(message)

        return message
    }

    suspend fun createDraftMessage(content: String): Message {
        // Generate local-only ID for draft (not synced yet)
        val draftId = AvidGenerator.generateLocal()

        val draft = Message(
            id = draftId,             // AVIDL-A-000124
            conversationId = null,    // No conversation yet
            content = content,
            timestamp = Clock.System.now(),
            status = MessageStatus.DRAFT
        )

        db.drafts.insert(draft)
        return draft
    }

    suspend fun promoteDraftToMessage(draft: Message, conversationId: String): Message {
        // Promote local ID to cloud ID
        val cloudId = AvidGenerator.promoteToGlobal(draft.id)
            ?: throw IllegalStateException("Invalid draft ID: ${draft.id}")

        val message = draft.copy(
            id = cloudId,             // AVID-A-000124
            conversationId = conversationId,
            status = MessageStatus.SENT
        )

        db.drafts.delete(draft.id)
        db.messages.insert(message)
        syncService.enqueue(message)

        return message
    }
}
```

### WebAvanue Tab Management

```kotlin
class TabManager(private val db: BrowserDatabase) {

    suspend fun createTab(url: String): BrowserTab {
        val tabId = AvidGenerator.generateTabId()

        val tab = BrowserTab(
            id = tabId,               // AVID-A-000456
            url = url,
            title = "Loading...",
            createdAt = Clock.System.now(),
            lastAccessedAt = Clock.System.now()
        )

        db.tabs.insert(tab)
        return tab
    }

    suspend fun createFavorite(tab: BrowserTab): Favorite {
        val favoriteId = AvidGenerator.generateFavoriteId()

        val favorite = Favorite(
            id = favoriteId,          // AVID-A-000457
            url = tab.url,
            title = tab.title,
            folderId = null,
            createdAt = Clock.System.now()
        )

        db.favorites.insert(favorite)
        return favorite
    }

    suspend fun createFolder(name: String, parentId: String? = null): Folder {
        val folderId = AvidGenerator.generateGroupId()

        val folder = Folder(
            id = folderId,            // AVID-A-000458
            name = name,
            parentId = parentId,
            createdAt = Clock.System.now()
        )

        db.folders.insert(folder)
        return folder
    }
}
```

### Cross-Platform Sync

```kotlin
class SyncService {

    suspend fun syncFromCloud(cloudData: List<CloudEntity>) {
        for (entity in cloudData) {
            // Validate that cloud data has proper AVID format
            if (!AvidGenerator.isAvid(entity.id)) {
                Log.w("SyncService", "Invalid cloud ID: ${entity.id}")
                continue
            }

            // Parse to verify platform origin
            val parsed = AvidGenerator.parse(entity.id)
            Log.d("SyncService", "Received ${entity.id} from ${parsed?.platform?.displayName}")

            // Merge with local data
            localDb.upsert(entity)
        }
    }

    suspend fun pushToCloud(): List<CloudEntity> {
        // Find all local-only IDs that need promotion
        val localItems = localDb.findByIdPrefix("AVIDL")

        return localItems.map { item ->
            // Promote to cloud ID
            val cloudId = AvidGenerator.promoteToGlobal(item.id)
                ?: throw IllegalStateException("Cannot promote: ${item.id}")

            // Update local record
            localDb.updateId(item.id, cloudId)

            // Return entity with cloud ID
            item.copy(id = cloudId)
        }
    }
}
```

---

## ElementFingerprint (UI Elements)

For UI elements that need **deterministic, reproducible IDs** (same element always gets same ID), use `ElementFingerprint` instead of sequential AVIDs.

### Why ElementFingerprint?

| Requirement | AVID | ElementFingerprint |
|-------------|------|-------------------|
| Unique across sessions | Yes | Yes |
| Same ID for same element | No (sequential) | Yes (hash-based) |
| Cloud syncable | Yes | Not typically |
| Human readable | Partially | Yes (BTN:a3f2e1c9) |
| Semantic type | No | Yes (type code prefix) |

### ElementFingerprint Format

```
{TypeCode}:{hash8}

Examples:
  BTN:a3f2e1c9    # Button element
  INP:b917cc9d    # Input/EditText element
  TXT:c8e2f1a0    # TextView element
  SCR:d1b3e4f2    # Scrollable element
```

### Using ElementFingerprint

```kotlin
import com.augmentalis.voiceoscore.ElementFingerprint

// Generate fingerprint for UI element
// packageName is included in the hash for cross-app uniqueness and VOS export portability
val fingerprint = ElementFingerprint.generate(
    className = "android.widget.Button",
    packageName = "com.example.app",
    resourceId = "com.example.app:id/submit_btn",
    text = "Submit",
    contentDesc = "Submit form button"
)
// Result: "BTN:a3f2e1c9"

// Parse fingerprint
val parsed = ElementFingerprint.parse(fingerprint)
// parsed.first = "BTN" (type code)
// parsed.second = "a3f2e1c9" (hash)

// Validate fingerprint
val isValid = ElementFingerprint.isValid(fingerprint)  // true

// Get type code only
val typeCode = ElementFingerprint.getTypeCode("android.widget.Button")
// Result: "BTN"
```

### VoiceOSCoreNG Integration

```kotlin
// In VoiceOSCoreNG command processing
class CommandGenerator {

    fun fromElement(element: ElementInfo, packageName: String): QuantizedCommand {
        // Generate deterministic fingerprint for element
        val fingerprint = ElementFingerprint.generate(
            className = element.className,
            packageName = packageName,
            resourceId = element.resourceId,
            text = element.text,
            contentDesc = element.contentDescription
        )

        return QuantizedCommand(
            uuid = AvidGenerator.generate(),  // Unique command ID
            phrase = "click ${element.text}",
            actionType = CommandActionType.CLICK,
            targetVuid = fingerprint,         // Element fingerprint
            confidence = 0.9f,
            metadata = mapOf("packageName" to packageName)
        )
    }
}
```

---

## TypeCode Reference

TypeCode provides semantic categorization for identifiers and fingerprints.

### UI Element Types

| Code | Type | Class Names |
|------|------|-------------|
| `BTN` | Button | Button, ImageButton, FloatingActionButton |
| `INP` | Input | EditText, TextField, TextInput |
| `TXT` | Text | TextView, Label |
| `IMG` | Image | ImageView, Icon |
| `SCR` | Scroll | ScrollView, RecyclerView, ListView |
| `CRD` | Card | CardView |
| `LST` | List | RecyclerView, ListView |
| `ITM` | Item | ListItem, ViewHolder |
| `MNU` | Menu | Menu, PopupMenu |
| `DLG` | Dialog | Dialog, AlertDialog, BottomSheet |
| `CHK` | Checkbox | Checkbox, CheckboxPreference |
| `SWT` | Switch | Switch, Toggle, SwitchCompat |
| `SLD` | Slider | Slider, SeekBar |
| `TAB` | Tab | Tab, BrowserTab |
| `LAY` | Layout | Layout, Container, View, ViewGroup |

### Entity Types

| Code | Type | Use Case |
|------|------|----------|
| `MSG` | Message | Chat messages, notifications |
| `CNV` | Conversation | Chat threads, conversations |
| `DOC` | Document | Files, documents |
| `CHU` | Chunk | Document chunks (RAG) |
| `MEM` | Memory | AI memory entries |
| `DEC` | Decision | Decision logs |
| `LRN` | Learning | Learning data |
| `INT` | Intent | NLU intents |
| `FAV` | Favorite | Bookmarks |
| `DWN` | Download | Downloads |
| `HST` | History | History entries |
| `SES` | Session | Browser sessions |
| `GRP` | Group | Groups, folders |
| `CMD` | Command | Voice commands |
| `SCN` | Screen | Screen states |
| `APP` | App | Applications |
| `ELM` | Element | Default/unknown elements |

### TypeCode API

```kotlin
object TypeCode {
    // Constants
    const val BUTTON = "BTN"
    const val INPUT = "INP"
    const val TEXT = "TXT"
    // ... (40+ type codes)

    /**
     * Infer type code from class name.
     *
     * @param typeName Class name or type string
     * @return 3-character type code
     */
    fun fromTypeName(typeName: String): String
}

// Usage
val code = TypeCode.fromTypeName("android.widget.Button")  // "BTN"
val code = TypeCode.fromTypeName("EditText")               // "INP"
val code = TypeCode.fromTypeName("UnknownWidget")          // "ELM"
```

---

## Migration Guide

### From UUID to AVID

```kotlin
// OLD (UUID)
import java.util.UUID
val id = UUID.randomUUID().toString()
// Result: "550e8400-e29b-41d4-a716-446655440000"

// NEW (AVID)
import com.augmentalis.avid.AvidGenerator
val id = AvidGenerator.generate()
// Result: "AVID-A-000001"
```

### From VUIDGenerator to AvidGenerator/ElementFingerprint

```kotlin
// OLD (VUIDGenerator)
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
val vuid = VUIDGenerator.generate(packageName, VUIDTypeCode.BUTTON, elementHash)
val typeCode = VUIDGenerator.getTypeCode(className)
val isValid = VUIDGenerator.isValidVUID(vuid)
val components = VUIDGenerator.parseVUID(vuid)

// NEW (ElementFingerprint for UI elements)
import com.augmentalis.voiceoscore.ElementFingerprint
val fingerprint = ElementFingerprint.generate(className, packageName, resourceId, text, contentDesc)
val typeCode = ElementFingerprint.getTypeCode(className)
val isValid = ElementFingerprint.isValid(fingerprint)
val components = ElementFingerprint.parse(fingerprint)

// NEW (AvidGenerator for entity IDs)
import com.augmentalis.avid.AvidGenerator
val entityId = AvidGenerator.generate()           // For entities
val localId = AvidGenerator.generateLocal()       // For local-only
val isValid = AvidGenerator.isAvid(entityId)
val parsed = AvidGenerator.parse(entityId)
```

### From VUIDTypeCode to TypeCode

```kotlin
// OLD
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode
val type = VUIDTypeCode.BUTTON

// NEW
import com.augmentalis.avid.TypeCode
val type = TypeCode.BUTTON
```

### Module Dependency Updates

```kotlin
// OLD (build.gradle.kts)
dependencies {
    implementation(project(":Common:VUID"))
    // or
    implementation(project(":Modules:VoiceOSCoreNG"))  // for VUIDGenerator
}

// NEW (build.gradle.kts)
dependencies {
    implementation(project(":Modules:AVID"))
}
```

---

## File Structure

```
Modules/AVID/
├── build.gradle.kts              # KMP build configuration
└── src/
    └── commonMain/
        └── kotlin/
            └── com/augmentalis/avid/
                ├── AvidGenerator.kt    # Core AVID/AVIDL generation
                ├── Platform.kt         # Platform enum (A, I, W, M, X, L)
                ├── TypeCode.kt         # Semantic type codes (40+)
                └── Fingerprint.kt      # Deterministic hashing
```

---

## Best Practices

### 1. Initialize Platform Once

```kotlin
// In Application.onCreate() or main entry point
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AvidGenerator.setPlatform(Platform.ANDROID)
    }
}
```

### 2. Use Convenience Methods

```kotlin
// Prefer semantic methods over generic generate()
val messageId = AvidGenerator.generateMessageId()    // Clear intent
val tabId = AvidGenerator.generateTabId()            // Clear intent

// Avoid when entity type is clear
val id = AvidGenerator.generate()                     // Less clear
```

### 3. Validate External IDs

```kotlin
// Always validate IDs from external sources
fun processExternalData(data: ExternalEntity) {
    if (!AvidGenerator.isValid(data.id)) {
        throw IllegalArgumentException("Invalid ID: ${data.id}")
    }
    // Process validated data...
}
```

### 4. Use ElementFingerprint for UI Elements

```kotlin
// For UI elements that need reproducible IDs
val fingerprint = ElementFingerprint.generate(...)   // Deterministic

// For entities that need unique sequential IDs
val entityId = AvidGenerator.generate()              // Sequential
```

### 5. Handle Offline Gracefully

```kotlin
// Create with local ID when offline
val localId = AvidGenerator.generateLocal()          // AVIDL-A-*

// Promote when online
val cloudId = AvidGenerator.promoteToGlobal(localId) // AVID-A-*
```

---

## Related Documentation

- [VoiceOSCoreNG README](../VoiceOSCoreNG/README.md) - Voice command engine
- [WebAvanue README](../WebAvanue/README.md) - Voice-controlled browser
- [AVA README](../AVA/README.md) - AI assistant platform
- [PLATFORM-INDEX](../AI/PLATFORM-INDEX.ai.md) - AI-readable platform index

---

**Author:** Avanues Development Team | **Last Updated:** 2026-01-13 | **Version:** 1.0.0
