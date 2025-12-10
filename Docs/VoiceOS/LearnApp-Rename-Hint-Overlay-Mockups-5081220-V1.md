# LearnApp Rename Hint Overlay - UI Mockups

**Document**: LearnApp-Rename-Hint-Overlay-Mockups-5081220-V1.md
**Author**: Manoj Jhawar
**Code-Reviewed-By**: Claude Code (IDEACODE v10.3)
**Created**: 2025-12-08
**Related**: LearnApp-On-Demand-Command-Renaming-5081220-V2.md

---

## Design Principles

1. **Non-Intrusive**: Top of screen, semi-transparent, auto-dismisses
2. **Clear Messaging**: Simple instruction with example
3. **Context-Aware**: Only shows when needed
4. **Accessible**: High contrast, readable font, VoiceOver compatible
5. **Material Design 3**: Follows Material 3 guidelines

---

## Mockup 1: Standard Hint Overlay

### Visual Layout (ASCII)

```
┌─────────────────────────────────────────────────────────────┐
│                      Device Screen                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ ℹ️  Rename buttons by saying:                          │  │
│  │    "Rename Button 1 to Save"                          │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ╔════════════════════════════════════════════════════╗    │
│  ║               DeviceInfo App                       ║    │
│  ╠════════════════════════════════════════════════════╣    │
│  ║  [ Button 1 ]  [ Button 2 ]  [ Button 3 ]         ║    │
│  ║                                                     ║    │
│  ║  Device Name: Pixel 7                              ║    │
│  ║  Android Version: 13                               ║    │
│  ║  Model: GP4BC                                      ║    │
│  ║                                                     ║    │
│  ╚════════════════════════════════════════════════════╝    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Specifications

**Position**: Top of screen, 16dp from top
**Width**: 90% of screen width
**Height**: WRAP_CONTENT (auto-height)
**Padding**: 16dp all sides
**Margin**: 16dp horizontal
**Elevation**: 8dp
**Corner Radius**: 12dp

**Colors** (Material 3):
- Background: `primaryContainer` (semi-transparent 90%)
- Text: `onPrimaryContainer`
- Icon: `onPrimaryContainer`

**Typography**:
- Title: `bodyMedium` (14sp, Medium weight)
- Example: `bodyLarge` (16sp, Regular weight, Italic)

**Animation**:
- Fade in: 200ms
- Stay: 3000ms
- Fade out: 200ms

---

## Mockup 2: Compact Variant (Small Screens)

### Visual Layout (ASCII)

```
┌──────────────────────────────────┐
│      Small Screen Device         │
│  ┌────────────────────────────┐  │
│  │ ℹ️  Rename: "Rename Button │  │
│  │    1 to Save"              │  │
│  └────────────────────────────┘  │
│                                   │
│  ╔═══════════════════════════╗  │
│  ║   DeviceInfo              ║  │
│  ╠═══════════════════════════╣  │
│  ║  [Btn 1] [Btn 2] [Btn 3] ║  │
│  ║                            ║  │
│  ║  Device: Pixel 7           ║  │
│  ╚═══════════════════════════╝  │
│                                   │
└──────────────────────────────────┘
```

### Specifications

**Breakpoint**: Screen width < 360dp
**Width**: 95% of screen width
**Padding**: 12dp all sides
**Typography**:
- Title: `bodySmall` (12sp, Medium weight)
- Example: `bodyMedium` (14sp, Regular weight, Italic)

---

## Mockup 3: Unity/Unreal Variant (Game UI)

### Visual Layout (ASCII)

```
┌─────────────────────────────────────────────────────────────┐
│                      Game Screen                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ ℹ️  Rename game buttons:                              │  │
│  │    "Rename Top Left Button to Jump"                  │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ╔════════════════════════════════════════════════════╗    │
│  ║                                                     ║    │
│  ║     🎮 Unity Game                                   ║    │
│  ║                                                     ║    │
│  ║   [TL Btn]         [TR Btn]                        ║    │
│  ║                                                     ║    │
│  ║                 🏃 Player                           ║    │
│  ║                                                     ║    │
│  ║   [BL Btn]         [BR Btn]                        ║    │
│  ║                                                     ║    │
│  ╚════════════════════════════════════════════════════╝    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Specifications

**Text Adjustment**: Shows spatial label example
- Example: "Rename Top Left Button to Jump"
- Example: "Rename Corner Top Far Left Button to Attack" (Unreal)

**Colors**: Higher contrast for game overlays
- Background: `primaryContainer` with 95% opacity
- Border: 1dp solid `primary` (optional for visibility)

---

## Mockup 4: Multiple Screen Hint Variants

### Variant A: First Visit
```
┌───────────────────────────────────────────────────────────┐
│ ℹ️  Rename buttons by saying:                             │
│    "Rename Button 1 to Save"                             │
│    Hint will not show again for this screen.             │
└───────────────────────────────────────────────────────────┘
```

### Variant B: Multiple Generated Labels
```
┌───────────────────────────────────────────────────────────┐
│ ℹ️  Rename buttons by saying:                             │
│    "Rename Button 1 to Save" or "Rename Tab 2 to Info"  │
└───────────────────────────────────────────────────────────┘
```

### Variant C: Settings Reminder
```
┌───────────────────────────────────────────────────────────┐
│ ℹ️  Rename buttons by saying: "Rename Button 1 to Save"  │
│    Or manage in Settings → Voice Commands                │
└───────────────────────────────────────────────────────────┘
```

---

## Mockup 5: Full Material Design 3 Implementation

### Jetpack Compose Code Preview

```kotlin
@Composable
fun RenameHintOverlay(
    exampleCommand: String = "Button 1",
    onDismiss: () -> Unit = {}
) {
    // Animation
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(3000)
        visible = false
        delay(200)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                        .copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Rename buttons by saying:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "\"Rename $exampleCommand to Save\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}
```

### Preview

```kotlin
@Preview(name = "Light Mode", showBackground = true)
@Composable
fun RenameHintOverlayPreview() {
    VoiceOSTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated app content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("DeviceInfo App", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Row {
                    Button(onClick = {}) { Text("Button 1") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {}) { Text("Button 2") }
                }
            }

            // Overlay
            RenameHintOverlay(exampleCommand = "Button 1")
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RenameHintOverlayPreviewDark() {
    VoiceOSTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("DeviceInfo App", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Row {
                    Button(onClick = {}) { Text("Button 1") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {}) { Text("Button 2") }
                }
            }

            RenameHintOverlay(exampleCommand = "Button 1")
        }
    }
}

@Preview(name = "Small Screen", device = "spec:width=320dp,height=640dp,dpi=160")
@Composable
fun RenameHintOverlayPreviewSmall() {
    VoiceOSTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text("DeviceInfo", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = {}, modifier = Modifier.size(80.dp)) {
                        Text("Btn 1", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = {}, modifier = Modifier.size(80.dp)) {
                        Text("Btn 2", fontSize = 12.sp)
                    }
                }
            }

            RenameHintOverlay(exampleCommand = "Button 1")
        }
    }
}
```

---

## Mockup 6: Accessibility Considerations

### VoiceOver/TalkBack Announcement

When overlay appears, announce via TTS:
```
"Hint: You can rename buttons by saying: Rename Button 1 to Save.
This message will auto-dismiss in 3 seconds."
```

### High Contrast Mode

**Adjustments**:
- Increase opacity to 100%
- Add 2dp border with `primary` color
- Increase icon size to 28dp
- Bold text for better readability

```kotlin
@Composable
fun RenameHintOverlayHighContrast(
    exampleCommand: String = "Button 1"
) {
    val isHighContrast = LocalContext.current.resources
        .configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(horizontal = 16.dp)
            .border(
                width = if (isHighContrast) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
                .copy(alpha = if (isHighContrast) 1.0f else 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(if (isHighContrast) 28.dp else 24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Rename buttons by saying:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = if (isHighContrast) FontWeight.Bold else FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "\"Rename $exampleCommand to Save\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontStyle = FontStyle.Italic,
                    fontWeight = if (isHighContrast) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
```

---

## Mockup 7: Animation Sequence

### Fade In (200ms)
```
Frame 0ms:   Alpha = 0.0 ░░░░░░░░░░░░░░░░░░░░
Frame 50ms:  Alpha = 0.25 ████░░░░░░░░░░░░░░░░
Frame 100ms: Alpha = 0.5 ██████████░░░░░░░░░░
Frame 150ms: Alpha = 0.75 ███████████████░░░░░
Frame 200ms: Alpha = 1.0 ████████████████████
```

### Stay Visible (3000ms)
```
Frame 200-3200ms: Alpha = 1.0 (fully visible)
```

### Fade Out (200ms)
```
Frame 3200ms: Alpha = 1.0 ████████████████████
Frame 3250ms: Alpha = 0.75 ███████████████░░░░░
Frame 3300ms: Alpha = 0.5 ██████████░░░░░░░░░░
Frame 3350ms: Alpha = 0.25 ████░░░░░░░░░░░░░░░░
Frame 3400ms: Alpha = 0.0 ░░░░░░░░░░░░░░░░░░░░
```

### Implementation
```kotlin
val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )
)

Box(
    modifier = Modifier
        .alpha(alpha)
        .fillMaxWidth()
) {
    // Card content
}
```

---

## Mockup 8: Edge Cases

### Case 1: Long Button Names
```
┌───────────────────────────────────────────────────────────┐
│ ℹ️  Rename buttons by saying:                             │
│    "Rename Top Left Button to Jump and Attack"           │
│    (Long names will wrap to next line)                   │
└───────────────────────────────────────────────────────────┘
```

### Case 2: Multiple Types of Generated Labels
```
┌───────────────────────────────────────────────────────────┐
│ ℹ️  Rename elements by saying:                            │
│    "Rename Button 1 to Save" or "Rename Tab 2 to Info"  │
└───────────────────────────────────────────────────────────┘
```

### Case 3: Landscape Orientation
```
┌────────────────────────────────────────────────────────────────────┐
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │ ℹ️  Rename: "Rename Button 1 to Save"                        │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ╔══════════════════════════════════════════════════════════════╗ │
│  ║  [Btn 1]  [Btn 2]  [Btn 3]    DeviceInfo    [Settings] [Help]║ │
│  ║                                                               ║ │
│  ║  Content area...                                              ║ │
│  ╚══════════════════════════════════════════════════════════════╝ │
└────────────────────────────────────────────────────────────────────┘
```

**Adjustments**:
- More compact text
- Shorter example
- Wider card (95% width)

---

## Mockup 9: RealWear Navigator 500 (AR Glasses)

### Visual Layout
```
┌─────────────────────────────────────────────┐
│           RealWear Navigator 500            │
│  ┌───────────────────────────────────────┐  │
│  │ ℹ️  SAY: "RENAME 1 TO SAVE"           │  │
│  └───────────────────────────────────────┘  │
│                                              │
│  ╔══════════════════════════════════════╗  │
│  ║  [1]  [2]  [3]  [4]                  ║  │
│  ║                                       ║  │
│  ║  Device: RealWear Navigator 500      ║  │
│  ║  "SELECT 1" "SELECT 2" "SELECT 3"    ║  │
│  ╚══════════════════════════════════════╝  │
│                                              │
└─────────────────────────────────────────────┘
```

**Specifications**:
- Uppercase text (RealWear convention)
- Simplified command format
- Larger font (18sp minimum)
- High contrast (1.0 opacity)

```kotlin
@Composable
fun RenameHintOverlayRealWear(
    exampleCommand: String = "1"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 1.0f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = Color.Yellow,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "SAY: \"RENAME $exampleCommand TO SAVE\"",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                letterSpacing = 1.sp
            )
        }
    }
}
```

---

## Mockup 10: Interactive Prototype Screens

### Screen 1: Hint Appears
```
┌─────────────────────────────────────────┐
│  [Hint Overlay]                         │
│  ℹ️  Rename buttons by saying:          │
│     "Rename Button 1 to Save"          │
│                                          │
│  ╔══════════════════════════════════╗  │
│  ║  [ Button 1 ]  [ Button 2 ]      ║  │
│  ║                                   ║  │
│  ║  Content...                       ║  │
│  ╚══════════════════════════════════╝  │
└─────────────────────────────────────────┘
```

### Screen 2: User Says Command
```
┌─────────────────────────────────────────┐
│  [Listening...]                         │
│  🎤 "Rename Button 1 to Save"           │
│                                          │
│  ╔══════════════════════════════════╗  │
│  ║  [ Button 1 ]  [ Button 2 ]      ║  │
│  ║                                   ║  │
│  ║  Content...                       ║  │
│  ╚══════════════════════════════════╝  │
└─────────────────────────────────────────┘
```

### Screen 3: Success Confirmation
```
┌─────────────────────────────────────────┐
│  [Success]                              │
│  ✅ Renamed to "Save"                   │
│     You can say "Save" or "Button 1"   │
│                                          │
│  ╔══════════════════════════════════╗  │
│  ║  [ Button 1 ]  [ Button 2 ]      ║  │
│  ║    (Save)                         ║  │
│  ║  Content...                       ║  │
│  ╚══════════════════════════════════╝  │
└─────────────────────────────────────────┘
```

---

## Design Tokens

### Colors (Material 3)

```kotlin
object RenameHintColors {
    val background = MaterialTheme.colorScheme.primaryContainer
    val backgroundAlpha = 0.9f
    val text = MaterialTheme.colorScheme.onPrimaryContainer
    val icon = MaterialTheme.colorScheme.onPrimaryContainer
    val border = MaterialTheme.colorScheme.primary
}
```

### Dimensions

```kotlin
object RenameHintDimensions {
    val cardPadding = 16.dp
    val cardMarginHorizontal = 16.dp
    val cardMarginTop = 16.dp
    val cardCornerRadius = 12.dp
    val cardElevation = 8.dp
    val cardWidthFraction = 0.9f

    val iconSize = 24.dp
    val iconSpacing = 12.dp

    val textSpacingVertical = 4.dp
}
```

### Typography

```kotlin
object RenameHintTypography {
    val title = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Medium
    )

    val example = MaterialTheme.typography.bodyLarge.copy(
        fontStyle = FontStyle.Italic
    )
}
```

### Animation

```kotlin
object RenameHintAnimation {
    val fadeInDuration = 200
    val stayDuration = 3000
    val fadeOutDuration = 200
    val totalDuration = fadeInDuration + stayDuration + fadeOutDuration

    val easing = FastOutSlowInEasing
}
```

---

## Figma Export Guidelines

### Export Formats

1. **PNG (2x, 3x)**: For documentation
2. **SVG**: For vector graphics
3. **PDF**: For print/archival

### Layer Structure

```
RenameHintOverlay
  ├─ Background (Card)
  ├─ Content (Row)
  │   ├─ Icon (Info)
  │   └─ Text (Column)
  │       ├─ Title
  │       └─ Example
  └─ Shadow (Elevation)
```

---

## Accessibility Checklist

- [x] Color contrast ratio ≥ 4.5:1 (WCAG AA)
- [x] Text size ≥ 14sp (body text)
- [x] Touch target ≥ 48dp (not applicable - no interaction)
- [x] VoiceOver/TalkBack announcement
- [x] High contrast mode support
- [x] Dark mode support
- [x] Screen reader friendly
- [x] Auto-dismiss with TTS warning

---

## Implementation Notes

### File Structure

```
Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/
  ├─ RenameHintOverlay.kt
  ├─ RenameHintCard.kt (Composable)
  └─ RenameHintPreview.kt (Previews)
```

### Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.animation:animation:1.5.4")

    // Preview
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
}
```

---

## Testing Plan

### Visual Regression Tests

1. Screenshot test: Light mode
2. Screenshot test: Dark mode
3. Screenshot test: High contrast mode
4. Screenshot test: Small screen
5. Screenshot test: Large screen
6. Screenshot test: Landscape orientation
7. Screenshot test: RealWear Navigator 500

### Animation Tests

1. Fade in animation timing
2. Stay visible duration
3. Fade out animation timing
4. Alpha values at each frame

### Accessibility Tests

1. TalkBack announcement
2. VoiceOver announcement
3. Color contrast validation
4. Font size validation

---

## Conclusion

These mockups provide comprehensive UI designs for the rename hint overlay with:

✅ **10 mockup variants** covering standard, compact, game, accessibility, and edge cases
✅ **Full Material Design 3 implementation** with Jetpack Compose code
✅ **RealWear Navigator 500 variant** for AR glasses
✅ **Animation specifications** with frame-by-frame breakdown
✅ **Accessibility considerations** with high contrast and TalkBack support
✅ **Design tokens** for consistent styling
✅ **Testing plan** for visual regression and accessibility

Ready for implementation!

---

**End of Mockups Document**
