# AI Prompt: Generate AVA Overlay UI Implementation

**Purpose**: Instructions for another AI model to generate the complete Jetpack Compose UI code for AVA's transparent overlay system.

---

## Context for AI

You are a senior Android developer implementing a transparent AI overlay using Jetpack Compose and Material3. The overlay floats over any running app on Android, providing voice-first AI assistance with a glassmorphic VisionOS-inspired design.

**Project**: AVA AI Assistant (Android)
**Framework**: Jetpack Compose + Material3
**Architecture**: Clean Architecture with MVVM
**Theme**: Glassmorphic / VisionOS aesthetic

---

## Required Components to Generate

Generate **complete, production-ready** Kotlin files for:

### 1. VoiceOrb.kt - Draggable Microphone Bubble

**Location**: `features/overlay/src/main/java/com/augmentalis/ava/features/overlay/ui/VoiceOrb.kt`

**Requirements**:
```kotlin
@Composable
fun VoiceOrb(
    position: Offset,              // Current screen position
    state: OrbState,               // Idle, Listening, Processing, Speaking
    onTap: () -> Unit,             // Tap to activate voice
    onDrag: (Offset) -> Unit,      // Drag to reposition
    modifier: Modifier = Modifier
)
```

**Visual Specs**:
- **Size**: 64dp diameter circle
- **Background**: Translucent glass with 0.8 alpha
  ```kotlin
  color = Color(0x1E, 0x1E, 0x20).copy(alpha = 0.8f)
  blur = 24.dp  // Apply blur effect
  ```
- **Border**: 1dp white stroke with 20% opacity
- **Shadow**: Elevation 8dp, ambient shadow
- **Icon**: Material Icons.Filled.Mic, white color
- **Animation States**:
  - `Idle`: Gentle pulse (scale 1.0 → 1.05 → 1.0, 2s infinite)
  - `Listening`: Waveform animation (3 bars pulsing)
  - `Processing`: Rotating spinner
  - `Speaking`: Pulsing glow (blur radius animates 20-30dp)

**Drag Behavior**:
```kotlin
.pointerInput(Unit) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        onDrag(dragAmount)
    }
}
```

**Example Code Structure**:
```kotlin
Box(
    modifier = modifier
        .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
        .size(64.dp)
        .clip(CircleShape)
        .background(/* glass effect */)
        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        .clickable { onTap() }
        .pointerInput(Unit) { /* drag handling */ },
    contentAlignment = Alignment.Center
) {
    when (state) {
        OrbState.Idle -> PulsingMicIcon()
        OrbState.Listening -> WaveformAnimation()
        OrbState.Processing -> RotatingSpinner()
        OrbState.Speaking -> PulsingGlow()
    }
}
```

---

### 2. GlassMorphicPanel.kt - Expandable Glass Card

**Location**: `features/overlay/src/main/java/com/augmentalis/ava/features/overlay/ui/GlassMorphicPanel.kt`

**Requirements**:
```kotlin
@Composable
fun GlassMorphicPanel(
    expanded: Boolean,             // Collapse/expand state
    title: String,                 // Panel header text
    content: @Composable () -> Unit, // Main content slot
    suggestions: @Composable () -> Unit, // Suggestion chips
    onClose: () -> Unit,           // Close button callback
    modifier: Modifier = Modifier
)
```

**Visual Specs**:
- **Width**: MATCH_PARENT - 32dp horizontal margin
- **Height**: WRAP_CONTENT (max 60% screen height)
- **Background**: Translucent glass with 0.7 alpha
  ```kotlin
  color = Color(0x1E, 0x1E, 0x20).copy(alpha = 0.7f)
  blur = 28.dp
  ```
- **Corner Radius**: 24.dp all corners
- **Border**: 1dp white 15% opacity
- **Shadow**: 0dp offset, 10dp blur, 25% black
- **Padding**: 16dp all sides
- **Animation**: Expand/collapse with 220ms ease-out

**Layout Structure**:
```kotlin
AnimatedVisibility(
    visible = expanded,
    enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)),
    exit = shrinkVertically(animationSpec = tween(180))
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(/* glass effect */)
            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        // Title bar (56dp height)
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Assistant, null, tint = Color.White.copy(0.9f))
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(0.95f)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, null, tint = Color.White.copy(0.7f))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Content area
        Box(modifier = Modifier.weight(1f, fill = false)) {
            content()
        }

        Spacer(Modifier.height(12.dp))

        // Suggestions row
        suggestions()
    }
}
```

---

### 3. SuggestionChips.kt - Action Chips

**Location**: `features/overlay/src/main/java/com/augmentalis/ava/features/overlay/ui/SuggestionChips.kt`

**Requirements**:
```kotlin
@Composable
fun SuggestionChipsRow(
    suggestions: List<Suggestion>,  // List of actions
    onSuggestionClick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier
)

data class Suggestion(
    val label: String,              // "Copy", "Translate", etc.
    val icon: ImageVector? = null,  // Optional icon
    val action: String              // Action identifier
)
```

**Visual Specs**:
- **Chip Style**: Material3 AssistChip
- **Height**: 32dp
- **Padding**: 12dp horizontal, 6dp vertical
- **Background**: Transparent
- **Border**: Accent color (Material3 primary) with 70% opacity, 1dp width
- **Text**: Body small, white 90% opacity
- **Spacing**: 8dp horizontal gap between chips
- **Layout**: FlowRow (wraps if needed, max 2 rows)

**Example Implementation**:
```kotlin
FlowRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    suggestions.forEach { suggestion ->
        AssistChip(
            onClick = { onSuggestionClick(suggestion) },
            label = {
                Text(
                    suggestion.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.9f)
                )
            },
            leadingIcon = suggestion.icon?.let { icon ->
                {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(0.8f)
                    )
                }
            },
            border = AssistChipDefaults.assistChipBorder(
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                borderWidth = 1.dp
            ),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color.Transparent,
                labelColor = Color.White.copy(0.9f)
            )
        )
    }
}
```

---

### 4. GlassEffects.kt - Reusable Modifiers

**Location**: `features/overlay/src/main/java/com/augmentalis/ava/features/overlay/theme/GlassEffects.kt`

**Requirements**:
Generate modifier extensions for glassmorphic effects:

```kotlin
/**
 * Apply translucent glass background with blur
 */
fun Modifier.glassBackground(
    color: Color = Color(0x1E, 0x1E, 0x20),
    alpha: Float = 0.7f,
    blurRadius: Dp = 24.dp
): Modifier

/**
 * Apply glass border (1dp white with low opacity)
 */
fun Modifier.glassBorder(
    shape: Shape = RoundedCornerShape(24.dp),
    borderAlpha: Float = 0.15f
): Modifier

/**
 * Apply glass shadow (soft elevation)
 */
fun Modifier.glassShadow(
    elevation: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier

/**
 * Full glass effect (combines all above)
 */
fun Modifier.glassEffect(
    color: Color = Color(0x1E, 0x1E, 0x20),
    alpha: Float = 0.7f,
    blurRadius: Dp = 24.dp,
    borderAlpha: Float = 0.15f,
    elevation: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier = this
    .shadow(elevation, shape, ambientColor = Color.Black.copy(0.25f))
    .clip(shape)
    .background(color.copy(alpha = alpha))
    .border(1.dp, Color.White.copy(borderAlpha), shape)
```

**Note on Blur**: Since Compose doesn't have native blur yet, use:
```kotlin
// For now, simulate blur with layered semi-transparent boxes
// Or use RenderEffect on Android 12+ via graphicsLayer
modifier.graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        renderEffect = RenderEffect.createBlurEffect(
            blurRadius.toPx(),
            blurRadius.toPx(),
            Shader.TileMode.CLAMP
        )
    }
}
```

---

### 5. AnimationSpecs.kt - Motion Design

**Location**: `features/overlay/src/main/java/com/augmentalis/ava/features/overlay/theme/AnimationSpecs.kt`

**Requirements**:
Define reusable animation specifications:

```kotlin
object OverlayAnimations {
    // Panel expand/collapse
    val panelExpand = tween<IntSize>(
        durationMillis = 220,
        easing = FastOutSlowInEasing
    )

    val panelCollapse = tween<IntSize>(
        durationMillis = 180,
        easing = LinearOutSlowInEasing
    )

    // Orb pulse (idle state)
    val orbPulse = infiniteRepeatable<Float>(
        animation = tween(2000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )

    // Fade animations
    val fadeIn = tween<Float>(
        durationMillis = 150,
        easing = LinearEasing
    )

    val fadeOut = tween<Float>(
        durationMillis = 100,
        easing = LinearEasing
    )

    // Rotation (for processing spinner)
    val spinnerRotation = infiniteRepeatable<Float>(
        animation = tween(1000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )

    // Glow pulse (speaking state)
    val glowPulse = infiniteRepeatable<Float>(
        animation = tween(800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
}
```

---

### 6. OverlayComposables.kt - Root UI

**Location**: `features/overlay/src/main/java/com/augmentalis/ava/features/overlay/ui/OverlayComposables.kt`

**Requirements**:
Compose all components into the main overlay UI:

```kotlin
@Composable
fun OverlayRootUI(
    controller: OverlayController,
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsState()
    val position by controller.orbPosition.collectAsState()
    val expanded by controller.expanded.collectAsState()
    val transcript by controller.transcript.collectAsState()
    val response by controller.response.collectAsState()
    val suggestions by controller.suggestions.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // Main glass panel (expanded state)
        GlassMorphicPanel(
            expanded = expanded,
            title = "AVA Assistant",
            onClose = { controller.collapse() },
            content = {
                // Show transcript or response
                Text(
                    text = response ?: transcript ?: "Listening... say a command",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(0.9f)
                )
            },
            suggestions = {
                SuggestionChipsRow(
                    suggestions = suggestions,
                    onSuggestionClick = { controller.executeSuggestion(it) }
                )
            }
        )

        // Voice orb (always visible)
        VoiceOrb(
            position = position,
            state = state.toOrbState(),
            onTap = {
                if (expanded) controller.collapse()
                else controller.startListening()
            },
            onDrag = { offset ->
                controller.updateOrbPosition(offset)
            }
        )

        // Hint tooltip when collapsed
        if (!expanded) {
            Box(
                modifier = Modifier
                    .offset(x = position.x + 70.dp, y = position.y + 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(0.35f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Hey AVA",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.9f)
                )
            }
        }
    }
}
```

---

## Code Generation Guidelines

When generating the code, follow these rules:

### File Header Template
Every file MUST start with:
```kotlin
// filename: [relative path from project root]
// created: 2025-11-01 22:00:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase [1-4] - [Component name]
// agent: Engineer | mode: ACT

package com.augmentalis.ava.features.overlay.[subpackage]

// imports in alphabetical order
```

### Compose Best Practices
- Use `remember { }` for state that survives recomposition
- Use `LaunchedEffect` for side effects
- Collect StateFlows with `collectAsState()`
- Apply `@Composable` annotation to all composable functions
- Use `@Preview` for preview functions
- Follow Material3 theming (don't hardcode colors except for glass effects)

### Performance
- Keep composable functions small and focused
- Avoid heavy computations in composables
- Use `derivedStateOf` for computed values
- Implement `key()` for LazyColumn items
- Use `remember` with proper keys

### Accessibility
- Provide `contentDescription` for all icons
- Use semantic modifiers for screen readers
- Support TalkBack navigation
- Ensure 44dp minimum touch targets

### Testing
- Generate preview composables for each component
- Include example data in previews
- Test both light and dark theme (even though overlay is transparent)

---

## Expected Output

Generate **6 complete Kotlin files**:

1. `VoiceOrb.kt` (~200 lines)
2. `GlassMorphicPanel.kt` (~150 lines)
3. `SuggestionChips.kt` (~100 lines)
4. `GlassEffects.kt` (~80 lines)
5. `AnimationSpecs.kt` (~60 lines)
6. `OverlayComposables.kt` (~120 lines)

**Total**: ~710 lines of production-ready, tested, documented Kotlin code.

---

## Validation Checklist

After generating, the code must:

- [ ] Compile without errors in Android Studio
- [ ] Match the visual specifications exactly (colors, sizes, borders)
- [ ] Implement all required animations smoothly
- [ ] Handle edge cases (null states, empty lists)
- [ ] Include KDoc comments for all public functions
- [ ] Follow Kotlin coding conventions
- [ ] Use proper Material3 components
- [ ] Support Android API 24+ (minSdk)
- [ ] Have no hardcoded strings (use string resources)
- [ ] Be production-ready (no TODOs or placeholder logic)

---

## Example Invocation

**AI Prompt**:
```
Generate the 6 Kotlin Compose files for AVA's overlay UI system
following the specifications in OVERLAY-UI-GENERATION-PROMPT.md.

Requirements:
- Use Jetpack Compose + Material3
- Glassmorphic VisionOS aesthetic
- Follow IdeaCode file header protocol
- Complete, production-ready code with no placeholders
- Include preview composables

Output format: One file per code block, with filename as heading.
```

---

## Integration After Generation

Once files are generated, they will be integrated with:

1. **OverlayController.kt** (state management)
   - Provides StateFlows for UI to observe
   - Handles user interactions
   - Manages overlay lifecycle

2. **OverlayService.kt** (Android service)
   - Inflates the Compose UI in a WindowManager overlay
   - Manages service lifecycle
   - Handles permissions

3. **AvaIntegrationBridge.kt** (AI integration)
   - Connects voice input to NLU classifier
   - Fetches AI responses from ChatViewModel
   - Generates contextual suggestions

---

## Success Criteria

The generated UI is successful when:

✅ Overlay renders transparently over other apps
✅ Voice orb is draggable and animates correctly
✅ Panel expands/collapses smoothly
✅ Glass effect looks professional (blur, border, shadow)
✅ Suggestion chips are interactive
✅ Animations run at 60fps on mid-range devices
✅ Code compiles and passes lint checks
✅ Matches AVA's existing chat UI aesthetic

---

**End of AI Generation Prompt**

*This document provides complete specifications for another AI model to generate the overlay UI code. All visual design, animation timing, and component structure is fully specified.*
