# UI Guidelines - v2

**Enforced by:** Global CLAUDE.md > UI Framework
**Full Protocol:** `protocols/Protocol-Universal-UI-v1.0.md`

---

## Quick Reference

| Aspect | Rule |
|--------|------|
| Input Priority | Voice > Touch > Gaze > Gesture |
| Touch Targets | 48dp min (60pt spatial) |
| Voice Options | Max 3 per prompt |
| Command Hierarchy | Flat for voice, 2-3 levels for touch |
| Z-Axis Layers | HUD(0.5m) > Interactive(1m) > Primary(1.5m) > Ambient(3m) |
| Default Window | 1.5m distance, Primary layer |
| Accessibility | WCAG AAA + spatial |

---

## Device Tiers

| Tier | Devices | Primary Input |
|------|---------|---------------|
| Spatial | Smartglasses, XR headsets | Gaze, gesture, voice |
| Mobile | Phones, tablets | Touch, voice |
| Desktop | Laptops, monitors | Mouse, keyboard |
| Wearable | Watches, bands | Touch, voice |

---

## Platform SDK Mapping

| Platform | Framework | UI System |
|----------|-----------|-----------|
| Android | Jetpack Compose | Material3 / MagicUI |
| iOS | SwiftUI | HIG / MagicUI |
| Web | React + Tailwind | MagicUI Web |
| Desktop | Tauri + React | MagicUI Desktop |
| visionOS | SwiftUI + RealityKit | Spatial MagicUI |
| AndroidXR | Compose XR | Spatial MagicUI |
| Meta SDK | React Native / Unity | Spatial MagicUI |

---

## Input Context Switching

| Context | Priority | Reason |
|---------|----------|--------|
| Hands occupied | Voice > Gaze > Gesture | Driving, gloves |
| Noisy | Touch > Gesture > Gaze | Voice unreliable |
| Privacy | Gesture > Touch > Gaze | Silent |
| Accessibility | Voice > Gaze > Touch | Motor impairment |
| Spatial/XR | Gaze > Gesture > Voice | Natural |

---

## Decision Tree

| Question | Yes | No |
|----------|-----|-----|
| Cross-platform needed? | MagicUI | Check next |
| Platform-specific API? | Native | Check next |
| MagicUI component exists? | MagicUI | Native |

---

## UI Creation Pipeline

| Step | Interactive | YOLO |
|------|-------------|------|
| 1. Requirements | Q&A | Infer |
| 2. Research | Auto-search | Auto |
| 3. Design | Generate hierarchy | Generate |
| 4. ASCII Preview | Await approval | Skip |
| 5. Generate Code | After approval | Immediate |
| 6. HTML Demo | Ask user | Skip |
| 7. Verify | Check a11y | Check a11y |

---

## Voice-First Architecture

### Modes

| Mode | Trigger | Use Case |
|------|---------|----------|
| Always-on | Natural speech | Default (private) |
| Wake word | "Hey Ava, ..." | Shared/noisy |
| Push-to-talk | Button/gesture | Precision |

### State Machine

```
IDLE → LISTENING → PROCESSING → COMPLETED
                 ↓
              CLARIFYING
```

### VUI Rules

| Rule | Requirement |
|------|-------------|
| Max options | 3 per prompt |
| Hierarchy | Flat (top-level) |
| Feedback | Audio + visual + haptic |
| Fallback | Touch/gaze alternative |
| Offline | Core commands local |

### Intent Model

```yaml
command:
  intent: "navigate"
  utterances: ["go to {dest}", "open {dest}"]
  slots:
    dest: [settings, home, profile]
```

---

## Floating Command Bar

### Behavior Modes

| Mode | Trigger | Behavior | Duration |
|------|---------|----------|----------|
| Hidden | Default | Not visible | Until summoned |
| Head-locked | Voice/gesture | Follows gaze | 5s or dismiss |
| World-anchored | Pin command | Fixed in space | Until unpinned |
| Hybrid | Context | Starts locked, can pin | App pref |

### Spatial Positioning (XR)

| Position | Distance | Angle | Use |
|----------|----------|-------|-----|
| Primary | 1.5m | 0-15 below | Main |
| Secondary | 2m | 30 below | Less freq |
| Contextual | 0.5m | Adjacent | Object-specific |

### 2D Positioning

| Orientation | Position | Style |
|-------------|----------|-------|
| Portrait | Bottom center | Pill, 3-5 icons |
| Landscape | Right/bottom | Bar, 5-8 icons |
| Tablet port | Bottom center | Pill, 5-7 icons |
| Tablet land | Side rail | Full bar + labels |

### Hierarchy

| Level | Content | Max |
|-------|---------|-----|
| Domains | Nav, Actions, Media, Comms, Settings, AI | 6 |
| Categories | Per domain groupings | 6 |
| Actions | Specific commands | 6 |
| Contextual | AI-suggested | 3-5 |

### Display by Context

| Context | Mode | Max Visible |
|---------|------|-------------|
| Simple | Flat contextual | 3-5 |
| Complex | 2-level | 6 + submenu |
| Power user | 3-level | Full tree |
| Glasses HUD | Flat | 3 |

---

## Z-Axis Spatial Layer System

### Core Concept

```
USER (origin)
    │
    ├── 0.5m  → Immediate Layer (HUD, alerts)
    ├── 1.0m  → Interactive Layer (active window)
    ├── 1.5m  → Primary Layer (main content)
    ├── 2.0m  → Secondary Layer (supporting windows)
    ├── 3.0m  → Ambient Layer (background, context)
    └── ∞     → Environment Layer (world-anchored)
```

### Z-Layer Definitions

| Layer | Z-Index | Distance | Purpose | Interaction |
|-------|---------|----------|---------|-------------|
| HUD | 1000 | 0.3-0.5m | Alerts, status, always visible | Gaze only |
| Immediate | 900 | 0.5-0.8m | Tooltips, popovers, menus | Full |
| Interactive | 800 | 0.8-1.2m | Active/focused window | Full |
| Primary | 700 | 1.2-1.8m | Main content windows | Full |
| Secondary | 500 | 1.8-2.5m | Supporting/reference windows | Reduced |
| Ambient | 300 | 2.5-4.0m | Background info, dashboards | Gaze to activate |
| Environment | 100 | 4.0m+ | World-anchored, persistent | Walk to interact |

### Window Tracking State

```yaml
SpatialWindow:
  id: string
  position: {x, y, z}        # meters from origin
  rotation: {pitch, yaw, roll}
  size: {width, height}      # in points (UI) or meters (3D)
  layer: LayerType           # HUD | Interactive | Primary | etc
  zIndex: number             # within layer (0-100)
  anchor: AnchorType         # head | world | object
  focusState: FocusState     # focused | active | visible | hidden
  interactionMode: Mode      # full | reduced | gaze-only | disabled
```

### Depth Hierarchy Rules

| Principle | Implementation |
|-----------|----------------|
| Closer = Priority | Active tasks at Interactive layer |
| Distance = Importance | Critical closer, ambient farther |
| Focus advances Z | Focused window moves 0.2m closer |
| Blur recedes Z | Unfocused windows move 0.3m back |
| Occlusion handling | Semi-transparent when blocking |

### Z-Index Within Layers

| Element Type | Z-Index Range | Notes |
|--------------|---------------|-------|
| System alerts | 90-100 | Always on top |
| Modals/dialogs | 70-89 | Above content |
| Popovers/menus | 50-69 | Context menus |
| Active content | 30-49 | User focus |
| Passive content | 10-29 | Background |
| Decorative | 0-9 | Visual only |

### Distance Zones (Interaction)

| Zone | Distance | Behavior |
|------|----------|----------|
| Personal | 0-1m | Full interaction, high detail |
| Reach | 1-2m | Standard interaction |
| Social | 2-4m | Reduced detail, gaze to activate |
| Public | 4m+ | Overview only, walk to interact |

### Dynamic Z Management

| Event | Z-Axis Response |
|-------|-----------------|
| Window focus | Move to Interactive layer (0.8-1.2m) |
| Window blur | Return to original layer |
| New window | Place at Primary layer (1.5m default) |
| Alert/notification | HUD layer (0.5m), auto-dismiss |
| Menu open | Immediate layer, relative to parent |
| Window minimize | Fade + move to Ambient layer |
| Window close | Fade out at current position |

### Platform Implementation

#### visionOS (SwiftUI + RealityKit)

```swift
struct SpatialWindow: View {
    @State var zLayer: ZLayer = .primary

    var body: some View {
        WindowGroup {
            ContentView()
        }
        .defaultSize(width: 1280, height: 720, depth: 0, in: .points)
        .windowStyle(.plain)
    }
}

// RealityKit entity positioning
entity.position = SIMD3<Float>(x: 0, y: 1.5, z: -zLayer.distance)
```

#### Unity (World Space Canvas)

```csharp
public class SpatialLayerManager : MonoBehaviour {
    public enum ZLayer { HUD=1000, Interactive=800, Primary=700 }

    public void SetWindowLayer(Canvas canvas, ZLayer layer) {
        canvas.renderMode = RenderMode.WorldSpace;
        canvas.transform.position = new Vector3(
            canvas.transform.position.x,
            canvas.transform.position.y,
            -GetLayerDistance(layer)
        );
        canvas.sortingOrder = (int)layer;
    }
}
```

#### Compose XR (AndroidXR)

```kotlin
@Composable
fun SpatialWindow(
    layer: ZLayer = ZLayer.Primary,
    content: @Composable () -> Unit
) {
    Subspace(
        position = Position3D(z = -layer.distance),
        rotation = Rotation3D.Identity
    ) {
        SpatialPanel(
            modifier = Modifier.spatialElevation(layer.zIndex)
        ) {
            content()
        }
    }
}
```

#### 2D Fallback (elevation/shadow)

```kotlin
// Android Compose - use elevation as z-proxy
Surface(
    elevation = zLayer.toElevation(), // HUD=24dp, Interactive=16dp, etc
    modifier = Modifier.zIndex(zLayer.index.toFloat())
) { content() }
```

```swift
// SwiftUI - use zIndex + shadow
content
    .zIndex(Double(layer.zIndex))
    .shadow(radius: layer.shadowRadius)
```

### Collision & Occlusion

| Scenario | Resolution |
|----------|------------|
| Windows overlap | Back window 50% opacity |
| Window blocks HUD | HUD always renders on top |
| User walks into window | Window fades, moves aside |
| Multiple focus requests | Most recent wins, queue others |

### Coordinate System

| Platform | Origin | Units | Forward |
|----------|--------|-------|---------|
| visionOS | User feet | meters | -Z |
| Unity | World center | meters | +Z |
| Unreal | World center | cm | +X |
| AndroidXR | User head | meters | -Z |
| **Unified** | User chest | meters | -Z |

### Unified Spatial Coordinate

```
        +Y (up)
         │
         │
         │
    ─────┼─────→ +X (right)
        /│
       / │
      /  │
    +Z   (behind user)

    -Z = forward (user facing direction)
    Origin = user chest height (1.2m from floor)
```

---

## Orientation & Breakpoints

### Breakpoints

| Name | Width | Layout |
|------|-------|--------|
| Compact | < 600dp | Single column, bottom bar |
| Medium | 600-840dp | Two column opt, side bar |
| Expanded | > 840dp | Multi-column, side rail |

### Component Adaptation

| Component | Portrait | Landscape |
|-----------|----------|-----------|
| Command bar | Bottom pill | Right rail |
| Navigation | Bottom tabs | Side rail |
| Content | Single column | Multi-column |
| Dialogs | Full width | Centered 560dp |
| Sheets | Bottom | Side |

---

## Theming System

### Theme Switching Triggers

| Trigger | Behavior |
|---------|----------|
| User pref | Manual in settings |
| System | Follow OS dark/light |
| Time | Auto dark at night |
| Ambient | Adaptive brightness |
| XR env | Passthrough adapts |

### Switchable Systems

| System | Purpose |
|--------|---------|
| MagicUI | Cross-platform components |
| AvaMagic | AVA branding |
| MagicCode | Developer UI |
| Native | Platform default |

### Ocean Theme Tokens

| Token | Light | Dark |
|-------|-------|------|
| Surface | #FFFFFF | rgba(255,255,255,0.05-0.30) |
| Border | rgba(0,0,0,0.10) | rgba(255,255,255,0.10-0.30) |
| Primary | #3B82F6 | #60A5FA |
| Success | #10B981 | #34D399 |
| Error | #EF4444 | #F87171 |

---

## Accessibility

### WCAG AAA + Spatial

| Requirement | Target |
|-------------|--------|
| Contrast | 7:1 minimum |
| Touch targets | 48dp (60pt spatial) |
| Focus | Visible 3px / glow |
| Motion | Reduced option |
| Cognitive | Max 3 options |

### Spatial Features

| Feature | Implementation |
|---------|----------------|
| Spatial audio | Directional cues |
| Haptic | Vibration patterns |
| Gaze alt | Head tracking, switch |
| Voice desc | Audio labels |
| Depth cues | Z-ordering, shadows |

### Multi-Modal Feedback

| Action | Visual | Audio | Haptic |
|--------|--------|-------|--------|
| Press | Highlight + ripple | Click | Short pulse |
| Accept | Checkmark | Confirm tone | Double pulse |
| Error | Red flash | Error tone | Long vibe |
| Navigate | Transition | Swoosh | Light pulse |
| Listening | Mic indicator | Listen tone | Continuous |

---

## Command Registry

### Structure (Static + Dynamic + AI)

```yaml
commands:
  - id: nav.home
    domain: navigation
    voice: ["go home", "home screen"]
    icon: home
    children: [...]
```

### AI Suggestion Weights

| Signal | Weight |
|--------|--------|
| Recent usage | 0.3 |
| Current screen | 0.3 |
| User patterns | 0.2 |
| Time of day | 0.1 |
| Location | 0.1 |

---

## State Sync

### Architecture

| Layer | Method |
|-------|--------|
| Local-first | Room / CoreData / IndexedDB |
| P2P (AvaConnect) | BLE, WiFi Direct, LAN |
| Cloud | WebSocket, REST delta |

### Sync Targets

| Data | Method | Latency |
|------|--------|---------|
| UI state | P2P | < 100ms |
| Preferences | P2P + Cloud | < 500ms |
| Content | Cloud | < 2s |

### Offline

| Feature | Capability |
|---------|------------|
| Voice | Core commands local |
| Navigation | Cached screens |
| Data entry | Queue for sync |
| AI | Static fallback |

---

## Component Mapping

| Need | MagicUI | Compose | SwiftUI |
|------|---------|---------|---------|
| Card | GlassmorphicSurface | Card | GroupBox |
| List | DataTable | LazyColumn | List |
| Toggle | MagicSwitch | Switch | Toggle |
| Dialog | OceanDialog | AlertDialog | .alert |
| Toast | OceanToast | Snackbar | .toast |
| CommandBar | UniversalCommandBar | - | - |

---

## Platform Code

### Compose
```kotlin
@Composable
fun UniversalCommandBar(
    commands: List<Command>,
    orientation: Orientation
) {
    MagicSurface(elevation = MagicElevation.CommandBar) {
        CommandBarContent(commands, rememberLayout(orientation))
    }
}
```

### SwiftUI
```swift
struct UniversalCommandBar: View {
    let commands: [Command]
    @Environment(\.horizontalSizeClass) var sizeClass
    var body: some View {
        MagicSurface { CommandBarContent(commands: commands) }
            .accessibilityLabel("Command bar")
    }
}
```

### React
```tsx
const UniversalCommandBar: FC<{commands: Command[]}> = ({commands}) => {
    const layout = useCommandBarLayout();
    return <MagicSurface role="toolbar">{...}</MagicSurface>;
};
```

---

## MagicUI Migration Strategy

### Pre-MagicUI Implementation Rules

When implementing UI before MagicUI is ready:

| Rule | Requirement | Rationale |
|------|-------------|-----------|
| Ocean Theme First | Apply glassmorphic styling NOW | Consistent look immediately |
| 1:1 Component Mapping | Name components to match MagicUI | Easy swap later |
| Abstraction Layer | Wrap native components | Minimal code changes |
| Semantic Names | Use MagicUI naming conventions | Direct replacement |

### Component Wrapper Pattern

```kotlin
// Pre-MagicUI: Wraps native, applies Ocean theme
@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // TODAY: Native Surface with Ocean theme
    Surface(
        modifier = modifier,
        color = OceanTheme.glassSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OceanTheme.glassBorder)
    ) { content() }

    // FUTURE: Replace with single line
    // MagicSurface(modifier) { content() }
}
```

### Migration Checklist

| Step | Action | Effort |
|------|--------|--------|
| 1 | Create wrapper composables/views | Once |
| 2 | Apply Ocean theme tokens | Per component |
| 3 | Use semantic component names | Per usage |
| 4 | When MagicUI ready: swap imports | Per file |
| 5 | Remove wrappers | Optional cleanup |

### Pre-MagicUI Component Naming

| Pre-MagicUI (Now) | MagicUI (Future) | Notes |
|-------------------|------------------|-------|
| GlassmorphicSurface | MagicSurface | Glassmorphic card |
| OceanButton | MagicButton | Primary actions |
| OceanTextField | MagicTextField | Text input |
| OceanDialog | MagicDialog | Modal dialogs |
| OceanToast | MagicToast | Notifications |
| OceanSwitch | MagicSwitch | Toggle switches |
| UniversalCommandBar | MagicCommandBar | Floating nav |

### Ocean Theme Glassmorphic Tokens (Use NOW)

```kotlin
object OceanTheme {
    // Surfaces
    val glassSurface = Color.White.copy(alpha = 0.08f)
    val glassSurfaceHover = Color.White.copy(alpha = 0.12f)
    val glassSurfacePressed = Color.White.copy(alpha = 0.16f)

    // Borders
    val glassBorder = Color.White.copy(alpha = 0.15f)
    val glassBorderFocus = Color.White.copy(alpha = 0.30f)

    // Blur
    val glassBlur = 20.dp

    // Shadows
    val glassShadow = Color.Black.copy(alpha = 0.25f)
}
```

### Swap Process (When MagicUI Ready)

1. **Add MagicUI dependency**
2. **Find & replace imports:**
   ```kotlin
   // Before
   import com.yourapp.ui.GlassmorphicSurface
   // After
   import com.magicui.MagicSurface
   ```
3. **Remove wrapper files** (optional, for cleanup)
4. **Test visual parity**

---

## Quality Gates

| Gate | Requirement | Blocking |
|------|-------------|----------|
| Voice accuracy | 95%+ | YES |
| Touch targets | 48dp verified | YES |
| Orientation | All breakpoints | YES |
| A11y audit | Zero critical | YES |
| Theme switch | No regression | YES |
| Sync latency | P2P<100ms, Cloud<2s | NO |
| Offline | Core works | YES |
| MagicUI naming | 1:1 mapping | YES |
| Ocean theme | Glassmorphic applied | YES |

---

## Full Specs

| Document | Location |
|----------|----------|
| Universal Protocol | `protocols/Protocol-Universal-UI-v1.0.md` |
| Theming Protocol | `protocols/Protocol-UI-Theming-Architecture-v1.0.md` |
| Design System | `Avanues/docs/universal/LD-magicui-design-system.md` |

---

**Updated:** 2025-12-01
