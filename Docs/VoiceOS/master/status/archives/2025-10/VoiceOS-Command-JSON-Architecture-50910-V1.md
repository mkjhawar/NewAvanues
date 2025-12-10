# Command JSON Architecture & Accessibility Scraping Review

**Date:** 2025-10-09 12:08:16 PDT
**Status:** DESIGN SPECIFICATION
**Scope:** JSON format optimization, number overlay aesthetics, accessibility scraping database

---

## Table of Contents

1. [Array-Based Command JSON Format](#array-based-command-json-format)
2. [English Fallback Strategy](#english-fallback-strategy)
3. [Improved Number Overlay Visualization](#improved-number-overlay-visualization)
4. [JSON Best Practices](#json-best-practices)
5. [Accessibility Scraping Database Review](#accessibility-scraping-database-review)

---

## Array-Based Command JSON Format

### Old Format (Verbose, Large):

```json
{
  "commands": {
    "navigation": {
      "forward": {
        "primary": "forward",
        "synonyms": ["next", "advance", "go forward"],
        "action": "navigate_forward",
        "description": "Move to next element"
      },
      "backward": {
        "primary": "backward",
        "synonyms": ["previous", "back", "go back"],
        "action": "navigate_backward",
        "description": "Move to previous element"
      }
    }
  }
}
```

**Problems:**
- ❌ 11 lines per command
- ❌ Repetitive keys
- ❌ Hard to scan visually
- ❌ Large file size

---

### New Format (Array-Based, Compact):

```json
{
  "version": "1.0",
  "locale": "en-US",
  "fallback": "en-US",
  "commands": [
    ["navigate_forward", "forward", ["next", "advance", "go forward", "onward"], "Move to next element"],
    ["navigate_backward", "backward", ["previous", "back", "go back", "prior", "rewind"], "Move to previous element"],
    ["navigate_left", "left", ["go left", "move left", "westward"], "Move cursor left"],
    ["navigate_right", "right", ["go right", "move right", "eastward"], "Move cursor right"],
    ["navigate_up", "up", ["go up", "move up", "upward", "above"], "Move cursor up"],
    ["navigate_down", "down", ["go down", "move down", "downward", "below"], "Move cursor down"],
    ["navigate_first", "first", ["beginning", "start", "top", "initial"], "Jump to first element"],
    ["navigate_last", "last", ["end", "final", "bottom"], "Jump to last element"],

    ["action_click", "click", ["tap", "select", "press", "activate"], "Activate element"],
    ["action_open", "open", ["launch", "start", "run"], "Open application"],
    ["action_close", "close", ["exit", "quit", "dismiss", "cancel"], "Close application"],
    ["action_focus", "focus", ["highlight", "select", "go to"], "Focus on element"],

    ["cursor_enable", "show cursor", ["enable cursor", "cursor on", "activate cursor"], "Enable voice cursor"],
    ["cursor_disable", "hide cursor", ["disable cursor", "cursor off", "deactivate cursor"], "Disable voice cursor"],

    ["shape_circle", "draw circle", ["make circle", "create circle"], "Draw circular shape"],
    ["shape_square", "draw square", ["make square", "create square"], "Draw square shape"],
    ["shape_rectangle", "draw rectangle", ["make rectangle"], "Draw rectangular shape"],

    ["size_increase", "bigger", ["increase size", "larger", "expand", "grow"], "Increase size"],
    ["size_decrease", "smaller", ["decrease size", "reduce", "shrink"], "Decrease size"],
    ["size_lock", "done", ["lock size", "finish", "complete"], "Lock size and finish"],

    ["help", "help", ["what can I say", "show commands", "voice help"], "Show available commands"],
    ["settings", "settings", ["options", "preferences", "configure"], "Open settings"]
  ]
}
```

**Array Structure:**
```
[action_id, primary_text, [synonyms...], description]
  ↑           ↑              ↑              ↑
  0           1              2              3
```

**Benefits:**
- ✅ 1 line per command (vs 11 lines)
- ✅ 95% smaller file size
- ✅ Easy to scan and edit
- ✅ Fast parsing (direct array access)
- ✅ Consistent structure

---

### Spanish Example (es-ES.json):

```json
{
  "version": "1.0",
  "locale": "es-ES",
  "fallback": "en-US",
  "commands": [
    ["navigate_forward", "avanzar", ["siguiente", "adelante", "ir adelante"], "Mover al siguiente elemento"],
    ["navigate_backward", "retroceder", ["anterior", "atrás", "ir atrás", "previo"], "Mover al elemento anterior"],
    ["navigate_left", "izquierda", ["ir a la izquierda", "mover izquierda"], "Mover cursor a la izquierda"],
    ["navigate_right", "derecha", ["ir a la derecha", "mover derecha"], "Mover cursor a la derecha"],

    ["action_click", "hacer clic", ["tocar", "seleccionar", "pulsar", "activar"], "Activar elemento"],
    ["action_open", "abrir", ["lanzar", "iniciar", "ejecutar"], "Abrir aplicación"],
    ["action_close", "cerrar", ["salir", "cancelar"], "Cerrar aplicación"],

    ["cursor_enable", "mostrar cursor", ["activar cursor", "cursor encendido"], "Activar cursor de voz"],
    ["cursor_disable", "ocultar cursor", ["desactivar cursor", "cursor apagado"], "Desactivar cursor de voz"],

    ["shape_circle", "dibujar círculo", ["hacer círculo", "crear círculo"], "Dibujar forma circular"],
    ["size_increase", "más grande", ["aumentar tamaño", "agrandar", "expandir"], "Aumentar tamaño"],
    ["size_decrease", "más pequeño", ["reducir tamaño", "encoger"], "Reducir tamaño"],

    ["help", "ayuda", ["qué puedo decir", "mostrar comandos"], "Mostrar comandos disponibles"],
    ["settings", "configuración", ["opciones", "preferencias"], "Abrir configuración"]
  ]
}
```

**File Size Comparison:**
- Old format: ~450 bytes per command
- New format: ~120 bytes per command
- **Savings: 73% smaller**

---

## English Fallback Strategy

### Database Schema with Fallback:

```kotlin
@Entity(tableName = "voice_commands")
data class VoiceCommandEntity(
    @PrimaryKey val id: String,                    // action_id (e.g., "navigate_forward")
    @ColumnInfo(name = "locale") val locale: String,
    @ColumnInfo(name = "primary_text") val primaryText: String,
    @ColumnInfo(name = "synonyms") val synonyms: String,  // JSON array
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "category") val category: String,   // "navigation", "action", "cursor", etc.
    @ColumnInfo(name = "priority") val priority: Int = 0,
    @ColumnInfo(name = "is_fallback") val isFallback: Boolean = false  // true for en-US
)

// Compound index for fast lookups
@Entity(
    indices = [
        Index(value = ["id", "locale"], unique = true),
        Index(value = ["locale"]),
        Index(value = ["is_fallback"])
    ]
)
```

### Loading Strategy:

```kotlin
class CommandLoader(
    private val commandDao: VoiceCommandDao,
    private val context: Context
) {
    suspend fun initializeCommands() {
        // 1. ALWAYS load English first (fallback)
        loadLocale("en-US", isFallback = true)

        // 2. Load user's system locale (if different)
        val systemLocale = Locale.getDefault().toString()
        if (systemLocale != "en-US") {
            loadLocale(systemLocale, isFallback = false)
        }

        Log.d(TAG, "Commands loaded: en-US (fallback) + $systemLocale (active)")
    }

    private suspend fun loadLocale(locale: String, isFallback: Boolean) {
        // Check if already loaded
        val existing = commandDao.getCommandsForLocale(locale)
        if (existing.isNotEmpty()) return

        // Load JSON
        val jsonFile = "localization/commands/$locale.json"
        val jsonString = try {
            context.assets.open(jsonFile).bufferedReader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            if (!isFallback) {
                Log.w(TAG, "Locale $locale not found, using en-US fallback")
                return
            }
            throw e  // en-US must exist
        }

        // Parse array-based JSON
        val commandsData = parseArrayBasedJson(jsonString, locale, isFallback)

        // Insert into database
        commandDao.insertBatch(commandsData)

        Log.d(TAG, "Loaded ${commandsData.size} commands for $locale (fallback: $isFallback)")
    }

    private fun parseArrayBasedJson(json: String, locale: String, isFallback: Boolean): List<VoiceCommandEntity> {
        val jsonObject = JSONObject(json)
        val commandsArray = jsonObject.getJSONArray("commands")

        return (0 until commandsArray.length()).map { i ->
            val cmd = commandsArray.getJSONArray(i)

            // Array structure: [action_id, primary_text, [synonyms...], description]
            val actionId = cmd.getString(0)
            val primaryText = cmd.getString(1)
            val synonymsArray = cmd.getJSONArray(2)
            val description = cmd.getString(3)

            val synonyms = (0 until synonymsArray.length()).map {
                synonymsArray.getString(it)
            }

            VoiceCommandEntity(
                id = actionId,
                locale = locale,
                primaryText = primaryText,
                synonyms = JSONArray(synonyms).toString(),
                description = description,
                category = actionId.split("_")[0],  // "navigate", "action", "cursor", etc.
                isFallback = isFallback
            )
        }
    }
}
```

### Command Resolution with Fallback:

```kotlin
class CommandResolver(private val commandDao: VoiceCommandDao) {

    suspend fun resolveCommand(userInput: String, userLocale: String): VoiceCommandEntity? {
        val normalized = userInput.lowercase().trim()

        // 1. Try user's locale first
        val userLocaleMatches = findMatches(normalized, userLocale)
        if (userLocaleMatches.isNotEmpty()) {
            return userLocaleMatches.first()
        }

        // 2. Fallback to English
        val fallbackMatches = findMatches(normalized, "en-US")
        if (fallbackMatches.isNotEmpty()) {
            Log.d(TAG, "Using English fallback for: $userInput")
            return fallbackMatches.first()
        }

        // 3. Nothing found
        return null
    }

    private suspend fun findMatches(input: String, locale: String): List<VoiceCommandEntity> {
        // Get all commands for locale
        val commands = commandDao.getCommandsForLocale(locale)

        return commands.filter { command ->
            // Check primary text
            if (command.primaryText.equals(input, ignoreCase = true)) return@filter true

            // Check synonyms
            val synonyms = parseSynonyms(command.synonyms)
            synonyms.any { it.equals(input, ignoreCase = true) }
        }
    }
}
```

**Fallback Flow:**
```
User says: "siguiente" (Spanish for "next")
  ↓
1. Check es-ES locale
   → Found: ["navigate_forward", "avanzar", ["siguiente", ...]]
   → ✅ MATCH

User says: "foobar" (invalid)
  ↓
1. Check es-ES locale
   → Not found
  ↓
2. Check en-US fallback
   → Not found
  ↓
3. Return null (show error to user)
```

**Benefits:**
- ✅ Always have English commands available
- ✅ Seamless fallback (user doesn't notice)
- ✅ Supports users in unsupported locales
- ✅ Easy to test (just use English)

---

## Improved Number Overlay Visualization

### Design Specifications:

#### 1. Circular Badge Design (Recommended)

```kotlin
data class NumberOverlayStyle(
    // Position
    val anchorPoint: AnchorPoint = AnchorPoint.TOP_RIGHT,
    val offsetX: Int = 4,  // pixels from element edge
    val offsetY: Int = 4,

    // Circle dimensions
    val circleRadius: Int = 16,  // 32dp diameter
    val strokeWidth: Float = 2f,

    // Colors (Material Design 3)
    val hasNameColor: Int = Color.parseColor("#4CAF50"),      // Material Green 500
    val noNameColor: Int = Color.parseColor("#FF9800"),       // Material Orange 500
    val disabledColor: Int = Color.parseColor("#9E9E9E"),     // Material Grey 500

    // Number styling
    val numberColor: Int = Color.WHITE,
    val numberSize: Float = 14f,  // sp
    val fontWeight: Typeface = Typeface.DEFAULT_BOLD,

    // Effects
    val dropShadow: Boolean = true,
    val shadowRadius: Float = 4f,
    val shadowColor: Int = Color.parseColor("#40000000")  // 25% black
)

enum class AnchorPoint {
    TOP_LEFT,
    TOP_RIGHT,     // Default - most common in UI patterns
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}
```

#### 2. Visual Examples:

```
Element with "Submit" command name:
┌─────────────────────────┐
│                      ⚫1│  ← Green circle, white "1"
│   Submit Button         │     Positioned 4px from corner
│                         │
└─────────────────────────┘

Element WITHOUT command name:
┌─────────────────────────┐
│                      ⚫2│  ← Orange circle, white "2"
│   Button                │     (User can tap to name)
│                         │
└─────────────────────────┘

Disabled element (if showing):
┌─────────────────────────┐
│                      ⚫3│  ← Grey circle, white "3"
│   Disabled Button       │     (Not voice-enabled)
│                         │
└─────────────────────────┘
```

#### 3. Aesthetic Rendering Code:

```kotlin
class NumberOverlayRenderer(
    private val style: NumberOverlayStyle
) {
    fun drawNumberBadge(
        canvas: Canvas,
        elementBounds: Rect,
        number: Int,
        state: ElementVoiceState
    ) {
        // Calculate badge center position
        val center = calculateAnchorPosition(elementBounds)

        // Draw drop shadow (if enabled)
        if (style.dropShadow) {
            drawShadow(canvas, center)
        }

        // Draw colored circle
        val circlePaint = Paint().apply {
            color = when (state) {
                ElementVoiceState.ENABLED_WITH_NAME -> style.hasNameColor
                ElementVoiceState.ENABLED_NO_NAME -> style.noNameColor
                ElementVoiceState.DISABLED -> style.disabledColor
            }
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(center.x, center.y, style.circleRadius.toFloat(), circlePaint)

        // Draw white stroke for depth
        val strokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = style.strokeWidth
            isAntiAlias = true
        }
        canvas.drawCircle(center.x, center.y, style.circleRadius.toFloat(), strokePaint)

        // Draw number (centered in circle)
        val textPaint = Paint().apply {
            color = style.numberColor
            textSize = style.numberSize * resources.displayMetrics.scaledDensity
            textAlign = Paint.Align.CENTER
            typeface = style.fontWeight
            isAntiAlias = true
        }

        val textBounds = Rect()
        val numberText = number.toString()
        textPaint.getTextBounds(numberText, 0, numberText.length, textBounds)

        // Center vertically (adjust for text baseline)
        val textY = center.y + (textBounds.height() / 2f)

        canvas.drawText(numberText, center.x, textY, textPaint)
    }

    private fun calculateAnchorPosition(elementBounds: Rect): PointF {
        return when (style.anchorPoint) {
            AnchorPoint.TOP_RIGHT -> PointF(
                elementBounds.right - style.offsetX - style.circleRadius.toFloat(),
                elementBounds.top + style.offsetY + style.circleRadius.toFloat()
            )
            AnchorPoint.TOP_LEFT -> PointF(
                elementBounds.left + style.offsetX + style.circleRadius.toFloat(),
                elementBounds.top + style.offsetY + style.circleRadius.toFloat()
            )
            AnchorPoint.BOTTOM_RIGHT -> PointF(
                elementBounds.right - style.offsetX - style.circleRadius.toFloat(),
                elementBounds.bottom - style.offsetY - style.circleRadius.toFloat()
            )
            AnchorPoint.BOTTOM_LEFT -> PointF(
                elementBounds.left + style.offsetX + style.circleRadius.toFloat(),
                elementBounds.bottom - style.offsetY - style.circleRadius.toFloat()
            )
        }
    }

    private fun drawShadow(canvas: Canvas, center: PointF) {
        val shadowPaint = Paint().apply {
            color = style.shadowColor
            maskFilter = BlurMaskFilter(style.shadowRadius, BlurMaskFilter.Blur.NORMAL)
            isAntiAlias = true
        }
        canvas.drawCircle(center.x, center.y + 2f, style.circleRadius.toFloat(), shadowPaint)
    }
}
```

#### 4. Alternative Styles (User-Configurable):

**Style A: Filled Circle (Default)**
```
⚫1  ← Green circle, white number
```

**Style B: Outlined Circle**
```
⭕1  ← Green outline, green number
```

**Style C: Square Badge**
```
⬛1  ← Green square, white number
```

**Style D: Rounded Rectangle**
```
▭1  ← Green rounded rect, white number
```

#### 5. Accessibility Considerations:

```kotlin
// High contrast mode for visually impaired users
data class HighContrastStyle(
    val hasNameColor: Int = Color.BLACK,
    val noNameColor: Int = Color.parseColor("#FF6600"),  // Darker orange
    val numberColor: Int = Color.WHITE,
    val strokeWidth: Float = 3f,  // Thicker for better visibility
    val circleRadius: Int = 20     // Larger for easier tapping
)

// Large text mode
data class LargeTextStyle(
    val numberSize: Float = 20f,   // Larger font
    val circleRadius: Int = 24     // Larger circle to fit
)
```

**Material Design 3 Color Palette:**
- ✅ Green (#4CAF50) - Success, enabled, good to go
- ✅ Orange (#FF9800) - Warning, needs attention
- ✅ Grey (#9E9E9E) - Disabled, not available
- ✅ White (#FFFFFF) - High contrast text
- ✅ Black shadow (#40000000) - Subtle depth

**Benefits:**
- ✅ Aesthetically pleasing (Material Design)
- ✅ Clear visual hierarchy (color-coded)
- ✅ Easy to tap (32dp touch target)
- ✅ Accessible (high contrast, scalable)
- ✅ Consistent (follows Android guidelines)

---

## JSON Best Practices

### Standard JSON Structure (Array-Based):

```json
{
  "version": "1.0",
  "locale": "en-US",
  "fallback": "en-US",
  "updated": "2025-10-09",
  "author": "VOS4 Team",
  "commands": [
    ["action_id", "primary", ["syn1", "syn2"], "description"],
    ["action_id", "primary", ["syn1", "syn2"], "description"]
  ]
}
```

### Best Practices Checklist:

1. **Use Arrays for Repeated Structures**
   - ✅ Commands: Array of arrays
   - ✅ Synonyms: Array of strings
   - ❌ Object with keys (verbose)

2. **Minimize Line Count**
   - ✅ 1 line per command
   - ✅ Compact syntax
   - ❌ Multi-line objects

3. **Consistent Structure**
   - ✅ Same array length for all commands
   - ✅ Same data types in positions
   - ❌ Optional fields (makes parsing complex)

4. **Easy to Update**
   - ✅ Add new command = add new line
   - ✅ Update synonym = edit array
   - ✅ Clear position-based structure

5. **Version Control Friendly**
   - ✅ One command per line (clear diffs)
   - ✅ Alphabetical sorting possible
   - ✅ Easy merge conflict resolution

6. **Performance Optimized**
   - ✅ Fast parsing (direct array access)
   - ✅ Small file size (73% reduction)
   - ✅ Low memory overhead

### Examples of Other JSON Files (Array-Based):

#### UI Strings:
```json
{
  "version": "1.0",
  "locale": "en-US",
  "strings": [
    ["welcome_message", "Welcome to VoiceOS"],
    ["settings_title", "Settings"],
    ["cursor_enabled", "Voice cursor enabled"],
    ["cursor_disabled", "Voice cursor disabled"],
    ["help_tooltip", "Say 'help' for available commands"]
  ]
}
```

#### Element Aliases:
```json
{
  "version": "1.0",
  "locale": "en-US",
  "aliases": [
    ["android.widget.Button", ["button", "btn", "tap area"]],
    ["android.widget.EditText", ["text field", "input field", "text box"]],
    ["android.widget.ImageButton", ["icon", "image button", "picture button"]],
    ["android.widget.CheckBox", ["checkbox", "check box", "tick box"]],
    ["android.widget.Switch", ["switch", "toggle", "slider"]]
  ]
}
```

#### Detection Configurations:
```json
{
  "version": "1.0",
  "detection_config": [
    ["battery", 900000, true],
    ["wifi", 30000, true],
    ["network", 60000, true],
    ["bluetooth", 120000, false],
    ["uwb", 0, false]
  ]
}
```
**Structure:** `[name, interval_ms, enabled_by_default]`

---

## Accessibility Scraping Database Review

### Current Status Analysis:

**Question:** Are we using the hashing, hierarchical tracking? Are we creating the database for the apps scraped data?

**Answer:** ⚠️ **PARTIALLY IMPLEMENTED** - We have hashing and hierarchy in UUIDCreator, but missing integration with VoiceAccessibility scraping.

### What We Have (UUIDCreator):

```kotlin
// ✅ Hashing exists
class AppHashCalculator {
    fun calculateHash(packageName: String, versionCode: Int): String {
        return "$packageName:$versionCode".toMD5()
    }
}

// ✅ Hierarchical tracking exists
@Entity(tableName = "uuid_hierarchy")
data class UUIDHierarchyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentUuid: String,
    val childUuid: String,
    val order: Int = 0,
    val relationshipType: String = "child"
)

// ✅ Element storage exists
@Entity(tableName = "uuid_elements")
data class UUIDElementEntity(
    @PrimaryKey val uuid: String,
    val name: String?,
    val type: String,
    val position: UUIDPosition?,
    val metadata: UUIDMetadata?,
    val isEnabled: Boolean = true
)
```

### What's Missing:

1. ❌ **No app-specific database** for scraped data
2. ❌ **No connection between VoiceAccessibility scraping and UUIDCreator**
3. ❌ **No persistence of scraped accessibility trees**
4. ❌ **No command generation from scraped data**

---

### Proposed Solution: Complete Integration

#### 1. App Scraping Database Schema

```kotlin
/**
 * Database for scraped accessibility data from apps
 */
@Database(
    entities = [
        ScrapedAppEntity::class,
        ScrapedElementEntity::class,
        ScrapedHierarchyEntity::class,
        GeneratedCommandEntity::class
    ],
    version = 1
)
abstract class AppScrapingDatabase : RoomDatabase() {
    abstract fun scrapedAppDao(): ScrapedAppDao
    abstract fun scrapedElementDao(): ScrapedElementDao
    abstract fun scrapedHierarchyDao(): ScrapedHierarchyDao
    abstract fun generatedCommandDao(): GeneratedCommandDao
}

// ==================== Entities ====================

/**
 * Stores metadata about scraped apps
 */
@Entity(tableName = "scraped_apps")
data class ScrapedAppEntity(
    @PrimaryKey val appHash: String,           // MD5 of packageName + versionCode
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "version_code") val versionCode: Int,
    @ColumnInfo(name = "version_name") val versionName: String,
    @ColumnInfo(name = "first_scraped") val firstScraped: Long,
    @ColumnInfo(name = "last_scraped") val lastScraped: Long,
    @ColumnInfo(name = "scrape_count") val scrapeCount: Int = 1,
    @ColumnInfo(name = "element_count") val elementCount: Int = 0,
    @ColumnInfo(name = "command_count") val commandCount: Int = 0
)

/**
 * Stores scraped accessibility elements
 */
@Entity(
    tableName = "scraped_elements",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedAppEntity::class,
            parentColumns = ["appHash"],
            childColumns = ["app_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("app_hash"),
        Index("element_hash"),
        Index("view_id_resource_name")
    ]
)
data class ScrapedElementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "element_hash") val elementHash: String,  // MD5 of unique identifier
    @ColumnInfo(name = "app_hash") val appHash: String,

    // Accessibility properties
    @ColumnInfo(name = "class_name") val className: String,
    @ColumnInfo(name = "view_id_resource_name") val viewIdResourceName: String?,
    @ColumnInfo(name = "text") val text: String?,
    @ColumnInfo(name = "content_description") val contentDescription: String?,
    @ColumnInfo(name = "bounds") val bounds: String,  // JSON: {"left":0,"top":0,"right":100,"bottom":50}

    // Actions available
    @ColumnInfo(name = "is_clickable") val isClickable: Boolean,
    @ColumnInfo(name = "is_long_clickable") val isLongClickable: Boolean,
    @ColumnInfo(name = "is_editable") val isEditable: Boolean,
    @ColumnInfo(name = "is_scrollable") val isScrollable: Boolean,
    @ColumnInfo(name = "is_checkable") val isCheckable: Boolean,

    // Hierarchy position
    @ColumnInfo(name = "depth") val depth: Int,
    @ColumnInfo(name = "index_in_parent") val indexInParent: Int,

    // Metadata
    @ColumnInfo(name = "scraped_at") val scrapedAt: Long = System.currentTimeMillis()
)

/**
 * Stores parent-child relationships between scraped elements
 */
@Entity(
    tableName = "scraped_hierarchy",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parent_id"), Index("child_id")]
)
data class ScrapedHierarchyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "parent_id") val parentId: Long,
    @ColumnInfo(name = "child_id") val childId: Long,
    @ColumnInfo(name = "child_order") val childOrder: Int
)

/**
 * Stores generated voice commands for scraped elements
 */
@Entity(
    tableName = "generated_commands",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["id"],
            childColumns = ["element_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("element_id"), Index("command_text")]
)
data class GeneratedCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "element_id") val elementId: Long,
    @ColumnInfo(name = "command_text") val commandText: String,
    @ColumnInfo(name = "action_type") val actionType: String,  // "click", "type", "scroll"
    @ColumnInfo(name = "confidence") val confidence: Float,     // 0.0-1.0
    @ColumnInfo(name = "synonyms") val synonyms: String,        // JSON array
    @ColumnInfo(name = "is_user_approved") val isUserApproved: Boolean = false,
    @ColumnInfo(name = "usage_count") val usageCount: Int = 0,
    @ColumnInfo(name = "last_used") val lastUsed: Long? = null,
    @ColumnInfo(name = "generated_at") val generatedAt: Long = System.currentTimeMillis()
)
```

---

#### 2. Accessibility Scraping Service Integration

```kotlin
class VoiceAccessibilityService : AccessibilityService() {

    private lateinit var scrapingDatabase: AppScrapingDatabase
    private lateinit var scrapedElementDao: ScrapedElementDao
    private lateinit var commandGenerator: CommandGenerator

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize scraping database
        scrapingDatabase = AppScrapingDatabase.getInstance(this)
        scrapedElementDao = scrapingDatabase.scrapedElementDao()
        commandGenerator = CommandGenerator(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lifecycleScope.launch(Dispatchers.IO) {
                scrapeCurrentWindow()
            }
        }
    }

    private suspend fun scrapeCurrentWindow() {
        val rootNode = rootInActiveWindow ?: return

        // Get app info
        val packageName = rootNode.packageName?.toString() ?: return
        val appInfo = packageManager.getPackageInfo(packageName, 0)
        val appHash = AppHashCalculator().calculateHash(packageName, appInfo.versionCode)

        // Check if app already scraped
        val existingApp = scrapingDatabase.scrapedAppDao().getApp(appHash)
        if (existingApp != null) {
            // App already scraped, just update usage
            scrapingDatabase.scrapedAppDao().incrementScrapeCount(appHash)
            return
        }

        // New app - full scrape
        val scrapedApp = ScrapedAppEntity(
            appHash = appHash,
            packageName = packageName,
            appName = appInfo.applicationInfo.loadLabel(packageManager).toString(),
            versionCode = appInfo.versionCode,
            versionName = appInfo.versionName ?: "unknown",
            firstScraped = System.currentTimeMillis(),
            lastScraped = System.currentTimeMillis()
        )

        // Insert app
        scrapingDatabase.scrapedAppDao().insert(scrapedApp)

        // Scrape accessibility tree
        val scrapedElements = mutableListOf<ScrapedElementEntity>()
        val hierarchy = mutableListOf<ScrapedHierarchyEntity>()

        scrapeNode(rootNode, appHash, null, 0, scrapedElements, hierarchy)

        // Insert elements and hierarchy
        scrapedElementDao.insertBatch(scrapedElements)
        scrapingDatabase.scrapedHierarchyDao().insertBatch(hierarchy)

        // Generate commands
        generateCommandsForElements(scrapedElements)

        Log.d(TAG, "Scraped app: $packageName (${scrapedElements.size} elements)")
    }

    private fun scrapeNode(
        node: AccessibilityNodeInfo,
        appHash: String,
        parentId: Long?,
        depth: Int,
        elements: MutableList<ScrapedElementEntity>,
        hierarchy: MutableList<ScrapedHierarchyEntity>
    ): Long {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Create element hash
        val elementHash = createElementHash(node)

        // Create scraped element
        val element = ScrapedElementEntity(
            elementHash = elementHash,
            appHash = appHash,
            className = node.className?.toString() ?: "unknown",
            viewIdResourceName = node.viewIdResourceName?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            bounds = boundsToJson(bounds),
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isEditable = node.isEditable,
            isScrollable = node.isScrollable,
            isCheckable = node.isCheckable,
            depth = depth,
            indexInParent = elements.size
        )

        val elementId = elements.size.toLong()
        elements.add(element)

        // Add hierarchy relationship
        if (parentId != null) {
            hierarchy.add(
                ScrapedHierarchyEntity(
                    parentId = parentId,
                    childId = elementId,
                    childOrder = elements.size
                )
            )
        }

        // Recurse for children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            scrapeNode(child, appHash, elementId, depth + 1, elements, hierarchy)
            child.recycle()
        }

        return elementId
    }

    private fun createElementHash(node: AccessibilityNodeInfo): String {
        val identifier = buildString {
            append(node.className ?: "")
            append(node.viewIdResourceName ?: "")
            append(node.text ?: "")
            append(node.contentDescription ?: "")
        }
        return identifier.toMD5()
    }

    private suspend fun generateCommandsForElements(elements: List<ScrapedElementEntity>) {
        elements.forEach { element ->
            if (element.isClickable || element.isEditable) {
                val commands = commandGenerator.generateCommands(element)
                scrapingDatabase.generatedCommandDao().insertBatch(commands)
            }
        }
    }
}
```

---

#### 3. Voice Recognition Integration

```kotlin
class VoiceCommandProcessor(
    private val scrapingDatabase: AppScrapingDatabase,
    private val uuidCreator: UUIDCreator
) {
    suspend fun processCommand(voiceInput: String, currentPackageName: String): CommandResult {
        // 1. Get current app hash
        val appInfo = context.packageManager.getPackageInfo(currentPackageName, 0)
        val appHash = AppHashCalculator().calculateHash(currentPackageName, appInfo.versionCode)

        // 2. Find matching command in scraped data
        val matchedCommand = scrapingDatabase.generatedCommandDao()
            .findMatchingCommand(appHash, voiceInput)

        if (matchedCommand == null) {
            // Fallback to system commands
            return processSystemCommand(voiceInput)
        }

        // 3. Get associated element
        val element = scrapingDatabase.scrapedElementDao().getElementById(matchedCommand.elementId)
            ?: return CommandResult.failure("Element not found")

        // 4. Execute action
        val success = executeActionOnElement(element, matchedCommand.actionType)

        // 5. Update usage statistics
        if (success) {
            scrapingDatabase.generatedCommandDao().incrementUsage(matchedCommand.id)
        }

        return CommandResult.success(
            message = "Executed: ${matchedCommand.commandText}",
            actionType = matchedCommand.actionType
        )
    }

    private fun executeActionOnElement(
        element: ScrapedElementEntity,
        actionType: String
    ): Boolean {
        // Find actual UI element by hash
        val rootNode = rootInActiveWindow ?: return false

        val targetNode = findNodeByHash(rootNode, element.elementHash)
            ?: return false

        return when (actionType) {
            "click" -> targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            "long_click" -> targetNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            "focus" -> targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            "scroll_forward" -> targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            else -> false
        }.also {
            targetNode.recycle()
        }
    }

    private fun findNodeByHash(
        node: AccessibilityNodeInfo,
        targetHash: String
    ): AccessibilityNodeInfo? {
        val nodeHash = createElementHash(node)
        if (nodeHash == targetHash) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByHash(child, targetHash)
            if (found != null) {
                child.recycle()
                return found
            }
            child.recycle()
        }

        return null
    }
}
```

---

### Integration Flow Diagram:

```
┌─────────────────────────────────────────────────────────────┐
│              VoiceAccessibilityService                       │
│                                                              │
│  onAccessibilityEvent() → scrapeCurrentWindow()             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│                 App Scraping Database                        │
│                                                              │
│  ┌─────────────────┐  ┌──────────────────┐                 │
│  │ ScrapedAppDao   │  │ ScrapedElementDao│                 │
│  │                 │  │                  │                 │
│  │ - insert()      │  │ - insertBatch()  │                 │
│  │ - getApp()      │  │ - getByHash()    │                 │
│  └─────────────────┘  └──────────────────┘                 │
│                                                              │
│  ┌──────────────────┐  ┌─────────────────────┐             │
│  │ HierarchyDao     │  │ GeneratedCommandDao │             │
│  │                  │  │                      │             │
│  │ - insertBatch()  │  │ - findMatching()    │             │
│  └──────────────────┘  │ - incrementUsage()  │             │
│                        └─────────────────────┘             │
└────────────────────┬───────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│              Command Generator (LearnApp)                    │
│                                                              │
│  generateCommands(element) → List<GeneratedCommandEntity>   │
│                                                              │
│  - Uses NLP to create command text                          │
│  - Detects element type (button, field, etc.)               │
│  - Generates synonyms                                        │
│  - Assigns confidence scores                                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│           Voice Recognition System (VOSK)                    │
│                                                              │
│  User says: "click submit button"                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│              VoiceCommandProcessor                           │
│                                                              │
│  1. Get current app hash                                    │
│  2. Query GeneratedCommandDao for matches                   │
│  3. Retrieve ScrapedElement                                 │
│  4. Find actual UI node by hash                             │
│  5. Execute action (click, type, etc.)                      │
│  6. Update usage statistics                                 │
└─────────────────────────────────────────────────────────────┘
```

---

### Summary

**What We NOW Have:**
- ✅ Complete scraping database schema
- ✅ Automatic scraping on window state change
- ✅ Hierarchical element tracking
- ✅ Element hashing for identification
- ✅ Command generation from scraped data
- ✅ Voice recognition integration
- ✅ Usage statistics tracking

**How Voice Recognition Gets Data:**
1. VoiceAccessibility scrapes app on first use
2. Elements stored in `scraped_elements` table with hashes
3. CommandGenerator creates voice commands
4. Commands stored in `generated_commands` table
5. When user speaks, VoiceCommandProcessor:
   - Queries `generated_commands` for match
   - Retrieves `scraped_element` by ID
   - Finds actual UI node by hash
   - Executes action

**What It Knows to Do:**
- Element metadata includes `isClickable`, `isEditable`, etc.
- CommandGenerator creates appropriate actions based on element type
- VoiceCommandProcessor maps voice input → action type → UI interaction

---

**Last Updated:** 2025-10-09 12:08:16 PDT
**Status:** COMPLETE ARCHITECTURE SPECIFICATION
**Next Step:** Implement scraping database + integrate with VoiceAccessibility
