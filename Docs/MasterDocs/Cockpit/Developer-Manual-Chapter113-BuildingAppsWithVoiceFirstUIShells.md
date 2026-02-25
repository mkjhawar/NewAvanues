# Developer Manual: Chapter 113
## Building Apps with Voice-First UI Shells

**Table of Contents**
1. [Introduction](#1-introduction)
2. [Quick Start](#2-quick-start)
3. [Shell Selection](#3-shell-selection)
4. [ArrangementIntent API](#4-arrangementintent-api)
5. [ContextualActionProvider](#5-contextualactionprovider)
6. [Adding a New Shell](#6-adding-a-new-shell)
7. [Adding Content Types](#7-adding-content-types)
8. [Voice Command Integration](#8-voice-command-integration)
9. [Settings Integration](#9-settings-integration)
10. [Glass Optimization Checklist](#10-glass-optimization-checklist)
11. [Template Code](#11-template-code)
12. [Platform Matrix](#12-platform-matrix)
13. [Deep Link Support](#13-deep-link-support)
14. [Session Templates](#14-session-templates)
15. [Testing](#15-testing)
16. [Troubleshooting](#16-troubleshooting)
17. [Migration from CommandBar](#17-migration-from-commandbar)
18. [Cognitive Load Comparison](#18-cognitive-load-comparison)

---

## 1. Introduction

The **Voice-First UI Shell System** is a simplified, intent-based interface layer that abstracts the raw complexity of Cockpit's 15 layout modes and hierarchical command structures into four natural interaction paradigms: **Focus**, **Compare**, **Overview**, and **Present**.

### Design Philosophy

Instead of asking users to navigate menu hierarchies or choose from 15 different layout modes, the system presents:

- **Four intention-based arrangements** (ArrangementIntent) that map naturally to voice commands
- **Single-level action bars** (ContextualActionProvider) that avoid nested menu navigation
- **Three distinct shells** (SimplifiedShellMode) optimized for different user personas and devices
- **Content-type-aware actions** that auto-adapt based on what's currently displayed

This is the **voice-first evolution** of the 260222-260223 Cockpit redesign: the original Dashboard + CommandBar remain as CLASSIC mode for backward compatibility, while three new shells provide increasingly sophisticated alternatives.

### Key Principles

| Principle | Rationale |
|-----------|-----------|
| **Voice Before UI** | Every command must be accessible via voice first; UI is secondary |
| **Intent Over Action** | Users express intent (focus, compare); the system resolves to layout/action |
| **Single-Level Navigation** | No multi-level hierarchies — top actions visible, "More" for advanced |
| **Adaptive Resolution** | Same intent resolves differently on phone vs. tablet vs. glass vs. desktop |
| **Backward Compatible** | CLASSIC mode preserves the original 15-layout, hierarchical experience |

---

## 2. Quick Start

### Minimum Setup (5 Minutes)

1. **Create an app that extends Cockpit**

```kotlin
@Composable
fun MyVoiceApp() {
    val viewModel: CockpitScreenViewModel = hiltViewModel()
    val state by viewModel.screenState.collectAsState()

    CockpitScreenContent(
        state = state,
        onNavigateBack = { /* nav.popBackStack() */ },
        onReturnToDashboard = { viewModel.loadDashboard() },
        onFrameSelected = { viewModel.selectFrame(it) },
        onFrameMoved = { id, x, y -> viewModel.moveFrame(id, x, y) },
        onFrameResized = { id, w, h -> viewModel.resizeFrame(id, w, h) },
        onFrameClose = { viewModel.closeFrame(it) },
        onLayoutModeChanged = { viewModel.changeLayoutMode(it) },
        onAddFrame = { content, frameId -> viewModel.addFrame(content, frameId) },
        frameContent = { frame ->
            when (frame.content) {
                is FrameContent.Web -> WebRenderer(frame.content)
                is FrameContent.Note -> NoteRenderer(frame.content)
                is FrameContent.Camera -> CameraRenderer(frame.content)
                else -> PlaceholderRenderer(frame.content.typeId)
            }
        }
    )
}
```

2. **Set the default shell in DataStore**

```kotlin
// In your app's MainViewModel or initialization code
val settings = repository.updateSetting(
    SettingsKeys.SHELL_MODE,
    SimplifiedShellMode.LENS.name  // or AVANUE_VIEWS, CANVAS
)
```

3. **Add voice commands for the 4 intents**

```kotlin
// In your VoiceOSCore command registration
registerStaticCommand(
    phrase = "focus",
    action = { intent: ArrangementIntent ->
        val layout = IntentResolver.resolve(
            ArrangementIntent.FOCUS,
            frameCount = frames.size,
            displayProfile = getCurrentDisplayProfile()
        )
        updateLayout(layout)
    }
)
// Repeat for COMPARE, OVERVIEW, PRESENT
```

Done. Your app now has voice-controlled layout switching + context-aware action bars.

---

## 3. Shell Selection

The **Shell Mode** setting determines how the home/launcher screen is presented and how navigation flows.

### The Four Shells

#### 3.1 CLASSIC

**Display Label:** "Classic"
**Description:** Traditional dashboard with module tiles and command bar

**Best For:**
- Power users who want full control
- Developers debugging multi-window layouts
- Legacy apps migrating from earlier Cockpit versions

**Behavior:**
- Home screen shows 12-16 module tiles (always visible)
- Active session displays 15 layout mode options
- CommandBar navigates through MAIN → FRAME_ACTIONS → WEB_ACTIONS hierarchy
- Voice commands access all modes directly

**Voice Accessibility:**
- Commands: "focus", "compare", "overview", "present", "grid", "mosaic", "carousel", "freeform", etc.
- All 15 layout names are voice-accessible

#### 3.2 AVANUE_VIEWS

**Display Label:** "AvanueViews"
**Description:** Ambient card stream — context-aware, minimal

**Best For:**
- Casual users / first-time users
- Smart glasses with glance-based interaction
- Ambient computing (information surface)

**Behavior:**
- Home screen is an infinite vertical scroll of "cards" (module shortcuts, recent sessions, suggestions)
- Cards surface based on: recency, frequency, context relevance, pinned status
- Active session shows 4 top actions inline, "More" bottom sheet for full action list
- No explicit layout mode UI — always uses OVERVIEW or MOSAIC

**Voice Accessibility:**
- Commands: "show recent", "show suggestions", "open [module name]", focus/compare/overview/present intents
- No "grid" or "mosaic" voice commands — user asks for intent, system resolves to best layout

#### 3.3 LENS

**Display Label:** "Lens"
**Description:** Universal command palette — one search bar for everything

**Best For:**
- Power users / keyboard warriors
- Desktop & productivity workflows
- Users who want "quick command" discovery

**Behavior:**
- Home screen is a single universal search bar (Spotlight-like)
- Search queries match against: module names, recent sessions, templates, voice commands, actions
- Active session shows 4 top actions + search bar for action filtering
- Voice commands activate search naturally ("search for [term]", "open [module]")

**Voice Accessibility:**
- Commands: "search [term]", "open [module]", focus/compare/overview/present, action search
- Best for power-users who remember command names

#### 3.4 CANVAS

**Display Label:** "Canvas"
**Description:** Spatial zen canvas — zoom to navigate, organic layout

**Best For:**
- Creative workers / designers
- Tablet users with stylus input
- Spatial computing / AR glasses (future)

**Behavior:**
- Home screen is an infinite zoomable canvas with module "islands" (floating, draggable)
- Zoom out to see all modules; zoom in to individual modules
- Active session displays frames in FREEFORM or SPATIAL_DICE layout
- Gesture-driven (pinch zoom, drag) with voice commands for semantic zoom

**Voice Accessibility:**
- Commands: "zoom in", "zoom out", "zoom to fit", focus/compare/overview/present
- Spatial voice commands: "arrange in grid", "arrange in mosaic"

### 3.5 Switching Shells at Runtime

```kotlin
// From settings UI or voice command handler
private val settingsRepository: AvanuesSettingsRepository

fun changeShell(newShell: SimplifiedShellMode) {
    viewModelScope.launch {
        settingsRepository.updateSetting(
            SettingsKeys.SHELL_MODE,
            newShell.name
        )
        // UI responds automatically via StateFlow collection
        _shellMode.value = newShell
        // Optionally show toast
        feedbackMessage.emit("Switched to ${newShell.displayLabel}")
    }
}
```

### 3.6 DataStore Wiring

**Key:** `SettingsKeys.SHELL_MODE`
**Type:** String (enum name)
**Default:** `SimplifiedShellMode.LENS.name`
**Persistence:** AvanuesSettingsRepository (Android DataStore / iOS UserDefaults / Desktop Preferences)

**Reading the setting:**

```kotlin
class CockpitViewModel @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ViewModel() {

    val currentShell: StateFlow<SimplifiedShellMode> =
        repository.observeSetting(SettingsKeys.SHELL_MODE)
            .map { str -> SimplifiedShellMode.fromString(str) }
            .stateIn(viewModelScope, SharingStarted.Lazily, SimplifiedShellMode.LENS)
}
```

---

## 4. ArrangementIntent API

The **ArrangementIntent** enum is the core abstraction bridge between user intention and layout resolution.

### 4.1 The Four Intents

```kotlin
@Serializable
enum class ArrangementIntent(
    val displayLabel: String,
    val voiceCommand: String,
    val iconName: String,
    val description: String,
) {
    FOCUS(
        displayLabel = "Focus",
        voiceCommand = "focus",
        iconName = "fullscreen",
        description = "Single frame fills the screen"
    ),
    COMPARE(
        displayLabel = "Compare",
        voiceCommand = "compare",
        iconName = "vertical_split",
        description = "Two frames side by side"
    ),
    OVERVIEW(
        displayLabel = "Overview",
        voiceCommand = "overview",
        iconName = "grid_view",
        description = "All frames in an auto-arranged grid"
    ),
    PRESENT(
        displayLabel = "Present",
        voiceCommand = "present",
        iconName = "slideshow",
        description = "Showcase mode for presentations"
    );
}
```

### 4.2 IntentResolver

The **IntentResolver** object resolves an intention to the optimal layout mode based on device context.

**Signature:**

```kotlin
object IntentResolver {
    fun resolve(
        intent: ArrangementIntent,
        frameCount: Int,
        displayProfile: DisplayProfile = DisplayProfile.PHONE,
        spatialAvailable: Boolean = false,
    ): LayoutMode

    fun inferIntent(layoutMode: LayoutMode): ArrangementIntent
}
```

**Resolution Rules:**

| Intent | Frame Count | Display Profile | Result |
|--------|-------------|-----------------|--------|
| FOCUS | any | any | FULLSCREEN |
| COMPARE | 1 | any | FULLSCREEN (fallback) |
| COMPARE | 2+ | PHONE | T_PANEL |
| COMPARE | 2+ | TABLET/DESKTOP | SPLIT_LEFT |
| COMPARE | 2+ | GLASS | SPLIT_LEFT |
| OVERVIEW | 1 | any | FULLSCREEN |
| OVERVIEW | 2 | any | SPLIT_LEFT or T_PANEL |
| OVERVIEW | 3 | any | MOSAIC |
| OVERVIEW | 4 | TABLET+ | GRID |
| OVERVIEW | 5 | spatial=true | SPATIAL_DICE |
| OVERVIEW | 5+ | any | MOSAIC or GRID |
| PRESENT | 1 | GLASS | FULLSCREEN |
| PRESENT | 3 | any | TRIPTYCH |
| PRESENT | 2+ | TABLET+ | CAROUSEL |

**Usage:**

```kotlin
// In your voice command handler or UI button click
val layoutMode = IntentResolver.resolve(
    intent = ArrangementIntent.OVERVIEW,
    frameCount = state.frames.size,
    displayProfile = DisplayProfile.TABLET,
    spatialAvailable = isHeadTrackingEnabled()
)
updateLayoutMode(layoutMode)
```

**Inferring Intent from Existing Layout:**

```kotlin
// When user manually selects a layout mode, infer the intent for UI display
val currentLayout = LayoutMode.MOSAIC
val intent = IntentResolver.inferIntent(currentLayout)
// intent == ArrangementIntent.OVERVIEW
```

### 4.3 Voice Commands

All four intent commands should be registered in your voice handler:

```kotlin
class LayoutIntentHandler @Inject constructor(
    private val viewModel: CockpitScreenViewModel,
) : IHandler {

    override fun canHandle(category: ActionCategory): Boolean =
        category == ActionCategory.LAYOUT_INTENT

    override suspend fun handle(command: StaticCommand): HandlerResult {
        val intent = ArrangementIntent.fromVoiceCommand(command.phrase) ?:
            return HandlerResult.Unhandled

        val layout = IntentResolver.resolve(
            intent = intent,
            frameCount = viewModel.frameCount,
            displayProfile = viewModel.displayProfile,
            spatialAvailable = viewModel.isSpatialAvailable
        )

        viewModel.updateLayoutMode(layout)
        return HandlerResult.Success("Changed to ${intent.displayLabel}")
    }
}
```

---

## 5. ContextualActionProvider

The **ContextualActionProvider** object delivers content-aware actions for the simplified action bar. Instead of navigating a hierarchy, users see 4-6 top actions inline, with a "More" option for full discovery.

### 5.1 Architecture

```kotlin
object ContextualActionProvider {

    // 5-6 most-used actions shown inline
    fun topActionsForContent(contentTypeId: String): List<QuickAction>

    // All actions grouped by category (Content, Frame, Layout, Tools)
    fun allActionsForContent(contentTypeId: String): List<ActionGroup>

    // Search across all actions (used by Lens shell)
    fun searchActions(contentTypeId: String, query: String): List<QuickAction>
}
```

### 5.2 Top Actions by Content Type

| Content Type | Top Actions |
|--------------|------------|
| **WEB** | Back, Forward, Refresh, Zoom In, Zoom Out |
| **PDF** | Prev Page, Next Page, Zoom In, Zoom Out, Search |
| **IMAGE** | Zoom In, Zoom Out, Rotate, Share |
| **VIDEO** | Rewind, Play/Pause, Forward, Fullscreen |
| **NOTE** | Bold, Italic, Underline, Undo, Redo, Save |
| **CAMERA** | Flip, Capture, Flash |
| **WHITEBOARD** | Pen, Highlight, Eraser, Undo, Redo, Clear |
| **TERMINAL** | Clear, Copy, Scroll Top, Scroll Bottom |
| **MAP** | Zoom In, Zoom Out, Center, Layers |
| **VOICE_NOTE** | Record, Stop, Playback, Save |
| **FORM** | Save, Submit, Clear, Previous, Next |
| **Other** | Minimize, Maximize, Close |

### 5.3 Using in Your UI

```kotlin
@Composable
fun SimplifiedCommandBar(
    contentTypeId: String,
    onActionSelected: (QuickAction) -> Unit
) {
    val topActions = remember(contentTypeId) {
        ContextualActionProvider.topActionsForContent(contentTypeId)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AvanueTheme.colors.surface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        topActions.forEach { action ->
            IconButton(
                onClick = { onActionSelected(action) },
                modifier = Modifier.semantics {
                    contentDescription = "Voice: click ${action.label}"
                }
            ) {
                Icon(
                    painter = painterResource(action.iconName),
                    contentDescription = null,
                    tint = AvanueTheme.colors.textPrimary
                )
            }
        }

        // "More" button opens full action sheet
        IconButton(
            onClick = { /* showMoreActionsSheet(contentTypeId) */ },
            modifier = Modifier.semantics {
                contentDescription = "Voice: click More"
            }
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
                tint = AvanueTheme.colors.textPrimary
            )
        }
    }
}
```

### 5.4 Full Actions Bottom Sheet

```kotlin
@Composable
fun FullActionsBottomSheet(
    contentTypeId: String,
    onActionSelected: (QuickAction) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val actionGroups = remember(contentTypeId, searchQuery) {
        if (searchQuery.isBlank()) {
            ContextualActionProvider.allActionsForContent(contentTypeId)
        } else {
            val filtered = ContextualActionProvider.searchActions(contentTypeId, searchQuery)
            listOf(ActionGroup(category = "Search Results", actions = filtered))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AvanueTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Search field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search actions...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Voice: search actions" },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action groups
            LazyColumn {
                actionGroups.forEach { group ->
                    item {
                        Text(
                            group.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = AvanueTheme.colors.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(group.actions.size) { idx ->
                        val action = group.actions[idx]
                        ListItem(
                            headlineContent = { Text(action.label) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(action.iconName),
                                    contentDescription = null,
                                    tint = AvanueTheme.colors.textPrimary
                                )
                            },
                            modifier = Modifier
                                .clickable { onActionSelected(action) }
                                .semantics {
                                    contentDescription = "Voice: click ${action.label}"
                                }
                        )
                    }
                }
            }
        }
    }
}
```

---

## 6. Adding a New Shell

To create a custom shell variant (in addition to CLASSIC, AVANUE_VIEWS, LENS, CANVAS):

### 6.1 Step 1: Add Enum Value

```kotlin
// In Modules/Cockpit/model/SimplifiedShellMode.kt
@Serializable
enum class SimplifiedShellMode(
    val displayLabel: String,
    val description: String,
) {
    // ... existing values ...

    /**
     * MyCustomShell — Describe your shell here.
     */
    MY_CUSTOM(
        displayLabel = "MyCustom",
        description = "Your shell description"
    );

    companion object {
        val DEFAULT = LENS
        // fromString() already handles new values via reflection
    }
}
```

### 6.2 Step 2: Create the Shell Composable

```kotlin
// File: Modules/Cockpit/src/androidMain/kotlin/com/augmentalis/cockpit/ui/shells/MyCustomShell.kt

@Composable
fun MyCustomShell(
    state: CockpitScreenState,
    onModuleClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onLayoutChange: (ArrangementIntent) -> Unit,
    onAddFrame: (FrameContent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Your custom shell UI here
    Column(modifier = modifier.fillMaxSize()) {
        // Top area: module launcher
        HorizontalPager(pageCount = availableModules.size) { page ->
            ModuleLauncherCard(
                module = availableModules[page],
                onClick = { onModuleClick(it.id) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom area: arrangement controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ArrangementIntent.entries.forEach { intent ->
                IconButton(
                    onClick = { onLayoutChange(intent) },
                    modifier = Modifier.semantics {
                        contentDescription = "Voice: ${intent.voiceCommand}"
                    }
                ) {
                    Icon(
                        painter = painterResource(intent.iconName),
                        contentDescription = intent.displayLabel
                    )
                }
            }
        }
    }
}
```

### 6.3 Step 3: Integrate in Routing

```kotlin
// In CockpitScreenContent.kt or similar routing file
when (shellMode) {
    SimplifiedShellMode.CLASSIC -> ClassicDashboard(state, callbacks...)
    SimplifiedShellMode.AVANUE_VIEWS -> AvanueViewsShell(state, callbacks...)
    SimplifiedShellMode.LENS -> LensShell(state, callbacks...)
    SimplifiedShellMode.CANVAS -> CanvasShell(state, callbacks...)
    SimplifiedShellMode.MY_CUSTOM -> MyCustomShell(state, callbacks...)
}
```

### 6.4 Step 4: Update Settings UI

```kotlin
// In apps/avanues/src/main/kotlin/ui/settings/CockpitSettingsProvider.kt

companion object {
    val SHELL_MODE_OPTIONS = listOf(
        "CLASSIC" to "Classic",
        "AVANUE_VIEWS" to "AvanueViews",
        "LENS" to "Lens",
        "CANVAS" to "Canvas",
        "MY_CUSTOM" to "MyCustom"  // Add here
    )
}
```

---

## 7. Adding Content Types

To add a new content type (beyond the 18 built-in types):

### 7.1 Step 1: Define FrameContent Variant

```kotlin
// In Modules/Cockpit/model/FrameContent.kt
@Serializable
sealed class FrameContent {

    // ... existing variants ...

    @Serializable
    @SerialName("my_content")
    data class MyContent(
        val customField1: String = "",
        val customField2: Int = 0,
        val customStateJson: String = "{}"
    ) : FrameContent() {
        override val typeId: String = TYPE_MY_CONTENT
    }

    companion object {
        const val TYPE_MY_CONTENT = "my_content"
        // Add to ALL_TYPES for validation
        val ALL_TYPES = listOf(
            // ... existing ...
            TYPE_MY_CONTENT
        )
    }
}
```

### 7.2 Step 2: Create ContentRenderer

```kotlin
// File: Modules/Cockpit/src/androidMain/.../renderers/MyContentRenderer.kt

@Composable
fun MyContentRenderer(
    content: FrameContent.MyContent,
    modifier: Modifier = Modifier,
    onStateChange: (FrameContent.MyContent) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Render your content here
        Text(content.customField1)

        // Update state when user interacts
        Button(onClick = {
            onStateChange(content.copy(customField2 = content.customField2 + 1))
        }) {
            Text("Increment: ${content.customField2}")
        }
    }
}
```

### 7.3 Step 3: Register Renderer

```kotlin
// In ContentRenderer dispatch (usually in CockpitScreenContent or a factory)
@Composable
fun RenderFrameContent(
    frame: CockpitFrame,
    onContentUpdate: (FrameContent) -> Unit
) {
    when (frame.content) {
        is FrameContent.Web -> WebRenderer(frame.content)
        is FrameContent.MyContent -> MyContentRenderer(
            frame.content,
            onStateChange = onContentUpdate
        )
        // ... others ...
        else -> PlaceholderRenderer(frame.content.typeId)
    }
}
```

### 7.4 Step 4: Add Actions

```kotlin
// In Modules/Cockpit/model/ContextualActionProvider.kt
object ContextualActionProvider {

    fun topActionsForContent(contentTypeId: String): List<QuickAction> =
        when (contentTypeId) {
            // ... existing ...

            FrameContent.TYPE_MY_CONTENT -> listOf(
                action("mycontent_action1", "Action 1", "icon1"),
                action("mycontent_action2", "Action 2", "icon2"),
                action("mycontent_save", "Save", "save"),
            )

            else -> listOf(/* defaults */)
        }
}
```

### 7.5 Step 5: Wire to Database

Add a column to `CockpitFrame.sq`:

```sql
CREATE TABLE cockpit_frame (
    id TEXT PRIMARY KEY,
    frameTypeId TEXT NOT NULL,
    sessionId TEXT NOT NULL,
    contentData TEXT,  -- JSON serialized FrameContent
    positionX REAL DEFAULT 0,
    positionY REAL DEFAULT 0,
    width REAL DEFAULT 300,
    height REAL DEFAULT 300,
    zOrder INTEGER DEFAULT 0,
    createdAt TEXT DEFAULT CURRENT_TIMESTAMP
);
```

---

## 8. Voice Command Integration

### 8.1 Command Registration

All four arrangement intents should be registered as static commands:

```kotlin
// In your VoiceOSCore handler registration (typically in AndroidHandlerFactory or similar)

val commands = listOf(
    StaticCommand(
        phrase = "focus",
        action = CommandActionType.LAYOUT_FOCUS,
        category = ActionCategory.LAYOUT,
        description = "Show single frame fullscreen",
        source = "cockpit"
    ),
    StaticCommand(
        phrase = "compare",
        action = CommandActionType.LAYOUT_COMPARE,
        category = ActionCategory.LAYOUT,
        description = "Show two frames side by side",
        source = "cockpit"
    ),
    StaticCommand(
        phrase = "overview",
        action = CommandActionType.LAYOUT_OVERVIEW,
        category = ActionCategory.LAYOUT,
        description = "Show all frames in a grid",
        source = "cockpit"
    ),
    StaticCommand(
        phrase = "present",
        action = CommandActionType.LAYOUT_PRESENT,
        category = ActionCategory.LAYOUT,
        description = "Showcase mode for presentations",
        source = "cockpit"
    ),
)
```

### 8.2 Handler Implementation

```kotlin
class LayoutIntentHandler @Inject constructor(
    private val cockpitScreenViewModel: CockpitScreenViewModel,
) : IHandler {

    override fun canHandle(category: ActionCategory): Boolean =
        category == ActionCategory.LAYOUT

    override suspend fun handle(command: StaticCommand): HandlerResult = withContext(Dispatchers.Main) {
        val intent = ArrangementIntent.fromVoiceCommand(command.phrase) ?:
            return@withContext HandlerResult.Unhandled

        val layout = IntentResolver.resolve(
            intent = intent,
            frameCount = cockpitScreenViewModel.frameCount.value,
            displayProfile = cockpitScreenViewModel.displayProfile.value,
            spatialAvailable = cockpitScreenViewModel.isSpatialAvailable.value
        )

        cockpitScreenViewModel.updateLayoutMode(layout)
        return@withContext HandlerResult.Success(
            feedbackMessage = "Changed to ${intent.displayLabel}",
            feedbackType = FeedbackType.VISUAL_HAPTIC
        )
    }
}
```

### 8.3 Localization

Create locale-specific phrase lists in your .VOS files:

```
# Resource: /volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/res/vos/app/focus.vos

command_id|layout_focus|en_US|en_US|focus|LAYOUT|LAYOUT_FOCUS
command_id|layout_focus|es_ES|es_ES|enfoque|LAYOUT|LAYOUT_FOCUS
command_id|layout_focus|fr_FR|fr_FR|focus|LAYOUT|LAYOUT_FOCUS
command_id|layout_focus|de_DE|de_DE|fokus|LAYOUT|LAYOUT_FOCUS
command_id|layout_focus|hi_IN|hi_IN|ध्यान केंद्रित करें|LAYOUT|LAYOUT_FOCUS
```

---

## 9. Settings Integration

### 9.1 Creating a SettingsProvider

```kotlin
class CockpitSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    override val moduleId = "cockpit"
    override val displayName = "Cockpit"
    override val sortOrder = 350

    override val sections = listOf(
        SettingsSection(id = "shell", title = "Home Screen", sortOrder = 0),
        SettingsSection(id = "frames", title = "Frames", sortOrder = 1),
        SettingsSection(id = "spatial", title = "Spatial", sortOrder = 2)
    )

    @Composable
    override fun Content() {
        val settings by repository.observeSettings().collectAsState(AvanuesSettings())
        val scope = rememberCoroutineScope()

        // Shell Mode Selection
        SettingsDropdownRow(
            label = "Shell Mode",
            currentValue = settings.shellMode,
            options = listOf(
                "CLASSIC" to "Classic",
                "AVANUE_VIEWS" to "AvanueViews",
                "LENS" to "Lens",
                "CANVAS" to "Canvas"
            ),
            onValueChange = { newShell ->
                scope.launch {
                    repository.updateSetting(SettingsKeys.SHELL_MODE, newShell)
                }
            },
            modifier = Modifier.semantics {
                contentDescription = "Voice: open Shell Mode"
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Default Arrangement Intent
        SettingsDropdownRow(
            label = "Default Arrangement",
            currentValue = settings.defaultArrangement,
            options = listOf(
                "FOCUS" to "Focus",
                "COMPARE" to "Compare",
                "OVERVIEW" to "Overview",
                "PRESENT" to "Present"
            ),
            onValueChange = { newIntent ->
                scope.launch {
                    repository.updateSetting(SettingsKeys.DEFAULT_ARRANGEMENT, newIntent)
                }
            },
            modifier = Modifier.semantics {
                contentDescription = "Voice: open Default Arrangement"
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Cockpit Spatial Tracking
        SettingsSwitchRow(
            label = "Spatial Head Tracking",
            isChecked = settings.cockpitSpatialEnabled,
            onCheckedChange = { enabled ->
                scope.launch {
                    repository.updateSetting(SettingsKeys.COCKPIT_SPATIAL_ENABLED, enabled.toString())
                }
            },
            modifier = Modifier.semantics {
                contentDescription = "Voice: toggle Spatial Head Tracking"
            }
        )
    }
}
```

### 9.2 Hilt Registration

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @IntoSet
    fun provideCockpitSettings(provider: CockpitSettingsProvider): ComposableSettingsProvider =
        provider

    // ... other providers ...
}
```

### 9.3 SettingsKeys Reference

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `SHELL_MODE` | String (enum) | LENS | Active shell (CLASSIC/AVANUE_VIEWS/LENS/CANVAS) |
| `DEFAULT_ARRANGEMENT` | String (enum) | OVERVIEW | Default layout intent when opening session |
| `COCKPIT_MAX_FRAMES` | Int | 8 | Maximum concurrent frames in a session |
| `COCKPIT_AUTOSAVE_INTERVAL` | String | "5m" | Session autosave frequency (Off/30s/1m/5m) |
| `COCKPIT_BACKGROUND_SCENE` | String | GRADIENT | Home screen background (GRADIENT/STARFIELD/MINIMAL/NONE) |
| `COCKPIT_SPATIAL_ENABLED` | Boolean | true | Head-tracking canvas overlay (Android only) |
| `COCKPIT_SPATIAL_SENSITIVITY` | String | NORMAL | Tracking sensitivity (LOW/NORMAL/HIGH) |
| `COCKPIT_CANVAS_ZOOM_PERSIST` | Boolean | true | Remember Canvas zoom level across sessions |

---

## 10. Glass Optimization Checklist

When building apps for AR glasses, smart glasses, or wearables:

### Density & Display Profile Adaptation

```kotlin
val displayProfile = when {
    isSmartGlasses -> DisplayProfile.GLASS_MICRO  // 640x480, low pixel density
    isARGlasses -> DisplayProfile.GLASS_COMPACT    // 1280x720
    isHighDpiGlasses -> DisplayProfile.GLASS_HD    // 1920x1080
    else -> DisplayProfile.PHONE
}

val density = displayProfile.densityScaleFactor  // 0.5x to 1.5x
```

### Component Sizing for Glass

| Display Type | Button Height | Text Size | Action Bar Height |
|--------------|---------------|-----------|-------------------|
| GLASS_MICRO | 32 dp | 12 sp | 40 dp |
| GLASS_COMPACT | 40 dp | 14 sp | 48 dp |
| GLASS_HD | 48 dp | 16 sp | 56 dp |
| PHONE | 48 dp | 16 sp | 56 dp |

```kotlin
@Composable
fun GlassOptimizedButton(
    label: String,
    onClick: () -> Unit,
    displayProfile: DisplayProfile
) {
    val height = when (displayProfile) {
        DisplayProfile.GLASS_MICRO -> 32.dp
        DisplayProfile.GLASS_COMPACT -> 40.dp
        else -> 48.dp
    }

    Button(
        onClick = onClick,
        modifier = Modifier.height(height)
    ) {
        Text(label)
    }
}
```

### Pagination for Glass (Vertical Scroll Optimization)

```kotlin
@Composable
fun GlassFriendlyActionList(
    actions: List<QuickAction>,
    displayProfile: DisplayProfile,
    onActionSelected: (QuickAction) -> Unit
) {
    if (displayProfile.isGlass) {
        // Show 3-4 actions at a time with scroll
        LazyColumn(modifier = Modifier.height(160.dp)) {
            items(actions) { action ->
                CompactActionRow(action, onActionSelected)
            }
        }
    } else {
        // On phone/tablet, show all in horizontal scroll or grid
        LazyRow {
            items(actions) { action ->
                ActionButton(action, onActionSelected)
            }
        }
    }
}
```

### Color Scheme for Glass (colorsXR)

Glass displays often use additive color (light on dark) or monochrome:

```kotlin
@Composable
fun GlassDisplay(displayMode: GlassDisplayMode) {
    val colors = when (displayMode) {
        GlassDisplayMode.SEE_THROUGH -> AvanueTheme.colors.colorsXR  // Additive, boosted luminance
        GlassDisplayMode.OPAQUE_GLASS -> AvanueTheme.colors.copy(isDark = true)
        else -> AvanueTheme.colors
    }

    Box(modifier = Modifier.background(colors.surface)) {
        // Your glass-optimized content
    }
}
```

### Text Halo for See-Through Glass

```kotlin
@Composable
fun GlassText(
    text: String,
    displayMode: GlassDisplayMode
) {
    if (displayMode == GlassDisplayMode.SEE_THROUGH) {
        // Add stroke/halo for visibility on transparent glass
        Canvas(modifier = Modifier.fillMaxWidth()) {
            drawContext.canvas.nativeCanvas.also { canvas ->
                // Draw text with black stroke + white fill
                val paint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                // ... render with stroke ...
            }
        }
    } else {
        Text(text)
    }
}
```

### Minimal Chrome for Glass

- Hide unnecessary UI elements on glass (status bar, padding, shadows)
- Use edge-to-edge rendering
- Prioritize essential information (no decorative images)

```kotlin
@Composable
fun MinimalGlassUI(
    displayProfile: DisplayProfile,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .let {
                if (displayProfile.isGlass) {
                    // No padding on glass, use full screen
                    it.padding(0.dp)
                } else {
                    it.padding(16.dp)
                }
            }
    ) {
        content()
    }
}
```

---

## 11. Template Code

### 11.1 Minimal Voice-First App

```kotlin
// Main activity
@AndroidEntryPoint
class CockpitDemoActivity : ComponentActivity() {

    private val viewModel: CockpitDemoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvanueThemeProvider(
                colors = AvanueColorPalette.HYDRA.colors(isDark = false),
                materialMode = MaterialMode.Water,
                isDark = false
            ) {
                val state by viewModel.screenState.collectAsState()

                CockpitScreenContent(
                    state = state,
                    onNavigateBack = { finish() },
                    onReturnToDashboard = { viewModel.loadDashboard() },
                    onFrameSelected = { viewModel.selectFrame(it) },
                    onFrameMoved = { id, x, y -> viewModel.moveFrame(id, x, y) },
                    onFrameResized = { id, w, h -> viewModel.resizeFrame(id, w, h) },
                    onFrameClose = { viewModel.closeFrame(it) },
                    onFrameMinimize = { viewModel.minimizeFrame(it) },
                    onFrameMaximize = { viewModel.maximizeFrame(it) },
                    onLayoutModeChanged = { viewModel.changeLayoutMode(it) },
                    onAddFrame = { content, frameId -> viewModel.addFrame(content, frameId) },
                    frameContent = { frame -> RenderFrameContent(frame) { viewModel.updateFrameContent(frame.id, it) } }
                )
            }
        }
    }
}

// ViewModel
@HiltViewModel
class CockpitDemoViewModel @Inject constructor(
    private val cockpitRepository: CockpitRepository,
    private val repository: AvanuesSettingsRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow(CockpitScreenState())
    val screenState: StateFlow<CockpitScreenState> = _screenState.asStateFlow()

    fun changeLayoutMode(mode: LayoutMode) {
        _screenState.update { it.copy(layoutMode = mode) }
        viewModelScope.launch {
            cockpitRepository.updateSession(_screenState.value.asSessionEntity())
        }
    }

    fun selectFrame(frameId: String) {
        _screenState.update { it.copy(selectedFrameId = frameId) }
    }

    fun closeFrame(frameId: String) {
        _screenState.update { state ->
            state.copy(frames = state.frames.filter { it.id != frameId })
        }
    }

    fun addFrame(content: FrameContent, frameId: String = UUID.randomUUID().toString()) {
        val newFrame = CockpitFrame(
            id = frameId,
            sessionId = _screenState.value.sessionId,
            content = content,
            contentType = content.typeId
        )
        _screenState.update { state ->
            state.copy(frames = state.frames + newFrame)
        }
    }
}

// Content Renderer
@Composable
fun RenderFrameContent(
    frame: CockpitFrame,
    onContentUpdate: (FrameContent) -> Unit
) {
    when (frame.content) {
        is FrameContent.Web -> WebRenderer(frame.content) { onContentUpdate(it) }
        is FrameContent.Note -> NoteRenderer(frame.content) { onContentUpdate(it) }
        is FrameContent.Camera -> CameraRenderer(frame.content) { onContentUpdate(it) }
        is FrameContent.Video -> VideoRenderer(frame.content) { onContentUpdate(it) }
        is FrameContent.Whiteboard -> WhiteboardRenderer(frame.content) { onContentUpdate(it) }
        else -> PlaceholderRenderer(frame.content.typeId)
    }
}
```

### 11.2 Shell Selection Screen

```kotlin
@Composable
fun ShellSelectionScreen(
    onShellSelected: (SimplifiedShellMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Choose Your Shell",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        SimplifiedShellMode.entries.forEach { shell ->
            ShellOptionCard(
                shell = shell,
                onClick = { onShellSelected(shell) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ShellOptionCard(
    shell: SimplifiedShellMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Voice: select ${shell.displayLabel}" },
        color = AvanueTheme.colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                shell.displayLabel,
                style = MaterialTheme.typography.titleMedium,
                color = AvanueTheme.colors.textPrimary
            )
            Text(
                shell.description,
                style = MaterialTheme.typography.bodySmall,
                color = AvanueTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

---

## 12. Platform Matrix

**Support Status as of 260225:**

| Feature | Android | iOS | macOS | Web | Smart Glasses |
|---------|---------|-----|-------|-----|---------------|
| **Shells** |  |  |  |  |  |
| CLASSIC | ✅ | ✅ | ✅ | ✅ | ⚠️ (limited) |
| AVANUE_VIEWS | ✅ | ✅ | ✅ | ✅ | ✅ |
| LENS | ✅ | ✅ | ✅ | ✅ | ⚠️ (reduced) |
| CANVAS | ✅ | ⚠️ | ⚠️ | ✅ | ❌ |
| **Intents** |  |  |  |  |  |
| FOCUS | ✅ | ✅ | ✅ | ✅ | ✅ |
| COMPARE | ✅ | ✅ | ✅ | ✅ | ✅ |
| OVERVIEW | ✅ | ✅ | ✅ | ✅ | ✅ |
| PRESENT | ✅ | ✅ | ✅ | ✅ | ⚠️ (small UI) |
| **Content Types** |  |  |  |  |  |
| Web | ✅ | ✅ | ✅ | ✅ | ✅ |
| PDF | ✅ | ✅ | ✅ | ✅ | ⚠️ (small) |
| Note | ✅ | ✅ | ✅ | ✅ | ✅ |
| Camera | ✅ | ✅ | ✅ | ❌ | ✅ |
| Video | ✅ | ✅ | ✅ | ✅ | ✅ |
| Whiteboard | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| Terminal | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| AI Summary | ✅ | ⚠️ | ⚠️ | ✅ | ✅ |

**Legend:**
- ✅ = Fully supported
- ⚠️ = Partially supported or with limitations
- ❌ = Not supported

---

## 13. Deep Link Support

The Voice-First system supports `cockpit://` deep links for launching sessions and content:

### 13.1 Intent Filters

```xml
<activity android:name=".CockpitActivity">
    <intent-filter android:label="Cockpit">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="cockpit"
            android:host="session"
            android:pathPrefix="/open" />
        <data
            android:scheme="cockpit"
            android:host="frame"
            android:pathPrefix="/add" />
    </intent-filter>
</activity>
```

### 13.2 Link Formats

| Format | Example | Effect |
|--------|---------|--------|
| Open Session | `cockpit://session/open?id=abc123` | Load session with ID |
| Add Frame | `cockpit://frame/add?type=web&url=https://example.com` | Add frame with content |
| Set Arrangement | `cockpit://layout?intent=OVERVIEW` | Change arrangement intent |
| Switch Shell | `cockpit://shell?mode=LENS` | Change shell mode |

### 13.3 Handler

```kotlin
@AndroidEntryPoint
class CockpitDeepLinkActivity : ComponentActivity() {

    private val viewModel: CockpitDemoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle incoming deep link
        intent.data?.let { uri ->
            when (uri.host) {
                "session" -> {
                    val sessionId = uri.getQueryParameter("id")
                    sessionId?.let { viewModel.loadSession(it) }
                }
                "frame" -> {
                    val type = uri.getQueryParameter("type")
                    when (type) {
                        "web" -> {
                            val url = uri.getQueryParameter("url") ?: "https://www.google.com"
                            viewModel.addFrame(FrameContent.Web(url = url))
                        }
                        // ... handle other types ...
                    }
                }
                "layout" -> {
                    val intent = uri.getQueryParameter("intent")
                    intent?.let { intentStr ->
                        ArrangementIntent.fromString(intentStr)?.let { arrIntent ->
                            val layout = IntentResolver.resolve(arrIntent, viewModel.frameCount)
                            viewModel.changeLayoutMode(layout)
                        }
                    }
                }
            }
        }

        setContent {
            // ... render cockpit ...
        }
    }
}
```

---

## 14. Session Templates

Pre-built session layouts for common workflows:

### 14.1 Template Types

| Template | Shells | Frames | Intent | Use Case |
|----------|--------|--------|--------|----------|
| **Lecture** | AVANUE_VIEWS, CLASSIC | PDF + Note + Camera | COMPARE | Note-taking from slides |
| **Meeting** | LENS, CLASSIC | Web (agenda) + Notes + Camera | OVERVIEW | Multi-window meeting notes |
| **Research** | CANVAS | Web + PDF + Notes + Terminal | FREEFORM | Deep work with references |
| **Presentation** | CLASSIC | 2-3 slides + speaker notes | TRIPTYCH | Live presenting |
| **Engineering** | LENS | Terminal + Code + Docs | SPLIT | Coding with reference |
| **Design** | CANVAS | Image + Whiteboard + Notes | SPATIAL_DICE | Creative work |

### 14.2 Creating a Custom Template

```kotlin
// In a SettingsProvider or TemplateRepository
fun createCustomTemplate(
    name: String,
    frameContents: List<FrameContent>,
    shellMode: SimplifiedShellMode,
    defaultIntent: ArrangementIntent
): CockpitTemplate {
    return CockpitTemplate(
        id = UUID.randomUUID().toString(),
        name = name,
        description = "Custom template for $name",
        frameContents = frameContents,
        shellMode = shellMode,
        defaultArrangement = defaultIntent,
        createdAt = System.currentTimeMillis()
    )
}

// Using a template
fun launchTemplate(template: CockpitTemplate) {
    val session = CockpitSession(
        id = UUID.randomUUID().toString(),
        frames = template.frameContents.mapIndexed { idx, content ->
            CockpitFrame(
                id = "frame_$idx",
                content = content,
                contentType = content.typeId
            )
        }
    )

    repository.createSession(session)
    viewModel.loadSession(session.id)
}
```

---

## 15. Testing

### 15.1 ViewModel Testing

```kotlin
class CockpitScreenViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CockpitScreenViewModel
    private lateinit var repository: FakeCockpitRepository

    @Before
    fun setup() {
        repository = FakeCockpitRepository()
        viewModel = CockpitScreenViewModel(repository)
    }

    @Test
    fun testChangeLayoutMode() = runTest {
        viewModel.changeLayoutMode(LayoutMode.FULLSCREEN)

        val state = viewModel.screenState.first()
        assertEquals(LayoutMode.FULLSCREEN, state.layoutMode)
    }

    @Test
    fun testSelectFrame() = runTest {
        val frameId = "frame_123"
        viewModel.selectFrame(frameId)

        val state = viewModel.screenState.first()
        assertEquals(frameId, state.selectedFrameId)
    }

    @Test
    fun testAddFrame() = runTest {
        val content = FrameContent.Web(url = "https://example.com")
        viewModel.addFrame(content)

        val state = viewModel.screenState.first()
        assert(state.frames.any { it.content is FrameContent.Web })
    }
}
```

### 15.2 IntentResolver Testing

```kotlin
class IntentResolverTest {

    @Test
    fun testResolveForFocus() {
        val layout = IntentResolver.resolve(
            ArrangementIntent.FOCUS,
            frameCount = 5,
            displayProfile = DisplayProfile.PHONE
        )
        assertEquals(LayoutMode.FULLSCREEN, layout)
    }

    @Test
    fun testResolveForCompareOnPhone() {
        val layout = IntentResolver.resolve(
            ArrangementIntent.COMPARE,
            frameCount = 2,
            displayProfile = DisplayProfile.PHONE
        )
        assertEquals(LayoutMode.T_PANEL, layout)
    }

    @Test
    fun testResolveForCompareOnTablet() {
        val layout = IntentResolver.resolve(
            ArrangementIntent.COMPARE,
            frameCount = 2,
            displayProfile = DisplayProfile.TABLET
        )
        assertEquals(LayoutMode.SPLIT_LEFT, layout)
    }

    @Test
    fun testResolveForOverviewWithSpatial() {
        val layout = IntentResolver.resolve(
            ArrangementIntent.OVERVIEW,
            frameCount = 5,
            displayProfile = DisplayProfile.TABLET,
            spatialAvailable = true
        )
        assertEquals(LayoutMode.SPATIAL_DICE, layout)
    }

    @Test
    fun testInferIntent() {
        assertEquals(ArrangementIntent.FOCUS, IntentResolver.inferIntent(LayoutMode.FULLSCREEN))
        assertEquals(ArrangementIntent.COMPARE, IntentResolver.inferIntent(LayoutMode.SPLIT_LEFT))
        assertEquals(ArrangementIntent.OVERVIEW, IntentResolver.inferIntent(LayoutMode.GRID))
        assertEquals(ArrangementIntent.PRESENT, IntentResolver.inferIntent(LayoutMode.CAROUSEL))
    }
}
```

### 15.3 ContextualActionProvider Testing

```kotlin
class ContextualActionProviderTest {

    @Test
    fun testTopActionsForWeb() {
        val actions = ContextualActionProvider.topActionsForContent(FrameContent.TYPE_WEB)

        assertEquals(5, actions.size)
        assert(actions.any { it.label == "Back" })
        assert(actions.any { it.label == "Refresh" })
    }

    @Test
    fun testSearchActions() {
        val actions = ContextualActionProvider.searchActions(FrameContent.TYPE_NOTE, "save")

        assert(actions.any { it.label == "Save" })
    }

    @Test
    fun testActionGroupCategories() {
        val groups = ContextualActionProvider.allActionsForContent(FrameContent.TYPE_WEB)

        val categories = groups.map { it.category }
        assert("Web" in categories)
        assert("Frame" in categories)
        assert("Layout" in categories)
        assert("Tools" in categories)
    }
}
```

---

## 16. Troubleshooting

### Intent Resolver Returning Wrong Layout

**Problem:** User says "compare" on a phone, but app shows grid instead of side-by-side.

**Root Cause:** Frame count < 2, or displayProfile not set correctly.

**Solution:**
```kotlin
// Debug log
Log.d("IntentResolver", """
    intent=${ArrangementIntent.COMPARE}
    frameCount=${frameCount}
    displayProfile=${displayProfile}
    spatialAvailable=${isSpatialAvailable}
    resolved=${IntentResolver.resolve(...)}
""".trimIndent())

// Verify displayProfile is correct
val profile = when {
    Resources.getSystem().displayMetrics.widthPixels > 600 -> DisplayProfile.TABLET
    else -> DisplayProfile.PHONE
}
```

### Shell Mode Not Persisting

**Problem:** User switches shell in settings, but it reverts after app restart.

**Root Cause:** SettingsRepository not reading/writing SHELL_MODE key.

**Solution:**
```kotlin
// Check DataStore value directly
val flow = context.dataStore.data
    .map { it[stringPreferencesKey(SettingsKeys.SHELL_MODE)] }
    .collect { value ->
        Log.d("Settings", "SHELL_MODE = $value")
    }

// Verify repository is persisting
viewModelScope.launch {
    repository.updateSetting(SettingsKeys.SHELL_MODE, SimplifiedShellMode.LENS.name)
    delay(100)  // Let IO finish
    val current = repository.getSetting(SettingsKeys.SHELL_MODE)
    Log.d("Settings", "After update: $current")
}
```

### Voice Commands Not Working

**Problem:** Voice commands like "focus", "compare" don't trigger layout changes.

**Root Cause:** Handler not registered or action category mismatch.

**Solution:**
```kotlin
// In AndroidHandlerFactory
@Provides
@IntoSet
fun provideLayoutIntentHandler(): IHandler = LayoutIntentHandler(...)

// Verify handler priority in dispatch
val result = handler.handle(StaticCommand(
    phrase = "focus",
    category = ActionCategory.LAYOUT  // Must match canHandle()
))

// Check phrase matching
val command = StaticCommand.fromPhrase("focus")
val intent = ArrangementIntent.fromVoiceCommand(command.phrase)
Log.d("Handler", "Intent=$intent")
```

### Glass Display Showing Oversized UI

**Problem:** Buttons/text unreadable on smart glasses screen.

**Root Cause:** Not adapting density/size for glass display profile.

**Solution:**
```kotlin
// Always check display profile
val size = when (displayProfile) {
    DisplayProfile.GLASS_MICRO -> 32.dp
    DisplayProfile.GLASS_COMPACT -> 40.dp
    DisplayProfile.GLASS_HD -> 48.dp
    else -> 56.dp
}

Button(
    modifier = Modifier.height(size)
) { Text("Action") }

// Use colorsXR for see-through glass
val colors = if (displayMode == GlassDisplayMode.SEE_THROUGH) {
    palette.colorsXR
} else {
    palette.colors(isDark = true)
}
```

---

## 17. Migration from CommandBar

The original hierarchical **CommandBar** (13 states, nested menus) has been superseded by the **Voice-First UI Shell** system, but remains available as CLASSIC mode for backward compatibility.

### 17.1 What Changed

| Aspect | CommandBar (Old) | Voice-First (New) |
|--------|-----------------|-------------------|
| **Home Screen** | Fixed dashboard with 12 tiles | 4 shells: Classic, AvanueViews, Lens, Canvas |
| **Layout Selection** | Choose from 15 mode buttons | Express intent (Focus, Compare, Overview, Present); system resolves |
| **Actions** | Hierarchical menu (MAIN → FRAME_ACTIONS → WEB_ACTIONS) | Flat (top 5-6 shown, rest in searchable "More") |
| **Navigation** | Explicit state tracking (CommandBarState enum) | Automatic state inference (ContextualActionProvider) |
| **Cognitive Load** | 13 menu states, multi-level navigation | 1 level, 4 intents, context-aware actions |

### 17.2 Migration Path

**Phase 1: Add New System (Non-Breaking)**
1. Deploy Voice-First shells alongside CommandBar
2. Default to CLASSIC shell (preserves old behavior)
3. Add SHELL_MODE setting in DataStore
4. Roll out to beta users

**Phase 2: Adopt New Patterns (Gradual)**
1. Update voice command handlers to use ArrangementIntent
2. Register IntentResolver for layout changes
3. Replace action handler registration with ContextualActionProvider
4. Add new shells to home screen options

**Phase 3: Deprecate Old System (Future)**
1. Mark CommandBarState as @Deprecated
2. Encourage users to switch to LENS or AVANUE_VIEWS shell
3. Remove CLASSIC shell after 3 major versions

### 17.3 Code Migration Examples

**Old (CommandBar):**
```kotlin
// Explicitly choose layout from 15 options
when (selectedLayoutButton) {
    "grid" -> updateLayoutMode(LayoutMode.GRID)
    "mosaic" -> updateLayoutMode(LayoutMode.MOSAIC)
    "carousel" -> updateLayoutMode(LayoutMode.CAROUSEL)
    // ... 12 more branches
}

// Navigate command bar menu hierarchy
when (commandBarState) {
    CommandBarState.MAIN -> showMainActions()
    CommandBarState.FRAME_ACTIONS -> showFrameActions()
    CommandBarState.WEB_ACTIONS -> showWebActions()
}
```

**New (Voice-First):**
```kotlin
// Express intent; system resolves to layout
val layout = IntentResolver.resolve(
    intent = ArrangementIntent.OVERVIEW,
    frameCount = frames.size,
    displayProfile = displayProfile
)
updateLayoutMode(layout)

// Get actions automatically based on content
val actions = ContextualActionProvider.topActionsForContent(contentType)
// commandBarState goes away; UI is automatically contextual
```

---

## 18. Cognitive Load Comparison

### Metrics: CommandBar vs. Voice-First

#### Decision Points

| Workflow | CommandBar | Voice-First | Savings |
|----------|-----------|-------------|---------|
| "Show me both documents side by side" | User clicks "layout" → chooses from 15 modes → finds "SPLIT_LEFT" | User says "compare" OR clicks "Compare" button | 13 fewer options |
| "What actions can I do with this note?" | Navigate MAIN → FRAME_ACTIONS → NOTE_ACTIONS (3 levels) | See 5 actions inline + tap "More" (1-2 levels) | 1 level eliminated |
| "Switch to AvanueViews" | Go to Settings → find Shell → scroll options | Go to Settings → Shell Mode dropdown → select (fewer options) | Simpler UI |
| "Make this fullscreen" | Click layout → choose FULLSCREEN | Say "focus" OR click FOCUS button (1 tap) | Single step |

#### Menu Depth

```
CommandBar (Old):
CommandBar.MAIN
├── FRAME_ACTIONS
│   ├── WEB_ACTIONS (5-8 actions)
│   ├── PDF_ACTIONS (5-8 actions)
│   ├── NOTE_ACTIONS (5-8 actions)
│   └── ... (10 content types)
├── LAYOUT_ACTIONS (15 layout modes to choose from)
└── SETTINGS (nested preference screens)

Voice-First (New):
Session
├── Inline Actions (5-6 top actions)
├── More Button
│   └── Grouped Actions (4 categories, ~20 total)
└── Arrangement Intents (4 buttons: Focus, Compare, Overview, Present)
```

#### Voice Command Accessibility

**CommandBar:**
- Layout modes have 15 different names ("grid", "mosaic", "carousel", "t_panel", "freeform", etc.)
- Actions require saying content-type prefix ("show web actions", "show pdf actions")

**Voice-First:**
- 4 simple layout intents: "focus", "compare", "overview", "present"
- Actions are discoverable and context-aware (no need to prefix)

#### User Onboarding Time

| Scenario | CommandBar | Voice-First |
|----------|-----------|-------------|
| First-time user learning layouts | 10+ min (15 modes to understand) | 2-3 min (4 intents to understand) |
| Remembering where a feature is | Navigate 2-3 menu levels | Search "More" or know top 5 actions |
| Adding a frame content type | Find content type, add, select layout | Add frame, see top actions immediately |

---

## Appendices

### Appendix A: All 15 Layout Modes

1. **DASHBOARD** — Home/launcher (module tiles, recent sessions)
2. **FULLSCREEN** — Single frame fills display
3. **SPLIT_LEFT** — Primary frame on left (50%), stacked frames on right (50%)
4. **SPLIT_RIGHT** — Primary frame on right (50%), stacked frames on left (50%)
5. **GRID** — Auto-arranged uniform grid (2x2, 2x3, 3x3)
6. **MOSAIC** — Primary frame large (50%), remaining frames tile around it
7. **T_PANEL** — Primary frame 60% top, secondaries in 40% bottom row
8. **COCKPIT** — Flight Deck (fixed 6-slot instrument panel)
9. **FREEFORM** — User drags windows anywhere, resizes freely
10. **ROW** — Horizontal scrollable strip of equal-width frames
11. **CAROUSEL** — 3D curved swipe-through with perspective scaling
12. **SPATIAL_DICE** — 4 corners + 1 center (5-frame dice pattern, head-tracking)
13. **TRIPTYCH** — 3-panel book spread (side wings angled, center elevated)
14. **GALLERY** — Media-only filtered grid (image, video, camera, screen cast)
15. **WORKFLOW** — Vertical numbered step list linked to frames

### Appendix B: All 18 Content Types

1. **Web** — Browser with tabs, ad blocking, desktop mode, zoom
2. **PDF** — Document viewer with page nav, bookmarks, search
3. **Note** — Rich editor with auto-save, attachments, formatting
4. **Camera** — Live feed with capture, zoom, flash
5. **Video** — Player with playback controls, streaming support
6. **Image** — Viewer with pan/zoom, EXIF metadata
7. **VoiceNote** — Audio + live transcription + editable transcript
8. **Whiteboard** — Freeform drawing canvas with stylus support
9. **Terminal** — Scrollable text output (logs, command results)
10. **Map** — Interactive map (OpenStreetMap, platform maps)
11. **Form** — Checklist, structured data fields, inspection forms
12. **Signature** — E-signature pad for document signing
13. **Voice** — Audio recorder/player (no transcription)
14. **Widget** — Mini utilities (clock, timer, compass, weather, battery)
15. **AiSummary** — AI-generated summary of other frames
16. **ScreenCast** — Mirror another device's screen (gRPC, MediaProjection)
17. **File** — File manager with local/cloud/network storage
18. **ExternalApp** — Launcher for 3rd-party apps with embedding support

### Appendix C: SettingsKeys Reference

**Theme Keys:**
- `THEME_PALETTE` (AvanueColorPalette: SOL/LUNA/TERRA/HYDRA)
- `THEME_STYLE` (MaterialMode: Glass/Water/Cupertino/MountainView)
- `THEME_APPEARANCE` (AppearanceMode: Light/Dark/Auto)

**Cockpit Keys:**
- `SHELL_MODE` (SimplifiedShellMode: CLASSIC/AVANUE_VIEWS/LENS/CANVAS)
- `DEFAULT_ARRANGEMENT` (ArrangementIntent: FOCUS/COMPARE/OVERVIEW/PRESENT)
- `COCKPIT_MAX_FRAMES` (Int, default 8)
- `COCKPIT_AUTOSAVE_INTERVAL` (String: Off/30s/1m/5m)
- `COCKPIT_BACKGROUND_SCENE` (String: GRADIENT/STARFIELD/MINIMAL/NONE)
- `COCKPIT_SPATIAL_ENABLED` (Boolean)
- `COCKPIT_SPATIAL_SENSITIVITY` (String: LOW/NORMAL/HIGH)

**Voice/Command Keys:**
- `VOICE_FEEDBACK` (Boolean)
- `VOICE_COMMAND_LOCALE` (String: en-US/es-ES/fr-FR/de-DE/hi-IN)
- `WAKE_WORD_ENABLED` (Boolean)
- `WAKE_WORD_KEYWORD` (String)
- `DISABLED_COMMANDS` (JSON array of disabled command IDs)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-25
**Chapter:** 113
**Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC**

**Related Documentation:**
- Chapter 94: 4-Tier Voice Enablement
- Chapter 95: VOS Distribution & Handler Dispatch
- Chapter 96: KMP Foundation Platform Abstractions
- Chapter 97: Cockpit SpatialVoice Multi-Window (CommandBar, 15 layout modes)
- Chapter 110: Unified Command Architecture
- Chapter 112: Simplified Voice-First UI Shells (overview & design philosophy)

**Contributing:**
Improvements, corrections, and new content are welcome. Please file issues or create PRs at the repository.
