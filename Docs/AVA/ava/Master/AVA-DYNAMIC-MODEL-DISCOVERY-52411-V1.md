# Dynamic Model Discovery Implementation

**Version:** 1.0
**Last Updated:** 2025-11-24
**Status:** ✅ Complete

---

## Overview

The AVA app now **dynamically discovers** LLM models installed on device storage instead of using hardcoded model names. This eliminates "Model not found" errors and allows flexible model deployment.

---

## Key Changes

### ❌ OLD APPROACH (Hardcoded)
```kotlin
private const val DEFAULT_LLM_MODEL = "AVA-GE2-2B16"  // ❌ Hardcoded

val model = when (selectedModel) {
    "AVA-GE3-4B16" -> config1  // ❌ Hardcoded
    "AVA-GE2-2B16" -> config2  // ❌ Hardcoded
    else -> defaultConfig
}
```

### ✅ NEW APPROACH (Dynamic)
```kotlin
private const val DEFAULT_LLM_MODEL: String? = null  // ✅ No default

val discovery = ModelDiscovery(context)
val model = discovery.getFirstAvailableModel()  // ✅ Discovers installed models

if (model != null) {
    val config = LLMConfig(
        modelPath = model.path,           // ✅ From discovery
        modelLib = "${model.id}.ADco"     // ✅ Dynamic
    )
}
```

---

## How It Works

### 1. Model Discovery

```kotlin
val discovery = ModelDiscovery(context)
val models = discovery.discoverInstalledModels()

// Returns list of all installed models found in:
// - /sdcard/ava-ai-models/llm/
// - <external-files-dir>/ava-ai-models/llm/
// - <files-dir>/models/llm/
```

### 2. Validation

Each directory is checked for required files:
- ✅ `AVALibrary.ADco` (TVM runtime library)
- ✅ `tokenizer.model` (Tokenizer)
- ✅ At least one `.ADco` device code file

Invalid directories are skipped automatically.

### 3. Selection Priority

```kotlin
val model = discovery.getFirstAvailableModel()

// Priority:
// 1. Extracted directories over .ALM archives
// 2. Larger models over smaller (usually better quality)
// 3. Alphabetically first if same size
```

---

## Files Modified

### 1. ChatPreferences.kt
**Changes:**
- ❌ Removed hardcoded `DEFAULT_LLM_MODEL = "AVA-GE2-2B16"`
- ✅ Changed to `DEFAULT_LLM_MODEL = null`
- ✅ `getSelectedLLMModel()` now returns `String?` (nullable)
- ✅ Auto-migration from old model names still works
- ✅ Returns null if no model selected (caller discovers)

### 2. ChatViewModel.kt
**Changes:**
- ❌ Removed hardcoded model name matching
- ✅ Uses `ModelDiscovery` to find installed models
- ✅ Selects first available if none previously selected
- ✅ Builds config dynamically from discovered model
- ✅ Logs warning if no models installed
- ✅ Shows helpful message about installation location

### 3. ModelSelector.kt
**Changes:**
- ❌ Removed hardcoded fallback to "AVA-GE2-2B16"
- ✅ Uses `ModelDiscovery` for fallback
- ✅ Returns "NO_MODEL_INSTALLED" if none found
- ✅ Model list still has model info (for future UI)

---

## New Components

### 1. ModelDiscovery.kt
**Location:** `Features/LLM/src/main/java/.../loader/ModelDiscovery.kt`

**Key Methods:**
```kotlin
// Discover all installed models
suspend fun discoverInstalledModels(): List<DiscoveredModel>

// Get first available (auto-select)
suspend fun getFirstAvailableModel(): DiscoveredModel?

// Check if specific model installed
suspend fun isModelInstalled(modelId: String): Boolean

// Get model by ID
suspend fun getModelById(modelId: String): DiscoveredModel?
```

**DiscoveredModel Data:**
```kotlin
data class DiscoveredModel(
    val id: String,                    // e.g., "AVA-GE3-4B16"
    val name: String,                  // Display name
    val path: String,                  // Absolute path
    val sizeBytes: Long,               // Total size
    val type: ModelType,               // ARCHIVE or EXTRACTED
    val metadata: Map<String, String>  // From config.json
)
```

### 2. ModelDownloader.kt (Placeholder)
**Location:** `Features/LLM/src/main/java/.../loader/ModelDownloader.kt`

**Status:** Commented out - waiting for download URL

**TODOs Added:**
```kotlin
// TODO #backlog: Configure download server URL
// TODO #backlog: Implement authentication if required
// TODO #backlog: Add resume support for failed downloads
// TODO #backlog: Verify checksums after download
// TODO #backlog: Show download progress in UI
```

**Example download locations to configure:**
1. Self-hosted: `https://models.augmentalis.com/ava/llm/`
2. HuggingFace: `https://huggingface.co/augmentalis/ava-models/`
3. GitHub Releases: `https://github.com/augmentalis/ava-models/releases/`
4. Cloud Storage: Google Cloud, AWS S3, etc.

---

## Usage

### Automatic Model Selection (Default Behavior)

```kotlin
// In ChatViewModel or any model-loading code:
val discovery = ModelDiscovery(context)
val model = discovery.getFirstAvailableModel()

if (model != null) {
    // Model found - use it
    val config = LLMConfig(
        modelPath = model.path,
        modelLib = "${model.id}.ADco"
    )
    loadModel(config)
} else {
    // No models installed
    showInstallationPrompt()
}
```

### List All Installed Models

```kotlin
val discovery = ModelDiscovery(context)
val models = discovery.discoverInstalledModels()

models.forEach { model ->
    println("${model.id} - ${model.getDisplaySize()}")
}

// Example output:
// AVA-GE2-2B16 - 3.8MB
// AVA-GE3-4B16 - 2.13GB
```

### Check Specific Model

```kotlin
val discovery = ModelDiscovery(context)
val hasGemma3 = discovery.isModelInstalled("AVA-GE3-4B16")

if (hasGemma3) {
    val model = discovery.getModelById("AVA-GE3-4B16")
    // Use model
}
```

---

## Installation Methods

### Method 1: Push .ALM Archive (Recommended)

```bash
adb push AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/

# App will:
# 1. Discover the .ALM file
# 2. Auto-extract on first load
# 3. Use the extracted model
```

### Method 2: Push Extracted Directory

```bash
adb push AVA-GE3-4B16/ /sdcard/ava-ai-models/llm/

# App will:
# 1. Discover the extracted directory
# 2. Validate required files present
# 3. Use directly (no extraction needed)
```

### Method 3: Download (Future - Not Yet Implemented)

```kotlin
// This will work once download URLs are configured:
val downloader = ModelDownloader(context)
downloader.downloadModel("AVA-GE3-4B16").collect { progress ->
    showProgress(progress)
}
```

---

## Scan Locations

Models are discovered in these locations (in order):

1. **Primary:** `/sdcard/ava-ai-models/llm/`
2. **External:** `<external-files-dir>/ava-ai-models/llm/`
3. **Legacy:** `<external-files-dir>/models/llm/`
4. **Fallback:** `<files-dir>/models/`

---

## Validation Rules

A directory is considered a valid model if it contains:

### Required Files
- ✅ `AVALibrary.ADco` - TVM runtime library
- ✅ `tokenizer.model` - SentencePiece tokenizer
- ✅ At least one `.ADco` file - Compiled device code

### Optional But Recommended
- `ava-model-config.json` or `mlc-chat-config.json` - Model metadata
- `ndarray-cache.json` - Weight shard mapping
- `tokenizer.json`, `tokenizer_config.json` - Additional tokenizer files
- `params_shard_*.bin` - Model weight files

---

## Error Handling

### No Models Installed

**Logs:**
```
ChatViewModel: No LLM models found on device - LLM responses disabled
ChatViewModel: Install models to /sdcard/ava-ai-models/llm/ to enable LLM
```

**App Behavior:**
- Falls back to template-only responses
- Shows installation instructions in logs
- TODO: Show UI prompt (future enhancement)

### Model Missing Required Files

**Logs:**
```
ModelDiscovery: Invalid model directory (missing required files): AVA-BROKEN-MODEL
```

**App Behavior:**
- Skips invalid directory
- Continues searching for valid models
- Logs warning for debugging

### All Models Fail to Load

**Logs:**
```
ChatViewModel: LLM initialization failed: [error message]
ChatViewModel: Falling back to improved template responses
```

**App Behavior:**
- Uses template-based responses
- Still functional for basic intents
- Complex queries may not work as well

---

## Migration from Old Names

Old cached model names are automatically migrated:

```kotlin
// In getSelectedLLMModel():
val migratedModel = when (storedModel) {
    "AVA-GEM-2B-Q4" -> "AVA-GE2-2B16"  // Auto-fix
    "AVA-GEM-4B-Q4" -> "AVA-GE3-4B16"  // Auto-fix
    else -> storedModel
}
```

**Result:** Users upgrading from old versions automatically get the correct model name!

---

## Future Enhancements

### Phase 1: Model Selection UI
```kotlin
// Show list of discovered models to user
val models = discovery.discoverInstalledModels()
if (models.size > 1) {
    showModelSelectionDialog(models)
}
```

### Phase 2: Download Integration
```kotlin
// Configure download URL (see ModelDownloader.kt TODOs)
val downloader = ModelDownloader(context)
downloader.downloadModel("AVA-GE3-4B16").collect { progress ->
    updateProgressBar(progress)
}
```

### Phase 3: Model Management UI
- View installed models
- Delete unused models
- Download new models
- Switch between models
- See model details (size, languages, etc.)

---

## Testing

### Test Model Discovery

```kotlin
val discovery = ModelDiscovery(context)
val models = discovery.discoverInstalledModels()

models.forEach { model ->
    println("ID: ${model.id}")
    println("Path: ${model.path}")
    println("Size: ${model.getDisplaySize()}")
    println("Type: ${model.type}")
    println("Metadata: ${model.metadata}")
    println("---")
}
```

### Test Auto-Selection

```kotlin
val model = discovery.getFirstAvailableModel()
if (model != null) {
    println("Auto-selected: ${model.id}")
} else {
    println("No models installed")
}
```

### Verify on Device

```bash
# Check what models are installed
adb shell ls -la /sdcard/ava-ai-models/llm/

# Watch discovery logs
adb logcat | grep ModelDiscovery

# Expected logs:
# "Scanning for models in: /sdcard/ava-ai-models/llm"
# "Found installed model: AVA-GE3-4B16 (2130MB)"
# "Discovered 1 installed model(s)"
```

---

## Troubleshooting

### Issue: No Models Discovered

**Check:**
```bash
# Verify files exist on device
adb shell ls -la /sdcard/ava-ai-models/llm/

# Check model directory has required files
adb shell ls -la /sdcard/ava-ai-models/llm/AVA-GE3-4B16/
```

**Required:**
- AVALibrary.ADco
- tokenizer.model
- At least one .ADco file

**Fix:**
```bash
# Re-push model
adb push AVA-GE3-4B16/ /sdcard/ava-ai-models/llm/
```

### Issue: Model Discovered But Won't Load

**Check logs:**
```bash
adb logcat | grep -E "ModelDiscovery|ChatViewModel|LocalLLMProvider"
```

**Common causes:**
- Missing weight files (`params_shard_*.bin`)
- Corrupt .ADco files
- Incompatible TVM version

---

## Benefits

### 1. **No Hardcoded Names**
- Works with any model name following AVA convention
- Flexible for future models
- No code changes needed for new models

### 2. **Automatic Detection**
- Scans storage on app start
- Finds whatever is installed
- No manual configuration needed

### 3. **Graceful Fallbacks**
- No model? Falls back to templates
- Invalid model? Skips and finds next
- Old model name? Auto-migrates

### 4. **Future-Proof**
- Supports .ALM archives (auto-extract)
- Supports extracted directories
- Extensible for download functionality

### 5. **User Control**
- User installs models they want
- App discovers and uses them
- No forced downloads
- Clear installation instructions

---

## Configuration Guide

### For Users: Install Models

**Option 1: Using .ALM Archive**
```bash
adb push AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/
# App auto-extracts on first use
```

**Option 2: Using Extracted Directory**
```bash
adb push AVA-GE3-4B16/ /sdcard/ava-ai-models/llm/
# App uses directly
```

### For Developers: Configure Downloads (Future)

Edit `ModelDownloader.kt` and update:

```kotlin
private const val BASE_DOWNLOAD_URL = "https://your-server.com/models/"
```

Then uncomment the download implementation and remove `NotImplementedError`.

---

## Related Documentation

- [MODEL-FILES-REQUIRED.md](MODEL-FILES-REQUIRED.md) - Required files for each model
- [ALM-AUTO-EXTRACTION.md](ALM-AUTO-EXTRACTION.md) - .ALM archive extraction
- [Developer Manual Chapter 44](Developer-Manual-Chapter44-AVA-Naming-Convention.md) - Naming conventions

---

## API Reference

### ModelDiscovery

```kotlin
class ModelDiscovery(context: Context)

// Discover all installed models
suspend fun discoverInstalledModels(): List<DiscoveredModel>

// Get first available (for auto-selection)
suspend fun getFirstAvailableModel(): DiscoveredModel?

// Check if model installed
suspend fun isModelInstalled(modelId: String): Boolean

// Get specific model
suspend fun getModelById(modelId: String): DiscoveredModel?
```

### DiscoveredModel

```kotlin
data class DiscoveredModel(
    val id: String,                    // "AVA-GE3-4B16"
    val name: String,                  // Display name
    val path: String,                  // Absolute path
    val sizeBytes: Long,               // Size in bytes
    val type: ModelType,               // ARCHIVE or EXTRACTED
    val metadata: Map<String, String>  // From config
)

// Convenience methods
val sizeMB: Float
val sizeGB: Float
fun getDisplaySize(): String  // "2.13GB"
```

### ModelDownloader (Placeholder)

```kotlin
class ModelDownloader(context: Context)

// Download model (NOT YET IMPLEMENTED)
suspend fun downloadModel(modelId: String): Flow<DownloadProgress>

// Check availability (NOT YET IMPLEMENTED)
suspend fun isModelAvailableForDownload(modelId: String): Boolean

// List available (NOT YET IMPLEMENTED)
suspend fun listAvailableModels(): List<ModelInfo>
```

---

## Migration Notes

### Existing Users

Users who previously had a model selected will:
1. See old name auto-migrate (e.g., `AVA-GEM-2B-Q4` → `AVA-GE2-2B16`)
2. If new model not found, discovery kicks in
3. First available model is used
4. Selection is saved for next time

### Fresh Installs

Users installing AVA for the first time:
1. App scans for models
2. If none found, shows installation instructions
3. Falls back to template-only mode
4. Still functional for basic intents

---

## Example Workflows

### Workflow 1: User Installs AVA-GE3-4B16

```bash
# 1. User pushes model
adb push AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/

# 2. Launch app
adb shell am start -n com.augmentalis.ava/.MainActivity

# 3. App automatically:
#    - Scans /sdcard/ava-ai-models/llm/
#    - Finds AVA-GE3-4B16.ALM
#    - Extracts to AVA-GE3-4B16/
#    - Validates required files
#    - Selects AVA-GE3-4B16
#    - Initializes LLM
#    - Saves selection

# 4. User asks: "What's the weather in Delhi?"
#    - LLM generates natural response ✅
```

### Workflow 2: User Has Multiple Models

```bash
# User has both models installed
ls /sdcard/ava-ai-models/llm/
# AVA-GE2-2B16/
# AVA-GE3-4B16/

# App behavior:
# 1. Discovers both models
# 2. Selects AVA-GE3-4B16 (larger = better)
# 3. Uses AVA-GE3-4B16 for responses
# 4. Saves selection

# TODO: Future - show UI to let user choose preferred model
```

### Workflow 3: No Models Installed

```bash
# User has no models
ls /sdcard/ava-ai-models/llm/
# (empty)

# App behavior:
# 1. Scans storage
# 2. No models found
# 3. Logs: "No LLM models found - LLM responses disabled"
# 4. Logs: "Install models to /sdcard/ava-ai-models/llm/"
# 5. Falls back to template responses
# 6. Basic intents still work (Show time, Set alarm, etc.)
```

---

## Summary

✅ **No Hardcoded Model Names** - Discovers what's installed
✅ **Automatic Discovery** - Scans storage on startup
✅ **Graceful Degradation** - Falls back to templates if no models
✅ **Auto-Migration** - Old names automatically updated
✅ **Future-Ready** - Prepared for download functionality
✅ **User Friendly** - Clear instructions when models missing

**Result:** The app adapts to whatever models the user has installed, primarily using AVA-GE3-4B16 when available, without any hardcoded assumptions!

---

**Document Version:** 1.0
**Last Updated:** 2025-11-24
**Author:** AVA Development Team
