# Settings Screen Refresh & Reload Fixes

## Problem Summary

The **Settings** screen was unnecessarily refreshing and redrawing on every minor change, such as:

- Typing or deleting a single character in the **Homepage URL** input field

- Toggling a switch near the bottom of the screen

### Observed Issues

- Keyboard hides while typing

- Screen scrolls back to the top after toggling a switch

- Entire Settings screen reloads on every update

---

## Root Cause Analysis

1. **`isLoading` misuse**
   
   - `SettingsViewModel.updateSettings()` was toggling `isLoading = true` on every settings update.
   
   - This caused the UI to switch between loading state and content state repeatedly.
   
   - Result: **focus loss**, **keyboard dismissal**, and **full recomposition**.

2. **No preserved scroll state**
   
   - `LazyColumn` in `SettingsContent` did not use a remembered `LazyListState`.
   
   - Any recomposition caused the list to reset to the top.

3. **Write-through updates on every keystroke**
   
   - Homepage text field persisted changes to the database on **every character typed**.
   
   - This amplified recompositions and state emissions.

---

## Fixes Implemented

---

## 1. Separate `isLoading` and `isSaving` States (ViewModel Fix)

### Before

- `isLoading` was used for:
  
  - Initial settings load
  
  - Every settings update (toggle / text change)

### After

- `isLoading` → **used only for initial load**

- New `isSaving` → **used for background save operations**

### Changes in `SettingsViewModel.kt`

`private val _isSaving = MutableStateFlow(false) val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()  fun updateSettings(settings: BrowserSettings) {     viewModelScope.launch {         _isSaving.value = true         _error.value = null         _saveSuccess.value = false          // Optimistic UI update         _settings.value = settings         repository.updateSettings(settings)             .onSuccess {                 _saveSuccess.value = true             }             .onFailure { e ->                 _error.value = "Failed to save settings: ${e.message}"             }         _isSaving.value = false     } }`

### Result

- UI no longer swaps between loading and content

- Keyboard focus is preserved

- Screen no longer redraws entirely on save

---

## 2. Update Settings Screen UI Logic

### Key Change

- **Show full-screen loader only on first load**

- During save, show a **non-blocking progress indicator**

### Changes in `SettingsScreen.kt`

`val settings by viewModel.settings.collectAsState() val isLoading by viewModel.isLoading.collectAsState() val isSaving by viewModel.isSaving.collectAsState() val error by viewModel.error.collectAsState() Scaffold(     topBar = {         Column {             TopAppBar(title = { Text("Settings") })            if (isSaving) {                 LinearProgressIndicator(modifier = Modifier.fillMaxWidth())             }         }     } ) { paddingValues ->    when {         settings == null && isLoading -> {             CircularProgressIndicator()         }         error != null && settings == null -> {             Text(error!!)         }         settings != null -> {             SettingsContent(                 settings = settings!!,                 onUpdateSettings = { viewModel.updateSettings(it) },                 modifier = Modifier.padding(paddingValues)             )         }     } }`

### Result

- Save operations no longer destroy UI hierarchy

- Keyboard remains open

- Smooth UX during updates

---

## 3. Preserve Scroll Position in `LazyColumn`

### Problem

- Toggling any switch caused the list to jump back to the top

### Fix

- Use a remembered `LazyListState` with `rememberSaveable`

### Changes in `SettingsContent`

`val listState = rememberSaveable(     saver = LazyListState.Saver ) {     LazyListState() } LazyColumn(     state = listState,     modifier = modifier.fillMaxSize(),     contentPadding = PaddingValues(vertical = 8.dp) ) {    // items }`

### Result

- Scroll position is preserved

- Toggling settings no longer resets the screen position

---

## 4. Debounce Homepage Input Saves

### Problem

- Homepage input saved to DB on **every keystroke**

- Caused frequent state updates and recompositions

### Fix

- Introduced **local state + debounce** before persisting

### Changes in `HomepageSettingItem`

`@Composable fun HomepageSettingItem(     currentHomepage: String,     onHomepageChanged: (String) -> Unit ) {    var text by rememberSaveable { mutableStateOf(currentHomepage) }    val scope = rememberCoroutineScope()    var job by remember { mutableStateOf<Job?>(null) }     LaunchedEffect(currentHomepage) {        if (currentHomepage != text) text = currentHomepage     }     OutlinedTextField(         value = text,         onValueChange = { newValue ->             text = newValue             job?.cancel()             job = scope.launch {                 delay(400)                 onHomepageChanged(newValue)             }         },         label = { Text("Homepage") },         singleLine = true,         modifier = Modifier.fillMaxWidth()     ) }`

### Result

- Typing feels smooth

- No keyboard dismissal

- Settings saved only after user pauses typing

---

## Final Outcome

### Issues Resolved ✅

- ❌ Keyboard hiding on input

- ❌ Screen refresh on every change

- ❌ Scroll reset to top

- ❌ Excessive recompositions

### UX Improvements 🚀

- Smooth inline updates

- Stable keyboard focus

- Preserved scroll position

- Non-blocking save feedback

---

## Optional Future Improvements

- Add `distinctUntilChanged()` on settings Flow to avoid redundant emissions

- Persist homepage only on IME `Done` action (alternative UX)

- Add save success/error snackbar feedback

---

**Status:** ✅ Fixed  
**Scope:** `SettingsScreen`, `SettingsViewModel`, `SettingsContent`, `HomepageSettingItem`
