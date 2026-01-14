<!--
filename: 0002-magicui-framework.md
created: 2025-10-19 02:40:19 PDT
author: Manoj Jhawar
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: IDEADEV Implementation Plan for MagicUI Framework
last-modified: 2025-10-19 02:40:19 PDT
version: 1.0.0
-->

# IDEADEV Implementation Plan: MagicUI Framework

**Plan ID:** 0002
**Spec Reference:** `/ideadev/specs/0002-magicui-framework.md`
**Created:** 2025-10-19 02:40:19 PDT
**Status:** Ready for Implementation (YOLO Mode)

---

## Table of Contents

1. [Implementation Overview](#implementation-overview)
2. [Phase Breakdown](#phase-breakdown)
3. [Phase 1: Core DSL Foundation](#phase-1-core-dsl-foundation)
4. [Phase 2: Themes & VOS4 Integration](#phase-2-themes--vos4-integration)
5. [Phase 3: Spatial UI & Database](#phase-3-spatial-ui--database)
6. [Phase 4: Creator Tools & Polish](#phase-4-creator-tools--polish)
7. [Testing Strategy](#testing-strategy)
8. [Risk Mitigation](#risk-mitigation)
9. [Team & Specialists](#team--specialists)

---

## Implementation Overview

### Timeline

**Total Duration:** 16 weeks (4 months)

```
Phase 1: Core DSL Foundation        Weeks  1-4  (4 weeks)
Phase 2: Themes & VOS4 Integration  Weeks  5-8  (4 weeks)
Phase 3: Spatial UI & Database      Weeks  9-12 (4 weeks)
Phase 4: Creator Tools & Polish     Weeks 13-16 (4 weeks)
```

### Success Criteria

**Phase-Specific Gates:**
- Each phase must pass all tests before proceeding
- VOS4 subagent review required (orchestrator enforces)
- Documentation updated before phase completion
- Commit required before next phase

**Overall Success:**
- All 9 architectural decisions implemented
- Performance targets met (<200ms startup, <150ms theme switch)
- 100% test coverage for core DSL
- LearnApp integration working (no scraping)
- Sample apps demonstrate all features

---

## Phase Breakdown

### Phase 1: Core DSL Foundation (Weeks 1-4)

**Goal:** SwiftUI-like DSL compiles to Jetpack Compose

**Deliverables:**
1. Core DSL components (VStack, HStack, ZStack, Text, Button, TextField)
2. State management helpers (state(), computed())
3. Modifier system (.font(), .padding(), .foregroundColor())
4. MagicScreen container
5. 15 basic components working

**Success Criteria:**
- ✅ Login screen builds in <25 lines
- ✅ State updates trigger recomposition
- ✅ All components render correctly
- ✅ Compiles to Compose without errors
- ✅ Unit tests pass (80%+ coverage)

**IDE Loop:**
- **Implement**: @vos4-kotlin-expert (DSL design)
- **Defend**: @vos4-test-specialist (unit tests)
- **Evaluate**: User approval + commit

---

### Phase 2: Themes & VOS4 Integration (Weeks 5-8)

**Goal:** Modular themes + automatic accessibility

**Deliverables:**
1. Theme system architecture (modular plugins)
2. 3 base themes (Material 3, Cupertino, Glass)
3. Theme switching engine (<300ms)
4. VOS4 automatic accessibility integration
5. Magic* templates (5 templates: Login, Settings, Form, List, Dashboard)
6. Pseudo-3D components (shadows, bevels, neumorphic)

**Success Criteria:**
- ✅ Theme switching works (<300ms total)
- ✅ VOS4 voice commands auto-generated
- ✅ 3rd party theme plugin works
- ✅ Magic* templates generate screens
- ✅ Pseudo-3D shadows render correctly
- ✅ Integration tests pass

**IDE Loop:**
- **Implement**: @vos4-kotlin-expert (themes) + @vos4-android-expert (VOS4 integration)
- **Defend**: @vos4-test-specialist (integration tests)
- **Evaluate**: User approval + VOS4 team review + commit

---

### Phase 3: Spatial UI & Database (Weeks 9-12)

**Goal:** Android XR support + Room persistence + LearnApp integration

**Deliverables:**
1. Android XR support (Jetpack XR + AOSP fallback)
2. SpatialWindow, FloatingElement components
3. OpenGL ES integration (basic 3D model loading)
4. Pseudo-spatial UI (parallax, layered depth)
5. Room database schema (LearnApp compliant)
6. DataStore preferences
7. UUID-based component tracking

**Success Criteria:**
- ✅ Spatial UI works on Android XR devices (AOSP + Play)
- ✅ 90fps maintained in XR mode
- ✅ OpenGL renders 3D models
- ✅ Pseudo-spatial parallax works on non-XR devices
- ✅ LearnApp queries database (no scraping)
- ✅ Room + DataStore hybrid works
- ✅ Database tests pass

**IDE Loop:**
- **Implement**: @vos4-android-expert (XR) + @vos4-database-expert (Room)
- **Defend**: @vos4-test-specialist (XR + database tests)
- **Evaluate**: XR device testing + LearnApp integration test + commit

---

### Phase 4: Creator Tools & Polish (Weeks 13-16)

**Goal:** CLI + Web platform + Documentation + Sample apps

**Deliverables:**
1. CLI tool (AI generation, mockup conversion)
2. Web platform foundation (visual designer)
3. Developer documentation (50+ pages)
4. 5 sample apps (showcase all features)
5. Performance optimization
6. Final polish & bug fixes

**Success Criteria:**
- ✅ CLI generates correct MagicUI code
- ✅ Web designer works in browser
- ✅ Documentation complete and accurate
- ✅ Sample apps demonstrate all 9 architectural decisions
- ✅ Performance targets met
- ✅ Zero critical bugs

**IDE Loop:**
- **Implement**: @vos4-orchestrator coordinates all specialists
- **Defend**: @vos4-test-specialist (E2E tests, performance tests)
- **Evaluate**: Full team review + documentation review + commit

---

## Phase 1: Core DSL Foundation (DETAILED)

### Week 1: Project Setup + Basic DSL

#### Day 1-2: Module Structure
```bash
# Create MagicUI module
mkdir -p modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/{core,components,modifiers,state}
mkdir -p modules/libraries/MagicUI/src/test/java/com/augmentalis/magicui
```

**Tasks:**
1. Create `build.gradle.kts` with Compose dependencies
2. Set up KSP for code generation
3. Create module structure
4. Initialize git tracking

**Files to Create:**
- `modules/libraries/MagicUI/build.gradle.kts`
- `modules/libraries/MagicUI/src/main/AndroidManifest.xml`
- `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/MagicUI.kt` (entry point)

**Specialist:** @vos4-kotlin-expert

---

#### Day 3-5: Core DSL Components

**Task 1: MagicScreen Container**
```kotlin
// File: core/MagicScreen.kt
@Composable
fun MagicScreen(
    name: String,
    theme: ThemeMode = ThemeMode.AUTO,
    spatialMode: SpatialMode = SpatialMode.DETECT_AUTO,
    content: @Composable MagicUIScope.() -> Unit
) {
    // Screen container with VOS4 integration
}
```

**Task 2: MagicUIScope (DSL Builder)**
```kotlin
// File: core/MagicUIScope.kt
@MagicUIDsl
class MagicUIScope {
    @Composable
    fun VStack(
        spacing: Dp = 0.dp,
        padding: Dp = 0.dp,
        content: @Composable MagicUIScope.() -> Unit
    )

    @Composable
    fun HStack(
        spacing: Dp = 0.dp,
        padding: Dp = 0.dp,
        content: @Composable MagicUIScope.() -> Unit
    )

    @Composable
    fun Text(text: String): TextComponent

    @Composable
    fun Button(text: String, onClick: () -> Unit): ButtonComponent
}
```

**Task 3: Basic Components**
```kotlin
// File: components/Text.kt
class TextComponent(val text: String) {
    fun font(font: Font): TextComponent
    fun foregroundColor(color: Color): TextComponent
    fun bold(): TextComponent
}

// File: components/Button.kt
class ButtonComponent(val text: String, val onClick: () -> Unit) {
    fun buttonStyle(style: ButtonStyle): ButtonComponent
}
```

**Specialist:** @vos4-kotlin-expert

**Tests:** @vos4-test-specialist
- Unit tests for each component
- DSL builder tests
- Modifier chain tests

---

### Week 2: State Management + More Components

#### Day 6-7: State Helpers

**Task 1: State Helper Functions**
```kotlin
// File: state/StateHelpers.kt
@Composable
fun <T> state(initial: T): MutableState<T> =
    remember { mutableStateOf(initial) }

@Composable
fun <T> computed(calculation: () -> T): State<T> =
    remember { derivedStateOf(calculation) }

@Composable
fun <T> stateOf(initial: T): MutableState<T> = state(initial)
```

**Task 2: State Management Tests**
- Test state updates trigger recomposition
- Test computed state reactivity
- Test state persistence across recomposition

**Specialist:** @vos4-kotlin-expert + @vos4-test-specialist

---

#### Day 8-10: Component Library Expansion

**Components to Implement (15 total):**

1. ✅ Text
2. ✅ Button
3. TextField
4. SecureField
5. Toggle
6. Slider
7. Image
8. Icon
9. Spacer
10. Divider
11. Card
12. LazyColumn
13. LazyRow
14. Progress
15. Loading

**File Structure:**
```
components/
├── Text.kt
├── Button.kt
├── TextField.kt
├── SecureField.kt
├── Toggle.kt
├── Slider.kt
├── Image.kt
├── Icon.kt
├── Spacer.kt
├── Divider.kt
├── Card.kt
├── LazyColumn.kt
├── LazyRow.kt
├── Progress.kt
└── Loading.kt
```

**Each Component:**
- Composable function
- Modifier support
- VOS4 accessibility hooks (placeholder)
- Unit tests

**Specialist:** @vos4-kotlin-expert

---

### Week 3: Modifier System

#### Day 11-13: Modifier Implementation

**Task 1: Modifier Extensions**
```kotlin
// File: modifiers/TextModifiers.kt
fun TextComponent.font(font: Font): TextComponent
fun TextComponent.foregroundColor(color: Color): TextComponent
fun TextComponent.bold(): TextComponent
fun TextComponent.italic(): TextComponent
fun TextComponent.underline(): TextComponent

// File: modifiers/LayoutModifiers.kt
fun Component.padding(padding: Dp): Component
fun Component.padding(horizontal: Dp, vertical: Dp): Component
fun Component.padding(top: Dp, bottom: Dp, start: Dp, end: Dp): Component
fun Component.frame(width: Dp? = null, height: Dp? = null): Component
fun Component.background(color: Color): Component
```

**Task 2: Modifier Chain Support**
```kotlin
// Enable chaining:
Text("Hello")
    .font(.title)
    .foregroundColor(.blue)
    .bold()
    .padding(16.dp)
```

**Specialist:** @vos4-kotlin-expert

**Tests:** @vos4-test-specialist
- Modifier chain tests
- Modifier application order tests
- Modifier immutability tests

---

#### Day 14-15: Compose Integration

**Task 1: DSL → Compose Compilation**
```kotlin
// File: core/ComposeAdapter.kt
internal fun TextComponent.toCompose(): @Composable () -> Unit = {
    Text(
        text = this.text,
        style = this.textStyle.toCompose(),
        color = this.color ?: MaterialTheme.colorScheme.onSurface,
        modifier = this.modifiers.toCompose()
    )
}
```

**Task 2: Integration Tests**
- Test DSL compiles to Compose correctly
- Test rendered output matches expected
- Test performance (compilation overhead)

**Specialist:** @vos4-kotlin-expert + @vos4-test-specialist

---

### Week 4: Testing + Phase 1 Completion

#### Day 16-18: Comprehensive Testing

**Test Coverage Goals:**
- Unit tests: 90%+ coverage
- Integration tests: 80%+ coverage
- Component tests: 100% (all 15 components)

**Test Suites:**
```kotlin
// Test: Login Screen Example
@Test
fun testLoginScreenBuild() {
    composeTestRule.setContent {
        MagicScreen("Login") {
            val email = state("")
            val password = state("")

            VStack(spacing = 16.dp) {
                Text("Welcome").font(.largeTitle).bold()
                TextField("Email", text = email)
                SecureField("Password", text = password)
                Button("Sign In") { login(email.value, password.value) }
            }
        }
    }

    // Verify components render
    composeTestRule.onNodeWithText("Welcome").assertExists()
    composeTestRule.onNodeWithText("Email").assertExists()
}

// Test: State Management
@Test
fun testStateUpdatesRecompose() {
    var count = 0
    composeTestRule.setContent {
        val counter = state(0)
        Button("Increment") { counter.value++ }
        Text("Count: ${counter.value}")
    }

    composeTestRule.onNodeWithText("Increment").performClick()
    composeTestRule.onNodeWithText("Count: 1").assertExists()
}
```

**Specialist:** @vos4-test-specialist

---

#### Day 19-20: Documentation + Phase 1 Commit

**Documentation:**
```markdown
# MagicUI Phase 1 Documentation

## Core DSL
- VStack, HStack, ZStack
- Text, Button, TextField, SecureField
- 15 basic components

## State Management
- state() helper
- computed() helper
- Reactive updates

## Modifier System
- .font(), .padding(), .foregroundColor()
- Modifier chaining
- Type-safe modifiers

## Usage Examples
[30 code examples]
```

**Commit Checklist:**
- [ ] All tests pass (90%+ coverage)
- [ ] Documentation complete
- [ ] Code review approved
- [ ] Performance benchmarks met
- [ ] No critical bugs

**Specialist:** @vos4-documentation-specialist + @vos4-orchestrator (review)

---

## Phase 2: Themes & VOS4 Integration (DETAILED)

### Week 5: Theme System Architecture

#### Day 21-23: Theme Foundation

**Task 1: Theme Plugin System**
```kotlin
// File: theme/ThemePlugin.kt
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemePlugin

interface MagicTheme {
    val name: String
    val basedOn: String?
    val tokens: ThemeTokens
    val components: Map<String, @Composable () -> Unit>
}

// File: theme/ThemeTokens.kt
data class ThemeTokens(
    val colors: ColorTokens,
    val typography: TypographyTokens,
    val spacing: SpacingTokens,
    val shapes: ShapeTokens,
    val effects: EffectTokens
)
```

**Task 2: Theme Engine**
```kotlin
// File: theme/ThemeEngine.kt
object ThemeEngine {
    private val themes = mutableMapOf<String, MagicTheme>()

    fun registerTheme(theme: MagicTheme)
    fun getTheme(name: String): MagicTheme?
    suspend fun switchTheme(name: String)
}
```

**Specialist:** @vos4-kotlin-expert + @vos4-architecture-reviewer

---

#### Day 24-25: Base Themes Implementation

**3 Base Themes:**

1. **Material 3**
```kotlin
@ThemePlugin
class Material3Theme : MagicTheme {
    override val name = "material3"
    override val basedOn = null
    override val tokens = ThemeTokens(
        colors = Material3Colors,
        typography = Material3Typography,
        // ...
    )
}
```

2. **Cupertino (iOS-like)**
3. **Glass (Modern blur)**

**Specialist:** @vos4-kotlin-expert

---

### Week 6: Theme Switching + VOS4 Integration

#### Day 26-27: Dynamic Theme Switching

**Task 1: GlobalThemeService**
```kotlin
// File: theme/GlobalThemeService.kt
object GlobalThemeService {
    private val _currentTheme = MutableStateFlow<ThemeMode>(ThemeMode.AUTO)
    val currentTheme: StateFlow<ThemeMode> = _currentTheme

    suspend fun setGlobalTheme(theme: ThemeMode) {
        _currentTheme.value = theme
        broadcastThemeChange(theme)
    }
}
```

**Task 2: LiveThemeReactor**
```kotlin
// File: theme/LiveThemeReactor.kt
class LiveThemeReactor(context: Context) {
    fun start()
    fun stop()
    val currentTheme: StateFlow<ThemeMode>
}
```

**Performance Target:** <300ms theme switching

**Specialist:** @vos4-kotlin-expert + @vos4-performance-analyzer

---

#### Day 28-30: VOS4 Automatic Accessibility

**Task 1: Accessibility Auto-Generator**
```kotlin
// File: integration/VOS4Accessibility.kt
object VOS4Accessibility {
    fun generateAccessibility(component: Component): AccessibilityNode {
        return AccessibilityNode(
            label = inferLabel(component),
            voiceCommands = generateVoiceCommands(component),
            role = component.type.toAccessibilityRole(),
            uuid = generateUUID()
        )
    }

    private fun inferLabel(component: Component): String {
        return when (component) {
            is Button -> component.text
            is TextField -> component.label
            is Text -> component.text
            else -> component.toString()
        }
    }

    private fun generateVoiceCommands(component: Component): List<String> {
        return when (component) {
            is Button -> listOf(
                component.text.lowercase(),
                "click ${component.text.lowercase()}",
                "tap ${component.text.lowercase()}",
                "${component.text.lowercase()} button"
            )
            else -> emptyList()
        }
    }
}
```

**Task 2: Override Mechanism**
```kotlin
// Allow explicit override:
Button("Save") { save() }
    .voiceCommands("save", "apply", "done")
    .accessibilityLabel("Save Settings Button")
```

**Specialist:** @vos4-android-expert (VOS4 integration)

---

### Week 7: Magic* Templates

#### Day 31-33: Template System

**5 Magic* Templates:**

1. **MagicLoginScreen**
```kotlin
@Composable
fun MagicLoginScreen(
    title: String = "Sign In",
    onLogin: (email: String, password: String) -> Unit,
    allowSignUp: Boolean = false
) {
    MagicScreen("Login") {
        val email = state("")
        val password = state("")

        VStack(spacing = 16.dp, padding = 20.dp) {
            Text(title).font(.largeTitle).bold()
            TextField("Email", text = email).textContentType(.emailAddress)
            SecureField("Password", text = password)
            Button("Sign In") { onLogin(email.value, password.value) }

            if (allowSignUp) {
                TextButton("Create Account") { /* navigate to signup */ }
            }
        }
    }
}
```

2. **MagicSettingsScreen**
3. **MagicFormScreen**
4. **MagicListScreen**
5. **MagicDashboardScreen**

**Specialist:** @vos4-kotlin-expert

---

#### Day 34-35: Pseudo-3D Components

**Pseudo-3D Components:**
```kotlin
// File: components/Pseudo3DCard.kt
@Composable
fun Pseudo3DCard(
    depth: Dp = 10.dp,
    shadowColor: Color = Color.Black.copy(alpha = 0.3f),
    lightAngle: Float = 45f,
    content: @Composable () -> Unit
)

// File: components/Pseudo3DButton.kt
@Composable
fun Pseudo3DButton(
    text: String,
    onClick: () -> Unit,
    bevelDepth: Dp = 4.dp
)

// File: components/NeumorphicCard.kt
@Composable
fun NeumorphicCard(content: @Composable () -> Unit)
```

**Specialist:** @vos4-kotlin-expert

---

### Week 8: Phase 2 Testing + Completion

#### Day 36-40: Testing, Documentation, Commit

**Test Coverage:**
- Theme switching tests
- VOS4 accessibility generation tests
- Magic* template tests
- Pseudo-3D rendering tests

**Documentation:**
- Theme system guide
- VOS4 integration guide
- Magic* templates reference
- Pseudo-3D components guide

**Performance Benchmarks:**
- Theme switching: <300ms ✓
- VOS4 accessibility generation: <50ms ✓
- Pseudo-3D rendering: 60fps ✓

**Phase 2 Commit**

**Specialist:** @vos4-test-specialist + @vos4-documentation-specialist

---

## Phase 3: Spatial UI & Database (SUMMARY)

### Week 9-10: Android XR + OpenGL

**Deliverables:**
- Jetpack XR integration
- AOSP XR fallback
- SpatialWindow, FloatingElement
- OpenGL ES integration
- Basic 3D model loading
- Pseudo-spatial UI (parallax, layered depth)

**Specialists:** @vos4-android-expert + @vos4-kotlin-expert

---

### Week 11-12: Room Database + LearnApp

**Deliverables:**
- Room database schema (UUID-based)
- DataStore preferences
- LearnApp integration (no scraping)
- Component metadata tracking
- Theme preferences persistence

**Specialists:** @vos4-database-expert + @vos4-test-specialist

---

## Phase 4: Creator Tools & Polish (SUMMARY)

### Week 13-14: CLI Tool

**Deliverables:**
- CLI tool (npm package)
- AI generation (Claude, GPT-4)
- Mockup conversion
- Interactive mode

**Specialists:** External (Node.js team) + @vos4-orchestrator (coordination)

---

### Week 15: Web Platform Foundation

**Deliverables:**
- Web platform skeleton
- Visual designer (basic)
- Cloud deployment

**Specialists:** External (Web team)

---

### Week 16: Documentation + Sample Apps + Launch

**Deliverables:**
- Developer documentation (50+ pages)
- 5 sample apps
- Performance optimization
- Final polish
- v1.0 Release

**Specialists:** @vos4-orchestrator (all specialists)

---

## Testing Strategy

### Unit Tests (90%+ coverage)

**Core DSL:**
- Component creation tests
- State management tests
- Modifier tests
- DSL builder tests

**Themes:**
- Theme loading tests
- Theme switching tests
- Theme plugin tests

**VOS4 Integration:**
- Accessibility generation tests
- Voice command tests
- UUID tracking tests

---

### Integration Tests (80%+ coverage)

**End-to-End Flows:**
- Login screen flow
- Theme switching flow
- Spatial UI rendering
- Database persistence

---

### Performance Tests

**Benchmarks:**
- Startup time: <200ms perceived ✓
- Theme switch: <300ms total ✓
- XR frame rate: 90fps ✓
- Component creation: <50ms ✓

---

## Risk Mitigation

### Risk 1: Performance

**Risk:** Theme switching or XR rendering too slow

**Mitigation:**
- Adaptive performance strategy
- Early profiling (Phase 2)
- Performance budgets enforced
- @vos4-performance-analyzer review

---

### Risk 2: VOS4 Integration Complexity

**Risk:** Automatic accessibility doesn't work well

**Mitigation:**
- Hybrid approach (auto + override)
- Early VOS4 team review (Phase 2)
- Fallback to manual accessibility
- Comprehensive testing

---

### Risk 3: XR Device Compatibility

**Risk:** Spatial UI doesn't work on AOSP devices

**Mitigation:**
- Fallback to pseudo-spatial
- Device detection
- Graceful degradation
- Test on multiple devices

---

### Risk 4: LearnApp Integration

**Risk:** Database schema doesn't meet LearnApp needs

**Mitigation:**
- Early LearnApp team consultation
- UUID-first design
- Database migration support
- Integration tests with LearnApp

---

## Team & Specialists

### VOS4 Subagents (Automatic)

**Primary Specialists:**
- @vos4-kotlin-expert: Core DSL, themes, components
- @vos4-android-expert: VOS4 integration, XR support
- @vos4-database-expert: Room schema, LearnApp integration
- @vos4-test-specialist: All testing (mandatory before each phase)
- @vos4-performance-analyzer: Performance optimization
- @vos4-documentation-specialist: All documentation
- @vos4-architecture-reviewer: Architecture review
- @vos4-orchestrator: Coordination, routing

**External Teams:**
- Node.js team: CLI tool
- Web team: Web platform
- Design team: Theme design

---

## Appendix A: File Structure

```
modules/libraries/MagicUI/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/augmentalis/magicui/
│   │       ├── MagicUI.kt
│   │       ├── core/
│   │       │   ├── MagicScreen.kt
│   │       │   ├── MagicUIScope.kt
│   │       │   └── ComposeAdapter.kt
│   │       ├── components/
│   │       │   ├── Text.kt
│   │       │   ├── Button.kt
│   │       │   ├── TextField.kt
│   │       │   ├── Pseudo3DCard.kt
│   │       │   └── [15+ components]
│   │       ├── modifiers/
│   │       │   ├── TextModifiers.kt
│   │       │   ├── LayoutModifiers.kt
│   │       │   └── [modifier types]
│   │       ├── state/
│   │       │   └── StateHelpers.kt
│   │       ├── theme/
│   │       │   ├── ThemePlugin.kt
│   │       │   ├── ThemeEngine.kt
│   │       │   ├── GlobalThemeService.kt
│   │       │   ├── Material3Theme.kt
│   │       │   └── [theme implementations]
│   │       ├── spatial/
│   │       │   ├── SpatialWindow.kt
│   │       │   ├── FloatingElement.kt
│   │       │   └── PseudoSpatialUI.kt
│   │       ├── integration/
│   │       │   ├── VOS4Accessibility.kt
│   │       │   └── LearnAppIntegration.kt
│   │       ├── database/
│   │       │   ├── MagicUIDatabase.kt
│   │       │   ├── ComponentEntity.kt
│   │       │   └── ThemePreferences.kt
│   │       ├── gl/
│   │       │   ├── GLSurface.kt
│   │       │   ├── Model3D.kt
│   │       │   └── GLRenderer.kt
│   │       └── templates/
│   │           ├── MagicLoginScreen.kt
│   │           ├── MagicSettingsScreen.kt
│   │           └── [template screens]
│   └── test/
│       └── java/com/augmentalis/magicui/
│           ├── [unit tests]
│           └── [integration tests]
└── docs/
    ├── API.md
    ├── Themes.md
    ├── VOS4Integration.md
    └── Examples.md
```

---

## Appendix B: Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.foundation:foundation:1.5.4")

    // Jetpack XR (optional, for spatial UI)
    implementation("androidx.xr:xr-compose:1.0.0")

    // Room (database)
    implementation("androidx.room:room-runtime:2.6.0")
    ksp("androidx.room:room-compiler:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")

    // DataStore (preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // OpenGL ES (3D support)
    // No external dependency (Android built-in)

    // VOS4 integration
    implementation(project(":modules:apps:VoiceOSCore"))

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
```

---

## Document History

**Version 1.0.0** - 2025-10-19 02:40:19 PDT
- Initial implementation plan
- 4 phases defined (16 weeks total)
- Detailed Phase 1 breakdown (YOLO ready)
- Testing strategy complete
- Ready for immediate implementation

---

**Next Step:** Begin Phase 1 YOLO Implementation! 🚀

**Status:** ✅ Plan Complete - Ready to Code
