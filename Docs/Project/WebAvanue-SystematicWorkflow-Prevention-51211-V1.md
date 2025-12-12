# WebAvanue Systematic Workflow - Preventing Recurring Issues

**Version:** 1.0
**Date:** 2025-12-11
**Status:** Active
**Type:** Process Documentation

---

## Purpose

This document defines systematic workflows to **prevent recurring issues** in WebAvanue development. Use these checklists and processes for all feature development, bug fixes, and refactoring.

**Key Principle:** Issues recur when we fix symptoms without addressing root causes. This workflow enforces root cause analysis and systemic fixes.

---

## Table of Contents

1. [Issue Analysis Workflow](#issue-analysis-workflow)
2. [Implementation Checklist](#implementation-checklist)
3. [UI Component Development](#ui-component-development)
4. [State Management Patterns](#state-management-patterns)
5. [Settings Integration](#settings-integration)
6. [Testing Requirements](#testing-requirements)
7. [Code Review Checklist](#code-review-checklist)
8. [IDEACODE Commands for Common Tasks](#ideacode-commands-for-common-tasks)

---

## Issue Analysis Workflow

### Step 1: Reproduce the Issue

**Before touching any code:**

- [ ] Reproduce issue on minimum 2 Android versions (API 26, API 33+)
- [ ] Document exact steps to reproduce
- [ ] Capture screenshots/video
- [ ] Check if issue occurs in portrait AND landscape
- [ ] Note any console errors/logs

**Template:**
```markdown
## Issue Reproduction

**Steps:**
1. Open app
2. Navigate to Settings
3. Toggle "Enable JavaScript"
4. Return to browser
5. Visit test site requiring JavaScript

**Expected:** JavaScript disabled, site shows error
**Actual:** JavaScript still enabled, site works normally

**Platforms Tested:**
- Android 13 (API 33): ❌ Fails
- Android 10 (API 29): ❌ Fails

**Logs:**
```
[No errors in logcat]
```
```

---

### Step 2: Chain of Thought (CoT) Root Cause Analysis

**Use CoT reasoning to trace issue to root cause:**

```
Issue: [User-reported symptom]
↓
Symptom: [Observable behavior]
↓
Investigation: [What code was checked]
↓
Immediate Cause: [Direct cause in code]
↓
Pattern: [Is this an instance of a larger pattern?]
↓
Root Cause: [Systemic issue]
↓
Fix Strategy: [How to prevent recurrence]
```

**Example:**
```
Issue: Settings don't affect browser
↓
Symptom: Toggle JavaScript setting, but JavaScript still works
↓
Investigation: Checked SettingsScreen.kt, SettingsViewModel.kt, SettingsApplicator.kt
↓
Immediate Cause: SettingsApplicator.applySettings() never called
↓
Pattern: UI → ViewModel → Repository works, but Repository → UI → WebView missing
↓
Root Cause: Settings state not observed in BrowserScreen
↓
Fix Strategy: Add LaunchedEffect(settings) { applySettings() } in BrowserScreen
```

---

### Step 3: Scope Analysis

**Determine fix scope:**

| Scope | Description | Example |
|-------|-------------|---------|
| **Local** | Single function fix | Fix typo in function parameter |
| **Component** | Single file/component | Update grid layout in one dialog |
| **Module** | Multiple files in one module | Wire ViewModel to UI across 3 files |
| **Cross-Module** | Multiple modules | Create new shared interface in Common/ |
| **Architectural** | Design pattern change | Move from imperative to reactive state |

**Decision Matrix:**

| If Root Cause Is... | Then Scope Is... | Requires... |
|---------------------|------------------|-------------|
| Typo, logic error | Local | Code fix only |
| Missing UI wiring | Component | UI + ViewModel update |
| State not propagating | Module | State flow refactoring |
| Inconsistent patterns across app | Cross-Module | Create shared components |
| Hardcoded values, no config | Architectural | Settings system redesign |

---

### Step 4: Impact Analysis

**Before implementing fix:**

- [ ] List all files that will change
- [ ] Identify components that depend on changed code
- [ ] Check if fix affects other platforms (iOS, Desktop, Web)
- [ ] Estimate test coverage needed
- [ ] Determine if fix requires database migration
- [ ] Check if fix is breaking change for external APIs

**Template:**
```markdown
## Impact Analysis

**Files Changed:**
- `SettingsViewModel.kt` (line 45-60): Add settings state flow
- `BrowserScreen.kt` (line 100-120): Observe settings, apply to WebView
- `SettingsApplicator.kt` (line 1-200): Refactor to handle all settings

**Dependent Components:**
- BrowserScreen (updated to observe settings)
- WebView (all existing instances must use new applicator)
- SettingsScreen (no changes, already emits settings)

**Platform Impact:**
- Android: Full implementation
- iOS: Need IOSSettingsApplicator.kt (new file)
- Desktop: Need DesktopSettingsApplicator.kt (new file)

**Testing Needed:**
- Unit tests: SettingsViewModel (5 tests)
- Integration tests: Settings → WebView flow (20 tests, one per setting)
- Instrumented tests: End-to-end settings changes (10 scenarios)

**Breaking Changes:**
- None (internal changes only)

**Database Migration:**
- Not required (settings schema unchanged)
```

---

## Implementation Checklist

### For Every Code Change

**Pre-Implementation:**
- [ ] Run CoT analysis (documented above)
- [ ] Create implementation plan with specific line numbers
- [ ] Get approval for architectural changes
- [ ] Create feature branch: `feature/fix-settings-application`

**During Implementation:**
- [ ] Follow Ocean Design System (use OceanComponents, not raw Material3)
- [ ] Use design tokens (OceanDesignTokens.*, not hardcoded colors/spacing)
- [ ] Implement for ALL platforms (Android, iOS, Desktop) if in `commonMain/`
- [ ] Add TODO comments for incomplete implementations
- [ ] Use proper Kotlin coroutines (viewModelScope, avoid GlobalScope)
- [ ] Follow SOLID principles (Single Responsibility, Open/Closed, etc.)

**Post-Implementation:**
- [ ] Write unit tests (90%+ coverage for new code)
- [ ] Write integration tests (critical user flows)
- [ ] Update documentation (README, CLAUDE.md if architecture changed)
- [ ] Run lint: `./gradlew ktlintCheck`
- [ ] Run tests: `./gradlew test`
- [ ] Test on physical device (emulator + real device)
- [ ] Create pull request with detailed description

---

## UI Component Development

### Mandatory Pattern: Ocean Design System

**❌ NEVER do this:**
```kotlin
Button(
    onClick = { /* ... */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF3B82F6),  // Hardcoded color
        contentColor = Color.White
    ),
    shape = RoundedCornerShape(12.dp)  // Hardcoded radius
) {
    Text("Click Me")
}
```

**✅ ALWAYS do this:**
```kotlin
OceanComponents.Button(
    onClick = { /* ... */ },
    variant = ButtonVariant.Primary,  // Uses design tokens
    enabled = true
) {
    Text("Click Me")
}
```

### Grid Layout Pattern

**For all list-style UIs that should fit on screen:**

**❌ NEVER:**
```kotlin
LazyColumn {
    items(items) { item ->
        ItemCard(item)
    }
}
```

**✅ ALWAYS:**
```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 160.dp),
    contentPadding = PaddingValues(OceanDesignTokens.Spacing.md),
    horizontalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.sm),
    verticalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.sm)
) {
    items(items) { item ->
        ItemCard(
            item = item,
            modifier = Modifier.aspectRatio(1.2f)  // Consistent size
        )
    }
}
```

### Orientation Handling Pattern

**For components that differ in portrait/landscape:**

```kotlin
@Composable
fun AdaptiveComponent() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LandscapeLayout()  // Vertical command bar on right
    } else {
        PortraitLayout()   // Horizontal command bar on bottom
    }
}
```

### Touch Target Minimum

**All interactive elements MUST be 48dp minimum:**

```kotlin
IconButton(
    onClick = { /* ... */ },
    modifier = Modifier.size(OceanDesignTokens.Spacing.touchTarget)  // 48.dp
) {
    Icon(...)
}
```

---

## State Management Patterns

### Pattern 1: ViewModel Exposes StateFlow

**✅ Correct:**
```kotlin
class SettingsViewModel(
    private val repository: BrowserRepository
) : ViewModel() {

    // Expose as StateFlow for UI observation
    val settings: StateFlow<BrowserSettings> = repository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),  // Stop after 5s of no subscribers
            initialValue = BrowserSettings.default()
        )

    fun updateSetting(key: String, value: Any) {
        viewModelScope.launch {
            repository.updateSetting(key, value)
            // settings StateFlow automatically updates
        }
    }
}
```

**❌ Incorrect:**
```kotlin
class SettingsViewModel(
    private val repository: BrowserRepository
) : ViewModel() {

    // Don't use mutableStateOf in ViewModel - not lifecycle-aware
    var settings by mutableStateOf(BrowserSettings.default())
        private set

    init {
        viewModelScope.launch {
            repository.getSettings().collect { newSettings ->
                settings = newSettings  // Manual update - error-prone
            }
        }
    }
}
```

### Pattern 2: UI Observes StateFlow with collectAsState()

**✅ Correct:**
```kotlin
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = getViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    // Use settings
    WebView(javaScriptEnabled = settings.enableJavaScript)
}
```

**❌ Incorrect:**
```kotlin
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = getViewModel()
) {
    // Don't access ViewModel state directly without collectAsState
    WebView(javaScriptEnabled = viewModel.settings.value.enableJavaScript)
}
```

### Pattern 3: LaunchedEffect for Side Effects

**Use for:**
- Applying state changes to platform components (WebView, DownloadManager)
- One-time actions on state change
- Cancellable coroutines tied to Composable lifecycle

**✅ Correct:**
```kotlin
@Composable
fun BrowserScreen(
    tabViewModel: TabViewModel,
    settingsViewModel: SettingsViewModel
) {
    val currentTab by tabViewModel.currentTab.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    var webView: WebView? by remember { mutableStateOf(null) }

    // Apply settings whenever they change OR webView changes
    LaunchedEffect(settings, webView) {
        webView?.let { view ->
            SettingsApplicator.applySettings(view, settings)
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).also { webView = it }
        }
    )
}
```

**❌ Incorrect:**
```kotlin
@Composable
fun BrowserScreen(
    tabViewModel: TabViewModel,
    settingsViewModel: SettingsViewModel
) {
    val settings by settingsViewModel.settings.collectAsState()

    // Don't apply settings directly in composition - will run on every recomposition
    AndroidView(
        factory = { context ->
            val view = WebView(context)
            SettingsApplicator.applySettings(view, settings)  // ❌ Runs every recomposition
            view
        }
    )
}
```

### Pattern 4: Focus State Management

**For text fields that need to react to external state changes:**

**✅ Correct:**
```kotlin
@Composable
fun AddressBar(
    currentTab: Tab,
    onNavigate: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentTab.url) }
    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Reset URL and clear focus when tab changes
    LaunchedEffect(currentTab.id) {
        if (!isEditing) {
            url = currentTab.url
        }
        focusManager.clearFocus()
    }

    // Update URL when tab navigates (but not when user is typing)
    LaunchedEffect(currentTab.url) {
        if (!isEditing) {
            url = currentTab.url
        }
    }

    TextField(
        value = url,
        onValueChange = { url = it },
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isEditing = focusState.isFocused
                if (!focusState.isFocused && url != currentTab.url) {
                    url = currentTab.url  // Reset if user didn't navigate
                }
            },
        keyboardActions = KeyboardActions(
            onGo = {
                onNavigate(url)
                focusManager.clearFocus()
                isEditing = false
            }
        )
    )
}
```

---

## Settings Integration

### Complete Settings Flow

**Every setting MUST follow this flow:**

```
1. SettingsScreen.kt
   ↓ User toggles setting
2. SettingsViewModel.updateSetting(key, value)
   ↓ Calls repository
3. BrowserRepository.updateSetting(key, value)
   ↓ Saves to database
4. BrowserRepository.getSettings() emits new BrowserSettings
   ↓ StateFlow updates
5. BrowserScreen observes settings StateFlow
   ↓ collectAsState() triggers recomposition
6. LaunchedEffect(settings) detects change
   ↓ Calls SettingsApplicator
7. SettingsApplicator.applySettings(webView, settings)
   ↓ Updates WebView configuration
8. WebView behavior changes
   ✅ Setting applied!
```

### Settings Applicator Template

**For each new setting category, create applicator method:**

```kotlin
// SettingsApplicator.kt

private fun applyNewSettingCategory(webView: WebView, settings: BrowserSettings) {
    webView.settings.apply {
        // Map each setting to WebView property
        newSetting1 = settings.newSetting1
        newSetting2 = settings.newSetting2

        // For complex settings, use when expressions
        when (settings.complexSetting) {
            SettingValue.OPTION_A -> {
                webViewProperty1 = valueA
                webViewProperty2 = valueB
            }
            SettingValue.OPTION_B -> {
                webViewProperty1 = valueC
                webViewProperty2 = valueD
            }
        }
    }

    // For settings requiring non-WebSettings APIs
    if (settings.requiresSpecialAPI) {
        SomeManager.getInstance().setProperty(value)
    }
}

// Add to main applySettings() function
fun applySettings(webView: WebView, settings: BrowserSettings) {
    applyPrivacySettings(webView, settings)
    applyDisplaySettings(webView, settings)
    applyPerformanceSettings(webView, settings)
    applyNewSettingCategory(webView, settings)  // ← Add here
}
```

### Settings Testing Checklist

For every new setting:

- [ ] Unit test: ViewModel updates setting in repository
- [ ] Integration test: Repository saves to database and emits new state
- [ ] Instrumented test: UI change triggers WebView change
- [ ] Visual test: Verify setting effect is visible to user
- [ ] Regression test: Ensure setting persists across app restarts

**Example Test:**
```kotlin
@Test
fun `when JavaScript disabled, WebView blocks JavaScript`() = runInstrumentedTest {
    // 1. Open settings
    onView(withId(R.id.menu_button)).perform(click())
    onView(withText("Settings")).perform(click())
    onView(withText("Privacy & Security")).perform(click())

    // 2. Disable JavaScript
    onView(withText("Enable JavaScript")).perform(click())

    // 3. Return to browser
    pressBack()
    pressBack()

    // 4. Verify WebView has JavaScript disabled
    val webView = getWebViewFromActivity()
    assertFalse(webView.settings.javaScriptEnabled)

    // 5. Navigate to JS test site
    onView(withId(R.id.address_bar)).perform(
        typeText("https://javascript-test.com"),
        pressImeActionButton()
    )

    // 6. Verify JS didn't execute
    Thread.sleep(2000)  // Wait for page load
    val pageContent = getWebViewContent(webView)
    assertTrue(pageContent.contains("JavaScript is disabled"))
}
```

---

## Testing Requirements

### Test Coverage Targets

| Code Type | Coverage Target | Priority |
|-----------|----------------|----------|
| ViewModels | 95%+ | P0 |
| Repositories | 90%+ | P0 |
| UI Components (critical) | 80%+ | P1 |
| UI Components (non-critical) | 60%+ | P2 |
| Utilities | 90%+ | P1 |

### Test Pyramid

```
        /\
       /  \       10% - E2E Tests (Instrumented, slow)
      /____\
     /      \     30% - Integration Tests (Multiple classes)
    /________\
   /          \   60% - Unit Tests (Single class, fast)
  /__________  \
```

### Mandatory Tests for Each Issue Fix

**For UI Issues:**
- [ ] Compose UI test (verify layout, interactions)
- [ ] Screenshot test (visual regression)
- [ ] Accessibility test (touch targets, contrast)

**For State Issues:**
- [ ] ViewModel unit test (state transitions)
- [ ] Repository integration test (database persistence)
- [ ] End-to-end instrumented test (full flow)

**For Settings Issues:**
- [ ] Settings toggle test (UI → ViewModel)
- [ ] Settings persistence test (Repository → Database)
- [ ] Settings application test (ViewModel → WebView)

### Test Naming Convention

```kotlin
// Pattern: `when [action], then [expected result]`

@Test
fun `when tab changes while address bar focused, then URL updates and focus clears`()

@Test
fun `when JavaScript disabled in settings, then WebView blocks JavaScript execution`()

@Test
fun `when download started, then status changes from PENDING to DOWNLOADING`()
```

---

## Code Review Checklist

### For Reviewer

**Architecture:**
- [ ] Uses Ocean Design System (not raw Material3)
- [ ] Uses design tokens (not hardcoded values)
- [ ] Follows SOLID principles
- [ ] No code duplication (DRY)
- [ ] Appropriate separation of concerns

**State Management:**
- [ ] ViewModel exposes StateFlow (not mutableStateOf)
- [ ] UI uses collectAsState() to observe
- [ ] Side effects in LaunchedEffect (not in composition)
- [ ] Coroutines use viewModelScope (not GlobalScope)

**Testing:**
- [ ] Unit tests for new ViewModels/Repositories
- [ ] Integration tests for multi-component features
- [ ] Instrumented tests for critical user flows
- [ ] Tests follow naming convention
- [ ] Coverage meets targets

**Documentation:**
- [ ] Public functions have KDoc comments
- [ ] Complex logic has inline comments
- [ ] TODOs reference issue numbers
- [ ] README updated if architecture changed

**Platform Compatibility:**
- [ ] Implements for all target platforms (Android, iOS, Desktop)
- [ ] Uses expect/actual for platform-specific code
- [ ] No Android-specific code in commonMain

**Performance:**
- [ ] No blocking operations on main thread
- [ ] Large lists use LazyColumn/LazyVerticalGrid
- [ ] Images loaded asynchronously
- [ ] Database queries use coroutines

---

## IDEACODE Commands for Common Tasks

### For Bug Fixes

**Step 1: Analyze the Issue**
```bash
/i.analyze .code .cot [file-or-folder]
```
Use CoT reasoning to trace root cause.

**Step 2: Create Fix Plan**
```bash
/i.plan .cot "Fix [issue-name]"
```
Generate step-by-step fix plan.

**Step 3: Implement Fix**
```bash
/i.fix .yolo .tdd "[issue-description]"
```
Implement with test-driven development.

**Step 4: Review Implementation**
```bash
/i.review .code [changed-files]
```
Review for quality and patterns.

---

### For New Features

**Step 1: Create Specification**
```bash
/i.specify .cot .tot "[feature-description]"
```
Use Chain of Thought + Tree of Thought for thorough spec.

**Step 2: Create Implementation Plan**
```bash
/i.plan .cot spec.md
```
Generate detailed implementation plan.

**Step 3: Implement with Tests**
```bash
/i.implement .tdd .ood plan.md
```
Implement using TDD and Object-Oriented Design principles.

**Step 4: Review and Refactor**
```bash
/i.review .code src/
/i.refactor .ood .solid [component]
```
Review and refactor to enforce SOLID.

---

### For UI Components

**Step 1: Design UI**
```bash
/i.createui .app "[component-name]"
```
Generate UI design with ASCII preview.

**Step 2: Analyze Existing Patterns**
```bash
/i.analyze .ui screenshot.png
```
Analyze screenshot to understand patterns.

**Step 3: Implement Component**
```bash
/i.develop .yolo "Create [component-name] using Ocean Design System"
```
Implement following design system.

---

### For Settings Integration

**Complete Workflow:**
```bash
# 1. Analyze existing settings flow
/i.analyze .code .workflow Modules/WebAvanue/universal/src/commonMain/kotlin/presentation/ui/settings/

# 2. Create plan for new setting
/i.plan .cot "Add new setting: [setting-name]"

# 3. Implement setting end-to-end
/i.implement .tdd plan.md

# 4. Test setting application
/i.review .code Modules/WebAvanue/universal/src/androidMain/kotlin/platform/SettingsApplicator.kt
```

---

## Quick Reference

### Issue Recurrence Prevention Checklist

**Before closing any issue:**

- [ ] Root cause identified (not just symptom fixed)
- [ ] Fix applied systematically (not just in one place)
- [ ] Tests added to prevent regression
- [ ] Documentation updated
- [ ] Code review completed
- [ ] Verified on multiple devices/orientations
- [ ] No hardcoded values introduced
- [ ] Ocean Design System used throughout
- [ ] Settings properly wired (if applicable)

### Common Anti-Patterns to Avoid

| ❌ Anti-Pattern | ✅ Correct Pattern |
|----------------|-------------------|
| Hardcoded colors/spacing | Use OceanDesignTokens.* |
| Direct Material3 calls | Use OceanComponents.* |
| LazyColumn for grid UIs | Use LazyVerticalGrid |
| mutableStateOf in ViewModel | Use StateFlow + stateIn() |
| Side effects in composition | Use LaunchedEffect |
| GlobalScope.launch | Use viewModelScope.launch |
| Blocking DB calls | Use suspend functions + coroutines |
| Android code in commonMain | Use expect/actual pattern |

---

**Document Version:** 1.0
**Last Updated:** 2025-12-11
**Maintained By:** WebAvanue Team
**Review Frequency:** After each sprint or when patterns change
