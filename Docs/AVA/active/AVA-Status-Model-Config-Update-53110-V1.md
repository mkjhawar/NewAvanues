# Status: Model Configuration Update - mALBERT as Default

**Date**: 2025-10-31 12:45 PDT
**Status**: ✅ Complete
**Change**: mALBERT now default, MobileBERT via user settings

---

## 🎯 Summary

Updated AVA's NLU configuration to make **mALBERT (multilingual, 52 languages) the default model**, with MobileBERT (English-only) available as a user-selectable option for testing purposes.

**Key Changes**:
- ✅ mALBERT is now the default NLU model
- ✅ MobileBERT can be selected via settings UI
- ✅ User preferences persist across app restarts
- ✅ VisionOS-style settings dialog
- ✅ Requires app restart to switch models

---

## 🔄 What Changed

### 1. Build Configuration
**File**: `features/nlu/build.gradle.kts`

**Before** (Product Flavors):
```kotlin
flavorDimensions += "nlu"
productFlavors {
    create("lite") {  // MobileBERT
        buildConfigField("String", "NLU_MODEL_TYPE", "\"MOBILEBERT_ENGLISH\"")
    }
    create("full") {  // mALBERT
        buildConfigField("String", "NLU_MODEL_TYPE", "\"MALBERT_MULTILINGUAL\"")
    }
}
```

**After** (Removed flavors, set default):
```kotlin
defaultConfig {
    buildConfigField(
        "String",
        "DEFAULT_NLU_MODEL_TYPE",
        "\"MALBERT_MULTILINGUAL\""  // mALBERT is default
    )
    buildConfigField(
        "boolean",
        "ALLOW_MODEL_SWITCHING",
        "true"  // Users can switch via settings
    )
}
```

**Result**: Single build variant with mALBERT as default, user-selectable model.

### 2. Preferences Manager
**File**: `features/nlu/.../preferences/NLUPreferences.kt` (NEW)

**Features**:
- Stores user's selected model in SharedPreferences
- Default: `MALBERT_MULTILINGUAL`
- Persists across app restarts
- Singleton pattern for efficient access

**API**:
```kotlin
class NLUPreferences(context: Context) {
    fun getSelectedModelType(): NLUModelType
    fun setSelectedModelType(modelType: NLUModelType)
    fun getDefaultModelType(): NLUModelType  // Returns mALBERT
    fun resetToDefault()
    fun isModelSwitchingAllowed(): Boolean
    fun getAvailableModels(): List<NLUModelType>
}
```

### 3. NLUModelFactory Updates
**File**: `features/nlu/.../NLUModelFactory.kt`

**Changes**:
- Added `getModelTypeFromPreferences(context)` - Reads user preference
- Deprecated `getModelTypeFromBuildConfig()` - Old build-time selection
- Updated `createFromPreferences(context)` - New method using preferences
- Deprecated `createFromBuildConfig(context)` - Redirects to new method

**New API**:
```kotlin
// Primary method (replaces old approach)
fun getModelTypeFromPreferences(context: Context): NLUModelType {
    val prefs = NLUPreferences.getInstance(context)
    return prefs.getSelectedModelType()  // Default: mALBERT
}

// Convenience method
fun createFromPreferences(context: Context): INLUModel {
    val modelType = getModelTypeFromPreferences(context)
    return createModel(modelType, context)
}
```

### 4. ClassifyIntentUseCase Update
**File**: `features/nlu/.../ClassifyIntentUseCase.kt`

**Change**:
```kotlin
// OLD
val model = NLUModelFactory.createFromBuildConfig(context)

// NEW
val model = NLUModelFactory.createFromPreferences(context)
```

### 5. Settings UI Component
**File**: `features/nlu/.../ui/ModelSelectionDialog.kt` (NEW)

**Features**:
- VisionOS-style dialog
- Shows both model options:
  - **mALBERT** (Default badge, 52 languages, 41 MB)
  - **MobileBERT** (Testing, 1 language, 25 MB)
- Displays model metadata (languages, size)
- Apply button (disabled if no change)
- Shows "Restart Required" message
- Cancel button

**Visual Design**:
```
┌──────────────────────────────────────┐
│          🧠                          │
│     Select NLU Model                 │
│  Choose the AI model for...         │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ mALBERT         [DEFAULT] ✓   │ │
│  │ 52 languages                   │ │
│  │ 41 MB                          │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │ MobileBERT                     │ │
│  │ 1 language                     │ │
│  │ 25 MB                          │ │
│  │ For testing purposes           │ │
│  └────────────────────────────────┘ │
│                                      │
│  [Apply (Restart Required)]         │
│  [Cancel]                            │
└──────────────────────────────────────┘
```

### 6. ChatScreen Integration
**File**: `features/chat/.../ui/ChatScreen.kt`

**Changes**:
- Added Settings icon to top bar (replaced "More" icon)
- Added `onOpenSettings` callback parameter
- Shows ModelSelectionDialog when settings clicked
- Displays current model name in top bar

**Top Bar Update**:
- **Before**: "More" icon (⋮)
- **After**: "Settings" icon (⚙️)
- **Function**: Opens model selection dialog

---

## 📊 Model Comparison

| Feature | mALBERT (Default) | MobileBERT (Testing) |
|---------|-------------------|----------------------|
| **Status** | ✅ Default | Optional (via settings) |
| **Languages** | 52 (multilingual) | 1 (English only) |
| **Model Size** | 41 MB | 25 MB |
| **Vocab Size** | 128,000 | 30,522 |
| **Tokenizer** | SentencePiece | WordPiece |
| **Inference** | <80ms target | <50ms target |
| **Use Case** | Production (global) | Testing (English markets) |

---

## 🔄 User Flow

### Changing Model

1. **Open Settings**:
   - Tap Settings icon (⚙️) in top bar

2. **Model Selection Dialog**:
   - See current model (mALBERT, marked with ✓)
   - See alternative (MobileBERT)
   - View model details (languages, size)

3. **Select Model**:
   - Tap desired model option
   - Model gets highlighted with blue border

4. **Apply Change**:
   - Tap "Apply (Restart Required)" button
   - Preference saved to SharedPreferences

5. **Restart App**:
   - User must manually restart app
   - New model loads on next launch

### First Launch (Default)

```
App Launch
    ↓
NLUPreferences.getSelectedModelType()
    ↓
No preference found
    ↓
Return default: MALBERT_MULTILINGUAL
    ↓
Load mALBERT model (52 languages)
```

---

## 🎨 Settings UI Design

### VisionOS Style Elements

- **Glassmorphism**: Frosted glass background
- **Shadow Depth**: 20dp elevation
- **Corner Radius**: 28dp (dialog), 16dp (cards)
- **Colors**:
  - Selected: iOS Blue (`#007AFF`) with 10% alpha background
  - Unselected: Light gray (`#E5E5EA`) with 50% alpha
  - Default badge: Green (`#34C759`)
  - Testing label: Orange (`#FF9500`)

### Model Cards

Each model option displays:
- **Name**: Bold, 18sp
- **Badge**: "DEFAULT" (green) for mALBERT
- **Languages**: "52 languages" / "1 language"
- **Size**: "41 MB" / "25 MB"
- **Note**: "For testing purposes" (MobileBERT)
- **Check Icon**: Blue checkmark when selected

---

## 💻 Code Examples

### Getting Current Model

```kotlin
val context = LocalContext.current
val prefs = NLUPreferences.getInstance(context)
val currentModel = prefs.getSelectedModelType()

when (currentModel) {
    NLUModelType.MALBERT_MULTILINGUAL -> {
        println("Using mALBERT (52 languages)")
    }
    NLUModelType.MOBILEBERT_ENGLISH -> {
        println("Using MobileBERT (English only)")
    }
}
```

### Changing Model

```kotlin
val prefs = NLUPreferences.getInstance(context)

// Switch to MobileBERT for testing
prefs.setSelectedModelType(NLUModelType.MOBILEBERT_ENGLISH)

// Reset to default (mALBERT)
prefs.resetToDefault()
```

### Using in UI

```kotlin
@Composable
fun MyScreen() {
    var showModelDialog by remember { mutableStateOf(false) }

    // Settings button
    IconButton(onClick = { showModelDialog = true }) {
        Icon(Icons.Default.Settings, "Settings")
    }

    // Model selection dialog
    if (showModelDialog) {
        ModelSelectionDialog(
            onDismiss = { showModelDialog = false },
            onModelSelected = { newModel ->
                // Preference saved automatically
                // Show "Restart app" message
                showModelDialog = false
            }
        )
    }
}
```

---

## 🧪 Testing

### Manual Testing Checklist

- [x] Default model is mALBERT on first launch
- [x] Settings icon appears in top bar
- [x] Settings dialog opens correctly
- [x] Both models listed with correct metadata
- [x] mALBERT shows "DEFAULT" badge
- [x] MobileBERT shows "For testing" note
- [x] Selection updates with checkmark
- [x] Apply button enabled only when changed
- [x] Preference persists after app restart
- [x] Model switches correctly on restart

### Unit Tests (TODO)

```kotlin
class NLUPreferencesTest {
    @Test
    fun `default model is mALBERT`()

    @Test
    fun `setSelectedModelType saves preference`()

    @Test
    fun `getSelectedModelType reads preference`()

    @Test
    fun `resetToDefault sets mALBERT`()
}
```

---

## 📝 Migration Guide

### For Existing Code Using Build Flavors

**Before**:
```bash
# Build commands
./gradlew assembleLiteDebug    # MobileBERT
./gradlew assembleFullDebug    # mALBERT
```

**After**:
```bash
# Single build command (mALBERT default)
./gradlew assembleDebug

# Users switch models in-app via settings
```

### For Code Using createFromBuildConfig()

**Before**:
```kotlin
val model = NLUModelFactory.createFromBuildConfig(context)
```

**After**:
```kotlin
val model = NLUModelFactory.createFromPreferences(context)
```

**Note**: Old method still works (redirects to new method) but is deprecated.

---

## 🎯 Benefits

### 1. **Simplified Build Process**
- ❌ No more product flavors
- ✅ Single build variant
- ✅ Faster compilation
- ✅ Simpler CI/CD

### 2. **Better User Experience**
- ✅ Users can test both models
- ✅ No need to download separate APKs
- ✅ Flexible for different use cases
- ✅ Easy to switch for testing

### 3. **Default Multilingual**
- ✅ mALBERT (52 languages) is default
- ✅ Better for global users
- ✅ MobileBERT available for English-only testing
- ✅ Aligns with privacy-first, multilingual vision

### 4. **Testing Flexibility**
- ✅ Developers can easily switch models
- ✅ Users can test English-only mode
- ✅ Performance comparison possible
- ✅ Easy A/B testing

---

## 🐛 Known Limitations

### 1. **Requires App Restart**
- **Issue**: Model switch needs app restart to take effect
- **Reason**: Models are loaded at startup
- **Mitigation**: Dialog shows "Restart Required" message
- **Future**: Hot-swap models without restart (Phase 2)

### 2. **No Automatic Model Download**
- **Issue**: Both models must be in APK
- **Reason**: No dynamic model loading yet
- **Impact**: APK includes both models (~66 MB total)
- **Future**: Download models on-demand (Phase 3)

### 3. **No A/B Testing Metrics**
- **Issue**: No automatic usage tracking
- **Reason**: Analytics not implemented
- **Mitigation**: Manual testing and user feedback
- **Future**: Analytics integration (Phase 2)

---

## 📚 Files Modified/Created

### Modified (3 files)
1. `features/nlu/build.gradle.kts` - Removed flavors, added default config
2. `features/nlu/.../NLUModelFactory.kt` - Added preferences methods
3. `features/nlu/.../ClassifyIntentUseCase.kt` - Use preferences instead of build config
4. `features/chat/.../ui/ChatScreen.kt` - Added settings button

### Created (2 files)
1. `features/nlu/.../preferences/NLUPreferences.kt` - Preferences manager (120 lines)
2. `features/nlu/.../ui/ModelSelectionDialog.kt` - Settings dialog (320 lines)

---

## 🚀 Deployment

### Build Commands

```bash
# Clean build
./gradlew clean

# Build release APK (mALBERT default)
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Launch app
adb shell am start -n com.augmentalis.ava/.MainActivity
```

### APK Size

- **Before**: 2 APKs (Lite: 4.23 GB, Full: 4.28 GB)
- **After**: 1 APK (~4.28 GB) with both models

---

## 🎉 Conclusion

**Configuration update complete!** AVA now:

✅ **Defaults to mALBERT**: Best experience for global users (52 languages)
✅ **Allows MobileBERT**: Testing option via settings
✅ **User-Friendly**: VisionOS-style settings dialog
✅ **Persistent**: Preference saved across restarts
✅ **Flexible**: Easy to switch for testing

**Impact**: Better out-of-box experience for multilingual users, with testing flexibility for developers.

---

**Created by**: AVA Team
**Last Updated**: 2025-10-31 12:45 PDT
**Status**: ✅ Complete
**Change Type**: Configuration Update

---

**Default Model**: mALBERT (Multilingual, 52 languages) 🌍
**Optional Model**: MobileBERT (English-only, for testing) 🧪
