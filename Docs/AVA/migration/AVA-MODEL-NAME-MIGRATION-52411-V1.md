# Model Name Migration Guide

**Issue:** App tries to load old model name `AVA-GEM-2B-Q4` which doesn't exist
**Fixed:** Updated to new naming convention `AVA-GE2-2B16` / `AVA-GE3-4B16`

## What Was Changed

### Old Names → New Names
- `AVA-GEM-2B-Q4` → `AVA-GE2-2B16`
- `AVA-GEM-4B-Q4` → `AVA-GE3-4B16`

## Files Fixed

1. **ChatPreferences.kt** - Default model name
2. **ModelSelector.kt** - Available models list and fallback
3. **ChatViewModel.kt** - Model path configuration

## For Users: Clear App Data

The old model name is cached in SharedPreferences. To fix:

### Option 1: Clear App Data (Recommended)
```bash
adb shell pm clear com.augmentalis.ava.debug
# or
adb shell pm clear com.augmentalis.ava
```

### Option 2: Clear Only Preferences
```bash
adb shell
run-as com.augmentalis.ava.debug
cd shared_prefs
rm chat_preferences.xml
exit
exit
```

### Option 3: Manually Update in Code
Add migration logic to `ChatPreferences.kt`:

```kotlin
// In getInstance() or init block
private fun migrateOldModelNames() {
    val currentModel = prefs.getString(KEY_SELECTED_LLM_MODEL, null)
    val migratedModel = when (currentModel) {
        "AVA-GEM-2B-Q4" -> "AVA-GE2-2B16"
        "AVA-GEM-4B-Q4" -> "AVA-GE3-4B16"
        else -> currentModel
    }
    if (migratedModel != currentModel && migratedModel != null) {
        setSelectedLLMModel(migratedModel)
        Timber.i("Migrated model name: $currentModel -> $migratedModel")
    }
}
```

## Why This Happened

The app stores the selected model name in SharedPreferences. When we renamed the models to follow AVA Naming Convention v2.0, existing installations still had the old name cached.

## Prevention

Future model renames should include migration logic in `ChatPreferences.kt`.
