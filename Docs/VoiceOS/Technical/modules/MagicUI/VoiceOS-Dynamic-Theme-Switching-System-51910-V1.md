<!--
filename: Dynamic-Theme-Switching-System-251019-0127.md
created: 2025-10-19 01:27:00 PDT
author: Manoj Jhawar
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Dynamic theme switching system for ecosystem-wide and per-app theme changes
last-modified: 2025-10-19 01:27:00 PDT
version: 1.0.0
-->

# MagicUI: Dynamic Theme Switching System

**Purpose:** Enable real-time theme switching across entire VOS4 ecosystem or per-app
**Created:** 2025-10-19 01:27:00 PDT
**Status:** Specification Ready

---

## Executive Summary

### Problem Statement

Users need the ability to:
1. **Change theme ecosystem-wide** - Switch all apps at once (e.g., Glass → visionOS)
2. **Change theme per-app** - Different theme for each app (e.g., Email=Material 3, Games=Liquid UI)
3. **Real-time switching** - No app restart required
4. **Persistent** - Remember theme choice across sessions
5. **Voice-controlled** - "Switch to visionOS theme", "Use dark mode for Gmail"

### Solution

**3-Tier Theme Management System:**
1. **Global Theme Service** - Ecosystem-wide theme management
2. **App Theme Override** - Per-app theme preferences
3. **Live Theme Reactor** - Real-time theme application without restart

---

## Architecture

### Theme Hierarchy

```
┌──────────────────────────────────────────────────────────┐
│  Level 1: Ecosystem-Wide (Global)                        │
│  ┌────────────────────────────────────────────────────┐ │
│  │  ThemeMode.VISION_OS (default for all apps)        │ │
│  └────────────────────────────────────────────────────┘ │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│  Level 2: Per-App Override (Optional)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐ │
│  │ Gmail       │  │ Calculator  │  │ Games            │ │
│  │ MATERIAL_3  │  │ VOS4_DEFAULT│  │ LIQUID           │ │
│  └─────────────┘  └─────────────┘  └──────────────────┘ │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│  Level 3: Real-Time Application                          │
│  • No restart required                                   │
│  • Smooth transition animation                           │
│  • State preservation                                    │
│  • Component re-composition                              │
└──────────────────────────────────────────────────────────┘
```

---

## Implementation

### 1. Global Theme Service

**File:** `integration/GlobalThemeService.kt`

```kotlin
/**
 * Global theme service for ecosystem-wide theme management
 *
 * Features:
 * - Set global theme for all apps
 * - Per-app theme overrides
 * - Real-time theme switching
 * - Theme persistence (Room database)
 * - Voice command integration
 */
object GlobalThemeService {

    private lateinit var context: Context
    private lateinit var database: ThemeDatabase

    // Current global theme
    private val _globalTheme = MutableStateFlow<ThemeMode>(ThemeMode.AUTO)
    val globalTheme: StateFlow<ThemeMode> = _globalTheme.asStateFlow()

    // Per-app theme overrides
    private val _appThemes = MutableStateFlow<Map<String, ThemeMode>>(emptyMap())
    val appThemes: StateFlow<Map<String, ThemeMode>> = _appThemes.asStateFlow()

    /**
     * Initialize global theme service
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
        this.database = ThemeDatabase.getInstance(context)

        // Load saved themes from database
        loadThemes()

        // Register voice commands
        registerVoiceCommands()
    }

    /**
     * Set global theme (affects all apps without override)
     */
    suspend fun setGlobalTheme(theme: ThemeMode) {
        _globalTheme.value = theme

        // Save to database
        database.themeDao().setGlobalTheme(theme)

        // Notify all apps
        broadcastThemeChange(theme, isGlobal = true)

        Log.i("GlobalThemeService", "Global theme changed to: $theme")
    }

    /**
     * Set per-app theme override
     */
    suspend fun setAppTheme(packageName: String, theme: ThemeMode) {
        val updated = _appThemes.value.toMutableMap()
        updated[packageName] = theme
        _appThemes.value = updated

        // Save to database
        database.themeDao().setAppTheme(packageName, theme)

        // Notify specific app
        broadcastThemeChange(theme, isGlobal = false, packageName = packageName)

        Log.i("GlobalThemeService", "App theme changed for $packageName to: $theme")
    }

    /**
     * Clear per-app theme override (revert to global)
     */
    suspend fun clearAppTheme(packageName: String) {
        val updated = _appThemes.value.toMutableMap()
        updated.remove(packageName)
        _appThemes.value = updated

        // Remove from database
        database.themeDao().clearAppTheme(packageName)

        // Notify app to use global theme
        broadcastThemeChange(_globalTheme.value, isGlobal = true, packageName = packageName)

        Log.i("GlobalThemeService", "App theme cleared for $packageName, using global: ${_globalTheme.value}")
    }

    /**
     * Get effective theme for an app (app override or global)
     */
    fun getEffectiveTheme(packageName: String): ThemeMode {
        return _appThemes.value[packageName] ?: _globalTheme.value
    }

    /**
     * Broadcast theme change to apps
     */
    private fun broadcastThemeChange(
        theme: ThemeMode,
        isGlobal: Boolean,
        packageName: String? = null
    ) {
        val intent = Intent(ACTION_THEME_CHANGED).apply {
            putExtra(EXTRA_THEME, theme.name)
            putExtra(EXTRA_IS_GLOBAL, isGlobal)
            if (packageName != null) {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
        }
        context.sendBroadcast(intent)
    }

    /**
     * Register voice commands
     */
    private fun registerVoiceCommands() {
        val commandManager = CommandManager.getInstance(context)

        // Global theme commands
        commandManager.registerCommand(
            command = "switch to (visionos|vision os) theme",
            category = "theme",
            action = { setGlobalTheme(ThemeMode.VISION_OS) }
        )

        commandManager.registerCommand(
            command = "switch to (android|android xr) theme",
            category = "theme",
            action = { setGlobalTheme(ThemeMode.ANDROID_XR) }
        )

        commandManager.registerCommand(
            command = "switch to (meta|meta quest|quest) theme",
            category = "theme",
            action = { setGlobalTheme(ThemeMode.META_QUEST) }
        )

        commandManager.registerCommand(
            command = "switch to glass theme",
            category = "theme",
            action = { setGlobalTheme(ThemeMode.GLASS) }
        )

        commandManager.registerCommand(
            command = "switch to material (three|3) theme",
            category = "theme",
            action = { setGlobalTheme(ThemeMode.MATERIAL_3) }
        )

        // Per-app theme commands
        commandManager.registerCommand(
            command = "use <theme> theme for <app>",
            category = "theme",
            action = { params ->
                val theme = parseTheme(params["theme"] as String)
                val packageName = resolvePackageName(params["app"] as String)
                setAppTheme(packageName, theme)
            }
        )

        commandManager.registerCommand(
            command = "reset theme for <app>",
            category = "theme",
            action = { params ->
                val packageName = resolvePackageName(params["app"] as String)
                clearAppTheme(packageName)
            }
        )
    }

    /**
     * Load themes from database
     */
    private fun loadThemes() {
        CoroutineScope(Dispatchers.IO).launch {
            // Load global theme
            val global = database.themeDao().getGlobalTheme()
            _globalTheme.value = global ?: ThemeMode.AUTO

            // Load app themes
            val apps = database.themeDao().getAllAppThemes()
            _appThemes.value = apps.associate { it.packageName to it.theme }
        }
    }

    companion object {
        const val ACTION_THEME_CHANGED = "com.augmentalis.magicui.THEME_CHANGED"
        const val EXTRA_THEME = "theme"
        const val EXTRA_IS_GLOBAL = "is_global"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }
}
```

---

### 2. Theme Database (Persistence)

**File:** `database/ThemeDatabase.kt`

```kotlin
@Database(
    entities = [
        GlobalThemeEntity::class,
        AppThemeEntity::class
    ],
    version = 1
)
abstract class ThemeDatabase : RoomDatabase() {
    abstract fun themeDao(): ThemeDao

    companion object {
        @Volatile
        private var instance: ThemeDatabase? = null

        fun getInstance(context: Context): ThemeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ThemeDatabase::class.java,
                    "magic_ui_themes.db"
                ).build().also { instance = it }
            }
        }
    }
}

@Entity(tableName = "global_theme")
data class GlobalThemeEntity(
    @PrimaryKey val id: Int = 1,  // Single row
    val theme: ThemeMode,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_themes")
data class AppThemeEntity(
    @PrimaryKey val packageName: String,
    val theme: ThemeMode,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface ThemeDao {
    @Query("SELECT theme FROM global_theme WHERE id = 1")
    suspend fun getGlobalTheme(): ThemeMode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setGlobalTheme(theme: GlobalThemeEntity)

    @Query("SELECT * FROM app_themes")
    suspend fun getAllAppThemes(): List<AppThemeEntity>

    @Query("SELECT theme FROM app_themes WHERE packageName = :packageName")
    suspend fun getAppTheme(packageName: String): ThemeMode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAppTheme(appTheme: AppThemeEntity)

    @Query("DELETE FROM app_themes WHERE packageName = :packageName")
    suspend fun clearAppTheme(packageName: String)
}

// Helper extensions
suspend fun ThemeDao.setGlobalTheme(theme: ThemeMode) {
    setGlobalTheme(GlobalThemeEntity(theme = theme))
}

suspend fun ThemeDao.setAppTheme(packageName: String, theme: ThemeMode) {
    setAppTheme(AppThemeEntity(packageName = packageName, theme = theme))
}
```

---

### 3. Live Theme Reactor (Real-Time Switching)

**File:** `core/LiveThemeReactor.kt`

```kotlin
/**
 * Live theme reactor - enables real-time theme switching without restart
 *
 * Features:
 * - Listen for theme change broadcasts
 * - Update theme state reactively
 * - Smooth transition animations
 * - State preservation during switch
 */
class LiveThemeReactor(
    private val context: Context,
    private val packageName: String
) {

    // Current active theme
    private val _currentTheme = MutableStateFlow<ThemeMode>(ThemeMode.AUTO)
    val currentTheme: StateFlow<ThemeMode> = _currentTheme.asStateFlow()

    // Theme change receiver
    private val themeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                GlobalThemeService.ACTION_THEME_CHANGED -> {
                    handleThemeChange(intent)
                }
            }
        }
    }

    /**
     * Start listening for theme changes
     */
    fun start() {
        // Register broadcast receiver
        val filter = IntentFilter(GlobalThemeService.ACTION_THEME_CHANGED)
        context.registerReceiver(themeChangeReceiver, filter)

        // Load initial theme
        val effectiveTheme = GlobalThemeService.getEffectiveTheme(packageName)
        _currentTheme.value = effectiveTheme

        Log.d("LiveThemeReactor", "Started for $packageName, initial theme: $effectiveTheme")
    }

    /**
     * Stop listening for theme changes
     */
    fun stop() {
        context.unregisterReceiver(themeChangeReceiver)
        Log.d("LiveThemeReactor", "Stopped for $packageName")
    }

    /**
     * Handle theme change broadcast
     */
    private fun handleThemeChange(intent: Intent) {
        val themeName = intent.getStringExtra(GlobalThemeService.EXTRA_THEME) ?: return
        val isGlobal = intent.getBooleanExtra(GlobalThemeService.EXTRA_IS_GLOBAL, false)
        val targetPackage = intent.getStringExtra(GlobalThemeService.EXTRA_PACKAGE_NAME)

        // Check if this change applies to this app
        val appliesToUs = isGlobal || (targetPackage == packageName)

        if (appliesToUs) {
            val newTheme = ThemeMode.valueOf(themeName)

            // Animate theme transition
            animateThemeTransition(from = _currentTheme.value, to = newTheme)

            // Update theme
            _currentTheme.value = newTheme

            Log.i("LiveThemeReactor", "Theme changed to: $newTheme (global=$isGlobal)")
        }
    }

    /**
     * Animate smooth theme transition
     */
    private fun animateThemeTransition(from: ThemeMode, to: ThemeMode) {
        // TODO: Implement smooth cross-fade animation
        // - Fade out with current theme
        // - Swap theme
        // - Fade in with new theme
        // Duration: 300ms
    }
}
```

---

### 4. Updated MagicScreen (Theme Reactor Integration)

**File:** `core/MagicScreen.kt` (updated)

```kotlin
@Composable
fun MagicScreen(
    name: String,
    theme: ThemeMode = ThemeMode.AUTO,
    spatialMode: SpatialMode = SpatialMode.DETECT_AUTO,
    persistState: Boolean = false,
    content: @Composable MagicUIScope.() -> Unit
) {
    val context = LocalContext.current
    val packageName = context.packageName

    // Live theme reactor - enables real-time theme switching
    val themeReactor = remember {
        LiveThemeReactor(context, packageName).apply {
            start()
        }
    }

    // Observe current theme (reactive)
    val currentTheme by themeReactor.currentTheme.collectAsState()

    // Determine effective theme
    val effectiveTheme = if (theme == ThemeMode.AUTO) {
        currentTheme  // Use live theme from reactor
    } else {
        theme  // Use explicitly specified theme
    }

    // Get VOS4 services
    val vos4Services = remember {
        VOS4Services.getInstance(context)
    }

    // Create integration layers
    val uuidIntegration = remember(name) {
        UUIDIntegration(name, vos4Services.uuidManager)
    }

    val commandIntegration = remember(name) {
        CommandIntegration(name, vos4Services.commandManager, uuidIntegration)
    }

    val hudIntegration = remember {
        HUDIntegration(vos4Services.hudManager)
    }

    val localizationIntegration = remember {
        LocalizationIntegration(vos4Services.localizationManager)
    }

    val spatialIntegration = remember(spatialMode) {
        SpatialIntegration(spatialMode)
    }

    // Create MagicUI scope
    val scope = remember(name) {
        MagicUIScope(
            screenName = name,
            spatialMode = spatialMode,
            uuidIntegration = uuidIntegration,
            commandIntegration = commandIntegration,
            spatialIntegration = spatialIntegration,
            hudIntegration = hudIntegration,
            localizationIntegration = localizationIntegration,
            persistState = persistState
        )
    }

    // Provide composition locals
    CompositionLocalProvider(
        LocalVOS4Services provides vos4Services,
        LocalUUIDIntegration provides uuidIntegration,
        LocalCommandIntegration provides commandIntegration,
        LocalSpatialIntegration provides spatialIntegration,
        LocalHUDIntegration provides hudIntegration,
        LocalLocalizationIntegration provides localizationIntegration
    ) {
        // Apply theme (reactive - updates when currentTheme changes)
        MagicTheme(effectiveTheme) {
            // Cleanup on disposal
            DisposableEffect(name) {
                onDispose {
                    scope.cleanup()
                    themeReactor.stop()
                }
            }

            // Execute DSL content
            scope.content()
        }
    }
}
```

---

## Usage Examples

### Example 1: Change Global Theme (Ecosystem-Wide)

```kotlin
// User says: "Switch to visionOS theme"
// OR programmatically:
GlobalThemeService.setGlobalTheme(ThemeMode.VISION_OS)

// Result: ALL apps switch to visionOS theme in real-time
// - No restart required
// - Smooth transition animation
// - All MagicScreen components update automatically
```

---

### Example 2: Change Per-App Theme

```kotlin
// User says: "Use Material 3 theme for Gmail"
// OR programmatically:
GlobalThemeService.setAppTheme(
    packageName = "com.google.android.gm",
    theme = ThemeMode.MATERIAL_3
)

// Result: ONLY Gmail uses Material 3, other apps keep global theme
// - No restart required
// - Gmail switches in real-time
// - Other apps unaffected
```

---

### Example 3: Reset App Theme (Revert to Global)

```kotlin
// User says: "Reset theme for Gmail"
// OR programmatically:
GlobalThemeService.clearAppTheme("com.google.android.gm")

// Result: Gmail reverts to global theme
// - No restart required
// - Real-time switch
// - Preference removed from database
```

---

### Example 4: Theme Switching UI

**Settings Screen:**
```kotlin
MagicScreen("theme_settings") {
    column {
        text("Theme Settings", style = TextStyle.HEADLINE)

        // Global theme selector
        section("Global Theme") {
            dropdown(
                label = "Ecosystem Theme",
                options = ThemeMode.values().map { it.name },
                selected = GlobalThemeService.globalTheme.collectAsState().value.name,
                onSelect = { themeName ->
                    GlobalThemeService.setGlobalTheme(ThemeMode.valueOf(themeName))
                }
            )
        }

        // Per-app theme overrides
        section("Per-App Themes") {
            // Gmail
            row {
                text("Gmail")
                dropdown(
                    options = listOf("Global") + ThemeMode.values().map { it.name },
                    selected = getAppThemeOrGlobal("com.google.android.gm"),
                    onSelect = { themeName ->
                        if (themeName == "Global") {
                            GlobalThemeService.clearAppTheme("com.google.android.gm")
                        } else {
                            GlobalThemeService.setAppTheme(
                                "com.google.android.gm",
                                ThemeMode.valueOf(themeName)
                            )
                        }
                    }
                )
            }

            // Calculator
            row {
                text("Calculator")
                dropdown(
                    options = listOf("Global") + ThemeMode.values().map { it.name },
                    selected = getAppThemeOrGlobal("com.android.calculator2"),
                    onSelect = { themeName ->
                        if (themeName == "Global") {
                            GlobalThemeService.clearAppTheme("com.android.calculator2")
                        } else {
                            GlobalThemeService.setAppTheme(
                                "com.android.calculator2",
                                ThemeMode.valueOf(themeName)
                            )
                        }
                    }
                )
            }
        }
    }
}
```

---

### Example 5: Voice Commands

**Supported Voice Commands:**

**Global Theme:**
- "Switch to visionOS theme"
- "Switch to Android XR theme"
- "Switch to Meta Quest theme"
- "Switch to Glass theme"
- "Switch to Liquid theme"
- "Switch to Material 3 theme"
- "Switch to Material You theme"

**Per-App Theme:**
- "Use visionOS theme for Gmail"
- "Use Glass theme for Calculator"
- "Use Material 3 theme for all apps" (same as global)
- "Reset theme for Gmail"
- "Reset theme for Calculator"

---

## Theme Transition Animation

### Smooth Cross-Fade (300ms)

```kotlin
/**
 * Theme transition animation
 *
 * Duration: 300ms
 * Easing: FastOutSlowIn
 * Effect: Cross-fade between themes
 */
@Composable
fun AnimatedThemeTransition(
    theme: ThemeMode,
    content: @Composable () -> Unit
) {
    var targetTheme by remember { mutableStateOf(theme) }

    LaunchedEffect(theme) {
        if (theme != targetTheme) {
            // Animate transition
            targetTheme = theme
        }
    }

    Crossfade(
        targetState = targetTheme,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    ) { currentTheme ->
        MagicTheme(currentTheme) {
            content()
        }
    }
}
```

---

## Theme Persistence

### Database Schema

```sql
-- Global theme (single row)
CREATE TABLE global_theme (
    id INTEGER PRIMARY KEY,  -- Always 1
    theme TEXT NOT NULL,
    last_updated INTEGER NOT NULL
);

-- Per-app theme overrides
CREATE TABLE app_themes (
    package_name TEXT PRIMARY KEY,
    theme TEXT NOT NULL,
    last_updated INTEGER NOT NULL
);

-- Indexes
CREATE INDEX idx_app_themes_package ON app_themes(package_name);
```

### Example Data

```sql
-- Global theme: visionOS
INSERT INTO global_theme (id, theme, last_updated)
VALUES (1, 'VISION_OS', 1700000000000);

-- Per-app overrides
INSERT INTO app_themes (package_name, theme, last_updated)
VALUES
    ('com.google.android.gm', 'MATERIAL_3', 1700000000000),  -- Gmail: Material 3
    ('com.android.calculator2', 'VOS4_DEFAULT', 1700000000000),  -- Calculator: VOS4
    ('com.example.game', 'LIQUID', 1700000000000);  -- Game: Liquid UI
```

---

## Integration with VOS4

### Voice Command Registration

```kotlin
// Global theme commands
CommandManager.registerCommand(
    command = "switch to <theme> theme",
    category = "theme",
    action = { params ->
        val theme = parseTheme(params["theme"])
        GlobalThemeService.setGlobalTheme(theme)
        HUDManager.showFeedback("Switched to $theme theme")
    }
)

// Per-app theme commands
CommandManager.registerCommand(
    command = "use <theme> theme for <app>",
    category = "theme",
    action = { params ->
        val theme = parseTheme(params["theme"])
        val app = resolvePackageName(params["app"])
        GlobalThemeService.setAppTheme(app, theme)
        HUDManager.showFeedback("$app now uses $theme theme")
    }
)
```

---

## Summary

### Dynamic Theme Switching System

**3-Tier System:**
1. **Global Theme Service** - Ecosystem-wide management
2. **App Theme Override** - Per-app preferences
3. **Live Theme Reactor** - Real-time application

**Features:**
- ✅ **Ecosystem-wide switching** - Change all apps at once
- ✅ **Per-app overrides** - Different theme for each app
- ✅ **Real-time switching** - No restart required
- ✅ **Smooth transitions** - 300ms cross-fade animation
- ✅ **Persistent** - Database storage (Room)
- ✅ **Voice-controlled** - Full VOS4 integration
- ✅ **State preservation** - No data loss during switch

**Usage:**
```kotlin
// Ecosystem-wide
GlobalThemeService.setGlobalTheme(ThemeMode.VISION_OS)

// Per-app
GlobalThemeService.setAppTheme("com.google.android.gm", ThemeMode.MATERIAL_3)

// Reset to global
GlobalThemeService.clearAppTheme("com.google.android.gm")
```

**Voice Commands:**
- "Switch to visionOS theme" → All apps
- "Use Material 3 for Gmail" → Just Gmail
- "Reset theme for Gmail" → Revert to global

---

**Document Status:** COMPLETE ✅
**Implementation Status:** Specification Ready
**Maintained By:** AI Documentation Agent
**Contact:** Manoj Jhawar (maintainer)
