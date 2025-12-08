# Vivoka Model Deployment - Quick Reference Card

**Project:** VoiceOS (VOS4)
**Feature:** External Model Deployment with Multi-Location Fallback
**Created:** 2025-11-20 01:23
**Author:** VOS4 Development Team
**Audience:** Developers, QA, DevOps

---

## TL;DR

**Deploy models to shared folder once, test unlimited APK builds:**
```bash
adb push ./vsdk /storage/emulated/0/.voiceos/vivoka/vsdk/
```

**Benefits:**
- ✅ Survives app uninstall
- ✅ 100MB smaller APK
- ✅ Instant testing without model redeployment
- ✅ Shared across all VoiceOS installations

---

## Three Deployment Options

| Option | When to Use | APK Size | Network | Survives Uninstall |
|--------|-------------|----------|---------|-------------------|
| 🎯 **External Folder** | Development, QA | 50MB | No | ✅ Yes (shared) |
| 📦 **APK Bundle** | Offline demos | 150-250MB | No | N/A (in APK) |
| ☁️ **On-Demand** | Production | 50MB | Yes (first use) | No |

---

## Option 1: External Folder (RECOMMENDED for Dev/QA)

### Setup Once:
```bash
# Push models to shared hidden folder
adb push ./vsdk /storage/emulated/0/.voiceos/vivoka/vsdk/

# Verify deployment
adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/config/
adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/data/csdk/asr/acmod/

# Check model size (should be ~100-200MB)
adb shell "du -sh /storage/emulated/0/.voiceos/vivoka/vsdk/"
```

### Benefits:
✅ Models survive app uninstall (shared folder)
✅ No need to rebuild/redeploy models for each APK
✅ 100MB smaller APK size
✅ Instant language switching without rebuilds
✅ Faster development iteration cycles

### Build Configuration:
```gradle
// app/build.gradle.kts
android {
    buildTypes {
        debug {
            // No bundled models in debug builds
            sourceSets {
                main {
                    assets.srcDirs = []  // Exclude all assets
                }
            }
        }
    }
}
```

---

## Option 2: APK Bundle (Offline Demos)

### When to Use:
- Offline demonstrations
- Single-language deployments
- No network available
- APK size not a concern

### Build Config:
```gradle
// app/build.gradle.kts
android {
    buildTypes {
        release {
            sourceSets {
                main {
                    assets.srcDirs = ['src/main/assets']
                }
            }
        }
    }
}
```

### Required Structure:
```
app/src/main/assets/vsdk/
├── config/
│   └── vsdk.json
└── data/
    └── csdk/
        └── asr/
            ├── acmod/
            ├── clc/
            ├── ctx/
            └── lm/
```

**APK Size Impact:** +100-200MB per language

---

## Option 3: On-Demand Download (Production)

### When to Use:
- Production releases
- Multi-language support
- Smaller APK distribution
- Users have network access

### Build Config:
```gradle
// app/build.gradle.kts
android {
    buildTypes {
        release {
            // No bundled models - pure on-demand download
            sourceSets {
                main {
                    assets.srcDirs = []
                }
            }
        }
    }
}
```

### Firebase Remote Config:
```json
{
  "es_json": "https://www.augmentalis.com/avanuevoiceosava/Spanish/spanish.json",
  "es_voice_resource": "https://www.augmentalis.com/avanuevoiceosava/Spanish/es_voice_resource.zip",
  "fr_json": "https://www.augmentalis.com/avanuevoiceosava/French/french.json",
  "fr_voice_resource": "https://www.augmentalis.com/avanuevoiceosava/French/fr_voice_resource.zip"
}
```

**Download Time:** ~60 seconds per language over Wi-Fi

---

## Path Resolution Order (Priority)

The app checks locations in this order:

```
1. Internal App:  /data/data/com.augmentalis.voiceos/files/vsdk/
   └─ Default location, automatic download target

2. External App:  /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk/
   └─ Accessible via file manager, deleted on uninstall

3. Shared Folder: /storage/emulated/0/.voiceos/vivoka/vsdk/  ⭐ RECOMMENDED
   └─ Hidden folder, survives uninstall, shared across installations

4. Download:      Firebase Remote Config → Internal App
   └─ Fallback if no models found in 1-3
```

**First valid location wins.** No download triggered if models found in any location.

---

## Required Directory Structure

**VSDK Validation Requirements:**

```
vsdk/
├── config/
│   └── vsdk.json           ← REQUIRED (app fails without this)
└── data/
    └── csdk/
        └── asr/
            ├── acmod/      ← REQUIRED (acoustic models, non-empty)
            ├── ctx/        ← REQUIRED (context files, non-empty)
            ├── clc/        ← Optional (language components for dynamic grammar)
            └── lm/         ← Optional (language models for free speech)
```

**Validation Checks:**
- ✅ `vsdk.json` must exist in `config/` directory
- ✅ `acmod/` directory must exist and contain files
- ✅ `ctx/` directory must exist and contain files

---

## Verification Commands

### Check All Locations:
```bash
# Internal app storage
adb shell run-as com.augmentalis.voiceos ls -la /data/data/com.augmentalis.voiceos/files/vsdk/

# External app-specific storage
adb shell ls -la /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk/

# Shared hidden folder (RECOMMENDED)
adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/
```

### Check Logs:
```bash
# Monitor path resolution
adb logcat -s VivokaPathResolver:* VivokaConfig:* VivokaInitializer:*

# Clear app data to force re-initialization
adb shell pm clear com.augmentalis.voiceos

# Launch app and watch logs
adb shell am start -n com.augmentalis.voiceos/.MainActivity
```

### Expected Log Output:
```
VivokaPathResolver: Searching for VSDK in locations:
  - /data/data/com.augmentalis.voiceos/files/vsdk
  - /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk
  - /storage/emulated/0/.voiceos/vivoka/vsdk
VivokaPathResolver: Found valid VSDK directory at: /storage/emulated/0/.voiceos/vivoka/vsdk
VivokaConfig: Using EXTERNAL VSDK location (pre-deployed or fallback)
VivokaConfig: Asset paths configured - assets: /storage/emulated/0/.voiceos/vivoka/vsdk
VivokaConfig: Vivoka configuration initialized successfully
```

---

## Common Issues & Solutions

### Issue: Models Not Found
**Symptoms:** App downloads models despite external deployment

**Check:**
```bash
# Verify vsdk.json exists
adb shell cat /storage/emulated/0/.voiceos/vivoka/vsdk/config/vsdk.json

# Verify acmod has files
adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/data/csdk/asr/acmod/

# Check ctx has files
adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/data/csdk/asr/ctx/
```

**Solution:**
- Ensure `config/vsdk.json` exists (not in root, must be in `config/` subdirectory)
- Ensure `acmod/` and `ctx/` directories are non-empty
- Re-push models if structure incorrect

### Issue: App Still Downloads Models
**Symptoms:** External models ignored, Firebase download triggered

**Check Logs:**
```bash
adb logcat -s VivokaPathResolver:D VivokaConfig:D
```

**Common Causes:**
- Missing `vsdk.json` in `config/` subdirectory
- Empty `acmod/` or `ctx/` directories
- Incorrect directory structure (models in wrong location)
- Permission issues (rare on `/storage/emulated/0/`)

**Solution:**
- Verify exact directory structure matches requirements above
- Check log output for validation errors
- Re-push models to correct location

### Issue: Models Disappeared After App Update
**Symptoms:** Voice recognition stopped working after update

**Cause:** Models were in internal storage, reset during update

**Solution:**
- Use shared folder (`/storage/emulated/0/.voiceos/vivoka/`) to preserve models
- Re-download models (automatic)
- For development: Always use shared folder

---

## CI/CD Integration Examples

### Jenkinsfile (Jenkins):
```groovy
stage('Deploy Test Models') {
    steps {
        sh '''
            # Push models to shared folder
            adb push ./test-models/vsdk /storage/emulated/0/.voiceos/vivoka/vsdk/

            # Verify deployment
            adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/config/vsdk.json

            # Install APK
            adb install -r app-debug.apk

            # Clear app data to force initialization
            adb shell pm clear com.augmentalis.voiceos
        '''
    }
}
```

### GitLab CI (.gitlab-ci.yml):
```yaml
deploy_models:
  stage: test
  script:
    - adb push ./test-models/vsdk /storage/emulated/0/.voiceos/vivoka/vsdk/
    - adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/config/
    - adb install -r app-debug.apk
  only:
    - develop
    - feature/*
```

### GitHub Actions (.github/workflows/android.yml):
```yaml
- name: Deploy Vivoka Models
  run: |
    adb push ./test-models/vsdk /storage/emulated/0/.voiceos/vivoka/vsdk/
    adb shell "du -sh /storage/emulated/0/.voiceos/vivoka/vsdk/"
    adb install -r app-debug.apk
```

---

## Developer Workflow Examples

### Workflow 1: Development with External Models (RECOMMENDED)

```bash
# One-time setup (models persist)
adb push ./vsdk /storage/emulated/0/.voiceos/vivoka/vsdk/

# Daily development loop
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Models already on device, no redeployment needed!
```

**Benefits:**
- ⚡ Faster build times (no model bundling)
- ⚡ Faster install times (50MB vs 150MB APK)
- ⚡ One-time model deployment

### Workflow 2: Testing Multiple Languages

```bash
# Deploy all language models once
adb push ./vsdk_multilang /storage/emulated/0/.voiceos/vivoka/vsdk/

# Switch languages in app settings
# No rebuilds, no redeployments!
```

### Workflow 3: Production Release (On-Demand)

```bash
# Build release with no models
./gradlew bundleRelease

# Upload to Play Store (50MB vs 250MB)
# Users download models on first launch
```

---

## Performance Metrics

| Metric | External Folder | APK Bundle | On-Demand |
|--------|----------------|------------|-----------|
| **APK Size** | ~50MB | ~150-250MB | ~50MB |
| **Build Time** | Faster (no assets) | Slower (bundle assets) | Faster (no assets) |
| **Install Time** | Faster (smaller APK) | Slower (larger APK) | Faster (smaller APK) |
| **First Launch** | Instant | Instant | 30-60s (download) |
| **Network Required** | No | No | Yes (first use) |
| **Survives Uninstall** | Yes (shared folder) | N/A (in APK) | No |

---

## Key Takeaways

✅ **External Folder Deployment:**
- Best for development and QA
- One-time setup, survives uninstalls
- 100MB smaller APK
- Instant language switching

✅ **APK Bundle:**
- Best for offline demos
- No setup required
- Large APK size (150-250MB)

✅ **On-Demand Download:**
- Best for production releases
- Smallest APK size (50MB)
- Requires network on first use
- Automatic language management

✅ **Multi-Location Fallback:**
- App checks 3 locations before downloading
- First valid location wins
- No unnecessary downloads
- Developer flexibility

---

## Related Documentation

**Developer Manual:**
- **Chapter 7.5.1:** Vivoka Model Deployment (implementation details)
- **Chapter 35.4:** Vivoka Model Deployment (deployment strategies)

**Active Documentation:**
- `Vivoka-Model-File-Paths-251120.md` - Technical reference with device paths

**User Manual:**
- **Appendix C:** Managing Voice Recognition Models (end-user guide)

**Code References:**
- `VivokaPathResolver.kt:48` - Multi-location resolution logic
- `VivokaConfig.kt:87` - Path setup and validation
- `VivokaInitializer.kt` - Download and extraction logic

---

**Quick Reference Version:** 1.0
**Last Updated:** 2025-11-20 01:23
**Status:** Production Ready
**Maintained By:** VOS4 Development Team
