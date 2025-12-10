# VoiceOS Internationalization (i18n) Guide

**Version:** 1.0
**Author:** Manoj Jhawar
**Created:** 2025-11-09
**Phase:** 3 (Medium Priority)

---

## Overview

This guide provides comprehensive best practices for internationalizing VoiceOS to support multiple languages and regions.

### Current Status

- ✅ String resources extracted to `res/values/strings.xml`
- ✅ 140+ string resources defined
- ✅ Plurals support implemented
- ✅ Format string parameters properly typed
- ⏳ Additional language translations pending

---

## Table of Contents

1. [String Externalization](#string-externalization)
2. [Format Strings](#format-strings)
3. [Plurals](#plurals)
4. [Adding New Languages](#adding-new-languages)
5. [Best Practices](#best-practices)
6. [Common Mistakes](#common-mistakes)
7. [Testing i18n](#testing-i18n)

---

## String Externalization

### ✅ DO: Use String Resources

```kotlin
// CORRECT
val message = context.getString(R.string.service_started)
Toast.makeText(context, R.string.database_backup_created, Toast.LENGTH_SHORT).show()
```

### ❌ DON'T: Hardcode Strings

```kotlin
// INCORRECT
val message = "Voice Accessibility service started"
Toast.makeText(context, "Database backup created successfully", Toast.LENGTH_SHORT).show()
```

### When to Externalize

**Externalize:**
- All user-facing messages
- Error messages
- Status messages
- UI labels and buttons
- Notification content
- Accessibility descriptions
- Dialog titles and messages

**Don't Externalize:**
- Log messages (Log.d, Log.e, etc.)
- Debug strings
- Developer comments
- Internal identifiers
- Configuration keys

### Example: String Externalization

**Before:**
```kotlin
class DatabaseBackupManager(private val context: Context) {
    fun createBackup(): BackupResult {
        return if (success) {
            Toast.makeText(context, "Database backup created successfully", Toast.LENGTH_SHORT).show()
            BackupResult(true, "Backup created")
        } else {
            BackupResult(false, "Backup failed")
        }
    }
}
```

**After:**
```kotlin
class DatabaseBackupManager(private val context: Context) {
    fun createBackup(): BackupResult {
        return if (success) {
            Toast.makeText(context, R.string.database_backup_created, Toast.LENGTH_SHORT).show()
            BackupResult(true, context.getString(R.string.database_backup_created))
        } else {
            BackupResult(false, context.getString(R.string.database_backup_failed, error))
        }
    }
}
```

---

## Format Strings

Format strings allow dynamic content insertion while maintaining translatability.

### Single Parameter

**strings.xml:**
```xml
<string name="command_executed">Command executed: %s</string>
<string name="app_not_found">Application not found: %s</string>
```

**Code:**
```kotlin
val message = context.getString(R.string.command_executed, commandName)
val error = context.getString(R.string.app_not_found, appName)
```

### Multiple Parameters (Positional)

**strings.xml:**
```xml
<string name="metrics_command_executed">Command executed: %1$s (%2$d ms)</string>
<string name="accessibility_cursor_move">Move cursor to %1$d, %2$d</string>
```

**Code:**
```kotlin
val message = context.getString(R.string.metrics_command_executed, commandName, executionTime)
val accessibilityMsg = context.getString(R.string.accessibility_cursor_move, x, y)
```

### Format Specifiers

- `%s` - String
- `%d` - Integer (decimal)
- `%f` - Float
- `%.1f` - Float with 1 decimal place
- `%1$s` - Positional string (1st argument)
- `%2$d` - Positional integer (2nd argument)

### Escaping Special Characters

**Apostrophes:**
```xml
<!-- INCORRECT -->
<string name="error">Can't complete the operation</string>

<!-- CORRECT -->
<string name="error">Can\'t complete the operation</string>
<!-- OR -->
<string name="error">Cannot complete the operation</string>
```

**Quotes:**
```xml
<!-- Use \' or \" -->
<string name="quote">He said \"Hello\"</string>
```

---

## Plurals

Use plurals for count-dependent strings that change based on quantity.

### Defining Plurals

**strings.xml:**
```xml
<plurals name="commands_executed">
    <item quantity="one">%d command executed</item>
    <item quantity="other">%d commands executed</item>
</plurals>

<plurals name="backups_available">
    <item quantity="one">%d backup available</item>
    <item quantity="other">%d backups available</item>
</plurals>
```

### Using Plurals

```kotlin
val count = 5
val message = context.resources.getQuantityString(
    R.plurals.commands_executed,
    count,
    count
)
// Result: "5 commands executed"

val backupCount = 1
val backupMessage = context.resources.getQuantityString(
    R.plurals.backups_available,
    backupCount,
    backupCount
)
// Result: "1 backup available"
```

### Plural Quantities

- `zero` - When count is 0 (some languages like Arabic)
- `one` - Singular (1 item)
- `two` - Dual (some languages like Arabic)
- `few` - Few items (some Slavic languages)
- `many` - Many items (some languages)
- `other` - Default fallback (required)

**Note:** English only uses `one` and `other`, but other languages may use all quantities.

---

## Adding New Languages

### 1. Create Language-Specific Directory

```bash
# Spanish
mkdir -p modules/apps/VoiceOSCore/src/main/res/values-es

# French
mkdir -p modules/apps/VoiceOSCore/src/main/res/values-fr

# German
mkdir -p modules/apps/VoiceOSCore/src/main/res/values-de

# Japanese
mkdir -p modules/apps/VoiceOSCore/src/main/res/values-ja
```

### 2. Copy and Translate strings.xml

```bash
# Copy base strings.xml to Spanish directory
cp modules/apps/VoiceOSCore/src/main/res/values/strings.xml \
   modules/apps/VoiceOSCore/src/main/res/values-es/strings.xml
```

Then translate the content:

**values-es/strings.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Voice OS</string>
    <string name="service_name">Servicio Voice OS</string>
    <string name="service_started">Servicio de accesibilidad por voz iniciado</string>
    <string name="command_executed">Comando ejecutado: %s</string>
    <!-- ... translate all strings ... -->
</resources>
```

### 3. Test Language Selection

```kotlin
// Programmatically test different locales
val locale = Locale("es") // Spanish
Locale.setDefault(locale)
val config = Configuration()
config.setLocale(locale)
context.resources.updateConfiguration(config, context.resources.displayMetrics)
```

### 4. Regional Variants

```bash
# Spanish (Spain)
values-es-rES/

# Spanish (Mexico)
values-es-rMX/

# English (UK)
values-en-rGB/

# Chinese (Simplified)
values-zh-rCN/

# Chinese (Traditional)
values-zh-rTW/
```

---

## Best Practices

### 1. Always Use Resource IDs

```kotlin
// ✅ CORRECT
textView.text = getString(R.string.welcome_message)
Toast.makeText(this, R.string.operation_success, Toast.LENGTH_SHORT).show()

// ❌ INCORRECT
textView.text = "Welcome to VoiceOS"
Toast.makeText(this, "Operation completed successfully", Toast.LENGTH_SHORT).show()
```

### 2. Keep Strings Concise

```xml
<!-- ❌ Too verbose -->
<string name="error_message">An error occurred while trying to process your request. Please check your internet connection and try again later.</string>

<!-- ✅ Concise -->
<string name="error_network">Network error. Please try again.</string>
```

### 3. Use Meaningful Resource Names

```xml
<!-- ❌ Generic names -->
<string name="text1">Voice OS Service</string>
<string name="msg">Service started</string>

<!-- ✅ Descriptive names -->
<string name="service_name">Voice OS Service</string>
<string name="service_started">Service started</string>
```

### 4. Group Related Strings

```xml
<!-- Group by feature -->
<!-- Database Messages -->
<string name="database_backup_created">Database backup created successfully</string>
<string name="database_backup_failed">Database backup failed: %s</string>
<string name="database_restored">Database restored successfully</string>

<!-- Security & Privacy -->
<string name="security_unauthorized_access">Unauthorized access attempt blocked</string>
<string name="security_signature_invalid">Invalid application signature</string>
```

### 5. Don't Concatenate Strings

```kotlin
// ❌ INCORRECT - Won't work in other languages
val message = getString(R.string.welcome) + " " + userName + "!"

// ✅ CORRECT - Use format strings
val message = getString(R.string.welcome_user, userName)
```

**strings.xml:**
```xml
<string name="welcome_user">Welcome %s!</string>
```

### 6. Handle Text Direction (RTL)

For right-to-left languages (Arabic, Hebrew):

```xml
<!-- Use start/end instead of left/right -->
<Button
    android:layout_marginStart="16dp"
    android:layout_marginEnd="16dp" />

<!-- NOT -->
<Button
    android:layout_marginLeft="16dp"
    android:layout_marginRight="16dp" />
```

---

## Common Mistakes

### 1. Hardcoded Strings in Logs

```kotlin
// ✅ CORRECT - Logs can be hardcoded (not user-facing)
Log.d(TAG, "Database backup created successfully")
Log.e(TAG, "Error creating backup: ${e.message}")

// ❌ INCORRECT - User-facing strings should be in resources
Toast.makeText(this, "Backup created", Toast.LENGTH_SHORT).show()
```

### 2. Forgetting Format Parameters

```xml
<string name="error_occurred">An error occurred: %s</string>
```

```kotlin
// ❌ INCORRECT - Missing parameter
val message = getString(R.string.error_occurred)

// ✅ CORRECT
val message = getString(R.string.error_occurred, errorDetails)
```

### 3. Mismatched Plural Quantities

```kotlin
// ❌ INCORRECT - Passing wrong count
val message = context.resources.getQuantityString(
    R.plurals.commands_executed,
    5,  // quantity for determining which string to use
    10  // value to substitute in the string
)

// ✅ CORRECT - Both should match
val count = 5
val message = context.resources.getQuantityString(
    R.plurals.commands_executed,
    count,
    count
)
```

---

## Testing i18n

### 1. Enable Pseudolocales (Android Studio)

Settings → Developer Options → Drawing → Enable Pseudolocales

This will show:
- **en-XA** - Accented English (to spot hardcoded strings)
- **ar-XB** - RTL testing (to test layout issues)

### 2. Test Multiple Languages

```kotlin
// Test Spanish
val esLocale = Locale("es")
val esResources = updateResourcesLocale(context, esLocale)
val esMessage = esResources.getString(R.string.service_started)

// Test Japanese
val jaLocale = Locale("ja")
val jaResources = updateResourcesLocale(context, jaLocale)
val jaMessage = jaResources.getString(R.string.service_started)
```

### 3. Check for Missing Translations

```bash
# Use Android Lint
./gradlew :modules:apps:VoiceOSCore:lintDebug

# Look for warnings like:
# "string_name" is not translated in es, fr, de
```

### 4. Test Layout with Long Text

Some languages (German, Finnish) have much longer words. Test UI with these languages to ensure layouts don't break.

---

## Resources

### Android Documentation

- [Localizing with Resources](https://developer.android.com/guide/topics/resources/localization)
- [String Resources](https://developer.android.com/guide/topics/resources/string-resource)
- [Plurals](https://developer.android.com/guide/topics/resources/string-resource#Plurals)
- [RTL Support](https://developer.android.com/training/basics/supporting-devices/languages#SupportLayoutDirections)

### Translation Services

- Google Cloud Translation API
- Microsoft Translator
- DeepL API
- Professional translation services (for production)

---

## Migration Checklist

To fully internationalize VoiceOS:

- [x] Create base strings.xml with all user-facing strings
- [x] Define format strings with proper parameter types
- [x] Implement plurals for count-dependent strings
- [ ] Add Spanish (es) translation
- [ ] Add French (fr) translation
- [ ] Add German (de) translation
- [ ] Add Japanese (ja) translation
- [ ] Add Chinese Simplified (zh-CN) translation
- [ ] Test all layouts with RTL languages
- [ ] Run Lint to check for missing translations
- [ ] Test with pseudolocales
- [ ] Update all code to use R.string references

---

## Example Usage in VoiceOS

### UserConsentManager

**strings.xml:**
```xml
<string name="consent_analytics_title">Analytics</string>
<string name="consent_granted">Consent granted for %s</string>
```

**Code:**
```kotlin
class UserConsentManager(private val context: Context) {
    fun grantConsent(type: ConsentType) {
        val typeName = context.getString(getConsentTitleRes(type))
        val message = context.getString(R.string.consent_granted, typeName)
        Log.i(TAG, message)
    }

    private fun getConsentTitleRes(type: ConsentType): Int {
        return when (type) {
            ConsentType.ANALYTICS -> R.string.consent_analytics_title
            ConsentType.CRASH_REPORTS -> R.string.consent_crash_reports_title
            // ...
        }
    }
}
```

### DatabaseBackupManager

**strings.xml:**
```xml
<string name="database_backup_created">Database backup created successfully</string>
<string name="database_backup_failed">Database backup failed: %s</string>
```

**Code:**
```kotlin
class DatabaseBackupManager(private val context: Context) {
    suspend fun createBackup(label: String?): BackupResult {
        return try {
            // ... backup logic ...
            BackupResult(
                success = true,
                message = context.getString(R.string.database_backup_created)
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = context.getString(R.string.database_backup_failed, e.message)
            )
        }
    }
}
```

---

**End of Guide**

**Last Updated:** 2025-11-09
**Version:** 1.0
**Phase:** 3 (Medium Priority)
