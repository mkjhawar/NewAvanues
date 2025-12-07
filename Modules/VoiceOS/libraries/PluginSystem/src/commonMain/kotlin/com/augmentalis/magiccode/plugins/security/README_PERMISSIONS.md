# Permission UI Framework

A comprehensive user consent system for plugin permissions, replacing the previous auto-grant behavior with proper platform-specific UI dialogs.

## Overview

The permission system consists of three main components:

1. **PermissionUIHandler** - Platform-specific UI for showing permission dialogs
2. **PermissionPersistence** - Storage for user permission decisions
3. **PermissionManager** - Orchestrates permission requests and enforcement

## Architecture

### Core Components

#### PermissionUIHandler (expect/actual)

An expect class with platform-specific implementations:

- **Android**: Uses `AlertDialog.Builder` for permission dialogs
- **JVM**: Uses Swing `JOptionPane` (GUI) or console prompts (headless)
- **iOS**: Uses `UIAlertController` (currently stubbed)

**Methods:**
```kotlin
suspend fun showPermissionDialog(request: PermissionRequest): PermissionResult
suspend fun showRationaleDialog(pluginId: String, pluginName: String, permission: Permission, rationale: String): Boolean
suspend fun showPermissionSettings(pluginId: String, pluginName: String, currentPermissions: Map<Permission, Boolean>): Map<Permission, Boolean>?
```

#### PermissionPersistence

Manages storage of permission decisions with support for:
- Grant/deny tracking
- "Don't ask again" preferences
- Permission history and statistics
- Bulk operations

**Storage Backends:**
- **Android**: `SharedPreferences` (JSON serialization)
- **JVM**: JSON files in platform-appropriate directories
  - Windows: `%APPDATA%/MagicCode/plugin_permissions`
  - macOS: `~/Library/Application Support/MagicCode/plugin_permissions`
  - Linux: `~/.config/MagicCode/plugin_permissions`
- **iOS**: `UserDefaults` (JSON serialization)

#### PermissionManager

Updated to use the permission UI framework:

**Key Features:**
- Checks existing grants before prompting
- Respects "don't ask again" preferences
- Maintains in-memory cache for performance
- Syncs with persistent storage
- Backward compatible API

## Data Models

### PermissionRequest
```kotlin
data class PermissionRequest(
    val pluginId: String,
    val pluginName: String,
    val permissions: Set<Permission>,
    val rationales: Map<Permission, String> = emptyMap()
)
```

### PermissionResult
```kotlin
data class PermissionResult(
    val granted: Set<Permission>,
    val denied: Set<Permission>,
    val dontAskAgain: Set<Permission> = emptySet()
)
```

### PermissionRecord
```kotlin
data class PermissionRecord(
    val permission: Permission,
    val status: GrantStatus,
    val grantedAt: Long?,
    val deniedAt: Long?,
    val dontAskAgain: Boolean,
    val askedCount: Int
)
```

## Usage

### Basic Permission Request

```kotlin
val permissionManager = PermissionManager(
    uiHandler = PermissionUIHandler(context),
    persistence = PermissionPersistence(PermissionStorage(context))
)

// Initialize on startup
permissionManager.initialize()

// Request permissions
val granted = permissionManager.requestPermissions(
    pluginId = "com.example.plugin",
    pluginName = "Example Plugin",
    permissions = setOf(Permission.NETWORK, Permission.STORAGE_READ)
)
```

### With Rationales

```kotlin
val granted = permissionManager.requestPermissions(
    pluginId = "com.example.plugin",
    pluginName = "Example Plugin",
    permissions = setOf(Permission.NETWORK, Permission.STORAGE_READ),
    rationales = mapOf(
        Permission.NETWORK to "Required to sync your data with the cloud",
        Permission.STORAGE_READ to "Needed to import your existing documents"
    )
)
```

### Plugin Manifest Integration

```yaml
id: com.example.my-plugin
name: My Plugin
version: 1.0.0
permissions:
  - NETWORK
  - STORAGE_READ
permissionRationales:
  NETWORK: "Required to download theme updates from the cloud"
  STORAGE_READ: "Needed to access user's custom color palettes"
```

### Checking Permissions

```kotlin
// Check single permission
if (permissionManager.hasPermission(pluginId, Permission.NETWORK)) {
    // Permission granted
}

// Enforce permission (throws if not granted)
try {
    permissionManager.enforcePermission(pluginId, Permission.NETWORK)
    // Proceed with operation
} catch (e: PermissionDeniedException) {
    // Handle denial
}

// Get all granted permissions
val granted = permissionManager.getGrantedPermissions(pluginId)

// Get all denied permissions
val denied = permissionManager.getDeniedPermissions(pluginId)
```

### Managing Permissions

```kotlin
// Show permission settings UI
val changed = permissionManager.showPermissionSettings(pluginId, pluginName)

// Revoke specific permission
permissionManager.revokePermission(pluginId, Permission.NETWORK)

// Revoke all permissions
permissionManager.revokeAllPermissions(pluginId)
```

## Implementation Status

### Fully Implemented
✅ Common interfaces and data models
✅ PermissionPersistence core logic
✅ PermissionManager with UI integration
✅ Android platform implementation (with TODOs for enhancements)
✅ JVM platform implementation (Swing + console)
✅ PluginManifest rationale support

### Stubbed/Partial
⚠️ Android UI - Basic AlertDialog implementation, needs:
- Custom dialog fragments
- Material Design 3 components
- Individual permission toggles
- Better UX for permission grouping

⚠️ JVM UI - Basic Swing dialogs, needs:
- Custom Swing components
- JavaFX alternative
- Better visual design
- Native OS integration

⚠️ iOS UI - Console fallback only, needs:
- UIAlertController implementation
- SwiftUI alternative
- Native iOS permission integration
- iPad adaptations

### Storage Implementations
✅ Android: SharedPreferences
✅ JVM: JSON files
✅ iOS: UserDefaults (basic, needs CoreData option)

## TODO: Full UI Implementation

### Android
```kotlin
// TODO: Create custom DialogFragment
class PermissionDialogFragment : DialogFragment() {
    // Material Design 3 components
    // Individual permission checkboxes
    // Expandable rationale sections
    // Remember choice option
}

// TODO: Use Jetpack Compose for modern UI
@Composable
fun PermissionDialog(request: PermissionRequest) {
    // Compose-based permission dialog
}
```

### JVM
```kotlin
// TODO: Create JavaFX alternative
class PermissionDialogFX : Stage() {
    // Modern JavaFX UI
    // Better visual design
    // Platform-native styling
}

// TODO: JNA integration for native OS dialogs
fun showNativePermissionDialog() {
    // Use platform-native permission dialogs
}
```

### iOS
```kotlin
// TODO: Complete UIAlertController implementation
actual class PermissionUIHandler(
    private val viewController: UIViewController
) {
    // Full UIKit implementation
    // SwiftUI sheet presentation
    // Native iOS permission integration
}

// TODO: Create custom UIViewController
class PermissionViewController : UITableViewController {
    // Grouped table view
    // UISwitch for each permission
    // Detail disclosure
}
```

## Integration Points

### Plugin Installation

When a plugin is installed, request permissions from its manifest:

```kotlin
suspend fun installPlugin(manifest: PluginManifest) {
    // Parse permissions from manifest
    val permissions = manifest.permissions.mapNotNull {
        try { Permission.valueOf(it) } catch (e: Exception) { null }
    }.toSet()

    // Parse rationales
    val rationales = manifest.permissionRationales.mapNotNull { (perm, rationale) ->
        try { Permission.valueOf(perm) to rationale } catch (e: Exception) { null }
    }.toMap()

    // Request permissions
    val granted = permissionManager.requestPermissions(
        pluginId = manifest.id,
        pluginName = manifest.name,
        permissions = permissions,
        rationales = rationales
    )

    if (!granted) {
        // Handle denial - abort installation or continue with limited permissions
    }
}
```

### Runtime Permission Requests

Plugins can request additional permissions at runtime:

```kotlin
class MyPlugin : Plugin {
    override suspend fun onSomeAction() {
        // Check if permission already granted
        if (!permissionManager.hasPermission(pluginId, Permission.CAMERA)) {
            // Request permission with rationale
            val granted = permissionManager.requestPermissions(
                pluginId = pluginId,
                pluginName = manifest.name,
                permissions = setOf(Permission.CAMERA),
                rationales = mapOf(
                    Permission.CAMERA to "Needed to scan QR codes for this feature"
                )
            )

            if (!granted) return
        }

        // Use camera
    }
}
```

### Settings UI Integration

```kotlin
// In plugin settings screen
Button("Manage Permissions") {
    onClick {
        permissionManager.showPermissionSettings(
            pluginId = plugin.id,
            pluginName = plugin.name
        )
    }
}
```

## Security Considerations

1. **Default Deny**: If no UI handler is available, permissions are denied by default
2. **Persistence**: All decisions are persisted to prevent re-prompting
3. **Don't Ask Again**: Respects user's decision to not be prompted again
4. **Revocation**: Users can revoke permissions at any time
5. **Audit Trail**: Permission records include timestamps and request counts

## Testing

### Console Mode (JVM/Headless)

The JVM implementation supports console-based permission prompts for testing:

```kotlin
val uiHandler = PermissionUIHandler() // No parent component = headless mode

// Will use console prompts
val result = uiHandler.showPermissionDialog(request)
```

Example console output:
```
=== Permission Request ===
Plugin: Example Plugin (com.example.plugin)

Requested Permissions:
  1. Network
     Required to sync your data with the cloud
  2. Read Storage
     Needed to import your existing documents

Grant all permissions? (yes/no/choose): choose
Choose permissions individually:
  Grant Network? (y/n): y
    ✓ Granted
  Grant Read Storage? (y/n): n
    ✗ Denied
```

### Unit Tests

```kotlin
@Test
fun testPermissionRequest() = runTest {
    val mockUI = MockPermissionUIHandler(
        result = PermissionResult(
            granted = setOf(Permission.NETWORK),
            denied = setOf(Permission.STORAGE_READ)
        )
    )

    val persistence = PermissionPersistence(InMemoryStorage())
    val manager = PermissionManager(mockUI, persistence)

    val granted = manager.requestPermissions(
        "test.plugin",
        "Test",
        setOf(Permission.NETWORK, Permission.STORAGE_READ)
    )

    assertFalse(granted) // Not all permissions granted
    assertTrue(manager.hasPermission("test.plugin", Permission.NETWORK))
    assertFalse(manager.hasPermission("test.plugin", Permission.STORAGE_READ))
}
```

## Migration Guide

### From Auto-Grant to User Consent

**Before:**
```kotlin
val manager = PermissionManager()
// Auto-grants all permissions
```

**After:**
```kotlin
val manager = PermissionManager(
    uiHandler = PermissionUIHandler(context),
    persistence = PermissionPersistence(PermissionStorage(context))
)
manager.initialize() // Load persisted state
```

**Backward Compatibility:**

The old API still works:
```kotlin
manager.requestPermissions(pluginId, permissions) // Uses pluginId as name, no rationales
```

But the new API is recommended:
```kotlin
manager.requestPermissions(pluginId, pluginName, permissions, rationales)
```

## Performance Considerations

1. **Caching**: In-memory cache for quick permission checks
2. **Lazy Loading**: Persistence loaded on-demand
3. **Batch Operations**: Support for requesting multiple permissions at once
4. **Async UI**: All UI operations are suspending functions

## Future Enhancements

- [ ] Permission groups (e.g., "Storage" includes READ + WRITE)
- [ ] Temporary permissions (auto-revoke after time period)
- [ ] Permission usage analytics
- [ ] Admin/enterprise policy support
- [ ] Remote permission management
- [ ] A/B testing for permission request UX
- [ ] Localization support for permission descriptions
- [ ] Integration with platform permission systems (Android runtime permissions, etc.)

## References

- `PermissionUIHandler.kt` - UI interface and expect class
- `PermissionPersistence.kt` - Storage and persistence logic
- `PermissionManager.kt` - Main permission orchestration
- `PluginManifest.kt` - Permission declaration in manifests
- Platform implementations:
  - `androidMain/...security/PermissionUIHandler.kt`
  - `jvmMain/...security/PermissionUIHandler.kt`
  - `iosMain/...security/PermissionUIHandler.kt`
