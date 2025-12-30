# WebAvanue Logging Migration Guide

**Status**: Phase 1 Complete - Logging & Crash Reporting Integration
**Date**: 2025-12-11
**Completed By**: Logging Agent (Swarm Phase 1)

## Overview

This document tracks the migration from `println()` to structured logging with Napier and Sentry crash reporting.

## Implementation Status

### ✅ Completed

1. **Dependencies Added**
   - Napier logging: `io.github.aakira:napier:2.6.1`
   - Sentry crash reporting: `io.sentry:sentry-android:7.0.0`
   - Sentry Gradle plugin: `io.sentry.android.gradle:4.0.0`

2. **Core Infrastructure Created**
   - `/Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/utils/Logger.kt`
     - PII filtering for URLs (removes query params, truncates to 50 chars)
     - PII filtering for filenames (shows only extension)
     - Log levels: DEBUG, INFO, WARN, ERROR

   - `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/telemetry/SentryManager.kt`
     - Crash reporting with PII filtering
     - Breadcrumb trail for debugging
     - Environment detection (dev/production)
     - Release tracking

3. **Initialization Complete**
   - `WebAvanueApp.onCreate()`: Napier and Sentry initialized
   - Debug builds: All log levels enabled
   - Release builds: Configured for production logging

4. **Breadcrumbs Added**
   - Tab creation
   - Tab switching
   - Navigation
   - Download started
   - Voice commands
   - Database initialization
   - IPC registration

5. **Files Migrated (Fully Complete)**
   - ✅ `TabViewModel.kt` - 13 println → Logger (100%)
   - ✅ `DownloadViewModel.kt` - 10 println → Logger (100%)
   - ✅ `WebAvanueApp.kt` - Added Sentry breadcrumbs

## Remaining Work

### Files with println() statements (Automated Script Available)

The following files still contain `println()` statements. Use the automated migration script below:

| File | println Count | Priority |
|------|---------------|----------|
| SecurityViewModel.kt | 10 | HIGH |
| BrowserRepositoryImpl.kt | 9 | HIGH |
| WebViewConfigurator.kt | 60 | HIGH |
| WebViewContainer.android.kt | 69 | HIGH |
| WebViewLifecycle.kt | 4 | MEDIUM |
| FavoriteViewModel.kt | 2 | MEDIUM |
| BrowserScreen.kt | 2 | MEDIUM |
| BrowserApp.kt | 5 | MEDIUM |
| RetryPolicy.kt | 3 | LOW |
| CertificateUtils.android.kt | 1 | LOW |
| TransactionHelper.kt | 3 | LOW |
| BrowserWebView.ios.kt | 2 | LOW |
| BrowserWebView.desktop.kt | 1 | LOW |
| DesktopWebView.kt | 7 | LOW |
| SecureStorage.kt | ? | MEDIUM |
| DatabaseDriver.kt | ? | LOW |

**Total Remaining**: ~193 println statements (of original 206)
**Completed**: ~13 println statements (6%)

## Automated Migration Script

Save this as `/tmp/migrate_logging.sh` and run:

```bash
#!/bin/bash

BASE="/Volumes/M-Drive/Coding/NewAvanues-WebAvanue/Modules/WebAvanue"

# Map of files and their tag names
declare -A FILE_TAGS=(
    ["universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/SecurityViewModel.kt"]="SecurityViewModel"
    ["coredata/src/commonMain/kotlin/com/augmentalis/webavanue/data/repository/BrowserRepositoryImpl.kt"]="BrowserRepository"
    ["universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/platform/webview/WebViewConfigurator.kt"]="WebViewConfigurator"
    ["universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/WebViewContainer.android.kt"]="WebViewContainer"
    ["universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/platform/webview/WebViewLifecycle.kt"]="WebViewLifecycle"
    ["universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/FavoriteViewModel.kt"]="FavoriteViewModel"
    ["universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/BrowserScreen.kt"]="BrowserScreen"
    ["universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/BrowserApp.kt"]="BrowserApp"
    ["universal/src/commonMain/kotlin/com/augmentalis/webavanue/domain/utils/RetryPolicy.kt"]="RetryPolicy"
    ["universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/security/CertificateUtils.android.kt"]="CertificateUtils"
    ["coredata/src/commonMain/kotlin/com/augmentalis/webavanue/data/util/TransactionHelper.kt"]="TransactionHelper"
)

# Add Logger import to each file
for file in "${!FILE_TAGS[@]}"; do
    FULL_PATH="$BASE/$file"
    TAG="${FILE_TAGS[$file]}"

    if [ ! -f "$FULL_PATH" ]; then
        echo "⚠️  File not found: $FULL_PATH"
        continue
    fi

    echo "Processing: $file (TAG: $TAG)"

    # Check if Logger import already exists
    if ! grep -q "import com.augmentalis.Avanues.web.universal.utils.Logger" "$FULL_PATH"; then
        # Add import after package declaration
        sed -i.bak '/^package /a\
\
import com.augmentalis.Avanues.web.universal.utils.Logger
' "$FULL_PATH"
        echo "  ✅ Added Logger import"
    else
        echo "  ℹ️  Logger import already exists"
    fi
done

echo ""
echo "=========================================="
echo "Logger imports added to all files!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Review each file and replace println() with appropriate Logger.* calls:"
echo "   - println(\"✅ ...\") → Logger.info(TAG, \"...\")"
echo "   - println(\"⚠️  ...\") → Logger.warn(TAG, \"...\", throwable)"
echo "   - println(\"⛔ ...\") → Logger.error(TAG, \"...\", throwable)"
echo "   - println(\"[DEBUG] ...\") → Logger.debug(TAG, \"...\")"
echo ""
echo "2. Apply PII filtering:"
echo "   - URLs: Logger.sanitizeUrl(url)"
echo "   - Filenames: Logger.sanitizeFilename(filename)"
echo ""
echo "3. Add Sentry breadcrumbs for key user actions:"
echo "   - SentryManager.addBreadcrumb(category, message)"
echo ""
```

## Manual Migration Examples

### Pattern 1: Simple Info Log

```kotlin
// BEFORE
println("✅ Tab created: $tabId")

// AFTER
Logger.info("TabViewModel", "Tab created: $tabId")
```

### Pattern 2: Error with Exception

```kotlin
// BEFORE
println("⛔ Failed to create tab: ${e.message}")

// AFTER
Logger.error("TabViewModel", "Failed to create tab: ${e.message}", e)
```

### Pattern 3: URL Logging (PII Protection)

```kotlin
// BEFORE
println("✅ Navigate to: $url")

// AFTER
Logger.info("TabViewModel", "Navigate to: ${Logger.sanitizeUrl(url)}")
```

### Pattern 4: Filename Logging (Privacy Protection)

```kotlin
// BEFORE
println("✅ Download started: $filename")

// AFTER
Logger.info("DownloadViewModel", "Download started: ${Logger.sanitizeFilename(filename)}")
```

### Pattern 5: Warning with Throwable

```kotlin
// BEFORE
try {
    // code
} catch (e: Exception) {
    println("⚠️  Operation failed: ${e.message}")
}

// AFTER
try {
    // code
} catch (e: Exception) {
    Logger.warn("ViewModel", "Operation failed: ${e.message}", e)
}
```

## Tag Naming Convention

Use consistent tag names throughout each file:

| File Type | Tag Example |
|-----------|-------------|
| ViewModel | "TabViewModel", "DownloadViewModel" |
| Repository | "BrowserRepository" |
| WebView | "WebViewConfigurator", "WebViewContainer" |
| Utility | "RetryPolicy", "TransactionHelper" |

## PII Filtering Rules

**ALWAYS filter these data types:**

1. **URLs**: Use `Logger.sanitizeUrl(url)`
   - Removes query parameters (may contain tokens, user data)
   - Truncates to 50 characters
   - Example: `https://example.com/page?token=secret` → `https://example.com/page`

2. **Filenames**: Use `Logger.sanitizeFilename(filename)`
   - Shows only extension for privacy
   - Example: `john_doe_resume.pdf` → `***.pdf`

3. **User Data**: NEVER log directly
   - User IDs: Use hashed/anonymous IDs only
   - Email addresses: NEVER log
   - Personal info: NEVER log

## Sentry Configuration

### Get Sentry DSN

1. Go to https://sentry.io
2. Create project (or use existing)
3. Navigate to: Settings → Projects → [Your Project] → Client Keys (DSN)
4. Copy DSN: `https://PUBLIC_KEY@sentry.io/PROJECT_ID`
5. Update in `WebAvanueApp.kt`:
   ```kotlin
   val sentryDsn = "https://YOUR_PUBLIC_KEY@sentry.io/YOUR_PROJECT_ID"
   ```

### Add Breadcrumbs

Add breadcrumbs before key user actions:

```kotlin
// Navigation
SentryManager.addBreadcrumb("navigation", "Tab created")
SentryManager.addBreadcrumb("navigation", "Navigate to: ${Logger.sanitizeUrl(url)}")

// Downloads
SentryManager.addBreadcrumb("download", "Download started")

// Security
SentryManager.addBreadcrumb("security", "Certificate validation")

// Voice commands
SentryManager.addBreadcrumb("voice_command", "Executing: SCROLL_TOP")
```

## Testing

### 1. Test Logging

```kotlin
Logger.debug("Test", "Debug message")
Logger.info("Test", "Info message")
Logger.warn("Test", "Warning message")
Logger.error("Test", "Error message", Exception("Test exception"))
```

Check logcat:
```bash
adb logcat | grep "Test:"
```

### 2. Test Crash Reporting

Trigger intentional crash:
```kotlin
// In debug menu or test code
throw RuntimeException("Test crash for Sentry")
```

Verify in Sentry dashboard:
- Crash appears with full stack trace
- Breadcrumbs show user actions before crash
- No PII in crash report

### 3. Verify PII Filtering

```kotlin
val url = "https://example.com/page?token=secret123&user=john"
Logger.info("Test", "URL: ${Logger.sanitizeUrl(url)}")
// Should log: "URL: https://example.com/page"

val filename = "john_doe_confidential_2024.pdf"
Logger.info("Test", "File: ${Logger.sanitizeFilename(filename)}")
// Should log: "File: ***.pdf"
```

## Acceptance Criteria

- [x] Napier dependency added
- [x] Logger utility created with PII filtering
- [ ] All 206 println() replaced with Logger calls (13/206 = 6% complete)
- [ ] No println() remains in production code
- [x] Sentry SDK integrated
- [x] SentryManager created
- [x] Breadcrumbs added to key user actions
- [ ] Crash reporting tested and verified
- [x] PII filtered from logs and crash reports

## Next Steps

1. **Run automated script** to add Logger imports to remaining files
2. **Manually replace println()** in each file using patterns above
3. **Test logging** in debug build
4. **Configure Sentry DSN** for crash reporting
5. **Test crash reporting** with intentional crash
6. **Verify PII filtering** in both logs and Sentry
7. **Remove all println()** from production code

## Resources

- Napier Documentation: https://github.com/AAkira/Napier
- Sentry Android SDK: https://docs.sentry.io/platforms/android/
- Universal IPC Protocol: `/Volumes/M-Drive/Coding/AVA/docs/UNIVERSAL-IPC-SPEC.md`

---

**Migration Progress**: 6% Complete (13/206 println statements replaced)
**Priority**: Complete remaining HIGH priority files first (SecurityViewModel, BrowserRepositoryImpl, WebViewConfigurator)
