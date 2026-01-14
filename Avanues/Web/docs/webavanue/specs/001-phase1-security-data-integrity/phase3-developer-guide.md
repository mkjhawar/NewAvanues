# WebAvanue Phase 3: Security Implementation - Developer Guide

**Version:** 1.0
**Date:** 2025-11-28
**Status:** ✅ Complete
**Author:** Development Team

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [HTTP Authentication Dialog](#http-authentication-dialog)
4. [File Upload Support](#file-upload-support)
5. [Site Permissions Management](#site-permissions-management)
6. [Security ViewModel](#security-viewmodel)
7. [Testing](#testing)
8. [Deployment](#deployment)

---

## Overview

Phase 3 completes the security implementation for WebAvanue by adding:

1. **HTTP Authentication Dialog** - Material Design 3 dialog for HTTP Basic/Digest authentication
2. **File Upload Support** - Native file picker integration for HTML file inputs
3. **Site Permissions Management UI** - User interface to view and revoke granted permissions

### Security Vulnerabilities Addressed

- **CWE-295**: Improper Certificate Validation → SSL error dialogs with user consent
- **CWE-276**: Incorrect Default Permissions → User consent required for all permissions
- **CWE-1021**: Improper Restriction of Rendered UI Layers → Dialog spam prevention

### Commits

**Implementation:**
- **ff2229b**: HTTP Authentication Dialog (5 files, 252 insertions)
- **861a16f**: File Upload Support (3 files, 46 insertions)
- **a010427**: Site Permissions Management UI (6 files, 391 insertions)

**Testing & Documentation:**
- **565fb25**: SecurityViewModel unit tests (14 tests)
- **0ca209a**: Integration tests (20 tests, build config)
- **2ff34d5**: Complete Phase 3 documentation (user manual, dev guide)
- **c405637**: Update completion summary with test status

**Total**: 17 files, ~3,200 lines (689 production + 350 unit tests + 820 integration tests + 1,400 docs)

---

## Architecture

### Module Structure

```
common/libs/webavanue/
├── coredata/                          # Data layer
│   ├── domain/
│   │   ├── model/
│   │   │   └── SitePermission.kt     # Permission domain model
│   │   └── repository/
│   │       └── BrowserRepository.kt   # Repository interface (with getAllSitePermissions)
│   ├── data/
│   │   └── repository/
│   │       └── BrowserRepositoryImpl.kt  # Repository implementation
│   └── sqldelight/
│       └── BrowserDatabase.sq         # SQL queries (getAllSitePermissions)
└── universal/                         # UI layer
    ├── presentation/
    │   ├── viewmodel/
    │   │   └── SecurityViewModel.kt   # Security state management
    │   └── ui/
    │       ├── security/
    │       │   ├── SecurityState.kt   # State models (HttpAuthRequest, HttpAuthCredentials)
    │       │   └── SecurityDialogs.kt # Dialog composables (HttpAuthenticationDialog)
    │       ├── settings/
    │       │   ├── SettingsScreen.kt  # Settings with Site Permissions nav
    │       │   └── SitePermissionsScreen.kt  # Permissions management UI
    │       └── browser/
    │           ├── BrowserScreen.kt   # Dialog rendering
    │           └── WebViewContainer.android.kt  # WebView integration
    └── build.gradle.kts               # Added androidx.activity:activity-compose
```

### Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      WebView Callbacks                       │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│              WebViewContainer.android.kt                     │
│  • onReceivedHttpAuthRequest()                              │
│  • onShowFileChooser()                                      │
│  • onPermissionRequest()                                    │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                   SecurityViewModel                          │
│  • showHttpAuthDialog()                                     │
│  • showPermissionRequestDialog()                            │
│  • Dialog spam prevention                                   │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                     BrowserScreen                            │
│  • Collects StateFlows                                      │
│  • Renders dialogs                                          │
│  • Handles user actions                                     │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                 BrowserRepository                            │
│  • insertSitePermission()                                   │
│  • getSitePermission()                                      │
│  • getAllSitePermissions()                                  │
│  • deleteSitePermission()                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## HTTP Authentication Dialog

### Overview

Implements HTTP Basic and Digest authentication with Material Design 3 dialog.

### Implementation Files

**SecurityState.kt** - Data models:
```kotlin
data class HttpAuthRequest(
    val host: String,
    val realm: String,
    val scheme: String = "Basic"
)

data class HttpAuthCredentials(
    val username: String,
    val password: String
)
```

**SecurityDialogs.kt** - UI composable:
```kotlin
@Composable
fun HttpAuthenticationDialog(
    authRequest: HttpAuthRequest,
    onAuthenticate: (HttpAuthCredentials) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
)
```

**SecurityViewModel.kt** - State management:
```kotlin
private val _httpAuthState = MutableStateFlow<HttpAuthDialogState?>(null)
val httpAuthState: StateFlow<HttpAuthDialogState?> = _httpAuthState.asStateFlow()

fun showHttpAuthDialog(
    authRequest: HttpAuthRequest,
    onAuthenticate: (HttpAuthCredentials) -> Unit,
    onCancel: () -> Unit
)

fun dismissHttpAuthDialog()
```

**WebViewContainer.android.kt** - WebView integration:
```kotlin
override fun onReceivedHttpAuthRequest(
    view: WebView?,
    handler: android.webkit.HttpAuthHandler?,
    host: String?,
    realm: String?
) {
    if (handler == null) {
        handler?.cancel()
        return
    }

    val authRequest = HttpAuthRequest(
        host = host ?: "Unknown",
        realm = realm ?: "",
        scheme = "Basic"
    )

    securityViewModel?.showHttpAuthDialog(
        authRequest = authRequest,
        onAuthenticate = { credentials ->
            handler.proceed(credentials.username, credentials.password)
        },
        onCancel = {
            handler.cancel()
        }
    )
}
```

**BrowserScreen.kt** - Dialog rendering:
```kotlin
val httpAuthState by securityViewModel.httpAuthState.collectAsState()

httpAuthState?.let { state ->
    HttpAuthenticationDialog(
        authRequest = state.authRequest,
        onAuthenticate = state.onAuthenticate,
        onCancel = state.onCancel,
        onDismiss = { securityViewModel.dismissHttpAuthDialog() }
    )
}
```

### Features

- ✅ Username and password input fields with validation
- ✅ Sign In button (enabled only when both fields filled)
- ✅ Cancel button
- ✅ Displays hostname and realm information
- ✅ Supports authentication scheme display (Basic/Digest)
- ✅ Dialog spam prevention (max 3 per 10 seconds)
- ✅ Material Design 3 styling

### Security Considerations

- Passwords are never stored by WebAvanue
- Credentials passed directly to WebView's HttpAuthHandler
- Dialog can be dismissed by user at any time
- No automatic authentication (always requires user input)

---

## File Upload Support

### Overview

Integrates Android file picker for HTML `<input type="file">` elements.

### Implementation Files

**build.gradle.kts** - New dependency:
```kotlin
androidMain {
    dependencies {
        // Activity Compose - For rememberLauncherForActivityResult
        implementation("androidx.activity:activity-compose:1.8.2")
    }
}
```

**WebViewContainer.android.kt** - File picker integration:
```kotlin
// File upload support - callback from onShowFileChooser
var filePathCallback by remember(tabId) { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

// File picker launcher - handles file selection for <input type="file">
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetMultipleContents()
) { uris: List<Uri> ->
    // Return selected files to WebView
    filePathCallback?.onReceiveValue(uris.toTypedArray())
    filePathCallback = null
}

// WebChromeClient override
override fun onShowFileChooser(
    webView: WebView?,
    newFilePathCallback: ValueCallback<Array<Uri>>?,
    fileChooserParams: FileChooserParams?
): Boolean {
    if (newFilePathCallback == null) return false

    // Cancel previous callback if any
    filePathCallback?.onReceiveValue(null)

    // Store callback for file picker result
    filePathCallback = newFilePathCallback

    // Extract MIME types from params
    val acceptTypes = fileChooserParams?.acceptTypes?.firstOrNull() ?: "*/*"

    // Launch file picker with appropriate MIME type
    filePickerLauncher.launch(acceptTypes)

    return true
}
```

### Features

- ✅ Native Android file picker integration
- ✅ MIME type filtering from HTML accept attribute
  - Example: `accept="image/*"` shows only images
  - Example: `accept=".pdf"` shows only PDFs
- ✅ Multiple file selection support (`<input type="file" multiple>`)
- ✅ Proper callback lifecycle management
- ✅ Cancellation support (user can dismiss picker)
- ✅ Logging for debugging

### Security Considerations

- Only user-selected files are accessible
- File access limited to file picker session
- No background file access
- Picker permissions handled by Android system

### Supported File Types

- Images: image/*, image/jpeg, image/png, image/gif, image/webp
- Documents: application/pdf, text/*, application/msword
- Archives: application/zip, application/x-rar-compressed
- All types: */*

---

## Site Permissions Management

### Overview

User interface to view and revoke granted permissions (camera, microphone, location).

### Database Schema

**BrowserDatabase.sq** - New query:
```sql
getAllSitePermissions:
SELECT * FROM site_permission ORDER BY domain, permission_type;
```

Existing table schema:
```sql
CREATE TABLE site_permission (
    domain TEXT NOT NULL,
    permission_type TEXT NOT NULL,
    granted INTEGER NOT NULL,  -- 0 = denied, 1 = granted
    timestamp INTEGER NOT NULL,
    PRIMARY KEY (domain, permission_type)
);
```

### Repository Layer

**BrowserRepository.kt** - New method:
```kotlin
suspend fun getAllSitePermissions(): Result<List<SitePermission>>
```

**BrowserRepositoryImpl.kt** - Implementation:
```kotlin
override suspend fun getAllSitePermissions(): Result<List<SitePermission>> = withContext(Dispatchers.IO) {
    try {
        val permissions = queries.getAllSitePermissions()
            .executeAsList()
            .map { it.toDomainModel() }
        Result.success(permissions)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**FakeBrowserRepository.kt** - Test stub:
```kotlin
override suspend fun getAllSitePermissions(): Result<List<SitePermission>> {
    return Result.success(emptyList()) // No permissions for tests
}
```

### UI Layer

**SitePermissionsScreen.kt** - New screen (360 lines):
```kotlin
@Composable
fun SitePermissionsScreen(
    repository: BrowserRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

Features:
- Lists permissions grouped by domain
- Individual permission rows with icons
- Delete button for individual permissions
- Delete all button for entire domain
- Confirmation dialog before clearing all
- Empty state when no permissions
- Loading state with spinner
- Error state with retry button
- Real-time updates after revocation

**SettingsScreen.kt** - Navigation integration:
```kotlin
// Added parameter
onNavigateToSitePermissions: () -> Unit = {}

// Added navigation item in Privacy & Security section
item {
    NavigationSettingItem(
        title = "Site Permissions",
        subtitle = "Manage camera, microphone, and location permissions",
        onClick = onNavigateToSitePermissions
    )
}
```

### UI Components

**SitePermissionItem** - Card showing permissions for one domain:
```kotlin
@Composable
fun SitePermissionItem(
    domain: String,
    permissions: List<SitePermission>,
    onDeletePermission: (String) -> Unit,
    onDeleteAllPermissions: () -> Unit,
    modifier: Modifier = Modifier
)
```

**PermissionRow** - Single permission display:
```kotlin
@Composable
fun PermissionRow(
    permissionType: String,
    granted: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
)
```

### Features

- ✅ Domain-grouped permission display
- ✅ Icons for each permission type (Camera 📹, Mic 🎤, Location 📍)
- ✅ Status indicators (Allowed ✅, Denied ❌)
- ✅ Individual permission revocation
- ✅ Clear all permissions for domain with confirmation
- ✅ Empty state when no permissions granted
- ✅ Loading and error states with retry
- ✅ Material Design 3 styling
- ✅ Real-time updates after changes

---

## Security ViewModel

### Overview

Centralized security state management for all security dialogs.

### Dialog Spam Prevention

```kotlin
private val dialogTimestamps = mutableListOf<Long>()
private val maxDialogsPerWindow = 3
private val timeWindowMs = 10_000L // 10 seconds

private fun isDialogSpamDetected(): Boolean {
    val now = Clock.System.now().toEpochMilliseconds()

    // Remove timestamps older than time window
    dialogTimestamps.removeAll { timestamp ->
        (now - timestamp) > timeWindowMs
    }

    // Check if we've hit the limit
    return dialogTimestamps.size >= maxDialogsPerWindow
}

private fun recordDialogShown() {
    val now = Clock.System.now().toEpochMilliseconds()
    dialogTimestamps.add(now)

    // Log spam prevention status
    if (dialogTimestamps.size >= maxDialogsPerWindow - 1) {
        println("⚠️  Dialog spam warning: ${dialogTimestamps.size}/$maxDialogsPerWindow dialogs in last 10s")
    }
}
```

### State Management Pattern

All dialogs follow the same pattern:

1. **State Flow**: `MutableStateFlow<DialogState?>`
2. **Public StateFlow**: `val dialogState: StateFlow<DialogState?>`
3. **Show Method**: `fun showDialog(...) { _dialogState.value = DialogState(...) }`
4. **Dismiss Method**: `fun dismissDialog() { _dialogState.value = null }`
5. **Spam Check**: Call `isDialogSpamDetected()` before showing

### Managed Dialogs

- SSL Error Dialog (Phase 1)
- Permission Request Dialog (Phase 1)
- JavaScript Alert Dialog (Phase 1)
- JavaScript Confirm Dialog (Phase 1)
- JavaScript Prompt Dialog (Phase 1)
- HTTP Authentication Dialog (Phase 3) ✅
- (Future: File Upload Progress, Download Manager, etc.)

---

## Testing

### Unit Tests

**SecurityViewModelTest.kt** - Existing tests (14 tests, all passing):
- SSL error dialog state management
- Permission request dialog with spam prevention
- JavaScript dialogs (alert, confirm, prompt)
- Dialog spam prevention algorithm
- Permission persistence

### Integration Tests

**Status**: ✅ Complete (Commit: 0ca209a)

**SecurityFeaturesIntegrationTest.kt** - 20 integration tests covering all Phase 3 features:

**Test File Location:**
```
common/libs/webavanue/universal/src/androidTest/kotlin/
  com/augmentalis/Avanues/web/universal/SecurityFeaturesIntegrationTest.kt
```

**Test Categories:**

1. **HTTP Authentication Dialog (Tests 1-10)**
   - ✅ Dialog rendering with all UI elements
   - ✅ Button state management (disabled/enabled)
   - ✅ Credential input and callback validation
   - ✅ Cancel flow and dismiss handling
   - ✅ Realm display (present and empty)
   - ✅ SecurityViewModel integration
   - ✅ Dialog spam prevention

2. **Site Permissions Management UI (Tests 11-16)**
   - ✅ Empty state display
   - ✅ Permission cards with status indicators
   - ✅ Individual permission revocation
   - ✅ Bulk domain permission clearing
   - ✅ Confirmation dialogs
   - ✅ Navigation and back button

3. **Infrastructure & Database (Tests 17-20)**
   - ✅ File upload infrastructure validation
   - ✅ Site permissions CRUD operations
   - ✅ getAllSitePermissions query
   - ✅ Permission persistence across restarts

**Running the Tests:**

```bash
# Compile integration tests
./gradlew :common:libs:webavanue:universal:compileDebugAndroidTestKotlin

# Run all integration tests (requires device/emulator)
./gradlew :common:libs:webavanue:universal:connectedDebugAndroidTest

# Run specific test
./gradlew :common:libs:webavanue:universal:connectedDebugAndroidTest \
  --tests SecurityFeaturesIntegrationTest.test_httpAuthDialog_rendersCorrectly
```

**Test Dependencies:**

Added to `build.gradle.kts`:
```gradle
val androidInstrumentedTest by getting {
    dependencies {
        implementation(kotlin("test"))
        implementation(kotlin("test-junit"))
        implementation("androidx.test:core:1.5.0")
        implementation("androidx.test:runner:1.5.2")
        implementation("androidx.test.ext:junit:1.1.5")
        implementation("androidx.compose.ui:ui-test-junit4:1.5.4")
        implementation("app.cash.sqldelight:android-driver:2.0.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    }
}
```

**Test Configuration:**

```gradle
android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}
```

### Manual Testing Checklist

**HTTP Authentication:**
- [ ] Visit site with HTTP Basic auth (e.g., httpbin.org/basic-auth/user/pass)
- [ ] Dialog appears with correct hostname and realm
- [ ] Username and password fields work
- [ ] Sign In button enabled only when both fields filled
- [ ] Cancel button works
- [ ] Successful authentication proceeds to page
- [ ] Spam prevention activates after 3 dialogs

**File Upload:**
- [ ] Visit site with `<input type="file">`
- [ ] Android file picker opens
- [ ] MIME type filtering works (test with image/*, .pdf)
- [ ] Single file selection works
- [ ] Multiple file selection works (with multiple attribute)
- [ ] Cancel works (no file uploaded)
- [ ] Selected files upload successfully

**Site Permissions:**
- [ ] Settings → Site Permissions opens
- [ ] Empty state shown when no permissions
- [ ] Grant permissions on test site (camera/mic/location)
- [ ] Permissions appear grouped by domain
- [ ] Icons display correctly for each type
- [ ] Individual delete works
- [ ] Confirmation dialog appears for delete all
- [ ] Delete all works correctly
- [ ] Real-time updates after revocation

---

## Deployment

### Build Configuration

**Version**: 1.2.0
**Build**: Release
**Modules**: coredata + universal

### Gradle Tasks

```bash
# Clean build
./gradlew clean

# Compile all modules
./gradlew :common:libs:webavanue:coredata:compileDebugKotlinAndroid
./gradlew :common:libs:webavanue:universal:compileDebugKotlinAndroid

# Run tests
./gradlew :common:libs:webavanue:universal:testDebugUnitTest

# Build release APK (when ready)
./gradlew :common:libs:webavanue:app:assembleRelease
```

### Pre-Deployment Checklist

- [x] All code compiles without errors
- [x] Unit tests pass (14/14 SecurityViewModelTest)
- [x] Integration tests written (20/20 SecurityFeaturesIntegrationTest)
- [ ] Integration tests executed on device/emulator
- [x] User manual updated with new features
- [x] Developer documentation updated
- [x] Version numbers updated (1.2.0)
- [x] All commits pushed to remote
- [ ] Release notes prepared
- [ ] Git tags created for commits

### Rollout Plan

**Phase 1: Internal Testing**
- Deploy to development devices
- QA team manual testing
- Fix any critical bugs

**Phase 2: Beta Release**
- Deploy to beta testers
- Gather feedback
- Monitor crash reports

**Phase 3: Production Release**
- Deploy to all users
- Monitor metrics (dialog usage, permission grants, file uploads)
- Prepare hotfix pipeline

---

## Migration Guide

### For Developers

**No breaking changes** - Phase 3 is additive only.

**New Dependencies:**
```gradle
// Add to universal/build.gradle.kts
implementation("androidx.activity:activity-compose:1.8.2")
```

**New Repository Methods:**
```kotlin
// BrowserRepository
suspend fun getAllSitePermissions(): Result<List<SitePermission>>
```

**New ViewModel:**
- SecurityViewModel already exists (Phase 1)
- Added httpAuthState StateFlow
- Added showHttpAuthDialog() and dismissHttpAuthDialog()

**New Screens:**
- SitePermissionsScreen.kt (new file)
- SettingsScreen.kt (added navigation parameter)

### For Users

**Automatic Updates:**
- New dialogs appear automatically when triggered
- File upload works seamlessly with existing HTML forms
- Site Permissions accessible from Settings

**No Action Required:**
- Existing permissions remain valid
- No data migration needed
- No settings changes required

---

## Future Enhancements

### Potential Additions

1. **Remember HTTP Auth Credentials**
   - Store encrypted credentials per domain
   - Auto-fill on subsequent auth requests
   - Clear credentials option in settings

2. **File Upload Progress**
   - Show upload progress bar
   - Cancel upload option
   - Upload queue for multiple files

3. **Permission Usage Indicators**
   - Show when camera/mic is active
   - Notification when website uses permissions
   - Privacy dashboard with usage stats

4. **Advanced Permission Controls**
   - Temporary permissions (one-time use)
   - Time-based permissions (expire after X hours)
   - Per-tab permissions (not global)

5. **Security Audit Log**
   - Log all permission grants/denials
   - Log SSL warnings and user choices
   - Export security audit log

---

## Troubleshooting

### Common Issues

**Issue: HTTP Auth dialog not appearing**
- Check SecurityViewModel is passed to WebViewContainer
- Verify WebChromeClient.onReceivedHttpAuthRequest is called
- Check dialog spam prevention logs

**Issue: File picker not opening**
- Verify androidx.activity:activity-compose dependency
- Check Android storage permissions
- Verify rememberLauncherForActivityResult is called in composition

**Issue: Permissions not persisting**
- Check SQLDelight database connection
- Verify site_permission table exists
- Check repository.insertSitePermission calls

**Issue: Permissions screen empty**
- Grant some permissions first (camera/mic/location)
- Check getAllSitePermissions() returns data
- Verify SQLDelight query syntax

### Debug Logging

Enable debug logging by searching for:
- `println("🔐 HTTP Authentication requested")`
- `println("📎 File upload requested")`
- `println("✅ Persisted permissions")`
- `println("⚠️  Dialog spam detected")`

---

## References

### Documentation

- [Phase 1 Security Spec](./spec.md)
- [Phase 2 Implementation Guide](./phase2-implementation-guide.md)
- [WebAvanue User Manual](../../webavanue-user-manual.md)

### External Resources

- [Android WebView](https://developer.android.com/reference/android/webkit/WebView)
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [ActivityResultContracts](https://developer.android.com/training/basics/intents/result)

### Security Standards

- [CWE-295: Improper Certificate Validation](https://cwe.mitre.org/data/definitions/295.html)
- [CWE-276: Incorrect Default Permissions](https://cwe.mitre.org/data/definitions/276.html)
- [CWE-1021: Improper Restriction of Rendered UI Layers](https://cwe.mitre.org/data/definitions/1021.html)
- [OWASP Mobile Top 10](https://owasp.org/www-project-mobile-top-10/)

---

**Document Version:** 1.0
**Last Updated:** 2025-11-28
**Status:** ✅ Complete
**© 2025 Augmentalis. All rights reserved.**
