# MagicUI Compose Implementation

Platform: Android (Jetpack Compose) | Min SDK: 24 | Target SDK: 34 | Version: 1.0.0

---

## Dependencies

```kotlin
// build.gradle.kts (Module)
dependencies {
    implementation("com.augmentalis.avanues.core:magicui:1.0.0")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
}

android {
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
}
```

---

## Theme Setup

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MagicUITheme(theme = OceanTheme.dark()) {
                YourApp()
            }
        }
    }
}
```

---

## Ocean Colors

```kotlin
// OceanTheme.kt
object OceanTheme {
    val DeepOcean = Color(0xFF0A1929)
    val OceanDepth = Color(0xFF0F172A)
    val OceanMid = Color(0xFF1E293B)
    val OceanShallow = Color(0xFF334155)

    val CoralBlue = Color(0xFF3B82F6)
    val TurquoiseCyan = Color(0xFF06B6D4)
    val SeafoamGreen = Color(0xFF10B981)
    val SunsetOrange = Color(0xFFF59E0B)
    val CoralRed = Color(0xFFEF4444)

    val PearlWhite = Color(0xFFF8FAFC)
    val SeaMist = Color(0xFFE2E8F0)
    val StormGray = Color(0xFF94A3B8)
    val DeepFog = Color(0xFF475569)

    val Surface5 = Color.White.copy(alpha = 0.05f)
    val Surface10 = Color.White.copy(alpha = 0.10f)
    val Surface15 = Color.White.copy(alpha = 0.15f)
    val Surface20 = Color.White.copy(alpha = 0.20f)
    val Surface30 = Color.White.copy(alpha = 0.30f)

    val Border10 = Color.White.copy(alpha = 0.10f)
    val Border20 = Color.White.copy(alpha = 0.20f)
    val Border30 = Color.White.copy(alpha = 0.30f)

    val TextPrimary = Color.White.copy(alpha = 0.90f)
    val TextSecondary = Color.White.copy(alpha = 0.80f)
    val TextMuted = Color.White.copy(alpha = 0.60f)
    val TextDisabled = Color.White.copy(alpha = 0.40f)

    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(DeepOcean, OceanDepth, OceanMid, OceanDepth)
    )

    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(
            CoralBlue.copy(alpha = 0.2f),
            TurquoiseCyan.copy(alpha = 0.2f)
        )
    )
}
```

---

## Background

```kotlin
@Composable
fun OceanBackground(
    showGrid: Boolean = true,
    showAmbientLights: Boolean = true,
    gridSpacing: Dp = 50.dp,
    gridOpacity: Float = 0.1f
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OceanTheme.BackgroundGradient)
        )

        if (showGrid) {
            GridPattern(spacing = gridSpacing, opacity = gridOpacity)
        }

        if (showAmbientLights) {
            AmbientLights()
        }
    }
}
```

---

## Glassmorphic Surface

```kotlin
@Composable
fun GlassmorphicSurface(
    background: Color,
    border: Color,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 40.dp,
    cornerRadius: Dp = 16.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier
                    .blur(blurRadius)
                    .background(background, RoundedCornerShape(cornerRadius))
                    .border(borderWidth, border, RoundedCornerShape(cornerRadius))
            } else {
                Modifier
                    .background(background.copy(alpha = 0.95f), RoundedCornerShape(cornerRadius))
                    .border(borderWidth, border, RoundedCornerShape(cornerRadius))
            }
        )
    ) {
        content()
    }
}
```

---

## Data Table

```kotlin
@Composable
fun DataTable(
    columns: List<TableColumn>,
    rows: List<TableRow>,
    modifier: Modifier = Modifier
) {
    GlassmorphicSurface(
        background = Color.Transparent,
        border = OceanTheme.Border10,
        cornerRadius = 16.dp,
        modifier = modifier
    ) {
        LazyColumn {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(OceanTheme.Surface10)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    columns.forEach { column ->
                        Text(
                            text = column.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = OceanTheme.TextPrimary,
                            modifier = Modifier.weight(column.weight)
                        )
                    }
                }
            }

            itemsIndexed(rows) { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(if (index % 2 == 0) OceanTheme.Surface5 else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.cells.forEachIndexed { i, cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OceanTheme.TextSecondary,
                            modifier = Modifier.weight(row.weights[i])
                        )
                    }
                }

                if (index < rows.size - 1) {
                    Divider(color = OceanTheme.Border10, thickness = 1.dp)
                }
            }
        }
    }
}

data class TableColumn(val label: String, val weight: Float)
data class TableRow(val cells: List<String>, val weights: List<Float>)
```

---

## Todo List

```kotlin
@Composable
fun TodoList(
    tasks: List<Task>,
    onTaskToggle: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicSurface(
        background = OceanTheme.Surface5,
        border = OceanTheme.Border10,
        cornerRadius = 16.dp,
        modifier = modifier
    ) {
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            items(tasks) { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 72.dp)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.status == TaskStatus.Completed,
                        onCheckedChange = { onTaskToggle(task) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = OceanTheme.SeafoamGreen,
                            uncheckedColor = OceanTheme.Border20
                        )
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (task.status) {
                                TaskStatus.Completed -> OceanTheme.TextMuted
                                TaskStatus.InProgress -> OceanTheme.TextPrimary
                                TaskStatus.Pending -> OceanTheme.TextSecondary
                            },
                            textDecoration = if (task.status == TaskStatus.Completed) {
                                TextDecoration.LineThrough
                            } else null
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriorityBadge(task.priority)
                            StatusIndicator(
                                status = when (task.status) {
                                    TaskStatus.Completed -> StatusIndicator.Status.Success
                                    TaskStatus.InProgress -> StatusIndicator.Status.Info
                                    TaskStatus.Pending -> StatusIndicator.Status.Pending
                                },
                                size = 6.dp
                            )
                        }
                    }
                }

                if (task != tasks.last()) {
                    Divider(color = OceanTheme.Border10)
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: TaskPriority) {
    val (text, color) = when (priority) {
        TaskPriority.High -> "High" to OceanTheme.CoralRed
        TaskPriority.Medium -> "Medium" to OceanTheme.SunsetOrange
        TaskPriority.Low -> "Low" to OceanTheme.CoralBlue
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun StatusIndicator(status: StatusIndicator.Status, size: Dp = 8.dp) {
    val color = when (status) {
        StatusIndicator.Status.Success -> OceanTheme.SeafoamGreen
        StatusIndicator.Status.Warning -> OceanTheme.SunsetOrange
        StatusIndicator.Status.Error -> OceanTheme.CoralRed
        StatusIndicator.Status.Info -> OceanTheme.CoralBlue
        StatusIndicator.Status.Pending -> OceanTheme.TextMuted
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
    )
}

data class Task(val id: String, val title: String, val status: TaskStatus, val priority: TaskPriority)
enum class TaskStatus { Pending, InProgress, Completed }
enum class TaskPriority { Low, Medium, High }
```

---

## Modal Dialog

```kotlin
@Composable
fun OceanDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel"
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicSurface(
            background = OceanTheme.Surface20,
            border = OceanTheme.Border30,
            cornerRadius = 24.dp,
            blurRadius = 40.dp,
            modifier = Modifier.width(600.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = OceanTheme.TextPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = OceanTheme.TextSecondary)
                    }
                }

                Divider(color = OceanTheme.Border10)

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OceanTheme.TextSecondary,
                    modifier = Modifier.padding(24.dp)
                )

                Divider(color = OceanTheme.Border10)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissText, color = OceanTheme.TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = OceanTheme.CoralBlue)
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}
```

---

## Toast

```kotlin
@Composable
fun OceanToast(
    message: String,
    type: ToastType = ToastType.Info,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, borderColor) = when (type) {
        ToastType.Info -> Icons.Default.Info to OceanTheme.CoralBlue
        ToastType.Success -> Icons.Default.CheckCircle to OceanTheme.SeafoamGreen
        ToastType.Warning -> Icons.Default.Warning to OceanTheme.SunsetOrange
        ToastType.Error -> Icons.Default.Error to OceanTheme.CoralRed
    }

    GlassmorphicSurface(
        background = OceanTheme.Surface30,
        border = Color.Transparent,
        cornerRadius = 12.dp,
        blurRadius = 24.dp,
        modifier = modifier
            .widthIn(max = 400.dp)
            .border(4.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = borderColor, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = OceanTheme.TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Dismiss", tint = OceanTheme.TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

enum class ToastType { Info, Success, Warning, Error }
```

---

## Layout: Dashboard

```kotlin
@Composable
fun DashboardScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        OceanBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                GlassmorphicSurface(
                    background = Color.Transparent,
                    border = OceanTheme.Border10,
                    cornerRadius = 16.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OceanTheme.AccentGradient)
                            .padding(32.dp)
                    ) {
                        Column {
                            Text("Enterprise Dashboard", style = MaterialTheme.typography.headlineLarge, color = OceanTheme.TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Welcome back", style = MaterialTheme.typography.bodyLarge, color = OceanTheme.TextMuted)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("24", "156", "94%", "38").chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { value ->
                                MetricCard(value, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(value: String, modifier: Modifier = Modifier) {
    GlassmorphicSurface(
        background = OceanTheme.Surface5,
        border = OceanTheme.Border10,
        cornerRadius = 16.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = OceanTheme.TextPrimary)
        }
    }
}
```

---

## Material Design 3 Mapping

```kotlin
val oceanMaterial3ColorScheme = darkColorScheme(
    primary = OceanTheme.CoralBlue,
    onPrimary = Color.Black,
    primaryContainer = OceanTheme.DeepOcean,
    onPrimaryContainer = OceanTheme.TurquoiseCyan.copy(alpha = 0.9f),
    secondary = OceanTheme.TurquoiseCyan,
    onSecondary = Color.Black,
    tertiary = OceanTheme.SeafoamGreen,
    error = OceanTheme.CoralRed,
    background = OceanTheme.DeepOcean,
    onBackground = OceanTheme.PearlWhite,
    surface = OceanTheme.OceanDepth,
    onSurface = OceanTheme.PearlWhite
)
```

---

## Accessibility

```kotlin
// Touch targets: 48dp minimum
Button(
    onClick = { },
    modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
) { Text("Button") }

// Content descriptions
Icon(
    imageVector = Icons.Default.Settings,
    contentDescription = "Open settings"
)

// Stable keys for lists
LazyColumn {
    items(items = data, key = { it.id }) { item ->
        ItemCard(item)
    }
}
```

---

## Examples

`android/avanues/core/magicui/examples/OceanThemeExample.kt`

---

**Platform:** Android (Jetpack Compose) | **SDK:** 24-34 | **Version:** 1.0.0 | **Updated:** 2025-11-28
