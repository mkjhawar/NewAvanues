# Phase 2: Integration & Implementation Guide

**Status:** Ready to implement
**Prerequisites:** Phase 1 foundation committed (commit 4d686ad)
**Estimated Effort:** 9.5 hours
**Files to Modify:** 2 files (~500 LOC)

---

## Overview

Phase 2 integrates the security foundation from Phase 1 with the WebView and Repository layers. This eliminates the 5 critical security vulnerabilities and adds database transaction support.

---

## Task 1: Implement `onReceivedSslError()` (2 hours)

**File:** `common/libs/webavanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/WebViewContainer.android.kt`

**Current:** Method not implemented (line 287 - add after `shouldOverrideUrlLoading`)
**Vulnerability:** CWE-295 - Invalid certificates proceed silently
**Impact:** CRITICAL - Man-in-the-middle attacks possible

### Implementation

```kotlin
// Add to WebViewClient (after line 286)
override fun onReceivedSslError(
    view: WebView?,
    handler: SslErrorHandler?,
    error: SslError?
) {
    // Convert Android SslError to our SecurityState
    if (error == null || handler == null) {
        handler?.cancel()
        return
    }

    val sslErrorInfo = CertificateUtils.convertSslError(error)

    // Show SSL error dialog (requires composable state)
    // OPTION 1: Via ViewModel state
    // viewModel.showSslErrorDialog(sslErrorInfo, handler)

    // OPTION 2: Via callback parameter
    // onSslError(sslErrorInfo) { proceed ->
    //     if (proceed) handler.proceed()
    //     else handler.cancel()
    // }

    // For now: REJECT all invalid certificates (secure default)
    handler.cancel()

    // TODO: Integrate with SslErrorDialog from SecurityDialogs.kt
    // TODO: Update SecurityState in ViewModel
    println("SSL Error: ${sslErrorInfo.primaryError} on ${sslErrorInfo.url}")
}
```

### Integration Requirements

1. **Add callback parameter to WebViewContainer:**
   ```kotlin
   actual fun WebViewContainer(
       // ... existing parameters
       onSslError: (SslErrorInfo, (Boolean) -> Unit) -> Unit,  // NEW
       modifier: Modifier
   )
   ```

2. **ViewModel state for SSL dialog:**
   ```kotlin
   data class BrowserState(
       // ... existing state
       val sslErrorInfo: SslErrorInfo? = null,  // NEW
       val sslErrorHandler: ((Boolean) -> Unit)? = null  // NEW
   )
   ```

3. **Show dialog in BrowserScreen:**
   ```kotlin
   // In BrowserScreen.kt
   state.sslErrorInfo?.let { errorInfo ->
       SslErrorDialog(
           sslErrorInfo = errorInfo,
           onGoBack = {
               state.sslErrorHandler?.invoke(false)
               viewModel.dismissSslError()
           },
           onProceedAnyway = {
               state.sslErrorHandler?.invoke(true)
               viewModel.dismissSslError()
           },
           onDismiss = {
               state.sslErrorHandler?.invoke(false)
               viewModel.dismissSslError()
           }
       )
   }
   ```

### Testing

- [ ] Test expired certificate (https://expired.badssl.com)
- [ ] Test self-signed certificate (https://self-signed.badssl.com)
- [ ] Test hostname mismatch (https://wrong.host.badssl.com)
- [ ] Test untrusted CA (https://untrusted-root.badssl.com)
- [ ] Verify dialog shows correct error details
- [ ] Verify "Go Back" cancels navigation
- [ ] Verify "Proceed Anyway" continues (if implemented)

---

## Task 2: Implement `onPermissionRequest()` (2 hours)

**File:** `WebViewContainer.android.kt`
**Current:** Lines 316-333 - Auto-grants all permissions
**Vulnerability:** CWE-276 - Privacy violation, Google Play policy violation
**Impact:** CRITICAL - App store rejection risk

### Implementation

```kotlin
// REPLACE lines 316-333 with:
override fun onPermissionRequest(request: PermissionRequest?) {
    if (request == null) return

    // Extract domain from request origin
    val domain = request.origin?.host ?: "Unknown"

    // Convert Android permission resources to our PermissionType
    val permissions = request.resources.mapNotNull { resource ->
        PermissionType.fromResourceString(resource)
    }

    if (permissions.isEmpty()) {
        // Unknown permission type - deny for security
        request.deny()
        return
    }

    // Check if permission already granted for this domain
    // TODO: Query site_permission table via Repository
    // val existingPermission = repository.getSitePermission(domain, permissions[0])
    // if (existingPermission?.granted == true) {
    //     request.grant(request.resources)
    //     return
    // }

    // Show permission request dialog
    val permissionRequest = PermissionRequest(
        domain = domain,
        permissions = permissions,
        requestId = UUID.randomUUID().toString()
    )

    // OPTION 1: Via ViewModel state
    // viewModel.showPermissionDialog(permissionRequest, request)

    // OPTION 2: Via callback
    // onPermissionRequested(permissionRequest) { granted, remember ->
    //     if (granted) {
    //         if (remember) {
    //             // Save to database
    //             repository.insertSitePermission(domain, permissions[0], granted = true)
    //         }
    //         request.grant(request.resources)
    //     } else {
    //         request.deny()
    //     }
    // }

    // For now: DENY all permissions (secure default)
    request.deny()
    println("Permission request from $domain: ${permissions.map { it.getUserFriendlyName() }}")
}
```

### Integration Requirements

1. **Add Repository dependency:**
   ```kotlin
   // WebViewContainer needs access to repository for permission storage
   actual fun WebViewContainer(
       // ... existing parameters
       repository: BrowserRepository,  // NEW
       modifier: Modifier
   )
   ```

2. **ViewModel state for permission dialog:**
   ```kotlin
   data class BrowserState(
       // ... existing state
       val permissionRequest: PermissionRequest? = null,  // NEW
       val permissionHandler: ((Boolean, Boolean) -> Unit)? = null  // NEW (granted, remember)
   )
   ```

3. **Show dialog in BrowserScreen:**
   ```kotlin
   state.permissionRequest?.let { permRequest ->
       PermissionRequestDialog(
           permissionRequest = permRequest,
           onAllow = { remember ->
               state.permissionHandler?.invoke(true, remember)
               viewModel.dismissPermissionDialog()
           },
           onDeny = {
               state.permissionHandler?.invoke(false, false)
               viewModel.dismissPermissionDialog()
           },
           onDismiss = {
               state.permissionHandler?.invoke(false, false)
               viewModel.dismissPermissionDialog()
           }
       )
   }
   ```

4. **Repository method for permission storage:**
   ```kotlin
   // In BrowserRepository interface
   suspend fun insertSitePermission(
       domain: String,
       permissionType: PermissionType,
       granted: Boolean
   )

   suspend fun getSitePermission(
       domain: String,
       permissionType: PermissionType
   ): SitePermissionEntity?

   // In BrowserRepositoryImpl
   override suspend fun insertSitePermission(
       domain: String,
       permissionType: PermissionType,
       granted: Boolean
   ) {
       queries.insertSitePermission(
           domain = domain,
           permission_type = permissionType.resourceString,
           granted = if (granted) 1 else 0,
           timestamp = System.currentTimeMillis()
       )
   }
   ```

### Testing

- [ ] Test camera permission request
- [ ] Test microphone permission request
- [ ] Test location permission request
- [ ] Test multiple permissions at once
- [ ] Test "Remember my choice" checkbox
- [ ] Verify permission stored in database
- [ ] Verify permission persists across sessions
- [ ] Test permission revocation from Settings

---

## Task 3: Implement JavaScript Dialog Callbacks (2 hours)

**File:** `WebViewContainer.android.kt`
**Current:**
- Line 301-311: `onJsAlert()` auto-confirms
- Missing: `onJsConfirm()`, `onJsPrompt()`
**Vulnerability:** CWE-1021 - Broken UX, phishing risk
**Impact:** HIGH - Websites broken, user trust compromised

### Implementation

```kotlin
// REPLACE onJsAlert (lines 301-311):
override fun onJsAlert(
    view: WebView?,
    url: String?,
    message: String?,
    result: JsResult?
) {
    if (result == null || message == null) {
        result?.cancel()
        return
    }

    val domain = url?.let { Uri.parse(it).host } ?: "Unknown"

    // Show JavaScript alert dialog
    // OPTION 1: Via ViewModel state
    // viewModel.showJsAlertDialog(domain, message, result)

    // OPTION 2: Via callback
    // onJsAlert(domain, message) {
    //     result.confirm()
    // }

    // For now: Auto-confirm (same as before, but logged)
    println("JS Alert from $domain: $message")
    result.confirm()
    return true
}

// ADD onJsConfirm (after onJsAlert):
override fun onJsConfirm(
    view: WebView?,
    url: String?,
    message: String?,
    result: JsResult?
): Boolean {
    if (result == null || message == null) {
        result?.cancel()
        return true
    }

    val domain = url?.let { Uri.parse(it).host } ?: "Unknown"

    // Show JavaScript confirm dialog
    // onJsConfirm(domain, message) { confirmed ->
    //     if (confirmed) result.confirm()
    //     else result.cancel()
    // }

    // For now: Auto-cancel (conservative default)
    println("JS Confirm from $domain: $message")
    result.cancel()
    return true
}

// ADD onJsPrompt (after onJsConfirm):
override fun onJsPrompt(
    view: WebView?,
    url: String?,
    message: String?,
    defaultValue: String?,
    result: JsPromptResult?
): Boolean {
    if (result == null || message == null) {
        result?.cancel()
        return true
    }

    val domain = url?.let { Uri.parse(it).host } ?: "Unknown"

    // Show JavaScript prompt dialog
    // onJsPrompt(domain, message, defaultValue ?: "") { input ->
    //     if (input != null) result.confirm(input)
    //     else result.cancel()
    // }

    // For now: Auto-cancel (conservative default)
    println("JS Prompt from $domain: $message")
    result.cancel()
    return true
}
```

### Dialog Spam Prevention

```kotlin
// Add to WebViewContainer class level
private val jsDialogTimestamps = mutableListOf<Long>()
private val MAX_DIALOGS_PER_WINDOW = 3
private val DIALOG_WINDOW_MS = 10_000L  // 10 seconds

private fun shouldBlockDialog(): Boolean {
    val now = System.currentTimeMillis()

    // Remove timestamps older than 10 seconds
    jsDialogTimestamps.removeAll { it < now - DIALOG_WINDOW_MS }

    // Check if limit exceeded
    if (jsDialogTimestamps.size >= MAX_DIALOGS_PER_WINDOW) {
        println("⚠️  JS Dialog spam detected - blocking")
        return true
    }

    // Record this dialog
    jsDialogTimestamps.add(now)
    return false
}

// Use in each dialog callback:
if (shouldBlockDialog()) {
    result?.cancel()
    return true
}
```

### Testing

- [ ] Test `window.alert("message")`
- [ ] Test `window.confirm("question")`
- [ ] Test `window.prompt("question", "default")`
- [ ] Test dialog with long message (>1000 chars)
- [ ] Test dialog spam (>3 in 10 seconds blocked)
- [ ] Test beforeunload confirmation
- [ ] Verify domain shown in dialog title

---

## Task 4: Change Mixed Content Mode (0.5 hours)

**File:** `WebViewContainer.android.kt`
**Current:** Line 241 - `MIXED_CONTENT_COMPATIBILITY_MODE`
**Vulnerability:** CWE-319 - Cleartext transmission on HTTPS pages
**Impact:** HIGH - Security weakness, downgrade attacks

### Implementation

```kotlin
// CHANGE line 241 from:
mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

// TO:
mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
```

### Optional: Settings Override

```kotlin
// Add to browser_settings table (if not exists):
// enable_mixed_content INTEGER NOT NULL DEFAULT 0

// In WebSettings configuration:
mixedContentMode = if (settings.enableMixedContent) {
    WebSettings.MIXED_CONTENT_ALWAYS_ALLOW  // User override (advanced)
} else {
    WebSettings.MIXED_CONTENT_NEVER_ALLOW  // Secure default
}
```

### Testing

- [ ] Test HTTPS page with HTTP images (verify blocked)
- [ ] Test HTTPS page with HTTP scripts (verify blocked)
- [ ] Test HTTPS page with HTTP iframes (verify blocked)
- [ ] Verify error console shows blocked resources
- [ ] Test Settings override (if implemented)

---

## Task 5: Repository Transaction Refactoring (3 hours)

**File:** `common/libs/webavanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/data/repository/BrowserRepositoryImpl.kt`
**Current:** 11 operations with no transactions
**Vulnerability:** Data corruption from partial writes
**Impact:** CRITICAL - User data loss

### Operations to Wrap

**Priority 1 (Most Critical):**
1. `setActiveTab()` - Lines 139-148
2. `reorderTabs()` - Lines 177-204
3. `importData()` - Lines 563-577

**Priority 2 (Important):**
4. `closeTabs()` - Batch delete
5. `clearAllTabs()` - Delete all
6. `removeFavorites()` - Batch delete
7. `clearHistoryByTimeRange()` - Batch delete by date
8. `clearAllHistory()` - Delete all history

**Priority 3 (Nice to Have):**
9. `addFavoriteWithFolder()` - Insert favorite + create folder
10. `updateBrowserSettings()` - Multiple setting updates
11. Tab group operations - Create group + assign tabs

### Example Refactor: `setActiveTab()`

**BEFORE (vulnerable):**
```kotlin
suspend fun setActiveTab(tabId: String) {
    queries.clearActiveTab()  // Step 1
    // ← APP COULD CRASH HERE
    queries.setTabActive(tabId, 1)  // Step 2
    // Result: NO ACTIVE TAB (corrupted state)
}
```

**AFTER (ACID guaranteed):**
```kotlin
suspend fun setActiveTab(tabId: String): Result<Unit> {
    return TransactionHelper.transaction(database.driver) {
        queries.clearActiveTab()
        queries.setTabActive(tabId, 1)
        // Either BOTH succeed or BOTH rollback
    }
}
```

### Example Refactor: `reorderTabs()`

**BEFORE (N+1 queries, vulnerable):**
```kotlin
suspend fun reorderTabs(tabIds: List<String>) {
    tabIds.forEachIndexed { index, tabId ->
        // Query 1: SELECT position
        val currentPosition = queries.selectTabById(tabId).executeAsOne().position
        // Query 2: UPDATE position
        queries.updateTabPosition(tabId, index)
        // 200 queries for 100 tabs! (100 SELECT + 100 UPDATE)
    }
}
```

**AFTER (batched, transactional):**
```kotlin
suspend fun reorderTabs(tabIds: List<String>): Result<Unit> {
    return TransactionHelper.transaction(database.driver) {
        tabIds.forEachIndexed { index, tabId ->
            queries.updateTabPosition(tabId, index)
            // 100 queries for 100 tabs (all UPDATE, no SELECT needed)
            // All atomic - either ALL succeed or ALL rollback
        }
    }
}
```

### Testing

- [ ] Unit test: Transaction commits successfully
- [ ] Unit test: Transaction rolls back on error
- [ ] Unit test: Simulated crash mid-operation (rollback verified)
- [ ] Integration test: `setActiveTab()` with crash → no corrupted state
- [ ] Integration test: `reorderTabs()` with 100 tabs → atomicity verified
- [ ] Performance test: Transaction overhead <5ms
- [ ] Stress test: 1000+ operations without corruption

---

## Integration Checklist

### WebView Integration

- [ ] Add `onReceivedSslError()` to WebViewClient
- [ ] Replace `onPermissionRequest()` with secure implementation
- [ ] Replace `onJsAlert()` with dialog integration
- [ ] Add `onJsConfirm()` implementation
- [ ] Add `onJsPrompt()` implementation
- [ ] Change `mixedContentMode` to NEVER_ALLOW
- [ ] Add dialog spam prevention
- [ ] Add SecurityState to BrowserViewModel
- [ ] Show SecurityDialogs in BrowserScreen
- [ ] Add permission storage to Repository

### Repository Integration

- [ ] Import TransactionHelper
- [ ] Wrap `setActiveTab()` in transaction
- [ ] Wrap `reorderTabs()` in transaction
- [ ] Wrap `importData()` in transaction
- [ ] Wrap all 11 operations in transactions
- [ ] Add permission queries to Repository interface
- [ ] Implement permission queries in RepositoryImpl
- [ ] Add error handling for transaction failures

### Testing Integration

- [ ] Write unit tests for each WebView callback
- [ ] Write unit tests for each transaction wrapper
- [ ] Write integration tests for SSL error flow
- [ ] Write integration tests for permission flow
- [ ] Write integration tests for JS dialog flow
- [ ] Write stress tests for transaction atomicity
- [ ] Manual test on 3+ real devices
- [ ] Performance profile with Android Profiler

---

## Build & Test

### Build

```bash
# Build universal module
./gradlew :common:libs:webavanue:universal:build

# Build coredata module (transactions)
./gradlew :common:libs:webavanue:coredata:build

# Build Android app
./gradlew :android:apps:webavanue:assembleDebug
```

### Run Tests

```bash
# Unit tests
./gradlew :common:libs:webavanue:universal:testDebugUnitTest
./gradlew :common:libs:webavanue:coredata:testDebugUnitTest

# Android instrumented tests
./gradlew :common:libs:webavanue:universal:connectedDebugAndroidTest
```

### Install & Test

```bash
# Install on emulator
adb install -r android/apps/webavanue/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell monkey -p com.augmentalis.Avanues.web.debug -c android.intent.category.LAUNCHER 1

# Test SSL error
# Navigate to: https://expired.badssl.com
# Expected: SSL error dialog appears

# Test permissions
# Navigate to: https://tests.caniuse.com/
# Click "Test Camera" button
# Expected: Permission dialog appears
```

---

## Commit Strategy

### Commit 1: WebView Security Callbacks
```bash
git add WebViewContainer.android.kt
git commit -m "feat(security): Implement WebView security callbacks (SSL, Permissions, JS dialogs)

- Add onReceivedSslError() with SslErrorDialog integration
- Replace auto-grant onPermissionRequest() with permission dialog
- Implement onJsAlert(), onJsConfirm(), onJsPrompt() with dialogs
- Change mixedContentMode to NEVER_ALLOW
- Add dialog spam prevention (max 3 per 10 seconds)

Vulnerabilities Fixed:
- CWE-295: SSL certificate validation implemented
- CWE-276: Permission requests now require user consent
- CWE-319: Mixed content blocked on HTTPS pages
- CWE-1021: JavaScript dialogs shown properly

Testing: Manual testing on emulator
Phase: 2/3 (Integration - WebView complete)
"
```

### Commit 2: Repository Transactions
```bash
git add BrowserRepositoryImpl.kt
git commit -m "feat(database): Add transaction support to repository operations

- Wrap 11 multi-query operations in ACID transactions
- Prevent data corruption from partial writes
- Add error handling and rollback logic

Operations wrapped:
- setActiveTab(), reorderTabs(), closeTabs(), clearAllTabs()
- importData(), removeFavorites(), clearHistory*()
- addFavoriteWithFolder(), updateBrowserSettings()
- Tab group operations

Testing: Unit tests + integration tests
Performance: <5ms overhead per transaction
Phase: 2/3 (Integration - Database complete)
"
```

---

## Next Steps After Phase 2

1. **Phase 3: Testing & Security Audit** (10 hours)
   - Write 50+ unit tests
   - Write integration tests
   - Security audit (verify 5 CWEs eliminated)
   - Manual testing on 3+ devices
   - Performance profiling

2. **Production Readiness**
   - Code review (2+ approvals)
   - QA testing
   - Documentation updates
   - Release notes
   - Google Play Store compliance verification

3. **Future Phases (from master analysis)**
   - Phase 2: Critical Features (Download, Folders/Tags)
   - Phase 3: Database & Performance
   - Phase 4: UI Polish & Accessibility
   - Phase 5-8: Chrome Parity, iOS, Desktop

---

**Document Version:** 1.0
**Last Updated:** 2025-11-28
**Status:** Ready for implementation
**Prerequisites:** Phase 1 committed (commit 4d686ad)
