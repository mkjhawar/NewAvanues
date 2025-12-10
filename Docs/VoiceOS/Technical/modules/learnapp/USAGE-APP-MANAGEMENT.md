# LearnApp - In-App Management Usage Guide

**Created:** 2025-10-30 00:55 PDT
**Purpose:** How to use LearnAppIntegration methods for resetting/deleting learned apps from within VoiceOS

---

## Overview

LearnAppIntegration now provides public methods for managing learned apps directly from within the VoiceOS app. This eliminates the need for ADB commands during testing and provides a foundation for future UI features.

---

## Available Methods

### 1. `resetLearnedApp(packageName, callback)`

Clears all exploration data and marks the app for re-exploration.

**What it does:**
- Deletes exploration sessions
- Deletes screen states
- Deletes navigation edges
- Sets exploration status to PARTIAL
- Keeps app metadata (name, version, etc.)

**When to use:**
- Testing LearnApp fixes
- Re-exploring an app after it's been updated
- Clearing corrupt exploration data

**Example:**
```kotlin
learnAppIntegration.resetLearnedApp("com.realwear.testcomp") { success, message ->
    if (success) {
        Log.d("VoiceOS", "Reset successful: $message")
        // Show toast: "App Reset" - "App 'com.realwear.testcomp' reset successfully..."
    } else {
        Log.e("VoiceOS", "Reset failed: $message")
        // Show toast: "Reset Failed" - "Failed to reset app: ..."
    }
}
```

---

### 2. `deleteLearnedApp(packageName, callback)`

Completely removes the app from the database.

**What it does:**
- Deletes the app entry
- Deletes ALL associated data (CASCADE)
- Completely removes app from learned_apps table

**When to use:**
- Completely forgetting an app
- Cleaning up test data
- Starting from scratch (more thorough than reset)

**Example:**
```kotlin
learnAppIntegration.deleteLearnedApp("com.realwear.testcomp") { success, message ->
    if (success) {
        Log.d("VoiceOS", "Deleted: $message")
        // Show toast: "App Deleted" - "App 'com.realwear.testcomp' deleted completely..."
    } else {
        Log.e("VoiceOS", "Delete failed: $message")
        // Show toast: "Delete Failed" - "Failed to delete app: ..."
    }
}
```

---

### 3. `getLearnedApps(callback)`

Get list of all learned app package names.

**What it does:**
- Queries learned_apps table
- Returns list of package names

**When to use:**
- Displaying list of learned apps in UI
- Finding apps to reset/delete
- App management screens

**Example:**
```kotlin
learnAppIntegration.getLearnedApps { apps ->
    Log.d("VoiceOS", "Found ${apps.size} learned apps")
    apps.forEach { packageName ->
        Log.d("VoiceOS", "  - $packageName")
    }

    // Example: Display in UI
    recyclerView.adapter = LearnedAppsAdapter(apps)
}
```

---

### 4. `isAppLearned(packageName, callback)`

Check if a specific app has been learned.

**What it does:**
- Checks if package exists in learned_apps table
- Returns boolean

**When to use:**
- Conditional logic based on learning status
- Showing different UI states
- Preventing duplicate learning

**Example:**
```kotlin
learnAppIntegration.isAppLearned("com.realwear.testcomp") { isLearned ->
    if (isLearned) {
        Log.d("VoiceOS", "App already learned")
        showResetButton()
    } else {
        Log.d("VoiceOS", "App not learned yet")
        showLearnButton()
    }
}
```

---

## Integration Examples

### Example 1: Voice Command for Resetting Apps

**In VoiceOSService or CommandProcessor:**

```kotlin
// Handle voice command: "reset learned apps"
fun handleResetLearnedAppsCommand() {
    val learnApp = LearnAppIntegration.getInstance()

    // Get list of learned apps
    learnApp.getLearnedApps { apps ->
        if (apps.isEmpty()) {
            speakResponse("No learned apps to reset")
            return@getLearnedApps
        }

        // For simplicity, reset the most recent app or ask user
        // Here we'll reset the first one as an example
        val packageToReset = apps.first()

        speakResponse("Resetting $packageToReset")

        learnApp.resetLearnedApp(packageToReset) { success, message ->
            if (success) {
                speakResponse("App reset successfully. Launch it again to re-learn.")
            } else {
                speakResponse("Failed to reset app. $message")
            }
        }
    }
}

// Handle voice command: "reset [app name]"
fun handleResetSpecificApp(appName: String) {
    // You'd need a mapping from app name to package name
    val packageName = appNameToPackage(appName) // e.g., "RealWear" -> "com.realwear.testcomp"

    if (packageName == null) {
        speakResponse("Unknown app: $appName")
        return
    }

    val learnApp = LearnAppIntegration.getInstance()

    learnApp.isAppLearned(packageName) { isLearned ->
        if (!isLearned) {
            speakResponse("$appName has not been learned yet")
            return@isAppLearned
        }

        learnApp.resetLearnedApp(packageName) { success, message ->
            if (success) {
                speakResponse("$appName reset. Launch it to re-learn.")
            } else {
                speakResponse("Failed to reset $appName")
            }
        }
    }
}
```

---

### Example 2: UI Screen for Managing Learned Apps

**LearnedAppsFragment.kt:**

```kotlin
class LearnedAppsFragment : Fragment() {
    private lateinit var learnApp: LearnAppIntegration
    private val apps = mutableListOf<String>()
    private lateinit var adapter: LearnedAppsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        learnApp = LearnAppIntegration.getInstance()

        // Setup RecyclerView
        adapter = LearnedAppsAdapter(
            apps = apps,
            onResetClick = { packageName -> resetApp(packageName) },
            onDeleteClick = { packageName -> deleteApp(packageName) }
        )

        recyclerView.adapter = adapter

        // Load learned apps
        loadLearnedApps()
    }

    private fun loadLearnedApps() {
        learnApp.getLearnedApps { appList ->
            apps.clear()
            apps.addAll(appList)
            adapter.notifyDataSetChanged()

            if (apps.isEmpty()) {
                emptyStateTextView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun resetApp(packageName: String) {
        // Show confirmation dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Reset App")
            .setMessage("This will delete all learned data for $packageName. Continue?")
            .setPositiveButton("Reset") { _, _ ->
                learnApp.resetLearnedApp(packageName) { success, message ->
                    if (success) {
                        Toast.makeText(context, "App reset", Toast.LENGTH_SHORT).show()
                        loadLearnedApps() // Refresh list
                    } else {
                        Toast.makeText(context, "Failed: $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteApp(packageName: String) {
        // Show confirmation dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Delete App")
            .setMessage("This will completely remove $packageName from learned apps. Continue?")
            .setPositiveButton("Delete") { _, _ ->
                learnApp.deleteLearnedApp(packageName) { success, message ->
                    if (success) {
                        Toast.makeText(context, "App deleted", Toast.LENGTH_SHORT).show()
                        loadLearnedApps() // Refresh list
                    } else {
                        Toast.makeText(context, "Failed: $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
```

---

### Example 3: Quick Testing Helper

**In VoiceOSService for quick testing:**

```kotlin
// Add this as a debug/testing method
fun quickResetTestCompApp() {
    val learnApp = LearnAppIntegration.getInstance()

    learnApp.resetLearnedApp("com.realwear.testcomp") { success, message ->
        Log.d("VoiceOS", "QuickReset: $message")
        // Toast is already shown by resetLearnedApp method
    }
}

// Call from accessibility service for testing:
// Example: Triple-tap volume down to reset
override fun onKeyEvent(event: KeyEvent): Boolean {
    if (isTripleTapVolumeDown(event)) {
        quickResetTestCompApp()
        return true
    }
    return super.onKeyEvent(event)
}
```

---

## Method Behavior Details

### Reset vs Delete

| Feature | `resetLearnedApp()` | `deleteLearnedApp()` |
|---------|---------------------|----------------------|
| Deletes sessions | ✅ Yes | ✅ Yes |
| Deletes screen states | ✅ Yes | ✅ Yes |
| Deletes navigation edges | ✅ Yes | ✅ Yes |
| Deletes app entry | ❌ No (keeps it) | ✅ Yes |
| Keeps metadata | ✅ Yes (name, version) | ❌ No |
| Sets status | PARTIAL (ready to relearn) | N/A (app deleted) |
| Use case | Testing, updates | Complete removal |

---

### Callbacks

All methods use callbacks to return results on the Main thread:

```kotlin
callback: (success: Boolean, message: String) -> Unit
```

- `success`: True if operation succeeded, false otherwise
- `message`: Human-readable message describing result
- Executes on **Main thread** (safe for UI updates)
- Toast notification automatically shown

---

### Toast Notifications

All reset/delete methods automatically show toast notifications:

**Success:**
- Title: "App Reset" or "App Deleted"
- Message: "[Action] successful. Launch app again to [re-learn/learn from scratch]"

**Failure:**
- Title: "Reset Failed" or "Delete Failed"
- Message: "Failed to [reset/delete] app: [reason]"

You can suppress toasts by checking callback but ignoring result if needed.

---

## Testing Workflow

### Quick Testing Cycle:

**Step 1: Reset the app**
```kotlin
learnApp.resetLearnedApp("com.realwear.testcomp") { success, message ->
    Log.d("Test", "Reset: $success")
}
```

**Step 2: Launch the app**
- Open RealWear TestComp
- LearnApp will detect launch and start exploration

**Step 3: Verify fixes**
- Check logs for package validation
- Check database for correct screen count
- Verify no launcher elements

**Step 4: Repeat as needed**

---

## Error Handling

All methods return errors via callback:

**Common Errors:**

1. **App not found:**
```
success = false
message = "App with package 'com.example.app' not found in database"
```

2. **Database error:**
```
success = false
message = "Error deleting app 'com.example.app': [SQL error]"
```

3. **No error:**
```
success = true
message = "App 'com.example.app' reset successfully..."
```

---

## Future Enhancements

### Planned Features:

1. **UI Screen:**
   - List all learned apps
   - Show app icons and names
   - Reset/Delete buttons per app
   - Search/filter learned apps

2. **Voice Commands:**
   - "Reset learned apps"
   - "Reset [app name]"
   - "Forget [app name]"
   - "Show learned apps"

3. **Batch Operations:**
   - Reset all apps
   - Delete all apps
   - Export/Import learned data

4. **App Details:**
   - Show screens discovered
   - Show elements registered
   - Show last exploration date
   - Show exploration status

---

## Summary

**Quick Reference:**

```kotlin
// Get integration instance
val learnApp = LearnAppIntegration.getInstance()

// Reset app (clear data, keep metadata)
learnApp.resetLearnedApp(packageName) { success, message ->
    // Handle result
}

// Delete app (complete removal)
learnApp.deleteLearnedApp(packageName) { success, message ->
    // Handle result
}

// List all apps
learnApp.getLearnedApps { apps ->
    // Show list
}

// Check if learned
learnApp.isAppLearned(packageName) { isLearned ->
    // Show UI state
}
```

**Benefits:**
- ✅ No ADB required
- ✅ Works from within app
- ✅ Toast notifications automatic
- ✅ Main thread callbacks (UI safe)
- ✅ Error handling included
- ✅ Ready for voice commands
- ✅ Ready for UI integration

---

**Created:** 2025-10-30 00:55 PDT
**Related:** HOW-TO-RELEARN-APP.md, LearnAppIntegration.kt:381-532
