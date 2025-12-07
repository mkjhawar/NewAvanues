# SpeechRecognition Module Changelog
## November 2025

**Version**: 2.1.0
**Date**: 2025-11-21
**Branch**: kmp/main
**Commit**: 5c8ebf27

---

## 🚀 New Features

### Multi-Location Model Path Resolution

Added universal path resolution for speech recognition models across all offline engines (Vivoka, Whisper, VOSK).

**Key Benefits**:
- 📦 **Smaller APK**: Models don't need to be bundled (saves 50-500MB)
- 🔄 **Manual Pre-deployment**: Deploy models via ADB before app installation
- 💾 **Persistent Storage**: Models survive app uninstall/reinstall  
- 🚀 **Faster Development**: Deploy once, test multiple builds
- 🌐 **Flexible Testing**: Switch model versions without rebuilding

**Path Resolution Priority**:
1. Internal app storage: `/data/data/.../files/{model_dir}/`
2. External app-specific: `/Android/data/.../files/{model_dir}/`
3. **Shared hidden folder (NEW)**: `/.voiceos/models/{engine}/{model_dir}/` ⭐
4. Download/extract if not found

**Engines Updated**:
- ✅ Vivoka: `VivokaInitializer.kt` uses path resolver before asset extraction
- ✅ Whisper: `WhisperModelManager.kt` searches multiple locations before downloading
- ✅ VOSK: `VoskModel.kt` searches multiple locations before extracting
- ℹ️ Android STT: Cloud-based, no local models needed
- ℹ️ Google Cloud STT: Cloud-based, no local models needed

---

## 📝 Changes

### Files Added

**New Files** (1):
- `SpeechModelPathResolver.kt` - Universal path resolver (182 lines)
  - Location: `engines/common/`
  - Provides multi-location search for all engines
  - Includes validation, logging, and helper methods

### Files Modified

**Engine Initializers** (3):
1. **`VivokaInitializer.kt`**
   - Added path resolver before asset extraction
   - Logs all search paths for debugging
   - Falls back to APK extraction if models not found

2. **`WhisperModelManager.kt`**
   - Modified `getModelFile()` to use path resolver
   - Searches all locations before triggering download
   - Detailed logging of search results

3. **`VoskModel.kt`**
   - Modified `calculateModelPath()` to use path resolver
   - Validates model directory structure
   - Returns first valid location found

---

## 🔧 Technical Details

### Implementation

**Architecture**:
```
SpeechModelPathResolver (common component)
├── Used by: VivokaInitializer
├── Used by: WhisperModelManager  
└── Used by: VoskModel
```

**Key Methods**:
- `resolveModelPath(validationFunction?)` - Find model directory with optional validation
- `getSearchPathsForLogging()` - Get human-readable search paths
- `getManualDeploymentPath()` - Get shared hidden folder path
- `getAdbPushCommand(localPath)` - Generate ADB push command for deployment

**Logging Compliance**:
- All logging uses `ConditionalLogger` (VOS4 standards compliant)
- Lambda-based logging for performance
- Includes engine name prefix for easy filtering

### Manual Deployment Commands

**Vivoka**:
```bash
adb push ./vsdk /storage/emulated/0/.voiceos/models/vivoka/vsdk
```

**Whisper**:
```bash
adb push ./whisper_models /storage/emulated/0/.voiceos/models/whisper/whisper_models
```

**VOSK**:
```bash
adb push ./model /storage/emulated/0/.voiceos/models/vosk/model
```

---

## 📚 Documentation Updates

### Developer Manual

Added comprehensive section: **"Model Deployment and Path Resolution"**

**Includes**:
- Architecture overview
- Path resolution priority
- Engine-specific paths and requirements
- Implementation details with code examples
- Manual deployment instructions (ADB, file manager, root)
- Testing and troubleshooting guides
- Performance and security considerations
- Migration guide from v2.0.0

**Location**: `docs/modules/SpeechRecognition/developer-manual.md`

### User Manual

Added user-friendly section: **"Advanced: Manual Model Deployment"**

**Includes**:
- Step-by-step deployment guide for non-technical users
- ADB installation instructions (Windows/Mac/Linux)
- Per-engine deployment examples
- File manager alternative method
- Benefits comparison table
- Troubleshooting common issues
- Model storage locations summary

**Location**: `docs/modules/SpeechRecognition/user-manual.md`

---

## ✅ Testing

### Build Status

- **Gradle Build**: ✅ SUCCESS
- **Compile Time**: 2m 5s
- **Warnings**: 1 (unused parameter in VivokaPathResolver - non-critical)
- **Errors**: 0
- **Logging Standards**: ✅ PASSED

### Verification

- ✅ SpeechRecognition module builds successfully
- ✅ All engines compile without errors
- ✅ ConditionalLogger compliance verified via pre-commit hook
- ⏳ Device testing required (models must be pre-deployed for full test)

---

## 🔄 Backward Compatibility

**Fully Backward Compatible**: ✅

- Internal app storage (default) still works as before
- No breaking changes to public APIs
- Existing apps continue to function normally
- Models in internal storage are still found first
- New feature is opt-in (manual deployment is optional)

---

## 🎯 Future Enhancements

Planned for future versions:

1. **Cloud Sync**: Auto-sync models across devices
2. **Model Versioning**: Auto-update when new versions available
3. **Compression**: On-the-fly decompression for smaller storage
4. **Differential Updates**: Only download changed files
5. **Model Marketplace**: Browse/install community models

---

## 👥 Contributors

- **Implementation**: Claude Code (AI Assistant)
- **Review**: Manoj Jhawar
- **Testing**: Pending device testing
- **Documentation**: Claude Code

---

## 🔗 Related

- **Commit**: `5c8ebf27` on `kmp/main`
- **Pull Request**: N/A (direct push to kmp/main)
- **Issues**: Resolves model deployment flexibility requirement
- **Previous Version**: v2.0.0 (SOLID Refactored)

---

**Changelog Version**: 1.0
**Generated**: 2025-11-21
**Format**: Markdown
**Review Status**: Complete
