# VoiceOS ContentCapture Crash - Migration Guide

**Document Type:** Implementation Guide
**Module:** VoiceOS
**Component:** Compose UI Activities
**Created:** 2025-12-23
**Status:** READY FOR IMPLEMENTATION
**Priority:** HIGH

---

## Quick Start

**Problem:** Activities crash with "scroll observation scope does not exist" during finish.

**Solution:** Migrate to `ContentCaptureSafeComposeActivity` base class.

**Time Required:**
- Immediate hotfix: 10 minutes
- Full migration: 2-3 hours
- Testing: 1 day

---

## Phase 1: Immediate Hotfix (10 minutes)

**Goal:** Stop crashes immediately while proper fix is developed.

### Step 1: Update AndroidManifest.xml

Add `android:contentCaptureEnabled="false"` to crashing activities:

```xml
<!-- File: Modules/VoiceOS/apps/VoiceOSCore/src/main/AndroidManifest.xml -->

<activity
    android:name="com.augmentalis.voiceoscore.ui.LearnAppActivity"
    android:contentCaptureEnabled="false"  ← ADD THIS LINE
    android:exported="false"
    android:label="Learn Apps" />

<activity
    android:name="com.augmentalis.voiceoscore.settings.DeveloperSettingsActivity"
    android:contentCaptureEnabled="false"  ← ADD THIS LINE
    android:exported="false"
    android:label="Developer Settings" />

<activity
    android:name="com.augmentalis.voiceoscore.cleanup.ui.CleanupPreviewActivity"
    android:contentCaptureEnabled="false"  ← ADD THIS LINE
    android:exported="false"
    android:label="Cleanup Preview" />
```

### Step 2: Test Hotfix

```bash
# Build and install
cd /Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS
./gradlew :apps:VoiceOSCore:installDebug

# Test each activity
# 1. Open LearnAppActivity → scroll → back button → should NOT crash
# 2. Open DeveloperSettingsActivity → scroll → back button → should NOT crash
# 3. Open CleanupPreviewActivity → scroll → back button → should NOT crash
```

### Step 3: Document as Temporary

Add comment in manifest:

```xml
<!-- TEMPORARY HOTFIX (2025-12-23): Disable ContentCapture to prevent crash
     TODO: Remove after migrating to ContentCaptureSafeComposeActivity
     See: VoiceOS-ContentCapture-Migration-Guide-251223-V1.md -->
<activity
    android:name="com.augmentalis.voiceoscore.ui.LearnAppActivity"
    android:contentCaptureEnabled="false"
    ...
```

**✅ PHASE 1 COMPLETE: Crashes stopped, but accessibility features degraded.**

---

## Phase 2: Migrate to Safe Base Class (2-3 hours)

**Goal:** Proper fix that prevents crashes WITHOUT disabling ContentCapture.

### Step 1: Verify Base Class Exists

Check that these files were created:

```bash
ls -la Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/ui/ContentCaptureSafeComposeActivity.kt
```

**Expected:** File exists (created as part of fix implementation)

**If missing:** The file is provided in the RoT analysis document appendix.

### Step 2: Migrate LearnAppActivity

**Before:**
```kotlin
// File: LearnAppActivity.kt
class LearnAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentWithScrollSupport {  ← OLD FUNCTION (BROKEN)
            VoiceOSTheme {
                LearnAppScreen(
                    packageManager = packageManager,
                    scrapedAppRepository = null,
                    scrapingIntegration = null
                )
            }
        }
    }
}
```

**After:**
```kotlin
// File: LearnAppActivity.kt
import com.augmentalis.voiceoscore.ui.ContentCaptureSafeComposeActivity  ← ADD IMPORT

class LearnAppActivity : ContentCaptureSafeComposeActivity() {  ← CHANGE BASE CLASS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentSafely {  ← CHANGE FUNCTION NAME
            VoiceOSTheme {
                LearnAppScreen(
                    packageManager = packageManager,
                    scrapedAppRepository = null,
                    scrapingIntegration = null
                )
            }
        }
    }
}
```

**Changes:**
1. Change `ComponentActivity` → `ContentCaptureSafeComposeActivity`
2. Change `setContentWithScrollSupport` → `setContentSafely`
3. Add import for new base class

### Step 3: Migrate DeveloperSettingsActivity

**Before:**
```kotlin
// File: DeveloperSettingsActivity.kt
class DeveloperSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val featureGateManager = FeatureGateManager(applicationContext)
        val subscriptionProvider = DeveloperSubscriptionProvider(applicationContext)

        setContentWithScrollSupport {  ← OLD FUNCTION
            MaterialTheme {
                Surface(/* ... */) {
                    DeveloperSettingsScreen(/* ... */)
                }
            }
        }
    }
}
```

**After:**
```kotlin
// File: DeveloperSettingsActivity.kt
import com.augmentalis.voiceoscore.ui.ContentCaptureSafeComposeActivity  ← ADD IMPORT

class DeveloperSettingsActivity : ContentCaptureSafeComposeActivity() {  ← CHANGE BASE CLASS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val featureGateManager = FeatureGateManager(applicationContext)
        val subscriptionProvider = DeveloperSubscriptionProvider(applicationContext)

        setContentSafely {  ← CHANGE FUNCTION NAME
            MaterialTheme {
                Surface(/* ... */) {
                    DeveloperSettingsScreen(/* ... */)
                }
            }
        }
    }
}
```

### Step 4: Migrate CleanupPreviewActivity

**Before:**
```kotlin
// File: CleanupPreviewActivity.kt
class CleanupPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependencies...
        viewModel = ViewModelProvider(/* ... */)[CleanupPreviewViewModel::class.java]

        setContentWithScrollSupport {  ← OLD FUNCTION
            MaterialTheme {
                Surface(/* ... */) {
                    CleanupPreviewScreen(/* ... */)
                }
            }
        }
    }
}
```

**After:**
```kotlin
// File: CleanupPreviewActivity.kt
import com.augmentalis.voiceoscore.ui.ContentCaptureSafeComposeActivity  ← ADD IMPORT

class CleanupPreviewActivity : ContentCaptureSafeComposeActivity() {  ← CHANGE BASE CLASS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependencies...
        viewModel = ViewModelProvider(/* ... */)[CleanupPreviewViewModel::class.java]

        setContentSafely {  ← CHANGE FUNCTION NAME
            MaterialTheme {
                Surface(/* ... */) {
                    CleanupPreviewScreen(/* ... */)
                }
            }
        }
    }
}
```

### Step 5: Deprecate Old Helper Function

**File:** `ComposeScrollLifecycle.kt`

Add deprecation warning:

```kotlin
/**
 * @deprecated This function does NOT prevent ContentCapture crashes.
 * Use ContentCaptureSafeComposeActivity.setContentSafely() instead.
 *
 * See: VoiceOS-ContentCapture-Migration-Guide-251223-V1.md
 */
@Deprecated(
    message = "Does not prevent ContentCapture crashes. Use ContentCaptureSafeComposeActivity instead.",
    replaceWith = ReplaceWith(
        expression = "setContentSafely(content)",
        imports = ["com.augmentalis.voiceoscore.ui.ContentCaptureSafeComposeActivity"]
    ),
    level = DeprecationLevel.ERROR
)
fun ComponentActivity.setContentWithScrollSupport(content: @Composable () -> Unit) {
    // Keep implementation for now (will be deleted after migration verified)
    setContent {
        ScrollableContent { content() }
    }
}
```

**Note:** After migration is verified, delete this file entirely.

**✅ PHASE 2 COMPLETE: All activities migrated to safe base class.**

---

## Phase 3: Remove Hotfix (30 minutes)

**Goal:** Re-enable ContentCapture and verify proper fix works.

### Step 1: Remove Manifest Flags

```xml
<!-- File: AndroidManifest.xml -->

<!-- BEFORE: -->
<activity
    android:name="com.augmentalis.voiceoscore.ui.LearnAppActivity"
    android:contentCaptureEnabled="false"  ← REMOVE THIS LINE
    android:exported="false" />

<!-- AFTER: -->
<activity
    android:name="com.augmentalis.voiceoscore.ui.LearnAppActivity"
    android:exported="false" />
```

Repeat for all 3 activities.

### Step 2: Verify ContentCapture Works

```bash
# Build and install
./gradlew :apps:VoiceOSCore:installDebug

# Run and check logcat
adb logcat -s ContentCaptureSafe:D ContentCaptureSafeComposeActivity:D

# Expected logs when finishing activity:
# ContentCaptureSafeComposeActivity: ContentCaptureSafeComposeActivity created: LearnAppActivity
# ContentCaptureSafe: ContentCapture disabled for safe disposal (from: finish() override)
# ContentCaptureSafe: ON_STOP event - disabling ContentCapture
# ContentCaptureSafe: ContentCapture already disabled (from: ON_STOP lifecycle event)
```

### Step 3: Test Activity Finish Cycles

**Test Case 1: Normal Finish**
```
1. Open LearnAppActivity
2. Scroll content up and down
3. Press back button
4. Check logcat for "ContentCapture disabled for safe disposal"
5. Verify NO crash
```

**Test Case 2: Rapid Finish**
```
1. Open and close LearnAppActivity 10 times rapidly
2. Check logcat for proper disposal sequence
3. Verify NO crashes, NO memory warnings
```

**Test Case 3: Accessibility Event During Finish**
```
1. Enable VoiceOS AccessibilityService
2. Open DeveloperSettingsActivity
3. Scroll to trigger accessibility events
4. Press back button quickly
5. Verify NO crash (race condition prevented)
```

**✅ PHASE 3 COMPLETE: ContentCapture re-enabled, crashes prevented.**

---

## Phase 4: Prevent Future Issues (2 hours)

**Goal:** Ensure all future Compose activities use safe pattern.

### Step 1: Create Activity Template

**File:** `.idea/fileTemplates/VoiceOS Compose Activity.kt`

```kotlin
#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}

#end
import android.os.Bundle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.augmentalis.voiceoscore.ui.ContentCaptureSafeComposeActivity

/**
 * ${NAME} - ${DESCRIPTION}
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: ${USER}
 * Created: ${DATE}
 */
class ${NAME} : ContentCaptureSafeComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentSafely {
            MaterialTheme {
                Surface {
                    ${NAME}Screen()
                }
            }
        }
    }
}

@Composable
fun ${NAME}Screen() {
    // TODO: Implement screen content
}
```

### Step 2: Add Lint Rule

**File:** `custom-lint-rules/src/main/kotlin/UnsafeComposeActivityDetector.kt`

```kotlin
/**
 * Detects unsafe ComponentActivity usage in VoiceOS.
 *
 * All Compose activities MUST extend ContentCaptureSafeComposeActivity
 * to prevent ContentCapture crashes.
 */
class UnsafeComposeActivityDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUdtTypes(): List<String> {
        return listOf("android.app.Activity", "androidx.activity.ComponentActivity")
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {
        // Check if class extends ComponentActivity directly
        val superClass = declaration.superClass ?: return

        if (superClass.qualifiedName == "androidx.activity.ComponentActivity") {
            // Check if it uses Compose (setContent call)
            val usesCompose = declaration.methods.any { method ->
                method.name == "onCreate" && hasSetContentCall(method)
            }

            if (usesCompose) {
                context.report(
                    issue = UNSAFE_COMPOSE_ACTIVITY,
                    location = context.getLocation(declaration),
                    message = "Compose activity must extend ContentCaptureSafeComposeActivity to prevent crashes. " +
                            "See: VoiceOS-ContentCapture-Migration-Guide-251223-V1.md"
                )
            }
        }
    }

    companion object {
        val UNSAFE_COMPOSE_ACTIVITY = Issue.create(
            id = "UnsafeComposeActivity",
            briefDescription = "Compose activity should extend ContentCaptureSafeComposeActivity",
            explanation = """
                VoiceOS Compose activities crash with "scroll observation scope does not exist"
                if they extend ComponentActivity directly.

                Solution: Extend ContentCaptureSafeComposeActivity instead.
            """,
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                UnsafeComposeActivityDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
```

### Step 3: Update Code Review Checklist

**File:** `.github/PULL_REQUEST_TEMPLATE.md`

Add checklist item:

```markdown
## VoiceOS Compose Activity Checklist

If this PR adds or modifies a Compose activity:

- [ ] Activity extends `ContentCaptureSafeComposeActivity` (NOT `ComponentActivity`)
- [ ] Uses `setContentSafely()` (NOT `setContent()` or `setContentWithScrollSupport()`)
- [ ] Tested activity finish scenario (no crashes)
- [ ] Verified in logcat: "ContentCapture disabled for safe disposal"
- [ ] Tested with accessibility service active (VoiceOSService)
```

### Step 4: Update Architecture Guidelines

**File:** `Docs/VoiceOS-Architecture-Guidelines.md`

Add section:

```markdown
## Compose Activities

### Rule: Always Use ContentCaptureSafeComposeActivity

**Problem:**
VoiceOS AccessibilityService generates WINDOW_STATE_CHANGED events that trigger
ContentCapture to check scroll state during activity finish. This causes crashes
if Compose has already disposed scroll observation scopes.

**Solution:**
ALL Compose activities MUST extend `ContentCaptureSafeComposeActivity` instead of
`ComponentActivity`.

**Example:**
```kotlin
// ✅ CORRECT
class MyActivity : ContentCaptureSafeComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentSafely { /* content */ }
    }
}

// ❌ WRONG - WILL CRASH
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { /* content */ }
    }
}
```

**See:** VoiceOS-ContentCapture-Migration-Guide-251223-V1.md
```

**✅ PHASE 4 COMPLETE: Future issues prevented.**

---

## Verification Checklist

Before marking migration as complete:

### Code Changes
- [ ] All 3 activities migrated to `ContentCaptureSafeComposeActivity`
- [ ] All 3 activities use `setContentSafely()` instead of old function
- [ ] `ComposeScrollLifecycle.kt` deprecated with error level
- [ ] Manifest hotfix flags removed (ContentCapture re-enabled)

### Testing
- [ ] LearnAppActivity finish: no crash
- [ ] DeveloperSettingsActivity finish: no crash
- [ ] CleanupPreviewActivity finish: no crash
- [ ] Rapid finish cycles (10x): no crashes, no memory leaks
- [ ] Accessibility service active: no race condition crashes
- [ ] Logcat shows "ContentCapture disabled for safe disposal"

### Documentation
- [ ] RoT analysis document created
- [ ] Migration guide created (this document)
- [ ] Architecture guidelines updated
- [ ] Code review checklist updated
- [ ] Activity template created

### Prevention
- [ ] Lint rule added to detect unsafe ComponentActivity usage
- [ ] CI/CD runs lint check on every commit
- [ ] Team trained on new pattern

---

## Troubleshooting

### Issue: Still Crashing After Migration

**Symptoms:**
```
java.lang.IllegalStateException: scroll observation scope does not exist
```

**Diagnosis:**
1. Check activity extends `ContentCaptureSafeComposeActivity`:
   ```kotlin
   class MyActivity : ContentCaptureSafeComposeActivity()  // ✅ Correct
   class MyActivity : ComponentActivity()  // ❌ Wrong
   ```

2. Check using `setContentSafely()`:
   ```kotlin
   setContentSafely { /* ... */ }  // ✅ Correct
   setContent { /* ... */ }  // ❌ Wrong
   ```

3. Check logcat for disposal logs:
   ```bash
   adb logcat -s ContentCaptureSafe:D
   ```
   **Expected:** "ContentCapture disabled for safe disposal"
   **If missing:** Base class not being called

4. Check for custom `finish()` override:
   ```kotlin
   override fun finish() {
       // Custom code...
       super.finish()  // ✅ Must call super
   }
   ```

### Issue: ContentCapture Not Working in Other Activities

**Symptoms:**
Auto-fill not working, password managers not detecting fields.

**Diagnosis:**
1. Check manifest - ensure ContentCapture NOT disabled globally:
   ```xml
   <!-- ❌ WRONG - disables for whole app -->
   <application android:contentCaptureEnabled="false">
   ```

2. Check only specific activities have flag (should be REMOVED after migration):
   ```xml
   <!-- ✅ After migration, this should be REMOVED -->
   <activity android:contentCaptureEnabled="false" />
   ```

3. Verify ContentCapture re-enabled in new activity:
   ```kotlin
   val newActivity = startActivity(Intent(this, OtherActivity::class.java))
   // ContentCapture should be enabled in OtherActivity
   ```

### Issue: Memory Leak After Multiple Finish Cycles

**Symptoms:**
Memory usage increases after opening/closing activity multiple times.

**Diagnosis:**
1. Check for leaked observers:
   ```bash
   adb shell dumpsys meminfo com.augmentalis.voiceos
   ```

2. Check LifecycleObserver cleanup:
   ```kotlin
   DisposableEffect(lifecycleOwner) {
       // ...
       onDispose {
           lifecycle.removeObserver(observer)  // ✅ Must clean up
       }
   }
   ```

3. Profile with Android Studio Memory Profiler:
   - Open activity 10 times
   - Force GC
   - Check for retained instances of disposed activities

---

## Rollback Plan

If migration causes issues:

### Step 1: Revert Code Changes
```bash
git checkout HEAD -- \
  Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/ui/LearnAppActivity.kt \
  Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/settings/DeveloperSettingsActivity.kt \
  Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/ui/CleanupPreviewActivity.kt
```

### Step 2: Re-apply Hotfix
```xml
<!-- Re-add to AndroidManifest.xml -->
<activity
    android:name="com.augmentalis.voiceoscore.ui.LearnAppActivity"
    android:contentCaptureEnabled="false"  ← RE-ADD
    ...
```

### Step 3: Document Issues
Create bug report with:
- Exact crash stack trace
- Android version
- Device model
- Reproduction steps
- Logcat logs

---

## Success Criteria

Migration is successful when:

1. ✅ Zero crashes with "scroll observation scope does not exist"
2. ✅ ContentCapture enabled and working (auto-fill, screen readers)
3. ✅ No memory leaks after 20+ activity finish cycles
4. ✅ Accessibility service active without race conditions
5. ✅ All future activities use safe pattern (enforced by lint)

---

## Timeline

| Phase | Duration | Completion Date |
|-------|----------|-----------------|
| Phase 1: Immediate Hotfix | 10 minutes | 2025-12-23 |
| Phase 2: Migration | 2-3 hours | 2025-12-23 |
| Phase 3: Remove Hotfix | 30 minutes | 2025-12-24 |
| Phase 4: Prevention | 2 hours | 2025-12-24 |
| Testing & Verification | 1 day | 2025-12-25 |

**Total Time:** 1-2 days

---

## Questions?

See:
- **Root Cause Analysis:** VoiceOS-ContentCapture-RoT-Analysis-251223-V1.md
- **Base Class Implementation:** ContentCaptureSafeComposeActivity.kt
- **Slack Channel:** #voiceos-development
- **Lead Developer:** Manoj Jhawar

---

**Document Version:** 1.0
**Author:** Claude Code Agent (IDEACODE v12.1)
**Status:** READY FOR IMPLEMENTATION
