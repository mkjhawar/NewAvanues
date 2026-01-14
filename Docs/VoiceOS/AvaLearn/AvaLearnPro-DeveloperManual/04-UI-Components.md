# Chapter 4: UI Components & Theme

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch04
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 4.1 Theme System

### 4.1.1 Ocean Blue XR Dark Theme

```kotlin
package com.augmentalis.learnappdev.theme

import androidx.compose.ui.graphics.Color

object OceanDevTheme {
    // Primary Colors (Ocean Blue Dark Mode)
    val Primary = Color(0xFF60A5FA)
    val PrimaryDark = Color(0xFF3B82F6)
    val PrimaryContainer = Color(0xFF1E3A5F)
    val OnPrimaryContainer = Color(0xFFDDEBFF)

    // Developer Accent (Cyan)
    val Accent = Color(0xFF22D3EE)
    val AccentDark = Color(0xFF06B6D4)
    val AccentContainer = Color(0xFF164E63)
    val OnAccentContainer = Color(0xFFCFFAFE)

    // Semantic Colors
    val Success = Color(0xFF34D399)
    val SuccessContainer = Color(0xFF065F46)
    val Error = Color(0xFFF87171)
    val ErrorContainer = Color(0xFF7F1D1D)
    val Warning = Color(0xFFFBBF24)
    val WarningContainer = Color(0xFF78350F)

    // Surface Colors (Dark)
    val Surface = Color(0xFF0F172A)
    val SurfaceVariant = Color(0xFF1E293B)
    val SurfaceDim = Color(0xFF0C1929)
    val SurfaceBright = Color(0xFF334155)
    val Background = Color(0xFF0C1929)

    // On Colors
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFFE2E8F0)
    val OnSurfaceVariant = Color(0xFF94A3B8)
    val OnBackground = Color(0xFFE2E8F0)

    // Developer-Specific Colors
    val ConsoleBackground = Color(0xFF0D0D0D)
    val ConsoleBorder = Color(0xFF374151)
    val ConsoleForeground = Color(0xFF10B981)

    // Log Level Colors
    val LogDebug = Color(0xFF9E9E9E)
    val LogInfo = Color(0xFF60A5FA)
    val LogWarn = Color(0xFFFBBF24)
    val LogError = Color(0xFFF87171)
    val LogEvent = Color(0xFFA78BFA)

    // Status Colors
    val StatusActive = Color(0xFF34D399)
    val StatusPaused = Color(0xFFFBBF24)
    val StatusIdle = Color(0xFF6B7280)
    val StatusExploring = Color(0xFF60A5FA)
    val StatusError = Color(0xFFF87171)
}
```

### 4.1.2 Material Theme Configuration

```kotlin
@Composable
fun LearnAppDevTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = OceanDevTheme.Primary,
        onPrimary = OceanDevTheme.OnPrimary,
        primaryContainer = OceanDevTheme.PrimaryContainer,
        onPrimaryContainer = OceanDevTheme.OnPrimaryContainer,
        secondary = OceanDevTheme.Accent,
        onSecondary = Color.White,
        secondaryContainer = OceanDevTheme.AccentContainer,
        onSecondaryContainer = OceanDevTheme.OnAccentContainer,
        surface = OceanDevTheme.Surface,
        onSurface = OceanDevTheme.OnSurface,
        surfaceVariant = OceanDevTheme.SurfaceVariant,
        onSurfaceVariant = OceanDevTheme.OnSurfaceVariant,
        background = OceanDevTheme.Background,
        onBackground = OceanDevTheme.OnBackground,
        error = OceanDevTheme.Error,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

---

## 4.2 Main Activity Structure

### 4.2.1 LearnAppDevActivity

```kotlin
class LearnAppDevActivity : ComponentActivity() {

    // Service binding
    private var jitService: IElementCaptureService? = null
    private var isBound = false

    // State
    private val uiState = mutableStateOf(DevUiState())
    private val logs = mutableStateListOf<LogEntry>()
    private val elements = mutableStateListOf<ParcelableNodeInfo>()
    private var selectedTab by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LearnAppDevTheme {
                LearnAppDevScreen(
                    state = uiState.value,
                    logs = logs,
                    elements = elements,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onPauseClick = { pauseJIT() },
                    onResumeClick = { resumeJIT() },
                    onRefreshClick = { refreshState() },
                    onStartExploration = { startExploration() },
                    onStopExploration = { stopExploration() },
                    onExportClick = { exportData() },
                    onQueryElements = { queryElements() },
                    onClearLogs = { logs.clear() }
                )
            }
        }

        bindToService()
    }
}
```

### 4.2.2 Main Screen Composable

```kotlin
@Composable
fun LearnAppDevScreen(
    state: DevUiState,
    logs: List<LogEntry>,
    elements: List<ParcelableNodeInfo>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onStartExploration: () -> Unit,
    onStopExploration: () -> Unit,
    onExportClick: () -> Unit,
    onQueryElements: () -> Unit,
    onClearLogs: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LearnApp Dev")
                        Spacer(modifier = Modifier.width(8.dp))
                        DevBadge()
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OceanDevTheme.PrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(OceanDevTheme.Background)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = OceanDevTheme.SurfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text("Status") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text("Logs (${logs.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    text = { Text("Elements (${elements.size})") }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> StatusTab(
                    state = state,
                    onPauseClick = onPauseClick,
                    onResumeClick = onResumeClick,
                    onRefreshClick = onRefreshClick,
                    onStartExploration = onStartExploration,
                    onStopExploration = onStopExploration,
                    onExportClick = onExportClick
                )
                1 -> LogsTab(
                    logs = logs,
                    onClearLogs = onClearLogs
                )
                2 -> ElementsTab(
                    elements = elements,
                    onQueryElements = onQueryElements
                )
            }
        }
    }
}

@Composable
fun DevBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = OceanDevTheme.Accent
    ) {
        Text(
            text = "DEV",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}
```

---

## 4.3 Tab Components

### 4.3.1 Status Tab

```kotlin
@Composable
fun StatusTab(
    state: DevUiState,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onStartExploration: () -> Unit,
    onStopExploration: () -> Unit,
    onExportClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { JITStatusCard(state, onPauseClick, onResumeClick, onRefreshClick) }
        item { ExplorationCard(state, onStartExploration, onStopExploration) }
        item { SafetyCard(state) }
        item { ExportCard(state, onExportClick) }
    }
}

@Composable
fun JITStatusCard(
    state: DevUiState,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    DevCard(title = "JIT Service") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Status", color = OceanDevTheme.OnSurfaceVariant)
            StatusBadge(
                text = if (state.jitActive) "Active" else "Inactive",
                color = if (state.jitActive) OceanDevTheme.StatusActive else OceanDevTheme.StatusIdle
            )
        }

        StatRow("Screens Learned", state.screensExplored.toString())
        StatRow("Elements Discovered", state.elementsDiscovered.toString())
        StatRow("Current Package", state.currentPackage.ifEmpty { "None" })

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPauseClick,
                enabled = state.jitActive && !state.jitPaused,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OceanDevTheme.Warning
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Pause")
            }

            Button(
                onClick = onResumeClick,
                enabled = state.jitPaused,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OceanDevTheme.Success
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Resume")
            }

            IconButton(onClick = onRefreshClick) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = OceanDevTheme.Accent
                )
            }
        }
    }
}
```

### 4.3.2 Logs Tab

```kotlin
@Composable
fun LogsTab(
    logs: List<LogEntry>,
    onClearLogs: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OceanDevTheme.SurfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${logs.size} entries",
                color = OceanDevTheme.OnSurfaceVariant
            )
            TextButton(onClick = onClearLogs) {
                Text("Clear", color = OceanDevTheme.Accent)
            }
        }

        // Log Console
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(OceanDevTheme.ConsoleBackground)
                .padding(8.dp),
            reverseLayout = true  // Newest at bottom
        ) {
            items(logs.reversed()) { entry ->
                LogEntryRow(entry)
            }
        }
    }
}

@Composable
fun LogEntryRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.DEBUG -> OceanDevTheme.LogDebug
        LogLevel.INFO -> OceanDevTheme.LogInfo
        LogLevel.WARN -> OceanDevTheme.LogWarn
        LogLevel.ERROR -> OceanDevTheme.LogError
        LogLevel.EVENT -> OceanDevTheme.LogEvent
    }

    Text(
        text = entry.formatted(),
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
```

### 4.3.3 Elements Tab

```kotlin
@Composable
fun ElementsTab(
    elements: List<ParcelableNodeInfo>,
    onQueryElements: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OceanDevTheme.SurfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${elements.size} elements",
                color = OceanDevTheme.OnSurfaceVariant
            )
            Button(
                onClick = onQueryElements,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OceanDevTheme.Accent
                )
            ) {
                Text("Query", color = Color.Black)
            }
        }

        // Element List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(elements) { element ->
                ElementCard(element)
            }
        }
    }
}

@Composable
fun ElementCard(element: ParcelableNodeInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = OceanDevTheme.SurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Name and Type
            Text(
                text = element.getDisplayName(),
                style = MaterialTheme.typography.titleSmall,
                color = OceanDevTheme.OnSurface
            )
            Text(
                text = element.getShortClassName(),
                style = MaterialTheme.typography.bodySmall,
                color = OceanDevTheme.Accent
            )

            // Resource ID
            if (element.resourceId.isNotEmpty()) {
                Text(
                    text = element.resourceId,
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanDevTheme.OnSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (element.isClickable) ActionChip("click")
                if (element.isLongClickable) ActionChip("long")
                if (element.isEditable) ActionChip("edit")
                if (element.isScrollable) ActionChip("scroll")
            }

            // Bounds
            Text(
                text = element.getBoundsString(),
                style = MaterialTheme.typography.labelSmall,
                color = OceanDevTheme.OnSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ActionChip(action: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = OceanDevTheme.PrimaryContainer
    ) {
        Text(
            text = action,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = OceanDevTheme.OnPrimaryContainer
        )
    }
}
```

---

## 4.4 Reusable Components

### 4.4.1 DevCard

```kotlin
@Composable
fun DevCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = OceanDevTheme.Surface
        ),
        border = BorderStroke(1.dp, OceanDevTheme.ConsoleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OceanDevTheme.Accent
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
```

### 4.4.2 StatusBadge

```kotlin
@Composable
fun StatusBadge(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}
```

### 4.4.3 StatRow

```kotlin
@Composable
fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = OceanDevTheme.OnSurfaceVariant
        )
        Text(
            text = value,
            color = OceanDevTheme.OnSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
```

---

## 4.5 Theme Comparison

### 4.5.1 User vs Developer Edition

| Component | User (Light) | Developer (Dark) |
|-----------|--------------|------------------|
| Background | #F0F9FF | #0C1929 |
| Surface | #FFFFFF | #0F172A |
| Primary | #3B82F6 | #60A5FA |
| Accent | N/A | #22D3EE (Cyan) |
| Console | N/A | #0D0D0D |
| Dev Badge | N/A | Cyan badge |
| Logs Tab | N/A | Full console |
| Elements Tab | N/A | Inspector view |

### 4.5.2 Visual Identification

Developers can identify the edition by:
1. **Dark theme** - User edition is light
2. **Cyan "DEV" badge** - Top app bar
3. **Three tabs** - User has single scrolling view
4. **Console colors** - Monospace log output

---

## 4.6 Next Steps

Continue to [Chapter 5: Event Streaming System](./05-Event-Streaming.md) for real-time event handling.

---

**End of Chapter 4**
