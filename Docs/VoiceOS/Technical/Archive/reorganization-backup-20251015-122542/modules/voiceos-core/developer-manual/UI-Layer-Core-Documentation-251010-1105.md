# VoiceAccessibility UI Layer - Core Documentation

**Module:** VoiceAccessibility
**Layer:** User Interface - Core Components
**Created:** 2025-10-10 11:05:00 PDT
**Author:** VOS4 Development Team
**Status:** Complete

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Main Activities](#main-activities)
4. [Dashboard Screens](#dashboard-screens)
5. [Settings Screens](#settings-screens)
6. [Jetpack Compose Integration](#jetpack-compose-integration)
7. [UI Components](#ui-components)
8. [Theme System](#theme-system)
9. [Utilities](#utilities)
10. [Integration Points](#integration-points)
11. [Best Practices](#best-practices)

---

## Overview

### Purpose

The VoiceAccessibility UI Layer provides the user-facing interface for the accessibility service, built entirely with Jetpack Compose and using Material Design 3 with glassmorphism styling. This layer handles all user interactions, configuration, and visual feedback.

### Key Features

- **Jetpack Compose UI**: 100% Compose-based interface with Material Design 3
- **Glassmorphism Design**: Consistent with VoiceOS SRS design language
- **Activity-based Navigation**: Traditional Android activities for main screens
- **Dark Theme**: Optimized for low-light accessibility
- **Reactive State Management**: LiveData and StateFlow for real-time updates
- **Accessibility-First**: Designed for users with mobility challenges

### File Locations

**Main Activities:**
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/MainActivity.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/AccessibilitySettings.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceaccessibility/ui/LearnAppActivity.kt`

**Composables:**
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/AccessibilityDashboard.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/ConfidenceIndicator.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceaccessibility/ui/components/FloatingEngineSelector.kt`

**Theme:**
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/theme/Theme.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/theme/Type.kt`

**Utilities:**
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/utils/ThemeUtils.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/utils/DisplayUtils.kt`

---

## Architecture

### UI Layer Structure

```
UI Layer
├── Activities (Entry Points)
│   ├── MainActivity - Main app screen
│   ├── AccessibilitySettings - Configuration screen
│   └── LearnAppActivity - App learning UI
│
├── Composables (Screens)
│   ├── MainScreen - Main dashboard
│   ├── SettingsScreen - Settings interface
│   ├── AccessibilityDashboard - Status overview
│   └── LearnAppScreen - App learning flow
│
├── Components (Reusable)
│   ├── ConfidenceIndicator - Speech confidence
│   ├── FloatingEngineSelector - Engine switcher
│   └── Various UI components
│
├── Theme
│   ├── AccessibilityTheme - Material3 theme
│   ├── Color schemes - Dark/light modes
│   └── Typography - Text styles
│
└── Utilities
    ├── ThemeUtils - Glassmorphism effects
    └── DisplayUtils - Display metrics
```

### Component Hierarchy

```
ComponentActivity
└── MainActivity
    ├── MainScreen (@Composable)
    │   ├── HeaderSection
    │   ├── ServiceStatusCard
    │   ├── QuickStatsCard
    │   ├── NavigationCards
    │   └── FloatingEngineSelector
    │
    └── AccessibilityTheme (wraps all)
```

### Data Flow

```
ViewModel → LiveData/StateFlow → Composable → UI Update
    ↑                                  ↓
    └──────── User Interaction ────────┘
```

---

## Main Activities

### MainActivity

**File:** `MainActivity.kt`
**Purpose:** Primary entry point for the app, displays service status and navigation options.

#### Class Definition

```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    // Activity result launchers for permissions
    private val accessibilitySettingsLauncher: ActivityResultLauncher<Intent>
    private val overlayPermissionLauncher: ActivityResultLauncher<Intent>
}
```

#### Lifecycle

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 1. Initialize ViewModel
    viewModel.initialize(this)

    // 2. Set Compose content
    setContent {
        AccessibilityTheme {
            MainScreen(
                viewModel = viewModel,
                onNavigateToSettings = ::navigateToAccessibilitySettings,
                onNavigateToTesting = ::navigateToTesting,
                onRequestOverlayPermission = ::requestOverlayPermission
            )
        }
    }

    // 3. Check permissions asynchronously
    lifecycleScope.launch {
        delay(500) // Allow UI to initialize
        viewModel.checkAllPermissions()
    }
}
```

#### Permission Handling

**Accessibility Service Permission:**
```kotlin
private fun openAccessibilitySettings() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    accessibilitySettingsLauncher.launch(intent)
}
```

**Overlay Permission:**
```kotlin
private fun requestOverlayPermission() {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
        data = Uri.parse("package:$packageName")
    }
    overlayPermissionLauncher.launch(intent)
}
```

#### Key Composables

**MainScreen:**
```kotlin
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToTesting: () -> Unit,
    onRequestOverlayPermission: () -> Unit
)
```

**Observed State:**
```kotlin
val serviceEnabled by viewModel.serviceEnabled.observeAsState(false)
val overlayPermissionGranted by viewModel.overlayPermissionGranted.observeAsState(false)
val configuration by viewModel.configuration.observeAsState(ServiceConfiguration.createDefault())
val selectedEngine by viewModel.selectedEngine.observeAsState("vivoka")
val isRecognizing by viewModel.isRecognizing.observeAsState(false)
```

---

### AccessibilitySettings

**File:** `AccessibilitySettings.kt`
**Purpose:** Configuration screen for service settings, handlers, and performance options.

#### Class Definition

```kotlin
class AccessibilitySettings : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
}
```

#### Screen Structure

```kotlin
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    // Layout structure:
    Column {
        SettingsTopBar()
        LazyColumn {
            item { ServiceControlSection() }
            item { HandlerTogglesSection() }
            item { PerformanceSettingsSection() }
            item { CursorSettingsSection() }
            item { AdvancedSettingsSection() }
        }
    }
}
```

#### Settings Sections

**1. Service Control Section:**
- Service enabled/disabled status
- Show toast notifications toggle
- Verbose logging toggle

```kotlin
@Composable
fun ServiceControlSection(settingsViewModel: SettingsViewModel) {
    SettingsSection(title = "Service Control", icon = Icons.Default.Power) {
        SettingsToggle(
            title = "Service Enabled",
            description = "Enable/disable the accessibility service",
            isChecked = configuration.isEnabled,
            onCheckedChange = { /* Handled by system */ }
        )
        // ... additional toggles
    }
}
```

**2. Handler Toggles Section:**
- Enable/disable individual command handlers
- Core handlers cannot be disabled
- Visual status for each handler

```kotlin
@Composable
fun HandlerTogglesSection(
    settingsViewModel: SettingsViewModel,
    handlerStates: Map<String, Boolean>
) {
    SettingsSection(title = "Command Handlers", icon = Icons.Default.Tune) {
        settingsViewModel.getAllHandlerDefinitions().forEach { handler ->
            SettingsToggle(
                title = handler.name,
                description = handler.description,
                isChecked = handlerStates[handler.id] ?: false,
                enabled = !handler.isCore,
                onCheckedChange = { enabled ->
                    settingsViewModel.toggleHandler(handler.id, enabled)
                }
            )
        }
    }
}
```

**3. Performance Settings Section:**
- Performance mode selection (High Performance, Balanced, Power Saver)
- Dynamic commands toggle
- Command caching configuration

```kotlin
@Composable
fun PerformanceSettingsSection(
    settingsViewModel: SettingsViewModel,
    performanceMode: PerformanceMode
) {
    SettingsSection(title = "Performance", icon = Icons.Default.Speed) {
        // Radio button selection for performance modes
        PerformanceMode.values().forEach { mode ->
            RadioButton(
                selected = performanceMode == mode,
                onClick = { settingsViewModel.updatePerformanceMode(mode) }
            )
        }

        // Cache settings
        SettingsSlider(
            title = "Cache Size",
            value = maxCacheSize.toFloat(),
            range = 10f..500f,
            onValueChange = { settingsViewModel.updateMaxCacheSize(it.toInt()) }
        )
    }
}
```

**4. Cursor Settings Section:**
- Voice cursor enable/disable
- Cursor size adjustment
- Cursor speed multiplier

**5. Advanced Settings Section:**
- UI scraping experimental features
- Fingerprint gestures
- Reset to defaults button

---

### LearnAppActivity

**File:** `LearnAppActivity.kt`
**Purpose:** Provides UI for triggering comprehensive app learning via LearnApp mode.

#### Overview

This activity allows users to select installed apps and trigger full UI traversal to discover all interactive elements for voice control.

#### Key Data Classes

```kotlin
data class AppInfo(
    val appName: String,
    val packageName: String
)
```

#### Main Screen

```kotlin
@Composable
fun LearnAppScreen(
    packageManager: PackageManager,
    database: AppScrapingDatabase,
    scrapingIntegration: AccessibilityScrapingIntegration?
) {
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var scrapedApps by remember { mutableStateOf<List<ScrapedAppEntity>>(emptyList()) }
    var learningInProgress by remember { mutableStateOf(false) }

    // Load data and display app list
}
```

#### App Learning Flow

```kotlin
// 1. User selects an app from the list
AppCard(
    appInfo = appInfo,
    scrapedApp = scrapedApp,
    isLearning = learningInProgress,
    onLearnClick = {
        scope.launch {
            val result = scrapingIntegration.learnApp(appInfo.packageName)
            // Update UI with result
        }
    }
)

// 2. System traverses app UI
// 3. Results displayed showing elements discovered
ResultCard(result = result)
```

---

## Dashboard Screens

### AccessibilityDashboard

**File:** `AccessibilityDashboard.kt`
**Purpose:** Comprehensive dashboard with service status, stats, and navigation.

#### Main Composable

```kotlin
@Composable
fun AccessibilityDashboard(
    serviceEnabled: Boolean,
    configuration: ServiceConfiguration,
    onNavigateToSettings: () -> Unit,
    onNavigateToTesting: () -> Unit,
    onRequestPermission: () -> Unit,
    commandsExecuted: Int = 0,
    successRate: Float = 0.0f,
    performanceMode: String = "Balanced"
)
```

#### Dashboard Components

**1. Dashboard Header:**
- App branding with icon
- Version information
- VoiceOS Accessibility title

```kotlin
@Composable
fun DashboardHeader() {
    Card(modifier = Modifier.glassMorphism(...)) {
        Row {
            Icon(imageVector = Icons.Default.Accessibility)
            Column {
                Text("VoiceOS Accessibility")
                Text("Voice Control Dashboard")
            }
        }
    }
}
```

**2. Service Status Card:**
- Real-time service status
- Enable/disable actions
- Color-coded status indicator

```kotlin
@Composable
fun ServiceStatusCard(
    serviceEnabled: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.glassMorphism(
            tintColor = if (serviceEnabled) Color.Green else Color.Red
        )
    ) {
        StatusIndicatorBadge(isActive = serviceEnabled)
        if (!serviceEnabled) {
            Button(onClick = onRequestPermission) {
                Text("Enable Accessibility Service")
            }
        }
    }
}
```

**3. Quick Statistics Section:**
- Commands executed count
- Success rate percentage
- Active handlers count

```kotlin
@Composable
fun QuickStatsSection(
    commandsExecuted: Int,
    successRate: Float,
    handlersActive: Int
) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
        StatisticItem(
            value = commandsExecuted.toString(),
            label = "Commands",
            icon = Icons.Default.PlayArrow,
            color = Color.Blue
        )
        StatisticItem(
            value = "${(successRate * 100).toInt()}%",
            label = "Success Rate",
            icon = Icons.Default.CheckCircle,
            color = Color.Green
        )
        StatisticItem(
            value = "$handlersActive",
            label = "Handlers",
            icon = Icons.Default.Extension,
            color = Color.Orange
        )
    }
}
```

**4. Performance Mode Card:**
- Current performance mode display
- Mode-specific icon and color

**5. Handler Status Overview:**
- Grid layout showing all handlers
- Individual handler status indicators
- Green (enabled) / Red (disabled) dots

**6. Dashboard Navigation:**
- Settings button
- Testing button (enabled only when service active)

**7. System Info Card:**
- Cache size configuration
- Command timeout settings
- Cursor state

---

## Jetpack Compose Integration

### Compose Architecture

The entire UI is built with Jetpack Compose, providing:

- **Declarative UI**: UI described as functions of state
- **Reactive Updates**: Automatic recomposition on state changes
- **Material Design 3**: Modern design system
- **Animation Support**: Built-in animations and transitions

### State Management Patterns

**1. LiveData with Compose:**
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val state by viewModel.stateFlow.observeAsState(initialValue)

    // UI updates automatically when state changes
    Text(text = state.value)
}
```

**2. remember and mutableStateOf:**
```kotlin
@Composable
fun MyComponent() {
    var isExpanded by remember { mutableStateOf(false) }

    // UI reflects isExpanded state
    AnimatedVisibility(visible = isExpanded) {
        // Expanded content
    }
}
```

**3. StateFlow with collectAsState:**
```kotlin
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val configuration by viewModel.configuration.collectAsState()

    // Configuration changes trigger recomposition
    ConfigurationDisplay(config = configuration)
}
```

### Glassmorphism Modifier

All glassmorphism effects are applied via custom modifiers:

```kotlin
@Composable
fun MyCard() {
    Card(
        modifier = Modifier.glassMorphism(
            config = GlassMorphismConfig(
                cornerRadius = 16.dp,
                backgroundOpacity = 0.1f,
                borderOpacity = 0.2f,
                borderWidth = 1.dp,
                tintColor = Color(0xFF4285F4),
                tintOpacity = 0.15f
            ),
            depth = DepthLevel(0.6f)
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        // Card content
    }
}
```

### Animation Patterns

**Fade In/Out:**
```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(300)),
    exit = fadeOut(animationSpec = tween(300))
) {
    // Content
}
```

**Scale Animation:**
```kotlin
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.95f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
)
Box(modifier = Modifier.scale(scale)) {
    // Content
}
```

---

## UI Components

### ConfidenceIndicator

**File:** `ConfidenceIndicator.kt`
**Purpose:** Visual feedback for speech recognition confidence levels.

#### Confidence Levels

```kotlin
enum class ConfidenceLevel {
    HIGH,      // >85% - Green - Execute immediately
    MEDIUM,    // 70-85% - Yellow - Ask confirmation
    LOW,       // 50-70% - Orange - Show alternatives
    REJECT     // <50% - Red - Command not recognized
}
```

#### Simple Indicator

```kotlin
@Composable
fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Row {
        // Color-coded dot
        Canvas(modifier = Modifier.size(16.dp)) {
            drawCircle(color = getConfidenceColor(level))
        }
        // Percentage display
        Text(text = "${(confidence * 100).toInt()}%", color = color)
    }
}
```

#### Detailed Indicator

```kotlin
@Composable
fun DetailedConfidenceIndicator(
    confidence: Float,
    text: String,
    showPercentage: Boolean = true
) {
    Column {
        // Header with text and percentage
        Row {
            Text(text = text)
            if (showPercentage) {
                Text(text = "${(confidence * 100).toInt()}%")
            }
        }
        // Progress bar
        ConfidenceProgressBar(confidence = confidence, color = color)
        // Level text
        Text(text = getConfidenceLevelText(level))
    }
}
```

#### Circular Indicator

```kotlin
@Composable
fun CircularConfidenceIndicator(
    confidence: Float,
    size: Dp = 60.dp,
    strokeWidth: Dp = 6.dp,
    showPercentage: Boolean = true
) {
    Box(modifier = Modifier.size(size)) {
        // Background circle
        Canvas { drawCircle(color = Gray, style = Stroke(width)) }
        // Confidence arc (animated)
        Canvas {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * confidence,
                useCenter = false,
                style = Stroke(width, cap = StrokeCap.Round)
            )
        }
        // Percentage text
        if (showPercentage) {
            Text("${(confidence * 100).toInt()}%")
        }
    }
}
```

---

### FloatingEngineSelector

**File:** `FloatingEngineSelector.kt`
**Purpose:** Quick engine selection during testing.

#### Component Overview

Floating UI that displays speech recognition engine options with initials for quick switching.

#### Data Structure

```kotlin
data class EngineOption(
    val id: String,
    val initial: String,
    val fullName: String,
    val color: Color
)
```

#### Main Composable

```kotlin
@Composable
fun FloatingEngineSelector(
    selectedEngine: String,
    onEngineSelected: (String) -> Unit,
    onInitiate: (String) -> Unit,
    isRecognizing: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Available engines
    val engines = listOf(
        EngineOption("vivoka", "V", "Vivoka", Color.Green),
        EngineOption("vosk", "K", "Vosk", Color.Blue),
        EngineOption("android_stt", "A", "Android", Color.Orange),
        EngineOption("whisper", "W", "Whisper", Color.Purple),
        EngineOption("google_cloud", "G", "Google", Color.Red)
    )

    // Floating button and expandable selector
}
```

#### Engine Selection

```kotlin
@Composable
private fun EngineButton(
    engine: EngineOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isSelected) engine.color else engine.color.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onClick() }
    ) {
        Text(engine.initial, color = Color.White, fontSize = 18.sp)
    }
}
```

---

## Theme System

### AccessibilityTheme

**File:** `Theme.kt`
**Purpose:** Material3 theme configuration with dark mode optimization.

#### Color Schemes

**Dark Theme (Primary):**
```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4285F4),           // VoiceOS Blue
    secondary = Color(0xFF673AB7),         // VoiceOS Purple
    tertiary = Color(0xFF00C853),          // VoiceOS Green
    error = Color(0xFFFF5722),             // VoiceOS Red
    background = Color(0xFF000000),        // Pure black
    surface = Color(0xFF121212),           // Dark surface
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)
```

**Light Theme (Fallback):**
```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF673AB7),
    tertiary = Color(0xFF00A845),
    error = Color(0xFFD32F2F),
    background = Color.White,
    surface = Color(0xFFFAFAFA)
)
```

#### Theme Application

```kotlin
@Composable
fun AccessibilityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

#### Usage

```kotlin
setContent {
    AccessibilityTheme {
        // All composables inherit theme
        MainScreen()
    }
}
```

---

## Utilities

### ThemeUtils

**File:** `ThemeUtils.kt`
**Purpose:** Glassmorphism effects and theme utilities.

#### Glassmorphism Configuration

```kotlin
data class GlassMorphismConfig(
    val cornerRadius: Dp,
    val backgroundOpacity: Float,
    val borderOpacity: Float,
    val borderWidth: Dp,
    val tintColor: Color,
    val tintOpacity: Float,
    val noiseOpacity: Float = 0.05f
)
```

#### Glassmorphism Modifier

```kotlin
fun Modifier.glassMorphism(
    config: GlassMorphismConfig,
    depth: DepthLevel,
    isDarkTheme: Boolean = true
): Modifier {
    val shape = RoundedCornerShape(config.cornerRadius)

    return this
        .clip(shape)
        .background(
            brush = createGlassBrush(config, depth, isDarkTheme),
            shape = shape
        )
        .border(
            width = config.borderWidth,
            brush = createBorderBrush(config, isDarkTheme),
            shape = shape
        )
}
```

#### Preset Styles

```kotlin
object GlassMorphismDefaults {
    val Primary = GlassMorphismConfig(...)
    val Secondary = GlassMorphismConfig(...)
    val Card = GlassMorphismConfig(...)
    val Button = GlassMorphismConfig(...)
}
```

#### Helper Functions

```kotlin
object ThemeUtils {
    fun getTextColor(isDarkTheme: Boolean = true, alpha: Float = 1f): Color
    fun getSecondaryTextColor(isDarkTheme: Boolean = true): Color
    fun getDisabledTextColor(isDarkTheme: Boolean = true): Color
}
```

---

### DisplayUtils

**File:** `DisplayUtils.kt`
**Purpose:** Modern display metrics utilities with Android 11+ support.

#### Real Screen Size

```kotlin
object DisplayUtils {
    fun getRealScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use modern WindowMetrics API (Android 11+)
            val bounds = windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            // Fallback to deprecated API
            val display = windowManager.defaultDisplay
            val point = Point()
            display.getRealSize(point)
            point
        }
    }
}
```

#### Display Metrics

```kotlin
fun getRealDisplayMetrics(context: Context): DisplayMetrics {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Modern API
        val bounds = windowManager.currentWindowMetrics.bounds
        val metrics = DisplayMetrics()
        metrics.widthPixels = bounds.width()
        metrics.heightPixels = bounds.height()
        // Copy density from system metrics
        val systemMetrics = Resources.getSystem().displayMetrics
        metrics.density = systemMetrics.density
        metrics.densityDpi = systemMetrics.densityDpi
        metrics
    } else {
        // Deprecated API
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        metrics
    }
}
```

---

## Integration Points

### Service Integration

The UI layer integrates with the accessibility service through ViewModels:

```kotlin
class MainViewModel : ViewModel() {
    private val accessibilityService: VoiceOSService?

    val serviceEnabled = MutableLiveData<Boolean>()
    val configuration = MutableLiveData<ServiceConfiguration>()

    fun initialize(context: Context) {
        // Connect to service
        checkServiceStatus()
    }

    fun checkServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled(context)
        serviceEnabled.postValue(isEnabled)
    }
}
```

### ViewModel Communication

```kotlin
// In Activity/Composable:
val viewModel: MainViewModel by viewModels()

// Observe state
val serviceEnabled by viewModel.serviceEnabled.observeAsState(false)

// Trigger actions
viewModel.toggleService()
viewModel.updateConfiguration(newConfig)
```

### Navigation Flow

```
MainActivity (Entry)
    ├─→ AccessibilitySettings (via Intent)
    │   └─→ Back to MainActivity
    │
    ├─→ LearnAppActivity (via Intent)
    │   └─→ Back to MainActivity
    │
    └─→ System Settings (via Intent)
        └─→ Back to MainActivity
```

---

## Best Practices

### Compose Guidelines

**1. State Hoisting:**
```kotlin
// Good: Stateless composable
@Composable
fun MyButton(isEnabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = isEnabled) {
        Text("Click Me")
    }
}

// Parent manages state
@Composable
fun ParentScreen() {
    var isEnabled by remember { mutableStateOf(true) }
    MyButton(isEnabled = isEnabled, onClick = { /* action */ })
}
```

**2. Remember for Expensive Operations:**
```kotlin
@Composable
fun ExpensiveComputation() {
    val result = remember { computeExpensiveValue() }
    Text(result)
}
```

**3. Key for Lists:**
```kotlin
LazyColumn {
    items(items, key = { it.id }) { item ->
        ItemCard(item)
    }
}
```

### Glassmorphism Usage

**1. Consistent Depth Levels:**
```kotlin
// Background layer
depth = DepthLevel(0.2f)

// Interactive elements
depth = DepthLevel(0.6f)

// Top layer (headers, FABs)
depth = DepthLevel(1.0f)
```

**2. Appropriate Opacity:**
```kotlin
// Subtle background
backgroundOpacity = 0.08f

// Interactive cards
backgroundOpacity = 0.15f

// Prominent elements
backgroundOpacity = 0.25f
```

**3. Color Coordination:**
```kotlin
// Use theme colors
tintColor = MaterialTheme.colorScheme.primary

// Status-based colors
tintColor = if (isSuccess) Color.Green else Color.Red
```

### Performance Optimization

**1. Minimize Recompositions:**
```kotlin
@Composable
fun OptimizedList(items: List<Item>) {
    LazyColumn {
        items(items, key = { it.id }) { item ->
            // Use remember to avoid recomputation
            val processedItem = remember(item.id) { processItem(item) }
            ItemView(processedItem)
        }
    }
}
```

**2. Stable Parameters:**
```kotlin
// Avoid lambda recreation
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val onClick = remember { { viewModel.handleClick() } }
    Button(onClick = onClick)
}
```

**3. derivedStateOf for Computed Values:**
```kotlin
val filteredList by remember {
    derivedStateOf {
        items.filter { it.isActive }
    }
}
```

### Accessibility Best Practices

**1. Semantic Content Descriptions:**
```kotlin
Icon(
    imageVector = Icons.Default.Settings,
    contentDescription = "Open settings"
)
```

**2. Clickable Areas:**
```kotlin
// Minimum 48.dp touch target
IconButton(
    onClick = { /* action */ },
    modifier = Modifier.size(48.dp)
) {
    Icon(...)
}
```

**3. Color Contrast:**
```kotlin
// Ensure sufficient contrast
Text(
    text = "Important Message",
    color = Color.White, // Against dark background
    fontWeight = FontWeight.Bold
)
```

---

## Summary

The VoiceAccessibility UI Layer provides a comprehensive, accessibility-first interface built entirely with Jetpack Compose and Material Design 3. Key highlights:

- **Modern Architecture**: Jetpack Compose with reactive state management
- **Consistent Design**: Glassmorphism effects matching VoiceOS SRS
- **User-Friendly**: Clear status indicators, intuitive navigation
- **Performance**: Optimized rendering and state management
- **Accessibility**: High contrast, large touch targets, semantic descriptions
- **Extensible**: Modular composables, reusable components

For overlay system documentation, see `Overlay-System-Documentation-251010-1105.md`.

---

**Document Version:** 1.0
**Last Updated:** 2025-10-10 11:05:00 PDT
**Next Review:** 2025-11-10
